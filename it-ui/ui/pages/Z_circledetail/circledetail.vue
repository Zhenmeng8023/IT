<template>
  <div class="post-detail-container">
    <!-- 帖子内容卡片 -->
    <el-card class="post-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="loading">
      <div class="post-kicker">
        <span class="post-channel">圈子讨论</span>
        <span class="post-id">主题 #{{ postId }}</span>
      </div>
      <h1 class="post-title">{{ post.title || '圈子讨论详情' }}</h1>
      <div class="post-meta">
        <div class="post-author">
          <el-avatar :size="50" :src="post.avatar"></el-avatar>
          <div class="post-author-info">
            <span class="author-name">{{ post.author }}</span>
            <span class="publish-date">发布于 {{ post.publishDate }}</span>
          </div>
        </div>
        <div class="post-overview">
          <span>{{ totalComments }} 条评论</span>
          <span>持续交流中</span>
        </div>
      </div>
      <div class="post-content" v-html="post.content"></div>
    </el-card>

    <!-- 评论区卡片 -->
    <el-card class="comment-section" shadow="hover">
      <div slot="header" class="comment-header">
        <span>评论（{{ totalComments }}）</span>
        <div class="comment-tools">
          <!-- 只看楼主开关 -->
          <el-switch
            v-model="onlyAuthor"
            active-text="只看楼主"
            inactive-text="全部评论"
            @change="handleFilterChange"
          >
          </el-switch>
        </div>
      </div>

      <!-- 全局评论输入框（发表顶级评论） -->
      <div class="comment-input-area">
        <el-input
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          v-model="newComment"
          resize="none"
        ></el-input>
        <div v-if="!canSubmitComment" class="comment-warning">
          未获取到圈子ID，暂不可发表评论，请刷新后重试。
        </div>
        <div class="comment-submit">
          <el-button type="primary" @click="submitTopLevelComment" :disabled="!newComment.trim() || !canSubmitComment || !userId" :loading="submitting">
            发表评论
          </el-button>
        </div>
      </div>

      <!-- 评论列表 -->
      <div class="comment-list" v-loading="commentLoading">
        <!-- 顶级评论循环 -->
        <div v-for="topComment in topLevelComments" :key="topComment.id" class="comment-thread">
          <!-- 顶级评论 -->
          <div class="comment-item">
            <el-avatar :size="40" :src="topComment.avatar"></el-avatar>
            <div class="comment-content">
              <div class="comment-meta">
                <span class="comment-author">{{ topComment.nickname }}</span>
                <span class="comment-time">{{ formatTime(topComment.createTime) }}</span>
              </div>
              <div class="comment-text">{{ topComment.content }}</div>
              <div class="comment-actions">
                <el-button type="text" size="small" @click="showReplyInput(topComment)">
                  回复
                </el-button>
                <!-- 可选点赞 -->
                <!-- <el-button type="text" size="small">点赞 {{ topComment.likeCount }}</el-button> -->
              </div>
            </div>
          </div>

          <!-- 回复输入框（针对顶级评论） -->
          <div v-if="replyTarget && replyTarget.id === topComment.id" class="reply-input-wrapper">
            <el-input
              type="textarea"
              :rows="2"
              :placeholder="'回复 @' + topComment.nickname"
              v-model="replyContent"
              resize="none"
            ></el-input>
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply(topComment)" :loading="replySubmitting" :disabled="!replyContent.trim() || !canSubmitComment || !userId">
                提交回复
              </el-button>
            </div>
          </div>

          <!-- 递归显示所有层级的回复 -->
          <div v-if="topComment.children && topComment.children.length" class="replies">
            <div v-for="reply in topComment.children" :key="reply.id" class="reply-item">
              <el-avatar :size="30" :src="reply.avatar"></el-avatar>
              <div class="reply-content">
                <div class="comment-meta">
                  <span class="comment-author">{{ reply.nickname }}</span>
                  <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button type="text" size="small" @click="showReplyInput(reply, topComment)">
                    回复
                  </el-button>
                </div>
              </div>

              <!-- 针对回复的回复输入框（仍作为该顶级评论的子级） -->
              <div v-if="replyTarget && replyTarget.id === reply.id" class="reply-input-wrapper nested">
                <el-input
                  type="textarea"
                  :rows="2"
                  :placeholder="'回复 @' + reply.nickname"
                  v-model="replyContent"
                  resize="none"
                ></el-input>
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button type="primary" size="small" @click="submitReply(topComment, reply)" :loading="replySubmitting" :disabled="!replyContent.trim() || !canSubmitComment || !userId">
                    提交回复
                  </el-button>
                </div>
              </div>

              <!-- 递归显示嵌套回复 -->
              <div v-if="reply.children && reply.children.length" class="replies nested">
                <div v-for="nestedReply in reply.children" :key="nestedReply.id" class="reply-item">
                  <el-avatar :size="25" :src="nestedReply.avatar"></el-avatar>
                  <div class="reply-content">
                    <div class="comment-meta">
                      <span class="comment-author">{{ nestedReply.nickname }}</span>
                      <span class="comment-time">{{ formatTime(nestedReply.createTime) }}</span>
                    </div>
                    <div class="comment-text">{{ nestedReply.content }}</div>
                    <div class="comment-actions">
                      <el-button type="text" size="small" @click="showReplyInput(nestedReply, topComment)">
                        回复
                      </el-button>
                    </div>
                  </div>

                  <!-- 针对嵌套回复的回复输入框 -->
                  <div v-if="replyTarget && replyTarget.id === nestedReply.id" class="reply-input-wrapper nested">
                    <el-input
                      type="textarea"
                      :rows="2"
                      :placeholder="'回复 @' + nestedReply.nickname"
                      v-model="replyContent"
                      resize="none"
                    ></el-input>
                    <div class="reply-actions">
                      <el-button size="small" @click="cancelReply">取消</el-button>
                      <el-button type="primary" size="small" @click="submitReply(topComment, nestedReply)" :loading="replySubmitting" :disabled="!replyContent.trim() || !canSubmitComment || !userId">
                        提交回复
                      </el-button>
                    </div>
                  </div>

                  <!-- 递归显示更深层次的回复 -->
                  <div v-if="nestedReply.children && nestedReply.children.length" class="replies nested">
                    <div v-for="deepReply in nestedReply.children" :key="deepReply.id" class="reply-item">
                      <el-avatar :size="20" :src="deepReply.avatar"></el-avatar>
                      <div class="reply-content">
                        <div class="comment-meta">
                          <span class="comment-author">{{ deepReply.nickname }}</span>
                          <span class="comment-time">{{ formatTime(deepReply.createTime) }}</span>
                        </div>
                        <div class="comment-text">{{ deepReply.content }}</div>
                        <div class="comment-actions">
                          <el-button type="text" size="small" @click="showReplyInput(deepReply, topComment)">
                            回复
                          </el-button>
                        </div>
                      </div>

                      <!-- 针对深层回复的回复输入框 -->
                      <div v-if="replyTarget && replyTarget.id === deepReply.id" class="reply-input-wrapper nested">
                        <el-input
                          type="textarea"
                          :rows="2"
                          :placeholder="'回复 @' + deepReply.nickname"
                          v-model="replyContent"
                          resize="none"
                        ></el-input>
                        <div class="reply-actions">
                          <el-button size="small" @click="cancelReply">取消</el-button>
                          <el-button type="primary" size="small" @click="submitReply(topComment, deepReply)" :loading="replySubmitting" :disabled="!replyContent.trim() || !canSubmitComment || !userId">
                            提交回复
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 没有评论时显示 -->
        <div v-if="!topLevelComments.length" class="no-comment">
          暂无评论，快来抢沙发吧～
        </div>
      </div>
    </el-card>

    <!-- 回到顶部按钮 -->
    <el-backtop target=".post-detail-container" :bottom="100" :right="40">
      <div class="backtop-inner">
        <i class="el-icon-arrow-up"></i>
        <span>顶部</span>
      </div>
    </el-backtop>
  </div>
</template>

<script>
import { pickAvatarUrl } from '@/utils/avatar'
import {
  createCircleComment,
  extractApiErrorMessage,
  getCirclePostComments,
  getCirclePostDetail
} from '@/api/circlePublicComment'

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

function normalizeId(value) {
  if (value === null || value === undefined) {
    return null
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }

  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') {
    return null
  }

  if (/^\d+$/.test(text)) {
    return Number(text)
  }

  return text
}

function toComparableId(value) {
  const normalized = normalizeId(value)
  return normalized === null ? '' : String(normalized)
}

function normalizeTimestamp(value) {
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

export default {
  layout: 'circle',
  data() {
    return {
      postId: normalizeId(this.$route.params.id),
      circleId: normalizeId(this.$route.params.circleId || this.$route.query.circleId),
      loading: false,
      commentLoading: false,
      currentUser: null,
      userId: null,
      username: '',
      userAvatar: '',
      rawComments: [],
      post: {
        id: null,
        title: '圈子讨论详情',
        author: '匿名用户',
        avatar: DEFAULT_AVATAR,
        publishDate: '',
        content: '暂无内容',
        likes: 0,
        replyCount: 0,
        circleId: null
      },
      sortOrder: 'desc',
      onlyAuthor: false,
      newComment: '',
      submitting: false,
      replyTarget: null,
      replyContent: '',
      replySubmitting: false,
      circleIdWarned: false
    }
  },

  watch: {
    '$route.params.id': {
      handler() {
        this.syncRouteParams()
        this.resetReplyState()
        this.newComment = ''
        this.rawComments = []
        this.circleIdWarned = false
        this.initializePage()
      }
    }
  },

  computed: {
    resolvedCircleId() {
      return normalizeId(this.circleId || (this.post && this.post.circleId))
    },
    canSubmitComment() {
      return Boolean(this.resolvedCircleId)
    },
    totalComments() {
      return this.rawComments.length
    },
    filteredComments() {
      if (!this.onlyAuthor) {
        return this.rawComments
      }

      const postAuthor = this.parseAuthorInfo(this.post.author).toString().trim().toLowerCase()
      if (!postAuthor) {
        return this.rawComments
      }

      return this.rawComments.filter(comment => {
        const commentAuthor = this.parseAuthorInfo(comment.nickname || comment.author).toString().trim().toLowerCase()
        return commentAuthor === postAuthor
      })
    },
    sortedComments() {
      return [...this.filteredComments].sort((a, b) => {
        const timeDiff = normalizeTimestamp(a.createTime) - normalizeTimestamp(b.createTime)
        return this.sortOrder === 'asc' ? timeDiff : -timeDiff
      })
    },
    topLevelComments() {
      const commentMap = new Map()
      const roots = []
      const postIdKey = toComparableId(this.postId)

      this.sortedComments.forEach(comment => {
        const key = toComparableId(comment.id)
        if (!key) {
          return
        }
        commentMap.set(key, {
          ...comment,
          children: []
        })
      })

      commentMap.forEach(comment => {
        const parentKey = toComparableId(comment.parentCommentId)
        if (!parentKey || parentKey === postIdKey) {
          roots.push(comment)
          return
        }

        const parent = commentMap.get(parentKey)
        if (parent) {
          parent.children.push(comment)
          return
        }

        roots.push(comment)
      })

      const sortRecursively = comments => {
        comments.sort((a, b) => {
          const timeDiff = normalizeTimestamp(a.createTime) - normalizeTimestamp(b.createTime)
          return this.sortOrder === 'asc' ? timeDiff : -timeDiff
        })

        comments.forEach(comment => {
          if (comment.children && comment.children.length) {
            sortRecursively(comment.children)
          }
        })
      }

      sortRecursively(roots)
      return roots
    }
  },

  created() {
    this.syncRouteParams()
    this.initializePage()
  },

  mounted() {
    this.getCurrentUser()
  },

  methods: {
    syncRouteParams() {
      this.postId = normalizeId(this.$route.params.id)
      this.circleId = normalizeId(this.$route.params.circleId || this.$route.query.circleId)
    },
    resetReplyState() {
      this.replyTarget = null
      this.replyContent = ''
    },
    async initializePage() {
      if (!this.postId) {
        this.$message.error('帖子ID缺失，无法加载详情')
        this.loading = false
        this.commentLoading = false
        return
      }

      await Promise.all([this.fetchPostDetail(), this.fetchComments()])
    },
    unwrapData(payload) {
      if (!payload || typeof payload !== 'object') {
        return payload
      }

      if (payload.data !== undefined && payload.data !== null) {
        return payload.data
      }

      if (
        payload.code !== undefined &&
        payload.data !== undefined &&
        payload.data !== null
      ) {
        return payload.data
      }

      return payload
    },
    async getCurrentUser() {
      try {
        const res = await this.$axios.get('/api/users/current')
        const userData = this.unwrapData(res)

        if (!userData || !userData.id) {
          return
        }

        this.currentUser = userData
        this.userId = userData.id
        this.username = userData.username || userData.nickname || userData.name || ''
        this.userAvatar = pickAvatarUrl(userData.avatarUrl, userData.avatar, DEFAULT_AVATAR)
      } catch (error) {
        // 公开页允许未登录访问，这里静默处理
      }
    },
    normalizePost(postData) {
      return {
        id: normalizeId(postData.id) || this.postId,
        title: postData.title || `帖子 #${this.postId}`,
        author: this.parseAuthorInfo(
          postData.author || postData.nickname || postData.username || '匿名用户'
        ),
        avatar: pickAvatarUrl(
          postData.avatarUrl,
          postData.avatar,
          postData.author && postData.author.avatarUrl,
          postData.author && postData.author.avatar,
          DEFAULT_AVATAR
        ),
        publishDate: this.formatTime(postData.createdAt || postData.createTime || new Date().toISOString()),
        content: postData.content || '暂无内容',
        likes: Number(postData.likes || 0),
        replyCount: Number(postData.replyCount || 0),
        circleId: normalizeId(postData.circleId || this.circleId)
      }
    },
    getParentCommentId(comment) {
      if (
        comment.parentCommentId !== undefined &&
        comment.parentCommentId !== null &&
        comment.parentCommentId !== ''
      ) {
        return comment.parentCommentId
      }
      if (
        comment.parent_id !== undefined &&
        comment.parent_id !== null &&
        comment.parent_id !== ''
      ) {
        return comment.parent_id
      }
      if (
        comment.parent_comment_id !== undefined &&
        comment.parent_comment_id !== null &&
        comment.parent_comment_id !== ''
      ) {
        return comment.parent_comment_id
      }
      return null
    },
    normalizeComment(comment) {
      const authorSource = comment.author || comment.user || comment.creator || {}
      const nickname = this.parseAuthorInfo(
        comment.nickname ||
          comment.username ||
          (authorSource && authorSource.nickname) ||
          (authorSource && authorSource.username) ||
          (authorSource && authorSource.name) ||
          '匿名用户'
      )

      return {
        id: normalizeId(comment.id || comment.commentId),
        parentCommentId: normalizeId(this.getParentCommentId(comment)),
        postId: normalizeId(comment.postId || comment.post_id || this.postId),
        author: this.parseAuthorInfo(authorSource),
        nickname,
        avatar: pickAvatarUrl(
          comment.avatarUrl,
          comment.avatar,
          comment.userAvatar,
          authorSource && authorSource.avatarUrl,
          authorSource && authorSource.avatar,
          DEFAULT_AVATAR
        ),
        createTime: comment.createTime || comment.createdAt || comment.create_date || new Date().toISOString(),
        publishDate: comment.createTime || comment.createdAt || comment.create_date || new Date().toISOString(),
        content: comment.content || comment.body || '',
        likeCount: Number(comment.likeCount || comment.likes || 0)
      }
    },
    notifyMissingCircleId() {
      if (this.circleIdWarned) {
        return
      }
      this.circleIdWarned = true
      this.$message.warning('未获取到圈子ID，评论功能暂不可用，请刷新后重试')
    },
    async fetchPostDetail() {
      this.loading = true
      try {
        const postData = await getCirclePostDetail(this.postId)
        if (!postData || !normalizeId(postData.id)) {
          this.$message.error('获取帖子失败：数据不完整')
          return
        }

        this.post = this.normalizePost(postData)
        this.circleId = normalizeId(this.post.circleId || this.circleId)

        if (!this.resolvedCircleId) {
          this.notifyMissingCircleId()
        }
      } catch (error) {
        this.$message.error(extractApiErrorMessage(error, '网络错误，无法获取帖子详情'))
      } finally {
        this.loading = false
      }
    },
    async fetchComments() {
      this.commentLoading = true
      try {
        const commentsData = await getCirclePostComments(this.postId)
        const currentPostId = toComparableId(this.postId)

        this.rawComments = commentsData
          .map(comment => this.normalizeComment(comment))
          .filter(comment => {
            const commentId = toComparableId(comment.id)
            return commentId && commentId !== currentPostId
          })
      } catch (error) {
        this.$message.error(extractApiErrorMessage(error, '网络错误，无法获取评论'))
      } finally {
        this.commentLoading = false
      }
    },
    formatTime(time) {
      if (!time) return ''

      const date = new Date(time)
      if (!Number.isFinite(date.getTime())) {
        return ''
      }

      const year = date.getFullYear()
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}`
    },
    handleSortChange() {},
    handleFilterChange() {},
    ensureCommentContext(action) {
      if (!this.postId) {
        this.$message.warning(`帖子ID缺失，暂无法${action}`)
        return null
      }

      if (!this.resolvedCircleId) {
        this.$message.warning(`未获取到圈子ID，暂无法${action}`)
        return null
      }

      return this.resolvedCircleId
    },
    async submitTopLevelComment() {
      const content = this.newComment.trim()
      if (!content) return

      if (!this.userId) {
        this.$message.warning('请先登录后再发表评论')
        return
      }

      const circleId = this.ensureCommentContext('发表评论')
      if (!circleId) return

      this.submitting = true
      try {
        await createCircleComment({
          circleId,
          parentCommentId: this.postId,
          content,
          authorId: this.userId
        })

        this.newComment = ''
        this.$message.success('评论发表成功')
        await this.fetchComments()
      } catch (error) {
        this.$message.error(extractApiErrorMessage(error, '评论发表失败'))
      } finally {
        this.submitting = false
      }
    },
    showReplyInput(comment) {
      this.replyTarget = comment
      this.replyContent = ''
    },
    cancelReply() {
      this.resetReplyState()
    },
    parseAuthorInfo(authorInfo) {
      if (!authorInfo) return '匿名用户'

      if (typeof authorInfo === 'string') {
        const text = authorInfo.trim()
        if (!text) {
          return '匿名用户'
        }

        try {
          const parsed = JSON.parse(text)
          if (parsed && typeof parsed === 'object') {
            return parsed.nickname || parsed.username || parsed.name || text
          }
        } catch (error) {
          return text
        }

        return text
      }

      if (typeof authorInfo === 'object') {
        return authorInfo.nickname || authorInfo.username || authorInfo.name || '匿名用户'
      }

      return String(authorInfo)
    },
    async submitReply(topComment, replyTo = null) {
      const content = this.replyContent.trim()
      if (!content) return

      if (!this.userId) {
        this.$message.warning('请先登录后再回复')
        return
      }

      const circleId = this.ensureCommentContext('提交回复')
      if (!circleId) return

      const parentCommentId = normalizeId(
        (replyTo && replyTo.id) || (topComment && topComment.id)
      )

      if (!parentCommentId) {
        this.$message.error('父评论ID缺失，无法提交回复')
        return
      }

      this.replySubmitting = true
      try {
        await createCircleComment({
          circleId,
          authorId: this.userId,
          parentCommentId,
          content
        })

        this.replyContent = ''
        this.replyTarget = null
        this.$message.success('回复成功')
        await this.fetchComments()
      } catch (error) {
        this.$message.error(extractApiErrorMessage(error, '回复失败'))
      } finally {
        this.replySubmitting = false
      }
    }
  }
}
</script>

<style scoped>
.post-detail-container {
  max-width: 1020px;
  margin: 24px auto;
  padding: 0 20px 40px;
  min-height: 100vh;
  position: relative;
  background:
    radial-gradient(circle at top left, rgba(2, 132, 199, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.12), transparent 30%);
}

.post-header,
.comment-section {
  border-radius: 28px !important;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 22px 46px rgba(15, 23, 42, 0.08) !important;
  background: rgba(255, 255, 255, 0.94) !important;
  backdrop-filter: blur(14px);
}

.post-header {
  margin-bottom: 22px;
  overflow: hidden;
}

.post-kicker,
.post-meta,
.post-author,
.comment-header,
.comment-tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.post-kicker {
  gap: 10px;
  margin-bottom: 16px;
}

.post-channel,
.post-id,
.post-overview span {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.post-channel {
  background: #e0f2fe;
  color: #0284c7;
}

.post-id {
  background: #eff6ff;
  color: #1d4ed8;
}

.post-title {
  margin: 0 0 18px;
  font-size: clamp(2rem, 4vw, 3rem);
  color: #0f172a;
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.post-meta {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.post-author {
  gap: 14px;
}

.post-author-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 1.05rem;
  font-weight: 700;
  color: #0284c7;
}

.publish-date {
  color: #64748b;
  font-size: 0.92rem;
}

.post-overview {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.post-overview span {
  background: #f8fafc;
  color: #475569;
}

.post-content {
  font-size: 1.03rem;
  line-height: 1.9;
  color: #334155;
}

.post-content h2,
.post-content h3 {
  color: #0f172a;
  margin-top: 26px;
  margin-bottom: 14px;
}

.post-content p {
  margin: 14px 0;
}

.post-content pre {
  background: #0f172a;
  color: #e2e8f0;
  padding: 18px;
  border-radius: 16px;
  overflow-x: auto;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.18);
}

.post-content code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 6px;
  color: #db2777;
}

.post-content pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

.post-content ul,
.post-content ol {
  padding-left: 22px;
}

.comment-section :deep(.el-card__header) {
  border-bottom: none;
  padding-bottom: 6px;
}

.comment-section {
  overflow: hidden;
}

.comment-header {
  justify-content: space-between;
  gap: 15px;
}

.comment-tools {
  gap: 15px;
}

.comment-input-area {
  margin: 18px 0 24px;
  padding: 18px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 20px;
}

.comment-input-area :deep(.el-textarea__inner),
.reply-input-wrapper :deep(.el-textarea__inner) {
  border-radius: 14px;
  border: 1px solid #dbeafe;
  min-height: 92px;
  background: #ffffff;
}

.comment-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.comment-warning {
  margin-top: 10px;
  font-size: 13px;
  color: #d97706;
}

.comment-submit .el-button,
.reply-actions .el-button--primary {
  border-radius: 999px;
}

.comment-list {
  margin-top: 6px;
}

.comment-thread {
  margin-bottom: 22px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 22px;
}

.comment-thread:last-child {
  border-bottom: none;
}

.comment-item,
.reply-item {
  display: flex;
  gap: 14px;
}

.comment-item {
  margin-bottom: 10px;
}

.reply-item {
  margin-left: 55px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

.reply-item .el-avatar {
  flex-shrink: 0;
}

.comment-content,
.reply-content {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  margin-bottom: 6px;
}

.comment-author {
  font-weight: 700;
  color: #0284c7;
  margin-right: 10px;
}

.comment-time {
  color: #94a3b8;
  font-size: 0.85rem;
}

.comment-text {
  color: #334155;
  line-height: 1.75;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-actions {
  margin-top: 8px;
}

.comment-actions .el-button {
  padding: 0;
  margin-right: 15px;
  color: #64748b;
  font-weight: 600;
}

.comment-actions .el-button:hover {
  color: #0284c7;
}

.reply-input-wrapper {
  margin-top: 15px;
  margin-left: 55px;
  padding: 16px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 18px;
}

.reply-input-wrapper.nested {
  margin-left: 0;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.no-comment {
  text-align: center;
  padding: 34px 20px;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 18px;
}

.backtop-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #0284c7, #2563eb);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.backtop-inner:hover {
  transform: scale(1.08);
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.35);
}

.backtop-inner i {
  font-size: 20px;
  margin-bottom: 2px;
}

@media screen and (max-width: 768px) {
  .post-detail-container {
    margin: 16px auto;
    padding: 0 12px 32px;
  }

  .post-title {
    font-size: 2rem;
  }

  .post-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .reply-item,
  .reply-input-wrapper {
    margin-left: 28px;
  }
}

@media screen and (max-width: 480px) {
  .post-title {
    font-size: 1.7rem;
  }

  .comment-item,
  .reply-item {
    flex-direction: column;
  }

  .reply-item,
  .reply-input-wrapper {
    margin-left: 0;
  }

  .post-overview {
    gap: 8px;
  }
}
</style>
<style scoped>
.post-detail-container {
  position: relative;
  background:
    radial-gradient(circle at top left, rgba(45, 212, 191, 0.15), transparent 28%),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 22%),
    linear-gradient(180deg, #06101b 0%, #0a1626 46%, #07111d 100%);
}

.post-detail-container::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.04) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.7), transparent 90%);
}

.post-header,
.comment-section {
  background: rgba(8, 15, 29, 0.74) !important;
  border: 1px solid rgba(148, 163, 184, 0.18) !important;
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.4);
  backdrop-filter: blur(22px);
}

.post-channel,
.post-overview span {
  background: rgba(45, 212, 191, 0.12) !important;
  color: #99f6e4 !important;
  border-color: rgba(153, 246, 228, 0.18) !important;
}

.post-id {
  color: #7dd3fc !important;
}

.post-title,
.comment-header span,
.comment-author {
  color: #f8fafc !important;
}

.author-name,
.publish-date,
.comment-time,
.comment-text,
.no-comment {
  color: #cbd5e1 !important;
}

.post-content,
.post-content :deep(p),
.post-content :deep(li) {
  color: #dbeafe !important;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3) {
  color: #f8fafc !important;
}

.post-content :deep(pre) {
  background: rgba(2, 6, 23, 0.9);
  border: 1px solid rgba(125, 211, 252, 0.14);
  color: #e2e8f0;
}

.post-content :deep(code) {
  background: rgba(15, 23, 42, 0.9);
  color: #7dd3fc;
}

.comment-input-area,
.reply-input-wrapper,
.comment-thread,
.reply-item,
.no-comment {
  background: rgba(15, 23, 42, 0.64) !important;
  border-color: rgba(148, 163, 184, 0.14) !important;
}

.comment-thread {
  border-bottom-color: rgba(148, 163, 184, 0.14) !important;
}

.reply-item {
  border-top-color: rgba(148, 163, 184, 0.12) !important;
}

.comment-input-area :deep(.el-textarea__inner),
.reply-input-wrapper :deep(.el-textarea__inner) {
  background: rgba(2, 6, 23, 0.6);
  border-color: rgba(148, 163, 184, 0.18);
  color: #e2e8f0;
}

.comment-input-area :deep(.el-textarea__inner:focus),
.reply-input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: rgba(125, 211, 252, 0.45);
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.16);
}

.comment-submit .el-button,
.reply-actions .el-button--primary {
  background: linear-gradient(135deg, #14b8a6, #2563eb) !important;
  border-color: transparent !important;
  color: #eff6ff !important;
  border-radius: 999px !important;
  box-shadow: 0 16px 30px rgba(20, 184, 166, 0.2);
}

.comment-actions .el-button {
  color: #94a3b8 !important;
}

.comment-actions .el-button:hover {
  color: #7dd3fc !important;
}

.comment-warning {
  color: #fbbf24 !important;
}

.comment-tools :deep(.el-switch__label) {
  color: #cbd5e1;
}

.comment-tools :deep(.el-switch.is-checked .el-switch__core) {
  border-color: #14b8a6;
  background: #14b8a6;
}

.backtop-inner {
  background: linear-gradient(135deg, #14b8a6, #2563eb) !important;
  box-shadow: 0 16px 32px rgba(20, 184, 166, 0.3) !important;
}
</style>

