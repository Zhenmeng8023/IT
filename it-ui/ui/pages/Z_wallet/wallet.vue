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
        <div class="balance-card">
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
        </div>
  
        <!-- 收益明细区域 -->
        <div class="section-card">
          <div class="section-header">
            <h2>收益明细</h2>
            <el-select v-model="revenueType" size="small" @change="loadRevenueHistory">
              <el-option label="全部" value="all"></el-option>
              <el-option label="知识产品" value="knowledge"></el-option>
              <el-option label="其他" value="other"></el-option>
            </el-select>
          </div>
          <div v-loading="revenueLoading" class="revenue-list">
            <el-table :data="revenueHistory" style="width: 100%">
              <el-table-column prop="date" label="日期" width="180"></el-table-column>
              <el-table-column prop="type" label="类型"></el-table-column>
              <el-table-column prop="amount" label="金额" width="120" align="right">
                <template slot-scope="scope">
                  <span class="amount-positive">+¥{{ scope.row.amount.toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100"></el-table-column>
            </el-table>
            <div v-if="revenueHistory.length === 0 && !revenueLoading" class="empty-list">
              <i class="el-icon-coin"></i>
              <p>暂无收益记录</p>
            </div>
          </div>
        </div>

        <!-- 提现功能区域 -->
        <div class="section-card">
          <div class="section-header">
            <h2>提现</h2>
          </div>
          <div class="withdraw-form">
            <el-form :model="withdrawForm" label-width="80px">
              <el-form-item label="提现金额">
                <el-input-number 
                  v-model="withdrawForm.amount" 
                  :min="10" 
                  :max="balance" 
                  :step="10" 
                  placeholder="请输入提现金额"
                  style="width: 100%"
                ></el-input-number>
                <div class="balance-tip">可用余额：¥{{ balance.toFixed(2) }}</div>
              </el-form-item>
              <el-form-item label="提现方式">
                <el-radio-group v-model="withdrawForm.method">
                  <el-radio label="alipay">支付宝</el-radio>
                  <el-radio label="wechat">微信</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="账号">
                <el-input v-model="withdrawForm.account" placeholder="请输入提现账号"></el-input>
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="withdrawForm.name" placeholder="请输入真实姓名"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button 
                  type="primary" 
                  @click="handleWithdraw" 
                  :disabled="withdrawForm.amount <= 0 || withdrawForm.amount > balance"
                  class="action-btn"
                >
                  确认提现
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 提现记录区域 -->
        <div class="section-card">
          <div class="section-header">
            <h2>提现记录</h2>
          </div>
          <div v-loading="withdrawLoading" class="withdraw-list">
            <el-table :data="withdrawHistory" style="width: 100%">
              <el-table-column prop="date" label="日期" width="180"></el-table-column>
              <el-table-column prop="amount" label="金额" width="120" align="right">
                <template slot-scope="scope">
                  <span class="amount-negative">-¥{{ scope.row.amount.toFixed(2) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="method" label="提现方式" width="120"></el-table-column>
              <el-table-column prop="status" label="状态" width="100"></el-table-column>
            </el-table>
            <div v-if="withdrawHistory.length === 0 && !withdrawLoading" class="empty-list">
              <i class="el-icon-upload2"></i>
              <p>暂无提现记录</p>
            </div>
          </div>
        </div>

        <!-- VIP 套餐区域 -->
        <div class="section-card">
          <div class="section-header">
            <h2>VIP 会员</h2>
            <span v-if="isVip && vipExpireDate">有效期至：{{ formatDate(vipExpireDate) }}</span>
            <span v-else>畅享专属权益</span>
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
              <el-radio label="wechat">微信支付</el-radio>
              <el-radio label="alipay">支付宝支付</el-radio>
            </el-radio-group>
          </div>
          
          <!-- 优惠券选择 -->
          <div class="coupon-section">
            <div class="coupon-header">
              <span class="coupon-label">使用优惠券：</span>
              <el-button type="text" size="small" @click="loadAvailableCoupons" :loading="couponsLoading">
                <i class="el-icon-refresh"></i> 刷新
              </el-button>
            </div>
            
            <!-- 有优惠券时显示下拉选择 -->
            <el-select 
              v-if="availableCoupons.length > 0"
              v-model="selectedCouponId" 
              placeholder="请选择优惠券（可选）"
              clearable
              style="width: 100%"
              @change="handleCouponChange"
            >
              <el-option
                v-for="coupon in availableCoupons"
                :key="coupon.id"
                :label="getCouponLabel(coupon)"
                :value="coupon.id"
              >
                <span style="float: left">{{ coupon.couponName }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">
                  {{ getCouponDiscountText(coupon) }}
                </span>
              </el-option>
            </el-select>
            
            <!-- 无优惠券时显示提示 -->
            <div v-else class="no-coupon-tip">
              <i class="el-icon-ticket"></i>
              <p>暂无可用优惠券</p>
              <el-button type="primary" size="small" @click="$router.push('/user/coupons')">
                去领取优惠券
              </el-button>
            </div>
            
            <div v-if="selectedCouponId && discountAmount > 0" class="discount-info">
              <span>优惠金额：</span>
              <span class="discount-amount">-¥{{ discountAmount.toFixed(2) }}</span>
            </div>
          </div>
          
          <!-- 价格明细 -->
          <div class="price-summary" v-if="selectedPlan">
            <div class="price-item">
              <span>套餐价格：</span>
              <span>¥{{ selectedPlanPrice }}</span>
            </div>
            <div class="price-item" v-if="discountAmount > 0">
              <span>优惠券优惠：</span>
              <span class="discount-text">-¥{{ discountAmount.toFixed(2) }}</span>
            </div>
            <div class="price-item total">
              <span>实付金额：</span>
              <span class="total-price">¥{{ finalPrice.toFixed(2) }}</span>
            </div>
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
  // 导入优惠券 API
  import { getUserAvailableCoupons, calculateDiscount } from '@/api/coupon'
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
        balance: 1000.00,
        balanceLoading: false,
        vipPlans: [],
        selectedPlan: null,
        payMethod: '',
        vipLoading: false,
        isVip: false, // 是否是 VIP 会员
        vipExpireDate: null, // VIP 过期时间
        
        // 优惠券相关
        availableCoupons: [],
        selectedCouponId: null,
        discountAmount: 0,
        couponsLoading: false,
        
        // 收益相关
        revenueType: 'all',
        revenueLoading: false,
        revenueHistory: [
          {
            id: 1,
            date: '2026-03-28',
            type: '知识产品',
            amount: 99.00,
            status: '已到账'
          },
          {
            id: 2,
            date: '2026-03-25',
            type: '知识产品',
            amount: 199.00,
            status: '已到账'
          },
          {
            id: 3,
            date: '2026-03-20',
            type: '其他',
            amount: 50.00,
            status: '已到账'
          }
        ],
        
        // 提现相关
        withdrawForm: {
          amount: 100,
          method: 'alipay',
          account: '',
          name: ''
        },
        withdrawLoading: false,
        withdrawHistory: [
          {
            id: 1,
            date: '2026-03-15',
            amount: 500.00,
            method: '支付宝',
            status: '已完成'
          },
          {
            id: 2,
            date: '2026-03-10',
            amount: 300.00,
            method: '微信',
            status: '已完成'
          }
        ]
      };
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll);
      this.getUserInfo();
      this.fetchVipPlans();
      this.loadAvailableCoupons(); // 加载可用优惠券
    },
    beforeDestroy() {
      window.removeEventListener('scroll', this.handleScroll);
    },
    computed: {
      // 选中的套餐价格
      selectedPlanPrice() {
        if (!this.selectedPlan) return 0;
        const plan = this.vipPlans.find(p => p.id === this.selectedPlan);
        return plan ? plan.price : 0;
      },
      // 最终价格（扣除优惠）
      finalPrice() {
        return Math.max(0, this.selectedPlanPrice - this.discountAmount);
      }
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
        // 选择套餐时重新计算优惠
        if (this.selectedCouponId) {
          this.calculateDiscountAmount();
        }
      },
      
      // 加载可用优惠券
      async loadAvailableCoupons() {
        if (!this.userId) return;
        
        this.couponsLoading = true;
        try {
          const res = await getUserAvailableCoupons(this.userId);
          if (res.data.success) {
            this.availableCoupons = res.data.data || [];
          }
        } catch (error) {
          console.error('加载优惠券失败:', error);
        } finally {
          this.couponsLoading = false;
        }
      },
      
      // 优惠券选择变化
      async handleCouponChange(couponId) {
        if (!couponId) {
          this.discountAmount = 0;
          return;
        }
        
        await this.calculateDiscountAmount();
      },
      
      // 计算优惠金额
      async calculateDiscountAmount() {
        if (!this.selectedCouponId || !this.selectedPlanPrice) {
          this.discountAmount = 0;
          return;
        }
        
        try {
          const res = await calculateDiscount({
            couponId: this.selectedCouponId,
            orderAmount: this.selectedPlanPrice
          });
          
          if (res.data.success) {
            const finalAmount = parseFloat(res.data.finalAmount);
            this.discountAmount = this.selectedPlanPrice - finalAmount;
          }
        } catch (error) {
          console.error('计算优惠失败:', error);
          this.$message.warning('优惠券不可用');
          this.selectedCouponId = null;
          this.discountAmount = 0;
        }
      },
      
      // 获取优惠券显示文本
      getCouponLabel(coupon) {
        return `${coupon.couponName} (${this.getCouponDiscountText(coupon)})`;
      },
      
      getCouponDiscountText(coupon) {
        if (coupon.couponType === 'DISCOUNT') {
          return `${coupon.value}折`;
        } else {
          return `减¥${coupon.value}`;
        }
      },
      async handleBuyVip() {
        if (!this.selectedPlan) {
          this.$message.warning('请选择一个 VIP 套餐');
          return;
        }
        const plan = this.vipPlans.find(p => p.id === this.selectedPlan);
        if (!plan) return;
        
        // 如果是续费，给出提示
        if (this.isVip) {
          const action = await this.$confirm(`您已是 VIP 会员，确定要续费吗？`, '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).catch(() => null);
          
          if (!action) return;
        }
              
        // 验证是否选择了支付方式
        if (!this.payMethod) {
          this.$message.warning('请选择支付方式');
          return;
        }
        
        // 验证用户是否已登录
        if (!this.userId) {
          this.$message.error('用户未登录，请先登录');
          this.$router.push('/login');
          return;
        }
              
        this.vipLoading = true;
        try {
          // 调用新的VIP购买接口（直接使用PayUtil）
          const params = {
            userId: this.userId,
            membershipLevelId: this.selectedPlan
          };
          
          // 如果选择了优惠券，添加优惠券ID
          if (this.selectedCouponId) {
            params.couponId = this.selectedCouponId;
          }
          
          const response = await axios.post('/api/membership/buy-vip', null, {
            params: params
          });
          
          console.log('VIP购买响应:', response.data);
          
          if (response.data.code === 200) {
            const { orderNo, amount, paymentForm } = response.data.data;
            
            // 判断支付方式
            if (this.payMethod === 'alipay') {
              // 支付宝支付：打开新窗口写入表单
              const newWindow = window.open('about:_blank');
              newWindow.document.write(paymentForm);
              newWindow.document.close();
              
              this.$message.success('正在跳转到支付宝支付页面...');
              
              // 开始轮询检查支付状态
              this.checkPaymentStatusByOrderNo(orderNo);
            } else {
              // TODO: 微信支付逻辑（后续实现）
              this.$message.info('微信支付功能开发中');
            }
          } else {
            this.$message.error(response.data.message || '购买失败');
          }
        } catch (error) {
          console.error('VIP购买失败:', error);
          this.$message.error(error.response?.data?.message || '购买失败，请稍后重试');
        } finally {
          this.vipLoading = false;
        }
      },

      // 通过订单号检查支付状态
      checkPaymentStatusByOrderNo(orderNo) {
        let checkCount = 0;
        const maxChecks = 20; // 最多检查20次（60秒）
        
        const interval = setInterval(async () => {
          checkCount++;
          
          try {
            // 查询订单状态
            const response = await axios.get(`/api/orders/order-no/${orderNo}`);
            const order = response.data;
            
            if (order.status === 'PAID') {
              clearInterval(interval);
              this.$message.success('购买成功，您现在已是 VIP 会员');
              await this.getUserInfo(); // 刷新余额和 VIP 状态
              await this.fetchUserMembershipStatus(); // 重新获取会员状态
            } else if (checkCount >= maxChecks) {
              // 超时停止检查
              clearInterval(interval);
              this.$message.warning('支付超时，请检查订单状态');
            }
          } catch (error) {
            console.error('检查支付状态失败', error);
            if (checkCount >= maxChecks) {
              clearInterval(interval);
            }
          }
        }, 3000); // 每3秒检查一次
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
      
      // 加载收益历史
      loadRevenueHistory() {
        this.revenueLoading = true;
        // 模拟API调用
        setTimeout(() => {
          this.revenueLoading = false;
        }, 500);
      },
      
      // 处理提现
      handleWithdraw() {
        if (this.withdrawForm.amount <= 0 || this.withdrawForm.amount > this.balance) {
          this.$message.error('请输入正确的提现金额');
          return;
        }
        
        if (!this.withdrawForm.account || !this.withdrawForm.name) {
          this.$message.error('请填写完整的提现信息');
          return;
        }
        
        this.withdrawLoading = true;
        // 模拟API调用
        setTimeout(() => {
          this.balance -= this.withdrawForm.amount;
          this.withdrawHistory.unshift({
            id: Date.now(),
            date: new Date().toISOString().split('T')[0],
            amount: this.withdrawForm.amount,
            method: this.withdrawForm.method === 'alipay' ? '支付宝' : '微信',
            status: '处理中'
          });
          this.$message.success('提现申请已提交');
          this.withdrawForm = {
            amount: 100,
            method: 'alipay',
            account: '',
            name: ''
          };
          this.withdrawLoading = false;
        }, 1000);
      },
      
      // 格式化日期
      formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')}`;
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
  
  /* 收益和提现相关样式 */
  .revenue-list, .withdraw-list {
    margin-top: 20px;
  }
  
  .withdraw-form {
    max-width: 500px;
  }
  
  .balance-tip {
    font-size: 12px;
    color: #a0a0a0;
    margin-top: 5px;
  }
  
  .amount-positive {
    color: #67c23a;
    font-weight: bold;
  }
  
  .amount-negative {
    color: #f56c6c;
    font-weight: bold;
  }
  
  .empty-list {
    text-align: center;
    padding: 40px 0;
    color: #a0a0a0;
  }
  
  .empty-list i {
    font-size: 48px;
    margin-bottom: 10px;
    opacity: 0.5;
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
    .withdraw-form {
      max-width: 100%;
    }
  }
  
  /* 优惠券区域样式 */
  .coupon-section {
    margin: 20px 0;
  }
  
  .coupon-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }
  
  .coupon-label {
    font-size: 14px;
    color: #a0a0a0;
  }
  
  .no-coupon-tip {
    text-align: center;
    padding: 30px 20px;
    background: rgba(255, 255, 255, 0.02);
    border: 1px dashed rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    color: #a0a0a0;
  }
  
  .no-coupon-tip i {
    font-size: 36px;
    margin-bottom: 10px;
    opacity: 0.5;
  }
  
  .no-coupon-tip p {
    margin: 10px 0 15px;
    font-size: 14px;
  }
  
  .discount-info {
    margin-top: 10px;
    padding: 10px;
    background: rgba(103, 194, 58, 0.1);
    border-radius: 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .discount-amount {
    color: #67c23a;
    font-weight: bold;
    font-size: 16px;
  }
  
  .price-summary {
    margin: 15px 0;
    padding: 15px;
    background: rgba(255, 255, 255, 0.02);
    border-radius: 12px;
  }
  
  .price-item {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    font-size: 14px;
    color: #a0a0a0;
  }
  
  .price-item.total {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    margin-top: 8px;
    padding-top: 12px;
    font-weight: bold;
    color: #ffffff;
  }
  
  .total-price {
    color: #f5a623;
    font-size: 20px;
  }
  
  .discount-text {
    color: #67c23a;
  }
  </style>