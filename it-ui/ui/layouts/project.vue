<template>
  <div class="project-layout">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-content">
        <!-- 左侧Logo和标题 -->
        <div class="header-left">
          <div class="logo" @click="$router.push('/')">
            <i class="el-icon-s-promotion"></i>
            <span class="logo-text">项目中心</span>
          </div>
        </div>

        <!-- 中间搜索区域 -->
        <div class="header-center">
          <div class="search-container">
            <!-- 搜索类型选择 -->
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
            
            <!-- 搜索输入框 -->
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

        <!-- 右侧用户操作区域 -->
        <div class="header-right">
          <!-- 发布项目按钮 -->
          <el-button 
            type="primary" 
            size="small" 
            icon="el-icon-plus"
            @click="goToCreateProject"
            class="publish-btn"
          >
            发布项目
          </el-button>
          
          <!-- 用户信息 -->
          <div class="user-info" @click="goToUserProfile">
            <el-avatar 
              :size="32" 
              :src="userAvatar" 
              class="user-avatar"
            ></el-avatar>
            <span class="username">{{ username }}</span>
            <i class="el-icon-arrow-down"></i>
          </div>
        </div>
      </div>
    </el-header>

    <!-- 主体内容区域 -->
    <el-container class="main-container">
      <!-- 侧边栏 -->
      <el-aside width="220px" class="sidebar">
        <el-menu
          :default-active="$route.path"
          class="project-menu"
          background-color="#f8fafc"
          text-color="#000000"
          active-text-color="#3b82f6"
          router
        >
          <!-- 系统链接 -->
            
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
        
           <!-- 项目相关菜单 -->
          <el-submenu index="projects">
            <template #title>
              <i class="el-icon-s-management"></i>
              <span>项目</span>
            </template>
            <el-menu-item index="/projectlist">
              <i class="el-icon-document"></i>
              <span>项目列表</span>
            </el-menu-item>
            <el-menu-item index="/myproject">
              <i class="el-icon-folder-opened"></i>
              <span>我的项目</span>
            </el-menu-item>
            <el-menu-item index="/projectcollection">
              <i class="el-icon-star-on"></i>
              <span>收藏项目</span>
            </el-menu-item>
          </el-submenu>
          <el-menu-item index="/user">
            <i class="el-icon-user"></i>
            <span>个人中心</span>
          </el-menu-item>

        </el-menu>
      </el-aside>

      <!-- 主内容区域 -->
      <el-main class="content-area">
        <!-- 路由页面内容 -->
        <nuxt />
      </el-main>
    </el-container>
  </div>
</template>

<script>
// 预留项目相关的API接口
import { 
  SearchProjects, 
  SearchProjectsByTech, 
  SearchProjectsByAuthor,
  GetCurrentUser,
  GetProjectCategories,
  GetPopularTechnologies
} from '@/api/index'

export default {
  name: 'ProjectLayout',
  data() {
    return {
      // 搜索相关
      searchType: 'keyword',
      searchKeyword: '',
      searching: false,
      
      // 用户信息
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      username: '用户',
      
      // 项目分类和技术栈数据
      categories: [],
      technologies: [],
      
      // 菜单项配置
      menuItems: [
        { index: '/project', icon: 'el-icon-s-home', title: '项目首页' },
        { index: '/project/list', icon: 'el-icon-s-management', title: '项目列表' },
        { index: '/project/my', icon: 'el-icon-folder-opened', title: '我的项目' },
        { index: '/project/starred', icon: 'el-icon-star-on', title: '收藏项目' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' },
        { index: '/collection', icon: 'el-icon-s-collection', title: '收藏项目' },
        { index: '/project/detail', icon: 'el-icon-s-management', title: '项目详情' },
      ]
    };
  },
  computed: {
    // 判断当前路由是否为特殊页面
    isSpecialPage() {
      const path = this.$route.path;
      return path.startsWith('/project/detail') || path.startsWith('/project/create');
    }
  },
  mounted() {
    // 初始化数据
    this.initData();
  },
  methods: {
    // 初始化数据
    async initData() {
      try {
        // 预留：获取用户信息
        // const userData = await GetCurrentUser();
        // this.userAvatar = userData.avatar;
        // this.username = userData.nickname;
        
        // 预留：获取项目分类
        // this.categories = await GetProjectCategories();
        
        // 预留：获取热门技术栈
        // this.technologies = await GetPopularTechnologies();
        
      } catch (error) {
        console.error('初始化项目布局数据失败:', error);
      }
    },

    // 处理搜索类型变化
    handleSearchTypeChange() {
      this.searchKeyword = '';
    },

    // 处理搜索
    async handleSearch() {
      if (!this.searchKeyword.trim()) {
        this.$message.warning('请输入搜索关键词');
        return;
      }

      this.searching = true;
      try {
        let searchResult;
        
        // 根据搜索类型调用不同的API
        switch (this.searchType) {
          case 'keyword':
            // searchResult = await SearchProjects({ keyword: this.searchKeyword });
            break;
          case 'tech':
            // searchResult = await SearchProjectsByTech({ tech: this.searchKeyword });
            break;
          case 'author':
            // searchResult = await SearchProjectsByAuthor({ author: this.searchKeyword });
            break;
        }
        
        // 预留：处理搜索结果，跳转到搜索结果页面或显示结果
        this.$message.success(`搜索到相关项目`);
        
        // 清空搜索框
        this.searchKeyword = '';
        
      } catch (error) {
        console.error('搜索失败:', error);
        this.$message.error('搜索失败，请稍后重试');
      } finally {
        this.searching = false;
      }
    },

    // 跳转到创建项目页面
    goToCreateProject() {
      this.$router.push('/project/create');
    },

    // 跳转到用户个人资料
    goToUserProfile() {
      this.$router.push('/user/profile');
    },

    // 处理标签点击（预留功能）
    handleTagClick(tag) {
      this.$router.push(`/project/tag/${tag}`);
    }
  }
};
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
</style>