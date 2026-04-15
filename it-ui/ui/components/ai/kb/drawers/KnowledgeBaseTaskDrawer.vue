<template>
  <el-drawer
    :visible.sync="drawerVisible"
    :title="title"
    size="60%"
    destroy-on-close
  >
    <div class="drawer-body">
      <div class="drawer-meta">{{ subtitle }}</div>

      <el-table v-loading="loading" :data="tasks" border stripe size="small">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="taskType" label="任务类型" width="140" />
        <el-table-column label="状态" width="120">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="taskStatusTagType(row.status)">{{ row.status || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="documentId" label="文档 ID" width="100" />
        <el-table-column prop="message" label="消息" min-width="240" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="170">
          <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template slot-scope="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script>
export default {
  name: 'KnowledgeBaseTaskDrawer',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '索引任务'
    },
    subtitle: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    tasks: {
      type: Array,
      default: () => []
    },
    taskStatusTagType: {
      type: Function,
      default: () => 'info'
    },
    formatTime: {
      type: Function,
      default: value => value
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
</style>
