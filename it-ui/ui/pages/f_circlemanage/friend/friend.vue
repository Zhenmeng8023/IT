<template>
  <div class="circle-friend">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>圈子好友管理</h1>
      <p>管理圈子成员的好友关系，支持分组、权限设置和好友申请处理</p>
    </div>

    <!-- 选项卡 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 好友列表 -->
      <el-tab-pane label="好友列表" name="friendList">
        <!-- 好友筛选工具栏 -->
        <el-card class="filter-card" shadow="never">
          <div class="filter-toolbar">
            <div class="filter-left">
              <el-select v-model="friendFilter.group" placeholder="好友分组" clearable style="width: 120px">
                <el-option label="全部好友" value=""></el-option>
                <el-option v-for="group in friendGroups" :key="group.id" :label="group.name" :value="group.id"></el-option>
              </el-select>
              
              <el-select v-model="friendFilter.status" placeholder="好友状态" clearable style="width: 120px; margin-left: 10px">
                <el-option label="正常" value="normal"></el-option>
                <el-option label="特别关注" value="special"></el-option>
                <el-option label="已屏蔽" value="blocked"></el-option>
              </el-select>
              
              <el-input
                v-model="friendFilter.keyword"
                placeholder="搜索好友昵称或备注"
                clearable
                style="width: 200px; margin-left: 10px"
                prefix-icon="el-icon-search"
                @input="handleFriendSearch">
              </el-input>
            </div>
            
            <div class="filter-right">
              <el-button type="primary" icon="el-icon-plus" @click="handleAddFriend">添加好友</el-button>
              <el-button icon="el-icon-refresh" @click="refreshFriendList">刷新</el-button>
            </div>
          </div>
        </el-card>

        <!-- 好友列表 -->
        <el-card class="table-card" shadow="never">
          <el-table
            :data="friendList"
            v-loading="friendLoading"
            stripe
            style="width: 100%">
            
            <el-table-column prop="avatar" label="头像" width="80" align="center">
              <template slot-scope="scope">
                <el-avatar :size="40" :src="scope.row.avatar" :alt="scope.row.nickname"></el-avatar>
              </template>
            </el-table-column>
            
            <el-table-column prop="nickname" label="昵称" width="120">
              <template slot-scope="scope">
                <div class="user-info">
                  <span class="nickname">{{ scope.row.nickname }}</span>
                  <el-tag v-if="scope.row.isSpecial" size="mini" type="warning" style="margin-left: 5px">特别关注</el-tag>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="remark" label="备注" width="150">
              <template slot-scope="scope">
                <el-input
                  v-if="scope.row.editingRemark"
                  v-model="scope.row.tempRemark"
                  size="mini"
                  @blur="handleSaveRemark(scope.row)"
                  @keyup.enter="handleSaveRemark(scope.row)">
                </el-input>
                <span v-else @dblclick="handleEditRemark(scope.row)" class="remark-text">
                  {{ scope.row.remark || '无备注' }}
                </span>
              </template>
            </el-table-column>
            
            <el-table-column prop="groupName" label="分组" width="120">
              <template slot-scope="scope">
                <el-select
                  v-model="scope.row.groupId"
                  size="mini"
                  @change="handleChangeGroup(scope.row)"
                  style="width: 100px">
                  <el-option v-for="group in friendGroups" :key="group.id" :label="group.name" :value="group.id"></el-option>
                </el-select>
              </template>
            </el-table-column>
            
            <el-table-column prop="joinTime" label="成为好友时间" width="160" align="center">
              <template slot-scope="scope">
                {{ formatDate(scope.row.joinTime) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="lastContact" label="最后联系" width="160" align="center">
              <template slot-scope="scope">
                {{ formatDate(scope.row.lastContact) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="interactionCount" label="互动次数" width="100" align="center">
              <template slot-scope="scope">
                {{ scope.row.interactionCount }}
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="280" fixed="right" align="center">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-chat-dot-round"
                  @click="handleSendMessage(scope.row)">
                  发消息
                </el-button>
                
                <el-button
                  size="mini"
                  type="text"
                  :icon="scope.row.isSpecial ? 'el-icon-star-off' : 'el-icon-star-on'"
                  @click="handleToggleSpecial(scope.row)"
                  :style="{color: scope.row.isSpecial ? '#E6A23C' : '#909399'}">
                  {{ scope.row.isSpecial ? '取消关注' : '特别关注' }}
                </el-button>
                
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-user"
                  @click="handleViewProfile(scope.row)">
                  查看资料
                </el-button>
                
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-close"
                  @click="handleRemoveFriend(scope.row)"
                  style="color: #F56C6C;">
                  删除好友
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              @size-change="handleFriendSizeChange"
              @current-change="handleFriendCurrentChange"
              :current-page="friendPagination.currentPage"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="friendPagination.pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="friendPagination.total">
            </el-pagination>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 好友申请 -->
      <el-tab-pane label="好友申请" name="friendRequest">
        <!-- 申请筛选 -->
        <el-card class="filter-card" shadow="never">
          <div class="filter-toolbar">
            <div class="filter-left">
              <el-select v-model="requestFilter.status" placeholder="申请状态" clearable style="width: 120px">
                <el-option label="待处理" value="pending"></el-option>
                <el-option label="已同意" value="approved"></el-option>
                <el-option label="已拒绝" value="rejected"></el-option>
              </el-select>
              
              <el-date-picker
                v-model="requestFilter.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 240px; margin-left: 10px">
              </el-date-picker>
            </div>
            
            <div class="filter-right">
              <el-button icon="el-icon-refresh" @click="refreshRequestList">刷新</el-button>
            </div>
          </div>
        </el-card>

        <!-- 申请列表 -->
        <el-card class="table-card" shadow="never">
          <el-table
            :data="requestList"
            v-loading="requestLoading"
            stripe
            style="width: 100%">
            
            <el-table-column prop="avatar" label="申请人" width="120">
              <template slot-scope="scope">
                <div class="applicant-info">
                  <el-avatar :size="32" :src="scope.row.avatar" :alt="scope.row.nickname"></el-avatar>
                  <span style="margin-left: 8px;">{{ scope.row.nickname }}</span>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="message" label="申请消息" min-width="200">
              <template slot-scope="scope">
                {{ scope.row.message || '无附加消息' }}
              </template>
            </el-table-column>
            
            <el-table-column prop="applyTime" label="申请时间" width="160" align="center">
              <template slot-scope="scope">
                {{ formatDate(scope.row.applyTime) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="handleTime" label="处理时间" width="160" align="center">
              <template slot-scope="scope">
                {{ scope.row.handleTime ? formatDate(scope.row.handleTime) : '-' }}
              </template>
            </el-table-column>
            
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="getRequestStatusType(scope.row.status)" size="small">
                  {{ getRequestStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template slot-scope="scope">
                <el-button
                  v-if="scope.row.status === 'pending'"
                  size="mini"
                  type="text"
                  icon="el-icon-check"
                  @click="handleApproveRequest(scope.row)"
                  style="color: #67C23A;">
                  同意
                </el-button>
                
                <el-button
                  v-if="scope.row.status === 'pending'"
                  size="mini"
                  type="text"
                  icon="el-icon-close"
                  @click="handleRejectRequest(scope.row)"
                  style="color: #F56C6C;">
                  拒绝
                </el-button>
                
                <el-button
                  size="mini"
                  type="text"
                  icon="el-icon-view"
                  @click="handleViewApplicant(scope.row)">
                  查看资料
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              @size-change="handleRequestSizeChange"
              @current-change="handleRequestCurrentChange"
              :current-page="requestPagination.currentPage"
              :page-sizes="[10, 20, 50, 100]"
              :page-size="requestPagination.pageSize"
              layout="total, sizes, prev, pager, next, jumper"
              :total="requestPagination.total">
            </el-pagination>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 分组管理 -->
      <el-tab-pane label="分组管理" name="groupManage">
        <el-card class="group-card" shadow="never">
          <div class="group-header">
            <h3>好友分组</h3>
            <el-button type="primary" icon="el-icon-plus" size="small" @click="handleAddGroup">添加分组</el-button>
          </div>
          
          <div class="group-list">
            <el-row :gutter="20">
              <el-col :span="6" v-for="group in friendGroups" :key="group.id" style="margin-bottom: 20px;">
                <el-card shadow="hover" class="group-item">
                  <div class="group-info">
                    <div class="group-name">{{ group.name }}</div>
                    <div class="group-count">好友数量: {{ group.friendCount }}</div>
                    <div class="group-actions">
                      <el-button size="mini" type="text" @click="handleEditGroup(group)">编辑</el-button>
                      <el-button size="mini" type="text" @click="handleDeleteGroup(group)" style="color: #F56C6C;">删除</el-button>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加好友对话框 -->
    <el-dialog
      title="添加好友"
      :visible.sync="addFriendDialogVisible"
      width="500px">
      <el-form :model="addFriendForm" label-width="80px">
        <el-form-item label="用户ID">
          <el-input v-model="addFriendForm.userId" placeholder="请输入用户ID"></el-input>
        </el-form-item>
        <el-form-item label="验证消息">
          <el-input
            type="textarea"
            :rows="3"
            v-model="addFriendForm.message"
            placeholder="请输入验证消息（可选）">
          </el-input>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="addFriendForm.groupId" placeholder="选择分组" style="width: 100%;">
            <el-option v-for="group in friendGroups" :key="group.id" :label="group.name" :value="group.id"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="addFriendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAddFriend">发送申请</el-button>
      </span>
    </el-dialog>

    <!-- 添加/编辑分组对话框 -->
    <el-dialog
      :title="groupDialogTitle"
      :visible.sync="groupDialogVisible"
      width="400px">
      <el-form :model="groupForm" label-width="80px">
        <el-form-item label="分组名称">
          <el-input v-model="groupForm.name" placeholder="请输入分组名称"></el-input>
        </el-form-item>
        <el-form-item label="分组描述">
          <el-input
            type="textarea"
            :rows="2"
            v-model="groupForm.description"
            placeholder="请输入分组描述（可选）">
          </el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmGroup">确认</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getFriendList,
  getFriendRequests,
  getFriendGroups,
  sendFriendRequest,
  updateFriendRemark,
  moveFriendGroup,
  toggleSpecialFocus,
  deleteFriend,
  approveFriendRequest,
  rejectFriendRequest,
  createFriendGroup,
  updateFriendGroup,
  deleteFriendGroup
} from '@/api/social'
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
    total: Number.isFinite(total) ? total : normalizedList.length
  }
}

function toDateString(value) {
  if (!value) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export default {
  name: 'CircleFriend',
  layout: 'manage',
  data() {
    return {
      activeTab: 'friendList',
      friendList: [],
      friendLoading: false,
      friendFilter: {
        group: '',
        status: '',
        keyword: ''
      },
      friendPagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      requestList: [],
      requestLoading: false,
      requestFilter: {
        status: '',
        dateRange: []
      },
      requestPagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      friendGroups: [],
      addFriendDialogVisible: false,
      groupDialogVisible: false,
      addFriendForm: {
        userId: '',
        message: '',
        groupId: ''
      },
      groupForm: {
        id: '',
        name: '',
        description: ''
      },
      groupDialogTitle: '添加分组'
    }
  },
  watch: {
    'friendFilter.group'() {
      this.friendPagination.currentPage = 1
      this.loadFriendList()
    },
    'friendFilter.status'() {
      this.friendPagination.currentPage = 1
      this.loadFriendList()
    },
    'requestFilter.status'() {
      this.requestPagination.currentPage = 1
      this.loadRequestList()
    },
    'requestFilter.dateRange'() {
      this.requestPagination.currentPage = 1
      this.loadRequestList()
    }
  },
  mounted() {
    this.loadFriendGroups()
    this.loadFriendList()
    this.loadRequestList()
  },
  methods: {
    mapFriend(raw) {
      const user = raw.user || raw.friend || {}
      const group = raw.group || {}
      return {
        id: raw.id || raw.friendId || raw.userId || user.id,
        avatar: pickAvatarUrl(raw.avatarUrl, raw.avatar, user.avatarUrl, user.avatar),
        nickname: raw.nickname || raw.username || user.nickname || user.username || '未知用户',
        remark: raw.remark || '',
        groupId: raw.groupId || group.id || '',
        groupName: raw.groupName || group.name || '',
        joinTime: raw.joinTime || raw.createdAt || raw.createTime || null,
        lastContact: raw.lastContact || raw.lastContactTime || raw.updatedAt || null,
        interactionCount: raw.interactionCount || raw.chatCount || raw.messageCount || 0,
        isSpecial: Boolean(raw.isSpecial || raw.specialFocus),
        editingRemark: false,
        tempRemark: raw.remark || ''
      }
    },
    mapRequest(raw) {
      const user = raw.user || raw.applicant || {}
      return {
        id: raw.id || raw.requestId,
        avatar: pickAvatarUrl(raw.avatarUrl, raw.avatar, user.avatarUrl, user.avatar),
        nickname: raw.nickname || raw.username || user.nickname || user.username || '未知用户',
        message: raw.message || raw.applyMessage || raw.remark || '',
        applyTime: raw.applyTime || raw.createdAt || raw.createTime || null,
        handleTime: raw.handleTime || raw.updatedAt || null,
        status: raw.status || 'pending'
      }
    },
    mapGroup(raw) {
      return {
        id: raw.id || raw.groupId,
        name: raw.name || raw.groupName || '未命名分组',
        friendCount: raw.friendCount || raw.count || raw.memberCount || 0,
        description: raw.description || ''
      }
    },
    buildFriendParams() {
      return {
        page: this.friendPagination.currentPage,
        size: this.friendPagination.pageSize,
        pageNum: this.friendPagination.currentPage,
        pageSize: this.friendPagination.pageSize,
        groupId: this.friendFilter.group || undefined,
        status: this.friendFilter.status || undefined,
        keyword: this.friendFilter.keyword || undefined
      }
    },
    buildRequestParams() {
      const range = this.requestFilter.dateRange || []
      return {
        page: this.requestPagination.currentPage,
        size: this.requestPagination.pageSize,
        pageNum: this.requestPagination.currentPage,
        pageSize: this.requestPagination.pageSize,
        status: this.requestFilter.status || undefined,
        startDate: toDateString(range[0]),
        endDate: toDateString(range[1])
      }
    },
    async loadFriendList() {
      this.friendLoading = true
      try {
        const response = await getFriendList(this.buildFriendParams())
        const pageData = normalizePage(response)
        this.friendList = pageData.list.map(this.mapFriend)
        this.friendPagination.total = pageData.total
      } catch (error) {
        this.friendList = []
        this.friendPagination.total = 0
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载好友列表失败')
      } finally {
        this.friendLoading = false
      }
    },
    async loadRequestList() {
      this.requestLoading = true
      try {
        const response = await getFriendRequests(this.buildRequestParams())
        const pageData = normalizePage(response)
        this.requestList = pageData.list.map(this.mapRequest)
        this.requestPagination.total = pageData.total
      } catch (error) {
        this.requestList = []
        this.requestPagination.total = 0
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载好友申请列表失败')
      } finally {
        this.requestLoading = false
      }
    },
    async loadFriendGroups() {
      try {
        const response = await getFriendGroups()
        const data = unwrapResponse(response)
        const list = Array.isArray(data)
          ? data
          : Array.isArray(data && data.list)
            ? data.list
            : []
        this.friendGroups = list.map(this.mapGroup)
      } catch (error) {
        this.friendGroups = []
        this.$message.error((error.response && error.response.data && error.response.data.message) || '加载好友分组失败')
      }
    },
    handleTabClick(tab) {
      this.activeTab = tab.name
      if (tab.name === 'friendList') {
        this.loadFriendList()
      }
      if (tab.name === 'friendRequest') {
        this.loadRequestList()
      }
      if (tab.name === 'groupManage') {
        this.loadFriendGroups()
      }
    },
    handleFriendSearch() {
      this.friendPagination.currentPage = 1
      this.loadFriendList()
    },
    refreshFriendList() {
      this.friendPagination.currentPage = 1
      this.loadFriendList()
    },
    refreshRequestList() {
      this.requestPagination.currentPage = 1
      this.loadRequestList()
    },
    handleFriendSizeChange(size) {
      this.friendPagination.pageSize = size
      this.loadFriendList()
    },
    handleFriendCurrentChange(page) {
      this.friendPagination.currentPage = page
      this.loadFriendList()
    },
    handleRequestSizeChange(size) {
      this.requestPagination.pageSize = size
      this.loadRequestList()
    },
    handleRequestCurrentChange(page) {
      this.requestPagination.currentPage = page
      this.loadRequestList()
    },
    handleAddFriend() {
      this.addFriendForm = {
        userId: '',
        message: '',
        groupId: this.friendGroups[0] ? this.friendGroups[0].id : ''
      }
      this.addFriendDialogVisible = true
    },
    async handleConfirmAddFriend() {
      try {
        await sendFriendRequest({ ...this.addFriendForm })
        this.$message.success('好友申请发送成功')
        this.addFriendDialogVisible = false
        this.loadRequestList()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '发送好友申请失败')
      }
    },
    handleEditRemark(friend) {
      friend.editingRemark = true
      friend.tempRemark = friend.remark || ''
    },
    async handleSaveRemark(friend) {
      const nextRemark = friend.tempRemark || ''
      try {
        if (nextRemark !== (friend.remark || '')) {
          await updateFriendRemark(friend.id, { remark: nextRemark })
          friend.remark = nextRemark
        }
        friend.editingRemark = false
        this.$message.success('备注保存成功')
      } catch (error) {
        friend.editingRemark = false
        this.$message.error((error.response && error.response.data && error.response.data.message) || '保存备注失败')
      }
    },
    async handleChangeGroup(friend) {
      try {
        await moveFriendGroup(friend.id, { groupId: friend.groupId })
        const group = this.friendGroups.find(item => item.id === friend.groupId)
        friend.groupName = group ? group.name : ''
        this.$message.success('分组修改成功')
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '修改分组失败')
      }
    },
    async handleToggleSpecial(friend) {
      try {
        await toggleSpecialFocus(friend.id, { specialFocus: !friend.isSpecial })
        friend.isSpecial = !friend.isSpecial
        this.$message.success(friend.isSpecial ? '已设为特别关注' : '已取消特别关注')
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '操作失败')
      }
    },
    handleSendMessage(friend) {
      this.$message.info(`准备和 ${friend.nickname} 聊天`)
    },
    handleViewProfile(friend) {
      this.$message.info(`查看 ${friend.nickname} 的资料`)
    },
    async handleRemoveFriend(friend) {
      try {
        await this.$confirm(`确定要删除好友 ${friend.nickname} 吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteFriend(friend.id)
        this.$message.success('好友删除成功')
        this.loadFriendList()
        this.loadFriendGroups()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '删除好友失败')
        }
      }
    },
    async handleApproveRequest(request) {
      try {
        await approveFriendRequest(request.id)
        this.$message.success('好友申请已同意')
        this.loadRequestList()
        this.loadFriendList()
        this.loadFriendGroups()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '同意申请失败')
      }
    },
    async handleRejectRequest(request) {
      try {
        await rejectFriendRequest(request.id)
        this.$message.success('好友申请已拒绝')
        this.loadRequestList()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '拒绝申请失败')
      }
    },
    handleViewApplicant(request) {
      this.$message.info(`查看 ${request.nickname} 的资料`)
    },
    handleAddGroup() {
      this.groupForm = {
        id: '',
        name: '',
        description: ''
      }
      this.groupDialogTitle = '添加分组'
      this.groupDialogVisible = true
    },
    handleEditGroup(group) {
      this.groupForm = {
        id: group.id,
        name: group.name,
        description: group.description || ''
      }
      this.groupDialogTitle = '编辑分组'
      this.groupDialogVisible = true
    },
    async handleConfirmGroup() {
      try {
        if (this.groupForm.id) {
          await updateFriendGroup(this.groupForm.id, {
            name: this.groupForm.name,
            description: this.groupForm.description
          })
          this.$message.success('分组编辑成功')
        } else {
          await createFriendGroup({
            name: this.groupForm.name,
            description: this.groupForm.description
          })
          this.$message.success('分组添加成功')
        }
        this.groupDialogVisible = false
        this.loadFriendGroups()
        this.loadFriendList()
      } catch (error) {
        this.$message.error((error.response && error.response.data && error.response.data.message) || '分组操作失败')
      }
    },
    async handleDeleteGroup(group) {
      try {
        await this.$confirm(`确定要删除分组 "${group.name}" 吗？分组内好友将移动到默认分组。`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteFriendGroup(group.id)
        this.$message.success('分组删除成功')
        this.loadFriendGroups()
        this.loadFriendList()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error((error.response && error.response.data && error.response.data.message) || '删除分组失败')
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
    getRequestStatusType(status) {
      const typeMap = {
        pending: 'warning',
        approved: 'success',
        rejected: 'danger'
      }
      return typeMap[status] || 'info'
    },
    getRequestStatusText(status) {
      const textMap = {
        pending: '待处理',
        approved: '已同意',
        rejected: '已拒绝'
      }
      return textMap[status] || status
    }
  }
}
</script>

<style scoped>
.circle-friend {
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

.table-card {
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
}

.nickname {
  font-weight: 500;
}

.remark-text {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 3px;
}

.remark-text:hover {
  background-color: #f5f7fa;
}

.applicant-info {
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.group-card {
  margin-bottom: 20px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.group-item {
  height: 120px;
}

.group-info {
  text-align: center;
}

.group-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}

.group-count {
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
}

.group-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
