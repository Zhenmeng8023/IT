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
        <el-button v-permission="'btn:menu:create'" type="primary" icon="el-icon-plus" @click="handleAddMenu">
          新增菜单
        </el-button>
        <el-button v-permission="'btn:menu:refresh'" type="primary" icon="el-icon-refresh" @click="refreshData">
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

        <el-table-column prop="component" label="组件路径" min-width="160"></el-table-column>

        <el-table-column prop="icon" label="菜单图标" width="120" align="center">
          <template slot-scope="scope">
            <i :class="scope.row.icon" v-if="scope.row.icon"></i>
            <span v-else>-</span>
          </template>
        </el-table-column>

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

        <el-table-column prop="isHidden" label="状态" width="120" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.isHidden"
              :active-value="true"
              :inactive-value="false"
              @change="handleStatusChange(scope.row)">
            </el-switch>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="权限代码" width="150" align="center">
        <template slot-scope="scope">
          {{ getPermissionCode(scope.row.permissionId) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="'btn:menu:edit'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)">
              编辑
            </el-button>

            <el-button v-permission="'btn:menu:add-child'"
              size="mini"
              type="text"
              icon="el-icon-plus"
              @click="handleAddSubMenu(scope.row)"
              v-if="scope.row.type === 'menu'">
              添加子菜单
            </el-button>

            <el-button v-permission="'btn:menu:delete'"
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

        <el-form-item label="菜单路径" prop="path" v-if="menuForm.type === 'menu'">
          <el-input v-model="menuForm.path" placeholder="请输入菜单路径"></el-input>
        </el-form-item>

        <el-form-item label="组件路径" prop="component" v-if="menuForm.type === 'menu'">
          <el-input v-model="menuForm.component" placeholder="请输入组件路径"></el-input>
        </el-form-item>

        <el-form-item label="菜单图标" prop="icon">
          <el-popover
            placement="bottom"
            title="选择图标"
            width="400"
            trigger="click"
            v-model="iconPopoverVisible">
            <div class="icon-selector">
              <div
                v-for="icon in iconList"
                :key="icon"
                class="icon-item"
                @click="selectIcon(icon)">
                <i :class="icon"></i>
                <span>{{ icon }}</span>
              </div>
            </div>
            <el-input
              slot="reference"
              v-model="menuForm.icon"
              placeholder="请输入图标类名，如：el-icon-menu">
              <template slot="prepend">
                <i :class="menuForm.icon" v-if="menuForm.icon"></i>
                <span v-else>图标</span>
              </template>
            </el-input>
          </el-popover>
        </el-form-item>

        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number
            v-model="menuForm.sortOrder"
            :min="0"
            :max="999"
            style="width: 100%">
          </el-input-number>
        </el-form-item>

        <el-form-item label="状态" prop="isHidden">
          <el-radio-group v-model="menuForm.isHidden">
            <el-radio :label="false">显示</el-radio>
            <el-radio :label="true">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="权限代码" prop="permissionId">
          <el-select v-model="menuForm.permissionId" placeholder="请选择权限">
            <el-option
              v-for="permission in permissions"
              :key="permission.id"
              :label="permission.permissionCode || permission.permission_code || permission.name || permission.id"
              :value="permission.id">
            </el-option>
          </el-select>
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
import {
  GetAllMenus,
  CreateMenu,
  UpdateMenu,
  DeleteMenu,
  GetAllPermissions
} from '@/api/index.js'

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
        component: '',
        icon: '',
        type: 'menu',
        parentId: 0,
        sortOrder: 0,
        isHidden: false,
        permissionId: null,
        createdAt: null,
        remark: ''
      },
      // 图标选择器状态
      iconPopoverVisible: false,
      // 常用图标列表
      iconList: [
        'el-icon-menu',
        'el-icon-setting',
        'el-icon-user',
        'el-icon-document',
        'el-icon-s-operation',
        'el-icon-s-management',
        'el-icon-s-grid',
        'el-icon-s-tools',
        'el-icon-s-marketing',
        'el-icon-s-finance',
        'el-icon-s-custom',
        'el-icon-s-flag',
        'el-icon-s-platform',
        'el-icon-s-release',
        'el-icon-s-home',
        'el-icon-s-fold',
        'el-icon-s-unfold',
        'el-icon-s-order',
        'el-icon-s-shop',
        'el-icon-s-ticket',
        'el-icon-s-claim',
        'el-icon-s-comment',
        'el-icon-s-avatar',
        'el-icon-s-check',
        'el-icon-s-remove',
        'el-icon-s-help',
        'el-icon-s-question',
        'el-icon-s-warning',
        'el-icon-s-info',
        'el-icon-s-success'
      ],
      // 权限列表
      permissions: [],
      // 表单验证规则
      rules: {
        name: [
          { required: true, message: '请输入菜单名称', trigger: 'blur' },
          { min: 2, max: 20, message: '菜单名称长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        path: [
          { required: true, message: '请输入菜单路径', trigger: 'blur', validator: (rule, value, callback) => {
            if (this.menuForm.type === 'menu' && !value) {
              callback(new Error('请输入菜单路径'))
            } else {
              callback()
            }
          }}
        ],
        component: [
          { required: true, message: '请输入组件路径', trigger: 'blur', validator: (rule, value, callback) => {
            if (this.menuForm.type === 'menu' && !value) {
              callback(new Error('请输入组件路径'))
            } else {
              callback()
            }
          }}
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
      // 扁平化菜单树，获取所有菜单选项
      const flattenMenus = (menus) => {
        let result = []
        menus.forEach(menu => {
          // 只添加菜单类型的选项，排除当前编辑的菜单（避免循环引用）
          if (menu.type === 'menu' && menu.id !== this.menuForm.id) {
            result.push(menu)
          }
          if (menu.children && menu.children.length > 0) {
            result = result.concat(flattenMenus(menu.children))
          }
        })
        return result
      }
      return flattenMenus(this.menuList)
    }
  },
  mounted() {
    this.fetchMenuList()
    this.fetchPermissions()
  },
  methods: {
    // 获取菜单列表
    async fetchMenuList() {
      this.loading = true
      try {
        // 调用后端API获取菜单列表
        const response = await GetAllMenus()
        console.log('获取菜单列表响应:', response)

        if (response && response.data && Array.isArray(response.data)) {
          // 为每个菜单项添加permissionId字段的默认值，并确保数据结构与后端一致
          this.menuList = response.data.map(menu => ({
            ...menu,
            permissionId: menu.permissionId !== undefined ? menu.permissionId : menu.permission_id || null,
            parentId: menu.parentId !== undefined ? menu.parentId : menu.parent_id || null
          }))
        } else {
          this.$message.error('获取菜单列表失败: 数据格式错误')
        }

        this.filteredMenuList = this.menuList
        // 在 fetchMenuList 方法中添加
        console.log('菜单列表数据:', this.menuList)
        this.menuList.forEach(menu => {
          console.log('菜单项:', menu)
          console.log('菜单项 createdAt:', menu.createdAt)
          console.log('菜单项 permissionId:', menu.permissionId)
        })

      } catch (error) {
        console.error('获取菜单列表失败:', error)
        this.$message.error('获取菜单列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },

    // 获取所有权限
    async fetchPermissions() {
      try {
        // 调用后端API获取所有权限
        const response = await GetAllPermissions()
        console.log('获取权限列表响应:', response)

        if (response && response.data && Array.isArray(response.data)) {
          this.permissions = response.data
          // 打印权限数据结构，查看是否包含permission_code字段
          console.log('权限数据结构:', this.permissions)
          // 检查第一个权限对象的结构
          if (this.permissions.length > 0) {
            console.log('第一个权限对象:', this.permissions[0])
            console.log('权限对象的所有键:', Object.keys(this.permissions[0]))
          }
        } else {
          console.error('获取权限列表失败: 数据格式错误')
          // 即使获取失败，也确保permissions是一个数组
          this.permissions = []
        }
      } catch (error) {
        console.error('获取权限列表失败:', error)
        // 即使出错，也确保permissions是一个数组
        this.permissions = []
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
          const match = (menu.name && menu.name.toLowerCase().includes(keyword)) ||
                       (menu.path && menu.path.toLowerCase().includes(keyword)) ||
                       (menu.component && menu.component.toLowerCase().includes(keyword)) ||
                       (menu.remark && menu.remark.toLowerCase().includes(keyword))

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
      if (parentId === 0 || parentId === null) return '根菜单'
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
        component: '',
        icon: '',
        type: 'menu',
        parentId: 0,
        sortOrder: 0,
        isHidden: false,
        permissionId: null,
        createdAt: new Date().toISOString(), // 添加当前时间戳
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
        component: '',
        icon: '',
        type: 'menu',
        parentId: parentMenu.id,
        sortOrder: 0,
        isHidden: false,
        permissionId: null,
        createdAt: new Date().toISOString(), // 添加当前时间戳
        remark: ''
      }
      this.dialogVisible = true
    },

    // 编辑菜单
    handleEdit(menu) {
      this.dialogType = 'edit'
      // 确保isHidden和permissionId字段存在
      this.menuForm = {
        ...menu,
        isHidden: menu.isHidden !== undefined ? menu.isHidden : false,
        permissionId: menu.permissionId !== undefined ? menu.permissionId : null
      }
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
          // 调用后端API删除菜单
          const response = await DeleteMenu(menu.id)
          console.log('删除菜单响应:', response)

          // 只要没有抛出错误，就认为删除成功
          // 从本地列表中移除
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
          console.error('菜单删除失败:', error)
          console.error('错误详情:', error.response)
          const errorMessage = error.response?.data?.message || error.message || '网络错误'
          this.$message.error('菜单删除失败: ' + errorMessage)
        }
      }).catch(() => {})
    },

    // 菜单类型改变
    handleTypeChange(type) {
      if (type === 'button') {
        this.menuForm.parentId = 0
        this.menuForm.path = ''
        this.menuForm.component = ''
      }
      // 重新验证表单
      this.$refs.menuForm.validate()
    },

    // 排序改变
    async handleSortChange(menu) {
      try {
        // 调用后端API更新排序
        const response = await UpdateMenu(menu.id, {
          sortOrder: menu.sortOrder
        })
        console.log('更新排序响应:', response)

        // 只要没有抛出错误，就认为更新成功
        this.$message.success(`菜单 "${menu.name}" 排序已更新为 ${menu.sortOrder}`)
      } catch (error) {
        console.error('排序更新失败:', error)
        console.error('错误详情:', error.response)
        const errorMessage = error.response?.data?.message || error.message || '网络错误'
        this.$message.error('排序更新失败: ' + errorMessage)
      }
    },

    // 状态改变
    async handleStatusChange(menu) {
      try {
        // 调用后端API更新状态
        const response = await UpdateMenu(menu.id, {
          isHidden: menu.isHidden
        })
        console.log('更新状态响应:', response)

        // 只要没有抛出错误，就认为更新成功
        const statusText = menu.isHidden ? '隐藏' : '显示'
        this.$message.success(`菜单 "${menu.name}" 已${statusText}`)
      } catch (error) {
        console.error('状态更新失败:', error)
        console.error('错误详情:', error.response)
        const errorMessage = error.response?.data?.message || error.message || '网络错误'
        this.$message.error('状态更新失败: ' + errorMessage)
      }
    },

    // 增强错误处理和调试信息
    async handleSubmit() {
      this.$refs.menuForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
            console.log('提交数据:', this.menuForm) // 添加调试信息
            if (this.dialogType === 'add') {
              // 新增菜单
              // 准备后端需要的数据格式
              const menuData = {
                name: this.menuForm.name,
                path: this.menuForm.type === 'button' ? null : this.menuForm.path,
                component: this.menuForm.type === 'button' ? null : this.menuForm.component,
                icon: this.menuForm.icon,
                type: this.menuForm.type,
                parentId: this.menuForm.parentId === 0 ? null : this.menuForm.parentId,
                sortOrder: this.menuForm.sortOrder,
                isHidden: this.menuForm.isHidden,
                permissionId: this.menuForm.permissionId,
                remark: this.menuForm.remark
              }
              console.log('发送给后端的数据:', menuData)
              try {
                const response = await CreateMenu(menuData)
                console.log('后端响应:', response)

                // 只要没有抛出错误，就认为创建成功
                await this.fetchMenuList()
                this.$message.success('菜单新增成功')
                this.dialogVisible = false
              } catch (addError) {
                console.error('新增菜单失败:', addError)
                console.error('错误详情:', addError.response)
                console.error('错误数据:', addError.response?.data)
                const errorMessage = addError.response?.data?.message || addError.response?.data || addError.message || '网络错误'
                this.$message.error('菜单新增失败: ' + JSON.stringify(errorMessage))
              }
            } else {
              // 编辑菜单
              // 准备后端需要的数据格式
              const menuData = {
                name: this.menuForm.name,
                path: this.menuForm.type === 'button' ? null : this.menuForm.path,
                component: this.menuForm.type === 'button' ? null : this.menuForm.component,
                icon: this.menuForm.icon,
                type: this.menuForm.type,
                parentId: this.menuForm.parentId === 0 ? null : this.menuForm.parentId,
                sortOrder: this.menuForm.sortOrder,
                isHidden: this.menuForm.isHidden,
                permissionId: this.menuForm.permissionId,
                remark: this.menuForm.remark
              }
              console.log('发送给后端的数据:', menuData)
              try {
                const response = await UpdateMenu(this.menuForm.id, menuData)
                console.log('后端响应:', response)

                // 只要没有抛出错误，就认为更新成功
                await this.fetchMenuList()
                this.$message.success('菜单信息更新成功')
                this.dialogVisible = false
              } catch (editError) {
                console.error('编辑菜单失败:', editError)
                console.error('错误详情:', editError.response)
                console.error('错误数据:', editError.response?.data)
                const errorMessage = editError.response?.data?.message || editError.response?.data || editError.message || '网络错误'
                this.$message.error('菜单信息更新失败: ' + JSON.stringify(errorMessage))
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

    // 对话框关闭
    handleDialogClose() {
      this.$refs.menuForm.clearValidate()
      this.iconPopoverVisible = false
    },
    
    // 选择图标
    selectIcon(icon) {
      this.menuForm.icon = icon
      this.iconPopoverVisible = false
    },

    // 刷新数据
    async refreshData() {
      await this.fetchMenuList()
      this.$message.success('数据刷新成功')
    },

    // 根据权限ID获取权限代码
    getPermissionCode(permissionId) {
      if (!permissionId) return '-'
      const permission = this.permissions.find(p => p.id === permissionId)
      if (!permission) return permissionId
      // 增加更多的属性名检查，以适应不同的后端返回格式
      return permission.permissionCode || 
             permission.permission_code || 
             permission.code || 
             permission.permission || 
             permission.name || 
             permissionId
    },

    formatDate(date) {
      console.log('格式化日期:', date)
      if (!date) {
        console.log('日期为空')
        return ''
      }
      try {
        const dateObj = new Date(date)
        if (isNaN(dateObj.getTime())) {
          console.log('无效的日期:', date)
          return ''
        }
        return dateObj.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (error) {
        console.error('日期格式化错误:', error)
        return ''
      }
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

.icon-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-item:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.icon-item i {
  font-size: 24px;
  margin-bottom: 8px;
}

.icon-item span {
  font-size: 12px;
  color: #606266;
  text-align: center;
  word-break: break-all;
}
</style>