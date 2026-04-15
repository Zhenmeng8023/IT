<template>
  <div>
    <el-card shadow="never">
      <div slot="header" class="card-header">
        <span>成员管理</span>
        <div class="toolbar-actions">
          <el-input v-model="memberFilter.keyword" size="small" clearable placeholder="搜索成员名称/用户名" class="toolbar-input"></el-input>

          <el-badge v-if="canManageProject" :value="pendingJoinRequestCount" :hidden="!pendingJoinRequestCount">
            <el-button size="small" icon="el-icon-document" @click="openJoinRequestDialog">加入申请</el-button>
          </el-badge>

          <el-button v-if="canManageProject" type="primary" size="small" icon="el-icon-plus" @click="openInviteDialog">添加成员</el-button>
          <el-button type="warning" size="small" icon="el-icon-switch-button" @click="quitProject">退出项目</el-button>
        </div>
      </div>

      <el-table :data="filteredMembers" border :row-class-name="memberRowClassName">
        <el-table-column prop="name" label="名称" min-width="140"></el-table-column>
        <el-table-column prop="username" label="用户名" min-width="160"></el-table-column>
        <el-table-column prop="role" label="角色" width="180">
          <template slot-scope="scope">
            <div class="member-role-cell">
              <el-tag v-if="isSelfMember(scope.row)" size="mini" type="info">{{ getMemberRoleText(scope.row.role) }} · 我自己</el-tag>
              <el-tag v-else-if="isReadonlyMember(scope.row)" size="mini" :type="scope.row.role === 'owner' ? 'danger' : 'info'">{{ getMemberRoleText(scope.row.role) }}</el-tag>
              <el-select
                v-else-if="canEditMemberRole(scope.row)"
                v-model="scope.row.role"
                size="mini"
                style="width: 130px"
                :loading="roleSavingMemberId === scope.row.memberId"
                :disabled="roleSavingMemberId === scope.row.memberId"
                @change="value => updateMemberRoleInline(scope.row, value)"
              >
                <el-option label="成员" value="member"></el-option>
                <el-option label="管理员" value="admin"></el-option>
                <el-option label="查看者" value="viewer"></el-option>
              </el-select>
              <el-tag v-else size="mini" :type="scope.row.role === 'owner' ? 'danger' : 'info'">{{ getMemberRoleText(scope.row.role) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="joinTime" label="加入时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.joinTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="canRemoveMember(scope.row)" size="mini" type="danger" @click="deleteMember(scope.row)">移除</el-button>
            <span v-else class="member-row-readonly-text">{{ isSelfMember(scope.row) ? '不可操作自己' : '不可操作' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="canManageProject" shadow="never" class="member-extra-card">
      <div slot="header" class="card-header">
        <span>协作申请审核</span>
        <div class="toolbar-actions">
          <el-input v-model="joinRequestFilter.keyword" size="small" clearable placeholder="搜索申请人/留言" class="toolbar-input"></el-input>
          <el-select v-model="joinRequestFilter.status" size="small" clearable class="toolbar-input short-input">
            <el-option label="待处理" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
          <el-button size="small" icon="el-icon-refresh" @click="loadJoinRequests">刷新</el-button>
        </div>
      </div>
      <el-table :data="filteredJoinRequests" border v-loading="joinRequestLoading">
        <el-table-column prop="applicantName" label="申请人" min-width="140" />
        <el-table-column prop="desiredRole" label="申请角色" width="120">
          <template slot-scope="scope">{{ getMemberRoleText(scope.row.desiredRole || 'member') }}</template>
        </el-table-column>
        <el-table-column prop="applyMessage" label="申请说明" min-width="220" />
        <el-table-column prop="status" label="状态" width="110">
          <template slot-scope="scope"><el-tag size="mini" :type="getJoinRequestStatusTag(scope.row.status)">{{ getJoinRequestStatusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <template v-if="scope.row.status === 'pending'">
              <el-button size="mini" type="success" :loading="joinRequestAuditLoadingId === scope.row.id" @click="handleJoinRequestAudit(scope.row, true)">通过</el-button>
              <el-button size="mini" type="danger" plain :loading="joinRequestAuditLoadingId === scope.row.id" @click="handleJoinRequestAudit(scope.row, false)">拒绝</el-button>
            </template>
            <span v-else class="member-row-readonly-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="canManageProject" shadow="never" class="member-extra-card">
      <div slot="header" class="card-header">
        <span>待接受邀请</span>
        <div class="toolbar-actions">
          <el-input v-model="invitationFilter.keyword" size="small" clearable placeholder="搜索被邀请人/留言" class="toolbar-input"></el-input>
          <el-select v-model="invitationFilter.status" size="small" clearable class="toolbar-input short-input">
            <el-option label="待接受" value="pending" />
            <el-option label="已接受" value="accepted" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已撤销" value="cancelled" />
          </el-select>
          <el-button size="small" icon="el-icon-refresh" @click="loadProjectInvitations">刷新</el-button>
        </div>
      </div>
      <el-table :data="filteredProjectInvitations" border v-loading="invitationLoading">
        <el-table-column prop="inviteeName" label="被邀请人" min-width="140" />
        <el-table-column prop="inviteRole" label="邀请角色" width="120">
          <template slot-scope="scope">{{ getMemberRoleText(scope.row.inviteRole || 'member') }}</template>
        </el-table-column>
        <el-table-column prop="inviteMessage" label="邀请留言" min-width="220" />
        <el-table-column prop="status" label="状态" width="110">
          <template slot-scope="scope"><el-tag size="mini" :type="getInvitationStatusTag(scope.row.status)">{{ getInvitationStatusText(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="邀请时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="expiredAt" label="过期时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.expiredAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="scope.row.status === 'pending'" size="mini" type="danger" @click="cancelInvitation(scope.row)">撤销</el-button>
            <span v-else class="member-row-readonly-text">不可操作</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="邀请成员" :visible.sync="inviteDialogVisible" width="520px" @close="resetInviteForm">
      <el-form :model="invitationForm" label-width="90px">
        <el-form-item label="搜索用户">
          <el-select
            v-model="invitationForm.inviteeId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="输入用户显示名或用户名搜索"
            :remote-method="searchInviteUsers"
            :loading="inviteUserSearchLoading"
            style="width: 100%"
          >
            <el-option v-for="user in inviteUserOptions" :key="user.id" :label="buildNewMemberUserLabel(user)" :value="user.id">
              <div class="member-user-option">
                <div class="member-user-option__title">{{ user.nickname || user.username || ('用户' + user.id) }}</div>
                <div class="member-user-option__desc">{{ user.username || '-' }} · ID {{ user.id }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <div v-if="selectedInviteUser" class="selected-user-card">
          <el-avatar :size="34" :src="selectedInviteUser.avatarUrl || ''">{{ (selectedInviteUser.nickname || selectedInviteUser.username || 'U').slice(0, 1) }}</el-avatar>
          <div class="selected-user-card__text">
            <div class="selected-user-card__name">{{ selectedInviteUser.nickname || selectedInviteUser.username || ('用户' + selectedInviteUser.id) }}</div>
            <div class="selected-user-card__meta">{{ selectedInviteUser.username || '-' }} · ID {{ selectedInviteUser.id }}</div>
          </div>
        </div>

        <el-form-item label="角色">
          <el-select v-model="invitationForm.inviteRole" style="width: 100%">
            <el-option label="成员" value="member"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="查看者" value="viewer"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="邀请留言">
          <el-input v-model="invitationForm.inviteMessage" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="可选，告诉对方为什么邀请他加入" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="invitationForm.expireDays" style="width: 100%">
            <el-option label="3 天" :value="3" />
            <el-option label="7 天" :value="7" />
            <el-option label="15 天" :value="15" />
          </el-select>
        </el-form-item>
        <div class="dialog-tip">搜索用户后不再直接拉入项目，而是发送邀请，等待对方在项目列表或我的项目页接受后才正式加入。</div>
      </el-form>
      <span slot="footer">
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendingInvitation" @click="sendProjectInvitation">{{ sendingInvitation ? '发送中...' : '发送邀请' }}</el-button>
      </span>
    </el-dialog>

    <el-dialog title="待审核加入申请" :visible.sync="joinRequestDialogVisible" width="900px">
      <el-table :data="pendingJoinRequests" border v-loading="joinRequestLoading">
        <el-table-column prop="applicantName" label="申请人" min-width="160" />
        <el-table-column prop="desiredRole" label="申请角色" width="120">
          <template slot-scope="scope">{{ getMemberRoleText(scope.row.desiredRole) }}</template>
        </el-table-column>
        <el-table-column prop="applyMessage" label="申请说明" min-width="320" />
        <el-table-column prop="createdAt" label="申请时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="success" @click="auditJoinRequest(scope.row, 'approved')">通过</el-button>
            <el-button size="mini" type="danger" @click="auditJoinRequest(scope.row, 'rejected')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!joinRequestLoading && pendingJoinRequests.length === 0" description="暂无待审核申请" />
    </el-dialog>
  </div>
</template>

<script>
import {
  cancelProjectInvitation,
  createProjectInvitation,
  listProjectInvitations,
  quitProject as apiQuitProject,
  removeProjectMember,
  searchProjectMemberUsers,
  updateProjectMemberRole
} from '@/api/project'
import { auditProjectJoinRequest, listProjectJoinRequests } from '@/api/projectJoinRequest'
import {
  formatTime,
  getInvitationStatusTag,
  getInvitationStatusText,
  getJoinRequestStatusTag,
  getJoinRequestStatusText,
  getMemberRoleText
} from '../services/projectManageShared'

export default {
  name: 'ProjectManageMemberTab',
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    members: {
      type: Array,
      default: () => []
    },
    currentUserId: {
      type: Number,
      default: null
    },
    currentMemberRecord: {
      type: Object,
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    refreshSeed: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      memberFilter: { keyword: '' },
      roleSavingMemberId: null,
      inviteDialogVisible: false,
      invitationForm: { inviteeId: null, inviteRole: 'member', inviteMessage: '', expireDays: 7 },
      inviteUserSearchLoading: false,
      inviteUserOptions: [],
      sendingInvitation: false,
      projectInvitations: [],
      invitationLoading: false,
      invitationFilter: { status: 'pending', keyword: '' },
      joinRequestDialogVisible: false,
      joinRequests: [],
      joinRequestLoading: false,
      joinRequestAuditLoadingId: null,
      joinRequestFilter: { status: 'pending', keyword: '' }
    }
  },
  computed: {
    filteredMembers() {
      return this.members.filter(member => {
        const keyword = (this.memberFilter.keyword || '').trim().toLowerCase()
        if (!keyword) return true
        return [member.name, member.username, member.nickname].filter(Boolean).some(value => value.toLowerCase().includes(keyword))
      })
    },
    filteredProjectInvitations() {
      const keyword = (this.invitationFilter.keyword || '').trim().toLowerCase()
      return (this.projectInvitations || []).filter(item => {
        const statusMatched = !this.invitationFilter.status || item.status === this.invitationFilter.status
        if (!statusMatched) return false
        if (!keyword) return true
        return [item.inviteeName, item.inviteeEmail, item.inviteMessage, item.projectName].filter(Boolean).some(value => String(value).toLowerCase().includes(keyword))
      })
    },
    filteredJoinRequests() {
      const keyword = (this.joinRequestFilter.keyword || '').trim().toLowerCase()
      return (this.joinRequests || []).filter(item => {
        const statusMatched = !this.joinRequestFilter.status || item.status === this.joinRequestFilter.status
        if (!statusMatched) return false
        if (!keyword) return true
        return [item.applicantName, item.applyMessage, item.projectName].filter(Boolean).some(value => String(value).toLowerCase().includes(keyword))
      })
    },
    pendingJoinRequests() {
      return (this.joinRequests || []).filter(item => item.status === 'pending')
    },
    pendingJoinRequestCount() {
      return this.pendingJoinRequests.length
    },
    selectedInviteUser() {
      return this.inviteUserOptions.find(item => Number(item.id) === Number(this.invitationForm.inviteeId)) || null
    }
  },
  watch: {
    projectId: {
      immediate: true,
      handler() {
        this.loadManageData()
      }
    },
    refreshSeed() {
      this.loadManageData()
    },
    canManageProject() {
      this.loadManageData()
    },
    'invitationFilter.status'() {
      if (this.canManageProject) this.loadProjectInvitations()
    },
    'joinRequestFilter.status'() {
      if (this.canManageProject) this.loadJoinRequests()
    }
  },
  methods: {
    formatTime,
    getInvitationStatusTag,
    getInvitationStatusText,
    getJoinRequestStatusTag,
    getJoinRequestStatusText,
    getMemberRoleText,
    async loadManageData() {
      if (!this.projectId || !this.canManageProject) {
        this.projectInvitations = []
        this.joinRequests = []
        return
      }
      await Promise.all([this.loadProjectInvitations(), this.loadJoinRequests()])
    },
    isSelfMember(member) {
      return !!member && this.currentUserId !== null && this.currentUserId !== undefined && Number(member.userId) === Number(this.currentUserId)
    },
    memberRoleWeight(role) {
      return { viewer: 0, member: 1, admin: 2, owner: 3 }[String(role || '').toLowerCase()] ?? -1
    },
    canEditMemberRole(member) {
      if (!this.canManageProject || !member || !member.memberId || this.isSelfMember(member) || member.role === 'owner') return false
      const currentRole = this.currentMemberRecord && this.currentMemberRecord.role ? this.currentMemberRecord.role : 'owner'
      return this.memberRoleWeight(currentRole) > this.memberRoleWeight(member.role)
    },
    canRemoveMember(member) {
      return this.canEditMemberRole(member)
    },
    isReadonlyMember(member) {
      if (!member) return true
      return this.isSelfMember(member) || !this.canEditMemberRole(member)
    },
    memberRowClassName({ row }) {
      if (!row) return ''
      if (this.isSelfMember(row)) return 'member-row-self'
      if (this.isReadonlyMember(row)) return 'member-row-readonly'
      return 'member-row-manageable'
    },
    openJoinRequestDialog() {
      if (!this.canManageProject) return
      this.joinRequestDialogVisible = true
      this.loadJoinRequests()
    },
    openInviteDialog() {
      this.resetInviteForm()
      this.inviteDialogVisible = true
    },
    resetInviteForm() {
      this.invitationForm = { inviteeId: null, inviteRole: 'member', inviteMessage: '', expireDays: 7 }
      this.inviteUserOptions = []
      this.inviteUserSearchLoading = false
    },
    buildNewMemberUserLabel(user) {
      const displayName = user.nickname || user.username || `用户${user.id}`
      return `${displayName}（${user.username || '-'} / ID ${user.id}）`
    },
    async searchInviteUsers(keyword) {
      const value = (keyword || '').trim()
      if (!value) {
        this.inviteUserOptions = []
        return
      }
      this.inviteUserSearchLoading = true
      try {
        const response = await searchProjectMemberUsers(value, 10)
        const memberUserIdSet = new Set(this.members.map(item => Number(item.userId)))
        const pendingInviteeSet = new Set((this.projectInvitations || []).filter(item => item.status === 'pending' && item.inviteeId).map(item => Number(item.inviteeId)))
        this.inviteUserOptions = (response.data || []).filter(user => Number(user.id) !== Number(this.currentUserId) && !memberUserIdSet.has(Number(user.id)) && !pendingInviteeSet.has(Number(user.id)))
      } catch (error) {
        this.inviteUserOptions = []
        this.$message.error(error.response?.data?.message || '搜索用户失败')
      } finally {
        this.inviteUserSearchLoading = false
      }
    },
    async sendProjectInvitation() {
      if (!this.invitationForm.inviteeId) {
        this.$message.warning('请先选择用户')
        return
      }
      this.sendingInvitation = true
      try {
        await createProjectInvitation({
          projectId: Number(this.projectId),
          inviteeId: Number(this.invitationForm.inviteeId),
          inviteRole: this.invitationForm.inviteRole,
          inviteMessage: this.invitationForm.inviteMessage,
          expireDays: this.invitationForm.expireDays
        })
        this.$message.success('邀请已发送，等待对方接受')
        this.inviteDialogVisible = false
        this.resetInviteForm()
        await this.loadProjectInvitations()
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '发送邀请失败')
      } finally {
        this.sendingInvitation = false
      }
    },
    async loadProjectInvitations() {
      if (!this.canManageProject) {
        this.projectInvitations = []
        return
      }
      this.invitationLoading = true
      try {
        const response = await listProjectInvitations(this.projectId, { status: this.invitationFilter.status || undefined })
        this.projectInvitations = response.data || []
      } catch (error) {
        this.projectInvitations = []
      } finally {
        this.invitationLoading = false
      }
    },
    async cancelInvitation(item) {
      try {
        await this.$confirm('确认撤销这条邀请吗？', '提示', { type: 'warning' })
        await cancelProjectInvitation(item.id)
        this.$message.success('邀请已撤销')
        await this.loadProjectInvitations()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '撤销邀请失败')
        }
      }
    },
    async loadJoinRequests() {
      if (!this.canManageProject) {
        this.joinRequests = []
        return
      }
      this.joinRequestLoading = true
      try {
        const response = await listProjectJoinRequests(this.projectId)
        this.joinRequests = response.data || []
      } catch (error) {
        this.joinRequests = []
      } finally {
        this.joinRequestLoading = false
      }
    },
    async handleJoinRequestAudit(item, approved) {
      const actionText = approved ? '通过' : '拒绝'
      try {
        const { value } = await this.$prompt(`可选填写${actionText}备注`, `${actionText}加入申请`, {
          confirmButtonText: actionText,
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: `可选填写${actionText}备注`,
          inputValidator: () => true
        })
        this.joinRequestAuditLoadingId = item.id
        await auditProjectJoinRequest(item.id, { status: approved ? 'approved' : 'rejected', reviewMessage: String(value || '').trim() })
        this.$message.success(`${actionText}成功`)
        await this.loadJoinRequests()
        this.$emit('request-refresh')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error.response?.data?.message || `${actionText}失败`)
        }
      } finally {
        this.joinRequestAuditLoadingId = null
      }
    },
    async updateMemberRoleInline(member, role) {
      if (!member.memberId) {
        this.$message.warning('项目所有者角色不能在这里修改')
        return
      }
      if (this.isSelfMember(member)) {
        this.$message.warning('不能修改自己的项目身份')
        return
      }
      if (!this.canEditMemberRole(member)) {
        this.$message.warning('你只能管理权限低于自己的成员')
        return
      }
      const previousRole = member.role
      member.role = role
      this.roleSavingMemberId = member.memberId
      try {
        await updateProjectMemberRole({ memberId: member.memberId, role })
        this.$message.success('角色更新成功')
        this.$emit('request-refresh')
      } catch (error) {
        member.role = previousRole
        this.$message.error(error.response?.data?.message || '角色更新失败')
      } finally {
        this.roleSavingMemberId = null
      }
    },
    async deleteMember(member) {
      if (!member.memberId) {
        this.$message.warning('项目所有者不可移除')
        return
      }
      if (this.isSelfMember(member)) {
        this.$message.warning('不能在成员管理中移除自己，请使用退出项目')
        return
      }
      if (!this.canRemoveMember(member)) {
        this.$message.warning('你只能管理权限低于自己的成员')
        return
      }
      try {
        await this.$confirm(`确定移除成员 ${member.name} 吗？`, '提示', { type: 'warning' })
        await removeProjectMember(member.memberId)
        this.$message.success('移除成功')
        this.$emit('request-refresh')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '移除失败')
        }
      }
    },
    async quitProject() {
      try {
        await this.$confirm('确定退出项目吗？退出后将无法继续协作该项目。', '提示', { type: 'warning' })
        await apiQuitProject(this.projectId)
        this.$message.success('已退出项目')
        this.$router.push('/projectlist')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '退出项目失败')
        }
      }
    },
    async auditJoinRequest(row, status) {
      try {
        const reviewMessage = status === 'approved' ? '审核通过' : '审核拒绝'
        await auditProjectJoinRequest(row.id, { status, reviewMessage })
        this.$message.success(status === 'approved' ? '已通过申请' : '已拒绝申请')
        await this.loadJoinRequests()
        this.$emit('request-refresh')
      } catch (error) {
        this.$message.error(error.response?.data?.message || '审核加入申请失败')
      }
    }
  }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar-input { width: 220px; }
.short-input { width: 140px; }
.member-extra-card { margin-top: 16px; }
.member-row-readonly .el-table__cell,
.member-row-self .el-table__cell { background: var(--it-surface-muted); }
.member-row-readonly-text { color: #909399; font-size: 12px; }
.member-role-cell { display: flex; align-items: center; min-height: 28px; }
.member-user-option { line-height: 1.4; }
.member-user-option__title { font-size: 14px; color: #303133; }
.member-user-option__desc { font-size: 12px; color: #909399; }
.selected-user-card { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 10px 12px; background: var(--it-surface-muted); border: 1px solid var(--it-border); border-radius: 10px; }
.selected-user-card__text { min-width: 0; }
.selected-user-card__name { font-size: 14px; color: #303133; font-weight: 600; }
.selected-user-card__meta, .dialog-tip { font-size: 12px; color: #909399; }

@media (max-width: 768px) {
  .toolbar-input, .short-input { width: 100%; }
}
</style>
