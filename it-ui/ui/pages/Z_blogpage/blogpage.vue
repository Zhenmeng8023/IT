<template>
  <div class="blog-container">
    <!-- ========== 页面头部 ========== -->
    <section class="blog-hero">
      <div class="blog-header">
        <span class="hero-badge">Blog Space</span>
        <h1 class="page-title">技术博客</h1>
        <p class="page-subtitle">发现最新的技术文章、沉淀团队经验，与开发者一起成长。</p>
      </div>

      <div class="hero-stats">
        <div class="hero-stat-card">
          <span class="hero-stat-label">当前页文章</span>
          <strong class="hero-stat-value">{{ posts.length }}</strong>
        </div>
        <div class="hero-stat-card">
          <span class="hero-stat-label">全部文章</span>
          <strong class="hero-stat-value">{{ total || posts.length }}</strong>
        </div>
        <div class="hero-stat-card">
          <span class="hero-stat-label">当前筛选</span>
          <strong class="hero-stat-value hero-stat-text">{{ keyword || tag || author || '全部主题' }}</strong>
        </div>
      </div>
    </section>

    <!-- ========== 排序工具栏 ========== -->
    <div class="sort-toolbar">
      <div class="sort-wrapper">
        <div class="sort-copy">
          <span class="sort-label">内容排序</span>
          <p class="sort-desc">
            按{{
              sortType === 'hot'
                ? '热门热度'
                : sortType === 'time_asc'
                  ? '发布时间从早到晚'
                  : '发布时间从新到旧'
            }}查看文章
          </p>
        </div>
        <el-radio-group v-model="sortType" size="small" @change="handleSortChange" class="sort-group">
          <el-radio-button label="hot">
            <i class="el-icon-fire"></i> 热门
          </el-radio-button>
          <el-radio-button label="time_desc">
            <i class="el-icon-download"></i> 最新
          </el-radio-button>
          <el-radio-button label="time_asc">
            <i class="el-icon-upload"></i> 最早
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="sort-hint">
        <span v-if="tag">标签：#{{ tag }}</span>
        <span v-else-if="author">作者：{{ author }}</span>
        <span v-else-if="keyword">关键词：{{ keyword }}</span>
        <span v-else>当前展示全部文章</span>
      </div>
    </div>

    <!-- ========== 加载状态 ========== -->
    <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
      <!-- 博客列表 - 卡片网格 -->
      <div v-if="!loading && posts.length > 0" class="blog-grid">
        <el-card
          v-for="post in posts"
          :key="post.id"
          class="blog-card"
          shadow="hover"
          @click.native="goToDetail(post)"
        >
          <div class="card-content">
            <div class="card-eyebrow">
              <span class="eyebrow-pill">
                {{ post.tags && post.tags.length ? '#' + post.tags[0] : '技术分享' }}
              </span>
              <span class="eyebrow-link">点击阅读全文</span>
            </div>

            <!-- 标题 + VIP 标识行 -->
            <div class="title-wrapper">
              <h3 class="blog-title">{{ post.title || '无标题' }}</h3>
              <!-- 价格标签：根据 price 字段显示不同类型 -->
              <el-tag 
                v-if="post.price !== undefined && post.price !== null" 
                :type="getPriceTagType(post.price)" 
                size="mini" 
                class="price-tag"
                v-text="getPriceTagText(post.price)"
              ></el-tag>
            </div>

            <!-- 作者信息 -->
            <div class="card-meta-row">
              <div class="author-info" @click.stop="goToAuthorPage(post.author)">
                <el-avatar :size="28" :src="post.author?.avatar" class="author-avatar"></el-avatar>
                <span class="author-name">{{ post.author?.nickname || '未知作者' }}</span>
              </div>
              <span class="meta-reading">
                <i class="el-icon-view"></i>
                {{ post.viewCount || 0 }} 阅读
              </span>
            </div>

            <!-- 标签区域 -->
            <div class="tags-container" v-if="post.tags && post.tags.length > 0">
              <el-tag
                v-for="tag in post.tags"
                :key="tag"
                size="small"
                class="tag-item"
                @click.stop="filterByTag(tag)"
              >
                #{{ tag }}
              </el-tag>
            </div>

            <!-- 内容预览（去除HTML标签并截断） -->
            <p class="blog-excerpt">{{ formatContent(post.content) }}</p>

            <!-- 统计信息（浏览量、点赞数、评论数） -->
            <div class="post-stats">
              <span v-if="post.likeCount !== undefined" class="stat-item">
                <i class="el-icon-star-off"></i>
                <span>{{ post.likeCount }}</span>
              </span>
              <span v-if="post.commentCount !== undefined" class="stat-item">
                <i class="el-icon-chat-line-round"></i>
                <span>{{ post.commentCount }}</span>
              </span>
              <span class="stat-item stat-item-link">
                <i class="el-icon-right"></i>
                <span>查看详情</span>
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && posts.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="el-icon-document"></i>
        </div>
        <h3 class="empty-title">暂无博客</h3>
        <p class="empty-desc">还没有任何博客文章，去看看其他内容吧</p>
      </div>
    </div>

    <!-- ========== 分页 ========== -->
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

    <!-- ========== VIP开通引导弹窗 ========== -->
    <el-dialog
      title="VIP专属内容"
      :visible.sync="vipDialogVisible"
      width="400px"
      :close-on-click-modal="false"
    >
      <div style="text-align: center; padding: 20px;">
        <i class="el-icon-star-on" style="font-size: 48px; color: #f5a623;"></i>
        <h3>此内容仅限VIP会员阅读</h3>
        <p>开通VIP会员，畅享全部优质内容</p>
        <el-button type="primary" @click="goToVipPage" style="margin-top: 20px;">立即开通VIP</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
// 导入博客相关的API方法
import { GetAllBlogs, SearchBlogs, SearchBlogsByTag, SearchBlogsByAuthor, SortBlogs } from '@/api/index'
import { GetCurrentUser } from '@/api/index'

export default {
  layout: 'blog',                     // 使用博客布局（包含侧边栏、头部等）
  data() {
    return {
      // ---------- 博客数据 ----------
      posts: [],                      // 博客列表数据
      total: 0,                       // 博客总数（用于分页）
      pageSize: 5,                    // 每页显示数量
      loading: false,                // 加载状态
      sortType: 'time_desc',         // 当前排序方式：hot（热门）、time_desc（最新）、time_asc（最早）

      // ---------- 用户VIP状态 ----------
      isLoggedIn: false,              // 用户是否已登录
      isVipUser: false,              // 当前用户是否为VIP会员

      // ---------- VIP弹窗 ----------
      vipDialogVisible: false,        // 控制VIP引导弹窗显示
      pendingBlog: null               // 记录被点击的付费博客（可用于开通后自动跳转）
    };
  },
  computed: {
    /**
     * 当前页码的 getter/setter
     * 从路由 query 中获取，并通过路由更新
     */
    currentPage: {
      get() {
        return parseInt(this.$route.query.page) || 1;
      },
      set(page) {
        const newQuery = { ...this.$route.query };
        if (page > 1) {
          newQuery.page = page;
        } else {
          delete newQuery.page; // 页码为1时移除 page 参数
        }

        // 如果 query 为空对象，则不传递任何 query 参数
        if (Object.keys(newQuery).length === 0) {
          this.$router.push({ path: this.$route.path });
        } else {
          this.$router.push({ query: newQuery });
        }
      },
    },
    /**
     * 从路由获取作者筛选参数
     */
    author() {
      return this.$route.query.author || '';
    },
    /**
     * 从路由获取关键词搜索参数
     */
    keyword() {
      return this.$route.query.keyword || '';
    },
    /**
     * 从路由获取标签筛选参数
     */
    tag() {
      return this.$route.query.tag || '';
    },
    /**
     * 从路由获取排序类型
     */
    sortFromRoute() {
      return this.$route.query.sort || 'time_desc';
    },
  },
  watch: {
    /**
     * 监听路由参数变化，当查询条件改变时重新获取数据
     */
    '$route.query': {
      handler() {
        this.sortType = this.sortFromRoute;
        this.fetchPosts();
      },
      deep: true,
      immediate: true,
    },
  },
  created() {
    // 组件创建时获取用户VIP状态
    this.getUserVipStatus();
  },
  methods: {
    getBlogSortTime(post) {
      const rawTime = post?.publishTime || post?.createdAt || post?.updatedAt || null;
      if (!rawTime) return 0;
      const time = new Date(rawTime).getTime();
      return Number.isNaN(time) ? 0 : time;
    },

    getHotScore(post) {
      const viewCount = Number(post?.viewCount) || 0;
      const likeCount = Number(post?.likeCount) || 0;
      const collectCount = Number(post?.collectCount) || 0;
      const downloadCount = Number(post?.downloadCount) || 0;
      return viewCount + likeCount * 5 + collectCount * 10 + downloadCount * 8;
    },

    sortPostList(list) {
      const posts = Array.isArray(list) ? [...list] : [];
      return posts.sort((a, b) => {
        if (this.sortType === 'hot') {
          const scoreDiff = this.getHotScore(b) - this.getHotScore(a);
          if (scoreDiff !== 0) return scoreDiff;
          return this.getBlogSortTime(b) - this.getBlogSortTime(a);
        }

        if (this.sortType === 'time_asc') {
          return this.getBlogSortTime(a) - this.getBlogSortTime(b);
        }

        return this.getBlogSortTime(b) - this.getBlogSortTime(a);
      });
    },

    // ========== VIP相关方法 ==========
    /**
     * 获取当前用户的VIP状态
     * 调用获取当前用户信息的接口，判断是否登录及是否为VIP会员
     */
    async getUserVipStatus() {
      try {
        const res = await GetCurrentUser();
        if (res && (res.data || res)) {
          const user = res.data || res;
          this.isLoggedIn = true;
          // 根据后端实际返回字段判断VIP状态（假设字段为 isVip 或 vipStatus）
          this.isVipUser = user.isVip || user.vipStatus === 'active' || false;
        }
      } catch (error) {
        // 未登录或接口失败，视为未登录状态
        this.isLoggedIn = false;
        this.isVipUser = false;
        console.log('未获取到用户信息，视为未登录');
      }
    },

    /**
     * 跳转到博客详情页，并进行VIP权限检查
     * @param {Object} post - 博客对象
     */
    goToDetail(post) {
      // 如果是付费内容且用户不是VIP，则拦截并提示开通VIP
      if (post.isVipOnly === true) {
        if (!this.isLoggedIn) {
          this.$message.warning('请先登录');
          this.$router.push('/login');
          return;
        }
        if (!this.isVipUser) {
          // 记录当前点击的博客，以便开通后跳转（可选功能）
          this.pendingBlog = post;
          this.vipDialogVisible = true;
          return;
        }
      }
      // 正常跳转到博客详情页
      this.$router.push(`/blog/${post.id}`);
    },

    /**
     * 跳转到 VIP 开通页面（充值页面）
     */
    goToVipPage() {
      this.vipDialogVisible = false;
      // 跳转到充值/VIP 开通页面（假设已有 /recharge 路由）
      this.$router.push('/recharge');
    },

    /**
     * 跳转到作者主页
     * @param {Object} author - 作者对象
     */
    goToAuthorPage(author) {
      if (author && author.id) {
        // 跳转到他人主页 other.vue
        this.$router.push(`/other/${author.id}`);
      } else {
        this.$message.warning('作者信息不可用');
      }
    },

    // ========== 列表操作 ==========
    /**
     * 点击标签进行筛选
     * @param {string} tag - 标签名称
     */
    filterByTag(tag) {
      this.$router.push({
        query: {
          ...this.$route.query,
          tag: tag,
          page: 1,   // 重置到第一页
        }
      });
    },

    /**
     * 处理排序变化
     */
    handleSortChange() {
      this.$router.push({
        query: {
          ...this.$route.query,
          sort: this.sortType,
          page: 1,   // 重置到第一页
        }
      });
    },

    // ========== 数据获取 ==========
    /**
     * 获取排序后的博客列表（无搜索条件时调用）
     */
    async getSortedBlogs() {
      try {
        console.log(`获取排序后的博客列表，排序方式: ${this.sortType}`);

        // 映射前端排序类型到后端接口需要的参数
        let sortTypeMap = {
          'hot': 'hot',
          'time_asc': 'time/oldest',
          'time_desc': 'time/newest'
        };
        const sortType = sortTypeMap[this.sortType] || 'time/newest';
        const params = {
          page: this.currentPage,
          limit: this.pageSize
        };

        const apiResponse = await SortBlogs(sortType, params);
        console.log('排序博客API响应:', apiResponse);

        // 处理响应数据（兼容多种格式）
        if (apiResponse && typeof apiResponse === 'object') {
          let list = [];
          let total = 0;
          if (Array.isArray(apiResponse)) {
            list = apiResponse;
            total = apiResponse.length;
          } else if (apiResponse.data && Array.isArray(apiResponse.data.list)) {
            list = apiResponse.data.list;
            total = apiResponse.data.total || 0;
          } else if (Array.isArray(apiResponse.data)) {
            list = apiResponse.data;
            total = apiResponse.data.length;
          } else {
            list = apiResponse.data || [];
            total = apiResponse.data?.length || 0;
          }
          list = this.sortPostList(list);
          // 确保每个博客对象都有 isVipOnly 字段（若后端未提供则默认 false）
          this.posts = list.map(p => ({ ...p, isVipOnly: p.isVipOnly || false }));
          this.total = total;
          return;
        }

        console.error('未知的API响应格式:', apiResponse);
        this.$message.error('获取博客列表失败：未知的响应格式');
        this.posts = [];
        this.total = 0;

      } catch (error) {
        console.error('获取排序博客列表失败:', error);
        this.$message.error('获取博客列表失败：' + (error.message || '网络错误'));
      }
    },

    /**
     * 执行搜索（有搜索条件时调用）
     */
    async performSearch() {
      try {
        let apiResponse;
        console.log('执行搜索');

        let params = {
          page: this.currentPage,
          limit: this.pageSize
        };

        // 根据不同的搜索类型调用不同接口
        if (this.keyword) {
          params.keyword = this.keyword;
          console.log('使用关键词搜索:', this.keyword);
          apiResponse = await SearchBlogs(params);
        } else if (this.tag) {
          params.keyword = this.tag;
          console.log('使用标签搜索:', this.tag);
          apiResponse = await SearchBlogsByTag(params);
        } else if (this.author) {
          params.keyword = this.author;
          console.log('使用作者搜索:', this.author);
          apiResponse = await SearchBlogsByAuthor(params);
        } else {
          this.posts = [];
          this.total = 0;
          return;
        }

        console.log('搜索API响应:', apiResponse);

        // 处理响应数据（格式同 getSortedBlogs）
        if (apiResponse && typeof apiResponse === 'object') {
          let list = [];
          let total = 0;
          if (Array.isArray(apiResponse)) {
            list = apiResponse;
            total = apiResponse.length;
          } else if (apiResponse.data && Array.isArray(apiResponse.data.list)) {
            list = apiResponse.data.list;
            total = apiResponse.data.total || 0;
          } else if (Array.isArray(apiResponse.data)) {
            list = apiResponse.data;
            total = apiResponse.data.length;
          } else {
            list = apiResponse.data || [];
            total = apiResponse.data?.length || 0;
          }
          list = this.sortPostList(list);
          this.posts = list.map(p => ({ ...p, isVipOnly: p.isVipOnly || false }));
          this.total = total;
          return;
        }

        console.error('未知的API响应格式:', apiResponse);
        this.$message.error('搜索博客失败：未知的响应格式');
        this.posts = [];
        this.total = 0;

      } catch (error) {
        console.error('执行搜索失败:', error);
        this.$message.error('搜索博客失败：' + (error.message || '网络错误'));
      }
    },

    /**
     * 获取博客列表（统一入口）
     * 根据是否存在搜索条件决定调用排序还是搜索方法
     */
    async fetchPosts() {
      this.loading = true;
      try {
        console.log('开始获取博客列表...');

        if (this.keyword || this.tag || this.author) {
          await this.performSearch();
        } else {
          await this.getSortedBlogs();
        }

      } catch (error) {
        console.error('获取博客列表失败:', error);
        this.$message.error('获取博客列表失败：' + (error.message || '网络错误'));
      } finally {
        this.loading = false;
      }
    },

    /**
     * 处理页码变化
     * @param {number} page - 新页码
     */
    handlePageChange(page) {
      console.log('页码变化:', page);
      this.currentPage = page; // setter 会自动更新路由
    },

    /**
     * 格式化内容，去除 HTML 标签并截断
     * @param {string} content - 博客内容 HTML
     * @returns {string} - 纯文本预览
     */
    formatContent(content) {
      if (!content) return '';
      // 去除 HTML 标签
      const plainText = content.replace(/<[^>]*>/g, '');
      // 截断前 100 个字符
      return plainText.length > 100 ? plainText.substring(0, 100) + '...' : plainText;
    },

    /**
     * 根据价格获取标签类型
     * @param {number} price - 博客价格
     * @returns {string} - 标签类型：success(免费), warning(VIP), primary(付费)
     */
    getPriceTagType(price) {
      if (price === 0) {
        return 'success'; // 免费博客 - 绿色
      } else if (price === -1) {
        return 'warning'; // VIP 专属 - 橙色
      } else {
        return 'primary'; // 付费博客 - 蓝色
      }
    },

    /**
     * 根据价格获取标签文本
     * @param {number} price - 博客价格
     * @returns {string} - 标签文本
     */
    getPriceTagText(price) {
      if (price === 0) {
        return '免费';
      } else if (price === -1) {
        return 'VIP';
      } else {
        return `¥${price}`;
      }
    },
  },
};
</script>

<style scoped>
.blog-container {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), transparent 28%),
    radial-gradient(circle at bottom left, rgba(14, 165, 233, 0.12), transparent 32%),
    linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
  color: #0f172a;
  padding: 32px 20px 48px;
  max-width: 1240px;
  margin: 0 auto;
}

.blog-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.95fr);
  gap: 22px;
  margin-bottom: 28px;
}

.blog-header,
.hero-stat-card,
.sort-toolbar,
.blog-card,
.empty-state {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(14px);
}

.blog-header {
  position: relative;
  overflow: hidden;
  padding: 32px;
  border-radius: 28px;
  margin: 0;
}

.blog-header::before {
  content: '';
  position: absolute;
  inset: auto -10% -38% 42%;
  height: 220px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.2), transparent 70%);
  pointer-events: none;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(30, 64, 175, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 14px;
}

.page-title {
  font-size: clamp(2rem, 4vw, 3.3rem);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -0.03em;
  margin: 0 0 12px;
  color: #0f172a;
}

.page-subtitle {
  margin: 0;
  max-width: 620px;
  font-size: 15px;
  line-height: 1.8;
  color: #475569;
}

.hero-stats {
  display: grid;
  gap: 16px;
}

.hero-stat-card {
  border-radius: 24px;
  padding: 22px 20px;
}

.hero-stat-label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
}

.hero-stat-value {
  display: block;
  font-size: 1.9rem;
  line-height: 1.1;
  color: #0f172a;
}

.hero-stat-text {
  font-size: 1.1rem;
  word-break: break-word;
}

.sort-toolbar {
  margin-bottom: 28px;
  padding: 18px 22px;
  border-radius: 24px;
}

.sort-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
}

.sort-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sort-label {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.sort-desc,
.sort-hint {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.sort-hint {
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
}

.sort-group :deep(.el-radio-button__inner) {
  border: 0;
  background: transparent;
  color: #475569;
  padding: 10px 18px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.25s ease;
}

.sort-group :deep(.el-radio-button__inner i) {
  margin-right: 4px;
  font-size: 12px;
}

.sort-group :deep(.el-radio-button__orig-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2);
}

.sort-group :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 999px 0 0 999px;
}

.sort-group :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 999px 999px 0;
}

.loading-container {
  min-height: 400px;
}

.blog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
  gap: 22px;
  margin-bottom: 28px;
}

.blog-card {
  border-radius: 24px;
  overflow: hidden;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
  cursor: pointer;
}

.blog-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 45px rgba(15, 23, 42, 0.12);
  border-color: rgba(59, 130, 246, 0.22);
}

.card-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 22px;
}

.card-eyebrow,
.card-meta-row,
.post-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-eyebrow {
  margin-bottom: 16px;
}

.eyebrow-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e0ecff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.eyebrow-link {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.title-wrapper {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.blog-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.45;
  flex: 1;
  transition: color 0.2s ease;
}

.blog-card:hover .blog-title {
  color: #1d4ed8;
}

.price-tag {
  margin-left: 8px;
  border: none;
  font-weight: 700;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
}

.card-meta-row {
  margin-bottom: 14px;
}

.author-info {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.author-avatar {
  border: 2px solid #dbeafe;
  transition: transform 0.25s ease, border-color 0.25s ease;
  flex-shrink: 0;
}

.blog-card:hover .author-avatar {
  border-color: #3b82f6;
  transform: scale(1.04);
}

.author-name {
  font-size: 14px;
  color: #334155;
  font-weight: 600;
}

.meta-reading {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.tag-item {
  background: #f8fafc;
  border: 1px solid transparent;
  color: #475569;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tag-item:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
  transform: translateY(-1px);
}

.blog-excerpt {
  font-size: 14px;
  color: #64748b;
  line-height: 1.8;
  margin: 0 0 18px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 76px;
}

.post-stats {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 12px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.stat-item i {
  font-size: 14px;
  color: #94a3b8;
  transition: color 0.2s ease;
}

.stat-item-link {
  margin-left: auto;
  color: #1d4ed8;
}

.blog-card:hover .stat-item-link i,
.stat-item:hover i {
  color: #1d4ed8;
}

.empty-state {
  text-align: center;
  padding: 64px 24px;
  border-radius: 28px;
  max-width: 420px;
  margin: 52px auto;
}

.empty-icon {
  font-size: 64px;
  color: #cbd5e1;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 10px;
}

.empty-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  line-height: 1.7;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 8px 0 0;
}

.pagination-wrapper :deep(.el-pagination) {
  font-weight: 500;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:not(.disabled).active) {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: white;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2);
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li),
.pagination-wrapper :deep(.el-pagination.is-background .btn-prev),
.pagination-wrapper :deep(.el-pagination.is-background .btn-next) {
  border-radius: 10px;
  transition: all 0.25s ease;
}

.pagination-wrapper :deep(.el-pagination.is-background .el-pager li:hover),
.pagination-wrapper :deep(.el-pagination.is-background .btn-prev:hover),
.pagination-wrapper :deep(.el-pagination.is-background .btn-next:hover) {
  color: #1d4ed8;
  background: #eff6ff;
}

@media screen and (max-width: 1024px) {
  .blog-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .blog-container {
    padding: 22px 14px 36px;
  }

  .blog-header {
    padding: 24px 20px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .sort-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .sort-group {
    width: 100%;
  }

  .sort-group :deep(.el-radio-group) {
    display: flex;
    width: 100%;
  }

  .sort-group :deep(.el-radio-button) {
    flex: 1;
  }

  .sort-group :deep(.el-radio-button__inner) {
    width: 100%;
    text-align: center;
    padding: 10px 8px;
  }
}

@media screen and (max-width: 480px) {
  .page-title {
    font-size: 1.9rem;
  }

  .card-content {
    padding: 18px;
  }

  .title-wrapper {
    flex-direction: column;
  }

  .blog-title {
    font-size: 18px;
  }

  .post-stats,
  .card-meta-row {
    align-items: flex-start;
  }

  .stat-item-link {
    margin-left: 0;
  }
}
</style>
