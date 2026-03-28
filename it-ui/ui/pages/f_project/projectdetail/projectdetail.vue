<template>
  <div class="project-detail-container">
    <div class="detail-header">
      <div class="breadcrumb-wrap">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/projectlist' }">项目列表</el-breadcrumb-item>
          <el-breadcrumb-item>{{ project.name || '项目详情' }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-actions">
        <el-button size="small" icon="el-icon-s-tools" @click="goToProjectManage">
          项目管理
        </el-button>
        <el-button
          type="success"
          size="small"
          icon="el-icon-document"
          :loading="aiSummaryLoading"
          @click="handleAiSummarizeProject"
        >
          AI 总结项目
        </el-button>
        <el-button
          type="warning"
          size="small"
          icon="el-icon-s-operation"
          :loading="aiTaskLoading"
          @click="handleAiSplitProjectTasks"
        >
          AI 拆任务
        </el-button>
        <el-button
          type="primary"
          size="small"
          icon="el-icon-star-off"
          :loading="starLoading"
          @click="toggleStar"
        >
          {{ project.starred ? '取消收藏' : '收藏项目' }}
        </el-button>
        <el-button size="small" icon="el-icon-download" @click="downloadMainFile">
          下载主文件
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="project-overview-card">
      <div class="project-overview">
        <div class="overview-main">
          <div class="title-row">
            <h1 class="project-title">{{ project.name || '未命名项目' }}</h1>
            <div class="title-tags">
              <el-tag size="small" type="primary">{{ categoryLabel }}</el-tag>
              <el-tag size="small" :type="statusTagType">{{ statusLabel }}</el-tag>
              <el-tag v-if="project.visibility" size="small" type="info">{{ visibilityLabel }}</el-tag>
            </div>
          </div>
          <div class="project-desc">{{ project.description || '暂无项目描述' }}</div>
          <div class="tag-list" v-if="tagList.length">
            <el-tag
              v-for="tag in tagList"
              :key="tag"
              size="mini"
              effect="plain"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
          </div>
          <div class="meta-row">
            <div class="author-box">
              <el-avatar :size="40" :src="project.authorAvatar || ''">
                {{ (project.authorName || '未知作者').slice(0, 1) }}
              </el-avatar>
              <div class="author-text">
                <div class="author-name">{{ project.authorName || '未知作者' }}</div>
                <div class="author-time">创建于 {{ formatTime(project.createdAt) }}</div>
              </div>
            </div>
            <div class="stats-row">
              <div class="stat-item">
                <div class="stat-value">{{ project.stars || 0 }}</div>
                <div class="stat-label">收藏</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ project.downloads || 0 }}</div>
                <div class="stat-label">下载</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ project.views || 0 }}</div>
                <div class="stat-label">浏览</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <div class="content-layout">
      <div class="content-main">
        <el-card shadow="never" class="section-card">
          <div slot="header" class="section-header">
            <span>README</span>
            <el-button size="mini" type="text" icon="el-icon-edit" @click="showEditDialog = true">编辑项目</el-button>
          </div>
          <div class="readme-box ai-rich-content" v-html="renderedReadme"></div>
        </el-card>

        <el-card v-if="hasAiResult" shadow="never" class="section-card ai-result-card">
          <div slot="header" class="section-header section-header-flex">
            <span>AI 项目辅助结果</span>
            <div class="ai-result-header-actions">
              <el-tag size="mini" type="success" effect="plain">{{ lastAiModelLabel || currentAiModelLabel }}</el-tag>
              <el-button size="mini" type="text" @click="clearAiResult">清空</el-button>
            </div>
          </div>
          <el-tabs v-model="aiActiveTab" class="ai-result-tabs">
            <el-tab-pane label="项目总结" name="summary">
              <div v-if="aiProjectSummary" class="ai-result-box ai-rich-content" v-html="renderedAiProjectSummary"></div>
              <el-empty v-else description="还没有生成项目总结" :image-size="70" />
            </el-tab-pane>
            <el-tab-pane label="任务拆解" name="tasks">
              <div v-if="aiProjectTasks" class="ai-result-box ai-rich-content" v-html="renderedAiProjectTasks"></div>
              <el-empty v-else description="还没有生成任务拆解" :image-size="70" />
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-card shadow="never" class="section-card">
          <div slot="header" class="section-header section-header-flex">
            <span>项目文件</span>
            <div class="file-section-actions">
              <el-button
                size="mini"
                icon="el-icon-folder-checked"
                :disabled="!selectedFileIds.length"
                @click="handleBatchDownload"
              >
                批量下载<span v-if="selectedFileIds.length">（{{ selectedFileIds.length }}）</span>
              </el-button>
              <el-button size="mini" icon="el-icon-upload2" :loading="uploadLoading" @click="openUploadDialog(false)">
                上传文件
              </el-button>
              <el-button
                size="mini"
                icon="el-icon-top"
                :disabled="!currentFile.id"
                @click="openUploadDialog(true)"
              >
                上传新版本
              </el-button>
            </div>
          </div>
          <div class="file-browser">
            <div class="file-tree-panel">
              <el-input
                v-model="treeFilterText"
                size="small"
                clearable
                prefix-icon="el-icon-search"
                placeholder="搜索文件"
              />
              <div class="tree-selection-bar">
                <span>已勾选 {{ selectedFileIds.length }} 个文件</span>
                <div class="tree-selection-actions">
                  <el-button type="text" size="mini" @click="checkAllFiles">全选</el-button>
                  <el-button type="text" size="mini" @click="clearCheckedFiles">清空</el-button>
                </div>
              </div>
              <div class="tree-wrap">
                <el-tree
                  ref="fileTreeRef"
                  :data="fileTree"
                  :props="treeProps"
                  node-key="path"
                  default-expand-all
                  highlight-current
                  show-checkbox
                  :check-strictly="true"
                  :filter-node-method="filterNode"
                  @node-click="handleFileClick"
                  @check-change="handleTreeCheckChange"
                >
                  <span slot-scope="{ data }" class="tree-node">
                    <i :class="getTreeIcon(data)"></i>
                    <span class="tree-node-name">{{ data.name }}</span>
                    <span v-if="data.type === 'file' && data.isMain" class="main-file-badge">主文件</span>
                  </span>
                </el-tree>
              </div>
            </div>
            <div class="file-preview-panel">
              <div class="file-preview-toolbar">
                <div class="file-preview-title">
                  {{ currentFile.path || '请选择文件' }}
                </div>
                <div class="file-preview-actions">
                  <el-button size="mini" :disabled="!currentFile.id" @click="downloadCurrentFile">下载</el-button>
                  <el-button size="mini" :disabled="!currentFile.id" @click="markMainFile">设为主文件</el-button>
                  <el-button size="mini" type="danger" :disabled="!currentFile.id" @click="removeCurrentFile">删除</el-button>
                </div>
              </div>
              <div v-if="currentFile.id" class="file-preview-meta">
                <span>大小：{{ formatFileSize(currentFile.size) || '-' }}</span>
                <span>版本数：{{ currentFile.versions.length }}</span>
                <span>已勾选：{{ selectedFileIds.length }} 个</span>
              </div>
              <div v-if="currentFile.id" class="code-container">
                <div class="line-numbers">
                  <div v-for="i in (currentFile.content.match(/\n/g) || []).length + 1" :key="i" class="line-number">{{ i }}</div>
                </div>
                <pre class="code-content"><code :class="'language-' + currentFile.extension">{{ currentFile.content }}</code></pre>
              </div>
              <div v-else class="empty-preview">点击左侧文件查看内容</div>
            </div>
          </div>
          <div v-if="currentFile.versions.length" class="version-box">
            <div class="sub-title">版本记录</div>
            <el-timeline>
              <el-timeline-item
                v-for="version in currentFile.versions"
                :key="version.id || version.version"
                :timestamp="formatTime(version.createdAt || version.uploadedAt)"
              >
                <div class="version-item">
                  <span class="version-name">{{ version.version || '未命名版本' }}</span>
                  <span class="version-desc">{{ version.commitMessage || version.remark || '无说明' }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </div>

      <div class="content-side">
        <el-card shadow="never" class="section-card side-card">
          <div slot="header" class="section-header">
            <span>AI 项目助手</span>
          </div>
          <div class="ai-assistant-box">
            <div class="ai-model-field">
              <div class="ai-model-label">当前模型</div>
              <el-select
                v-model="selectedAiModelId"
                size="small"
                clearable
                filterable
                :loading="aiModelsLoading"
                placeholder="请选择 AI 模型"
                style="width: 100%"
                @change="handleAiModelChange"
              >
                <el-option
                  v-for="item in aiModels"
                  :key="item.id"
                  :label="formatAiModelOption(item)"
                  :value="item.id"
                />
              </el-select>
            </div>
            <div class="ai-model-tag-row">
              <el-tag size="mini" type="success" effect="plain">{{ currentAiModelLabel }}</el-tag>
              <el-tag v-if="currentAiProviderLabel" size="mini" type="info" effect="plain">{{ currentAiProviderLabel }}</el-tag>
            </div>
            <div class="ai-helper-text">
              已接入已启用模型列表；不手动选择时，会优先使用当前激活模型。
            </div>
            <div class="ai-helper-actions">
              <el-button
                size="small"
                type="success"
                plain
                :loading="aiSummaryLoading"
                @click="handleAiSummarizeProject"
              >
                生成项目总结
              </el-button>
              <el-button
                size="small"
                type="warning"
                plain
                :loading="aiTaskLoading"
                @click="handleAiSplitProjectTasks"
              >
                生成任务拆解
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="section-card side-card">
          <div slot="header" class="section-header">
            <span>项目信息</span>
          </div>
          <div class="info-list">
            <div class="info-item"><span class="info-label">项目 ID</span><span class="info-value">{{ project.id || '-' }}</span></div>
            <div class="info-item"><span class="info-label">状态</span><span class="info-value">{{ statusLabel }}</span></div>
            <div class="info-item"><span class="info-label">分类</span><span class="info-value">{{ categoryLabel }}</span></div>
            <div class="info-item"><span class="info-label">最后更新</span><span class="info-value">{{ formatTime(project.updatedAt) }}</span></div>
            <div class="info-item"><span class="info-label">可见性</span><span class="info-value">{{ visibilityLabel }}</span></div>
          </div>
        </el-card>

        <el-card shadow="never" class="section-card side-card">
          <div slot="header" class="section-header">
            <span>贡献者</span>
          </div>
          <div v-if="contributors.length" class="contributors-list">
            <div v-for="contributor in contributors" :key="contributor.id || contributor.userId" class="contributor-item">
              <el-avatar :size="34" :src="contributor.avatar || ''">
                {{ contributor.displayName.slice(0, 1) }}
              </el-avatar>
              <div class="contributor-text">
                <div class="contributor-name">{{ contributor.displayName }}</div>
                <div class="contributor-role">{{ roleLabel(contributor.role) }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无贡献者数据" :image-size="70" />
        </el-card>

        <el-card shadow="never" class="section-card side-card">
          <div slot="header" class="section-header">
            <span>相关项目</span>
          </div>
          <div v-if="relatedProjects.length" class="related-list">
            <div
              v-for="item in relatedProjects"
              :key="item.id"
              class="related-item"
              @click="goToDetail(item.id)"
            >
              <div class="related-title">{{ item.name || item.title || '未命名项目' }}</div>
              <div class="related-desc">{{ item.description || '暂无项目描述' }}</div>
              <div class="related-meta">{{ mapCategory(item.category) }} · {{ item.stars || 0 }} 收藏</div>
            </div>
          </div>
          <el-empty v-else description="暂无相关推荐" :image-size="70" />
        </el-card>
      </div>
    </div>

    <el-dialog title="编辑项目信息" :visible.sync="showEditDialog" width="640px" append-to-body>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="editForm.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="项目分类" prop="category">
          <el-select v-model="editForm.category" style="width: 100%" placeholder="请选择分类">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目状态" prop="status">
          <el-select v-model="editForm.status" style="width: 100%" placeholder="请选择状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性" prop="visibility">
          <el-radio-group v-model="editForm.visibility">
            <el-radio-button label="public">公开</el-radio-button>
            <el-radio-button label="private">私有</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目标签">
          <el-input v-model="editForm.tagsText" placeholder="多个标签请用逗号分隔，例如：Vue,SpringBoot,AI" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="submitEdit">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="uploadDialog.isVersion ? '上传新版本' : '上传项目文件'" :visible.sync="uploadDialog.visible" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="选择文件">
          <input ref="uploadInput" :multiple="!uploadDialog.isVersion" type="file" @change="handlePickedFiles" />
          <div class="upload-dialog-tip">
            <span v-if="uploadDialog.isVersion">版本上传仅支持单文件。</span>
            <span v-else>可一次选择多个文件批量上传。</span>
          </div>
          <div v-if="uploadDialog.fileNames.length" class="upload-file-list">
            <div v-for="name in uploadDialog.fileNames" :key="name" class="upload-file-item">{{ name }}</div>
          </div>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="uploadDialog.version" placeholder="例如：1.0.1" />
        </el-form-item>
        <el-form-item label="提交说明">
          <el-input v-model="uploadDialog.commitMessage" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item v-if="!uploadDialog.isVersion" label="设为主文件">
          <el-switch v-model="uploadDialog.isMain" />
          <div class="upload-dialog-tip">批量上传时，开启后会把第一个选中的文件设为主文件。</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="closeUploadDialog">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="submitUpload">上传</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getProjectDetail,
  getProjectContributors,
  getRelatedProjects,
  starProject,
  unstarProject,
  getProjectStarStatus,
  updateProject,
  listProjectFiles,
  listFileVersions,
  uploadProjectFile,
  uploadProjectFiles,
  uploadFileNewVersion,
  setMainFile,
  deleteFile,
  downloadFile,
  downloadProjectFiles
} from '@/api/project'
import { aiSummarizeProject, aiSplitProjectTasks } from '@/api/aiAssistant'
import { listEnabledAiModels, getActiveAiModel } from '@/api/aiAdmin'
import { getToken } from '@/utils/auth'

const CATEGORY_MAP = {
  frontend: '前端项目',
  backend: '后端项目',
  fullstack: '全栈项目',
  mobile: '移动应用',
  ai: 'AI 项目',
  tools: '工具项目'
}

const STATUS_MAP = {
  draft: '草稿',
  pending: '待审核',
  published: '已发布',
  rejected: '已拒绝',
  archived: '已归档'
}

const STATUS_TAG_MAP = {
  draft: 'info',
  pending: 'warning',
  published: 'success',
  rejected: 'danger',
  archived: 'info'
}

const ROLE_MAP = {
  owner: '创建者',
  admin: '管理员',
  member: '成员',
  viewer: '查看者'
}

const TEXT_EXTENSIONS = new Set([
  'js', 'ts', 'vue', 'json', 'md', 'txt', 'html', 'htm', 'css', 'scss', 'less',
  'java', 'kt', 'xml', 'yml', 'yaml', 'sql', 'sh', 'py', 'rb', 'go', 'rs', 'c',
  'cpp', 'h', 'hpp', 'cs', 'php', 'ini', 'log', 'properties'
])

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      if (Array.isArray(parsed)) return parsed.filter(Boolean)
    } catch (e) {}
    return tags.split(',').map(v => v.trim()).filter(Boolean)
  }
  return []
}

function toBackendDateTime(value) {
  if (!value) return undefined
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return undefined
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function extractApiData(res) {
  if (res == null) return null
  const payload = res.data !== undefined ? res.data : res
  if (
    payload &&
    typeof payload === 'object' &&
    payload.data !== undefined &&
    (payload.code !== undefined || payload.success !== undefined || payload.message !== undefined)
  ) {
    return payload.data
  }
  return payload
}

function normalizeAiModel(item = {}) {
  const rawId = item.id ?? item.modelId ?? item.value ?? item.code ?? ''
  return {
    ...item,
    id: rawId === null || rawId === undefined ? '' : String(rawId),
    rawId,
    modelName: item.modelName || item.name || item.label || item.model || item.code || '',
    providerCode: item.providerCode || item.provider || item.providerName || item.vendor || '',
    isEnabled: item.isEnabled !== false
  }
}

function buildProjectAiContent(project = {}, contributors = [], currentFile = {}) {
  const tags = parseTags(project.tags)
  const contributorNames = (contributors || [])
    .map(item => item && item.displayName)
    .filter(Boolean)

  const fileSummary = []
  if (Array.isArray(project.files) && project.files.length) {
    fileSummary.push(`文件数量：${project.files.length}`)
  }
  if (currentFile && currentFile.path) {
    fileSummary.push(`当前浏览文件：${currentFile.path}`)
  }

  return [
    `项目名称：${project.name || '未提供'}`,
    `项目描述：${project.description || '未提供'}`,
    `项目分类：${CATEGORY_MAP[project.category] || project.category || '未提供'}`,
    `项目状态：${STATUS_MAP[project.status] || project.status || '未提供'}`,
    `可见性：${project.visibility === 'private' ? '私有' : '公开'}`,
    `项目标签：${tags.length ? tags.join('、') : '未提供'}`,
    `作者：${project.authorName || '未提供'}`,
    `贡献者：${contributorNames.length ? contributorNames.join('、') : '未提供'}`,
    `项目文件：${fileSummary.length ? fileSummary.join('；') : '未提供'}`,
    `README：${project.readme || '未提供'}`
  ].join('\n')
}

function decodeJwtPayload(token = '') {
  try {
    const parts = String(token || '').split('.')
    if (parts.length < 2) return null
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + (4 - (base64.length % 4 || 4)) % 4, '=')
    const json = process.client ? window.atob(normalized) : Buffer.from(normalized, 'base64').toString('utf-8')
    return JSON.parse(json)
  } catch (e) {
    return null
  }
}

function pickUserIdFromObject(source) {
  if (!source || typeof source !== 'object') return null
  const queue = [source]
  const seen = new Set()
  const keys = ['id', 'userId', 'uid', 'memberId', 'loginId', 'accountId', 'sub']

  while (queue.length) {
    const current = queue.shift()
    if (!current || typeof current !== 'object' || seen.has(current)) continue
    seen.add(current)

    for (const key of keys) {
      const value = current[key]
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        const text = String(value).trim()
        if (/^\d+$/.test(text)) return Number(text)
        return text
      }
    }

    ;['user', 'userInfo', 'profile', 'account', 'loginUser', 'currentUser', 'data'].forEach((key) => {
      if (current[key] && typeof current[key] === 'object') {
        queue.push(current[key])
      }
    })
  }

  return null
}

function escapeHtmlValue(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInlineMarkdown(text) {
  return escapeHtmlValue(text)
    .replace(/&lt;br\s*\/?&gt;/gi, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
}

function parseMarkdownTableCells(line) {
  return String(line || '')
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map(cell => cell.trim())
}

function isMarkdownTableSeparator(line) {
  const cells = parseMarkdownTableCells(line)
  return cells.length > 0 && cells.every(cell => /^:?-{3,}:?$/.test(cell.replace(/\s+/g, '')))
}

function looksLikeMarkdownTableRow(line) {
  const text = String(line || '').trim()
  return text.includes('|') && parseMarkdownTableCells(text).length >= 2
}

function isSpecialMarkdownLine(line, nextLine) {
  const text = String(line || '').trim()
  if (!text) return true
  if (/^```/.test(text)) return true
  if (/^([-*_])\1{2,}$/.test(text)) return true
  if (/^#{1,6}\s+/.test(text)) return true
  if (/^>\s+/.test(text)) return true
  if (/^\s*[-*+]\s+/.test(text)) return true
  if (/^\s*\d+\.\s+/.test(text)) return true
  if (looksLikeMarkdownTableRow(text) && isMarkdownTableSeparator(nextLine)) return true
  return false
}

function renderMarkdownToHtml(source, emptyText = '暂无内容') {
  const raw = String(source || '').replace(/\r\n?/g, '\n').trim()
  if (!raw) {
    return `<div class="empty-readme">${escapeHtmlValue(emptyText)}</div>`
  }

  const lines = raw.split('\n')
  const blocks = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]
    const trimmed = String(line || '').trim()

    if (!trimmed) {
      i += 1
      continue
    }

    if (/^```/.test(trimmed)) {
      const codeLines = []
      i += 1
      while (i < lines.length && !/^```/.test(String(lines[i] || '').trim())) {
        codeLines.push(lines[i])
        i += 1
      }
      if (i < lines.length && /^```/.test(String(lines[i] || '').trim())) {
        i += 1
      }
      blocks.push(`<pre><code>${escapeHtmlValue(codeLines.join('\n'))}</code></pre>`)
      continue
    }

    if (/^([-*_])\1{2,}$/.test(trimmed)) {
      blocks.push('<hr>')
      i += 1
      continue
    }

    if (/^#{1,6}\s+/.test(trimmed)) {
      const level = trimmed.match(/^#+/)[0].length
      const content = trimmed.slice(level).trim()
      blocks.push(`<h${level}>${renderInlineMarkdown(content)}</h${level}>`)
      i += 1
      continue
    }

    if (looksLikeMarkdownTableRow(trimmed) && isMarkdownTableSeparator(lines[i + 1])) {
      const headers = parseMarkdownTableCells(trimmed)
      i += 2
      const rows = []
      while (i < lines.length) {
        const rowLine = String(lines[i] || '').trim()
        if (!rowLine || !looksLikeMarkdownTableRow(rowLine)) break
        if (isMarkdownTableSeparator(rowLine)) {
          i += 1
          continue
        }
        rows.push(parseMarkdownTableCells(rowLine))
        i += 1
      }
      const thead = `<thead><tr>${headers.map(cell => `<th>${renderInlineMarkdown(cell)}</th>`).join('')}</tr></thead>`
      const tbody = rows.length
        ? `<tbody>${rows.map(row => `<tr>${headers.map((_, idx) => `<td>${renderInlineMarkdown(row[idx] || '')}</td>`).join('')}</tr>`).join('')}</tbody>`
        : ''
      blocks.push(`<div class="markdown-table-wrap"><table>${thead}${tbody}</table></div>`)
      continue
    }

    if (/^>\s+/.test(trimmed)) {
      const quoteLines = []
      while (i < lines.length && /^>\s+/.test(String(lines[i] || '').trim())) {
        quoteLines.push(String(lines[i] || '').trim().replace(/^>\s+/, ''))
        i += 1
      }
      blocks.push(`<blockquote>${quoteLines.map(item => renderInlineMarkdown(item)).join('<br>')}</blockquote>`)
      continue
    }

    if (/^\s*[-*+]\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
        items.push(renderInlineMarkdown(String(lines[i] || '').replace(/^\s*[-*+]\s+/, '')))
        i += 1
      }
      blocks.push(`<ul>${items.map(item => `<li>${item}</li>`).join('')}</ul>`)
      continue
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items = []
      while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
        items.push(renderInlineMarkdown(String(lines[i] || '').replace(/^\s*\d+\.\s+/, '')))
        i += 1
      }
      blocks.push(`<ol>${items.map(item => `<li>${item}</li>`).join('')}</ol>`)
      continue
    }

    const paragraphLines = []
    while (i < lines.length) {
      const current = String(lines[i] || '')
      const currentTrimmed = current.trim()
      if (!currentTrimmed) {
        i += 1
        break
      }
      if (paragraphLines.length > 0 && isSpecialMarkdownLine(current, lines[i + 1])) {
        break
      }
      paragraphLines.push(renderInlineMarkdown(currentTrimmed))
      i += 1
    }

    if (paragraphLines.length) {
      blocks.push(`<p>${paragraphLines.join('<br>')}</p>`)
    }
  }

  return blocks.join('')
}

export default {
  layout: 'project',

  data() {
    return {
      projectId: null,
      loading: false,
      starLoading: false,
      saveLoading: false,
      uploadLoading: false,
      aiModelsLoading: false,
      aiSummaryLoading: false,
      aiTaskLoading: false,
      aiModels: [],
      activeAiModel: null,
      selectedAiModelId: null,
      lastAiModelLabel: '',
      aiActiveTab: 'summary',
      aiProjectSummary: '',
      aiProjectTasks: '',
      treeFilterText: '',
      selectedFileIds: [],
      project: {
        id: null,
        name: '',
        description: '',
        category: '',
        status: '',
        visibility: 'public',
        tags: '',
        stars: 0,
        downloads: 0,
        views: 0,
        starred: false,
        authorId: null,
        authorName: '',
        authorAvatar: '',
        createdAt: '',
        updatedAt: '',
        members: [],
        tasks: [],
        files: [],
        contributors: [],
        relatedProjects: []
      },
      contributors: [],
      relatedProjects: [],
      fileTree: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      currentFile: {
        id: null,
        name: '',
        path: '',
        size: 0,
        extension: '',
        content: '',
        isMain: false,
        versions: []
      },
      showEditDialog: false,
      editForm: {
        name: '',
        description: '',
        category: '',
        status: '',
        visibility: 'public',
        tagsText: ''
      },
      editRules: {
        name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
        category: [{ required: true, message: '请选择项目分类', trigger: 'change' }],
        status: [{ required: true, message: '请选择项目状态', trigger: 'change' }]
      },
      uploadDialog: {
        visible: false,
        isVersion: false,
        version: '1.0.0',
        commitMessage: '',
        isMain: false,
        file: null,
        files: [],
        fileNames: []
      },
      categoryOptions: Object.keys(CATEGORY_MAP).map(key => ({ value: key, label: CATEGORY_MAP[key] })),
      statusOptions: Object.keys(STATUS_MAP).map(key => ({ value: key, label: STATUS_MAP[key] }))
    }
  },

  computed: {
    renderedReadme() {
      return this.renderMarkdownContent(this.project.readme, '暂无 README 文档')
    },
    renderedAiProjectSummary() {
      return this.renderMarkdownContent(this.aiProjectSummary)
    },
    renderedAiProjectTasks() {
      return this.renderMarkdownContent(this.aiProjectTasks)
    },
    tagList() {
      return parseTags(this.project.tags)
    },
    categoryLabel() {
      return this.mapCategory(this.project.category)
    },
    statusLabel() {
      return STATUS_MAP[this.project.status] || this.project.status || '未知状态'
    },
    statusTagType() {
      return STATUS_TAG_MAP[this.project.status] || 'info'
    },
    visibilityLabel() {
      return this.project.visibility === 'private' ? '私有' : '公开'
    },
    hasAiResult() {
      return !!(this.aiProjectSummary || this.aiProjectTasks)
    },
    selectedAiModel() {
      const targetId = this.selectedAiModelId
      if (targetId === null || targetId === undefined || targetId === '') return this.activeAiModel
      return this.aiModels.find(item => String(item.id) === String(targetId)) || this.activeAiModel
    },
    currentAiModelLabel() {
      const model = this.selectedAiModel || this.activeAiModel
      return model && model.modelName ? model.modelName : '默认激活模型'
    },
    currentAiProviderLabel() {
      const model = this.selectedAiModel || this.activeAiModel
      return model && model.providerCode ? model.providerCode : ''
    }
  },

  watch: {
    treeFilterText(val) {
      if (this.$refs.fileTreeRef) {
        this.$refs.fileTreeRef.filter(val)
      }
    },
    'currentFile.content': {
      immediate: false,
      handler() {
        if (!process.client) return
        this.$nextTick(() => {
          this.highlightCode()
        })
      }
    },
    // 监听路由变化，当点击相关项目时重新加载数据
    '$route': {
      handler() {
        const newProjectId = this.$route.query.projectId || this.$route.params.id
        if (newProjectId && newProjectId !== this.projectId) {
          this.projectId = newProjectId
          this.initPage()
        }
      },
      deep: true
    }
  },

  async mounted() {
    this.projectId = this.$route.query.projectId || this.$route.params.id
    if (!this.projectId) {
      this.$message.error('缺少项目ID')
      return
    }
    await this.initPage()
  },

  methods: {
    async initPage() {
      this.loading = true
      try {
        const tasks = [
          this.fetchProjectDetail(),
          this.fetchContributors(),
          this.fetchRelatedProjects(),
          this.fetchFiles(),
          this.loadAiModels()
        ]
        const token = getToken ? getToken() : ''
        if (token) {
          tasks.push(this.fetchProjectStarState())
        }
        await Promise.all(tasks)
      } finally {
        this.loading = false
      }
    },

    async loadAiModels() {
      this.aiModelsLoading = true
      try {
        const [enabledRes, activeRes] = await Promise.allSettled([
          listEnabledAiModels(),
          getActiveAiModel()
        ])

        let enabledList = []
        let activeModel = null

        if (enabledRes.status === 'fulfilled') {
          const enabledData = extractApiData(enabledRes.value)
          enabledList = Array.isArray(enabledData) ? enabledData.map(normalizeAiModel) : []
        }

        if (activeRes.status === 'fulfilled') {
          const activeData = extractApiData(activeRes.value)
          if (activeData && typeof activeData === 'object' && activeData.id !== undefined && activeData.id !== null) {
            activeModel = normalizeAiModel(activeData)
          }
        }

        if (activeModel && !enabledList.some(item => String(item.id) === String(activeModel.id))) {
          enabledList.unshift(activeModel)
        }

        this.aiModels = enabledList
        this.activeAiModel = activeModel

        let savedModelId = null
        if (process.client) {
          savedModelId = window.localStorage.getItem('project_detail_ai_model_id')
        }

        const preferredModelId = savedModelId
          ? String(savedModelId)
          : (activeModel && activeModel.id) || (enabledList[0] && enabledList[0].id) || null
        this.selectedAiModelId = preferredModelId === '' ? null : preferredModelId
      } catch (error) {
        console.error(error)
        this.aiModels = []
        this.activeAiModel = null
      } finally {
        this.aiModelsLoading = false
      }
    },

    handleAiModelChange(value) {
      const modelId = value === '' || value === undefined || value === null ? null : String(value)
      this.selectedAiModelId = modelId
      if (process.client) {
        if (modelId === null) {
          window.localStorage.removeItem('project_detail_ai_model_id')
        } else {
          window.localStorage.setItem('project_detail_ai_model_id', modelId)
        }
      }
    },

    formatAiModelOption(item) {
      const modelName = item.modelName || '未命名模型'
      const provider = item.providerCode ? `（${item.providerCode}）` : ''
      return `${modelName}${provider}`
    },

    getCurrentAiUserId() {
      const directCandidates = [
        this.$store && this.$store.state && this.$store.state.user && this.$store.state.user.id,
        this.$store && this.$store.state && this.$store.state.user && this.$store.state.user.userId,
        this.$store && this.$store.state && this.$store.state.login && this.$store.state.login.userInfo && this.$store.state.login.userInfo.id,
        this.$store && this.$store.state && this.$store.state.login && this.$store.state.login.userInfo && this.$store.state.login.userInfo.userId
      ].filter(value => value !== undefined && value !== null && String(value).trim() !== '')

      if (directCandidates.length) {
        const value = String(directCandidates[0]).trim()
        return /^\d+$/.test(value) ? Number(value) : value
      }

      if (process.client) {
        const storageKeys = [
          'userInfo',
          'user',
          'loginUser',
          'currentUser',
          'Admin-User',
          'auth_user',
          'authUser',
          'memberInfo'
        ]

        for (const storage of [window.localStorage, window.sessionStorage]) {
          for (const key of storageKeys) {
            try {
              const raw = storage.getItem(key)
              if (!raw) continue
              const parsed = JSON.parse(raw)
              const foundId = pickUserIdFromObject(parsed)
              if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
                return foundId
              }
            } catch (e) {}
          }
        }

        try {
          const nuxtState = window.__NUXT__
          const foundId = pickUserIdFromObject(nuxtState)
          if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
            return foundId
          }
        } catch (e) {}
      }

      const token = getToken ? getToken() : ''
      if (token) {
        const payload = decodeJwtPayload(token)
        const foundId = pickUserIdFromObject(payload)
        if (foundId !== null && foundId !== undefined && String(foundId).trim() !== '') {
          return foundId
        }
      }

      return null
    },

    hasAiLoginContext() {
      const userId = this.getCurrentAiUserId()
      if (userId !== null && userId !== undefined && String(userId).trim() !== '') {
        return true
      }
      const token = getToken ? getToken() : ''
      return !!token
    },

    buildAiProjectContent() {
      return buildProjectAiContent(this.project, this.contributors, this.currentFile)
    },

    async handleAiSummarizeProject() {
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.aiSummaryLoading = true
      try {
        const modelId = this.selectedAiModelId || undefined
        const result = await aiSummarizeProject({
          userId: userId || undefined,
          modelId,
          projectId: this.projectId,
          title: this.project.name,
          content: this.buildAiProjectContent(),
          project: this.project
        })
        if (!result) {
          this.$message.warning('AI 未返回项目总结')
          return
        }
        this.aiProjectSummary = typeof result === 'string' ? result : (result && result.text) || JSON.stringify(result, null, 2)
        this.aiActiveTab = 'summary'
        this.lastAiModelLabel = this.currentAiModelLabel
        this.$message.success('AI 项目总结生成成功')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || error.message || 'AI 项目总结生成失败')
      } finally {
        this.aiSummaryLoading = false
      }
    },

    async handleAiSplitProjectTasks() {
      const userId = this.getCurrentAiUserId()
      if (!this.hasAiLoginContext()) {
        this.$message.warning('请先登录后再使用 AI 功能')
        return
      }

      this.aiTaskLoading = true
      try {
        const modelId = this.selectedAiModelId || undefined
        const result = await aiSplitProjectTasks({
          userId: userId || undefined,
          modelId,
          projectId: this.projectId,
          title: this.project.name,
          content: this.buildAiProjectContent(),
          project: this.project
        })
        if (!result) {
          this.$message.warning('AI 未返回任务拆解结果')
          return
        }
        this.aiProjectTasks = typeof result === 'string' ? result : (result && result.text) || JSON.stringify(result, null, 2)
        this.aiActiveTab = 'tasks'
        this.lastAiModelLabel = this.currentAiModelLabel
        this.$message.success('AI 任务拆解生成成功')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || error.message || 'AI 任务拆解生成失败')
      } finally {
        this.aiTaskLoading = false
      }
    },

    clearAiResult() {
      this.aiProjectSummary = ''
      this.aiProjectTasks = ''
      this.aiActiveTab = 'summary'
      this.lastAiModelLabel = ''
    },

    async fetchProjectStarState() {
      const token = getToken ? getToken() : ''
      if (!token) {
        this.project.starred = false
        return
      }
      try {
        const res = await getProjectStarStatus(this.projectId)
        const data = res?.data || res || {}
        this.project.starred = !!data.starred
        if (data.stars !== undefined && data.stars !== null && !Number.isNaN(Number(data.stars))) {
          this.project.stars = Number(data.stars)
        }
      } catch (error) {
        console.error(error)
      }
    },

    async fetchProjectDetail() {
      try {
        const res = await getProjectDetail(this.projectId)
        const data = res.data || {}
        this.project = {
          ...this.project,
          ...data,
          name: data.name || '',
          description: data.description || '',
          category: data.category || '',
          status: data.status || 'draft',
          visibility: data.visibility || 'public',
          tags: data.tags || '',
          stars: data.stars || 0,
          downloads: data.downloads || 0,
          views: data.views || 0,
          starred: !!data.starred,
          authorId: data.authorId || null,
          authorName: data.authorName || '',
          authorAvatar: data.authorAvatar || '',
          createdAt: data.createdAt || '',
          updatedAt: data.updatedAt || '',
          readme: '',
          members: data.members || [],
          tasks: data.tasks || [],
          files: data.files || [],
          contributors: data.contributors || [],
          relatedProjects: data.relatedProjects || []
        }
        this.editForm = {
          name: this.project.name,
          description: this.project.description,
          category: this.project.category,
          status: this.project.status,
          visibility: this.project.visibility || 'public',
          tagsText: parseTags(this.project.tags).join(', ')
        }
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '获取项目详情失败')
      }
    },

    async fetchContributors() {
      try {
        const res = await getProjectContributors(this.projectId)
        const list = Array.isArray(res.data) ? res.data : []
        this.contributors = list.map(item => ({
          ...item,
          displayName: item.nickname || item.username || `用户${item.userId || ''}`
        }))
      } catch (error) {
        console.error(error)
        this.contributors = []
      }
    },

    async fetchRelatedProjects() {
      try {
        const res = await getRelatedProjects(this.projectId, { size: 6 })
        this.relatedProjects = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        console.error(error)
        this.relatedProjects = []
      }
    },

    async fetchFiles() {
      const previousCurrentFileId = this.currentFile.id
      const previousSelectedIds = Array.isArray(this.selectedFileIds) ? [...this.selectedFileIds] : []
      try {
        const res = await listProjectFiles(this.projectId)
        const files = Array.isArray(res.data) ? res.data : []
        this.project.files = files
        this.fileTree = this.buildFileTree(files)
        const availableIds = new Set((files || []).map(item => item.id))
        this.selectedFileIds = previousSelectedIds.filter(id => availableIds.has(id))
        await this.$nextTick()
        this.restoreCheckedTreeNodes()
        await this.loadReadme(files)
        if (previousCurrentFileId && availableIds.has(previousCurrentFileId)) {
          const flatList = this.flattenFileTree(this.fileTree)
          const selected = flatList.find(item => item.id === previousCurrentFileId)
          if (selected) {
            await this.handleFileClick(selected)
          }
        }
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '获取项目文件失败')
      }
    },

    buildFileTree(files) {
      const root = []
      const nodeMap = {}
      const ensureFolder = (folderPath) => {
        if (!folderPath) return root
        if (nodeMap[folderPath]) return nodeMap[folderPath].children
        const segments = folderPath.split('/').filter(Boolean)
        let currentPath = ''
        let currentChildren = root
        segments.forEach((segment) => {
          currentPath = currentPath ? `${currentPath}/${segment}` : segment
          if (!nodeMap[currentPath]) {
            const folderNode = { name: segment, path: currentPath, type: 'folder', children: [] }
            nodeMap[currentPath] = folderNode
            currentChildren.push(folderNode)
          }
          currentChildren = nodeMap[currentPath].children
        })
        return currentChildren
      }

      ;(files || []).forEach((file) => {
        const fullPath = file.filePath || file.path || file.fileName || `file-${file.id}`
        const segments = fullPath.split('/').filter(Boolean)
        const fileName = file.fileName || segments[segments.length - 1] || `文件${file.id}`
        const folderPath = segments.slice(0, -1).join('/')
        const ext = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
        const children = ensureFolder(folderPath)
        children.push({
          id: file.id,
          name: fileName,
          path: fullPath,
          type: 'file',
          size: file.fileSizeBytes || 0,
          extension: ext,
          isMain: !!file.isMain,
          raw: file,
          children: []
        })
      })
      return root
    },

    async loadReadme(files) {
      const readmeFile = (files || []).find(file => {
        const name = (file.fileName || file.path || '').toLowerCase()
        return name === 'readme.md' || name.endsWith('/readme.md') || name === 'readme.txt' || name.endsWith('/readme.txt')
      })
      if (!readmeFile) {
        this.project.readme = ''
        return
      }
      try {
        const blob = await downloadFile(readmeFile.id)
        this.project.readme = await blob.text()
      } catch (error) {
        console.error(error)
      }
    },

    async handleFileClick(node) {
      if (!node || node.type !== 'file') return
      try {
        const [blob, versionRes] = await Promise.all([
          downloadFile(node.id),
          listFileVersions(node.id).catch(() => ({ data: [] }))
        ])
        const isText = TEXT_EXTENSIONS.has((node.extension || '').toLowerCase())
        const content = isText ? await blob.text() : '该文件为二进制文件，暂不支持在线预览，请直接下载。'
        this.currentFile = {
          id: node.id,
          name: node.name,
          path: node.path,
          size: node.size,
          extension: node.extension,
          isMain: !!node.isMain,
          content,
          versions: Array.isArray(versionRes.data) ? versionRes.data : []
        }
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '读取文件失败')
      }
    },

    async toggleStar() {
      this.starLoading = true
      try {
        const res = this.project.starred
          ? await unstarProject(this.projectId)
          : await starProject(this.projectId)
        const data = res.data || {}
        this.project.starred = !!data.starred
        this.project.stars = data.stars != null ? data.stars : this.project.stars
        this.$message.success(this.project.starred ? '收藏成功' : '已取消收藏')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '操作失败')
      } finally {
        this.starLoading = false
      }
    },

    async submitEdit() {
      this.$refs.editFormRef.validate(async (valid) => {
        if (!valid) return
        this.saveLoading = true
        try {
          const payload = {
            name: this.editForm.name,
            description: this.editForm.description,
            category: this.editForm.category,
            status: this.editForm.status,
            visibility: this.editForm.visibility,
            tags: JSON.stringify(parseTags(this.editForm.tagsText)),
            updatedAt: toBackendDateTime(new Date())
          }
          await updateProject(this.projectId, payload)
          this.$message.success('项目更新成功')
          this.showEditDialog = false
          await this.fetchProjectDetail()
          await this.fetchRelatedProjects()
        } catch (error) {
          console.error(error)
          this.$message.error(error.response?.data?.message || '更新项目失败')
        } finally {
          this.saveLoading = false
        }
      })
    },

    openUploadDialog(isVersion) {
      if (isVersion && !this.currentFile.id) {
        this.$message.warning('请先选择一个文件')
        return
      }
      this.uploadDialog = {
        visible: true,
        isVersion,
        version: isVersion ? '1.0.1' : '1.0.0',
        commitMessage: '',
        isMain: false,
        file: null,
        files: [],
        fileNames: []
      }
      this.$nextTick(() => {
        if (this.$refs.uploadInput) this.$refs.uploadInput.value = ''
      })
    },

    closeUploadDialog() {
      this.uploadDialog.visible = false
      this.uploadDialog.file = null
      this.uploadDialog.files = []
      this.uploadDialog.fileNames = []
      if (this.$refs.uploadInput) this.$refs.uploadInput.value = ''
    },

    handlePickedFiles(event) {
      const files = Array.from((event.target && event.target.files) || []).filter(Boolean)
      this.uploadDialog.files = files
      this.uploadDialog.file = files[0] || null
      this.uploadDialog.fileNames = files.map(item => item.name)
    },

    restoreCheckedTreeNodes() {
      this.$nextTick(() => {
        if (!this.$refs.fileTreeRef) return
        const flatList = this.flattenFileTree(this.fileTree)
        const checkedKeys = flatList
          .filter(item => this.selectedFileIds.includes(item.id))
          .map(item => item.path)
        this.$refs.fileTreeRef.setCheckedKeys(checkedKeys)
      })
    },

    syncCheckedFileIds() {
      if (!this.$refs.fileTreeRef) {
        this.selectedFileIds = []
        return
      }
      const checkedNodes = this.$refs.fileTreeRef.getCheckedNodes(false, true) || []
      this.selectedFileIds = checkedNodes
        .filter(item => item && item.type === 'file' && item.id)
        .map(item => item.id)
    },

    handleTreeCheckChange(data, checked) {
      if (data && data.type === 'folder' && checked && this.$refs.fileTreeRef) {
        this.$refs.fileTreeRef.setChecked(data.path, false, false)
      }
      this.syncCheckedFileIds()
    },

    checkAllFiles() {
      const flatList = this.flattenFileTree(this.fileTree)
      if (!flatList.length) {
        this.$message.warning('暂无可勾选文件')
        return
      }
      this.selectedFileIds = flatList.map(item => item.id)
      this.restoreCheckedTreeNodes()
    },

    clearCheckedFiles() {
      this.selectedFileIds = []
      if (this.$refs.fileTreeRef) {
        this.$refs.fileTreeRef.setCheckedKeys([])
      }
    },

    async submitUpload() {
      const files = Array.isArray(this.uploadDialog.files) ? this.uploadDialog.files : []
      if (!files.length) {
        this.$message.warning('请选择文件')
        return
      }
      this.uploadLoading = true
      try {
        if (this.uploadDialog.isVersion) {
          const formData = new FormData()
          formData.append('file', this.uploadDialog.file)
          formData.append('version', this.uploadDialog.version || '1.0.0')
          formData.append('commitMessage', this.uploadDialog.commitMessage || '前端上传新版本')
          await uploadFileNewVersion(this.currentFile.id, formData)
        } else if (files.length === 1) {
          const formData = new FormData()
          formData.append('projectId', this.projectId)
          formData.append('file', files[0])
          formData.append('isMain', this.uploadDialog.isMain ? 'true' : 'false')
          formData.append('version', this.uploadDialog.version || '1.0.0')
          formData.append('commitMessage', this.uploadDialog.commitMessage || '前端上传文件')
          await uploadProjectFile(this.projectId, formData)
        } else {
          const formData = new FormData()
          formData.append('projectId', this.projectId)
          formData.append('version', this.uploadDialog.version || '1.0.0')
          formData.append('commitMessage', this.uploadDialog.commitMessage || '前端批量上传文件')
          if (this.uploadDialog.isMain) {
            formData.append('mainFileIndex', '0')
          }
          files.forEach(file => {
            formData.append('files', file)
          })
          await uploadProjectFiles(this.projectId, formData)
        }
        this.$message.success(this.uploadDialog.isVersion ? '新版本上传成功' : '文件上传成功')
        this.closeUploadDialog()
        await this.fetchFiles()
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '上传失败')
      } finally {
        this.uploadLoading = false
      }
    },

    async markMainFile() {
      if (!this.currentFile.id) return
      try {
        await setMainFile(this.currentFile.id)
        this.$message.success('已设置为主文件')
        await this.fetchFiles()
        const flatList = this.flattenFileTree(this.fileTree)
        const selected = flatList.find(item => item.id === this.currentFile.id)
        if (selected) await this.handleFileClick(selected)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '设置主文件失败')
      }
    },

    async removeCurrentFile() {
      if (!this.currentFile.id) return
      try {
        await this.$confirm(`确认删除文件“${this.currentFile.name}”吗？`, '提示', {
          type: 'warning'
        })
        await deleteFile(this.currentFile.id)
        this.selectedFileIds = this.selectedFileIds.filter(id => id !== this.currentFile.id)
        this.$message.success('文件删除成功')
        this.currentFile = {
          id: null,
          name: '',
          path: '',
          size: 0,
          extension: '',
          content: '',
          isMain: false,
          versions: []
        }
        await this.fetchFiles()
      } catch (error) {
        if (error !== 'cancel') {
          console.error(error)
          this.$message.error(error.response?.data?.message || '删除文件失败')
        }
      }
    },

    async handleBatchDownload() {
      if (!this.selectedFileIds.length) {
        this.$message.warning('请先勾选要下载的文件')
        return
      }
      try {
        const blob = await downloadProjectFiles(this.projectId, this.selectedFileIds)
        this.triggerBlobDownload(blob, `project-${this.projectId}-files.zip`)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '批量下载失败')
      }
    },

    async downloadCurrentFile() {
      if (!this.currentFile.id) {
        this.$message.warning('请先选择文件')
        return
      }
      try {
        const blob = await downloadFile(this.currentFile.id)
        this.triggerBlobDownload(blob, this.currentFile.name)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '下载失败')
      }
    },

    async downloadMainFile() {
      const flatList = this.flattenFileTree(this.fileTree)
      const mainFile = flatList.find(item => item.isMain) || flatList[0]
      if (!mainFile) {
        this.$message.warning('暂无可下载文件')
        return
      }
      try {
        const blob = await downloadFile(mainFile.id)
        this.triggerBlobDownload(blob, mainFile.name)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '下载失败')
      }
    },

    triggerBlobDownload(blob, filename) {
      if (!process.client || typeof document === 'undefined') return
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename || 'download'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    },

    highlightCode() {
      if (!process.client || typeof document === 'undefined' || typeof hljs === 'undefined') return
      const codeBlocks = document.querySelectorAll('.code-content code')
      codeBlocks.forEach(block => {
        hljs.highlightElement(block)
      })
    },

    escapeHtml(text) {
      return escapeHtmlValue(text)
    },

    renderMarkdownContent(content, emptyText = '暂无内容') {
      return renderMarkdownToHtml(content, emptyText)
    },

    flattenFileTree(nodes = []) {
      const list = []
      nodes.forEach(node => {
        if (node.type === 'file') {
          list.push(node)
        }
        if (node.children && node.children.length) {
          list.push(...this.flattenFileTree(node.children))
        }
      })
      return list
    },

    filterNode(value, data) {
      if (!value) return true
      return (data.name || '').toLowerCase().includes(value.toLowerCase())
    },

    getTreeIcon(data) {
      if (data.type === 'folder') return 'el-icon-folder'
      return 'el-icon-document'
    },

    roleLabel(role) {
      return ROLE_MAP[role] || role || '成员'
    },

    mapCategory(category) {
      return CATEGORY_MAP[category] || category || '未分类'
    },

    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return date.toLocaleString('zh-CN')
    },

    formatFileSize(size) {
      const bytes = Number(size || 0)
      if (!bytes) return '0 B'
      if (bytes < 1024) return `${bytes} B`
      if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
      return `${(bytes / 1024 / 1024).toFixed(1)} MB`
    },

    goToProjectManage() {
      this.$router.push(`/projectmanage?projectId=${this.projectId}`)
    },

    goToDetail(id) {
      this.$router.push(`/projectdetail?projectId=${id}`)
    }
  }
}
</script>

<style scoped>
.project-detail-container {
  max-width: 1320px;
  margin: 0 auto;
  padding: 20px;
  background: #f7f8fa;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.project-overview-card,
.section-card {
  border-radius: 12px;
}

.project-overview {
  display: flex;
  gap: 20px;
}

.overview-main {
  width: 100%;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.project-title {
  margin: 0;
  font-size: 28px;
  color: #1f2d3d;
}

.title-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.project-desc {
  margin-top: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}

.tag-list {
  margin-top: 14px;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
}

.meta-row {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.author-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  font-weight: 600;
  color: #303133;
}

.author-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stats-row {
  display: flex;
  gap: 16px;
}

.stat-item {
  min-width: 72px;
  text-align: center;
  background: #f7f8fa;
  border-radius: 10px;
  padding: 10px 12px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.content-layout {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.content-main,
.content-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-header {
  font-weight: 600;
  color: #303133;
}

.section-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.readme-box {
  min-height: 140px;
  line-height: 1.9;
  color: #606266;
  white-space: normal;
  word-break: break-word;
}

.file-section-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.file-browser {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
}

.file-tree-panel,
.file-preview-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.file-tree-panel {
  padding: 12px;
}

.tree-selection-bar {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.tree-selection-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-wrap {
  margin-top: 12px;
  max-height: 540px;
  overflow: auto;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.tree-node-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-file-badge {
  margin-left: auto;
  font-size: 12px;
  color: #409eff;
}

.file-preview-panel {
  display: flex;
  flex-direction: column;
  min-height: 540px;
}

.file-preview-toolbar {
  padding: 12px 14px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.file-preview-title {
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.file-preview-actions {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  width: auto;
  min-width: 300px;
}

.file-preview-meta {
  padding: 10px 14px 0;
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}

.code-container {
  display: flex;
  flex: 1;
  border-radius: 0 0 10px 10px;
  overflow: hidden;
}

.line-numbers {
  width: 40px;
  background: #f0f2f5;
  border-right: 1px solid #ebeef5;
  text-align: right;
  user-select: none;
  padding: 0;
  overflow: hidden;
}

.line-number {
  height: 1.5em;
  padding: 0 8px;
  font-size: 11px;
  color: #909399;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.5;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.code-content {
  margin: 0;
  padding: 0 14px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
  flex: 1;
  background: #fafafa;
  font-size: 11px;
  line-height: 1.5;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.code-content pre {
  margin: 0;
  padding: 0;
}

.code-content code {
  font-size: 11px;
  line-height: 1.5;
}

.empty-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.upload-dialog-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.upload-file-list {
  margin-top: 10px;
  max-height: 120px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  padding: 8px 10px;
}

.upload-file-item {
  font-size: 12px;
  color: #606266;
  line-height: 1.8;
  word-break: break-all;
}

.version-box {
  margin-top: 16px;
}

.sub-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.version-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.version-name {
  font-weight: 600;
  color: #303133;
}

.version-desc {
  color: #606266;
  font-size: 13px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.info-label {
  color: #909399;
}

.info-value {
  color: #303133;
  text-align: right;
  word-break: break-word;
}

.contributors-list,
.related-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contributor-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.contributor-name {
  font-weight: 600;
  color: #303133;
}

.contributor-role {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}

.related-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all .2s;
}

.related-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.12);
}

.related-title {
  font-weight: 600;
  color: #303133;
}

.related-desc {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.related-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.ai-assistant-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-model-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-model-label {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.ai-model-tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-helper-text {
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.ai-helper-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-result-card {
  overflow: hidden;
}

.ai-result-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.ai-result-tabs {
  margin-top: -8px;
}

.ai-result-box {
  min-height: 180px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  line-height: 1.9;
  color: #303133;
  white-space: normal;
  word-break: break-word;
}

.ai-rich-content p,
.ai-rich-content ul,
.ai-rich-content ol,
.ai-rich-content blockquote,
.ai-rich-content pre,
.ai-rich-content .markdown-table-wrap {
  margin: 0 0 14px;
}

.ai-rich-content p:last-child,
.ai-rich-content ul:last-child,
.ai-rich-content ol:last-child,
.ai-rich-content blockquote:last-child,
.ai-rich-content pre:last-child,
.ai-rich-content .markdown-table-wrap:last-child {
  margin-bottom: 0;
}

.ai-rich-content h1,
.ai-rich-content h2,
.ai-rich-content h3,
.ai-rich-content h4,
.ai-rich-content h5,
.ai-rich-content h6 {
  margin: 18px 0 10px;
  line-height: 1.5;
  color: #1f2d3d;
}

.ai-rich-content h1:first-child,
.ai-rich-content h2:first-child,
.ai-rich-content h3:first-child,
.ai-rich-content h4:first-child,
.ai-rich-content h5:first-child,
.ai-rich-content h6:first-child {
  margin-top: 0;
}

.ai-rich-content h1 { font-size: 24px; }
.ai-rich-content h2 { font-size: 22px; }
.ai-rich-content h3 { font-size: 20px; }
.ai-rich-content h4 { font-size: 18px; }
.ai-rich-content h5 { font-size: 16px; }
.ai-rich-content h6 { font-size: 15px; }

.ai-rich-content ul,
.ai-rich-content ol {
  padding-left: 22px;
}

.ai-rich-content li + li {
  margin-top: 6px;
}

.ai-rich-content hr {
  border: none;
  border-top: 1px solid #ebeef5;
  margin: 18px 0;
}

.ai-rich-content blockquote {
  padding: 10px 14px;
  border-left: 4px solid #dcdfe6;
  background: #f7f8fa;
  color: #606266;
  border-radius: 6px;
}

.ai-rich-content code {
  padding: 2px 6px;
  border-radius: 4px;
  background: #f2f4f5;
  font-size: 13px;
  color: #c45656;
}

.ai-rich-content pre {
  padding: 12px 14px;
  border-radius: 8px;
  background: #1f2937;
  color: #f9fafb;
  overflow: auto;
}

.ai-rich-content pre code {
  padding: 0;
  background: transparent;
  color: inherit;
}

.ai-rich-content .markdown-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.ai-rich-content table {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
}

.ai-rich-content th,
.ai-rich-content td {
  border: 1px solid #ebeef5;
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
}

.ai-rich-content th {
  background: #f5f7fa;
  font-weight: 600;
}

.ai-rich-content strong {
  font-weight: 700;
  color: #1f2d3d;
}

.ai-rich-content em {
  font-style: italic;
}

@media (max-width: 1100px) {
  .content-layout {
    grid-template-columns: 1fr;
  }

  .file-browser {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .detail-header,
  .title-row,
  .meta-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .file-preview-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .file-preview-actions {
    justify-content: flex-end;
  }

  .stats-row {
    width: 100%;
    justify-content: space-between;
  }

  .ai-helper-actions {
    width: 100%;
  }

  .ai-result-header-actions {
    justify-content: space-between;
    width: 100%;
  }
}
</style>