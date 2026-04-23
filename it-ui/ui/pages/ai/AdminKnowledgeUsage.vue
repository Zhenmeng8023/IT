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
        <el-input
          v-model.trim="filters.roleId"
          clearable
          size="small"
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
            <el-table-column label="用户名" min-width="160">
              <template slot-scope="{ row }">
                <div class="user-cell">
                  <div class="user-cell__name">{{ row.username || `用户#${row.userId}` }}</div>
                  <div class="user-cell__sub">{{ row.nickname || '-' }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="personalKnowledgeBaseCount" label="个人库数" width="90" />
            <el-table-column prop="projectKnowledgeBaseCount" label="参与项目库数" width="112" />
            <el-table-column prop="totalDocumentCount" label="文档总数" width="90" />
            <el-table-column prop="qaCountLast7Days" label="近7日问答" width="96" />
            <el-table-column prop="importCountLast7Days" label="近7日导入" width="96" />
            <el-table-column label="最近活跃" min-width="128">
              <template slot-scope="{ row }">
                {{ formatTime(row.recentActiveAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="102">
              <template slot-scope="{ row }">
                <el-tag size="mini" :type="usageStatusTagType(row.usageStatus)">{{ usageStatusLabel(row.usageStatus) }}</el-tag>
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
          <el-empty v-if="!currentUserId && !loading.detail" description="请选择左侧用户查看知识库使用详情" :image-size="88" />

          <template v-else>
            <div class="detail-header">
              <div>
                <div class="detail-title">{{ detail.username || `用户#${detail.userId || ''}` }}</div>
                <div class="detail-subtitle">{{ detail.nickname || detail.email || '暂无补充信息' }}</div>
              </div>
              <div class="detail-actions">
                <el-button size="small" :loading="loading.detail" @click="loadCurrentDetail">刷新详情</el-button>
              </div>
            </div>

            <el-skeleton :rows="8" animated :loading="loading.detail">
              <el-card shadow="never" class="detail-block">
                <div slot="header" class="detail-block__header">
                  <span>用户视图</span>
                </div>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="用户 ID">{{ detail.userId || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="用户名">{{ detail.username || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="拥有个人知识库数">{{ detail.personalKnowledgeBaseCount || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="参与项目知识库数">{{ detail.projectKnowledgeBaseCount || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="文档总数">{{ detail.totalDocumentCount || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="近 7 日问答次数">{{ detail.qaCountLast7Days || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="近 7 日导入次数">{{ detail.importCountLast7Days || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="最近活跃时间">{{ formatTime(detail.recentActiveAt) }}</el-descriptions-item>
                  <el-descriptions-item label="账号状态">{{ detail.userStatus || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="使用状态">
                    <el-tag size="mini" :type="usageStatusTagType(detail.usageStatus)">{{ usageStatusLabel(detail.usageStatus) }}</el-tag>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>

              <el-card shadow="never" class="detail-block">
                <div slot="header" class="detail-block__header detail-block__header--between">
                  <span>配额视图</span>
                  <el-button size="mini" type="primary" :loading="loading.quota" @click="saveQuota">保存配额</el-button>
                </div>

                <el-form label-width="180px" size="small">
                  <el-form-item label="个人知识库数量上限">
                    <el-input-number v-model="quotaForm.maxKnowledgeBaseCount" :min="1" :step="1" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="单知识库文档数上限">
                    <el-input-number v-model="quotaForm.maxDocumentCount" :min="1" :step="10" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="月问答次数限额">
                    <el-input-number v-model="quotaForm.maxMonthlyQaCount" :min="1" :step="10" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="月 Token 限额">
                    <div class="quota-inline">
                      <span>{{ formatNumber(quotaTokenLimitPreview) }}</span>
                      <span class="quota-inline__hint">按 1 次问答≈{{ tokenPerQaEstimate }} Tokens 估算</span>
                    </div>
                  </el-form-item>
                  <el-form-item label="月导入任务限额">
                    <el-input-number v-model="quotaForm.maxMonthlyImportCount" :min="1" :step="1" controls-position="right" />
                  </el-form-item>
                  <el-form-item label="备注">
                    <el-input v-model.trim="quotaForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
                  </el-form-item>
                </el-form>
              </el-card>

              <el-card shadow="never" class="detail-block">
                <div slot="header" class="detail-block__header">
                  <span>风险视图</span>
                </div>

                <div class="risk-head">
                  <el-tag :type="usageStatusTagType(detail.usageStatus)">{{ usageStatusLabel(detail.usageStatus) }}</el-tag>
                  <span class="risk-reason">{{ detail.riskReason || '暂无风险信号' }}</span>
                </div>

                <div class="risk-metrics">
                  <div class="risk-metrics__row">
                    <span>月问答次数</span>
                    <span>{{ detail.qaCountThisMonth || 0 }} / {{ detail.maxMonthlyQaCount || '不限' }}</span>
                  </div>
                  <el-progress :percentage="usagePercent(detail.qaCountThisMonth, detail.maxMonthlyQaCount)" :status="progressStatus(detail.qaCountThisMonth, detail.maxMonthlyQaCount)" />

                  <div class="risk-metrics__row">
                    <span>月 Token 使用量</span>
                    <span>{{ formatNumber(detail.tokenCountThisMonth || 0) }} / {{ detail.maxMonthlyTokenCount ? formatNumber(detail.maxMonthlyTokenCount) : '不限' }}</span>
                  </div>
                  <el-progress :percentage="usagePercent(detail.tokenCountThisMonth, detail.maxMonthlyTokenCount)" :status="progressStatus(detail.tokenCountThisMonth, detail.maxMonthlyTokenCount)" />

                  <div class="risk-metrics__row">
                    <span>月导入任务</span>
                    <span>{{ detail.importCountThisMonth || 0 }} / {{ detail.maxMonthlyImportCount || '不限' }}</span>
                  </div>
                  <el-progress :percentage="usagePercent(detail.importCountThisMonth, detail.maxMonthlyImportCount)" :status="progressStatus(detail.importCountThisMonth, detail.maxMonthlyImportCount)" />
                </div>
              </el-card>

              <el-card shadow="never" class="detail-block">
                <div slot="header" class="detail-block__header">
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
                    {{ detail.qaEnabled ? '禁止知识库问答' : '恢复知识库问答' }}
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

const DAYS_IN_MONTH = 30
const TOKEN_PER_QA_ESTIMATE = 2000

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
        maxMonthlyQaCount: 6000,
        maxMonthlyImportCount: 1500,
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
  computed: {
    tokenPerQaEstimate() {
      return TOKEN_PER_QA_ESTIMATE
    },
    quotaTokenLimitPreview() {
      return this.computeMonthlyTokenLimit(this.quotaForm.maxMonthlyQaCount)
    }
  },
  created() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      this.loading.list = true
      try {
        const roleId = this.normalizeRoleId(this.filters.roleId)
        const res = await adminKnowledgeUsageService.fetchUsers({
          keyword: this.filters.keyword || undefined,
          roleId,
          userStatus: this.filters.userStatus || undefined,
          frozen: this.filters.frozen,
          importEnabled: this.filters.importEnabled,
          qaEnabled: this.filters.qaEnabled,
          page: this.pagination.page - 1,
          size: this.pagination.size
        })
        const pageData = adminKnowledgeUsageService.extractPageContent(res)
        this.tableData = (pageData.content || []).map(item => this.normalizeUsageData(item))
        this.pagination.total = pageData.total

        const maxPage = Math.max(1, Math.ceil(this.pagination.total / this.pagination.size))
        if (!this.tableData.length && this.pagination.total > 0 && this.pagination.page > maxPage) {
          this.pagination.page = maxPage
          await this.loadUsers()
          return
        }

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
        this.detail = this.normalizeUsageData(adminKnowledgeUsageService.extractApiData(res) || {})
        this.syncQuotaForm()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error?.message || '加载用户知识库使用详情失败')
      } finally {
        this.loading.detail = false
      }
    },

    normalizeUsageData(source = {}) {
      const personalKnowledgeBaseCount = this.asNumber(source.personalKnowledgeBaseCount, source.knowledgeBaseCount)
      const projectKnowledgeBaseCount = this.asNumber(source.projectKnowledgeBaseCount, source.memberKnowledgeBaseCount)
      const totalDocumentCount = this.asNumber(source.totalDocumentCount, source.documentCount)
      const maxMonthlyQaCount = this.resolveMonthlyLimit(source.maxMonthlyQaCount, source.maxDailyQuestionCount)
      const maxMonthlyImportCount = this.resolveMonthlyLimit(source.maxMonthlyImportCount, source.maxDailyImportCount)
      const maxMonthlyTokenCount = this.asNumber(source.maxMonthlyTokenCount, this.computeMonthlyTokenLimit(maxMonthlyQaCount))
      const qaCountThisMonth = this.asNumber(source.qaCountThisMonth, source.qaCallCount)
      const tokenCountThisMonth = this.asNumber(source.tokenCountThisMonth, source.totalTokens)
      const importCountThisMonth = this.asNumber(source.importCountThisMonth)
      const recentActiveAt = source.recentActiveAt || source.lastActiveAt || source.lastQaAt || source.lastImportAt || source.lastLoginAt || null
      const usageStatus = source.usageStatus || this.inferUsageStatus({
        ...source,
        personalKnowledgeBaseCount,
        totalDocumentCount,
        maxMonthlyQaCount,
        maxMonthlyImportCount,
        maxMonthlyTokenCount,
        qaCountThisMonth,
        tokenCountThisMonth,
        importCountThisMonth
      })

      return {
        ...source,
        personalKnowledgeBaseCount,
        projectKnowledgeBaseCount,
        totalDocumentCount,
        qaCountLast7Days: this.asNumber(source.qaCountLast7Days, source.qaCallCount),
        importCountLast7Days: this.asNumber(source.importCountLast7Days),
        qaCountThisMonth,
        tokenCountThisMonth,
        importCountThisMonth,
        maxMonthlyQaCount,
        maxMonthlyImportCount,
        maxMonthlyTokenCount,
        recentActiveAt,
        usageStatus,
        riskReason: source.riskReason || this.inferRiskReason(usageStatus, source)
      }
    },

    syncQuotaForm() {
      this.quotaForm = {
        maxKnowledgeBaseCount: this.asNumber(this.detail.maxKnowledgeBaseCount, 20),
        maxDocumentCount: this.asNumber(this.detail.maxDocumentCount, 500),
        maxMonthlyQaCount: this.resolveMonthlyLimit(this.detail.maxMonthlyQaCount, this.detail.maxDailyQuestionCount, 6000),
        maxMonthlyImportCount: this.resolveMonthlyLimit(this.detail.maxMonthlyImportCount, this.detail.maxDailyImportCount, 1500),
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

    normalizeRoleId(value) {
      if (value === null || value === undefined || value === '') return undefined
      const roleId = Number(value)
      return Number.isInteger(roleId) && roleId > 0 ? roleId : undefined
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
        const monthlyQa = this.normalizePositive(this.quotaForm.maxMonthlyQaCount, 6000)
        const monthlyImport = this.normalizePositive(this.quotaForm.maxMonthlyImportCount, 1500)
        const payload = {
          maxKnowledgeBaseCount: this.normalizePositive(this.quotaForm.maxKnowledgeBaseCount, 20),
          maxDocumentCount: this.normalizePositive(this.quotaForm.maxDocumentCount, 500),
          maxMonthlyQaCount: monthlyQa,
          maxMonthlyImportCount: monthlyImport,
          maxDailyQuestionCount: this.monthlyToDaily(monthlyQa),
          maxDailyImportCount: this.monthlyToDaily(monthlyImport),
          remark: this.quotaForm.remark || ''
        }
        const res = await adminKnowledgeUsageService.updateQuota(this.currentUserId, payload)
        this.detail = this.normalizeUsageData(adminKnowledgeUsageService.extractApiData(res) || {})
        this.syncQuotaForm()
        this.$message.success('用户配额已更新')
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
          label: '禁止知识库问答',
          request: reason => adminKnowledgeUsageService.disableQa(this.currentUserId, { reason })
        },
        enableQa: {
          loadingKey: 'qa',
          label: '恢复知识库问答',
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
        this.detail = this.normalizeUsageData(adminKnowledgeUsageService.extractApiData(res) || {})
        this.syncQuotaForm()
        this.$message.success(`${currentAction.label}已执行`)
        this.loadUsers()
      } catch (error) {
        if (error === 'cancel') return
        this.$message.error(error?.response?.data?.message || error?.message || '治理动作执行失败')
      } finally {
        this.loading.action = ''
      }
    },

    asNumber(...values) {
      for (let i = 0; i < values.length; i += 1) {
        const value = values[i]
        if (value === null || value === undefined || value === '') continue
        const num = Number(value)
        if (!Number.isNaN(num)) return num
      }
      return 0
    },

    normalizePositive(value, fallback) {
      const num = Number(value)
      if (Number.isNaN(num) || num <= 0) return fallback
      return Math.floor(num)
    },

    resolveMonthlyLimit(monthlyValue, dailyValue, fallback = 0) {
      const monthly = this.asNumber(monthlyValue)
      if (monthly > 0) return monthly
      const daily = this.asNumber(dailyValue)
      if (daily > 0) return daily * DAYS_IN_MONTH
      return fallback
    },

    monthlyToDaily(monthlyLimit) {
      const normalized = this.normalizePositive(monthlyLimit, 1)
      return Math.max(1, Math.ceil(normalized / DAYS_IN_MONTH))
    },

    computeMonthlyTokenLimit(monthlyQaLimit) {
      const normalized = this.normalizePositive(monthlyQaLimit, 0)
      if (normalized <= 0) return 0
      return normalized * TOKEN_PER_QA_ESTIMATE
    },

    inferUsageStatus(row) {
      if (row.frozen) {
        return 'FROZEN'
      }
      const overLimit =
        this.isExceeded(row.personalKnowledgeBaseCount, row.maxKnowledgeBaseCount) ||
        this.isExceeded(row.maxDocumentCountPerKnowledgeBase, row.maxDocumentCount) ||
        this.isExceeded(row.qaCountThisMonth, row.maxMonthlyQaCount) ||
        this.isExceeded(row.importCountThisMonth, row.maxMonthlyImportCount) ||
        this.isExceeded(row.tokenCountThisMonth, row.maxMonthlyTokenCount)
      if (overLimit) {
        return 'OVER_LIMIT'
      }
      if (
        !row.importEnabled ||
        !row.qaEnabled ||
        String(row.userStatus || '').toUpperCase() === 'DISABLED' ||
        String(row.userStatus || '').toUpperCase() === 'BANNED'
      ) {
        return 'RISK'
      }
      return 'NORMAL'
    },

    inferRiskReason(status, row) {
      if (status === 'NORMAL') {
        return ''
      }
      const reasons = []
      if (status === 'FROZEN' || row.frozen) reasons.push('已冻结')
      if (!row.importEnabled) reasons.push('导入能力禁用')
      if (!row.qaEnabled) reasons.push('问答能力禁用')
      if (status === 'OVER_LIMIT') reasons.push('配额超限')
      if (!reasons.length) reasons.push('存在风险信号')
      return reasons.join('；')
    },

    isExceeded(current, limit) {
      const currentValue = this.asNumber(current)
      const limitValue = this.asNumber(limit)
      if (limitValue <= 0) return false
      return currentValue > limitValue
    },

    usagePercent(current, limit) {
      const currentValue = this.asNumber(current)
      const limitValue = this.asNumber(limit)
      if (limitValue <= 0) return 0
      return Math.min(100, Math.max(0, Math.round((currentValue / limitValue) * 100)))
    },

    progressStatus(current, limit) {
      const percent = this.usagePercent(current, limit)
      if (percent >= 100) return 'exception'
      if (percent >= 90) return 'warning'
      return 'success'
    },

    usageStatusLabel(status) {
      const map = {
        NORMAL: '正常',
        FROZEN: '冻结',
        OVER_LIMIT: '超额',
        RISK: '风险'
      }
      return map[status] || '正常'
    },

    usageStatusTagType(status) {
      const map = {
        NORMAL: 'success',
        FROZEN: 'info',
        OVER_LIMIT: 'danger',
        RISK: 'warning'
      }
      return map[status] || 'success'
    },

    formatNumber(value) {
      const number = this.asNumber(value)
      return number.toLocaleString('zh-CN')
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
  width: 56%;
  min-width: 620px;
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

.detail-block {
  margin-bottom: 14px;
}

.detail-block__header {
  font-size: 14px;
  font-weight: 600;
  color: var(--it-text);
}

.detail-block__header--between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quota-inline {
  display: flex;
  gap: 8px;
  align-items: center;
}

.quota-inline__hint {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.risk-head {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.risk-reason {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.risk-metrics {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-metrics__row {
  display: flex;
  justify-content: space-between;
  color: var(--it-text);
  font-size: 13px;
}

.governance-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1400px) {
  .usage-layout {
    flex-direction: column;
  }

  .usage-list {
    width: 100%;
    min-width: 0;
  }
}
</style>
