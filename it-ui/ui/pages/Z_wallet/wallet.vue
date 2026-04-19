<template>
    <div class="wallet-page">
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
                  :max="withdrawMaxAmount" 
                  :step="10" 
                  placeholder="请输入提现金额"
                  style="width: 100%"
                ></el-input-number>
                <div class="balance-tip">可提现金额：¥{{ withdrawMaxAmount.toFixed(2) }}</div>
              </el-form-item>
              <el-form-item label="提现方式">
                <el-radio-group v-model="withdrawForm.method" @change="handleWithdrawMethodChange">
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
                  :disabled="withdrawForm.amount <= 0 || withdrawForm.amount > withdrawMaxAmount"
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
              <el-radio label="wechat" disabled>微信支付（暂未接通）</el-radio>
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
              <el-button type="primary" size="small" @click="$router.push('/coupons')">
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
  import { pollOrderStatus } from '@/api/payment'
  import {
    buyVipMembership,
    calculateMembershipDiscount,
    getAvailableMembershipCoupons,
    getUserActiveMembership,
    getVipLevels
  } from '@/api/membership'
  import {
    getAvailableWithdrawBalance,
    getCurrentUserProfile,
    getRevenueRecordsBySourceUserId,
    getSettlementAccountsByUserId,
    getWithdrawRequestsByUserId,
    submitWithdrawRequest
  } from '@/api/wallet'
  import { useUserStore } from '@/store/user'

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
        availableWithdrawBalance: 0,
        balanceLoading: false,
        vipPlans: [],
        selectedPlan: null,
        payMethod: 'alipay',
        vipLoading: false,
        isVip: false,
        vipExpireDate: null,

        availableCoupons: [],
        selectedCouponId: null,
        discountAmount: 0,
        couponsLoading: false,

        revenueType: 'all',
        revenueLoading: false,
        revenueHistory: [],

        withdrawForm: {
          amount: 100,
          method: 'alipay',
          account: '',
          name: ''
        },
        withdrawLoading: false,
        withdrawHistory: [],
        settlementAccountId: null,
        paymentStatusTimer: null,
        paymentStatusChecking: false
      };
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll);
      this.initializeWalletPage();
    },
    beforeDestroy() {
      window.removeEventListener('scroll', this.handleScroll);
      this.stopPaymentStatusPolling();
    },
    computed: {
      selectedPlanPrice() {
        if (!this.selectedPlan) return 0;
        const plan = this.vipPlans.find(p => p.id === this.selectedPlan);
        return plan ? Number(plan.price || 0) : 0;
      },
      finalPrice() {
        return Math.max(0, this.selectedPlanPrice - this.discountAmount);
      },
      withdrawMaxAmount() {
        const amount = Number(this.availableWithdrawBalance || 0);
        return Number.isFinite(amount) ? amount : 0;
      }
    },
    methods: {
      async initializeWalletPage() {
        await this.getUserInfo();
        await Promise.all([
          this.fetchVipPlans(),
          this.fetchUserMembershipStatus(),
          this.loadAvailableCoupons(),
          this.loadRevenueHistory(),
          this.loadSettlementAccount(),
          this.loadAvailableWithdrawBalance(),
          this.loadWithdrawHistory()
        ]);
      },
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
          const user = await getCurrentUserProfile();
          this.userId = user.id;
          this.username = user.username;
          this.nickname = user.nickname || user.username;
          this.userAvatar = user.avatarUrl || this.userAvatar;
          this.balance = Number(user.balance || 0);
        } catch (error) {
          console.error('获取用户信息失败', error);
          this.$message.error('获取用户信息失败');
        }
      },
      async fetchUserMembershipStatus() {
        if (!this.userId) return;

        try {
          const result = await getUserActiveMembership(this.userId);
          if (result && result.success && result.data) {
            this.isVip = result.data.isVip === true;
            this.vipExpireDate = result.data.endTime;
          } else {
            this.isVip = false;
            this.vipExpireDate = null;
          }
        } catch (error) {
          console.error('获取会员状态失败', error);
          this.isVip = false;
          this.vipExpireDate = null;
        }
      },
      async refreshBalance() {
        this.balanceLoading = true;
        try {
          await this.getUserInfo();
          await this.loadAvailableWithdrawBalance();
        } finally {
          this.balanceLoading = false;
        }
      },
      async fetchVipPlans() {
        try {
          this.vipPlans = await getVipLevels({ enabledOnly: true });
        } catch (error) {
          console.error('获取VIP套餐失败', error);
          this.$message.error('获取VIP套餐失败');
        }
      },
      selectPlan(id) {
        this.selectedPlan = id;
        if (this.selectedCouponId) {
          this.calculateDiscountAmount();
        }
      },
      async loadAvailableCoupons() {
        if (!this.userId) return;

        this.couponsLoading = true;
        try {
          this.availableCoupons = await getAvailableMembershipCoupons(this.userId);
        } catch (error) {
          console.error('加载优惠券失败:', error);
          this.availableCoupons = [];
        } finally {
          this.couponsLoading = false;
        }
      },
      async handleCouponChange(couponId) {
        if (!couponId) {
          this.discountAmount = 0;
          return;
        }

        await this.calculateDiscountAmount();
      },
      async calculateDiscountAmount() {
        if (!this.selectedCouponId || !this.selectedPlanPrice) {
          this.discountAmount = 0;
          return;
        }

        try {
          const result = await calculateMembershipDiscount({
            couponId: this.selectedCouponId,
            orderAmount: this.selectedPlanPrice
          });

          if (result && result.success) {
            const finalAmount = Number(result.finalAmount);
            this.discountAmount = Number.isFinite(finalAmount)
              ? Math.max(0, this.selectedPlanPrice - finalAmount)
              : 0;
          } else {
            throw new Error(result && result.message ? result.message : '优惠券不可用');
          }
        } catch (error) {
          console.error('计算优惠失败:', error);
          this.$message.warning('优惠券不可用');
          this.selectedCouponId = null;
          this.discountAmount = 0;
        }
      },
      getCouponLabel(coupon) {
        return `${coupon.couponName} (${this.getCouponDiscountText(coupon)})`;
      },
      getCouponDiscountText(coupon) {
        const couponType = (coupon.couponType || coupon.type || '').toLowerCase();
        if (couponType === 'discount') {
          return `${coupon.value}折`;
        }
        return `减¥${coupon.value}`;
      },
      async handleBuyVip() {
        if (!this.selectedPlan) {
          this.$message.warning('请选择一个 VIP 套餐');
          return;
        }

        const plan = this.vipPlans.find(p => p.id === this.selectedPlan);
        if (!plan) return;

        if (this.isVip) {
          const action = await this.$confirm('您已是 VIP 会员，确定要续费吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).catch(() => null);

          if (!action) return;
        }

        if (!this.payMethod) {
          this.$message.warning('请选择支付方式');
          return;
        }

        if (this.payMethod !== 'alipay') {
          this.$message.warning('当前仅支持支付宝支付');
          return;
        }

        if (!this.userId) {
          this.$message.error('用户未登录，请先登录');
          this.$router.push('/login');
          return;
        }

        this.vipLoading = true;
        try {
          const response = await buyVipMembership({
            userId: this.userId,
            membershipLevelId: this.selectedPlan,
            couponId: this.selectedCouponId
          });

          if (response && response.code === 200) {
            const { orderNo, paymentForm } = response.data || {};

            if (!orderNo || !paymentForm) {
              throw new Error('支付订单返回不完整');
            }

            const newWindow = window.open('about:blank');
            if (!newWindow) {
              this.$message.error('浏览器阻止了支付窗口，请允许弹窗后重试');
              return;
            }

            newWindow.document.write(paymentForm);
            newWindow.document.close();

            this.$message.success('正在跳转到支付宝支付页面...');
            this.checkPaymentStatusByOrderNo(orderNo);
          } else {
            this.$message.error((response && response.message) || '购买失败');
          }
        } catch (error) {
          console.error('VIP购买失败:', error);
          this.$message.error(error.response?.data?.message || error.message || '购买失败，请稍后重试');
        } finally {
          this.vipLoading = false;
        }
      },
      checkPaymentStatusByOrderNo(orderNo) {
        if (!orderNo) return;

        this.stopPaymentStatusPolling();
        let checkCount = 0;
        const maxChecks = 20;

        this.paymentStatusTimer = setInterval(async () => {
          if (this.paymentStatusChecking) return;

          this.paymentStatusChecking = true;
          checkCount += 1;

          try {
            const snapshot = await pollOrderStatus(orderNo);
            if (snapshot.paid) {
              this.stopPaymentStatusPolling();
              this.$message.success('购买成功，您现在已是 VIP 会员');
              await this.getUserInfo();
              await Promise.all([
                this.fetchUserMembershipStatus(),
                this.loadAvailableCoupons(),
                this.loadRevenueHistory(),
                this.loadAvailableWithdrawBalance()
              ]);
              return;
            }

            if (snapshot.failed || checkCount >= maxChecks) {
              this.stopPaymentStatusPolling();
              this.$message.warning('支付结果确认超时，请稍后刷新钱包页查看状态');
            }
          } catch (error) {
            console.error('检查支付状态失败', error);
            if (checkCount >= maxChecks) {
              this.stopPaymentStatusPolling();
            }
          } finally {
            this.paymentStatusChecking = false;
          }
        }, 3000);
      },
      stopPaymentStatusPolling() {
        if (this.paymentStatusTimer) {
          clearInterval(this.paymentStatusTimer);
          this.paymentStatusTimer = null;
        }
        this.paymentStatusChecking = false;
      },
      handleUserCommand(command) {
        switch (command) {
          case 'profile': this.$router.push('/user'); break;
          case 'blog': this.$router.push('/blog'); break;
          case 'circle': this.$router.push('/circle'); break;
          case 'logout': this.logout(); break;
        }
      },
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
      async loadRevenueHistory() {
        if (!this.userId) {
          this.revenueHistory = [];
          return;
        }

        this.revenueLoading = true;
        try {
          const records = await getRevenueRecordsBySourceUserId(this.userId);
          const mappedRecords = records.map(item => this.mapRevenueRecord(item));
          this.revenueHistory = this.filterRevenueRecords(mappedRecords);
        } catch (error) {
          console.error('加载收益历史失败:', error);
          this.revenueHistory = [];
        } finally {
          this.revenueLoading = false;
        }
      },
      mapRevenueRecord(item) {
        return {
          id: item.id,
          date: this.formatDate(item.createdAt || item.settledAt || item.updatedAt),
          type: '知识产品',
          amount: Number(item.authorRevenue || 0),
          status: this.getRevenueStatusText(item.settlementStatus)
        };
      },
      filterRevenueRecords(records) {
        if (this.revenueType === 'knowledge' || this.revenueType === 'all') {
          return records;
        }
        return [];
      },
      async handleWithdraw() {
        if (this.withdrawForm.amount <= 0 || this.withdrawForm.amount > this.withdrawMaxAmount) {
          this.$message.error('请输入正确的提现金额');
          return;
        }

        if (!this.withdrawForm.account || !this.withdrawForm.name) {
          this.$message.error('请填写完整的提现信息');
          return;
        }

        if (!this.settlementAccountId) {
          this.$message.error('请先设置结算账户');
          return;
        }

        this.withdrawLoading = true;
        try {
          const response = await submitWithdrawRequest({
            userId: this.userId,
            settlementAccountId: this.settlementAccountId,
            withdrawAmount: this.withdrawForm.amount,
            remark: `提现至${this.getWithdrawMethodText(this.withdrawForm.method)}: ${this.withdrawForm.account}`
          });

          if (response && response.success) {
            this.$message.success('提现申请已提交，请等待审核');
            this.withdrawForm = {
              amount: 100,
              method: 'alipay',
              account: '',
              name: ''
            };
            this.settlementAccountId = null;
            await this.getUserInfo();
            await Promise.all([
              this.loadAvailableWithdrawBalance(),
              this.loadSettlementAccount(),
              this.loadWithdrawHistory()
            ]);
          } else {
            this.$message.error((response && response.message) || '提现申请失败');
          }
        } catch (error) {
          console.error('提现申请失败:', error);
          this.$message.error(error.response?.data?.message || error.message || '提现申请失败，请稍后重试');
        } finally {
          this.withdrawLoading = false;
        }
      },
      async handleWithdrawMethodChange() {
        this.withdrawForm.account = '';
        this.withdrawForm.name = '';
        this.settlementAccountId = null;
        await this.loadSettlementAccount();
      },
      async loadSettlementAccount() {
        if (!this.userId) return;

        try {
          const accounts = await getSettlementAccountsByUserId(this.userId);
          const preferredType = String(this.withdrawForm.method || '').toUpperCase();
          const activeAccount = accounts.find(acc =>
            acc.status === 'ACTIVE' && acc.accountType === preferredType
          );

          if (activeAccount) {
            this.settlementAccountId = activeAccount.id;
            this.withdrawForm.account = activeAccount.accountNumber;
            this.withdrawForm.name = activeAccount.accountName;
          } else {
            this.settlementAccountId = null;
          }
        } catch (error) {
          console.error('加载结算账户失败:', error);
          this.settlementAccountId = null;
        }
      },
      async loadAvailableWithdrawBalance() {
        if (!this.userId) return;

        try {
          this.availableWithdrawBalance = await getAvailableWithdrawBalance(this.userId);
        } catch (error) {
          console.error('加载可提现余额失败:', error);
          this.availableWithdrawBalance = 0;
        }
      },
      async loadWithdrawHistory() {
        if (!this.userId) return;

        this.withdrawLoading = true;
        try {
          const requests = await getWithdrawRequestsByUserId(this.userId);
          this.withdrawHistory = requests.map(item => ({
            id: item.id,
            date: this.formatDate(item.applyTime || item.createdAt),
            amount: Number(item.withdrawAmount || 0),
            method: this.getWithdrawMethodTextByAccountType(item.accountType),
            status: this.getWithdrawStatusText(item.status)
          }));
        } catch (error) {
          console.error('加载提现历史失败:', error);
          this.withdrawHistory = [];
        } finally {
          this.withdrawLoading = false;
        }
      },
      getWithdrawMethodText(method) {
        return method === 'wechat' ? '微信' : '支付宝';
      },
      getWithdrawMethodTextByAccountType(accountType) {
        return String(accountType || '').toUpperCase() === 'WECHAT' ? '微信' : '支付宝';
      },
      getWithdrawStatusText(status) {
        const statusMap = {
          PENDING: '待审核',
          APPROVED: '已通过',
          REJECTED: '已拒绝',
          PAID: '已完成',
          CANCELLED: '已取消'
        };
        return statusMap[status] || status;
      },
      getRevenueStatusText(status) {
        const statusMap = {
          unsettled: '待结算',
          pending: '待结算',
          settled: '已到账',
          paid: '已到账',
          withdrawn: '已提现'
        };
        return statusMap[String(status || '').toLowerCase()] || status || '未知';
      },
      formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        if (Number.isNaN(date.getTime())) return '';
        return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')}`;
      }
    }
  };
  </script>
  
  <style scoped>
  /* ========== 全局样式 ========== */
  .wallet-page {
    min-height: 100vh;
    background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
    color: var(--it-surface-solid);
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
    color: var(--it-accent);
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
    color: var(--it-surface-solid);
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
    color: var(--it-text-subtle);
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
    background: var(--it-primary-gradient);
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
    color: var(--it-text-subtle);
    display: block;
    margin-bottom: 5px;
  }
  .balance-value {
    font-size: 32px;
    font-weight: 600;
    color: var(--it-surface-solid);
    margin-right: 10px;
  }
  .refresh-btn {
    color: var(--it-text-subtle);
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
    color: var(--it-surface-solid);
  }
  .section-header span {
    font-size: 13px;
    color: var(--it-text-subtle);
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
    border-color: var(--it-accent);
  }
  .plan-card h3 {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 5px;
    color: var(--it-surface-solid);
  }
  .plan-desc {
    font-size: 13px;
    color: var(--it-text-subtle);
    margin: 0 0 12px;
  }
  .plan-price {
    font-size: 24px;
    font-weight: 600;
    color: var(--it-accent);
    margin-bottom: 5px;
  }
  .plan-duration {
    font-size: 12px;
    color: var(--it-text-subtle);
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
    color: var(--it-success);
  }
  .empty-tip {
    text-align: center;
    padding: 30px;
    color: var(--it-text-subtle);
  }
  
  /* 支付方式 */
  .payment-method {
    margin: 20px 0;
  }
  .method-label {
    font-size: 14px;
    color: var(--it-text-subtle);
    margin-right: 15px;
  }
  .payment-method :deep(.el-radio__label) {
    color: #e0e0e0;
  }
  .payment-method :deep(.el-radio__input.is-checked .el-radio__inner) {
    background-color: var(--it-accent);
    border-color: var(--it-accent);
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
    color: var(--it-text-subtle);
    margin-top: 5px;
  }
  
  .amount-positive {
    color: var(--it-success);
    font-weight: bold;
  }
  
  .amount-negative {
    color: #f56c6c;
    font-weight: bold;
  }
  
  .empty-list {
    text-align: center;
    padding: 40px 0;
    color: var(--it-text-subtle);
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
    color: var(--it-text-subtle);
  }
  
  .no-coupon-tip {
    text-align: center;
    padding: 30px 20px;
    background: rgba(255, 255, 255, 0.02);
    border: 1px dashed rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    color: var(--it-text-subtle);
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
    color: var(--it-success);
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
    color: var(--it-text-subtle);
  }
  
  .price-item.total {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    margin-top: 8px;
    padding-top: 12px;
    font-weight: bold;
    color: var(--it-surface-solid);
  }
  
  .total-price {
    color: #f5a623;
    font-size: 20px;
  }
  
  .discount-text {
    color: var(--it-success);
  }
  </style>
<style scoped>
.wallet-page {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(45, 212, 191, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(250, 204, 21, 0.1), transparent 20%),
    linear-gradient(180deg, #07111f 0%, #0b1628 46%, #08111e 100%);
}

.wallet-page::before {
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
.balance-card,
.section-card,
.plan-card,
.no-coupon-tip,
.price-summary,
.footer {
  background: rgba(8, 15, 29, 0.74) !important;
  border-color: rgba(148, 163, 184, 0.16) !important;
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.34);
  backdrop-filter: blur(22px);
}

.page-title,
.section-header h2,
.balance-value,
.plan-card h3 {
  color: #f8fafc !important;
}

.page-subtitle,
.balance-label,
.section-header span,
.plan-desc,
.plan-duration,
.plan-benefits,
.coupon-label,
.price-item,
.balance-tip,
.empty-tip,
.empty-list,
.no-coupon-tip,
.method-label {
  color: #cbd5e1 !important;
}

.balance-icon,
.plan-price,
.total-price {
  color: #99f6e4 !important;
}

.plan-card.active {
  background: rgba(20, 184, 166, 0.16) !important;
  border-color: rgba(45, 212, 191, 0.38) !important;
  box-shadow: 0 18px 34px rgba(20, 184, 166, 0.12);
}

.discount-info {
  background: rgba(34, 197, 94, 0.14) !important;
  color: #dcfce7 !important;
}

.discount-amount,
.discount-text,
.amount-positive {
  color: #86efac !important;
}

.amount-negative {
  color: #fda4af !important;
}

.action-btn {
  border-radius: 999px !important;
}

.action-btn.el-button--warning {
  background: linear-gradient(135deg, #14b8a6, #2563eb) !important;
  box-shadow: 0 16px 30px rgba(20, 184, 166, 0.2) !important;
}

.section-card :deep(.el-table),
.section-card :deep(.el-table__expanded-cell) {
  background: transparent !important;
  color: #cbd5e1 !important;
}

.section-card :deep(.el-table::before) {
  background-color: rgba(148, 163, 184, 0.12);
}

.section-card :deep(.el-table th) {
  background: rgba(15, 23, 42, 0.86) !important;
  color: #f8fafc !important;
  border-bottom-color: rgba(148, 163, 184, 0.12);
}

.section-card :deep(.el-table td) {
  background: rgba(8, 15, 29, 0.38) !important;
  color: #cbd5e1 !important;
  border-bottom-color: rgba(148, 163, 184, 0.08);
}

.section-card :deep(.el-input__inner),
.section-card :deep(.el-select .el-input__inner),
.section-card :deep(.el-input-number),
.section-card :deep(.el-textarea__inner) {
  background: rgba(2, 6, 23, 0.58);
  border-color: rgba(148, 163, 184, 0.18);
  color: #e2e8f0;
}

.section-card :deep(.el-radio__label) {
  color: #cbd5e1;
}

.section-card :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: var(--it-success);
  border-color: var(--it-success);
}

.section-card :deep(.el-button--text) {
  color: var(--it-accent);
}
</style>

<style scoped>
.wallet-page {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
}

.wallet-page::before {
  background-image:
    linear-gradient(var(--it-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--it-grid-line) 1px, transparent 1px) !important;
}

.navbar,
.balance-card,
.section-card,
.coupon-card,
.vip-card,
.order-summary,
.empty-list,
.footer {
  background: var(--it-surface) !important;
  border: 1px solid var(--it-border) !important;
  border-radius: var(--it-radius-card) !important;
  box-shadow: var(--it-shadow) !important;
}

.navbar,
.footer {
  background: var(--it-header-bg) !important;
  border-left: 0 !important;
  border-right: 0 !important;
  border-radius: 0 !important;
}

.logo-icon,
.balance-icon,
.amount-positive,
.section-card :deep(.el-button--text) {
  color: var(--it-accent) !important;
}

.logo-text,
.page-title {
  background: var(--it-primary-gradient) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
}

.username,
.balance-value,
.section-header h2,
.coupon-title,
.summary-total,
.section-card :deep(.el-table th),
.section-card :deep(.el-table__body-wrapper td) {
  color: var(--it-text) !important;
}

.page-subtitle,
.balance-label,
.coupon-desc,
.empty-list,
.section-card :deep(.el-radio__label),
.section-card :deep(.el-table),
.section-card :deep(.el-table__empty-text) {
  color: var(--it-text-muted) !important;
}

.section-card :deep(.el-table th),
.section-card :deep(.el-table__body-wrapper td) {
  background: var(--it-surface-solid) !important;
  border-bottom-color: var(--it-border) !important;
}

.section-card :deep(.el-input__inner),
.section-card :deep(.el-input-number),
.section-card :deep(.el-textarea__inner) {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}
</style>


<style scoped>
.wallet-page,
.wallet-page .main-content {
  background: var(--it-page-bg) !important;
  color: var(--it-text) !important;
}

.page-header,
.balance-card,
.section-card,
.coupon-card,
.vip-card,
.order-summary,
.empty-list,
.footer {
  background: var(--it-panel-bg, var(--it-surface)) !important;
  border-color: var(--it-border) !important;
  box-shadow: var(--it-shadow) !important;
}

.balance-card,
.section-card,
.coupon-card,
.vip-card {
  background: var(--it-panel-bg-strong, var(--it-surface-solid)) !important;
}

.page-title,
.logo-text {
  background: var(--it-primary-gradient) !important;
  -webkit-background-clip: text !important;
  -webkit-text-fill-color: transparent !important;
}

.balance-card {
  position: relative;
  overflow: hidden;
}

.balance-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--it-soft-gradient);
  opacity: 0.7;
  pointer-events: none;
}

.balance-card > * {
  position: relative;
  z-index: 1;
}

.logo-icon,
.balance-icon,
.amount-positive,
.refresh-btn,
.section-card :deep(.el-button--text) {
  color: var(--it-accent) !important;
}

.page-subtitle,
.balance-label,
.coupon-desc,
.empty-list,
.section-card :deep(.el-radio__label),
.section-card :deep(.el-table),
.section-card :deep(.el-table__empty-text) {
  color: var(--it-text-muted) !important;
}

.section-card :deep(.el-table th),
.section-card :deep(.el-table__body-wrapper td) {
  background: var(--it-panel-bg-strong, var(--it-surface-solid)) !important;
  border-bottom-color: var(--it-border) !important;
}

.section-card :deep(.el-input__inner),
.section-card :deep(.el-input-number),
.section-card :deep(.el-textarea__inner) {
  background: var(--it-surface-muted) !important;
  border-color: var(--it-border) !important;
  color: var(--it-text) !important;
}
</style>

<style scoped>
.action-btn.el-button--warning,
.action-btn.el-button--primary {
  background: var(--it-primary-gradient) !important;
  border: none !important;
  color: var(--it-text-light) !important;
  box-shadow: var(--it-button-shadow) !important;
}

.action-btn.el-button--warning:hover,
.action-btn.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--it-shadow-strong) !important;
}

.action-btn.is-disabled,
.action-btn.is-disabled:hover {
  background: color-mix(in srgb, var(--it-surface-muted) 92%, transparent) !important;
  color: var(--it-text-subtle) !important;
  box-shadow: none !important;
}
</style>


