<template>
  <div data-testid="blog-layout" class="blog-layout">
    <FrontNavShell
      brand-subtitle="博客空间"
      :show-search="!isSpecialPage"
      :menu-groups="menuGroups"
      :active-menu="activeMenu"
      :aside-width="asideWidth"
      :menu-collapsed="menuCollapsed"
      :is-compact="isCompact"
      @toggle-sidebar="toggleSidebar"
      @menu-select="handleMenuSelect"
    >
      <template #search>
        <el-select
          data-testid="blog-search-type-select"
          v-model="searchType"
          placeholder="关键词"
          class="search-type-select"
          @change="handleSearchTypeChange"
        >
          <el-option label="关键词" value="keyword"></el-option>
          <el-option label="标签" value="tag"></el-option>
          <el-option label="作者" value="author"></el-option>
        </el-select>

        <el-input
          data-testid="blog-search-input"
          v-model="searchKeyword"
          :placeholder="getPlaceholderByType()"
          class="search-input"
          clearable
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
      </template>

      <template #actions>
        <ThemeToggle />
        <el-button
          data-testid="blog-write-open"
          type="info"
          plain
          class="write-btn"
          @click="goToWrite"
        >
          写文章
        </el-button>
        <AppUserMenu :size="36" />
      </template>

      <template #main-prefix>
        <el-tabs v-if="isHomePage" v-model="activeTag" @tab-click="handleTagClick">
          <el-tab-pane
            v-for="tagName in visibleTagTabs"
            :key="tagName"
            :label="tagName"
            :name="tagName"
          ></el-tab-pane>
        </el-tabs>
      </template>

      <nuxt />
    </FrontNavShell>
  </div>
</template>

<script>
import { GetAllTags, GetHotTags } from '@/api/index'
import { useUserStore } from '@/store/user'
import FrontNavShell from '@/components/front/FrontNavShell.vue'
import { getFrontNavigationGroups, isFrontProtectedRoute, resolveFrontActiveMenu } from '@/components/front/frontNavigation'

export default {
  components: {
    FrontNavShell
  },
  data() {
    return {
      searchType: 'keyword',
      searchKeyword: '',
      activeTag: '全部',
      hotTags: [],
      isCollapse: false,
      isCompact: false
    }
  },
  computed: {
    menuGroups() {
      return getFrontNavigationGroups()
    },
    asideWidth() {
      if (this.isCompact) {
        return '100%'
      }
      return this.isCollapse ? '84px' : '228px'
    },
    menuCollapsed() {
      return this.isCompact ? false : this.isCollapse
    },
    isSpecialPage() {
      const path = this.$route.path
      return path.startsWith('/blog/') || path.startsWith('/write')
    },
    isHomePage() {
      return this.$route.path === '/' || this.$route.path === '/blog'
    },
    activeMenu() {
      return resolveFrontActiveMenu(this.$route)
    },
    visibleTagTabs() {
      const tags = ['全部', ...this.hotTags.map(item => item.name).filter(Boolean)]
      if (this.$route.query.tag && !tags.includes(this.$route.query.tag)) {
        tags.push(this.$route.query.tag)
      }
      return [...new Set(tags)].slice(0, 10)
    }
  },
  watch: {
    '$route.query': {
      handler(query) {
        if (query.type === 'tag') {
          this.searchType = 'tag'
          this.searchKeyword = query.tag || ''
        } else if (query.type === 'author') {
          this.searchType = 'author'
          this.searchKeyword = query.author || ''
        } else {
          this.searchType = 'keyword'
          this.searchKeyword = query.keyword || ''
        }
        this.activeTag = query.tag || '全部'
      },
      immediate: true
    }
  },
  created() {
    this.fetchHotTags()
    this.restoreSession()
  },
  mounted() {
    this.syncViewport()
    window.addEventListener('resize', this.syncViewport, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewport)
  },
  methods: {
    getUserStore() {
      return useUserStore()
    },
    syncViewport() {
      if (!process.client) {
        return
      }
      this.isCompact = window.innerWidth <= 960
    },
    async restoreSession() {
      if (!process.client) {
        return
      }

      const userStore = this.getUserStore()
      userStore.restorePermissions()

      if (!userStore.userInfo && !userStore.token) {
        return
      }

      try {
        await userStore.syncSessionFromServer({
          forceReloadPermissions: !userStore.permissions?.length
        })
      } catch (error) {
        userStore.clearLocalState()
      }
    },
    ensureAuthenticated(actionName = '继续操作') {
      const userStore = this.getUserStore()
      if (userStore.isLoggedIn && (userStore.userInfo || userStore.user)) {
        return true
      }

      this.$confirm(`${actionName}前需要先登录，是否前往登录页？`, '未登录', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.$router.push({
          path: '/login',
          query: { redirect: '/blogwrite' }
        })
      }).catch(() => {})
      return false
    },
    extractList(payload) {
      if (Array.isArray(payload)) return payload
      if (Array.isArray(payload?.data)) return payload.data
      if (Array.isArray(payload?.data?.data)) return payload.data.data
      if (Array.isArray(payload?.list)) return payload.list
      return []
    },
    async fetchHotTags() {
      try {
        let tags = this.extractList(await GetHotTags())
        if (!tags.length) {
          tags = this.extractList(await GetAllTags())
        }
        this.hotTags = tags.slice(0, 9)
      } catch (error) {
        this.hotTags = []
      }
    },
    toggleSidebar() {
      if (this.isCompact) {
        return
      }
      this.isCollapse = !this.isCollapse
    },
    getPlaceholderByType() {
      if (this.searchType === 'tag') return '请输入标签名搜索'
      if (this.searchType === 'author') return '请输入作者名搜索'
      return '请输入关键词搜索'
    },
    handleSearchTypeChange() {
      this.searchKeyword = ''
      if (this.$route.query.type) {
        this.$router.push({
          path: '/blog',
          query: { page: 1 }
        })
      }
    },
    handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.$router.push({
          path: '/blog',
          query: { page: 1 }
        })
        return
      }

      const newQuery = {
        page: 1,
        type: this.searchType
      }

      if (this.searchType === 'keyword') {
        newQuery.keyword = this.searchKeyword
      } else if (this.searchType === 'tag') {
        newQuery.tag = this.searchKeyword
      } else if (this.searchType === 'author') {
        newQuery.author = this.searchKeyword
      }

      this.$router.push({
        path: '/blog',
        query: newQuery
      })
    },
    handleTagClick(tab) {
      if (tab.name === '全部') {
        this.$router.push({
          path: '/blog',
          query: { page: 1 }
        })
        return
      }

      this.$router.push({
        path: '/blog',
        query: {
          tag: tab.name,
          type: 'tag',
          page: 1
        }
      })
    },
    goToWrite() {
      if (!this.ensureAuthenticated('写文章')) {
        return
      }
      this.$router.push('/blogwrite')
    },
    handleMenuSelect(index) {
      if (index === '/') {
        if (process.client && window.location.pathname !== '/') {
          window.location.assign('/')
        }
        return
      }
      if (isFrontProtectedRoute(index) && !this.ensureAuthenticated('访问该页面')) {
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
.search-type-select {
  width: 112px;
  flex: 0 0 112px;
}

.search-input {
  flex: 1 1 auto;
  min-width: 0;
}

.search-type-select :deep(.el-input__inner),
.search-input :deep(.el-input__inner),
.search-input :deep(.el-input-group__append) {
  height: 40px;
  line-height: 40px;
}

.search-type-select :deep(.el-input__inner) {
  border-radius: 12px 0 0 12px;
  border-right: none;
}

.search-input :deep(.el-input__inner) {
  border-radius: 0;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 12px 12px 0;
  border-color: transparent;
  background: var(--it-primary-gradient);
  color: #fff;
}

.search-input :deep(.el-input-group__append .el-button) {
  color: inherit;
}

.write-btn {
  min-width: 98px;
  height: 40px;
  padding: 0 16px;
  border-radius: 12px;
  border-color: var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text);
  font-size: 14px;
  font-weight: 600;
}

.write-btn:hover,
.write-btn:focus {
  color: var(--it-accent);
  border-color: color-mix(in srgb, var(--it-accent) 30%, var(--it-border));
  background: var(--it-accent-soft);
}

:deep(.main-content > .el-tabs) {
  margin-bottom: 18px;
  padding: 0 16px;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  background: var(--it-surface-solid);
  box-shadow: var(--it-shadow-soft);
}

@media screen and (max-width: 768px) {
  .search-type-select {
    width: 100%;
    flex: 1 1 auto;
  }

  .search-type-select :deep(.el-input__inner) {
    border-radius: 12px;
    border-right: 1px solid var(--it-border);
  }

  .search-input :deep(.el-input__inner) {
    border-radius: 12px 0 0 12px;
  }
}
</style>
