
<template>
  <div class="project-settings-panel">
    <el-empty v-if="!canManageProject" description="只有项目管理员和所有者可以修改项目设置" />

    <template v-else>
      <el-tabs v-model="innerTab" type="border-card">
        <el-tab-pane label="基础设置" name="basic">
          <el-form :model="basicForm" label-width="100px">
            <el-form-item label="项目名称">
              <el-input v-model="basicForm.name" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="项目描述">
              <el-input v-model="basicForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
            </el-form-item>
            <el-form-item label="项目分类">
              <el-input v-model="basicForm.category" />
            </el-form-item>
            <el-form-item label="可见范围">
              <el-select v-model="basicForm.visibility" style="width: 100%">
                <el-option label="公开" value="public" />
                <el-option label="私有" value="private" />
              </el-select>
            </el-form-item>
            <el-form-item label="项目标签">
              <el-select v-model="basicForm.tags" multiple filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="tag in basicForm.tags" :key="tag" :label="tag" :value="tag" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="panel-footer">
            <el-button type="primary" :loading="loading" @click="emitSave">保存基础设置</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="保存为模板" name="template">
          <el-alert
            title="这里仅保留“把当前项目沉淀为模板”的能力；使用模板创建新项目，请回到“我的项目”的创建流程中完成。"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 16px;"
          />
          <el-form :model="templateForm" label-width="100px">
            <el-form-item label="模板名称">
              <el-input v-model="templateForm.name" maxlength="100" show-word-limit />
            </el-form-item>
            <el-form-item label="模板描述">
              <el-input v-model="templateForm.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item label="模板分类">
              <el-input v-model="templateForm.category" />
            </el-form-item>
            <el-form-item label="是否公开">
              <el-switch v-model="templateForm.isPublic" active-text="公开模板" inactive-text="仅自己可见" />
            </el-form-item>
          </el-form>
          <div class="panel-footer">
            <el-button type="primary" :loading="loading" @click="emitSaveTemplate">保存为模板</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script>
export default {
  name: 'ProjectSettingsPanel',
  props: {
    project: {
      type: Object,
      default: () => ({})
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    initialTab: {
      type: String,
      default: 'basic'
    }
  },
  data() {
    return {
      innerTab: 'basic',
      basicForm: {
        name: '',
        description: '',
        category: '',
        visibility: 'public',
        tags: []
      },
      templateForm: {
        name: '',
        description: '',
        category: '',
        isPublic: false
      }
    }
  },
  watch: {
    initialTab: {
      immediate: true,
      handler(val) {
        this.innerTab = val === 'template' ? 'template' : 'basic'
      }
    },
    project: {
      immediate: true,
      deep: true,
      handler(val) {
        const source = val || {}
        this.basicForm = {
          name: source.title || source.name || '',
          description: source.description || '',
          category: source.category || '',
          visibility: source.visibility || 'public',
          tags: Array.isArray(source.tags) ? [...source.tags] : []
        }
        this.templateForm = {
          name: source.name || source.title ? `${source.name || source.title}-模板` : '',
          description: source.description || '',
          category: source.category || '',
          isPublic: false
        }
      }
    }
  },
  methods: {
    emitSave() {
      this.$emit('save', { ...this.basicForm })
    },
    emitSaveTemplate() {
      this.$emit('save-template', { ...this.templateForm })
    }
  }
}
</script>

<style scoped>
.project-settings-panel {
  min-height: 260px;
}
.panel-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
