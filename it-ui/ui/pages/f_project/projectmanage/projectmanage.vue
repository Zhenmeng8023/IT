<template>
  <div class="project-manage-container">
    <!-- 页面头部 -->
    <div class="manage-header">
      <div class="header-content">
        <div class="project-info">
          <h1 class="project-title">
            <i class="el-icon-s-management"></i>
            {{ project.title || '项目管理' }}
          </h1>
          <p class="project-description">{{ project.description || '项目描述' }}</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" size="small" icon="el-icon-plus" @click="showCreateDialog = true">
            新建
          </el-button>
          <el-button size="small" icon="el-icon-setting" @click="showSettings = true">
            设置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 导航标签 -->
    <div class="manage-nav">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="概览" name="overview">
          <i class="el-icon-data-line" slot="label"></i>
        </el-tab-pane>
        <el-tab-pane label="Issues" name="issues">
          <i class="el-icon-warning-outline" slot="label"></i>
          <span class="tab-badge" v-if="issueStats.total > 0">{{ issueStats.total }}</span>
        </el-tab-pane>
        <el-tab-pane label="Pull Requests" name="pull-requests">
          <i class="el-icon-share" slot="label"></i>
          <span class="tab-badge" v-if="prStats.total > 0">{{ prStats.total }}</span>
        </el-tab-pane>
        <el-tab-pane label="Projects" name="projects">
          <i class="el-icon-s-fold" slot="label"></i>
        </el-tab-pane>
        <el-tab-pane label="Actions" name="actions">
          <i class="el-icon-c-scale-to-original" slot="label"></i>
        </el-tab-pane>
        <el-tab-pane label="Wiki" name="wiki">
          <i class="el-icon-notebook-2" slot="label"></i>
        </el-tab-pane>
        <el-tab-pane label="Security" name="security">
          <i class="el-icon-lock" slot="label"></i>
        </el-tab-pane>
        <el-tab-pane label="Insights" name="insights">
          <i class="el-icon-data-analysis" slot="label"></i>
        </el-tab-pane>
        <el-tab-pane label="Settings" name="settings">
          <i class="el-icon-setting" slot="label"></i>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 主要内容区域 -->
    <div class="manage-content">
      <!-- 概览页面 -->
      <div v-if="activeTab === 'overview'" class="overview-page">
        <div class="overview-stats">
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="stat-card" @click="activeTab = 'issues'">
                <div class="stat-icon issue-icon">
                  <i class="el-icon-warning-outline"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ issueStats.total }}</div>
                  <div class="stat-label">Issues</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card" @click="activeTab = 'pull-requests'">
                <div class="stat-icon pr-icon">
                  <i class="el-icon-share"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ prStats.total }}</div>
                  <div class="stat-label">Pull Requests</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-icon star-icon">
                  <i class="el-icon-star-off"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ project.starCount || 0 }}</div>
                  <div class="stat-label">Stars</div>
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-icon fork-icon">
                  <i class="el-icon-share"></i>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ project.forkCount || 0 }}</div>
                  <div class="stat-label">Forks</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="overview-main">
          <el-row :gutter="20">
            <el-col :span="16">
              <!-- 最近活动 -->
              <el-card class="recent-activity">
                <template #header>
                  <div class="card-header">
                    <span>最近活动</span>
                  </div>
                </template>
                <div class="activity-list">
                  <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
                    <div class="activity-avatar">
                      <el-avatar :size="32" :src="activity.user.avatar"></el-avatar>
                    </div>
                    <div class="activity-content">
                      <div class="activity-text">
                        <span class="user-name">{{ activity.user.name }}</span>
                        <span class="action">{{ activity.action }}</span>
                        <span class="target">{{ activity.target }}</span>
                      </div>
                      <div class="activity-time">{{ formatTime(activity.time) }}</div>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <!-- 项目信息 -->
              <el-card class="project-info-card">
                <template #header>
                  <div class="card-header">
                    <span>项目信息</span>
                  </div>
                </template>
                <div class="info-list">
                  <div class="info-item">
                    <span class="info-label">语言：</span>
                    <span class="info-value">{{ project.language || 'JavaScript' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">许可证：</span>
                    <span class="info-value">{{ project.license || 'MIT' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">最后更新：</span>
                    <span class="info-value">{{ formatTime(project.updateTime) }}</span>
                  </div>
                </div>
              </el-card>

              <!-- 贡献者 -->
              <el-card class="contributors-card">
                <template #header>
                  <div class="card-header">
                    <span>贡献者</span>
                  </div>
                </template>
                <div class="contributors-list">
                  <div v-for="contributor in contributors" :key="contributor.id" class="contributor-item">
                    <el-avatar :size="32" :src="contributor.avatar"></el-avatar>
                    <span class="contributor-name">{{ contributor.name }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>

      <!-- Issues页面 -->
      <div v-if="activeTab === 'issues'" class="issues-page">
        <div class="page-header">
          <div class="filter-bar">
            <el-input
              v-model="issueFilter.keyword"
              placeholder="搜索Issues..."
              size="small"
              class="search-input"
              @keyup.enter="searchIssues"
            >
              <template #append>
                <el-button icon="el-icon-search" @click="searchIssues"></el-button>
              </template>
            </el-input>
            
            <el-select v-model="issueFilter.state" size="small" @change="filterIssues">
              <el-option label="全部" value="all"></el-option>
              <el-option label="开启" value="open"></el-option>
              <el-option label="关闭" value="closed"></el-option>
            </el-select>

            <el-button type="primary" size="small" icon="el-icon-plus" @click="createIssue">
              新建Issue
            </el-button>
          </div>
        </div>

        <div class="issues-list">
          <div v-for="issue in filteredIssues" :key="issue.id" class="issue-item">
            <div class="issue-icon">
              <i :class="issue.state === 'open' ? 'el-icon-warning-outline open' : 'el-icon-circle-check closed'"></i>
            </div>
            <div class="issue-content">
              <div class="issue-title">
                <span class="title-text" @click="viewIssue(issue.id)">{{ issue.title }}</span>
                <div class="issue-labels">
                  <el-tag
                    v-for="label in issue.labels"
                    :key="label"
                    :type="getLabelType(label)"
                    size="small"
                  >
                    {{ label }}
                  </el-tag>
                </div>
              </div>
              <div class="issue-meta">
                <span>#{{ issue.number }}</span>
                <span>由 {{ issue.author.name }} 创建于 {{ formatTime(issue.createTime) }}</span>
                <span v-if="issue.comments > 0">{{ issue.comments }} 条评论</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Pull Requests页面 -->
      <div v-if="activeTab === 'pull-requests'" class="pr-page">
        <div class="page-header">
          <div class="filter-bar">
            <el-input
              v-model="prFilter.keyword"
              placeholder="搜索Pull Requests..."
              size="small"
              class="search-input"
            >
              <template #append>
                <el-button icon="el-icon-search"></el-button>
              </template>
            </el-input>
            
            <el-select v-model="prFilter.state" size="small">
              <el-option label="全部" value="all"></el-option>
              <el-option label="开启" value="open"></el-option>
              <el-option label="关闭" value="closed"></el-option>
              <el-option label="合并" value="merged"></el-option>
            </el-select>

            <el-button type="primary" size="small" icon="el-icon-plus" @click="createPR">
              新建PR
            </el-button>
          </div>
        </div>

        <div class="pr-list">
          <div v-for="pr in pullRequests" :key="pr.id" class="pr-item">
            <div class="pr-icon">
              <i :class="getPRIcon(pr.state)"></i>
            </div>
            <div class="pr-content">
              <div class="pr-title">
                <span class="title-text" @click="viewPR(pr.id)">{{ pr.title }}</span>
                <div class="pr-labels">
                  <el-tag
                    v-for="label in pr.labels"
                    :key="label"
                    type="success"
                    size="small"
                  >
                    {{ label }}
                  </el-tag>
                </div>
              </div>
              <div class="pr-meta">
                <span>#{{ pr.number }}</span>
                <span>由 {{ pr.author.name }} 创建于 {{ formatTime(pr.createTime) }}</span>
                <span>{{ pr.commits }} 次提交</span>
                <span>{{ pr.changedFiles }} 个文件变更</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 其他页面占位 -->
      <div v-else class="placeholder-page">
        <div class="placeholder-content">
          <i class="el-icon-s-management placeholder-icon"></i>
          <h3>{{ getTabTitle(activeTab) }} 页面开发中</h3>
          <p>该功能正在开发中，敬请期待...</p>
        </div>
      </div>
    </div>

    <!-- 新建Issue对话框 -->
    <el-dialog
      title="新建Issue"
      v-model="showCreateIssueDialog"
      width="600px"
    >
      <issue-create-form
        @success="handleIssueCreateSuccess"
        @cancel="showCreateIssueDialog = false"
      />
    </el-dialog>

    <!-- 设置对话框 -->
    <el-dialog
      title="项目设置"
      v-model="showSettings"
      width="800px"
    >
      <project-settings-form
        :project="project"
        @success="handleSettingsSuccess"
        @cancel="showSettings = false"
      />
    </el-dialog>
  </div>
</template>

<script>
// 预留API接口
import { 
  GetProjectDetail,
  GetProjectIssues,
  GetProjectPullRequests,
  GetProjectActivities,
  GetProjectContributors,
  CreateIssue,
  UpdateIssue,
  CreatePullRequest,
  UpdateProjectSettings
} from '@/api/index'

export default {
  layout: 'project',
  data() {
    return {
      activeTab: 'overview',
      project: {},
      
      // Issues相关
      issues: [],
      issueFilter: {
        keyword: '',
        state: 'all'
      },
      issueStats: {
        total: 0,
        open: 0,
        closed: 0
      },
      
      // Pull Requests相关
      pullRequests: [],
      prFilter: {
        keyword: '',
        state: 'all'
      },
      prStats: {
        total: 0,
        open: 0,
        closed: 0,
        merged: 0
      },
      
      // 活动记录
      recentActivities: [],
      contributors: [],
      
      // 对话框控制
      showCreateIssueDialog: false,
      showCreatePRDialog: false,
      showSettings: false
    }
  },
  computed: {
    filteredIssues() {
      return this.issues.filter(issue => {
        const matchesKeyword = !this.issueFilter.keyword || 
          issue.title.toLowerCase().includes(this.issueFilter.keyword.toLowerCase()) ||
          issue.description.toLowerCase().includes(this.issueFilter.keyword.toLowerCase())
        
        const matchesState = this.issueFilter.state === 'all' || issue.state === this.issueFilter.state
        
        return matchesKeyword && matchesState
      })
    }
  },
  async mounted() {
    await this.loadProjectData()
    await this.loadIssues()
    await this.loadPullRequests()
    await this.loadActivities()
    await this.loadContributors()
  },
  methods: {
    // 加载项目数据
    async loadProjectData() {
      try {
        // 预留API调用
        // const response = await GetProjectDetail(this.$route.query.projectId)
        // this.project = response.data
        
        // 模拟数据
        this.project = {
          id: 1,
          title: '博客管理系统',
          description: '一个基于Vue.js和Node.js的现代化博客管理系统',
          language: 'JavaScript',
          license: 'MIT',
          starCount: 45,
          forkCount: 12,
          updateTime: '2024-03-25T10:00:00Z'
        }
      } catch (error) {
        this.$message.error('加载项目数据失败')
      }
    },

    // 加载Issues
    async loadIssues() {
      try {
        // 预留API调用
        // const response = await GetProjectIssues(this.project.id)
        // this.issues = response.data
        
        // 模拟数据
        this.issues = [
          {
            id: 1,
            number: 1,
            title: '修复登录页面样式问题',
            description: '登录页面的按钮样式需要调整',
            state: 'open',
            author: { name: '开发者A', avatar: '' },
            labels: ['bug', 'frontend'],
            comments: 3,
            createTime: '2024-03-20T14:30:00Z'
          },
          {
            id: 2,
            number: 2,
            title: '添加用户权限管理功能',
            description: '需要实现基于角色的权限控制系统',
            state: 'open',
            author: { name: '开发者B', avatar: '' },
            labels: ['enhancement', 'backend'],
            comments: 5,
            createTime: '2024-03-22T09:15:00Z'
          },
          {
            id: 3,
            number: 3,
            title: '优化数据库查询性能',
            description: '某些查询语句需要优化以提高性能',
            state: 'closed',
            author: { name: '开发者C', avatar: '' },
            labels: ['performance', 'database'],
            comments: 2,
            createTime: '2024-03-18T16:45:00Z'
          }
        ]
        
        this.updateIssueStats()
      } catch (error) {
        this.$message.error('加载Issues失败')
      }
    },

    // 加载Pull Requests
    async loadPullRequests() {
      try {
        // 预留API调用
        // const response = await GetProjectPullRequests(this.project.id)
        // this.pullRequests = response.data
        
        // 模拟数据
        this.pullRequests = [
          {
            id: 1,
            number: 1,
            title: '重构用户认证模块',
            state: 'open',
            author: { name: '开发者A', avatar: '' },
            labels: ['refactor', 'auth'],
            commits: 5,
            changedFiles: 12,
            createTime: '2024-03-24T11:20:00Z'
          },
          {
            id: 2,
            number: 2,
            title: '添加单元测试',
            state: 'merged',
            author: { name: '开发者B', avatar: '' },
            labels: ['test', 'quality'],
            commits: 8,
            changedFiles: 15,
            createTime: '2024-03-23T15:30:00Z'
          }
        ]
        
        this.updatePRStats()
      } catch (error) {
        this.$message.error('加载Pull Requests失败')
      }
    },

    // 加载活动记录
    async loadActivities() {
      try {
        // 预留API调用
        // const response = await GetProjectActivities(this.project.id)
        // this.recentActivities = response.data
        
        // 模拟数据
        this.recentActivities = [
          {
            id: 1,
            user: { name: '开发者A', avatar: '' },
            action: '创建了',
            target: 'Issue #1',
            time: '2024-03-25T09:00:00Z'
          },
          {
            id: 2,
            user: { name: '开发者B', avatar: '' },
            action: '评论了',
            target: 'Pull Request #2',
            time: '2024-03-25T08:30:00Z'
          },
          {
            id: 3,
            user: { name: '开发者C', avatar: '' },
            action: '合并了',
            target: 'Pull Request #2',
            time: '2024-03-25T08:15:00Z'
          }
        ]
      } catch (error) {
        console.error('加载活动记录失败')
      }
    },

    // 加载贡献者
    async loadContributors() {
      try {
        // 预留API调用
        // const response = await GetProjectContributors(this.project.id)
        // this.contributors = response.data
        
        // 模拟数据
        this.contributors = [
          { id: 1, name: '开发者A', avatar: '' },
          { id: 2, name: '开发者B', avatar: '' },
          { id: 3, name: '开发者C', avatar: '' }
        ]
      } catch (error) {
        console.error('加载贡献者失败')
      }
    },

    // 更新Issue统计
    updateIssueStats() {
      this.issueStats.total = this.issues.length
      this.issueStats.open = this.issues.filter(i => i.state === 'open').length
      this.issueStats.closed = this.issues.filter(i => i.state === 'closed').length
    },

    // 更新PR统计
    updatePRStats() {
      this.prStats.total = this.pullRequests.length
      this.prStats.open = this.pullRequests.filter(pr => pr.state === 'open').length
      this.prStats.closed = this.pullRequests.filter(pr => pr.state === 'closed').length
      this.prStats.merged = this.pullRequests.filter(pr => pr.state === 'merged').length
    },

    // 标签页切换
    handleTabChange(tab) {
      this.activeTab = tab.name
    },

    // 搜索Issues
    searchIssues() {
      // 搜索逻辑已在computed中实现
    },

    // 过滤Issues
    filterIssues() {
      // 过滤逻辑已在computed中实现
    },

    // 创建Issue
    createIssue() {
      this.showCreateIssueDialog = true
    },

    // 创建PR
    createPR() {
      this.showCreatePRDialog = true
    },

    // 查看Issue详情
    viewIssue(issueId) {
      this.$router.push(`/f_project/issue?issueId=${issueId}`)
    },

    // 查看PR详情
    viewPR(prId) {
      this.$router.push(`/f_project/pull-request?prId=${prId}`)
    },

    // 获取标签类型
    getLabelType(label) {
      const typeMap = {
        'bug': 'danger',
        'enhancement': 'success',
        'feature': 'primary',
        'documentation': 'info',
        'performance': 'warning'
      }
      return typeMap[label] || 'info'
    },

    // 获取PR图标
    getPRIcon(state) {
      const iconMap = {
        'open': 'el-icon-share',
        'closed': 'el-icon-circle-close',
        'merged': 'el-icon-success'
      }
      return iconMap[state] || 'el-icon-share'
    },

    // 获取标签页标题
    getTabTitle(tab) {
      const titleMap = {
        'overview': '概览',
        'issues': 'Issues',
        'pull-requests': 'Pull Requests',
        'projects': 'Projects',
        'actions': 'Actions',
        'wiki': 'Wiki',
        'security': 'Security',
        'insights': 'Insights',
        'settings': 'Settings'
      }
      return titleMap[tab] || tab
    },

    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    },

    // Issue创建成功
    handleIssueCreateSuccess(issue) {
      this.issues.unshift(issue)
      this.updateIssueStats()
      this.showCreateIssueDialog = false
      this.$message.success('Issue创建成功')
    },

    // 设置保存成功
    handleSettingsSuccess() {
      this.showSettings = false
      this.$message.success('设置保存成功')
    }
  }
}
</script>

<style scoped>
.project-manage-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.manage-header {
  border-bottom: 1px solid #e1e4e8;
  padding: 20px 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.project-info {
  flex: 1;
}

.project-title {
  font-size: 24px;
  font-weight: 600;
  color: #24292e;
  margin-bottom: 8px;
}

.project-description {
  font-size: 16px;
  color: #586069;
  margin: 0;
}

.manage-nav {
  border-bottom: 1px solid #e1e4e8;
}

:deep(.el-tabs__item) {
  position: relative;
  padding: 0 16px;
  font-size: 14px;
}

.tab-badge {
  background: #0366d6;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  margin-left: 4px;
}

.manage-content {
  padding: 20px 0;
}

/* 概览页面样式 */
.overview-stats {
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  padding: 20px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.stat-card:hover {
  border-color: #0366d6;
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.issue-icon { background: #28a745; }
.pr-icon { background: #6f42c1; }
.star-icon { background: #ffd33d; }
.fork-icon { background: #6a737d; }

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #24292e;
}

.stat-label {
  font-size: 14px;
  color: #586069;
}

.overview-main {
  margin-top: 30px;
}

.activity-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f6f8fa;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-avatar {
  margin-right: 12px;
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: 14px;
  color: #24292e;
}

.user-name {
  font-weight: 600;
}

.action {
  margin: 0 4px;
}

.target {
  color: #0366d6;
  cursor: pointer;
}

.activity-time {
  font-size: 12px;
  color: #586069;
  margin-top: 4px;
}

/* Issues页面样式 */
.page-header {
  margin-bottom: 20px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 300px;
}

.issues-list {
  background: white;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
}

.issue-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid #eaecef;
}

.issue-item:last-child {
  border-bottom: none;
}

.issue-icon {
  margin-right: 12px;
  font-size: 16px;
}

.issue-icon .open {
  color: #28a745;
}

.issue-icon .closed {
  color: #cb2431;
}

.issue-content {
  flex: 1;
}

.issue-title {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #24292e;
  cursor: pointer;
  margin-right: 8px;
}

.title-text:hover {
  color: #0366d6;
}

.issue-labels {
  display: flex;
  gap: 4px;
}

.issue-meta {
  font-size: 12px;
  color: #586069;
}

.issue-meta span {
  margin-right: 16px;
}

/* 占位页面样式 */
.placeholder-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.placeholder-content {
  text-align: center;
}

.placeholder-icon {
  font-size: 64px;
  color: #e1e4e8;
  margin-bottom: 20px;
}

.placeholder-content h3 {
  font-size: 20px;
  color: #24292e;
  margin-bottom: 8px;
}

.placeholder-content p {
  color: #586069;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .project-manage-container {
    padding: 0 16px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input {
    width: 100%;
  }
  
  .overview-main .el-col {
    margin-bottom: 20px;
  }
}
</style>