<template>
  <div class="layout-container">
    <!-- 导航栏开始 -->
    <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
      <el-menu-item index="1">首页</el-menu-item>
      <el-menu-item index="2">博客</el-menu-item>
      <!-- <el-menu-item index="3">项目</el-menu-item> -->
      <el-menu-item index="4">圈子</el-menu-item>
      <el-menu-item index="5">个人主页</el-menu-item>
    </el-menu>
    <!-- 导航栏结束 -->
    
    <!-- 主要内容区域 -->
    <main class="main-content">
      <nuxt/>
    </main>
    
    <!-- 返回顶部按钮 -->
    <el-backtop target=".main-content" :visibility-height="1"></el-backtop>
  </div>
</template>

<script>
export default {
    data() {
      return {
        activeIndex: '1',
        activeIndex2: '1'
      };
    },
    methods: {
      handleSelect(key, keyPath) {
        console.log(key, keyPath);
      }
    }
  }
</script>

<style scoped>
/* 导航栏样式 */
.el-menu-demo {
  background-color: #1a1a1a !important;
  border-bottom: 1px solid #333 !important;
}

.el-menu-demo .el-menu-item {
  color: #e0e0e0 !important;
  background-color: transparent !important;
  border-bottom: 3px solid transparent !important;
  transition: all 0.3s ease !important;
}

/* 回到顶部按钮样式 */
.el-backtop {
  background-color: #333 !important;
  color: #ffffff !important;
  border: 1px solid #555 !important;
  border-radius: 50% !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
  transition: all 0.3s ease !important;
}

.el-backtop:hover {
  background-color: #555 !important;
  border-color: #777 !important;
  transform: scale(1.1) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4) !important;
}

/* 回到顶部按钮图标样式 */
.el-backtop i {
  color: #ffffff !important;
}

/* 布局容器样式 */
.layout-container {
  background-color: #000000;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.main-content {
  background-color: #000000;
  color: #ffffff;
  font-weight: 500 !important;
  margin: 0;
  padding: 0;
  width: 100%;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}
.el-menu-demo .el-menu-item:hover {
  color: #ffffff !important;
  background-color: #2a2a2a !important;
  border-bottom-color: #555 !important;
}

.el-menu-demo .el-menu-item.is-active {
  color: #ffffff !important;
  background-color: #2a2a2a !important;
  border-bottom-color: #3498db !important;
}

.el-menu--horizontal .el-menu-item:not(.is-disabled):hover {
  border-bottom: 3px solid transparent !important;
}

.el-menu--horizontal {
  border: none !important;
}

@media (max-width: 768px) {
  .el-menu-demo {
    flex-direction: column !important;
  }
  .el-menu-demo .el-menu-item {
    width: 100% !important;
    text-align: center !important;
    border-bottom: 1px solid #333 !important;
    border-right: none !important;
  }
}
</style>