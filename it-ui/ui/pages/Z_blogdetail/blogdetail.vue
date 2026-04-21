<template>
  <div data-testid="blog-detail-page" class="blog-detail-container">
    <el-card data-testid="blog-detail-header" class="blog-header" :body-style="{ padding: '24px 28px' }" shadow="hover" v-loading="detailLoading">
      <div class="blog-title-wrapper">
        <h1 data-testid="blog-detail-title" class="blog-title">{{ blog.title }}</h1>
        <el-tag size="small" class="status-tag" :type="statusTagType">
          {{ statusText }}
        </el-tag>
        <el-tag
          :type="getPriceTagType(blog)"
          size="small"
          class="price-tag"
          v-text="getPriceTagText(blog)"
        ></el-tag>
      </div>

      <div class="blog-meta">
        <el-avatar :size="50" :src="blog.avatar" @click="goToAuthor" style="cursor: pointer;"></el-avatar>
        <span class="author-name" @click="goToAuthor" style="cursor: pointer;">{{ blog.author }}</span>
        <span class="publish-date">{{ publishDateLabel }} {{ blog.publishDate }}</span>

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
            class="download-button"
            size="small"
            icon="el-icon-download"
            :loading="downloadLoading"
            :disabled="!canDownloadBlog"
            @click="handleDownload"
          >
            {{ downloadButtonText }}
          </el-button>

          <el-button
            class="report-button"
            size="small"
            icon="el-icon-warning-outline"
            @click="handleReport"
            :loading="reportLoading"
            :disabled="!canReport"
          >
            {{ reportButtonText }}
          </el-button>

          <el-dropdown trigger="click" @command="openAiAssistantWithAction">
            <el-button
              class="ai-assistant-button"
              size="small"
              icon="el-icon-chat-dot-round"
              :disabled="!blog.id || detailLoading"
            >
              AI 助手
              <i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="blog.detail.summary">获取当前信息</el-dropdown-item>
              <el-dropdown-item command="blog.detail.explain">解释当前博客</el-dropdown-item>
              <el-dropdown-item command="blog.detail.possible-questions">猜你可能疑惑</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>

      <el-alert
        v-if="!isPublished"
        class="status-alert"
        type="info"
        :closable="false"
        show-icon
        :title="statusNoticeText"
      />

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
        <div class="content-preview">
          <RichContentViewer :source="blog.previewContent" empty-text="暂无预览内容" />
        </div>
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
        <div class="content-preview">
          <RichContentViewer :source="blog.previewContent" empty-text="暂无预览内容" />
        </div>
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

      <div v-else data-testid="blog-detail-content" class="content-body">
        <RichContentViewer :source="blog.content" empty-text="暂无内容" />
      </div>
    </el-card>

    <el-dialog
      title="VIP 专属内容"
      :visible.sync="vipDialogVisible"
      width="400px"
      :close-on-click-modal="false"
    >
      <div style="text-align: center; padding: 20px;">
        <i class="el-icon-star-on" style="font-size: 48px; color: var(--it-warning);"></i>
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

    <el-card data-testid="blog-recommend-section" class="recommend-section" shadow="hover" v-loading="recommendLoading">
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

      <div v-if="recommendations.length" data-testid="blog-recommend-grid" class="recommend-grid">
        <div
          v-for="item in recommendations"
          :key="item.id"
          class="recommend-card"
          :data-testid="`blog-recommend-card-${item.id}`"
          @click="goToRecommendedBlog(item)"
        >
          <div class="recommend-card-head">
            <h3 :data-testid="`blog-recommend-title-${item.id}`" class="recommend-card-title">{{ item.title || '未命名文章' }}</h3>
            <el-tag
              size="mini"
              effect="plain"
              :type="getPriceTagType(item)"
            >
              {{ getPriceTagText(item) }}
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

      <div v-else data-testid="blog-recommend-empty">
        <el-empty description="暂无相关推荐" :image-size="70" />
      </div>
    </el-card>

    <el-card data-testid="blog-comment-section" class="comment-section" shadow="hover" v-loading="commentLoading">
      <div slot="header" class="comment-header">
        <span>评论（{{ totalComments }}）</span>
      </div>

      <div class="comment-list">
        <div
          v-for="comment in topLevelComments"
          :key="comment.id"
          :data-testid="`blog-comment-thread-${comment.id}`"
          class="comment-thread"
        >
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
                <el-button v-if="!comment.deleted && canComment" type="text" size="small" @click="showReplyInput(comment)">
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
            <div
              v-for="reply in comment.replies"
              :key="reply.id"
              :id="`blog-comment-${reply.id}`"
              :data-testid="`blog-comment-reply-${reply.id}`"
              class="reply-item"
            >
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
                  <el-button v-if="!reply.deleted && canComment" type="text" size="small" @click="showReplyInput(reply, comment)">
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

        <div v-if="!topLevelComments.length" data-testid="blog-comment-empty" class="no-comment">
          暂无评论，快来抢沙发吧～
        </div>
      </div>

      <div data-testid="blog-comment-composer" class="comment-input-area">
        <el-input
          data-testid="blog-comment-input"
          type="textarea"
          :rows="3"
          :placeholder="commentPlaceholder"
          v-model="newComment"
          resize="none"
        ></el-input>
        <div class="comment-submit">
          <el-button
            data-testid="blog-comment-submit"
            type="primary"
            @click="submitTopLevelComment"
            :disabled="!canComment || !newComment.trim()"
            :loading="submitting"
          >
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
  blogStatusTagType,
  blogStatusText,
  completeBlogPurchaseOrder,
  createBlogComment,
  createBlogPurchaseOrder,
  createCollectRecord,
  createLikeRecord,
  downloadBlogContent,
  fetchBlogComments,
  fetchBlogDetail,
  fetchBlogRecommendations,
  fetchCollectRecord,
  fetchCurrentUserProfile,
  fetchLikeRecord,
  removeBlogComment,
  removeCollectRecord,
  removeLikeRecord,
  resolveBlogAccessType,
  replyBlogComment,
  submitBlogReport
} from '@/api/blog'
import { getUserAvailableCoupons, calculateDiscount } from '@/api/coupon'
import { pickAvatarUrl } from '@/utils/avatar'
import { collectBlogDetailContext, buildBlogDetailPrompt } from '@/utils/aiContextCollectors'

function escapeHtmlValue(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function normalizeRichTextInput(value) {
  return String(value || '').replace(/\r\n?/g, '\n').trim()
}

function decodeHtmlEntities(text) {
  return String(text || '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
}

function looksLikeHtml(text) {
  return /<\/?[a-z][\s\S]*>/i.test(String(text || ''))
}

function looksLikeEscapedHtml(text) {
  return /&lt;\/?[a-z][\s\S]*?&gt;/i.test(String(text || ''))
}

function sanitizeHtmlContent(html) {
  return String(html || '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/\son[a-z]+\s*=\s*(".*?"|'.*?'|[^\s>]+)/gi, '')
    .replace(/\s(href|src)\s*=\s*(["'])\s*javascript:[^"']*\2/gi, ' $1="#"')
    .replace(/\s(href|src)\s*=\s*javascript:[^\s>]+/gi, ' $1="#"')
}

function renderInlineMarkdown(text) {
  return escapeHtmlValue(text)
    .replace(/&lt;br\s*\/?&gt;/gi, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
}

function parseMarkdownTableCells(line) {
  return String(line || '')
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map(cell => cell.trim())
}

function isMarkdownTableSeparator(line) {
  const cells = parseMarkdownTableCells(line)
  return cells.length > 0 && cells.every(cell => /^:?-{3,}:?$/.test(cell.replace(/\s+/g, '')))
}

function looksLikeMarkdownTableRow(line) {
  const text = String(line || '').trim()
  return text.includes('|') && parseMarkdownTableCells(text).length >= 2
}

function isSpecialMarkdownLine(line, nextLine) {
  const text = String(line || '').trim()
  if (!text) return true
  if (/^```/.test(text)) return true
  if (/^([-*_])\1{2,}$/.test(text)) return true
  if (/^#{1,6}\s+/.test(text)) return true
  if (/^>\s+/.test(text)) return true
  if (/^\s*[-*+]\s+/.test(text)) return true
  if (/^\s*\d+\.\s+/.test(text)) return true
  if (looksLikeMarkdownTableRow(text) && isMarkdownTableSeparator(nextLine)) return true
  return false
}

function renderMarkdownToHtml(source, emptyText = '暂无内容') {
  const raw = normalizeRichTextInput(source)
  if (!raw) {
    return `<div class="empty-rich-content">${escapeHtmlValue(emptyText)}</div>`
  }

  const lines = raw.split('\n')
  const blocks = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]
    const trimmed = String(line || '').trim()

    if (!trimmed) {
      i += 1
      continue
    }

    if (/^```/.test(trimmed)) {
      const codeLines = []
      i += 1
      while (i < lines.length && !/^```/.test(String(lines[i] || '').trim())) {
        codeLines.push(lines[i])
        i += 1
      }
      if (i < lines.length && /^```/.test(String(lines[i] || '').trim())) {
        i += 1
      }
      blocks.push(`<pre><code>${escapeHtmlValue(codeLines.join('\n'))}</code></pre>`)
      continue
    }

    if (/^([-*_])\1{2,}$/.test(trimmed)) {
      blocks.push('<hr>')
      i += 1
      continue
    }

    if (/^#{1,6}\s+/.test(trimmed)) {
      const level = trimmed.match(/^#+/)[0].length
      const body = trimmed.slice(level).trim()
      blocks.push(`<h${level}>${renderInlineMarkdown(body)}</h${level}>`)
      i += 1
      continue
    }

    if (looksLikeMarkdownTableRow(trimmed) && isMarkdownTableSeparator(lines[i + 1])) {
      const headers = parseMarkdownTableCells(trimmed)
      i += 2
      const rows = []
      while (i < lines.length) {
        const rowLine = String(lines[i] || '').trim()
        if (!rowLine || !looksLikeMarkdownTableRow(rowLine)) break
        if (isMarkdownTableSeparator(rowLine)) {
          i += 1
          continue
        }
        rows.push(parseMarkdownTableCells(rowLine))
        i += 1
      }
      const thead = `<thead><tr>${headers.map(cell => `<th>${renderInlineMarkdown(cell)}</th>`).join('')}</tr></thead>`
      const tbody = rows.length
        ? `<tbody>${rows.map(row => `<tr>${headers.map((_, idx) => `<td>${renderInlineMarkdown(row[idx] || '')}</td>`).join('')}</tr>`).join('')}</tbody>`
        : ''
      blocks.push(`<div class="rich-table-wrap"><table>${thead}${tbody}</table></div>`)
      continue
    }

    if (/^>\s+/.test(trimmed)) {
      const quoteLines = []
      while (i < lines.length && /^>\s+/.test(String(lines[i] || '').trim())) {
        quoteLines.push(String(lines[i] || '').trim().replace(/^>\s+/, ''))
        i += 1
      }
      blocks.push(`<blockquote>${quoteLines.map(item => renderInlineMarkdown(item)).join('<br>')}</blockquote>`)
      continue
    }

    if (/^\s*[-*+]\s+/.test(trimmed)) {
      const items = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(String(lines[i] || '').trim())) {
        items.push(String(lines[i] || '').trim().replace(/^\s*[-*+]\s+/, ''))
        i += 1
      }
      blocks.push(`<ul>${items.map(item => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ul>`)
      continue
    }

    if (/^\s*\d+\.\s+/.test(trimmed)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(String(lines[i] || '').trim())) {
        items.push(String(lines[i] || '').trim().replace(/^\s*\d+\.\s+/, ''))
        i += 1
      }
      blocks.push(`<ol>${items.map(item => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ol>`)
      continue
    }

    const paragraphLines = [trimmed]
    i += 1
    while (i < lines.length) {
      const nextLine = String(lines[i] || '').trim()
      if (!nextLine) {
        i += 1
        break
      }
      if (isSpecialMarkdownLine(lines[i], lines[i + 1])) {
        break
      }
      paragraphLines.push(nextLine)
      i += 1
    }

    blocks.push(`<p>${paragraphLines.map(item => renderInlineMarkdown(item)).join('<br>')}</p>`)
  }

  return blocks.join('')
}

function renderRichTextContent(source, emptyText = '暂无内容') {
  const raw = normalizeRichTextInput(source)
  if (!raw) {
    return `<div class="empty-rich-content">${escapeHtmlValue(emptyText)}</div>`
  }

  if (looksLikeEscapedHtml(raw)) {
    return sanitizeHtmlContent(decodeHtmlEntities(raw))
  }

  if (looksLikeHtml(raw)) {
    return sanitizeHtmlContent(raw)
  }

  return renderMarkdownToHtml(raw, emptyText)
}

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
        avatarUrl: '',
        publishDate: '',
        status: 'draft',
        statusLabel: '草稿',
        rejectReason: '',
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
      downloadLoading: false,
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
      couponLoading: false,
      aiContextCollectorDisposer: null
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
    blogAccessType() {
      return resolveBlogAccessType(this.blog)
    },
    statusText() {
      return this.blog.statusLabel || blogStatusText(this.blog.status)
    },
    statusTagType() {
      return blogStatusTagType(this.blog.status)
    },
    isPublished() {
      return String(this.blog.status || '').toLowerCase() === 'published'
    },
    isAuthorViewer() {
      return String(this.blog.authorId || '') !== '' && String(this.blog.authorId || '') === String(this.userId || '')
    },
    canComment() {
      return this.isPublished
    },
    reportDisabled() {
      if (!this.isPublished) return true
      if (this.reportSubmitted) return true
      return String(this.blog.authorId || '') === String(this.userId || '')
    },
    canReport() {
      return !this.reportDisabled
    },
    reportButtonText() {
      if (!this.isPublished) return '仅已发布可举报'
      if (this.reportSubmitted) return '已举报'
      if (String(this.blog.authorId || '') === String(this.userId || '')) return '不可举报自己'
      return '举报'
    },
    statusNoticeText() {
      if (this.blog.status === 'pending') return '该博客正在审核中，审核通过后将对外可见。'
      if (this.blog.status === 'rejected') {
        return this.blog.rejectReason
          ? `该博客已被拒绝：${this.blog.rejectReason}`
          : '该博客已被拒绝，请修改后重新提交审核。'
      }
      if (this.blog.status === 'draft') return '该博客为草稿状态，仅作者本人可见。'
      return '当前博客不处于公开发布状态。'
    },
    publishDateLabel() {
      return this.isPublished ? '发布于' : '更新于'
    },
    commentPlaceholder() {
      return this.canComment ? '写下你的评论...' : '仅已发布博客支持评论'
    },
    isPaidLocked() {
      return this.blog.locked === true && this.blogAccessType === 'paid'
    },
    isVipLocked() {
      return this.blog.locked === true && this.blogAccessType === 'vip'
    },
    canDownloadBlog() {
      if (!this.isPublished) return false
      if (this.blog.canDownload === true) return true
      if (this.blog.canDownload === false) return false
      return this.blog.hasAccess !== false && this.blog.locked !== true
    },
    downloadButtonText() {
      if (!this.isPublished) return '仅已发布可下载'
      if (this.canDownloadBlog) return '下载'
      if (this.isPaidLocked) return '购买后可下载'
      if (this.isVipLocked) return 'VIP 可下载'
      return '暂不可下载'
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
    renderedBlogContent() {
      return renderRichTextContent(this.blog.content, '暂无内容')
    },
    renderedBlogPreviewContent() {
      return renderRichTextContent(this.blog.previewContent, '暂无预览内容')
    },
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
  mounted() {
    this.bindAiAssistantBridge()
  },
  beforeDestroy() {
    this.unbindAiAssistantBridge()
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
    bindAiAssistantBridge() {
      if (!process.client) return
      this.unbindAiAssistantBridge()
      if (this.$aiActionBridge && typeof this.$aiActionBridge.registerContextCollector === 'function') {
        this.aiContextCollectorDisposer = this.$aiActionBridge.registerContextCollector('blog.detail', () => this.collectAiContextPayload())
      }
    },
    unbindAiAssistantBridge() {
      if (typeof this.aiContextCollectorDisposer === 'function') {
        this.aiContextCollectorDisposer()
      }
      this.aiContextCollectorDisposer = null
    },
    collectAiContextPayload() {
      return collectBlogDetailContext({
        blog: this.blog,
        comments: this.comments
      })
    },
    openAiAssistantWithAction(actionCode = '') {
      const normalizedActionCode = String(actionCode || '').trim()
      if (!normalizedActionCode || !process.client) return false

      const contextPayload = this.collectAiContextPayload()
      const prompt = buildBlogDetailPrompt(normalizedActionCode, contextPayload)
      const detail = {
        prompt,
        sceneCode: 'blog.detail',
        actionCode: normalizedActionCode,
        scene: 'blog.detail',
        action: normalizedActionCode,
        contextPayload: {
          ...contextPayload,
          blogId: this.blog.id || null
        },
        source: 'blog.detail.page',
        autoSend: true
      }

      if (this.$aiActionBridge && typeof this.$aiActionBridge.open === 'function') {
        this.$aiActionBridge.open(detail)
        return true
      }

      window.dispatchEvent(new CustomEvent('ai-assistant-open', { detail }))
      return true
    },
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
    getPriceTagType(source) {
      const accessType = resolveBlogAccessType(source)
      if (accessType === 'free') return 'success'
      if (accessType === 'vip') return 'warning'
      return 'primary'
    },
    getPriceTagText(source) {
      const accessType = resolveBlogAccessType(source)
      const price = this.normalizePrice(source && source.price)
      if (accessType === 'free') return '免费'
      if (accessType === 'vip') return 'VIP'
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
        const u = await fetchCurrentUserProfile()
        if (u) {
          this.currentUser = u
          this.userId = u.id
          this.username = u.username || u.nickname || ''
          this.userAvatar = pickAvatarUrl(u.avatarUrl, u.avatar, this.userAvatar)
          await Promise.all([
            this.checkLikeStatus(),
            this.checkCollectStatus()
          ])
          if (this.blog.id) {
            await this.getComments()
          }
        }
      } catch (e) {
        if (e && e.response && e.response.status === 404) {
          return
        }
        console.error('获取当前用户信息失败', e)
      }
    },
    async getBlogDetail(blogId) {
      this.currentProcessingBlogId = blogId
      this.detailLoading = true
      try {
        const detail = await fetchBlogDetail(blogId)
        if (this.currentProcessingBlogId !== blogId) {
          return
        }

        const author = detail.author || {}
        const publishDateValue = detail.publishTime || detail.updatedAt || detail.createdAt
        const publishDate = publishDateValue
          ? new Date(publishDateValue).toLocaleString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
          })
          : ''

        const price = this.normalizePrice(detail.price)
        const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        const avatarUrl = pickAvatarUrl(author.avatarUrl, author.avatar, defaultAvatar)
        const info = {
          id: detail.id || blogId,
          like_id: '',
          collect_id: '',
          title: detail.title || '无标题',
          author: author.displayName || author.nickname || author.username || '未知作者',
          authorId: author.id || '',
          avatar: avatarUrl,
          avatarUrl: avatarUrl,
          publishDate,
          status: detail.status || 'draft',
          statusLabel: detail.statusLabel || blogStatusText(detail.status),
          rejectReason: detail.rejectReason || '',
          likeCount: Number(detail.likeCount || 0),
          collectCount: Number(detail.collectCount || 0),
          reportCount: Number(detail.reportCount || 0),
          isLiked: false,
          isCollected: false,
          tags: this.normalizeTags(detail.tags),
          content: detail.content || '',
          previewContent: detail.previewContent || '',
          isVipOnly: resolveBlogAccessType(detail) === 'vip',
          price,
          accessType: resolveBlogAccessType(detail),
          locked: detail.locked === true,
          lockType: detail.lockType || 'none',
          canDownload: detail.canDownload,
          hasAccess: detail.hasAccess !== false,
          hasPurchased: detail.hasPurchased === true,
          isVipUser: detail.isVipUser === true
        }

        this.$set(this, 'blog', info)
        this.reportSubmitted = false
        this.purchaseAmount = price > 0 ? price : 0
        await this.getComments()
        if (String(info.status).toLowerCase() === 'published') {
          await this.fetchRecommendations(blogId)
        } else {
          this.recommendations = []
          this.recommendMeta = {
            source: 'fallback',
            algorithmVersion: '',
            generatedAt: ''
          }
        }
      } catch (e) {
        console.error('获取博客详情失败', e)
        this.$message.error(e?.response?.data?.message || '获取博客详情失败')
      } finally {
        this.detailLoading = false
      }
    },
    async fetchRecommendations(blogId) {
      if (!blogId) return

      const currentBlogId = String(blogId)
      this.recommendLoading = true
      try {
        const recommendationData = await fetchBlogRecommendations(blogId, { size: 6 })
        if (String(this.blog.id) !== currentBlogId) {
          return
        }
        this.recommendations = Array.isArray(recommendationData.items) ? recommendationData.items : []
        this.recommendMeta = {
          source: recommendationData.source || 'fallback',
          algorithmVersion: recommendationData.algorithmVersion || '',
          generatedAt: recommendationData.generatedAt || ''
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
        const commentList = await fetchBlogComments(currentBlogId)
        if (this.blog.id !== currentBlogId) {
          return
        }

        const list = (Array.isArray(commentList) ? commentList : []).map(i => this.convertCommentData(i))
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
      const authorId = comment.authorId || comment.author?.id || null
      const deleted = comment.deleted === true || (comment.status || '').toLowerCase() === 'deleted'
      const isCommentOwner = String(authorId || '') === String(this.userId || '')
      const isBlogOwner = String(this.userId || '') === String(this.blog.authorId || '')

      if (String(authorId || '') === String(this.blog.authorId || '')) {
        nickname = this.blog.author
        avatar = pickAvatarUrl(this.blog.avatarUrl, this.blog.avatar, avatar)
      } else if (comment.nickname) {
        nickname = comment.nickname
        avatar = comment.avatar || avatar
      } else if (comment.author) {
        if (typeof comment.author === 'object') {
          nickname = comment.author.displayName || comment.author.nickname || comment.author.username || '匿名用户'
          avatar = pickAvatarUrl(comment.author.avatarUrl, comment.author.avatar, avatar)
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
        parentId: comment.parentCommentId || comment.parentId || null,
        postId: comment.postId || this.blog.id,
        content: comment.content || comment.body || '',
        nickname: nickname,
        avatar: avatar,
        createTime: comment.createdAt || comment.createTime || new Date().toISOString(),
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
        const record = await fetchCollectRecord(this.userId, this.blog.id)
        if (this.blog.id !== currentBlogId) {
          return
        }
        const ok = !!record
        this.blog.isCollected = ok
        this.blog.collect_id = ok && record.id ? record.id : ''
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
        const record = await fetchLikeRecord(this.userId, this.blog.id)
        if (this.blog.id !== currentBlogId) {
          return
        }
        const ok = !!record
        this.blog.isLiked = ok
        this.blog.like_id = ok && record.id ? record.id : ''
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
      if (!this.isPublished) {
        this.$message.warning('仅已发布博客支持点赞')
        return
      }

      this.likeLoading = true
      try {
        if (this.blog.isLiked) {
          if (this.blog.like_id) {
            await removeLikeRecord(this.blog.like_id)
            this.blog.isLiked = false
            this.blog.likeCount = Math.max(0, Number(this.blog.likeCount || 0) - 1)
            this.blog.like_id = ''
            this.$message.success('取消点赞成功')
          }
        } else {
          const wasLiked = this.blog.isLiked
          const record = await createLikeRecord({
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          })
          if (record && record.id) {
            this.blog.isLiked = true
            if (!wasLiked) {
              this.blog.likeCount = Number(this.blog.likeCount || 0) + 1
            }
            this.blog.like_id = record.id
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
      if (!this.isPublished) {
        this.$message.warning('仅已发布博客支持收藏')
        return
      }

      this.collectLoading = true
      try {
        if (this.blog.isCollected) {
          if (this.blog.collect_id) {
            await removeCollectRecord(this.blog.collect_id)
            this.blog.isCollected = false
            this.blog.collectCount = Math.max(0, Number(this.blog.collectCount || 0) - 1)
            this.blog.collect_id = ''
            this.$message.success('取消收藏成功')
          }
        } else {
          const wasCollected = this.blog.isCollected
          const record = await createCollectRecord({
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          })
          if (record && record.id) {
            this.blog.isCollected = true
            if (!wasCollected) {
              this.blog.collectCount = Number(this.blog.collectCount || 0) + 1
            }
            this.blog.collect_id = record.id
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
      if (!this.canReport) {
        if (!this.isPublished) {
          this.$message.warning('仅已发布博客可举报')
        } else if (String(this.blog.authorId) === String(this.userId)) {
          this.$message.warning('不能举报自己的博客')
        } else if (this.reportSubmitted) {
          this.$message.warning('该博客已举报')
        }
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
        await submitBlogReport(this.blog.id, value.trim())
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
    async handleDownload() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }
      if (!this.canDownloadBlog) {
        this.$message.warning(this.downloadButtonText)
        return
      }

      this.downloadLoading = true
      try {
        await downloadBlogContent(this.blog.id)
        this.blog.downloadCount = Number(this.blog.downloadCount || 0) + 1
        this.$message.success('下载已记录')
      } catch (e) {
        console.error('下载博客失败', e)
        this.$message.error(e.response?.data?.message || e.message || '下载失败，请稍后重试')
      } finally {
        this.downloadLoading = false
      }
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

        const d = await createBlogPurchaseOrder(this.blog.id, {
          paymentMethod,
          userCouponId,
          userId: this.userId
        })
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
        await completeBlogPurchaseOrder(this.blog.id, this.purchaseOrderNo, {
          userId: this.userId
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
        await removeBlogComment(comment.id)
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
      if (!this.canComment) {
        this.$message.warning('仅已发布博客支持评论')
        return
      }

      this.submitting = true
      try {
        const created = await createBlogComment({
          content: this.newComment,
          parentCommentId: null,
          postId: this.blog.id,
          authorId: this.userId
        })
        if (created && created.id) {
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
      if (!this.canComment) {
        this.$message.warning('仅已发布博客支持评论')
        return
      }

      this.replySubmitting = true
      try {
        const created = await replyBlogComment({
          content: this.replyContent,
          parentCommentId: replyToComment ? replyToComment.id : parentComment.id,
          postId: this.blog.id,
          authorId: this.userId
        })
        if (created && created.id) {
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
  background: var(--it-page-bg);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.blog-header {
  margin-bottom: 25px;
  border-radius: 20px !important;
  border: 1px solid var(--it-border);
  overflow: hidden;
  background: var(--it-panel-bg-strong) !important;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.blog-header:hover {
  transform: translateY(-2px);
  box-shadow: var(--it-shadow-hover) !important;
}

.blog-header::before {
  content: '';
  position: absolute;
  inset: auto -12% -38% 48%;
  height: 220px;
  background: radial-gradient(circle, color-mix(in srgb, var(--it-accent) 24%, transparent), transparent 70%);
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
  color: var(--it-text);
  line-height: 1.3;
  font-weight: 600;
  background: linear-gradient(135deg, var(--it-text), var(--it-accent));
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

.status-tag {
  border-radius: 12px;
  padding: 4px 12px;
  font-weight: 600;
}

.status-alert {
  margin-top: 14px;
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
  border: 3px solid color-mix(in srgb, var(--it-accent-soft) 80%, transparent);
  box-shadow: 0 10px 18px color-mix(in srgb, var(--it-accent) 18%, transparent);
}

.author-name {
  font-size: 1.2rem;
  font-weight: 500;
  color: var(--it-accent);
  cursor: pointer;
  transition: color 0.2s;
}

.author-name:hover {
  color: var(--it-accent);
}

.publish-date {
  color: var(--it-text-muted);
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
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}

.like-count,
.collect-count {
  margin-right: 10px;
  font-size: 14px;
  color: var(--it-text-muted);
  font-weight: 500;
}

.action-buttons .el-button {
  border: none;
  background: color-mix(in srgb, var(--it-surface-muted) 88%, var(--it-surface-solid));
  color: var(--it-text-muted);
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: scale(1.08);
  background: color-mix(in srgb, var(--it-border) 72%, var(--it-surface-muted));
}

.action-buttons .el-button.el-button--primary {
  background: var(--it-primary-gradient);
  color: var(--it-text-light);
  box-shadow: 0 4px 10px color-mix(in srgb, var(--it-accent) 28%, transparent);
}

.action-buttons .el-button.el-button--warning {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 88%, white), var(--it-warning));
  color: var(--it-text-light);
  box-shadow: 0 4px 10px color-mix(in srgb, var(--it-warning) 26%, transparent);
}

.action-buttons .report-button {
  margin-left: 4px;
  border-radius: 999px;
  padding: 0 16px;
  background: var(--it-danger-panel-bg);
  color: var(--it-tone-danger-text);
  border: 1px solid var(--it-danger-panel-border);
  box-shadow: none;
}

.action-buttons .report-button:hover:not(.is-disabled) {
  background: color-mix(in srgb, var(--it-danger-soft) 82%, var(--it-surface-solid));
  color: color-mix(in srgb, var(--it-tone-danger-text) 84%, var(--it-text));
  border-color: color-mix(in srgb, var(--it-danger) 40%, var(--it-border));
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
  background-color: var(--it-panel-bg-strong) !important;
  margin-bottom: 25px;
  border: 1px solid var(--it-border);
  overflow: hidden;
  box-shadow: var(--it-shadow-soft);
}

.content-body {
  font-size: 1.1rem;
  line-height: 1.8;
  color: var(--it-text);
  max-width: 100%;
}

.content-body h2 {
  margin: 28px 0 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--it-border);
  padding-bottom: 8px;
  color: var(--it-text);
}

.content-body p {
  margin: 16px 0;
}

.content-body code {
  background-color: var(--it-inline-code-bg);
  padding: 2px 6px;
  border-radius: 6px;
  font-family: 'Fira Code', 'Courier New', monospace;
  color: var(--it-inline-code-text);
}

.content-body pre {
  background-color: var(--it-text);
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  border: none;
  box-shadow: var(--it-shadow-soft);
}

.content-body pre code {
  background-color: transparent;
  padding: 0;
  color: var(--it-contrast-text);
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 0.95rem;
}

.content-body img {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: var(--it-shadow-soft);
  margin: 20px 0;
}

.content-preview :deep(img) {
  max-width: 100%;
  border-radius: 12px;
  box-shadow: var(--it-shadow-soft);
  margin: 20px 0;
}

.content-body :deep(blockquote),
.content-preview :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 16px;
  border-left: 4px solid var(--it-quote-border);
  background: var(--it-quote-bg);
  color: var(--it-text-muted);
  border-radius: 10px;
}

.content-body :deep(a),
.content-preview :deep(a) {
  color: var(--it-accent);
  text-decoration: none;
}

.content-body :deep(a:hover),
.content-preview :deep(a:hover) {
  text-decoration: underline;
}

.content-body :deep(ul),
.content-preview :deep(ul),
.content-body :deep(ol),
.content-preview :deep(ol) {
  margin: 14px 0 14px 20px;
  padding: 0;
}

.content-body :deep(table),
.content-preview :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.content-body :deep(th),
.content-preview :deep(th),
.content-body :deep(td),
.content-preview :deep(td) {
  border: 1px solid var(--it-border);
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
}

.content-body :deep(th),
.content-preview :deep(th) {
  background: var(--it-contrast-panel-bg);
  color: var(--it-surface-muted);
}

.content-body :deep(hr),
.content-preview :deep(hr) {
  border: 0;
  border-top: 1px solid var(--it-border);
  margin: 18px 0;
}

.rich-table-wrap {
  width: 100%;
  overflow-x: auto;
  margin: 16px 0;
}

.empty-rich-content {
  color: var(--it-text-subtle);
}

.paid-content-wrapper,
.vip-content-wrapper {
  position: relative;
  overflow: hidden;
}

.content-preview {
  font-size: 1.1rem;
  line-height: 1.8;
  color: var(--it-text);
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
  background: color-mix(in srgb, var(--it-tone-info-soft) 88%, var(--it-surface-solid));
  color: var(--it-accent-hover);
  font-weight: 600;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}

.recommend-section,
.comment-section {
  border-radius: 24px !important;
  overflow: hidden;
  box-shadow: var(--it-shadow) !important;
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
  background: linear-gradient(to bottom, transparent, color-mix(in srgb, var(--it-surface-solid) 94%, transparent));
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
  background: var(--it-warning-panel-bg);
  border: 2px solid var(--it-warning-panel-border);
}

.vip-prompt i {
  font-size: 42px;
  color: var(--it-warning);
  margin-bottom: 15px;
  display: block;
}

.vip-prompt h3 {
  margin: 0 0 8px;
  color: var(--it-tone-warning-text);
  font-size: 18px;
  font-weight: 700;
}

.vip-prompt p {
  margin: 0 0 20px;
  color: color-mix(in srgb, var(--it-tone-warning-text) 84%, var(--it-text));
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
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 86%, white), var(--it-warning));
  border: none;
  box-shadow: 0 4px 15px color-mix(in srgb, var(--it-warning) 34%, transparent);
}

.vip-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px color-mix(in srgb, var(--it-warning) 42%, transparent);
}

.paid-prompt {
  background: var(--it-info-panel-bg);
  border: 2px solid var(--it-info-panel-border);
}

.paid-prompt i {
  font-size: 48px;
  color: var(--it-tone-purple-text);
  margin-bottom: 15px;
  display: block;
}

.paid-prompt h3 {
  margin: 0 0 15px;
  color: color-mix(in srgb, var(--it-tone-purple-text) 84%, var(--it-text));
  font-size: 20px;
  font-weight: 700;
}

.price-card {
  margin: 20px 0;
  padding: 15px 18px;
  background: var(--it-surface-solid);
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.price-label {
  font-size: 14px;
  color: var(--it-text-muted);
}

.price-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--it-tone-danger-text);
}

.purchase-benefit {
  margin: 15px 0 25px;
  color: var(--it-text-muted);
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
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-tone-purple-text) 86%, white), var(--it-accent));
  border: none;
  box-shadow: 0 4px 15px color-mix(in srgb, var(--it-accent) 32%, transparent);
}

.purchase-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px color-mix(in srgb, var(--it-accent) 40%, transparent);
}

.purchase-confirm-content p {
  margin: 10px 0;
  font-size: 15px;
  color: var(--it-text);
}

.amount-info .amount {
  font-size: 28px;
  font-weight: bold;
  color: var(--it-tone-danger-text);
  display: block;
  margin-top: 10px;
}

.original-amount {
  font-size: 18px;
  font-weight: 600;
  color: var(--it-text-muted);
  text-decoration: line-through;
}

.discount-info {
  margin: 10px 0;
  padding: 10px;
  background: var(--it-warning-panel-bg);
  border-radius: 8px;
  border-left: 4px solid var(--it-warning);
}

.discount-info .discount-amount {
  font-size: 20px;
  font-weight: bold;
  color: var(--it-tone-danger-text);
  display: block;
  margin-top: 5px;
}

.final-amount-info {
  margin: 15px 0;
  padding: 12px;
  background: var(--it-info-panel-bg);
  border-radius: 8px;
  border-left: 4px solid var(--it-quote-border);
}

.final-amount-info .final-amount {
  font-size: 24px;
  font-weight: bold;
  color: var(--it-tone-info-text);
  display: block;
  margin-top: 5px;
}

.balance-info {
  color: var(--it-text-muted);
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
  background-color: var(--it-panel-bg-strong) !important;
  border: 1px solid var(--it-border);
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
  color: var(--it-text);
}

.recommend-subtitle {
  margin-top: 6px;
  color: var(--it-text-muted);
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
  border: 1px solid var(--it-border);
  background: linear-gradient(180deg, var(--it-panel-bg-strong) 0%, var(--it-panel-bg) 100%);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.recommend-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--it-accent) 34%, var(--it-border));
  box-shadow: 0 14px 28px color-mix(in srgb, var(--it-accent) 20%, transparent);
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
  color: var(--it-text);
  font-size: 16px;
  line-height: 1.5;
  font-weight: 600;
}

.recommend-card-summary {
  margin: 12px 0 0;
  min-height: 66px;
  color: var(--it-text-muted);
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
  background: var(--it-quote-bg);
  color: var(--it-accent);
  font-size: 12px;
  line-height: 1;
}

.recommend-card-footer {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--it-text-muted);
  font-size: 12px;
}

.comment-section {
  border-radius: 20px !important;
  background-color: var(--it-panel-bg-strong) !important;
  border: 1px solid var(--it-border);
  overflow: hidden;
}

.comment-header {
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--it-text);
  padding: 15px 20px;
  border-bottom: 1px solid var(--it-border);
}

.comment-list {
  margin-bottom: 20px;
  padding: 0 20px;
}

.comment-thread {
  margin-bottom: 25px;
  border-bottom: 1px solid var(--it-border);
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
  background: var(--it-quote-bg);
  border-radius: 12px;
  transition: background 0.3s ease;
}

.comment-item .el-avatar {
  border: 2px solid var(--it-border);
  transition: border-color 0.2s;
}

.comment-item:hover .el-avatar {
  border-color: var(--it-accent);
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
  color: var(--it-accent);
  margin-right: 10px;
}

.comment-time {
  color: var(--it-text-subtle);
  font-size: 0.85rem;
}

.comment-text {
  color: var(--it-text);
  line-height: 1.6;
}

.deleted-text {
  color: var(--it-text-subtle);
  font-style: italic;
}

.author-badge {
  font-size: 12px;
  padding: 2px 8px;
  background: var(--it-primary-gradient);
  color: var(--it-text-light);
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
  border-top: 1px dashed var(--it-border);
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
  color: var(--it-text-subtle);
  background-color: var(--it-inline-code-bg);
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
  color: var(--it-text-muted);
  transition: color 0.2s;
}

.comment-actions .el-button:hover {
  color: var(--it-accent);
  background: transparent !important;
}

.comment-actions .delete-action:hover {
  color: var(--it-tone-danger-text);
}

.comment-actions .el-button i {
  margin-right: 3px;
  font-size: 14px;
}

.reply-input-wrapper {
  margin-top: 15px;
  margin-left: 55px;
  padding: 15px;
  background-color: var(--it-surface-muted);
  border-radius: 12px;
  border: 1px solid var(--it-border);
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
  background-color: var(--it-surface-muted);
  border: 1px solid var(--it-border);
  border-radius: 12px;
  padding: 12px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.comment-input-area .el-textarea__inner:focus {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
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
  background: var(--it-primary-gradient);
  border: none;
  color: var(--it-text-light);
  transition: transform 0.2s, box-shadow 0.2s;
}

.comment-submit .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px color-mix(in srgb, var(--it-accent) 26%, transparent);
}

.comment-submit .el-button:disabled {
  background: color-mix(in srgb, var(--it-border) 80%, var(--it-surface-muted));
  opacity: 0.6;
}

.no-comment {
  text-align: center;
  padding: 40px;
  color: var(--it-text-subtle);
  font-size: 14px;
  background: var(--it-surface-muted);
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
  background: var(--it-primary-gradient);
  color: var(--it-text-light);
  border-radius: 50%;
  font-size: 14px;
  box-shadow: 0 4px 12px color-mix(in srgb, var(--it-accent) 34%, transparent);
  transition: transform 0.2s, box-shadow 0.2s;
}

.backtop-inner:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--it-accent) 42%, transparent);
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
    radial-gradient(circle at top left, color-mix(in srgb, var(--it-accent-soft) 82%, transparent), transparent 28%),
    radial-gradient(circle at top right, color-mix(in srgb, var(--it-warning-soft) 72%, transparent), transparent 22%),
    var(--it-page-bg);
}

.blog-detail-container::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(var(--it-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--it-grid-line) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.55), transparent 90%);
}

.blog-header,
.blog-content,
.recommend-section,
.comment-section {
  background: var(--it-panel-bg-strong) !important;
  border: 1px solid var(--it-border) !important;
  box-shadow: var(--it-shadow) !important;
  backdrop-filter: blur(22px);
}

.blog-title,
.recommend-title,
.comment-header span,
.paid-prompt h3,
.vip-prompt h3 {
  color: var(--it-text) !important;
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
  color: var(--it-text-muted) !important;
}

.blog-tag-item,
.recommend-tag,
.reply-to {
  background: color-mix(in srgb, var(--it-accent-soft) 88%, var(--it-surface-solid)) !important;
  color: var(--it-accent) !important;
  border-color: color-mix(in srgb, var(--it-accent) 18%, var(--it-border)) !important;
}

.action-buttons {
  background: color-mix(in srgb, var(--it-surface-muted) 88%, var(--it-surface-solid)) !important;
  border: 1px solid var(--it-border);
  box-shadow: inset 0 1px 0 color-mix(in srgb, var(--it-text-light) 28%, transparent);
}

.blog-content {
  overflow: hidden;
}

.content-body,
.content-preview,
.comment-author,
.recommend-card-title,
.recommend-card-footer {
  color: var(--it-text) !important;
}

.content-body :deep(h1),
.content-body :deep(h2),
.content-body :deep(h3),
.content-preview :deep(h1),
.content-preview :deep(h2),
.content-preview :deep(h3) {
  color: var(--it-text);
}

.content-body :deep(p),
.content-preview :deep(p),
.content-body :deep(li),
.content-preview :deep(li) {
  color: var(--it-text-muted);
}

.content-body :deep(code),
.content-preview :deep(code) {
  background: var(--it-inline-code-bg);
  color: var(--it-inline-code-text);
}

.content-body :deep(pre),
.content-preview :deep(pre) {
  background: var(--it-contrast-panel-bg);
  border: 1px solid var(--it-contrast-panel-border);
  color: var(--it-contrast-text);
}

.recommend-card,
.comment-thread,
.reply-input-wrapper,
.comment-input-area {
  background: linear-gradient(180deg, var(--it-panel-bg-strong) 0%, var(--it-panel-bg) 100%) !important;
  border-color: var(--it-border) !important;
}

.recommend-card:hover {
  border-color: color-mix(in srgb, var(--it-accent) 34%, var(--it-border)) !important;
  box-shadow: var(--it-shadow-hover) !important;
}

.comment-actions .el-button,
.delete-action {
  color: var(--it-text-subtle) !important;
}

.comment-actions .el-button:hover {
  color: var(--it-accent) !important;
}

.delete-action:hover {
  color: var(--it-tone-danger-text) !important;
}

.comment-input-area :deep(.el-textarea__inner),
.reply-input-wrapper :deep(.el-textarea__inner) {
  background: var(--it-surface-solid) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.comment-input-area :deep(.el-textarea__inner:focus),
.reply-input-wrapper :deep(.el-textarea__inner:focus) {
  border-color: color-mix(in srgb, var(--it-accent) 45%, var(--it-border)) !important;
  box-shadow: 0 0 0 3px var(--it-accent-soft) !important;
}

.comment-submit .el-button,
.reply-actions .el-button--primary,
.purchase-actions .el-button--primary,
.vip-actions .el-button--primary {
  background: var(--it-primary-gradient) !important;
  border-color: transparent !important;
  color: var(--it-text-light) !important;
  box-shadow: var(--it-button-shadow);
}
</style>
