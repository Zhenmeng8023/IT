<template>
  <div>
    <el-card shadow="never">
      <div slot="header" class="card-header task-card-header">
        <div class="task-header-left">
          <span>任务协作面板</span>
          <span class="task-header-desc">按状态、优先级、负责人快速筛选，并直接在列表中更新状态和负责人。</span>
        </div>
        <div class="toolbar-actions">
          <el-input v-model="taskFilter.keyword" size="small" clearable placeholder="搜索任务标题或描述" class="toolbar-input"></el-input>
          <el-select v-model="taskFilter.status" size="small" placeholder="状态" class="toolbar-select">
            <el-option label="全部状态" value="all"></el-option>
            <el-option label="待处理" value="todo"></el-option>
            <el-option label="进行中" value="in_progress"></el-option>
            <el-option label="已完成" value="done"></el-option>
          </el-select>
          <el-select v-model="taskFilter.priority" size="small" placeholder="优先级" class="toolbar-select">
            <el-option label="全部优先级" value="all"></el-option>
            <el-option label="低" value="low"></el-option>
            <el-option label="中" value="medium"></el-option>
            <el-option label="高" value="high"></el-option>
            <el-option label="紧急" value="urgent"></el-option>
          </el-select>
          <el-select v-model="taskFilter.assigneeId" size="small" placeholder="负责人" class="toolbar-select toolbar-select-wide">
            <el-option label="全部负责人" value="all"></el-option>
            <el-option label="未分配" value="unassigned"></el-option>
            <el-option v-for="member in taskAssigneeOptions" :key="member.userId" :label="member.name" :value="member.userId"></el-option>
          </el-select>
          <el-select v-model="taskFilter.sortBy" size="small" placeholder="排序字段" class="toolbar-select">
            <el-option label="最近更新" value="updatedAt"></el-option>
            <el-option label="创建时间" value="createdAt"></el-option>
            <el-option label="截止时间" value="dueDate"></el-option>
            <el-option label="优先级" value="priority"></el-option>
            <el-option label="标题" value="title"></el-option>
          </el-select>
          <el-select v-model="taskFilter.sortOrder" size="small" placeholder="排序方向" class="toolbar-select">
            <el-option label="倒序" value="desc"></el-option>
            <el-option label="正序" value="asc"></el-option>
          </el-select>
          <el-button size="small" @click="taskFilter.assigneeId = currentUserId || 'all'">只看我的</el-button>
          <el-button size="small" @click="taskFilter.status = 'todo'">只看待办</el-button>
          <el-button size="small" @click="taskFilter.status = 'in_progress'">只看进行中</el-button>
          <el-button size="small" icon="el-icon-refresh" @click="loadTasks">刷新</el-button>
          <el-button size="small" @click="resetTaskFilters">重置</el-button>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreateTaskDialog">新建任务</el-button>
        </div>
      </div>

      <template v-if="canSeeTaskCollaboration">
        <div class="task-summary-grid">
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.total }}</div>
            <div class="task-summary-label">当前列表任务</div>
          </div>
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.todo }}</div>
            <div class="task-summary-label">待处理</div>
          </div>
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.inProgress }}</div>
            <div class="task-summary-label">进行中</div>
          </div>
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.done }}</div>
            <div class="task-summary-label">已完成</div>
          </div>
          <div class="task-summary-card danger">
            <div class="task-summary-value">{{ taskStats.overdue }}</div>
            <div class="task-summary-label">已逾期</div>
          </div>
        </div>

        <el-table :data="filteredTasks" border>
          <el-table-column label="任务信息" min-width="280">
            <template slot-scope="scope">
              <div class="task-title-cell">
                <div class="task-title-main">
                  <el-button type="text" class="task-title-link" @click="openTaskCollab(scope.row, 'comment')">{{ scope.row.title }}</el-button>
                  <el-tag v-if="isTaskOverdue(scope.row)" size="mini" type="danger" effect="plain">已逾期</el-tag>
                </div>
                <div v-if="scope.row.description" class="task-title-desc">{{ scope.row.description }}</div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="负责人" width="190">
            <template slot-scope="scope">
              <div class="task-assignee-cell">
                <el-avatar :size="30" :src="scope.row.assigneeAvatar || ''">{{ (scope.row.assigneeName || '未').slice(0, 1) }}</el-avatar>
                <div class="task-assignee-text">
                  <div class="task-assignee-name">{{ scope.row.assigneeName || '未分配' }}</div>
                  <div v-if="scope.row.creatorName" class="task-assignee-meta">创建人：{{ scope.row.creatorName }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="priority" label="优先级" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getTaskPriorityType(scope.row.priority)">{{ getTaskPriorityText(scope.row.priority) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getTaskStatusType(scope.row.status)">{{ getTaskStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="dueDate" label="截止时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.dueDate) || '-' }}</template>
          </el-table-column>

          <el-table-column label="快捷更新" min-width="260">
            <template slot-scope="scope">
              <div class="task-quick-actions">
                <el-select size="mini" :value="scope.row.status" class="task-inline-select" :disabled="!canQuickUpdateTaskStatus(scope.row)" @change="changeTaskStatus(scope.row.id, $event)">
                  <el-option label="待处理" value="todo"></el-option>
                  <el-option label="进行中" value="in_progress"></el-option>
                  <el-option label="已完成" value="done"></el-option>
                </el-select>
                <el-select size="mini" :value="scope.row.assigneeId" placeholder="切换负责人" class="task-inline-select" :disabled="!taskAssigneeOptions.length || !canQuickUpdateTaskAssignee(scope.row)" @change="changeTaskAssignee(scope.row.id, $event)">
                  <el-option v-for="member in taskAssigneeOptions" :key="'quick-' + member.userId" :label="member.name" :value="member.userId"></el-option>
                </el-select>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="220" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" plain @click="openTaskCollab(scope.row, 'comment')">协作详情</el-button>
              <el-button v-if="canEditTaskRow(scope.row)" size="mini" @click="openEditTaskDialog(scope.row)">编辑</el-button>
              <el-button v-if="canDeleteTaskRow(scope.row)" size="mini" type="danger" @click="deleteTask(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="filteredTasks.length === 0" description="当前筛选条件下没有任务"></el-empty>
      </template>

      <el-empty v-else description="只有已加入项目的成员才能参与任务协作"></el-empty>
    </el-card>

    <ProjectTaskCollabDrawer
      :visible.sync="taskCollabDrawerVisible"
      :task="selectedTaskForCollab"
      :project-id="projectId"
      :current-user-id="currentUserId"
      :current-member-joined-at="currentMemberRecord && currentMemberRecord.joinedAt ? currentMemberRecord.joinedAt : ''"
      :can-manage-project="canManageProject"
      :active-tab.sync="taskCollabActiveTab"
      :refresh-seed="taskCollabRefreshSeed"
      @changed="handleTaskCollabChanged"
      @close="handleTaskCollabDrawerClosed"
    />

    <el-dialog :title="taskDialogTitle" :visible.sync="taskDialogVisible" width="600px" @close="resetTaskForm">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="任务标题"><el-input v-model="taskForm.title" placeholder="请输入任务标题"></el-input></el-form-item>
        <el-form-item label="任务描述"><el-input v-model="taskForm.description" type="textarea" :rows="4"></el-input></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="taskForm.assigneeId" style="width: 100%" :disabled="taskDialogType === 'edit' && !canManageProject">
            <el-option v-for="member in members" :key="member.userId" :label="member.name" :value="member.userId"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority" style="width: 100%">
            <el-option label="低" value="low"></el-option>
            <el-option label="中" value="medium"></el-option>
            <el-option label="高" value="high"></el-option>
            <el-option label="紧急" value="urgent"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="taskForm.status" style="width: 100%" :disabled="taskDialogType === 'edit' && taskDialogOriginalTask && taskDialogOriginalTask.status === 'done' && !canManageProject">
            <el-option label="待处理" value="todo"></el-option>
            <el-option label="进行中" value="in_progress"></el-option>
            <el-option label="已完成" value="done"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="taskForm.dueDate" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择截止时间" style="width: 100%"></el-date-picker>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  createTask,
  deleteTask as apiDeleteTask,
  listProjectTasks,
  updateTask,
  updateTaskStatus
} from '@/api/project'
import ProjectTaskCollabDrawer from '../../components/ProjectTaskCollabDrawer.vue'
import {
  formatBackendDateTime,
  formatTime,
  getTaskPriorityText,
  getTaskPriorityType,
  getTaskStatusText,
  getTaskStatusType,
  normalizeTask
} from '../services/projectManageShared'

export default {
  name: 'ProjectManageTaskTab',
  components: {
    ProjectTaskCollabDrawer
  },
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    members: {
      type: Array,
      default: () => []
    },
    currentUserId: {
      type: Number,
      default: null
    },
    currentMemberRecord: {
      type: Object,
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    canSeeTaskCollaboration: {
      type: Boolean,
      default: false
    },
    refreshSeed: {
      type: Number,
      default: 0
    },
    initialMineOnly: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      tasks: [],
      taskFilter: { keyword: '', status: 'all', priority: 'all', assigneeId: 'all', sortBy: 'updatedAt', sortOrder: 'desc' },
      taskDialogVisible: false,
      taskDialogType: 'create',
      taskForm: { id: null, title: '', description: '', assigneeId: null, status: 'todo', priority: 'medium', dueDate: '' },
      taskDialogOriginalTask: null,
      taskCollabDrawerVisible: false,
      selectedTaskForCollab: null,
      taskCollabActiveTab: 'comment',
      taskCollabRefreshSeed: 0
    }
  },
  computed: {
    filteredTasks() {
      const list = this.tasks.filter(task => {
        const keyword = (this.taskFilter.keyword || '').trim().toLowerCase()
        const matchesKeyword = !keyword || (task.title || '').toLowerCase().includes(keyword) || (task.description || '').toLowerCase().includes(keyword)
        const matchesStatus = !this.taskFilter.status || this.taskFilter.status === 'all' || task.status === this.taskFilter.status
        const matchesPriority = !this.taskFilter.priority || this.taskFilter.priority === 'all' || task.priority === this.taskFilter.priority
        const assigneeFilter = this.taskFilter.assigneeId
        const matchesAssignee = !assigneeFilter || assigneeFilter === 'all' ? true : assigneeFilter === 'unassigned' ? !task.assigneeId : Number(task.assigneeId) === Number(assigneeFilter)
        return matchesKeyword && matchesStatus && matchesPriority && matchesAssignee
      })
      const order = this.taskFilter.sortOrder === 'asc' ? 1 : -1
      const priorityWeight = { low: 1, medium: 2, high: 3, urgent: 4 }
      return list.slice().sort((a, b) => {
        const sortBy = this.taskFilter.sortBy || 'updatedAt'
        if (sortBy === 'priority') return ((priorityWeight[a.priority] || 0) - (priorityWeight[b.priority] || 0)) * order
        if (sortBy === 'title') return String(a.title || '').localeCompare(String(b.title || ''), 'zh-CN') * order
        const av = new Date(a[sortBy] || 0).getTime() || 0
        const bv = new Date(b[sortBy] || 0).getTime() || 0
        return (av - bv) * order
      })
    },
    taskStats() {
      const list = this.filteredTasks
      return {
        total: list.length,
        todo: list.filter(task => task.status === 'todo').length,
        inProgress: list.filter(task => task.status === 'in_progress').length,
        done: list.filter(task => task.status === 'done').length,
        overdue: list.filter(task => this.isTaskOverdue(task)).length
      }
    },
    taskAssigneeOptions() {
      return this.members.filter(member => member && member.userId).map(member => ({
        userId: Number(member.userId),
        name: member.name || member.username || `用户${member.userId}`,
        avatar: member.avatar || ''
      }))
    },
    taskDialogTitle() {
      return this.taskDialogType === 'create' ? '新建任务' : '编辑任务'
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.applyInitialMineOnly()
        this.loadTasks()
      }
    },
    refreshSeed() {
      this.loadTasks()
    },
    canSeeTaskCollaboration() {
      this.applyInitialMineOnly()
      this.loadTasks()
    },
    initialMineOnly() {
      this.applyInitialMineOnly()
    }
  },
  methods: {
    formatTime,
    getTaskPriorityText,
    getTaskPriorityType,
    getTaskStatusText,
    getTaskStatusType,
    applyInitialMineOnly() {
      if (this.initialMineOnly && this.currentUserId) {
        this.taskFilter.assigneeId = this.currentUserId
      } else if (this.taskFilter.assigneeId === null || this.taskFilter.assigneeId === undefined) {
        this.taskFilter.assigneeId = 'all'
      }
    },
    async loadTasks() {
      if (!this.projectId || !this.canSeeTaskCollaboration) {
        this.tasks = []
        this.$emit('task-count-change', 0)
        return
      }
      try {
        const response = await listProjectTasks(this.projectId)
        this.tasks = (response.data || []).map(normalizeTask)
        this.$emit('task-count-change', this.tasks.length)
      } catch (error) {
        this.tasks = []
        this.$emit('task-count-change', 0)
        this.$message.error(error.response?.data?.message || '加载任务列表失败')
      }
    },
    getComparableTaskCycleTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },
    isHistoricalDoneTaskForCurrentUser(task) {
      if (!task || task.status !== 'done' || this.canManageProject) return false
      if (this.currentUserId === null || this.currentUserId === undefined) return false
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      const completedCycle = this.getComparableTaskCycleTime(task.completedMemberJoinedAt)
      const currentCycle = this.getComparableTaskCycleTime(this.currentMemberRecord && this.currentMemberRecord.joinedAt)
      if (!completedCycle || !currentCycle) return false
      return completedCycle !== currentCycle
    },
    getTaskReopenBlockedReason(task, targetStatus = 'todo') {
      if (!task) return '任务不存在'
      if (this.canManageProject) return ''
      if (!this.canSeeTaskCollaboration) return '加入项目后才可参与任务协作'
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return '只有当前负责人、管理员或所有者可以提交重开申请'
      if (task.status !== 'done' || targetStatus === 'done') return ''
      if (task.hasPendingReopenRequest) return '该任务已有待处理的重开申请，请勿重复提交'
      if (this.isHistoricalDoneTaskForCurrentUser(task)) return '你不能修改上一轮已完成的任务，请联系项目管理员或所有者处理'
      return ''
    },
    canEditTaskRow(task) {
      if (!task) return false
      if (this.canManageProject) return true
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      if (task.status === 'done') return false
      return !this.isHistoricalDoneTaskForCurrentUser(task)
    },
    canDeleteTaskRow(task) {
      return !!task && this.canManageProject
    },
    canQuickUpdateTaskStatus(task) {
      if (!task) return false
      if (this.canManageProject) return true
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      if (task.status === 'done') return !this.getTaskReopenBlockedReason(task, 'todo')
      return true
    },
    canQuickUpdateTaskAssignee(task) {
      return !!task && this.canManageProject
    },
    openTaskCollab(task, tab = 'comment') {
      if (!task || !task.id) return
      const latestTask = this.tasks.find(item => Number(item.id) === Number(task.id)) || task
      this.selectedTaskForCollab = { ...latestTask }
      this.taskCollabActiveTab = tab
      this.taskCollabDrawerVisible = true
      this.taskCollabRefreshSeed += 1
    },
    async handleTaskCollabChanged() {
      await this.loadTasks()
      if (this.selectedTaskForCollab && this.selectedTaskForCollab.id) {
        const latestTask = this.tasks.find(item => Number(item.id) === Number(this.selectedTaskForCollab.id))
        if (latestTask) this.selectedTaskForCollab = { ...latestTask }
      }
      this.taskCollabRefreshSeed += 1
      this.$emit('request-refresh')
    },
    handleTaskCollabDrawerClosed() {
      this.selectedTaskForCollab = null
      this.taskCollabActiveTab = 'comment'
    },
    openCreateTaskDialog() {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      this.taskDialogType = 'create'
      this.taskDialogOriginalTask = null
      this.resetTaskForm()
      this.taskDialogVisible = true
    },
    openEditTaskDialog(task) {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.canManageProject && task && task.status === 'done') {
        const blockedReason = this.getTaskReopenBlockedReason(task, 'todo')
        if (blockedReason) {
          this.$message.error(blockedReason)
          return
        }
        this.$message.warning('已完成任务不能直接编辑，如需改回未完成请先提交重开申请')
        this.openTaskCollab(task, 'reopen')
        return
      }
      if (!this.canEditTaskRow(task)) {
        this.$message.warning('只有当前负责人、管理员或所有者可以编辑任务')
        return
      }
      this.taskDialogType = 'edit'
      this.taskDialogOriginalTask = { ...task }
      this.taskForm = { id: task.id, title: task.title, description: task.description, assigneeId: task.assigneeId, status: task.status, priority: task.priority, dueDate: task.dueDate || '' }
      this.taskDialogVisible = true
    },
    resetTaskForm() {
      this.taskForm = { id: null, title: '', description: '', assigneeId: this.members[0] ? this.members[0].userId : null, status: 'todo', priority: 'medium', dueDate: '' }
      this.taskDialogOriginalTask = null
    },
    async submitTask() {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.taskForm.title || !this.taskForm.assigneeId) {
        this.$message.warning('请填写完整信息')
        return
      }
      const payload = {
        title: this.taskForm.title,
        description: this.taskForm.description,
        assigneeId: Number(this.taskForm.assigneeId),
        priority: this.taskForm.priority,
        dueDate: formatBackendDateTime(this.taskForm.dueDate)
      }
      try {
        if (this.taskDialogType === 'create') {
          const response = await createTask({ projectId: Number(this.projectId), ...payload })
          const createdTaskId = response?.data?.id
          if (createdTaskId && this.taskForm.status && this.taskForm.status !== 'todo') {
            await updateTaskStatus(createdTaskId, { status: this.taskForm.status })
          }
          this.$message.success('任务创建成功')
        } else {
          const originalTask = this.taskDialogOriginalTask || this.tasks.find(item => Number(item.id) === Number(this.taskForm.id))
          if (!originalTask) {
            this.$message.error('原任务不存在或已被删除')
            return
          }
          if (!this.canManageProject && Number(originalTask.assigneeId) !== Number(this.currentUserId)) {
            this.$message.error('只有当前负责人、管理员或所有者可以编辑任务')
            return
          }
          if (!this.canManageProject && originalTask.status === 'done') {
            this.$message.warning('已完成任务不能直接编辑，如需改回未完成请先提交重开申请')
            this.taskDialogVisible = false
            this.openTaskCollab(originalTask, 'reopen')
            return
          }
          const updatePayload = {
            title: payload.title,
            description: payload.description,
            priority: payload.priority,
            dueDate: payload.dueDate
          }
          if (this.canManageProject) {
            updatePayload.assigneeId = payload.assigneeId
          }
          await updateTask(this.taskForm.id, updatePayload)
          if (this.taskForm.status !== originalTask.status) {
            if (!this.canManageProject && originalTask.status === 'done' && this.taskForm.status !== 'done') {
              const blockedReason = this.getTaskReopenBlockedReason(originalTask, this.taskForm.status)
              if (blockedReason) {
                this.$message.error(blockedReason)
                return
              }
              await this.submitTaskReopenRequest(originalTask, this.taskForm.status)
            } else {
              await updateTaskStatus(this.taskForm.id, { status: this.taskForm.status })
            }
          }
          this.$message.success('任务更新成功')
        }
        this.taskDialogVisible = false
        await this.loadTasks()
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '任务保存失败')
      }
    },
    async deleteTask(taskId) {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      try {
        await this.$confirm('确定删除该任务吗？', '提示', { type: 'warning' })
        await apiDeleteTask(taskId)
        this.$message.success('删除成功')
        await this.loadTasks()
        this.$emit('request-refresh')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }
    },
    async submitTaskReopenRequest(task, targetStatus) {
      const blockedReason = this.getTaskReopenBlockedReason(task, targetStatus)
      if (blockedReason) {
        this.$message.error(blockedReason)
        return false
      }
      const { value } = await this.$prompt('请填写重开原因，管理员或所有者确认后才会把任务改回未完成。', '提交重开申请', {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：验收发现遗漏、联调失败、完成结论需要撤回等',
        inputValidator: inputValue => {
          if (!String(inputValue || '').trim()) return '请填写重开原因'
          if (String(inputValue || '').trim().length < 2) return '重开原因至少 2 个字'
          return true
        }
      })
      await this.$axios({
        url: `/api/project/task/${task.id}/reopen-requests`,
        method: 'post',
        data: { targetStatus, reason: String(value || '').trim() }
      })
      this.$message.success('已提交重开申请，请等待管理员或所有者确认')
      this.openTaskCollab(task, 'reopen')
      await this.loadTasks()
      this.$emit('request-refresh')
      return true
    },
    async changeTaskStatus(taskId, status) {
      const task = this.tasks.find(item => Number(item.id) === Number(taskId))
      if (!task || !status || task.status === status) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.canManageProject && Number(task.assigneeId) !== Number(this.currentUserId)) {
        this.$message.error('只有当前负责人、管理员或所有者可以修改任务状态')
        return
      }
      if (!this.canManageProject && task.status === 'done' && status !== 'done') {
        const blockedReason = this.getTaskReopenBlockedReason(task, status)
        if (blockedReason) {
          this.$message.error(blockedReason)
          return
        }
        try {
          await this.submitTaskReopenRequest(task, status)
        } catch (error) {
          if (error !== 'cancel' && error !== 'close') {
            this.$message.error(error.response?.data?.message || '提交重开申请失败')
          }
        }
        return
      }
      try {
        await updateTaskStatus(taskId, { status })
        this.$message.success('状态更新成功')
        await this.loadTasks()
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '状态更新失败')
      }
    },
    async changeTaskAssignee(taskId, assigneeId) {
      if (!assigneeId) {
        this.$message.warning('请选择负责人')
        return
      }
      if (!this.canManageProject) {
        this.$message.error('只有管理员或所有者可以修改任务负责人')
        return
      }
      try {
        await updateTask(taskId, { assigneeId: Number(assigneeId) })
        this.$message.success('负责人更新成功')
        await this.loadTasks()
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '负责人更新失败')
      }
    },
    resetTaskFilters() {
      this.taskFilter = {
        keyword: '',
        status: 'all',
        priority: 'all',
        assigneeId: this.initialMineOnly && this.currentUserId ? this.currentUserId : 'all',
        sortBy: 'updatedAt',
        sortOrder: 'desc'
      }
    },
    isTaskOverdue(task) {
      if (!task || !task.dueDate || task.status === 'done') return false
      const dueDate = new Date(task.dueDate)
      return !Number.isNaN(dueDate.getTime()) && dueDate.getTime() < Date.now()
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.task-card-header { align-items: flex-start; }
.task-header-left { display: flex; flex-direction: column; gap: 4px; }
.task-header-desc { color: #909399; font-size: 12px; }
.toolbar-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar-input { width: 220px; }
.toolbar-select { width: 140px; }
.toolbar-select-wide { width: 180px; }
.task-summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.task-summary-card { padding: 14px 16px; border-radius: 12px; background: var(--it-surface-muted); border: 1px solid var(--it-border); }
.task-summary-card.danger { background: var(--it-danger-soft); border-color: var(--it-danger); }
.task-summary-value { font-size: 24px; font-weight: 700; color: #303133; }
.task-summary-label { margin-top: 6px; font-size: 12px; color: #909399; }
.task-title-cell { display: flex; flex-direction: column; gap: 6px; }
.task-title-main { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.task-title-link { padding: 0; color: #303133; font-weight: 600; }
.task-title-link:hover { color: #409eff; }
.task-title-desc { color: #909399; font-size: 12px; line-height: 1.5; }
.task-assignee-cell { display: flex; align-items: center; gap: 10px; }
.task-assignee-text { min-width: 0; }
.task-assignee-name { color: #303133; font-weight: 600; }
.task-assignee-meta { color: #909399; font-size: 12px; }
.task-quick-actions { display: flex; flex-direction: column; gap: 8px; }
.task-inline-select { width: 100%; }

@media (max-width: 768px) {
  .toolbar-input, .toolbar-select, .toolbar-select-wide { width: 100%; }
  .task-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 480px) {
  .task-summary-grid { grid-template-columns: 1fr; }
}
</style>
