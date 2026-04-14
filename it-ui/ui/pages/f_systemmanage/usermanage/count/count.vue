<template>
  <div class="system-page">
    <div class="page-header">
      <div>
        <h1>账号管理</h1>
        <p>支持查询、创建、编辑、启用禁用、重置密码和删除账号。</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" icon="el-icon-plus" @click="openCreateDialog">新增账号</el-button>
        <el-button icon="el-icon-refresh" :loading="loading" @click="fetchUserList">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" :inline="true" label-width="68px">
        <el-form-item label="用户名">
          <el-input v-model.trim="searchForm.username" clearable placeholder="请输入用户名" style="width: 180px" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model.trim="searchForm.email" clearable placeholder="请输入邮箱" style="width: 180px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model.trim="searchForm.phone" clearable placeholder="请输入手机号" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="disabled" />
            <el-option label="未激活" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleId" clearable placeholder="全部角色" style="width: 140px">
            <el-option label="超级管理员" :value="1" />
            <el-option label="管理员" :value="2" />
            <el-option label="审核员" :value="3" />
            <el-option label="普通用户" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="userList" v-loading="loading" stripe border>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="头像" width="90" align="center">
          <template slot-scope="{ row }">
            <el-avatar :size="36" :src="row.avatarUrl">{{ getInitial(row.username) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="getRoleType(row.roleId)">{{ getRoleLabel(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170" align="center">
          <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="最后登录" width="170" align="center">
          <template slot-scope="{ row }">{{ row.lastLoginAt ? formatDate(row.lastLoginAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="text" size="mini" @click="resetPassword(row)">重置密码</el-button>
            <el-dropdown @command="cmd => handleMoreCommand(cmd, row)">
              <el-button type="text" size="mini">更多<i class="el-icon-arrow-down el-icon--right"></i></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="disable" v-if="row.status === 'active'">禁用账号</el-dropdown-item>
                <el-dropdown-item command="enable" v-if="row.status !== 'active'">启用账号</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除账号</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          :current-page="pagination.currentPage"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="640px" @close="handleDialogClose">
      <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="userForm.username" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="密码" prop="password">
          <el-input v-model.trim="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="userForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="userForm.phone" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model.trim="userForm.nickname" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="userForm.roleId" style="width: 100%">
            <el-option label="超级管理员" :value="1" />
            <el-option label="管理员" :value="2" />
            <el-option label="审核员" :value="3" />
            <el-option label="普通用户" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="userForm.status" style="width: 100%">
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="disabled" />
            <el-option label="未激活" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="userForm.gender" style="width: 100%">
            <el-option label="未知" value="0" />
            <el-option label="男" value="1" />
            <el-option label="女" value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { AdminResetUserPassword, CreateUser, DeleteUser, GetUsersPage, UpdateUser } from '@/api/index'

export default {
  name: 'UserAccountManage',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchForm: {
        username: '',
        email: '',
        phone: '',
        status: '',
        roleId: ''
      },
      userList: [],
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      dialogVisible: false,
      dialogMode: 'create',
      editingUserId: null,
      userForm: this.createEmptyUserForm(),
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
        roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogMode === 'create' ? '新增账号' : '编辑账号'
    }
  },
  mounted() {
    this.fetchUserList()
  },
  methods: {
    createEmptyUserForm() {
      return {
        username: '',
        password: '',
        email: '',
        phone: '',
        nickname: '',
        roleId: 4,
        status: 'active',
        gender: '0'
      }
    },
    unwrapList(response) {
      const payload = response?.data !== undefined ? response.data : response
      if (Array.isArray(payload)) return { list: payload, total: payload.length }
      if (Array.isArray(payload?.content)) return { list: payload.content, total: payload.totalElements || payload.content.length }
      if (Array.isArray(payload?.records)) return { list: payload.records, total: payload.total || payload.records.length }
      if (Array.isArray(payload?.data)) return { list: payload.data, total: payload.total || payload.data.length }
      return { list: [], total: 0 }
    },
    normalizeUser(user) {
      return {
        id: user.id,
        username: user.username || '',
        email: user.email || '',
        phone: user.phone || '',
        nickname: user.nickname || '',
        roleId: Number(user.roleId ?? user.role_id ?? 4),
        status: user.status || 'active',
        gender: String(user.gender ?? '0'),
        avatarUrl: user.avatarUrl || user.avatar_url || '',
        createdAt: user.createdAt || user.created_at || user.createTime || '',
        lastLoginAt: user.lastLoginAt || user.last_login_at || ''
      }
    },
    async fetchUserList() {
      this.loading = true
      try {
        const params = {
          page: this.pagination.currentPage - 1,
          size: this.pagination.pageSize
        }
        if (this.searchForm.username) params.username = this.searchForm.username
        if (this.searchForm.email) params.email = this.searchForm.email
        if (this.searchForm.phone) params.phone = this.searchForm.phone
        if (this.searchForm.status) params.status = this.searchForm.status
        if (this.searchForm.roleId) params.roleId = this.searchForm.roleId

        const response = await GetUsersPage(params)
        const { list, total } = this.unwrapList(response)
        this.userList = list.map(item => this.normalizeUser(item))
        this.pagination.total = total
      } catch (error) {
        console.error('获取账号列表失败:', error)
        this.$message.error(error?.response?.data?.message || error.message || '获取账号列表失败')
        this.userList = []
        this.pagination.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    handleReset() {
      this.searchForm = { username: '', email: '', phone: '', status: '', roleId: '' }
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    openCreateDialog() {
      this.dialogMode = 'create'
      this.editingUserId = null
      this.userForm = this.createEmptyUserForm()
      this.dialogVisible = true
    },
    openEditDialog(row) {
      this.dialogMode = 'edit'
      this.editingUserId = row.id
      this.userForm = {
        username: row.username,
        password: '',
        email: row.email,
        phone: row.phone,
        nickname: row.nickname,
        roleId: row.roleId,
        status: row.status,
        gender: row.gender
      }
      this.dialogVisible = true
    },
    async handleSubmit() {
      this.$refs.userFormRef.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          const payload = {
            username: this.userForm.username,
            email: this.userForm.email,
            phone: this.userForm.phone,
            nickname: this.userForm.nickname,
            roleId: Number(this.userForm.roleId),
            status: this.userForm.status,
            gender: this.userForm.gender
          }

          if (this.dialogMode === 'create') {
            payload.passwordHash = this.userForm.password
            await CreateUser(payload)
            this.$message.success('账号创建成功')
          } else {
            await UpdateUser(this.editingUserId, payload)
            this.$message.success('账号更新成功')
          }

          this.dialogVisible = false
          await this.fetchUserList()
        } catch (error) {
          console.error('保存账号失败:', error)
          this.$message.error(error?.response?.data?.message || error.message || '保存账号失败')
        } finally {
          this.submitLoading = false
        }
      })
    },
    async resetPassword(row) {
      try {
        const { value } = await this.$prompt(`请输入 ${row.username} 的新密码`, '重置密码', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          inputType: 'password',
          inputPlaceholder: '请输入 8-20 位新密码',
          inputValidator: password => {
            if (!password) return '请输入新密码'
            if (password.length < 8 || password.length > 20) return '密码长度需为 8-20 位'
            return true
          }
        })
        await AdminResetUserPassword(row.id, { newPassword: value })
        this.$message.success('密码重置成功')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error?.response?.data?.message || error.message || '重置密码失败')
          console.error(error)
        }
      }
    },
    handleMoreCommand(command, row) {
      if (command === 'delete') this.removeUser(row)
      if (command === 'disable') this.updateStatus(row, 'disabled')
      if (command === 'enable') this.updateStatus(row, 'active')
    },
    async updateStatus(row, status) {
      try {
        await UpdateUser(row.id, {
          username: row.username,
          email: row.email,
          phone: row.phone,
          nickname: row.nickname,
          roleId: Number(row.roleId),
          status,
          gender: row.gender
        })
        this.$message.success(status === 'active' ? '账号已启用' : '账号已禁用')
        await this.fetchUserList()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error.message || '状态更新失败')
      }
    },
    async removeUser(row) {
      try {
        await this.$confirm(`确认删除账号 ${row.username} 吗？`, '提示', { type: 'warning' })
        await DeleteUser(row.id)
        this.$message.success('账号已删除')
        await this.fetchUserList()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error?.response?.data?.message || error.message || '删除失败')
        }
      }
    },
    handleDialogClose() {
      if (this.$refs.userFormRef) this.$refs.userFormRef.clearValidate()
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchUserList()
    },
    getRoleLabel(roleId) {
      const labels = { 1: '超级管理员', 2: '管理员', 3: '审核员', 4: '普通用户' }
      return labels[Number(roleId)] || '未知'
    },
    getRoleType(roleId) {
      const types = { 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' }
      return types[Number(roleId)] || 'info'
    },
    getStatusLabel(status) {
      const labels = { active: '正常', disabled: '禁用', inactive: '未激活' }
      return labels[status] || status || '-'
    },
    getStatusType(status) {
      const types = { active: 'success', disabled: 'danger', inactive: 'warning' }
      return types[status] || 'info'
    },
    getInitial(username) {
      return username ? username.charAt(0).toUpperCase() : 'U'
    },
    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.system-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-card,
.table-card {
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.pagination-box {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
