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
          <el-select v-model="filterForm.status" placeholder="审核状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已通过" value="approved"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
          </el-select>
          
          <!-- <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px; margin-left: 10px">
          </el-date-picker> -->
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
            <el-avatar :size="24" :src="scope.row.author?.avatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
            {{ getAuthorName(scope.row.author) }}
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
export default {
  name: 'BlogAudit',
  layout: 'manage',
  data() {
    return {
      loading: false,
      blogList: [],
      selectedBlogs: [],
      currentBlog: null,
      detailDialogVisible: false,
      rejectReason: '',

      filterForm: {
        status: 'approved',
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
    // 构建查询参数
    buildQueryParams() {
      const params = {
        page: 0, // API期望从0开始的页码
        size: this.pagination.pageSize,
        status: this.filterForm.status || undefined,
        category: this.filterForm.category || undefined,
        keyword: this.filterForm.keyword || undefined
      }

      // 处理日期范围
      if (this.filterForm.dateRange && this.filterForm.dateRange.length === 2) {
        params.startDate = this.formatDateParam(this.filterForm.dateRange[0])
        params.endDate = this.formatDateParam(this.filterForm.dateRange[1])
      }

      return params
    },

    // 格式化日期为 YYYY-MM-DD
    formatDateParam(date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`
    },

    // 修改 loadBlogData 方法，添加对对象响应的处理
async loadBlogData() {
  this.loading = true
  try {
    const params = this.buildQueryParams()
    console.log('请求参数:', params)
    console.log('筛选状态:', this.filterForm.status)
    
     // 根据状态选择不同的API端点
    // 根据状态选择不同的API端点
    let apiUrl = '/api/blogs'
    if (this.filterForm.status === 'rejected') {
      apiUrl = '/api/blogs/rejected'
    } else if (this.filterForm.status === 'pending') {
      apiUrl = '/api/blogs/pending'
    } else if (this.filterForm.status === 'approved') {
      apiUrl = '/api/blogs'
    }
    
    console.log('API端点:', apiUrl)
    const response = await this.$axios.get(apiUrl, { params })
    console.log('API响应:', response)

    // 检查响应格式
    if (response) {
      // 检查是否是直接的数组响应
      if (Array.isArray(response)) {
        // 格式: [{...}, {...}]
        this.blogList = response
        this.pagination.total = response.length
        console.log('使用数组响应格式，数量:', response.length)
      } else if (Array.isArray(response.data)) {
        // 格式: { data: [{...}, {...}] }
        this.blogList = response.data
        this.pagination.total = response.data.length
        console.log('使用data数组响应格式，数量:', response.data.length)
      } else if (response.code === 200) {
        // 格式1: { code: 200, list: [], total: 100 }
        this.blogList = response.list || []
        this.pagination.total = response.total || 0
        console.log('使用响应格式1，数量:', response.list?.length || 0)
      } else if (response.data?.code === 200) {
        // 格式2: { data: { code: 200, list: [], total: 100 } }
        this.blogList = response.data.list || []
        this.pagination.total = response.data.total || 0
        console.log('使用响应格式2，数量:', response.data.list?.length || 0)
      } else if (response.content) {
        // 分页响应格式: { content: [...], totalElements: 50, ... }
        this.blogList = Array.isArray(response.content) ? response.content : []
        this.pagination.total = response.totalElements || 0
        console.log('使用分页响应格式，数量:', this.blogList.length, '总数量:', this.pagination.total)
      } else if (response.data?.content) {
        // 包装在data中的分页响应格式: { data: { content: [...], totalElements: 50, ... } }
        this.blogList = Array.isArray(response.data.content) ? response.data.content : []
        this.pagination.total = response.data.totalElements || 0
        console.log('使用data包装的分页响应格式，数量:', this.blogList.length, '总数量:', this.pagination.total)
      } else if (typeof response === 'object') {
        // 其他对象格式
        this.blogList = []
        this.pagination.total = 0
        console.log('对象响应格式不符合预期，设置为空数组')
      } else {
        // 其他格式
        console.log('响应格式不符合预期:', response)
        this.$message.error('加载失败')
      }
    } else {
      console.log('响应为空:', response)
      this.$message.error('加载失败')
    }
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

    // 查看博客详情（可选：单独加载详情）
    async handleView(blog) {
      // 如果列表数据中已经包含完整内容，可以直接使用
      // 否则可调用详情接口获取最新数据
      try {
        const response = await this.$axios.get(`/api/blogs/${blog.id}`)
        console.log('查看博客详情响应:', response)
        if (response) {
          this.currentBlog = response.data || response
          this.detailDialogVisible = true
        } else {
          this.$message.error('获取详情失败')
        }
      } catch (error) {
        console.error('获取详情失败:', error)
        this.$message.error('获取详情失败')
      }
    },

    // 单个通过审核
    async handleApprove(blog) {
      try {
        await this.$confirm('确定通过该博客的审核吗？', '提示', { type: 'warning' })
        const response = await this.$axios.put(`/api/blogs/${blog.id}/approve`)
        console.log('通过审核响应:', response)
        if (response) {
          this.$message.success('审核通过')
          // 更新本地数据
          blog.status = 'published'
          blog.auditTime = new Date().toISOString()
          this.detailDialogVisible = false
          // 刷新列表
          this.loadBlogData()
        } else {
          this.$message.error('操作失败')
        }
      } catch (error) {
        // 用户取消或请求失败
        if (error !== 'cancel') {
          console.error('审核通过失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 单个拒绝审核
    async handleReject(blog) {
      try {
        await this.$confirm('确定拒绝该博客的审核吗？', '提示', { type: 'warning' })
        const response = await this.$axios.put(`/api/blogs/${blog.id}/reject`, {
          reason: this.rejectReason
        })
        console.log('拒绝审核响应:', response)
        if (response) {
          this.$message.success('审核已拒绝')
          blog.status = 'rejected'
          blog.auditTime = new Date().toISOString()
          blog.rejectReason = this.rejectReason
          this.detailDialogVisible = false
          this.rejectReason = ''
          // 刷新列表
          this.loadBlogData()
        } else {
          this.$message.error('操作失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('拒绝失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 切换置顶状态（单个）
    async handleToggleTop(blog) {
      const action = blog.isTop ? '取消置顶' : '置顶'
      try {
        await this.$confirm(`确定${action}该博客吗？`, '提示', { type: 'warning' })
        const response = await this.$axios.put(`/api/admin/posts/${blog.id}/top`, {
          isTop: !blog.isTop
        })
        if (response.data.code === 200) {
          blog.isTop = !blog.isTop
          blog.topTime = blog.isTop ? new Date().toISOString() : null
          this.$message.success(`${action}成功`)
        } else {
          this.$message.error(response.data.message || '操作失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('置顶操作失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 删除博客（单个）
    async handleDelete(blog) {
      try {
        await this.$confirm('确定删除该博客吗？此操作不可恢复！', '警告', {
          type: 'warning',
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          confirmButtonClass: 'el-button--danger'
        })
        const response = await this.$axios.delete(`/api/blogs/${blog.id}`)
        console.log('删除博客响应:', response)
        if (response) {
          this.$message.success('删除成功')
          // 从列表中移除
          const index = this.blogList.findIndex(item => item.id === blog.id)
          if (index > -1) {
            this.blogList.splice(index, 1)
            this.pagination.total--
          }
        } else {
          this.$message.error('操作失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 批量通过审核
    async handleBatchApprove() {
      if (this.selectedBlogs.length === 0) return
      try {
        await this.$confirm(`确定通过选中的 ${this.selectedBlogs.length} 篇博客吗？`, '提示', { type: 'warning' })
        const blogIds = this.selectedBlogs.map(blog => blog.id)
        console.log('批量通过请求:', { blogIds, status: 'published' })
        const response = await this.$axios.put('/api/blogs/batch', {
          blogIds,
          status: 'published'
        })
        console.log('批量通过响应:', response)
        // API返回200 OK，无响应体
        this.$message.success(`已通过 ${this.selectedBlogs.length} 篇博客的审核`)
        // 刷新列表
        this.loadBlogData()
        this.selectedBlogs = []
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量通过失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 批量拒绝审核
    async handleBatchReject() {
      if (this.selectedBlogs.length === 0) return
      try {
        // 弹出输入拒绝原因的对话框
        await this.$prompt('请输入拒绝原因', '批量拒绝', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputPlaceholder: '请输入拒绝原因',
          inputValidator: (value) => {
            if (!value || value.trim() === '') {
              return '拒绝原因不能为空'
            }
            return true
          }
        }).then(async (result) => {
          const blogIds = this.selectedBlogs.map(blog => blog.id)
          console.log('批量拒绝请求:', { blogIds, status: 'rejected', reason: result.value })
          const response = await this.$axios.put('/api/blogs/batch', {
            blogIds,
            status: 'rejected',
            reason: result.value
          })
          console.log('批量拒绝响应:', response)
          // API返回200 OK，无响应体
          this.$message.success(`已拒绝 ${this.selectedBlogs.length} 篇博客的审核`)
          // 刷新列表
          this.loadBlogData()
          this.selectedBlogs = []
        })
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量拒绝失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 批量置顶
    async handleBatchTop() {
      if (this.selectedBlogs.length === 0) return
      try {
        await this.$confirm(`确定置顶选中的 ${this.selectedBlogs.length} 篇博客吗？`, '提示', { type: 'warning' })
        const ids = this.selectedBlogs.map(blog => blog.id)
        const response = await this.$axios.post('/api/admin/posts/batch-top', { ids, isTop: true })
        if (response.data.code === 200) {
          this.$message.success(`已置顶 ${this.selectedBlogs.length} 篇博客`)
          this.loadBlogData()
          this.selectedBlogs = []
        } else {
          this.$message.error(response.data.message || '操作失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量置顶失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 批量取消置顶
    async handleBatchCancelTop() {
      if (this.selectedBlogs.length === 0) return
      try {
        await this.$confirm(`确定取消选中的 ${this.selectedBlogs.length} 篇博客的置顶吗？`, '提示', { type: 'warning' })
        const ids = this.selectedBlogs.map(blog => blog.id)
        const response = await this.$axios.post('/api/admin/posts/batch-top', { ids, isTop: false })
        if (response.data.code === 200) {
          this.$message.success(`已取消 ${this.selectedBlogs.length} 篇博客的置顶`)
          this.loadBlogData()
          this.selectedBlogs = []
        } else {
          this.$message.error(response.data.message || '操作失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量取消置顶失败:', error)
          this.$message.error('操作失败')
        }
      }
    },

    // 搜索处理（防抖可自行添加）
    handleSearch() {
      console.log('搜索处理，状态:', this.filterForm.status)
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

    // 导出数据（调用导出接口）
    async handleExport() {
      try {
        const params = this.buildQueryParams()
        // 使用 GET 请求导出文件
        const response = await this.$axios.get('/api/blogs/export', {
          params,
          responseType: 'blob' // 关键：接收二进制数据
        })
        // 创建下载链接
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', '博客审核数据.xlsx') // 文件名
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (error) {
        console.error('导出失败:', error)
        this.$message.error('导出失败')
      }
    },

    // 格式化日期显示
    formatDate(dateString) {
      if (!dateString) return ''
      return new Date(dateString).toLocaleString('zh-CN')
    },

    // 获取状态类型
    getStatusType(status) {
      const typeMap = {
        pending: 'warning',
        approved: 'success',
        rejected: 'danger',
        published: 'success' // 映射 published 到 success 类型
      }
      return typeMap[status] || 'info'
    },

    // 获取状态文本
    getStatusText(status) {
      const textMap = {
        pending: '待审核',
        approved: '已通过',
        rejected: '已拒绝',
        published: '已通过' // 映射 published 到 已通过 文本
      }
      return textMap[status] || '未知'
    },

    // 获取分类类型
    getCategoryType(category) {
      const typeMap = {
        技术: '',
        生活: 'success',
        学习: 'warning',
        其他: 'info'
      }
      return typeMap[category] || 'info'
    },

    // 获取作者名称
    getAuthorName(author) {
      if (!author) return '未知作者'
      return author.nickname || author.displayName || author.username || '未知作者'
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