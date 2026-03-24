<template>
  <div class="write-blog-container">
    <!-- ========== 头部区域：用户信息和操作按钮 ========== -->
    <div class="write-header">
      <!-- 用户信息区域，点击跳转到用户主页 -->
      <div class="user-info" @click="goToUserHome">
        <el-avatar :size="50" :src="userAvatar"></el-avatar>
        <span class="username">{{ username }}</span>
      </div>
      
      <!-- 右侧操作按钮组 -->
      <div class="action-buttons">
        <!-- 草稿箱按钮：打开抽屉查看所有草稿 -->
        <el-button type="warning" plain @click="openDraftDrawer" :loading="loadingDrafts">
          草稿箱
        </el-button>
        <!-- 最后保存时间提示（仅草稿模式显示） -->
        <span v-if="lastSaved" class="save-tip">最后保存：{{ lastSaved }}</span>
        <!-- 存草稿按钮：保存为草稿状态 -->
        <el-button type="info" plain @click="saveDraft" :loading="savingDraft">
          存草稿
        </el-button>
        <!-- 发布按钮：发布为公开状态 -->
        <el-button type="primary" @click="publishBlog" :loading="publishing">
          发布
        </el-button>
      </div>
    </div>

    <!-- ========== 博客编辑主体区域 ========== -->
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

      <!-- 标签选择器：多选标签 -->
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
          <!-- 动态渲染标签选项 -->
          <el-option
            v-for="tag in tagOptions"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          ></el-option>
        </el-select>
      </div>

      <!-- ========== 富文本编辑器（Quill 1.x） ========== -->
      <!-- 使用 v-if="isClient" 确保只在客户端渲染编辑器 -->
      <div v-if="isClient" class="editor-container">
        <!-- 编辑器工具栏：包含各种格式化按钮 -->
        <div id="toolbar">
          <!-- 工具栏内容由 Quill 自动渲染，这里只需一个容器 -->
        </div>
        <!-- 编辑器主体区域 -->
        <div id="editor" class="ql-editor-container"></div>
      </div>
      <!-- 服务端渲染时显示加载占位符 -->
      <div v-else class="editor-placeholder">
        <el-skeleton :rows="10" animated />
      </div>
    </div>

    <!-- ========== 草稿箱抽屉 ========== -->
    <el-drawer
      title="我的草稿"
      :visible.sync="drawerVisible"
      direction="rtl"
      size="400px"
      :before-close="handleDrawerClose"
    >
      <div class="draft-list" v-loading="loadingDrafts">
        <!-- 遍历显示草稿列表 -->
        <el-card
          v-for="draft in draftList"
          :key="draft.id"
          class="draft-item"
          shadow="hover"
          @click.native="editDraft(draft.id)"
        >
          <h4>{{ draft.title || '无标题' }}</h4>
          <!-- 草稿摘要：去除HTML标签后显示前50个字符 -->
          <p class="draft-summary">{{ draft.summary || stripHtml(draft.content).slice(0, 50) + '...' }}</p>
          <div class="draft-meta">
            <span>最后更新：{{ formatTime(draft.updatedAt) }}</span>
          </div>
        </el-card>
        
        <!-- 分页组件：当草稿总数大于每页条数时显示 -->
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

        <!-- 空状态提示 -->
        <div v-if="draftList.length === 0 && !loadingDrafts" class="empty-draft">
          暂无草稿
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
/**
 * 写博客页面组件
 * 使用 Quill 富文本编辑器（通过客户端插件注入）
 */
import { GetCurrentUser, GetAllTags, GetBlogById, CreateBlog, UpdateBlog, GetBlogDrafts, UploadFile } from '@/api/index'

export default {
  name: 'WriteBlog', // 组件名称
  layout: 'login',
  // ========== 组件数据 ==========
  data() {
    return {
      // 博客数据对象
      blog: {
        id: null,           // 博客ID，编辑模式时有值
        title: '',          // 博客标题
        content: '',        // 博客内容（HTML格式）
        tags: [],           // 选中的标签ID数组
        status: 'draft',    // 状态：draft(草稿) 或 published(已发布)
      },
      
      // 标签相关
      tagOptions: [],       // 从后端获取的标签选项列表
      loadingTags: false,   // 标签加载状态
      
      // 用户信息
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      username: '当前用户',
      userId: '',           // 用户ID，用于跳转个人主页
      
      // 操作状态
      savingDraft: false,   // 保存草稿按钮加载状态
      publishing: false,    // 发布按钮加载状态
      lastSaved: '',        // 最后一次保存草稿的时间
      
      // 草稿箱相关
      drawerVisible: false, // 草稿箱抽屉显示状态
      draftList: [],        // 草稿列表
      loadingDrafts: false, // 加载草稿中
      draftPage: 1,         // 草稿列表当前页码
      pageSize: 10,         // 每页显示草稿数量
      draftTotal: 0,        // 草稿总数
      
      // 编辑器实例
      quill: null,          // Quill 编辑器实例
      isClient: false,      // 是否在客户端环境
    };
  },
  
  // ========== 生命周期钩子 ==========
  
  /**
   * 组件挂载完成后初始化编辑器
   * 使用 $nextTick 确保 DOM 已经渲染完成
   */
  mounted() {
    // 标记已经在客户端
    this.isClient = true;
    
    // 等待 DOM 更新后初始化编辑器
    this.$nextTick(() => {
      this.initEditor();
    });
  },
  
  /**
   * 组件创建时加载数据
   */
  created() {
    this.fetchTags();           // 获取标签列表
    this.loadUserInfo();        // 获取用户信息
    
    // 从路由获取博客ID，如果是编辑模式则加载博客数据
    const blogId = this.$route.params.id;
    if (blogId) {
      this.fetchBlog(blogId);
    }
  },
  
  /**
   * 组件销毁前清理资源
   */
  beforeDestroy() {
    if (this.quill) {
      this.quill = null;        // 释放 Quill 实例，避免内存泄漏
    }
  },
  
  // ========== 组件方法 ==========
  methods: {
    
    // ========== 用户相关方法 ==========
    
    /**
     * 跳转到用户主页
     */
    goToUserHome() {
      this.$router.push(`/user/${this.userId}`);
    },
    
    /**
     * 加载用户信息（入口方法）
     */
    loadUserInfo() {
      console.log('loadUserInfo方法被调用');
      this.fetchUserInfoFromApi();
    },

    /**
     * 从API获取用户信息
     */
    async fetchUserInfoFromApi() {
      console.log('开始调用API获取用户信息...');
      try {
        const response = await GetCurrentUser();
        console.log('API响应:', response);
        
        let userData = null;
        
        // 处理不同的响应格式
        if (response.data && typeof response.data.code !== 'undefined') {
          // 标准格式 {code: 0, data: {...}}
          if (response.data.code === 0 && response.data.data) {
            userData = response.data.data;
          }
        } else if (response.data && response.data.id) {
          // 直接返回用户对象在data字段中
          userData = response.data;
        } else if (response && response.id) {
          // 直接返回用户对象（无data字段）
          userData = response;
        }
        
        // 更新用户信息
        if (userData && userData.id) {
          this.userId = userData.id;
          this.username = userData.nickname || userData.username || '当前用户';
          this.userAvatar = userData.avatarUrl || userData.avatar || this.userAvatar;
          
          // 存储用户信息到 localStorage（仅在客户端）
          if (process.client) {
            try {
              localStorage.setItem('userInfo', JSON.stringify(userData));
            } catch (storageError) {
              console.error('存储用户信息失败:', storageError);
            }
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        // 404 表示用户未登录，不是错误
        if (error.response && error.response.status === 404) {
          console.log('用户未登录');
        }
      }
    },

    // ========== 标签相关方法 ==========
    
    /**
     * 获取标签列表
     */
    async fetchTags() {
      this.loadingTags = true;
      try {
        console.log('开始请求标签数据...');
        const res = await GetAllTags();
        console.log('API返回数据:', res);

        // 处理不同的响应格式
        if (Array.isArray(res)) {
          this.tagOptions = res;                       // 直接返回数组
        } else if (Array.isArray(res.data)) {
          this.tagOptions = res.data;                   // 数据在 data 字段中
        } else if (res.data && typeof res.data === 'object' && res.data.code === 0) {
          this.tagOptions = res.data.data || [];        // 标准格式 {code:0, data: [...]}
        } else {
          console.warn('未识别的API返回格式:', res);
          this.tagOptions = [];
        }
      } catch (error) {
        console.error('获取标签失败:', error);
        this.$message.error('获取标签失败：' + (error.message || '网络错误'));
      } finally {
        this.loadingTags = false;
      }
    },

    // ========== 博客相关方法 ==========
    
    /**
     * 获取博客详情（用于编辑模式）
     * @param {number|string} blogId - 博客ID
     */
    async fetchBlog(blogId) {
      try {
        console.log('获取博客详情:', blogId);
        const res = await GetBlogById(blogId);
        
        if (res && typeof res === 'object') {
          let blogData = null;
          
          // 处理不同的响应格式
          if (res.data && typeof res.data.code !== 'undefined') {
            if (res.data.code === 0) {
              blogData = res.data.data;                 // 标准格式
            } else {
              this.$message.error('获取博客失败：' + res.data.message);
              return;
            }
          } else {
            blogData = res;                              // 直接返回博客对象
          }
          
          if (blogData) {
            // 更新博客数据
            this.blog = {
              id: blogData.id,
              title: blogData.title,
              content: blogData.content,
              tags: Array.isArray(blogData.tags) ? blogData.tags : (blogData.tags ? [blogData.tags] : []),
              status: blogData.status,
            };
            
            // 如果有内容且编辑器已初始化，设置到编辑器中
            if (this.quill && blogData.content) {
              this.quill.root.innerHTML = blogData.content;
            }
          }
        }
      } catch (error) {
        console.error('获取博客失败:', error);
        this.$message.error('网络错误，请稍后重试');
      }
    },

    /**
     * 保存博客（通用方法）
     * @param {string} status - 'draft' 或 'published'
     * @returns {Promise<boolean>} - 是否保存成功
     */
    async saveBlog(status) {
      // 校验标题
      if (!this.blog.title.trim()) {
        this.$message.warning('请填写博客标题');
        return false;
      }
      
      // 校验内容（去除HTML标签后）
      const contentText = this.stripHtml(this.blog.content).trim();
      if (!contentText) {
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
        // 处理标签：转换为数字类型的标签ID数组
        let tagIds = [];
        if (this.blog.tags && Array.isArray(this.blog.tags)) {
          if (this.blog.tags.length > 0 && typeof this.blog.tags[0] === 'object') {
            // 如果是标签对象数组，提取id并转换为数字
            tagIds = this.blog.tags.map(tag => {
              const id = tag.id;
              return typeof id === 'string' ? parseInt(id, 10) : id;
            }).filter(id => typeof id === 'number' && !isNaN(id));
          } else if (this.blog.tags.length > 0) {
            // 如果是ID数组，确保转换为数字类型
            tagIds = this.blog.tags.map(id => {
              return typeof id === 'string' ? parseInt(id, 10) : id;
            }).filter(id => typeof id === 'number' && !isNaN(id));
          }
        }

        // 构建请求数据
        const requestData = {
          title: this.blog.title,
          content: this.blog.content,
          status: status,
          tagIds: tagIds
        };

        // 如果是新建博客，添加用户ID
        if (!this.blog.id && this.userId) {
          requestData.userId = this.userId;
        }

        let res;
        if (this.blog.id) {
          // 编辑模式：PUT 请求
          console.log('编辑博客，ID:', this.blog.id);
          res = await UpdateBlog(this.blog.id, requestData);
        } else {
          // 新建模式：POST 请求
          console.log('创建新博客');
          res = await CreateBlog(requestData);
        }

        // 处理响应
        if (res && typeof res === 'object') {
          // 处理不同的响应格式
          let result = res;
          if (res.data && typeof res.data === 'object') {
          result = res.data;
          }
          
          // 如果是新建博客，保存返回的ID
          if (!this.blog.id) {
            this.blog.id = result.id || result._id;
          }
          this.blog.status = result.status || status;

          // 保存成功后的处理
          if (status === 'draft') {
            const now = new Date();
            this.lastSaved = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
            this.$message.success('草稿保存成功');
          } else {
            this.$message.success('已发布申请，请等待审核通过');
          }
          return true;
        }
      } catch (error) {
        console.error('保存博客出错', error);
        // 错误处理
        if (error.response) {
          this.$message.error((isPublish ? '发布' : '保存草稿') + '失败：' + (error.response.data?.message || '未知错误'));
        } else if (error.request) {
          this.$message.error('网络错误，请检查连接');
        } else {
          this.$message.error('请求错误：' + error.message);
        }
        return false;
      } finally {
        // 重置加载状态
        if (isPublish) {
          this.publishing = false;
        } else {
          this.savingDraft = false;
        }
      }
    },

    /**
     * 保存为草稿
     */
    saveDraft() {
      this.saveBlog('draft');
    },

    /**
     * 发布博客
     */
    publishBlog() {
      this.saveBlog('pending');
    },

    // ========== 草稿箱相关方法 ==========
    
    /**
     * 打开草稿箱抽屉并加载草稿列表
     */
    openDraftDrawer() {
      this.drawerVisible = true;
      this.draftPage = 1; // 重置到第一页
      this.fetchDrafts();
    },

    /**
     * 获取草稿列表
     */
    async fetchDrafts() {
      this.loadingDrafts = true;
      try {
        const res = await GetBlogDrafts();
        
        console.log('获取草稿响应:', res);
        
        let draftsData = [];
        let total = 0;
        
        // 处理不同的响应格式
        if (res && typeof res === 'object') {
          if (res.data && typeof res.data.code !== 'undefined') {
            if (res.data.code === 0) {
              draftsData = res.data.data.list || res.data.data;
              total = res.data.data.total || 0;
            }
          } else if (Array.isArray(res)) {
            draftsData = res;
            total = res.length;
          } else if (res.data && Array.isArray(res.data)) {
            draftsData = res.data;
            total = res.data.length;
          } else {
            draftsData = [];
          }
        }
        
        this.draftList = draftsData;
        this.draftTotal = total;
      } catch (error) {
        console.error('获取草稿失败:', error);
        this.$message.error('网络错误，无法加载草稿');
      } finally {
        this.loadingDrafts = false;
      }
    },

    /**
     * 编辑草稿：加载草稿内容到编辑器
     * @param {number|string} id - 草稿ID
     */
    editDraft(id) {
      // 先从当前列表查找
      const draft = this.draftList.find(item => item.id === id);
      if (draft) {
        this.blog = {
          id: draft.id,
          title: draft.title || '',
          content: draft.content || '',
          tags: draft.tags || [],
          status: draft.status || 'draft'
        };
        
        // 更新编辑器内容
        if (this.quill && draft.content) {
          this.quill.root.innerHTML = draft.content;
        }
        
        this.drawerVisible = false;
        this.$message.success('已加载草稿内容');
      } else {
        // 如果不在当前列表，通过ID单独加载
        this.loadDraftById(id);
      }
    },

    /**
     * 通过ID加载草稿
     * @param {number|string} id - 草稿ID
     */
    async loadDraftById(id) {
      try {
        const res = await GetBlogById(id);
        
        if (res && typeof res === 'object') {
          let draftData = null;
          
          if (res.data && typeof res.data.code !== 'undefined') {
            if (res.data.code === 0) {
              draftData = res.data.data;
            }
          } else {
            draftData = res;
          }
          
          if (draftData) {
            this.blog = {
              id: draftData.id,
              title: draftData.title || '',
              content: draftData.content || '',
              tags: Array.isArray(draftData.tags) ? draftData.tags : [],
              status: draftData.status || 'draft'
            };
            
            if (this.quill && draftData.content) {
              this.quill.root.innerHTML = draftData.content;
            }
            
            this.drawerVisible = false;
            this.$message.success('已加载草稿内容');
          }
        }
      } catch (error) {
        console.error('加载草稿失败:', error);
        this.$message.error('加载草稿失败：' + (error.message || '网络错误'));
      }
    },

    /**
     * 抽屉关闭前的处理
     * @param {Function} done - 关闭回调
     */
    handleDrawerClose(done) {
      done(); // 直接关闭
    },

    // ========== 编辑器相关方法 ==========
    
    /**
     * 初始化 Quill 编辑器
     * 通过客户端插件注入的 $quill 访问 Quill 构造函数
     */
    initEditor() {
      // 确保只在客户端执行
      if (!process.client) return;
      
      // 通过 this.$quill 获取 Quill 构造函数（从插件注入）
      const Quill = this.$quill;
      
      if (!Quill) {
        console.error('Quill 未加载，请检查插件配置');
        return;
      }
      
      // 创建 Quill 编辑器实例
      this.quill = new Quill('#editor', {
        modules: {
          toolbar: '#toolbar',      // 使用指定的工具栏
          syntax: false,            // 禁用代码高亮（需要额外配置）
        },
        placeholder: '请输入博客内容...',
        theme: 'snow',              // 使用雪碧主题
        readOnly: false
      });

      // 监听内容变化，同步到 blog.content
      this.quill.on('text-change', () => {
        this.blog.content = this.quill.root.innerHTML;
      });

      // 如果有初始内容，设置到编辑器
      if (this.blog.content) {
        this.quill.root.innerHTML = this.blog.content;
      }

      // 为图片按钮添加自定义处理器
      const toolbar = this.quill.getModule('toolbar');
      toolbar.addHandler('image', this.imageHandler);
      
      console.log('Quill 编辑器初始化完成');
    },

    /**
     * 图片上传处理器
     */
    imageHandler() {
      // 创建文件输入框
      const input = document.createElement('input');
      input.setAttribute('type', 'file');
      input.setAttribute('accept', 'image/*');
      input.click();

      input.onchange = async () => {
        const file = input.files[0];
        const formData = new FormData();
        formData.append('image', file);

        try {
          // 上传图片到服务器
          const res = await UploadFile(formData);
          // 获取当前光标位置
          const range = this.quill.getSelection();
          // 在光标位置插入图片
          this.quill.insertEmbed(range.index, 'image', res.data.url);
        } catch (error) {
          console.error('图片上传失败:', error);
          this.$message.error('图片上传失败');
        }
      };
    },

    /**
     * 去除HTML标签，获取纯文本
     * @param {string} html - HTML字符串
     * @returns {string} - 纯文本
     */
    stripHtml(html) {
      if (!html) return '';
      // 仅在客户端执行
      if (process.client) {
        const tmp = document.createElement('div');
        tmp.innerHTML = html;
        return tmp.textContent || tmp.innerText || '';
      }
      return html.replace(/<[^>]*>/g, ''); // 服务端简单替换
    },

    /**
     * 格式化时间
     * @param {string} time - 时间字符串
     * @returns {string} - 格式化后的时间
     */
    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`;
    }
  }
};
</script>

<style scoped>
/* ========== 全局样式 ========== */
.write-blog-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 0 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* ========== 头部区域 ========== */
.write-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

/* 用户信息 */
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
  font-weight: 500;
  color: #1e293b;
  background: linear-gradient(135deg, #1e293b, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 最后保存提示 */
.save-tip {
  font-size: 14px;
  color: #64748b;
  background: #f1f5f9;
  padding: 5px 10px;
  border-radius: 20px;
}

/* 按钮统一美化 */
.action-buttons .el-button {
  border-radius: 30px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}
.action-buttons .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
}
.action-buttons .el-button.el-button--warning {
  background: #f59e0b;
  border-color: #f59e0b;
  color: white;
}
.action-buttons .el-button.el-button--info {
  background: #94a3b8;
  border-color: #94a3b8;
  color: white;
}
.action-buttons .el-button.el-button--primary {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

/* ========== 编辑区域 ========== */
.edit-area {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

/* 标题输入框 */
.title-input >>> .el-input__inner {
  font-size: 28px;
  height: 60px;
  line-height: 60px;
  font-weight: 600;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 0 20px;
  color: #1e293b;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.title-input >>> .el-input__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
.title-input >>> .el-input__count {
  background: transparent;
  color: #94a3b8;
}

/* 标签选择器 */
.tag-selector {
  display: flex;
  align-items: center;
  gap: 15px;
  background: white;
  padding: 10px 20px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}
.tag-label {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
}
.tag-select {
  flex: 1;
  max-width: 500px;
}
.tag-select >>> .el-input__inner {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 30px;
  height: 40px;
  color: #1e293b;
}
.tag-select >>> .el-select__tags {
  margin-left: 10px;
}
.tag-select >>> .el-tag {
  background: #e2e8f0;
  border-color: #cbd5e1;
  color: #1e293b;
}

/* ========== 编辑器容器 ========== */
.editor-container {
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}
#toolbar {
  border: none;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 10px;
}
.ql-editor-container {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  background: white;
  color: #1e293b;
}
.ql-editor {
  min-height: 400px;
  font-size: 16px;
  line-height: 1.8;
  font-family: 'Inter', 'Microsoft YaHei', sans-serif;
  color: #1e293b;
}

/* 编辑器占位符（SSR时） */
.editor-placeholder {
  padding: 30px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  min-height: 400px;
}

/* ========== 草稿箱抽屉 ========== */
.draft-list {
  padding: 15px;
}
.draft-item {
  margin-bottom: 15px;
  cursor: pointer;
  border-radius: 16px !important;
  border: 1px solid #f1f5f9;
  transition: transform 0.2s, box-shadow 0.2s;
}
.draft-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
  border-color: #3b82f6;
}
.draft-item h4 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.draft-summary {
  margin: 0 0 8px;
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.draft-meta {
  font-size: 12px;
  color: #94a3b8;
}
.draft-pagination {
  margin-top: 20px;
  text-align: center;
}
.draft-pagination >>> .el-pager li {
  border-radius: 30px;
  font-weight: 500;
}
.draft-pagination >>> .el-pager li.active {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
}
.empty-draft {
  text-align: center;
  padding: 60px 20px;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
  border-radius: 16px;
  margin: 20px;
}

/* ========== 响应式 ========== */
@media screen and (max-width: 768px) {
  .write-header {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  .action-buttons {
    justify-content: flex-end;
  }
  .tag-selector {
    flex-wrap: wrap;
  }
  .tag-label {
    width: 100%;
  }
  .tag-select {
    max-width: 100%;
  }
}
</style>