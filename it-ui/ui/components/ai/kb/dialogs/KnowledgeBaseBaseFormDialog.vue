<template>
  <el-dialog
    :title="mode === 'create' ? 'Create Knowledge Base' : 'Edit Knowledge Base'"
    :visible.sync="dialogVisible"
    width="760px"
    destroy-on-close
    @closed="handleClosed"
  >
    <el-form ref="kbFormRef" :model="localForm" :rules="rules" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="showOwnerIdField ? 12 : 24">
          <el-form-item label="Knowledge Base Name" prop="name">
            <el-input v-model.trim="localForm.name" />
          </el-form-item>
        </el-col>

        <el-col v-if="showOwnerIdField" :span="12">
          <el-form-item label="Owner ID">
            <el-input :value="localForm.ownerId || '-'" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="Description">
        <el-input v-model.trim="localForm.description" type="textarea" :rows="3" />
      </el-form-item>

      <el-row v-if="showScopeTypeField || showProjectIdField" :gutter="16">
        <el-col v-if="showScopeTypeField" :span="showProjectIdField ? 12 : 24">
          <el-form-item label="Scope Type" prop="scopeType">
            <el-select v-model="localForm.scopeType" style="width: 100%">
              <el-option label="PERSONAL" value="PERSONAL" />
              <el-option label="PROJECT" value="PROJECT" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col v-if="showProjectIdField" :span="showScopeTypeField ? 12 : 24">
          <el-form-item label="Project ID">
            <el-input-number v-model="localForm.projectId" :min="1" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col v-if="showSourceTypeField" :span="12">
          <el-form-item label="Source Type">
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

        <el-col :span="showSourceTypeField ? 12 : 24">
          <el-form-item label="Visibility">
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
          <el-form-item label="Chunk Strategy">
            <el-select v-model="localForm.chunkStrategy" style="width: 100%">
              <el-option label="PARAGRAPH" value="PARAGRAPH" />
              <el-option label="FIXED" value="FIXED" />
              <el-option label="MARKDOWN" value="MARKDOWN" />
              <el-option label="CUSTOM" value="CUSTOM" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="Default TopK">
            <el-input-number v-model="localForm.defaultTopK" :min="1" :max="20" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="Default QA Model">
            <el-select
              v-model="localForm.defaultModelId"
              clearable
              filterable
              placeholder="Follow active model if empty"
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
          <el-form-item label="Model Hint">
            <div class="text-muted">If not set, Q&A falls back to the active system model.</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="showEmbeddingFields" :gutter="16">
        <el-col :span="12">
          <el-form-item label="Embedding Provider">
            <el-select
              v-model="localForm.embeddingProvider"
              clearable
              filterable
              allow-create
              style="width: 100%"
              placeholder="Select or type embedding provider"
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
              placeholder="Select or type embedding model"
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
      <el-button @click="dialogVisible = false">Cancel</el-button>
      <el-button type="primary" :loading="saving" :disabled="disabled" @click="submit">
        {{ mode === 'create' ? 'Create' : 'Save' }}
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
    },
    showOwnerIdField: {
      type: Boolean,
      default: true
    },
    showScopeTypeField: {
      type: Boolean,
      default: true
    },
    showProjectIdField: {
      type: Boolean,
      default: true
    },
    showSourceTypeField: {
      type: Boolean,
      default: true
    },
    showEmbeddingFields: {
      type: Boolean,
      default: true
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
