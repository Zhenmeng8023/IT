<template>
  <div class="app-user-menu">
    <template v-if="isLoggedIn">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <el-avatar :size="avatarSize" :src="userAvatar"></el-avatar>
          <div v-if="showName" class="user-meta">
            <span class="user-name">{{ displayName }}</span>
            <span class="user-status">{{ statusText }}</span>
          </div>
          <i class="el-icon-arrow-down user-arrow"></i>
        </div>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="profile">个人中心</el-dropdown-item>
          <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </template>

    <div v-else class="guest-actions">
      <el-button plain class="ghost-btn" @click="goToLogin">登录</el-button>
      <el-button type="primary" class="primary-btn" @click="goToRegister">注册</el-button>
    </div>
  </div>
</template>

<script>
import { useUserStore } from '@/store/user'

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

export default {
  name: 'AppUserMenu',
  props: {
    size: {
      type: Number,
      default: 40
    },
    showName: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      syncing: false
    }
  },
  computed: {
    userStore() {
      return useUserStore()
    },
    currentUser() {
      return this.userStore.userInfo || this.userStore.user || null
    },
    isLoggedIn() {
      return !!(this.userStore.isLoggedIn && this.currentUser)
    },
    avatarSize() {
      return this.size
    },
    displayName() {
      return (
        this.currentUser?.nickname ||
        this.currentUser?.username ||
        this.currentUser?.name ||
        '当前用户'
      )
    },
    userAvatar() {
      return this.currentUser?.avatarUrl || this.currentUser?.avatar || DEFAULT_AVATAR
    },
    statusText() {
      if (this.currentUser?.roleName) {
        return this.currentUser.roleName
      }
      return '已登录'
    }
  },
  created() {
    this.bootstrapSession()
  },
  methods: {
    buildRedirectQuery() {
      const fullPath = this.$route?.fullPath || '/'
      if (!fullPath || fullPath === '/login') {
        return {}
      }
      return { redirect: fullPath }
    },
    async bootstrapSession() {
      if (!process.client || this.syncing) {
        return
      }

      this.syncing = true
      try {
        this.userStore.restorePermissions()
        if (this.userStore.userInfo || this.userStore.token) {
          await this.userStore.syncSessionFromServer({
            forceReloadPermissions: !this.userStore.permissions?.length
          })
        }
      } catch (error) {
        this.userStore.clearLocalState()
      } finally {
        this.syncing = false
      }
    },
    goToLogin() {
      this.$router.push({ path: '/login', query: this.buildRedirectQuery() })
    },
    goToRegister() {
      this.$router.push({ path: '/registe', query: this.buildRedirectQuery() })
    },
    async handleCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user')
        return
      }

      if (command !== 'logout') {
        return
      }

      try {
        await this.$confirm('确定要退出当前账号吗？', '退出登录', {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await this.userStore.logout()
        this.$message.success('已安全退出登录')
        this.$router.push({ path: '/login', query: this.buildRedirectQuery() })
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('退出登录失败，请稍后重试')
        }
      }
    }
  }
}
</script>

<style scoped>
.app-user-menu {
  display: inline-flex;
  align-items: center;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px 6px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.user-trigger:hover {
  transform: translateY(-1px);
  border-color: rgba(59, 130, 246, 0.28);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.user-status {
  font-size: 12px;
  color: #64748b;
  line-height: 1.2;
}

.user-arrow {
  color: #94a3b8;
  font-size: 12px;
}

.guest-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.ghost-btn,
.primary-btn {
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 600;
}

.ghost-btn {
  border-color: #cbd5e1;
  color: #334155;
  background: rgba(255, 255, 255, 0.92);
}

.ghost-btn:hover {
  border-color: #93c5fd;
  color: #1d4ed8;
  background: #eff6ff;
}

.primary-btn {
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.18);
}

@media (max-width: 768px) {
  .user-meta,
  .user-arrow {
    display: none;
  }

  .guest-actions {
    gap: 8px;
  }

  .ghost-btn,
  .primary-btn {
    padding: 8px 12px;
  }
}
</style>
