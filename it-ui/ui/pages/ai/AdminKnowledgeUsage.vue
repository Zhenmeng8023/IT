<template>
  <div class="knowledge-usage-page">
    <el-card shadow="never" class="usage-card">
      <div class="usage-toolbar">
        <el-input
          v-model.trim="filters.keyword"
          clearable
          size="small"
          placeholder="搜索用户名 / 昵称 / 邮箱"
          class="toolbar-item toolbar-keyword"
          @keyup.enter.native="handleSearch"
        />
        <el-input-number
          v-model="filters.roleId"
          :min="1"
          :step="1"
          size="small"
          controls-position="right"
          placeholder="角色 ID"
          class="toolbar-item"
        />
        <el-select v-model="filters.userStatus" clearable size="small" placeholder="用户状态" class="toolbar-item">
          <el-option label="ACTIVE" value="ACTIVE" />
          <el-option label="DISABLED" value="DISABLED" />
          <el-option label="BANNED" value="BANNED" />
        </el-select>
        <el-select v-model="filters.frozen" clearable size="small" placeholder="冻结状态" class="toolbar-item">
          <el-option label="已冻结" :value="true" />
          <el-option label="未冻结" :value="false" />
        </el-select>
        <el-select v-model="filters.importEnabled" clearable size="small" placeholder="导入能力" class="toolbar-item">
          <el-option label="允许导入" :value="true" />
          <el-option label="禁止导入" :value="false" />
        </el-select>
        <el-select v-model="filters.qaEnabled" clearable size="small" placeholder="问答能力" class="toolbar-item">
          <el-option label="允许问答" :value="true" />
          <el-option label="禁止问答" :value="false" />
        </el-select>
        <el-button size="small" type="primary" :loading="loading.list" @click="handleSearch">查询</el-button>
        <el-button size="small" @click="resetFilters">重置</el-button>
      </div>

      <div class="usage-layout">
        <div class="usage-list">
          <el-table
            v-loading="loading.list"
            :data="tableData"
            border
            height="640"
            highlight-current-row
            row-key="userId"
            @current-change="handleCurrentChange"
            @row-click="handleRowClick"
          >
            <el-table-column prop="userId" label="用户ID" width="88" />
            <el-table-column label="用户" min-width="170">
              <template slot-scope="{ row }">
                <div class="user-cell">
                  <div class="user-cell__name">{{ row.nickname || row.username || `用户#${row.userId}` }}</div>
                  <div class="user-cell__sub">{{ row.username || '-' }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="roleId" label="角色" width="72" />
            <el-table-column prop="knowledgeBaseCount" label="知识库" width="82" />
            <el-table-column prop="memberKnowledgeBaseCount" label="成员库" width="82" />
            <el-table-column prop="documentCount" label="文档" width="82" />
            <el-table-column prop="qaCallCount" label="问答调用" width="100" />
            <el-table-column label="能力状态" min-width="190">
              <template slot-scope="{ row }">
                <div class="tag-group">
                  <el-tag size="mini" :type="row.knowledgeEnabled ? 'success' : 'info'">{{ row.knowledgeEnabled ? '已启用' : '已冻结' }}</el-tag>
                  <el-tag size="mini" :type="row.importEnabled ? 'success' : 'warning'">{{ row.importEnabled ? '可导入' : '禁导入' }}</el-tag>
                  <el-tag size="mini" :type="row.qaEnabled ? 'success' : 'danger'">{{ row.qaEnabled ? '可问答' : '禁问答' }}</el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="usage-pagination">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :current-page="pagination.page"
              :page-size="pagination.size"
              :total="pagination.total"
              @current-change="handlePageChange"
            />
          </div>
        </div>

        <div class="usage-detail">
          <el-empty v-if="!currentUserId && !loading.detail" description="请选择左侧用户查看知识库使用状态" :image-size="88" />

          <template v-else>
            <div class="detail-header">
              <div>
                <div class="detail-title">{{ detail.nickname || detail.username || '用户知识库能力状态' }}</div>
                <div class="detail-subtitle">{{ detail.email || '未配置邮箱' }}</div>
              </div>
              <div class="detail-actions">
                <el-button size="small" :loading="loading.detail" @click="loadCurrentDetail">刷新详情</el-button>
              </div>
            </div>

            <el-skeleton :rows="8" animated :loading="loading.detail">
              <el-descriptions :column="2" border size="small" class="detail-descriptions">
                <el-descriptions-item label="用户ID">{{ detail.userId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="角色ID">{{ detail.roleId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="账号">{{ detail.username || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ detail.userStatus || '-' }}</el-descriptions-item>
                <el-descriptions-item label="最近登录">{{ formatTime(detail.lastLoginAt) }}</el-descriptions-item>
                <el-descriptions-item label="最近活跃">{{ formatTime(detail.lastActiveAt) }}</el-descriptions-item>
                <el-descriptions-item label="知识库数">{{ detail.knowledgeBaseCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="成员知识库数">{{ detail.memberKnowledgeBaseCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="文档数">{{ detail.documentCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="Chunk 数">{{ detail.chunkCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="问答调用">{{ detail.qaCallCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="累计 Tokens">{{ detail.totalTokens || 0 }}</el-descriptions-item>
                <el-descriptions-item label="最近问答">{{ formatTime(detail.lastQaAt) }}</el-descriptions-item>
                <el-descriptions-item label="最近知识库更新时间">{{ formatTime(detail.lastKnowledgeUpdateAt) }}</el-descriptions-item>
                <el-descriptions-item label="策略更新时间">{{ formatTime(detail.policyUpdatedAt) }}</el-descriptions-item>
                <el-descriptions-item label="策略更新人">{{ detail.policyUpdatedBy || '-' }}</el-descriptions-item>
              </el-descriptions>

              <div class="status-panel">
                <el-tag :type="detail.knowledgeEnabled ? 'success' : 'info'">{{ detail.knowledgeEnabled ? '知识库能力启用中' : '知识库能力冻结中' }}</el-tag>
                <el-tag :type="detail.importEnabled ? 'success' : 'warning'">{{ detail.importEnabled ? '允许导入' : '禁止导入' }}</el-tag>
                <el-tag :type="detail.qaEnabled ? 'success' : 'danger'">{{ detail.qaEnabled ? '允许问答' : '禁止问答' }}</el-tag>
              </div>

              <el-card shadow="never" class="quota-card">
                <div slot="header" class="quota-card__header">
                  <span>配额配置</span>
                  <el-button size="mini" type="primary" :loading="loading.quota" @click="saveQuota">保存配额</el-button>
                </div>

                <el-form label-width="130px" size="small">
                  <el-form-item label="最大知识库数">
                    <el-input-number v-model="quotaForm.maxKnowledgeBaseCount" :min="1" :step="1" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="最大文档数">
                    <el-input-number v-model="quotaForm.maxDocumentCount" :min="1" :step="10" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="每日最大问答数">
                    <el-input-number v-model="quotaForm.maxDailyQuestionCount" :min="1" :step="10" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="每日最大导入数">
                    <el-input-number v-model="quotaForm.maxDailyImportCount" :min="1" :step="1" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="备注">
                    <el-input v-model.trim="quotaForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
                  </el-form-item>
                </el-form>
              </el-card>

              <el-card shadow="never" class="quota-card">
                <div slot="header" class="quota-card__header">
                  <span>治理动作</span>
                </div>

                <div class="governance-grid">
                  <el-button
                    size="small"
                    :type="detail.frozen ? 'success' : 'warning'"
                    :loading="loading.action === 'freeze'"
                    @click="handleGovernance(detail.frozen ? 'unfreeze' : 'freeze')"
                  >
                    {{ detail.frozen ? '解除冻结' : '冻结用户知识库能力' }}
                  </el-button>
                  <el-button
                    size="small"
                    :type="detail.importEnabled ? 'warning' : 'success'"
                    :loading="loading.action === 'import'"
                    @click="handleGovernance(detail.importEnabled ? 'disableImport' : 'enableImport')"
                  >
                    {{ detail.importEnabled ? '禁止导入' : '恢复导入' }}
                  </el-button>
                  <el-button
                    size="small"
                    :type="detail.qaEnabled ? 'danger' : 'success'"
                    :loading="loading.action === 'qa'"
                    @click="handleGovernance(detail.qaEnabled ? 'disableQa' : 'enableQa')"
                  >
                    {{ detail.qaEnabled ? '禁止问答' : '恢复问答' }}
                  </el-button>
                </div>
              </el-card>
            </el-skeleton>
          </template>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { adminKnowledgeUsageService } from '@/pages/ai/services/adminKnowledgeUsageService'

export default {
  name: 'AdminKnowledgeUsagePage',
  layout: 'manage',
  data() {
    return {
      filters: {
        keyword: '',
        roleId: null,
        userStatus: '',
        frozen: null,
        importEnabled: null,
        qaEnabled: null
      },
      tableData: [],
      pagination: {
        page: 1,
        size: 10,
        total: 0
      },
      currentUserId: null,
      detail: {},
      quotaForm: {
        maxKnowledgeBaseCount: 20,
        maxDocumentCount: 500,
        maxDailyQuestionCount: 200,
        maxDailyImportCount: 50,
        remark: ''
      },
      loading: {
        list: false,
        detail: false,
        quota: false,
        action: ''
      }
    }
  },
  created() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      this.loading.list = true
      try {
        const res = await adminKnowledgeUsageService.fetchUsers({
          keyword: this.filters.keyword || undefined,
          roleId: this.filters.roleId || undefined,
          userStatus: this.filters.userStatus || undefined,
          frozen: this.filters.frozen,
          importEnabled: this.filters.importEnabled,
          qaEnabled: this.filters.qaEnabled,
          page: this.pagination.page - 1,
          size: this.pagination.size
        })
        const pageData = adminKnowledgeUsageService.extractPageContent(res)
        this.tableData = pageData.content
        this.pagination.total = pageData.total

        if (!this.currentUserId && this.tableData.length) {
          this.currentUserId = this.tableData[0].userId
          this.loadCurrentDetail()
          return
        }

        if (this.currentUserId) {
          const exists = this.tableData.some(item => item.userId === this.currentUserId)
          if (!exists) {
            this.currentUserId = this.tableData.length ? this.tableData[0].userId : null
          }
        }

        if (this.currentUserId) {
          this.loadCurrentDetail()
        } else {
          this.detail = {}
        }
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error?.message || '加载用户知识库使用列表失败')
      } finally {
        this.loading.list = false
      }
    },

    async loadCurrentDetail() {
      if (!this.currentUserId) return
      this.loading.detail = true
      try {
        const res = await adminKnowledgeUsageService.fetchUserStatus(this.currentUserId)
        this.detail = adminKnowledgeUsageService.extractApiData(res) || {}
        this.syncQuotaForm()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error?.message || '加载用户能力状态失败')
      } finally {
        this.loading.detail = false
      }
    },

    syncQuotaForm() {
      this.quotaForm = {
        maxKnowledgeBaseCount: this.detail.maxKnowledgeBaseCount || 20,
        maxDocumentCount: this.detail.maxDocumentCount || 500,
        maxDailyQuestionCount: this.detail.maxDailyQuestionCount || 200,
        maxDailyImportCount: this.detail.maxDailyImportCount || 50,
        remark: this.detail.remark || ''
      }
    },

    handleSearch() {
      this.pagination.page = 1
      this.loadUsers()
    },

    resetFilters() {
      this.filters = {
        keyword: '',
        roleId: null,
        userStatus: '',
        frozen: null,
        importEnabled: null,
        qaEnabled: null
      }
      this.pagination.page = 1
      this.loadUsers()
    },

    handlePageChange(page) {
      this.pagination.page = page
      this.loadUsers()
    },

    handleCurrentChange(row) {
      if (!row || !row.userId || row.userId === this.currentUserId) return
      this.currentUserId = row.userId
      this.loadCurrentDetail()
    },

    handleRowClick(row) {
      if (!row || !row.userId) return
      this.currentUserId = row.userId
      this.loadCurrentDetail()
    },

    async saveQuota() {
      if (!this.currentUserId) return
      this.loading.quota = true
      try {
        const res = await adminKnowledgeUsageService.updateQuota(this.currentUserId, this.quotaForm)
        this.detail = adminKnowledgeUsageService.extractApiData(res) || {}
        this.syncQuotaForm()
        this.$message.success('配额配置已更新')
        this.loadUsers()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error?.message || '保存配额失败')
      } finally {
        this.loading.quota = false
      }
    },

    async handleGovernance(action) {
      if (!this.currentUserId) return
      const actionMap = {
        freeze: {
          loadingKey: 'freeze',
          label: '冻结知识库能力',
          request: reason => adminKnowledgeUsageService.freezeUser(this.currentUserId, { reason })
        },
        unfreeze: {
          loadingKey: 'freeze',
          label: '解除冻结',
          request: reason => adminKnowledgeUsageService.unfreezeUser(this.currentUserId, { reason })
        },
        disableImport: {
          loadingKey: 'import',
          label: '禁止导入',
          request: reason => adminKnowledgeUsageService.disableImport(this.currentUserId, { reason })
        },
        enableImport: {
          loadingKey: 'import',
          label: '恢复导入',
          request: reason => adminKnowledgeUsageService.enableImport(this.currentUserId, { reason })
        },
        disableQa: {
          loadingKey: 'qa',
          label: '禁止问答',
          request: reason => adminKnowledgeUsageService.disableQa(this.currentUserId, { reason })
        },
        enableQa: {
          loadingKey: 'qa',
          label: '恢复问答',
          request: reason => adminKnowledgeUsageService.enableQa(this.currentUserId, { reason })
        }
      }

      const currentAction = actionMap[action]
      if (!currentAction) return

      try {
        const promptResult = await this.$prompt(`请输入“${currentAction.label}”原因`, '治理动作', {
          confirmButtonText: '提交',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入操作原因',
          inputValidator: value => (value && value.trim() ? true : '原因不能为空')
        })
        this.loading.action = currentAction.loadingKey
        const res = await currentAction.request(promptResult.value.trim())
        this.detail = adminKnowledgeUsageService.extractApiData(res) || {}
        this.syncQuotaForm()
        this.$message.success(`${currentAction.label}已提交`)
        this.loadUsers()
      } catch (error) {
        if (error === 'cancel') return
        this.$message.error(error?.response?.data?.message || error?.message || '治理动作执行失败')
      } finally {
        this.loading.action = ''
      }
    },

    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      const pad = num => String(num).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
  }
}
</script>

<style scoped>
.knowledge-usage-page {
  padding: 16px;
}

.usage-card {
  border-radius: 12px;
}

.usage-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.toolbar-item {
  width: 140px;
}

.toolbar-keyword {
  width: 260px;
}

.usage-layout {
  display: flex;
  gap: 16px;
  min-height: 720px;
}

.usage-list {
  width: 52%;
  min-width: 520px;
}

.usage-detail {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--it-border);
  border-radius: 16px;
  padding: 16px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.usage-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.user-cell__name {
  font-weight: 600;
  color: var(--it-text);
}

.user-cell__sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--it-text);
}

.detail-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.detail-descriptions {
  margin-bottom: 14px;
}

.status-panel,
.tag-group,
.governance-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-panel {
  margin-bottom: 14px;
}

.quota-card {
  margin-bottom: 14px;
}

.quota-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 1200px) {
  .usage-layout {
    flex-direction: column;
  }

  .usage-list {
    width: 100%;
    min-width: 0;
  }
}
</style>
