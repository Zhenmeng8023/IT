
<template>
  <el-dialog title="邀请成员" :visible.sync="dialogVisible" width="720px" @open="handleOpen">
    <div class="invite-top-bar">
      <el-select
        v-model="form.inviteeId"
        filterable
        remote
        clearable
        reserve-keyword
        placeholder="搜索用户昵称或账号"
        :remote-method="searchUsers"
        :loading="userLoading"
        class="invite-user-select"
      >
        <el-option
          v-for="item in userOptions"
          :key="item.id"
          :label="item.nickname || item.username || ('用户' + item.id)"
          :value="item.id"
        />
      </el-select>
      <el-select v-model="form.inviteRole" placeholder="邀请角色" class="invite-role-select">
        <el-option label="管理员" value="admin" />
        <el-option label="成员" value="member" />
        <el-option label="查看者" value="viewer" />
      </el-select>
      <el-input-number v-model="expireDays" :min="1" :max="30" label="有效天数" />
    </div>

    <el-input
      v-model="form.inviteMessage"
      type="textarea"
      :rows="3"
      maxlength="500"
      show-word-limit
      placeholder="可选：填写邀请说明"
    />

    <div class="invite-action-row">
      <el-button type="primary" :loading="saving" @click="handleCreateInvite">生成邀请</el-button>
      <span v-if="latestInvite && latestInvite.inviteCode" class="invite-code-text">邀请码：{{ latestInvite.inviteCode }}</span>
      <el-button v-if="latestInvite && latestInvite.inviteCode" size="mini" @click="copyInviteCode(latestInvite.inviteCode)">复制邀请码</el-button>
    </div>

    <el-divider>历史邀请</el-divider>

    <el-table :data="inviteList" border size="mini" max-height="320">
      <el-table-column prop="inviteeName" label="被邀请人" min-width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.inviteeName || scope.row.inviteeEmail || '公开邀请' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="inviteRole" label="角色" width="100" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="inviteCode" label="邀请码" min-width="180" />
      <el-table-column prop="expiredAt" label="过期时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" @click="copyInviteCode(scope.row.inviteCode)">复制</el-button>
          <el-button
            v-if="scope.row.status === 'pending'"
            size="mini"
            type="danger"
            @click="handleCancel(scope.row)"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script>
import { searchProjectMemberUsers } from '@/api/project'
import { createProjectInvite, listProjectInvites, cancelProjectInvite } from '@/api/projectInvite'

export default {
  name: 'ProjectInviteDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    projectId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      dialogVisible: false,
      saving: false,
      userLoading: false,
      userOptions: [],
      inviteList: [],
      latestInvite: null,
      expireDays: 7,
      form: {
        inviteeId: null,
        inviteRole: 'member',
        inviteMessage: ''
      }
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.dialogVisible = val
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val)
    }
  },
  methods: {
    handleOpen() {
      this.loadInviteList()
    },
    async searchUsers(keyword) {
      this.userLoading = true
      try {
        const res = await searchProjectMemberUsers(keyword || '', 10)
        const list = Array.isArray(res?.data) ? res.data : []
        this.userOptions = list
      } finally {
        this.userLoading = false
      }
    },
    buildExpiredAt() {
      const target = new Date()
      target.setDate(target.getDate() + Number(this.expireDays || 7))
      const pad = value => String(value).padStart(2, '0')
      return `${target.getFullYear()}-${pad(target.getMonth() + 1)}-${pad(target.getDate())} ${pad(target.getHours())}:${pad(target.getMinutes())}:${pad(target.getSeconds())}`
    },
    async handleCreateInvite() {
      if (!this.projectId) return
      if (!this.form.inviteeId) {
        this.$message.warning('请先选择被邀请人')
        return
      }
      this.saving = true
      try {
        const res = await createProjectInvite(this.projectId, {
          inviteeId: this.form.inviteeId,
          inviteRole: this.form.inviteRole,
          inviteMessage: this.form.inviteMessage,
          expiredAt: this.buildExpiredAt()
        })
        this.latestInvite = res?.data || null
        this.form.inviteeId = null
        this.form.inviteMessage = ''
        await this.loadInviteList()
        this.$emit('changed')
        this.$message.success('邀请已生成')
      } finally {
        this.saving = false
      }
    },
    async loadInviteList() {
      if (!this.projectId) return
      const res = await listProjectInvites(this.projectId)
      this.inviteList = Array.isArray(res?.data) ? res.data : []
    },
    copyInviteCode(code) {
      if (!code) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code)
      }
      this.$message.success('邀请码已复制')
    },
    async handleCancel(row) {
      await cancelProjectInvite(row.id)
      this.$message.success('邀请已取消')
      await this.loadInviteList()
      this.$emit('changed')
    }
  }
}
</script>

<style scoped>
.invite-top-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  align-items: center;
}
.invite-user-select {
  flex: 1;
}
.invite-role-select {
  width: 140px;
}
.invite-action-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
}
.invite-code-text {
  color: #606266;
}
</style>
