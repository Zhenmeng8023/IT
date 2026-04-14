<template>
  <div class="project-activity-manage-panel">
    <el-card shadow="never" class="activity-filter-card">
      <div slot="header" class="panel-header">
        <div>
          <div class="panel-title">项目活动流</div>
          <div class="panel-subtitle">支持筛选、分页查询、从项目详情时间线深链定位到指定活动，并进一步跳到关联对象。</div>
        </div>
        <div class="panel-actions">
          <el-button size="small" icon="el-icon-refresh" @click="loadPage(false)">刷新</el-button>
        </div>
      </div>

      <div class="filter-row">
        <el-select v-model="filters.action" size="small" clearable placeholder="动作类型" @change="handleFilterChange">
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select v-model="filters.targetType" size="small" clearable placeholder="对象类型" @change="handleFilterChange">
          <el-option label="任务" value="task" />
          <el-option label="文档" value="doc" />
          <el-option label="文件" value="file" />
          <el-option label="成员" value="member" />
          <el-option label="邀请" value="invitation" />
          <el-option label="加入申请" value="join_request" />
          <el-option label="合并请求" value="merge_request" />
          <el-option label="模板" value="template" />
          <el-option label="项目" value="project" />
        </el-select>

        <el-select v-model="filters.operatorId" size="small" clearable filterable placeholder="操作人" @change="handleFilterChange">
          <el-option v-for="item in finalOperatorOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-date-picker
          v-model="filters.dateRange"
          size="small"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="yyyy-MM-dd HH:mm:ss"
          @change="handleFilterChange"
        />

        <el-button size="small" @click="clearFilters">清空筛选</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="activity-list-card">
      <div v-if="locating" class="state-wrap locating-wrap">
        <i class="el-icon-loading" />
        <span>正在定位目标活动...</span>
      </div>

      <div v-else-if="loading" class="state-wrap loading-wrap">
        <i class="el-icon-loading" />
        <span>活动流加载中...</span>
      </div>

      <div v-else-if="!activities.length" class="state-wrap empty-wrap">
        <el-empty description="暂无项目活动" />
      </div>

      <div v-else class="activity-list">
        <div
          v-for="item in activities"
          :id="`activity-row-${item.id}`"
          :key="item.id"
          class="activity-row"
          :class="{
            'is-highlight': Number(highlightActivityId) === Number(item.id),
            'is-clicking': Number(activeActivityId) === Number(item.id)
          }"
          @click="handleActivityClick(item)"
        >
          <div class="row-left">
            <div class="row-dot" :class="resolveActionClass(item)">
              <i :class="resolveActionIcon(item)" />
            </div>
          </div>
          <div class="row-main">
            <div class="row-top">
              <div class="row-title-group">
                <div class="row-title">{{ item.actionLabel || item.action || '项目动态' }}</div>
                <el-tag size="mini" :type="resolveTagType(item)">{{ formatActionTag(item.action) }}</el-tag>
              </div>
              <div class="row-time">{{ formatDate(item.createdAt) }}</div>
            </div>

            <div class="row-operator">
              <div class="row-avatar">{{ firstChar(formatOperator(item)) }}</div>
              <div class="row-operator-text">
                <div class="row-operator-name">{{ formatOperator(item) }}</div>
                <div class="row-operator-desc">{{ formatTargetType(item.targetType) }}</div>
              </div>
            </div>

            <div class="row-content-wrap">
              <div class="row-content-title">动态内容</div>
              <div class="row-content">{{ item.content || '暂无描述' }}</div>
            </div>

            <div class="row-footer-actions">
              <el-button v-if="canJumpTarget(item)" size="mini" type="text" @click.stop="goToTarget(item)">查看关联对象</el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { pageProjectActivities, getProjectActivityPosition } from '@/api/projectActivity'

export default {
  name: 'ProjectActivityManagePanel',
  props: {
    projectId: {
      type: [Number, String],
      default: null
    },
    initialActivityId: {
      type: [Number, String],
      default: null
    },
    memberOptions: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      loading: false,
      locating: false,
      initialized: false,
      activities: [],
      page: 1,
      size: 10,
      total: 0,
      highlightActivityId: null,
      activeActivityId: null,
      filters: {
        action: '',
        targetType: '',
        operatorId: null,
        dateRange: []
      },
      actionOptions: [
        { label: '创建项目', value: 'create_project' },
        { label: '编辑项目', value: 'update_project' },
        { label: '保存模板', value: 'save_as_template' },
        { label: '套用模板', value: 'apply_template' },
        { label: '新增成员', value: 'add_member' },
        { label: '移除成员', value: 'remove_member' },
        { label: '退出项目', value: 'quit_project' },
        { label: '发起邀请', value: 'create_invite' },
        { label: '接受邀请', value: 'accept_invite' },
        { label: '取消邀请', value: 'cancel_invite' },
        { label: '提交加入申请', value: 'submit_join_request' },
        { label: '通过加入申请', value: 'approve_join_request' },
        { label: '拒绝加入申请', value: 'reject_join_request' },
        { label: '上传文件', value: 'upload_file' },
        { label: '删除文件', value: 'delete_file' },
        { label: '设主文件', value: 'set_main_file' },
        { label: '新建文档', value: 'create_doc' },
        { label: '更新文档', value: 'update_doc' },
        { label: '回滚文档', value: 'rollback_doc' },
        { label: '设为主文档', value: 'set_primary_doc' },
        { label: '删除文档', value: 'delete_doc' },
        { label: '创建任务', value: 'create_task' },
        { label: '更新任务', value: 'update_task' },
        { label: '修改任务状态', value: 'change_task_status' },
        { label: '删除任务', value: 'delete_task' },
        { label: '冲突处理记录', value: 'mr_conflict_resolve' },
        { label: '冲突解决开始', value: 'mr_conflict_resolve_start' },
        { label: '冲突策略已应用', value: 'mr_conflict_resolve_apply' },
        { label: '冲突自动重检', value: 'mr_conflict_resolve_recheck' },
        { label: '冲突解决失败', value: 'mr_conflict_resolve_fail' }
      ]
    }
  },
  computed: {
    operatorOptions() {
      const map = new Map()
      this.activities.forEach(item => {
        if (!item || item.operatorId == null) return
        if (!map.has(item.operatorId)) {
          map.set(item.operatorId, {
            value: item.operatorId,
            label: this.formatOperator(item)
          })
        }
      })
      return Array.from(map.values())
    },
    finalOperatorOptions() {
      if (Array.isArray(this.memberOptions) && this.memberOptions.length) {
        return this.memberOptions.map(item => ({
          value: Number(item.userId),
          label: item.name || item.username || `用户${item.userId}`
        }))
      }
      return this.operatorOptions
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(val) {
        if (!val) {
          this.activities = []
          this.total = 0
          return
        }
        this.initializeFromRoute()
      }
    },
    initialActivityId: {
      immediate: true,
      handler(val) {
        if (!this.initialized || !val) return
        this.locateActivity(val)
      }
    },
    '$route.query.activityId'(val) {
      if (this.$route.query.tab !== 'activity-manage') return
      if (!val) return
      this.locateActivity(val)
    },
    '$route.query.tab'(val) {
      if (val === 'activity-manage') {
        this.initializeFromRoute()
      }
    }
  },
  methods: {
    initializeFromRoute() {
      this.applyFiltersFromRoute()
      this.initialized = true
      const activityId = this.$route.query.activityId || this.initialActivityId
      if (activityId) {
        this.locateActivity(activityId)
      } else {
        this.loadPage(false)
      }
    },
    applyFiltersFromRoute() {
      const query = (this.$route && this.$route.query) || {}
      this.filters.action = query.action || ''
      this.filters.targetType = query.targetType || ''
      this.filters.operatorId = query.operatorId ? Number(query.operatorId) : null
      if (query.startTime && query.endTime) {
        this.filters.dateRange = [query.startTime, query.endTime]
      } else {
        this.filters.dateRange = []
      }
    },
    buildParams() {
      const params = {
        page: this.page,
        size: this.size
      }
      if (this.filters.action) params.action = this.filters.action
      if (this.filters.targetType) params.targetType = this.filters.targetType
      if (this.filters.operatorId !== null && this.filters.operatorId !== undefined && this.filters.operatorId !== '') {
        params.operatorId = this.filters.operatorId
      }
      if (Array.isArray(this.filters.dateRange) && this.filters.dateRange.length === 2) {
        params.startTime = this.filters.dateRange[0]
        params.endTime = this.filters.dateRange[1]
      }
      return params
    },
    buildPositionParams() {
      const params = { size: this.size }
      if (this.filters.action) params.action = this.filters.action
      if (this.filters.targetType) params.targetType = this.filters.targetType
      if (this.filters.operatorId !== null && this.filters.operatorId !== undefined && this.filters.operatorId !== '') {
        params.operatorId = this.filters.operatorId
      }
      if (Array.isArray(this.filters.dateRange) && this.filters.dateRange.length === 2) {
        params.startTime = this.filters.dateRange[0]
        params.endTime = this.filters.dateRange[1]
      }
      return params
    },
    syncFiltersToRoute() {
      const query = {
        ...this.$route.query,
        projectId: String(this.projectId),
        tab: 'activity-manage'
      }
      if (this.filters.action) query.action = this.filters.action
      else delete query.action
      if (this.filters.targetType) query.targetType = this.filters.targetType
      else delete query.targetType
      if (this.filters.operatorId !== null && this.filters.operatorId !== undefined && this.filters.operatorId !== '') {
        query.operatorId = String(this.filters.operatorId)
      } else {
        delete query.operatorId
      }
      if (Array.isArray(this.filters.dateRange) && this.filters.dateRange.length === 2) {
        query.startTime = this.filters.dateRange[0]
        query.endTime = this.filters.dateRange[1]
      } else {
        delete query.startTime
        delete query.endTime
      }
      if (!this.highlightActivityId) delete query.activityId
      this.$router.replace({ path: '/projectmanage', query }).catch(() => {})
    },
    normalizePagePayload(res) {
      const payload = res && res.data ? res.data : res
      if (payload && Array.isArray(payload.list)) {
        return {
          list: payload.list,
          total: Number(payload.total || 0),
          page: Number(payload.page || this.page || 1),
          size: Number(payload.size || this.size || 10)
        }
      }
      if (Array.isArray(payload)) {
        return {
          list: payload,
          total: payload.length,
          page: this.page,
          size: this.size
        }
      }
      return {
        list: [],
        total: 0,
        page: this.page,
        size: this.size
      }
    },
    async loadPage(reset = false) {
      if (!this.projectId) return
      if (reset) this.page = 1
      this.loading = true
      try {
        const res = await pageProjectActivities(this.projectId, this.buildParams())
        const data = this.normalizePagePayload(res)
        this.activities = data.list
        this.total = data.total
        this.page = data.page
        this.size = data.size
        this.$emit('total-change', this.total)
        this.syncFiltersToRoute()
        if (this.highlightActivityId) {
          this.$nextTick(() => {
            this.scrollToActivity(this.highlightActivityId)
          })
        }
      } catch (e) {
        this.activities = []
        this.total = 0
        const m = e.response?.data?.message || e.response?.data?.msg || '加载项目活动失败'
        this.$message.error(m)
      } finally {
        this.loading = false
      }
    },
    async locateActivity(activityId) {
      if (!this.projectId || !activityId) return
      this.locating = true
      try {
        const res = await getProjectActivityPosition(this.projectId, activityId, this.buildPositionParams())
        const payload = res && res.data ? res.data : res
        if (!payload || payload.exists === false) {
          this.page = 1
          this.highlightActivityId = null
          await this.loadPage(false)
          return
        }
        this.page = Number(payload.page || 1)
        this.highlightActivityId = Number(activityId)
        await this.loadPage(false)
      } catch (e) {
        this.page = 1
        this.highlightActivityId = null
        await this.loadPage(false)
      } finally {
        this.locating = false
      }
    },
    scrollToActivity(activityId) {
      if (!activityId) return
      const target = document.getElementById(`activity-row-${activityId}`)
      if (!target) return
      target.scrollIntoView({ behavior: 'smooth', block: 'center' })
      this.highlightActivity(activityId)
    },
    highlightActivity(activityId) {
      this.highlightActivityId = Number(activityId)
      window.clearTimeout(this._highlightTimer)
      this._highlightTimer = window.setTimeout(() => {
        this.highlightActivityId = null
      }, 2600)
    },
    handleFilterChange() {
      this.loadPage(true)
    },
    handlePageChange(page) {
      this.page = page
      this.loadPage(false)
    },
    handleSizeChange(size) {
      this.size = size
      this.page = 1
      this.loadPage(false)
    },
    clearFilters() {
      this.filters = {
        action: '',
        targetType: '',
        operatorId: null,
        dateRange: []
      }
      this.highlightActivityId = null
      this.page = 1
      this.syncFiltersToRoute()
      this.loadPage(false)
    },
    handleActivityClick(item) {
      if (!item || !item.id) return
      this.activeActivityId = Number(item.id)
      this.highlightActivity(item.id)
      this.$nextTick(() => {
        this.scrollToActivity(item.id)
      })
      window.clearTimeout(this._activeTimer)
      this._activeTimer = window.setTimeout(() => {
        this.activeActivityId = null
      }, 600)
    },
    buildTargetQuery(item) {
      if (!item || !item.targetType) return null
      const map = {
        task: {
          tab: 'task-manage',
          taskId: item.targetId ? String(item.targetId) : undefined
        },
        doc: {
          tab: 'doc-manage',
          docId: item.targetId ? String(item.targetId) : undefined
        },
        file: {
          tab: 'file-manage',
          fileId: item.targetId ? String(item.targetId) : undefined
        },
        member: {
          tab: 'member-manage'
        },
        invitation: {
          tab: 'member-manage',
          panel: 'invite'
        },
        join_request: {
          tab: 'member-manage',
          panel: 'join-request'
        },
        template: {
          tab: 'settings',
          settingsTab: 'template'
        },
        project: {
          tab: 'overview'
        }
      }
      return map[item.targetType] || { tab: 'overview' }
    },
    goToTarget(item) {
      const targetQuery = this.buildTargetQuery(item)
      if (!targetQuery) return
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          ...targetQuery
        }
      })
    },
    canJumpTarget(item) {
      return !!(item && item.targetType)
    },
    formatOperator(item) {
      return item.operatorName || ('用户#' + (item.operatorId || '-'))
    },
    formatTargetType(value) {
      const map = {
        task: '任务动态',
        doc: '文档动态',
        file: '文件动态',
        member: '成员动态',
        invitation: '邀请动态',
        join_request: '加入申请动态',
        merge_request: '合并请求动态',
        template: '模板动态',
        project: '项目动态'
      }
      return map[value] || '项目动态'
    },
    formatActionTag(action) {
      const map = {
        create_project: '创建',
        update_project: '更新',
        add_member: '成员',
        remove_member: '成员',
        quit_project: '成员',
        upload_file: '文件',
        delete_file: '文件',
        set_main_file: '主文件',
        create_doc: '文档',
        update_doc: '文档',
        rollback_doc: '回滚',
        set_primary_doc: '主文档',
        delete_doc: '文档',
        create_task: '任务',
        update_task: '任务',
        change_task_status: '状态',
        delete_task: '任务',
        save_as_template: '模板',
        apply_template: '模板',
        create_invite: '邀请',
        accept_invite: '邀请',
        cancel_invite: '邀请',
        submit_join_request: '申请',
        approve_join_request: '审批',
        reject_join_request: '审批'
        ,
        mr_conflict_resolve: '冲突',
        mr_conflict_resolve_start: '冲突',
        mr_conflict_resolve_apply: '冲突',
        mr_conflict_resolve_recheck: '重检',
        mr_conflict_resolve_fail: '失败'
      }
      return map[action] || '动态'
    },
    resolveActionClass(item) {
      const action = item && item.action
      const map = {
        create_project: 'is-create',
        create_doc: 'is-create',
        create_task: 'is-create',
        add_member: 'is-create',
        upload_file: 'is-create',
        save_as_template: 'is-create',
        apply_template: 'is-create',
        create_invite: 'is-create',
        accept_invite: 'is-create',
        approve_join_request: 'is-create',
        update_project: 'is-update',
        update_doc: 'is-update',
        update_task: 'is-update',
        set_main_file: 'is-priority',
        change_task_status: 'is-status',
        rollback_doc: 'is-status',
        set_primary_doc: 'is-priority',
        submit_join_request: 'is-status',
        remove_member: 'is-delete',
        quit_project: 'is-delete',
        delete_file: 'is-delete',
        delete_doc: 'is-delete',
        delete_task: 'is-delete',
        cancel_invite: 'is-delete',
        reject_join_request: 'is-delete'
        ,
        mr_conflict_resolve: 'is-create',
        mr_conflict_resolve_start: 'is-status',
        mr_conflict_resolve_apply: 'is-create',
        mr_conflict_resolve_recheck: 'is-status',
        mr_conflict_resolve_fail: 'is-delete'
      }
      return map[action] || 'is-default'
    },
    resolveActionIcon(item) {
      const action = item && item.action
      const map = {
        create_project: 'el-icon-circle-plus-outline',
        create_doc: 'el-icon-document-add',
        create_task: 'el-icon-s-claim',
        add_member: 'el-icon-user-solid',
        upload_file: 'el-icon-upload',
        save_as_template: 'el-icon-folder-add',
        apply_template: 'el-icon-copy-document',
        create_invite: 'el-icon-message',
        accept_invite: 'el-icon-circle-check',
        approve_join_request: 'el-icon-circle-check',
        update_project: 'el-icon-edit-outline',
        update_doc: 'el-icon-edit',
        update_task: 'el-icon-edit',
        set_main_file: 'el-icon-star-on',
        change_task_status: 'el-icon-refresh',
        rollback_doc: 'el-icon-refresh-left',
        set_primary_doc: 'el-icon-star-on',
        submit_join_request: 'el-icon-position',
        remove_member: 'el-icon-remove',
        quit_project: 'el-icon-close',
        delete_file: 'el-icon-delete',
        delete_doc: 'el-icon-delete',
        delete_task: 'el-icon-delete',
        cancel_invite: 'el-icon-close',
        reject_join_request: 'el-icon-close'
        ,
        mr_conflict_resolve: 'el-icon-document-checked',
        mr_conflict_resolve_start: 'el-icon-warning-outline',
        mr_conflict_resolve_apply: 'el-icon-check',
        mr_conflict_resolve_recheck: 'el-icon-refresh',
        mr_conflict_resolve_fail: 'el-icon-close'
      }
      return map[action] || 'el-icon-time'
    },
    resolveTagType(item) {
      const level = this.resolveActionClass(item)
      if (level === 'is-create') return 'success'
      if (level === 'is-delete') return 'danger'
      if (level === 'is-status' || level === 'is-priority') return 'warning'
      return 'info'
    },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString()
    },
    firstChar(value) {
      return value ? String(value).slice(0, 1) : 'U'
    }
  },
  beforeDestroy() {
    window.clearTimeout(this._highlightTimer)
    window.clearTimeout(this._activeTimer)
  }
}
</script>

<style scoped>
.project-activity-manage-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.panel-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.activity-list-card {
  min-height: 520px;
}
.state-wrap {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.activity-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 14px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  transition: all 0.25s ease;
}
.activity-row:hover {
  border-color: #d9ecff;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.08);
}
.activity-row.is-highlight {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
}
.row-left {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.row-dot {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}
.row-main {
  min-width: 0;
}
.row-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.row-title-group {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.row-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}
.row-time {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}
.row-operator {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.row-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.row-operator-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.row-operator-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #909399;
}
.row-content-wrap {
  margin-top: 14px;
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}
.row-content-title {
  font-size: 13px;
  font-weight: 700;
  color: #606266;
}
.row-content {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
  color: #303133;
  word-break: break-word;
}
.row-footer-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
.pagination-wrap {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
.is-create {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}
.is-update,
.is-default {
  background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);
}
.is-status {
  background: linear-gradient(135deg, #34d399 0%, #059669 100%);
}
.is-priority {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}
.is-delete {
  background: linear-gradient(135deg, #f87171 0%, #dc2626 100%);
}
@media (max-width: 768px) {
  .panel-header {
    flex-direction: column;
  }
  .filter-row > * {
    width: 100%;
  }
  .activity-row {
    grid-template-columns: 44px minmax(0, 1fr);
    padding: 14px;
  }
  .row-top {
    flex-direction: column;
  }
  .pagination-wrap {
    justify-content: flex-start;
    overflow-x: auto;
  }
  .row-footer-actions {
    justify-content: flex-start;
  }
}
</style>
