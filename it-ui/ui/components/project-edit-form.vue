<template>
  <div class="project-edit-form">
    <el-form :model="localProject" :rules="projectRules" ref="projectFormRef" label-width="100px">
      <el-form-item label="项目名称" prop="name" required>
        <el-input v-model="localProject.name" placeholder="请输入项目名称"></el-input>
      </el-form-item>
      <el-form-item label="项目描述" prop="description">
        <el-input type="textarea" v-model="localProject.description" rows="3" placeholder="请输入项目描述"></el-input>
      </el-form-item>
      <el-form-item label="项目分类" prop="category">
        <el-input v-model="localProject.category" placeholder="例如：后端开发、前端框架"></el-input>
      </el-form-item>
      <el-form-item label="项目状态" prop="status">
        <el-select v-model="localProject.status" placeholder="请选择状态">
          <el-option label="草稿" value="draft"></el-option>
          <el-option label="进行中" value="active"></el-option>
          <el-option label="已归档" value="archived"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="可见性" prop="visibility">
        <el-select v-model="localProject.visibility">
          <el-option label="公开" value="public"></el-option>
          <el-option label="私有" value="private"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="标签" prop="tags">
        <el-select
          v-model="localProject.tags"
          multiple
          placeholder="请选择标签"
          style="width: 100%"
        >
          <el-option
            v-for="tag in tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          ></el-option>
        </el-select>
        <span class="form-tip">可多选标签</span>
      </el-form-item>
      <el-form-item label="模板ID" prop="templateId">
        <el-input-number v-model="localProject.templateId" :min="0" :step="1" placeholder="可选"></el-input-number>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="$emit('cancel')">取消</el-button>
      <el-button type="primary" @click="submitForm" :loading="loading">确定</el-button>
    </span>
  </div>
</template>

<script>
import { GetAllTags } from '@/api/index'

export default {
  props: {
    project: {
      type: Object,
      default: () => ({})
    },
    isEdit: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      localProject: {
        name: '',
        description: '',
        category: '',
        status: 'draft',
        visibility: 'public',
        tags: [],
        templateId: null
      },
      projectRules: {
        name: [
          { required: true, message: '请输入项目名称', trigger: 'blur' },
          { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
        ]
      },
      tags: [],
      loading: false
    }
  },
  mounted() {
    this.initForm()
    this.loadTags()
  },
  methods: {
    initForm() {
      if (this.project) {
        this.localProject = {
          name: this.project.name || '',
          description: this.project.description || '',
          category: this.project.category || '',
          status: this.project.status || 'draft',
          visibility: this.project.visibility || 'public',
          tags: this.project.tags || [],
          templateId: this.project.templateId || null
        }
      }
    },
    async loadTags() {
      try {
        const response = await GetAllTags()
        this.tags = response.data
      } catch (error) {
        console.error('加载标签失败', error)
      }
    },
    async submitForm() {
      try {
        await this.$refs.projectFormRef.validate()
        this.loading = true

        // 构建请求数据
        const requestData = {
          name: this.localProject.name,
          description: this.localProject.description || undefined,
          category: this.localProject.category || undefined,
          status: this.localProject.status || 'draft',
          visibility: this.localProject.visibility || 'public',
          templateId: this.localProject.templateId || undefined
        }

        // 处理 tags：如果选择了标签，转换为 JSON 字符串
        if (this.localProject.tags && this.localProject.tags.length > 0) {
          requestData.tags = JSON.stringify(this.localProject.tags)
        }

        this.$emit('success', requestData)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('表单验证失败', error)
        }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.project-edit-form {
  padding: 20px 0;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  display: block;
}
</style>