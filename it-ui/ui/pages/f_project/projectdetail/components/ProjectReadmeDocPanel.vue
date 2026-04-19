<template>
  <el-card shadow="never" class="section-card readme-section-card project-doc-readme-panel">
    <div slot="header" class="section-header section-header-flex readme-section-header">
      <div class="readme-section-title">
        <div class="readme-title-main">
          <span>README</span>
          <el-tag size="mini" effect="plain" :type="primaryDoc ? 'success' : 'info'">
            {{ primaryDoc ? '文档中心优先' : '项目文件回退' }}
          </el-tag>
          <el-tag v-if="primaryDoc && primaryDoc.isPrimary" size="mini" type="warning" effect="plain">主 README</el-tag>
        </div>
        <div class="readme-title-sub">{{ resolvedLeadText }}</div>
      </div>

      <div class="readme-header-right readme-header-right-doc">
        <div class="readme-stats-inline">
          <span class="readme-stat-chip"><i class="el-icon-document"></i><em>{{ docCount }}</em> 文档</span>
          <span class="readme-stat-chip"><i class="el-icon-folder-opened"></i><em>{{ sourceLabel }}</em></span>
        </div>
        <el-button size="mini" type="text" icon="el-icon-tickets" @click="openDocCenter()">文档入口</el-button>
        <el-button v-if="canManageProject" size="mini" type="text" icon="el-icon-edit-outline" @click="emitOpenDocManage(primaryDoc, 'view')">管理文档</el-button>
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
            <div class="readme-hero-stat-value">{{ primaryDoc ? (primaryDoc.isPrimary ? '主 README' : statusText(primaryDoc.status)) : '回退' }}</div>
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
              <div class="doc-quick-entry-tags">
                <el-tag size="mini" effect="plain">{{ typeText(item.docType) }}</el-tag>
                <el-tag v-if="item.isPrimary" size="mini" type="warning" effect="plain">主 README</el-tag>
              </div>
            </div>
            <div class="doc-quick-entry-meta">{{ formatTime(item.updatedAt || item.createdAt) }}</div>
          </div>
        </div>
      </div>

      <div class="readme-shell">
        <div v-if="primaryDoc && primaryDocHtml" class="readme-box ai-rich-content" v-html="primaryDocHtml"></div>
        <div v-else-if="fallbackHasContent" class="readme-box ai-rich-content" v-html="fallbackHtml"></div>
        <el-empty v-else description="暂无 README 或项目文档" :image-size="72" />
      </div>
    </div>

    <el-drawer
      title="项目文档"
      :visible.sync="innerDrawerVisible"
      size="56%"
      append-to-body
      custom-class="project-doc-drawer"
    >
      <div class="doc-drawer-layout">
        <div class="doc-drawer-left">
          <div class="doc-drawer-toolbar">
            <el-input
              v-model.trim="innerKeyword"
              size="small"
              clearable
              placeholder="搜索文档标题"
            />
            <el-button size="small" icon="el-icon-refresh" @click="$emit('refresh-docs')">刷新</el-button>
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
              <div class="doc-drawer-item-title-row">
                <div class="doc-drawer-item-title">{{ item.title }}</div>
                <el-tag v-if="item.isPrimary" size="mini" type="warning" effect="plain">主 README</el-tag>
              </div>
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
                <span v-if="activeDoc.isPrimary">· 主 README</span>
              </div>
            </div>
            <div class="doc-drawer-preview-actions">
              <el-button v-if="canManageProject && activeDoc" size="mini" type="primary" plain @click="emitOpenDocManage(activeDoc, 'edit')">编辑文档</el-button>
              <el-button v-if="canManageProject" size="mini" plain @click="emitOpenDocManage(activeDoc, 'view')">进入文档中心</el-button>
            </div>
          </div>

          <div v-if="activeDocHtml" class="doc-drawer-preview-body ai-rich-content" v-html="activeDocHtml"></div>
          <el-empty v-else description="左侧选择一篇文档后可在这里预览" :image-size="70" />
        </div>
      </div>
    </el-drawer>
  </el-card>
</template>

<script>
export default {
  name: 'ProjectReadmeDocPanel',
  props: {
    projectName: { type: String, default: '' },
    canManageProject: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    docs: { type: Array, default: () => [] },
    primaryDoc: { type: Object, default: null },
    activeDoc: { type: Object, default: null },
    primaryDocHtml: { type: String, default: '' },
    activeDocHtml: { type: String, default: '' },
    fallbackHtml: { type: String, default: '' },
    fallbackHasContent: { type: Boolean, default: false },
    fallbackLeadText: { type: String, default: '' },
    drawerVisible: { type: Boolean, default: false }
  },
  data() {
    return {
      innerKeyword: ''
    }
  },
  computed: {
    innerDrawerVisible: {
      get() {
        return this.drawerVisible
      },
      set(v) {
        this.$emit('update:drawerVisible', v)
      }
    },
    hasPrimaryContent() {
      if (this.primaryDoc && this.primaryDocHtml) return true
      return !!this.fallbackHasContent
    },
    docCount() {
      return Array.isArray(this.docs) ? this.docs.length : 0
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
      return Array.isArray(this.docs) ? this.docs.slice(0, 4) : []
    },
    filteredDocs() {
      const rows = Array.isArray(this.docs) ? this.docs : []
      if (!this.innerKeyword) return rows
      const k = this.innerKeyword.toLowerCase()
      return rows.filter(item => {
        const title = String(item.title || '').toLowerCase()
        const type = String(item.docType || '').toLowerCase()
        return title.includes(k) || type.includes(k)
      })
    }
  },
  methods: {
    openDocCenter(item = null) {
      if (item) {
        this.$emit('select-doc', item)
        this.innerDrawerVisible = true
        return
      }
      if (this.primaryDoc && this.primaryDoc.id) {
        this.$emit('select-doc', this.primaryDoc)
      }
      this.innerDrawerVisible = true
    },
    emitOpenDocManage(item = null, mode = 'view') {
      const payload = { tab: 'doc-manage', mode }
      if (item && item.id) {
        payload.docId = item.id
      }
      this.$emit('open-doc-manage', payload)
    },
    selectDoc(item) {
      this.$emit('select-doc', item)
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
    typeText(v) {
      const m = { wiki: '说明文档', readme: 'README', spec: '需求规格', meeting_note: '会议纪要', design: '设计文档', manual: '使用手册', other: '其他' }
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
.readme-doc-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.readme-section-header,
.readme-header-right,
.readme-header-right-doc,
.readme-stats-inline,
.readme-hero-stats,
.doc-quick-entry-top,
.doc-drawer-item-title-row,
.doc-drawer-preview-top {
  display: flex;
  align-items: center;
}

.readme-section-header,
.readme-header-right,
.readme-header-right-doc,
.doc-drawer-preview-top {
  justify-content: space-between;
  gap: 12px;
}

.readme-stats-inline,
.readme-hero-stats,
.doc-drawer-preview-actions,
.doc-quick-entry-tags {
  gap: 8px;
  flex-wrap: wrap;
}

.readme-title-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--it-text);
  flex-wrap: wrap;
}

.readme-title-sub {
  margin-top: 8px;
  color: var(--it-text-subtle);
  font-size: 13px;
  line-height: 1.7;
  max-width: 760px;
}

.readme-stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
  color: var(--it-text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.readme-stat-chip i {
  color: var(--it-accent);
}

.readme-stat-chip em {
  font-style: normal;
  font-weight: 700;
  color: var(--it-text);
}

.readme-hero {
  display: grid;
  grid-template-columns: minmax(0,1fr) auto;
  gap: 18px;
  padding: 22px 24px;
  border-radius: 20px;
  background: var(--it-soft-gradient);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-soft);
}

.readme-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 700;
}

.readme-hero-title {
  margin-top: 12px;
  font-size: 24px;
  line-height: 1.35;
  font-weight: 700;
  color: var(--it-text);
}

.readme-hero-desc {
  margin-top: 10px;
  color: var(--it-text-muted);
  line-height: 1.85;
  font-size: 14px;
  max-width: 760px;
}

.readme-hero-stats {
  display: grid;
  grid-template-columns: repeat(2,minmax(0,1fr));
  gap: 12px;
  min-width: 320px;
}

.readme-hero-stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-surface-elevated) 94%, transparent), var(--it-surface-solid));
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-soft);
}

.readme-hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--it-text);
  line-height: 1.2;
}

.readme-hero-stat-label {
  margin-top: 6px;
  color: var(--it-text-subtle);
  font-size: 12px;
}

.doc-hero-actions {
  min-width: 360px;
}

.doc-quick-entry {
  margin: 0 0 18px;
  padding: 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 7%, var(--it-surface-elevated)), color-mix(in srgb, var(--it-success) 6%, var(--it-surface-solid)));
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-soft);
}

.doc-quick-entry-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--it-text);
}

.doc-quick-entry-list {
  display: grid;
  grid-template-columns: repeat(2,minmax(0,1fr));
  gap: 10px;
}

.doc-quick-entry-item {
  padding: 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-surface-elevated) 96%, transparent), var(--it-surface-solid));
  border: 1px solid var(--it-border);
  cursor: pointer;
  transition: all .2s ease;
}

.doc-quick-entry-item:hover,
.doc-quick-entry-item.is-active {
  border-color: var(--it-border-strong);
  box-shadow: var(--it-shadow-hover);
  transform: translateY(-1px);
}

.doc-quick-entry-name {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--it-text);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.doc-quick-entry-meta,
.doc-drawer-item-meta,
.doc-drawer-preview-meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.readme-shell {
  position: relative;
  padding: 24px 26px;
  border-radius: 20px;
  background: var(--it-panel-bg-strong);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-soft);
}

.readme-showcase.is-empty .readme-shell {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.readme-box {
  min-height: 140px;
  line-height: 1.9;
  color: var(--it-text-muted);
  white-space: normal;
  word-break: break-word;
  font-size: 15px;
}

.ai-rich-content {
  line-height: 1.9;
  color: var(--it-text);
  white-space: normal;
  word-break: break-word;
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
  border: 1px solid var(--it-border);
  border-radius: 16px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
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
  border-bottom: 1px solid var(--it-border);
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
  color: var(--it-text-muted);
}

.doc-drawer-item {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--it-border);
  cursor: pointer;
  transition: all .2s ease;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-surface-elevated) 96%, transparent), var(--it-surface-solid));
}

.doc-drawer-item + .doc-drawer-item {
  margin-top: 10px;
}

.doc-drawer-item:hover,
.doc-drawer-item.is-active {
  border-color: var(--it-border-strong);
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 8%, var(--it-surface-elevated)), var(--it-surface-solid));
}

.doc-drawer-item-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--it-text);
}

.doc-drawer-right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.doc-drawer-preview-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.doc-drawer-preview-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--it-text);
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
    grid-template-columns: repeat(4,minmax(0,1fr));
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
    grid-template-columns: repeat(2,minmax(0,1fr));
  }
}
</style>

