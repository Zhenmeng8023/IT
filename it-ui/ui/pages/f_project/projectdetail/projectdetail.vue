<template>
  <div class="project-detail-container">
    <!-- 页面头部导航 -->
    <div class="detail-header">
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/f_project/list' }">项目列表</el-breadcrumb-item>
          <el-breadcrumb-item>{{ project.title }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-actions">
        <el-button
          size="small"
          icon="el-icon-s-tools"
          @click="goToProjectManage"
        >
          项目管理
        </el-button>

        <el-button
          type="success"
          size="small"
          icon="el-icon-document"
          :loading="aiSummaryLoading"
          @click="handleAiSummarizeProject"
        >
          AI 总结项目
        </el-button>

        <el-button
          type="warning"
          size="small"
          icon="el-icon-s-operation"
          :loading="aiTaskLoading"
          @click="handleAiSplitProjectTasks"
        >
          AI 拆任务
        </el-button>

        <el-button
          type="primary"
          size="small"
          icon="el-icon-star"
          @click="handleStar"
          :loading="starLoading"
        >
          {{ isStarred ? '已收藏' : '收藏' }} ({{ project.starCount || 0 }})
        </el-button>

        <!-- <el-button
          size="small"
          icon="el-icon-share"
          @click="handleFork"
          :loading="forkLoading"
        >
          复刻 ({{ project.forkCount || 0 }})
        </el-button> -->

        <el-button
          size="small"
          icon="el-icon-download"
          @click="handleDownload"
        >
          下载
        </el-button>
      </div>
    </div>

    <!-- 项目基本信息 -->
    <div class="project-info-section">
      <div class="project-header">
        <div class="project-title-section">
          <h1 class="project-title">
            <i class="el-icon-s-management"></i>
            {{ project.title }}
          </h1>
          <div class="project-meta-tags">
            <el-tag 
              :type="getProjectTypeTag(project.type)" 
              size="medium"
              class="type-tag"
            >
              {{ project.type }}
            </el-tag>
            <el-tag 
              :type="getStatusTag(project.status)" 
              size="medium"
              class="status-tag"
            >
              {{ project.status }}
            </el-tag>
            <!-- 许可证标签暂时注释掉 -->
          <!-- <el-tag 
              v-if="project.license"
              type="info" 
              size="medium"
            >
              {{ project.license }}
            </el-tag> -->
          </div>
        </div>
        
        <div class="project-stats-overview">
          <div class="stat-item">
            <span class="stat-number">{{ project.starCount || 0 }}</span>
            <span class="stat-label">收藏</span>
          </div>
          <!-- 复刻统计暂时注释掉 -->
          <!-- <div class="stat-item">
            <span class="stat-number">{{ project.forkCount || 0 }}</span>
            <span class="stat-label">复刻</span>
          </div> -->
          <div class="stat-item">
            <span class="stat-number">{{ project.watchCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
          <!-- 问题统计暂时注释掉 -->
          <!-- <div class="stat-item">
            <span class="stat-number">{{ project.issueCount || 0 }}</span>
            <span class="stat-label">问题</span>
          </div> -->
        </div>
      </div>

      <!-- 作者信息 -->
      <div class="author-section">
        <div class="author-info">
          <el-avatar :size="40" :src="project.author?.avatar" class="author-avatar"></el-avatar>
          <div class="author-details">
            <span class="author-name">{{ project.author?.nickname || '未知作者' }}</span>
            <span class="publish-time">发布于 {{ formatTime(project.createTime) }}</span>
          </div>
        </div>
        <div class="project-actions">
          <!-- 反馈问题功能暂时注释掉 -->
          <!-- <el-button type="text" icon="el-icon-chat-dot-round" @click="showIssues = true">
            反馈问题
          </el-button> -->
          <el-button type="text" icon="el-icon-edit" @click="showEditDialog = true">
            编辑信息
          </el-button>
        </div>
      </div>
    </div>

    <!-- 代码浏览区域 -->
    <div class="code-browser-section" v-if="showCodeBrowser">
      <div class="code-browser-header">
        <h2 class="section-title">
          <i class="el-icon-folder-opened"></i>
          代码浏览
        </h2>
        <div class="browser-actions">
          <el-button 
            size="small" 
            icon="el-icon-refresh"
            @click="refreshCodeBrowser"
          >
            刷新
          </el-button>
          <el-button 
            size="small" 
            icon="el-icon-close"
            @click="closeCodeBrowser"
          >
            关闭
          </el-button>
        </div>
      </div>
      
      <div class="code-browser-content">
        <!-- 左侧目录树 -->
        <div class="file-tree-panel">
          <div class="tree-header">
            <span class="tree-title">文件结构</span>
            <el-input
              v-model="treeFilterText"
              placeholder="搜索文件..."
              size="small"
              prefix-icon="el-icon-search"
              clearable
            />
          </div>
          <div class="tree-container">
            <el-tree
              ref="fileTreeRef"
              :data="codeFileTree"
              :props="codeTreeProps"
              :filter-node-method="filterNode"
              node-key="path"
              default-expand-all
              highlight-current
              @node-click="handleFileClick"
            >
              <template #default="{data }">
                <span class="tree-node-item">
                  <i :class="getFileIconClass(data)"></i>
                  <span class="file-name">{{ data.name }}</span>
                  <span v-if="data.type === 'file'" class="file-size">{{ formatFileSize(data.size) }}</span>
                  <span v-if="data.type === 'file'" class="file-actions">
                    <el-tooltip content="设为主文件" placement="top">
                      <el-button 
                        size="mini" 
                        type="text" 
                        icon="el-icon-star-on"
                        :class="{ 'is-primary': data.isMainFile }"
                        @click.stop="setAsMainFile(data)"
                      ></el-button>
                    </el-tooltip>
                    <el-tooltip content="删除文件" placement="top">
                      <el-button 
                        size="mini" 
                        type="text" 
                        icon="el-icon-delete"
                        @click.stop="deleteFile(data)"
                      ></el-button>
                    </el-tooltip>
                  </span>
                </span>
              </template>
            </el-tree>
          </div>
        </div>
        
        <!-- 右侧代码编辑器 -->
        <div class="code-editor-panel">
          <div class="editor-header">
            <div class="file-info">
              <i :class="getFileIconClass(currentFile)"></i>
              <span class="file-path">{{ currentFilePath }}</span>
              <el-tag v-if="currentFile.language" size="small">{{ currentFile.language }}</el-tag>
              <el-tag v-if="currentFile.size" size="small">{{ formatFileSize(currentFile.size) }}</el-tag>
            </div>
            <div class="editor-actions">
              <el-button
                size="small"
                icon="el-icon-upload2"
                :disabled="!currentFile.id"
                @click="openVersionDialog"
              >
                上传新版本
              </el-button>
              <el-button 
                size="small" 
                icon="el-icon-copy-document"
                @click="copyFileContent"
              >
                复制
              </el-button>
              <el-button 
                size="small" 
                icon="el-icon-download"
                @click="downloadFile"
              >
                下载
              </el-button>
            </div>
          </div>
          
          <div class="code-editor-container">
            <pre v-if="currentFile.content" class="code-content">
            <code :class="'language-' + (currentFile.language || 'text')">{{ currentFile.content }}</code>
            </pre>
            <div v-else class="no-file-selected">
              <i class="el-icon-document"></i>
              <p>请选择左侧文件查看代码内容</p>
            </div>
          </div>

          <div v-if="currentFile.id" class="file-version-panel">
            <div class="version-panel-header">
              <span class="version-title">版本记录</span>
              <span class="version-count">{{ currentFile.versions.length }} 个版本</span>
            </div>
            <div v-if="currentFile.versions.length > 0" class="version-list">
              <div v-for="version in currentFile.versions" :key="version.id" class="version-item">
                <div class="version-main">
                  <span class="version-name">{{ version.version || '未命名版本' }}</span>
                  <span class="version-time">{{ formatTime(version.uploadedAt) }}</span>
                </div>
                <div class="version-sub">{{ version.commitMessage || '未填写版本说明' }}</div>
              </div>
            </div>
            <div v-else class="version-empty">当前文件暂无版本记录</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧内容区域 -->
      <div class="content-left">
        <!-- 项目描述 -->
        <el-card class="description-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">项目描述</span>
            </div>
          </template>
          <div class="description-content">
            <p>{{ project.description }}</p>
            <div v-if="project.features && project.features.length > 0" class="features-section">
              <h4>主要特性：</h4>
              <ul>
                <li v-for="feature in project.features" :key="feature">{{ feature }}</li>
              </ul>
            </div>
            <!-- 添加代码浏览入口 -->
            <div class="code-browser-entry">
              <el-button 
                type="primary" 
                icon="el-icon-folder-opened" 
                @click="openCodeBrowser"
              >
                浏览项目代码
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card
          v-if="showAiProjectPanel"
          class="ai-project-card"
          shadow="never"
        >
          <template #header>
            <div class="card-header ai-card-header">
              <span class="card-title">
                <i class="el-icon-magic-stick"></i>
                AI 项目辅助结果
              </span>

              <div class="ai-card-tabs">
                <el-button
                  size="mini"
                  :type="aiActiveTab === 'summary' ? 'primary' : 'default'"
                  plain
                  @click="aiActiveTab = 'summary'"
                >
                  项目总结
                </el-button>
                <el-button
                  size="mini"
                  :type="aiActiveTab === 'tasks' ? 'primary' : 'default'"
                  plain
                  @click="aiActiveTab = 'tasks'"
                >
                  任务拆解
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="aiActiveTab === 'summary'" class="ai-result-content">
            <pre>{{ aiProjectSummary || '暂无项目总结' }}</pre>
          </div>

          <div v-else class="ai-result-content">
            <pre>{{ aiProjectTasks || '暂无任务拆解' }}</pre>
          </div>
        </el-card>

        <!-- 技术栈 -->
        <el-card class="tech-stack-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">技术栈</span>
            </div>
          </template>
          <div class="tech-stack-content">
            <el-tag
              v-for="tagId in (project.technologies || []).slice(0, 4)"
              :key="tagId"
              type="success"
              size="medium"
              class="tech-tag"
              @click="filterByTech(tagId)"
            >
              {{ getTagName(tagId) }}
            </el-tag>
          </div>
        </el-card>

        <!-- 文件结构 -->
        <el-card class="file-structure-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">文件结构</span>
            </div>
          </template>
          <div class="file-tree">
            <el-tree
              :data="fileTree"
              :props="treeProps"
              node-key="path"
              default-expand-all
              :expand-on-click-node="false"
            >
              <template #default="{data }">
                <span class="tree-node">
                  <i :class="getFileIcon(data)"></i>
                  <span>{{ data.name }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>

        <!-- README文档 -->
        <el-card class="readme-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">README.md</span>
            </div>
          </template>
          <div class="readme-content" v-html="renderedReadme"></div>
        </el-card>
      </div>

      <!-- 右侧边栏 -->
      <div class="content-right">
        <!-- 项目信息卡片 -->
        <el-card class="info-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">项目信息</span>
            </div>
          </template>
          <div class="info-content">
            <div class="info-item">
              <span class="info-label">版本：</span>
              <span class="info-value">{{ project.version || 'v1.0.0' }}</span>
            </div>
            <!-- 许可证信息暂时注释掉 -->
            <!-- <div class="info-item">
              <span class="info-label">许可证：</span>
              <span class="info-value">{{ project.license || 'MIT' }}</span>
            </div> -->
            <div class="info-item">
              <span class="info-label">最后更新：</span>
              <span class="info-value">{{ formatTime(project.updateTime) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">语言：</span>
              <span class="info-value">{{ project.language || 'JavaScript' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">大小：</span>
              <span class="info-value">{{ project.size || '2.5 MB' }}</span>
            </div>
          </div>
        </el-card>

        <!-- 贡献者 -->
        <el-card class="contributors-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">贡献者</span>
            </div>
          </template>
          <div class="contributors-content">
            <div v-for="contributor in contributors" :key="contributor.id" class="contributor-item">
              <el-avatar :size="32" :src="contributor.avatar"></el-avatar>
              <span class="contributor-name">{{ contributor.name }}</span>
            </div>
          </div>
        </el-card>

        <!-- 相关项目 -->
        <el-card class="related-projects-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">相关项目</span>
            </div>
          </template>
          <div class="related-projects-content">
            <div v-for="related in relatedProjects" :key="related.id" class="related-project-item">
              <h4 @click="goToDetail(related.id)" class="related-title">{{ related.title }}</h4>
              <p class="related-desc">{{ related.description }}</p>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog
      title="编辑项目信息"
      :visible.sync="showEditDialog"
      width="600px"
      @open="resetEditFormFromProject"
    >
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="editForm.name" placeholder="请输入项目名称"></el-input>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入项目描述"></el-input>
        </el-form-item>
        <el-form-item label="项目分类">
          <el-input v-model="editForm.category" placeholder="例如：frontend / backend / ai"></el-input>
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="草稿" value="draft"></el-option>
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已发布" value="published"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
            <el-option label="已归档" value="archived"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="editForm.visibility" style="width: 100%">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私有" value="private"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-select v-model="editForm.tags" multiple allow-create filterable default-first-option placeholder="输入后回车添加标签" style="width: 100%">
            <el-option v-for="tag in editForm.tags" :key="tag" :label="tag" :value="tag"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEditProject">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="上传文件新版本"
      :visible.sync="versionDialogVisible"
      width="520px"
      @close="resetVersionForm"
    >
      <el-form :model="versionForm" label-width="100px">
        <el-form-item label="当前文件">
          <div class="dialog-file-name">{{ currentFile.name || '未选择文件' }}</div>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="versionForm.version" placeholder="例如：1.1 / 2.0.0"></el-input>
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="versionForm.commitMessage" type="textarea" :rows="3" placeholder="请输入本次更新说明"></el-input>
        </el-form-item>
        <el-form-item label="选择文件">
          <input class="native-file-input" type="file" @change="handleVersionFileChange" />
          <div class="file-input-tip">请选择要上传的新文件版本</div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="versionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="versionLoading" @click="submitUploadNewVersion">上传</el-button>
      </span>
    </el-dialog>

    <!-- 问题反馈对话框暂时注释掉 -->
    <!-- <el-dialog
      title="反馈问题"
      v-model="showIssues"
      width="700px"
    >
      <issue-report-form 
        :project-id="projectId"
        @success="handleIssueSuccess"
        @cancel="showIssues = false"
      />
    </el-dialog> -->
  </div>
</template>


<script>

import {
  getProjectDetail,
  updateProject,
  listProjectFiles,
  listFileVersions,
  setMainFile as apiSetMainFile,
  deleteFile as apiDeleteFile,
  downloadFile as apiDownloadFile,
  uploadProjectFile,
  uploadFileNewVersion,
  pageProjects
} from '@/api/project'
import { GetAllTags } from '@/api/index'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import { aiSummarizeProject, aiSplitProjectTasks, buildProjectAiPayload } from '@/api/aiAssistant'
import 'highlight.js/styles/github.css'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('vue', xml)

const STATUS_LABEL_MAP = {
  draft: '草稿',
  pending: '待审核',
  published: '已发布',
  rejected: '已拒绝',
  archived: '已归档'
}

const STATUS_TAG_MAP = {
  draft: 'info',
  pending: 'warning',
  published: 'success',
  rejected: 'danger',
  archived: 'info',
  '草稿': 'info',
  '待审核': 'warning',
  '已发布': 'success',
  '已拒绝': 'danger',
  '已归档': 'info'
}

const TYPE_LABEL_MAP = {
  frontend: '前端项目',
  backend: '后端项目',
  fullstack: '全栈项目',
  mobile: '移动应用',
  ai: 'AI 项目',
  tools: '工具项目'
}

const TEXT_FILE_EXTENSIONS = new Set([
  'js', 'ts', 'vue', 'json', 'md', 'txt', 'html', 'htm', 'css', 'scss', 'less',
  'java', 'kt', 'xml', 'yml', 'yaml', 'sql', 'sh', 'py', 'rb', 'go', 'rs', 'c',
  'cpp', 'h', 'hpp', 'cs', 'php', 'ini', 'log', 'properties'
])

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (e) {}
    return tags.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

function formatDate(dateLike) {
  if (!dateLike) return ''
  const date = new Date(dateLike)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('zh-CN')
}

function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  link.click()
  URL.revokeObjectURL(url)
}

function createEmptyCurrentFile() {
  return {
    id: null,
    path: '',
    name: '',
    type: '',
    language: '',
    size: null,
    content: '',
    isBinary: false,
    isMainFile: false,
    versions: []
  }
}

export default {
  layout: 'project',
  data() {
    return {
      projectId: null,
      rawProject: null,
      project: {
        id: null,
        title: '',
        type: '',
        status: '',
        description: '',
        technologies: [],
        features: [],
        author: {},
        starCount: 0,
        forkCount: 0,
        watchCount: 0,
        issueCount: 0,
        createTime: '',
        updateTime: '',
        license: '',
        version: '',
        language: '',
        size: '',
        readme: '',
        visibility: '',
        members: [],
        tasks: [],
        files: []
      },
      contributors: [],
      relatedProjects: [],
      fileTree: [],
      isStarred: false,
      tags: [], // 标签列表
      starLoading: false,
      forkLoading: false,
      showEditDialog: false,
      editLoading: false,
      editForm: {
        name: '',
        description: '',
        category: '',
        status: 'draft',
        visibility: 'public',
        tags: []
      },
      treeProps: {
        children: 'children',
        label: 'name'
      },
      aiSummaryLoading: false,
      aiTaskLoading: false,
      showAiProjectPanel: false,
      aiActiveTab: 'summary',
      aiProjectSummary: '',
      aiProjectTasks: '',
      showCodeBrowser: false,
      codeFileTree: [],
      currentFile: createEmptyCurrentFile(),
      treeFilterText: '',
      codeTreeProps: {
        children: 'children',
        label: 'name'
      },
      uploadLoading: false,
      versionDialogVisible: false,
      versionLoading: false,
      versionForm: {
        version: '',
        commitMessage: '',
        file: null
      }
    }
  },
  computed: {
    renderedReadme() {
      return this.project.readme ? this.project.readme.replace(/\n/g, '<br>') : '暂无 README 文档'
    },
    currentFilePath() {
      return this.currentFile.path || '未选择文件'
    }
  },
  watch: {
    treeFilterText(val) {
      if (this.$refs.fileTreeRef && this.$refs.fileTreeRef.filter) {
        this.$refs.fileTreeRef.filter(val)
      }
    },
    'currentFile.content': {
      handler() {
        this.$nextTick(() => this.highlightCode())
      },
      immediate: true
    }
  },
  async mounted() {
    this.projectId = this.$route.params.id || this.$route.query.projectId
    if (!this.projectId) {
      this.$message.error('项目ID不存在')
      return
    }
    await this.fetchTags()
    await this.fetchProjectDetail()
    await this.fetchRelatedProjects()
    await this.fetchFileTree()
  },
  methods: {
    parseTags,
    mapProjectType(type) {
      return TYPE_LABEL_MAP[type] || type || '未分类'
    },
    mapProjectStatus(status) {
      return STATUS_LABEL_MAP[status] || status || '未知状态'
    },
    resetEditFormFromProject() {
      this.editForm = {
        name: this.project.title || '',
        description: this.project.description || '',
        category: this.rawProject?.category || '',
        status: this.rawProject?.status || 'draft',
        visibility: this.rawProject?.visibility || 'public',
        tags: [...(this.project.technologies || [])]
      }
    },
    async submitEditProject() {
      if (!this.editForm.name) {
        this.$message.warning('请输入项目名称')
        return
      }
      this.editLoading = true
      try {
        await updateProject(this.projectId, {
          name: this.editForm.name,
          description: this.editForm.description || undefined,
          category: this.editForm.category || undefined,
          status: this.editForm.status || 'draft',
          visibility: this.editForm.visibility || 'public',
          tags: JSON.stringify(this.editForm.tags || [])
        })
        this.$message.success('项目信息更新成功')
        this.showEditDialog = false
        await this.fetchProjectDetail()
        await this.fetchRelatedProjects()
      } catch (error) {
        console.error('更新项目失败:', error)
        this.$message.error(error.response?.data?.message || '更新项目失败')
      } finally {
        this.editLoading = false
      }
    },
    async fetchProjectDetail() {
      try {
        
        const response = await getProjectDetail(this.projectId)
        const apiData = response.data || {}
        this.rawProject = apiData
        const technologies = this.parseTagsWithNames(apiData.tags)
        this.project = {
          id: apiData.id,
          title: apiData.name,
          type: this.mapProjectType(apiData.category),
          status: this.mapProjectStatus(apiData.status),
          description: apiData.description || '暂无项目描述',
          technologies,
          features: [],
          author: {
            id: apiData.authorId,
            nickname: apiData.authorName || '未知作者',
            avatar: apiData.authorAvatar || ''
          },
          starCount: apiData.stars || 0,
          forkCount: 0,
          watchCount: apiData.views || 0,
          issueCount: (apiData.tasks || []).length || 0,
          createTime: apiData.createdAt,
          updateTime: apiData.updatedAt,
          license: '',
          version: apiData.files && apiData.files[0] ? apiData.files[0].version || '' : '',
          language: technologies[0] || '',
          size: apiData.sizeMb != null ? `${apiData.sizeMb} MB` : '',
          readme: '',
          visibility: apiData.visibility || '',
          members: apiData.members || [],
          tasks: apiData.tasks || [],
          files: apiData.files || []
        }
        this.contributors = this.buildContributors(apiData)
        this.resetEditFormFromProject()
      } catch (error) {
        console.error('获取项目详情失败:', error)
        this.$message.error(error.response?.data?.message || '获取项目详情失败')
      }
    },
    
    // 获取标签列表
    async fetchTags() {
  try {
    const response = await GetAllTags()
    console.log('GetAllTags响应:', response)
    // 检查响应结构，不同的API可能有不同的响应格式
    if (response.data && Array.isArray(response.data)) {
      this.tags = response.data
      console.log('标签列表加载成功:', this.tags)
    } else if (response.success && Array.isArray(response.data)) {
      this.tags = response.data
      console.log('标签列表加载成功:', this.tags)
    } else {
      console.warn('获取标签列表失败，响应数据异常:', response)
      // 设置一个默认的标签列表，避免后续逻辑出错
      this.tags = []
    }
  } catch (error) {
    console.error('获取标签列表失败:', error)
    // 设置一个空的标签列表，避免后续逻辑出错
    this.tags = []
  }
},
    
    // 解析标签并转换为标签名称
    parseTagsWithNames(tags) {
      if (!tags) return []
      if (Array.isArray(tags)) {
        return tags.filter(Boolean).map(tagId => this.getTagName(tagId))
      }
      if (typeof tags === 'string') {
        try {
          const parsed = JSON.parse(tags)
          if (Array.isArray(parsed)) {
            return parsed.filter(Boolean).map(tagId => this.getTagName(tagId))
          }
        } catch (e) {}
        return tags.split(',').map(item => item.trim()).filter(Boolean)
      }
      return []
    },
    buildContributors(apiData) {
      const list = []
      if (apiData.authorId) {
        list.push({
          id: `author-${apiData.authorId}`,
          userId: apiData.authorId,
          name: apiData.authorName || '项目作者',
          avatar: apiData.authorAvatar || '',
          role: 'owner'
        })
      }
      ;(apiData.members || []).forEach(member => {
        const exists = list.some(item => Number(item.userId) === Number(member.userId))
        if (!exists) {
          list.push({
            id: member.id,
            userId: member.userId,
            name: member.nickname || member.username || `用户${member.userId}`,
            avatar: member.avatar || '',
            role: member.role
          })
        }
      })
      return list.slice(0, 10)
    },
    async fetchRelatedProjects() {
      if (!this.rawProject || !this.rawProject.category) {
        this.relatedProjects = []
        return
      }
      try {
        const response = await pageProjects({ page: 1, size: 6, category: this.rawProject.category, sortBy: 'latest' })
        this.relatedProjects = (response.data?.list || [])
          .filter(item => Number(item.id) !== Number(this.projectId))
          .slice(0, 4)
          .map(item => ({ id: item.id, title: item.name, description: item.description || '暂无项目描述' }))
      } catch (error) {
        console.error('获取相关项目失败:', error)
        this.relatedProjects = []
      }
    },
    async fetchFileTree(preserveCurrentFile = true) {
      if (!this.projectId) return
      const currentFileId = preserveCurrentFile ? this.currentFile.id : null
      try {
        const response = await listProjectFiles(this.projectId)
        const files = response.data || []
        const tree = this.transformFilesToTree(files)
        this.codeFileTree = tree
        this.fileTree = tree
        this.project.files = files
        await this.loadReadmeIfExists(files)
        if (currentFileId) {
          const matched = tree.find(item => Number(item.id) === Number(currentFileId))
          if (matched) await this.handleFileClick(matched)
        }
      } catch (error) {
        console.error('获取文件结构失败:', error)
        this.$message.error(error.response?.data?.message || '获取文件结构失败')
      }
    },
    transformFilesToTree(files) {
      return (files || []).map(file => {
        const name = file.fileName || file.name || '未命名文件'
        const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : (file.fileType || '')
        return {
          id: file.id,
          name,
          path: file.filePath || name,
          type: 'file',
          size: file.fileSizeBytes || 0,
          language: ext,
          extension: ext,
          isMainFile: !!file.isMain,
          version: file.version || '',
          uploadTime: file.uploadTime || file.updatedAt || '',
          versions: file.versions || []
        }
      })
    },
    async loadReadmeIfExists(files) {
      const readmeFile = (files || []).find(file => /(^|\/)readme(\.md|\.txt)?$/i.test(file.fileName || ''))
      if (!readmeFile) {
        this.project.readme = ''
        return
      }
      try {
        const blob = await apiDownloadFile(readmeFile.id)
        this.project.readme = await blob.text()
      } catch (error) {
        console.error('读取 README 失败:', error)
      }
    },
    getCurrentAiUserId() {
      const normalizeId = (value) => {
        if (value === null || value === undefined || value === '') return null
        const num = Number(value)
        return Number.isNaN(num) ? value : num
      }
      const collectCandidateIds = (source, out = []) => {
        if (!source || typeof source !== 'object') return out
        const directKeys = ['id', 'userId', 'uid']
        const nestedKeys = ['user', 'userInfo', 'profile', 'account', 'currentUser', 'loginUser', 'data']

        directKeys.forEach((key) => {
          const value = normalizeId(source[key])
          if (value !== null && value !== undefined && value !== '') out.push(value)
        })

        nestedKeys.forEach((key) => {
          const nested = source[key]
          if (nested && nested !== source && typeof nested === 'object') {
            collectCandidateIds(nested, out)
          }
        })

        return out
      }
      const pickFirstValidId = (values) => {
        for (const value of values) {
          const normalized = normalizeId(value)
          if (normalized !== null && normalized !== undefined && normalized !== '') {
            return normalized
          }
        }
        return null
      }

      if (this.$store && this.$store.state) {
        const s = this.$store.state
        const moduleKeys = Object.keys(s || {})
        const storeCandidates = []

        moduleKeys.forEach((key) => {
          collectCandidateIds(s[key], storeCandidates)
        })
        collectCandidateIds(s, storeCandidates)

        const storeId = pickFirstValidId(storeCandidates)
        if (storeId !== null) return storeId
      }

      if (process.client) {
        try {
          const storageKeys = [
            'userInfo',
            'user',
            'currentUser',
            'loginUser',
            'user_info',
            'login_user',
            'profile',
            'auth',
            'auth_user',
            'account'
          ]

          const localCandidates = []
          storageKeys.forEach((key) => {
            const raw = localStorage.getItem(key)
            if (!raw) return
            try {
              const parsed = JSON.parse(raw)
              collectCandidateIds(parsed, localCandidates)
            } catch (e) {
              const normalized = normalizeId(raw)
              if (normalized !== null) localCandidates.push(normalized)
            }
          })

          const tokenUserId = normalizeId(localStorage.getItem('userId'))
          if (tokenUserId !== null) localCandidates.push(tokenUserId)

          const localId = pickFirstValidId(localCandidates)
          if (localId !== null) return localId
        } catch (e) {
          console.error('读取本地用户信息失败:', e)
        }
      }

      if (this.project && this.project.author && this.project.author.id) {
        return normalizeId(this.project.author.id)
      }

      return null
    },
    async handleAiSummarizeProject() {
      const userId = this.getCurrentAiUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }
      this.aiSummaryLoading = true
      try {
        const result = await aiSummarizeProject({
          userId,
          projectId: this.projectId,
          title: this.project.title,
          content: buildProjectAiPayload(this.project),
          project: this.project
        })
        if (!result) {
          this.$message.warning('AI 未返回项目总结')
          return
        }
        this.aiProjectSummary = result
        this.aiActiveTab = 'summary'
        this.showAiProjectPanel = true
        this.$message.success('AI 项目总结生成成功')
      } catch (error) {
        console.error('AI 项目总结失败:', error)
        this.$message.error('AI 项目总结失败，请稍后重试')
      } finally {
        this.aiSummaryLoading = false
      }
    },
    async handleAiSplitProjectTasks() {
      const userId = this.getCurrentAiUserId()
      if (!userId) {
        this.$message.warning('请先登录')
        return
      }
      this.aiTaskLoading = true
      try {
        const result = await aiSplitProjectTasks({
          userId,
          projectId: this.projectId,
          title: this.project.title,
          content: buildProjectAiPayload(this.project),
          project: this.project
        })
        if (!result) {
          this.$message.warning('AI 未返回任务拆解结果')
          return
        }
        this.aiProjectTasks = result
        this.aiActiveTab = 'tasks'
        this.showAiProjectPanel = true
        this.$message.success('AI 任务拆解生成成功')
      } catch (error) {
        console.error('AI 任务拆解失败:', error)
        this.$message.error('AI 任务拆解失败，请稍后重试')
      } finally {
        this.aiTaskLoading = false
      }
    },
    async setAsMainFile(fileData) {
      if (fileData.type !== 'file') return
      try {
        await apiSetMainFile(fileData.id)
        this.$message.success(`已将 ${fileData.name} 设为主文件`)
        await this.fetchFileTree()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '设置主文件失败')
      }
    },
    async deleteFile(fileData) {
      if (fileData.type !== 'file') return
      try {
        await this.$confirm(`确定要删除文件 “${fileData.name}” 吗？此操作不可撤销。`, '提示', { type: 'warning' })
        await apiDeleteFile(fileData.id)
        this.$message.success('文件删除成功')
        if (this.currentFile.id === fileData.id) this.currentFile = createEmptyCurrentFile()
        await this.fetchFileTree(false)
      } catch (error) {
        if (error !== 'cancel') this.$message.error(error.response?.data?.message || '删除失败')
      }
    },
    openCodeBrowser() {
      this.showCodeBrowser = true
      if (this.codeFileTree.length === 0) this.fetchFileTree()
    },
    closeCodeBrowser() {
      this.showCodeBrowser = false
    },
    async refreshCodeBrowser() {
      this.codeFileTree = []
      this.fileTree = []
      this.currentFile = createEmptyCurrentFile()
      this.treeFilterText = ''
      await this.fetchFileTree(false)
      this.$message.success('刷新成功')
    },
    isPreviewableTextFile(fileNode) {
      if (!fileNode) return false
      const ext = (fileNode.extension || fileNode.language || '').toLowerCase()
      return TEXT_FILE_EXTENSIONS.has(ext)
    },
    async loadFileVersions(fileId) {
      try {
        const response = await listFileVersions(fileId)
        return response.data || []
      } catch (error) {
        console.error('获取文件版本失败:', error)
        return []
      }
    },
    async handleFileClick(data) {
      if (data.type !== 'file') return
      const loading = this.$loading({ target: '.code-editor-container', text: '加载文件中...' })
      try {
        const [blob, versions] = await Promise.all([
          apiDownloadFile(data.id),
          this.loadFileVersions(data.id)
        ])
        const isTextFile = this.isPreviewableTextFile(data)
        let content = ''
        let isBinary = false
        if (isTextFile) content = await blob.text()
        else {
          content = '该文件为二进制文件，暂不支持在线预览，请使用下载功能查看。'
          isBinary = true
        }
        this.currentFile = { ...data, content, isBinary, versions }
      } catch (error) {
        console.error('文件内容加载失败:', error)
        this.$message.error(error.response?.data?.message || '文件内容加载失败')
        this.currentFile = { ...data, content: '加载失败，请稍后重试。', isBinary: false, versions: [] }
      } finally {
        loading.close()
      }
    },
    highlightCode() {
      this.$nextTick(() => {
        const blocks = document.querySelectorAll('.code-content code')
        blocks.forEach(block => hljs.highlightElement(block))
      })
    },
    copyFileContent() {
      if (!this.currentFile.content) {
        this.$message.warning('没有可复制的内容')
        return
      }
      navigator.clipboard.writeText(this.currentFile.content)
        .then(() => this.$message.success('复制成功'))
        .catch(() => this.$message.error('复制失败'))
    },
    async downloadFile() {
      if (!this.currentFile.id) {
        this.$message.warning('请先选择文件')
        return
      }
      try {
        const blob = await apiDownloadFile(this.currentFile.id)
        triggerBlobDownload(blob, this.currentFile.name)
        this.$message.success('下载开始')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '下载失败')
      }
    },
    openFileDialog() {
      const fileInput = document.createElement('input')
      fileInput.type = 'file'
      fileInput.accept = '*'
      fileInput.style.display = 'none'
      fileInput.addEventListener('change', async (event) => {
        const file = event.target.files[0]
        if (file) await this.uploadFile(file)
      })
      document.body.appendChild(fileInput)
      fileInput.click()
      document.body.removeChild(fileInput)
    },
    async uploadFile(file) {
      if (!file) {
        this.$message.error('请选择要上传的文件')
        return
      }
      this.uploadLoading = true
      try {
        const formData = new FormData()
        formData.append('projectId', this.projectId)
        formData.append('file', file)
        formData.append('isMain', 'false')
        formData.append('version', '1.0')
        formData.append('commitMessage', '前端上传文件')
        await uploadProjectFile(this.projectId, formData)
        this.$message.success('文件上传成功')
        await this.fetchFileTree(false)
      } catch (error) {
        console.error('文件上传失败:', error)
        this.$message.error(error.response?.data?.message || '文件上传失败')
      } finally {
        this.uploadLoading = false
      }
    },
    openVersionDialog() {
      if (!this.currentFile.id) {
        this.$message.warning('请先选择文件')
        return
      }
      this.versionDialogVisible = true
      this.versionForm.version = this.currentFile.version || ''
    },
    handleVersionFileChange(event) {
      this.versionForm.file = event.target.files && event.target.files[0] ? event.target.files[0] : null
    },
    resetVersionForm() {
      this.versionForm = { version: this.currentFile.version || '', commitMessage: '', file: null }
    },
    async submitUploadNewVersion() {
      if (!this.currentFile.id) {
        this.$message.warning('请先选择文件')
        return
      }
      if (!this.versionForm.file) {
        this.$message.warning('请选择要上传的新版本文件')
        return
      }
      this.versionLoading = true
      try {
        const formData = new FormData()
        formData.append('file', this.versionForm.file)
        if (this.versionForm.version) formData.append('version', this.versionForm.version)
        if (this.versionForm.commitMessage) formData.append('commitMessage', this.versionForm.commitMessage)
        await uploadFileNewVersion(this.currentFile.id, formData)
        this.$message.success('新版本上传成功')
        this.versionDialogVisible = false
        await this.fetchFileTree()
      } catch (error) {
        console.error('上传新版本失败:', error)
        this.$message.error(error.response?.data?.message || '上传新版本失败')
      } finally {
        this.versionLoading = false
      }
    },
    filterNode(value, data) {
      if (!value) return true
      return (data.name || '').toLowerCase().includes(value.toLowerCase())
    },
    getFileIconClass(data) {
      if (data.type === 'folder') return 'el-icon-folder'
      const ext = (data.extension || data.language || (data.name || '').split('.').pop() || '').toLowerCase()
      const iconMap = { js: 'el-icon-document', vue: 'el-icon-document', json: 'el-icon-document', md: 'el-icon-document', html: 'el-icon-document', css: 'el-icon-document', java: 'el-icon-document' }
      return iconMap[ext] || 'el-icon-document'
    },
    formatFileSize(bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return `${bytes} B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
      return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    },
    async handleStar() {
      this.starLoading = true
      try {
        this.$message.warning('当前后端暂未提供项目收藏接口，无法真正保存收藏状态')
      } finally {
        this.starLoading = false
      }
    },
    async handleFork() {
      this.forkLoading = true
      try {
        this.$message.warning('当前后端暂未提供项目复刻接口')
      } finally {
        this.forkLoading = false
      }
    },
    async handleDownload() {
      try {
        const mainFile = this.codeFileTree.find(item => item.isMainFile) || this.codeFileTree[0]
        if (!mainFile) {
          this.$message.warning('当前项目暂无可下载文件')
          return
        }
        const blob = await apiDownloadFile(mainFile.id)
        triggerBlobDownload(blob, mainFile.name)
        this.$message.success('当前后端暂无项目打包下载接口，已为你下载主文件')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '下载失败')
      }
    },
    goToProjectManage() {
      this.$router.push(`/projectmanage?projectId=${this.projectId}`)
    },
    getFileIcon(data) {
      if (data.children && data.children.length) return 'el-icon-folder'
      return this.getFileIconClass(data)
    },
    getProjectTypeTag(type) {
      const typeMap = { '前端项目': 'primary', '后端项目': 'success', '全栈项目': 'warning', '移动应用': 'success', 'AI 项目': 'danger', '工具项目': 'info' }
      return typeMap[type] || 'info'
    },
    getStatusTag(status) {
      return STATUS_TAG_MAP[status] || 'info'
    },

    // 根据标签ID获取标签名称
    getTagName(tagId) {
    console.log('getTagName called with tagId:', tagId)
    console.log('this.tags array:', this.tags)

    // 检查tagId是否已经是字符串（如"标签1"）
    if (typeof tagId === 'string' && tagId.startsWith('标签')) {
      return tagId
    }

    // 尝试通过ID查找标签
    const tag = this.tags.find(t => t.id === Number(tagId))
    console.log('Found tag:', tag)

    return tag ? tag.name : `${tagId}`
  },

    formatTime(time) {
      return formatDate(time)
    },
    filterByTech(tech) {
      this.$router.push({ path: '/projectlist', query: { tech } })
    },
    goToDetail(id) {
      this.$router.push(`/projectdetail?projectId=${id}`)
    }
  }
}

</script>


<style scoped>
/* 样式保持原样，此处省略，但实际使用时需保留原有样式 */
.project-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 头部导航样式 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eaeaea;
}

.breadcrumb {
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* 项目信息区域 */
.project-info-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.project-title-section .project-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.project-meta-tags {
  display: flex;
  gap: 8px;
}

.project-stats-overview {
  display: flex;
  gap: 30px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

/* 作者信息 */
.author-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-details {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-weight: 500;
  color: #333;
}

.publish-time {
  font-size: 12px;
  color: #999;
}

/* 主要内容区域 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
}

.content-left {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 卡片样式 */
.el-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 技术栈标签 */
.tech-stack-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.tech-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* 文件树 */
.file-tree {
  max-height: 300px;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* README内容 */
.readme-content {
  line-height: 1.6;
  color: #333;
}

.readme-content h1, .readme-content h2, .readme-content h3 {
  margin-top: 20px;
  margin-bottom: 10px;
}

.readme-content ul {
  padding-left: 20px;
}

.readme-content li {
  margin-bottom: 5px;
}

/* 信息卡片 */
.info-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  color: #666;
  font-size: 14px;
}

.info-value {
  color: #333;
  font-weight: 500;
}

/* 贡献者 */
.contributors-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 代码浏览区域样式 */
.code-browser-section {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.code-browser-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.browser-actions {
  display: flex;
  gap: 10px;
}

.code-browser-content {
  display: grid;
  grid-template-columns: 300px 1fr;
  height: 600px;
}

/* 文件树面板 */
.file-tree-panel {
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}

.tree-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tree-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  width: 100%;
}

.file-name {
  flex: 1;
  font-size: 14px;
}

.file-size {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-item:hover .file-actions {
  opacity: 1;
}

.file-actions .el-button {
  padding: 4px;
  min-height: auto;
}

.file-actions .el-button.is-primary {
  color: #091119;
}

/* 代码编辑器面板 */
/* 代码编辑器面板 */
.code-editor-panel {
  display: flex;
  flex-direction: column;
  height: 100%;           /* 填满父容器高度 */
  overflow: hidden;       /* 防止自身溢出 */
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #f8f9fa;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-path {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  color: #333;
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.code-editor-container {
  flex: 1;
  overflow: auto;         /* 出现滚动条 */
  background: #f6f8fa;
  min-height: 0;          /* 修复 flex 子项滚动问题 */
}

.code-content {
  margin: 16px;
  padding: 16px 24px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
  color: #24292e;
  background: #fff;
  border: 1px solid #e1e4e8;
  border-radius: 4px;
  white-space: pre;     
  overflow-x: auto;       
  word-break: normal;
}

.no-file-selected {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 16px;
}

.no-file-selected i {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 代码高亮样式（可根据需要调整） */
.language-javascript {
  color: #24292e;
}
.language-javascript .keyword {
  color: #d73a49;
}
.language-javascript .string {
  color: #032f62;
}
.language-javascript .comment {
  color: #6a737d;
}
.language-vue {
  color: #24292e;
}
.language-html {
  color: #24292e;
}
.language-html .tag {
  color: #22863a;
}
.language-html .attribute {
  color: #6f42c1;
}
.language-json {
  color: #24292e;
}
.language-json .key {
  color: #d73a49;
}
.language-json .string {
  color: #032f62;
}
.language-markdown {
  color: #24292e;
}
.language-markdown .header {
  color: #005cc5;
  font-weight: bold;
}
.language-markdown .code {
  background: #f6f8fa;
  padding: 2px 4px;
  border-radius: 3px;
}

/* 代码浏览入口 */
.code-browser-entry {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .code-browser-content {
    grid-template-columns: 1fr;
    height: auto;
  }
  
  .file-tree-panel {
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    max-height: 300px;
  }
  
  .main-content {
    grid-template-columns: 1fr;
  }
}

.contributor-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.contributor-item:hover {
  background-color: #f5f5f5;
}

.contributor-name {
  font-size: 14px;
  color: #333;
}

/* 相关项目 */
.related-projects-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.related-project-item {
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
  cursor: pointer;
}

.related-project-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.related-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.related-desc {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .project-header {
    flex-direction: column;
    gap: 20px;
  }
  
  .project-stats-overview {
    justify-content: space-around;
  }
  
  .author-section {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}

.file-version-panel {
  border-top: 1px solid #f0f0f0;
  padding: 16px 20px;
  background: #fafafa;
}
.version-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.version-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.version-count {
  font-size: 12px;
  color: #909399;
}
.version-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 220px;
  overflow-y: auto;
}
.version-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
}
.version-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}
.version-name {
  font-weight: 600;
  color: #303133;
}
.version-time, .version-sub, .version-empty, .file-input-tip {
  font-size: 12px;
  color: #909399;
}
.dialog-file-name {
  color: #303133;
  font-weight: 500;
}
.native-file-input {
  display: block;
  width: 100%;
}

</style>