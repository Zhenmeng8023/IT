<template>
  <div class="blog-detail-container">
    <el-card class="blog-header" :body-style="{ padding: '24px 28px' }" shadow="hover" v-loading="detailLoading">
      <div class="blog-title-wrapper">
        <h1 class="blog-title">{{ blog.title }}</h1>
        <el-tag
          v-if="blog.price !== undefined && blog.price !== null"
          :type="getPriceTagType(normalizedPrice)"
          size="small"
          class="price-tag"
          v-text="getPriceTagText(normalizedPrice)"
        ></el-tag>
      </div>

      <div class="blog-meta">
        <el-avatar :size="50" :src="blog.avatar" @click="goToAuthor" style="cursor: pointer;"></el-avatar>
        <span class="author-name" @click="goToAuthor" style="cursor: pointer;">{{ blog.author }}</span>
        <span class="publish-date">发布于 {{ blog.publishDate }}</span>

        <div class="action-buttons">
          <el-button
            :type="blog.isLiked ? 'primary' : 'default'"
            size="small"
            :icon="blog.isLiked ? 'el-icon-sunny' : 'el-icon-sunrise'"
            @click="handleLike"
            circle
            :class="{ 'liked-button': blog.isLiked }"
            :loading="likeLoading"
          ></el-button>
          <span class="like-count">{{ blog.likeCount }}</span>

          <el-button
            :type="blog.isCollected ? 'warning' : 'default'"
            size="small"
            :icon="blog.isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"
            @click="handleCollect"
            circle
            :loading="collectLoading"
          ></el-button>
          <span class="collect-count">{{ blog.collectCount }}</span>

          <el-button
            class="report-button"
            size="small"
            icon="el-icon-warning-outline"
            @click="handleReport"
            :loading="reportLoading"
            :disabled="reportSubmitted || String(blog.authorId) === String(userId)"
          >
            {{ reportSubmitted ? '已举报' : '举报' }}
          </el-button>
        </div>
      </div>

      <div v-if="blog.tags && blog.tags.length" class="blog-tags">
        <el-tag
          v-for="tag in blog.tags"
          :key="tag"
          size="small"
          class="blog-tag-item"
          @click="goToTag(tag)"
        >
          #{{ tag }}
        </el-tag>
      </div>
    </el-card>

    <el-card class="blog-content" :body-style="{ padding: '32px' }" shadow="never">
      <div v-if="isPaidLocked" class="paid-content-wrapper">
        <div class="content-preview" v-html="blog.previewContent"></div>
        <div class="content-blur"></div>
        <div class="paid-prompt">
          <i class="el-icon-lock"></i>
          <h3>此内容为付费文章</h3>
          <p class="price-card">
            <span class="price-label">价格：</span>
            <span class="price-value">¥{{ normalizedPrice }}</span>
          </p>
          <p class="purchase-benefit">购买后可永久查看该内容</p>
          <div class="purchase-actions">
            <el-button type="primary" @click="purchaseBlog">立即购买</el-button>
            <el-button @click="$router.back()">返回上一页</el-button>
          </div>
        </div>
      </div>

      <div v-else-if="isVipLocked" class="vip-content-wrapper">
        <div class="content-preview" v-html="blog.previewContent"></div>
        <div class="content-blur"></div>
        <div class="vip-prompt">
          <i class="el-icon-star-on"></i>
          <h3>此内容为 VIP 专属</h3>
          <p>开通 VIP 会员，畅享全部优质内容</p>
          <div class="vip-actions">
            <el-button type="primary" @click="vipDialogVisible = true">立即开通 VIP</el-button>
            <el-button @click="$router.back()">返回上一页</el-button>
          </div>
        </div>
      </div>

      <div v-else class="content-body" v-html="blog.content"></div>
    </el-card>

    <el-dialog
      title="VIP 专属内容"
      :visible.sync="vipDialogVisible"
      width="400px"
      :close-on-click-modal="false"
    >
      <div style="text-align: center; padding: 20px;">
        <i class="el-icon-star-on" style="font-size: 48px; color: #f5a623;"></i>
        <h3>此内容仅限 VIP 会员阅读</h3>
        <p>开通 VIP 会员，畅享全部优质内容</p>
        <el-button type="primary" @click="goToVipPage" style="margin-top: 20px;">立即开通 VIP</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="确认购买"
      :visible.sync="showPurchaseDialog"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="purchase-confirm-content">
        <p><strong>文章标题：</strong>{{ blog.title }}</p>
        <p><strong>支付方式：</strong>{{ selectedPaymentMethodText }}</p>
        <p class="amount-info">
          <span>原价：</span>
          <span class="original-amount">¥{{ purchaseAmount }}</span>
        </p>
        <p v-if="discountAmount > 0" class="discount-info">
          <span>优惠金额：</span>
          <span class="discount-amount">-¥{{ discountAmount.toFixed(2) }}</span>
        </p>
        <p class="final-amount-info">
          <span>实付金额：</span>
          <span class="final-amount">¥{{ (discountAmount > 0 ? finalAmount : purchaseAmount).toFixed(2) }}</span>
        </p>
        <p class="balance-info">请确认支付并完成购买</p>
      </div>
      <span slot="footer">
        <el-button @click="showPurchaseDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmPayment" :loading="purchaseSubmitting">确认支付</el-button>
      </span>
    </el-dialog>

    <el-card class="recommend-section" shadow="hover" v-loading="recommendLoading">
      <div slot="header" class="recommend-header">
        <div>
          <div class="recommend-title">{{ recommendSectionTitle }}</div>
          <div class="recommend-subtitle">{{ recommendSectionDesc }}</div>
        </div>
        <div class="recommend-badges">
          <el-tag v-if="recommendMeta.source !== 'fallback'" size="mini" type="success" effect="plain">后台联动</el-tag>
          <el-tag
            v-if="recommendMeta.source !== 'fallback' && recommendMeta.algorithmVersion"
            size="mini"
            effect="plain"
          >
            {{ recommendMeta.algorithmVersion }}
          </el-tag>
        </div>
      </div>

      <div v-if="recommendations.length" class="recommend-grid">
        <div
          v-for="item in recommendations"
          :key="item.id"
          class="recommend-card"
          @click="goToRecommendedBlog(item)"
        >
          <div class="recommend-card-head">
            <h3 class="recommend-card-title">{{ item.title || '未命名文章' }}</h3>
            <el-tag
              v-if="item.price !== undefined && item.price !== null"
              size="mini"
              effect="plain"
              :type="getPriceTagType(normalizePrice(item.price))"
            >
              {{ getPriceTagText(normalizePrice(item.price)) }}
            </el-tag>
          </div>
          <p class="recommend-card-summary">{{ resolveRecommendSummary(item) }}</p>
          <div v-if="normalizeTags(item.tags).length" class="recommend-card-tags">
            <span
              v-for="tag in normalizeTags(item.tags).slice(0, 3)"
              :key="`${item.id}-${tag}`"
              class="recommend-tag"
            >
              #{{ tag }}
            </span>
          </div>
          <div class="recommend-card-footer">
            <span>{{ getRecommendAuthorName(item.author) }}</span>
            <span>{{ formatRecommendTime(item.publishTime || item.createdAt) }}</span>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无相关推荐" :image-size="70" />
    </el-card>

    <el-card class="comment-section" shadow="hover" v-loading="commentLoading">
      <div slot="header" class="comment-header">
        <span>评论（{{ totalComments }}）</span>
      </div>

      <div class="comment-list">
        <div v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
          <div :id="`blog-comment-${comment.id}`" class="comment-item">
            <el-avatar :size="40" :src="comment.avatar"></el-avatar>
            <div class="comment-content">
              <div class="comment-meta">
                <span class="comment-author">{{ comment.nickname }}</span>
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                <span v-if="comment.isAuthor" class="author-badge">作者</span>
              </div>
              <div class="comment-text" :class="{ 'deleted-text': comment.deleted }">{{ comment.content }}</div>
              <div class="comment-actions">
                <el-button v-if="!comment.deleted" type="text" size="small" @click="showReplyInput(comment)">
                  <i class="el-icon-chat-line-round"></i> 回复
                </el-button>
                <el-button v-if="comment.canDelete" type="text" size="small" class="delete-action" @click="handleDeleteComment(comment)">
                  <i class="el-icon-delete"></i> 删除
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="replyTarget && replyTarget.id === comment.id && !replyTarget.parentId" class="reply-input-wrapper">
            <el-input
              type="textarea"
              :rows="2"
              :placeholder="'回复 @' + (replyTarget.nickname || '用户')"
              v-model="replyContent"
              resize="none"
            ></el-input>
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply(comment)" :loading="replySubmitting">
                提交回复
              </el-button>
            </div>
          </div>

          <div v-if="comment.replies && comment.replies.length" class="replies">
            <div v-for="reply in comment.replies" :key="reply.id" :id="`blog-comment-${reply.id}`" class="reply-item">
              <el-avatar :size="30" :src="reply.avatar"></el-avatar>
              <div class="reply-content">
                <div class="comment-meta">
                  <span class="comment-author">{{ reply.nickname }}</span>
                  <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                  <span v-if="reply.isAuthor" class="author-badge">作者</span>
                  <span v-if="reply.replyTo" class="reply-to">回复 @{{ reply.replyTo }}</span>
                </div>
                <div class="comment-text" :class="{ 'deleted-text': reply.deleted }">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button v-if="!reply.deleted" type="text" size="small" @click="showReplyInput(reply, comment)">
                    <i class="el-icon-chat-line-round"></i> 回复
                  </el-button>
                  <el-button v-if="reply.canDelete" type="text" size="small" class="delete-action" @click="handleDeleteComment(reply)">
                    <i class="el-icon-delete"></i> 删除
                  </el-button>
                </div>
              </div>

              <div v-if="replyTarget && replyTarget.id === reply.id" class="reply-input-wrapper nested">
                <el-input
                  type="textarea"
                  :rows="2"
                  :placeholder="'回复 @' + (replyTarget.nickname || '用户')"
                  v-model="replyContent"
                  resize="none"
                ></el-input>
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button type="primary" size="small" @click="submitReply(comment, reply)" :loading="replySubmitting">
                    提交回复
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!topLevelComments.length" class="no-comment">
          暂无评论，快来抢沙发吧～
        </div>
      </div>

      <div class="comment-input-area">
        <el-input
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          v-model="newComment"
          resize="none"
        ></el-input>
        <div class="comment-submit">
          <el-button type="primary" @click="submitTopLevelComment" :disabled="!newComment.trim()" :loading="submitting">
            发表评论
          </el-button>
        </div>
      </div>
    </el-card>

    <el-backtop target=".blog-detail-container" :bottom="100" :right="40">
      <div class="backtop-inner">
        <i class="el-icon-arrow-up"></i>
        <span>顶部</span>
      </div>
    </el-backtop>
  </div>
</template>

<script>
import {
  GetCurrentUser,
  GetBlogById,
  GetBlogRecommendations,
  CheckUserLiked,
  DeleteLike,
  AddLike,
  CollectBlog,
  CancelCollectBlog,
  AddComment,
  ReplyComment,
  DeleteComment,
  GetCommentsByPost,
  IsCollected,
  ReportBlog
} from '@/api/index'
import { getUserAvailableCoupons, calculateDiscount } from '@/api/coupon'

export default {
  name: 'BlogDetail',
  data() {
    return {
      blog: {
        id: '',
        like_id: '',
        title: '',
        author: '',
        authorId: '',
        avatar: '',
        publishDate: '',
        likeCount: 0,
        collectCount: 0,
        reportCount: 0,
        isLiked: false,
        isCollected: false,
        tags: [],
        content: '',
        previewContent: '',
        isVipOnly: false,
        price: 0,
        locked: false,
        lockType: 'none',
        hasAccess: true,
        hasPurchased: false,
        isVipUser: false
      },
      comments: [],
      newComment: '',
      submitting: false,
      replyTarget: null,
      replyContent: '',
      replySubmitting: false,
      detailLoading: false,
      commentLoading: false,
      likeLoading: false,
      collectLoading: false,
      reportLoading: false,
      reportSubmitted: false,
      currentProcessingBlogId: null,
      currentUser: null,
      userId: null,
      username: '',
      userAvatar: '',
      vipDialogVisible: false,
      showPurchaseDialog: false,
      purchaseAmount: 0,
      purchaseOrderNo: '',
      purchaseSubmitting: false,
      selectedPaymentMethod: 'alipay',
      recommendations: [],
      recommendLoading: false,
      recommendMeta: {
        source: 'fallback',
        algorithmVersion: '',
        generatedAt: ''
      },
      // 优惠券相关
      availableCoupons: [],
      selectedCouponId: null,
      discountAmount: 0,
      finalAmount: 0,
      couponLoading: false
    }
  },
  computed: {
    topLevelComments() {
      const m = new Map()
      const a = []

      this.comments.forEach(i => {
        m.set(i.id, { ...i, replies: [] })
      })

      this.comments.forEach(i => {
        const c = m.get(i.id)
        if (i.parentId === null) {
          a.push(c)
        } else {
          let p = m.get(i.parentId)
          if (p) {
            let t = p
            while (t.parentId) {
              t = m.get(t.parentId)
            }
            if (t) {
              t.replies.push(c)
            }
          }
        }
      })

      const s = list => {
        list.sort((x, y) => new Date(x.createTime) - new Date(y.createTime))
        list.forEach(i => {
          if (i.replies && i.replies.length) {
            i.replies.sort((x, y) => new Date(x.createTime) - new Date(y.createTime))
          }
        })
      }

      s(a)
      return a
    },
    totalComments() {
      return this.comments.length
    },
    normalizedPrice() {
      const n = Number(this.blog.price)
      return Number.isFinite(n) ? n : 0
    },
    isPaidLocked() {
      return this.blog.locked === true && this.blog.lockType === 'paid'
    },
    isVipLocked() {
      return this.blog.locked === true && this.blog.lockType === 'vip'
    },
    recommendSectionTitle() {
      if (this.recommendMeta.source === 'algorithm') return '算法推荐'
      if (this.recommendMeta.source === 'mixed') return '算法推荐 + 智能补充'
      return '相关推荐'
    },
    recommendSectionDesc() {
      if (this.recommendMeta.source === 'algorithm') return '当前结果来自后台算法推荐缓存'
      if (this.recommendMeta.source === 'mixed') return '优先展示后台算法结果，并补充了当前文章的相似内容'
      return '后台算法结果为空时，会回退到作者、标签与热度相关的内容'
    },
    selectedPaymentMethodText() {
      return this.selectedPaymentMethod === 'wechat' ? '微信支付' : '支付宝'
    },
    // 是否有可用优惠券
    hasAvailableCoupons() {
      return this.availableCoupons && this.availableCoupons.length > 0
    }
  },
  created() {
    const blogId = this.$route.params.id
    if (blogId) {
      this.loadBlogPage(blogId)
    }
  },
  watch: {
    '$route.params.id': {
      async handler(newId, oldId) {
        if (!newId || String(newId) === String(oldId)) return
        await this.loadBlogPage(newId)
      }
    }
  },
  methods: {
    async loadBlogPage(blogId) {
      if (!blogId) return
      this.blog.id = blogId
      this.comments = []
      this.recommendations = []
      this.recommendMeta = {
        source: 'fallback',
        algorithmVersion: '',
        generatedAt: ''
      }
      this.reportSubmitted = false
      this.newComment = ''
      this.cancelReply()
      await this.getBlogDetail(blogId)
      await this.getCurrentUser()
    },
    unwrapResponse(res) {
      if (!res) return null
      if (res.data === undefined) return res
      const d = res.data
      if (
        d &&
        typeof d === 'object' &&
        Object.prototype.hasOwnProperty.call(d, 'data') &&
        (
          Object.prototype.hasOwnProperty.call(d, 'code') ||
          Object.prototype.hasOwnProperty.call(d, 'message') ||
          Object.prototype.hasOwnProperty.call(d, 'success')
        )
      ) {
        return d.data
      }
      return d
    },
    normalizePrice(v) {
      const n = Number(v)
      return Number.isFinite(n) ? n : 0
    },
    normalizeTags(v) {
      if (Array.isArray(v)) return v
      if (v && typeof v === 'object') return Object.values(v)
      return []
    },
    stripHtmlText(value) {
      if (!value) return ''
      return String(value)
        .replace(/<script[\s\S]*?<\/script>/gi, ' ')
        .replace(/<style[\s\S]*?<\/style>/gi, ' ')
        .replace(/<[^>]+>/g, ' ')
        .replace(/&nbsp;/g, ' ')
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/\s+/g, ' ')
        .trim()
    },
    resolvePaymentMethod(v) {
      const s = String(v || '').trim().toLowerCase()
      return s === 'wechat' ? 'wechat' : 'alipay'
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      return d.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    getPriceTagType(price) {
      if (price === 0) return 'success'
      if (price === -1) return 'warning'
      return 'primary'
    },
    getPriceTagText(price) {
      if (price === 0) return '免费'
      if (price === -1) return 'VIP'
      return `¥${price}`
    },
    goToTag(tag) {
      if (!tag) return
      this.$router.push({
        path: '/blog',
        query: {
          tag,
          type: 'tag',
          page: 1
        }
      })
    },
    getRecommendAuthorName(author) {
      if (!author) return '未知作者'
      if (typeof author === 'string') return author
      return author.displayName || author.nickname || author.username || '未知作者'
    },
    formatRecommendTime(value) {
      if (!value) return '最近更新'
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return '最近更新'
      return d.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      })
    },
    resolveRecommendSummary(item) {
      if (!item) return '暂无摘要'
      const raw = item.summary || this.stripHtmlText(item.previewContent || item.content)
      if (!raw) return '暂无摘要'
      return raw.length > 88 ? `${raw.slice(0, 88)}...` : raw
    },
    async getCurrentUser() {
      try {
        const r = await GetCurrentUser()
        const u = this.unwrapResponse(r)
        if (u) {
          this.currentUser = u
          this.userId = u.id
          this.username = u.username || u.nickname || ''
          this.userAvatar = u.avatar || u.avatarUrl || ''
          this.checkLikeStatus()
          this.checkCollectStatus()
          if (this.blog.id) {
            await this.getComments()
          }
        }
      } catch (e) {
        if (e.response && e.response.status === 404) {
          return
        }
        console.error('获取当前用户信息失败', e)
      }
    },
    async getBlogDetail(blogId) {
      this.currentProcessingBlogId = blogId
      this.detailLoading = true
      try {
        const r = await GetBlogById(blogId)
        if (this.currentProcessingBlogId !== blogId) {
          return
        }

        const d = this.unwrapResponse(r)
        if (d) {
          this.reportSubmitted = false

          let authorName = '未知作者'
          let authorId = ''
          let authorAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

          if (d.author) {
            if (typeof d.author === 'object') {
              authorName = d.author.displayName || d.author.nickname || d.author.username || '未知作者'
              authorId = d.author.id || ''
              authorAvatar = d.author.avatar || authorAvatar
            } else {
              authorName = d.author
            }
          }

          let publishDate = ''
          if (d.publishTime) {
            publishDate = new Date(d.publishTime).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            })
          } else if (d.createdAt) {
            publishDate = new Date(d.createdAt).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            })
          }

          const p = this.normalizePrice(d.price)
          const tags = this.normalizeTags(d.tags)

          const info = {
            id: blogId,
            like_id: '',
            title: d.title || '无标题',
            author: authorName,
            authorId: authorId,
            avatar: authorAvatar,
            publishDate: publishDate,
            likeCount: d.likeCount || 0,
            collectCount: d.collectCount || 0,
            reportCount: d.reportCount || 0,
            isLiked: false,
            isCollected: false,
            tags: tags,
            content: d.content || '',
            previewContent: d.previewContent || '',
            isVipOnly: d.isVipOnly === true || p === -1,
            price: p,
            locked: d.locked === true,
            lockType: d.lockType || 'none',
            hasAccess: d.hasAccess !== false,
            hasPurchased: d.hasPurchased === true,
            isVipUser: d.isVipUser === true
          }

          this.$set(this, 'blog', info)
          this.purchaseAmount = p > 0 ? p : 0
          await Promise.all([
            this.getComments(),
            this.fetchRecommendations(blogId)
          ])
        }
      } catch (e) {
        console.error('获取博客详情失败', e)
        this.$message.error('获取博客详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    async fetchRecommendations(blogId) {
      if (!blogId) return

      const currentBlogId = String(blogId)
      this.recommendLoading = true
      try {
        const r = await GetBlogRecommendations(blogId, { size: 6 })
        if (String(this.blog.id) !== currentBlogId) {
          return
        }

        const data = this.unwrapResponse(r) || {}
        this.recommendations = Array.isArray(data.items) ? data.items : []
        this.recommendMeta = {
          source: data.source || 'fallback',
          algorithmVersion: data.algorithmVersion || '',
          generatedAt: data.generatedAt || ''
        }
      } catch (e) {
        if (String(this.blog.id) !== currentBlogId) {
          return
        }
        console.error('获取博客推荐失败', e)
        this.recommendations = []
        this.recommendMeta = {
          source: 'fallback',
          algorithmVersion: '',
          generatedAt: ''
        }
      } finally {
        if (String(this.blog.id) === currentBlogId) {
          this.recommendLoading = false
        }
      }
    },
    async getComments() {
      if (!this.blog.id) return

      const currentBlogId = this.blog.id
      this.commentLoading = true
      try {
        const r = await GetCommentsByPost(currentBlogId)
        if (this.blog.id !== currentBlogId) {
          return
        }

        let data = this.unwrapResponse(r)
        if (!Array.isArray(data)) {
          data = []
        }

        const list = data.map(i => this.convertCommentData(i))
        list.forEach(i => {
          if (i.parentId) {
            const p = list.find(x => x.id === i.parentId)
            if (p) {
              i.replyTo = p.nickname || '匿名用户'
            }
          }
        })

        this.comments = list
        this.$nextTick(() => this.scrollToCommentFromQuery())
      } catch (e) {
        if (this.blog.id !== currentBlogId) {
          return
        }
        console.error('获取评论列表失败', e)
        this.$message.error('获取评论列表失败：' + (e.message || '网络错误'))
      } finally {
        this.commentLoading = false
      }
    },
    convertCommentData(comment, allComments = null) {
      let nickname = '匿名用户'
      let avatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
      const authorId = comment.authorId || comment.author?.id || this.userId
      const deleted = comment.deleted === true || (comment.status || '').toLowerCase() === 'deleted'
      const isCommentOwner = String(authorId || '') === String(this.userId || '')
      const isBlogOwner = String(this.userId || '') === String(this.blog.authorId || '')

      if (String(authorId || '') === String(this.blog.authorId || '')) {
        nickname = this.blog.author
        avatar = this.blog.avatar
      } else if (comment.nickname) {
        nickname = comment.nickname
        avatar = comment.avatar || avatar
      } else if (comment.author) {
        if (typeof comment.author === 'object') {
          nickname = comment.author.displayName || comment.author.nickname || comment.author.username || '匿名用户'
          avatar = comment.author.avatar || avatar
        } else {
          nickname = comment.author
        }
      } else if (comment.username) {
        nickname = comment.username
      } else if (this.username) {
        nickname = this.username
        avatar = this.userAvatar || avatar
      }

      const c = {
        id: comment.id || comment.commentId,
        parentId: comment.parentCommentId || comment.parent_id || null,
        postId: comment.postId || comment.post_id || this.blog.id,
        content: comment.content || comment.body || '',
        nickname: nickname,
        avatar: avatar,
        createTime: comment.createTime || comment.createdAt || comment.createDate || new Date().toISOString(),
        likeCount: comment.likes || 0,
        status: comment.status || 'normal',
        deleted: deleted,
        canDelete: comment.canDelete === true || (!deleted && (isCommentOwner || isBlogOwner)),
        authorId: authorId,
        replyTo: '',
        isAuthor: String(authorId || '') === String(this.blog.authorId || '')
      }

      if (c.parentId) {
        let p = null
        if (allComments) {
          p = allComments.find(x => x.id === c.parentId)
        } else {
          p = this.comments.find(x => x.id === c.parentId)
        }
        if (p) {
          c.replyTo = p.nickname || '匿名用户'
        }
      }

      return c
    },
    async checkCollectStatus() {
      if (!this.userId || !this.blog.id) return

      const currentBlogId = this.blog.id
      try {
        const r = await IsCollected(this.userId, 'blog', this.blog.id)
        if (this.blog.id !== currentBlogId) {
          return
        }
        const d = this.unwrapResponse(r)
        const ok = !!d
        this.blog.isCollected = ok
        this.blog.collect_id = ok && d.id ? d.id : ''
      } catch (e) {
        if (this.blog.id !== currentBlogId) {
          return
        }
        this.blog.isCollected = false
        this.blog.collect_id = ''
      }
    },
    async checkLikeStatus() {
      if (!this.userId || !this.blog.id) return

      const currentBlogId = this.blog.id
      try {
        const r = await CheckUserLiked(this.userId, 'blog', this.blog.id)
        if (this.blog.id !== currentBlogId) {
          return
        }
        const d = this.unwrapResponse(r)
        const ok = !!d
        this.blog.isLiked = ok
        this.blog.like_id = ok && d.id ? d.id : ''
      } catch (e) {
        if (this.blog.id !== currentBlogId) {
          return
        }
        this.blog.isLiked = false
        this.blog.like_id = ''
      }
    },
    async handleLike() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }

      this.likeLoading = true
      try {
        if (this.blog.isLiked) {
          if (this.blog.like_id) {
            await DeleteLike(this.blog.like_id)
            this.blog.isLiked = false
            this.blog.likeCount = Math.max(0, Number(this.blog.likeCount || 0) - 1)
            this.blog.like_id = ''
            this.$message.success('取消点赞成功')
          }
        } else {
          const r = await AddLike({
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          })
          const d = this.unwrapResponse(r)
          if (d && d.id) {
            this.blog.isLiked = true
            this.blog.likeCount = Number(this.blog.likeCount || 0) + 1
            this.blog.like_id = d.id
            this.$message.success('点赞成功')
          }
        }
      } catch (e) {
        console.error('处理点赞失败', e)
        this.$message.error('操作失败，请稍后重试')
      } finally {
        this.likeLoading = false
      }
    },
    async handleCollect() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }

      this.collectLoading = true
      try {
        if (this.blog.isCollected) {
          if (this.blog.collect_id) {
            await CancelCollectBlog(this.blog.collect_id)
            this.blog.isCollected = false
            this.blog.collectCount = Math.max(0, Number(this.blog.collectCount || 0) - 1)
            this.blog.collect_id = ''
            this.$message.success('取消收藏成功')
          }
        } else {
          const r = await CollectBlog({
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          })
          const d = this.unwrapResponse(r)
          if (d && d.id) {
            this.blog.isCollected = true
            this.blog.collectCount = Number(this.blog.collectCount || 0) + 1
            this.blog.collect_id = d.id
            this.$message.success('收藏成功')
          }
        }
      } catch (e) {
        console.error('处理收藏失败', e)
        this.$message.error('操作失败，请稍后重试')
      } finally {
        this.collectLoading = false
      }
    },
    async handleReport() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }

      if (String(this.blog.authorId) === String(this.userId)) {
        this.$message.warning('不能举报自己的博客')
        return
      }

      try {
        const { value } = await this.$prompt('请输入举报原因', '举报博客', {
          confirmButtonText: '提交举报',
          cancelButtonText: '取消',
          inputPlaceholder: '请简要说明举报原因',
          inputValidator: v => {
            const s = (v || '').trim()
            if (!s) return '举报原因不能为空'
            if (s.length < 2) return '举报原因至少输入 2 个字'
            return true
          }
        })

        this.reportLoading = true
        await ReportBlog(this.blog.id, { reason: value.trim() })
        this.reportSubmitted = true
        this.$message.success('举报成功，我们会尽快处理')
      } catch (e) {
        if (e === 'cancel' || e?.message === 'cancel') {
          return
        }
        console.error('举报博客失败', e)
        const msg = e.response?.data?.message || e.message || '举报失败，请稍后重试'
        if (String(msg).includes('已经举报') || String(msg).includes('重复')) {
          this.reportSubmitted = true
        }
        this.$message.error(msg)
      } finally {
        this.reportLoading = false
      }
    },
    goToAuthor() {
      if (this.blog.authorId) {
        this.$router.push(`/other/${this.blog.authorId}`)
      }
    },
    goToRecommendedBlog(item) {
      if (!item || !item.id) return
      if (String(item.id) === String(this.blog.id)) return
      this.$router.push(`/blog/${item.id}`)
    },
    goToVipPage() {
      this.vipDialogVisible = false
      this.$router.push('/wallet')
    },
    async purchaseBlog() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }

      this.purchaseSubmitting = true
      try {
        const { value } = await this.$prompt('请选择支付方式：输入 wechat 或 alipay', '支付方式', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '输入 wechat 或 alipay',
          inputValue: this.selectedPaymentMethod,
          inputValidator: v => {
            const s = String(v || '').trim().toLowerCase()
            if (s !== 'wechat' && s !== 'alipay') {
              return '请输入 wechat 或 alipay'
            }
            return true
          }
        })

        const paymentMethod = this.resolvePaymentMethod(value)
        this.selectedPaymentMethod = paymentMethod
        
        // 加载可用优惠券
        await this.loadAvailableCoupons()
        
        // 如果有可用优惠券，让用户选择
        let userCouponId = null
        if (this.hasAvailableCoupons) {
          try {
            const couponOptions = this.availableCoupons.map(c => ({
              label: `${c.couponName} (${c.couponType === 'DISCOUNT' ? c.value + '折' : '减¥' + c.value})`,
              value: c.id
            }))
            
            const { value: selectedCoupon } = await this.$prompt(
              '请选择要使用的优惠券（输入编号，0表示不使用）',
              '选择优惠券',
              {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                inputPlaceholder: '输入优惠券编号',
                inputValue: '0',
                inputValidator: v => {
                  const n = parseInt(v)
                  if (isNaN(n) || n < 0 || n >= this.availableCoupons.length) {
                    return `请输入 0-${this.availableCoupons.length - 1} 之间的数字`
                  }
                  return true
                }
              }
            )
            
            const selectedIndex = parseInt(selectedCoupon)
            if (selectedIndex > 0) {
              userCouponId = this.availableCoupons[selectedIndex].id
              // 计算优惠后的金额
              await this.onCouponChange(userCouponId)
            }
          } catch (e) {
            if (e !== 'cancel' && e?.message !== 'cancel') {
              console.warn('优惠券选择取消，将按原价购买')
            }
          }
        }

        const r = await this.$axios.post('/api/content-purchase/create-order', {
          blogId: this.blog.id,
          paymentMethod: paymentMethod,
          userCouponId: userCouponId
        }, {
          headers: { 'X-User-Id': this.userId }
        })

        const d = this.unwrapResponse(r) || {}
        if (d.success) {
          if (d.alreadyPurchased) {
            this.$message.success('您已购买过该博客')
            await this.getBlogDetail(this.blog.id)
          } else {
            this.purchaseAmount = d.amount || this.normalizedPrice
            this.purchaseOrderNo = d.orderNo || ''
            // 如果后端返回了优惠信息，使用后端的数据
            if (d.discountAmount > 0) {
              this.discountAmount = d.discountAmount
              this.finalAmount = d.amount
            } else {
              this.finalAmount = this.purchaseAmount
            }
            this.showPurchaseDialog = true
          }
        } else {
          this.$message.error(d.message || '创建订单失败')
        }
      } catch (e) {
        if (e === 'cancel' || e?.message === 'cancel') {
          return
        }
        console.error('创建订单失败', e)
        this.$message.error('创建订单失败：' + (e.response?.data?.message || e.message || '网络错误'))
      } finally {
        this.purchaseSubmitting = false
      }
    },
    // 加载可用优惠券
    async loadAvailableCoupons() {
      if (!this.userId) return
      
      this.couponLoading = true
      try {
        const res = await getUserAvailableCoupons(this.userId)
        const data = this.unwrapResponse(res)
        if (data && Array.isArray(data)) {
          this.availableCoupons = data
        }
      } catch (e) {
        console.error('加载优惠券失败', e)
        this.availableCoupons = []
      } finally {
        this.couponLoading = false
      }
    },
    // 选择优惠券
    async onCouponChange(couponId) {
      if (!couponId) {
        this.selectedCouponId = null
        this.discountAmount = 0
        this.finalAmount = this.purchaseAmount
        return
      }
      
      try {
        const res = await calculateDiscount({
          couponId: couponId,
          orderAmount: this.purchaseAmount
        })
        const data = this.unwrapResponse(res)
        if (data && data.success) {
          this.selectedCouponId = couponId
          this.discountAmount = data.discountAmount || 0
          this.finalAmount = data.finalAmount || this.purchaseAmount
        }
      } catch (e) {
        console.error('计算优惠金额失败', e)
        this.$message.warning('优惠券计算失败，将按原价支付')
        this.selectedCouponId = null
        this.discountAmount = 0
        this.finalAmount = this.purchaseAmount
      }
    },
    async confirmPayment() {
      if (!this.purchaseOrderNo) return

      this.purchaseSubmitting = true
      try {
        await this.$axios.post('/api/content-purchase/complete', null, {
          params: {
            blogId: this.blog.id,
            orderNo: this.purchaseOrderNo
          },
          headers: { 'X-User-Id': this.userId }
        })

        this.showPurchaseDialog = false
        this.$message.success('购买成功')
        await this.getBlogDetail(this.blog.id)
      } catch (e) {
        console.error('支付失败', e)
        this.$message.error('支付失败：' + (e.response?.data?.message || e.message || '网络错误'))
      } finally {
        this.purchaseSubmitting = false
      }
    },
    showReplyInput(comment) {
      this.replyTarget = comment
      this.replyContent = ''
    },
    cancelReply() {
      this.replyTarget = null
      this.replyContent = ''
    },
    scrollToCommentFromQuery() {
      const commentId = this.$route.query.commentId
      if (!commentId) return
      const target = document.getElementById(`blog-comment-${commentId}`)
      if (!target) return
      target.scrollIntoView({ behavior: 'smooth', block: 'center' })
      if (this.$route.query.highlight) {
        target.classList.add('comment-highlight')
        window.setTimeout(() => target.classList.remove('comment-highlight'), 2500)
      }
    },
    async handleDeleteComment(comment) {
      if (!comment || !comment.id) return
      try {
        await this.$confirm('删除后会保留楼层结构，并显示“该评论已删除”。确定继续吗？', '删除评论', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await DeleteComment(comment.id)
        if (this.replyTarget && this.replyTarget.id === comment.id) {
          this.cancelReply()
        }
        await this.getComments()
        this.$message.success('评论已删除')
      } catch (e) {
        if (e === 'cancel' || e?.message === 'cancel') {
          return
        }
        console.error('删除评论失败', e)
        this.$message.error(e.response?.data?.message || '删除评论失败，请稍后重试')
      }
    },
    async submitTopLevelComment() {
      if (!this.newComment.trim()) return
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }

      this.submitting = true
      try {
        const r = await AddComment({
          content: this.newComment,
          parentCommentId: null,
          postId: this.blog.id,
          authorId: this.userId
        })
        const d = this.unwrapResponse(r)
        if (d && d.id) {
          this.newComment = ''
          await this.getComments()
          this.$message.success('评论发表成功')
        }
      } catch (e) {
        console.error('发表评论出错', e)
        this.$message.error('网络错误，请稍后重试')
      } finally {
        this.submitting = false
      }
    },
    async submitReply(parentComment, replyToComment = null) {
      if (!this.replyContent.trim()) return
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }

      this.replySubmitting = true
      try {
        const r = await ReplyComment({
          content: this.replyContent,
          parentCommentId: replyToComment ? replyToComment.id : parentComment.id,
          postId: this.blog.id,
          authorId: this.userId
        })
        const d = this.unwrapResponse(r)
        if (d && d.id) {
          this.replyTarget = null
          this.replyContent = ''
          await this.getComments()
          this.$message.success('回复发表成功')
        }
      } catch (e) {
        console.error('发表回复出错', e)
        this.$message.error('网络错误，请稍后重试')
      } finally {
        this.replySubmitting = false
      }
    }
  }
}
</script>

<style scoped>
.blog-detail-container {
  max-width: 900px;
  margin: 30px auto;
  padding: 0 20px;
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.blog-header {
  margin-bottom: 25px;
  border-radius: 20px !important;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
  background: white !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.blog-header:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.05) !important;
}

.blog-header::before {
  content: '';
  position: absolute;
  inset: auto -12% -38% 48%;
  height: 220px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.18), transparent 70%);
  pointer-events: none;
}

.blog-title-wrapper {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.blog-title {
  margin: 0;
  font-size: 2.2rem;
  color: #1e293b;
  line-height: 1.3;
  font-weight: 600;
  background: linear-gradient(135deg, #1e293b, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  word-break: break-word;
}

.price-tag {
  border: none;
  font-weight: 600;
  border-radius: 12px;
  padding: 4px 12px;
  font-size: 13px;
  white-space: nowrap;
}

.blog-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
  justify-content: space-between;
  padding-top: 4px;
}

.blog-meta > .el-avatar {
  flex-shrink: 0;
  border: 3px solid #dbeafe;
  box-shadow: 0 10px 18px rgba(59, 130, 246, 0.12);
}

.author-name {
  font-size: 1.2rem;
  font-weight: 500;
  color: #3b82f6;
  cursor: pointer;
  transition: color 0.2s;
}

.author-name:hover {
  color: #2563eb;
}

.publish-date {
  color: #64748b;
  font-size: 0.95rem;
}

.action-buttons {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding: 10px 12px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.like-count,
.collect-count {
  margin-right: 10px;
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.action-buttons .el-button {
  border: none;
  background: #f1f5f9;
  color: #475569;
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: scale(1.08);
  background: #e2e8f0;
}

.action-buttons .el-button.el-button--primary {
  background: #3b82f6;
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.action-buttons .el-button.el-button--warning {
  background: #f59e0b;
  color: white;
  box-shadow: 0 4px 10px rgba(245, 158, 11, 0.2);
}

.action-buttons .report-button {
  margin-left: 4px;
  border-radius: 999px;
  padding: 0 16px;
  background: #fff1f2;
  color: #dc2626;
  border: 1px solid #fecdd3;
  box-shadow: none;
}

.action-buttons .report-button:hover:not(.is-disabled) {
  background: #ffe4e6;
  color: #be123c;
  border-color: #fda4af;
}

.liked-button {
  animation: pulse 0.5s ease-in-out;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.blog-content {
  border-radius: 20px !important;
  background-color: white !important;
  margin-bottom: 25px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06);
}

.content-body {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
  max-width: 100%;
}

.content-body h2 {
  margin: 28px 0 16px;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
  color: #1e293b;
}

.content-body p {
  margin: 16px 0;
}

.content-body code {
  background-color: #f1f5f9;
  padding: 2px 6px;
  border-radius: 6px;
  font-family: 'Fira Code', 'Courier New', monospace;
  color: #e11d48;
}

.content-body pre {
  background-color: #0f172a;
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  border: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.content-body pre code {
  background-color: transparent;
  padding: 0;
  color: #e2e8f0;
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 0.95rem;
}

.content-body img {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin: 20px 0;
}

.paid-content-wrapper,
.vip-content-wrapper {
  position: relative;
  overflow: hidden;
}

.content-preview {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #334155;
  max-height: 300px;
  overflow: hidden;
  position: relative;
  pointer-events: none;
  user-select: none;
  max-width: 100%;
}

.blog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.blog-tag-item {
  border: none;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

.recommend-section,
.comment-section {
  border-radius: 24px !important;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06) !important;
}

.recommend-section :deep(.el-card__header),
.comment-section :deep(.el-card__header) {
  border-bottom: none;
  padding-bottom: 6px;
}

.recommend-section :deep(.el-card__body),
.comment-section :deep(.el-card__body) {
  padding-top: 0;
}

.content-blur {
  position: absolute;
  bottom: 120px;
  left: 0;
  right: 0;
  height: 180px;
  background: linear-gradient(to bottom, transparent, rgba(255, 255, 255, 0.99));
  backdrop-filter: blur(8px);
  z-index: 2;
}

.vip-prompt,
.paid-prompt {
  position: relative;
  margin-top: 20px;
  padding: 32px 20px;
  text-align: center;
  border-radius: 16px;
  z-index: 3;
}

.vip-prompt {
  background: linear-gradient(135deg, #fef3c7 0%, #fef9e7 100%);
  border: 2px solid #fcd34d;
}

.vip-prompt i {
  font-size: 42px;
  color: #f59e0b;
  margin-bottom: 15px;
  display: block;
}

.vip-prompt h3 {
  margin: 0 0 8px;
  color: #92400e;
  font-size: 18px;
  font-weight: 700;
}

.vip-prompt p {
  margin: 0 0 20px;
  color: #78350f;
  font-size: 14px;
  line-height: 1.6;
}

.vip-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  flex-wrap: wrap;
}

.vip-actions .el-button {
  border-radius: 25px;
  padding: 10px 30px;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
}

.vip-actions .el-button--primary {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: none;
  box-shadow: 0 4px 15px rgba(245, 158, 11, 0.3);
}

.vip-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 158, 11, 0.4);
}

.paid-prompt {
  background: linear-gradient(135deg, #e0e7ff 0%, #f0f4ff 100%);
  border: 2px solid #6366f1;
}

.paid-prompt i {
  font-size: 48px;
  color: #6366f1;
  margin-bottom: 15px;
  display: block;
}

.paid-prompt h3 {
  margin: 0 0 15px;
  color: #3730a3;
  font-size: 20px;
  font-weight: 700;
}

.price-card {
  margin: 20px 0;
  padding: 15px 18px;
  background: white;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.price-label {
  font-size: 14px;
  color: #6b7280;
}

.price-value {
  font-size: 28px;
  font-weight: bold;
  color: #ef4444;
}

.purchase-benefit {
  margin: 15px 0 25px;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
}

.purchase-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.purchase-actions .el-button--primary {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}

.purchase-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.purchase-confirm-content p {
  margin: 10px 0;
  font-size: 15px;
  color: #374151;
}

.amount-info .amount {
  font-size: 28px;
  font-weight: bold;
  color: #ef4444;
  display: block;
  margin-top: 10px;
}

.original-amount {
  font-size: 18px;
  font-weight: 600;
  color: #6b7280;
  text-decoration: line-through;
}

.discount-info {
  margin: 10px 0;
  padding: 10px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 8px;
  border-left: 4px solid #f59e0b;
}

.discount-info .discount-amount {
  font-size: 20px;
  font-weight: bold;
  color: #dc2626;
  display: block;
  margin-top: 5px;
}

.final-amount-info {
  margin: 15px 0;
  padding: 12px;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
}

.final-amount-info .final-amount {
  font-size: 24px;
  font-weight: bold;
  color: #1e40af;
  display: block;
  margin-top: 5px;
}

.balance-info {
  color: #6b7280;
  font-size: 13px;
  margin-top: 15px;
}

.blog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.blog-tag-item {
  cursor: pointer;
}

.recommend-section {
  margin-bottom: 25px;
  border-radius: 20px !important;
  background-color: white !important;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.recommend-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.recommend-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #0f172a;
}

.recommend-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.recommend-badges {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  padding: 0 20px 20px;
}

.recommend-card {
  padding: 18px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.recommend-card:hover {
  transform: translateY(-3px);
  border-color: #93c5fd;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.12);
}

.recommend-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.recommend-card-title {
  margin: 0;
  flex: 1;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.5;
  font-weight: 600;
}

.recommend-card-summary {
  margin: 12px 0 0;
  min-height: 66px;
  color: #475569;
  font-size: 13px;
  line-height: 1.75;
}

.recommend-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.recommend-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.08);
  color: #2563eb;
  font-size: 12px;
  line-height: 1;
}

.recommend-card-footer {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
}

.comment-section {
  border-radius: 20px !important;
  background-color: white !important;
  border: 1px solid rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.comment-header {
  font-weight: 600;
  font-size: 1.1rem;
  color: #1e293b;
  padding: 15px 20px;
  border-bottom: 1px solid #e2e8f0;
}

.comment-list {
  margin-bottom: 20px;
  padding: 0 20px;
}

.comment-thread {
  margin-bottom: 25px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 25px;
}

.comment-thread:last-child {
  border-bottom: none;
}

.comment-item {
  display: flex;
  gap: 15px;
  padding: 10px 0;
}

.comment-highlight {
  background: rgba(59, 130, 246, 0.08);
  border-radius: 12px;
  transition: background 0.3s ease;
}

.comment-item .el-avatar {
  border: 2px solid #e2e8f0;
  transition: border-color 0.2s;
}

.comment-item:hover .el-avatar {
  border-color: #3b82f6;
}

.comment-content {
  flex: 1;
}

.comment-meta {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-author {
  font-weight: 600;
  color: #3b82f6;
  margin-right: 10px;
}

.comment-time {
  color: #94a3b8;
  font-size: 0.85rem;
}

.comment-text {
  color: #334155;
  line-height: 1.6;
}

.deleted-text {
  color: #94a3b8;
  font-style: italic;
}

.author-badge {
  font-size: 12px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  border-radius: 20px;
  font-weight: normal;
  display: inline-block;
}

.replies {
  margin-left: 55px;
  margin-top: 15px;
}

.reply-item {
  display: flex;
  gap: 12px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #e2e8f0;
}

.reply-item:first-child {
  border-top: none;
  padding-top: 0;
}

.reply-content {
  flex: 1;
}

.reply-to {
  font-size: 12px;
  color: #94a3b8;
  background-color: #f1f5f9;
  padding: 2px 8px;
  border-radius: 20px;
  margin-left: 5px;
}

.comment-actions {
  margin-top: 8px;
}

.comment-actions .el-button {
  padding: 0;
  margin-right: 15px;
  font-size: 13px;
  color: #64748b;
  transition: color 0.2s;
}

.comment-actions .el-button:hover {
  color: #3b82f6;
  background: transparent !important;
}

.comment-actions .delete-action:hover {
  color: #ef4444;
}

.comment-actions .el-button i {
  margin-right: 3px;
  font-size: 14px;
}

.reply-input-wrapper {
  margin-top: 15px;
  margin-left: 55px;
  padding: 15px;
  background-color: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
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

.comment-input-area {
  margin-top: 20px;
  padding: 0 20px 20px;
}

.comment-input-area .el-textarea__inner {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.comment-input-area .el-textarea__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.comment-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-submit .el-button {
  border-radius: 30px;
  padding: 10px 25px;
  font-weight: 500;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
  transition: transform 0.2s, box-shadow 0.2s;
}

.comment-submit .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.2);
}

.comment-submit .el-button:disabled {
  background: #cbd5e1;
  opacity: 0.6;
}

.no-comment {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 12px;
  margin: 20px;
}

.backtop-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.backtop-inner:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.4);
}

.backtop-inner i {
  font-size: 20px;
  margin-bottom: 2px;
}

@media screen and (max-width: 768px) {
  .blog-detail-container {
    padding: 0 12px 24px;
    margin: 18px auto;
  }

  .blog-title {
    font-size: 1.8rem;
  }

  .blog-title-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .blog-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .action-buttons {
    margin-left: 0;
    justify-content: flex-start;
    width: 100%;
    border-radius: 20px;
  }

  .recommend-header {
    flex-direction: column;
  }

  .recommend-badges {
    justify-content: flex-start;
  }

  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .replies {
    margin-left: 30px;
  }

  .reply-input-wrapper {
    margin-left: 30px;
  }
}

@media screen and (max-width: 480px) {
  .blog-title {
    font-size: 1.5rem;
  }

  .blog-header,
  .blog-content,
  .recommend-section,
  .comment-section {
    border-radius: 20px !important;
  }

  .comment-item {
    flex-direction: column;
    gap: 10px;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .comment-item .el-avatar {
    align-self: flex-start;
  }

  .replies {
    margin-left: 20px;
  }

  .price-card {
    flex-direction: column;
    align-items: center;
  }
}
</style>
<style scoped>
.blog-detail-container {
  position: relative;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.16), transparent 28%),
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.08), transparent 22%),
    linear-gradient(180deg, #06101b 0%, #0a1626 45%, #07111d 100%);
}

.blog-detail-container::before {
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

.blog-header,
.blog-content,
.recommend-section,
.comment-section {
  background: rgba(8, 15, 29, 0.74) !important;
  border: 1px solid rgba(148, 163, 184, 0.18) !important;
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.4);
  backdrop-filter: blur(22px);
}

.blog-title,
.recommend-title,
.comment-header span,
.paid-prompt h3,
.vip-prompt h3 {
  color: #f8fafc !important;
}

.author-name,
.publish-date,
.like-count,
.collect-count,
.recommend-subtitle,
.recommend-card-summary,
.comment-text,
.comment-time,
.purchase-confirm-content,
.balance-info,
.purchase-benefit,
.vip-prompt p,
.paid-prompt p {
  color: #cbd5e1 !important;
}

.blog-tag-item,
.recommend-tag,
.reply-to {
  background: rgba(14, 165, 233, 0.14) !important;
  color: #7dd3fc !important;
  border-color: rgba(125, 211, 252, 0.2) !important;
}

.action-buttons {
  background: rgba(15, 23, 42, 0.78) !important;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.blog-content {
  overflow: hidden;
}

.content-body,
.content-preview,
.comment-author,
.recommend-card-title,
.recommend-card-footer {
  color: #e2e8f0 !important;
}

.content-body :deep(h1),
.content-body :deep(h2),
.content-body :deep(h3),
.content-preview :deep(h1),
.content-preview :deep(h2),
.content-preview :deep(h3) {
  color: #f8fafc;
}

.content-body :deep(p),
.content-preview :deep(p),
.content-body :deep(li),
.content-preview :deep(li) {
  color: #cbd5e1;
}

.content-body :deep(code),
.content-preview :deep(code) {
  background: rgba(15, 23, 42, 0.9);
  color: #7dd3fc;
}

.content-body :deep(pre),
.content-preview :deep(pre) {
  background: rgba(2, 6, 23, 0.9);
  border: 1px solid rgba(125, 211, 252, 0.14);
  color: #e2e8f0;
}

.recommend-card,
.comment-thread,
.reply-input-wrapper,
.comment-input-area {
  background: rgba(15, 23, 42, 0.64) !important;
  border-color: rgba(148, 163, 184, 0.14) !important;
}

.recommend-card:hover {
  border-color: rgba(125, 211, 252, 0.28) !important;
  box-shadow: 0 20px 40px rgba(2, 6, 23, 0.4) !important;
}

.comment-actions .el-button,
.delete-action {
  color: #94a3b8 !important;
}

.comment-actions .el-button:hover {
  color: #7dd3fc !important;
}

.delete-action:hover {
  color: #fca5a5 !important;
}

.comment-input-area :deep(.el-textarea__inner),
.reply-input-wrapper :deep(.el-textarea__inner) {
  background: rgba(2, 6, 23, 0.6) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  color: #e2e8f0 !important;
}

.comment-input-area :deep(.el-textarea__inner:focus),
.reply-input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: rgba(125, 211, 252, 0.45) !important;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.16) !important;
}

.comment-submit .el-button,
.reply-actions .el-button--primary,
.purchase-actions .el-button--primary,
.vip-actions .el-button--primary {
  background: linear-gradient(135deg, #0ea5e9, #2563eb) !important;
  border-color: transparent !important;
  color: #eff6ff !important;
  box-shadow: 0 16px 30px rgba(14, 165, 233, 0.22);
}

.purchase-actions .el-button,
.vip-actions .el-button,
.reply-actions .el-button,
.comment-submit .el-button {
  border-radius: 999px !important;
}

.no-comment {
  background: rgba(15, 23, 42, 0.68) !important;
  color: #94a3b8 !important;
}

.backtop-inner {
  background: linear-gradient(135deg, #0ea5e9, #2563eb) !important;
  box-shadow: 0 16px 32px rgba(14, 165, 233, 0.34) !important;
}

:deep(.el-dialog) {
  background: linear-gradient(180deg, rgba(8, 15, 29, 0.96), rgba(12, 23, 39, 0.94));
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 24px;
  box-shadow: 0 30px 70px rgba(2, 6, 23, 0.6);
}

:deep(.el-dialog__title),
:deep(.el-dialog__body strong) {
  color: #f8fafc;
}

:deep(.el-dialog__body),
:deep(.el-dialog__body p),
:deep(.el-dialog__body span) {
  color: #cbd5e1;
}

:deep(.el-dialog__footer .el-button--primary) {
  background: linear-gradient(135deg, #0ea5e9, #2563eb);
  border-color: transparent;
  border-radius: 999px;
}


.blog-detail-container {
  max-width: 1040px;
  padding: 12px 24px 72px;
  background: transparent !important;
}

.blog-header {
  position: relative;
}

.blog-header::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 20px;
  pointer-events: none;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.03), transparent 36%, rgba(14, 165, 233, 0.04));
}

.blog-meta {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  grid-template-rows: auto auto;
  column-gap: 16px;
  row-gap: 6px;
  align-items: center;
}

.blog-meta > .el-avatar {
  grid-column: 1;
  grid-row: 1 / span 2;
}

.author-name {
  grid-column: 2;
  grid-row: 1;
  align-self: end;
  margin: 0;
}

.publish-date {
  grid-column: 2;
  grid-row: 2;
  align-self: start;
}

.action-buttons {
  grid-column: 3;
  grid-row: 1 / span 2;
  align-self: center;
  margin-left: 0;
  padding: 10px 14px;
  gap: 10px;
  min-height: 56px;
}

.action-buttons .report-button {
  min-width: 84px;
  justify-content: center;
}

.blog-content {
  border-radius: 24px !important;
}

.content-body {
  font-size: 16px;
  line-height: 1.95;
}

.content-body :deep(h1),
.content-body :deep(h2),
.content-body :deep(h3) {
  margin-top: 30px;
}

.content-body :deep(p:first-child) {
  margin-top: 0;
}

.recommend-section,
.comment-section {
  border-radius: 24px !important;
}

.recommend-grid {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.recommend-card {
  min-height: 220px;
  display: flex;
  flex-direction: column;
}

.recommend-card-summary {
  flex: 1;
  min-height: 0;
}

.recommend-card-footer {
  margin-top: auto;
}

.comment-thread {
  padding: 18px 0 22px;
}

.comment-item {
  gap: 16px;
}

.comment-input-area {
  padding-top: 8px;
}

.comment-input-area .el-textarea__inner {
  min-height: 112px;
}

@media (max-width: 900px) {
  .blog-detail-container {
    padding: 8px 14px 56px;
  }

  .blog-meta {
    grid-template-columns: 56px minmax(0, 1fr);
    grid-template-rows: auto auto auto;
  }

  .action-buttons {
    grid-column: 1 / -1;
    grid-row: 3;
    justify-content: flex-start;
    width: 100%;
  }
}

html[data-theme='dark'] .blog-header::after {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.08), transparent 42%, rgba(59, 130, 246, 0.05));
}

html[data-theme='dark'] .action-buttons {
  background: rgba(15, 23, 42, 0.84) !important;
}

html[data-theme='dark'] .recommend-card,
html[data-theme='dark'] .comment-thread,
html[data-theme='dark'] .reply-input-wrapper,
html[data-theme='dark'] .comment-input-area {
  background: rgba(15, 23, 42, 0.58) !important;
}
</style>
<style scoped>
.blog-detail-container {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
  max-width: 1040px;
  margin: 0 auto;
  padding: 12px var(--it-shell-padding-x) 56px;
}

.blog-detail-container::before {
  display: none;
}

.blog-header,
.blog-content,
.recommend-section,
.comment-section {
  background: var(--it-surface) !important;
  border: 1px solid var(--it-border) !important;
  box-shadow: var(--it-shadow) !important;
  backdrop-filter: blur(0);
  border-radius: 14px !important;
}

.blog-title,
.recommend-title,
.comment-header span,
.paid-prompt h3,
.vip-prompt h3,
.recommend-card-title,
.comment-author,
.author-name {
  color: var(--it-text) !important;
}

.publish-date,
.like-count,
.collect-count,
.recommend-subtitle,
.recommend-card-summary,
.comment-text,
.comment-time,
.purchase-confirm-content,
.balance-info,
.purchase-benefit,
.vip-prompt p,
.paid-prompt p,
.recommend-card-footer {
  color: var(--it-text-muted) !important;
}

.blog-tag-item,
.recommend-tag,
.reply-to {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
  border: 1px solid var(--it-border) !important;
}

.action-buttons {
  background: var(--it-surface-muted) !important;
  border: 1px solid var(--it-border) !important;
  box-shadow: none !important;
}

.recommend-card,
.comment-thread,
.reply-input-wrapper,
.comment-input-area,
.no-comment {
  background: var(--it-surface-solid) !important;
  border-color: var(--it-border) !important;
}

.comment-input-area :deep(.el-textarea__inner),
.reply-input-wrapper :deep(.el-textarea__inner) {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.comment-input-area :deep(.el-textarea__inner:focus),
.reply-input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: var(--it-accent) !important;
  box-shadow: 0 0 0 3px var(--it-accent-soft) !important;
}

.comment-submit .el-button,
.reply-actions .el-button--primary,
.purchase-actions .el-button--primary,
.vip-actions .el-button--primary {
  background: var(--it-primary-gradient) !important;
  border-color: transparent !important;
  color: #fff !important;
  box-shadow: none !important;
  border-radius: 8px !important;
}

.purchase-actions .el-button,
.vip-actions .el-button,
.reply-actions .el-button,
.comment-submit .el-button {
  border-radius: 8px !important;
}

.content-blur {
  background: linear-gradient(to bottom, transparent, var(--it-surface-solid)) !important;
}

.price-card,
.vip-prompt,
.paid-prompt,
.discount-info,
.final-amount-info {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
}

.backtop-inner {
  background: var(--it-primary-gradient) !important;
  border-radius: 8px;
  box-shadow: var(--it-shadow) !important;
}

:deep(.el-dialog) {
  background: var(--it-surface-solid) !important;
  border: 1px solid var(--it-border) !important;
  border-radius: 14px !important;
  box-shadow: var(--it-shadow-strong) !important;
}

:deep(.el-dialog__title),
:deep(.el-dialog__body strong) {
  color: var(--it-text) !important;
}

:deep(.el-dialog__body),
:deep(.el-dialog__body p),
:deep(.el-dialog__body span) {
  color: var(--it-text-muted) !important;
}

:deep(.el-dialog__footer .el-button--primary) {
  background: var(--it-primary-gradient) !important;
  border-color: transparent !important;
  border-radius: 8px !important;
}

@media (max-width: 900px) {
  .blog-detail-container {
    padding: 8px 12px 44px;
  }
}
</style>
