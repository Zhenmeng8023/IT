<template>
  <div class="user-coupon-page">
    <div class="coupon-body">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-kicker">Smart Savings Hub</span>
          <h1 class="page-title">我的优惠券</h1>
          <p class="page-subtitle">统一查看可用优惠、领取入口与使用记录，让每一次支付都更省、更清楚。</p>
        </div>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-label">可用优惠券</span>
            <strong class="hero-stat-value">{{ availableCoupons.length }}</strong>
          </div>
          <div class="hero-stat">
            <span class="hero-stat-label">累计优惠</span>
            <strong class="hero-stat-value">¥{{ totalDiscount.toFixed(2) }}</strong>
          </div>
          <div class="hero-stat">
            <span class="hero-stat-label">当前分组</span>
            <strong class="hero-stat-value">{{ activeTab === 'my' ? '我的优惠券' : activeTab === 'available' ? '可领取' : '使用记录' }}</strong>
          </div>
        </div>
      </section>

      <section class="coupon-shell">
        <el-tabs v-model="activeTab" class="coupon-tabs" @tab-click="handleTabClick">
        <el-tab-pane label="我的优惠券" name="my">
          <div class="tab-content">
            <div class="stats-cards">
              <div class="stat-card available">
                <div class="stat-icon">
                  <i class="el-icon-ticket"></i>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ availableCoupons.length }}</div>
                  <div class="stat-label">可用优惠券</div>
                </div>
              </div>
              <div class="stat-card used">
                <div class="stat-icon">
                  <i class="el-icon-check"></i>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ usedCoupons.length }}</div>
                  <div class="stat-label">已使用</div>
                </div>
              </div>
              <div class="stat-card expired">
                <div class="stat-icon">
                  <i class="el-icon-time"></i>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ expiredCoupons.length }}</div>
                  <div class="stat-label">已过期</div>
                </div>
              </div>
              <div class="stat-card total">
                <div class="stat-icon">
                  <i class="el-icon-money"></i>
                </div>
                <div class="stat-info">
                  <div class="stat-value">¥{{ totalDiscount.toFixed(2) }}</div>
                  <div class="stat-label">累计优惠</div>
                </div>
              </div>
            </div>

            <div class="action-bar">
              <el-button type="primary" icon="el-icon-plus" @click="showRedeemDialog = true">
                兑换优惠券
              </el-button>
              <el-button icon="el-icon-refresh" @click="loadUserCoupons">
                刷新
              </el-button>
            </div>

            <div class="coupon-list" v-loading="loading">
              <div v-if="userCoupons.length === 0" class="empty-state">
                <i class="el-icon-ticket"></i>
                <p>暂无优惠券</p>
                <el-button type="primary" size="small" @click="showRedeemDialog = true">
                  去领取
                </el-button>
              </div>

              <div 
                v-for="coupon in userCoupons" 
                :key="coupon.id" 
                class="coupon-card"
                :class="{ 
                  'coupon-available': isCouponAvailable(coupon),
                  'coupon-used': coupon.receiveStatus === 'used',
                  'coupon-expired': coupon.receiveStatus === 'expired'
                }"
              >
                <div class="coupon-left">
                  <div class="coupon-amount">
                    <span class="currency">¥</span>
                    <span class="value">{{ getCouponValue(coupon) }}</span>
                  </div>
                  <div class="coupon-condition" v-if="getCouponMinAmount(coupon)">
                    满{{ getCouponMinAmount(coupon) }}可用
                  </div>
                </div>
                <div class="coupon-right">
                  <div class="coupon-header">
                    <h3 class="coupon-name">{{ coupon.couponName }}</h3>
                    <el-tag 
                      size="mini" 
                      :type="getStatusType(coupon.receiveStatus)"
                    >
                      {{ getStatusText(coupon.receiveStatus) }}
                    </el-tag>
                  </div>
                  <div class="coupon-info">
                    <p><i class="el-icon-time"></i> 有效期至：{{ formatDate(coupon.endTime) }}</p>
                    <p v-if="coupon.remark"><i class="el-icon-document"></i> {{ coupon.remark }}</p>
                  </div>
                  <div class="coupon-actions">
                    <el-button 
                      v-if="isCouponAvailable(coupon)"
                      type="primary" 
                      size="small"
                      @click="useCoupon(coupon)"
                    >
                      立即使用
                    </el-button>
                    <el-button 
                      v-else-if="coupon.receiveStatus === 'used'"
                      size="small"
                      disabled
                    >
                      已使用
                    </el-button>
                    <el-button 
                      v-else-if="coupon.receiveStatus === 'expired'"
                      size="small"
                      disabled
                    >
                      已过期
                    </el-button>
                  </div>
                </div>
                <div class="coupon-type-badge" :class="getCouponTypeClass(coupon)">
                  {{ getCouponTypeText(coupon) }}
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="可领取" name="available">
          <div class="tab-content">
            <div class="coupon-list" v-loading="availableLoading">
              <div v-if="availableList.length === 0" class="empty-state">
                <i class="el-icon-goods"></i>
                <p>暂无可领取的优惠券</p>
              </div>

              <div 
                v-for="coupon in availableList" 
                :key="coupon.id" 
                class="coupon-card available-card"
              >
                <div class="coupon-left">
                  <div class="coupon-amount">
                    <span class="currency">¥</span>
                    <span class="value">{{ getCouponTemplateValue(coupon) }}</span>
                  </div>
                  <div class="coupon-condition" v-if="coupon.minAmount">
                    满{{ coupon.minAmount }}可用
                  </div>
                </div>
                <div class="coupon-right">
                  <div class="coupon-header">
                    <h3 class="coupon-name">{{ coupon.name }}</h3>
                    <el-tag size="mini" type="success">可领取</el-tag>
                  </div>
                  <div class="coupon-info">
                    <p><i class="el-icon-time"></i> 有效期：{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</p>
                    <p><i class="el-icon-user"></i> 已领取 {{ coupon.issuedCount || 0 }}/{{ coupon.totalLimit || '∞' }}</p>
                  </div>
                  <div class="coupon-actions">
                    <el-button 
                      type="primary" 
                      size="small"
                      @click="redeemAvailableCoupon(coupon.code)"
                    >
                      立即领取
                    </el-button>
                  </div>
                </div>
                <div class="coupon-type-badge" :class="coupon.type === 'discount' ? 'DISCOUNT' : 'AMOUNT_OFF'">
                  {{ coupon.type === 'discount' ? '折扣券' : '代金券' }}
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="使用记录" name="records">
          <div class="tab-content">
            <div class="redemption-list" v-loading="recordsLoading">
              <div v-if="redemptions.length === 0" class="empty-state">
                <i class="el-icon-document"></i>
                <p>暂无使用记录</p>
              </div>

              <el-table 
                v-else
                :data="redemptions" 
                style="width: 100%"
              >
                <el-table-column prop="couponName" label="优惠券" width="200"></el-table-column>
                <el-table-column label="订单金额" width="120">
                  <template slot-scope="scope">
                    ¥{{ scope.row.originalAmount }}
                  </template>
                </el-table-column>
                <el-table-column label="优惠金额" width="120">
                  <template slot-scope="scope">
                    <span style="color: #f56c6c;">-¥{{ scope.row.discountAmount }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="实付金额" width="120">
                  <template slot-scope="scope">
                    ¥{{ scope.row.finalAmount }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template slot-scope="scope">
                    <el-tag :type="getRedemptionStatusType(scope.row.status)" size="small">
                      {{ getRedemptionStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="redeemedAt" label="使用时间" width="180">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.redeemedAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作">
                  <template slot-scope="scope">
                    <el-button 
                      v-if="scope.row.status === 'SUCCESS'"
                      type="text" 
                      size="small"
                      @click="viewOrderDetail(scope.row.orderId)"
                    >
                      查看订单
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
        </el-tabs>
      </section>
    </div>

    <el-dialog
      title="兑换优惠券"
      :visible.sync="showRedeemDialog"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form ref="redeemForm" :model="redeemForm" :rules="redeemRules" label-width="80px">
        <el-form-item label="兑换码" prop="couponCode">
          <el-input 
            v-model="redeemForm.couponCode" 
            placeholder="请输入优惠券兑换码"
            prefix-icon="el-icon-ticket"
            clearable
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="showRedeemDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRedeem" :loading="redeeming">
          确认兑换
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getUserCoupons, 
  getUserAvailableCoupons, 
  getAvailableCoupons,
  redeemCoupon,
  getUserRedemptions,
  getUserTotalDiscount
} from '@/api/coupon'
import { GetCurrentUser } from '@/api/index'

export default {
  name: 'UserCoupons',
  data() {
    return {
      activeTab: 'my',
      loading: false,
      availableLoading: false,
      recordsLoading: false,
      redeeming: false,
      
      // 用户信息
      userId: null,
      
      // 用户优惠券
      userCoupons: [],
      
      // 可领取的优惠券
      availableList: [],
      
      // 核销记录
      redemptions: [],
      
      // 统计数据
      totalDiscount: 0,
      
      // 兑换对话框
      showRedeemDialog: false,
      redeemForm: {
        couponCode: ''
      },
      redeemRules: {
        couponCode: [
          { required: true, message: '请输入兑换码', trigger: 'blur' }
        ]
      }
    }
  },
  
  computed: {
    // 可用优惠券（包含 received 和 locked 状态）
    availableCoupons() {
      return this.userCoupons.filter(c => c.receiveStatus === 'received' || c.receiveStatus === 'locked')
    },
    
    // 已使用优惠券
    usedCoupons() {
      return this.userCoupons.filter(c => c.receiveStatus === 'used')
    },
    
    // 已过期优惠券
    expiredCoupons() {
      return this.userCoupons.filter(c => c.receiveStatus === 'expired')
    }
  },
  
  mounted() {
    this.initPage()
  },
  
  methods: {
    // 初始化页面
    async initPage() {
      // 先获取当前用户信息
      try {
        const res = await GetCurrentUser()
        console.log('GetCurrentUser 响应:', res)
        console.log('res.data:', res.data)
        
        // 处理不同的响应格式
        let userData = null
        if (res.data && res.data.id) {
          // 格式 1: { data: { id: xxx, ... } }
          userData = res.data
        } else if (res.data && res.data.data && res.data.data.id) {
          // 格式 2: { data: { data: { id: xxx, ... } } }
          userData = res.data.data
        } else if (res && res.id) {
          // 格式 3: { id: xxx, ... }
          userData = res
        }
        
        console.log('解析后的 userData:', userData)
        this.userId = userData?.id || null
        
        if (!this.userId) {
          console.error('无法获取用户ID, res:', res)
          this.$message.warning('请先登录')
          this.$router.push('/login')
          return
        }
        
        console.log('当前用户ID:', this.userId)
        // 获取用户信息成功后加载优惠券
        this.loadUserCoupons()
      } catch (error) {
        console.error('获取用户信息失败:', error)
        console.error('错误详情:', error.response || error.message)
        if (error.response && (error.response.status === 401 || error.response.status === 404)) {
          this.$message.warning('请先登录')
          this.$router.push('/login')
        } else {
          this.$message.error('获取用户信息失败: ' + (error.message || '未知错误'))
          this.$router.push('/login')
        }
      }
    },
    
    handleTabClick(tab) {
      if (tab.name === 'available') {
        this.loadAvailableCoupons()
      } else if (tab.name === 'records') {
        this.loadRedemptionRecords()
      }
    },
    
    // 加载用户优惠券
    async loadUserCoupons() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }
      
      this.loading = true
      try {
        const res = await getUserCoupons(this.userId)
        if (res.data.success) {
          this.userCoupons = res.data.data || []
        }
        
        // 加载总优惠金额
        const discountRes = await getUserTotalDiscount(this.userId)
        if (discountRes.data.success) {
          this.totalDiscount = discountRes.data.totalDiscount || 0
        }
      } catch (error) {
        console.error('加载优惠券失败:', error)
        this.$message.error('加载优惠券失败')
      } finally {
        this.loading = false
      }
    },
    
    // 加载可领取的优惠券
    async loadAvailableCoupons() {
      this.availableLoading = true
      try {
        const res = await getAvailableCoupons()
        if (res.data.success) {
          this.availableList = res.data.data || []
        }
      } catch (error) {
        console.error('加载可领取优惠券失败:', error)
        this.$message.error('加载失败')
      } finally {
        this.availableLoading = false
      }
    },
    
    // 加载核销记录
    async loadRedemptionRecords() {
      if (!this.userId) return
      
      this.recordsLoading = true
      try {
        const res = await getUserRedemptions(this.userId)
        if (res.data.success) {
          this.redemptions = res.data.data || []
        }
      } catch (error) {
        console.error('加载核销记录失败:', error)
        this.$message.error('加载失败')
      } finally {
        this.recordsLoading = false
      }
    },
    
    // 兑换优惠券
    handleRedeem() {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }
      
      this.$refs.redeemForm.validate(async (valid) => {
        if (!valid) return
        
        this.redeeming = true
        try {
          const res = await redeemCoupon({
            couponCode: this.redeemForm.couponCode,
            userId: this.userId
          })
          
          if (res.data.success) {
            this.$message.success('兑换成功')
            this.showRedeemDialog = false
            this.redeemForm.couponCode = ''
            this.loadUserCoupons()
          } else {
            this.$message.error(res.data.message || '兑换失败')
          }
        } catch (error) {
          console.error('兑换失败:', error)
          this.$message.error(error.response?.data?.message || '兑换失败')
        } finally {
          this.redeeming = false
        }
      })
    },
    
    // 领取可领取的优惠券
    async redeemAvailableCoupon(code) {
      if (!this.userId) {
        this.$message.warning('请先登录')
        return
      }
      
      try {
        const res = await redeemCoupon({
          couponCode: code,
          userId: this.userId
        })
        
        if (res.data.success) {
          this.$message.success('领取成功')
          this.loadUserCoupons()
          this.loadAvailableCoupons()
        } else {
          this.$message.error(res.data.message || '领取失败')
        }
      } catch (error) {
        console.error('领取失败:', error)
        this.$message.error(error.response?.data?.message || '领取失败')
      }
    },
    
    // 使用优惠券
    useCoupon(coupon) {
      this.$message.info('跳转到支付页面使用优惠券')
      // TODO: 跳转到支付页面，传递优惠券ID
      // this.$router.push({
      //   path: '/Z_payment/payment',
      //   query: { couponId: coupon.id }
      // })
    },
    
    // 查看订单详情
    viewOrderDetail(orderId) {
      this.$router.push({
        path: '/Z_userpage/orders_purchases',
        query: { orderId }
      })
    },
    
    // 获取优惠券面值
    getCouponValue(coupon) {
      if (coupon.couponType === 'discount' || coupon.couponType === 'DISCOUNT') {
        // 折扣券显示折扣
        return (coupon.value || 0) + '折'
      } else {
        // 现金券显示金额
        return coupon.value || 0
      }
    },
    
    getCouponTemplateValue(coupon) {
      if (coupon.type === 'discount' || coupon.type === 'DISCOUNT') {
        return (coupon.value || 0) + '折'
      } else {
        return coupon.value || 0
      }
    },
    
    getCouponMinAmount(coupon) {
      // 从后端返回的 minAmount 字段获取
      if (coupon.minAmount) {
        return coupon.minAmount
      }
      return null
    },
    
    // 获取状态类型
    getStatusType(status) {
      // 后端返回小写枚举，直接匹配
      const map = {
        'received': 'success',
        'locked': 'warning',
        'used': 'info',
        'expired': 'danger',
        'voided': 'info'
      }
      return map[status] || 'info'
    },
    
    getStatusText(status) {
      // 后端返回小写枚举，直接匹配
      const map = {
        'received': '可使用',
        'locked': '锁定中',
        'used': '已使用',
        'expired': '已过期',
        'voided': '已作废'
      }
      return map[status] || status
    },
    
    getRedemptionStatusType(status) {
      // 后端返回大写枚举
      const upperStatus = String(status || '').toUpperCase()
      const map = {
        'SUCCESS': 'success',
        'LOCKED': 'warning',
        'CANCELLED': 'info',
        'ROLLBACK': 'danger'
      }
      return map[upperStatus] || 'info'
    },
    
    getRedemptionStatusText(status) {
      // 后端返回大写枚举
      const upperStatus = String(status || '').toUpperCase()
      const map = {
        'SUCCESS': '成功',
        'LOCKED': '锁定',
        'CANCELLED': '取消',
        'ROLLBACK': '回滚'
      }
      return map[upperStatus] || status
    },
    
    // 判断优惠券是否可用
    isCouponAvailable(coupon) {
      const status = String(coupon.receiveStatus || '').toLowerCase()
      return status === 'received' || status === 'locked'
    },
    
    // 获取优惠券类型CSS类
    getCouponTypeClass(coupon) {
      const type = String(coupon.couponType || '').toLowerCase()
      if (type === 'discount') {
        return 'discount'
      } else if (type === 'amount_off') {
        return 'amount-off'
      }
      // 兼容旧的大写格式
      const upperType = String(coupon.couponType || '').toUpperCase()
      if (upperType === 'DISCOUNT') {
        return 'discount'
      } else if (upperType === 'AMOUNT_OFF') {
        return 'amount-off'
      }
      return 'amount-off'
    },
    
    // 获取优惠券类型文本
    getCouponTypeText(coupon) {
      const type = String(coupon.couponType || '').toLowerCase()
      if (type === 'discount') {
        return '折扣券'
      } else if (type === 'amount_off') {
        return '代金券'
      }
      // 兼容旧的大写格式
      const upperType = String(coupon.couponType || '').toUpperCase()
      if (upperType === 'DISCOUNT') {
        return '折扣券'
      } else if (upperType === 'AMOUNT_OFF') {
        return '代金券'
      }
      return '代金券'
    },
    
    // 格式化日期
    formatDate(date) {
      if (!date) return '-'
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hour = String(d.getHours()).padStart(2, '0')
      const minute = String(d.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}`
    }
  }
}
</script>

<style scoped>
.user-coupon-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 0 48px;
  color: var(--it-text);
}







.coupon-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-panel,
.coupon-shell {
  border-radius: var(--it-radius-card-lg);
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.hero-panel {
  padding: 30px 32px;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(240px, 340px);
  gap: 20px;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
  text-transform: uppercase;
}

.page-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 44px);
  line-height: 1.08;
  color: var(--it-text);
}

.page-subtitle {
  margin: 0;
  max-width: 620px;
  color: var(--it-text-muted);
  line-height: 1.75;
}

.hero-stats {
  display: grid;
  gap: 14px;
}

.hero-stat {
  padding: 18px 20px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.hero-stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--it-text-muted);
}

.hero-stat-value {
  font-size: 22px;
  color: var(--it-text);
}

.coupon-shell {
  padding: 20px 22px 24px;
}

.coupon-tabs :deep(.el-tabs__header) {
  margin: 0 0 18px;
  border-bottom: 1px solid var(--it-border);
}

.coupon-tabs :deep(.el-tabs__item) {
  color: var(--it-text-muted);
  font-size: 15px;
  font-weight: 600;
}

.coupon-tabs :deep(.el-tabs__item.is-active) {
  color: var(--it-accent);
}

.coupon-tabs :deep(.el-tabs__active-bar) {
  background: var(--it-primary-gradient);
}

.coupon-tabs :deep(.el-tabs__nav-wrap::after) {
  background: var(--it-border);
}

.tab-content {
  min-height: 400px;
  padding: 8px 0 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.stat-card {
  border-radius: var(--it-radius-card);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #fff;
  box-shadow: var(--it-shadow);
}

.stat-card.available { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-card.used { background: linear-gradient(135deg, #f59e0b, #d97706); }
.stat-card.expired { background: linear-gradient(135deg, #ef4444, #dc2626); }
.stat-card.total { background: linear-gradient(135deg, #10b981, #059669); }

.stat-icon {
  font-size: 30px;
  opacity: .95;
}

.stat-info { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; margin-bottom: 4px; }
.stat-label { font-size: 13px; opacity: .92; }

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
}

.coupon-card {
  position: relative;
  display: flex;
  overflow: hidden;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  box-shadow: var(--it-shadow);
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}

.coupon-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--it-shadow-hover);
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border));
}

.coupon-used,
.coupon-expired {
  opacity: .82;
}

.coupon-left {
  width: 126px;
  background: var(--it-primary-gradient);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 18px;
}

.coupon-amount { text-align: center; }
.currency { font-size: 18px; vertical-align: top; }
.value { font-size: 34px; font-weight: 700; }
.coupon-condition { font-size: 12px; margin-top: 8px; opacity: .9; }

.coupon-right {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.coupon-header,
.coupon-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.coupon-header {
  align-items: flex-start;
  margin-bottom: 12px;
}

.coupon-name {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
  color: var(--it-text);
}

.coupon-info {
  flex: 1;
  font-size: 13px;
  color: var(--it-text-muted);
}

.coupon-info p {
  margin: 8px 0;
  display: flex;
  align-items: center;
}

.coupon-info i {
  margin-right: 6px;
  color: var(--it-accent);
}

.coupon-actions {
  justify-content: flex-end;
  margin-top: 16px;
}

.coupon-type-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  font-weight: 700;
}

.coupon-type-badge.discount,
.coupon-type-badge.DISCOUNT { background: linear-gradient(135deg, #f59e0b, #d97706); }
.coupon-type-badge.amount-off,
.coupon-type-badge.AMOUNT_OFF { background: linear-gradient(135deg, #3b82f6, #2563eb); }

.empty-state {
  min-height: 260px;
  border-radius: var(--it-radius-card);
  border: 1px dashed var(--it-border-strong);
  background: color-mix(in srgb, var(--it-surface-solid) 90%, transparent);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
}

.empty-state i {
  font-size: 32px;
  color: var(--it-accent);
}

.empty-state p {
  margin: 0 0 8px;
  color: var(--it-text-muted);
}

.redemption-list { margin-top: 6px; }

:deep(.el-table) {
  border-radius: var(--it-radius-card);
  overflow: hidden;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  box-shadow: var(--it-shadow);
}

:deep(.el-table::before) {
  background-color: var(--it-border);
}

:deep(.el-table th),
:deep(.el-table tr),
:deep(.el-table td) {
  background: transparent;
}

:deep(.el-table__header-wrapper th) {
  background: color-mix(in srgb, var(--it-surface-solid) 88%, var(--it-page-bg)) !important;
  color: var(--it-text);
  border-bottom: 1px solid var(--it-border);
}

:deep(.el-table__body td) {
  color: var(--it-text-muted);
  border-bottom: 1px solid var(--it-border);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background: var(--it-accent-soft) !important;
}

:deep(.el-button) {
  border-radius: 999px;
}

:deep(.el-tag) {
  border-radius: 999px;
  border: none;
  font-weight: 600;
}

:deep(.el-dialog) {
  border-radius: var(--it-radius-card-lg);
  border: 1px solid var(--it-border);
  background: var(--it-surface);
}

@media (max-width: 960px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .user-coupon-page {
    padding: 18px 0 32px;
  }

  .hero-panel,
  .coupon-shell {
    padding: 20px;
  }

  .coupon-list {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }

  .coupon-card {
    flex-direction: column;
  }

  .coupon-left {
    width: 100%;
    padding: 18px;
  }
}
</style>
