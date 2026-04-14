<template>
  <div class="user-home-container">
    <!-- ========== 用户主页主体 ========== -->
    <div class="main-content">
      <!-- 左侧个人资料卡片 -->
      <div class="left-profile">
        <div class="profile-card">
          <div class="profile-header">
            <div class="avatar-wrapper">
              <el-avatar :size="180" :src="avatarUrl" class="profile-avatar" @error="handleAvatarError"></el-avatar>
              <!-- 只有访问自己的主页时才显示编辑按钮 -->
              <div v-if="isSelf" class="avatar-edit-overlay" @click="openEditDialog">
                <i class="el-icon-edit"></i>
                <span>编辑资料</span>
              </div>
            </div>
            <h2 class="profile-name">{{ nickname || username }}</h2>
            <p class="profile-bio">{{ usersign || '这个用户很懒，什么都没留下~' }}</p>
          </div>

          <div class="profile-stats">
            <div class="stat-item" @click="handleCollectClick">
              <span class="stat-value">{{ userStats.totalCollects || 0 }}</span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ userStats.totalLikes || 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" @click="handleKnowledgeClick">
              <span class="stat-value">{{ userStats.totalKnowledge || 0 }}</span>
              <span class="stat-label">知识产品</span>
            </div>
          </div>

          <div class="profile-info">
            <div class="info-item">
              <i class="el-icon-location-outline"></i>
              <span>{{ useraddress || '未设置居住地' }}</span>
            </div>
            <div class="info-item">
              <i class="el-icon-message"></i>
              <span>{{ useremail || '未设置邮箱' }}</span>
            </div>
            <div class="info-item">
              <i class="el-icon-phone"></i>
              <span>{{ userphone || '未设置电话' }}</span>
            </div>
            <div class="info-item">
              <i class="el-icon-date"></i>
              <span>{{ formatDate(userbrithday) || '未设置生日' }}</span>
            </div>
            <div class="info-item">
              <i class="el-icon-present"></i>
              <span>性别：{{ usersex === 'male' ? '男' : usersex === 'female' ? '女' : '其他' }}</span>
            </div>
            <div class="info-item tags">
              <i class="el-icon-collection-tag"></i>
              <el-tag size="small" v-if="authorTagName">{{ authorTagName }}</el-tag>
              <span v-else>未设置标签</span>
            </div>
          </div>

          <!-- 只有访问自己的主页时才显示编辑按钮 -->
          <el-button v-if="isSelf" type="primary" class="edit-profile-btn" @click="openEditDialog">
            <i class="el-icon-edit"></i> 编辑资料
          </el-button>
        </div>

        <!-- 编辑资料弹窗 -->
        <el-dialog 
          title="编辑个人资料" 
          :visible.sync="dialogFormVisible" 
          width="500px"
          class="edit-dialog"
          :close-on-click-modal="false"
          :append-to-body="true"
          :modal-append-to-body="true"
          @close="handleDialogClose"
        >
          <el-form ref="form" :model="formData" label-width="80px" class="edit-form">
            <!-- 头像选择 -->
            <el-form-item label="头像">
              <div class="avatar-uploader-container">
                <div class="avatar-preview-wrapper">
                  <el-avatar 
                    :size="100" 
                    :src="previewAvatarUrl || avatarUrl" 
                    class="upload-avatar"
                    @error="handlePreviewAvatarError"
                  ></el-avatar>
                </div>
                <div class="avatar-selector">
                  <p class="selector-title">选择头像：</p>
                  <div class="avatar-options">
                    <div 
                      v-for="avatar in avatarOptions" 
                      :key="avatar.value"
                      class="avatar-option"
                      :class="{ active: (previewAvatarUrl || avatarUrl) === avatar.value }"
                      @click="selectAvatar(avatar.value)"
                    >
                      <el-avatar :size="60" :src="avatar.value"></el-avatar>
                      <span class="avatar-name">{{ avatar.label }}</span>
                    </div>
                  </div>
                </div>
                <div class="upload-tip" v-if="previewAvatarUrl || avatarUrl">
                  <p class="current-path">
                    当前头像: {{ getAvatarLabel(previewAvatarUrl || avatarUrl) }}
                  </p>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="昵称">
              <el-input v-model="formData.nickname" placeholder="请输入昵称" prefix-icon="el-icon-user" clearable></el-input>
            </el-form-item>
            <el-form-item label="电话">
              <el-input v-model="formData.userphone" placeholder="请输入电话" prefix-icon="el-icon-phone" clearable></el-input>
            </el-form-item>
            <el-form-item label="生日">
              <el-date-picker 
                type="date" 
                placeholder="请选择生日" 
                v-model="formData.userbrithday" 
                style="width: 100%;"
                format="yyyy-MM-dd"
                value-format="yyyy-MM-dd"
              ></el-date-picker>
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="formData.usersex">
                <el-radio label="male">男</el-radio>
                <el-radio label="female">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="签名">
              <el-input 
                v-model="formData.usersign" 
                placeholder="为大家留下签名吧" 
                prefix-icon="el-icon-edit" 
                clearable
                type="textarea"
                :rows="3"
              ></el-input>
            </el-form-item>
            <el-form-item label="标签">
              <el-cascader 
                v-model="formData.authorTagId" 
                :options="tagList" 
                :props="{ 
                  expandTrigger: 'hover', 
                  value: 'value',
                  label: 'label', 
                  children: 'children',
                  multiple: false,
                  checkStrictly: true
                }" 
                @change="handleTagChange"
                popper-class="tag-cascader"
                placeholder="请选择标签"
                clearable
                style="width: 100%"
              ></el-cascader>
            </el-form-item>
            <el-form-item label="省市县">
              <el-cascader 
                v-model="formData.usercity" 
                :options="cityList" 
                :props="{ 
                  expandTrigger: 'hover', 
                  value: 'value', 
                  label: 'label', 
                  children: 'children' 
                }" 
                @change="handleChange"
                popper-class="region-cascader"
                placeholder="请选择地区"
                clearable
                style="width: 100%"
              ></el-cascader>
            </el-form-item>
          </el-form>
          <span slot="footer" class="dialog-footer">
            <el-button @click="dialogFormVisible = false">取消</el-button>
            <el-button type="primary" @click="onSubmit" :loading="submitting">保存修改</el-button>
          </span>
        </el-dialog>
      </div>

      <!-- 中间内容区域 -->
      <div class="middle-content">
        <!-- 统计卡片 -->
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-icon like-icon">
              <i class="el-icon-star-off"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ userStats.totalLikes || 0 }}</div>
              <div class="stat-label">总获赞</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon collect-icon">
              <i class="el-icon-collection"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ userStats.totalCollects || 0 }}</div>
              <div class="stat-label">收藏数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon knowledge-icon">
              <i class="el-icon-book"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ userStats.totalKnowledge || 0 }}</div>
              <div class="stat-label">知识产品</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon revenue-icon">
              <i class="el-icon-money"></i>
            </div>
            <div class="stat-info">
              <div class="stat-number">¥{{ userStats.totalRevenue || 0 }}</div>
              <div class="stat-label">总收入</div>
            </div>
          </div>
        </div>

        <!-- 操作按钮组 -->
        <div class="action-buttons">
          <el-button type="warning" plain @click="handleCollectClick" class="action-btn">
            <i class="el-icon-star-off"></i> 我的收藏
          </el-button>
          <el-button type="success" plain @click="handleMyPostsClick" class="action-btn">
            <i class="el-icon-s-promotion"></i> 我的发布
          </el-button>
          <el-button type="primary" plain @click="handleKnowledgeClick" class="action-btn">
            <i class="el-icon-book"></i> 我的知识产品
          </el-button>
          <el-button type="info" plain @click="handleCouponsClick" class="action-btn">
            <i class="el-icon-ticket"></i> 我的优惠券
          </el-button>
          <el-button type="info" plain @click="handlePayClick" class="action-btn">
            <i class="el-icon-wallet"></i> 账户充值
          </el-button>
        </div>

        <!-- 我的发布内容区域（只有自己主页才显示） -->
        <div v-if="isSelf && showMyPosts" class="my-posts-section">
          <div class="section-header">
            <h3>我的发布</h3>
            <el-radio-group v-model="postType" size="small" @change="loadMyPosts">
              <el-radio-button label="blogs">博客</el-radio-button>
              <el-radio-button label="posts">帖子</el-radio-button>
              <el-radio-button label="knowledge">知识产品</el-radio-button>
            </el-radio-group>
          </div>
          
          <div v-loading="postsLoading" class="posts-list">
            <!-- 博客列表 -->
            <div v-if="postType === 'blogs'" class="blog-list">
              <el-card v-for="blog in blogList" :key="blog.id" class="post-item" shadow="hover">
                <div class="post-content" @click="goToBlogDetail(blog.id)">
                  <h4>{{ blog.title }}</h4>
                  <p class="post-summary">{{ blog.summary || blog.content?.substring(0, 100) + '...' }}</p>
                  <div class="post-meta">
                    <span><i class="el-icon-view"></i> {{ blog.viewCount || 0 }}</span>
                    <span><i class="el-icon-star-off"></i> {{ blog.likeCount || 0 }}</span>
                    <span><i class="el-icon-time"></i> {{ formatDate(blog.createTime) }}</span>
                    <el-tag :type="blog.status === 'published' ? 'success' : 'info'" size="mini">
                      {{ blog.status === 'published' ? '已发布' : '草稿' }}
                    </el-tag>
                  </div>
                </div>
                <div class="post-actions">
                  <el-button 
                    type="danger" 
                    icon="el-icon-delete" 
                    size="mini" 
                    circle
                    @click.stop="handleDeleteBlog(blog)"
                    title="删除博客"
                  ></el-button>
                </div>
              </el-card>
              <div v-if="blogList.length === 0" class="empty-list">
                <i class="el-icon-document"></i>
                <p>暂无博客</p>
              </div>
            </div>

            <!-- 帖子列表 -->
            <div v-if="postType === 'posts'" class="post-list">
              <el-card v-for="post in postList" :key="post.id" class="post-item" shadow="hover">
                <div class="post-content" @click="goToPostDetail(post.id)">
                  <h4>{{ post.title }}</h4>
                  <p class="post-summary">{{ post.summary || post.content?.substring(0, 100) + '...' }}</p>
                  <div class="post-meta">
                    <span><i class="el-icon-chat-dot-round"></i> {{ post.commentCount || 0 }}</span>
                    <span><i class="el-icon-star-off"></i> {{ post.likeCount || 0 }}</span>
                    <span><i class="el-icon-time"></i> {{ formatDate(post.createTime) }}</span>
                  </div>
                </div>
                <div class="post-actions">
                  <el-button 
                    type="danger" 
                    icon="el-icon-delete" 
                    size="mini" 
                    circle
                    @click.stop="handleDeletePost(post)"
                    title="删除帖子"
                  ></el-button>
                </div>
              </el-card>
              <div v-if="postList.length === 0" class="empty-list">
                <i class="el-icon-chat-dot-round"></i>
                <p>暂无帖子</p>
              </div>
            </div>

            <!-- 知识产品列表 -->
            <div v-if="postType === 'knowledge'" class="knowledge-list">
              <el-card v-for="knowledge in knowledgeList" :key="knowledge.id" class="post-item" shadow="hover">
                <div class="post-content" @click="goToKnowledgeDetail(knowledge.id)">
                  <h4>{{ knowledge.title }}</h4>
                  <p class="post-summary">{{ knowledge.summary || knowledge.description?.substring(0, 100) + '...' }}</p>
                  <div class="post-meta">
                    <span><i class="el-icon-view"></i> {{ knowledge.viewCount || 0 }}</span>
                    <span><i class="el-icon-star-off"></i> {{ knowledge.likeCount || 0 }}</span>
                    <span><i class="el-icon-money"></i> ¥{{ knowledge.price }}</span>
                    <span><i class="el-icon-time"></i> {{ formatDate(knowledge.createTime) }}</span>
                    <el-tag :type="knowledge.status === 'published' ? 'success' : 'info'" size="mini">
                      {{ knowledge.status === 'published' ? '已发布' : '草稿' }}
                    </el-tag>
                  </div>
                </div>
                <div class="post-actions">
                  <el-button 
                    type="primary" 
                    icon="el-icon-edit" 
                    size="mini" 
                    circle
                    @click.stop="handleEditKnowledge(knowledge)"
                    title="编辑知识产品"
                  ></el-button>
                  <el-button 
                    type="danger" 
                    icon="el-icon-delete" 
                    size="mini" 
                    circle
                    @click.stop="handleDeleteKnowledge(knowledge)"
                    title="删除知识产品"
                  ></el-button>
                </div>
              </el-card>
              <div v-if="knowledgeList.length === 0" class="empty-list">
                <i class="el-icon-book"></i>
                <p>暂无知识产品</p>
                <el-button type="primary" size="small" @click="handleCreateKnowledge">创建知识产品</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 热力图区域 -->
        <div class="heatmap-section">
          <div class="section-header">
            <h3>活动热力图</h3>
            <span class="section-subtitle">近30天综合活跃度（提交优先）</span>
          </div>
          <div class="heatmap-container">
            <HeatmapTracker
              :activity-data="activityHeatmapData"
              :loading="activityLoading"
              :days="30"
            />
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="right-content">
        <ContentSection
          :display-name="nickname || username"
          :is-self="isSelf"
          :activity-data="activityHeatmapData"
        />
      </div>
    </div>

    <!-- ========== 页脚 ========== -->
    <footer class="footer">
      <FooterPlayer />
    </footer>
  </div>
</template>

<script>
import ContentSection from '../Z_userpage/components/ContentSection.vue'
import FooterPlayer from '../Z_userpage/components/FooterPlayer.vue'
import HeatmapTracker from '../Z_userpage/components/HeatmapTracker.vue'
import { useUserStore } from '@/store/user'
import {
  GetCurrentUser, 
  GetAllRegions, 
  GetAllTags, 
  UpdateCurrentUser, 
  GetUserById,
  GetLikesByUser,
  GetCollectsByUser,
  GetUserLogs,
  GetBlogsByAuthorId,
  GetUserCirclePosts,
  GetUserActivityHeatmap,
  GetMyKnowledgeProducts,
  GetUserTotalRevenue,
  GetLikesReceivedByAuthor,
  GetCollectsReceivedByAuthor
} from '@/api/index.js'
import { getMyProjects } from '@/api/project'
import { listProjectBranches } from '@/api/projectBranch'
import { listProjectCommits } from '@/api/projectCommit'

export default {
  name: 'UserHome',
  layout: 'default',
  components: {
    ContentSection,
    FooterPlayer,
    HeatmapTracker
  },
  data() {
    return {
      // 滚动状态
      scrolled: false,
      
      // 用户数据
      imageUrl: '',
      like: false,
      dialogFormVisible: false,
      submitting: false,
      username: '',
      nickname: '',
      userbrithday: '',
      userphone: '',
      useraddress: '',
      usersex: '',
      authorTagId: null,
      authorTagName: '',
      usersign: '',
      loading: false,
      error: '',
      avatarUrl: '/pic/choubi.jpg',
      usercity: [],
      cityList: [],
      tagList: [],
      userId: null,
      regionId: null,
      useremail: '',
      
      // 用户统计数据
      userStats: {
        totalLikes: 0,
        totalCollects: 0,
        followersCount: 0,
        historyCount: 0,
        totalKnowledge: 0,
        totalRevenue: 0
      },
      
      // 我的发布相关
      showMyPosts: false,
      postType: 'blogs',
      postsLoading: false,
      activityLoading: false,
      activityHeatmapData: [],
      activitySummary: {
        commits: 0,
        blogs: 0,
        posts: 0,
        likes: 0,
        collects: 0,
        logs: 0
      },
      blogList: [],
      postList: [],
      knowledgeList: [],
      
      // 表单数据
      formData: {
        nickname: '',
        userbrithday: '',
        userphone: '',
        useraddress: '',
        usersex: '',
        authorTagId: null,
        usersign: '',
        usercity: []
      },
      
      // 头像选择相关
      previewAvatarUrl: '', // 预览头像URL
      avatarOptions: [
        { label: '默认', value: '/pic/choubi.jpg' },
        { label: '小熊', value: '/pic/bear.jpg' },
        { label: '鸭子', value: '/pic/duck.jpg' },
        { label: '兔子', value: '/pic/rabbit.jpg' },
        { label: '乌龟', value: '/pic/tortoise.jpg' }
      ],
      
      // 文件选择相关（保留用于兼容性）
      selectedFile: null,
      selectedFileName: '',
      selectedFileLocalPath: '' // 本地文件路径
    }
  },
  computed: {
    // 判断是否为当前用户自己的主页
    isSelf() {
      const routeId = this.$route.params.id;
      // 无路由参数或参数与当前登录用户ID相同，则认为是自己
      if (!routeId || routeId === ':id' || routeId === 'null' || routeId === 'undefined') {
        return true;
      }
      // 如果已经获取到当前用户ID，则与路由参数比较
      if (this.userId && routeId) {
        return this.userId.toString() === routeId.toString();
      }
      // 默认认为不是自己（等待userId加载）
      return false;
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
    // 清理临时URL
    if (this.previewAvatarUrl && this.previewAvatarUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.previewAvatarUrl);
    }
  },
  created() {
    this.getUserInfo();
    this.getCityList();
    this.getTagList();
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler(newVal) {
        // 路由参数变化时重新获取用户信息
        this.getUserInfo();
      }
    }
  },
  methods: {
    handleScroll() {
      this.scrolled = window.scrollY > 50
    },
    scrollToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },

    // 选择头像
    selectAvatar(avatarUrl) {
      this.previewAvatarUrl = avatarUrl;
      this.$message.success('头像选择成功，点击保存更新头像');
    },

    // 获取头像标签
    getAvatarLabel(avatarUrl) {
      const avatar = this.avatarOptions.find(option => option.value === avatarUrl);
      return avatar ? avatar.label : '未知';
    },

    // 打开编辑资料弹窗（只有自己主页才会调用）
    openEditDialog() {
      console.log('打开编辑资料弹窗');
      
      // 清理之前的预览URL
      if (this.previewAvatarUrl && this.previewAvatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(this.previewAvatarUrl);
        this.previewAvatarUrl = '';
      }
      
      this.formData = {
        nickname: this.nickname,
        userphone: this.userphone,
        usersign: this.usersign,
        useraddress: this.useraddress,
        usersex: this.usersex,
        userbrithday: this.userbrithday,
        authorTagId: this.authorTagId,
        usercity: this.usercity
      };
      
      // 清空文件选择相关
      this.selectedFile = null;
      this.selectedFileName = '';
      
      this.dialogFormVisible = true;
    },

    // 处理弹窗关闭
    handleDialogClose() {
      // 清理临时文件URL
      if (this.previewAvatarUrl && this.previewAvatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(this.previewAvatarUrl);
        this.previewAvatarUrl = '';
      }
      this.selectedFile = null;
      this.selectedFileName = '';
    },

    // 头像加载错误处理
    handleAvatarError() {
      console.log('头像加载失败，使用默认头像');
      this.avatarUrl = '/pic/choubi.jpg';
      return true;
    },

    // 预览头像加载错误处理
    handlePreviewAvatarError() {
      console.log('预览头像加载失败');
      if (this.previewAvatarUrl && this.previewAvatarUrl.startsWith('blob:')) {
        URL.revokeObjectURL(this.previewAvatarUrl);
        this.previewAvatarUrl = '';
      }
      return true;
    },

    // 获取用户信息（根据路由参数决定获取自己还是他人）
    getUserInfo() {
      this.loading = true;
      this.error = "";

      if (!this.$axios) {
        console.error("axios 实例未找到");
        this.error = "系统错误，请刷新页面重试";
        this.loading = false;
        this.$message.error("系统错误，请刷新页面重试");
        return;
      }

      if (process.client) {
        const userId = this.$route.params.id;
        console.log("=== getUserInfo 被调用 ===");
        console.log("当前路由:", this.$route.path);
        console.log("路由参数中的用户 ID:", userId);

        // 如果有 userId 参数且不是占位符，则获取其他用户的公开信息
        if (userId && userId !== ':id' && userId !== 'null' && userId !== 'undefined') {
          console.log("访问其他用户主页，用户 ID:", userId);
          this.getUserPublicInfo(userId);
        } else {
          console.log("访问自己的主页");
          this.getCurrentUserInfo();
        }
      } else {
        this.loading = false;
      }
    },

    // 获取当前登录用户信息
    getCurrentUserInfo() {
      GetCurrentUser()
        .then((response) => {
          console.log("获取当前用户信息成功:", response);
          const userInfo = response.data || response;
          this.processUserInfo(userInfo);
        })
        .catch((error) => {
          console.error("获取当前用户信息失败:", error);
          if (error.response && error.response.status === 401) {
            this.error = "登录已过期，请重新登录";
            this.$message.error("登录已过期，请重新登录");
            this.$router.push("/login");
          } else {
            this.error = "获取用户信息失败，请重试";
            this.$message.error("获取用户信息失败，请重试");
          }
          this.loading = false;
        });
    },

    // 获取其他用户的公开信息
    getUserPublicInfo(userId) {
      console.log('=== getUserPublicInfo 被调用 ===');
      console.log('传入的 userId:', userId);
      
      if (!userId || userId === ':id' || userId === 'null' || userId === 'undefined') {
        console.error('无效的 userId:', userId);
        this.loading = false;
        return;
      }
      
      GetUserById(userId)
        .then((response) => {
          console.log("获取其他用户信息成功:", response);
          const userInfo = response.data || response;
          this.processUserInfo(userInfo);
        })
        .catch((error) => {
          console.error("获取其他用户信息失败:", error);
          this.error = "获取用户信息失败，请重试";
          this.$message.error("获取用户信息失败，请重试");
          this.loading = false;
        });
    },

    // 处理用户信息（公用）
    processUserInfo(userInfo) {
      this.userId = userInfo.id || "";
      
      // 处理头像URL
      if (userInfo.avatarUrl) {
        const isLocalPath = /^[A-Za-z]:\\/i.test(userInfo.avatarUrl) || /^file:\/\//i.test(userInfo.avatarUrl);
        if (isLocalPath) {
          this.avatarUrl = '/pic/choubi.jpg';
          console.log('检测到本地路径或file:///URL，使用默认头像');
        } else {
          this.avatarUrl = userInfo.avatarUrl;
        }
        console.log('从服务器获取的头像路径:', userInfo.avatarUrl);
      } else {
        this.avatarUrl = '/pic/choubi.jpg';
      }
      console.log('实际使用的头像路径:', this.avatarUrl);
      
      this.regionId = userInfo.regionId || null;
      
      this.username = userInfo.username || "";
      this.nickname = userInfo.nickname || "";
      this.useremail = userInfo.email || "";
      this.userphone = userInfo.phone || "";
      this.usersign = userInfo.bio || "";
      this.usercity = userInfo.regionName || "";
      this.usersex = userInfo.gender || "";

      if (userInfo.birthday) {
        this.userbrithday = new Date(userInfo.birthday);
      }

      this.authorTagId = userInfo.authorTagId || userInfo.author_tags_id || null;
      this.authorTagName = userInfo.authorTagName || '';
      
      if (this.regionId) {
        this.useraddress = this.getRegionNameByCode(this.regionId);
      } else {
        this.useraddress = "";
      }
      
      // 同步数据到 formData
      this.formData.nickname = this.nickname;
      this.formData.userphone = this.userphone;
      this.formData.usersign = this.usersign;
      this.formData.useraddress = this.useraddress;
      this.formData.usersex = this.usersex;
      this.formData.userbrithday = this.userbrithday;
      this.formData.authorTagId = this.authorTagId;
      this.formData.usercity = this.usercity;

      this.loading = false;
      
      // 获取用户信息成功后，获取统计数据
      this.getUserStats();
      this.loadActivityHeatmap();
    },

    // 获取用户统计数据
    async getUserStats() {
      if (!this.userId) {
        console.log('userId 为空，暂不获取统计数据');
        return;
      }

      try {
        // 使用新的 API 获取作者收到的点赞和收藏
        const [likesRes, collectsRes, logsRes, revenueRes] = await Promise.all([
          GetLikesReceivedByAuthor(this.userId),  // 获取作者收到的点赞
          GetCollectsReceivedByAuthor(this.userId), // 获取作者收到的收藏
          GetUserLogs(this.userId),
          GetUserTotalRevenue(this.userId)
        ]);

        const likes = this.extractListData(likesRes);
        const collects = this.extractListData(collectsRes);
        const logs = this.extractListData(logsRes);
        
        // 处理总收益数据
        let totalRevenue = 0;
        if (revenueRes && revenueRes.data !== undefined) {
          totalRevenue = parseFloat(revenueRes.data) || 0;
        } else if (revenueRes && typeof revenueRes === 'number') {
          totalRevenue = revenueRes;
        }

        this.userStats = {
          totalLikes: likes.length,
          totalCollects: collects.length,
          followersCount: 0,
          historyCount: logs.length,
          totalKnowledge: 2, // TODO: 需要从后端获取真实数据
          totalRevenue: totalRevenue
        };
      } catch (error) {
        console.error('获取用户统计数据失败:', error);
        this.userStats = {
          totalLikes: 0,
          totalCollects: 0,
          followersCount: 0,
          historyCount: 0,
          totalKnowledge: 0,
          totalRevenue: 0
        };
      }
    },

    // 获取城市列表
    getCityList() {
      GetAllRegions()
        .then((response) => {
          const regions = Array.isArray(response) ? response : response.data || [];
          this.cityList = this.formatRegionData(regions);
        })
        .catch((error) => {
          console.error("获取城市列表失败:", error);
        });
    },

    // 获取标签列表
    getTagList() {
      GetAllTags()
        .then((response) => {
          const tags = Array.isArray(response) ? response : response.data || [];
          this.tagList = this.formatTagData(tags);
        })
        .catch((error) => {
          console.error("获取标签列表失败:", error);
        });
    },

    // 处理城市选择变化
    handleChange(value) {
      console.log("城市选择变化:", value);
      if (value && value.length > 0) {
        this.regionId = value[value.length - 1];
        this.useraddress = this.getRegionNameByCode(value[value.length - 1]);
        this.formData.usercity = value;
      } else {
        this.regionId = null;
        this.useraddress = "";
        this.formData.usercity = [];
      }
    },

    // 根据地区代码获取地区名称
    getRegionNameByCode(code) {
      function findRegion(regions, code) {
        for (const region of regions) {
          if (region.value === code) {
            return region.label;
          }
          if (region.children) {
            const found = findRegion(region.children, code);
            if (found) {
              return found;
            }
          }
        }
        return "";
      }
      return findRegion(this.cityList, code);
    },

    // 格式化地区数据
    formatRegionData(regions) {
      const regionMap = {};
      regions.forEach((region) => {
        regionMap[region.id] = {
          value: region.id,
          label: region.name,
          children: [],
        };
      });

      const result = [];
      regions.forEach((region) => {
        if (!region.parentId) {
          result.push(regionMap[region.id]);
        } else {
          if (regionMap[region.parentId]) {
            regionMap[region.parentId].children.push(regionMap[region.id]);
          }
        }
      });

      function filterEmptyChildren(regions) {
        return regions.map((region) => {
          const filteredRegion = { ...region };
          if (filteredRegion.children && filteredRegion.children.length > 0) {
            filteredRegion.children = filterEmptyChildren(filteredRegion.children);
          } else {
            delete filteredRegion.children;
          }
          return filteredRegion;
        });
      }

      return filterEmptyChildren(result);
    },

    // 格式化标签数据
    formatTagData(tags) {
      const tagMap = {};
      tags.forEach(tag => {
        tagMap[tag.id] = {
          value: tag.id,
          label: tag.name,
          children: []
        };
      });
      
      const result = [];
      tags.forEach(tag => {
        const parentId = tag.parentId || tag.parent_id;
        if (!parentId) {
          result.push(tagMap[tag.id]);
        } else {
          if (tagMap[parentId]) {
            tagMap[parentId].children.push(tagMap[tag.id]);
          }
        }
      });
      
      function filterEmptyChildren(tags) {
        return tags.map(tag => {
          const filteredTag = { ...tag };
          if (filteredTag.children && filteredTag.children.length > 0) {
            filteredTag.children = filterEmptyChildren(filteredTag.children);
          } else {
            delete filteredTag.children;
          }
          return filteredTag;
        });
      }
      
      return filterEmptyChildren(result);
    },

    // 处理标签选择变化
    handleTagChange(value) {
      console.log('标签选择变化:', value);
      this.authorTagId = value ? value[value.length - 1] : null;
      this.formData.authorTagId = value;
    },

    // 提交表单
    onSubmit() {
      console.log('开始提交表单');
      this.submitting = true;
      
      try {
        if (!this.$refs.form) {
          console.error('表单引用不存在');
          this.$message.error('表单初始化失败，请刷新页面重试');
          this.submitting = false;
          return;
        }
        
        this.$refs.form.validate((valid) => {
          console.log('表单验证结果:', valid);
          if (valid) {
            const avatarData = this.previewAvatarUrl || this.avatarUrl;
            console.log('准备提交的头像URL:', avatarData);
            this.submitUserData(avatarData);
          } else {
            this.$message.error('请填写完整信息');
            this.submitting = false;
          }
        });
      } catch (error) {
        console.error('提交过程中发生错误:', error);
        this.$message.error('提交失败，请刷新页面重试');
        this.submitting = false;
      }
    },

    // 提交用户数据到后端
    async submitUserData(avatarData) {
      try {
        const userData = {
          nickname: this.formData.nickname,
          phone: this.formData.userphone,
          gender: this.formData.usersex || 'other',
          bio: this.formData.usersign,
          regionId: this.regionId ? this.regionId.toString() : null,
          avatarUrl: avatarData,
          birthday: this.formData.userbrithday ? new Date(this.formData.userbrithday).toISOString().split('T')[0] : null,
          authorTagId: this.formData.authorTagId ? this.formData.authorTagId.toString() : null
        };

        console.log('提交的用户数据:', userData);

        const response = await UpdateCurrentUser(userData);
        console.log('更新用户信息成功:', response);

        const updatedData = response.data || response;

        const isLocalPath = avatarData.includes('\\') || avatarData.includes('file:///');
        if (isLocalPath) {
          this.avatarUrl = '/pic/choubi.jpg';
          console.log('检测到本地路径或file:///URL，使用默认头像');
        } else {
          this.avatarUrl = avatarData;
        }
        console.log('本地头像已更新:', this.avatarUrl);

        if (updatedData.nickname) this.nickname = updatedData.nickname;
        if (updatedData.phone) this.userphone = updatedData.phone;
        if (updatedData.bio) this.usersign = updatedData.bio;
        if (updatedData.gender) this.usersex = updatedData.gender;
        if (updatedData.birthday) this.userbrithday = new Date(updatedData.birthday);

        if (this.regionId) {
          this.useraddress = this.getRegionNameByCode(this.regionId);
        }

        if (updatedData.authorTagName) {
          this.authorTagName = updatedData.authorTagName;
        }

        this.$message.success('资料更新成功');
        this.dialogFormVisible = false;

        if (this.previewAvatarUrl && this.previewAvatarUrl.startsWith('blob:')) {
          URL.revokeObjectURL(this.previewAvatarUrl);
          this.previewAvatarUrl = '';
          this.selectedFile = null;
          this.selectedFileName = '';
          this.selectedFileLocalPath = '';
        }

        this.getUserStats();
      } catch (error) {
        console.error('更新用户信息失败:', error);
        
        if (error.response) {
          console.error('错误状态:', error.response.status);
          console.error('错误数据:', error.response.data);
          
          const errorMsg = error.response.data?.message || 
                          error.response.data?.msg || 
                          `服务器错误 ${error.response.status}`;
          this.$message.error(`更新失败: ${errorMsg}`);
          
        } else if (error.request) {
          console.error('网络错误:', error.request);
          this.$message.error('网络错误，无法连接到服务器');
        } else {
          console.error('请求错误:', error.message);
          this.$message.error('请求错误: ' + error.message);
        }
      } finally {
        this.submitting = false;
      }
    },

    // 处理用户命令
    handleUserCommand(command) {
      switch(command) {
        case 'profile':
          this.$router.push('/user')
          break
        case 'blog':
          this.$router.push('/blog')
          break
        case 'circle':
          this.$router.push('/circle')
          break
        case 'knowledge':
          this.handleKnowledgeClick()
          break
        case 'coupons':
          this.$router.push('/coupons')
          break
        case 'pay':
          this.handlePayClick()
          break
        case 'logout':
          this.logout()
          break
      }
    },

    async logout() {
      try {
        const userStore = useUserStore()
        await userStore.logout()
        this.$message.success('已退出登录')
      } catch (error) {
        console.error('退出登录失败:', error)
      } finally {
        this.$router.push('/login')
      }
    },

    extractListData(response) {
      const source = response && response.data !== undefined ? response.data : response;
      if (Array.isArray(source)) return source;
      if (source && Array.isArray(source.records)) return source.records;
      if (source && Array.isArray(source.rows)) return source.rows;
      if (source && Array.isArray(source.list)) return source.list;
      return [];
    },

    normalizeActivityDate(value) {
      if (!value) return null;
      const normalizedValue = typeof value === 'string' ? value.replace(/-/g, '/') : value;
      const date = new Date(normalizedValue);
      if (Number.isNaN(date.getTime())) {
        return null;
      }
      date.setHours(0, 0, 0, 0);
      return date;
    },

    formatDateKey(date) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },

    addDays(date, amount) {
      const nextDate = new Date(date);
      nextDate.setDate(nextDate.getDate() + amount);
      nextDate.setHours(0, 0, 0, 0);
      return nextDate;
    },

    buildActivityHeatmapData(dateList, days = 30) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const startDate = this.addDays(today, -(days - 1));
      const counterMap = {};

      dateList.forEach((value) => {
        const date = this.normalizeActivityDate(value);
        if (!date) return;
        if (date.getTime() < startDate.getTime() || date.getTime() > today.getTime()) return;

        const key = this.formatDateKey(date);
        counterMap[key] = (counterMap[key] || 0) + 1;
      });

      return Object.keys(counterMap)
        .sort()
        .map((date) => ({
          date,
          count: counterMap[date]
        }));
    },

    getActivityHeatmapRules() {
      return {
        commits: { weight: 8, cap: 32 },
        blogs: { weight: 4, cap: 8 },
        posts: { weight: 1, cap: 2 },
        likes: { weight: 1, cap: 1 },
        collects: { weight: 1, cap: 1 },
        logs: { weight: 1, cap: 1 }
      };
    },

    buildWeightedActivityHeatmap(sources, days = 30) {
      const rules = this.getActivityHeatmapRules();
      const breakdownTemplate = Object.keys(rules).reduce((result, key) => {
        result[key] = 0;
        return result;
      }, {});

      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const startDate = this.addDays(today, -(days - 1));
      const groupedMap = {};

      Object.keys(sources).forEach((type) => {
        const values = Array.isArray(sources[type]) ? sources[type] : [];
        values.forEach((item) => {
          const date = this.normalizeActivityDate(item);
          if (!date) return;
          if (date.getTime() < startDate.getTime() || date.getTime() > today.getTime()) return;

          const key = this.formatDateKey(date);
          if (!groupedMap[key]) {
            groupedMap[key] = {
              date: key,
              count: 0,
              eventCount: 0,
              breakdown: { ...breakdownTemplate }
            };
          }

          const { weight: unitWeight = 1, cap: scoreCap = 1 } = rules[type] || {};
          const currentTypeScore = groupedMap[key].breakdown[type] * unitWeight;
          const nextTypeScore = Math.min(currentTypeScore + unitWeight, scoreCap);
          groupedMap[key].count += (nextTypeScore - currentTypeScore);
          groupedMap[key].eventCount += 1;
          groupedMap[key].breakdown[type] += 1;
        });
      });

      return Object.keys(groupedMap)
        .sort()
        .map((date) => groupedMap[date]);
    },

    getActivityDatesByFields(list, fields) {
      return (Array.isArray(list) ? list : [])
        .map((item) => {
          const matchedField = fields.find((field) => item && item[field]);
          return matchedField ? item[matchedField] : null;
        })
        .filter(Boolean);
    },

    stripHtml(html) {
      if (!html) return '';
      return String(html)
        .replace(/<[^>]+>/g, ' ')
        .replace(/&nbsp;/gi, ' ')
        .replace(/\s+/g, ' ')
        .trim();
    },

    isMeaningfulText(text, minLength = 1) {
      const normalized = String(text || '').trim();
      if (normalized.length < minLength) return false;
      if (/^(测试|test|demo|草稿|无标题|111+|aaa+|123+|qwe+)$/i.test(normalized)) return false;
      const denseText = normalized.replace(/\s+/g, '');
      const uniqueChars = new Set(denseText.split(''));
      return uniqueChars.size >= Math.min(6, denseText.length);
    },

    isValidPublishedBlog(blog) {
      if (!blog) return false;

      const status = String(blog.status || blog.auditStatus || blog.publishStatus || '').toLowerCase();
      const published = blog.published === true || ['published', 'approve', 'approved', 'public'].includes(status);
      if (!published) return false;

      const title = String(blog.title || '').trim();
      const summary = this.stripHtml(blog.summary || '');
      const content = this.stripHtml(blog.content || '');
      const totalText = `${title} ${summary} ${content}`.trim();

      if (!this.isMeaningfulText(title, 6)) return false;
      if (!this.isMeaningfulText(totalText, 80)) return false;
      if (content.length < 120 && summary.length < 40) return false;

      return true;
    },

    getPublishedBlogActivityDates(list) {
      return (Array.isArray(list) ? list : [])
        .map((blog) => {
          if (!blog) return null;
          return (
            blog.publishTime ||
            blog.publishedAt ||
            blog.approvedAt ||
            blog.approveTime ||
            blog.auditTime ||
            null
          );
        })
        .filter(Boolean);
    },

    pickPrimaryBranch(branches = []) {
      if (!Array.isArray(branches) || !branches.length) return null;
      return (
        branches.find(branch => branch && (branch.isDefault || branch.defaultFlag || branch.defaultBranchFlag)) ||
        branches.find(branch => ['main', 'master'].includes(String(branch.name || '').toLowerCase())) ||
        branches[0]
      );
    },

    async fetchProjectCommitDates() {
      try {
        const projectRes = await getMyProjects({ page: 1, size: 12 });
        const projectList = Array.isArray(projectRes?.data?.list) ? projectRes.data.list : [];
        if (!projectList.length) return [];

        const branchResults = await Promise.allSettled(
          projectList.map(project => listProjectBranches(project.id))
        );

        const commitTasks = [];
        branchResults.forEach((result, index) => {
          if (result.status !== 'fulfilled') return;
          const branchList = Array.isArray(result.value?.data) ? result.value.data : (Array.isArray(result.value) ? result.value : []);
          const primaryBranch = this.pickPrimaryBranch(branchList);
          if (!primaryBranch) return;
          commitTasks.push(listProjectCommits(projectList[index].id, primaryBranch.id));
        });

        if (!commitTasks.length) return [];

        const commitResults = await Promise.allSettled(commitTasks);
        const commitMap = {};

        commitResults.forEach((result) => {
          if (result.status !== 'fulfilled') return;
          const commitList = Array.isArray(result.value?.data) ? result.value.data : (Array.isArray(result.value) ? result.value : []);
          commitList.forEach((commit) => {
            const uniqueKey = commit.id || commit.commitId || commit.sha || `${commit.message || ''}-${commit.createdAt || ''}`;
            const commitTime = commit.createdAt || commit.createTime || commit.commitTime || commit.authoredAt || null;
            if (!uniqueKey || !commitTime || commitMap[uniqueKey]) return;
            commitMap[uniqueKey] = commitTime;
          });
        });

        return Object.values(commitMap);
      } catch (error) {
        console.error('获取项目提交记录失败:', error);
        return [];
      }
    },

    async loadActivityHeatmap() {
      if (!this.userId) {
        this.activityHeatmapData = [];
        return;
      }

      this.activityLoading = true;

      try {
        const response = await GetUserActivityHeatmap(this.userId, { days: 30 });
        const raw = response && response.data !== undefined ? response.data : response;
        const payload = raw && raw.data !== undefined ? raw.data : raw;
        const activities = Array.isArray(payload && payload.activities) ? payload.activities : [];
        const summary = payload && payload.summary ? payload.summary : {};

        this.activitySummary = {
          commits: Number(summary.commits || 0),
          blogs: Number(summary.blogs || 0),
          posts: Number(summary.posts || 0),
          likes: Number(summary.likes || 0),
          collects: Number(summary.collects || 0),
          logs: Number(summary.logs || 0)
        };

        this.activityHeatmapData = activities.map(item => ({
          date: item.date,
          count: Number(item.count || 0),
          breakdown: item.breakdown || {}
        }));
      } catch (error) {
        console.error('加载活跃热力图失败:', error);
        this.activityHeatmapData = [];
        this.activitySummary = {
          commits: 0,
          blogs: 0,
          posts: 0,
          likes: 0,
          collects: 0,
          logs: 0
        };
      } finally {
        this.activityLoading = false;
      }
    },

    handleHistoryClick() {
      this.$router.push("/history");
    },

    handleCollectClick() {
      this.$router.push("/collection");
    },

    handleKnowledgeClick() {
      if (this.isSelf) {
        this.showMyPosts = true;
        this.postType = 'knowledge';
        this.loadMyPosts();
      } else {
        this.$message.info('暂不支持查看他人知识产品');
      }
    },

    handlePayClick() {
      this.$router.push("/wallet");
    },

    handleCouponsClick() {
      this.$router.push("/coupons");
    },

    // 我的发布相关方法
    handleMyPostsClick() {
      if (this.isSelf) {
        this.showMyPosts = !this.showMyPosts;
        if (this.showMyPosts) {
          this.loadMyPosts();
        }
      } else {
        this.$message.info('暂不支持查看他人发布内容');
      }
    },

    async loadMyPosts() {
      if (!this.isSelf) return;
      
      this.postsLoading = true;
      try {
        if (this.postType === 'blogs') {
          const response = await GetBlogsByAuthorId(this.userId);
          console.log('博客列表响应:', response);
          
          let blogs = [];
          if (response.data && Array.isArray(response.data)) {
            blogs = response.data;
          } else if (Array.isArray(response)) {
            blogs = response;
          }
          
          this.blogList = blogs.map(blog => ({
            id: blog.id,
            title: blog.title,
            summary: blog.summary || (blog.content ? blog.content.substring(0, 100) + '...' : ''),
            viewCount: blog.viewCount || 0,
            likeCount: blog.likeCount || 0,
            createTime: blog.createTime || blog.createdAt,
            status: blog.status || (blog.published ? 'published' : 'draft')
          }));
        } else if (this.postType === 'posts') {
          const response = await GetUserCirclePosts(this.userId);
          console.log('帖子列表响应:', response);
          
          let posts = [];
          if (response.data && Array.isArray(response.data)) {
            posts = response.data;
          } else if (Array.isArray(response)) {
            posts = response;
          }
          
          this.postList = posts.map(post => ({
            id: post.id,
            title: post.title,
            summary: post.summary || (post.content ? post.content.substring(0, 100) + '...' : ''),
            commentCount: post.commentCount || 0,
            likeCount: post.likeCount || 0,
            createTime: post.createTime || post.createdAt
          }));
        } else if (this.postType === 'knowledge') {
          // 调用真实API获取知识产品（付费博客）列表
          const response = await GetMyKnowledgeProducts();
          console.log('知识产品列表响应:', response);
          
          let knowledgeProducts = [];
          if (response.data && Array.isArray(response.data)) {
            knowledgeProducts = response.data;
          } else if (Array.isArray(response)) {
            knowledgeProducts = response;
          }
          
          this.knowledgeList = knowledgeProducts.map(knowledge => ({
            id: knowledge.id,
            title: knowledge.title,
            summary: knowledge.summary || (knowledge.content ? knowledge.content.substring(0, 100) + '...' : ''),
            price: knowledge.price || 0,
            viewCount: knowledge.viewCount || 0,
            likeCount: knowledge.likeCount || 0,
            createTime: knowledge.publishTime || knowledge.createdAt || knowledge.createTime,
            status: knowledge.status || 'draft'
          }));
        }
      } catch (error) {
        console.error('加载发布内容失败:', error);
        this.$message.error('加载失败，请稍后重试');
        this.blogList = [];
        this.postList = [];
        this.knowledgeList = [];
      } finally {
        this.postsLoading = false;
      }
    },

    goToBlogDetail(id) {
      this.$router.push(`/blog/${id}`);
    },

    goToPostDetail(id) {
      this.$router.push(`/circle/${id}`);
    },

    goToKnowledgeDetail(id) {
      this.$router.push(`/blog/${id}`);
    },

    handleCreateKnowledge() {
      this.$router.push('/blogwrite');
    },

    handleEditKnowledge(knowledge) {
      this.$router.push(`/blog/${blog.id}`);
    },

    handleDeleteKnowledge(knowledge) {
      this.$confirm(`确定要删除知识产品《${knowledge.title}》吗？此操作不可恢复！`, '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.deleteKnowledge(knowledge.id);
      }).catch(() => {});
    },

    async deleteKnowledge(knowledgeId) {
      try {
        // 调用真实API删除博客
        const { DeleteBlog } = await import('@/api/index.js');
        await DeleteBlog(knowledgeId);
        
        // 从本地列表中移除
        this.knowledgeList = this.knowledgeList.filter(k => k.id !== knowledgeId);
        this.$message.success('知识产品删除成功');
        
        // 刷新统计数据
        this.getUserStats();
        this.loadActivityHeatmap();
      } catch (error) {
        console.error('删除知识产品失败:', error);
        this.$message.error('删除知识产品失败，请重试');
      }
    },

    handleDeleteBlog(blog) {
      this.$confirm(`确定要删除博客《${blog.title}》吗？此操作不可恢复！`, '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.deleteBlog(blog.id);
      }).catch(() => {});
    },

    async deleteBlog(blogId) {
      try {
        await new Promise(resolve => setTimeout(resolve, 500));
        this.blogList = this.blogList.filter(b => b.id !== blogId);
        this.$message.success('博客删除成功');
        this.getUserStats();
        this.loadActivityHeatmap();
      } catch (error) {
        console.error('删除博客失败:', error);
        this.$message.error('删除博客失败，请重试');
      }
    },

    handleDeletePost(post) {
      this.$confirm(`确定要删除帖子《${post.title}》吗？此操作不可恢复！`, '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.deletePost(post.id);
      }).catch(() => {});
    },

    async deletePost(postId) {
      try {
        await new Promise(resolve => setTimeout(resolve, 500));
        this.postList = this.postList.filter(p => p.id !== postId);
        this.$message.success('帖子删除成功');
        this.getUserStats();
        this.loadActivityHeatmap();
      } catch (error) {
        console.error('删除帖子失败:', error);
        this.$message.error('删除帖子失败，请重试');
      }
    },

    formatDate(date) {
      if (!date) return "";
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    },
  }
}
</script>

<style scoped>
/* 头像选择器样式 */
.avatar-selector {
  margin-top: 20px;
}

.selector-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 10px;
  color: #303133;
}

.avatar-options {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 15px;
}

.avatar-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  width: 80px;
}

.avatar-option:hover {
  background-color: #f5f7fa;
}

.avatar-option.active {
  background-color: #ecf5ff;
  border: 2px solid #409eff;
}

.avatar-name {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  text-align: center;
}

/* 头像预览样式 */
.avatar-preview-wrapper {
  margin-bottom: 15px;
}

.upload-avatar {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.upload-avatar:hover {
  transform: scale(1.05);
}


/* 头像上传器样式 */
.avatar-uploader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.avatar-uploader {
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 120px;
  height: 120px;
  margin: 0 auto 10px;
  transition: all 0.3s ease;
}

.avatar-uploader:hover {
  border-color: #409EFF;
  background: rgba(64, 158, 255, 0.05);
}

.avatar-preview-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: rgba(255, 255, 255, 0.5);
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}

.upload-tip {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 5px;
}

.current-path {
  font-size: 11px;
  color: rgba(64, 158, 255, 0.7);
  word-break: break-all;
  max-width: 300px;
  margin-top: 5px;
}

.user-home-container {
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

.nav-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 30px;
  transition: background 0.3s ease;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.05);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
}

/* ========== 主内容区域 ========== */
.main-content {
  max-width: 1400px;
  margin: 24px auto 0;
  padding: 0 30px;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 24px;
}

/* ========== 左侧个人资料卡片 ========== */
.left-profile {
  position: sticky;
  top: 90px;
  height: fit-content;
}

.profile-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 30px 20px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.profile-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  border-color: rgba(64, 158, 255, 0.2);
}

.profile-header {
  text-align: center;
  margin-bottom: 25px;
}

.avatar-wrapper {
  position: relative;
  width: fit-content;
  margin: 0 auto 20px;
}

.profile-avatar {
  border: 3px solid rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.profile-avatar:hover {
  border-color: #409EFF;
  transform: scale(1.05);
}

.avatar-edit-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 40px;
  height: 40px;
  background: #409EFF;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.3);
}

.avatar-edit-overlay i {
  font-size: 18px;
  color: white;
}

.avatar-edit-overlay span {
  display: none;
}

.avatar-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
  transform: scale(1);
}

.profile-name {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
  background: linear-gradient(135deg, #ffffff, #409EFF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.profile-bio {
  font-size: 14px;
  color: #a0a0a0;
  margin: 0;
  line-height: 1.6;
}

/* 统计信息 */
.profile-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  cursor: pointer;
  padding: 5px 15px;
  border-radius: 12px;
  transition: background 0.3s ease;
}

.stat-item:hover {
  background: rgba(64, 158, 255, 0.1);
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #a0a0a0;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: linear-gradient(to bottom, transparent, rgba(255,255,255,0.1), transparent);
}

/* 个人信息列表 */
.profile-info {
  margin-bottom: 25px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 14px;
  color: #e0e0e0;
  transition: transform 0.2s ease;
}

.info-item:hover {
  transform: translateX(5px);
}

.info-item i {
  width: 20px;
  color: #409EFF;
  font-size: 16px;
}

.info-item.tags {
  flex-wrap: wrap;
}

.info-item .el-tag {
  background: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.3);
  color: #409EFF;
}

/* 编辑按钮 */
.edit-profile-btn {
  width: 100%;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  border: none;
  border-radius: 30px;
  padding: 12px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.edit-profile-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(64, 158, 255, 0.3);
}

/* ========== 中间内容区域 ========== */
.middle-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  border-color: rgba(64, 158, 255, 0.2);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.like-icon {
  background: linear-gradient(135deg, #f093fb, #f5576c);
  color: white;
}

.stat-icon.collect-icon {
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  color: white;
}

.stat-icon.knowledge-icon {
  background: linear-gradient(135deg, #E6A23C, #ebb563);
  color: white;
}

.stat-icon.revenue-icon {
  background: linear-gradient(135deg, #F56C6C, #f78989);
  color: white;
}

.stat-icon.history-icon {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
  color: white;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
}

.stat-label {
  font-size: 12px;
  color: #a0a0a0;
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 0;
  padding: 18px;
}

.action-btn {
  flex: 1 1 160px;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.02);
  color: #ffffff;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  background: rgba(64, 158, 255, 0.1);
  border-color: #409EFF;
  color: #409EFF;
}

.action-btn i {
  margin-right: 6px;
}

/* ========== 我的发布区域样式 ========== */
.my-posts-section {
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 25px;
}

.posts-list {
  min-height: 200px;
  margin-top: 20px;
}

.post-item {
  margin-bottom: 15px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 15px;
}

.post-item:hover {
  transform: translateY(-2px);
  border-color: #409EFF;
}

.post-content {
  flex: 1;
  padding: 20px;
}

.post-content h4 {
  margin: 0 0 10px 0;
  color: #ffffff;
  font-size: 16px;
}

.post-summary {
  font-size: 13px;
  color: #a0a0a0;
  margin: 0 0 10px 0;
  line-height: 1.5;
}

.post-meta {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #a0a0a0;
  align-items: center;
}

.post-meta i {
  margin-right: 3px;
}

.post-actions {
  display: flex;
  gap: 8px;
  margin-left: 10px;
}

.post-actions .el-button {
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.post-actions .el-button:hover {
  opacity: 1;
}

.empty-list {
  text-align: center;
  padding: 40px;
  color: #a0a0a0;
}

.empty-list i {
  font-size: 48px;
  margin-bottom: 10px;
  color: #409EFF;
  opacity: 0.5;
}

.empty-list p {
  font-size: 14px;
  margin: 0;
}

/* 热力图区域 */
.heatmap-section {
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 25px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #ffffff;
}

.section-subtitle {
  font-size: 12px;
  color: #a0a0a0;
}

.heatmap-container {
  min-height: 280px;
}

.heatmap-image {
  width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 右侧内容区域 */
.right-content {
  grid-column: 2;
  min-width: 0;
}

/* ========== 编辑弹窗样式 ========== */
.edit-dialog {
  border-radius: 24px;
  overflow: hidden;
}

.edit-dialog :deep(.el-dialog) {
  background: rgba(20, 20, 20, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
  z-index: 2000;
}

.edit-dialog :deep(.el-dialog__title) {
  color: #ffffff;
  font-weight: 600;
}

.edit-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #a0a0a0;
}

.edit-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #409EFF;
}

.edit-dialog :deep(.el-dialog__body) {
  padding: 30px;
  max-height: 60vh;
  overflow-y: auto;
}

.edit-form :deep(.el-form-item__label) {
  color: #a0a0a0;
}

.edit-form :deep(.el-input__inner),
.edit-form :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.edit-form :deep(.el-input__inner:focus),
.edit-form :deep(.el-textarea__inner:focus) {
  border-color: #409EFF;
  background: rgba(255, 255, 255, 0.08);
}

.edit-form :deep(.el-input__prefix) {
  color: #a0a0a0;
}

.edit-form :deep(.el-radio__label) {
  color: #ffffff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
}

.dialog-footer .el-button {
  border-radius: 20px;
  padding: 10px 25px;
}

.dialog-footer .el-button:first-child {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #a0a0a0;
}

.dialog-footer .el-button:first-child:hover {
  border-color: #409EFF;
  color: #409EFF;
}

/* ========== 页脚 ========== */
.footer {
  margin-top: 50px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  background: rgba(10, 10, 10, 0.8);
  backdrop-filter: blur(10px);
}

/* ========== 响应式设计 ========== */
@media screen and (max-width: 1200px) {
  .main-content {
    grid-template-columns: 300px minmax(0, 1fr);
    gap: 20px;
  }

  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 992px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .left-profile,
  .right-content {
    position: static;
  }

  .right-content {
    grid-column: auto;
  }
  
  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 768px) {
  .navbar-content {
    padding: 10px 20px;
  }
  
  .main-content {
    padding: 0 20px;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .username {
    display: none;
  }
  
  .post-meta {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .post-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .post-actions {
    width: 100%;
    justify-content: flex-end;
    padding: 0 20px 20px;
    margin-left: 0;
  }
}
</style>
<style scoped>
.user-home-container {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.12), transparent 22%),
    linear-gradient(180deg, #06101b 0%, #0a1626 45%, #07111d 100%);
}

.user-home-container::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.04) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.72), transparent 90%);
}

.navbar,
.profile-card,
.stat-card,
.action-buttons,
.my-posts-section,
.heatmap-section,
.right-content,
.footer {
  background: rgba(8, 15, 29, 0.74) !important;
  border-color: rgba(148, 163, 184, 0.16) !important;
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.34);
  backdrop-filter: blur(22px);
}

.right-content {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  padding: 22px;
}

.profile-name,
.stat-number,
.section-header h3,
.post-content h4 {
  color: #f8fafc !important;
}

.profile-bio,
.stat-label,
.info-item span,
.section-subtitle,
.post-summary,
.post-meta,
.empty-list,
.dialog-footer,
.edit-form :deep(.el-form-item__label) {
  color: #cbd5e1 !important;
}

.profile-stats,
.stat-divider,
.post-item,
.comment-thread {
  border-color: rgba(148, 163, 184, 0.12) !important;
}

.stat-item,
.info-item i,
.post-meta i {
  color: #7dd3fc !important;
}

.action-buttons {
  border-radius: 24px;
}

.action-btn {
  border-radius: 999px !important;
}

.action-btn.el-button--warning,
.action-btn.el-button--success,
.action-btn.el-button--primary,
.action-btn.el-button--info,
.edit-profile-btn {
  background: linear-gradient(135deg, #0ea5e9, #2563eb) !important;
  border-color: transparent !important;
  color: #eff6ff !important;
  box-shadow: 0 16px 30px rgba(14, 165, 233, 0.2);
}

.post-item,
.empty-list {
  background: rgba(15, 23, 42, 0.64) !important;
  border-color: rgba(148, 163, 184, 0.14) !important;
}

.post-item:hover {
  border-color: rgba(125, 211, 252, 0.24) !important;
  box-shadow: 0 20px 40px rgba(2, 6, 23, 0.34) !important;
}

.heatmap-image {
  border-color: rgba(148, 163, 184, 0.14) !important;
}

.edit-dialog :deep(.el-dialog) {
  background: linear-gradient(180deg, rgba(8, 15, 29, 0.96), rgba(12, 23, 39, 0.94)) !important;
  border: 1px solid rgba(148, 163, 184, 0.18) !important;
}

.edit-dialog :deep(.el-dialog__title),
.edit-dialog :deep(.avatar-name),
.edit-dialog :deep(.selector-title) {
  color: #f8fafc !important;
}

.edit-form :deep(.el-input__inner),
.edit-form :deep(.el-textarea__inner),
.edit-form :deep(.el-date-editor .el-input__inner),
.edit-form :deep(.el-cascader .el-input__inner) {
  background: rgba(2, 6, 23, 0.6) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  color: #e2e8f0 !important;
}

.edit-form :deep(.el-input__inner:focus),
.edit-form :deep(.el-textarea__inner:focus) {
  border-color: rgba(125, 211, 252, 0.45) !important;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.16) !important;
}

.edit-form :deep(.el-radio__label),
.edit-form :deep(.el-tag) {
  color: #cbd5e1 !important;
}

.dialog-footer .el-button:last-child {
  background: linear-gradient(135deg, #0ea5e9, #2563eb) !important;
  border-color: transparent !important;
}
</style>

<style scoped>
.user-home-container {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
}

.user-home-container::before {
  background-image:
    linear-gradient(var(--it-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--it-grid-line) 1px, transparent 1px) !important;
}

.navbar,
.profile-card,
.stat-card,
.action-buttons,
.my-posts-section,
.heatmap-section,
.right-content,
.post-item,
.empty-list,
.footer {
  background: var(--it-surface) !important;
  border: 1px solid var(--it-border) !important;
  border-radius: var(--it-radius-card) !important;
  box-shadow: var(--it-shadow) !important;
}

.navbar {
  background: var(--it-header-bg) !important;
  border-top: 0 !important;
  border-left: 0 !important;
  border-right: 0 !important;
  border-radius: 0 !important;
}

.navbar-scrolled {
  box-shadow: var(--it-shadow-strong) !important;
}

.logo-icon,
.stat-value,
.info-item i,
.post-meta i,
.empty-list i {
  color: var(--it-accent) !important;
}

.logo-text,
.profile-name {
  background: var(--it-primary-gradient) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
}

.username,
.user-info i,
.stat-number,
.section-header h3,
.post-content h4,
.edit-dialog :deep(.el-dialog__title),
.edit-dialog :deep(.avatar-name),
.edit-dialog :deep(.selector-title) {
  color: var(--it-text) !important;
}

.profile-bio,
.stat-label,
.info-item,
.info-item span,
.section-subtitle,
.post-summary,
.post-meta,
.empty-list,
.upload-tip,
.current-path,
.edit-form :deep(.el-form-item__label),
.edit-form :deep(.el-radio__label),
.edit-form :deep(.el-tag) {
  color: var(--it-text-muted) !important;
}

.user-info:hover,
.stat-item:hover,
.action-btn:hover,
.avatar-option:hover,
.avatar-option.active {
  background: var(--it-accent-soft) !important;
}

.profile-stats,
.post-stats,
.stat-divider {
  border-color: var(--it-border) !important;
}

.avatar-edit-overlay,
.edit-profile-btn,
.action-btn.el-button--warning,
.action-btn.el-button--success,
.action-btn.el-button--primary,
.action-btn.el-button--info,
.dialog-footer .el-button:last-child {
  background: var(--it-primary-gradient) !important;
  border-color: transparent !important;
  color: #ffffff !important;
  border-radius: var(--it-radius-control) !important;
}

.action-btn {
  background: var(--it-surface-solid) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
  border-radius: var(--it-radius-control) !important;
}

.post-item:hover,
.profile-card:hover,
.stat-card:hover {
  border-color: var(--it-border-strong) !important;
  box-shadow: var(--it-shadow-strong) !important;
}

.edit-dialog :deep(.el-dialog) {
  background: var(--it-surface-solid) !important;
  border: 1px solid var(--it-border) !important;
  border-radius: var(--it-radius-card) !important;
  box-shadow: var(--it-shadow-strong) !important;
}

.edit-form :deep(.el-input__inner),
.edit-form :deep(.el-textarea__inner),
.edit-form :deep(.el-date-editor .el-input__inner),
.edit-form :deep(.el-cascader .el-input__inner) {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}

.edit-form :deep(.el-input__inner:focus),
.edit-form :deep(.el-textarea__inner:focus) {
  border-color: var(--it-accent) !important;
  box-shadow: 0 0 0 3px var(--it-accent-soft) !important;
}
</style>
