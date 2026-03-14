<template>
  <div class="write-blog-container">
    <!-- 头部区域：左侧头像，右侧操作按钮（新增草稿箱按钮） -->
    <div class="write-header">
      <div class="user-info" @click="goToUserHome">
        <el-avatar :size="50" :src="userAvatar"></el-avatar>
        <span class="username">{{ username }}</span>
      </div>
      <div class="action-buttons">
        <!-- 草稿箱按钮（新增） -->
        <el-button type="warning" plain @click="openDraftDrawer" :loading="loadingDrafts">
          草稿箱
        </el-button>
        <span v-if="lastSaved" class="save-tip">最后保存：{{ lastSaved }}</span>
        <el-button type="info" plain @click="saveDraft" :loading="savingDraft">
          存草稿
        </el-button>
        <el-button type="primary" @click="publishBlog" :loading="publishing">
          发布
        </el-button>
      </div>
    </div>

    <!-- 博客编辑主体区域 -->
    <div class="edit-area">
      <el-input
        class="title-input"
        v-model="blog.title"
        placeholder="请输入博客标题"
        maxlength="100"
        show-word-limit
        clearable
      ></el-input>

      <!-- 标签选择器 -->
      <div class="tag-selector">
        <span class="tag-label">标签：</span>
        <el-select
          v-model="blog.tags"
          multiple
          placeholder="请选择标签"
          class="tag-select"
          clearable
          :loading="loadingTags"
        >
          <el-option
            v-for="tag in tagOptions"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          ></el-option>
        </el-select>
      </div>

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

    <!-- 草稿箱抽屉（新增） -->
    <el-drawer
      title="我的草稿"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="400px"
      :before-close="handleDrawerClose"
    >
      <div class="draft-list" v-loading="loadingDrafts">
        <el-card
          v-for="draft in draftList"
          :key="draft.id"
          class="draft-item"
          shadow="hover"
          @click.native="editDraft(draft.id)"
        >
          <h4>{{ draft.title || '无标题' }}</h4>
          <p class="draft-summary">{{ draft.summary || draft.content.slice(0, 50) + '...' }}</p>
          <div class="draft-meta">
            <span>最后更新：{{ formatTime(draft.updatedAt) }}</span>
          </div>
        </el-card>

        <!-- 分页 -->
        <el-pagination
          v-if="draftTotal > pageSize"
          background
          layout="prev, pager, next"
          :total="draftTotal"
          :page-size="pageSize"
          :current-page.sync="draftPage"
          @current-change="fetchDrafts"
          class="draft-pagination"
        ></el-pagination>

        <div v-if="draftList.length === 0 && !loadingDrafts" class="empty-draft">
          暂无草稿
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
export default {
  name: 'WriteBlog',
  data() {
    return {
      // ---------- 博客数据 ----------
      blog: {
        id: null,
        title: '',
        content: '',
        tags: [],
        status: 'draft',
      },
      // ---------- 标签选项 ----------
      tagOptions: [],
      loadingTags: false,

      // ---------- 用户信息 ----------
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      username: '当前用户',
      userId: '',

      // ---------- 界面状态 ----------
      savingDraft: false,
      publishing: false,
      lastSaved: '',

      // ---------- 草稿箱相关（新增）----------
      drawerVisible: false,        // 抽屉显示状态
      draftList: [],               // 草稿列表
      loadingDrafts: false,        // 加载草稿中
      draftPage: 1,                // 当前页码
      pageSize: 10,                // 每页条数
      draftTotal: 0,               // 草稿总数
    };
  },
  created() {
    this.fetchTags();
    this.loadUserInfo();
    const blogId = this.$route.params.id;
    if (blogId) {
      this.fetchBlog(blogId);
    }
  },
  methods: {
    // 跳转到用户主页
    goToUserHome() {
      this.$router.push(`/user/${this.userId}`);
    },

    loadUserInfo() {

      console.log('loadUserInfo方法被调用');
      
      this.fetchUserInfoFromApi();
    },

    async fetchUserInfoFromApi() {
      console.log('开始调用API获取用户信息...');
      try {
        const response = await this.$axios.get('/api/users/current');
        console.log('API响应:', response);
        console.log('API响应类型:', typeof response);
        console.log('API响应数据:', response.data);
        console.log('API响应状态:', response.status);
        
        // 从之前的调试输出看到，实际的用户数据就在response对象中
        // 有两种可能：1. response就是用户数据对象；2. response.data包含用户数据
        let userData = null;
        
        // 检查response本身是否包含用户信息字段
        if (response && response.id) {
          userData = response;
        } 
        // 检查response.data是否包含用户信息字段
        else if (response && response.data && response.data.id) {
          userData = response.data;
        }
        // 检查是否是标准格式 {code: 0, data: {...}}
        else if (response && response.data && response.data.code !== undefined) {
          if (response.data.code === 0 && response.data.data && response.data.data.id) {
            userData = response.data.data;
          } else {
            console.error('获取用户信息失败：', response.data.message || '未知错误');
            return;
          }
        }
        
        console.log('最终解析的用户数据:', userData);
        
        if (userData && userData.id) {
          // 正确获取到用户数据
          this.userId = userData.id;
          // 优先使用昵称，如果昵称为null或undefined则使用用户名
          this.username = userData.nickname || userData.username || userData.displayName || '当前用户';
          // 设置头像URL
          this.userAvatar = userData.avatarUrl || userData.avatar || userData.headImgUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';

          console.log('设置的用户信息:', {
            userId: this.userId,
            username: this.username,
            userAvatar: this.userAvatar
          });

          // 同时更新本地存储和vuex，以便后续使用
          if (this.$store && this.$store.commit) {
            this.$store.commit('user/setUserInfo', userData);
          }

          // 安全地存储到localStorage
          try {
            localStorage.setItem('userInfo', JSON.stringify(userData));
          } catch (storageError) {
            console.error('存储用户信息到localStorage失败:', storageError);
          }
        } else {
          console.warn('未获取到有效的用户数据:', userData);
          console.warn('响应结构可能不符合预期，请检查API返回的数据格式');
        }
      } catch (error) {
        console.error('通过API获取用户信息失败:', error);
        console.error('错误详情:', error.response || error.message);
        
        // 检查是否是404错误（用户未登录）
        if (error.response) {
          console.error('HTTP状态码:', error.response.status);
          console.error('错误响应数据:', error.response.data);
          
          if (error.response.status === 404) {
            console.warn('用户未登录或登录已过期');
          }
        }
        
        // 如果API调用失败，仍然保持默认值
        if (!this.userId) {
          this.userId = '';
        }
        if (!this.username || this.username === '当前用户') {
          this.username = '当前用户';
        }
        if (!this.userAvatar) {
          this.userAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
        }
      }
    },
    // 获取标签列表
    async fetchTags() {
      this.loadingTags = true;
      try {
        console.log('开始请求标签数据...');
        const res = await this.$axios.get('/api/common/tags');
        console.log('API返回数据:', res);

        // 根据实际返回的数据结构处理
        if (Array.isArray(res)) {
          // 如果整个响应就是一个数组
          this.tagOptions = res;
          console.log('成功获取标签数据:', this.tagOptions);
        } else if (Array.isArray(res.data)) {
          // 如果响应的data字段是数组（常见情况）
          this.tagOptions = res.data;
          console.log('成功获取标签数据:', this.tagOptions);
        } else if (res.data && typeof res.data === 'object' && res.data.code === 0) {
          // 如果是标准格式 {code: 0, data: [...]}
          this.tagOptions = res.data.data || [];
          console.log('成功获取标签数据:', this.tagOptions);
        } else {
          // 尝试其他可能的数据结构
          if (res && typeof res === 'object' && Array.isArray(res.tags)) {
            this.tagOptions = res.tags;
            console.log('以tags字段获取标签数据:', this.tagOptions);
          } else {
            // 如果是字符串响应，尝试解析JSON
            if (typeof res.data === 'string') {
              try {
                const parsedData = JSON.parse(res.data);
                if (Array.isArray(parsedData)) {
                  this.tagOptions = parsedData;
                } else if (parsedData && parsedData.code === 0 && Array.isArray(parsedData.data)) {
                  this.tagOptions = parsedData.data;
                }
              } catch (parseError) {
                console.error('解析响应数据失败:', parseError);
                this.$message.error('数据格式错误');
                return;
              }
            } else {
              console.warn('未识别的API返回格式:', res);
              // 尝试直接使用res作为标签数组
              if(Array.isArray(res)) {
                this.tagOptions = res;
              } else {
                this.$message.warning('标签数据格式异常');
              }
            }
          }
        }
      } catch (error) {
        console.error('请求错误:', error);
        if (error.response) {
          // 服务器响应了错误状态码
          console.error('响应错误:', error.response.status, error.response.data);
          this.$message.error(`API错误: ${error.response.status} - ${error.response.data?.message || '请求失败'}`);
        } else if (error.request) {
          // 请求发出但没有收到响应
          console.error('网络错误:', error.request);
          this.$message.error('网络错误：无法连接到服务器');
        } else {
          // 其他错误
          console.error('其他错误:', error.message);
          this.$message.error('请求配置错误：' + error.message);
        }
      } finally {
        this.loadingTags = false;
      }
    },

    // 获取博客详情（编辑模式）
     // 获取博客详情（编辑模式）
     async fetchBlog(blogId) {
      try {
        const res = await this.$axios.get(`/api/blog/${blogId}`);
        
        console.log('获取博客详情响应:', res); // 添加调试日志
        
        if (res && typeof res === 'object') {
          let blogData = null;
          
          if (res.data && typeof res.data.code !== 'undefined') {
            // 标准格式 {code: 0, data: {...}}
            if (res.data.code === 0) {
              blogData = res.data.data;
            } else {
              this.$message.error('获取博客失败：' + res.data.message);
              return;
            }
          } else {
            // 直接返回数据格式
            blogData = res;
          }
          
          if (blogData) {
            this.blog = {
              id: blogData.id,
              title: blogData.title,
              content: blogData.content,
              tags: Array.isArray(blogData.tags) ? blogData.tags : (blogData.tags ? [blogData.tags] : []), // 确保tags是数组
              status: blogData.status,
            };
          }
        }
      } catch (error) {
        console.error(error);
        this.$message.error('网络错误，请稍后重试');
      }
    },

    async saveBlog(status) {
      // 校验标题和内容
      if (!this.blog.title.trim()) {
        this.$message.warning('请填写博客标题');
        return false;
      }
      if (!this.blog.content.trim()) {
        this.$message.warning('请填写博客内容');
        return false;
      }

      const isPublish = status === 'published';
      if (isPublish) {
        this.publishing = true;
      } else {
        this.savingDraft = true;
      }

      try {
        // 处理标签：将标签ID数组传递给后端
        let tagIds = [];
        if (this.blog.tags && Array.isArray(this.blog.tags)) {
          // 如果this.blog.tags是标签对象数组（例如{id: 1, name: "JavaScript"}）
          if (this.blog.tags.length > 0 && typeof this.blog.tags[0] === 'object') {
            // 从标签对象中提取ID
            tagIds = this.blog.tags.map(tag => {
              // 尝试获取ID，如果不存在则尝试从标签选项中查找
              if (tag.id) {
                return tag.id;
              } else if (tag.name) {
                // 在标签选项中查找对应ID
                const tagOption = this.tagOptions.find(option => option.name === tag.name);
                return tagOption ? tagOption.id : null;
              }
              return null;
            }).filter(id => id !== null);
          } 
          // 如果this.blog.tags是字符串数组（例如["JavaScript", "Vue"]）
          else if (this.blog.tags.length > 0 && typeof this.blog.tags[0] === 'string') {
            // 从标签选项中查找对应的ID
            tagIds = this.blog.tags.map(tagName => {
              const tagOption = this.tagOptions.find(option => option.name === tagName || option.id === tagName);
              return tagOption ? tagOption.id : null;
            }).filter(id => id !== null);
          }
          // 如果this.blog.tags已经是ID数组（例如[1, 2, 3]）
          else if (this.blog.tags.length > 0 && typeof this.blog.tags[0] === 'number') {
            tagIds = this.blog.tags.filter(id => id !== undefined);
          }
        }

        // 构建请求数据
        const requestData = {
          title: this.blog.title,
          content: this.blog.content,
          status: status,
          tagIds: tagIds  // 发送标签ID数组而不是标签对象数组
        };

        // 如果不是编辑现有博客且userId存在，添加userId
        if (!this.blog.id && this.userId) {
          requestData.userId = this.userId;
        }

        console.log('发送的请求数据:', requestData); // 调试日志
        console.log('当前用户ID:', this.userId); // 调试日志
        console.log('要发送的标签IDs:', tagIds); // 调试日志

        let res;
        if (this.blog.id) {
          // 编辑模式：PUT /api/blog/:id
          console.log('编辑博客，ID:', this.blog.id);
          res = await this.$axios.put(`/api/blog/${this.blog.id}`, requestData);
        } else {
          // 新建模式：POST /api/blog
          console.log('创建新博客');
          res = await this.$axios.post('/api/blog', requestData);
        }

        console.log('保存博客响应:', res); // 调试日志
        console.log('响应类型:', typeof res);
        console.log('响应是否为对象:', typeof res === 'object');

        // 从日志来看，res 就是后端返回的博客对象，而不是 axios 的响应对象
        if (res && typeof res === 'object') {
          // 响应就是博客数据
          const result = res;
          
          // 更新本地博客数据
          if (!this.blog.id) {
            this.blog.id = result.id || result._id; // 兼容不同ID字段名
          }
          this.blog.status = result.status || status;

          // 如果是存草稿，记录保存时间
          if (status === 'draft') {
            const now = new Date();
            this.lastSaved = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
            this.$message.success('草稿保存成功');
          } else {
            this.$message.success('发布成功');
          }
          return true;
        } else {
          this.$message.error((isPublish ? '发布' : '保存草稿') + '失败：服务器返回无效数据');
          return false;
        }
      } catch (error) {
        console.error('保存博客出错', error);
        console.error('错误详情:', error.message);
        console.error('错误堆栈:', error.stack);
        
        if (error.response) {
          console.error('错误响应数据:', error.response.data);
          console.error('错误响应状态:', error.response.status);
          console.error('错误响应头:', error.response.headers);
          
          let errorMessage = '未知错误';
          if (error.response.data) {
            if (typeof error.response.data === 'string') {
              errorMessage = error.response.data;
            } else if (typeof error.response.data.message === 'string') {
              errorMessage = error.response.data.message;
            } else if (typeof error.response.data.msg === 'string') {
              errorMessage = error.response.data.msg;
            } else if (typeof error.response.data.errMsg === 'string') {
              errorMessage = error.response.data.errMsg;
            }
          }
          
          this.$message.error((isPublish ? '发布' : '保存草稿') + '失败：' + errorMessage);
        } else if (error.request) {
          // 请求已发出但没有收到响应
          console.error('请求错误详情:', error.request);
          this.$message.error('网络错误，请检查连接');
        } else {
          // 其他错误
          console.error('请求配置错误:', error.message);
          this.$message.error('请求错误：' + error.message);
        }
        return false;
      } finally {
        if (isPublish) {
          this.publishing = false;
        } else {
          this.savingDraft = false;
        }
      }
    },

    saveDraft() {
      this.saveBlog('draft');
    },

    publishBlog() {
      this.saveBlog('published');
    },

    // ---------- 草稿箱相关方法（新增）----------
    // 打开抽屉并加载草稿
    openDraftDrawer() {
      this.drawerVisible = true;
      this.draftPage = 1; // 重置到第一页
      this.fetchDrafts();
    },

    // 获取草稿列表
    async fetchDrafts() {
      this.loadingDrafts = true;
      try {
        // 草稿接口：GET /api/blogs?status=draft&page=...&limit=...
        const res = await this.$axios.get('/api/blog/draft', {
          params: {
            status: 'draft',
            page: this.draftPage,
            limit: this.pageSize,
          },
        });
        
        console.log('获取草稿响应:', res); // 调试日志
        
        let draftsData = null;
        let total = 0;
        
        // 检查响应格式
        if (res && typeof res === 'object') {
          if (res.data && typeof res.data.code !== 'undefined') {
            // 标准格式 {code: 0, data: {list: [...], total: num}}
            if (res.data.code === 0) {
              draftsData = res.data.data.list || res.data.data;
              total = res.data.data.total || 0;
            } else {
              this.$message.error('获取草稿失败：' + res.data.message);
              return;
            }
          } else if (Array.isArray(res)) {
            // 直接返回数组格式
            draftsData = res;
            total = res.length;
          } else if (res.data && Array.isArray(res.data)) {
            // 数据在 data 字段中，且是数组
            draftsData = res.data;
            total = res.data.length;
          } else if (Array.isArray(res)) {
            // 响应本身就是数组
            draftsData = res;
            total = res.length;
          } else if (res.data && typeof res.data === 'object') {
            // 响应是对象，检查是否有 list 或其他数组字段
            if (res.data.list && Array.isArray(res.data.list)) {
              draftsData = res.data.list;
              total = res.data.total || res.data.list.length;
            } else if (res.data.drafts && Array.isArray(res.data.drafts)) {
              draftsData = res.data.drafts;
              total = res.data.drafts.length;
            } else {
              // 假设整个对象就是数据
              draftsData = [res];
              total = 1;
            }
          } else {
            // 其他情况，尝试使用整个响应
            draftsData = [res];
            total = 1;
          }
        } else {
          this.$message.error('获取草稿失败：响应格式错误');
          return;
        }
        
        this.draftList = draftsData;
        this.draftTotal = total;
      } catch (error) {
        console.error(error);
        this.$message.error('网络错误，无法加载草稿');
      } finally {
        this.loadingDrafts = false;
      }
    },

    // 编辑草稿（跳转到带id的写博客页面）
    editDraft(id) {
      // 通过ID获取草稿详情
      const draft = this.draftList.find(item => item.id === id);
      
      if (draft) {
        // 将草稿内容填充到当前博客编辑区
        this.blog = {
          id: draft.id,
          title: draft.title || '',
          content: draft.content || '',
          tags: draft.tags || [],
          status: draft.status || 'draft'
        };
        
        // 关闭抽屉
        this.drawerVisible = false;
        
        // 提示用户已加载草稿
        this.$message.success('已加载草稿内容');
      } else {
        // 如果在当前列表中没找到，通过API获取
        this.loadDraftById(id);
      }
    },

    async loadDraftById(id) {
      try {
        const res = await this.$axios.get(`/api/blog/${id}`);
        
        if (res && typeof res === 'object') {
          let draftData = null;
          
          // 检查响应格式
          if (res.data && typeof res.data.code !== 'undefined') {
            // 标准格式 {code: 0, data: {...}}
            if (res.data.code === 0) {
              draftData = res.data.data;
            } else {
              this.$message.error('获取草稿失败：' + res.data.message);
              return;
            }
          } else {
            // 直接返回数据格式
            draftData = res;
          }
          
          if (draftData) {
            // 将草稿内容填充到当前博客编辑区
            this.blog = {
              id: draftData.id,
              title: draftData.title || '',
              content: draftData.content || '',
              tags: Array.isArray(draftData.tags) ? draftData.tags : (draftData.tags ? [draftData.tags] : []),
              status: draftData.status || 'draft'
            };
            
            // 关闭抽屉
            this.drawerVisible = false;
            
            // 提示用户已加载草稿
            this.$message.success('已加载草稿内容');
          }
        }
      } catch (error) {
        console.error('加载草稿失败:', error);
        this.$message.error('加载草稿失败：' + (error.message || '网络错误'));
      }
    },
    // 抽屉关闭前的处理（可选）
    handleDrawerClose(done) {
      done();
    },

    // 格式化时间
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`;
    },
  },
};
</script>

<style scoped>
/* 原有样式保留，新增草稿箱相关样式 */
.write-blog-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

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

.tag-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tag-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}
.tag-select {
  flex: 1;
  max-width: 500px;
}

.content-input >>> .el-textarea__inner {
  font-size: 16px;
  line-height: 1.8;
  font-family: 'Microsoft YaHei', sans-serif;
  min-height: 400px;
}

/* 草稿箱抽屉样式 */
.draft-list {
  padding: 10px;
}
.draft-item {
  margin-bottom: 15px;
  cursor: pointer;
  transition: transform 0.2s;
}
.draft-item:hover {
  transform: translateY(-2px);
}
.draft-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}
.draft-summary {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.draft-meta {
  font-size: 12px;
  color: #909399;
}
.draft-pagination {
  margin-top: 20px;
  text-align: center;
}
.empty-draft {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>