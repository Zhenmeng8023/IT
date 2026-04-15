<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    :width="width"
    :before-close="beforeClose"
    :close-on-click-modal="closeOnClickModal"
    @close="$emit('close')">
    <slot />
    <div slot="footer" class="admin-form-dialog__footer">
      <slot v-if="$slots.footer" name="footer" />
      <template v-else>
        <el-button @click="handleCancel">{{ cancelText }}</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">{{ confirmText }}</el-button>
      </template>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'AdminFormDialog',
  props: {
    title: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    width: {
      type: String,
      default: '600px'
    },
    loading: {
      type: Boolean,
      default: false
    },
    confirmText: {
      type: String,
      default: '确定'
    },
    cancelText: {
      type: String,
      default: '取消'
    },
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    beforeClose: {
      type: Function,
      default(done) {
        done()
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    }
  },
  methods: {
    handleCancel() {
      this.$emit('cancel')
      this.dialogVisible = false
    },
    handleConfirm() {
      this.$emit('confirm')
    }
  }
}
</script>

<style scoped>
.admin-form-dialog__footer {
  text-align: right;
}
</style>
