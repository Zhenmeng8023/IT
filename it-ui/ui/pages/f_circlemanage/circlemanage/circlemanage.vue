<template>
  <div class="circle-manage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>圈子管理</h1>
      <p>管理所有圈子，包括成员管理、帖子审核、数据统计等</p>
    </div>

    <!-- 筛选工具栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-toolbar">
        <div class="filter-left">
          <el-select v-model="filterForm.status" placeholder="圈子状态" clearable style="width: 120px" @change="handleSearch">
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已通过" value="approved"></el-option>
            <el-option label="已关闭" value="close"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
          </el-select>
          
          
          <el-select v-model="filterForm.privacy" placeholder="隐私状态" clearable style="width: 120px; margin-left: 10px" @change="handleSearch">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私密" value="private"></el-option>
          </el-select>

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

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #409EFF;">
              <i class="el-icon-user-solid"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalCircles }}</div>
              <div class="stat-label">圈子总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #67C23A;">
              <i class="el-icon-user"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalMembers }}</div>
              <div class="stat-label">成员总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #E6A23C;">
              <i class="el-icon-document"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalPosts }}</div>
              <div class="stat-label">帖子总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
    </el-row>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button v-permission="'btn:circle-manage:create'" type="primary" icon="el-icon-plus" @click="handleCreateCircle">创建圈子</el-button>
        <el-button v-permission="'btn:circle-audit:batch-approve'" type="success" icon="el-icon-check" @click="handleBatchApprove" :disabled="selectedCircles.length === 0">
          批量通过
        </el-button>
        <el-button v-permission="'btn:circle-audit:batch-close'" type="warning" icon="el-icon-close" @click="handleBatchClose" :disabled="selectedCircles.length === 0">
          批量关闭
        </el-button>
        <el-button v-permission="'btn:circle-audit:batch-delete'" type="danger" icon="el-icon-delete" @click="handleBatchDelete" :disabled="selectedCircles.length === 0">
          批量删除
        </el-button>
        <el-button icon="el-icon-refresh" @click="refreshData">刷新</el-button>
        <!-- <div v-permission="'btn:circle-manage:export'" class="toolbar-right">
          <el-button type="text" icon="el-icon-download">导出数据</el-button>
          <el-button type="text" icon="el-icon-setting">设置</el-button>
        </div> -->
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
        
        <el-table-column prop="name" label="圈子信息" min-width="200">
          <template slot-scope="scope">
            <div class="circle-info">
              <div class="circle-details">
                <div class="circle-name">
                  <span class="name-text">{{ scope.row.name }}</span>
                  <!-- <el-tag v-if="scope.row.isRecommended" size="mini" type="warning" style="margin-left: 5px">推荐</el-tag> -->
                  <el-tag v-if="scope.row.type === 'official'" size="mini" type="danger" style="margin-left: 5px">官方</el-tag>
                  <el-tag v-if="scope.row.type === 'private'" size="mini" type="info" style="margin-left: 5px">私密</el-tag>
                  <el-tag v-if="scope.row.type === 'public'" size="mini" type="success" style="margin-left: 5px">公开</el-tag>
                </div>
                <div class="circle-description">{{ scope.row.description }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="creator" label="创建人" width="120">
          <template slot-scope="scope">
            <el-avatar :size="24" :src="scope.row.creatorAvatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
            {{ scope.row.creator }}
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getTypeType(scope.row.type)" size="small">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="memberCount" label="成员" width="80" align="center">
          <template slot-scope="scope">
            <el-tooltip :content="'成员数: ' + scope.row.memberCount" placement="top">
              <span class="count-text">{{ scope.row.memberCount }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="postCount" label="帖子" width="80" align="center">
          <template slot-scope="scope">
            <el-tooltip :content="'帖子数: ' + scope.row.postCount" placement="top">
              <span class="count-text">{{ scope.row.postCount }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="privacy" label="可见性" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getVisibilityType(scope.row.privacy)" size="small">
              {{ getVisibilityText(scope.row.privacy) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="350" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button v-permission="'btn:circle-audit:view'"
              size="mini"
              type="text"
              icon="el-icon-view"
              @click="handleViewDetail(scope.row)">
              详情
            </el-button>
            
            <el-button v-permission="'btn:circle-audit:member-manage'"
              size="mini"
              type="text"
              icon="el-icon-user"
              @click="handleMemberManage(scope.row)"
              style="color: #409EFF;">
              成员
            </el-button>
            
            <el-button v-permission="'btn:circle-audit:post-manage'"
              size="mini"
              type="text"
              icon="el-icon-document"
              @click="handlePostManage(scope.row)"
              style="color: #67C23A;">
              帖子
            </el-button>
            
            <el-button v-permission="'btn:circle-audit:approve'"
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="handleApprove(scope.row)"
              style="color: #67C23A;">
              通过
            </el-button>
            
            <!-- <el-button v-permission="scope.row.isRecommended ? 'btn:circle-audit:cancel-recommend' : 'btn:circle-audit:recommend'"
              v-if="scope.row.status === 'normal'"
              size="mini"
              type="text"
              :icon="scope.row.isRecommended ? 'el-icon-star-off' : 'el-icon-star-on'"
              @click="handleToggleRecommend(scope.row)"
              :style="{color: scope.row.isRecommended ? '#E6A23C' : '#909399'}">
              {{ scope.row.isRecommended ? '取消推荐' : '推荐' }}
            </el-button> -->
            
            <el-button v-permission="'btn:circle-audit:close'"
              v-if="scope.row.status === 'normal'"
              size="mini"
              type="text"
              icon="el-icon-switch-button"
              @click="handleCloseCircle(scope.row)"
              style="color: #909399;">
              关闭
            </el-button>
            
            <el-button v-permission="'btn:circle-manage:edit'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditCircle(scope.row)"
              style="color: #409EFF;">
              编辑
            </el-button>
            
            <el-button v-permission="'btn:circle-manage:delete'"
              size="mini"
              type="text"
              icon="el-icon-delete"
              @click="handleDeleteCircle(scope.row)"
              style="color: #F56C6C;">
              删除
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
      width="80%"
      :before-close="handleCloseDetail">
      <div v-if="currentCircle" class="circle-detail">
        <el-tabs v-model="detailTab" type="border-card">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="basic-info">
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
                    <el-descriptions-item label="今日活跃">{{ currentCircle.todayActive }}人</el-descriptions-item>
                    <el-descriptions-item label="创建时间">{{ formatDate(currentCircle.createTime) }}</el-descriptions-item>
                    <el-descriptions-item label="圈子状态">
                      <el-tag :type="getStatusType(currentCircle.status)" size="small">
                        {{ getStatusText(currentCircle.status) }}
                      </el-tag>
                    </el-descriptions-item>
                  </el-descriptions>
                </div>
              </el-col>
              
              <el-col :span="16">
                <div style="padding: 20px;">
                  <h4>圈子介绍</h4>
                  <p>{{ currentCircle.introduction || '暂无详细介绍' }}</p>
                  
                  <h4 style="margin-top: 20px;">圈子规则</h4>
                  <p>{{ currentCircle.rules || '暂无规则说明' }}</p>
                  
                  <h4 style="margin-top: 20px;">数据统计</h4>
                  <el-row :gutter="20">
                    <el-col :span="8">
                      <div class="mini-stat">
                        <div class="mini-value">{{ currentCircle.memberCount }}</div>
                        <div class="mini-label">总成员</div>
                      </div>
                    </el-col>
                    <el-col :span="8">
                      <div class="mini-stat">
                        <div class="mini-value">{{ currentCircle.postCount }}</div>
                        <div class="mini-label">总帖子</div>
                      </div>
                    </el-col>
                    <el-col :span="8">
                      <div class="mini-stat">
                        <div class="mini-value">{{ currentCircle.todayActive }}</div>
                        <div class="mini-label">今日活跃</div>
                      </div>
                    </el-col>
                  </el-row>
                </div>
              </el-col>
            </el-row>
          </el-tab-pane>
          
          <!-- 成员管理 -->
          <el-tab-pane label="成员管理" name="members">
            <div class="member-manage">
              <h4>成员列表</h4>
              <el-table :data="memberList" style="width: 100%; margin-top: 15px;" v-loading="memberLoading">
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
                <el-table-column prop="joinTime" label="加入时间" width="160">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.joinTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="lastActive" label="最后活跃" width="160">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.lastActive) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150" align="center">
                  <template slot-scope="scope">
                    <el-button v-if="scope.row.role !== 'creator'" size="mini" type="text" @click="handleSetAdmin(scope.row)">设为管理员</el-button>
                    <el-button v-if="scope.row.role !== 'creator'" size="mini" type="text" style="color: #F56C6C;" @click="handleRemoveMember(scope.row)">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          
          <!-- 帖子管理 -->
          <el-tab-pane label="帖子管理" name="posts">
            <div class="post-manage">
              <h4>帖子列表</h4>
              <el-table :data="postList" style="width: 100%; margin-top: 15px;" v-loading="postLoading">
                <el-table-column prop="title" label="标题" min-width="200"></el-table-column>
                <el-table-column prop="author" label="作者" width="120"></el-table-column>
                <el-table-column prop="createTime" label="发布时间" width="160">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.createTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="viewCount" label="浏览数" width="80" align="center"></el-table-column>
                <el-table-column prop="commentCount" label="评论数" width="80" align="center"></el-table-column>
                <el-table-column prop="status" label="状态" width="100" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="scope.row.status === 'published' ? 'success' : 'warning'" size="small">
                      {{ scope.row.status === 'published' ? '已发布' : '待审核' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200" align="center">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" @click="handleViewPost(scope.row)">查看</el-button>
                    <el-button v-if="scope.row.status === 'pending'" size="mini" type="text" style="color: #67C23A;" @click="handleApprovePost(scope.row)">通过</el-button>
                    <el-button size="mini" type="text" style="color: #F56C6C;" @click="handleDeletePost(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'pending'" type="primary" @click="handleApprove(currentCircle)">通过审核</el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'normal'" type="warning" @click="handleToggleRecommend(currentCircle)">
          {{ currentCircle.isRecommended ? '取消推荐' : '推荐' }}
        </el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'normal'" type="danger" @click="handleCloseCircle(currentCircle)">关闭圈子</el-button>
      </span>
    </el-dialog>

    <!-- 创建/编辑圈子对话框 -->
    <el-dialog
      :title="circleDialogTitle"
      :visible.sync="circleDialogVisible"
      width="600px">
      <el-form :model="circleForm" :rules="circleRules" ref="circleForm" label-width="100px">
        <el-form-item label="圈子名称" prop="name">
          <el-input v-model="circleForm.name" placeholder="请输入圈子名称"></el-input>
        </el-form-item>
        
        <el-form-item label="圈子描述" prop="description">
          <el-input
            type="textarea"
            :rows="2"
            v-model="circleForm.description"
            placeholder="请输入圈子描述">
          </el-input>
        </el-form-item>
        
        <el-form-item label="隐私设置" prop="privacy">
          <el-radio-group v-model="circleForm.privacy">
            <el-radio label="public">公开</el-radio>
            <el-radio label="private">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="圈子头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload">
            <img v-if="circleForm.avatar" :src="circleForm.avatar" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="circleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCircle">确认</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'CircleManage',
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
      
      // 统计数据
      stats: {
        totalCircles: 0,
        totalMembers: 0,
        totalPosts: 0,
        todayActive: 0
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
      circleDialogVisible: false,
      
      // 当前操作的圈子
      currentCircle: null,
      
      // 详情页标签
      detailTab: 'basic',
      
      // 成员列表
      memberList: [],
      memberLoading: false,
      
      // 帖子列表
      postList: [],
      postLoading: false,
      
      // 圈子表单
      circleForm: {
        id: '',
        name: '',
        description: '',
        type: '',
        privacy: 'public',
        introduction: '',
        rules: '',
        avatar: ''
      },
      
      // 表单验证规则
      circleRules: {
        name: [
          { required: true, message: '请输入圈子名称', trigger: 'blur' },
          { min: 2, max: 20, message: '圈子名称长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入圈子描述', trigger: 'blur' },
          { min: 5, max: 100, message: '圈子描述长度在 5 到 100 个字符', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择圈子类型', trigger: 'change' }
        ],
        privacy: [
          { required: true, message: '请选择隐私设置', trigger: 'change' }
        ]
      },
      
      // 对话框标题
      circleDialogTitle: '创建圈子'
    }
  },
  
  mounted() {
    this.loadStats()
    this.loadCircleList()
  },
  
  methods: {
// 根据用户ID获取用户信息
async getUserInfo(userId) {
      try {
        // 这里假设有一个获取用户信息的API
        const response = await this.$axios.get(`/api/user/${userId}`)
        return {
          nickname: response.data.nickname || `用户${userId}`,
          avatar: response.data.avatar || ''
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        return {
          nickname: `用户${userId}`,
          avatar: ''
        }
      }
    },
   // 加载统计数据
  async loadStats() {
    try {
      console.log('请求统计数据...')
      const response = await this.$axios.get('/api/circle/manage/stats')
      console.log('统计数据响应:', response)
      console.log('响应类型:', typeof response)
      
      // 检查响应是否直接是统计数据对象
      if (typeof response === 'object' && response !== null) {
        // 检查是否包含统计字段
        const hasStats = 'totalCircles' in response || 
                        'totalPosts' in response || 
                        'totalMembers' in response || 
                        'todayActive' in response
        
        console.log('响应是否包含统计字段:', hasStats)
        
        if (hasStats) {
          // 后端直接返回了统计数据对象
          this.stats = response
          console.log('使用直接返回的统计数据:', this.stats)
        } else if (response.data) {
          // 响应是一个包含data属性的对象
          console.log('响应data属性:', response.data)
          console.log('响应data类型:', typeof response.data)
          
          if (typeof response.data === 'object' && response.data !== null) {
            // 检查data中是否包含统计字段
            const hasStatsInData = 'totalCircles' in response.data || 
                                'totalPosts' in response.data || 
                                'totalMembers' in response.data || 
                                'todayActive' in response.data
            
            if (hasStatsInData) {
              this.stats = response.data
              console.log('使用response.data中的统计数据:', this.stats)
            } else if (response.data.code === 200 && response.data.data) {
              // 如果后端返回了包装格式 { code: 200, message: "...", data: {...} }
              this.stats = response.data.data
              console.log('使用包装格式的统计数据:', this.stats)
            } else {
              console.error('Unexpected response format:', response)
              // 使用默认值
              this.stats = {
                totalCircles: 0,
                totalMembers: 0,
                totalPosts: 0,
                todayActive: 0
              }
              console.log('使用默认统计数据:', this.stats)
            }
          } else {
            console.error('Response data is not an object:', response.data)
            // 使用默认值
            this.stats = {
              totalCircles: 0,
              totalMembers: 0,
              totalPosts: 0,
              todayActive: 0
            }
            console.log('使用默认统计数据（非对象）:', this.stats)
          }
        } else {
          console.error('Response has no data property:', response)
          // 使用默认值
          this.stats = {
            totalCircles: 0,
            totalMembers: 0,
            totalPosts: 0,
            todayActive: 0
          }
          console.log('使用默认统计数据（无data属性）:', this.stats)
        }
      } else {
        console.error('Response is not an object:', response)
        // 使用默认值
        this.stats = {
          totalCircles: 0,
          totalMembers: 0,
          totalPosts: 0,
          todayActive: 0
        }
        console.log('使用默认统计数据（非对象响应）:', this.stats)
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
      // 发生异常时，设置默认值
      this.stats = {
        totalCircles: 0,
        totalMembers: 0,
        totalPosts: 0,
        todayActive: 0
      }
      console.log('使用默认统计数据（异常）:', this.stats)
      // 不显示错误消息，避免页面频繁报错
    }
  },
    
// 加载圈子列表
async loadCircleList() {
  this.loading = true
  try {
    console.log('请求圈子列表...')
    // 构建请求参数
    const params = {
      page: this.pagination.currentPage - 1, // 后端页码从0开始
      size: this.pagination.pageSize
    }
    
    // 添加筛选条件
    if (this.filterForm.status) {
      params.type = this.filterForm.status
    }
    if (this.filterForm.privacy) {
      params.visibility = this.filterForm.privacy
    }
    if (this.filterForm.keyword) {
      // 如果keyword是用于搜索名称或创建人，可能需要后端支持
      params.keyword = this.filterForm.keyword
    }
    
    const response = await this.$axios.get('/api/circle/manage/list', { params })
    console.log('圈子列表响应:', response)
    
    // 检查响应是否是分页格式的对象
    if (response && typeof response === 'object') {
      // 检查是否直接是分页格式的响应
      if (response.content && Array.isArray(response.content)) {
        // 后端直接返回了分页格式的响应
        this.circleList = response.content.map(circle => ({
          id: circle.id,
          name: circle.name,
          description: circle.description || '',
          type: circle.type || 'other',
          privacy: circle.visibility || 'public', // 后端字段是visibility
          status: circle.status || 'normal',
          memberCount: circle.memberCount || 0,
          postCount: circle.postCount || 0,
          todayActive: circle.activeMemberCount || 0, // 使用activeMemberCount作为今日活跃
          createTime: circle.createdAt,
          creator: circle.creator?.username || '未知用户', // 从嵌套的creator对象中获取用户名
          creatorAvatar: circle.creator?.avatar || '', // 从嵌套的creator对象中获取头像
          isRecommended: circle.recommended || false,
          introduction: circle.introduction || '', // 尝试获取圈子介绍
          rules: circle.rules || '' // 尝试获取圈子规则
        }))
        this.pagination.total = response.totalElements || response.total || 0
        this.pagination.currentPage = response.number + 1 // 后端页码从0开始，前端从1开始
        this.pagination.pageSize = response.size
        console.log('使用直接返回的分页格式圈子列表数据:', this.circleList)
      } else if (response.data && typeof response.data === 'object' && response.data.content && Array.isArray(response.data.content)) {
        // 响应是一个包含data属性的对象，且data是分页格式
        this.circleList = response.data.content.map(circle => ({
          id: circle.id,
          name: circle.name,
          description: circle.description || '',
          type: circle.type || 'other',
          privacy: circle.visibility || 'public', // 后端字段是visibility
          status: circle.status || 'normal',
          memberCount: circle.memberCount || 0,
          postCount: circle.postCount || 0,
          todayActive: circle.activeMemberCount || 0, // 使用activeMemberCount作为今日活跃
          createTime: circle.createdAt,
          creator: circle.creator?.username || '未知用户', // 从嵌套的creator对象中获取用户名
          creatorAvatar: circle.creator?.avatar || '', // 从嵌套的creator对象中获取头像
          isRecommended: circle.recommended || false,
          introduction: circle.introduction || '', // 尝试获取圈子介绍
          rules: circle.rules || '' // 尝试获取圈子规则
        }))
        this.pagination.total = response.data.totalElements || response.data.total || 0
        this.pagination.currentPage = response.data.number + 1 // 后端页码从0开始，前端从1开始
        this.pagination.pageSize = response.data.size
        console.log('使用response.data中的分页格式圈子列表数据:', this.circleList)
      } else {
        console.error('Unexpected response format:', response)
        // 即使后端返回错误，也要设置空数组，避免页面显示异常
        this.circleList = []
        this.pagination.total = 0
      }
    } else {
      console.error('Response is not an object:', response)
      // 即使后端返回错误，也要设置空数组，避免页面显示异常
      this.circleList = []
      this.pagination.total = 0
    }
  } catch (error) {
    console.error('加载圈子列表失败:', error)
    // 发生异常时，设置空数组
    this.circleList = []
    this.pagination.total = 0
    // 不显示错误消息，避免页面频繁报错
  } finally {
    this.loading = false
  }
},
    
    // 加载成员列表
    async loadMemberList(circleId) {
      this.memberLoading = true
      try {
        // TODO: 调用后端接口获取成员列表
        // const response = await this.$axios.get(`/api/circle/manage/members/${circleId}`)
        // this.memberList = response.data
        
        // 模拟数据
        this.memberList = [
          {
            id: 1,
            avatar: '',
            nickname: '张三',
            role: 'creator',
            joinTime: new Date('2024-01-15'),
            lastActive: new Date('2024-01-22')
          },
          {
            id: 2,
            avatar: '',
            nickname: '李四',
            role: 'admin',
            joinTime: new Date('2024-01-16'),
            lastActive: new Date('2024-01-21')
          },
          {
            id: 3,
            avatar: '',
            nickname: '王五',
            role: 'member',
            joinTime: new Date('2024-01-18'),
            lastActive: new Date('2024-01-20')
          }
        ]
      } catch (error) {
        console.error('加载成员列表失败:', error)
        this.$message.error('加载成员列表失败')
      } finally {
        this.memberLoading = false
      }
    },
    
    // 加载帖子列表
    async loadPostList(circleId) {
      this.postLoading = true
      try {
        // TODO: 调用后端接口获取帖子列表
        // const response = await this.$axios.get(`/api/circle/manage/posts/${circleId}`)
        // this.postList = response.data
        
        // 模拟数据
        this.postList = [
          {
            id: 1,
            title: 'Vue3新特性详解',
            author: '张三',
            createTime: new Date('2024-01-20'),
            viewCount: 120,
            commentCount: 15,
            status: 'published'
          },
          {
            id: 2,
            title: 'React Hooks最佳实践',
            author: '李四',
            createTime: new Date('2024-01-18'),
            viewCount: 89,
            commentCount: 8,
            status: 'published'
          },
          {
            id: 3,
            title: '新帖子待审核',
            author: '王五',
            createTime: new Date('2024-01-22'),
            viewCount: 0,
            commentCount: 0,
            status: 'pending'
          }
        ]
      } catch (error) {
        console.error('加载帖子列表失败:', error)
        this.$message.error('加载帖子列表失败')
      } finally {
        this.postLoading = false
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
    
    // 获取可见性类型对应的标签类型
    getVisibilityType(visibility) {
      switch (visibility) {
        case 'public':
          return 'success'
        case 'private':
          return 'info'
        default:
          return 'default'
      }
    },
    
    // 获取可见性类型对应的文本
    getVisibilityText(visibility) {
      switch (visibility) {
        case 'public':
          return '公开'
        case 'private':
          return '私密'
        default:
          return '未知'
      }
    },
    
    // 刷新数据
    refreshData() {
      this.pagination.currentPage = 1
      this.loadStats()
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
    handleViewDetail(circle) {
      this.currentCircle = circle
      this.detailTab = 'basic'
      this.detailDialogVisible = true
      this.loadMemberList(circle.id)
      this.loadPostList(circle.id)
    },
    
    // 关闭详情对话框
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.currentCircle = null
      this.memberList = []
      this.postList = []
    },
    
    // 成员管理
    handleMemberManage(circle) {
      this.currentCircle = circle
      this.detailTab = 'members'
      this.detailDialogVisible = true
      this.loadMemberList(circle.id)
    },
    
    // 帖子管理
    handlePostManage(circle) {
      this.currentCircle = circle
      this.detailTab = 'posts'
      this.detailDialogVisible = true
      this.loadPostList(circle.id)
    },
    
    // 创建圈子
async handleConfirmCircle() {
  try {
    this.$refs.circleForm.validate(async (valid) => {
      if (valid) {
        if (this.circleForm.id) {
          // 编辑现有圈子
          await this.$axios.put(`/api/circle/manage/${this.circleForm.id}`, this.circleForm)
          this.$message.success('圈子更新成功')
        } else {
          // 创建新圈子 - 修正接口路径
          await this.$axios.post('/api/circle/manage', this.circleForm)
          this.$message.success('圈子创建成功')
        }
        
        this.circleDialogVisible = false
        this.refreshData()
      }
    })
  } catch (error) {
    console.error('圈子操作失败:', error)
    this.$message.error('圈子操作失败：' + (error.response?.data?.message || error.message || '未知错误'))
  }
},
    // 编辑圈子
    handleEditCircle(circle) {
      this.circleForm = {
        id: circle.id,
        name: circle.name,
        description: circle.description,
        type: circle.type,
        privacy: circle.privacy,
        introduction: circle.introduction || '',
        rules: circle.rules || '',
        avatar: circle.avatar || ''
      }
      this.circleDialogTitle = '编辑圈子'
      this.circleDialogVisible = true
    },
    
     // 确认圈子操作
    async handleConfirmCircle() {
      try {
        this.$refs.circleForm.validate(async (valid) => {
          if (valid) {
            if (this.circleForm.id) {
              // 编辑现有圈子 - PUT 请求
              await this.$axios.put(`/api/circle/manage/${this.circleForm.id}`, this.circleForm)
              this.$message.success('圈子更新成功')
            } else {
              // 创建新圈子 - POST 请求到 /api/circle
              await this.$axios.post('/api/circle', this.circleForm, {
                headers: {
                  'Content-Type': 'application/json'
                }
              })
              this.$message.success('圈子创建成功')
            }
            
            this.circleDialogVisible = false
            this.refreshData()
          }
        })
      } catch (error) {
        console.error('圈子操作失败:', error)
        this.$message.error('圈子操作失败：' + (error.response?.data?.message || error.message || '未知错误'))
      }
    },
    
    // 头像上传成功
    handleAvatarSuccess(res, file) {
      this.circleForm.avatar = URL.createObjectURL(file.raw)
    },
    
    // 头像上传前验证
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg'
      const isPNG = file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG && !isPNG) {
        this.$message.error('头像只能是 JPG 或 PNG 格式!')
      }
      if (!isLt2M) {
        this.$message.error('头像大小不能超过 2MB!')
      }
      return (isJPG || isPNG) && isLt2M
    },
    
    // 通过审核
    async handleApprove(circle) {
      try {
        // TODO: 调用后端接口通过审核
        await this.$axios.post(`/api/circle/manage/approve/${circle.id}`)
        
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        console.error('审核通过失败:', error)
        this.$message.error('审核通过失败')
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
        await this.$axios.post(`/api/circle/manage/close/${circle.id}`)
        
        this.$message.success('圈子关闭成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('关闭圈子失败:', error)
          this.$message.error('关闭圈子失败')
        }
      }
    },
    
    // 删除圈子
    async handleDeleteCircle(circle) {
      try {
        await this.$confirm('确定要删除这个圈子吗？此操作不可恢复！', '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        
        // TODO: 调用后端接口删除圈子
        await this.$axios.delete(`/api/circle/manage/delete/${circle.id}`)
        
        this.$message.success('圈子删除成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除圈子失败:', error)
          this.$message.error('删除圈子失败')
        }
      }
    },
    
    // 设为管理员
    async handleSetAdmin(member) {
      try {
        await this.$confirm(`确定要将成员 ${member.nickname} 设为管理员吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口设为管理员
        await this.$axios.post(`/api/circle/manage/set-admin/${member.id}`)
        
        this.$message.success('设置管理员成功')
        this.loadMemberList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('设置管理员失败:', error)
          this.$message.error('设置管理员失败')
        }
      }
    },
    
    // 移除成员
    async handleRemoveMember(member) {
      try {
        await this.$confirm(`确定要将成员 ${member.nickname} 从圈子中移除吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口移除成员
        await this.$axios.post(`/api/circle/manage/remove-member/${member.id}`)
        
        this.$message.success('成员移除成功')
        this.loadMemberList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('移除成员失败:', error)
          this.$message.error('移除成员失败')
        }
      }
    },
    
    // 查看帖子
    handleViewPost(post) {
      // TODO: 跳转到帖子详情页面
      this.$message.info(`查看帖子 ${post.title}`)
    },
    
    // 通过帖子
    async handleApprovePost(post) {
      try {
        // TODO: 调用后端接口通过帖子
        await this.$axios.post(`/api/circle/manage/approve-post/${post.id}`)
        
        this.$message.success('帖子审核通过成功')
        this.loadPostList(this.currentCircle.id)
      } catch (error) {
        console.error('帖子审核通过失败:', error)
        this.$message.error('帖子审核通过失败')
      }
    },
    
    // 删除帖子
    async handleDeletePost(post) {
      try {
        await this.$confirm(`确定要删除帖子 "${post.title}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // TODO: 调用后端接口删除帖子
        await this.$axios.delete(`/api/circle/manage/delete-post/${post.id}`)
        
        this.$message.success('帖子删除成功')
        this.loadPostList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除帖子失败:', error)
          this.$message.error('删除帖子失败')
        }
      }
    },
    
    // 批量通过
    async handleBatchApprove() {
      try {
        const circleIds = this.selectedCircles.map(c => c.id)
        await this.$confirm(`确定要批量通过 ${circleIds.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        //TODO: 调用后端接口批量通过
        await this.$axios.post('/api/circle/manage/batch-approve', {
          ids: circleIds
        })
        
        this.$message.success(`批量通过 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量通过失败:', error)
          this.$message.error('批量通过失败')
        }
      }
    },
    
    // 批量关闭
    async handleBatchClose() {
      try {
        const circleIds = this.selectedCircles.map(c => c.id)
        await this.$confirm(`确定要批量关闭 ${circleIds.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        //TODO: 调用后端接口批量关闭
        await this.$axios.post('/api/circle/manage/batch-close', {
          ids: circleIds
        })
        
        this.$message.success(`批量关闭 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量关闭失败:', error)
          this.$message.error('批量关闭失败')
        }
      }
    },
    
    // 批量删除
    async handleBatchDelete() {
      try {
        const circleIds = this.selectedCircles.map(c => c.id)
        await this.$confirm(`确定要批量删除 ${circleIds.length} 个圈子吗？此操作不可恢复！`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        
         // 发送纯JSON数组格式的请求体
         const response = await this.$axios.post('/api/circle/manage/batch-delete', circleIds, {
          headers: {
            'Content-Type': 'application/json'
          }
        })

        this.$message.success(`批量删除 ${circleIds.length} 个圈子成功`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除失败:', error)
          this.$message.error('批量删除失败')
        }
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
        normal: 'success',
        pending: 'warning',
        closed: 'info',
        violation: 'danger'
      }
      return typeMap[status] || 'info'
    },
    
    // 获取状态文本
    getStatusText(status) {
      const textMap = {
        normal: '正常',
        pending: '待审核',
        closed: '已关闭',
        violation: '违规'
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
.circle-manage {
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin-right: 15px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.toolbar-card {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.toolbar .el-button {
  flex: 1;
  margin: 0 5px;
  text-align: center;
}

.toolbar-right {
  margin-left: auto;
}

.table-card {
  margin-bottom: 20px;
}

.circle-info {
  display: flex;
  align-items: center;
}

.circle-details {
  flex: 1;
}

.circle-name {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
}

.name-text {
  font-weight: 500;
  margin-right: 8px;
}

.circle-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.count-text {
  font-weight: 500;
  color: #409EFF;
}

.active-text {
  font-weight: 500;
  color: #67C23A;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.circle-detail {
  max-height: 600px;
  overflow-y: auto;
}

.basic-info {
  text-align: center;
  padding: 20px;
}

.mini-stat {
  text-align: center;
  padding: 15px;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
}

.mini-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.mini-label {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.member-manage,
.post-manage,
.circle-statistics {
  padding: 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 15px;
  text-align: center;
}

.chart-placeholder {
  height: 200px;
  background-color: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100px;
  height: 100px;
}

.avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
}
</style>