<template>
  <div class="project-layout">
    <FrontNavShell
      brand-subtitle="项目空间"
      :show-search="true"
      :menu-groups="menuGroups"
      :active-menu="activeMenu"
      :aside-width="asideWidth"
      :menu-collapsed="menuCollapsed"
      :is-compact="isCompact"
      main-class="project-main-content"
      @toggle-sidebar="toggleSidebar"
      @menu-select="handleMenuSelect"
    >
      <template #search>
        <div class="search-container">
          <el-select
            v-model="searchType"
            size="small"
            class="search-type-select"
            @change="handleSearchTypeChange"
          >
            <el-option label="关键字" value="keyword"></el-option>
            <el-option label="技术栈" value="tech"></el-option>
            <el-option label="作者" value="author"></el-option>
          </el-select>

          <el-input
            v-model="searchKeyword"
            placeholder="搜索项目..."
            size="small"
            class="search-input"
            @keyup.enter.native="handleSearch"
          >
            <template #append>
              <el-button icon="el-icon-search" :loading="searching" @click="handleSearch"></el-button>
            </template>
          </el-input>
        </div>
      </template>

      <template #actions>
        <ThemeToggle />
        <el-button
          type="primary"
          size="small"
          icon="el-icon-plus"
          class="publish-btn"
          @click="goToCreateProject"
        >
          {{ resolvedIsLoggedIn ? '发布项目' : '登录后发布' }}
        </el-button>

        <template v-if="resolvedIsLoggedIn">
          <AppUserMenu :size="36" />
        </template>
        <template v-else>
          <div class="guest-actions">
            <el-button size="small" @click="goToLogin">登录</el-button>
            <el-button size="small" plain @click="goToRegister">注册</el-button>
          </div>
        </template>
      </template>

      <nuxt />
    </FrontNavShell>

    <client-only>
      <AIAssistant />
      <SceneAiDock scene="project.detail" />
    </client-only>
  </div>
</template>

<script>
import FrontNavShell from '@/components/front/FrontNavShell.vue'
import { getFrontNavigationGroups, isFrontProtectedRoute, resolveFrontActiveMenu } from '@/components/front/frontNavigation'
import { getCurrentUser, getToken } from '@/utils/auth'

function readStoredToken() {
  return getToken() || ''
}

function readCurrentUser() {
  return getCurrentUser()
}

export default {
  name: 'ProjectLayout',
  components: {
    FrontNavShell
  },
  data() {
    return {
      clientHydrated: false,
      searchType: 'keyword',
      searchKeyword: '',
      searching: false,
      isCollapse: false,
      isCompact: false
    }
  },
  computed: {
    isLoggedIn() {
      return !!readStoredToken() || !!readCurrentUser()
    },
    resolvedIsLoggedIn() {
      return this.clientHydrated && this.isLoggedIn
    },
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
      return this.isCollapse ? '84px' : '228px'
    },
    menuCollapsed() {
      return this.isCompact ? false : this.isCollapse
    }
  },
  mounted() {
    this.clientHydrated = true
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
    toggleSidebar() {
      if (this.isCompact) {
        return
      }
      this.isCollapse = !this.isCollapse
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
        this.$message.error('搜索失败，请稍后重试')
      } finally {
        this.searching = false
      }
    },
    goToCreateProject() {
      if (!this.ensureLogin('发布项目')) return
      this.$router.push('/myproject?create=1')
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
.project-layout {
  min-height: 100vh;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  width: 100%;
}

.search-type-select {
  width: 100px;
  flex: 0 0 100px;
}

.search-type-select :deep(.el-input__inner) {
  border-radius: 12px 0 0 12px;
  border-right: none;
}

.search-input {
  flex: 1 1 auto;
  min-width: 0;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 12px 12px 0;
  background: var(--it-accent);
  border-color: var(--it-accent);
  color: #fff;
}

.publish-btn {
  border-radius: var(--it-radius-control);
  padding: 8px 16px;
  font-weight: 600;
}

.guest-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.project-main-content {
  width: 100%;
  max-width: none;
  min-width: 0;
}

@media screen and (max-width: 768px) {
  .search-type-select {
    display: none;
  }

  .guest-actions {
    display: none;
  }
}
</style>
