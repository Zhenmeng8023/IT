<template>
  <div class="collection-container">
    <nav class="navbar" :class="{ 'navbar-scrolled': scrolled }">
      <div class="navbar-content">
        <div class="logo" @click="goBack">
          <span class="logo-icon">●</span>
          <span class="logo-text">ITSpace</span>
        </div>
        <div class="nav-actions">
          <el-button type="text" class="back-btn" @click="goBack">
            <i class="el-icon-arrow-left"></i> 返回个人主页
          </el-button>
        </div>
      </div>
    </nav>

    <div class="main-content">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-kicker">Curated Reading Shelf</span>
          <h1 class="page-title">我的收藏</h1>
          <p class="page-subtitle">把值得反复阅读的内容集中归档，方便下一次继续深入。</p>
        </div>
        <div class="hero-metrics">
          <div class="metric-card">
            <span class="metric-label">收藏文章</span>
            <strong class="metric-value">{{ totalCount }}</strong>
          </div>
          <div class="metric-card">
            <span class="metric-label">当前状态</span>
            <strong class="metric-value">{{ loading ? '同步中' : '已同步' }}</strong>
          </div>
        </div>
      </section>

      <section class="shelf-panel">
        <div class="section-toolbar">
          <div>
            <span class="section-tag">Saved Articles</span>
            <h2>收藏列表</h2>
          </div>
          <el-button type="primary" class="browse-btn" @click="goToBlog">
            <i class="el-icon-view"></i> 继续逛博客
          </el-button>
        </div>

        <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
          <div v-if="!loading && collectionList.length > 0" class="collection-grid">
            <article
              v-for="(item, index) in collectionList"
              :key="item.id"
              class="collection-card"
            >
              <div class="card-top">
                <span class="card-index">0{{ index + 1 }}</span>
                <div class="card-time">
                  <i class="el-icon-time"></i>
                  {{ formatDate(item.createTime) }}
                </div>
              </div>

              <div class="article-info" @click="goToDetail(item)">
                <h3 class="article-title">{{ item.title }}</h3>
                <div class="article-meta">
                  <div class="author-info">
                    <el-avatar :size="36" :src="item.authorAvatar" class="author-avatar"></el-avatar>
                    <div class="author-copy">
                      <span class="author-caption">作者</span>
                      <span class="author-name">{{ item.author }}</span>
                    </div>
                  </div>
                  <span class="status-pill">已收藏</span>
                </div>
                <p class="article-excerpt" v-if="item.excerpt">{{ item.excerpt }}</p>
                <div class="article-link">
                  查看文章
                  <i class="el-icon-right"></i>
                </div>
              </div>

              <div class="card-actions">
                <el-button
                  type="danger"
                  plain
                  size="small"
                  icon="el-icon-star-off"
                  @click="removeFromCollection(item)"
                  :loading="item.deleting"
                  class="uncollect-btn"
                >取消收藏</el-button>
              </div>
            </article>
          </div>

          <div v-if="!loading && collectionList.length === 0" class="empty-state">
            <div class="empty-icon">
              <i class="el-icon-star-off"></i>
            </div>
            <h3 class="empty-title">暂无收藏</h3>
            <p class="empty-desc">你还没有收藏任何文章，快去浏览博客吧。</p>
            <el-button type="primary" @click="goToBlog" class="empty-btn">
              <i class="el-icon-view"></i> 浏览博客
            </el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
  
  <script>
  import { 
    GetCollectsByUser,
    CancelCollectBlog,
    GetCurrentUser,
    GetBlogById
  } from '@/api/index.js'
  
  export default {
    layout: 'default',
    data() {
      return {
        scrolled: false,
        collectionList: [],
        loading: false,
        userId: null,
        totalCount: 0
      }
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll)
    },
    beforeDestroy() {
      window.removeEventListener('scroll', this.handleScroll)
    },
    created() {
      this.fetchCurrentUser();
    },
    methods: {
      handleScroll() {
        this.scrolled = window.scrollY > 50
      },
  
      // 获取当前用户信息
      async fetchCurrentUser() {
        try {
          const userResponse = await GetCurrentUser();
          console.log('获取当前用户信息:', userResponse);
          if (userResponse.data && userResponse.data.id) {
            this.userId = userResponse.data.id;
            await this.fetchCollections();
          } else if (userResponse.data && userResponse.data.data && userResponse.data.data.id) {
            this.userId = userResponse.data.data.id;
            await this.fetchCollections();
          } else {
            console.error('获取用户ID失败:', userResponse);
            this.$message.error('获取用户信息失败，请重新登录');
          }
        } catch (error) {
          console.error('获取当前用户失败:', error);
          this.$message.error('获取用户信息失败，请重新登录');
        }
      },

      // 获取收藏列表
      async fetchCollections() {
        if (!this.userId) {
          console.error('用户ID为空，无法获取收藏列表');
          return;
        }
        
        this.loading = true;
        try {
          const response = await GetCollectsByUser(this.userId)
          console.log('获取收藏列表成功:', response);
          
          let collections = [];
          
          // 更全面的响应处理
          if (response.data && response.data.code === 0) {
            collections = response.data.data.list || response.data.data || [];
          } else if (response.data && response.data.code === 200) {
            collections = response.data.data || [];
          } else if (Array.isArray(response.data)) {
            collections = response.data;
          } else if (Array.isArray(response)) {
            collections = response;
          } else if (response.data && typeof response.data === 'object') {
            // 处理可能的对象结构
            collections = [response.data];
          }
          
          console.log('处理后的收藏列表:', collections);

          // 过滤出博客类型的收藏
          const blogCollections = collections.filter(item => {
            console.log('处理前的收藏项:', item);
            return item.targetType === 'blog';
          });
          

          // 为每个收藏项获取博客详情
          const collectionWithDetails = await Promise.all(
            blogCollections.map(async (item) => {
              try {
                // 根据targetId获取博客详情
                const blogResponse = await GetBlogById(item.targetId);
                console.log('获取博客详情成功:', blogResponse);
                
                let blogData = {};
                if (blogResponse.data && blogResponse.data.code === 0) {
                  blogData = blogResponse.data.data || {};
                } else if (blogResponse.data && blogResponse.data.code === 200) {
                  blogData = blogResponse.data.data || {};
                } else if (blogResponse.data) {
                  blogData = blogResponse.data;
                }
                
                console.log('博客详情数据:', blogData);

                // 定义移除HTML标签的辅助函数
                const removeHtmlTags = (str) => {
                  if (!str) return '';
                  return str.replace(/<[^>]*>/g, '');
                };
                
                // 解析可能的JSON字符串
                const parseIfJson = (str) => {
                  if (!str || typeof str !== 'string') return str;
                  try {
                    return JSON.parse(str);
                  } catch (e) {
                    return str;
                  }
                };
                
                // 提取作者信息
                let authorName = '未知作者';
                let authorAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
                
                if (blogData.author) {
                  const authorData = parseIfJson(blogData.author);
                  if (typeof authorData === 'object' && authorData !== null) {
                    authorName = authorData.displayName || authorData.nickname || authorData.username || '未知作者';
                    authorAvatar = authorData.avatar || authorAvatar;
                  } else if (typeof authorData === 'string') {
                    authorName = authorData;
                  }
                } else {
                  authorName = blogData.authorName || blogData.username || '未知作者';
                  authorAvatar = blogData.authorAvatar || blogData.avatar || authorAvatar;
                }
                
                // 构建完整的收藏项信息
                return {
                  id: item.id,
                  targetId: item.targetId,
                  title: blogData.title || '无标题',
                  excerpt: removeHtmlTags(blogData.excerpt || blogData.summary || blogData.content?.substring(0, 120) + '...' || '...'),
                  author: authorName,
                  authorAvatar: authorAvatar,
                  createTime: item.createdAt || item.createTime || blogData.createdAt || blogData.createTime,
                  deleting: false
                };
              } catch (error) {
                console.error(`获取博客详情失败 (ID: ${item.targetId}):`, error);
                // 如果获取失败，返回基本信息
                return {
                  id: item.id,
                  targetId: item.targetId,
                  title: '获取失败',
                  excerpt: '无法加载博客详情',
                  author: '未知作者',
                  authorAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
                  createTime: item.createdAt || item.createTime,
                  deleting: false
                };
              }
            })
          );

          this.collectionList = collectionWithDetails;
  
          this.totalCount = this.collectionList.length;
  
        } catch (error) {
          console.error('获取收藏列表失败:', error);
          this.$message.error('获取收藏列表失败，请重试');
          this.collectionList = [];
        } finally {
          this.loading = false;
        }
      },
  
      // 取消收藏
      async removeFromCollection(item) {
        try {
          this.$set(item, 'deleting', true);
  
          await this.$confirm(`确定要取消收藏《${item.title}》吗？`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
            center: true,
            roundButton: true
          });
  
          const collectId = item.id;
          const response = await CancelCollectBlog(collectId);
          console.log('取消收藏成功:', response);
  
          const index = this.collectionList.findIndex(c => c.id === item.id);
          if (index !== -1) {
            this.collectionList.splice(index, 1);
            this.totalCount = this.collectionList.length;
          }
  
          this.$message({
            type: 'success',
            message: '取消收藏成功',
            duration: 2000,
            showClose: true
          });
  
        } catch (error) {
          if (error !== 'cancel') {
            console.error('取消收藏失败:', error);
            this.$message.error('取消收藏失败，请重试');
          }
        } finally {
          this.$set(item, 'deleting', false);
        }
      },
  
      // 跳转到博客详情
      goToDetail(item) {
        this.$router.push(`/blog/${item.targetId}`);
      },
  
      // 跳转到博客列表页
      goToBlog() {
        this.$router.push('/blog');
      },
  
      // 返回上一页
      goBack() {
        this.$router.push('/user');
      },
  
      // 格式化日期
      formatDate(date) {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      }
    }
  }
  </script>
  
<style scoped>
.collection-container {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.2), transparent 25%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.12), transparent 20%),
    linear-gradient(135deg, #060914 0%, #0d1321 45%, #151d2d 100%);
  color: #ffffff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(6, 9, 20, 0.78);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.navbar-scrolled {
  box-shadow: 0 12px 28px rgba(2, 6, 23, 0.28);
}

.navbar-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 14px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.logo-icon {
  font-size: 28px;
  color: #6ee7ff;
  line-height: 1;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #7dd3fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.back-btn {
  color: rgba(214, 231, 255, 0.72);
  font-size: 14px;
}

.back-btn:hover {
  color: #6ee7ff;
}

.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 34px 28px 56px;
}

.hero-panel,
.shelf-panel {
  background: rgba(12, 18, 32, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24px 50px rgba(2, 6, 23, 0.35);
  backdrop-filter: blur(20px);
}

.hero-panel {
  border-radius: 30px;
  padding: 30px;
  margin-bottom: 26px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.8fr);
  gap: 24px;
  position: relative;
  overflow: hidden;
}

.hero-panel::after {
  content: '';
  position: absolute;
  width: 260px;
  height: 260px;
  right: -80px;
  bottom: -120px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.25), transparent 68%);
  pointer-events: none;
}

.hero-kicker,
.section-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(110, 231, 255, 0.12);
  border: 1px solid rgba(110, 231, 255, 0.18);
  color: #9eeaf9;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 50px);
  line-height: 1.05;
}

.page-subtitle {
  margin: 0;
  max-width: 620px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(214, 231, 255, 0.74);
}

.hero-metrics {
  display: grid;
  gap: 14px;
}

.metric-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.metric-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: rgba(214, 231, 255, 0.58);
}

.metric-value {
  font-size: 22px;
  color: #ffffff;
}

.shelf-panel {
  border-radius: 30px;
  padding: 28px;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.section-toolbar h2 {
  margin: 14px 0 0;
  font-size: 24px;
}

.browse-btn {
  border: none;
  border-radius: 999px;
  padding: 12px 22px;
  background: linear-gradient(135deg, #409eff, #60a5fa);
  box-shadow: 0 14px 24px rgba(64, 158, 255, 0.22);
}

.loading-container {
  min-height: 360px;
}

.collection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.collection-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  border-radius: 26px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.07);
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
}

.collection-card:hover {
  transform: translateY(-5px);
  border-color: rgba(110, 231, 255, 0.25);
  box-shadow: 0 18px 32px rgba(2, 6, 23, 0.28);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-index,
.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.card-index {
  min-width: 46px;
  height: 32px;
  padding: 0 12px;
  background: rgba(64, 158, 255, 0.14);
  color: #9eeaf9;
  border: 1px solid rgba(64, 158, 255, 0.18);
}

.card-time {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(214, 231, 255, 0.58);
  font-size: 12px;
}

.article-info {
  cursor: pointer;
}

.article-title {
  margin: 0 0 16px;
  font-size: 22px;
  line-height: 1.4;
  color: #ffffff;
  transition: color 0.25s ease;
}

.article-info:hover .article-title {
  color: #7dd3fc;
}

.article-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  border: 2px solid rgba(125, 211, 252, 0.26);
}

.author-copy {
  display: flex;
  flex-direction: column;
}

.author-caption {
  font-size: 11px;
  color: rgba(214, 231, 255, 0.48);
}

.author-name {
  font-size: 14px;
  font-weight: 600;
  color: #e8f3ff;
}

.status-pill {
  padding: 8px 12px;
  color: #a7f3d0;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.18);
}

.article-excerpt {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(214, 231, 255, 0.7);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #7dd3fc;
  font-size: 13px;
  font-weight: 600;
}

.article-link i {
  transition: transform 0.25s ease;
}

.article-info:hover .article-link i {
  transform: translateX(4px);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.uncollect-btn {
  border-radius: 999px;
  padding: 10px 18px;
  background: rgba(245, 108, 108, 0.08);
  border-color: rgba(245, 108, 108, 0.24);
  color: #fca5a5;
}

.uncollect-btn:hover {
  background: rgba(245, 108, 108, 0.18);
  border-color: rgba(245, 108, 108, 0.34);
  color: #ffffff;
}

.empty-state {
  max-width: 520px;
  margin: 18px auto 0;
  padding: 72px 24px;
  text-align: center;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed rgba(255, 255, 255, 0.12);
}

.empty-icon {
  font-size: 72px;
  color: rgba(125, 211, 252, 0.38);
  margin-bottom: 20px;
}

.empty-title {
  margin: 0 0 10px;
  font-size: 24px;
}

.empty-desc {
  margin: 0 0 28px;
  color: rgba(214, 231, 255, 0.7);
}

.empty-btn {
  border: none;
  border-radius: 999px;
  padding: 12px 28px;
  background: linear-gradient(135deg, #409eff, #60a5fa);
  box-shadow: 0 14px 24px rgba(64, 158, 255, 0.22);
}

@media screen and (max-width: 900px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 768px) {
  .navbar-content,
  .main-content {
    padding-left: 18px;
    padding-right: 18px;
  }

  .hero-panel,
  .shelf-panel {
    padding: 22px;
  }

  .section-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .collection-grid {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 480px) {
  .card-actions {
    justify-content: stretch;
  }

  .uncollect-btn,
  .browse-btn {
    width: 100%;
  }
}
</style>
