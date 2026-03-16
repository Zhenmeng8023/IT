<template>
  <div class="role-permission-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>角色权限管理</h1>
      <p>管理系统角色权限分配</p>
    </div>

    <!-- 角色列表 -->
    <el-card class="role-card" shadow="never">
      <div slot="header" class="card-header">
        <span>角色列表</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="createRole">
          新增角色
        </el-button>
      </div>
      
      <el-table
        :data="roleList"
        v-loading="loading"
        stripe
        style="width: 100%">
        
        <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
        
        <el-table-column prop="name" label="角色名称" width="120">
          <template slot-scope="scope">
            <el-tag :type="getRoleType(scope.row.code)" size="medium">
              {{ scope.row.role_name }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="角色描述" min-width="300"></el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-setting"
              @click="handlePermission(scope.row)">
              权限配置
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)"
              :disabled="scope.row.code === 'super_admin'">
              编辑
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              :disabled="scope.row.code === 'super_admin'"
              style="color: #F56C6C;">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 权限配置对话框 -->
    <el-dialog
      :title="permissionDialogTitle"
      :visible.sync="permissionDialogVisible"
      width="800px"
      @close="handlePermissionDialogClose">
      
      <el-tree
        ref="permissionTree"
        :data="permissionsTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="currentPermissions"
        :props="treeProps"
        :check-strictly="false">
      </el-tree>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSave" :loading="saveLoading">保存权限</el-button>
      </div>
    </el-dialog>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog
      :title="roleDialogTitle"
      :visible.sync="roleDialogVisible"
      width="500px">
      <el-form ref="roleForm" :model="roleForm" :rules="roleRules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称"></el-input>
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
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  GetAllRoles, 
  CreateRole, 
  UpdateRole, 
  DeleteRole, 
  AssignPermissionsToRole, 
  GetRolePermissions 
} from '@/api/index.js'

export default {
  name: 'RoleLimit',
  layout: 'manage',
  data() {
    return {
      loading: false,
      saveLoading: false,
      submitLoading: false,
      // 角色列表
      roleList: [],
      // 权限配置对话框控制
      permissionDialogVisible: false,
      // 角色对话框控制
      roleDialogVisible: false,
      roleDialogType: 'add',
      // 当前操作的角色
      currentRole: {},
      // 角色表单
      roleForm: {
        id: null,
        name: '',
        code: '',
        description: ''
      },
      // 角色表单验证规则
      roleRules: {
        name: [
          { required: true, message: '请输入角色名称', trigger: 'blur' },
          { min: 2, max: 20, message: '角色名称长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入角色描述', trigger: 'blur' }
        ]
      },
      // 树形控件配置
      treeProps: {
        label: 'name',
        children: 'children'
      },
      // 权限树数据
      permissionsTree: [
        {
          id: 'system_manage',
          name: '系统管理',
          children: [
            { id: 'user_manage', name: '用户管理' },
            { id: 'role_manage', name: '角色权限管理' },
            { id: 'menu_manage', name: '菜单管理' },
            { id: 'log_manage', name: '日志管理' },
            { id: 'label_manage', name: '标签管理' }
          ]
        },
        {
          id: 'blog_manage',
          name: '博客管理',
          children: [
            { id: 'blog_audit', name: '博客审核' },
            { id: 'blog_dashboard', name: '博客仪表盘' },
            { id: 'blog_algorithm', name: '推荐算法' }
          ]
        },
        {
          id: 'project_manage',
          name: '项目管理',
          children: [
            { id: 'project_audit', name: '项目审核' }
          ]
        },
        {
          id: 'user_center',
          name: '用户中心',
          children: [
            { id: 'user_profile', name: '个人资料' },
            { id: 'user_history', name: '历史记录' },
            { id: 'user_settings', name: '设置' }
          ]
        },
        {
          id: 'blog_function',
          name: '博客功能',
          children: [
            { id: 'blog_write', name: '写博客' },
            { id: 'blog_read', name: '阅读博客' },
            { id: 'blog_comment', name: '评论博客' },
            { id: 'blog_like', name: '点赞博客' }
          ]
        }
      ],
      // 当前角色的权限
      currentPermissions: []
    }
  },
  computed: {
    permissionDialogTitle() {
      return `配置 ${this.currentRole.name} 权限`
    },
    
    roleDialogTitle() {
      return this.roleDialogType === 'add' ? '新增角色' : '编辑角色'
    }
  },
  mounted() {
    this.fetchRoleList()
  },
  methods: {
    // 获取角色列表
    async fetchRoleList() {
      this.loading = true
      try {
        const response = await GetAllRoles()
        if (response.data && Array.isArray(response.data)) {
          this.roleList = response.data
        } else if (response.data && response.data.success) {
          this.roleList = response.data.data?.list || response.data.data || []
        } else {
          this.$message.error('获取角色列表失败: ' + (response.data?.message || '未知错误'))
        }
      } catch (error) {
        console.error('获取角色列表失败:', error)
        this.$message.error('获取角色列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },
    
    // 获取角色类型样式
    getRoleType(code) {
      const types = {
        'super_admin': 'danger',
        'admin': 'warning',
        'reviewer': 'success',
        'user': 'info'
      }
      return types[code] || 'info'
    },
    
    // 新增角色
    createRole() {
      this.roleDialogType = 'add'
      this.roleForm = {
        id: null,
        name: '',
        code: '',
        description: ''
      }
      this.roleDialogVisible = true
    },
    
    // 编辑角色
    handleEdit(role) {
      this.roleDialogType = 'edit'
      this.currentRole = { ...role }
      this.roleForm = {
        id: role.id,
        name: role.name,
        code: role.code,
        description: role.description
      }
      this.roleDialogVisible = true
    },



 // 删除角色
handleDelete(role) {
  
  this.$confirm(`确定要删除角色 "${role.name}" 吗？此操作不可恢复。`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      // 调用删除角色接口
      const response = await DeleteRole(role.id)
      
      // 处理不同格式的响应
      let isSuccess = false
      if (response.data && typeof response.data === 'object') {
        if (response.data.success !== undefined) {
          isSuccess = response.data.success
        } else if (response.success !== undefined) {
          isSuccess = response.success
        } else {
          // 对于 DELETE 请求，通常返回空对象或成功状态码
          // 我们可以认为删除成功
          isSuccess = true
        }
      } else {
        // 其他情况，认为删除成功
        isSuccess = true
      }
      
      if (isSuccess) {
        // 更新角色列表
        const index = this.roleList.findIndex(item => item.id === role.id)
        if (index !== -1) {
          this.roleList.splice(index, 1)
        }
        this.$message.success('角色删除成功')
      } else {
        this.$message.error('角色删除失败')
      }
    } catch (error) {
      console.error('角色删除失败:', error)
      this.$message.error('角色删除失败')
    }
  }).catch(() => {})
},
    // 角色表单提交
    async handleRoleSubmit() {
      this.$refs.roleForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            if (this.roleDialogType === 'add') {
              // 新增角色
              const response = await CreateRole({
                name: this.roleForm.name,
                code: this.roleForm.code,
                description: this.roleForm.description
              })
              
              if (response.data.success) {
                await this.fetchRoleList()
                this.$message.success('角色新增成功')
                this.roleDialogVisible = false
              } else {
                this.$message.error('角色新增失败: ' + (response.data?.message || '未知错误'))
              }
            } else {
              // 编辑角色
              const response = await UpdateRole(this.roleForm.id, {
                name: this.roleForm.name,
                code: this.roleForm.code,
                description: this.roleForm.description
              })
              
              if (response.data.success) {
                await this.fetchRoleList()
                this.$message.success('角色信息更新成功')
                this.roleDialogVisible = false
              } else {
                this.$message.error('角色信息更新失败: ' + (response.data?.message || '未知错误'))
              }
            }
          } catch (error) {
            console.error('操作失败:', error)
            this.$message.error('操作失败: ' + (error.message || '网络错误'))
          } finally {
            this.submitLoading = false
          }
        }
      })
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
    },
    
    // 处理权限配置
    async handlePermission(role) {
      this.currentRole = { ...role }
      this.permissionDialogVisible = true
      // 加载角色当前权限
      await this.loadRolePermissions(role.id)
    },
    
    // 加载角色权限
    async loadRolePermissions(roleId) {
      try {
        const response = await GetRolePermissions(roleId)
        let permissions = []
        
        if (response.data && response.data.success && response.data.data) {
          permissions = response.data.data
        } else if (response.data && Array.isArray(response.data)) {
          permissions = response.data
        } else if (response.success && response.data) {
          permissions = response.data
        }
        
        // 提取权限ID
        this.currentPermissions = permissions.map(p => typeof p === 'object' ? p.id : p)
      } catch (error) {
        console.error('加载角色权限失败:', error)
        this.$message.error('加载角色权限失败')
        this.currentPermissions = []
      }
    },
    
    // 保存权限配置
    async handlePermissionSave() {
      this.saveLoading = true
      try {
        // 收集所有选中的权限
        const selectedPermissions = this.$refs.permissionTree.getCheckedKeys()
        console.log('选中的权限:', selectedPermissions)
        console.log('当前角色ID:', this.currentRole.id)
        
        // 调用分配权限接口
        const response = await AssignPermissionsToRole(this.currentRole.id, {
          permissions: selectedPermissions
        })
        
        // 处理不同格式的响应
        let isSuccess = false
        if (response.data && typeof response.data === 'object') {
          if (response.data.success !== undefined) {
            isSuccess = response.data.success
          } else if (response.success !== undefined) {
            isSuccess = response.success
          } else {
            // 对于成功的请求，通常返回空对象或成功状态码
            isSuccess = true
          }
        } else {
          // 其他情况，认为保存成功
          isSuccess = true
        }
        
        if (isSuccess) {
          this.$message.success('权限配置成功')
          this.permissionDialogVisible = false
        } else {
          this.$message.error('权限配置失败')
        }
      } catch (error) {
        console.error('保存权限失败:', error)
        console.error('错误详情:', error.response)
        this.$message.error('保存权限失败: ' + (error.response?.data?.message || error.message || '未知错误'))
      } finally {
        this.saveLoading = false
      }
    },
    
    // 处理权限对话框关闭
    handlePermissionDialogClose() {
      // 清空当前权限
      this.currentPermissions = []
    }
  }
}
</script>

<style scoped>
.role-permission-management {
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

.role-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-footer {
  text-align: right;
}
</style>