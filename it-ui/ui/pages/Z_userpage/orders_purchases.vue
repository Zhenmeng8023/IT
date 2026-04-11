<template>
    <div class="assets-page">
      <!-- 顶部导航栏 -->
      <nav class="navbar" :class="{ 'navbar-scrolled': scrolled }">
        <div class="navbar-content">
          <div class="logo" @click="scrollToTop">
            <span class="logo-icon">●</span>
            <span class="logo-text">ITSpace</span>
          </div>
          <div class="nav-actions">
            <el-dropdown @command="handleUserCommand">
              <div class="user-info">
                <el-avatar :size="40" :src="userAvatar" @error="handleAvatarError"></el-avatar>
                <span class="username">{{ nickname || username }}</span>
                <i class="el-icon-arrow-down"></i>
              </div>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile">个人主页</el-dropdown-item>
                <el-dropdown-item command="blog">我的博客</el-dropdown-item>
                <el-dropdown-item command="circle">我的圈子</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </nav>
  
      <div class="main-content">
        <div class="page-header">
          <h1 class="page-title">我的资产</h1>
          <p class="page-subtitle">订单与购买记录</p>
        </div>
  
        <el-tabs v-model="activeTab" class="tabs">
          <!-- 我的订单 -->
          <el-tab-pane label="我的订单" name="orders">
            <div v-loading="ordersLoading" class="tab-content">
              <div v-if="orders.length === 0" class="empty-state">
                <i class="el-icon-document"></i>
                <p>暂无订单</p>
              </div>
              <div v-else class="order-list">
                <div v-for="order in orders" :key="order.id" class="order-card">
                  <div class="order-header">
                    <span class="order-no">订单号：{{ order.orderNo }}</span>
                    <el-tag :type="getOrderStatusType(order.status)" size="small">
                      {{ getOrderStatusText(order.status) }}
                    </el-tag>
                  </div>
                  <div class="order-body">
                    <div class="order-info">
                      <span>金额：¥{{ order.amount }}</span>
                      <span>类型：{{ order.type === 'content' ? '内容购买' : 'VIP会员' }}</span>
                      <span>支付方式：{{ order.paymentMethod || '-' }}</span>
                    </div>
                    <div class="order-time">
                      <span>创建时间：{{ formatDate(order.createdAt) }}</span>
                      <span v-if="order.payTime">支付时间：{{ formatDate(order.payTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
  
          <!-- 我的购买
          <el-tab-pane label="我的购买" name="purchases">
            <div v-loading="purchasesLoading" class="tab-content">
              <div v-if="purchases.length === 0" class="empty-state">
                <i class="el-icon-star-on"></i>
                <p>暂无购买内容</p>
              </div>
              <div v-else class="purchase-list">
                <div v-for="item in purchases" :key="item.id" class="purchase-card" @click="goToContent(item)">
                  <div class="purchase-info">
                    <h4>{{ item.title }}</h4>
                    <p>购买时间：{{ formatDate(item.purchaseTime) }}</p>
                    <p v-if="item.accessExpiredAt">有效期至：{{ formatDate(item.accessExpiredAt) }}</p>
                  </div>
                  <el-button type="text" class="view-btn">查看详情 <i class="el-icon-arrow-right"></i></el-button>
                </div>
              </div>
            </div>
          </el-tab-pane> -->
        </el-tabs>
      </div>
  
      <footer class="footer">
        <FooterPlayer />
      </footer>
    </div>
  </template>
  
  <script>
  import FooterPlayer from '../Z_userpage/components/FooterPlayer.vue'
  // 导入接口：获取当前用户信息、获取用户订单、获取用户购买记录
  // 导入接口：获取当前用户信息、获取用户订单、获取用户购买记录
  import { GetCurrentUser, GetOrdersByUser, GetUserPurchases, Logout } from '@/api/index.js'
  import { clearAuthState } from '@/utils/auth'
  import axios from 'axios'

  export default {
    layout: 'default',
    components: { FooterPlayer },
    data() {
      return {
        scrolled: false, // 导航栏滚动状态
        userAvatar: '/pic/choubi.jpg', // 用户头像
        username: '', // 用户名
        nickname: '', // 用户昵称
        userId: null, // 用户 ID
        activeTab: 'orders', // 当前激活的选项卡
        orders: [], // 订单列表
        ordersLoading: false, // 订单加载状态
        purchases: [], // 购买记录列表
        purchasesLoading: false, // 购买记录加载状态
        isVip: false, // 是否是 VIP 会员
        vipExpireDate: null, // VIP 过期时间
      };
    },
    mounted() {
      // 监听滚动事件
      window.addEventListener('scroll', this.handleScroll);
      // 获取用户信息
      this.getUserInfo().then(() => {
        this.fetchOrders();
        this.fetchPurchases();
      });
    },
    beforeDestroy() {
      // 移除滚动事件监听
      window.removeEventListener('scroll', this.handleScroll);
    },
    methods: {
      // 处理滚动事件
      handleScroll() {
        this.scrolled = window.scrollY > 50;
      },
      // 滚动到顶部
      scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      // 处理头像加载失败
      handleAvatarError() {
        this.userAvatar = '/pic/choubi.jpg';
        return true;
      },
      // 获取用户信息
      async getUserInfo() {
        try {
          // 调用接口：获取当前用户信息
          const res = await GetCurrentUser();
          const user = res.data || res;
          this.userId = user.id;
          this.username = user.username;
          this.nickname = user.nickname || user.username;
          this.userAvatar = user.avatarUrl || this.userAvatar;
          
          // 调用新的会员状态接口获取最新状态
          await this.fetchUserMembershipStatus();
        } catch (error) {
          console.error('获取用户信息失败', error);
        }
      },
      // 获取用户会员状态
      async fetchUserMembershipStatus() {
        if (!this.userId) return;
        
        try {
          // 调用接口：获取用户当前有效的会员信息
          const res = await axios.get(`/api/membership/user/${this.userId}/active`);
          const { data } = res;
          
          if (data.success && data.data) {
            this.isVip = data.data.isVip === true;
            this.vipExpireDate = data.data.endTime;
          } else {
            // 如果没有有效会员，设置为非会员
            this.isVip = false;
            this.vipExpireDate = null;
          }
        } catch (error) {
          console.error('获取会员状态失败', error);
        }
      },
      // 获取订单列表
      async fetchOrders() {
        this.ordersLoading = true;
        try {
          // 调用接口：获取用户订单
          const res = await axios.get(`/api/orders/user/${this.userId}`);
          // 按创建时间倒序排序，最新的订单在最上面
          this.orders = (res.data || []).sort((a, b) => {
            return new Date(b.createdAt) - new Date(a.createdAt);
          });
        } catch (error) {
          console.error('获取订单失败', error);
          this.$message.error('获取订单失败');
        } finally {
          this.ordersLoading = false;
        }
      },
      // 获取购买记录
      async fetchPurchases() {
        this.purchasesLoading = true;
        try {
          // 调用接口：获取用户购买记录
          const res = await axios.get(`/api/orders/user/${this.userId}` );
          this.purchases = res.data;
        } catch (error) {
          console.error('获取购买记录失败', error);
          this.$message.error('获取购买记录失败');
        } finally {
          this.purchasesLoading = false;
        }
      },
      // 获取订单状态对应的标签类型
      getOrderStatusType(status) {
        const map = {
          pending: 'warning',
          paid: 'success',
          refunded: 'info',
          failed: 'danger',
        };
        return map[status] || 'info';
      },
      // 获取订单状态对应的文本
      getOrderStatusText(status) {
        const map = {
          pending: '待支付',
          paid: '已支付',
          refunded: '已退款',
          failed: '支付失败',
        };
        return map[status] || status;
      },
      // 格式化日期
      formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`;
      },
      // 跳转到内容详情页
      goToContent(item) {
        if (item.targetType === 'blog') {
          this.$router.push(`/blog/${item.targetId}`);
        } else if (item.targetType === 'project') {
          this.$router.push(`/project/${item.targetId}`);
        }
      },
      // 处理用户下拉菜单命令
      handleUserCommand(command) {
        switch (command) {
          case 'profile': this.$router.push('/user'); break;
          case 'blog': this.$router.push('/blog'); break;
          case 'circle': this.$router.push('/circle'); break;
          case 'logout': this.logout(); break;
        }
      },
      // 退出登录
      async logout() {
        try {
          await Logout();
        } catch (error) {
          console.error('退出登录失败', error);
        }
        clearAuthState();
        this.$router.push('/login');
      },
    },
  };
  </script>
  
  <style scoped>
  /* ========== 全局样式 ========== */
  .assets-page {
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
    padding: 10px 0;
  }
  .navbar-scrolled {
    background: rgba(10, 10, 10, 0.95);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  }
  .navbar-content {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 30px;
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
    max-width: 1200px;
    margin: 40px auto;
    padding: 0 20px;
  }
  
  /* 页面头部 */
  .page-header {
    text-align: center;
    margin-bottom: 40px;
  }
  .page-title {
    font-size: 36px;
    font-weight: 600;
    margin: 0 0 10px;
    background: linear-gradient(135deg, #ffffff, #409EFF);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .page-subtitle {
    font-size: 14px;
    color: #a0a0a0;
    margin: 0;
  }
  
  /* 选项卡样式 */
  .tabs {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    padding: 10px;
  }
  .tabs :deep(.el-tabs__header) {
    margin: 0 0 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
  .tabs :deep(.el-tabs__item) {
    color: #a0a0a0;
    font-size: 16px;
    font-weight: 500;
    padding: 0 20px;
  }
  .tabs :deep(.el-tabs__item.is-active) {
    color: #409EFF;
  }
  .tabs :deep(.el-tabs__active-bar) {
    background: #409EFF;
  }
  .tabs :deep(.el-tabs__nav-wrap::after) {
    background-color: rgba(255, 255, 255, 0.05);
  }
  
  .tab-content {
    padding: 20px;
    min-height: 400px;
  }
  
  /* 订单列表 */
  .order-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  .order-card {
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 16px;
    padding: 20px;
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .order-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
    border-color: rgba(64, 158, 255, 0.2);
  }
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  .order-no {
    font-size: 14px;
    color: #a0a0a0;
  }
  .order-body {
    font-size: 14px;
    color: #e0e0e0;
  }
  .order-info {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
    margin-bottom: 10px;
  }
  .order-time {
    display: flex;
    gap: 20px;
    flex-wrap: wrap;
    color: #a0a0a0;
    font-size: 12px;
  }
  
  /* 购买记录列表 */
  .purchase-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
  }
  .purchase-card {
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 16px;
    padding: 20px;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .purchase-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
    border-color: #409EFF;
  }
  .purchase-info h4 {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 12px;
    color: #ffffff;
  }
  .purchase-info p {
    font-size: 13px;
    color: #a0a0a0;
    margin: 6px 0;
  }
  .view-btn {
    margin-top: 12px;
    color: #409EFF;
    font-size: 13px;
    padding: 0;
  }
  .view-btn i {
    transition: transform 0.2s;
  }
  .purchase-card:hover .view-btn i {
    transform: translateX(4px);
  }
  
  /* 空状态 */
  .empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #a0a0a0;
  }
  .empty-state i {
    font-size: 64px;
    margin-bottom: 16px;
    opacity: 0.5;
  }
  .empty-state p {
    font-size: 14px;
    margin: 0;
  }
  
  /* 页脚 */
  .footer {
    margin-top: 60px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    background: rgba(10, 10, 10, 0.8);
    backdrop-filter: blur(10px);
  }
  
  /* 响应式 */
  @media screen and (max-width: 768px) {
    .order-info, .order-time {
      flex-direction: column;
      gap: 6px;
    }
    .purchase-list {
      grid-template-columns: 1fr;
    }
  }
  </style>
