<template>
  <div class="conflict-diff-editor" v-loading="loading || monacoLoading">
    <div class="editor-topbar">
      <div class="editor-title-group">
        <div class="editor-title">内容冲突编辑器</div>
        <div class="editor-subtitle">
          默认对比 Source 与 Target，最终合并内容可手工编辑后保存。
        </div>
      </div>
      <div class="editor-actions">
        <el-button size="mini" plain :disabled="loading || saving" @click="$emit('refresh')">
          重新加载
        </el-button>
        <el-button
          size="mini"
          plain
          :disabled="actionDisabled"
          @click="useSourceContent"
        >
          {{ sourceActionLabel }}
        </el-button>
        <el-button
          size="mini"
          plain
          :disabled="actionDisabled"
          @click="useTargetContent"
        >
          {{ targetActionLabel }}
        </el-button>
        <el-button
          size="mini"
          type="primary"
          :loading="saving"
          :disabled="saveDisabled"
          @click="emitSave"
        >
          保存合并结果
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="errorText"
      :title="errorText"
      type="error"
      :closable="false"
      show-icon
      class="editor-alert"
    />

    <el-empty v-if="!loading && !hasDetail" description="请选择一个内容冲突，或点击重新加载内容详情。" :image-size="80" />

    <div v-else-if="isBinaryConflict" class="binary-state">
      <el-alert
        title="当前文件被识别为二进制内容，禁止在线编辑。"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="metadata-grid">
        <div v-for="item in metadataRows" :key="item.label" class="metadata-item">
          <span class="metadata-label">{{ item.label }}</span>
          <span class="metadata-value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="hasDetail" class="editor-body">
      <div class="metadata-row">
        <el-tag size="mini" effect="plain">{{ displayPath }}</el-tag>
        <el-tag size="mini" effect="plain">{{ languageLabel }}</el-tag>
        <el-tag v-if="metadata.encoding" size="mini" effect="plain">{{ metadata.encoding }}</el-tag>
        <el-tag v-if="metadata.sizeLabel" size="mini" effect="plain">{{ metadata.sizeLabel }}</el-tag>
      </div>

      <el-collapse v-model="basePanels" class="base-collapse" @change="layoutEditors">
        <el-collapse-item name="base">
          <template slot="title">
            <span class="base-title">Base 内容（可折叠）</span>
          </template>
          <pre class="base-content">{{ baseContent || '接口未返回 base content' }}</pre>
        </el-collapse-item>
      </el-collapse>

      <ConflictBlockNavigator
        :blocks="normalizedBlocks"
        :active-index="activeBlockIndex"
        :disabled="saving"
        @previous="goPreviousBlock"
        @next="goNextBlock"
        @select="selectBlock"
      />

      <div v-if="editorError" class="fallback-editor">
        <el-alert
          :title="editorError"
          type="warning"
          :closable="false"
          show-icon
          class="editor-alert"
        />
        <div class="fallback-grid">
          <div class="fallback-pane">
            <div class="pane-label">Source</div>
            <el-input :value="sourceContent" type="textarea" :rows="10" readonly />
          </div>
          <div class="fallback-pane">
            <div class="pane-label">Target</div>
            <el-input :value="targetContent" type="textarea" :rows="10" readonly />
          </div>
        </div>
        <div class="fallback-pane final">
          <div class="pane-label">Final merged content</div>
          <el-input
            v-model="internalContent"
            type="textarea"
            :rows="14"
            :disabled="saving"
          />
        </div>
      </div>

      <div v-else class="monaco-shell">
        <div class="pane-card">
          <div class="pane-head">
            <span>Source vs Target</span>
            <el-tag size="mini" type="warning" effect="plain">Diff</el-tag>
          </div>
          <div ref="diffEditor" class="diff-editor"></div>
        </div>

        <div class="pane-card final-pane">
          <div class="pane-head">
            <span>Final merged content</span>
            <el-tag size="mini" type="success" effect="plain">Editable</el-tag>
          </div>
          <div ref="finalEditor" class="final-editor"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ConflictBlockNavigator from './ConflictBlockNavigator.vue'

const EXTENSION_LANGUAGE_MAP = {
  css: 'css',
  go: 'go',
  htm: 'html',
  html: 'html',
  java: 'java',
  js: 'javascript',
  json: 'json',
  less: 'less',
  md: 'markdown',
  py: 'python',
  scss: 'scss',
  sh: 'shell',
  sql: 'sql',
  ts: 'typescript',
  vue: 'html',
  xml: 'xml',
  yaml: 'yaml',
  yml: 'yaml'
}

export default {
  name: 'ConflictDiffEditor',
  components: {
    ConflictBlockNavigator
  },
  props: {
    conflict: {
      type: Object,
      default: null
    },
    detail: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    },
    saving: {
      type: Boolean,
      default: false
    },
    errorText: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      activeBlockIndex: 0,
      basePanels: [],
      diffEditor: null,
      editorError: '',
      finalEditor: null,
      finalModel: null,
      finalDecorations: [],
      internalContent: '',
      monaco: null,
      monacoLoading: false,
      sourceDecorations: [],
      sourceModel: null,
      suppressChange: false,
      targetDecorations: [],
      targetModel: null
    }
  },
  computed: {
    hasDetail() {
      return !!(this.detail && typeof this.detail === 'object')
    },
    metadata() {
      const detail = this.detail && typeof this.detail === 'object' ? this.detail : {}
      const metadata = detail.metadata && typeof detail.metadata === 'object' ? detail.metadata : {}
      const result = { ...metadata }
      if (!result.language) result.language = this.detectLanguage(this.displayPath)
      if (!result.encoding && detail.encoding) result.encoding = detail.encoding
      if (!result.sizeLabel && (metadata.size || metadata.fileSize || detail.size || detail.fileSize)) {
        result.sizeLabel = this.formatSize(metadata.size || metadata.fileSize || detail.size || detail.fileSize)
      }
      return result
    },
    metadataRows() {
      return [
        { label: '文件路径', value: this.displayPath || '-' },
        { label: '语言', value: this.languageLabel },
        { label: '编码', value: this.metadata.encoding || '-' },
        { label: '大小', value: this.metadata.sizeLabel || '-' }
      ]
    },
    currentConflictId() {
      const detail = this.detail && typeof this.detail === 'object' ? this.detail : {}
      const conflict = this.conflict && typeof this.conflict === 'object' ? this.conflict : {}
      return String(detail.conflictId || conflict.conflictId || conflict.id || '')
    },
    displayPath() {
      const metadata = this.detail && this.detail.metadata && typeof this.detail.metadata === 'object'
        ? this.detail.metadata
        : {}
      const conflict = this.conflict && typeof this.conflict === 'object' ? this.conflict : {}
      return String(metadata.filePath || metadata.path || conflict.filePath || conflict.path || conflict.sourcePath || conflict.targetPath || conflict.basePath || '')
    },
    language() {
      const raw = String(this.metadata.language || '').trim().toLowerCase()
      return raw || 'plaintext'
    },
    languageLabel() {
      return this.language === 'plaintext' ? 'plain text' : this.language
    },
    baseContent() {
      return this.textValue(this.detail && this.detail.baseContent)
    },
    sourceContent() {
      return this.textValue(this.detail && this.detail.sourceContent)
    },
    targetContent() {
      return this.textValue(this.detail && this.detail.targetContent)
    },
    normalizedBlocks() {
      const rows = this.detail && Array.isArray(this.detail.blocks) ? this.detail.blocks : []
      return rows.map((block, index) => this.normalizeBlock(block, index))
    },
    currentBlock() {
      return this.normalizedBlocks[this.activeBlockIndex] || null
    },
    isBinaryConflict() {
      const detail = this.detail && typeof this.detail === 'object' ? this.detail : {}
      const metadata = this.metadata || {}
      const binaryFlags = [
        detail.binary,
        detail.isBinary,
        detail.binaryFile,
        metadata.binary,
        metadata.isBinary,
        metadata.binaryFile
      ]
      if (binaryFlags.some(Boolean)) return true
      const typeText = String(metadata.contentType || metadata.mimeType || detail.contentType || detail.mimeType || '').toLowerCase()
      if (typeText && !typeText.startsWith('text/') && !typeText.includes('json') && !typeText.includes('xml')) return true
      const encoding = String(metadata.encoding || detail.encoding || '').toLowerCase()
      return encoding === 'binary' || encoding === 'base64'
    },
    actionDisabled() {
      return this.loading || this.saving || !this.hasDetail || this.isBinaryConflict
    },
    saveDisabled() {
      return this.actionDisabled
    },
    sourceActionLabel() {
      return this.currentBlock ? '采用当前块 Source' : '采用 Source 全文'
    },
    targetActionLabel() {
      return this.currentBlock ? '采用当前块 Target' : '采用 Target 全文'
    }
  },
  watch: {
    detail: {
      immediate: true,
      handler() {
        this.syncContentFromDetail()
        this.$nextTick(() => {
          this.refreshMonaco()
        })
      }
    },
    conflict() {
      this.activeBlockIndex = 0
    },
    saving(value) {
      if (this.finalEditor) {
        this.finalEditor.updateOptions({ readOnly: !!value })
      }
    },
    activeBlockIndex() {
      this.applyBlockDecorations()
    },
    normalizedBlocks() {
      if (this.activeBlockIndex >= this.normalizedBlocks.length) {
        this.activeBlockIndex = Math.max(this.normalizedBlocks.length - 1, 0)
      }
      this.$nextTick(() => this.applyBlockDecorations())
    }
  },
  mounted() {
    this.refreshMonaco()
  },
  beforeDestroy() {
    this.disposeEditors()
  },
  methods: {
    textValue(value) {
      return typeof value === 'string' ? value : ''
    },
    detectLanguage(path) {
      const raw = String(path || '')
      const ext = raw.includes('.') ? raw.split('.').pop().toLowerCase() : ''
      return EXTENSION_LANGUAGE_MAP[ext] || 'plaintext'
    },
    formatSize(value) {
      const bytes = Number(value)
      if (!Number.isFinite(bytes) || bytes < 0) return String(value || '')
      if (bytes < 1024) return `${bytes} B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
      return `${(bytes / 1024 / 1024).toFixed(1)} MB`
    },
    firstPositiveNumber(values, fallback) {
      for (let index = 0; index < values.length; index += 1) {
        const number = Number(values[index])
        if (Number.isFinite(number) && number > 0) return number
      }
      return fallback
    },
    normalizeBlock(block, index) {
      const item = block && typeof block === 'object' ? { ...block } : {}
      const start = this.firstPositiveNumber([item.startLine, item.lineStart, item.start], index + 1)
      const end = this.firstPositiveNumber([item.endLine, item.lineEnd, item.end], start)
      item.sourceStartLine = this.firstPositiveNumber([item.sourceStartLine, item.sourceStart, item.sourceLineStart, item.leftStartLine, item.leftStart, start], start)
      item.sourceEndLine = this.firstPositiveNumber([item.sourceEndLine, item.sourceEnd, item.sourceLineEnd, item.leftEndLine, item.leftEnd, end], item.sourceStartLine)
      item.targetStartLine = this.firstPositiveNumber([item.targetStartLine, item.targetStart, item.targetLineStart, item.rightStartLine, item.rightStart, start], start)
      item.targetEndLine = this.firstPositiveNumber([item.targetEndLine, item.targetEnd, item.targetLineEnd, item.rightEndLine, item.rightEnd, end], item.targetStartLine)
      item.finalStartLine = this.firstPositiveNumber([item.finalStartLine, item.resolvedStartLine, item.mergedStartLine, item.targetStartLine, item.sourceStartLine, start], item.targetStartLine)
      item.finalEndLine = this.firstPositiveNumber([item.finalEndLine, item.resolvedEndLine, item.mergedEndLine, item.targetEndLine, item.sourceEndLine, end], item.finalStartLine)
      item.sourceText = this.blockText(item, 'source')
      item.targetText = this.blockText(item, 'target')
      return item
    },
    blockText(block, side) {
      const contentKey = `${side}Content`
      const linesKey = `${side}Lines`
      if (typeof block[contentKey] === 'string') return block[contentKey]
      if (Array.isArray(block[linesKey])) return block[linesKey].map(item => (item == null ? '' : String(item))).join('\n')
      const fullText = side === 'source' ? this.sourceContent : this.targetContent
      const start = side === 'source' ? block.sourceStartLine : block.targetStartLine
      const end = side === 'source' ? block.sourceEndLine : block.targetEndLine
      return this.sliceLineRange(fullText, start, end)
    },
    sliceLineRange(content, startLine, endLine) {
      const lines = String(content || '').replace(/\r\n/g, '\n').split('\n')
      const start = Math.max(0, (Number(startLine) || 1) - 1)
      const end = Math.max(start, Number(endLine) || Number(startLine) || 1)
      return lines.slice(start, end).join('\n')
    },
    syncContentFromDetail() {
      this.activeBlockIndex = 0
      this.internalContent = this.initialResolvedContent()
    },
    initialResolvedContent() {
      const detail = this.detail && typeof this.detail === 'object' ? this.detail : {}
      if (typeof detail.resolvedContent === 'string') return detail.resolvedContent
      if (typeof detail.finalContent === 'string') return detail.finalContent
      if (typeof detail.sourceContent === 'string') return detail.sourceContent
      if (typeof detail.targetContent === 'string') return detail.targetContent
      if (typeof detail.baseContent === 'string') return detail.baseContent
      return ''
    },
    async refreshMonaco() {
      if (!process.client || !this.hasDetail || this.isBinaryConflict || this.editorError) {
        if (this.isBinaryConflict || !this.hasDetail) this.disposeEditors()
        return
      }

      try {
        await this.ensureMonaco()
        this.createOrUpdateEditors()
        this.applyBlockDecorations()
      } catch (error) {
        this.editorError = this.extractEditorError(error)
        this.disposeEditors()
      }
    },
    async ensureMonaco() {
      if (this.monaco) return
      if (!this.$loadMonaco) {
        throw new Error('Monaco client plugin is not available')
      }
      this.monacoLoading = true
      try {
        this.monaco = await this.$loadMonaco()
      } finally {
        this.monacoLoading = false
      }
    },
    createOrUpdateEditors() {
      if (!this.monaco || !this.$refs.diffEditor || !this.$refs.finalEditor) return
      this.ensureModels()

      if (!this.diffEditor) {
        this.diffEditor = this.monaco.editor.createDiffEditor(this.$refs.diffEditor, {
          automaticLayout: true,
          glyphMargin: true,
          minimap: { enabled: false },
          originalEditable: false,
          readOnly: true,
          renderSideBySide: true,
          scrollBeyondLastLine: false
        })
        this.diffEditor.setModel({
          original: this.sourceModel,
          modified: this.targetModel
        })
      }

      if (!this.finalEditor) {
        this.finalEditor = this.monaco.editor.create(this.$refs.finalEditor, {
          automaticLayout: true,
          glyphMargin: true,
          minimap: { enabled: false },
          readOnly: !!this.saving,
          scrollBeyondLastLine: false,
          wordWrap: 'on'
        })
        this.finalEditor.setModel(this.finalModel)
        this.finalEditor.onDidChangeModelContent(() => {
          if (this.suppressChange || !this.finalModel) return
          this.internalContent = this.finalModel.getValue()
        })
      }

      this.layoutEditors()
    },
    ensureModels() {
      const language = this.language || 'plaintext'
      if (!this.sourceModel) {
        this.sourceModel = this.monaco.editor.createModel(this.sourceContent, language, this.modelUri('source'))
      }
      if (!this.targetModel) {
        this.targetModel = this.monaco.editor.createModel(this.targetContent, language, this.modelUri('target'))
      }
      if (!this.finalModel) {
        this.finalModel = this.monaco.editor.createModel(this.internalContent, language, this.modelUri('final'))
      }

      this.setModelValue(this.sourceModel, this.sourceContent)
      this.setModelValue(this.targetModel, this.targetContent)
      this.setModelValue(this.finalModel, this.internalContent)
      this.monaco.editor.setModelLanguage(this.sourceModel, language)
      this.monaco.editor.setModelLanguage(this.targetModel, language)
      this.monaco.editor.setModelLanguage(this.finalModel, language)
    },
    modelUri(side) {
      const safeId = String(this.currentConflictId || Date.now()).replace(/[^a-zA-Z0-9_-]/g, '_')
      const ext = this.language && this.language !== 'plaintext' ? this.language : 'txt'
      return this.monaco.Uri.parse(`inmemory://project-merge/${safeId}/${side}.${ext}`)
    },
    setModelValue(model, value) {
      if (!model || model.getValue() === value) return
      this.suppressChange = true
      model.setValue(value)
      this.suppressChange = false
    },
    extractEditorError(error) {
      const message = error && error.message ? error.message : String(error || '')
      return `Monaco 加载失败，已切换为文本兜底编辑器：${message || '未知错误'}`
    },
    disposeEditors() {
      if (this.diffEditor) {
        this.diffEditor.dispose()
        this.diffEditor = null
      }
      if (this.finalEditor) {
        this.finalEditor.dispose()
        this.finalEditor = null
      }
      ;[this.sourceModel, this.targetModel, this.finalModel].forEach(model => {
        if (model && !model.isDisposed()) model.dispose()
      })
      this.sourceModel = null
      this.targetModel = null
      this.finalModel = null
      this.sourceDecorations = []
      this.targetDecorations = []
      this.finalDecorations = []
    },
    layoutEditors() {
      this.$nextTick(() => {
        if (this.diffEditor) this.diffEditor.layout()
        if (this.finalEditor) this.finalEditor.layout()
      })
    },
    goPreviousBlock() {
      if (this.activeBlockIndex <= 0) return
      this.activeBlockIndex -= 1
    },
    goNextBlock() {
      if (this.activeBlockIndex >= this.normalizedBlocks.length - 1) return
      this.activeBlockIndex += 1
    },
    selectBlock(index) {
      const next = Number(index)
      if (!Number.isFinite(next)) return
      this.activeBlockIndex = Math.max(0, Math.min(next, this.normalizedBlocks.length - 1))
    },
    applyBlockDecorations() {
      if (!this.monaco || !this.currentBlock) return
      const sourceEditor = this.diffEditor && this.diffEditor.getOriginalEditor()
      const targetEditor = this.diffEditor && this.diffEditor.getModifiedEditor()

      if (sourceEditor && this.sourceModel) {
        this.sourceDecorations = sourceEditor.deltaDecorations(this.sourceDecorations, [
          this.lineDecoration(this.currentBlock.sourceStartLine, this.currentBlock.sourceEndLine, 'current-source-block', 'current-block-glyph')
        ])
        sourceEditor.revealLineInCenter(this.currentBlock.sourceStartLine)
      }

      if (targetEditor && this.targetModel) {
        this.targetDecorations = targetEditor.deltaDecorations(this.targetDecorations, [
          this.lineDecoration(this.currentBlock.targetStartLine, this.currentBlock.targetEndLine, 'current-target-block', 'current-block-glyph')
        ])
        targetEditor.revealLineInCenter(this.currentBlock.targetStartLine)
      }

      if (this.finalEditor && this.finalModel) {
        this.finalDecorations = this.finalEditor.deltaDecorations(this.finalDecorations, [
          this.lineDecoration(this.currentBlock.finalStartLine, this.currentBlock.finalEndLine, 'current-final-block', 'current-block-glyph')
        ])
        this.finalEditor.revealLineInCenter(this.currentBlock.finalStartLine)
      }
    },
    lineDecoration(start, end, className, glyphClassName) {
      const from = Math.max(1, Number(start) || 1)
      const to = Math.max(from, Number(end) || from)
      return {
        range: new this.monaco.Range(from, 1, to, 1),
        options: {
          className,
          glyphMarginClassName: glyphClassName,
          isWholeLine: true,
          overviewRuler: {
            color: '#f59e0b',
            position: this.monaco.editor.OverviewRulerLane.Center
          }
        }
      }
    },
    useSourceContent() {
      this.applyContentChoice('source')
    },
    useTargetContent() {
      this.applyContentChoice('target')
    },
    applyContentChoice(side) {
      if (this.actionDisabled) return
      const fullText = side === 'source' ? this.sourceContent : this.targetContent
      const block = this.currentBlock
      if (!block) {
        this.setFinalContent(fullText)
        return
      }
      const blockText = side === 'source' ? block.sourceText : block.targetText
      this.setFinalContent(this.replaceLineRange(this.internalContent, block.finalStartLine, block.finalEndLine, blockText))
    },
    replaceLineRange(content, startLine, endLine, replacement) {
      const lines = String(content || '').replace(/\r\n/g, '\n').split('\n')
      const start = Math.max(0, (Number(startLine) || 1) - 1)
      const end = Math.max(start, Number(endLine) || Number(startLine) || 1)
      const replacementLines = String(replacement || '').replace(/\r\n/g, '\n').split('\n')
      lines.splice(start, end - start, ...replacementLines)
      return lines.join('\n')
    },
    setFinalContent(value) {
      this.internalContent = String(value || '')
      if (this.finalModel) {
        this.setModelValue(this.finalModel, this.internalContent)
      }
      this.$nextTick(() => this.applyBlockDecorations())
    },
    emitSave() {
      if (this.saveDisabled) return
      const content = this.finalModel ? this.finalModel.getValue() : this.internalContent
      this.$emit('save', {
        conflict: this.conflict,
        conflictId: this.currentConflictId,
        resolvedContent: content
      })
    }
  }
}
</script>

<style scoped>
.conflict-diff-editor {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.editor-title-group {
  min-width: 0;
}

.editor-title {
  font-size: 16px;
  color: #0f172a;
  font-weight: 700;
}

.editor-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.editor-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.editor-alert {
  margin-bottom: 2px;
}

.editor-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metadata-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metadata-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metadata-item {
  border: 1px solid #dbe7f7;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metadata-label {
  font-size: 12px;
  color: #64748b;
}

.metadata-value {
  font-size: 13px;
  color: #0f172a;
  word-break: break-word;
}

.base-collapse {
  border: 1px solid #dbe7f7;
  border-radius: 14px;
  overflow: hidden;
  background: #ffffff;
}

.base-title {
  padding-left: 12px;
  color: #0f172a;
  font-weight: 700;
  font-size: 13px;
}

.base-content {
  margin: 0;
  max-height: 240px;
  overflow: auto;
  padding: 12px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.binary-state {
  border: 1px solid #fed7aa;
  background: #fff7ed;
  border-radius: 14px;
  padding: 12px;
}

.monaco-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pane-card {
  border: 1px solid #dbe7f7;
  border-radius: 14px;
  background: #ffffff;
  overflow: hidden;
}

.pane-head {
  min-height: 42px;
  padding: 10px 12px;
  border-bottom: 1px solid #e5ecf6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.diff-editor {
  height: 360px;
}

.final-editor {
  height: 420px;
}

.fallback-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fallback-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.fallback-pane {
  border: 1px solid #dbe7f7;
  border-radius: 14px;
  background: #ffffff;
  padding: 12px;
}

.fallback-pane.final {
  border-color: #bfdbfe;
}

.pane-label {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
}

/deep/ .current-source-block {
  background: rgba(59, 130, 246, 0.14);
}

/deep/ .current-target-block {
  background: rgba(245, 158, 11, 0.16);
}

/deep/ .current-final-block {
  background: rgba(16, 185, 129, 0.16);
}

/deep/ .current-block-glyph {
  background: #f59e0b;
  width: 4px !important;
  margin-left: 3px;
}

@media (max-width: 1200px) {
  .editor-topbar {
    flex-direction: column;
  }

  .editor-actions {
    justify-content: flex-start;
  }

  .fallback-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .metadata-grid {
    grid-template-columns: 1fr;
  }

  .diff-editor,
  .final-editor {
    height: 320px;
  }
}
</style>
