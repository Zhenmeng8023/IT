<template>
  <div class="menu-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>菜单管理</h1>
      <p>管理系统菜单配置，支持菜单的增删改查和排序</p>
    </div>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="handleAddMenu">
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
        
        <el-table-column prop="type" label="菜单类型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.type === 'menu' ? 'primary' : 'success'">
              {{ scope.row.type === 'menu' ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="parentId" label="父级菜单" width="120" align="center">
          <template slot-scope="scope">
            {{ scope.row.parentId === 0 ? '根菜单' : getParentName(scope.row.parentId) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="sortOrder" label="排序序号" width="150" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.sortOrder"
              :min="0"
              :max="999"
              size="mini"
              @change="handleSortChange(scope.row)">
            </el-input-number>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(scope.row)">
            </el-switch>
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
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-plus"
              @click="handleAddSubMenu(scope.row)"
              v-if="scope.row.type === 'menu'">
              添加子菜单
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              style="color: #F56C6C;">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑菜单对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="600px"
      @close="handleDialogClose">
      
      <el-form ref="menuForm" :model="menuForm" :rules="rules" label-width="80px">
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="menuForm.type" @change="handleTypeChange">
            <el-radio label="menu">菜单</el-radio>
            <el-radio label="button">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="父级菜单" prop="parentId">
          <el-select v-model="menuForm.parentId" placeholder="请选择父级菜单" clearable>
            <el-option label="根菜单" :value="0"></el-option>
            <el-option
              v-for="menu in parentMenuOptions"
              :key="menu.id"
              :label="menu.name"
              :value="menu.id">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="menuForm.name" placeholder="请输入菜单名称"></el-input>
        </el-form-item>
        
        <el-form-item label="菜单路径" prop="path">
          <el-input v-model="menuForm.path" placeholder="请输入菜单路径"></el-input>
        </el-form-item>
        
        <el-form-item label="菜单图标" prop="icon">
          <el-input v-model="menuForm.icon" placeholder="请输入图标类名，如：el-icon-menu">
            <template slot="prepend">
              <i :class="menuForm.icon" v-if="menuForm.icon"></i>
              <span v-else>图标</span>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number
            v-model="menuForm.sortOrder"
            :min="0"
            :max="999"
            style="width: 100%">
          </el-input-number>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="menuForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            :rows="3"
            v-model="menuForm.remark"
            placeholder="请输入备注信息">
          </el-input>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Menu',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      // 搜索关键词
      searchKeyword: '',
      // 菜单列表数据
      menuList: [],
      // 过滤后的菜单列表
      filteredMenuList: [],
      // 对话框控制
      dialogVisible: false,
      dialogType: 'add', // 'add' 或 'edit'
      // 菜单表单
      menuForm: {
        id: null,
        name: '',
        path: '',
        icon: '',
        type: 'menu',
        parentId: 0,
        sortOrder: 0,
        status: 1,
        remark: ''
      },
      // 表单验证规则
      rules: {
        name: [
          { required: true, message: '请输入菜单名称', trigger: 'blur' },
          { min: 2, max: 20, message: '菜单名称长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        path: [
          { required: true, message: '请输入菜单路径', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择菜单类型', trigger: 'change' }
        ],
        sortOrder: [
          { required: true, message: '请输入排序序号', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.dialogType === 'add' ? '新增菜单' : '编辑菜单'
    },
    
    // 父级菜单选项（只包含菜单类型，不包含按钮）
    parentMenuOptions() {
      return this.menuList.filter(menu => menu.type === 'menu' && menu.status === 1)
    }
  },
  mounted() {
    this.fetchMenuList()
  },
  methods: {
    // 获取菜单列表
    async fetchMenuList() {
      this.loading = true
      try {
        // 模拟数据
        this.menuList = [
          {
            id: 1,
            name: '系统管理',
            path: '/system',
            icon: 'el-icon-s-tools',
            type: 'menu',
            parentId: 0,
            sortOrder: 1,
            status: 1,
            remark: '系统管理模块',
            createTime: '2024-01-01 00:00:00',
            children: [
              {
                id: 2,
                name: '用户管理',
                path: '/usermanage',
                icon: 'el-icon-user',
                type: 'menu',
                parentId: 1,
                sortOrder: 1,
                status: 1,
                remark: '用户管理菜单',
                createTime: '2024-01-01 00:00:00',
                children: [
                  {
                    id: 3,
                    name: '账户管理',
                    path: '/count',
                    icon: 'el-icon-s-custom',
                    type: 'menu',
                    parentId: 2,
                    sortOrder: 1,
                    status: 1,
                    remark: '账户管理页面',
                    createTime: '2024-01-01 00:00:00'
                  },
                  {
                    id: 4,
                    name: '用户信息管理',
                    path: '/info',
                    icon: 'el-icon-document',
                    type: 'menu',
                    parentId: 2,
                    sortOrder: 2,
                    status: 1,
                    remark: '用户信息管理页面',
                    createTime: '2024-01-01 00:00:00'
                  },
                  {
                    id: 5,
                    name: '角色权限管理',
                    path: '/rolelimit',
                    icon: 'el-icon-lock',
                    type: 'menu',
                    parentId: 2,
                    sortOrder: 3,
                    status: 1,
                    remark: '角色权限管理页面',
                    createTime: '2024-01-01 00:00:00'
                  }
                ]
              },
              {
                id: 6,
                name: '菜单管理',
                path: '/menu',
                icon: 'el-icon-menu',
                type: 'menu',
                parentId: 1,
                sortOrder: 2,
                status: 1,
                remark: '菜单管理页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 7,
                name: '日志管理',
                path: '/log',
                icon: 'el-icon-notebook-1',
                type: 'menu',
                parentId: 1,
                sortOrder: 3,
                status: 1,
                remark: '日志管理页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 8,
                name: '标签管理',
                path: '/label',
                icon: 'el-icon-collection-tag',
                type: 'menu',
                parentId: 1,
                sortOrder: 4,
                status: 1,
                remark: '标签管理页面',
                createTime: '2024-01-01 00:00:00'
              }
            ]
          },
          {
            id: 9,
            name: '博客管理',
            path: '/blog',
            icon: 'el-icon-notebook-2',
            type: 'menu',
            parentId: 0,
            sortOrder: 2,
            status: 1,
            remark: '博客管理模块',
            createTime: '2024-01-01 00:00:00',
            children: [
              {
                id: 10,
                name: '审核',
                path: '/audit',
                icon: 'el-icon-check',
                type: 'menu',
                parentId: 9,
                sortOrder: 1,
                status: 1,
                remark: '博客审核页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 11,
                name: '仪表盘',
                path: '/dashboard',
                icon: 'el-icon-data-analysis',
                type: 'menu',
                parentId: 9,
                sortOrder: 2,
                status: 1,
                remark: '博客仪表盘页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 12,
                name: '推荐算法',
                path: '/algoreco',
                icon: 'el-icon-cpu',
                type: 'menu',
                parentId: 9,
                sortOrder: 3,
                status: 1,
                remark: '推荐算法页面',
                createTime: '2024-01-01 00:00:00'
              }
            ]
          },
          {
            id: 13,
            name: '项目管理',
            path: '/project',
            icon: 'el-icon-s-management',
            type: 'menu',
            parentId: 0,
            sortOrder: 3,
            status: 1,
            remark: '项目管理模块',
            createTime: '2024-01-01 00:00:00',
            children: [
              {
                id: 14,
                name: '项目审核',
                path: '/projectaudit',
                icon: 'el-icon-document-checked',
                type: 'menu',
                parentId: 13,
                sortOrder: 1,
                status: 1,
                remark: '项目审核页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 15,
                name: '项目下架',
                path: '/projectmiss',
                icon: 'el-icon-remove',
                type: 'menu',
                parentId: 13,
                sortOrder: 2,
                status: 1,
                remark: '项目下架页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 16,
                name: '推荐算法',
                path: '/projectalgoreco',
                icon: 'el-icon-cpu',
                type: 'menu',
                parentId: 13,
                sortOrder: 3,
                status: 1,
                remark: '项目推荐算法页面',
                createTime: '2024-01-01 00:00:00'
              }
            ]
          },
          {
            id: 17,
            name: '圈子管理',
            path: '/circle',
            icon: 'el-icon-s-promotion',
            type: 'menu',
            parentId: 0,
            sortOrder: 4,
            status: 1,
            remark: '圈子管理模块',
            createTime: '2024-01-01 00:00:00',
            children: [
              {
                id: 18,
                name: '好友',
                path: '/circlefriend',
                icon: 'el-icon-service',
                type: 'menu',
                parentId: 17,
                sortOrder: 1,
                status: 1,
                remark: '好友管理页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 19,
                name: '圈子审核',
                path: '/circleaudit',
                icon: 'el-icon-check',
                type: 'menu',
                parentId: 17,
                sortOrder: 2,
                status: 1,
                remark: '圈子审核页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 20,
                name: '圈子分类',
                path: '/circlesort',
                icon: 'el-icon-s-operation',
                type: 'menu',
                parentId: 17,
                sortOrder: 3,
                status: 1,
                remark: '圈子分类页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 21,
                name: '圈子管理',
                path: '/circlemanage',
                icon: 'el-icon-s-management',
                type: 'menu',
                parentId: 17,
                sortOrder: 4,
                status: 1,
                remark: '圈子管理页面',
                createTime: '2024-01-01 00:00:00'
              },
              {
                id: 22,
                name: '官方圈子详细管理',
                path: '/circleofficial',
                icon: 'el-icon-office-building',
                type: 'menu',
                parentId: 17,
                sortOrder: 5,
                status: 1,
                remark: '官方圈子详细管理页面',
                createTime: '2024-01-01 00:00:00'
              }
            ]
          }
        ]
        
        this.filteredMenuList = this.menuList
      } catch (error) {
        console.error('获取菜单列表失败:', error)
        this.$message.error('获取菜单列表失败')
      } finally {
        this.loading = false
      }
    },
    
    // 搜索菜单
    handleSearch() {
      if (!this.searchKeyword) {
        this.filteredMenuList = this.menuList
        return
      }
      
      const keyword = this.searchKeyword.toLowerCase()
      const filterMenu = (menus) => {
        const result = []
        menus.forEach(menu => {
          const match = menu.name.toLowerCase().includes(keyword) || 
                       menu.path.toLowerCase().includes(keyword)
          
          if (match) {
            result.push({ ...menu })
          } else if (menu.children && menu.children.length > 0) {
            const filteredChildren = filterMenu(menu.children)
            if (filteredChildren.length > 0) {
              result.push({
                ...menu,
                children: filteredChildren
              })
            }
          }
        })
        return result
      }
      
      this.filteredMenuList = filterMenu(this.menuList)
    },
    
    // 获取父级菜单名称
    getParentName(parentId) {
      if (parentId === 0) return '根菜单'
      const findParent = (menus, targetId) => {
        for (const menu of menus) {
          if (menu.id === targetId) return menu.name
          if (menu.children) {
            const result = findParent(menu.children, targetId)
            if (result) return result
          }
        }
        return '未知'
      }
      return findParent(this.menuList, parentId)
    },
    
    // 新增菜单
    handleAddMenu() {
      this.dialogType = 'add'
      this.menuForm = {
        id: null,
        name: '',
        path: '',
        icon: '',
        type: 'menu',
        parentId: 0,
        sortOrder: 0,
        status: 1,
        remark: ''
      }
      this.dialogVisible = true
    },
    
    // 添加子菜单
    handleAddSubMenu(parentMenu) {
      this.dialogType = 'add'
      this.menuForm = {
        id: null,
        name: '',
        path: '',
        icon: '',
        type: 'menu',
        parentId: parentMenu.id,
        sortOrder: 0,
        status: 1,
        remark: ''
      }
      this.dialogVisible = true
    },
    
    // 编辑菜单
    handleEdit(menu) {
      this.dialogType = 'edit'
      this.menuForm = { ...menu }
      this.dialogVisible = true
    },
    
    // 删除菜单
    handleDelete(menu) {
      this.$confirm(`确定要删除菜单 "${menu.name}" 吗？此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          // 模拟删除操作
          const deleteMenu = (menus, targetId) => {
            return menus.filter(menu => {
              if (menu.id === targetId) return false
              if (menu.children) {
                menu.children = deleteMenu(menu.children, targetId)
              }
              return true
            })
          }
          
          this.menuList = deleteMenu(this.menuList, menu.id)
          this.filteredMenuList = this.menuList
          this.$message.success('菜单删除成功')
        } catch (error) {
          this.$message.error('菜单删除失败')
        }
      }).catch(() => {})
    },
    
    // 菜单类型改变
    handleTypeChange(type) {
      if (type === 'button') {
        this.menuForm.parentId = 0
      }
    },
    
    // 排序改变
    handleSortChange(menu) {
      // 模拟保存排序
      this.$message.success(`菜单 "${menu.name}" 排序已更新为 ${menu.sortOrder}`)
    },
    
    // 状态改变
    handleStatusChange(menu) {
      const statusText = menu.status === 1 ? '启用' : '禁用'
      this.$message.success(`菜单 "${menu.name}" 已${statusText}`)
    },
    
    // 表单提交
    async handleSubmit() {
      this.$refs.menuForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            if (this.dialogType === 'add') {
              // 新增菜单
              const newMenu = {
                id: Date.now(),
                ...this.menuForm,
                createTime: new Date().toISOString(),
                children: []
              }
              
              // 添加到菜单列表
              if (newMenu.parentId === 0) {
                this.menuList.push(newMenu)
              } else {
                const addToParent = (menus, parentId, newMenu) => {
                  for (const menu of menus) {
                    if (menu.id === parentId) {
                      if (!menu.children) menu.children = []
                      menu.children.push(newMenu)
                      return true
                    }
                    if (menu.children) {
                      if (addToParent(menu.children, parentId, newMenu)) return true
                    }
                  }
                  return false
                }
                addToParent(this.menuList, newMenu.parentId, newMenu)
              }
              
              this.$message.success('菜单新增成功')
            } else {
              // 编辑菜单
              const updateMenu = (menus, updatedMenu) => {
                for (const menu of menus) {
                  if (menu.id === updatedMenu.id) {
                    Object.assign(menu, updatedMenu)
                    return true
                  }
                  if (menu.children) {
                    if (updateMenu(menu.children, updatedMenu)) return true
                  }
                }
                return false
              }
              
              updateMenu(this.menuList, this.menuForm)
              this.$message.success('菜单信息更新成功')
            }
            
            this.dialogVisible = false
            this.filteredMenuList = this.menuList
          } catch (error) {
            this.$message.error('操作失败')
          } finally {
            this.submitLoading = false
          }
        }
      })
    },
    
    // 对话框关闭
    handleDialogClose() {
      this.$refs.menuForm.clearValidate()
    },
    
    // 刷新数据
    refreshData() {
      this.$message.success('数据刷新成功')
      this.fetchMenuList()
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
.menu-management {
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

.menu-name {
  display: flex;
  align-items: center;
}

.dialog-footer {
  text-align: right;
}
</style>