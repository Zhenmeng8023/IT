<template>
  <div class="circle-console">
    <div class="page-header">
      <div>
        <div class="page-header__eyebrow">圈子后台</div>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
      <div class="page-header__meta">
        <el-tag size="small" type="info">统一管理台</el-tag>
        <el-tag size="small" :type="defaultLifecycle ? 'warning' : 'success'">
          {{ defaultLifecycle ? '默认聚焦待处理' : '全量管理视图' }}
        </el-tag>
        <span class="page-header__summary">{{ activeFilterSummary }}</span>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="filter-toolbar">
        <div class="filter-toolbar__main">
          <el-select
            v-model="filterForm.status"
            clearable
            placeholder="圈子状态"
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已关闭" value="close" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>

          <el-select
            v-model="filterForm.privacy"
            clearable
            placeholder="可见范围"
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option label="公开" value="public" />
            <el-option label="私密" value="private" />
            <el-option label="需审核" value="approval" />
          </el-select>

          <el-date-picker
            v-model="filterForm.dateRange"
            clearable
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px"
            @change="handleSearch"
          />
        </div>

        <div class="filter-toolbar__side">
          <el-input
            data-testid="circle-manage-search-input"
            v-model="filterForm.keyword"
            clearable
            placeholder="搜索圈子名、创建人或简介"
            prefix-icon="el-icon-search"
            style="width: 280px"
            @input="handleSearchInput"
            @clear="handleSearch"
            @keyup.enter.native="handleSearch"
          />
          <el-button data-testid="circle-manage-search-submit" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh-left" @click="handleResetFilters">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card__icon is-blue">
            <i class="el-icon-office-building" />
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">圈子总数</div>
            <div class="stat-card__value">{{ stats.totalCircles }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card__icon is-green">
            <i class="el-icon-user-solid" />
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">成员总数</div>
            <div class="stat-card__value">{{ stats.totalMembers }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card__icon is-orange">
            <i class="el-icon-document-copy" />
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">帖子总数</div>
            <div class="stat-card__value">{{ stats.totalPosts }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card__icon is-red">
            <i class="el-icon-data-analysis" />
          </div>
          <div class="stat-card__content">
            <div class="stat-card__label">今日活跃</div>
            <div class="stat-card__value">{{ stats.todayActive }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar__left">
          <el-button
            v-permission="'btn:circle-manage:create'"
            type="primary"
            icon="el-icon-plus"
            @click="handleCreateCircle"
          >
            新建圈子
          </el-button>
          <el-button
            v-permission="'btn:circle-audit:batch-approve'"
            icon="el-icon-check"
            :disabled="pendingSelectionCount === 0"
            @click="handleBatchApprove"
          >
            批量通过
          </el-button>
          <el-button
            v-permission="'btn:circle-audit:batch-close'"
            icon="el-icon-switch-button"
            :disabled="approvedSelectionCount === 0"
            @click="handleBatchClose"
          >
            批量关闭
          </el-button>
          <el-button
            v-permission="'btn:circle-audit:batch-delete'"
            type="danger"
            plain
            icon="el-icon-delete"
            :disabled="selectedCircles.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <div class="toolbar__right">
          <span class="toolbar__summary">已选 {{ selectedCircles.length }} 项</span>
          <el-button icon="el-icon-refresh" @click="refreshData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="section-heading">
        <div>
          <h2>圈子列表</h2>
          <p>统一展示真实生命周期状态、成员数据与帖子数据。</p>
        </div>
        <div class="section-heading__meta">共 {{ pagination.total }} 条</div>
      </div>

      <el-table
        data-testid="circle-manage-table"
        v-loading="loading"
        :data="circleList"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />

        <el-table-column label="圈子信息" min-width="260">
          <template slot-scope="scope">
            <div class="circle-cell" :data-testid="`circle-row-${scope.row.id}`">
              <el-avatar :size="44" :src="scope.row.avatar">
                {{ (scope.row.name || '?').slice(0, 1) }}
              </el-avatar>
              <div class="circle-cell__body">
                <div class="circle-cell__title" :data-testid="`circle-row-name-${scope.row.id}`">
                  <span>{{ scope.row.name || '未命名圈子' }}</span>
                  <el-tag size="mini" :type="getLifecycleTagType(scope.row.type)">
                    {{ getLifecycleText(scope.row.type) }}
                  </el-tag>
                  <el-tag size="mini" effect="plain" :type="getVisibilityTagType(scope.row.visibility)">
                    {{ getVisibilityText(scope.row.visibility) }}
                  </el-tag>
                </div>
                <div class="circle-cell__desc">{{ scope.row.description || '暂无圈子简介' }}</div>
                <div class="circle-cell__meta">
                  <span>创建人：{{ scope.row.creatorName }}</span>
                  <span>创建时间：{{ formatDate(scope.row.createdAt) }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="成员 / 帖子" width="140" align="center">
          <template slot-scope="scope">
            <div class="metric-pair">
              <span>{{ scope.row.memberCount }} 成员</span>
              <span>{{ scope.row.postCount }} 帖子</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="活跃" width="120" align="center">
          <template slot-scope="scope">
            <div class="metric-pair">
              <span>{{ scope.row.todayActive }} 今日活跃</span>
              <span>{{ scope.row.maxMembers || '未设上限' }} 成员上限</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="360" fixed="right">
          <template slot-scope="scope">
            <div class="row-actions">
              <el-button
                :data-testid="`circle-row-detail-${scope.row.id}`"
                v-permission="'btn:circle-audit:view'"
                size="mini"
                type="primary"
                plain
                @click="handleViewDetail(scope.row)"
              >
                详情
              </el-button>
              <el-button
                :data-testid="`circle-row-members-${scope.row.id}`"
                v-permission="'btn:circle-audit:member-manage'"
                size="mini"
                type="text"
                @click="handleMemberManage(scope.row)"
              >
                成员
              </el-button>
              <el-button
                :data-testid="`circle-row-posts-${scope.row.id}`"
                v-permission="'btn:circle-audit:post-manage'"
                size="mini"
                type="text"
                @click="handlePostManage(scope.row)"
              >
                帖子
              </el-button>
              <el-button
                :data-testid="`circle-row-approve-${scope.row.id}`"
                v-permission="'btn:circle-audit:approve'"
                v-if="scope.row.type === 'pending'"
                size="mini"
                type="text"
                class="action-success"
                @click="handleApprove(scope.row)"
              >
                通过
              </el-button>
              <el-button
                :data-testid="`circle-row-reject-${scope.row.id}`"
                v-permission="'btn:circle-audit:reject'"
                v-if="scope.row.type === 'pending'"
                size="mini"
                type="text"
                class="action-danger"
                @click="handleRejectCircle(scope.row)"
              >
                拒绝
              </el-button>
              <el-button
                :data-testid="`circle-row-close-${scope.row.id}`"
                v-permission="'btn:circle-audit:close'"
                v-if="scope.row.type === 'approved'"
                size="mini"
                type="text"
                class="action-warning"
                @click="handleCloseCircle(scope.row)"
              >
                关闭
              </el-button>
              <el-button
                :data-testid="`circle-row-edit-${scope.row.id}`"
                v-permission="'btn:circle-manage:edit'"
                size="mini"
                type="text"
                @click="handleEditCircle(scope.row)"
              >
                编辑
              </el-button>
              <el-button
                :data-testid="`circle-row-delete-${scope.row.id}`"
                v-permission="'btn:circle-manage:delete'"
                size="mini"
                type="text"
                class="action-danger"
                @click="handleDeleteCircle(scope.row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          :current-page="pagination.currentPage"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      :title="detailDialogTitle"
      :visible.sync="detailDialogVisible"
      width="84%"
      :close-on-click-modal="false"
      @close="handleCloseDetail"
    >
      <div v-if="currentCircle" class="detail-panel">
        <div class="detail-hero">
          <div class="detail-hero__main">
            <el-avatar :size="56" :src="currentCircle.avatar">
              {{ (currentCircle.name || '?').slice(0, 1) }}
            </el-avatar>
            <div>
              <div class="detail-hero__title">
                <span>{{ currentCircle.name }}</span>
                <el-tag size="small" :type="getLifecycleTagType(currentCircle.type)">
                  {{ getLifecycleText(currentCircle.type) }}
                </el-tag>
                <el-tag size="small" effect="plain" :type="getVisibilityTagType(currentCircle.visibility)">
                  {{ getVisibilityText(currentCircle.visibility) }}
                </el-tag>
              </div>
              <div class="detail-hero__desc">{{ currentCircle.description || '暂无圈子简介' }}</div>
              <div class="detail-hero__meta">
                <span>创建人：{{ currentCircle.creatorName }}</span>
                <span>创建时间：{{ formatDate(currentCircle.createdAt) }}</span>
              </div>
            </div>
          </div>
          <div class="detail-hero__actions">
            <el-button
              v-if="currentCircle.type === 'pending'"
              size="mini"
              type="success"
              plain
              @click="handleApprove(currentCircle)"
            >
              通过
            </el-button>
            <el-button
              v-if="currentCircle.type === 'pending'"
              size="mini"
              type="danger"
              plain
              @click="handleRejectCircle(currentCircle)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="currentCircle.type === 'approved'"
              size="mini"
              plain
              @click="handleCloseCircle(currentCircle)"
            >
              关闭
            </el-button>
            <el-button size="mini" type="danger" plain @click="handleDeleteCircle(currentCircle)">
              删除
            </el-button>
          </div>
        </div>

        <el-row :gutter="12" class="detail-stat-grid">
          <el-col :span="6">
            <div class="detail-stat">
              <div class="detail-stat__label">成员</div>
              <div class="detail-stat__value">{{ currentCircle.memberCount }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="detail-stat">
              <div class="detail-stat__label">帖子</div>
              <div class="detail-stat__value">{{ currentCircle.postCount }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="detail-stat">
              <div class="detail-stat__label">今日活跃</div>
              <div class="detail-stat__value">{{ currentCircle.todayActive }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="detail-stat">
              <div class="detail-stat__label">成员上限</div>
              <div class="detail-stat__value">{{ currentCircle.maxMembers || '未设' }}</div>
            </div>
          </el-col>
        </el-row>

        <el-tabs v-model="detailTab">
          <el-tab-pane label="基础信息" name="overview">
            <el-descriptions :column="2" border class="detail-descriptions">
              <el-descriptions-item label="圈子 ID">{{ currentCircle.id }}</el-descriptions-item>
              <el-descriptions-item label="生命周期">{{ getLifecycleText(currentCircle.type) }}</el-descriptions-item>
              <el-descriptions-item label="可见范围">{{ getVisibilityText(currentCircle.visibility) }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ currentCircle.creatorName }}</el-descriptions-item>
              <el-descriptions-item label="成员数量">{{ currentCircle.memberCount }}</el-descriptions-item>
              <el-descriptions-item label="帖子数量">{{ currentCircle.postCount }}</el-descriptions-item>
              <el-descriptions-item label="今日活跃">{{ currentCircle.todayActive }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDate(currentCircle.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatDate(currentCircle.updatedAt) }}</el-descriptions-item>
              <el-descriptions-item label="成员上限">{{ currentCircle.maxMembers || '未设置' }}</el-descriptions-item>
            </el-descriptions>
            <div class="detail-section">
              <h3>圈子简介</h3>
              <p>{{ currentCircle.description || '暂无圈子简介' }}</p>
            </div>
          </el-tab-pane>

          <el-tab-pane label="成员数据" name="members">
            <div class="detail-section">
              <div class="section-heading section-heading--compact">
                <div>
                  <h3>成员列表</h3>
                  <p>统一展示圈主、管理员与普通成员角色。</p>
                </div>
                <div class="section-heading__meta">共 {{ memberList.length }} 人</div>
              </div>

              <el-table v-loading="memberLoading" :data="memberList" border stripe>
                <el-table-column label="成员" min-width="220">
                  <template slot-scope="scope">
                    <div class="member-cell">
                      <el-avatar :size="36" :src="scope.row.avatar">
                        {{ (scope.row.nickname || '?').slice(0, 1) }}
                      </el-avatar>
                      <div>
                        <div class="member-cell__name">{{ scope.row.nickname }}</div>
                        <div class="member-cell__meta">
                          {{ scope.row.username || scope.row.userId || '未知账号' }}
                        </div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="角色" width="120" align="center">
                  <template slot-scope="scope">
                    <el-tag size="small" :type="getMemberRoleTagType(scope.row.role)">
                      {{ getMemberRoleText(scope.row.role) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="加入时间" min-width="160" align="center">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.joinTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="最近活跃" min-width="160" align="center">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.lastActive) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template slot-scope="scope">
                    <el-button
                      v-permission="'btn:circle-audit:set-admin'"
                      size="mini"
                      type="text"
                      :disabled="scope.row.role === 'creator'"
                      @click="handleSetAdmin(scope.row)"
                    >
                      设为管理员
                    </el-button>
                    <el-button
                      v-permission="'btn:circle-audit:remove-member'"
                      size="mini"
                      type="text"
                      class="action-danger"
                      :disabled="scope.row.role === 'creator'"
                      @click="handleRemoveMember(scope.row)"
                    >
                      移除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="帖子管理" name="posts">
            <div class="detail-section">
              <div class="section-heading section-heading--compact">
                <div>
                  <h3>帖子列表</h3>
                  <p>统一展示帖子状态、作者、互动数据与管理动作。</p>
                </div>
                <div class="section-heading__meta">共 {{ postList.length }} 篇</div>
              </div>

              <el-table data-testid="circle-post-table" v-loading="postLoading" :data="postList" border stripe>
                <el-table-column label="帖子内容" min-width="280">
                  <template slot-scope="scope">
                    <div class="post-cell" :data-testid="`circle-post-row-${scope.row.id}`">
                      <div class="post-cell__title" :data-testid="`circle-post-title-${scope.row.id}`">{{ scope.row.title }}</div>
                      <div class="post-cell__desc">{{ scope.row.content || '暂无正文' }}</div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="作者" width="160">
                  <template slot-scope="scope">
                    <div class="member-cell">
                      <el-avatar :size="32" :src="scope.row.authorAvatar">
                        {{ (scope.row.authorName || '?').slice(0, 1) }}
                      </el-avatar>
                      <div>
                        <div class="member-cell__name">{{ scope.row.authorName }}</div>
                        <div class="member-cell__meta">ID {{ scope.row.authorId || '-' }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="110" align="center">
                  <template slot-scope="scope">
                    <el-tag size="small" :type="getPostStatusTagType(scope.row.status)">
                      {{ getPostStatusText(scope.row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="互动" width="120" align="center">
                  <template slot-scope="scope">
                    <div class="metric-pair">
                      <span>{{ scope.row.commentCount }} 评论</span>
                      <span>{{ scope.row.likes }} 点赞</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="发布时间" min-width="160" align="center">
                  <template slot-scope="scope">
                    {{ formatDate(scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                  <template slot-scope="scope">
                    <el-button :data-testid="`circle-post-view-${scope.row.id}`" size="mini" type="text" @click="handleViewPost(scope.row)">查看</el-button>
                    <el-button
                      :data-testid="`circle-post-approve-${scope.row.id}`"
                      v-permission="'btn:circle-audit:approve-post'"
                      v-if="scope.row.status === 'pending'"
                      size="mini"
                      type="text"
                      class="action-success"
                      @click="handleApprovePost(scope.row)"
                    >
                      通过
                    </el-button>
                    <el-button
                      :data-testid="`circle-post-delete-${scope.row.id}`"
                      size="mini"
                      type="text"
                      class="action-danger"
                      @click="handleDeletePost(scope.row)"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-dialog
      title="帖子详情"
      :visible.sync="postDetailDialogVisible"
      width="640px"
      :close-on-click-modal="false"
    >
      <div v-if="currentPost" class="post-detail">
        <div class="post-detail__title">{{ currentPost.title }}</div>
        <div class="post-detail__meta">
          <span>作者：{{ currentPost.authorName }}</span>
          <span>状态：{{ getPostStatusText(currentPost.status) }}</span>
          <span>发布时间：{{ formatDate(currentPost.createdAt) }}</span>
        </div>
        <div class="post-detail__content">{{ currentPost.content || '暂无正文' }}</div>
      </div>
    </el-dialog>

    <el-dialog
      :title="circleDialogTitle"
      :visible.sync="circleDialogVisible"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="circleForm" :model="circleForm" :rules="circleRules" label-width="92px">
        <el-form-item label="圈子名称" prop="name">
          <el-input v-model.trim="circleForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="圈子简介" prop="description">
          <el-input
            v-model.trim="circleForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="可见范围" prop="visibility">
          <el-select v-model="circleForm.visibility" style="width: 100%">
            <el-option label="公开" value="public" />
            <el-option label="私密" value="private" />
            <el-option label="需审核" value="approval" />
          </el-select>
        </el-form-item>
        <el-form-item label="成员上限" prop="maxMembers">
          <el-input-number
            v-model="circleForm.maxMembers"
            :min="1"
            :max="10000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="头像地址" prop="avatar">
          <el-input v-model.trim="circleForm.avatar" placeholder="可选，填写圈子头像 URL" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="circleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCircle">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  approveCircle,
  approveCirclePost,
  batchApproveCircles,
  batchCloseCircles,
  batchDeleteCircles,
  closeCircle,
  createCircle,
  deleteCircle,
  deleteCirclePost,
  getCircleManageList,
  getCircleManageStats,
  getCircleMembers,
  getCirclePosts,
  normalizeCircleManagePage,
  parseCircleManageError,
  rejectCircle,
  removeCircleMember,
  requireCircleManageData,
  setCircleAdmin,
  updateCircle
} from '@/api/circleManage'
import { pickAvatarUrl } from '@/utils/avatar'

function toSafeNumber(value, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function buildCirclePayload(form) {
  const payload = {
    name: form.name ? form.name.trim() : '',
    description: form.description ? form.description.trim() : '',
    visibility: form.visibility || 'public',
    maxMembers: form.maxMembers,
    avatar: form.avatar ? form.avatar.trim() : ''
  }

  return Object.keys(payload).reduce((accumulator, key) => {
    const value = payload[key]
    if (value !== null && value !== undefined && value !== '') {
      accumulator[key] = value
    }
    return accumulator
  }, {})
}

export default {
  name: 'CircleManageConsole',
  layout: 'manage',
  props: {
    pageTitle: {
      type: String,
      default: '圈子管理'
    },
    pageSubtitle: {
      type: String,
      default: '统一处理圈子列表、详情、成员数据、帖子管理与生命周期操作。'
    },
    defaultLifecycle: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      filterForm: {
        status: this.defaultLifecycle || '',
        privacy: '',
        keyword: '',
        dateRange: []
      },
      searchTimer: null,
      stats: {
        totalCircles: 0,
        totalMembers: 0,
        totalPosts: 0,
        todayActive: 0
      },
      loading: false,
      circleList: [],
      selectedCircles: [],
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      detailDialogVisible: false,
      detailTab: 'overview',
      currentCircle: null,
      memberLoading: false,
      memberList: [],
      postLoading: false,
      postList: [],
      postDetailDialogVisible: false,
      currentPost: null,
      circleDialogVisible: false,
      circleDialogTitle: '新建圈子',
      circleForm: {
        id: null,
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: 500,
        avatar: ''
      },
      circleRules: {
        name: [
          { required: true, message: '请输入圈子名称', trigger: 'blur' },
          { max: 100, message: '圈子名称不能超过 100 个字符', trigger: 'blur' }
        ],
        description: [
          { max: 1000, message: '圈子简介不能超过 1000 个字符', trigger: 'blur' }
        ],
        visibility: [
          { required: true, message: '请选择可见范围', trigger: 'change' }
        ],
        maxMembers: [
          { type: 'number', min: 1, max: 10000, message: '成员上限需在 1-10000 之间', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    pendingSelectionCount() {
      return this.selectedCircles.filter(circle => circle.type === 'pending').length
    },
    approvedSelectionCount() {
      return this.selectedCircles.filter(circle => circle.type === 'approved').length
    },
    activeFilterSummary() {
      const summaries = []
      if (this.filterForm.status) {
        summaries.push(`状态：${this.getLifecycleText(this.filterForm.status)}`)
      }
      if (this.filterForm.privacy) {
        summaries.push(`可见范围：${this.getVisibilityText(this.filterForm.privacy)}`)
      }
      if (this.filterForm.keyword) {
        summaries.push(`关键词：${this.filterForm.keyword}`)
      }
      if (Array.isArray(this.filterForm.dateRange) && this.filterForm.dateRange.length === 2) {
        summaries.push(`日期：${this.filterForm.dateRange[0]} 至 ${this.filterForm.dateRange[1]}`)
      }
      return summaries.length ? summaries.join(' · ') : '当前未设置筛选条件'
    },
    detailDialogTitle() {
      return this.currentCircle ? `${this.currentCircle.name} - 详情` : '圈子详情'
    }
  },
  mounted() {
    this.refreshData()
  },
  beforeDestroy() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
      this.searchTimer = null
    }
  },
  methods: {
    normalizeCircle(raw) {
      const creatorInfo = raw.creatorInfo || {}
      const visibility = raw.visibility || raw.privacy || 'public'
      const lifecycle = raw.type || 'pending'

      return {
        id: raw.id,
        name: raw.name || '',
        description: raw.description || '',
        type: lifecycle,
        status: raw.status || lifecycle,
        visibility,
        privacy: visibility,
        avatar: pickAvatarUrl(raw.avatar, raw.avatarUrl),
        creatorId: raw.creatorId || creatorInfo.id || null,
        creatorName: raw.creatorName || raw.creator || creatorInfo.nickname || creatorInfo.username || '未知用户',
        creatorAvatar: pickAvatarUrl(raw.creatorAvatarUrl, raw.creatorAvatar, creatorInfo.avatarUrl, creatorInfo.avatar),
        memberCount: toSafeNumber(raw.memberCount),
        postCount: toSafeNumber(raw.postCount),
        todayActive: toSafeNumber(raw.todayActive, toSafeNumber(raw.activeMemberCount)),
        maxMembers: raw.maxMembers || null,
        createdAt: raw.createdAt || raw.createTime || null,
        updatedAt: raw.updatedAt || null
      }
    },
    normalizeMember(raw) {
      return {
        id: raw.id,
        circleId: raw.circleId || null,
        userId: raw.userId || null,
        username: raw.username || '',
        nickname: raw.nickname || raw.username || '未知用户',
        avatar: pickAvatarUrl(raw.avatarUrl, raw.avatar),
        role: (raw.role || 'member').toLowerCase(),
        joinTime: raw.joinTime || null,
        lastActive: raw.lastActive || raw.joinTime || null
      }
    },
    normalizePost(raw) {
      const content = raw.content || ''
      return {
        id: raw.id,
        circleId: raw.circleId || null,
        authorId: raw.authorId || null,
        authorName: raw.authorName || raw.author || '未知用户',
        authorAvatar: pickAvatarUrl(raw.authorAvatarUrl, raw.authorAvatar),
        title: raw.title || (content ? String(content).slice(0, 30) : '无标题'),
        content,
        status: raw.status || 'pending',
        commentCount: toSafeNumber(raw.commentCount),
        likes: toSafeNumber(raw.likes),
        createdAt: raw.createdAt || raw.createTime || null
      }
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
      if (this.filterForm.keyword) {
        params.keyword = this.filterForm.keyword.trim()
      }
      if (Array.isArray(this.filterForm.dateRange) && this.filterForm.dateRange.length === 2) {
        params.startDate = this.filterForm.dateRange[0]
        params.endDate = this.filterForm.dateRange[1]
      }
      return params
    },
    async loadStats() {
      try {
        const payload = await getCircleManageStats()
        const data = requireCircleManageData(payload, '加载统计信息失败') || {}
        this.stats = {
          totalCircles: toSafeNumber(data.totalCircles),
          totalMembers: toSafeNumber(data.totalMembers),
          totalPosts: toSafeNumber(data.totalPosts),
          todayActive: toSafeNumber(data.todayActive, toSafeNumber(data.activeMembers))
        }
      } catch (error) {
        this.stats = {
          totalCircles: 0,
          totalMembers: 0,
          totalPosts: 0,
          todayActive: 0
        }
        this.$message.error(parseCircleManageError(error, '加载统计信息失败'))
      }
    },
    async loadCircleList() {
      this.loading = true
      try {
        const payload = await getCircleManageList(this.buildListParams())
        const pageData = normalizeCircleManagePage(payload, '加载圈子列表失败')
        this.circleList = pageData.list.map(this.normalizeCircle)
        this.pagination.total = pageData.total
        this.pagination.currentPage = pageData.currentPage
        if (pageData.pageSize > 0) {
          this.pagination.pageSize = pageData.pageSize
        }
      } catch (error) {
        this.circleList = []
        this.pagination.total = 0
        this.$message.error(parseCircleManageError(error, '加载圈子列表失败'))
      } finally {
        this.loading = false
      }
    },
    async loadMemberList(circleId) {
      this.memberLoading = true
      try {
        const payload = await getCircleMembers(circleId)
        const data = requireCircleManageData(payload, '加载成员列表失败')
        this.memberList = (Array.isArray(data) ? data : []).map(this.normalizeMember)
        if (this.currentCircle && this.currentCircle.id === circleId) {
          this.currentCircle.memberCount = this.memberList.length
        }
      } catch (error) {
        this.memberList = []
        this.$message.error(parseCircleManageError(error, '加载成员列表失败'))
      } finally {
        this.memberLoading = false
      }
    },
    async loadPostList(circleId) {
      this.postLoading = true
      try {
        const payload = await getCirclePosts(circleId)
        const data = requireCircleManageData(payload, '加载帖子列表失败')
        this.postList = (Array.isArray(data) ? data : []).map(this.normalizePost)
        if (this.currentCircle && this.currentCircle.id === circleId) {
          this.currentCircle.postCount = this.postList.length
        }
      } catch (error) {
        this.postList = []
        this.$message.error(parseCircleManageError(error, '加载帖子列表失败'))
      } finally {
        this.postLoading = false
      }
    },
    handleSelectionChange(selection) {
      this.selectedCircles = selection
    },
    handleSearchInput() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.searchTimer = setTimeout(() => {
        this.handleSearch()
      }, 300)
    },
    handleSearch() {
      this.pagination.currentPage = 1
      this.loadCircleList()
    },
    handleResetFilters() {
      this.filterForm = {
        status: this.defaultLifecycle || '',
        privacy: '',
        keyword: '',
        dateRange: []
      }
      this.handleSearch()
    },
    refreshData() {
      this.selectedCircles = []
      return Promise.all([this.loadStats(), this.loadCircleList()])
    },
    handleSizeChange(size) {
      this.pagination.pageSize = size
      this.loadCircleList()
    },
    handleCurrentChange(page) {
      this.pagination.currentPage = page
      this.loadCircleList()
    },
    async openDetail(circle, tab) {
      this.currentCircle = { ...circle }
      this.detailTab = tab
      this.detailDialogVisible = true
      if (tab === 'members') {
        await this.loadMemberList(circle.id)
        return
      }
      if (tab === 'posts') {
        await this.loadPostList(circle.id)
        return
      }
      await Promise.all([this.loadMemberList(circle.id), this.loadPostList(circle.id)])
    },
    handleViewDetail(circle) {
      this.openDetail(circle, 'overview')
    },
    handleMemberManage(circle) {
      this.openDetail(circle, 'members')
    },
    handlePostManage(circle) {
      this.openDetail(circle, 'posts')
    },
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.detailTab = 'overview'
      this.currentCircle = null
      this.memberList = []
      this.postList = []
    },
    handleCreateCircle() {
      this.circleForm = {
        id: null,
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: 500,
        avatar: ''
      }
      this.circleDialogTitle = '新建圈子'
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
        visibility: circle.visibility || 'public',
        maxMembers: circle.maxMembers || 500,
        avatar: circle.avatar || ''
      }
      this.circleDialogTitle = '编辑圈子'
      this.circleDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.circleForm) {
          this.$refs.circleForm.clearValidate()
        }
      })
    },
    validateCircleForm() {
      return new Promise((resolve) => {
        if (!this.$refs.circleForm) {
          resolve(false)
          return
        }
        this.$refs.circleForm.validate(valid => resolve(valid))
      })
    },
    async handleConfirmCircle() {
      const valid = await this.validateCircleForm()
      if (!valid) {
        return
      }

      try {
        const payload = buildCirclePayload(this.circleForm)
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
        this.$message.error(parseCircleManageError(error, '圈子保存失败'))
      }
    },
    async handleApprove(circle) {
      if (circle.type !== 'pending') {
        this.$message.warning('只有待审核圈子才能执行通过')
        return
      }
      try {
        await this.$confirm('确定要通过该圈子的审核吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await requireCircleManageData(await approveCircle(circle.id), '审核通过失败')
        this.$message.success('审核通过成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '审核通过失败'))
        }
      }
    },
    async handleRejectCircle(circle) {
      if (circle.type !== 'pending') {
        this.$message.warning('只有待审核圈子才能拒绝')
        return
      }
      try {
        await this.$confirm('确定要拒绝该圈子的审核吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await requireCircleManageData(await rejectCircle(circle.id), '拒绝审核失败')
        this.$message.success('拒绝审核成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '拒绝审核失败'))
        }
      }
    },
    async handleCloseCircle(circle) {
      if (circle.type !== 'approved') {
        this.$message.warning('只有已通过圈子才能关闭')
        return
      }
      try {
        await this.$confirm('确定要关闭该圈子吗？关闭后圈子将不可继续使用。', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await requireCircleManageData(await closeCircle(circle.id), '关闭圈子失败')
        this.$message.success('圈子关闭成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '关闭圈子失败'))
        }
      }
    },
    async handleDeleteCircle(circle) {
      try {
        await this.$confirm('确定要删除该圈子吗？此操作不可恢复。', '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        await requireCircleManageData(await deleteCircle(circle.id), '删除圈子失败')
        this.$message.success('圈子删除成功')
        this.detailDialogVisible = false
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '删除圈子失败'))
        }
      }
    },
    async handleSetAdmin(member) {
      if (member.role === 'creator') {
        this.$message.warning('圈主不能通过此处修改角色')
        return
      }
      try {
        await this.$confirm(`确定将 ${member.nickname} 设为管理员吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await requireCircleManageData(await setCircleAdmin(member.id, 'admin'), '设置管理员失败')
        this.$message.success('设置管理员成功')
        if (this.currentCircle) {
          this.loadMemberList(this.currentCircle.id)
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '设置管理员失败'))
        }
      }
    },
    async handleRemoveMember(member) {
      if (member.role === 'creator') {
        this.$message.warning('圈主不能被移除')
        return
      }
      try {
        await this.$confirm(`确定将 ${member.nickname} 移出圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await requireCircleManageData(await removeCircleMember(member.id), '移除成员失败')
        this.$message.success('成员移除成功')
        if (this.currentCircle) {
          await Promise.all([
            this.loadMemberList(this.currentCircle.id),
            this.loadCircleList(),
            this.loadStats()
          ])
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '移除成员失败'))
        }
      }
    },
    handleViewPost(post) {
      this.currentPost = { ...post }
      this.postDetailDialogVisible = true
    },
    async handleApprovePost(post) {
      if (post.status !== 'pending') {
        this.$message.warning('只有待审核帖子才能通过')
        return
      }
      try {
        await requireCircleManageData(await approveCirclePost(post.id), '帖子审核通过失败')
        this.$message.success('帖子审核通过成功')
        if (this.currentCircle) {
          await Promise.all([
            this.loadPostList(this.currentCircle.id),
            this.loadCircleList(),
            this.loadStats()
          ])
        }
      } catch (error) {
        this.$message.error(parseCircleManageError(error, '帖子审核通过失败'))
      }
    },
    async handleDeletePost(post) {
      try {
        await this.$confirm(`确定要删除帖子“${post.title}”吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await requireCircleManageData(await deleteCirclePost(post.id), '删除帖子失败')
        this.$message.success('帖子删除成功')
        if (this.currentCircle) {
          await Promise.all([
            this.loadPostList(this.currentCircle.id),
            this.loadCircleList(),
            this.loadStats()
          ])
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '删除帖子失败'))
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
      const pendingCircles = this.selectedCircles.filter(circle => circle.type === 'pending')
      const skippedCount = this.selectedCircles.length - pendingCircles.length
      if (!pendingCircles.length) {
        this.$message.warning('请至少选择一个待审核圈子')
        return
      }

      try {
        await this.$confirm(`确定批量通过 ${pendingCircles.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await batchApproveCircles(pendingCircles.map(circle => circle.id))
        const result = this.normalizeBatchResult(requireCircleManageData(payload, '批量通过失败'), pendingCircles.length)
        this.$message.success(`批量通过完成：成功 ${result.successCount}，失败 ${result.failedCount}`)
        if (skippedCount > 0) {
          this.$message.warning(`已跳过 ${skippedCount} 个非待审核圈子`)
        }
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '批量通过失败'))
        }
      }
    },
    async handleBatchClose() {
      const approvedCircles = this.selectedCircles.filter(circle => circle.type === 'approved')
      const skippedCount = this.selectedCircles.length - approvedCircles.length
      if (!approvedCircles.length) {
        this.$message.warning('请至少选择一个已通过圈子')
        return
      }

      try {
        await this.$confirm(`确定批量关闭 ${approvedCircles.length} 个圈子吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const payload = await batchCloseCircles(approvedCircles.map(circle => circle.id))
        const result = this.normalizeBatchResult(requireCircleManageData(payload, '批量关闭失败'), approvedCircles.length)
        this.$message.success(`批量关闭完成：成功 ${result.successCount}，失败 ${result.failedCount}`)
        if (skippedCount > 0) {
          this.$message.warning(`已跳过 ${skippedCount} 个非已通过圈子`)
        }
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '批量关闭失败'))
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
        await this.$confirm(`确定批量删除 ${circleIds.length} 个圈子吗？此操作不可恢复。`, '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'error',
          confirmButtonClass: 'el-button--danger'
        })
        const payload = await batchDeleteCircles(circleIds)
        const result = this.normalizeBatchResult(requireCircleManageData(payload, '批量删除失败'), circleIds.length)
        this.$message.success(`批量删除完成：成功 ${result.successCount}，失败 ${result.failedCount}`)
        this.refreshData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(parseCircleManageError(error, '批量删除失败'))
        }
      }
    },
    formatDate(value) {
      if (!value) {
        return '-'
      }
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return '-'
      }
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    getLifecycleTagType(type) {
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
      return textMap[type] || type || '-'
    },
    getVisibilityTagType(visibility) {
      const typeMap = {
        public: 'success',
        private: 'info',
        approval: 'warning'
      }
      return typeMap[visibility] || 'info'
    },
    getVisibilityText(visibility) {
      const textMap = {
        public: '公开',
        private: '私密',
        approval: '需审核'
      }
      return textMap[visibility] || visibility || '-'
    },
    getMemberRoleTagType(role) {
      const typeMap = {
        creator: 'danger',
        admin: 'warning',
        moderator: '',
        member: 'info'
      }
      return typeMap[role] || 'info'
    },
    getMemberRoleText(role) {
      const textMap = {
        creator: '圈主',
        admin: '管理员',
        moderator: '版主',
        member: '成员'
      }
      return textMap[role] || role || '-'
    },
    getPostStatusTagType(status) {
      const typeMap = {
        pending: 'warning',
        published: 'success',
        deleted: 'info'
      }
      return typeMap[status] || 'info'
    },
    getPostStatusText(status) {
      const textMap = {
        pending: '待审核',
        published: '已发布',
        deleted: '已删除'
      }
      return textMap[status] || status || '-'
    }
  }
}
</script>

<style scoped>
.circle-console {
  padding: 20px;
  background: linear-gradient(180deg, #f6f8fc 0%, #ffffff 160px);
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 8px 0 6px;
  color: #1f2d3d;
  font-size: 28px;
}

.page-header p {
  margin: 0;
  color: #6b7280;
  line-height: 1.8;
}

.page-header__eyebrow {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.12);
  color: #2b6cb0;
  font-size: 12px;
  font-weight: 600;
}

.page-header__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 220px;
}

.page-header__summary {
  color: #8b95a7;
  font-size: 12px;
  text-align: right;
  line-height: 1.6;
}

.filter-card,
.toolbar-card,
.table-card {
  margin-bottom: 16px;
  border-radius: 18px;
}

.filter-toolbar,
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-toolbar__main,
.filter-toolbar__side,
.toolbar__left,
.toolbar__right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar__summary,
.section-heading__meta {
  color: #6b7280;
  font-size: 13px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 18px;
  border: 1px solid #e6edf7;
}

.stat-card /deep/ .el-card__body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #fff;
  font-size: 22px;
}

.stat-card__icon.is-blue {
  background: linear-gradient(135deg, #4f8cff, #2d6cdf);
}

.stat-card__icon.is-green {
  background: linear-gradient(135deg, #4ebc7a, #20915c);
}

.stat-card__icon.is-orange {
  background: linear-gradient(135deg, #f7b34d, #e2871f);
}

.stat-card__icon.is-red {
  background: linear-gradient(135deg, #ff8378, #df4b4b);
}

.stat-card__label {
  color: #7a8496;
  font-size: 13px;
}

.stat-card__value {
  margin-top: 6px;
  color: #1f2937;
  font-size: 28px;
  font-weight: 700;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.section-heading h2,
.section-heading h3 {
  margin: 0;
  color: #1f2937;
}

.section-heading p {
  margin: 6px 0 0;
  color: #8b95a7;
  font-size: 13px;
}

.section-heading--compact {
  margin-bottom: 12px;
}

.circle-cell,
.member-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.circle-cell__body {
  min-width: 0;
  flex: 1;
}

.circle-cell__title {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 6px;
  color: #1f2937;
  font-weight: 600;
}

.circle-cell__desc,
.post-cell__desc {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.circle-cell__meta,
.detail-hero__meta,
.post-detail__meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 8px;
  color: #8b95a7;
  font-size: 12px;
}

.metric-pair {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.action-success {
  color: #1f9d62;
}

.action-warning {
  color: #d97706;
}

.action-danger {
  color: #dc2626;
}

.pagination-container {
  margin-top: 16px;
  text-align: right;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid #e8eef7;
  border-radius: 18px;
  background: linear-gradient(180deg, #fbfcff, #f4f8ff);
}

.detail-hero__main {
  display: flex;
  gap: 14px;
}

.detail-hero__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
}

.detail-hero__desc {
  margin-top: 8px;
  color: #526072;
  line-height: 1.8;
}

.detail-hero__actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-stat-grid {
  margin-bottom: 6px;
}

.detail-stat {
  padding: 16px;
  border: 1px solid #e8eef7;
  border-radius: 16px;
  background: #fff;
}

.detail-stat__label {
  color: #8b95a7;
  font-size: 12px;
}

.detail-stat__value {
  margin-top: 8px;
  color: #1f2937;
  font-size: 24px;
  font-weight: 700;
}

.detail-descriptions {
  margin-bottom: 16px;
}

.detail-section h3 {
  margin: 0 0 8px;
  color: #1f2937;
}

.detail-section p {
  margin: 0;
  color: #526072;
  line-height: 1.8;
}

.member-cell__name,
.post-cell__title,
.post-detail__title {
  color: #1f2937;
  font-weight: 600;
}

.member-cell__meta {
  margin-top: 4px;
  color: #8b95a7;
  font-size: 12px;
}

.post-detail__content {
  margin-top: 14px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fafc;
  color: #334155;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .page-header,
  .detail-hero {
    flex-direction: column;
  }

  .page-header__meta {
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .circle-console {
    padding: 14px;
  }

  .page-header h1 {
    font-size: 24px;
  }

  .stats-row /deep/ .el-col,
  .detail-stat-grid /deep/ .el-col {
    width: 100%;
  }
}
</style>
