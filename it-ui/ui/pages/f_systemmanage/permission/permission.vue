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
            clearable>
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
      :data="filteredPermissionList"
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
          <el-button v-permission="'btn:permission:edit'"
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleEditPermission(scope.row)">
            编辑
          </el-button>
          <el-button v-permission="'btn:permission:delete'"
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
    </AdminTableCard>

    <!-- 权限编辑对话框 -->
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
import { GetAllPermissions, CreatePermission, UpdatePermission, DeletePermission } from '~/api/index'
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
  computed: {
    filteredPermissionList() {
      if (!this.searchKeyword) {
        return this.permissionList
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.permissionList.filter(permission => 
        (permission.permissionCode && permission.permissionCode.toLowerCase().includes(keyword)) ||
        (permission.description && permission.description.toLowerCase().includes(keyword))
      )
    }
  },
  mounted() {
    this.refreshData()
  },
  methods: {
    // 获取权限列表
    async refreshData() {
      this.loading = true
      try {
        const response = await GetAllPermissions()
        
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
        
        // 标准化权限数据
        this.permissionList = permissionData.map(permission => ({
          id: permission.id,
          permissionCode: permission.permissionCode,
          description: permission.description || '',
          createdAt: permission.createdAt
        }))
        
      } catch (error) {
        console.error('获取权限列表失败:', error)
        this.$message.error('获取权限列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },



    // 日期格式化
    formatDate(date) {
      if (!date) return '未知'
      try {
        const dateObj = new Date(date)
        if (isNaN(dateObj.getTime())) {
          return '未知'
        }
        const year = dateObj.getFullYear()
        const month = String(dateObj.getMonth() + 1).padStart(2, '0')
        const day = String(dateObj.getDate()).padStart(2, '0')
        const hours = String(dateObj.getHours()).padStart(2, '0')
        const minutes = String(dateObj.getMinutes()).padStart(2, '0')
        const seconds = String(dateObj.getSeconds()).padStart(2, '0')
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
      } catch (error) {
        return '未知'
      }
    },

    // 新增权限
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

    // 编辑权限
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

    // 删除权限
    async handleDeletePermission(permission) {
      try {
        await this.$confirm(`确定要删除权限"${permission.name}"吗？`, '提示', {
          type: 'warning'
        })
        
        await DeletePermission(permission.id)
        this.$message.success('删除成功')
        this.refreshData()
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除权限失败:', error)
          this.$message.error('删除失败: ' + (error.message || '网络错误'))
        }
      }
    },



    // 提交表单
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
          // 更新权限
          await UpdatePermission(this.currentPermissionId, formData)
          this.$message.success('权限更新成功')
        } else {
          // 新增权限
          await CreatePermission(formData)
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

    // 对话框关闭
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

.permission-code-chip {
  display: inline-flex;
  max-width: 100%;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.dialog-footer {
  text-align: right;
}
</style>
