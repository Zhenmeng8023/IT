<template>
  <div class="task-dependency-panel">
    <div class="panel-card create-card">
      <div class="row-title">新增前置依赖</div>
      <div class="create-row">
        <el-select v-model="form.predecessorTaskId" size="small" filterable placeholder="选择前置任务" class="task-select">
          <el-option
            v-for="item in taskOptions"
            :key="item.id"
            :label="item.title || ('任务#' + item.id)"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="form.dependencyType" size="small" class="type-select">
          <el-option label="完成后开始" value="finish_to_start" />
          <el-option label="开始后开始" value="start_to_start" />
          <el-option label="完成后完成" value="finish_to_finish" />
          <el-option label="开始后完成" value="start_to_finish" />
        </el-select>
        <el-button type="primary" size="small" :loading="submitting" @click="addDependency">添加</el-button>
      </div>
    </div>

    <div v-if="!dependencies.length" class="panel-card empty-wrap">
      <el-empty description="暂无依赖关系" />
    </div>

    <div v-else class="dependency-list">
      <div v-for="item in dependencies" :key="item.id" class="dependency-card">
        <div class="dependency-main">
          <div class="dependency-title">{{ resolveTitle(item) }}</div>
          <div class="dependency-meta">
            <el-tag size="mini" effect="plain">{{ formatType(item.dependencyType) }}</el-tag>
            <span>状态：{{ resolveStatus(item) }}</span>
          </div>
        </div>
        <div class="dependency-actions">
          <el-button size="mini" type="danger" plain @click="removeDependency(item)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  createTaskDependency,
  deleteTaskDependency,
  getTaskDependencies,
  listProjectTasks
} from '@/api/project'

export default {
  name: 'TaskDependencyPanel',
  props: {
    taskId: {
      type: Number,
      default: null
    },
    projectId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      dependencies: [],
      taskOptions: [],
      form: {
        predecessorTaskId: null,
        dependencyType: 'finish_to_start'
      },
      submitting: false
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) {
          this.loadDependencies()
          this.loadTaskOptions()
        } else {
          this.dependencies = []
          this.taskOptions = []
        }
      }
    }
  },
  methods: {
    async loadDependencies() {
      if (!this.taskId) return
      const res = await getTaskDependencies(this.taskId)
      this.dependencies = Array.isArray(res && res.data) ? res.data : []
    },
    async loadTaskOptions() {
      if (!this.projectId) return
      const res = await listProjectTasks(this.projectId, { page: 1, size: 300 })
      const data = res && res.data
      const list = Array.isArray(data && data.list) ? data.list : (Array.isArray(data) ? data : [])
      this.taskOptions = list.filter(item => Number(item.id) !== Number(this.taskId))
    },
    async addDependency() {
      if (!this.form.predecessorTaskId) {
        this.$message.warning('请选择前置任务')
        return
      }
      this.submitting = true
      try {
        await createTaskDependency(this.taskId, {
          predecessorTaskId: this.form.predecessorTaskId,
          dependencyType: this.form.dependencyType
        })
        this.form.predecessorTaskId = null
        this.form.dependencyType = 'finish_to_start'
        await this.loadDependencies()
        this.$emit('changed')
        this.$message.success('依赖已添加')
      } finally {
        this.submitting = false
      }
    },
    async removeDependency(item) {
      try {
        await this.$confirm('确认删除这条依赖关系吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      await deleteTaskDependency(item.id)
      await this.loadDependencies()
      this.$emit('changed')
      this.$message.success('删除成功')
    },
    resolveTitle(item) {
      return item.predecessorTaskTitle || item.taskTitle || item.title || ('任务#' + (item.predecessorTaskId || item.id))
    },
    resolveStatus(item) {
      return item.predecessorTaskStatusText || item.predecessorTaskStatus || item.statusText || item.status || '-'
    },
    formatType(type) {
      const map = {
        finish_to_start: '完成后开始',
        start_to_start: '开始后开始',
        finish_to_finish: '完成后完成',
        start_to_finish: '开始后完成'
      }
      return map[type] || type || '-'
    }
  }
}
</script>

<style scoped>
.task-dependency-panel { display: flex; flex-direction: column; gap: 16px; }
.panel-card, .dependency-card { border: 1px solid var(--it-border); border-radius: 12px; background: var(--it-surface); padding: 12px; }
.row-title { font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 12px; }
.create-row { display: flex; gap: 8px; flex-wrap: wrap; }
.task-select { flex: 1; min-width: 240px; }
.type-select { width: 140px; }
.dependency-list { display: flex; flex-direction: column; gap: 12px; }
.dependency-card { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.dependency-title { font-weight: 600; color: #303133; }
.dependency-meta { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; font-size: 12px; color: #909399; }
.empty-wrap { padding: 20px 12px; }
</style>