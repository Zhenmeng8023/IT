<template>
  <div class="commit-history-panel">
    <el-card shadow="never" class="panel-card">
      <div slot="header" class="panel-header">
        <span>提交历史</span>
        <el-button
          size="mini"
          :disabled="compareIds.length !== 2"
          @click="handleCompare"
        >
          比较所选提交
        </el-button>
      </div>

      <div class="history-body">
        <el-empty v-if="!loading && !commitList.length" description="当前分支还没有提交记录" />

        <div v-else class="commit-list">
          <div
            v-for="item in commitList"
            :key="item.id"
            class="commit-item"
            :class="{ active: String(item.id) === String(selectedCommitId) }"
          >
            <div class="commit-main" @click="handleSelect(item)">
              <div class="commit-top">
                <div class="commit-message">{{ item.message || '无提交说明' }}</div>
                <el-tag size="mini" effect="plain">{{ item.displaySha || ('#' + item.commitNo) }}</el-tag>
              </div>
              <div class="commit-meta">
                <span>提交人：{{ item.operatorName || item.operatorId || '-' }}</span>
                <span>时间：{{ item.createdAt || '-' }}</span>
              </div>
            </div>

            <div class="commit-actions">
              <el-checkbox
                :value="compareIds.indexOf(item.id) > -1"
                @change="toggleCompare(item)"
              >
                用于比较
              </el-checkbox>
              <el-button type="text" size="mini" @click.stop="handleRevert(item)">
                回退
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-box">
          <i class="el-icon-loading"></i>
          <span>正在加载提交历史...</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'CommitHistoryPanel',
  props: {
    commitList: {
      type: Array,
      default: function () {
        return []
      }
    },
    loading: {
      type: Boolean,
      default: false
    },
    selectedCommitId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      compareIds: []
    }
  },
  methods: {
    handleSelect(item) {
      this.$emit('select-commit', item.id)
    },
    toggleCompare(item) {
      const index = this.compareIds.indexOf(item.id)
      if (index > -1) {
        this.compareIds.splice(index, 1)
      } else {
        if (this.compareIds.length >= 2) {
          this.compareIds.shift()
        }
        this.compareIds.push(item.id)
      }
    },
    handleCompare() {
      if (this.compareIds.length !== 2) return
      this.$emit('compare-commits', {
        fromCommitId: this.compareIds[0],
        toCommitId: this.compareIds[1]
      })
    },
    handleRevert(item) {
      this.$emit('revert-commit', item.id)
    }
  }
}
</script>

<style scoped>
.commit-history-panel {
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
.history-body {
  min-height: 520px;
  position: relative;
}
.commit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.commit-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  transition: all .2s;
}
.commit-item.active {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64,158,255,.08);
}
.commit-main {
  cursor: pointer;
}
.commit-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.commit-message {
  font-weight: 600;
  color: #303133;
}
.commit-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #909399;
  font-size: 13px;
}
.commit-actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.loading-box {
  position: absolute;
  inset: 0;
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  color: #409eff;
}
</style>
