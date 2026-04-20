<template>
  <div class="log-management">
    <AdminPageHeader
      title="日志管理"
      description="查看系统操作日志与行为明细，支持筛选、导出与详情查看。" />

    <AdminToolbarCard class="search-card">
      <el-form :model="searchForm" :inline="true" class="log-search-form">
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.type" clearable placeholder="全部类型" style="width: 140px">
            <el-option label="用户操作" value="user" />
            <el-option label="系统操作" value="system" />
            <el-option label="安全操作" value="security" />
            <el-option label="错误日志" value="error" />
          </el-select>
        </el-form-item>

        <el-form-item label="操作人员">
          <el-input
            v-model.trim="searchForm.operator"
            clearable
            placeholder="输入用户名"
            style="width: 150px" />
        </el-form-item>

        <el-form-item label="操作模块">
          <el-input
            v-model.trim="searchForm.module"
            clearable
            placeholder="输入模块名"
            style="width: 160px" />
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 250px" />
        </el-form-item>

        <el-form-item>
          <el-button v-permission="'btn:log:search'" type="primary" @click="handleSearch">查询</el-button>
          <el-button v-permission="'btn:log:reset'" @click="handleReset">重置</el-button>
          <el-button v-permission="'btn:log:export'" type="warning" :loading="exportLoading" @click="handleExport">导出日志</el-button>
          <el-button v-permission="'btn:log:clean'" type="danger" plain @click="handleCleanExpired">清理过期</el-button>
        </el-form-item>
      </el-form>
    </AdminToolbarCard>

    <AdminTableCard class="table-card">
      <el-table
        :data="logList"
        v-loading="loading"
        stripe
        style="width: 100%"
        height="600">
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="createTime" label="操作时间" width="170" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="operator" label="操作人员" width="140" align="center" />

        <el-table-column prop="type" label="操作类型" width="110" align="center">
          <template slot-scope="scope">
            <StatusTag :value="scope.row.type" :text-map="typeTextMap" :type-map="typeTagMap" size="small" />
          </template>
        </el-table-column>

        <el-table-column prop="module" label="操作模块" width="160" align="center" />

        <el-table-column prop="action" label="操作内容" min-width="220">
          <template slot-scope="scope">
            <el-tooltip
              v-if="toText(scope.row.action).length > 30"
              :content="toText(scope.row.action)"
              placement="top">
              <span>{{ shortText(scope.row.action, 30) }}</span>
            </el-tooltip>
            <span v-else>{{ toText(scope.row.action) || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ip" label="IP" width="140" align="center">
          <template slot-scope="scope">
            <CodeTag :value="scope.row.ip || '-'" />
          </template>
        </el-table-column>

        <el-table-column prop="result" label="结果" width="100" align="center">
          <template slot-scope="scope">
            <StatusTag :value="scope.row.result" :text-map="resultTextMap" :type-map="resultTagMap" size="small" />
          </template>
        </el-table-column>

        <el-table-column prop="details" label="详情" min-width="260">
          <template slot-scope="scope">
            <el-tooltip
              v-if="toText(scope.row.details).length > 40"
              :content="toText(scope.row.details)"
              placement="top">
              <span>{{ shortText(scope.row.details, 40) }}</span>
            </el-tooltip>
            <span v-else>{{ toText(scope.row.details) || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template slot-scope="scope">
            <AdminActionGroup>
              <el-button
                v-permission="'btn:log:view-detail'"
                size="mini"
                type="text"
                icon="el-icon-view"
                @click="handleView(scope.row)">
                查看
              </el-button>
            </AdminActionGroup>
          </template>
        </el-table-column>
      </el-table>

      <template slot="pagination">
        <el-pagination
          :current-page="pagination.currentPage"
          :page-sizes="[20, 50, 100, 200]"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </template>
    </AdminTableCard>

    <AdminFormDialog
      title="日志详情"
      :visible.sync="detailDialogVisible"
      width="760px"
      :loading="detailLoading">
      <div class="log-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作时间">{{ formatDateTime(currentLog.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="操作人员">{{ currentLog.operator || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <StatusTag :value="currentLog.type" :text-map="typeTextMap" :type-map="typeTagMap" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="操作模块">{{ currentLog.module || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP">{{ currentLog.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结果">
            <StatusTag :value="currentLog.result" :text-map="resultTextMap" :type-map="resultTagMap" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="操作内容" :span="2">{{ toText(currentLog.action) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="详情信息" :span="2">
            <pre class="log-detail-pre">{{ toText(currentLog.details) || '-' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template slot="footer">
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </AdminFormDialog>
  </div>
</template>

<script>
import {
  GetOperationLogsPage,
  ExportOperationLogs,
  GetOperationLogDetail,
  CleanExpiredLogs
} from '@/api/adminLog'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbarCard from '@/components/admin/AdminToolbarCard.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminActionGroup from '@/components/admin/AdminActionGroup.vue'
import AdminFormDialog from '@/components/admin/AdminFormDialog.vue'
import StatusTag from '@/components/admin/StatusTag.vue'
import CodeTag from '@/components/admin/CodeTag.vue'

export default {
  name: 'Log',
  layout: 'manage',
  components: {
    AdminPageHeader,
    AdminToolbarCard,
    AdminTableCard,
    AdminActionGroup,
    AdminFormDialog,
    StatusTag,
    CodeTag
  },
  data() {
    return {
      loading: false,
      detailLoading: false,
      exportLoading: false,
      searchForm: {
        type: '',
        operator: '',
        module: '',
        dateRange: []
      },
      logList: [],
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      detailDialogVisible: false,
      currentLog: {},
      typeTextMap: {
        user: '用户操作',
        system: '系统操作',
        security: '安全操作',
        error: '错误日志'
      },
      typeTagMap: {
        user: 'primary',
        system: 'info',
        security: 'warning',
        error: 'danger'
      },
      resultTextMap: {
        success: '成功',
        failed: '失败'
      },
      resultTagMap: {
        success: 'success',
        failed: 'danger'
      }
    }
  },
  mounted() {
    this.fetchLogList()
  },
  methods: {
    unwrapResponse(raw) {
      if (!raw) return {}
      if (raw.data !== undefined && raw.status !== undefined) {
        return raw.data || {}
      }
      return raw
    },
    parseResponse(raw) {
      const body = this.unwrapResponse(raw)
      const hasSuccessFlag = Object.prototype.hasOwnProperty.call(body || {}, 'success')
      const success = hasSuccessFlag ? !!body.success : body?.code === undefined || body?.code === 0
      return {
        success,
        message: body?.message || '',
        data: body?.data !== undefined ? body.data : body,
        page: body?.page || null
      }
    },
    buildQueryParams() {
      const params = {
        page: this.pagination.currentPage,
        size: this.pagination.pageSize,
        type: this.searchForm.type || undefined,
        operator: this.searchForm.operator || undefined,
        module: this.searchForm.module || undefined
      }
      const range = this.searchForm.dateRange || []
      if (Array.isArray(range) && range.length === 2) {
        params.startTime = range[0]
        params.endTime = range[1]
      }
      return params
    },
    normalizeResult(value) {
      if (value === true || value === 1) return 'success'
      const text = String(value || '').toLowerCase()
      if (['success', 'ok', 'passed', 'pass', 'true', '1'].includes(text)) {
        return 'success'
      }
      return 'failed'
    },
    normalizeLogItem(item) {
      return {
        id: item.id || item.logId || item.operationLogId,
        createTime: item.createTime || item.operationTime || item.createdAt || item.createAt,
        operator: item.operator || item.operatorName || item.username || item.userName,
        type: item.type || item.operationType || item.category || 'system',
        module: item.module || item.moduleName || item.bizModule || '',
        action: item.action || item.operation || item.operationContent || item.content || '',
        ip: item.ip || item.ipAddress || item.requestIp || '',
        result: this.normalizeResult(item.result || item.operationResult || item.status || item.success),
        details: item.details || item.detail || item.message || item.remark || '',
        raw: item
      }
    },
    async fetchLogList() {
      this.loading = true
      try {
        const response = await GetOperationLogsPage(this.buildQueryParams())
        const { success, message, data, page } = this.parseResponse(response)
        if (!success) {
          throw new Error(message || '获取日志列表失败')
        }
        const list =
          (Array.isArray(data) && data) ||
          (Array.isArray(data?.list) && data.list) ||
          (Array.isArray(data?.records) && data.records) ||
          (Array.isArray(data?.items) && data.items) ||
          []

        this.logList = list.map(item => this.normalizeLogItem(item))
        const total = Number(page?.total || data?.total || data?.count || data?.totalCount || this.logList.length)
        this.pagination.total = Number.isFinite(total) ? total : this.logList.length
        if (page?.current) {
          const current = Number(page.current)
          if (Number.isFinite(current) && current > 0) {
            this.pagination.currentPage = current
          }
        }
        if (page?.size) {
          const size = Number(page.size)
          if (Number.isFinite(size) && size > 0) {
            this.pagination.pageSize = size
          }
        }
      } catch (error) {
        console.error('fetch operation logs failed:', error)
        this.$message.error(error?.response?.data?.message || error.message || '获取日志列表失败')
      } finally {
        this.loading = false
      }
    },
    async fetchLogDetail(logId) {
      if (!logId) return null
      const response = await GetOperationLogDetail(logId)
      const { success, message, data } = this.parseResponse(response)
      if (!success) {
        throw new Error(message || '获取日志详情失败')
      }
      if (!data) return null
      return this.normalizeLogItem(data)
    },
    async handleView(log) {
      this.detailDialogVisible = true
      this.detailLoading = true
      try {
        const detail = await this.fetchLogDetail(log.id)
        this.currentLog = detail || { ...log }
      } catch (error) {
        this.currentLog = { ...log }
      } finally {
        this.detailLoading = false
      }
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchLogList()
    },
    handleReset() {
      this.searchForm = {
        type: '',
        operator: '',
        module: '',
        dateRange: []
      }
      this.pagination.currentPage = 1
      this.fetchLogList()
    },
    async handleExport() {
      this.exportLoading = true
      try {
        const fileData = await ExportOperationLogs(this.buildQueryParams())
        const blob = fileData instanceof Blob ? fileData : new Blob([fileData], { type: 'text/csv;charset=utf-8' })
        if (typeof window !== 'undefined') {
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `operation-logs-${new Date().toISOString().slice(0, 10)}.csv`
          link.click()
          window.URL.revokeObjectURL(url)
        }
        this.$message.success('日志导出成功')
      } catch (error) {
        console.error('export operation logs failed:', error)
        this.$message.error(error?.response?.data?.message || error.message || '导出日志失败')
      } finally {
        this.exportLoading = false
      }
    },
    async handleCleanExpired() {
      try {
        const { value } = await this.$prompt('可选填写保留天数，不填则使用系统默认值。', '清理过期日志', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          inputPattern: /^$|^[1-9]\d*$/,
          inputErrorMessage: '请输入正整数，或留空'
        })
        const retainDays = String(value || '').trim()
        const payload = retainDays ? { retainDays: Number(retainDays) } : {}
        const response = await CleanExpiredLogs(payload)
        const { success, message, data } = this.parseResponse(response)
        if (!success) {
          throw new Error(message || '清理过期日志失败')
        }
        const deletedCount = Number(data?.deletedCount || 0)
        const currentRetainDays = data?.retainDays
        const desc = [`Deleted ${deletedCount} logs`]
        if (currentRetainDays !== undefined && currentRetainDays !== null) {
          desc.push(`Retain ${currentRetainDays} days`)
        }
        this.$message.success(`Cleaned expired logs (${desc.join(", ")})`)
        this.fetchLogList()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error?.response?.data?.message || error.message || '清理过期日志失败')
        }
      }
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchLogList()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchLogList()
    },
    toText(value) {
      if (value === null || value === undefined) return ''
      if (typeof value === 'string') return value
      if (typeof value === 'number' || typeof value === 'boolean') return String(value)
      try {
        return JSON.stringify(value, null, 2)
      } catch (error) {
        return String(value)
      }
    },
    shortText(value, maxLength) {
      const text = this.toText(value)
      if (text.length <= maxLength) return text
      return `${text.slice(0, maxLength)}...`
    },
    formatDateTime(date) {
      if (!date) return '-'
      const dateObj = new Date(date)
      if (Number.isNaN(dateObj.getTime())) return String(date)
      return dateObj.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.log-management {
  padding: 20px;
}

.search-card,
.table-card {
  margin-bottom: 18px;
}

.log-search-form {
  display: flex;
  flex-wrap: wrap;
}

.log-detail {
  padding: 4px 2px;
}

.log-detail-pre {
  margin: 0;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--it-showcase-bg);
  color: var(--it-showcase-text);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-soft);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
}
</style>
