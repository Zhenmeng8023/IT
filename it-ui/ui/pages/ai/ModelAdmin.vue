<template>
  <div class="ai-page">
    <div class="page-header">
      <div>
        <h2>AI 模型管理</h2>
        <p>管理当前可用模型、启停状态、默认配置与调用参数。</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" icon="el-icon-plus" @click="openCreateDialog">新建模型</el-button>
        <el-button icon="el-icon-refresh" @click="fetchAll">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="activeModel && activeModel.id"
      :title="`当前激活模型：${activeModel.modelName || '-'}（${activeModel.providerCode || '-'}）`"
      type="success"
      show-icon
      :closable="false"
      class="page-alert"
    />

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model.trim="keyword"
          clearable
          placeholder="搜索模型名 / provider / baseUrl"
          prefix-icon="el-icon-search"
          class="filter-item input-item"
        />
        <el-select v-model="typeFilter" clearable placeholder="模型类型" class="filter-item">
          <el-option v-for="item in modelTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="deployFilter" clearable placeholder="部署方式" class="filter-item">
          <el-option v-for="item in deploymentModeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="enableFilter" clearable placeholder="启用状态" class="filter-item">
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="filteredModels"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="模型名称" min-width="180">
          <template slot-scope="{ row }">
            <div class="title-cell">
              <span class="title-text">{{ row.modelName || '-' }}</span>
              <el-tag v-if="activeModel && activeModel.id === row.id" size="mini" type="success">当前激活</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="modelType" label="模型类型" width="120" />
        <el-table-column prop="providerCode" label="Provider" width="140" />
        <el-table-column prop="deploymentMode" label="部署方式" width="130" />
        <el-table-column label="能力" min-width="180">
          <template slot-scope="{ row }">
            <div class="capability-tags">
              <el-tag size="mini" :type="row.supportsStream ? 'success' : 'info'">流式</el-tag>
              <el-tag size="mini" :type="row.supportsTools ? 'warning' : 'info'">Tools</el-tag>
              <el-tag size="mini" :type="row.supportsEmbedding ? 'primary' : 'info'">Embedding</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template slot-scope="{ row }">
            {{ row.priority == null ? '-' : row.priority }}
          </template>
        </el-table-column>
        <el-table-column label="超时(ms)" width="110">
          <template slot-scope="{ row }">
            {{ row.timeoutMs == null ? '-' : row.timeoutMs }}
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'">
              {{ row.isEnabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template slot-scope="{ row }">
            {{ formatTime(row.updatedAt || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              v-if="!row.isEnabled"
              type="text"
              @click="handleEnable(row)"
            >
              启用
            </el-button>
            <el-button
              v-else
              type="text"
              class="danger-text"
              @click="handleDisable(row)"
            >
              停用
            </el-button>
            <el-button type="text" @click="viewJson(row)">查看详情</el-button>
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
      :title="form.id ? '编辑模型' : '新建模型'"
      :visible.sync="dialogVisible"
      width="760px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model.trim="form.modelName" placeholder="例如：deepseek-chat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Provider" prop="providerCode">
              <el-input v-model.trim="form.providerCode" placeholder="例如：deepseek / openai / ollama" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="form.modelType" style="width: 100%">
                <el-option v-for="item in modelTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部署方式" prop="deploymentMode">
              <el-select v-model="form.deploymentMode" style="width: 100%">
                <el-option v-for="item in deploymentModeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Base URL">
          <el-input v-model.trim="form.baseUrl" placeholder="例如：https://api.deepseek.com/chat/completions" />
        </el-form-item>

        <el-form-item label="API Key">
          <el-input v-model.trim="form.apiKey" type="textarea" :rows="2" placeholder="可留空或填写密钥" />
        </el-form-item>

        <el-form-item label="默认参数">
          <el-input
            v-model="form.defaultParams"
            type="textarea"
            :rows="4"
            placeholder='例如：{"temperature":0.7,"top_p":0.9}'
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="0" :max="999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(ms)">
              <el-input-number v-model="form.timeoutMs" :min="1000" :max="600000" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用状态">
              <el-switch v-model="form.isEnabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="能力支持">
          <div class="switch-row">
            <el-switch v-model="form.supportsStream" active-text="支持流式" />
            <el-switch v-model="form.supportsTools" active-text="支持 Tools" />
            <el-switch v-model="form.supportsEmbedding" active-text="支持 Embedding" />
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="输入成本">
              <el-input-number v-model="form.costInputPer1m" :precision="4" :step="0.0001" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输出成本">
              <el-input-number v-model="form.costOutputPer1m" :precision="4" :step="0.0001" :min="0" controls-position="right" style="width: 100%" />
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

    <el-dialog title="模型详情" :visible.sync="jsonDialogVisible" width="760px">
      <pre class="json-block">{{ selectedJson }}</pre>
      <div slot="footer">
        <el-button @click="jsonDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  pageAiModels,
  getActiveAiModel,
  saveAiModel,
  enableAiModel,
  disableAiModel,
  extractPageContent,
  extractApiData
} from '@/api/aiAdmin'

function emptyForm() {
  return {
    id: null,
    modelName: '',
    modelType: 'DEEPSEEK',
    providerCode: '',
    deploymentMode: 'REMOTE_API',
    apiKey: '',
    baseUrl: '',
    defaultParams: '',
    priority: 0,
    timeoutMs: 60000,
    supportsStream: true,
    supportsTools: false,
    supportsEmbedding: false,
    costInputPer1m: 0,
    costOutputPer1m: 0,
    isEnabled: true,
    remark: ''
  }
}

export default {
  name: 'ModelAdmin',
  data() {
    return {
      loading: false,
      saving: false,
      dialogVisible: false,
      jsonDialogVisible: false,
      selectedJson: '',
      keyword: '',
      typeFilter: '',
      deployFilter: '',
      enableFilter: '',
      page: 1,
      size: 10,
      total: 0,
      list: [],
      activeModel: null,
      form: emptyForm(),
      modelTypeOptions: ['OPENAI', 'BAIDU', 'QWEN', 'DEEPSEEK', 'OLLAMA', 'CUSTOM'],
      deploymentModeOptions: ['REMOTE_API', 'LOCAL_OLLAMA', 'MOCK'],
      rules: {
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
        modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
        providerCode: [{ required: true, message: '请输入 providerCode', trigger: 'blur' }],
        deploymentMode: [{ required: true, message: '请选择部署方式', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredModels() {
      return this.list.filter(item => {
        const keyword = this.keyword.toLowerCase()
        const matchKeyword =
          !keyword ||
          String(item.modelName || '').toLowerCase().includes(keyword) ||
          String(item.providerCode || '').toLowerCase().includes(keyword) ||
          String(item.baseUrl || '').toLowerCase().includes(keyword)

        const matchType = !this.typeFilter || item.modelType === this.typeFilter
        const matchDeploy = !this.deployFilter || item.deploymentMode === this.deployFilter
        const matchEnable = this.enableFilter === '' || this.enableFilter === null || item.isEnabled === this.enableFilter

        return matchKeyword && matchType && matchDeploy && matchEnable
      })
    }
  },
  created() {
    this.fetchAll()
  },
  methods: {
    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString('zh-CN', { hour12: false })
    },
    async fetchAll() {
      await Promise.all([this.fetchList(), this.fetchActive()])
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await pageAiModels({
          page: this.page - 1,
          size: this.size
        })
        const pageData = extractPageContent(res)
        this.list = Array.isArray(pageData.content) ? pageData.content : []
        this.total = pageData.total || 0
      } catch (e) {
        console.error(e)
        this.$message.error('获取模型列表失败')
      } finally {
        this.loading = false
      }
    },
    async fetchActive() {
      try {
        const res = await getActiveAiModel()
        this.activeModel = extractApiData(res) || null
      } catch (e) {
        console.error(e)
        this.activeModel = null
      }
    },
    openCreateDialog() {
      this.form = emptyForm()
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.form = {
        id: row.id || null,
        modelName: row.modelName || '',
        modelType: row.modelType || 'DEEPSEEK',
        providerCode: row.providerCode || '',
        deploymentMode: row.deploymentMode || 'REMOTE_API',
        apiKey: row.apiKey || '',
        baseUrl: row.baseUrl || '',
        defaultParams: row.defaultParams || '',
        priority: row.priority == null ? 0 : row.priority,
        timeoutMs: row.timeoutMs == null ? 60000 : row.timeoutMs,
        supportsStream: !!row.supportsStream,
        supportsTools: !!row.supportsTools,
        supportsEmbedding: !!row.supportsEmbedding,
        costInputPer1m: row.costInputPer1m == null ? 0 : Number(row.costInputPer1m),
        costOutputPer1m: row.costOutputPer1m == null ? 0 : Number(row.costOutputPer1m),
        isEnabled: row.isEnabled !== false,
        remark: row.remark || ''
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.$refs.formRef && this.$refs.formRef.clearValidate()
      this.form = emptyForm()
    },
    async submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          await saveAiModel({ ...this.form })
          this.$message.success(this.form.id ? '模型更新成功' : '模型创建成功')
          this.dialogVisible = false
          await this.fetchAll()
        } catch (e) {
          console.error(e)
          this.$message.error(this.form.id ? '模型更新失败' : '模型创建失败')
        } finally {
          this.saving = false
        }
      })
    },
    async handleEnable(row) {
      try {
        await this.$confirm(`确定启用模型「${row.modelName}」吗？`, '提示', { type: 'warning' })
        await enableAiModel(row.id)
        this.$message.success('启用成功')
        await this.fetchAll()
      } catch (e) {
        if (e !== 'cancel') {
          console.error(e)
          this.$message.error('启用失败')
        }
      }
    },
    async handleDisable(row) {
      try {
        await this.$confirm(`确定停用模型「${row.modelName}」吗？`, '提示', { type: 'warning' })
        await disableAiModel(row.id)
        this.$message.success('停用成功')
        await this.fetchAll()
      } catch (e) {
        if (e !== 'cancel') {
          console.error(e)
          this.$message.error('停用失败')
        }
      }
    },
    viewJson(row) {
      this.selectedJson = JSON.stringify(row, null, 2)
      this.jsonDialogVisible = true
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
.page-alert {
  margin-bottom: 16px;
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
.capability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}
.json-block {
  background: #0f172a;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 12px;
  overflow: auto;
  max-height: 520px;
  margin: 0;
}
.danger-text {
  color: #f56c6c;
}
</style>