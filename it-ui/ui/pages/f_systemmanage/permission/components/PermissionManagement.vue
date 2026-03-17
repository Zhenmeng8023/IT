<template>
  <div class="permission-management">
    <div class="header">
      <h2>权限管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索权限名称或代码"
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
            <p><strong>创建时间：</strong>{{ formatDate(props.row.createTime) }}</p>
            <p><strong>更新时间：</strong>{{ formatDate(props.row.updateTime) }}</p>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
      
      <el-table-column prop="name" label="权限名称" min-width="150">
        <template slot-scope="scope">
          <el-tag :type="getPermissionType(scope.row.type)" size="medium">
            {{ scope.row.name }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="code" label="权限代码" min-width="150">
        <template slot-scope="scope">
          <code style="background: #f5f7fa; padding: 2px 6px; border-radius: 3px;">
            {{ scope.row.code }}
          </code>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="权限类型" width="120" align="center">
        <template slot-scope="scope">
          <el-tag :type="getPermissionTypeTag(scope.row.type)" size="small">
            {{ getPermissionTypeText(scope.row.type) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="resource" label="资源路径" min-width="200">
        <template slot-scope="scope">
          <span v-if="scope.row.resource">{{ scope.row.resource }}</span>
          <span v-else style="color: #c0c4cc;">无</span>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="80" align="center">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            @change="handleStatusChange(scope.row)"
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
        
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permissionForm.name" placeholder="请输入权限名称"></el-input>
        </el-form-item>

        <el-form-item label="权限代码" prop="code">
          <el-input v-model="permissionForm.code" placeholder="请输入权限代码，如：user:read">
            <template slot="prepend">
              <el-select v-model="permissionForm.type" placeholder="类型" style="width: 120px;">
                <el-option label="菜单" value="menu"></el-option>
                <el-option label="按钮" value="button"></el-option>
                <el-option label="接口" value="api"></el-option>
              </el-select>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="权限类型" prop="type">
          <el-radio-group v-model="permissionForm.type">
            <el-radio label="menu">菜单权限</el-radio>
            <el-radio label="button">按钮权限</el-radio>
            <el-radio label="api">接口权限</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="资源路径" prop="resource">
          <el-input v-model="permissionForm.resource" placeholder="请输入资源路径，如：/api/user"></el-input>
        </el-form-item>

        <el-form-item label="权限描述" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="permissionForm.description"
            placeholder="请输入权限描述">
          </el-input>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="permissionForm.status"
            active-text="启用"
            inactive-text="禁用">
          </el-switch>
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
  name: 'PermissionManagement',
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
        name: '',
        code: '',
        type: 'menu',
        resource: '',
        description: '',
        status: true
      },
      
      rules: {
        name: [
          { required: true, message: '请输入权限名称', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入权限代码', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择权限类型', trigger: 'change' }
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
        permission.name.toLowerCase().includes(keyword) ||
        permission.code.toLowerCase().includes(keyword) ||
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
        
        this.permissionList = permissionData
        console.log('处理后的权限列表:', this.permissionList)
        
      } catch (error) {
        console.error('获取权限列表失败:', error)
        this.$message.error('获取权限列表失败: ' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },

    // 权限类型样式
    getPermissionType(type) {
      const typeMap = {
        'menu': 'primary',
        'button': 'success',
        'api': 'warning'
      }
      return typeMap[type] || 'info'
    },

    getPermissionTypeTag(type) {
      const typeMap = {
        'menu': 'primary',
        'button': 'success',
        'api': 'warning'
      }
      return typeMap[type] || 'info'
    },

    getPermissionTypeText(type) {
      const typeMap = {
        'menu': '菜单权限',
        'button': '按钮权限',
        'api': '接口权限'
      }
      return typeMap[type] || '未知'
    },

    // 日期格式化
    formatDate(date) {
      if (!date) return '未知'
      return new Date(date).toLocaleString('zh-CN')
    },

    // 新增权限
    handleAddPermission() {
      this.dialogTitle = '新增权限'
      this.currentPermissionId = null
      this.permissionForm = {
        name: '',
        code: '',
        type: 'menu',
        resource: '',
        description: '',
        status: true
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
        name: permission.name,
        code: permission.code,
        type: permission.type,
        resource: permission.resource || '',
        description: permission.description || '',
        status: permission.status !== false
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

    // 状态切换
    async handleStatusChange(permission) {
      try {
        await UpdatePermission(permission.id, { status: permission.status })
        this.$message.success('状态更新成功')
      } catch (error) {
        console.error('状态更新失败:', error)
        permission.status = !permission.status // 回滚状态
        this.$message.error('状态更新失败: ' + (error.message || '网络错误'))
      }
    },

    // 提交表单
    async handleSubmit() {
      try {
        const valid = await this.$refs.permissionForm.validate()
        if (!valid) return
        
        this.submitting = true
        
        const formData = {
          name: this.permissionForm.name,
          code: this.permissionForm.code,
          type: this.permissionForm.type,
          resource: this.permissionForm.resource,
          description: this.permissionForm.description,
          status: this.permissionForm.status
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
}

.header {
  display: flex;
  justify-content: space-between;
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
</style>