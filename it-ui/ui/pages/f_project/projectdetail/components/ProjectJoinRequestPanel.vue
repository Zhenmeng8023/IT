
<template>
  <el-card v-if="visiblePanel" shadow="never" class="join-request-card">
    <div slot="header" class="join-request-header">
      <span>{{ canManageProject ? '加入申请与邀请' : '加入项目' }}</span>
      <el-button size="mini" type="text" @click="refreshAll">刷新</el-button>
    </div>

    <div v-if="status.member" class="join-status-tip">你已加入当前项目。</div>

    <div v-else>
      <el-alert
        v-if="status.hasPendingInvite && status.pendingInviteCode"
        title="你有一个待接受的项目邀请"
        type="success"
        :closable="false"
        show-icon
      >
        <div class="alert-action-row">
          <span>邀请码：{{ status.pendingInviteCode }}</span>
          <el-button size="mini" type="primary" :loading="accepting" @click="handleAcceptInvite">接受邀请</el-button>
        </div>
      </el-alert>

      <el-alert
        v-else-if="status.hasPendingJoinRequest"
        title="你的加入申请正在审核中"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-form v-else-if="status.canApplyJoin" label-width="90px" class="apply-form">
        <el-form-item label="申请角色">
          <el-select v-model="form.desiredRole" style="width: 220px">
            <el-option label="成员" value="member" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.applyMessage" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">申请加入</el-button>
      </el-form>
    </div>

    <div v-if="canManageProject" class="audit-section">
      <el-divider>待审核申请</el-divider>
      <el-table :data="pendingRequests" border size="mini">
        <el-table-column prop="applicantName" label="申请人" min-width="120" />
        <el-table-column prop="desiredRole" label="申请角色" width="100" />
        <el-table-column prop="applyMessage" label="申请说明" min-width="200" />
        <el-table-column prop="createdAt" label="申请时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleAudit(scope.row, 'approved')">通过</el-button>
            <el-button size="mini" type="danger" @click="handleAudit(scope.row, 'rejected')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="pendingRequests.length === 0" description="暂无待审核申请" />
    </div>
  </el-card>
</template>

<script>
import { getProjectMemberStatus } from '@/api/project'
import { acceptProjectInvite } from '@/api/projectInvite'
import {
  submitProjectJoinRequest,
  listProjectJoinRequests,
  auditProjectJoinRequest
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
      loading: false,
      accepting: false,
      submitting: false,
      status: {
        member: false,
        canApplyJoin: false,
        hasPendingJoinRequest: false,
        hasPendingInvite: false,
        pendingInviteCode: ''
      },
      pendingRequests: [],
      form: {
        desiredRole: 'member',
        applyMessage: ''
      }
    }
  },
  computed: {
    visiblePanel() {
      return !!this.projectId && !!this.currentUserId && (this.canManageProject || !this.status.member || this.status.hasPendingInvite || this.status.hasPendingJoinRequest)
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.refreshAll()
      }
    },
    canManageProject() {
      this.refreshAll()
    }
  },
  methods: {
    async refreshAll() {
      if (!this.projectId || !this.currentUserId) return
      const res = await getProjectMemberStatus(this.projectId)
      this.status = res?.data || this.status
      if (this.canManageProject) {
        const auditRes = await listProjectJoinRequests(this.projectId)
        const list = Array.isArray(auditRes?.data) ? auditRes.data : []
        this.pendingRequests = list.filter(item => item.status === 'pending')
      } else {
        this.pendingRequests = []
      }
    },
    async handleAcceptInvite() {
      if (!this.status.pendingInviteCode) return
      this.accepting = true
      try {
        await acceptProjectInvite(this.status.pendingInviteCode, { inviteCode: this.status.pendingInviteCode })
        this.$message.success('已接受项目邀请')
        await this.refreshAll()
        this.$emit('changed')
      } finally {
        this.accepting = false
      }
    },
    async handleSubmit() {
      this.submitting = true
      try {
        await submitProjectJoinRequest(this.projectId, this.form)
        this.$message.success('加入申请已提交')
        await this.refreshAll()
        this.$emit('changed')
      } finally {
        this.submitting = false
      }
    },
    async handleAudit(row, status) {
      const reviewMessage = status === 'approved' ? '审核通过' : '审核拒绝'
      await auditProjectJoinRequest(row.id, { status, reviewMessage })
      this.$message.success(status === 'approved' ? '已通过申请' : '已拒绝申请')
      await this.refreshAll()
      this.$emit('changed')
    }
  }
}
</script>

<style scoped>
.join-request-card {
  margin-bottom: 16px;
}
.join-request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.join-status-tip {
  color: #67c23a;
}
.alert-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.apply-form {
  margin-top: 8px;
}
.audit-section {
  margin-top: 8px;
}
</style>
