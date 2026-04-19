<template>
  <div class="conflict-diff-editor" v-loading="loading || monacoLoading">
    <div class="editor-topbar">
      <div class="editor-title-group">
        <div class="editor-title">内容冲突逐块处理</div>
        <div class="editor-subtitle">
          统一使用“源分支 / 目标分支”语义。每个冲突块都可选择保留源、保留目标、两边保留或手动编辑，最终结果实时预览并保存到源分支补充提交。
        </div>
      </div>
      <div class="editor-actions">
        <el-button size="mini" plain :disabled="loading || saving" @click="reloadKeepingDraft">
          刷新详情（保留草稿）
        </el-button>
        <el-button size="mini" plain :disabled="loading || saving" @click="reloadDiscardingDraft">
          重载服务端结果
        </el-button>
        <el-button
          size="mini"
          type="primary"
          :loading="saving"
          :disabled="saveDisabled"
          @click="emitSave"
        >
          保存冲突处理
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

    <el-alert
      v-if="staleMergeCheck"
      :title="staleAlertText"
      type="warning"
      :closable="false"
      show-icon
      class="editor-alert"
    />

    <el-empty v-if="!loading && !hasDetail" description="请选择一个内容冲突，或点击刷新详情。" :image-size="80" />

    <div v-else-if="isBinaryConflict" class="binary-state">
      <el-alert
        title="当前文件是二进制内容，无法在线逐块处理。"
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
        <el-tag size="mini" effect="plain">{{ displayPath || '-' }}</el-tag>
        <el-tag size="mini" effect="plain">源分支：{{ sourceBranchLabel }}</el-tag>
        <el-tag size="mini" effect="plain">目标分支：{{ targetBranchLabel }}</el-tag>
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
        :choice-map="blockChoiceMap"
        :source-branch-name="sourceBranchLabel"
        :target-branch-name="targetBranchLabel"
        :active-index="activeBlockIndex"
        :disabled="saving"
        @previous="goPreviousBlock"
        @next="goNextBlock"
        @select="selectBlock"
      />

      <div v-if="hasBlocks && currentBlock" class="block-action-panel">
        <div class="block-action-head">
          <div>
            <div class="block-action-title">当前冲突块 #{{ activeBlockIndex + 1 }}</div>
            <div class="block-action-subtitle">
              选择当前块如何进入最终结果；保存后将写入源分支补充提交。
            </div>
          </div>
          <el-tag size="mini" type="info" effect="plain">{{ currentBlock.blockType || 'BLOCK' }}</el-tag>
        </div>

        <div class="block-action-buttons">
          <el-button size="mini" :plain="currentChoice !== CONTENT_BLOCK_CHOICES.KEEP_SOURCE" type="primary" :disabled="actionDisabled" @click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.KEEP_SOURCE)">
            保留源分支（{{ sourceBranchLabel }}）
          </el-button>
          <el-button size="mini" :plain="currentChoice !== CONTENT_BLOCK_CHOICES.KEEP_TARGET" type="success" :disabled="actionDisabled" @click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.KEEP_TARGET)">
            保留目标分支（{{ targetBranchLabel }}）
          </el-button>
          <el-button size="mini" :plain="currentChoice !== CONTENT_BLOCK_CHOICES.KEEP_BOTH_SOURCE_THEN_TARGET" :disabled="actionDisabled" @click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.KEEP_BOTH_SOURCE_THEN_TARGET)">
            两边都保留（{{ sourceBranchLabel }} 在前）
          </el-button>
          <el-button size="mini" :plain="currentChoice !== CONTENT_BLOCK_CHOICES.KEEP_BOTH_TARGET_THEN_SOURCE" :disabled="actionDisabled" @click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.KEEP_BOTH_TARGET_THEN_SOURCE)">
            两边都保留（{{ targetBranchLabel }} 在前）
          </el-button>
          <el-button size="mini" :plain="currentChoice !== CONTENT_BLOCK_CHOICES.MANUAL" type="warning" :disabled="actionDisabled" @click="setCurrentBlockChoice(CONTENT_BLOCK_CHOICES.MANUAL)">
            手动编辑
          </el-button>
        </div>

        <div v-if="currentChoice === CONTENT_BLOCK_CHOICES.MANUAL" class="manual-block-editor">
          <div class="manual-title">当前块手动内容</div>
          <el-input
            v-model="currentBlockManualContent"
            type="textarea"
            :rows="8"
            :disabled="actionDisabled"
            placeholder="请输入该块最终内容"
          />
        </div>
      </div>

      <el-alert
        v-else-if="!hasBlocks"
        title="接口未返回结构化冲突块，已切换为整文件手动编辑模式。"
        type="info"
        :closable="false"
        show-icon
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
            <div class="pane-label">源分支（{{ sourceBranchLabel }}）</div>
            <el-input :value="sourceContent" type="textarea" :rows="10" readonly />
          </div>
          <div class="fallback-pane">
            <div class="pane-label">目标分支（{{ targetBranchLabel }}）</div>
            <el-input :value="targetContent" type="textarea" :rows="10" readonly />
          </div>
        </div>
        <div class="fallback-pane final">
          <div class="pane-label">最终结果预览</div>
          <el-input
            :value="internalContent"
            type="textarea"
            :rows="14"
            :readonly="finalPreviewReadOnly"
            :disabled="saving"
            @input="handleFallbackFinalInput"
          />
        </div>
      </div>

      <div v-else class="monaco-shell">
        <div class="pane-card">
          <div class="pane-head">
            <span>源分支（{{ sourceBranchLabel }}） vs 目标分支（{{ targetBranchLabel }}）</span>
            <el-tag size="mini" type="warning" effect="plain">Diff</el-tag>
          </div>
          <div ref="diffEditor" class="diff-editor"></div>
        </div>

        <div class="pane-card final-pane">
          <div class="pane-head">
            <span>最终结果预览（保存后进入源分支补充提交）</span>
            <el-tag size="mini" type="success" effect="plain">Preview</el-tag>
          </div>
          <div ref="finalEditor" class="final-editor"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ConflictBlockNavigator from './ConflictBlockNavigator.vue'
import {
  CONTENT_BLOCK_CHOICES,
  joinTextLines,
  normalizeContentBlockChoice,
  splitTextLines
} from '@/utils/projectMergeConflictAdapter'

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
    },
    sourceBranchName: {
      type: String,
      default: ''
    },
    targetBranchName: {
      type: String,
      default: ''
    },
    projectId: {
      type: [String, Number],
      default: ''
    },
    mergeRequestId: {
      type: [String, Number],
      default: ''
    },
    staleMergeCheck: {
      type: Boolean,
      default: false
    },
    staleMessage: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      CONTENT_BLOCK_CHOICES,
      activeBlockIndex: 0,
      basePanels: [],
      blockChoiceMap: {},
      diffEditor: null,
      discardNextDraftRestore: false,
      editorError: '',
      finalBlockRanges: {},
      finalDecorations: [],
      finalEditor: null,
      finalModel: null,
      internalContent: '',
      localDraftCache: {},
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
      return String(metadata.filePath || metadata.path || conflict.filePath || conflict.path || conflict.sourcePath || conflict.targetPath || conflict.basePath || this.detail.path || '')
    },
    sourceBranchLabel() {
      return String(this.sourceBranchName || this.metadata.sourceBranchName || this.metadata.sourceBranch || 'source')
    },
    targetBranchLabel() {
      return String(this.targetBranchName || this.metadata.targetBranchName || this.metadata.targetBranch || 'target')
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
    hasBlocks() {
      return this.normalizedBlocks.length > 0
    },
    currentBlock() {
      return this.normalizedBlocks[this.activeBlockIndex] || null
    },
    currentBlockId() {
      return this.currentBlock ? String(this.currentBlock.blockId || this.currentBlock.id || this.currentBlock.conflictBlockId || '') : ''
    },
    currentChoice() {
      if (!this.currentBlockId) return CONTENT_BLOCK_CHOICES.KEEP_SOURCE
      const row = this.blockChoiceMap[this.currentBlockId]
      return normalizeContentBlockChoice(row && row.choice ? row.choice : this.currentBlock.defaultChoice)
    },
    currentBlockManualContent: {
      get() {
        if (!this.currentBlockId) return ''
        const row = this.blockChoiceMap[this.currentBlockId] || {}
        if (typeof row.manualContent === 'string') return row.manualContent
        return this.defaultManualContentForBlock(this.currentBlock)
      },
      set(value) {
        this.updateCurrentBlockManualContent(value)
      }
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
      return this.loading || this.saving || !this.hasDetail || this.isBinaryConflict || this.staleMergeCheck
    },
    saveDisabled() {
      return this.actionDisabled
    },
    finalPreviewReadOnly() {
      return this.hasBlocks
    },
    staleAlertText() {
      return this.staleMessage || '当前 merge-check 已过期，请先重新检查后再保存。'
    }
  },
  watch: {
    detail: {
      immediate: true,
      handler() {
        this.syncDraftFromDetail()
        this.$nextTick(() => {
          this.refreshMonaco()
        })
      }
    },
    conflict() {
      this.activeBlockIndex = 0
    },
    saving() {
      if (this.finalEditor) {
        this.finalEditor.updateOptions({ readOnly: this.finalEditorReadOnly() })
      }
    },
    activeBlockIndex() {
      this.persistCurrentDraft()
      this.applyBlockDecorations()
    },
    normalizedBlocks() {
      if (this.activeBlockIndex >= this.normalizedBlocks.length) {
        this.activeBlockIndex = Math.max(this.normalizedBlocks.length - 1, 0)
      }
      this.ensureBlockChoiceMap()
      if (this.hasBlocks) {
        this.rebuildFinalContentFromChoices()
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
    positiveCount(values, fallback = 0) {
      for (let index = 0; index < values.length; index += 1) {
        const number = Number(values[index])
        if (Number.isFinite(number) && number >= 0) return number
      }
      return fallback
    },
    endLineByCount(startLine, count, fallbackEnd) {
      const start = this.firstPositiveNumber([startLine], 1)
      const lineCount = this.positiveCount([count], 0)
      if (lineCount > 0) return start + lineCount - 1
      return this.firstPositiveNumber([fallbackEnd], start)
    },
    fallbackChoiceByBlockType(blockType) {
      const type = String(blockType || '').trim().toUpperCase()
      if (type === 'TARGET_ONLY') return CONTENT_BLOCK_CHOICES.KEEP_TARGET
      if (type === 'MANUAL') return CONTENT_BLOCK_CHOICES.MANUAL
      return CONTENT_BLOCK_CHOICES.KEEP_SOURCE
    },
    normalizeBlock(block, index) {
      const item = block && typeof block === 'object' ? { ...block } : {}
      item.blockId = String(item.blockId || item.id || item.conflictBlockId || `block-${index + 1}`)
      item.blockType = String(item.blockType || item.type || '').toUpperCase()

      const sourceStartLine = this.firstPositiveNumber([
        item.sourceStartLine,
        item.sourceStart,
        item.sourceLineStart,
        item.leftStartLine,
        item.leftStart,
        item.startLine,
        item.lineStart,
        item.start
      ], index + 1)
      const sourceEndLine = this.endLineByCount(
        sourceStartLine,
        this.positiveCount([item.sourceLineCount], 0),
        this.firstPositiveNumber([item.sourceEndLine, item.sourceEnd, item.sourceLineEnd, item.leftEndLine, item.leftEnd, item.endLine, item.lineEnd, item.end], sourceStartLine)
      )

      const targetStartLine = this.firstPositiveNumber([
        item.targetStartLine,
        item.targetStart,
        item.targetLineStart,
        item.rightStartLine,
        item.rightStart,
        item.startLine,
        item.lineStart,
        item.start
      ], sourceStartLine)
      const targetEndLine = this.endLineByCount(
        targetStartLine,
        this.positiveCount([item.targetLineCount], 0),
        this.firstPositiveNumber([item.targetEndLine, item.targetEnd, item.targetLineEnd, item.rightEndLine, item.rightEnd, item.endLine, item.lineEnd, item.end], targetStartLine)
      )

      const baseStartLine = this.firstPositiveNumber([
        item.baseStartLine,
        item.baseStart,
        item.baseLineStart,
        item.startLine,
        item.lineStart,
        item.start
      ], sourceStartLine)
      const baseEndLine = this.endLineByCount(
        baseStartLine,
        this.positiveCount([item.baseLineCount], 0),
        this.firstPositiveNumber([item.baseEndLine, item.baseEnd, item.baseLineEnd, item.endLine, item.lineEnd, item.end], baseStartLine)
      )

      item.sourceStartLine = sourceStartLine
      item.sourceEndLine = sourceEndLine
      item.targetStartLine = targetStartLine
      item.targetEndLine = targetEndLine
      item.baseStartLine = baseStartLine
      item.baseEndLine = baseEndLine

      item.sourceLines = this.extractBlockLines(item, 'source', this.sourceContent, sourceStartLine, sourceEndLine)
      item.targetLines = this.extractBlockLines(item, 'target', this.targetContent, targetStartLine, targetEndLine)
      item.baseLines = this.extractBlockLines(item, 'base', this.baseContent, baseStartLine, baseEndLine)

      item.sourceText = joinTextLines(item.sourceLines)
      item.targetText = joinTextLines(item.targetLines)
      item.baseText = joinTextLines(item.baseLines)
      item.defaultChoice = normalizeContentBlockChoice(item.defaultChoice, this.fallbackChoiceByBlockType(item.blockType))
      return item
    },
    extractBlockLines(block, side, fullText, startLine, endLine) {
      const linesKey = `${side}Lines`
      const contentKey = `${side}Content`
      if (Array.isArray(block[linesKey])) {
        return block[linesKey].map(item => (item == null ? '' : String(item)))
      }
      if (typeof block[contentKey] === 'string') {
        return splitTextLines(block[contentKey])
      }
      return this.sliceLineRange(fullText, startLine, endLine)
    },
    sliceLineRange(content, startLine, endLine) {
      const lines = splitTextLines(content)
      const start = Math.max(0, (Number(startLine) || 1) - 1)
      const end = Math.max(start, Number(endLine) || Number(startLine) || 1)
      return lines.slice(start, end)
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
    draftStorageKey(conflictId = this.currentConflictId) {
      const project = String(this.projectId || '')
      const mr = String(this.mergeRequestId || (this.detail && this.detail.mergeRequestId) || '')
      return `project-merge-content-draft:${project}:${mr}:${String(conflictId || '')}`
    },
    readPersistedDraft(conflictId = this.currentConflictId) {
      if (!process.client || !conflictId) return null
      try {
        const raw = window.localStorage.getItem(this.draftStorageKey(conflictId))
        if (!raw) return null
        const parsed = JSON.parse(raw)
        return parsed && typeof parsed === 'object' ? parsed : null
      } catch (error) {
        return null
      }
    },
    writePersistedDraft(draft, conflictId = this.currentConflictId) {
      if (!process.client || !conflictId || !draft || typeof draft !== 'object') return
      try {
        window.localStorage.setItem(this.draftStorageKey(conflictId), JSON.stringify(draft))
      } catch (error) {
        // ignore quota/storage errors
      }
    },
    removePersistedDraft(conflictId = this.currentConflictId) {
      if (!process.client || !conflictId) return
      try {
        window.localStorage.removeItem(this.draftStorageKey(conflictId))
      } catch (error) {
        // ignore
      }
    },
    buildDefaultChoiceMap(blocks = this.normalizedBlocks) {
      const map = {}
      blocks.forEach(block => {
        if (!block) return
        const blockId = String(block.blockId || block.id || '')
        if (!blockId) return
        const choice = normalizeContentBlockChoice(block.defaultChoice, this.fallbackChoiceByBlockType(block.blockType))
        map[blockId] = {
          choice,
          manualContent: this.defaultManualContentForBlock(block)
        }
      })
      return map
    },
    sanitizeChoiceMap(choiceMap, blocks = this.normalizedBlocks) {
      const draftMap = choiceMap && typeof choiceMap === 'object' ? choiceMap : {}
      const next = {}
      blocks.forEach(block => {
        if (!block) return
        const blockId = String(block.blockId || block.id || '')
        if (!blockId) return
        const raw = draftMap[blockId] && typeof draftMap[blockId] === 'object' ? draftMap[blockId] : {}
        const choice = normalizeContentBlockChoice(raw.choice, normalizeContentBlockChoice(block.defaultChoice, this.fallbackChoiceByBlockType(block.blockType)))
        next[blockId] = {
          choice,
          manualContent: typeof raw.manualContent === 'string' ? raw.manualContent : this.defaultManualContentForBlock(block)
        }
      })
      return next
    },
    defaultManualContentForBlock(block) {
      if (!block || typeof block !== 'object') return ''
      if (typeof block.resolvedContent === 'string') return block.resolvedContent
      if (Array.isArray(block.resolvedLines)) return joinTextLines(block.resolvedLines)
      if (typeof block.sourceText === 'string') return block.sourceText
      if (typeof block.targetText === 'string') return block.targetText
      return ''
    },
    syncDraftFromDetail() {
      this.activeBlockIndex = 0
      this.finalBlockRanges = {}
      this.editorError = ''

      if (!this.hasDetail || !this.currentConflictId) {
        this.blockChoiceMap = {}
        this.internalContent = this.initialResolvedContent()
        return
      }

      const defaultChoiceMap = this.buildDefaultChoiceMap(this.normalizedBlocks)
      const memoryDraft = this.localDraftCache[this.currentConflictId]
      const persistedDraft = memoryDraft || this.readPersistedDraft(this.currentConflictId)
      const shouldRestore = !this.discardNextDraftRestore

      if (this.hasBlocks) {
        const restoreMap = shouldRestore && persistedDraft && persistedDraft.blockChoiceMap
          ? this.sanitizeChoiceMap(persistedDraft.blockChoiceMap, this.normalizedBlocks)
          : defaultChoiceMap
        this.blockChoiceMap = restoreMap

        if (shouldRestore && persistedDraft && persistedDraft.activeBlockId) {
          const index = this.normalizedBlocks.findIndex(block => String(block && block.blockId) === String(persistedDraft.activeBlockId))
          if (index >= 0) this.activeBlockIndex = index
        }

        this.rebuildFinalContentFromChoices(false)
      } else {
        this.blockChoiceMap = {}
        if (shouldRestore && persistedDraft && typeof persistedDraft.resolvedContent === 'string') {
          this.setFinalContent(persistedDraft.resolvedContent, false, false)
        } else {
          this.setFinalContent(this.initialResolvedContent(), false, false)
        }
      }

      this.discardNextDraftRestore = false
      this.persistCurrentDraft()
    },
    ensureBlockChoiceMap() {
      if (!this.hasBlocks) {
        this.blockChoiceMap = {}
        return
      }
      this.blockChoiceMap = this.sanitizeChoiceMap(this.blockChoiceMap, this.normalizedBlocks)
    },
    getBlockChoice(block) {
      if (!block) return null
      const blockId = String(block.blockId || block.id || '')
      if (!blockId) return null
      if (!this.blockChoiceMap[blockId]) {
        const choice = normalizeContentBlockChoice(block.defaultChoice, this.fallbackChoiceByBlockType(block.blockType))
        this.$set(this.blockChoiceMap, blockId, {
          choice,
          manualContent: this.defaultManualContentForBlock(block)
        })
      }
      return this.blockChoiceMap[blockId]
    },
    setCurrentBlockChoice(choice) {
      if (this.actionDisabled || !this.currentBlock) return
      const blockId = String(this.currentBlock.blockId || '')
      if (!blockId) return
      const current = this.getBlockChoice(this.currentBlock) || {}
      const normalizedChoice = normalizeContentBlockChoice(choice, this.currentBlock.defaultChoice)
      const next = {
        choice: normalizedChoice,
        manualContent: typeof current.manualContent === 'string' ? current.manualContent : this.defaultManualContentForBlock(this.currentBlock)
      }
      this.$set(this.blockChoiceMap, blockId, next)
      this.rebuildFinalContentFromChoices()
    },
    updateCurrentBlockManualContent(value) {
      if (!this.currentBlock || this.actionDisabled) return
      const blockId = String(this.currentBlock.blockId || '')
      if (!blockId) return
      const current = this.getBlockChoice(this.currentBlock) || {}
      const next = {
        choice: CONTENT_BLOCK_CHOICES.MANUAL,
        manualContent: String(value == null ? '' : value)
      }
      if (current.choice === next.choice && current.manualContent === next.manualContent) return
      this.$set(this.blockChoiceMap, blockId, next)
      this.rebuildFinalContentFromChoices()
    },
    resolveLinesByChoice(block, choiceState) {
      const choice = normalizeContentBlockChoice(choiceState && choiceState.choice ? choiceState.choice : block.defaultChoice)
      const sourceLines = Array.isArray(block.sourceLines) ? [...block.sourceLines] : []
      const targetLines = Array.isArray(block.targetLines) ? [...block.targetLines] : []

      if (choice === CONTENT_BLOCK_CHOICES.KEEP_SOURCE) return sourceLines
      if (choice === CONTENT_BLOCK_CHOICES.KEEP_TARGET) return targetLines
      if (choice === CONTENT_BLOCK_CHOICES.KEEP_BOTH_SOURCE_THEN_TARGET) return sourceLines.concat(targetLines)
      if (choice === CONTENT_BLOCK_CHOICES.KEEP_BOTH_TARGET_THEN_SOURCE) return targetLines.concat(sourceLines)

      const manualContent = choiceState && typeof choiceState.manualContent === 'string'
        ? choiceState.manualContent
        : this.defaultManualContentForBlock(block)
      return splitTextLines(manualContent)
    },
    rebuildFinalContentFromChoices(persist = true) {
      if (!this.hasBlocks) {
        if (persist) this.persistCurrentDraft()
        return
      }
      const lines = []
      const ranges = {}

      this.normalizedBlocks.forEach(block => {
        if (!block) return
        const blockId = String(block.blockId || '')
        if (!blockId) return
        const choiceState = this.getBlockChoice(block)
        const selectedLines = this.resolveLinesByChoice(block, choiceState)
        const startLine = Math.max(1, lines.length + 1)
        if (selectedLines.length) {
          lines.push(...selectedLines)
        }
        const endLine = selectedLines.length ? lines.length : startLine
        ranges[blockId] = {
          startLine,
          endLine
        }
      })

      this.finalBlockRanges = ranges
      this.setFinalContent(joinTextLines(lines), true, persist)
    },
    persistCurrentDraft() {
      if (!this.currentConflictId) return
      const draft = {
        conflictId: this.currentConflictId,
        activeBlockId: this.currentBlockId,
        resolvedContent: this.internalContent,
        blockChoiceMap: this.hasBlocks ? this.sanitizeChoiceMap(this.blockChoiceMap, this.normalizedBlocks) : {},
        updatedAt: Date.now()
      }
      this.$set(this.localDraftCache, this.currentConflictId, draft)
      this.writePersistedDraft(draft, this.currentConflictId)
    },
    clearCurrentDraft() {
      if (!this.currentConflictId) return
      this.$delete(this.localDraftCache, this.currentConflictId)
      this.removePersistedDraft(this.currentConflictId)
    },
    reloadKeepingDraft() {
      this.$emit('refresh')
    },
    reloadDiscardingDraft() {
      this.clearCurrentDraft()
      this.discardNextDraftRestore = true
      this.$emit('refresh')
    },
    handleFallbackFinalInput(value) {
      if (this.finalPreviewReadOnly || this.saving) return
      this.setFinalContent(value, true)
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
          readOnly: this.finalEditorReadOnly(),
          scrollBeyondLastLine: false,
          wordWrap: 'on'
        })
        this.finalEditor.setModel(this.finalModel)
        this.finalEditor.onDidChangeModelContent(() => {
          if (this.suppressChange || !this.finalModel || this.finalPreviewReadOnly) return
          this.internalContent = this.finalModel.getValue()
          this.persistCurrentDraft()
        })
      }

      this.finalEditor.updateOptions({ readOnly: this.finalEditorReadOnly() })
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
    finalEditorReadOnly() {
      return !!this.saving || this.finalPreviewReadOnly
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
        const range = this.finalBlockRanges[this.currentBlockId] || {
          startLine: this.currentBlock.targetStartLine,
          endLine: this.currentBlock.targetEndLine
        }
        this.finalDecorations = this.finalEditor.deltaDecorations(this.finalDecorations, [
          this.lineDecoration(range.startLine, range.endLine, 'current-final-block', 'current-block-glyph')
        ])
        this.finalEditor.revealLineInCenter(range.startLine || 1)
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
    setFinalContent(value, syncEditor = true, persist = true) {
      this.internalContent = String(value == null ? '' : value)
      if (syncEditor && this.finalModel) {
        this.setModelValue(this.finalModel, this.internalContent)
      }
      if (persist) {
        this.persistCurrentDraft()
      }
      this.$nextTick(() => this.applyBlockDecorations())
    },
    buildBlockChoicesPayload() {
      if (!this.hasBlocks) return []
      return this.normalizedBlocks.map(block => {
        const blockId = String(block.blockId || '')
        const row = this.getBlockChoice(block) || {}
        const choice = normalizeContentBlockChoice(row.choice, block.defaultChoice)
        const payload = {
          blockId,
          choice
        }
        if (choice === CONTENT_BLOCK_CHOICES.MANUAL) {
          const manualContent = typeof row.manualContent === 'string' ? row.manualContent : this.defaultManualContentForBlock(block)
          payload.resolvedContent = manualContent
          payload.resolvedLines = splitTextLines(manualContent)
        }
        return payload
      })
    },
    emitSave() {
      if (this.saveDisabled) return
      const content = this.finalModel ? this.finalModel.getValue() : this.internalContent
      this.$emit('save', {
        conflict: this.conflict,
        conflictId: this.currentConflictId,
        resolvedContent: content,
        blockChoices: this.buildBlockChoicesPayload(),
        metadata: {
          resolutionMode: this.hasBlocks ? 'BLOCK_CHOICES' : 'RAW_CONTENT'
        }
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

.editor-title,
.metadata-value,
.base-title,
.block-action-title,
.manual-title,
.pane-head,
.pane-label {
  color: var(--it-text);
}

.editor-title {
  font-size: 16px;
  font-weight: 700;
}

.editor-subtitle,
.metadata-label,
.block-action-subtitle {
  margin-top: 6px;
  color: var(--it-text-muted);
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

.editor-body,
.monaco-shell,
.fallback-editor {
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

.metadata-item,
.base-collapse,
.manual-block-editor,
.pane-card,
.fallback-pane {
  border: 1px solid var(--it-border);
  border-radius: 14px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
}

.metadata-item {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metadata-value {
  font-size: 13px;
  word-break: break-word;
}

.base-collapse {
  overflow: hidden;
}

.base-title {
  padding-left: 12px;
  font-weight: 700;
  font-size: 13px;
}

.base-content {
  margin: 0;
  max-height: 240px;
  overflow: auto;
  padding: 12px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-text) 86%, black 20%), color-mix(in srgb, var(--it-text) 94%, black 10%));
  color: #e2e8f0;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.block-action-panel {
  border: 1px solid var(--it-border);
  border-radius: 14px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 7%, var(--it-surface-elevated)), var(--it-surface-solid));
  box-shadow: var(--it-shadow-soft);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.block-action-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.block-action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.manual-block-editor {
  padding: 10px;
}

.manual-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
}

.binary-state {
  border: 1px solid color-mix(in srgb, var(--it-warning) 30%, var(--it-border));
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 12%, var(--it-surface-elevated)), var(--it-surface-solid));
  border-radius: 14px;
  padding: 12px;
}

.pane-card {
  overflow: hidden;
}

.pane-head {
  min-height: 42px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--it-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
}

.diff-editor {
  height: 360px;
}

.final-editor {
  height: 420px;
}

.fallback-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.fallback-pane {
  padding: 12px;
}

.fallback-pane.final {
  border-color: var(--it-border-strong);
}

.pane-label {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
}

/deep/ .current-source-block {
  background: color-mix(in srgb, var(--it-accent) 16%, transparent);
}

/deep/ .current-target-block {
  background: color-mix(in srgb, var(--it-warning) 18%, transparent);
}

/deep/ .current-final-block {
  background: color-mix(in srgb, var(--it-success) 18%, transparent);
}

/deep/ .current-block-glyph {
  background: var(--it-warning);
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

