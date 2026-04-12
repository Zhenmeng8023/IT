<template>
  <div class="project-miss">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目下架管理</h1>
        <p class="page-subtitle">管理已下架的项目，支持查看、恢复、永久删除，并和项目管理/审核中心共享同一套管理入口。</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model.trim="projectIdInput"
          size="small"
          placeholder="可选：绑定项目 ID"
          class="project-id-input"
          @keyup.enter.native="applyProjectId"
        />
        <el-button size="small" @click="applyProjectId">绑定项目</el-button>
      </div>
    </div>

    <ProjectManageEntryHub
      current-key="miss"
      title="管理侧统一入口"
      subtitle="在项目管理、审核中心和下架治理之间切换时，尽量保留同一个项目上下文；未绑定时则按全局治理视角查看。"
      :project-id="projectId"
      :entries="manageEntries"
      :status-cards="statusCards"
    />

    <el-card class="filter-card" shadow="never">
      <div class="filter-toolbar">
        <div class="filter-left">
          <el-select v-model="filterForm.status" placeholder="下架状态" clearable style="width: 120px">
            <el-option label="已下架" value="removed"></el-option>
            <el-option label="已恢复" value="restored"></el-option>
            <el-option label="永久删除" value="deleted"></el-option>
          </el-select>
          
          <el-select v-model="filterForm.type" placeholder="项目类型" clearable style="width: 120px; margin-left: 10px">
            <el-option label="技术开发" value="tech"></el-option>
            <el-option label="产品设计" value="design"></el-option>
            <el-option label="运营推广" value="operation"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
          
          <el-date-picker
            v-model="filterForm.removeDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="下架开始日期"
            end-placeholder="下架结束日期"
            style="width: 240px; margin-left: 10px">
          </el-date-picker>
        </div>
        
        <div class="filter-right">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索项目名称或下架人"
            clearable
            style="width: 250px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button type="success" icon="el-icon-refresh-left" @click="handleBatchRestore" :disabled="selectedProjects.length === 0">
          批量恢复
        </el-button>
        <el-button type="danger" icon="el-icon-delete" @click="handleBatchDelete" :disabled="selectedProjects.length === 0">
          批量永久删除
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
        <div class="toolbar-right">
          <el-button type="text" icon="el-icon-download">
            导出数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 下架项目列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="projectList"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
        style="width: 100%">
        
        <el-table-column type="selection" width="55"></el-table-column>
        
        <el-table-column prop="name" label="项目名称" min-width="200">
          <template slot-scope="scope">
            <div class="project-name">
              <span class="name-text">{{ scope.row.name }}</span>
              <el-tag v-if="scope.row.isRecommended" size="mini" type="warning" style="margin-left: 5px">原推荐</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="applicant" label="原申请人" width="120">
          <template slot-scope="scope">
            <el-avatar :size="24" :src="scope.row.avatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
            {{ scope.row.applicant }}
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="项目类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="getTypeType(scope.row.type)" size="small">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="budget" label="预算(元)" width="120" align="center">
          <template slot-scope="scope">
            {{ formatBudget(scope.row.budget) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="removeTime" label="下架时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.removeTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="remover" label="下架人" width="120" align="center">
          <template slot-scope="scope">
            {{ scope.row.remover }}
          </template>
        </el-table-column>
        
        <el-table-column prop="removeReason" label="下架原因" min-width="150">
          <template slot-scope="scope">
            <el-tooltip v-if="scope.row.removeReason && scope.row.removeReason.length > 20" :content="scope.row.removeReason" placement="top">
              <span>{{ scope.row.removeReason.substring(0, 20) }}...</span>
            </el-tooltip>
            <span v-else>{{ scope.row.removeReason || '无' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="下架状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'removed'"
              size="mini"
              type="text"
              icon="el-icon-refresh-left"
              @click="handleRestore(scope.row)"
              style="color: #67C23A;">
              恢复
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'removed'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              style="color: #F56C6C;">
              永久删除
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'restored'"
              size="mini"
              type="text"
              icon="el-icon-info"
              style="color: #909399;">
              已恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 项目详情对话框 -->
    <el-dialog
      :title="currentProject ? currentProject.name + ' - 下架详情' : '项目下架详情'"
      :visible.sync="detailDialogVisible"
      width="60%"
      :before-close="handleCloseDetail">
      <div v-if="currentProject" class="project-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ currentProject.name }}</el-descriptions-item>
          <el-descriptions-item label="原申请人">{{ currentProject.applicant }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ currentProject.type }}</el-descriptions-item>
          <el-descriptions-item label="预算">{{ formatBudget(currentProject.budget) }}元</el-descriptions-item>
          <el-descriptions-item label="下架时间">{{ formatDate(currentProject.removeTime) }}</el-descriptions-item>
          <el-descriptions-item label="下架人">{{ currentProject.remover }}</el-descriptions-item>
          <el-descriptions-item label="下架原因" :span="2">{{ currentProject.removeReason || '无' }}</el-descriptions-item>
          <el-descriptions-item label="项目描述" :span="2">{{ currentProject.description }}</el-descriptions-item>
          <el-descriptions-item label="技术栈" :span="2">{{ currentProject.techStack }}</el-descriptions-item>
          <el-descriptions-item label="团队成员" :span="2">{{ currentProject.teamMembers }}</el-descriptions-item>
          <el-descriptions-item label="原审核时间">{{ formatDate(currentProject.auditTime) }}</el-descriptions-item>
          <el-descriptions-item label="原审核人">{{ currentProject.auditor }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentProject && currentProject.status === 'removed'" type="success" @click="handleRestore(currentProject)">恢复项目</el-button>
        <el-button v-if="currentProject && currentProject.status === 'removed'" type="danger" @click="handleDelete(currentProject)">永久删除</el-button>
      </span>
    </el-dialog>

    <!-- 永久删除确认对话框 -->
    <el-dialog
      title="永久删除确认"
      :visible.sync="deleteDialogVisible"
      width="400px">
      <div class="delete-warning">
        <el-alert
          title="警告"
          type="error"
          description="此操作将永久删除项目数据，删除后无法恢复，请谨慎操作！"
          show-icon
          :closable="false">
        </el-alert>
        <div style="margin-top: 15px;">
          <p>确定要永久删除项目 <strong>{{ currentProject ? currentProject.name : '' }}</strong> 吗？</p>
          <el-input
            v-model="deleteConfirmText"
            placeholder="请输入项目名称确认删除"
            style="margin-top: 10px;">
          </el-input>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button 
          type="danger" 
          @click="confirmDelete" 
          :disabled="!deleteConfirmText || deleteConfirmText !== (currentProject ? currentProject.name : '')">
          确认删除
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import ProjectManageEntryHub from '../../f_project/projectmanage/components/ProjectManageEntryHub.vue'

export default {
  name: 'ProjectMiss',
  layout: 'manage',
  components: {
    ProjectManageEntryHub
  },
  data() {
    const queryProjectId = this.$route && this.$route.query ? this.$route.query.projectId : ''
    return {
      projectId: queryProjectId ? Number(queryProjectId) : null,
      projectIdInput: queryProjectId ? String(queryProjectId) : '',
      // 筛选表单
      filterForm: {
        status: '',
        type: '',
        removeDateRange: [],
        keyword: ''
      },
      
      // 项目列表数据
      projectList: [],
      
      // 选中的项目
      selectedProjects: [],
      
      // 加载状态
      loading: false,
      
      // 分页信息
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      
      // 对话框显示状态
      detailDialogVisible: false,
      deleteDialogVisible: false,
      
      // 当前操作的项目
      currentProject: null,
      
      // 删除确认文本
      deleteConfirmText: ''
    }
  },
  computed: {
    returnManageTab() {
      const raw = String((this.$route.query && this.$route.query.fromTab) || 'overview')
      const allow = ['overview', 'repo-workbench', 'audit-manage', 'milestone-manage', 'release-manage', 'stat-manage', 'task-manage', 'member-manage', 'file-manage', 'doc-manage', 'activity-manage', 'sprint-manage', 'download-manage', 'settings']
      return allow.includes(raw) ? raw : 'overview'
    },
    removedCount() {
      return this.projectList.filter(item => item.status === 'removed').length
    },
    restoredCount() {
      return this.projectList.filter(item => item.status === 'restored').length
    },
    deletedCount() {
      return this.projectList.filter(item => item.status === 'deleted').length
    },
    manageEntries() {
      return [
        {
          key: 'manage',
          title: '项目管理',
          desc: '进入项目总览、任务、成员、仓库和设置的统一管理入口。',
          path: '/projectmanage',
          query: this.projectId ? { projectId: String(this.projectId), tab: this.returnManageTab } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'blue'
        },
        {
          key: 'audit',
          title: '审核中心',
          desc: '进入 MR、评审、检查和主线保护的统一治理入口。',
          path: '/projectaudit',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.returnManageTab } : undefined,
          requiresProjectId: true,
          disabled: !this.projectId,
          tone: 'cyan'
        },
        {
          key: 'miss',
          title: '下架管理',
          desc: '处理已下架项目的恢复、删除和状态复核。',
          path: '/projectmiss',
          query: this.projectId ? { projectId: String(this.projectId), fromTab: this.returnManageTab } : undefined,
          tone: 'orange'
        }
      ]
    },
    statusCards() {
      return [
        {
          key: 'context',
          label: '项目上下文',
          value: this.projectId ? `#${this.projectId}` : '全局治理',
          desc: this.projectId ? '从这里跳去项目管理或审核中心时会沿用同一项目上下文。' : '当前从全局视角查看下架治理记录。',
          tone: 'blue'
        },
        {
          key: 'total',
          label: '当前列表',
          value: this.projectList.length,
          desc: '当前页面里已加载的下架治理记录数量。',
          tone: 'cyan'
        },
        {
          key: 'removed',
          label: '待处理下架',
          value: this.removedCount,
          desc: this.removedCount ? '这些记录还可以继续恢复或永久删除。' : '当前没有待处理的下架记录。',
          tone: this.removedCount ? 'orange' : 'purple'
        },
        {
          key: 'selection',
          label: '当前选中',
          value: this.selectedProjects.length,
          desc: this.selectedProjects.length ? '已可执行批量恢复或批量删除。' : '先勾选记录，再进行批量治理。',
          tone: this.selectedProjects.length ? 'danger' : 'purple'
        }
      ]
    }
  },
  watch: {
    '$route.query.projectId': {
      immediate: false,
      handler(value) {
        this.projectId = value ? Number(value) : null
        this.projectIdInput = value ? String(value) : ''
      }
    }
  },
  
  mounted() {
    this.loadProjectList()
  },
  
  methods: {
    applyProjectId() {
      if (!this.projectIdInput) {
        this.projectId = null
        this.$router.replace({
          path: this.$route.path,
          query: Object.keys(this.$route.query || {}).reduce((result, key) => {
            if (key !== 'projectId') {
              result[key] = this.$route.query[key]
            }
            return result
          }, {})
        })
        return
      }
      const nextId = Number(this.projectIdInput)
      if (!nextId) {
        this.$message.warning('请输入有效的项目 ID')
        return
      }
      this.projectId = nextId
      this.$router.replace({
        path: this.$route.path,
        query: {
          ...this.$route.query,
          projectId: String(nextId)
        }
      })
    },
    // 加载下架项目列表
    async loadProjectList() {
      this.loading = true
      try {
        // TODO: 调用后端接口获取下架项目列表
        // const response = await this.$axios.get('/api/project/miss/list', {
        //   params: {
        //     ...this.filterForm,
        //     page: this.pagination.currentPage,
        //     size: this.pagination.pageSize
        //   }
        // })
        // this.projectList = response.data.list
        // this.pagination.total = response.data.total
        
        // 模拟数据
        this.projectList = [
          {
            id: 1,
            name: '智能客服系统开发',
            applicant: '张三',
            avatar: '',
            type: '技术开发',
            budget: 50000,
            removeTime: new Date('2024-01-20'),
            remover: '管理员A',
            removeReason: '项目需求变更，需要重新规划',
            status: 'removed',
            isRecommended: true,
            description: '开发基于AI的智能客服系统，支持多渠道接入',
            techStack: 'Vue.js, Node.js, Python, MySQL',
            teamMembers: '张三、李四、王五',
            auditTime: new Date('2024-01-15'),
            auditor: '审核员B'
          },
          {
            id: 2,
            name: '电商平台UI设计',
            applicant: '李四',
            avatar: '',
            type: '产品设计',
            budget: 20000,
            removeTime: new Date('2024-01-18'),
            remover: '管理员C',
            removeReason: '设计风格不符合要求',
            status: 'restored',
            isRecommended: false,
            description: '为电商平台设计现代化UI界面',
            techStack: 'Figma, Sketch, Adobe XD',
            teamMembers: '李四、赵六',
            auditTime: new Date('2024-01-12'),
            auditor: '审核员A'
          }
        ]
        this.pagination.total = 2
      } catch (error) {
        console.error('加载下架项目列表失败:', error)
        this.$message.error('加载下架项目列表失败')
      } finally {
        this.loading = false
      }
    },
    
    // 处理选择变化
    handleSelectionChange(selection) {
      this.selectedProjects = selection
    },
    
    // 搜索处理
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadProjectList()
    },
    
    // 刷新数据
    refreshData() {
      this.pagination.currentPage = 1
      this.loadProjectList()
    },
    
    // 分页大小变化
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.loadProjectList()
    },
    
    // 当前页变化
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadProjectList()
    },
    
    // 查看项目详情
    handleView(project) {
      this.currentProject = project
      this.detailDialogVisible = true
    },
    
    // 关闭详情对话框
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.currentProject = null
    },
    
    // 恢复项目
    async handleRestore(project) {
      try {
        await this.$confirm('确定要恢复这个项目吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口恢复项目
        // await this.$axios.post(`/api/project/miss/restore/${project.id}`)
        
        this.$message.success('项目恢复成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('恢复项目失败:', error)
          this.$message.error('恢复项目失败')
        }
      }
    },
    
    // 永久删除项目
    handleDelete(project) {
      this.currentProject = project
      this.deleteConfirmText = ''
      this.deleteDialogVisible = true
    },
    
    // 确认永久删除
    async confirmDelete() {
      try {
        // TODO: 调用后端接口永久删除项目
        // await this.$axios.delete(`/api/project/miss/delete/${this.currentProject.id}`)
        
        this.$message.success('项目永久删除成功')
        this.deleteDialogVisible = false
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('永久删除项目失败:', error)
        this.$message.error('永久删除项目失败')
      }
    },
    
    // 批量恢复
    async handleBatchRestore() {
      try {
        const projectIds = this.selectedProjects.map(p => p.id)
        await this.$confirm(`确定要批量恢复 ${projectIds.length} 个项目吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口批量恢复
        // await this.$axios.post('/api/project/miss/batch-restore', {
        //   ids: projectIds
        // })
        
        this.$message.success(`批量恢复 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量恢复失败:', error)
          this.$message.error('批量恢复失败')
        }
      }
    },
    
    // 批量永久删除
    async handleBatchDelete() {
      try {
        const projectIds = this.selectedProjects.map(p => p.id)
        await this.$confirm(`确定要批量永久删除 ${projectIds.length} 个项目吗？此操作不可逆！`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        
        // TODO: 调用后端接口批量删除
        // await this.$axios.post('/api/project/miss/batch-delete', {
        //   ids: projectIds
        // })
        
        this.$message.success(`批量永久删除 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          this.$message.error('批量删除失败')
        }
      }
    },
    
    // 格式化日期
    formatDate(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    // 格式化预算
    formatBudget(budget) {
      return budget.toLocaleString('zh-CN')
    },
    
    // 获取状态类型
    getStatusType(status) {
      const typeMap = {
        removed: 'warning',
        restored: 'success',
        deleted: 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    // 获取状态文本
    getStatusText(status) {
      const textMap = {
        removed: '已下架',
        restored: '已恢复',
        deleted: '已删除'
      }
      return textMap[status] || status
    },
    
    // 获取类型类型
    getTypeType(type) {
      const typeMap = {
        '技术开发': 'primary',
        '产品设计': 'success',
        '运营推广': 'warning',
        '其他': 'info'
      }
      return typeMap[type] || 'info'
    }
  }
}
</script>

<style scoped>
.project-miss {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #303133;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.project-id-input {
  width: 180px;
}

.filter-card {
  margin-bottom: 0;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-card {
  margin-bottom: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-right {
  margin-left: auto;
}

.project-name {
  display: flex;
  align-items: center;
}

.name-text {
  font-weight: 500;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.project-detail {
  max-height: 400px;
  overflow-y: auto;
}

.delete-warning {
  padding: 10px 0;
}

@media (max-width: 768px) {
  .project-miss {
    padding: 16px;
  }

  .page-header,
  .filter-toolbar {
    flex-direction: column;
  }

  .header-actions,
  .filter-left,
  .filter-right {
    width: 100%;
  }

  .project-id-input {
    width: 100%;
  }

  .filter-right .el-input {
    width: 100% !important;
  }
}
</style>
