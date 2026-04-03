<template>
  <el-dialog
    title="文档版本历史"
    :visible.sync="innerVisible"
    width="980px"
    top="5vh"
    append-to-body
    @close="$emit('close')"
  >
    <div class="history-layout">
      <div class="history-left">
        <div class="history-header">
          <div>
            <div class="history-title">{{ doc ? doc.title : '未选择文档' }}</div>
            <div class="history-subtitle">当前版本：v{{ doc && doc.currentVersion ? doc.currentVersion : '-' }}</div>
          </div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadVersions">刷新</el-button>
        </div>

        <el-table v-loading="loading" :data="versions" border height="420" highlight-current-row @current-change="handleCurrentChange">
          <el-table-column prop="versionNo" label="版本" width="80">
            <template slot-scope="scope">v{{ scope.row.versionNo }}</template>
          </el-table-column>
          <el-table-column prop="changeSummary" label="变更摘要" min-width="240"></el-table-column>
          <el-table-column prop="createdAt" label="时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="previewVersion(scope.row)">查看</el-button>
              <el-button type="text" size="mini" @click="rollback(scope.row)">回滚</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="history-right">
        <div class="preview-header">
          <span>版本预览</span>
          <span v-if="selectedVersion">v{{ selectedVersion.versionNo }}</span>
        </div>
        <pre class="preview-box">{{ selectedVersion ? selectedVersion.contentSnapshot : '请选择左侧版本查看正文快照' }}</pre>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { listProjectDocVersions, getProjectDocVersion, rollbackProjectDocVersion } from '@/api/projectDoc'

export default {
  name: 'ProjectDocHistory',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    doc: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      innerVisible: false,
      loading: false,
      versions: [],
      selectedVersion: null
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(v) {
        this.innerVisible = v
        if (v) {
          this.loadVersions()
        }
      }
    },
    innerVisible(v) {
      this.$emit('update:visible', v)
    },
    doc: {
      deep: true,
      handler() {
        if (this.innerVisible) {
          this.loadVersions()
        }
      }
    }
  },
  methods: {
    formatTime(v) {
      if (!v) return '-'
      const d = new Date(v)
      if (Number.isNaN(d.getTime())) return v
      const p = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    async loadVersions() {
      if (!this.doc || !this.doc.id) {
        this.versions = []
        this.selectedVersion = null
        return
      }
      try {
        this.loading = true
        const res = await listProjectDocVersions(this.doc.id)
        this.versions = Array.isArray(res?.data) ? res.data : []
        this.selectedVersion = this.versions.length ? this.versions[0] : null
        if (this.selectedVersion && !this.selectedVersion.contentSnapshot) {
          await this.previewVersion(this.selectedVersion)
        }
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '加载版本失败'
        this.$message.error(m)
      } finally {
        this.loading = false
      }
    },
    handleCurrentChange(row) {
      if (row) {
        this.previewVersion(row)
      }
    },
    async previewVersion(row) {
      if (!row || !this.doc || !this.doc.id) return
      try {
        const res = await getProjectDocVersion(this.doc.id, row.versionNo)
        this.selectedVersion = res?.data || row
      } catch (e) {
        const m = e.response?.data?.message || e.response?.data?.msg || '加载版本详情失败'
        this.$message.error(m)
      }
    },
    rollback(row) {
      if (!row || !this.doc || !this.doc.id) return
      this.$confirm(`确定回滚到版本 v${row.versionNo} 吗？当前数据库结构只会回滚正文内容。`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await rollbackProjectDocVersion(this.doc.id, row.versionNo)
          this.$message.success(`已回滚到版本 v${row.versionNo}`)
          this.$emit('rolled-back')
          await this.loadVersions()
        } catch (e) {
          const m = e.response?.data?.message || e.response?.data?.msg || '回滚失败'
          this.$message.error(m)
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.history-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 16px;
}

.history-header,
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.history-title {
  font-size: 16px;
  font-weight: 600;
}

.history-subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.preview-box {
  margin: 0;
  min-height: 470px;
  max-height: 470px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
  padding: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, Monaco, monospace;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .history-layout {
    grid-template-columns: 1fr;
  }
}
</style>
