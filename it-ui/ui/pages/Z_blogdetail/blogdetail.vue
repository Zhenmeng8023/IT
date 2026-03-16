<template>
  <div class="blog-detail-container">
    <!-- ========== 文章头部卡片 ========== -->
    <el-card class="blog-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="detailLoading">
      <!-- 博客标题 -->
      <h1 class="blog-title">{{ blog.title }}</h1>

      <!-- 作者信息及互动按钮区 -->
      <div class="blog-meta">
        <!-- 作者头像 -->
        <el-avatar :size="50" :src="blog.avatar"></el-avatar>
        <!-- 作者名称 -->
        <span class="author-name">{{ blog.author }}</span>
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
          ></el-button>
          <span class="like-count">{{ blog.likeCount }}</span>

          <!-- 收藏按钮 -->
          <el-button
            :type="blog.isCollected ? 'warning' : 'default'"
            size="small"
            :icon="blog.isCollected ? 'el-icon-star-on' : 'el-icon-star-off'"
            @click="handleCollect"
            circle
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
export default {
  name: 'BlogDetail',
  data() {
    return {
      // ---------- 博客详情数据 ----------
      blog: {
        id: '',
        title: 'Vue 2 组合式 API 实践与思考',
        author: 'Evan You',
        avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
        publishDate: '2025-03-11',
        likeCount: 128,
        collectCount: 24,
        isLiked: false,
        isCollected: false,
        content: `请还没有完成心理测评的同学尽快完成，该心理测评参与情况将作为本年度本科生《心理健康教育》必修课环节成绩评定依据，未参与本次测评的学生该门课程成绩将认定不合格。请尽快完成！[表情]@全体成员 
        内容仅供测试`, // 内容省略
      },
      // ---------- 评论列表数据（扁平结构）----------
      // 测试样例
      comments: [
        {
          id: 1,
          parentId: null, // null 表示顶级评论
          avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
          nickname: '前端小迷妹',
          createTime: '2025-03-11 10:30',
          content: '写的真好，受益匪浅！',
          isAuthor: false, // 是否是作者
        },
        {
          id: 2,
          parentId: null,
          avatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          nickname: '代码猎人',
          createTime: '2025-03-11 11:20',
          content: '组合式 API 确实让逻辑复用更方便了。',
          isAuthor: false,
        },
        {
          id: 3,
          parentId: 1, // 回复给评论1
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
          nickname: '前端小王子',
          createTime: '2025-03-11 11:30',
          content: '感谢支持！欢迎交流更多经验。',
          isAuthor: true, // 这是作者回复
          replyTo: '前端小迷妹', // 回复给谁
        },
        {
          id: 4,
          parentId: 3, // 回复给评论3
          avatar: 'https://cube.elemecdn.com/4/5d/6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s.png',
          nickname: '路人甲',
          createTime: '2025-03-11 12:00',
          content: '同问，有没有更多例子？',
          isAuthor: false,
          replyTo: '前端小王子',
        },
        {
          id: 5,
          parentId: null,
          avatar: 'https://cube.elemecdn.com/5/6f/7g8h9i0j1k2l3m4n5o6p7q8r9s0t.png',
          nickname: '技术小白',
          createTime: '2025-03-11 14:30',
          content: '请问组合式 API 和选项式 API 可以混用吗？',
          isAuthor: false,
        },
        {
          id: 6,
          parentId: 5,
          avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          nickname: 'Evan You',
          createTime: '2025-03-11 15:00',
          content: '在 Vue 2 中需要通过 @vue/composition-api 插件，可以混用，但建议保持一致性。',
          isAuthor: true,
          replyTo: '技术小白',
        },
      ],
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
    };
  },
  computed: {
    /**
     * 计算总评论数
     */
    totalComments() {
      return this.comments.length;
    },

    /**
     * 将扁平评论列表转换为树形结构
     */
    topLevelComments() {
      const commentMap = new Map();
      const topComments = [];

      // 先创建所有评论的映射
      this.comments.forEach(comment => {
        commentMap.set(comment.id, {
          ...comment,
          replies: [] // 初始化回复数组
        });
      });

      // 构建树形结构
      this.comments.forEach(comment => {
        const commentWithReplies = commentMap.get(comment.id);
        if (comment.parentId) {
          // 如果有父评论，添加到父评论的 replies 中
          const parent = commentMap.get(comment.parentId);
          if (parent) {
            parent.replies.push(commentWithReplies);
          } else {
            // 如果父评论不存在（异常情况），当作顶级评论处理
            topComments.push(commentWithReplies);
          }
        } else {
          // 顶级评论
          topComments.push(commentWithReplies);
        }
      });

      // 按时间排序（最新的在前）
      topComments.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
      
      // 对每个顶级评论下的回复也按时间排序
      topComments.forEach(comment => {
        if (comment.replies && comment.replies.length) {
          comment.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime)); // 最早的在前
        }
      });

      return topComments;
    }
  },
  mounted() {
    this.fetchBlogDetail();
    // 评论数据已经在 data 中，所以不需要再获取
  },
  methods: {
    /**
     * 获取博客详情
     */
    async fetchBlogDetail() {
      const blogId = this.$route.params.id;
      if (!blogId) {
        console.warn('未获取到博客ID，使用模拟数据');
        return;
      }
      this.detailLoading = true;
      try {
        const blogData = await this.$axios.get(`/api/blog/${blogId}`); 
        
        if (!blogData) {
          console.error('博客数据为undefined或null');
          this.$message.error('获取博客详情失败：响应数据为空');
          return;
        }
        
        // 更新博客数据
        this.blog = {
          ...this.blog,
          id: blogData.id || blogId,
          title: blogData.title || this.blog.title,
          author: blogData.author ? (blogData.author.nickname || blogData.author.displayName || blogData.author.username) : this.blog.author,
          avatar: blogData.author ? blogData.author.avatar : this.blog.avatar,
          publishDate: blogData.createdAt ? new Date(blogData.createdAt).toLocaleDateString() : this.blog.publishDate,
          likeCount: blogData.likeCount !== undefined ? blogData.likeCount : this.blog.likeCount,
          isLiked: blogData.isLiked !== undefined ? blogData.isLiked : this.blog.isLiked,
          isCollected: blogData.isMarked !== undefined ? blogData.isMarked : this.blog.isCollected,
          collectCount: blogData.collectCount !== undefined ? blogData.collectCount : this.blog.collectCount,
          content: blogData.content || this.blog.content,
        };
      } catch (error) {
        console.error('获取博客详情出错', error);
        if (error.response) {
          this.$message.error(`获取博客详情失败：${error.response.status} - ${error.response.statusText}`);
        } else if (error.request) {
          this.$message.error('网络错误：无法连接到服务器');
        } else {
          this.$message.error('请求错误：' + error.message);
        }
      } finally {
        this.detailLoading = false;
      }
    },

    /**
     * 获取评论列表
     */
    async fetchComments() {
      const blogId = this.$route.params.id;
      if (!blogId) return;
      this.commentLoading = true;
      try {
        const res = await this.$axios.get(`/api/blog/${blogId}/comments`);
        if (res.data.code === 0) {
          this.comments = res.data.data.map(c => ({
            id: c.id,
            parentId: c.parentId || null,
            avatar: c.avatar,
            nickname: c.nickname,
            createTime: c.createTime,
            content: c.content,
            isAuthor: c.isAuthor || false,
            replyTo: c.replyTo || null,
          }));
        } else {
          this.$message.error('获取评论失败：' + res.data.message);
        }
      } catch (error) {
        console.error('获取评论出错', error);
        this.$message.error('网络错误，请稍后重试');
      } finally {
        this.commentLoading = false;
      }
    },

    /**
     * 格式化时间
     */
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
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      }
    },

    /**
     * 点赞/取消点赞
     */
    async handleLike() {
      const oldLiked = this.blog.isLiked;
      const oldCount = this.blog.likeCount;

      this.blog.isLiked = !this.blog.isLiked;
      this.blog.likeCount += this.blog.isLiked ? 1 : -1;
      this.likeLoading = true;

      const blogId = this.$route.params.id;
      try {
        await this.$axios.post(`/api/blog/${blogId}/like`, {
          liked: this.blog.isLiked,
        });
      } catch (error) {
        this.blog.isLiked = oldLiked;
        this.blog.likeCount = oldCount;
        this.$message.error('点赞操作失败，请重试');
        console.error('点赞失败', error);
      } finally {
        this.likeLoading = false;
      }
    },

    /**
     * 收藏/取消收藏
     */
    async handleCollect() {
      const oldCollected = this.blog.isCollected;
      const oldCount = this.blog.collectCount;

      this.blog.isCollected = !this.blog.isCollected;
      this.blog.collectCount += this.blog.isCollected ? 1 : -1;
      this.collectLoading = true;

      const blogId = this.$route.params.id;
      try {
        await this.$axios.post(`/api/blog/${blogId}/collect`, {
          collected: this.blog.isCollected,
        });
        this.$message({
          message: this.blog.isCollected ? '已加入收藏' : '已取消收藏',
          type: 'success',
          duration: 1000,
        });
      } catch (error) {
        this.blog.isCollected = oldCollected;
        this.blog.collectCount = oldCount;
        this.$message.error('收藏操作失败，请重试');
        console.error('收藏失败', error);
      } finally {
        this.collectLoading = false;
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
      
      this.submitting = true;
      try {
        // 模拟新评论数据
        const newComment = {
          id: Date.now(), // 临时ID
          parentId: null,
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', // 默认头像
          nickname: '当前用户',
          createTime: new Date().toISOString(),
          content: this.newComment,
          isAuthor: false,
        };
        
        // 添加到评论列表
        this.comments.push(newComment);
        this.newComment = '';
        this.$message.success('评论发表成功');
        
        // 实际项目中应该调用API
        // const res = await this.$axios.post(`/api/blog/${blogId}/comments`, {
        //   content: this.newComment,
        //   parentId: null,
        // });
        // if (res.data.code === 0) {
        //   this.comments.push(res.data.data);
        //   this.newComment = '';
        //   this.$message.success('评论发表成功');
        // }
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
      
      const targetComment = replyToComment || this.replyTarget;
      this.replySubmitting = true;
      
      try {
        // 构建新回复数据
        const newReply = {
          id: Date.now(), // 临时ID
          parentId: parentComment.id,
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
          nickname: '当前用户',
          createTime: new Date().toISOString(),
          content: this.replyContent,
          isAuthor: false,
          replyTo: targetComment.nickname,
        };
        
        // 添加到评论列表
        this.comments.push(newReply);
        this.replyContent = '';
        this.replyTarget = null;
        this.$message.success('回复成功');
        
        // 实际项目中应该调用API
        // const res = await this.$axios.post(`/api/blog/${blogId}/comments`, {
        //   content: this.replyContent,
        //   parentId: parentComment.id,
        //   replyToId: targetComment.id,
        // });
        // if (res.data.code === 0) {
        //   this.comments.push(res.data.data);
        //   this.replyContent = '';
        //   this.replyTarget = null;
        //   this.$message.success('回复成功');
        // }
      } catch (error) {
        console.error('回复出错', error);
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