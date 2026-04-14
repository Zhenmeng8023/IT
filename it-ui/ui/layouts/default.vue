<template>
  <div class="layout-container" :class="{ 'layout--user-center': isUserCenterRoute, 'layout--module-shell': !isUserCenterRoute }">
    <template v-if="isHomeRoute">
      <main class="main-content home-standalone-content">
        <nuxt />
      </main>
    </template>

    <template v-else-if="isUserCenterRoute">
      <header class="app-header user-center-header">
        <div class="header-inner">
          <div class="brand-block" @click="navigateToHome">
            <span class="brand-mark">IT</span>
            <div class="brand-copy">
              <strong>IT Forum</strong>
              <span>内容、圈子与协作平台</span>
            </div>
          </div>

          <el-menu
            :default-active="activeIndex"
            class="app-nav"
            mode="horizontal"
            @select="handleSelect"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/blog">博客</el-menu-item>
            <el-menu-item index="/circle">圈子</el-menu-item>
            <el-menu-item index="/user">个人主页</el-menu-item>
            <el-menu-item index="/wallet">我的钱包</el-menu-item>
            <el-menu-item index="/vip">VIP服务</el-menu-item>

            <el-submenu index="/project">
              <template slot="title">项目相关</template>
              <el-menu-item index="/projectlist">项目列表</el-menu-item>
              <el-menu-item index="/myproject">我的项目</el-menu-item>
              <el-menu-item index="/projectcollection">收藏项目</el-menu-item>
              <el-menu-item v-if="canOpenKnowledgeBase" index="/knowledge-base">项目知识库</el-menu-item>
            </el-submenu>

            <el-submenu v-if="canOpenAiAdmin" index="/ai-admin">
              <template slot="title">AI 管理</template>
              <el-menu-item v-if="canOpenAiModels" index="/ai/models">模型管理</el-menu-item>
              <el-menu-item v-if="canOpenAiPrompts" index="/ai/prompts">提示词模板</el-menu-item>
              <el-menu-item v-if="canOpenAiLogs" index="/ai/logs">AI 日志</el-menu-item>
            </el-submenu>
          </el-menu>

          <div class="header-actions">
            <ThemeToggle />
            <AppUserMenu v-if="showGlobalUserMenu" :size="36" />
          </div>
        </div>
      </header>

      <main class="main-content user-center-content">
        <nuxt />
      </main>
    </template>

    <template v-else>
      <el-header class="header module-header">
        <div class="module-header__inner">
          <div class="module-header__left">
            <button class="collapse-btn" type="button" @click="toggleSidebar">
              <i :class="isSidebarCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
            </button>
            <div class="brand-block" @click="navigateToHome">
              <span class="brand-mark"><i class="el-icon-s-platform"></i></span>
              <div class="brand-copy">
                <strong>IT Forum</strong>
                <span>{{ moduleShellTitle }}</span>
              </div>
            </div>
          </div>

          <div class="module-header__center">
            <div class="module-summary">
              <span class="module-summary__tag">{{ currentModuleTag }}</span>
              <p class="module-summary__copy">{{ currentModuleDescription }}</p>
            </div>
          </div>

          <div class="module-header__right">
            <ThemeToggle />
            <template v-if="isLoggedIn">
              <AppUserMenu :size="36" />
            </template>
            <template v-else>
              <div class="guest-actions">
                <el-button size="small" @click="goToLogin">登录</el-button>
                <el-button size="small" type="primary" plain @click="goToRegister">注册</el-button>
              </div>
            </template>
          </div>
        </div>
      </el-header>

      <el-container class="main-container module-shell-container">
        <el-aside :width="sidebarWidth" class="sidebar module-sidebar" :class="{ 'is-collapsed': isSidebarCollapsed }">
          <el-menu
            :default-active="activeIndex"
            :collapse="isSidebarCollapsed"
            class="module-menu"
            @select="handleSelect"
          >
            <el-menu-item index="/">
              <i class="el-icon-s-home"></i>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/blog">
              <i class="el-icon-document"></i>
              <span>博客</span>
            </el-menu-item>
            <el-menu-item index="/circle">
              <i class="el-icon-chat-dot-round"></i>
              <span>圈子</span>
            </el-menu-item>

            <el-submenu index="module-projects">
              <template slot="title">
                <i class="el-icon-s-management"></i>
                <span>项目协作</span>
              </template>
              <el-menu-item index="/projectlist">项目列表</el-menu-item>
              <el-menu-item index="/myproject">我的项目</el-menu-item>
              <el-menu-item index="/projectcollection">收藏项目</el-menu-item>
              <el-menu-item v-if="canOpenKnowledgeBase" index="/knowledge-base">项目知识库</el-menu-item>
            </el-submenu>

            <el-menu-item index="/wallet">
              <i class="el-icon-wallet"></i>
              <span>我的钱包</span>
            </el-menu-item>
            <el-menu-item index="/vip">
              <i class="el-icon-s-goods"></i>
              <span>VIP 服务</span>
            </el-menu-item>

            <el-submenu v-if="canOpenAiAdmin" index="module-ai">
              <template slot="title">
                <i class="el-icon-s-operation"></i>
                <span>AI 管理</span>
              </template>
              <el-menu-item v-if="canOpenAiModels" index="/ai/models">模型管理</el-menu-item>
              <el-menu-item v-if="canOpenAiPrompts" index="/ai/prompts">提示词模板</el-menu-item>
              <el-menu-item v-if="canOpenAiLogs" index="/ai/logs">AI 日志</el-menu-item>
            </el-submenu>

            <el-menu-item index="/user">
              <i class="el-icon-user"></i>
              <span>个人中心</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="content-area module-content-area">
          <nuxt />
        </el-main>
      </el-container>
    </template>

    <el-backtop v-if="isHomeRoute" :visibility-height="260" />
    <el-backtop v-else :target="backtopTarget" :visibility-height="260" />

    <client-only>
      <AIAssistant v-if="showAiLayer" />
      <SceneAiDock v-if="showAiLayer" />
    </client-only>
  </div>
</template>

<script>
import { getCurrentUser, getToken } from '@/utils/auth'

export default {
  name: 'DefaultLayout',
  data() {
    return {
      activeIndex: '/',
      isSidebarCollapsed: false
    }
  },
  computed: {
    showAiLayer() {
      const hiddenRoutes = ['/login', '/registe', '/noPermission']
      return !hiddenRoutes.includes(this.$route.path)
    },
    isLoggedIn() {
      return !!getToken() || !!getCurrentUser()
    },
    isHomeRoute() {
      const path = this.$route && this.$route.path ? this.$route.path : ''
      return path === '/'
    },
    isUserCenterRoute() {
      const path = this.$route && this.$route.path ? this.$route.path : ''
      return [
        '/user',
        '/peoplehome',
        '/orders_purchases',
        '/collection',
        '/history',
        '/notification',
        '/payment',
        '/coupons'
      ].some(prefix => path.startsWith(prefix)) || path.startsWith('/other')
    },
    sidebarWidth() {
      return this.isSidebarCollapsed ? '72px' : '220px'
    },
    backtopTarget() {
      return this.isUserCenterRoute ? '.user-center-content' : '.content-area'
    },
    canOpenKnowledgeBase() {
      return this.checkPermission('view:knowledge-base')
    },
    canOpenAiModels() {
      return this.checkPermission('view:ai:model-admin')
    },
    canOpenAiPrompts() {
      return this.checkPermission('view:ai:prompt-template')
    },
    canOpenAiLogs() {
      return this.checkPermission('view:ai:log')
    },
    canOpenAiAdmin() {
      return this.canOpenAiModels || this.canOpenAiPrompts || this.canOpenAiLogs
    },
    showGlobalUserMenu() {
      const path = this.$route && this.$route.path ? this.$route.path : ''
      return !path.startsWith('/other/')
    },
    moduleShellTitle() {
      if (this.$route.path.startsWith('/ai/')) return 'AI 管理中心'
      if (this.$route.path.startsWith('/wallet') || this.$route.path.startsWith('/vip')) return '会员与钱包中心'
      if (this.$route.path.startsWith('/collection') || this.$route.path.startsWith('/history') || this.$route.path.startsWith('/notification') || this.$route.path.startsWith('/payment') || this.$route.path.startsWith('/coupons')) return '用户中心'
      if (this.$route.path.startsWith('/blog')) return '博客中心'
      if (this.$route.path.startsWith('/circle')) return '圈子中心'
      return '站点导航'
    },
    currentModuleTag() {
      if (this.$route.path.startsWith('/ai/')) return 'AI 工作台'
      if (this.$route.path.startsWith('/wallet')) return '钱包管理'
      if (this.$route.path.startsWith('/vip')) return '会员权益'
      if (this.$route.path.startsWith('/collection')) return '内容收藏'
      if (this.$route.path.startsWith('/history')) return '浏览记录'
      if (this.$route.path.startsWith('/notification')) return '通知消息'
      if (this.$route.path.startsWith('/payment')) return '支付服务'
      if (this.$route.path.startsWith('/blog')) return '博客社区'
      if (this.$route.path.startsWith('/circle')) return '圈子交流'
      if (this.$route.path.startsWith('/project')) return '项目协作'
      return '站点首页'
    },
    currentModuleDescription() {
      if (this.$route.path.startsWith('/ai/')) return '统一管理模型、提示词和 AI 能力配置。'
      if (this.$route.path.startsWith('/wallet') || this.$route.path.startsWith('/vip')) return '围绕钱包余额、权益订阅与服务状态提供清晰的一体化入口。'
      if (this.$route.path.startsWith('/collection') || this.$route.path.startsWith('/history') || this.$route.path.startsWith('/notification') || this.$route.path.startsWith('/payment') || this.$route.path.startsWith('/coupons')) return '保持与个人中心一致的阅读、记录和服务体验。'
      if (this.$route.path.startsWith('/blog')) return '以博客阅读、检索和创作为核心的统一模块头部。'
      if (this.$route.path.startsWith('/circle')) return '围绕圈子浏览、发帖与加入流程的统一侧栏布局。'
      return '用项目中心的布局骨架，统一首页与高频模块的使用体验。'
    }
  },
  watch: {
    '$route.path': {
      immediate: true,
      handler(val) {
        this.activeIndex = this.resolveActiveIndex(val)
      }
    }
  },
  methods: {
    checkPermission(permissionCode) {
      if (typeof this.$hasPermission === 'function') {
        return this.$hasPermission(permissionCode)
      }
      return true
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
    resolveActiveIndex(path) {
      if (!path) return '/'
      if (path.startsWith('/blog')) return '/blog'
      if (path.startsWith('/circle')) return '/circle'
      if (path.startsWith('/user') || path.startsWith('/peoplehome') || path.startsWith('/orders_purchases') || path.startsWith('/other')) return '/user'
      if (path.startsWith('/wallet')) return '/wallet'
      if (path.startsWith('/vip')) return '/vip'
      if (path.startsWith('/collection') || path.startsWith('/history') || path.startsWith('/notification') || path.startsWith('/payment') || path.startsWith('/coupons')) return '/user'
      if (path.startsWith('/knowledge-base')) return '/knowledge-base'
      if (path.startsWith('/projectcollection')) return '/projectcollection'
      if (path.startsWith('/projectlist')) return '/projectlist'
      if (path.startsWith('/project') || path.startsWith('/myproject')) return '/myproject'
      if (path.startsWith('/ai/models')) return '/ai/models'
      if (path.startsWith('/ai/prompts')) return '/ai/prompts'
      if (path.startsWith('/ai/logs')) return '/ai/logs'
      return '/'
    },
    toggleSidebar() {
      this.isSidebarCollapsed = !this.isSidebarCollapsed
    },
    navigateToHome() {
      if (process.client && window.location.pathname !== '/') {
        window.location.assign('/')
        return
      }
      if (this.$route.path !== '/') {
        this.$router.push('/').catch(() => {})
      }
    },
    goToLogin() {
      this.$router.push('/login')
    },
    goToRegister() {
      this.$router.push('/registe')
    },
    handleSelect(index) {
      if (index === '/') {
        this.navigateToHome()
        return
      }
      const protectedRoutes = new Set([
        '/myproject',
        '/projectcollection',
        '/wallet',
        '/vip',
        '/user'
      ])
      if (index === '/knowledge-base' && !this.canOpenKnowledgeBase) {
        this.$message.warning('当前账号暂无知识库访问权限')
        return
      }
      if (index === '/ai/models' && !this.canOpenAiModels) {
        this.$message.warning('当前账号暂无模型管理权限')
        return
      }
      if (index === '/ai/prompts' && !this.canOpenAiPrompts) {
        this.$message.warning('当前账号暂无提示词模板权限')
        return
      }
      if (index === '/ai/logs' && !this.canOpenAiLogs) {
        this.$message.warning('当前账号暂无 AI 日志权限')
        return
      }
      if (protectedRoutes.has(index) && !this.ensureLogin('访问该页面')) {
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
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--it-page-bg);
  color: var(--it-text);
}

.main-container {
  flex: 1;
  overflow: hidden;
}

.main-content,
.content-area {
  width: 100%;
  min-height: calc(100vh - var(--it-header-height));
  box-sizing: border-box;
}

.content-area {
  padding: 28px 24px 36px;
  overflow-y: auto;
  background: transparent;
}

.home-standalone-content {
  min-height: 100vh;
  background: transparent;
  overflow: visible;
}

.module-sidebar {
  transition: width .24s ease;
}

.module-sidebar.is-collapsed :deep(.el-menu--collapse) {
  width: 72px;
}

.user-center-content {
  overflow-y: auto;
}

.brand-block {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  min-width: 0;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--it-primary-gradient);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  box-shadow: var(--it-shadow);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  color: var(--it-text);
  font-size: 15px;
}

.brand-copy span {
  color: var(--it-text-muted);
  font-size: 12px;
}

.header-actions,
.module-header__right,
.guest-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 40;
  backdrop-filter: blur(18px);
  background: var(--it-header-bg);
  border-bottom: 1px solid var(--it-border);
}

.header-inner {
  max-width: var(--it-shell-max);
  margin: 0 auto;
  padding: 10px var(--it-shell-padding-x);
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.app-nav {
  min-width: 0;
  width: 100%;
  display: flex !important;
  flex-wrap: nowrap !important;
  align-items: center;
  justify-content: flex-start;
  border-bottom: none !important;
  background: transparent !important;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  scrollbar-width: none;
  padding: 0 6px;
}

.app-nav::-webkit-scrollbar {
  display: none;
}

.app-nav :deep(.el-menu-item),
.app-nav :deep(.el-submenu),
.app-nav :deep(.el-submenu__title) {
  float: none !important;
  flex: 0 0 auto;
}

.app-nav :deep(.el-menu-item),
.app-nav :deep(.el-submenu__title) {
  height: 40px;
  line-height: 40px;
  margin: 0 2px;
  padding: 0 10px;
  border-bottom: none !important;
  border-radius: 12px;
  color: var(--it-text-muted) !important;
  font-size: 13px;
  font-weight: 600;
}

.app-nav :deep(.el-menu-item:hover),
.app-nav :deep(.el-submenu__title:hover),
.app-nav :deep(.el-menu-item.is-active),
.app-nav :deep(.el-submenu.is-active .el-submenu__title) {
  color: var(--it-accent) !important;
  background: var(--it-accent-soft) !important;
}

.module-header {
  position: sticky;
  top: 0;
  background: var(--it-header-bg);
  box-shadow: var(--it-shadow);
  border-bottom: 1px solid var(--it-border);
  height: var(--it-header-height);
  display: flex;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(18px);
}

.module-header__inner {
  width: 100%;
  max-width: var(--it-shell-max);
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 520px) auto;
  align-items: center;
  gap: 16px;
  padding: 0 var(--it-shell-padding-x);
}

.module-header__left,
.module-header__center {
  min-width: 0;
}

.module-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--it-shadow-soft);
  transition: all .2s ease;
}

.collapse-btn:hover {
  color: var(--it-accent);
  background: var(--it-accent-soft);
}

.module-summary {
  min-height: 42px;
  border-radius: 16px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.04);
}

.module-summary__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 86px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: var(--it-accent);
  background: var(--it-accent-soft);
}

.module-summary__copy {
  margin: 0;
  color: var(--it-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.module-sidebar {
  background: var(--it-sidebar-bg);
  border-right: 1px solid var(--it-border);
  overflow-y: auto;
}

.module-menu {
  border: none;
  height: 100%;
  background: transparent !important;
}

.module-menu :deep(.el-menu-item),
.module-menu :deep(.el-submenu__title) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--it-radius-control);
  transition: all 0.25s ease;
  color: var(--it-text-muted) !important;
  background: transparent !important;
}

.module-menu :deep(.el-menu-item:hover),
.module-menu :deep(.el-submenu__title:hover) {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.module-menu :deep(.el-menu-item.is-active) {
  background: var(--it-primary-gradient) !important;
  color: #fff !important;
  box-shadow: var(--it-shadow);
}

.module-menu :deep(.el-submenu.is-opened > .el-submenu__title) {
  color: var(--it-text) !important;
}

.module-menu :deep(.el-menu) {
  background: transparent !important;
}

.el-backtop {
  background: var(--it-accent) !important;
  color: #ffffff !important;
  box-shadow: var(--it-shadow) !important;
}

@media (max-width: 1180px) {
  .module-header__inner,
  .header-inner {
    grid-template-columns: auto 1fr auto;
  }

  .brand-copy span,
  .module-summary__copy {
    display: none;
  }
}

@media (max-width: 900px) {
  .module-header__inner,
  .header-inner {
    grid-template-columns: 1fr auto;
  }

  .module-header__center,
  .app-nav {
    grid-column: 1 / -1;
    width: 100%;
  }

  .module-summary {
    margin-top: 8px;
  }

  .module-sidebar {
    width: 180px !important;
  }
}

@media (max-width: 768px) {
  .content-area {
    padding: 16px;
  }

  .module-header__inner,
  .header-inner {
    padding: 12px 16px;
    gap: 12px;
  }

  .guest-actions {
    display: none;
  }

  .brand-copy span {
    display: none;
  }
}
</style>
