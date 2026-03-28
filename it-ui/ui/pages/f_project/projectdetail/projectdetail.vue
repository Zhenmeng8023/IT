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

        <el-button
          size="small"
          icon="el-icon-share"
          @click="handleFork"
          :loading="forkLoading"
        >
          复刻 ({{ project.forkCount || 0 }})
        </el-button>

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
              v-for="tech in project.technologies"
              :key="tech"
              type="success"
              size="medium"
              class="tech-tag"
              @click="filterByTech(tech)"
            >
              {{ tech }}
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

    <!-- 编辑项目信息对话框 -->
    <el-dialog
      title="编辑项目信息"
      v-model="showEditDialog"
      width="600px"
    >
      <project-edit-form 
        :project="project"
        @success="handleEditSuccess"
        @cancel="showEditDialog = false"
      />
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
  listProjectFiles,
  setMainFile as apiSetMainFile,
  deleteFile as apiDeleteFile,
  downloadFile as apiDownloadFile,
  uploadProjectFile,
  pageProjects
} from '@/api/project'
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
    return tags
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  }
  return []
}

function formatDate(dateLike) {
  if (!dateLike) return ''
  const date = new Date(dateLike)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('zh-CN')
}

function formatDateTimeToBackend(dateLike) {
  if (!dateLike) return undefined
  const date = new Date(dateLike)
  if (Number.isNaN(date.getTime())) return undefined
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  link.click()
  URL.revokeObjectURL(url)
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
      starLoading: false,
      forkLoading: false,
      showEditDialog: false,
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
      currentFile: {
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
      },
      treeFilterText: '',
      codeTreeProps: {
        children: 'children',
        label: 'name'
      },
      uploadLoading: false
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
      this.$refs.fileTreeRef?.filter(val)
    },
    'currentFile.content': {
      handler() {
        if (!process.client) {
          return
        }
        this.$nextTick(() => {
          this.highlightCode()
        })
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

    async fetchProjectDetail() {
      try {
        const response = await getProjectDetail(this.projectId)
        const apiData = response.data || {}
        this.rawProject = apiData
        const technologies = parseTags(apiData.tags)
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
          version: '',
          language: '',
          size: apiData.sizeMb != null ? `${apiData.sizeMb} MB` : '',
          readme: '',
          visibility: apiData.visibility || '',
          members: apiData.members || [],
          tasks: apiData.tasks || [],
          files: apiData.files || []
        }
        this.contributors = this.buildContributors(apiData)
      } catch (error) {
        console.error('获取项目详情失败:', error)
        this.$message.error(error.response?.data?.message || '获取项目详情失败')
      }
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
      if (!this.rawProject?.category) {
        this.relatedProjects = []
        return
      }
      try {
        const response = await pageProjects({
          page: 1,
          size: 6,
          category: this.rawProject.category,
          sortBy: 'latest'
        })
        const list = (response.data?.list || [])
          .filter(item => Number(item.id) !== Number(this.projectId))
          .slice(0, 4)
          .map(item => ({
            id: item.id,
            title: item.name,
            description: item.description || '暂无项目描述'
          }))
        this.relatedProjects = list
      } catch (error) {
        console.error('获取相关项目失败:', error)
        this.relatedProjects = []
      }
    },

    async fetchFileTree() {
      if (!this.projectId) return
      try {
        const response = await listProjectFiles(this.projectId)
        const files = response.data || []
        const tree = this.transformFilesToTree(files)
        this.codeFileTree = tree
        this.fileTree = tree
        this.project.files = files
        await this.loadReadmeIfExists(files)
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
      if (this.$store && this.$store.state) {
        const s = this.$store.state
        const candidates = [
          s.user && s.user.id,
          s.user && s.user.userId,
          s.login && s.login.userInfo && s.login.userInfo.id,
          s.login && s.login.userInfo && s.login.userInfo.userId
        ].filter(Boolean)
        if (candidates.length > 0) return Number(candidates[0])
      }
      if (process.client) {
        try {
          const raw = localStorage.getItem('userInfo') || localStorage.getItem('user')
          if (raw) {
            const parsed = JSON.parse(raw)
            const id = parsed && (parsed.id || parsed.userId)
            if (id) return Number(id)
          }
        } catch (e) {}
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
        if (this.currentFile.id === fileData.id) {
          this.currentFile.isMainFile = true
        }
      } catch (error) {
        this.$message.error(error.response?.data?.message || '设置主文件失败')
      }
    },

    async deleteFile(fileData) {
      if (fileData.type !== 'file') return
      try {
        await this.$confirm(`确定要删除文件 “${fileData.name}” 吗？此操作不可撤销。`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await apiDeleteFile(fileData.id)
        this.$message.success('文件删除成功')
        if (this.currentFile.id === fileData.id) {
          this.currentFile = {
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
        await this.fetchFileTree()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }
    },

    openCodeBrowser() {
      this.showCodeBrowser = true
      if (this.codeFileTree.length === 0) {
        this.fetchFileTree()
      }
    },

    closeCodeBrowser() {
      this.showCodeBrowser = false
    },

    async refreshCodeBrowser() {
      this.codeFileTree = []
      this.fileTree = []
      this.currentFile = {
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
      this.treeFilterText = ''
      await this.fetchFileTree()
      this.$message.success('刷新成功')
    },

    isPreviewableTextFile(fileNode) {
      if (!fileNode) return false
      const ext = (fileNode.extension || fileNode.language || '').toLowerCase()
      return TEXT_FILE_EXTENSIONS.has(ext)
    },

    async handleFileClick(data) {
      if (data.type !== 'file') return
      const loading = this.$loading({
        target: '.code-editor-container',
        text: '加载文件中...'
      })
      try {
        const blob = await apiDownloadFile(data.id)
        const isTextFile = this.isPreviewableTextFile(data)
        let content = ''
        let isBinary = false
        if (isTextFile) {
          content = await blob.text()
        } else {
          content = '该文件为二进制文件，暂不支持在线预览，请使用下载功能查看。'
          isBinary = true
        }
        this.currentFile = {
          ...data,
          content,
          isBinary
        }
      } catch (error) {
        console.error('文件内容加载失败:', error)
        this.$message.error(error.response?.data?.message || '文件内容加载失败')
        this.currentFile = {
          ...data,
          content: '加载失败，请稍后重试。',
          isBinary: false
        }
      } finally {
        loading.close()
      }
    },

    highlightCode() {
      if (!process.client || typeof document === 'undefined') {
        return
      }
      this.$nextTick(() => {
        if (typeof document === 'undefined') {
          return
        }
        const blocks = document.querySelectorAll('.code-content code')
        blocks.forEach(block => {
          hljs.highlightElement(block)
        })
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
      if (!process.client || typeof document === 'undefined') {
        return
      }
      const fileInput = document.createElement('input')
      fileInput.type = 'file'
      fileInput.accept = '*'
      fileInput.style.display = 'none'
      fileInput.addEventListener('change', async (event) => {
        const file = event.target.files[0]
        if (file) {
          await this.uploadFile(file)
        }
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
        await this.fetchFileTree()
      } catch (error) {
        console.error('文件上传失败:', error)
        this.$message.error(error.response?.data?.message || '文件上传失败')
      } finally {
        this.uploadLoading = false
      }
    },

    filterNode(value, data) {
      if (!value) return true
      return data.name.toLowerCase().includes(value.toLowerCase())
    },

    getFileIconClass(data) {
      if (data.type === 'folder') return 'el-icon-folder'
      const ext = (data.extension || data.language || (data.name || '').split('.').pop() || '').toLowerCase()
      const iconMap = {
        js: 'el-icon-document',
        vue: 'el-icon-document',
        json: 'el-icon-document',
        md: 'el-icon-document',
        html: 'el-icon-document',
        css: 'el-icon-document',
        java: 'el-icon-document'
      }
      return iconMap[ext] || 'el-icon-document'
    },

    formatFileSize(bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
      const typeMap = {
        '前端项目': 'primary',
        '后端项目': 'success',
        '全栈项目': 'warning',
        '移动应用': 'success',
        'AI 项目': 'danger',
        '工具项目': 'info'
      }
      return typeMap[type] || 'info'
    },

    getStatusTag(status) {
      return STATUS_TAG_MAP[status] || 'info'
    },

    formatTime(time) {
      return formatDate(time)
    },

    filterByTech(tech) {
      this.$router.push({
        path: '/projectlist',
        query: { tech }
      })
    },

    goToDetail(id) {
      this.$router.push(`/projectdetail?projectId=${id}`)
    },

    handleEditSuccess() {
      this.showEditDialog = false
      this.fetchProjectDetail()
      this.$message.success('项目信息更新成功')
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
</style>