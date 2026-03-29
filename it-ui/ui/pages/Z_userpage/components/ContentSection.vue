<!-- 最新文章和关于网站 -->
<template>
    <div class="content-section">
      <!-- 个性化定制开始 -->
      <div class="block">
        <span class="demonstration">个性化定制</span>
        <el-carousel height="200px">
          <el-carousel-item v-for="(item, index) in carouselItems" :key="index">
            <div class="carousel-content">
              <img :src="item.image" :alt="item.title" class="carousel-image">
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
      <!-- 个性化定制结束 -->
      <div class="latest-articles">
        <!-- <h2>最近发布</h2>
        <article  class="article-item">
          <h3>{{ article.title }}</h3>
          <p class="meta">{{ article.meta }}</p>
        </article> -->
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
        carouselItems: [
        {
          image: '/pic/choubi.jpg' // 技术博客图片
        },
        {
          image: '/pic/duck.jpg' // 编程项目图片
        },
        {
          image: '/pic/hotpic.jpg' // 学习资源图片
        }
      ]
      }
    },
    methods: {
      handleWriteClick() {
        // 处理写文章按钮点击事件
        this.$router.push('/blogwrite');
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
  .carousel-content {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    width: 100%;
  }
  
  .carousel-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  </style>