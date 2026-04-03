<template>
  <el-dialog
    :title="mode === 'edit' ? '编辑文档' : '新建文档'"
    :visible.sync="innerVisible"
    width="900px"
    top="4vh"
    append-to-body
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="10">
          <el-form-item label="标题" prop="title">
            <el-input v-model.trim="form.title" maxlength="255" show-word-limit></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="类型" prop="docType">
            <el-select v-model="form.docType" style="width: 100%">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="草稿" value="draft"></el-option>
              <el-option label="已发布" value="published"></el-option>
              <el-option label="已归档" value="archived"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="3">
          <el-form-item label="可见性" prop="visibility">
            <el-select v-model="form.visibility" style="width: 100%">
              <el-option label="项目内" value="project"></el-option>
              <el-option label="团队" value="team"></el-option>
              <el-option label="仅自己" value="private"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="3">
          <el-form-item label="主入口" label-width="70px">
            <el-switch v-model="form.isPrimary"></el-switch>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="变更摘要">
        <el-input v-model.trim="form.changeSummary" maxlength="500" show-word-limit placeholder="例如：补充部署步骤、修复接口说明"></el-input>
      </el-form-item>

      <el-form-item label="正文" prop="content">
        <div class="editor-layout">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="18"
            resize="none"
            placeholder="支持 Markdown 文本"
          ></el-input>
          <div class="preview-panel">
            <div class="preview-title">预览</div>
            <pre class="preview-content">{{ form.content }}</pre>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <span slot="footer">
      <el-button @click="innerVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { createProjectDoc, updateProjectDoc } from '@/api/projectDoc'

export default {
  name: 'ProjectDocEditor',
  props: {
    visible: { type: Boolean, default: false },
    mode: { type: String, default: 'create' },
    projectId: { type: [Number, String], default: null },
    doc: { type: Object, default: null }
  },
  data() {
    return {
      innerVisible: false,
      saving: false,
      form: {
        title: '',
        docType: 'wiki',
        status: 'draft',
        visibility: 'project',
        isPrimary: false,
        content: '',
        changeSummary: ''
      },
      rules: {
        title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
        docType: [{ required: true, message: '请选择文档类型', trigger: 'change' }],
        status: [{ required: true, message: '请选择文档状态', trigger: 'change' }],
        visibility: [{ required: true, message: '请选择可见性', trigger: 'change' }],
        content: [{ required: true, message: '请输入文档正文', trigger: 'blur' }]
      },
      typeOptions: [
        { label: 'README', value: 'readme' },
        { label: '说明文档', value: 'wiki' },
        { label: '需求规格', value: 'spec' },
        { label: '会议纪要', value: 'meeting_note' },
        { label: '设计文档', value: 'design' },
        { label: '使用手册', value: 'manual' },
        { label: '其他', value: 'other' }
      ]
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(v) {
        this.innerVisible = v
        if (v) {
          this.initForm()
        }
      }
    },
    innerVisible(v) {
      this.$emit('update:visible', v)
    },
    doc: {
      deep: true,
      handler() {
        if (this.innerVisible) {
          this.initForm()
        }
      }
    }
  },
  methods: {
    initForm() {
      const item = this.doc || {}
      this.form = {
        title: item.title || '',
        docType: item.docType || 'wiki',
        status: item.status || 'draft',
        visibility: item.visibility || 'project',
        isPrimary: !!item.isPrimary,
        content: item.content || '',
        changeSummary: ''
      }
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate()
      })
    },
    handleClose() {
      this.$emit('close')
    },
    async handleSubmit() {
      try {
        await this.$refs.formRef.validate()
        this.saving = true
        const payload = { ...this.form }
        let res = null
        if (this.mode === 'edit' && this.doc && this.doc.id) {
          res = await updateProjectDoc(this.doc.id, payload)
        } else {
          res = await createProjectDoc(this.projectId, payload)
        }
        this.$message.success(this.mode === 'edit' ? '文档已更新' : '文档已创建')
        this.innerVisible = false
        this.$emit('saved', res?.data || null)
      } catch (e) {
        if (e && e.message) {
          const m = e.response?.data?.message || e.response?.data?.msg || e.message
          if (m && !m.includes('validate')) {
            this.$message.error(m)
          }
        }
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.editor-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.preview-panel { border: 1px solid #ebeef5; border-radius: 6px; background: #fafafa; min-height: 420px; display: flex; flex-direction: column; }
.preview-title { padding: 12px 14px; border-bottom: 1px solid #ebeef5; font-weight: 600; }
.preview-content { margin: 0; padding: 14px; flex: 1; overflow: auto; white-space: pre-wrap; word-break: break-word; font-family: Consolas, Monaco, monospace; line-height: 1.6; }
@media (max-width: 900px) { .editor-layout { grid-template-columns: 1fr; } }
</style>
