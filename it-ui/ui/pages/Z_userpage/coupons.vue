<template>
  <div class="coupon-container">
    <!-- 顶部导航 -->
    <nav class="navbar" :class="{ 'navbar-scrolled': scrolled }">
      <div class="navbar-content">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">●</span>
          <span class="logo-text">ITSpace</span>
        </div>
        <div class="nav-actions">
          <el-button type="text" @click="$router.push('/user')">
            <i class="el-icon-arrow-left"></i> 返回主页
          </el-button>
        </div>
      </div>
    </nav>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 标签页切换 -->
      <el-tabs v-model="activeTab" class="coupon-tabs" @tab-click="handleTabClick">
        <!-- 我的优惠券 -->
        <el-tab-pane label="我的优惠券" name="my">
          <div class="tab-content">
            <!-- 统计卡片 -->
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

            <!-- 领取优惠券按钮 -->
            <div class="action-bar">
              <el-button type="primary" icon="el-icon-plus" @click="showRedeemDialog = true">
                兑换优惠券
              </el-button>
              <el-button icon="el-icon-refresh" @click="loadUserCoupons">
                刷新
              </el-button>
            </div>

            <!-- 优惠券列表 -->
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

        <!-- 可领取优惠券 -->
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

        <!-- 使用记录 -->
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
                :header-cell-style="{ background: '#f5f7fa' }"
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
    </div>

    <!-- 兑换优惠券对话框 -->
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
      scrolled: false,
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
    window.addEventListener('scroll', this.handleScroll)
    this.initPage()
  },
  
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  
  methods: {
    handleScroll() {
      this.scrolled = window.scrollY > 50
    },
    
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
.coupon-container {
  min-height: 100vh;
  background: #f1f5f9;
}

/* 导航栏 */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 1000;
  transition: all 0.3s;
}

.navbar-scrolled {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.navbar-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: 600;
}

.logo-icon {
  color: #3b82f6;
  font-size: 24px;
  margin-right: 8px;
}

.logo-text {
  font-size: 20px;
  color: #1e293b;
}

/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 80px auto 40px;
  padding: 0 20px;
}

.coupon-tabs {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.tab-content {
  min-height: 400px;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  color: white;
  transition: transform 0.3s ease;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3);
}

.stat-card.available {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.stat-card.used {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.stat-card.expired {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.stat-card.total {
  background: linear-gradient(135deg, #10b981, #059669);
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
  opacity: 0.9;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

/* 操作栏 */
.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

/* 优惠券列表 */
.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.coupon-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  position: relative;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.coupon-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.05);
  border-color: rgba(59, 130, 246, 0.2);
}

.coupon-available {
  border-color: rgba(16, 185, 129, 0.2);
}

.coupon-used {
  border-color: rgba(148, 163, 184, 0.2);
  opacity: 0.7;
}

.coupon-expired {
  border-color: rgba(239, 68, 68, 0.2);
  opacity: 0.6;
}

.coupon-left {
  width: 120px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  padding: 20px;
}

.coupon-amount {
  text-align: center;
}

.currency {
  font-size: 18px;
  vertical-align: top;
}

.value {
  font-size: 36px;
  font-weight: bold;
}

.coupon-condition {
  font-size: 12px;
  margin-top: 8px;
  opacity: 0.9;
}

.coupon-right {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.coupon-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.coupon-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  flex: 1;
  margin-right: 10px;
  transition: color 0.2s ease;
}

.coupon-card:hover .coupon-name {
  color: #3b82f6;
}

.coupon-info {
  flex: 1;
  font-size: 13px;
  color: #64748b;
}

.coupon-info p {
  margin: 5px 0;
  display: flex;
  align-items: center;
}

.coupon-info i {
  margin-right: 5px;
  color: #94a3b8;
}

.coupon-actions {
  margin-top: 15px;
  text-align: right;
}

.coupon-type-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  color: white;
  font-weight: 500;
}

.coupon-type-badge.discount {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.coupon-type-badge.amount-off {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

/* 兼容旧的大写格式 */
.coupon-type-badge.DISCOUNT {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.coupon-type-badge.AMOUNT_OFF {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.3;
  color: #cbd5e1;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 20px;
  color: #64748b;
  font-weight: 500;
}

/* 核销记录表格 */
.redemption-list {
  margin-top: 20px;
}

/* 自定义表格样式 */
:deep(.el-table) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.03);
}

:deep(.el-table__header-wrapper th) {
  background: #f8fafc !important;
  color: #475569;
  font-weight: 600;
}

:deep(.el-table__body-wrapper tr:hover) {
  background: #f8fafc !important;
}

/* 自定义按钮样式 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  border-radius: 8px;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.2);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  box-shadow: 0 6px 12px rgba(59, 130, 246, 0.3);
}

:deep(.el-button--text) {
  color: #3b82f6;
  transition: color 0.2s ease;
}

:deep(.el-button--text:hover) {
  color: #2563eb;
}

/* 自定义标签样式 */
:deep(.el-tag) {
  border-radius: 12px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 500;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
  color: white;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border: none;
  color: white;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: none;
  color: white;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .coupon-list {
    grid-template-columns: 1fr;
  }
  
  .main-content {
    padding: 0 15px;
  }
}
</style>