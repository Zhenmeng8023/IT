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
          <el-button type="text" @click="$router.push('/Z_userpage/peoplehome')">
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
                  'coupon-available': coupon.receiveStatus === 'RECEIVED',
                  'coupon-used': coupon.receiveStatus === 'USED',
                  'coupon-expired': coupon.receiveStatus === 'EXPIRED'
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
                      v-if="coupon.receiveStatus === 'RECEIVED'"
                      type="primary" 
                      size="small"
                      @click="useCoupon(coupon)"
                    >
                      立即使用
                    </el-button>
                    <el-button 
                      v-else-if="coupon.receiveStatus === 'USED'"
                      size="small"
                      disabled
                    >
                      已使用
                    </el-button>
                    <el-button 
                      v-else-if="coupon.receiveStatus === 'EXPIRED'"
                      size="small"
                      disabled
                    >
                      已过期
                    </el-button>
                  </div>
                </div>
                <div class="coupon-type-badge" :class="coupon.couponType">
                  {{ coupon.couponType === 'DISCOUNT' ? '折扣券' : '代金券' }}
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
                <div class="coupon-type-badge" :class="coupon.type">
                  {{ coupon.type === 'DISCOUNT' ? '折扣券' : '代金券' }}
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
    // 可用优惠券
    availableCoupons() {
      return this.userCoupons.filter(c => c.receiveStatus === 'RECEIVED')
    },
    
    // 已使用优惠券
    usedCoupons() {
      return this.userCoupons.filter(c => c.receiveStatus === 'USED')
    },
    
    // 已过期优惠券
    expiredCoupons() {
      return this.userCoupons.filter(c => c.receiveStatus === 'EXPIRED')
    }
  },
  
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
    this.loadUserCoupons()
  },
  
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  
  methods: {
    handleScroll() {
      this.scrolled = window.scrollY > 50
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
      this.loading = true
      try {
        const userId = this.$store.state.user?.userInfo?.id || this.$route.query.userId
        if (!userId) {
          this.$message.warning('请先登录')
          return
        }
        
        const res = await getUserCoupons(userId)
        if (res.data.success) {
          this.userCoupons = res.data.data || []
        }
        
        // 加载总优惠金额
        const discountRes = await getUserTotalDiscount(userId)
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
      this.recordsLoading = true
      try {
        const userId = this.$store.state.user?.userInfo?.id || this.$route.query.userId
        if (!userId) return
        
        const res = await getUserRedemptions(userId)
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
      this.$refs.redeemForm.validate(async (valid) => {
        if (!valid) return
        
        this.redeeming = true
        try {
          const userId = this.$store.state.user?.userInfo?.id
          const res = await redeemCoupon({
            couponCode: this.redeemForm.couponCode,
            userId: userId
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
      try {
        const userId = this.$store.state.user?.userInfo?.id
        const res = await redeemCoupon({
          couponCode: code,
          userId: userId
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
      // 这里需要根据实际的优惠券模板信息来计算
      // 简化处理，实际应该从后端获取完整信息
      return coupon.couponType === 'DISCOUNT' ? '8折' : '20'
    },
    
    getCouponTemplateValue(coupon) {
      return coupon.type === 'DISCOUNT' ? (coupon.value + '折') : coupon.value
    },
    
    getCouponMinAmount(coupon) {
      // 简化处理
      return null
    },
    
    // 获取状态类型
    getStatusType(status) {
      const map = {
        'RECEIVED': 'success',
        'LOCKED': 'warning',
        'USED': 'info',
        'EXPIRED': 'danger',
        'VOID': 'info'
      }
      return map[status] || 'info'
    },
    
    getStatusText(status) {
      const map = {
        'RECEIVED': '可使用',
        'LOCKED': '锁定中',
        'USED': '已使用',
        'EXPIRED': '已过期',
        'VOID': '已作废'
      }
      return map[status] || status
    },
    
    getRedemptionStatusType(status) {
      const map = {
        'SUCCESS': 'success',
        'LOCKED': 'warning',
        'CANCELLED': 'info',
        'ROLLBACK': 'danger'
      }
      return map[status] || 'info'
    },
    
    getRedemptionStatusText(status) {
      const map = {
        'SUCCESS': '成功',
        'LOCKED': '锁定',
        'CANCELLED': '取消',
        'ROLLBACK': '回滚'
      }
      return map[status] || status
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
  background: #f5f7fa;
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
  color: #409eff;
  font-size: 24px;
  margin-right: 8px;
}

.logo-text {
  font-size: 20px;
  color: #303133;
}

/* 主内容区 */
.main-content {
  max-width: 1200px;
  margin: 80px auto 40px;
  padding: 0 20px;
}

.coupon-tabs {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  color: white;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-card.available {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card.used {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card.expired {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card.total {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  position: relative;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.coupon-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.coupon-available {
  border-color: #67c23a;
}

.coupon-used {
  border-color: #909399;
  opacity: 0.7;
}

.coupon-expired {
  border-color: #f56c6c;
  opacity: 0.6;
}

.coupon-left {
  width: 120px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
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
  color: #303133;
  margin: 0;
  flex: 1;
  margin-right: 10px;
}

.coupon-info {
  flex: 1;
  font-size: 13px;
  color: #909399;
}

.coupon-info p {
  margin: 5px 0;
  display: flex;
  align-items: center;
}

.coupon-info i {
  margin-right: 5px;
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

.coupon-type-badge.DISCOUNT {
  background: #e6a23c;
}

.coupon-type-badge.AMOUNT_OFF {
  background: #409eff;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.3;
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 20px;
}

/* 核销记录表格 */
.redemption-list {
  margin-top: 20px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .coupon-list {
    grid-template-columns: 1fr;
  }
}
</style>
