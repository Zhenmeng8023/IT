<template>
  <div class="task-comment-panel">
    <div class="editor-card">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="3"
        resize="vertical"
        placeholder="输入评论内容"
      />
      <div class="editor-actions">
        <el-button size="small" @click="loadComments">刷新</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="submitComment">发表评论</el-button>
      </div>
    </div>

    <div v-if="!comments.length" class="empty-wrap">
      <el-empty description="暂无评论" />
    </div>

    <div v-else class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-card">
        <div class="comment-header">
          <div class="author-wrap">
            <el-avatar :size="30" :src="comment.authorAvatar || ''">
              {{ firstChar(comment.authorName) }}
            </el-avatar>
            <div class="author-text">
              <div class="author-name">{{ comment.authorName || '未知用户' }}</div>
              <div class="comment-time">{{ formatDate(comment.createdAt) }}</div>
            </div>
          </div>
          <div class="comment-actions">
            <el-button type="text" size="mini" @click="toggleReply(comment.id)">回复</el-button>
            <el-button
              v-if="comment.canDelete"
              type="text"
              size="mini"
              class="danger-text"
              @click="removeComment(comment.id)"
            >
              删除
            </el-button>
          </div>
        </div>

        <div class="comment-content" :class="{ deleted: isDeleted(comment) }">
          {{ comment.content || (isDeleted(comment) ? '该评论已删除' : '-') }}
        </div>

        <div v-if="replyingId === comment.id" class="reply-box">
          <el-input
            v-model="replyDraft"
            type="textarea"
            :rows="2"
            resize="vertical"
            placeholder="输入回复内容"
          />
          <div class="editor-actions">
            <el-button size="small" @click="cancelReply">取消</el-button>
            <el-button type="primary" size="small" :loading="replySubmitting" @click="submitReply(comment.id)">发送回复</el-button>
          </div>
        </div>

        <div v-if="getReplies(comment).length" class="reply-list">
          <div v-for="reply in getReplies(comment)" :key="reply.id" class="reply-card">
            <div class="comment-header">
              <div class="author-wrap">
                <el-avatar :size="24" :src="reply.authorAvatar || ''">
                  {{ firstChar(reply.authorName) }}
                </el-avatar>
                <div class="author-text">
                  <div class="author-name">{{ reply.authorName || '未知用户' }}</div>
                  <div class="comment-time">{{ formatDate(reply.createdAt) }}</div>
                </div>
              </div>
              <div class="comment-actions">
                <el-button
                  v-if="reply.canDelete"
                  type="text"
                  size="mini"
                  class="danger-text"
                  @click="removeComment(reply.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <div class="comment-content" :class="{ deleted: isDeleted(reply) }">
              {{ reply.content || (isDeleted(reply) ? '该回复已删除' : '-') }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  createTaskComment,
  deleteTaskComment,
  getTaskComments,
  replyTaskComment
} from '@/api/project'

export default {
  name: 'TaskCommentPanel',
  props: {
    taskId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      comments: [],
      draft: '',
      submitting: false,
      replyingId: null,
      replyDraft: '',
      replySubmitting: false
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) {
          this.loadComments()
        } else {
          this.comments = []
        }
      }
    }
  },
  methods: {
    async loadComments() {
      if (!this.taskId) return
      try {
        const res = await getTaskComments(this.taskId)
        const payload = res && res.data
        const list = Array.isArray(payload)
          ? payload
          : Array.isArray(payload && payload.data)
            ? payload.data
            : []
        this.comments = list
      } catch (e) {
        this.comments = []
        const msg =
          (e && e.response && e.response.data && (e.response.data.message || e.response.data.msg)) ||
          '加载评论失败'
        this.$message.error(msg)
      }
    },
    async submitComment() {
      const content = (this.draft || '').trim()
      if (!content) {
        this.$message.warning('请输入评论内容')
        return
      }
      this.submitting = true
      try {
        await createTaskComment(this.taskId, { content })
        this.draft = ''
        await this.loadComments()
        this.$emit('changed')
        this.$message.success('评论成功')
      } finally {
        this.submitting = false
      }
    },
    toggleReply(id) {
      if (this.replyingId === id) {
        this.cancelReply()
        return
      }
      this.replyingId = id
      this.replyDraft = ''
    },
    cancelReply() {
      this.replyingId = null
      this.replyDraft = ''
    },
    async submitReply(id) {
      const content = (this.replyDraft || '').trim()
      if (!content) {
        this.$message.warning('请输入回复内容')
        return
      }
      this.replySubmitting = true
      try {
        await replyTaskComment(id, { content })
        this.cancelReply()
        await this.loadComments()
        this.$emit('changed')
        this.$message.success('回复成功')
      } finally {
        this.replySubmitting = false
      }
    },
    async removeComment(id) {
      try {
        await this.$confirm('确认删除这条评论吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      await deleteTaskComment(id)
      await this.loadComments()
      this.$emit('changed')
      this.$message.success('删除成功')
    },
    getReplies(comment) {
      return Array.isArray(comment && comment.replies) ? comment.replies : []
    },
    isDeleted(item) {
      return !!(item && (item.deleted || item.status === 'deleted' || item.status === 'hidden'))
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
  }
}
</script>

<style scoped>
.task-comment-panel { display: flex; flex-direction: column; gap: 16px; }
.editor-card, .comment-card, .reply-card { border: 1px solid #ebeef5; border-radius: 12px; padding: 12px; background: #fff; }
.editor-actions { margin-top: 10px; display: flex; justify-content: flex-end; gap: 8px; }
.empty-wrap { padding: 20px 0; background: #fff; border: 1px solid #ebeef5; border-radius: 12px; }
.comment-list, .reply-list { display: flex; flex-direction: column; gap: 12px; }
.reply-list { margin-top: 12px; padding-left: 20px; }
.comment-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.author-wrap { display: flex; align-items: center; gap: 10px; }
.author-name { font-weight: 600; color: #303133; }
.comment-time { font-size: 12px; color: #909399; margin-top: 2px; }
.comment-content { margin-top: 10px; white-space: pre-wrap; word-break: break-word; color: #303133; }
.comment-content.deleted { color: #909399; font-style: italic; }
.reply-box { margin-top: 12px; }
.danger-text { color: #f56c6c; }
</style>
