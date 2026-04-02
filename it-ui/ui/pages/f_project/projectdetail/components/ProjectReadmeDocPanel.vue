<template>
  <el-card shadow="never" class="section-card readme-section-card project-doc-readme-panel">
    <div slot="header" class="section-header section-header-flex readme-section-header">
      <div class="readme-section-title">
        <div class="readme-title-main">
          <span>README</span>
          <el-tag size="mini" effect="plain" :type="primaryDoc ? 'success' : 'info'">
            {{ primaryDoc ? '文档中心优先' : '项目文件回退' }}
          </el-tag>
        </div>
        <div class="readme-title-sub">{{ resolvedLeadText }}</div>
      </div>

      <div class="readme-header-right readme-header-right-doc">
        <div class="readme-stats-inline">
          <span class="readme-stat-chip"><i class="el-icon-document"></i><em>{{ docCount }}</em> 文档</span>
          <span class="readme-stat-chip"><i class="el-icon-folder-opened"></i><em>{{ sourceLabel }}</em></span>
        </div>
        <el-button size="mini" type="text" icon="el-icon-tickets" @click="openDocCenter">文档入口</el-button>
        <el-button
          v-if="canManageProject"
          size="mini"
          type="text"
          icon="el-icon-edit-outline"
          @click="$emit('open-doc-manage')"
        >管理文档</el-button>
      </div>
    </div>

    <div class="readme-showcase" :class="{ 'is-empty': !hasPrimaryContent }">
      <div v-if="hasPrimaryContent" class="readme-hero readme-hero-doc">
        <div class="readme-hero-main">
          <div class="readme-eyebrow">{{ primaryDoc ? '项目文档中心' : '项目说明文档' }}</div>
          <div class="readme-hero-title">{{ displayTitle }}</div>
          <div class="readme-hero-desc">{{ resolvedLeadText }}</div>
        </div>

        <div class="readme-hero-stats doc-hero-actions">
          <div class="readme-hero-stat">
            <div class="readme-hero-stat-value">{{ primaryDoc ? typeText(primaryDoc.docType) : 'README' }}</div>
            <div class="readme-hero-stat-label">文档类型</div>
          </div>
          <div class="readme-hero-stat">
            <div class="readme-hero-stat-value">{{ primaryDoc ? ('v' + (primaryDoc.currentVersion || 1)) : '-' }}</div>
            <div class="readme-hero-stat-label">当前版本</div>
          </div>
          <div class="readme-hero-stat">
            <div class="readme-hero-stat-value">{{ recentDocs.length }}</div>
            <div class="readme-hero-stat-label">最近文档</div>
          </div>
          <div class="readme-hero-stat">
            <div class="readme-hero-stat-value">{{ primaryDoc ? statusText(primaryDoc.status) : '回退' }}</div>
            <div class="readme-hero-stat-label">展示来源</div>
          </div>
        </div>
      </div>

      <div v-if="recentDocs.length" class="doc-quick-entry">
        <div class="doc-quick-entry-title">最近文档</div>
        <div class="doc-quick-entry-list">
          <div
            v-for="item in recentDocs"
            :key="item.id"
            class="doc-quick-entry-item"
            :class="{ 'is-active': Number(primaryDoc && primaryDoc.id) === Number(item.id) }"
            @click="openDocCenter(item)"
          >
            <div class="doc-quick-entry-top">
              <span class="doc-quick-entry-name">{{ item.title }}</span>
              <el-tag size="mini" effect="plain">{{ typeText(item.docType) }}</el-tag>
            </div>
            <div class="doc-quick-entry-meta">{{ formatTime(item.updatedAt || item.createdAt) }}</div>
          </div>
        </div>
      </div>

      <div class="readme-shell">
        <div v-if="primaryDoc" class="readme-box ai-rich-content" v-html="renderedPrimaryHtml"></div>
        <div v-else-if="fallbackHasContent" class="readme-box ai-rich-content" v-html="fallbackHtml"></div>
        <el-empty v-else description="暂无 README 或项目文档" :image-size="72" />
      </div>
    </div>

    <el-drawer
      title="项目文档"
      :visible.sync="drawerVisible"
      size="56%"
      append-to-body
      custom-class="project-doc-drawer"
    >
      <div class="doc-drawer-layout">
        <div class="doc-drawer-left">
          <div class="doc-drawer-toolbar">
            <el-input
              v-model.trim="keyword"
              size="small"
              clearable
              placeholder="搜索文档标题"
              @input="handleKeywordInput"
            />
            <el-button size="small" icon="el-icon-refresh" @click="loadDocs">刷新</el-button>
          </div>

          <div v-if="loading" class="doc-drawer-loading">
            <i class="el-icon-loading"></i>
            <span>文档加载中...</span>
          </div>

          <div v-else-if="filteredDocs.length" class="doc-drawer-list">
            <div
              v-for="item in filteredDocs"
              :key="item.id"
              class="doc-drawer-item"
              :class="{ 'is-active': Number(activeDoc && activeDoc.id) === Number(item.id) }"
              @click="selectDoc(item)"
            >
              <div class="doc-drawer-item-title">{{ item.title }}</div>
              <div class="doc-drawer-item-meta">
                <span>{{ typeText(item.docType) }}</span>
                <span>·</span>
                <span>v{{ item.currentVersion || 1 }}</span>
                <span>·</span>
                <span>{{ formatTime(item.updatedAt || item.createdAt) }}</span>
              </div>
            </div>
          </div>

          <el-empty v-else description="暂无匹配文档" :image-size="68" />
        </div>

        <div class="doc-drawer-right">
          <div class="doc-drawer-preview-top">
            <div>
              <div class="doc-drawer-preview-title">{{ activeDoc ? activeDoc.title : '文档预览' }}</div>
              <div v-if="activeDoc" class="doc-drawer-preview-meta">
                <span>{{ typeText(activeDoc.docType) }}</span>
                <span>·</span>
                <span>{{ statusText(activeDoc.status) }}</span>
                <span>·</span>
                <span>{{ visibilityText(activeDoc.visibility) }}</span>
              </div>
            </div>
            <el-button v-if="canManageProject" size="mini" type="primary" plain @click="$emit('open-doc-manage')">进入文档中心</el-button>
          </div>

          <div v-if="activeDoc" class="doc-drawer-preview-body ai-rich-content" v-html="renderActiveDocHtml"></div>
          <el-empty v-else description="左侧选择一篇文档后可在这里预览" :image-size="70" />
        </div>
      </div>
    </el-drawer>
  </el-card>
</template>

<script>
import { getProjectDoc, listProjectDocs } from '@/api/projectDoc'

export default {
  name: 'ProjectReadmeDocPanel',
  props: {
    projectId: {
      type: [Number, String],
      required: true
    },
    projectName: {
      type: String,
      default: ''
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    fallbackHtml: {
      type: String,
      default: ''
    },
    fallbackHasContent: {
      type: Boolean,
      default: false
    },
    fallbackLeadText: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      docs: [],
      primaryDoc: null,
      drawerVisible: false,
      activeDoc: null,
      keyword: ''
    }
  },
  computed: {
    hasPrimaryContent() {
      if (this.primaryDoc && this.primaryDoc.content) return true
      return !!this.fallbackHasContent
    },
    docCount() {
      return this.docs.length
    },
    sourceLabel() {
      return this.primaryDoc ? '文档中心' : '项目文件'
    },
    resolvedLeadText() {
      if (this.primaryDoc && this.primaryDoc.content) {
        return this.extractLeadText(this.primaryDoc.content)
      }
      return this.fallbackLeadText || '优先展示项目文档中心中的主 README 候选文档。'
    },
    displayTitle() {
      if (this.primaryDoc && this.primaryDoc.title) return this.primaryDoc.title
      return `${this.projectName || '未命名项目'} README`
    },
    recentDocs() {
      return this.docs.slice(0, 4)
    },
    filteredDocs() {
      if (!this.keyword) return this.docs
      const k = this.keyword.toLowerCase()
      return this.docs.filter(item => {
        const title = (item.title || '').toLowerCase()
        const type = (item.docType || '').toLowerCase()
        return title.includes(k) || type.includes(k)
      })
    },
    renderedPrimaryHtml() {
      if (!this.primaryDoc || !this.primaryDoc.content) return ''
      return this.renderBasicMarkdown(this.primaryDoc.content)
    },
    renderActiveDocHtml() {
      if (!this.activeDoc || !this.activeDoc.content) return ''
      return this.renderBasicMarkdown(this.activeDoc.content)
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(v) {
        if (v !== null && v !== undefined && String(v) !== '') {
          this.loadDocs()
        }
      }
    }
  },
  methods: {
    async loadDocs() {
      if (this.projectId === null || this.projectId === undefined || String(this.projectId) === '') return
      try {
        this.loading = true
        const res = await listProjectDocs(this.projectId, { status: 'published' })
        const rows = Array.isArray(res?.data) ? res.data.slice() : []
        rows.sort((a, b) => {
          const ta = new Date(a.updatedAt || a.createdAt || 0).getTime()
          const tb = new Date(b.updatedAt || b.createdAt || 0).getTime()
          return tb - ta
        })
        this.docs = rows
        const primary = this.pickPrimaryDoc(rows)
        this.primaryDoc = primary || null
        if (primary && primary.id) {
          await this.selectDoc(primary, false)
        } else {
          this.activeDoc = null
        }
      } catch (e) {
        this.docs = []
        this.primaryDoc = null
        this.activeDoc = null
      } finally {
        this.loading = false
      }
    },
    pickPrimaryDoc(rows) {
      if (!Array.isArray(rows) || !rows.length) return null
      const candidates = rows.filter(item => this.isReadmeCandidate(item))
      if (candidates.length) return candidates[0]
      const wiki = rows.find(item => ['wiki', 'manual', 'spec', 'design'].includes(item.docType))
      return wiki || rows[0]
    },
    isReadmeCandidate(item) {
      if (!item) return false
      const title = String(item.title || '').toLowerCase()
      return title.includes('readme') || title.includes('说明') || title.includes('项目介绍')
    },
    async selectDoc(item, openDrawer = false) {
      if (!item || !item.id) return
      try {
        const res = await getProjectDoc(item.id)
        this.activeDoc = res?.data || item
        if (openDrawer) this.drawerVisible = true
      } catch (e) {
        this.activeDoc = item
        if (openDrawer) this.drawerVisible = true
      }
    },
    async openDocCenter(item) {
      if (item) {
        await this.selectDoc(item, true)
        return
      }
      if (!this.activeDoc && this.primaryDoc) {
        await this.selectDoc(this.primaryDoc, true)
        return
      }
      this.drawerVisible = true
    },
    handleKeywordInput() {
      if (!this.filteredDocs.length) return
      const currentId = this.activeDoc && this.activeDoc.id
      const exists = this.filteredDocs.some(item => Number(item.id) === Number(currentId))
      if (!exists) {
        this.selectDoc(this.filteredDocs[0], false)
      }
    },
    extractLeadText(content) {
      const plain = String(content || '')
        .replace(/```[\s\S]*?```/g, ' ')
        .replace(/#+\s*/g, '')
        .replace(/[-*]\s+/g, '')
        .replace(/\[(.*?)\]\((.*?)\)/g, '$1')
        .replace(/[`>#]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
      if (!plain) return '已从项目文档中心读取主文档内容。'
      return plain.length > 78 ? `${plain.slice(0, 78)}...` : plain
    },
    escapeHtml(str) {
      return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
    },
    renderBasicMarkdown(content) {
      let html = this.escapeHtml(content)
      html = html.replace(/```([\s\S]*?)```/g, (_, code) => `<pre class="doc-md-code"><code>${code.trim()}</code></pre>`)
      html = html.replace(/^###\s+(.*)$/gm, '<h3>$1</h3>')
      html = html.replace(/^##\s+(.*)$/gm, '<h2>$1</h2>')
      html = html.replace(/^#\s+(.*)$/gm, '<h1>$1</h1>')
      html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
      html = html.replace(/^>\s?(.*)$/gm, '<blockquote>$1</blockquote>')
      html = html.replace(/^(?:- |\* )(.*)$/gm, '<li>$1</li>')
      html = html.replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>')
      html = html.replace(/\n{2,}/g, '</p><p>')
      html = `<p>${html}</p>`
      html = html.replace(/<p>(\s*)<(h1|h2|h3|pre|blockquote|ul)>/g, '<$2>')
      html = html.replace(/<\/(h1|h2|h3|pre|blockquote|ul)>(\s*)<\/p>/g, '</$1>')
      html = html.replace(/\n/g, '<br>')
      return html
    },
    typeText(v) {
      const m = {
        wiki: '说明文档',
        spec: '需求规格',
        meeting_note: '会议纪要',
        design: '设计文档',
        manual: '使用手册',
        other: '其他'
      }
      return m[v] || v || '-'
    },
    statusText(v) {
      const m = { draft: '草稿', published: '已发布', archived: '已归档' }
      return m[v] || v || '-'
    },
    visibilityText(v) {
      const m = { project: '项目内', team: '团队', private: '仅自己' }
      return m[v] || v || '-'
    },
    formatTime(v) {
      if (!v) return '-'
      const d = new Date(v)
      if (Number.isNaN(d.getTime())) return v
      const p = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    }
  }
}
</script>

<style scoped>
.project-doc-readme-panel {
  border-radius: 16px;
  overflow: hidden;
}

.section-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.readme-section-card {
  overflow: hidden;
}

.readme-section-header {
  gap: 16px;
  align-items: flex-start;
}

.readme-section-title {
  min-width: 0;
  flex: 1;
}

.readme-title-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.readme-title-sub {
  margin-top: 8px;
  color: #7a869a;
  font-size: 13px;
  line-height: 1.7;
  max-width: 760px;
}

.readme-header-right,
.readme-header-right-doc {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.readme-stats-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.readme-stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f6f9fc;
  border: 1px solid #e8eef7;
  color: #5b6b82;
  font-size: 12px;
  white-space: nowrap;
}

.readme-stat-chip i {
  color: #409eff;
}

.readme-stat-chip em {
  font-style: normal;
  font-weight: 700;
  color: #25364d;
}

.readme-showcase {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.readme-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 22px 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fbff 0%, #f4f8ff 42%, #eef6ff 100%);
  border: 1px solid #e4eefb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.readme-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.12);
  color: #3a7bd5;
  font-size: 12px;
  font-weight: 600;
}

.readme-hero-title {
  margin-top: 12px;
  font-size: 24px;
  line-height: 1.35;
  font-weight: 700;
  color: #20324a;
}

.readme-hero-desc {
  margin-top: 10px;
  color: #5e6f86;
  line-height: 1.85;
  font-size: 14px;
  max-width: 760px;
}

.readme-hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(112px, 1fr));
  gap: 12px;
  min-width: 248px;
}

.readme-hero-stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(219, 231, 245, 0.95);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

.readme-hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #20324a;
  line-height: 1.2;
}

.readme-hero-stat-label {
  margin-top: 6px;
  color: #7a869a;
  font-size: 12px;
}

.doc-hero-actions {
  min-width: 360px;
}

.doc-quick-entry {
  margin: 0 0 18px;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.06), rgba(103, 194, 58, 0.06));
  border: 1px solid rgba(64, 158, 255, 0.14);
}

.doc-quick-entry-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.doc-quick-entry-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.doc-quick-entry-item {
  padding: 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s ease;
}

.doc-quick-entry-item:hover,
.doc-quick-entry-item.is-active {
  border-color: #409eff;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.12);
}

.doc-quick-entry-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.doc-quick-entry-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.doc-quick-entry-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.readme-shell {
  position: relative;
  padding: 24px 26px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
}

.readme-showcase.is-empty .readme-shell {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.readme-box {
  min-height: 140px;
  line-height: 1.9;
  color: #4b5d73;
  white-space: normal;
  word-break: break-word;
  font-size: 15px;
}

.ai-rich-content {
  line-height: 1.9;
  color: #303133;
  white-space: normal;
  word-break: break-word;
}

.ai-rich-content :deep(p),
.ai-rich-content :deep(ul),
.ai-rich-content :deep(ol),
.ai-rich-content :deep(blockquote),
.ai-rich-content :deep(pre),
.ai-rich-content :deep(.markdown-table-wrap),
.ai-rich-content :deep(.markdown-code-block),
.ai-rich-content :deep(.markdown-image-only) {
  margin: 0 0 16px;
}

.ai-rich-content :deep(p:last-child),
.ai-rich-content :deep(ul:last-child),
.ai-rich-content :deep(ol:last-child),
.ai-rich-content :deep(blockquote:last-child),
.ai-rich-content :deep(pre:last-child),
.ai-rich-content :deep(.markdown-table-wrap:last-child),
.ai-rich-content :deep(.markdown-code-block:last-child),
.ai-rich-content :deep(.markdown-image-only:last-child) {
  margin-bottom: 0;
}

.ai-rich-content :deep(h1),
.ai-rich-content :deep(h2),
.ai-rich-content :deep(h3),
.ai-rich-content :deep(h4),
.ai-rich-content :deep(h5),
.ai-rich-content :deep(h6) {
  margin: 24px 0 12px;
  line-height: 1.45;
  color: #1f2d3d;
  font-weight: 700;
}

.ai-rich-content :deep(h1:first-child),
.ai-rich-content :deep(h2:first-child),
.ai-rich-content :deep(h3:first-child),
.ai-rich-content :deep(h4:first-child),
.ai-rich-content :deep(h5:first-child),
.ai-rich-content :deep(h6:first-child) {
  margin-top: 0;
}

.ai-rich-content :deep(h1) { font-size: 30px; }
.ai-rich-content :deep(h2) { font-size: 26px; }
.ai-rich-content :deep(h3) { font-size: 22px; }
.ai-rich-content :deep(h4) { font-size: 19px; }
.ai-rich-content :deep(h5) { font-size: 17px; }
.ai-rich-content :deep(h6) { font-size: 15px; }

.ai-rich-content :deep(ul),
.ai-rich-content :deep(ol) {
  padding-left: 24px;
}

.ai-rich-content :deep(li + li) {
  margin-top: 8px;
}

.ai-rich-content :deep(hr) {
  border: none;
  border-top: 1px solid #e8eef7;
  margin: 22px 0;
}

.ai-rich-content :deep(blockquote) {
  padding: 14px 16px;
  border-left: 4px solid #8bbdff;
  background: linear-gradient(180deg, #f7fbff 0%, #f3f8fd 100%);
  color: #5d6f88;
  border-radius: 12px;
}

.ai-rich-content :deep(a) {
  color: #1d6fdc;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 1px solid rgba(29, 111, 220, 0.2);
}

.ai-rich-content :deep(a:hover) {
  color: #409eff;
  border-bottom-color: rgba(64, 158, 255, 0.38);
}

.ai-rich-content :deep(code) {
  padding: 2px 7px;
  border-radius: 6px;
  background: #f3f6fb;
  font-size: 13px;
  color: #cc4b37;
  border: 1px solid #e6edf6;
}

.ai-rich-content :deep(del) {
  color: #8b98ab;
}

.ai-rich-content :deep(pre),
.ai-rich-content :deep(.doc-md-code) {
  margin: 0;
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(180deg, #101827 0%, #0b1320 100%);
  color: #f8fbff;
  overflow: auto;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.ai-rich-content :deep(pre code),
.ai-rich-content :deep(.doc-md-code code) {
  padding: 0;
  background: transparent;
  color: inherit;
  border: none;
}

.ai-rich-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
}

.ai-rich-content :deep(th),
.ai-rich-content :deep(td) {
  border: 1px solid #edf2f8;
  padding: 12px 14px;
  text-align: left;
  vertical-align: top;
}

.ai-rich-content :deep(th) {
  background: #f7fafe;
  font-weight: 700;
  color: #20324a;
}

.ai-rich-content :deep(strong) {
  font-weight: 700;
  color: #1f2d3d;
}

.ai-rich-content :deep(em) {
  font-style: italic;
}

.doc-drawer-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  height: 100%;
}

.doc-drawer-left,
.doc-drawer-right {
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #fff;
}

.doc-drawer-left {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.doc-drawer-toolbar {
  display: flex;
  gap: 10px;
  padding: 14px;
  border-bottom: 1px solid #f0f2f5;
}

.doc-drawer-list {
  flex: 1;
  overflow: auto;
  padding: 10px;
}

.doc-drawer-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px;
  color: #606266;
}

.doc-drawer-item {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s ease;
}

.doc-drawer-item + .doc-drawer-item {
  margin-top: 10px;
}

.doc-drawer-item:hover,
.doc-drawer-item.is-active {
  border-color: #409eff;
  background: #f5f9ff;
}

.doc-drawer-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.doc-drawer-item-meta,
.doc-drawer-preview-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.doc-drawer-right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.doc-drawer-preview-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid #f0f2f5;
}

.doc-drawer-preview-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.doc-drawer-preview-body {
  flex: 1;
  overflow: auto;
  padding: 18px;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .doc-drawer-layout {
    grid-template-columns: 1fr;
  }

  .doc-quick-entry-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1100px) {
  .readme-section-header,
  .readme-header-right,
  .readme-header-right-doc {
    flex-direction: column;
    align-items: flex-start;
  }

  .readme-stats-inline {
    justify-content: flex-start;
  }

  .readme-hero {
    grid-template-columns: 1fr;
  }

  .readme-hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .readme-shell {
    padding: 18px 16px;
  }

  .readme-hero {
    padding: 18px 16px;
  }

  .readme-title-main {
    flex-wrap: wrap;
  }

  .readme-hero-title {
    font-size: 20px;
  }

  .readme-hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
