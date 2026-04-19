<template>
  <div class="coupon-manage-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>优惠券管理</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="loadCoupons">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>

      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="showCreateDialog">
          创建优惠券
        </el-button>
        <el-button icon="el-icon-download" @click="exportCoupons">
          导出数据
        </el-button>
      </div>

      <!-- 优惠券列表表格 -->
      <el-table
        :data="couponList"
        v-loading="loading"
        style="width: 100%"
        border
        stripe
        :header-cell-style="couponHeaderCellStyle"
      >
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="code" label="优惠券码" width="150"></el-table-column>
        <el-table-column prop="name" label="名称" min-width="150"></el-table-column>
        <el-table-column label="类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.type === 'DISCOUNT' ? 'warning' : 'success'" size="small">
              {{ scope.row.type === 'DISCOUNT' ? '折扣券' : '代金券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠值" width="120">
          <template slot-scope="scope">
            {{ scope.row.type === 'DISCOUNT' ? scope.row.value + '折' : '¥' + scope.row.value }}
          </template>
        </el-table-column>
        <el-table-column label="使用门槛" width="120">
          <template slot-scope="scope">
            {{ scope.row.minAmount ? '满¥' + scope.row.minAmount : '无门槛' }}
          </template>
        </el-table-column>
        <el-table-column label="发放情况" width="150">
          <template slot-scope="scope">
            {{ scope.row.issuedCount || 0 }}/{{ scope.row.totalLimit || '∞' }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template slot-scope="scope">
            <div>{{ formatDate(scope.row.startTime) }}</div>
            <div>至 {{ formatDate(scope.row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.isEnabled"
              :active-color="'var(--it-success)'"
              :inactive-color="'var(--it-danger)'"
              @change="toggleStatus(scope.row)"
            >
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="showEditDialog(scope.row)">
              编辑
            </el-button>
            <el-button type="text" size="small" @click="showIssueDialog(scope.row)">
              发放
            </el-button>
            <el-button type="text" size="small" class="danger-text-btn" @click="deleteCoupon(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.page + 1"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        style="margin-top: 20px; text-align: right"
      >
      </el-pagination>
    </el-card>

    <!-- 创建/编辑优惠券对话框 -->
    <el-dialog
      :title="dialogMode === 'create' ? '创建优惠券' : '编辑优惠券'"
      :visible.sync="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="couponForm" :model="couponForm" :rules="couponRules" label-width="120px">
        <el-form-item label="优惠券码" prop="code">
          <el-input v-model="couponForm.code" placeholder="请输入优惠券码（唯一）" clearable></el-input>
        </el-form-item>
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="couponForm.name" placeholder="请输入优惠券名称" clearable></el-input>
        </el-form-item>
        <el-form-item label="优惠券类型" prop="type">
          <el-radio-group v-model="couponForm.type">
            <el-radio label="AMOUNT_OFF">代金券（减免现金）</el-radio>
            <el-radio label="DISCOUNT">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠值" prop="value">
          <el-input-number 
            v-model="couponForm.value" 
            :min="0" 
            :precision="2"
            :step="couponForm.type === 'DISCOUNT' ? 0.1 : 1"
          ></el-input-number>
          <span class="form-tip">
            {{ couponForm.type === 'DISCOUNT' ? '折（如8表示8折）' : '元' }}
          </span>
        </el-form-item>
        <el-form-item label="使用门槛" prop="minAmount">
          <el-input-number v-model="couponForm.minAmount" :min="0" :precision="2"></el-input-number>
          <span class="form-tip">元（0表示无门槛）</span>
        </el-form-item>
        <el-form-item label="每人限领" prop="usageLimitPerUser">
          <el-input-number v-model="couponForm.usageLimitPerUser" :min="1"></el-input-number>
          <span class="form-tip">次</span>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalLimit">
          <el-input-number v-model="couponForm.totalLimit" :min="1"></el-input-number>
          <span class="form-tip">张（留空表示不限制）</span>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="couponForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            format="yyyy-MM-dd HH:mm:ss"
            value-format="yyyy-MM-dd HH:mm:ss"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="couponForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            format="yyyy-MM-dd HH:mm:ss"
            value-format="yyyy-MM-dd HH:mm:ss"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="couponForm.isEnabled"></el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </span>
    </el-dialog>

    <!-- 发放优惠券对话框 -->
    <el-dialog
      title="发放优惠券"
      :visible.sync="issueDialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="issueForm" :model="issueForm" :rules="issueRules" label-width="100px">
        <el-form-item label="优惠券">
          <el-input :value="currentCoupon?.name" disabled></el-input>
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input-number v-model="issueForm.userId" :min="1" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="发放来源">
          <el-select v-model="issueForm.sourceType" style="width: 100%">
            <el-option label="后台发放" value="MANUAL"></el-option>
            <el-option label="系统发放" value="SYSTEM"></el-option>
            <el-option label="活动领取" value="CAMPAIGN"></el-option>
            <el-option label="注册赠送" value="REGISTER"></el-option>
            <el-option label="会员赠送" value="MEMBERSHIP"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="issueForm.remark" 
            type="textarea" 
            :rows="3"
            placeholder="选填"
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIssue" :loading="issuing">
          确认发放
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAllCoupons,
  createCoupon,
  updateCoupon,
  deleteCoupon,
  toggleCouponStatus,
  issueCoupon
} from '@/api/coupon'

export default {
  name: 'CouponManage',
  layout: 'manage',
  data() {
    return {
      loading: false,
      submitting: false,
      issuing: false,
      
      // 优惠券列表
      couponList: [],
      
      // 分页
      pagination: {
        page: 0,
        size: 10,
        total: 0
      },
      
      // 对话框
      dialogVisible: false,
      dialogMode: 'create', // create or edit
      
      // 优惠券表单
      couponForm: {
        code: '',
        name: '',
        type: 'AMOUNT_OFF',
        value: 0,
        minAmount: 0,
        usageLimitPerUser: 1,
        totalLimit: 100,
        startTime: '',
        endTime: '',
        isEnabled: true
      },
      
      // 表单验证规则
      couponRules: {
        code: [
          { required: true, message: '请输入优惠券码', trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入优惠券名称', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择优惠券类型', trigger: 'change' }
        ],
        value: [
          { required: true, message: '请输入优惠值', trigger: 'blur' }
        ],
        startTime: [
          { required: true, message: '请选择开始时间', trigger: 'change' }
        ],
        endTime: [
          { required: true, message: '请选择结束时间', trigger: 'change' }
        ]
      },
      
      // 发放对话框
      issueDialogVisible: false,
      currentCoupon: null,
      issueForm: {
        userId: 1,
        sourceType: 'MANUAL',
        remark: ''
      },
      issueRules: {
        userId: [
          { required: true, message: '请输入用户ID', trigger: 'blur' }
        ]
      },
      couponHeaderCellStyle: {
        background: 'var(--it-surface-muted)',
        color: 'var(--it-text)',
        borderColor: 'var(--it-border)'
      }
    }
  },
  
  mounted() {
    this.loadCoupons()
  },
  
  methods: {
    // 加载优惠券列表
    async loadCoupons() {
      this.loading = true
      try {
        const res = await getAllCoupons({
          page: this.pagination.page,
          size: this.pagination.size
        })
        
        if (res.data.success) {
          this.couponList = res.data.data || []
          this.pagination.total = res.data.total || 0
        }
      } catch (error) {
        console.error('加载优惠券失败:', error)
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    
    // 显示创建对话框
    showCreateDialog() {
      this.dialogMode = 'create'
      this.resetForm()
      this.dialogVisible = true
    },
    
    // 显示编辑对话框
    showEditDialog(coupon) {
      this.dialogMode = 'edit'
      this.couponForm = {
        id: coupon.id,
        code: coupon.code,
        name: coupon.name,
        type: coupon.type,
        value: coupon.value,
        minAmount: coupon.minAmount || 0,
        usageLimitPerUser: coupon.usageLimitPerUser || 1,
        totalLimit: coupon.totalLimit || 100,
        startTime: coupon.startTime,
        endTime: coupon.endTime,
        isEnabled: coupon.isEnabled
      }
      this.dialogVisible = true
    },
    
    // 重置表单
    resetForm() {
      this.couponForm = {
        code: '',
        name: '',
        type: 'AMOUNT_OFF',
        value: 0,
        minAmount: 0,
        usageLimitPerUser: 1,
        totalLimit: 100,
        startTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
        endTime: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19).replace('T', ' '),
        isEnabled: true
      }
      if (this.$refs.couponForm) {
        this.$refs.couponForm.clearValidate()
      }
    },
    
    // 提交表单
    handleSubmit() {
      this.$refs.couponForm.validate(async (valid) => {
        if (!valid) return
        
        this.submitting = true
        try {
          let res
          if (this.dialogMode === 'create') {
            res = await createCoupon(this.couponForm)
          } else {
            res = await updateCoupon(this.couponForm.id, this.couponForm)
          }
          
          if (res.data.success) {
            this.$message.success(this.dialogMode === 'create' ? '创建成功' : '更新成功')
            this.dialogVisible = false
            this.loadCoupons()
          } else {
            this.$message.error(res.data.message || '操作失败')
          }
        } catch (error) {
          console.error('提交失败:', error)
          this.$message.error(error.response?.data?.message || '操作失败')
        } finally {
          this.submitting = false
        }
      })
    },
    
    // 删除优惠券
    deleteCoupon(id) {
      this.$confirm('确定要删除该优惠券吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteCoupon(id)
          if (res.data.success) {
            this.$message.success('删除成功')
            this.loadCoupons()
          } else {
            this.$message.error(res.data.message || '删除失败')
          }
        } catch (error) {
          console.error('删除失败:', error)
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }).catch(() => {})
    },
    
    // 切换状态
    async toggleStatus(coupon) {
      try {
        const res = await toggleCouponStatus(coupon.id, { enabled: coupon.isEnabled })
        if (res.data.success) {
          this.$message.success(coupon.isEnabled ? '已启用' : '已禁用')
        } else {
          this.$message.error(res.data.message || '操作失败')
          coupon.isEnabled = !coupon.isEnabled // 恢复状态
        }
      } catch (error) {
        console.error('切换状态失败:', error)
        this.$message.error('操作失败')
        coupon.isEnabled = !coupon.isEnabled // 恢复状态
      }
    },
    
    // 显示发放对话框
    showIssueDialog(coupon) {
      this.currentCoupon = coupon
      this.issueForm = {
        userId: 1,
        sourceType: 'MANUAL',
        remark: ''
      }
      this.issueDialogVisible = true
    },
    
    // 发放优惠券
    handleIssue() {
      this.$refs.issueForm.validate(async (valid) => {
        if (!valid) return
        
        this.issuing = true
        try {
          const res = await issueCoupon({
            couponId: this.currentCoupon.id,
            userId: this.issueForm.userId,
            sourceType: this.issueForm.sourceType,
            remark: this.issueForm.remark
          })
          
          if (res.data.success) {
            this.$message.success('发放成功')
            this.issueDialogVisible = false
          } else {
            this.$message.error(res.data.message || '发放失败')
          }
        } catch (error) {
          console.error('发放失败:', error)
          this.$message.error(error.response?.data?.message || '发放失败')
        } finally {
          this.issuing = false
        }
      })
    },
    
    // 导出优惠券
    exportCoupons() {
      this.$message.info('导出功能开发中')
      // TODO: 实现导出功能
    },
    
    // 分页大小改变
    handleSizeChange(val) {
      this.pagination.size = val
      this.pagination.page = 0
      this.loadCoupons()
    },
    
    // 页码改变
    handleCurrentChange(val) {
      this.pagination.page = val - 1
      this.loadCoupons()
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
.coupon-manage-container {
  padding: 20px;
}

.box-card {
  min-height: 600px;
  border-radius: 20px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.form-tip {
  margin-left: 10px;
  color: var(--it-text-subtle);
}

.danger-text-btn {
  color: var(--it-danger) !important;
}
</style>
