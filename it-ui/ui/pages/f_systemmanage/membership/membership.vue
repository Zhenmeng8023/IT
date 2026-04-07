<template>
  <div class="admin-membership-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>会员管理</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="loadStatistics">刷新统计</el-button>
      </div>

      <!-- 统计数据 -->
      <el-row :gutter="20" class="statistics-row">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-number">{{ statistics.totalMemberships || 0 }}</div>
            <div class="stat-label">总会员数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card active">
            <div class="stat-number">{{ statistics.activeMemberships || 0 }}</div>
            <div class="stat-label">活跃会员</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card expired">
            <div class="stat-number">{{ statistics.expiredMemberships || 0 }}</div>
            <div class="stat-label">已过期</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card cancelled">
            <div class="stat-number">{{ statistics.cancelledMemberships || 0 }}</div>
            <div class="stat-label">已取消</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 操作按钮 -->
      <div class="operation-bar">
        <el-button type="primary" icon="el-icon-plus" @click="showGrantDialog">手动开通会员</el-button>
      </div>

      <!-- 筛选条件 -->
      <el-form :inline="true" class="filter-form">
        <el-form-item label="会员状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="handleFilterChange">
            <el-option label="激活" value="active"></el-option>
            <el-option label="已过期" value="expired"></el-option>
            <el-option label="已取消" value="cancelled"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filterUserId" placeholder="输入用户ID" clearable style="width: 200px"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 会员表格 -->
      <el-table
        :data="membershipList"
        border
        stripe
        v-loading="loading"
        style="width: 100%">
        <el-table-column prop="id" label="会员ID" width="80"></el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100"></el-table-column>
        <el-table-column prop="levelName" label="会员等级" width="150">
          <template slot-scope="scope">
            <el-tag type="success">{{ scope.row.levelName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="到期时间" width="180">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.status === 'active' && scope.row.endTime">
              {{ getRemainingDays(scope.row.endTime) }} 天
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="isVip" label="是否VIP" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isVip ? 'success' : 'info'">
              {{ scope.row.isVip ? '是' : '否' }}
            </el-tag>
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

    <!-- 会员详情对话框 -->
    <el-dialog title="会员详情" :visible.sync="detailDialogVisible" width="600px">
      <el-descriptions :column="1" border v-if="currentMembership">
        <el-descriptions-item label="会员ID">{{ currentMembership.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentMembership.userId }}</el-descriptions-item>
        <el-descriptions-item label="会员等级">{{ currentMembership.levelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentMembership.status)">
            {{ getStatusText(currentMembership.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDateTime(currentMembership.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="到期时间">{{ formatDateTime(currentMembership.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="是否VIP">
          <el-tag :type="currentMembership.isVip ? 'success' : 'info'">
            {{ currentMembership.isVip ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 修改状态对话框 -->
    <el-dialog title="修改会员状态" :visible.sync="statusDialogVisible" width="400px">
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(currentMembership?.status)">
            {{ getStatusText(currentMembership?.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新状态" required>
          <el-select v-model="statusForm.status" placeholder="请选择新状态" style="width: 100%">
            <el-option label="激活" value="active"></el-option>
            <el-option label="已过期" value="expired"></el-option>
            <el-option label="已取消" value="cancelled"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpdateStatus">确定</el-button>
      </span>
    </el-dialog>

    <!-- 手动开通会员对话框 -->
    <el-dialog title="手动开通/续费会员" :visible.sync="grantDialogVisible" width="500px">
      <el-form :model="grantForm" label-width="120px">
        <el-form-item label="用户ID" required>
          <el-input-number v-model="grantForm.userId" :min="1" placeholder="请输入用户ID" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="会员等级ID" required>
          <el-input-number v-model="grantForm.levelId" :min="1" placeholder="请输入会员等级ID" style="width: 100%"></el-input-number>
          <div class="form-tip">提示：1=月卡, 2=季卡, 3=年卡（根据实际配置）</div>
        </el-form-item>
        <el-form-item label="开通天数">
          <el-input-number v-model="grantForm.durationDays" :min="1" placeholder="留空则使用等级默认天数" style="width: 100%"></el-input-number>
          <div class="form-tip">不填写则使用该等级的默认天数</div>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGrantMembership" :loading="grantLoading">确定开通</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getMembershipsPage, getMembershipDetail, updateMembershipStatus, deleteMembership, getMembershipStatistics, grantMembership } from '@/api/adminMembership'

export default {
  name: 'AdminMembershipManagement',
  layout:'manage',
  data() {
    return {
      loading: false,
      membershipList: [],
      statistics: {},
      pagination: {
        page: 0,
        size: 10,
        total: 0
      },
      filterStatus: '',
      filterUserId: '',
      detailDialogVisible: false,
      statusDialogVisible: false,
      grantDialogVisible: false,
      currentMembership: null,
      statusForm: {
        status: ''
      },
      grantForm: {
        userId: null,
        levelId: null,
        durationDays: null
      },
      grantLoading: false
    }
  },
  created() {
    this.loadMemberships()
    this.loadStatistics()
  },
  methods: {
    // 加载会员列表
    async loadMemberships() {
      this.loading = true
      try {
        const params = {
          page: this.pagination.page,
          size: this.pagination.size
        }
        if (this.filterStatus) {
          params.status = this.filterStatus
        }
        if (this.filterUserId) {
          params.userId = parseInt(this.filterUserId)
        }

        const res = await getMembershipsPage(params)
        if (res.data.success) {
          this.membershipList = res.data.data
          this.pagination.total = res.data.total
        }
      } catch (error) {
        this.$message.error('加载会员列表失败')
        console.error(error)
      } finally {
        this.loading = false
      }
    },

    // 加载统计数据
    async loadStatistics() {
      try {
        const res = await getMembershipStatistics()
        if (res.data.success) {
          this.statistics = res.data.statistics
        }
      } catch (error) {
        console.error('加载统计数据失败', error)
      }
    },

    // 查看详情
    async viewDetail(membership) {
      try {
        const res = await getMembershipDetail(membership.id)
        if (res.data.success) {
          this.currentMembership = res.data.data
          this.detailDialogVisible = true
        }
      } catch (error) {
        this.$message.error('加载会员详情失败')
        console.error(error)
      }
    },

    // 显示修改状态对话框
    showStatusDialog(membership) {
      this.currentMembership = membership
      this.statusForm.status = membership.status
      this.statusDialogVisible = true
    },

    // 确认更新状态
    async confirmUpdateStatus() {
      if (!this.statusForm.status) {
        this.$message.warning('请选择新状态')
        return
      }

      try {
        const res = await updateMembershipStatus(this.currentMembership.id, this.statusForm.status)
        if (res.data.success) {
          this.$message.success('状态更新成功')
          this.statusDialogVisible = false
          this.loadMemberships()
          this.loadStatistics()
        }
      } catch (error) {
        this.$message.error('状态更新失败')
        console.error(error)
      }
    },

    // 显示开通会员对话框
    showGrantDialog() {
      this.grantForm = {
        userId: null,
        levelId: null,
        durationDays: null
      }
      this.grantDialogVisible = true
    },

    // 确认开通会员
    async confirmGrantMembership() {
      if (!this.grantForm.userId || !this.grantForm.levelId) {
        this.$message.warning('请填写用户ID和会员等级ID')
        return
      }

      this.grantLoading = true
      try {
        const data = {
          userId: this.grantForm.userId,
          levelId: this.grantForm.levelId
        }
        if (this.grantForm.durationDays) {
          data.durationDays = this.grantForm.durationDays
        }

        const res = await grantMembership(data)
        if (res.data.success) {
          this.$message.success(res.data.message)
          this.grantDialogVisible = false
          this.loadMemberships()
          this.loadStatistics()
        }
      } catch (error) {
        this.$message.error('开通失败：' + (error.response?.data?.message || error.message))
        console.error(error)
      } finally {
        this.grantLoading = false
      }
    },

    // 删除会员记录
    handleDelete(membership) {
      if (membership.status === 'active') {
        this.$confirm('该会员记录处于激活状态，不建议删除。如需取消，请将状态改为 cancelled。', '警告', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$message.info('激活状态的会员不允许删除')
        })
        return
      }

      this.$confirm('确定要删除该会员记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteMembership(membership.id)
          if (res.data.success) {
            this.$message.success('删除成功')
            this.loadMemberships()
            this.loadStatistics()
          }
        } catch (error) {
          this.$message.error('删除失败')
          console.error(error)
        }
      })
    },

    // 搜索
    handleSearch() {
      this.pagination.page = 0
      this.loadMemberships()
    },

    // 重置
    handleReset() {
      this.filterStatus = ''
      this.filterUserId = ''
      this.pagination.page = 0
      this.loadMemberships()
    },

    // 筛选变化
    handleFilterChange() {
      this.pagination.page = 0
      this.loadMemberships()
    },

    // 分页大小变化
    handleSizeChange(val) {
      this.pagination.size = val
      this.pagination.page = 0
      this.loadMemberships()
    },

    // 页码变化
    handlePageChange(val) {
      this.pagination.page = val - 1
      this.loadMemberships()
    },

    // 获取状态类型
    getStatusType(status) {
      const types = {
        'active': 'success',
        'expired': 'info',
        'cancelled': 'danger'
      }
      return types[status] || ''
    },

    // 获取状态文本
    getStatusText(status) {
      const texts = {
        'active': '激活',
        'expired': '已过期',
        'cancelled': '已取消'
      }
      return texts[status] || status
    },

    // 计算剩余天数
    getRemainingDays(endTime) {
      if (!endTime) return 0
      const end = new Date(endTime)
      const now = new Date()
      const diff = end.getTime() - now.getTime()
      const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
      return days > 0 ? days : 0
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
.admin-membership-container {
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

.stat-card.active .stat-number {
  color: #67C23A;
}

.stat-card.expired .stat-number {
  color: #909399;
}

.stat-card.cancelled .stat-number {
  color: #F56C6C;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.operation-bar {
  margin-bottom: 20px;
}

.filter-form {
  margin-bottom: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
