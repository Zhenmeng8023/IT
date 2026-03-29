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

  <!-- 正式微信支付模式 -->
  <div v-else-if="paymentType === 'wechat'" class="payment-test-page">
    <div class="payment-container">
      <h1 class="page-title">微信支付</h1>

      <div class="payment-info" v-if="orderNo && amount">
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付金额：</span>
          <span class="value amount-highlight">￥{{ amount }}</span>
        </div>
      </div>

      <div class="qrcode-container" v-loading="loading">
        <div class="qrcode-box" v-if="qrCodeUrl">
          <img :src="qrCodeUrl" alt="支付二维码" class="qrcode-image" />
        </div>
        <div v-else-if="!codeUrl" class="error-tip">
          <el-alert
            title="参数错误"
            type="error"
            description="未获取到支付码，请返回重试"
            show-icon
            :closable="false"
          />
        </div>
        <div class="qrcode-tip" v-if="codeUrl">请使用微信扫描二维码</div>
      </div>

      <div class="payment-amount-tip">
        <el-alert
          title="重要提示"
          type="warning"
          :closable="false"
          show-icon>
          <div class="tip-content">
            <p><strong>扫码后请手动输入支付金额：</strong></p>
            <div class="amount-input-hint">
              <span class="amount-value">￥{{ amount }}</span>
            </div>
            <p class="hint-text">⚠️ 微信个人收款码不支持自动设置金额，需要手动输入</p>
          </div>
        </el-alert>
      </div>

      <div class="test-action-tip">
        <el-alert
          title="测试环境提示"
          type="info"
          description="扫码支付后，请点击下方“我已扫码支付”按钮来模拟支付成功"
          show-icon
          :closable="false"
        />
      </div>

      <div class="action-buttons">
        <el-button type="primary" @click="simulatePayment">
          我已扫码支付
        </el-button>
        <el-button type="danger" @click="goBack">
          返回
        </el-button>
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
      codeUrl: '',
      qrCodeUrl: '',
      loading: false,
      countDown: 0,
      checkInterval: null,  // 定时器
      isChecking: false     // 是否正在检查支付状态
    }
  },
  mounted() {
    // 从 URL 参数获取订单号、金额和支付类型
    this.orderNo = this.$route.query.orderNo || ''
    this.amount = this.$route.query.amount || ''
    this.paymentType = this.$route.query.type || ''
    this.codeUrl = this.$route.query.codeUrl || ''

    console.log('支付页面收到的参数:', {
      orderNo: this.orderNo,
      amount: this.amount,
      paymentType: this.paymentType,
      codeUrl: this.codeUrl
    })

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
    } else if (this.paymentType === 'wechat') {
      // 正式微信支付模式
      if (this.codeUrl) {
        // 判断 codeUrl 是否是图片 URL
        if (this.codeUrl.endsWith('.png') || this.codeUrl.endsWith('.jpg') || this.codeUrl.endsWith('.jpeg') || this.codeUrl.endsWith('.gif')) {
          // 是图片 URL，直接使用
          this.qrCodeUrl = this.codeUrl
          console.log('使用图片 URL:', this.qrCodeUrl)
        } else {
          // 不是图片，生成二维码
          this.generateQRCode()
        }
      } else {
        // 没有 codeUrl，显示错误提示
        this.$message.error('未获取到支付码，请返回重试')
      }
    }
  },
  methods: {
    // 开始轮询检测支付状态
    startCheckPaymentStatus() {
      if (this.isChecking) return
      
      this.isChecking = true
      console.log('开始检测支付状态，订单号:', this.orderNo)
      
      // 每 2 秒检查一次
      this.checkInterval = setInterval(async () => {
        try {
          const response = await axios.get(`/api/orders/order-no/${this.orderNo}`)
          const order = response.data
          
          console.log('订单状态检查:', order.status)
          
          if (order.status === 'PAID') {
            // 支付成功
            clearInterval(this.checkInterval)
            this.isChecking = false
            
            this.$message.success('支付成功！')
            
            // 延迟跳转，让用户看到成功提示
            setTimeout(() => {
              window.location.href = '/wallet'
            }, 1500)
          }
        } catch (error) {
          console.error('检查支付状态失败:', error)
        }
      }, 2000)
    },
    
    // 停止检测支付状态
    stopCheckPaymentStatus() {
      if (this.checkInterval) {
        clearInterval(this.checkInterval)
        this.checkInterval = null
        this.isChecking = false
      }
    },
    
    // 生成二维码
    generateQRCode() {
      this.loading = true
      try {
        // 使用第三方二维码生成服务或库
        // 这里使用一个在线二维码生成 API 作为示例
        // 生产环境建议使用本地二维码生成库如 qrcode.js
        this.qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(this.codeUrl)}`
        this.loading = false
      } catch (error) {
        console.error('生成二维码失败:', error)
        this.loading = false
        this.$message.error('生成二维码失败，请刷新页面重试')
      }
    },
    async simulatePayment() {
      try {
        // 先停止自动检测
        this.stopCheckPaymentStatus()
        
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
  },
  beforeDestroy() {
    // 页面销毁时停止检测
    this.stopCheckPaymentStatus()
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

.qrcode-image {
  width: 200px;
  height: 200px;
  object-fit: contain;
}

.qrcode-tip {
  margin-top: 15px;
  color: #666;
  font-size: 14px;
}

.test-action-tip {
  margin: 20px 0;
}

.error-tip {
  padding: 20px;
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
