<template>
  <div class="role-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>角色管理</h1>
      <p>管理系统角色，支持角色的增删改查和权限配置</p>
    </div>

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
        
        <el-table-column prop="role_name" label="角色名称" width="150">
          <template slot-scope="scope">
            <el-tag :type="getRoleType(scope.row.id)" size="medium">
              {{ scope.row.role_name || scope.row.name || scope.row.roleName || '未知角色' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="角色描述" min-width="200"></el-table-column>
        
        <el-table-column prop="created_at" label="创建时间" width="180" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.created_at || scope.row.createdAt || scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="updated_at" label="更新时间" width="180" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.updated_at || scope.row.updatedAt || scope.row.updateTime) }}
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
              :disabled="scope.row.id === 1">
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

    <!-- 权限配置对话框 -->
    <el-dialog
      :title="`权限配置 - ${currentRoleName}`"
      :visible.sync="permissionDialogVisible"
      width="600px"
      @close="handlePermissionDialogClose">
      
      <div class="permission-config">
        <el-tree
          :data="menuList"
          show-checkbox
          node-key="id"
          :default-expanded-keys="expandedKeys"
          :default-checked-keys="checkedKeys"
          ref="menuTree"
          class="menu-tree">
          <template slot-scope="{ node, data }">
            <span class="menu-node">
              <i :class="data.icon" v-if="data.icon" style="margin-right: 8px;"></i>
              {{ node.label }}
            </span>
          </template>
        </el-tree>
      </div>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePermissionSubmit" :loading="permissionSubmitting">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { GetAllRoles, CreateRole, UpdateRole, DeleteRole, GetAllMenus, AssignMenusToRole } from '~/api/index'

export default {
  name: 'Role',
  layout: 'manage',
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
      },
      
      // 权限配置相关
      permissionDialogVisible: false,
      currentRoleIdForPermission: null,
      currentRoleName: '',
      menuList: [],
      expandedKeys: [],
      checkedKeys: [],
      permissionSubmitting: false
    }
  },
  computed: {
    filteredRoleList() {
      if (!this.searchKeyword) {
        return this.roleList
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.roleList.filter(role => 
        (role.role_name || role.name || role.roleName).toLowerCase().includes(keyword) ||
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
        console.log('开始获取角色列表')
        const response = await GetAllRoles()
        console.log('角色接口返回数据:', response)
        
        // 适配不同的返回数据结构
        let roleData = []
        if (response.data && Array.isArray(response.data)) {
          roleData = response.data
        } else if (response.data && response.data.success) {
          roleData = response.data.data?.list || response.data.data || []
        } else if (response.data && response.data.data) {
          roleData = response.data.data
        } else if (response.success && response.data) {
          roleData = response.data
        } else if (response.data) {
          roleData = response.data
        } else {
          roleData = []
          this.$message.error('获取角色列表失败: ' + (response.message || '数据结构异常'))
        }
        
        console.log('原始角色数据:', roleData)
        
        // 标准化角色数据
        this.roleList = roleData.map((role, index) => {
          console.log(`角色${index}完整数据:`, role)
          console.log(`角色${index}的时间字段:`, {
            createdAt: role.createdAt,
            updatedAt: role.updatedAt,
            created_at: role.created_at,
            updated_at: role.updated_at
          })
          
          return {
            id: role.id,
            role_name: role.role_name || role.name || role.roleName,
            description: role.description || '',
            created_at: role.createdAt || role.created_at,
            updated_at: role.updatedAt || role.updated_at
          }
        })
        
        this.total = this.roleList.length
        console.log('处理后的角色列表:', this.roleList)
        
      } catch (error) {
        console.error('获取角色列表失败:', error)
        console.error('错误响应:', error.response)
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
    getRoleType(id) {
      const typeMap = {
        1: 'danger',  // 超级管理员
        2: 'warning', // 管理员
        3: 'primary', // 审查员
        4: 'success'  // 用户
      }
      return typeMap[id] || 'info'
    },
    
    // 日期格式化
    formatDate(date) {
      if (!date) return ''
      
      try {
        let dateObj
        
        // 处理可能的时间戳格式
        if (typeof date === 'string') {
          // 尝试多种日期格式解析
          dateObj = new Date(date)
          
          // 如果解析失败，尝试Date.parse
          if (isNaN(dateObj.getTime())) {
            const timestamp = Date.parse(date)
            if (!isNaN(timestamp)) {
              dateObj = new Date(timestamp)
            } else {
              // 如果所有解析都失败，返回原始字符串
              return date
            }
          }
        } else if (typeof date === 'number') {
          // 处理时间戳
          dateObj = new Date(date)
        } else if (date instanceof Date) {
          // 处理Date对象
          dateObj = date
        } else {
          // 处理其他类型
          return String(date)
        }
        
        // 检查dateObj是否有效
        if (isNaN(dateObj.getTime())) {
          return String(date)
        }
        
        // 格式化日期
        const year = dateObj.getFullYear()
        const month = String(dateObj.getMonth() + 1).padStart(2, '0')
        const day = String(dateObj.getDate()).padStart(2, '0')
        const hours = String(dateObj.getHours()).padStart(2, '0')
        const minutes = String(dateObj.getMinutes()).padStart(2, '0')
        const seconds = String(dateObj.getSeconds()).padStart(2, '0')
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (error) {
        console.error('日期格式化错误:', error)
        // 确保返回一个字符串，避免潜在的类型错误
        try {
          return String(date)
        } catch (e) {
          return ''
        }
      }
    },
    
    // 创建角色
    handleCreateRole() {
      this.dialogTitle = '新增角色'
      this.dialogVisible = true
      this.currentRoleId = null
      this.$nextTick(() => {
        this.$refs.roleForm?.resetFields()
        this.roleForm = {
          roleName: '',
          description: ''
        }
      })
    },
    
    // 编辑角色
    handleEditRole(role) {
      this.dialogTitle = '编辑角色'
      this.dialogVisible = true
      this.currentRoleId = role.id
      this.$nextTick(() => {
        this.roleForm = {
          roleName: role.role_name || role.name || role.roleName,
          description: role.description || ''
        }
        // 重置表单验证状态
        this.$refs.roleForm?.resetFields()
      })
    },
    
    // 权限配置
    async handlePermissionConfig(role) {
      this.currentRoleIdForPermission = role.id
      this.currentRoleName = role.role_name || role.name || role.roleName || '未知角色'
      this.permissionDialogVisible = true
      
      try {
        // 加载菜单列表
        await this.loadMenus()
        // 这里可以添加加载角色现有权限的逻辑
      } catch (error) {
        console.error('加载权限配置失败:', error)
        this.$message.error('加载权限配置失败')
      }
    },
    
    // 加载菜单列表
    async loadMenus() {
      try {
        const response = await GetAllMenus()
        console.log('菜单接口返回数据:', response)
        
        let menuData = []
        // 适配不同的返回数据结构
        if (response.data && Array.isArray(response.data)) {
          menuData = response.data
        } else if (response.data && response.data.success) {
          menuData = response.data.data?.list || response.data.data || []
        } else if (response.data && response.data.data) {
          menuData = response.data.data
        } else {
          menuData = []
        }
        
        // 构建菜单树
        this.menuList = this.buildMenuTree(menuData)
        // 展开所有节点
        this.expandedKeys = this.getAllMenuIds(this.menuList)
        // 默认选中所有菜单（可以根据实际需求调整）
        this.checkedKeys = this.getAllMenuIds(this.menuList)
        
        console.log('处理后的菜单树:', this.menuList)
        console.log('展开的节点:', this.expandedKeys)
        console.log('选中的节点:', this.checkedKeys)
        
      } catch (error) {
        console.error('加载菜单失败:', error)
        this.$message.error('加载菜单失败')
        this.menuList = []
      }
    },
    
    // 构建菜单树
    buildMenuTree(menus, parentId = null) {
      return menus
        .filter(menu => menu.parent_id === parentId || (parentId === null && (menu.parent_id === null || menu.parent_id === 0)))
        .map(menu => ({
          id: menu.id,
          label: menu.name,
          icon: menu.icon,
          children: this.buildMenuTree(menus, menu.id)
        }))
    },
    
    // 获取所有菜单ID
    getAllMenuIds(menus) {
      let ids = []
      menus.forEach(menu => {
        ids.push(menu.id)
        if (menu.children && menu.children.length > 0) {
          ids = [...ids, ...this.getAllMenuIds(menu.children)]
        }
      })
      return ids
    },
    
    // 处理权限配置对话框关闭
    handlePermissionDialogClose() {
      this.permissionDialogVisible = false
      this.currentRoleIdForPermission = null
      this.currentRoleName = ''
      this.menuList = []
      this.expandedKeys = []
      this.checkedKeys = []
    },
    
    // 提交权限配置
    async handlePermissionSubmit() {
      try {
        // 获取选中的菜单ID
        const selectedMenuIds = this.$refs.menuTree.getCheckedKeys()
        console.log('选中的菜单ID:', selectedMenuIds)
        
        this.permissionSubmitting = true
        
        // 调用API分配菜单权限
        await AssignMenusToRole(this.currentRoleIdForPermission, selectedMenuIds)
        
        this.$message.success('权限配置成功')
        this.permissionDialogVisible = false
        
      } catch (error) {
        console.error('权限配置失败:', error)
        this.$message.error('权限配置失败: ' + (error.message || '网络错误'))
      } finally {
        this.permissionSubmitting = false
      }
    },
    
    // 删除角色
    handleDeleteRole(role) {
      if (role.id === 1) {
        this.$message.warning('超级管理员角色不可删除')
        return
      }
      
      this.$confirm(`确定要删除角色"${role.role_name || role.name || role.roleName}"吗？`, '提示', {
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
        // 检查表单引用是否存在
        if (!this.$refs.roleForm) {
          this.$message.error('表单初始化失败，请重试')
          return
        }
        
        const valid = await this.$refs.roleForm.validate()
        if (!valid) return
        
        this.submitting = true
        
        // 构建请求数据
        const roleData = {
          roleName: this.roleForm.roleName,
          description: this.roleForm.description
        }
        
        console.log('角色数据:', roleData)
        
        if (this.currentRoleId) {
          // 编辑角色
          console.log('更新角色ID:', this.currentRoleId)
          await UpdateRole(this.currentRoleId, roleData)
          this.$message.success('更新角色成功')
        } else {
          // 新增角色
          console.log('创建角色')
          await CreateRole(roleData)
          this.$message.success('创建角色成功')
        }
        
        this.dialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('操作失败:', error)
        console.error('错误响应:', error.response)
        console.error('错误状态:', error.response?.status)
        console.error('错误数据:', error.response?.data)
        
        const errorMessage = error.response?.data?.message || 
                            error.response?.data?.error || 
                            error.message || 
                            '网络错误'
        this.$message.error('操作失败: ' + errorMessage)
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
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 14px;
  color: #606266;
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

.dialog-footer {
  text-align: right;
}
  /* 权限配置对话框样式 */
  .permission-config {
    max-height: 400px;
    overflow-y: auto;
  }
  
  .menu-tree {
    line-height: 40px;
  }
  
  .menu-node {
    display: flex;
    align-items: center;
  }
  
  .menu-node i {
    font-size: 16px;
  }
</style>