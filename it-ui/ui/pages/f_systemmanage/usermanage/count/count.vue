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
            <el-option label="访客" value="guest"></el-option>
            <el-option label="用户" value="user"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="超级管理员" value="super_admin"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="table-card">
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
            <el-tag v-if="scope.row.isAdmin" size="mini" type="danger">管理员</el-tag>
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
              <el-option label="访客" value="guest"></el-option>
              <el-option label="用户" value="user"></el-option>
              <el-option label="管理员" value="admin"></el-option>
              <el-option label="超级管理员" value="super_admin"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态" prop="status">
            <el-select v-model="userForm.status" placeholder="请选择状态">
              <el-option label="正常" value="active"></el-option>
              <el-option label="禁用" value="disabled"></el-option>
              <el-option label="未激活" value="inactive"></el-option>
            </el-select>
          </el-form-item>
          
          <div class="dialog-footer">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit">确定</el-button>
          </div>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Count',
  layout: 'manage',
  data() {
    return {
      loading: false,
      // 搜索表单
      searchForm: {
        username: '',
        email: '',
        status: '',
        role: ''
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
        username: '',
        email: '',
        phone: '',
        role: '',
        status: 'active'
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ]
      },
      // 模拟数据
      mockData: [
        {
          id: 1,
          username: '爆爆爆',
          email: '爆爆爆@example.com',
          phone: '13800138001',
          avatar: '',
          role: 'super_admin',
          status: 'active',
          isAdmin: true,
          createTime: '2024-01-10 09:00:00',
          lastLogin: '2024-03-10 15:30:00'
        },
        {
          id: 2,
          username: 'admin',
          email: 'admin@example.com',
          phone: '13800138000',
          avatar: '',
          role: 'admin',
          status: 'active',
          isAdmin: true,
          createTime: '2024-01-15 10:30:00',
          lastLogin: '2024-03-10 14:20:00'
        },
        {
          id: 3,
          username: 'user001',
          email: 'user001@example.com',
          phone: '13600136000',
          avatar: '',
          role: 'user',
          status: 'active',
          isAdmin: false,
          createTime: '2024-03-01 14:20:00',
          lastLogin: '2024-03-08 11:30:00'
        },
        {
          id: 4,
          username: 'guest001',
          email: 'guest001@example.com',
          phone: '13500135000',
          avatar: '',
          role: 'guest',
          status: 'active',
          isAdmin: false,
          createTime: '2024-03-05 16:45:00',
          lastLogin: '2024-03-09 09:15:00'
        }
      ]
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
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 500))
        this.userList = this.mockData
        this.pagination.total = this.mockData.length
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
      this.fetchUserList()
    },
    
    // 新增用户
    handleAddUser() {
      this.dialogType = 'add'
      this.userForm = {
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
      }).then(() => {
        this.$message.success('密码重置成功')
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
      }).then(() => {
        this.$message.success('用户已禁用')
      }).catch(() => {})
    },
    
    // 启用用户
    enableUser(user) {
      this.$confirm(`确定要启用用户 "${user.username}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$message.success('用户已启用')
      }).catch(() => {})
    },
    
    // 删除用户
    deleteUser(user) {
      this.$confirm(`确定要删除用户 "${user.username}" 吗?此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(() => {
        this.$message.success('用户已删除')
      }).catch(() => {})
    },
    
    // 表单提交
    handleSubmit() {
      this.$refs.userForm.validate((valid) => {
        if (valid) {
          if (this.dialogType === 'add') {
            // 新增用户逻辑
            const newUser = {
              id: Date.now(),
              ...this.userForm,
              avatar: '',
              isAdmin: this.userForm.role === 'admin' || this.userForm.role === 'super_admin',
              createTime: new Date().toISOString(),
              lastLogin: null
            }
            this.mockData.unshift(newUser)
            this.$message.success('用户添加成功')
          } else {
            // 编辑用户逻辑
            const index = this.mockData.findIndex(item => item.id === this.userForm.id)
            if (index !== -1) {
              this.mockData[index] = {
                ...this.mockData[index],
                username: this.userForm.username,
                email: this.userForm.email,
                phone: this.userForm.phone,
                role: this.userForm.role,
                status: this.userForm.status,
                isAdmin: this.userForm.role === 'admin' || this.userForm.role === 'super_admin'
              }
              this.$message.success('用户信息更新成功')
            }
          }
          this.dialogVisible = false
          this.fetchUserList()
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
        guest: 'info',
        user: 'success',
        admin: 'warning',
        super_admin: 'danger'
      }
      return types[role] || 'info'
    },
    
    getRoleLabel(role) {
      const labels = {
        guest: '访客',
        user: '用户',
        admin: '管理员',
        super_admin: '超级管理员'
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
  text-align: right;
}

.dialog-content {
  padding: 20px 0;
}

.dialog-footer {
  text-align: right;
  margin-top: 30px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-management {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-right {
    margin-top: 15px;
    width: 100%;
  }
  
  .header-right .el-button {
    width: 100%;
    margin-bottom: 10px;
  }
}
</style>