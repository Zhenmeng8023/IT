<template>
  <el-dialog
    title="创建项目"
    :visible.sync="dialogVisible"
    width="1120px"
    top="4vh"
    append-to-body
  >
    <div class="create-mode-bar">
      <el-radio-group v-model="mode" size="small">
        <el-radio-button label="blank">空白创建</el-radio-button>
        <el-radio-button label="template">使用模板创建</el-radio-button>
      </el-radio-group>
      <div class="create-mode-tip">
        统一在当前弹窗内完成“空白创建 / 使用模板创建”，模板搜索、选择和可选项配置不再跳转到独立模板中心。
      </div>
    </div>

    <div v-if="mode === 'template'" class="template-flow-section">
      <div class="section-title">1. 选择模板</div>
      <ProjectTemplatePicker @select="handleTemplateSelect" />
    </div>

    <div class="section-card">
      <div class="section-title">{{ mode === 'template' ? '2. 新项目基础信息' : '项目基础信息' }}</div>
      <el-form :model="projectForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目名称">
              <el-input v-model="projectForm.name" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见范围">
              <el-select v-model="projectForm.visibility" style="width: 100%">
                <el-option label="公开" value="public" />
                <el-option label="私有" value="private" />
                              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="项目描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目分类">
              <el-input v-model="projectForm.category" />
            </el-form-item>
          </el-col>
          <el-col v-if="mode === 'blank'" :span="12">
            <el-form-item label="项目标签">
              <el-select
                v-model="projectForm.tags"
                multiple
                filterable
                allow-create
                default-first-option
                style="width: 100%"
              >
                <el-option v-for="tag in tags" :key="tag.id || tag.name" :label="tag.name" :value="tag.name"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <template v-if="mode === 'template'">
      <el-alert
        v-if="selectedTemplate"
        :title="`模板快照：文档 ${selectedTemplate.docCount || 0}，任务 ${selectedTemplate.taskCount || 0}，文件 ${selectedTemplate.fileCount || 0}，目录 ${selectedTemplate.folderCount || 0}，活动流 ${selectedTemplate.activityCount || 0}`"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 14px;"
      />

      <el-card shadow="never" class="section-card">
        <div slot="header" class="section-title-row">
          <span>文件内容</span>
          <el-switch v-model="applyForm.applyFiles" active-text="应用文件" />
        </div>
        <div v-if="applyForm.applyFiles">
          <el-alert
            v-if="hasSavedFileContent"
            title="该模板里已保存真实文件内容。建议保持“使用模板保存的文件内容”，这样套用后的文件预览和下载才能和正常项目一致。"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-alert
            v-else
            title="该模板没有保存真实文件内容，只能恢复结构。"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-form :model="applyForm" label-width="120px" size="small">
            <el-form-item label="应用模式">
              <el-radio-group v-model="applyForm.fileMode">
                <el-radio label="structure_only">只用模板里的目录/文件结构</el-radio>
                <el-radio label="structure_and_content" :disabled="!hasSavedFileContent">使用模板保存的文件内容</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="后缀筛选">
              <el-checkbox-group v-model="applyForm.fileSuffixes">
                <el-checkbox v-for="suffix in savedFileSuffixes" :key="suffix" :label="suffix">.{{ suffix }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
          <el-table ref="applyFileTable" :data="allFileRows" border size="mini" max-height="220" @selection-change="onFileSelectionChange">
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
          <el-switch v-model="applyForm.applyDocs" active-text="应用文档" />
        </div>
        <div v-if="applyForm.applyDocs">
          <el-form :model="applyForm" label-width="120px" size="small">
            <el-form-item label="文档选项">
              <el-checkbox v-model="applyForm.applyReadme">包含 README</el-checkbox>
              <el-checkbox v-model="applyForm.applyDocVersionHistory">导入版本历史</el-checkbox>
            </el-form-item>
          </el-form>
          <el-table ref="applyDocTable" :data="(selectedTemplate && selectedTemplate.docItems) || []" border size="mini" max-height="200" @selection-change="onDocSelectionChange">
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
          <el-switch v-model="applyForm.applyTasks" active-text="应用任务" />
        </div>
        <div v-if="applyForm.applyTasks">
          <el-form :model="applyForm" label-width="120px" size="small">
            <el-form-item label="任务选项">
              <el-checkbox v-model="applyForm.applyTaskDescription">使用任务描述</el-checkbox>
              <el-checkbox v-model="applyForm.applyTaskChecklist">带入 checklist 标记</el-checkbox>
              <el-checkbox v-model="applyForm.applyTaskAttachments">带入附件标记</el-checkbox>
              <el-checkbox v-model="applyForm.applyTaskDependencies">带入依赖标记</el-checkbox>
            </el-form-item>
          </el-form>
          <el-table ref="applyTaskTable" :data="(selectedTemplate && selectedTemplate.taskItems) || []" border size="mini" max-height="200" @selection-change="onTaskSelectionChange">
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
          <el-switch v-model="applyForm.applyActivities" active-text="应用活动流" />
        </div>
        <div v-if="applyForm.applyActivities">
          <el-form :model="applyForm" label-width="120px" size="small">
            <el-form-item label="导入方式">
              <el-radio-group v-model="applyForm.activityMode">
                <el-radio label="import_as_template_history">作为“模板历史”导入</el-radio>
                <el-radio label="import_as_new_log">作为新项目初始化日志导入</el-radio>
                <el-radio label="skip">不导入</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <el-table ref="applyActivityTable" :data="(selectedTemplate && selectedTemplate.activityItems) || []" border size="mini" max-height="200" @selection-change="onActivitySelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="fileName" label="摘要" min-width="260" />
            <el-table-column prop="previewText" label="预览" min-width="320" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
        </div>
      </el-card>
    </template>

    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitCreate">
        {{ mode === 'template' ? '创建项目' : '立即创建' }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { createProject } from '@/api/project'
import { applyProjectTemplate } from '@/api/projectTemplate'
import ProjectTemplatePicker from './ProjectTemplatePicker.vue'

export default {
  name: 'ProjectCreateDialog',
  components: {
    ProjectTemplatePicker
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    initialMode: {
      type: String,
      default: 'blank'
    },
    tags: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      dialogVisible: false,
      mode: 'blank',
      submitting: false,
      selectedTemplate: null,
      projectForm: this.createDefaultProjectForm(),
      applyForm: this.createDefaultApplyForm()
    }
  },
  computed: {
    savedFileSuffixes() {
      return Array.isArray(this.selectedTemplate && this.selectedTemplate.savedFileSuffixes)
        ? this.selectedTemplate.savedFileSuffixes
        : []
    },
    allFileRows() {
      if (!this.selectedTemplate) return []
      return [...(this.selectedTemplate.folderItems || []), ...(this.selectedTemplate.fileItems || [])]
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
          this.mode = this.initialMode === 'template' ? 'template' : 'blank'
          this.resetForms()
        }
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val)
    },
    mode() {
      this.resetProjectByMode()
    }
  },
  methods: {
    createDefaultProjectForm() {
      return {
        name: '',
        description: '',
        category: '',
        visibility: 'public',
        tags: []
      }
    },
    createDefaultApplyForm() {
      return {
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
    resetForms() {
      this.projectForm = this.createDefaultProjectForm()
      this.selectedTemplate = null
      this.applyForm = this.createDefaultApplyForm()
    },
    resetProjectByMode() {
      if (this.mode === 'blank') {
        this.selectedTemplate = null
        this.applyForm = this.createDefaultApplyForm()
        this.projectForm.visibility = 'public'
      }
    },
    handleTemplateSelect(template) {
      this.selectedTemplate = template
      this.applyForm = this.createDefaultApplyForm()
      if (template) {
        this.projectForm.name = template.name || ''
        this.projectForm.description = template.description || ''
        this.projectForm.category = template.category || ''
        this.projectForm.visibility = 'public'
        this.applyForm.fileSuffixes = [...this.savedFileSuffixes]
        this.applyForm.applyFiles = this.allFileRows.length > 0
        this.applyForm.applyDocs = Array.isArray(template.docItems) && template.docItems.length > 0
        this.applyForm.applyTasks = Array.isArray(template.taskItems) && template.taskItems.length > 0
        this.applyForm.applyActivities = false
        this.applyForm.fileMode = this.hasSavedFileContent ? 'structure_and_content' : 'structure_only'
        this.$nextTick(() => {
          this.selectAll('applyDocTable', template.docItems)
          this.selectAll('applyTaskTable', template.taskItems)
          this.selectAll('applyFileTable', this.allFileRows)
          this.selectAll('applyActivityTable', template.activityItems)
        })
      }
    },
    selectAll(refName, rows) {
      const table = this.$refs[refName]
      if (!table || !Array.isArray(rows)) return
      rows.forEach(row => table.toggleRowSelection(row, true))
    },
    onDocSelectionChange(rows) {
      this.applyForm.selectedTemplateDocIds = rows.map(item => item.id)
    },
    onTaskSelectionChange(rows) {
      this.applyForm.selectedTemplateTaskIds = rows.map(item => item.id)
    },
    onFileSelectionChange(rows) {
      this.applyForm.selectedTemplateFileIds = rows.map(item => item.id)
    },
    onActivitySelectionChange(rows) {
      this.applyForm.selectedTemplateActivityIds = rows.map(item => item.id)
    },
    extractCreatedProjectId(payload) {
      return payload?.id || payload?.projectId || payload?.data?.id || payload?.data?.projectId || null
    },
    async submitCreate() {
      if (!this.projectForm.name) {
        this.$message.warning('请先填写项目名称')
        return
      }
      if (this.mode === 'template' && (!this.selectedTemplate || !this.selectedTemplate.id)) {
        this.$message.warning('请先选择一个模板')
        return
      }
      this.submitting = true
      try {
        let payload = null
        if (this.mode === 'blank') {
          const res = await createProject({
            name: this.projectForm.name,
            description: this.projectForm.description || '',
            category: this.projectForm.category || '',
            status: 'published',
            visibility: this.projectForm.visibility || 'public',
            tags: JSON.stringify(this.projectForm.tags || [])
          })
          payload = res?.data || res || null
        } else {
          if (this.applyForm.applyFiles && this.applyForm.fileMode !== 'structure_and_content') {
            try {
              await this.$confirm(
                '当前选择的是“只恢复结构”。这样套用后的文件不能保证像正常项目一样预览和下载。确定继续吗？',
                '提示',
                { type: 'warning' }
              )
            } catch (e) {
              this.submitting = false
              return
            }
          }
          const res = await applyProjectTemplate(this.selectedTemplate.id, {
            ...this.applyForm,
            projectName: this.projectForm.name,
            projectDescription: this.projectForm.description,
            category: this.projectForm.category,
            visibility: this.projectForm.visibility
          })
          payload = res?.data || res || null
        }
        const projectId = this.extractCreatedProjectId(payload)
        this.$message.success('项目创建成功')
        this.$emit('created', payload)
        this.dialogVisible = false
        if (!projectId) {
          this.$emit('created', payload)
        }
      } catch (e) {
        const msg =
          e?.response?.data?.message ||
          e?.response?.data?.msg ||
          e?.message ||
          '创建项目失败'
        this.$message.error(msg)
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.create-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.create-mode-tip {
  color: #909399;
  font-size: 13px;
}
.section-card {
  margin-top: 14px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.template-flow-section {
  margin-bottom: 4px;
}
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
