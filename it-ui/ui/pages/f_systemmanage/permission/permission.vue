<template>
  <div class="permission-management">
    <AdminPageHeader title="权限管理" description="管理系统权限，支持权限项增删改查。" />

    <AdminToolbarCard class="toolbar-card">
      <div class="header permission-toolbar">
        <div class="header-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索权限代码或描述"
            style="width: 320px;"
            clearable
            @input="handleSearch">
            <el-button slot="append" icon="el-icon-search"></el-button>
          </el-input>
          <el-button v-permission="'btn:permission:create'" type="primary" icon="el-icon-plus" @click="handleAddPermission">
            新增权限
          </el-button>
        </div>
      </div>
    </AdminToolbarCard>

    <AdminTableCard class="table-card">
      <el-table
        class="admin-table admin-permission-table"
        :data="permissionList"
        v-loading="loading"
        style="width: 100%;"
        stripe>
        <el-table-column type="expand">
          <template slot-scope="props">
            <div class="expand-content">
              <p><strong>权限描述：</strong>{{ props.row.description || '无描述' }}</p>
              <p><strong>创建时间：</strong>{{ formatDate(props.row.createdAt) }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>

        <el-table-column prop="permissionCode" label="权限代码" min-width="220" show-overflow-tooltip>
          <template slot-scope="scope">
            <CodeTag :value="scope.row.permissionCode">
              {{ scope.row.permissionCode }}
            </CodeTag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="权限描述" min-width="240" show-overflow-tooltip>
          <template slot-scope="scope">
            <span class="permission-desc" :title="scope.row.description">{{ scope.row.description || '无描述' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template slot-scope="scope">
            <span class="date-text">{{ formatDate(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="176" align="center">
          <template slot-scope="scope">
            <AdminActionGroup class="table-actions table-actions--permission">
              <el-button
                v-permission="'btn:permission:edit'"
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleEditPermission(scope.row)">
                编辑
              </el-button>
              <el-button
                v-permission="'btn:permission:delete'"
                class="danger-action"
                size="mini"
                type="text"
                @click="handleDeletePermission(scope.row)">
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
      width="600px"
      :loading="submitting"
      :before-close="handleDialogClose"
      @confirm="handleSubmit">
      <el-form
        ref="permissionForm"
        :model="permissionForm"
        :rules="rules"
        label-width="100px">
        <el-form-item label="权限代码" prop="permissionCode">
          <el-input v-model="permissionForm.permissionCode" placeholder="请输入权限代码，如：user:read"></el-input>
        </el-form-item>

        <el-form-item label="权限描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="permissionForm.description"
            placeholder="请输入权限描述">
          </el-input>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </div>
    </AdminFormDialog>
  </div>
</template>

<script>
import {
  createAdminPermission,
  deleteAdminPermission,
  getAdminPermissionPage,
  updateAdminPermission
} from '@/api/admin-rbac'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminToolbarCard from '@/components/admin/AdminToolbarCard.vue'
import AdminTableCard from '@/components/admin/AdminTableCard.vue'
import AdminActionGroup from '@/components/admin/AdminActionGroup.vue'
import CodeTag from '@/components/admin/CodeTag.vue'
import AdminFormDialog from '@/components/admin/AdminFormDialog.vue'

export default {
  name: 'Permission',
  layout: 'manage',
  components: {
    AdminPageHeader,
    AdminToolbarCard,
    AdminTableCard,
    AdminActionGroup,
    CodeTag,
    AdminFormDialog
  },
  data() {
    return {
      loading: false,
      permissionList: [],
      searchKeyword: '',
      currentPage: 1,
      pageSize: 10,
      total: 0,

      dialogVisible: false,
      dialogTitle: '',
      submitting: false,
      currentPermissionId: null,
      permissionForm: {
        permissionCode: '',
        description: ''
      },
      rules: {
        permissionCode: [
          { required: true, message: '请输入权限代码', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入权限描述', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.refreshData()
  },
  methods: {
    async refreshData() {
      this.loading = true
      try {
        const pageData = await getAdminPermissionPage({
          page: this.currentPage - 1,
          size: this.pageSize,
          keyword: this.searchKeyword?.trim() || undefined
        })
        this.permissionList = pageData.content
        this.total = pageData.totalElements
        this.currentPage = pageData.number + 1
      } catch (error) {
        console.error('获取权限列表失败:', error)
        this.$message.error('获取权限列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },

    handleSearch() {
      this.currentPage = 1
      this.refreshData()
    },

    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.refreshData()
    },

    handleCurrentChange(page) {
      this.currentPage = page
      this.refreshData()
    },

    formatDate(date) {
      if (!date) return '未知'
      const dateObj = new Date(date)
      if (Number.isNaN(dateObj.getTime())) return '未知'
      const year = dateObj.getFullYear()
      const month = String(dateObj.getMonth() + 1).padStart(2, '0')
      const day = String(dateObj.getDate()).padStart(2, '0')
      const hours = String(dateObj.getHours()).padStart(2, '0')
      const minutes = String(dateObj.getMinutes()).padStart(2, '0')
      const seconds = String(dateObj.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    },

    handleAddPermission() {
      this.dialogTitle = '新增权限'
      this.currentPermissionId = null
      this.permissionForm = {
        permissionCode: '',
        description: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.permissionForm?.clearValidate()
      })
    },

    handleEditPermission(permission) {
      this.dialogTitle = '编辑权限'
      this.currentPermissionId = permission.id
      this.permissionForm = {
        permissionCode: permission.permissionCode,
        description: permission.description || ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.permissionForm?.clearValidate()
      })
    },

    async handleDeletePermission(permission) {
      try {
        await this.$confirm(`确定要删除权限 "${permission.permissionCode}" 吗？`, '提示', {
          type: 'warning'
        })
        await deleteAdminPermission(permission.id)
        this.$message.success('删除成功')
        if (this.permissionList.length === 1 && this.currentPage > 1) {
          this.currentPage -= 1
        }
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除权限失败:', error)
          this.$message.error('删除失败: ' + (error.message || '网络错误'))
        }
      }
    },

    async handleSubmit() {
      try {
        const valid = await this.$refs.permissionForm.validate()
        if (!valid) return

        this.submitting = true
        const formData = {
          permissionCode: this.permissionForm.permissionCode,
          description: this.permissionForm.description
        }

        if (this.currentPermissionId) {
          await updateAdminPermission(this.currentPermissionId, formData)
          this.$message.success('权限更新成功')
        } else {
          await createAdminPermission(formData)
          this.$message.success('权限创建成功')
        }

        this.dialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('操作失败: ' + (error.message || '网络错误'))
      } finally {
        this.submitting = false
      }
    },

    handleDialogClose() {
      this.dialogVisible = false
      this.$refs.permissionForm?.clearValidate()
    }
  }
}
</script>

<style scoped>
.permission-management {
  padding: 0;
  width: 100%;
}

.page-header,
.toolbar-card,
.table-card {
  margin-bottom: 18px;
}

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.permission-toolbar {
  margin-bottom: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
  flex-wrap: wrap;
}

.expand-content {
  padding: 14px 16px;
  background: var(--it-soft-gradient);
  border-radius: 12px;
}

.expand-content p {
  margin: 6px 0;
  color: var(--it-text-muted);
}

.permission-desc {
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

.table-actions--permission {
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
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
</style>
