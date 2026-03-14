<template>
    <div class="blog-detail-container">
      <!-- ========== 文章头部卡片 ========== -->
      <el-card class="blog-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="detailLoading">
        <!-- 博客标题 -->
        <h1 class="blog-title">{{ blog.title }}</h1>
  
        <!-- 作者信息及互动按钮区 -->
        <div class="blog-meta">
          <!-- 作者头像 -->
          <el-avatar :size="50" :src="blog.avatar"></el-avatar>
          <!-- 作者名称 -->
          <span class="author-name">{{ blog.author }}</span>
          <!-- 发布日期 -->
          <span class="publish-date">发布于 {{ blog.publishDate }}</span>
  
          <!-- 点赞 & 收藏按钮组 -->
          <div class="action-buttons">
            <!-- 点赞按钮：根据 isLiked 显示不同图标，点击触发 handleLike -->
            <el-button
              :type="blog.isLiked ? 'primary' : 'default'"
              size="small"
              :icon="blog.isLiked ? 'el-icon-sunny' : 'el-icon-sunrise'"
              @click="handleLike"
              circle
            ></el-button>
            <!-- 点赞数显示 -->
            <span class="like-count">{{ blog.likeCount }}</span>
  
            <!-- 收藏按钮：根据 isCollected 切换图标，点击触发 handleCollect -->
            <el-button
            :type="blog.isCollected ? 'warning' : 'default'"
            size="small"
            :icon="blog.isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"
            @click="handleCollect"
            circle
            ></el-button>
            <span class="collect-count">{{ blog.collectCount }}</span> 
          </div>
        </div>
      </el-card>
  
      <!-- ========== 文章正文卡片 ========== -->
      <el-card class="blog-content" :body-style="{ padding: '30px' }" shadow="never">
        <!-- 使用 v-html 渲染富文本内容（注意：如内容来自用户，需过滤 XSS） -->
        <div class="content-body" v-html="blog.content"></div>
      </el-card>
  
      <!-- ========== 评论区卡片 ========== -->
      <el-card class="comment-section" shadow="hover" v-loading="commentLoading">
        <!-- 卡片头部：显示评论总数 -->
        <div slot="header" class="comment-header">
          <span>评论（{{ comments.length }}）</span>
        </div>
  
        <!-- 评论列表 -->
        <div class="comment-list">
          <!-- 遍历 comments 数组，展示每条评论 -->
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <!-- 评论者头像 -->
            <el-avatar :size="40" :src="comment.avatar"></el-avatar>
            <div class="comment-content">
              <div class="comment-meta">
                <!-- 评论者昵称 -->
                <span class="comment-author">{{ comment.nickname }}</span>
                <!-- 评论时间 -->
                <span class="comment-time">{{ comment.time }}</span>
              </div>
              <!-- 评论正文 -->
              <div class="comment-text">{{ comment.content }}</div>
            </div>
          </div>
        </div>
  
        <!-- 发表评论输入区 -->
        <div class="comment-input-area">
          <!-- 绑定到 newComment -->
          <el-input
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            v-model="newComment"
            resize="none"
          ></el-input>
          <!-- 提交按钮：内容为空时禁用，点击触发 submitComment -->
          <div class="comment-submit">
            <el-button type="primary" @click="submitComment" :disabled="!newComment.trim()">
              发表评论
            </el-button>
          </div>
        </div>
      </el-card>
  
      <!-- ========== 回到顶部按钮 ========== -->
      <!-- target 指定监听滚动的容器，自定义按钮内容 -->
      <el-backtop target=".blog-detail-container" :bottom="100" :right="40">
        <div class="backtop-inner">
          <i class="el-icon-arrow-up"></i>
          <span>顶部</span>
        </div>
      </el-backtop>
    </div>
  </template>
  
  <script>
  export default {
    name: 'BlogDetail',
    data() {
      return {
        // ---------- 博客详情数据 ----------
        blog: {
          id: '',
          title: 'Vue 2 组合式 API 实践与思考',    // 博客标题
          author: 'Evan You',                     // 作者名
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', // 作者头像URL
          publishDate: '2025-03-11',               // 发布日期
          likeCount: 128,                           // 当前点赞总数
          collectCount: 24,                         // 当前收藏总数
          isLiked: false,                           // 当前登录用户是否已点赞
          isCollected: false,                       // 当前登录用户是否已收藏
          content: `                                 // 博客正文（HTML格式）
            <h2>为什么要使用组合式 API？</h2>
            <p>随着项目规模的增长，Vue 2 的选项式 API 可能导致代码关注点分散，而组合式 API 允许我们将逻辑按功能组织，提升可维护性。</p>
            <p>在 Vue 2 中，可以通过 <code>@vue/composition-api</code> 插件来使用组合式 API。下面是一个简单的例子：</p>
            <pre><code>
  import { ref, computed } from '@vue/composition-api';
  
  export default {
    setup() {
      const count = ref(0);
      const double = computed(() => count.value * 2);
      function increment() {
        count.value++;
      }
      return { count, double, increment };
    }
  };
            </code></pre>
            <p>这样可以将响应式状态、计算属性和方法集中在一个 <code>setup</code> 函数中，让组件更加清晰。</p>
            <p>当然，组合式 API 并不是必须的，它提供的是另一种代码组织方式，适用于大型组件或逻辑复用场景。</p>
            <p style="margin-top: 40px;">（以上内容仅为示例，实际博客请替换为真实文章）</p>
          `,
        },
        // ---------- 评论列表数据 ----------
        comments: [
          {
            id: 1,
            avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
            nickname: '前端小迷妹',
            time: '2025-03-11 10:30',
            content: '写的真好，受益匪浅！',
          },
          {
            id: 2,
            avatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
            nickname: '代码猎人',
            time: '2025-03-11 11:20',
            content: '组合式 API 确实让逻辑复用更方便了。',
          },
        ],
        // 新评论输入框绑定的内容
        newComment: '',
        // 加载状态变量
        detailLoading: false,
        commentLoading: false,
        likeLoading: false,
        collectLoading: false,
      };
    },
    mounted() {
      // 页面加载完成后获取博客详情
      this.fetchBlogDetail();
      // 同时获取评论
      //this.fetchComments();
    },
    methods: {
        /**
         * 获取博客详情
         * 接口：GET /api/blog/:id
         * 假设路由参数包含博客 ID，通过 this.$route.params.id 获取
         */
         async fetchBlogDetail() {
          const blogId = this.$route.params.id;
          if (!blogId) {
            console.warn('未获取到博客ID，使用模拟数据');
            return;
          }
          this.detailLoading = true;
          try {
            // 由于拦截器，这里直接得到博客数据，而不是完整的响应对象
            const blogData = await this.$axios.get(`/api/blog/${blogId}`); 
            
            console.log('博客数据:', blogData);
            
            if (!blogData) {
              console.error('博客数据为undefined或null');
              this.$message.error('获取博客详情失败：响应数据为空');
              return;
            }
            
            // 更新博客数据，根据实际API响应结构调整字段映射
            this.blog = {
              ...this.blog,
              id: blogData.id || blogId,
              title: blogData.title || this.blog.title,
              author: blogData.author ? (blogData.author.nickname || blogData.author.displayName || blogData.author.username) : this.blog.author,
              avatar: blogData.author ? blogData.author.avatar : this.blog.avatar,
              publishDate: blogData.createdAt ? new Date(blogData.createdAt).toLocaleDateString() : this.blog.publishDate,
              likeCount: blogData.likeCount !== undefined ? blogData.likeCount : this.blog.likeCount,
              isLiked: blogData.isLiked !== undefined ? blogData.isLiked : this.blog.isLiked,
              isCollected: blogData.isMarked !== undefined ? blogData.isMarked : this.blog.isCollected,
              collectCount: blogData.collectCount !== undefined ? blogData.collectCount : this.blog.collectCount,
              content: blogData.content || this.blog.content,
            };
            
            console.log('更新后的博客对象:', this.blog);
          } catch (error) {
            console.error('获取博客详情出错', error);
            console.error('错误对象:', error);
            
            // 由于拦截器，error.response 可能不可用
            if (error.response) {
              console.error('HTTP错误响应:', error.response);
              this.$message.error(`获取博客详情失败：${error.response.status} - ${error.response.statusText}`);
            } else if (error.request) {
              console.error('无响应:', error.request);
              this.$message.error('网络错误：无法连接到服务器');
            } else {
              console.error('请求错误:', error.message);
              this.$message.error('请求错误：' + error.message);
            }
          } finally {
            this.detailLoading = false;
          }
        }, 
        /**
         * 获取评论列表
         * 接口：GET /api/blog/:id/comments
         */
        async fetchComments() {
            const blogId = this.$route.params.id;
            if (!blogId) return;
            this.commentLoading = true;
            try {
                const res = await this.$axios.get(`/api/blog/${blogId}/comments`);
                if (res.data.code === 0) {
                // 假设返回的评论列表数组，每个评论包含 id, avatar, nickname, time, content
                this.comments = res.data.data.map(c => ({
                    id: c.id,
                    avatar: c.avatar,
                    nickname: c.nickname,
                    time: c.createTime, // 后端可能返回时间戳，需格式化
                    content: c.content,
                }));
                } else {
                this.$message.error('获取评论失败：' + res.data.message);
                }
            } catch (error) {
                console.error('获取评论出错', error);
                this.$message.error('网络错误，请稍后重试');
            } finally {
                this.commentLoading = false;
            }
        },
        /**
         * 点赞/取消点赞
         * 采用乐观更新：先更新界面，再发送请求，若失败则回滚
         */
        async handleLike() {
            // 保存旧状态，用于回滚
            const oldLiked = this.blog.isLiked;
            const oldCount = this.blog.likeCount;

            // 乐观更新
            this.blog.isLiked = !this.blog.isLiked;
            this.blog.likeCount += this.blog.isLiked ? 1 : -1;
            this.likeLoading = true; // 显示按钮加载状态

            const blogId = this.$route.params.id;
            try {
                // 发送请求，告诉后端当前操作（或后端自动切换）
                // 接口设计示例：POST /api/blog/:id/like 携带 { liked: this.blog.isLiked }
                await this.$axios.post(`/api/blog/${blogId}/like`, {
                liked: this.blog.isLiked, // 发送当前点赞状态
                });
                // 请求成功，无需额外操作
            } catch (error) {
                // 请求失败，回滚数据
                this.blog.isLiked = oldLiked;
                this.blog.likeCount = oldCount;
                this.$message.error('点赞操作失败，请重试');
                console.error('点赞失败', error);
            } finally {
                this.likeLoading = false;
            }
        },
  
        /**
         * 收藏/取消收藏
         * 同样乐观更新
         */
        async handleCollect() {
            const oldCollected = this.blog.isCollected;
            const oldCount = this.blog.collectCount;

            this.blog.isCollected = !this.blog.isCollected;
            this.blog.collectCount += this.blog.isCollected ? 1 : -1;
            this.collectLoading = true;

            const blogId = this.$route.params.id;
            try {
                await this.$axios.post(`/api/blog/${blogId}/collect`, {
                collected: this.blog.isCollected,
                });
                this.$message({
                message: this.blog.isCollected ? '已加入收藏' : '已取消收藏',
                type: 'success',
                duration: 1000,
                });
            } catch (error) {
                // 回滚
                this.blog.isCollected = oldCollected;
                this.blog.collectCount = oldCount;
                this.$message.error('收藏操作失败，请重试');
                console.error('收藏失败', error);
            } finally {
                this.collectLoading = false;
            }
        },
        /**
         * 发表评论
         */
        async submitComment() {
            if (!this.newComment.trim()) return;

            const blogId = this.$route.params.id;
            this.submitting = true;
            try {
                // 接口：POST /api/blog/:id/comments，请求体包含评论内容
                const res = await this.$axios.post(`/api/blog/${blogId}/comments`, {
                content: this.newComment,
                });
                if (res.data.code === 0) {
                // 假设后端返回新创建的评论对象
                const newComment = res.data.data;
                // 将新评论添加到列表顶部
                this.comments.unshift({
                    id: newComment.id,
                    avatar: newComment.avatar,   // 当前用户头像
                    nickname: newComment.nickname, // 当前用户昵称
                    time: newComment.createTime,  // 后端返回的时间
                    content: newComment.content,
                });
                this.newComment = '';
                this.$message.success('评论发表成功');
                } else {
                this.$message.error('发表失败：' + res.data.message);
                }
            } catch (error) {
                console.error('发表评论出错', error);
                this.$message.error('网络错误，请稍后重试');
            } finally {
                this.submitting = false;
            }
        },
    },
};
  </script>
  
  <style scoped>
  /* 整个博客详情容器，限制最大宽度，居中显示 */
  .blog-detail-container {
    max-width: 900px;
    margin: 20px auto;
    padding: 0 20px;
    min-height: 120vh; /* 设置足够高度以确保回到顶部按钮出现 */
    position: relative;
  }
  
  /* 头部卡片样式 */
  .blog-header {
    margin-bottom: 20px;
    border-radius: 8px;
  }
  .blog-title {
    margin: 0 0 16px 0;
    font-size: 2.2rem;
    color: #303133;
    line-height: 1.3;
  }
  .blog-meta {
    display: flex;
    align-items: center;
    gap: 15px;
    flex-wrap: wrap;
  }
  .author-name {
    font-size: 1.2rem;
    font-weight: 500;
    color: #409EFF;
  }
  .publish-date {
    color: #909399;
    font-size: 0.95rem;
  }
  /* 互动按钮组靠右对齐 */
  .action-buttons {
    display: flex;
    align-items: center;
    margin-left: auto;
    gap: 5px;
  }
  .like-count {
    margin-right: 15px;
    font-size: 14px;
    color: #606266;
  }

  .collect-count {
  margin-right: 0;        /* 可根据需要调整间距 */
  font-size: 14px;
  color: #606266;
  }
  .collect-text {
    font-size: 14px;
    color: #606266;
  }
  
  /* 正文卡片样式 */
  .blog-content {
    border-radius: 8px;
    background-color: #ffffff;
    margin-bottom: 20px;
  }
  .content-body {
    font-size: 1.1rem;
    line-height: 1.8;
    color: #2c3e50;
  }
  /* 正文内标题样式 */
  .content-body h2 {
    margin: 28px 0 16px;
    font-weight: 500;
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 8px;
  }
  .content-body p {
    margin: 16px 0;
  }
  /* 行内代码样式 */
  .content-body code {
    background-color: #f8f8f8;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    color: #e96900;
  }
  /* 代码块样式 */
  .content-body pre {
    background-color: #f8f8f8;
    padding: 16px;
    border-radius: 6px;
    overflow-x: auto;
    border: 1px solid #eaeefb;
  }
  .content-body pre code {
    background-color: transparent;
    padding: 0;
    color: #476582;
  }
  
  /* 评论区卡片样式 */
  .comment-section {
    border-radius: 8px;
    margin-bottom: 20px;
  }
  .comment-header {
    font-weight: 500;
    font-size: 1.1rem;
  }
  .comment-list {
    margin-bottom: 20px;
  }
  /* 单条评论项 */
  .comment-item {
    display: flex;
    gap: 15px;
    padding: 15px 0;
    border-bottom: 1px solid #f0f2f5;
  }
  .comment-item:last-child {
    border-bottom: none;
  }
  .comment-content {
    flex: 1;
  }
  .comment-meta {
    margin-bottom: 6px;
  }
  .comment-author {
    font-weight: 500;
    color: #409EFF;
    margin-right: 10px;
  }
  .comment-time {
    color: #909399;
    font-size: 0.85rem;
  }
  .comment-text {
    color: #2c3e50;
    line-height: 1.6;
  }
  /* 评论输入区 */
  .comment-input-area {
    margin-top: 10px;
  }
  .comment-submit {
    display: flex;
    justify-content: flex-end;
    margin-top: 10px;
  }
  
  /* 回到顶部按钮自定义内容 */
  .backtop-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    background-color: #409EFF;
    color: white;
    border-radius: 50%;
    font-size: 14px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    transition: background-color 0.3s;
  }
  .backtop-inner:hover {
    background-color: #66b1ff;
  }
  .backtop-inner i {
    font-size: 20px;
    margin-bottom: 2px;
  }
  </style>