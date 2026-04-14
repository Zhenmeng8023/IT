<template>
  <div class="project-layout">
    <el-header class="header">
      <div class="header-content">
        <div class="header-left">
          <button class="collapse-btn" type="button" @click="toggleSidebar">
            <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>
          <div class="brand-block" @click="$router.push('/')">
            <span class="brand-mark"><i class="el-icon-s-promotion"></i></span>
            <div class="brand-copy">
              <strong>IT Forum</strong>
              <span>项目中心</span>
            </div>
          </div>
        </div>

        <div class="header-center">
          <div class="search-container">
            <el-select
              v-model="searchType"
              size="small"
              class="search-type-select"
              @change="handleSearchTypeChange"
            >
              <el-option label="关键词" value="keyword"></el-option>
              <el-option label="技术栈" value="tech"></el-option>
              <el-option label="作者" value="author"></el-option>
            </el-select>

            <el-input
              v-model="searchKeyword"
              placeholder="搜索项目..."
              size="small"
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button
                  icon="el-icon-search"
                  @click="handleSearch"
                  :loading="searching"
                ></el-button>
              </template>
            </el-input>
          </div>
        </div>

        <div class="header-right">
          <ThemeToggle />
          <el-button
            type="primary"
            size="small"
            icon="el-icon-plus"
            @click="goToCreateProject"
            class="publish-btn"
          >
            {{ isLoggedIn ? '发布项目' : '登录后发布' }}
          </el-button>

          <template v-if="isLoggedIn">
            <AppUserMenu :size="36" />
          </template>
          <template v-else>
            <div class="guest-actions">
              <el-button size="small" @click="goToLogin">登录</el-button>
              <el-button size="small" plain @click="goToRegister">注册</el-button>
            </div>
          </template>
        </div>
      </div>
    </el-header>

    <el-container class="main-container">
      <el-aside :width="asideWidth" class="sidebar" :class="{ 'is-collapsed': isCollapse }">
        <el-menu
          :default-active="$route.path"
          :collapse="isCollapse"
          class="project-menu"
          @select="handleMenuSelect"
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
            <i class="el-icon-s-comment"></i>
            <span>圈子</span>
          </el-menu-item>

          <el-submenu index="projects">
            <template #title>
              <i class="el-icon-s-management"></i>
              <span>项目</span>
            </template>
            <el-menu-item index="/projectlist">
              <i class="el-icon-document"></i>
              <span>项目列表</span>
            </el-menu-item>
            <el-menu-item v-if="isLoggedIn" index="/myproject">
              <i class="el-icon-folder-opened"></i>
              <span>我的项目</span>
            </el-menu-item>
            <el-menu-item v-if="isLoggedIn" index="/projectcollection">
              <i class="el-icon-star-on"></i>
              <span>收藏项目</span>
            </el-menu-item>
          </el-submenu>

          <el-menu-item v-if="isLoggedIn" index="/wallet">
            <i class="el-icon-wallet"></i>
            <span>我的钱包</span>
          </el-menu-item>
          <el-menu-item v-if="isLoggedIn" index="/vip">
            <i class="el-icon-crown"></i>
            <span>VIP服务</span>
          </el-menu-item>
          <el-menu-item v-if="isLoggedIn" index="/user">
            <i class="el-icon-user"></i>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="content-area">
        <nuxt />
      </el-main>
    </el-container>

    <client-only>
      <AIAssistant />
      <SceneAiDock scene="project-detail" />
    </client-only>
  </div>
</template>

<script>
import { getCurrentUser, getToken } from '@/utils/auth'

function readStoredToken() {
  return getToken() || ''
}

function readCurrentUser() {
  return getCurrentUser()
}

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

export default {
  name: 'ProjectLayout',
  data() {
    return {
      searchType: 'keyword',
      searchKeyword: '',
      searching: false,
      userAvatar: DEFAULT_AVATAR,
      username: '游客',
      isCollapse: false
    }
  },
  computed: {
    isLoggedIn() {
      return !!readStoredToken() || !!readCurrentUser()
    },
    asideWidth() {
      return this.isCollapse ? '72px' : '220px'
    }
  },
  watch: {
    '$route.fullPath'() {
      this.syncUserState()
    }
  },
  mounted() {
    this.syncUserState()
    if (process.client) {
      window.addEventListener('storage', this.syncUserState)
    }
  },
  beforeDestroy() {
    if (process.client) {
      window.removeEventListener('storage', this.syncUserState)
    }
  },
  methods: {
    toggleSidebar() {
      this.isCollapse = !this.isCollapse
    },
    syncUserState() {
      const user = readCurrentUser()
      if (user) {
        this.username = user.nickname || user.username || user.name || '用户'
        this.userAvatar = user.avatar || user.avatarUrl || DEFAULT_AVATAR
        return
      }
      this.username = '游客'
      this.userAvatar = DEFAULT_AVATAR
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
    handleSearchTypeChange() {
      this.searchKeyword = ''
    },
    async handleSearch() {
      const keyword = this.searchKeyword.trim()
      if (!keyword) {
        this.$message.warning('请输入搜索关键词')
        return
      }
      this.searching = true
      try {
        const query = {}
        if (this.searchType === 'tech') query.tech = keyword
        else if (this.searchType === 'author') query.author = keyword
        else query.keyword = keyword

        await this.$router.push({ path: '/projectlist', query })
        this.searchKeyword = ''
      } catch (error) {
        console.error('搜索失败:', error)
        this.$message.error('搜索失败，请稍后重试')
      } finally {
        this.searching = false
      }
    },
    goToCreateProject() {
      if (!this.ensureLogin('发布项目')) return
      this.$router.push('/myproject?create=1')
    },
    goToUserProfile() {
      if (!this.ensureLogin('访问个人中心')) return
      this.$router.push('/user')
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
        }
        return
      }
      const protectedRoutes = new Set(['/myproject', '/projectcollection', '/wallet', '/vip', '/user'])
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
/* ========== 布局容器 ========== */
.project-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--it-page-bg);
  color: var(--it-text);
}

/* ========== 头部样式 ========== */
.header {
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

.header-content {
  width: 100%;
  max-width: var(--it-shell-max);
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 520px) auto;
  align-items: center;
  gap: 16px;
  padding: 0 var(--it-shell-padding-x);
}

/* 左侧Logo */
.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.brand-block {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.brand-block:hover {
  transform: translateY(-1px);
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
  box-shadow: var(--it-shadow);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 15px;
  color: var(--it-text);
}

.brand-copy span {
  font-size: 12px;
  color: var(--it-text-muted);
}

/* 中间搜索区域 */
.header-center {
  min-width: 0;
  width: 100%;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.search-type-select {
  width: 100px;
  flex: 0 0 100px;
}

.search-type-select :deep(.el-input__inner) {
  border-radius: var(--it-radius-control) 0 0 var(--it-radius-control);
  border-right: none;
  background: var(--it-surface-solid);
  border-color: var(--it-border);
  color: var(--it-text);
}

.search-input {
  flex: 1 1 auto;
  min-width: 0;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 var(--it-radius-control) var(--it-radius-control) 0;
  background: var(--it-accent);
  border-color: var(--it-accent);
  color: white;
}

.search-input :deep(.el-input-group__append:hover) {
  background: var(--it-accent-hover);
  border-color: var(--it-accent-hover);
}

/* 右侧用户区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: fit-content;
}

.publish-btn {
  border-radius: var(--it-radius-control);
  padding: 8px 16px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.publish-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}


/* ========== 主体容器 ========== */
.main-container {
  flex: 1;
  overflow: hidden;
}

/* ========== 侧边栏样式 ========== */
.sidebar {
  background: var(--it-sidebar-bg);
  transition: width .24s ease;
  border-right: 1px solid var(--it-border);
  overflow-y: auto;
}

.project-menu {
  border: none;
  height: 100%;
  background: transparent !important;
}

.project-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--it-radius-control);
  transition: all 0.3s ease;
  color: var(--it-text-muted) !important;
  background: transparent !important;
}

.project-menu :deep(.el-menu-item:hover) {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.project-menu :deep(.el-menu-item.is-active) {
  background: var(--it-primary-gradient) !important;
  color: white !important;
  box-shadow: var(--it-shadow);
}

.project-menu :deep(.el-submenu__title) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--it-radius-control);
  transition: all 0.3s ease;
  color: var(--it-text-muted) !important;
  background: transparent !important;
}

.project-menu :deep(.el-submenu__title:hover) {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.project-menu :deep(.el-divider) {
  margin: 12px 8px;
}

/* ========== 内容区域 ========== */
.content-area {
  padding: 28px 24px 36px;
  background: transparent;
  color: var(--it-text);
  overflow-y: auto;
}

/* ========== 响应式设计 ========== */
@media screen and (max-width: 768px) {
  .header-content {
    padding: 0 15px;
  }
  
  .header-center {
    margin: 0 20px;
    max-width: 300px;
  }
  
  .logo-text {
    display: none;
  }
  
  .username {
    display: none;
  }
  
  .sidebar {
    width: 180px !important;
  }
}

@media screen and (max-width: 480px) {
  .header-center {
    display: none;
  }
  
  .publish-btn span {
    display: none;
  }
  
  .publish-btn {
    padding: 8px;
  }
}

.guest-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

</style>
