<template>
  <div class="task-checklist-panel">
    <div class="panel-card">
      <div class="top-row">
        <div class="progress-info">
          <div class="title">检查项</div>
          <div class="desc">已完成 {{ checkedCount }} / {{ items.length }}</div>
        </div>
        <el-progress :percentage="progressPercent" :stroke-width="10" />
      </div>

      <div class="create-row">
        <el-input v-model="draft" size="small" placeholder="新增检查项内容" @keyup.enter.native="addItem" />
        <el-button type="primary" size="small" :loading="submitting" @click="addItem">新增</el-button>
      </div>
    </div>

    <div v-if="!items.length" class="panel-card empty-wrap">
      <el-empty description="暂无检查项" />
    </div>

    <div v-else class="item-list">
      <div v-for="(item, index) in items" :key="item.id" class="item-card">
        <div class="item-main">
          <el-checkbox :value="!!item.checked" @change="toggleItem(item)"></el-checkbox>
          <el-input v-model="item.content" size="small" class="content-input" />
        </div>
        <div class="item-actions">
          <el-tag v-if="item.checked" size="mini" type="success">已完成</el-tag>
          <el-button size="mini" @click="moveUp(index)" :disabled="index === 0">上移</el-button>
          <el-button size="mini" @click="moveDown(index)" :disabled="index === items.length - 1">下移</el-button>
          <el-button size="mini" type="primary" plain @click="saveItem(item)">保存</el-button>
          <el-button size="mini" type="danger" plain @click="removeItem(item)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  createTaskChecklistItem,
  deleteTaskChecklistItem,
  getTaskChecklist,
  sortTaskChecklistItems,
  toggleTaskChecklistItem,
  updateTaskChecklistItem
} from '@/api/project'

export default {
  name: 'TaskChecklist',
  props: {
    taskId: {
      type: Number,
      default: null
    },
    task: {
      type: Object,
      default: function() {
        return null
      }
    }
  },
  data() {
    return {
      items: [],
      draft: '',
      submitting: false,
      sortSaving: false
    }
  },
  computed: {
    checkedCount() {
      return this.items.filter(item => !!item.checked).length
    },
    progressPercent() {
      if (!this.items.length) return 0
      return Math.round((this.checkedCount / this.items.length) * 100)
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) this.loadChecklist()
        else this.items = []
      }
    }
  },
  methods: {
    normalizeItems(list) {
      return (Array.isArray(list) ? list : []).map((item, index) => ({
        ...item,
        checked: !!(item.checked || item.isChecked),
        content: item.content || '',
        sortOrder: typeof item.sortOrder === 'number' ? item.sortOrder : index + 1
      }))
    },
    async loadChecklist() {
      if (!this.taskId) return
      const res = await getTaskChecklist(this.taskId)
      this.items = this.normalizeItems(res && res.data)
    },
    async addItem() {
      const content = (this.draft || '').trim()
      if (!content) {
        this.$message.warning('请输入检查项内容')
        return
      }
      this.submitting = true
      try {
        await createTaskChecklistItem(this.taskId, { content, sortOrder: this.items.length + 1 })
        this.draft = ''
        await this.loadChecklist()
        this.$emit('changed')
        this.$message.success('新增成功')
      } finally {
        this.submitting = false
      }
    },
    async saveItem(item) {
      const content = (item.content || '').trim()
      if (!content) {
        this.$message.warning('检查项内容不能为空')
        return
      }
      await updateTaskChecklistItem(item.id, { content, sortOrder: item.sortOrder })
      this.$emit('changed')
      this.$message.success('保存成功')
    },
    async toggleItem(item) {
      await toggleTaskChecklistItem(item.id)
      await this.loadChecklist()
      this.$emit('changed')
      this.$message.success('状态已更新')
    },
    async removeItem(item) {
      try {
        await this.$confirm('确认删除该检查项吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      await deleteTaskChecklistItem(item.id)
      await this.loadChecklist()
      this.$emit('changed')
      this.$message.success('删除成功')
    },
    moveUp(index) {
      if (index <= 0) return
      const list = this.items.slice()
      const current = list[index]
      list.splice(index, 1)
      list.splice(index - 1, 0, current)
      this.items = this.resetSortOrder(list)
      this.persistSort()
    },
    moveDown(index) {
      if (index >= this.items.length - 1) return
      const list = this.items.slice()
      const current = list[index]
      list.splice(index, 1)
      list.splice(index + 1, 0, current)
      this.items = this.resetSortOrder(list)
      this.persistSort()
    },
    resetSortOrder(list) {
      return list.map((item, index) => ({ ...item, sortOrder: index + 1 }))
    },
    async persistSort() {
      if (this.sortSaving || !this.taskId) return
      this.sortSaving = true
      try {
        await sortTaskChecklistItems(this.taskId, {
          taskId: this.taskId,
          items: this.items.map(item => ({ id: item.id, sortOrder: item.sortOrder }))
        })
        this.$emit('changed')
      } finally {
        this.sortSaving = false
      }
    }
  }
}
</script>

<style scoped>
.task-checklist-panel { display: flex; flex-direction: column; gap: 16px; }
.panel-card, .item-card { border: 1px solid #ebeef5; border-radius: 12px; background: #fff; padding: 12px; }
.top-row { display: flex; flex-direction: column; gap: 12px; }
.title { font-size: 16px; font-weight: 600; color: #303133; }
.desc { font-size: 13px; color: #909399; margin-top: 2px; }
.create-row { display: flex; gap: 8px; margin-top: 12px; }
.item-list { display: flex; flex-direction: column; gap: 12px; }
.item-main { display: flex; align-items: center; gap: 10px; }
.content-input { flex: 1; }
.item-actions { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
.empty-wrap { padding: 20px 12px; }
</style>
