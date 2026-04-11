<template>
    <div class="vip-benefits-page">
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
          <h1 class="page-title">VIP会员权益</h1>
          <p class="page-subtitle">解锁更多特权，享受极致体验</p>
        </div>
  
        <!-- 当前VIP状态卡片 -->
        <div class="status-card" v-if="isLoggedIn">
          <div class="status-icon">
            <i class="el-icon-medal"></i>
          </div>
          <div class="status-info">
            <h3 v-if="isVip">您是尊贵的VIP会员</h3>
            <h3 v-else>您还不是VIP会员</h3>
            <p v-if="isVip && vipExpireDate">
              会员有效期至：{{ formatDate(vipExpireDate) }}
            </p>
            <p v-else>开通VIP，畅享全部权益</p>
          </div>
          <el-button v-if="!isVip" type="warning" @click="goToWallet">立即开通</el-button>
          <el-button v-else type="primary" plain @click="goToWallet">续费升级</el-button>
        </div>
  
        <!-- 会员等级卡片列表 -->
        <div class="plans-section">
          <div class="section-title">
            <h2>会员套餐</h2>
            <span>选择适合你的套餐，开启VIP之旅</span>
          </div>
          <div class="plans-grid">
            <div v-for="plan in vipPlans" :key="plan.id" class="plan-card">
              <div class="plan-header">
                <h3>{{ plan.name }}</h3>
                <div class="plan-price">
                  <span class="price">¥{{ plan.price }}</span>
                  <span class="duration">/{{ plan.durationDays }}天</span>
                </div>
              </div>
              <div class="plan-body">
                <ul class="benefits-list">
                  <li v-for="(benefit, index) in getBenefits(plan)" :key="index">
                    <i class="el-icon-check"></i> {{ benefit }}
                  </li>
                </ul>
              </div>
              <div class="plan-footer">
                <el-button
                  :type="isVip ? 'info' : 'warning'"
                  :plain="isVip"
                  @click="handlePurchase(plan)"
                >
                  {{ isVip ? '续费' : '立即开通' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
  
        <!-- 权益对比表格（可选，用于展示所有等级的详细权益） -->
        <div class="comparison-section">
          <div class="section-title">
            <h2>权益对比</h2>
            <span>所有会员等级权益一览</span>
          </div>
          <el-table :data="vipPlans" stripe class="comparison-table">
            <el-table-column prop="name" label="套餐" width="150" />
            <el-table-column prop="price" label="价格" width="120">
              <template slot-scope="scope">
                ¥{{ scope.row.price }} / {{ scope.row.durationDays }}天
              </template>
            </el-table-column>
            <el-table-column label="权益" min-width="300">
              <template slot-scope="scope">
                <div class="benefits-tag-list">
                  <el-tag
                    v-for="(benefit, idx) in getBenefits(scope.row)"
                    :key="idx"
                    size="small"
                    type="success"
                    style="margin: 2px 4px;"
                  >
                    {{ benefit }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template slot-scope="scope">
                <el-button
                  type="text"
                  :disabled="isVip && isCurrentVipPlan(scope.row)"
                  @click="handlePurchase(scope.row)"
                >
                  {{ isVip && isCurrentVipPlan(scope.row) ? '当前套餐' : '选择' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
  
      <footer class="footer">
        <FooterPlayer />
      </footer>
    </div>
  </template>
  
  <script>
  import FooterPlayer from '../Z_userpage/components/FooterPlayer.vue'
  // 导入接口：获取当前用户信息
  import { GetCurrentUser } from '@/api'
  // 导入 axios 实例
  import axios from 'axios'
  import { clearAuthState } from '@/utils/auth'

  export default {
    layout: 'default',
    components: { FooterPlayer },
    data() {
      return {
        scrolled: false, // 导航栏滚动状态
        userAvatar: '/pic/choubi.jpg', // 用户头像
        username: '', // 用户名
        nickname: '', // 用户昵称
        userId: null, // 用户ID
        isLoggedIn: false, // 是否登录
        isVip: false, // 是否是VIP会员
        vipExpireDate: null, // VIP会员过期日期
        vipPlans: [], // VIP套餐列表
      };
    },
    mounted() {
      // 监听滚动事件
      window.addEventListener('scroll', this.handleScroll);
      // 获取用户信息
      this.getUserInfo();
      // 获取VIP套餐
      this.fetchVipPlans();
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
          this.isLoggedIn = true;
          // 从用户信息中获取 VIP 状态（备用）
          this.isVip = user.isPremiumMember === true;
          this.vipExpireDate = user.premiumExpiryDate;
                
          // 调用新的会员状态接口获取最新状态
          await this.fetchUserMembershipStatus();
        } catch (error) {
          console.error('获取用户信息失败', error);
          this.isLoggedIn = false;
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
          // 接口调用失败时，使用用户信息中的 VIP 状态作为备用
        }
      },
      // 获取VIP套餐列表
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
      // 获取套餐权益列表
      getBenefits(plan) {
        // 根据套餐数据生成权益列表
        // 如果后端返回的 benefits 是数组，直接使用；否则返回默认权益
        if (plan.benefits && Array.isArray(plan.benefits)) {
          return plan.benefits;
        }
        // 默认权益（可根据实际调整）
        return [
          '免费阅读所有VIP博客',
          '专属会员标识',
          '优先审核',
          '参与专属活动',
          '积分双倍',
        ];
      },
      // 判断是否是当前VIP套餐
      isCurrentVipPlan(plan) {
        // 简单判断：如果用户已开通VIP且当前套餐价格与用户已购套餐一致（需后端提供，此处简化）
        // 实际可通过用户已购套餐ID判断，这里仅作示例
        return false;
      },
      // 处理购买/续费操作
      async handlePurchase(plan) {
        if (!this.isLoggedIn) {
          this.$message.warning('请先登录');
          this.$router.push('/login');
          return;
        }
        // 跳转到钱包页面并传递套餐信息
        this.$router.push({
          path: '/wallet',
          query: { planId: plan.id, planName: plan.name, price: plan.price }
        });
      },
      // 跳转到钱包页面
      goToWallet() {
        this.$router.push('/wallet');
      },
      // 格式化日期
      formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')}`;
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
      logout() {
        clearAuthState();
        this.$router.push('/login');
      },
    },
  };
  </script>
  
  <style scoped>
  /* ========== 全局样式 ========== */
  .vip-benefits-page {
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
  
  /* 状态卡片 */
  .status-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 24px;
    padding: 24px 30px;
    margin-bottom: 40px;
    display: flex;
    align-items: center;
    gap: 20px;
    flex-wrap: wrap;
    justify-content: space-between;
  }
  .status-icon {
    width: 50px;
    height: 50px;
    background: linear-gradient(135deg, #f5a623, #e67e22);
    border-radius: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: white;
  }
  .status-info h3 {
    margin: 0 0 5px;
    font-size: 20px;
    font-weight: 600;
  }
  .status-info p {
    margin: 0;
    color: #a0a0a0;
    font-size: 14px;
  }
  
  /* 套餐区域 */
  .plans-section {
    margin-bottom: 60px;
  }
  .section-title {
    text-align: center;
    margin-bottom: 30px;
  }
  .section-title h2 {
    font-size: 28px;
    font-weight: 600;
    margin: 0 0 5px;
    color: #ffffff;
  }
  .section-title span {
    font-size: 14px;
    color: #a0a0a0;
  }
  .plans-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 30px;
  }
  .plan-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 24px;
    padding: 30px 20px;
    transition: transform 0.3s, box-shadow 0.3s;
    text-align: center;
  }
  .plan-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 20px 30px rgba(0, 0, 0, 0.2);
    border-color: rgba(64, 158, 255, 0.3);
  }
  .plan-header h3 {
    font-size: 24px;
    font-weight: 600;
    margin: 0 0 15px;
  }
  .plan-price {
    font-size: 28px;
    font-weight: 700;
    color: #409EFF;
    margin-bottom: 20px;
  }
  .plan-price .duration {
    font-size: 14px;
    font-weight: normal;
    color: #a0a0a0;
  }
  .plan-body {
    margin: 20px 0;
    text-align: left;
  }
  .benefits-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  .benefits-list li {
    margin: 12px 0;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #e0e0e0;
  }
  .benefits-list li i {
    color: #67c23a;
    font-size: 16px;
  }
  .plan-footer {
    margin-top: 20px;
  }
  .plan-footer .el-button {
    width: 100%;
    border-radius: 30px;
    padding: 10px;
    font-weight: 500;
  }
  
  /* 对比表格 */
  .comparison-section {
    margin-top: 20px;
  }
  .comparison-table {
    background: rgba(255, 255, 255, 0.02);
    border-radius: 16px;
    overflow: hidden;
  }
  .comparison-table :deep(.el-table__header-wrapper th) {
    background: rgba(255, 255, 255, 0.05);
    color: #ffffff;
    font-weight: 600;
  }
  .comparison-table :deep(.el-table__body-wrapper td) {
    background: transparent;
    color: #e0e0e0;
    border-color: rgba(255, 255, 255, 0.05);
  }
  .benefits-tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
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
    .status-card {
      flex-direction: column;
      text-align: center;
    }
    .plans-grid {
      grid-template-columns: 1fr;
    }
    .comparison-table {
      font-size: 12px;
    }
  }
  </style>
