<template>
  <div class="ai-page">
    <div class="page-header">
      <div>
        <h2>提示词模板管理</h2>
        <p>管理不同场景的系统提示词、变量结构、发布状态与默认模型。</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" icon="el-icon-plus" @click="openCreateDialog">新建模板</el-button>
        <el-button icon="el-icon-refresh" @click="fetchAll">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="搜索模板名 / 场景码"
          prefix-icon="el-icon-search"
          class="filter-item input-item"
        />
        <el-select v-model="sceneFilter" clearable placeholder="场景码" class="filter-item">
          <el-option v-for="item in sceneOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="typeFilter" clearable placeholder="模板类型" class="filter-item">
          <el-option v-for="item in templateTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="scopeFilter" clearable placeholder="作用域" class="filter-item">
          <el-option v-for="item in scopeTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="publishFilter" clearable placeholder="发布状态" class="filter-item">
          <el-option label="DRAFT" value="DRAFT" />
          <el-option label="PUBLISHED" value="PUBLISHED" />
          <el-option label="DISABLED" value="DISABLED" />
        </el-select>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="filteredList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="模板名称" min-width="180">
          <template slot-scope="{ row }">
            <div class="title-cell">
              <span class="title-text">{{ row.templateName || '-' }}</span>
              <el-tag v-if="row.isEnabled" size="mini" type="success">启用</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sceneCode" label="场景码" min-width="170" />
        <el-table-column prop="templateType" label="模板类型" width="150" />
        <el-table-column prop="scopeType" label="作用域" width="110" />
        <el-table-column label="默认模型" min-width="140">
          <template slot-scope="{ row }">
            {{ (row.defaultModel && row.defaultModel.modelName) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="版本" width="90">
          <template slot-scope="{ row }">
            {{ row.versionNo == null ? '-' : row.versionNo }}
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="120">
          <template slot-scope="{ row }">
            <el-tag :type="publishTagType(row.publishStatus)">
              {{ row.publishStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template slot-scope="{ row }">
            {{ formatTime(row.updatedAt || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="row.publishStatus !== 'PUBLISHED'"
              type="text"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.publishStatus !== 'DISABLED'"
              type="text"
              class="danger-text"
              @click="handleDisable(row)"
            >
              停用
            </el-button>
            <el-button type="text" @click="viewPrompt(row)">查看内容</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          :current-page="page"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      :title="form.id ? '编辑模板' : '新建模板'"
      :visible.sync="dialogVisible"
      width="900px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model.trim="form.templateName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景码" prop="sceneCode">
              <el-input v-model.trim="form.sceneCode" placeholder="例如：blog.write.summary" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="form.templateType" style="width: 100%">
                <el-option v-for="item in templateTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="作用域" prop="scopeType">
              <el-select v-model="form.scopeType" style="width: 100%">
                <el-option v-for="item in scopeTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认模型">
              <el-select v-model="form.defaultModelId" clearable style="width: 100%">
                <el-option
                  v-for="item in modelOptions"
                  :key="item.id"
                  :label="item.modelName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Project ID">
              <el-input-number v-model="form.projectId" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Owner ID">
              <el-input-number v-model="form.ownerId" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用">
              <el-switch v-model="form.isEnabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="System Prompt" prop="systemPrompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="8"
            placeholder="系统提示词"
          />
        </el-form-item>

        <el-form-item label="User Prompt">
          <el-input
            v-model="form.userPromptTemplate"
            type="textarea"
            :rows="5"
            placeholder="用户提示词模板"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="变量 Schema">
              <el-input
                v-model="form.variablesSchema"
                type="textarea"
                :rows="4"
                placeholder='例如：{"title":"string","content":"string"}'
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输出 Schema">
              <el-input
                v-model="form.outputSchema"
                type="textarea"
                :rows="4"
                placeholder='例如：{"summary":"string","tags":"array"}'
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model.trim="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="模板内容预览" :visible.sync="previewVisible" width="900px">
      <div class="prompt-preview">
        <div class="preview-block">
          <div class="preview-title">System Prompt</div>
          <pre>{{ previewData.systemPrompt || '-' }}</pre>
        </div>
        <div class="preview-block">
          <div class="preview-title">User Prompt Template</div>
          <pre>{{ previewData.userPromptTemplate || '-' }}</pre>
        </div>
        <div class="preview-block">
          <div class="preview-title">Variables Schema</div>
          <pre>{{ previewData.variablesSchema || '-' }}</pre>
        </div>
        <div class="preview-block">
          <div class="preview-title">Output Schema</div>
          <pre>{{ previewData.outputSchema || '-' }}</pre>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="previewVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

function emptyForm() {
  return {
    id: null,
    sceneCode: '',
    templateName: '',
    templateType: 'GENERAL_CHAT',
    scopeType: 'PLATFORM',
    projectId: 0,
    ownerId: 0,
    defaultModelId: null,
    systemPrompt: '',
    userPromptTemplate: '',
    variablesSchema: '',
    outputSchema: '',
    isEnabled: true,
    remark: ''
  }
}

export default {
  name: 'PromptTemplate',
  data() {
    return {
      loading: false,
      saving: false,
      dialogVisible: false,
      previewVisible: false,
      previewData: {},
      list: [],
      total: 0,
      page: 1,
      size: 10,
      keyword: '',
      sceneFilter: '',
      typeFilter: '',
      scopeFilter: '',
      publishFilter: '',
      form: emptyForm(),
      modelOptions: [],
      templateTypeOptions: [
        'GENERAL_CHAT',
        'KNOWLEDGE_QA',
        'PROJECT_ASSISTANT',
        'BLOG_ASSISTANT',
        'SUMMARY',
        'CODE_EXPLAIN',
        'CUSTOM'
      ],
      scopeTypeOptions: ['PLATFORM', 'PROJECT', 'PERSONAL'],
      rules: {
        templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
        sceneCode: [{ required: true, message: '请输入场景码', trigger: 'blur' }],
        templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
        scopeType: [{ required: true, message: '请选择作用域', trigger: 'change' }],
        systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }]
      }
    }
  },
  computed: {
    sceneOptions() {
      return [...new Set(this.list.map(item => item.sceneCode).filter(Boolean))]
    },
    filteredList() {
      return this.list.filter(item => {
        const keyword = this.keyword.toLowerCase()
        const matchKeyword =
          !keyword ||
          String(item.templateName || '').toLowerCase().includes(keyword) ||
          String(item.sceneCode || '').toLowerCase().includes(keyword)

        const matchScene = !this.sceneFilter || item.sceneCode === this.sceneFilter
        const matchType = !this.typeFilter || item.templateType === this.typeFilter
        const matchScope = !this.scopeFilter || item.scopeType === this.scopeFilter
        const matchPublish = !this.publishFilter || item.publishStatus === this.publishFilter

        return matchKeyword && matchScene && matchType && matchScope && matchPublish
      })
    }
  },
  created() {
    this.fetchAll()
  },
  methods: {
    unwrap(res) {
      if (res == null) return null
      if (res.data !== undefined) return res.data
      return res
    },
    normalizePage(res) {
      const data = this.unwrap(res) || {}
      const content = data.content || data.records || data.list || []
      return {
        content: Array.isArray(content) ? content : [],
        total: Number(data.totalElements || data.total || 0)
      }
    },
    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString('zh-CN', { hour12: false })
    },
    publishTagType(status) {
      if (status === 'PUBLISHED') return 'success'
      if (status === 'DISABLED') return 'info'
      return 'warning'
    },
    async fetchAll() {
      await Promise.all([this.fetchList(), this.fetchModels()])
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await request({
          url: '/ai/prompt-templates',
          method: 'get',
          params: {
            page: this.page - 1,
            size: this.size
          }
        })
        const pageData = this.normalizePage(res)
        this.list = pageData.content
        this.total = pageData.total
      } catch (e) {
        console.error(e)
        this.$message.error('获取模板列表失败')
      } finally {
        this.loading = false
      }
    },
    async fetchModels() {
      try {
        const res = await request({
          url: '/admin/ai/models/enabled',
          method: 'get'
        })
        const data = this.unwrap(res)
        this.modelOptions = Array.isArray(data) ? data : []
      } catch (e) {
        console.error(e)
        this.modelOptions = []
      }
    },
    openCreateDialog() {
      this.form = emptyForm()
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.form = {
        id: row.id || null,
        sceneCode: row.sceneCode || '',
        templateName: row.templateName || '',
        templateType: row.templateType || 'GENERAL_CHAT',
        scopeType: row.scopeType || 'PLATFORM',
        projectId: row.projectId || 0,
        ownerId: row.ownerId || 0,
        defaultModelId: row.defaultModel && row.defaultModel.id ? row.defaultModel.id : null,
        systemPrompt: row.systemPrompt || '',
        userPromptTemplate: row.userPromptTemplate || '',
        variablesSchema: row.variablesSchema || '',
        outputSchema: row.outputSchema || '',
        isEnabled: row.isEnabled !== false,
        remark: row.remark || ''
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.$refs.formRef && this.$refs.formRef.clearValidate()
      this.form = emptyForm()
    },
    buildPayload() {
      return {
        id: this.form.id || null,
        sceneCode: this.form.sceneCode,
        templateName: this.form.templateName,
        templateType: this.form.templateType,
        scopeType: this.form.scopeType,
        projectId: this.form.projectId || null,
        ownerId: this.form.ownerId || null,
        defaultModel: this.form.defaultModelId ? { id: this.form.defaultModelId } : null,
        systemPrompt: this.form.systemPrompt,
        userPromptTemplate: this.form.userPromptTemplate,
        variablesSchema: this.form.variablesSchema,
        outputSchema: this.form.outputSchema,
        isEnabled: this.form.isEnabled,
        remark: this.form.remark
      }
    },
    async submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          const payload = this.buildPayload()
          if (this.form.id) {
            await request({
              url: `/ai/prompt-templates/${this.form.id}`,
              method: 'put',
              data: payload
            })
            this.$message.success('模板更新成功')
          } else {
            await request({
              url: '/ai/prompt-templates',
              method: 'post',
              data: payload
            })
            this.$message.success('模板创建成功')
          }
          this.dialogVisible = false
          await this.fetchList()
        } catch (e) {
          console.error(e)
          this.$message.error(this.form.id ? '模板更新失败' : '模板创建失败')
        } finally {
          this.saving = false
        }
      })
    },
    async handlePublish(row) {
      try {
        await this.$confirm(`确定发布模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
        await request({
          url: `/ai/prompt-templates/${row.id}/publish`,
          method: 'put'
        })
        this.$message.success('发布成功')
        await this.fetchList()
      } catch (e) {
        if (e !== 'cancel') {
          console.error(e)
          this.$message.error('发布失败')
        }
      }
    },
    async handleDisable(row) {
      try {
        await this.$confirm(`确定停用模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
        await request({
          url: `/ai/prompt-templates/${row.id}/disable`,
          method: 'put'
        })
        this.$message.success('停用成功')
        await this.fetchList()
      } catch (e) {
        if (e !== 'cancel') {
          console.error(e)
          this.$message.error('停用失败')
        }
      }
    },
    viewPrompt(row) {
      this.previewData = { ...row }
      this.previewVisible = true
    },
    handlePageChange(page) {
      this.page = page
      this.fetchList()
    },
    handleSizeChange(size) {
      this.size = size
      this.page = 1
      this.fetchList()
    }
  }
}
</script>

<style scoped>
.ai-page {
  padding: 24px;
  background: #f6f8fb;
  min-height: 100vh;
}
.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0 0 8px;
  color: #1f2937;
}
.page-header p {
  margin: 0;
  color: #6b7280;
}
.page-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-card {
  margin-bottom: 16px;
}
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.filter-item {
  width: 180px;
}
.input-item {
  width: 320px;
}
.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-text {
  font-weight: 600;
}
.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.prompt-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.preview-block {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}
.preview-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #1f2937;
}
.preview-block pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #334155;
}
.danger-text {
  color: #f56c6c;
}
</style>