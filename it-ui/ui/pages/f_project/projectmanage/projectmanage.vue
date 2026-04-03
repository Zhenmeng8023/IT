<template>
  <div v-if="pageReady" class="project-manage-page">
    <div class="manage-header">
      <div>
        <div class="header-top">
          <el-button type="text" icon="el-icon-arrow-left" @click="goToDetail">返回项目详情</el-button>
        </div>
        <h1 class="page-title">{{ project.title || '项目管理' }}</h1>
        <p class="page-subtitle">{{ project.description || '在这里管理任务、成员、文件和项目设置。' }}</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" icon="el-icon-plus" @click="openCreateProjectDialog">新建项目</el-button>
        <el-button icon="el-icon-setting" @click="openSettingsDialog">项目设置</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="manage-tabs">
      <el-tab-pane label="概览" name="overview"></el-tab-pane>
      <el-tab-pane v-if="canSeeTaskCollaboration" :label="`我的任务 (${myTasks.length})`" name="my-tasks"></el-tab-pane>
      <el-tab-pane v-if="canSeeTaskCollaboration" :label="`任务管理 (${tasks.length})`" name="task-manage"></el-tab-pane>
      <el-tab-pane :label="`成员管理 (${members.length})`" name="member-manage"></el-tab-pane>
      <el-tab-pane :label="`文件管理 (${files.length})`" name="file-manage"></el-tab-pane>
      <el-tab-pane :label="`文档管理 (${docCount})`" name="doc-manage"></el-tab-pane>
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
            <div class="stat-number">{{ myTaskDoneCount }}</div>
            <div class="stat-label">我已完成任务</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="16">
          <el-card shadow="never">
            <div slot="header" class="card-header">
              <span>最近活动</span>
              <el-button type="text" @click="refreshAll">刷新</el-button>
            </div>
            <div v-if="recentActivities.length > 0" class="activity-list">
              <div v-for="item in recentActivities" :key="item.id" class="activity-item">
                <div class="activity-left">
                  <el-avatar :size="32" :src="item.avatar"></el-avatar>
                </div>
                <div class="activity-right">
                  <div class="activity-title">{{ item.title }}</div>
                  <div class="activity-desc">{{ item.description }}</div>
                </div>
                <div class="activity-time">{{ formatTime(item.time) }}</div>
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
              <el-select size="mini" :value="scope.row.status" @change="changeTaskStatus(scope.row.id, $event)">
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
              <el-option
                v-for="member in taskAssigneeOptions"
                :key="member.userId"
                :label="member.name"
                :value="member.userId"
              ></el-option>
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
                <el-avatar :size="30" :src="scope.row.assigneeAvatar || ''">
                  {{ (scope.row.assigneeName || '未').slice(0, 1) }}
                </el-avatar>
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
                <el-select
                  size="mini"
                  :value="scope.row.status"
                  class="task-inline-select"
                  @change="changeTaskStatus(scope.row.id, $event)"
                >
                  <el-option label="待处理" value="todo"></el-option>
                  <el-option label="进行中" value="in_progress"></el-option>
                  <el-option label="已完成" value="done"></el-option>
                </el-select>
                <el-select
                  size="mini"
                  :value="scope.row.assigneeId"
                  placeholder="切换负责人"
                  class="task-inline-select"
                  :disabled="!taskAssigneeOptions.length"
                  @change="changeTaskAssignee(scope.row.id, $event)"
                >
                  <el-option
                    v-for="member in taskAssigneeOptions"
                    :key="'quick-' + member.userId"
                    :label="member.name"
                    :value="member.userId"
                  ></el-option>
                </el-select>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" plain @click="openTaskCollab(scope.row, 'comment')">协作详情</el-button>
              <el-button size="mini" @click="openEditTaskDialog(scope.row)">编辑</el-button>
              <el-button size="mini" type="danger" @click="deleteTask(scope.row.id)">删除</el-button>
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
            <el-input v-model="memberFilter.keyword" size="small" clearable placeholder="搜索成员昵称/用户名" class="toolbar-input"></el-input>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddMemberDialog">添加成员</el-button>
            <el-button type="warning" size="small" icon="el-icon-switch-button" @click="quitProject">退出项目</el-button>
          </div>
        </div>
        <el-table :data="filteredMembers" border>
          <el-table-column prop="name" label="昵称" min-width="140"></el-table-column>
          <el-table-column prop="username" label="用户名" min-width="160"></el-table-column>
          <el-table-column prop="role" label="角色" width="180">
            <template slot-scope="scope">
              <el-tag v-if="!scope.row.memberId || scope.row.role === 'owner'" size="mini" :type="scope.row.role === 'owner' ? 'danger' : 'info'">{{ getMemberRoleText(scope.row.role) }}</el-tag>
              <el-select
                v-else
                v-model="scope.row.role"
                size="mini"
                style="width: 130px"
                :loading="roleSavingMemberId === scope.row.memberId"
                :disabled="roleSavingMemberId === scope.row.memberId"
                @change="value => updateMemberRoleInline(scope.row, value)"
              >
                <el-option label="成员" value="member"></el-option>
                <el-option label="管理员" value="admin"></el-option>
                <el-option label="查看者" value="viewer"></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="joinTime" label="加入时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.joinTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="danger" @click="deleteMember(scope.row)" :disabled="!scope.row.memberId">移除</el-button>
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
        <el-table ref="fileTableRef" :data="filteredFiles" border @selection-change="handleFileSelectionChange">
          <el-table-column type="selection" width="55" align="center"></el-table-column>
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



    <ProjectTaskCollabDrawer
      :visible.sync="taskCollabDrawerVisible"
      :task="selectedTaskForCollab"
      :project-id="projectId"
      :active-tab.sync="taskCollabActiveTab"
      :refresh-seed="taskCollabRefreshSeed"
      @changed="handleTaskCollabChanged"
      @close="handleTaskCollabDrawerClosed"
    />

    <el-dialog :title="taskDialogTitle" :visible.sync="taskDialogVisible" width="600px" @close="resetTaskForm">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="任务标题">
          <el-input v-model="taskForm.title" placeholder="请输入任务标题"></el-input>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="4"></el-input>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="taskForm.assigneeId" style="width: 100%">
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
          <el-select v-model="taskForm.status" style="width: 100%">
            <el-option label="待处理" value="todo"></el-option>
            <el-option label="进行中" value="in_progress"></el-option>
            <el-option label="已完成" value="done"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="taskForm.dueDate" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择截止时间" style="width: 100%"></el-date-picker>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTask">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="添加成员" :visible.sync="addMemberDialogVisible" width="460px" @close="resetAddMemberForm">
      <el-form :model="newMemberForm" label-width="90px">
        <el-form-item label="搜索用户">
          <el-select
            v-model="newMemberForm.userId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="输入用户昵称或用户名搜索"
            :remote-method="searchNewMemberUsers"
            :loading="newMemberSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="user in newMemberUserOptions"
              :key="user.id"
              :label="buildNewMemberUserLabel(user)"
              :value="user.id"
            >
              <div class="member-user-option">
                <div class="member-user-option__title">{{ user.nickname || user.username || ('用户' + user.id) }}</div>
                <div class="member-user-option__desc">{{ user.username || '-' }} · ID {{ user.id }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <div v-if="selectedNewMemberUser" class="selected-user-card">
          <el-avatar :size="34" :src="selectedNewMemberUser.avatarUrl || ''">
            {{ (selectedNewMemberUser.nickname || selectedNewMemberUser.username || 'U').slice(0, 1) }}
          </el-avatar>
          <div class="selected-user-card__text">
            <div class="selected-user-card__name">{{ selectedNewMemberUser.nickname || selectedNewMemberUser.username || ('用户' + selectedNewMemberUser.id) }}</div>
            <div class="selected-user-card__meta">{{ selectedNewMemberUser.username || '-' }} · ID {{ selectedNewMemberUser.id }}</div>
          </div>
        </div>
        <el-form-item label="角色">
          <el-select v-model="newMemberForm.role" style="width: 100%">
            <el-option label="成员" value="member"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="查看者" value="viewer"></el-option>
          </el-select>
        </el-form-item>
        <div class="dialog-tip">先输入昵称或用户名搜索，再从结果中选择要加入项目的用户；提交时仍会把所选用户的 userId 发送给后端添加成员接口。</div>
      </el-form>
      <span slot="footer">
        <el-button @click="addMemberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addMember">确定</el-button>
      </span>
    </el-dialog>

    

    <el-dialog title="上传项目文件" :visible.sync="fileUploadDialogVisible" width="520px" @close="resetFileUploadForm">
      <el-form :model="fileUploadForm" label-width="100px">
        <el-form-item label="版本号">
          <el-input v-model="fileUploadForm.version" placeholder="例如：1.0"></el-input>
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="fileUploadForm.commitMessage" type="textarea" :rows="3"></el-input>
        </el-form-item>
        <el-form-item label="是否主文件">
          <el-switch v-model="fileUploadForm.isMain"></el-switch>
        </el-form-item>
        <el-form-item label="选择文件">
          <input type="file" @change="handleUploadFileChange" class="native-file-input">
        </el-form-item>
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
        <el-table-column prop="fileSizeBytes" label="大小" width="120">
          <template slot-scope="scope">{{ formatFileSize(scope.row.fileSizeBytes) }}</template>
        </el-table-column>
        <el-table-column prop="uploadedAt" label="上传时间" width="180">
          <template slot-scope="scope">{{ formatTime(scope.row.uploadedAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!fileVersionsLoading && fileVersions.length === 0" description="暂无版本记录"></el-empty>
    </el-dialog>

    <el-dialog title="上传文件新版本" :visible.sync="versionDialogVisible" width="520px" @close="resetVersionForm">
      <el-form :model="versionForm" label-width="100px">
        <el-form-item label="当前文件">
          <div class="dialog-file-name">{{ versionForm.fileName || '-' }}</div>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="versionForm.version" placeholder="例如：1.1 / 2.0.0"></el-input>
        </el-form-item>
        <el-form-item label="版本说明">
          <el-input v-model="versionForm.commitMessage" type="textarea" :rows="3"></el-input>
        </el-form-item>
        <el-form-item label="选择文件">
          <input type="file" @change="handleVersionFileChange" class="native-file-input">
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="versionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="versionLoading" @click="submitUploadNewVersion">上传</el-button>
      </span>
    </el-dialog>

    <el-dialog title="项目设置" :visible.sync="settingsDialogVisible" width="620px">
      <el-form :model="settingsForm" label-width="100px">
        <el-form-item label="项目名称">
          <el-input v-model="settingsForm.name"></el-input>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="settingsForm.description" type="textarea" :rows="4"></el-input>
        </el-form-item>
        <el-form-item label="项目分类">
          <el-input v-model="settingsForm.category"></el-input>
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="settingsForm.status" style="width: 100%">
            <el-option label="草稿" value="draft"></el-option>
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已发布" value="published"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
            <el-option label="已归档" value="archived"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="settingsForm.visibility" style="width: 100%">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私有" value="private"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-select v-model="settingsForm.tags" multiple allow-create filterable default-first-option style="width: 100%">
            <el-option v-for="tag in settingsForm.tags" :key="tag" :label="tag" :value="tag"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingsLoading" @click="submitSettings">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="新建项目" :visible.sync="createProjectDialogVisible" width="620px" @close="resetProjectForm">
      <el-form :model="projectForm" :rules="projectRules" ref="projectFormRef" label-width="100px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="projectForm.name"></el-input>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="4"></el-input>
        </el-form-item>
        <el-form-item label="项目分类">
          <el-input v-model="projectForm.category"></el-input>
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="projectForm.status" style="width: 100%">
            <el-option label="草稿" value="draft"></el-option>
            <el-option label="待审核" value="pending"></el-option>
            <el-option label="已发布" value="published"></el-option>
            <el-option label="已拒绝" value="rejected"></el-option>
            <el-option label="已归档" value="archived"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="projectForm.visibility" style="width: 100%">
            <el-option label="公开" value="public"></el-option>
            <el-option label="私有" value="private"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-input v-model="projectForm.tags" type="textarea" :rows="2" placeholder='请输入 JSON 数组，例如：["Java", "Spring Boot"]'></el-input>
        </el-form-item>
        <el-form-item label="模板ID">
          <el-input-number v-model="projectForm.templateId" :min="0" :step="1"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="createProjectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createProjectLoading" @click="submitCreateProject">创建</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getProjectDetail,
  updateProject,
  listProjectTasks,
  listMyTasks,
  createTask,
  updateTask,
  deleteTask as apiDeleteTask,
  updateTaskStatus,
  listProjectMembers,
  searchProjectMemberUsers,
  addProjectMember,
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
  createProject
} from '@/api/project'
import ProjectDocList from './components/ProjectDocList.vue'
import ProjectTaskCollabDrawer from '../components/ProjectTaskCollabDrawer.vue'
import { getToken } from '@/utils/auth'

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
    ProjectTaskCollabDrawer
  },
  data() {
    return {
      projectId: null,
      activeTab: 'overview',
      pageReady: false,
      project: {},
      tasks: [],
      myTasks: [],
      members: [],
      contributors: [],
      files: [],
      recentActivities: [],
      taskFilter: { keyword: '', status: 'all', priority: 'all', assigneeId: 'all', sortBy: 'updatedAt', sortOrder: 'desc' },
      memberFilter: { keyword: '' },
      fileFilter: { keyword: '' },
      selectedFileRows: [],
      taskDialogVisible: false,
      taskDialogType: 'create',
      taskForm: { id: null, title: '', description: '', assigneeId: null, status: 'todo', priority: 'medium', dueDate: '' },
      addMemberDialogVisible: false,
      newMemberForm: { userId: null, role: 'member' },
      newMemberSearchLoading: false,
      newMemberUserOptions: [],
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
      settingsForm: { name: '', description: '', category: '', status: 'draft', visibility: 'public', tags: [] },
      createProjectDialogVisible: false,
      createProjectLoading: false,
      projectForm: { name: '', description: '', category: '', status: 'draft', visibility: 'public', tags: '', templateId: null },
      projectRules: {
        name: [
          { required: true, message: '请输入项目名称', trigger: 'blur' },
          { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
        ],
        tags: [{
          validator: (rule, value, callback) => {
            if (!value) return callback()
            try {
              JSON.parse(value)
              callback()
            } catch (e) {
              callback(new Error('请输入合法的 JSON 数组字符串'))
            }
          },
          trigger: 'blur'
        }]
      },
      docCount: 0,
      taskCollabDrawerVisible: false,
      selectedTaskForCollab: null,
      taskCollabActiveTab: 'comment',
      taskCollabRefreshSeed: 0,
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
    filteredTasks() {
      const list = this.tasks.filter(task => {
        const keyword = (this.taskFilter.keyword || '').trim().toLowerCase()
        const matchesKeyword = !keyword || (task.title || '').toLowerCase().includes(keyword) || (task.description || '').toLowerCase().includes(keyword)
        const matchesStatus = !this.taskFilter.status || this.taskFilter.status === 'all' || task.status === this.taskFilter.status
        const matchesPriority = !this.taskFilter.priority || this.taskFilter.priority === 'all' || task.priority === this.taskFilter.priority
        const assigneeFilter = this.taskFilter.assigneeId
        const matchesAssignee = !assigneeFilter || assigneeFilter === 'all'
          ? true
          : assigneeFilter === 'unassigned'
            ? !task.assigneeId
            : Number(task.assigneeId) === Number(assigneeFilter)
        return matchesKeyword && matchesStatus && matchesPriority && matchesAssignee
      })
      const order = this.taskFilter.sortOrder === 'asc' ? 1 : -1
      const priorityWeight = { low: 1, medium: 2, high: 3, urgent: 4 }
      return list.slice().sort((a, b) => {
        const sortBy = this.taskFilter.sortBy || 'updatedAt'
        if (sortBy === 'priority') {
          return ((priorityWeight[a.priority] || 0) - (priorityWeight[b.priority] || 0)) * order
        }
        if (sortBy === 'title') {
          return String(a.title || '').localeCompare(String(b.title || ''), 'zh-CN') * order
        }
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
      return this.members
        .filter(member => member && member.userId)
        .map(member => ({
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
    taskDialogTitle() {
      return this.taskDialogType === 'create' ? '新建任务' : '编辑任务'
    },
    myTaskDoneCount() {
      return this.myTasks.filter(task => task.status === 'done').length
    },
    selectedNewMemberUser() {
      return this.newMemberUserOptions.find(item => Number(item.id) === Number(this.newMemberForm.userId)) || null
    }
  },
  async mounted() {
    this.pageReady = false
    this.projectId = this.$route.query.projectId || this.$route.params.id
    const routeTab = this.$route.query.tab
    if (routeTab) this.activeTab = routeTab
    if (!['overview', 'my-tasks', 'task-manage', 'member-manage', 'file-manage', 'doc-manage'].includes(this.activeTab)) {
      this.activeTab = 'overview'
    }
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
    ensureTaskCollaborationAccess(redirect = false, showFeedback = false) {
      if (this.canSeeTaskCollaboration) return true
      this.tasks = []
      this.myTasks = []
      if (this.activeTab === 'my-tasks' || this.activeTab === 'task-manage') {
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
        if (latestTask) {
          this.selectedTaskForCollab = { ...latestTask }
        }
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
        completedAt: task.completedAt
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
    rebuildOverview() {
      const owner = this.buildOwnerRow()
      const memberRows = this.members.filter(item => !item.isOwner)
      this.contributors = [owner, ...memberRows].filter(Boolean).slice(0, 8)
      const activities = []
      this.tasks.forEach(task => {
        activities.push({
          id: `task-${task.id}`,
          avatar: '',
          title: `任务：${task.title}`,
          description: `状态 ${this.getTaskStatusText(task.status)}，负责人 ${task.assigneeName}`,
          time: task.updatedAt || task.createdAt || task.completedAt
        })
      })
      this.members.forEach(member => {
        activities.push({
          id: `member-${member.id}`,
          avatar: member.avatar,
          title: `成员：${member.name}`,
          description: `以 ${this.getMemberRoleText(member.role)} 身份加入项目`,
          time: member.joinTime
        })
      })
      this.files.forEach(file => {
        activities.push({
          id: `file-${file.id}`,
          avatar: '',
          title: `文件：${file.fileName}`,
          description: `版本 ${file.version || '-'}${file.isMain ? ' · 主文件' : ''}`,
          time: file.uploadTime
        })
      })
      this.recentActivities = activities
        .filter(item => item.time)
        .sort((a, b) => new Date(b.time) - new Date(a.time))
        .slice(0, 8)
    },
    async refreshAll() {
      await this.loadProjectData()
      if (this.activeTab === 'my-tasks' || this.activeTab === 'task-manage') {
        if (this.currentUserId === null || this.currentUserId === undefined) {
          this.ensureTaskCollaborationAccess(true, false)
          return
        }
      }
      if (this.currentUserId === null || this.currentUserId === undefined) {
        this.tasks = []
        this.myTasks = []
        await this.loadFiles()
        this.rebuildOverview()
        return
      }
      await this.loadMembers()
      if (!this.ensureTaskCollaborationAccess(true, false)) {
        return
      }
      await Promise.all([
        this.loadTasks(),
        this.loadMyTasks(),
        this.loadFiles()
      ])
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
        this.clearFileSelection()
      } catch (error) {
        console.error('加载文件列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载文件列表失败')
      }
    },
    openCreateTaskDialog() {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      this.taskDialogType = 'create'
      this.resetTaskForm()
      this.taskDialogVisible = true
    },
    openEditTaskDialog(task) {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      this.taskDialogType = 'edit'
      this.taskForm = {
        id: task.id,
        title: task.title,
        description: task.description,
        assigneeId: task.assigneeId,
        status: task.status,
        priority: task.priority,
        dueDate: task.dueDate || ''
      }
      this.taskDialogVisible = true
    },
    resetTaskForm() {
      this.taskForm = { id: null, title: '', description: '', assigneeId: this.project.authorId || null, status: 'todo', priority: 'medium', dueDate: '' }
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
          await updateTask(this.taskForm.id, { ...payload, status: this.taskForm.status })
          this.$message.success('任务更新成功')
        }
        this.taskDialogVisible = false
        await Promise.all([this.loadTasks(), this.loadMyTasks()])
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
        await Promise.all([this.loadTasks(), this.loadMyTasks()])
        this.rebuildOverview()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除任务失败:', error)
          this.$message.error(error.response?.data?.message || '删除失败')
        }
      }
    },
    async changeTaskStatus(taskId, status) {
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      try {
        await updateTaskStatus(taskId, { status })
        this.$message.success('状态更新成功')
        await Promise.all([this.loadTasks(), this.loadMyTasks()])
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
      try {
        await updateTask(taskId, { assigneeId: Number(assigneeId) })
        this.$message.success('负责人更新成功')
        await Promise.all([this.loadTasks(), this.loadMyTasks()])
        this.rebuildOverview()
      } catch (error) {
        console.error('更新负责人失败:', error)
        this.$message.error(error.response?.data?.message || '负责人更新失败')
      }
    },
    handleFileSelectionChange(rows) {
      this.selectedFileRows = Array.isArray(rows) ? rows.slice() : []
    },
    clearFileSelection() {
      this.selectedFileRows = []
      this.$nextTick(() => {
        if (this.$refs.fileTableRef && this.$refs.fileTableRef.clearSelection) {
          this.$refs.fileTableRef.clearSelection()
        }
      })
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
        await this.loadFiles()
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
    handleTaskQuickAction(command, row) {
      this.changeTaskStatus(row.id, command)
    },
    openAddMemberDialog() {
      this.resetAddMemberForm()
      this.addMemberDialogVisible = true
    },
    resetAddMemberForm() {
      this.newMemberForm = { userId: null, role: 'member' }
      this.newMemberUserOptions = []
      this.newMemberSearchLoading = false
    },
    buildNewMemberUserLabel(user) {
      const displayName = user.nickname || user.username || `用户${user.id}`
      return `${displayName}（${user.username || '-'} / ID ${user.id}）`
    },
    async searchNewMemberUsers(keyword) {
      const value = (keyword || '').trim()
      if (!value) {
        this.newMemberUserOptions = []
        return
      }
      this.newMemberSearchLoading = true
      try {
        const response = await searchProjectMemberUsers(value, 10)
        const memberUserIdSet = new Set(this.members.map(item => Number(item.userId)))
        this.newMemberUserOptions = (response.data || []).filter(user => !memberUserIdSet.has(Number(user.id)))
      } catch (error) {
        console.error('搜索用户失败:', error)
        this.newMemberUserOptions = []
        this.$message.error(error.response?.data?.message || '搜索用户失败')
      } finally {
        this.newMemberSearchLoading = false
      }
    },
    async addMember() {
      if (!this.newMemberForm.userId) {
        this.$message.warning('请先选择用户')
        return
      }
      try {
        await addProjectMember({ projectId: Number(this.projectId), userId: Number(this.newMemberForm.userId), role: this.newMemberForm.role })
        this.$message.success('添加成员成功')
        this.addMemberDialogVisible = false
        this.resetAddMemberForm()
        await this.loadMembers()
        this.rebuildOverview()
      } catch (error) {
        console.error('添加成员失败:', error)
        this.$message.error(error.response?.data?.message || '添加成员失败')
      }
    },
    async updateMemberRoleInline(member, role) {
      if (!member.memberId) {
        this.$message.warning('项目所有者角色不能在这里修改')
        return
      }
      const previousRole = member.role
      member.role = role
      this.roleSavingMemberId = member.memberId
      try {
        await updateProjectMemberRole({ memberId: member.memberId, role })
        this.$message.success('角色更新成功')
        await this.loadMembers()
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
      try {
        await this.$confirm(`确定移除成员 ${member.name} 吗？`, '提示', { type: 'warning' })
        await removeProjectMember(member.memberId)
        this.$message.success('移除成功')
        await this.loadMembers()
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
        await this.loadFiles()
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
        await this.loadFiles()
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
        await this.loadFiles()
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
        await this.loadFiles()
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
        status: this.project.status || 'draft',
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
          status: this.settingsForm.status || 'draft',
          visibility: this.settingsForm.visibility || 'public',
          tags: JSON.stringify(this.settingsForm.tags || [])
        })
        this.$message.success('项目设置更新成功')
        this.settingsDialogVisible = false
        await this.loadProjectData()
        this.rebuildOverview()
      } catch (error) {
        console.error('更新项目设置失败:', error)
        this.$message.error(error.response?.data?.message || '更新项目设置失败')
      } finally {
        this.settingsLoading = false
      }
    },
    openCreateProjectDialog() {
      this.resetProjectForm()
      this.createProjectDialogVisible = true
    },
    resetProjectForm() {
      this.projectForm = { name: '', description: '', category: '', status: 'draft', visibility: 'public', tags: '', templateId: null }
      this.$nextTick(() => {
        if (this.$refs.projectFormRef) this.$refs.projectFormRef.clearValidate()
      })
    },
    async submitCreateProject() {
      try {
        await this.$refs.projectFormRef.validate()
        this.createProjectLoading = true
        const requestData = {
          name: this.projectForm.name,
          description: this.projectForm.description || undefined,
          category: this.projectForm.category || undefined,
          status: this.projectForm.status || 'draft',
          visibility: this.projectForm.visibility || 'public',
          templateId: this.projectForm.templateId || undefined
        }
        if (this.projectForm.tags) requestData.tags = this.projectForm.tags
        const response = await createProject(requestData)
        this.$message.success('项目创建成功')
        this.createProjectDialogVisible = false
        const newProjectId = response.data?.id || response.data?.data?.id
        if (newProjectId) this.$router.push(`/projectdetail?projectId=${newProjectId}`)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('创建项目失败:', error)
          this.$message.error(error.response?.data?.message || '创建项目失败，请重试')
        }
      } finally {
        this.createProjectLoading = false
      }
    },
    goToDetail() {
      this.$router.push(`/projectdetail?projectId=${this.projectId}`)
    },
    getProjectStatusType(status) {
      return {
        draft: 'info',
        pending: 'warning',
        published: 'success',
        rejected: 'danger',
        archived: 'info'
      }[status] || 'info'
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
    }
  }
}
</script>

<style scoped>
.project-manage-page {
  max-width: 1360px;
  margin: 0 auto;
  padding: 24px;
}
.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}
.header-top {
  margin-bottom: 8px;
}
.page-title {
  margin: 0;
  font-size: 30px;
  color: #303133;
}
.page-subtitle {
  margin: 8px 0 0;
  color: #909399;
}
.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.manage-tabs {
  margin-bottom: 16px;
}
.tab-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stats-row {
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}
.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  margin-top: 8px;
  color: #909399;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.toolbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.toolbar-input {
  width: 220px;
}
.toolbar-select {
  width: 140px;
}
.side-card {
  margin-bottom: 16px;
}
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #606266;
}
.contributor-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.contributor-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.contributor-text {
  display: flex;
  flex-direction: column;
}
.contributor-name {
  color: #303133;
  font-weight: 600;
}
.contributor-role {
  color: #909399;
  font-size: 12px;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.activity-item {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f4f4f5;
}
.activity-item:last-child {
  border-bottom: none;
}
.activity-title {
  color: #303133;
  font-weight: 600;
}
.activity-desc,
.activity-time,
.dialog-tip {
  color: #909399;
  font-size: 12px;
}
.activity-time {
  white-space: nowrap;
}

.task-card-header {
  align-items: flex-start;
}
.task-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.task-header-desc {
  color: #909399;
  font-size: 12px;
}
.task-summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.task-summary-card {
  padding: 14px 16px;
  border-radius: 12px;
  background: #f7f9fc;
  border: 1px solid #ebeef5;
}
.task-summary-card.danger {
  background: #fff6f6;
  border-color: #f5c2c7;
}
.task-summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.task-summary-label {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.task-title-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.task-title-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.task-title-link {
  padding: 0;
  color: #303133;
  font-weight: 600;
}
.task-title-link:hover {
  color: #409eff;
}
.task-title-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
.task-assignee-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.task-assignee-text {
  min-width: 0;
}
.task-assignee-name {
  color: #303133;
  font-weight: 600;
}
.task-assignee-meta {
  color: #909399;
  font-size: 12px;
}
.task-quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.task-inline-select {
  width: 100%;
}
.toolbar-select-wide {
  width: 180px;
}

.member-user-option {
  line-height: 1.4;
}
.member-user-option__title {
  font-size: 14px;
  color: #303133;
}
.member-user-option__desc {
  font-size: 12px;
  color: #909399;
}
.selected-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}
.selected-user-card__text {
  min-width: 0;
}
.selected-user-card__name {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}
.selected-user-card__meta {
  font-size: 12px;
  color: #909399;
}

.dialog-file-name {
  color: #303133;
  font-weight: 600;
}
.native-file-input {
  display: block;
  width: 100%;
}
@media (max-width: 768px) {
  .project-manage-page {
    padding: 16px;
  }
  .manage-header {
    flex-direction: column;
  }
  .toolbar-input,
  .toolbar-select,
  .toolbar-select-wide {
    width: 100%;
  }
  .task-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .activity-item {
    grid-template-columns: 40px 1fr;
  }
  .activity-time {
    grid-column: 2 / 3;
  }
}
@media (max-width: 480px) {
  .task-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>