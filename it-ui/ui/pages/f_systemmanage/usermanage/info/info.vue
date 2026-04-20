<template>
  <div class="system-page">
    <div class="page-header">
      <div>
        <h1>用户信息管理</h1>
        <p>查看和维护用户资料、登录信息与基础属性。</p>
      </div>
      <div class="header-actions">
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
        <el-table-column prop="nickname" label="昵称" width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="性别" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="getGenderType(row.gender)">{{ getGenderLabel(row.gender) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="100" align="center">
          <template slot-scope="{ row }">{{ row.balance || 0 }}</template>
        </el-table-column>
        <el-table-column label="登录次数" width="100" align="center">
          <template slot-scope="{ row }">{{ row.loginCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="170" align="center">
          <template slot-scope="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="handleView(row)">查看</el-button>
            <el-button type="text" size="mini" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog title="用户详情" :visible.sync="viewDialogVisible" width="720px">
      <el-descriptions v-if="currentUser" :column="2" border>
        <el-descriptions-item label="ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ getGenderLabel(currentUser.gender) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusLabel(currentUser.status) }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ getRoleLabel(currentUser.roleId) }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ currentUser.balance || 0 }}</el-descriptions-item>
        <el-descriptions-item label="登录次数">{{ currentUser.loginCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentUser.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ currentUser.lastLoginAt ? formatDate(currentUser.lastLoginAt) : '-' }}</el-descriptions-item>
      </el-descriptions>
      <span slot="footer">
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <el-dialog :title="editDialogTitle" :visible.sync="editDialogVisible" width="620px" @close="handleEditDialogClose">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="editForm.username" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model.trim="editForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="editForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="editForm.phone" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="editForm.gender" style="width: 100%">
            <el-option label="未知" value="0" />
            <el-option label="男" value="1" />
            <el-option label="女" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额">
          <el-input-number v-model="editForm.balance" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="登录次数">
          <el-input-number v-model="editForm.loginCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.roleId" style="width: 100%">
            <el-option label="超级管理员" :value="1" />
            <el-option label="管理员" :value="2" />
            <el-option label="审核员" :value="3" />
            <el-option label="普通用户" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="disabled" />
            <el-option label="未激活" value="inactive" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEditSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { AdminGetUserById, AdminGetUsersPage, AdminUpdateUser } from '@/api/index'
import { buildAdminUserPageParams, normalizeAdminUser, unwrapAdminUserPage } from '@/utils/adminUserAdapter'

export default {
  name: 'UserInfoManage',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchForm: {
        username: '',
        email: '',
        phone: ''
      },
      userList: [],
      pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },
      viewDialogVisible: false,
      editDialogVisible: false,
      currentUser: null,
      editForm: this.createEmptyEditForm(),
      editRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }]
      }
    }
  },
  computed: {
    editDialogTitle() {
      return this.editForm.id ? '编辑用户信息' : '新增用户信息'
    }
  },
  mounted() {
    this.fetchUserList()
  },
  methods: {
    createEmptyEditForm() {
      return {
        id: null,
        username: '',
        nickname: '',
        email: '',
        phone: '',
        gender: '0',
        balance: 0,
        loginCount: 0,
        roleId: 4,
        status: 'active'
      }
    },
    async fetchUserList() {
      this.loading = true
      try {
        const params = buildAdminUserPageParams(this.pagination, this.searchForm)
        const response = await AdminGetUsersPage(params)
        const { list, total } = unwrapAdminUserPage(response)
        this.userList = list.map(item => normalizeAdminUser(item))
        this.pagination.total = total
      } catch (error) {
        console.error('获取用户信息列表失败:', error)
        this.$message.error(error?.response?.data?.message || error.message || '获取用户信息列表失败')
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
      this.searchForm = { username: '', email: '', phone: '' }
      this.pagination.currentPage = 1
      this.fetchUserList()
    },
    async handleView(row) {
      try {
        const response = await AdminGetUserById(row.id)
        const payload = response?.data !== undefined ? response.data : response
        this.currentUser = normalizeAdminUser(payload)
        this.viewDialogVisible = true
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error.message || '查看详情失败')
      }
    },
    async handleEdit(row) {
      try {
        const response = await AdminGetUserById(row.id)
        const payload = response?.data !== undefined ? response.data : row
        const normalized = normalizeAdminUser(payload)
        this.editForm = {
          id: normalized.id,
          username: normalized.username,
          nickname: normalized.nickname,
          email: normalized.email,
          phone: normalized.phone,
          gender: normalized.gender,
          balance: normalized.balance,
          loginCount: normalized.loginCount,
          roleId: normalized.roleId,
          status: normalized.status
        }
        this.editDialogVisible = true
      } catch (error) {
        this.$message.error(error?.response?.data?.message || error.message || '加载编辑数据失败')
      }
    },
    async handleEditSubmit() {
      this.$refs.editFormRef.validate(async valid => {
        if (!valid) return
        this.submitLoading = true
        try {
          await AdminUpdateUser(this.editForm.id, {
            username: this.editForm.username,
            nickname: this.editForm.nickname,
            email: this.editForm.email,
            phone: this.editForm.phone,
            gender: this.editForm.gender,
            balance: this.editForm.balance,
            loginCount: this.editForm.loginCount,
            roleId: Number(this.editForm.roleId),
            status: this.editForm.status
          })
          this.$message.success('用户信息已更新')
          this.editDialogVisible = false
          await this.fetchUserList()
        } catch (error) {
          this.$message.error(error?.response?.data?.message || error.message || '更新失败')
        } finally {
          this.submitLoading = false
        }
      })
    },
    handleEditDialogClose() {
      if (this.$refs.editFormRef) this.$refs.editFormRef.clearValidate()
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
    getGenderLabel(gender) {
      const labels = { '0': '未知', '1': '男', '2': '女', male: '男', female: '女' }
      return labels[String(gender)] || '未知'
    },
    getGenderType(gender) {
      const genderValue = String(gender)
      if (genderValue === '1' || genderValue === 'male') return 'primary'
      if (genderValue === '2' || genderValue === 'female') return 'danger'
      return 'info'
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
  color: var(--it-text);
}

.page-header p {
  margin: 0;
  color: var(--it-text-muted);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-card,
.table-card {
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-soft);
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
