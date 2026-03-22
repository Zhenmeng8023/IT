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
        <el-button v-permission="'btn:role:create'" type="primary" icon="el-icon-plus" @click="handleCreateRole">
          新增角色
        </el-button>
        <el-button v-permission="'btn:role:refresh'" type="primary" icon="el-icon-refresh" @click="refreshData">
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
            <el-button v-permission="'btn:role:edit'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditRole(scope.row)">
              编辑
            </el-button>
            <el-button v-permission="'btn:role:assign-permission'"
              size="mini"
              type="text"
              icon="el-icon-setting"
              @click="handlePermissionConfig(scope.row)">
              权限配置
            </el-button>
            <el-button v-permission="'btn:role:delete'"
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
          :check-strictly="true"
          @check="handleMenuCheck"
          ref="menuTree"
          class="menu-tree">
          <template slot-scope="{ node, data }">
            <span class="menu-node">
              <i :class="data.icon" v-if="data.icon" style="margin-right: 8px;"></i>
              {{ node.label }}
              <span v-if="data.type === 'button'" style="margin-left: 8px; font-size: 12px; color: #909399;">(按钮)</span>
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
import { GetAllRoles, CreateRole, UpdateRole, DeleteRole, GetAllMenus, AssignMenusToRole, GetRoleMenus } from '~/api/index'
import { useUserStore } from '~/store/user'

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
      this.permissionDialogVisible = true
      this.currentRoleIdForPermission = role.id
      this.currentRoleName = role.role_name || role.name || role.roleName
      
      try {
        // 并行获取所有菜单和角色现有权限
        const [menusResponse, roleMenusResponse] = await Promise.all([
          GetAllMenus(),
          GetRoleMenus(role.id)
        ])
        
        console.log('菜单接口返回:', menusResponse)
        console.log('角色菜单接口返回:', roleMenusResponse)
        
        // 处理菜单数据
        let menuData = []
        if (menusResponse.data && Array.isArray(menusResponse.data)) {
          menuData = menusResponse.data
        } else if (menusResponse.data && menusResponse.data.success) {
          menuData = menusResponse.data.data?.list || menusResponse.data.data || []
        } else if (menusResponse.data && menusResponse.data.data) {
          menuData = menusResponse.data.data
        } else if (menusResponse.success && menusResponse.data) {
          menuData = menusResponse.data
        } else if (menusResponse.data) {
          menuData = menusResponse.data
        }
        
        console.log('处理后的菜单数据:', menuData)
        
        // 构建菜单树
        this.menuList = this.buildMenuTree(menuData)
        console.log('构建的菜单树:', this.menuList)
        
        // 展开所有节点
        this.expandedKeys = this.getAllMenuIds(this.menuList)
        console.log('展开的节点:', this.expandedKeys)
        
        // 处理角色现有权限
        let roleMenuIds = []
        if (roleMenusResponse.data && Array.isArray(roleMenusResponse.data)) {
          roleMenuIds = roleMenusResponse.data.map(menu => menu.id)
        } else if (roleMenusResponse.data && roleMenusResponse.data.success) {
          const roleMenus = roleMenusResponse.data.data?.list || roleMenusResponse.data.data || []
          roleMenuIds = roleMenus.map(menu => menu.id)
        } else if (roleMenusResponse.data && roleMenusResponse.data.data) {
          const roleMenus = roleMenusResponse.data.data
          roleMenuIds = Array.isArray(roleMenus) ? roleMenus.map(menu => menu.id) : []
        } else if (roleMenusResponse.success && roleMenusResponse.data) {
          roleMenuIds = Array.isArray(roleMenusResponse.data) ? roleMenusResponse.data.map(menu => menu.id) : []
        }
        
        // 设置初始选中的菜单
        this.checkedKeys = roleMenuIds
        console.log('初始选中的菜单:', this.checkedKeys)
        
        // 确保父菜单也被选中
        this.ensureParentNodesChecked()
      } catch (error) {
        console.error('获取权限配置失败:', error)
        console.error('错误响应:', error.response)
        this.$message.error('获取权限配置失败: ' + (error.response?.data?.message || error.message || '网络错误'))
        
        // 即使获取失败，也要构建菜单树以便用户操作
        try {
          const response = await GetAllMenus()
          let menuData = []
          if (response.data && Array.isArray(response.data)) {
            menuData = response.data
          } else if (response.data && response.data.success) {
            menuData = response.data.data?.list || response.data.data || []
          } else if (response.data && response.data.data) {
            menuData = response.data.data
          } else if (response.success && response.data) {
            menuData = response.data
          } else if (response.data) {
            menuData = response.data
          }
          
          this.menuList = this.buildMenuTree(menuData)
          this.expandedKeys = this.getAllMenuIds(this.menuList)
          this.checkedKeys = []
        } catch (menuError) {
          console.error('获取菜单列表失败:', menuError)
        }
      }
    },
    
    // 构建菜单树
    buildMenuTree(menus) {
      const menuMap = {}
      const rootMenus = []
      
      console.log('原始菜单数据:', menus)
      
      // 先将所有菜单按ID映射，并确保每个菜单都有label属性
      menus.forEach(menu => {
        // 确保菜单有label属性，使用name作为标签
        const menuWithLabel = {
          ...menu,
          label: menu.name || menu.label || menu.title || menu.menu_name || '未知菜单',
          children: []
        }
        menuMap[menu.id] = menuWithLabel
      })
      
      console.log('菜单映射:', menuMap)
      
      // 构建树状结构
      menus.forEach(menu => {
        // 兼容parent_id和parentId两种字段名
        const parentId = menu.parent_id || menu.parentId
        console.log(`处理菜单: ${menu.name}, parentId: ${parentId}, 类型: ${typeof parentId}`)
        
        // 尝试转换parentId为数字进行比较
        const parentIdNum = parseInt(parentId)
        
        if (parentId === 0 || parentId === '0' || parentIdNum === 0 || !parentId) {
          console.log(`添加根菜单: ${menu.name}`)
          rootMenus.push(menuMap[menu.id])
        } else if (menuMap[parentId] || menuMap[parentIdNum]) {
          const parentMenu = menuMap[parentId] || menuMap[parentIdNum]
          console.log(`添加子菜单: ${menu.name} 到 ${parentMenu.name}`)
          parentMenu.children.push(menuMap[menu.id])
        } else {
          console.log(`找不到父菜单: ${parentId}，添加为根菜单`)
          rootMenus.push(menuMap[menu.id])
        }
      })
      
      console.log('构建的菜单树:', rootMenus)
      return rootMenus
    },
    
    // 获取所有菜单ID
    getAllMenuIds(menus) {
      const ids = []
      
      function traverse(node) {
        ids.push(node.id)
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => traverse(child))
        }
      }
      
      menus.forEach(menu => traverse(menu))
      return ids
    },
    
    // 菜单勾选处理
    handleMenuCheck(data, treeObj) {
      // 直接使用树组件的checkedKeys，因为check-strictly设为false后，Element UI会自动处理父子节点关系
      this.checkedKeys = [...treeObj.checkedKeys]
      console.log('当前选中的菜单:', this.checkedKeys)
    },
    
    // 确保父节点被选中（保持兼容，用于初始化时）
    ensureParentNodesChecked() {
      // 由于check-strictly设为false，Element UI会自动处理父子节点关系
      // 这里只需要确保checkedKeys正确设置即可
      this.$nextTick(() => {
        if (this.$refs.menuTree) {
          this.$refs.menuTree.setCheckedKeys(this.checkedKeys)
        }
      })
    },
    
    // 根据ID查找节点
    findNodeById(menus, id) {
      for (const menu of menus) {
        if (menu.id === id) {
          return menu
        }
        if (menu.children && menu.children.length > 0) {
          const found = this.findNodeById(menu.children, id)
          if (found) {
            return found
          }
        }
      }
      return null
    },
    
    // 权限配置提交
    async handlePermissionSubmit() {
      try {
        this.permissionSubmitting = true
        
        // 确保数据格式正确
        const menuIds = this.checkedKeys
        console.log('提交的菜单ID:', menuIds)
        
        // 调用API分配菜单权限
        await AssignMenusToRole(this.currentRoleIdForPermission, menuIds)
        
        // 刷新当前用户的权限
        const userStore = useUserStore()
        await userStore.refreshPermissions()
        
        this.$message.success('权限配置成功')
        this.permissionDialogVisible = false
      } catch (error) {
        console.error('权限配置失败:', error)
        console.error('错误响应:', error.response)
        console.error('错误状态:', error.response?.status)
        console.error('错误数据:', error.response?.data)
        
        const errorMessage = error.response?.data?.message || 
                            error.response?.data?.error || 
                            error.message || 
                            '网络错误'
        this.$message.error('权限配置失败: ' + errorMessage)
      } finally {
        this.permissionSubmitting = false
      }
    },
    
    // 权限配置对话框关闭
    handlePermissionDialogClose() {
      this.menuList = []
      this.expandedKeys = []
      this.checkedKeys = []
      this.currentRoleIdForPermission = null
      this.currentRoleName = ''
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