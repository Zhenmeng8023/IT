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
                    <el-descriptions-item label="创建人">
                      {{ creatorInfo ? (creatorInfo.nickname || creatorInfo.username) : '未知用户' }}
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
import {
  getCircleManageStats,
  getCircleManageList,
  searchCircleByName,
  createCircle,
  updateCircle,
  approveCircle,
  rejectCircle,
  toggleCircleRecommend,
  closeCircle,
  deleteCircle,
  batchApproveCircles,
  batchCloseCircles,
  batchDeleteCircles,
  getCircleMembers,
  setCircleAdmin,
  removeCircleMember,
  getCirclePosts,
  getCirclePostDetail,
  approveCirclePost,
  deleteCirclePost,
  getUserById
} from '@/api/circleManage'
import { pickAvatarUrl } from '@/utils/avatar'

function unwrapResponse(payload) {
  if (!payload || typeof payload !== 'object') return payload
  if (!Object.prototype.hasOwnProperty.call(payload, 'data') || payload.data === payload) {
    return payload
  }
  return unwrapResponse(payload.data)
}

function normalizePage(payload) {
  const data = unwrapResponse(payload)
  if (Array.isArray(data)) {
    return { list: data, total: data.length }
  }
  if (!data || typeof data !== 'object') {
    return { list: [], total: 0 }
  }

  const list = data.list || data.records || data.content || data.items || data.rows || []
  const normalizedList = Array.isArray(list) ? list : []
  const total = Number(data.total != null ? data.total : data.totalElements)

  return {
    list: normalizedList,
    total: Number.isFinite(total) ? total : normalizedList.length,
    currentPage: data.number != null ? Number(data.number) + 1 : undefined,
    pageSize: Number(data.size)
  }
}

function toStatus(type, fallback) {
  if (fallback) return fallback
  if (type === 'approved') return 'normal'
  if (type === 'pending') return 'pending'
  if (type === 'close' || type === 'closed') return 'closed'
  if (type === 'rejected') return 'violation'
  return 'normal'
}

function sanitizeCirclePayload(form) {
  const payload = {
    name: form.name,
    description: form.description,
    visibility: form.visibility,
    maxMembers: form.maxMembers
  }

  return Object.keys(payload).reduce((acc, key) => {
    const value = payload[key]
    if (value !== null && value !== undefined && value !== '') {
      acc[key] = value
    }
    return acc
  }, {})
}

export default {
  name: 'CircleManage',
  layout: 'manage',
  data() {
    return {
      filterForm: {
        status: '',
        type: '',
        privacy: '',
        dateRange: [],
        keyword: ''
      },
      searchTimer: null,
      stats: {
        totalCircles: 0,
        totalMembers: 0,
        totalPosts: 0,
        todayActive: 0
      },
      circleList: [],
      selectedCircles: [],
      loading: false,
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      detailDialogVisible: false,
      circleDialogVisible: false,
      currentCircle: null,
      creatorInfo: null,
      detailTab: 'basic',
      memberList: [],
      postDetailDialogVisible: false,
      currentPost: null,
      memberLoading: false,
      postList: [],
      postLoading: false,
      circleForm: {
        id: '',
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: null,
        avatar: ''
      },
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
      circleDialogTitle: '创建圈子'
    }
  },
  mounted() {
    this.loadStats()
    this.loadCircleList()
  },
  beforeDestroy() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
      this.searchTimer = null
    }
  },
  methods: {
    mapCircle(raw) {
      const creator = raw.creator || {}
      const type = raw.type || raw.status || 'pending'
      return {
        id: raw.id,
        name: raw.name || '',
        description: raw.description || '',
        type,
        status: toStatus(type, raw.status),
        privacy: raw.visibility || raw.privacy || 'public',
        memberCount: raw.memberCount || 0,
        postCount: raw.postCount || 0,
        todayActive: raw.activeMemberCount || raw.todayActive || 0,
        createTime: raw.createdAt || raw.createTime,
        creatorId: raw.creatorId || creator.id,
        creator: creator.nickname || creator.username || raw.creatorName || '未知用户',
        creatorAvatar: pickAvatarUrl(raw.creatorAvatarUrl, raw.creatorAvatar, creator.avatarUrl, creator.avatar),
        isRecommended: Boolean(raw.isRecommended || raw.recommended)
      }
    },
    mapMember(raw) {
      return {
        id: raw.id,
        userId: raw.userId,
        avatarUrl: pickAvatarUrl(raw.avatarUrl, raw.avatar),
        nickname: raw.nickname || raw.username || '未知用户',
        role: raw.role || 'member',
        status: raw.status || 'active',
        joinTime: raw.joinTime || raw.createdAt || null,
        lastActive: raw.lastActive || raw.updatedAt || null
      }
    },
    mapPost(raw) {
      const author = raw.author || {}
      const title = raw.title || (raw.content ? String(raw.content).slice(0, 24) : '无标题')
      return {
        id: raw.id,
        title,
        content: raw.content || '',
        author: {
          id: author.id,
          username: author.username || '',
          nickname: author.nickname || author.username || '未知用户',
          avatarUrl: pickAvatarUrl(author.avatarUrl, author.avatar)
        },
        createTime: raw.createdAt || raw.createTime || null,
        createdAt: raw.createdAt || raw.createTime || null,
        status: raw.status || 'published',
        commentCount: raw.replyCount || raw.commentCount || 0,
        likes: raw.likes || 0
      }
    },
    async validateCircleForm() {
      return new Promise(resolve => {
        this.$refs.circleForm.validate(valid => resolve(valid))
      })
    },
    async loadStats() {
      try {
        const response = await getCircleManageStats()
        const data = unwrapResponse(response) || {}
        this.stats = {
          totalCircles: Number(data.totalCircles) || 0,
          totalMembers: Number(data.totalMembers) || 0,
          totalPosts: Number(data.totalPosts) || 0,
          todayActive: Number(data.todayActive != null ? data.todayActive : data.activeMembers) || 0
        }
      } catch (error) {
        this.stats = {
          totalCircles: 0,
          totalMembers: 0,
          totalPosts: 0,
          todayActive: 0
        }
      }
    },
    buildListParams() {
      return {
        page: this.pagination.currentPage - 1,
        size: this.pagination.pageSize,
        type: this.filterForm.status || undefined,
        visibility: this.filterForm.privacy || undefined
      }
    },
    async loadCircleList() {
      this.loading = true
      try {
        if (this.filterForm.keyword) {
          await this.searchByName(this.filterForm.keyword)
          return
        }

        const response = await getCircleManageList(this.buildListParams())
        const pageData = normalizePage(response)
        this.circleList = pageData.list.map(this.mapCircle)
        this.pagination.total = pageData.total
        if (Number.isFinite(pageData.currentPage)) {
          this.pagination.currentPage = pageData.currentPage
        }
        if (Number.isFinite(pageData.pageSize) && pageData.pageSize > 0) {
          this.pagination.pageSize = pageData.pageSize
        }
      } catch (error) {
        this.circleList = []
        this.pagination.total = 0
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载圈子列表失败')
      } finally {
        this.loading = false
      }
    },
    async searchByName(keyword) {
      try {
        const response = await searchCircleByName(keyword)
        const data = unwrapResponse(response)
        const list = data && data.id ? [data] : []
        this.circleList = list.map(this.mapCircle)
        this.pagination.total = this.circleList.length
      } catch (error) {
        if (error.response && error.response.status === 404) {
          this.circleList = []
          this.pagination.total = 0
          return
        }
        throw error
      }
    },
    async loadMemberList(circleId) {
      this.memberLoading = true
      try {
        const response = await getCircleMembers(circleId)
        const data = unwrapResponse(response)
        const list = Array.isArray(data) ? data : []
        this.memberList = list.map(this.mapMember)
      } catch (error) {
        this.memberList = []
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载成员列表失败')
      } finally {
        this.memberLoading = false
      }
    },
    async loadPostList(circleId) {
      this.postLoading = true
      try {
        const response = await getCirclePosts(circleId)
        const data = unwrapResponse(response)
        const list = Array.isArray(data) ? data : []
        this.postList = list.map(this.mapPost)
      } catch (error) {
        this.postList = []
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载帖子列表失败')
      } finally {
        this.postLoading = false
      }
    },
    handleSelectionChange(selection) {
      this.selectedCircles = selection
    },
    handleInput() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.searchTimer = setTimeout(() => {
        this.handleSearch()
      }, 300)
    },
    async handleSearch() {
      this.pagination.currentPage = 1
      await this.loadCircleList()
    },
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
    refreshData() {
      this.pagination.currentPage = 1
      this.loadStats()
      this.loadCircleList()
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.loadCircleList()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadCircleList()
    },
    async handleViewDetail(circle) {
      this.currentCircle = { ...circle }
      this.detailTab = 'basic'
      this.detailDialogVisible = true
      this.loadMemberList(circle.id)
      this.loadPostList(circle.id)
      this.loadCreatorInfo(circle.creatorId)
    },
    async loadCreatorInfo(creatorId) {
      if (!creatorId) {
        this.creatorInfo = null
        return
      }
      try {
        const response = await getUserById(creatorId)
        this.creatorInfo = unwrapResponse(response)
      } catch (error) {
        this.creatorInfo = null
      }
    },
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.currentCircle = null
      this.creatorInfo = null
      this.memberList = []
      this.postList = []
    },
    handleMemberManage(circle) {
      this.currentCircle = { ...circle }
      this.detailTab = 'members'
      this.detailDialogVisible = true
      this.loadMemberList(circle.id)
    },
    handlePostManage(circle) {
      this.currentCircle = { ...circle }
      this.detailTab = 'posts'
      this.detailDialogVisible = true
      this.loadPostList(circle.id)
    },
    async handleConfirmCircle() {
      const valid = await this.validateCircleForm()
      if (!valid) return

      const payload = sanitizeCirclePayload(this.circleForm)
      try {
        if (this.circleForm.id) {
          await updateCircle(this.circleForm.id, payload)
          this.$message.success('圈子更新成功')
        } else {
          await createCircle(payload)
          this.$message.success('圈子创建成功')
        }
        this.circleDialogVisible = false
        this.refreshData()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '圈子操作失败')
      }
    },
    handleCreateCircle() {
      this.circleForm = {
        id: '',
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: null,
        avatar: ''
      }
      this.circleDialogTitle = '创建圈子'
      this.circleDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.circleForm) {
          this.$refs.circleForm.clearValidate()
        }
      })
    },
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
    handleAvatarSuccess(res, file) {
      this.circleForm.avatar = URL.createObjectURL(file.raw)
    },
    async handleApprove(circle) {
      try {
        if (circle.type !== 'pending') {
          this.$message.warning('只有待审核的圈子才能通过审核')
          return
        }
        await this.$confirm('确定要通过这个圈子的审核吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        })
        await approveCircle(circle.id)
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '审核通过失败')
        }
      }
    },
    async handleToggleRecommend(circle) {
      try {
        await toggleCircleRecommend(circle.id)
        circle.isRecommended = !circle.isRecommended
        this.$message.success(circle.isRecommended ? '已设置推荐' : '已取消推荐')
      } catch (error) {
        if (error.response && error.response.status === 501) {
          this.$message.warning('推荐功能后端暂未实现')
          return
        }
        this.$message.error((error.response && error.response.data && error.response.data.message) || '操作失败')
      }
    },
    async handleCloseCircle(circle) {
      try {
        if (circle.type !== 'approved') {
          this.$message.warning('只有已通过的圈子才能关闭')
          return
        }
        await this.$confirm('确定要关闭这个圈子吗？关闭后圈子将不可用，但数据会保留。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await closeCircle(circle.id)
        this.$message.success('圈子关闭成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '关闭圈子失败')
        }
      }
    },
    async handleRejectCircle(circle) {
      try {
        if (circle.type !== 'pending') {
          this.$message.warning('只有待审核的圈子才能拒绝审核')
          return
        }
        await this.$confirm('确定要拒绝这个圈子的审核吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'error'
        })
        await rejectCircle(circle.id)
        this.$message.success('拒绝审核成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '拒绝审核失败')
        }
      }
    },
    async handleDeleteCircle(circle) {
      try {
        await this.$confirm('确定要删除这个圈子吗？此操作不可恢复！', '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        await deleteCircle(circle.id)
        this.$message.success('圈子删除成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '删除圈子失败')
        }
      }
    },
    async handleSetAdmin(member) {
      try {
        await this.$confirm(`确定要将成员 ${member.nickname} 设为管理员吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await setCircleAdmin(member.id, 'admin')
        this.$message.success('设置管理员成功')
        this.loadMemberList(this.currentCircle.id)
      } catch (error) {
        if (error === 'cancel') return
        if (error.response && error.response.status === 501) {
          this.$message.warning('设置管理员接口后端暂未实现')
          return
        }
        this.$message.error((error.response && error.response.data && error.response.data.message) || '设置管理员失败')
      }
    },
    async handleRemoveMember(member) {
      try {
        await this.$confirm(`确定要将成员 ${member.nickname} 从圈子中移除吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await removeCircleMember(member.id)
        this.$message.success('成员移除成功')
        this.loadMemberList(this.currentCircle.id)
      } catch (error) {
        if (error === 'cancel') return
        if (error.response && error.response.status === 501) {
          this.$message.warning('移除成员接口后端暂未实现')
          return
        }
        this.$message.error((error.response && error.response.data && error.response.data.message) || '移除成员失败')
      }
    },
    async handleViewPost(post) {
      try {
        const response = await getCirclePostDetail(post.id)
        const detail = unwrapResponse(response)
        this.currentPost = detail || post
        this.postDetailDialogVisible = true
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '查看帖子失败')
      }
    },
    async handleApprovePost(post) {
      try {
        await approveCirclePost(post.id)
        this.$message.success('帖子审核通过成功')
        this.loadPostList(this.currentCircle.id)
      } catch (error) {
        if (error.response && error.response.status === 501) {
          this.$message.warning('帖子审核接口后端暂未实现')
          return
        }
        this.$message.error((error.response && error.response.data && error.response.data.message) || '帖子审核通过失败')
      }
    },
    async handleDeletePost(post) {
      try {
        await this.$confirm(`确定要删除帖子 "${post.title}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteCirclePost(post.id)
        this.$message.success('帖子删除成功')
        this.loadPostList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '删除帖子失败')
        }
      }
    },
    async handleBatchApprove() {
      const circleIds = this.selectedCircles.map(circle => circle.id)
      if (!circleIds.length) return

      try {
        await this.$confirm(`确定要批量通过 ${circleIds.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await batchApproveCircles(circleIds)
        this.$message.success(`批量通过 ${circleIds.length} 个圈子成功`)
        this.selectedCircles = []
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '批量通过失败')
        }
      }
    },
    async handleBatchClose() {
      const circleIds = this.selectedCircles.map(circle => circle.id)
      if (!circleIds.length) return

      try {
        await this.$confirm(`确定要批量关闭 ${circleIds.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await batchCloseCircles(circleIds)
        this.$message.success(`批量关闭 ${circleIds.length} 个圈子成功`)
        this.selectedCircles = []
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '批量关闭失败')
        }
      }
    },
    async handleBatchDelete() {
      const circleIds = this.selectedCircles.map(circle => circle.id)
      if (!circleIds.length) return

      try {
        await this.$confirm(`确定要批量删除 ${circleIds.length} 个圈子吗？此操作不可恢复！`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        await batchDeleteCircles(circleIds)
        this.$message.success(`批量删除 ${circleIds.length} 个圈子成功`)
        this.selectedCircles = []
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '批量删除失败')
        }
      }
    },
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
    getStatusType(status) {
      const typeMap = {
        normal: 'success',
        pending: 'warning',
        closed: 'info',
        violation: 'danger'
      }
      return typeMap[status] || 'info'
    },
    getStatusText(status) {
      const textMap = {
        normal: '正常',
        pending: '待审核',
        closed: '已关闭',
        violation: '违规'
      }
      return textMap[status] || status
    },
    getTypeType(type) {
      const typeMap = {
        pending: 'warning',
        approved: 'success',
        close: 'info',
        rejected: 'danger',
        public: 'success',
        private: 'info',
        official: 'danger'
      }
      return typeMap[type] || 'info'
    },
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
