<template>
  <div class="project-audit">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>项目审核</h1>
      <p>审核用户提交的项目申请，支持批量操作和详细查看</p>
    </div>

    <!-- 筛选工具栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-toolbar">
        <div class="filter-left">
          <el-select v-model="filterForm.status" placeholder="审核状态" clearable style="width: 120px">
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已通过" value="approved"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
          </el-select>
          
          <el-select v-model="filterForm.type" placeholder="项目类型" clearable style="width: 120px; margin-left: 10px">
            <el-option label="技术开发" value="tech"></el-option>
            <el-option label="产品设计" value="design"></el-option>
            <el-option label="运营推广" value="operation"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
          
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px; margin-left: 10px">
          </el-date-picker>
        </div>
        
        <div class="filter-right">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索项目名称或申请人"
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
        <el-button type="primary" icon="el-icon-check" @click="handleBatchApprove" :disabled="selectedProjects.length === 0">
          批量通过
        </el-button>
        <el-button type="danger" icon="el-icon-close" @click="handleBatchReject" :disabled="selectedProjects.length === 0">
          批量拒绝
        </el-button>
        <el-button type="warning" icon="el-icon-star-on" @click="handleBatchRecommend" :disabled="selectedProjects.length === 0 || !selectedProjects.some(p => p.status === 'approved' && !p.isRecommended)">
          批量推荐
        </el-button>
        <el-button type="info" icon="el-icon-star-off" @click="handleBatchCancelRecommend" :disabled="selectedProjects.length === 0 || !selectedProjects.some(p => p.isRecommended)">
          取消推荐
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

    <!-- 项目列表 -->
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
              <el-tag v-if="scope.row.isRecommended" size="mini" type="warning" style="margin-left: 5px">推荐</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="applicant" label="申请人" width="120">
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
        
        <el-table-column prop="duration" label="预计周期" width="100" align="center">
          <template slot-scope="scope">
            {{ scope.row.duration }}天
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="提交时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="auditTime" label="审核时间" width="160" align="center">
          <template slot-scope="scope">
            {{ scope.row.auditTime ? formatDate(scope.row.auditTime) : '-' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="审核状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="handleApprove(scope.row)"
              style="color: #67C23A;">
              通过
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-close"
              @click="handleReject(scope.row)"
              style="color: #F56C6C;">
              拒绝
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'approved'"
              size="mini"
              type="text"
              :icon="scope.row.isRecommended ? 'el-icon-star-on' : 'el-icon-star-off'"
              @click="handleToggleRecommend(scope.row)"
              :style="{color: scope.row.isRecommended ? '#E6A23C' : '#909399'}">
              {{ scope.row.isRecommended ? '取消推荐' : '推荐' }}
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              style="color: #F56C6C;">
              删除
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
      :title="currentProject ? currentProject.name : '项目详情'"
      :visible.sync="detailDialogVisible"
      width="60%"
      :before-close="handleCloseDetail">
      <div v-if="currentProject" class="project-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="项目名称">{{ currentProject.name }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentProject.applicant }}</el-descriptions-item>
          <el-descriptions-item label="项目类型">{{ currentProject.type }}</el-descriptions-item>
          <el-descriptions-item label="预算">{{ formatBudget(currentProject.budget) }}元</el-descriptions-item>
          <el-descriptions-item label="预计周期">{{ currentProject.duration }}天</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDate(currentProject.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="项目描述" :span="2">{{ currentProject.description }}</el-descriptions-item>
          <el-descriptions-item label="技术栈" :span="2">{{ currentProject.techStack }}</el-descriptions-item>
          <el-descriptions-item label="团队成员" :span="2">{{ currentProject.teamMembers }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentProject && currentProject.status === 'pending'" type="primary" @click="handleApprove(currentProject)">通过审核</el-button>
        <el-button v-if="currentProject && currentProject.status === 'pending'" type="danger" @click="handleReject(currentProject)">拒绝审核</el-button>
      </span>
    </el-dialog>

    <!-- 审核拒绝对话框 -->
    <el-dialog
      title="拒绝审核"
      :visible.sync="rejectDialogVisible"
      width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input
            type="textarea"
            :rows="3"
            v-model="rejectForm.reason"
            placeholder="请输入拒绝原因（必填）">
          </el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :disabled="!rejectForm.reason.trim()">确认拒绝</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ProjectAudit',
  layout: 'manage',
  data() {
    return {
      // 筛选表单
      filterForm: {
        status: '',
        type: '',
        dateRange: [],
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
      rejectDialogVisible: false,
      
      // 当前操作的项目
      currentProject: null,
      
      // 拒绝表单
      rejectForm: {
        reason: ''
      }
    }
  },
  
  mounted() {
    this.loadProjectList()
  },
  
  methods: {
    // 加载项目列表
    async loadProjectList() {
      this.loading = true
      try {
        // TODO: 调用后端接口获取项目列表
        // const response = await this.$axios.get('/api/project/audit/list', {
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
            duration: 30,
            createTime: new Date('2024-01-15'),
            auditTime: null,
            status: 'pending',
            isRecommended: false,
            description: '开发基于AI的智能客服系统，支持多渠道接入',
            techStack: 'Vue.js, Node.js, Python, MySQL',
            teamMembers: '张三、李四、王五'
          },
          {
            id: 2,
            name: '电商平台UI设计',
            applicant: '李四',
            avatar: '',
            type: '产品设计',
            budget: 20000,
            duration: 15,
            createTime: new Date('2024-01-10'),
            auditTime: new Date('2024-01-12'),
            status: 'approved',
            isRecommended: true,
            description: '为电商平台设计现代化UI界面',
            techStack: 'Figma, Sketch, Adobe XD',
            teamMembers: '李四、赵六'
          }
        ]
        this.pagination.total = 2
      } catch (error) {
        console.error('加载项目列表失败:', error)
        this.$message.error('加载项目列表失败')
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
    
    // 通过审核
    async handleApprove(project) {
      try {
        // TODO: 调用后端接口通过审核
        // await this.$axios.post(`/api/project/audit/approve/${project.id}`)
        
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('审核通过失败:', error)
        this.$message.error('审核通过失败')
      }
    },
    
    // 拒绝审核
    handleReject(project) {
      this.currentProject = project
      this.rejectForm.reason = ''
      this.rejectDialogVisible = true
    },
    
    // 确认拒绝
    async confirmReject() {
      try {
        // TODO: 调用后端接口拒绝审核
        // await this.$axios.post(`/api/project/audit/reject/${this.currentProject.id}`, {
        //   reason: this.rejectForm.reason
        // })
        
        this.$message.success('审核拒绝成功')
        this.rejectDialogVisible = false
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('审核拒绝失败:', error)
        this.$message.error('审核拒绝失败')
      }
    },
    
    // 批量通过
    async handleBatchApprove() {
      try {
        const projectIds = this.selectedProjects.map(p => p.id)
        // TODO: 调用后端接口批量通过
        // await this.$axios.post('/api/project/audit/batch-approve', {
        //   ids: projectIds
        // })
        
        this.$message.success(`批量通过 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量通过失败:', error)
        this.$message.error('批量通过失败')
      }
    },
    
    // 批量拒绝
    async handleBatchReject() {
      try {
        const projectIds = this.selectedProjects.map(p => p.id)
        // TODO: 调用后端接口批量拒绝
        // await this.$axios.post('/api/project/audit/batch-reject', {
        //   ids: projectIds,
        //   reason: '批量拒绝'
        // })
        
        this.$message.success(`批量拒绝 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量拒绝失败:', error)
        this.$message.error('批量拒绝失败')
      }
    },
    
    // 推荐项目
    async handleToggleRecommend(project) {
      try {
        // TODO: 调用后端接口推荐/取消推荐
        // await this.$axios.post(`/api/project/audit/toggle-recommend/${project.id}`)
        
        this.$message.success(project.isRecommended ? '取消推荐成功' : '推荐成功')
        this.refreshData()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      }
    },
    
    // 批量推荐
    async handleBatchRecommend() {
      try {
        const projectIds = this.selectedProjects.filter(p => p.status === 'approved' && !p.isRecommended).map(p => p.id)
        // TODO: 调用后端接口批量推荐
        // await this.$axios.post('/api/project/audit/batch-recommend', {
        //   ids: projectIds
        // })
        
        this.$message.success(`批量推荐 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量推荐失败:', error)
        this.$message.error('批量推荐失败')
      }
    },
    
    // 批量取消推荐
    async handleBatchCancelRecommend() {
      try {
        const projectIds = this.selectedProjects.filter(p => p.isRecommended).map(p => p.id)
        // TODO: 调用后端接口批量取消推荐
        // await this.$axios.post('/api/project/audit/batch-cancel-recommend', {
        //   ids: projectIds
        // })
        
        this.$message.success(`批量取消推荐 ${projectIds.length} 个项目成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量取消推荐失败:', error)
        this.$message.error('批量取消推荐失败')
      }
    },
    
    // 删除项目
    async handleDelete(project) {
      try {
        await this.$confirm('确定要删除这个项目吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口删除项目
        // await this.$axios.delete(`/api/project/audit/delete/${project.id}`)
        
        this.$message.success('删除成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
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
        pending: 'warning',
        approved: 'success',
        rejected: 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    // 获取状态文本
    getStatusText(status) {
      const textMap = {
        pending: '待审核',
        approved: '已通过',
        rejected: '已拒绝'
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
.project-audit {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
}

.toolbar-right {
  margin-left: auto;
}

.table-card {
  margin-bottom: 20px;
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
</style>