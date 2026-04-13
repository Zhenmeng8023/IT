<template>
  <div class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <div class="header-left">
          <button class="collapse-btn" type="button" @click="toggleSidebar">
            <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>
          <div class="brand-block" @click="$router.push('/')">
            <span class="brand-mark">IT</span>
            <div class="brand-copy">
              <strong>IT Forum</strong>
              <span>博客空间</span>
            </div>
          </div>
        </div>

        <!-- 搜索区域（在非详情/写博客页显示） -->
        <div v-if="!isSpecialPage" class="search-area">
          <!-- 搜索类型下拉选择器 -->
          <el-select
            v-model="searchType"
            placeholder="搜索类型"
            class="search-type-select"
            @change="handleSearchTypeChange"
          >
            <el-option label="关键词" value="keyword"></el-option>
            <el-option label="标签" value="tag"></el-option>
            <el-option label="作者" value="author"></el-option>
          </el-select>

          <!-- 搜索输入框，placeholder 根据搜索类型动态变化 -->
          <el-input
            v-model="searchKeyword"
            :placeholder="getPlaceholderByType()"
            class="search-input"
            @keyup.enter.native="handleSearch"
            clearable
          >
            <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
          </el-input>
        </div>

        <!-- 右侧操作区：写文章按钮 + 头像 -->
        <div class="right-actions">
          <ThemeToggle />
          <el-button type="info" @click="goToWrite" plain class="write-btn">写文章</el-button>
          <AppUserMenu :size="36" />
        </div>
      </div>
    </el-header>

    <el-container class="main-shell">
      <!-- 侧边菜单 -->
      <el-aside :width="asideWidth" class="asid-content" :class="{ 'is-collapsed': isCollapse }">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          class="el-menu-vertical-demo module-menu"
          @select="handleMenuSelect"
          @open="handleOpen"
          @close="handleClose"
        >
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区域 -->
      <el-main class="main-content">
        <!-- 标签页（仅在首页显示） -->
        <el-tabs v-if="isHomePage" v-model="activeTag" @tab-click="handleTagClick">
          <el-tab-pane
            v-for="tagName in visibleTagTabs"
            :key="tagName"
            :label="tagName"
            :name="tagName"
          ></el-tab-pane>
        </el-tabs>
        <!-- 路由页面内容 -->
        <nuxt />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { GetAllTags, GetHotTags } from '@/api/index'
import { useUserStore } from '@/store/user'

export default {
  data() {
    return {
      searchType: 'keyword',      // 搜索类型：keyword、tag 或 author
      searchKeyword: '',          // 搜索关键词
      activeTag: '全部',          // 当前选中的标签
      hotTags: [],
      isCollapse: false,
      // 菜单项配置
      menuItems: [
        { index: '/', icon: 'el-icon-s-home', title: '首页' },
        { index: '/blog', icon: 'el-icon-document', title: '博客' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/projectlist', icon: 'el-icon-document', title: '项目列表' },
        { index: '/myproject', icon: 'el-icon-folder-opened', title: '我的项目' },
        { index: '/projectcollection', icon: 'el-icon-star-on', title: '收藏项目' },
        { index: '/wallet', icon: 'el-icon-wallet', title: '我的钱包' },
        { index: '/vip', icon: 'el-icon-crown', title: 'VIP服务' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' },
      ],
    };
  },
  computed: {
    asideWidth() {
      return this.isCollapse ? '72px' : '200px'
    },
    // 判断当前路由是否为特殊页面（详情页或写博客页）
    isSpecialPage() {
      const path = this.$route.path;
      return path.startsWith('/blog/') || path.startsWith('/write');
    },
    // 判断是否为首页（博客列表页）
    isHomePage() {
      return this.$route.path === '/' || this.$route.path === '/blog';
    },
    // 动态计算当前激活的菜单项
    activeMenu() {
      const path = this.$route.path;
      if (path.startsWith('/blog')) {
        return '/blog';
      }
      if (path.startsWith('/user')) {
        return '/user';
      }
      if (path.startsWith('/circle')) {
        return '/circle';
      }
      if (path.startsWith('/projectlist')) {
        return '/projectlist';
      }
      if (path.startsWith('/myproject')) {
        return '/myproject';
      }
      if (path.startsWith('/projectcollection')) {
        return '/projectcollection';
      }
      if (path.startsWith('/wallet')) {
        return '/wallet';
      }
      if (path.startsWith('/vip')) {
        return '/vip';
      }
      return '/';
    },
    // 获取当前用户ID（从Vuex，若无则使用默认值）
    userId() {
      return this.$store?.state?.user?.id || 1;
    },
    visibleTagTabs() {
      const tags = ['全部', ...this.hotTags.map(item => item.name).filter(Boolean)];
      if (this.$route.query.tag && !tags.includes(this.$route.query.tag)) {
        tags.push(this.$route.query.tag);
      }
      return [...new Set(tags)].slice(0, 10);
    }
  },
  watch: {
    // 从路由初始化搜索框、搜索类型和标签
    '$route.query': {
      handler(query) {
        // 根据路由中的 type 参数设置搜索类型
        if (query.type === 'tag') {
          this.searchType = 'tag';
          this.searchKeyword = query.tag || '';
        } else if (query.type === 'author') {
          this.searchType = 'author';
          this.searchKeyword = query.author || '';
        } else {
          this.searchType = 'keyword';
          this.searchKeyword = query.keyword || '';
        }
        // 设置当前选中的标签（从路由的 tag 参数）
        this.activeTag = query.tag || '全部';
      },
      immediate: true,
    },
  },
  created() {
    this.fetchHotTags();
    this.restoreSession();
  },
  methods: {
    getUserStore() {
      return useUserStore()
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
    ensureAuthenticated() {
      const userStore = this.getUserStore()
      if (userStore.isLoggedIn && (userStore.userInfo || userStore.user)) {
        return true
      }

      this.$confirm('写文章前需要先登录，是否前往登录页？', '未登录', {
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
      if (Array.isArray(payload)) return payload;
      if (Array.isArray(payload?.data)) return payload.data;
      if (Array.isArray(payload?.data?.data)) return payload.data.data;
      if (Array.isArray(payload?.list)) return payload.list;
      return [];
    },
    async fetchHotTags() {
      try {
        let tags = this.extractList(await GetHotTags());
        if (!tags.length) {
          tags = this.extractList(await GetAllTags());
        }
        this.hotTags = tags.slice(0, 9);
      } catch (error) {
        console.error('加载标签导航失败:', error);
        this.hotTags = [];
      }
    },
    toggleSidebar() {
      this.isCollapse = !this.isCollapse
    },
    handleOpen(key, keyPath) {
      console.log('菜单打开', key, keyPath);
    },
    handleClose(key, keyPath) {
      console.log('菜单关闭', key, keyPath);
    },

    // 根据搜索类型返回对应的placeholder
    getPlaceholderByType() {
      switch(this.searchType) {
        case 'keyword':
          return '请输入关键词搜索';
        case 'tag':
          return '请输入标签名称';
        case 'author':
          return '请输入作者名称';
        default:
          return '请输入搜索内容';
      }
    },

    // 处理搜索类型变化
    handleSearchTypeChange() {
      this.searchKeyword = ''; // 清空输入框
      // 如果当前有搜索参数，清除并重新搜索
      if (this.$route.query.type) {
        this.$router.push({
          path: '/blog',
          query: {
            page: 1,
          }
        });
      }
    },

    // 处理搜索
    handleSearch() {
      if (!this.searchKeyword.trim()) {
        // 如果搜索内容为空，清除搜索参数
        this.$router.push({
          path: '/blog',
          query: {
            page: 1,
          }
        });
        return;
      }

      // 根据搜索类型构造不同的 query 参数
      const newQuery = {
        page: 1,
        type: this.searchType,
      };

      if (this.searchType === 'keyword') {
        newQuery.keyword = this.searchKeyword;
      } else if (this.searchType === 'tag') {
        newQuery.tag = this.searchKeyword;
      } else if (this.searchType === 'author') {
        newQuery.author = this.searchKeyword;
      }

      this.$router.push({
        path: '/blog',
        query: newQuery,
      });
    },

    // 处理标签点击
    handleTagClick(tab) {
      // 如果点击的是"全部"，则清除标签筛选
      if (tab.name === '全部') {
        this.$router.push({
          path: '/blog',
          query: {
            page: 1,
          }
        });
      } else {
        this.$router.push({
          path: '/blog',
          query: {
            tag: tab.name,
            type: 'tag',
            page: 1,
          }
        });
      }
    },

    // 跳转到写博客
    goToWrite() {
      if (!this.ensureAuthenticated()) {
        return
      }
      this.$router.push('/blogwrite');
    },

    handleMenuSelect(index) {
      const protectedRoutes = new Set(['/myproject', '/projectcollection', '/wallet', '/vip', '/user'])
      if (index === '/') {
        if (process.client && window.location.pathname !== '/') {
          window.location.assign('/')
        }
        return
      }
      if (protectedRoutes.has(index) && !this.ensureAuthenticated('访问该页面')) {
        return
      }
      if (this.$route.path !== index) {
        this.$router.push(index).catch(() => {})
      }
    }
  },
};
</script>

<style scoped>
.layout-container {
  background: var(--it-page-bg);
  color: var(--it-text);
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.el-header {
  padding: 0;
  background: var(--it-header-bg);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--it-border);
  height: auto;
}

.header-content {
  width: 100%;
  max-width: var(--it-shell-max);
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 560px) auto;
  align-items: center;
  gap: 18px;
  padding: 12px var(--it-shell-padding-x);
  min-height: 72px;
  box-sizing: border-box;
}

.header-left {
  min-width: 0;
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

.brand-block {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
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
  font-weight: 800;
  font-size: 14px;
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

/* 搜索区域样式 - 居中显示 */
.search-area {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
  margin: 0;
}

.search-type-select {
  width: 108px;
  flex: 0 0 108px;
}

.search-input {
  flex: 1 1 auto;
  width: auto;
  min-width: 0;
}

/* 右侧操作区样式 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-shrink: 0;
  width: auto;
  justify-content: flex-end;
  min-width: fit-content;
}

.write-btn {
  min-width: 96px;
  border-radius: var(--it-radius-control);
  color: var(--it-accent);
  border-color: var(--it-border);
  background: var(--it-surface-solid);
}

.main-content {
  background: transparent;
  color: var(--it-text);
  font-weight: 500 !important;
  margin: 0;
  padding: 20px;
  min-height: calc(100vh - 70px);
  box-sizing: border-box;
}

.asid-content {
  background: var(--it-sidebar-bg);
  transition: width .24s ease;
  color: var(--it-text);
  font-weight: 500 !important;
  margin: 0;
  padding: 0;
  min-height: calc(100vh - 70px);
  box-sizing: border-box;
  width: 200px;
}

/* 侧边菜单样式微调 */
.el-menu {
  border-right: none;
  background: transparent;
}

.el-menu :deep(.el-menu-item) {
  color: var(--it-text-muted);
}

.el-menu :deep(.el-menu-item:hover),
.el-menu :deep(.el-menu-item.is-active) {
  background: var(--it-accent-soft);
  color: var(--it-accent);
}

/* 标签页样式调整 */
.el-tabs {
  margin-bottom: 20px;
  background: var(--it-surface);
  padding: 0 20px;
  border: 1px solid var(--it-border);
  border-radius: var(--it-radius-card);
  box-shadow: var(--it-shadow);
}

.el-tabs :deep(.el-tabs__item) {
  color: var(--it-text-muted);
}

.el-tabs :deep(.el-tabs__item.is-active),
.el-tabs :deep(.el-tabs__item:hover) {
  color: var(--it-accent);
}

.el-tabs :deep(.el-tabs__active-bar) {
  background: var(--it-accent);
}

/* 响应式调整 */
@media screen and (max-width: 1100px) {
  .header-content {
    grid-template-columns: auto auto;
    grid-template-areas:
      "left actions"
      "search search";
  }

  .header-left {
    grid-area: left;
  }

  .search-area {
    grid-area: search;
    min-width: 0;
  }

  .right-actions {
    grid-area: actions;
    width: auto;
  }
}

@media screen and (max-width: 768px) {
  .header-content {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 12px 16px;
  }

  .brand-copy span {
    display: none;
  }

  .search-area {
    width: 100%;
    max-width: 100%;
    min-width: auto;
    margin: 0;
    flex-wrap: wrap;
  }

  .search-type-select {
    width: 100%;
    flex: 1 1 100%;
  }

  .search-input {
    width: 100%;
    flex: 1 1 100%;
  }

  .right-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
    flex-wrap: wrap;
  }
}


/* ===== 第十轮：博客布局改为以项目中心为基底的统一侧栏壳层 ===== */
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

.layout-container {
  min-height: 100vh;
  background: var(--it-page-bg);
  color: var(--it-text);
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

.search-area {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-type-select {
  width: 112px;
  flex: 0 0 112px;
}

.search-type-select :deep(.el-input__inner) {
  border-radius: var(--it-radius-control) 0 0 var(--it-radius-control);
  border-right: none;
}

.search-input {
  flex: 1 1 auto;
  min-width: 0;
}

.search-input :deep(.el-input-group__append) {
  background: var(--it-accent);
  border-color: var(--it-accent);
  color: #fff;
  border-radius: 0 var(--it-radius-control) var(--it-radius-control) 0;
}

.right-actions {
  justify-content: flex-end;
  gap: 10px;
}

.write-btn {
  border-radius: var(--it-radius-control);
  font-weight: 600;
}

.main-shell {
  flex: 1;
  overflow: hidden;
}

.asid-content {
  width: auto !important;
  background: var(--it-sidebar-bg);
  border-right: 1px solid var(--it-border);
}

.module-menu {
  border: none;
  height: 100%;
  background: transparent !important;
}

.module-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--it-radius-control);
  color: var(--it-text-muted) !important;
  background: transparent !important;
}

.module-menu :deep(.el-menu-item:hover) {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.module-menu :deep(.el-menu-item.is-active) {
  background: var(--it-primary-gradient) !important;
  color: #fff !important;
  box-shadow: var(--it-shadow);
}

.main-content {
  padding: 24px;
  overflow-y: auto;
  background: transparent;
}

.main-content :deep(.el-tabs__header) {
  margin: 0 0 18px;
}

.main-content :deep(.el-tabs__nav-wrap::after) {
  background: var(--it-border);
}

@media screen and (max-width: 900px) {
  .header-content {
    grid-template-columns: 1fr auto;
  }

  .search-area {
    grid-column: 1 / -1;
    width: 100%;
  }
}

</style>
