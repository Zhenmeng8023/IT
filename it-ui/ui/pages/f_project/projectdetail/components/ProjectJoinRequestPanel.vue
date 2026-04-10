<template>
  <div v-if="visiblePanel" class="join-request-entry">
    <el-button
      v-if="status.hasPendingInvite && status.pendingInviteCode"
      type="success"
      size="small"
      icon="el-icon-circle-check"
      :loading="accepting"
      @click="handleAcceptInvite"
    >
      接受邀请
    </el-button>

    <el-button
      v-else-if="hasPendingJoinRequest"
      type="warning"
      size="small"
      icon="el-icon-time"
      @click="openStatusDialog"
    >
      申请审核中
    </el-button>

    <el-button
      v-else-if="canReapply"
      type="danger"
      size="small"
      icon="el-icon-refresh-left"
      @click="openApplyDialog"
    >
      重新申请
    </el-button>

    <el-button
      v-else-if="requestApprovedButNotJoined"
      type="success"
      size="small"
      icon="el-icon-refresh"
      @click="refreshAll"
    >
      已通过，刷新加入
    </el-button>

    <el-button
      v-else
      type="primary"
      size="small"
      icon="el-icon-plus"
      @click="openApplyDialog"
    >
      加入项目
    </el-button>

    <el-dialog
      title="申请加入项目"
      :visible.sync="applyDialogVisible"
      width="520px"
      append-to-body
      @close="resetForm"
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="申请角色">
          <el-select v-model="form.desiredRole" style="width: 100%">
            <el-option label="成员" value="member" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input
            v-model="form.applyMessage"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请简要说明你想加入这个项目的原因"
          />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交申请
        </el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="申请状态"
      :visible.sync="statusDialogVisible"
      width="480px"
      append-to-body
    >
      <div v-if="status.hasPendingInvite && status.pendingInviteCode" class="join-dialog-content">
        <el-alert
          title="你有一个待接受的项目邀请"
          type="success"
          :closable="false"
          show-icon
        />
        <div class="join-dialog-desc">邀请码：{{ status.pendingInviteCode }}</div>
      </div>

      <div v-else-if="hasPendingJoinRequest" class="join-dialog-content">
        <el-alert
          title="你的加入申请正在审核中"
          type="warning"
          :closable="false"
          show-icon
        />
        <div class="join-dialog-desc">
          申请提交后需等待管理员或所有者处理，你也可以先撤销后重新填写。
        </div>
      </div>

      <div v-else-if="canReapply" class="join-dialog-content">
        <el-alert
          title="你的加入申请已被拒绝"
          type="error"
          :closable="false"
          show-icon
        />
        <div class="join-dialog-desc">
          {{ rejectedReasonText }}
        </div>
      </div>

      <div v-else-if="requestApprovedButNotJoined" class="join-dialog-content">
        <el-alert
          title="你的加入申请已通过"
          type="success"
          :closable="false"
          show-icon
        />
        <div class="join-dialog-desc">
          成员状态正在同步，刷新后即可进入项目工作台。
        </div>
      </div>

      <span slot="footer">
        <el-button v-if="hasPendingJoinRequest" :loading="canceling" @click="handleCancelRequest">
          撤销申请
        </el-button>
        <el-button v-if="canReapply" type="primary" @click="switchToApplyDialog">
          重新申请
        </el-button>
        <el-button v-if="requestApprovedButNotJoined" type="primary" @click="handleRefreshAndClose">
          刷新
        </el-button>
        <el-button @click="statusDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getProjectMemberStatus } from '@/api/project'
import { acceptProjectInvite } from '@/api/projectInvite'
import {
  submitProjectJoinRequest,
  getMyProjectJoinRequestStatus,
  cancelProjectJoinRequest
} from '@/api/projectJoinRequest'

export default {
  name: 'ProjectJoinRequestPanel',
  props: {
    projectId: {
      type: [Number, String],
      default: null
    },
    currentUserId: {
      type: [Number, String],
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      accepting: false,
      submitting: false,
      canceling: false,
      applyDialogVisible: false,
      statusDialogVisible: false,
      status: {
        member: false,
        canApplyJoin: false,
        hasPendingJoinRequest: false,
        hasPendingInvite: false,
        pendingInviteCode: ''
      },
      myJoinRequest: null,
      form: {
        desiredRole: 'member',
        applyMessage: ''
      }
    }
  },
  computed: {
    visiblePanel() {
      return !!this.projectId && !!this.currentUserId && !this.status.member
    },
    hasPendingJoinRequest() {
      return !!(this.myJoinRequest && this.myJoinRequest.status === 'pending')
    },
    canReapply() {
      return !!(this.myJoinRequest && this.myJoinRequest.status === 'rejected')
    },
    requestApprovedButNotJoined() {
      return !!(this.myJoinRequest && this.myJoinRequest.status === 'approved' && !this.status.member)
    },
    rejectedReasonText() {
      if (!this.myJoinRequest) return '你可以重新填写说明后再次申请'
      return this.myJoinRequest.reviewMessage || '你可以重新填写说明后再次申请'
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.refreshAll()
      }
    },
    currentUserId() {
      this.refreshAll()
    }
  },
  methods: {
    resetForm() {
      this.form = {
        desiredRole: 'member',
        applyMessage: ''
      }
    },
    openApplyDialog() {
      this.resetForm()
      this.applyDialogVisible = true
    },
    openStatusDialog() {
      this.statusDialogVisible = true
    },
    switchToApplyDialog() {
      this.statusDialogVisible = false
      this.openApplyDialog()
    },
    async handleRefreshAndClose() {
      await this.refreshAll()
      this.statusDialogVisible = false
      this.$emit('changed')
    },
    async refreshAll() {
      if (!this.projectId || !this.currentUserId) return
      try {
        const [memberRes, myRequestRes] = await Promise.all([
          getProjectMemberStatus(this.projectId),
          getMyProjectJoinRequestStatus(this.projectId).catch(() => ({ data: null }))
        ])
        this.status = memberRes?.data || this.status
        this.myJoinRequest = myRequestRes?.data || null
      } catch (error) {
        console.error(error)
      }
    },
    async handleAcceptInvite() {
      if (!this.status.pendingInviteCode) return
      this.accepting = true
      try {
        await acceptProjectInvite(this.status.pendingInviteCode, {
          inviteCode: this.status.pendingInviteCode
        })
        this.$message.success('已接受项目邀请')
        await this.refreshAll()
        this.$emit('changed')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '接受邀请失败')
      } finally {
        this.accepting = false
      }
    },
    async handleSubmit() {
      this.submitting = true
      try {
        await submitProjectJoinRequest(this.projectId, this.form)
        this.$message.success('加入申请已提交')
        this.applyDialogVisible = false
        await this.refreshAll()
        this.$emit('changed')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '加入申请提交失败')
      } finally {
        this.submitting = false
      }
    },
    async handleCancelRequest() {
      if (!this.myJoinRequest || !this.myJoinRequest.id) return
      this.canceling = true
      try {
        await cancelProjectJoinRequest(this.myJoinRequest.id)
        this.$message.success('已撤销申请')
        await this.refreshAll()
        this.statusDialogVisible = false
        this.$emit('changed')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '撤销申请失败')
      } finally {
        this.canceling = false
      }
    }
  }
}
</script>

<style scoped>
.join-request-entry {
  display: inline-flex;
  align-items: center;
}

.join-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.join-dialog-desc {
  color: #909399;
  line-height: 1.8;
  font-size: 13px;
}
</style>