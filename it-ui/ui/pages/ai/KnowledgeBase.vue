<template>
  <div class="kb-page">
    <el-row :gutter="16">
      <el-col :xl="6" :lg="7" :md="8" :sm="24" :xs="24">
        <el-card shadow="never" class="kb-sidebar-card">
          <div class="sidebar-header">
            <div>
              <div class="page-title">知识库</div>
              <div class="page-subtitle">把文档、切片、索引与问答串成闭环</div>
            </div>
            <el-tag size="mini" type="success">AI</el-tag>
          </div>

          <div class="sidebar-toolbar">
            <el-input
              v-model.trim="keyword"
              clearable
              size="small"
              placeholder="搜索知识库名称或描述"
              prefix-icon="el-icon-search"
            />
          </div>

          <div class="sidebar-toolbar sidebar-toolbar--compact">
            <el-radio-group v-model="listMode" size="mini" :disabled="!!routeProjectId" @change="handleListModeChange">
              <el-radio-button label="owner">我的</el-radio-button>
              <el-radio-button label="project">项目</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="mini" icon="el-icon-plus" @click="openKbDialog('create')">新建</el-button>
            <el-button size="mini" icon="el-icon-refresh" @click="loadKnowledgeBases">刷新</el-button>
          </div>

          <div class="sidebar-form">
            <el-form label-position="top" size="small" :inline="false">
              <el-form-item v-if="listMode === 'owner'" label="拥有者 ID">
                <el-input-number
                  v-model="ownerId"
                  :min="1"
                  controls-position="right"
                  style="width: 100%"
                  @change="handleOwnerChange"
                />
              </el-form-item>
              <el-form-item v-else label="项目 ID">
                <el-input-number
                  v-model="projectId"
                  :min="1"
                  controls-position="right"
                  style="width: 100%"
                  @change="handleProjectChange"
                />
              </el-form-item>
            </el-form>
          </div>

          <div v-loading="loading.kbList" class="kb-list">
            <el-empty v-if="!filteredKnowledgeBases.length" description="暂无知识库" :image-size="72" />
            <div
              v-for="item in filteredKnowledgeBases"
              :key="item.id"
              class="kb-list-item"
              :class="{ active: currentKnowledgeBase && currentKnowledgeBase.id === item.id }"
              @click="selectKnowledgeBase(item)"
            >
              <div class="kb-list-item__top">
                <div class="kb-list-item__name">{{ item.name || `知识库 #${item.id}` }}</div>
                <el-tag size="mini" :type="item.scopeType === 'PROJECT' ? 'warning' : 'info'">
                  {{ item.scopeType || 'PERSONAL' }}
                </el-tag>
              </div>
              <div class="kb-list-item__desc">{{ item.description || '暂无描述' }}</div>
              <div class="kb-list-item__meta">
                <span>文档 {{ item.docCount || 0 }}</span>
                <span>切片 {{ item.chunkCount || 0 }}</span>
                <span>{{ item.visibility || 'PRIVATE' }}</span>
              </div>
            </div>
          </div>

          <div class="sidebar-pagination">
            <el-pagination
              background
              small
              layout="prev, pager, next"
              :current-page="pagination.page + 1"
              :page-size="pagination.size"
              :total="pagination.total"
              @current-change="handleKnowledgeBasePageChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xl="18" :lg="17" :md="16" :sm="24" :xs="24">
        <el-card shadow="never" class="kb-main-card">
          <div class="main-toolbar">
            <div>
              <div class="main-title">{{ currentKnowledgeBase ? currentKnowledgeBase.name : '未选择知识库' }}</div>
              <div class="main-subtitle">
                {{ currentKnowledgeBase ? (currentKnowledgeBase.description || '暂无描述') : '先从左侧选择或创建一个知识库' }}
              </div>
            </div>
            <div class="main-actions">
              <el-button size="mini" :disabled="!currentKnowledgeBase" icon="el-icon-edit" @click="openKbDialog('edit', currentKnowledgeBase)">
                编辑知识库
              </el-button>
              <el-button size="mini" type="primary" :disabled="!currentKnowledgeBase" icon="el-icon-document-add" @click="openDocumentDialog">
                新增文档
              </el-button>
              <el-button size="mini" :disabled="!currentKnowledgeBase" icon="el-icon-user" @click="openMemberDialog">添加成员</el-button>
              <el-button size="mini" :disabled="!currentKnowledgeBase" icon="el-icon-refresh-right" @click="handleReindex">重建索引</el-button>
              <el-button size="mini" :disabled="!currentKnowledgeBase" icon="el-icon-refresh" @click="reloadCurrentKnowledgeBase">刷新详情</el-button>
            </div>
          </div>

          <el-empty
            v-if="!currentKnowledgeBase"
            class="main-empty"
            description="请先从左侧选择一个知识库"
            :image-size="96"
          />

          <template v-else>
            <el-alert
              v-if="taskHintText"
              class="task-alert"
              type="info"
              :closable="false"
              show-icon
              :title="taskHintText"
            />

            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库 ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="拥有者">{{ currentKnowledgeBase.ownerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="作用域">{{ currentKnowledgeBase.scopeType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ currentKnowledgeBase.visibility || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分块策略">{{ currentKnowledgeBase.chunkStrategy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认 TopK">{{ currentKnowledgeBase.defaultTopK || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Provider">{{ currentKnowledgeBase.embeddingProvider || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Model">{{ currentKnowledgeBase.embeddingModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="文档数">{{ currentKnowledgeBase.docCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="切片数">{{ currentKnowledgeBase.chunkCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="最后索引时间">{{ formatTime(currentKnowledgeBase.lastIndexedAt) }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ currentKnowledgeBase.status || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-tabs v-model="activeTab" class="kb-tabs">
              <el-tab-pane label="文档管理" name="documents">
                <div class="tab-toolbar">
                  <div class="tab-toolbar__left">
                    <el-button type="primary" size="small" @click="openDocumentDialog">新增文档</el-button>
                    <el-button size="small" @click="triggerLocalFileSelect">导入本地文本</el-button>
                    <input
                      ref="localFileInput"
                      class="hidden-file-input"
                      type="file"
                      accept=".txt,.md,.markdown,.json,.csv,.js,.java,.xml,.html,.htm,.css,.vue,.sql,.yml,.yaml"
                      @change="handleLocalFileChange"
                    />
                    <el-button size="small" @click="loadDocuments">刷新文档</el-button>
                    <el-button size="small" @click="openKnowledgeBaseTasks">索引任务</el-button>
                  </div>
                  <div class="tab-toolbar__right text-muted">
                    当前控制器是 JSON 文本入库，这个版本把“上传/导入”改成前端读取文本后再提交。
                  </div>
                </div>

                <el-table v-loading="loading.documents" :data="documents" border stripe size="small">
                  <el-table-column prop="id" label="ID" width="80" />
                  <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
                  <el-table-column prop="sourceType" label="来源" width="120" />
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
                  <el-table-column label="操作" min-width="300" fixed="right">
                    <template slot-scope="{ row }">
                      <el-button type="text" size="small" @click="viewChunks(row)">查看切片</el-button>
                      <el-button type="text" size="small" @click="reindexDocument(row)">重建索引</el-button>
                      <el-button type="text" size="small" @click="viewIndexTasks(row)">索引记录</el-button>
                      <el-button type="text" size="small" @click="seedChatQuestion(row)">引用到提问</el-button>
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
                    <el-button type="primary" size="small" @click="openMemberDialog">添加成员</el-button>
                    <el-button size="small" @click="loadMembers">刷新成员</el-button>
                  </div>
                </div>

                <el-table v-loading="loading.members" :data="members" border stripe size="small">
                  <el-table-column prop="id" label="ID" width="80" />
                  <el-table-column prop="userId" label="用户 ID" width="120" />
                  <el-table-column prop="roleCode" label="角色" width="120" />
                  <el-table-column prop="grantedBy" label="授权人" width="120" />
                  <el-table-column label="创建时间" min-width="180">
                    <template slot-scope="{ row }">{{ formatTime(row.createdAt) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="120" fixed="right">
                    <template slot-scope="{ row }">
                      <el-button type="text" size="small" :disabled="row.roleCode === 'OWNER'" @click="removeMember(row)">
                        移除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="知识库问答" name="chat">
                <div class="chat-config">
                  <el-form :inline="true" size="small">
                    <el-form-item label="用户 ID">
                      <el-input-number v-model="chatForm.userId" :min="1" controls-position="right" />
                    </el-form-item>
                    <el-form-item label="模型 ID">
                      <el-input-number v-model="chatForm.modelId" :min="1" controls-position="right" />
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
                      <el-empty v-if="!filteredSessions.length" description="暂无会话历史" :image-size="60" />
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
                  </div>

                  <div class="chat-main">
                    <div class="chat-box">
                      <div class="chat-messages">
                        <el-empty v-if="!chatMessages.length" description="先问一个问题，回答里会展示命中来源与切片内容" :image-size="80" />
                        <div
                          v-for="(msg, index) in chatMessages"
                          :key="msg.id || index"
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

                          <div v-if="msg.role === 'assistant' && msg.sources && msg.sources.length" class="chat-message__sources">
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
                                <div v-if="source.knowledgeBaseName" class="source-card__kb">知识库：{{ source.knowledgeBaseName }}</div>
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
                          <el-button type="primary" :loading="loading.streamChat" @click="sendChat(true)">流式发送</el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      :title="kbDialogMode === 'create' ? '新建知识库' : '编辑知识库'"
      :visible.sync="kbDialogVisible"
      width="760px"
      @closed="resetKbForm"
    >
      <el-form ref="kbFormRef" :model="kbForm" :rules="kbRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model.trim="kbForm.name" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="拥有者 ID" prop="ownerId">
              <el-input-number v-model="kbForm.ownerId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

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

        <el-form-item label="描述">
          <el-input v-model.trim="kbForm.description" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="kbDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveKb" @click="submitKbForm">保存</el-button>
      </div>
    </el-dialog>

    <el-dialog title="新增文档 / 上传导入" :visible.sync="documentDialogVisible" width="860px" @closed="resetDocumentForm">
      <el-form ref="documentFormRef" :model="documentForm" :rules="documentRules" label-width="110px">
        <el-alert
          class="dialog-alert"
          type="info"
          :closable="false"
          show-icon
          title="这个页面现在支持三种录入方式：手工粘贴、本地文本文件导入、业务引用导入。因为当前公开控制器是 JSON 入库，所以本地文件会先在前端读取成文本再提交。"
        />

        <el-form-item label="录入方式">
          <el-radio-group v-model="documentImportMode" size="small">
            <el-radio-button label="manual">手工粘贴</el-radio-button>
            <el-radio-button label="upload">本地文件导入</el-radio-button>
            <el-radio-button label="reference">业务引用导入</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model.trim="documentForm.title" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="documentForm.sourceType" style="width: 100%">
                <el-option label="MANUAL" value="MANUAL" />
                <el-option label="UPLOAD" value="UPLOAD" />
                <el-option label="PROJECT_DOC" value="PROJECT_DOC" />
                <el-option label="BLOG" value="BLOG" />
                <el-option label="CIRCLE" value="CIRCLE" />
                <el-option label="PAID_CONTENT" value="PAID_CONTENT" />
                <el-option label="URL" value="URL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="documentImportMode === 'upload'">
          <el-form-item label="本地文件">
            <div class="upload-box">
              <div class="upload-box__left">
                <el-button type="primary" plain size="small" @click="triggerLocalFileSelect">选择文件</el-button>
                <el-button v-if="selectedLocalFileName" size="small" @click="clearSelectedFile">清空</el-button>
              </div>
              <div class="upload-box__right">
                <div v-if="selectedLocalFileName" class="upload-file-name">已选择：{{ selectedLocalFileName }}</div>
                <div v-else class="text-muted">支持 txt、md、json、csv、html、js、java、xml、yml 等文本文件</div>
                <div v-if="fileReadError" class="file-error">{{ fileReadError }}</div>
              </div>
            </div>
          </el-form-item>
        </template>

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

        <el-form-item v-else-if="documentImportMode !== 'reference'" label="来源引用 ID">
          <el-input-number v-model="documentForm.sourceRefId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>

        <el-form-item label="正文" prop="contentText">
          <el-input
            v-model="documentForm.contentText"
            type="textarea"
            :rows="15"
            placeholder="手工粘贴正文，或通过本地文件导入后在这里预览/微调。"
          />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="documentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveDocument" @click="submitDocumentForm">保存并入库</el-button>
      </div>
    </el-dialog>

    <el-dialog title="添加成员" :visible.sync="memberDialogVisible" width="480px" @closed="resetMemberForm">
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
        <el-button type="primary" :loading="loading.saveMember" @click="submitMemberForm">添加</el-button>
      </div>
    </el-dialog>

    <el-drawer title="文档切片" :visible.sync="chunkDrawerVisible" size="42%" direction="rtl">
      <div class="drawer-headline">
        <div class="drawer-subtitle">{{ activeChunkDocument ? activeChunkDocument.title : '当前文档' }}</div>
      </div>
      <div class="drawer-body" v-loading="loading.chunks">
        <el-empty v-if="!chunks.length" description="暂无切片" :image-size="80" />
        <div v-for="item in chunks" :key="item.id" class="chunk-card">
          <div class="chunk-card__header">
            <span>Chunk #{{ item.chunkIndex }}</span>
            <el-tag size="mini" effect="plain">Token {{ item.tokenCount || 0 }}</el-tag>
          </div>
          <div class="chunk-card__content">{{ item.content }}</div>
        </div>
      </div>
    </el-drawer>

    <el-drawer :title="taskDrawerTitle" :visible.sync="taskDrawerVisible" size="38%" direction="rtl">
      <div class="drawer-headline">
        <div class="drawer-subtitle">{{ taskDrawerSubtitle }}</div>
        <el-button size="mini" @click="refreshCurrentTasks(true)">立即刷新</el-button>
      </div>
      <div class="drawer-body" v-loading="loading.tasks">
        <el-empty v-if="!indexTasks.length" description="暂无任务记录" :image-size="80" />
        <div v-for="item in indexTasks" :key="item.id" class="task-card">
          <div class="task-card__top">
            <span>{{ item.taskType }}</span>
            <el-tag size="mini" :type="taskStatusTagType(item.status)">{{ item.status }}</el-tag>
          </div>
          <div class="task-card__row">创建时间：{{ formatTime(item.createdAt) }}</div>
          <div class="task-card__row">开始时间：{{ formatTime(item.startedAt) }}</div>
          <div class="task-card__row">结束时间：{{ formatTime(item.finishedAt) }}</div>
          <div class="task-card__row">重试次数：{{ item.retryCount || 0 }}</div>
          <div class="task-card__row">错误信息：{{ item.errorMessage || '无' }}</div>
        </div>
      </div>
    </el-drawer>

    <el-drawer title="检索日志 / 命中切片" :visible.sync="retrievalDrawerVisible" size="42%" direction="rtl">
      <div class="drawer-headline">
        <div class="drawer-subtitle">{{ currentRetrievalMeta }}</div>
      </div>
      <div class="drawer-body" v-loading="loading.retrievals">
        <el-empty v-if="!retrievalLogs.length" description="暂无检索日志" :image-size="80" />
        <div v-for="item in retrievalLogs" :key="item.id" class="retrieval-card">
          <div class="retrieval-card__top">
            <div class="retrieval-card__title">{{ item.title || '未命名文档' }}</div>
            <el-tag size="mini" effect="plain">#{{ item.rankNo || item.chunkIndex || '-' }}</el-tag>
          </div>
          <div class="retrieval-card__meta">
            <span v-if="item.knowledgeBaseName">知识库：{{ item.knowledgeBaseName }}</span>
            <span v-if="item.score !== null && item.score !== undefined">Score {{ item.score }}</span>
            <span v-if="item.retrievalMethod">{{ item.retrievalMethod }}</span>
          </div>
          <div class="retrieval-card__content">{{ item.content || '暂无切片内容' }}</div>
          <div class="retrieval-card__actions">
            <el-button type="text" size="mini" @click="locateSourceDocument(item)">定位文档</el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {
  pageKnowledgeBasesByOwner,
  pageKnowledgeBasesByProject,
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  pageKnowledgeDocuments,
  addKnowledgeDocument,
  listDocumentChunks,
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
  pageSessionCallLogs,
  listCallRetrievals,
  streamChatWithKnowledgeBase
} from '@/api/knowledgeBase'

export default {
  name: 'KnowledgeBasePage',
  data() {
    return {
      listMode: 'owner',
      keyword: '',
      ownerId: 1,
      projectId: 1,
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
        retrievals: false
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
        size: 20,
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
        ownerId: 1,
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
        userId: 1,
        content: '',
        modelId: 1,
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
      kbRules: {
        name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
        ownerId: [{ required: true, message: '请输入拥有者 ID', trigger: 'change' }],
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
    taskDrawerTitle() {
      return this.taskDrawerScope === 'document' ? '文档索引任务' : '知识库索引任务'
    },
    taskDrawerSubtitle() {
      if (this.taskDrawerScope === 'document' && this.currentTaskDocument) {
        return this.currentTaskDocument.title || `文档 #${this.currentTaskDocument.id}`
      }
      return this.currentKnowledgeBase ? this.currentKnowledgeBase.name : '当前知识库'
    },
    taskHintText() {
      const running = (this.indexTasks || []).find(item => item.status === 'PENDING' || item.status === 'RUNNING')
      if (!running) return ''
      return `当前有索引任务仍在执行：${running.taskType} / ${running.status}，右侧抽屉会自动刷新状态。`
    }
  },
  watch: {
    taskDrawerVisible(val) {
      if (val) {
        this.startTaskPolling()
      } else {
        this.stopTaskPolling()
      }
    }
  },
  mounted() {
    this.kbForm = this.getDefaultKbForm()
    this.documentForm = this.getDefaultDocumentForm()
    this.memberForm = this.getDefaultMemberForm()
    this.initRouteContext()
    this.initUserId()
    this.loadKnowledgeBases()
  },
  beforeDestroy() {
    this.stopTaskPolling()
    this.stopStream()
  },
  methods: {
    getDefaultKbForm() {
      return {
        scopeType: 'PERSONAL',
        projectId: this.routeProjectId || null,
        ownerId: this.ownerId || 1,
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
        data.references ||
        data.retrievals ||
        data.retrievedChunks ||
        data.citations ||
        data.quotedChunks ||
        []
      return this.normalizeSources(sourceList, extra)
    },
    normalizeMessage(raw = {}) {
      const roleRaw = String(raw.role || raw.roleCode || raw.messageRole || '').toUpperCase()
      const role = roleRaw === 'USER' || roleRaw === 'HUMAN' ? 'user' : 'assistant'
      return {
        id: raw.id || raw.messageId || null,
        role,
        content: raw.content || raw.messageContent || raw.answer || raw.text || '',
        sources: this.extractAnswerSources(raw, { callLogId: raw.callLogId || raw.logId || null }),
        sourceOpen: false,
        callLogId: raw.callLogId || raw.logId || null,
        createdAt: raw.createdAt || raw.createTime || null,
        modelName: raw.modelName || (raw.model && raw.model.name) || '',
        promptTokens: raw.promptTokens || null,
        completionTokens: raw.completionTokens || null,
        totalTokens: raw.totalTokens || null,
        latencyMs: raw.latencyMs || null
      }
    },
    normalizeRetrievalLogs(list = [], callLogId = null) {
      return this.normalizeSources(list, { callLogId }).map(item => ({
        ...item,
        score: item.score !== undefined && item.score !== null ? item.score : null
      }))
    },
    formatTime(value) {
      if (!value) return '-'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return value
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      const hh = String(date.getHours()).padStart(2, '0')
      const mm = String(date.getMinutes()).padStart(2, '0')
      const ss = String(date.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
    },
    docStatusTagType(status) {
      const map = {
        INDEXED: 'success',
        UPLOADED: 'info',
        PARSING: 'warning',
        FAILED: 'danger',
        DISABLED: ''
      }
      return map[status] || 'info'
    },
    taskStatusTagType(status) {
      const map = {
        SUCCESS: 'success',
        RUNNING: 'warning',
        PENDING: 'info',
        FAILED: 'danger'
      }
      return map[status] || 'info'
    },
    handleListModeChange() {
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },
    handleOwnerChange() {
      if (this.listMode !== 'owner') return
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },
    handleProjectChange() {
      if (this.listMode !== 'project') return
      this.pagination.page = 0
      this.loadKnowledgeBases()
    },
    handleKnowledgeBasePageChange(page) {
      this.pagination.page = page - 1
      this.loadKnowledgeBases()
    },
    handleDocumentPageChange(page) {
      this.documentPagination.page = page - 1
      this.loadDocuments()
    },
    async loadKnowledgeBases() {
      this.loading.kbList = true
      try {
        let res
        if (this.listMode === 'project') {
          res = await pageKnowledgeBasesByProject(this.projectId, {
            page: this.pagination.page,
            size: this.pagination.size
          })
        } else {
          res = await pageKnowledgeBasesByOwner(this.ownerId, {
            page: this.pagination.page,
            size: this.pagination.size
          })
        }
        const pageData = this.extractPageData(res)
        this.knowledgeBases = pageData.content || []
        this.pagination.total = pageData.total || 0
        if (this.knowledgeBases.length) {
          const currentId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
          const matched = this.knowledgeBases.find(item => item.id === currentId)
          await this.selectKnowledgeBase(matched || this.knowledgeBases[0], false)
        } else {
          this.currentKnowledgeBase = null
          this.documents = []
          this.members = []
          this.sessions = []
          this.chatMessages = []
          this.indexTasks = []
        }
      } catch (e) {
        this.$message.error('加载知识库失败')
      } finally {
        this.loading.kbList = false
      }
    },
    async selectKnowledgeBase(item, forceLoadDetail = true) {
      if (!item) return
      this.currentKnowledgeBase = item
      if (forceLoadDetail) {
        await this.loadKnowledgeBaseDetail(item.id)
      }
      this.chatForm.projectId = item.projectId || this.chatForm.projectId || this.routeProjectId || null
      await Promise.all([this.loadDocuments(), this.loadMembers(), this.loadSessions()])
      if (this.chatForm.sessionId) {
        await this.bindCurrentSessionKnowledgeBases()
      }
      if (this.taskDrawerVisible && this.taskDrawerScope === 'knowledgeBase') {
        await this.refreshCurrentTasks(false)
      }
    },
    async reloadCurrentKnowledgeBase() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      await this.loadKnowledgeBaseDetail(this.currentKnowledgeBase.id)
      await Promise.all([this.loadDocuments(), this.loadMembers(), this.loadSessions()])
      if (this.taskDrawerVisible) {
        await this.refreshCurrentTasks(false)
      }
    },
    async loadKnowledgeBaseDetail(id) {
      try {
        const res = await getKnowledgeBase(id)
        const detail = this.extractResponseData(res)
        this.currentKnowledgeBase = detail || this.currentKnowledgeBase
      } catch (e) {
        this.$message.error('加载知识库详情失败')
      }
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
        this.documents = pageData.content || []
        this.documentPagination.total = pageData.total || 0
      } catch (e) {
        this.$message.error('加载文档失败')
      } finally {
        this.loading.documents = false
      }
    },
    async loadMembers() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.members = true
      try {
        const res = await listKnowledgeBaseMembers(this.currentKnowledgeBase.id)
        this.members = this.extractResponseData(res) || []
      } catch (e) {
        this.$message.error('加载成员失败')
      } finally {
        this.loading.members = false
      }
    },
    async loadSessions() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.sessions = []
        return
      }
      this.sessionLoading = true
      try {
        const res = await pageAiSessions({
          userId: this.chatForm.userId,
          page: this.sessionPagination.page,
          size: this.sessionPagination.size
        })
        const pageData = this.extractPageData(res)
        const currentKbId = this.currentKnowledgeBase.id
        let list = Array.isArray(pageData.content) ? pageData.content.map(this.normalizeSession) : []
        list = list.filter(item => {
          const ids = Array.isArray(item.knowledgeBaseIds) ? item.knowledgeBaseIds.filter(Boolean) : []
          if (ids.length) return ids.includes(currentKbId)
          if (item.defaultKnowledgeBaseId) return item.defaultKnowledgeBaseId === currentKbId
          return true
        })
        this.sessions = list
        this.sessionPagination.total = list.length
      } catch (e) {
        this.$message.error('加载会话失败')
      } finally {
        this.sessionLoading = false
      }
    },
    async selectSession(item) {
      if (!item || !item.id) return
      this.selectedSessionId = item.id
      this.chatForm.sessionId = item.id
      await this.bindCurrentSessionKnowledgeBases()
      await this.loadSessionMessages(item.id)
    },
    async loadSessionMessages(sessionId) {
      if (!sessionId) return
      try {
        const res = await pageAiSessionMessages(sessionId, { page: 0, size: 100 })
        const pageData = this.extractPageData(res)
        const list = Array.isArray(pageData.content) ? pageData.content.map(this.normalizeMessage) : []
        this.chatMessages = list
      } catch (e) {
        this.$message.error('加载会话消息失败')
      }
    },
    openKbDialog(mode, item) {
      this.kbDialogMode = mode
      if (mode === 'edit' && item) {
        this.kbForm = {
          ...this.getDefaultKbForm(),
          ...item,
          ownerId: item.ownerId || this.ownerId || 1,
          projectId: item.projectId || this.routeProjectId || null
        }
      } else {
        this.kbForm = this.getDefaultKbForm()
      }
      if (this.routeProjectId) {
        this.kbForm.scopeType = 'PROJECT'
        this.kbForm.projectId = this.routeProjectId
        this.kbForm.sourceType = 'PROJECT_DOC'
      }
      this.kbDialogVisible = true
      this.$nextTick(() => {
        this.$refs.kbFormRef && this.$refs.kbFormRef.clearValidate()
      })
    },
    resetKbForm() {
      this.kbForm = this.getDefaultKbForm()
      this.$refs.kbFormRef && this.$refs.kbFormRef.clearValidate()
    },
    submitKbForm() {
      this.$refs.kbFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveKb = true
        try {
          const payload = { ...this.kbForm }
          if (payload.scopeType !== 'PROJECT') {
            payload.projectId = null
          }
          if (this.kbDialogMode === 'edit' && payload.id) {
            await updateKnowledgeBase(payload.id, payload)
          } else {
            await createKnowledgeBase(payload)
          }
          this.$message.success(this.kbDialogMode === 'edit' ? '知识库更新成功' : '知识库创建成功')
          this.kbDialogVisible = false
          await this.loadKnowledgeBases()
        } catch (e) {
          this.$message.error('保存知识库失败')
        } finally {
          this.loading.saveKb = false
        }
      })
    },
    openDocumentDialog() {
      this.documentImportMode = 'manual'
      this.documentForm = this.getDefaultDocumentForm()
      if (this.routeProjectId) {
        this.documentForm.sourceType = 'PROJECT_DOC'
      }
      this.selectedLocalFileName = ''
      this.fileReadError = ''
      this.documentDialogVisible = true
      this.$nextTick(() => {
        this.$refs.documentFormRef && this.$refs.documentFormRef.clearValidate()
      })
    },
    resetDocumentForm() {
      this.documentImportMode = 'manual'
      this.documentForm = this.getDefaultDocumentForm()
      this.selectedLocalFileName = ''
      this.fileReadError = ''
      if (this.$refs.localFileInput) {
        this.$refs.localFileInput.value = ''
      }
      this.$refs.documentFormRef && this.$refs.documentFormRef.clearValidate()
    },
    triggerLocalFileSelect() {
      this.documentDialogVisible = true
      this.documentImportMode = 'upload'
      this.$nextTick(() => {
        if (this.$refs.localFileInput) {
          this.$refs.localFileInput.click()
        }
      })
    },
    clearSelectedFile() {
      this.selectedLocalFileName = ''
      this.fileReadError = ''
      this.documentForm.contentText = ''
      if (this.$refs.localFileInput) {
        this.$refs.localFileInput.value = ''
      }
    },
    readFileTitle(name) {
      if (!name) return '未命名文档'
      const index = name.lastIndexOf('.')
      return index > 0 ? name.slice(0, index) : name
    },
    handleLocalFileChange(event) {
      const file = event && event.target && event.target.files ? event.target.files[0] : null
      if (!file) return
      const name = file.name || ''
      const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : ''
      const unsupported = ['pdf', 'doc', 'docx']
      if (unsupported.includes(ext)) {
        this.fileReadError = '当前版本没有接入后端 Multipart 文件解析接口，暂不支持 pdf/doc/docx 直接导入，请先转换成 txt 或 md。'
        return
      }
      this.fileReadError = ''
      this.selectedLocalFileName = name
      this.documentImportMode = 'upload'
      this.documentForm.sourceType = 'UPLOAD'
      if (!this.documentForm.title) {
        this.documentForm.title = this.readFileTitle(name)
      }
      const reader = new FileReader()
      reader.onload = () => {
        const text = typeof reader.result === 'string' ? reader.result : ''
        this.documentForm.contentText = text
        this.documentForm.contentHash = null
        this.$nextTick(() => {
          this.$refs.documentFormRef && this.$refs.documentFormRef.clearValidate()
        })
      }
      reader.onerror = () => {
        this.fileReadError = '文件读取失败，请重试。'
      }
      reader.readAsText(file, 'utf-8')
    },
    submitDocumentForm() {
      this.$refs.documentFormRef.validate(async valid => {
        if (!valid) return
        if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
          this.$message.warning('请先选择知识库')
          return
        }
        this.loading.saveDocument = true
        try {
          const payload = {
            ...this.documentForm,
            title: String(this.documentForm.title || '').trim(),
            contentText: String(this.documentForm.contentText || '').trim()
          }
          if (!payload.contentText) {
            this.$message.warning('文档正文不能为空')
            this.loading.saveDocument = false
            return
          }
          const res = await addKnowledgeDocument(this.currentKnowledgeBase.id, payload)
          this.$message.success(this.extractResponseMessage(res, '文档入库成功'))
          this.documentDialogVisible = false
          await this.loadDocuments()
          await this.loadKnowledgeBaseDetail(this.currentKnowledgeBase.id)
        } catch (e) {
          this.$message.error('保存文档失败')
        } finally {
          this.loading.saveDocument = false
        }
      })
    },
    openMemberDialog() {
      this.memberForm = this.getDefaultMemberForm()
      this.memberDialogVisible = true
      this.$nextTick(() => {
        this.$refs.memberFormRef && this.$refs.memberFormRef.clearValidate()
      })
    },
    resetMemberForm() {
      this.memberForm = this.getDefaultMemberForm()
      this.$refs.memberFormRef && this.$refs.memberFormRef.clearValidate()
    },
    submitMemberForm() {
      this.$refs.memberFormRef.validate(async valid => {
        if (!valid) return
        if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
          this.$message.warning('请先选择知识库')
          return
        }
        this.loading.saveMember = true
        try {
          await addKnowledgeBaseMember(this.currentKnowledgeBase.id, { ...this.memberForm })
          this.$message.success('成员添加成功')
          this.memberDialogVisible = false
          await this.loadMembers()
        } catch (e) {
          this.$message.error('添加成员失败')
        } finally {
          this.loading.saveMember = false
        }
      })
    },
    async removeMember(row) {
      if (!row || !row.id || !this.currentKnowledgeBase) return
      try {
        await this.$confirm('确认移除该成员吗？', '提示', { type: 'warning' })
        await removeKnowledgeBaseMember(this.currentKnowledgeBase.id, row.id)
        this.$message.success('成员已移除')
        await this.loadMembers()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('移除成员失败')
        }
      }
    },
    async viewChunks(row) {
      if (!row || !row.id) return
      this.activeChunkDocument = row
      this.loading.chunks = true
      this.chunkDrawerVisible = true
      try {
        const res = await listDocumentChunks(row.id)
        this.chunks = this.extractResponseData(res) || []
      } catch (e) {
        this.$message.error('加载切片失败')
      } finally {
        this.loading.chunks = false
      }
    },
    async viewIndexTasks(row) {
      if (!row || !row.id) return
      this.taskDrawerScope = 'document'
      this.currentTaskDocument = row
      this.taskDrawerVisible = true
      await this.refreshCurrentTasks(false)
    },
    async openKnowledgeBaseTasks() {
      this.taskDrawerScope = 'knowledgeBase'
      this.currentTaskDocument = null
      this.taskDrawerVisible = true
      await this.refreshCurrentTasks(false)
    },
    async refreshCurrentTasks(showToast) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      this.loading.tasks = true
      try {
        let res
        if (this.taskDrawerScope === 'document' && this.currentTaskDocument && this.currentTaskDocument.id) {
          res = await listDocumentIndexTasks(this.currentTaskDocument.id)
        } else {
          res = await listKnowledgeBaseIndexTasks(this.currentKnowledgeBase.id)
        }
        this.indexTasks = this.extractResponseData(res) || []
        if (showToast) {
          this.$message.success('索引任务已刷新')
        }
      } catch (e) {
        this.$message.error('加载索引任务失败')
      } finally {
        this.loading.tasks = false
      }
    },
    startTaskPolling() {
      this.stopTaskPolling()
      this.taskPollTimer = setInterval(() => {
        if (this.taskDrawerVisible) {
          this.refreshCurrentTasks(false)
        }
      }, 5000)
    },
    stopTaskPolling() {
      if (this.taskPollTimer) {
        clearInterval(this.taskPollTimer)
        this.taskPollTimer = null
      }
    },
    async reindexDocument(row) {
      if (!row || !row.id || !this.currentKnowledgeBase) return
      try {
        await createKnowledgeIndexTask(this.currentKnowledgeBase.id, {
          documentId: row.id,
          taskType: 'REINDEX'
        })
        this.$message.success('已发起文档重建索引')
        this.taskDrawerScope = 'document'
        this.currentTaskDocument = row
        this.taskDrawerVisible = true
        await this.refreshCurrentTasks(false)
        await this.loadDocuments()
      } catch (e) {
        this.$message.error('发起文档索引失败')
      }
    },
    async handleReindex() {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      try {
        await createKnowledgeIndexTask(this.currentKnowledgeBase.id, {
          taskType: 'REINDEX'
        })
        this.$message.success('已发起知识库重建索引')
        this.taskDrawerScope = 'knowledgeBase'
        this.currentTaskDocument = null
        this.taskDrawerVisible = true
        await this.refreshCurrentTasks(false)
      } catch (e) {
        this.$message.error('发起知识库索引失败')
      }
    },
    seedChatQuestion(row) {
      if (!row) return
      this.activeTab = 'chat'
      const title = row.title || `文档 #${row.id}`
      const template = `请基于文档《${title}》总结要点，并在回答里标注命中的来源片段。`
      this.chatForm.content = template
    },
    clearChat() {
      this.stopStream()
      this.chatMessages = []
      this.chatForm.content = ''
    },
    stopStream() {
      if (this.streamStopper) {
        this.streamStopper()
        this.streamStopper = null
      }
    },
    toggleMessageSources(index) {
      const msg = this.chatMessages[index]
      if (!msg) return
      this.$set(this.chatMessages[index], 'sourceOpen', !msg.sourceOpen)
    },
    buildChatRequest(content) {
      return {
        sessionId: this.chatForm.sessionId || null,
        userId: this.chatForm.userId,
        content,
        modelId: this.chatForm.modelId || null,
        promptTemplateId: this.chatForm.promptTemplateId || null,
        requestType: this.chatForm.requestType,
        knowledgeBaseIds: this.currentKnowledgeBase ? [this.currentKnowledgeBase.id] : [],
        bizType: this.chatForm.bizType,
        bizId: this.chatForm.bizId || null,
        projectId: this.chatForm.projectId || this.routeProjectId || null,
        sceneCode: this.chatForm.sceneCode || 'knowledge.base',
        memoryMode: this.chatForm.memoryMode || 'SHORT',
        defaultKnowledgeBaseId: this.currentKnowledgeBase ? this.currentKnowledgeBase.id : null
      }
    },
    createLocalUserMessage(content) {
      return {
        id: `user-${Date.now()}`,
        role: 'user',
        content,
        createdAt: new Date().toISOString(),
        sources: [],
        sourceOpen: false,
        callLogId: null,
        totalTokens: null,
        latencyMs: null,
        modelName: ''
      }
    },
    createLocalAssistantMessage(content = '', extra = {}) {
      return {
        id: extra.id || `assistant-${Date.now()}-${Math.random().toString(16).slice(2)}`,
        role: 'assistant',
        content,
        createdAt: new Date().toISOString(),
        sources: Array.isArray(extra.sources) ? extra.sources : [],
        sourceOpen: Array.isArray(extra.sources) && extra.sources.length > 0,
        callLogId: extra.callLogId || null,
        totalTokens: extra.totalTokens || null,
        latencyMs: extra.latencyMs || null,
        modelName: extra.modelName || ''
      }
    },
    async bindCurrentSessionKnowledgeBases() {
      if (!this.chatForm.sessionId || !this.currentKnowledgeBase || !this.currentKnowledgeBase.id) return
      try {
        await bindSessionKnowledgeBases(this.chatForm.sessionId, [this.currentKnowledgeBase.id])
      } catch (e) {}
    },
    async sendChat(useStream) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!this.chatForm.userId) {
        this.$message.warning('请先填写用户 ID')
        return
      }
      const question = String(this.chatForm.content || '').trim()
      if (!question) {
        this.$message.warning('请输入问题')
        return
      }

      this.stopStream()
      this.chatMessages.push(this.createLocalUserMessage(question))
      this.chatForm.content = ''

      if (useStream) {
        this.loading.streamChat = true
        const assistantMessage = this.createLocalAssistantMessage('')
        this.chatMessages.push(assistantMessage)
        const requestBody = this.buildChatRequest(question)
        const stop = await streamChatWithKnowledgeBase({
          body: requestBody,
          onMessage: payload => {
            if (payload && payload.sessionId && !this.chatForm.sessionId) {
              this.chatForm.sessionId = payload.sessionId
              this.selectedSessionId = payload.sessionId
              this.bindCurrentSessionKnowledgeBases()
            }
            const delta = typeof payload === 'string' ? payload : payload && payload.delta
            if (delta) {
              assistantMessage.content += delta
            }
          },
          onError: () => {
            this.$message.error('流式问答失败')
          },
          onFinish: async () => {
            this.loading.streamChat = false
            this.streamStopper = null
            if (this.chatForm.sessionId) {
              await this.bindCurrentSessionKnowledgeBases()
              await this.loadSessions()
              await this.tryAttachLatestRetrievals(assistantMessage)
            }
          }
        })
        this.streamStopper = stop
        return
      }

      this.loading.chat = true
      try {
        const res = await chatWithKnowledgeBase(this.buildChatRequest(question))
        const data = this.extractResponseData(res) || {}
        if (data.sessionId) {
          this.chatForm.sessionId = data.sessionId
          this.selectedSessionId = data.sessionId
          await this.bindCurrentSessionKnowledgeBases()
        }
        let sources = this.extractAnswerSources(data, { callLogId: data.callLogId || null })
        if ((!sources || !sources.length) && data.callLogId) {
          sources = await this.fetchRetrievalLogs(data.callLogId, false)
        }
        this.chatMessages.push(
          this.createLocalAssistantMessage(data.content || '', {
            id: data.assistantMessageId || null,
            callLogId: data.callLogId || null,
            totalTokens: data.totalTokens || null,
            latencyMs: data.latencyMs || null,
            modelName: data.modelName || '',
            sources
          })
        )
        await this.loadSessions()
      } catch (e) {
        this.$message.error('问答失败')
      } finally {
        this.loading.chat = false
      }
    },
    async tryAttachLatestRetrievals(targetMessage) {
      if (!this.chatForm.sessionId || !targetMessage) return
      try {
        const res = await pageSessionCallLogs(this.chatForm.sessionId, { page: 0, size: 1 })
        const pageData = this.extractPageData(res)
        const latest = Array.isArray(pageData.content) && pageData.content.length ? pageData.content[0] : null
        if (!latest || !latest.id) {
          await this.loadSessionMessages(this.chatForm.sessionId)
          return
        }
        const sources = await this.fetchRetrievalLogs(latest.id, false)
        if (sources.length) {
          targetMessage.callLogId = latest.id
          targetMessage.sources = sources
          targetMessage.sourceOpen = true
        } else {
          await this.loadSessionMessages(this.chatForm.sessionId)
        }
      } catch (e) {
        await this.loadSessionMessages(this.chatForm.sessionId)
      }
    },
    async fetchRetrievalLogs(callLogId, openDrawer = true) {
      if (!callLogId) return []
      if (openDrawer) {
        this.loading.retrievals = true
      }
      try {
        const res = await listCallRetrievals(callLogId)
        const list = this.extractResponseData(res) || []
        const normalized = this.normalizeRetrievalLogs(list, callLogId)
        if (openDrawer) {
          this.retrievalLogs = normalized
        }
        return normalized
      } catch (e) {
        if (openDrawer) {
          this.$message.error('加载检索日志失败')
        }
        return []
      } finally {
        if (openDrawer) {
          this.loading.retrievals = false
        }
      }
    },
    async openRetrievalDrawer(callLogId, title) {
      this.currentRetrievalMeta = title || `CallLog #${callLogId}`
      this.retrievalDrawerVisible = true
      await this.fetchRetrievalLogs(callLogId, true)
    },
    async openRetrievalDrawerByMessage(msg) {
      if (!msg || !msg.callLogId) return
      const title = msg.modelName ? `${msg.modelName} / 检索日志` : '检索日志'
      await this.openRetrievalDrawer(msg.callLogId, title)
    },
    locateSourceDocument(source) {
      if (!source) return
      const matched = this.documents.find(item => {
        if (source.documentId && item.id === source.documentId) return true
        return source.title && (item.title || '') === source.title
      })
      this.activeTab = 'documents'
      if (!matched) {
        this.$message.warning('当前文档列表中未找到对应文档')
        return
      }
      this.$message.success(`已定位到文档：${matched.title}`)
      if (source.chunkId || source.chunkIndex !== null) {
        this.viewChunks(matched)
      }
    },
    async archiveSession(item) {
      if (!item || !item.id) return
      try {
        await archiveAiSession(item.id)
        this.$message.success('会话已归档')
        if (this.selectedSessionId === item.id) {
          this.selectedSessionId = null
          this.chatForm.sessionId = null
          this.chatMessages = []
        }
        await this.loadSessions()
      } catch (e) {
        this.$message.error('归档会话失败')
      }
    },
    async removeSession(item) {
      if (!item || !item.id) return
      try {
        await this.$confirm('确认删除这个会话吗？', '提示', { type: 'warning' })
        await deleteAiSession(item.id)
        this.$message.success('会话已删除')
        if (this.selectedSessionId === item.id) {
          this.selectedSessionId = null
          this.chatForm.sessionId = null
          this.chatMessages = []
        }
        await this.loadSessions()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('删除会话失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.kb-page {
  padding: 16px;
  background: #f6f8fb;
  min-height: calc(100vh - 64px);
}

.kb-sidebar-card,
.kb-main-card {
  border-radius: 16px;
}

.sidebar-header,
.main-toolbar,
.drawer-headline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title,
.main-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.page-subtitle,
.main-subtitle,
.drawer-subtitle,
.text-muted {
  color: #8492a6;
  font-size: 12px;
}

.sidebar-toolbar,
.sidebar-form,
.sidebar-pagination,
.task-alert,
.kb-descriptions,
.kb-tabs,
.dialog-alert {
  margin-top: 14px;
}

.sidebar-toolbar--compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.kb-list {
  margin-top: 12px;
  max-height: calc(100vh - 340px);
  overflow-y: auto;
  padding-right: 4px;
}

.kb-list-item {
  border: 1px solid #e8edf5;
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.kb-list-item:hover,
.kb-list-item.active {
  border-color: #409eff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
}

.kb-list-item__top,
.kb-list-item__meta,
.tab-toolbar,
.tab-toolbar__left,
.tab-toolbar__right,
.chat-actions,
.upload-box,
.upload-box__left,
.upload-box__right,
.source-card__top,
.source-card__meta,
.source-card__actions,
.chunk-card__header,
.task-card__top,
.retrieval-card__top,
.retrieval-card__meta,
.retrieval-card__actions,
.chat-message__footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.kb-list-item__top,
.tab-toolbar,
.chat-actions,
.upload-box,
.source-card__top,
.task-card__top,
.retrieval-card__top,
.main-toolbar {
  justify-content: space-between;
}

.kb-list-item__name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.kb-list-item__desc {
  margin-top: 8px;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.kb-list-item__meta {
  flex-wrap: wrap;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.main-actions,
.chat-message__footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.main-empty {
  margin-top: 48px;
}

.tab-toolbar {
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.hidden-file-input {
  display: none;
}

.upload-box {
  width: 100%;
  align-items: flex-start;
  padding: 12px 14px;
  border: 1px dashed #dcdfe6;
  border-radius: 12px;
  background: #fafcff;
}

.upload-box__right {
  flex: 1;
  align-items: flex-start;
  flex-direction: column;
}

.upload-file-name {
  font-size: 13px;
  color: #303133;
}

.file-error {
  margin-top: 6px;
  color: #f56c6c;
  font-size: 12px;
}

.chat-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  min-height: 640px;
}

.chat-sidebar,
.chat-box {
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
}

.chat-sidebar__toolbar {
  padding: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.session-list {
  padding: 12px;
  max-height: 580px;
  overflow-y: auto;
}

.session-item {
  border: 1px solid #edf1f7;
  border-radius: 12px;
  padding: 10px 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-item:hover,
.session-item.active {
  border-color: #409eff;
  background: #f5f9ff;
}

.session-item__title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.session-item__meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.session-item__actions {
  margin-top: 6px;
}

.chat-box {
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  min-height: 460px;
  max-height: 520px;
  overflow-y: auto;
  padding: 18px;
  background: linear-gradient(180deg, #fafcff 0%, #ffffff 100%);
}

.chat-message {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #edf1f7;
}

.chat-message.user {
  background: #f3f8ff;
  border-color: #dce9ff;
}

.chat-message__role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.chat-message__content {
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
  line-height: 1.75;
}

.chat-message__footer {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.chat-message__sources {
  margin-top: 12px;
  border-top: 1px dashed #e5e9f2;
  padding-top: 10px;
}

.sources-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}

.sources-header .el-icon-arrow-down {
  transition: transform 0.2s ease;
}

.sources-header .is-open {
  transform: rotate(180deg);
}

.sources-list {
  margin-top: 10px;
}

.source-card,
.chunk-card,
.task-card,
.retrieval-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
  margin-bottom: 10px;
}

.source-card__title,
.retrieval-card__title {
  font-weight: 600;
  color: #303133;
}

.source-card__meta,
.retrieval-card__meta {
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}

.source-card__kb {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
}

.source-card__content,
.chunk-card__content,
.retrieval-card__content {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #4c566a;
}

.chat-input {
  border-top: 1px solid #eef2f7;
  padding: 14px;
  background: #fff;
}

.chat-hint {
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.drawer-body {
  padding: 16px;
}

.drawer-headline {
  padding: 0 16px 8px;
}

.task-card__row {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
}

@media (max-width: 1200px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .session-list {
    max-height: 240px;
  }
}

@media (max-width: 768px) {
  .kb-page {
    padding: 10px;
  }

  .main-toolbar,
  .tab-toolbar,
  .sidebar-toolbar--compact,
  .upload-box {
    flex-direction: column;
    align-items: stretch;
  }

  .main-actions,
  .tab-toolbar__left {
    width: 100%;
  }
}
</style>
