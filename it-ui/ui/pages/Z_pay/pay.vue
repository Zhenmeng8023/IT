<template>
    <div class="recharge-container">
      <!-- 顶部导航栏（复用现有布局） -->
      <nav class="navbar" :class="{ 'navbar-scrolled': scrolled }">
        <div class="navbar-content">
          <div class="logo" @click="scrollToTop">
            <span class="logo-icon">●</span>
            <span class="logo-text">ITSpace</span>
          </div>
          <div class="nav-actions">
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
                <el-dropdown-item command="recharge">账户充值</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </nav>
  
      <div class="main-content">
        <!-- 左侧：用户信息卡片 -->
        <div class="left-card">
          <el-card class="user-card" shadow="hover">
            <div class="user-header">
              <el-avatar :size="80" :src="avatarUrl"></el-avatar>
              <div class="user-info-text">
                <h3>{{ nickname || username }}</h3>
                <p class="user-id">ID: {{ userId }}</p>
              </div>
            </div>
            <div class="balance-info">
              <div class="balance-item">
                <span class="label">账户余额</span>
                <span class="value">¥{{ balance }}</span>
              </div>
              <div class="balance-item">
                <span class="label">会员状态</span>
                <span class="value" :class="{ 'vip': isVip }">
                  {{ isVip ? 'VIP会员' : '普通用户' }}
                </span>
                <el-tag v-if="!isVip" type="danger" size="small" class="upgrade-tip">开通VIP享更多权益</el-tag>
              </div>
            </div>
            <el-divider />
            <div class="vip-benefits">
              <p><i class="el-icon-check"></i> 免费阅读所有VIP博客</p>
              <p><i class="el-icon-check"></i> 独家技术课程</p>
              <p><i class="el-icon-check"></i> 会员专属标识</p>
              <el-button type="warning" plain size="small" @click="openVipDialog">开通VIP</el-button>
            </div>
          </el-card>
        </div>
  
        <!-- 右侧：充值面板 -->
        <div class="right-panel">
          <el-card class="recharge-card" shadow="hover">
            <div slot="header" class="card-header">
              <span>账户充值</span>
              <el-link type="primary" @click="goToRecords">充值记录</el-link>
            </div>
  
            <!-- 选择金额 -->
            <div class="amount-section">
              <div class="section-title">选择充值金额</div>
              <div class="amount-options">
                <el-button
                  v-for="amount in presetAmounts"
                  :key="amount.value"
                  :type="selectedAmount === amount.value ? 'primary' : 'default'"
                  @click="selectPresetAmount(amount.value)"
                  class="amount-btn"
                >
                  ¥{{ amount.value }}
                  <span class="extra" v-if="amount.gift">+{{ amount.gift }}</span>
                </el-button>
                <el-input
                  v-model="customAmount"
                  placeholder="自定义金额"
                  class="custom-input"
                  @input="onCustomInput"
                  :min="1"
                  type="number"
                >
                  <template slot="prepend">¥</template>
                </el-input>
              </div>
            </div>
  
            <!-- 支付方式 -->
            <div class="pay-method-section">
              <div class="section-title">选择支付方式</div>
              <el-radio-group v-model="payMethod" class="pay-methods">
                <el-radio label="wechat">
                  <i class="el-icon-wechat"></i> 微信支付
                </el-radio>
                <el-radio label="alipay">
                  <i class="el-icon-alipay"></i> 支付宝
                </el-radio>
              </el-radio-group>
            </div>
  
            <!-- 充值按钮 -->
            <div class="action-buttons">
              <el-button type="primary" size="large" @click="submitRecharge" :loading="recharging">
                立即充值 ¥{{ finalAmount }}
              </el-button>
            </div>
  
            <div class="notice">
              <i class="el-icon-info"></i> 充值成功后，金额将自动计入您的账户余额。如有问题请联系客服。
            </div>
          </el-card>
        </div>
      </div>
  
      <!-- VIP开通弹窗 -->
      <el-dialog title="开通VIP会员" :visible.sync="vipDialogVisible" width="400px">
        <el-form :model="vipForm" label-width="100px">
          <el-form-item label="会员套餐">
            <el-radio-group v-model="vipForm.plan">
              <el-radio label="month">月度会员 ¥30/月</el-radio>
              <el-radio label="quarter">季度会员 ¥80/季</el-radio>
              <el-radio label="year">年度会员 ¥300/年</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="支付方式">
            <el-radio-group v-model="vipForm.payMethod">
              <el-radio label="wechat">微信支付</el-radio>
              <el-radio label="alipay">支付宝</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="vipDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitVip" :loading="vipSubmitting">立即开通</el-button>
        </span>
      </el-dialog>
  
      <!-- 充值记录弹窗（简化版，实际可跳转独立页面） -->
      <el-dialog title="充值记录" :visible.sync="recordsDialogVisible" width="600px">
        <el-table :data="rechargeRecords" style="width: 100%">
          <el-table-column prop="time" label="时间" width="180"></el-table-column>
          <el-table-column prop="amount" label="金额" width="100">
            <template slot-scope="scope">¥{{ scope.row.amount }}</template>
          </el-table-column>
          <el-table-column prop="method" label="支付方式" width="120"></el-table-column>
          <el-table-column prop="status" label="状态">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'" size="small">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-dialog>
    </div>
  </template>
  
  <script>
  import { getToken } from '@/utils/auth'
  import { GetCurrentUser } from '@/api/index'
  import axios from 'axios'
  
  export default {
    layout: 'default',
    data() {
      return {
        scrolled: false,
        avatarUrl: '/pic/choubi.jpg',
        username: '',
        nickname: '',
        userId: null,
        balance: 0,
        isVip: false,
        vipExpireDate: null,
        // 充值相关
        presetAmounts: [
          { value: 10, gift: '无' },
          { value: 50, gift: '送5元' },
          { value: 100, gift: '送15元' },
          { value: 200, gift: '送40元' },
          { value: 500, gift: '送120元' }
        ],
        selectedAmount: null,
        customAmount: '',
        payMethod: 'wechat',
        recharging: false,
        // VIP相关
        vipDialogVisible: false,
        vipForm: {
          plan: 'month',
          payMethod: 'wechat'
        },
        vipSubmitting: false,
        // 充值记录
        recordsDialogVisible: false,
        rechargeRecords: []
      }
    },
    computed: {
      finalAmount() {
        if (this.selectedAmount !== null) return this.selectedAmount
        if (this.customAmount && !isNaN(this.customAmount) && this.customAmount > 0) {
          return parseFloat(this.customAmount)
        }
        return 0
      }
    },
    mounted() {
      window.addEventListener('scroll', this.handleScroll)
      this.getUserInfo()
      this.getUserBalanceAndVipStatus()
      this.loadRechargeRecords()
    },
    beforeDestroy() {
      window.removeEventListener('scroll', this.handleScroll)
    },
    methods: {
      handleScroll() {
        this.scrolled = window.scrollY > 50
      },
      scrollToTop() {
        window.scrollTo({ top: 0, behavior: 'smooth' })
      },
      async getUserInfo() {
        try {
          const res = await GetCurrentUser()
          const user = res.data || res
          this.userId = user.id
          this.username = user.username
          this.nickname = user.nickname || user.username
          this.avatarUrl = user.avatarUrl || this.avatarUrl
        } catch (error) {
          console.error('获取用户信息失败', error)
          this.$message.error('获取用户信息失败')
        }
      },
      async getUserBalanceAndVipStatus() {
        // 模拟获取余额和VIP状态（实际调用后端接口）
        // 实际接口例如：GET /api/user/balance 返回 { balance, isVip, vipExpireDate }
        try {
          // 这里假设已有接口，我们模拟数据
          const res = await axios.get('/api/user/balance')
          this.balance = res.data.balance || 0
          this.isVip = res.data.isVip || false
          this.vipExpireDate = res.data.vipExpireDate
        } catch (error) {
          console.error('获取余额失败', error)
          // 模拟数据，实际可置0
          this.balance = 0
          this.isVip = false
        }
      },
      async loadRechargeRecords() {
        // 获取充值记录，用于弹窗显示
        try {
          const res = await axios.get('/api/recharge/records')
          this.rechargeRecords = res.data || []
        } catch (error) {
          console.error('获取充值记录失败', error)
        }
      },
      selectPresetAmount(value) {
        this.selectedAmount = value
        this.customAmount = '' // 清除自定义金额
      },
      onCustomInput() {
        // 自定义金额时，清除预设选中
        if (this.customAmount && !isNaN(this.customAmount) && this.customAmount > 0) {
          this.selectedAmount = null
        }
      },
      async submitRecharge() {
        if (this.finalAmount <= 0) {
          this.$message.warning('请选择或输入充值金额')
          return
        }
        this.recharging = true
        try {
          // 1. 创建充值订单
          const orderRes = await axios.post('/api/recharge/order', {
            amount: this.finalAmount,
            payMethod: this.payMethod
          })
          const orderId = orderRes.data.orderId

          // 2. 调用支付接口（实际需要唤起支付）
          // 这里简化：模拟支付成功，并调用后端确认
          // 真实场景需要根据支付方式拉起微信/支付宝支付，并接收回调
          const payRes = await axios.post('/api/recharge/pay', {
            orderId,
            payMethod: this.payMethod
          })
          if (payRes.data.success) {
            this.$message.success('充值成功')
            // 刷新余额
            this.getUserBalanceAndVipStatus()
            // 刷新充值记录
            this.loadRechargeRecords()
          } else {
            this.$message.error(payRes.data.message || '充值失败')
          }
        } catch (error) {
          console.error('充值失败', error)
          this.$message.error('充值失败，请稍后重试')
        } finally {
          this.recharging = false
        }
      },
      openVipDialog() {
        this.vipDialogVisible = true
      },
      async submitVip() {
        this.vipSubmitting = true
        try {
          const res = await axios.post('/api/vip/buy', {
            plan: this.vipForm.plan,
            payMethod: this.vipForm.payMethod
          })
          if (res.data.success) {
            this.$message.success('开通成功，您已成为VIP会员')
            this.isVip = true
            this.vipDialogVisible = false
            this.getUserBalanceAndVipStatus() // 刷新余额
          } else {
            this.$message.error(res.data.message || '开通失败')
          }
        } catch (error) {
          console.error('开通VIP失败', error)
          this.$message.error('开通失败，请重试')
        } finally {
          this.vipSubmitting = false
        }
      },
      goToRecords() {
        this.recordsDialogVisible = true
      },
      handleUserCommand(command) {
        switch (command) {
          case 'profile': this.$router.push('/user'); break
          case 'blog': this.$router.push('/blog'); break
          case 'circle': this.$router.push('/circle'); break
          case 'recharge': this.$router.push('/recharge'); break
          case 'logout': this.logout(); break
        }
      },
      logout() {
        localStorage.removeItem('token')
        this.$router.push('/login')
      }
    }
  }
  </script>
  
  <style scoped>
  .recharge-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
    color: #ffffff;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
  }
  
  /* 导航栏样式（与原有保持一致） */
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
  
  /* 主内容区域 */
  .main-content {
    max-width: 1200px;
    margin: 40px auto;
    padding: 0 20px;
    display: flex;
    gap: 30px;
    flex-wrap: wrap;
  }
  .left-card {
    flex: 1;
    min-width: 280px;
  }
  .right-panel {
    flex: 2;
    min-width: 400px;
  }
  
  /* 用户卡片 */
  .user-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 20px;
    padding: 20px;
  }
  .user-header {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 20px;
  }
  .user-info-text h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #ffffff;
  }
  .user-id {
    font-size: 12px;
    color: #a0a0a0;
    margin: 5px 0 0;
  }
  .balance-info {
    margin-top: 15px;
  }
  .balance-item {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 12px;
    padding: 8px 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
  .balance-item .label {
    font-size: 14px;
    color: #a0a0a0;
  }
  .balance-item .value {
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
  }
  .balance-item .value.vip {
    color: #f5a623;
  }
  .upgrade-tip {
    margin-left: 10px;
  }
  .vip-benefits {
    margin-top: 15px;
  }
  .vip-benefits p {
    font-size: 13px;
    color: #a0a0a0;
    margin: 8px 0;
  }
  .vip-benefits i {
    color: #67c23a;
    margin-right: 6px;
  }
  
  /* 充值卡片 */
  .recharge-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 20px;
  }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 18px;
    font-weight: 500;
    color: #ffffff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    padding-bottom: 12px;
  }
  .section-title {
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 15px;
    color: #e0e0e0;
  }
  .amount-options {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
    align-items: center;
    margin-bottom: 20px;
  }
  .amount-btn {
    border-radius: 30px;
    padding: 10px 20px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #ffffff;
    transition: all 0.3s ease;
  }
  .amount-btn:hover {
    background: rgba(64, 158, 255, 0.2);
    border-color: #409EFF;
  }
  .amount-btn.is-active {
    background: #409EFF;
    border-color: #409EFF;
    color: white;
  }
  .extra {
    font-size: 12px;
    color: #f5a623;
    margin-left: 4px;
  }
  .custom-input {
    width: 140px;
  }
  .custom-input :deep(.el-input__inner) {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #ffffff;
  }
  .pay-methods {
    display: flex;
    gap: 20px;
    margin-bottom: 30px;
  }
  .pay-methods .el-radio {
    color: #e0e0e0;
  }
  .pay-methods i {
    font-size: 18px;
    margin-right: 5px;
  }
  .action-buttons {
    text-align: center;
    margin: 20px 0;
  }
  .action-buttons .el-button {
    width: 80%;
    background: linear-gradient(135deg, #409EFF, #66b1ff);
    border: none;
    border-radius: 40px;
    padding: 12px;
    font-size: 16px;
    font-weight: 500;
  }
  .notice {
    font-size: 12px;
    color: #909399;
    text-align: center;
    margin-top: 20px;
  }
  
  /* 响应式 */
  @media screen and (max-width: 768px) {
    .main-content {
      flex-direction: column;
    }
    .right-panel {
      min-width: auto;
    }
    .amount-options {
      justify-content: center;
    }
  }
  </style>