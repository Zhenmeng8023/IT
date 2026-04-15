<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="side-card">
          <div slot="header" class="card-header"><span>项目设置</span></div>
          <div class="info-list">
            <div class="info-item"><span>项目名称</span><span>{{ project.title || '-' }}</span></div>
            <div class="info-item"><span>分类</span><span>{{ project.category || '-' }}</span></div>
            <div class="info-item"><span>可见性</span><span>{{ project.visibility || '-' }}</span></div>
          </div>
          <div class="toolbar-actions settings-actions-row">
            <el-button size="small" icon="el-icon-setting" @click="openSettingsDialog">编辑项目设置</el-button>
            <el-button size="small" type="primary" icon="el-icon-folder-add" @click="$emit('open-save-template')">保存为模板</el-button>
            <el-button v-if="canManageProject" size="small" type="danger" icon="el-icon-delete" :loading="deleteProjectLoading" @click="confirmDeleteProject">删除项目</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="side-card">
          <div slot="header" class="card-header"><span>说明</span></div>
          <div class="settings-tip-box">
            <div class="settings-tip-title">这里统一收口项目治理类操作</div>
            <div class="settings-tip-desc">当前统一收口项目设置、模板保存与删除项目等操作，避免把无关后端字段直接暴露到页面主区域。</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog title="项目设置" :visible.sync="settingsDialogVisible" width="620px">
      <el-form :model="settingsForm" label-width="100px">
        <el-form-item label="项目名称"><el-input v-model="settingsForm.name"></el-input></el-form-item>
        <el-form-item label="项目描述"><el-input v-model="settingsForm.description" type="textarea" :rows="4"></el-input></el-form-item>
        <el-form-item label="项目分类"><el-input v-model="settingsForm.category"></el-input></el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="settingsForm.visibility" style="width: 100%">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私有" value="private"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-select v-model="settingsForm.tags" multiple allow-create filterable default-first-option style="width: 100%">
            <el-option v-for="tag in settingsForm.tags" :key="tag" :label="tag" :value="tag"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingsLoading" @click="submitSettings">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { deleteProject as apiDeleteProject, updateProject } from '@/api/project'

export default {
  name: 'ProjectManageSettingsTab',
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    project: {
      type: Object,
      default: () => ({})
    },
    canManageProject: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      settingsDialogVisible: false,
      settingsLoading: false,
      deleteProjectLoading: false,
      settingsForm: { name: '', description: '', category: '', visibility: 'public', tags: [] }
    }
  },
  methods: {
    openSettingsDialog() {
      this.settingsForm = {
        name: this.project.title || '',
        description: this.project.description || '',
        category: this.project.category || '',
        visibility: this.project.visibility || 'public',
        tags: [...(this.project.tags || [])]
      }
      this.settingsDialogVisible = true
    },
    async submitSettings() {
      if (!this.settingsForm.name) {
        this.$message.warning('请输入项目名称')
        return
      }
      this.settingsLoading = true
      try {
        await updateProject(this.projectId, {
          name: this.settingsForm.name,
          description: this.settingsForm.description || undefined,
          category: this.settingsForm.category || undefined,
          visibility: this.settingsForm.visibility || 'public',
          tags: JSON.stringify(this.settingsForm.tags || [])
        })
        this.$message.success('项目设置更新成功')
        this.settingsDialogVisible = false
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '更新项目设置失败')
      } finally {
        this.settingsLoading = false
      }
    },
    async confirmDeleteProject() {
      try {
        await this.$prompt(`请输入项目名称“${this.project.title || ''}”确认删除`, '删除项目', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          inputValidator: value => String(value || '').trim() === String(this.project.title || '').trim() ? true : '输入的项目名称不匹配'
        })
        this.deleteProjectLoading = true
        await apiDeleteProject(this.projectId)
        this.$message.success('项目已删除')
        this.$router.push('/projectlist')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error.response?.data?.message || '删除项目失败')
        }
      } finally {
        this.deleteProjectLoading = false
      }
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.side-card { margin-bottom: 16px; }
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-item { display: flex; justify-content: space-between; gap: 12px; color: #606266; }
.settings-actions-row { margin-top: 16px; }
.settings-tip-box { padding: 8px 0; }
.settings-tip-title { font-size: 16px; font-weight: 700; color: #303133; }
.settings-tip-desc { margin-top: 8px; color: #909399; line-height: 1.8; }
</style>
