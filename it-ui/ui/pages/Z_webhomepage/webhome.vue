<template>
  <div class="home-container">
    <!-- ========== 导航栏 ========== -->
    <nav class="navbar" :class="{ 'navbar-scrolled': scrolled }">
      <div class="navbar-content">
        <!-- Logo -->
        <div class="logo" @click="scrollToTop">
          <span class="logo-icon">●</span>
          <span class="logo-text">ITSpace</span>
        </div>

        <!-- 导航菜单 -->
        <div class="nav-menu">
          <a 
            v-for="item in navItems" 
            :key="item.name"
            :href="item.hash"
            @click.prevent="scrollToSection(item.hash)"
            class="nav-link"
            :class="{ active: activeSection === item.hash }"
          >
            {{ item.name }}
          </a>
        </div>

        <!-- 右侧操作区 - 登录状态相关 -->
        <div class="nav-actions">
          <!-- 消息通知组件（仅在登录后显示） -->
          <!-- <NotificationBell v-if="isLoggedIn" /> -->
          
          <!-- 未登录状态：显示登录和注册按钮 -->
          <template v-if="!isLoggedIn && !loadingUser">
            <el-button 
              type="text" 
              class="login-btn"
              @click="goToLogin"
            >
              登录
            </el-button>
            <el-button 
              type="primary" 
              class="register-btn"
              @click="goToRegister"
            >
              注册
            </el-button>
          </template>
          
          <!-- 用户信息加载中状态，显示加载动画 -->
          <div v-if="loadingUser" class="user-loading">
            <i class="el-icon-loading"></i>
          </div>
          
          <!-- 已登录状态：显示用户头像和下拉菜单 -->
          <el-dropdown v-if="isLoggedIn" @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="40" :src="userAvatar"></el-avatar>
              <span class="username">{{ username }}</span>
              <i class="el-icon-arrow-down"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="user">个人主页</el-dropdown-item>
              <el-dropdown-item command="blog">我的博客</el-dropdown-item>
              <el-dropdown-item command="circle">我的圈子</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </nav>

    <!-- ========== 英雄区域（大屏展示） ========== -->
    <section id="hero" class="hero-section">
      <div class="hero-background">
        <div class="gradient-sphere"></div>
        <div class="gradient-sphere second"></div>
      </div>
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">
            <span class="gradient-text">分享知识</span><br>
            <span class="gradient-text">连接未来</span>
          </h1>
          <p class="hero-subtitle">
            一个集博客、技术圈、项目协作于一体的<br>
            新一代开发者社区
          </p>
          <div class="hero-buttons">
            <el-button type="primary" class="get-started-btn" @click="goToBlog">
              开始探索
              <i class="el-icon-arrow-right"></i>
            </el-button>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-number">10k+</span>
              <span class="stat-label">活跃用户</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">5k+</span>
              <span class="stat-label">技术博客</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">1k+</span>
              <span class="stat-label">技术圈子</span>
            </div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="code-window">
            <div class="window-header">
              <span class="window-dot red"></span>
              <span class="window-dot yellow"></span>
              <span class="window-dot green"></span>
            </div>
            <div class="window-content">
              <pre><code class="language-javascript">
<span class="code-line"><span class="keyword">import</span> { ref } <span class="keyword">from</span> <span class="string">'vue'</span></span>
<span class="code-line"></span>
<span class="code-line"><span class="keyword">const</span> <span class="function">createApp</span> = <span class="punctuation">(</span><span class="punctuation">)</span> <span class="operator">=></span> <span class="punctuation">{</span></span>
<span class="code-line indent">  <span class="keyword">const</span> count <span class="operator">=</span> <span class="function">ref</span><span class="punctuation">(</span><span class="number">0</span><span class="punctuation">)</span></span>
<span class="code-line indent">  <span class="keyword">const</span> increment <span class="operator">=</span> <span class="punctuation">(</span><span class="punctuation">)</span> <span class="operator">=></span> count<span class="punctuation">.</span>value<span class="operator">++</span></span>
<span class="code-line indent">  <span class="keyword">return</span> <span class="punctuation">{</span> count<span class="punctuation">,</span> increment <span class="punctuation">}</span></span>
<span class="code-line"><span class="punctuation">}</span></span>
              </code></pre>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 特色功能 ========== -->
    <section id="features" class="features-section">
      <div class="section-header">
        <span class="section-tag">为什么选择我们</span>
        <h2 class="section-title">强大的功能，极致的体验</h2>
        <p class="section-subtitle">
          为开发者打造的一站式技术社区，让知识分享更简单
        </p>
      </div>

      <div class="features-grid">
        <div 
          v-for="feature in features" 
          :key="feature.title"
          class="feature-card"
          @mouseenter="featureHover = feature.title"
          @mouseleave="featureHover = null"
        >
          <div class="feature-icon" :class="{ 'hovered': featureHover === feature.title }">
            <i :class="feature.icon"></i>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
          <a href="#" class="feature-link" @click.prevent="goToFeature(feature)">
            了解更多 <i class="el-icon-arrow-right"></i>
          </a>
        </div>
      </div>
    </section>

    <!-- ========== 热门博客预览 ========== -->
    <section id="blog" class="blog-preview-section">
      <div class="section-header">
        <span class="section-tag">热门推荐</span>
        <h2 class="section-title">精选技术博客</h2>
        <p class="section-subtitle">
          来自社区的技术干货，助你快速成长
        </p>
      </div>

      <div class="blog-grid">
        <article 
          v-for="post in hotPosts" 
          :key="post.id"
          class="blog-card"
          @click="goToBlogDetail(post.id)"
        >
          <div class="blog-card-image">
            <img :src="post.coverImage" :alt="post.title">
            <div class="blog-card-overlay">
              <span class="read-time">{{ post.readTime }}分钟阅读</span>
            </div>
          </div>
          <div class="blog-card-content">
            <div class="blog-card-tags">
              <span 
                v-for="tag in post.tags.slice(0, 2)" 
                :key="tag"
                class="tag"
              >
                {{ tag }}
              </span>
            </div>
            <h3 class="blog-card-title">{{ post.title }}</h3>
            <p class="blog-card-excerpt">{{ post.excerpt }}</p>
            <div class="blog-card-meta">
              <div class="author">
                <el-avatar :size="24" :src="post.authorAvatar"></el-avatar>
                <span class="author-name">{{ post.author }}</span>
              </div>
              <div class="stats">
                <span><i class="el-icon-view"></i> {{ post.views }}</span>
                <span><i class="el-icon-star-off"></i> {{ post.likes }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div class="view-more">
        <el-button type="text" class="view-more-btn" @click="goToBlog">
          查看更多博客 <i class="el-icon-arrow-right"></i>
        </el-button>
      </div>
    </section>

    <!-- ========== 热门圈子预览 ========== -->
    <section id="circle" class="circle-preview-section">
      <div class="section-header">
        <span class="section-tag">活跃圈子</span>
        <h2 class="section-title">发现有趣的技术圈子</h2>
        <p class="section-subtitle">
          找到志同道合的伙伴，一起交流进步
        </p>
      </div>

      <div class="circle-grid">
        <div 
          v-for="circle in hotCircles" 
          :key="circle.id"
          class="circle-card"
          @click="goToCircle(circle.id)"
        >
          <div class="circle-card-header" :style="{ backgroundColor: circle.color }">
            <i :class="circle.icon"></i>
          </div>
          <div class="circle-card-content">
            <h3 class="circle-card-title">{{ circle.name }}</h3>
            <p class="circle-card-description">{{ circle.description }}</p>
            <div class="circle-card-stats">
              <span><i class="el-icon-user"></i> {{ circle.members }} 成员</span>
              <span><i class="el-icon-chat-dot-round"></i> {{ circle.posts }} 帖子</span>
            </div>
            <div class="circle-card-members">
              <el-avatar 
                v-for="(member, index) in circle.memberAvatars.slice(0, 3)" 
                :key="index"
                :size="28"
                :src="member"
                class="member-avatar"
              ></el-avatar>
              <span v-if="circle.members > 3" class="more-members">+{{ circle.members - 3 }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="view-more">
        <el-button type="text" class="view-more-btn" @click="goToCircleList">
          查看更多圈子 <i class="el-icon-arrow-right"></i>
        </el-button>
      </div>
    </section>

    <!-- ========== 数据统计 ========== -->
    <section class="stats-section">
      <div class="stats-background"></div>
      <div class="stats-content">
        <div class="stats-grid">
          <div class="stat-block">
            <span class="stat-block-number">50k+</span>
            <span class="stat-block-label">注册用户</span>
          </div>
          <div class="stat-block">
            <span class="stat-block-number">20k+</span>
            <span class="stat-block-label">技术博客</span>
          </div>
          <div class="stat-block">
            <span class="stat-block-number">5k+</span>
            <span class="stat-block-label">技术圈子</span>
          </div>
          <div class="stat-block">
            <span class="stat-block-number">100k+</span>
            <span class="stat-block-label">每日互动</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 行动号召 ========== -->
    <section class="cta-section">
      <div class="cta-content">
        <h2 class="cta-title">加入技术社区，开启你的成长之旅</h2>
        <p class="cta-subtitle">与万千开发者一起分享知识、交流经验</p>
        <div class="cta-buttons">
          <el-button type="primary" class="cta-btn primary" @click="goToRegister">
            立即注册
          </el-button>
          <el-button class="cta-btn secondary" @click="goToBlog">
            随便看看
          </el-button>
        </div>
      </div>
    </section>

    <!-- ========== 页脚 ========== -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-info">
          <div class="footer-logo">
            <span class="footer-logo-icon">●</span>
            <span class="footer-logo-text">ITSpace</span>
          </div>
          <p class="footer-description">
            为开发者打造的技术分享社区<br>
            让知识流动，让灵感碰撞
          </p>
          <div class="footer-social">
            <a href="#" class="social-link"><i class="el-icon-platform-eleme"></i></a>
            <a href="#" class="social-link"><i class="el-icon-platform-wechat"></i></a>
            <a href="#" class="social-link"><i class="el-icon-platform-qq"></i></a>
            <a href="#" class="social-link"><i class="el-icon-platform-github"></i></a>
          </div>
        </div>

        <div class="footer-links">
          <div class="footer-links-column">
            <h4>产品</h4>
            <ul>
              <li><a href="#">博客</a></li>
              <li><a href="#">圈子</a></li>
              <li><a href="#">项目</a></li>
              <li><a href="#">问答</a></li>
            </ul>
          </div>
          <div class="footer-links-column">
            <h4>支持</h4>
            <ul>
              <li><a href="#">帮助中心</a></li>
              <li><a href="#">社区指南</a></li>
              <li><a href="#">联系我们</a></li>
              <li><a href="#">反馈建议</a></li>
            </ul>
          </div>
          <div class="footer-links-column">
            <h4>关于</h4>
            <ul>
              <li><a href="#">关于我们</a></li>
              <li><a href="#">加入我们</a></li>
              <li><a href="#">合作伙伴</a></li>
              <li><a href="#">媒体报道</a></li>
            </ul>
          </div>
          <div class="footer-links-column">
            <h4>法律</h4>
            <ul>
              <li><a href="#">隐私政策</a></li>
              <li><a href="#">服务条款</a></li>
              <li><a href="#">版权声明</a></li>
            </ul>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2025 TechSpace. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script>
import NotificationBell from '@/components/NotificationBell.vue'
// 导入获取当前用户信息的API
import { GetCurrentUser } from '@/api/index'
import { clearAuthState } from '@/utils/auth'

export default {
  layout: 'login',
  name: 'HomePage',
  components: {
    NotificationBell
  },
  data() {
    return {
      // ========== 界面状态 ==========
      scrolled: false,                    // 导航栏滚动状态
      activeSection: '#hero',              // 当前激活的导航区域
      featureHover: null,                  // 当前悬停的特色功能
      
      // ========== 用户登录状态 ==========
      isLoggedIn: false,                   // 用户是否已登录
      loadingUser: true,                   // 用户信息加载中状态，避免闪烁
      userAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      username: '当前用户',
      userId: null,                        // 用户ID，用于后续操作
      
      // ========== 导航菜单配置 ==========
      navItems: [
        { name: '首页', hash: '#hero' },
        { name: '特色', hash: '#features' },
        { name: '博客', hash: '#blog' },
        { name: '圈子', hash: '#circle' },
      ],
      
      // ========== 特色功能配置 ==========
      features: [
        {
          icon: 'el-icon-edit-outline',
          title: '技术博客',
          description: '支持富文本编辑、代码高亮、标签分类，让写作更专注。',
          link: '/blog'
        },
        {
          icon: 'el-icon-chat-dot-round',
          title: '技术圈子',
          description: '创建或加入技术圈子，与志同道合的开发者深入交流。',
          link: '/circle'
        },
        {
          icon: 'el-icon-data-line',
          title: '项目协作',
          description: '发布开源项目，寻找合作伙伴，一起构建更好的产品。',
          link: '/projects'
        },
        {
          icon: 'el-icon-s-promotion',
          title: '问答社区',
          description: '遇到技术难题？来这里提问，社区高手帮你解答。',
          link: '/qa'
        }
      ],
      
      // ========== 热门博客数据 ==========
      hotPosts: [
        {
          id: 1,
          title: 'Vue 3 组合式 API 最佳实践',
          excerpt: '深入探讨 Vue 3 组合式 API 的使用技巧和常见陷阱...',
          coverImage: 'https://picsum.photos/400/225?random=1',
          author: '前端小王子',
          authorAvatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
          tags: ['Vue', '前端'],
          views: '1.2k',
          likes: 128,
          readTime: 8
        },
        {
          id: 2,
          title: 'Java 并发编程实战：线程池详解',
          excerpt: '从源码角度分析 Java 线程池的实现原理和最佳配置...',
          coverImage: 'https://picsum.photos/400/225?random=2',
          author: 'Java老兵',
          authorAvatar: 'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
          tags: ['Java', '后端'],
          views: '856',
          likes: 67,
          readTime: 12
        },
        {
          id: 3,
          title: 'Docker 容器化部署 Spring Boot 应用',
          excerpt: '手把手教你将 Spring Boot 应用容器化并部署到服务器...',
          coverImage: 'https://picsum.photos/400/225?random=3',
          author: '运维小能手',
          authorAvatar: 'https://cube.elemecdn.com/4/5d/6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s.png',
          tags: ['Docker', '运维'],
          views: '2.1k',
          likes: 156,
          readTime: 10
        }
      ],
      
      // ========== 热门圈子数据 ==========
      hotCircles: [
        {
          id: 1,
          name: '前端开发',
          description: 'Vue、React、Angular 等前端技术交流',
          icon: 'el-icon-monitor',
          color: '#409EFF',
          members: 1234,
          posts: 567,
          memberAvatars: [
            'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
            'https://cube.elemecdn.com/1/8e/5c2a0e7c8b3a4b8a8f3b9a6d5c4b3png.png',
            'https://cube.elemecdn.com/2/8e/4a7b8c9d0e1f2a3b4c5d6e7f8g9h0i.png',
            'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
          ]
        },
        {
          id: 2,
          name: '后端架构',
          description: 'Java、Python、Go、微服务架构讨论',
          icon: 'el-icon-connection',
          color: '#67C23A',
          members: 987,
          posts: 432,
          memberAvatars: [
            'https://cube.elemecdn.com/4/5d/6e7f8g9h0i1j2k3l4m5n6o7p8q9r0s.png',
            'https://cube.elemecdn.com/5/6f/7g8h9i0j1k2l3m4n5o6p7q8r9s0t.png',
            'https://cube.elemecdn.com/6/7g/8h9i0j1k2l3m4n5o6p7q8r9s0t1u.png',
            'https://cube.elemecdn.com/7/8h/9i0j1k2l3m4n5o6p7q8r9s0t1u2v.png'
          ]
        },
        {
          id: 3,
          name: '数据库技术',
          description: 'MySQL、Redis、MongoDB 数据库技术分享',
          icon: 'el-icon-data-line',
          color: '#E6A23C',
          members: 756,
          posts: 321,
          memberAvatars: [
            'https://cube.elemecdn.com/8/9i/0j1k2l3m4n5o6p7q8r9s0t1u2v3w.png',
            'https://cube.elemecdn.com/9/0j/1k2l3m4n5o6p7q8r9s0t1u2v3w4x.png',
            'https://cube.elemecdn.com/0/1k/2l3m4n5o6p7q8r9s0t1u2v3w4x5y.png',
            'https://cube.elemecdn.com/1/2l/3m4n5o6p7q8r9s0t1u2v3w4x5y6z.png'
          ]
        }
      ]
    }
  },
  mounted() {
    // 组件挂载时检查用户登录状态
    this.checkLoginStatus()
    
    // 监听滚动事件
    window.addEventListener('scroll', this.handleScroll)
    this.observeSections()
  },
  beforeDestroy() {
    // 组件销毁前移除事件监听
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    /**
     * 检查用户登录状态
     * 调用后端 API 获取当前用户信息
     */
    async checkLoginStatus() {
      this.loadingUser = true
      try {
        const response = await GetCurrentUser()
        console.log('获取用户信息成功:', response)
        
        // 处理不同的响应格式
        let userData = null
        if (response.data && response.data.code === 0 && response.data.data) {
          userData = response.data.data
        } else if (response.data && response.data.id) {
          userData = response.data
        } else if (response && response.id) {
          userData = response
        }
        
        if (userData && userData.id) {
          // 用户已登录，更新状态
          this.isLoggedIn = true
          this.userId = userData.id
          this.username = userData.nickname || userData.username || '用户'
          this.userAvatar = userData.avatarUrl || userData.avatar || this.userAvatar
          
          // 可选：将用户信息存储到 Vuex 或 localStorage
          if (this.$store) {
            this.$store.commit('user/setUserInfo', userData)
          }
        } else {
          // 用户未登录
          this.isLoggedIn = false
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 获取用户信息失败（通常为未登录），设置未登录状态
        this.isLoggedIn = false
      } finally {
        this.loadingUser = false
      }
    },
    
    /**
     * 处理滚动事件，更新导航栏样式
     */
    handleScroll() {
      this.scrolled = window.scrollY > 50
    },
    
    /**
     * 使用 Intersection Observer 监听区域可见性，更新激活的导航项
     */
    observeSections() {
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.activeSection = '#' + entry.target.id
          }
        })
      }, { threshold: 0.3 })

      this.navItems.forEach(item => {
        const element = document.querySelector(item.hash)
        if (element) observer.observe(element)
      })
    },
    
    /**
     * 平滑滚动到指定区域
     * @param {string} hash - 目标区域的 ID
     */
    scrollToSection(hash) {
      const element = document.querySelector(hash)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
      }
    },
    
    /**
     * 滚动到页面顶部
     */
    scrollToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    
    /**
     * 跳转到登录页
     */
    goToLogin() {
      this.$router.push('/login')
    },
    
    /**
     * 跳转到注册页
     */
    goToRegister() {
      this.$router.push('/registe')
    },
    
    /**
     * 跳转到博客列表页
     */
    goToBlog() {
      this.$router.push('/blog')
    },
    
    /**
     * 跳转到博客详情页
     * @param {number} id - 博客ID
     */
    goToBlogDetail(id) {
      this.$router.push(`/blog/${id}`)
    },
    
    /**
     * 跳转到圈子详情页
     * @param {number} id - 圈子ID
     */
    goToCircle(id) {
      this.$router.push(`/circle?id=${id}`)
    },
    
    /**
     * 跳转到圈子列表页
     */
    goToCircleList() {
      this.$router.push('/circle')
    },
    
    /**
     * 跳转到功能页面
     * @param {Object} feature - 功能对象
     */
    goToFeature(feature) {
      if (feature.link) {
        this.$router.push(feature.link)
      }
    },
    
    /**
     * 处理用户下拉菜单命令
     * @param {string} command - 命令名称
     */
    handleUserCommand(command) {
      switch(command) {
        case 'user':
          this.$router.push(`/user`)
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
    
    /**
     * 退出登录
     * 清除本地存储的用户信息和 token
     */
    logout() {
      // 清除本地存储的 token 和用户信息
      clearAuthState()
      
      // 更新登录状态
      this.isLoggedIn = false
      this.userId = null
      this.username = '当前用户'
      this.userAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
      
      // 如果使用了 Vuex，清除 store 中的用户信息
      if (this.$store) {
        this.$store.commit('user/clearUserInfo')
      }
      
      // 显示退出成功提示
      this.$message.success('已退出登录')
      
      // 跳转到首页
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
/* ========== 全局样式 ========== */
.home-container {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #1a1a1a;
  overflow-x: hidden;
}

/* ========== 导航栏 ========== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: transparent;
  transition: all 0.3s ease;
  padding: 20px 0;
}

.navbar-scrolled {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.05);
  padding: 10px 0;
}

.navbar-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.logo-icon {
  font-size: 32px;
  color: #409EFF;
  line-height: 1;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #1a1a1a 0%, #409EFF 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  gap: 40px;
}

.nav-link {
  font-size: 16px;
  color: #1a1a1a;
  text-decoration: none;
  position: relative;
  transition: color 0.3s ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #409EFF, #66b1ff);
  transition: width 0.3s ease;
}

.nav-link:hover,
.nav-link.active {
  color: #409EFF;
}

.nav-link:hover::after,
.nav-link.active::after {
  width: 100%;
}

/* 右侧操作区 */
.nav-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.login-btn {
  font-size: 16px;
  color: #1a1a1a;
}

.login-btn:hover {
  color: #409EFF;
}

.register-btn {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  border: none;
  border-radius: 20px;
  padding: 10px 24px;
  font-weight: 500;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(64, 158, 255, 0.3);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  font-weight: 500;
}

/* 用户信息加载动画 */
.user-loading {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-loading i {
  font-size: 20px;
  color: #409EFF;
}

/* ========== 英雄区域 ========== */
.hero-section {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
}

.gradient-sphere {
  position: absolute;
  width: 800px;
  height: 800px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(64, 158, 255, 0.15), transparent 70%);
  top: -200px;
  right: -200px;
  animation: float 20s ease-in-out infinite;
}

.gradient-sphere.second {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle at 70% 70%, rgba(103, 194, 58, 0.1), transparent 70%);
  bottom: -200px;
  left: -200px;
  top: auto;
  right: auto;
  animation: float 15s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, -30px) rotate(120deg); }
  66% { transform: translate(-20px, 20px) rotate(240deg); }
}

.hero-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 40px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
  position: relative;
  z-index: 1;
}

/* 左侧文字 */
.hero-title {
  font-size: 64px;
  font-weight: 700;
  line-height: 1.2;
  margin: 0 0 20px;
}

.gradient-text {
  background: linear-gradient(135deg, #1a1a1a 0%, #409EFF 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  display: inline-block;
}

.hero-subtitle {
  font-size: 18px;
  line-height: 1.6;
  color: #4a4a4a;
  margin-bottom: 40px;
}

/* 按钮 */
.hero-buttons {
  display: flex;
  gap: 20px;
  margin-bottom: 50px;
}

.get-started-btn {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  border: none;
  border-radius: 30px;
  padding: 14px 36px;
  font-size: 16px;
  font-weight: 500;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.get-started-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(64, 158, 255, 0.3);
}

.get-started-btn i {
  margin-left: 8px;
  transition: transform 0.3s ease;
}

.get-started-btn:hover i {
  transform: translateX(5px);
}

/* 统计数据 */
.hero-stats {
  display: flex;
  align-items: center;
  gap: 30px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: linear-gradient(to bottom, transparent, #e0e0e0, transparent);
}

/* 右侧代码窗口 */
.hero-visual {
  position: relative;
}

.code-window {
  background: #1e1e1e;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.2);
  transform: perspective(1000px) rotateY(-5deg) rotateX(5deg);
  transition: transform 0.5s ease;
}

.code-window:hover {
  transform: perspective(1000px) rotateY(0deg) rotateX(0deg);
}

.window-header {
  background: #2d2d2d;
  padding: 12px 16px;
  display: flex;
  gap: 8px;
}

.window-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.window-dot.red { background: #ff5f56; }
.window-dot.yellow { background: #ffbd2e; }
.window-dot.green { background: #27c93f; }

.window-content {
  padding: 20px;
}

.window-content pre {
  margin: 0;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #d4d4d4;
}

.code-line {
  display: block;
}

.code-line.indent {
  margin-left: 20px;
}

.keyword { color: #569cd6; }
.function { color: #dcdcaa; }
.string { color: #ce9178; }
.number { color: #b5cea8; }
.punctuation { color: #d4d4d4; }
.operator { color: #d4d4d4; }

/* ========== 特色功能 ========== */
.features-section {
  padding: 100px 40px;
  background: white;
}

.section-header {
  text-align: center;
  max-width: 600px;
  margin: 0 auto 60px;
}

.section-tag {
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: #409EFF;
  background: rgba(64, 158, 255, 0.1);
  padding: 6px 16px;
  border-radius: 30px;
  display: inline-block;
  margin-bottom: 20px;
}

.section-title {
  font-size: 36px;
  font-weight: 600;
  margin: 0 0 20px;
  color: #1a1a1a;
}

.section-subtitle {
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.features-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
}

.feature-card {
  background: white;
  border-radius: 20px;
  padding: 40px 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #f0f0f0;
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 30px 50px rgba(64, 158, 255, 0.1);
  border-color: transparent;
}

.feature-icon {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #f0f7ff, #e6f0ff);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
  transition: all 0.3s ease;
}

.feature-icon.hovered {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
}

.feature-icon i {
  font-size: 32px;
  color: #409EFF;
  transition: color 0.3s ease;
}

.feature-icon.hovered i {
  color: white;
}

.feature-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 15px;
  color: #1a1a1a;
}

.feature-description {
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
  margin: 0 0 20px;
}

.feature-link {
  color: #409EFF;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: gap 0.3s ease;
}

.feature-link:hover {
  gap: 10px;
}

/* ========== 博客预览 ========== */
.blog-preview-section {
  padding: 100px 40px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
}

.blog-grid {
  max-width: 1200px;
  margin: 0 auto 50px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.blog-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

.blog-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 30px 50px rgba(0, 0, 0, 0.1);
}

.blog-card-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.blog-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.blog-card:hover .blog-card-image img {
  transform: scale(1.1);
}

.blog-card-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  background: linear-gradient(to top, rgba(0,0,0,0.8), transparent);
  color: white;
}

.read-time {
  font-size: 12px;
  background: rgba(255,255,255,0.2);
  padding: 4px 12px;
  border-radius: 20px;
}

.blog-card-content {
  padding: 25px;
}

.blog-card-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
}

.tag {
  font-size: 12px;
  padding: 4px 12px;
  background: #f0f7ff;
  color: #409EFF;
  border-radius: 20px;
}

.blog-card-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #1a1a1a;
  line-height: 1.4;
}

.blog-card-excerpt {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.blog-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
}

.stats {
  display: flex;
  gap: 15px;
  color: #909399;
  font-size: 13px;
}

.stats i {
  margin-right: 4px;
}

.view-more {
  text-align: center;
  margin-top: 20px;
}

.view-more-btn {
  font-size: 16px;
  color: #409EFF;
  font-weight: 500;
}

.view-more-btn i {
  transition: transform 0.3s ease;
}

.view-more-btn:hover i {
  transform: translateX(5px);
}

/* ========== 圈子预览 ========== */
.circle-preview-section {
  padding: 100px 40px;
  background: white;
}

.circle-grid {
  max-width: 1200px;
  margin: 0 auto 50px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.circle-card {
  background: white;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.circle-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 30px 50px rgba(0, 0, 0, 0.1);
}

.circle-card-header {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.circle-card-header i {
  font-size: 48px;
  color: white;
  opacity: 0.9;
  transition: transform 0.3s ease;
}

.circle-card:hover .circle-card-header i {
  transform: scale(1.1);
}

.circle-card-content {
  padding: 25px;
}

.circle-card-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #1a1a1a;
}

.circle-card-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 15px;
}

.circle-card-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
  color: #909399;
  font-size: 13px;
}

.circle-card-stats i {
  margin-right: 4px;
}

.circle-card-members {
  display: flex;
  align-items: center;
}

.member-avatar {
  margin-right: -8px;
  border: 2px solid white;
  transition: transform 0.3s ease;
}

.member-avatar:hover {
  transform: translateY(-3px);
  z-index: 1;
}

.more-members {
  margin-left: 10px;
  font-size: 13px;
  color: #909399;
}

/* ========== 数据统计 ========== */
.stats-section {
  padding: 80px 40px;
  position: relative;
  overflow: hidden;
}

.stats-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #1a1a1a, #2d2d2d);
  z-index: 0;
}

.stats-background::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.1), transparent);
  top: -200px;
  right: -200px;
}

.stats-background::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.1), transparent);
  bottom: -150px;
  left: -150px;
}

.stats-content {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 30px;
}

.stat-block {
  text-align: center;
  padding: 40px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.3s ease;
}

.stat-block:hover {
  transform: translateY(-10px);
}

.stat-block-number {
  display: block;
  font-size: 48px;
  font-weight: 700;
  color: white;
  margin-bottom: 10px;
}

.stat-block-label {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

/* ========== 行动号召 ========== */
.cta-section {
  padding: 120px 40px;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  position: relative;
  overflow: hidden;
}

.cta-section::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.2), transparent);
  top: -200px;
  right: -200px;
}

.cta-section::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.15), transparent);
  bottom: -150px;
  left: -150px;
}

.cta-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  position: relative;
  z-index: 1;
}

.cta-title {
  font-size: 42px;
  font-weight: 600;
  color: white;
  margin: 0 0 20px;
}

.cta-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 40px;
}

.cta-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.cta-btn {
  border-radius: 30px;
  padding: 14px 42px;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.cta-btn.primary {
  background: white;
  color: #409EFF;
  border: none;
}

.cta-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
}

.cta-btn.secondary {
  background: transparent;
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.cta-btn.secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: white;
  transform: translateY(-2px);
}

/* ========== 页脚 ========== */
.footer {
  background: #1a1a1a;
  color: white;
  padding: 80px 40px 20px;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 100px;
  margin-bottom: 60px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.footer-logo-icon {
  font-size: 32px;
  color: #409EFF;
}

.footer-logo-text {
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.footer-description {
  font-size: 14px;
  line-height: 1.8;
  color: #a0a0a0;
  margin-bottom: 30px;
}

.footer-social {
  display: flex;
  gap: 15px;
}

.social-link {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  transition: all 0.3s ease;
}

.social-link:hover {
  background: #409EFF;
  transform: translateY(-3px);
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 40px;
}

.footer-links-column h4 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 20px;
  color: white;
}

.footer-links-column ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-links-column li {
  margin-bottom: 12px;
}

.footer-links-column a {
  color: #a0a0a0;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-links-column a:hover {
  color: #409EFF;
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  text-align: center;
}

.footer-bottom p {
  font-size: 14px;
  color: #a0a0a0;
  margin: 0;
}

/* ========== 响应式设计 ========== */
@media screen and (max-width: 1024px) {
  .hero-title {
    font-size: 48px;
  }

  .features-grid,
  .blog-grid,
  .circle-grid,
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .footer-content {
    grid-template-columns: 1fr;
    gap: 60px;
  }
}

@media screen and (max-width: 768px) {
  .navbar-content {
    flex-direction: column;
    gap: 15px;
  }

  .nav-menu {
    display: none;
  }

  .hero-content {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .hero-stats {
    justify-content: center;
  }

  .hero-visual {
    display: none;
  }

  .features-grid,
  .blog-grid,
  .circle-grid,
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .cta-title {
    font-size: 32px;
  }

  .footer-links {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ========== 动画效果 ========== */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-text,
.feature-card,
.blog-card,
.circle-card {
  animation: fadeInUp 0.8s ease forwards;
}

.feature-card:nth-child(2) { animation-delay: 0.2s; }
.feature-card:nth-child(3) { animation-delay: 0.4s; }
.feature-card:nth-child(4) { animation-delay: 0.6s; }
</style>
