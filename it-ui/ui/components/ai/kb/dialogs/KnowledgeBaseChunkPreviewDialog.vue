<template>
  <el-dialog
    title="文档切片"
    :visible.sync="dialogVisible"
    width="880px"
    destroy-on-close
  >
    <div class="drawer-meta">
      <span v-if="document">文档：{{ document.title || `文档 #${document.id}` }}</span>
    </div>

    <el-table v-loading="loading" :data="chunks" border stripe size="small">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="chunkIndex" label="Chunk" width="90" />
      <el-table-column prop="tokenCount" label="Tokens" width="90" />
      <el-table-column label="内容" min-width="520">
        <template slot-scope="{ row }">
          <div class="chunk-content">{{ row.content || row.text || '-' }}</div>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script>
export default {
  name: 'KnowledgeBaseChunkPreviewDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    document: {
      type: Object,
      default: null
    },
    chunks: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    }
  }
}
</script>

<style scoped>
.drawer-meta {
  margin-bottom: 12px;
  color: #909399;
  font-size: 12px;
}

.chunk-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}
</style>
