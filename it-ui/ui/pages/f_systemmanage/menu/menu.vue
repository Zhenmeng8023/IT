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

    <!-- 菜单列表 - 树形表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="filteredMenuList"
        v-loading="loading"
        row-key="id"
        :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
        :default-expand-all="false"
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
            {{ scope.row.parentId === 0 || scope.row.parentId === null ? '根菜单' : getParentName(scope.row.parentId) }}
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序序号" width="150" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.sortOrder"
              :min="0"
              :max="999"
              size="mini"
              @change="handleSortChange(scope.row)"
              @blur="handleSortBlur(scope.row)">
            </el-input-number>
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

      <el-form ref="menuForm" :model="menuForm" :rules="rules" label-width="100px" class="beautified-form">
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="menuForm.type" @change="handleTypeChange" class="type-radio-group">
            <el-radio label="menu">菜单</el-radio>
            <el-radio label="button">按钮</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 父级菜单：改为级联选择器 -->
        <el-form-item label="父级菜单" prop="parentId">
          <el-cascader
            v-model="cascaderValue"
            :options="cascaderParentOptions"
            :props="cascaderProps"
            placeholder="请选择父级菜单"
            clearable
            change-on-select
            @change="handleCascaderChange"
            class="beautified-cascader">
          </el-cascader>
        </el-form-item>

        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="menuForm.name" placeholder="请输入菜单名称" class="beautified-input"></el-input>
        </el-form-item>

        <el-form-item label="菜单路径" prop="path" v-if="menuForm.type === 'menu'">
          <el-input v-model="menuForm.path" placeholder="请输入菜单路径" class="beautified-input"></el-input>
        </el-form-item>

        <el-form-item label="组件路径" prop="component" v-if="menuForm.type === 'menu'">
          <el-input v-model="menuForm.component" placeholder="请输入组件路径" class="beautified-input"></el-input>
        </el-form-item>

        <el-form-item label="菜单图标" prop="icon">
          <el-popover
            placement="bottom-start"
            title="选择图标"
            width="420"
            trigger="click"
            v-model="iconPopoverVisible"
            popper-class="beautified-popover">
            <div class="icon-selector">
              <div
                v-for="icon in iconList"
                :key="icon"
                class="icon-item"
                @click="selectIcon(icon)">
                <i :class="icon"></i>
                <span class="icon-text">{{ icon }}</span>
              </div>
            </div>
            <el-input
              slot="reference"
              v-model="menuForm.icon"
              placeholder="请输入图标类名，如：el-icon-menu"
              class="beautified-input icon-input">
              <template slot="prepend">
                <i :class="menuForm.icon" v-if="menuForm.icon" class="icon-preview"></i>
                <span v-else class="icon-placeholder">图标</span>
              </template>
            </el-input>
          </el-popover>
        </el-form-item>

        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number
            v-model="menuForm.sortOrder"
            :min="0"
            :max="999"
            class="beautified-input-number">
          </el-input-number>
        </el-form-item>

        <el-form-item label="状态" prop="isHidden">
          <el-radio-group v-model="menuForm.isHidden" class="status-radio-group">
            <el-radio :label="false">显示</el-radio>
            <el-radio :label="true">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="权限代码" prop="permissionId">
          <el-select v-model="menuForm.permissionId" placeholder="请选择权限" clearable class="beautified-select">
            <el-option
              v-for="permission in permissions"
              :key="permission.id"
              :label="permission.permissionCode || permission.permission_code || permission.code || permission.name || permission.id"
              :value="permission.id"
              class="beautified-option">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            :rows="3"
            v-model="menuForm.remark"
            placeholder="请输入备注信息"
            class="beautified-textarea">
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
      // 菜单列表数据（树形结构）
      menuList: [],
      // 过滤后的菜单列表（树形）
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
      // 级联选择器的值（存储选中的路径数组）
      cascaderValue: [],
      // 级联选择器配置
      cascaderProps: {
        value: 'id',
        label: 'name',
        children: 'children',
        checkStrictly: true, // 允许选择任意一级
        emitPath: true
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

    // 级联选择器选项（构建完整的菜单树，只包含菜单类型，排除按钮）
    cascaderParentOptions() {
      // 复制原始菜单树，并过滤掉按钮节点（按钮不能作为父级）
      const filterButton = (menus) => {
        if (!menus || !Array.isArray(menus)) return []
        return menus
          .filter(menu => menu.type === 'menu') // 只保留菜单类型
          .map(menu => ({
            ...menu,
            children: filterButton(menu.children)
          }))
      }

      let menuTree = filterButton(this.menuList)
      
      // 如果是编辑模式，需要排除当前编辑的菜单及其后代
      if (this.dialogType === 'edit' && this.menuForm.id) {
        const excludeNodeAndDescendants = (nodes, excludeId) => {
          return nodes
            .filter(node => node.id !== excludeId)
            .map(node => ({
              ...node,
              children: excludeNodeAndDescendants(node.children || [], excludeId)
            }))
            .filter(node => {
              // 如果节点被过滤掉，其子节点也被过滤
              if (node.id === excludeId) return false
              return true
            })
        }
        menuTree = excludeNodeAndDescendants(menuTree, this.menuForm.id)
      }

      // 添加根菜单选项（顶级）
      const rootOption = {
        id: 0,
        name: '根菜单',
        children: menuTree
      }
      return [rootOption]
    }
  },
  mounted() {
    this.fetchMenuList()
    this.fetchPermissions()
  },
  methods: {
    // ================= 树形数据处理 =================
    /**
     * 将扁平菜单列表转换为树形结构
     * @param {Array} flatMenus 扁平菜单数组
     * @returns {Array} 树形菜单数组
     */
    buildMenuTree(flatMenus) {
      if (!flatMenus || !Array.isArray(flatMenus)) return []
      
      const map = new Map()
      const roots = []
      
      // 先建立 id -> 节点的映射
      flatMenus.forEach(menu => {
        map.set(menu.id, { ...menu, children: [] })
      })
      
      // 构建树结构
      flatMenus.forEach(menu => {
        const node = map.get(menu.id)
        const parentId = menu.parentId === 0 || menu.parentId === null ? 0 : menu.parentId
        if (parentId === 0) {
          roots.push(node)
        } else {
          const parent = map.get(parentId)
          if (parent) {
            parent.children.push(node)
          } else {
            // 如果父节点不存在，作为根节点处理
            roots.push(node)
          }
        }
      })
      
      // 对每个节点的子节点按 sortOrder 排序
      const sortChildren = (nodes) => {
        nodes.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        nodes.forEach(node => {
          if (node.children && node.children.length) {
            sortChildren(node.children)
          }
        })
      }
      sortChildren(roots)
      
      return roots
    },

    // 对树形菜单进行排序（递归）
    sortMenus(menus) {
      if (!menus || !Array.isArray(menus)) return []
      
      const sortedMenus = menus.sort((a, b) => {
        const orderA = a.sortOrder || 0
        const orderB = b.sortOrder || 0
        return orderA - orderB
      })
      
      sortedMenus.forEach(menu => {
        if (menu.children && Array.isArray(menu.children)) {
          menu.children = this.sortMenus(menu.children)
        }
      })
      
      return sortedMenus
    },

    // 根据节点id查找其在树中的路径（用于级联选择器回显）
    findPathById(tree, targetId, path = []) {
      for (const node of tree) {
        const newPath = [...path, node.id]
        if (node.id === targetId) {
          return newPath
        }
        if (node.children && node.children.length) {
          const found = this.findPathById(node.children, targetId, newPath)
          if (found) return found
        }
      }
      return null
    },

    // 获取所有菜单（扁平或树形，后端可能返回扁平）
    async fetchMenuList() {
      this.loading = true
      try {
        const response = await GetAllMenus()
        console.log('获取菜单列表响应:', response)

        if (response && response.data && Array.isArray(response.data)) {
          // 标准化字段名
          const normalizedMenus = response.data.map(menu => ({
            ...menu,
            permissionId: menu.permissionId !== undefined ? menu.permissionId : menu.permission_id || null,
            parentId: menu.parentId !== undefined ? menu.parentId : menu.parent_id || 0,
            sortOrder: menu.sortOrder !== undefined ? menu.sortOrder : menu.sort_order || 0,
            isHidden: menu.isHidden !== undefined ? menu.isHidden : menu.is_hidden || false,
            createdAt: menu.createdAt !== undefined ? menu.createdAt : menu.created_at || null
          }))
          
          // 构建树形结构（如果后端返回的是扁平数据）
          this.menuList = this.buildMenuTree(normalizedMenus)
        } else {
          this.$message.error('获取菜单列表失败: 数据格式错误')
          this.menuList = []
        }

        this.filteredMenuList = this.menuList
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
        const response = await GetAllPermissions()
        console.log('获取权限列表响应:', response)

        if (response && response.data && Array.isArray(response.data)) {
          this.permissions = response.data
        } else {
          this.permissions = []
        }
      } catch (error) {
        console.error('获取权限列表失败:', error)
        this.permissions = []
      }
    },

    // 搜索菜单（递归过滤树形结构）
    handleSearch() {
      if (!this.searchKeyword) {
        this.filteredMenuList = this.menuList
        return
      }

      const keyword = this.searchKeyword.toLowerCase()
      const filterTree = (menus) => {
        const result = []
        menus.forEach(menu => {
          const match = (menu.name && menu.name.toLowerCase().includes(keyword)) ||
                       (menu.path && menu.path.toLowerCase().includes(keyword)) ||
                       (menu.component && menu.component.toLowerCase().includes(keyword)) ||
                       (menu.remark && menu.remark.toLowerCase().includes(keyword))

          if (match) {
            result.push({ ...menu })
          } else if (menu.children && menu.children.length > 0) {
            const filteredChildren = filterTree(menu.children)
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

      this.filteredMenuList = filterTree(this.menuList)
    },

    // 获取父级菜单名称（在表格中显示）
    getParentName(parentId) {
      if (parentId === 0 || parentId === null) return '根菜单'
      
      const findParentName = (menus, targetId) => {
        for (const menu of menus) {
          if (menu.id === targetId) return menu.name
          if (menu.children) {
            const result = findParentName(menu.children, targetId)
            if (result) return result
          }
        }
        return '未知'
      }
      return findParentName(this.menuList, parentId)
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
        createdAt: new Date().toISOString(),
        remark: ''
      }
      // 默认选中根菜单
      this.cascaderValue = [0]
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
        createdAt: new Date().toISOString(),
        remark: ''
      }
      // 构建级联路径（从根到当前父菜单）
      const path = this.findPathById(this.menuList, parentMenu.id)
      if (path) {
        this.cascaderValue = path
      } else {
        this.cascaderValue = [0, parentMenu.id] // 后备方案
      }
      this.dialogVisible = true
    },

    // 编辑菜单
    handleEdit(menu) {
      this.dialogType = 'edit'
      this.menuForm = {
        ...menu,
        isHidden: menu.isHidden !== undefined ? menu.isHidden : false,
        permissionId: menu.permissionId !== undefined ? menu.permissionId : null
      }
      // 构建父级菜单的级联路径
      if (menu.parentId && menu.parentId !== 0) {
        const path = this.findPathById(this.menuList, menu.parentId)
        if (path) {
          this.cascaderValue = path
        } else {
          this.cascaderValue = [menu.parentId]
        }
      } else {
        this.cascaderValue = [0]
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
          await DeleteMenu(menu.id)
          // 重新获取列表以更新树形结构
          await this.fetchMenuList()
          this.$message.success('菜单删除成功')
        } catch (error) {
          console.error('菜单删除失败:', error)
          const errorMessage = error.response?.data?.message || error.message || '网络错误'
          this.$message.error('菜单删除失败: ' + errorMessage)
        }
      }).catch(() => {})
    },

    // 菜单类型改变
    handleTypeChange(type) {
      if (type === 'button') {
        // 按钮类型强制设为根菜单，清空路径
        this.menuForm.parentId = 0
        this.cascaderValue = [0]
        this.menuForm.path = ''
        this.menuForm.component = ''
      }
      this.$refs.menuForm.validate()
    },

    // 级联选择器值改变
    handleCascaderChange(value) {
      if (value && value.length > 0) {
        // 取最后一项作为 parentId
        const parentId = value[value.length - 1]
        this.menuForm.parentId = parentId === 0 ? 0 : parentId
      } else {
        this.menuForm.parentId = 0
        this.cascaderValue = [0]
      }
    },

    // 排序改变
    async handleSortChange(menu) {
      try {
        const menuData = {
          name: menu.name,
          path: menu.type === 'button' ? null : menu.path,
          component: menu.type === 'button' ? null : menu.component,
          icon: menu.icon,
          type: menu.type,
          parentId: menu.parentId === 0 ? null : menu.parentId,
          sortOrder: menu.sortOrder,
          isHidden: menu.isHidden,
          permissionId: menu.permissionId,
          remark: menu.remark
        }
        await UpdateMenu(menu.id, menuData)
        this.$message.success(`菜单 "${menu.name}" 排序已更新为 ${menu.sortOrder}`)
        await this.fetchMenuList() // 刷新列表以重新排序
      } catch (error) {
        console.error('排序更新失败:', error)
        const errorMessage = error.response?.data?.message || error.message || '网络错误'
        this.$message.error('排序更新失败: ' + errorMessage)
        // 刷新恢复正确排序
        await this.fetchMenuList()
      }
    },

    // 排序输入框失去焦点时刷新本地排序
    handleSortBlur(menu) {
      this.filteredMenuList = this.sortMenus(this.filteredMenuList)
    },

    // 提交表单
    async handleSubmit() {
      this.$refs.menuForm.validate(async (valid) => {
        if (valid) {
          this.submitLoading = true
          try {
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

            if (this.dialogType === 'add') {
              await CreateMenu(menuData)
              this.$message.success('菜单新增成功')
            } else {
              await UpdateMenu(this.menuForm.id, menuData)
              this.$message.success('菜单信息更新成功')
            }
            await this.fetchMenuList()
            this.dialogVisible = false
          } catch (error) {
            console.error('操作失败:', error)
            const errorMessage = error.response?.data?.message || error.message || '网络错误'
            this.$message.error('操作失败: ' + errorMessage)
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
      return permission.permissionCode || 
             permission.permission_code || 
             permission.code || 
             permission.permission || 
             permission.name || 
             permissionId
    },

    // 格式化日期
    formatDate(date) {
      if (!date) return ''
      try {
        const dateObj = new Date(date)
        if (isNaN(dateObj.getTime())) return ''
        return dateObj.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (error) {
        return ''
      }
    }
  }
}
</script>

<style scoped>
/* 样式保持不变 */
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
  max-height: 240px;
  overflow-y: auto;
  padding: 12px;
  background-color: #fafbfc;
  border-radius: 0 0 8px 8px;
}
.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #ffffff;
}
.icon-item:hover {
  border-color: #3b82f6;
  background-color: #eff6ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}
.icon-item i {
  font-size: 24px;
  margin-bottom: 8px;
  color: #4b5563;
}
.icon-item span {
  font-size: 11px;
  color: #6b7280;
  text-align: center;
  word-break: break-all;
  line-height: 1.3;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
}

</style>

<style>
/* 下拉菜单清晰度优化 - 重点改进选项区分度 */

/* 图标选择器弹窗样式优化 */
.beautified-popover {
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  border: 1px solid #dcdfe6;
  background: #ffffff;
  overflow: hidden;
}

.beautified-popover .el-popover__title {
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding: 12px 16px;
  margin-bottom: 0;
  font-size: 14px;
  background: #f5f7fa;
  border-radius: 8px 8px 0 0;
}

/* 级联选择器下拉菜单样式优化 - 更清晰的分隔 */
.el-cascader__dropdown {
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  border: 1px solid #dcdfe6;
  background: #ffffff;
  overflow: hidden;
}

.el-cascader-panel {
  border-radius: 8px;
  background: #ffffff;
}

.el-cascader-menu {
  padding: 8px 0;
  border-right: 1px solid #ebeef5;
  min-width: 160px;
  background: #ffffff;
}

.el-cascader-menu:last-child {
  border-right: none;
}

.el-cascader-node {
  padding: 0 16px;
  height: 36px;
  line-height: 36px;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #606266;
  position: relative;
  font-weight: 500;
  border-bottom: 1px solid #f5f7fa;
}

.el-cascader-node:hover {
  background: #f5f7fa;
  color: #303133;
  font-weight: 600;
}

.el-cascader-node.in-active-path,
.el-cascader-node.is-active {
  color: #409eff;
  font-weight: 600;
  background: #ecf5ff;
}

.el-cascader-node__prefix {
  margin-right: 8px;
  font-size: 14px;
}

.el-cascader-node__postfix {
  margin-left: 8px;
}

/* 选择器下拉菜单样式优化 - 更清晰的选项分隔 */
.el-select-dropdown {
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  border: 1px solid #dcdfe6;
  background: #ffffff;
  max-height: 280px;
  overflow-y: auto;
}

.el-select-dropdown__list {
  padding: 4px 0;
}

.el-select-dropdown__item {
  padding: 0 16px;
  height: 36px;
  line-height: 36px;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #606266;
  position: relative;
  display: flex;
  align-items: center;
  font-weight: 500;
  border-bottom: 1px solid #f5f7fa;
}

.el-select-dropdown__item:hover {
  background: #f5f7fa;
  color: #303133;
  font-weight: 600;
}

.el-select-dropdown__item.selected {
  color: #409eff;
  font-weight: 600;
  background: #ecf5ff;
}

.el-select-dropdown__item span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: inherit;
}

/* 滚动条优化 */
.el-select-dropdown::-webkit-scrollbar,
.el-cascader__dropdown::-webkit-scrollbar,
.beautified-popover::-webkit-scrollbar {
  width: 6px;
}

.el-select-dropdown::-webkit-scrollbar-track,
.el-cascader__dropdown::-webkit-scrollbar-track,
.beautified-popover::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.el-select-dropdown::-webkit-scrollbar-thumb,
.el-cascader__dropdown::-webkit-scrollbar-thumb,
.beautified-popover::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.el-select-dropdown::-webkit-scrollbar-thumb:hover,
.el-cascader__dropdown::-webkit-scrollbar-thumb:hover,
.beautified-popover::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
</style>