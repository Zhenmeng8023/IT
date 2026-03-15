<template>
  <div class="layout-container">
    <!-- 头部：包含折叠按钮、搜索框、写文章按钮、头像 -->
    <el-header>
      <div class="header-content">
        <!-- 折叠按钮（始终显示） -->
        <div class="collapse-btn" @click="toggleSidebar">
          <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
        </div>

        <!-- 搜索区域（只在非详情/写博客页显示） -->
        <div v-if="!isSpecialPage" class="search-area">
          <el-input
            placeholder="输入要搜索的圈子名"
            prefix-icon="el-icon-search"
            v-model="searchKeyword"
            class="search-input"
            @keyup.enter.native="handleSearch"
            clearable
          ></el-input>
          <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
        </div>

        <!-- 右侧操作区：写文章按钮 + 头像 -->
        <div class="right-actions">
          <el-button type="info" @click="openPostDialog" plain class="write-btn">发帖子</el-button>
          <div class="avatar-wrapper" @click="goToUserHome">
            <el-avatar :size="50" :src="userAvatar"></el-avatar>
          </div>
        </div>
      </div>
    </el-header>

    <!-- 发帖子弹框 -->
    <el-dialog 
      title="发布新帖子" 
      :visible.sync="dialogVisible" 
      width="600px"
      :before-close="handleDialogClose"
      class="post-dialog"
    >
      <el-form :model="postForm" :rules="postRules" ref="postForm" label-width="80px">
        <!-- 帖子标题 -->
        <el-form-item label="标题" prop="title">
          <el-input 
            v-model="postForm.title" 
            placeholder="请输入帖子标题" 
            maxlength="100" 
            show-word-limit
          ></el-input>
        </el-form-item>

        <!-- 帖子内容 -->
        <el-form-item label="内容" prop="content">
          <el-input 
            type="textarea" 
            v-model="postForm.content" 
            placeholder="请输入帖子内容..." 
            :rows="8"
            maxlength="5000" 
            show-word-limit
          ></el-input>
        </el-form-item>

        <!-- 标签选择 -->
        <el-form-item label="圈子" prop="circles">
          <el-select
            v-model="postForm.circles"
            multiple
            placeholder="请选择圈子（可多选）"
            class="circle-select"
            clearable
          >
            <el-option label="技术交流圈" value="技术交流圈"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitPost" :loading="submitting">发 布</el-button>
      </span>
    </el-dialog>

    <!-- 主体：侧边栏 + 内容区 -->
    <el-container>
      <!-- 侧边栏（可折叠） -->
      <el-aside :width="asideWidth" class="asid-content">
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          router
          :collapse="isCollapse"
          :collapse-transition="true"
        >
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <!-- 路由页面内容 -->
        <nuxt />
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  data() {
    return {
      isCollapse: false,               // 侧边栏折叠状态
      searchKeyword: '',
      activeTag: 'all',
      dialogVisible: false,            // 发帖子弹框显示状态
      submitting: false,               // 提交中状态
      postForm: {                      // 帖子表单数据
        title: '',
        content: '',
        tags: [],
      },
      postRules: {                      // 表单验证规则
        title: [
          { required: true, message: '请输入帖子标题', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '请输入帖子内容', trigger: 'blur' },
          { min: 5, max: 100, message: '长度在 5 到 100 个字符', trigger: 'blur' }
        ],
        circles: [
          { type: 'array', required: true, message: '请选择圈子', trigger: 'change' }
        ]
      },
      menuItems: [
        { index: '/', icon: 'el-icon-s-home', title: '首页' },
        { index: '/blog', icon: 'el-icon-document', title: '博客' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' },
      ],
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    };
  },
  computed: {
    // 侧边栏宽度，根据折叠状态动态变化
    asideWidth() {
      return this.isCollapse ? '64px' : '200px';
    },
    // 是否为特殊页面（博客详情或写博客页）
    isSpecialPage() {
      const path = this.$route.path;
      return path.startsWith('/circle/') || path.startsWith('/write');
    },
    // 是否为首页（显示标签页）
    isHomePage() {
      return this.$route.path === '/' || this.$route.path === '/circle';
    },
    // 当前激活的菜单项（用于高亮）
    activeMenu() {
      const path = this.$route.path;
      if (path.startsWith('/blog')) return '/blog';
      if (path.startsWith('/circle')) return '/circle';
      if (path.startsWith('/user')) return '/user';
      return '/';
    },
    // 当前用户ID（模拟数据）
    userId() {
      return 1; // 临时模拟用户ID
    },
    // 当前用户名（模拟数据）
    username() {
      return '测试用户';
    },
  },
  watch: {
    // 监听路由 query，同步搜索框和标签
    '$route.query': {
      handler(query) {
        this.searchKeyword = query.keyword || '';
        this.activeTag = query.tag || 'all';
      },
      immediate: true,
    },
  },
  methods: {
    // 切换侧边栏折叠状态
    toggleSidebar() {
      this.isCollapse = !this.isCollapse;
    },
    // 处理搜索：更新路由 query
    handleSearch() {
      this.$router.push({
        query: {
          ...this.$route.query,
          keyword: this.searchKeyword || undefined,
          page: 1, // 重置到第一页
        },
      });
    },
    // 跳转到用户主页
    goToUserHome() {
      this.$router.push(`/user/${this.userId}`);
    },
    
    // 打开发帖子弹框
    openPostDialog() {
      // 重置表单
      this.postForm = {
        title: '',
        content: '',
        tags: [],
      };
      // 清除验证状态
      this.$nextTick(() => {
        if (this.$refs.postForm) {
          this.$refs.postForm.clearValidate();
        }
      });
      this.dialogVisible = true;
    },
    
    // 弹框关闭前的处理
    handleDialogClose(done) {
      // 如果有未保存的内容，提示用户
      if (this.postForm.title || this.postForm.content || this.postForm.tags.length > 0) {
        this.$confirm('确定关闭？未保存的内容将会丢失', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          done();
        }).catch(() => {});
      } else {
        done();
      }
    },
    
    // 提交帖子
    submitPost() {
      this.$refs.postForm.validate(async (valid) => {
        if (!valid) {
          this.$message.error('请完善帖子信息');
          return false;
        }
        
        this.submitting = true;
        try {
          // 构建请求数据
          const postData = {
            title: this.postForm.title,
            content: this.postForm.content,
            tags: this.postForm.tags,
            author: {
              id: this.userId,
              nickname: this.username,
              avatar: this.userAvatar,
            },
            createTime: new Date().toISOString(),
            commentCount: 0,
            likeCount: 0,
          };
          
          // 调用后端 API 保存帖子
          const response = await this.$axios.post('/api/posts', postData);
          
          // 根据实际后端返回格式处理
          if (response.data && (response.data.code === 0 || response.data.success)) {
            this.$message.success('帖子发布成功！');
            this.dialogVisible = false;
            
            // 通知圈子页面刷新帖子列表（通过添加时间戳参数触发重新加载）
            this.$router.push({
              path: '/circle',
              query: {
                ...this.$route.query,
                _t: Date.now(), // 添加时间戳强制刷新
              }
            });
            
          } else {
            this.$message.error('发布失败：' + (response.data.message || '未知错误'));
          }
        } catch (error) {
          console.error('发布帖子出错:', error);
          if (error.response) {
            this.$message.error('发布失败：' + (error.response.data?.message || `服务器错误 ${error.response.status}`));
          } else if (error.request) {
            this.$message.error('网络错误，请检查连接');
          } else {
            this.$message.error('请求错误：' + error.message);
          }
        } finally {
          this.submitting = false;
        }
      });
    },
  },
};
</script>

<style scoped>
.layout-container {
  background-color: #d4d4d4;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 头部样式 */
.el-header {
  padding: 0;
  background-color: #f6eeee;
}
.header-content {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 20px;
}

/* 折叠按钮 */
.collapse-btn {
  width: 50px;
  text-align: center;
  font-size: 22px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}
.collapse-btn:hover {
  color: #409EFF;
}

/* 搜索区域（居中） */
.search-area {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 20px;
}
.search-input {
  width: 60%;
  max-width: 600px;
}
.search-btn {
  margin-left: 10px;
}

/* 右侧操作区 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}
.avatar-wrapper {
  cursor: pointer;
}

/* 侧边栏样式 */
.asid-content {
  transition: width 0.3s;
  overflow-x: hidden;
  background-color: #f6eeee;
}
.el-menu:not(.el-menu--collapse) {
  width: 200px;
}
.el-menu {
  border-right: none;
  background-color: #f6eeee;
}

/* 主内容区 */
.main-content {
  background-color: #d4d4d4;
  min-height: calc(100vh - 60px);
  padding: 20px;
}

/* 发帖子弹框样式 */
.post-dialog {
  border-radius: 8px;
}

.post-dialog .el-dialog__body {
  padding: 20px 30px;
}

.tag-select {
  width: 100%;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .post-dialog {
    width: 90% !important;
  }
  
  .search-area {
    flex-direction: column;
    gap: 10px;
  }
  
  .search-input {
    width: 100%;
  }
  
  .search-btn {
    width: 100%;
    margin-left: 0;
  }
}
</style>