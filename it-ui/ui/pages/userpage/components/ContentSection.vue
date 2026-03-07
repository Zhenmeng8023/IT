<!-- 最新文章和关于网站 -->
<template>
    <div class="content-section">
      <!-- 个性化定制开始 -->
      <div class="block">
        <span class="demonstration">个性化定制</span>
        <el-carousel height="150px">
          <el-carousel-item v-for="item in 4" :key="item">
            <h3 class="small">{{ item }}</h3>
          </el-carousel-item>
        </el-carousel>
      </div>
      <!-- 个性化定制结束 -->
      <div class="latest-articles">
        <h2>最近发布</h2>
        <article  class="article-item">
          <h3>{{ article.title }}</h3>
          <p class="meta">{{ article.meta }}</p>
        </article>
        <button class="write-btn" @click="handleWriteClick">写文章</button>
      </div>
      
      <!-- <aside class="site-info">
        <h2></h2>
        <nav>
          <ul>
            <li><a href="#">General</a></li>
            <li><a href="#">近期文章</a></li>
            <li><a href="#">我的项目</a></li>
            <li><a href="#">关于网站</a></li>
            <li><a href="#">推荐分享</a></li>
            <li><a href="#">优秀博客</a></li>
          </ul>
        </nav>
      </aside> -->
    </div>
  </template>
  
  <script>
  export default {
    name: 'ContentSection',
    data() {
      return {
        article: {
            title: 'Blender场景建模笔记',
            meta: 'blender 笔记 · 2026/3/4'
          },
      }
    },
    methods: {
      handleWriteClick() {
        // 处理写文章按钮点击事件
        console.log('写文章按钮被点击了');
      }
    }
  }
  </script>
  
  <style scoped>
  .content-section {
    display: flex;
    flex-direction: column;
    gap: 30px;
    color: #ffffff;
  }
  .latest-articles h2, .site-info h2 {
    font-size: 1.5rem;
    margin-bottom: 20px;
  }
  .article-item {
    margin-bottom: 20px;
  }
  .article-item h3 {
    margin: 0 0 5px 0;
    font-size: 1.2rem;
    font-weight: 500;
  }
  .meta {
    color: #b0b0b0;
    font-size: 0.9rem;
    margin: 0;
  }
  .write-btn {
    background: #333;
    border: 1px solid #555;
    padding: 8px 20px;
    border-radius: 20px;
    cursor: pointer;
    margin-top: 10px;
    color: #fff;
  }
  .site-info ul {
    list-style: none;
    padding: 0;
  }
  .site-info li {
    margin-bottom: 10px;
  }
  .site-info a {
    text-decoration: none;
    color: #b0b0b0;
  }
  .site-info a:hover {
    color: #fff;
  }
  /* 个性化定制样式 */
  .el-carousel__item h3 {
    color: #85e511;
    font-size: 14px;
    opacity: 0.75;
    line-height: 150px;
    margin: 0;
  }
  .el-carousel__item:nth-child(2n) {
     background-color: #0e6aeb;
  }
  
  .el-carousel__item:nth-child(2n+1) {
     background-color: #e51818;
  }
  </style>