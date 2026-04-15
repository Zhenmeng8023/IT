<template>
  <div>
    <el-card shadow="never">
      <div slot="header" class="card-header">
        <span>文件管理</span>
        <div class="toolbar-actions">
          <el-input v-model="fileFilter.keyword" size="small" clearable placeholder="搜索文件名" class="toolbar-input"></el-input>
          <el-button type="danger" size="small" icon="el-icon-delete" :disabled="!selectedFileRows.length" @click="batchDeleteProjectFiles">
            批量删除{{ selectedFileRows.length ? `（${selectedFileRows.length}）` : '' }}
          </el-button>
          <el-button type="primary" size="small" icon="el-icon-upload" @click="openUploadFileDialog">上传进工作区</el-button>
          <el-button size="small" icon="el-icon-refresh" @click="loadFiles">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredFiles" border>
        <el-table-column width="55" align="center">
          <template slot="header">
            <el-checkbox :value="isAllFilteredFilesSelected" :indeterminate="isFileSelectionIndeterminate" @change="toggleAllFileSelection" />
          </template>
          <template slot-scope="scope">
            <el-checkbox :value="isFileSelected(scope.row)" @change="value => toggleFileSelection(scope.row, value)" />
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="240"></el-table-column>
        <el-table-column prop="version" label="当前版本" width="120"></el-table-column>
        <el-table-column prop="fileSizeBytes" label="大小" width="120">
          <template slot-scope="scope">{{ formatFileSize(scope.row.fileSizeBytes) }}</template>
        </el-table-column>
        <el-table-column prop="isMain" label="主文件" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.isMain ? 'success' : 'info'">{{ scope.row.isMain ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.uploadTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="360" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" @click="downloadProjectFile(scope.row)">下载</el-button>
            <el-button size="mini" @click="viewFileVersions(scope.row)">版本</el-button>
            <el-button size="mini" @click="openUploadNewVersionDialog(scope.row)">提交变更</el-button>
            <el-button size="mini" type="warning" :disabled="scope.row.isMain" @click="setMainProjectFile(scope.row)">设主文件</el-button>
            <el-button size="mini" type="danger" @click="deleteProjectFile(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="上传进工作区" :visible.sync="fileUploadDialogVisible" width="520px" @close="resetFileUploadForm">
      <el-form :model="fileUploadForm" label-width="100px">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="文件会先进入当前项目的工作区，不会直接写入主线。上传后请到“仓库工作台”确认并提交。"
          class="dialog-alert"
        />
        <el-form-item label="选择文件"><input type="file" @change="handleUploadFileChange" class="native-file-input"></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="fileUploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fileUploadLoading" @click="submitUploadFile">加入工作区</el-button>
      </span>
    </el-dialog>

    <el-dialog title="文件版本记录" :visible.sync="fileVersionsDialogVisible" width="680px">
      <el-table :data="fileVersions" border v-loading="fileVersionsLoading">
        <el-table-column prop="version" label="版本号" width="120"></el-table-column>
        <el-table-column prop="commitMessage" label="版本说明" min-width="220"></el-table-column>
        <el-table-column prop="fileSizeBytes" label="大小" width="120">
          <template slot-scope="scope">{{ formatFileSize(scope.row.fileSizeBytes) }}</template>
        </el-table-column>
        <el-table-column prop="uploadedAt" label="上传时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.uploadedAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!fileVersionsLoading && fileVersions.length === 0" description="暂无版本记录"></el-empty>
    </el-dialog>

    <el-dialog title="提交文件变更到工作区" :visible.sync="versionDialogVisible" width="520px" @close="resetVersionForm">
      <el-form :model="versionForm" label-width="100px">
        <el-form-item label="当前文件"><div class="dialog-file-name">{{ versionForm.fileName || '-' }}</div></el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="这次变更会以同一路径进入工作区，确认无误后再在仓库工作台提交。"
          class="dialog-alert"
        />
        <el-form-item label="选择文件"><input type="file" @change="handleVersionFileChange" class="native-file-input"></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="versionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="versionLoading" @click="submitUploadNewVersion">加入工作区</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  deleteFile as apiDeleteFile,
  downloadFile as apiDownloadFile,
  listFileVersions,
  listProjectFiles,
  setMainFile as apiSetMainFile,
  uploadFileNewVersion,
  uploadProjectFile
} from '@/api/project'
import {
  formatFileSize,
  formatTime,
  normalizeFile,
  triggerBlobDownload
} from '../services/projectManageShared'

export default {
  name: 'ProjectManageFileTab',
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    refreshSeed: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      files: [],
      fileFilter: { keyword: '' },
      selectedFileRows: [],
      selectedFileIds: [],
      fileUploadDialogVisible: false,
      fileUploadLoading: false,
      fileUploadForm: { file: null },
      fileVersionsDialogVisible: false,
      fileVersionsLoading: false,
      fileVersions: [],
      versionDialogVisible: false,
      versionLoading: false,
      versionForm: { fileId: null, fileName: '', file: null }
    }
  },
  computed: {
    filteredFiles() {
      const keyword = (this.fileFilter.keyword || '').trim().toLowerCase()
      if (!keyword) return this.files
      return this.files.filter(file => (file.fileName || '').toLowerCase().includes(keyword))
    },
    isAllFilteredFilesSelected() {
      return this.filteredFiles.length > 0 && this.filteredFiles.every(file => this.selectedFileIds.includes(Number(file.id)))
    },
    isFileSelectionIndeterminate() {
      const total = this.filteredFiles.length
      if (!total) return false
      const selectedCount = this.filteredFiles.filter(file => this.selectedFileIds.includes(Number(file.id))).length
      return selectedCount > 0 && selectedCount < total
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.loadFiles()
      }
    },
    refreshSeed() {
      this.loadFiles()
    }
  },
  methods: {
    formatFileSize,
    formatTime,
    async loadFiles() {
      if (!this.projectId) {
        this.files = []
        this.$emit('file-count-change', 0)
        return
      }
      try {
        const response = await listProjectFiles(this.projectId)
        this.files = (response.data || []).map(normalizeFile)
        const validIds = new Set(this.files.map(item => Number(item.id)))
        this.selectedFileIds = this.selectedFileIds.filter(id => validIds.has(Number(id)))
        this.syncSelectedFileRows()
        this.$emit('file-count-change', this.files.length)
      } catch (error) {
        this.files = []
        this.$emit('file-count-change', 0)
        this.$message.error(error.response?.data?.message || '加载文件列表失败')
      }
    },
    syncSelectedFileRows() {
      const idSet = new Set(this.selectedFileIds.map(id => Number(id)))
      this.selectedFileRows = this.files.filter(file => idSet.has(Number(file.id)))
    },
    isFileSelected(file) {
      if (!file || file.id === undefined || file.id === null) return false
      return this.selectedFileIds.includes(Number(file.id))
    },
    toggleFileSelection(file, checked) {
      const id = Number(file && file.id)
      if (!id) return
      if (checked) {
        if (!this.selectedFileIds.includes(id)) {
          this.selectedFileIds = [...this.selectedFileIds, id]
        }
      } else {
        this.selectedFileIds = this.selectedFileIds.filter(item => Number(item) !== id)
      }
      this.syncSelectedFileRows()
    },
    toggleAllFileSelection(checked) {
      const ids = this.filteredFiles.map(file => Number(file.id)).filter(Boolean)
      if (checked) {
        const set = new Set([...this.selectedFileIds, ...ids])
        this.selectedFileIds = Array.from(set)
      } else {
        const removeSet = new Set(ids)
        this.selectedFileIds = this.selectedFileIds.filter(id => !removeSet.has(Number(id)))
      }
      this.syncSelectedFileRows()
    },
    clearFileSelection() {
      this.selectedFileRows = []
      this.selectedFileIds = []
    },
    async batchDeleteProjectFiles() {
      if (!this.selectedFileRows.length) {
        this.$message.warning('请先勾选要加入工作区删除的文件')
        return
      }
      const rows = this.selectedFileRows.slice()
      try {
        await this.$confirm(`确定把选中的 ${rows.length} 个文件加入工作区删除吗？正式文件会在后续 Commit 后才移除。`, '提示', { type: 'warning' })
        const results = await Promise.allSettled(rows.map(item => apiDeleteFile(item.id)))
        const successCount = results.filter(item => item.status === 'fulfilled').length
        const failCount = results.length - successCount
        if (successCount > 0) {
          this.$message.success(`已将 ${successCount} 个文件加入工作区删除${failCount ? `，失败 ${failCount} 个` : ''}`)
          this.clearFileSelection()
          this.$emit('switch-tab', 'repo-workbench')
          this.$emit('request-refresh')
        } else {
          this.$message.error('批量加入工作区删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '批量加入工作区删除失败')
        }
      }
    },
    openUploadFileDialog() {
      this.resetFileUploadForm()
      this.fileUploadDialogVisible = true
    },
    resetFileUploadForm() {
      this.fileUploadForm = { file: null }
    },
    handleUploadFileChange(event) {
      this.fileUploadForm.file = event.target.files && event.target.files[0] ? event.target.files[0] : null
    },
    async submitUploadFile() {
      if (!this.fileUploadForm.file) {
        this.$message.warning('请选择要上传的文件')
        return
      }
      this.fileUploadLoading = true
      try {
        const formData = new FormData()
        formData.append('projectId', this.projectId)
        formData.append('file', this.fileUploadForm.file)
        await uploadProjectFile(this.projectId, formData)
        this.$message.success('文件已加入工作区，接下来请在仓库工作台提交')
        this.fileUploadDialogVisible = false
        this.$emit('switch-tab', 'repo-workbench')
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '加入工作区失败')
      } finally {
        this.fileUploadLoading = false
      }
    },
    async viewFileVersions(file) {
      this.fileVersionsDialogVisible = true
      this.fileVersionsLoading = true
      try {
        const response = await listFileVersions(file.id)
        this.fileVersions = response.data || []
      } catch (error) {
        this.$message.error(error.response?.data?.message || '加载版本记录失败')
      } finally {
        this.fileVersionsLoading = false
      }
    },
    openUploadNewVersionDialog(file) {
      this.versionForm = { fileId: file.id, fileName: file.fileName, file: null }
      this.versionDialogVisible = true
    },
    resetVersionForm() {
      this.versionForm = { fileId: null, fileName: '', file: null }
    },
    handleVersionFileChange(event) {
      this.versionForm.file = event.target.files && event.target.files[0] ? event.target.files[0] : null
    },
    async submitUploadNewVersion() {
      if (!this.versionForm.fileId || !this.versionForm.file) {
        this.$message.warning('请选择要上传的新版本文件')
        return
      }
      this.versionLoading = true
      try {
        const formData = new FormData()
        formData.append('file', this.versionForm.file)
        await uploadFileNewVersion(this.versionForm.fileId, formData)
        this.$message.success('变更已加入工作区，接下来请在仓库工作台提交')
        this.versionDialogVisible = false
        this.$emit('switch-tab', 'repo-workbench')
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '加入工作区失败')
      } finally {
        this.versionLoading = false
      }
    },
    async setMainProjectFile(file) {
      try {
        await apiSetMainFile(file.id)
        this.$message.success('已设为主文件')
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '设置主文件失败')
      }
    },
    async deleteProjectFile(file) {
      try {
        await this.$confirm(`确定把文件 ${file.fileName} 加入工作区删除吗？正式文件会在后续 Commit 后才移除。`, '提示', { type: 'warning' })
        await apiDeleteFile(file.id)
        this.$message.success('删除请求已加入工作区，请前往仓库工作台提交')
        this.$emit('switch-tab', 'repo-workbench')
        this.$emit('request-refresh')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '加入工作区删除失败')
        }
      }
    },
    async downloadProjectFile(file) {
      try {
        const blob = await apiDownloadFile(file.id)
        triggerBlobDownload(blob, file.fileName)
        this.$message.success('下载开始')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '下载文件失败')
      }
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar-input { width: 220px; }
.dialog-file-name { color: #303133; font-weight: 600; }
.dialog-alert { margin-bottom: 16px; }
.native-file-input { display: block; width: 100%; }

@media (max-width: 768px) {
  .toolbar-input { width: 100%; }
}
</style>
