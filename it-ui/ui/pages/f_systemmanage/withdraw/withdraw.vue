<template>
  <div class="withdraw-audit-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>提现审核管理</span>
        <el-button style="float: right; padding: 3px 10px" icon="el-icon-refresh" circle @click="fetchData"></el-button>
      </div>

      <!-- 筛选区域 -->
      <div class="filter-container">
        <el-select v-model="query.status" placeholder="提现状态" clearable @change="fetchData" style="width: 150px; margin-right: 10px;">
          <el-option label="待审核" value="PENDING"></el-option>
          <el-option label="已通过" value="APPROVED"></el-option>
          <el-option label="已拒绝" value="REJECTED"></el-option>
          <el-option label="已打款" value="PAID"></el-option>
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索用户ID或单号" style="width: 200px; margin-right: 10px;" @keyup.enter.native="fetchData"></el-input>
        <el-button type="primary" icon="el-icon-search" @click="fetchData">查询</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" style="width: 100%; margin-top: 20px;" border>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="requestNo" label="提现单号" width="200"></el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100"></el-table-column>
        <el-table-column prop="withdrawAmount" label="提现金额" width="120">
          <template slot-scope="scope">¥{{ scope.row.withdrawAmount }}</template>
        </el-table-column>
        <el-table-column prop="serviceFee" label="手续费" width="100">
          <template slot-scope="scope">¥{{ scope.row.serviceFee }}</template>
        </el-table-column>
        <el-table-column prop="actualAmount" label="实际到账" width="120">
          <template slot-scope="scope">¥{{ scope.row.actualAmount }}</template>
        </el-table-column>
        <el-table-column prop="accountInfo" label="收款账户" min-width="180">
          <template slot-scope="scope">
            {{ scope.row.settlementAccount ? scope.row.settlementAccount.accountNumber : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="180">
          <template slot-scope="scope">{{ scope.row.createdAt | formatDate }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="scope.row.status === 'PENDING'" type="success" size="mini" @click="handleApprove(scope.row)">审核通过</el-button>
            <el-button v-if="scope.row.status === 'PENDING'" type="danger" size="mini" @click="handleReject(scope.row)">拒绝</el-button>
            <el-button v-if="scope.row.status === 'APPROVED'" type="primary" size="mini" @click="handlePay(scope.row)">执行打款</el-button>
            <el-button size="mini" @click="viewDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 审核/拒绝/打款 弹窗 -->
      <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
        <el-form :model="form" label-width="100px">
          <el-form-item label="提现单号">
            <el-input v-model="form.requestNo" disabled></el-input>
          </el-form-item>
          <el-form-item label="提现金额">
            <el-input v-model="form.withdrawAmount" disabled></el-input>
          </el-form-item>
          <el-form-item label="审核/打款备注" v-if="dialogType !== 'pay'">
            <el-input type="textarea" v-model="form.note" placeholder="请输入备注信息"></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitAction" :loading="submitting">确 定</el-button>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      loading: false,
      submitting: false,
      query: {
        status: 'PENDING',
        keyword: ''
      },
      tableData: [],
      dialogVisible: false,
      dialogTitle: '',
      dialogType: '', // approve, reject, pay
      form: {
        id: null,
        requestNo: '',
        withdrawAmount: '',
        note: ''
      }
    }
  },
  filters: {
    formatDate(date) {
      if (!date) return '-'
      return new Date(date).toLocaleString()
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        let url = '/api/creator-withdraw-requests'
        if (this.query.status) {
          url = `/api/creator-withdraw-requests/status/${this.query.status}`
        }
        
        const res = await axios.get(url)
        let list = res.data
        if (res.data && res.data.success && Array.isArray(res.data.data)) {
          list = res.data.data
        }
        if (!Array.isArray(list)) list = []
        
        // 使用 Promise.all 并行请求所有关联账户，并生成新对象以确保 Vue 响应式更新
        const enrichedData = await Promise.all(
          list.map(async (item) => {
            let accountInfo = null
            if (item.settlementAccountId) {
              try {
                const accRes = await axios.get(`/api/creator-settlement-accounts/${item.settlementAccountId}`)
                let accData = accRes.data
                if (accRes.data && accRes.data.success && accRes.data.data) {
                  accData = accRes.data.data
                }
                accountInfo = accData
              } catch (e) {
                console.error(`加载账户 ${item.settlementAccountId} 失败`, e)
              }
            }
            // 返回包含新属性的新对象
            return { ...item, settlementAccount: accountInfo }
          })
        )
        
        this.tableData = enrichedData
      } catch (error) {
        this.$message.error('获取数据失败')
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    getStatusText(status) {
      const map = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'PAID': '已打款'
      }
      return map[status] || status
    },
    getStatusType(status) {
      const map = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'PAID': 'info'
      }
      return map[status] || 'info'
    },
    handleApprove(row) {
      this.dialogType = 'approve'
      this.dialogTitle = '审核通过'
      this.form = { id: row.id, requestNo: row.requestNo, withdrawAmount: row.withdrawAmount, note: '' }
      this.dialogVisible = true
    },
    handleReject(row) {
      this.dialogType = 'reject'
      this.dialogTitle = '拒绝申请'
      this.form = { id: row.id, requestNo: row.requestNo, withdrawAmount: row.withdrawAmount, note: '' }
      this.dialogVisible = true
    },
    handlePay(row) {
      this.dialogType = 'pay'
      this.dialogTitle = '执行打款'
      this.form = { id: row.id, requestNo: row.requestNo, withdrawAmount: row.withdrawAmount, note: '' }
      this.dialogVisible = true
    },
    async submitAction() {
      this.submitting = true
      try {
        const currentUserId = 1 // 这里应该从 Vuex 或 Cookie 获取当前管理员 ID
        let url = ''
        let method = 'put'
        let data = { reviewedBy: currentUserId, reviewNote: this.form.note }

        if (this.dialogType === 'approve') {
          url = `/api/creator-withdraw-requests/${this.form.id}/approve`
        } else if (this.dialogType === 'reject') {
          url = `/api/creator-withdraw-requests/${this.form.id}/reject`
        } else if (this.dialogType === 'pay') {
          url = `/api/creator-withdraw-requests/${this.form.id}/pay`
          data = {} // 打款不需要备注参数，根据后端定义
        }

        await axios({ method, url, data })
        this.$message.success('操作成功')
        this.dialogVisible = false
        this.fetchData()
      } catch (error) {
        this.$message.error(error.response?.data?.message || '操作失败')
      } finally {
        this.submitting = false
      }
    },
    viewDetail(row) {
      this.$alert(`
        <p>单号：${row.requestNo}</p>
        <p>用户ID：${row.userId}</p>
        <p>提现金额：${row.withdrawAmount}</p>
        <p>手续费：${row.serviceFee}</p>
        <p>实际到账：${row.actualAmount}</p>
        <p>状态：${this.getStatusText(row.status)}</p>
        <p>审核备注：${row.reviewNote || '无'}</p>
        <p>支付宝交易号：${row.payChannelRef || '无'}</p>
      `, '提现详情', {
        dangerouslyUseHTMLString: true
      })
    }
  }
}
</script>

<style scoped>
.withdraw-audit-container {
  padding: 20px;
}
.filter-container {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>
