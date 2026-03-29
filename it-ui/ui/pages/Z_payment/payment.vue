<template>
  <div class="payment-test-page" v-if="paymentType === 'wechat-test'">
    <div class="payment-container">
      <h1 class="page-title">微信支付测试</h1>

      <div class="payment-info">
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付金额：</span>
          <span class="value">¥{{ amount }}</span>
        </div>
      </div>

      <div class="qrcode-container">
        <div class="qrcode-box">
          <div class="qrcode-placeholder">
            <div class="qrcode-icon">📱</div>
            <p>测试环境 - 模拟支付</p>
            <p class="tip">请使用微信扫描二维码（测试）</p>
          </div>
        </div>
        <div class="qrcode-tip">请使用微信扫描二维码（测试）</div>
      </div>

      <div class="action-buttons">
        <el-button type="success" @click="simulatePayment">
          模拟支付成功
        </el-button>
        <el-button type="danger" @click="goBack">
          返回
        </el-button>
      </div>

      <div class="test-tips">
        <h3>测试说明：</h3>
        <ul>
          <li>当前为测试环境，不会真实扣款</li>
          <li>点击“模拟支付成功”按钮即可完成支付</li>
          <li>支付完成后将自动跳转回钱包页面</li>
        </ul>
      </div>
    </div>
  </div>

  <!-- 非测试模式下显示的内容 -->
  <div v-else class="payment-test-page">
    <div class="payment-container">
      <h1 class="page-title">支付处理中</h1>
      <p>正在跳转至支付平台...</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  layout: 'default',
  data() {
    return {
      orderNo: '',
      amount: '',
      paymentType: '',
      countDown: 0
    }
  },
  mounted() {
    // 从 URL 参数获取订单号、金额和支付类型
    this.orderNo = this.$route.query.orderNo || ''
    this.amount = this.$route.query.amount || ''
    this.paymentType = this.$route.query.type || ''

    // 如果是测试支付，显示测试页面
    if (this.paymentType === 'wechat-test') {
      // 模拟倒计时
      this.countDown = 10
      const timer = setInterval(() => {
        this.countDown--
        if (this.countDown <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  },
  methods: {
    // 模拟支付成功
    async simulatePayment() {
      try {
        // 调用后端接口更新订单状态为已支付
        await axios.post(`/api/orders/pay-test`, null, {
          params: { orderNo: this.orderNo }
        })

        this.$message.success('支付成功！')

        // 延迟跳转，让用户看到成功提示
        setTimeout(() => {
          window.location.href = '/wallet'
        }, 1500)
      } catch (error) {
        this.$message.error('支付失败：' + (error.response?.data?.message || '未知错误'))
      }
    },

    // 返回上一页
    goBack() {
      window.location.href = '/wallet'
    }
  }
}
</script>

<style scoped>
.payment-test-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.payment-container {
  background: white;
  border-radius: 20px;
  padding: 40px;
  max-width: 500px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.page-title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 28px;
}

.payment-info {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 16px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {
  color: #666;
  font-weight: 500;
}

.value {
  color: #333;
  font-weight: 600;
}

.qrcode-container {
  text-align: center;
  margin: 30px 0;
}

.qrcode-box {
  background: white;
  border-radius: 15px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.qrcode-tip {
  margin-top: 15px;
  color: #666;
  font-size: 14px;
}

.qrcode-placeholder {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 15px;
  padding: 40px 20px;
  color: white;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.qrcode-placeholder p {
  margin: 10px 0;
  font-size: 18px;
}

.tip {
  font-size: 14px;
  opacity: 0.9;
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin: 30px 0;
}

.action-buttons .el-button {
  flex: 1;
  height: 50px;
  font-size: 16px;
}

.test-tips {
  background: #fff3cd;
  border-left: 4px solid #ffc107;
  padding: 15px 20px;
  border-radius: 5px;
  margin-top: 20px;
}

.test-tips h3 {
  margin: 0 0 10px;
  color: #856404;
  font-size: 16px;
}

.test-tips ul {
  margin: 0;
  padding-left: 20px;
}

.test-tips li {
  color: #856404;
  margin-bottom: 5px;
  font-size: 14px;
}
</style>
