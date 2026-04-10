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
      type="warning"
      size="small"
      icon="el-icon-warning-outline"
      :loading="refreshingApprovedState"
      @click="handleRefreshApprovedState"
    >
      状态待同步
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
      width="500px"
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
          title="申请已通过，但成员状态未同步"
          type="warning"
          :closable="false"
          show-icon
        />
        <div class="join-dialog-desc">
          当前并未识别到你已经成为项目成员。你可以先刷新状态；如果刷新后仍未加入，请联系项目管理员检查成员关系是否真正落库。
        </div>
      </div>

      <span slot="footer">
        <el-button v-if="hasPendingJoinRequest" :loading="canceling" @click="handleCancelRequest">
          撤销申请
        </el-button>
        <el-button v-if="canReapply || (requestApprovedButNotJoined && status.canApplyJoin)" type="primary" @click="switchToApplyDialog">
          重新申请
        </el-button>
        <el-button v-if="requestApprovedButNotJoined" type="warning" :loading="refreshingApprovedState" @click="handleRefreshApprovedState">
          刷新状态
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
      refreshingApprovedState: false,
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
      if (!this.myJoinRequest || this.myJoinRequest.status !== 'approved') return false
      if (this.status.member) return false
      if (this.status.canApplyJoin) return false
      return true
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
      if (this.status.member) {
        this.$message.success('你已经是项目成员')
        return
      }
      if (this.status.hasPendingInvite) {
        this.statusDialogVisible = true
        return
      }
      if (this.hasPendingJoinRequest) {
        this.statusDialogVisible = true
        return
      }
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
    async handleRefreshApprovedState() {
      this.refreshingApprovedState = true
      try {
        await this.refreshAll()
        if (this.status.member) {
          this.$message.success('成员状态已同步，你现在可以进入项目工作台')
          this.statusDialogVisible = false
          this.$emit('changed')
          return
        }
        if (this.status.canApplyJoin) {
          this.$message.warning('当前成员状态未同步，且系统允许重新发起申请，你可以重新申请或联系管理员检查')
          this.statusDialogVisible = false
          return
        }
        this.$message.warning('申请已通过，但成员关系仍未同步，请联系项目管理员检查成员数据')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '刷新状态失败')
      } finally {
        this.refreshingApprovedState = false
      }
    },
    async refreshAll() {
      if (!this.projectId || !this.currentUserId) {
        this.status = {
          member: false,
          canApplyJoin: false,
          hasPendingJoinRequest: false,
          hasPendingInvite: false,
          pendingInviteCode: ''
        }
        this.myJoinRequest = null
        return
      }
      try {
        const [memberRes, myRequestRes] = await Promise.all([
          getProjectMemberStatus(this.projectId),
          getMyProjectJoinRequestStatus(this.projectId).catch(() => ({ data: null }))
        ])
        this.status = memberRes?.data || {
          member: false,
          canApplyJoin: false,
          hasPendingJoinRequest: false,
          hasPendingInvite: false,
          pendingInviteCode: ''
        }
        this.myJoinRequest = myRequestRes?.data || null
        if (this.status.member) {
          this.myJoinRequest = null
        }
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
      if (this.status.member) {
        this.$message.success('你已经是项目成员')
        this.applyDialogVisible = false
        return
      }
      if (this.status.hasPendingInvite) {
        this.$message.warning('你有待接受的项目邀请，请优先接受邀请')
        this.applyDialogVisible = false
        this.statusDialogVisible = true
        return
      }
      if (this.hasPendingJoinRequest) {
        this.$message.warning('你已有待审核的加入申请，请勿重复提交')
        this.applyDialogVisible = false
        this.statusDialogVisible = true
        return
      }
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
