<template>
  <div class="app-user-menu">
    <template v-if="isLoggedIn">
      <NotificationBell class="menu-notification" />
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <div class="user-avatar" :style="avatarFrameStyle">
            <img :src="userAvatar" :style="avatarImageStyle" alt="用户头像">
          </div>
          <div v-if="showName" class="user-meta">
            <span class="user-name">{{ displayName }}</span>
            <span class="user-status">{{ statusText }}</span>
          </div>
          <i class="el-icon-arrow-down user-arrow"></i>
        </div>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="profile">个人中心</el-dropdown-item>
          <el-dropdown-item command="collection">内容收藏</el-dropdown-item>
          <el-dropdown-item command="history">浏览历史</el-dropdown-item>
          <el-dropdown-item command="notifications">通知中心</el-dropdown-item>
          <el-dropdown-item command="wallet" divided>钱包</el-dropdown-item>
          <el-dropdown-item command="vip">VIP</el-dropdown-item>
          <el-dropdown-item command="orders">订单购买</el-dropdown-item>
          <el-dropdown-item command="coupons">优惠券</el-dropdown-item>
          <el-dropdown-item command="payment">支付</el-dropdown-item>
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
import NotificationBell from '@/components/NotificationBell.vue'
import { useUserStore } from '@/store/user'

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

export default {
  name: 'AppUserMenu',
  components: {
    NotificationBell
  },
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
    avatarPositionStyle() {
      const position = this.parseAvatarPosition(this.userAvatar)
      return {
        '--avatar-position': `${position.x}% ${position.y}%`
      }
    },
    avatarFrameStyle() {
      return {
        width: `${this.avatarSize}px`,
        height: `${this.avatarSize}px`,
        '--avatar-position': this.avatarPositionStyle['--avatar-position']
      }
    },
    avatarImageStyle() {
      const position = this.parseAvatarPosition(this.userAvatar)
      return {
        objectFit: 'cover',
        objectPosition: `${position.x}% ${position.y}%`
      }
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
    parseAvatarPosition(url) {
      const hash = String(url || '').split('#')[1] || ''
      const match = hash.match(/avatar-position=(\d+(?:\.\d+)?),(\d+(?:\.\d+)?)/)
      if (!match) {
        return { x: 50, y: 50 }
      }
      return {
        x: this.clampAvatarPosition(match[1]),
        y: this.clampAvatarPosition(match[2])
      }
    },
    clampAvatarPosition(value) {
      const numberValue = Number(value)
      if (!Number.isFinite(numberValue)) return 50
      return Math.max(0, Math.min(100, Math.round(numberValue)))
    },
    async handleCommand(command) {
      const commandRoutes = {
        profile: '/user',
        collection: '/collection',
        history: '/history',
        notifications: '/notifications',
        wallet: '/wallet',
        vip: '/vip',
        orders: '/orders_purchases',
        coupons: '/coupons',
        payment: '/payment'
      }

      if (commandRoutes[command]) {
        this.$router.push(commandRoutes[command])
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
        this.$router.replace('/login')
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
  gap: 8px;
  min-width: 0;
}

.menu-notification {
  flex: 0 0 auto;
}

.user-avatar {
  flex: 0 0 auto;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
}

.user-avatar img {
  display: block;
  width: 100%;
  height: 100%;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  min-width: 0;
  max-width: 154px;
  padding: 4px 10px 4px 5px;
  border-radius: 13px;
  background: var(--it-surface-solid);
  border: 1px solid var(--it-border);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.user-trigger:hover {
  transform: translateY(-1px);
  border-color: var(--it-border-strong);
  box-shadow: var(--it-shadow);
}

.user-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1px;
  min-width: 0;
  max-width: 78px;
  flex: 1 1 auto;
}

.user-name {
  font-size: 12px;
  font-weight: 700;
  color: var(--it-text);
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-status {
  font-size: 10px;
  color: var(--it-text-muted);
  line-height: 1.2;
  white-space: nowrap;
}

.user-arrow {
  color: var(--it-text-subtle);
  font-size: 12px;
  flex: 0 0 auto;
}

.guest-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.ghost-btn,
.primary-btn {
  border-radius: var(--it-radius-control);
  padding: 9px 14px;
  font-weight: 600;
}

.ghost-btn {
  border-color: var(--it-border);
  color: var(--it-text-muted);
  background: var(--it-surface-solid);
}

.ghost-btn:hover {
  border-color: var(--it-border-strong);
  color: var(--it-accent);
  background: var(--it-accent-soft);
}

.primary-btn {
  background: var(--it-primary-gradient);
  border-color: transparent;
  box-shadow: var(--it-shadow);
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
