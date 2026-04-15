<template>
  <el-dialog :title="uploadDialog.isVersion ? '提交文件变更到工作区' : '上传进工作区'" :visible.sync="uploadDialog.visible" width="520px" append-to-body>
    <el-form label-width="90px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="uploadDialog.isVersion ? '这次变更会先进入工作区，确认无误后再提交到分支。' : '上传的文件会先进入工作区，不会直接改动主线。'"
        class="upload-workspace-alert"
      />
      <el-form-item label="选择文件">
        <input type="file" :multiple="!uploadDialog.isVersion" @change="handlePickedFile" />
        <div v-if="uploadDialog.isVersion && uploadDialog.file" class="upload-picked-tip">
          已选择：{{ uploadDialog.file.name }}
        </div>
        <div v-else-if="!uploadDialog.isVersion && uploadDialog.files.length" class="upload-picked-list">
          <div class="upload-picked-title">已选择 {{ uploadDialog.files.length }} 个文件：</div>
          <div class="upload-picked-items">
            <span v-for="file in uploadDialog.files" :key="file.name + '_' + file.size" class="upload-picked-item">
              {{ file.name }}
            </span>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="closeUploadDialog">取消</el-button>
      <el-button type="primary" :loading="uploadLoading" @click="submitUpload">加入工作区</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ProjectUploadDialog',
  props: {
    uploadDialog: {
      type: Object,
      default: () => ({ visible: false, isVersion: false, file: null, files: [] })
    },
    uploadLoading: {
      type: Boolean,
      default: false
    },
    handlePickedFile: {
      type: Function,
      default: () => {}
    },
    closeUploadDialog: {
      type: Function,
      default: () => {}
    },
    submitUpload: {
      type: Function,
      default: () => {}
    }
  }
}
</script>
