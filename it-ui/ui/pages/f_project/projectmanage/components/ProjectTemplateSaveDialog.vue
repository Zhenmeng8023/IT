<template>
  <el-dialog title="保存当前项目为模板" :visible.sync="dialogVisible" width="1120px" top="4vh">
    <div v-loading="loading">
      <el-form :model="form" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="模板名称">
              <el-input v-model="form.name" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模板分类">
              <el-input v-model="form.category" placeholder="如：博客、知识库、项目协作" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="公开模板">
              <el-switch v-model="form.isPublic" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述这个模板适合什么项目使用" />
        </el-form-item>
      </el-form>

      <el-divider>保存内容选择</el-divider>

      <el-card shadow="never" class="section-card">
        <div slot="header" class="section-title-row">
          <span>文件快照</span>
          <el-switch v-model="form.includeFiles" active-text="保存文件" />
        </div>
        <div v-if="form.includeFiles">
          <el-form :model="form" label-width="120px" size="small">
            <el-form-item label="文件模式">
              <el-radio-group v-model="form.fileMode">
                <el-radio label="structure_only">只保存目录/文件结构</el-radio>
                <el-radio label="structure_and_content">保存目录结构 + 文件内容</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="后缀筛选">
              <el-checkbox-group v-model="form.fileSuffixes">
                <el-checkbox v-for="suffix in fileSuffixOptions" :key="suffix" :label="suffix">.{{ suffix }}</el-checkbox>
              </el-checkbox-group>
              <div class="custom-ext-row">
                <el-input v-model="customSuffixInput" placeholder="自定义后缀，多个用逗号分隔，如 vue,java,xml" size="small" />
                <el-button size="small" @click="appendCustomSuffixes">加入筛选</el-button>
              </div>
            </el-form-item>
          </el-form>
          <el-table ref="fileTable" :data="source.files || []" border size="mini" max-height="240" @selection-change="onFileSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="name" label="文件名" min-width="180" />
            <el-table-column prop="path" label="路径" min-width="260" show-overflow-tooltip />
            <el-table-column prop="fileExt" label="后缀" width="100">
              <template slot-scope="scope">
                <el-tag size="mini">{{ scope.row.fileExt || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="信息" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </el-card>

      <el-card shadow="never" class="section-card">
        <div slot="header" class="section-title-row">
          <span>项目文档</span>
          <el-switch v-model="form.includeDocs" active-text="保存文档" />
        </div>
        <div v-if="form.includeDocs">
          <el-form :model="form" label-width="120px" size="small">
            <el-form-item label="文档选项">
              <el-checkbox v-model="form.includeReadme">包含主 README</el-checkbox>
              <el-checkbox v-model="form.includeDocVersionHistory">包含版本历史</el-checkbox>
            </el-form-item>
          </el-form>
          <el-table ref="docTable" :data="source.docs || []" border size="mini" max-height="220" @selection-change="onDocSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="name" label="标题" min-width="180" />
            <el-table-column prop="subType" label="类型" width="120" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
          </el-table>
        </div>
      </el-card>

      <el-card shadow="never" class="section-card">
        <div slot="header" class="section-title-row">
          <span>任务快照</span>
          <el-switch v-model="form.includeTasks" active-text="保存任务" />
        </div>
        <div v-if="form.includeTasks">
          <el-form :model="form" label-width="120px" size="small">
            <el-form-item label="任务选项">
              <el-checkbox v-model="form.includeTaskDescription">包含任务描述</el-checkbox>
              <el-checkbox v-model="form.includeTaskChecklist">包含 checklist 标记</el-checkbox>
              <el-checkbox v-model="form.includeTaskAttachments">包含附件标记</el-checkbox>
              <el-checkbox v-model="form.includeTaskDependencies">包含依赖标记</el-checkbox>
            </el-form-item>
          </el-form>
          <el-table ref="taskTable" :data="source.tasks || []" border size="mini" max-height="220" @selection-change="onTaskSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="name" label="标题" min-width="180" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="subType" label="优先级" width="120" />
            <el-table-column prop="summary" label="说明" min-width="260" show-overflow-tooltip />
          </el-table>
        </div>
      </el-card>

      <el-card shadow="never" class="section-card">
        <div slot="header" class="section-title-row">
          <span>活动流快照</span>
          <el-switch v-model="form.includeActivities" active-text="保存活动流" />
        </div>
        <div v-if="form.includeActivities">
          <el-form :model="form" label-width="120px" size="small">
            <el-form-item label="动作筛选">
              <el-checkbox-group v-model="form.selectedActivityActions">
                <el-checkbox v-for="action in activityActionOptions" :key="action" :label="action">{{ action }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
          <el-table ref="activityTable" :data="source.activities || []" border size="mini" max-height="220" @selection-change="onActivitySelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="action" label="动作" width="160" />
            <el-table-column prop="summary" label="摘要" min-width="300" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
        </div>
      </el-card>
    </div>

    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitSave">保存为模板</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { getProjectTemplateSource, saveProjectAsTemplate } from '@/api/projectTemplate'

export default {
  name: 'ProjectTemplateSaveDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    defaultName: {
      type: String,
      default: ''
    },
    defaultCategory: {
      type: String,
      default: ''
    },
    defaultDescription: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      dialogVisible: false,
      loading: false,
      saving: false,
      source: {
        docs: [],
        tasks: [],
        files: [],
        activities: []
      },
      customSuffixInput: '',
      form: this.createDefaultForm()
    }
  },
  computed: {
    fileSuffixOptions() {
      return Array.isArray(this.source.fileSuffixOptions) ? this.source.fileSuffixOptions : []
    },
    activityActionOptions() {
      return Array.isArray(this.source.activityActionOptions) ? this.source.activityActionOptions : []
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.dialogVisible = val
        if (val) {
          this.resetForm()
          this.loadSource()
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
        name: '',
        description: '',
        category: '',
        isPublic: false,
        includeFiles: true,
        fileMode: 'structure_only',
        selectedFileIds: [],
        fileSuffixes: [],
        includeDocs: true,
        includeReadme: true,
        includeDocVersionHistory: false,
        selectedDocIds: [],
        includeTasks: true,
        includeTaskDescription: true,
        includeTaskChecklist: false,
        includeTaskAttachments: false,
        includeTaskDependencies: false,
        selectedTaskIds: [],
        includeActivities: false,
        selectedActivityActions: [],
        selectedActivityIds: []
      }
    },
    resetForm() {
      this.form = this.createDefaultForm()
      this.form.name = this.defaultName ? `${this.defaultName} 模板` : ''
      this.form.category = this.defaultCategory || ''
      this.form.description = this.defaultDescription || ''
      this.customSuffixInput = ''
    },
    async loadSource() {
      if (!this.projectId) return
      this.loading = true
      try {
        const res = await getProjectTemplateSource(this.projectId)
        this.source = res?.data || res || this.source
        this.$nextTick(() => {
          this.toggleAllRows('docTable', this.source.docs)
          this.toggleAllRows('taskTable', this.source.tasks)
          this.toggleAllRows('fileTable', this.source.files)
        })
      } finally {
        this.loading = false
      }
    },
    toggleAllRows(refName, rows) {
      const table = this.$refs[refName]
      if (!table || !Array.isArray(rows)) return
      rows.forEach(row => table.toggleRowSelection(row, true))
    },
    appendCustomSuffixes() {
      const values = this.customSuffixInput
        .split(',')
        .map(item => item.trim().replace(/^\./, ''))
        .filter(Boolean)
      const merged = Array.from(new Set([...(this.form.fileSuffixes || []), ...values]))
      this.form.fileSuffixes = merged
      this.customSuffixInput = ''
    },
    onDocSelectionChange(rows) {
      this.form.selectedDocIds = rows.map(item => item.id)
    },
    onTaskSelectionChange(rows) {
      this.form.selectedTaskIds = rows.map(item => item.id)
    },
    onFileSelectionChange(rows) {
      this.form.selectedFileIds = rows.map(item => item.id)
    },
    onActivitySelectionChange(rows) {
      this.form.selectedActivityIds = rows.map(item => item.id)
    },
    async submitSave() {
      if (!this.projectId) return
      if (!this.form.name) {
        this.$message.warning('请先填写模板名称')
        return
      }
      this.saving = true
      try {
        const res = await saveProjectAsTemplate(this.projectId, this.form)
        this.$message.success('模板保存成功')
        this.$emit('saved', res?.data || res)
        this.dialogVisible = false
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
.custom-ext-row {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
</style>
