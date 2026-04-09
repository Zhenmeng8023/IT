<template>
  <div v-if="hasInvitations" class="project-invitation-sidebar" :class="{ collapsed }">
    <div class="sidebar-header" @click="collapsed = !collapsed">
      <div class="sidebar-header__title">项目邀请</div>
      <el-badge :value="pendingCount" class="invite-badge" />
    </div>
    <div v-show="!collapsed" class="sidebar-body" v-loading="loading">
      <div v-for="item in invitations" :key="item.id" class="invite-card">
        <div class="invite-card__title">{{ item.projectName || '未命名项目' }}</div>
        <div class="invite-card__summary">{{ item.projectSummary || item.projectDescription || '暂无项目摘要' }}</div>
        <div class="invite-card__meta">
          <span>{{ item.projectCategory || '-' }}</span>
          <span>{{ item.projectVisibility || '-' }}</span>
        </div>
        <div class="invite-card__meta">邀请人：{{ item.inviterName || '-' }}</div>
        <div class="invite-card__meta">邀请角色：{{ getRoleText(item.inviteRole) }}</div>
        <div v-if="item.inviteMessage" class="invite-card__message">留言：{{ item.inviteMessage }}</div>
        <div class="invite-card__time">邀请时间：{{ formatTime(item.createdAt) }}</div>
        <div class="invite-card__actions">
          <el-button size="mini" type="success" :loading="actingId === item.id" @click="accept(item)">接受</el-button>
          <el-button size="mini" type="danger" plain :loading="actingId === item.id" @click="reject(item)">拒绝</el-button>
          <el-button size="mini" @click="goProject(item)">查看项目</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  listMyPendingProjectInvitations,
  acceptProjectInvitation,
  rejectProjectInvitation
} from '@/api/project'

export default {
  name: 'ProjectInvitationSidebarNotice',
  data() {
    return {
      loading: false,
      actingId: null,
      collapsed: false,
      invitations: []
    }
  },
  computed: {
    hasInvitations() {
      return Array.isArray(this.invitations) && this.invitations.length > 0
    },
    pendingCount() {
      return this.invitations.length
    }
  },
  watch: {
    '$route.fullPath': {
      immediate: true,
      handler() {
        this.loadInvitations()
      }
    }
  },
  methods: {
    async loadInvitations() {
      this.loading = true
      try {
        const res = await listMyPendingProjectInvitations()
        this.invitations = Array.isArray(res?.data) ? res.data : []
      } catch (error) {
        this.invitations = []
      } finally {
        this.loading = false
      }
    },
    async accept(item) {
      this.actingId = item.id
      try {
        await acceptProjectInvitation(item.id)
        this.$message.success('已接受邀请')
        await this.loadInvitations()
        this.$emit('accepted', item)
      } catch (error) {
        this.$message.error(error.response?.data?.message || '接受邀请失败')
      } finally {
        this.actingId = null
      }
    },
    async reject(item) {
      this.actingId = item.id
      try {
        await rejectProjectInvitation(item.id)
        this.$message.success('已拒绝邀请')
        await this.loadInvitations()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '拒绝邀请失败')
      } finally {
        this.actingId = null
      }
    },
    goProject(item) {
      this.$router.push(`/projectdetail?projectId=${item.projectId}`)
    },
    getRoleText(role) {
      return { owner: '所有者', admin: '管理员', member: '成员', viewer: '查看者' }[role] || role || '-'
    },
    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    }
  }
}
</script>

<style scoped>
.project-invitation-sidebar {
  position: fixed;
  right: 16px;
  bottom: 16px;
  width: 360px;
  max-height: calc(100vh - 120px);
  z-index: 2000;
  border-radius: 18px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.16);
}
.project-invitation-sidebar.collapsed {
  width: 180px;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  cursor: pointer;
}
.sidebar-header__title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}
.sidebar-body {
  padding: 12px;
  overflow: auto;
  max-height: calc(100vh - 180px);
}
.invite-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}
.invite-card + .invite-card {
  margin-top: 12px;
}
.invite-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}
.invite-card__summary {
  margin-top: 8px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}
.invite-card__meta,
.invite-card__message,
.invite-card__time {
  margin-top: 8px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.6;
}
.invite-card__meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.invite-card__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}
@media (max-width: 768px) {
  .project-invitation-sidebar {
    left: 12px;
    right: 12px;
    width: auto;
  }
}
</style>
