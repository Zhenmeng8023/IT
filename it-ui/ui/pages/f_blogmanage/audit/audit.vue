<template>
  <div class="blog-audit">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>博客审核</h1>
      <p>审核用户提交的博客内容，支持批量操作和详细查看</p>
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
          
          <el-select v-model="filterForm.category" placeholder="分类" clearable style="width: 120px; margin-left: 10px">
            <el-option label="技术" value="tech"></el-option>
            <el-option label="生活" value="life"></el-option>
            <el-option label="学习" value="study"></el-option>
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
            placeholder="搜索博客标题或作者"
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
        <el-button type="primary" icon="el-icon-check" @click="handleBatchApprove" :disabled="selectedBlogs.length === 0">
          批量通过
        </el-button>
        <el-button type="danger" icon="el-icon-close" @click="handleBatchReject" :disabled="selectedBlogs.length === 0">
          批量拒绝
        </el-button>
        <el-button type="warning" icon="el-icon-top" @click="handleBatchTop" :disabled="selectedBlogs.length === 0 || !selectedBlogs.some(b => b.status === 'approved' && !b.isTop)">
          批量置顶
        </el-button>
        <el-button type="info" icon="el-icon-bottom" @click="handleBatchCancelTop" :disabled="selectedBlogs.length === 0 || !selectedBlogs.some(b => b.isTop)">
          取消置顶
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

    <!-- 博客列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="blogList"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
        style="width: 100%">
        
        <el-table-column type="selection" width="55"></el-table-column>
        
        <el-table-column prop="title" label="博客标题" min-width="200">
          <template slot-scope="scope">
            <div class="blog-title">
              <span class="title-text">{{ scope.row.title }}</span>
              <el-tag v-if="scope.row.isTop" size="mini" type="warning" style="margin-left: 5px">置顶</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="author" label="作者" width="120">
          <template slot-scope="scope">
            <el-avatar :size="24" :src="scope.row.avatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
            {{ scope.row.author }}
          </template>
        </el-table-column>
        
        <el-table-column prop="category" label="分类" width="100">
          <template slot-scope="scope">
            <el-tag :type="getCategoryType(scope.row.category)" size="small">
              {{ scope.row.category }}
            </el-tag>
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
        
        <el-table-column prop="viewCount" label="阅读量" width="80" align="center">
          <template slot-scope="scope">
            {{ scope.row.viewCount }}
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="审核状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="280" fixed="right" align="center">
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
              :icon="scope.row.isTop ? 'el-icon-top' : 'el-icon-bottom'"
              @click="handleToggleTop(scope.row)"
              :style="{color: scope.row.isTop ? '#E6A23C' : '#909399'}">
              {{ scope.row.isTop ? '取消置顶' : '置顶' }}
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

    <!-- 博客详情对话框 -->
    <el-dialog
      title="博客详情"
      :visible.sync="detailDialogVisible"
      width="80%"
      top="5vh">
      
      <div v-if="currentBlog" class="blog-detail">
        <div class="detail-header">
          <h2>{{ currentBlog.title }}</h2>
          <div class="meta-info">
            <span>作者：{{ currentBlog.author }}</span>
            <span>分类：{{ currentBlog.category }}</span>
            <span>提交时间：{{ formatDate(currentBlog.createTime) }}</span>
            <span v-if="currentBlog.auditTime">审核时间：{{ formatDate(currentBlog.auditTime) }}</span>
            <span v-if="currentBlog.rejectReason">拒绝原因：{{ currentBlog.rejectReason }}</span>
          </div>
        </div>
        
        <div class="detail-content">
          <div class="content-section">
            <h3>博客内容</h3>
            <div class="content-preview" v-html="currentBlog.content"></div>
          </div>
          
          <div class="action-section" v-if="currentBlog.status === 'pending'">
            <el-divider>审核操作</el-divider>
            <div class="action-buttons">
              <el-button type="success" icon="el-icon-check" @click="handleApprove(currentBlog)">
                通过审核
              </el-button>
              <el-button type="danger" icon="el-icon-close" @click="handleReject(currentBlog)">
                拒绝审核
              </el-button>
              <el-input
                v-model="rejectReason"
                placeholder="请输入拒绝原因（可选）"
                style="width: 300px; margin-left: 10px">
              </el-input>
            </div>
          </div>
        </div>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import manageVue from '../../../layouts/manage.vue'
export default {
  name: 'BlogAudit',
  layout:"manage",
  data() {
    return {
      loading: false,
      blogList: [],
      selectedBlogs: [],
      currentBlog: null,
      detailDialogVisible: false,
      rejectReason: '',
      
      filterForm: {
        status: '',
        category: '',
        dateRange: [],
        keyword: ''
      },
      
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      }
    }
  },
  
  mounted() {
    this.loadBlogData()
  },
  
  methods: {
    // 加载博客数据
    async loadBlogData() {
      this.loading = true
      try {
        // 模拟数据
        this.blogList = [
          {
            id: 1,
            title: 'Vue3 组合式 API 最佳实践',
            author: '张三',
            avatar: '',
            category: '技术',
            createTime: '2024-01-15 10:30:00',
            auditTime: null,
            viewCount: 156,
            status: 'pending',
            isTop: false,
            content: '<p>这是一篇关于Vue3组合式API的详细教程...</p>',
            rejectReason: ''
          },
          {
            id: 2,
            title: 'Spring Boot 微服务架构设计',
            author: '李四',
            avatar: '',
            category: '技术',
            createTime: '2024-01-14 14:20:00',
            auditTime: '2024-01-14 15:30:00',
            viewCount: 89,
            status: 'approved',
            isTop: true,
            content: '<p>Spring Boot微服务架构的设计原则和实践...</p>',
            rejectReason: ''
          },
          {
            id: 3,
            title: '机器学习入门指南',
            author: '王五',
            avatar: '',
            category: '学习',
            createTime: '2024-01-13 09:15:00',
            auditTime: '2024-01-13 10:20:00',
            viewCount: 234,
            status: 'rejected',
            isTop: false,
            content: '<p>机器学习的基础概念和入门方法...</p>',
            rejectReason: '内容质量不符合要求'
          }
        ]
        this.pagination.total = this.blogList.length
      } catch (error) {
        console.error('加载数据失败:', error)
        this.$message.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    // 处理选择变化
    handleSelectionChange(selection) {
      this.selectedBlogs = selection
    },
    
    // 查看博客详情
    handleView(blog) {
      this.currentBlog = blog
      this.detailDialogVisible = true
    },
    
    // 单个通过审核
    handleApprove(blog) {
      this.$confirm('确定通过该博客的审核吗？', '提示', {
        type: 'warning'
      }).then(() => {
        blog.status = 'approved'
        blog.auditTime = new Date().toISOString()
        this.$message.success('审核通过')
        this.detailDialogVisible = false
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 单个拒绝审核
    handleReject(blog) {
      this.$confirm('确定拒绝该博客的审核吗？', '提示', {
        type: 'warning'
      }).then(() => {
        blog.status = 'rejected'
        blog.auditTime = new Date().toISOString()
        blog.rejectReason = this.rejectReason
        this.$message.success('审核已拒绝')
        this.detailDialogVisible = false
        this.rejectReason = ''
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 切换置顶状态
    handleToggleTop(blog) {
      const action = blog.isTop ? '取消置顶' : '置顶'
      this.$confirm(`确定${action}该博客吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        blog.isTop = !blog.isTop
        blog.topTime = blog.isTop ? new Date().toISOString() : null
        this.$message.success(`${action}成功`)
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 删除博客
    handleDelete(blog) {
      this.$confirm('确定删除该博客吗？此操作不可恢复！', '警告', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        const index = this.blogList.findIndex(item => item.id === blog.id)
        if (index > -1) {
          this.blogList.splice(index, 1)
          this.pagination.total--
          this.$message.success('删除成功')
        }
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 批量通过审核
    handleBatchApprove() {
      this.$confirm(`确定通过选中的 ${this.selectedBlogs.length} 篇博客吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        const currentTime = new Date().toISOString()
        this.selectedBlogs.forEach(blog => {
          blog.status = 'approved'
          blog.auditTime = currentTime
        })
        this.$message.success(`已通过 ${this.selectedBlogs.length} 篇博客的审核`)
        this.selectedBlogs = []
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 批量拒绝审核
    handleBatchReject() {
      this.$confirm(`确定拒绝选中的 ${this.selectedBlogs.length} 篇博客吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        const currentTime = new Date().toISOString()
        this.selectedBlogs.forEach(blog => {
          blog.status = 'rejected'
          blog.auditTime = currentTime
        })
        this.$message.success(`已拒绝 ${this.selectedBlogs.length} 篇博客的审核`)
        this.selectedBlogs = []
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 批量置顶操作
    handleBatchTop() {
      this.$confirm(`确定置顶选中的 ${this.selectedBlogs.length} 篇博客吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        const currentTime = new Date().toISOString()
        this.selectedBlogs.forEach(blog => {
          blog.isTop = true
          blog.topTime = currentTime
        })
        this.$message.success(`已置顶 ${this.selectedBlogs.length} 篇博客`)
        this.selectedBlogs = []
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 批量取消置顶
    handleBatchCancelTop() {
      this.$confirm(`确定取消选中的 ${this.selectedBlogs.length} 篇博客的置顶吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        this.selectedBlogs.forEach(blog => {
          blog.isTop = false
          blog.topTime = null
        })
        this.$message.success(`已取消 ${this.selectedBlogs.length} 篇博客的置顶`)
        this.selectedBlogs = []
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 搜索处理
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadBlogData()
    },
    
    // 刷新数据
    refreshData() {
      this.loadBlogData()
      this.$message.success('数据已刷新')
    },
    
    // 分页大小变化
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.loadBlogData()
    },
    
    // 当前页变化
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadBlogData()
    },
    
    // 格式化日期
    formatDate(dateString) {
      if (!dateString) return ''
      return new Date(dateString).toLocaleString('zh-CN')
    },
    
    // 获取状态类型
    getStatusType(status) {
      const typeMap = {
        'pending': 'warning',
        'approved': 'success',
        'rejected': 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    // 获取状态文本
    getStatusText(status) {
      const textMap = {
        'pending': '待审核',
        'approved': '已通过',
        'rejected': '已拒绝'
      }
      return textMap[status] || '未知'
    },
    
    // 获取分类类型
    getCategoryType(category) {
      const typeMap = {
        '技术': '',
        '生活': 'success',
        '学习': 'warning',
        '其他': 'info'
      }
      return typeMap[category] || 'info'
    }
  }
}
</script>

<style scoped>
.blog-audit {
  padding: 20px;
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

.filter-left {
  display: flex;
  align-items: center;
}

.filter-right {
  display: flex;
  align-items: center;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.table-card {
  margin-bottom: 20px;
}

.blog-title {
  display: flex;
  align-items: center;
}

.title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.blog-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
}

.content-section h3 {
  margin: 20px 0 10px 0;
  color: #303133;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

.content-preview {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  background: #fafafa;
  min-height: 200px;
  max-height: 300px;
  overflow-y: auto;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.dialog-footer {
  text-align: right;
}
</style>