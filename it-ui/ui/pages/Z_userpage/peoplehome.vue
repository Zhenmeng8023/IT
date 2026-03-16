<template>
  <div class="user-home-container">
    <!-- ========== 顶部导航栏 ========== -->
    <nav class="navbar" :class="{ 'navbar-scrolled': scrolled }">
      <div class="navbar-content">
        <div class="logo" @click="scrollToTop">
          <span class="logo-icon">●</span>
          <span class="logo-text">TechSpace</span>
        </div>
        <div class="nav-actions">
          <NotificationBell />
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="40" :src="avatarUrl"></el-avatar>
              <span class="username">{{ nickname || username }}</span>
              <i class="el-icon-arrow-down"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人主页</el-dropdown-item>
              <el-dropdown-item command="blog">我的博客</el-dropdown-item>
              <el-dropdown-item command="circle">我的圈子</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </nav>

    <!-- ========== 用户主页主体 ========== -->
    <div class="main-content">
      <!-- 左侧个人资料卡片 -->
      <div class="left-profile">
        <div class="profile-card">
          <div class="profile-header">
            <div class="avatar-wrapper">
              <el-avatar :size="180" :src="avatarUrl" class="profile-avatar"></el-avatar>
              <div class="avatar-edit-overlay" @click="dialogFormVisible = true">
                <i class="el-icon-edit"></i>
                <span>编辑资料</span>
              </div>
            </div>
            <h2 class="profile-name">{{ nickname || username }}</h2>
            <p class="profile-bio">{{ usersign || '这个用户很懒，什么都没留下~' }}</p>
          </div>

          <div class="profile-stats">
            <div class="stat-item" @click="handleCollectClick">
              <span class="stat-value">{{ value2 }}</span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" @click="handleHistoryClick">
              <span class="stat-value">{{ value1 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ followersCount || 128 }}</span>
              <span class="stat-label">粉丝</span>
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

          <el-button type="primary" class="edit-profile-btn" @click="dialogFormVisible = true">
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
        >
          <el-form ref="form" label-width="80px" :model="formData" class="edit-form">
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
              <span class="stat-number">{{ value1 }}</span>
              <span class="stat-label">总获赞</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon collect-icon">
              <i class="el-icon-collection"></i>
            </div>
            <div class="stat-info">
              <span class="stat-number">{{ value2 }}</span>
              <span class="stat-label">收藏数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon history-icon">
              <i class="el-icon-time"></i>
            </div>
            <div class="stat-info">
              <span class="stat-number">{{ historyCount || 56 }}</span>
              <span class="stat-label">浏览历史</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮组 -->
        <div class="action-buttons">
          <el-button type="primary" plain @click="handleHistoryClick" class="action-btn">
            <i class="el-icon-time"></i> 历史记录
          </el-button>
          <el-button type="warning" plain @click="handleCollectClick" class="action-btn">
            <i class="el-icon-star-off"></i> 我的收藏
          </el-button>
          <el-button type="success" plain class="action-btn">
            <i class="el-icon-s-promotion"></i> 我的发布
          </el-button>
        </div>

        <!-- 热力图区域 -->
        <div class="heatmap-section">
          <div class="section-header">
            <h3>活动热力图</h3>
            <span class="section-subtitle">近30天活跃度</span>
          </div>
          <div class="heatmap-container">
            <!-- 这里保留原有的热力图组件 -->
            <div class="block">
              <el-image src="/pic/choubi.jpg" class="heatmap-image"></el-image>
            </div>
          </div>
        </div>

        <!-- 最近活动 -->
        <div class="recent-activity">
          <div class="section-header">
            <h3>最近动态</h3>
            <span class="section-subtitle">最新互动记录</span>
          </div>
          <div class="activity-list">
            <div class="activity-item" v-for="i in 5" :key="i">
              <div class="activity-icon">
                <i class="el-icon-chat-dot-round"></i>
              </div>
              <div class="activity-content">
                <p>你收到了来自 <span class="activity-user">前端小王子</span> 的评论</p>
                <span class="activity-time">5分钟前</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="right-content">
        <ContentSection />
      </div>
    </div>

    <!-- ========== 页脚 ========== -->
    <footer class="footer">
      <FooterPlayer />
    </footer>
  </div>
</template>

<script>
import NotificationBell from '@/components/NotificationBell.vue'
import HeaderGreeting from '../Z_userpage/components/HeaderGreeting.vue'
import Calendar from '../Z_userpage/components/Calendar.vue'
import ContentSection from '../Z_userpage/components/ContentSection.vue'
import FooterPlayer from '../Z_userpage/components/FooterPlayer.vue'
import { getToken } from '@/utils/auth';
import { GetCurrentUser, GetAllRegions, GetAllTags, UpdateCurrentUser } from '@/api/index.js'

export default {
  name: 'UserHome',
  layout: 'default',
  components: {
    NotificationBell,
    HeaderGreeting,
    Calendar,
    ContentSection,
    FooterPlayer
  },
  data() {
    return {
      // 滚动状态
      scrolled: false,
      
      // 用户数据
      imageUrl: '',
      value1: 12322,
      value2: 1222,
      title: '',
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
      
      // 额外数据
      followersCount: 128,
      historyCount: 56,
      
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
      }
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  created() {
    this.getUserInfo();
    this.getCityList();
    this.getTagList();
  },
  methods: {
    handleScroll() {
      this.scrolled = window.scrollY > 50
    },
    scrollToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },

    // 获取用户信息
    getUserInfo() {
      this.loading = true;
      this.error = "";

      if (!this.$axios) {
        console.error("axios实例未找到");
        this.error = "系统错误，请刷新页面重试";
        this.loading = false;
        this.$message.error("系统错误，请刷新页面重试");
        return;
      }

      if (process.client) {
        const token = getToken();
        console.log("获取到的token:", token);

        GetCurrentUser()
          .then((response) => {
            console.log("获取用户信息成功:", response);
            const userInfo = response;
            console.log("用户信息:", userInfo);

            this.userId = userInfo.id || "";
            this.avatarUrl = userInfo.avatarUrl || this.avatarUrl;
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
            
            this.formData.nickname = this.nickname;
            this.formData.userphone = this.userphone;
            this.formData.usersign = this.usersign;
            this.formData.useraddress = this.useraddress;
            this.formData.usersex = this.usersex;
            this.formData.userbrithday = this.userbrithday;
            this.formData.authorTagId = this.authorTagId;
            this.formData.usercity = this.usercity;

            this.loading = false;
          })
          .catch((error) => {
            console.error("获取用户信息失败:", error);
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
      } else {
        this.loading = false;
      }
    },

    // 获取城市列表
    getCityList() {
      GetAllRegions()
        .then((response) => {
          console.log("获取城市列表成功:", response);
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
          console.log("获取标签列表成功:", response);
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
      } else {
        this.regionId = null;
        this.useraddress = "";
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
        if (!tag.parent_id) {
          result.push(tagMap[tag.id]);
        } else {
          if (tagMap[tag.parent_id]) {
            tagMap[tag.parent_id].children.push(tagMap[tag.id]);
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
            const userData = {
              nickname: this.formData.nickname,
              phone: this.formData.userphone,
              gender: this.formData.usersex || 'other',
              bio: this.formData.usersign,
              regionId: this.regionId ? this.regionId.toString() : null,
              avatarUrl: this.avatarUrl,
              birthday: this.formData.userbrithday ? new Date(this.formData.userbrithday).toISOString().split('T')[0] : null,
              authorTagId: this.formData.authorTagId ? this.formData.authorTagId.toString() : null
            };
            
            if (!this.$axios) {
              console.error('axios实例不存在');
              this.$message.error('网络请求失败，请刷新页面重试');
              this.submitting = false;
              return;
            }
            
            UpdateCurrentUser(userData)
              .then(response => {
                console.log('更新用户信息成功:', response);
                this.$message.success('资料更新成功');
                this.dialogFormVisible = false;
                this.getUserInfo();
              })
              .catch(error => {
                console.error('更新用户信息失败:', error);
                this.$message.error('更新失败，请重试');
              })
              .finally(() => {
                this.submitting = false;
              });
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
        case 'settings':
          this.$router.push('/settings')
          break
        case 'logout':
          this.logout()
          break
      }
    },

    logout() {
      // 退出登录逻辑
      localStorage.removeItem('token')
      this.$router.push('/login')
    },

    // 处理历史记录点击
    handleHistoryClick() {
      this.$router.push("/history");
    },

    // 处理收藏点击
    handleCollectClick() {
      this.$router.push("/collection");
    },

    // 格式化日期
    formatDate(date) {
      if (!date) return "";
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return `${year}年${month}月${day}日`;
    },
  }
}
</script>

<style scoped>
/* ========== 全局样式 ========== */
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
  z-index: 100;
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
  margin: 30px auto;
  padding: 0 30px;
  display: grid;
  grid-template-columns: 350px 1fr 300px;
  gap: 30px;
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
  gap: 30px;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
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
  gap: 15px;
  margin: 10px 0;
}

.action-btn {
  flex: 1;
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
  align-items: baseline;
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
  min-height: 200px;
}

.heatmap-image {
  width: 100%;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 最近活动 */
.recent-activity {
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 25px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  border-radius: 12px;
  transition: background 0.3s ease;
}

.activity-item:hover {
  background: rgba(255, 255, 255, 0.03);
}

.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409EFF;
}

.activity-content {
  flex: 1;
}

.activity-content p {
  margin: 0 0 4px;
  font-size: 14px;
  color: #e0e0e0;
}

.activity-user {
  color: #409EFF;
  font-weight: 500;
}

.activity-time {
  font-size: 12px;
  color: #a0a0a0;
}

/* ========== 右侧内容区域 ========== */
.right-content {
  position: sticky;
  top: 90px;
  height: fit-content;
}

/* ========== 编辑弹窗样式 ========== */
.edit-dialog {
  border-radius: 24px;
  overflow: hidden;
}

.edit-dialog :deep(.el-dialog) {
  background: rgba(20, 20, 20, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
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
    grid-template-columns: 300px 1fr 250px;
    gap: 20px;
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
  
  .stats-cards {
    grid-template-columns: repeat(3, 1fr);
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
  
  .action-buttons {
    flex-direction: column;
  }
  
  .username {
    display: none;
  }
}
</style>