<template>
  <div>
    <div class="tab-toolbar">
      <div class="tab-toolbar__left">
        <el-button v-if="canManage" type="primary" size="small" @click="dialogVisible = true">添加成员</el-button>
        <el-button size="small" @click="$emit('refresh')">刷新成员</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="members" border stripe size="small">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="userId" label="用户 ID" width="120" />
      <el-table-column prop="roleCode" label="角色" width="120" />
      <el-table-column prop="grantedBy" label="授权人" width="120" />
      <el-table-column label="创建时间" min-width="180">
        <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template slot-scope="{ row }">
          <el-button
            type="text"
            size="small"
            :disabled="row.roleCode === 'OWNER' || !canManage"
            @click="$emit('remove-member', row)"
          >
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      title="添加成员"
      :visible.sync="dialogVisible"
      width="520px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="memberFormRef" :model="memberForm" :rules="memberRules" label-width="100px">
        <el-form-item label="用户 ID" prop="userId">
          <el-input-number v-model="memberForm.userId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>

        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="memberForm.roleCode" style="width: 100%">
            <el-option label="EDITOR" value="EDITOR" />
            <el-option label="VIEWER" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canManage" @click="submit">添加</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'KnowledgeBaseMemberTab',
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    saving: {
      type: Boolean,
      default: false
    },
    members: {
      type: Array,
      default: () => []
    },
    canManage: {
      type: Boolean,
      default: false
    },
    formatTime: {
      type: Function,
      default: value => value
    }
  },
  data() {
    return {
      dialogVisible: false,
      memberForm: {
        userId: null,
        roleCode: 'EDITOR'
      },
      memberRules: {
        userId: [{ required: true, message: '请输入用户 ID', trigger: 'change' }],
        roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },
  methods: {
    resetForm() {
      this.memberForm = {
        userId: null,
        roleCode: 'EDITOR'
      }
      if (this.$refs.memberFormRef) {
        this.$refs.memberFormRef.resetFields()
      }
    },

    submit() {
      if (!this.$refs.memberFormRef) return
      this.$refs.memberFormRef.validate(valid => {
        if (!valid) return
        this.$emit('add-member', { ...this.memberForm }, () => {
          this.dialogVisible = false
          this.resetForm()
        })
      })
    }
  }
}
</script>

<style scoped>
.tab-toolbar,
.tab-toolbar__left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tab-toolbar {
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
