<template>
  <el-dialog
    title="文档版本历史"
    :visible.sync="innerVisible"
    width="1200px"
    top="4vh"
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
          <div class="history-header-actions">
            <el-button size="mini" icon="el-icon-refresh" @click="loadVersions">刷新</el-button>
            <el-button size="mini" plain @click="resetCompare">清空对比</el-button>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="versions"
          border
          height="500"
          highlight-current-row
          @current-change="handleCurrentChange"
        >
          <el-table-column prop="versionNo" label="版本" width="90">
            <template slot-scope="scope">
              <div class="version-no-cell">
                <span>v{{ scope.row.versionNo }}</span>
                <el-tag v-if="scope.row.currentVersion" size="mini" type="success" effect="plain">当前</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="changeSummary" label="变更摘要" min-width="220" />
          <el-table-column prop="createdAt" label="时间" width="170">
            <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="对比" width="165">
            <template slot-scope="scope">
              <div class="compare-actions">
                <el-button
                  size="mini"
                  :type="leftVersionNo === scope.row.versionNo ? 'primary' : 'default'"
                  plain
                  @click="handleSelectLeftVersion(scope.row)"
                >
                  左侧
                </el-button>
                <el-button
                  size="mini"
                  :type="rightVersionNo === scope.row.versionNo ? 'primary' : 'default'"
                  plain
                  @click="handleSelectRightVersion(scope.row)"
                >
                  右侧
                </el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" size="mini" @click="previewVersion(scope.row)">查看</el-button>
              <el-button type="text" size="mini" @click="rollback(scope.row)">回滚</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="history-right">
        <div class="preview-header">
          <div class="preview-title-wrap">
            <div class="preview-title">{{ compareMode ? '版本对比' : '版本预览' }}</div>
            <div class="preview-subtitle" v-if="compareMode">
              <span>左侧：v{{ leftVersionNo || '-' }}</span>
              <span>右侧：v{{ rightVersionNo || '-' }}</span>
            </div>
            <div class="preview-subtitle" v-else-if="selectedVersion">
              当前查看：v{{ selectedVersion.versionNo }}
            </div>
          </div>
          <div class="preview-actions">
            <el-button
              v-if="canLoadCompare"
              size="mini"
              type="primary"
              plain
              :loading="compareLoading"
              @click="loadVersionCompare"
            >
              重新对比
            </el-button>
            <el-button
              v-if="selectedVersion && !compareMode"
              size="mini"
              plain
              @click="useCurrentAndCompare(selectedVersion)"
            >
              与当前版本对比
            </el-button>
          </div>
        </div>

        <div v-if="compareMode" v-loading="compareLoading" class="compare-shell">
          <div class="compare-toolbar">
            <el-tag size="mini" type="info" effect="plain">共 {{ compareRows.length }} 行</el-tag>
            <el-tag size="mini" type="success" effect="plain">相同行：{{ sameRowCount }}</el-tag>
            <el-tag size="mini" type="warning" effect="plain">差异行：{{ changedRowCount }}</el-tag>
          </div>

          <div class="compare-grid compare-grid-head">
            <div class="compare-col-head">
              <span>v{{ leftVersionNo || '-' }}</span>
              <span class="compare-col-meta">{{ leftVersionMeta }}</span>
            </div>
            <div class="compare-col-head">
              <span>v{{ rightVersionNo || '-' }}</span>
              <span class="compare-col-meta">{{ rightVersionMeta }}</span>
            </div>
          </div>

          <div class="compare-body">
            <div
              v-for="(row, index) in compareRows"
              :key="'cmp-' + index"
              class="compare-grid compare-row"
              :class="{ 'is-diff': !row.same }"
            >
              <div class="compare-col" :class="{ 'is-empty': !row.left }">
                <div class="compare-line-no">{{ index + 1 }}</div>
                <pre class="compare-line-text">{{ row.left || ' ' }}</pre>
              </div>
              <div class="compare-col" :class="{ 'is-empty': !row.right }">
                <div class="compare-line-no">{{ index + 1 }}</div>
                <pre class="compare-line-text">{{ row.right || ' ' }}</pre>
              </div>
            </div>
          </div>
        </div>

        <pre v-else class="preview-box">{{ selectedVersion ? selectedVersion.contentSnapshot : '请选择左侧版本查看正文快照' }}</pre>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import {
  listProjectDocVersions,
  getProjectDocVersion,
  compareProjectDocVersions,
  rollbackProjectDocVersion
} from '@/api/projectDoc'

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
      compareLoading: false,
      versions: [],
      selectedVersion: null,
      leftVersionNo: null,
      rightVersionNo: null,
      compareResult: null
    }
  },
  computed: {
    compareMode() {
      return !!(this.compareResult && this.leftVersionNo && this.rightVersionNo)
    },
    canLoadCompare() {
      return !!(this.doc && this.doc.id && this.leftVersionNo && this.rightVersionNo)
    },
    compareRows() {
      if (!this.compareResult) return []
      const leftLines = this.normalizeLines(this.compareResult.leftContent)
      const rightLines = this.normalizeLines(this.compareResult.rightContent)
      const total = Math.max(leftLines.length, rightLines.length)
      return Array.from({ length: total }, (_, index) => {
        const left = leftLines[index] !== undefined ? leftLines[index] : ''
        const right = rightLines[index] !== undefined ? rightLines[index] : ''
        return {
          left,
          right,
          same: left === right
        }
      })
    },
    sameRowCount() {
      return this.compareRows.filter(item => item.same).length
    },
    changedRowCount() {
      return this.compareRows.filter(item => !item.same).length
    },
    leftVersionMeta() {
      if (!this.compareResult || !this.compareResult.leftCreatedAt) return '-'
      return this.formatTime(this.compareResult.leftCreatedAt)
    },
    rightVersionMeta() {
      if (!this.compareResult || !this.compareResult.rightCreatedAt) return '-'
      return this.formatTime(this.compareResult.rightCreatedAt)
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
    normalizeLines(content) {
      return String(content || '').replace(/\r\n?/g, '\n').split('\n')
    },
    resetCompare() {
      this.leftVersionNo = null
      this.rightVersionNo = null
      this.compareResult = null
    },
    async loadVersions() {
      if (!this.doc || !this.doc.id) {
        this.versions = []
        this.selectedVersion = null
        this.resetCompare()
        return
      }
      try {
        this.loading = true
        const res = await listProjectDocVersions(this.doc.id)
        this.versions = Array.isArray(res?.data) ? res.data : []
        this.selectedVersion = this.versions.length ? this.versions[0] : null
        this.resetCompare()
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
    async handleSelectLeftVersion(row) {
      if (!row) return
      this.leftVersionNo = row.versionNo
      await this.tryAutoCompare()
    },
    async handleSelectRightVersion(row) {
      if (!row) return
      this.rightVersionNo = row.versionNo
      await this.tryAutoCompare()
    },
    async useCurrentAndCompare(row) {
      if (!row || !this.doc || !this.doc.currentVersion) return
      this.leftVersionNo = row.versionNo
      this.rightVersionNo = this.doc.currentVersion
      await this.loadVersionCompare()
    },
    async tryAutoCompare() {
      if (this.canLoadCompare) {
        await this.loadVersionCompare()
      }
    },
    async loadVersionCompare() {
      if (!this.canLoadCompare) return
      try {
        this.compareLoading = true
        const res = await compareProjectDocVersions(this.doc.id, this.leftVersionNo, this.rightVersionNo)
        this.compareResult = res?.data || null
      } catch (e) {
        this.compareResult = null
        const m = e.response?.data?.message || e.response?.data?.msg || '加载版本对比失败'
        this.$message.error(m)
      } finally {
        this.compareLoading = false
      }
    },
    rollback(row) {
      if (!row || !this.doc || !this.doc.id) return
      this.$confirm(`确定回滚到版本 v${row.versionNo} 吗？回滚后会生成一个新的当前版本。`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await rollbackProjectDocVersion(this.doc.id, row.versionNo)
          const latestDoc = res?.data || null
          this.$message.success(`已回滚到版本 v${row.versionNo}`)
          this.$emit('rolled-back', {
            docId: this.doc.id,
            versionNo: row.versionNo,
            latestDoc
          })
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
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 16px;
}

.history-header,
.preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.history-header-actions,
.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.history-title {
  font-size: 16px;
  font-weight: 600;
}

.history-subtitle,
.preview-subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.version-no-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.compare-actions {
  display: flex;
  gap: 6px;
}

.preview-box {
  margin: 0;
  min-height: 548px;
  max-height: 548px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  padding: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, Monaco, monospace;
  line-height: 1.6;
}

.compare-shell {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.compare-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
  background: #f8fafc;
}

.compare-body {
  max-height: 490px;
  overflow: auto;
}

.compare-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.compare-grid-head {
  border-bottom: 1px solid #ebeef5;
  background: #fafcff;
}

.compare-col-head {
  padding: 10px 12px;
  border-right: 1px solid #ebeef5;
  font-weight: 600;
  color: #303133;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compare-col-head:last-child {
  border-right: none;
}

.compare-col-meta {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.compare-row {
  border-bottom: 1px solid #f0f2f5;
}

.compare-row.is-diff {
  background: #fffaf2;
}

.compare-col {
  display: flex;
  align-items: stretch;
  min-width: 0;
  border-right: 1px solid #f0f2f5;
}

.compare-col:last-child {
  border-right: none;
}

.compare-col.is-empty {
  background: #fcfcfc;
}

.compare-line-no {
  flex: 0 0 52px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 10px 10px 10px 8px;
  color: #a0a8b7;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.02);
  border-right: 1px solid #f0f2f5;
  user-select: none;
}

.compare-line-text {
  margin: 0;
  padding: 10px 12px;
  flex: 1;
  min-width: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, Monaco, monospace;
  line-height: 1.6;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .history-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .compare-grid {
    grid-template-columns: 1fr;
  }

  .compare-col,
  .compare-col-head {
    border-right: none;
  }

  .compare-col-head:first-child {
    border-bottom: 1px solid #ebeef5;
  }
}
</style>
