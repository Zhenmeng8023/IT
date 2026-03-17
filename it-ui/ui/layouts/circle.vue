<template>
  <!-- 整体布局容器 -->
  <div class="layout-container">
    <!-- ========== 头部区域：包含折叠按钮、搜索框、操作按钮、头像 ========== -->
    <el-header>
      <div class="header-content">
        <!-- 折叠按钮：点击切换侧边栏折叠状态 -->
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

        <!-- 右侧操作区：写帖子、创建圈子、加入圈子、头像 -->
        <div class="right-actions">
          <!-- 写帖子按钮 -->
          <el-button type="primary" plain @click="openPostDialog" class="write-btn">写帖子</el-button>
          <!-- 创建圈子按钮 -->
          <el-button type="success" plain @click="openCreateDialog" class="create-btn">创建圈子</el-button>
          <!-- 加入圈子按钮 -->
          <el-button type="info" plain @click="openJoinDialog" class="join-btn">加入圈子</el-button>
          <!-- 用户头像，点击跳转到个人主页 -->
          <div class="avatar-wrapper" @click="goToUserHome">
            <el-avatar :size="50" :src="userAvatar"></el-avatar>
          </div>
        </div>
      </div>
    </el-header>

    <!-- ========== 发帖子弹框 ========== -->
    <el-dialog 
      title="发布新帖子" 
      :visible.sync="dialogVisible" 
      width="600px"
      :before-close="handleDialogClose"
      class="post-dialog"
    >
      <el-form :model="postForm" :rules="postRules" ref="postForm" label-width="80px">
        <!-- 帖子内容（无标题） -->
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

        <!-- 圈子选择（多选） -->
        <el-form-item label="发布到圈子" prop="circleIds">
          <el-select
            v-model="postForm.circleIds"
            multiple
            placeholder="请选择要发布的圈子（可多选）"
            class="circle-select"
            clearable
            :loading="circleLoading"
          >
            <el-option
              v-for="circle in circleOptions"
              :key="circle.id"
              :label="circle.name"
              :value="circle.id"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitPost" :loading="submitting">发 布</el-button>
      </span>
    </el-dialog>

    <!-- ========== 创建圈子弹框 ========== -->
    <el-dialog
      title="创建新圈子"
      :visible.sync="createDialogVisible"
      width="500px"
      @close="resetCreateForm"
    >
      <el-form :model="createForm" :rules="createRules" ref="createForm" label-width="100px">
        <el-form-item label="圈子名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入圈子名称"></el-input>
        </el-form-item>
        <el-form-item label="隐私设置" prop="privacy">
          <el-radio-group v-model="createForm.privacy">
            <el-radio label="public">公开（任何人可加入）</el-radio>
            <el-radio label="private">私密（仅邀请）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="submittingCreate">创建</el-button>
      </span>
    </el-dialog>

    <!-- ========== 加入圈子弹框 ========== -->
    <el-dialog
      title="加入圈子"
      :visible.sync="joinDialogVisible"
      width="500px"
      @close="resetJoinForm"
    >
      <el-form :model="joinForm" ref="joinForm" label-width="80px">
        <el-form-item label="选择圈子" prop="circleId" :rules="[{ required: true, message: '请选择圈子', trigger: 'change' }]">
          <el-select
            v-model="joinForm.circleId"
            placeholder="请选择要加入的圈子"
            style="width:100%"
            :loading="circleLoading"
          >
            <el-option
              v-for="circle in circleOptions"
              :key="circle.id"
              :label="circle.name"
              :value="circle.id"
            >
              <span style="float: left">{{ circle.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">{{ circle.memberCount || 0 }} 成员</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitJoin" :loading="submittingJoin">加入</el-button>
      </span>
    </el-dialog>

    <!-- ========== 主体：侧边栏 + 内容区 ========== -->
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
          <!-- 动态生成菜单项 -->
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区：渲染子路由页面 -->
      <el-main class="main-content">
        <nuxt />
      </el-main>
    </el-container>
  </div>
</template>

<script>
// 导入圈子相关API（根据实际项目路径调整）
import { GetAllCircles, CreateCircle, JoinCircle, CreateCircleComment } from '@/api/index';

export default {
  data() {
    return {
      // ---------- 界面状态 ----------
      isCollapse: false,               // 侧边栏折叠状态
      searchKeyword: '',                // 搜索关键词
      activeTag: 'all',                 // 当前选中的标签（备用）

      // ---------- 发帖子弹框 ----------
      dialogVisible: false,             // 控制发帖子弹框显示
      submitting: false,                // 发帖提交中状态
      postForm: {
        content: '',                    // 帖子内容
        circleIds: [],                  // 选中的圈子ID数组（多选）
      },
      // 发帖表单验证规则
      postRules: {
        content: [
          { required: true, message: '请输入帖子内容', trigger: 'blur' },
          { min: 5, max: 5000, message: '长度在 5 到 5000 个字符', trigger: 'blur' }
        ],
        circleIds: [
          { type: 'array', required: true, message: '请至少选择一个圈子', trigger: 'change' }
        ]
      },

      // ---------- 创建圈子弹框 ----------
      createDialogVisible: false,       // 控制创建圈子弹框显示
      createForm: {
        name: '',                       // 圈子名称
        privacy: 'public',              // 隐私设置（默认公开）
      },
      createRules: {
        name: [
          { required: true, message: '请输入圈子名称', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        privacy: [
          { required: true, message: '请选择隐私设置', trigger: 'change' }
        ]
      },
      submittingCreate: false,          // 创建圈子提交中

      // ---------- 加入圈子弹框 ----------
      joinDialogVisible: false,         // 控制加入圈子弹框显示
      joinForm: {
        circleId: null,                 // 选中的圈子ID（单选）
      },
      submittingJoin: false,            // 加入圈子提交中

      // ---------- 圈子数据 ----------
      circleOptions: [],                // 圈子列表（用于下拉框）
      circleLoading: false,             // 圈子列表加载中

      // ---------- 菜单项 ----------
      menuItems: [
        { index: '/', icon: 'el-icon-s-home', title: '首页' },
        { index: '/blog', icon: 'el-icon-document', title: '博客' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' },
      ],

      // ---------- 用户信息（模拟，无登录验证） ----------
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      userId: 1,                         // 模拟用户ID
      username: '测试用户',               // 模拟用户名
    };
  },
  computed: {
    // 侧边栏宽度，根据折叠状态动态计算
    asideWidth() {
      return this.isCollapse ? '64px' : '200px';
    },
    // 判断是否为特殊页面（圈子详情页或写博客页），在这些页面上隐藏搜索框
    isSpecialPage() {
      const path = this.$route.path;
      return path.startsWith('/circle/') || path.startsWith('/write');
    },
    // 判断是否为首页（备用，当前未使用）
    isHomePage() {
      return this.$route.path === '/' || this.$route.path === '/circle';
    },
    // 根据当前路由计算高亮菜单项
    activeMenu() {
      const path = this.$route.path;
      if (path.startsWith('/blog')) return '/blog';
      if (path.startsWith('/circle')) return '/circle';
      if (path.startsWith('/user')) return '/user';
      return '/';
    },
  },
  watch: {
    // 监听路由查询参数，同步搜索框和标签
    '$route.query': {
      handler(query) {
        this.searchKeyword = query.keyword || '';
        this.activeTag = query.tag || 'all';
      },
      immediate: true,
    },
  },
  created() {
    // 组件创建时加载圈子列表，供下拉框使用
    this.fetchCircles();
  },
  methods: {
    // ---------- 通用方法 ----------
    /**
     * 获取圈子列表（用于下拉框）
     * 调用 GetAllCircles API，处理后端返回的不同格式
     */
    async fetchCircles() {
      this.circleLoading = true;
      try {
        const res = await GetAllCircles();
        let circles = [];
        if (Array.isArray(res)) {
          circles = res;
        } else if (res.data && Array.isArray(res.data)) {
          circles = res.data;
        } else if (res.data && res.data.list) {
          circles = res.data.list;
        }
        this.circleOptions = circles;
      } catch (error) {
        console.error('获取圈子列表失败', error);
        this.$message.error('获取圈子列表失败');
      } finally {
        this.circleLoading = false;
      }
    },

    /**
     * 切换侧边栏折叠状态
     */
    toggleSidebar() {
      this.isCollapse = !this.isCollapse;
    },

    /**
     * 处理搜索：将关键词写入路由 query，触发搜索
     */
    handleSearch() {
      this.$router.push({
        query: {
          ...this.$route.query,
          keyword: this.searchKeyword || undefined,
          page: 1,
        },
      });
    },

    /**
     * 跳转到用户主页（模拟用户）
     */
    goToUserHome() {
      this.$router.push(`/user/${this.userId}`);
    },

    /**
     * 统一错误处理函数
     * @param {Object} error - axios 错误对象
     * @param {string} action - 操作名称（用于提示）
     */
    handleRequestError(error, action) {
      if (error.response) {
        this.$message.error(`${action}失败：${error.response.data?.message || `服务器错误 ${error.response.status}`}`);
      } else if (error.request) {
        this.$message.error('网络错误，请检查连接');
      } else {
        this.$message.error(`${action}请求错误：` + error.message);
      }
    },

    // ---------- 发帖子相关 ----------
    /**
     * 打开发帖子弹框，重置表单
     */
    openPostDialog() {
      this.postForm = {
        content: '',
        circleIds: [],
      };
      this.$nextTick(() => {
        if (this.$refs.postForm) {
          this.$refs.postForm.clearValidate();
        }
      });
      this.dialogVisible = true;
    },

    /**
     * 发帖子弹框关闭前的处理，防止内容丢失
     * @param {Function} done - 关闭回调
     */
    handleDialogClose(done) {
      if (this.postForm.content || this.postForm.circleIds.length > 0) {
        this.$confirm('确定关闭？未保存的内容将会丢失', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => done()).catch(() => {});
      } else {
        done();
      }
    },

    /**
     * 提交新帖子
     * 验证表单，调用后端 API，成功后刷新圈子页面
     */
    async submitPost() {
      this.$refs.postForm.validate(async (valid) => {
        if (!valid) {
          this.$message.error('请完善帖子信息');
          return;
        }

        this.submitting = true;
        try {
          const postData = {
            content: this.postForm.content,
            circleIds: this.postForm.circleIds,
            authorId: this.userId,               // 模拟用户ID
          };
          const response = await CreateCircleComment(postData);
          if (response.data && (response.data.code === 0 || response.data.success)) {
            this.$message.success('帖子发布成功！');
            this.dialogVisible = false;
            // 如果当前在圈子首页，添加时间戳强制刷新帖子列表
            if (this.$route.path === '/circle') {
              this.$router.push({
                path: '/circle',
                query: { ...this.$route.query, _t: Date.now() }
              });
            }
          } else {
            this.$message.error('发布失败：' + (response.data.message || '未知错误'));
          }
        } catch (error) {
          console.error('发布帖子出错:', error);
          this.handleRequestError(error, '发布');
        } finally {
          this.submitting = false;
        }
      });
    },

    // ---------- 创建圈子相关 ----------
    /**
     * 打开创建圈子弹框，重置表单
     */
    openCreateDialog() {
      this.createDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.createForm?.clearValidate();
      });
    },

    /**
     * 重置创建圈子表单（弹框关闭时调用）
     */
    resetCreateForm() {
      this.createForm.name = '';
      this.createForm.privacy = 'public';
      this.$refs.createForm?.clearValidate();
    },

    /**
     * 提交创建圈子
     */
    async submitCreate() {
      this.$refs.createForm.validate(async (valid) => {
        if (!valid) return;
        this.submittingCreate = true;
        try {
          const createData = {
            name: this.createForm.name,
            privacy: this.createForm.privacy,
            creatorId: this.userId,               // 模拟用户ID
          };
          const response = await CreateCircle(createData);
          if (response.data && (response.data.code === 0 || response.data.success)) {
            this.$message.success('圈子创建成功');
            this.createDialogVisible = false;
            await this.fetchCircles();             // 刷新圈子列表供其他弹框使用
          } else {
            this.$message.error('创建失败：' + (response.data.message || '未知错误'));
          }
        } catch (error) {
          console.error('创建圈子出错:', error);
          this.handleRequestError(error, '创建');
        } finally {
          this.submittingCreate = false;
        }
      });
    },

    // ---------- 加入圈子相关 ----------
    /**
     * 打开加入圈子弹框，确保圈子列表已加载
     */
    async openJoinDialog() {
      // 如果圈子列表为空，先加载
      if (this.circleOptions.length === 0) {
        await this.fetchCircles();
      }
      this.joinDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.joinForm?.clearValidate();
      });
    },

    /**
     * 重置加入圈子表单（弹框关闭时调用）
     */
    resetJoinForm() {
      this.joinForm.circleId = null;
      this.$refs.joinForm?.clearValidate();
    },

    /**
     * 提交加入圈子
     */
    async submitJoin() {
      this.$refs.joinForm.validate(async (valid) => {
        if (!valid) return;
        this.submittingJoin = true;
        try {
          const joinData = {
            userId: this.userId,                   // 模拟用户ID
          };
          const response = await JoinCircle(this.joinForm.circleId, joinData);
          if (response.data && (response.data.code === 0 || response.data.success)) {
            this.$message.success('成功加入圈子');
            this.joinDialogVisible = false;
            // 如果当前在圈子首页，刷新帖子列表
            if (this.$route.path === '/circle') {
              this.$router.push({
                path: '/circle',
                query: { ...this.$route.query, _t: Date.now() }
              });
            }
          } else {
            this.$message.error('加入失败：' + (response.data.message || '未知错误'));
          }
        } catch (error) {
          console.error('加入圈子出错:', error);
          this.handleRequestError(error, '加入');
        } finally {
          this.submittingJoin = false;
        }
      });
    },
  },
};
</script>

<style scoped>
/* ========== 整体布局容器 ========== */
.layout-container {
  background-color: #d4d4d4;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ========== 头部样式 ========== */
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

/* 右侧操作区按钮组 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-wrapper {
  cursor: pointer;
}

/* ========== 侧边栏样式 ========== */
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

/* ========== 主内容区样式 ========== */
.main-content {
  background-color: #d4d4d4;
  min-height: calc(100vh - 60px);
  padding: 20px;
}

/* ========== 发帖子弹框样式 ========== */
.post-dialog {
  border-radius: 8px;
}
.post-dialog .el-dialog__body {
  padding: 20px 30px;
}
.circle-select {
  width: 100%;
}

/* ========== 响应式调整 ========== */
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
  .right-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}
</style>