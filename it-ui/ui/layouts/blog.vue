<template>
  <div class="layout-container">
    <el-header>
      <div class="header-content">
        <!-- 搜索框（在非详情/写博客页显示） -->
        <el-input
          v-if="!isSpecialPage"
          placeholder="请输入搜索内容"
          prefix-icon="el-icon-search"
          v-model="searchKeyword"
          class="search-input"
          @keyup.enter.native="handleSearch"
          clearable
        ></el-input>
        <el-button v-if="!isSpecialPage" type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
        <el-button type="info" @click="goToWrite" plain class="write-btn">写文章</el-button>
        <div class="avatar-wrapper" @click="goToUserHome">
          <el-avatar :size="50" :src="userAvatar"></el-avatar>
        </div>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边菜单 -->
      <el-aside width="200px" class="asid-content">
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          router
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
          <el-tab-pane label="全部" name="全部"></el-tab-pane>
          <el-tab-pane label="Java" name="Java"></el-tab-pane>
          <el-tab-pane label="Python" name="Python"></el-tab-pane>
          <el-tab-pane label="JavaScript" name="JavaScript"></el-tab-pane>
          <el-tab-pane label="Spring Boot" name="Spring Boot"></el-tab-pane>
          <el-tab-pane label="Django" name="Django"></el-tab-pane>
          <el-tab-pane label="React" name="React"></el-tab-pane>
          <el-tab-pane label="Git" name="Git"></el-tab-pane>
          <el-tab-pane label="Docker" name="Docker"></el-tab-pane>
          <el-tab-pane label="Maven" name="Maven"></el-tab-pane>
        </el-tabs>
        <!-- 路由页面内容 -->
        <nuxt />
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  data() {
    return {
      searchKeyword: '',
      activeTag: 'all',
      // 菜单项配置
      menuItems: [
        { index: '/', icon: 'el-icon-s-home', title: '首页' },
        { index: '/blog', icon: 'el-icon-document', title: '博客' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' }, // 稍后动态处理
      ],
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', // 可从store获取
    };
  },
  computed: {
    // 判断当前路由是否为特殊页面（详情页或写博客页）
    isSpecialPage() {
      const path = this.$route.path;
      return path.startsWith('/blog/') || path.startsWith('/write');
    },
    isHomePage() {
      return this.$route.path === '/' || this.$route.path === '/blog';
    },
    // 动态计算当前激活的菜单项
    activeMenu() {
      const path = this.$route.path;
      // 如果路径以 /blog 开头（包括详情页），激活博客菜单
      if (path.startsWith('/blog')) {
        return '/blog';
      }
      // 如果路径以 /user 开头，激活个人中心菜单
      if (path.startsWith('/user')) {
        return '/user';
      }
      // 如果路径以 /circle 开头，激活圈子菜单
      if (path.startsWith('/circle')) {
        return '/circle';
      }
      // 默认首页
      return '/';
    },
    // 获取当前用户ID（从Vuex，若无则使用默认值）
    userId() {
      return this.$store?.state?.user?.id || 1; // 假设store存在
    },
  },
  watch: {
    // 从路由初始化搜索框和标签
    '$route.query': {
      handler(query) {
        this.searchKeyword = query.keyword || '';
        this.activeTag = query.tag || 'all';
      },
      immediate: true,
    },
  },
  methods: {
    handleOpen(key, keyPath) {
      console.log('菜单打开', key, keyPath);
    },
    handleClose(key, keyPath) {
      console.log('菜单关闭', key, keyPath);
    },
    // 处理搜索
    handleSearch() {
      this.$router.push({
        query: {
          ...this.$route.query,
          keyword: this.searchKeyword || undefined,
          page: 1,
        },
      });
    },
    // 处理标签点击
    handleTagClick(tab) {
      this.$router.push({
        path: '/blog',
        query: {
          tag: tab.name === '全部' ? undefined : tab.name,
          page: 1,
        },
      });
    },
    // 跳转到写博客
    goToWrite() {
      this.$router.push('/blogwrite');
    },
    // 跳转到用户主页
    goToUserHome() {
      this.$router.push(`/user/${this.userId}`);
    },
  },
};
</script>

<style scoped>
/* 原有样式保留，此处省略以节省篇幅，请参照之前的样式 */
.layout-container {
  background-color: #d4d4d4;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.header-content {
  background-color: #f6eeee;
  color: #000000;
  font-weight: 500 !important;
  margin: 10px 0;
  padding: 0;
  width: 100%;
  min-height: 40px;
  box-sizing: border-box;
}

.search-input {
  margin: 0 10px;
  padding: 0;
  width: 300px;
  min-height: 60px;
  box-sizing: border-box;
}

.search-btn {
  margin: 0 auto;
  padding: 0;
  width: 100px;
  min-height: 30px;
  box-sizing: border-box;
}

.write-btn {
  margin: 0 auto;
  padding: 0;
  width: 100px;
  min-height: 30px;
  box-sizing: border-box;
}

.avatar-wrapper {
  margin: 0 auto;
  padding: 0;
  width: 100px;
  min-height: 60px;
  box-sizing: border-box;
  cursor: pointer;
}

.main-content {
  background-color: #d4d4d4;
  color: #000000;
  font-weight: 500 !important;
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}

.asid-content {
  background-color: #f6eeee;
  color: #000000;
  font-weight: 500 !important;
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}

/* 侧边菜单样式微调 */
.el-menu {
  border-right: none;
  background-color: #f6eeee;
}
</style>