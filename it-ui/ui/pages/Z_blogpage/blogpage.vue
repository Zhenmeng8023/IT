<template>
    <div>
      <el-container class="container">
          <el-header>
            <!-- 搜索栏+头像 -->
              <div class="header-content">
                  <el-input placeholder="请输入搜索内容" prefix-icon="el-icon-search" v-model="searchtext" class="search-input"></el-input>
                  <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
                  <div class="avatar-wrapper">
                      <el-avatar :size="50" :src="'/pic/choubi.jpg'"></el-avatar>
                  </div>
              </div>
          </el-header>
          <el-main>
        <!-- 博客卡片 1 -->
        <a href="/Z_blogdetail/blogdetail">
            <el-card class="blog-card" shadow="never">
            <template slot="header">
                <span class="blog-title">博客标题1</span>
                <p>博客1作者</p>
                <p>博客1内容 preview...</p>
            </template>
            </el-card>
        </a>
        <!-- 分割线（卡片1与卡片2之间） -->
        <el-divider></el-divider>
        <a href="/Z_blogdetail/blogdetail">
            <el-card class="blog-card" shadow="never">
            <template slot="header">
                <span class="blog-title">博客标题2</span>
                <p>博客2作者</p>
                <p>博客2内容 preview...</p>
            </template>
            </el-card>
        </a>
        </el-main>
      </el-container>
    </div>
  </template>
  
<script>
  export default {
    layout:'default',
      data() {
          return {
              searchtext: ''
          }
      },
      methods: {
        handleSearch() {
            console.log(this.searchtext)
        }
      }
  }
</script>
  
<style>
  .container {
    margin: 0;
    padding: 0;
    min-height: 100vh;
    overflow: hidden;          /* 将overflow: hidden限制在当前组件内部 */
  }

  .container {
    height: 100vh;
    width: 100vw;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
  }

  .header-content {
    display: flex;
    align-items: center;
    height: 100%;
    background-color: #E9EEF3;
    color: #ffffff;
  }
  
  .search-input {
      width: 40%;
      margin-left: auto;
      margin-right: 10px;
  }
  
  .search-btn {
      margin-right: 20px;
  }
  
  .avatar-wrapper {
      margin-left: auto;
  }
  
  body {
      background-color: white !important;
  }
  
  .el-main {
  background-color: #E9EEF3;
  color: #333;
  padding: 0 20px;
  overflow-y: auto;
  }

  .blog-title {
    font-size: 25px;
    font-weight: bold;
  }
  
  .blog-card {
    margin-bottom: 20px;
  }
  .el-header {
    background-color: #E9EEF3;
  }
</style>