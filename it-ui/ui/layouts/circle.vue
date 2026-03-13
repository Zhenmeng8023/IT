<template>
    <div class="layout-container">
      <!-- 头部：包含折叠按钮、搜索框、写文章按钮、头像 -->
      <el-header>
        <div class="header-content">
          <!-- 折叠按钮（始终显示） -->
          <div class="collapse-btn" @click="toggleSidebar">
            <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </div>
  
          <!-- 搜索区域（只在非详情/写博客页显示） -->
          <div v-if="!isSpecialPage" class="search-area">
            <el-input
              placeholder="输入想看的帖子或者圈名吧"
              prefix-icon="el-icon-search"
              v-model="searchKeyword"
              class="search-input"
              @keyup.enter.native="handleSearch"
              clearable
            ></el-input>
            <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
          </div>
  
          <!-- 右侧操作区：写文章按钮 + 头像 -->
          <div class="right-actions">
            <el-button type="info" @click="goToWrite" plain class="write-btn">发帖子</el-button>
            <div class="avatar-wrapper" @click="goToUserHome">
              <el-avatar :size="50" :src="userAvatar"></el-avatar>
            </div>
          </div>
        </div>
      </el-header>
  
      <!-- 主体：侧边栏 + 内容区 -->
      <el-container>
        <!-- 侧边栏（可折叠） -->
        <el-aside :width="asideWidth" class="asid-content">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical-demo"
            router
            :collapse="isCollapse"
            :collapse-transition="true"
          >
            <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
              <i :class="item.icon"></i>
              <span slot="title">{{ item.title }}</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
  
        <!-- 主内容区 -->
        <el-main class="main-content">
          <!-- 标签页（仅在首页显示） -->
          <!-- <el-tabs v-if="isHomePage" v-model="activeTag" @tab-click="handleTagClick">
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="前端" name="frontend"></el-tab-pane>
            <el-tab-pane label="后端" name="backend"></el-tab-pane>
            <el-tab-pane label="数据库" name="database"></el-tab-pane>
          </el-tabs> -->
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
        isCollapse: false,               // 侧边栏折叠状态
        searchKeyword: '',
        activeTag: 'all',
        menuItems: [
          { index: '/', icon: 'el-icon-s-home', title: '首页' },
          { index: '/blog', icon: 'el-icon-document', title: '博客' },
          { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
          { index: '/user', icon: 'el-icon-user', title: '个人中心' },
        ],
        userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      };
    },
    computed: {
      // 侧边栏宽度，根据折叠状态动态变化
      asideWidth() {
        return this.isCollapse ? '64px' : '200px';
      },
      // 是否为特殊页面（博客详情或写博客页）
      isSpecialPage() {
        const path = this.$route.path;
        return path.startsWith('/blog/') || path.startsWith('/write');
      },
      // 是否为首页（显示标签页）
      isHomePage() {
        return this.$route.path === '/' || this.$route.path === '/blog';
      },
      // 当前激活的菜单项（用于高亮）
      activeMenu() {
        const path = this.$route.path;
        if (path.startsWith('/blog')) return '/blog';
        if (path.startsWith('/circle')) return '/circle';
        if (path.startsWith('/user')) return '/user';
        return '/';
      },
      // 当前用户ID（从 Vuex 获取，若无则默认 1）
      userId() {
        return this.$store?.state?.user?.id || 1;
      },
    },
    watch: {
      // 监听路由 query，同步搜索框和标签
      '$route.query': {
        handler(query) {
          this.searchKeyword = query.keyword || '';
          this.activeTag = query.tag || 'all';
        },
        immediate: true,
      },
    },
    methods: {
      // 切换侧边栏折叠状态
      toggleSidebar() {
        this.isCollapse = !this.isCollapse;
      },
      // 处理搜索：更新路由 query
      handleSearch() {
        this.$router.push({
          query: {
            ...this.$route.query,
            keyword: this.searchKeyword || undefined,
            page: 1, // 重置到第一页
          },
        });
      },
      // 跳转帖子发布页面
      goToWrite() {
        this.$router.push('/write');
      },
      // 跳转到用户主页
      goToUserHome() {
        this.$router.push(`/user/${this.userId}`);
      },
    },
  };
  </script>
  
  <style scoped>
  .layout-container {
    background-color: #d4d4d4;
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }
  
  /* 头部样式 */
  .el-header {
    padding: 0;
    background-color: #f6eeee;
  }
  .header-content {
    display: flex;
    align-items: center;
    height: 60px;
    padding: 0 20px;
  }
  
  /* 折叠按钮 */
  .collapse-btn {
    width: 50px;
    text-align: center;
    font-size: 22px;
    cursor: pointer;
    color: #606266;
    transition: color 0.2s;
  }
  .collapse-btn:hover {
    color: #409EFF;
  }
  
  /* 搜索区域（居中） */
  .search-area {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 0 20px;
  }
  .search-input {
    width: 60%;
    max-width: 600px;
  }
  .search-btn {
    margin-left: 10px;
  }
  
  /* 右侧操作区 */
  .right-actions {
    display: flex;
    align-items: center;
    gap: 15px;
  }
  .avatar-wrapper {
    cursor: pointer;
  }
  
  /* 侧边栏样式 */
  .asid-content {
    transition: width 0.3s;
    overflow-x: hidden;
    background-color: #f6eeee;
  }
  .el-menu:not(.el-menu--collapse) {
    width: 200px;
  }
  .el-menu {
    border-right: none;
    background-color: #f6eeee;
  }
  
  /* 主内容区 */
  .main-content {
    background-color: #d4d4d4;
    min-height: calc(100vh - 60px);
    padding: 20px;
  }
  
  /* 分页等样式已在页面组件中定义，此处无需重复 */
  </style>