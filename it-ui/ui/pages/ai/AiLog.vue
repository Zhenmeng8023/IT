<template>
  <div class="ai-page">
    <div class="page-header">
      <div>
        <h2>AI 日志中心</h2>
        <p>查看用户调用日志、会话调用日志、检索日志和反馈记录。</p>
      </div>
      <div class="page-actions">
        <el-button icon="el-icon-refresh" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-radio-group v-model="queryMode" size="small">
          <el-radio-button label="user">按用户</el-radio-button>
          <el-radio-button label="session">按会话</el-radio-button>
        </el-radio-group>

        <el-input-number
          v-if="queryMode === 'user'"
          v-model="userId"
          :min="1"
          controls-position="right"
          class="filter-item"
        />

        <el-input-number
          v-else
          v-model="sessionId"
          :min="1"
          controls-position="right"
          class="filter-item"
        />

        <el-select v-model="statusFilter" clearable placeholder="状态过滤" class="filter-item">
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="FAILED" value="FAILED" />
          <el-option label="TIMEOUT" value="TIMEOUT" />
          <el-option label="CANCELLED" value="CANCELLED" />
        </el-select>

        <el-select v-model="requestTypeFilter" clearable placeholder="请求类型" class="filter-item">
          <el-option v-for="item in requestTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>

        <el-button type="primary" icon="el-icon-search" @click="refreshAll">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="17">
        <el-card shadow="never">
          <div slot="header" class="card-header">
            <span>调用日志</span>
            <span class="header-tip">点击“查看详情”可继续查看检索日志</span>
          </div>

          <el-table
            v-loading="callLoading"
            :data="filteredCalls"
            border
            stripe
            style="width: 100%"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userId" label="用户ID" width="90" />
            <el-table-column label="业务类型" width="100">
              <template slot-scope="{ row }">
                {{ row.bizType || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="请求类型" width="150">
              <template slot-scope="{ row }">
                {{ row.requestType || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="模型" min-width="140">
              <template slot-scope="{ row }">
                {{ (row.aiModel && row.aiModel.modelName) || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template slot-scope="{ row }">
                <el-tag :type="statusTagType(row.status)">
                  {{ row.status || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Token" width="110">
              <template slot-scope="{ row }">
                {{ row.totalTokens == null ? '-' : row.totalTokens }}
              </template>
            </el-table-column>
            <el-table-column label="耗时(ms)" width="100">
              <template slot-scope="{ row }">
                {{ row.latencyMs == null ? '-' : row.latencyMs }}
              </template>
            </el-table-column>
            <el-table-column label="成本" width="100">
              <template slot-scope="{ row }">
                {{ row.costAmount == null ? '-' : row.costAmount }}
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="170">
              <template slot-scope="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" @click="openCallDetail(row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="table-footer">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="callTotal"
              :page-size="callSize"
              :page-sizes="[10, 20, 50]"
              :current-page="callPage"
              @current-change="handleCallPageChange"
              @size-change="handleCallSizeChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="7">
        <el-card shadow="never" class="feedback-card">
          <div slot="header" class="card-header">
            <span>反馈记录</span>
            <span class="header-tip" v-if="queryMode === 'session'">会话模式下不加载</span>
          </div>

          <div v-if="queryMode !== 'user'" class="empty-side">
            请切换到“按用户”后查看反馈记录
          </div>

          <div v-else-if="feedbackLoading" class="empty-side">
            正在加载反馈...
          </div>

          <div v-else-if="feedbackList.length === 0" class="empty-side">
            暂无反馈记录
          </div>

          <div v-else class="feedback-list">
            <div v-for="item in feedbackList" :key="item.id" class="feedback-item">
              <div class="feedback-top">
                <el-tag size="mini" :type="feedbackTagType(item.feedbackType)">
                  {{ item.feedbackType || '-' }}
                </el-tag>
                <span class="feedback-time">{{ formatTime(item.createdAt) }}</span>
              </div>
              <div class="feedback-main">
                <div>反馈ID：{{ item.id }}</div>
                <div>消息ID：{{ item.message && item.message.id ? item.message.id : '-' }}</div>
                <div>调用ID：{{ item.callLog && item.callLog.id ? item.callLog.id : '-' }}</div>
              </div>
              <div v-if="item.commentText" class="feedback-comment">{{ item.commentText }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer
      title="调用详情"
      :visible.sync="detailVisible"
      size="55%"
      :with-header="true"
    >
      <div v-if="currentCall" class="drawer-wrap">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="调用ID">{{ currentCall.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ currentCall.userId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ currentCall.bizType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求类型">{{ currentCall.requestType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentCall.status || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模型">
            {{ currentCall.aiModel && currentCall.aiModel.modelName ? currentCall.aiModel.modelName : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Token">{{ currentCall.totalTokens == null ? '-' : currentCall.totalTokens }}</el-descriptions-item>
          <el-descriptions-item label="耗时(ms)">{{ currentCall.latencyMs == null ? '-' : currentCall.latencyMs }}</el-descriptions-item>
          <el-descriptions-item label="成本">{{ currentCall.costAmount == null ? '-' : currentCall.costAmount }}</el-descriptions-item>
          <el-descriptions-item label="错误码">{{ currentCall.errorCode || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-block">
          <div class="block-title">请求内容</div>
          <pre>{{ currentCall.requestText || '-' }}</pre>
        </div>

        <div class="detail-block">
          <div class="block-title">响应内容</div>
          <pre>{{ currentCall.responseText || '-' }}</pre>
        </div>

        <div class="detail-block">
          <div class="block-title">检索日志</div>

          <div v-if="retrievalLoading" class="empty-side small">正在加载检索日志...</div>
          <div v-else-if="retrievalList.length === 0" class="empty-side small">暂无检索日志</div>
          <el-table v-else :data="retrievalList" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="知识库" min-width="120">
              <template slot-scope="{ row }">
                {{ row.knowledgeBase && row.knowledgeBase.name ? row.knowledgeBase.name : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="文档" min-width="140">
              <template slot-scope="{ row }">
                {{ row.document && row.document.title ? row.document.title : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="Score" width="100">
              <template slot-scope="{ row }">
                {{ row.score == null ? '-' : row.score }}
              </template>
            </el-table-column>
            <el-table-column label="Rank" width="80">
              <template slot-scope="{ row }">
                {{ row.rankNo == null ? '-' : row.rankNo }}
              </template>
            </el-table-column>
            <el-table-column prop="retrievalMethod" label="检索方式" width="120" />
            <el-table-column label="Query" min-width="180">
              <template slot-scope="{ row }">
                <span class="ellipsis">{{ row.queryText || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AiLog',
  data() {
    return {
      queryMode: 'user',
      userId: 1,
      sessionId: 1,
      statusFilter: '',
      requestTypeFilter: '',
      requestTypeOptions: [
        'CHAT',
        'KNOWLEDGE_QA',
        'SUMMARY',
        'REWRITE',
        'PROJECT_ASSISTANT',
        'BLOG_ASSISTANT',
        'CODE_EXPLAIN',
        'OTHER'
      ],
      callLoading: false,
      feedbackLoading: false,
      retrievalLoading: false,
      detailVisible: false,
      currentCall: null,
      retrievalList: [],
      feedbackList: [],
      callList: [],
      callPage: 1,
      callSize: 10,
      callTotal: 0
    }
  },
  computed: {
    filteredCalls() {
      return this.callList.filter(item => {
        const matchStatus = !this.statusFilter || item.status === this.statusFilter
        const matchType = !this.requestTypeFilter || item.requestType === this.requestTypeFilter
        return matchStatus && matchType
      })
    }
  },
  created() {
    this.initDefaultUserId()
    this.refreshAll()
  },
  methods: {
    unwrap(res) {
      if (res == null) return null
      if (res.data !== undefined) return res.data
      return res
    },
    normalizePage(res) {
      const data = this.unwrap(res) || {}
      const content = data.content || data.records || data.list || []
      return {
        content: Array.isArray(content) ? content : [],
        total: Number(data.totalElements || data.total || 0)
      }
    },
    initDefaultUserId() {
      if (!process.client) return
      try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const uid = Number(userInfo.id || userInfo.userId || 0)
        if (uid > 0) this.userId = uid
      } catch (e) {
        console.error(e)
      }
    },
    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString('zh-CN', { hour12: false })
    },
    statusTagType(status) {
      if (status === 'SUCCESS') return 'success'
      if (status === 'FAILED') return 'danger'
      if (status === 'TIMEOUT') return 'warning'
      if (status === 'CANCELLED') return 'info'
      return ''
    },
    feedbackTagType(type) {
      if (type === 'LIKE' || type === 'ACCEPTED') return 'success'
      if (type === 'DISLIKE') return 'danger'
      if (type === 'RETRY') return 'warning'
      return ''
    },
    async refreshAll() {
      await this.fetchCalls()
      if (this.queryMode === 'user') {
        await this.fetchFeedbacks()
      } else {
        this.feedbackList = []
      }
    },
    async fetchCalls() {
      this.callLoading = true
      try {
        const url =
          this.queryMode === 'user'
            ? `/ai/logs/user/${this.userId}/calls`
            : `/ai/logs/session/${this.sessionId}/calls`

        const res = await request({
          url,
          method: 'get',
          params: {
            page: this.callPage - 1,
            size: this.callSize
          }
        })
        const pageData = this.normalizePage(res)
        this.callList = pageData.content
        this.callTotal = pageData.total
      } catch (e) {
        console.error(e)
        this.$message.error('获取调用日志失败')
      } finally {
        this.callLoading = false
      }
    },
    async fetchFeedbacks() {
      this.feedbackLoading = true
      try {
        const res = await request({
          url: `/ai/logs/user/${this.userId}/feedbacks`,
          method: 'get'
        })
        const data = this.unwrap(res)
        this.feedbackList = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        this.feedbackList = []
        this.$message.error('获取反馈日志失败')
      } finally {
        this.feedbackLoading = false
      }
    },
    async fetchRetrievals(callId) {
      this.retrievalLoading = true
      try {
        const res = await request({
          url: `/ai/logs/call/${callId}/retrievals`,
          method: 'get'
        })
        const data = this.unwrap(res)
        this.retrievalList = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        this.retrievalList = []
        this.$message.error('获取检索日志失败')
      } finally {
        this.retrievalLoading = false
      }
    },
    async openCallDetail(row) {
      this.currentCall = row
      this.detailVisible = true
      this.retrievalList = []
      if (row && row.id) {
        await this.fetchRetrievals(row.id)
      }
    },
    handleCallPageChange(page) {
      this.callPage = page
      this.fetchCalls()
    },
    handleCallSizeChange(size) {
      this.callSize = size
      this.callPage = 1
      this.fetchCalls()
    }
  }
}
</script>

<style scoped>
.ai-page {
  padding: 24px;
  background: #f6f8fb;
  min-height: 100vh;
}
.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0 0 8px;
  color: #1f2937;
}
.page-header p {
  margin: 0;
  color: #6b7280;
}
.page-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-card {
  margin-bottom: 16px;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.filter-item {
  width: 180px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-tip {
  color: #94a3b8;
  font-size: 12px;
}
.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.feedback-card {
  min-height: 100%;
}
.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.feedback-item {
  background: #f8fafc;
  border-radius: 12px;
  padding: 14px;
}
.feedback-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.feedback-time {
  color: #94a3b8;
  font-size: 12px;
}
.feedback-main {
  color: #334155;
  line-height: 1.8;
  font-size: 13px;
}
.feedback-comment {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  line-height: 1.7;
}
.empty-side {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  text-align: center;
}
.empty-side.small {
  min-height: 100px;
}
.drawer-wrap {
  padding-right: 12px;
}
.detail-block {
  margin-top: 18px;
}
.block-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 10px;
}
.detail-block pre {
  margin: 0;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  max-height: 320px;
  overflow: auto;
}
.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>