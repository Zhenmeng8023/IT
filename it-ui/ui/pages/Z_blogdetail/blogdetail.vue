<template>
  <div class="blog-detail-container">
    <!-- ========== 文章头部卡片 ========== -->
    <el-card class="blog-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="detailLoading">
      <!-- 博客标题 -->
      <h1 class="blog-title">{{ blog.title }}</h1>

      <!-- 作者信息及互动按钮区 -->
      <div class="blog-meta">
        <!-- 作者头像 -->
        <el-avatar :size="50" :src="blog.avatar" @click="goToAuthor" style="cursor: pointer;"></el-avatar>
        <!-- 作者名称 -->
        <span class="author-name" @click="goToAuthor" style="cursor: pointer;">{{ blog.author }}</span>
        <!-- 发布日期 -->
        <span class="publish-date">发布于 {{ blog.publishDate }}</span>

        <!-- 点赞 & 收藏按钮组 -->
        <div class="action-buttons">
          <!-- 点赞按钮 -->
          <el-button
            :type="blog.isLiked ? 'primary' : 'default'"
            size="small"
            :icon="blog.isLiked ? 'el-icon-sunny' : 'el-icon-sunrise'"
            @click="handleLike"
            circle
            :class="{ 'liked-button': blog.isLiked }"
            :loading="likeLoading"
          ></el-button>
          <span class="like-count">{{ blog.likeCount }}</span>

          <!-- 收藏按钮 -->
          <el-button
            :type="blog.isCollected ? 'warning' : 'default'"
            size="small"
            :icon="blog.isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"
            @click="handleCollect"
            circle
            :loading="collectLoading"
          ></el-button>
          <span class="collect-count">{{ blog.collectCount }}</span> 
        </div>
      </div>
    </el-card>

    <!-- ========== 文章正文卡片 ========== -->
    <el-card class="blog-content" :body-style="{ padding: '30px' }" shadow="never">
      <div class="content-body" v-html="blog.content"></div>
    </el-card>

    <!-- ========== 评论区卡片 ========== -->
    <el-card class="comment-section" shadow="hover" v-loading="commentLoading">
      <!-- 卡片头部：显示评论总数 -->
      <div slot="header" class="comment-header">
        <span>评论（{{ totalComments }}）</span>
      </div>

      <!-- 评论列表（支持多级回复） -->
      <div class="comment-list">
        <!-- 遍历顶级评论 -->
        <div v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
          <!-- 顶级评论 -->
          <div class="comment-item">
            <el-avatar :size="40" :src="comment.avatar"></el-avatar>
            <div class="comment-content">
              <div class="comment-meta">
                <span class="comment-author">{{ comment.nickname }}</span>
                <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                <span v-if="comment.isAuthor" class="author-badge">作者</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-actions">
                <el-button type="text" size="small" @click="showReplyInput(comment)">
                  <i class="el-icon-chat-line-round"></i> 回复
                </el-button>
              </div>
            </div>
          </div>

          <!-- 回复输入框（针对顶级评论） -->
          <div v-if="replyTarget && replyTarget.id === comment.id && !replyTarget.parentId" class="reply-input-wrapper">
            <el-input
              type="textarea"
              :rows="2"
              :placeholder="'回复 @' + comment.nickname"
              v-model="replyContent"
              resize="none"
            ></el-input>
            <div class="reply-actions">
              <el-button size="small" @click="cancelReply">取消</el-button>
              <el-button type="primary" size="small" @click="submitReply(comment)" :loading="replySubmitting">
                提交回复
              </el-button>
            </div>
          </div>

          <!-- 该顶级评论下的回复列表 -->
          <div v-if="comment.replies && comment.replies.length" class="replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <el-avatar :size="30" :src="reply.avatar"></el-avatar>
              <div class="reply-content">
                <div class="comment-meta">
                  <span class="comment-author">{{ reply.nickname }}</span>
                  <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                  <span v-if="reply.isAuthor" class="author-badge">作者</span>
                  <span v-if="reply.replyTo" class="reply-to">回复 @{{ reply.replyTo }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button type="text" size="small" @click="showReplyInput(reply, comment)">
                    <i class="el-icon-chat-line-round"></i> 回复
                  </el-button>
                </div>
              </div>

              <!-- 针对回复的回复输入框 -->
              <div v-if="replyTarget && replyTarget.id === reply.id" class="reply-input-wrapper nested">
                <el-input
                  type="textarea"
                  :rows="2"
                  :placeholder="'回复 @' + reply.nickname"
                  v-model="replyContent"
                  resize="none"
                ></el-input>
                <div class="reply-actions">
                  <el-button size="small" @click="cancelReply">取消</el-button>
                  <el-button type="primary" size="small" @click="submitReply(comment, reply)" :loading="replySubmitting">
                    提交回复
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 没有评论时显示 -->
        <div v-if="!topLevelComments.length" class="no-comment">
          暂无评论，快来抢沙发吧～
        </div>
      </div>

      <!-- 发表评论输入区（顶级评论） -->
      <div class="comment-input-area">
        <el-input
          type="textarea"
          :rows="3"
          placeholder="写下你的评论..."
          v-model="newComment"
          resize="none"
        ></el-input>
        <div class="comment-submit">
          <el-button type="primary" @click="submitTopLevelComment" :disabled="!newComment.trim()" :loading="submitting">
            发表评论
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ========== 回到顶部按钮 ========== -->
    <el-backtop target=".blog-detail-container" :bottom="100" :right="40">
      <div class="backtop-inner">
        <i class="el-icon-arrow-up"></i>
        <span>顶部</span>
      </div>
    </el-backtop>
  </div>
</template>

<script>
import { GetCurrentUser, GetBlogById, CheckUserLiked, DeleteLike, AddLike, CollectBlog, CancelCollectBlog, AddComment, ReplyComment, GetCommentsByPost, IsCollected } from '@/api/index'

export default {
  name: 'BlogDetail',
  data() {
    return {
      // ---------- 博客详情数据 ----------
      blog: {
        id: '',
        like_id: '',
        title: '',
        author: '',
        authorId: '',
        avatar: '',
        publishDate: '',
        likeCount: 0,
        collectCount: 0,
        isLiked: false,
        isCollected: false,
        content: null, // 内容省略
      },

      // ---------- 评论列表数据（扁平结构）----------
      comments: [],
      // 新评论（顶级）
      newComment: '',
      submitting: false,
      // 回复相关
      replyTarget: null, // 当前正在回复的评论对象
      replyContent: '',
      replySubmitting: false,
      // 加载状态
      detailLoading: false,
      commentLoading: false,
      likeLoading: false,
      collectLoading: false,
      // 当前正在处理的博客ID，用于处理竞态条件
      currentProcessingBlogId: null,
      // 用户信息
      currentUser: null,
      userId: null,
      username: '',
      userAvatar: ''
    };
  },
  computed: {
    // 计算顶级评论
    topLevelComments() {
      return this.comments.filter(comment => comment.parentId === null).map(comment => {
        return {
          ...comment,
          replies: this.comments.filter(reply => reply.parentId === comment.id)
        };
      });
    },
    // 评论总数
    totalComments() {
      return this.comments.length;
    }
  },
  created() {
    // 从路由参数获取博客id
    const blogId = this.$route.params.id;
    if (blogId) {
      this.blog.id = blogId;
      // 先获取博客详情，再获取用户信息
      this.getBlogDetail(blogId).then(() => {
        // 博客详情获取完成后，再获取用户信息
        this.getCurrentUser();
      });
    }
  },
  methods: {
    /**
     * 格式化时间
     * @param {string} time - 时间字符串
     * @returns {string} 格式化后的时间
     */
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    /**
     * 获取当前用户信息
     */
    async getCurrentUser() {
      try {
        const res = await GetCurrentUser();
        
        // 处理不同的响应格式
        let userData = null;
        
        if (res.data && typeof res.data.code !== 'undefined') {
          // 标准格式 {code: 0, data: {...}}
          if (res.data.code === 0 && res.data.data) {
            userData = res.data.data;
          }
        } else if (res.data && res.data.id) {
          // 直接返回用户对象
          userData = res.data;
        } else if (res.id) {
          // 直接返回用户对象（无data字段）
          userData = res;
        }
        
        if (userData) {
          this.currentUser = userData;
          this.userId = userData.id;
          this.username = userData.username || userData.nickname;
          this.userAvatar = userData.avatar || userData.avatarUrl;
          // 获取用户点赞状态
          this.checkLikeStatus();
          // 检查收藏状态
          this.checkCollectStatus();
        }
      } catch (error) {
        console.error('获取当前用户信息失败', error);
        // 404 表示用户未登录，不是错误
        if (error.response && error.response.status === 404) {
          console.log('用户未登录');
        }
      }
    },

    /**
     * 获取博客详情
     * @param {string} blogId - 博客id
     * @returns {Promise} - 返回Promise
     */
    async getBlogDetail(blogId) {
      // 设置当前正在处理的博客ID，用于处理竞态条件
      this.currentProcessingBlogId = blogId;
      this.detailLoading = true;
      try {
        const res = await GetBlogById(blogId);
        console.log('获取博客详情响应:', res);
        
        // 检查是否是当前正在处理的博客，避免竞态条件
        if (this.currentProcessingBlogId !== blogId) {
          console.log('忽略过期的博客详情响应');
          return;
        }
        
        // 处理不同的响应格式
        let blogData = null;
        
        if (res.data && typeof res.data.code !== 'undefined') {
          // 标准格式 {code: 0, data: {...}}
          if (res.data.code === 0 && res.data.data) {
            blogData = res.data.data;
          }
        } else if (res.data && res.data.id) {
          // 直接返回博客对象在data字段中
          blogData = res.data;
        } else if (res && res.id) {
          // 直接返回博客对象（无data字段）
          blogData = res;
        }
        
        if (blogData) {
          // 处理作者信息（嵌套对象）
          let authorName = '未知作者';
          let authorId = '';
          let authorAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
          
          if (blogData.author) {
            if (typeof blogData.author === 'object') {
              // 作者是对象格式
              authorName = blogData.author.displayName || blogData.author.nickname || blogData.author.username || '未知作者';
              authorId = blogData.author.id || '';
              authorAvatar = blogData.author.avatar || authorAvatar;
            } else {
              // 作者是字符串格式
              authorName = blogData.author;
            }
          }
          
          // 处理发布时间
          let publishDate = '';
          if (blogData.publishTime) {
            publishDate = new Date(blogData.publishTime).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });
          } else if (blogData.createdAt) {
            publishDate = new Date(blogData.createdAt).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });
          }
          
          // 确保所有必要字段都有值
          const blogInfo = {
            id: blogId,
            like_id: '',
            title: blogData.title || '无标题',
            author: authorName, // 确保是字符串
            authorId: authorId,
            avatar: authorAvatar,
            publishDate: publishDate,
            likeCount: blogData.likeCount || 0,
            collectCount: blogData.collectCount || 0,
            isLiked: false,
            isCollected: false,
            content: blogData.content || ''
          };
          console.log('处理后的博客数据:', blogInfo);
          // 确保blog对象被正确更新
          this.$set(this, 'blog', blogInfo);
          // 获取评论列表
          await this.getComments();
        }
      } catch (error) {
        console.error('获取博客详情失败', error);
        this.$message.error('获取博客详情失败');
      } finally {
        this.detailLoading = false;
      }
    },

    /**
     * 获取评论列表
     */
    async getComments() {
      if (!this.blog.id) return;
      
      // 保存当前博客ID，用于后续检查
      const currentBlogId = this.blog.id;
      
      this.commentLoading = true;
      try {
        const res = await GetCommentsByPost(currentBlogId);
        console.log('获取评论列表响应:', res);
        
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的评论列表响应');
          return;
        }
        
        // 后端直接返回评论列表
        if (Array.isArray(res.data)) {
          this.comments = res.data;
        }
      } catch (error) {
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的评论列表错误');
          return;
        }
        
        console.error('获取评论列表失败', error);
      } finally {
        this.commentLoading = false;
      }
    },

    /**
     * 检查收藏状态
     */
    async checkCollectStatus() {
      if (!this.userId || !this.blog.id) return;
      
      // 保存当前博客ID，用于后续检查
      const currentBlogId = this.blog.id;
      
      try {
        const res = await IsCollected(this.userId, 'blog', currentBlogId);
        console.log('检查收藏状态响应:', res);
        
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的收藏状态响应');
          return;
        }
        
        // 后端返回收藏记录对象
        if (res.data && res.data.id) {
          this.blog.isCollected = true;
          this.blog.collect_id = res.data.id; // 保存收藏记录ID，用于取消收藏
          console.log('用户已收藏，收藏记录ID:', res.data.id);
        } else {
          this.blog.isCollected = false;
          this.blog.collect_id = '';
        }
      } catch (error) {
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的收藏状态错误');
          return;
        }
        
        // 404 表示没有收藏记录，不是错误
        if (error.response && error.response.status !== 404) {
          console.error('检查收藏状态失败', error);
        }
        // 未收藏状态
        this.blog.isCollected = false;
        this.blog.collect_id = '';
      }
    },

    /**
     * 检查用户点赞状态
     */
    async checkLikeStatus() {
      if (!this.userId || !this.blog.id) return;
      
      // 保存当前博客ID，用于后续检查
      const currentBlogId = this.blog.id;
      
      try {
        const res = await CheckUserLiked(this.userId, 'blog', currentBlogId);
        console.log('检查点赞状态响应:', res);
        
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的点赞状态响应');
          return;
        }
        
        // 后端直接返回LikeRecord对象
        if (res.data && res.data.id) {
          this.blog.isLiked = true;
          this.blog.like_id = res.data.id;
          console.log('用户已点赞，点赞记录ID:', res.data.id);
        } else {
          // 未点赞状态
          this.blog.isLiked = false;
          this.blog.like_id = '';
        }
      } catch (error) {
        // 检查是否是当前博客的响应，避免竞态条件
        if (this.blog.id !== currentBlogId) {
          console.log('忽略过期的点赞状态错误');
          return;
        }
        
        // 404 表示没有点赞记录，不是错误
        if (error.response && error.response.status === 404) {
          console.log('用户未点赞');
        } else if (error.response && error.response.status !== 404) {
          console.error('检查点赞状态失败', error);
        }
        // 未点赞状态
        this.blog.isLiked = false;
        this.blog.like_id = '';
      }
    },

    /**
     * 处理点赞/取消点赞
     */
    async handleLike() {
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.likeLoading = true;
      try {
        if (this.blog.isLiked) {
          // 取消点赞
          if (this.blog.like_id) {
            await DeleteLike(this.blog.like_id);
            this.blog.isLiked = false;
            this.blog.likeCount--;
            this.blog.like_id = '';
            this.$message.success('取消点赞成功');
          }
        } else {
          // 点赞
          const likeData = {
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          };
          console.log('发送点赞请求:', likeData);
          const res = await AddLike(likeData);
          console.log('点赞响应:', res);
          // 后端直接返回LikeRecord对象
          if (res.data && res.data.id) {
            this.blog.isLiked = true;
            this.blog.likeCount++;
            this.blog.like_id = res.data.id;
            console.log('点赞成功，点赞记录ID:', res.data.id);
            this.$message.success('点赞成功');
          }
        }
      } catch (error) {
        console.error('处理点赞失败', error);
        this.$message.error('操作失败，请稍后重试');
      } finally {
        this.likeLoading = false;
      }
    },

    /**
     * 处理收藏/取消收藏
     */
    async handleCollect() {
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.collectLoading = true;
      try {
        if (this.blog.isCollected) {
          // 取消收藏
          if (this.blog.collect_id) {
            await CancelCollectBlog(this.blog.collect_id);
            this.blog.isCollected = false;
            this.blog.collectCount--;
            this.blog.collect_id = '';
            this.$message.success('取消收藏成功');
          }
        } else {
          // 收藏
          const collectData = {
            userId: this.userId,
            targetId: this.blog.id,
            targetType: 'blog'
          };
          console.log('发送收藏请求:', collectData);
          const res = await CollectBlog(collectData);
          console.log('收藏响应:', res);
          // 后端返回收藏记录对象
          if (res.data && res.data.id) {
            this.blog.isCollected = true;
            this.blog.collectCount++;
            this.blog.collect_id = res.data.id; // 保存收藏记录ID，用于取消收藏
            console.log('收藏成功，收藏记录ID:', res.data.id);
            this.$message.success('收藏成功');
          }
        }
      } catch (error) {
        console.error('处理收藏失败', error);
        this.$message.error('操作失败，请稍后重试');
      } finally {
        this.collectLoading = false;
      }
    },

    /**
     * 跳转到作者详情页
     */
    goToAuthor() {
      if (this.blog.authorId) {
        this.$router.push(`/user/${this.blog.authorId}`);
      }
    },

    /**
     * 显示回复输入框
     * @param {Object} comment - 要回复的评论对象
     * @param {Object} parentComment - 父评论（可选）
     */
    showReplyInput(comment, parentComment = null) {
      this.replyTarget = comment;
      this.replyContent = '';
    },

    /**
     * 取消回复
     */
    cancelReply() {
      this.replyTarget = null;
      this.replyContent = '';
    },

    /**
     * 提交顶级评论
     */
    async submitTopLevelComment() {
      if (!this.newComment.trim()) return;
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.submitting = true;
      try {
        const res = await AddComment({
          content: this.newComment,
          parentCommentId: null,
          postId: this.blog.id,
          authorId: this.userId
        });
        console.log('发表评论响应:', res);
        // 后端直接返回Comment对象
        if (res.data && res.data.id) {
          this.comments.push(res.data);
          this.newComment = '';
          this.$message.success('评论发表成功');
        }
      } catch (error) {
        console.error('发表评论出错', error);
        this.$message.error('网络错误，请稍后重试');
      } finally {
        this.submitting = false;
      }
    },

    /**
     * 提交回复
     * @param {Object} parentComment - 父评论（顶级评论）
     * @param {Object} replyToComment - 被回复的评论（可选，用于嵌套回复）
     */
    async submitReply(parentComment, replyToComment = null) {
      if (!this.replyContent.trim()) return;
      if (!this.userId) {
        this.$message.warning('请先登录');
        return;
      }
      
      this.replySubmitting = true;
      try {
        // 由于后端没有专门的回复接口，我们直接使用AddComment接口
        const res = await AddComment({
          content: this.replyContent,
          parentCommentId: replyToComment ? replyToComment.id : parentComment.id,
          postId: this.blog.id,
          authorId: this.userId
        });
        console.log('发表回复响应:', res);
        // 后端直接返回Comment对象
        if (res.data && res.data.id) {
          this.comments.push(res.data);
          this.replyTarget = null;
          this.replyContent = '';
          this.$message.success('回复发表成功');
        }
      } catch (error) {
        console.error('发表回复出错', error);
        this.$message.error('网络错误，请稍后重试');
      } finally {
        this.replySubmitting = false;
      }
    },
  },
};
</script>

<style scoped>
/* 整个博客详情容器，限制最大宽度，居中显示 */
.blog-detail-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
  min-height: 120vh; /* 设置足够高度以确保回到顶部按钮出现 */
  position: relative;
}

/* 头部卡片样式 */
.blog-header {
  margin-bottom: 20px;
  border-radius: 8px;
}
.blog-title {
  margin: 0 0 16px 0;
  font-size: 2.2rem;
  color: #303133;
  line-height: 1.3;
}
.blog-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}
.author-name {
  font-size: 1.2rem;
  font-weight: 500;
  color: #409EFF;
}
.publish-date {
  color: #909399;
  font-size: 0.95rem;
}
/* 互动按钮组靠右对齐 */
.action-buttons {
  display: flex;
  align-items: center;
  margin-left: auto;
  gap: 5px;
}
.like-count {
  margin-right: 15px;
  font-size: 14px;
  color: #606266;
}

.collect-count {
margin-right: 0;        /* 可根据需要调整间距 */
font-size: 14px;
color: #606266;
}

/* 点赞按钮高亮样式 */
.liked-button {
  animation: pulse 0.5s ease-in-out;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.3);
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}
.collect-text {
  font-size: 14px;
  color: #606266;
}

/* 正文卡片样式 */
.blog-content {
  border-radius: 8px;
  background-color: #ffffff;
  margin-bottom: 20px;
}
.content-body {
  font-size: 1.1rem;
  line-height: 1.8;
  color: #2c3e50;
}
/* 正文内标题样式 */
.content-body h2 {
  margin: 28px 0 16px;
  font-weight: 500;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}
.content-body p {
  margin: 16px 0;
}
/* 行内代码样式 */
.content-body code {
  background-color: #f8f8f8;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #e96900;
}
/* 代码块样式 */
.content-body pre {
  background-color: #f8f8f8;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  border: 1px solid #eaeefb;
}
.content-body pre code {
  background-color: transparent;
  padding: 0;
  color: #476582;
}

/* 评论区卡片样式 */
.comment-section {
  border-radius: 8px;
  margin-bottom: 20px;
}
.comment-header {
  font-weight: 500;
  font-size: 1.1rem;
}
.comment-list {
  margin-bottom: 20px;
}
/* 单条评论项 */
.comment-item {
  display: flex;
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #f0f2f5;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-content {
  flex: 1;
}
.comment-meta {
  margin-bottom: 6px;
}
.comment-author {
  font-weight: 500;
  color: #409EFF;
  margin-right: 10px;
}
.comment-time {
  color: #909399;
  font-size: 0.85rem;
}
.comment-text {
  color: #2c3e50;
  line-height: 1.6;
}
/* 评论输入区 */
.comment-input-area {
  margin-top: 10px;
}
.comment-submit {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

/* 回到顶部按钮自定义内容 */
.backtop-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background-color: #409EFF;
  color: white;
  border-radius: 50%;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: background-color 0.3s;
}
.backtop-inner:hover {
  background-color: #66b1ff;
}
.backtop-inner i {
  font-size: 20px;
  margin-bottom: 2px;
}

/* 评论线程样式 */
.comment-thread {
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f2f5;
  padding-bottom: 20px;
}
.comment-thread:last-child {
  border-bottom: none;
}

/* 回复列表样式 */
.replies {
  margin-left: 55px; /* 缩进表示层级 */
  margin-top: 15px;
}

.reply-item {
  display: flex;
  gap: 12px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px dashed #f0f2f5;
}
.reply-item:first-child {
  border-top: none;
  padding-top: 0;
}

.reply-item .el-avatar {
  flex-shrink: 0;
}

/* 评论元信息样式 */
.comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 5px;
}

.author-badge {
  font-size: 12px;
  padding: 2px 6px;
  background-color: #ecf5ff;
  color: #409EFF;
  border-radius: 4px;
  font-weight: normal;
}

.reply-to {
  font-size: 12px;
  color: #909399;
}
.reply-to::before {
  content: '@';
  margin-right: 2px;
}

/* 评论操作按钮 */
.comment-actions {
  margin-top: 8px;
}
.comment-actions .el-button {
  padding: 0;
  margin-right: 15px;
  font-size: 13px;
}
.comment-actions .el-button i {
  margin-right: 3px;
}

/* 回复输入框样式 */
.reply-input-wrapper {
  margin-top: 15px;
  margin-left: 55px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 6px;
}
.reply-input-wrapper.nested {
  margin-left: 0; /* 针对回复的回复，已在父级缩进 */
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

/* 无评论提示 */
.no-comment {
  text-align: center;
  padding: 30px;
  color: #909399;
  font-size: 14px;
}

/* 原有样式保留 */
/* ... 所有之前的样式保持不变 ... */
</style>