<template>
  <div class="user-management">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h1>账户管理</h1>
        <p>管理系统用户账户信息</p>
      </div>
      <div class="header-right">
        <el-button type="primary" icon="el-icon-plus" @click="handleAddUser">
          新增用户
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" ref="searchForm" :inline="true">
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
        
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="active"></el-option>
            <el-option label="禁用" value="disabled"></el-option>
            <el-option label="未激活" value="inactive"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
            <el-option label="超级管理员" value="super_admin"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="审查员" value="reviewer"></el-option>
            <el-option label="用户" value="user"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
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
        
        <el-table-column prop="username" label="用户名" min-width="120">
          <template slot-scope="scope">
            <span class="username">{{ scope.row.username }}</span>
            <el-tag v-if="scope.row.role === 'admin' || scope.row.role === 'super_admin'" 
                   size="mini" type="danger">管理员</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        
        <el-table-column prop="phone" label="手机号" width="130" align="center"></el-table-column>
        
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getRoleType(scope.row.role)">
              {{ getRoleLabel(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" effect="dark">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="注册时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="lastLogin" label="最后登录" width="160" align="center">
          <template slot-scope="scope">
            {{ scope.row.lastLogin ? formatDate(scope.row.lastLogin) : '从未登录' }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-key"
              @click="handleResetPassword(scope.row)">
              重置密码
            </el-button>
            
            <el-dropdown @command="handleCommand($event, scope.row)">
              <el-button size="mini" type="text">
                更多<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="disable" v-if="scope.row.status === 'active'">
                  禁用账户
                </el-dropdown-item>
                <el-dropdown-item command="enable" v-if="scope.row.status === 'disabled'">
                  启用账户
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  删除账户
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
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

    <!-- 新增/编辑用户对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="600px"
      @close="handleDialogClose">
      <div class="dialog-content">
        <el-form ref="userForm" :model="userForm" :rules="rules" label-width="80px">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" placeholder="请输入邮箱"></el-input>
          </el-form-item>
          
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" placeholder="请输入手机号"></el-input>
          </el-form-item>
          
          <el-form-item label="角色" prop="role">
            <el-select v-model="userForm.role" placeholder="请选择角色">
              <el-option label="超级管理员" value="super_admin"></el-option>
              <el-option label="管理员" value="admin"></el-option>
              <el-option label="审查员" value="reviewer"></el-option>
              <el-option label="用户" value="user"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态" prop="status">
            <el-select v-model="userForm.status" placeholder="请选择状态">
              <el-option label="正常" value="active"></el-option>
              <el-option label="禁用" value="disabled"></el-option>
              <el-option label="未激活" value="inactive"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="地区" prop="region">
            <el-select v-model="userForm.region" placeholder="请选择地区">
              <el-option label="北京" value="beijing"></el-option>
              <el-option label="上海" value="shanghai"></el-option>
              <el-option label="广州" value="guangzhou"></el-option>
              <el-option label="深圳" value="shenzhen"></el-option>
              <el-option label="杭州" value="hangzhou"></el-option>
              <el-option label="其他" value="other"></el-option>
            </el-select>
          </el-form-item>
          
          <div class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
          </div>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
   GetUsersPage,
   CreateUser,
   UpdateUser,
   DeleteUser
 } from '@/api/index'
export default {
  name: 'Count',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      // 搜索表单
      searchForm: {
        username: '',
        email: '',
        status: '',
        role: '',
        region: ''
      },
      // 用户列表数据
      userList: [],
      // 分页信息
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      // 对话框控制
      dialogVisible: false,
      dialogType: 'add', // 'add' 或 'edit'
      userForm: {
        id: null,
        username: '',
        email: '',
        phone: '',
        role: '',
        status: 'active',
        region: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ],
        region: [
          { required: true, message: '请选择地区', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogType === 'add' ? '新增用户' : '编辑用户'
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
        const response = await this.$axios.get('/api/users', {
          params: {
            page: this.pagination.currentPage,
            size: this.pagination.pageSize,
            ...this.searchForm
          }
        })
        
        if (response.data.success) {
          this.userList = response.data.data.list
          this.pagination.total = response.data.data.total
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败')
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
        status: '',
        role: ''
      }
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    
    // 刷新数据
    refreshData() {
      this.$message.success('数据刷新成功')
      this.fetchUserList()      //连接到后端api可以使用
    },
    
    // 新增用户
    handleAddUser() {
      this.dialogType = 'add'
      this.userForm = {
        id: null,
        username: '',
        email: '',
        phone: '',
        role: '',
        status: 'active'
      }
      this.dialogVisible = true
    },
    
    // 编辑用户
    handleEdit(user) {
      this.dialogType = 'edit'
      this.userForm = { 
        id: user.id,
        username: user.username,
        email: user.email,
        phone: user.phone,
        role: user.role,
        status: user.status
      }
      this.dialogVisible = true
    },
    
    // 重置密码
    handleResetPassword(user) {
      this.$confirm(`确定要重置用户 "${user.username}" 的密码吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await this.$axios.post(`/api/users/${user.id}/reset-password`)
          if (response.data.success) {
            this.$message.success('密码重置成功，新密码为：123456')
          } else {
            this.$message.error(response.data.message)
          }
        } catch (error) {
          this.$message.error('密码重置失败')
        }
      }).catch(() => {})
    },
    
    // 更多操作
    handleCommand(command, user) {
      switch (command) {
        case 'disable':
          this.disableUser(user)
          break
        case 'enable':
          this.enableUser(user)
          break
        case 'delete':
          this.deleteUser(user)
          break
      }
    },
    
    // 禁用用户
    disableUser(user) {
      this.$confirm(`确定要禁用用户 "${user.username}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await DeleteUser(user.id, {
            ...user,
            status: 'disabled'
          })
          if (response.data.success) {
            this.$message.success('用户已禁用')
            this.fetchUserList()
          } else {
            this.$message.error(response.data.message)
          }
        } catch (error) {
          this.$message.error('禁用用户失败')
        }
      }).catch(() => {})
    },
    
    // 启用用户
    enableUser(user) {
      this.$confirm(`确定要启用用户 "${user.username}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await DeleteUser(user.id, {
            ...user,
            status: 'active'
          })
          if (response.data.success) {
            this.$message.success('用户已启用')
            this.fetchUserList()
          } else {
            this.$message.error(response.data.message)
          }
        } catch (error) {
          this.$message.error('启用用户失败')
        }
      }).catch(() => {})
    },
    
    // 删除用户
    deleteUser(user) {
      this.$confirm(`确定要删除用户 "${user.username}" 吗?此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          const response = await this.$axios.delete(`/api/users/${user.id}`)
          if (response.data.success) {
            this.$message.success('用户已删除')
            this.fetchUserList()
          } else {
            this.$message.error(response.data.message)
          }
        } catch (error) {
          this.$message.error('删除用户失败')
        }
      }).catch(() => {})
    },
    
    // 表单提交
    async handleSubmit() {
      this.$refs.userForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            let response
            if (this.dialogType === 'add') {
              // 新增用户
              response = await this.$axios.post('/api/users', this.userForm)
            } else {
              // 编辑用户
              response = await this.$axios.put(`/api/users/${this.userForm.id}`, this.userForm)
            }
            
            if (response.data.success) {
              this.$message.success(response.data.message)
              this.dialogVisible = false
              this.fetchUserList()
            } else {
              this.$message.error(response.data.message)
            }
          } catch (error) {
            this.$message.error(this.dialogType === 'add' ? '添加用户失败' : '更新用户信息失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    
    // 对话框关闭
    handleDialogClose() {
      this.$refs.userForm.clearValidate()
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
    
    // 工具方法
    getRoleType(role) {
      const types = {
        user: 'success',
        admin: 'warning',
        super_admin: 'danger',
        reviewer: 'success'
      }
      return types[role] || 'info'
    },
    
    getRoleLabel(role) {
      const labels = {
        user: '用户',
        admin: '管理员',
        super_admin: '超级管理员',
        reviewer: '审查员'
      }
      return labels[role] || role
    },
    
    getStatusType(status) {
      const types = {
        active: 'success',
        disabled: 'danger',
        inactive: 'warning'
      }
      return types[status] || 'info'
    },
    
    getStatusLabel(status) {
      const labels = {
        active: '正常',
        disabled: '禁用',
        inactive: '未激活'
      }
      return labels[status] || status
    },
    
    formatDate(dateString) {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h1 {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px 0;
}

.header-left p {
  color: #909399;
  margin: 0;
  font-size: 14px;
}

.search-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.username {
  font-weight: 500;
  margin-right: 8px;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.dialog-footer {
  text-align: right;
  margin-top: 20px;
}
</style>