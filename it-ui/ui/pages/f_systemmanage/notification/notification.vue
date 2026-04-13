<template>
  <div class="admin-notification-page">
    <div class="page-header">
      <div>
        <h1>消息通知管理</h1>
        <p>管理全站通知、发送系统消息并维护通知模板</p>
      </div>
      <el-button type="primary" icon="el-icon-refresh" @click="refreshAll">刷新</el-button>
    </div>

    <div class="stat-grid">
      <div v-for="item in statCards" :key="item.key" class="stat-item">
        <span>{{ item.label }}</span>
        <strong>{{ stats[item.key] || 0 }}</strong>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="通知列表" name="list">
        <el-card shadow="never" class="search-card">
          <el-form :inline="true" :model="query" size="small">
            <el-form-item label="接收人ID"><el-input v-model="query.receiverId" clearable class="w120" /></el-form-item>
            <el-form-item label="发送人ID"><el-input v-model="query.senderId" clearable class="w120" /></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="query.category" clearable class="w120">
                <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="类型"><el-input v-model="query.type" clearable class="w150" /></el-form-item>
            <el-form-item label="已读">
              <el-select v-model="query.readStatus" clearable class="w120">
                <el-option label="未读" value="false" />
                <el-option label="已读" value="true" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务状态"><el-input v-model="query.businessStatus" clearable class="w120" /></el-form-item>
            <el-form-item label="时间">
              <el-date-picker v-model="query.timeRange" type="datetimerange" start-placeholder="开始时间" end-placeholder="结束时间" value-format="yyyy-MM-ddTHH:mm:ssZ" class="date-range" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="resetSearch">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never">
          <div class="table-toolbar">
            <el-button size="small" :disabled="!selectedIds.length" @click="batchRead">批量已读</el-button>
            <el-button size="small" type="danger" plain :disabled="!selectedIds.length" @click="batchDelete">批量删除</el-button>
          </div>
          <el-table v-loading="loading" :data="notifications" border stripe @selection-change="selection => selectedIds = selection.map(item => item.id)">
            <el-table-column type="selection" width="46" />
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="标题" min-width="180">
              <template slot-scope="scope">
                <span class="ellipsis strong">{{ scope.row.title || '消息通知' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="内容" min-width="240">
              <template slot-scope="scope"><span class="ellipsis">{{ scope.row.content }}</span></template>
            </el-table-column>
            <el-table-column prop="receiverId" label="接收人" width="90" />
            <el-table-column prop="senderName" label="发送人" width="120" />
            <el-table-column label="分类" width="90">
              <template slot-scope="scope"><el-tag size="mini">{{ categoryText(scope.row.category) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column label="已读" width="80">
              <template slot-scope="scope">
                <el-tag :type="scope.row.readStatus ? 'success' : 'danger'" size="mini">{{ scope.row.readStatus ? '已读' : '未读' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="businessStatus" label="业务状态" width="100" />
            <el-table-column label="时间" width="160">
              <template slot-scope="scope">{{ formatDate(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="viewDetail(scope.row)">详情</el-button>
                <el-button type="text" size="mini" :disabled="scope.row.readStatus" @click="markAdminRead(scope.row)">已读</el-button>
                <el-button type="text" size="mini" class="danger-text" @click="deleteAdminItem(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container">
            <el-pagination background layout="total, sizes, prev, pager, next, jumper" :current-page.sync="query.page" :page-size="query.size" :page-sizes="[10, 20, 50, 100]" :total="total" @size-change="handleSizeChange" @current-change="fetchNotifications" />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="发送系统通知" name="send">
        <el-card shadow="never">
          <el-form ref="sendForm" :model="sendForm" :rules="sendRules" label-width="100px" class="send-form">
            <el-form-item label="发送范围" prop="sendScope">
              <el-radio-group v-model="sendForm.sendScope">
                <el-radio label="all">全部用户</el-radio>
                <el-radio label="users">指定用户</el-radio>
                <el-radio label="roles">指定角色</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="sendForm.sendScope === 'users'" label="指定用户" prop="receiverIds">
              <el-select v-model="sendForm.receiverIds" multiple filterable remote reserve-keyword placeholder="输入用户名或昵称搜索" :remote-method="searchUsers" :loading="userSearching" class="full-width">
                <el-option v-for="user in userOptions" :key="user.id" :label="userLabel(user)" :value="user.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="sendForm.sendScope === 'roles'" label="指定角色" prop="roleIds">
              <el-select v-model="sendForm.roleIds" multiple class="full-width">
                <el-option label="超级管理员" :value="1" />
                <el-option label="管理员" :value="2" />
                <el-option label="普通用户" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="标题" prop="title"><el-input v-model="sendForm.title" maxlength="120" show-word-limit /></el-form-item>
            <el-form-item label="内容" prop="content"><el-input v-model="sendForm.content" type="textarea" :rows="5" maxlength="1000" show-word-limit /></el-form-item>
            <el-form-item label="优先级"><el-input-number v-model="sendForm.priority" :min="0" :max="9" /></el-form-item>
            <el-form-item label="跳转链接"><el-input v-model="sendForm.actionUrl" placeholder="/notifications 或业务页面路径" /></el-form-item>
            <el-form-item label="过期时间"><el-date-picker v-model="sendForm.expiresAt" type="datetime" value-format="yyyy-MM-ddTHH:mm:ssZ" placeholder="可选" /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="sending" @click="submitSystemNotification">发送通知</el-button>
              <el-button @click="resetSendForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="通知模板" name="template">
        <el-card shadow="never">
          <div class="table-toolbar">
            <el-input v-model="templateQuery.keyword" placeholder="搜索模板编码或标题" clearable class="template-search" @keyup.enter.native="fetchTemplates" />
            <el-button type="primary" icon="el-icon-plus" @click="openTemplateDialog()">新增模板</el-button>
          </div>
          <el-table v-loading="templateLoading" :data="templates" border stripe>
            <el-table-column prop="code" label="编码" min-width="150" />
            <el-table-column prop="category" label="分类" width="90" />
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column label="标题模板" min-width="180"><template slot-scope="scope"><span class="ellipsis">{{ scope.row.titleTemplate }}</span></template></el-table-column>
            <el-table-column label="内容模板" min-width="220"><template slot-scope="scope"><span class="ellipsis">{{ scope.row.contentTemplate }}</span></template></el-table-column>
            <el-table-column prop="defaultPriority" label="优先级" width="80" />
            <el-table-column label="启用" width="80">
              <template slot-scope="scope"><el-switch v-model="scope.row.enabled" @change="toggleTemplate(scope.row)" /></template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="openTemplateDialog(scope.row)">编辑</el-button>
                <el-button type="text" size="mini" class="danger-text" @click="removeTemplate(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container">
            <el-pagination background layout="total, prev, pager, next" :current-page.sync="templateQuery.page" :page-size="templateQuery.size" :total="templateTotal" @current-change="fetchTemplates" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog title="通知详情" :visible.sync="detailVisible" width="680px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题" :span="2">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">{{ current.content }}</el-descriptions-item>
        <el-descriptions-item label="接收人">{{ current.receiverId }}</el-descriptions-item>
        <el-descriptions-item label="发送人">{{ current.senderName || current.senderId || '系统' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ categoryText(current.category) }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.type }}</el-descriptions-item>
        <el-descriptions-item label="跳转链接" :span="2">{{ current.actionUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(current.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="已读状态">{{ current.readStatus ? '已读' : '未读' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog :title="templateForm.id ? '编辑模板' : '新增模板'" :visible.sync="templateDialogVisible" width="720px">
      <el-alert :title="templateVariableTip" type="info" show-icon :closable="false" class="template-tip" />
      <el-form ref="templateForm" :model="templateForm" :rules="templateRules" label-width="140px">
        <el-form-item label="code" prop="code"><el-input v-model="templateForm.code" /></el-form-item>
        <el-form-item label="category" prop="category"><el-input v-model="templateForm.category" /></el-form-item>
        <el-form-item label="type" prop="type"><el-input v-model="templateForm.type" /></el-form-item>
        <el-form-item label="titleTemplate" prop="titleTemplate"><el-input v-model="templateForm.titleTemplate" /></el-form-item>
        <el-form-item label="contentTemplate" prop="contentTemplate"><el-input v-model="templateForm.contentTemplate" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="actionUrlTemplate"><el-input v-model="templateForm.actionUrlTemplate" /></el-form-item>
        <el-form-item label="defaultPriority"><el-input-number v-model="templateForm.defaultPriority" :min="0" :max="9" /></el-form-item>
        <el-form-item label="enabled"><el-switch v-model="templateForm.enabled" /></el-form-item>
        <el-form-item label="remark"><el-input v-model="templateForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  batchDeleteAdminNotifications,
  batchMarkAdminNotificationsAsRead,
  createNotificationTemplate,
  deleteAdminNotification,
  deleteNotificationTemplate,
  getAdminNotificationStats,
  getAdminNotifications,
  getNotificationTemplates,
  markAdminNotificationAsRead,
  searchNotificationUsers,
  sendAdminSystemNotification,
  updateNotificationTemplate,
  updateNotificationTemplateEnabled
} from '@/api/notification'

export default {
  name: 'AdminNotificationManage',
  layout: 'manage',
  data() {
    return {
      activeTab: 'list',
      loading: false,
      templateLoading: false,
      sending: false,
      userSearching: false,
      notifications: [],
      templates: [],
      userOptions: [],
      selectedIds: [],
      total: 0,
      templateTotal: 0,
      stats: {},
      current: {},
      detailVisible: false,
      templateDialogVisible: false,
      query: { page: 1, size: 20, receiverId: '', senderId: '', category: '', type: '', readStatus: '', businessStatus: '', timeRange: [] },
      templateQuery: { page: 1, size: 10, keyword: '' },
      sendForm: { sendScope: 'all', receiverIds: [], roleIds: [], title: '', content: '', priority: 0, actionUrl: '', expiresAt: '' },
      templateForm: { id: null, code: '', category: 'system', type: 'system', titleTemplate: '', contentTemplate: '', actionUrlTemplate: '', defaultPriority: 0, enabled: true, remark: '' },
      categories: [
        { label: '互动', value: 'interaction' },
        { label: '项目', value: 'project' },
        { label: '邀请', value: 'invite' },
        { label: '请求', value: 'request' },
        { label: '系统', value: 'system' }
      ],
      statCards: [
        { key: 'total', label: '总通知数' },
        { key: 'unread', label: '未读通知数' },
        { key: 'today', label: '今日新增' },
        { key: 'system', label: '系统通知数' },
        { key: 'inviteRequest', label: '邀请/请求' }
      ],
      sendRules: {
        title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }]
      },
      templateRules: {
        code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
        titleTemplate: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
        contentTemplate: [{ required: true, message: '请输入内容模板', trigger: 'blur' }]
      }
    }
  },
  computed: {
    templateVariableTip() {
      return '模板变量格式示例：{{senderName}}、{{projectName}}、{{targetTitle}}'
    }
  },
  mounted() {
    this.refreshAll()
  },
  methods: {
    unwrap(response) {
      if (!response) return null
      if (response.data && response.data.data !== undefined) return response.data.data
      return response.data !== undefined ? response.data : response
    },
    refreshAll() {
      this.fetchStats()
      this.fetchNotifications()
      this.fetchTemplates()
    },
    async fetchStats() {
      this.stats = this.unwrap(await getAdminNotificationStats()) || {}
    },
    buildQuery() {
      const params = { ...this.query }
      if (params.timeRange && params.timeRange.length === 2) {
        params.startTime = params.timeRange[0]
        params.endTime = params.timeRange[1]
      }
      delete params.timeRange
      Object.keys(params).forEach(key => {
        if (params[key] === '') delete params[key]
      })
      return params
    },
    async fetchNotifications() {
      this.loading = true
      try {
        const data = this.unwrap(await getAdminNotifications(this.buildQuery())) || {}
        this.notifications = data.list || []
        this.total = Number(data.total || 0)
      } catch (error) {
        this.$message.error('通知列表加载失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.query.page = 1
      this.fetchNotifications()
    },
    resetSearch() {
      this.query = { page: 1, size: 20, receiverId: '', senderId: '', category: '', type: '', readStatus: '', businessStatus: '', timeRange: [] }
      this.fetchNotifications()
    },
    handleSizeChange(size) {
      this.query.size = size
      this.query.page = 1
      this.fetchNotifications()
    },
    viewDetail(row) {
      this.current = { ...row }
      this.detailVisible = true
    },
    async markAdminRead(row) {
      await markAdminNotificationAsRead(row.id)
      this.$message.success('已标记为已读')
      this.refreshAll()
    },
    async deleteAdminItem(row) {
      await this.$confirm('确定删除这条通知吗？', '删除通知', { type: 'warning' })
      await deleteAdminNotification(row.id)
      this.$message.success('通知已删除')
      this.refreshAll()
    },
    async batchRead() {
      await batchMarkAdminNotificationsAsRead(this.selectedIds)
      this.$message.success('批量标记已读完成')
      this.refreshAll()
    },
    async batchDelete() {
      await this.$confirm(`确定删除选中的 ${this.selectedIds.length} 条通知吗？`, '批量删除', { type: 'warning' })
      await batchDeleteAdminNotifications(this.selectedIds)
      this.$message.success('批量删除完成')
      this.refreshAll()
    },
    async searchUsers(keyword) {
      this.userSearching = true
      try {
        this.userOptions = this.unwrap(await searchNotificationUsers({ keyword, size: 10 })) || []
      } finally {
        this.userSearching = false
      }
    },
    userLabel(user) {
      return `${user.nickname || user.username || '用户'}（ID: ${user.id}）`
    },
    submitSystemNotification() {
      this.$refs.sendForm.validate(async valid => {
        if (!valid) return
        if (this.sendForm.sendScope === 'users' && !this.sendForm.receiverIds.length) {
          this.$message.warning('请选择指定用户')
          return
        }
        if (this.sendForm.sendScope === 'roles' && !this.sendForm.roleIds.length) {
          this.$message.warning('请选择指定角色')
          return
        }
        this.sending = true
        try {
          const data = this.unwrap(await sendAdminSystemNotification({ ...this.sendForm, category: 'system', type: 'system' })) || {}
          this.$message.success(`通知发送成功，共发送 ${data.sentCount || 0} 条`)
          this.resetSendForm()
          this.refreshAll()
          this.activeTab = 'list'
        } finally {
          this.sending = false
        }
      })
    },
    resetSendForm() {
      this.sendForm = { sendScope: 'all', receiverIds: [], roleIds: [], title: '', content: '', priority: 0, actionUrl: '', expiresAt: '' }
      this.$nextTick(() => this.$refs.sendForm && this.$refs.sendForm.clearValidate())
    },
    async fetchTemplates() {
      this.templateLoading = true
      try {
        const data = this.unwrap(await getNotificationTemplates(this.templateQuery)) || {}
        this.templates = data.list || []
        this.templateTotal = Number(data.total || 0)
      } finally {
        this.templateLoading = false
      }
    },
    openTemplateDialog(row) {
      this.templateForm = row ? { ...row } : { id: null, code: '', category: 'system', type: 'system', titleTemplate: '', contentTemplate: '', actionUrlTemplate: '', defaultPriority: 0, enabled: true, remark: '' }
      this.templateDialogVisible = true
    },
    saveTemplate() {
      this.$refs.templateForm.validate(async valid => {
        if (!valid) return
        if (this.templateForm.id) {
          await updateNotificationTemplate(this.templateForm.id, this.templateForm)
        } else {
          await createNotificationTemplate(this.templateForm)
        }
        this.$message.success('模板已保存')
        this.templateDialogVisible = false
        this.fetchTemplates()
      })
    },
    async toggleTemplate(row) {
      await updateNotificationTemplateEnabled(row.id, row.enabled)
      this.$message.success(row.enabled ? '模板已启用' : '模板已禁用')
    },
    async removeTemplate(row) {
      await this.$confirm(`确定删除模板 ${row.code} 吗？`, '删除模板', { type: 'warning' })
      await deleteNotificationTemplate(row.id)
      this.$message.success('模板已删除')
      this.fetchTemplates()
    },
    categoryText(category) {
      const matched = this.categories.find(item => item.value === category)
      return matched ? matched.label : '系统'
    },
    formatDate(value) {
      if (!value) return '-'
      return new Date(value).toLocaleString('zh-CN', { hour12: false })
    }
  }
}
</script>

<style scoped>
.admin-notification-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 6px 0 0;
  color: #909399;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.stat-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
}

.stat-item span {
  display: block;
  color: #909399;
  font-size: 13px;
}

.stat-item strong {
  display: block;
  margin-top: 8px;
  color: #303133;
  font-size: 24px;
}

.search-card,
.table-toolbar {
  margin-bottom: 14px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.pagination-container {
  margin-top: 16px;
  text-align: right;
}

.w120 {
  width: 120px;
}

.w150 {
  width: 150px;
}

.date-range {
  width: 340px;
}

.full-width,
.send-form {
  max-width: 760px;
}

.template-search {
  width: 260px;
}

.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.strong {
  font-weight: 600;
  color: #303133;
}

.danger-text {
  color: #f56c6c;
}

.template-tip {
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(130px, 1fr));
  }

  .page-header,
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .date-range {
    width: 100%;
  }
}
</style>
