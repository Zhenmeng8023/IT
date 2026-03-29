<template>
  <div class="layout-container">
    <el-header>
      <div class="header-content">
        <!-- 折叠按钮：点击切换侧边栏折叠状态 -->
        <div class="collapse-btn" @click="toggleSidebar">
          <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
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
            <!-- <notification-bell /> -->
          <el-button type="info" @click="goToWrite" plain class="write-btn">写文章</el-button>
          <div class="avatar-wrapper" @click="goToUserHome">
            <el-avatar :size="50" :src="userAvatar"></el-avatar>
          </div>
        </div>
      </div>
    </el-header>

    <div class="main-container">
      <!-- 侧边菜单 -->
      <div :class="['sidebar', { 'collapsed': isCollapse }]">
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          router
          :collapse="isCollapse"
          :collapse-transition="true"
          @open="handleOpen"
          @close="handleClose"
        >
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- 主内容区域 -->
      <div class="content-wrapper">
        <el-main class="main-content">
          <!-- 标签页（仅在首页显示） -->
          <div class="tags-container" v-if="isHomePage">
            <el-tabs v-model="activeTag" @tab-click="handleTagClick" class="centered-tabs">
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
          </div>
          <!-- 路由页面内容 -->
          <nuxt />
        </el-main>
      </div>
    </div>
  </div>
</template>

<script>
// import NotificationBell from '@/components/NotificationBell.vue'
export default {
    // components: {
    //     NotificationBell
    // },
  data() {
    return {
      isCollapse: false,               // 侧边栏折叠状态
      searchType: 'keyword',      // 搜索类型：keyword、tag 或 author
      searchKeyword: '',          // 搜索关键词
      activeTag: '全部',          // 当前选中的标签
      // 菜单项配置
      menuItems: [
        { index: '/', icon: 'el-icon-s-home', title: '首页' },
        { index: '/blog', icon: 'el-icon-document', title: '博客' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' },
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
      return '/';
    },
    // 获取当前用户ID（从Vuex，若无则使用默认值）
    userId() {
      return this.$store?.state?.user?.id || 1;
    },
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
  methods: {
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
      this.$router.push('/blogwrite');
    },
    
    // 跳转到用户主页
    goToUserHome() {
      this.$router.push(`/user`);
    },
    
    /**
     * 切换侧边栏折叠状态
     */
    toggleSidebar() {
      this.isCollapse = !this.isCollapse;
    },
  },
};
</script>

<style scoped>
/* 全局变量 */
:root {
  --primary-color: #409EFF;
  --secondary-color: #67C23A;
  --text-color: #303133;
  --text-color-secondary: #606266;
  --border-color: #E4E7ED;
  --background-color: #F5F7FA;
  --card-background: #FFFFFF;
  --hover-color: #ECF5FF;
  --shadow-light: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  --shadow-medium: 0 4px 16px 0 rgba(0, 0, 0, 0.12);
  --border-radius: 8px;
  --transition: all 0.3s ease;
}

.layout-container {
  background-color: var(--background-color);
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

/* 头部样式 */
.el-header {
  padding: 0;
  background-color: var(--card-background);
  height: auto;
  box-shadow: var(--shadow-light);
  z-index: 10;
}

.header-content {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  min-height: 72px;
  box-sizing: border-box;
  position: relative;
}

/* 折叠按钮 */
.collapse-btn {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  color: var(--text-color-secondary);
  transition: var(--transition);
  border-radius: 50%;
  margin-right: 16px;
}

.collapse-btn:hover {
  color: var(--primary-color);
  background-color: var(--hover-color);
}

/* 搜索区域样式 - 居中显示 */
.search-area {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 600px;
  margin: 0 auto;
  justify-content: center;
  width: 100%;
}
.search-type-select {
  width: 100px;
  border-radius: var(--border-radius);
  transition: var(--transition);
}

.search-type-select:hover {
  border-color: var(--primary-color);
}

.search-input {
  width: 350px;
  border-radius: var(--border-radius);
  transition: var(--transition);
}

.search-input:hover {
  border-color: var(--primary-color);
}

.search-input .el-input__inner {
  border-radius: var(--border-radius);
}

.search-input .el-input__append {
  border-radius: 0 var(--border-radius) var(--border-radius) 0;
}

/* 右侧操作区样式 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  justify-content: flex-end;
}

.avatar-wrapper {
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: var(--transition);
  padding: 4px;
  border-radius: 50%;
}

.avatar-wrapper:hover {
  background-color: var(--hover-color);
}

.avatar-wrapper .el-avatar {
  transition: var(--transition);
  border: 2px solid transparent;
}

.avatar-wrapper:hover .el-avatar {
  border-color: var(--primary-color);
  transform: scale(1.05);
}

.write-btn {
  min-width: 90px;
  border-radius: var(--border-radius);
  transition: var(--transition);
  font-weight: 500;
}

.write-btn:hover {
  color: var(--primary-color);
  border-color: var(--primary-color);
  background-color: var(--hover-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-light);
}

.main-container {
  position: relative;
  min-height: calc(100vh - 72px);
  overflow: hidden;
}

/* 侧边栏样式 */
.sidebar {
  position: absolute;
  left: 0;
  top: 0;
  width: 200px;
  min-height: calc(100vh - 72px);
  background-color: var(--card-background);
  box-shadow: var(--shadow-light);
  z-index: 5;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
}

/* 主内容包装器 */
.content-wrapper {
  margin-left: 200px;
  min-height: calc(100vh - 72px);
  transition: none;
}

.el-menu:not(.el-menu--collapse) {
  width: 200px;
}

/* 侧边菜单样式 */
.el-menu {
  border-right: none;
  background-color: var(--card-background);
  height: 100%;
}

.el-menu-item {
  height: 56px;
  line-height: 56px;
  margin: 8px 16px;
  border-radius: var(--border-radius);
  transition: var(--transition);
  font-weight: 500;
}

.el-menu-item:hover {
  background-color: var(--hover-color) !important;
  color: var(--primary-color) !important;
}

.el-menu-item.is-active {
  background-color: var(--hover-color) !important;
  color: var(--primary-color) !important;
}

.el-menu-item i {
  margin-right: 12px;
  font-size: 18px;
}

/* 主内容区域 */
.main-content {
  background-color: var(--background-color);
  color: var(--text-color);
  font-weight: 400;
  margin: 0;
  padding: 24px;
  min-height: 100%;
  box-sizing: border-box;
}

/* 标签容器样式 */
.tags-container {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
  width: 100%;
}

/* 标签页样式 */
.centered-tabs {
  background-color: var(--card-background);
  padding: 0 24px;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-light);
  overflow: hidden;
  width: 100%;
  max-width: 1200px;
}

.el-tabs__header {
  margin: 0;
  padding: 0;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: center;
}

.el-tabs__nav {
  display: flex;
  justify-content: center;
  width: 100%;
}

.el-tabs__item {
  padding: 0 20px;
  height: 56px;
  line-height: 56px;
  font-weight: 500;
  transition: var(--transition);
}

.el-tabs__item:hover {
  color: var(--primary-color);
}

.el-tabs__item.is-active {
  color: var(--primary-color);
  font-weight: 600;
}

.el-tabs__active-bar {
  height: 3px;
  background-color: var(--primary-color);
  border-radius: 3px;
}

/* 响应式调整 */
@media screen and (max-width: 1024px) {
  .search-area {
    min-width: 300px;
    margin: 0 16px;
  }
  
  .search-input {
    width: 250px;
  }
  
  .right-actions {
    gap: 12px;
  }
}

@media screen and (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 12px;
    padding: 12px;
    align-items: stretch;
  }
  
  .collapse-btn {
    align-self: flex-start;
    margin-right: 0;
  }
  
  .search-area {
    width: 100%;
    max-width: 100%;
    min-width: auto;
    margin: 0;
    order: 2;
  }
  
  .search-type-select {
    width: 80px;
  }
  
  .search-input {
    width: 100%;
  }
  
  .right-actions {
    width: 100%;
    justify-content: flex-end;
    order: 1;
  }
  
  .sidebar {
    width: 180px;
  }
  
  .sidebar.collapsed {
    width: 56px;
  }
  
  .content-wrapper {
    margin-left: 180px;
  }
  
  .main-content {
    padding: 16px;
  }
  
  .el-tabs {
    padding: 0 16px;
    margin-bottom: 16px;
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--background-color);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>