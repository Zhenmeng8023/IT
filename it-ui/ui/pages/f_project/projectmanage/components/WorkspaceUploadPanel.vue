<template>
  <div class="workspace-upload-panel">
    <el-card shadow="never" class="panel-card">
      <div slot="header" class="panel-header">
        <span>工作区上传</span>
        <el-tag size="mini" type="success">{{ currentBranchName }}</el-tag>
      </div>

      <div class="panel-body">
        <div class="upload-block">
          <div class="upload-title">上传单个文件</div>
          <el-upload
            drag
            action=""
            :show-file-list="false"
            :auto-upload="false"
            :before-upload="beforeUploadFile"
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">拖拽文件到这里，或 <em>点击上传</em></div>
            <div slot="tip" class="el-upload__tip">文件将先进入工作区，不会直接覆盖主线</div>
          </el-upload>
        </div>

        <div class="upload-block zip-block">
          <div class="upload-title">上传 ZIP 包</div>
          <el-upload
            drag
            action=""
            accept=".zip,application/zip,application/x-zip-compressed"
            :show-file-list="false"
            :auto-upload="false"
            :before-upload="beforeUploadZip"
          >
            <i class="el-icon-folder-opened"></i>
            <div class="el-upload__text">拖拽 ZIP 到这里，或 <em>点击上传</em></div>
            <div slot="tip" class="el-upload__tip">适合一次导入多个项目文件</div>
          </el-upload>
        </div>

        <div class="tips">
          <div class="tip-item">
            <i class="el-icon-info"></i>
            <span>上传后会先进入工作区，确认无误再提交。</span>
          </div>
          <div class="tip-item">
            <i class="el-icon-warning-outline"></i>
            <span>相同路径相同文件名的变更会进入版本链路，而不是直接覆盖正式版本。</span>
          </div>
        </div>
      </div>

      <div v-if="loading" class="panel-mask">
        <i class="el-icon-loading"></i>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'WorkspaceUploadPanel',
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    currentBranch: {
      type: Object,
      default: null
    }
  },
  computed: {
    currentBranchName() {
      return this.currentBranch && this.currentBranch.name ? this.currentBranch.name : '未选择分支'
    }
  },
  methods: {
    beforeUploadFile(file) {
      this.$emit('upload-file', file)
      return false
    },
    beforeUploadZip(file) {
      this.$emit('upload-zip', file)
      return false
    }
  }
}
</script>

<style scoped>
.workspace-upload-panel {
  height: 100%;
}
.panel-card {
  height: 100%;
  position: relative;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.panel-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.upload-block {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  padding: 12px;
  background: var(--it-panel-bg-strong);
}
.upload-title {
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--it-text);
}
.zip-block {
  margin-top: 4px;
}
.tips {
  border-radius: 10px;
  background: var(--it-panel-bg);
  padding: 12px;
}
.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--it-text-muted);
  line-height: 22px;
}
.tip-item + .tip-item {
  margin-top: 6px;
}
.panel-mask {
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--it-surface-elevated) 82%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--it-accent);
  z-index: 2;
  border-radius: 4px;
}
</style>
