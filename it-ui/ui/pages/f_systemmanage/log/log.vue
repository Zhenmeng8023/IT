<template>
  <div class="log-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>日志管理</h1>
      <p>查看系统操作日志和运行日志</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" :inline="true">
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.type" placeholder="请选择操作类型" clearable>
            <el-option label="用户操作" value="user"></el-option>
            <el-option label="系统操作" value="system"></el-option>
            <el-option label="安全操作" value="security"></el-option>
            <el-option label="错误日志" value="error"></el-option>
            <el-option label="所有类型" value=""></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="操作人员">
          <el-input
            v-model="searchForm.operator"
            placeholder="请输入操作人员"
            clearable
            style="width: 150px">
          </el-input>
        </el-form-item>
        
        <el-form-item label="操作模块">
          <el-input
            v-model="searchForm.module"
            placeholder="请输入操作模块"
            clearable
            style="width: 150px">
          </el-input>
        </el-form-item>
        
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            style="width: 240px">
          </el-date-picker>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="warning" @click="handleExport" :loading="exportLoading">导出日志</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="logList"
        v-loading="loading"
        stripe
        style="width: 100%"
        height="600">
        
        <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
        
        <el-table-column prop="createTime" label="操作时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="operator" label="操作人员" width="120" align="center"></el-table-column>
        
        <el-table-column prop="type" label="操作类型" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getLogType(scope.row.type)" size="small">
              {{ getTypeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="module" label="操作模块" width="150" align="center"></el-table-column>
        
        <el-table-column prop="action" label="操作内容" min-width="200">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.action" placement="top" v-if="scope.row.action && scope.row.action.length > 30">
              <span>{{ scope.row.action.substring(0, 30) }}...</span>
            </el-tooltip>
            <span v-else>{{ scope.row.action }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="ip" label="IP地址" width="130" align="center"></el-table-column>
        
        <el-table-column prop="result" label="操作结果" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.result === 'success' ? 'success' : 'danger'" size="small">
              {{ scope.row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="details" label="详细信息" min-width="250">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.details" placement="top" v-if="scope.row.details && scope.row.details.length > 40">
              <span>{{ scope.row.details.substring(0, 40) }}...</span>
            </el-tooltip>
            <span v-else>{{ scope.row.details || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.currentPage"
          :page-sizes="[20, 50, 100, 200]"
          :page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog
      title="日志详细信息"
      :visible.sync="detailDialogVisible"
      width="700px">
      <div class="log-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作时间">{{ formatDateTime(currentLog.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="操作人员">{{ currentLog.operator }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="getLogType(currentLog.type)" size="small">
              {{ getTypeLabel(currentLog.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作模块">{{ currentLog.module }}</el-descriptions-item>
          <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
          <el-descriptions-item label="操作结果">
            <el-tag :type="currentLog.result === 'success' ? 'success' : 'danger'" size="small">
              {{ currentLog.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作内容" :span="2">{{ currentLog.action }}</el-descriptions-item>
          <el-descriptions-item label="详细信息" :span="2">
            <pre style="white-space: pre-wrap; word-wrap: break-word; margin: 0;">{{ currentLog.details || '无详细信息' }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Log',
  layout: 'manage',
  data() {
    return {
      loading: false,
      exportLoading: false,
      // 搜索表单
      searchForm: {
        type: '',
        operator: '',
        module: '',
        dateRange: []
      },
      // 日志列表数据
      logList: [],
      // 分页信息
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      // 详情对话框控制
      detailDialogVisible: false,
      // 当前查看的日志
      currentLog: {},
      // 接口地址配置（预留）
      apiUrls: {
        getLogs: '/api/logs',
        exportLogs: '/api/logs/export'
      }
    }
  },
  mounted() {
    this.fetchLogList()
  },
  methods: {
    // 获取日志列表（预留接口接入点）
    async fetchLogList() {
      this.loading = true
      try {
        // TODO: 接入后端接口时替换以下代码
        // const response = await this.$axios.get(this.apiUrls.getLogs, {
        //   params: {
        //     page: this.pagination.currentPage,
        //     size: this.pagination.pageSize,
        //     ...this.searchForm
        //   }
        // })
        // 
        // if (response.data.success) {
        //   this.logList = response.data.data.list
        //   this.pagination.total = response.data.data.total
        // } else {
        //   this.$message.error(response.data.message)
        // }
        
        // 模拟数据（后端接口接入后删除）
        this.logList = this.generateMockData()
        this.pagination.total = this.logList.length
        
      } catch (error) {
        console.error('获取日志列表失败:', error)
        this.$message.error('获取日志列表失败')
      } finally {
        this.loading = false
      }
    },
    
    // 生成模拟数据（后端接口接入后删除）
    generateMockData() {
      const operators = ['admin', 'user1', 'user2', 'system']
      const modules = ['用户管理', '角色管理', '菜单管理', '博客管理', '项目管理']
      const actions = [
        '新增用户：张三',
        '修改用户权限',
        '删除菜单项',
        '审核博客文章',
        '导出用户数据',
        '重置用户密码',
        '修改系统配置',
        '登录系统',
        '退出系统',
        '备份数据库'
      ]
      
      const mockData = []
      for (let i = 0; i < 50; i++) {
        const type = ['user', 'system', 'security', 'error'][Math.floor(Math.random() * 4)]
        const result = Math.random() > 0.1 ? 'success' : 'failed'
        
        mockData.push({
          id: i + 1,
          createTime: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
          operator: operators[Math.floor(Math.random() * operators.length)],
          type: type,
          module: modules[Math.floor(Math.random() * modules.length)],
          action: actions[Math.floor(Math.random() * actions.length)],
          ip: `192.168.1.${Math.floor(Math.random() * 255)}`,
          result: result,
          details: result === 'success' ? '操作执行成功' : '操作执行失败，原因：权限不足'
        })
      }
      
      return mockData.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    },
    
    // 搜索日志
    handleSearch() {
      this.pagination.currentPage = 1
      this.fetchLogList()
    },
    
    // 重置搜索
    handleReset() {
      this.searchForm = {
        type: '',
        operator: '',
        module: '',
        dateRange: []
      }
      this.pagination.currentPage = 1
      this.fetchLogList()
    },
    
    // 导出日志（预留接口接入点）
    async handleExport() {
      this.exportLoading = true
      try {
        // TODO: 接入后端接口时替换以下代码
        // const response = await this.$axios.get(this.apiUrls.exportLogs, {
        //   params: this.searchForm,
        //   responseType: 'blob'
        // })
        // 
        // // 处理文件下载
        // const blob = new Blob([response.data])
        // const url = window.URL.createObjectURL(blob)
        // const link = document.createElement('a')
        // link.href = url
        // link.download = `系统日志_${new Date().toLocaleDateString()}.xlsx`
        // link.click()
        // window.URL.revokeObjectURL(url)
        
        // 模拟导出成功
        this.$message.success('日志导出功能已预留，后端接口接入后即可使用')
        
      } catch (error) {
        console.error('导出日志失败:', error)
        this.$message.error('导出日志失败')
      } finally {
        this.exportLoading = false
      }
    },
    
    // 查看日志详情
    handleView(log) {
      this.currentLog = { ...log }
      this.detailDialogVisible = true
    },
    
    // 获取日志类型标签
    getTypeLabel(type) {
      const labels = {
        user: '用户操作',
        system: '系统操作',
        security: '安全操作',
        error: '错误日志'
      }
      return labels[type] || type
    },
    
    // 获取日志类型样式
    getLogType(type) {
      const types = {
        user: 'primary',
        system: 'info',
        security: 'warning',
        error: 'danger'
      }
      return types[type] || 'info'
    },
    
    // 分页大小改变
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.pagination.currentPage = 1
      this.fetchLogList()
    },
    
    // 当前页改变
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.fetchLogList()
    },
    
    // 格式化日期时间
    formatDateTime(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
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
.log-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.log-detail {
  padding: 10px;
}

.log-detail pre {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.dialog-footer {
  text-align: right;
}
</style>