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

        <div class="header-actions" v-if="showGlobalUserMenu">
          <AppUserMenu />
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
      const hiddenPaths = new Set(['/', '/user', '/wallet', '/vip', '/orders_purchases'])
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
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 22%),
    linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 40;
  backdrop-filter: blur(18px);
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(226, 232, 240, 0.85);
}

.header-inner {
  max-width: 1360px;
  margin: 0 auto;
  padding: 12px 18px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
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
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  color: #0f172a;
  font-size: 15px;
}

.brand-copy span {
  color: #64748b;
  font-size: 12px;
}

.app-nav {
  border-bottom: none !important;
  background: transparent !important;
}

.app-nav :deep(.el-menu-item),
.app-nav :deep(.el-submenu__title) {
  height: 46px;
  line-height: 46px;
  margin: 0 4px;
  border-bottom: none !important;
  border-radius: 14px;
  color: #334155 !important;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.app-nav :deep(.el-menu-item:hover),
.app-nav :deep(.el-submenu__title:hover),
.app-nav :deep(.el-menu-item.is-active),
.app-nav :deep(.el-submenu.is-active .el-submenu__title) {
  color: #1d4ed8 !important;
  background: #eff6ff !important;
}

.header-actions {
  display: flex;
  justify-content: flex-end;
}

.main-content {
  width: 100%;
  min-height: calc(100vh - 72px);
  box-sizing: border-box;
}

.el-backtop {
  background: #1d4ed8 !important;
  color: #ffffff !important;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.24) !important;
}

@media (max-width: 1100px) {
  .header-inner {
    grid-template-columns: 1fr;
  }

  .header-actions {
    justify-content: flex-start;
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
</style>
