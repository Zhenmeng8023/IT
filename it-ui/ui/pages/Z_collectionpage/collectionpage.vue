<template>
    <div class="collection-container">
      <!-- ========== 顶部导航栏 ========== -->
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
  
      <!-- ========== 主内容区域 ========== -->
      <div class="main-content">
        <!-- 页面标题 -->
        <div class="page-header">
          <h1 class="page-title">我的收藏</h1>
          <p class="page-subtitle">共 {{ totalCount }} 篇收藏文章</p>
        </div>
  
        <!-- 加载状态 -->
        <div v-loading="loading" element-loading-text="加载中..." class="loading-container">
          <!-- 收藏列表 - 卡片视图 -->
          <div v-if="!loading && collectionList.length > 0" class="collection-grid">
            <el-card 
              v-for="item in collectionList" 
              :key="item.id" 
              class="collection-card"
              shadow="hover"
            >
              <div class="card-content">
                <!-- 文章信息 -->
                <div class="article-info" @click="goToDetail(item)">
                  <h3 class="article-title">{{ item.title }}</h3>
                  <div class="article-meta">
                    <div class="author-info">
                      <el-avatar :size="32" :src="item.authorAvatar" class="author-avatar"></el-avatar>
                      <span class="author-name">{{ item.author }}</span>
                    </div>
                    <div class="article-time">
                      <i class="el-icon-time"></i>
                      {{ formatDate(item.createTime) }}
                    </div>
                  </div>
                  <p class="article-excerpt" v-if="item.excerpt">{{ item.excerpt }}</p>
                </div>
  
                <!-- 操作按钮 -->
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
              </div>
            </el-card>
          </div>
  
          <!-- 空状态 - 美化版 -->
          <div v-if="!loading && collectionList.length === 0" class="empty-state">
            <div class="empty-icon">
              <i class="el-icon-star-off"></i>
            </div>
            <h3 class="empty-title">暂无收藏</h3>
            <p class="empty-desc">你还没有收藏任何文章，快去浏览博客吧</p>
            <el-button type="primary" @click="goToBlog" class="empty-btn">
              <i class="el-icon-view"></i> 浏览博客
            </el-button>
          </div>
        </div>
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
  /* ========== 全局样式 ========== */
  .collection-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
    color: #ffffff;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  }
  
  /* ========== 导航栏 ========== */
  .navbar {
    position: sticky;
    top: 0;
    z-index: 1000;
    background: rgba(10, 10, 10, 0.8);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    transition: all 0.3s ease;
  }
  
  .navbar-scrolled {
    background: rgba(10, 10, 10, 0.95);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  }
  
  .navbar-content {
    max-width: 1400px;
    margin: 0 auto;
    padding: 10px 30px;
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
    color: #409EFF;
    line-height: 1;
  }
  
  .logo-text {
    font-size: 18px;
    font-weight: 600;
    background: linear-gradient(135deg, #ffffff, #409EFF);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .back-btn {
    color: #a0a0a0;
    font-size: 14px;
    transition: all 0.3s ease;
  }
  
  .back-btn:hover {
    color: #409EFF;
    transform: translateX(-3px);
  }
  
  .back-btn i {
    margin-right: 5px;
  }
  
  /* ========== 主内容区域 ========== */
  .main-content {
    max-width: 1200px;
    margin: 40px auto;
    padding: 0 30px;
  }
  
  /* ========== 页面标题 ========== */
  .page-header {
    margin-bottom: 40px;
    text-align: center;
  }
  
  .page-title {
    font-size: 36px;
    font-weight: 600;
    margin: 0 0 10px;
    background: linear-gradient(135deg, #ffffff, #409EFF);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .page-subtitle {
    font-size: 16px;
    color: #a0a0a0;
    margin: 0;
  }
  
  /* ========== 收藏卡片网格 ========== */
  .collection-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 25px;
  }
  
  .collection-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    transition: all 0.3s ease;
    overflow: hidden;
  }
  
  .collection-card:hover {
    transform: translateY(-5px);
    border-color: rgba(64, 158, 255, 0.3);
    box-shadow: 0 20px 30px rgba(0, 0, 0, 0.3);
  }
  
  .card-content {
    padding: 20px;
  }
  
  /* 文章信息区域 - 可点击 */
  .article-info {
    cursor: pointer;
    margin-bottom: 20px;
  }
  
  .article-title {
    font-size: 18px;
    font-weight: 600;
    color: #ffffff;
    margin: 0 0 15px;
    line-height: 1.4;
    transition: color 0.3s ease;
  }
  
  .article-info:hover .article-title {
    color: #409EFF;
  }
  
  /* 文章元信息 */
  .article-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 15px;
  }
  
  .author-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .author-avatar {
    border: 2px solid rgba(64, 158, 255, 0.3);
    transition: border-color 0.3s ease;
  }
  
  .author-avatar:hover {
    border-color: #409EFF;
  }
  
  .author-name {
    font-size: 14px;
    font-weight: 500;
    color: #e0e0e0;
  }
  
  .article-time {
    font-size: 12px;
    color: #a0a0a0;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  
  .article-time i {
    font-size: 13px;
  }
  
  /* 文章摘要 */
  .article-excerpt {
    font-size: 13px;
    color: #a0a0a0;
    line-height: 1.6;
    margin: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  
  /* 卡片操作按钮 */
  .card-actions {
    display: flex;
    justify-content: flex-end;
    padding-top: 15px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
  }
  
  .uncollect-btn {
    border-radius: 20px;
    padding: 8px 16px;
    font-size: 12px;
    background: rgba(245, 108, 108, 0.1);
    border-color: rgba(245, 108, 108, 0.3);
    color: #f56c6c;
    transition: all 0.3s ease;
  }
  
  .uncollect-btn:hover {
    background: rgba(245, 108, 108, 0.2);
    border-color: #f56c6c;
    color: #f56c6c;
    transform: translateY(-2px);
  }
  
  .uncollect-btn i {
    margin-right: 4px;
  }
  
  /* ========== 空状态样式 ========== */
  .empty-state {
    text-align: center;
    padding: 80px 20px;
    background: rgba(255, 255, 255, 0.02);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 30px;
    max-width: 500px;
    margin: 50px auto;
  }
  
  .empty-icon {
    font-size: 80px;
    color: rgba(64, 158, 255, 0.3);
    margin-bottom: 20px;
    animation: pulse 2s infinite;
  }
  
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
      transform: scale(1);
    }
    50% {
      opacity: 0.7;
      transform: scale(1.1);
    }
  }
  
  .empty-title {
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    margin: 0 0 10px;
  }
  
  .empty-desc {
    font-size: 14px;
    color: #a0a0a0;
    margin: 0 0 30px;
  }
  
  .empty-btn {
    background: linear-gradient(135deg, #409EFF, #66b1ff);
    border: none;
    border-radius: 30px;
    padding: 12px 36px;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
  }
  
  .empty-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(64, 158, 255, 0.3);
  }
  
  .empty-btn i {
    margin-right: 6px;
  }
  
  /* ========== 加载状态 ========== */
  .loading-container {
    min-height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  /* ========== 响应式设计 ========== */
  @media screen and (max-width: 768px) {
    .navbar-content {
      padding: 10px 20px;
    }
  
    .main-content {
      padding: 0 20px;
      margin: 30px auto;
    }
  
    .page-title {
      font-size: 28px;
    }
  
    .collection-grid {
      grid-template-columns: 1fr;
      gap: 20px;
    }
  
    .article-meta {
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
    }
  
    .empty-state {
      padding: 60px 20px;
    }
  
    .empty-icon {
      font-size: 60px;
    }
  
    .empty-title {
      font-size: 20px;
    }
  }
  
  @media screen and (max-width: 480px) {
    .card-content {
      padding: 15px;
    }
  
    .article-title {
      font-size: 16px;
    }
  
    .card-actions {
      justify-content: center;
    }
  
    .uncollect-btn {
      width: 100%;
    }
  }
  </style>