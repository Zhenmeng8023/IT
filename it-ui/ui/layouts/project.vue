<template>
  <div class="project-layout">
    <el-header class="header">
      <div class="header-content">
        <div class="header-left">
          <div class="logo" @click="$router.push('/')">
            <i class="el-icon-s-promotion"></i>
            <span class="logo-text">项目中心</span>
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
            <div class="user-info" @click="goToUserProfile">
              <el-avatar
                :size="32"
                :src="userAvatar"
                class="user-avatar"
              ></el-avatar>
              <span class="username">{{ username }}</span>
              <i class="el-icon-arrow-down"></i>
            </div>
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
      <el-aside width="220px" class="sidebar">
        <el-menu
          :default-active="$route.path"
          class="project-menu"
          background-color="#f8fafc"
          text-color="#000000"
          active-text-color="#3b82f6"
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

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

export default {
  name: 'ProjectLayout',
  data() {
    return {
      searchType: 'keyword',
      searchKeyword: '',
      searching: false,
      userAvatar: DEFAULT_AVATAR,
      username: '游客'
    }
  },
  computed: {
    isLoggedIn() {
      return !!getToken() || !!getCurrentUser()
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
    syncUserState() {
      const user = getCurrentUser()
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
      const protectedRoutes = new Set(['/myproject', '/projectcollection', '/wallet', '/vip', '/user'])
      if (protectedRoutes.has(index) && !this.ensureLogin('访问该页面')) {
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
/* ========== 布局容器 ========== */
.project-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

/* ========== 头部样式 ========== */
.header {
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border-bottom: 1px solid #e2e8f0;
  height: 64px;
  display: flex;
  align-items: center;
  z-index: 1000;
}

.header-content {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

/* 左侧Logo */
.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logo:hover {
  transform: translateY(-1px);
}

.logo i {
  font-size: 24px;
  color: #3b82f6;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 中间搜索区域 */
.header-center {
  flex: 1;
  max-width: 500px;
  margin: 0 40px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-type-select {
  width: 100px;
}

.search-type-select :deep(.el-input__inner) {
  border-radius: 20px 0 0 20px;
  border-right: none;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 20px 20px 0;
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.search-input :deep(.el-input-group__append:hover) {
  background: #2563eb;
  border-color: #2563eb;
}

/* 右侧用户区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.publish-btn {
  border-radius: 20px;
  padding: 8px 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.publish-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: #f1f5f9;
}

.username {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.user-info .el-icon-arrow-down {
  font-size: 12px;
  color: #94a3b8;
}

/* ========== 主体容器 ========== */
.main-container {
  flex: 1;
  overflow: hidden;
}

/* ========== 侧边栏样式 ========== */
.sidebar {
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  overflow-y: auto;
}

.project-menu {
  border: none;
  height: 100%;
}

.project-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.project-menu :deep(.el-menu-item:hover) {
  background: #e2e8f0 !important;
}

.project-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.project-menu :deep(.el-submenu__title) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.project-menu :deep(.el-submenu__title:hover) {
  background: #e2e8f0 !important;
}

.project-menu :deep(.el-divider) {
  margin: 12px 8px;
}

/* ========== 内容区域 ========== */
.content-area {
  padding: 24px;
  background: transparent;
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
