<template>
    <div class="write-blog-container">
      <!-- 头部区域：左侧头像，右侧操作按钮 -->
      <div class="write-header">
        <!-- 头像区域，点击跳转到用户主页（示例路由：/user/:userId） -->
        <div class="user-info" @click="goToUserHome">
          <el-avatar :size="50" :src="userAvatar"></el-avatar>
          <span class="username">{{ username }}</span>
        </div>
        <div class="action-buttons">
          <!-- 最后保存时间提示（仅草稿） -->
          <span v-if="lastSaved" class="save-tip">最后保存：{{ lastSaved }}</span>
          <!-- 存草稿按钮 -->
          <el-button type="info" plain @click="saveDraft" :loading="savingDraft">
            存草稿
          </el-button>
          <!-- 发布按钮 -->
          <el-button type="primary" @click="publishBlog" :loading="publishing">
            发布
          </el-button>
        </div>
      </div>
  
      <!-- 博客编辑主体区域（居中） -->
      <div class="edit-area">
        <!-- 标题输入框 -->
        <el-input
          class="title-input"
          v-model="blog.title"
          placeholder="请输入博客标题"
          maxlength="100"
          show-word-limit
          clearable
        ></el-input>
  
        <!-- 正文输入框（文本域，可扩展为富文本编辑器） -->
        <el-input
          class="content-input"
          type="textarea"
          v-model="blog.content"
          placeholder="请输入博客内容..."
          :rows="20"
          resize="vertical"
          maxlength="10000"
          show-word-limit
        ></el-input>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'WriteBlog',
    data() {
      return {
        // ---------- 博客数据 ----------
        blog: {
          id: null,           // 博客ID，如果编辑已有文章则有值
          title: '',
          content: '',
          status: 'draft',    // 默认为草稿，发布时为 'published'
        },
        // ---------- 用户信息（模拟，实际从store或props获取）----------
        userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
        username: '当前用户',
        userId: '',          // 用于跳转个人主页
  
        // ---------- 界面状态 ----------
        savingDraft: false,   // 保存草稿按钮loading
        publishing: false,    // 发布按钮loading
        lastSaved: '',        // 最后一次保存草稿的时间（格式 HH:mm:ss）
      };
    },
    created() {
      // 如果路由参数中有博客ID，则加载博客内容（编辑模式）
      const blogId = this.$route.params.id;
      if (blogId) {
        this.fetchBlog(blogId);
      }
    },
    methods: {
      /**
       * 跳转到用户主页
       * 根据实际路由调整路径
       */
      goToUserHome() {
        this.$router.push(`/user/${this.userId}`);
      },
  
      /**
       * 获取博客详情（用于编辑已有博客）
       * 接口：GET /api/blogs/:id
       */
      async fetchBlog(blogId) {
        try {
          const res = await this.$axios.get(`/api/blogs/${blogId}`);
          if (res.data.code === 0) {
            const blogData = res.data.data;
            this.blog = {
              id: blogData.id,
              title: blogData.title,
              content: blogData.content,
              status: blogData.status, // 可能为 'draft' 或 'published'
            };
            // 如果博客已发布，存草稿按钮可以禁用或隐藏？这里保持可用（会更新为草稿版本）
          } else {
            this.$message.error('获取博客失败：' + res.data.message);
          }
        } catch (error) {
          console.error('获取博客失败', error);
          this.$message.error('网络错误，请刷新重试');
        }
      },
  
      /**
       * 保存博客（通用方法，根据参数决定发布或存草稿）
       * @param {string} status - 'draft' 或 'published'
       */
      async saveBlog(status) {
        // 校验标题和内容（可根据需求调整）
        if (!this.blog.title.trim()) {
          this.$message.warning('请填写博客标题');
          return false;
        }
        if (!this.blog.content.trim()) {
          this.$message.warning('请填写博客内容');
          return false;
        }
  
        const isPublish = status === 'published';
        const loading = isPublish ? (this.publishing = true) : (this.savingDraft = true);
  
        try {
          // 构建请求数据
          const requestData = {
            title: this.blog.title,
            content: this.blog.content,
            status: status,
          };
          let res;
          if (this.blog.id) {
            // 编辑模式：PUT /api/blogs/:id
            res = await this.$axios.put(`/api/blogs/${this.blog.id}`, requestData);
          } else {
            // 新建模式：POST /api/blogs
            res = await this.$axios.post('/api/blogs', requestData);
          }
  
          if (res.data.code === 0) {
            const result = res.data.data;
            // 更新本地博客ID（如果是新建则返回新ID）
            if (!this.blog.id) {
              this.blog.id = result.id;
            }
            // 更新状态为后端返回的状态（可能后端处理了）
            this.blog.status = result.status || status;
  
            // 如果是存草稿，记录保存时间
            if (status === 'draft') {
              const now = new Date();
              this.lastSaved = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
              this.$message.success('草稿保存成功');
            } else {
              this.$message.success('发布成功');
              // 发布成功后，可选择跳转到博客详情页或留在当前页
              // this.$router.push(`/blog/${this.blog.id}`);
            }
            return true;
          } else {
            this.$message.error((isPublish ? '发布' : '保存草稿') + '失败：' + res.data.message);
            return false;
          }
        } catch (error) {
          console.error('保存博客出错', error);
          this.$message.error('网络错误，请稍后重试');
          return false;
        } finally {
          if (isPublish) {
            this.publishing = false;
          } else {
            this.savingDraft = false;
          }
        }
      },
  
      /**
       * 存草稿（手动）
       */
      saveDraft() {
        this.saveBlog('draft');
      },
  
      /**
       * 发布博客
       */
      publishBlog() {
        this.saveBlog('published');
      },
    },
  };
  </script>
  
  <style scoped>
  .write-blog-container {
    max-width: 1000px;
    margin: 0 auto;
    padding: 20px;
  }
  
  /* 头部样式 */
  .write-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding-bottom: 10px;
    border-bottom: 1px solid #ebeef5;
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    transition: opacity 0.2s;
  }
  .user-info:hover {
    opacity: 0.8;
  }
  .username {
    font-size: 16px;
    color: #409EFF;
    font-weight: 500;
  }
  
  .action-buttons {
    display: flex;
    align-items: center;
    gap: 15px;
  }
  .save-tip {
    font-size: 14px;
    color: #909399;
  }
  
  /* 编辑区域 */
  .edit-area {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  
  .title-input >>> .el-input__inner {
    font-size: 24px;
    height: 50px;
    line-height: 50px;
    font-weight: 500;
  }
  
  .content-input >>> .el-textarea__inner {
    font-size: 16px;
    line-height: 1.8;
    font-family: 'Microsoft YaHei', sans-serif;
    min-height: 400px;
  }
  </style>