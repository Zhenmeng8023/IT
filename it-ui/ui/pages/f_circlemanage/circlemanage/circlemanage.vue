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
            @input="handleInput">
            <template slot="append">
              <el-button @click="handleSearch" icon="el-icon-search"></el-button>
            </template>
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
              size="mini"
              type="text"
              icon="el-icon-close"
              @click="handleCloseCircle(scope.row)"
              style="color: #F56C6C;">
              关闭
            </el-button>
            
            <el-button
              size="mini"
              type="text"
              icon="el-icon-close"
              @click="handleRejectCircle(scope.row)"
              style="color: #F56C6C;">
              拒绝
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
                  <h3 style="margin-top: 10px;">{{ currentCircle.name }}</h3>
                  <p style="color: #909399;">{{ currentCircle.description }}</p>
                  
                  <el-descriptions :column="1" border style="margin-top: 20px;">
                    <el-descriptions-item label="创建人">{{ creatorInfo ? creatorInfo.nickname || creatorInfo.username : '未知用户' }}
                </el-descriptions-item>
                    <el-descriptions-item label="圈子类型">{{ currentCircle.type }}</el-descriptions-item>
                    <el-descriptions-item label="隐私设置">{{ getPrivacyText(currentCircle.privacy) }}</el-descriptions-item>
                    <el-descriptions-item label="成员数量">{{ currentCircle.memberCount }}人</el-descriptions-item>
                    <el-descriptions-item label="帖子数量">{{ currentCircle.postCount }}篇</el-descriptions-item>
                    <el-descriptions-item label="今日活跃">{{ currentCircle.todayActive }}人</el-descriptions-item>
                    <el-descriptions-item label="创建时间">{{ formatDate(currentCircle.createTime) }}</el-descriptions-item>
                  </el-descriptions>
                </div>
              </el-col>
              
              <el-col :span="16">
                <div style="padding: 20px;">
                  <h4>圈子介绍</h4>
                  <p>{{ currentCircle.description || '暂无详细介绍' }}</p>
                  
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
          <el-tab-pane label="成员数据" name="members">
            <div class="member-manage">
              <h4>成员列表</h4>
              <el-table :data="this.memberList" style="width: 100%; margin-top: 15px;" v-loading="memberLoading">
                <el-table-column prop="avatarUrl" label="头像" width="80" align="center">
                  <template slot-scope="scope">
                    <el-avatar :size="32" :src="scope.row.avatarUrl"></el-avatar>
                  </template>
                </el-table-column>
                <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
                <el-table-column prop="joinTime" label="加入时间" width="160">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.joinTime) }}
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
                <el-table-column label="用户头像" width="80" align="center"> 
                  <template slot-scope="scope"> 
                    <el-avatar :size="40" :src="scope.row.author?.avatarUrl" :alt="scope.row.author?.username || '未知用户'">
                      {{ (scope.row.author?.username || '未知用户').charAt(0) }}
                    </el-avatar> 
                  </template> 
                </el-table-column>
                <el-table-column label="作者" width="120">
                <template slot-scope="scope">
                  {{ scope.row.author?.username || scope.row.author?.nickname || '未知用户' }}
                </template>
                </el-table-column>
                <el-table-column prop="createTime" label="发布时间" width="160">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200" align="center">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" @click="handleViewPost(scope.row)">查看</el-button>
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

    <!-- 帖子详情对话框 -->
    <el-dialog
      title="帖子详情"
      :visible.sync="postDetailDialogVisible"
      width="800px">
      <div v-if="currentPost" class="post-detail">
        <div class="post-meta">
          <span>作者: {{ currentPost.author?.nickname || currentPost.author?.username || '未知用户' }}</span>
          <span>发布时间: {{ formatDate(currentPost.createTime || currentPost.createdAt) }}</span>
        </div>
        <div class="post-content" v-html="currentPost.content || '无内容'">
        </div>
      </div>
      <div v-else class="loading">
        加载中...
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="postDetailDialogVisible = false">关闭</el-button>
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
        
        <el-form-item label="可见性" prop="visibility">
          <el-radio-group v-model="circleForm.visibility">
            <el-radio label="public">公开</el-radio>
            <el-radio label="private">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="最大成员数" prop="maxMembers">
          <el-input-number v-model="circleForm.maxMembers" :min="1" :max="10000" placeholder="请输入最大成员数"></el-input-number>
        </el-form-item>
        
        <!-- <el-form-item label="圈子头像">
          <el-upload
            class="avatar-uploader"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload">
            <img v-if="circleForm.avatar" :src="circleForm.avatar" class="avatar">
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item> -->
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="circleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCircle">确认</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { GetCircleMembers, GetCirclePosts, GetUserById, DeleteCircleComment, GetCircleCommentById, GetCircleByName } from '@/api'

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
      
      // 搜索防抖定时器
      searchTimer: null,
      
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
      // 创建人信息
      creatorInfo: null,
      
      // 详情页标签
      detailTab: 'basic',
      
      // 成员列表数据
      memberList: [],
      
      // 帖子详情对话框
      postDetailDialogVisible: false,
      currentPost: null,
      memberLoading: false,
      
      // 帖子列表
      postList: [],
      postLoading: false,
      
      // 圈子表单
      circleForm: {
        id: '',
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: null,
        avatar: ''
      },
      
      // 表单验证规则
      circleRules: {
        name: [
          { required: true, message: '请输入圈子名称', trigger: 'blur' },
          { max: 100, message: '圈子名称长度不能超过 100 个字符', trigger: 'blur' }
        ],
        description: [
          { max: 1000, message: '圈子描述长度不能超过 1000 个字符', trigger: 'blur' }
        ],
        visibility: [
          { required: true, message: '请选择可见性', trigger: 'change' }
        ],
        maxMembers: [
          { type: 'number', min: 1, max: 10000, message: '最大成员数应在 1-10000 之间', trigger: 'blur' }
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
          type: circle.type || 'pending', // 状态字段，值为pending、approved、close、rejected
          privacy: circle.visibility || 'public', // 后端字段是visibility
          memberCount: circle.memberCount || 0,
          postCount: circle.postCount || 0,
          todayActive: circle.activeMemberCount || 0, // 使用activeMemberCount作为今日活跃
          createTime: circle.createdAt,
          creatorId: circle.creatorId || circle.creator?.id, // 从creatorId字段或creator对象中获取创建人ID
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
          type: circle.type || 'pending', // 状态字段，值为pending、approved、close、rejected
          privacy: circle.visibility || 'public', // 后端字段是visibility
          memberCount: circle.memberCount || 0,
          postCount: circle.postCount || 0,
          todayActive: circle.activeMemberCount || 0, // 使用activeMemberCount作为今日活跃
          createTime: circle.createdAt,
          creatorId: circle.creatorId || circle.creator?.id, // 从creatorId字段或creator对象中获取创建人ID
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
        // 调用后端接口获取成员列表
        const response = await GetCircleMembers(circleId)
        console.log('获取成员列表响应:', response)
        
        // 处理不同格式的响应
        if (Array.isArray(response)) {
          this.memberList = response
        } else if (response.data && Array.isArray(response.data)) {
          this.memberList = response.data
        } else if (response.data && response.data.list) {
          this.memberList = response.data.list
        } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
          this.memberList = response.data.data
        } else {
          console.error('未知的成员列表响应格式:', response)
          this.memberList = []
        }
      } catch (error) {
        console.error('加载成员列表失败:', error)
        this.$message.error('加载成员列表失败')
        this.memberList = []
      } finally {
        this.memberLoading = false
      }
    },
    
    // 加载帖子列表
    async loadPostList(circleId) {
      this.postLoading = true
      try {
        // 调用后端接口获取帖子列表
        const response = await GetCirclePosts(circleId)
        console.log('获取帖子列表响应:', response)
        
        // 处理不同格式的响应
        let rawPosts = []
        if (Array.isArray(response)) {
          rawPosts = response
        } else if (response.data && Array.isArray(response.data)) {
          rawPosts = response.data
        } else if (response.data && response.data.list) {
          rawPosts = response.data.list
        } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
          rawPosts = response.data.data
        } else {
          console.error('未知的帖子列表响应格式:', response)
          rawPosts = []
        }
        
        // 处理帖子数据，确保作者字段正确映射
        this.postList = rawPosts.map(post => {
          let authorInfo = null
          
          // 处理作者信息
          if (post.author) {
            if (typeof post.author === 'string') {
              try {
                // 尝试解析 JSON 字符串
                authorInfo = JSON.parse(post.author)
              } catch (e) {
                // 如果解析失败，将字符串作为用户名
                authorInfo = { username: post.author, nickname: post.author }
              }
            } else if (typeof post.author === 'object') {
              // 如果已经是对象，直接使用
              authorInfo = post.author
            }
          } else {
            // 使用其他可能的作者字段
            authorInfo = {
              username: post.username || post.creator || post.user?.username || '未知用户',
              nickname: post.nickname || post.user?.nickname || post.username || '未知用户',
              avatarUrl: post.avatarUrl || post.user?.avatarUrl || ''
            }
          }
          
          return {
            id: post.id || post.postId || post.commentId,
            author: authorInfo,
            createTime: post.createTime || post.createdAt || post.createDate,
            content: post.content || post.body || '',
            title: post.title || post.subject || '无标题',
            viewCount: post.viewCount || post.views || 0,
            commentCount: post.commentCount || post.replyCount || 0,
            status: post.status || 'published'
          }
        })
      } catch (error) {
        console.error('加载帖子列表失败:', error)
        this.$message.error('加载帖子列表失败')
        this.postList = []
      } finally {
        this.postLoading = false
      }
    },
    
    // 处理选择变化
    handleSelectionChange(selection) {
      this.selectedCircles = selection
    },
    
    // 搜索处理
    // 处理输入事件（添加防抖）
    handleInput() {
      // 清除之前的定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      
      // 设置新的定时器，300ms后执行搜索
      this.searchTimer = setTimeout(() => {
        this.handleSearch()
      }, 300)
    },
    
    // 执行搜索
    async handleSearch() {
      this.loading = true
      try {
        if (this.filterForm.keyword) {
          // 使用 GetCircleByName 函数搜索圈子
          const response = await GetCircleByName(this.filterForm.keyword)
          console.log('搜索圈子响应:', response)
          
          // 处理响应数据
          let circles = []
          
          // 检查响应是否是单个对象
          if (response && typeof response === 'object' && !Array.isArray(response) && response.id) {
            // 单个圈子对象
            circles = [response]
          } else if (response.data && typeof response.data === 'object' && !Array.isArray(response.data) && response.data.id) {
            // 响应在 data 中，是单个圈子对象
            circles = [response.data]
          } else if (response.data && response.data.data && typeof response.data.data === 'object' && !Array.isArray(response.data.data) && response.data.data.id) {
            // 响应在 data.data 中，是单个圈子对象
            circles = [response.data.data]
          } else if (Array.isArray(response)) {
            // 响应是数组
            circles = response
          } else if (response.data && Array.isArray(response.data)) {
            // 响应在 data 中，是数组
            circles = response.data
          } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
            // 响应在 data.data 中，是数组
            circles = response.data.data
          } else if (response.data && response.data.list) {
            // 响应在 data.list 中
            circles = response.data.list
          }
          
          // 格式化圈子数据
          this.circleList = circles.map(circle => ({
            id: circle.id,
            name: circle.name,
            description: circle.description || '',
            type: circle.type || 'pending',
            privacy: circle.visibility || 'public',
            memberCount: circle.memberCount || 0,
            postCount: circle.postCount || 0,
            todayActive: circle.activeMemberCount || 0,
            createTime: circle.createdAt || circle.createTime,
            creatorId: circle.creatorId || circle.creator?.id,
            creator: circle.creator?.username || '未知用户',
            creatorAvatar: circle.creator?.avatar || '',
            isRecommended: circle.recommended || false,
            introduction: circle.introduction || '',
            rules: circle.rules || ''
          }))
          
          this.pagination.total = this.circleList.length
          console.log('搜索结果:', this.circleList)
        } else {
          // 没有关键词时，加载所有圈子
          this.pagination.currentPage = 1
          await this.loadCircleList()
        }
      } catch (error) {
        console.error('搜索圈子失败:', error)
        this.$message.error('搜索圈子失败')
        // 搜索失败时，加载所有圈子
        await this.loadCircleList()
      } finally {
        this.loading = false
      }
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
    async handleViewDetail(circle) {
      this.currentCircle = circle
      this.detailTab = 'basic'
      this.detailDialogVisible = true
      this.loadMemberList(circle.id)
      this.loadPostList(circle.id)
      // 获取创建人信息
      this.loadCreatorInfo(circle.creatorId)
    },
    
    // 加载创建人信息
    async loadCreatorInfo(creatorId) {
      if (!creatorId) {
        this.creatorInfo = null
        return
      }
      try {
        const response = await GetUserById(creatorId)
        console.log('获取创建人信息响应:', response)
        
        if (response.data) {
          this.creatorInfo = response.data
        } else if (typeof response === 'object') {
          this.creatorInfo = response
        } else {
          this.creatorInfo = null
        }
      } catch (error) {
        console.error('获取创建人信息失败:', error)
        this.creatorInfo = null
      }
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
          // 创建新圈子 - 使用正确的接口路径
          await this.$axios.post('/api/circle', this.circleForm)
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
    // 创建圈子
    handleCreateCircle() {
      this.circleForm = {
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: null,
        avatar: ''
      }
      this.circleDialogTitle = '创建圈子'
      this.circleDialogVisible = true
    },
    
    // 编辑圈子
    handleEditCircle(circle) {
      this.circleForm = {
        id: circle.id,
        name: circle.name,
        description: circle.description,
        visibility: circle.visibility || circle.privacy || 'public',
        maxMembers: circle.maxMembers || null,
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
              // 只传递后端期望的字段，不传递 id 和 avatar
              const { id, avatar, ...updateData } = this.circleForm
              // 移除值为null或undefined的字段
              const filteredData = Object.entries(updateData).reduce((acc, [key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                  acc[key] = value
                }
                return acc
              }, {})
              console.log('更新圈子请求数据:', filteredData)
              await this.$axios.put(`/api/circle/${this.circleForm.id}`, filteredData)
              this.$message.success('圈子更新成功')
            } else {
              // 创建新圈子 - POST 请求到 /api/circle
              // 后端会自动获取创建者ID，所以不需要传递creatorId
              // 创建新圈子时不应该传递id字段
              // 只传递非空字段
              const { id, ...createData } = this.circleForm
              // 移除值为null或undefined的字段
              const filteredData = Object.entries(createData).reduce((acc, [key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                  acc[key] = value
                }
                return acc
              }, {})
              console.log('创建圈子请求数据:', filteredData)
              await this.$axios.post('/api/circle', filteredData, {
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
    
    // 通过审核
    async handleApprove(circle) {
      try {
        // 检查圈子状态是否为待审核
        if (circle.type !== 'pending') {
          this.$message.warning('只有待审核的圈子才能通过审核')
          return
        }
        
        await this.$confirm('确定要通过这个圈子的审核吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        })
        
        // 调用后端接口通过审核
        await this.$axios.put(`/api/circle/manage/approve/${circle.id}`)
        
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('审核通过失败:', error)
          this.$message.error('审核通过失败')
        }
      }
    },
    
    
    // 关闭圈子
    async handleCloseCircle(circle) {
      try {
        // 检查圈子状态是否为已通过
        if (circle.type !== 'approved') {
          this.$message.warning('只有已通过的圈子才能关闭')
          return
        }
        
        await this.$confirm('确定要关闭这个圈子吗？关闭后圈子将不可用，但数据会保留。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 调用后端接口关闭圈子
        await this.$axios.put(`/api/circle/${circle.id}/close`, {})
        
        this.$message.success('圈子关闭成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('关闭圈子失败:', error)
          this.$message.error('关闭圈子失败')
        }
      }
    },

    // 拒绝圈子审核
async handleRejectCircle(circle) {
  try {
    // 检查圈子状态是否为待审核
    if (circle.type !== 'pending') {
      this.$message.warning('只有待审核的圈子才能拒绝审核')
      return
    }
    
    await this.$confirm('确定要拒绝这个圈子的审核吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    // 调用后端接口拒绝圈子审核
        await this.$axios.put(`/api/circle/manage/reject/${circle.id}`, {})
    
    this.$message.success('拒绝审核成功')
    this.refreshData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝审核失败:', error)
      this.$message.error('拒绝审核失败')
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
    async handleViewPost(post) {
      try {
        const response = await GetCircleCommentById(post.id)
        console.log('获取帖子详情响应:', response)
        
        // 处理响应数据
        let postDetail = response
        if (response.data) {
          postDetail = response.data
        } else if (response.data && response.data.data) {
          postDetail = response.data.data
        }
        
        this.currentPost = postDetail
        this.postDetailDialogVisible = true
      } catch (error) {
        console.error('查看帖子失败:', error)
        this.$message.error('查看帖子失败')
      }
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
        
        // 调用后端接口删除帖子
        await DeleteCircleComment(post.id)
        
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
        
        // 调用批量通过接口，直接传递ID数组
        await this.$axios.post('/api/circle/manage/batch-approve', circleIds, {
          headers: {
            'Content-Type': 'application/json'
          }
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
  
  <style scoped>
  .post-detail {
    padding: 20px 0;
  }
  
  .post-meta {
    margin: 10px 0 20px 0;
    color: #909399;
    font-size: 14px;
  }
  
  .post-meta span {
    margin-right: 20px;
  }
  
  .post-content {
    line-height: 1.6;
    white-space: pre-wrap;
  }
  
  .loading {
    text-align: center;
    padding: 50px 0;
    color: #909399;
  }
  </style>