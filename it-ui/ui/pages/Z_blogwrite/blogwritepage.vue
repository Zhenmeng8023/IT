<template>
  <div class="write-blog-container">
    <!-- ========== 头部区域：用户信息和操作按钮 ========== -->
    <div class="write-header">
      <!-- 用户信息区域，点击跳转到用户主页 -->
      <div class="user-info" @click="goToUserHome">
        <el-avatar :size="50" :src="userAvatar"></el-avatar>
        <span class="username">{{ username }}</span>
      </div>
      
      <!-- 右侧操作按钮组 -->
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
          发布
        </el-button>
      </div>
    </div>

    <!-- ========== 博客编辑主体区域 ========== -->
    <div class="edit-area">
      <!-- 标题输入框 -->
      <el-input
        class="title-input"
        v-model="blog.title"
        placeholder="请输入博客标题"
        maxlength="100"
        show-word-limit
        clearable
      ></el-input>

      <!-- 标签选择器：多选标签 -->
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
          <!-- 动态渲染标签选项 -->
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
            已接入已启用模型列表；可切换模型进行正文润色、摘要生成与标签匹配。
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
      </div>      <!-- ========== AI 结果面板 ========== -->
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
            <div v-if="aiPolishResult" class="ai-rich-content ai-polish-preview" v-html="renderedAiPolishResult"></div>
            <el-empty v-else description="还没有生成润色结果" :image-size="72" />
            <div v-if="aiPolishResult" class="ai-result-actions">
              <el-button size="mini" type="success" plain @click="handleApplyAiPolish(aiPolishResult)">应用到正文</el-button>
              <el-button size="mini" type="text" icon="el-icon-document-copy" @click="copyText(aiPolishResult)">复制润色结果</el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="摘要 / 标签" name="summary">
            <div v-if="aiSummaryResult" class="ai-rich-content" v-html="renderedAiSummaryResult"></div>
            <el-empty v-else description="还没有生成摘要结果" :image-size="72" />

            <div v-if="aiSuggestedTags.length > 0" class="ai-tag-suggest">
              <span class="ai-tag-label">已匹配标签：</span>
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

            <div v-if="aiSummaryResult" class="ai-result-actions">
              <el-button size="mini" type="primary" plain @click="applyAiSummaryToForm">应用摘要到表单</el-button>
              <el-button size="mini" type="text" icon="el-icon-document-copy" @click="copyText(aiSummaryResult)">复制摘要</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- ========== 知识付费选项（新增） ========== -->
      <div class="vip-option">
        <div class="vip-label">
          <i class="el-icon-star-on"></i> VIP专属内容
        </div>
        <el-switch
          v-model="blog.isVipOnly"
          active-text="开启"
          inactive-text="关闭"
          :active-color="'#f5a623'"
          inactive-color="'#cbd5e1'"
        />
        <div class="vip-tip">
          <i class="el-icon-info"></i> 开启后，只有VIP会员才能阅读此博客
        </div>
      </div>

      <!-- ========== 富文本编辑器（Quill 1.x） ========== -->
      <!-- 使用 v-if="isClient" 确保只在客户端渲染编辑器，避免 SSR 报错 -->
      <div v-if="isClient" class="editor-container">
        <!-- 编辑器工具栏：包含各种格式化按钮 -->
        <div id="toolbar">
          <!-- 工具栏内容由 Quill 自动渲染，这里只需一个容器 -->
        </div>
        <!-- 编辑器主体区域 -->
        <div id="editor" class="ql-editor-container"></div>
      </div>
      <!-- 服务端渲染时显示加载占位符，避免白屏 -->
      <div v-else class="editor-placeholder">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <!-- ========== 草稿箱抽屉 ========== -->
    <el-drawer
      title="我的草稿"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="400px"
      :before-close="handleDrawerClose"
    >
      <div class="draft-list" v-loading="loadingDrafts">
        <!-- 遍历显示草稿列表 -->
        <el-card
          v-for="draft in draftList"
          :key="draft.id"
          class="draft-item"
          shadow="hover"
          @click.native="editDraft(draft.id)"
        >
          <h4>{{ draft.title || '无标题' }}</h4>
          <!-- 草稿摘要：去除HTML标签后显示前50个字符 -->
          <p class="draft-summary">{{ draft.summary || stripHtml(draft.content).slice(0, 50) + '...' }}</p>
          <div class="draft-meta">
            <span>最后更新：{{ formatTime(draft.updatedAt) }}</span>
          </div>
        </el-card>
        
        <!-- 分页组件：当草稿总数大于每页条数时显示 -->
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

        <!-- 空状态提示 -->
        <div v-if="draftList.length === 0 && !loadingDrafts" class="empty-draft">
          暂无草稿
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
/**
 * 写博客页面组件
 * 功能：
 * - 创建/编辑博客（支持标题、标签、富文本内容）
 * - 设置博客是否为VIP专属（知识付费）
 * - 草稿箱管理（保存草稿、加载草稿）
 * - 图片上传（通过Quill自定义处理器）
 * 
 * 依赖：
 * - Quill 富文本编辑器（通过客户端插件注入）
 * - 后端API：GetCurrentUser, GetAllTags, GetBlogById, CreateBlog, UpdateBlog, GetBlogDrafts, UploadFile
 */
import { GetCurrentUser, GetAllTags, GetBlogById, CreateBlog, UpdateBlog, GetBlogDrafts, UploadFile } from '@/api/index'
import { aiPolishBlog, aiGenerateBlogSummary, parseBlogSummaryResult } from '@/api/aiAssistant'
import { listEnabledAiModels, getActiveAiModel, pageAiModels } from '@/api/aiAdmin'

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
      const content = trimmed.slice(level).trim()
      blocks.push(`<h${level}>${renderInlineMarkdown(content)}</h${level}>`)
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

export default {
  name: 'WriteBlog',
  layout: 'blogwrite', // 使用简单布局（无侧边栏）
  
  // ========== 组件数据 ==========
  data() {
    return {
      // ----- 博客数据对象 -----
      blog: {
        id: null,
        title: '',
        summary: '',
        content: '',
        tags: [],
        status: 'draft',
        isVipOnly: false,
      },
      
      // ----- 标签相关 -----
      tagOptions: [],       // 从后端获取的标签选项列表
      loadingTags: false,   // 标签加载状态
      
      // ----- 用户信息 -----
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', // 默认头像
      username: '当前用户',  // 用户名（优先显示昵称）
      userId: '',           // 用户ID，用于跳转个人主页及关联作者
      
      // ----- 操作状态 -----
      savingDraft: false,   // 保存草稿按钮loading状态
      publishing: false,    // 发布按钮loading状态
      lastSaved: '',        // 最后一次保存草稿的时间（HH:mm:ss）
      
      // ----- 草稿箱相关 -----
      drawerVisible: false, // 草稿箱抽屉显示状态
      draftList: [],        // 草稿列表
      loadingDrafts: false, // 加载草稿中
      draftPage: 1,         // 草稿列表当前页码
      pageSize: 10,         // 每页显示草稿数量
      draftTotal: 0,        // 草稿总数
      
      // ----- 编辑器实例 -----
      quill: null,          // Quill 编辑器实例
      isClient: false,      // 是否在客户端环境（用于控制编辑器渲染）

      aiPolishing: false,
      aiSummarizing: false,
      aiModelLoading: false,
      aiModelOptions: [],
      activeAiModel: null,
      selectedAiModelId: null,
      aiSummaryResult: '',
      aiPolishResult: '',
      aiSuggestedTags: [],
      aiResultTab: 'summary',
      lastAiModelLabel: '',
      showAiResult: false
    };
  },
  
  computed: {
    hasAiResult() {
      return !!(this.aiPolishResult || this.aiSummaryResult || this.aiSuggestedTags.length)
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
    renderedAiSummaryResult() {
      return this.renderAiResultContent(this.aiSummaryResult, 'markdown', '暂无 AI 摘要结果')
    },
    renderedAiPolishResult() {
      return this.renderAiResultContent(this.aiPolishResult, 'auto', '暂无 AI 润色结果')
    }
  },

  // ========== 生命周期钩子 ==========
  
  /**
   * 组件挂载完成后初始化编辑器
   * 使用 $nextTick 确保 DOM 已经渲染完成
   */
  mounted() {
    this.isClient = true;
    this.$nextTick(() => {
      this.initEditor();
    });
  },
  
  /**
   * 组件创建时加载数据
   */
  created() {
    this.fetchTags();
    this.restoreUserIdentityFromCache();
    this.loadUserInfo();
    this.initAiModels();

    const blogId = this.$route.params.id;
    if (blogId) {
      this.fetchBlog(blogId);
    }
  },
  
  /**
   * 组件销毁前清理资源
   */
  beforeDestroy() {
    if (this.quill) {
      this.quill = null;        // 释放 Quill 实例，避免内存泄漏
    }
  },
  
  // ========== 组件方法 ==========
  methods: {

    getAiModelStorageKey() {
      return 'blog_write_ai_model_id'
    },

    normalizeAiModelId(value) {
      if (value === null || value === undefined || value === '') return null
      return String(value)
    },

    getAiModelOptionLabel(item = {}) {
      const modelName = item.modelName || item.name || `模型#${item.id || '-'}`
      const provider = item.providerCode || item.provider || item.platform || ''
      return provider ? `${modelName}（${provider}）` : modelName
    },

    async initAiModels() {
      this.aiModelLoading = true
      try {
        const [enabledRes, activeRes] = await Promise.allSettled([
          listEnabledAiModels(),
          getActiveAiModel()
        ])

        let enabled = []
        let active = null

        if (enabledRes.status === 'fulfilled') {
          const enabledData = extractApiData(enabledRes.value)
          enabled = Array.isArray(enabledData) ? enabledData.map(normalizeAiModel) : []
        } else {
          console.error('加载已启用模型失败:', enabledRes.reason)
        }

        if (activeRes.status === 'fulfilled') {
          const activeData = extractApiData(activeRes.value)
          if (activeData && typeof activeData === 'object' && activeData.id !== undefined && activeData.id !== null) {
            active = normalizeAiModel(activeData)
          }
        } else {
          console.error('加载当前激活模型失败:', activeRes.reason)
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

        if (active && !enabled.some(item => String(item.id) === String(active.id))) {
          enabled.unshift(active)
        }

        this.aiModelOptions = enabled
        this.activeAiModel = active

        let cached = null
        if (process.client) {
          cached = window.localStorage.getItem(this.getAiModelStorageKey())
        }

        const preferredModelId = cached
          ? String(cached)
          : (active && active.id) || (enabled[0] && enabled[0].id) || null

        this.selectedAiModelId = preferredModelId === '' ? null : preferredModelId
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

    getPlainBlogContent() {
      return this.stripHtml(this.blog.content || '').trim()
    },

    renderAiResultContent(source, mode = 'auto', emptyText = '暂无内容') {
      const raw = String(source || '').trim()
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

      const contentText = this.getPlainBlogContent()
      if (!contentText) {
        this.$message.warning('请先填写博客内容')
        return
      }

      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.aiPolishing = true

      try {
        const result = await aiPolishBlog({
          userId: userId || undefined,
          modelId: this.selectedAiModelId || undefined,
          title: this.blog.title,
          content: contentText
        })

        if (!result) {
          this.$message.warning('AI 未返回润色结果')
          return
        }

        const polished = typeof result === 'string' ? result : (result && (result.content || result.text || result.html)) || JSON.stringify(result, null, 2)
        this.aiPolishResult = polished
        this.aiResultTab = 'polish'
        this.lastAiModelLabel = this.currentAiModelLabel
        this.showAiResult = true

        this.handleApplyAiPolish(polished)
      } catch (error) {
        console.error('AI 润色失败:', error)
        this.$message.error(error.response?.data?.message || error.message || 'AI 润色失败，请稍后重试')
      } finally {
        this.aiPolishing = false
      }
    },

    async handleAiGenerateSummary() {
      if (!this.blog.title.trim()) {
        this.$message.warning('请先填写博客标题')
        return
      }

      const contentText = this.getPlainBlogContent()
      if (!contentText) {
        this.$message.warning('请先填写博客内容')
        return
      }

      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.aiSummarizing = true

      try {
        const result = await aiGenerateBlogSummary({
          userId: userId || undefined,
          modelId: this.selectedAiModelId || undefined,
          title: this.blog.title,
          content: contentText
        })

        if (!result) {
          this.$message.warning('AI 未返回摘要结果')
          return
        }

        const parsed = parseBlogSummaryResult(result) || {}
        const summaryText = parsed.summary || (typeof result === 'string' ? result : (result && (result.summary || result.text)) || JSON.stringify(result, null, 2))

        this.aiSummaryResult = summaryText
        this.blog.summary = summaryText
        this.showAiResult = true
        this.aiResultTab = 'summary'
        this.lastAiModelLabel = this.currentAiModelLabel

        const matchedTags = this.matchAiTagsToOptions(parsed.tags || [])
        this.aiSuggestedTags = matchedTags

        if (matchedTags.length > 0) {
          const currentTagIds = Array.isArray(this.blog.tags) ? this.blog.tags.slice() : []
          const merged = [...new Set([...currentTagIds, ...matchedTags.map(item => item.id)])]
          this.blog.tags = merged
          this.$message.success(`AI 已生成摘要，并自动匹配 ${matchedTags.length} 个标签`)
        } else {
          this.$message.success('AI 已生成摘要，但没有匹配到现有标签')
        }
      } catch (error) {
        console.error('AI 生成摘要失败:', error)
        this.$message.error(error.response?.data?.message || error.message || 'AI 生成摘要失败，请稍后重试')
      } finally {
        this.aiSummarizing = false
      }
    },

    applyAiSummaryToForm() {
      if (!this.aiSummaryResult) {
        this.$message.warning('没有可应用的摘要')
        return
      }
      this.blog.summary = this.aiSummaryResult
      if (this.aiSuggestedTags.length > 0) {
        const currentTagIds = Array.isArray(this.blog.tags) ? this.blog.tags.slice() : []
        this.blog.tags = [...new Set([...currentTagIds, ...this.aiSuggestedTags.map(item => item.id)])]
      }
      this.$message.success('AI 摘要与标签已应用到表单')
    },

    clearAiResult() {
      this.aiSummaryResult = ''
      this.aiPolishResult = ''
      this.aiSuggestedTags = []
      this.aiResultTab = 'summary'
      this.lastAiModelLabel = ''
      this.showAiResult = false
    },

    copyText(text) {
      if (!text) {
        this.$message.warning('没有可复制的内容')
        return
      }

      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text)
          .then(() => this.$message.success('复制成功'))
          .catch(() => this.$message.error('复制失败'))
        return
      }

      const textarea = document.createElement('textarea')
      textarea.value = text
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success('复制成功')
    },

    copyAiSummaryResult() {
      this.copyText(this.aiSummaryResult)
    },

    // ========== 用户相关方法 ==========
    
    /**
     * 跳转到用户主页
     * 使用当前用户ID拼接路由
     */
    goToUserHome() {
      if (!this.userId) {
        this.$message.warning('暂未获取到当前用户信息')
        return
      }
      this.$router.push(`/user/${this.userId}`);
    },
    
    /**
     * 加载用户信息（入口方法）
     */
    loadUserInfo() {
      console.log('loadUserInfo方法被调用');
      this.fetchUserInfoFromApi();
    },

    /**
     * 从API获取用户信息
     * 处理不同的响应格式，提取用户ID、昵称、头像
     */
    async fetchUserInfoFromApi() {
      console.log('开始调用API获取用户信息...');
      try {
        const response = await GetCurrentUser();
        console.log('API响应:', response);
        
        let userData = null;
        
        // 处理不同的响应格式
        if (response.data && typeof response.data.code !== 'undefined') {
          // 标准格式 {code: 0, data: {...}}
          if (response.data.code === 0 && response.data.data) {
            userData = response.data.data;
          }
        } else if (response.data && response.data.id) {
          // 直接返回用户对象在data字段中
          userData = response.data;
        } else if (response && response.id) {
          // 直接返回用户对象（无data字段）
          userData = response;
        }
        
        // 更新用户信息
        if (userData && userData.id) {
          this.userId = userData.id;
          this.username = userData.nickname || userData.username || '当前用户';
          this.userAvatar = userData.avatarUrl || userData.avatar || this.userAvatar;
          
          // 存储用户信息到 localStorage（仅在客户端，便于其他页面使用）
          if (process.client) {
            try {
              localStorage.setItem('userInfo', JSON.stringify(userData));
            } catch (storageError) {
              console.error('存储用户信息失败:', storageError);
            }
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        if (error.response && error.response.status === 404) {
          console.log('用户未登录');
        }
        this.restoreUserIdentityFromCache()
      }
    },

    // ========== 标签相关方法 ==========
    
    /**
     * 获取标签列表
     * 用于标签选择下拉框
     */
    async fetchTags() {
      this.loadingTags = true;
      try {
        console.log('开始请求标签数据...');
        const res = await GetAllTags();
        console.log('API返回数据:', res);

        // 处理不同的响应格式
        if (Array.isArray(res)) {
          this.tagOptions = res;                       // 直接返回数组
        } else if (Array.isArray(res.data)) {
          this.tagOptions = res.data;                   // 数据在 data 字段中
        } else if (res.data && typeof res.data === 'object' && res.data.code === 0) {
          this.tagOptions = res.data.data || [];        // 标准格式 {code:0, data: [...]}
        } else {
          console.warn('未识别的API返回格式:', res);
          this.tagOptions = [];
        }
      } catch (error) {
        console.error('获取标签失败:', error);
        this.$message.error('获取标签失败：' + (error.message || '网络错误'));
      } finally {
        this.loadingTags = false;
      }
    },

    // ========== 博客相关方法 ==========
    
    /**
     * 获取博客详情（用于编辑模式）
     * @param {number|string} blogId - 博客ID
     */
    async fetchBlog(blogId) {
      try {
        console.log('获取博客详情:', blogId);
        const res = await GetBlogById(blogId);
        
        if (res && typeof res === 'object') {
          let blogData = null;
          
          // 处理不同的响应格式
          if (res.data && typeof res.data.code !== 'undefined') {
            if (res.data.code === 0) {
              blogData = res.data.data;                 // 标准格式
            } else {
              this.$message.error('获取博客失败：' + res.data.message);
              return;
            }
          } else {
            blogData = res;                              // 直接返回博客对象
          }
          
          if (blogData) {
            // 更新博客数据，包括 isVipOnly 字段（若后端返回）
            this.blog = {
              id: blogData.id,
              title: blogData.title,
              content: blogData.content,
              tags: Array.isArray(blogData.tags) ? blogData.tags : (blogData.tags ? [blogData.tags] : []),
              status: blogData.status,
              summary: blogData.summary || '',
              isVipOnly: blogData.isVipOnly || false,   // 获取 VIP 状态，默认为 false
            };
            
            // 如果有内容且编辑器已初始化，设置到编辑器中
            if (this.quill && blogData.content) {
              this.quill.root.innerHTML = blogData.content;
            }
          }
        }
      } catch (error) {
        console.error('获取博客失败:', error);
        this.$message.error('网络错误，请稍后重试');
      }
    },

    /**
     * 保存博客（通用方法）
     * @param {string} status - 'draft' 或 'pending'（待审核）
     * @returns {Promise<boolean>} - 是否保存成功
     */
    async saveBlog(status) {
      // 校验标题
      if (!this.blog.title.trim()) {
        this.$message.warning('请填写博客标题');
        return false;
      }
      
      // 校验内容（去除HTML标签后）
      const contentText = this.stripHtml(this.blog.content).trim();
      if (!contentText) {
        this.$message.warning('请填写博客内容');
        return false;
      }

      const isPublish = status === 'pending' || status === 'published';
      if (isPublish) {
        this.publishing = true;
      } else {
        this.savingDraft = true;
      }

      try {
        // 处理标签：转换为数字类型的标签ID数组
        let tagIds = [];
        if (this.blog.tags && Array.isArray(this.blog.tags)) {
          if (this.blog.tags.length > 0 && typeof this.blog.tags[0] === 'object') {
            // 如果是标签对象数组，提取id并转换为数字
            tagIds = this.blog.tags.map(tag => {
              const id = tag.id;
              return typeof id === 'string' ? parseInt(id, 10) : id;
            }).filter(id => typeof id === 'number' && !isNaN(id));
          } else if (this.blog.tags.length > 0) {
            // 如果是ID数组，确保转换为数字类型
            tagIds = this.blog.tags.map(id => {
              return typeof id === 'string' ? parseInt(id, 10) : id;
            }).filter(id => typeof id === 'number' && !isNaN(id));
          }
        }

        // 构建请求数据，包含 VIP 标志
        const requestData = {
          title: this.blog.title,
          content: this.blog.content,
          status: status,
          tagIds: tagIds,
          summary: this.blog.summary ? this.blog.summary.trim() : '',
          isVipOnly: this.blog.isVipOnly,   // 新增：是否VIP专属
        };

        // 如果是新建博客，添加用户ID
        if (!this.blog.id && this.userId) {
          requestData.userId = this.userId;
        }

        let res;
        if (this.blog.id) {
          // 编辑模式：PUT 请求
          console.log('编辑博客，ID:', this.blog.id);
          res = await UpdateBlog(this.blog.id, requestData);
        } else {
          // 新建模式：POST 请求
          console.log('创建新博客');
          res = await CreateBlog(requestData);
        }

        // 处理响应
        if (res && typeof res === 'object') {
          // 处理不同的响应格式
          let result = res;
          if (res.data && typeof res.data === 'object') {
            result = res.data;
          }
          
          // 如果是新建博客，保存返回的ID
          if (!this.blog.id) {
            this.blog.id = result.id || result._id;
          }
          this.blog.status = result.status || status;

          // 保存成功后的处理
          if (status === 'draft') {
            const now = new Date();
            this.lastSaved = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
            this.$message.success('草稿保存成功');
          } else {
            this.$message.success('已发布申请，请等待审核通过');
          }
          return true;
        }
      } catch (error) {
        console.error('保存博客出错', error);
        // 错误处理
        if (error.response) {
          this.$message.error((isPublish ? '发布' : '保存草稿') + '失败：' + (error.response.data?.message || '未知错误'));
        } else if (error.request) {
          this.$message.error('网络错误，请检查连接');
        } else {
          this.$message.error('请求错误：' + error.message);
        }
        return false;
      } finally {
        // 重置加载状态
        if (isPublish) {
          this.publishing = false;
        } else {
          this.savingDraft = false;
        }
      }
    },

    /**
     * 保存为草稿
     */
    saveDraft() {
      this.saveBlog('draft');
    },

    /**
     * 发布博客（实际是提交待审核）
     * 修改状态为 'pending'，由后台管理员审核后变为 'published'
     */
    publishBlog() {
      this.saveBlog('pending');
    },

    // ========== 草稿箱相关方法 ==========
    
    /**
     * 打开草稿箱抽屉并加载草稿列表
     */
    openDraftDrawer() {
      this.drawerVisible = true;
      this.draftPage = 1; // 重置到第一页
      this.fetchDrafts();
    },

    /**
     * 获取草稿列表
     * 调用 GetBlogDrafts API 获取当前用户的草稿
     */
    async fetchDrafts() {
      this.loadingDrafts = true;
      try {
        const res = await GetBlogDrafts();
        
        console.log('获取草稿响应:', res);
        
        let draftsData = [];
        let total = 0;
        
        // 处理不同的响应格式
        if (res && typeof res === 'object') {
          if (res.data && typeof res.data.code !== 'undefined') {
            if (res.data.code === 0) {
              draftsData = res.data.data.list || res.data.data;
              total = res.data.data.total || 0;
            }
          } else if (Array.isArray(res)) {
            draftsData = res;
            total = res.length;
          } else if (res.data && Array.isArray(res.data)) {
            draftsData = res.data;
            total = res.data.length;
          } else {
            draftsData = [];
          }
        }
        
        this.draftList = draftsData;
        this.draftTotal = total;
      } catch (error) {
        console.error('获取草稿失败:', error);
        this.$message.error('网络错误，无法加载草稿');
      } finally {
        this.loadingDrafts = false;
      }
    },

    /**
     * 编辑草稿：加载草稿内容到编辑器
     * @param {number|string} id - 草稿ID
     */
    editDraft(id) {
      // 先从当前列表查找
      const draft = this.draftList.find(item => item.id === id);
      if (draft) {
        this.blog = {
          id: draft.id,
          title: draft.title || '',
          content: draft.content || '',
          tags: draft.tags || [],
          status: draft.status || 'draft',
          summary: draft.summary || '',
          isVipOnly: draft.isVipOnly || false, // 加载草稿的 VIP 状态
        };
        
        // 更新编辑器内容
        if (this.quill && draft.content) {
          this.quill.root.innerHTML = draft.content;
        }
        
        this.drawerVisible = false;
        this.$message.success('已加载草稿内容');
      } else {
        // 如果不在当前列表，通过ID单独加载
        this.loadDraftById(id);
      }
    },

    /**
     * 通过ID加载草稿
     * @param {number|string} id - 草稿ID
     */
    async loadDraftById(id) {
      try {
        const res = await GetBlogById(id);
        
        if (res && typeof res === 'object') {
          let draftData = null;
          
          if (res.data && typeof res.data.code !== 'undefined') {
            if (res.data.code === 0) {
              draftData = res.data.data;
            }
          } else {
            draftData = res;
          }
          
          if (draftData) {
            this.blog = {
              id: draftData.id,
              title: draftData.title || '',
              content: draftData.content || '',
              tags: Array.isArray(draftData.tags) ? draftData.tags : [],
              status: draftData.status || 'draft',
              summary: draftData.summary || '',
              isVipOnly: draftData.isVipOnly || false,
            };
            
            if (this.quill && draftData.content) {
              this.quill.root.innerHTML = draftData.content;
            }
            
            this.drawerVisible = false;
            this.$message.success('已加载草稿内容');
          }
        }
      } catch (error) {
        console.error('加载草稿失败:', error);
        this.$message.error('加载草稿失败：' + (error.message || '网络错误'));
      }
    },

    /**
     * 抽屉关闭前的处理
     * @param {Function} done - 关闭回调
     */
    handleDrawerClose(done) {
      done(); // 直接关闭，无需额外操作
    },

    // ========== 编辑器相关方法 ==========
    
    /**
     * 初始化 Quill 编辑器
     * 通过客户端插件注入的 $quill 访问 Quill 构造函数
     */
    initEditor() {
      // 确保只在客户端执行
      if (!process.client) return;
      
      // 通过 this.$quill 获取 Quill 构造函数（从插件注入）
      const Quill = this.$quill;
      
      if (!Quill) {
        console.error('Quill 未加载，请检查插件配置');
        return;
      }
      
      // 创建 Quill 编辑器实例
      this.quill = new Quill('#editor', {
        modules: {
          toolbar: '#toolbar',      // 使用指定的工具栏
          syntax: false,            // 禁用代码高亮（需要额外配置）
        },
        placeholder: '请输入博客内容...',
        theme: 'snow',              // 使用雪碧主题
        readOnly: false
      });

      // 监听内容变化，同步到 blog.content
      this.quill.on('text-change', () => {
        this.blog.content = this.quill.root.innerHTML;
      });

      // 如果有初始内容，设置到编辑器
      if (this.blog.content) {
        this.quill.root.innerHTML = this.blog.content;
      }

      // 为图片按钮添加自定义处理器
      const toolbar = this.quill.getModule('toolbar');
      toolbar.addHandler('image', this.imageHandler);
      
      console.log('Quill 编辑器初始化完成');
    },

    /**
     * 图片上传处理器
     * 在工具栏中点击图片按钮时触发，打开文件选择器，上传后插入编辑器
     */
    imageHandler() {
      // 创建文件输入框
      const input = document.createElement('input');
      input.setAttribute('type', 'file');
      input.setAttribute('accept', 'image/*');
      input.click();

      input.onchange = async () => {
        const file = input.files[0];
        const formData = new FormData();
        formData.append('image', file);

        try {
          // 上传图片到服务器
          const res = await UploadFile(formData);
          // 获取当前光标位置
          const range = this.quill.getSelection();
          // 在光标位置插入图片
          this.quill.insertEmbed(range.index, 'image', res.data.url);
        } catch (error) {
          console.error('图片上传失败:', error);
          this.$message.error('图片上传失败');
        }
      };
    },

    /**
     * 去除HTML标签，获取纯文本
     * @param {string} html - HTML字符串
     * @returns {string} - 纯文本
     */
    stripHtml(html) {
      if (!html) return '';
      // 仅在客户端执行，避免服务端报错
      if (process.client) {
        const tmp = document.createElement('div');
        tmp.innerHTML = html;
        return tmp.textContent || tmp.innerText || '';
      }
      return html.replace(/<[^>]*>/g, ''); // 服务端简单替换
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

    handleApplyAiPolish(content) {
      if (!content) {
        this.$message.warning('没有可应用的正文')
        return
      }

      this.blog.content = content

      if (this.quill) {
        this.quill.root.innerHTML = content
      }

      this.$message.success('AI 润色结果已应用到正文')
    },

    handleApplyAiSummary(payload) {
      const { summary, tags } = payload || {}

      if (summary) {
        console.log('AI 摘要：', summary)
      }

      const matchedTagIds = this.findMatchedTagIdsByNames(tags || [])
      if (matchedTagIds.length > 0) {
        const currentTagIds = Array.isArray(this.blog.tags) ? this.blog.tags.slice() : []
        this.blog.tags = [...new Set([...currentTagIds, ...matchedTagIds])]
        this.$message.success(`已自动应用 ${matchedTagIds.length} 个标签`)
      } else {
        this.$message.success('AI 摘要已生成，但没有匹配到现有标签')
      }
    },

    /**
     * 格式化时间
     * @param {string} time - 时间字符串
     * @returns {string} - 格式化后的时间 YYYY-MM-DD HH:mm
     */
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`;
    },

  }
};
</script>

<style scoped>
.ai-model-select {
  width: 220px;
}

/* ========== 全局样式 ========== */
.write-blog-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* ========== 头部区域 ========== */
.write-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

/* 用户信息 */
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

/* 操作按钮组 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 最后保存提示 */
.save-tip {
  font-size: 14px;
  color: #64748b;
  background: #f1f5f9;
  padding: 5px 10px;
  border-radius: 20px;
}

/* 按钮统一美化 */
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

/* ========== 编辑区域 ========== */
.edit-area {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

/* 标题输入框 */
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

/* 标签选择器 */
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

/* ========== 知识付费选项样式（新增） ========== */
.vip-option {
  display: flex;
  align-items: center;
  gap: 15px;
  background: white;
  padding: 15px 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.vip-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #f59e0b;
}
.vip-label i {
  font-size: 16px;
}
.vip-tip {
  font-size: 12px;
  color: #64748b;
  margin-left: auto;
}
.vip-tip i {
  margin-right: 4px;
}

/* ========== 编辑器容器 ========== */
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

/* 编辑器占位符（SSR时） */
.editor-placeholder {
  padding: 30px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  min-height: 400px;
}

/* ========== 草稿箱抽屉 ========== */
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
.draft-item h4 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.draft-summary {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
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

/* ========== 响应式 ========== */
@media screen and (max-width: 768px) {
  .write-header {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  .action-buttons {
    justify-content: flex-end;
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
  .vip-option {
    flex-wrap: wrap;
    gap: 10px;
  }
  .vip-tip {
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

  .action-buttons {
    flex-wrap: wrap;
  }


}
</style>