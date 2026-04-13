<template>
  <div class="layout-container">
    <header class="app-header">
      <div class="header-inner">
        <div class="brand-block" @click="$router.push('/')">
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
          <AppUserMenu v-if="showGlobalUserMenu" />
        </div>
      </div>
    </header>

    <main class="main-content">
      <nuxt />
    </main>

    <el-backtop target=".main-content" :visibility-height="260" />

    <client-only>
      <AIAssistant v-if="showAiLayer" />
      <SceneAiDock v-if="showAiLayer" />
    </client-only>
  </div>
</template>

<script>
export default {
  name: 'DefaultLayout',
  data() {
    return {
      activeIndex: '/'
    }
  },
  computed: {
    showAiLayer() {
      const hiddenRoutes = ['/login', '/registe', '/noPermission']
      return !hiddenRoutes.includes(this.$route.path)
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
      const hiddenPaths = new Set(['/'])
      if (hiddenPaths.has(path)) {
        return false
      }
      return !path.startsWith('/other/')
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
    resolveActiveIndex(path) {
      if (!path) return '/'
      if (path.startsWith('/blog')) return '/blog'
      if (path.startsWith('/circle')) return '/circle'
      if (path.startsWith('/user')) return '/user'
      if (path.startsWith('/wallet')) return '/wallet'
      if (path.startsWith('/vip')) return '/vip'
      if (
        path.startsWith('/project') ||
        path.startsWith('/projectlist') ||
        path.startsWith('/myproject') ||
        path.startsWith('/projectcollection')
      ) {
        return '/project'
      }
      if (path.startsWith('/knowledge-base')) return '/knowledge-base'
      if (path.startsWith('/ai/models')) return '/ai/models'
      if (path.startsWith('/ai/prompts')) return '/ai/prompts'
      if (path.startsWith('/ai/logs')) return '/ai/logs'
      return '/'
    },
    handleSelect(index) {
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
      if (this.$route.path !== index) {
        this.$router.push(index)
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

.app-header {
  position: sticky;
  top: 0;
  z-index: 40;
  backdrop-filter: blur(18px);
  background: var(--it-header-bg);
  border-bottom: 1px solid var(--it-border);
}

.header-inner {
  max-width: 1360px;
  margin: 0 auto;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-block {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: var(--it-radius-control);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--it-primary-gradient);
  color: #fff;
  font-weight: 800;
  letter-spacing: 0;
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

.app-nav {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: none !important;
  background: transparent !important;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  scrollbar-width: none;
}

.app-nav::-webkit-scrollbar {
  display: none;
}

.app-nav :deep(.el-menu-item),
.app-nav :deep(.el-submenu__title) {
  height: 42px;
  line-height: 42px;
  margin: 0 2px;
  padding: 0 12px;
  border-bottom: none !important;
  border-radius: 12px;
  color: var(--it-text-muted) !important;
  font-size: 13px;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.app-nav :deep(.el-menu-item:hover),
.app-nav :deep(.el-submenu__title:hover),
.app-nav :deep(.el-menu-item.is-active),
.app-nav :deep(.el-submenu.is-active .el-submenu__title) {
  color: var(--it-accent) !important;
  background: var(--it-accent-soft) !important;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  flex: 0 0 auto;
}

.main-content {
  width: 100%;
  min-height: calc(100vh - 72px);
  box-sizing: border-box;
}

.el-backtop {
  background: var(--it-accent) !important;
  color: #ffffff !important;
  box-shadow: var(--it-shadow) !important;
}

@media (max-width: 1100px) {
  .header-inner {
    flex-wrap: wrap;
  }

  .app-nav {
    order: 3;
    width: 100%;
    justify-content: flex-start;
  }

  .header-actions {
    margin-left: auto;
  }
}

@media (max-width: 768px) {
  .header-inner {
    padding: 12px;
    gap: 12px;
  }

  .brand-copy span {
    display: none;
  }

  .app-nav {
    overflow-x: auto;
  }
}

@media (max-width: 1180px) {
  .brand-copy span {
    display: none;
  }

  .app-nav :deep(.el-menu-item),
  .app-nav :deep(.el-submenu__title) {
    padding: 0 10px;
    font-size: 12px;
  }
}

</style>
