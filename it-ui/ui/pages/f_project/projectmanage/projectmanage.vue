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
          <i class="el-icon-data-line" slot="label" title="概览"></i>
        </el-tab-pane>
        <el-tab-pane label="Issues" name="issues">
          <i class="el-icon-warning-outline" slot="label" title="Issues"></i>
          <span class="tab-badge" v-if="issueStats.total > 0">{{ issueStats.total }}</span>
        </el-tab-pane>
        <el-tab-pane label="Pull Requests" name="pull-requests">
          <i class="el-icon-share" slot="label" title="Pull Requests"></i>
          <span class="tab-badge" v-if="prStats.total > 0">{{ prStats.total }}</span>
        </el-tab-pane>
        <el-tab-pane label="项目任务管理" name="task-manage">
          <i class="el-icon-s-order" slot="label" title="项目任务管理"></i>
        </el-tab-pane>
        <el-tab-pane label="项目成员管理" name="member-manage">
          <i class="el-icon-s-custom" slot="label" title="项目成员管理"></i>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 主要内容区域 -->
    <div class="manage-content">
      <!-- 概览页面 -->
      <div v-if="activeTab === 'overview'" class="overview-page">
        <!-- ... 原有概览内容保持不变 ... -->
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

      <!-- Issues页面（保持不变） -->
      <div v-if="activeTab === 'issues'" class="issues-page">
        <!-- ... 原有issues内容 ... -->
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

      <!-- Pull Requests页面（保持不变） -->
      <div v-if="activeTab === 'pull-requests'" class="pr-page">
        <!-- ... 原有pr内容 ... -->
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

      <!-- 项目任务管理页面 -->
      <div v-if="activeTab === 'task-manage'" class="task-manage-page">
        <div class="page-header">
          <div class="filter-bar">
            <el-input
              v-model="taskFilter.keyword"
              placeholder="搜索任务..."
              size="small"
              class="search-input"
              @keyup.enter="searchTasks"
            >
              <template #append>
                <el-button icon="el-icon-search" @click="searchTasks"></el-button>
              </template>
            </el-input>
            <el-select v-model="taskFilter.status" size="small" @change="searchTasks">
              <el-option label="全部状态" value="all"></el-option>
              <el-option label="待处理" value="pending"></el-option>
              <el-option label="进行中" value="in-progress"></el-option>
              <el-option label="已完成" value="completed"></el-option>
            </el-select>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreateTaskDialog">
              新建任务
            </el-button>
          </div>
        </div>

        <div class="task-list">
          <el-table :data="filteredTasks" border style="width: 100%">
            <el-table-column prop="title" label="任务标题" min-width="180"></el-table-column>
            <el-table-column prop="description" label="描述" min-width="200"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template slot-scope="scope">
                <el-tag :type="getTaskStatusType(scope.row.status)" size="small">
                  {{ getTaskStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="负责人" width="120"></el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160">
              <template slot-scope="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template slot-scope="scope">
                <el-button size="mini" type="primary" @click="openEditTaskDialog(scope.row)">编辑</el-button>
                <el-button size="mini" type="danger" @click="deleteTask(scope.row.id)">删除</el-button>
                <el-dropdown @command="(cmd) => changeTaskStatus(scope.row.id, cmd)">
                  <el-button size="mini" type="info">
                    修改状态 <i class="el-icon-arrow-down el-icon--right"></i>
                  </el-button>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="pending">待处理</el-dropdown-item>
                    <el-dropdown-item command="in-progress">进行中</el-dropdown-item>
                    <el-dropdown-item command="completed">已完成</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 新建/编辑任务对话框 -->
        <el-dialog
          :title="taskDialogTitle"
          :visible.sync="taskDialogVisible"
          width="500px"
          @close="resetTaskForm"
        >
          <el-form :model="taskForm" label-width="80px">
            <el-form-item label="标题" required>
              <el-input v-model="taskForm.title" placeholder="请输入任务标题"></el-input>
            </el-form-item>
            <el-form-item label="描述">
              <el-input type="textarea" v-model="taskForm.description" rows="3" placeholder="请输入任务描述"></el-input>
            </el-form-item>
            <el-form-item label="负责人" required>
              <el-select v-model="taskForm.assigneeId" placeholder="请选择负责人" filterable>
                <el-option
                  v-for="member in members"
                  :key="member.id"
                  :label="member.name"
                  :value="member.id"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="taskForm.status">
                <el-option label="待处理" value="pending"></el-option>
                <el-option label="进行中" value="in-progress"></el-option>
                <el-option label="已完成" value="completed"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
          <span slot="footer" class="dialog-footer">
            <el-button @click="taskDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitTask">确定</el-button>
          </span>
        </el-dialog>
      </div>

      <!-- 项目成员管理页面 -->
      <div v-if="activeTab === 'member-manage'" class="member-manage-page">
        <div class="page-header">
          <div class="filter-bar">
            <el-input
              v-model="memberFilter.keyword"
              placeholder="搜索成员..."
              size="small"
              class="search-input"
              @keyup.enter="searchMembers"
            >
              <template #append>
                <el-button icon="el-icon-search" @click="searchMembers"></el-button>
              </template>
            </el-input>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddMemberDialog">
              添加成员
            </el-button>
            <el-button type="warning" size="small" icon="el-icon-switch-button" @click="quitProject">
              退出项目
            </el-button>
          </div>
        </div>

        <div class="member-list">
          <el-table :data="filteredMembers" border style="width: 100%">
            <el-table-column prop="name" label="姓名" min-width="120"></el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
            <el-table-column prop="role" label="角色" width="100">
              <template slot-scope="scope">
                <el-tag :type="scope.row.role === 'owner' ? 'danger' : 'info'">
                  {{ scope.row.role === 'owner' ? '负责人' : '成员' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="joinTime" label="加入时间" width="160">
              <template slot-scope="scope">
                {{ formatTime(scope.row.joinTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template slot-scope="scope">
                <el-dropdown @command="(cmd) => handleMemberAction(cmd, scope.row)">
                  <el-button size="mini" type="primary">
                    管理 <i class="el-icon-arrow-down el-icon--right"></i>
                  </el-button>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="editRole">修改角色</el-dropdown-item>
                    <el-dropdown-item command="delete" :divided="true">删除成员</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 添加成员对话框 -->
        <el-dialog
          title="添加成员"
          :visible.sync="addMemberDialogVisible"
          width="400px"
        >
          <el-form :model="newMemberForm" label-width="80px">
            <el-form-item label="用户名" required>
              <el-input v-model="newMemberForm.name" placeholder="请输入用户名"></el-input>
            </el-form-item>
            <el-form-item label="邮箱" required>
              <el-input v-model="newMemberForm.email" placeholder="请输入邮箱"></el-input>
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="newMemberForm.role">
                <el-option label="成员" value="member"></el-option>
                <el-option label="负责人" value="owner"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
          <span slot="footer">
            <el-button @click="addMemberDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="addMember">确定</el-button>
          </span>
        </el-dialog>

        <!-- 修改角色对话框 -->
        <el-dialog
          title="修改角色"
          :visible.sync="editRoleDialogVisible"
          width="400px"
        >
          <el-form :model="editRoleForm" label-width="80px">
            <el-form-item label="新角色">
              <el-select v-model="editRoleForm.role">
                <el-option label="成员" value="member"></el-option>
                <el-option label="负责人" value="owner"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
          <span slot="footer">
            <el-button @click="editRoleDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="updateMemberRole">确定</el-button>
          </span>
        </el-dialog>
      </div>
    </div>

    <!-- 原有的新建Issue对话框和设置对话框保持不变 -->
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

    <!-- 新建项目对话框 -->

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
  UpdateProjectSettings,
  // 任务管理相关接口（预留）
  GetProjectTasks,
  CreateTask,
  UpdateTask,
  DeleteTask,
  UpdateTaskStatus,
  // 成员管理相关接口（预留）
  GetProjectMembers,
  AddProjectMember,
  UpdateMemberRole,
  RemoveProjectMember,
  QuitProject
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
      showSettings: false,
      
      // ========== 项目任务管理相关数据 ==========
      tasks: [],
      taskFilter: {
        keyword: '',
        status: 'all'
      },
      taskDialogVisible: false,
      taskDialogType: 'create', // 'create' or 'edit'
      taskForm: {
        id: null,
        title: '',
        description: '',
        assigneeId: null,
        status: 'pending'
      },
      
      // ========== 项目成员管理相关数据 ==========
      members: [],
      memberFilter: {
        keyword: ''
      },
      addMemberDialogVisible: false,
      editRoleDialogVisible: false,
      newMemberForm: {
        name: '',
        email: '',
        role: 'member'
      },
      editRoleForm: {
        memberId: null,
        role: ''
      },
      // 当前登录用户（模拟，后期接入真实用户）
      currentUser: {
        id: 1,
        name: '当前用户',
        email: 'current@example.com'
      },
      
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
    },
    // 任务过滤
    filteredTasks() {
      return this.tasks.filter(task => {
        const matchesKeyword = !this.taskFilter.keyword ||
          task.title.toLowerCase().includes(this.taskFilter.keyword.toLowerCase()) ||
          (task.description && task.description.toLowerCase().includes(this.taskFilter.keyword.toLowerCase()))
        const matchesStatus = this.taskFilter.status === 'all' || task.status === this.taskFilter.status
        return matchesKeyword && matchesStatus
      })
    },
    // 成员过滤
    filteredMembers() {
      return this.members.filter(member => {
        return !this.memberFilter.keyword ||
          member.name.toLowerCase().includes(this.memberFilter.keyword.toLowerCase()) ||
          (member.email && member.email.toLowerCase().includes(this.memberFilter.keyword.toLowerCase()))
      })
    },
    taskDialogTitle() {
      return this.taskDialogType === 'create' ? '新建任务' : '编辑任务'
    }
  },
  async mounted() {
    await this.loadProjectData()
    await this.loadIssues()
    await this.loadPullRequests()
    await this.loadActivities()
    await this.loadContributors()
    await this.loadTasks()      // 加载任务列表
    await this.loadMembers()    // 加载成员列表
  },
  methods: {
  

    // 加载项目数据（保持不变）
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

    // 加载Issues（保持不变）
    async loadIssues() {
      try {
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

    // 加载Pull Requests（保持不变）
    async loadPullRequests() {
      try {
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

    // 加载活动记录（保持不变）
    async loadActivities() {
      try {
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

    // 加载贡献者（保持不变）
    async loadContributors() {
      try {
        this.contributors = [
          { id: 1, name: '开发者A', avatar: '' },
          { id: 2, name: '开发者B', avatar: '' },
          { id: 3, name: '开发者C', avatar: '' }
        ]
      } catch (error) {
        console.error('加载贡献者失败')
      }
    },

    // 加载任务列表
    async loadTasks() {
      try {
        // 预留API调用
        // const response = await GetProjectTasks(this.project.id)
        // this.tasks = response.data
        
        // 模拟任务数据
        this.tasks = [
          {
            id: 1,
            title: '设计数据库模型',
            description: '完成项目核心数据表设计',
            status: 'completed',
            assigneeId: 2,
            assigneeName: '开发者B',
            createTime: '2024-03-15T10:00:00Z'
          },
          {
            id: 2,
            title: '实现用户认证模块',
            description: '完成登录、注册、JWT功能',
            status: 'in-progress',
            assigneeId: 1,
            assigneeName: '开发者A',
            createTime: '2024-03-18T14:30:00Z'
          },
          {
            id: 3,
            title: '编写单元测试',
            description: '为核心业务逻辑编写测试用例',
            status: 'pending',
            assigneeId: 3,
            assigneeName: '开发者C',
            createTime: '2024-03-22T09:15:00Z'
          }
        ]
      } catch (error) {
        this.$message.error('加载任务列表失败')
      }
    },

    // 加载成员列表
    async loadMembers() {
      try {
        // 预留API调用
        // const response = await GetProjectMembers(this.project.id)
        // this.members = response.data
        
        // 模拟成员数据，包含当前用户
        this.members = [
          {
            id: 1,
            name: '当前用户',
            email: 'current@example.com',
            role: 'owner',
            joinTime: '2024-03-01T08:00:00Z'
          },
          {
            id: 2,
            name: '开发者A',
            email: 'devA@example.com',
            role: 'member',
            joinTime: '2024-03-05T10:30:00Z'
          },
          {
            id: 3,
            name: '开发者B',
            email: 'devB@example.com',
            role: 'member',
            joinTime: '2024-03-10T14:20:00Z'
          }
        ]
      } catch (error) {
        this.$message.error('加载成员列表失败')
      }
    },

    // ========== 任务管理方法 ==========
    openCreateTaskDialog() {
      this.taskDialogType = 'create'
      this.taskForm = {
        id: null,
        title: '',
        description: '',
        assigneeId: null,
        status: 'pending'
      }
      this.taskDialogVisible = true
    },
    
    openEditTaskDialog(task) {
      this.taskDialogType = 'edit'
      this.taskForm = {
        id: task.id,
        title: task.title,
        description: task.description,
        assigneeId: task.assigneeId,
        status: task.status
      }
      this.taskDialogVisible = true
    },
    
    resetTaskForm() {
      this.taskForm = {
        id: null,
        title: '',
        description: '',
        assigneeId: null,
        status: 'pending'
      }
    },
    
    submitTask() {
      if (!this.taskForm.title || !this.taskForm.assigneeId) {
        this.$message.warning('请填写完整信息')
        return
      }
      const assignee = this.members.find(m => m.id === this.taskForm.assigneeId)
      if (!assignee) return
      
      if (this.taskDialogType === 'create') {
        // 新建任务
        const newTask = {
          id: Date.now(),
          title: this.taskForm.title,
          description: this.taskForm.description,
          status: this.taskForm.status,
          assigneeId: this.taskForm.assigneeId,
          assigneeName: assignee.name,
          createTime: new Date().toISOString()
        }
        // 预留API调用
        // await CreateTask(this.project.id, newTask)
        this.tasks.unshift(newTask)
        this.$message.success('任务创建成功')
      } else {
        // 编辑任务
        const index = this.tasks.findIndex(t => t.id === this.taskForm.id)
        if (index !== -1) {
          const updatedTask = {
            ...this.tasks[index],
            title: this.taskForm.title,
            description: this.taskForm.description,
            status: this.taskForm.status,
            assigneeId: this.taskForm.assigneeId,
            assigneeName: assignee.name
          }
          // 预留API调用
          // await UpdateTask(this.project.id, updatedTask)
          this.$set(this.tasks, index, updatedTask)
          this.$message.success('任务更新成功')
        }
      }
      this.taskDialogVisible = false
    },
    
    async deleteTask(taskId) {
      try {
        await this.$confirm('确定删除该任务吗？', '提示', { type: 'warning' })
        // 预留API调用
        // await DeleteTask(this.project.id, taskId)
        const index = this.tasks.findIndex(t => t.id === taskId)
        if (index !== -1) {
          this.tasks.splice(index, 1)
          this.$message.success('删除成功')
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },
    
    async changeTaskStatus(taskId, newStatus) {
      try {
        // 预留API调用
        // await UpdateTaskStatus(this.project.id, taskId, newStatus)
        const task = this.tasks.find(t => t.id === taskId)
        if (task) {
          task.status = newStatus
          this.$message.success('状态更新成功')
        }
      } catch (error) {
        this.$message.error('状态更新失败')
      }
    },
    
    searchTasks() {
      // 搜索逻辑由 computed 处理
    },
    
    getTaskStatusType(status) {
      const map = {
        'pending': 'info',
        'in-progress': 'warning',
        'completed': 'success'
      }
      return map[status] || 'info'
    },
    
    getTaskStatusText(status) {
      const map = {
        'pending': '待处理',
        'in-progress': '进行中',
        'completed': '已完成'
      }
      return map[status] || status
    },
    
    // ========== 成员管理方法 ==========
    openAddMemberDialog() {
      this.newMemberForm = { name: '', email: '', role: 'member' }
      this.addMemberDialogVisible = true
    },
    
    async addMember() {
      if (!this.newMemberForm.name || !this.newMemberForm.email) {
        this.$message.warning('请填写完整信息')
        return
      }
      // 预留API调用
      // await AddProjectMember(this.project.id, this.newMemberForm)
      const newMember = {
        id: Date.now(),
        name: this.newMemberForm.name,
        email: this.newMemberForm.email,
        role: this.newMemberForm.role,
        joinTime: new Date().toISOString()
      }
      this.members.push(newMember)
      this.$message.success('添加成员成功')
      this.addMemberDialogVisible = false
    },
    
    handleMemberAction(command, member) {
      if (command === 'editRole') {
        this.editRoleForm.memberId = member.id
        this.editRoleForm.role = member.role
        this.editRoleDialogVisible = true
      } else if (command === 'delete') {
        this.deleteMember(member)
      }
    },
    
    async deleteMember(member) {
      try {
        await this.$confirm(`确定删除成员 ${member.name} 吗？`, '提示', { type: 'warning' })
        // 预留API调用
        // await RemoveProjectMember(this.project.id, member.id)
        const index = this.members.findIndex(m => m.id === member.id)
        if (index !== -1) {
          this.members.splice(index, 1)
          this.$message.success('删除成功')
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },
    
    async updateMemberRole() {
      const member = this.members.find(m => m.id === this.editRoleForm.memberId)
      if (member) {
        // 预留API调用
        // await UpdateMemberRole(this.project.id, member.id, this.editRoleForm.role)
        member.role = this.editRoleForm.role
        this.$message.success('角色更新成功')
      }
      this.editRoleDialogVisible = false
    },
    
    async quitProject() {
      try {
        await this.$confirm('确定退出项目吗？退出后您将无法查看项目内容。', '提示', { type: 'warning' })
        // 预留API调用
        // await QuitProject(this.project.id, this.currentUser.id)
        const index = this.members.findIndex(m => m.id === this.currentUser.id)
        if (index !== -1) {
          this.members.splice(index, 1)
          this.$message.success('已退出项目')
          // 可跳转至项目列表页
          // this.$router.push('/projects')
        } else {
          this.$message.error('当前用户不在项目中')
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('退出项目失败')
        }
      }
    },
    
    searchMembers() {
      // 搜索逻辑由 computed 处理
    },
    
    // 原有的公共方法（保持不变）
    updateIssueStats() {
      this.issueStats.total = this.issues.length
      this.issueStats.open = this.issues.filter(i => i.state === 'open').length
      this.issueStats.closed = this.issues.filter(i => i.state === 'closed').length
    },
    
    updatePRStats() {
      this.prStats.total = this.pullRequests.length
      this.prStats.open = this.pullRequests.filter(pr => pr.state === 'open').length
      this.prStats.closed = this.pullRequests.filter(pr => pr.state === 'closed').length
      this.prStats.merged = this.pullRequests.filter(pr => pr.state === 'merged').length
    },
    
    handleTabChange(tab) {
      this.activeTab = tab.name
    },
    
    searchIssues() {},
    filterIssues() {},
    createIssue() { this.showCreateIssueDialog = true },
    createPR() { this.showCreatePRDialog = true },
    viewIssue(issueId) { this.$router.push(`/f_project/issue?issueId=${issueId}`) },
    viewPR(prId) { this.$router.push(`/f_project/pull-request?prId=${prId}`) },
    
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
    
    getPRIcon(state) {
      const iconMap = {
        'open': 'el-icon-share',
        'closed': 'el-icon-circle-close',
        'merged': 'el-icon-success'
      }
      return iconMap[state] || 'el-icon-share'
    },
    
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    },
    
    handleIssueCreateSuccess(issue) {
      this.issues.unshift(issue)
      this.updateIssueStats()
      this.showCreateIssueDialog = false
      this.$message.success('Issue创建成功')
    },
    
    handleSettingsSuccess() {
      this.showSettings = false
      this.$message.success('设置保存成功')
    }
  }
}
</script>

<style scoped>
/* ========== 全局变量 ========== */
.project-manage-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* ========== 头部样式 ========== */
.manage-header {
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 100%);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.project-info {
  flex: 1;
}

.project-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2f3d;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-title i {
  color: #409eff;
  font-size: 28px;
}

.project-description {
  font-size: 15px;
  color: #5a6e7c;
  margin: 0;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button {
  border-radius: 20px;
  padding: 8px 20px;
  font-weight: 500;
  transition: all 0.2s;
}

.header-actions .el-button--primary {
  background: linear-gradient(135deg, #409eff, #36a1ff);
  border: none;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}

.header-actions .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

/* ========== 导航标签 ========== */
.manage-nav {
  margin-bottom: 28px;
  background: white;
  border-radius: 12px;
  padding: 0 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.manage-nav :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: none;
}

.manage-nav :deep(.el-tabs__item) {
  padding: 0 20px;
  height: 52px;
  line-height: 52px;
  font-size: 15px;
  font-weight: 500;
  color: #5a6e7c;
  transition: color 0.2s;
}

.manage-nav :deep(.el-tabs__item.is-active) {
  color: #409eff;
  font-weight: 600;
}

.manage-nav :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(90deg, #409eff, #66b1ff);
}

.manage-nav :deep(.el-tabs__item:hover) {
  color: #409eff;
}

.tab-badge {
  background: #ecf5ff;
  color: #409eff;
  border-radius: 20px;
  padding: 2px 8px;
  font-size: 12px;
  margin-left: 8px;
  font-weight: normal;
}

/* ========== 通用卡片样式 ========== */
.el-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}

.el-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.el-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f2f5;
  padding: 16px 20px;
  font-weight: 600;
  background-color: #fafcfd;
  color: #1f2f3d;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}

/* ========== 概览页统计卡片 ========== */
.overview-stats {
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 20px;
  padding: 20px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #edf2f7;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: #e0e7ff;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 26px;
  color: white;
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.issue-icon { background: linear-gradient(135deg, #28a745, #34ce57); }
.pr-icon { background: linear-gradient(135deg, #6f42c1, #8c62d6); }
.star-icon { background: linear-gradient(135deg, #ffc107, #ffda6a); }
.fork-icon { background: linear-gradient(135deg, #6c757d, #8f9bae); }

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 800;
  color: #1f2f3d;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #6c7a8a;
  margin-top: 4px;
}

/* 活动列表 */
.activity-item {
  display: flex;
  padding: 14px 0;
  border-bottom: 1px solid #f0f2f5;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-avatar {
  margin-right: 14px;
}

.activity-content {
  flex: 1;
}

.activity-text {
  font-size: 14px;
  color: #2c3e50;
}

.user-name {
  font-weight: 600;
  color: #1f2f3d;
}

.action {
  margin: 0 4px;
  color: #5a6e7c;
}

.target {
  color: #409eff;
  cursor: pointer;
  font-weight: 500;
}

.activity-time {
  font-size: 12px;
  color: #8a9aa8;
  margin-top: 6px;
}

/* 项目信息 & 贡献者 */
.info-list {
  padding: 8px 0;
}

.info-item {
  margin-bottom: 12px;
  font-size: 14px;
}

.info-label {
  color: #6c7a8a;
  width: 70px;
  display: inline-block;
}

.info-value {
  color: #2c3e50;
  font-weight: 500;
}

.contributors-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 8px 0;
}

.contributor-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: calc(50% - 8px);
}

.contributor-name {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 500;
}

/* ========== Issues / PR 列表样式 ========== */
.issues-page,
.pr-page {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-header {
  margin-bottom: 24px;
}

.filter-bar {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.issues-list,
.pr-list {
  border-top: 1px solid #edf2f7;
}

.issue-item,
.pr-item {
  display: flex;
  align-items: flex-start;
  padding: 20px 12px;
  border-bottom: 1px solid #f0f2f5;
  transition: background 0.2s;
}

.issue-item:hover,
.pr-item:hover {
  background: #fafcfd;
}

.issue-icon,
.pr-icon {
  margin-right: 16px;
  font-size: 20px;
}

.issue-icon .open {
  color: #28a745;
}
.issue-icon .closed {
  color: #cb2431;
}

.issue-content,
.pr-content {
  flex: 1;
}

.issue-title,
.pr-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #1f2f3d;
  cursor: pointer;
  transition: color 0.2s;
}

.title-text:hover {
  color: #409eff;
}

.issue-labels,
.pr-labels {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.issue-meta,
.pr-meta {
  font-size: 12px;
  color: #8a9aa8;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

/* ========== 任务管理 & 成员管理 表格美化 ========== */
.task-manage-page,
.member-manage-page {
  background: white;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.task-list,
.member-list {
  margin-top: 24px;
  overflow-x: auto;
}

.el-table {
  border-radius: 16px;
  overflow: hidden;
  font-size: 14px;
}

.el-table :deep(th) {
  background-color: #fafcfd;
  color: #2c3e50;
  font-weight: 600;
  border-bottom: 1px solid #edf2f7;
}

.el-table :deep(td) {
  border-bottom: 1px solid #f5f7fa;
  color: #2c3e50;
}

.el-table :deep(.el-table__row:hover > td) {
  background-color: #f8fafc;
}

.el-table :deep(.el-button--mini) {
  border-radius: 12px;
  padding: 5px 12px;
  font-size: 12px;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409eff;
}

/* 任务状态标签优化 */
.el-tag {
  border-radius: 20px;
  border: none;
  font-weight: 500;
  padding: 0 12px;
  height: 24px;
  line-height: 24px;
}

/* ========== 对话框美化 ========== */
.el-dialog {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.el-dialog :deep(.el-dialog__header) {
  background: #fafcfd;
  padding: 18px 24px;
  border-bottom: 1px solid #edf2f7;
}

.el-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #1f2f3d;
}

.el-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.el-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #edf2f7;
  background: #fafcfd;
}

.el-form-item__label {
  font-weight: 500;
  color: #2c3e50;
}

.el-input__inner,
.el-textarea__inner,
.el-select .el-input__inner {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.el-input__inner:focus,
.el-textarea__inner:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* ========== 响应式布局 ========== */
@media (max-width: 992px) {
  .project-manage-container {
    padding: 0 16px;
  }

  .manage-header {
    padding: 20px;
  }

  .project-title {
    font-size: 24px;
  }

  .stat-card {
    padding: 16px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
    font-size: 22px;
  }

  .stat-number {
    font-size: 24px;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
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

  .contributor-item {
    width: 100%;
  }

  .issue-title,
  .pr-title {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>