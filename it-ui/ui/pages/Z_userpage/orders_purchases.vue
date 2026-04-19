<template>
  <div class="assets-page">
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
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-kicker">Asset Overview</span>
          <h1 class="page-title">我的资产</h1>
          <p class="page-subtitle">集中查看账户订单、购买足迹与会员状态，让每一次支付都更清楚可追溯。</p>
        </div>
        <div class="hero-stats">
          <div class="stat-card">
            <span class="stat-label">订单总数</span>
            <strong class="stat-value">{{ orders.length }}</strong>
          </div>
          <div class="stat-card">
            <span class="stat-label">会员状态</span>
            <strong class="stat-value">{{ isVip ? 'VIP 已开通' : '普通用户' }}</strong>
          </div>
          <div class="stat-card">
            <span class="stat-label">到期时间</span>
            <strong class="stat-value">{{ vipExpireDate ? formatDate(vipExpireDate) : '未开通会员' }}</strong>
          </div>
        </div>
      </section>

      <section class="tabs-shell">
        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane label="我的订单" name="orders">
            <div v-loading="ordersLoading" class="tab-content">
              <div v-if="orders.length === 0" class="empty-state">
                <i class="el-icon-document"></i>
                <h3>暂无订单</h3>
                <p>当你购买内容或开通会员后，订单会出现在这里。</p>
              </div>
              <div v-else class="order-list">
                <article v-for="order in orders" :key="order.id" class="order-card">
                  <div class="order-top">
                    <div>
                      <span class="order-caption">订单编号</span>
                      <div class="order-no">{{ order.orderNo }}</div>
                    </div>
                    <el-tag :type="getOrderStatusType(order.status)" size="small" effect="dark">
                      {{ getOrderStatusText(order.status) }}
                    </el-tag>
                  </div>

                  <div class="order-main">
                    <div class="order-amount">
                      <span class="amount-label">订单金额</span>
                      <strong>¥{{ order.amount }}</strong>
                    </div>
                    <div class="order-meta-grid">
                      <div class="meta-item">
                        <span class="meta-label">订单类型</span>
                        <span class="meta-value">{{ order.type === 'content' ? '内容购买' : 'VIP会员' }}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">支付方式</span>
                        <span class="meta-value">{{ order.paymentMethod || '-' }}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">创建时间</span>
                        <span class="meta-value">{{ formatDate(order.createdAt) }}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">支付时间</span>
                        <span class="meta-value">{{ order.payTime ? formatDate(order.payTime) : '待支付' }}</span>
                      </div>
                    </div>
                  </div>
                </article>
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
      </section>
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
import { GetCurrentUser, GetOrdersByUser, GetUserPurchases } from '@/api/index.js'
import { useUserStore } from '@/store/user'
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
          const userStore = useUserStore();
          await userStore.logout();
          this.$message.success('已退出登录');
        } catch (error) {
          console.error('退出登录失败', error);
        } finally {
          this.$router.push('/login');
        }
      },
    },
  };
  </script>
  
<style scoped>
.assets-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, var(--it-glow-secondary) 0, transparent 24%),
    radial-gradient(circle at top right, var(--it-glow-primary) 0, transparent 22%),
    var(--it-page-bg);
  color: var(--it-text);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: color-mix(in srgb, var(--it-header-bg) 92%, transparent);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--it-border);
  transition: all 0.3s ease;
  padding: 12px 0;
}

.navbar-scrolled { box-shadow: var(--it-shadow-strong); }

.navbar-content {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.logo-icon { font-size: 28px; color: var(--it-accent); line-height: 1; }
.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: var(--it-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nav-actions { display: flex; align-items: center; gap: 20px; }

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 999px;
  transition: background 0.3s ease;
}
.user-info:hover { background: var(--it-accent-soft); }
.username { font-size: 14px; font-weight: 500; color: var(--it-text); }

.main-content { max-width: 1320px; margin: 0 auto; padding: 34px 28px 56px; }

.hero-panel,
.tabs-shell {
  background: linear-gradient(145deg, color-mix(in srgb, var(--it-surface-elevated) 88%, white) 0%, var(--it-panel-bg) 100%);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-strong);
  backdrop-filter: blur(20px);
}

.hero-panel {
  border-radius: 30px;
  padding: 30px;
  margin-bottom: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 24px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  border: 1px solid var(--it-border);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 50px);
  line-height: 1.05;
  background: var(--it-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.page-subtitle { margin: 0; max-width: 640px; font-size: 15px; line-height: 1.8; color: var(--it-text-muted); }
.hero-stats { display: grid; gap: 14px; }
.stat-card {
  padding: 18px 20px;
  border-radius: 22px;
  background: var(--it-panel-bg-strong);
  border: 1px solid var(--it-border);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.28);
}
.stat-label { display: block; margin-bottom: 8px; font-size: 12px; color: var(--it-text-subtle); }
.stat-value { font-size: 21px; line-height: 1.5; color: var(--it-text); }
.tabs-shell { border-radius: 30px; padding: 16px; }
.tabs :deep(.el-tabs__header) { margin: 0 0 18px; border-bottom: 1px solid var(--it-border); }
.tabs :deep(.el-tabs__item) { color: var(--it-text-subtle); font-size: 16px; font-weight: 600; }
.tabs :deep(.el-tabs__item.is-active) { color: var(--it-accent); }
.tabs :deep(.el-tabs__active-bar) { background: var(--it-primary-gradient); }
.tabs :deep(.el-tabs__nav-wrap::after) { background: var(--it-border); }
.tab-content { min-height: 420px; padding: 10px 8px 8px; }
.order-list { display: grid; gap: 18px; }
.order-card {
  padding: 22px;
  border-radius: 26px;
  background: linear-gradient(145deg, var(--it-panel-bg-strong), var(--it-surface-elevated));
  border: 1px solid var(--it-border);
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
  box-shadow: var(--it-shadow-soft);
}
.order-card:hover { transform: translateY(-4px); border-color: var(--it-border-strong); box-shadow: var(--it-shadow-hover); }
.order-top { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 18px; flex-wrap: wrap; }
.order-caption { display: block; margin-bottom: 6px; font-size: 12px; color: var(--it-text-subtle); }
.order-no { font-size: 18px; font-weight: 700; letter-spacing: 0.02em; color: var(--it-text); word-break: break-all; }
.order-main { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 20px; }
.order-amount { padding: 20px; border-radius: 22px; background: linear-gradient(135deg, var(--it-tone-info-soft), var(--it-tone-cyan-soft)); border: 1px solid var(--it-tone-info-border); }
.amount-label,
.meta-label { display: block; margin-bottom: 8px; font-size: 12px; color: var(--it-text-subtle); }
.order-amount strong { font-size: 30px; line-height: 1.2; color: var(--it-accent); }
.order-meta-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.meta-item { padding: 18px; border-radius: 20px; background: var(--it-panel-bg); border: 1px solid var(--it-border); }
.meta-value { display: block; color: var(--it-text); line-height: 1.6; word-break: break-word; }
.empty-state { padding: 80px 24px; text-align: center; border-radius: 26px; background: var(--it-panel-bg-strong); border: 1px dashed var(--it-border-strong); }
.empty-state i { font-size: 68px; margin-bottom: 18px; color: var(--it-accent); opacity: .55; }
.empty-state h3 { margin: 0 0 10px; font-size: 22px; color: var(--it-text); }
.empty-state p { margin: 0; color: var(--it-text-muted); }
.footer { margin-top: 56px; border-top: 1px solid var(--it-border); background: color-mix(in srgb, var(--it-header-bg) 90%, transparent); backdrop-filter: blur(18px); }

@media screen and (max-width: 920px) {
  .hero-panel,
  .order-main { grid-template-columns: 1fr; }
}

@media screen and (max-width: 768px) {
  .navbar-content,
  .main-content { padding-left: 18px; padding-right: 18px; }
  .hero-panel,
  .tabs-shell,
  .order-card { padding: 22px; }
  .order-meta-grid { grid-template-columns: 1fr; }
}
</style>
