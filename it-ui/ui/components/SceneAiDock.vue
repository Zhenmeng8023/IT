<template>
  <div v-if="enabled" class="scene-ai-dock" :class="{ 'is-inline': inline }">
    <div class="dock-card">
      <div class="dock-header">
        <div class="dock-title">
          <i class="el-icon-magic-stick"></i>
          <span>场景 AI 助手</span>
        </div>
        <span class="dock-scene">{{ sceneLabel }}</span>
      </div>

      <div class="dock-actions">
        <template v-if="currentScene === 'project-detail'">
          <el-button
            type="primary"
            size="small"
            icon="el-icon-document"
            :loading="summaryLoading"
            @click="handleProjectSummary"
          >
            AI 总结项目
          </el-button>

          <el-button
            size="small"
            icon="el-icon-s-operation"
            :loading="taskLoading"
            @click="handleProjectTaskSplit"
          >
            AI 拆任务
          </el-button>
        </template>

        <template v-else-if="currentScene === 'blog-write'">
          <el-button
            type="primary"
            size="small"
            icon="el-icon-edit-outline"
            :loading="polishLoading"
            @click="handleBlogPolish"
          >
            AI 润色
          </el-button>

          <el-button
            size="small"
            icon="el-icon-document-copy"
            :loading="blogSummaryLoading"
            @click="handleBlogSummary"
          >
            AI 摘要/标签
          </el-button>
        </template>

        <template v-else>
          <el-button
            type="primary"
            size="small"
            icon="el-icon-chat-dot-round"
            :loading="generalLoading"
            @click="handleFallbackAsk"
          >
            AI 帮我分析当前页面
          </el-button>
        </template>
      </div>
    </div>

    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      width="760px"
      append-to-body
    >
      <div v-loading="dialogLoading" class="ai-dialog-body">
        <div class="dialog-toolbar">
          <el-button size="mini" icon="el-icon-document-copy" @click="copyResult">
            复制
          </el-button>
        </div>

        <div class="ai-result-content">{{ dialogContent || '暂无结果' }}</div>

        <div
          v-if="currentScene === 'blog-write' && parsedBlogTags.length > 0"
          class="blog-tag-block"
        >
          <div class="tag-title">标签建议</div>
          <div class="tag-list">
            <el-tag
              v-for="(tag, index) in parsedBlogTags"
              :key="`${tag}-${index}`"
              size="mini"
              type="success"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>

        <el-button
          v-if="currentScene === 'blog-write' && pendingPolishContent"
          type="primary"
          @click="applyPolishToParent"
        >
          应用到正文
        </el-button>

        <el-button
          v-if="currentScene === 'blog-write' && (pendingSummaryText || parsedBlogTags.length > 0)"
          type="primary"
          plain
          @click="applySummaryToParent"
        >
          应用摘要/标签
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  aiChatTurn,
  aiGenerateBlogSummary,
  aiPolishBlog,
  aiSplitProjectTasks,
  aiSummarizeProject,
  buildProjectAiPayload,
  parseBlogSummaryResult
} from '@/api/aiAssistant'

export default {
  name: 'SceneAiDock',
  props: {
    scene: {
      type: String,
      default: ''
    },
    project: {
      type: Object,
      default: () => ({})
    },
    blog: {
      type: Object,
      default: () => ({})
    },
    inline: {
      type: Boolean,
      default: false
    },
    enabled: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      summaryLoading: false,
      taskLoading: false,
      polishLoading: false,
      blogSummaryLoading: false,
      generalLoading: false,
      dialogVisible: false,
      dialogLoading: false,
      dialogTitle: 'AI 结果',
      dialogContent: '',
      pendingPolishContent: '',
      pendingSummaryText: '',
      parsedBlogTags: []
    }
  },
  computed: {
    currentScene() {
      if (this.scene) return this.scene

      const path = (this.$route && this.$route.path) || ''

      if (path.includes('/projectdetail')) return 'project-detail'
      if (path.includes('/blogwrite')) return 'blog-write'

      return 'general'
    },
    sceneLabel() {
      if (this.currentScene === 'project-detail') return '项目详情'
      if (this.currentScene === 'blog-write') return '博客写作'
      return '通用场景'
    }
  },
  methods: {
    resolveUserId() {
      const candidates = []

      if (this.$store && this.$store.state) {
        const s = this.$store.state
        candidates.push(s.user && (s.user.id || s.user.userId))
        candidates.push(s.userInfo && (s.userInfo.id || s.userInfo.userId))
        candidates.push(s.currentUser && (s.currentUser.id || s.currentUser.userId))
      }

      if (process.client) {
        try {
          const localUser = JSON.parse(localStorage.getItem('userInfo') || '{}')
          const localUser2 = JSON.parse(localStorage.getItem('user') || '{}')
          const sessionUser = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
          const sessionUser2 = JSON.parse(sessionStorage.getItem('user') || '{}')

          candidates.push(localUser.id || localUser.userId)
          candidates.push(localUser2.id || localUser2.userId)
          candidates.push(sessionUser.id || sessionUser.userId)
          candidates.push(sessionUser2.id || sessionUser2.userId)
        } catch (e) {}
      }

      const found = candidates.find(Boolean)
      return found ? Number(found) : 0
    },
    stripHtml(html) {
      return String(html || '')
        .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '')
        .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
        .replace(/<[^>]+>/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
    },
    openDialog(title, content) {
      this.dialogTitle = title
      this.dialogContent = content || ''
      this.dialogVisible = true
    },
    resetPending() {
      this.pendingPolishContent = ''
      this.pendingSummaryText = ''
      this.parsedBlogTags = []
    },
    async handleProjectSummary() {
      const userId = this.resolveUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }

      const projectId = this.project.id || this.project.projectId || this.$route.query.projectId || null
      const title = this.project.title || this.project.projectName || this.project.name || '未命名项目'
      const content = buildProjectAiPayload(this.project)

      this.resetPending()
      this.summaryLoading = true
      this.dialogLoading = true
      this.openDialog('AI 项目总结', '')

      try {
        const result = await aiSummarizeProject({
          userId,
          projectId,
          title,
          content,
          project: this.project
        })

        this.dialogContent = (result && result.text) || '暂无返回内容'
        this.$emit('project-summary-generated', this.dialogContent)
      } catch (e) {
        console.error(e)
        this.dialogContent = ''
        this.$message.error('AI 总结项目失败')
      } finally {
        this.summaryLoading = false
        this.dialogLoading = false
      }
    },
    async handleProjectTaskSplit() {
      const userId = this.resolveUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }

      const projectId = this.project.id || this.project.projectId || this.$route.query.projectId || null
      const title = this.project.title || this.project.projectName || this.project.name || '未命名项目'
      const content = buildProjectAiPayload(this.project)

      this.resetPending()
      this.taskLoading = true
      this.dialogLoading = true
      this.openDialog('AI 任务拆解', '')

      try {
        const result = await aiSplitProjectTasks({
          userId,
          projectId,
          title,
          content,
          project: this.project
        })

        this.dialogContent = (result && result.text) || '暂无返回内容'
        this.$emit('project-task-split-generated', this.dialogContent)
      } catch (e) {
        console.error(e)
        this.dialogContent = ''
        this.$message.error('AI 拆任务失败')
      } finally {
        this.taskLoading = false
        this.dialogLoading = false
      }
    },
    async handleBlogPolish() {
      const userId = this.resolveUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }

      const title = this.blog.title || ''
      const content = this.stripHtml(this.blog.content || '')

      if (!title.trim()) {
        this.$message.warning('请先填写博客标题')
        return
      }

      if (!content.trim()) {
        this.$message.warning('请先填写博客正文')
        return
      }

      this.resetPending()
      this.polishLoading = true
      this.dialogLoading = true
      this.openDialog('AI 润色结果', '')

      try {
        const result = await aiPolishBlog({
          userId,
          title,
          content
        })

        this.pendingPolishContent = (result && result.text) || ''
        this.dialogContent = (result && result.text) || '暂无返回内容'
        this.$emit('blog-polished', (result && result.text) || '')
      } catch (e) {
        console.error(e)
        this.dialogContent = ''
        this.$message.error('AI 润色失败')
      } finally {
        this.polishLoading = false
        this.dialogLoading = false
      }
    },
    async handleBlogSummary() {
      const userId = this.resolveUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }

      const title = this.blog.title || ''
      const content = this.stripHtml(this.blog.content || '')

      if (!title.trim()) {
        this.$message.warning('请先填写博客标题')
        return
      }

      if (!content.trim()) {
        this.$message.warning('请先填写博客正文')
        return
      }

      this.resetPending()
      this.blogSummaryLoading = true
      this.dialogLoading = true
      this.openDialog('AI 摘要 / 标签建议', '')

      try {
        const result = await aiGenerateBlogSummary({
          userId,
          title,
          content
        })

        const rawText = (result && result.text) || ''
        const parsed = parseBlogSummaryResult(rawText)
        this.pendingSummaryText = parsed.summary || ''
        this.parsedBlogTags = parsed.tags || []
        this.dialogContent = parsed.summary || rawText || '暂无返回内容'

        this.$emit('blog-summary-generated', {
          raw: rawText,
          summary: this.pendingSummaryText,
          tags: this.parsedBlogTags
        })
      } catch (e) {
        console.error(e)
        this.dialogContent = ''
        this.$message.error('AI 生成摘要失败')
      } finally {
        this.blogSummaryLoading = false
        this.dialogLoading = false
      }
    },
    async handleFallbackAsk() {
      const userId = this.resolveUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }

      this.resetPending()
      this.generalLoading = true
      this.dialogLoading = true
      this.openDialog('AI 页面分析', '')

      try {
        const pageTitle =
          document && document.title ? document.title : '当前页面'
        const path = (this.$route && this.$route.fullPath) || '/'

        const res = await aiChatTurn({
          userId,
          modelId: 1,
          content: `请根据以下页面信息给出简要分析和建议：页面标题：${pageTitle}；路由：${path}`,
          requestType: 'CHAT',
          bizType: 'GENERAL',
          sceneCode: 'scene.dock.general',
          sessionTitle: '场景 AI 助手'
        })

        const data = res && res.data ? res.data : {}
        this.dialogContent = data.answer || data.content || data.message || '暂无返回内容'
      } catch (e) {
        console.error(e)
        this.dialogContent = ''
        this.$message.error('AI 页面分析失败')
      } finally {
        this.generalLoading = false
        this.dialogLoading = false
      }
    },
    applyPolishToParent() {
      if (!this.pendingPolishContent) {
        this.$message.warning('没有可应用的正文')
        return
      }

      this.$emit('apply-blog-polish', this.pendingPolishContent)
      this.$message.success('已发送到父组件，请在父组件中接收并回填正文')
    },
    applySummaryToParent() {
      this.$emit('apply-blog-summary', {
        summary: this.pendingSummaryText,
        tags: this.parsedBlogTags
      })
      this.$message.success('已发送到父组件，请在父组件中接收并回填摘要/标签')
    },
    copyResult() {
      if (!this.dialogContent) {
        this.$message.warning('没有可复制的内容')
        return
      }

      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(this.dialogContent)
          .then(() => this.$message.success('复制成功'))
          .catch(() => this.$message.error('复制失败'))
        return
      }

      const textarea = document.createElement('textarea')
      textarea.value = this.dialogContent
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success('复制成功')
    }
  }
}
</script>

<style scoped>
.scene-ai-dock {
  position: fixed;
  right: 24px;
  bottom: 110px;
  z-index: 1200;
  width: 220px;
}

.scene-ai-dock.is-inline {
  position: static;
  width: 100%;
}

.dock-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  padding: 14px;
}

.dock-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 10px;
}

.dock-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #111827;
  font-weight: 600;
  font-size: 14px;
}

.dock-title i {
  color: #6366f1;
  font-size: 16px;
}

.dock-scene {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.dock-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dock-actions .el-button {
  width: 100%;
}

.ai-dialog-body {
  min-height: 220px;
}

.dialog-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.ai-result-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.85;
  color: #334155;
  background: #f8fafc;
  border-radius: 14px;
  padding: 16px;
  min-height: 180px;
}

.blog-tag-block {
  margin-top: 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 14px;
}

.tag-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 10px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .scene-ai-dock {
    right: 12px;
    left: 12px;
    width: auto;
    bottom: 88px;
  }
}
</style>