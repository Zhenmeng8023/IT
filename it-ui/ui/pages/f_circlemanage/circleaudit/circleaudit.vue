<template>
  <div class="circle-audit">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>圈子审核管理</h1>
      <p>审核用户创建的圈子申请，管理圈子信息和成员审核</p>
    </div>

    <!-- 筛选工具栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-toolbar">
        <div class="filter-left">
          <el-select v-model="filterForm.status" placeholder="审核状态" clearable style="width: 120px">
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已通过" value="approved"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
            <el-option label="已关闭" value="closed"></el-option>
          </el-select>
          
          <el-select v-model="filterForm.type" placeholder="圈子类型" clearable style="width: 120px; margin-left: 10px">
            <el-option label="技术交流" value="tech"></el-option>
            <el-option label="学习讨论" value="study"></el-option>
            <el-option label="兴趣爱好" value="hobby"></el-option>
            <el-option label="生活分享" value="life"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
          
          <el-select v-model="filterForm.privacy" placeholder="隐私设置" clearable style="width: 120px; margin-left: 10px">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私密" value="private"></el-option>
            <el-option label="需要审核" value="approval"></el-option>
          </el-select>
          
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="申请开始日期"
            end-placeholder="申请结束日期"
            style="width: 240px; margin-left: 10px">
          </el-date-picker>
        </div>
        
        <div class="filter-right">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索圈子名称或创建人"
            clearable
            style="width: 250px"
            prefix-icon="el-icon-search"
            @input="handleSearch">
          </el-input>
        </div>
      </div>
    </el-card>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" icon="el-icon-check" @click="handleBatchApprove" :disabled="selectedCircles.length === 0">
          批量通过
        </el-button>
        <el-button type="danger" icon="el-icon-close" @click="handleBatchReject" :disabled="selectedCircles.length === 0">
          批量拒绝
        </el-button>
        <el-button type="warning" icon="el-icon-star-on" @click="handleBatchRecommend" :disabled="selectedCircles.length === 0 || !selectedCircles.some(c => c.status === 'approved' && !c.isRecommended)">
          批量推荐
        </el-button>
        <el-button type="info" icon="el-icon-star-off" @click="handleBatchCancelRecommend" :disabled="selectedCircles.length === 0 || !selectedCircles.some(c => c.isRecommended)">
          取消推荐
        </el-button>
        <el-button type="success" icon="el-icon-user" @click="handleBatchClose" :disabled="selectedCircles.length === 0">
          批量关闭
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">
          刷新
        </el-button>
        <div class="toolbar-right">
          <el-button type="text" icon="el-icon-download">
            导出数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 圈子列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="circleList"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
        style="width: 100%">
        
        <el-table-column type="selection" width="55"></el-table-column>
        
        <el-table-column prop="name" label="圈子名称" min-width="180">
          <template slot-scope="scope">
            <div class="circle-name">
              <el-avatar :size="32" :src="scope.row.avatar" :alt="scope.row.name" style="vertical-align: middle; margin-right: 8px;"></el-avatar>
              <span class="name-text">{{ scope.row.name }}</span>
              <el-tag v-if="scope.row.isRecommended" size="mini" type="warning" style="margin-left: 5px">推荐</el-tag>
              <el-tag v-if="scope.row.privacy === 'private'" size="mini" type="info" style="margin-left: 5px">私密</el-tag>
              <el-tag v-if="scope.row.privacy === 'approval'" size="mini" type="warning" style="margin-left: 5px">需审核</el-tag>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="creator" label="创建人" width="120">
          <template slot-scope="scope">
            <el-avatar :size="24" :src="scope.row.creatorAvatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
            {{ scope.row.creator }}
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="圈子类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="getTypeType(scope.row.type)" size="small">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="memberCount" label="成员数" width="80" align="center">
          <template slot-scope="scope">
            {{ scope.row.memberCount }}
          </template>
        </el-table-column>
        
        <el-table-column prop="postCount" label="帖子数" width="80" align="center">
          <template slot-scope="scope">
            {{ scope.row.postCount }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="auditTime" label="审核时间" width="160" align="center">
          <template slot-scope="scope">
            {{ scope.row.auditTime ? formatDate(scope.row.auditTime) : '-' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="审核状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleView(scope.row)">
              查看
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="handleApprove(scope.row)"
              style="color: #67C23A;">
              通过
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-close"
              @click="handleReject(scope.row)"
              style="color: #F56C6C;">
              拒绝
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'approved'"
              size="mini"
              type="text"
              :icon="scope.row.isRecommended ? 'el-icon-star-on' : 'el-icon-star-off'"
              @click="handleToggleRecommend(scope.row)"
              :style="{color: scope.row.isRecommended ? '#E6A23C' : '#909399'}">
              {{ scope.row.isRecommended ? '取消推荐' : '推荐' }}
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'approved'"
              size="mini"
              type="text"
              icon="el-icon-user"
              @click="handleMemberAudit(scope.row)"
              style="color: #409EFF;">
              成员审核
            </el-button>
            
            <el-button
              v-if="scope.row.status === 'approved'"
              size="mini"
              type="text"
              icon="el-icon-switch-button"
              @click="handleCloseCircle(scope.row)"
              style="color: #909399;">
              关闭圈子
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
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 圈子详情对话框 -->
    <el-dialog
      :title="currentCircle ? currentCircle.name + ' - 圈子详情' : '圈子详情'"
      :visible.sync="detailDialogVisible"
      width="70%"
      :before-close="handleCloseDetail">
      <div v-if="currentCircle" class="circle-detail">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="circle-basic-info">
              <el-avatar :size="80" :src="currentCircle.avatar" :alt="currentCircle.name"></el-avatar>
              <h3 style="margin-top: 10px;">{{ currentCircle.name }}</h3>
              <p style="color: #909399;">{{ currentCircle.description }}</p>
              
              <el-descriptions :column="1" border style="margin-top: 20px;">
                <el-descriptions-item label="创建人">
                  <el-avatar :size="24" :src="currentCircle.creatorAvatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
                  {{ currentCircle.creator }}
                </el-descriptions-item>
                <el-descriptions-item label="圈子类型">{{ currentCircle.type }}</el-descriptions-item>
                <el-descriptions-item label="隐私设置">{{ getPrivacyText(currentCircle.privacy) }}</el-descriptions-item>
                <el-descriptions-item label="成员数量">{{ currentCircle.memberCount }}人</el-descriptions-item>
                <el-descriptions-item label="帖子数量">{{ currentCircle.postCount }}篇</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(currentCircle.createTime) }}</el-descriptions-item>
                <el-descriptions-item label="审核时间">{{ currentCircle.auditTime ? formatDate(currentCircle.auditTime) : '未审核' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
          
          <el-col :span="16">
            <el-tabs type="border-card">
              <el-tab-pane label="圈子介绍">
                <div style="padding: 20px;">
                  <h4>圈子介绍</h4>
                  <p>{{ currentCircle.introduction || '暂无详细介绍' }}</p>
                  
                  <h4 style="margin-top: 20px;">圈子规则</h4>
                  <p>{{ currentCircle.rules || '暂无规则说明' }}</p>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="最新帖子">
                <div style="padding: 20px;">
                  <el-table :data="currentCircle.recentPosts" style="width: 100%">
                    <el-table-column prop="title" label="帖子标题" min-width="200"></el-table-column>
                    <el-table-column prop="author" label="作者" width="100"></el-table-column>
                    <el-table-column prop="createTime" label="发布时间" width="120">
                      <template slot-scope="scope">
                        {{ formatDate(scope.row.createTime) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="viewCount" label="浏览数" width="80" align="center"></el-table-column>
                  </el-table>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="成员列表">
                <div style="padding: 20px;">
                  <el-table :data="currentCircle.members" style="width: 100%">
                    <el-table-column prop="avatar" label="头像" width="80" align="center">
                      <template slot-scope="scope">
                        <el-avatar :size="32" :src="scope.row.avatar"></el-avatar>
                      </template>
                    </el-table-column>
                    <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
                    <el-table-column prop="role" label="角色" width="100">
                      <template slot-scope="scope">
                        <el-tag :type="scope.row.role === 'creator' ? 'danger' : scope.row.role === 'admin' ? 'warning' : 'success'" size="small">
                          {{ scope.row.role === 'creator' ? '创建者' : scope.row.role === 'admin' ? '管理员' : '成员' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="joinTime" label="加入时间" width="120">
                      <template slot-scope="scope">
                        {{ formatDate(scope.row.joinTime) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-col>
        </el-row>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'pending'" type="primary" @click="handleApprove(currentCircle)">通过审核</el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'pending'" type="danger" @click="handleReject(currentCircle)">拒绝审核</el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'approved'" type="warning" @click="handleToggleRecommend(currentCircle)">
          {{ currentCircle.isRecommended ? '取消推荐' : '推荐' }}
        </el-button>
      </span>
    </el-dialog>

    <!-- 审核拒绝对话框 -->
    <el-dialog
      title="拒绝审核"
      :visible.sync="rejectDialogVisible"
      width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input
            type="textarea"
            :rows="3"
            v-model="rejectForm.reason"
            placeholder="请输入拒绝原因（必填）">
          </el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :disabled="!rejectForm.reason.trim()">确认拒绝</el-button>
      </span>
    </el-dialog>

    <!-- 成员审核对话框 -->
    <el-dialog
      :title="memberAuditCircle ? memberAuditCircle.name + ' - 成员审核' : '成员审核'"
      :visible.sync="memberAuditDialogVisible"
      width="80%"
      :before-close="handleCloseMemberAudit">
      <div v-if="memberAuditCircle" class="member-audit">
        <el-tabs v-model="memberAuditTab">
          <el-tab-pane label="待审核成员" name="pending">
            <el-table :data="pendingMembers" style="width: 100%">
              <el-table-column prop="avatar" label="头像" width="80" align="center">
                <template slot-scope="scope">
                  <el-avatar :size="40" :src="scope.row.avatar"></el-avatar>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
              <el-table-column prop="applyTime" label="申请时间" width="160">
                <template slot-scope="scope">
                  {{ formatDate(scope.row.applyTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="message" label="申请消息" min-width="200">
                <template slot-scope="scope">
                  {{ scope.row.message || '无附加消息' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" align="center">
                <template slot-scope="scope">
                  <el-button size="mini" type="success" @click="handleApproveMember(scope.row)">同意</el-button>
                  <el-button size="mini" type="danger" @click="handleRejectMember(scope.row)">拒绝</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="已审核成员" name="audited">
            <el-table :data="auditedMembers" style="width: 100%">
              <el-table-column prop="avatar" label="头像" width="80" align="center">
                <template slot-scope="scope">
                  <el-avatar :size="40" :src="scope.row.avatar"></el-avatar>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
              <el-table-column prop="applyTime" label="申请时间" width="160">
                <template slot-scope="scope">
                  {{ formatDate(scope.row.applyTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="auditTime" label="审核时间" width="160">
                <template slot-scope="scope">
                  {{ formatDate(scope.row.auditTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status === 'approved' ? 'success' : 'danger'" size="small">
                    {{ scope.row.status === 'approved' ? '已通过' : '已拒绝' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="memberAuditDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'CircleAudit',
  layout: 'manage',
  data() {
    return {
      // 筛选表单
      filterForm: {
        status: '',
        type: '',
        privacy: '',
        dateRange: [],
        keyword: ''
      },
      
      // 圈子列表数据
      circleList: [],
      
      // 选中的圈子
      selectedCircles: [],
      
      // 加载状态
      loading: false,
      
      // 分页信息
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      
      // 对话框显示状态
      detailDialogVisible: false,
      rejectDialogVisible: false,
      memberAuditDialogVisible: false,
      
      // 当前操作的圈子
      currentCircle: null,
      memberAuditCircle: null,
      
      // 拒绝表单
      rejectForm: {
        reason: ''
      },
      
      // 成员审核相关
      memberAuditTab: 'pending',
      pendingMembers: [],
      auditedMembers: []
    }
  },
  
  mounted() {
    this.loadCircleList()
  },
  
  methods: {
    // 加载圈子列表
    async loadCircleList() {
      this.loading = true
      try {
        // TODO: 调用后端接口获取圈子列表
        // const response = await this.$axios.get('/api/circle/audit/list', {
        //   params: {
        //     ...this.filterForm,
        //     page: this.pagination.currentPage,
        //     size: this.pagination.pageSize
        //   }
        // })
        // this.circleList = response.data.list
        // this.pagination.total = response.data.total
        
        // 模拟数据
        this.circleList = [
          {
            id: 1,
            name: '前端技术交流圈',
            avatar: '',
            description: '前端开发技术交流与分享',
            creator: '张三',
            creatorAvatar: '',
            type: '技术交流',
            privacy: 'public',
            memberCount: 156,
            postCount: 89,
            createTime: new Date('2024-01-15'),
            auditTime: null,
            status: 'pending',
            isRecommended: false,
            introduction: '专注于前端开发技术交流，包括Vue、React、Angular等框架',
            rules: '禁止发布广告，文明交流',
            recentPosts: [
              { title: 'Vue3新特性详解', author: '李四', createTime: new Date('2024-01-20'), viewCount: 120 },
              { title: 'React Hooks最佳实践', author: '王五', createTime: new Date('2024-01-18'), viewCount: 89 }
            ],
            members: [
              { avatar: '', nickname: '张三', role: 'creator', joinTime: new Date('2024-01-15') },
              { avatar: '', nickname: '李四', role: 'admin', joinTime: new Date('2024-01-16') }
            ]
          },
          {
            id: 2,
            name: '摄影爱好者',
            avatar: '',
            description: '摄影技巧分享与作品展示',
            creator: '李四',
            creatorAvatar: '',
            type: '兴趣爱好',
            privacy: 'approval',
            memberCount: 89,
            postCount: 45,
            createTime: new Date('2024-01-10'),
            auditTime: new Date('2024-01-12'),
            status: 'approved',
            isRecommended: true,
            introduction: '摄影爱好者聚集地，分享摄影技巧和作品',
            rules: '原创作品，禁止盗图',
            recentPosts: [
              { title: '夜景拍摄技巧', author: '赵六', createTime: new Date('2024-01-19'), viewCount: 67 }
            ],
            members: [
              { avatar: '', nickname: '李四', role: 'creator', joinTime: new Date('2024-01-10') }
            ]
          }
        ]
        this.pagination.total = 2
      } catch (error) {
        console.error('加载圈子列表失败:', error)
        this.$message.error('加载圈子列表失败')
      } finally {
        this.loading = false
      }
    },
    
    // 处理选择变化
    handleSelectionChange(selection) {
      this.selectedCircles = selection
    },
    
    // 搜索处理
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadCircleList()
    },
    
    // 刷新数据
    refreshData() {
      this.pagination.currentPage = 1
      this.loadCircleList()
    },
    
    // 分页大小变化
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.loadCircleList()
    },
    
    // 当前页变化
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadCircleList()
    },
    
    // 查看圈子详情
    handleView(circle) {
      this.currentCircle = circle
      this.detailDialogVisible = true
    },
    
    // 关闭详情对话框
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.currentCircle = null
    },
    
    // 通过审核
    async handleApprove(circle) {
      try {
        // TODO: 调用后端接口通过审核
        // await this.$axios.post(`/api/circle/audit/approve/${circle.id}`)
        
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('审核通过失败:', error)
        this.$message.error('审核通过失败')
      }
    },
    
    // 拒绝审核
    handleReject(circle) {
      this.currentCircle = circle
      this.rejectForm.reason = ''
      this.rejectDialogVisible = true
    },
    
    // 确认拒绝
    async confirmReject() {
      try {
        // TODO: 调用后端接口拒绝审核
        // await this.$axios.post(`/api/circle/audit/reject/${this.currentCircle.id}`, {
        //   reason: this.rejectForm.reason
        // })
        
        this.$message.success('审核拒绝成功')
        this.rejectDialogVisible = false
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('审核拒绝失败:', error)
        this.$message.error('审核拒绝失败')
      }
    },
    
    // 批量通过
    async handleBatchApprove() {
      try {
        const circleIds = this.selectedCircles.map(c => c.id)
        // TODO: 调用后端接口批量通过
        // await this.$axios.post('/api/circle/audit/batch-approve', {
        //   ids: circleIds
        // })
        
        this.$message.success(`批量通过 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量通过失败:', error)
        this.$message.error('批量通过失败')
      }
    },
    
    // 批量拒绝
    async handleBatchReject() {
      try {
        const circleIds = this.selectedCircles.map(c => c.id)
        // TODO: 调用后端接口批量拒绝
        // await this.$axios.post('/api/circle/audit/batch-reject', {
        //   ids: circleIds,
        //   reason: '批量拒绝'
        // })
        
        this.$message.success(`批量拒绝 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量拒绝失败:', error)
        this.$message.error('批量拒绝失败')
      }
    },
    
    // 推荐圈子
    async handleToggleRecommend(circle) {
      try {
        // TODO: 调用后端接口推荐/取消推荐
        // await this.$axios.post(`/api/circle/audit/toggle-recommend/${circle.id}`)
        
        this.$message.success(circle.isRecommended ? '取消推荐成功' : '推荐成功')
        this.refreshData()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      }
    },
    
    // 批量推荐
    async handleBatchRecommend() {
      try {
        const circleIds = this.selectedCircles.filter(c => c.status === 'approved' && !c.isRecommended).map(c => c.id)
        // TODO: 调用后端接口批量推荐
        // await this.$axios.post('/api/circle/audit/batch-recommend', {
        //   ids: circleIds
        // })
        
        this.$message.success(`批量推荐 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量推荐失败:', error)
        this.$message.error('批量推荐失败')
      }
    },
    
    // 批量取消推荐
    async handleBatchCancelRecommend() {
      try {
        const circleIds = this.selectedCircles.filter(c => c.isRecommended).map(c => c.id)
        // TODO: 调用后端接口批量取消推荐
        // await this.$axios.post('/api/circle/audit/batch-cancel-recommend', {
        //   ids: circleIds
        // })
        
        this.$message.success(`批量取消推荐 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        console.error('批量取消推荐失败:', error)
        this.$message.error('批量取消推荐失败')
      }
    },
    
    // 关闭圈子
    async handleCloseCircle(circle) {
      try {
        await this.$confirm('确定要关闭这个圈子吗？关闭后圈子将不可用，但数据会保留。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口关闭圈子
        // await this.$axios.post(`/api/circle/audit/close/${circle.id}`)
        
        this.$message.success('圈子关闭成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('关闭圈子失败:', error)
          this.$message.error('关闭圈子失败')
        }
      }
    },
    
    // 批量关闭圈子
    async handleBatchClose() {
      try {
        const circleIds = this.selectedCircles.map(c => c.id)
        await this.$confirm(`确定要批量关闭 ${circleIds.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口批量关闭
        // await this.$axios.post('/api/circle/audit/batch-close', {
        //   ids: circleIds
        // })
        
        this.$message.success(`批量关闭 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量关闭失败:', error)
          this.$message.error('批量关闭失败')
        }
      }
    },
    
    // 成员审核
    async handleMemberAudit(circle) {
      this.memberAuditCircle = circle
      this.memberAuditTab = 'pending'
      
      // TODO: 调用后端接口获取成员审核列表
      // const response = await this.$axios.get(`/api/circle/audit/member/${circle.id}`)
      // this.pendingMembers = response.data.pending
      // this.auditedMembers = response.data.audited
      
      // 模拟数据
      this.pendingMembers = [
        { id: 1, avatar: '', nickname: '王五', applyTime: new Date('2024-01-21'), message: '想加入技术交流' },
        { id: 2, avatar: '', nickname: '赵六', applyTime: new Date('2024-01-22'), message: '' }
      ]
      this.auditedMembers = [
        { id: 3, avatar: '', nickname: '钱七', applyTime: new Date('2024-01-18'), auditTime: new Date('2024-01-19'), status: 'approved' }
      ]
      
      this.memberAuditDialogVisible = true
    },
    
    // 关闭成员审核对话框
    handleCloseMemberAudit() {
      this.memberAuditDialogVisible = false
      this.memberAuditCircle = null
      this.pendingMembers = []
      this.auditedMembers = []
    },
    
    // 同意成员加入
    async handleApproveMember(member) {
      try {
        // TODO: 调用后端接口同意成员加入
        // await this.$axios.post(`/api/circle/audit/member/approve/${member.id}`)
        
        this.$message.success('成员加入申请已同意')
        this.handleMemberAudit(this.memberAuditCircle) // 重新加载数据
      } catch (error) {
        console.error('同意成员加入失败:', error)
        this.$message.error('同意成员加入失败')
      }
    },
    
    // 拒绝成员加入
    async handleRejectMember(member) {
      try {
        // TODO: 调用后端接口拒绝成员加入
        // await this.$axios.post(`/api/circle/audit/member/reject/${member.id}`)
        
        this.$message.success('成员加入申请已拒绝')
        this.handleMemberAudit(this.memberAuditCircle) // 重新加载数据
      } catch (error) {
        console.error('拒绝成员加入失败:', error)
        this.$message.error('拒绝成员加入失败')
      }
    },
    
    // 格式化日期
    formatDate(date) {
      if (!date) return ''
      return new Date(date).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    // 获取状态类型
    getStatusType(status) {
      const typeMap = {
        pending: 'warning',
        approved: 'success',
        rejected: 'danger',
        closed: 'info'
      }
      return typeMap[status] || 'info'
    },
    
    // 获取状态文本
    getStatusText(status) {
      const textMap = {
        pending: '待审核',
        approved: '已通过',
        rejected: '已拒绝',
        closed: '已关闭'
      }
      return textMap[status] || status
    },
    
    // 获取类型类型
    getTypeType(type) {
      const typeMap = {
        '技术交流': 'primary',
        '学习讨论': 'success',
        '兴趣爱好': 'warning',
        '生活分享': 'info',
        '其他': 'info'
      }
      return typeMap[type] || 'info'
    },
    
    // 获取隐私设置文本
    getPrivacyText(privacy) {
      const textMap = {
        public: '公开',
        private: '私密',
        approval: '需要审核'
      }
      return textMap[privacy] || privacy
    }
  }
}
</script>

<style scoped>
.circle-audit {
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

.filter-card {
  margin-bottom: 20px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
}

.toolbar-right {
  margin-left: auto;
}

.table-card {
  margin-bottom: 20px;
}

.circle-name {
  display: flex;
  align-items: center;
}

.name-text {
  font-weight: 500;
  margin-right: 8px;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.circle-detail {
  max-height: 600px;
  overflow-y: auto;
}

.circle-basic-info {
  text-align: center;
  padding: 20px;
}

.member-audit {
  max-height: 500px;
  overflow-y: auto;
}
</style>