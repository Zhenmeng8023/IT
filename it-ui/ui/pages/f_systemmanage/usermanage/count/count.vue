<template>
  <div class="user-management">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h1>账户管理</h1>
        <p>管理系统用户账户信息</p>
      </div>
      <div class="header-right">
        <el-button v-permission="'btn:user:create'" type="primary" icon="el-icon-plus" @click="handleAddUser">
          新增用户
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
      </div>
    </div>



    <!-- 用户列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="userList"
        v-loading="loading"
        stripe
        style="width: 100%">
        
        <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
        
        <el-table-column prop="avatarUrl" label="头像" width="80" align="center">
          <template slot-scope="scope">
            <el-avatar :size="40" :src="getAvatarUrl(scope.row.avatarUrl)" :alt="scope.row.username">
              {{ scope.row.username ? scope.row.username.charAt(0).toUpperCase() : '用' }}
            </el-avatar>
          </template>
        </el-table-column>
        
        <el-table-column prop="username" label="用户名" min-width="120">
          <template slot-scope="scope">
            <span class="username">{{ scope.row.username }}</span>
            <el-tag v-if="scope.row.roleId === 1 || scope.row.roleId === 2" 
                   size="mini" type="danger">管理员</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        
        <el-table-column prop="phone" label="手机号" width="130" align="center"></el-table-column>
        
        <el-table-column prop="roleId" label="角色" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getRoleType(scope.row.roleId)">
              {{ getRoleLabel(scope.row.roleId) }}
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
        
        <!-- <el-table-column prop="created_at" label="注册时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.created_at) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="last_login_at" label="最后登录" width="160" align="center">
          <template slot-scope="scope">
            {{ scope.row.last_login_at ? formatDate(scope.row.last_login_at) : '从未登录' }}
          </template>
        </el-table-column> -->
        
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="'btn:user:edit'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>
            
            <el-button v-permission="'btn:user:delete'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="deleteUser(scope.row)">
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
          
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="userForm.nickname" placeholder="请输入昵称"></el-input>
          </el-form-item>
          
          <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
            <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password></el-input>
          </el-form-item>
          
          <el-form-item label="角色" prop="roleId">
            <el-select v-model="userForm.roleId" placeholder="请选择角色">
              <el-option :label="'超级管理员'" :value="1"></el-option>
              <el-option :label="'管理员'" :value="2"></el-option>
              <el-option :label="'审查员'" :value="3"></el-option>
              <el-option :label="'用户'" :value="4"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态" prop="status">
            <el-select v-model="userForm.status" placeholder="请选择状态">
              <el-option label="正常" value="active"></el-option>
              <el-option label="禁用" value="disabled"></el-option>
              <el-option label="未激活" value="inactive"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="性别" prop="gender">
            <el-select v-model="userForm.gender" placeholder="请选择性别">
              <el-option label="男" value="1"></el-option>
              <el-option label="女" value="2"></el-option>
              <el-option label="未知" value="0"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="生日" prop="birthday">
            <el-date-picker
              v-model="userForm.birthday"
              type="date"
              placeholder="选择日期"
              format="yyyy-MM-dd"
              value-format="yyyy-MM-dd">
            </el-date-picker>
          </el-form-item>
          
          <el-form-item label="身份证" prop="identityCard">
            <el-input v-model="userForm.identityCard" placeholder="请输入身份证号"></el-input>
          </el-form-item>
          
          <el-form-item label="个人简介" prop="bio">
            <el-input v-model="userForm.bio" type="textarea" :rows="3" placeholder="请输入个人简介"></el-input>
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
  name: 'UserManagement',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,

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
      dialogType: 'add',
      userForm: {
        id: null,
        username: '',
        password: '',
        email: '',
        phone: '',
        nickname: '',
        gender: '0',  // 默认未知
        birthday: '',
        status: 'active',
        identityCard: '',
        bio: '',
        roleId: 4,
        avatarUrl: '',
        loginCount: 0
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ],
        roleId: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
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
    // 处理头像URL
    getAvatarUrl(url) {
      if (!url) return ''
      // 如果是本地文件路径，使用默认头像或空
      if (url.startsWith('file://')) {
        return ''  // 返回空字符串，让el-avatar显示文字头像
      }
      return url
    },
    
    // 获取用户列表
    async fetchUserList() {
      this.loading = true
      console.log('========== 开始获取用户列表 ==========')
      
      try {
        const params = {
          page: this.pagination.currentPage - 1,
          size: this.pagination.pageSize
        }
        
        console.log('📋 查询参数:', params)
        
        const response = await GetUsersPage(params)
        console.log('API响应:', response)
        
        const responseData = response.data || response
        
        if (responseData && responseData.content && Array.isArray(responseData.content)) {
          this.userList = responseData.content
          this.pagination.total = responseData.totalElements || 0
          console.log('📊 搜索返回用户数量:', this.userList.length)
          console.log('📄 总记录数:', this.pagination.total)
          
          // 检查用户对象的字段
          if (this.userList.length > 0) {
            console.log('第一个用户对象:', this.userList[0])
            console.log('第一个用户的created_at:', this.userList[0].created_at)
            console.log('第一个用户的last_login_at:', this.userList[0].last_login_at)
            console.log('第一个用户的last_active_at:', this.userList[0].last_active_at)
            // 检查所有可能的日期字段
            console.log('用户对象的所有字段:', Object.keys(this.userList[0]))
          }
          
          // 检查搜索结果
          if (this.userList.length === 0) {
            console.log('🔍 搜索条件可能过于严格，未找到匹配用户')
          }
        } else if (Array.isArray(responseData)) {
          // 如果直接返回数组
          this.userList = responseData
          this.pagination.total = responseData.length
          console.log('📊 直接返回用户数组，数量:', this.userList.length)
          
          // 检查用户对象的字段
          if (this.userList.length > 0) {
            console.log('第一个用户对象:', this.userList[0])
            console.log('第一个用户的createdAt:', this.userList[0].createdAt)
            console.log('第一个用户的lastLoginAt:', this.userList[0].lastLoginAt)
            // 检查所有可能的日期字段
            console.log('用户对象的所有字段:', Object.keys(this.userList[0]))
          }
        } else {
          console.warn('⚠️ 响应格式异常:', responseData)
          this.userList = []
          this.pagination.total = 0
          this.$message.warning('数据格式异常，请检查后端接口')
        }
        
      } catch (error) {
        console.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败')
        this.userList = []
        this.pagination.total = 0
      } finally {
        this.loading = false
      }
    },
    

    
    // 刷新数据
    refreshData() {
      this.$message.info('正在刷新数据...')
      this.fetchUserList()
    },
    
    // 新增用户
    handleAddUser() {
      this.dialogType = 'add'
      this.userForm = {
        id: null,
        username: '',
        password: '',
        email: '',
        phone: '',
        nickname: '',
        gender: '0',  // 默认未知
        birthday: '',
        status: 'active',
        identityCard: '',
        bio: '',
        roleId: 4,
        avatarUrl: '',
        loginCount: 0
      }
      this.dialogVisible = true
    },
    
    // 编辑用户
    handleEdit(user) {
      this.dialogType = 'edit'
      // 复制用户数据，注意字段映射
      this.userForm = {
        id: user.id,
        username: user.username,
        password: '',  // 编辑时不需要密码
        email: user.email,
        phone: user.phone || '',
        nickname: user.nickname || '',
        gender: user.gender || '0',
        birthday: user.birthday || '',
        status: user.status || 'active',
        identityCard: user.identityCard || '',
        bio: user.bio || '',
        roleId: user.roleId || 4,
        avatarUrl: user.avatarUrl || '',
        loginCount: user.loginCount || 0,
        passwordHash: user.passwordHash || ''  // 保存原用户的passwordHash用于编辑时提交
      }
      this.dialogVisible = true
    },
    
    // 表单提交
    async handleSubmit() {
      console.log('提交表单, 类型:', this.dialogType, '表单数据:', this.userForm)
      
      this.$refs.userForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            let response
            
            // 基础请求数据（所有字段）
            const requestData = {
              username: this.userForm.username,
              email: this.userForm.email,
              phone: this.userForm.phone || '',
              nickname: this.userForm.nickname || '',
              gender: this.userForm.gender,  // 已经是数字字符串 '0','1','2'
              birthday: this.userForm.birthday || null,
              status: this.userForm.status,
              identityCard: this.userForm.identityCard || '',
              bio: this.userForm.bio || '',
              roleId: Number(this.userForm.roleId),  // 确保是数字
              avatarUrl: this.userForm.avatarUrl || '',
              loginCount: this.userForm.loginCount || 0
            }
            
            // 新增用户 - 需要密码
            if (this.dialogType === 'add') {
              if (!this.userForm.password) {
                this.$message.error('密码不能为空')
                this.submitLoading = false
                return
              }
              // 根据后端要求，使用passwordHash字段
              requestData.passwordHash = this.userForm.password
              
              console.log('调用CreateUser, 数据:', requestData)
              response = await CreateUser(requestData)
            } 
            // 编辑用户 - 不需要密码，但需要提供passwordHash字段（可以为空字符串）
            else {
              // 编辑时如果没有修改密码，使用原用户的passwordHash或空字符串
              requestData.passwordHash = this.userForm.passwordHash || ''
              
              console.log('调用UpdateUser, ID:', this.userForm.id, '数据:', requestData)
              response = await UpdateUser(this.userForm.id, requestData)
            }
            
            console.log('API响应:', response)
            
            // 检查响应 - 成功时返回的用户对象会有id
            if (response.data && response.data.id) {
              this.$message.success(this.dialogType === 'add' ? '添加成功' : '更新成功')
              this.dialogVisible = false
              this.fetchUserList()
            } else {
              this.$message.error(response.data?.message || '操作失败')
            }
            
          } catch (error) {
            console.error('提交表单失败:', error)
            
            if (error.response) {
              console.error('错误状态:', error.response.status)
              console.error('错误数据:', error.response.data)
              
              // 提取后端错误信息
              const errorMsg = error.response.data?.message || 
                              error.response.data?.msg || 
                              `操作失败 (${error.response.status})`
              this.$message.error(errorMsg)
            } else {
              this.$message.error(this.dialogType === 'add' ? '添加用户失败' : '更新用户失败')
            }
          } finally {
            this.submitLoading = false
          }
        } else {
          console.log('表单验证失败')
          this.$message.warning('请填写完整表单信息')
        }
      })
    },
    

    
    // 删除用户
    deleteUser(user) {
      console.log('删除用户:', user)
      this.$confirm(`确定要删除用户 "${user.username}" 吗?此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          const response = await DeleteUser(user.id)
          console.log('删除用户响应:', response)
          
          // 更灵活的响应处理：只要删除操作成功（HTTP 200-299）就认为删除成功
          // 因为后端可能返回不同的响应格式
          if (response.status >= 200 && response.status < 300) {
            this.$message.success('用户已删除')
            this.fetchUserList()
          } else {
            this.$message.error(response.data?.message || '删除用户失败')
          }
        } catch (error) {
          console.error('删除用户失败:', error)
          // 即使发生错误，也尝试刷新列表，因为可能删除操作实际已成功
          this.fetchUserList()
          // 显示更详细的错误信息
          const errorMsg = error.response?.data?.message || '删除用户失败'
          this.$message.error(errorMsg)
        }
      }).catch(() => {
        console.log('用户取消删除')
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
    
    // 工具方法 - 角色相关
    getRoleType(roleId) {
      const types = {
        1: 'danger',   // 超级管理员
        2: 'warning',  // 管理员
        3: 'success',  // 审查员
        4: 'info'      // 普通用户
      }
      return types[roleId] || 'info'
    },
    
    getRoleLabel(roleId) {
      const labels = {
        1: '超级管理员',
        2: '管理员',
        3: '审查员',
        4: '用户'
      }
      return labels[roleId] || '未知'
    },
    
    // 状态相关
    getStatusType(status) {
      const types = {
        'active': 'success',
        'disabled': 'danger',
        'inactive': 'warning'
      }
      return types[status] || 'primary'
    },
    
    getStatusLabel(status) {
      const labels = {
        'active': '正常',
        'disabled': '禁用',
        'inactive': '未激活'
      }
      return labels[status] || status
    },
    
    // 日期格式化
    formatDate(dateString) {
      console.log('格式化日期:', dateString)
      if (!dateString) return '-'
      try {
        const date = new Date(dateString)
        // 检查是否是有效日期
        if (isNaN(date.getTime())) {
          console.warn('无效日期:', dateString)
          return '-'
        }
        return date.toLocaleString('zh-CN')
      } catch (e) {
        console.error('日期格式化失败:', e)
        return '-'
      }
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