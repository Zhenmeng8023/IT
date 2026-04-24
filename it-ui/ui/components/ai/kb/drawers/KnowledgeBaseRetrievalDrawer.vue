<template>
  <el-drawer
    :visible.sync="drawerVisible"
    title="检索日志"
    size="68%"
    destroy-on-close
  >
    <div class="drawer-body">
      <div class="drawer-meta">{{ meta }}</div>

      <el-table v-loading="loading" :data="logs" border stripe size="small">
        <el-table-column prop="title" label="命中文档" min-width="200" show-overflow-tooltip />
        <el-table-column prop="documentId" label="文档 ID" width="100" />
        <el-table-column prop="chunkIndex" label="切片" width="90" />
        <el-table-column prop="score" label="总分" width="110" />
        <el-table-column prop="keywordScore" label="关键词分" width="110" />
        <el-table-column prop="vectorScore" label="向量分" width="110" />
        <el-table-column label="检索方式" width="140">
          <template slot-scope="{ row }">
            {{ retrievalMethodLabel(row.retrievalMethod) }}
          </template>
        </el-table-column>
        <el-table-column label="路径" min-width="220" show-overflow-tooltip>
          <template slot-scope="{ row }">
            {{ row.archiveEntryPath || row.fileName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="360">
          <template slot-scope="{ row }">
            <div class="chunk-content">{{ row.content || '-' }}</div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script>
export default {
  name: 'KnowledgeBaseRetrievalDrawer',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    meta: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    logs: {
      type: Array,
      default: () => []
    },
    retrievalMethodLabel: {
      type: Function,
      default: value => value || '-'
    }
  },
  computed: {
    drawerVisible: {
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
.drawer-body {
  padding: 0 20px 20px;
}

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
