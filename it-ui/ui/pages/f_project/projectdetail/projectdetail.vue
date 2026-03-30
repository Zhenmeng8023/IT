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
        <el-button v-if="pageAccessResolved && canManageProject" size="small" icon="el-icon-user-solid" @click="handleMemberManageClick">
          成员管理
        </el-button>
        <el-button v-if="pageAccessResolved && canSeeTaskCollaboration" size="small" icon="el-icon-s-operation" @click="handleTaskManageClick">
          任务协作
        </el-button>
        <el-button v-if="pageAccessResolved && canManageProject" size="small" icon="el-icon-s-tools" @click="handleProjectManageClick">
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
          v-if="pageAccessResolved && canManageProject"
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

    <el-card v-if="pageAccessResolved && canSeeTaskCollaboration" shadow="never" class="section-card task-board-card">
      <div slot="header" class="section-header section-header-flex">
        <span>任务看板</span>
        <div class="task-board-header-actions">
          <el-button size="mini" type="text" :loading="taskBoardLoading" @click="fetchProjectTasks">刷新</el-button>
          <el-button size="mini" type="primary" plain icon="el-icon-s-operation" @click="handleTaskManageClick">
            进入任务协作
          </el-button>
        </div>
      </div>
      <div class="task-board-summary">
        <div class="task-mini-stat">
          <div class="task-mini-stat-value">{{ taskSummary.total }}</div>
          <div class="task-mini-stat-label">总任务</div>
        </div>
        <div class="task-mini-stat">
          <div class="task-mini-stat-value">{{ taskSummary.todo }}</div>
          <div class="task-mini-stat-label">待处理</div>
        </div>
        <div class="task-mini-stat">
          <div class="task-mini-stat-value">{{ taskSummary.inProgress }}</div>
          <div class="task-mini-stat-label">进行中</div>
        </div>
        <div class="task-mini-stat">
          <div class="task-mini-stat-value">{{ taskSummary.done }}</div>
          <div class="task-mini-stat-label">已完成</div>
        </div>
        <div class="task-mini-stat danger">
          <div class="task-mini-stat-value">{{ taskSummary.overdue }}</div>
          <div class="task-mini-stat-label">已逾期</div>
        </div>
      </div>
      <div v-loading="taskBoardLoading" class="task-board-grid">
        <div class="task-compact-panel">
          <div class="task-compact-panel-head">
            <span>最近任务</span>
            <el-tag size="mini" effect="plain">{{ recentTasks.length }}</el-tag>
          </div>
          <div v-if="recentTasks.length" class="task-compact-list">
            <div
              v-for="task in recentTasks"
              :key="'recent-' + task.id"
              class="task-compact-item"
              :class="{ 'is-overdue': isTaskOverdue(task) }"
            >
              <div class="task-compact-main">
                <div class="task-compact-top">
                  <div class="task-compact-title">{{ task.title || '未命名任务' }}</div>
                  <div class="task-compact-tags">
                    <el-tag size="mini" effect="plain" :type="getTaskPriorityType(task.priority)">{{ getTaskPriorityText(task.priority) }}</el-tag>
                    <el-tag size="mini" :type="getTaskStatusType(task.status)">{{ getTaskStatusText(task.status) }}</el-tag>
                  </div>
                </div>
                <div class="task-compact-meta">
                  <div class="task-assignee-inline">
                    <el-avatar :size="26" :src="task.assigneeAvatar || ''">{{ getTaskAssigneeName(task).slice(0, 1) }}</el-avatar>
                    <span>{{ getTaskAssigneeName(task) }}</span>
                  </div>
                  <span>{{ getTaskTimeLabel(task) }}</span>
                </div>
              </div>
              <div class="task-compact-side">
                <el-select
                  :value="task.status"
                  size="mini"
                  placeholder="状态"
                  :disabled="taskQuickUpdatingId === task.id"
                  @change="handleQuickTaskStatusChange(task, $event)"
                >
                  <el-option label="待处理" value="todo" />
                  <el-option label="进行中" value="in_progress" />
                  <el-option label="已完成" value="done" />
                </el-select>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无最近任务" :image-size="60" />
        </div>

        <div class="task-compact-panel">
          <div class="task-compact-panel-head">
            <span>逾期任务</span>
            <el-tag size="mini" type="danger" effect="plain">{{ overdueTasks.length }}</el-tag>
          </div>
          <div v-if="overdueTasks.length" class="task-compact-list">
            <div
              v-for="task in overdueTasks"
              :key="'overdue-' + task.id"
              class="task-compact-item is-overdue"
            >
              <div class="task-compact-main">
                <div class="task-compact-top">
                  <div class="task-compact-title">{{ task.title || '未命名任务' }}</div>
                  <div class="task-compact-tags">
                    <el-tag size="mini" effect="plain" :type="getTaskPriorityType(task.priority)">{{ getTaskPriorityText(task.priority) }}</el-tag>
                    <el-tag size="mini" type="danger">逾期</el-tag>
                  </div>
                </div>
                <div class="task-compact-meta">
                  <div class="task-assignee-inline">
                    <el-avatar :size="26" :src="task.assigneeAvatar || ''">{{ getTaskAssigneeName(task).slice(0, 1) }}</el-avatar>
                    <span>{{ getTaskAssigneeName(task) }}</span>
                  </div>
                  <span>{{ getTaskDueLabel(task) }}</span>
                </div>
              </div>
              <div class="task-compact-side">
                <el-button
                  size="mini"
                  type="success"
                  plain
                  :loading="taskQuickUpdatingId === task.id"
                  @click="handleQuickTaskStatusChange(task, 'done')"
                >
                  标记完成
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无逾期任务" :image-size="60" />
        </div>
      </div>
    </el-card>

    <div class="content-layout">
      <div class="content-main">
        <el-card shadow="never" class="section-card readme-section-card">
          <div slot="header" class="section-header section-header-flex readme-section-header">
            <div class="readme-section-title">
              <div class="readme-title-main">
                <span>README</span>
                <el-tag size="mini" effect="plain" type="success">Markdown</el-tag>
              </div>
              <div class="readme-title-sub">{{ readmeLeadText }}</div>
            </div>
            <div class="readme-header-right">
              <div class="readme-stats-inline">
                <span class="readme-stat-chip"><i class="el-icon-document"></i><em>{{ readmeReadableUnits }}</em> 内容量</span>
                <span class="readme-stat-chip"><i class="el-icon-menu"></i><em>{{ readmeHeadingCount }}</em> 标题</span>
                <span class="readme-stat-chip"><i class="el-icon-tickets"></i><em>{{ readmeCodeBlockCount }}</em> 代码块</span>
                <span class="readme-stat-chip"><i class="el-icon-time"></i><em>{{ readmeReadTimeText }}</em></span>
              </div>
              <el-button v-if="canManageProject" size="mini" type="text" icon="el-icon-edit" @click="showEditProjectDialog">编辑项目</el-button>
            </div>
          </div>
          <div class="readme-showcase" :class="{ 'is-empty': !readmeHasContent }">
            <div v-if="readmeHasContent" class="readme-hero">
              <div class="readme-hero-main">
                <div class="readme-eyebrow">项目说明文档</div>
                <div class="readme-hero-title">{{ project.name || '未命名项目' }} README</div>
                <div class="readme-hero-desc">{{ readmeLeadText }}</div>
              </div>
              <div class="readme-hero-stats">
                <div class="readme-hero-stat">
                  <div class="readme-hero-stat-value">{{ readmeReadableUnits }}</div>
                  <div class="readme-hero-stat-label">内容量</div>
                </div>
                <div class="readme-hero-stat">
                  <div class="readme-hero-stat-value">{{ readmeHeadingCount }}</div>
                  <div class="readme-hero-stat-label">标题</div>
                </div>
                <div class="readme-hero-stat">
                  <div class="readme-hero-stat-value">{{ readmeCodeBlockCount }}</div>
                  <div class="readme-hero-stat-label">代码块</div>
                </div>
                <div class="readme-hero-stat">
                  <div class="readme-hero-stat-value">{{ readmeReadTimeText }}</div>
                  <div class="readme-hero-stat-label">预计阅读</div>
                </div>
              </div>
            </div>
            <div class="readme-shell">
              <div class="readme-box ai-rich-content" v-html="renderedReadme"></div>
            </div>
          </div>
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
              <div v-if="aiSummaryCard.overview" class="ai-struct-panel">
                <div class="ai-hero-overview">{{ aiSummaryCard.overview }}</div>
                <div class="ai-struct-grid">
                  <div class="ai-struct-item">
                    <div class="ai-struct-title">目标用户 / 场景</div>
                    <ul class="ai-struct-list">
                      <li v-for="(item, index) in aiSummaryCard.scenarios" :key="'scene-' + index">{{ item }}</li>
                    </ul>
                  </div>
                  <div class="ai-struct-item">
                    <div class="ai-struct-title">核心功能</div>
                    <ul class="ai-struct-list">
                      <li v-for="(item, index) in aiSummaryCard.features" :key="'feature-' + index">{{ item }}</li>
                    </ul>
                  </div>
                  <div class="ai-struct-item">
                    <div class="ai-struct-title">风险与待补项</div>
                    <ul class="ai-struct-list ai-struct-list-warn">
                      <li v-for="(item, index) in aiSummaryCard.risks" :key="'risk-' + index">{{ item }}</li>
                    </ul>
                  </div>
                  <div class="ai-struct-item">
                    <div class="ai-struct-title">下一步建议</div>
                    <ul class="ai-struct-list">
                      <li v-for="(item, index) in aiSummaryCard.nextActions" :key="'next-' + index">{{ item }}</li>
                    </ul>
                  </div>
                </div>
              </div>
              <el-empty v-else description="还没有生成项目总结" :image-size="70" />
            </el-tab-pane>
            <el-tab-pane label="任务拆解" name="tasks">
              <div v-if="aiTaskCard.phases.length" class="ai-struct-panel">
                <div
                  v-for="(phase, phaseIndex) in aiTaskCard.phases"
                  :key="'phase-' + phaseIndex"
                  class="ai-phase-card"
                >
                  <div class="ai-phase-title">{{ phase.name }}</div>
                  <div class="ai-task-list">
                    <div
                      v-for="(task, taskIndex) in phase.tasks"
                      :key="'task-' + phaseIndex + '-' + taskIndex"
                      class="ai-task-item"
                    >
                      <div class="ai-task-top">
                        <span class="ai-task-name">{{ task.title }}</span>
                        <el-tag size="mini" effect="plain">{{ task.priority || 'P2' }}</el-tag>
                      </div>
                      <div class="ai-task-meta">目标：{{ task.goal || '—' }}</div>
                      <div class="ai-task-meta">产出物：{{ task.deliverable || '—' }}</div>
                      <div class="ai-task-meta">预计耗时：{{ task.estimate || '—' }}</div>
                    </div>
                  </div>
                </div>

                <div v-if="aiTaskCard.executionOrder.length" class="ai-struct-item">
                  <div class="ai-struct-title">建议执行顺序</div>
                  <ol class="ai-struct-list ai-ordered-list">
                    <li v-for="(item, index) in aiTaskCard.executionOrder" :key="'order-' + index">{{ item }}</li>
                  </ol>
                </div>

                <div v-if="aiTaskCard.risks.length" class="ai-struct-item">
                  <div class="ai-struct-title">依赖 / 阻塞点</div>
                  <ul class="ai-struct-list ai-struct-list-warn">
                    <li v-for="(item, index) in aiTaskCard.risks" :key="'task-risk-' + index">{{ item }}</li>
                  </ul>
                </div>
              </div>
              <el-empty v-else description="还没有生成任务拆解" :image-size="70" />
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <el-card shadow="never" class="section-card">
          <div slot="header" class="section-header section-header-flex">
            <span>项目文件</span>
            <div class="file-header-actions">
              <el-button
                size="mini"
                icon="el-icon-download"
                :disabled="!selectedFileIds.length"
                @click="handleBatchDownload"
              >
                批量下载{{ selectedFileIds.length ? '（' + selectedFileIds.length + '）' : '' }}
              </el-button>
              <el-button v-if="canManageProject" size="mini" icon="el-icon-upload2" :loading="uploadLoading" @click="openUploadDialog(false)">
                上传文件
              </el-button>
              <el-button
                v-if="canManageProject"
                size="mini"
                icon="el-icon-top"
                :disabled="!currentFile.id"
                @click="openUploadDialog(true)"
              >
                上传新版本
              </el-button>
            </div>
          </div>
          <div ref="fileBrowserRef" class="file-browser">
            <div class="file-tree-panel" :style="treePanelStyle">
              <el-input
                v-model="treeFilterText"
                size="small"
                clearable
                prefix-icon="el-icon-search"
                placeholder="搜索文件"
              />
              <div class="tree-selection-bar">
                <span>已选 {{ selectedFileIds.length }} / {{ totalFileCount }} 个</span>
                <div class="tree-selection-actions">
                  <el-button size="mini" type="text" @click="toggleSelectAllFiles">
                    {{ isAllFilesSelected ? '取消全选' : '全选文件' }}
                  </el-button>
                  <el-button size="mini" type="text" :disabled="!selectedFileIds.length" @click="clearSelectedFiles">
                    清空
                  </el-button>
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
                  :filter-node-method="filterNode"
                  @node-click="handleFileClick"
                >
                  <span slot-scope="{ data }" class="tree-node">
                    <el-checkbox
                      v-if="data.type === 'file'"
                      class="tree-node-checkbox"
                      :value="isFileChecked(data.id)"
                      @click.stop.native
                      @change="toggleFileSelection(data, $event)"
                    />
                    <i :class="getTreeIcon(data)"></i>
                    <span class="tree-node-name" :title="data.path || data.name">{{ data.name }}</span>
                    <span v-if="data.type === 'file' && data.isMain" class="main-file-badge">主文件</span>
                  </span>
                </el-tree>
              </div>
            </div>
            <div
              class="file-browser-splitter"
              title="拖动调整目录树宽度"
              @mousedown="startTreeResize"
            ></div>
            <div
              ref="filePreviewPanelRef"
              class="file-preview-panel"
              :class="{ 'is-fullscreen': previewFullscreen }"
              tabindex="0"
            >
              <div class="file-preview-toolbar">
                <div class="file-preview-title-wrap">
                  <div class="file-preview-title-row">
                    <span v-if="currentFile.id" class="preview-index-badge">{{ currentFileDisplayIndex }}/{{ totalFileCount }}</span>
                    <div class="file-preview-title-group">
                      <div class="file-preview-title" :title="currentFile.name || '请选择文件'">{{ currentFile.name || '请选择文件' }}</div>
                      <div v-if="currentFile.path" class="file-preview-subtitle" :title="currentFile.path">{{ currentFile.path }}</div>
                    </div>
                  </div>
                  <div v-if="currentFile.id" class="file-preview-toolbar-tip">
                    <i class="el-icon-position"></i>
                    <span>{{ previewFullscreen ? 'Esc 退出全屏，← / → 切换文件' : '支持上一个 / 下一个文件快速切换' }}</span>
                  </div>
                </div>
                <div class="file-preview-toolbar-actions">
                  <div class="file-preview-switchers">
                    <el-button-group>
                      <el-button size="mini" icon="el-icon-arrow-left" :disabled="!hasPrevPreviewFile" @click="goPrevPreviewFile">上一个</el-button>
                      <el-button size="mini" icon="el-icon-arrow-right" :disabled="!hasNextPreviewFile" @click="goNextPreviewFile">下一个</el-button>
                    </el-button-group>
                  </div>
                  <div class="file-preview-actions">
                    <el-button
                      size="mini"
                      plain
                      icon="el-icon-full-screen"
                      :disabled="!currentFile.id"
                      @click="togglePreviewFullscreen"
                    >
                      {{ previewFullscreenButtonText }}
                    </el-button>
                    <el-button size="mini" :disabled="!currentFile.id" @click="downloadCurrentFile">下载</el-button>
                    <el-button v-if="canManageProject" size="mini" :disabled="!currentFile.id" @click="markMainFile">设为主文件</el-button>
                    <el-button v-if="canManageProject" size="mini" type="danger" :disabled="!currentFile.id" @click="removeCurrentFile">删除</el-button>
                  </div>
                </div>
              </div>
              <div v-if="currentFile.id" class="file-preview-meta">
                <div class="preview-meta-left">
                  <span class="meta-pill meta-pill-lang">{{ currentFileLanguageLabel }}</span>
                  <span v-if="currentPreviewMetricLabel" class="meta-pill">{{ currentPreviewMetricLabel }}</span>
                  <span class="meta-pill">{{ formatFileSize(currentFile.size) || '-' }}</span>
                  <span class="meta-pill">{{ currentFile.versions.length }} 个版本</span>
                </div>
                <div class="preview-meta-right">
                  <div v-if="currentFile.id && canAdjustPreview" class="preview-view-tools">
                    <span class="preview-font-indicator">{{ previewFontSize }}px</span>
                    <el-button size="mini" plain @click="decreasePreviewFont">A-</el-button>
                    <el-button size="mini" plain @click="resetPreviewFont">重置</el-button>
                    <el-button size="mini" plain @click="increasePreviewFont">A+</el-button>
                    <el-button size="mini" :type="previewWrap ? 'primary' : 'default'" plain @click="togglePreviewWrap">
                      {{ previewWrapButtonText }}
                    </el-button>
                  </div>
                  <div class="preview-copy-action">
                    <el-button
                      size="mini"
                      plain
                      icon="el-icon-document-copy"
                      :disabled="!canCopyCurrentFileContent"
                      @click="copyCurrentFileContent"
                    >
                      复制内容
                    </el-button>
                  </div>
                </div>
              </div>
              <div v-if="currentFile.id && currentFile.previewError" class="preview-warning-banner">
                <i class="el-icon-warning-outline"></i>
                <span>{{ currentFile.previewError }}</span>
              </div>
              <div v-if="currentFile.id && currentFile.previewLoading" class="empty-preview preview-loading-state">
                <i class="el-icon-loading"></i>
                <span>正在解析文件预览...</span>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'code'" class="code-preview-shell" :style="previewFontStyle">
                <div class="code-preview-header">
                  <div class="code-header-left">
                    <span class="code-dot code-dot-red"></span>
                    <span class="code-dot code-dot-yellow"></span>
                    <span class="code-dot code-dot-green"></span>
                    <span class="code-file-name">{{ currentFile.name || '未命名文件' }}</span>
                  </div>
                  <div class="code-header-right">
                    <span class="code-extension-chip">{{ currentFile.extension ? '.' + currentFile.extension : '.txt' }}</span>
                  </div>
                </div>
                <div class="code-container">
                  <div ref="lineNumbersRef" class="line-numbers">
                    <div v-for="i in currentFileLineCount" :key="i" class="line-number">{{ i }}</div>
                  </div>
                  <pre ref="codeContentRef" class="code-content" :class="{ 'is-wrap': previewWrap }" :style="previewFontStyle" @scroll="syncCodeScroll"><code class="hljs" :data-language="currentFileHighlightLanguage" v-html="highlightedCurrentFileHtml"></code></pre>
                </div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'markdown'" class="rich-preview-shell">
                <div class="rich-preview-body ai-rich-content" :style="previewFontStyle" v-html="currentMarkdownHtml"></div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'text'" class="text-preview-shell">
                <pre class="plain-text-preview" :class="{ 'is-wrap': previewWrap }" :style="previewFontStyle">{{ currentFile.content }}</pre>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'image'" class="media-preview-shell image-preview-shell">
                <img :src="currentFile.blobUrl" :alt="currentFile.name" class="image-preview-element">
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'pdf'" class="media-preview-shell pdf-preview-shell">
                <iframe :src="currentFile.blobUrl" class="pdf-preview-frame" frameborder="0"></iframe>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'audio'" class="media-preview-shell audio-preview-shell">
                <div class="media-preview-title">音频预览</div>
                <audio :src="currentFile.blobUrl" controls class="audio-preview-element"></audio>
                <div class="media-preview-tip">支持在线播放音频，并可继续下载原文件。</div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'video'" class="media-preview-shell video-preview-shell">
                <video :src="currentFile.blobUrl" controls class="video-preview-element"></video>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'table'" class="table-preview-shell">
                <div v-if="currentFile.tablePreview && currentFile.tablePreview.headers.length" class="table-preview-wrap">
                  <table class="preview-table">
                    <thead>
                      <tr>
                        <th v-for="(header, index) in currentFile.tablePreview.headers" :key="'head-' + index">{{ header || ('列' + (index + 1)) }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rowIndex) in currentFile.tablePreview.rows" :key="'row-' + rowIndex">
                        <td v-for="(cell, cellIndex) in row" :key="rowIndex + '-' + cellIndex">{{ cell }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="empty-preview">表格内容为空或解析失败</div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'docx'" class="office-preview-shell">
                <div class="office-preview-head">
                  <span class="office-preview-badge">Word</span>
                  <span class="office-preview-subtitle">提取文档正文进行在线预览</span>
                </div>
                <div class="office-document-body">
                  <p v-for="(paragraph, index) in currentFile.officePreview.paragraphs" :key="'docx-' + index">{{ paragraph }}</p>
                  <div v-if="!currentFile.officePreview.paragraphs.length" class="empty-preview">文档内容为空或暂时无法解析</div>
                </div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'spreadsheet'" class="office-preview-shell spreadsheet-preview-shell">
                <div class="office-preview-head">
                  <span class="office-preview-badge">Excel</span>
                  <span class="office-preview-subtitle">已解析工作表内容，可滚动查看</span>
                </div>
                <div v-if="currentFile.officePreview.sheets && currentFile.officePreview.sheets.length" class="sheet-preview-list">
                  <div v-for="(sheet, index) in currentFile.officePreview.sheets" :key="'sheet-' + index" class="sheet-preview-card">
                    <div class="sheet-preview-name">{{ sheet.name || ('Sheet ' + (index + 1)) }}</div>
                    <div class="table-preview-wrap">
                      <table class="preview-table">
                        <thead>
                          <tr>
                            <th v-for="(header, headerIndex) in sheet.headers" :key="'sheet-head-' + index + '-' + headerIndex">{{ header || ('列' + (headerIndex + 1)) }}</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(row, rowIndex) in sheet.rows" :key="'sheet-row-' + index + '-' + rowIndex">
                            <td v-for="(cell, cellIndex) in row" :key="'sheet-cell-' + index + '-' + rowIndex + '-' + cellIndex">{{ cell }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
                <div v-else class="empty-preview">工作表内容为空或暂时无法解析</div>
              </div>
              <div v-else-if="currentFile.id && currentPreviewType === 'presentation'" class="office-preview-shell ppt-preview-shell">
                <div class="office-preview-head">
                  <span class="office-preview-badge">PPT</span>
                  <span class="office-preview-subtitle">已提取幻灯片文本内容进行结构化展示</span>
                </div>
                <div v-if="currentFile.officePreview.slides && currentFile.officePreview.slides.length" class="ppt-slide-list">
                  <div v-for="slide in currentFile.officePreview.slides" :key="'slide-' + slide.index" class="ppt-slide-card">
                    <div class="ppt-slide-title">第 {{ slide.index }} 页</div>
                    <div class="ppt-slide-lines">
                      <p v-for="(line, lineIndex) in slide.lines" :key="'slide-line-' + slide.index + '-' + lineIndex">{{ line }}</p>
                    </div>
                  </div>
                </div>
                <div v-else class="empty-preview">幻灯片内容为空或暂时无法解析</div>
              </div>
              <div v-else-if="currentFile.id" class="empty-preview unsupported-preview">
                <div class="unsupported-title">当前文件暂不支持在线预览</div>
                <div class="unsupported-desc">{{ currentFile.previewError || unsupportedPreviewMessage }}</div>
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

        <el-card v-if="pageAccessResolved && canSeeTaskCollaboration" shadow="never" class="section-card side-card">
          <div slot="header" class="section-header section-header-flex">
            <span>我的待办</span>
            <div class="side-task-header-actions">
              <el-tag size="mini" type="warning" effect="plain">{{ myTodoTasks.length }}</el-tag>
              <el-button size="mini" type="text" :loading="myTasksLoading" @click="fetchMyTasks">刷新</el-button>
            </div>
          </div>
          <div v-loading="myTasksLoading" class="side-task-card-body">
            <div class="side-task-summary">
              <div class="side-task-summary-value">{{ myTodoPendingCount }}</div>
              <div class="side-task-summary-label">未完成</div>
            </div>
            <div v-if="myTodoTasks.length" class="side-task-list">
              <div
                v-for="task in myTodoTasks"
                :key="'my-' + task.id"
                class="side-task-item"
                :class="{ 'is-overdue': isTaskOverdue(task) }"
              >
                <div class="side-task-main">
                  <div class="side-task-title">{{ task.title || '未命名任务' }}</div>
                  <div class="side-task-meta">
                    <el-tag size="mini" effect="plain" :type="getTaskPriorityType(task.priority)">{{ getTaskPriorityText(task.priority) }}</el-tag>
                    <span>{{ getTaskDueLabel(task) }}</span>
                  </div>
                </div>
                <el-button
                  size="mini"
                  type="success"
                  plain
                  :loading="taskQuickUpdatingId === task.id"
                  @click="handleQuickTaskStatusChange(task, 'done')"
                >
                  完成
                </el-button>
              </div>
            </div>
            <el-empty v-else description="暂无我的待办" :image-size="60" />
          </div>
        </el-card>

        <el-card v-if="pageAccessResolved && canSeeTaskCollaboration" shadow="never" class="section-card side-card">
          <div slot="header" class="section-header section-header-flex">
            <span>今天到期提醒</span>
            <div class="side-task-header-actions">
              <el-tag size="mini" type="danger" effect="plain">{{ todayDueTasks.length }}</el-tag>
              <el-button size="mini" type="text" :loading="taskBoardLoading" @click="fetchProjectTasks">刷新</el-button>
            </div>
          </div>
          <div v-loading="taskBoardLoading" class="side-task-card-body">
            <div class="side-task-summary danger">
              <div class="side-task-summary-value">{{ todayDueTasks.length }}</div>
              <div class="side-task-summary-label">今日到期</div>
            </div>
            <div v-if="todayDueTasks.length" class="side-task-list">
              <div
                v-for="task in todayDueTasks"
                :key="'due-' + task.id"
                class="side-task-item side-task-item-due"
              >
                <div class="side-task-main">
                  <div class="side-task-title">{{ task.title || '未命名任务' }}</div>
                  <div class="side-task-meta">
                    <span>{{ getTaskAssigneeName(task) }}</span>
                    <span>{{ formatTaskDueClock(task.dueDate) }}</span>
                  </div>
                </div>
                <el-select
                  :value="task.status"
                  size="mini"
                  placeholder="状态"
                  class="side-task-status-select"
                  :disabled="taskQuickUpdatingId === task.id"
                  @change="handleQuickTaskStatusChange(task, $event)"
                >
                  <el-option label="待处理" value="todo" />
                  <el-option label="进行中" value="in_progress" />
                  <el-option label="已完成" value="done" />
                </el-select>
              </div>
            </div>
            <el-empty v-else description="今天暂无到期任务" :image-size="60" />
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
          <input ref="uploadInput" type="file" :multiple="!uploadDialog.isVersion" @change="handlePickedFile" />
          <div v-if="uploadDialog.isVersion && uploadDialog.file" class="upload-picked-tip">
            已选择：{{ uploadDialog.file.name }}
          </div>
          <div v-else-if="!uploadDialog.isVersion && uploadDialog.files.length" class="upload-picked-list">
            <div class="upload-picked-title">已选择 {{ uploadDialog.files.length }} 个文件：</div>
            <div class="upload-picked-items">
              <span v-for="file in uploadDialog.files" :key="file.name + '_' + file.size" class="upload-picked-item">
                {{ file.name }}
              </span>
            </div>
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
import hljs from 'highlight.js/lib/common'
import JSZip from 'jszip'
import 'highlight.js/styles/atom-one-dark.css'
import {
  getProjectDetail,
  getProjectContributors,
  getRelatedProjects,
  starProject,
  unstarProject,
  getProjectStarStatus,
  updateProject,
  listProjectMembers,
  listProjectTasks,
  listMyTasks,
  updateTaskStatus,
  listProjectFiles,
  listFileVersions,
  uploadProjectFile,
  uploadProjectZip,
  uploadFileNewVersion,
  setMainFile,
  deleteFile,
  previewProjectFile,
  downloadFile,
  downloadProjectFiles
} from '@/api/project'
import { aiSummarizeProject, aiSplitProjectTasks, normalizeProjectSummaryPayload, normalizeProjectTaskPayload } from '@/api/aiAssistant'
import { listEnabledAiModels, pageAiModels } from '@/api/aiAdmin'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

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

const CODE_EXTENSIONS = new Set([
  'js', 'mjs', 'cjs', 'jsx', 'ts', 'tsx', 'vue', 'json', 'html', 'htm', 'css', 'scss', 'less',
  'java', 'kt', 'xml', 'yml', 'yaml', 'sql', 'sh', 'bash', 'zsh', 'py', 'rb', 'go', 'rs', 'c',
  'cpp', 'cc', 'cxx', 'h', 'hpp', 'cs', 'php', 'ini', 'log', 'properties', 'txt'
])

const MARKDOWN_EXTENSIONS = new Set(['md', 'markdown'])
const IMAGE_EXTENSIONS = new Set(['png', 'jpg', 'jpeg', 'gif', 'bmp', 'svg', 'webp', 'ico', 'avif'])
const PDF_EXTENSIONS = new Set(['pdf'])
const AUDIO_EXTENSIONS = new Set(['mp3', 'wav', 'ogg', 'm4a', 'aac', 'flac'])
const VIDEO_EXTENSIONS = new Set(['mp4', 'webm', 'ogg', 'mov', 'm4v'])
const TABLE_EXTENSIONS = new Set(['csv', 'tsv'])
const DOCX_EXTENSIONS = new Set(['docx'])
const SPREADSHEET_EXTENSIONS = new Set(['xlsx'])
const PRESENTATION_EXTENSIONS = new Set(['pptx'])
const LEGACY_OFFICE_EXTENSIONS = new Set(['doc', 'xls', 'ppt'])
const TEXT_EXTENSIONS = new Set([
  ...Array.from(CODE_EXTENSIONS),
  ...Array.from(MARKDOWN_EXTENSIONS),
  ...Array.from(TABLE_EXTENSIONS)
])

const HIGHLIGHT_LANGUAGE_MAP = {
  js: 'javascript',
  mjs: 'javascript',
  cjs: 'javascript',
  jsx: 'javascript',
  ts: 'typescript',
  tsx: 'typescript',
  vue: 'xml',
  java: 'java',
  kt: 'kotlin',
  py: 'python',
  rb: 'ruby',
  go: 'go',
  rs: 'rust',
  c: 'c',
  h: 'c',
  cpp: 'cpp',
  cc: 'cpp',
  cxx: 'cpp',
  hpp: 'cpp',
  cs: 'csharp',
  php: 'php',
  json: 'json',
  yml: 'yaml',
  yaml: 'yaml',
  xml: 'xml',
  html: 'xml',
  htm: 'xml',
  css: 'css',
  scss: 'scss',
  less: 'less',
  md: 'markdown',
  sql: 'sql',
  sh: 'bash',
  bash: 'bash',
  zsh: 'bash',
  properties: 'properties',
  ini: 'ini',
  txt: 'plaintext',
  log: 'plaintext'
}

function getHighlightLanguage(extension = '') {
  const ext = String(extension || '').trim().toLowerCase()
  const target = HIGHLIGHT_LANGUAGE_MAP[ext] || ext
  if (!target) return 'plaintext'
  return hljs.getLanguage(target) ? target : 'plaintext'
}

function detectPreviewType(extension = '') {
  const ext = String(extension || '').trim().toLowerCase()
  if (CODE_EXTENSIONS.has(ext)) return 'code'
  if (MARKDOWN_EXTENSIONS.has(ext)) return 'markdown'
  if (TABLE_EXTENSIONS.has(ext)) return 'table'
  if (IMAGE_EXTENSIONS.has(ext)) return 'image'
  if (PDF_EXTENSIONS.has(ext)) return 'pdf'
  if (AUDIO_EXTENSIONS.has(ext)) return 'audio'
  if (VIDEO_EXTENSIONS.has(ext)) return 'video'
  if (DOCX_EXTENSIONS.has(ext)) return 'docx'
  if (SPREADSHEET_EXTENSIONS.has(ext)) return 'spreadsheet'
  if (PRESENTATION_EXTENSIONS.has(ext)) return 'presentation'
  if (LEGACY_OFFICE_EXTENSIONS.has(ext)) return 'office-legacy'
  if (TEXT_EXTENSIONS.has(ext)) return 'text'
  return 'binary'
}

function createEmptyPreviewState() {
  return {
    id: null,
    name: '',
    path: '',
    size: 0,
    extension: '',
    actualType: '',
    content: '',
    isMain: false,
    versions: [],
    previewType: '',
    blobUrl: '',
    mimeType: '',
    markdownHtml: '',
    tablePreview: {
      headers: [],
      rows: []
    },
    officePreview: {
      paragraphs: [],
      sheets: [],
      slides: []
    },
    previewError: '',
    previewLoading: false
  }
}

function normalizeLineBreaks(text = '') {
  return String(text || '').replace(/\r\n?/g, '\n')
}


async function safeReadBlobText(blob) {
  if (!blob) return ''
  try {
    if (typeof blob.text === 'function') {
      const text = await blob.text()
      return normalizeLineBreaks(text)
    }
  } catch (error) {}

  try {
    const buffer = await blob.arrayBuffer()
    const utf8 = new TextDecoder('utf-8', { fatal: false }).decode(buffer)
    return normalizeLineBreaks(utf8)
  } catch (error) {}

  return ''
}

async function blobLooksLikeZip(blob) {
  if (!blob) return false
  try {
    const buffer = await blob.slice(0, 4).arrayBuffer()
    const bytes = new Uint8Array(buffer)
    return bytes.length >= 4 && bytes[0] === 0x50 && bytes[1] === 0x4B && (
      (bytes[2] === 0x03 && bytes[3] === 0x04) ||
      (bytes[2] === 0x05 && bytes[3] === 0x06) ||
      (bytes[2] === 0x07 && bytes[3] === 0x08)
    )
  } catch (error) {
    return false
  }
}

function parseDelimitedText(source = '', delimiter = ',') {
  const text = normalizeLineBreaks(String(source || '')).replace(/^﻿/, '')
  const rows = []
  let currentRow = []
  let currentCell = ''
  let inQuotes = false

  for (let i = 0; i < text.length; i += 1) {
    const char = text[i]
    const next = text[i + 1]
    if (char === '"') {
      if (inQuotes && next === '"') {
        currentCell += '"'
        i += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }
    if (!inQuotes && char === delimiter) {
      currentRow.push(currentCell)
      currentCell = ''
      continue
    }
    if (!inQuotes && char === '\n') {
      currentRow.push(currentCell)
      rows.push(currentRow)
      currentRow = []
      currentCell = ''
      continue
    }
    currentCell += char
  }
  if (currentCell !== '' || currentRow.length) {
    currentRow.push(currentCell)
    rows.push(currentRow)
  }
  const normalizedRows = rows.map(row => row.map(cell => String(cell || '').trim())).filter(row => row.some(cell => cell !== ''))
  if (!normalizedRows.length) return { headers: [], rows: [] }
  const columnCount = Math.max(...normalizedRows.map(row => row.length))
  const normalizedMatrix = normalizedRows.map(row => Array.from({ length: columnCount }, (_, index) => row[index] || ''))
  return {
    headers: normalizedMatrix[0].map((cell, index) => cell || `列${index + 1}`),
    rows: normalizedMatrix.slice(1)
  }
}

function collectXmlNodesByLocalName(root, localName) {
  const result = []
  const visit = (node) => {
    if (!node || !node.childNodes) return
    Array.from(node.childNodes).forEach((child) => {
      if (child.nodeType === 1) {
        if (child.localName === localName) result.push(child)
        visit(child)
      }
    })
  }
  visit(root.documentElement || root)
  return result
}

function parseXmlText(xmlText = '') {
  if (!process.client || !window.DOMParser) return null
  try {
    return new window.DOMParser().parseFromString(xmlText, 'application/xml')
  } catch (error) {
    return null
  }
}

function getColumnIndexByRef(ref = '') {
  const letters = String(ref || '').match(/[A-Za-z]+/)
  if (!letters) return 0
  return letters[0].toUpperCase().split('').reduce((total, char) => total * 26 + char.charCodeAt(0) - 64, 0) - 1
}

async function parseDocxFile(blob) {
  const zip = await JSZip.loadAsync(await blob.arrayBuffer())
  const documentEntry = zip.file('word/document.xml')
  if (!documentEntry) return { paragraphs: [] }
  const xmlText = await documentEntry.async('string')
  const doc = parseXmlText(xmlText)
  if (!doc) {
    const fallback = xmlText.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    return { paragraphs: fallback ? [fallback] : [] }
  }
  return {
    paragraphs: collectXmlNodesByLocalName(doc, 'p')
      .map(paragraph => collectXmlNodesByLocalName(paragraph, 't').map(node => node.textContent || '').join(''))
      .map(item => String(item || '').trim())
      .filter(Boolean)
  }
}

async function parseXlsxFile(blob) {
  const zip = await JSZip.loadAsync(await blob.arrayBuffer())
  const sharedStringsEntry = zip.file('xl/sharedStrings.xml')
  let sharedStrings = []
  if (sharedStringsEntry) {
    const sharedDoc = parseXmlText(await sharedStringsEntry.async('string'))
    if (sharedDoc) {
      sharedStrings = collectXmlNodesByLocalName(sharedDoc, 'si').map(item => collectXmlNodesByLocalName(item, 't').map(node => node.textContent || '').join(''))
    }
  }
  const workbookEntry = zip.file('xl/workbook.xml')
  const workbookRelsEntry = zip.file('xl/_rels/workbook.xml.rels')
  if (!workbookEntry || !workbookRelsEntry) return { sheets: [] }
  const workbookDoc = parseXmlText(await workbookEntry.async('string'))
  const relsDoc = parseXmlText(await workbookRelsEntry.async('string'))
  if (!workbookDoc || !relsDoc) return { sheets: [] }
  const relMap = {}
  collectXmlNodesByLocalName(relsDoc, 'Relationship').forEach((relation) => {
    const id = relation.getAttribute('Id')
    const target = relation.getAttribute('Target')
    if (id && target) relMap[id] = target.replace(/^\/?/, '')
  })
  const sheets = []
  for (const sheetNode of collectXmlNodesByLocalName(workbookDoc, 'sheet').slice(0, 3)) {
    const name = sheetNode.getAttribute('name') || 'Sheet'
    const relationId = sheetNode.getAttribute('r:id') || sheetNode.getAttribute('id')
    const target = relMap[relationId]
    if (!target) continue
    const sheetEntry = zip.file(`xl/${target}`)
    if (!sheetEntry) continue
    const sheetDoc = parseXmlText(await sheetEntry.async('string'))
    if (!sheetDoc) continue
    const rows = []
    let maxColumnCount = 0
    collectXmlNodesByLocalName(sheetDoc, 'row').slice(0, 80).forEach((rowNode) => {
      const rowData = []
      collectXmlNodesByLocalName(rowNode, 'c').forEach((cellNode) => {
        const columnIndex = getColumnIndexByRef(cellNode.getAttribute('r') || '')
        const cellType = cellNode.getAttribute('t') || ''
        let value = ''
        if (cellType === 's') {
          const valueNode = collectXmlNodesByLocalName(cellNode, 'v')[0]
          const sharedIndex = Number(valueNode && valueNode.textContent)
          value = Number.isNaN(sharedIndex) ? '' : (sharedStrings[sharedIndex] || '')
        } else if (cellType === 'inlineStr') {
          value = collectXmlNodesByLocalName(cellNode, 't').map(node => node.textContent || '').join('')
        } else {
          const valueNode = collectXmlNodesByLocalName(cellNode, 'v')[0]
          value = valueNode ? valueNode.textContent || '' : ''
        }
        rowData[columnIndex] = String(value || '').trim()
      })
      maxColumnCount = Math.max(maxColumnCount, rowData.length)
      rows.push(rowData)
    })
    const normalizedRows = rows.map(row => Array.from({ length: maxColumnCount }, (_, index) => row[index] || '')).filter(row => row.some(cell => cell !== ''))
    if (normalizedRows.length) {
      sheets.push({
        name,
        headers: normalizedRows[0].map((cell, index) => cell || `列${index + 1}`),
        rows: normalizedRows.slice(1)
      })
    }
  }
  return { sheets }
}

async function parsePptxFile(blob) {
  const zip = await JSZip.loadAsync(await blob.arrayBuffer())
  const slideEntries = Object.keys(zip.files)
    .filter(name => /^ppt\/slides\/slide\d+\.xml$/.test(name))
    .sort((a, b) => Number((a.match(/slide(\d+)\.xml/) || [])[1] || 0) - Number((b.match(/slide(\d+)\.xml/) || [])[1] || 0))
  const slides = []
  for (const [index, slideName] of slideEntries.slice(0, 20).entries()) {
    const entry = zip.file(slideName)
    if (!entry) continue
    const doc = parseXmlText(await entry.async('string'))
    if (!doc) continue
    const lines = collectXmlNodesByLocalName(doc, 't').map(node => String(node.textContent || '').trim()).filter(Boolean)
    slides.push({ index: index + 1, lines })
  }
  return { slides }
}

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

function normalizeMarkdownUrl(url = '') {
  const value = String(url || '').trim()
  if (!value) return ''
  if (/^(https?:|mailto:|tel:)/i.test(value)) return value
  if (/^(\/|#|\.\/|\.\.\/)/.test(value)) return value
  return ''
}

function renderMarkdownLink(label = '', url = '', title = '') {
  const safeUrl = normalizeMarkdownUrl(url)
  const safeLabel = escapeHtmlValue(label || url)
  const safeTitle = escapeHtmlValue(title || '')
  if (!safeUrl) return safeLabel
  const titleAttr = safeTitle ? ` title="${safeTitle}"` : ''
  return `<a href="${safeUrl}" target="_blank" rel="noopener noreferrer nofollow"${titleAttr}>${safeLabel}</a>`
}

function renderMarkdownImage(alt = '', url = '', title = '') {
  const safeUrl = normalizeMarkdownUrl(url)
  const safeAlt = escapeHtmlValue(alt || 'README 图片')
  const safeTitle = escapeHtmlValue(title || alt || 'README 图片')
  if (!safeUrl) return `<span class="markdown-image-alt">${safeAlt}</span>`
  return `<span class="markdown-image-wrap"><img src="${safeUrl}" alt="${safeAlt}" title="${safeTitle}" loading="lazy"></span>`
}

function renderInlineMarkdown(text) {
  const tokens = []
  const pushToken = (html) => {
    const key = `@@MD_TOKEN_${tokens.length}@@`
    tokens.push(html)
    return key
  }

  let raw = String(text || '')
  raw = raw.replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+"([^"]*)")?\)/g, (_, alt, url, title) => pushToken(renderMarkdownImage(alt, url, title)))
  raw = raw.replace(/\[([^\]]+)\]\(([^)\s]+)(?:\s+"([^"]*)")?\)/g, (_, label, url, title) => pushToken(renderMarkdownLink(label, url, title)))

  return escapeHtmlValue(raw)
    .replace(/&lt;br\s*\/?&gt;/gi, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/~~(.+?)~~/g, '<del>$1</del>')
    .replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>')
    .replace(/@@MD_TOKEN_(\d+)@@/g, (_, index) => tokens[Number(index)] || '')
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

function stripMarkdownToPlainText(source = '') {
  return String(source || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '$1 ')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '$1 ')
    .replace(/[`>#*_~|-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function countReadmeReadableUnits(source = '') {
  const plain = stripMarkdownToPlainText(source)
  if (!plain) return 0
  const chineseCount = (plain.match(/[\u4e00-\u9fff]/g) || []).length
  const latinCount = (plain.match(/[A-Za-z0-9_]+/g) || []).length
  return chineseCount + latinCount
}

function countMarkdownHeadings(source = '') {
  return String(source || '')
    .split('\n')
    .filter(line => /^\s*#{1,6}\s+/.test(String(line || '')))
    .length
}

function countMarkdownCodeBlocks(source = '') {
  const matches = String(source || '').match(/```/g)
  return matches ? Math.floor(matches.length / 2) : 0
}

function buildReadmeLeadText(source = '', fallback = '') {
  const plain = stripMarkdownToPlainText(source)
  if (plain) return plain.slice(0, 96)
  return String(fallback || '当前项目还没有补充 README 内容').trim()
}

function formatEstimatedReadTime(source = '') {
  const units = countReadmeReadableUnits(source)
  if (!units) return '少于 1 分钟'
  const minutes = Math.max(1, Math.ceil(units / 360))
  return `${minutes} 分钟`
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
      const fenceMatch = trimmed.match(/^```\s*([A-Za-z0-9_+-]*)\s*$/)
      const codeLang = fenceMatch && fenceMatch[1] ? fenceMatch[1].toLowerCase() : ''
      const codeLines = []
      i += 1
      while (i < lines.length && !/^```/.test(String(lines[i] || '').trim())) {
        codeLines.push(lines[i])
        i += 1
      }
      if (i < lines.length && /^```/.test(String(lines[i] || '').trim())) {
        i += 1
      }
      const codeText = codeLines.join('\n')
      const language = getHighlightLanguage(codeLang)
      let codeHtml = escapeHtmlValue(codeText)
      try {
        codeHtml = hljs.highlight(codeText, { language, ignoreIllegals: true }).value
      } catch (error) {}
      const langLabel = escapeHtmlValue(codeLang || 'text').toUpperCase()
      blocks.push(`<div class="markdown-code-block"><div class="markdown-code-head"><span class="markdown-code-lang">${langLabel}</span></div><pre><code class="hljs language-${escapeHtmlValue(language)}">${codeHtml}</code></pre></div>`)
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

    if (/^!\[[^\]]*\]\([^)]+\)$/.test(trimmed)) {
      blocks.push(`<p class="markdown-image-only">${renderInlineMarkdown(trimmed)}</p>`)
      i += 1
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
        const rawItem = String(lines[i] || '').replace(/^\s*[-*+]\s+/, '')
        const taskMatch = rawItem.match(/^\[( |x|X)\]\s+(.*)$/)
        if (taskMatch) {
          const checked = String(taskMatch[1] || '').toLowerCase() === 'x'
          items.push(`<li class="markdown-task-item${checked ? ' is-checked' : ''}"><span class="markdown-task-box">${checked ? '✓' : ''}</span><span>${renderInlineMarkdown(taskMatch[2] || '')}</span></li>`)
        } else {
          items.push(`<li>${renderInlineMarkdown(rawItem)}</li>`)
        }
        i += 1
      }
      blocks.push(`<ul>${items.join('')}</ul>`)
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
      pageAccessResolved: false,
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
      aiSummaryCard: {
        overview: '',
        scenarios: [],
        features: [],
        risks: [],
        nextActions: [],
        rawText: ''
      },
      aiTaskCard: {
        phases: [],
        executionOrder: [],
        risks: [],
        rawText: ''
      },
      taskBoardLoading: false,
      myTasksLoading: false,
      taskQuickUpdatingId: null,
      taskList: [],
      myTaskList: [],
      memberListLoaded: false,
      memberList: [],
      treeFilterText: '',
      selectedFileIds: [],
      previewWrap: false,
      previewFontSize: 14,
      previewFullscreen: false,
      treePanelWidth: 360,
      treeResizeActive: false,
      treeResizeMinWidth: 280,
      treeResizeMaxWidth: 640,
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
      currentFile: createEmptyPreviewState(),
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
        files: []
      },
      categoryOptions: Object.keys(CATEGORY_MAP).map(key => ({ value: key, label: CATEGORY_MAP[key] })),
      statusOptions: Object.keys(STATUS_MAP).map(key => ({ value: key, label: STATUS_MAP[key] }))
    }
  },

  computed: {
    renderedReadme() {
      return this.renderMarkdownContent(this.project.readme, '暂无 README 文档')
    },
    readmeSourceText() {
      return String(this.project.readme || '')
    },
    readmeHasContent() {
      return !!this.readmeSourceText.trim()
    },
    readmeReadableUnits() {
      return countReadmeReadableUnits(this.readmeSourceText)
    },
    readmeHeadingCount() {
      return countMarkdownHeadings(this.readmeSourceText)
    },
    readmeCodeBlockCount() {
      return countMarkdownCodeBlocks(this.readmeSourceText)
    },
    readmeReadTimeText() {
      return formatEstimatedReadTime(this.readmeSourceText)
    },
    readmeLeadText() {
      return buildReadmeLeadText(this.readmeSourceText, this.project.description)
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
    currentUserId() {
      return this.getCurrentAiUserId()
    },
    isProjectOwner() {
      return this.currentUserId !== null && this.currentUserId !== undefined && Number(this.project.authorId) === Number(this.currentUserId)
    },
    currentMemberRecord() {
      if (this.currentUserId === null || this.currentUserId === undefined || this.currentUserId === '') return null
      const sources = []
      if (Array.isArray(this.memberList) && this.memberList.length) sources.push(...this.memberList)
      if (Array.isArray(this.project.members) && this.project.members.length) sources.push(...this.project.members)
      return sources.find(item => {
        if (!item) return false
        const targetUserId = item.userId ?? item.id
        if (targetUserId === null || targetUserId === undefined || targetUserId === '') return false
        const status = String(item.status || item.memberStatus || 'active').toLowerCase()
        return Number(targetUserId) === Number(this.currentUserId) && status !== 'inactive' && status !== 'removed'
      }) || null
    },
    currentProjectRole() {
      if (this.isProjectOwner) return 'owner'
      return this.currentMemberRecord && this.currentMemberRecord.role ? String(this.currentMemberRecord.role).toLowerCase() : ''
    },
    canManageProject() {
      return this.currentProjectRole === 'owner' || this.currentProjectRole === 'admin'
    },
    canSeeTaskCollaboration() {
      if (this.currentUserId === null || this.currentUserId === undefined || this.currentUserId === '') return false
      if (this.isProjectOwner) return true
      return !!this.currentMemberRecord
    },
    hasAiResult() {
      return !!(this.aiSummaryCard.overview || this.aiTaskCard.phases.length || this.aiProjectSummary || this.aiProjectTasks)
    },
    taskSummary() {
      return {
        total: this.taskList.length,
        todo: this.taskList.filter(item => item.status === 'todo').length,
        inProgress: this.taskList.filter(item => item.status === 'in_progress').length,
        done: this.taskList.filter(item => item.status === 'done').length,
        overdue: this.taskList.filter(item => this.isTaskOverdue(item)).length
      }
    },
    recentTasks() {
      return [...this.taskList]
        .sort((a, b) => this.getTaskSortTime(b) - this.getTaskSortTime(a))
        .slice(0, 5)
    },
    overdueTasks() {
      return this.taskList
        .filter(item => this.isTaskOverdue(item))
        .sort((a, b) => this.getTaskDueTimestamp(a) - this.getTaskDueTimestamp(b))
        .slice(0, 5)
    },
    myTodoTasks() {
      return this.myTaskList
        .filter(item => item.status !== 'done')
        .sort((a, b) => {
          const dueDiff = this.getTaskDueTimestamp(a) - this.getTaskDueTimestamp(b)
          if (dueDiff !== 0) return dueDiff
          return this.getTaskSortTime(b) - this.getTaskSortTime(a)
        })
        .slice(0, 4)
    },
    myTodoPendingCount() {
      return this.myTaskList.filter(item => item.status !== 'done').length
    },
    todayDueTasks() {
      return this.taskList
        .filter(item => this.isTaskDueToday(item) && item.status !== 'done')
        .sort((a, b) => this.getTaskDueTimestamp(a) - this.getTaskDueTimestamp(b))
        .slice(0, 4)
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
    },
    totalFileCount() {
      return this.flattenFileTree(this.fileTree).length
    },
    isAllFilesSelected() {
      return this.totalFileCount > 0 && this.selectedFileIds.length === this.totalFileCount
    },
    currentFileFlatIndex() {
      if (!this.currentFile.id) return -1
      return this.flattenFileTree(this.fileTree).findIndex(item => String(item.id) === String(this.currentFile.id))
    },
    currentFileDisplayIndex() {
      return this.currentFileFlatIndex >= 0 ? this.currentFileFlatIndex + 1 : 0
    },
    hasPrevPreviewFile() {
      return this.currentFileFlatIndex > 0
    },
    hasNextPreviewFile() {
      return this.currentFileFlatIndex >= 0 && this.currentFileFlatIndex < this.totalFileCount - 1
    },
    previewFullscreenButtonText() {
      return this.previewFullscreen ? '退出全屏' : '全屏预览'
    },
    currentPreviewType() {
      return this.currentFile.previewType || detectPreviewType(this.currentFile.actualType || this.currentFile.extension)
    },
    currentFileLineCount() {
      const normalized = normalizeLineBreaks(this.currentFile.content || '')
      return normalized ? normalized.split('\n').length : 0
    },
    currentPreviewMetricLabel() {
      if (!this.currentFile.id) return ''
      if (this.currentPreviewType === 'code' || this.currentPreviewType === 'markdown' || this.currentPreviewType === 'text') return `${this.currentFileLineCount || 0} 行`
      if (this.currentPreviewType === 'table') {
        const table = this.currentFile.tablePreview || { rows: [], headers: [] }
        return `${table.rows.length} 行 / ${table.headers.length} 列`
      }
      if (this.currentPreviewType === 'docx') return `${(this.currentFile.officePreview.paragraphs || []).length} 段`
      if (this.currentPreviewType === 'spreadsheet') return `${(this.currentFile.officePreview.sheets || []).length} 个工作表`
      if (this.currentPreviewType === 'presentation') return `${(this.currentFile.officePreview.slides || []).length} 页幻灯片`
      return '在线预览'
    },
    canAdjustPreview() {
      return ['code', 'text', 'markdown'].includes(this.currentPreviewType)
    },
    previewWrapButtonText() {
      return this.previewWrap ? '关闭换行' : '自动换行'
    },
    previewFontStyle() {
      return { '--preview-font-size': `${this.previewFontSize}px`, fontSize: `${this.previewFontSize}px` }
    },
    treePanelStyle() {
      return { width: `${this.treePanelWidth}px` }
    },
    currentMarkdownHtml() {
      return this.currentFile.markdownHtml || this.renderMarkdownContent(this.currentFile.content, '暂无 Markdown 内容')
    },
    canCopyCurrentFileContent() {
      return !!String(this.currentFile.content || '').trim()
    },
    unsupportedPreviewMessage() {
      if (this.currentPreviewType === 'office-legacy') return '当前支持 docx / xlsx / pptx 在线预览，旧版 Office 文件请下载后在本地软件中查看。'
      return '该文件类型暂不支持在线预览，请下载后查看。'
    },
    currentFileLanguageLabel() {
      const extension = String(this.currentFile.actualType || this.currentFile.extension || '').toLowerCase()
      const map = {
        js: 'JavaScript', ts: 'TypeScript', vue: 'Vue', java: 'Java', py: 'Python', go: 'Go', rs: 'Rust', cpp: 'C++', c: 'C', cs: 'C#', php: 'PHP',
        json: 'JSON', yml: 'YAML', yaml: 'YAML', xml: 'XML', html: 'HTML', css: 'CSS', scss: 'SCSS', less: 'LESS', md: 'Markdown', sql: 'SQL', sh: 'Shell',
        txt: 'Text', csv: 'CSV', tsv: 'TSV', pdf: 'PDF', png: '图片', jpg: '图片', jpeg: '图片', gif: '图片', webp: '图片', svg: 'SVG',
        mp3: '音频', wav: '音频', mp4: '视频', webm: '视频', docx: 'Word', xlsx: 'Excel', pptx: 'PPT'
      }
      return map[extension] || (extension ? extension.toUpperCase() : 'TEXT')
    },
    currentFileHighlightLanguage() {
      return getHighlightLanguage(this.currentFile.actualType || this.currentFile.extension)
    },
    highlightedCurrentFileHtml() {
      const content = String(this.currentFile.content || '')
      if (!content) return ''
      const language = this.currentFileHighlightLanguage
      try {
        if (language && language !== 'plaintext' && hljs.getLanguage(language)) return hljs.highlight(content, { language, ignoreIllegals: true }).value
        return hljs.highlight(content, { language: 'plaintext', ignoreIllegals: true }).value
      } catch (error) {
        return escapeHtmlValue(content)
      }
    }
  },

  watch: {
    treeFilterText(val) {
      if (this.$refs.fileTreeRef) {
        this.$refs.fileTreeRef.filter(val)
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
    document.addEventListener('keydown', this.handlePreviewKeyboard)
    this.projectId = this.$route.query.projectId || this.$route.params.id
    if (!this.projectId) {
      this.$message.error('缺少项目ID')
      return
    }
    await this.initPage()
  },

  beforeDestroy() {
    document.removeEventListener('keydown', this.handlePreviewKeyboard)
    this.syncPreviewFullscreenBody(false)
    this.clearPreviewBlobUrl()
    this.stopTreeResize()
  },

  methods: {
    togglePreviewWrap() {
      this.previewWrap = !this.previewWrap
    },
    syncPreviewFullscreenBody(nextState) {
      if (!process.client || typeof document === 'undefined' || !document.body) return
      document.body.style.overflow = nextState ? 'hidden' : ''
    },
    focusPreviewPanel() {
      this.$nextTick(() => {
        const panel = this.$refs.filePreviewPanelRef
        if (panel && typeof panel.focus === 'function') {
          panel.focus()
        }
      })
    },
    togglePreviewFullscreen() {
      if (!this.currentFile.id) return
      this.previewFullscreen = !this.previewFullscreen
      this.syncPreviewFullscreenBody(this.previewFullscreen)
      if (this.previewFullscreen) {
        this.focusPreviewPanel()
      }
    },
    handlePreviewKeyboard(event) {
      if (!this.previewFullscreen || !event) return
      const target = event.target
      const tagName = target && target.tagName ? String(target.tagName).toUpperCase() : ''
      if (target && (target.isContentEditable || tagName === 'INPUT' || tagName === 'TEXTAREA' || tagName === 'SELECT')) return
      if (event.key === 'Escape') {
        event.preventDefault()
        this.previewFullscreen = false
        this.syncPreviewFullscreenBody(false)
        return
      }
      if (event.key === 'ArrowLeft') {
        event.preventDefault()
        this.goPrevPreviewFile()
        return
      }
      if (event.key === 'ArrowRight') {
        event.preventDefault()
        this.goNextPreviewFile()
      }
    },
    increasePreviewFont() {
      this.previewFontSize = Math.min(18, this.previewFontSize + 1)
    },
    decreasePreviewFont() {
      this.previewFontSize = Math.max(12, this.previewFontSize - 1)
    },
    resetPreviewFont() {
      this.previewFontSize = 14
    },
    startTreeResize(event) {
      if (event && typeof event.preventDefault === 'function') event.preventDefault()
      this.treeResizeActive = true
      document.addEventListener('mousemove', this.handleTreeResize)
      document.addEventListener('mouseup', this.stopTreeResize)
    },
    handleTreeResize(event) {
      if (!this.treeResizeActive) return
      const browser = this.$refs.fileBrowserRef
      if (!browser || !event) return
      const rect = browser.getBoundingClientRect()
      const maxWidth = Math.min(this.treeResizeMaxWidth, Math.max(this.treeResizeMinWidth + 40, rect.width - 360))
      const nextWidth = Math.min(maxWidth, Math.max(this.treeResizeMinWidth, event.clientX - rect.left))
      this.treePanelWidth = nextWidth
    },
    stopTreeResize() {
      this.treeResizeActive = false
      document.removeEventListener('mousemove', this.handleTreeResize)
      document.removeEventListener('mouseup', this.stopTreeResize)
    },
    syncCodeScroll(event) {
      const lineBox = this.$refs.lineNumbersRef
      if (lineBox && event && event.target) {
        lineBox.scrollTop = event.target.scrollTop
      }
    },
    buildEmptyCurrentFile() {
      return createEmptyPreviewState()
    },

    clearPreviewBlobUrl() {
      if (process.client && this.currentFile && this.currentFile.blobUrl) {
        window.URL.revokeObjectURL(this.currentFile.blobUrl)
      }
    },

    async resolveFilePreview(node, blob) {
      const extension = String(node.extension || '').toLowerCase()
      const actualType = String(node.actualType || extension || '').toLowerCase()
      const previewType = detectPreviewType(actualType)
      const payload = {
        previewType,
        blobUrl: '',
        mimeType: blob.type || '',
        content: '',
        markdownHtml: '',
        tablePreview: { headers: [], rows: [] },
        officePreview: { paragraphs: [], sheets: [], slides: [] },
        previewError: ''
      }
      if (previewType === 'code' || previewType === 'text') {
        payload.content = await safeReadBlobText(blob)
        return payload
      }
      if (previewType === 'markdown') {
        payload.content = await safeReadBlobText(blob)
        payload.markdownHtml = this.renderMarkdownContent(payload.content, '暂无 Markdown 内容')
        return payload
      }
      if (previewType === 'table') {
        payload.content = await safeReadBlobText(blob)
        payload.tablePreview = parseDelimitedText(payload.content, actualType === 'tsv' ? '\t' : ',')
        return payload
      }
      if (previewType === 'image' || previewType === 'pdf' || previewType === 'audio' || previewType === 'video') {
        if (process.client) payload.blobUrl = window.URL.createObjectURL(blob)
        return payload
      }
      if (previewType === 'docx') {
        if (!(await blobLooksLikeZip(blob))) {
          payload.previewType = 'text'
          payload.previewError = '当前文件名像 Word 文档，但实际文件内容不是标准 docx 压缩包，已按文本回退预览。'
          payload.content = await safeReadBlobText(blob)
          return payload
        }
        payload.officePreview = await parseDocxFile(blob)
        payload.content = (payload.officePreview.paragraphs || []).join('\n')
        if (!(payload.officePreview.paragraphs || []).length) {
          payload.previewError = '未提取到正文内容，可能是扫描件、模板文档或当前文件内容与扩展名不一致。'
        }
        return payload
      }
      if (previewType === 'spreadsheet') {
        if (!(await blobLooksLikeZip(blob))) {
          payload.previewType = 'text'
          payload.previewError = '当前文件名像 Excel 文档，但实际文件内容不是标准 xlsx 压缩包，已按文本回退预览。'
          payload.content = await safeReadBlobText(blob)
          return payload
        }
        payload.officePreview = await parseXlsxFile(blob)
        if (!(payload.officePreview.sheets || []).length) {
          payload.previewError = '未提取到工作表内容，可能是空表、受保护文件或当前文件内容与扩展名不一致。'
        }
        return payload
      }
      if (previewType === 'presentation') {
        if (!(await blobLooksLikeZip(blob))) {
          payload.previewType = 'text'
          payload.previewError = '当前文件名像 PPT 文档，但实际文件内容不是标准 pptx 压缩包，已按文本回退预览。'
          payload.content = await safeReadBlobText(blob)
          return payload
        }
        payload.officePreview = await parsePptxFile(blob)
        payload.content = (payload.officePreview.slides || []).map(slide => slide.lines.join('\n')).join('\n\n')
        if (!(payload.officePreview.slides || []).length) {
          payload.previewError = '未提取到幻灯片文本，可能是纯图片 PPT、受保护文件或当前文件内容与扩展名不一致。'
        }
        return payload
      }
      payload.previewError = previewType === 'office-legacy'
        ? '当前支持 docx / xlsx / pptx 在线预览，旧版 Office 文件请下载后在本地软件中查看。'
        : '该文件类型暂不支持在线预览，请下载后查看。'
      return payload
    },

    async initPage() {
      this.loading = true
      this.pageAccessResolved = false
      try {
        const baseTasks = [
          this.fetchProjectDetail(),
          this.fetchContributors(),
          this.fetchRelatedProjects(),
          this.fetchFiles(),
          this.loadAiModels()
        ]
        const token = getToken ? getToken() : ''
        if (token) {
          baseTasks.push(this.fetchProjectStarState())
        } else {
          this.project.starred = false
          this.myTaskList = []
          this.memberList = []
          this.memberListLoaded = true
        }
        await Promise.all(baseTasks)

        if (token) {
          await this.fetchMemberSnapshot()
        }

        if (this.canSeeTaskCollaboration) {
          const taskJobs = [this.fetchProjectTasks()]
          if (token) {
            taskJobs.push(this.fetchMyTasks())
          } else {
            this.myTaskList = []
          }
          await Promise.all(taskJobs)
        } else {
          this.taskList = []
          this.myTaskList = []
        }
      } finally {
        this.pageAccessResolved = true
        this.loading = false
      }
    },

    async loadAiModels() {
      this.aiModelsLoading = true
      try {
        let enabledList = []
        try {
          const enabledRes = await listEnabledAiModels()
          const enabledData = extractApiData(enabledRes)
          enabledList = Array.isArray(enabledData) ? enabledData.map(normalizeAiModel) : []
        } catch (error) {
          console.error('加载已启用模型失败', error)
        }

        if (!enabledList.length) {
          try {
            const pageRes = await pageAiModels({ page: 0, size: 100 })
            const pagePayload = extractApiData(pageRes)
            enabledList = Array.isArray(pagePayload?.content)
              ? pagePayload.content.map(normalizeAiModel)
              : Array.isArray(pagePayload?.records)
                ? pagePayload.records.map(normalizeAiModel)
                : Array.isArray(pagePayload?.list)
                  ? pagePayload.list.map(normalizeAiModel)
                  : Array.isArray(pagePayload)
                    ? pagePayload.map(normalizeAiModel)
                    : []
          } catch (error) {
            console.error('兜底加载全部模型失败', error)
          }
        }

        this.aiModels = enabledList

        let savedModelId = null
        if (process.client) {
          savedModelId = window.localStorage.getItem('project_detail_ai_model_id')
        }

        const preferredModelId = savedModelId
          ? String(savedModelId)
          : (enabledList[0] && enabledList[0].id) || null
        this.selectedAiModelId = preferredModelId === '' ? null : preferredModelId
        this.activeAiModel = enabledList.find(item => String(item.id) === String(this.selectedAiModelId)) || enabledList[0] || null
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
        const normalized = result?.normalized || normalizeProjectSummaryPayload(result)
        this.aiSummaryCard = {
          overview: normalized.overview || '',
          scenarios: Array.isArray(normalized.scenarios) ? normalized.scenarios : [],
          features: Array.isArray(normalized.features) ? normalized.features : [],
          risks: Array.isArray(normalized.risks) ? normalized.risks : [],
          nextActions: Array.isArray(normalized.nextActions) ? normalized.nextActions : [],
          rawText: normalized.rawText || result?.text || ''
        }
        this.aiProjectSummary = normalized.displayText || result?.displayText || result?.text || ''
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
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可进行 AI 拆任务')
        return
      }
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
        const normalized = result?.normalized || normalizeProjectTaskPayload(result)
        this.aiTaskCard = {
          phases: Array.isArray(normalized.phases) ? normalized.phases : [],
          executionOrder: Array.isArray(normalized.executionOrder) ? normalized.executionOrder : [],
          risks: Array.isArray(normalized.risks) ? normalized.risks : [],
          rawText: normalized.rawText || result?.text || ''
        }
        this.aiProjectTasks = normalized.displayText || result?.displayText || result?.text || ''
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
      this.aiSummaryCard = {
        overview: '',
        scenarios: [],
        features: [],
        risks: [],
        nextActions: [],
        rawText: ''
      }
      this.aiTaskCard = {
        phases: [],
        executionOrder: [],
        risks: [],
        rawText: ''
      }
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

    normalizeTaskItem(item = {}) {
      return {
        ...item,
        assigneeName: item.assigneeName || '',
        assigneeAvatar: item.assigneeAvatar || '',
        creatorName: item.creatorName || '',
        status: item.status || 'todo',
        priority: item.priority || 'medium'
      }
    },

    async fetchMemberSnapshot() {
      const token = getToken ? getToken() : ''
      if (!token) {
        this.memberList = []
        this.memberListLoaded = true
        return
      }
      try {
        const res = await listProjectMembers(this.projectId)
        const list = Array.isArray(res.data) ? res.data : []
        this.memberList = list.map(item => ({ ...item }))
      } catch (error) {
        console.warn('获取成员快照失败:', error)
        this.memberList = []
      } finally {
        this.memberListLoaded = true
      }
    },

    showEditProjectDialog() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可编辑项目信息')
        return
      }
      this.showEditDialog = true
    },

    async fetchProjectTasks() {
      if (!this.canSeeTaskCollaboration) {
        this.taskList = []
        return
      }
      this.taskBoardLoading = true
      try {
        const res = await listProjectTasks(this.projectId)
        const list = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        this.taskList = list.map(this.normalizeTaskItem)
      } catch (error) {
        console.error(error)
        this.taskList = []
      } finally {
        this.taskBoardLoading = false
      }
    },

    async fetchMyTasks() {
      const token = getToken ? getToken() : ''
      if (!token || !this.canSeeTaskCollaboration) {
        this.myTaskList = []
        return
      }
      this.myTasksLoading = true
      try {
        const res = await listMyTasks(this.projectId)
        const list = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        this.myTaskList = list.map(this.normalizeTaskItem)
      } catch (error) {
        console.error(error)
        this.myTaskList = []
      } finally {
        this.myTasksLoading = false
      }
    },

    syncTaskCollections(updatedTask = {}) {
      const normalized = this.normalizeTaskItem(updatedTask)
      const targetId = String(normalized.id || '')
      if (!targetId) return
      const replaceItem = item => String(item.id) === targetId ? { ...item, ...normalized } : item
      this.taskList = this.taskList.map(replaceItem)
      this.myTaskList = this.myTaskList.map(replaceItem)
    },

    async handleQuickTaskStatusChange(task, status) {
      if (!task || !task.id || !status || task.status === status) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.warning('加入项目后才可参与任务协作')
        return
      }
      this.taskQuickUpdatingId = task.id
      try {
        const res = await updateTaskStatus(task.id, { status })
        const updatedTask = extractApiData(res) || { ...task, status }
        this.syncTaskCollections({ ...task, ...updatedTask, status })
        this.$message.success('任务状态已更新')
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '更新任务状态失败')
      } finally {
        this.taskQuickUpdatingId = null
      }
    },

    getTaskSortTime(task) {
      return this.parseDateTime(task && (task.updatedAt || task.createdAt || task.dueDate))
    },

    getTaskDueTimestamp(task) {
      return this.parseDateTime(task && task.dueDate)
    },

    parseDateTime(value) {
      if (!value) return 0
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? 0 : time
    },

    isTaskOverdue(task) {
      if (!task || task.status === 'done' || !task.dueDate) return false
      const dueTime = this.parseDateTime(task.dueDate)
      if (!dueTime) return false
      return dueTime < Date.now()
    },

    isTaskDueToday(task) {
      if (!task || !task.dueDate) return false
      const date = new Date(task.dueDate)
      if (Number.isNaN(date.getTime())) return false
      const now = new Date()
      return date.getFullYear() === now.getFullYear() &&
        date.getMonth() === now.getMonth() &&
        date.getDate() === now.getDate()
    },

    getTaskAssigneeName(task) {
      return (task && task.assigneeName) || '未分配'
    },

    getTaskTimeLabel(task) {
      if (task && task.updatedAt) return `更新于 ${this.formatTaskShortTime(task.updatedAt)}`
      if (task && task.createdAt) return `创建于 ${this.formatTaskShortTime(task.createdAt)}`
      return '时间未知'
    },

    getTaskDueLabel(task) {
      if (task && task.dueDate) return `截止 ${this.formatTaskShortTime(task.dueDate)}`
      return '未设置截止时间'
    },

    formatTaskShortTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },

    formatTaskDueClock(value) {
      if (!value) return '未设置时间'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      return `今天 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
    },

    getTaskStatusType(status) {
      return {
        todo: 'info',
        in_progress: 'warning',
        done: 'success'
      }[status] || 'info'
    },

    getTaskStatusText(status) {
      return {
        todo: '待处理',
        in_progress: '进行中',
        done: '已完成'
      }[status] || status || '未知状态'
    },

    getTaskPriorityType(priority) {
      return {
        low: 'info',
        medium: '',
        high: 'warning',
        urgent: 'danger'
      }[priority] || 'info'
    },

    getTaskPriorityText(priority) {
      return {
        low: '低',
        medium: '中',
        high: '高',
        urgent: '紧急'
      }[priority] || priority || '中'
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
      } catch (e) {
        console.error('getProjectDetail error:', e?.response?.data || e)
        this.$message.error(
          e?.response?.data?.message ||
          e?.response?.data?.msg ||
          '项目详情加载失败'
        )
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
      try {
        const res = await listProjectFiles(this.projectId)
        const files = Array.isArray(extractApiData(res)) ? extractApiData(res) : []
        this.project.files = files
        this.fileTree = this.buildFileTree(files)
        this.syncSelectedFileIds()
        await this.loadReadme(files)
      } catch (error) {
        console.error(error)
        this.$message.error(error.response?.data?.message || '获取项目文件失败')
      }
    },

    buildFileTree(files) {
      const root = []
      const nodeMap = {}

      const normalizePath = (value) => String(value || '')
        .replace(/\\/g, '/')
        .replace(/^\/+/, '')
        .replace(/\/+/g, '/')
        .trim()

      const resolveRelativePath = (file) => {
        const explicitRelativePath = normalizePath(file.relativePath || file.relative_file_path || '')
        if (explicitRelativePath) {
          return explicitRelativePath
        }

        const fileName = normalizePath(file.fileName || file.name || '')
        if (fileName) {
          return fileName
        }

        const fallback = normalizePath(file.path || file.filePath || '')
        if (!fallback) {
          return `file-${file.id || 'unknown'}`
        }

        const segments = fallback.split('/').filter(Boolean)
        return segments[segments.length - 1] || fallback
      }

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
        const relativePath = resolveRelativePath(file)
        const segments = relativePath.split('/').filter(Boolean)
        const fileName = segments[segments.length - 1] || `文件${file.id}`
        const folderPath = segments.slice(0, -1).join('/')
        const extFromName = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
        const actualType = String(file.fileType || '').trim().toLowerCase() || extFromName
        const children = ensureFolder(folderPath)

        children.push({
          id: file.id,
          name: fileName,
          path: relativePath,
          type: 'file',
          size: file.fileSizeBytes || 0,
          extension: extFromName,
          actualType,
          isMain: !!file.isMain,
          raw: file,
          children: []
        })
      })

      const sortNodes = (nodes = []) => {
        nodes.sort((a, b) => {
          if (a.type !== b.type) {
            return a.type === 'folder' ? -1 : 1
          }
          return String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN')
        })
        nodes.forEach(item => {
          if (Array.isArray(item.children) && item.children.length) {
            sortNodes(item.children)
          }
        })
      }

      sortNodes(root)
      return root
    },

    async loadReadme(files) {
      const list = (files || [])
        .map(file => {
          const rawPath = String(file.relativePath || file.fileName || file.path || '').replace(/\\/g, '/')
          const lowerPath = rawPath.toLowerCase()
          return {
            file,
            rawPath,
            lowerPath,
            depth: rawPath ? rawPath.split('/').length - 1 : 0
          }
        })
        .filter(item => {
          return item.lowerPath === 'readme.md'
            || item.lowerPath === 'readme.txt'
            || item.lowerPath.endsWith('/readme.md')
            || item.lowerPath.endsWith('/readme.txt')
        })
        .sort((a, b) => {
          const aRoot = a.lowerPath === 'readme.md' || a.lowerPath === 'readme.txt' ? 0 : 1
          const bRoot = b.lowerPath === 'readme.md' || b.lowerPath === 'readme.txt' ? 0 : 1
          if (aRoot !== bRoot) return aRoot - bRoot
          if (a.depth !== b.depth) return a.depth - b.depth
          return a.rawPath.localeCompare(b.rawPath, 'zh-CN')
        })

      const readmeFile = list.length ? list[0].file : null

      if (!readmeFile) {
        this.project.readme = ''
        return
      }

      try {
        const blob = await previewProjectFile(readmeFile.id)
        this.project.readme = await safeReadBlobText(blob)
      } catch (error) {
        console.error(error)
        this.project.readme = ''
      }
    },

    async handleDownloadCurrentFile() {
      if (!this.currentFile || !this.currentFile.id) return
      const blob = await downloadFile(this.currentFile.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = this.currentFile.fileName || 'file'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    },

    async handleFileClick(node) {
      if (!node || !node.id) return

      try {
        this.fileLoading = true
        this.syncCurrentTreeNode(node)
        this.clearPreviewBlobUrl()

        const [blob, versionRes] = await Promise.all([
          previewProjectFile(node.id),
          listFileVersions(node.id).catch(() => ({ data: [] }))
        ])

        const versionData = extractApiData(versionRes)
        const versions = Array.isArray(versionData) ? versionData : []

        this.currentFile = {
          ...this.buildEmptyCurrentFile(),
          ...node,
          versions,
          previewLoading: true
        }

        const previewPayload = await this.resolveFilePreview(this.currentFile, blob)

        this.currentFile = {
          ...this.buildEmptyCurrentFile(),
          ...node,
          versions,
          blob,
          previewLoading: false,
          ...previewPayload
        }
      } catch (error) {
        console.error(error)
        this.currentFile = {
          ...this.buildEmptyCurrentFile(),
          ...node,
          previewLoading: false,
          previewError: error?.message || '读取文件失败'
        }
        this.$message.error(error?.response?.data?.message || error?.message || '读取文件失败')
      } finally {
        this.fileLoading = false
      }
    },

    async selectFile(node) {
      if (!node) return
      if (node.type === 'file' && node.id) {
        await this.handleFileClick(node)
        return
      }

      const firstFile = this.findFirstFileNode(Array.isArray(node.children) ? node.children : [])
      if (firstFile) {
        await this.handleFileClick(firstFile)
      }
    },

    findFirstFileNode(nodes = []) {
      for (const item of nodes) {
        if (!item) continue
        if (item.type === 'file' && item.id) {
          return item
        }
        if (Array.isArray(item.children) && item.children.length) {
          const matched = this.findFirstFileNode(item.children)
          if (matched) {
            return matched
          }
        }
      }
      return null
    },

    syncCurrentTreeNode(node) {
      if (!node || !node.path) return
      this.$nextTick(() => {
        const tree = this.$refs.fileTreeRef
        if (tree && typeof tree.setCurrentKey === 'function') {
          tree.setCurrentKey(node.path)
        }
      })
    },

    async jumpPreviewFile(offset) {
      if (!offset || !this.currentFile.id) return
      const flatList = this.flattenFileTree(this.fileTree)
      const currentIndex = flatList.findIndex(item => String(item.id) === String(this.currentFile.id))
      if (currentIndex < 0) return
      const target = flatList[currentIndex + offset]
      if (!target) return
      await this.handleFileClick(target)
      if (this.previewFullscreen) {
        this.focusPreviewPanel()
      }
    },

    async goPrevPreviewFile() {
      if (!this.hasPrevPreviewFile) return
      await this.jumpPreviewFile(-1)
    },

    async goNextPreviewFile() {
      if (!this.hasNextPreviewFile) return
      await this.jumpPreviewFile(1)
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
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可编辑项目信息')
        return
      }
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
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可管理项目文件')
        return
      }
      if (isVersion && !this.currentFile.id) {
        this.$message.warning('请先选择一个文件')
        return
      }
      this.uploadDialog = {
        visible: true,
        isVersion,
        version: isVersion ? this.getNextAvailableVersion() : '1.0.0',
        commitMessage: '',
        isMain: false,
        file: null,
        files: []
      }
      this.$nextTick(() => {
        if (this.$refs.uploadInput) this.$refs.uploadInput.value = ''
      })
    },

    getNextAvailableVersion() {
      const current = String(this.currentFile.version || '').trim()
      const existing = new Set(
        (Array.isArray(this.currentFile.versions) ? this.currentFile.versions : [])
          .map(item => String(item && item.version ? item.version : '').trim())
          .filter(Boolean)
      )

      let candidate = this.incrementSemanticVersion(current || '1.0.0')
      let guard = 0
      while (existing.has(candidate) && guard < 100) {
        candidate = this.incrementSemanticVersion(candidate)
        guard += 1
      }
      return candidate
    },

    incrementSemanticVersion(version) {
      const source = String(version || '').trim()
      if (!source) return '1.0.0'
      const parts = source.split('.')
      const last = Number(parts[parts.length - 1])
      if (Number.isNaN(last)) {
        return `${source}.1`
      }
      parts[parts.length - 1] = String(last + 1)
      return parts.join('.')
    },

    closeUploadDialog() {
      this.uploadDialog.visible = false
      this.uploadDialog.file = null
      this.uploadDialog.files = []
      if (this.$refs.uploadInput) this.$refs.uploadInput.value = ''
    },

    handlePickedFile(event) {
      const files = Array.from((event.target && event.target.files) || [])
        .filter(Boolean)
        .map(file => (file && file.raw ? file.raw : file))
        .filter(file => file instanceof File || file instanceof Blob)

      this.uploadDialog.files = files
      this.uploadDialog.file = files[0] || null
    },

    async submitUpload() {
      const pickedFiles = Array.isArray(this.uploadDialog.files) ? this.uploadDialog.files : []

      const getRawFile = (file) => {
        const raw = file && file.raw ? file.raw : file
        return raw instanceof File || raw instanceof Blob ? raw : null
      }

      if (this.uploadDialog.isVersion) {
        if (!getRawFile(this.uploadDialog.file)) {
          this.$message.warning('请选择文件')
          return
        }
      } else if (!pickedFiles.length) {
        this.$message.warning('请选择文件')
        return
      }

      this.uploadLoading = true
      const previousCurrentFileId = this.currentFile.id

      try {
        if (this.uploadDialog.isVersion) {
          const rawFile = getRawFile(this.uploadDialog.file)
          const formData = new FormData()
          formData.append('file', rawFile)
          formData.append('version', this.uploadDialog.version || '1.0.0')
          formData.append('commitMessage', this.uploadDialog.commitMessage || '前端上传新版本')
          await uploadFileNewVersion(this.currentFile.id, formData)
        } else if (pickedFiles.length === 1) {
          const rawFile = getRawFile(pickedFiles[0])
          if (!rawFile) {
            this.$message.error('所选文件无效，请重新选择')
            return
          }

          const version = this.uploadDialog.version || '1.0.0'
          const commitMessage = this.uploadDialog.commitMessage || '前端上传文件'
          const isZipFile = /\.zip$/i.test(rawFile.name || '')

          if (isZipFile) {
            await uploadProjectZip(this.projectId, rawFile, {
              version,
              commitMessage
            })
          } else {
            const formData = new FormData()
            formData.append('projectId', String(this.projectId))
            formData.append('file', rawFile)
            formData.append('version', version)
            formData.append('commitMessage', commitMessage)
            formData.append('isMain', this.uploadDialog.isMain ? 'true' : 'false')
            await uploadProjectFile(this.projectId, formData)
          }
        } else {
          const normalizedFiles = pickedFiles
            .map(getRawFile)
            .filter(Boolean)

          if (!normalizedFiles.length) {
            this.$message.error('所选文件无效，请重新选择')
            return
          }

          const formData = new FormData()
          formData.append('projectId', String(this.projectId))
          formData.append('version', this.uploadDialog.version || '1.0.0')
          formData.append('commitMessage', this.uploadDialog.commitMessage || '前端批量上传文件')
          if (this.uploadDialog.isMain) {
            formData.append('mainFileIndex', '0')
          }
          normalizedFiles.forEach(file => formData.append('files', file))
          await this.uploadBatchFiles(formData)
          this.selectedFileIds = []
        }

        this.$message.success(this.uploadDialog.isVersion ? '新版本上传成功' : '文件上传成功')
        this.closeUploadDialog()
        await this.fetchFiles()

        if (previousCurrentFileId) {
          const flatList = this.flattenFileTree(this.fileTree)
          const selected = flatList.find(item => item.id === previousCurrentFileId)
          if (selected) {
            await this.selectFile(selected)
          } else if (this.fileTree.length) {
            await this.selectFile(this.fileTree[0])
          }
        } else if (this.fileTree.length) {
          await this.selectFile(this.fileTree[0])
        }
      } catch (e) {
        const message = e?.response?.data?.message || e?.response?.data?.msg || e?.message || '上传失败'
        this.$message.error(message)
      } finally {
        this.uploadLoading = false
      }
    },

    async uploadBatchFiles(formData) {
      return request({
        url: '/project/file/upload/batch',
        method: 'post',
        data: formData,
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    },

    async downloadBatchFiles(projectId, fileIds = []) {
      return request({
        url: '/project/file/download/batch',
        method: 'post',
        data: { projectId, fileIds },
        responseType: 'blob'
      })
    },



    async markMainFile() {
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可设置主文件')
        return
      }
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
      if (!this.canManageProject) {
        this.$message.warning('仅项目所有者或管理员可删除文件')
        return
      }
      if (!this.currentFile.id) return
      try {
        await this.$confirm(`确认删除文件“${this.currentFile.name}”吗？`, '提示', {
          type: 'warning'
        })
        await deleteFile(this.currentFile.id)
        this.$message.success('文件删除成功')
        this.previewFullscreen = false
        this.syncPreviewFullscreenBody(false)
        this.clearPreviewBlobUrl()
        this.currentFile = this.buildEmptyCurrentFile()
        await this.fetchFiles()
      } catch (error) {
        if (error !== 'cancel') {
          console.error(error)
          this.$message.error(error.response?.data?.message || '删除文件失败')
        }
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

    async copyCurrentFileContent() {
      if (!process.client || !this.currentFile.content) return
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(this.currentFile.content)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = this.currentFile.content
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.focus()
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        this.$message.success('文件内容已复制')
      } catch (error) {
        console.error(error)
        this.$message.error('复制失败')
      }
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

    syncSelectedFileIds() {
      const availableIds = new Set(this.flattenFileTree(this.fileTree).map(item => item.id))
      this.selectedFileIds = this.selectedFileIds.filter(id => availableIds.has(id))
    },

    isFileChecked(fileId) {
      return this.selectedFileIds.includes(fileId)
    },

    toggleFileSelection(node, checked) {
      if (!node || node.type !== 'file' || !node.id) return
      const exists = this.selectedFileIds.includes(node.id)
      if (checked && !exists) {
        this.selectedFileIds = [...this.selectedFileIds, node.id]
      } else if (!checked && exists) {
        this.selectedFileIds = this.selectedFileIds.filter(id => id !== node.id)
      }
    },

    toggleSelectAllFiles() {
      if (this.isAllFilesSelected) {
        this.selectedFileIds = []
        return
      }
      this.selectedFileIds = this.flattenFileTree(this.fileTree).map(item => item.id)
    },

    clearSelectedFiles() {
      this.selectedFileIds = []
    },

    async handleBatchDownload() {
      if (!this.selectedFileIds.length) {
        this.$message.warning('请先选择文件')
        return
      }
      try {
        const blob = await this.downloadBatchFiles(this.projectId, this.selectedFileIds)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `project-${this.projectId}-files.zip`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.error(error)
        const message = error.response?.data?.message || error.response?.data || ''
        this.$message.error(message || '批量下载失败')
      }
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

    handleTaskManageClick() {
      if (!this.pageAccessResolved) return
      if (!this.canSeeTaskCollaboration) {
        this.$message.closeAll()
        this.$message.warning('只有已加入项目的成员才能查看任务协作')
        return
      }
      this.goToProjectManage('task-manage')
    },

    handleMemberManageClick() {
      if (!this.pageAccessResolved) return
      if (!this.canManageProject) {
        this.$message.closeAll()
        this.$message.warning('仅项目所有者或管理员可进入管理页面')
        return
      }
      this.goToProjectManage('member-manage')
    },

    handleProjectManageClick() {
      if (!this.pageAccessResolved) return
      if (!this.canManageProject) {
        this.$message.closeAll()
        this.$message.warning('仅项目所有者或管理员可进入管理页面')
        return
      }
      this.goToProjectManage('')
    },

    goToProjectManage(tab = '') {
      if (!this.pageAccessResolved) {
        return
      }
      if (tab === 'task-manage' && !this.canSeeTaskCollaboration) {
        return
      }
      if ((tab === 'member-manage' || !tab) && !this.canManageProject) {
        return
      }
      const query = [`projectId=${this.projectId}`]
      if (tab) {
        query.push(`tab=${tab}`)
      }
      this.$router.push(`/projectmanage?${query.join('&')}`)
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
  margin-top: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(336px, 25vw, 388px);
  gap: 22px;
  align-items: start;
}

.content-main,
.content-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.content-side {
  gap: 16px;
}

.side-card {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e8eef7;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

::v-deep(.side-card .el-card__header) {
  padding: 16px 18px 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border-bottom: 1px solid #edf2f8;
}

::v-deep(.side-card .el-card__body) {
  padding: 18px;
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

.readme-section-card {
  overflow: hidden;
}

.readme-section-header {
  gap: 16px;
  align-items: flex-start;
}

.readme-section-title {
  min-width: 0;
  flex: 1;
}

.readme-title-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
}

.readme-title-sub {
  margin-top: 8px;
  color: #7a869a;
  font-size: 13px;
  line-height: 1.7;
  max-width: 760px;
}

.readme-header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.readme-stats-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.readme-stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f6f9fc;
  border: 1px solid #e8eef7;
  color: #5b6b82;
  font-size: 12px;
  white-space: nowrap;
}

.readme-stat-chip i {
  color: #409eff;
}

.readme-stat-chip em {
  font-style: normal;
  font-weight: 700;
  color: #25364d;
}

.readme-showcase {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.readme-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 22px 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fbff 0%, #f4f8ff 42%, #eef6ff 100%);
  border: 1px solid #e4eefb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.readme-eyebrow {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.12);
  color: #3a7bd5;
  font-size: 12px;
  font-weight: 600;
}

.readme-hero-title {
  margin-top: 12px;
  font-size: 24px;
  line-height: 1.35;
  font-weight: 700;
  color: #20324a;
}

.readme-hero-desc {
  margin-top: 10px;
  color: #5e6f86;
  line-height: 1.85;
  font-size: 14px;
  max-width: 760px;
}

.readme-hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(112px, 1fr));
  gap: 12px;
  min-width: 248px;
}

.readme-hero-stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(219, 231, 245, 0.95);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.05);
}

.readme-hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #20324a;
  line-height: 1.2;
}

.readme-hero-stat-label {
  margin-top: 6px;
  color: #7a869a;
  font-size: 12px;
}

.readme-shell {
  position: relative;
  padding: 24px 26px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.05);
}

.readme-showcase.is-empty .readme-shell {
  min-height: 180px;
  display: flex;
  align-items: center;
}

.readme-box {
  min-height: 140px;
  line-height: 1.9;
  color: #4b5d73;
  white-space: normal;
  word-break: break-word;
  font-size: 15px;
}

.file-browser {
  display: flex;
  gap: 14px;
  align-items: stretch;
  min-height: 720px;
  height: min(82vh, 920px);
}

.file-tree-panel,
.file-preview-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.file-tree-panel {
  display: flex;
  flex-direction: column;
  flex: 0 0 auto;
  min-width: 280px;
  max-width: 640px;
  padding: 14px 14px 12px;
  border-radius: 16px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.file-browser-splitter {
  flex: 0 0 10px;
  position: relative;
  cursor: col-resize;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.14) 0%, rgba(148, 163, 184, 0.04) 100%);
}

.file-browser-splitter::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 52px;
  transform: translate(-50%, -50%);
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.5);
}

.file-browser-splitter:hover::before {
  background: rgba(59, 130, 246, 0.62);
}

::v-deep(.file-tree-panel .el-tree) {
  background: transparent;
  width: max-content;
  min-width: 100%;
}

::v-deep(.file-tree-panel .el-tree-node__content) {
  min-height: 38px;
  padding: 0 8px 0 4px;
  border-radius: 10px;
  margin: 4px 0;
  transition: background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

::v-deep(.file-tree-panel .el-tree-node__content:hover) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.10) 0%, rgba(103, 194, 58, 0.08) 100%);
}

::v-deep(.file-tree-panel .el-tree-node.is-current > .el-tree-node__content) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.16) 0%, rgba(103, 194, 58, 0.10) 100%);
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.16);
}

::v-deep(.file-tree-panel .el-tree-node__expand-icon) {
  color: #7b8ba7;
  padding: 6px;
}

::v-deep(.file-tree-panel .el-tree-node__children) {
  padding-left: 10px;
}

.tree-wrap {
  margin-top: 14px;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 6px;
  padding-bottom: 6px;
}

.tree-wrap::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.tree-wrap::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 999px;
}

.tree-wrap::-webkit-scrollbar-track {
  background: rgba(241, 245, 249, 0.9);
  border-radius: 999px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: max-content;
  min-width: 100%;
  padding-right: 10px;
}

.tree-node i {
  font-size: 14px;
  color: #6d7d96;
}

.tree-node-name {
  white-space: nowrap;
  font-size: 13px;
  color: #334155;
}

.main-file-badge {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.14);
}

.file-preview-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  position: relative;
}

.file-preview-panel.is-fullscreen {
  position: fixed;
  inset: 16px;
  width: auto;
  height: auto;
  min-height: auto;
  max-height: none;
  z-index: 3000;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 24px 72px rgba(15, 23, 42, 0.24);
}

.file-preview-toolbar {
  padding: 18px 20px 16px;
  border-bottom: 1px solid #eef3f9;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 14px 18px;
  align-items: flex-start;
  background:
    radial-gradient(circle at top right, rgba(64, 158, 255, 0.14), transparent 32%),
    linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
}

.file-preview-title-wrap {
  min-width: 0;
  flex: 1 1 360px;
}

.file-preview-title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.preview-index-badge {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.16) 0%, rgba(103, 194, 58, 0.16) 100%);
  color: #2563eb;
  border: 1px solid rgba(37, 99, 235, 0.14);
  font-size: 12px;
  font-weight: 700;
}

.file-preview-title-group {
  min-width: 0;
  flex: 1 1 auto;
}

.file-preview-title {
  font-weight: 700;
  font-size: 20px;
  line-height: 1.4;
  color: #1f2937;
  word-break: normal;
  overflow-wrap: anywhere;
  white-space: normal;
}

.file-preview-subtitle {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.7;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.file-preview-toolbar-tip {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  color: #64748b;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid #e8eef7;
  font-size: 12px;
  max-width: 100%;
}

.file-preview-toolbar-actions {
  display: flex;
  flex: 1 1 100%;
  width: 100%;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.file-preview-switchers {
  display: flex;
  justify-content: flex-start;
  width: auto;
  flex: 0 0 auto;
}

.file-preview-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  flex: 1 1 auto;
  min-width: 0;
}

.file-preview-meta {
  padding: 12px 16px 0;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.preview-meta-left,
.preview-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-meta-right {
  justify-content: flex-end;
  flex: 1;
}

.preview-view-tools {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-right: 6px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f4f7fb;
  border: 1px solid #e8eef7;
  color: #607089;
  font-size: 12px;
  line-height: 1;
}

.meta-pill-lang {
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: #fff;
  border: none;
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.18);
}

.rich-preview-shell,
.text-preview-shell,
.media-preview-shell,
.table-preview-shell,
.office-preview-shell {
  margin: 12px 14px 14px;
  flex: 1;
  min-height: 0;
  max-height: calc(100% - 142px);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e8edf5;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.file-preview-panel.is-fullscreen .rich-preview-shell,
.file-preview-panel.is-fullscreen .text-preview-shell,
.file-preview-panel.is-fullscreen .media-preview-shell,
.file-preview-panel.is-fullscreen .table-preview-shell,
.file-preview-panel.is-fullscreen .office-preview-shell,
.file-preview-panel.is-fullscreen .code-preview-shell {
  margin: 14px 18px 18px;
}

.rich-preview-body {
  height: 100%;
  overflow: auto;
  padding: 22px 24px 24px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  font-size: 15px;
  line-height: 1.9;
}

.text-preview-shell {
  background: linear-gradient(180deg, #f8fafc 0%, #eef3f8 100%);
}

.plain-text-preview {
  margin: 0;
  padding: 22px 24px 24px;
  height: 100%;
  overflow: auto;
  white-space: pre;
  word-break: normal;
  line-height: 1.8;
  color: #334155;
  font-size: var(--preview-font-size, 14px);
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
}

.plain-text-preview.is-wrap {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.media-preview-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
}

.image-preview-shell {
  overflow: auto;
}

.image-preview-element {
  display: block;
  max-width: 100%;
  max-height: 560px;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.14);
  background: #fff;
}

.pdf-preview-shell {
  padding: 0;
  background: #eef2f7;
}

.pdf-preview-frame {
  width: 100%;
  min-height: 600px;
  height: 100%;
  background: #fff;
}

.video-preview-element {
  width: 100%;
  max-height: 560px;
  border-radius: 12px;
  background: #000;
}

.audio-preview-shell {
  gap: 14px;
}

.audio-preview-element {
  width: min(620px, 100%);
}

.media-preview-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.media-preview-tip {
  font-size: 13px;
  color: #7b8798;
}

.table-preview-shell,
.spreadsheet-preview-shell,
.office-preview-shell {
  display: flex;
  flex-direction: column;
}

.table-preview-wrap {
  overflow: auto;
}

.preview-table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
}

.preview-table th,
.preview-table td {
  border: 1px solid #e8edf5;
  padding: 10px 12px;
  text-align: left;
  vertical-align: top;
  font-size: 13px;
  color: #334155;
}

.preview-table th {
  background: #f4f7fb;
  font-weight: 700;
  color: #1f2d3d;
}

.table-preview-shell .table-preview-wrap,
.spreadsheet-preview-shell .table-preview-wrap {
  padding: 16px;
}

.office-preview-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid #edf1f7;
  background: linear-gradient(180deg, #fbfdff 0%, #f4f7fb 100%);
}

.office-preview-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.office-preview-subtitle {
  font-size: 13px;
  color: #6b7280;
}

.office-document-body {
  padding: 20px 22px;
  overflow: auto;
  line-height: 1.95;
  color: #374151;
}

.office-document-body p {
  margin: 0 0 14px;
  text-indent: 2em;
}

.sheet-preview-list,
.ppt-slide-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  overflow: auto;
}

.sheet-preview-card,
.ppt-slide-card {
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.sheet-preview-name,
.ppt-slide-title {
  padding: 12px 14px;
  border-bottom: 1px solid #edf1f7;
  font-weight: 700;
  color: #1f2d3d;
  background: #f8fafc;
}

.ppt-slide-lines {
  padding: 14px 16px 16px;
}

.ppt-slide-lines p {
  margin: 0 0 10px;
  line-height: 1.8;
  color: #475569;
}

.unsupported-preview {
  flex-direction: column;
  gap: 10px;
  text-align: center;
  padding: 24px;
}

.unsupported-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2d3d;
}

.unsupported-desc {
  max-width: 520px;
  line-height: 1.8;
  color: #6b7280;
}

.preview-loading-state {
  flex-direction: column;
  gap: 10px;
  font-size: 14px;
}

.preview-loading-state i {
  font-size: 24px;
  color: #409eff;
}

.code-preview-shell {
  margin: 12px 16px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: calc(100% - 142px);
  height: auto;
  border-radius: 18px;
  overflow: hidden;
  background: radial-gradient(circle at top left, rgba(56, 189, 248, 0.08), transparent 28%), linear-gradient(180deg, #081120 0%, #0a1526 52%, #07101d 100%);
  box-shadow: 0 20px 48px rgba(2, 6, 23, 0.28);
  border: 1px solid rgba(96, 165, 250, 0.14);
}

.code-preview-header {
  min-height: 50px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, rgba(255, 255, 255, 0.02) 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.code-header-left,
.code-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.code-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.code-dot-red {
  background: #ff5f57;
}

.code-dot-yellow {
  background: #febc2e;
}

.code-dot-green {
  background: #28c840;
}

.code-file-name {
  margin-left: 6px;
  color: #dbe7ff;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-extension-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.16);
  border: 1px solid rgba(96, 165, 250, 0.28);
  color: #bfdbfe;
  font-size: 12px;
}

.code-container {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.96) 0 64px, rgba(8, 17, 32, 0.96) 64px);
}

.line-numbers {
  width: 64px;
  background: rgba(2, 6, 23, 0.78);
  border-right: 1px solid rgba(148, 163, 184, 0.14);
  text-align: right;
  user-select: none;
  padding: 16px 0 24px;
  overflow: auto;
  scrollbar-width: none;
}

.line-numbers::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.line-number {
  min-height: 1.75em;
  padding: 0 14px 0 8px;
  font-size: calc(var(--preview-font-size, 14px) - 1px);
  color: #6f86a8;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  line-height: 1.75;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.code-content {
  margin: 0;
  padding: 16px 20px 24px;
  white-space: pre;
  word-break: normal;
  overflow: auto;
  flex: 1;
  background: linear-gradient(180deg, rgba(7, 16, 29, 0.72) 0%, rgba(8, 17, 32, 0.94) 100%);
  color: #e5eefc;
  font-size: var(--preview-font-size, 14px);
  line-height: 1.75;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  tab-size: 2;
}

.code-content.is-wrap {
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.code-content::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

.code-content::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.28);
  border-radius: 999px;
}

.code-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.code-content pre {
  margin: 0;
  padding: 0;
}

.code-content code {
  display: block;
  min-width: 100%;
  font-size: inherit;
  line-height: inherit;
  background: transparent !important;
  color: inherit;
  padding: 0;
}

.code-content code.hljs {
  background: transparent !important;
}

.empty-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
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


.side-task-header-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.side-task-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-task-summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
}

.side-task-summary.danger {
  background: #fff5f5;
  border-color: #fde2e2;
}

.side-task-summary-value {
  font-size: 22px;
  line-height: 1;
  font-weight: 700;
  color: #303133;
}

.side-task-summary-label {
  font-size: 12px;
  color: #909399;
}

.side-task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.side-task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  background: #fff;
}

.side-task-item.is-overdue,
.side-task-item-due {
  border-color: #f7c7c7;
  background: #fff8f8;
}

.side-task-main {
  flex: 1;
  min-width: 0;
}

.side-task-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.side-task-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}

.side-task-status-select {
  width: 104px;
  flex-shrink: 0;
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
.ai-rich-content .markdown-table-wrap,
.ai-rich-content .markdown-code-block,
.ai-rich-content .markdown-image-only {
  margin: 0 0 16px;
}

.ai-rich-content p:last-child,
.ai-rich-content ul:last-child,
.ai-rich-content ol:last-child,
.ai-rich-content blockquote:last-child,
.ai-rich-content pre:last-child,
.ai-rich-content .markdown-table-wrap:last-child,
.ai-rich-content .markdown-code-block:last-child,
.ai-rich-content .markdown-image-only:last-child {
  margin-bottom: 0;
}

.ai-rich-content h1,
.ai-rich-content h2,
.ai-rich-content h3,
.ai-rich-content h4,
.ai-rich-content h5,
.ai-rich-content h6 {
  margin: 24px 0 12px;
  line-height: 1.45;
  color: #1f2d3d;
  font-weight: 700;
}

.ai-rich-content h1:first-child,
.ai-rich-content h2:first-child,
.ai-rich-content h3:first-child,
.ai-rich-content h4:first-child,
.ai-rich-content h5:first-child,
.ai-rich-content h6:first-child {
  margin-top: 0;
}

.ai-rich-content h1 { font-size: 30px; }
.ai-rich-content h2 { font-size: 26px; }
.ai-rich-content h3 { font-size: 22px; }
.ai-rich-content h4 { font-size: 19px; }
.ai-rich-content h5 { font-size: 17px; }
.ai-rich-content h6 { font-size: 15px; }

.ai-rich-content ul,
.ai-rich-content ol {
  padding-left: 24px;
}

.ai-rich-content li + li {
  margin-top: 8px;
}

.ai-rich-content hr {
  border: none;
  border-top: 1px solid #e8eef7;
  margin: 22px 0;
}

.ai-rich-content blockquote {
  padding: 14px 16px;
  border-left: 4px solid #8bbdff;
  background: linear-gradient(180deg, #f7fbff 0%, #f3f8fd 100%);
  color: #5d6f88;
  border-radius: 12px;
}

.ai-rich-content a {
  color: #1d6fdc;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 1px solid rgba(29, 111, 220, 0.2);
}

.ai-rich-content a:hover {
  color: #409eff;
  border-bottom-color: rgba(64, 158, 255, 0.38);
}

.ai-rich-content code {
  padding: 2px 7px;
  border-radius: 6px;
  background: #f3f6fb;
  font-size: 13px;
  color: #cc4b37;
  border: 1px solid #e6edf6;
}

.ai-rich-content del {
  color: #8b98ab;
}

.ai-rich-content .markdown-code-block {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(96, 165, 250, 0.12);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.ai-rich-content .markdown-code-head {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 36px;
  padding: 0 14px;
  background: linear-gradient(180deg, #1f2a3d 0%, #162132 100%);
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.ai-rich-content .markdown-code-lang {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.16);
  color: #dbeafe;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.ai-rich-content pre {
  margin: 0;
  padding: 16px 18px;
  border-radius: 0;
  background: linear-gradient(180deg, #101827 0%, #0b1320 100%);
  color: #f8fbff;
  overflow: auto;
}

.ai-rich-content pre code {
  padding: 0;
  background: transparent;
  color: inherit;
  border: none;
}

.ai-rich-content .markdown-image-only {
  display: flex;
  justify-content: center;
}

.ai-rich-content .markdown-image-wrap {
  display: inline-flex;
  max-width: 100%;
}

.ai-rich-content .markdown-image-wrap img {
  max-width: 100%;
  max-height: 520px;
  border-radius: 16px;
  border: 1px solid #e7edf5;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
  object-fit: contain;
  background: #fff;
}

.ai-rich-content .markdown-image-alt {
  color: #909399;
  font-size: 13px;
}

.ai-rich-content .markdown-task-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  list-style: none;
  margin-left: -18px;
}

.ai-rich-content .markdown-task-box {
  flex: 0 0 18px;
  width: 18px;
  height: 18px;
  margin-top: 3px;
  border-radius: 6px;
  border: 1px solid #c8d7ea;
  background: #fff;
  color: transparent;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ai-rich-content .markdown-task-item.is-checked .markdown-task-box {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.ai-rich-content .markdown-table-wrap {
  width: 100%;
  overflow-x: auto;
  border: 1px solid #e8eef7;
  border-radius: 14px;
  background: #fff;
}

.ai-rich-content table {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
}

.ai-rich-content th,
.ai-rich-content td {
  border: 1px solid #edf2f8;
  padding: 12px 14px;
  text-align: left;
  vertical-align: top;
}

.ai-rich-content th {
  background: #f7fafe;
  font-weight: 700;
  color: #20324a;
}

.ai-rich-content strong {
  font-weight: 700;
  color: #1f2d3d;
}

.ai-rich-content em {
  font-style: italic;
}

@media (max-width: 1100px) {
  .task-board-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .task-board-grid {
    grid-template-columns: 1fr;
  }

  .readme-section-header,
  .readme-header-right {
    flex-direction: column;
    align-items: flex-start;
  }

  .readme-stats-inline {
    justify-content: flex-start;
  }

  .readme-hero {
    grid-template-columns: 1fr;
  }

  .readme-hero-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    min-width: 0;
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .file-browser {
    grid-template-columns: 1fr;
  }

  .content-side {
    gap: 14px;
  }
}


@media (max-width: 1360px) {
  .file-preview-toolbar-actions {
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
  }

  .file-preview-switchers,
  .file-preview-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .file-preview-title {
    font-size: 18px;
  }
}

@media (max-width: 768px) {
  .readme-shell {
    padding: 18px 16px;
  }

  .readme-hero {
    padding: 18px 16px;
  }

  .readme-title-main {
    flex-wrap: wrap;
  }

  .readme-hero-title {
    font-size: 20px;
  }

  .readme-hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-header,
  .title-row,
  .meta-row,
  .task-compact-item,
  .task-compact-top {
    flex-direction: column;
    align-items: stretch;
  }

  .task-board-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-compact-side .el-select,
  .task-compact-side .el-button {
    width: 100%;
  }
  .file-preview-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .file-preview-toolbar-actions {
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
  }

  .file-preview-switchers,
  .file-preview-actions,
  .preview-meta-right {
    justify-content: flex-start;
    align-items: flex-start;
    width: 100%;
  }

  .file-preview-title {
    font-size: 18px;
  }

  .file-preview-title-row {
    align-items: flex-start;
  }

  .preview-index-badge {
    min-width: 48px;
    padding: 0 10px;
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
.file-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tree-selection-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin-top: 10px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #f8fafc;
  color: #606266;
  font-size: 13px;
}

.tree-selection-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-node-checkbox {
  margin-right: 8px;
}

.upload-picked-tip,
.upload-picked-list {
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
}

.upload-picked-title {
  margin-bottom: 6px;
}

.upload-picked-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.upload-picked-item {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f4f4f5;
  color: #606266;
  line-height: 1.4;
}



.task-board-card {
  margin-top: 18px;
}

.task-board-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-board-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.task-mini-stat {
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  border: 1px solid #e8eef7;
}

.task-mini-stat.danger {
  background: linear-gradient(180deg, #fff7f7 0%, #fff1f0 100%);
  border-color: #ffd8d6;
}

.task-mini-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.task-mini-stat-label {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.task-board-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.task-compact-panel {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fff;
  padding: 14px;
  min-height: 240px;
}

.task-compact-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
}

.task-compact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-compact-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  background: #fbfcff;
}

.task-compact-item.is-overdue {
  border-color: #ffd9d4;
  background: #fff7f6;
}

.task-compact-main {
  min-width: 0;
  flex: 1;
}

.task-compact-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.task-compact-title {
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.task-compact-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.task-compact-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  flex-wrap: wrap;
}

.task-assignee-inline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.task-compact-side {
  flex-shrink: 0;
}

.file-preview-title-group {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-preview-subtitle {
  font-size: 12px;
  color: #8a94a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-warning-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  color: #ad6800;
  font-size: 13px;
}

.ai-struct-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ai-hero-overview {
  padding: 16px 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(14, 165, 233, 0.12));
  color: #1e293b;
  font-size: 16px;
  line-height: 1.7;
  font-weight: 600;
}
.ai-struct-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}
.ai-struct-item,
.ai-phase-card {
  border: 1px solid #e5edf6;
  border-radius: 14px;
  background: #fff;
  padding: 14px 16px;
}
.ai-struct-title,
.ai-phase-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 10px;
}
.ai-struct-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}
.ai-ordered-list {
  padding-left: 20px;
}
.ai-struct-list-warn {
  color: #b45309;
}
.ai-task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-task-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e5edf6;
}
.ai-task-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.ai-task-name {
  font-weight: 600;
  color: #0f172a;
}
.ai-task-meta {
  color: #475569;
  line-height: 1.7;
  font-size: 13px;
}


@media (max-width: 1280px) {
  .file-browser {
    height: auto;
    min-height: 0;
    flex-direction: column;
  }

  .file-tree-panel {
    width: 100% !important;
    max-width: none;
  }

  .file-browser-splitter {
    display: none;
  }

  .file-preview-panel {
    min-height: 720px;
  }
}



/* ===== 预览区稳定布局修复：避免标题与按钮被挤压错位 ===== */
@media (min-width: 1281px) {
  .file-tree-panel {
    max-width: min(38vw, 460px);
  }
}

.file-preview-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-auto-rows: auto;
  row-gap: 14px;
  align-items: start;
}

.file-preview-title-wrap,
.file-preview-toolbar-actions,
.file-preview-meta,
.preview-meta-left,
.preview-meta-right,
.preview-view-tools,
.preview-copy-action,
.file-preview-switchers,
.file-preview-actions {
  min-width: 0;
}

.file-preview-title-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.file-preview-title-group {
  min-width: 0;
  overflow: hidden;
}

.file-preview-title {
  width: 100%;
  white-space: nowrap !important;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: keep-all !important;
  overflow-wrap: normal !important;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
}

.file-preview-subtitle {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: normal;
  overflow-wrap: normal;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
}

.file-preview-toolbar-tip {
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-preview-toolbar-actions {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  width: 100%;
  flex: none;
}

.file-preview-switchers {
  justify-self: start;
}

.file-preview-actions {
  justify-self: end;
  justify-content: flex-end;
  flex-wrap: wrap !important;
  overflow: visible;
}

.file-preview-switchers ::v-deep(.el-button-group) {
  display: inline-flex;
  flex-wrap: nowrap;
}

.file-preview-switchers ::v-deep(.el-button),
.file-preview-actions ::v-deep(.el-button),
.preview-view-tools ::v-deep(.el-button),
.preview-copy-action ::v-deep(.el-button) {
  flex: 0 0 auto;
  height: 32px;
  white-space: nowrap;
}

.file-preview-meta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px 16px;
}

.preview-meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-meta-right {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  justify-content: end;
  gap: 10px 12px;
}

.preview-view-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-copy-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.preview-copy-action ::v-deep(.el-button) {
  min-width: 108px;
}

.preview-font-indicator {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  height: 32px;
  padding: 0 10px;
  border-radius: 10px;
  background: #f4f7fb;
  border: 1px solid #e8eef7;
  color: #607089;
  font-weight: 600;
}

.plain-text-preview,
.code-content,
.rich-preview-body,
.file-preview-title,
.file-preview-subtitle {
  writing-mode: horizontal-tb;
  text-orientation: mixed;
}

@media (max-width: 1460px) {
  .file-preview-toolbar-actions {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .file-preview-switchers,
  .file-preview-actions {
    justify-self: stretch;
    width: 100%;
  }

  .file-preview-actions {
    justify-content: flex-start;
  }

  .file-preview-meta {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .preview-meta-right {
    grid-template-columns: 1fr;
    justify-content: stretch;
  }

  .preview-view-tools,
  .preview-copy-action {
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .preview-view-tools {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(68px, max-content));
    justify-content: flex-start;
  }

  .preview-font-indicator {
    min-width: 68px;
  }
}

@media (max-width: 768px) {
  .file-preview-toolbar {
    row-gap: 12px;
  }

  .file-preview-title {
    font-size: 17px !important;
  }

  .file-preview-subtitle,
  .file-preview-toolbar-tip {
    font-size: 12px;
  }

  .file-preview-switchers ::v-deep(.el-button),
  .file-preview-actions ::v-deep(.el-button),
  .preview-view-tools ::v-deep(.el-button),
  .preview-copy-action ::v-deep(.el-button) {
    padding-left: 10px;
    padding-right: 10px;
  }

  .preview-view-tools {
    grid-template-columns: repeat(auto-fit, minmax(64px, 1fr));
  }

  .preview-copy-action,
  .preview-copy-action ::v-deep(.el-button) {
    width: 100%;
  }
}


/* === preview toolbar beautify & stable layout override === */
.file-preview-toolbar {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) !important;
  gap: 16px !important;
  padding: 22px 22px 18px !important;
}

.file-preview-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.file-preview-title-row {
  align-items: flex-start !important;
  gap: 14px !important;
}

.file-preview-title-group {
  min-width: 0;
}

.file-preview-title {
  font-size: 18px !important;
  line-height: 1.35 !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  word-break: normal !important;
}

.file-preview-subtitle {
  margin-top: 6px !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  line-height: 1.5 !important;
}

.file-preview-toolbar-tip {
  margin-top: 0 !important;
  align-self: flex-start;
  background: rgba(248, 250, 252, 0.92) !important;
}

.file-preview-toolbar-actions {
  display: flex !important;
  width: 100% !important;
  flex-wrap: wrap !important;
  justify-content: space-between !important;
  align-items: center !important;
  gap: 12px 14px !important;
}

.file-preview-switchers,
.file-preview-actions {
  display: flex !important;
  align-items: center !important;
  flex-wrap: wrap !important;
  gap: 10px !important;
  min-width: 0;
}

.file-preview-switchers {
  flex: 0 0 auto !important;
}

.file-preview-actions {
  flex: 1 1 auto !important;
  justify-content: flex-end !important;
}

.file-preview-switchers ::v-deep(.el-button-group) {
  display: inline-flex !important;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}

.file-preview-switchers ::v-deep(.el-button),
.file-preview-actions ::v-deep(.el-button),
.preview-view-tools ::v-deep(.el-button),
.preview-copy-action ::v-deep(.el-button) {
  min-width: 86px;
  height: 34px !important;
  border-radius: 12px !important;
  white-space: nowrap !important;
}

.file-preview-meta {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) auto !important;
  align-items: center !important;
  gap: 14px 16px !important;
  padding: 14px 20px 0 !important;
}

.preview-meta-left {
  min-width: 0;
  display: flex !important;
  align-items: center !important;
  flex-wrap: wrap !important;
  gap: 10px !important;
}

.preview-meta-right {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
  flex-wrap: wrap !important;
  gap: 10px !important;
  min-width: 0;
}

.meta-pill {
  flex: 0 0 auto !important;
  min-width: max-content;
  white-space: nowrap !important;
  word-break: keep-all !important;
  border-radius: 999px !important;
}

.preview-view-tools {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
  flex-wrap: wrap !important;
  gap: 8px !important;
  padding: 8px 10px;
  background: #fbfcfe;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  margin-right: 0 !important;
}

.preview-font-indicator {
  min-width: 72px !important;
  height: 34px !important;
  border-radius: 12px !important;
  white-space: nowrap !important;
}

.preview-copy-action {
  display: flex !important;
  align-items: center !important;
  flex: 0 0 auto !important;
}

.preview-copy-action ::v-deep(.el-button) {
  min-width: 114px !important;
}

@media (max-width: 1280px) {
  .file-preview-toolbar-actions {
    justify-content: flex-start !important;
  }

  .file-preview-actions {
    flex: 0 1 auto !important;
    justify-content: flex-start !important;
  }

  .file-preview-meta {
    grid-template-columns: 1fr !important;
  }

  .preview-meta-right,
  .preview-view-tools {
    justify-content: flex-start !important;
  }
}

@media (max-width: 860px) {
  .file-preview-title {
    white-space: normal !important;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden !important;
  }

  .file-preview-subtitle {
    white-space: normal !important;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden !important;
  }

  .file-preview-switchers,
  .file-preview-actions,
  .preview-meta-right,
  .preview-copy-action {
    width: 100%;
    justify-content: flex-start !important;
  }
}

@media (max-width: 640px) {
  .preview-view-tools {
    width: 100%;
    justify-content: flex-start !important;
  }

  .preview-copy-action ::v-deep(.el-button) {
    width: 100%;
  }
}


/* === preview meta left-side visibility fix === */
.file-preview-meta {
  grid-template-columns: minmax(0, 1fr) !important;
  grid-template-areas:
    "left"
    "right" !important;
  align-items: start !important;
  gap: 12px !important;
}

.preview-meta-left {
  grid-area: left;
  width: 100%;
  justify-content: flex-start !important;
  align-items: center !important;
  row-gap: 10px !important;
  column-gap: 10px !important;
}

.preview-meta-right {
  grid-area: right;
  width: 100%;
  justify-content: flex-end !important;
  gap: 10px 12px !important;
}

.preview-view-tools {
  max-width: 100%;
}

.preview-copy-action {
  flex: 0 0 auto !important;
}

@media (max-width: 900px) {
  .preview-meta-right {
    justify-content: flex-start !important;
  }

  .preview-copy-action,
  .preview-copy-action ::v-deep(.el-button) {
    width: auto;
  }
}

</style>
