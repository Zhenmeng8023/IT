<template>
  <div class="user-info-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>用户信息管理</h1>
      <p>查看和管理用户的详细信息</p>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" :inline="true">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px">
          </el-input>
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input
            v-model="searchForm.email"
            placeholder="请输入邮箱"
            clearable
            style="width: 200px">
          </el-input>
        </el-form-item>
        
        <el-form-item label="手机号">
          <el-input
            v-model="searchForm.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 200px">
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button v-permission="'btn:user-info:search'" type="primary" @click="handleSearch">查询</el-button>
          <el-button v-permission="'btn:user-info:reset'" type="primary" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户信息列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="userList"
        v-loading="loading"
        stripe
        style="width: 100%">
        
        <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
        
        <el-table-column prop="avatar" label="头像" width="80" align="center">
          <template slot-scope="scope">
            <el-avatar :size="40" :src="scope.row.avatar" :alt="scope.row.username">
              {{ scope.row.username.charAt(0) }}
            </el-avatar>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="用户名" min-width="120"></el-table-column>
        
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        
        <el-table-column prop="phone" label="手机号" width="130" align="center"></el-table-column>
        
        <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
        
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.gender === '男' ? 'primary' : 'danger'">
              {{ scope.row.gender }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="birthday" label="生日" width="120" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.birthday) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="address" label="地址" min-width="200">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.address" placement="top">
              <span>{{ scope.row.address ? scope.row.address.substring(0, 15) + '...' : '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="signature" label="签名" min-width="200">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.signature" placement="top">
              <span>{{ scope.row.signature ? scope.row.signature.substring(0, 20) + '...' : '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="tags" label="标签" width="150">
          <template slot-scope="scope">
            <el-tag
              v-for="tag in scope.row.tags"
              :key="tag"
              size="mini"
              style="margin-right: 5px; margin-bottom: 5px">
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="注册时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="'btn:user-info:view'"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
            
            <el-button v-permission="'btn:user-info:edit'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
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

    <!-- 查看用户详情对话框 -->
    <el-dialog
      title="用户详细信息"
      :visible.sync="viewDialogVisible"
      width="700px">
      <div class="user-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ currentUser.gender || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ formatDate(currentUser.birthday) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ currentUser.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="签名" :span="2">{{ currentUser.signature || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag
              v-for="tag in currentUser.tags"
              :key="tag"
              size="mini"
              style="margin-right: 5px; margin-bottom: 5px">
              {{ tag }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(currentUser.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ currentUser.lastLogin ? formatDate(currentUser.lastLogin) : '从未登录' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 编辑用户信息对话框 -->
    <el-dialog
      :title="editDialogTitle"
      :visible.sync="editDialogVisible"
      width="600px">
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称"></el-input>
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="editForm.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="editForm.birthday"
            type="date"
            placeholder="选择生日"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>
        
        <el-form-item label="地址" prop="address">
          <el-input
            type="textarea"
            :rows="2"
            v-model="editForm.address"
            placeholder="请输入地址">
          </el-input>
        </el-form-item>
        
        <el-form-item label="签名" prop="signature">
          <el-input
            type="textarea"
            :rows="2"
            v-model="editForm.signature"
            placeholder="请输入签名">
          </el-input>
        </el-form-item>
        
        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="editForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入标签"
            style="width: 100%">
            <el-option
              v-for="tag in tagOptions"
              :key="tag"
              :label="tag"
              :value="tag">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { GetUsersPage, GetUserById, UpdateUser } from '@/api/index.js'

export default {
  name: 'Info',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      // 搜索表单
      searchForm: {
        username: '',
        email: '',
        phone: ''
      },
      // 用户列表数据
      userList: [],
      // 分页信息
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      // 查看对话框控制
      viewDialogVisible: false,
      // 编辑对话框控制
      editDialogVisible: false,
      editDialogType: 'edit', // 'edit' 或 'add'
      // 当前操作用户
      currentUser: {},
      // 编辑表单
      editForm: {
        id: null,
        username: '',
        nickname: '',
        email: '',
        phone: '',
        gender: '',
        birthday: '',
        address: '',
        signature: '',
        tags: []
      },
      // 编辑表单验证规则
      editRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ]
      },
      // 标签选项
      tagOptions: ['java', 'python', 'javascript', 'vue', 'react', 'nodejs', 'mysql', 'redis', 'docker', 'linux']
    }
  },
  computed: {
    editDialogTitle() {
      return this.editDialogType === 'edit' ? '编辑用户信息' : '新增用户信息'
    }
  },
  mounted() {
    this.fetchUserList()
  },
  methods: {
    // 获取用户列表
    async fetchUserList() {
      this.loading = true
      try {
        const params = {
          page: this.pagination.currentPage,
          size: this.pagination.pageSize,
          username: this.searchForm.username,
          email: this.searchForm.email,
          phone: this.searchForm.phone
        }
        console.log('请求参数:', params)
        const response = await GetUsersPage(params)
        console.log('响应数据:', response)
        this.userList = response.data?.records || []
        this.pagination.total = response.data?.total || 0
      } catch (error) {
        console.error('获取用户列表失败:', error)
        console.error('错误详情:', error.response)
        this.$message.error(`获取用户列表失败: ${error.message || '未知错误'}`)
        this.pagination.total = this.userList.length
      } finally {
        this.loading = false
      }
    },
    
    // 搜索用户
    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    
    // 重置搜索
    handleReset() {
      this.searchForm = {
        username: '',
        email: '',
        phone: ''
      }
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    
    // 查看用户详情
    async handleView(user) {
      try {
        const response = await GetUserById(user.id)
        this.currentUser = response.data
        this.viewDialogVisible = true
      } catch (error) {
        console.error('获取用户详情失败:', error)
        this.$message.error('获取用户详情失败')
      }
    },
    
    // 编辑用户信息
    handleEdit(user) {
      this.editDialogType = 'edit'
      this.currentUser = { ...user }
      this.editForm = {
        id: user.id,
        username: user.username,
        nickname: user.nickname || '',
        email: user.email,
        phone: user.phone || '',
        gender: user.gender || '',
        birthday: user.birthday || '',
        address: user.address || '',
        signature: user.signature || '',
        tags: user.tags || []
      }
      this.editDialogVisible = true
    },
    
    // 编辑表单提交
    async handleEditSubmit() {
      this.$refs.editForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            if (this.editDialogType === 'edit') {
              // 更新用户信息
              await UpdateUser(this.editForm.id, this.editForm)
              this.$message.success('用户信息更新成功')
            }
            
            this.editDialogVisible = false
            this.fetchUserList()
          } catch (error) {
            console.error('更新用户信息失败:', error)
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    
    // 分页大小改变
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    
    // 当前页改变
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchUserList()
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
    }
  }
}
</script>

<style scoped>
.user-info-management {
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

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.user-detail {
  padding: 10px;
}

.username {
  font-weight: 600;
}

.dialog-footer {
  text-align: right;
}
</style>