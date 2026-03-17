<template>
  <div class="menu-management">
    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="handleCreateMenu">
          新增菜单
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索菜单名称或路径"
            clearable
            style="width: 250px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 菜单列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="filteredMenuList"
        v-loading="loading"
        row-key="id"
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
        stripe
        style="width: 100%">

        <el-table-column prop="name" label="菜单名称" min-width="150">
          <template slot-scope="scope">
            <div class="menu-name">
              <i :class="scope.row.icon" v-if="scope.row.icon" style="margin-right: 8px;"></i>
              <span>{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="path" label="菜单路径" min-width="160"></el-table-column>

        <el-table-column prop="component" label="组件路径" min-width="180"></el-table-column>

        <el-table-column label="排序" width="80" align="center">
          <template slot-scope="scope">
            <el-tag size="small">{{ scope.row.sortOrder || scope.row.sort || 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="菜单类型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getMenuType(scope.row.type)" size="small">
              {{ getMenuTypeText(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="权限代码" width="120" align="center">
          <template slot-scope="scope">
            <el-tag v-if="getPermissionCode(scope.row.permissionId)" type="info" size="small">
              {{ getPermissionCode(scope.row.permissionId) }}
            </el-tag>
            <span v-else style="color: #c0c4cc;">无</span>
          </template>
        </el-table-column>

        <el-table-column label="可见性" width="80" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="!scope.row.isHidden"
              @change="handleVisibleChange(scope.row)"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditMenu(scope.row)">
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
              @click="handleDeleteMenu(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 菜单编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="600px"
      @close="handleDialogClose">
      
      <el-form ref="menuForm" :model="menuForm" :rules="rules" label-width="100px">
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="menuForm.name" placeholder="请输入菜单名称"></el-input>
        </el-form-item>
        
        <el-form-item label="父级菜单" prop="parentId">
          <el-select v-model="menuForm.parentId" placeholder="请选择父级菜单" style="width: 100%">
            <el-option label="根菜单" :value="null"></el-option>
            <el-option
              v-for="menu in parentMenuOptions"
              :key="menu.id"
              :label="menu.name"
              :value="menu.id">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="菜单路径" prop="path">
          <el-input v-model="menuForm.path" placeholder="请输入菜单路径"></el-input>
        </el-form-item>
        
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="menuForm.component" placeholder="请输入组件路径"></el-input>
        </el-form-item>
        
        <el-form-item label="菜单图标" prop="icon">
          <el-input v-model="menuForm.icon" placeholder="请输入图标类名">
            <template slot="prepend">
              <i :class="menuForm.icon" v-if="menuForm.icon"></i>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="menuForm.sortOrder" :min="0" :max="999"></el-input-number>
        </el-form-item>
        
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="menuForm.type">
            <el-radio label="menu">菜单</el-radio>
            <el-radio label="button">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        
         <el-form-item label="权限代码" prop="permissionId">
          <el-select v-model="menuForm.permissionId" placeholder="请选择权限">
            <el-option
              v-for="permission in permissionList"
              :key="permission.id"
              :label="permission.permissionCode || permission.permission_code || permission.name || permission.id"
              :value="permission.id">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="可见性" prop="visible">
          <el-switch
            v-model="menuForm.visible"
            active-text="可见"
            inactive-text="隐藏">
          </el-switch>
        </el-form-item>
        
        <el-form-item label="菜单描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="menuForm.description"
            placeholder="请输入菜单描述">
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
import { GetAllMenus, CreateMenu, UpdateMenu, DeleteMenu, GetAllPermissions } from '~/api/index'

export default {
  name: 'MenuManagement',
  data() {
    return {
      loading: false,
      menuList: [],
      permissionList: [], // 权限列表
      searchKeyword: '',
      
      dialogVisible: false,
      dialogTitle: '',
      submitting: false,
      currentMenuId: null,
      
      menuForm: {
        id: null,
        name: '',
        path: '',
        component: '',
        icon: '',
        type: 'menu',
        parentId: 0,
        sortOrder: 0,
        isHidden: false,
        permissionId: null,
        remark: ''
      },
      
      rules: {
        name: [
          { required: true, message: '请输入菜单名称', trigger: 'blur' }
        ],
        path: [
          { required: true, message: '请输入菜单路径', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    filteredMenuList() {
      if (!this.searchKeyword) {
        return this.menuList
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.menuList.filter(menu => 
        menu.name.toLowerCase().includes(keyword) ||
        menu.path.toLowerCase().includes(keyword) ||
        (menu.description && menu.description.toLowerCase().includes(keyword))
      )
    },
    
    parentMenuOptions() {
      return this.menuList.filter(menu => menu.type === 0) // 只显示目录类型的菜单作为父级
    }
  },
  mounted() {
    this.refreshData()
    this.loadPermissions()
  },
  methods: {
    // 获取权限列表
    async loadPermissions() {
      try {
        const response = await GetAllPermissions()
        console.log('权限接口返回数据:', response)
        
        let permissionData = []
        // 适配不同的返回数据结构
        if (response.data && Array.isArray(response.data)) {
          permissionData = response.data
        } else if (response.data && response.data.success) {
          permissionData = response.data.data?.list || response.data.data || []
        } else if (response.data && response.data.data) {
          permissionData = response.data.data
        } else {
          permissionData = []
        }
        
        this.permissionList = permissionData
        console.log('处理后的权限列表:', this.permissionList)
        
      } catch (error) {
        console.error('获取权限列表失败:', error)
        this.$message.error('获取权限列表失败: ' + (error.message || '网络错误'))
      }
    },
    
    // 获取菜单列表
    async refreshData() {
      this.loading = true
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
        
        this.menuList = this.buildTree(menuData)
        console.log('处理后的菜单列表:', this.menuList)
        
      } catch (error) {
        console.error('获取菜单列表失败:', error)
        this.$message.error('获取菜单列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },
    
    // 构建树形结构
    buildTree(menus) {
      const menuMap = {}
      const tree = []
      
      // 创建映射
      menus.forEach(menu => {
        menuMap[menu.id] = { ...menu, children: [] }
      })
      
      // 构建树
      menus.forEach(menu => {
        if (menu.parentId && menuMap[menu.parentId]) {
          menuMap[menu.parentId].children.push(menuMap[menu.id])
        } else {
          tree.push(menuMap[menu.id])
        }
      })
      
      return tree
    },
    
    // 搜索处理
    handleSearch() {
      // 搜索逻辑在computed中处理
    },
    
    // 菜单类型样式
    getMenuType(type) {
      const typeMap = {
        'menu': 'success',   // 菜单
        'button': 'warning'  // 按钮
      }
      return typeMap[type] || 'info'
    },
    
    getMenuTypeText(type) {
      const typeMap = {
        'menu': '菜单',
        'button': '按钮'
      }
      return typeMap[type] || '未知'
    },
    
    // 根据权限ID获取权限代码
    getPermissionCode(permissionId) {
      if (!permissionId) return ''
      
      const permission = this.permissionList.find(p => p.id === permissionId)
      if (permission) {
        return permission.code || permission.permissionCode || permission.permission_code || permission.name
      }
      return ''
    },
    
    // 可见性切换
    async handleVisibleChange(menu) {
      try {
        await UpdateMenu(menu.id, { visible: menu.visible })
        this.$message.success('更新成功')
      } catch (error) {
        console.error('更新可见性失败:', error)
        this.$message.error('更新失败')
        menu.visible = !menu.visible // 恢复原状态
      }
    },
    
    // 创建菜单
    handleCreateMenu() {
      this.dialogTitle = '新增菜单'
      this.dialogVisible = true
      this.currentMenuId = null
      this.$nextTick(() => {
        this.$refs.menuForm?.resetFields()
        this.menuForm = {
          name: '',
          parentId: null,
          path: '',
          component: '',
          icon: '',
          sort: 0,
          type: 0,
          visible: true,
          description: ''
        }
      })
    },
    
    // 编辑菜单
    handleEditMenu(menu) {
      this.dialogTitle = '编辑菜单'
      this.dialogVisible = true
      this.currentMenuId = menu.id
      this.menuForm = { ...menu }
    },
    
    // 权限配置
    handlePermissionConfig(menu) {
      this.$router.push({
        path: '/f_systemmanage/permission/permission',
        query: { activeTab: 'permission', menuId: menu.id }
      })
    },
    
    // 删除菜单
    handleDeleteMenu(menu) {
      if (menu.children && menu.children.length > 0) {
        this.$message.warning('请先删除子菜单')
        return
      }
      
      this.$confirm(`确定要删除菜单"${menu.name}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await DeleteMenu(menu.id)
          this.$message.success('删除成功')
          this.refreshData()
        } catch (error) {
          console.error('删除菜单失败:', error)
          this.$message.error('删除菜单失败')
        }
      }).catch(() => {})
    },
    
    // 对话框关闭
    handleDialogClose() {
      this.$refs.menuForm?.resetFields()
      this.currentMenuId = null
    },
    
    // 提交表单
    async handleSubmit() {
      try {
        const valid = await this.$refs.menuForm.validate()
        if (!valid) return
        
        this.submitting = true
        
        if (this.currentMenuId) {
          // 编辑菜单
          await UpdateMenu(this.currentMenuId, this.menuForm)
          this.$message.success('更新菜单成功')
        } else {
          // 新增菜单
          await CreateMenu(this.menuForm)
          this.$message.success('创建菜单成功')
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
.menu-management {
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

.menu-name {
  display: flex;
  align-items: center;
}

.table-card {
  margin-bottom: 20px;
}
</style>