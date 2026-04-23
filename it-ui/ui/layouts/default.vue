<template>
  <div class="default-layout">
    <FrontNavShell
      :brand-subtitle="brandSubtitle"
      :show-search="false"
      :menu-groups="menuGroups"
      :active-menu="activeMenu"
      :aside-width="asideWidth"
      :menu-collapsed="menuCollapsed"
      :is-compact="isCompact"
      main-class="default-main-content"
      @toggle-sidebar="toggleSidebar"
      @menu-select="handleMenuSelect"
    >
      <template #actions>
        <div class="header-actions">
          <ThemeToggle />
          <template v-if="isLoggedIn">
            <AppUserMenu v-if="showGlobalUserMenu" :size="36" />
          </template>
          <template v-else>
            <div class="guest-actions">
              <el-button size="small" @click="goToLogin">登录</el-button>
              <el-button size="small" type="primary" plain @click="goToRegister">注册</el-button>
            </div>
          </template>
        </div>
      </template>

      <nuxt />
    </FrontNavShell>

    <el-backtop :target="backtopTarget" :visibility-height="260" />

    <client-only>
      <AIAssistant v-if="showAiLayer" />
      <SceneAiDock v-if="showAiLayer" />
    </client-only>
  </div>
</template>

<script>
import FrontNavShell from '@/components/front/FrontNavShell.vue'
import { getFrontNavigationGroups, isFrontProtectedRoute, resolveFrontActiveMenu } from '@/components/front/frontNavigation'
import { getCurrentUser, getToken } from '@/utils/auth'

export default {
  name: 'DefaultLayout',
  components: {
    FrontNavShell
  },
  data() {
    return {
      isSidebarCollapsed: false,
      isCompact: false
    }
  },
  computed: {
    menuGroups() {
      return getFrontNavigationGroups()
    },
    activeMenu() {
      return resolveFrontActiveMenu(this.$route)
    },
    asideWidth() {
      if (this.isCompact) {
        return '100%'
      }
      return this.isSidebarCollapsed ? '84px' : '228px'
    },
    menuCollapsed() {
      return this.isCompact ? false : this.isSidebarCollapsed
    },
    showAiLayer() {
      const hiddenRoutes = ['/login', '/registe', '/noPermission']
      return !hiddenRoutes.includes(this.$route.path)
    },
    isLoggedIn() {
      return !!getToken() || !!getCurrentUser()
    },
    showGlobalUserMenu() {
      return !String(this.$route.path || '').startsWith('/other/')
    },
    backtopTarget() {
      return '.main-content'
    },
    brandSubtitle() {
      const path = String(this.$route.path || '')
      if (path.startsWith('/project')) return '项目空间'
      if (path.startsWith('/blog')) return '博客空间'
      if (path.startsWith('/circle')) return '圈子空间'
      if (path.startsWith('/wallet') || path.startsWith('/vip') || path.startsWith('/orders_purchases') || path.startsWith('/coupons')) return '资产中心'
      if (path.startsWith('/user') || path.startsWith('/collection') || path.startsWith('/history') || path.startsWith('/notifications') || path.startsWith('/notification') || path.startsWith('/personal-knowledge-base')) return '个人中心'
      return '前台导航'
    }
  },
  mounted() {
    this.syncViewport()
    window.addEventListener('resize', this.syncViewport, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewport)
  },
  methods: {
    syncViewport() {
      if (!process.client) {
        return
      }
      this.isCompact = window.innerWidth <= 960
    },
    ensureLogin(actionText = '访问该页面') {
      if (this.isLoggedIn) return true
      this.$confirm(`需要登录后才能${actionText}，是否前往登录页？`, '未登录', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$router.push('/login')
      }).catch(() => {})
      return false
    },
    toggleSidebar() {
      if (this.isCompact) {
        return
      }
      this.isSidebarCollapsed = !this.isSidebarCollapsed
    },
    goToLogin() {
      this.$router.push('/login')
    },
    goToRegister() {
      this.$router.push('/registe')
    },
    handleMenuSelect(index) {
      if (index === '/') {
        if (process.client && window.location.pathname !== '/') {
          window.location.assign('/')
          return
        }
      }
      if (isFrontProtectedRoute(index) && !this.ensureLogin('访问该页面')) {
        return
      }
      if (this.$route.path !== index) {
        this.$router.push(index).catch(() => {})
      }
    }
  }
}
</script>

<style scoped>
.default-layout {
  min-height: 100vh;
}

.header-actions,
.guest-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.default-main-content {
  width: 100%;
  max-width: none;
  min-width: 0;
}

.el-backtop {
  background: var(--it-accent) !important;
  color: #ffffff !important;
  box-shadow: var(--it-shadow) !important;
}

@media (max-width: 768px) {
  .guest-actions {
    display: none;
  }
}
</style>
