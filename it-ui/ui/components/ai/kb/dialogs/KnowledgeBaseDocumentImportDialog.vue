<template>
  <el-dialog
    title="新增文档"
    :visible.sync="dialogVisible"
    width="760px"
    destroy-on-close
    @closed="$emit('closed')"
  >
    <el-form ref="documentFormRef" :model="localForm" :rules="rules" label-width="110px">
      <el-form-item label="导入方式">
        <el-radio-group :value="importMode" size="small" @input="$emit('update:importMode', $event)">
          <el-radio-button label="manual">正文录入</el-radio-button>
          <el-radio-button label="reference">引用导入</el-radio-button>
        </el-radio-group>
      </el-form-item>

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
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="标题" prop="title">
            <el-input v-model.trim="localForm.title" />
          </el-form-item>
        </el-col>
      </el-row>

      <template v-if="importMode === 'reference'">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源引用 ID">
              <el-input-number v-model="localForm.sourceRefId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="导入提示">
              <div class="text-muted">适合引用项目文档、博客、圈子或付费内容，再补充正文后一并入库。</div>
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <el-form-item label="正文" prop="contentText">
        <el-input v-model="localForm.contentText" type="textarea" :rows="10" />
      </el-form-item>

      <div v-if="selectedLocalFileName" class="upload-tip">
        当前已导入本地文本：{{ selectedLocalFileName }}
      </div>
      <div v-if="fileReadError" class="upload-tip upload-tip--error">
        {{ fileReadError }}
      </div>
    </el-form>

    <div slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="disabled" @click="submit">保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'KnowledgeBaseDocumentImportDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    form: {
      type: Object,
      default: () => ({})
    },
    importMode: {
      type: String,
      default: 'manual'
    },
    rules: {
      type: Object,
      default: () => ({})
    },
    selectedLocalFileName: {
      type: String,
      default: ''
    },
    fileReadError: {
      type: String,
      default: ''
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
    submit() {
      if (!this.$refs.documentFormRef) return
      this.$refs.documentFormRef.validate(valid => {
        if (!valid) return
        this.$emit('save', { ...this.localForm })
      })
    }
  }
}
</script>

<style scoped>
.text-muted,
.upload-tip {
  color: #409eff;
  font-size: 12px;
}

.upload-tip {
  margin-top: 8px;
}

.upload-tip--error {
  color: #f56c6c;
}
</style>
