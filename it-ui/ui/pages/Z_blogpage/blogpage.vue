<template>
    <div>
        <div class="blog-list">
            <!-- padding为主体内边距 -->
            <el-card
            :body-style="{ padding: '20px' }"
            shadow="hover"
            @click.native="goToDetail(post.id)"
            class="blog-card-item"
            v-for="post in paginatedPosts"
            :key="post.id"
            >
              <!-- 遍历posts数组每个为一个post为博客 -->
              <!-- 标题 -->
              <h3>{{ post.title }}</h3>
              <!-- 作者 -->
              <p style="color: #909399; font-size: 14px;">作者：{{ post.author }}</p>
              <!-- 摘要 -->
              <p>{{ post.summary }}</p>
            </el-card>
        </div>
        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            @current-change="handlePageChange">
          </el-pagination>
        </div>
    </div>
  </template>
  
<script>
  export default {
    layout:'blog',
    data() {
      return {
        searchtext: '',
        currentPage: 1,
        pageSize: 5,
        total: 0,
        posts: [],
        loading: true
      }
    },
    async mounted() {
      await this.fetchPosts();
    },
    computed: {
          //搜索关键词过滤博客
          filteredPosts() {
            if (!this.searchtext) {
              return this.posts;               // 如果搜索框为空，返回所有博客
            }
            return this.posts.filter(post => 
              post.title.toLowerCase().includes(this.searchtext.toLowerCase()) ||       // 标题包含搜索关键词
              post.summary.toLowerCase().includes(this.searchtext.toLowerCase())       // 摘要包含搜索关键词
            );
          },
          //分页函数
          paginatedPosts() {
            const start = (this.currentPage - 1) * this.pageSize;   // 计算当前页的起始索引
            const end = start + this.pageSize;                      // 计算当前页的结束索引
            return this.filteredPosts.slice(start, end);            // 返回当前页的博客数组
          }
        },
      methods: {
        goToDetail(id) {
            // 跳转到博客详情页，传递博客ID作为参数
            console.log("点击了博客详情页", id);
        },
        async fetchPosts() {
          try {
            this.loading = true;
            // 从后端 API 获取博客数据
            const response = await this.$axios.get('/api/posts', {
              params: {
                page: this.currentPage,
                limit: this.pageSize,
                keyword: this.searchtext
              }
            });
            
            // 根据后端返回的数据结构进行适配
            // 假设后端返回格式为 { success: true, data: [...], total: 100 }
            if (response.success) {
              this.posts = response.data;
              this.total = response.total || response.data.length;
            } else {
              console.error('获取博客数据失败:', response.message);
              this.posts = [];
              this.total = 0;
            }
          } catch (error) {
            console.error('请求博客数据时出错:', error);
            this.posts = [];
            this.total = 0;
          } finally {
            this.loading = false;
          }
        },
        async handleSearch() {
          console.log(this.searchtext);
          this.currentPage = 1; // 搜索时重置到第一页
          await this.fetchPosts();
        },
        async handlePageChange(page) {
          this.currentPage = page;
          console.log('当前页码:', page);
          await this.fetchPosts();
        }
      }
    }
</script>
  
<style>
  .container {
    margin: 0;
    padding: 0;
    min-height: 100vh;
    overflow: hidden;          /* 将overflow: hidden限制在当前组件内部 */
  }

  .container {
    height: 100vh;
    width: 100vw;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
  }

  .header-content {
    display: flex;
    align-items: center;
    height: 100%;
    background-color: #E9EEF3;
    color: #ffffff;
  }
  
  .search-input {
      width: 40%;
      margin-left: auto;
      margin-right: 10px;
  }
  
  .search-btn {
      margin-right: 20px;
  }
  
  .avatar-wrapper {
      margin-left: auto;
  }
  
  body {
      background-color: white !important;
  }
  
  .el-main {
  background-color: #E9EEF3;
  color: #333;
  padding: 0 20px;
  overflow-y: auto;
  }

  .blog-title {
    font-size: 25px;
    font-weight: bold;
  }
  
  .blog-card {
    margin-bottom: 20px;
  }
  .el-header {
    background-color: #E9EEF3;
  }

  .pagination-wrapper {
    margin-top: auto;
    margin-bottom: 20px;
    display: flex;
    justify-content: center;
  }
</style>