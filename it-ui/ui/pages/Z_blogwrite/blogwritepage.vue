<template>
  <div class="write-blog-container">
    <div class="write-header">
      <div class="user-info" @click="goToUserHome">
        <el-avatar :size="50" :src="userAvatar"></el-avatar>
        <span class="username">{{ username }}</span>
      </div>

      <div class="action-buttons">
        <el-button type="warning" plain @click="openDraftDrawer" :loading="loadingDrafts">
          我的博客
        </el-button>

        <el-button
          type="success"
          plain
          icon="el-icon-magic-stick"
          @click="handleAiPolish"
          :loading="aiPolishing"
        >
          AI 润色
        </el-button>

        <el-button
          type="primary"
          plain
          icon="el-icon-s-opportunity"
          @click="handleAiGenerateSummary"
          :loading="aiSummarizing"
        >
          AI 生成摘要/标签
        </el-button>

        <span v-if="lastSaved" class="save-tip">最后保存：{{ lastSaved }}</span>

        <el-tag size="small" class="status-tag-pill" :type="currentStatusTagType">
          {{ currentStatusText }}
        </el-tag>

        <el-button data-testid="blog-save-draft" type="info" plain @click="saveDraft" :loading="savingDraft">
          存草稿
        </el-button>

        <el-button data-testid="blog-publish-submit" type="primary" @click="publishBlog" :loading="publishing">
          {{ publishButtonText }}
        </el-button>

        <el-button
          v-if="blog.id"
          type="danger"
          plain
          @click="deleteCurrentBlog"
          :loading="deletingBlog"
        >
          删除
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="showRejectBanner"
      class="reject-banner"
      type="error"
      :closable="false"
      show-icon
    >
      <template slot="title">
        该博客已被拒绝
      </template>
      <div class="reject-banner-text">
        {{ currentRejectReason || '管理员未填写拒绝原因，请修改后重新提交审核。' }}
      </div>
    </el-alert>

    <el-alert
      class="status-banner"
      type="info"
      :closable="false"
      show-icon
    >
      <template slot="title">
        当前状态：{{ currentStatusText }}
      </template>
      <div class="status-banner-text">
        {{ statusActionHint }}
      </div>
    </el-alert>

    <div class="edit-area">
      <el-input
        data-testid="blog-title-input"
        class="title-input"
        v-model="blog.title"
        placeholder="请输入博客标题"
        maxlength="100"
        show-word-limit
        clearable
      ></el-input>

      <div class="tag-selector">
        <span class="tag-label">标签：</span>
        <el-select
          v-model="blog.tags"
          multiple
          placeholder="请选择标签"
          class="tag-select"
          clearable
          :loading="loadingTags"
        >
          <el-option
            v-for="tag in tagOptions"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          ></el-option>
        </el-select>
      </div>

      <div class="summary-block">
        <div class="summary-label">摘要：</div>
        <el-input
          data-testid="blog-summary-input"
          v-model="blog.summary"
          type="textarea"
          :rows="3"
          maxlength="120"
          show-word-limit
          placeholder="请输入博客摘要，或使用 AI 自动生成"
        />
      </div>

      <div class="ai-helper-panel">
        <div class="ai-helper-main">
          <div class="ai-helper-title">
            <i class="el-icon-cpu"></i>
            AI 写作助手
          </div>
          <div class="ai-helper-desc">
            已接入博客写作 AI 能力，可按模型切换进行正文润色、摘要生成与标签推荐，结果会先在下方预览，再由你决定是否应用到表单。
          </div>
          <div class="ai-helper-meta">
            <div class="ai-helper-meta-item">
              <span class="ai-helper-meta-label">可用模型</span>
              <strong>{{ aiModelOptions.length || 0 }}</strong>
            </div>
            <div class="ai-helper-meta-item">
              <span class="ai-helper-meta-label">当前模型</span>
              <strong>{{ currentAiModelLabel }}</strong>
            </div>
          </div>
        </div>
        <div class="ai-helper-side">
          <div class="ai-model-field">
            <div class="ai-model-label">当前模型</div>
            <el-select
              v-model="selectedAiModelId"
              size="small"
              clearable
              filterable
              :loading="aiModelLoading"
              placeholder="请选择 AI 模型"
              class="ai-model-select"
              @change="handleAiModelChange"
            >
              <el-option
                v-for="item in aiModelOptions"
                :key="item.id"
                :label="getAiModelOptionLabel(item)"
                :value="item.id"
              ></el-option>
            </el-select>
          </div>
          <div class="ai-model-tag-row">
            <el-tag size="mini" type="success" effect="plain">{{ currentAiModelLabel }}</el-tag>
            <el-tag v-if="currentAiProviderLabel" size="mini" type="info" effect="plain">{{ currentAiProviderLabel }}</el-tag>
          </div>
        </div>
      </div>

      <div v-if="hasAiResult" class="ai-result-panel">
        <div class="ai-result-header">
          <span class="ai-result-title">
            <i class="el-icon-cpu"></i>
            AI 写作辅助结果
          </span>
          <div class="ai-result-header-right">
            <el-tag size="mini" type="success" effect="plain">{{ lastAiModelLabel || currentAiModelLabel }}</el-tag>
            <el-button type="text" icon="el-icon-delete" @click="clearAiResult">清空</el-button>
          </div>
        </div>

        <el-tabs v-model="aiResultTab" class="ai-result-tabs">
          <el-tab-pane label="润色结果" name="polish">
            <div v-if="aiPolishCard.polishedContent || aiPolishResult" class="ai-polish-card">
              <div class="ai-preview-head">
                <span class="ai-preview-title">精选润色正文</span>
                <el-tag size="mini" type="warning" effect="plain">已筛掉解释性废话，仅保留可发布正文</el-tag>
              </div>
              <div class="ai-rich-content ai-polish-preview" v-html="renderedAiPolishResult"></div>

              <div v-if="aiPolishCard.changeSummary.length" class="ai-struct-block">
                <div class="ai-struct-title">本次优化点</div>
                <ul class="ai-struct-list">
                  <li v-for="(item, index) in aiPolishCard.changeSummary" :key="'change-' + index">{{ item }}</li>
                </ul>
              </div>

              <div v-if="aiPolishCard.warnings.length" class="ai-struct-block">
                <div class="ai-struct-title">建议你再确认</div>
                <ul class="ai-struct-list ai-struct-list-warn">
                  <li v-for="(item, index) in aiPolishCard.warnings" :key="'warn-' + index">{{ item }}</li>
                </ul>
              </div>

              <div v-if="aiPolishCard.titleSuggestions.length" class="ai-struct-block">
                <div class="ai-struct-title">可选标题</div>
                <div class="ai-title-suggest-row">
                  <el-tag
                    v-for="(item, index) in aiPolishCard.titleSuggestions"
                    :key="'title-' + index"
                    size="mini"
                    effect="plain"
                    class="ai-tag-item ai-tag-item-ghost"
                  >
                    {{ item }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-empty v-else description="还没有生成润色结果" :image-size="72" />
            <div v-if="aiPolishCard.polishedContent || aiPolishResult" class="ai-result-actions">
              <el-button size="mini" type="success" plain @click="handleApplyAiPolish(aiPolishCard.polishedContent || aiPolishResult)">应用到正文</el-button>
              <el-button size="mini" type="text" icon="el-icon-document-copy" @click="copyText(aiPolishCard.polishedContent || aiPolishResult)">复制润色正文</el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="摘要 / 标签" name="summary">
            <div v-if="aiSummaryDisplayText" class="ai-summary-showcase">
              <div class="ai-summary-card">
                <div class="ai-preview-head">
                  <span class="ai-preview-title">精选摘要</span>
                  <el-tag size="mini" type="info" effect="plain">{{ aiSummaryDisplayText.length }}/120</el-tag>
                </div>
                <div class="ai-rich-content ai-summary-preview" v-html="renderedAiSummaryResult"></div>
              </div>

              <div v-if="aiSuggestedTagNames.length > 0 || aiSuggestedTags.length > 0 || aiSummaryCard.rejectedTags.length > 0" class="ai-tag-section">
                <div v-if="aiSuggestedTagNames.length > 0" class="ai-tag-suggest">
                  <span class="ai-tag-label">AI 原始候选：</span>
                  <el-tag
                    v-for="tagName in aiSuggestedTagNames"
                    :key="`raw-${tagName}`"
                    size="mini"
                    effect="plain"
                    class="ai-tag-item ai-tag-item-ghost"
                  >
                    {{ tagName }}
                  </el-tag>
                </div>

                <div v-if="aiSuggestedTags.length > 0" class="ai-tag-suggest">
                  <span class="ai-tag-label">筛选后系统标签：</span>
                  <el-tag
                    v-for="tag in aiSuggestedTags"
                    :key="tag.id"
                    size="mini"
                    type="success"
                    class="ai-tag-item"
                  >
                    {{ tag.name }}
                  </el-tag>
                </div>

                <div v-if="aiSummaryCard.rejectedTags.length > 0" class="ai-tag-suggest">
                  <span class="ai-tag-label">已剔除泛化标签：</span>
                  <el-tag
                    v-for="tagName in aiSummaryCard.rejectedTags"
                    :key="`rejected-${tagName}`"
                    size="mini"
                    type="info"
                    effect="plain"
                    class="ai-tag-item ai-tag-item-ghost"
                  >
                    {{ tagName }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-empty v-else description="还没有生成摘要结果" :image-size="72" />

            <div v-if="aiSummaryDisplayText" class="ai-result-actions">
              <el-button size="mini" type="primary" plain @click="applyAiSummaryToForm">应用摘要到表单</el-button>
              <el-button size="mini" type="success" plain :disabled="!aiSuggestedTags.length" @click="applyMatchedAiTags">应用匹配标签</el-button>
              <el-button size="mini" type="text" icon="el-icon-document-copy" @click="copyText(aiSummaryDisplayText)">复制摘要</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="blog-type-option">
        <div class="type-label">
          <i class="el-icon-s-operation"></i> 博客类型
        </div>
        <el-radio-group v-model="selectedBlogType" @change="handleBlogTypeChange">
          <el-radio label="free">免费博客</el-radio>
          <el-radio label="vip">VIP专属博客</el-radio>
          <el-radio label="paid">付费博客</el-radio>
        </el-radio-group>
        <div v-if="selectedBlogType === 'paid'" class="price-input">
          <el-input-number
            v-model="customPrice"
            :min="0.01"
            :max="9999"
            :step="0.01"
            :precision="2"
            placeholder="请输入价格"
            style="width: 200px; margin-left: 10px;"
            @change="updatePaidPrice"
          ></el-input-number>
          <span class="price-unit">元</span>
        </div>
        <div class="type-tip">
          <i class="el-icon-info"></i> 免费博客：所有人可阅读 | VIP专属：仅VIP会员可阅读 | 付费博客：用户需付费阅读
        </div>
      </div>

      <div v-if="isClient" class="editor-container" data-testid="blog-editor-container">
        <div id="toolbar"></div>
        <div id="editor" class="ql-editor-container"></div>
      </div>
      <div v-else class="editor-placeholder">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <el-drawer
      title="我的博客"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="420px"
      :before-close="handleDrawerClose"
    >
      <div class="draft-list" v-loading="loadingDrafts">
        <el-card
          v-for="draft in draftList"
          :key="draft.id"
          class="draft-item"
          shadow="hover"
          @click.native="editDraft(draft.id)"
        >
          <div class="draft-head">
            <h4>{{ draft.title || '无标题' }}</h4>
            <div class="draft-head-right">
              <el-tag :type="getDraftStatusTagType(draft.status)" size="mini">
                {{ getDraftStatusText(draft.status) }}
              </el-tag>
              <el-button
                type="text"
                size="mini"
                icon="el-icon-delete"
                class="draft-delete-btn"
                @click.stop="removeBlogFromList(draft)"
              >
                删除
              </el-button>
            </div>
          </div>

          <div v-if="draft.rejectReason" class="draft-reject-reason">
            驳回原因：{{ draft.rejectReason }}
          </div>

          <div class="draft-summary rich-preview" v-html="buildDraftPreview(draft)"></div>
          <div class="draft-status-hint">
            {{ getDraftStatusHint(draft.status) }}
          </div>

          <div class="draft-meta">
            <span>最后更新：{{ formatTime(draft.updatedAt || draft.createTime) }}</span>
          </div>
        </el-card>

        <el-pagination
          v-if="draftTotal > pageSize"
          background
          layout="prev, pager, next"
          :total="draftTotal"
          :page-size="pageSize"
          :current-page.sync="draftPage"
          @current-change="fetchDrafts"
          class="draft-pagination"
        ></el-pagination>

        <div v-if="draftList.length === 0 && !loadingDrafts" class="empty-draft">
          暂无博客
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  blogStatusTagType,
  blogStatusText,
  fetchBlogDetail as fetchBlogDetailApi,
  fetchBlogTags,
  fetchCurrentUserProfile,
  fetchMyBlogList,
  removeBlog,
  saveBlog as saveBlogApi,
  uploadBlogImage
} from '@/api/blog'
import { getToken } from '@/utils/auth'
import { pickAvatarUrl } from '@/utils/avatar'
import {
  aiPolishBlog,
  aiGenerateBlogSummary,
  normalizeBlogPolishPayload,
  normalizeBlogSummaryPayload,
  matchSystemTagsByNames
} from '@/api/aiAssistant'
import { listEnabledAiModels, pageAiModels } from '@/api/aiAdmin'
import { renderRichContent } from '@/utils/richContent'
import { collectBlogWriteContext, buildBlogWritePrompt } from '@/utils/aiContextCollectors'
import { createBlogWriteAiApplyHandlers, resolveBlogWriteActionCode } from '@/utils/aiApplyHandlers'

function extractApiData(res) {
  if (res == null) return null
  const payload = res.data !== undefined ? res.data : res
  if (
    payload &&
    typeof payload === 'object' &&
    payload.data !== undefined &&
    (payload.code !== undefined || payload.success !== undefined || payload.message !== undefined)
  ) {
    return payload.data
  }
  return payload
}

function normalizeAiModel(item = {}) {
  const rawId = item.id ?? item.modelId ?? item.value ?? item.code ?? ''
  return {
    ...item,
    id: rawId === null || rawId === undefined ? '' : String(rawId),
    rawId,
    modelName: item.modelName || item.name || item.label || item.model || item.code || '',
    providerCode: item.providerCode || item.provider || item.providerName || item.vendor || item.platform || '',
    isEnabled: item.isEnabled !== false
  }
}

function escapeHtmlValue(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInlineMarkdown(text) {
  return escapeHtmlValue(text)
    .replace(/&lt;br\s*\/?&gt;/gi, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
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

function looksLikeHtml(text) {
  return /<\/?[a-z][\s\S]*>/i.test(String(text || ''))
}

function looksLikeMarkdown(text) {
  const value = String(text || '')
  return /(^|\n)#{1,6}\s+|\*\*.+?\*\*|(^|\n)\s*[-*+]\s+|(^|\n)\s*\d+\.\s+|\|.+\|/.test(value)
}

function normalizeHtmlContent(source, emptyText = '暂无内容') {
  const raw = String(source || '').trim()
  if (!raw) return `<div class="empty-ai-content">${escapeHtmlValue(emptyText)}</div>`
  return raw.replace(/&lt;br\s*\/?&gt;/gi, '<br>')
}

function renderMarkdownToHtml(source, emptyText = '暂无内容') {
  const raw = String(source || '').replace(/\r\n?/g, '\n').trim()
  if (!raw) {
    return `<div class="empty-ai-content">${escapeHtmlValue(emptyText)}</div>`
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
      if (i < lines.length && /^```/.test(String(lines[i] || '').trim())) i += 1
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
      blocks.push(`<div class="markdown-table-wrap"><table>${thead}${tbody}</table></div>`)
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

    if (/^\s*[-*+]\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
        items.push(renderInlineMarkdown(String(lines[i] || '').replace(/^\s*[-*+]\s+/, '')))
        i += 1
      }
      blocks.push(`<ul>${items.map(item => `<li>${item}</li>`).join('')}</ul>`)
      continue
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(renderInlineMarkdown(String(lines[i] || '').replace(/^\s*\d+\.\s+/, '')))
        i += 1
      }
      blocks.push(`<ol>${items.map(item => `<li>${item}</li>`).join('')}</ol>`)
      continue
    }

    const paragraphLines = []
    while (i < lines.length) {
      const current = String(lines[i] || '')
      const currentTrimmed = current.trim()
      if (!currentTrimmed) {
        i += 1
        break
      }
      if (paragraphLines.length > 0 && isSpecialMarkdownLine(current, lines[i + 1])) {
        break
      }
      paragraphLines.push(renderInlineMarkdown(currentTrimmed))
      i += 1
    }

    if (paragraphLines.length) {
      blocks.push(`<p>${paragraphLines.join('<br>')}</p>`)
    }
  }

  return blocks.join('')
}

function getStoredToken() {
  if (!process.client) return ''
  return getToken() || ''
}

function decodeJwtPayload(token = '') {
  try {
    const parts = String(token || '').split('.')
    if (parts.length < 2) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + (4 - (base64.length % 4 || 4)) % 4, '=')
    const json = process.client ? window.atob(normalized) : Buffer.from(normalized, 'base64').toString('utf-8')
    return JSON.parse(json)
  } catch (e) {
    return null
  }
}

function pickUserIdFromObject(source) {
  if (!source || typeof source !== 'object') return null
  const queue = [source]
  const seen = new Set()
  const keys = ['id', 'userId', 'uid', 'memberId', 'loginId', 'accountId', 'sub']

  while (queue.length) {
    const current = queue.shift()
    if (!current || typeof current !== 'object' || seen.has(current)) continue
    seen.add(current)

    for (const key of keys) {
      const value = current[key]
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        const text = String(value).trim()
        if (/^\d+$/.test(text)) return Number(text)
        return text
      }
    }

    ;['user', 'userInfo', 'profile', 'account', 'loginUser', 'currentUser', 'data'].forEach((key) => {
      if (current[key] && typeof current[key] === 'object') {
        queue.push(current[key])
      }
    })
  }

  return null
}

function tryParseJsonLikeValue(value) {
  if (typeof value !== 'string') return value
  const text = value.trim()
  if (!text) return value
  if (!((text.startsWith('{') && text.endsWith('}')) || (text.startsWith('[') && text.endsWith(']')))) {
    return value
  }
  try {
    return JSON.parse(text)
  } catch (e) {
    return value
  }
}

function extractReadableText(value, preferredKeys = [], depth = 0) {
  if (depth > 6 || value === null || value === undefined) return ''

  const normalized = tryParseJsonLikeValue(value)

  if (normalized === null || normalized === undefined) return ''
  if (typeof normalized === 'string') return normalized.trim()
  if (typeof normalized === 'number' || typeof normalized === 'boolean') return String(normalized)

  if (Array.isArray(normalized)) {
    return normalized
      .map(item => extractReadableText(item, preferredKeys, depth + 1))
      .filter(Boolean)
      .join('\n')
      .trim()
  }

  if (typeof normalized !== 'object') return ''

  const priorityKeys = [...preferredKeys, 'content', 'text', 'markdown', 'html', 'answer', 'result', 'output', 'message', 'value', 'description', 'desc', 'body']
  for (const key of priorityKeys) {
    if (normalized[key] !== undefined && normalized[key] !== null) {
      const text = extractReadableText(normalized[key], preferredKeys, depth + 1)
      if (text) return text
    }
  }

  const ignoreKeys = new Set(['id', 'modelId', 'provider', 'providerCode', 'code', 'status', 'success', 'tags', 'tagList', 'labels', 'keywords'])
  for (const [key, val] of Object.entries(normalized)) {
    if (ignoreKeys.has(key)) continue
    const text = extractReadableText(val, preferredKeys, depth + 1)
    if (text) return text
  }

  return ''
}

function uniqTextList(list = []) {
  const seen = new Set()
  return list
    .map(item => String(item || '').trim())
    .filter(Boolean)
    .filter(item => {
      const key = item.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

function normalizeDisplayText(value, preferredKeys = []) {
  const text = extractReadableText(value, preferredKeys)
  if (text) return text
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch (e) {
    return String(value)
  }
}

export default {
  name: 'WriteBlog',
  layout: 'blogwrite',

  data() {
    return {
      blog: {
        id: null,
        title: '',
        summary: '',
        content: '',
        tags: [],
        status: 'draft',
        price: 0
      },
      tagOptions: [],
      loadingTags: false,
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      username: '当前用户',
      userId: '',
      savingDraft: false,
      publishing: false,
      deletingBlog: false,
      lastSaved: '',
      selectedBlogType: 'free',
      customPrice: 9.99,
      drawerVisible: false,
      draftList: [],
      loadingDrafts: false,
      draftPage: 1,
      pageSize: 10,
      draftTotal: 0,
      quill: null,
      isClient: false,
      aiPolishing: false,
      aiSummarizing: false,
      aiModelLoading: false,
      aiModelOptions: [],
      activeAiModel: null,
      selectedAiModelId: null,
      aiSummaryResult: '',
      aiSummaryCard: {
        summary: '',
        tags: [],
        rejectedTags: [],
        rawText: ''
      },
      aiPolishResult: '',
      aiPolishCard: {
        polishedContent: '',
        changeSummary: [],
        warnings: [],
        titleSuggestions: [],
        rawText: ''
      },
      aiSuggestedTags: [],
      aiResultTab: 'summary',
      lastAiModelLabel: '',
      showAiResult: false,
      aiStreaming: false,
      aiStreamingType: '',
      aiStreamStopper: null,
      aiBlogAiSessionId: null,
      aiResultUnsubscribe: null,
      aiResultWindowListener: null,
      aiContextCollectorDisposer: null,
      aiPendingActionCode: '',
      aiPendingTimeoutId: null,
      currentRejectReason: ''
    }
  },

  watch: {
    'blog.summary'(value) {
      if (typeof value === 'string') return
      const next = normalizeDisplayText(value, ['summary', 'abstract', 'digest'])
      if (next !== value) this.$set(this.blog, 'summary', next)
    },
    aiSummaryResult(value) {
      if (typeof value === 'string') return
      const next = normalizeDisplayText(value, ['summary', 'abstract', 'digest'])
      if (next !== value) this.aiSummaryResult = next
    },
    aiPolishResult(value) {
      if (typeof value === 'string') return
      const next = normalizeDisplayText(value, ['content', 'text', 'html', 'result'])
      if (next !== value) this.aiPolishResult = next
    }
  },

  computed: {
    hasAiResult() {
      return !!(
        this.aiPolishCard.polishedContent ||
        this.aiPolishResult ||
        this.aiSummaryCard.summary ||
        this.aiSummaryResult ||
        this.aiSuggestedTags.length
      )
    },
    selectedAiModel() {
      const targetId = this.selectedAiModelId
      if (targetId === null || targetId === undefined || targetId === '') return this.activeAiModel
      return this.aiModelOptions.find(item => String(item.id) === String(targetId)) || this.activeAiModel
    },
    currentAiModelLabel() {
      const model = this.selectedAiModel || this.activeAiModel
      return model && model.modelName ? model.modelName : '默认激活模型'
    },
    currentAiProviderLabel() {
      const model = this.selectedAiModel || this.activeAiModel
      return model && model.providerCode ? model.providerCode : ''
    },
    aiSummaryDisplayText() {
      return normalizeDisplayText(this.aiSummaryCard.summary || this.aiSummaryResult, ['summary', 'abstract', 'digest']).trim()
    },
    aiSuggestedTagNames() {
      return uniqTextList(Array.isArray(this.aiSummaryCard.tags) ? this.aiSummaryCard.tags : [])
    },
    renderedAiSummaryResult() {
      return this.renderAiResultContent(this.aiSummaryDisplayText, 'markdown', '暂无 AI 摘要结果')
    },
    renderedAiPolishResult() {
      return this.renderAiResultContent(this.aiPolishCard.polishedContent || this.aiPolishResult, 'auto', '暂无 AI 润色结果')
    },
    showRejectBanner() {
      return this.blog.status === 'rejected'
    },
    currentStatusText() {
      return blogStatusText(this.blog.status)
    },
    currentStatusTagType() {
      return blogStatusTagType(this.blog.status)
    },
    statusActionHint() {
      const status = String(this.blog.status || '').toLowerCase()
      if (status === 'draft') return '可继续编辑并保存草稿，也可提交审核。'
      if (status === 'pending') return '博客正在审核中，修改后可再次提交审核。'
      if (status === 'published') return '博客已发布，修改后重新提交将进入审核流程。'
      if (status === 'rejected') return '请根据拒绝原因修改内容后重新提交审核。'
      return '请继续完善内容。'
    },
    publishButtonText() {
      const status = String(this.blog.status || '').toLowerCase()
      if (status === 'rejected') return '修改后重新提交'
      if (status === 'published') return '更新并重新提交'
      if (status === 'pending') return '重新提交审核'
      return '提交审核'
    }
  },

  mounted() {
    this.isClient = true
    this.bindAiAssistantBridge()
    this.$nextTick(() => {
      this.initEditor()
    })
  },

  created() {
    this.fetchTags()
    this.restoreUserIdentityFromCache()
    this.loadUserInfo()
    this.initAiModels()

    const blogId = this.$route.params.id
    if (blogId) {
      this.fetchBlog(blogId)
    }
  },

  beforeDestroy() {
    this.unbindAiAssistantBridge()
    this.finishAiPending({ silent: true })
    if (this.quill) {
      this.quill = null
    }
  },

  methods: {
    bindAiAssistantBridge() {
      if (!process.client) return
      this.unbindAiAssistantBridge()
      if (this.$aiActionBridge && typeof this.$aiActionBridge.registerContextCollector === 'function') {
        this.aiContextCollectorDisposer = this.$aiActionBridge.registerContextCollector('blog.write', () => this.collectAiContextPayload())
      }
      if (this.$aiActionBridge && typeof this.$aiActionBridge.subscribeResult === 'function') {
        this.aiResultUnsubscribe = this.$aiActionBridge.subscribeResult(
          detail => this.handleAiAssistantResultEvent(detail),
          { sceneCode: 'blog.write' }
        )
        return
      }
      this.aiResultWindowListener = event => this.handleAiAssistantResultEvent(event && event.detail ? event.detail : {})
      window.addEventListener('ai-assistant-result', this.aiResultWindowListener)
    },
    unbindAiAssistantBridge() {
      if (typeof this.aiResultUnsubscribe === 'function') {
        this.aiResultUnsubscribe()
      }
      this.aiResultUnsubscribe = null
      if (typeof this.aiContextCollectorDisposer === 'function') {
        this.aiContextCollectorDisposer()
      }
      this.aiContextCollectorDisposer = null
      if (process.client && this.aiResultWindowListener) {
        window.removeEventListener('ai-assistant-result', this.aiResultWindowListener)
      }
      this.aiResultWindowListener = null
    },
    syncAiAssistantModelPreference() {
      if (!process.client) return
      const key = 'ai_assistant_selected_model_id'
      const modelId = this.selectedAiModelId === null || this.selectedAiModelId === undefined
        ? ''
        : String(this.selectedAiModelId).trim()
      if (modelId) {
        window.localStorage.setItem(key, modelId)
      } else {
        window.localStorage.removeItem(key)
      }
      window.dispatchEvent(new Event('storage'))
    },
    collectAiContextPayload() {
      return collectBlogWriteContext({
        blog: this.blog,
        quill: this.quill,
        tagOptions: this.tagOptions
      })
    },
    prepareAiResultPanel(actionCode) {
      if (actionCode === 'blog.polish') {
        this.aiPolishResult = ''
        this.aiPolishCard = {
          polishedContent: '',
          changeSummary: [],
          warnings: [],
          titleSuggestions: [],
          rawText: ''
        }
        this.aiResultTab = 'polish'
        return
      }
      if (actionCode === 'blog.summary') {
        this.aiSummaryResult = ''
        this.aiSummaryCard = {
          summary: '',
          tags: [],
          rejectedTags: [],
          rawText: ''
        }
        this.aiSuggestedTags = []
        this.aiResultTab = 'summary'
      }
    },
    startAiPending(actionCode) {
      this.finishAiPending({ silent: true })
      this.aiPendingActionCode = actionCode
      this.aiPolishing = actionCode === 'blog.polish'
      this.aiSummarizing = actionCode === 'blog.summary'
      if (process.client) {
        this.aiPendingTimeoutId = window.setTimeout(() => {
          const pendingAction = this.aiPendingActionCode
          this.finishAiPending({ silent: true })
          if (pendingAction) {
            this.$message.warning('No AI result received in time. Please retry.')
          }
        }, 180000)
      }
    },
    finishAiPending({ silent = false } = {}) {
      if (this.aiPendingTimeoutId && process.client) {
        window.clearTimeout(this.aiPendingTimeoutId)
      }
      this.aiPendingTimeoutId = null
      const hadPending = !!this.aiPendingActionCode
      this.aiPendingActionCode = ''
      this.aiPolishing = false
      this.aiSummarizing = false
      if (!silent && hadPending) {
        this.$message.info('AI generation canceled.')
      }
    },
    openAiAssistantWithAction(actionCode) {
      if (!process.client) return false
      if (!this.$aiActionBridge || typeof this.$aiActionBridge.open !== 'function') {
        this.$message.error('AI bridge is unavailable on current page.')
        return false
      }
      const contextPayload = this.collectAiContextPayload()
      const prompt = buildBlogWritePrompt(actionCode, contextPayload)
      const detail = {
        prompt,
        sceneCode: 'blog.write',
        actionCode,
        contextPayload: {
          ...contextPayload,
          blogId: this.blog.id || null,
          selectedModelId: this.selectedAiModelId || null,
          aiSessionId: this.aiBlogAiSessionId || null
        },
        source: 'blog.write.page',
        autoSend: true
      }
      this.prepareAiResultPanel(actionCode)
      this.showAiResult = true
      this.lastAiModelLabel = this.currentAiModelLabel
      this.syncAiAssistantModelPreference()
      this.startAiPending(actionCode)
      this.$aiActionBridge.open(detail)
      return true
    },
    handleAiAssistantResultEvent(rawDetail = {}) {
      const detail = this.$aiActionBridge && typeof this.$aiActionBridge.normalizeResultDetail === 'function'
        ? this.$aiActionBridge.normalizeResultDetail(rawDetail)
        : rawDetail
      const sceneCode = String((detail && detail.sceneCode) || '').trim().toLowerCase()
      if (sceneCode && sceneCode !== 'blog.write') return

      const actionCode = resolveBlogWriteActionCode(detail)
      if (!actionCode) return
      if (this.aiPendingActionCode && this.aiPendingActionCode !== actionCode) return

      if (detail && detail.rawResponse && detail.rawResponse.sessionId) {
        this.aiBlogAiSessionId = detail.rawResponse.sessionId
      }

      const handlerMap = createBlogWriteAiApplyHandlers(this)
      const applyHandler = handlerMap[actionCode]
      if (!applyHandler) return

      let applyResult = null
      try {
        applyResult = applyHandler(detail)
      } catch (error) {
        console.error('Apply AI blog result failed:', error)
        this.finishAiPending({ silent: true })
        this.$message.error('AI result parse failed, please retry.')
        return
      }

      this.finishAiPending({ silent: true })
      if (applyResult && applyResult.applied) {
        this.$message.success(applyResult.message || 'AI result applied.')
        return
      }
      this.$message.warning((applyResult && applyResult.message) || 'AI result has no applicable structured fields.')
    },
    handleBlogTypeChange(value) {
      this.selectedBlogType = value
      switch (value) {
        case 'free':
          this.blog.price = 0
          break
        case 'vip':
          this.blog.price = -1
          break
        case 'paid':
          this.blog.price = this.customPrice
          break
      }
    },
    updatePaidPrice(value) {
      this.customPrice = value
      if (this.selectedBlogType === 'paid') {
        this.blog.price = value
      }
    },

    stopAiStream(silent = false) {
      if (typeof this.aiStreamStopper === 'function') {
        this.aiStreamStopper()
      }
      this.aiStreamStopper = null
      const wasStreaming = this.aiStreaming
      this.aiStreaming = false
      this.aiStreamingType = ''
      this.aiPolishing = false
      this.aiSummarizing = false
      if (!silent && wasStreaming) {
        this.$message.info('已停止当前 AI 生成')
      }
    },
    getAiModelStorageKey() {
      return 'blog_write_ai_model_id'
    },
    getAiModelOptionLabel(item = {}) {
      const modelName = item.modelName || item.name || `模型#${item.id || '-'}`
      const provider = item.providerCode || item.provider || item.platform || ''
      return provider ? `${modelName}（${provider}）` : modelName
    },
    async initAiModels() {
      this.aiModelLoading = true
      try {
        let enabled = []
        try {
          const enabledRes = await listEnabledAiModels()
          const enabledData = extractApiData(enabledRes)
          enabled = Array.isArray(enabledData) ? enabledData.map(normalizeAiModel) : []
        } catch (e) {
          console.error('加载已启用模型失败:', e)
        }

        if (!enabled.length) {
          try {
            const pageRes = await pageAiModels({ page: 0, size: 100 })
            const pagePayload = extractApiData(pageRes)
            enabled = Array.isArray(pagePayload?.content)
              ? pagePayload.content.map(normalizeAiModel)
              : Array.isArray(pagePayload?.records)
                ? pagePayload.records.map(normalizeAiModel)
                : Array.isArray(pagePayload?.list)
                  ? pagePayload.list.map(normalizeAiModel)
                  : Array.isArray(pagePayload)
                    ? pagePayload.map(normalizeAiModel)
                    : []
          } catch (e) {
            console.error('兜底加载全部模型失败:', e)
          }
        }

        this.aiModelOptions = enabled

        let cached = null
        if (process.client) {
          cached = window.localStorage.getItem(this.getAiModelStorageKey())
        }

        const preferredModelId = cached
          ? String(cached)
          : (enabled[0] && enabled[0].id) || null

        this.selectedAiModelId = preferredModelId === '' ? null : preferredModelId
        this.activeAiModel = enabled.find(item => String(item.id) === String(this.selectedAiModelId)) || enabled[0] || null
      } finally {
        this.aiModelLoading = false
      }
    },
    handleAiModelChange(val) {
      const modelId = val === '' || val === undefined || val === null ? null : String(val)
      this.selectedAiModelId = modelId
      if (!process.client) return
      const key = this.getAiModelStorageKey()
      if (modelId === null) {
        window.localStorage.removeItem(key)
      } else {
        window.localStorage.setItem(key, modelId)
      }
    },
    renderAiResultContent(source, mode = 'auto', emptyText = '暂无内容') {
      const raw = normalizeDisplayText(source, ['summary', 'abstract', 'digest', 'content', 'text', 'html', 'result']).trim()
      if (!raw) return `<div class="empty-ai-content">${emptyText}</div>`
      if (mode === 'html') return normalizeHtmlContent(raw, emptyText)
      if (mode === 'markdown') return renderMarkdownToHtml(raw, emptyText)
      if (looksLikeHtml(raw) && !looksLikeMarkdown(raw)) {
        return normalizeHtmlContent(raw, emptyText)
      }
      return renderMarkdownToHtml(raw, emptyText)
    },
    matchAiTagsToOptions(tagNames = []) {
      if (!Array.isArray(tagNames) || tagNames.length === 0) return []
      return this.tagOptions.filter(option => {
        const optionName = String(option.name || '').trim().toLowerCase()
        return tagNames.some(tag => {
          const tagName = String(tag || '').trim().toLowerCase()
          return optionName === tagName || optionName.includes(tagName) || tagName.includes(optionName)
        })
      })
    },
    async handleAiPolishLegacy() {
      return this.handleAiPolish()
      if (!this.blog.title.trim()) {
        this.$message.warning('请先填写博客标题')
        return
      }
      const contentText = this.stripHtml(this.blog.content || '').trim()
      if (!contentText) {
        this.$message.warning('请先填写博客内容')
        return
      }
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.stopAiStream(true)
      this.aiPolishing = true
      this.aiStreaming = false
      this.aiStreamingType = ''
      this.aiPolishResult = ''
      this.aiPolishCard = {
        polishedContent: '',
        changeSummary: [],
        warnings: [],
        titleSuggestions: [],
        rawText: ''
      }
      this.aiResultTab = 'polish'
      this.lastAiModelLabel = this.currentAiModelLabel
      this.showAiResult = true

      try {
        const result = await aiPolishBlog({
          userId: userId || undefined,
          sessionId: this.aiBlogAiSessionId || undefined,
          modelId: this.selectedAiModelId || undefined,
          title: this.blog.title,
          content: contentText
        })

        if (result && result.sessionId) {
          this.aiBlogAiSessionId = result.sessionId
        }

        const normalized = result?.normalized || normalizeBlogPolishPayload(result)
        this.aiPolishCard = normalized
        this.aiPolishResult = normalized.polishedContent || result?.displayText || result?.text || ''
        this.$message.success('AI 润色完成')
      } catch (error) {
        console.error('AI 润色失败:', error)
        this.$message.error(error?.response?.data?.message || error?.message || 'AI 润色失败，请稍后重试')
      } finally {
        this.aiStreamStopper = null
        this.aiPolishing = false
        this.aiStreaming = false
        this.aiStreamingType = ''
      }
    },
    async handleAiGenerateSummaryLegacy() {
      return this.handleAiGenerateSummary()
      if (!this.blog.title.trim()) {
        this.$message.warning('请先填写博客标题')
        return
      }
      const contentText = this.stripHtml(this.blog.content || '').trim()
      if (!contentText) {
        this.$message.warning('请先填写博客内容')
        return
      }
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.stopAiStream(true)
      this.aiSummarizing = true
      this.aiStreaming = false
      this.aiStreamingType = ''
      this.aiSummaryResult = ''
      this.aiSummaryCard = {
        summary: '',
        tags: [],
        rejectedTags: [],
        rawText: ''
      }
      this.aiSuggestedTags = []
      this.aiResultTab = 'summary'
      this.lastAiModelLabel = this.currentAiModelLabel
      this.showAiResult = true

      try {
        const result = await aiGenerateBlogSummary({
          userId: userId || undefined,
          sessionId: this.aiBlogAiSessionId || undefined,
          modelId: this.selectedAiModelId || undefined,
          title: this.blog.title,
          content: contentText
        })

        if (result && result.sessionId) {
          this.aiBlogAiSessionId = result.sessionId
        }

        const normalized = result?.normalized || normalizeBlogSummaryPayload(result)
        this.aiSummaryResult = normalized.summary || result?.displayText || result?.text || ''
        this.aiSummaryCard = {
          summary: normalized.summary || '',
          tags: Array.isArray(normalized.tags) ? normalized.tags : [],
          rejectedTags: Array.isArray(normalized.rejectedTags) ? normalized.rejectedTags : [],
          rawText: normalized.rawText || result?.text || ''
        }
        this.aiSuggestedTags = matchSystemTagsByNames(this.aiSummaryCard.tags, this.tagOptions || [])
        this.$message.success('AI 摘要/标签生成完成')
      } catch (error) {
        console.error('AI 生成摘要失败:', error)
        this.$message.error(error?.response?.data?.message || error?.message || 'AI 生成摘要失败，请稍后重试')
      } finally {
        this.aiStreamStopper = null
        this.aiSummarizing = false
        this.aiStreaming = false
        this.aiStreamingType = ''
      }
    },
    applyAiSummaryToForm() {
      if (!this.aiSummaryDisplayText) {
        this.$message.warning('没有可应用的摘要')
        return
      }
      this.blog.summary = normalizeDisplayText(this.aiSummaryDisplayText, ['summary', 'abstract', 'digest'])
      this.$message.success('AI 摘要已应用到表单')
    },
    applyMatchedAiTags() {
      if (!this.aiSuggestedTags.length) {
        this.$message.warning('没有可应用的系统标签')
        return
      }
      const currentTagIds = Array.isArray(this.blog.tags) ? this.blog.tags.slice() : []
      this.blog.tags = [...new Set([...currentTagIds, ...this.aiSuggestedTags.map(item => item.id)])]
      this.$message.success('匹配标签已应用到表单')
    },
    clearAiResult() {
      this.stopAiStream(true)
      this.finishAiPending({ silent: true })
      this.aiSummaryResult = ''
      this.aiSummaryCard = {
        summary: '',
        tags: [],
        rejectedTags: [],
        rawText: ''
      }
      this.aiPolishResult = ''
      this.aiPolishCard = {
        polishedContent: '',
        changeSummary: [],
        warnings: [],
        titleSuggestions: [],
        rawText: ''
      }
      this.aiSuggestedTags = []
      this.aiResultTab = 'summary'
      this.lastAiModelLabel = ''
      this.showAiResult = false
    },
    copyText(text) {
      const value = normalizeDisplayText(text, ['summary', 'abstract', 'digest', 'content', 'text', 'html', 'result'])
      if (!value) {
        this.$message.warning('没有可复制的内容')
        return
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(value)
          .then(() => this.$message.success('复制成功'))
          .catch(() => this.$message.error('复制失败'))
        return
      }
      const textarea = document.createElement('textarea')
      textarea.value = value
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success('复制成功')
    },
    handleApplyAiPolish(content) {
      const nextContent = normalizeDisplayText(content, ['content', 'text', 'html', 'result', 'answer', 'message']).trim()
      if (!nextContent) {
        this.$message.warning('没有可应用的正文')
        return
      }
      this.blog.content = nextContent
      if (this.quill) {
        this.quill.root.innerHTML = nextContent
      }
      this.$message.success('AI 润色结果已应用到正文')
    },

    restoreUserIdentityFromCache() {
      if (!process.client) return
      const storageKeys = ['userInfo', 'user', 'loginUser', 'currentUser', 'Admin-User', 'auth_user', 'authUser', 'memberInfo']
      for (const storage of [window.localStorage, window.sessionStorage]) {
        for (const key of storageKeys) {
          try {
            const raw = storage.getItem(key)
            if (!raw) continue
            const parsed = JSON.parse(raw)
            const foundId = pickUserIdFromObject(parsed)
            if (!this.userId && foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
              this.userId = foundId
            }
            const profile = parsed.user || parsed.userInfo || parsed.profile || parsed.currentUser || parsed
            if (!this.username && profile) {
              this.username = profile.nickname || profile.username || this.username || '当前用户'
            }
            if (!this.userAvatar && profile) {
              this.userAvatar = pickAvatarUrl(profile.avatarUrl, profile.avatar, this.userAvatar)
            }
            if (this.userId) return
          } catch (e) {}
        }
      }
      try {
        const nuxtState = window.__NUXT__
        const foundId = pickUserIdFromObject(nuxtState)
        if (!this.userId && foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
          this.userId = foundId
        }
      } catch (e) {}
      if (!this.userId) {
        const token = getStoredToken()
        const payload = decodeJwtPayload(token)
        const foundId = pickUserIdFromObject(payload)
        if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
          this.userId = foundId
        }
      }
    },
    getCurrentAiUserId() {
      const directCandidates = [this.userId].filter(value => value !== undefined && value !== null && String(value).trim() !== '')
      if (directCandidates.length) {
        const value = String(directCandidates[0]).trim()
        return /^\d+$/.test(value) ? Number(value) : value
      }
      if (process.client) {
        const storageKeys = ['userInfo', 'user', 'loginUser', 'currentUser', 'Admin-User', 'auth_user', 'authUser', 'memberInfo']
        for (const storage of [window.localStorage, window.sessionStorage]) {
          for (const key of storageKeys) {
            try {
              const raw = storage.getItem(key)
              if (!raw) continue
              const parsed = JSON.parse(raw)
              const foundId = pickUserIdFromObject(parsed)
              if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
                return foundId
              }
            } catch (e) {}
          }
        }
        try {
          const nuxtState = window.__NUXT__
          const foundId = pickUserIdFromObject(nuxtState)
          if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
            return foundId
          }
        } catch (e) {}
      }
      const token = getStoredToken()
      if (token) {
        const payload = decodeJwtPayload(token)
        const foundId = pickUserIdFromObject(payload)
        if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
          return foundId
        }
      }
      return null
    },
    hasAiLoginContext() {
      const userId = this.getCurrentAiUserId()
      if (userId !== null && userId !== undefined && String(userId).trim() !== '') {
        return true
      }
      return !!getStoredToken()
    },
    goToUserHome() {
      if (!this.userId) {
        this.$message.warning('暂未获取到当前用户信息')
        return
      }
      this.$router.push('/user')
    },
    loadUserInfo() {
      this.fetchUserInfoFromApi()
    },
    async fetchUserInfoFromApi() {
      try {
        const userData = await fetchCurrentUserProfile()
        if (userData && userData.id) {
          this.userId = userData.id
          this.username = userData.nickname || userData.username || '当前用户'
          this.userAvatar = pickAvatarUrl(userData.avatarUrl, userData.avatar, this.userAvatar)
          if (process.client) {
            try {
              localStorage.setItem('userInfo', JSON.stringify(userData))
            } catch (storageError) {
              console.error('存储用户信息失败:', storageError)
            }
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.restoreUserIdentityFromCache()
      }
    },

    async fetchTags() {
      this.loadingTags = true
      try {
        this.tagOptions = await fetchBlogTags()
      } catch (error) {
        console.error('获取标签失败:', error)
        this.$message.error('获取标签失败：' + (error.message || '网络错误'))
        this.tagOptions = []
      } finally {
        this.loadingTags = false
      }
    },

    async fetchBlog(blogId) {
      try {
        const blogData = await fetchBlogDetailApi(blogId)
        if (blogData) {
          this.applyBlogToEditor(blogData)
        }
      } catch (error) {
        console.error('获取博客失败:', error)
        this.$message.error('网络错误，请稍后重试')
      }
    },
    applyBlogToEditor(blogData) {
      this.blog = {
        id: blogData.id,
        title: blogData.title || '',
        content: blogData.content || '',
        tags: Array.isArray(blogData.tagIds)
          ? blogData.tagIds
          : this.findMatchedTagIdsByNames(Array.isArray(blogData.tags) ? blogData.tags : (blogData.tags ? [blogData.tags] : [])),
        status: blogData.status || 'draft',
        summary: normalizeDisplayText(blogData.summary, ['summary', 'abstract', 'digest']),
        price: blogData.price !== undefined ? blogData.price : 0
      }
      this.currentRejectReason = this.resolveRejectReason(blogData)

      if (this.blog.price === 0) {
        this.selectedBlogType = 'free'
      } else if (this.blog.price === -1) {
        this.selectedBlogType = 'vip'
      } else if (this.blog.price > 0) {
        this.selectedBlogType = 'paid'
        this.customPrice = this.blog.price
      }

      if (this.quill) {
        this.quill.root.innerHTML = this.blog.content || ''
      }
    },
    resolveRejectReason(source = {}) {
      return normalizeDisplayText(
        source.rejectReason || source.latestReportReason || source.auditReason || source.lastRejectReason || source.reason || source.reject_comment || '',
        ['rejectReason', 'auditReason', 'reason', 'message']
      ).trim()
    },
    normalizeDraftItem(item = {}) {
      const authorId = item.authorId || item.author?.id || item.author?.userId || item.userId
      return {
        ...item,
        id: item.id,
        title: item.title || '',
        content: item.content || '',
        status: item.status || 'draft',
        summary: normalizeDisplayText(item.summary, ['summary', 'abstract', 'digest']).trim(),
        rejectReason: this.resolveRejectReason(item),
        updatedAt: item.updatedAt || item.updateTime || item.auditTime || item.publishTime || item.createdAt || item.createTime || '',
        authorId
      }
    },
    async saveBlog(status) {
      if (!this.blog.title.trim()) {
        this.$message.warning('请填写博客标题')
        return false
      }
      const contentText = this.stripHtml(this.blog.content).trim()
      if (!contentText) {
        this.$message.warning('请填写博客内容')
        return false
      }
      const isPublish = status === 'pending' || status === 'published'
      if (isPublish) {
        this.publishing = true
      } else {
        this.savingDraft = true
      }
      try {
        let tagIds = []
        if (this.blog.tags && Array.isArray(this.blog.tags)) {
          if (this.blog.tags.length > 0 && typeof this.blog.tags[0] === 'object') {
            tagIds = this.blog.tags.map(tag => {
              const id = tag.id
              return typeof id === 'string' ? parseInt(id, 10) : id
            }).filter(id => typeof id === 'number' && !isNaN(id))
          } else if (this.blog.tags.length > 0) {
            tagIds = this.blog.tags.map(id => {
              return typeof id === 'string' ? parseInt(id, 10) : id
            }).filter(id => typeof id === 'number' && !isNaN(id))
          }
        }
        const requestData = {
          id: this.blog.id || null,
          title: this.blog.title,
          content: this.blog.content,
          status,
          tagIds,
          summary: normalizeDisplayText(this.blog.summary, ['summary', 'abstract', 'digest']).trim(),
          price: this.blog.price !== undefined ? this.blog.price : 0
        }
        const result = await saveBlogApi(requestData)
        if (result && result.id) {
          this.blog.id = result.id
          this.blog.status = result.status || status
          this.currentRejectReason = this.resolveRejectReason(result)
          if (Array.isArray(result.tagIds) && result.tagIds.length) {
            this.blog.tags = result.tagIds
          }
          if (status === 'draft') {
            const now = new Date()
            this.lastSaved = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
            this.$message.success('草稿保存成功')
          } else if (this.blog.status === 'published') {
            this.currentRejectReason = ''
            this.$message.success('自动审核通过，博客已发布')
          } else if (this.blog.status === 'pending') {
            this.$message.warning(this.currentRejectReason || '已提交成功，系统建议人工复核，请等待管理员处理')
          } else if (this.blog.status === 'rejected') {
            this.$message.error(this.currentRejectReason || '自动审核未通过，请修改内容后重新提交')
          } else {
            this.$message.success('博客状态已更新')
          }
          return true
        }
      } catch (error) {
        console.error('保存博客出错', error)
        if (error.response) {
          this.$message.error((isPublish ? '发布' : '保存草稿') + '失败：' + (error.response.data?.message || '未知错误'))
        } else if (error.request) {
          this.$message.error('网络错误，请检查连接')
        } else {
          this.$message.error('请求错误：' + error.message)
        }
        return false
      } finally {
        if (isPublish) {
          this.publishing = false
        } else {
          this.savingDraft = false
        }
      }
    },
    saveDraft() {
      this.saveBlog('draft')
    },
    async publishBlog() {
      await this.saveBlog('pending')
    },

    openDraftDrawer() {
      this.drawerVisible = true
      this.draftPage = 1
      this.fetchDrafts()
    },
    async fetchDrafts() {
      this.loadingDrafts = true
      try {
        if (!this.userId) {
          this.restoreUserIdentityFromCache()
        }
        if (!this.userId) {
          await this.fetchUserInfoFromApi()
        }
        if (!this.userId) {
          this.draftTotal = 0
          this.draftList = []
          return
        }
        const pageResult = await fetchMyBlogList({
          userId: this.userId,
          page: this.draftPage,
          pageSize: this.pageSize
        })
        if ((pageResult.list || []).length === 0 && (pageResult.total || 0) > 0 && this.draftPage > 1) {
          this.draftPage -= 1
          await this.fetchDrafts()
          return
        }
        this.draftTotal = pageResult.total || 0
        this.draftList = (pageResult.list || []).map(item => this.normalizeDraftItem(item))
      } catch (error) {
        console.error('获取草稿失败:', error)
        this.$message.error('网络错误，无法加载博客列表')
        this.draftTotal = 0
        this.draftList = []
      } finally {
        this.loadingDrafts = false
      }
    },
    editDraft(id) {
      const draft = this.draftList.find(item => item.id === id)
      if (draft) {
        this.applyBlogToEditor(draft)
        this.drawerVisible = false
        this.$message.success(draft.status === 'rejected' ? '已加载被拒绝博客，请修改后重新提交' : '已加载草稿内容')
      } else {
        this.loadDraftById(id)
      }
    },
    async loadDraftById(id) {
      try {
        const draftData = await fetchBlogDetailApi(id)
        if (draftData) {
          this.applyBlogToEditor(draftData)
          this.drawerVisible = false
          this.$message.success('已加载草稿内容')
        }
      } catch (error) {
        console.error('加载草稿失败:', error)
        this.$message.error('加载草稿失败：' + (error.message || '网络错误'))
      }
    },
    resetEditorState() {
      this.blog = {
        id: null,
        title: '',
        summary: '',
        content: '',
        tags: [],
        status: 'draft',
        price: 0
      }
      this.currentRejectReason = ''
      this.lastSaved = ''
      this.selectedBlogType = 'free'
      this.customPrice = 9.99
      if (this.quill) {
        this.quill.root.innerHTML = ''
      }
    },
    async deleteCurrentBlog() {
      if (!this.blog.id) return
      try {
        await this.$confirm('删除后无法恢复，是否继续？', '删除博客', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        this.deletingBlog = true
        await removeBlog(this.blog.id)
        this.$message.success('博客已删除')
        this.resetEditorState()
        if (this.drawerVisible) {
          await this.fetchDrafts()
        }
      } catch (error) {
        if (error === 'cancel' || error?.message === 'cancel') return
        console.error('删除博客失败:', error)
        this.$message.error(error?.response?.data?.message || '删除博客失败，请稍后重试')
      } finally {
        this.deletingBlog = false
      }
    },
    async removeBlogFromList(draft) {
      if (!draft || !draft.id) return
      try {
        await this.$confirm(`确认删除《${draft.title || '无标题'}》吗？`, '删除博客', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await removeBlog(draft.id)
        this.$message.success('博客已删除')
        if (String(this.blog.id || '') === String(draft.id)) {
          this.resetEditorState()
        }
        await this.fetchDrafts()
      } catch (error) {
        if (error === 'cancel' || error?.message === 'cancel') return
        console.error('删除博客失败:', error)
        this.$message.error(error?.response?.data?.message || '删除博客失败，请稍后重试')
      }
    },
    handleDrawerClose(done) {
      done()
    },

    initEditor() {
      if (!process.client) return
      const Quill = this.$quill
      if (!Quill) {
        console.error('Quill 未加载，请检查插件配置')
        return
      }
      this.quill = new Quill('#editor', {
        modules: {
          toolbar: '#toolbar',
          syntax: false
        },
        placeholder: '请输入博客内容...',
        theme: 'snow',
        readOnly: false
      })
      this.quill.on('text-change', () => {
        this.blog.content = this.quill.root.innerHTML
      })
      if (this.blog.content) {
        this.quill.root.innerHTML = this.blog.content
      }
      const toolbar = this.quill.getModule('toolbar')
      toolbar.addHandler('image', this.imageHandler)
    },
    imageHandler() {
      const input = document.createElement('input')
      input.setAttribute('type', 'file')
      input.setAttribute('accept', 'image/*')
      input.click()
      input.onchange = async () => {
        const file = input.files[0]
        const formData = new FormData()
        formData.append('image', file)
        try {
          const res = await uploadBlogImage(formData)
          const imageUrl = res.url || res.fileUrl || res.path || ''
          if (!imageUrl) {
            this.$message.error('图片上传失败：未返回可用地址')
            return
          }
          const range = this.quill.getSelection()
          const insertIndex = range && typeof range.index === 'number' ? range.index : this.quill.getLength()
          this.quill.insertEmbed(insertIndex, 'image', imageUrl)
        } catch (error) {
          console.error('图片上传失败:', error)
          this.$message.error('图片上传失败')
        }
      }
    },
    stripHtml(html) {
      if (!html) return ''
      if (process.client) {
        const tmp = document.createElement('div')
        tmp.innerHTML = html
        return tmp.textContent || tmp.innerText || ''
      }
      return html.replace(/<[^>]*>/g, '')
    },
    findMatchedTagIdsByNames(tagNames) {
      if (!Array.isArray(tagNames) || tagNames.length === 0) return []
      const normalizedNames = tagNames
        .map(item => String(item || '').trim().toLowerCase())
        .filter(Boolean)
      return this.tagOptions
        .filter(option => {
          const optionName = String(option.name || '').trim().toLowerCase()
          return normalizedNames.some(tagName => {
            return optionName === tagName || optionName.includes(tagName) || tagName.includes(optionName)
          })
        })
        .map(option => option.id)
    },
    async handleAiPolish() {
      if (!this.blog.title.trim()) {
        this.$message.warning('璇峰厛濉啓鍗氬鏍囬')
        return
      }
      const contentText = this.stripHtml(this.blog.content || '').trim()
      if (!contentText) {
        this.$message.warning('璇峰厛濉啓鍗氬鍐呭')
        return
      }
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('璇峰厛鐧诲綍鍚庡啀浣跨敤 AI 鍔熻兘')
        return
      }
      if (userId !== null && userId !== undefined && String(userId).trim() !== '') {
        this.userId = userId
      }
      this.blog.content = this.quill && this.quill.root ? this.quill.root.innerHTML : this.blog.content
      this.openAiAssistantWithAction('blog.polish')
    },
    async handleAiGenerateSummary() {
      if (!this.blog.title.trim()) {
        this.$message.warning('璇峰厛濉啓鍗氬鏍囬')
        return
      }
      const contentText = this.stripHtml(this.blog.content || '').trim()
      if (!contentText) {
        this.$message.warning('璇峰厛濉啓鍗氬鍐呭')
        return
      }
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('璇峰厛鐧诲綍鍚庡啀浣跨敤 AI 鍔熻兘')
        return
      }
      if (userId !== null && userId !== undefined && String(userId).trim() !== '') {
        this.userId = userId
      }
      this.blog.content = this.quill && this.quill.root ? this.quill.root.innerHTML : this.blog.content
      this.openAiAssistantWithAction('blog.summary')
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    },
    getDraftStatusText(status) {
      return blogStatusText(status)
    },
    getDraftStatusTagType(status) {
      return blogStatusTagType(status)
    },
    getDraftStatusHint(status) {
      const normalized = String(status || '').toLowerCase()
      if (normalized === 'draft') return '草稿可继续编辑或提交审核。'
      if (normalized === 'pending') return '待审核博客可继续修改并重新提交。'
      if (normalized === 'published') return '已发布博客修改后需重新提交审核。'
      if (normalized === 'rejected') return '请根据拒绝原因修改后重新提交。'
      return '可继续编辑该博客。'
    },
    buildDraftPreview(draft) {
      const summary = normalizeDisplayText(draft.summary, ['summary', 'abstract', 'digest']).trim()
      if (summary) {
        return escapeHtmlValue(summary)
      }
      return renderRichContent(draft.content, { emptyText: '暂无内容' })
    }
  }
}
</script>

<style scoped>
.ai-model-select {
  width: 220px;
}

.write-blog-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.write-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.reject-banner {
  margin-bottom: 18px;
  border-radius: 14px;
}

.reject-banner-text {
  margin-top: 4px;
  line-height: 1.8;
}

.status-banner {
  margin-bottom: 18px;
  border-radius: 14px;
}

.status-banner-text {
  margin-top: 4px;
  line-height: 1.7;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.user-info:hover {
  opacity: 0.8;
}
.username {
  font-size: 16px;
  font-weight: 500;
  color: var(--it-text);
  background: linear-gradient(135deg, #1e293b, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}
.save-tip {
  font-size: 14px;
  color: var(--it-text-muted);
  background: #f1f5f9;
  padding: 5px 10px;
  border-radius: 20px;
}

.status-tag-pill {
  border-radius: 999px;
  padding: 0 12px;
  font-weight: 600;
}
.action-buttons .el-button {
  border-radius: 30px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}
.action-buttons .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
}
.action-buttons .el-button.el-button--warning {
  background: #f59e0b;
  border-color: #f59e0b;
  color: white;
}
.action-buttons .el-button.el-button--info {
  background: var(--it-text-subtle);
  border-color: var(--it-text-subtle);
  color: white;
}
.action-buttons .el-button.el-button--primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}
.action-buttons .el-button.el-button--success {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
  color: white;
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.18);
}

.edit-area {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.title-input >>> .el-input__inner {
  font-size: 28px;
  height: 60px;
  line-height: 60px;
  font-weight: 600;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 0 20px;
  color: var(--it-text);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.title-input >>> .el-input__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.title-input >>> .el-input__count {
  background: transparent;
  color: var(--it-text-subtle);
}

.tag-selector {
  display: flex;
  align-items: center;
  gap: 15px;
  background: white;
  padding: 10px 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.tag-label {
  font-size: 14px;
  color: var(--it-text-muted);
  font-weight: 500;
  white-space: nowrap;
}
.tag-select {
  flex: 1;
  max-width: 500px;
}
.tag-select >>> .el-input__inner {
  background: var(--it-surface-muted);
  border: 1px solid #e2e8f0;
  border-radius: 30px;
  height: 40px;
  color: var(--it-text);
}
.tag-select >>> .el-select__tags {
  margin-left: 10px;
}
.tag-select >>> .el-tag {
  background: #e2e8f0;
  border-color: #cbd5e1;
  color: var(--it-text);
}

.blog-type-option {
  background-color: var(--it-surface-muted);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin: 20px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.type-label {
  font-weight: 500;
  color: #4b5563;
  display: flex;
  align-items: center;
  gap: 6px;
}
.price-input {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-left: 10px;
}
.price-unit {
  font-size: 14px;
  color: #4b5563;
}
.type-tip {
  font-size: 12px;
  color: var(--it-text-muted);
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-basis: 100%;
  margin-top: 10px;
}
.blog-type-option :deep(.el-radio) {
  margin-right: 20px;
}

.ai-helper-panel {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
}
.ai-helper-main {
  flex: 1;
}
.ai-helper-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--it-text);
  margin-bottom: 6px;
}
.ai-helper-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--it-text-muted);
}
.ai-helper-side {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-helper-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.ai-helper-meta-item {
  min-width: 120px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(59, 130, 246, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.12);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ai-helper-meta-label {
  font-size: 12px;
  color: var(--it-text-muted);
}
.ai-helper-meta-item strong {
  font-size: 13px;
  color: var(--it-text);
  line-height: 1.6;
}
.ai-model-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ai-model-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--it-text);
}
.ai-model-select {
  width: 100%;
}
.ai-model-tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-result-panel {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 18px 20px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
.ai-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  gap: 12px;
}
.ai-result-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ai-result-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--it-text);
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-result-tabs {
  margin-top: 8px;
}
.ai-summary-showcase,
.ai-polish-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ai-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.ai-preview-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--it-text);
}
.ai-summary-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ai-summary-preview {
  min-height: 88px;
}
.ai-tag-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-tag-suggest {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.ai-tag-label {
  font-size: 13px;
  color: var(--it-text-muted);
  font-weight: 600;
}
.ai-tag-item {
  margin-right: 0;
}
.ai-tag-item-ghost {
  color: var(--it-accent);
  border-color: rgba(37, 99, 235, 0.24);
  background: rgba(59, 130, 246, 0.06);
}
.ai-rich-content {
  color: var(--it-text);
  line-height: 1.9;
  background: var(--it-surface-muted);
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 18px;
  overflow-x: auto;
}
.ai-rich-content h1,
.ai-rich-content h2,
.ai-rich-content h3,
.ai-rich-content h4,
.ai-rich-content h5,
.ai-rich-content h6 {
  margin: 0 0 14px;
  color: var(--it-text);
  line-height: 1.5;
}
.ai-rich-content p {
  margin: 0 0 12px;
}
.ai-rich-content p:last-child {
  margin-bottom: 0;
}
.ai-rich-content ul,
.ai-rich-content ol {
  margin: 0 0 12px 20px;
  padding: 0;
}
.ai-rich-content li + li {
  margin-top: 6px;
}
.ai-rich-content hr {
  border: 0;
  border-top: 1px solid #cbd5e1;
  margin: 14px 0;
}
.ai-rich-content blockquote {
  margin: 0 0 12px;
  padding: 10px 14px;
  border-left: 4px solid #60a5fa;
  background: rgba(59, 130, 246, 0.06);
  color: #1e3a8a;
  border-radius: 8px;
}
.ai-rich-content code {
  background: rgba(15, 23, 42, 0.06);
  border-radius: 6px;
  padding: 2px 6px;
  font-size: 13px;
}
.ai-rich-content pre {
  margin: 0 0 12px;
  padding: 14px 16px;
  background: var(--it-text);
  color: #e2e8f0;
  border-radius: 12px;
  overflow-x: auto;
}
.ai-rich-content pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}
.markdown-table-wrap {
  overflow-x: auto;
  margin-bottom: 12px;
}
.ai-rich-content table {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
  background: var(--it-surface-solid);
}
.ai-rich-content th,
.ai-rich-content td {
  border: 1px solid #dbe4f0;
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
}
.ai-rich-content th {
  background: #eff6ff;
  color: var(--it-text);
  font-weight: 600;
}
.ai-polish-preview >>> p,
.ai-polish-preview >>> h1,
.ai-polish-preview >>> h2,
.ai-polish-preview >>> h3,
.ai-polish-preview >>> h4,
.ai-polish-preview >>> h5,
.ai-polish-preview >>> h6 {
  margin-top: 0;
}
.empty-ai-content {
  color: var(--it-text-subtle);
}
.ai-result-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}
.ai-struct-block {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}
.ai-struct-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--it-text);
  margin-bottom: 8px;
}
.ai-struct-list {
  margin: 0;
  padding-left: 18px;
  color: var(--it-text-muted);
  line-height: 1.7;
}
.ai-struct-list-warn {
  color: #b45309;
}
.ai-title-suggest-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-block {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  background: white;
  padding: 16px 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.summary-label {
  font-size: 14px;
  color: var(--it-text-muted);
  font-weight: 500;
  line-height: 32px;
  white-space: nowrap;
}

.editor-container {
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}
#toolbar {
  border: none;
  border-bottom: 1px solid #e2e8f0;
  background: var(--it-surface-solid);
  padding: 10px;
}
.ql-editor-container {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  background: white;
  color: var(--it-text);
}
.ql-editor {
  min-height: 400px;
  font-size: 16px;
  line-height: 1.8;
  font-family: 'Inter', 'Microsoft YaHei', sans-serif;
  color: var(--it-text);
}
.editor-placeholder {
  padding: 30px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  min-height: 400px;
}

.draft-list {
  padding: 15px;
}
.draft-item {
  margin-bottom: 15px;
  cursor: pointer;
  border-radius: 16px !important;
  border: 1px solid #f1f5f9;
  transition: transform 0.2s, box-shadow 0.2s;
}
.draft-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
  border-color: #3b82f6;
}
.draft-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.draft-head-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.draft-delete-btn {
  color: #ef4444;
}

.draft-status-hint {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--it-text-muted);
}
.draft-item h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--it-text);
}
.draft-reject-reason {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.7;
}
.draft-summary {
  margin: 12px 0 8px;
  font-size: 13px;
  color: var(--it-text-muted);
}
.rich-preview {
  max-height: 120px;
  overflow: hidden;
  border: 1px solid #eef2f7;
  background: #fafcff;
  border-radius: 12px;
  padding: 12px;
  line-height: 1.8;
}
.rich-preview :deep(p) {
  margin: 0 0 8px 0;
}
.rich-preview :deep(p:last-child) {
  margin-bottom: 0;
}
.rich-preview :deep(h1),
.rich-preview :deep(h2),
.rich-preview :deep(h3),
.rich-preview :deep(h4) {
  margin: 0 0 8px 0;
  font-size: 14px;
}
.rich-preview :deep(ul),
.rich-preview :deep(ol) {
  margin: 0 0 8px 18px;
}
.draft-meta {
  font-size: 12px;
  color: var(--it-text-subtle);
}
.draft-pagination {
  margin-top: 20px;
  text-align: center;
}
.draft-pagination >>> .el-pager li {
  border-radius: 30px;
  font-weight: 500;
}
.draft-pagination >>> .el-pager li.active {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
}
.empty-draft {
  text-align: center;
  padding: 60px 20px;
  color: var(--it-text-subtle);
  font-size: 14px;
  background: var(--it-surface-muted);
  border-radius: 16px;
  margin: 20px;
}

@media screen and (max-width: 768px) {
  .write-header {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  .action-buttons {
    justify-content: flex-end;
    flex-wrap: wrap;
  }
  .tag-selector {
    flex-wrap: wrap;
  }
  .tag-label {
    width: 100%;
  }
  .tag-select {
    max-width: 100%;
  }
  .blog-type-option {
    flex-wrap: wrap;
    gap: 10px;
  }
  .type-tip {
    margin-left: 0;
    width: 100%;
  }
  .summary-block {
    flex-direction: column;
  }
  .ai-helper-panel {
    flex-direction: column;
  }
  .ai-helper-side {
    width: 100%;
  }
  .ai-preview-head {
    flex-wrap: wrap;
  }
}


/* theme polish overrides */
.write-blog-container {
  background:
    radial-gradient(circle at top right, var(--it-glow-primary) 0%, transparent 26%),
    radial-gradient(circle at bottom left, var(--it-glow-secondary) 0%, transparent 32%),
    var(--it-page-bg);
}
.write-header { border-bottom-color: var(--it-border); }
.username {
  background: var(--it-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.save-tip,
.blog-type-option,
.summary-block,
.tag-selector,
.ai-result-panel,
.editor-container,
.editor-placeholder,
.ai-helper-panel,
.draft-item,
.rich-preview {
  background: var(--it-panel-bg-strong);
  border-color: var(--it-border);
  box-shadow: var(--it-shadow-soft);
}
.title-input >>> .el-input__inner,
.tag-select >>> .el-input__inner,
.ai-rich-content,
#toolbar,
.ql-editor-container,
.editor-placeholder {
  background: var(--it-surface-solid);
  border-color: var(--it-border);
}
.title-input >>> .el-input__inner:focus {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
}
.tag-select >>> .el-tag {
  background: var(--it-tag-bg);
  border-color: var(--it-tag-border);
  color: var(--it-tag-text);
}
.action-buttons .el-button.el-button--warning,
.action-buttons .el-button.el-button--primary,
.action-buttons .el-button.el-button--success {
  background: var(--it-primary-gradient);
  border-color: transparent;
  box-shadow: var(--it-button-shadow);
}
.action-buttons .el-button.el-button--info {
  background: var(--it-surface-muted);
  border-color: var(--it-border);
  color: var(--it-text);
}
.blog-type-option,
.ai-helper-meta-item,
.ai-struct-block,
.ai-rich-content table,
.rich-preview,
.summary-block,
.save-tip {
  background: var(--it-soft-gradient);
}
.ai-helper-meta-item,
.ai-tag-item-ghost,
.ai-rich-content blockquote,
.ai-rich-content th {
  background: var(--it-accent-soft);
  border-color: var(--it-tone-info-border);
  color: var(--it-accent);
}
.ai-rich-content hr,
.ai-rich-content th,
.ai-rich-content td,
.ai-rich-content blockquote,
.ai-rich-content code,
.ai-struct-block,
.rich-preview,
.draft-item,
.draft-reject-reason { border-color: var(--it-border); }
.ai-rich-content code { background: var(--it-fill-soft); }
.ai-rich-content pre { background: var(--it-text); color: var(--it-text-light); }
.ai-rich-content pre code { background: transparent; }
.draft-item:hover {
  border-color: var(--it-accent);
  box-shadow: var(--it-shadow-hover);
}
.draft-delete-btn { color: var(--it-danger); }
.draft-reject-reason {
  background: var(--it-danger-soft);
  color: var(--it-tone-danger-text);
}

</style>
<style scoped>
.write-blog-container {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.12), transparent 20%),
    linear-gradient(180deg, #06101b 0%, #0a1626 44%, #07111d 100%);
}

.write-blog-container,
.write-blog-container :deep(.el-input__count),
.write-blog-container :deep(.el-empty__description p) {
  color: #cbd5e1;
}

.write-header,
.tag-selector,
.summary-block,
.ai-helper-panel,
.ai-result-panel,
.blog-type-option,
.editor-container,
.editor-placeholder,
.draft-item,
.empty-draft,
.reject-banner {
  background: rgba(8, 15, 29, 0.74) !important;
  border-color: rgba(148, 163, 184, 0.16) !important;
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.32);
  backdrop-filter: blur(22px);
}

.username,
.ai-helper-title,
.ai-result-title,
.ai-preview-title,
.summary-label,
.type-label,
.draft-item h4 {
  color: #f8fafc !important;
}

.save-tip,
.tag-label,
.ai-helper-desc,
.ai-helper-meta-label,
.type-tip,
.draft-summary,
.draft-meta,
.empty-ai-content,
.ai-struct-list,
.ai-rich-content {
  color: #cbd5e1 !important;
}

.ai-helper-meta-item,
.ai-struct-block,
.rich-preview {
  background: rgba(15, 23, 42, 0.68) !important;
  border-color: rgba(148, 163, 184, 0.14) !important;
}

.title-input :deep(.el-input__inner),
.summary-block :deep(.el-textarea__inner),
.tag-select :deep(.el-input__inner),
.ai-model-select :deep(.el-input__inner),
.write-blog-container :deep(.el-input-number__decrease),
.write-blog-container :deep(.el-input-number__increase) {
  background: rgba(2, 6, 23, 0.62) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  color: #e2e8f0 !important;
}

.write-blog-container :deep(.el-input__inner:focus),
.write-blog-container :deep(.el-textarea__inner:focus) {
  border-color: rgba(125, 211, 252, 0.45) !important;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.16) !important;
}

.write-blog-container :deep(.el-select .el-tag),
.ai-tag-item,
.ai-tag-item-ghost {
  background: rgba(14, 165, 233, 0.14) !important;
  border-color: rgba(125, 211, 252, 0.2) !important;
  color: #7dd3fc !important;
}

.action-buttons :deep(.el-button),
.ai-result-actions :deep(.el-button),
.draft-pagination :deep(.el-pager li.active) {
  border-radius: 999px !important;
}

.action-buttons :deep(.el-button--primary),
.ai-result-actions :deep(.el-button--primary),
.ai-result-actions :deep(.el-button--success) {
  background: linear-gradient(135deg, #0ea5e9, #2563eb) !important;
  border-color: transparent !important;
  color: #eff6ff !important;
}

.action-buttons :deep(.el-button--warning),
.action-buttons :deep(.el-button--info),
.action-buttons :deep(.el-button--success) {
  background: rgba(15, 23, 42, 0.82) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  color: #e2e8f0 !important;
}

#toolbar {
  background: rgba(8, 15, 29, 0.92) !important;
  border-bottom-color: rgba(148, 163, 184, 0.14) !important;
}

.write-blog-container :deep(.ql-toolbar .ql-stroke) {
  stroke: #cbd5e1;
}

.write-blog-container :deep(.ql-toolbar .ql-fill) {
  fill: #cbd5e1;
}

.write-blog-container :deep(.ql-picker),
.write-blog-container :deep(.ql-picker-label),
.write-blog-container :deep(.ql-picker-item) {
  color: #cbd5e1;
}

.ql-editor-container,
.ql-editor {
  background: rgba(2, 6, 23, 0.72) !important;
  color: #e2e8f0 !important;
}

.write-blog-container :deep(.ql-editor.ql-blank::before) {
  color: rgba(148, 163, 184, 0.72);
}

.ai-rich-content table {
  background: rgba(8, 15, 29, 0.86) !important;
}

.ai-rich-content th,
.ai-rich-content td {
  border-color: rgba(148, 163, 184, 0.12) !important;
  color: #cbd5e1 !important;
}

.ai-rich-content th {
  background: rgba(15, 23, 42, 0.88) !important;
}

.draft-item:hover {
  border-color: rgba(125, 211, 252, 0.28) !important;
  box-shadow: 0 20px 40px rgba(2, 6, 23, 0.38) !important;
}

:deep(.el-drawer) {
  background: linear-gradient(180deg, rgba(8, 15, 29, 0.98), rgba(12, 23, 39, 0.96));
  color: #cbd5e1;
}

:deep(.el-drawer__header) {
  color: var(--it-surface-muted);
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  margin-bottom: 0;
}


.write-blog-container {
  width: min(1320px, calc(100vw - 72px));
  max-width: 1320px;
  margin: 20px auto 72px;
  padding: 12px 20px 96px;
  background: transparent !important;
}

.write-header {
  position: sticky;
  top: 16px;
  z-index: 30;
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(8, 15, 29, 0.86) !important;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 24px 50px rgba(2, 6, 23, 0.28);
  backdrop-filter: blur(22px);
}

.edit-area {
  gap: 20px;
}

.action-buttons {
  justify-content: flex-end;
  row-gap: 10px;
}

.action-buttons .el-button {
  min-height: 38px;
}

.title-input :deep(.el-input__inner) {
  height: 68px;
  line-height: 68px;
  font-size: 34px;
  border-radius: 18px;
}

.tag-selector,
.summary-block,
.ai-helper-panel,
.ai-result-panel,
.blog-type-option,
.editor-container {
  border-radius: 22px !important;
}

.summary-block,
.blog-type-option {
  padding: 18px 20px;
}

.summary-block :deep(.el-textarea__inner) {
  min-height: 118px;
  border-radius: 16px;
}

.ai-helper-panel {
  padding: 22px 24px;
}

.ai-helper-main {
  padding-right: 8px;
}

.ai-helper-side {
  width: 320px;
}

.editor-container {
  min-height: 680px;
}

#toolbar {
  padding: 14px 16px;
}

.ql-editor-container {
  min-height: 560px;
  max-height: none;
}

.ql-editor {
  min-height: 560px;
  padding: 24px 26px 80px;
  font-size: 16px;
  line-height: 1.95;
}

.editor-placeholder {
  min-height: 560px;
}

@media (max-width: 1200px) {
  .write-blog-container {
    width: min(100%, calc(100vw - 28px));
    padding-left: 8px;
    padding-right: 8px;
  }
}

@media (max-width: 960px) {
  .write-header {
    position: static;
  }

  .action-buttons {
    justify-content: flex-start;
  }

  .title-input :deep(.el-input__inner) {
    font-size: 26px;
    height: 60px;
    line-height: 60px;
  }

  .ai-helper-panel {
    flex-direction: column;
  }

  .ai-helper-side {
    width: 100%;
  }

  .editor-container {
    min-height: 560px;
  }

  .ql-editor-container,
  .ql-editor,
  .editor-placeholder {
    min-height: 460px;
  }
}

html[data-mode='light'] .write-blog-container {
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.10), transparent 24%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08), transparent 20%),
    transparent !important;
}

html[data-mode='light'] .write-header,
html[data-mode='light'] .tag-selector,
html[data-mode='light'] .summary-block,
html[data-mode='light'] .ai-helper-panel,
html[data-mode='light'] .ai-result-panel,
html[data-mode='light'] .blog-type-option,
html[data-mode='light'] .editor-container,
html[data-mode='light'] .editor-placeholder,
html[data-mode='light'] .reject-banner {
  background: rgba(255, 255, 255, 0.94) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

html[data-mode='light'] .write-header {
  backdrop-filter: blur(18px);
}

html[data-mode='dark'] .write-blog-container {
  background: transparent !important;
}


/* theme polish overrides */
.write-blog-container {
  background:
    radial-gradient(circle at top right, var(--it-glow-primary) 0%, transparent 26%),
    radial-gradient(circle at bottom left, var(--it-glow-secondary) 0%, transparent 32%),
    var(--it-page-bg);
}
.write-header { border-bottom-color: var(--it-border); }
.username {
  background: var(--it-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.save-tip,
.blog-type-option,
.summary-block,
.tag-selector,
.ai-result-panel,
.editor-container,
.editor-placeholder,
.ai-helper-panel,
.draft-item,
.rich-preview {
  background: var(--it-panel-bg-strong);
  border-color: var(--it-border);
  box-shadow: var(--it-shadow-soft);
}
.title-input >>> .el-input__inner,
.tag-select >>> .el-input__inner,
.ai-rich-content,
#toolbar,
.ql-editor-container,
.editor-placeholder {
  background: var(--it-surface-solid);
  border-color: var(--it-border);
}
.title-input >>> .el-input__inner:focus {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
}
.tag-select >>> .el-tag {
  background: var(--it-tag-bg);
  border-color: var(--it-tag-border);
  color: var(--it-tag-text);
}
.action-buttons .el-button.el-button--warning,
.action-buttons .el-button.el-button--primary,
.action-buttons .el-button.el-button--success {
  background: var(--it-primary-gradient);
  border-color: transparent;
  box-shadow: var(--it-button-shadow);
}
.action-buttons .el-button.el-button--info {
  background: var(--it-surface-muted);
  border-color: var(--it-border);
  color: var(--it-text);
}
.blog-type-option,
.ai-helper-meta-item,
.ai-struct-block,
.ai-rich-content table,
.rich-preview,
.summary-block,
.save-tip {
  background: var(--it-soft-gradient);
}
.ai-helper-meta-item,
.ai-tag-item-ghost,
.ai-rich-content blockquote,
.ai-rich-content th {
  background: var(--it-accent-soft);
  border-color: var(--it-tone-info-border);
  color: var(--it-accent);
}
.ai-rich-content hr,
.ai-rich-content th,
.ai-rich-content td,
.ai-rich-content blockquote,
.ai-rich-content code,
.ai-struct-block,
.rich-preview,
.draft-item,
.draft-reject-reason { border-color: var(--it-border); }
.ai-rich-content code { background: var(--it-fill-soft); }
.ai-rich-content pre { background: var(--it-text); color: var(--it-text-light); }
.ai-rich-content pre code { background: transparent; }
.draft-item:hover {
  border-color: var(--it-accent);
  box-shadow: var(--it-shadow-hover);
}
.draft-delete-btn { color: var(--it-danger); }
.draft-reject-reason {
  background: var(--it-danger-soft);
  color: var(--it-tone-danger-text);
}

</style>
<style scoped>
.write-blog-container {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
  width: min(var(--it-shell-max), calc(100vw - 24px));
  margin: 16px auto 56px;
  padding: 10px var(--it-shell-padding-x) 56px;
}

.write-header,
.tag-selector,
.summary-block,
.ai-helper-panel,
.ai-result-panel,
.blog-type-option,
.editor-container,
.editor-placeholder,
.draft-item,
.empty-draft,
.reject-banner {
  background: var(--it-surface) !important;
  border: 1px solid var(--it-border) !important;
  box-shadow: var(--it-shadow) !important;
  backdrop-filter: blur(0);
}

.write-header,
.tag-selector,
.summary-block,
.ai-helper-panel,
.ai-result-panel,
.blog-type-option,
.editor-container,
.editor-placeholder {
  border-radius: 14px !important;
}

.username,
.ai-helper-title,
.ai-result-title,
.ai-preview-title,
.summary-label,
.type-label,
.draft-item h4 {
  color: var(--it-text) !important;
}

.save-tip,
.tag-label,
.ai-helper-desc,
.ai-helper-meta-label,
.type-tip,
.draft-summary,
.draft-meta,
.empty-ai-content,
.ai-struct-list,
.ai-rich-content,
.ai-tag-label {
  color: var(--it-text-muted) !important;
}

.title-input :deep(.el-input__inner),
.summary-block :deep(.el-textarea__inner),
.tag-select :deep(.el-input__inner),
.ai-model-select :deep(.el-input__inner),
.write-blog-container :deep(.el-input-number__decrease),
.write-blog-container :deep(.el-input-number__increase) {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.write-blog-container :deep(.el-input__inner:focus),
.write-blog-container :deep(.el-textarea__inner:focus) {
  border-color: var(--it-accent) !important;
  box-shadow: 0 0 0 3px var(--it-accent-soft) !important;
}

.write-blog-container :deep(.el-select .el-tag),
.ai-tag-item,
.ai-tag-item-ghost {
  background: var(--it-accent-soft) !important;
  border-color: var(--it-border) !important;
  color: var(--it-accent) !important;
}

.action-buttons :deep(.el-button),
.ai-result-actions :deep(.el-button),
.draft-pagination :deep(.el-pager li.active) {
  border-radius: 8px !important;
}

.action-buttons :deep(.el-button--primary),
.ai-result-actions :deep(.el-button--primary),
.ai-result-actions :deep(.el-button--success) {
  background: var(--it-primary-gradient) !important;
  border-color: transparent !important;
  color: #fff !important;
}

.action-buttons :deep(.el-button--warning),
.action-buttons :deep(.el-button--info),
.action-buttons :deep(.el-button--success) {
  background: var(--it-surface-solid) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.ai-helper-meta-item,
.ai-struct-block,
.rich-preview,
.ai-rich-content {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
}

#toolbar {
  background: var(--it-surface-solid) !important;
  border-bottom: 1px solid var(--it-border) !important;
}

.write-blog-container :deep(.ql-toolbar .ql-stroke) {
  stroke: var(--it-text-muted);
}

.write-blog-container :deep(.ql-toolbar .ql-fill) {
  fill: var(--it-text-muted);
}

.write-blog-container :deep(.ql-picker),
.write-blog-container :deep(.ql-picker-label),
.write-blog-container :deep(.ql-picker-item) {
  color: var(--it-text-muted);
}

.ql-editor-container,
.ql-editor {
  background: var(--it-surface-solid) !important;
  color: var(--it-text) !important;
}

.write-blog-container :deep(.ql-editor.ql-blank::before) {
  color: var(--it-text-subtle);
}

.ai-rich-content table,
.ai-rich-content th,
.ai-rich-content td {
  background: transparent !important;
  border-color: var(--it-border) !important;
  color: var(--it-text-muted) !important;
}

.ai-rich-content th {
  color: var(--it-text) !important;
  background: var(--it-surface-solid) !important;
}

@media (max-width: 960px) {
  .write-blog-container {
    width: min(100%, calc(100vw - 16px));
    padding: 8px 8px 36px;
  }

  .action-buttons {
    gap: 8px;
  }
}


/* theme polish overrides */
.write-blog-container {
  background:
    radial-gradient(circle at top right, var(--it-glow-primary) 0%, transparent 26%),
    radial-gradient(circle at bottom left, var(--it-glow-secondary) 0%, transparent 32%),
    var(--it-page-bg);
}
.write-header { border-bottom-color: var(--it-border); }
.username {
  background: var(--it-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.save-tip,
.blog-type-option,
.summary-block,
.tag-selector,
.ai-result-panel,
.editor-container,
.editor-placeholder,
.ai-helper-panel,
.draft-item,
.rich-preview {
  background: var(--it-panel-bg-strong);
  border-color: var(--it-border);
  box-shadow: var(--it-shadow-soft);
}
.title-input >>> .el-input__inner,
.tag-select >>> .el-input__inner,
.ai-rich-content,
#toolbar,
.ql-editor-container,
.editor-placeholder {
  background: var(--it-surface-solid);
  border-color: var(--it-border);
}
.title-input >>> .el-input__inner:focus {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
}
.tag-select >>> .el-tag {
  background: var(--it-tag-bg);
  border-color: var(--it-tag-border);
  color: var(--it-tag-text);
}
.action-buttons .el-button.el-button--warning,
.action-buttons .el-button.el-button--primary,
.action-buttons .el-button.el-button--success {
  background: var(--it-primary-gradient);
  border-color: transparent;
  box-shadow: var(--it-button-shadow);
}
.action-buttons .el-button.el-button--info {
  background: var(--it-surface-muted);
  border-color: var(--it-border);
  color: var(--it-text);
}
.blog-type-option,
.ai-helper-meta-item,
.ai-struct-block,
.ai-rich-content table,
.rich-preview,
.summary-block,
.save-tip {
  background: var(--it-soft-gradient);
}
.ai-helper-meta-item,
.ai-tag-item-ghost,
.ai-rich-content blockquote,
.ai-rich-content th {
  background: var(--it-accent-soft);
  border-color: var(--it-tone-info-border);
  color: var(--it-accent);
}
.ai-rich-content hr,
.ai-rich-content th,
.ai-rich-content td,
.ai-rich-content blockquote,
.ai-rich-content code,
.ai-struct-block,
.rich-preview,
.draft-item,
.draft-reject-reason { border-color: var(--it-border); }
.ai-rich-content code { background: var(--it-fill-soft); }
.ai-rich-content pre { background: var(--it-text); color: var(--it-text-light); }
.ai-rich-content pre code { background: transparent; }
.draft-item:hover {
  border-color: var(--it-accent);
  box-shadow: var(--it-shadow-hover);
}
.draft-delete-btn { color: var(--it-danger); }
.draft-reject-reason {
  background: var(--it-danger-soft);
  color: var(--it-tone-danger-text);
}

</style>

<style scoped>
/* blogwrite-round6-theme-polish */
.write-blog-container {
  background: var(--it-page-bg) !important;
}

.write-header {
  border-bottom: 1px solid var(--it-border) !important;
}

.username {
  background: var(--it-primary-gradient) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
}

.save-tip,
.tag-selector,
.blog-type-option,
.ai-helper-panel,
.ai-result-panel,
.summary-block,
.editor-container,
.editor-placeholder,
.draft-item {
  background: var(--it-panel-bg) !important;
  border: 1px solid var(--it-border) !important;
  box-shadow: var(--it-shadow-soft) !important;
}

.title-input >>> .el-input__inner,
.tag-select >>> .el-input__inner,
#toolbar,
.ql-editor-container,
.editor-placeholder {
  background: var(--it-panel-bg-strong) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.title-input >>> .el-input__inner:focus {
  border-color: var(--it-accent) !important;
  box-shadow: 0 0 0 3px var(--it-accent-soft) !important;
}

.tag-select >>> .el-tag,
.ai-tag-item-ghost {
  background: var(--it-tag-bg) !important;
  border-color: var(--it-tag-border) !important;
  color: var(--it-tag-text) !important;
}

.action-buttons .el-button.el-button--warning {
  background: linear-gradient(135deg, var(--it-warning), color-mix(in srgb, var(--it-warning) 68%, var(--it-accent))) !important;
  border-color: transparent !important;
  color: #fff !important;
}

.action-buttons .el-button.el-button--info {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-text-muted) 84%, var(--it-surface-solid)), color-mix(in srgb, var(--it-text-subtle) 76%, var(--it-surface-solid))) !important;
  border-color: transparent !important;
  color: #fff !important;
}

.action-buttons .el-button.el-button--primary {
  background: var(--it-primary-gradient) !important;
  box-shadow: var(--it-button-shadow) !important;
}

.action-buttons .el-button.el-button--success {
  background: linear-gradient(135deg, var(--it-success), var(--it-accent)) !important;
  box-shadow: 0 10px 22px color-mix(in srgb, var(--it-success) 28%, transparent) !important;
}

.ai-helper-meta-item {
  background: color-mix(in srgb, var(--it-accent-soft) 76%, var(--it-surface-solid)) !important;
  border: 1px solid var(--it-border) !important;
}

.ai-rich-content {
  background: var(--it-panel-bg-strong) !important;
  border-color: var(--it-border) !important;
}

.ai-rich-content hr,
.ai-rich-content th,
.ai-rich-content td {
  border-color: var(--it-border) !important;
}

.ai-rich-content th {
  background: color-mix(in srgb, var(--it-accent-soft) 68%, var(--it-surface-solid)) !important;
}

.ai-rich-content blockquote {
  border-left-color: var(--it-accent) !important;
  background: var(--it-accent-soft) !important;
  color: var(--it-text) !important;
}

.ai-rich-content code {
  background: color-mix(in srgb, var(--it-text) 8%, var(--it-surface-solid)) !important;
}

.ai-rich-content pre {
  background: var(--it-showcase-bg) !important;
  color: var(--it-showcase-text) !important;
}

.ai-rich-content table {
  background: var(--it-panel-bg) !important;
}

.ai-struct-list-warn,
.draft-delete-btn {
  color: var(--it-tone-warning-text) !important;
}

.draft-item:hover {
  border-color: var(--it-border-strong) !important;
  box-shadow: var(--it-shadow-hover) !important;
}
</style>


<style scoped>
/* round11-final-theme-unify */
.write-blog-container {
  background:
    radial-gradient(circle at top left, var(--it-glow-primary) 0%, transparent 24%),
    radial-gradient(circle at top right, var(--it-glow-secondary) 0%, transparent 22%),
    var(--it-page-bg) !important;
  color: var(--it-text) !important;
}

.write-header,
.tag-selector,
.summary-block,
.ai-helper-panel,
.ai-result-panel,
.blog-type-option,
.editor-container,
.editor-placeholder,
.draft-item,
.empty-draft,
.reject-banner,
.status-banner,
.rich-preview {
  background: var(--it-panel-bg-strong) !important;
  border: 1px solid var(--it-border) !important;
  box-shadow: var(--it-shadow-soft) !important;
  backdrop-filter: blur(18px);
}

.write-header {
  background: color-mix(in srgb, var(--it-panel-bg-strong) 92%, transparent) !important;
}

.username,
.ai-helper-title,
.ai-result-title,
.ai-preview-title,
.summary-label,
.type-label,
.draft-item h4,
.ai-model-label,
.ai-struct-title,
.ai-rich-content h1,
.ai-rich-content h2,
.ai-rich-content h3,
.ai-rich-content h4,
.ai-rich-content h5,
.ai-rich-content h6 {
  color: var(--it-text) !important;
}

.save-tip,
.tag-label,
.ai-helper-desc,
.ai-helper-meta-label,
.type-tip,
.draft-summary,
.draft-meta,
.empty-ai-content,
.ai-struct-list,
.ai-rich-content,
.price-unit {
  color: var(--it-text-muted) !important;
}

.title-input :deep(.el-input__inner),
.summary-block :deep(.el-textarea__inner),
.tag-select :deep(.el-input__inner),
.ai-model-select :deep(.el-input__inner),
.write-blog-container :deep(.el-input-number__decrease),
.write-blog-container :deep(.el-input-number__increase),
#toolbar,
.ql-editor-container,
.ql-editor,
.editor-placeholder,
.ai-rich-content,
.ai-rich-content table,
.rich-preview {
  background: var(--it-surface-solid) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.write-blog-container :deep(.el-input__inner:focus),
.write-blog-container :deep(.el-textarea__inner:focus),
.title-input :deep(.el-input__inner:focus) {
  border-color: var(--it-accent) !important;
  box-shadow: 0 0 0 3px var(--it-accent-soft) !important;
}

.write-blog-container :deep(.el-select .el-tag),
.tag-select :deep(.el-tag),
.ai-tag-item,
.ai-tag-item-ghost,
.status-tag-pill,
.hub-context {
  background: var(--it-tag-bg) !important;
  border-color: var(--it-tag-border) !important;
  color: var(--it-tag-text) !important;
}

.ai-helper-meta-item,
.ai-struct-block,
.summary-block,
.blog-type-option,
.save-tip {
  background: var(--it-soft-gradient) !important;
  border-color: var(--it-border) !important;
}

.ai-rich-content blockquote {
  background: var(--it-quote-bg) !important;
  border-left-color: var(--it-quote-border) !important;
  color: var(--it-text) !important;
}

.ai-rich-content code {
  background: var(--it-inline-code-bg) !important;
  color: var(--it-inline-code-text) !important;
  border: 1px solid var(--it-border) !important;
}

.ai-rich-content pre,
.ql-editor-container,
.ql-editor {
  background: var(--it-showcase-bg) !important;
  color: var(--it-showcase-text) !important;
}

.ai-rich-content pre code {
  background: transparent !important;
  color: inherit !important;
  border: none !important;
}

.ai-rich-content hr,
.ai-rich-content th,
.ai-rich-content td,
.ai-rich-content blockquote,
.ai-struct-block,
.draft-item,
.draft-reject-reason,
.rich-preview,
.empty-draft {
  border-color: var(--it-border) !important;
}

.ai-rich-content th {
  background: var(--it-accent-soft) !important;
  color: var(--it-text) !important;
}

.ai-result-actions :deep(.el-button--primary),
.ai-result-actions :deep(.el-button--success),
.action-buttons :deep(.el-button--primary) {
  background: var(--it-primary-gradient) !important;
  border-color: transparent !important;
  color: var(--it-text-light) !important;
  box-shadow: var(--it-button-shadow) !important;
}

.action-buttons :deep(.el-button--warning) {
  background: linear-gradient(135deg, var(--it-warning), color-mix(in srgb, var(--it-warning) 62%, var(--it-accent))) !important;
  border-color: transparent !important;
  color: var(--it-text-light) !important;
  box-shadow: var(--it-button-shadow) !important;
}

.action-buttons :deep(.el-button--success) {
  background: linear-gradient(135deg, var(--it-success), color-mix(in srgb, var(--it-success) 62%, var(--it-accent))) !important;
  border-color: transparent !important;
  color: var(--it-text-light) !important;
  box-shadow: var(--it-button-shadow) !important;
}

.action-buttons :deep(.el-button--info),
.ai-result-actions :deep(.el-button:not(.el-button--primary):not(.el-button--success)) {
  background: var(--it-panel-bg) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.write-blog-container :deep(.ql-toolbar .ql-stroke) { stroke: var(--it-showcase-text) !important; }
.write-blog-container :deep(.ql-toolbar .ql-fill) { fill: var(--it-showcase-text) !important; }
.write-blog-container :deep(.ql-picker),
.write-blog-container :deep(.ql-picker-label),
.write-blog-container :deep(.ql-picker-item),
.write-blog-container :deep(.el-input__count),
.write-blog-container :deep(.el-empty__description p),
.write-blog-container :deep(.ql-editor.ql-blank::before) {
  color: var(--it-showcase-muted) !important;
}

.draft-item:hover {
  border-color: var(--it-border-strong) !important;
  box-shadow: var(--it-shadow-hover) !important;
}

.draft-delete-btn { color: var(--it-danger) !important; }
.draft-reject-reason {
  background: var(--it-danger-panel-bg) !important;
  color: var(--it-tone-danger-text) !important;
}

:deep(.el-drawer) {
  background: var(--it-contrast-panel-bg) !important;
  color: var(--it-contrast-text) !important;
}

:deep(.el-drawer__header) {
  color: var(--it-contrast-text) !important;
  border-bottom: 1px solid var(--it-contrast-panel-border) !important;
}

.draft-pagination :deep(.el-pager li.active) {
  background: var(--it-primary-gradient) !important;
  color: var(--it-text-light) !important;
}
</style>

