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
        
        <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
        
        <el-table-column prop="email" label="邮箱" min-width="180"></el-table-column>
        
        <el-table-column prop="phone" label="手机号" width="130" align="center"></el-table-column>
        
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.gender === '1' || scope.row.gender === 'male' ? 'primary' : scope.row.gender === '2' || scope.row.gender === 'female' ? 'danger' : 'info'">
              {{ scope.row.gender === '1' || scope.row.gender === 'male' ? '男' : scope.row.gender === '2' || scope.row.gender === 'female' ? '女' : '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="birthday" label="生日" width="120" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.birthday) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="occupation" label="职业" width="120">
          <template slot-scope="scope">
            {{ scope.row.occupation || '-' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="regionName" label="地区" width="120">
          <template slot-scope="scope">
            {{ scope.row.regionName || '-' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'active' ? 'success' : scope.row.status === 'disabled' ? 'danger' : 'warning'">
              {{ scope.row.status === 'active' ? '正常' : scope.row.status === 'disabled' ? '禁用' : '未激活' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="balance" label="余额" width="100" align="center">
          <template slot-scope="scope">
            ¥{{ scope.row.balance || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column prop="isPremiumMember" label="VIP" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isPremiumMember ? 'success' : 'info'" size="mini">
              {{ scope.row.isPremiumMember ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="experiencePoints" label="经验值" width="100" align="center">
          <template slot-scope="scope">
            {{ scope.row.experiencePoints || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column prop="loginCount" label="登录次数" width="100" align="center">
          <template slot-scope="scope">
            {{ scope.row.loginCount || 0 }}
          </template>
        </el-table-column>
        
        <el-table-column prop="lastLoginAt" label="最后登录" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.lastLoginAt) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="注册时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createdAt) }}
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
          <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ currentUser.gender === '1' || currentUser.gender === 'male' ? '男' : currentUser.gender === '2' || currentUser.gender === 'female' ? '女' : '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="生日">{{ formatDate(currentUser.birthday) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="职业">{{ currentUser.occupation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地区">{{ currentUser.regionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentUser.identityCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">{{ currentUser.bio || '-' }}</el-descriptions-item>
          <el-descriptions-item label="余额">¥{{ currentUser.balance || 0 }}</el-descriptions-item>
          <el-descriptions-item label="VIP会员">
            <el-tag :type="currentUser.isPremiumMember ? 'success' : 'info'" size="small">
              {{ currentUser.isPremiumMember ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="VIP过期时间">{{ formatDateTime(currentUser.premiumExpiryDate) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经验值">{{ currentUser.experiencePoints || 0 }}</el-descriptions-item>
          <el-descriptions-item label="登录次数">{{ currentUser.loginCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ formatDateTime(currentUser.lastLoginAt) || '从未登录' }}</el-descriptions-item>
          <el-descriptions-item label="最后活跃">{{ formatDateTime(currentUser.lastActiveAt) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(currentUser.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(currentUser.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="角色ID">{{ currentUser.roleId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentUser.status === 'active' ? 'success' : currentUser.status === 'disabled' ? 'danger' : 'warning'" size="small">
              {{ currentUser.status === 'active' ? '正常' : currentUser.status === 'disabled' ? '禁用' : '未激活' }}
            </el-tag>
          </el-descriptions-item>
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
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="editForm.username" placeholder="请输入用户名"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editForm.nickname" placeholder="请输入昵称"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editForm.email" placeholder="请输入邮箱"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="editForm.phone" placeholder="请输入手机号"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="editForm.gender" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="1"></el-option>
                <el-option label="女" value="2"></el-option>
                <el-option label="未知" value="0"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日" prop="birthday">
              <el-date-picker
                v-model="editForm.birthday"
                type="date"
                placeholder="选择生日"
                style="width: 100%"
                value-format="yyyy-MM-dd">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职业" prop="occupation">
              <el-input v-model="editForm.occupation" placeholder="请输入职业"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地区ID" prop="regionId">
              <el-input v-model="editForm.regionId" placeholder="请输入地区ID"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="身份证号" prop="identityCard">
          <el-input v-model="editForm.identityCard" placeholder="请输入身份证号"></el-input>
        </el-form-item>
        
        <el-form-item label="个人简介" prop="bio">
          <el-input
            type="textarea"
            :rows="3"
            v-model="editForm.bio"
            placeholder="请输入个人简介">
          </el-input>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="余额" prop="balance">
              <el-input-number v-model="editForm.balance" :min="0" :precision="2" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经验值" prop="experiencePoints">
              <el-input-number v-model="editForm.experiencePoints" :min="0" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="VIP会员" prop="isPremiumMember">
              <el-switch v-model="editForm.isPremiumMember" active-text="是" inactive-text="否"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="VIP过期时间" prop="premiumExpiryDate">
              <el-date-picker
                v-model="editForm.premiumExpiryDate"
                type="datetime"
                placeholder="选择VIP过期时间"
                style="width: 100%"
                value-format="yyyy-MM-dd HH:mm:ss">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="角色ID" prop="roleId">
              <el-input-number v-model="editForm.roleId" :min="1" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="editForm.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="正常" value="active"></el-option>
                <el-option label="禁用" value="disabled"></el-option>
                <el-option label="未激活" value="inactive"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { GetUsersPage, GetUserById, UpdateUser, AdminUpdateUser } from '@/api/index.js'

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
        occupation: '',
        regionId: null,
        identityCard: '',
        bio: '',
        balance: 0,
        isPremiumMember: false,
        premiumExpiryDate: '',
        experiencePoints: 0,
        roleId: null,
        status: 'active'
      },
      // 编辑表单验证规则
      editRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
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
        // Spring Data JPA 分页参数：page 从 0 开始，size 是每页数量
        const params = {
          page: this.pagination.currentPage - 1,  // 前端从1开始，后端从0开始
          size: this.pagination.pageSize,
          username: this.searchForm.username || undefined,  // 空值不传
          email: this.searchForm.email || undefined,
          phone: this.searchForm.phone || undefined
        }
        
        console.log('📤 请求参数:', params)
        const response = await GetUsersPage(params)
        console.log('📥 完整响应数据:', response)
        console.log('📥 response.data:', response.data)
        
        // 处理不同可能的响应结构
        let records = []
        let total = 0
        
        if (response.data) {
          console.log('🔍 response.data 的所有键:', Object.keys(response.data))
          
          // 情况1: Spring Data JPA 分页结构 { content: [], totalElements: 0 }
          if (response.data.content !== undefined) {
            console.log('📋 content 字段存在')
            console.log('📋 content 类型:', typeof response.data.content)
            console.log('📋 content 是否为数组:', Array.isArray(response.data.content))
            console.log('📋 content 内容:', response.data.content)
            
            if (Array.isArray(response.data.content)) {
              records = response.data.content
              total = response.data.totalElements || 0
              console.log('✅ 检测到 Spring Data JPA 分页结构')
              console.log('   - content 长度:', records.length)
              console.log('   - totalElements:', total)
              
              if (records.length === 0 && total > 0) {
                console.warn('⚠️ 警告：content 为空但 totalElements > 0，可能是后端问题')
                console.warn('   - 当前页码:', params.page)
                console.warn('   - 每页大小:', params.size)
                console.warn('   - 总页数:', response.data.totalPages)
              }
              
              if (records.length > 0) {
                console.log('   - 第一条数据示例:', records[0])
              }
            }
          }
          // 情况2: 标准分页结构 { records: [], total: 0 }
          else if (response.data.records && Array.isArray(response.data.records)) {
            records = response.data.records
            total = response.data.total || 0
            console.log('✅ 检测到标准分页结构')
          }
          // 情况3: response.data 直接是数组
          else if (Array.isArray(response.data)) {
            records = response.data
            total = response.data.length
            console.log('✅ 检测到直接数组结构')
          }
          // 情况4: 其他常见结构
          else if (typeof response.data === 'object') {
            records = response.data.list || 
                      response.data.items || 
                      response.data.users || 
                      response.data.data || []
            total = response.data.total || 
                    response.data.count || 
                    response.data.totalElements ||
                    records.length
            console.log('✅ 检测到其他分页结构')
          }
        }
        
        console.log('📊 提取的records数量:', records.length)
        console.log('📊 总数:', total)
        
        // 标准化字段
        this.userList = Array.isArray(records) ? records.map(user => this.normalizeUserFields(user)) : []
        this.pagination.total = total
        
        console.log('✨ 处理后的用户列表数量:', this.userList.length)
        if (this.userList.length > 0) {
          console.log('✨ 第一条用户数据:', this.userList[0])
        } else if (total > 0) {
          console.error('❌ 错误：总数为', total, '但列表为空！请检查后端接口')
        }
      } catch (error) {
        console.error('❌ 获取用户列表失败:', error)
        console.error('错误详情:', error.response)
        this.$message.error(`获取用户列表失败: ${error.message || '未知错误'}`)
        this.userList = []
        this.pagination.total = 0
      } finally {
        this.loading = false
      }
    },
    
    // 标准化用户字段（兼容蛇形命名和驼峰命名）
    normalizeUserFields(user) {
      return {
        id: user.id,
        username: user.username,
        nickname: user.nickname,
        email: user.email,
        phone: user.phone,
        avatar: user.avatar_url || user.avatarUrl || '/pic/choubi.jpg',
        gender: user.gender,
        birthday: user.birthday,
        occupation: user.occupation,
        regionId: user.region_id || user.regionId,
        regionName: user.region_name || user.regionName,
        identityCard: user.identity_card || user.identityCard,
        bio: user.bio,
        balance: user.balance || 0,
        isPremiumMember: user.is_premium_member !== undefined ? user.is_premium_member : user.isPremiumMember,
        premiumExpiryDate: user.premium_expiry_date || user.premiumExpiryDate,
        experiencePoints: user.experience_points || user.experiencePoints || 0,
        loginCount: user.login_count || user.loginCount || 0,
        lastLoginAt: user.last_login_at || user.lastLoginAt,
        lastActiveAt: user.last_active_at || user.lastActiveAt,
        createdAt: user.created_at || user.createdAt,
        updatedAt: user.updated_at || user.updatedAt,
        roleId: user.role_id || user.roleId,
        status: user.status,
        // 保留原始数据用于调试
        _raw: user
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
        console.log('用户详情原始数据:', response)
        // 标准化字段
        this.currentUser = this.normalizeUserFields(response.data || response)
        console.log('处理后的用户详情:', this.currentUser)
        this.viewDialogVisible = true
      } catch (error) {
        console.error('获取用户详情失败:', error)
        this.$message.error('获取用户详情失败')
      }
    },
    
    // 编辑用户信息
    handleEdit(user) {
      this.editDialogType = 'edit'
      // 使用原始数据或已标准化的数据
      const userData = user._raw || user
      this.currentUser = { ...userData }
      
      this.editForm = {
        id: userData.id,
        username: userData.username,
        nickname: userData.nickname || '',
        email: userData.email,
        phone: userData.phone || '',
        gender: userData.gender || '',
        birthday: userData.birthday || '',
        occupation: userData.occupation || '',
        regionId: userData.region_id || userData.regionId || null,
        identityCard: userData.identity_card || userData.identityCard || '',
        bio: userData.bio || '',
        balance: userData.balance || 0,
        isPremiumMember: userData.is_premium_member !== undefined ? userData.is_premium_member : (userData.isPremiumMember || false),
        premiumExpiryDate: userData.premium_expiry_date || userData.premiumExpiryDate || '',
        experiencePoints: userData.experience_points !== undefined ? userData.experience_points : (userData.experiencePoints || 0),
        roleId: userData.role_id || userData.roleId || null,
        status: userData.status || 'active'
      }
      console.log('编辑表单数据:', this.editForm)
      this.editDialogVisible = true
    },
    
    // 编辑表单提交
    async handleEditSubmit() {
      console.log('🔍 开始表单验证...')
      console.log('📋 当前表单数据:', this.editForm)
      
      this.$refs.editForm.validate(async (valid) => {
        console.log('✅ 表单验证结果:', valid)
        
        if (!valid) {
          console.error('❌ 表单验证失败，请检查必填项')
          this.$message.warning('请检查表单填写是否正确')
          return
        }
        
        this.submitLoading = true
        try {
          // 准备更新数据（不包含 password_hash）
          const submitData = {
            username: this.editForm.username,
            nickname: this.editForm.nickname,
            email: this.editForm.email,
            phone: this.editForm.phone,
            gender: this.editForm.gender,
            birthday: this.editForm.birthday,
            occupation: this.editForm.occupation,
            region_id: this.editForm.regionId,
            identity_card: this.editForm.identityCard,
            bio: this.editForm.bio,
            balance: this.editForm.balance,
            is_premium_member: this.editForm.isPremiumMember,
            premium_expiry_date: this.editForm.premiumExpiryDate,
            experience_points: this.editForm.experiencePoints,
            role_id: this.editForm.roleId,
            status: this.editForm.status
          }
          
          console.log('📤 提交的数据（蛇形命名）:', submitData)
          console.log('📤 用户ID:', this.editForm.id)
          console.log('📤 提交字段列表:', Object.keys(submitData))
          
          if (this.editDialogType === 'edit') {
            // 步骤 1: 先获取用户完整信息
            console.log('🔄 步骤 1: 获取用户原始数据以保留 passwordHash...')
            const userDetailResponse = await GetUserById(this.editForm.id)
            const originalUserData = userDetailResponse.data || userDetailResponse
            
            // 步骤 2: 检查原始数据中是否有 passwordHash
            const hasPasswordHash = originalUserData.password_hash || originalUserData.passwordHash
            console.log('📋 原始数据是否包含 passwordHash:', !!hasPasswordHash)
            
            // 步骤 3: 如果有 passwordHash，合并到提交数据中
            let finalSubmitData = { ...submitData }
            if (hasPasswordHash) {
              finalSubmitData.password_hash = originalUserData.password_hash || originalUserData.passwordHash
              console.log('✅ 已添加 password_hash 到提交数据')
            } else {
              console.warn('⚠️ 警告：原始数据中也没有 passwordHash，后端可能会验证失败')
              console.warn('💡 建议：联系后端开发人员修复 UserInfo 实体类，将 passwordHash 标记为 updatable=false')
              this.$message.warning('未获取到密码哈希值，更新可能受限，请联系管理员')
            }
            
            console.log('📤 最终提交数据字段:', Object.keys(finalSubmitData))
            
            // 步骤 4: 尝试使用管理员接口更新
            console.log('🔄 步骤 4: 调用 AdminUpdateUser API...')
            try {
              const response = await AdminUpdateUser(this.editForm.id, finalSubmitData)
              console.log('✅ AdminUpdateUser 成功:', response)
              this.$message.success('用户信息更新成功')
            } catch (adminError) {
              console.warn('⚠️ AdminUpdateUser 失败，尝试 UpdateUser...', adminError.message)
              
              // 步骤 5: 回退到普通用户接口
              console.log('🔄 步骤 5: 调用 UpdateUser API...')
              const response = await UpdateUser(this.editForm.id, finalSubmitData)
              console.log('✅ UpdateUser 成功:', response)
              this.$message.success('用户信息更新成功')
            }
          }
          
          this.editDialogVisible = false
          this.fetchUserList()
        } catch (error) {
          console.error('❌ 更新用户信息失败:', error)
          console.error('❌ 错误响应:', error.response)
          console.error('❌ 错误状态码:', error.response?.status)
          console.error('❌ 错误消息:', error.response?.data?.message || error.response?.data)
          
          // 提取错误消息
          let errorMsg = '操作失败'
          if (error.response?.data) {
            errorMsg = error.response.data.message || 
                      error.response.data.msg || 
                      error.response.data.error || 
                      error.response.data.errorMessage ||
                      JSON.stringify(error.response.data)
          } else if (error.message) {
            errorMsg = error.message
          }
          
          this.$message.error(`更新失败: ${errorMsg}`)
        } finally {
          this.submitLoading = false
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
      return new Date(date).toLocaleDateString('zh-CN')
    },
    
    // 格式化日期时间
    formatDateTime(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
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