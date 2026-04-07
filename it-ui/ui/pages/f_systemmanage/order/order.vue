<template>
  <div class="admin-order-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>订单管理</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="loadStatistics">刷新统计</el-button>
      </div>

      <!-- 统计数据 -->
      <el-row :gutter="20" class="statistics-row">
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-number">{{ statistics.totalOrders || 0 }}</div>
            <div class="stat-label">总订单数</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card pending">
            <div class="stat-number">{{ statistics.pendingOrders || 0 }}</div>
            <div class="stat-label">待支付</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card paid">
            <div class="stat-number">{{ statistics.paidOrders || 0 }}</div>
            <div class="stat-label">已支付</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card refunded">
            <div class="stat-number">{{ statistics.refundedOrders || 0 }}</div>
            <div class="stat-label">已退款</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card failed">
            <div class="stat-number">{{ statistics.failedOrders || 0 }}</div>
            <div class="stat-label">失败</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card amount">
            <div class="stat-number">¥{{ statistics.totalAmount?.toFixed(2) || '0.00' }}</div>
            <div class="stat-label">总收入</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 筛选条件 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="订单状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="handleFilterChange">
            <el-option label="待支付" value="PENDING"></el-option>
            <el-option label="已支付" value="PAID"></el-option>
            <el-option label="已退款" value="REFUNDED"></el-option>
            <el-option label="失败" value="FAILED"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="filterType" placeholder="全部类型" clearable @change="handleFilterChange">
            <el-option label="会员订阅" value="membership"></el-option>
            <el-option label="内容购买" value="content"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-refresh" @click="loadOrders">刷新</el-button>
        </el-form-item>
      </el-form>

      <!-- 订单表格 -->
      <el-table
        :data="orderList"
        border
        stripe
        v-loading="loading"
        style="width: 100%">
        <el-table-column prop="id" label="订单ID" width="80"></el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="200"></el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100"></el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template slot-scope="scope">
            <el-tag :type="scope.row.type === 'membership' ? 'success' : 'primary'">
              {{ scope.row.type === 'membership' ? '会员订阅' : '内容购买' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template slot-scope="scope">
            ¥{{ scope.row.amount?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="120">
          <template slot-scope="scope">
            {{ scope.row.paymentMethod === 'alipay' ? '支付宝' : 
               scope.row.paymentMethod === 'wechat' ? '微信' : scope.row.paymentMethod }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="180">
          <template slot-scope="scope">
            {{ scope.row.payTime ? formatDateTime(scope.row.payTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="250">
          <template slot-scope="scope">
            <el-button size="mini" @click="viewDetail(scope.row)">详情</el-button>
            <el-button size="mini" type="warning" @click="showStatusDialog(scope.row)">改状态</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        :current-page="pagination.page + 1"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        style="margin-top: 20px; text-align: right;">
      </el-pagination>
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog title="订单详情" :visible.sync="detailDialogVisible" width="70%">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ currentOrder.type }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentOrder.amount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentOrder.paymentMethod }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.status)">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间" :span="2">
          {{ currentOrder.payTime ? formatDateTime(currentOrder.payTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 支付记录 -->
      <el-divider content-position="left">支付记录</el-divider>
      <el-table :data="paymentRecords" border stripe v-if="paymentRecords.length > 0">
        <el-table-column prop="id" label="记录ID" width="80"></el-table-column>
        <el-table-column prop="paymentPlatform" label="支付平台" width="120"></el-table-column>
        <el-table-column prop="transactionId" label="交易号" width="200"></el-table-column>
        <el-table-column prop="paymentStatus" label="状态" width="100"></el-table-column>
        <el-table-column prop="paymentAmount" label="金额" width="120">
          <template slot-scope="scope">¥{{ scope.row.paymentAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="paymentTime" label="支付时间" width="180">
          <template slot-scope="scope">{{ formatDateTime(scope.row.paymentTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无支付记录"></el-empty>
    </el-dialog>

    <!-- 修改状态对话框 -->
    <el-dialog title="修改订单状态" :visible.sync="statusDialogVisible" width="400px">
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(currentOrder?.status)">
            {{ getStatusText(currentOrder?.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新状态" required>
          <el-select v-model="statusForm.status" placeholder="请选择新状态" style="width: 100%">
            <el-option label="待支付" value="PENDING"></el-option>
            <el-option label="已支付" value="PAID"></el-option>
            <el-option label="已退款" value="REFUNDED"></el-option>
            <el-option label="失败" value="FAILED"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpdateStatus">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getOrdersPage, getOrderDetail, updateOrderStatus, deleteOrder, getOrderStatistics, getPaymentRecords } from '@/api/adminOrder'

export default {
  name: 'AdminOrderManagement',
  layout:'manage',
  data() {
    return {
      loading: false,
      orderList: [],
      statistics: {},
      pagination: {
        page: 0,
        size: 10,
        total: 0
      },
      filterStatus: '',
      filterType: '',
      detailDialogVisible: false,
      statusDialogVisible: false,
      currentOrder: null,
      paymentRecords: [],
      statusForm: {
        status: ''
      }
    }
  },
  created() {
    this.loadOrders()
    this.loadStatistics()
  },
  methods: {
    // 加载订单列表
    async loadOrders() {
      this.loading = true
      try {
        const params = {
          page: this.pagination.page,
          size: this.pagination.size
        }
        if (this.filterStatus) {
          params.status = this.filterStatus
        }
        if (this.filterType) {
          params.type = this.filterType
        }

        const res = await getOrdersPage(params)
        if (res.data.success) {
          this.orderList = res.data.data
          this.pagination.total = res.data.total
        }
      } catch (error) {
        this.$message.error('加载订单列表失败')
        console.error(error)
      } finally {
        this.loading = false
      }
    },

    // 加载统计数据
    async loadStatistics() {
      try {
        const res = await getOrderStatistics()
        if (res.data.success) {
          this.statistics = res.data.statistics
        }
      } catch (error) {
        console.error('加载统计数据失败', error)
      }
    },

    // 查看详情
    async viewDetail(order) {
      try {
        const res = await getOrderDetail(order.id)
        if (res.data.success) {
          this.currentOrder = res.data.order
          this.paymentRecords = res.data.paymentRecords || []
          this.detailDialogVisible = true
        }
      } catch (error) {
        this.$message.error('加载订单详情失败')
        console.error(error)
      }
    },

    // 显示修改状态对话框
    showStatusDialog(order) {
      this.currentOrder = order
      this.statusForm.status = order.status
      this.statusDialogVisible = true
    },

    // 确认更新状态
    async confirmUpdateStatus() {
      if (!this.statusForm.status) {
        this.$message.warning('请选择新状态')
        return
      }

      try {
        const res = await updateOrderStatus(this.currentOrder.id, this.statusForm.status)
        if (res.data.success) {
          this.$message.success('状态更新成功')
          this.statusDialogVisible = false
          this.loadOrders()
          this.loadStatistics()
        }
      } catch (error) {
        this.$message.error('状态更新失败')
        console.error(error)
      }
    },

    // 删除订单
    handleDelete(order) {
      if (order.status === 'PAID') {
        this.$confirm('该订单已支付，不建议删除。如需删除，请先退款并取消订单。', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 用户确认后也不建议删除
          this.$message.info('已支付的订单不允许删除')
        })
        return
      }

      this.$confirm('确定要删除该订单吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteOrder(order.id)
          if (res.data.success) {
            this.$message.success('删除成功')
            this.loadOrders()
            this.loadStatistics()
          }
        } catch (error) {
          this.$message.error('删除失败')
          console.error(error)
        }
      })
    },

    // 筛选变化
    handleFilterChange() {
      this.pagination.page = 0
      this.loadOrders()
    },

    // 分页大小变化
    handleSizeChange(val) {
      this.pagination.size = val
      this.pagination.page = 0
      this.loadOrders()
    },

    // 页码变化
    handlePageChange(val) {
      this.pagination.page = val - 1
      this.loadOrders()
    },

    // 获取状态类型
    getStatusType(status) {
      const types = {
        'PENDING': 'warning',
        'PAID': 'success',
        'REFUNDED': 'info',
        'FAILED': 'danger'
      }
      return types[status] || ''
    },

    // 获取状态文本
    getStatusText(status) {
      const texts = {
        'PENDING': '待支付',
        'PAID': '已支付',
        'REFUNDED': '已退款',
        'FAILED': '失败'
      }
      return texts[status] || status
    },

    // 格式化日期时间
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.admin-order-container {
  padding: 20px;
}

.statistics-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-card.pending .stat-number {
  color: #E6A23C;
}

.stat-card.paid .stat-number {
  color: #67C23A;
}

.stat-card.refunded .stat-number {
  color: #909399;
}

.stat-card.failed .stat-number {
  color: #F56C6C;
}

.stat-card.amount .stat-number {
  color: #E6A23C;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.filter-form {
  margin-bottom: 20px;
}
</style>
