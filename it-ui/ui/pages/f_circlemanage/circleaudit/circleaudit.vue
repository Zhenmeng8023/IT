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
          <el-select v-model="filterForm.status" placeholder="圈子状态" clearable style="width: 120px">
            <el-option label="正常" value="normal"></el-option>
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已关闭" value="closed"></el-option>
            <el-option label="违规" value="violation"></el-option>
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
            start-placeholder="创建开始日期"
            end-placeholder="创建结束日期"
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

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
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
      
      <el-col :span="6">
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
      
      <el-col :span="6">
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
      
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background-color: #F56C6C;">
              <i class="el-icon-chat-dot-round"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.todayActive }}</div>
              <div class="stat-label">今日活跃</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-button v-permission="'btn:circle-audit:create'" type="primary" icon="el-icon-plus" @click="handleCreateCircle">创建圈子</el-button>
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
        <div v-permission="'btn:circle-audit:export'" class="toolbar-right">
          <el-button type="text" icon="el-icon-download">导出数据</el-button>
          <el-button type="text" icon="el-icon-setting">设置</el-button>
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
        
        <el-table-column prop="name" label="圈子信息" min-width="200">
          <template slot-scope="scope">
            <div class="circle-info">
              <el-avatar :size="40" :src="scope.row.avatar" :alt="scope.row.name" style="vertical-align: middle; margin-right: 10px;"></el-avatar>
              <div class="circle-details">
                <div class="circle-name">
                  <span class="name-text">{{ scope.row.name }}</span>
                  <el-tag v-if="scope.row.privacy === 'private'" size="mini" type="info" style="margin-left: 5px">私密</el-tag>
                  <el-tag v-if="scope.row.privacy === 'approval'" size="mini" type="warning" style="margin-left: 5px">需审核</el-tag>
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
        
        <el-table-column prop="type" label="生命周期" width="100">
          <template slot-scope="scope">
            <el-tag :type="getTypeType(scope.row.type)" size="small">
              {{ getLifecycleText(scope.row.type) }}
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
        
        <el-table-column prop="todayActive" label="今日活跃" width="90" align="center">
          <template slot-scope="scope">
            <span class="active-text">{{ scope.row.todayActive }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
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
            
            <el-button v-permission="'btn:circle-audit:close'"
              v-if="scope.row.status === 'normal'"
              size="mini"
              type="text"
              icon="el-icon-switch-button"
              @click="handleCloseCircle(scope.row)"
              style="color: #909399;">
              关闭
            </el-button>
            
            <el-button v-permission="'btn:circle-audit:edit'"
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleEditCircle(scope.row)"
              style="color: #409EFF;">
              编辑
            </el-button>
            
            <el-button v-permission="'btn:circle-audit:delete'"
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
                    <el-descriptions-item label="生命周期">{{ getLifecycleText(currentCircle.type) }}</el-descriptions-item>
                    <el-descriptions-item label="隐私设置">{{ getPrivacyText(currentCircle.privacy) }}</el-descriptions-item>
                    <el-descriptions-item label="成员上限">{{ currentCircle.maxMembers || '未设置' }}</el-descriptions-item>
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
                    <el-button v-permission="'btn:circle-audit:set-admin'" v-if="scope.row.role !== 'creator'" size="mini" type="text" @click="handleSetAdmin(scope.row)">设为管理员</el-button>
                    <el-button v-permission="'btn:circle-audit:remove-member'" v-if="scope.row.role !== 'creator'" size="mini" type="text" style="color: #F56C6C;" @click="handleRemoveMember(scope.row)">移除</el-button>
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
                    <el-tag :type="getPostStatusType(scope.row.status)" size="small">
                      {{ getPostStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200" align="center">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" @click="handleViewPost(scope.row)">查看</el-button>
                    <el-button v-permission="'btn:circle-audit:approve-post'" v-if="scope.row.status === 'pending'" size="mini" type="text" style="color: #67C23A;" @click="handleApprovePost(scope.row)">通过</el-button>
                    <el-button v-permission="'btn:circle-audit:delete-post'" size="mini" type="text" style="color: #F56C6C;" @click="handleDeletePost(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
          
          <!-- 数据统计 -->
          <el-tab-pane label="数据统计" name="statistics">
            <div class="circle-statistics">
              <h4>数据统计</h4>
              <el-row :gutter="20" style="margin-top: 20px;">
                <el-col :span="8">
                  <el-card shadow="hover">
                    <div class="stat-chart">
                      <div class="chart-title">成员增长趋势</div>
                      <div class="chart-placeholder">图表区域</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover">
                    <div class="stat-chart">
                      <div class="chart-title">帖子发布趋势</div>
                      <div class="chart-placeholder">图表区域</div>
                    </div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card shadow="hover">
                    <div class="stat-chart">
                      <div class="chart-title">活跃度分析</div>
                      <div class="chart-placeholder">图表区域</div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="currentCircle && currentCircle.status === 'pending'" type="primary" @click="handleApprove(currentCircle)">通过审核</el-button>
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
        
        <el-form-item label="隐私设置" prop="visibility">
          <el-radio-group v-model="circleForm.visibility">
            <el-radio label="public">公开（任何人可加入）</el-radio>
            <el-radio label="private">私密（仅邀请加入）</el-radio>
            <el-radio label="approval">需要审核（申请后需管理员审核）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="成员上限" prop="maxMembers">
          <el-input-number
            v-model="circleForm.maxMembers"
            :min="1"
            :max="10000"
            :controls-position="'right'"
            style="width: 100%;">
          </el-input-number>
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
import {
  getCircleManageStats,
  getCircleManageList,
  createCircle,
  updateCircle,
  approveCircle,
  closeCircle,
  deleteCircle,
  batchApproveCircles,
  batchCloseCircles,
  batchDeleteCircles,
  getCircleMembers,
  setCircleAdmin,
  removeCircleMember,
  getCirclePosts,
  approveCirclePost,
  deleteCirclePost
} from '@/api/circleManage'
import { pickAvatarUrl } from '@/utils/avatar'

function createDefaultStats() {
  return {
    totalCircles: 0,
    totalMembers: 0,
    totalPosts: 0,
    todayActive: 0
  }
}

function normalizeLifecycleToStatus(lifecycle) {
  if (lifecycle === 'approved') return 'normal'
  if (lifecycle === 'close' || lifecycle === 'closed') return 'closed'
  if (lifecycle === 'rejected') return 'violation'
  return 'pending'
}

function normalizePostStatus(status) {
  if (!status) return 'pending'
  if (status === 'approved' || status === 'normal') return 'published'
  if (status === 'close' || status === 'closed') return 'deleted'
  return status
}

function toSafeNumber(value, fallback = 0) {
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

function parseErrorMessage(error, fallback) {
  if (error && error.response && error.response.data && error.response.data.message) return error.response.data.message
  if (error && error.message) return error.message
  return fallback
}

function requireManageData(payload, fallbackMessage) {
  if (!payload || typeof payload !== 'object') {
    throw new Error(fallbackMessage)
  }
  if (payload.code !== 200) {
    throw new Error(payload.message || fallbackMessage)
  }
  return payload.data
}

function buildCirclePayload(form) {
  const payload = {
    name: form.name ? form.name.trim() : '',
    description: form.description ? form.description.trim() : '',
    visibility: form.visibility,
    maxMembers: form.maxMembers
  }

  Object.keys(payload).forEach((key) => {
    if (payload[key] === '' || payload[key] === null || payload[key] === undefined) {
      delete payload[key]
    }
  })

  return payload
}

export default {
  name: 'CircleManage',
  layout: 'manage',
  data() {
    return {
      filterForm: {
        status: '',
        privacy: '',
        dateRange: [],
        keyword: ''
      },
      stats: createDefaultStats(),
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
      detailTab: 'basic',
      memberList: [],
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
          { min: 2, max: 100, message: '圈子名称长度在 2 到 100 个字符', trigger: 'blur' }
        ],
        description: [
          { max: 1000, message: '圈子描述长度不能超过 1000 个字符', trigger: 'blur' }
        ],
        visibility: [
          { required: true, message: '请选择隐私设置', trigger: 'change' }
        ],
        maxMembers: [
          {
            validator: (_rule, value, callback) => {
              if (value === null || value === undefined || value === '') {
                callback()
                return
              }
              if (!Number.isInteger(value) || value < 1 || value > 10000) {
                callback(new Error('成员上限应为 1-10000 的整数'))
                return
              }
              callback()
            },
            trigger: 'change'
          }
        ]
      },
      circleDialogTitle: '创建圈子'
    }
  },
  mounted() {
    this.loadStats()
    this.loadCircleList()
  },
  methods: {
    mapCircle(raw) {
      const lifecycle = raw.type || 'pending'
      const creator = raw.creatorInfo || {}
      return {
        id: raw.id,
        name: raw.name || '',
        description: raw.description || '',
        type: lifecycle,
        status: raw.status || normalizeLifecycleToStatus(lifecycle),
        visibility: raw.visibility || raw.privacy || 'public',
        privacy: raw.privacy || raw.visibility || 'public',
        memberCount: toSafeNumber(raw.memberCount),
        postCount: toSafeNumber(raw.postCount),
        todayActive: toSafeNumber(raw.todayActive != null ? raw.todayActive : raw.activeMemberCount),
        createTime: raw.createTime || raw.createdAt || null,
        creatorId: raw.creatorId || creator.id || null,
        creator: raw.creator || raw.creatorName || creator.username || '未知用户',
        creatorAvatar: pickAvatarUrl(raw.creatorAvatarUrl, raw.creatorAvatar, creator.avatar),
        avatar: pickAvatarUrl(raw.avatarUrl, raw.avatar),
        maxMembers: raw.maxMembers || null,
        isRecommended: Boolean(raw.isRecommended)
      }
    },
    mapMember(raw) {
      const role = (raw.role || 'member').toLowerCase()
      return {
        id: raw.id,
        userId: raw.userId,
        nickname: raw.nickname || raw.username || '未知用户',
        avatar: pickAvatarUrl(raw.avatar, raw.avatarUrl),
        role,
        status: raw.status || 'active',
        joinTime: raw.joinTime || null,
        lastActive: raw.lastActive || raw.joinTime || null
      }
    },
    mapPost(raw) {
      const content = raw.content || ''
      const title = raw.title || (content ? String(content).slice(0, 30) : '无标题')
      return {
        id: raw.id,
        postId: raw.postId || raw.id,
        title,
        content,
        author: raw.author || raw.authorName || '未知用户',
        createTime: raw.createTime || raw.createdAt || null,
        viewCount: toSafeNumber(raw.viewCount),
        commentCount: toSafeNumber(raw.commentCount),
        status: normalizePostStatus(raw.status),
        likes: toSafeNumber(raw.likes)
      }
    },
    formatDateForBackend(date) {
      if (!date) return null
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },
    buildListParams() {
      const params = {
        page: this.pagination.currentPage,
        pageSize: this.pagination.pageSize
      }
      if (this.filterForm.status) {
        params.status = this.filterForm.status
      }
      if (this.filterForm.privacy) {
        params.privacy = this.filterForm.privacy
      }
      if (this.filterForm.keyword && this.filterForm.keyword.trim()) {
        params.keyword = this.filterForm.keyword.trim()
      }
      if (this.filterForm.dateRange && this.filterForm.dateRange.length === 2) {
        params.startDate = this.formatDateForBackend(this.filterForm.dateRange[0])
        params.endDate = this.formatDateForBackend(this.filterForm.dateRange[1])
      }
      return params
    },
    async loadStats() {
      try {
        const payload = await getCircleManageStats()
        const data = requireManageData(payload, '加载统计数据失败')
        this.stats = {
          totalCircles: toSafeNumber(data.totalCircles),
          totalMembers: toSafeNumber(data.totalMembers),
          totalPosts: toSafeNumber(data.totalPosts),
          todayActive: toSafeNumber(data.todayActive)
        }
      } catch (error) {
        this.stats = createDefaultStats()
      }
    },
    async loadCircleList() {
      this.loading = true
      try {
        const payload = await getCircleManageList(this.buildListParams())
        const data = requireManageData(payload, '加载圈子列表失败') || {}
        const list = Array.isArray(data.list) ? data.list : []

        this.circleList = list.map(this.mapCircle)
        this.pagination.total = toSafeNumber(data.total, this.circleList.length)
        if (data.currentPage) {
          this.pagination.currentPage = toSafeNumber(data.currentPage, this.pagination.currentPage)
        }
        if (data.pageSize) {
          this.pagination.pageSize = toSafeNumber(data.pageSize, this.pagination.pageSize)
        }
      } catch (error) {
        this.circleList = []
        this.pagination.total = 0
        this.$message.error(parseErrorMessage(error, '加载圈子列表失败'))
      } finally {
        this.loading = false
      }
    },
    async loadMemberList(circleId) {
      this.memberLoading = true
      try {
        const payload = await getCircleMembers(circleId)
        const data = requireManageData(payload, '加载成员列表失败')
        this.memberList = (Array.isArray(data) ? data : []).map(this.mapMember)
      } catch (error) {
        this.memberList = []
        this.$message.error(parseErrorMessage(error, '加载成员列表失败'))
      } finally {
        this.memberLoading = false
      }
    },
    async loadPostList(circleId) {
      this.postLoading = true
      try {
        const payload = await getCirclePosts(circleId)
        const data = requireManageData(payload, '加载帖子列表失败')
        this.postList = (Array.isArray(data) ? data : []).map(this.mapPost)
      } catch (error) {
        this.postList = []
        this.$message.error(parseErrorMessage(error, '加载帖子列表失败'))
      } finally {
        this.postLoading = false
      }
    },
    handleSelectionChange(selection) {
      this.selectedCircles = selection
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadCircleList()
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
    handleViewDetail(circle) {
      this.currentCircle = { ...circle }
      this.detailTab = 'basic'
      this.detailDialogVisible = true
      this.loadMemberList(circle.id)
      this.loadPostList(circle.id)
    },
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.currentCircle = null
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
        description: circle.description || '',
        visibility: circle.visibility || circle.privacy || 'public',
        maxMembers: circle.maxMembers || null,
        avatar: circle.avatar || ''
      }
      this.circleDialogTitle = '编辑圈子'
      this.circleDialogVisible = true
    },
    async validateCircleForm() {
      return new Promise((resolve) => {
        this.$refs.circleForm.validate(valid => resolve(valid))
      })
    },
    async handleConfirmCircle() {
      const valid = await this.validateCircleForm()
      if (!valid) return

      try {
        const payload = buildCirclePayload(this.circleForm)
        if (this.circleForm.id) {
          await updateCircle(this.circleForm.id, payload)
          this.$message.success('圈子编辑成功')
        } else {
          await createCircle(payload)
          this.$message.success('圈子创建成功')
        }
        this.circleDialogVisible = false
        this.refreshData()
      } catch (error) {
        this.$message.error(parseErrorMessage(error, '圈子操作失败'))
      }
    },
    handleAvatarSuccess(res, file) {
      this.circleForm.avatar = URL.createObjectURL(file.raw)
    },
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
    async handleApprove(circle) {
      if (circle.status !== 'pending') {
        this.$message.warning('只有待审核圈子才能执行通过')
        return
      }
      try {
        await this.$confirm('确定要通过该圈子的审核吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await approveCircle(circle.id)
        requireManageData(payload, '审核通过失败')
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '审核通过失败'))
        }
      }
    },
    async handleCloseCircle(circle) {
      if (circle.status !== 'normal') {
        this.$message.warning('只有正常圈子才能关闭')
        return
      }
      try {
        await this.$confirm('确定要关闭这个圈子吗？关闭后圈子将不可用，但数据会保留。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await closeCircle(circle.id)
        requireManageData(payload, '圈子关闭失败')
        this.$message.success('圈子关闭成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '圈子关闭失败'))
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
        const payload = await deleteCircle(circle.id)
        requireManageData(payload, '圈子删除失败')
        this.$message.success('圈子删除成功')
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '圈子删除失败'))
        }
      }
    },
    async handleSetAdmin(member) {
      if (member.role === 'creator') {
        this.$message.warning('创建者角色不可修改')
        return
      }
      try {
        await this.$confirm(`确定要将成员 ${member.nickname} 设为管理员吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await setCircleAdmin(member.id, 'admin')
        requireManageData(payload, '设置管理员失败')
        this.$message.success('设置管理员成功')
        this.loadMemberList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '设置管理员失败'))
        }
      }
    },
    async handleRemoveMember(member) {
      if (member.role === 'creator') {
        this.$message.warning('创建者不可移除')
        return
      }
      try {
        await this.$confirm(`确定要将成员 ${member.nickname} 从圈子中移除吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await removeCircleMember(member.id)
        requireManageData(payload, '成员移除失败')
        this.$message.success('成员移除成功')
        this.loadMemberList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '成员移除失败'))
        }
      }
    },
    handleViewPost(post) {
      this.$alert(post.content || '暂无帖子正文', post.title || '帖子详情', {
        confirmButtonText: '关闭'
      })
    },
    async handleApprovePost(post) {
      if (post.status !== 'pending') {
        this.$message.warning('只有待审核帖子才能通过')
        return
      }
      try {
        const payload = await approveCirclePost(post.id)
        requireManageData(payload, '帖子审核通过失败')
        this.$message.success('帖子审核通过成功')
        this.loadPostList(this.currentCircle.id)
      } catch (error) {
        this.$message.error(parseErrorMessage(error, '帖子审核通过失败'))
      }
    },
    async handleDeletePost(post) {
      try {
        await this.$confirm(`确定要删除帖子 "${post.title}" 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await deleteCirclePost(post.id)
        requireManageData(payload, '帖子删除失败')
        this.$message.success('帖子删除成功')
        this.loadPostList(this.currentCircle.id)
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '帖子删除失败'))
        }
      }
    },
    normalizeBatchResult(result, fallbackTotal) {
      return {
        totalCount: toSafeNumber(result && result.totalCount, fallbackTotal),
        successCount: toSafeNumber(result && result.successCount),
        failedCount: toSafeNumber(result && result.failedCount)
      }
    },
    async handleBatchApprove() {
      const pendingCircles = this.selectedCircles.filter(circle => circle.status === 'pending')
      const skippedCount = this.selectedCircles.length - pendingCircles.length
      if (!pendingCircles.length) {
        this.$message.warning('请至少选择一个待审核圈子')
        return
      }

      try {
        await this.$confirm(`确定要批量通过 ${pendingCircles.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await batchApproveCircles(pendingCircles.map(circle => circle.id))
        const result = this.normalizeBatchResult(requireManageData(payload, '批量通过失败'), pendingCircles.length)
        this.$message.success(`批量通过完成：成功 ${result.successCount}，失败 ${result.failedCount}`)
        if (skippedCount > 0) {
          this.$message.warning(`已跳过 ${skippedCount} 个非待审核圈子`)
        }
        this.selectedCircles = []
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '批量通过失败'))
        }
      }
    },
    async handleBatchClose() {
      const activeCircles = this.selectedCircles.filter(circle => circle.status === 'normal')
      const skippedCount = this.selectedCircles.length - activeCircles.length
      if (!activeCircles.length) {
        this.$message.warning('请至少选择一个正常圈子')
        return
      }

      try {
        await this.$confirm(`确定要批量关闭 ${activeCircles.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await batchCloseCircles(activeCircles.map(circle => circle.id))
        const result = this.normalizeBatchResult(requireManageData(payload, '批量关闭失败'), activeCircles.length)
        this.$message.success(`批量关闭完成：成功 ${result.successCount}，失败 ${result.failedCount}`)
        if (skippedCount > 0) {
          this.$message.warning(`已跳过 ${skippedCount} 个非正常圈子`)
        }
        this.selectedCircles = []
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '批量关闭失败'))
        }
      }
    },
    async handleBatchDelete() {
      const circleIds = this.selectedCircles.map(circle => circle.id)
      if (!circleIds.length) {
        this.$message.warning('请先选择要删除的圈子')
        return
      }

      try {
        await this.$confirm(`确定要批量删除 ${circleIds.length} 个圈子吗？此操作不可恢复！`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        const payload = await batchDeleteCircles(circleIds)
        const result = this.normalizeBatchResult(requireManageData(payload, '批量删除失败'), circleIds.length)
        this.$message.success(`批量删除完成：成功 ${result.successCount}，失败 ${result.failedCount}`)
        this.selectedCircles = []
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseErrorMessage(error, '批量删除失败'))
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
        rejected: 'danger'
      }
      return typeMap[type] || 'info'
    },
    getLifecycleText(type) {
      const textMap = {
        pending: '待审核',
        approved: '已通过',
        close: '已关闭',
        rejected: '已拒绝'
      }
      return textMap[type] || type
    },
    getPostStatusType(status) {
      const typeMap = {
        published: 'success',
        pending: 'warning',
        deleted: 'info'
      }
      return typeMap[status] || 'info'
    },
    getPostStatusText(status) {
      const textMap = {
        published: '已发布',
        pending: '待审核',
        deleted: '已删除'
      }
      return textMap[status] || status
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

