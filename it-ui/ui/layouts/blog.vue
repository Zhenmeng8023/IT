<template>
  <div class="layout-container">
    <el-header>
      <div class="header-content">
        <!-- 搜索栏和写文章按钮以及头像 -->
        <el-input placeholder="请输入搜索内容" prefix-icon="el-icon-search" v-model="searchKeyword" class="search-input" width="300px"></el-input>
        <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
        <el-button type="info" @click="goToWrite" plain class="write-btn">写文章</el-button>
        <div class="avatar-wrapper">
            <el-avatar :size="50" :src="'/pic/choubi.jpg'"></el-avatar>
        </div>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="asid-content">
        <el-menu
        default-active="2"
        class="el-menu-vertical-demo"
        @open="handleOpen"
        @close="handleClose">
          <el-menu-item index="1">
              <i class="el-icon-menu"></i>
              <span slot="title">首页</span>
          </el-menu-item>
          <el-menu-item index="2">
              <i class="el-icon-menu"></i>
              <span slot="title">博客</span>
          </el-menu-item>
          <el-menu-item index="3">
              <i class="el-icon-menu"></i>
              <span slot="title">圈子</span>
          </el-menu-item>
          <el-menu-item index="4">
              <i class="el-icon-menu"></i>
              <span slot="title">个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <el-tabs v-model="activeName" @tab-click="handleClick">
          <!-- label=显示的文字 name=系统读到的 -->
            <el-tab-pane label="标签1" name="first"></el-tab-pane>
            <el-tab-pane label="标签2" name="second"></el-tab-pane>
            <el-tab-pane label="标签3" name="third"></el-tab-pane>
            <el-tab-pane label="标签4" name="fourth"></el-tab-pane>
        </el-tabs>
        <!-- 路由布局 -->
        <nuxt/>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
    data() {
      return {
        activeName: '',
      }
    },
    methods: {
      async handleClick(tab, event) {
        console.log(tab, event);
        
        try {
          // 根据标签名称向后端发起查询请求
          const response = await this.fetchBlogByTag(tab.name);
          console.log('获取的博客数据:', response.data);
          
          // 在这里处理返回的数据，更新页面内容
          // this.blogList = response.data;
          
        } catch (error) {
          console.error('获取数据失败:', error);
        }
      },
      
      // 预留的API接口函数
      async fetchBlogByTag(tagName) {
        // 构造API请求URL
        const apiUrl = `/api/blogs/tag/${tagName}`;
        
        // 发送GET请求到后端
        const response = await this.$axios.get(apiUrl);
        
        return response;
      }
    }

}
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

.header-content{
    background-color: #f6eeee;
    color: #000000;
    font-weight: 500 !important;
    margin: 10px 0;
    padding: 0;
    width: 100%;
    min-height: 40px;
    box-sizing: border-box;
}

.search-input{
    margin: 0 10px;
    padding: 0;
    width: 300px;
    min-height: 60px;
    box-sizing: border-box;
}

.search-btn{
    margin: 0 auto;
    padding: 0;
    width: 100px;
    min-height: 30px;
    box-sizing: border-box;
}

.write-btn{
    margin: 0 auto;
    padding: 0;
    width: 100px;
    min-height: 30px;
    box-sizing: border-box;
}

.avatar-wrapper{
    margin: 0 auto;
    padding: 0;
    width: 100px;
    min-height: 60px;
    box-sizing: border-box;
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

.asid-content{
    background-color: #f6eeee;
    color: #000000;
    font-weight: 500 !important;
    margin: 0;
    padding: 0;
    width: 100%;
    min-height: calc(100vh - 60px);
    box-sizing: border-box;
}

</style>