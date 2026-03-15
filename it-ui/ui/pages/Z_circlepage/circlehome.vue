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
          <i class="el-icon-chat-dot-round"></i> 技术交流圈
        </div>
        
        <!-- 帖子标题 -->
        <h3 class="post-title">{{ post.title }}</h3>
        
        <!-- 作者信息 -->
        <div class="post-author">
          <el-avatar :size="24" :src="post.authorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"></el-avatar>
          <span class="author-name">{{ post.author }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
        </div>
        
        <!-- 帖子内容摘要 -->
        <div class="post-content">
          {{ post.summary }}
        </div>
        
        <!-- 帖子底部信息：评论数、点赞数 -->
        <div class="post-footer">
          <span class="post-stat">
            <i class="el-icon-chat-line-round"></i> {{ post.commentCount || 0 }}
          </span>
          <span class="post-stat">
            <i class="el-icon-star-off"></i> {{ post.likeCount || 0 }}
          </span>
        </div>
      </el-card>

      <!-- 没有更多数据时的提示 -->
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        没有更多了
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
      posts: [],           // 帖子列表（初始为空，稍后通过模拟数据填充）
      
      // 模拟所有帖子数据（用于实验）
      mockPosts: [
        {
          id: 1,
          title: '【置顶】圈子发帖规范与社区守则',
          author: '圈务管理员',
          authorAvatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
          summary: '欢迎加入技术交流圈子！请仔细阅读本圈发帖规范：1. 禁止发布广告 2. 请选择合适标签 3. 友善交流，互相尊重...',
          commentCount: 45,
          likeCount: 128,
          createTime: '2024-03-15 10:30',
        },
        {
          id: 2,
          title: 'Vue 3 组合式 API 最佳实践分享',
          author: '前端小王子',
          authorAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          summary: '最近在项目中全面使用 Vue 3 + Composition API，总结了一些最佳实践：1. 逻辑拆分 2. 响应式陷阱 3. 与 TypeScript 结合...',
          commentCount: 32,
          likeCount: 89,
          createTime: '2024-03-14 15:20',
        },
        {
          id: 3,
          title: 'Java 并发编程实战：线程池使用指南',
          author: 'Java老兵',
          authorAvatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          summary: '线程池是 Java 并发编程中最重要的工具之一。本文将深入讲解 ThreadPoolExecutor 的 corePoolSize、maximumPoolSize、workQueue 等参数配置...',
          commentCount: 18,
          likeCount: 56,
          createTime: '2024-03-13 09:15',
        },
        {
          id: 4,
          title: 'React Hooks 依赖数组问题',
          author: 'React新手',
          authorAvatar: 'https://cube.elemecdn.com/2/8e/4a7b8c9d0e1f2a3b4c5d6e7f8g9h0i.png',
          summary: '我在使用 useEffect 时遇到一个问题：当我传入一个函数作为依赖时，会导致无限循环。代码如下：... 请问如何解决？',
          commentCount: 7,
          likeCount: 3,
          createTime: '2024-03-12 22:10',
        },
        {
          id: 5,
          title: 'Docker 容器化部署 Spring Boot 应用',
          author: '运维小能手',
          authorAvatar: 'https://cube.elemecdn.com/4/5d/6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s.png',
          summary: '手把手教你将 Spring Boot 应用容器化：1. 编写 Dockerfile 2. 构建镜像 3. 使用 docker-compose 编排 4. 部署到服务器...',
          commentCount: 24,
          likeCount: 67,
          createTime: '2024-03-11 14:30',
        },
        {
          id: 6,
          title: '算法刷题心得：动态规划入门',
          author: '算法爱好者',
          authorAvatar: 'https://cube.elemecdn.com/5/6f/7g8h9i0j1k2l3m4n5o6p7q8r9s0t.png',
          summary: '刷了 200+ 道 DP 题后，总结的动态规划解题模板：1. 定义状态 2. 状态转移方程 3. 初始化 4. 遍历顺序 5. 返回结果...',
          commentCount: 41,
          likeCount: 112,
          createTime: '2024-03-10 08:45',
        },
        {
          id: 7,
          title: 'Python 爬虫实战：爬取豆瓣电影 Top250',
          author: 'Pythonista',
          authorAvatar: 'https://cube.elemecdn.com/6/7g/8h9i0j1k2l3m4n5o6p7q8r9s0t1u.png',
          summary: '使用 requests + BeautifulSoup 爬取豆瓣电影 Top250，并保存到 CSV 文件。包含反爬策略、数据清洗等技巧...',
          commentCount: 29,
          likeCount: 78,
          createTime: '2024-03-09 16:20',
        },
        {
          id: 8,
          title: '微服务架构 vs 单体架构，你怎么选？',
          author: '架构师之路',
          authorAvatar: 'https://cube.elemecdn.com/7/8h/9i0j1k2l3m4n5o6p7q8r9s0t1u2v.png',
          summary: '在开始一个新项目时，如何选择架构风格？微服务带来灵活性的同时也增加了复杂度。欢迎大家讨论自己的实践经验...',
          commentCount: 53,
          likeCount: 94,
          createTime: '2024-03-08 11:05',
        },
        {
          id: 9,
          title: 'Git 工作流最佳实践：Git Flow vs GitHub Flow',
          author: '版本控制专家',
          authorAvatar: 'https://cube.elemecdn.com/8/9i/0j1k2l3m4n5o6p7q8r9s0t1u2v3w.png',
          summary: '详细对比 Git Flow 和 GitHub Flow 两种工作流，分析各自的适用场景，并给出团队协作建议...',
          commentCount: 16,
          likeCount: 45,
          createTime: '2024-03-07 13:40',
        },
        {
          id: 10,
          title: 'MySQL 索引优化实战',
          author: '数据库老司机',
          authorAvatar: 'https://cube.elemecdn.com/9/0j/1k2l3m4n5o6p7q8r9s0t1u2v3w4x.png',
          summary: '从执行计划分析到索引设计，深入浅出讲解 MySQL 索引优化技巧。包含 explain 详解、索引失效场景、覆盖索引等...',
          commentCount: 37,
          likeCount: 103,
          createTime: '2024-03-06 10:15',
        },
        {
          id: 11,
          title: 'TypeScript 高级类型技巧',
          author: 'TS高手',
          authorAvatar: 'https://cube.elemecdn.com/0/1k/2l3m4n5o6p7q8r9s0t1u2v3w4x5y.png',
          summary: '分享 TypeScript 中的高级类型用法：条件类型、映射类型、类型守卫、infer 关键字等，让你的代码更安全...',
          commentCount: 22,
          likeCount: 61,
          createTime: '2024-03-05 17:30',
        },
        {
          id: 12,
          title: 'Spring Boot 集成 Redis 报错',
          author: 'Java萌新',
          authorAvatar: 'https://cube.elemecdn.com/1/2l/3m4n5o6p7q8r9s0t1u2v3w4x5y6z.png',
          summary: '按照教程配置 RedisTemplate 后，运行时出现序列化异常。错误日志如下：... 求大佬指点！',
          commentCount: 5,
          likeCount: 2,
          createTime: '2024-03-04 19:25',
        },
      ],
    };
  },
  
  computed: {
    // 从路由 query 中获取搜索关键词（由头部搜索框联动）
    keyword() {
      return this.$route.query.keyword || '';
    },
    // 无限滚动是否应禁用：正在加载 或 没有更多数据
    scrollDisabled() {
      return this.loading || !this.hasMore;
    },
    // 根据搜索关键词过滤后的帖子
    filteredPosts() {
      let filtered = this.mockPosts;
      
      // 关键词搜索（搜索标题、作者和内容）
      if (this.keyword) {
        const keywordLower = this.keyword.toLowerCase();
        filtered = filtered.filter(post => 
          post.title.toLowerCase().includes(keywordLower) || 
          post.author.toLowerCase().includes(keywordLower) ||
          post.summary.toLowerCase().includes(keywordLower)
        );
      }
      
      return filtered;
    },
  },
  
  watch: {
    // 监听路由参数变化（关键词）
    '$route.query': {
      handler() {
        this.resetAndFetch();
      },
      deep: true,
      immediate: true,
    },
  },
  
  created() {
    // 初始化时直接加载第一页（从过滤后的数据中取）
    this.loadPageData();
  },
  
  methods: {
    // 跳转到帖子详情页
    goToPostDetail(id) {
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
    },
    
    // 重置状态并重新加载
    resetAndFetch() {
      this.posts = [];
      this.page = 1;
      this.hasMore = true;
      this.loading = false;
      this.loadPageData();
    },
    
    // 加载更多数据（由无限滚动触发）
    async loadMore() {
      // 如果正在加载或没有更多数据，则不再触发
      if (this.loading || !this.hasMore) return;
      
      // 模拟网络请求延迟
      this.loading = true;
      try {
        // 模拟 API 请求延迟
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // 页码加1
        this.page += 1;
        
        // 加载下一页数据
        this.loadPageData();
        
      } catch (error) {
        console.error('加载更多帖子出错', error);
        this.$message.error('加载失败，请稍后重试');
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