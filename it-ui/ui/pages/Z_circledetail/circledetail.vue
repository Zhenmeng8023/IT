<template>
    <div class="post-detail-container">
      <!-- 帖子内容卡片 -->
      <el-card class="post-header" :body-style="{ padding: '20px' }" shadow="hover" v-loading="loading">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <el-avatar :size="50" :src="post.avatar"></el-avatar>
          <span class="author-name">{{ post.author }}</span>
          <span class="publish-date">发布于 {{ post.publishDate }}</span>
        </div>
        <div class="post-content" v-html="post.content"></div>
      </el-card>
  
      <!-- 评论区卡片 -->
      <el-card class="comment-section" shadow="hover">
        <div slot="header" class="comment-header">
          <span>评论（{{ totalComments }}）</span>
          <div class="comment-tools">
            <!-- 排序切换 -->
            <el-radio-group v-model="sortOrder" size="small" @change="handleSortChange">
              <el-radio-button label="desc">最新</el-radio-button>
              <el-radio-button label="asc">最早</el-radio-button>
            </el-radio-group>
            <!-- 只看楼主开关 -->
            <el-switch
              v-model="onlyAuthor"
              active-text="只看楼主"
              inactive-text="全部评论"
              @change="handleFilterChange"
            >
            </el-switch>
          </div>
        </div>
  
        <!-- 全局评论输入框（发表顶级评论） -->
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
  
        <!-- 评论列表 -->
        <div class="comment-list" v-loading="commentLoading">
          <!-- 顶级评论循环 -->
          <div v-for="topComment in topLevelComments" :key="topComment.id" class="comment-thread">
            <!-- 顶级评论 -->
            <div class="comment-item">
              <el-avatar :size="40" :src="topComment.avatar"></el-avatar>
              <div class="comment-content">
                <div class="comment-meta">
                  <span class="comment-author">{{ topComment.nickname }}</span>
                  <span class="comment-time">{{ formatTime(topComment.createTime) }}</span>
                </div>
                <div class="comment-text">{{ topComment.content }}</div>
                <div class="comment-actions">
                  <el-button type="text" size="small" @click="showReplyInput(topComment)">
                    回复
                  </el-button>
                  <!-- 可选点赞 -->
                  <!-- <el-button type="text" size="small">点赞 {{ topComment.likeCount }}</el-button> -->
                </div>
              </div>
            </div>
  
            <!-- 回复输入框（针对顶级评论） -->
            <div v-if="replyTarget && replyTarget.id === topComment.id" class="reply-input-wrapper">
              <el-input
                type="textarea"
                :rows="2"
                :placeholder="'回复 @' + topComment.nickname"
                v-model="replyContent"
                resize="none"
              ></el-input>
              <div class="reply-actions">
                <el-button size="small" @click="cancelReply">取消</el-button>
                <el-button type="primary" size="small" @click="submitReply(topComment)" :loading="replySubmitting">
                  提交回复
                </el-button>
              </div>
            </div>
  
            <!-- 该顶级评论下的回复列表（二级评论） -->
            <div v-if="topComment.children && topComment.children.length" class="replies">
              <div v-for="reply in topComment.children" :key="reply.id" class="reply-item">
                <el-avatar :size="30" :src="reply.avatar"></el-avatar>
                <div class="reply-content">
                  <div class="comment-meta">
                    <span class="comment-author">{{ reply.nickname }}</span>
                    <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                  </div>
                  <div class="comment-text">{{ reply.content }}</div>
                  <div class="comment-actions">
                    <el-button type="text" size="small" @click="showReplyInput(reply, topComment)">
                      回复
                    </el-button>
                  </div>
                </div>
  
                <!-- 针对回复的回复输入框（仍作为该顶级评论的子级） -->
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
                    <el-button type="primary" size="small" @click="submitReply(topComment, reply)" :loading="replySubmitting">
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
      </el-card>
  
      <!-- 回到顶部按钮 -->
      <el-backtop target=".post-detail-container" :bottom="100" :right="40">
        <div class="backtop-inner">
          <i class="el-icon-arrow-up"></i>
          <span>顶部</span>
        </div>
      </el-backtop>
    </div>
  </template>
  
  <script>
//   import { formatDistance } from 'date-fns'; // 可选，用于格式化时间
  
  export default {
    layout: 'blog',
    data() {
      return {
        postId: this.$route.params.id, // 从路由获取帖子ID
        loading: false,
        post: {
          title: '',
          author: '',
          avatar: '',
          publishDate: '',
          content: '',
        },
        // 原始评论列表（扁平）
        rawComments: [
            {
                id: 1,
                parentId: null,
                nickname: '用户A',
                avatar: 'https://example.com/avatar1.jpg',
                createTime: '2023-10-01T12:00:00Z',
                content: '这是一条评论',
                likeCount: 5,
            },
        ],
        commentLoading: false,
        // 排序与过滤
        sortOrder: 'desc', // 'desc' 最新，'asc' 最早
        onlyAuthor: false,
        // 新评论（顶级）
        newComment: '',
        submitting: false,
        // 回复相关
        replyTarget: null, // 当前正在回复的评论对象（顶级或回复）
        replyContent: '',
        replySubmitting: false,
      };
    },
    computed: {
      totalComments() {
        return this.rawComments.length;
      },
      // 根据只看楼主过滤后的评论
      filteredComments() {
        if (!this.onlyAuthor) return this.rawComments;
        // 假设帖子作者名称存储在 post.author，评论作者昵称与之比较
        return this.rawComments.filter(c => c.nickname === this.post.author);
      },
      // 按时间排序后的评论
      sortedComments() {
        const sorted = [...this.filteredComments];
        sorted.sort((a, b) => {
          const timeA = new Date(a.createTime).getTime();
          const timeB = new Date(b.createTime).getTime();
          return this.sortOrder === 'desc' ? timeB - timeA : timeA - timeB;
        });
        return sorted;
      },
      // 构建顶级评论及其 children
      // 构建顶级评论及其 children
      topLevelComments() {
        const topMap = new Map();
        // 使用空对象初始化数组，避免类型推断问题
        const topList = [{}];
        topList.length = 0; // 清空数组但保留正确的类型推断

        // 先找出所有顶级评论（parentId 为 null 或 0）
        this.sortedComments.forEach(comment => {
          if (!comment.parentId) {
            // 创建副本避免修改原始数据
            const topComment = { ...comment, children: [] };
            topList.push(topComment);
            topMap.set(comment.id, topComment);
          }
        });

        // 将非顶级评论（回复）分配到对应的顶级评论下
        this.sortedComments.forEach(comment => {
          if (comment.parentId) {
            const parentComment = topMap.get(comment.parentId);
            if (parentComment && parentComment.children) {
              parentComment.children.push(comment);
            }
          }
        });

        // 对每个顶级评论下的 children 也按时间排序
        topList.forEach(top => {
          if (top.children && top.children.length) {
            top.children.sort((a, b) => {
              const timeA = new Date(a.createTime).getTime();
              const timeB = new Date(b.createTime).getTime();
              return this.sortOrder === 'desc' ? timeB - timeA : timeA - timeB;
            });
          }
        });

        return topList;
      },
    },
    created() {
      this.fetchPostDetail();
      this.fetchComments();
    },
    methods: {
      // 获取帖子详情
      async fetchPostDetail() {
        this.loading = true;
        try {
          const res = await this.$axios.get(`/api/posts/${this.postId}`);
          if (res.data.code === 0) {
            this.post = res.data.data;
          } else {
            this.$message.error('获取帖子失败：' + res.data.message);
          }
        } catch (error) {
          console.error(error);
          this.$message.error('网络错误');
        } finally {
          this.loading = false;
        }
      },
      // 获取评论列表
      async fetchComments() {
        this.commentLoading = true;
        try {
          const res = await this.$axios.get(`/api/posts/${this.postId}/comments`);
          if (res.data.code === 0) {
            // 确保数据是数组
            this.rawComments = Array.isArray(res.data.data) ? res.data.data : [];
          } else {
            this.$message.error('获取评论失败：' + res.data.message);
          }
        } catch (error) {
          console.error(error);
          this.$message.error('网络错误');
        } finally {
          this.commentLoading = false;
        }
      },
      // 格式化时间（可选用 date-fns 或简单截取）
      formatTime(time) {
        if (!time) return '';
        // 简单返回本地字符串，可根据需要优化
        return new Date(time).toLocaleString('zh-CN', { hour12: false });
      },
      // 排序变化
      handleSortChange() {
        // 由于是计算属性自动更新，无需额外操作
      },
      // 只看楼主变化
      handleFilterChange() {
        // 计算属性自动更新
      },
      // 提交顶级评论
      async submitTopLevelComment() {
        if (!this.newComment.trim()) return;
        this.submitting = true;
        try {
          const res = await this.$axios.post(`/api/posts/${this.postId}/comments`, {
            content: this.newComment,
            parentId: null, // 顶级评论
          });
          if (res.data.code === 0) {
            const newComment = res.data.data;
            // 添加到列表，触发视图更新
            this.rawComments.push(newComment);
            this.newComment = '';
            this.$message.success('评论发表成功');
          } else {
            this.$message.error('发表失败：' + res.data.message);
          }
        } catch (error) {
          console.error(error);
          this.$message.error('网络错误');
        } finally {
          this.submitting = false;
        }
      },
      // 显示回复输入框
      showReplyInput(comment, parentTopComment) {
        // 如果是回复某个回复，parentTopComment 为顶级评论对象（用于提交时确定 parentId）
        // 我们统一将回复的 parentId 设置为顶级评论的ID，所以只需记录当前回复的是哪个评论（用于显示提示）
        this.replyTarget = comment;
        this.replyContent = ''; // 清空之前内容
      },
      // 取消回复
      cancelReply() {
        this.replyTarget = null;
        this.replyContent = '';
      },
      // 提交回复
      async submitReply(topComment, replyTo = null) {
        if (!this.replyContent.trim()) return;
        this.replySubmitting = true;
        try {
          // 无论回复的是顶级评论还是二级评论，parentId 都设为顶级评论的ID（二级评论统一挂在该顶级下）
          const parentId = topComment.id;
          const res = await this.$axios.post(`/api/posts/${this.postId}/comments`, {
            content: this.replyContent,
            parentId: parentId,
          });
          if (res.data.code === 0) {
            const newReply = res.data.data;
            this.rawComments.push(newReply);
            this.replyContent = '';
            this.replyTarget = null;
            this.$message.success('回复成功');
          } else {
            this.$message.error('回复失败：' + res.data.message);
          }
        } catch (error) {
          console.error(error);
          this.$message.error('网络错误');
        } finally {
          this.replySubmitting = false;
        }
      },
    },
  };
  </script>
  
  <style scoped>
  .post-detail-container {
    max-width: 900px;
    margin: 20px auto;
    padding: 0 20px;
    min-height: 120vh;
    position: relative;
  }
  
  /* 帖子卡片 */
  .post-header {
    margin-bottom: 20px;
    border-radius: 8px;
  }
  .post-title {
    margin: 0 0 16px 0;
    font-size: 2.2rem;
    color: #303133;
    line-height: 1.3;
  }
  .post-meta {
    display: flex;
    align-items: center;
    gap: 15px;
    flex-wrap: wrap;
    margin-bottom: 20px;
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
  .post-content {
    font-size: 1.1rem;
    line-height: 1.8;
    color: #2c3e50;
  }
  
  /* 评论区卡片 */
  .comment-section {
    border-radius: 8px;
  }
  .comment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 15px;
  }
  .comment-tools {
    display: flex;
    align-items: center;
    gap: 15px;
  }
  .comment-input-area {
    margin: 20px 0;
  }
  .comment-submit {
    display: flex;
    justify-content: flex-end;
    margin-top: 10px;
  }
  
  /* 评论列表 */
  .comment-list {
    margin-top: 20px;
  }
  .comment-thread {
    margin-bottom: 20px;
    border-bottom: 1px solid #f0f2f5;
    padding-bottom: 20px;
  }
  .comment-thread:last-child {
    border-bottom: none;
  }
  .comment-item {
    display: flex;
    gap: 15px;
    margin-bottom: 10px;
  }
  .reply-item {
    display: flex;
    gap: 12px;
    margin-left: 55px; /* 缩进表示层级 */
    margin-top: 15px;
  }
  .reply-item .el-avatar {
    flex-shrink: 0;
  }
  .comment-content,
  .reply-content {
    flex: 1;
  }
  .comment-meta {
    margin-bottom: 5px;
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
    word-break: break-word;
  }
  .comment-actions {
    margin-top: 8px;
  }
  .comment-actions .el-button {
    padding: 0;
    margin-right: 15px;
  }
  
  /* 回复输入框 */
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
  
  .no-comment {
    text-align: center;
    padding: 30px;
    color: #909399;
    font-size: 14px;
  }
  
  /* 回到顶部按钮 */
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
  </style>