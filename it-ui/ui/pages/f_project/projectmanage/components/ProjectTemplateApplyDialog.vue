<template>
  <el-dialog title="从模板创建项目" :visible.sync="dialogVisible" width="1120px" top="4vh" class="template-apply-dialog">
    <div class="template-apply-shell" v-if="template">
      <div class="template-apply-steps">
        <div class="template-step" :class="{ active: stepIndex === 0, done: stepIndex > 0 }">
          <span>1</span>
          <strong>模板摘要</strong>
        </div>
        <div class="template-step" :class="{ active: stepIndex === 1, done: stepIndex > 1 }">
          <span>2</span>
          <strong>选择内容</strong>
        </div>
        <div class="template-step" :class="{ active: stepIndex === 2 }">
          <span>3</span>
          <strong>创建项目</strong>
        </div>
      </div>

      <div v-show="stepIndex === 0" class="template-step-panel">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never" class="section-card">
              <div slot="header" class="section-title-row">
                <span>模板信息</span>
              </div>
              <div class="summary-grid">
                <div class="summary-item"><label>模板名称</label><div>{{ template.name || '-' }}</div></div>
                <div class="summary-item"><label>分类</label><div>{{ template.category || '-' }}</div></div>
                <div class="summary-item summary-item-full"><label>描述</label><div>{{ template.description || '暂无描述' }}</div></div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" class="section-card">
              <div slot="header" class="section-title-row">
                <span>模板快照</span>
              </div>
              <div class="summary-grid">
                <div class="summary-item"><label>文档</label><div>{{ template.docCount || 0 }}</div></div>
                <div class="summary-item"><label>任务</label><div>{{ template.taskCount || 0 }}</div></div>
                <div class="summary-item"><label>文件</label><div>{{ template.fileCount || 0 }}</div></div>
                <div class="summary-item"><label>目录</label><div>{{ template.folderCount || 0 }}</div></div>
                <div class="summary-item"><label>活动流</label><div>{{ template.activityCount || 0 }}</div></div>
                <div class="summary-item"><label>已保存文件内容</label><div>{{ hasSavedFileContent ? '是' : '否' }}</div></div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-alert
          :title="hasSavedFileContent ? '该模板包含真实文件内容，推荐选择“结构 + 内容”以确保套用后文件预览与下载正常。' : '该模板没有保存真实文件内容，套用时建议理解为只恢复结构。'"
          :type="hasSavedFileContent ? 'success' : 'warning'"
          :closable="false"
          show-icon
        />
      </div>

      <div v-show="stepIndex === 1" class="template-step-panel">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="section-title-row">
            <span>选择要继承的内容</span>
            <el-tag size="mini" type="info">{{ selectionSummaryText }}</el-tag>
          </div>

          <div class="switch-grid">
            <div class="switch-card">
              <div class="switch-top">
                <span>文件</span>
                <el-switch v-model="form.applyFiles" active-text="应用文件" />
              </div>
              <div v-if="form.applyFiles" class="switch-body">
                <el-radio-group v-model="form.fileMode" size="mini">
                  <el-radio-button label="structure_only">只恢复结构</el-radio-button>
                  <el-radio-button label="structure_and_content" :disabled="!hasSavedFileContent">结构 + 内容</el-radio-button>
                </el-radio-group>
                <el-checkbox-group v-model="form.fileSuffixes" class="suffix-checks">
                  <el-checkbox v-for="suffix in savedFileSuffixes" :key="suffix" :label="suffix">.{{ suffix }}</el-checkbox>
                </el-checkbox-group>
                <el-table ref="applyFileTable" :data="allFileRows" border size="mini" max-height="220" @selection-change="onFileSelectionChange">
                  <el-table-column type="selection" width="48" />
                  <el-table-column prop="itemType" label="类型" width="90" />
                  <el-table-column prop="fileName" label="名称" min-width="160" />
                  <el-table-column prop="filePath" label="路径" min-width="260" show-overflow-tooltip />
                </el-table>
              </div>
            </div>

            <div class="switch-card">
              <div class="switch-top">
                <span>文档</span>
                <el-switch v-model="form.applyDocs" active-text="应用文档" />
              </div>
              <div v-if="form.applyDocs" class="switch-body">
                <div class="inline-checks">
                  <el-checkbox v-model="form.applyReadme">包含 README</el-checkbox>
                  <el-checkbox v-model="form.applyDocVersionHistory">导入版本历史</el-checkbox>
                </div>
                <el-table ref="applyDocTable" :data="(template && template.docItems) || []" border size="mini" max-height="220" @selection-change="onDocSelectionChange">
                  <el-table-column type="selection" width="48" />
                  <el-table-column prop="fileName" label="标题" min-width="180" />
                  <el-table-column prop="previewText" label="摘要" min-width="260" show-overflow-tooltip />
                </el-table>
              </div>
            </div>

            <div class="switch-card">
              <div class="switch-top">
                <span>任务</span>
                <el-switch v-model="form.applyTasks" active-text="应用任务" />
              </div>
              <div v-if="form.applyTasks" class="switch-body">
                <div class="inline-checks">
                  <el-checkbox v-model="form.applyTaskDescription">使用任务描述</el-checkbox>
                  <el-checkbox v-model="form.applyTaskChecklist">带入 checklist 标记</el-checkbox>
                  <el-checkbox v-model="form.applyTaskAttachments">带入附件标记</el-checkbox>
                  <el-checkbox v-model="form.applyTaskDependencies">带入依赖标记</el-checkbox>
                </div>
                <el-table ref="applyTaskTable" :data="(template && template.taskItems) || []" border size="mini" max-height="220" @selection-change="onTaskSelectionChange">
                  <el-table-column type="selection" width="48" />
                  <el-table-column prop="fileName" label="标题" min-width="180" />
                  <el-table-column prop="previewText" label="说明" min-width="260" show-overflow-tooltip />
                </el-table>
              </div>
            </div>

            <div class="switch-card">
              <div class="switch-top">
                <span>活动流</span>
                <el-switch v-model="form.applyActivities" active-text="应用活动流" />
              </div>
              <div v-if="form.applyActivities" class="switch-body">
                <el-radio-group v-model="form.activityMode" size="mini">
                  <el-radio-button label="import_as_template_history">模板历史</el-radio-button>
                  <el-radio-button label="import_as_new_log">初始化日志</el-radio-button>
                  <el-radio-button label="skip">不导入</el-radio-button>
                </el-radio-group>
                <el-table ref="applyActivityTable" :data="(template && template.activityItems) || []" border size="mini" max-height="220" @selection-change="onActivitySelectionChange">
                  <el-table-column type="selection" width="48" />
                  <el-table-column prop="fileName" label="摘要" min-width="200" />
                  <el-table-column prop="previewText" label="预览" min-width="260" show-overflow-tooltip />
                </el-table>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-show="stepIndex === 2" class="template-step-panel">
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
        <el-alert :title="selectionSummaryText" type="info" :closable="false" show-icon />
      </div>
    </div>

    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button v-if="stepIndex > 0" @click="goPrevStep">上一步</el-button>
      <el-button v-if="stepIndex < 2" type="primary" @click="goNextStep">下一步</el-button>
      <el-button v-else type="primary" :loading="saving" @click="submitApply">创建项目</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { applyProjectTemplate } from '@/api/projectTemplate'

export default {
  name: 'ProjectTemplateApplyDialog',
  props: {
    visible: { type: Boolean, default: false },
    template: { type: Object, default: null },
    entrySource: { type: String, default: 'manage' },
    autoNavigateAfterApply: { type: Boolean, default: false }
  },
  data() {
    return {
      dialogVisible: false,
      saving: false,
      createdProjectId: null,
      stepIndex: 0,
      form: this.createDefaultForm()
    }
  },
  computed: {
    normalizedEntrySource() {
      return ['manage', 'template-center', 'myproject'].includes(this.entrySource) ? this.entrySource : 'manage'
    },
    savedFileSuffixes() {
      return Array.isArray(this.template && this.template.savedFileSuffixes) ? this.template.savedFileSuffixes : []
    },
    allFileRows() {
      if (!this.template) return []
      return [...(this.template.folderItems || []), ...(this.template.fileItems || [])]
    },
    hasSavedFileContent() {
      return this.allFileRows.some(item => item && item.itemType === 'file' && item.includeContent)
    },
    selectionSummaryText() {
      return `文件 ${this.form.selectedTemplateFileIds.length} 个，文档 ${this.form.selectedTemplateDocIds.length} 个，任务 ${this.form.selectedTemplateTaskIds.length} 个，活动流 ${this.form.selectedTemplateActivityIds.length} 条`
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
    },
    template() {
      if (this.dialogVisible && this.template) {
        this.resetForm()
      }
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
      this.stepIndex = 0
      this.form = this.createDefaultForm()
      if (this.template) {
        this.form.projectName = `${this.template.name || '新项目'}`
        this.form.projectDescription = this.template.description || ''
        this.form.category = this.template.category || ''
        this.form.fileSuffixes = [...this.savedFileSuffixes]
        this.form.applyFiles = this.allFileRows.length > 0
        this.form.applyDocs = Array.isArray(this.template.docItems) && this.template.docItems.length > 0
        this.form.applyTasks = Array.isArray(this.template.taskItems) && this.template.taskItems.length > 0
        this.form.fileMode = this.hasSavedFileContent ? 'structure_and_content' : 'structure_only'
      }
    },
    goNextStep() {
      if (this.stepIndex < 2) this.stepIndex += 1
    },
    goPrevStep() {
      if (this.stepIndex > 0) this.stepIndex -= 1
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
    extractCreatedProjectId(payload) {
      return payload?.id || payload?.projectId || payload?.data?.id || payload?.data?.projectId || null
    },
    goToCreatedProject(projectId) {
      if (!projectId) return
      this.$router.push({ path: '/projectdetail', query: { projectId: String(projectId) } })
    },
    async submitApply() {
      if (!this.template || !this.template.id) return
      if (!this.form.projectName) {
        this.$message.warning('请先填写项目名称')
        return
      }
      if (this.form.applyFiles && this.form.fileMode !== 'structure_and_content') {
        try {
          await this.$confirm('当前选择的是“只恢复结构”。这样套用后的文件不能保证像正常项目一样预览和下载。确定继续吗？', '提示', { type: 'warning' })
        } catch (e) {
          return
        }
      }
      this.saving = true
      try {
        const res = await applyProjectTemplate(this.template.id, this.form)
        const payload = res?.data || res || null
        this.createdProjectId = this.extractCreatedProjectId(payload)
        this.$message.success('项目创建成功')
        this.$emit('applied', payload)
        this.dialogVisible = false
        if (this.autoNavigateAfterApply && this.createdProjectId) {
          this.goToCreatedProject(this.createdProjectId)
        }
      } catch (e) {
        const msg = e?.response?.data?.message || e?.response?.data?.msg || e?.message || '套用模板失败'
        this.$message.error(msg)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.template-apply-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.template-apply-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.template-step {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  background: #f8fbff;
  color: #5f6f86;
}
.template-step span {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}
.template-step.active,
.template-step.done {
  background: #eef6ff;
  border-color: #bfdbfe;
  color: #1f2937;
}
.template-step-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.section-card {
  margin-bottom: 0;
}
.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.summary-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e5eaf3;
}
.summary-item-full {
  grid-column: 1 / -1;
}
.summary-item label {
  display: block;
  margin-bottom: 8px;
  color: #7b8ba5;
  font-size: 12px;
}
.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.switch-card {
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  padding: 14px;
  background: #fff;
}
.switch-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.switch-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.inline-checks,
.suffix-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}
@media (max-width: 900px) {
  .switch-grid,
  .summary-grid,
  .template-apply-steps {
    grid-template-columns: 1fr;
  }
}
</style>
