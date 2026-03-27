<template>
  <div class="project-manage-page">
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
      <el-tab-pane :label="`我的任务 (${myTasks.length})`" name="my-tasks"></el-tab-pane>
      <el-tab-pane :label="`任务管理 (${tasks.length})`" name="task-manage"></el-tab-pane>
      <el-tab-pane :label="`成员管理 (${members.length})`" name="member-manage"></el-tab-pane>
      <el-tab-pane :label="`文件管理 (${files.length})`" name="file-manage"></el-tab-pane>
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

    <div v-if="activeTab === 'my-tasks'" class="tab-panel">
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

    <div v-if="activeTab === 'task-manage'" class="tab-panel">
      <el-card shadow="never">
        <div slot="header" class="card-header">
          <span>任务管理</span>
          <div class="toolbar-actions">
            <el-input v-model="taskFilter.keyword" size="small" clearable placeholder="搜索任务标题或描述" class="toolbar-input"></el-input>
            <el-select v-model="taskFilter.status" size="small" clearable placeholder="状态" class="toolbar-select">
              <el-option label="全部" value="all"></el-option>
              <el-option label="待处理" value="todo"></el-option>
              <el-option label="进行中" value="in_progress"></el-option>
              <el-option label="已完成" value="done"></el-option>
            </el-select>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreateTaskDialog">新建任务</el-button>
          </div>
        </div>
        <el-table :data="filteredTasks" border>
          <el-table-column prop="title" label="标题" min-width="220"></el-table-column>
          <el-table-column prop="assigneeName" label="负责人" width="140"></el-table-column>
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
            <template slot-scope="scope">{{ formatTime(scope.row.dueDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" @click="openEditTaskDialog(scope.row)">编辑</el-button>
              <el-dropdown @command="command => handleTaskQuickAction(command, scope.row)">
                <el-button size="mini" type="primary">状态<i class="el-icon-arrow-down el-icon--right"></i></el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="todo">待处理</el-dropdown-item>
                  <el-dropdown-item command="in_progress">进行中</el-dropdown-item>
                  <el-dropdown-item command="done">已完成</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
              <el-button size="mini" type="danger" @click="deleteTask(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
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
          <el-table-column prop="role" label="角色" width="120">
            <template slot-scope="scope">
              <el-tag size="mini" :type="scope.row.role === 'owner' ? 'danger' : 'info'">{{ getMemberRoleText(scope.row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="joinTime" label="加入时间" width="180">
            <template slot-scope="scope">{{ formatTime(scope.row.joinTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" @click="openEditRoleDialog(scope.row)" :disabled="!scope.row.memberId">修改角色</el-button>
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
            <el-button type="primary" size="small" icon="el-icon-upload" @click="openUploadFileDialog">上传文件</el-button>
            <el-button size="small" icon="el-icon-refresh" @click="loadFiles">刷新</el-button>
          </div>
        </div>
        <el-table :data="filteredFiles" border>
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

    <el-dialog title="添加成员" :visible.sync="addMemberDialogVisible" width="420px">
      <el-form :model="newMemberForm" label-width="90px">
        <el-form-item label="用户ID">
          <el-input-number v-model="newMemberForm.userId" :min="1" controls-position="right" style="width: 100%"></el-input-number>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="newMemberForm.role" style="width: 100%">
            <el-option label="成员" value="member"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="查看者" value="viewer"></el-option>
          </el-select>
        </el-form-item>
        <div class="dialog-tip">当前后端添加成员接口使用 userId，所以这里直接输入用户 ID。</div>
      </el-form>
      <span slot="footer">
        <el-button @click="addMemberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addMember">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="修改成员角色" :visible.sync="editRoleDialogVisible" width="420px">
      <el-form :model="editRoleForm" label-width="90px">
        <el-form-item label="角色">
          <el-select v-model="editRoleForm.role" style="width: 100%">
            <el-option label="成员" value="member"></el-option>
            <el-option label="管理员" value="admin"></el-option>
            <el-option label="查看者" value="viewer"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateMemberRole">保存</el-button>
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

export default {
  layout: 'project',
  data() {
    return {
      projectId: null,
      activeTab: 'overview',
      project: {},
      tasks: [],
      myTasks: [],
      members: [],
      contributors: [],
      files: [],
      recentActivities: [],
      taskFilter: { keyword: '', status: 'all' },
      memberFilter: { keyword: '' },
      fileFilter: { keyword: '' },
      taskDialogVisible: false,
      taskDialogType: 'create',
      taskForm: { id: null, title: '', description: '', assigneeId: null, status: 'todo', priority: 'medium', dueDate: '' },
      addMemberDialogVisible: false,
      editRoleDialogVisible: false,
      newMemberForm: { userId: null, role: 'member' },
      editRoleForm: { memberId: null, role: 'member' },
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
      }
    }
  },
  computed: {
    filteredTasks() {
      return this.tasks.filter(task => {
        const keyword = (this.taskFilter.keyword || '').trim().toLowerCase()
        const matchesKeyword = !keyword || task.title.toLowerCase().includes(keyword) || (task.description || '').toLowerCase().includes(keyword)
        const matchesStatus = !this.taskFilter.status || this.taskFilter.status === 'all' || task.status === this.taskFilter.status
        return matchesKeyword && matchesStatus
      })
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
    }
  },
  async mounted() {
    this.projectId = this.$route.query.projectId || this.$route.params.id
    if (!this.projectId) {
      this.$message.error('项目ID不存在')
      return
    }
    await this.refreshAll()
  },
  methods: {
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
      await Promise.all([
        this.loadProjectData(),
        this.loadTasks(),
        this.loadMyTasks(),
        this.loadMembers(),
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
      try {
        const response = await listProjectTasks(this.projectId)
        this.tasks = (response.data || []).map(this.normalizeTask)
      } catch (error) {
        console.error('加载任务列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载任务列表失败')
      }
    },
    async loadMyTasks() {
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
        this.members = [owner, ...rows].filter(Boolean)
      } catch (error) {
        console.error('加载成员列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载成员列表失败')
      }
    },
    async loadFiles() {
      try {
        const response = await listProjectFiles(this.projectId)
        this.files = (response.data || []).map(this.normalizeFile)
      } catch (error) {
        console.error('加载文件列表失败:', error)
        this.$message.error(error.response?.data?.message || '加载文件列表失败')
      }
    },
    openCreateTaskDialog() {
      this.taskDialogType = 'create'
      this.resetTaskForm()
      this.taskDialogVisible = true
    },
    openEditTaskDialog(task) {
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
          await createTask({ projectId: Number(this.projectId), ...payload })
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
    handleTaskQuickAction(command, row) {
      this.changeTaskStatus(row.id, command)
    },
    openAddMemberDialog() {
      this.newMemberForm = { userId: null, role: 'member' }
      this.addMemberDialogVisible = true
    },
    async addMember() {
      if (!this.newMemberForm.userId) {
        this.$message.warning('请输入用户ID')
        return
      }
      try {
        await addProjectMember({ projectId: Number(this.projectId), userId: Number(this.newMemberForm.userId), role: this.newMemberForm.role })
        this.$message.success('添加成员成功')
        this.addMemberDialogVisible = false
        await this.loadMembers()
        this.rebuildOverview()
      } catch (error) {
        console.error('添加成员失败:', error)
        this.$message.error(error.response?.data?.message || '添加成员失败')
      }
    },
    openEditRoleDialog(member) {
      if (!member.memberId) {
        this.$message.warning('项目所有者角色不能在这里修改')
        return
      }
      this.editRoleForm = { memberId: member.memberId, role: member.role }
      this.editRoleDialogVisible = true
    },
    async updateMemberRole() {
      try {
        await updateProjectMemberRole({ memberId: this.editRoleForm.memberId, role: this.editRoleForm.role })
        this.$message.success('角色更新成功')
        this.editRoleDialogVisible = false
        await this.loadMembers()
        this.rebuildOverview()
      } catch (error) {
        console.error('更新成员角色失败:', error)
        this.$message.error(error.response?.data?.message || '角色更新失败')
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
  .toolbar-select {
    width: 100%;
  }
  .activity-item {
    grid-template-columns: 40px 1fr;
  }
  .activity-time {
    grid-column: 2 / 3;
  }
}
</style>
