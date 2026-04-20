<template>
  <div class="menu-management">
    <AdminPageHeader title="菜单管理" description="管理系统菜单配置，支持菜单增删改查和排序。" />

    <AdminToolbarCard class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button v-permission="'btn:menu:create'" type="primary" icon="el-icon-plus" @click="handleAddMenu">
            新增菜单
          </el-button>
          <el-button v-permission="'btn:menu:refresh'" type="primary" icon="el-icon-refresh" @click="refreshData">
            刷新
          </el-button>
        </div>
        <div class="toolbar-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索菜单名称或路径"
            clearable
            style="width: 220px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
          <el-select v-model="typeFilter" clearable placeholder="类型" style="width: 120px" @change="handleFilterChange">
            <el-option label="菜单" value="menu"></el-option>
            <el-option label="按钮" value="button"></el-option>
          </el-select>
          <el-select v-model="hiddenFilter" clearable placeholder="状态" style="width: 120px" @change="handleFilterChange">
            <el-option label="显示" :value="false"></el-option>
            <el-option label="隐藏" :value="true"></el-option>
          </el-select>
        </div>
      </div>
    </AdminToolbarCard>

    <AdminTableCard class="table-card">
      <el-table class="admin-table admin-menu-table" :data="menuList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="菜单名称" min-width="160">
          <template slot-scope="scope">
            <div class="menu-name">
              <i v-if="scope.row.icon" :class="scope.row.icon" style="margin-right: 8px;"></i>
              <span>{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="path" label="菜单路径" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="path-text" :title="scope.row.path">{{ scope.row.path || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="component" label="组件路径" min-width="150" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="path-text" :title="scope.row.component">{{ scope.row.component || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="icon" label="菜单图标" width="120" align="center">
          <template slot-scope="scope">
            <i v-if="scope.row.icon" :class="scope.row.icon"></i>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="type" label="菜单类型" width="100" align="center">
          <template slot-scope="scope">
            <StatusTag :type="scope.row.type === 'menu' ? 'primary' : 'success'" size="small">
              {{ scope.row.type === 'menu' ? '菜单' : '按钮' }}
            </StatusTag>
          </template>
        </el-table-column>

        <el-table-column prop="parentId" label="父级菜单" width="120" align="center">
          <template slot-scope="scope">
            {{ !scope.row.parentId ? '根菜单' : getParentName(scope.row.parentId) }}
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

        <el-table-column label="创建时间" width="168" align="center">
          <template slot-scope="scope">
            <span class="date-text">{{ formatDate(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="权限代码" width="180" align="center" show-overflow-tooltip>
          <template slot-scope="scope">
            <CodeTag :value="scope.row.permissionCode || '-'">
              {{ scope.row.permissionCode || '-' }}
            </CodeTag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" align="center">
          <template slot-scope="scope">
            <AdminActionGroup class="table-actions table-actions--menu">
              <el-button
                v-permission="'btn:menu:edit'"
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleEdit(scope.row)">
                编辑
              </el-button>

              <el-button
                v-permission="'btn:menu:add-child'"
                size="mini"
                type="text"
                icon="el-icon-plus"
                v-if="scope.row.type === 'menu'"
                @click="handleAddSubMenu(scope.row)">
                添加子菜单
              </el-button>

              <el-button
                v-permission="'btn:menu:delete'"
                class="danger-action"
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)">
                删除
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
      width="600px"
      :loading="submitLoading"
      @close="handleDialogClose"
      @confirm="handleSubmit">
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
                <i v-if="menuForm.icon" :class="menuForm.icon"></i>
                <span v-else>图标</span>
              </template>
            </el-input>
          </el-popover>
        </el-form-item>

        <el-form-item label="排序序号" prop="sortOrder">
          <el-input-number v-model="menuForm.sortOrder" :min="0" :max="999" style="width: 100%"></el-input-number>
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
              :label="permission.permissionCode"
              :value="permission.id">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </div>
    </AdminFormDialog>
  </div>
</template>

<script>
import {
  createAdminMenu,
  deleteAdminMenu,
  getAdminMenuPage,
  listAdminMenus,
  listAdminPermissions,
  updateAdminMenu
} from '@/api/admin-rbac'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbarCard from '@/components/admin/AdminToolbarCard.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminActionGroup from '@/components/admin/AdminActionGroup.vue'
import StatusTag from '@/components/admin/StatusTag.vue'
import CodeTag from '@/components/admin/CodeTag.vue'
import AdminFormDialog from '@/components/admin/AdminFormDialog.vue'

export default {
  name: 'Menu',
  layout: 'manage',
  components: {
    AdminPageHeader,
    AdminToolbarCard,
    AdminTableCard,
    AdminActionGroup,
    StatusTag,
    CodeTag,
    AdminFormDialog
  },
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchKeyword: '',
      typeFilter: '',
      hiddenFilter: null,
      currentPage: 1,
      pageSize: 10,
      total: 0,

      menuList: [],
      allMenus: [],
      permissions: [],

      dialogVisible: false,
      dialogType: 'add',
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
        permissionId: null
      },
      iconPopoverVisible: false,
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
      rules: {
        name: [
          { required: true, message: '请输入菜单名称', trigger: 'blur' },
          { min: 2, max: 20, message: '菜单名称长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        path: [
          { validator: (rule, value, callback) => {
            if (this.menuForm.type === 'menu' && !value) {
              callback(new Error('请输入菜单路径'))
              return
            }
            callback()
          }, trigger: 'blur' }
        ],
        component: [
          { validator: (rule, value, callback) => {
            if (this.menuForm.type === 'menu' && !value) {
              callback(new Error('请输入组件路径'))
              return
            }
            callback()
          }, trigger: 'blur' }
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
    parentMenuOptions() {
      return this.allMenus.filter(menu => menu.type === 'menu' && menu.id !== this.menuForm.id)
    }
  },
  mounted() {
    this.refreshData()
  },
  methods: {
    async refreshData() {
      await Promise.all([this.fetchMenuPage(), this.fetchReferenceData()])
      this.$message.success('数据刷新成功')
    },

    async fetchReferenceData() {
      try {
        const [menus, permissions] = await Promise.all([listAdminMenus(), listAdminPermissions()])
        this.allMenus = menus
        this.permissions = permissions
      } catch (error) {
        console.error('获取菜单引用数据失败:', error)
      }
    },

    async fetchMenuPage() {
      this.loading = true
      try {
        const pageData = await getAdminMenuPage({
          page: this.currentPage - 1,
          size: this.pageSize,
          keyword: this.searchKeyword?.trim() || undefined,
          type: this.typeFilter || undefined,
          isHidden: this.hiddenFilter
        })
        this.menuList = pageData.content
        this.total = pageData.totalElements
        this.currentPage = pageData.number + 1
      } catch (error) {
        console.error('获取菜单列表失败:', error)
        this.$message.error('获取菜单列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.currentPage = 1
      this.fetchMenuPage()
    },

    handleFilterChange() {
      this.currentPage = 1
      this.fetchMenuPage()
    },

    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.fetchMenuPage()
    },

    handleCurrentChange(val) {
      this.currentPage = val
      this.fetchMenuPage()
    },

    getParentName(parentId) {
      const parent = this.allMenus.find(menu => menu.id === parentId)
      return parent ? parent.name : '未知'
    },

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
        permissionId: null
      }
      this.dialogVisible = true
    },

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
        permissionId: null
      }
      this.dialogVisible = true
    },

    handleEdit(menu) {
      this.dialogType = 'edit'
      this.menuForm = {
        id: menu.id,
        name: menu.name || '',
        path: menu.path || '',
        component: menu.component || '',
        icon: menu.icon || '',
        type: menu.type || 'menu',
        parentId: menu.parentId || 0,
        sortOrder: menu.sortOrder ?? 0,
        isHidden: Boolean(menu.isHidden),
        permissionId: menu.permissionId || null
      }
      this.dialogVisible = true
    },

    handleDelete(menu) {
      this.$confirm(`确定要删除菜单 "${menu.name}" 吗？此操作不可恢复。`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(async () => {
        try {
          await deleteAdminMenu(menu.id)
          this.$message.success('菜单删除成功')
          if (this.menuList.length === 1 && this.currentPage > 1) {
            this.currentPage -= 1
          }
          await Promise.all([this.fetchMenuPage(), this.fetchReferenceData()])
        } catch (error) {
          console.error('菜单删除失败:', error)
          this.$message.error('菜单删除失败: ' + (error.message || '网络错误'))
        }
      }).catch(() => {})
    },

    handleTypeChange(type) {
      if (type === 'button') {
        this.menuForm.path = ''
        this.menuForm.component = ''
      }
      this.$refs.menuForm?.validate()
    },

    async handleSortChange(menu) {
      try {
        await updateAdminMenu(menu.id, { sortOrder: menu.sortOrder })
        this.$message.success(`菜单 "${menu.name}" 排序已更新为 ${menu.sortOrder}`)
      } catch (error) {
        console.error('排序更新失败:', error)
        await this.fetchMenuPage()
        this.$message.error('排序更新失败: ' + (error.message || '网络错误'))
      }
    },

    async handleStatusChange(menu) {
      const previous = !menu.isHidden
      try {
        await updateAdminMenu(menu.id, { isHidden: menu.isHidden })
        this.$message.success(`菜单 "${menu.name}" 状态已更新`)
      } catch (error) {
        console.error('状态更新失败:', error)
        menu.isHidden = previous
        this.$message.error('状态更新失败: ' + (error.message || '网络错误'))
      }
    },

    async handleSubmit() {
      this.$refs.menuForm.validate(async (valid) => {
        if (!valid) return
        this.submitLoading = true
        try {
          const payload = {
            name: this.menuForm.name,
            path: this.menuForm.type === 'button' ? null : this.menuForm.path,
            component: this.menuForm.type === 'button' ? null : this.menuForm.component,
            icon: this.menuForm.icon || null,
            type: this.menuForm.type,
            parentId: this.menuForm.parentId === 0 ? null : this.menuForm.parentId,
            sortOrder: this.menuForm.sortOrder,
            isHidden: this.menuForm.isHidden,
            permissionId: this.menuForm.permissionId
          }

          if (this.dialogType === 'add') {
            await createAdminMenu(payload)
            this.$message.success('菜单新增成功')
          } else {
            await updateAdminMenu(this.menuForm.id, payload)
            this.$message.success('菜单信息更新成功')
          }

          this.dialogVisible = false
          await Promise.all([this.fetchMenuPage(), this.fetchReferenceData()])
        } catch (error) {
          console.error('提交菜单失败:', error)
          this.$message.error('操作失败: ' + (error.message || '网络错误'))
        } finally {
          this.submitLoading = false
        }
      })
    },

    handleDialogClose() {
      this.$refs.menuForm?.clearValidate()
      this.iconPopoverVisible = false
    },

    selectIcon(icon) {
      this.menuForm.icon = icon
      this.iconPopoverVisible = false
    },

    formatDate(date) {
      if (!date) return ''
      const dateObj = new Date(date)
      if (Number.isNaN(dateObj.getTime())) return ''
      return dateObj.toLocaleString('zh-CN', {
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

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-wrap: wrap;
}

.menu-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.path-text {
  display: inline-block;
  max-width: 100%;
  color: var(--it-text-muted);
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.table-actions--menu {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, max-content);
  justify-content: center;
  justify-items: center;
  max-width: 288px;
  gap: 6px 8px;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  min-width: 0;
}

.pagination-container {
  margin-top: 18px;
  text-align: right;
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
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  border: 1px solid var(--it-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: var(--it-surface-elevated);
}

.icon-item:hover {
  border-color: var(--it-accent);
  background-color: var(--it-accent-soft);
  transform: translateY(-1px);
}

.icon-item i {
  font-size: 24px;
  margin-bottom: 8px;
}

.icon-item span {
  font-size: 12px;
  color: var(--it-text-muted);
  text-align: center;
  word-break: break-all;
}

@media (max-width: 1280px) {
  .table-actions--menu {
    grid-template-columns: 1fr;
  }
}
</style>
