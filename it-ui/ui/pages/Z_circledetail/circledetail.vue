<template>
  <div data-testid="circle-detail-page" class="circle-detail-page">
    <section class="detail-hero">
      <div class="detail-main-card" v-loading="loading">
        <div class="detail-kicker">
          <el-button type="text" class="detail-back" @click="goBackToCircle">
            <i class="el-icon-arrow-left"></i>
            返回圈子首页
          </el-button>
          <span class="detail-chip detail-chip--accent">圈子讨论</span>
          <span class="detail-chip">帖子 #{{ postId || '--' }}</span>
        </div>

        <h1 data-testid="circle-detail-title" class="detail-title">
          {{ post.title || '圈子讨论详情' }}
        </h1>

        <p class="detail-summary">
          {{ contentExcerpt || '围绕当前圈子主题展开讨论、沉淀经验与协作思路。' }}
        </p>

        <div class="detail-meta-grid">
          <div class="detail-author-card">
            <el-avatar :size="48" :src="post.avatar"></el-avatar>
            <div class="detail-author-copy">
              <span class="detail-author-name">{{ post.author }}</span>
              <span class="detail-author-subline">发布于 {{ post.publishDate }}</span>
            </div>
          </div>

          <div class="detail-meta-tags">
            <span class="detail-chip">{{ totalComments }} 条评论</span>
            <span class="detail-chip">{{ canSubmitComment ? '可参与回复' : '评论暂不可用' }}</span>
            <span class="detail-chip">{{ userId ? '已登录' : '游客浏览' }}</span>
          </div>
        </div>
      </div>

      <aside class="detail-side-card">
        <div class="detail-side-item">
          <span class="detail-side-label">讨论热度</span>
          <strong class="detail-side-value">{{ totalComments }}</strong>
          <p class="detail-side-copy">评论总数会在这里持续更新，方便快速判断活跃度。</p>
        </div>
        <div class="detail-side-item">
          <span class="detail-side-label">当前状态</span>
          <strong class="detail-side-value detail-side-value--text">
            {{ loading ? '加载中' : '内容已就绪' }}
          </strong>
          <p class="detail-side-copy">帖子内容与评论区采用统一层级展示，方便连续阅读。</p>
        </div>
      </aside>
    </section>

    <el-card data-testid="circle-detail-content-card" class="detail-content-card" shadow="never">
      <div class="content-heading">
        <h2>帖子内容</h2>
        <span class="detail-chip detail-chip--muted">公开讨论</span>
      </div>
      <div data-testid="circle-detail-content" class="detail-content" v-html="post.content"></div>
    </el-card>

    <el-card data-testid="circle-comment-section" class="comment-section" shadow="never">
      <div slot="header" class="comment-section-head">
        <div class="comment-section-copy">
          <span class="comment-section-title">评论区</span>
          <span class="comment-section-subtitle">按时间展示当前讨论，支持直接回复任意楼层。</span>
        </div>
        <div class="comment-section-tools">
          <span class="detail-chip detail-chip--muted">{{ totalComments }} 条评论</span>
          <el-switch
            v-model="onlyAuthor"
            active-text="只看楼主"
            inactive-text="全部评论"
            @change="handleFilterChange"
          ></el-switch>
        </div>
      </div>

      <div class="comment-composer">
        <div class="composer-header">
          <div class="composer-user">
            <el-avatar :size="40" :src="userAvatar || post.avatar"></el-avatar>
            <div class="composer-copy">
              <strong>{{ userId ? (username || '当前用户') : '游客浏览' }}</strong>
              <span>{{ composerHint }}</span>
            </div>
          </div>
          <span class="detail-chip detail-chip--accent">参与讨论</span>
        </div>

        <el-input
          data-testid="circle-comment-input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="写下你的评论，让讨论更完整"
          v-model="newComment"
        ></el-input>

        <div v-if="commentDisabledReason" class="comment-warning">
          {{ commentDisabledReason }}
        </div>

        <div class="composer-actions">
          <el-button
            data-testid="circle-comment-submit"
            type="primary"
            :disabled="!newComment.trim() || !canSubmitComment || !userId"
            :loading="submitting"
            @click="submitTopLevelComment"
          >
            发表评论
          </el-button>
        </div>
      </div>

      <div data-testid="circle-comment-list" class="comment-list" v-loading="commentLoading">
        <CircleCommentThread
          v-for="topComment in topLevelComments"
          :key="topComment.id"
          :comment="topComment"
          :top-comment="topComment"
          :active-reply-target-id="replyTarget && replyTarget.id"
          :reply-content="replyContent"
          :reply-submitting="replySubmitting"
          :reply-submit-disabled="replySubmitDisabled"
          :can-submit-comment="canSubmitComment"
          :user-id="userId"
          :format-time="formatTime"
          @show-reply="showReplyInput"
          @cancel-reply="cancelReply"
          @update-reply-content="replyContent = $event"
          @submit-reply="handleReplySubmit"
        />

        <div v-if="!commentLoading && !topLevelComments.length" data-testid="circle-comment-empty" class="no-comment">
          <i class="el-icon-chat-line-square"></i>
          <span>暂无评论，快来抢沙发吧～</span>
        </div>
      </div>
    </el-card>

    <el-backtop target=".circle-detail-page" :bottom="88" :right="32">
      <div class="backtop-inner">
        <i class="el-icon-arrow-up"></i>
        <span>顶部</span>
      </div>
    </el-backtop>
  </div>
</template>

<script>
import { pickAvatarUrl } from '@/utils/avatar'
import CircleCommentThread from '@/components/circle/CircleCommentThread.vue'
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

function stripHtml(text) {
  return String(text || '')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export default {
  layout: 'circle',
  components: {
    CircleCommentThread
  },
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
    postAuthorKey() {
      return this.normalizeAuthorKey(this.post.author)
    },
    contentExcerpt() {
      const plainText = stripHtml(this.post.content)
      if (!plainText) {
        return ''
      }

      return plainText.length > 120
        ? `${plainText.slice(0, 120)}...`
        : plainText
    },
    composerHint() {
      if (!this.userId) {
        return '登录后即可发表评论与回复'
      }

      if (!this.canSubmitComment) {
        return '等待圈子信息加载完成后可继续评论'
      }

      return '补充观点、提问或继续跟进当前讨论'
    },
    commentDisabledReason() {
      if (!this.userId) {
        return '请先登录后再发表评论'
      }

      if (!this.canSubmitComment) {
        return '未获取到圈子ID，暂不可发表评论，请刷新后重试。'
      }

      return ''
    },
    replySubmitDisabled() {
      const { replyContent, canSubmitComment, userId } = this
      return !replyContent.trim() || !canSubmitComment || !userId
    },
    filteredComments() {
      if (!this.onlyAuthor) {
        return this.rawComments
      }

      if (!this.postAuthorKey) {
        return this.rawComments
      }

      return this.rawComments.filter(comment => {
        return this.normalizeAuthorKey(comment.nickname || comment.author) === this.postAuthorKey
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
          children: [],
          replyToName: '',
          isPostAuthor: this.isPostAuthorComment(comment.nickname || comment.author)
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
          comment.replyToName = parent.nickname || '匿名用户'
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
    goBackToCircle() {
      this.$router.push({
        path: '/circle',
        query: this.resolvedCircleId ? { circleId: this.resolvedCircleId } : {}
      })
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

      if (payload.code !== undefined && payload.data !== undefined && payload.data !== null) {
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
        // 公开页允许未登录访问
      }
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
    normalizeAuthorKey(authorInfo) {
      return this.parseAuthorInfo(authorInfo).toString().trim().toLowerCase()
    },
    isPostAuthorComment(authorInfo) {
      return Boolean(this.postAuthorKey) && this.normalizeAuthorKey(authorInfo) === this.postAuthorKey
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
      if (comment.parentCommentId !== undefined && comment.parentCommentId !== null && comment.parentCommentId !== '') {
        return comment.parentCommentId
      }
      if (comment.parent_id !== undefined && comment.parent_id !== null && comment.parent_id !== '') {
        return comment.parent_id
      }
      if (comment.parent_comment_id !== undefined && comment.parent_comment_id !== null && comment.parent_comment_id !== '') {
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
      if (!this.userId) {
        this.$message.warning('请先登录后再回复')
        return
      }

      if (!this.canSubmitComment) {
        this.ensureCommentContext('提交回复')
        return
      }

      this.replyTarget = comment
      this.replyContent = ''
    },
    cancelReply() {
      this.resetReplyState()
    },
    async handleReplySubmit({ topComment, replyTo }) {
      await this.submitReply(topComment, replyTo)
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

      const parentCommentId = normalizeId((replyTo && replyTo.id) || (topComment && topComment.id))

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
.circle-detail-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px var(--it-shell-padding-x) 56px;
  min-height: 100vh;
  color: var(--it-text);
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(280px, 0.9fr);
  gap: 20px;
  margin-bottom: 20px;
}

.detail-main-card,
.detail-side-card,
.detail-content-card,
.comment-section {
  border-radius: 24px !important;
  border: 1px solid var(--it-border) !important;
  background: var(--it-surface) !important;
  box-shadow: var(--it-shadow) !important;
}

.detail-main-card,
.detail-side-card {
  position: relative;
  overflow: hidden;
}

.detail-main-card {
  padding: 28px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--it-accent-soft) 42%, transparent), transparent 52%),
    var(--it-surface) !important;
}

.detail-main-card::after,
.detail-side-card::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.1), transparent 42%);
}

.detail-kicker,
.detail-meta-grid,
.detail-meta-tags,
.content-heading,
.comment-section-head,
.comment-section-tools,
.composer-header,
.composer-user,
.composer-actions,
.backtop-inner {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.detail-kicker {
  gap: 10px;
  margin-bottom: 18px;
}

.detail-back {
  padding: 0;
  font-weight: 600;
  color: var(--it-accent);
}

.detail-back:hover {
  color: var(--it-accent-hover);
}

.detail-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-muted) 84%, transparent);
  color: var(--it-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.detail-chip--accent {
  border-color: transparent;
  background: color-mix(in srgb, var(--it-accent-soft) 92%, transparent);
  color: var(--it-accent);
}

.detail-chip--muted {
  color: var(--it-text-subtle);
}

.detail-title {
  margin: 0 0 14px;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.08;
  color: var(--it-text);
  letter-spacing: -0.03em;
}

.detail-summary {
  margin: 0 0 24px;
  max-width: 760px;
  color: var(--it-text-muted);
  line-height: 1.85;
  font-size: 15px;
}

.detail-meta-grid {
  justify-content: space-between;
  gap: 18px;
}

.detail-author-card {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.detail-author-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-author-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--it-text);
}

.detail-author-subline {
  color: var(--it-text-muted);
  font-size: 13px;
}

.detail-meta-tags {
  justify-content: flex-end;
  gap: 10px;
}

.detail-side-card {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.detail-side-item {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 92%, transparent);
}

.detail-side-label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--it-text-subtle);
  letter-spacing: 0.04em;
}

.detail-side-value {
  display: block;
  margin-bottom: 10px;
  font-size: 2rem;
  line-height: 1.1;
  color: var(--it-text);
}

.detail-side-value--text {
  font-size: 1.15rem;
}

.detail-side-copy {
  margin: 0;
  color: var(--it-text-muted);
  line-height: 1.7;
  font-size: 13px;
}

.detail-content-card,
.comment-section {
  overflow: hidden;
}

.detail-content-card {
  margin-bottom: 20px;
}

.detail-content-card :deep(.el-card__body),
.comment-section :deep(.el-card__body) {
  padding: 24px;
}

.content-heading {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.content-heading h2 {
  margin: 0;
  font-size: 18px;
  color: var(--it-text);
}

.detail-content {
  color: var(--it-text-muted);
  font-size: 15px;
  line-height: 1.95;
}

.detail-content :deep(h1),
.detail-content :deep(h2),
.detail-content :deep(h3) {
  color: var(--it-text);
  margin-top: 28px;
  margin-bottom: 12px;
}

.detail-content :deep(p),
.detail-content :deep(li),
.detail-content :deep(blockquote) {
  color: var(--it-text-muted);
}

.detail-content :deep(a) {
  color: var(--it-accent);
}

.detail-content :deep(code) {
  padding: 2px 8px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--it-accent-soft) 86%, transparent);
  color: var(--it-text);
}

.detail-content :deep(pre) {
  padding: 18px;
  overflow-x: auto;
  border-radius: 16px;
  background: color-mix(in srgb, var(--it-surface-muted) 88%, transparent);
  border: 1px solid var(--it-border);
}

.detail-content :deep(pre code) {
  padding: 0;
  background: transparent;
}

.comment-section :deep(.el-card__header) {
  padding: 22px 24px 0;
  border-bottom: none;
}

.comment-section-head {
  justify-content: space-between;
  gap: 14px;
}

.comment-section-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comment-section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--it-text);
}

.comment-section-subtitle {
  color: var(--it-text-muted);
  font-size: 13px;
}

.comment-section-tools {
  justify-content: flex-end;
  gap: 12px;
}

.comment-composer {
  padding: 22px;
  border-radius: 20px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-muted) 84%, transparent);
}

.composer-header {
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.composer-user {
  gap: 12px;
  min-width: 0;
}

.composer-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.composer-copy strong {
  color: var(--it-text);
  font-size: 14px;
}

.composer-copy span {
  color: var(--it-text-muted);
  font-size: 13px;
}

.comment-composer :deep(.el-textarea__inner) {
  min-height: 112px;
  border-radius: 16px !important;
  border-color: var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text);
  line-height: 1.8;
}

.comment-composer :deep(.el-textarea__inner:focus) {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
}

.comment-warning {
  margin-top: 10px;
  color: #d97706;
  font-size: 13px;
}

.composer-actions {
  justify-content: flex-end;
  margin-top: 14px;
}

.composer-actions .el-button {
  min-width: 112px;
  border-color: transparent;
  background: var(--it-primary-gradient);
}

.comment-list {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.no-comment {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 180px;
  border-radius: 18px;
  border: 1px dashed var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 90%, transparent);
  color: var(--it-text-muted);
  text-align: center;
}

.no-comment i {
  font-size: 30px;
  color: var(--it-accent);
}

.backtop-inner {
  justify-content: center;
  gap: 4px;
  flex-direction: column;
  width: 100%;
  height: 100%;
  color: #fff;
  background: var(--it-primary-gradient);
  border-radius: 14px;
  box-shadow: var(--it-shadow);
}

.backtop-inner i {
  font-size: 18px;
}

@media screen and (max-width: 1024px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .detail-side-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .circle-detail-page {
    padding: 16px 12px 40px;
  }

  .detail-main-card,
  .detail-side-card,
  .detail-content-card :deep(.el-card__body),
  .comment-section :deep(.el-card__body),
  .comment-composer {
    padding: 18px;
  }

  .detail-title {
    font-size: 2rem;
  }

  .detail-meta-grid,
  .comment-section-head,
  .composer-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-side-card {
    grid-template-columns: 1fr;
  }

  .comment-section :deep(.el-card__header) {
    padding: 18px 18px 0;
  }
}

@media screen and (max-width: 480px) {
  .detail-title {
    font-size: 1.7rem;
  }

  .detail-kicker,
  .detail-meta-tags,
  .comment-section-tools {
    gap: 8px;
  }

  .composer-actions,
  .comment-section-tools {
    width: 100%;
    justify-content: flex-start;
  }

  .composer-actions .el-button {
    width: 100%;
  }
}
</style>
