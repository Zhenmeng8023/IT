<template>
  <div class="role-permission-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>角色权限管理</h1>
      <p>管理系统角色权限分配，超级管理员可配置所有角色的页面访问权限</p>
    </div>

    <!-- 角色列表 -->
    <el-card class="role-card" shadow="never">
      <div slot="header" class="card-header">
        <span>角色列表</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddRole">
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
              {{ scope.row.name }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="code" label="角色代码" width="120"></el-table-column>
        
        <el-table-column prop="description" label="角色描述" min-width="200"></el-table-column>
        
        <el-table-column prop="userCount" label="用户数量" width="100" align="center">
          <template slot-scope="scope">
            <span class="user-count">{{ scope.row.userCount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-setting"
              @click="handlePermission(scope.row)"
              :disabled="!isSuperAdmin">
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
      
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 管理端权限 -->
        <el-tab-pane label="管理端权限" name="admin">
          <div class="permission-section">
            <h3>系统管理模块</h3>
            <el-tree
              ref="adminTree"
              :data="adminPermissions"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentAdminPermissions"
              :props="treeProps">
            </el-tree>
          </div>
          
          <div class="permission-section">
            <h3>博客管理模块</h3>
            <el-tree
              ref="blogTree"
              :data="blogPermissions"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentBlogPermissions"
              :props="treeProps">
            </el-tree>
          </div>
          
          <div class="permission-section">
            <h3>项目管理模块</h3>
            <el-tree
              ref="projectTree"
              :data="projectPermissions"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentProjectPermissions"
              :props="treeProps">
            </el-tree>
          </div>
        </el-tab-pane>
        
        <!-- 用户端权限 -->
        <el-tab-pane label="用户端权限" name="user">
          <div class="permission-section">
            <h3>用户中心模块</h3>
            <el-tree
              ref="userTree"
              :data="userPermissions"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentUserPermissions"
              :props="treeProps">
            </el-tree>
          </div>
          
          <div class="permission-section">
            <h3>博客功能模块</h3>
            <el-tree
              ref="userBlogTree"
              :data="userBlogPermissions"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentUserBlogPermissions"
              :props="treeProps">
            </el-tree>
          </div>
        </el-tab-pane>
      </el-tabs>
      
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
        
        <el-form-item label="角色代码" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色代码"></el-input>
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
export default {
  name: 'RoleLimit',
  layout: 'manage',
  data() {
    return {
      loading: false,
      saveLoading: false,
      submitLoading: false,
      // 当前用户角色（模拟超级管理员）
      currentUserRole: 'super_admin',
      // 角色列表
      roleList: [],
      // 权限配置对话框控制
      permissionDialogVisible: false,
      permissionDialogTitle: '',
      activeTab: 'admin',
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
        code: [
          { required: true, message: '请输入角色代码', trigger: 'blur' },
          { pattern: /^[a-z_]+$/, message: '角色代码只能包含小写字母和下划线', trigger: 'blur' }
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
      adminPermissions: [
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
        }
      ],
      blogPermissions: [
        {
          id: 'blog_manage',
          name: '博客管理',
          children: [
            { id: 'blog_audit', name: '博客审核' },
            { id: 'blog_dashboard', name: '博客仪表盘' },
            { id: 'blog_algorithm', name: '推荐算法' }
          ]
        }
      ],
      projectPermissions: [
        {
          id: 'project_manage',
          name: '项目管理',
          children: [
            { id: 'project_audit', name: '项目审核' }
          ]
        }
      ],
      userPermissions: [
        {
          id: 'user_center',
          name: '用户中心',
          children: [
            { id: 'user_profile', name: '个人资料' },
            { id: 'user_history', name: '历史记录' },
            { id: 'user_settings', name: '设置' }
          ]
        }
      ],
      userBlogPermissions: [
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
      currentAdminPermissions: [],
      currentBlogPermissions: [],
      currentProjectPermissions: [],
      currentUserPermissions: [],
      currentUserBlogPermissions: []
    }
  },
  computed: {
    // 判断当前用户是否为超级管理员
    isSuperAdmin() {
      return this.currentUserRole === 'super_admin'
    },
    
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
        // 模拟数据
        this.roleList = [
          {
            id: 1,
            name: '超级管理员',
            code: 'super_admin',
            description: '系统最高权限管理者，可以配置所有权限',
            userCount: 1,
            createTime: '2024-01-01 00:00:00',
            permissions: ['*'] // 所有权限
          },
          {
            id: 2,
            name: '管理员',
            code: 'admin',
            description: '系统管理员，负责系统日常管理',
            userCount: 3,
            createTime: '2024-01-02 09:00:00',
            permissions: ['user_manage', 'blog_manage', 'log_manage']
          },
          {
            id: 3,
            name: '审查员',
            code: 'reviewer',
            description: '内容审查员，负责审核博客和项目',
            userCount: 5,
            createTime: '2024-01-03 10:00:00',
            permissions: ['blog_audit', 'project_audit']
          },
          {
            id: 4,
            name: '用户',
            code: 'user',
            description: '普通用户，拥有基本的用户端权限',
            userCount: 100,
            createTime: '2024-01-04 08:00:00',
            permissions: ['user_profile', 'blog_read', 'blog_write', 'blog_comment', 'blog_like']
          }
        ]
      } catch (error) {
        console.error('获取角色列表失败:', error)
        this.$message.error('获取角色列表失败')
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
    
    // 权限配置
    handlePermission(role) {
      if (!this.isSuperAdmin) {
        this.$message.warning('只有超级管理员可以配置权限')
        return
      }
      
      this.currentRole = { ...role }
      this.permissionDialogTitle = `配置 ${role.name} 权限`
      this.permissionDialogVisible = true
      
      // 设置当前角色的权限
      this.setCurrentPermissions(role.permissions)
    },
    
    // 设置当前角色的权限
    setCurrentPermissions(permissions) {
      // 重置所有权限
      this.currentAdminPermissions = []
      this.currentBlogPermissions = []
      this.currentProjectPermissions = []
      this.currentUserPermissions = []
      this.currentUserBlogPermissions = []
      
      if (permissions.includes('*')) {
        // 超级管理员拥有所有权限
        this.currentAdminPermissions = this.getAllPermissionIds(this.adminPermissions)
        this.currentBlogPermissions = this.getAllPermissionIds(this.blogPermissions)
        this.currentProjectPermissions = this.getAllPermissionIds(this.projectPermissions)
        this.currentUserPermissions = this.getAllPermissionIds(this.userPermissions)
        this.currentUserBlogPermissions = this.getAllPermissionIds(this.userBlogPermissions)
      } else {
        // 根据权限列表设置选中状态
        permissions.forEach(permission => {
          if (this.isPermissionInTree(permission, this.adminPermissions)) {
            this.currentAdminPermissions.push(permission)
          } else if (this.isPermissionInTree(permission, this.blogPermissions)) {
            this.currentBlogPermissions.push(permission)
          } else if (this.isPermissionInTree(permission, this.projectPermissions)) {
            this.currentProjectPermissions.push(permission)
          } else if (this.isPermissionInTree(permission, this.userPermissions)) {
            this.currentUserPermissions.push(permission)
          } else if (this.isPermissionInTree(permission, this.userBlogPermissions)) {
            this.currentUserBlogPermissions.push(permission)
          }
        })
      }
    },
    
    // 获取权限树中的所有权限ID
    getAllPermissionIds(treeData) {
      const ids = []
      const traverse = (nodes) => {
        nodes.forEach(node => {
          ids.push(node.id)
          if (node.children) {
            traverse(node.children)
          }
        })
      }
      traverse(treeData)
      return ids
    },
    
    // 检查权限是否在权限树中
    isPermissionInTree(permission, treeData) {
      const findPermission = (nodes) => {
        for (const node of nodes) {
          if (node.id === permission) return true
          if (node.children && findPermission(node.children)) return true
        }
        return false
      }
      return findPermission(treeData)
    },
    
    // 保存权限配置
    async handlePermissionSave() {
      this.saveLoading = true
      try {
        // 获取所有选中的权限
        const adminChecked = this.$refs.adminTree.getCheckedKeys()
        const blogChecked = this.$refs.blogTree.getCheckedKeys()
        const projectChecked = this.$refs.projectTree.getCheckedKeys()
        const userChecked = this.$refs.userTree.getCheckedKeys()
        const userBlogChecked = this.$refs.userBlogTree.getCheckedKeys()
        
        const allPermissions = [
          ...adminChecked,
          ...blogChecked,
          ...projectChecked,
          ...userChecked,
          ...userBlogChecked
        ]
        
        // 模拟保存到后端
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        // 更新角色权限
        const roleIndex = this.roleList.findIndex(role => role.id === this.currentRole.id)
        if (roleIndex !== -1) {
          this.roleList[roleIndex].permissions = allPermissions
        }
        
        this.$message.success('权限配置保存成功')
        this.permissionDialogVisible = false
      } catch (error) {
        this.$message.error('权限配置保存失败')
      } finally {
        this.saveLoading = false
      }
    },
    
    // 权限对话框关闭
    handlePermissionDialogClose() {
      this.currentRole = {}
      this.currentAdminPermissions = []
      this.currentBlogPermissions = []
      this.currentProjectPermissions = []
      this.currentUserPermissions = []
      this.currentUserBlogPermissions = []
    },
    
    // 新增角色
    handleAddRole() {
      if (!this.isSuperAdmin) {
        this.$message.warning('只有超级管理员可以新增角色')
        return
      }
      
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
      if (!this.isSuperAdmin) {
        this.$message.warning('只有超级管理员可以编辑角色')
        return
      }
      
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
      if (!this.isSuperAdmin) {
        this.$message.warning('只有超级管理员可以删除角色')
        return
      }
      
      this.$confirm(`确定要删除角色 "${role.name}" 吗？此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          // 模拟删除操作
          const index = this.roleList.findIndex(item => item.id === role.id)
          if (index !== -1) {
            this.roleList.splice(index, 1)
            this.$message.success('角色删除成功')
          }
        } catch (error) {
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
              const newRole = {
                id: Date.now(),
                name: this.roleForm.name,
                code: this.roleForm.code,
                description: this.roleForm.description,
                userCount: 0,
                createTime: new Date().toISOString(),
                permissions: [] // 默认无权限
              }
              this.roleList.push(newRole)
              this.$message.success('角色新增成功')
            } else {
              // 编辑角色
              const index = this.roleList.findIndex(item => item.id === this.roleForm.id)
              if (index !== -1) {
                this.roleList[index] = {
                  ...this.roleList[index],
                  name: this.roleForm.name,
                  code: this.roleForm.code,
                  description: this.roleForm.description
                }
                this.$message.success('角色信息更新成功')
              }
            }
            
            this.roleDialogVisible = false
          } catch (error) {
            this.$message.error('操作失败')
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

.user-count {
  font-weight: 600;
  color: #409EFF;
}

.permission-section {
  margin-bottom: 20px;
}

.permission-section h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #606266;
}

.dialog-footer {
  text-align: right;
}
</style>