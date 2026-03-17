<template>
  <div class="permission-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>权限管理</h1>
      <p>管理系统权限，支持权限的增删改查和状态管理</p>
    </div>

    <div class="header">
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索权限代码或描述"
          style="width: 300px; margin-right: 10px;"
          clearable>
          <el-button slot="append" icon="el-icon-search"></el-button>
        </el-input>
        <el-button type="primary" icon="el-icon-plus" @click="handleAddPermission">
          新增权限
        </el-button>
      </div>
    </div>

    <el-table
      :data="filteredPermissionList"
      v-loading="loading"
      style="width: 100%; margin-top: 20px;"
      border>
      
      <el-table-column type="expand">
        <template slot-scope="props">
          <div class="expand-content">
            <p><strong>权限描述：</strong>{{ props.row.description || '无描述' }}</p>
            <p><strong>创建时间：</strong>{{ formatDate(props.row.createdAt) }}</p>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
      
      <el-table-column prop="permissionCode" label="权限代码" min-width="150">
        <template slot-scope="scope">
          <code style="background: #f5f7fa; padding: 2px 6px; border-radius: 3px;">
            {{ scope.row.permissionCode }}
          </code>
        </template>
      </el-table-column>

      <el-table-column prop="description" label="权限描述" min-width="200">
        <template slot-scope="scope">
          <span>{{ scope.row.description || '无描述' }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
        <template slot-scope="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleEditPermission(scope.row)">
            编辑
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            style="color: #f56c6c;"
            @click="handleDeletePermission(scope.row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 权限编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="600px"
      :before-close="handleDialogClose">
      
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
    </el-dialog>
  </div>
</template>

<script>
import { GetAllPermissions, CreatePermission, UpdatePermission, DeletePermission } from '~/api/index'

export default {
  name: 'Permission',
  layout: 'manage',
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
        
        // 标准化权限数据
        this.permissionList = permissionData.map(permission => ({
          id: permission.id,
          permissionCode: permission.permissionCode,
          description: permission.description || '',
          createdAt: permission.createdAt
        }))
        console.log('处理后的权限列表:', this.permissionList)
        console.log('处理后的权限列表:', this.permissionList)
        
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

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.expand-content {
  padding: 10px;
  background: #f8f9fa;
  border-radius: 4px;
}

.expand-content p {
  margin: 5px 0;
  color: #666;
}

code {
  font-family: 'Courier New', monospace;
  color: #e83e8c;
}

.dialog-footer {
  text-align: right;
}
</style>