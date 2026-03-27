<template>
    <div class="wallet-page">
      <!-- 顶部导航栏（复用项目现有样式） -->
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
        <!-- 页面头部 -->
        <div class="page-header">
          <h1 class="page-title">我的钱包</h1>
          <p class="page-subtitle">账户余额与会员权益管理</p>
        </div>
  
        <!-- 余额卡片 -->
        <!-- <div class="balance-card">
          <div class="balance-icon">
            <i class="el-icon-coin"></i>
          </div>
          <div class="balance-info">
            <span class="balance-label">账户余额</span>
            <span class="balance-value">¥{{ balance }}</span>
            <el-button type="text" class="refresh-btn" @click="refreshBalance" :loading="balanceLoading">
              <i class="el-icon-refresh"></i>
            </el-button>
          </div>
        </div> -->
  
        <!-- VIP套餐区域 -->
        <div class="section-card">
          <div class="section-header">
            <h2>VIP会员</h2>
            <span>畅享专属权益</span>
          </div>
          <div class="vip-plans">
            <div
              v-for="plan in vipPlans"
              :key="plan.id"
              class="plan-card"
              :class="{ active: selectedPlan === plan.id }"
              @click="selectPlan(plan.id)"
            >
              <h3>{{ plan.name }}</h3>
              <p class="plan-desc">{{ plan.description }}</p>
              <div class="plan-price">¥{{ plan.price }}</div>
              <div class="plan-duration">{{ plan.durationDays }}天</div>
              <div class="plan-benefits" v-if="plan.benefits">
                <i class="el-icon-check"></i> {{ plan.benefits.join('、') }}
              </div>
            </div>
            <div v-if="vipPlans.length === 0" class="empty-tip">暂无可用套餐</div>
          </div>
          <div class="payment-method">
            <span class="method-label">支付方式：</span>
            <el-radio-group v-model="payMethod">
              <el-radio label="balance">余额支付</el-radio>
            </el-radio-group>
          </div>
          <el-button
            type="warning"
            class="action-btn"
            :loading="vipLoading"
            :disabled="!selectedPlan"
            @click="handleBuyVip"
          >
            立即开通
          </el-button>
        </div>
      </div>
  
      <!-- 页脚 -->
      <footer class="footer">
        <FooterPlayer />
      </footer>
    </div>
  </template>
  
  <script>
  import FooterPlayer from '../Z_userpage/components/FooterPlayer.vue'
  // 导入接口：获取当前用户信息、创建订单
  import { GetCurrentUser, CreateOrder } from '@/api'
  // 导入 axios 实例
  import axios from 'axios'
  
  export default {
    layout: 'default',
    components: { FooterPlayer },
    data() {
      return {
        scrolled: false,
        userAvatar: '/pic/choubi.jpg',
        username: '',
        nickname: '',
        userId: null,
        balance: 0,
        balanceLoading: false,
        vipPlans: [],
        selectedPlan: null,
        payMethod: 'balance',
        vipLoading: false,
      };
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll);
      this.getUserInfo();
      this.fetchVipPlans();
    },
    beforeDestroy() {
      window.removeEventListener('scroll', this.handleScroll);
    },
    methods: {
      handleScroll() {
        this.scrolled = window.scrollY > 50;
      },
      scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      handleAvatarError() {
        this.userAvatar = '/pic/choubi.jpg';
        return true;
      },
      async getUserInfo() {
        try {
          // 调用接口：获取当前用户信息
          const res = await GetCurrentUser();
          const user = res.data || res;
          this.userId = user.id;
          this.username = user.username;
          this.nickname = user.nickname || user.username;
          this.userAvatar = user.avatarUrl || this.userAvatar;
          this.balance = user.balance || 0;
        } catch (error) {
          console.error('获取用户信息失败', error);
        }
      },
      refreshBalance() {
        this.getUserInfo();
      },
      async fetchVipPlans() {
        try {
          // 调用接口：获取会员等级套餐
          const res = await axios.get('/api/membership-levels');
          this.vipPlans = res.data;
        } catch (error) {
          console.error('获取VIP套餐失败', error);
          this.$message.error('获取VIP套餐失败');
        }
      },
      selectPlan(id) {
        this.selectedPlan = id;
      },
      async handleBuyVip() {
        if (!this.selectedPlan) {
          this.$message.warning('请选择一个VIP套餐');
          return;
        }
        const plan = this.vipPlans.find(p => p.id === this.selectedPlan);
        if (!plan) return;
        this.vipLoading = true;
        try {
          const orderData = {
            userId: this.userId,
            type: 'membership',
            membershipLevelId: this.selectedPlan,
            amount: plan.price,
            paymentMethod: this.payMethod,
            status: 'pending',
          };
          // 调用接口：创建订单
          await axios.post('/api/orders', orderData);
          // 余额支付时，假定后端会同步扣款并更新用户VIP状态
          this.$message.success('购买成功，您现在已是VIP会员');
          await this.getUserInfo(); // 刷新余额和VIP状态
        } catch (error) {
          this.$message.error(error.response?.data?.message || '购买失败');
        } finally {
          this.vipLoading = false;
        }
      },
      handleUserCommand(command) {
        switch (command) {
          case 'profile': this.$router.push('/user'); break;
          case 'blog': this.$router.push('/blog'); break;
          case 'circle': this.$router.push('/circle'); break;
          case 'logout': this.logout(); break;
        }
      },
      logout() {
        localStorage.removeItem('token');
        this.$router.push('/login');
      },
    },
  };
  </script>
  
  <style scoped>
  /* ========== 全局样式 ========== */
  .wallet-page {
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
  
  /* 余额卡片 */
  .balance-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 24px;
    padding: 30px;
    margin-bottom: 40px;
    display: flex;
    align-items: center;
    gap: 20px;
    transition: transform 0.3s ease;
  }
  .balance-card:hover {
    transform: translateY(-4px);
    border-color: rgba(64, 158, 255, 0.3);
  }
  .balance-icon {
    width: 60px;
    height: 60px;
    background: linear-gradient(135deg, #409EFF, #66b1ff);
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: white;
  }
  .balance-info {
    flex: 1;
  }
  .balance-label {
    font-size: 14px;
    color: #a0a0a0;
    display: block;
    margin-bottom: 5px;
  }
  .balance-value {
    font-size: 32px;
    font-weight: 600;
    color: #ffffff;
    margin-right: 10px;
  }
  .refresh-btn {
    color: #a0a0a0;
    font-size: 18px;
  }
  
  /* 通用卡片样式 */
  .section-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 24px;
    padding: 30px;
    transition: transform 0.3s ease;
  }
  .section-card:hover {
    transform: translateY(-4px);
    border-color: rgba(64, 158, 255, 0.2);
  }
  .section-header {
    margin-bottom: 25px;
  }
  .section-header h2 {
    font-size: 22px;
    font-weight: 600;
    margin: 0 0 5px;
    color: #ffffff;
  }
  .section-header span {
    font-size: 13px;
    color: #a0a0a0;
  }
  
  /* VIP套餐卡片 */
  .vip-plans {
    display: flex;
    flex-direction: column;
    gap: 20px;
    margin-bottom: 25px;
  }
  .plan-card {
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    padding: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
  }
  .plan-card.active {
    background: rgba(64, 158, 255, 0.1);
    border-color: #409EFF;
  }
  .plan-card h3 {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 5px;
    color: #ffffff;
  }
  .plan-desc {
    font-size: 13px;
    color: #a0a0a0;
    margin: 0 0 12px;
  }
  .plan-price {
    font-size: 24px;
    font-weight: 600;
    color: #409EFF;
    margin-bottom: 5px;
  }
  .plan-duration {
    font-size: 12px;
    color: #a0a0a0;
    margin-bottom: 10px;
  }
  .plan-benefits {
    font-size: 12px;
    color: #c0c0c0;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .plan-benefits i {
    color: #67c23a;
  }
  .empty-tip {
    text-align: center;
    padding: 30px;
    color: #a0a0a0;
  }
  
  /* 支付方式 */
  .payment-method {
    margin: 20px 0;
  }
  .method-label {
    font-size: 14px;
    color: #a0a0a0;
    margin-right: 15px;
  }
  .payment-method :deep(.el-radio__label) {
    color: #e0e0e0;
  }
  .payment-method :deep(.el-radio__input.is-checked .el-radio__inner) {
    background-color: #409EFF;
    border-color: #409EFF;
  }
  
  /* 操作按钮 */
  .action-btn {
    width: 100%;
    border-radius: 30px;
    padding: 12px;
    font-size: 16px;
    font-weight: 500;
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .action-btn:hover {
    transform: translateY(-2px);
  }
  .action-btn.el-button--warning {
    background: linear-gradient(135deg, #f5a623, #e67e22);
    border: none;
    box-shadow: 0 4px 12px rgba(245, 166, 35, 0.2);
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
    .balance-card {
      flex-direction: column;
      text-align: center;
    }
    .vip-plans {
      gap: 15px;
    }
  }
  </style>