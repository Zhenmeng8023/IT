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
            <el-tag 
              v-if="project.license"
              type="info" 
              size="medium"
            >
              {{ project.license }}
            </el-tag>
          </div>
        </div>
        
        <div class="project-stats-overview">
          <div class="stat-item">
            <span class="stat-number">{{ project.starCount || 0 }}</span>
            <span class="stat-label">收藏</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ project.forkCount || 0 }}</span>
            <span class="stat-label">复刻</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ project.watchCount || 0 }}</span>
            <span class="stat-label">关注</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ project.issueCount || 0 }}</span>
            <span class="stat-label">问题</span>
          </div>
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
          <el-button type="text" icon="el-icon-chat-dot-round" @click="showIssues = true">
            反馈问题
          </el-button>
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
              <template #default="{ node, data }">
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
              <template #default="{ node, data }">
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
            <div class="info-item">
              <span class="info-label">许可证：</span>
              <span class="info-value">{{ project.license || 'MIT' }}</span>
            </div>
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

    <!-- 问题反馈对话框 -->
    <el-dialog
      title="反馈问题"
      v-model="showIssues"
      width="700px"
    >
      <issue-report-form 
        :project-id="projectId"
        @success="handleIssueSuccess"
        @cancel="showIssues = false"
      />
    </el-dialog>
  </div>
</template>

<script>
// 预留API接口
import { 
  GetProjectDetail,
  StarProject,
  UnstarProject,
  ForkProject,
  DownloadProject,
  GetProjectContributors,
  GetRelatedProjects,
  GetProjectFileTree,      // 新增：获取文件树
  GetFileContent           // 新增：获取文件内容
} from '@/api/index'
// 导入核心库
import hljs from 'highlight.js/lib/core';
// 导入需要的语言
import javascript from 'highlight.js/lib/languages/javascript';
import xml from 'highlight.js/lib/languages/xml';       // HTML/XML
import json from 'highlight.js/lib/languages/json';
import markdown from 'highlight.js/lib/languages/markdown';
// 如果还需要其他语言，请参考 highlight.js 文档

// 注册语言
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('html', xml);
hljs.registerLanguage('json', json);
hljs.registerLanguage('markdown', markdown);

// 可选：为 Vue 单独注册（Vue 本质是 HTML）
hljs.registerLanguage('vue', xml);

// 导入样式
import 'highlight.js/styles/github.css';


export default {
  layout: 'project',
  data() {
    return {
      projectId: null,
      project: {
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
        readme: ''
      },
      contributors: [],
      relatedProjects: [],
      fileTree: [],
      isStarred: false,
      starLoading: false,
      forkLoading: false,
      showEditDialog: false,
      showIssues: false,
      treeProps: {
        children: 'children',
        label: 'name'
      },

      // 代码浏览相关
      showCodeBrowser: false,       // 控制代码浏览区域显示
      codeFileTree: [],             // 文件树数据
      currentFile: {               // 当前选中文件
        path: '',
        name: '',
        type: '',
        language: '',
        size: null,
        content: ''
      },
      treeFilterText: '',           // 文件树搜索关键字
      codeTreeProps: {              // el-tree 配置
        children: 'children',
        label: 'name'
      }
    }
  },
  computed: {
    renderedReadme() {
      // 简单的Markdown渲染逻辑，实际可以使用marked.js等库
      return this.project.readme ? this.project.readme.replace(/\n/g, '<br>') : '暂无README文档'
    },
    
    // 当前文件路径显示
    currentFilePath() {
      return this.currentFile.path || '未选择文件'
    }
  },

  watch: {
  treeFilterText(val) {
    this.$refs.fileTreeRef?.filter(val)
  },
  // 监听当前文件内容变化，自动高亮
  'currentFile.content': {
    handler() {
      this.$nextTick(() => {
        this.highlightCode()
      })
    },
    immediate: true
  }
  },
  async mounted() {
    this.projectId = this.$route.params.id
    await this.fetchProjectDetail()
    await this.fetchContributors()
    await this.fetchRelatedProjects()
    this.generateFileTree()
  },
  methods: {
    // 获取项目详情
    async fetchProjectDetail() {
      try {
        // 预留API调用
        // const response = await GetProjectDetail(this.projectId)
        // this.project = response.data
        
        // 模拟数据
        this.project = {
          id: this.projectId,
          title: '博客管理系统',
          type: 'Web应用',
          status: '已完成',
          description: '一个基于Vue.js和Node.js的现代化博客管理系统，支持多用户、标签管理、评论系统等功能。',
          technologies: ['Vue.js', 'Node.js', 'MongoDB', 'Express', 'Element UI'],
          features: [
            '多用户权限管理',
            '富文本编辑器',
            '标签分类系统',
            '评论互动功能',
            '数据统计分析'
          ],
          author: { 
            id: 1, 
            nickname: '开发者A', 
            avatar: '' 
          },
          starCount: 45,
          forkCount: 12,
          watchCount: 89,
          issueCount: 3,
          createTime: '2024-01-15T10:00:00Z',
          updateTime: '2024-03-20T14:30:00Z',
          license: 'MIT',
          version: 'v1.2.0',
          language: 'JavaScript',
          size: '2.5 MB',
          readme: '# 博客管理系统\n\n一个现代化的博客管理系统，基于Vue.js和Node.js构建。\n\n## 功能特性\n\n- 多用户权限管理\n- 富文本编辑器\n- 标签分类系统\n- 评论互动功能\n- 数据统计分析\n\n## 技术栈\n\n- 前端：Vue.js + Element UI\n- 后端：Node.js + Express\n- 数据库：MongoDB\n- 部署：Docker'
        }
      } catch (error) {
        this.$message.error('获取项目详情失败')
      }
    },

    // 获取贡献者列表
    async fetchContributors() {
      try {
        // 预留API调用
        // const response = await GetProjectContributors(this.projectId)
        // this.contributors = response.data
        
        // 模拟数据
        this.contributors = [
          { id: 1, name: '开发者A', avatar: '' },
          { id: 2, name: '贡献者B', avatar: '' },
          { id: 3, name: '贡献者C', avatar: '' }
        ]
      } catch (error) {
        console.error('获取贡献者失败')
      }
    },

    // 获取相关项目
    async fetchRelatedProjects() {
      try {
        // 预留API调用
        // const response = await GetRelatedProjects(this.projectId)
        // this.relatedProjects = response.data
        
        // 模拟数据
        this.relatedProjects = [
          {
            id: 2,
            title: '内容管理系统',
            description: '基于React的内容管理系统'
          },
          {
            id: 3,
            title: '在线文档编辑器',
            description: '支持多人协作的在线文档编辑器'
          }
        ]
      } catch (error) {
        console.error('获取相关项目失败')
      }
    },

    // 生成文件树结构（模拟数据，实际应替换为API请求）
    generateFileTree() {
      this.fileTree = [
        {
          name: 'src',
          children: [
            { name: 'components', type: 'folder' },
            { name: 'views', type: 'folder' },
            { name: 'utils', type: 'folder' },
            { name: 'main.js', type: 'file' }
          ]
        },
        {
          name: 'public',
          children: [
            { name: 'index.html', type: 'file' }
          ]
        },
        { name: 'package.json', type: 'file' },
        { name: 'README.md', type: 'file' }
      ]
    },

    // ==================== 文件操作相关方法 ====================
    // 在 methods 中添加以下两个方法

/**
 * 设为主文件
 * @param {Object} fileData - 文件节点数据
 */
async setAsMainFile(fileData) {
  if (fileData.type !== 'file') return; // 只允许文件设为主文件

  try {
    // TODO: 调用后端接口，将当前文件设为主文件
    // await SetMainFile(this.projectId, fileData.path);
    
    // 模拟成功响应
    this.$message.success(`已将 ${fileData.name} 设为主文件`);

    // 更新前端文件树中的主文件标记
    const updateMainFlag = (nodes) => {
      for (const node of nodes) {
        if (node.type === 'file') {
          node.isMainFile = (node.path === fileData.path);
        }
        if (node.children && node.children.length) {
          updateMainFlag(node.children);
        }
      }
    };
    updateMainFlag(this.codeFileTree);
    
    // 如果当前编辑器显示的就是该文件，同步标记
    if (this.currentFile.path === fileData.path) {
      this.currentFile.isMainFile = true;
    }
    
  } catch (error) {
    this.$message.error('设置主文件失败');
  }
},

/**
 * 删除文件
 * @param {Object} fileData - 文件节点数据
 */
async deleteFile(fileData) {
  if (fileData.type !== 'file') return; // 只允许删除文件

  try {
    // 二次确认
    await this.$confirm(`确定要删除文件 "${fileData.name}" 吗？此操作不可撤销。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    // TODO: 调用后端接口删除文件
    // await DeleteFile(this.projectId, fileData.path);

    // 模拟成功
    this.$message.success(`文件 ${fileData.name} 已删除`);

    // 从文件树中移除该节点
    const removeNode = (nodes, targetPath) => {
      for (let i = 0; i < nodes.length; i++) {
        const node = nodes[i];
        if (node.path === targetPath) {
          nodes.splice(i, 1);
          return true;
        }
        if (node.children && removeNode(node.children, targetPath)) {
          return true;
        }
      }
      return false;
    };
    removeNode(this.codeFileTree, fileData.path);

    // 如果删除的是当前正在浏览的文件，清空编辑器内容
    if (this.currentFile.path === fileData.path) {
      this.currentFile = { content: '' };
    }

    // 刷新文件树（确保 Vue 响应式更新）
    this.codeFileTree = [...this.codeFileTree];
    
  } catch (error) {
    if (error !== 'cancel') {
      this.$message.error('删除失败');
    }
  }
},


    // ==================== 代码浏览相关方法 ====================
    // 打开代码浏览区域
    openCodeBrowser() {
      this.showCodeBrowser = true
      if (this.codeFileTree.length === 0) {
        this.fetchFileTree()
      }
    },

    // 关闭代码浏览区域
    closeCodeBrowser() {
      this.showCodeBrowser = false
    },

    // 获取文件树（调用API）
    async fetchFileTree() {
      try {
        // 模拟数据（实际使用时替换为API调用）
        // const res = await GetProjectFileTree(this.projectId)
        // this.codeFileTree = res.data
        this.codeFileTree = [
          {
            name: 'src',
            path: 'src',
            type: 'folder',
            children: [
              { name: 'components', path: 'src/components', type: 'folder', children: [] },
              { name: 'views', path: 'src/views', type: 'folder', children: [] },
              { name: 'utils', path: 'src/utils', type: 'folder', children: [] },
              { name: 'main.js', path: 'src/main.js', type: 'file', size: 2048, language: 'javascript' }
            ]
          },
          {
            name: 'public',
            path: 'public',
            type: 'folder',
            children: [
              { name: 'index.html', path: 'public/index.html', type: 'file', size: 512, language: 'html' }
            ]
          },
          { name: 'package.json', path: 'package.json', type: 'file', size: 1024, language: 'json' },
          { name: 'README.md', path: 'README.md', type: 'file', size: 256, language: 'markdown' }
        ]
      } catch (error) {
        this.$message.error('获取文件结构失败')
      }
    },

    // 刷新代码浏览器（重新加载文件树，清空当前文件）
    async refreshCodeBrowser() {
      this.codeFileTree = []
      this.currentFile = { content: '' }
      this.treeFilterText = ''
      await this.fetchFileTree()
      this.$message.success('刷新成功')
    },

    // 点击文件节点
    async handleFileClick(data) {
      if (data.type !== 'file') return

      // 如果已有内容，直接展示
      if (data.content) {
        this.currentFile = data
        return
      }

      // 显示加载状态（可选，这里简单处理）
      const loading = this.$loading({
        target: '.code-editor-container',
        text: '加载文件中...'
      })

      try {
        // 调用API获取文件内容
        // const res = await GetFileContent(this.projectId, data.path)
        // data.content = res.data.content

        // 模拟内容
        data.content = `// 文件：${data.name}\n
        const codeBlock = document.querySelector('.code-content code');
        if (codeBlock) {
        hljs.highlightElement(codeBlock);
        }
        这里是模拟的代码内容\nconsole.log('Hello World');`
        

        this.currentFile = data
      } catch (error) {
        this.$message.error('文件内容加载失败')
        this.currentFile = {
          ...data,
          content: '加载失败，请稍后重试'
        }
      } finally {
        loading.close()
      }
    },

    // 高亮代码
    highlightCode() {
    this.$nextTick(() => {
      const blocks = document.querySelectorAll('.code-content code');
      blocks.forEach(block => {
        hljs.highlightElement(block);   // 新版 API
      });
    });
  },

    // 复制文件内容
    copyFileContent() {
      if (!this.currentFile.content) {
        this.$message.warning('没有可复制的内容')
        return
      }
      navigator.clipboard.writeText(this.currentFile.content)
        .then(() => this.$message.success('复制成功'))
        .catch(() => this.$message.error('复制失败'))
    },

    // 下载当前文件
    downloadFile() {
      if (!this.currentFile.content) {
        this.$message.warning('没有可下载的内容')
        return
      }
      const blob = new Blob([this.currentFile.content], { type: 'text/plain' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = this.currentFile.name
      link.click()
      URL.revokeObjectURL(link.href)
      this.$message.success('下载开始')
    },

    // 文件树过滤方法
    filterNode(value, data) {
      if (!value) return true
      return data.name.toLowerCase().includes(value.toLowerCase())
    },

    // 根据文件类型返回图标类名
    getFileIconClass(data) {
      if (data.type === 'folder') return 'el-icon-folder'
      const ext = data.name.split('.').pop()
      const iconMap = {
        js: 'el-icon-document',
        vue: 'el-icon-document',
        json: 'el-icon-document',
        md: 'el-icon-document',
        html: 'el-icon-document',
        css: 'el-icon-document'
      }
      return iconMap[ext] || 'el-icon-document'
    },

    // 格式化文件大小
    formatFileSize(bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },

    // ==================== 其他原有方法 ====================
    // 收藏项目
    async handleStar() {
      this.starLoading = true
      try {
        if (this.isStarred) {
          // await UnstarProject(this.projectId)
          this.project.starCount--
        } else {
          // await StarProject(this.projectId)
          this.project.starCount++
        }
        this.isStarred = !this.isStarred
        this.$message.success(this.isStarred ? '收藏成功' : '取消收藏成功')
      } catch (error) {
        this.$message.error('操作失败')
      } finally {
        this.starLoading = false
      }
    },

    // 复刻项目
    async handleFork() {
      this.forkLoading = true
      try {
        // await ForkProject(this.projectId)
        this.project.forkCount++
        this.$message.success('项目复刻成功')
      } catch (error) {
        this.$message.error('复刻失败')
      } finally {
        this.forkLoading = false
      }
    },

    // 下载项目
    async handleDownload() {
      try {
        // await DownloadProject(this.projectId)
        this.$message.success('开始下载项目')
      } catch (error) {
        this.$message.error('下载失败')
      }
    },

    // 跳转到项目管理页面
    goToProjectManage() {
      this.$router.push('/projectmanage')
    },

    // 获取文件图标（用于右侧文件结构卡片）
    getFileIcon(data) {
      if (data.children) {
        return 'el-icon-folder'
      }
      const ext = data.name.split('.').pop()
      const iconMap = {
        js: 'el-icon-document',
        vue: 'el-icon-document',
        json: 'el-icon-document',
        md: 'el-icon-document',
        html: 'el-icon-document'
      }
      return iconMap[ext] || 'el-icon-document'
    },

    // 工具方法 - 与列表页面保持一致
    getProjectTypeTag(type) {
      const typeMap = {
        'Web应用': 'primary',
        '移动应用': 'success',
        '桌面应用': 'warning',
        '工具库': 'info'
      }
      return typeMap[type] || 'info'
    },

    getStatusTag(status) {
      const statusMap = {
        '已完成': 'success',
        '开发中': 'warning',
        '维护中': 'info',
        '已归档': 'danger'
      }
      return statusMap[status] || 'info'
    },

    formatTime(time) {
      if (!time) return ''
      return new Date(time).toLocaleDateString('zh-CN')
    },

    filterByTech(tech) {
      this.$router.push({
        path: '/f_project/list',
        query: { tech }
      })
    },

    goToDetail(id) {
      this.$router.push(`/projectdetail/${id}`)
    },

    handleEditSuccess() {
      this.showEditDialog = false
      this.fetchProjectDetail()
      this.$message.success('项目信息更新成功')
    },

    handleIssueSuccess() {
      this.showIssues = false
      this.$message.success('问题反馈成功')
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