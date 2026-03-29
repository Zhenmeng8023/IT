<template>
  <div class="layout-container">
    <el-menu
      :default-active="activeIndex"
      class="el-menu-demo"
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

    <main class="main-content">
      <nuxt />
    </main>

    <el-backtop target=".main-content" :visibility-height="1" />

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
      if (path.startsWith('/project')) return '/project'
      if (path.startsWith('/projectlist')) return '/project'
      if (path.startsWith('/myproject')) return '/project'
      if (path.startsWith('/projectcollection')) return '/project'
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
.el-menu-demo {
  background-color: #1a1a1a !important;
  border-bottom: 1px solid #333 !important;
}

.el-menu-demo .el-menu-item {
  color: #e0e0e0 !important;
  background-color: transparent !important;
  border-bottom: 3px solid transparent !important;
  transition: all 0.3s ease !important;
}

.el-backtop {
  background-color: #333 !important;
  color: #ffffff !important;
  border: 1px solid #555 !important;
  border-radius: 50% !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
  transition: all 0.3s ease !important;
}

.el-backtop:hover {
  background-color: #555 !important;
  border-color: #777 !important;
  transform: scale(1.1) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4) !important;
}

.el-backtop i {
  color: #ffffff !important;
}

.layout-container {
  background-color: #000000;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.main-content {
  background-color: #000000;
  color: #ffffff;
  font-weight: 500 !important;
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}

.el-menu-demo .el-menu-item:hover {
  color: #ffffff !important;
  background-color: #2a2a2a !important;
  border-bottom-color: #555 !important;
}

.el-menu-demo .el-menu-item.is-active {
  color: #ffffff !important;
  background-color: #2a2a2a !important;
  border-bottom-color: #3498db !important;
}

.el-menu--horizontal .el-menu-item:not(.is-disabled):hover {
  border-bottom: 3px solid transparent !important;
}

.el-menu--horizontal {
  border: none !important;
}

::v-deep .el-menu--horizontal > .el-submenu .el-submenu__title {
  color: #e0e0e0 !important;
  border-bottom: 3px solid transparent !important;
  transition: all 0.3s ease !important;
}

::v-deep .el-menu--horizontal > .el-submenu .el-submenu__title:hover {
  color: #ffffff !important;
  background-color: #2a2a2a !important;
  border-bottom-color: #555 !important;
}

::v-deep .el-menu--horizontal > .el-submenu.is-active .el-submenu__title {
  color: #ffffff !important;
  background-color: #2a2a2a !important;
  border-bottom-color: #3498db !important;
}

@media (max-width: 768px) {
  .el-menu-demo {
    flex-direction: column !important;
  }

  .el-menu-demo .el-menu-item {
    width: 100% !important;
    text-align: center !important;
    border-bottom: 1px solid #333 !important;
    border-right: none !important;
  }
}
</style>