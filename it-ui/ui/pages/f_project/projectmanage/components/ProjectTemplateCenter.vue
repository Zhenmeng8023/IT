<template>
  <div class="template-center">
    <el-card shadow="never">
      <div slot="header" class="template-toolbar">
        <div class="template-toolbar-left">
          <el-input
            v-model="keyword"
            size="small"
            clearable
            placeholder="搜索模板名称/描述"
            class="template-keyword"
            @change="loadTemplates"
            @clear="loadTemplates"
            @keyup.enter.native="loadTemplates"
          />
          <el-checkbox v-model="mineOnly" @change="loadTemplates">仅看我的</el-checkbox>
        </div>
        <div class="template-toolbar-right">
          <el-button
            v-if="canManageProject && projectId"
            size="small"
            icon="el-icon-folder-add"
            @click="openSaveCurrentProject"
          >
            保存当前项目为模板
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="tableLoading"
        :data="templateList"
        border
        size="small"
        empty-text="暂无模板"
      >
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="creatorName" label="创建人" width="120" />
        <el-table-column label="公开" width="90">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.isPublic ? 'success' : 'info'">
              {{ scope.row.isPublic ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模板内容" min-width="320">
          <template slot-scope="scope">
            <div class="counts-line">
              <el-tag size="mini" type="success">文档 {{ scope.row.docCount || 0 }}</el-tag>
              <el-tag size="mini" type="warning">任务 {{ scope.row.taskCount || 0 }}</el-tag>
              <el-tag size="mini">文件 {{ scope.row.fileCount || 0 }}</el-tag>
              <el-tag size="mini" type="info">目录 {{ scope.row.folderCount || 0 }}</el-tag>
              <el-tag size="mini" type="danger">活动流 {{ scope.row.activityCount || 0 }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" min-width="220" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" @click="handleDetail(scope.row)">详情</el-button>
            <el-button
              v-if="isOwner(scope.row)"
              size="mini"
              type="danger"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!tableLoading && templateList.length === 0" description="暂无模板" />
    </el-card>

    <el-dialog title="模板详情" :visible.sync="detailVisible" width="980px" top="5vh">
      <div v-loading="detailLoading">
        <div v-if="detailData && detailData.id" class="detail-wrap">
          <div class="detail-header">
            <div>
              <h3>{{ detailData.name || '-' }}</h3>
              <p class="template-detail-desc">{{ detailData.description || '暂无描述' }}</p>
              <div class="template-detail-meta">
                分类：{{ detailData.category || '-' }} ｜ 创建人：{{ detailData.creatorName || '-' }}
              </div>
            </div>
            <div class="counts-line">
              <el-tag size="mini" type="success">文档 {{ detailData.docCount || 0 }}</el-tag>
              <el-tag size="mini" type="warning">任务 {{ detailData.taskCount || 0 }}</el-tag>
              <el-tag size="mini">文件 {{ detailData.fileCount || 0 }}</el-tag>
              <el-tag size="mini" type="info">目录 {{ detailData.folderCount || 0 }}</el-tag>
              <el-tag size="mini" type="danger">活动流 {{ detailData.activityCount || 0 }}</el-tag>
            </div>
          </div>

          <div v-if="detailData.readmeContent" class="detail-readme">
            <div class="detail-readme-title">{{ detailData.readmeTitle || 'README' }}</div>
            <pre class="detail-readme-content">{{ detailData.readmeContent }}</pre>
          </div>

          <el-tabs>
            <el-tab-pane :label="`文档 (${(detailData.docItems || []).length})`">
              <el-table :data="detailData.docItems || []" border size="mini" max-height="280">
                <el-table-column prop="fileName" label="标题" min-width="200" />
                <el-table-column prop="previewText" label="摘要" min-width="320" show-overflow-tooltip />
                <el-table-column prop="filePath" label="路径" min-width="220" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`任务 (${(detailData.taskItems || []).length})`">
              <el-table :data="detailData.taskItems || []" border size="mini" max-height="280">
                <el-table-column prop="fileName" label="标题" min-width="200" />
                <el-table-column prop="previewText" label="说明" min-width="320" show-overflow-tooltip />
                <el-table-column prop="filePath" label="路径" min-width="220" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`文件/目录 (${(detailData.fileItems || []).length + (detailData.folderItems || []).length})`">
              <el-table
                :data="[...(detailData.folderItems || []), ...(detailData.fileItems || [])]"
                border
                size="mini"
                max-height="280"
              >
                <el-table-column prop="itemType" label="类型" width="120" />
                <el-table-column prop="fileName" label="名称" min-width="180" />
                <el-table-column prop="filePath" label="路径" min-width="320" show-overflow-tooltip />
                <el-table-column label="内容" width="100">
                  <template slot-scope="scope">
                    <el-tag size="mini" :type="scope.row.includeContent ? 'success' : 'info'">
                      {{ scope.row.includeContent ? '已保存' : '仅结构' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`活动流 (${(detailData.activityItems || []).length})`">
              <el-table :data="detailData.activityItems || []" border size="mini" max-height="280">
                <el-table-column prop="fileName" label="摘要" min-width="260" />
                <el-table-column prop="previewText" label="预览" min-width="320" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="时间" width="180" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>

        <el-empty v-else-if="!detailLoading" description="暂无模板详情" />
      </div>
    </el-dialog>

    <ProjectTemplateSaveDialog
      :visible.sync="saveVisible"
      :project-id="projectId"
      :default-name="defaultProjectName"
      :default-category="defaultProjectCategory"
      :default-description="defaultProjectDescription"
      @saved="handleSaved"
    />
  </div>
</template>

<script>
import {
  listProjectTemplates,
  getProjectTemplateDetail,
  deleteProjectTemplate
} from '@/api/projectTemplate'
import ProjectTemplateSaveDialog from './ProjectTemplateSaveDialog.vue'

export default {
  name: 'ProjectTemplateCenter',
  components: {
    ProjectTemplateSaveDialog
  },
  props: {
    projectId: {
      type: [Number, String],
      default: null
    },
    currentUserId: {
      type: [Number, String],
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    defaultProjectName: {
      type: String,
      default: ''
    },
    defaultProjectCategory: {
      type: String,
      default: ''
    },
    defaultProjectDescription: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      keyword: '',
      mineOnly: false,
      tableLoading: false,
      detailLoading: false,
      templateList: [],
      detailVisible: false,
      saveVisible: false,
      detailData: this.createEmptyDetail()
    }
  },
  mounted() {
    this.loadTemplates()
  },
  methods: {
    createEmptyDetail() {
      return {
        id: null,
        name: '',
        description: '',
        category: '',
        creatorId: null,
        creatorName: '',
        isPublic: false,
        docCount: 0,
        taskCount: 0,
        fileCount: 0,
        folderCount: 0,
        activityCount: 0,
        readmeTitle: '',
        readmeContent: '',
        savedFileSuffixes: [],
        docItems: [],
        taskItems: [],
        fileItems: [],
        folderItems: [],
        activityItems: []
      }
    },
    extractPayload(res) {
      if (res && typeof res === 'object') {
        if (res.data && typeof res.data === 'object' && res.data.data !== undefined) {
          return res.data.data
        }
        if (res.data !== undefined) {
          return res.data
        }
      }
      return res
    },
    normalizeTemplateRow(row) {
      const a = row || {}
      const docItems = Array.isArray(a.docItems) ? a.docItems : []
      const taskItems = Array.isArray(a.taskItems) ? a.taskItems : []
      const fileItems = Array.isArray(a.fileItems) ? a.fileItems : []
      const folderItems = Array.isArray(a.folderItems) ? a.folderItems : []
      const activityItems = Array.isArray(a.activityItems) ? a.activityItems : []

      return {
        ...a,
        id: a.id || null,
        name: a.name || '',
        description: a.description || '',
        category: a.category || '',
        creatorId: a.creatorId || null,
        creatorName: a.creatorName || '',
        isPublic: !!(a.isPublic || a.publicFlag || a.visibility === 'public'),
        docCount: Number(a.docCount != null ? a.docCount : docItems.length) || 0,
        taskCount: Number(a.taskCount != null ? a.taskCount : taskItems.length) || 0,
        fileCount: Number(a.fileCount != null ? a.fileCount : fileItems.length) || 0,
        folderCount: Number(a.folderCount != null ? a.folderCount : folderItems.length) || 0,
        activityCount: Number(a.activityCount != null ? a.activityCount : activityItems.length) || 0,
        updatedAt: a.updatedAt || a.updateTime || a.createTime || ''
      }
    },
    normalizeTemplateList(payload) {
      if (Array.isArray(payload)) {
        return payload.map(this.normalizeTemplateRow)
      }
      const a = payload || {}
      const list =
        (Array.isArray(a.records) && a.records) ||
        (Array.isArray(a.list) && a.list) ||
        (Array.isArray(a.items) && a.items) ||
        (Array.isArray(a.rows) && a.rows) ||
        []
      return list.map(this.normalizeTemplateRow)
    },
    normalizeTemplateDetail(payload) {
      const empty = this.createEmptyDetail()
      const a = payload || {}
      const base = a.template && typeof a.template === 'object' ? a.template : a

      let docItems = Array.isArray(a.docItems) ? a.docItems : []
      let taskItems = Array.isArray(a.taskItems) ? a.taskItems : []
      let fileItems = Array.isArray(a.fileItems) ? a.fileItems : []
      let folderItems = Array.isArray(a.folderItems) ? a.folderItems : []
      let activityItems = Array.isArray(a.activityItems) ? a.activityItems : []

      if ((!docItems.length && !taskItems.length && !fileItems.length && !folderItems.length && !activityItems.length) && Array.isArray(a.items)) {
        docItems = a.items.filter(item => item && item.itemType === 'doc')
        taskItems = a.items.filter(item => item && item.itemType === 'task')
        fileItems = a.items.filter(item => item && item.itemType === 'file')
        folderItems = a.items.filter(item => item && item.itemType === 'folder')
        activityItems = a.items.filter(item => item && item.itemType === 'activity')
      }

      return {
        ...empty,
        ...this.normalizeTemplateRow(base),
        readmeTitle: a.readmeTitle || base.readmeTitle || '',
        readmeContent: a.readmeContent || base.readmeContent || '',
        savedFileSuffixes: Array.isArray(a.savedFileSuffixes)
          ? a.savedFileSuffixes
          : Array.isArray(base.savedFileSuffixes)
            ? base.savedFileSuffixes
            : [],
        docItems,
        taskItems,
        fileItems,
        folderItems,
        activityItems,
        docCount: Number(base.docCount != null ? base.docCount : docItems.length) || 0,
        taskCount: Number(base.taskCount != null ? base.taskCount : taskItems.length) || 0,
        fileCount: Number(base.fileCount != null ? base.fileCount : fileItems.length) || 0,
        folderCount: Number(base.folderCount != null ? base.folderCount : folderItems.length) || 0,
        activityCount: Number(base.activityCount != null ? base.activityCount : activityItems.length) || 0
      }
    },
    async loadTemplates() {
      this.tableLoading = true
      try {
        const res = await listProjectTemplates({
          keyword: this.keyword,
          mineOnly: this.mineOnly
        })
        const payload = this.extractPayload(res)
        this.templateList = this.normalizeTemplateList(payload)
      } catch (e) {
        this.templateList = []
        this.$message.error('加载模板列表失败')
      } finally {
        this.tableLoading = false
      }
    },
    isOwner(row) {
      return `${row && row.creatorId ? row.creatorId : ''}` === `${this.currentUserId || ''}`
    },
    openSaveCurrentProject() {
      this.saveVisible = true
    },
    async loadDetailById(id) {
      this.detailLoading = true
      try {
        const res = await getProjectTemplateDetail(id)
        const payload = this.extractPayload(res)
        this.detailData = this.normalizeTemplateDetail(payload)
        return this.detailData
      } catch (e) {
        this.detailData = this.createEmptyDetail()
        this.$message.error('加载模板详情失败')
        throw e
      } finally {
        this.detailLoading = false
      }
    },
    async handleDetail(row) {
      this.detailVisible = true
      this.detailData = this.createEmptyDetail()
      await this.loadDetailById(row.id)
    },
    handleSaved() {
      this.loadTemplates()
      this.$emit('template-saved')
    },
    async handleDelete(row) {
      try {
        await this.$confirm('确定删除这个模板吗？删除后无法恢复。', '提示', { type: 'warning' })
        await deleteProjectTemplate(row.id)
        this.$message.success('删除成功')
        this.loadTemplates()
      } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
          this.$message.error('删除模板失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.template-center {
  width: 100%;
}
.template-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.template-toolbar-left,
.template-toolbar-right,
.counts-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.template-keyword {
  width: 240px;
}
.detail-wrap {
  min-height: 120px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.template-detail-desc {
  margin: 0 0 8px;
  color: #606266;
}
.template-detail-meta {
  color: #909399;
  margin-bottom: 8px;
}
.detail-readme {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.detail-readme-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.detail-readme-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 220px;
  overflow: auto;
}
</style>
