<template>
  <el-container class="app">
    <el-header class="header">
      <div class="header-content">
        <span class="header-title">博客管理系统</span>
        <div class="header-right">
          <el-dropdown>
            <span class="el-dropdown-link">
              管理员<i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item>个人中心</el-dropdown-item>
              <el-dropdown-item>修改密码</el-dropdown-item>
              <el-dropdown-item divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边栏导航 start-->
      <el-aside width="220px">
        <el-menu
          :default-active="$route.path"
          class="el-menu-vertical-demo"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          unique-opened>
          
          <!-- 首页 -->
          <el-menu-item index="/manage">
            <i class="el-icon-s-home"></i>
            <span slot="title">首页</span>
          </el-menu-item>
          
          <!-- 系统管理 -->
          <el-submenu index="system">
            <template slot="title">
              <i class="el-icon-s-tools"></i>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/usermanage">用户管理</el-menu-item>
            <el-menu-item index="/menu">菜单管理</el-menu-item>
            <el-menu-item index="/log">日志管理</el-menu-item>
            <el-menu-item index="/label">标签管理</el-menu-item>
          </el-submenu>
          
          <!-- 博客管理 -->
          <el-submenu index="blog">
            <template slot="title">
              <i class="el-icon-notebook-2"></i>
              <span>博客管理</span>
            </template>
            <el-menu-item index="/audit">审核</el-menu-item>
            <el-menu-item index="/dashboard">仪表盘</el-menu-item>
            <el-menu-item index="/algoreco">推荐算法</el-menu-item>
          </el-submenu>
          
          <!-- 项目管理 -->
          <el-submenu index="project">
            <template slot="title">
              <i class="el-icon-s-management"></i>
              <span>项目管理</span>
            </template>
            <el-menu-item index="/projectaudit">项目审核</el-menu-item>
            <el-menu-item index="/projectmiss">项目下架</el-menu-item>
            <el-menu-item index="/projectalgoreco">推荐算法</el-menu-item>
          </el-submenu>
          
          <!-- 圈子管理 -->
          <el-submenu index="circle">
            <template slot="title">
              <i class="el-icon-s-promotion"></i>
              <span>圈子管理</span>
            </template>
            <el-menu-item index="/circlefriend">好友</el-menu-item>
            <el-menu-item index="/circleaudit">圈子审核</el-menu-item>
            <el-menu-item index="/circlesort">圈子分类</el-menu-item>
            <el-menu-item index="/circlemanage">圈子管理</el-menu-item>
            <el-menu-item index="/circleofficial">官方圈子详细管理</el-menu-item>
          </el-submenu>
        </el-menu>
      </el-aside>
      <!-- 侧边栏导航 end -->
      
      <el-container>
        <el-main class="main-content">
          <!-- 路由视图 -->
          <nuxt/>
        </el-main>
        
        <el-footer class="footer">
          © 2026 博客管理系统 - 技术支持
        </el-footer>
      </el-container>
    </el-container>
  </el-container>
</template>

<script>
export default {
  data() {
    return {
      activeIndex: '/manage'
    }
  },
  mounted() {
    // 设置默认激活菜单
    this.activeIndex = this.$route.path || '/manage'
  },
  watch: {
    '$route.path': function(newPath) {
      this.activeIndex = newPath
    }
  }
}
</script>

<style>
html, body, #__nuxt, #__layout, .app {
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 头部样式 */
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.header-title {
  font-size: 20px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
}

/* 侧边栏样式 */
.el-aside {
  background-color: #304156;
  height: calc(100vh - 60px);
}

.el-menu {
  border: none;
}

.el-menu-vertical-demo {
  height: 100%;
}

/* 主内容区域样式 */
.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

/* 底部样式 */
.footer {
  background-color: #f5f7fa;
  color: #909399;
  text-align: center;
  line-height: 40px;
  border-top: 1px solid #e4e7ed;
}

/* 菜单项悬停效果 */
.el-menu-item:hover {
  background-color: #263445 !important;
}

.el-menu-item.is-active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: #fff !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .el-aside {
    width: 180px !important;
  }
  
  .header-title {
    font-size: 16px;
  }
}
</style>