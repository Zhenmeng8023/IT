<template>
  <el-dialog title="从模板创建项目" :visible.sync="dialogVisible" width="1120px" top="4vh">
    <el-form :model="form" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="模板名称">
            <el-input :value="template && template.name" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="项目名称">
            <el-input v-model="form.projectName" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="可见性">
            <el-select v-model="form.visibility" style="width: 100%">
              <el-option label="公开" value="public" />
              <el-option label="仅好友" value="friends_only" />
              <el-option label="私有" value="private" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="项目描述">
        <el-input v-model="form.projectDescription" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="项目分类">
        <el-input v-model="form.category" />
      </el-form-item>
    </el-form>

    <el-alert
      v-if="template"
      :title="`模板快照：文档 ${template.docCount || 0}，任务 ${template.taskCount || 0}，文件 ${template.fileCount || 0}，目录 ${template.folderCount || 0}，活动流 ${template.activityCount || 0}`"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 14px;"
    />

    <el-card shadow="never" class="section-card">
      <div slot="header" class="section-title-row">
        <span>文件内容</span>
        <el-switch v-model="form.applyFiles" active-text="应用文件" />
      </div>
      <div v-if="form.applyFiles">
        <el-form :model="form" label-width="120px" size="small">
          <el-form-item label="应用模式">
            <el-radio-group v-model="form.fileMode">
              <el-radio label="structure_only">只用模板里的目录/文件结构</el-radio>
              <el-radio label="structure_and_content">使用模板保存的文件内容</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="后缀筛选">
            <el-checkbox-group v-model="form.fileSuffixes">
              <el-checkbox v-for="suffix in savedFileSuffixes" :key="suffix" :label="suffix">.{{ suffix }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
        <el-table ref="applyFileTable" :data="allFileRows" border size="mini" max-height="240" @selection-change="onFileSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column label="类型" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.itemType === 'folder' ? 'warning' : ''">{{ scope.row.itemType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileName" label="名称" min-width="180" />
          <el-table-column prop="filePath" label="路径" min-width="280" show-overflow-tooltip />
          <el-table-column prop="fileExt" label="后缀" width="100" />
          <el-table-column label="内容" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.includeContent ? 'success' : 'info'">{{ scope.row.includeContent ? '已保存' : '仅结构' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <div slot="header" class="section-title-row">
        <span>文档</span>
        <el-switch v-model="form.applyDocs" active-text="应用文档" />
      </div>
      <div v-if="form.applyDocs">
        <el-form :model="form" label-width="120px" size="small">
          <el-form-item label="文档选项">
            <el-checkbox v-model="form.applyReadme">包含 README</el-checkbox>
            <el-checkbox v-model="form.applyDocVersionHistory">导入版本历史</el-checkbox>
          </el-form-item>
        </el-form>
        <el-table ref="applyDocTable" :data="template.docItems || []" border size="mini" max-height="220" @selection-change="onDocSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="fileName" label="标题" min-width="200" />
          <el-table-column prop="previewText" label="摘要" min-width="320" show-overflow-tooltip />
          <el-table-column prop="filePath" label="路径" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <div slot="header" class="section-title-row">
        <span>任务</span>
        <el-switch v-model="form.applyTasks" active-text="应用任务" />
      </div>
      <div v-if="form.applyTasks">
        <el-form :model="form" label-width="120px" size="small">
          <el-form-item label="任务选项">
            <el-checkbox v-model="form.applyTaskDescription">使用任务描述</el-checkbox>
            <el-checkbox v-model="form.applyTaskChecklist">带入 checklist 标记</el-checkbox>
            <el-checkbox v-model="form.applyTaskAttachments">带入附件标记</el-checkbox>
            <el-checkbox v-model="form.applyTaskDependencies">带入依赖标记</el-checkbox>
          </el-form-item>
        </el-form>
        <el-table ref="applyTaskTable" :data="template.taskItems || []" border size="mini" max-height="220" @selection-change="onTaskSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="fileName" label="标题" min-width="200" />
          <el-table-column prop="previewText" label="说明" min-width="320" show-overflow-tooltip />
          <el-table-column prop="filePath" label="路径" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <div slot="header" class="section-title-row">
        <span>活动流</span>
        <el-switch v-model="form.applyActivities" active-text="应用活动流" />
      </div>
      <div v-if="form.applyActivities">
        <el-form :model="form" label-width="120px" size="small">
          <el-form-item label="导入方式">
            <el-radio-group v-model="form.activityMode">
              <el-radio label="import_as_template_history">作为“模板历史”导入</el-radio>
              <el-radio label="import_as_new_log">作为新项目初始化日志导入</el-radio>
              <el-radio label="skip">不导入</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <el-table ref="applyActivityTable" :data="template.activityItems || []" border size="mini" max-height="220" @selection-change="onActivitySelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="fileName" label="摘要" min-width="260" />
          <el-table-column prop="previewText" label="预览" min-width="320" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
      </div>
    </el-card>

    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitApply">创建项目</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { applyProjectTemplate } from '@/api/projectTemplate'

export default {
  name: 'ProjectTemplateApplyDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    template: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      dialogVisible: false,
      saving: false,
      form: this.createDefaultForm()
    }
  },
  computed: {
    savedFileSuffixes() {
      return Array.isArray(this.template && this.template.savedFileSuffixes) ? this.template.savedFileSuffixes : []
    },
    allFileRows() {
      if (!this.template) return []
      return [...(this.template.folderItems || []), ...(this.template.fileItems || [])]
    },
    hasSavedFileContent() {
      return this.allFileRows.some(item => item && item.itemType === 'file' && item.includeContent)
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.dialogVisible = val
        if (val) {
          this.resetForm()
          this.$nextTick(() => {
            this.selectAll('applyDocTable', this.template && this.template.docItems)
            this.selectAll('applyTaskTable', this.template && this.template.taskItems)
            this.selectAll('applyFileTable', this.allFileRows)
            this.selectAll('applyActivityTable', this.template && this.template.activityItems)
          })
        }
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val)
    }
  },
  methods: {
    createDefaultForm() {
      return {
        projectName: '',
        projectDescription: '',
        category: '',
        visibility: 'public',
        applyFiles: true,
        fileMode: 'structure_and_content',
        fileSuffixes: [],
        selectedTemplateFileIds: [],
        createFolders: true,
        applyDocs: true,
        applyReadme: true,
        applyDocVersionHistory: false,
        selectedTemplateDocIds: [],
        applyTasks: true,
        applyTaskDescription: true,
        applyTaskChecklist: false,
        applyTaskAttachments: false,
        applyTaskDependencies: false,
        selectedTemplateTaskIds: [],
        applyActivities: false,
        activityMode: 'skip',
        selectedTemplateActivityIds: []
      }
    },
    resetForm() {
      this.form = this.createDefaultForm()
      if (this.template) {
        this.form.projectName = `${this.template.name || '新项目'}`
        this.form.projectDescription = this.template.description || ''
        this.form.category = this.template.category || ''
        this.form.fileSuffixes = [...this.savedFileSuffixes]

        this.form.applyFiles = this.allFileRows.length > 0
        this.form.applyDocs = Array.isArray(this.template.docItems) && this.template.docItems.length > 0
        this.form.applyTasks = Array.isArray(this.template.taskItems) && this.template.taskItems.length > 0
        this.form.applyActivities = false

        this.form.fileMode = this.hasSavedFileContent ? 'structure_and_content' : 'structure_only'
      }
    },
    selectAll(refName, rows) {
      const table = this.$refs[refName]
      if (!table || !Array.isArray(rows)) return
      rows.forEach(row => table.toggleRowSelection(row, true))
    },
    onDocSelectionChange(rows) {
      this.form.selectedTemplateDocIds = rows.map(item => item.id)
    },
    onTaskSelectionChange(rows) {
      this.form.selectedTemplateTaskIds = rows.map(item => item.id)
    },
    onFileSelectionChange(rows) {
      this.form.selectedTemplateFileIds = rows.map(item => item.id)
    },
    onActivitySelectionChange(rows) {
      this.form.selectedTemplateActivityIds = rows.map(item => item.id)
    },
    async submitApply() {
      if (!this.template || !this.template.id) return
      if (!this.form.projectName) {
        this.$message.warning('请先填写项目名称')
        return
      }
      this.saving = true
      try {
        const res = await applyProjectTemplate(this.template.id, this.form)
        this.$message.success('项目创建成功')
        this.$emit('applied', res?.data || res || null)
        this.dialogVisible = false
      } catch (e) {
        const msg =
          e?.response?.data?.message ||
          e?.response?.data?.msg ||
          e?.message ||
          '套用模板失败'
        this.$message.error(msg)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.section-card {
  margin-bottom: 14px;
}
.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
