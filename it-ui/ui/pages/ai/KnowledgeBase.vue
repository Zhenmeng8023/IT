<template>
  <div class="kb-page">
    <el-card shadow="never" class="kb-card">
      <div class="page-toolbar">
        <div class="page-toolbar__left">
          <el-radio-group v-model="listMode" size="small" @change="handleListModeChange">
            <el-radio-button label="owner">我的知识库</el-radio-button>
            <el-radio-button label="project">项目知识库</el-radio-button>
          </el-radio-group>

          <el-input-number
            v-if="listMode === 'owner'"
            v-model="ownerId"
            :min="1"
            controls-position="right"
            size="small"
          />

          <el-input-number
            v-else
            v-model="projectId"
            :min="1"
            controls-position="right"
            size="small"
          />

          <el-input
            v-model.trim="keyword"
            size="small"
            clearable
            placeholder="搜索知识库名称/描述"
            style="width: 240px"
          />
        </div>

        <div class="page-toolbar__right">
          <el-button size="small" @click="loadKnowledgeBases">刷新</el-button>
          <el-button v-if="canCreateKnowledgeBase" type="primary" size="small" @click="openKbDialog('create')">新建知识库</el-button>
        </div>
      </div>

      <div class="kb-layout">
        <div class="kb-sidebar">
          <div class="kb-sidebar__title">知识库列表</div>

          <div v-loading="loading.kbList" class="kb-list">
            <el-empty
              v-if="!filteredKnowledgeBases.length"
              description="暂无知识库"
              :image-size="72"
            />

            <div
              v-for="item in filteredKnowledgeBases"
              :key="item.id"
              class="kb-list-item"
              :class="{ active: currentKnowledgeBase && currentKnowledgeBase.id === item.id }"
              @click="selectKnowledgeBase(item)"
            >
              <div class="kb-list-item__top">
                <div class="kb-list-item__name">{{ item.name || `知识库 #${item.id}` }}</div>
                <el-tag size="mini" :type="kbStatusTagType(item.status)">{{ item.status || 'UNKNOWN' }}</el-tag>
              </div>

              <div class="kb-list-item__desc">{{ item.description || '暂无描述' }}</div>

              <div class="kb-list-item__meta">
                <span>ID {{ item.id }}</span>
                <span>{{ item.scopeType || '-' }}</span>
                <span>{{ item.visibility || '-' }}</span>
              </div>

              <div class="kb-list-item__actions">
                <el-button v-if="canEditKnowledgeBaseItem(item)" type="text" size="mini" @click.stop="openKbDialog('edit', item)">编辑</el-button>
                <el-button v-if="canEditKnowledgeBaseItem(item)" type="text" size="mini" @click.stop="reindexKnowledgeBase(item)">重建索引</el-button>
              </div>
            </div>
          </div>

          <div class="table-pagination">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="pagination.page + 1"
              :page-size="pagination.size"
              :total="pagination.total"
              @current-change="handleKbPageChange"
            />
          </div>
        </div>

        <div class="kb-main">
          <el-empty
            v-if="!currentKnowledgeBase"
            description="请选择左侧知识库"
            :image-size="96"
          />

          <template v-else>
            <div class="kb-main__header">
              <div>
                <div class="kb-main__title">{{ currentKnowledgeBase.name || `知识库 #${currentKnowledgeBase.id}` }}</div>
                <div class="kb-main__subtitle">
                  {{ currentKnowledgeBase.description || '这个知识库还没有描述' }}
                </div>
              </div>

              <div class="kb-main__header-actions">
                <el-button v-if="canEditCurrentKnowledgeBase" size="small" @click="openKbDialog('edit', currentKnowledgeBase)">编辑知识库</el-button>
                <el-button size="small" @click="openKnowledgeBaseTasks">索引任务</el-button>
                <el-button v-if="canEditCurrentKnowledgeBase" type="primary" size="small" @click="reindexKnowledgeBase(currentKnowledgeBase)">重建索引</el-button>
              </div>
            </div>

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="拥有者">{{ currentKnowledgeBase.ownerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="作用域">{{ currentKnowledgeBase.scopeType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="项目 ID">{{ currentKnowledgeBase.projectId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ currentKnowledgeBase.sourceType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ currentKnowledgeBase.visibility || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Provider">{{ currentKnowledgeBase.embeddingProvider || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Model">{{ currentKnowledgeBase.embeddingModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分块策略">{{ currentKnowledgeBase.chunkStrategy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认 TopK">{{ currentKnowledgeBase.defaultTopK || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认问答模型">{{ currentKnowledgeBaseDefaultModelName }}</el-descriptions-item>
              <el-descriptions-item label="最后索引时间">{{ formatTime(currentKnowledgeBase.lastIndexedAt) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ currentKnowledgeBase.status || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-tabs v-model="activeTab" class="kb-tabs">
              <el-tab-pane label="文档管理" name="documents">
                <div class="tab-toolbar">
                  <div class="tab-toolbar__left">
                    <el-button v-if="canEditCurrentKnowledgeBase" type="primary" size="small" @click="openDocumentDialog">新增文档</el-button>
                    <el-button v-if="canEditCurrentKnowledgeBase" size="small" @click="triggerUploadSelect">上传文件</el-button>
                    <el-button v-if="canEditCurrentKnowledgeBase" size="small" @click="triggerZipUploadSelect">ZIP导入项目</el-button>
                    <el-button v-if="canEditCurrentKnowledgeBase" size="small" @click="triggerLocalFileSelect">导入本地文本</el-button>
                    <el-button size="small" @click="loadDocuments">刷新文档</el-button>
                    <el-button size="small" @click="openKnowledgeBaseTasks">索引任务</el-button>
                    <el-button size="small" @click="downloadCurrentDocumentsZip">打包下载</el-button>

                    <input
                      ref="uploadFileInput"
                      class="hidden-file-input"
                      type="file"
                      multiple
                      accept=".txt,.md,.markdown,.json,.csv,.js,.ts,.java,.xml,.html,.htm,.css,.vue,.sql,.yml,.yaml,.pdf,.doc,.docx"
                      @change="handleUploadFileChange"
                    />

                    <input
                      ref="uploadZipInput"
                      class="hidden-file-input"
                      type="file"
                      accept=".zip,application/zip"
                      @change="handleUploadZipChange"
                    />

                    <input
                      ref="localFileInput"
                      class="hidden-file-input"
                      type="file"
                      accept=".txt,.md,.markdown,.json,.csv,.js,.ts,.java,.xml,.html,.htm,.css,.vue,.sql,.yml,.yaml"
                      @change="handleLocalFileChange"
                    />
                  </div>

                  <div class="tab-toolbar__right text-muted">
                    当前知识库文档数：{{ documentPagination.total }}
                  </div>
                </div>

                <el-table v-loading="loading.documents" :data="documents" border stripe size="small">
                  <el-table-column prop="id" label="ID" width="90" />
                  <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
                  <el-table-column prop="sourceType" label="来源类型" width="120" />
                  <el-table-column prop="fileType" label="文件类型" width="120" />
                  <el-table-column label="状态" width="120">
                    <template slot-scope="{ row }">
                      <el-tag size="mini" :type="docStatusTagType(row.status)">{{ row.status || 'UNKNOWN' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="索引时间" min-width="170">
                    <template slot-scope="{ row }">{{ formatTime(row.indexedAt) }}</template>
                  </el-table-column>
                  <el-table-column label="更新时间" min-width="170">
                    <template slot-scope="{ row }">{{ formatTime(row.updatedAt) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" min-width="360" fixed="right">
                    <template slot-scope="{ row }">
                      <el-button type="text" size="small" @click="viewChunks(row)">查看切片</el-button>
                      <el-button v-if="canEditCurrentKnowledgeBase" type="text" size="small" @click="reindexDocument(row)">重建索引</el-button>
                      <el-button type="text" size="small" @click="viewIndexTasks(row)">索引记录</el-button>
                      <el-button type="text" size="small" @click="downloadDocument(row)">下载</el-button>
                      <el-button type="text" size="small" @click="seedChatQuestion(row)">引用到提问</el-button>
                    </template>
                  </el-table-column>
                  <el-table-column label="ZIP包" min-width="180" show-overflow-tooltip>
                    <template slot-scope="{ row }">
                      {{ row.archiveName || '-' }}
                    </template>
                  </el-table-column>

                  <el-table-column label="包内路径" min-width="260" show-overflow-tooltip>
                    <template slot-scope="{ row }">
                      {{ row.archiveEntryPath || '-' }}
                    </template>
                  </el-table-column>

                  <el-table-column label="导入批次" min-width="180" show-overflow-tooltip>
                    <template slot-scope="{ row }">
                      {{ row.importBatchId || '-' }}
                    </template>
                  </el-table-column>
                </el-table>

                <div class="table-pagination">
                  <el-pagination
                    background
                    layout="prev, pager, next"
                    :current-page="documentPagination.page + 1"
                    :page-size="documentPagination.size"
                    :total="documentPagination.total"
                    @current-change="handleDocumentPageChange"
                  />
                </div>
              </el-tab-pane>

              <el-tab-pane label="成员管理" name="members">
                <div class="tab-toolbar">
                  <div class="tab-toolbar__left">
                    <el-button v-if="canManageCurrentMembers" type="primary" size="small" @click="openMemberDialog">添加成员</el-button>
                    <el-button size="small" @click="loadMembers">刷新成员</el-button>
                  </div>
                </div>

                <el-table v-loading="loading.members" :data="members" border stripe size="small">
                  <el-table-column prop="id" label="ID" width="90" />
                  <el-table-column prop="userId" label="用户 ID" width="120" />
                  <el-table-column prop="roleCode" label="角色" width="120" />
                  <el-table-column prop="grantedBy" label="授权人" width="120" />
                  <el-table-column label="创建时间" min-width="180">
                    <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="120" fixed="right">
                    <template slot-scope="{ row }">
                      <el-button
                        type="text"
                        size="small"
                        :disabled="row.roleCode === 'OWNER' || !canManageCurrentMembers"
                        @click="removeMember(row)"
                      >
                        移除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="知识库问答" name="chat">
                <div class="chat-config">
                  <el-form :inline="true" size="small">
                    <el-form-item label="当前用户">
                      <el-input :value="chatForm.userId || '-'" disabled style="width: 120px" />
                    </el-form-item>

                    <el-form-item label="问答模型">
                      <el-select
                        v-model="chatForm.modelId"
                        clearable
                        filterable
                        placeholder="优先使用知识库默认模型"
                        style="width: 240px"
                      >
                        <el-option
                          v-for="model in enabledModels"
                          :key="model.id"
                          :label="buildModelLabel(model)"
                          :value="model.id"
                        />
                      </el-select>
                    </el-form-item>

                    <el-form-item>
                      <el-button size="small" @click="useKnowledgeBaseDefaultModel">使用知识库默认模型</el-button>
                    </el-form-item>

                    <el-form-item>
                      <el-button size="small" @click="useActiveModel">使用系统当前模型</el-button>
                    </el-form-item>

                    <el-form-item>
                      <el-button size="small" :loading="loading.models" @click="refreshModels">刷新模型</el-button>
                    </el-form-item>

                    <el-form-item label="请求类型">
                      <el-select v-model="chatForm.requestType" style="width: 160px">
                        <el-option label="KNOWLEDGE_QA" value="KNOWLEDGE_QA" />
                        <el-option label="CHAT" value="CHAT" />
                        <el-option label="SUMMARY" value="SUMMARY" />
                        <el-option label="REWRITE" value="REWRITE" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="记忆模式">
                      <el-select v-model="chatForm.memoryMode" style="width: 140px">
                        <el-option label="SHORT" value="SHORT" />
                        <el-option label="SUMMARY" value="SUMMARY" />
                        <el-option label="NONE" value="NONE" />
                      </el-select>
                    </el-form-item>

                    <el-form-item>
                      <el-button size="small" @click="createNewSession">新建会话</el-button>
                    </el-form-item>

                    <el-form-item>
                      <el-button size="small" @click="loadSessions">刷新会话</el-button>
                    </el-form-item>
                  </el-form>
                </div>

                <div class="chat-layout">
                  <div class="chat-sidebar">
                    <div class="chat-sidebar__toolbar">
                      <el-input
                        v-model.trim="sessionKeyword"
                        size="small"
                        clearable
                        placeholder="搜索会话"
                        prefix-icon="el-icon-search"
                      />
                    </div>

                    <div v-loading="sessionLoading" class="session-list">
                      <el-empty
                        v-if="!filteredSessions.length"
                        description="暂无会话历史"
                        :image-size="60"
                      />

                      <div
                        v-for="item in filteredSessions"
                        :key="item.id"
                        class="session-item"
                        :class="{ active: selectedSessionId === item.id }"
                        @click="selectSession(item)"
                      >
                        <div class="session-item__title">{{ item.title || `会话 #${item.id}` }}</div>
                        <div class="session-item__meta">{{ formatTime(item.updatedAt || item.createdAt) }}</div>

                        <div class="session-item__actions">
                          <el-button type="text" size="mini" @click.stop="archiveSession(item)">归档</el-button>
                          <el-button type="text" size="mini" @click.stop="removeSession(item)">删除</el-button>
                        </div>
                      </div>
                    </div>

                    <div class="table-pagination session-pagination">
                      <el-pagination
                        background
                        small
                        layout="prev, pager, next"
                        :current-page="sessionPagination.page + 1"
                        :page-size="sessionPagination.size"
                        :total="sessionPagination.total"
                        @current-change="handleSessionPageChange"
                      />
                    </div>
                  </div>

                  <div class="chat-main">
                    <div class="chat-box">
                      <div ref="chatMessagesRef" class="chat-messages">
                        <el-empty
                          v-if="!chatMessages.length"
                          description="先问一个问题，回答里会展示命中来源与切片内容"
                          :image-size="80"
                        />

                        <div
                          v-for="(msg, index) in chatMessages"
                          :key="msg.id || `${msg.role}-${index}`"
                          class="chat-message"
                          :class="msg.role"
                        >
                          <div class="chat-message__role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
                          <div class="chat-message__content">{{ msg.content }}</div>

                          <div v-if="msg.role === 'assistant'" class="chat-message__footer">
                            <span v-if="msg.totalTokens">Tokens {{ msg.totalTokens }}</span>
                            <span v-if="msg.latencyMs">耗时 {{ msg.latencyMs }} ms</span>
                            <span v-if="msg.modelName">模型 {{ msg.modelName }}</span>

                            <el-button
                              v-if="msg.callLogId"
                              type="text"
                              size="mini"
                              @click="openRetrievalDrawerByMessage(msg)"
                            >
                              检索日志
                            </el-button>
                          </div>

                          <div
                            v-if="msg.role === 'assistant' && msg.sources && msg.sources.length"
                            class="chat-message__sources"
                          >
                            <div class="sources-header" @click="toggleMessageSources(index)">
                              <span>命中文档 / 引用来源（{{ msg.sources.length }}）</span>
                              <i class="el-icon-arrow-down" :class="{ 'is-open': msg.sourceOpen }"></i>
                            </div>

                            <div v-show="msg.sourceOpen" class="sources-list">
                              <div
                                v-for="(source, sIndex) in msg.sources"
                                :key="source.id || `${index}-${sIndex}`"
                                class="source-card"
                              >
                                <div class="source-card__top">
                                  <div class="source-card__title">{{ source.title || '未命中文档标题' }}</div>
                                  <div class="source-card__meta">
                                    <span v-if="source.score !== null && source.score !== undefined">Score {{ source.score }}</span>
                                    <span v-if="source.chunkIndex !== null && source.chunkIndex !== undefined">Chunk #{{ source.chunkIndex }}</span>
                                    <span v-if="source.retrievalMethod">{{ source.retrievalMethod }}</span>
                                  </div>
                                </div>

                                <div v-if="source.knowledgeBaseName" class="source-card__kb">
                                  知识库：{{ source.knowledgeBaseName }}
                                </div>

                                <div class="source-card__actions">
                                  <el-button type="text" size="mini" @click="locateSourceDocument(source)">定位文档</el-button>
                                  <el-button
                                    v-if="source.callLogId"
                                    type="text"
                                    size="mini"
                                    @click="openRetrievalDrawer(source.callLogId, source.title || '检索日志')"
                                  >
                                    检索日志
                                  </el-button>
                                </div>

                                <el-collapse>
                                  <el-collapse-item title="展开切片内容" :name="`${index}-${sIndex}`">
                                    <div class="source-card__content">{{ source.content || '暂无切片内容' }}</div>
                                  </el-collapse-item>
                                </el-collapse>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div class="chat-input">
                        <div class="chat-hint">
                          当前默认绑定知识库：{{ currentKnowledgeBase.name }}
                          <span>｜知识库默认模型：{{ currentKnowledgeBaseDefaultModelName }}</span>
                          <span>｜当前生效模型：{{ effectiveChatModelName }}</span>
                          <span v-if="selectedSessionId">｜当前会话 ID：{{ selectedSessionId }}</span>
                        </div>

                        <el-input
                          v-model.trim="chatForm.content"
                          type="textarea"
                          :rows="4"
                          placeholder="请输入你的问题，比如：总结这个知识库里关于 Spring 事务传播行为的要点，并给出命中文档来源"
                          @keyup.ctrl.enter.native="sendChat(false)"
                        />

                        <div class="chat-actions">
                          <el-button @click="clearChat">清空当前窗口</el-button>
                          <el-button :loading="loading.chat" @click="sendChat(false)">普通发送</el-button>
                          <el-button
                            v-if="loading.streamChat"
                            type="warning"
                            @click="stopStream"
                          >
                            停止流式
                          </el-button>
                          <el-button
                            v-else
                            type="primary"
                            :loading="loading.streamChat"
                            @click="sendChat(true)"
                          >
                            流式发送
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </div>
      </div>
    </el-card>

    <el-dialog
      :title="kbDialogMode === 'create' ? '新建知识库' : '编辑知识库'"
      :visible.sync="kbDialogVisible"
      width="760px"
      destroy-on-close
    >
      <el-form ref="kbFormRef" :model="kbForm" :rules="kbRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="知识库名称" prop="name">
              <el-input v-model.trim="kbForm.name" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="拥有者 ID">
              <el-input :value="kbForm.ownerId || '-'" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input v-model.trim="kbForm.description" type="textarea" :rows="3" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="作用域" prop="scopeType">
              <el-select v-model="kbForm.scopeType" style="width: 100%">
                <el-option label="PERSONAL" value="PERSONAL" />
                <el-option label="PROJECT" value="PROJECT" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="项目 ID">
              <el-input-number v-model="kbForm.projectId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="kbForm.sourceType" style="width: 100%">
                <el-option label="MANUAL" value="MANUAL" />
                <el-option label="UPLOAD" value="UPLOAD" />
                <el-option label="PROJECT_DOC" value="PROJECT_DOC" />
                <el-option label="BLOG" value="BLOG" />
                <el-option label="CIRCLE" value="CIRCLE" />
                <el-option label="PAID_CONTENT" value="PAID_CONTENT" />
                <el-option label="MIXED" value="MIXED" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="可见性">
              <el-select v-model="kbForm.visibility" style="width: 100%">
                <el-option label="PRIVATE" value="PRIVATE" />
                <el-option label="TEAM" value="TEAM" />
                <el-option label="PUBLIC" value="PUBLIC" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分块策略">
              <el-select v-model="kbForm.chunkStrategy" style="width: 100%">
                <el-option label="PARAGRAPH" value="PARAGRAPH" />
                <el-option label="FIXED" value="FIXED" />
                <el-option label="MARKDOWN" value="MARKDOWN" />
                <el-option label="CUSTOM" value="CUSTOM" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="默认 TopK">
              <el-input-number v-model="kbForm.defaultTopK" :min="1" :max="20" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="默认问答模型">
              <el-select
                v-model="kbForm.defaultModelId"
                clearable
                filterable
                placeholder="不设置时跟随系统当前模型"
                style="width: 100%"
              >
                <el-option
                  v-for="model in enabledModels"
                  :key="model.id"
                  :label="buildModelLabel(model)"
                  :value="model.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="默认模型说明">
              <div class="text-muted">未设置时，问答会自动回退到系统当前模型。</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Embedding Provider">
              <el-input v-model.trim="kbForm.embeddingProvider" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Embedding Model">
              <el-input v-model.trim="kbForm.embeddingModel" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div slot="footer">
        <el-button @click="kbDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveKb" :disabled="kbDialogMode === 'edit' ? !canEditCurrentKnowledgeBase : !canCreateKnowledgeBase" @click="submitKbForm">
          {{ kbDialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="新增文档"
      :visible.sync="documentDialogVisible"
      width="760px"
      destroy-on-close
    >
      <el-form ref="documentFormRef" :model="documentForm" :rules="documentRules" label-width="110px">
        <el-form-item label="导入方式">
          <el-radio-group v-model="documentImportMode" size="small">
            <el-radio-button label="manual">正文录入</el-radio-button>
            <el-radio-button label="reference">引用导入</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="documentForm.sourceType" style="width: 100%">
                <el-option label="MANUAL" value="MANUAL" />
                <el-option label="UPLOAD" value="UPLOAD" />
                <el-option label="PROJECT_DOC" value="PROJECT_DOC" />
                <el-option label="BLOG" value="BLOG" />
                <el-option label="CIRCLE" value="CIRCLE" />
                <el-option label="PAID_CONTENT" value="PAID_CONTENT" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model.trim="documentForm.title" />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="documentImportMode === 'reference'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="来源引用 ID">
                <el-input-number v-model="documentForm.sourceRefId" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="导入提示">
                <div class="text-muted">适合填项目文档 / 博客 / 圈子 / 付费内容主键，同时补充正文后一起入库。</div>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item label="正文" prop="contentText">
          <el-input v-model="documentForm.contentText" type="textarea" :rows="10" />
        </el-form-item>

        <div v-if="selectedLocalFileName" class="upload-tip">
          当前已导入本地文本：{{ selectedLocalFileName }}
        </div>
        <div v-if="fileReadError" class="upload-tip upload-tip--error">
          {{ fileReadError }}
        </div>
      </el-form>

      <div slot="footer">
        <el-button @click="documentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveDocument" :disabled="!canEditCurrentKnowledgeBase" @click="submitDocumentForm">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="添加成员"
      :visible.sync="memberDialogVisible"
      width="520px"
      destroy-on-close
    >
      <el-form ref="memberFormRef" :model="memberForm" :rules="memberRules" label-width="100px">
        <el-form-item label="用户 ID" prop="userId">
          <el-input-number v-model="memberForm.userId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>

        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="memberForm.roleCode" style="width: 100%">
            <el-option label="EDITOR" value="EDITOR" />
            <el-option label="VIEWER" value="VIEWER" />
          </el-select>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveMember" :disabled="!canManageCurrentMembers" @click="submitMemberForm">添加</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="文档切片"
      :visible.sync="chunkDrawerVisible"
      width="880px"
      destroy-on-close
    >
      <div class="drawer-meta">
        <span v-if="activeChunkDocument">文档：{{ activeChunkDocument.title || `文档 #${activeChunkDocument.id}` }}</span>
      </div>

      <el-table v-loading="loading.chunks" :data="chunks" border stripe size="small">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="chunkIndex" label="Chunk" width="90" />
        <el-table-column prop="tokenCount" label="Tokens" width="90" />
        <el-table-column label="内容" min-width="520">
          <template slot-scope="{ row }">
            <div class="chunk-content">{{ row.content || row.text || '-' }}</div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      :title="taskDrawerTitle"
      :visible.sync="taskDrawerVisible"
      width="900px"
      destroy-on-close
    >
      <div class="drawer-meta">{{ taskDrawerSubtitle }}</div>

      <el-table v-loading="loading.tasks" :data="indexTasks" border stripe size="small">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="taskType" label="任务类型" width="140" />
        <el-table-column label="状态" width="120">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="taskStatusTagType(row.status)">{{ row.status || 'UNKNOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="documentId" label="文档 ID" width="100" />
        <el-table-column prop="message" label="消息" min-width="240" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="170">
          <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template slot-scope="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      title="检索日志"
      :visible.sync="retrievalDrawerVisible"
      width="980px"
      destroy-on-close
    >
      <div class="drawer-meta">{{ currentRetrievalMeta }}</div>

      <el-table v-loading="loading.retrievals" :data="retrievalLogs" border stripe size="small">
        <el-table-column prop="title" label="命中文档" min-width="200" show-overflow-tooltip />
        <el-table-column prop="documentId" label="文档 ID" width="100" />
        <el-table-column prop="chunkIndex" label="Chunk" width="90" />
        <el-table-column prop="score" label="Score" width="110" />
        <el-table-column prop="retrievalMethod" label="检索方式" width="140" />
        <el-table-column label="内容" min-width="360">
          <template slot-scope="{ row }">
            <div class="chunk-content">{{ row.content || '-' }}</div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>

function safeParsePermissionPayload(raw) {
  try {
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

function appendPermissionCodes(target, source) {
  if (!source) return
  if (Array.isArray(source)) {
    source.forEach(item => appendPermissionCodes(target, item))
    return
  }
  if (typeof source === 'string') {
    const code = source.trim()
    if (code) target.add(code)
    return
  }
  if (typeof source !== 'object') return
  ;[
    source.permissionCode,
    source.permission,
    source.code,
    source.authority,
    source.value,
    source.name
  ].forEach(item => appendPermissionCodes(target, item))
  ;[
    source.permissions,
    source.permissionCodes,
    source.authorities,
    source.roles,
    source.menus,
    source.children,
    source.buttonPermissions
  ].forEach(item => appendPermissionCodes(target, item))
}

function readBrowserPermissionCodes() {
  if (typeof window === 'undefined') return []
  const set = new Set()
  const storages = [window.localStorage, window.sessionStorage]
  const keys = ['permissions', 'permissionCodes', 'authorities', 'menus', 'userInfo', 'user', 'loginUser']
  storages.forEach(storage => {
    keys.forEach(key => {
      try {
        const raw = storage.getItem(key)
        if (!raw) return
        const parsed = safeParsePermissionPayload(raw)
        if (parsed == null) {
          appendPermissionCodes(set, raw)
        } else {
          appendPermissionCodes(set, parsed)
        }
      } catch (e) {
      }
    })
  })
  return Array.from(set)
}

import {
  pageKnowledgeBasesByOwner,
  pageKnowledgeBasesByProject,
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  pageKnowledgeDocuments,
  addKnowledgeDocument,
  uploadKnowledgeDocuments,
  uploadKnowledgeDocumentsZip,
  listDocumentChunks,
  downloadKnowledgeDocument,
  downloadKnowledgeDocumentsZip,
  listKnowledgeBaseMembers,
  addKnowledgeBaseMember,
  removeKnowledgeBaseMember,
  createKnowledgeIndexTask,
  listKnowledgeBaseIndexTasks,
  listDocumentIndexTasks,
  pageAiSessions,
  pageAiSessionMessages,
  bindSessionKnowledgeBases,
  archiveAiSession,
  deleteAiSession,
  chatWithKnowledgeBase,
  listCallRetrievals,
  streamChatWithKnowledgeBase
} from '@/api/knowledgeBase'
import { listEnabledAiModels } from '@/api/aiAdmin'

export default {
  name: 'KnowledgeBasePage',
  data() {
    return {
      permissionCodes: [],
      listMode: 'owner',
      keyword: '',
      ownerId: null,
      projectId: null,
      routeProjectId: null,

      loading: {
        kbList: false,
        saveKb: false,
        documents: false,
        saveDocument: false,
        members: false,
        saveMember: false,
        chunks: false,
        tasks: false,
        chat: false,
        streamChat: false,
        retrievals: false,
        models: false
      },

      pagination: {
        page: 0,
        size: 10,
        total: 0
      },

      documentPagination: {
        page: 0,
        size: 10,
        total: 0
      },

      sessionPagination: {
        page: 0,
        size: 10,
        total: 0
      },

      knowledgeBases: [],
      currentKnowledgeBase: null,
      activeTab: 'documents',

      documents: [],
      members: [],
      chunks: [],
      indexTasks: [],
      sessions: [],

      kbDialogVisible: false,
      kbDialogMode: 'create',
      kbForm: {
        scopeType: 'PERSONAL',
        projectId: null,
        ownerId: null,
        defaultModelId: null,
        name: '',
        description: '',
        sourceType: 'MANUAL',
        embeddingProvider: 'local',
        embeddingModel: 'bge-small',
        chunkStrategy: 'PARAGRAPH',
        defaultTopK: 5,
        visibility: 'PRIVATE'
      },

      documentDialogVisible: false,
      documentImportMode: 'manual',
      documentForm: {
        sourceType: 'MANUAL',
        sourceRefId: null,
        title: '',
        contentText: '',
        contentHash: null
      },
      selectedLocalFileName: '',
      fileReadError: '',

      memberDialogVisible: false,
      memberForm: {
        userId: null,
        roleCode: 'EDITOR'
      },

      chunkDrawerVisible: false,
      taskDrawerVisible: false,
      taskDrawerScope: 'knowledgeBase',
      activeChunkDocument: null,
      currentTaskDocument: null,
      taskPollTimer: null,

      retrievalDrawerVisible: false,
      retrievalLogs: [],
      currentRetrievalMeta: '',

      chatForm: {
        sessionId: null,
        userId: null,
        content: '',
        modelId: null,
        promptTemplateId: null,
        requestType: 'KNOWLEDGE_QA',
        bizType: 'GENERAL',
        bizId: null,
        projectId: null,
        sceneCode: 'knowledge.base',
        memoryMode: 'SHORT'
      },

      chatMessages: [],
      selectedSessionId: null,
      streamStopper: null,
      sessionKeyword: '',
      sessionLoading: false,
      enabledModels: [],
      activeModel: null,

      kbRules: {
        name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
        scopeType: [{ required: true, message: '请选择作用域', trigger: 'change' }]
      },

      documentRules: {
        title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
        contentText: [{ required: true, message: '请输入文档正文', trigger: 'blur' }]
      },

      memberRules: {
        userId: [{ required: true, message: '请输入用户 ID', trigger: 'change' }],
        roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },

  computed: {
    currentUserIdNumber() {
      return Number(this.ownerId || this.chatForm.userId || 0) || null
    },

    currentMemberRoleCode() {
      const uid = this.currentUserIdNumber
      if (!uid || !this.currentKnowledgeBase) return ''
      if (Number(this.currentKnowledgeBase.ownerId || 0) === uid) return 'OWNER'
      const hit = (this.members || []).find(item => Number(item.userId || 0) === uid)
      return hit ? String(hit.roleCode || '') : ''
    },

    canReadCurrentKnowledgeBase() {
      return !!this.currentKnowledgeBase && this.hasAuthority('view:knowledge-base')
    },

    // canCreateKnowledgeBase() {
    //   return this.hasAuthority('view:knowledge-base')
    // },

    // canEditCurrentKnowledgeBase() {
    //   return ['OWNER', 'EDITOR'].includes(this.currentMemberRoleCode)
    // },

    // canManageCurrentMembers() {
    //   return this.currentMemberRoleCode === 'OWNER'
    // },

    // canEditKnowledgeBaseItem(row) {
    //   return ['OWNER', 'EDITOR'].includes(this.roleOfKnowledgeBase(row))
    // },

    // 临时放开权限，后续根据实际权限需求调整
    canCreateKnowledgeBase() {
      return true
    },

    canEditCurrentKnowledgeBase() {
      return true
    },

    canManageCurrentMembers() {
      return true
    },

    // canEditKnowledgeBaseItem(item) {
    //   return true
    // },

    filteredKnowledgeBases() {
      if (!this.keyword) return this.knowledgeBases
      const key = String(this.keyword).toLowerCase()
      return this.knowledgeBases.filter(item => {
        if (!item) return false
        return (
          String(item.name || '').toLowerCase().includes(key) ||
          String(item.description || '').toLowerCase().includes(key)
        )
      })
    },

    filteredSessions() {
      if (!this.sessionKeyword) return this.sessions
      const key = String(this.sessionKeyword).toLowerCase()
      return this.sessions.filter(item => {
        if (!item) return false
        return (
          String(item.title || '').toLowerCase().includes(key) ||
          String(item.id || '').toLowerCase().includes(key)
        )
      })
    },

    effectiveChatModelId() {
      return this.chatForm.modelId || (this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId) || (this.activeModel && this.activeModel.id) || null
    },

    effectiveChatModelName() {
      const model = this.findModelById(this.effectiveChatModelId)
      return model ? this.buildModelLabel(model) : '未选择'
    },

    currentKnowledgeBaseDefaultModelName() {
      const model = this.findModelById(this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId)
      return model ? this.buildModelLabel(model) : '跟随系统当前模型'
    },

    taskDrawerTitle() {
      return this.taskDrawerScope === 'document' ? '文档索引任务' : '知识库索引任务'
    },

    taskDrawerSubtitle() {
      if (this.taskDrawerScope === 'document' && this.currentTaskDocument) {
        return this.currentTaskDocument.title || `文档 #${this.currentTaskDocument.id}`
      }
      return this.currentKnowledgeBase ? this.currentKnowledgeBase.name : '当前知识库'
    }
  },

  watch: {
    activeTab(val) {
      if (!this.currentKnowledgeBase) return
      if (val === 'documents') this.loadDocuments()
      if (val === 'members') this.loadMembers()
      if (val === 'chat') this.loadSessions()
    },

    taskDrawerVisible(val) {
      if (val) {
        this.startTaskPolling()
      } else {
        this.stopTaskPolling()
      }
    }
  },

  mounted() {
    this.initPermissionCodes()
    this.kbForm = this.getDefaultKbForm()
    this.documentForm = this.getDefaultDocumentForm()
    this.memberForm = this.getDefaultMemberForm()
    this.initRouteContext()
    this.initUserId()
    this.loadModels()
    this.loadKnowledgeBases()
  },

  beforeDestroy() {
    this.stopTaskPolling()
    this.stopStream()
  },

  methods: {
    canEditKnowledgeBaseItem(item) {
      return true
    },

    initPermissionCodes() {
      this.permissionCodes = readBrowserPermissionCodes()
    },

    hasAuthority(code) {
      if (!code) return true
      if (this.permissionCodes.includes(code)) return true
      const routePermissions = (((this.$route || {}).meta || {}).permissions) || []
      return Array.isArray(routePermissions) && routePermissions.includes(code)
    },

    roleOfKnowledgeBase(row) {
      if (!row) return ''
      const uid = this.currentUserIdNumber
      if (!uid) return ''
      if (Number(row.ownerId || 0) === uid) return 'OWNER'
      if (this.currentKnowledgeBase && Number(this.currentKnowledgeBase.id || 0) === Number(row.id || 0)) {
        return this.currentMemberRoleCode
      }
      return ''
    },

    ensureCanCreateKnowledgeBase() {
      if (this.canCreateKnowledgeBase) return true
      this.$message.warning('你没有知识库访问权限')
      return false
    },

    ensureCanEditCurrentKnowledgeBase(action = '执行该操作') {
      if (this.canEditCurrentKnowledgeBase) return true
      this.$message.warning(`只有知识库 OWNER / EDITOR 才能${action}`)
      return false
    },

    ensureCanManageCurrentMembers(action = '管理成员') {
      if (this.canManageCurrentMembers) return true
      this.$message.warning(`只有知识库 OWNER 才能${action}`)
      return false
    },

    getDefaultKbForm() {
      return {
        scopeType: this.routeProjectId ? 'PROJECT' : 'PERSONAL',
        projectId: this.routeProjectId || null,
        ownerId: this.ownerId || null,
        defaultModelId: this.activeModel && this.activeModel.id ? this.activeModel.id : null,
        name: '',
        description: '',
        sourceType: this.routeProjectId ? 'PROJECT_DOC' : 'MANUAL',
        embeddingProvider: 'local',
        embeddingModel: 'bge-small',
        chunkStrategy: 'PARAGRAPH',
        defaultTopK: 5,
        visibility: 'PRIVATE'
      }
    },

    getDefaultDocumentForm() {
      return {
        sourceType: this.routeProjectId ? 'PROJECT_DOC' : 'MANUAL',
        sourceRefId: null,
        title: '',
        contentText: '',
        contentHash: null
      }
    },

    getDefaultMemberForm() {
      return {
        userId: null,
        roleCode: 'EDITOR'
      }
    },

    getKnowledgeBaseModelStorageKey(kbId) {
      if (!kbId) return ''
      return `kb_default_model_${kbId}`
    },

    readKnowledgeBaseDefaultModel(kbId) {
      const key = this.getKnowledgeBaseModelStorageKey(kbId)
      if (!key || typeof localStorage === 'undefined') return null
      const raw = localStorage.getItem(key)
      return raw ? Number(raw) || null : null
    },

    persistKnowledgeBaseDefaultModel(kbId, modelId) {
      const key = this.getKnowledgeBaseModelStorageKey(kbId)
      if (!key || typeof localStorage === 'undefined') return
      if (modelId) {
        localStorage.setItem(key, String(modelId))
      } else {
        localStorage.removeItem(key)
      }
    },

    findModelById(id) {
      const targetId = Number(id) || null
      if (!targetId) return null
      return this.enabledModels.find(item => Number(item.id) === targetId) || (this.activeModel && Number(this.activeModel.id) === targetId ? this.activeModel : null) || null
    },

    buildModelLabel(model) {
      if (!model) return '未命名模型'
      const name = model.modelName || model.name || model.label || `模型 #${model.id}`
      const provider = model.providerCode || model.modelType || ''
      return provider ? `${name}（${provider}）` : name
    },

    async loadModels(showMessage = false) {
      this.loading.models = true
      try {
        const enabledRes = await listEnabledAiModels()
        const enabledList = this.extractListData(enabledRes)
        this.enabledModels = enabledList
          .map(item => ({ ...item, id: Number(item.id) || item.id }))
          .filter(item => item && item.id)

        this.activeModel = this.enabledModels.length ? this.enabledModels[0] : null

        if (this.currentKnowledgeBase && this.currentKnowledgeBase.id) {
          const persistedDefault = this.readKnowledgeBaseDefaultModel(this.currentKnowledgeBase.id)
          if (persistedDefault && !this.currentKnowledgeBase.defaultModelId) {
            this.$set(this.currentKnowledgeBase, 'defaultModelId', persistedDefault)
          }
          this.useKnowledgeBaseDefaultModel(false)
        } else if (!this.chatForm.modelId && this.activeModel && this.activeModel.id) {
          this.chatForm.modelId = this.activeModel.id
        }

        if (showMessage) this.$message.success('模型列表已刷新')
      } catch (e) {
        this.enabledModels = []
        this.activeModel = null
        if (showMessage) {
          this.$message.error(this.extractResponseMessage(e, '加载模型列表失败'))
        }
      } finally {
        this.loading.models = false
      }
    },

    refreshModels() {
      this.loadModels(true)
    },

    useKnowledgeBaseDefaultModel(showMessage = true) {
      const modelId = (this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId) || (this.activeModel && this.activeModel.id) || null
      this.chatForm.modelId = modelId
      if (showMessage) {
        this.$message.success(modelId ? `已切换到${this.currentKnowledgeBase && this.currentKnowledgeBase.defaultModelId ? '知识库默认' : '系统当前'}模型` : '当前没有可用模型')
      }
    },

    useActiveModel(showMessage = true) {
      const modelId = this.activeModel && this.activeModel.id ? this.activeModel.id : null
      this.chatForm.modelId = modelId
      if (showMessage) {
        this.$message.success(modelId ? '已切换到系统当前模型' : '当前没有系统默认模型')
      }
    },

    initRouteContext() {
      const routeProjectId = Number(this.$route.query.projectId || this.$route.params.projectId || 0) || null
      if (!routeProjectId) return
      this.routeProjectId = routeProjectId
      this.projectId = routeProjectId
      this.listMode = 'project'
      this.chatForm.projectId = routeProjectId
      this.chatForm.bizType = 'PROJECT'
      this.chatForm.sceneCode = 'project.knowledge-base'
    },

    initUserId() {
      try {
        const raw = localStorage.getItem('userInfo') || localStorage.getItem('user')
        if (!raw) return
        const user = JSON.parse(raw)
        const uid = user && (user.id || user.userId)
        if (uid) {
          this.ownerId = uid
          this.chatForm.userId = uid
          this.kbForm.ownerId = uid
        }
      } catch (e) {}
    },

    extractResponseData(res) {
      if (!res) return null
      const first = Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
      if (
        first &&
        typeof first === 'object' &&
        Object.prototype.hasOwnProperty.call(first, 'data') &&
        (Object.prototype.hasOwnProperty.call(first, 'code') ||
          Object.prototype.hasOwnProperty.call(first, 'msg') ||
          Object.prototype.hasOwnProperty.call(first, 'message'))
      ) {
        return first.data
      }
      return first
    },

    extractResponseMessage(res, fallback) {
      if (!res) return fallback || ''
      const first = Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res
      return first.msg || first.message || fallback || ''
    },

    extractPageData(res) {
      const data = this.extractResponseData(res) || {}
      return {
        content: data.content || data.records || data.list || [],
        total: data.totalElements || data.total || 0
      }
    },

    extractListData(res) {
      const data = this.extractResponseData(res)
      if (Array.isArray(data)) return data
      if (data && Array.isArray(data.content)) return data.content
      if (data && Array.isArray(data.records)) return data.records
      if (data && Array.isArray(data.list)) return data.list
      return []
    },

    normalizeKnowledgeBase(raw = {}) {
      return {
        id: raw.id || null,
        ownerId: raw.ownerId || raw.userId || null,
        scopeType: raw.scopeType || raw.scope || '',
        projectId: raw.projectId || null,
        name: raw.name || '',
        description: raw.description || '',
        sourceType: raw.sourceType || '',
        embeddingProvider: raw.embeddingProvider || '',
        embeddingModel: raw.embeddingModel || '',
        chunkStrategy: raw.chunkStrategy || '',
        defaultTopK: raw.defaultTopK || raw.topK || null,
        defaultModelId: raw.defaultModelId || raw.defaultAiModelId || raw.modelId || this.readKnowledgeBaseDefaultModel(raw.id || null) || null,
        visibility: raw.visibility || '',
        status: raw.status || '',
        lastIndexedAt: raw.lastIndexedAt || null,
        createdAt: raw.createdAt || null,
        updatedAt: raw.updatedAt || null
      }
    },

    normalizeDocument(raw = {}) {
      return {
        id: raw.id || raw.documentId || null,
        title: raw.title || raw.name || '',
        sourceType: raw.sourceType || '',
        status: raw.status || '',
        indexedAt: raw.indexedAt || null,
        updatedAt: raw.updatedAt || raw.createdAt || null,
        fileType: raw.fileType || raw.contentType || raw.extension || '',
        createdAt: raw.createdAt || null
      }
    },

    normalizeMember(raw = {}) {
      return {
        id: raw.id || null,
        userId: raw.userId || null,
        roleCode: raw.roleCode || raw.role || '',
        grantedBy: raw.grantedBy || raw.grantedByUserId || null,
        createdAt: raw.createdAt || null
      }
    },

    normalizeChunk(raw = {}) {
      return {
        id: raw.id || raw.chunkId || null,
        chunkIndex:
          raw.chunkIndex !== undefined && raw.chunkIndex !== null
            ? raw.chunkIndex
            : raw.rankNo !== undefined && raw.rankNo !== null
              ? raw.rankNo
              : null,
        tokenCount: raw.tokenCount || raw.tokens || null,
        content: raw.content || raw.text || ''
      }
    },

    normalizeTask(raw = {}) {
      return {
        id: raw.id || raw.taskId || null,
        taskType: raw.taskType || raw.type || '',
        status: raw.status || '',
        documentId: raw.documentId || null,
        message: raw.message || raw.errorMessage || '',
        createdAt: raw.createdAt || null,
        updatedAt: raw.updatedAt || raw.finishedAt || null
      }
    },

    normalizeSession(raw = {}) {
      return {
        id: raw.id || raw.sessionId || null,
        title: raw.sessionTitle || raw.title || raw.name || '',
        archived: !!(raw.archived || raw.isArchived || raw.status === 'ARCHIVED'),
        updatedAt: raw.updatedAt || raw.lastMessageAt || raw.modifiedAt || raw.createdAt || raw.createTime || null,
        createdAt: raw.createdAt || raw.createTime || null,
        defaultKnowledgeBaseId:
          raw.defaultKnowledgeBaseId ||
          (raw.defaultKnowledgeBase && raw.defaultKnowledgeBase.id) ||
          (raw.knowledgeBase && raw.knowledgeBase.id) ||
          null,
        knowledgeBaseIds:
          raw.knowledgeBaseIds ||
          raw.kbIds ||
          (Array.isArray(raw.knowledgeBases) ? raw.knowledgeBases.map(item => item.id) : [])
      }
    },

    normalizeSources(list = [], extra = {}) {
      if (!Array.isArray(list)) return []
      return list.map(item => {
        const document = item.document || {}
        const chunk = item.chunk || {}
        const knowledgeBase = item.knowledgeBase || {}
        return {
          id: item.id || item.chunkId || chunk.id || item.documentId || document.id || null,
          callLogId: extra.callLogId || item.callLogId || null,
          title: item.title || item.documentTitle || item.documentName || document.title || '',
          documentId: item.documentId || document.id || null,
          chunkId: item.chunkId || chunk.id || null,
          knowledgeBaseName: item.knowledgeBaseName || item.kbName || knowledgeBase.name || '',
          chunkIndex:
            item.chunkIndex !== undefined && item.chunkIndex !== null
              ? item.chunkIndex
              : chunk.chunkIndex !== undefined && chunk.chunkIndex !== null
                ? chunk.chunkIndex
                : item.rankNo !== undefined && item.rankNo !== null
                  ? item.rankNo
                  : null,
          rankNo: item.rankNo !== undefined && item.rankNo !== null ? item.rankNo : null,
          score: item.score !== undefined ? item.score : null,
          retrievalMethod: item.retrievalMethod || item.method || '',
          content: item.content || item.chunkContent || item.text || chunk.content || ''
        }
      })
    },

    extractAnswerSources(data = {}, extra = {}) {
      const sourceList =
        data.sources ||
        data.citations ||
        data.references ||
        data.retrievals ||
        data.retrievedChunks ||
        []
      return this.normalizeSources(sourceList, extra)
    },

    normalizeMessage(raw = {}) {
      const role = String(raw.role || '').toLowerCase()
      return {
        id: raw.id || null,
        role: role === 'assistant' || role === 'user' ? role : role === 'tool' ? 'assistant' : 'user',
        content: raw.content || '',
        totalTokens: raw.totalTokens || null,
        latencyMs: raw.latencyMs || null,
        modelName:
          raw.modelName ||
          (raw.model && (raw.model.name || raw.model.modelName || raw.model.code)) ||
          '',
        callLogId: raw.callLogId || null,
        finishReason: raw.finishReason || '',
        sources: [],
        sourceOpen: true
      }
    },

    persistCurrentKnowledgeBaseToAssistant(kb) {
      if (typeof window === 'undefined') return

      if (!kb || !kb.id) {
        localStorage.removeItem('ai_assistant_current_kb')
        window.dispatchEvent(
          new CustomEvent('ai-assistant-kb-change', {
            detail: null
          })
        )
        return
      }

      const payload = {
        id: kb.id,
        name: kb.name || '',
        ownerId: kb.ownerId || null,
        projectId: kb.projectId || null,
        scopeType: kb.scopeType || '',
        description: kb.description || '',
        status: kb.status || '',
        updatedAt: Date.now()
      }

      localStorage.setItem('ai_assistant_current_kb', JSON.stringify(payload))
      window.dispatchEvent(
        new CustomEvent('ai-assistant-kb-change', {
          detail: payload
        })
      )
    },

    formatTime(value) {
      if (!value) return '-'
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return value
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const i = String(d.getMinutes()).padStart(2, '0')
      const s = String(d.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${day} ${h}:${i}:${s}`
    },

    kbStatusTagType(status) {
      const s = String(status || '').toUpperCase()
      if (s === 'ACTIVE' || s === 'READY') return 'success'
      if (s === 'PENDING' || s === 'BUILDING') return 'warning'
      if (s === 'FAILED' || s === 'DELETED') return 'danger'
      return 'info'
    },

    docStatusTagType(status) {
      const s = String(status || '').toUpperCase()
      if (s === 'INDEXED' || s === 'READY') return 'success'
      if (s === 'PENDING' || s === 'UPLOADING' || s === 'PROCESSING') return 'warning'
      if (s === 'FAILED' || s === 'ERROR') return 'danger'
      return 'info'
    },

    taskStatusTagType(status) {
      const s = String(status || '').toUpperCase()
      if (s === 'SUCCESS' || s === 'DONE' || s === 'COMPLETED') return 'success'
      if (s === 'PENDING' || s === 'RUNNING') return 'warning'
      if (s === 'FAILED' || s === 'ERROR' || s === 'CANCELLED') return 'danger'
      return 'info'
    },

    handleListModeChange() {
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },

    handleKbPageChange(page) {
      this.pagination.page = page - 1
      this.loadKnowledgeBases()
    },

    handleDocumentPageChange(page) {
      this.documentPagination.page = page - 1
      this.loadDocuments()
    },

    handleSessionPageChange(page) {
      this.sessionPagination.page = page - 1
      this.loadSessions(false)
    },

    triggerZipUploadSelect() {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      const input = this.$refs.uploadZipInput
      if (input) {
        input.value = ''
        input.click()
      }
    },

    resetFileInput(refName) {
      const input = this.$refs[refName]
      if (input) {
        input.value = ''
      }
    },

    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        const params = {
          page: this.pagination.page,
          size: this.pagination.size
        }
        if (this.listMode === 'project' && !this.projectId) {
          this.knowledgeBases = []
          this.pagination.total = 0
          return
        }
        if (this.listMode === 'owner' && !this.ownerId) {
          this.knowledgeBases = []
          this.pagination.total = 0
          return
        }
        const res =
          this.listMode === 'project'
            ? await pageKnowledgeBasesByProject(this.projectId, params)
            : await pageKnowledgeBasesByOwner(this.ownerId, params)

        const pageData = this.extractPageData(res)
        this.knowledgeBases = (pageData.content || []).map(this.normalizeKnowledgeBase)
        this.pagination.total = pageData.total || 0

        const currentId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
        let target = null
        if (currentId) {
          target = this.knowledgeBases.find(item => item.id === currentId) || null
        }
        if (!target && this.knowledgeBases.length) {
          target = this.knowledgeBases[0]
        }

        if (target) {
          this.selectKnowledgeBase(target, { reloadBase: false })
        } else {
          this.currentKnowledgeBase = null
          this.persistCurrentKnowledgeBaseToAssistant(null)
          this.documents = []
          this.members = []
          this.indexTasks = []
          this.sessions = []
          this.chatMessages = []
          this.selectedSessionId = null
          this.chatForm.sessionId = null
        }
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载知识库失败'))
      } finally {
        this.loading.kbList = false
      }
    },

    async selectKnowledgeBase(item, options = {}) {
      if (!item || !item.id) return
      const reloadBase = options.reloadBase !== false
      try {
        if (reloadBase) {
          const res = await getKnowledgeBase(item.id)
          this.currentKnowledgeBase = this.normalizeKnowledgeBase(this.extractResponseData(res) || item)
        } else {
          this.currentKnowledgeBase = this.normalizeKnowledgeBase(item)
        }

        const persistedDefaultModelId = this.readKnowledgeBaseDefaultModel(this.currentKnowledgeBase.id)
        if (persistedDefaultModelId && !this.currentKnowledgeBase.defaultModelId) {
          this.$set(this.currentKnowledgeBase, 'defaultModelId', persistedDefaultModelId)
        }
        this.persistCurrentKnowledgeBaseToAssistant(this.currentKnowledgeBase)
        this.useKnowledgeBaseDefaultModel(false)

        await this.loadMembers(true)

        this.documentPagination.page = 0
        this.sessionPagination.page = 0
        this.createNewSession(false)

        if (this.activeTab === 'documents') {
          this.loadDocuments()
        } else if (this.activeTab === 'members') {
          this.loadMembers()
        } else if (this.activeTab === 'chat') {
          this.loadSessions()
        }

        this.loadIndexTasks('knowledgeBase')
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载知识库详情失败'))
      }
    },

    openKbDialog(mode, row) {
      if (mode === 'create' && !this.ensureCanCreateKnowledgeBase()) return
      if (mode === 'edit' && !this.canEditKnowledgeBaseItem(row || this.currentKnowledgeBase)) {
        this.$message.warning('只有知识库 OWNER / EDITOR 才能编辑知识库')
        return
      }
      this.kbDialogMode = mode
      if (mode === 'edit' && row) {
        this.kbForm = {
          ...this.getDefaultKbForm(),
          ...this.normalizeKnowledgeBase(row),
          defaultModelId: this.normalizeKnowledgeBase(row).defaultModelId || this.readKnowledgeBaseDefaultModel(row.id) || null
        }
      } else {
        this.kbForm = this.getDefaultKbForm()
      }
      this.kbDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.kbFormRef) this.$refs.kbFormRef.clearValidate()
      })
    },

    submitKbForm() {
      if (this.kbDialogMode === 'edit' && !this.ensureCanEditCurrentKnowledgeBase('编辑知识库')) return
      if (this.kbDialogMode !== 'edit' && !this.ensureCanCreateKnowledgeBase()) return
      if (!this.$refs.kbFormRef) return
      this.$refs.kbFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveKb = true
        try {
          const payload = {
            ...this.kbForm
          }
          let res = null
          if (this.kbDialogMode === 'edit' && this.kbForm.id) {
            res = await updateKnowledgeBase(this.kbForm.id, payload)
            this.$message.success('知识库已更新')
          } else {
            res = await createKnowledgeBase(payload)
            this.$message.success('知识库已创建')
          }
          const saved = this.extractResponseData(res) || {}
          const savedId = saved.id || this.kbForm.id || null
          if (savedId) {
            this.persistKnowledgeBaseDefaultModel(savedId, payload.defaultModelId || null)
          }
          if (this.currentKnowledgeBase && this.currentKnowledgeBase.id && Number(this.currentKnowledgeBase.id) === Number(savedId || this.currentKnowledgeBase.id)) {
            this.$set(this.currentKnowledgeBase, 'defaultModelId', payload.defaultModelId || null)
            this.useKnowledgeBaseDefaultModel(false)
          }
          this.kbDialogVisible = false
          this.loadKnowledgeBases()
        } catch (e) {
          this.$message.error(this.extractResponseMessage(e, '保存知识库失败'))
        } finally {
          this.loading.saveKb = false
        }
      })
    },

    async loadDocuments() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.documents = true
      try {
        const res = await pageKnowledgeDocuments(this.currentKnowledgeBase.id, {
          page: this.documentPagination.page,
          size: this.documentPagination.size
        })
        const pageData = this.extractPageData(res)
        this.documents = (pageData.content || []).map(this.normalizeDocument)
        this.documentPagination.total = pageData.total || 0
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载文档失败'))
      } finally {
        this.loading.documents = false
      }
    },

    openDocumentDialog() {
      if (!this.ensureCanEditCurrentKnowledgeBase('新增文档')) return
      this.documentForm = this.getDefaultDocumentForm()
      this.documentImportMode = 'manual'
      this.selectedLocalFileName = ''
      this.fileReadError = ''
      this.documentDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.documentFormRef) this.$refs.documentFormRef.clearValidate()
      })
    },

    triggerUploadSelect() {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      if (!this.currentKnowledgeBase) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (this.$refs.uploadFileInput) {
        this.$refs.uploadFileInput.value = ''
        this.$refs.uploadFileInput.click()
      }
    },

    async handleUploadFileChange(e) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      const files = Array.from((e && e.target && e.target.files) || [])
      if (!files.length) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      this.loading.saveDocument = true
      try {
        const formData = new FormData()
        files.forEach(file => {
          formData.append('files', file)
        })
        await uploadKnowledgeDocuments(this.currentKnowledgeBase.id, formData)
        this.$message.success('文件上传成功')
        this.loadDocuments()
      } catch (e2) {
        this.$message.error(this.extractResponseMessage(e2, '文件上传失败'))
      } finally {
        this.loading.saveDocument = false
        if (this.$refs.uploadFileInput) this.$refs.uploadFileInput.value = ''
      }
    },

    async handleUploadZipChange(event) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      const files = event && event.target && event.target.files ? Array.from(event.target.files) : []
      const file = files[0]

      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }

      if (!file) return

      const lowerName = (file.name || '').toLowerCase()
      if (!lowerName.endsWith('.zip')) {
        this.$message.warning('只能上传 zip 文件')
        return
      }

      const formData = new FormData()
      formData.append('file', file)
      formData.append('sourceType', 'PROJECT_DOC')

      this.loading.documents = true
      try {
        const res = await uploadKnowledgeDocumentsZip(this.currentKnowledgeBase.id, formData)
        const msg = (res && res.message) || (res && res.msg) || 'ZIP 项目导入成功'
        this.$message.success(msg)
        await this.loadDocuments()
        if (typeof this.loadKnowledgeBaseDetail === 'function') {
          await this.loadKnowledgeBaseDetail(this.currentKnowledgeBase.id)
        }
        if (typeof this.loadKnowledgeBaseTasks === 'function') {
          await this.loadKnowledgeBaseTasks()
        }
      } catch (e) {
        const msg = (e && e.message) || 'ZIP 项目导入失败'
        this.$message.error(msg)
      } finally {
        this.loading.documents = false
        const input = this.$refs.uploadZipInput
        if (input) input.value = ''
      }
    },  

    triggerLocalFileSelect() {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      if (!this.currentKnowledgeBase) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (this.$refs.localFileInput) {
        this.$refs.localFileInput.value = ''
        this.$refs.localFileInput.click()
      }
    },

    handleLocalFileChange(e) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      const file = e && e.target && e.target.files ? e.target.files[0] : null
      if (!file) return
      this.fileReadError = ''
      const reader = new FileReader()
      reader.onload = evt => {
        const content = evt && evt.target ? evt.target.result : ''
        this.documentForm = {
          ...this.getDefaultDocumentForm(),
          title: file.name,
          contentText: typeof content === 'string' ? content : ''
        }
        this.selectedLocalFileName = file.name
        this.documentImportMode = 'manual'
        this.documentDialogVisible = true
      }
      reader.onerror = () => {
        this.fileReadError = '读取本地文本失败'
        this.$message.error('读取本地文本失败')
      }
      reader.readAsText(file, 'utf-8')
    },

    submitDocumentForm() {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!this.$refs.documentFormRef) return
      this.$refs.documentFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveDocument = true
        try {
          await addKnowledgeDocument(this.currentKnowledgeBase.id, {
            ...this.documentForm
          })
          this.$message.success('文档已保存')
          this.documentDialogVisible = false
          this.loadDocuments()
        } catch (e) {
          this.$message.error(this.extractResponseMessage(e, '保存文档失败'))
        } finally {
          this.loading.saveDocument = false
        }
      })
    },

    async viewChunks(row) {
      if (!row || !row.id) return
      this.activeChunkDocument = row
      this.chunkDrawerVisible = true
      this.loading.chunks = true
      try {
        const res = await listDocumentChunks(row.id)
        this.chunks = this.extractListData(res).map(this.normalizeChunk)
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载切片失败'))
      } finally {
        this.loading.chunks = false
      }
    },

    async downloadDocument(row) {
      if (!row || !row.id) return
      try {
        const result = await downloadKnowledgeDocument(row.id)
        this.downloadBlob(result.blob, result.fileName || row.title || `document-${row.id}`)
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '下载文档失败'))
      }
    },

    async downloadCurrentDocumentsZip() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!this.documents.length) {
        this.$message.warning('当前页没有可下载文档')
        return
      }
      try {
        const ids = this.documents.map(item => item.id).filter(Boolean)
        const result = await downloadKnowledgeDocumentsZip(this.currentKnowledgeBase.id, ids)
        this.downloadBlob(result.blob, result.fileName || `${this.currentKnowledgeBase.name || 'knowledge-base'}.zip`)
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '打包下载失败'))
      }
    },

    downloadBlob(blob, fileName) {
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = fileName || 'download.bin'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
    },

    seedChatQuestion(row) {
      if (!row) return
      this.activeTab = 'chat'
      this.chatForm.content = `请基于文档《${row.title || `文档 #${row.id}` }》进行总结，并给出命中文档与切片来源。`
      this.$nextTick(() => this.scrollChatToBottom())
    },

    async loadMembers(silent = false) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.members = true
      try {
        const res = await listKnowledgeBaseMembers(this.currentKnowledgeBase.id)
        this.members = this.extractListData(res).map(this.normalizeMember)
      } catch (e) {
        if (!silent) {
          this.$message.error(this.extractResponseMessage(e, '加载成员失败'))
        }
      } finally {
        this.loading.members = false
      }
    },

    openMemberDialog() {
      if (!this.ensureCanManageCurrentMembers('添加成员')) return
      this.memberForm = this.getDefaultMemberForm()
      this.memberDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.memberFormRef) this.$refs.memberFormRef.clearValidate()
      })
    },

    submitMemberForm() {
      if (!this.ensureCanManageCurrentMembers('添加成员')) return
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!this.$refs.memberFormRef) return
      this.$refs.memberFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveMember = true
        try {
          await addKnowledgeBaseMember(this.currentKnowledgeBase.id, {
            ...this.memberForm
          })
          this.$message.success('成员已添加')
          this.memberDialogVisible = false
          this.loadMembers()
        } catch (e) {
          this.$message.error(this.extractResponseMessage(e, '添加成员失败'))
        } finally {
          this.loading.saveMember = false
        }
      })
    },

    async removeMember(row) {
      if (!this.ensureCanManageCurrentMembers('移除成员')) return
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await this.$confirm(`确定移除成员 ${row.userId} 吗？`, '提示', {
          type: 'warning'
        })
        await removeKnowledgeBaseMember(this.currentKnowledgeBase.id, row.id)
        this.$message.success('成员已移除')
        this.loadMembers()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error(this.extractResponseMessage(e, '移除成员失败'))
        }
      }
    },

    async reindexKnowledgeBase(row) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      const kb = row || this.currentKnowledgeBase
      if (!kb || !kb.id) return
      try {
        await createKnowledgeIndexTask(kb.id, {})
        this.$message.success('已提交知识库索引任务')
        this.openKnowledgeBaseTasks()
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '提交索引任务失败'))
      }
    },

    async reindexDocument(row) {
      if (!this.ensureCanEditCurrentKnowledgeBase('执行该操作')) return
      if (!row || !row.id || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await createKnowledgeIndexTask(this.currentKnowledgeBase.id, {
          documentId: row.id
        })
        this.$message.success('已提交文档索引任务')
        this.viewIndexTasks(row)
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '提交文档索引任务失败'))
      }
    },

    async loadIndexTasks(scope) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.tasks = true
      try {
        let res = null
        if (scope === 'document' && this.currentTaskDocument && this.currentTaskDocument.id) {
          res = await listDocumentIndexTasks(this.currentTaskDocument.id)
        } else {
          res = await listKnowledgeBaseIndexTasks(this.currentKnowledgeBase.id)
        }
        this.indexTasks = this.extractListData(res).map(this.normalizeTask)
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载索引任务失败'))
      } finally {
        this.loading.tasks = false
      }
    },

    openKnowledgeBaseTasks() {
      this.taskDrawerScope = 'knowledgeBase'
      this.currentTaskDocument = null
      this.taskDrawerVisible = true
      this.loadIndexTasks('knowledgeBase')
    },

    viewIndexTasks(row) {
      this.taskDrawerScope = 'document'
      this.currentTaskDocument = row
      this.taskDrawerVisible = true
      this.loadIndexTasks('document')
    },

    startTaskPolling() {
      this.stopTaskPolling()
      this.taskPollTimer = setInterval(() => {
        const scope = this.taskDrawerScope === 'document' ? 'document' : 'knowledgeBase'
        this.loadIndexTasks(scope)
      }, 4000)
    },

    stopTaskPolling() {
      if (this.taskPollTimer) {
        clearInterval(this.taskPollTimer)
        this.taskPollTimer = null
      }
    },

    getSessionStorageKey() {
      const kbId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
      const uid = this.chatForm.userId || this.ownerId || 'guest'
      if (!kbId) return ''
      return `kb_chat_session_${uid}_${kbId}`
    },

    persistSessionId(sessionId) {
      const key = this.getSessionStorageKey()
      if (!key || typeof localStorage === 'undefined') return
      if (sessionId) {
        localStorage.setItem(key, String(sessionId))
      } else {
        localStorage.removeItem(key)
      }
    },

    readPersistedSessionId() {
      const key = this.getSessionStorageKey()
      if (!key || typeof localStorage === 'undefined') return null
      const raw = localStorage.getItem(key)
      return raw ? Number(raw) || null : null
    },

    createNewSession(showMessage = true) {
      this.stopStream()
      this.selectedSessionId = null
      this.chatForm.sessionId = null
      this.chatMessages = []
      this.persistSessionId(null)
      if (showMessage) {
        this.$message.success('已切换为新会话')
      }
    },

    async loadSessions(autoRestore = true) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.sessionLoading = true
      try {
        const res = await pageAiSessions({
          page: this.sessionPagination.page,
          size: this.sessionPagination.size,
          userId: this.chatForm.userId || this.ownerId,
          bizType: this.chatForm.bizType || undefined,
          knowledgeBaseId: this.currentKnowledgeBase.id,
          status: 'ACTIVE'
        })
        const pageData = this.extractPageData(res)
        this.sessions = (pageData.content || [])
          .map(this.normalizeSession)
          .filter(item => item && item.id && !item.archived)
        this.sessionPagination.total = pageData.total || 0

        if (!autoRestore) return

        const rememberedId = this.readPersistedSessionId()
        const targetId = this.selectedSessionId || rememberedId
        if (!targetId) return

        const hit = this.sessions.find(item => item.id === targetId)
        if (hit) {
          await this.selectSession(hit, true)
        } else if (this.selectedSessionId === targetId) {
          this.createNewSession(false)
        }
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载会话失败'))
      } finally {
        this.sessionLoading = false
      }
    },

    async selectSession(item, silent = false) {
      if (!item || !item.id) return
      this.selectedSessionId = item.id
      this.chatForm.sessionId = item.id
      this.persistSessionId(item.id)
      await this.loadSessionMessages(item.id)
      if (!silent) {
        this.$message.success(`已打开会话 #${item.id}`)
      }
    },

    async loadSessionMessages(sessionId) {
      if (!sessionId) return
      try {
        const res = await pageAiSessionMessages(sessionId, {
          page: 0,
          size: 200
        })
        const pageData = this.extractPageData(res)
        this.chatMessages = (pageData.content || []).map(this.normalizeMessage)
        this.scrollChatToBottom()
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载会话消息失败'))
      }
    },

    async bindCurrentSessionKnowledgeBase(sessionId) {
      if (!sessionId || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await bindSessionKnowledgeBases(sessionId, [this.currentKnowledgeBase.id])
      } catch (e) {}
    },

    buildChatPayload() {
      const content = String(this.chatForm.content || '').trim()
      return {
        sessionId: this.selectedSessionId || this.chatForm.sessionId || null,
        userId: this.chatForm.userId || this.ownerId,
        content,
        modelId: this.effectiveChatModelId || null,
        promptTemplateId: this.chatForm.promptTemplateId || null,
        requestType: this.chatForm.requestType,
        knowledgeBaseIds: this.currentKnowledgeBase && this.currentKnowledgeBase.id ? [this.currentKnowledgeBase.id] : [],
        bizType: this.chatForm.bizType || 'GENERAL',
        bizId: this.chatForm.bizId || null,
        projectId: this.chatForm.projectId || this.routeProjectId || null,
        sceneCode: this.chatForm.sceneCode || 'knowledge.base',
        memoryMode: this.chatForm.memoryMode || 'SHORT',
        defaultKnowledgeBaseId: this.currentKnowledgeBase ? this.currentKnowledgeBase.id : null,
        sessionTitle: content.slice(0, 30)
      }
    },

    buildUserMessage(content) {
      return {
        id: `u-${Date.now()}`,
        role: 'user',
        content,
        totalTokens: null,
        latencyMs: null,
        modelName: '',
        callLogId: null,
        sources: [],
        sourceOpen: false
      }
    },

    buildAssistantMessage(data = {}) {
      return {
        id: data.assistantMessageId || `a-${Date.now()}`,
        role: 'assistant',
        content: data.content || '',
        totalTokens: data.totalTokens || null,
        latencyMs: data.latencyMs || null,
        modelName: data.modelName || '',
        callLogId: data.callLogId || null,
        finishReason: data.finishReason || '',
        sources: this.extractAnswerSources(data, { callLogId: data.callLogId || null }),
        sourceOpen: true
      }
    },

    async sendChat(isStream) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      const content = String(this.chatForm.content || '').trim()
      if (!content) {
        this.$message.warning('请输入问题')
        return
      }
      if (!this.effectiveChatModelId) {
        this.$message.warning('请先选择问答模型')
        return
      }
      if (this.loading.chat || this.loading.streamChat) return

      const payload = this.buildChatPayload()
      this.chatMessages.push(this.buildUserMessage(content))
      this.chatForm.content = ''
      this.scrollChatToBottom()

      if (isStream) {
        const assistant = this.buildAssistantMessage({})
        this.chatMessages.push(assistant)
        this.loading.streamChat = true

        const streamTask = streamChatWithKnowledgeBase({
          body: payload,
          onMessage: async chunk => {
            const data = this.extractResponseData(chunk) || chunk || {}
            if (typeof data === 'string') {
              assistant.content += data
              this.scrollChatToBottom()
              return
            }
            if (data.sessionId) {
              this.selectedSessionId = data.sessionId
              this.chatForm.sessionId = data.sessionId
              this.persistSessionId(data.sessionId)
            }
            if (data.delta) {
              assistant.content += data.delta
              this.scrollChatToBottom()
            }
            if (data.finished) {
              assistant.finishReason = data.finishReason || assistant.finishReason
            }
          },
          onError: err => {
            this.$message.error(this.extractResponseMessage(err, '流式发送失败'))
          },
          onFinish: async () => {
            this.loading.streamChat = false
            this.streamStopper = null
            if (this.selectedSessionId) {
              await this.bindCurrentSessionKnowledgeBase(this.selectedSessionId)
              await this.loadSessions(false)
            }
          }
        })

        this.streamStopper = streamTask && streamTask.abort ? streamTask.abort : null

        try {
          await streamTask
        } catch (e) {
          this.$message.error(this.extractResponseMessage(e, '流式发送失败'))
        } finally {
          this.loading.streamChat = false
          this.streamStopper = null
        }
        return
      }

      this.loading.chat = true
      try {
        const res = await chatWithKnowledgeBase(payload)
        const data = this.extractResponseData(res) || {}
        if (data.sessionId) {
          this.selectedSessionId = data.sessionId
          this.chatForm.sessionId = data.sessionId
          this.persistSessionId(data.sessionId)
        }
        this.chatMessages.push(this.buildAssistantMessage(data))
        if (this.selectedSessionId) {
          await this.bindCurrentSessionKnowledgeBase(this.selectedSessionId)
        }
        await this.loadSessions(false)
        this.scrollChatToBottom()
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '发送失败'))
      } finally {
        this.loading.chat = false
      }
    },

    clearChat() {
      this.chatMessages = []
      this.chatForm.content = ''
    },

    stopStream() {
      if (typeof this.streamStopper === 'function') {
        this.streamStopper()
      }
      this.streamStopper = null
      this.loading.streamChat = false
    },

    async archiveSession(item) {
      if (!item || !item.id) return
      try {
        await this.$confirm(`确定归档会话 #${item.id} 吗？`, '提示', {
          type: 'warning'
        })
        await archiveAiSession(item.id)
        this.$message.success('会话已归档')
        if (this.selectedSessionId === item.id) {
          this.createNewSession(false)
        }
        this.loadSessions(false)
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error(this.extractResponseMessage(e, '归档会话失败'))
        }
      }
    },

    async removeSession(item) {
      if (!item || !item.id) return
      try {
        await this.$confirm(`确定删除会话 #${item.id} 吗？`, '提示', {
          type: 'warning'
        })
        await deleteAiSession(item.id)
        this.$message.success('会话已删除')
        if (this.selectedSessionId === item.id) {
          this.createNewSession(false)
        }
        this.loadSessions(false)
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error(this.extractResponseMessage(e, '删除会话失败'))
        }
      }
    },

    toggleMessageSources(index) {
      const msg = this.chatMessages[index]
      if (!msg) return
      this.$set(msg, 'sourceOpen', !msg.sourceOpen)
    },

    locateSourceDocument(source) {
      if (!source || !source.documentId) return
      const hit = this.documents.find(item => item.id === source.documentId)
      if (hit) {
        this.viewChunks(hit)
        return
      }
      this.$message.info(`文档 ID：${source.documentId}`)
    },

    async openRetrievalDrawerByMessage(msg) {
      if (msg && msg.callLogId) {
        this.openRetrievalDrawer(msg.callLogId, '检索日志')
        return
      }
      this.$message.warning('当前消息没有关联检索日志')
    },

    async openRetrievalDrawer(callLogId, title) {
      if (!callLogId) {
        this.$message.warning('缺少 callLogId')
        return
      }
      this.retrievalDrawerVisible = true
      this.currentRetrievalMeta = title || `检索日志 #${callLogId}`
      this.loading.retrievals = true
      try {
        const res = await listCallRetrievals(callLogId)
        this.retrievalLogs = this.normalizeSources(this.extractListData(res), { callLogId })
      } catch (e) {
        this.$message.error(this.extractResponseMessage(e, '加载检索日志失败'))
      } finally {
        this.loading.retrievals = false
      }
    },

    scrollChatToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatMessagesRef
        if (el && typeof el.scrollTop === 'number') {
          el.scrollTop = el.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
.kb-page {
  padding: 16px;
}

.kb-card {
  border-radius: 12px;
}

.page-toolbar,
.tab-toolbar,
.kb-main__header,
.chat-sidebar__toolbar,
.chat-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-toolbar {
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-toolbar__left,
.page-toolbar__right,
.tab-toolbar__left,
.tab-toolbar__right,
.kb-main__header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.kb-layout {
  display: flex;
  gap: 16px;
  min-height: 720px;
}

.kb-sidebar {
  width: 320px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.kb-sidebar__title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.kb-list {
  flex: 1;
  overflow: auto;
  min-height: 0;
}

.kb-list-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.kb-list-item:hover,
.kb-list-item.active {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.12);
}

.kb-list-item__top,
.source-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kb-list-item__name,
.kb-main__title,
.session-item__title,
.source-card__title {
  font-weight: 600;
}

.kb-list-item__desc,
.kb-main__subtitle,
.text-muted,
.drawer-meta,
.session-item__meta,
.source-card__kb,
.source-card__meta,
.chat-hint {
  color: #909399;
  font-size: 12px;
}

.kb-list-item__desc {
  margin: 8px 0;
  line-height: 1.6;
  min-height: 40px;
}

.kb-list-item__meta,
.kb-list-item__actions,
.chat-message__footer,
.source-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.kb-main {
  flex: 1;
  min-width: 0;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  background: #fff;
}

.kb-main__header {
  margin-bottom: 16px;
}

.kb-main__title {
  font-size: 20px;
  margin-bottom: 6px;
}

.kb-descriptions {
  margin-bottom: 16px;
}

.kb-tabs {
  min-height: 540px;
}

.tab-toolbar {
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hidden-file-input {
  display: none;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.chat-config {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
}

.chat-layout {
  display: flex;
  gap: 16px;
  min-height: 560px;
}

.chat-sidebar {
  width: 280px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-sidebar__toolbar {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.session-list {
  flex: 1;
  overflow: auto;
  padding: 12px;
  min-height: 0;
}

.session-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover,
.session-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.session-pagination {
  padding: 8px 12px 12px;
  border-top: 1px solid #ebeef5;
}

.chat-main {
  flex: 1;
  min-width: 0;
}

.chat-box {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  min-height: 560px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px;
  background: #fafafa;
}

.chat-message {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.chat-message.user {
  background: #f5f7fa;
}

.chat-message.assistant {
  background: #ffffff;
}

.chat-message__role {
  font-weight: 600;
  margin-bottom: 8px;
}

.chat-message__content,
.chunk-content,
.source-card__content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.chat-message__footer {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.chat-message__sources {
  margin-top: 12px;
  border-top: 1px dashed #ebeef5;
  padding-top: 12px;
}

.sources-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-weight: 600;
}

.sources-header .is-open {
  transform: rotate(180deg);
}

.sources-list {
  margin-top: 10px;
}

.source-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fff;
}

.source-card__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-input {
  border-top: 1px solid #ebeef5;
  padding: 16px;
  background: #fff;
}

.chat-hint {
  margin-bottom: 10px;
}

.upload-tip {
  margin-top: 8px;
  color: #409eff;
  font-size: 12px;
}

.upload-tip--error {
  color: #f56c6c;
}

.drawer-meta {
  margin-bottom: 12px;
}
</style>