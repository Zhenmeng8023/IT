<template>
  <div class="circle-container">
    <section class="circle-hero">
      <div class="hero-copy">
        <span class="hero-badge">Circle Hub</span>
        <h1 class="hero-title">开发者圈子</h1>
        <p class="hero-subtitle">浏览实时讨论、经验沉淀和项目交流，让每次进入圈子都能快速找到值得参与的话题。</p>
      </div>
      <div class="hero-panel">
        <div class="hero-panel-item">
          <span class="hero-panel-label">已加载帖子</span>
          <strong class="hero-panel-value">{{ posts.length }}</strong>
        </div>
        <div class="hero-panel-item">
          <span class="hero-panel-label">圈子数量</span>
          <strong class="hero-panel-value">{{ circles.length }}</strong>
        </div>
        <div class="hero-panel-item">
          <span class="hero-panel-label">当前搜索</span>
          <strong class="hero-panel-value hero-panel-text">{{ keyword || '全部主题' }}</strong>
        </div>
      </div>
    </section>

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
        <div class="post-topline">
          <div class="circle-name">
            <i class="el-icon-chat-dot-round"></i> {{ getCircleNameById(post.circleId) || '未知圈子' }}
          </div>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>

        <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
        
        <!-- 作者信息 -->
        <div class="post-author">
          <el-avatar :size="32" :src="post.authorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"></el-avatar>
          <div class="author-meta">
            <span class="author-name">{{ post.author || '未知用户' }}</span>
            <span class="author-role">活跃讨论者</span>
          </div>
        </div>
        
        <!-- 帖子内容摘要 -->
        <div class="post-content">
          {{ post.summary || post.content || '暂无内容介绍' }}
        </div>
        
        <!-- 帖子底部信息：评论数、点赞数 -->
        <div class="post-footer">
          <div class="post-footer-main">
            <span class="post-stat">
              <i class="el-icon-chat-line-round"></i> {{ post.commentCount || 0 }}
            </span>
            <span class="post-stat">
              <i class="el-icon-star-off"></i> {{ post.likes || 0 }}
            </span>
          </div>
          <span class="post-enter">
            进入讨论
            <i class="el-icon-right"></i>
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
import { GetCirclePosts, GetAllCirclePosts, GetUserById, GetAllCircles } from '@/api/index.js';
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
      authorname: '未知用户', // 作者名称
      authorAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', // 作者头像
    };
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

     // 通过作者ID获取作者信息（包括nickname和avatarUrl）
     async getAuthorNameById(authorId) {
      if (!authorId) {
        return { nickname: '未知用户', avatarUrl: null };
      }
      
      try {
        // 假设后端有提供根据ID获取用户信息的API
        const response = await GetUserById(authorId);
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
    
    // 批量获取作者信息
    async getAuthorsByIds(authorIds) {
      const authorsMap = {};
      
      // 去重，避免重复请求
      const uniqueAuthorIds = [...new Set(authorIds)];
      
      for (const authorId of uniqueAuthorIds) {
        try {
          const authorInfo = await this.getAuthorNameById(authorId);
          authorsMap[authorId] = authorInfo;
        } catch (error) {
          console.error(`获取作者信息失败，ID: ${authorId}`, error);
          authorsMap[authorId] = { nickname: '未知用户', avatarUrl: null };
        }
      }
      
      return authorsMap;
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
        const response = await GetAllCircles()
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
          console.log('获取所有主题帖列表');
        
        // 使用获取全部主题帖的接口
        const response = await GetAllCirclePosts();
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
            authorId: post.authorId || post.userId || post.creatorId || (typeof post.author === 'object' ? post.author.id : undefined), // 从author对象中提取authorId
            author: this.parseAuthorInfo(post.author) || post.userName || post.creator || post.user?.username || '匿名用户',
            authorAvatar: post.avatarUrl || post.avatar || post.userAvatar || post.user?.avatar || post.user?.avatarUrl || (typeof post.author === 'object' ? post.author.avatarUrl : undefined) || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
            createdAt: post.createTime || post.createdAt || post.createDate || new Date().toISOString(),
            likes: post.likeCount || post.likes || 0,
            commentCount: post.commentCount || post.replyCount || 0,
          };
          
          console.log('转换后的帖子:', normalizedPost);
          console.log('作者头像URL:', normalizedPost.authorAvatar);
          return normalizedPost;
        });

        // 为所有帖子获取作者信息（包括头像）
        console.log('为所有帖子获取作者信息...');
        const postsWithAuthorId = convertedPosts.filter(post => post.authorId);
        console.log('有作者ID的帖子数量:', postsWithAuthorId.length);
        
        if (postsWithAuthorId.length > 0) {
          // 收集所有作者ID
          const authorIds = postsWithAuthorId
            .map(post => post.authorId);
          
          console.log('作者信息:', authorIds);
          
          if (authorIds.length > 0) {
            // 获取作者信息并更新帖子数据
            const authorsMap = await this.getAuthorsByIds(authorIds);
            
            convertedPosts = convertedPosts.map(post => {
              if (post.authorId) {
                // 使用nickname而不是username
                post.author = authorsMap[post.authorId]?.nickname || authorsMap[post.authorId]?.username || '未知用户';
                // 同时更新头像
                post.authorAvatar = authorsMap[post.authorId]?.avatarUrl || authorsMap[post.authorId]?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
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
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 20px 40px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.12), transparent 30%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.12), transparent 28%),
    linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%);
  min-height: 100vh;
}

.circle-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.95fr);
  gap: 22px;
  margin-bottom: 26px;
}

.hero-copy,
.hero-panel,
.post-card,
.no-more,
.no-posts {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px);
}

.hero-copy {
  padding: 32px;
  border-radius: 28px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(2, 132, 199, 0.08);
  color: #0284c7;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 14px;
}

.hero-title {
  margin: 0 0 12px;
  font-size: clamp(2rem, 4vw, 3.1rem);
  line-height: 1.08;
  color: #0f172a;
}

.hero-subtitle {
  margin: 0;
  max-width: 620px;
  color: #475569;
  font-size: 15px;
  line-height: 1.8;
}

.hero-panel {
  border-radius: 28px;
  padding: 20px;
  display: grid;
  gap: 14px;
}

.hero-panel-item {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.hero-panel-label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  color: #64748b;
}

.hero-panel-value {
  display: block;
  font-size: 1.8rem;
  line-height: 1.1;
  color: #0f172a;
}

.hero-panel-text {
  font-size: 1.05rem;
  word-break: break-word;
}

.post-list {
  min-height: 400px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.post-card {
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
  border-radius: 24px;
  overflow: hidden;
}

.post-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 45px rgba(15, 23, 42, 0.12);
  border-color: rgba(2, 132, 199, 0.22);
}

.post-topline,
.post-author,
.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.post-topline {
  margin-bottom: 14px;
}

.circle-name {
  font-size: 13px;
  color: #0284c7;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e0f2fe;
  font-weight: 700;
}

.circle-name i {
  font-size: 14px;
}

.post-time {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.post-title {
  font-size: 21px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 14px;
  line-height: 1.45;
}

.post-author {
  justify-content: flex-start;
  margin-bottom: 16px;
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.author-name {
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.author-role {
  font-size: 12px;
  color: #94a3b8;
}

.post-content {
  font-size: 14px;
  line-height: 1.8;
  color: #475569;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 100px;
}

.post-footer {
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  margin-top: auto;
}

.post-footer-main {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.post-stat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  transition: color 0.2s ease;
}

.post-stat:hover,
.post-enter {
  color: #0284c7;
}

.post-stat i,
.post-enter i {
  font-size: 15px;
}

.post-enter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
}

.no-more,
.no-posts {
  grid-column: 1 / -1;
  text-align: center;
  padding: 28px 20px;
  color: #64748b;
  font-size: 14px;
  border-radius: 22px;
}

.no-posts {
  padding: 64px 20px;
  font-size: 16px;
}

@media screen and (max-width: 1024px) {
  .circle-hero {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .circle-container {
    padding: 18px 12px 32px;
  }

  .hero-copy {
    padding: 24px 20px;
  }

  .hero-panel {
    grid-template-columns: 1fr;
  }

  .post-list {
    grid-template-columns: 1fr;
  }

  .post-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media screen and (max-width: 480px) {
  .hero-title {
    font-size: 1.9rem;
  }

  .post-topline {
    flex-direction: column;
    align-items: flex-start;
  }

  .post-title {
    font-size: 18px;
  }

  .post-content {
    min-height: auto;
  }
}
</style>
