<template>
  <div>
    <div class="tab-toolbar">
      <div class="tab-toolbar__left">
        <el-button v-if="canEdit" type="primary" size="small" @click="$emit('open-document-dialog')">新增文档</el-button>
        <el-button v-if="canEdit" size="small" @click="triggerFileInput('uploadFileInput')">上传文件</el-button>
        <el-button v-if="canEdit" size="small" @click="triggerFileInput('uploadZipInput')">ZIP 导入项目</el-button>
        <el-button v-if="canEdit" size="small" @click="triggerFileInput('localFileInput')">导入本地文本</el-button>
        <el-button size="small" @click="$emit('refresh')">刷新文档</el-button>
        <el-button size="small" @click="$emit('open-tasks')">索引任务</el-button>
        <el-button size="small" @click="$emit('download-zip')">打包下载</el-button>

        <input
          ref="uploadFileInput"
          class="hidden-file-input"
          type="file"
          multiple
          accept=".txt,.md,.markdown,.json,.csv,.js,.ts,.java,.xml,.html,.htm,.css,.vue,.sql,.yml,.yaml,.pdf,.doc,.docx"
          @change="handleUploadFiles"
        />

        <input
          ref="uploadZipInput"
          class="hidden-file-input"
          type="file"
          accept=".zip,application/zip"
          @change="handleUploadZip"
        />

        <input
          ref="localFileInput"
          class="hidden-file-input"
          type="file"
          accept=".txt,.md,.markdown,.json,.csv,.js,.ts,.java,.xml,.html,.htm,.css,.vue,.sql,.yml,.yaml"
          @change="handleLocalFile"
        />
      </div>

      <div class="tab-toolbar__right text-muted">
        当前知识库文档数：{{ pagination.total }}
      </div>
    </div>

    <el-table v-loading="loading" :data="documents" border stripe size="small">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="sourceType" label="来源类型" width="120" />
      <el-table-column prop="fileType" label="文件类型" width="120" />
      <el-table-column label="状态" width="120">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="docStatusTagType(row.status)">{{ row.status || 'UNKNOWN' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="向量状态" width="160">
        <template slot-scope="{ row }">
          <el-tag size="mini" :type="documentEmbeddingTagType(row)">
            {{ documentEmbeddingLabel(row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="索引时间" min-width="170">
        <template slot-scope="{ row }">{{ formatTime(row.indexedAt) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="170">
        <template slot-scope="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="ZIP 包" min-width="180" show-overflow-tooltip>
        <template slot-scope="{ row }">{{ row.archiveName || '-' }}</template>
      </el-table-column>
      <el-table-column label="包内路径" min-width="260" show-overflow-tooltip>
        <template slot-scope="{ row }">{{ row.archiveEntryPath || '-' }}</template>
      </el-table-column>
      <el-table-column label="导入批次" min-width="180" show-overflow-tooltip>
        <template slot-scope="{ row }">{{ row.importBatchId || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="360" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="$emit('view-chunks', row)">查看切片</el-button>
          <el-button type="text" size="small" @click="$emit('preview-chunks', row)">切片预览</el-button>
          <el-button type="text" size="small" @click="$emit('backfill-document', row)">向量回填</el-button>
          <el-button v-if="canEdit" type="text" size="small" @click="$emit('reindex-document', row)">重建索引</el-button>
          <el-button type="text" size="small" @click="$emit('view-tasks', row)">索引记录</el-button>
          <el-button type="text" size="small" @click="$emit('download-document', row)">下载</el-button>
          <el-button type="text" size="small" @click="$emit('seed-chat', row)">引用到提问</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pagination.page + 1"
        :page-size="pagination.size"
        :total="pagination.total"
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'KnowledgeBaseDocumentTab',
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    documents: {
      type: Array,
      default: () => []
    },
    pagination: {
      type: Object,
      default: () => ({ page: 0, size: 10, total: 0 })
    },
    canEdit: {
      type: Boolean,
      default: false
    },
    formatTime: {
      type: Function,
      default: value => value
    },
    docStatusTagType: {
      type: Function,
      default: () => 'info'
    },
    documentEmbeddingLabel: {
      type: Function,
      default: () => ''
    },
    documentEmbeddingTagType: {
      type: Function,
      default: () => 'info'
    }
  },
  methods: {
    triggerFileInput(refName) {
      const input = this.$refs[refName]
      if (!input) return
      input.value = ''
      input.click()
    },

    handleUploadFiles(event) {
      const files = Array.from((event && event.target && event.target.files) || [])
      if (files.length) {
        this.$emit('upload-files', files)
      }
      if (event && event.target) event.target.value = ''
    },

    handleUploadZip(event) {
      const file = event && event.target && event.target.files ? event.target.files[0] : null
      if (file) {
        this.$emit('upload-zip', file)
      }
      if (event && event.target) event.target.value = ''
    },

    handleLocalFile(event) {
      const file = event && event.target && event.target.files ? event.target.files[0] : null
      if (file) {
        this.$emit('import-local-file', file)
      }
      if (event && event.target) event.target.value = ''
    }
  }
}
</script>

<style scoped>
.tab-toolbar,
.tab-toolbar__left,
.tab-toolbar__right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tab-toolbar {
  justify-content: space-between;
  margin-bottom: 12px;
}

.hidden-file-input {
  display: none;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
