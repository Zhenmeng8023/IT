<!-- 推荐链接 -->
<template>
    <div class="recommend-links">
      <div class="icon-links">
        <!-- <a href="#" class="icon-link">稀土掘金</a>
        <a href="#" class="icon-link">Github</a> -->
      </div>
      <div class="random-recommend">
        <h3>随机推荐</h3>
        <a href="#" class="recommend-card">
          <h4>iLoveIMG</h4>
          <p>免费图片处理网站，我经常用来压缩图片，压缩效果会比 Tinypng 更好。链式处理功能非常好用。</p>
        </a>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'RecommendLinks'
  }
  </script>
  
  <style scoped>
  .recommend-links {
    margin-top: 30px;
    color: #ffffff;
  }
  .icon-links {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
  }
  .icon-link {
    padding: 8px 16px;
    background: #333;
    border-radius: 20px;
    text-decoration: none;
    color: #fff;
    font-weight: 500;
  }
  .random-recommend h3 {
    font-size: 1.2rem;
    margin-bottom: 10px;
  }
  .recommend-card {
    display: block;
    padding: 15px;
    background: #1a1a1a;
    border: 1px solid #333;
    border-radius: 8px;
    text-decoration: none;
    color: #fff;
    transition: box-shadow 0.2s;
  }
  .recommend-card:hover {
    box-shadow: 0 4px 12px rgba(255,255,255,0.1);
  }
  .recommend-card h4 {
    margin: 0 0 5px 0;
    font-size: 1.1rem;
    color: #fff;
  }
  .recommend-card p {
    margin: 0;
    font-size: 0.9rem;
    color: #b0b0b0;
  }
  </style>