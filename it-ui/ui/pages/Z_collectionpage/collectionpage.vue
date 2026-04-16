<template>
  <div class="collection-page">
    <section class="page-hero">
      <div>
        <span class="hero-badge">Curated Reading Shelf</span>
        <h1 class="page-title">我的收藏</h1>
        <p class="page-subtitle">把值得反复阅读的内容集中归档，方便下次继续深入查看。</p>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <span class="stat-label">收藏文章</span>
          <strong class="stat-value">{{ totalCount }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">当前状态</span>
          <strong class="stat-value">{{ loading ? '同步中' : '已同步' }}</strong>
        </div>
      </div>
    </section>

    <section class="content-panel">
      <div class="panel-header">
        <div>
          <span class="panel-tag">Saved Articles</span>
          <h2>收藏列表</h2>
        </div>
        <div class="panel-actions">
          <el-button plain @click="goBack">返回个人主页</el-button>
          <el-button type="primary" @click="goToBlog">继续逛博客</el-button>
        </div>
      </div>

      <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
        <div v-if="!loading && collectionList.length > 0" class="collection-grid">
          <article v-for="(item, index) in collectionList" :key="item.id" class="collection-card">
            <div class="card-top">
              <span class="card-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="card-time"><i class="el-icon-time"></i>{{ formatDate(item.createTime) }}</span>
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
              <div class="article-link">查看文章 <i class="el-icon-right"></i></div>
            </div>

            <div class="card-actions">
              <el-button
                type="danger"
                plain
                size="small"
                icon="el-icon-star-off"
                @click="removeFromCollection(item)"
                :loading="item.deleting"
              >取消收藏</el-button>
            </div>
          </article>
        </div>

        <div v-if="!loading && collectionList.length === 0" class="empty-state">
          <div class="empty-icon"><i class="el-icon-star-off"></i></div>
          <h3 class="empty-title">暂无收藏</h3>
          <p class="empty-desc">你还没有收藏任何文章，快去浏览博客吧。</p>
          <el-button type="primary" @click="goToBlog" class="empty-btn">浏览博客</el-button>
        </div>
      </div>
    </section>
  </div>
</template>
  
  <script>
  import { 
    GetCollectsByUser,
    CancelCollectBlog,
    GetCurrentUser,
    GetBlogById
  } from '@/api/index.js'
  import { pickAvatarUrl } from '@/utils/avatar'
  
  export default {
    layout: 'default',
    data() {
      return {
        collectionList: [],
        loading: false,
        userId: null,
        totalCount: 0
      }
    },
    created() {
      this.fetchCurrentUser();
    },
    methods: {
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
                    authorAvatar = pickAvatarUrl(authorData.avatarUrl, authorData.avatar, authorAvatar);
                  } else if (typeof authorData === 'string') {
                    authorName = authorData;
                  }
                } else {
                  authorName = blogData.authorName || blogData.username || '未知作者';
                  authorAvatar = pickAvatarUrl(blogData.authorAvatarUrl, blogData.authorAvatar, blogData.avatarUrl, blogData.avatar, authorAvatar);
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
.collection-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 0 48px;
}

.page-hero,
.content-panel {
  border-radius: var(--it-radius-card-lg);
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.page-hero {
  padding: 30px 32px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(240px, 360px);
  gap: 20px;
  margin-bottom: 24px;
}

.hero-badge,
.panel-tag {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
  text-transform: uppercase;
}

.page-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 44px);
  line-height: 1.08;
  color: var(--it-text);
}

.page-subtitle {
  max-width: 620px;
  margin: 0;
  color: var(--it-text-muted);
  line-height: 1.75;
}

.hero-stats {
  display: grid;
  gap: 14px;
}

.stat-card {
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  padding: 18px 20px;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--it-text-muted);
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  color: var(--it-text);
}

.content-panel {
  padding: 24px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 12px 0 0;
  color: var(--it-text);
  font-size: 28px;
}

.panel-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.collection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 18px;
}

.collection-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 280px;
  padding: 18px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}

.collection-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--it-shadow-hover);
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border));
}

.card-top,
.article-meta,
.card-actions {
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
  min-width: 52px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 700;
}

.card-time,
.author-caption {
  color: var(--it-text-light);
  font-size: 12px;
}

.article-info {
  flex: 1;
  cursor: pointer;
}

.article-title {
  margin: 0 0 14px;
  color: var(--it-text);
  font-size: 22px;
  line-height: 1.35;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name,
.article-link {
  color: var(--it-accent);
  font-weight: 600;
}

.article-excerpt {
  margin: 16px 0;
  color: var(--it-text-muted);
  line-height: 1.75;
}

.article-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.empty-state {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  border-radius: var(--it-radius-card);
  border: 1px dashed var(--it-border-strong);
  background: color-mix(in srgb, var(--it-surface-solid) 86%, transparent);
}

.empty-icon {
  width: 68px;
  height: 68px;
  border-radius: 24px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.empty-title {
  margin: 0;
  font-size: 24px;
  color: var(--it-text);
}

.empty-desc {
  margin: 0;
  color: var(--it-text-muted);
}

@media (max-width: 900px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .panel-header {
    flex-direction: column;
  }
}
</style>
