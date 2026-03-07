<!-- 最新文章和关于网站 -->
<template>
    <div class="content-section">
      <div class="latest-articles">
        <h2>最新文章</h2>
        <article v-for="n in 3" :key="n" class="article-item">
          <h3>Blender场景建模笔记</h3>
          <p class="meta">blender 笔记 · 2026/3/4</p>
        </article>
        <button class="write-btn">写文章</button>
      </div>
      
      <aside class="site-info">
        <h2>YYsuni (开发中)</h2>
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
      </aside>
    </div>
  </template>
  
  <script>
  export default {
    name: 'ContentSection'
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
  </style>