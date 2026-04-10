<template>
  <div v-if="pageReady" class="project-manage-page">
    <div class="manage-header">
      <div>
        <div class="header-top">
          <el-button type="text" icon="el-icon-arrow-left" @click="goToDetail">返回项目详情</el-button>
        </div>
        <h1 class="page-title">{{ project.title || '项目管理' }}</h1>
        <p class="page-subtitle">{{ project.description || '在这里管理任务、成员、文件、活动流和项目设置。' }}</p>
      </div>
      <div class="header-actions">
        <el-button icon="el-icon-setting" @click="goToSettingsTab('basic')">项目设置</el-button>
        <el-button type="primary" icon="el-icon-folder-add" @click="openSaveAsTemplate">保存为模板</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="manage-tabs">
      <el-tab-pane label="概览" name="overview"></el-tab-pane>
      <el-tab-pane v-if="canSeeTaskCollaboration" :label="`任务协作 (${tasks.length})`" name="task-manage"></el-tab-pane>
      <el-tab-pane :label="`成员 (${members.length})`" name="member-manage"></el-tab-pane>
      <el-tab-pane :label="`文件 (${files.length})`" name="file-manage"></el-tab-pane>
      <el-tab-pane :label="`文档 (${docCount})`" name="doc-manage"></el-tab-pane>
      <el-tab-pane :label="`活动流 (${activityTotal})`" name="activity-manage"></el-tab-pane>
      <el-tab-pane label="里程碑" name="milestone-manage"></el-tab-pane>
      <el-tab-pane label="Sprint" name="sprint-manage"></el-tab-pane>
      <el-tab-pane label="发布记录" name="release-manage"></el-tab-pane>
      <el-tab-pane label="下载记录" name="download-manage"></el-tab-pane>
      <el-tab-pane label="统计分析" name="stat-manage"></el-tab-pane>
      <el-tab-pane label="设置" name="settings"></el-tab-pane>
    </el-tabs>

    <div v-if="activeTab === 'overview'" class="tab-panel">
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-number">{{ tasks.length }}</div>
            <div class="stat-label">项目任务</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-number">{{ members.length }}</div>
            <div class="stat-label">项目成员</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-number">{{ files.length }}</div>
            <div class="stat-label">项目文件</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card">
            <div class="stat-number">{{ activityTotal }}</div>
            <div class="stat-label">项目活动</div>
          </div>
        </el-col>
      </el-row>

      <el-card shadow="never" class="feature-entry-card">
        <div slot="header" class="card-header">
          <span>新增能力快捷入口</span>
          <span class="feature-entry-tip">这部分只新增入口，不改动你原来的任务协作、成员管理、文件管理、项目设置逻辑。</span>
        </div>
        <div class="feature-entry-grid">
          <div class="feature-entry-item" @click="activeTab = 'milestone-manage'">
            <div class="feature-entry-title">里程碑</div>
            <div class="feature-entry-desc">查看阶段目标、截止时间、完成状态。</div>
          </div>
          <div class="feature-entry-item" @click="activeTab = 'sprint-manage'">
            <div class="feature-entry-title">Sprint</div>
            <div class="feature-entry-desc">查看当前迭代目标和时间范围。</div>
          </div>
          <div class="feature-entry-item" @click="activeTab = 'release-manage'">
            <div class="feature-entry-title">发布记录</div>
            <div class="feature-entry-desc">创建版本、绑定文件、发布与归档。</div>
          </div>
          <div class="feature-entry-item" @click="activeTab = 'download-manage'">
            <div class="feature-entry-title">下载记录</div>
            <div class="feature-entry-desc">查看下载摘要和下载明细。</div>
          </div>
          <div class="feature-entry-item" @click="activeTab = 'stat-manage'">
            <div class="feature-entry-title">统计分析</div>
            <div class="feature-entry-desc">查看浏览、下载、星标和日报趋势。</div>
          </div>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="16">
          <el-card shadow="never">
            <div slot="header" class="card-header">
              <span>最近活动</span>
              <el-button type="text" @click="loadRecentActivities">刷新</el-button>
            </div>
            <div v-if="recentActivities.length > 0" class="activity-list">
              <div v-for="item in recentActivities" :key="item.id" class="activity-item is-clickable" @click="handleOverviewActivityClick(item)">
                <div class="activity-left">
                  <el-avatar :size="32" :src="item.operatorAvatar || item.avatar"></el-avatar>
                </div>
                <div class="activity-right">
                  <div class="activity-title">{{ item.actionLabel || item.title }}</div>
                  <div class="activity-desc">{{ item.content || item.description }}</div>
                </div>
                <div class="activity-time">{{ formatTime(item.createdAt || item.time) }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无最近活动"></el-empty>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="side-card">
            <div slot="header" class="card-header"><span>项目信息</span></div>
            <div class="info-list">
              <div class="info-item"><span>状态</span><el-tag size="mini" :type="getProjectStatusType(project.status)">{{ project.statusText || '-' }}</el-tag></div>
              <div class="info-item"><span>可见性</span><span>{{ project.visibility || '-' }}</span></div>
              <div class="info-item"><span>分类</span><span>{{ project.category || '-' }}</span></div>
              <div class="info-item"><span>更新时间</span><span>{{ formatTime(project.updateTime) }}</span></div>
              <div class="info-item"><span>标签</span><span>{{ (project.tags || []).join('、') || '-' }}</span></div>
            </div>
          </el-card>
          <el-card shadow="never" class="side-card">
            <div slot="header" class="card-header"><span>贡献者</span></div>
            <div v-if="contributors.length > 0" class="contributor-list">
              <div v-for="member in contributors" :key="member.id" class="contributor-item">
                <el-avatar :size="30" :src="member.avatar"></el-avatar>
                <div class="contributor-text">
                  <div class="contributor-name">{{ member.name }}</div>
                  <div class="contributor-role">{{ getMemberRoleText(member.role) }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无成员"></el-empty>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div v-if="canSeeTaskCollaboration && activeTab === 'my-tasks'" class="tab-panel">
      <el-card shadow="never">
        <div slot="header" class="card-header">
          <span>我的任务</span>
          <el-button type="text" @click="loadMyTasks">刷新</el-button>
        </div>
        <el-table :data="myTasks" border>
          <el-table-column prop="title" label="任务标题" min-width="220"></el-table-column>
          <el-table-column prop="priority" label="优先级" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getTaskPriorityType(scope.row.priority)">{{ getTaskPriorityText(scope.row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getTaskStatusType(scope.row.status)">{{ getTaskStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dueDate" label="截止时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.dueDate) }}</template>
          </el-table-column>
          <el-table-column label="快速状态更新" width="180">
            <template slot-scope="scope">
              <el-select size="mini" :value="scope.row.status" :disabled="!canQuickUpdateTaskStatus(scope.row)" @change="changeTaskStatus(scope.row.id, $event)">
                <el-option label="待处理" value="todo"></el-option>
                <el-option label="进行中" value="in_progress"></el-option>
                <el-option label="已完成" value="done"></el-option>
              </el-select>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="myTasks.length === 0" description="当前没有分配给你的任务"></el-empty>
      </el-card>
    </div>

    <div v-if="canSeeTaskCollaboration && activeTab === 'task-manage'" class="tab-panel">
      <el-card shadow="never">
        <div slot="header" class="card-header task-card-header">
          <div class="task-header-left">
            <span>任务协作面板</span>
            <span class="task-header-desc">按状态、优先级、负责人快速筛选，并直接在列表里更新状态和负责人。</span>
          </div>
          <div class="toolbar-actions">
            <el-input v-model="taskFilter.keyword" size="small" clearable placeholder="搜索任务标题或描述" class="toolbar-input"></el-input>
            <el-select v-model="taskFilter.status" size="small" placeholder="状态" class="toolbar-select">
              <el-option label="全部状态" value="all"></el-option>
              <el-option label="待处理" value="todo"></el-option>
              <el-option label="进行中" value="in_progress"></el-option>
              <el-option label="已完成" value="done"></el-option>
            </el-select>
            <el-select v-model="taskFilter.priority" size="small" placeholder="优先级" class="toolbar-select">
              <el-option label="全部优先级" value="all"></el-option>
              <el-option label="低" value="low"></el-option>
              <el-option label="中" value="medium"></el-option>
              <el-option label="高" value="high"></el-option>
              <el-option label="紧急" value="urgent"></el-option>
            </el-select>
            <el-select v-model="taskFilter.assigneeId" size="small" placeholder="负责人" class="toolbar-select toolbar-select-wide">
              <el-option label="全部负责人" value="all"></el-option>
              <el-option label="未分配" value="unassigned"></el-option>
              <el-option v-for="member in taskAssigneeOptions" :key="member.userId" :label="member.name" :value="member.userId"></el-option>
            </el-select>
            <el-select v-model="taskFilter.sortBy" size="small" placeholder="排序字段" class="toolbar-select">
              <el-option label="最近更新" value="updatedAt"></el-option>
              <el-option label="创建时间" value="createdAt"></el-option>
              <el-option label="截止时间" value="dueDate"></el-option>
              <el-option label="优先级" value="priority"></el-option>
              <el-option label="标题" value="title"></el-option>
            </el-select>
            <el-select v-model="taskFilter.sortOrder" size="small" placeholder="排序方向" class="toolbar-select">
              <el-option label="倒序" value="desc"></el-option>
              <el-option label="正序" value="asc"></el-option>
            </el-select>
            <el-button size="small" @click="taskFilter.assigneeId = currentUserId || 'all'">只看我的</el-button>
            <el-button size="small" @click="taskFilter.status = 'todo'">只看待办</el-button>
            <el-button size="small" @click="taskFilter.status = 'in_progress'">只看进行中</el-button>
            <el-button size="small" icon="el-icon-refresh" @click="loadTasks">刷新</el-button>
            <el-button size="small" @click="resetTaskFilters">重置</el-button>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreateTaskDialog">新建任务</el-button>
          </div>
        </div>

        <div class="task-summary-grid">
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.total }}</div>
            <div class="task-summary-label">当前列表任务</div>
          </div>
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.todo }}</div>
            <div class="task-summary-label">待处理</div>
          </div>
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.inProgress }}</div>
            <div class="task-summary-label">进行中</div>
          </div>
          <div class="task-summary-card">
            <div class="task-summary-value">{{ taskStats.done }}</div>
            <div class="task-summary-label">已完成</div>
          </div>
          <div class="task-summary-card danger">
            <div class="task-summary-value">{{ taskStats.overdue }}</div>
            <div class="task-summary-label">已逾期</div>
          </div>
        </div>

        <el-table :data="filteredTasks" border>
          <el-table-column label="任务信息" min-width="280">
            <template slot-scope="scope">
              <div class="task-title-cell">
                <div class="task-title-main">
                  <el-button type="text" class="task-title-link" @click="openTaskCollab(scope.row, 'comment')">{{ scope.row.title }}</el-button>
                  <el-tag v-if="isTaskOverdue(scope.row)" size="mini" type="danger" effect="plain">已逾期</el-tag>
                </div>
                <div v-if="scope.row.description" class="task-title-desc">{{ scope.row.description }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="负责人" width="190">
            <template slot-scope="scope">
              <div class="task-assignee-cell">
                <el-avatar :size="30" :src="scope.row.assigneeAvatar || ''">{{ (scope.row.assigneeName || '未').slice(0, 1) }}</el-avatar>
                <div class="task-assignee-text">
                  <div class="task-assignee-name">{{ scope.row.assigneeName || '未分配' }}</div>
                  <div v-if="scope.row.creatorName" class="task-assignee-meta">创建人：{{ scope.row.creatorName }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getTaskPriorityType(scope.row.priority)">{{ getTaskPriorityText(scope.row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110">
            <template slot-scope="scope">
              <el-tag size="mini" :type="getTaskStatusType(scope.row.status)">{{ getTaskStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dueDate" label="截止时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.dueDate) || '-' }}</template>
          </el-table-column>
          <el-table-column label="快捷更新" min-width="260">
            <template slot-scope="scope">
              <div class="task-quick-actions">
                <el-select size="mini" :value="scope.row.status" class="task-inline-select" :disabled="!canQuickUpdateTaskStatus(scope.row)" @change="changeTaskStatus(scope.row.id, $event)">
                  <el-option label="待处理" value="todo"></el-option>
                  <el-option label="进行中" value="in_progress"></el-option>
                  <el-option label="已完成" value="done"></el-option>
                </el-select>
                <el-select size="mini" :value="scope.row.assigneeId" placeholder="切换负责人" class="task-inline-select" :disabled="!taskAssigneeOptions.length || !canQuickUpdateTaskAssignee(scope.row)" @change="changeTaskAssignee(scope.row.id, $event)">
                  <el-option v-for="member in taskAssigneeOptions" :key="'quick-' + member.userId" :label="member.name" :value="member.userId"></el-option>
                </el-select>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" plain @click="openTaskCollab(scope.row, 'comment')">协作详情</el-button>
              <el-button v-if="canEditTaskRow(scope.row)" size="mini" @click="openEditTaskDialog(scope.row)">编辑</el-button>
              <el-button v-if="canDeleteTaskRow(scope.row)" size="mini" type="danger" @click="deleteTask(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="filteredTasks.length === 0" description="当前筛选条件下没有任务"></el-empty>
      </el-card>
    </div>

    <div v-if="activeTab === 'member-manage'" class="tab-panel">
      <el-card shadow="never">
        <div slot="header" class="card-header">
          <span>成员管理</span>
          <div class="toolbar-actions">
            <el-input
              v-model="memberFilter.keyword"
              size="small"
              clearable
              placeholder="搜索成员昵称/用户名"
              class="toolbar-input"
            ></el-input>

            <el-badge v-if="canManageProject" :value="pendingJoinRequestCount" :hidden="!pendingJoinRequestCount">
              <el-button size="small" icon="el-icon-document" @click="openJoinRequestDialog">
                加入申请
              </el-button>
            </el-badge>

            <el-button
              v-if="canManageProject"
              type="primary"
              size="small"
              icon="el-icon-plus"
              @click="openAddMemberDialog"
            >
              添加成员
            </el-button>

            <el-button type="warning" size="small" icon="el-icon-switch-button" @click="quitProject">
              退出项目
            </el-button>
          </div>
        </div>
        <el-table :data="filteredMembers" border :row-class-name="memberRowClassName">
          <el-table-column prop="name" label="昵称" min-width="140"></el-table-column>
          <el-table-column prop="username" label="用户名" min-width="160"></el-table-column>
          <el-table-column prop="role" label="角色" width="180">
            <template slot-scope="scope">
              <div class="member-role-cell">
                <el-tag v-if="isSelfMember(scope.row)" size="mini" type="info">{{ getMemberRoleText(scope.row.role) }} · 我自己</el-tag>
                <el-tag v-else-if="isReadonlyMember(scope.row)" size="mini" :type="scope.row.role === 'owner' ? 'danger' : 'info'">{{ getMemberRoleText(scope.row.role) }}</el-tag>
                <el-select v-else-if="canEditMemberRole(scope.row)" v-model="scope.row.role" size="mini" style="width: 130px" :loading="roleSavingMemberId === scope.row.memberId" :disabled="roleSavingMemberId === scope.row.memberId" @change="value => updateMemberRoleInline(scope.row, value)">
                  <el-option label="成员" value="member"></el-option>
                  <el-option label="管理员" value="admin"></el-option>
                  <el-option label="查看者" value="viewer"></el-option>
                </el-select>
                <el-tag v-else size="mini" :type="scope.row.role === 'owner' ? 'danger' : 'info'">{{ getMemberRoleText(scope.row.role) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="joinTime" label="加入时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.joinTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template slot-scope="scope">
              <el-button v-if="canRemoveMember(scope.row)" size="mini" type="danger" @click="deleteMember(scope.row)">移除</el-button>
              <span v-else class="member-row-readonly-text">{{ isSelfMember(scope.row) ? '不可操作自己' : '不可操作' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="canManageProject" shadow="never" class="member-extra-card">
        <div slot="header" class="card-header">
          <span>协作申请审核</span>
          <div class="toolbar-actions">
            <el-input v-model="joinRequestFilter.keyword" size="small" clearable placeholder="搜索申请人/留言" class="toolbar-input"></el-input>
            <el-select v-model="joinRequestFilter.status" size="small" clearable class="toolbar-input" style="width: 140px">
              <el-option label="待处理" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
            <el-button size="small" icon="el-icon-refresh" @click="loadJoinRequests">刷新</el-button>
          </div>
        </div>
        <el-table :data="filteredJoinRequests" border v-loading="joinRequestLoading">
          <el-table-column prop="applicantName" label="申请人" min-width="140" />
          <el-table-column prop="desiredRole" label="申请角色" width="120">
            <template slot-scope="scope">{{ getMemberRoleText(scope.row.desiredRole || 'member') }}</template>
          </el-table-column>
          <el-table-column prop="applyMessage" label="申请说明" min-width="220" />
          <el-table-column prop="status" label="状态" width="110">
            <template slot-scope="scope"><el-tag size="mini" :type="getJoinRequestStatusTag(scope.row.status)">{{ getJoinRequestStatusText(scope.row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="180"><template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template></el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template slot-scope="scope">
              <template v-if="scope.row.status === 'pending'">
                <el-button size="mini" type="success" :loading="joinRequestAuditLoadingId === scope.row.id" @click="handleJoinRequestAudit(scope.row, true)">通过</el-button>
                <el-button size="mini" type="danger" plain :loading="joinRequestAuditLoadingId === scope.row.id" @click="handleJoinRequestAudit(scope.row, false)">拒绝</el-button>
              </template>
              <span v-else class="member-row-readonly-text">已处理</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="canManageProject" shadow="never" class="member-extra-card">
        <div slot="header" class="card-header">
          <span>待接受邀请</span>
          <div class="toolbar-actions">
            <el-input v-model="invitationFilter.keyword" size="small" clearable placeholder="搜索被邀请人/留言" class="toolbar-input"></el-input>
            <el-select v-model="invitationFilter.status" size="small" clearable class="toolbar-input" style="width: 140px">
              <el-option label="待接受" value="pending" />
              <el-option label="已接受" value="accepted" />
              <el-option label="已拒绝" value="rejected" />
              <el-option label="已撤销" value="cancelled" />
            </el-select>
            <el-button size="small" icon="el-icon-refresh" @click="loadProjectInvitations">刷新</el-button>
          </div>
        </div>
        <el-table :data="filteredProjectInvitations" border v-loading="invitationLoading">
          <el-table-column prop="inviteeName" label="被邀请人" min-width="140" />
          <el-table-column prop="inviteRole" label="邀请角色" width="120"><template slot-scope="scope">{{ getMemberRoleText(scope.row.inviteRole || 'member') }}</template></el-table-column>
          <el-table-column prop="inviteMessage" label="邀请留言" min-width="220" />
          <el-table-column prop="status" label="状态" width="110"><template slot-scope="scope"><el-tag size="mini" :type="getInvitationStatusTag(scope.row.status)">{{ getInvitationStatusText(scope.row.status) }}</el-tag></template></el-table-column>
          <el-table-column prop="createdAt" label="邀请时间" width="180"><template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template></el-table-column>
          <el-table-column prop="expiredAt" label="过期时间" width="180"><template slot-scope="scope">{{ formatTime(scope.row.expiredAt) }}</template></el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button v-if="scope.row.status === 'pending'" size="mini" type="danger" @click="cancelInvitation(scope.row)">撤销</el-button>
              <span v-else class="member-row-readonly-text">不可操作</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <div v-if="activeTab === 'file-manage'" class="tab-panel">
      <el-card shadow="never">
        <div slot="header" class="card-header">
          <span>文件管理</span>
          <div class="toolbar-actions">
            <el-input v-model="fileFilter.keyword" size="small" clearable placeholder="搜索文件名" class="toolbar-input"></el-input>
            <el-button type="danger" size="small" icon="el-icon-delete" :disabled="!selectedFileRows.length" @click="batchDeleteProjectFiles">
              批量删除{{ selectedFileRows.length ? '（' + selectedFileRows.length + '）' : '' }}
            </el-button>
            <el-button type="primary" size="small" icon="el-icon-upload" @click="openUploadFileDialog">上传文件</el-button>
            <el-button size="small" icon="el-icon-refresh" @click="loadFiles">刷新</el-button>
          </div>
        </div>
        <el-table ref="fileTableRef" :data="filteredFiles" border>
          <el-table-column width="55" align="center">
            <template slot="header">
              <el-checkbox
                :value="isAllFilteredFilesSelected"
                :indeterminate="isFileSelectionIndeterminate"
                @change="toggleAllFileSelection"
              />
            </template>
            <template slot-scope="scope">
              <el-checkbox
                :value="isFileSelected(scope.row)"
                @change="value => toggleFileSelection(scope.row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="fileName" label="文件名" min-width="240"></el-table-column>
          <el-table-column prop="version" label="当前版本" width="120"></el-table-column>
          <el-table-column prop="fileSizeBytes" label="大小" width="120">
            <template slot-scope="scope">{{ formatFileSize(scope.row.fileSizeBytes) }}</template>
          </el-table-column>
          <el-table-column prop="isMain" label="主文件" width="100">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.isMain ? 'success' : 'info'">{{ scope.row.isMain ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="uploadTime" label="上传时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.uploadTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="360" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" @click="downloadProjectFile(scope.row)">下载</el-button>
              <el-button size="mini" @click="viewFileVersions(scope.row)">版本</el-button>
              <el-button size="mini" @click="openUploadNewVersionDialog(scope.row)">新版本</el-button>
              <el-button size="mini" type="warning" @click="setMainProjectFile(scope.row)" :disabled="scope.row.isMain">设主文件</el-button>
              <el-button size="mini" type="danger" @click="deleteProjectFile(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <div v-if="activeTab === 'doc-manage'" class="tab-panel">
      <ProjectDocList
        ref="projectDocListRef"
        :project-id="projectId"
        :initial-doc-id="$route.query.docId || null"
        :initial-mode="$route.query.mode || 'view'"
        @count-change="docCount = $event"
        @changed="handleDocChanged"
        @primary-changed="handleDocPrimaryChanged"
      />
    </div>
<div v-if="activeTab === 'activity-manage'" class="tab-panel">
  <ProjectActivityManagePanel
    :project-id="projectId"
    :initial-activity-id="$route.query.activityId ? Number($route.query.activityId) : null"
    :member-options="members.filter(item => item && item.userId)"
    @total-change="activityTotal = $event"
  />
</div>

<div v-if="activeTab === 'milestone-manage'" class="tab-panel">
  <ProjectMilestoneManage
    :project-id="projectId"
    :can-manage-project="canManageProject"
  />
</div>

<div v-if="activeTab === 'sprint-manage'" class="tab-panel">
  <ProjectSprintManage
    :project-id="projectId"
    :can-manage-project="canManageProject"
  />
</div>

<div v-if="activeTab === 'release-manage'" class="tab-panel">
  <ProjectReleaseManage
    :project-id="projectId"
    :can-manage-project="canManageProject"
  />
</div>

<div v-if="activeTab === 'download-manage'" class="tab-panel">
  <ProjectDownloadRecordManage
    :project-id="projectId"
  />
</div>

<div v-if="activeTab === 'stat-manage'" class="tab-panel">
  <ProjectStatManage
    :project-id="projectId"
  />
</div>

<div v-if="activeTab === 'settings'" class="tab-panel">
  <el-row :gutter="16">
    <el-col :xs="24" :lg="10">
      <el-card shadow="never" class="side-card">
        <div slot="header" class="card-header"><span>项目设置</span></div>
        <div class="info-list">
          <div class="info-item"><span>项目名称</span><span>{{ project.title || '-' }}</span></div>
          <div class="info-item"><span>分类</span><span>{{ project.category || '-' }}</span></div>
          <div class="info-item"><span>可见性</span><span>{{ project.visibility || '-' }}</span></div>
        </div>
        <div class="toolbar-actions settings-actions-row">
          <el-button size="small" icon="el-icon-setting" @click="openSettingsDialog">编辑项目设置</el-button>
          <el-button size="small" type="primary" icon="el-icon-folder-add" @click="openSaveAsTemplate">保存为模板</el-button>
          <el-button v-if="canManageProject" size="small" type="danger" icon="el-icon-delete" :loading="deleteProjectLoading" @click="confirmDeleteProject">删除项目</el-button>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="24" :lg="14">
      <el-card shadow="never" class="side-card">
        <div slot="header" class="card-header"><span>说明</span></div>
        <div class="settings-tip-box">
          <div class="settings-tip-title">这里统一收口项目治理类操作</div>
          <div class="settings-tip-desc">当前统一收口项目设置、成员治理、模板治理与删除项目等操作，避免把无关后台字段直接暴露到前端。</div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</div>

<ProjectTaskCollabDrawer
      :visible.sync="taskCollabDrawerVisible"
      :task="selectedTaskForCollab"
      :project-id="projectId"
      :current-user-id="currentUserId"
      :current-member-joined-at="currentMemberRecord && currentMemberRecord.joinedAt ? currentMemberRecord.joinedAt : ''"
      :can-manage-project="canManageProject"
      :active-tab.sync="taskCollabActiveTab"
      :refresh-seed="taskCollabRefreshSeed"
      @changed="handleTaskCollabChanged"
      @close="handleTaskCollabDrawerClosed"
    />

    <el-dialog :title="taskDialogTitle" :visible.sync="taskDialogVisible" width="600px" @close="resetTaskForm">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="任务标题"><el-input v-model="taskForm.title" placeholder="请输入任务标题"></el-input></el-form-item>
        <el-form-item label="任务描述"><el-input v-model="taskForm.description" type="textarea" :rows="4"></el-input></el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="taskForm.assigneeId" style="width: 100%" :disabled="taskDialogType === 'edit' && !canManageProject">
            <el-option v-for="member in members" :key="member.userId" :label="member.name" :value="member.userId"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="taskForm.priority" style="width: 100%">
            <el-option label="低" value="low"></el-option>
            <el-option label="中" value="medium"></el-option>
            <el-option label="高" value="high"></el-option>
            <el-option label="紧急" value="urgent"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="taskForm.status" style="width: 100%" :disabled="taskDialogType === 'edit' && taskDialogOriginalTask && taskDialogOriginalTask.status === 'done' && !canManageProject">
            <el-option label="待处理" value="todo"></el-option>
            <el-option label="进行中" value="in_progress"></el-option>
            <el-option label="已完成" value="done"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间"><el-date-picker v-model="taskForm.dueDate" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择截止时间" style="width: 100%"></el-date-picker></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="邀请成员" :visible.sync="inviteDialogVisible" width="520px" @close="resetInviteForm">
      <el-form :model="invitationForm" label-width="90px">
        <el-form-item label="搜索用户">
          <el-select v-model="invitationForm.inviteeId" filterable remote clearable reserve-keyword placeholder="输入用户昵称或用户名搜索" :remote-method="searchInviteUsers" :loading="inviteUserSearchLoading" style="width: 100%">
            <el-option v-for="user in inviteUserOptions" :key="user.id" :label="buildNewMemberUserLabel(user)" :value="user.id">
              <div class="member-user-option">
                <div class="member-user-option__title">{{ user.nickname || user.username || ('用户' + user.id) }}</div>
                <div class="member-user-option__desc">{{ user.username || '-' }} · ID {{ user.id }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <div v-if="selectedInviteUser" class="selected-user-card">
          <el-avatar :size="34" :src="selectedInviteUser.avatarUrl || ''">{{ (selectedInviteUser.nickname || selectedInviteUser.username || 'U').slice(0, 1) }}</el-avatar>
          <div class="selected-user-card__text">
            <div class="selected-user-card__name">{{ selectedInviteUser.nickname || selectedInviteUser.username || ('用户' + selectedInviteUser.id) }}</div>
            <div class="selected-user-card__meta">{{ selectedInviteUser.username || '-' }} · ID {{ selectedInviteUser.id }}</div>
          </div>
        </div>
        <el-form-item label="角色">
          <el-select v-model="invitationForm.inviteRole" style="width: 100%">
            <el-option label="成员" value="member"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="查看者" value="viewer"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="邀请留言">
          <el-input v-model="invitationForm.inviteMessage" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="可选，告诉对方为什么邀请他加入" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="invitationForm.expireDays" style="width: 100%">
            <el-option label="3天" :value="3" />
            <el-option label="7天" :value="7" />
            <el-option label="15天" :value="15" />
          </el-select>
        </el-form-item>
        <div class="dialog-tip">搜索用户后不再直接拉入项目，而是发送邀请，等待对方在项目列表或我的项目页接受后才正式加入。</div>
      </el-form>
      <span slot="footer">
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendingInvitation" @click="sendProjectInvitation">{{ sendingInvitation ? '发送中...' : '发送邀请' }}</el-button>
      </span>
    </el-dialog>

    <el-dialog title="上传项目文件" :visible.sync="fileUploadDialogVisible" width="520px" @close="resetFileUploadForm">
      <el-form :model="fileUploadForm" label-width="100px">
        <el-form-item label="版本号"><el-input v-model="fileUploadForm.version" placeholder="例如：1.0"></el-input></el-form-item>
        <el-form-item label="版本说明"><el-input v-model="fileUploadForm.commitMessage" type="textarea" :rows="3"></el-input></el-form-item>
        <el-form-item label="是否主文件"><el-switch v-model="fileUploadForm.isMain"></el-switch></el-form-item>
        <el-form-item label="选择文件"><input type="file" @change="handleUploadFileChange" class="native-file-input"></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="fileUploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fileUploadLoading" @click="submitUploadFile">上传</el-button>
      </span>
    </el-dialog>

    <el-dialog title="文件版本记录" :visible.sync="fileVersionsDialogVisible" width="680px">
      <el-table :data="fileVersions" border v-loading="fileVersionsLoading">
        <el-table-column prop="version" label="版本号" width="120"></el-table-column>
        <el-table-column prop="commitMessage" label="版本说明" min-width="220"></el-table-column>
        <el-table-column prop="fileSizeBytes" label="大小" width="120"><template slot-scope="scope">{{ formatFileSize(scope.row.fileSizeBytes) }}</template></el-table-column>
        <el-table-column prop="uploadedAt" label="上传时间" width="180"><template slot-scope="scope">{{ formatTime(scope.row.uploadedAt) }}</template></el-table-column>
      </el-table>
      <el-empty v-if="!fileVersionsLoading && fileVersions.length === 0" description="暂无版本记录"></el-empty>
    </el-dialog>

    <el-dialog title="上传文件新版本" :visible.sync="versionDialogVisible" width="520px" @close="resetVersionForm">
      <el-form :model="versionForm" label-width="100px">
        <el-form-item label="当前文件"><div class="dialog-file-name">{{ versionForm.fileName || '-' }}</div></el-form-item>
        <el-form-item label="版本号"><el-input v-model="versionForm.version" placeholder="例如：1.1 / 2.0.0"></el-input></el-form-item>
        <el-form-item label="版本说明"><el-input v-model="versionForm.commitMessage" type="textarea" :rows="3"></el-input></el-form-item>
        <el-form-item label="选择文件"><input type="file" @change="handleVersionFileChange" class="native-file-input"></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="versionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="versionLoading" @click="submitUploadNewVersion">上传</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="待审核加入申请"
      :visible.sync="joinRequestDialogVisible"
      width="900px"
    >
      <el-table :data="pendingJoinRequests" border v-loading="joinRequestLoading">
        <el-table-column prop="applicantName" label="申请人" min-width="160" />
        <el-table-column prop="desiredRole" label="申请角色" width="120">
          <template slot-scope="scope">
            {{ getMemberRoleText(scope.row.desiredRole) }}
          </template>
        </el-table-column>
        <el-table-column prop="applyMessage" label="申请说明" min-width="320" />
        <el-table-column prop="createdAt" label="申请时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="success" @click="auditJoinRequest(scope.row, 'approved')">
              通过
            </el-button>
            <el-button size="mini" type="danger" @click="auditJoinRequest(scope.row, 'rejected')">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!joinRequestLoading && pendingJoinRequests.length === 0" description="暂无待审核申请" />
    </el-dialog>

    <el-dialog title="项目设置" :visible.sync="settingsDialogVisible" width="620px">
      <el-form :model="settingsForm" label-width="100px">
        <el-form-item label="项目名称"><el-input v-model="settingsForm.name"></el-input></el-form-item>
        <el-form-item label="项目描述"><el-input v-model="settingsForm.description" type="textarea" :rows="4"></el-input></el-form-item>
        <el-form-item label="项目分类"><el-input v-model="settingsForm.category"></el-input></el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="settingsForm.visibility" style="width: 100%">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私有" value="private"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签"><el-select v-model="settingsForm.tags" multiple allow-create filterable default-first-option style="width: 100%"><el-option v-for="tag in settingsForm.tags" :key="tag" :label="tag" :value="tag"></el-option></el-select></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingsLoading" @click="submitSettings">保存</el-button>
      </span>
    </el-dialog>

    <ProjectTemplateSaveDialog
      :visible.sync="saveTemplateDialogVisible"
      :project-id="projectId"
      :default-name="project.title || project.name || ''"
      :default-category="project.category || ''"
      :default-description="project.description || ''"
      @saved="handleTemplateSaved"
    />
  </div>
</template>

<script>
import {
  getProjectDetail,
  updateProject,
  deleteProject as apiDeleteProject,
  listProjectTasks,
  listMyTasks,
  createTask,
  updateTask,
  deleteTask as apiDeleteTask,
  updateTaskStatus,
  listProjectMembers,
  searchProjectMemberUsers,
  updateProjectMemberRole,
  removeProjectMember,
  quitProject as apiQuitProject,
  listProjectFiles,
  listFileVersions,
  uploadProjectFile,
  uploadFileNewVersion,
  setMainFile as apiSetMainFile,
  deleteFile as apiDeleteFile,
  downloadFile as apiDownloadFile,
  createProjectInvitation,
  listProjectInvitations,
  cancelProjectInvitation,
} from '@/api/project'
import { getProjectActivities } from '@/api/projectActivity'
import ProjectDocList from './components/ProjectDocList.vue'
import ProjectTemplateSaveDialog from './components/ProjectTemplateSaveDialog.vue'
import ProjectActivityManagePanel from './components/ProjectActivityManagePanel.vue'
import ProjectTaskCollabDrawer from '../components/ProjectTaskCollabDrawer.vue'
import ProjectMilestoneManage from './components/ProjectMilestoneManage.vue'
import ProjectSprintManage from './components/ProjectSprintManage.vue'
import ProjectReleaseManage from './components/ProjectReleaseManage.vue'
import ProjectDownloadRecordManage from './components/ProjectDownloadRecordManage.vue'
import ProjectStatManage from './components/ProjectStatManage.vue'
import { getToken } from '@/utils/auth'
import {
  listProjectJoinRequests,
  auditProjectJoinRequest
} from '@/api/projectJoinRequest'

const PROJECT_STATUS_LABEL_MAP = {
  draft: '草稿',
  pending: '待审核',
  published: '已发布',
  rejected: '已拒绝',
  archived: '已归档'
}

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (e) {}
    return tags.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

function formatBackendDateTime(dateLike) {
  if (!dateLike) return undefined
  const date = new Date(dateLike)
  if (Number.isNaN(date.getTime())) return typeof dateLike === 'string' ? dateLike : undefined
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'download'
  link.click()
  URL.revokeObjectURL(url)
}

function decodeJwtPayload(token) {
  try {
    const parts = String(token || '').split('.')
    if (parts.length < 2) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    if (typeof window !== 'undefined' && typeof window.atob === 'function') {
      return JSON.parse(decodeURIComponent(escape(window.atob(padded))))
    }
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf-8'))
  } catch (error) {
    return null
  }
}

function pickUserIdFromObject(source) {
  if (!source || typeof source !== 'object') return null
  const keys = ['id', 'userId', 'uid', 'memberId', 'sub']
  for (const key of keys) {
    const value = source[key]
    if (value !== undefined && value !== null && String(value).trim() !== '') return value
  }
  return null
}

function readCurrentUserId() {
  if (!process.client) return null
  const storageKeys = ['userInfo', 'user', 'loginUser', 'currentUser', 'Admin-User', 'auth_user', 'authUser', 'memberInfo']
  for (const storage of [window.localStorage, window.sessionStorage]) {
    for (const key of storageKeys) {
      try {
        const raw = storage.getItem(key)
        if (!raw) continue
        const parsed = JSON.parse(raw)
        const foundId = pickUserIdFromObject(parsed)
        if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') return Number(foundId)
      } catch (e) {}
    }
  }
  const token = getToken ? getToken() : ''
  if (token) {
    const payload = decodeJwtPayload(token)
    const foundId = pickUserIdFromObject(payload)
    if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') return Number(foundId)
  }
  return null
}

function sameId(a, b) {
  if (a === null || a === undefined || b === null || b === undefined) return false
  return Number(a) === Number(b)
}

export default {
  layout: 'project',
  components: {
    ProjectDocList,
    ProjectTemplateSaveDialog,
    ProjectActivityManagePanel,
    ProjectTaskCollabDrawer,
    ProjectMilestoneManage,
    ProjectSprintManage,
    ProjectReleaseManage,
    ProjectDownloadRecordManage,
    ProjectStatManage
  },
  data() {
    return {
      projectId: null,
      activeTab: 'overview',
      routeSyncing: false,
      settingsInnerTab: 'basic',
      pageReady: false,
      project: {},
      tasks: [],
      myTasks: [],
      members: [],
      contributors: [],
      files: [],
      recentActivities: [],
      activityTotal: 0,
      taskFilter: { keyword: '', status: 'all', priority: 'all', assigneeId: 'all', sortBy: 'updatedAt', sortOrder: 'desc' },
      memberFilter: { keyword: '' },
      fileFilter: { keyword: '' },
      selectedFileRows: [],
      selectedFileIds: [],
      taskDialogVisible: false,
      taskDialogType: 'create',
      taskForm: { id: null, title: '', description: '', assigneeId: null, status: 'todo', priority: 'medium', dueDate: '' },
      taskDialogOriginalTask: null,
      inviteDialogVisible: false,
      invitationForm: { inviteeId: null, inviteRole: 'member', inviteMessage: '', expireDays: 7 },
      inviteUserSearchLoading: false,
      inviteUserOptions: [],
      sendingInvitation: false,
      projectInvitations: [],
      invitationLoading: false,
      invitationFilter: { status: 'pending', keyword: '' },
      joinRequestDialogVisible: false,
      joinRequestLoading: false,
      pendingJoinRequests: [],
      joinRequests: [],
      joinRequestLoading: false,
      joinRequestAuditLoadingId: null,
      joinRequestFilter: { status: 'pending', keyword: '' },
      deleteProjectLoading: false,
      roleSavingMemberId: null,
      fileUploadDialogVisible: false,
      fileUploadLoading: false,
      fileUploadForm: { file: null, isMain: false, version: '1.0', commitMessage: '' },
      fileVersionsDialogVisible: false,
      fileVersionsLoading: false,
      fileVersions: [],
      versionDialogVisible: false,
      versionLoading: false,
      versionForm: { fileId: null, fileName: '', file: null, version: '', commitMessage: '' },
      settingsDialogVisible: false,
      settingsLoading: false,
      settingsForm: { name: '', description: '', category: '', visibility: 'public', tags: [] },
      saveTemplateDialogVisible: false,
      docCount: 0,
      taskCollabDrawerVisible: false,
      selectedTaskForCollab: null,
      taskCollabActiveTab: 'comment',
      taskCollabRefreshSeed: 0
    }
  },
  computed: {
    currentUserId() {
      return readCurrentUserId()
    },
    canSeeTaskCollaboration() {
      if (this.currentUserId === null || this.currentUserId === undefined) return false
      if (sameId(this.project.authorId, this.currentUserId)) return true
      return (this.members || []).some(member => {
        if (!member || member.isOwner) return false
        return sameId(member.userId, this.currentUserId)
      })
    },
    currentMemberRecord() {
      if (this.currentUserId === null || this.currentUserId === undefined) return null
      if (sameId(this.project.authorId, this.currentUserId)) {
        return { userId: this.currentUserId, role: 'owner', joinedAt: '' }
      }
      return (this.members || []).find(member => {
        if (!member || member.isOwner) return false
        return sameId(member.userId, this.currentUserId)
      }) || null
    },
    canManageProject() {
      if (this.currentUserId === null || this.currentUserId === undefined) return false
      if (sameId(this.project.authorId, this.currentUserId)) return true
      const role = this.currentMemberRecord && this.currentMemberRecord.role ? String(this.currentMemberRecord.role).toLowerCase() : ''
      return role === 'owner' || role === 'admin'
    },
    filteredTasks() {
      const list = this.tasks.filter(task => {
        const keyword = (this.taskFilter.keyword || '').trim().toLowerCase()
        const matchesKeyword = !keyword || (task.title || '').toLowerCase().includes(keyword) || (task.description || '').toLowerCase().includes(keyword)
        const matchesStatus = !this.taskFilter.status || this.taskFilter.status === 'all' || task.status === this.taskFilter.status
        const matchesPriority = !this.taskFilter.priority || this.taskFilter.priority === 'all' || task.priority === this.taskFilter.priority
        const assigneeFilter = this.taskFilter.assigneeId
        const matchesAssignee = !assigneeFilter || assigneeFilter === 'all' ? true : assigneeFilter === 'unassigned' ? !task.assigneeId : Number(task.assigneeId) === Number(assigneeFilter)
        return matchesKeyword && matchesStatus && matchesPriority && matchesAssignee
      })
      const order = this.taskFilter.sortOrder === 'asc' ? 1 : -1
      const priorityWeight = { low: 1, medium: 2, high: 3, urgent: 4 }
      return list.slice().sort((a, b) => {
        const sortBy = this.taskFilter.sortBy || 'updatedAt'
        if (sortBy === 'priority') return ((priorityWeight[a.priority] || 0) - (priorityWeight[b.priority] || 0)) * order
        if (sortBy === 'title') return String(a.title || '').localeCompare(String(b.title || ''), 'zh-CN') * order
        const av = new Date(a[sortBy] || 0).getTime() || 0
        const bv = new Date(b[sortBy] || 0).getTime() || 0
        return (av - bv) * order
      })
    },
    taskStats() {
      const list = this.filteredTasks
      return {
        total: list.length,
        todo: list.filter(task => task.status === 'todo').length,
        inProgress: list.filter(task => task.status === 'in_progress').length,
        done: list.filter(task => task.status === 'done').length,
        overdue: list.filter(task => this.isTaskOverdue(task)).length
      }
    },
    taskAssigneeOptions() {
      return this.members.filter(member => member && member.userId).map(member => ({
        userId: Number(member.userId),
        name: member.name || member.username || `用户${member.userId}`,
        avatar: member.avatar || ''
      }))
    },
    filteredMembers() {
      return this.members.filter(member => {
        const keyword = (this.memberFilter.keyword || '').trim().toLowerCase()
        if (!keyword) return true
        return [member.name, member.username, member.nickname].filter(Boolean).some(value => value.toLowerCase().includes(keyword))
      })
    },
    filteredFiles() {
      const keyword = (this.fileFilter.keyword || '').trim().toLowerCase()
      if (!keyword) return this.files
      return this.files.filter(file => (file.fileName || '').toLowerCase().includes(keyword))
    },
    isAllFilteredFilesSelected() {
      return this.filteredFiles.length > 0 && this.filteredFiles.every(file => this.selectedFileIds.includes(Number(file.id)))
    },
    isFileSelectionIndeterminate() {
      const total = this.filteredFiles.length
      if (!total) return false
      const selectedCount = this.filteredFiles.filter(file => this.selectedFileIds.includes(Number(file.id))).length
      return selectedCount > 0 && selectedCount < total
    },
    filteredProjectInvitations() {
      const keyword = (this.invitationFilter.keyword || '').trim().toLowerCase()
      return (this.projectInvitations || []).filter(item => {
        const statusMatched = !this.invitationFilter.status || item.status === this.invitationFilter.status
        if (!statusMatched) return false
        if (!keyword) return true
        return [item.inviteeName, item.inviteeEmail, item.inviteMessage, item.projectName].filter(Boolean).some(value => String(value).toLowerCase().includes(keyword))
      })
    },
    filteredJoinRequests() {
      const keyword = (this.joinRequestFilter.keyword || '').trim().toLowerCase()
      return (this.joinRequests || []).filter(item => {
        const statusMatched = !this.joinRequestFilter.status || item.status === this.joinRequestFilter.status
        if (!statusMatched) return false
        if (!keyword) return true
        return [item.applicantName, item.applyMessage, item.projectName].filter(Boolean).some(value => String(value).toLowerCase().includes(keyword))
      })
    },
    taskDialogTitle() {
      return this.taskDialogType === 'create' ? '新建任务' : '编辑任务'
    },
    myTaskDoneCount() {
      return this.myTasks.filter(task => task.status === 'done').length
    },
    selectedInviteUser() {
      return this.inviteUserOptions.find(item => Number(item.id) === Number(this.invitationForm.inviteeId)) || null
    },
    pendingJoinRequestCount() {
      return (this.pendingJoinRequests || []).filter(item => item.status === 'pending').length
    },
  },
  watch: {
    '$route.query': {
      immediate: true,
      handler(query) {
        this.applyRouteState(query)
      }
    },
    activeTab(val) {
      if (this.routeSyncing) return
      this.syncRouteTab(val)
      if (val === 'member-manage') {
        this.loadMembers()
        if (this.canManageProject) {
          this.loadProjectInvitations()
          this.loadJoinRequests()
        }
      }
    },
    'invitationFilter.status'() {
      if (this.activeTab === 'member-manage' && this.canManageProject) this.loadProjectInvitations()
    },
    'joinRequestFilter.status'() {
      if (this.activeTab === 'member-manage' && this.canManageProject) this.loadJoinRequests()
    }
  },
  async mounted() {
    this.pageReady = false
    this.projectId = this.$route.query.projectId || this.$route.params.id
    this.applyRouteState(this.$route.query || {})
    if (!this.projectId) {
      this.$message.error('项目ID不存在')
      return
    }
    await this.refreshAll()
    if (this.$route.path === '/projectmanage') {
      this.pageReady = true
    }
  },
  methods: {
    normalizeManageTab(tab) {
      const raw = String(tab || 'overview')
      const map = {
        'my-tasks': 'task-manage',
        'template-manage': 'settings',
        activity: 'activity-manage',
        tasks: 'task-manage',
        members: 'member-manage',
        files: 'file-manage',
        docs: 'doc-manage'
      }
      const next = map[raw] || raw
      const allow = ['overview', 'task-manage', 'member-manage', 'file-manage', 'doc-manage', 'activity-manage', 'settings', 'milestone-manage', 'sprint-manage', 'release-manage', 'download-manage', 'stat-manage']
      return allow.includes(next) ? next : 'overview'
    },
    applyRouteState(query = {}) {
      this.routeSyncing = true
      this.activeTab = this.normalizeManageTab(query.tab)
      this.settingsInnerTab = query.tab === 'template-manage' ? 'template' : (query.settingsTab || 'basic')
      if ((query.tab === 'my-tasks' || query.mineOnly === '1') && this.currentUserId) {
        this.taskFilter.assigneeId = this.currentUserId
      } else if (this.activeTab === 'task-manage' && !query.mineOnly) {
        this.taskFilter.assigneeId = 'all'
      }
      this.$nextTick(() => {
        this.routeSyncing = false
      })
    },
    syncRouteTab(tab, extraQuery = {}) {
      if (!this.projectId) return
      const query = {
        ...this.$route.query,
        projectId: String(this.projectId),
        tab
      }
      if (tab !== 'settings') delete query.settingsTab
      if (tab !== 'task-manage') {
        delete query.mineOnly
        delete query.taskId
      }
      if (tab !== 'activity-manage') {
        delete query.activityId
        delete query.action
        delete query.targetType
        delete query.operatorId
        delete query.startTime
        delete query.endTime
      }
      Object.keys(extraQuery).forEach(key => {
        const value = extraQuery[key]
        if (value === undefined || value === null || value === '') delete query[key]
        else query[key] = String(value)
      })
      this.routeSyncing = true
      this.$router.replace({ path: '/projectmanage', query }).finally(() => {
        this.routeSyncing = false
      })
    },
    goToSettingsTab(innerTab = 'basic') {
      this.activeTab = 'settings'
      this.settingsInnerTab = innerTab
      this.syncRouteTab('settings', { settingsTab: innerTab })
    },
    goToActivityManage(item, extra = {}) {
      if (!item || !item.id) return
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: 'activity-manage',
          activityId: String(item.id),
          ...extra
        }
      })
    },
    openSaveAsTemplate() {
      this.saveTemplateDialogVisible = true
    },
    handleTemplateSaved() {
      this.$message.success('模板保存成功')
      this.loadRecentActivities()
    },
    ensureTaskCollaborationAccess(redirect = false, showFeedback = false) {
      if (this.canSeeTaskCollaboration) return true
      this.tasks = []
      this.myTasks = []
      if (this.activeTab === 'task-manage') {
        this.activeTab = 'overview'
      }
      if (redirect) {
        if (showFeedback) {
          this.$message.closeAll()
          this.$message.warning('只有已加入项目的成员才能进入任务协作，其他用户仅可在项目详情页查看贡献者列表')
        }
        this.$router.replace(`/projectdetail?projectId=${this.projectId}`)
      }
      return false
    },
    openTaskCollab(task, tab = 'comment') {
      if (!task || !task.id) return
      const latestTask = [...this.tasks, ...this.myTasks].find(item => Number(item.id) === Number(task.id)) || task
      this.selectedTaskForCollab = { ...latestTask }
      this.taskCollabActiveTab = tab
      this.taskCollabDrawerVisible = true
      this.taskCollabRefreshSeed += 1
    },
    async handleTaskCollabChanged() {
      await this.refreshAll()
      if (this.selectedTaskForCollab && this.selectedTaskForCollab.id) {
        const latestTask = [...this.tasks, ...this.myTasks].find(item => Number(item.id) === Number(this.selectedTaskForCollab.id))
        if (latestTask) this.selectedTaskForCollab = { ...latestTask }
      }
      this.taskCollabRefreshSeed += 1
    },
    handleTaskCollabDrawerClosed() {
      this.selectedTaskForCollab = null
      this.taskCollabActiveTab = 'comment'
    },
    async handleDocChanged() {
      if (this.$route && this.$route.query && this.$route.query.tab === 'doc-manage') {
        await this.loadProjectData()
      }
    },
    async handleDocPrimaryChanged() {
      await this.loadProjectData()
    },
    normalizeProject(apiData) {
      return {
        id: apiData.id,
        title: apiData.name,
        description: apiData.description || '暂无项目描述',
        category: apiData.category || '',
        status: apiData.status || 'draft',
        statusText: PROJECT_STATUS_LABEL_MAP[apiData.status] || apiData.status || '-',
        visibility: apiData.visibility || 'public',
        tags: parseTags(apiData.tags),
        updateTime: apiData.updatedAt,
        authorId: apiData.authorId,
        authorName: apiData.authorName || '项目所有者',
        authorAvatar: apiData.authorAvatar || '',
        stars: apiData.stars || 0,
        downloads: apiData.downloads || 0,
        views: apiData.views || 0
      }
    },
    normalizeTask(task) {
      return {
        id: task.id,
        title: task.title,
        description: task.description || '',
        status: task.status || 'todo',
        priority: task.priority || 'medium',
        assigneeId: task.assigneeId,
        assigneeName: task.assigneeName || '未分配',
        assigneeAvatar: task.assigneeAvatar || '',
        creatorName: task.creatorName || '',
        dueDate: task.dueDate,
        createdAt: task.createdAt,
        updatedAt: task.updatedAt,
        completedAt: task.completedAt,
        completedBy: task.completedBy,
        completedMemberJoinedAt: task.completedMemberJoinedAt,
        hasPendingReopenRequest: !!task.hasPendingReopenRequest,
        pendingReopenRequestId: task.pendingReopenRequestId,
        pendingReopenRequestedAt: task.pendingReopenRequestedAt,
        pendingReopenTargetStatus: task.pendingReopenTargetStatus
      }
    },
    normalizeMember(member) {
      return {
        id: member.id,
        memberId: member.id,
        userId: member.userId,
        name: member.nickname || member.username || `用户${member.userId}`,
        username: member.username || `用户${member.userId}`,
        nickname: member.nickname || '',
        avatar: member.avatar || '',
        role: member.role || 'member',
        joinTime: member.joinedAt,
        isOwner: false
      }
    },
    buildOwnerRow() {
      if (!this.project.authorId) return null
      return {
        id: `owner-${this.project.authorId}`,
        memberId: null,
        userId: this.project.authorId,
        name: this.project.authorName || '项目所有者',
        username: this.project.authorName || `用户${this.project.authorId}`,
        nickname: this.project.authorName || '项目所有者',
        avatar: this.project.authorAvatar || '',
        role: 'owner',
        joinTime: this.project.updateTime,
        isOwner: true
      }
    },
    normalizeFile(file) {
      return {
        id: file.id,
        fileName: file.fileName,
        filePath: file.filePath,
        fileSizeBytes: file.fileSizeBytes,
        fileType: file.fileType,
        uploadTime: file.uploadTime,
        isMain: !!file.isMain,
        version: file.version || '',
        versions: file.versions || []
      }
    },
    isSelfMember(member) {
      return !!member && this.currentUserId !== null && this.currentUserId !== undefined && Number(member.userId) === Number(this.currentUserId)
    },
    memberRoleWeight(role) {
      return { viewer: 0, member: 1, admin: 2, owner: 3 }[String(role || '').toLowerCase()] ?? -1
    },
    canEditMemberRole(member) {
      if (!this.canManageProject || !member || !member.memberId || this.isSelfMember(member) || member.role === 'owner') return false
      const currentRole = this.currentMemberRecord && this.currentMemberRecord.role ? this.currentMemberRecord.role : 'owner'
      return this.memberRoleWeight(currentRole) > this.memberRoleWeight(member.role)
    },
    canRemoveMember(member) {
      return this.canEditMemberRole(member)
    },
    isReadonlyMember(member) {
      if (!member) return true
      return this.isSelfMember(member) || !this.canEditMemberRole(member)
    },
    memberRowClassName({ row }) {
      if (!row) return ''
      if (this.isSelfMember(row)) return 'member-row-self'
      if (this.isReadonlyMember(row)) return 'member-row-readonly'
      return 'member-row-manageable'
    },
    getInvitationStatusText(status) {
      return { pending: '待接受', accepted: '已接受', rejected: '已拒绝', cancelled: '已撤销', expired: '已过期' }[status] || status || '-'
    },
    getInvitationStatusTag(status) {
      return { pending: 'warning', accepted: 'success', rejected: 'info', cancelled: 'info', expired: 'danger' }[status] || 'info'
    },
    getJoinRequestStatusText(status) {
      return { pending: '待处理', approved: '已通过', rejected: '已拒绝', cancelled: '已取消' }[status] || status || '-'
    },
    getJoinRequestStatusTag(status) {
      return { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }[status] || 'info'
    },
    getTaskReopenBlockedReason(task, targetStatus = 'todo') {
      if (!task) return '任务不存在'
      if (this.canManageProject) return ''
      if (!this.canSeeTaskCollaboration) return '加入项目后才可参与任务协作'
      if (Number(task.assigneeId) !== Number(this.currentUserId)) {
        return '只有当前负责人、管理员或所有者可以提交重开申请'
      }
      if (task.status !== 'done' || targetStatus === 'done') return ''
      if (task.hasPendingReopenRequest) {
        return '该任务已有待处理的重开申请，请勿重复提交'
      }
      if (this.isHistoricalDoneTaskForCurrentUser(task)) {
        return '你不能修改上一入组周期已完成的任务，请联系项目管理员或所有者处理'
      }
      return ''
    },
    canEditTaskRow(task) {
      if (!task) return false
      if (this.canManageProject) return true
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      if (task.status === 'done') return false
      return !this.isHistoricalDoneTaskForCurrentUser(task)
    },
    canDeleteTaskRow(task) {
      if (!task) return false
      return this.canManageProject
    },
    canQuickUpdateTaskStatus(task) {
      if (!task) return false
      if (this.canManageProject) return true
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      if (task.status === 'done') {
        return !this.getTaskReopenBlockedReason(task, 'todo')
      }
      return true
    },
    canQuickUpdateTaskAssignee(task) {
      if (!task) return false
      return this.canManageProject
    },
    rebuildOverview() {
      const owner = this.buildOwnerRow()
      const memberRows = this.members.filter(item => !item.isOwner)
      this.contributors = [owner, ...memberRows].filter(Boolean).slice(0, 8)
    },
    normalizeActivityPayload(res, fallbackSize = 8) {
      const payload = res && res.data ? res.data : res
      if (payload && Array.isArray(payload.list)) {
        return {
          list: payload.list,
          total: Number(payload.total || 0)
        }
      }
      if (Array.isArray(payload)) {
        return {
          list: payload.slice(0, fallbackSize),
          total: payload.length
        }
      }
      return {
        list: [],
        total: 0
      }
    },
    async loadRecentActivities() {
      try {
        const response = await getProjectActivities(this.projectId, { page: 1, size: 8 })
        const pageData = this.normalizeActivityPayload(response, 8)
        this.recentActivities = pageData.list || []
        this.activityTotal = pageData.total || 0
      } catch (error) {
        console.error('加载项目活动失败:', error)
        this.recentActivities = []
        this.activityTotal = 0
      }
    },
    async refreshAll() {
      await this.loadProjectData()
      if (this.activeTab === 'task-manage') {
        if (this.currentUserId === null || this.currentUserId === undefined) {
          this.ensureTaskCollaborationAccess(true, false)
          return
        }
      }
      if (this.currentUserId === null || this.currentUserId === undefined) {
        this.tasks = []
        this.myTasks = []
        await this.loadFiles()
        await this.loadRecentActivities()
        this.rebuildOverview()
        return
      }
      await this.loadMembers()
      if (!this.ensureTaskCollaborationAccess(true, false)) {
        return
      }
      await Promise.all([this.loadTasks(), this.loadMyTasks(), this.loadFiles(), this.loadRecentActivities()])
      this.rebuildOverview()
    },
    async loadProjectData() {
      try {
        const response = await getProjectDetail(this.projectId)
        this.project = this.normalizeProject(response.data || {})
      } catch (error) {
        console.error('加载项目数据失败:', error)
        this.$message.error(error.response?.data?.message || '加载项目数据失败')
      }
    },
    async loadTasks() {
      if (!this.canSeeTaskCollaboration) {
        this.tasks = []
        return
      }
      try {
        const response = await listProjectTasks(this.projectId)
        this.tasks = (response.data || []).map(this.normalizeTask)
      } catch (error) {
        console.error('加载任务列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载任务列表失败')
      }
    },
    async loadMyTasks() {
      if (!this.canSeeTaskCollaboration) {
        this.myTasks = []
        return
      }
      try {
        const response = await listMyTasks(this.projectId)
        this.myTasks = (response.data || []).map(this.normalizeTask)
      } catch (error) {
        console.error('加载我的任务失败:', error)
        this.$message.error(error.response?.data?.message || '加载我的任务失败')
      }
    },
    async loadMembers() {
      try {
        const response = await listProjectMembers(this.projectId)
        const rows = (response.data || []).map(this.normalizeMember)
        const owner = this.buildOwnerRow()
        const ownerUserId = owner ? Number(owner.userId) : null
        const memberRows = rows.filter(item => {
          if (item.role === 'owner') return false
          if (!ownerUserId) return true
          return Number(item.userId) !== ownerUserId
        })
        this.members = [owner, ...memberRows].filter(Boolean)
      } catch (error) {
        console.error('加载成员列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载成员列表失败')
      }
    },
    async loadFiles() {
      try {
        const response = await listProjectFiles(this.projectId)
        this.files = (response.data || []).map(this.normalizeFile)
        const validIds = new Set(this.files.map(item => Number(item.id)))
        this.selectedFileIds = this.selectedFileIds.filter(id => validIds.has(Number(id)))
        this.syncSelectedFileRows()
      } catch (error) {
        console.error('加载文件列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载文件列表失败')
      }
    },
    handleOverviewActivityClick(item) {
      if (!item || !item.id) return
      this.$router.push({
        path: '/projectmanage',
        query: {
          projectId: String(this.projectId),
          tab: 'activity-manage',
          activityId: String(item.id)
        }
      })
      this.activeTab = 'activity-manage'
    },
    openCreateTaskDialog() {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      this.taskDialogType = 'create'
      this.taskDialogOriginalTask = null
      this.resetTaskForm()
      this.taskDialogVisible = true
    },
    openEditTaskDialog(task) {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.canManageProject && task && task.status === 'done') {
        const blockedReason = this.getTaskReopenBlockedReason(task, 'todo')
        if (blockedReason) {
          this.$message.error(blockedReason)
          return
        }
        this.$message.warning('已完成任务不能直接编辑，如需改回未完成请先提交重开申请')
        this.openTaskCollab(task, 'reopen')
        return
      }
      if (!this.canEditTaskRow(task)) {
        this.$message.warning('只有当前负责人、管理员或所有者可以编辑任务')
        return
      }
      this.taskDialogType = 'edit'
      this.taskDialogOriginalTask = { ...task }
      this.taskForm = { id: task.id, title: task.title, description: task.description, assigneeId: task.assigneeId, status: task.status, priority: task.priority, dueDate: task.dueDate || '' }
      this.taskDialogVisible = true
    },
    resetTaskForm() {
      this.taskForm = { id: null, title: '', description: '', assigneeId: this.project.authorId || null, status: 'todo', priority: 'medium', dueDate: '' }
      this.taskDialogOriginalTask = null
    },
    async submitTask() {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.taskForm.title || !this.taskForm.assigneeId) {
        this.$message.warning('请填写完整信息')
        return
      }
      const payload = {
        title: this.taskForm.title,
        description: this.taskForm.description,
        assigneeId: Number(this.taskForm.assigneeId),
        priority: this.taskForm.priority,
        dueDate: formatBackendDateTime(this.taskForm.dueDate)
      }
      try {
        if (this.taskDialogType === 'create') {
          const response = await createTask({ projectId: Number(this.projectId), ...payload })
          const createdTaskId = response?.data?.id
          if (createdTaskId && this.taskForm.status && this.taskForm.status !== 'todo') {
            await updateTaskStatus(createdTaskId, { status: this.taskForm.status })
          }
          this.$message.success('任务创建成功')
        } else {
          const originalTask = this.taskDialogOriginalTask || [...this.tasks, ...this.myTasks].find(item => Number(item.id) === Number(this.taskForm.id))
          if (!originalTask) {
            this.$message.error('原任务不存在或已被删除')
            return
          }
          if (!this.canManageProject && Number(originalTask.assigneeId) !== Number(this.currentUserId)) {
            this.$message.error('只有当前负责人、管理员或所有者可以编辑任务')
            return
          }
          if (!this.canManageProject && originalTask.status === 'done') {
            this.$message.warning('已完成任务不能直接编辑，如需改回未完成请先提交重开申请')
            this.taskDialogVisible = false
            this.openTaskCollab(originalTask, 'reopen')
            return
          }
          const updatePayload = {
            title: payload.title,
            description: payload.description,
            priority: payload.priority,
            dueDate: payload.dueDate
          }
          if (this.canManageProject) {
            updatePayload.assigneeId = payload.assigneeId
          }
          await updateTask(this.taskForm.id, updatePayload)
          if (this.taskForm.status !== originalTask.status) {
            if (!this.canManageProject && originalTask.status === 'done' && this.taskForm.status !== 'done') {
              const blockedReason = this.getTaskReopenBlockedReason(originalTask, this.taskForm.status)
              if (blockedReason) {
                this.$message.error(blockedReason)
                return
              }
              await this.submitTaskReopenRequest(originalTask, this.taskForm.status)
            } else {
              await updateTaskStatus(this.taskForm.id, { status: this.taskForm.status })
            }
          }
          this.$message.success('任务更新成功')
        }
        this.taskDialogVisible = false
        await Promise.all([this.loadTasks(), this.loadMyTasks(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('提交任务失败:', error)
        this.$message.error(error.response?.data?.message || '任务保存失败')
      }
    },
    async deleteTask(taskId) {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      try {
        await this.$confirm('确定删除该任务吗？', '提示', { type: 'warning' })
        await apiDeleteTask(taskId)
        this.$message.success('删除成功')
        await Promise.all([this.loadTasks(), this.loadMyTasks(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除任务失败:', error)
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }
    },
    getComparableTaskCycleTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },
    isHistoricalDoneTaskForCurrentUser(task) {
      if (!task || task.status !== 'done' || this.canManageProject) return false
      if (this.currentUserId === null || this.currentUserId === undefined) return false
      if (Number(task.assigneeId) !== Number(this.currentUserId)) return false
      const completedCycle = this.getComparableTaskCycleTime(task.completedMemberJoinedAt)
      const currentCycle = this.getComparableTaskCycleTime(this.currentMemberRecord && this.currentMemberRecord.joinedAt)
      if (!completedCycle || !currentCycle) return false
      return completedCycle !== currentCycle
    },
    async submitTaskReopenRequest(task, targetStatus) {
      const blockedReason = this.getTaskReopenBlockedReason(task, targetStatus)
      if (blockedReason) {
        this.$message.error(blockedReason)
        return false
      }
      const { value } = await this.$prompt('请填写重开原因，管理员或所有者确认后才会把任务改回未完成。', '提交重开申请', {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：验收发现遗漏、联调失败、完成结论需要撤回等',
        inputValidator: inputValue => {
          if (!String(inputValue || '').trim()) return '请填写重开原因'
          if (String(inputValue || '').trim().length < 2) return '重开原因至少写 2 个字'
          return true
        }
      })
      await this.$axios({
        url: `/api/project/task/${task.id}/reopen-requests`,
        method: 'post',
        data: { targetStatus, reason: String(value || '').trim() }
      })
      this.$message.success('已提交重开申请，请等待管理员或所有者确认')
      this.openTaskCollab(task, 'reopen')
      await Promise.all([this.loadTasks(), this.loadMyTasks(), this.loadRecentActivities()])
      this.rebuildOverview()
      return true
    },
    async changeTaskStatus(taskId, status) {
      const task = [...this.tasks, ...this.myTasks].find(item => Number(item.id) === Number(taskId))
      if (!task || !status || task.status === status) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      if (!this.canManageProject && Number(task.assigneeId) !== Number(this.currentUserId)) {
        this.$message.error('只有当前负责人、管理员或所有者可以修改任务状态')
        return
      }
      if (!this.canManageProject && task.status === 'done' && status !== 'done') {
        const blockedReason = this.getTaskReopenBlockedReason(task, status)
        if (blockedReason) {
          this.$message.error(blockedReason)
          return
        }
        try {
          await this.submitTaskReopenRequest(task, status)
        } catch (error) {
          if (error !== 'cancel' && error !== 'close') {
            this.$message.error(error.response?.data?.message || '提交重开申请失败')
          }
        }
        return
      }
      try {
        await updateTaskStatus(taskId, { status })
        this.$message.success('状态更新成功')
        await Promise.all([this.loadTasks(), this.loadMyTasks(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('更新任务状态失败:', error)
        this.$message.error(error.response?.data?.message || '状态更新失败')
      }
    },
    async changeTaskAssignee(taskId, assigneeId) {
      if (!assigneeId) {
        this.$message.warning('请选择负责人')
        return
      }
      if (!this.canManageProject) {
        this.$message.error('只有管理员或所有者可以修改任务负责人')
        return
      }
      try {
        await updateTask(taskId, { assigneeId: Number(assigneeId) })
        this.$message.success('负责人更新成功')
        await Promise.all([this.loadTasks(), this.loadMyTasks(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('更新负责人失败:', error)
        this.$message.error(error.response?.data?.message || '负责人更新失败')
      }
    },
    syncSelectedFileRows() {
      const idSet = new Set(this.selectedFileIds.map(id => Number(id)))
      this.selectedFileRows = this.files.filter(file => idSet.has(Number(file.id)))
    },
    isFileSelected(file) {
      if (!file || file.id === undefined || file.id === null) return false
      return this.selectedFileIds.includes(Number(file.id))
    },
    toggleFileSelection(file, checked) {
      const id = Number(file && file.id)
      if (!id) return
      if (checked) {
        if (!this.selectedFileIds.includes(id)) {
          this.selectedFileIds = [...this.selectedFileIds, id]
        }
      } else {
        this.selectedFileIds = this.selectedFileIds.filter(item => Number(item) !== id)
      }
      this.syncSelectedFileRows()
    },
    toggleAllFileSelection(checked) {
      const ids = this.filteredFiles.map(file => Number(file.id)).filter(Boolean)
      if (checked) {
        const set = new Set([...this.selectedFileIds, ...ids])
        this.selectedFileIds = Array.from(set)
      } else {
        const removeSet = new Set(ids)
        this.selectedFileIds = this.selectedFileIds.filter(id => !removeSet.has(Number(id)))
      }
      this.syncSelectedFileRows()
    },
    clearFileSelection() {
      this.selectedFileRows = []
      this.selectedFileIds = []
    },
    async batchDeleteProjectFiles() {
      if (!this.selectedFileRows.length) {
        this.$message.warning('请先勾选要删除的文件')
        return
      }
      const rows = this.selectedFileRows.slice()
      try {
        await this.$confirm(`确定批量删除选中的 ${rows.length} 个文件吗？此操作不可恢复。`, '提示', { type: 'warning' })
        const results = await Promise.allSettled(rows.map(item => apiDeleteFile(item.id)))
        const successCount = results.filter(item => item.status === 'fulfilled').length
        const failCount = results.length - successCount
        if (successCount > 0) {
          this.$message.success(`已删除 ${successCount} 个文件${failCount ? `，失败 ${failCount} 个` : ''}`)
        } else {
          this.$message.error('批量删除失败')
        }
        await Promise.all([this.loadFiles(), this.loadRecentActivities()])
        this.rebuildOverview()
        this.clearFileSelection()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除文件失败:', error)
          this.$message.error(error.response?.data?.message || '批量删除文件失败')
        }
      }
    },
    resetTaskFilters() {
      this.taskFilter = { keyword: '', status: 'all', priority: 'all', assigneeId: 'all', sortBy: 'updatedAt', sortOrder: 'desc' }
    },
    isTaskOverdue(task) {
      if (!task || !task.dueDate || task.status === 'done') return false
      const dueDate = new Date(task.dueDate)
      return !Number.isNaN(dueDate.getTime()) && dueDate.getTime() < Date.now()
    },
    openAddMemberDialog() {
      this.openInviteDialog()
    },
    openInviteDialog() {
      this.resetInviteForm()
      this.inviteDialogVisible = true
    },
    resetInviteForm() {
      this.invitationForm = { inviteeId: null, inviteRole: 'member', inviteMessage: '', expireDays: 7 }
      this.inviteUserOptions = []
      this.inviteUserSearchLoading = false
    },
    buildNewMemberUserLabel(user) {
      const displayName = user.nickname || user.username || `用户${user.id}`
      return `${displayName}（${user.username || '-'} / ID ${user.id}）`
    },
    async searchInviteUsers(keyword) {
      const value = (keyword || '').trim()
      if (!value) {
        this.inviteUserOptions = []
        return
      }
      this.inviteUserSearchLoading = true
      try {
        const response = await searchProjectMemberUsers(value, 10)
        const memberUserIdSet = new Set(this.members.map(item => Number(item.userId)))
        const pendingInviteeSet = new Set((this.projectInvitations || []).filter(item => item.status === 'pending' && item.inviteeId).map(item => Number(item.inviteeId)))
        this.inviteUserOptions = (response.data || []).filter(user => Number(user.id) !== Number(this.currentUserId) && !memberUserIdSet.has(Number(user.id)) && !pendingInviteeSet.has(Number(user.id)))
      } catch (error) {
        console.error('搜索用户失败:', error)
        this.inviteUserOptions = []
        this.$message.error(error.response?.data?.message || '搜索用户失败')
      } finally {
        this.inviteUserSearchLoading = false
      }
    },
    async sendProjectInvitation() {
      if (!this.invitationForm.inviteeId) {
        this.$message.warning('请先选择用户')
        return
      }
      this.sendingInvitation = true
      try {
        await createProjectInvitation({
          projectId: Number(this.projectId),
          inviteeId: Number(this.invitationForm.inviteeId),
          inviteRole: this.invitationForm.inviteRole,
          inviteMessage: this.invitationForm.inviteMessage,
          expireDays: this.invitationForm.expireDays
        })
        this.$message.success('邀请已发送，等待对方接受')
        this.inviteDialogVisible = false
        this.resetInviteForm()
        await this.loadProjectInvitations()
      } catch (error) {
        console.error('发送邀请失败:', error)
        this.$message.error(error.response?.data?.message || '发送邀请失败')
      } finally {
        this.sendingInvitation = false
      }
    },
    async loadProjectInvitations() {
      if (!this.canManageProject) {
        this.projectInvitations = []
        return
      }
      this.invitationLoading = true
      try {
        const response = await listProjectInvitations(this.projectId, { status: this.invitationFilter.status || undefined })
        this.projectInvitations = response.data || []
      } catch (error) {
        console.error('加载邀请列表失败:', error)
        this.projectInvitations = []
      } finally {
        this.invitationLoading = false
      }
    },
    async cancelInvitation(item) {
      try {
        await this.$confirm('确认撤销这条邀请吗？', '提示', { type: 'warning' })
        await cancelProjectInvitation(item.id)
        this.$message.success('邀请已撤销')
        await this.loadProjectInvitations()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.response?.data?.message || '撤销邀请失败')
        }
      }
    },
    async loadJoinRequests() {
      if (!this.canManageProject) {
        this.joinRequests = []
        return
      }
      this.joinRequestLoading = true
      try {
        const response = await listProjectJoinRequests(this.projectId)
        this.joinRequests = response.data || []
      } catch (error) {
        console.error('加载加入申请失败:', error)
        this.joinRequests = []
      } finally {
        this.joinRequestLoading = false
      }
    },
    async handleJoinRequestAudit(item, approved) {
      const actionText = approved ? '通过' : '拒绝'
      try {
        const { value } = await this.$prompt(`可选填写${actionText}备注`, `${actionText}加入申请`, {
          confirmButtonText: actionText,
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: `可选填写${actionText}备注`,
          inputValidator: () => true
        })
        this.joinRequestAuditLoadingId = item.id
        await auditProjectJoinRequest(item.id, { status: approved ? 'approved' : 'rejected', reviewMessage: String(value || '').trim() })
        this.$message.success(`${actionText}成功`)
        await Promise.all([this.loadJoinRequests(), this.loadMembers(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error.response?.data?.message || `${actionText}失败`)
        }
      } finally {
        this.joinRequestAuditLoadingId = null
      }
    },
    async updateMemberRoleInline(member, role) {
      if (!member.memberId) {
        this.$message.warning('项目所有者角色不能在这里修改')
        return
      }
      if (this.isSelfMember(member)) {
        this.$message.warning('不能修改自己的项目身份')
        return
      }
      if (!this.canEditMemberRole(member)) {
        this.$message.warning('你只能管理权限低于自己的成员')
        return
      }
      const previousRole = member.role
      member.role = role
      this.roleSavingMemberId = member.memberId
      try {
        await updateProjectMemberRole({ memberId: member.memberId, role })
        this.$message.success('角色更新成功')
        await Promise.all([this.loadMembers(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        member.role = previousRole
        console.error('更新成员角色失败:', error)
        this.$message.error(error.response?.data?.message || '角色更新失败')
      } finally {
        this.roleSavingMemberId = null
      }
    },
    async deleteMember(member) {
      if (!member.memberId) {
        this.$message.warning('项目所有者不可移除')
        return
      }
      if (this.isSelfMember(member)) {
        this.$message.warning('不能在成员管理中移除自己，请使用退出项目')
        return
      }
      if (!this.canRemoveMember(member)) {
        this.$message.warning('你只能管理权限低于自己的成员')
        return
      }
      try {
        await this.$confirm(`确定移除成员 ${member.name} 吗？`, '提示', { type: 'warning' })
        await removeProjectMember(member.memberId)
        this.$message.success('移除成功')
        await Promise.all([this.loadMembers(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('移除成员失败:', error)
          this.$message.error(error.response?.data?.message || '移除失败')
        }
      }
    },
    async quitProject() {
      try {
        await this.$confirm('确定退出项目吗？退出后将无法继续协作该项目。', '提示', { type: 'warning' })
        await apiQuitProject(this.projectId)
        this.$message.success('已退出项目')
        this.$router.push('/projectlist')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('退出项目失败:', error)
          this.$message.error(error.response?.data?.message || '退出项目失败')
        }
      }
    },
    async confirmDeleteProject() {
      try {
        await this.$prompt(`请输入项目名称“${this.project.title || ''}”确认删除`, '删除项目', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          inputValidator: value => String(value || '').trim() === String(this.project.title || '').trim() ? true : '输入的项目名称不匹配'
        })
        this.deleteProjectLoading = true
        await apiDeleteProject(this.projectId)
        this.$message.success('项目已删除')
        this.$router.push('/projectlist')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') {
          this.$message.error(error.response?.data?.message || '删除项目失败')
        }
      } finally {
        this.deleteProjectLoading = false
      }
    },
    openUploadFileDialog() {
      this.resetFileUploadForm()
      this.fileUploadDialogVisible = true
    },
    resetFileUploadForm() {
      this.fileUploadForm = { file: null, isMain: false, version: '1.0', commitMessage: '' }
    },
    handleUploadFileChange(event) {
      this.fileUploadForm.file = event.target.files && event.target.files[0] ? event.target.files[0] : null
    },
    async submitUploadFile() {
      if (!this.fileUploadForm.file) {
        this.$message.warning('请选择要上传的文件')
        return
      }
      this.fileUploadLoading = true
      try {
        const formData = new FormData()
        formData.append('projectId', this.projectId)
        formData.append('file', this.fileUploadForm.file)
        formData.append('isMain', this.fileUploadForm.isMain ? 'true' : 'false')
        if (this.fileUploadForm.version) formData.append('version', this.fileUploadForm.version)
        if (this.fileUploadForm.commitMessage) formData.append('commitMessage', this.fileUploadForm.commitMessage)
        await uploadProjectFile(this.projectId, formData)
        this.$message.success('文件上传成功')
        this.fileUploadDialogVisible = false
        await Promise.all([this.loadFiles(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('文件上传失败:', error)
        this.$message.error(error.response?.data?.message || '文件上传失败')
      } finally {
        this.fileUploadLoading = false
      }
    },
    async viewFileVersions(file) {
      this.fileVersionsDialogVisible = true
      this.fileVersionsLoading = true
      try {
        const response = await listFileVersions(file.id)
        this.fileVersions = response.data || []
      } catch (error) {
        console.error('加载版本记录失败:', error)
        this.$message.error(error.response?.data?.message || '加载版本记录失败')
      } finally {
        this.fileVersionsLoading = false
      }
    },
    openUploadNewVersionDialog(file) {
      this.versionForm = { fileId: file.id, fileName: file.fileName, file: null, version: file.version || '', commitMessage: '' }
      this.versionDialogVisible = true
    },
    resetVersionForm() {
      this.versionForm = { fileId: null, fileName: '', file: null, version: '', commitMessage: '' }
    },
    handleVersionFileChange(event) {
      this.versionForm.file = event.target.files && event.target.files[0] ? event.target.files[0] : null
    },
    async submitUploadNewVersion() {
      if (!this.versionForm.fileId || !this.versionForm.file) {
        this.$message.warning('请选择要上传的新版本文件')
        return
      }
      this.versionLoading = true
      try {
        const formData = new FormData()
        formData.append('file', this.versionForm.file)
        if (this.versionForm.version) formData.append('version', this.versionForm.version)
        if (this.versionForm.commitMessage) formData.append('commitMessage', this.versionForm.commitMessage)
        await uploadFileNewVersion(this.versionForm.fileId, formData)
        this.$message.success('新版本上传成功')
        this.versionDialogVisible = false
        await Promise.all([this.loadFiles(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('上传新版本失败:', error)
        this.$message.error(error.response?.data?.message || '上传新版本失败')
      } finally {
        this.versionLoading = false
      }
    },
    async setMainProjectFile(file) {
      try {
        await apiSetMainFile(file.id)
        this.$message.success('已设为主文件')
        await Promise.all([this.loadFiles(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('设置主文件失败:', error)
        this.$message.error(error.response?.data?.message || '设置主文件失败')
      }
    },
    async deleteProjectFile(file) {
      try {
        await this.$confirm(`确定删除文件 ${file.fileName} 吗？`, '提示', { type: 'warning' })
        await apiDeleteFile(file.id)
        this.$message.success('文件删除成功')
        await Promise.all([this.loadFiles(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除文件失败:', error)
          this.$message.error(error.response?.data?.message || '删除文件失败')
        }
      }
    },
    async downloadProjectFile(file) {
      try {
        const blob = await apiDownloadFile(file.id)
        triggerBlobDownload(blob, file.fileName)
        this.$message.success('下载开始')
      } catch (error) {
        console.error('下载文件失败:', error)
        this.$message.error(error.response?.data?.message || '下载文件失败')
      }
    },
    openSettingsDialog() {
      this.settingsForm = {
        name: this.project.title || '',
        description: this.project.description || '',
        category: this.project.category || '',
        visibility: this.project.visibility || 'public',
        tags: [...(this.project.tags || [])]
      }
      this.settingsDialogVisible = true
    },
    async submitSettings() {
      if (!this.settingsForm.name) {
        this.$message.warning('请输入项目名称')
        return
      }
      this.settingsLoading = true
      try {
        await updateProject(this.projectId, {
          name: this.settingsForm.name,
          description: this.settingsForm.description || undefined,
          category: this.settingsForm.category || undefined,
          visibility: this.settingsForm.visibility || 'public',
          tags: JSON.stringify(this.settingsForm.tags || [])
        })
        this.$message.success('项目设置更新成功')
        this.settingsDialogVisible = false
        await Promise.all([this.loadProjectData(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('更新项目设置失败:', error)
        this.$message.error(error.response?.data?.message || '更新项目设置失败')
      } finally {
        this.settingsLoading = false
      }
    },
    goToDetail() {
      this.$router.push(`/projectdetail?projectId=${this.projectId}`)
    },
    getProjectStatusType(status) {
      return { draft: 'info', pending: 'warning', published: 'success', rejected: 'danger', archived: 'info' }[status] || 'info'
    },
    getTaskStatusType(status) {
      return { todo: 'info', in_progress: 'warning', done: 'success' }[status] || 'info'
    },
    getTaskStatusText(status) {
      return { todo: '待处理', in_progress: '进行中', done: '已完成' }[status] || status
    },
    getTaskPriorityType(priority) {
      return { low: 'info', medium: '', high: 'warning', urgent: 'danger' }[priority] || ''
    },
    getTaskPriorityText(priority) {
      return { low: '低', medium: '中', high: '高', urgent: '紧急' }[priority] || priority
    },
    getMemberRoleText(role) {
      return { owner: '所有者', admin: '管理员', member: '成员', viewer: '查看者' }[role] || role
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      if (Number.isNaN(date.getTime())) return timeStr
      return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },
    formatFileSize(bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return `${bytes} B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
      return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    },
    openJoinRequestDialog() {
      if (!this.canManageProject) return
      this.joinRequestDialogVisible = true
      this.loadPendingJoinRequests()
    },

    async loadPendingJoinRequests() {
      if (!this.canManageProject) {
        this.pendingJoinRequests = []
        return
      }
      this.joinRequestLoading = true
      try {
        const res = await listProjectJoinRequests(this.projectId)
        const rows = Array.isArray(res.data) ? res.data : []
        this.pendingJoinRequests = rows.filter(item => item.status === 'pending')
      } catch (error) {
        console.error('加载加入申请失败:', error)
        this.pendingJoinRequests = []
        this.$message.error(error.response?.data?.message || '加载加入申请失败')
      } finally {
        this.joinRequestLoading = false
      }
    },

    async auditJoinRequest(row, status) {
      try {
        const reviewMessage = status === 'approved' ? '审核通过' : '审核拒绝'
        await auditProjectJoinRequest(row.id, { status, reviewMessage })
        this.$message.success(status === 'approved' ? '已通过申请' : '已拒绝申请')
        await this.loadPendingJoinRequests()
        await Promise.all([this.loadMembers(), this.loadRecentActivities()])
        this.rebuildOverview()
      } catch (error) {
        console.error('审核加入申请失败:', error)
        this.$message.error(error.response?.data?.message || '审核加入申请失败')
      }
    },
  }
}
</script>

<style scoped>
.project-manage-page { max-width: 1360px; margin: 0 auto; padding: 24px; }
.manage-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.header-top { margin-bottom: 8px; }
.page-title { margin: 0; font-size: 30px; color: #303133; }
.page-subtitle { margin: 8px 0 0; color: #909399; }
.header-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.manage-tabs { margin-bottom: 16px; }
.tab-panel { display: flex; flex-direction: column; gap: 16px; }
.stats-row { margin-bottom: 16px; }
.stat-card { background: #fff; border: 1px solid #ebeef5; border-radius: 14px; padding: 20px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04); }
.stat-number { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { margin-top: 8px; color: #909399; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.toolbar-input { width: 220px; }
.toolbar-select { width: 140px; }
.side-card { margin-bottom: 16px; }
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-item { display: flex; justify-content: space-between; gap: 12px; color: #606266; }
.contributor-list { display: flex; flex-direction: column; gap: 12px; }
.contributor-item { display: flex; align-items: center; gap: 12px; }
.contributor-text { display: flex; flex-direction: column; }
.contributor-name { color: #303133; font-weight: 600; }
.contributor-role { color: #909399; font-size: 12px; }
.activity-list { display: flex; flex-direction: column; gap: 14px; }
.activity-item { display: grid; grid-template-columns: 40px 1fr auto; gap: 12px; align-items: center; padding: 12px 0; border-bottom: 1px solid #f4f4f5; }
.activity-item:last-child { border-bottom: none; }
.activity-item.is-clickable { cursor: pointer; transition: all .25s ease; }
.activity-item.is-clickable:hover { background: #f7fbff; border-radius: 10px; }
.activity-title { color: #303133; font-weight: 600; }
.activity-desc, .activity-time, .dialog-tip { color: #909399; font-size: 12px; }
.activity-time { white-space: nowrap; }
.task-card-header { align-items: flex-start; }
.task-header-left { display: flex; flex-direction: column; gap: 4px; }
.task-header-desc { color: #909399; font-size: 12px; }
.task-summary-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.task-summary-card { padding: 14px 16px; border-radius: 12px; background: #f7f9fc; border: 1px solid #ebeef5; }
.task-summary-card.danger { background: #fff6f6; border-color: #f5c2c7; }
.task-summary-value { font-size: 24px; font-weight: 700; color: #303133; }
.task-summary-label { margin-top: 6px; font-size: 12px; color: #909399; }
.task-title-cell { display: flex; flex-direction: column; gap: 6px; }
.task-title-main { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.task-title-link { padding: 0; color: #303133; font-weight: 600; }
.task-title-link:hover { color: #409eff; }
.task-title-desc { color: #909399; font-size: 12px; line-height: 1.5; }
.task-assignee-cell { display: flex; align-items: center; gap: 10px; }
.task-assignee-text { min-width: 0; }
.task-assignee-name { color: #303133; font-weight: 600; }
.task-assignee-meta { color: #909399; font-size: 12px; }
.task-quick-actions { display: flex; flex-direction: column; gap: 8px; }
.task-inline-select { width: 100%; }
.toolbar-select-wide { width: 180px; }
.member-user-option { line-height: 1.4; }
.member-user-option__title { font-size: 14px; color: #303133; }
.member-user-option__desc { font-size: 12px; color: #909399; }
.selected-user-card { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 10px 12px; background: #f5f7fa; border: 1px solid #ebeef5; border-radius: 10px; }
.selected-user-card__text { min-width: 0; }
.selected-user-card__name { font-size: 14px; color: #303133; font-weight: 600; }
.selected-user-card__meta { font-size: 12px; color: #909399; }
.settings-actions-row { margin-top: 16px; }
.settings-tip-box { padding: 8px 0; }
.settings-tip-title { font-size: 16px; font-weight: 700; color: #303133; }
.settings-tip-desc { margin-top: 8px; color: #909399; line-height: 1.8; }
.dialog-file-name { color: #303133; font-weight: 600; }
.native-file-input { display: block; width: 100%; }
@media (max-width: 768px) {
  .project-manage-page { padding: 16px; }
  .manage-header { flex-direction: column; }
  .toolbar-input, .toolbar-select, .toolbar-select-wide { width: 100%; }
  .task-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .feature-entry-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .activity-item { grid-template-columns: 40px 1fr; }
  .activity-time { grid-column: 2 / 3; }
}
@media (max-width: 480px) {
  .task-summary-grid { grid-template-columns: 1fr; }
  .feature-entry-grid { grid-template-columns: 1fr; }
}


.feature-entry-card { margin-bottom: 16px; }
.feature-entry-tip { color: #909399; font-size: 12px; }
.feature-entry-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; }
.feature-entry-item { padding: 16px; border-radius: 14px; border: 1px solid #e6eef7; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); cursor: pointer; transition: all .2s ease; }
.feature-entry-item:hover { border-color: #409eff; transform: translateY(-2px); box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12); }
.feature-entry-title { font-size: 16px; font-weight: 700; color: #303133; }
.feature-entry-desc { margin-top: 10px; font-size: 12px; line-height: 1.7; color: #7b8794; }

.member-extra-card { margin-top: 16px; }
.member-row-readonly .el-table__cell { background: #fafafa; }
.member-row-self .el-table__cell { background: #f5f7fa; }
.member-row-readonly-text { color: #909399; font-size: 12px; }
.member-role-cell { display: flex; align-items: center; min-height: 28px; }

</style>
