<template>
  <div class="workspace-diff-panel">
    <el-card shadow="never" class="panel-card">
      <div slot="header" class="panel-header">
        <span>工作区变更</span>
        <div class="header-actions">
          <el-button
            size="mini"
            :disabled="!selectedIds.length"
            @click="handleStage"
          >
            暂存选中
          </el-button>
          <el-button
            size="mini"
            :disabled="!selectedIds.length"
            @click="handleUnstage"
          >
            取消暂存
          </el-button>
        </div>
      </div>

      <div class="diff-body">
        <el-table
          v-loading="loading"
          :data="workspaceDiff"
          height="320"
          border
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column label="变更类型" width="110">
            <template slot-scope="{ row }">
              <el-tag :type="changeTagType(row.changeType)" size="mini">
                {{ row.changeType || 'UNKNOWN' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="路径" min-width="280">
            <template slot-scope="{ row }">
              <div class="path-cell">
                <div class="path-main">{{ row.newPath || row.canonicalPath || row.oldPath || '-' }}</div>
                <div v-if="row.oldPath && row.newPath && row.oldPath !== row.newPath" class="path-sub">
                  原路径：{{ row.oldPath }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template slot-scope="{ row }">
              <el-tag v-if="row.conflictFlag" type="danger" size="mini">冲突</el-tag>
              <el-tag v-else-if="row.stagedFlag" type="success" size="mini">已暂存</el-tag>
              <el-tag v-else size="mini">未暂存</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="180">
            <template slot-scope="{ row }">
              <span>{{ row.detectedMessage || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="88" fixed="right">
            <template slot-scope="{ row }">
              <el-button type="text" size="mini" @click="handleRemove(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="commit-box">
          <div class="commit-title">提交本次工作区变更</div>
          <el-form :model="commitForm" label-width="72px" size="small">
            <el-form-item label="提交说明">
              <el-input
                v-model="commitForm.message"
                maxlength="200"
                show-word-limit
                placeholder="例如：修复登录页按钮状态同步问题"
              />
            </el-form-item>
            <el-form-item label="补充描述">
              <el-input
                v-model="commitForm.description"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="可选，描述本次提交涉及的范围、原因和影响"
              />
            </el-form-item>
          </el-form>

          <div class="commit-actions">
            <div class="commit-tip">
              当前工作区：{{ workspace && workspace.id ? ('#' + workspace.id) : '未生成' }}
            </div>
            <el-button
              type="primary"
              :disabled="!canCommit"
              @click="handleCommit"
            >
              提交到当前分支
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'WorkspaceDiffPanel',
  props: {
    workspace: {
      type: Object,
      default: null
    },
    workspaceDiff: {
      type: Array,
      default: function () {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      selectedIds: [],
      commitForm: {
        message: '',
        description: ''
      }
    }
  },
  computed: {
    canCommit() {
      return !!(this.workspace && this.workspace.id && this.workspaceDiff.length && this.commitForm.message.trim())
    }
  },
  methods: {
    handleSelectionChange(rows) {
      this.selectedIds = (rows || []).map(function (item) {
        return item.id
      })
    },
    changeTagType(type) {
      const map = {
        ADD: 'success',
        MODIFY: 'warning',
        DELETE: 'danger',
        RENAME: '',
        MOVE: 'info',
        REVERT: 'info'
      }
      return map[type] || 'info'
    },
    handleStage() {
      this.$emit('stage-selected', this.selectedIds.slice())
    },
    handleUnstage() {
      this.$emit('unstage-selected', this.selectedIds.slice())
    },
    handleRemove(row) {
      this.$emit('remove-item', row.id)
    },
    handleCommit() {
      this.$emit('commit', {
        message: this.commitForm.message,
        description: this.commitForm.description,
        itemIds: this.selectedIds.length ? this.selectedIds.slice() : this.workspaceDiff.map(function (item) { return item.id })
      })
      this.commitForm.message = ''
      this.commitForm.description = ''
      this.selectedIds = []
    }
  }
}
</script>

<style scoped>
.workspace-diff-panel {
  height: 100%;
}
.panel-card {
  height: 100%;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.diff-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.path-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.path-main {
  color: #303133;
  word-break: break-all;
}
.path-sub {
  color: #909399;
  font-size: 12px;
  word-break: break-all;
}
.commit-box {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px;
  background: #fff;
}
.commit-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}
.commit-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.commit-tip {
  color: #909399;
  font-size: 13px;
}
</style>
