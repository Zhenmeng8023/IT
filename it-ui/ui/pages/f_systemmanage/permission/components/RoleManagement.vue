<template>
  <div class="role-management">
    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="handleCreateRole">
          新增角色
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索角色名称"
            clearable
            style="width: 250px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 角色列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="filteredRoleList"
        v-loading="loading"
        stripe
        style="width: 100%">
        
        <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
        
        <el-table-column prop="roleName" label="角色名称" width="150">
          <template slot-scope="scope">
            <el-tag :type="getRoleType(scope.row.roleName)" size="medium">
              {{ scope.row.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="角色描述" min-width="200"></el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditRole(scope.row)">
              编辑
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-setting"
              @click="handlePermissionConfig(scope.row)">
              权限配置
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              style="color: #f56c6c"
              @click="handleDeleteRole(scope.row)"
              :disabled="scope.row.code === 'super_admin'">
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
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 角色编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      @close="handleDialogClose">
      
      <el-form ref="roleForm" :model="roleForm" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称"></el-input>
        </el-form-item>
        
        <el-form-item label="角色描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="roleForm.description"
            placeholder="请输入角色描述">
          </el-input>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { GetAllRoles, CreateRole, UpdateRole, DeleteRole } from '~/api/index'
export default {
  name: 'RoleManagement',
  data() {
    return {
      loading: false,
      roleList: [],
      searchKeyword: '',
      currentPage: 1,
      pageSize: 10,
      total: 0,
      
      dialogVisible: false,
      dialogTitle: '',
      submitting: false,
      currentRoleId: null,
      
      roleForm: {
        roleName: '',
        description: ''
      },
      
      rules: {
        roleName: [
          { required: true, message: '请输入角色名称', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    filteredRoleList() {
      if (!this.searchKeyword) {
        return this.roleList
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.roleList.filter(role => 
        role.roleName.toLowerCase().includes(keyword) ||
        role.code.toLowerCase().includes(keyword) ||
        (role.description && role.description.toLowerCase().includes(keyword))
      )
    }
  },
  mounted() {
    this.refreshData()
  },
  methods: {
    // 获取角色列表
    async refreshData() {
      this.loading = true
      try {
        const response = await GetAllRoles()
        console.log('角色接口返回数据:', response)
        
        // 适配不同的返回数据结构
        if (response.data && Array.isArray(response.data)) {
          this.roleList = response.data
        } else if (response.data && response.data.success) {
          this.roleList = response.data.data?.list || response.data.data || []
        } else if (response.data && response.data.data) {
          this.roleList = response.data.data
        } else {
          this.roleList = []
          this.$message.error('获取角色列表失败: ' + (response.data?.message || '数据结构异常'))
        }
        
        this.total = this.roleList.length
        console.log('处理后的角色列表:', this.roleList)
        
      } catch (error) {
        console.error('获取角色列表失败:', error)
        this.$message.error('获取角色列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },
    
    // 搜索处理
    handleSearch() {
      this.currentPage = 1
    },
    
    // 分页处理
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },
    
    handleCurrentChange(val) {
      this.currentPage = val
    },
    
    // 角色类型样式
    getRoleType(code) {
      const typeMap = {
        'super_admin': 'danger',
        'admin': 'warning',
        'reviewer': 'primary',
        'user': 'success'
      }
      return typeMap[code] || 'info'
    },
    
    // 日期格式化
    formatDate(date) {
      if (!date) return ''
      
      try {
        const dateObj = new Date(date)
        if (isNaN(dateObj.getTime())) {
          return date // 如果无法解析为日期，返回原始字符串
        }
        
        const year = dateObj.getFullYear()
        const month = String(dateObj.getMonth() + 1).padStart(2, '0')
        const day = String(dateObj.getDate()).padStart(2, '0')
        const hours = String(dateObj.getHours()).padStart(2, '0')
        const minutes = String(dateObj.getMinutes()).padStart(2, '0')
        const seconds = String(dateObj.getSeconds()).padStart(2, '0')
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (error) {
        console.error('日期格式化错误:', error)
        return date // 如果出错，返回原始字符串
      }
    },
    
    // 创建角色
    handleCreateRole() {
      this.dialogTitle = '新增角色'
      this.dialogVisible = true
      this.currentRoleId = null
      this.$nextTick(() => {
        this.$refs.roleForm?.resetFields()
      })
    },
    
    // 编辑角色
    handleEditRole(role) {
      this.dialogTitle = '编辑角色'
      this.dialogVisible = true
      this.currentRoleId = role.id
      this.roleForm = {
        roleName: role.roleName,
        code: role.code,
        description: role.description || ''
      }
    },
    
    // 权限配置
    handlePermissionConfig(role) {
      this.$router.push({
        path: '/f_systemmanage/permission/permission',
        query: { activeTab: 'permission', roleId: role.id }
      })
    },
    
    // 删除角色
    handleDeleteRole(role) {
      if (role.code === 'super_admin') {
        this.$message.warning('超级管理员角色不可删除')
        return
      }
      
      this.$confirm(`确定要删除角色"${role.roleName}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await DeleteRole(role.id)
          this.$message.success('删除成功')
          this.refreshData()
        } catch (error) {
          console.error('删除角色失败:', error)
          this.$message.error('删除角色失败')
        }
      }).catch(() => {})
    },
    
    // 对话框关闭
    handleDialogClose() {
      this.$refs.roleForm?.resetFields()
      this.currentRoleId = null
    },
    
    // 提交表单
    async handleSubmit() {
      try {
        const valid = await this.$refs.roleForm.validate()
        if (!valid) return
        
        this.submitting = true
        
        if (this.currentRoleId) {
          // 编辑角色
          await UpdateRole(this.currentRoleId, this.roleForm)
          this.$message.success('更新角色成功')
        } else {
          // 新增角色
          await CreateRole(this.roleForm)
          this.$message.success('创建角色成功')
        }
        
        this.dialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.role-management {
  padding: 20px;
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

.pagination-container {
  margin-top: 20px;
  text-align: right;
}
</style>