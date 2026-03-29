<template>
  <div class="layout-container">
    <el-header>
      <div class="header-content">
        <!-- 左侧空白占位，用于平衡居中效果 -->
        <div class="header-left-placeholder"></div>

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
// import NotificationBell from '@/components/NotificationBell.vue'
export default {
    // components: {
    //     NotificationBell
    // },
  data() {
    return {
      searchType: 'keyword',      // 搜索类型：keyword、tag 或 author
      searchKeyword: '',          // 搜索关键词
      activeTag: '全部',          // 当前选中的标签
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
  },
};
</script>

<style scoped>
.layout-container {
  background-color: #d4d4d4;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.el-header {
  padding: 0;
  background-color: #f6eeee;
  height: auto;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  min-height: 70px;
  box-sizing: border-box;
  position: relative;
}

/* 左侧空白占位，用于平衡右侧操作区的宽度，使搜索区域真正居中 */
.header-left-placeholder {
  width: 150px; /* 与右侧操作区大致宽度相同 */
  flex-shrink: 0;
}

/* 搜索区域样式 - 居中显示 */
.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 1 auto;
  max-width: 600px;
  min-width: 400px;
  margin: 0 auto; /* 水平居中 */
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.search-type-select {
  width: 100px;
}

.search-input {
  width: 350px;
}

/* 右侧操作区样式 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-left: auto;
  flex-shrink: 0;
  width: 150px; /* 固定宽度，用于平衡左侧占位 */
  justify-content: flex-end;
}

.avatar-wrapper {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.write-btn {
  min-width: 80px;
}

.main-content {
  background-color: #d4d4d4;
  color: #000000;
  font-weight: 500 !important;
  margin: 0;
  padding: 20px;
  min-height: calc(100vh - 70px);
  box-sizing: border-box;
}

.asid-content {
  background-color: #f6eeee;
  color: #000000;
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
  background-color: #f6eeee;
}

/* 标签页样式调整 */
.el-tabs {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 0 20px;
  border-radius: 4px;
}

/* 响应式调整 */
@media screen and (max-width: 1024px) {
  .search-area {
    min-width: 300px;
    position: static;
    transform: none;
    margin: 0 20px;
  }

  .search-input {
    width: 250px;
  }

  .header-left-placeholder {
    width: 120px;
  }

  .right-actions {
    width: 120px;
  }
}

@media screen and (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 10px;
    padding: 10px;
  }

  .header-left-placeholder {
    display: none; /* 移动端隐藏占位 */
  }

  .search-area {
    width: 100%;
    max-width: 100%;
    min-width: auto;
    margin: 0;
    paymentOrder: 2; /* 在移动端调整顺序 */
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
    margin-left: 0;
    paymentOrder: 1;
  }
}
</style>