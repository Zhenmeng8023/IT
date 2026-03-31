<template>
  <div class="task-attachment-panel">
    <div class="panel-card upload-card">
      <el-upload
        action=""
        :show-file-list="false"
        :http-request="handleUploadRequest"
        :before-upload="beforeUpload"
      >
        <el-button type="primary" size="small">上传附件</el-button>
      </el-upload>
      <div class="upload-tip">支持常见文档、图片、压缩包，上传后可预览或下载。</div>
    </div>

    <div v-if="!attachments.length" class="panel-card empty-wrap">
      <el-empty description="暂无附件" />
    </div>

    <div v-else class="attachment-list">
      <div v-for="item in attachments" :key="item.id" class="attachment-card">
        <div class="attachment-main">
          <div class="file-name">{{ item.fileName || '未命名文件' }}</div>
          <div class="file-meta">
            <span>{{ item.fileType || '-' }}</span>
            <span>{{ formatSize(item.fileSizeBytes) }}</span>
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>
        </div>
        <div class="attachment-actions">
          <el-button size="mini" @click="handlePreview(item)">预览</el-button>
          <el-button size="mini" @click="handleDownload(item)">下载</el-button>
          <el-button size="mini" type="danger" plain @click="handleDelete(item)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  deleteTaskAttachment,
  downloadTaskAttachment,
  getTaskAttachments,
  previewTaskAttachment,
  uploadTaskAttachment
} from '@/api/project'

export default {
  name: 'TaskAttachmentPanel',
  props: {
    taskId: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      attachments: []
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) this.loadAttachments()
        else this.attachments = []
      }
    }
  },
  methods: {
    async loadAttachments() {
      if (!this.taskId) return
      const res = await getTaskAttachments(this.taskId)
      this.attachments = Array.isArray(res && res.data) ? res.data : []
    },
    beforeUpload(file) {
      if (!file) return false
      return true
    },
    async handleUploadRequest(option) {
      const file = option && option.file
      if (!file) {
        this.$message.warning('未获取到上传文件')
        return
      }
      try {
        await uploadTaskAttachment(this.taskId, file)
        await this.loadAttachments()
        this.$emit('changed')
        this.$message.success('上传成功')
        if (option && typeof option.onSuccess === 'function') {
          option.onSuccess({}, file)
        }
      } catch (e) {
        if (option && typeof option.onError === 'function') {
          option.onError(e)
        }
        throw e
      }
    },
    async handleDelete(item) {
      try {
        await this.$confirm('确认删除该附件吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      await deleteTaskAttachment(item.id)
      await this.loadAttachments()
      this.$emit('changed')
      this.$message.success('删除成功')
    },
    async handleDownload(item) {
      const blob = await downloadTaskAttachment(item.id)
      this.downloadBlob(blob, item.fileName || 'attachment')
    },
    async handlePreview(item) {
      try {
        const blob = await previewTaskAttachment(item.id)
        const url = window.URL.createObjectURL(blob)
        window.open(url, '_blank')
        setTimeout(function() {
          window.URL.revokeObjectURL(url)
        }, 60000)
      } catch (e) {
        this.$message.warning('当前附件暂不支持在线预览，已建议使用下载')
      }
    },
    downloadBlob(blobData, fileName) {
      const blob = blobData instanceof Blob ? blobData : new Blob([blobData])
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString()
    },
    formatSize(value) {
      const size = Number(value)
      if (!size || size <= 0) return '-'
      if (size < 1024) return size + ' B'
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
      if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB'
      return (size / 1024 / 1024 / 1024).toFixed(1) + ' GB'
    }
  }
}
</script>

<style scoped>
.task-attachment-panel { display: flex; flex-direction: column; gap: 16px; }
.panel-card, .attachment-card { border: 1px solid #ebeef5; border-radius: 12px; background: #fff; padding: 12px; }
.upload-card { display: flex; align-items: center; gap: 12px; }
.upload-tip { font-size: 12px; color: #909399; }
.attachment-list { display: flex; flex-direction: column; gap: 12px; }
.attachment-card { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.file-name { font-weight: 600; color: #303133; }
.file-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 6px; font-size: 12px; color: #909399; }
.attachment-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.empty-wrap { padding: 20px 12px; }
</style>
