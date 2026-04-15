<template>
  <el-dialog
    :title="mode === 'create' ? '新建知识库' : '编辑知识库'"
    :visible.sync="dialogVisible"
    width="760px"
    destroy-on-close
    @closed="handleClosed"
  >
    <el-form ref="kbFormRef" :model="localForm" :rules="rules" label-width="110px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="知识库名称" prop="name">
            <el-input v-model.trim="localForm.name" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="拥有者 ID">
            <el-input :value="localForm.ownerId || '-'" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="描述">
        <el-input v-model.trim="localForm.description" type="textarea" :rows="3" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="作用域" prop="scopeType">
            <el-select v-model="localForm.scopeType" style="width: 100%">
              <el-option label="PERSONAL" value="PERSONAL" />
              <el-option label="PROJECT" value="PROJECT" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="项目 ID">
            <el-input-number v-model="localForm.projectId" :min="1" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="来源类型">
            <el-select v-model="localForm.sourceType" style="width: 100%">
              <el-option label="MANUAL" value="MANUAL" />
              <el-option label="UPLOAD" value="UPLOAD" />
              <el-option label="PROJECT_DOC" value="PROJECT_DOC" />
              <el-option label="BLOG" value="BLOG" />
              <el-option label="CIRCLE" value="CIRCLE" />
              <el-option label="PAID_CONTENT" value="PAID_CONTENT" />
              <el-option label="MIXED" value="MIXED" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="可见性">
            <el-select v-model="localForm.visibility" style="width: 100%">
              <el-option label="PRIVATE" value="PRIVATE" />
              <el-option label="TEAM" value="TEAM" />
              <el-option label="PUBLIC" value="PUBLIC" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="切块策略">
            <el-select v-model="localForm.chunkStrategy" style="width: 100%">
              <el-option label="PARAGRAPH" value="PARAGRAPH" />
              <el-option label="FIXED" value="FIXED" />
              <el-option label="MARKDOWN" value="MARKDOWN" />
              <el-option label="CUSTOM" value="CUSTOM" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="默认 TopK">
            <el-input-number v-model="localForm.defaultTopK" :min="1" :max="20" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="默认问答模型">
            <el-select
              v-model="localForm.defaultModelId"
              clearable
              filterable
              placeholder="未设置时跟随系统当前模型"
              style="width: 100%"
            >
              <el-option
                v-for="model in enabledModels"
                :key="model.id"
                :label="buildModelLabel(model)"
                :value="model.id"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="默认模型说明">
            <div class="text-muted">未设置时，问答会自动回退到系统当前模型。</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Embedding Provider">
            <el-select
              v-model="localForm.embeddingProvider"
              clearable
              filterable
              allow-create
              style="width: 100%"
              placeholder="请选择或输入 embedding provider"
              @change="handleEmbeddingProviderChange"
            >
              <el-option
                v-for="item in embeddingProviderOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="Embedding Model">
            <el-select
              v-model="localForm.embeddingModel"
              clearable
              filterable
              allow-create
              style="width: 100%"
              placeholder="请选择或输入 embedding model"
            >
              <el-option
                v-for="item in localEmbeddingModelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="disabled" @click="submit">
        {{ mode === 'create' ? '创建' : '保存' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'KnowledgeBaseBaseFormDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    mode: {
      type: String,
      default: 'create'
    },
    form: {
      type: Object,
      default: () => ({})
    },
    rules: {
      type: Object,
      default: () => ({})
    },
    enabledModels: {
      type: Array,
      default: () => []
    },
    embeddingProviderOptions: {
      type: Array,
      default: () => []
    },
    buildModelLabel: {
      type: Function,
      default: model => (model && model.modelName) || ''
    },
    saving: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      localForm: {}
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    localEmbeddingModelOptions() {
      const provider = String(this.localForm.embeddingProvider || '').trim().toLowerCase()
      return (this.enabledModels || [])
        .filter(item => {
          const flag = item && item.supportsEmbedding
          const supportsEmbedding = flag === true || Number(flag) === 1 || String(flag).toLowerCase() === 'true'
          if (!supportsEmbedding) return false
          const code = String(item.providerCode || '').trim().toLowerCase()
          return !provider || code === provider
        })
        .map(item => ({
          value: String(item.modelName || '').trim(),
          label: this.buildModelLabel(item)
        }))
        .filter(item => item.value)
    }
  },
  watch: {
    form: {
      immediate: true,
      deep: true,
      handler(value) {
        this.localForm = { ...(value || {}) }
      }
    }
  },
  methods: {
    handleClosed() {
      this.$emit('closed')
    },

    handleEmbeddingProviderChange(value) {
      const provider = String(value || '').trim().toLowerCase()
      if (!provider) {
        this.localForm.embeddingModel = ''
        return
      }
      const matched = this.localEmbeddingModelOptions.find(item => item.value === this.localForm.embeddingModel)
      if (!matched) {
        const first = this.localEmbeddingModelOptions[0]
        this.localForm.embeddingModel = first ? first.value : ''
      }
    },

    submit() {
      if (!this.$refs.kbFormRef) return
      this.$refs.kbFormRef.validate(valid => {
        if (!valid) return
        this.$emit('save', { ...this.localForm })
      })
    }
  }
}
</script>

<style scoped>
.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>
