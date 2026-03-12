<template>
  <div class="blog-list" v-loading="loading">
    <el-card
      v-for="post in posts"
      :key="post.id"
      class="blog-card-item"
      shadow="hover"
      @click.native="goToDetail(post.id)"
    >
      <h3>{{ post.title }}</h3>
      <p style="color: #909399; font-size: 14px;">作者：{{ post.author }}</p>
      <p>{{ post.summary }}</p>
    </el-card>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  layout:'blog',
  data() {
    return {
      posts: [],
      total: 0,
      pageSize: 5,
      loading: false,
    };
  },
  computed: {
    // 从路由 query 获取当前页
    currentPage: {
      get() {
        return parseInt(this.$route.query.page) || 1;
      },
      set(page) {
        this.$router.push({
          query: { ...this.$route.query, page: page > 1 ? page : undefined },
        });
      },
    },
    // 搜索关键词
    keyword() {
      return this.$route.query.keyword || '';
    },
    // 标签
    tag() {
      return this.$route.query.tag || '';
    },
  },
  watch: {
    // 监听路由参数变化，重新获取数据
    '$route.query': {
      handler: 'fetchPosts',
      deep: true,
      immediate: true,
    },
  },
  methods: {
    goToDetail(id) {
      this.$router.push(`/blog/${id}`);
    },
    async fetchPosts() {
      this.loading = true;
      try {
        const res = await this.$axios.get('/api/posts', {
          params: {
            page: this.currentPage,
            limit: this.pageSize,
            keyword: this.keyword,
            tag: this.tag,
          },
        });
        if (res.data.code === 0) {
          this.posts = res.data.data.list;
          this.total = res.data.data.total;
        } else {
          this.$message.error('获取博客列表失败');
        }
      } catch (error) {
        console.error(error);
        this.$message.error('网络错误');
      } finally {
        this.loading = false;
      }
    },
    handlePageChange(page) {
      this.currentPage = page; // setter 会自动更新路由
    },
  },
};
</script>

<style scoped>

</style>