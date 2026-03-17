<template>
  <div class="circle-container">
    <!-- 帖子列表，使用 Element UI 的无限滚动指令 -->
    <div
      class="post-list"
      v-infinite-scroll="loadMore"
      infinite-scroll-disabled="scrollDisabled"
      infinite-scroll-distance="10"
      v-loading="loading"
      element-loading-text="加载中..."
    >
    <el-card
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        shadow="hover"
        @click.native="goToPostDetail(post.id)"
      >
        <!-- 圈子名称 -->
        <div class="circle-name">
          <i class="el-icon-chat-dot-round"></i> {{ getCircleNameById(post.circleId) || '未知圈子' }}
        </div>
        
        <!-- 作者信息 -->
        <div class="post-author">
          <el-avatar :size="24" :src="post.authorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"></el-avatar>
          <span class="author-name">{{ post.author || '未知用户' }}</span>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>
        
        <!-- 帖子内容摘要 -->
        <div class="post-content">
          {{ post.content }}
        </div>
        
        <!-- 帖子底部信息：评论数、点赞数 -->
        <div class="post-footer">
          <span class="post-stat">
            <i class="el-icon-chat-line-round"></i> {{ post.replyCount || 0 }}
          </span>
          <span class="post-stat">
            <i class="el-icon-star-off"></i> {{ post.likes || 0 }}
          </span>
        </div>
      </el-card>

      <!-- 没有更多数据时的提示 -->
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        没有更多了
      </div>
      
      <!-- 没有帖子时的提示 -->
      <div v-if="!loading && posts.length === 0" class="no-posts">
        暂无帖子，快去发帖吧！
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'circle', // 使用圈子布局（包含头部搜索框、侧边栏等）
  
  data() {
    return {
      page: 1,             // 当前页码（从1开始）
      pageSize: 10,        // 每页加载数量
      hasMore: true,       // 是否还有更多数据
      loading: false,      // 是否正在加载中
      posts: [],           // 帖子列表
      allPosts: [],        // 所有帖子数据，用于分页
      circles: [],         // 圈子列表
      currentCircleId: null, // 当前显示的圈子ID
    };

    authorname:'未知用户';
  },
  
  computed: {
    // 从路由 query 中获取搜索关键词（由头部搜索框联动）
    keyword() {
      return this.$route.query.keyword || '';
    },
    // 从路由 query 中获取圈子ID
    circleIdFromRoute() {
      return this.$route.query.circleId || null;
    },
    // 无限滚动是否应禁用：正在加载 或 没有更多数据
    scrollDisabled() {
      return this.loading || !this.hasMore;
    },
    // 根据搜索关键词过滤后的帖子
    filteredPosts() {
      let filtered = this.allPosts;
      
      // 关键词搜索（搜索标题、作者和内容）
      if (this.keyword) {
        const keywordLower = this.keyword.toLowerCase();
        filtered = filtered.filter(post => 
          (post.title && post.title.toLowerCase().includes(keywordLower)) || 
          (post.author && post.author.toLowerCase().includes(keywordLower)) ||
          (post.summary && post.summary.toLowerCase().includes(keywordLower)) ||
          (post.content && post.content.toLowerCase().includes(keywordLower))
        );
      }
      
      return filtered;
    },
  },
  
  watch: {
    // 监听路由参数变化（关键词和圈子ID）
    '$route.query': {
      handler() {
        this.resetAndFetch();
      },
      deep: true,
      immediate: true,
    },
  },
  
  async created() {
    // 初始化时加载所有圈子信息
    await this.loadAllCircles();
    // 初始化时直接加载第一页（从过滤后的数据中取）
    this.fetchPosts();
  },
  
  methods: {
    // 根据圈子ID获取圈子名称
    getCircleNameById(circleId) {
      if (!circleId || !this.circles || this.circles.length === 0) {
        return '未知圈子';
      }
      const circle = this.circles.find(c => c.id == circleId);
      return circle ? circle.name : '未知圈子';
    },

     // 通过作者ID获取作者信息（包括nickname）
     async getAuthorNameById(authorId) {
      if (!authorId) {
        return { nickname: '未知用户', avatarUrl: null };
      }
      
      try {
        // 假设后端有提供根据ID获取用户信息的API
        const response = await this.$axios.get(`/api/users/${authorId}`);
        if (response && response.data) {
          // 根据实际API返回的数据结构调整
          return {
            nickname: response.data.nickname || response.data.username || '匿名用户',
            avatarUrl: response.data.avatarUrl || response.data.avatar || null
          };
        } else {
          return { nickname: '未知用户', avatarUrl: null };
        }
      } catch (error) {
        console.error(`获取作者信息失败，ID: ${authorId}`, error);
        return { nickname: '未知用户', avatarUrl: null };
      }
    },

    //获取帖子所属圈子id
    // async getCircleIdByPostId(postId) {
    //   if (!postId) {
    //     return null;
    //   }
      
    //   try {
    //     // 假设后端有提供根据ID获取帖子信息的API
    //     const response = await this.$axios.get(`/api/post/${postId}`);
    //     if (response && response.data) {
    //       // 根据实际API返回的数据结构调整
    //       return response.data.circle_id || null;
    //     } else {
    //       return null;
    //     }
    //   } catch (error) {
    //     console.error(`获取帖子所属圈子ID失败，ID: ${postId}`, error);
    //     return null;
    //   }
    // },


    // 获取所有圈子信息
    async loadAllCircles() {
      try {
        const response = await this.$axios.get('/api/circle');
        console.log('圈子API响应:', response);
        
        // 检查响应数据
        if (response && Array.isArray(response)) {
          // 如果响应本身就是数组
          this.circles = response;
        } else if (response && response.data) {
          // 如果响应包含data属性
          if (Array.isArray(response.data)) {
            this.circles = response.data;
          } else if (response.data.data && Array.isArray(response.data.data)) {
            this.circles = response.data.data;
          } else if (Array.isArray(response.data.rows)) {
            this.circles = response.data.rows;
          } else {
            console.error('未知的圈子API响应格式:', response.data);
            this.$message.error('获取圈子列表失败：数据格式错误');
            return;
          }
        } else {
          console.error('圈子API响应为空', response);
          this.$message.error('获取圈子列表失败');
          return;
        }
        
        console.log('解析出的圈子数据:', this.circles);
        
        // 如果有圈子数据，设置默认当前圈子为第一个
        if (this.circles.length > 0 && !this.currentCircleId) {
          this.currentCircleId = this.circles[0].id;
        }
      } catch (error) {
        console.error('获取圈子列表出错', error);
        this.$message.error('获取圈子列表失败');
      }
    },

    // 根据圈子ID获取圈子名称
    getCircleNameById(circleId) {
      if (!circleId || !this.circles || this.circles.length === 0) {
        return '未知圈子';
      }
      const circle = this.circles.find(c => c.id == circleId);
      return circle ? circle.name : '未知圈子';
    },

    // 跳转到帖子详情页
    goToPostDetail(id) {
      console.log('跳转圈子详情页，ID:', id);
      this.$router.push(`/circle/${id}`);
    },
    
    // 格式化时间
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const now = new Date();
      const diff = Math.floor((now - date) / 1000); // 秒数差
      
      if (diff < 60) {
        return '刚刚';
      } else if (diff < 3600) {
        return Math.floor(diff / 60) + '分钟前';
      } else if (diff < 86400) {
        return Math.floor(diff / 3600) + '小时前';
      } else if (diff < 2592000) {
        return Math.floor(diff / 86400) + '天前';
      } else {
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        const day = date.getDate().toString().padStart(2, '0');
        return `${year}-${month}-${day}`;
      }
    },
    
 // 获取帖子列表
 async fetchPosts() {
      this.loading = true;
      try {
        // 优先使用路由中的圈子ID，否则使用第一个圈子ID
        const targetCircleId = this.circleIdFromRoute || this.currentCircleId || 1;
        console.log('获取帖子列表，圈子ID:', targetCircleId);
        
        // 根据API文档，使用正确的接口
        const response = await this.$axios.get(`/api/circle/${targetCircleId}/posts`);
        console.log('帖子API响应成功:', response);
        
        // 检查响应数据
        let postsData = [];
        
        if (response && Array.isArray(response)) {
          // 如果响应本身就是数组
          postsData = response;
        } else if (response && response.data) {
          // 如果响应包含data属性
          if (Array.isArray(response.data)) {
            postsData = response.data;
          } else if (response.data.data && Array.isArray(response.data.data)) {
            postsData = response.data.data;
          } else if (Array.isArray(response.data.rows)) {
            postsData = response.data.rows;
          } else if (response.data.list && Array.isArray(response.data.list)) {
            postsData = response.data.list;
          } else if (response.data.items && Array.isArray(response.data.items)) {
            postsData = response.data.items;
          } else {
            console.error('未知的帖子API响应格式:', response.data);
            this.$message.error('获取帖子列表失败：数据格式错误');
            this.useMockData();
            return;
          }
        } else {
          console.error('帖子API响应为空', response);
          this.$message.error('获取帖子列表失败');
          this.useMockData();
          return;
        }
        
        console.log('原始帖子数据:', postsData);
        
        // 转换数据格式以匹配前端期望
        let convertedPosts = postsData.map(post => {
          // 根据API文档中的字段映射
          const normalizedPost = {
            id: post.id || post.postId || post.commentId, // 根据API，可能是id或commentId
            circleId: post.circleId || targetCircleId, // 如果接口没有返回圈子ID，使用请求的圈子ID
            title: post.title || post.subject || '无标题',
            content: post.content || post.body || '',
            summary: post.summary || post.content?.substring(0, 100) + (post.content?.length > 100 ? '...' : '') || '',
            authorId: post.authorId || post.userId || post.creatorId, // 添加作者ID
            author: this.parseAuthorInfo(post.author) || post.userName || post.creator || post.user?.username || '匿名用户',
            authorAvatar: post.avatar || post.userAvatar || post.user?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createTime: post.createTime || post.createdAt || post.createDate || new Date().toISOString(),
            likeCount: post.likeCount || post.likes || 0,
            commentCount: post.commentCount || post.replyCount || 0,
          };
          
          console.log('转换后的帖子:', normalizedPost);
          return normalizedPost;
        });

        // 如果帖子中有authorId但没有author信息，通过authorId获取作者名称
        console.log('检查是否需要获取作者信息...');
        const postsWithMissingAuthor = convertedPosts.filter(post => post.authorId && !post.author);
        console.log('缺少作者信息的帖子数量:', postsWithMissingAuthor.length);
        
        if (postsWithMissingAuthor.length > 0) {
          // 收集所有作者ID
          const authorIds = postsWithMissingAuthor
            .map(post => post.authorId);
          
          console.log('作者信息:', authorIds);
          
          if (authorIds.length > 0) {
            // 获取作者信息并更新帖子数据
            const authorsMap = await this.getAuthorsByIds(authorIds);
            
            convertedPosts = convertedPosts.map(post => {
              if (post.authorId && (!post.author || post.author === '匿名用户')) {
                // 使用nickname而不是username
                post.author = authorsMap[post.authorId]?.nickname || authorsMap[post.authorId]?.username || '未知用户';
              }
              return post;
            });
          }
        } else {
          console.log('所有帖子都有作者信息，无需额外查询');
        }

        this.allPosts = convertedPosts;

        this.loadPageData();
        
        console.log('最终帖子列表:', this.allPosts);
      } catch (error) {
        console.error('获取帖子列表出错', error);
        this.$message.error('获取帖子列表失败');
        this.useMockData();
      } finally {
        this.loading = false;
      }
    },
    
    // 使用模拟数据进行临时展示
    useMockData() {
      console.log('使用模拟数据');
      this.allPosts = [
        {
          id: 1,
          title: '【置顶】圈子发帖规范与社区守则',
          author: '圈务管理员',
          authorAvatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
          summary: '欢迎加入技术交流圈子！请仔细阅读本圈发帖规范：1. 禁止发布广告 2. 请选择合适标签 3. 友善交流，互相尊重...',
          content: '欢迎加入技术交流圈子！请仔细阅读本圈发帖规范：1. 禁止发布广告 2. 请选择合适标签 3. 友善交流，互相尊重...',
          commentCount: 45,
          likeCount: 128,
          createTime: '2024-03-15T10:30:00',
          circleId: 1
        },
        {
          id: 2,
          title: 'Vue 3 组合式 API 最佳实践分享',
          author: '前端小王子',
          authorAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          summary: '最近在项目中全面使用 Vue 3 + Composition API，总结了一些最佳实践：1. 逻辑拆分 2. 响应式陷阱 3. 与 TypeScript 结合...',
          content: '最近在项目中全面使用 Vue 3 + Composition API，总结了一些最佳实践：1. 逻辑拆分 2. 响应式陷阱 3. 与 TypeScript 结合...',
          commentCount: 32,
          likeCount: 89,
          createTime: '2024-03-14T15:20:00',
          circleId: 1
        },
        {
          id: 3,
          title: 'Java 并发编程实战：线程池使用指南',
          author: 'Java老兵',
          authorAvatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          summary: '线程池是 Java 并发编程中最重要的工具之一。本文将深入讲解 ThreadPoolExecutor 的 corePoolSize、maximumPoolSize、workQueue 等参数配置...',
          content: '线程池是 Java 并发编程中最重要的工具之一。本文将深入讲解 ThreadPoolExecutor 的 corePoolSize、maximumPoolSize、workQueue 等参数配置...',
          commentCount: 18,
          likeCount: 56,
          createTime: '2024-03-13T09:15:00',
          circleId: 1
        }
      ];
      this.loadPageData();
    },
    
    // 加载当前页数据（从 filteredPosts 中分页）
    loadPageData() {
      const start = (this.page - 1) * this.pageSize;
      const end = start + this.pageSize;
      const pageData = this.filteredPosts.slice(start, end);
      
      if (this.page === 1) {
        this.posts = pageData;
      } else {
        this.posts = [...this.posts, ...pageData];
      }
      
      // 判断是否还有更多数据
      this.hasMore = end < this.filteredPosts.length;
      console.log(`加载页面数据: 第${this.page}页, 共${this.posts.length}条, 还有更多: ${this.hasMore}`);
    },
    
    // 重置状态并重新加载
    resetAndFetch() {
      console.log('重置并重新加载');
      this.posts = [];
      this.page = 1;
      this.hasMore = true;
      this.loading = false;
      this.fetchPosts();
    },
    
    // 解析作者信息
    parseAuthorInfo(authorInfo) {
      if (!authorInfo) return null;
      
      // 如果是字符串，尝试解析为JSON
      if (typeof authorInfo === 'string') {
        try {
          const parsed = JSON.parse(authorInfo);
          return parsed.nickname || parsed.username || null;
        } catch (e) {
          // 如果不是JSON字符串，直接返回
          return authorInfo;
        }
      }
      
      // 如果是对象，直接提取用户名
      if (typeof authorInfo === 'object') {
        return authorInfo.nickname || authorInfo.username || null;
      }
      
      return authorInfo;
    },

    // 加载更多数据（由无限滚动触发）
    async loadMore() {
      console.log('尝试加载更多数据');
      // 如果正在加载或没有更多数据，则不再触发
      if (this.loading || !this.hasMore) {
        console.log('停止加载更多: loading=', this.loading, 'hasMore=', this.hasMore);
        return;
      }
      
      this.loading = true;
      try {
        console.log('开始加载下一页');
        
        // 在实际项目中，这里应该调用分页API
        // 但由于API可能不支持分页，我们暂时使用前端分页
        // 如果需要后端分页，需要修改fetchPosts方法支持分页参数
        this.page += 1;
        
        // 加载下一页数据
        this.loadPageData();
        console.log('成功加载下一页');
        
      } catch (error) {
        console.error('加载更多帖子出错', error);
        this.$message.error('加载失败，请稍后重试');
        // 恢复页码
        this.page -= 1;
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped>
.circle-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.post-list {
  min-height: 400px;
}

.post-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border-radius: 8px;
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

/* 圈子名称 */
.circle-name {
  font-size: 13px;
  color: #409EFF;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.circle-name i {
  font-size: 14px;
}

/* 帖子标题 */
.post-title {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

/* 作者信息 */
.post-author {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

/* 帖子内容 */
.post-content {
  font-size: 15px;
  line-height: 1.6;
  color: #4a4a4a;
  margin-bottom: 16px;
  display: -webkit-box;
  /* 限制显示3行 */
  -webkit-line-clamp: 3;     
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 帖子底部 */
.post-footer {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  color: #909399;
  font-size: 14px;
}

.post-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s;
}

.post-stat:hover {
  color: #409EFF;
}

.post-stat i {
  font-size: 16px;
}

.no-more {
  text-align: center;
  padding: 30px;
  color: #909399;
  font-size: 14px;
  background: #f9f9f9;
  border-radius: 8px;
}

.no-posts {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  font-size: 16px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-top: 20px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .circle-container {
    padding: 10px;
  }
  
  .post-title {
    font-size: 18px;
  }
  
  .post-content {
    font-size: 14px;
  }
  
  .post-footer {
    gap: 16px;
  }
}
</style>