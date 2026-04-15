<template>
  <el-dialog title="编辑项目信息" :visible.sync="showEditDialogProxy" width="640px" append-to-body>
    <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="editForm.name" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="项目描述" prop="description">
        <el-input v-model="editForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="项目分类" prop="category">
        <el-select v-model="editForm.category" style="width: 100%" placeholder="请选择分类">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="项目状态" prop="status">
        <el-select v-model="editForm.status" style="width: 100%" placeholder="请选择状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="可见性" prop="visibility">
        <el-radio-group v-model="editForm.visibility">
          <el-radio-button label="public">公开</el-radio-button>
          <el-radio-button label="private">私有</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="项目标签">
        <el-input v-model="editForm.tagsText" placeholder="多个标签请用逗号分隔，如：Vue,SpringBoot,AI" />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="showEditDialogProxy = false">取消</el-button>
      <el-button type="primary" :loading="saveLoading" @click="handleSubmit">保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ProjectEditDialog',
  props: {
    showEditDialog: {
      type: Boolean,
      default: false
    },
    editForm: {
      type: Object,
      default: () => ({})
    },
    editRules: {
      type: Object,
      default: () => ({})
    },
    categoryOptions: {
      type: Array,
      default: () => []
    },
    statusOptions: {
      type: Array,
      default: () => []
    },
    saveLoading: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    showEditDialogProxy: {
      get() {
        return this.showEditDialog
      },
      set(value) {
        this.$emit('update:showEditDialog', value)
      }
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.editFormRef.validate((valid) => {
        if (!valid) return
        this.$emit('submit')
      })
    }
  }
}
</script>
