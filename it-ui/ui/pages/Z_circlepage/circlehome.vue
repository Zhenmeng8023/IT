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
          <h3>{{ post.title }}</h3>
          <p style="color: #909399; font-size: 14px;">作者：{{ post.author }}</p>
          <p>{{ post.summary }}</p>
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
        posts: [             // 帖子列表
            {
                id: 1,
                title: '帖子1',
                author: '用户A',
                summary: '这是帖子1的摘要',
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
    },
    watch: {
      // 监听搜索关键词变化，重置列表并重新加载
      keyword: {
        handler() {
          this.resetAndFetch();
        },
        immediate: true, // 组件创建时立即执行一次
      },
    },
    methods: {
      // 跳转到帖子详情页（需自行创建 pages/circle/_id.vue）
      goToPostDetail(id) {
        this.$router.push(`/circle/${id}`);
      },
  
      // 重置状态（清空列表、重置页码、恢复 hasMore）
      resetAndFetch() {
        this.posts = [];
        this.page = 1;
        this.hasMore = true;
        this.loading = false;
        // 立即加载第一页
        this.loadMore();
      },
  
      // 加载更多数据（由无限滚动触发）
      async loadMore() {
        // 如果正在加载或没有更多数据，则不再触发
        if (this.loading || !this.hasMore) return;
  
        this.loading = true;
        try {
          // 调用后端 API，传递分页参数和搜索关键词
          const res = await this.$axios.get('/api/posts', {
            params: {
              page: this.page,
              limit: this.pageSize,
              keyword: this.keyword,
            },
          });
  
          // 假设后端返回格式为 { code: 0, data: { list: [], total: 100 } }
          if (res.data.code === 0) {
            const list = res.data.data.list || [];
            const total = res.data.data.total || 0;
  
            // 将新数据追加到现有列表
            this.posts.push(...list);
  
            // 判断是否还有更多数据：已加载数量 < 总条数
            this.hasMore = this.posts.length < total;
  
            // 页码加1，准备加载下一页
            this.page += 1;
          } else {
            this.$message.error('获取帖子列表失败：' + res.data.message);
          }
        } catch (error) {
          console.error('获取帖子列表出错', error);
          this.$message.error('网络错误，请稍后重试');
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
  }
  .post-list {
    min-height: 400px;
  }
  .post-card {
    margin-bottom: 20px;
    cursor: pointer;
    transition: all 0.3s;
  }
  .post-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
  .no-more {
    text-align: center;
    padding: 20px;
    color: #909399;
    font-size: 14px;
  }
  </style>