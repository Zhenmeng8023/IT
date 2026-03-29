<template>
  <div class="circle-manage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>圈子管理</h1>
      <p>管理所有圈子，包括删除,创建等</p>
    </div>

    <!-- 筛选工具栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-toolbar">
        <div class="filter-left">
          <!-- <el-select v-model="filterForm.type" placeholder="圈子类型" clearable style="width: 120px">
            <el-option label="官方" value="official"></el-option>
            <el-option label="私密" value="private"></el-option>
            <el-option label="公开" value="public"></el-option>
          </el-select> -->
          
          
          <el-select v-model="filterForm.privacy" placeholder="可见性" clearable style="width: 120px; margin-left: 10px" @change="handleSearch">
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
    <!-- 注释掉所有统计卡片，将圈子总数移到工具栏中 -->
    <!--
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
      
      <-- 注释掉成员总数和帖子总数，因为API没有返回相关数据 -->
      <!--
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
      -->
      
      <!-- <el-col :span="6">
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
      </el-col> -->

    <!-- 操作工具栏 -->
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar" style="display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; align-items: center; gap: 10px; flex: 1;">
          <div style="display: flex; align-items: center; padding: 8px 16px; background-color: #f5f7fa; border-radius: 4px; flex: 1;">
            <i class="el-icon-user-solid" style="color: #409EFF; margin-right: 8px;"></i>
            <span>圈子总数: {{ stats.totalCircles }}</span>
          </div>
          <el-button v-permission="'btn:circle-manage:create'" type="primary" icon="el-icon-plus" @click="handleCreateCircle" style="flex: 1;">创建圈子</el-button>
          <el-button v-permission="'btn:circle-audit:batch-delete'" type="danger" icon="el-icon-delete" @click="handleBatchDelete" :disabled="selectedCircles.length === 0" style="flex: 1;">
            批量删除
          </el-button>
          <el-button icon="el-icon-refresh" @click="refreshData" style="flex: 1;">刷新</el-button>
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
        style="width: 100%"
        border
        :default-sort="{prop: 'createdAt', order: 'descending'}">
         <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column type="index" width="50" label="序号"></el-table-column>
        
        <el-table-column prop="name" label="圈子信息" min-width="200">
          <template slot-scope="scope">
            <div class="circle-info">
              <div class="circle-details">
                <div class="circle-name">
                  <span class="name-text">{{ scope.row.name }}</span>
                  <!-- <el-tag v-if="scope.row.isRecommended" size="mini" type="warning" style="margin-left: 5px">推荐</el-tag> -->
                  <el-tag v-if="scope.row.type === 'official'" size="mini" type="danger" style="margin-left: 5px">官方</el-tag>
                </div>
                <div class="circle-description">{{ scope.row.description }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="creator" label="创建人" width="120">
          <template slot-scope="scope">
            <el-avatar :size="24" :src="scope.row.creator?.avatar || scope.row.creatorAvatar" style="vertical-align: middle; margin-right: 5px"></el-avatar>
            {{ formatCreatorName(scope.row.creator) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="visibility" label="可见性" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.visibility === 'public' ? 'success' : 'info'" size="small">
              {{ scope.row.visibility === 'public' ? '公开' : '私密' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <!-- 注释掉成员数和帖子数列，因为API没有返回相关数据 -->
        <!--
        <el-table-column prop="memberCount" label="成员" width="100" align="center">
          <template slot-scope="scope">
            <el-tooltip :content="'成员数: ' + (scope.row.memberCount || 0)" placement="top">
              <span class="count-text">{{ scope.row.memberCount || 0 }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <el-table-column prop="postCount" label="帖子" width="100" align="center">
          <template slot-scope="scope">
            <el-tooltip :content="'帖子数: ' + (scope.row.postCount || 0)" placement="top">
              <span class="count-text">{{ scope.row.postCount || 0 }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        -->
        
        <!-- 注释掉今日活跃列，因为API没有返回相关数据 -->
        <!--
        <el-table-column prop="todayActive" label="今日活跃" width="90" align="center">
          <template slot-scope="scope">
            <span class="active-text">{{ scope.row.todayActive }}</span>
          </template>
        </el-table-column>
        -->
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <!-- <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
         -->
        <el-table-column label="操作" width="350" align="center">
          <template slot-scope="scope">
            <!-- <el-button v-permission="'btn:circle-audit:view'"
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
            </el-button> -->
            
            <el-button v-permission="'btn:circle-audit:approve'"
              v-if="scope.row.status === 'pending'"
              size="mini"
              type="text"
              icon="el-icon-check"
              @click="handleApprove(scope.row)"
              style="color: #67C23A;">
              通过
            </el-button>
            
            <el-button v-permission="scope.row.isRecommended ? 'btn:circle-audit:cancel-recommend' : 'btn:circle-audit:recommend'"
              v-if="scope.row.status === 'normal'"
              size="mini"
              type="text"
              :icon="scope.row.isRecommended ? 'el-icon-star-off' : 'el-icon-star-on'"
              @click="handleToggleRecommend(scope.row)"
              :style="{color: scope.row.isRecommended ? '#E6A23C' : '#909399'}">
              {{ scope.row.isRecommended ? '取消推荐' : '推荐' }}
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
                    <el-descriptions-item label="隐私设置">{{ getPrivacyText(currentCircle.visibility || currentCircle.privacy) }}</el-descriptions-item>
                    <el-descriptions-item label="成员数量">{{ currentCircle.memberCount }}人</el-descriptions-item>
                    <el-descriptions-item label="帖子数量">{{ currentCircle.postCount }}篇</el-descriptions-item>
                    <el-descriptions-item label="今日活跃">{{ currentCircle.todayActive }}人</el-descriptions-item>
                    <el-descriptions-item label="创建时间">{{ formatDate(currentCircle.createTime) }}</el-descriptions-item>
                    <!-- <el-descriptions-item label="圈子状态"> -->
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
        
        <!-- <el-form-item label="圈子描述" prop="description">
          <el-input
            type="textarea"
            :rows="2"
            v-model="circleForm.description"
            placeholder="请输入圈子描述">
          </el-input>
        </el-form-item>
        
        <el-form-item label="圈子类型" prop="type">
          <el-select v-model="circleForm.type" placeholder="请选择圈子类型" style="width: 100%;">
            <el-option label="技术交流" value="tech"></el-option>
            <el-option label="学习讨论" value="study"></el-option>
            <el-option label="兴趣爱好" value="hobby"></el-option>
            <el-option label="生活分享" value="life"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
        </el-form-item> -->
        
        <el-form-item label="隐私设置" prop="visibility">
          <el-radio-group v-model="circleForm.visibility">
            <el-radio label="public">公开</el-radio>
            <el-radio label="private">私密</el-radio>
            <!-- <el-radio label="approval">需要审核（申请后需管理员审核）</el-radio> -->
          </el-radio-group>
        </el-form-item>
        
        <!-- <el-form-item label="圈子介绍" prop="introduction">
          <el-input
            type="textarea"
            :rows="3"
            v-model="circleForm.introduction"
            placeholder="请输入详细的圈子介绍（可选）">
          </el-input>
        </el-form-item>
        
        <el-form-item label="圈子规则" prop="rules">
          <el-input
            type="textarea"
            :rows="2"
            v-model="circleForm.rules"
            placeholder="请输入圈子规则（可选）">
          </el-input>
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
import { CreateCircle, UpdateCircle, DeleteCircle } from '@/api/index.js'

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
      
      // 用户信息缓存
      userCache: new Map(),
      
      // 圈子表单
      circleForm: {
        id: '',
        name: '',
        description: '',
        type: '',
        visibility: 'public',
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
        visibility: [
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
      // 使用现有API接口获取统计数据
      const response = await this.$axios.get('/api/circle')
      const circles = response.data || []
      
      // 计算统计数据，只计算圈子总数
      this.stats = {
        totalCircles: circles.length
        // 注释掉成员总数和帖子总数，因为API没有返回相关数据
        // totalMembers: circles.reduce((sum, circle) => sum + (circle.memberCount || circle.member_count || circle.members || 0), 0),
        // totalPosts: circles.reduce((sum, circle) => sum + (circle.postCount || circle.post_count || circle.posts || 0), 0),
        // todayActive: circles.reduce((sum, circle) => sum + (circle.todayActive || circle.today_active || 0), 0)
      }
    } catch (error) {
      console.error('加载统计数据失败:', error)
      // 如果API调用失败，使用模拟数据
      this.stats = {
        totalCircles: 156
        // 注释掉成员总数和帖子总数，因为API没有返回相关数据
        // totalMembers: 8924,
        // totalPosts: 45678,
        // todayActive: 234
      }
    }
  },
    
// 加载圈子列表
async loadCircleList() {
  this.loading = true
  try {
    // 使用现有API接口获取圈子列表
    const response = await this.$axios.get('/api/circle')
    let circles = response.data || []
    
    // 标准化数据格式，确保成员数和帖子数字段存在
circles = circles.map(circle => {
  // 确保visibility字段存在且值正确
  let visibilityValue = 'public'; // 默认公开
  if (circle.visibility) {
    // 检查visibility字段的值
    if (circle.visibility === 'private' || circle.visibility === '1') {
      visibilityValue = 'private';
    } else if (circle.visibility === 'public' || circle.visibility === '0') {
      visibilityValue = 'public';
    }
  } else if (circle.privacy) {
    visibilityValue = circle.privacy;
  }
  console.log('圈子:', circle.name, '原始visibility:', circle.visibility, '计算的visibility:', visibilityValue)
  return {
    ...circle,
    memberCount: circle.memberCount || circle.member_count || circle.members || circle.membercount || 0,
    postCount: circle.postCount || circle.post_count || circle.posts || circle.postcount || 0,
    todayActive: circle.todayActive || circle.today_active || 0,
    visibility: visibilityValue, // 确保visibility字段存在
    createTime: circle.createTime || circle.createdAt || circle.created_at || null // 确保createTime字段存在
  }
})
    
    console.log('标准化后的数据:', circles)
    
    // 前端过滤和分页
    if (this.filterForm.keyword) {
      const keyword = this.filterForm.keyword.toLowerCase()
      circles = circles.filter(circle => {
        // 搜索圈子名称
        const nameMatch = circle.name?.toLowerCase().includes(keyword)
        // 搜索创建人
        let creatorMatch = false
        if (circle.creator) {
          if (typeof circle.creator === 'object') {
            // 如果creator是对象，搜索其username、name或nickname
            creatorMatch = circle.creator.username?.toLowerCase().includes(keyword) ||
                          circle.creator.name?.toLowerCase().includes(keyword) ||
                          circle.creator.nickname?.toLowerCase().includes(keyword)
          } else {
            // 如果creator是字符串，直接搜索
            creatorMatch = circle.creator.toLowerCase().includes(keyword)
          }
        }
        return nameMatch || creatorMatch
      })
    }
    
    if (this.filterForm.type) {
      circles = circles.filter(circle => circle.type === this.filterForm.type)
    }
    
    if (this.filterForm.privacy) {
  console.log('筛选隐私状态:', this.filterForm.privacy)
  console.log('筛选前圈子数量:', circles.length)

  // 定义映射表，将前端的 'public'/'private' 映射为后端可能的值
  const visibilityMap = {
    public: ['public', '0', 'false'],
    private: ['private', '1', 'true']
  }

  const targetValues = visibilityMap[this.filterForm.privacy] || []

  circles = circles.filter(circle => {
    // 获取实际的 visibility 值（优先级：visibility > privacy）
    let actualVisibility = circle.visibility || circle.privacy || 'public'

    // 转换为字符串并标准化（去除空格、转小写）
    actualVisibility = String(actualVisibility).trim().toLowerCase()

    // 判断是否匹配目标值
    const match = targetValues.some(val => 
      String(val).trim().toLowerCase() === actualVisibility
    )

    console.log(`圈子 ${circle.name} 的visibility值: ${actualVisibility}, 是否匹配: ${match}`)
    return match
  })

  console.log('筛选后圈子数量:', circles.length)
}
    
    // 分页处理
    const startIndex = (this.pagination.currentPage - 1) * this.pagination.pageSize
    const endIndex = startIndex + this.pagination.pageSize
    
    this.circleList = circles.slice(startIndex, endIndex)
    this.pagination.total = circles.length
    
    // 打印最终显示的数据，用于调试
    console.log('最终显示的圈子列表:', this.circleList)
    
  } catch (error) {
    console.error('加载圈子列表失败:', error)
    // 如果API调用失败，使用模拟数据
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
        todayActive: 23,
        createTime: new Date('2024-01-15'),
        status: 'normal',
        isRecommended: true,
        introduction: '专注于前端开发技术交流，包括Vue、React、Angular等框架',
        rules: '禁止发布广告，文明交流'
      },
      {
        id: 2,
        name: '后端技术交流圈',
        avatar: '',
        description: '后端开发技术交流与分享',
        creator: '李四',
        creatorAvatar: '',
        type: '技术交流',
        privacy: 'public',
        memberCount: 234,
        postCount: 156,
        todayActive: 45,
        createTime: new Date('2024-01-10'),
        status: 'normal',
        isRecommended: false,
        introduction: '专注于后端开发技术交流，包括Java、Python、Node.js等',
        rules: '禁止发布广告，文明交流'
      },
      {
        id: 3,
        name: '移动开发交流圈',
        avatar: '',
        description: '移动应用开发技术交流与分享',
        creator: '王五',
        creatorAvatar: '',
        type: '技术交流',
        privacy: 'public',
        memberCount: 189,
        postCount: 123,
        todayActive: 32,
        createTime: new Date('2024-01-05'),
        status: 'normal',
        isRecommended: true,
        introduction: '专注于移动应用开发技术交流，包括iOS、Android等',
        rules: '禁止发布广告，文明交流'
      }
    ]
    this.pagination.total = this.circleList.length
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
    handleCreateCircle() {
      this.circleForm = {
        id: '',
        name: '',
        description: '',
        type: '',
        visibility: 'public',
        introduction: '',
        rules: '',
        avatar: ''
      }
      this.circleDialogTitle = '创建圈子'
      this.circleDialogVisible = true
      this.$nextTick(() => {
        this.$refs.circleForm && this.$refs.circleForm.clearValidate()
      })
    },
    
    // 编辑圈子
    handleEditCircle(circle) {
      this.circleForm = {
        id: circle.id,
        name: circle.name,
        description: circle.description,
        type: circle.type,
        visibility: circle.visibility || circle.privacy || 'public',
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
            const circleData = {
              name: this.circleForm.name,
              visibility: this.circleForm.visibility, // 直接使用visibility参数
              creatorId: 1 // 模拟用户ID，与circle.vue保持一致
            }
            
            if (this.circleForm.id) {
              // 编辑现有圈子 - 使用UpdateCircle API
              const response = await UpdateCircle(this.circleForm.id, circleData)
              console.log('更新圈子响应:', response)
              this.$message.success('圈子更新成功')
            } else {
              // 创建新圈子 - 使用CreateCircle API
              const response = await CreateCircle(circleData)
              console.log('创建圈子响应:', response)
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
        
        // 调用后端接口删除圈子
        await DeleteCircle(circle.id)
        
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
    
    // // 批量通过
    // async handleBatchApprove() {
    //   try {
    //     const circleIds = this.selectedCircles.map(c => c.id)
    //     await this.$confirm(`确定要批量通过 ${circleIds.length} 个圈子吗？`, '提示', {
    //       confirmButtonText: '确定',
    //       cancelButtonText: '取消',
    //       type: 'warning'
    //     })
        
    //     //TODO: 调用后端接口批量通过
    //     await this.$axios.post('/api/circle/manage/batch-approve', {
    //       ids: circleIds
    //     })
        
    //     this.$message.success(`批量通过 ${circleIds.length} 个圈子成功`)
    //     this.refreshData()
    //   } catch (error) {
    //     if (error !== 'cancel') {
    //       console.error('批量通过失败:', error)
    //       this.$message.error('批量通过失败')
    //     }
    //   }
    // },
    
    // // 批量关闭
    // async handleBatchClose() {
    //   try {
    //     const circleIds = this.selectedCircles.map(c => c.id)
    //     await this.$confirm(`确定要批量关闭 ${circleIds.length} 个圈子吗？`, '提示', {
    //       confirmButtonText: '确定',
    //       cancelButtonText: '取消',
    //       type: 'warning'
    //     })
        
    //     //TODO: 调用后端接口批量关闭
    //     await this.$axios.post('/api/circle/manage/batch-close', {
    //       ids: circleIds
    //     })
        
    //     this.$message.success(`批量关闭 ${circleIds.length} 个圈子成功`)
    //     this.refreshData()
    //   } catch (error) {
    //     if (error !== 'cancel') {
    //       console.error('批量关闭失败:', error)
    //       this.$message.error('批量关闭失败')
    //     }
    //   }
    // },
    
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

    // 获取创建人名称（处理对象或字符串）
    formatCreatorName(creator) {
      if (!creator) return '未知用户'
      
      // 如果creator是对象，获取username字段
      if (typeof creator === 'object' && creator !== null) {
        return creator.username || creator.name || creator.nickname || '未知用户'
      }
      
      // 如果creator是字符串，直接返回
      return creator
    },

    // 通过用户ID获取用户名
    async getUserNameById(userId) {
      // 确保userId是字符串或数字
      if (!userId || typeof userId === 'object') return '未知用户'
      
      const id = String(userId)
      
      // 检查缓存中是否已有该用户信息
      if (this.userCache.has(id)) {
        return this.userCache.get(id)
      }
      
      try {
        // 调用API获取用户信息
        const response = await this.$axios.get(`/api/user/${id}`)
        let userName = '未知用户'
        
        // 处理不同的响应格式
        if (response && response.data) {
          const userData = response.data
          // 支持多种可能的字段名
          userName = userData.username || userData.name || userData.nickname || `用户${id}`
        }
        
        // 缓存用户信息
        this.userCache.set(id, userName)
        return userName
        
      } catch (error) {
        console.error(`获取用户信息失败 (ID: ${id}):`, error)
        // 如果API调用失败，返回默认格式
        return `用户${id}`
      }
    },

    // 批量获取创建人姓名（优化性能）
    async batchGetCreatorNames(circleList) {
      if (!circleList || !Array.isArray(circleList)) return circleList
      
      // 找出所有需要获取的creatorId
      const creatorIds = circleList
        .map(circle => circle.creatorId || circle.creator_id)
        .filter(id => id && typeof id !== 'object' && !this.userCache.has(String(id)))
        .filter((id, index, array) => array.indexOf(id) === index) // 去重
      
      if (creatorIds.length === 0) return circleList
      
      try {
        // 批量获取用户信息（这里可以优化为批量接口，暂时使用循环）
        for (const creatorId of creatorIds) {
          await this.getUserNameById(creatorId)
        }
      } catch (error) {
        console.error('批量获取用户信息失败:', error)
      }
      
      return circleList
    },

    // 获取创建人姓名显示（带加载状态）
    getCreatorNameDisplay(creatorId) {
      if (!creatorId || typeof creatorId === 'object') return '未知用户'
      
      const id = String(creatorId)
      
      if (this.userCache.has(id)) {
        return this.userCache.get(id)
      } else {
        // 异步获取用户信息
        this.getUserNameById(creatorId)
        return '加载中...'
      }
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
    getPrivacyText(visibility) {
      const textMap = {
        public: '公开',
        private: '私密',
        approval: '需要审核'
      }
      return textMap[visibility] || visibility
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