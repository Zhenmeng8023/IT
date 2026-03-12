<template>
  <div class="official-manager">
    <!-- 顶部搜索和操作栏 -->
    <div class="header">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索帖子、用户或内容"
          prefix-icon="el-icon-search"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        ></el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <div class="actions">
        <el-button type="primary" @click="handleAddPost">发布公告</el-button>
        <el-button @click="handleBatchDelete" :disabled="selectedPosts.length === 0">批量删除</el-button>
        <el-button @click="handleExport">导出数据</el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filters">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable>
            <el-option label="全部" value=""></el-option>
            <el-option label="正常" value="normal"></el-option>
            <el-option label="置顶" value="top"></el-option>
            <el-option label="精华" value="essence"></el-option>
            <el-option label="已删除" value="deleted"></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterCategory" placeholder="分类筛选" clearable>
            <el-option label="全部" value=""></el-option>
            <el-option label="公告" value="announcement"></el-option>
            <el-option label="讨论" value="discussion"></el-option>
            <el-option label="问答" value="qa"></el-option>
            <el-option label="分享" value="share"></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
          ></el-date-picker>
        </el-col>
        <el-col :span="6">
          <el-button @click="resetFilters">重置筛选</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 帖子列表 -->
    <div class="post-list">
      <el-table
        :data="filteredPosts"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column label="标题" min-width="200">
          <template slot-scope="scope">
            <div class="post-title">
              <span v-if="scope.row.isTop" class="top-tag">置顶</span>
              <span v-if="scope.row.isEssence" class="essence-tag">精华</span>
              <el-link type="primary" @click="handleViewPost(scope.row)">{{ scope.row.title }}</el-link>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120"></el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template slot-scope="scope">
            <el-tag :type="getCategoryType(scope.row.category)">{{ getCategoryLabel(scope.row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="replyCount" label="回复数" width="80" sortable></el-table-column>
        <el-table-column prop="viewCount" label="浏览数" width="80" sortable></el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="160" sortable></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="scope">
            <div class="action-buttons">
              <el-button 
                size="mini" 
                type="primary" 
                icon="el-icon-edit" 
                @click="handleEdit(scope.row)"
                title="编辑帖子"
              >编辑</el-button>
              <el-button 
                size="mini" 
                :type="scope.row.isTop ? 'warning' : 'default'" 
                :icon="scope.row.isTop ? 'el-icon-top' : 'el-icon-top'"
                @click="handleToggleTop(scope.row)"
                :title="scope.row.isTop ? '取消置顶' : '置顶'"
              >
                {{ scope.row.isTop ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button 
                size="mini" 
                :type="scope.row.isEssence ? 'success' : 'default'" 
                :icon="scope.row.isEssence ? 'el-icon-star-on' : 'el-icon-star-off'"
                @click="handleToggleEssence(scope.row)"
                :title="scope.row.isEssence ? '取消精华' : '设置精华'"
              >
                {{ scope.row.isEssence ? '取消精华' : '精华' }}
              </el-button>
              <el-button 
                size="mini" 
                type="danger" 
                icon="el-icon-delete" 
                @click="handleDelete(scope.row)"
                title="删除帖子"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      ></el-pagination>
    </div>

    <!-- 帖子详情对话框 -->
    <el-dialog
      title="帖子详情"
      :visible.sync="postDetailVisible"
      width="80%"
      :before-close="handleCloseDetail"
    >
      <div class="post-detail" v-if="currentPost">
        <div class="post-header">
          <h2>{{ currentPost.title }}</h2>
          <div class="post-meta">
            <span>作者：{{ currentPost.author }}</span>
            <span>发布时间：{{ currentPost.createTime }}</span>
            <span>浏览：{{ currentPost.viewCount }}</span>
            <span>回复：{{ currentPost.replyCount }}</span>
          </div>
        </div>
        <div class="post-content">
          <div v-html="currentPost.content"></div>
        </div>
        <div class="post-actions">
          <el-button @click="postDetailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleEdit(currentPost)">编辑</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 编辑帖子对话框 -->
    <el-dialog
      :title="isEditing ? '编辑帖子' : '发布公告'"
      :visible.sync="editDialogVisible"
      width="60%"
    >
      <div class="post-editor" v-if="editDialogVisible">
        <el-form ref="form" :model="formData" :rules="rules" label-width="80px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="formData.title" placeholder="请输入标题"></el-input>
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="formData.category" placeholder="请选择分类">
              <el-option label="公告" value="announcement"></el-option>
              <el-option label="讨论" value="discussion"></el-option>
              <el-option label="问答" value="qa"></el-option>
              <el-option label="分享" value="share"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input
              type="textarea"
              v-model="formData.content"
              :rows="10"
              placeholder="请输入内容"
            ></el-input>
          </el-form-item>
          <el-form-item label="置顶">
            <el-switch v-model="formData.isTop"></el-switch>
          </el-form-item>
          <el-form-item label="精华">
            <el-switch v-model="formData.isEssence"></el-switch>
          </el-form-item>
        </el-form>
        <div class="editor-actions">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitPost" :loading="submitting">提交</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'OfficialManager',
  layout: "manage",
  data() {
    return {
      // 搜索和筛选
      searchKeyword: '',
      filterStatus: '',
      filterCategory: '',
      dateRange: [],
      
      // 分页
      currentPage: 1,
      pageSize: 20,
      total: 0,
      
      // 数据
      posts: [],
      filteredPosts: [],
      selectedPosts: [],
      currentPost: null,
      formData: {
        title: '',
        category: 'announcement',
        content: '',
        isTop: false,
        isEssence: false
      },
      
      // 状态控制
      loading: false,
      postDetailVisible: false,
      editDialogVisible: false,
      isEditing: false,
      submitting: false,
      
      // 表单验证规则
      rules: {
        title: [
          { required: true, message: '请输入标题', trigger: 'blur' },
          { min: 1, max: 100, message: '标题长度在 1 到 100 个字符', trigger: 'blur' }
        ],
        category: [
          { required: true, message: '请选择分类', trigger: 'change' }
        ],
        content: [
          { required: true, message: '请输入内容', trigger: 'blur' },
          { min: 1, message: '内容不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.loadPosts()
  },
  methods: {
    // 加载帖子列表
    async loadPosts() {
      this.loading = true
      try {
        // 模拟数据
        this.posts = [
          {
            id: 1,
            title: '系统维护公告',
            author: '管理员',
            category: 'announcement',
            content: '<p>系统将于今晚进行维护，预计耗时2小时。</p>',
            replyCount: 15,
            viewCount: 256,
            createTime: '2024-01-15 10:30:00',
            status: 'normal',
            isTop: true,
            isEssence: false
          },
          {
            id: 2,
            title: '关于新版贴吧的讨论',
            author: '用户A',
            category: 'discussion',
            content: '<p>大家对新版贴吧有什么建议？</p>',
            replyCount: 42,
            viewCount: 589,
            createTime: '2024-01-14 15:20:00',
            status: 'normal',
            isTop: false,
            isEssence: true
          },
          {
            id: 3,
            title: '技术问题求助',
            author: '用户B',
            category: 'qa',
            content: '<p>遇到一个技术问题，求大神解答。</p>',
            replyCount: 8,
            viewCount: 123,
            createTime: '2024-01-13 09:15:00',
            status: 'normal',
            isTop: false,
            isEssence: false
          },
          {
            id: 4,
            title: '资源分享：前端学习资料',
            author: '用户C',
            category: 'share',
            content: '<p>分享一些前端学习资料，希望对大家有帮助。</p>',
            replyCount: 25,
            viewCount: 345,
            createTime: '2024-01-12 14:30:00',
            status: 'normal',
            isTop: false,
            isEssence: false
          }
        ]
        this.total = this.posts.length
        this.applyFilters()
      } catch (error) {
        console.error('加载帖子失败:', error)
        this.$message.error('加载帖子失败')
      } finally {
        this.loading = false
      }
    },

    // 应用筛选条件
    applyFilters() {
      let filtered = [...this.posts]
      
      // 状态筛选
      if (this.filterStatus) {
        filtered = filtered.filter(post => post.status === this.filterStatus)
      }
      
      // 分类筛选
      if (this.filterCategory) {
        filtered = filtered.filter(post => post.category === this.filterCategory)
      }
      
      // 日期筛选
      if (this.dateRange && this.dateRange.length === 2) {
        filtered = filtered.filter(post => {
          const postDate = new Date(post.createTime)
          const startDate = new Date(this.dateRange[0])
          const endDate = new Date(this.dateRange[1])
          return postDate >= startDate && postDate <= endDate
        })
      }
      
      this.filteredPosts = filtered
    },

    // 搜索处理
    handleSearch() {
      this.currentPage = 1
      this.loadPosts()
    },

    // 重置筛选
    resetFilters() {
      this.searchKeyword = ''
      this.filterStatus = ''
      this.filterCategory = ''
      this.dateRange = []
      this.currentPage = 1
      this.loadPosts()
    },

    // 表格选择变化
    handleSelectionChange(selection) {
      this.selectedPosts = selection
    },

    // 查看帖子详情
    handleViewPost(post) {
      this.currentPost = post
      this.postDetailVisible = true
    },

    // 发布新帖子
    handleAddPost() {
      this.formData = {
        title: '',
        category: 'announcement',
        content: '',
        isTop: false,
        isEssence: false
      }
      this.isEditing = false
      this.editDialogVisible = true
    },

    // 编辑帖子
    handleEdit(post) {
      this.currentPost = post
      this.formData = { ...post }
      this.isEditing = true
      this.editDialogVisible = true
      this.postDetailVisible = false
    },

    // 提交帖子（新增或编辑）
    async handleSubmitPost() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          this.submitting = true
          try {
            if (this.isEditing) {
              // 更新帖子逻辑
              const index = this.posts.findIndex(p => p.id === this.currentPost.id)
              if (index !== -1) {
                this.posts[index] = { ...this.formData, id: this.currentPost.id }
              }
              this.$message.success('更新成功')
            } else {
              // 创建新帖子逻辑
              const newPost = {
                ...this.formData,
                id: Math.max(...this.posts.map(p => p.id)) + 1,
                author: '当前用户',
                replyCount: 0,
                viewCount: 0,
                createTime: new Date().toLocaleString(),
                status: 'normal'
              }
              this.posts.unshift(newPost)
              this.$message.success('发布成功')
            }
            this.editDialogVisible = false
            this.loadPosts()
          } catch (error) {
            console.error('操作失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitting = false
          }
        }
      })
    },

    // 置顶/取消置顶
    async handleToggleTop(post) {
      try {
        post.isTop = !post.isTop
        this.$message.success(post.isTop ? '置顶成功' : '取消置顶成功')
        this.loadPosts()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      }
    },

    // 精华/取消精华
    async handleToggleEssence(post) {
      try {
        post.isEssence = !post.isEssence
        this.$message.success(post.isEssence ? '设置精华成功' : '取消精华成功')
        this.loadPosts()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      }
    },

    // 删除帖子
    async handleDelete(post) {
      try {
        await this.$confirm('确定要删除这个帖子吗？', '提示', {
          type: 'warning'
        })
        this.posts = this.posts.filter(p => p.id !== post.id)
        this.$message.success('删除成功')
        this.loadPosts()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }
    },

    // 批量删除
    async handleBatchDelete() {
      try {
        await this.$confirm(`确定要删除选中的 ${this.selectedPosts.length} 个帖子吗？`, '提示', {
          type: 'warning'
        })
        const ids = this.selectedPosts.map(post => post.id)
        this.posts = this.posts.filter(p => !ids.includes(p.id))
        this.$message.success('批量删除成功')
        this.selectedPosts = []
        this.loadPosts()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          this.$message.error('批量删除失败')
        }
      }
    },

    // 导出数据
    async handleExport() {
      try {
        this.$message.success('导出功能开发中')
      } catch (error) {
        console.error('导出失败:', error)
        this.$message.error('导出失败')
      }
    },

    // 分页处理
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.loadPosts()
    },

    handleCurrentChange(page) {
      this.currentPage = page
      this.loadPosts()
    },

    // 关闭详情对话框
    handleCloseDetail() {
      this.postDetailVisible = false
    },

    // 工具方法
    getCategoryType(category) {
      const types = {
        announcement: 'primary',
        discussion: 'success',
        qa: 'warning',
        share: 'info'
      }
      return types[category] || 'default'
    },

    getCategoryLabel(category) {
      const labels = {
        announcement: '公告',
        discussion: '讨论',
        qa: '问答',
        share: '分享'
      }
      return labels[category] || category
    },

    getStatusType(status) {
      const types = {
        normal: 'success',
        top: 'warning',
        essence: 'danger',
        deleted: 'info'
      }
      return types[status] || 'default'
    },

    getStatusLabel(status) {
      const labels = {
        normal: '正常',
        top: '置顶',
        essence: '精华',
        deleted: '已删除'
      }
      return labels[status] || status
    }
  }
}
</script>

<style scoped>
.official-manager {
  padding: 20px;
  background-color: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-bar .el-input {
  width: 300px;
}

.filters {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.post-list {
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.pagination {
  margin-top: 20px;
  text-align: center;
}

.post-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-tag {
  background: #e6a23c;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.essence-tag {
  background: #f56c6c;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 10px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.action-buttons .el-button {
  margin: 1px;
  padding: 5px 8px;
  font-size: 12px;
  min-width: auto;
  flex-shrink: 0;
}

.action-buttons .el-button + .el-button {
  margin-left: 0;
}

.post-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.post-header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.post-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.post-meta {
  display: flex;
  gap: 20px;
  color: #909399;
  font-size: 14px;
}

.post-content {
  line-height: 1.6;
  margin-bottom: 20px;
}

.post-actions {
  text-align: right;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.post-editor {
  padding: 20px 0;
}

.editor-actions {
  text-align: right;
  margin-top: 20px;
}
</style>