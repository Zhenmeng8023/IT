<template>
  <div class="write-blog-container">
    <div class="write-header">
      <div class="user-info" @click="goToUserHome">
        <el-avatar :size="50" :src="userAvatar"></el-avatar>
        <span class="username">{{ username }}</span>
      </div>

      <div class="action-buttons">
        <el-button type="warning" plain @click="openDraftDrawer" :loading="loadingDrafts">
          草稿箱
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

        <el-button type="info" plain @click="saveDraft" :loading="savingDraft">
          存草稿
        </el-button>

        <el-button type="primary" @click="publishBlog" :loading="publishing">
          {{ publishButtonText }}
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

    <div class="edit-area">
      <el-input
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

      <div v-if="isClient" class="editor-container">
        <div id="toolbar"></div>
        <div id="editor" class="ql-editor-container"></div>
      </div>
      <div v-else class="editor-placeholder">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <el-drawer
      title="我的草稿"
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
            <el-tag :type="getDraftStatusTagType(draft.status)" size="mini">
              {{ getDraftStatusText(draft.status) }}
            </el-tag>
          </div>

          <div v-if="draft.rejectReason" class="draft-reject-reason">
            驳回原因：{{ draft.rejectReason }}
          </div>

          <div class="draft-summary rich-preview" v-html="buildDraftPreview(draft)"></div>

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
          暂无草稿
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  GetCurrentUser,
  GetAllTags,
  GetBlogById,
  CreateBlog,
  UpdateBlog,
  GetBlogDrafts,
  GetRejectedBlogs,
  UploadFile
} from '@/api/index'
import {
  aiPolishBlog,
  aiGenerateBlogSummary,
  normalizeBlogPolishPayload,
  normalizeBlogSummaryPayload,
  matchSystemTagsByNames
} from '@/api/aiAssistant'
import { listEnabledAiModels, pageAiModels } from '@/api/aiAdmin'

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
  const tokenKeys = ['token', 'access_token', 'accessToken', 'Authorization', 'auth_token', 'user-token', 'x-token']
  for (const storage of [window.localStorage, window.sessionStorage]) {
    for (const key of tokenKeys) {
      try {
        const value = storage.getItem(key)
        if (value && String(value).trim()) return String(value).replace(/^Bearer\s+/i, '').trim()
      } catch (e) {}
    }
  }
  try {
    const match = document.cookie.match(/(?:^|; )token=([^;]+)/)
    if (match && match[1]) return decodeURIComponent(match[1])
  } catch (e) {}
  return ''
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
    publishButtonText() {
      return this.blog.status === 'rejected' ? '重新提交审核' : '发布'
    }
  },

  mounted() {
    this.isClient = true
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
    if (this.quill) {
      this.quill = null
    }
  },

  methods: {
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
    async handleAiPolish() {
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
    async handleAiGenerateSummary() {
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
              this.userAvatar = profile.avatarUrl || profile.avatar || this.userAvatar
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
        const response = await GetCurrentUser()
        let userData = null
        if (response.data && typeof response.data.code !== 'undefined') {
          if (response.data.code === 0 && response.data.data) {
            userData = response.data.data
          }
        } else if (response.data && response.data.id) {
          userData = response.data
        } else if (response && response.id) {
          userData = response
        }
        if (userData && userData.id) {
          this.userId = userData.id
          this.username = userData.nickname || userData.username || '当前用户'
          this.userAvatar = userData.avatarUrl || userData.avatar || this.userAvatar
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
        const res = await GetAllTags()
        if (Array.isArray(res)) {
          this.tagOptions = res
        } else if (Array.isArray(res.data)) {
          this.tagOptions = res.data
        } else if (res.data && typeof res.data === 'object' && res.data.code === 0) {
          this.tagOptions = res.data.data || []
        } else {
          this.tagOptions = []
        }
      } catch (error) {
        console.error('获取标签失败:', error)
        this.$message.error('获取标签失败：' + (error.message || '网络错误'))
      } finally {
        this.loadingTags = false
      }
    },

    async fetchBlog(blogId) {
      try {
        const res = await GetBlogById(blogId)
        let blogData = null
        if (res.data && typeof res.data.code !== 'undefined') {
          if (res.data.code === 0) {
            blogData = res.data.data
          } else {
            this.$message.error('获取博客失败：' + res.data.message)
            return
          }
        } else {
          blogData = res.data || res
        }
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
    parseArrayResponse(res) {
      const payload = extractApiData(res)
      if (Array.isArray(payload)) return payload
      if (Array.isArray(payload?.list)) return payload.list
      if (Array.isArray(payload?.content)) return payload.content
      if (Array.isArray(res?.data)) return res.data
      return []
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
          title: this.blog.title,
          content: this.blog.content,
          status,
          tagIds,
          summary: normalizeDisplayText(this.blog.summary, ['summary', 'abstract', 'digest']).trim(),
          price: this.blog.price !== undefined ? this.blog.price : 0
        }
        if (!this.blog.id && this.userId) {
          requestData.userId = this.userId
        }
        let res
        if (this.blog.id) {
          res = await UpdateBlog(this.blog.id, requestData)
        } else {
          res = await CreateBlog(requestData)
        }
        if (res && typeof res === 'object') {
          let result = res
          if (res.data && typeof res.data === 'object') {
            result = res.data
          }
          if (!this.blog.id) {
            this.blog.id = result.id || result._id
          }
          this.blog.status = result.status || status
          if (status === 'draft') {
            const now = new Date()
            this.lastSaved = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
            this.$message.success('草稿保存成功')
          } else {
            this.currentRejectReason = ''
            this.$message.success(this.blog.status === 'rejected' ? '重新提交失败，请检查状态' : '已提交审核，请等待管理员处理')
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
        const [draftRes, rejectedRes] = await Promise.all([
          GetBlogDrafts().catch(() => null),
          this.userId ? GetRejectedBlogs().catch(() => null) : Promise.resolve(null)
        ])

        const drafts = this.parseArrayResponse(draftRes).map(item => this.normalizeDraftItem(item))
        const rejectedAll = this.parseArrayResponse(rejectedRes).map(item => this.normalizeDraftItem(item))
        const rejectedMine = rejectedAll.filter(item => String(item.authorId || '') === String(this.userId || ''))

        const mergedMap = new Map()
        ;[...drafts, ...rejectedMine].forEach(item => {
          if (!item || !item.id) return
          mergedMap.set(item.id, item)
        })

        const merged = Array.from(mergedMap.values()).sort((a, b) => {
          const ta = new Date(a.updatedAt || 0).getTime()
          const tb = new Date(b.updatedAt || 0).getTime()
          return tb - ta
        })

        this.draftTotal = merged.length
        const start = (this.draftPage - 1) * this.pageSize
        const end = start + this.pageSize
        this.draftList = merged.slice(start, end)
      } catch (error) {
        console.error('获取草稿失败:', error)
        this.$message.error('网络错误，无法加载草稿')
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
        const res = await GetBlogById(id)
        let draftData = null
        if (res.data && typeof res.data.code !== 'undefined') {
          if (res.data.code === 0) {
            draftData = res.data.data
          }
        } else {
          draftData = res.data || res
        }
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
          const res = await UploadFile(formData)
          const range = this.quill.getSelection()
          this.quill.insertEmbed(range.index, 'image', res.data.url)
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
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    },
    getDraftStatusText(status) {
      const map = {
        draft: '草稿',
        rejected: '已拒绝',
        pending: '审核中',
        published: '已发布'
      }
      return map[status] || '草稿'
    },
    getDraftStatusTagType(status) {
      const map = {
        draft: 'info',
        rejected: 'danger',
        pending: 'warning',
        published: 'success'
      }
      return map[status] || 'info'
    },
    buildDraftPreview(draft) {
      const summary = normalizeDisplayText(draft.summary, ['summary', 'abstract', 'digest']).trim()
      if (summary) {
        return escapeHtmlValue(summary)
      }
      const html = String(draft.content || '').trim()
      if (!html) return '暂无内容'
      if (looksLikeHtml(html)) return html
      return renderMarkdownToHtml(html, '暂无内容')
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
  color: #1e293b;
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
  color: #64748b;
  background: #f1f5f9;
  padding: 5px 10px;
  border-radius: 20px;
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
  background: #94a3b8;
  border-color: #94a3b8;
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
  color: #1e293b;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.title-input >>> .el-input__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.title-input >>> .el-input__count {
  background: transparent;
  color: #94a3b8;
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
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
}
.tag-select {
  flex: 1;
  max-width: 500px;
}
.tag-select >>> .el-input__inner {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 30px;
  height: 40px;
  color: #1e293b;
}
.tag-select >>> .el-select__tags {
  margin-left: 10px;
}
.tag-select >>> .el-tag {
  background: #e2e8f0;
  border-color: #cbd5e1;
  color: #1e293b;
}

.blog-type-option {
  background-color: #f8fafc;
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
  color: #6b7280;
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
  color: #0f172a;
  margin-bottom: 6px;
}
.ai-helper-desc {
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
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
  color: #64748b;
}
.ai-helper-meta-item strong {
  font-size: 13px;
  color: #0f172a;
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
  color: #334155;
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
  color: #0f172a;
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
  color: #0f172a;
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
  color: #475569;
  font-weight: 600;
}
.ai-tag-item {
  margin-right: 0;
}
.ai-tag-item-ghost {
  color: #2563eb;
  border-color: rgba(37, 99, 235, 0.24);
  background: rgba(59, 130, 246, 0.06);
}
.ai-rich-content {
  color: #334155;
  line-height: 1.9;
  background: #f8fafc;
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
  color: #0f172a;
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
  background: #0f172a;
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
  background: #ffffff;
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
  color: #0f172a;
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
  color: #94a3b8;
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
  background: #f8fafc;
  border: 1px solid #e5edf6;
}
.ai-struct-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}
.ai-struct-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
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
  color: #475569;
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
  background: #ffffff;
  padding: 10px;
}
.ql-editor-container {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  background: white;
  color: #1e293b;
}
.ql-editor {
  min-height: 400px;
  font-size: 16px;
  line-height: 1.8;
  font-family: 'Inter', 'Microsoft YaHei', sans-serif;
  color: #1e293b;
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
.draft-item h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
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
  color: #64748b;
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
  color: #94a3b8;
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
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
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
</style>
