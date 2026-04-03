<template>
  <div class="project-doc-manage">
    <el-card shadow="never">
      <div slot="header" class="card-header">
        <span>文档管理</span>
        <div class="toolbar-actions">
          <el-input v-model.trim="filters.keyword" size="small" clearable placeholder="搜索标题或正文" class="toolbar-input" @clear="loadDocs" @keyup.enter.native="loadDocs"></el-input>
          <el-select v-model="filters.type" size="small" clearable placeholder="文档类型" @change="loadDocs">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
          <el-select v-model="filters.status" size="small" clearable placeholder="状态" @change="loadDocs">
            <el-option label="草稿" value="draft"></el-option>
            <el-option label="已发布" value="published"></el-option>
            <el-option label="已归档" value="archived"></el-option>
          </el-select>
          <el-select v-model="filters.visibility" size="small" clearable placeholder="可见性" @change="loadDocs">
            <el-option label="项目内" value="project"></el-option>
            <el-option label="团队" value="team"></el-option>
            <el-option label="仅自己" value="private"></el-option>
          </el-select>
          <el-select v-model="filters.isPrimary" size="small" clearable placeholder="主文档" @change="loadDocs">
            <el-option label="仅主 README" value="true"></el-option>
            <el-option label="非主 README" value="false"></el-option>
          </el-select>
          <el-button size="small" icon="el-icon-search" @click="loadDocs">查询</el-button>
          <el-button type="danger" size="small" icon="el-icon-delete" :disabled="!selectedDocRows.length" @click="handleBatchDelete">
            批量删除{{ selectedDocRows.length ? "（" + selectedDocRows.length + "）" : "" }}
          </el-button>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate">新建文档</el-button>
          <el-button size="small" icon="el-icon-refresh" @click="loadDocs">刷新</el-button>
        </div>
      </div>

      <div class="doc-layout">
        <div class="doc-table-wrap">
          <el-table ref="docTableRef" v-loading="loading" :data="docs" border height="560" highlight-current-row @current-change="handleCurrentChange" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center"></el-table-column>
            <el-table-column prop="title" label="标题" min-width="240">
              <template slot-scope="scope">
                <div class="doc-title-cell">
                  <span class="doc-title-link" @click="openPreview(scope.row)">{{ scope.row.title }}</span>
                  <el-tag v-if="scope.row.isPrimary" size="mini" type="warning">主 README</el-tag>
                  <el-tag v-else-if="scope.row.readmeCandidate" size="mini" type="success">README候选</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="docType" label="类型" width="120">
              <template slot-scope="scope">{{ typeText(scope.row.docType) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template slot-scope="scope">
                <el-tag size="mini" :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="visibility" label="可见性" width="100">
              <template slot-scope="scope">{{ visibilityText(scope.row.visibility) }}</template>
            </el-table-column>
            <el-table-column prop="currentVersion" label="版本" width="90">
              <template slot-scope="scope">v{{ scope.row.currentVersion || 1 }}</template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="170">
              <template slot-scope="scope">{{ formatTime(scope.row.updatedAt || scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="openPreview(scope.row)">查看</el-button>
                <el-button type="text" size="mini" @click="openEdit(scope.row)">编辑</el-button>
                <el-button type="text" size="mini" @click="openHistory(scope.row)">历史</el-button>
                <el-button type="text" size="mini" @click="handleSetPrimary(scope.row)">{{ scope.row.isPrimary ? '当前主文档' : '设为 README' }}</el-button>
                <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="doc-preview-wrap">
          <div class="preview-card">
            <div class="preview-top">
              <div>
                <div class="preview-title-row">
                  <div class="preview-title">{{ currentDoc ? currentDoc.title : '文档预览' }}</div>
                  <el-tag v-if="currentDoc && currentDoc.isPrimary" size="mini" type="warning">主 README</el-tag>
                </div>
                <div class="preview-meta" v-if="currentDoc">
                  <span>{{ typeText(currentDoc.docType) }}</span>
                  <span>·</span>
                  <span>{{ statusText(currentDoc.status) }}</span>
                  <span>·</span>
                  <span>v{{ currentDoc.currentVersion || 1 }}</span>
                </div>
              </div>
              <el-button v-if="currentDoc" size="mini" @click="openEdit(currentDoc)">编辑当前文档</el-button>
            </div>
            <pre class="preview-content">{{ currentDoc ? currentDoc.content : '左侧选择一篇文档后，这里会显示正文内容。' }}</pre>
          </div>
        </div>
      </div>
    </el-card>

    <ProjectDocEditor
      :visible.sync="editorVisible"
      :mode="editorMode"
      :project-id="projectId"
      :doc="editingDoc"
      @saved="handleSaved"
    />

    <ProjectDocHistory
      :visible.sync="historyVisible"
      :doc="historyDoc"
      @rolled-back="handleRolledBack"
    />
  </div>
</template>

<script>
import {
  listProjectDocs,
  getProjectDoc,
  deleteProjectDoc,
  setPrimaryProjectDoc
} from '@/api/projectDoc'
import ProjectDocEditor from './ProjectDocEditor.vue'
import ProjectDocHistory from './ProjectDocHistory.vue'

export default {
  name: 'ProjectDocList',
  components: {
    ProjectDocEditor,
    ProjectDocHistory
  },
  props: {
    projectId: { type: [Number, String], required: true },
    initialDocId: { type: [Number, String], default: null },
    initialMode: { type: String, default: 'view' }
  },
  data() {
    return {
      loading: false,
      docs: [],
      currentDoc: null,
      editorVisible: false,
      editorMode: 'create',
      editingDoc: null,
      historyVisible: false,
      historyDoc: null,
      selectedDocRows: [],
      initialActionConsumed: false,
      filters: {
        keyword: '',
        type: '',
        status: '',
        visibility: '',
        isPrimary: ''
      },
      typeOptions: [
        { label: 'README', value: 'readme' },
        { label: '说明文档', value: 'wiki' },
        { label: '需求规格', value: 'spec' },
        { label: '会议纪要', value: 'meeting_note' },
        { label: '设计文档', value: 'design' },
        { label: '使用手册', value: 'manual' },
        { label: '其他', value: 'other' }
      ]
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler(v) {
        if (v !== null && v !== undefined && String(v) !== '') {
          this.initialActionConsumed = false
          this.loadDocs()
        }
      }
    },
    initialDocId() {
      this.initialActionConsumed = false
      if (this.docs.length) {
        this.applyInitialAction()
      }
    },
    initialMode() {
      this.initialActionConsumed = false
      if (this.docs.length) {
        this.applyInitialAction()
      }
    }
  },
  methods: {
    typeText(v) {
      const m = { readme: 'README', wiki: '说明文档', spec: '需求规格', meeting_note: '会议纪要', design: '设计文档', manual: '使用手册', other: '其他' }
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
    statusTagType(v) {
      const m = { draft: 'info', published: 'success', archived: '' }
      return m[v] || 'info'
    },
    formatTime(v) {
      if (!v) return '-'
      const d = new Date(v)
      if (Number.isNaN(d.getTime())) return v
      const p = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    normalizeDoc(item = {}) {
      return {
        ...item,
        isPrimary: !!item.isPrimary,
        readmeCandidate: !!item.readmeCandidate
      }
    },
    async loadDocs() {
      if (this.projectId === null || this.projectId === undefined || String(this.projectId) === '') return
      try {
        this.loading = true
        const params = {}
        if (this.filters.keyword) params.keyword = this.filters.keyword
        if (this.filters.type) params.type = this.filters.type
        if (this.filters.status) params.status = this.filters.status
        if (this.filters.visibility) params.visibility = this.filters.visibility
        if (this.filters.isPrimary !== '') params.isPrimary = this.filters.isPrimary
        const res = await listProjectDocs(this.projectId, params)
        const rows = Array.isArray(res?.data) ? res.data : []
        this.docs = rows.map(this.normalizeDoc)
        this.clearDocSelection()
        this.$emit('count-change', this.docs.length)
        const applied = await this.applyInitialAction()
        if (applied) return
        if (this.currentDoc && this.currentDoc.id) {
          const found = this.docs.find(item => Number(item.id) === Number(this.currentDoc.id))
          if (found) {
            await this.openPreview(found, false)
            return
          }
        }
        const primary = this.docs.find(item => item.isPrimary)
        if (primary) {
          await this.openPreview(primary, false)
        } else if (this.docs.length) {
          await this.openPreview(this.docs[0], false)
        } else {
          this.currentDoc = null
        }
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '加载文档列表失败'
        this.$message.error(m)
      } finally {
        this.loading = false
      }
    },
    async applyInitialAction() {
      if (this.initialActionConsumed || !this.initialDocId) return false
      const row = this.docs.find(item => Number(item.id) === Number(this.initialDocId))
      if (!row) return false
      this.initialActionConsumed = true
      const mode = String(this.initialMode || 'view').toLowerCase()
      if (mode === 'edit') {
        await this.openEdit(row)
      } else if (mode === 'history') {
        this.openHistory(row)
      } else {
        await this.openPreview(row, false)
      }
      return true
    },
    handleCurrentChange(row) {
      if (row) {
        this.openPreview(row, false)
      }
    },
    async openPreview(row, notify = false) {
      if (!row || !row.id) return
      try {
        const res = await getProjectDoc(row.id)
        this.currentDoc = this.normalizeDoc(res?.data || null)
        if (notify) {
          this.$message.success('已加载文档详情')
        }
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '加载文档详情失败'
        this.$message.error(m)
      }
    },
    openCreate() {
      this.editorMode = 'create'
      this.editingDoc = null
      this.editorVisible = true
    },
    async openEdit(row) {
      await this.openPreview(row, false)
      this.editorMode = 'edit'
      this.editingDoc = this.currentDoc ? { ...this.currentDoc } : null
      this.editorVisible = true
    },
    openHistory(row) {
      this.historyDoc = row || this.currentDoc
      this.historyVisible = true
    },
    async handleSaved(saved) {
      await this.loadDocs()
      if (saved && saved.id) {
        const row = this.docs.find(item => Number(item.id) === Number(saved.id))
        if (row) {
          await this.openPreview(row, false)
        }
      }
      this.$emit('changed')
      this.$emit('primary-changed')
    },
    async handleRolledBack() {
      await this.loadDocs()
      if (this.historyDoc && this.historyDoc.id) {
        const row = this.docs.find(item => Number(item.id) === Number(this.historyDoc.id))
        if (row) {
          this.historyDoc = row
          await this.openPreview(row, false)
        }
      }
      this.$emit('changed')
      this.$emit('primary-changed')
    },
    async handleSetPrimary(row) {
      if (!row || !row.id) return
      if (row.isPrimary) {
        this.$message.info('当前文档已经是主 README')
        return
      }
      try {
        await setPrimaryProjectDoc(row.id)
        this.$message.success('已设为主 README')
        await this.loadDocs()
        const target = this.docs.find(item => Number(item.id) === Number(row.id))
        if (target) {
          await this.openPreview(target, false)
        }
        this.$emit('changed')
        this.$emit('primary-changed')
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '设置主 README 失败'
        this.$message.error(m)
      }
    },
    handleSelectionChange(rows) {
      this.selectedDocRows = Array.isArray(rows) ? rows.slice() : []
    },
    clearDocSelection() {
      this.selectedDocRows = []
      this.$nextTick(() => {
        if (this.$refs.docTableRef && this.$refs.docTableRef.clearSelection) {
          this.$refs.docTableRef.clearSelection()
        }
      })
    },
    async handleBatchDelete() {
      if (!this.selectedDocRows.length) {
        this.$message.warning('请先勾选要删除的文档')
        return
      }
      const rows = this.selectedDocRows.slice()
      try {
        await this.$confirm(`确定批量删除选中的 ${rows.length} 篇文档吗？此操作不可恢复。`, '提示', { type: 'warning' })
        const results = await Promise.allSettled(rows.map(item => deleteProjectDoc(item.id)))
        const successIds = rows.filter((_, index) => results[index] && results[index].status === 'fulfilled').map(item => Number(item.id))
        const successCount = successIds.length
        const failCount = rows.length - successCount
        if (this.currentDoc && successIds.includes(Number(this.currentDoc.id))) {
          this.currentDoc = null
        }
        await this.loadDocs()
        this.clearDocSelection()
        this.$emit('changed')
        if (successCount > 0) {
          this.$message.success(`已删除 ${successCount} 篇文档${failCount ? `，失败 ${failCount} 篇` : ''}`)
        } else {
          this.$message.error('批量删除失败')
        }
      } catch (e) {
        if (e !== 'cancel') {
          const m = e.response?.data?.message || e.response?.data?.msg || '批量删除失败'
          this.$message.error(m)
        }
      }
    },
    handleDelete(row) {
      if (!row || !row.id) return
      this.$confirm(`确定删除文档《${row.title}》吗？`, '提示', { type: 'warning' }).then(async () => {
        try {
          await deleteProjectDoc(row.id)
          this.$message.success('删除成功')
          if (this.currentDoc && Number(this.currentDoc.id) === Number(row.id)) {
            this.currentDoc = null
          }
          await this.loadDocs()
          this.$emit('changed')
          this.$emit('primary-changed')
        } catch (e) {
          const m = e.response?.data?.message || e.response?.data?.msg || '删除失败'
          this.$message.error(m)
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-input { width: 220px; }
.doc-layout { display: grid; grid-template-columns: minmax(0, 1.45fr) minmax(320px, 1fr); gap: 16px; }
.doc-title-cell { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.doc-title-link { cursor: pointer; color: #409eff; }
.doc-title-link:hover { text-decoration: underline; }
.preview-card { border: 1px solid #ebeef5; border-radius: 6px; min-height: 560px; background: #fafafa; display: flex; flex-direction: column; }
.preview-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 14px; border-bottom: 1px solid #ebeef5; }
.preview-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.preview-title { font-size: 16px; font-weight: 600; }
.preview-meta { margin-top: 6px; display: flex; gap: 6px; color: #909399; font-size: 12px; flex-wrap: wrap; }
.preview-content { margin: 0; padding: 14px; flex: 1; overflow: auto; white-space: pre-wrap; word-break: break-word; font-family: Consolas, Monaco, monospace; line-height: 1.65; }
.danger-text { color: #f56c6c; }
@media (max-width: 1200px) { .doc-layout { grid-template-columns: 1fr; } }
</style>
