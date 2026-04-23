<template>
  <div>
    <div class="tab-toolbar">
      <div class="tab-toolbar__left">
        <el-button size="small" @click="$emit('refresh')">刷新文档</el-button>
      </div>
      <div class="tab-toolbar__right text-muted">
        当前知识库文档数：{{ pagination.total }}
      </div>
    </div>

    <el-table v-loading="loading" :data="documents" border stripe size="small">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
      <el-table-column prop="sourceType" label="来源类型" width="130" />
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
      <el-table-column label="操作" min-width="250" fixed="right">
        <template slot-scope="{ row }">
          <el-button
            type="text"
            size="small"
            :disabled="!canGovern"
            @click="$emit('reindex-document', row)"
          >
            重建索引
          </el-button>
          <el-button
            type="text"
            size="small"
            :disabled="!canGovern"
            @click="$emit('backfill-document', row)"
          >
            向量回填
          </el-button>
          <el-button type="text" size="small" @click="$emit('view-tasks', row)">任务状态</el-button>
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
  name: 'AdminKnowledgeDocumentTable',
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
    canGovern: {
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
