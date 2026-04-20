<template>
  <div class="role-management">
    <AdminPageHeader title="角色管理" description="管理系统角色，支持角色增删改查与权限配置。" />

    <AdminToolbarCard class="toolbar-card">
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
    </AdminToolbarCard>

    <AdminTableCard class="table-card">
      <el-table
        class="admin-table admin-role-table"
        :data="filteredRoleList"
        v-loading="loading"
        stripe
        style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column prop="roleName" label="角色名称" width="180">
          <template slot-scope="scope">
            <span class="role-tag role-tag--inline">
              <StatusTag :type="getRoleType(scope.row.id)" size="medium">
                {{ scope.row.roleName || '未知角色' }}
              </StatusTag>
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="角色描述" min-width="220" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="role-description" :title="scope.row.description">{{ scope.row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template slot-scope="scope">
            <span class="date-text">{{ formatDate(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="updatedAt" label="更新时间" width="180" align="center">
          <template slot-scope="scope">
            <span class="date-text">{{ formatDate(scope.row.updatedAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" align="center">
          <template slot-scope="scope">
            <AdminActionGroup class="table-actions table-actions--role">
              <el-button
                v-permission="'btn:role:edit'"
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleEditRole(scope.row)">
                编辑
              </el-button>
              <el-button
                v-permission="'btn:role:assign-permission'"
                size="mini"
                type="text"
                icon="el-icon-setting"
                @click="handlePermissionConfig(scope.row)">
                权限配置
              </el-button>
              <el-button
                v-permission="'btn:role:delete'"
                class="danger-action"
                size="mini"
                type="text"
                :disabled="scope.row.id === 1"
                @click="handleDeleteRole(scope.row)">
                <i class="el-icon-delete"></i> 删除
              </el-button>
            </AdminActionGroup>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange">
        </el-pagination>
      </div>
    </AdminTableCard>

    <AdminFormDialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      :loading="submitting"
      @close="handleDialogClose"
      @confirm="handleSubmit">
      <el-form ref="roleForm" :model="roleForm" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称"></el-input>
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input
            v-model="roleForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述">
          </el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </AdminFormDialog>

    <el-dialog
      :title="`权限配置 - ${currentRoleName}`"
      :visible.sync="permissionDialogVisible"
      width="760px"
      custom-class="permission-dialog"
      @close="handlePermissionDialogClose">
      <div class="permission-config">
        <div class="permission-tools">
          <el-input
            v-model="treeFilterKeyword"
            clearable
            prefix-icon="el-icon-search"
            placeholder="搜索菜单或权限"
            @input="handlePermissionFilter">
          </el-input>
          <div class="permission-tool-actions">
            <el-button size="mini" icon="el-icon-check" @click="handleCheckAllPermissions">全选</el-button>
            <el-button size="mini" icon="el-icon-close" @click="handleClearPermissionSelection">取消全选</el-button>
            <el-button size="mini" icon="el-icon-arrow-down" @click="setPermissionTreeExpanded(true)">展开全部</el-button>
            <el-button size="mini" icon="el-icon-arrow-up" @click="setPermissionTreeExpanded(false)">收起全部</el-button>
          </div>
        </div>

        <div class="permission-summary">
          <span>已选 {{ selectedPermissionCount }} / {{ allPermissionCount }} 项</span>
          <small>支持父子联动和半选状态</small>
        </div>

        <el-tree
          ref="menuTree"
          class="menu-tree"
          :data="menuList"
          :props="permissionTreeProps"
          :default-expanded-keys="expandedKeys"
          :default-checked-keys="checkedKeys"
          :filter-node-method="filterPermissionNode"
          node-key="id"
          show-checkbox
          @check="handleMenuCheck">
          <template slot-scope="{ node, data }">
            <span class="menu-node">
              <i v-if="data.icon" :class="data.icon" style="margin-right: 8px;"></i>
              {{ node.label }}
              <span v-if="data.type === 'button'" class="button-node-tip">(按钮)</span>
            </span>
          </template>
        </el-tree>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSubmitting" @click="handlePermissionSubmit">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  assignAdminRoleMenus,
  createAdminRole,
  deleteAdminRole,
  getAdminRoleMenus,
  getAdminRolePage,
  listAdminMenus,
  updateAdminRole
} from '@/api/admin-rbac'
import { useUserStore } from '~/store/user'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbarCard from '@/components/admin/AdminToolbarCard.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminActionGroup from '@/components/admin/AdminActionGroup.vue'
import StatusTag from '@/components/admin/StatusTag.vue'
import AdminFormDialog from '@/components/admin/AdminFormDialog.vue'

export default {
  name: 'Role',
  layout: 'manage',
  components: {
    AdminPageHeader,
    AdminToolbarCard,
    AdminTableCard,
    AdminActionGroup,
    StatusTag,
    AdminFormDialog
  },
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

      permissionDialogVisible: false,
      currentRoleIdForPermission: null,
      currentRoleName: '',
      menuList: [],
      expandedKeys: [],
      checkedKeys: [],
      halfCheckedKeys: [],
      treeFilterKeyword: '',
      permissionTreeProps: {
        children: 'children',
        label: 'label'
      },
      permissionSubmitting: false
    }
  },
  computed: {
    filteredRoleList() {
      return this.roleList
    },
    selectedPermissionCount() {
      return this.checkedKeys.length + this.halfCheckedKeys.length
    },
    allPermissionCount() {
      return this.getAllMenuIds(this.menuList).length
    }
  },
  mounted() {
    this.refreshData()
  },
  methods: {
    async refreshData() {
      this.loading = true
      try {
        const pageData = await getAdminRolePage({
          page: this.currentPage - 1,
          size: this.pageSize,
          keyword: this.searchKeyword?.trim() || undefined
        })
        this.roleList = pageData.content
        this.total = pageData.totalElements
        this.currentPage = pageData.number + 1
      } catch (error) {
        console.error('获取角色列表失败:', error)
        this.$message.error('获取角色列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.currentPage = 1
      this.refreshData()
    },

    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.refreshData()
    },

    handleCurrentChange(val) {
      this.currentPage = val
      this.refreshData()
    },

    getRoleType(id) {
      const typeMap = {
        1: 'danger',
        2: 'warning',
        3: 'primary',
        4: 'success'
      }
      return typeMap[id] || 'info'
    },

    formatDate(date) {
      if (!date) return ''
      const dateObj = new Date(date)
      if (Number.isNaN(dateObj.getTime())) {
        return ''
      }
      const year = dateObj.getFullYear()
      const month = String(dateObj.getMonth() + 1).padStart(2, '0')
      const day = String(dateObj.getDate()).padStart(2, '0')
      const hours = String(dateObj.getHours()).padStart(2, '0')
      const minutes = String(dateObj.getMinutes()).padStart(2, '0')
      const seconds = String(dateObj.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },

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

    handleEditRole(role) {
      this.dialogTitle = '编辑角色'
      this.dialogVisible = true
      this.currentRoleId = role.id
      this.$nextTick(() => {
        this.roleForm = {
          roleName: role.roleName || '',
          description: role.description || ''
        }
        this.$refs.roleForm?.clearValidate()
      })
    },

    async handlePermissionConfig(role) {
      this.permissionDialogVisible = true
      this.currentRoleIdForPermission = role.id
      this.currentRoleName = role.roleName || ''
      try {
        const [allMenus, roleMenus] = await Promise.all([
          listAdminMenus(),
          getAdminRoleMenus(role.id)
        ])

        this.menuList = this.buildMenuTree(allMenus)
        this.expandedKeys = this.getAllMenuIds(this.menuList)
        this.checkedKeys = roleMenus.map(menu => menu.id)
        this.halfCheckedKeys = []

        this.$nextTick(() => {
          this.$refs.menuTree?.setCheckedKeys(this.checkedKeys)
        })
      } catch (error) {
        console.error('获取权限配置失败:', error)
        this.$message.error('获取权限配置失败: ' + (error.message || '网络错误'))
      }
    },

    buildMenuTree(menus) {
      const menuMap = new Map()
      const roots = []

      menus.forEach(menu => {
        menuMap.set(menu.id, {
          ...menu,
          label: menu.name || `菜单-${menu.id}`,
          children: []
        })
      })

      menuMap.forEach(menu => {
        if (!menu.parentId || menu.parentId === 0) {
          roots.push(menu)
          return
        }
        const parent = menuMap.get(menu.parentId)
        if (parent) {
          parent.children.push(menu)
        } else {
          roots.push(menu)
        }
      })

      return roots
    },

    getAllMenuIds(menus) {
      const ids = []
      const traverse = (node) => {
        ids.push(node.id)
        if (Array.isArray(node.children) && node.children.length > 0) {
          node.children.forEach(traverse)
        }
      }
      menus.forEach(traverse)
      return ids
    },

    handleMenuCheck(data, treeObj) {
      this.checkedKeys = [...treeObj.checkedKeys]
      this.halfCheckedKeys = [...treeObj.halfCheckedKeys]
    },

    handlePermissionFilter() {
      this.$refs.menuTree?.filter(this.treeFilterKeyword)
    },

    filterPermissionNode(value, data) {
      if (!value) return true
      const keyword = String(value).toLowerCase()
      const label = String(data.label || '').toLowerCase()
      const permissionCode = String(data.permissionCode || '').toLowerCase()
      return label.includes(keyword) || permissionCode.includes(keyword)
    },

    handleCheckAllPermissions() {
      const allKeys = this.getAllMenuIds(this.menuList)
      this.$refs.menuTree?.setCheckedKeys(allKeys)
      this.checkedKeys = allKeys
      this.halfCheckedKeys = []
    },

    handleClearPermissionSelection() {
      this.$refs.menuTree?.setCheckedKeys([])
      this.checkedKeys = []
      this.halfCheckedKeys = []
    },

    setPermissionTreeExpanded(expanded) {
      this.expandedKeys = expanded ? this.getAllMenuIds(this.menuList) : []
    },

    async handlePermissionSubmit() {
      this.permissionSubmitting = true
      try {
        await assignAdminRoleMenus(this.currentRoleIdForPermission, this.checkedKeys)
        const userStore = useUserStore()
        await userStore.refreshPermissions()
        this.$message.success('权限配置成功')
        this.permissionDialogVisible = false
      } catch (error) {
        console.error('权限配置失败:', error)
        this.$message.error('权限配置失败: ' + (error.message || '网络错误'))
      } finally {
        this.permissionSubmitting = false
      }
    },

    handlePermissionDialogClose() {
      this.menuList = []
      this.expandedKeys = []
      this.checkedKeys = []
      this.halfCheckedKeys = []
      this.currentRoleIdForPermission = null
      this.currentRoleName = ''
      this.treeFilterKeyword = ''
    },

    handleDeleteRole(role) {
      if (role.id === 1) {
        this.$message.warning('超级管理员角色不可删除')
        return
      }
      this.$confirm(`确定要删除角色 "${role.roleName}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteAdminRole(role.id)
          this.$message.success('删除成功')
          if (this.roleList.length === 1 && this.currentPage > 1) {
            this.currentPage -= 1
          }
          this.refreshData()
        } catch (error) {
          console.error('删除角色失败:', error)
          this.$message.error('删除角色失败')
        }
      }).catch(() => {})
    },

    handleDialogClose() {
      this.$refs.roleForm?.resetFields()
      this.currentRoleId = null
    },

    async handleSubmit() {
      if (!this.$refs.roleForm) {
        this.$message.error('表单初始化失败，请重试')
        return
      }
      try {
        const valid = await this.$refs.roleForm.validate()
        if (!valid) return
        this.submitting = true
        const roleData = {
          roleName: this.roleForm.roleName,
          description: this.roleForm.description
        }
        if (this.currentRoleId) {
          await updateAdminRole(this.currentRoleId, roleData)
          this.$message.success('更新角色成功')
        } else {
          await createAdminRole(roleData)
          this.$message.success('创建角色成功')
        }
        this.dialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('角色操作失败:', error)
        this.$message.error('操作失败: ' + (error.message || '网络错误'))
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.role-management {
  padding: 0;
  width: 100%;
}

.page-header,
.toolbar-card,
.table-card {
  margin-bottom: 18px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.role-tag--inline {
  display: inline-flex;
  align-items: center;
}

.role-description {
  color: var(--it-text-muted);
}

.date-text {
  font-size: 12px;
  color: var(--it-text-subtle);
  white-space: nowrap;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.table-actions--role {
  width: 100%;
  max-width: 288px;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
}

.pagination-container {
  margin-top: 18px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}

.button-node-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.permission-config {
  max-height: 420px;
  overflow-y: auto;
}

.permission-tools {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.permission-tool-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: var(--it-text-subtle);
  font-size: 12px;
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
