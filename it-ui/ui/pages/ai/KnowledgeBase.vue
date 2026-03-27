<template>
  <div class="kb-page">
    <el-card class="kb-header" shadow="never">
      <div class="kb-header__top">
        <div>
          <div class="kb-title">知识库管理</div>
          <div class="kb-subtitle">创建知识库、录入文档、管理成员，并直接发起知识库问答</div>
        </div>
        <div class="kb-header__actions">
          <el-button type="primary" icon="el-icon-plus" @click="openCreateKbDialog">
            新建知识库
          </el-button>
          <el-button icon="el-icon-refresh" @click="loadKnowledgeBases">
            刷新
          </el-button>
        </div>
      </div>

      <div class="kb-filters">
        <el-radio-group v-model="listMode" size="small" @change="handleListModeChange">
          <el-radio-button label="owner">我的知识库</el-radio-button>
          <el-radio-button label="project">项目知识库</el-radio-button>
        </el-radio-group>

        <el-input
          v-model.trim="keyword"
          clearable
          size="small"
          placeholder="按名称搜索"
          class="kb-search"
        >
          <i slot="prefix" class="el-input__icon el-icon-search"></i>
        </el-input>

        <el-input-number
          v-if="listMode === 'project'"
          v-model="projectId"
          :min="1"
          size="small"
          controls-position="right"
          placeholder="项目ID"
          @change="loadKnowledgeBases"
        />

        <el-button size="small" @click="loadKnowledgeBases">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="kb-body">
      <el-col :span="9">
        <el-card shadow="never" class="kb-panel">
          <div slot="header" class="panel-header">
            <span>知识库列表</span>
            <span class="panel-header__meta">共 {{ pagination.total }} 条</span>
          </div>

          <div v-loading="loading.kbList">
            <div
              v-for="item in filteredKnowledgeBases"
              :key="item.id"
              class="kb-item"
              :class="{ active: currentKnowledgeBase && currentKnowledgeBase.id === item.id }"
              @click="selectKnowledgeBase(item)"
            >
              <div class="kb-item__top">
                <div class="kb-item__name">{{ item.name || '未命名知识库' }}</div>
                <el-tag size="mini" :type="statusTagType(item.status)">
                  {{ item.status || 'DRAFT' }}
                </el-tag>
              </div>
              <div class="kb-item__desc">{{ item.description || '暂无描述' }}</div>
              <div class="kb-item__meta">
                <span>文档 {{ item.docCount || 0 }}</span>
                <span>Chunk {{ item.chunkCount || 0 }}</span>
                <span>TopK {{ item.defaultTopK || '-' }}</span>
              </div>
              <div class="kb-item__bottom">
                <el-tag size="mini" effect="plain">{{ item.scopeType || '-' }}</el-tag>
                <el-tag size="mini" effect="plain">{{ item.visibility || '-' }}</el-tag>
              </div>
            </div>

            <el-empty
              v-if="!filteredKnowledgeBases.length"
              :image-size="80"
              description="暂无知识库"
            />
          </div>

          <div class="kb-pagination">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="pagination.page + 1"
              :page-size="pagination.size"
              :total="pagination.total"
              @current-change="handleKbPageChange"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card shadow="never" class="kb-panel">
          <div slot="header" class="panel-header">
            <span>{{ currentKnowledgeBase ? currentKnowledgeBase.name : '知识库详情' }}</span>
            <div class="panel-header__actions" v-if="currentKnowledgeBase">
              <el-button size="mini" @click="openEditKbDialog">编辑</el-button>
              <el-button size="mini" type="primary" @click="openDocumentDialog">新增文档</el-button>
              <el-button size="mini" @click="openMemberDialog">添加成员</el-button>
              <el-button size="mini" @click="handleReindex">重建索引</el-button>
            </div>
          </div>

          <el-empty
            v-if="!currentKnowledgeBase"
            :image-size="100"
            description="请先从左侧选择一个知识库"
          />

          <div v-else class="kb-detail">
            <el-descriptions :column="2" border size="small" class="kb-descriptions">
              <el-descriptions-item label="知识库ID">{{ currentKnowledgeBase.id }}</el-descriptions-item>
              <el-descriptions-item label="拥有者">{{ currentKnowledgeBase.ownerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="作用域">{{ currentKnowledgeBase.scopeType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="可见性">{{ currentKnowledgeBase.visibility || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分块策略">{{ currentKnowledgeBase.chunkStrategy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="默认TopK">{{ currentKnowledgeBase.defaultTopK || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Provider">{{ currentKnowledgeBase.embeddingProvider || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Embedding Model">{{ currentKnowledgeBase.embeddingModel || '-' }}</el-descriptions-item>
              <el-descriptions-item label="文档数">{{ currentKnowledgeBase.docCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Chunk数">{{ currentKnowledgeBase.chunkCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ currentKnowledgeBase.status || '-' }}</el-descriptions-item>
              <el-descriptions-item label="最后索引时间">{{ formatTime(currentKnowledgeBase.lastIndexedAt) }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">
                {{ currentKnowledgeBase.description || '暂无描述' }}
              </el-descriptions-item>
            </el-descriptions>

            <el-tabs v-model="activeTab" class="kb-tabs">
              <el-tab-pane label="文档管理" name="documents">
                <div class="tab-toolbar">
                  <el-button type="primary" size="small" @click="openDocumentDialog">新增文档</el-button>
                  <el-button size="small" @click="loadDocuments">刷新文档</el-button>
                </div>

                <el-table v-loading="loading.documents" :data="documents" border stripe size="small">
                  <el-table-column prop="id" label="ID" width="80" />
                  <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="sourceType" label="来源" width="120" />
                  <el-table-column prop="status" label="状态" width="120">
                    <template slot-scope="{ row }">
                      <el-tag size="mini" :type="docStatusTagType(row.status)">{{ row.status }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="indexedAt" label="索引时间" min-width="170">
                    <template slot-scope="{ row }">
                      {{ formatTime(row.indexedAt) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="updatedAt" label="更新时间" min-width="170">
                    <template slot-scope="{ row }">
                      {{ formatTime(row.updatedAt) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="210" fixed="right">
                    <template slot-scope="{ row }">
                      <el-button type="text" size="small" @click="viewChunks(row)">查看切片</el-button>
                      <el-button type="text" size="small" @click="reindexDocument(row)">重建索引</el-button>
                      <el-button type="text" size="small" @click="viewIndexTasks(row)">索引记录</el-button>
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
                  <el-button type="primary" size="small" @click="openMemberDialog">添加成员</el-button>
                  <el-button size="small" @click="loadMembers">刷新成员</el-button>
                </div>

                <el-table v-loading="loading.members" :data="members" border stripe size="small">
                  <el-table-column prop="id" label="ID" width="80" />
                  <el-table-column prop="userId" label="用户ID" width="120" />
                  <el-table-column prop="roleCode" label="角色" width="120" />
                  <el-table-column prop="grantedBy" label="授权人" width="120" />
                  <el-table-column prop="createdAt" label="创建时间" min-width="180">
                    <template slot-scope="{ row }">
                      {{ formatTime(row.createdAt) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="120" fixed="right">
                    <template slot-scope="{ row }">
                      <el-button
                        type="text"
                        size="small"
                        :disabled="row.roleCode === 'OWNER'"
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
                    <el-form-item label="用户ID">
                      <el-input-number v-model="chatForm.userId" :min="1" controls-position="right" />
                    </el-form-item>
                    <el-form-item label="模型ID">
                      <el-input-number v-model="chatForm.modelId" :min="1" controls-position="right" />
                    </el-form-item>
                    <el-form-item label="请求类型">
                      <el-select v-model="chatForm.requestType" style="width: 160px">
                        <el-option label="KNOWLEDGE_QA" value="KNOWLEDGE_QA" />
                        <el-option label="CHAT" value="CHAT" />
                      </el-select>
                    </el-form-item>
                  </el-form>
                </div>

                <div class="chat-box">
                  <div class="chat-messages">
                    <div
                      v-for="(msg, index) in chatMessages"
                      :key="index"
                      class="chat-message"
                      :class="msg.role"
                    >
                      <div class="chat-message__role">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
                      <div class="chat-message__content">{{ msg.content }}</div>
                    </div>
                  </div>

                  <div class="chat-input">
                    <el-input
                      v-model.trim="chatForm.content"
                      type="textarea"
                      :rows="4"
                      placeholder="请输入你的问题，比如：总结这个知识库里关于Spring事务传播行为的要点"
                    />
                    <div class="chat-actions">
                      <el-button @click="clearChat">清空</el-button>
                      <el-button :loading="loading.chat" @click="sendChat(false)">普通发送</el-button>
                      <el-button type="primary" :loading="loading.streamChat" @click="sendChat(true)">
                        流式发送
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
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
            <el-form-item label="拥有者ID" prop="ownerId">
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
                <el-option label="PLATFORM" value="PLATFORM" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目ID">
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
            <el-form-item label="默认TopK">
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

    <el-dialog
      title="新增文档"
      :visible.sync="documentDialogVisible"
      width="760px"
      @closed="resetDocumentForm"
    >
      <el-form ref="documentFormRef" :model="documentForm" :rules="documentRules" label-width="100px">
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

        <el-form-item label="来源引用ID">
          <el-input-number v-model="documentForm.sourceRefId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>

        <el-form-item label="正文" prop="contentText">
          <el-input
            v-model="documentForm.contentText"
            type="textarea"
            :rows="14"
            placeholder="当前后端是文本入库，这里直接贴正文内容"
          />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="documentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.saveDocument" @click="submitDocumentForm">保存并入库</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="添加成员"
      :visible.sync="memberDialogVisible"
      width="480px"
      @closed="resetMemberForm"
    >
      <el-form ref="memberFormRef" :model="memberForm" :rules="memberRules" label-width="100px">
        <el-form-item label="用户ID" prop="userId">
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

    <el-drawer
      title="文档切片"
      :visible.sync="chunkDrawerVisible"
      size="40%"
      direction="rtl"
    >
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

    <el-drawer
      title="索引任务记录"
      :visible.sync="taskDrawerVisible"
      size="35%"
      direction="rtl"
    >
      <div class="drawer-body" v-loading="loading.tasks">
        <el-empty v-if="!indexTasks.length" description="暂无任务记录" :image-size="80" />
        <div v-for="item in indexTasks" :key="item.id" class="task-card">
          <div class="task-card__top">
            <span>{{ item.taskType }}</span>
            <el-tag size="mini" :type="taskStatusTagType(item.status)">
              {{ item.status }}
            </el-tag>
          </div>
          <div class="task-card__row">创建时间：{{ formatTime(item.createdAt) }}</div>
          <div class="task-card__row">开始时间：{{ formatTime(item.startedAt) }}</div>
          <div class="task-card__row">结束时间：{{ formatTime(item.finishedAt) }}</div>
          <div class="task-card__row">重试次数：{{ item.retryCount || 0 }}</div>
          <div class="task-card__row">错误信息：{{ item.errorMessage || '无' }}</div>
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
  listDocumentIndexTasks,
  chatWithKnowledgeBase,
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
        streamChat: false
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

      knowledgeBases: [],
      currentKnowledgeBase: null,

      activeTab: 'documents',

      documents: [],
      members: [],
      chunks: [],
      indexTasks: [],

      kbDialogVisible: false,
      kbDialogMode: 'create',
      kbForm: this.getDefaultKbForm(),

      documentDialogVisible: false,
      documentForm: this.getDefaultDocumentForm(),

      memberDialogVisible: false,
      memberForm: this.getDefaultMemberForm(),

      chunkDrawerVisible: false,
      taskDrawerVisible: false,

      chatForm: {
        sessionId: null,
        userId: 1,
        content: '',
        modelId: 1,
        requestType: 'KNOWLEDGE_QA'
      },
      chatMessages: [],
      streamStopper: null,

      kbRules: {
        name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
        ownerId: [{ required: true, message: '请输入拥有者ID', trigger: 'change' }],
        scopeType: [{ required: true, message: '请选择作用域', trigger: 'change' }]
      },
      documentRules: {
        title: [{ required: true, message: '请输入文档标题', trigger: 'blur' }],
        contentText: [{ required: true, message: '请输入文档正文', trigger: 'blur' }]
      },
      memberRules: {
        userId: [{ required: true, message: '请输入用户ID', trigger: 'change' }],
        roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },
  computed: {
    filteredKnowledgeBases() {
      if (!this.keyword) return this.knowledgeBases
      const key = this.keyword.toLowerCase()
      return this.knowledgeBases.filter(item => {
        return (
          String(item.name || '').toLowerCase().includes(key) ||
          String(item.description || '').toLowerCase().includes(key)
        )
      })
    }
  },
  mounted() {
    this.initUserId()
    this.loadKnowledgeBases()
  },
  beforeDestroy() {
    if (this.streamStopper) {
      this.streamStopper()
      this.streamStopper = null
    }
  },
  methods: {
    initUserId() {
      try {
        const raw = localStorage.getItem('userInfo')
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

    getDefaultKbForm() {
      return {
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
      }
    },

    getDefaultDocumentForm() {
      return {
        sourceType: 'MANUAL',
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

        const pageData = (res && res.data) || {}
        this.knowledgeBases = pageData.content || []
        this.pagination.total = pageData.totalElements || 0

        if (this.knowledgeBases.length) {
          const currentId = this.currentKnowledgeBase && this.currentKnowledgeBase.id
          const matched = this.knowledgeBases.find(item => item.id === currentId)
          this.selectKnowledgeBase(matched || this.knowledgeBases[0], false)
        } else {
          this.currentKnowledgeBase = null
          this.documents = []
          this.members = []
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
      await Promise.all([this.loadDocuments(), this.loadMembers()])
    },

    async loadKnowledgeBaseDetail(id) {
      try {
        const res = await getKnowledgeBase(id)
        this.currentKnowledgeBase = (res && res.data) || this.currentKnowledgeBase
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
        const pageData = (res && res.data) || {}
        this.documents = pageData.content || []
        this.documentPagination.total = pageData.totalElements || 0
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
        this.members = (res && res.data) || []
      } catch (e) {
        this.$message.error('加载成员失败')
      } finally {
        this.loading.members = false
      }
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

    openCreateKbDialog() {
      this.kbDialogMode = 'create'
      this.kbDialogVisible = true
      this.kbForm = this.getDefaultKbForm()
      this.kbForm.ownerId = this.ownerId
    },

    openEditKbDialog() {
      if (!this.currentKnowledgeBase) return
      this.kbDialogMode = 'edit'
      this.kbDialogVisible = true
      this.kbForm = {
        scopeType: this.currentKnowledgeBase.scopeType || 'PERSONAL',
        projectId: this.currentKnowledgeBase.projectId || null,
        ownerId: this.currentKnowledgeBase.ownerId || this.ownerId,
        name: this.currentKnowledgeBase.name || '',
        description: this.currentKnowledgeBase.description || '',
        sourceType: this.currentKnowledgeBase.sourceType || 'MANUAL',
        embeddingProvider: this.currentKnowledgeBase.embeddingProvider || '',
        embeddingModel: this.currentKnowledgeBase.embeddingModel || '',
        chunkStrategy: this.currentKnowledgeBase.chunkStrategy || 'PARAGRAPH',
        defaultTopK: this.currentKnowledgeBase.defaultTopK || 5,
        visibility: this.currentKnowledgeBase.visibility || 'PRIVATE'
      }
    },

    submitKbForm() {
      this.$refs.kbFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveKb = true
        try {
          if (this.kbDialogMode === 'create') {
            const res = await createKnowledgeBase(this.kbForm)
            this.$message.success((res && res.message) || '创建成功')
          } else {
            const res = await updateKnowledgeBase(this.currentKnowledgeBase.id, this.kbForm)
            this.$message.success((res && res.message) || '更新成功')
          }
          this.kbDialogVisible = false
          await this.loadKnowledgeBases()
        } catch (e) {
          this.$message.error('保存知识库失败')
        } finally {
          this.loading.saveKb = false
        }
      })
    },

    resetKbForm() {
      this.kbForm = this.getDefaultKbForm()
      this.kbForm.ownerId = this.ownerId
      this.$refs.kbFormRef && this.$refs.kbFormRef.clearValidate()
    },

    openDocumentDialog() {
      if (!this.currentKnowledgeBase) {
        this.$message.warning('请先选择知识库')
        return
      }
      this.documentDialogVisible = true
    },

    submitDocumentForm() {
      this.$refs.documentFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveDocument = true
        try {
          const res = await addKnowledgeDocument(this.currentKnowledgeBase.id, this.documentForm)
          this.$message.success((res && res.message) || '文档已入库')
          this.documentDialogVisible = false
          await Promise.all([
            this.loadDocuments(),
            this.loadKnowledgeBaseDetail(this.currentKnowledgeBase.id)
          ])
        } catch (e) {
          this.$message.error('保存文档失败')
        } finally {
          this.loading.saveDocument = false
        }
      })
    },

    resetDocumentForm() {
      this.documentForm = this.getDefaultDocumentForm()
      this.$refs.documentFormRef && this.$refs.documentFormRef.clearValidate()
    },

    async viewChunks(row) {
      this.chunkDrawerVisible = true
      this.loading.chunks = true
      try {
        const res = await listDocumentChunks(row.id)
        this.chunks = (res && res.data) || []
      } catch (e) {
        this.chunks = []
        this.$message.error('加载切片失败')
      } finally {
        this.loading.chunks = false
      }
    },

    async viewIndexTasks(row) {
      this.taskDrawerVisible = true
      this.loading.tasks = true
      try {
        const res = await listDocumentIndexTasks(row.id)
        this.indexTasks = (res && res.data) || []
      } catch (e) {
        this.indexTasks = []
        this.$message.error('加载索引任务失败')
      } finally {
        this.loading.tasks = false
      }
    },

    async handleReindex() {
      if (!this.currentKnowledgeBase) return
      try {
        await this.$confirm('确认重建当前知识库索引吗？', '提示', { type: 'warning' })
        const res = await createKnowledgeIndexTask(this.currentKnowledgeBase.id, {
          documentId: null,
          taskType: 'REINDEX'
        })
        this.$message.success((res && res.message) || '索引任务已创建')
        await Promise.all([
          this.loadDocuments(),
          this.loadKnowledgeBaseDetail(this.currentKnowledgeBase.id)
        ])
      } catch (e) {
        if (e !== 'cancel') this.$message.error('创建索引任务失败')
      }
    },

    async reindexDocument(row) {
      try {
        await this.$confirm(`确认重建文档「${row.title}」的索引吗？`, '提示', { type: 'warning' })
        const res = await createKnowledgeIndexTask(this.currentKnowledgeBase.id, {
          documentId: row.id,
          taskType: 'REINDEX'
        })
        this.$message.success((res && res.message) || '文档索引任务已创建')
        await this.viewIndexTasks(row)
        await this.loadDocuments()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('创建文档索引任务失败')
      }
    },

    openMemberDialog() {
      if (!this.currentKnowledgeBase) {
        this.$message.warning('请先选择知识库')
        return
      }
      this.memberDialogVisible = true
    },

    submitMemberForm() {
      this.$refs.memberFormRef.validate(async valid => {
        if (!valid) return
        this.loading.saveMember = true
        try {
          const res = await addKnowledgeBaseMember(this.currentKnowledgeBase.id, this.memberForm)
          this.$message.success((res && res.message) || '成员添加成功')
          this.memberDialogVisible = false
          await this.loadMembers()
        } catch (e) {
          this.$message.error('添加成员失败')
        } finally {
          this.loading.saveMember = false
        }
      })
    },

    resetMemberForm() {
      this.memberForm = this.getDefaultMemberForm()
      this.$refs.memberFormRef && this.$refs.memberFormRef.clearValidate()
    },

    async removeMember(row) {
      try {
        await this.$confirm('确认移除该成员吗？', '提示', { type: 'warning' })
        const res = await removeKnowledgeBaseMember(this.currentKnowledgeBase.id, row.id)
        this.$message.success((res && res.message) || '成员已移除')
        await this.loadMembers()
      } catch (e) {
        if (e !== 'cancel') this.$message.error('移除成员失败')
      }
    },

    async sendChat(isStream) {
      if (!this.currentKnowledgeBase || !this.currentKnowledgeBase.id) {
        this.$message.warning('请先选择知识库')
        return
      }
      if (!this.chatForm.content) {
        this.$message.warning('请输入问题')
        return
      }

      const payload = {
        sessionId: this.chatForm.sessionId,
        userId: this.chatForm.userId,
        content: this.chatForm.content,
        modelId: this.chatForm.modelId,
        requestType: this.chatForm.requestType,
        knowledgeBaseIds: [this.currentKnowledgeBase.id],
        defaultKnowledgeBaseId: this.currentKnowledgeBase.id,
        bizType: 'GENERAL',
        sceneCode: 'general.chat',
        sessionTitle: this.currentKnowledgeBase.name + '问答',
        memoryMode: 'SHORT'
      }

      const question = this.chatForm.content
      this.chatMessages.push({ role: 'user', content: question })
      this.chatForm.content = ''

      if (!isStream) {
        this.loading.chat = true
        try {
          const res = await chatWithKnowledgeBase(payload)
          const data = (res && res.data) || {}
          const answer =
            data.answer ||
            data.content ||
            data.message ||
            '未返回回答内容'
          this.chatMessages.push({ role: 'assistant', content: answer })
          if (data.sessionId) {
            this.chatForm.sessionId = data.sessionId
          }
        } catch (e) {
          this.chatMessages.push({ role: 'assistant', content: '请求失败，请稍后重试' })
        } finally {
          this.loading.chat = false
        }
        return
      }

      this.loading.streamChat = true
      const aiMsg = { role: 'assistant', content: '' }
      this.chatMessages.push(aiMsg)

      try {
        if (this.streamStopper) {
          this.streamStopper()
          this.streamStopper = null
        }

        this.streamStopper = await streamChatWithKnowledgeBase({
          body: payload,
          onMessage: chunk => {
            if (chunk && chunk.sessionId && !this.chatForm.sessionId) {
              this.chatForm.sessionId = chunk.sessionId
            }
            if (chunk && chunk.delta) {
              aiMsg.content += chunk.delta
            }
            if (chunk && chunk.content && !chunk.delta) {
              aiMsg.content += chunk.content
            }
          },
          onError: () => {
            if (!aiMsg.content) {
              aiMsg.content = '流式请求失败，请稍后重试'
            }
            this.loading.streamChat = false
          },
          onFinish: () => {
            if (!aiMsg.content) {
              aiMsg.content = '已完成，但没有返回内容'
            }
            this.loading.streamChat = false
          }
        })
      } catch (e) {
        if (!aiMsg.content) {
          aiMsg.content = '流式请求失败，请稍后重试'
        }
        this.loading.streamChat = false
      }
    },

    clearChat() {
      if (this.streamStopper) {
        this.streamStopper()
        this.streamStopper = null
      }
      this.chatMessages = []
      this.chatForm.content = ''
      this.loading.streamChat = false
    },

    statusTagType(status) {
      const map = {
        ACTIVE: 'success',
        INDEXING: 'warning',
        FAILED: 'danger',
        DISABLED: 'info',
        DRAFT: ''
      }
      return map[status] || ''
    },

    docStatusTagType(status) {
      const map = {
        INDEXED: 'success',
        PARSING: 'warning',
        FAILED: 'danger',
        DISABLED: 'info',
        UPLOADED: ''
      }
      return map[status] || ''
    },

    taskStatusTagType(status) {
      const map = {
        SUCCESS: 'success',
        RUNNING: 'warning',
        FAILED: 'danger',
        PENDING: 'info'
      }
      return map[status] || ''
    },

    formatTime(val) {
      if (!val) return '-'
      const d = new Date(val)
      if (Number.isNaN(d.getTime())) return val
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const h = String(d.getHours()).padStart(2, '0')
      const min = String(d.getMinutes()).padStart(2, '0')
      const s = String(d.getSeconds()).padStart(2, '0')
      return `${y}-${m}-${day} ${h}:${min}:${s}`
    }
  }
}
</script>

<style scoped>
.kb-page {
  padding: 16px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.kb-header {
  margin-bottom: 16px;
  border-radius: 12px;
}

.kb-header__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.kb-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.kb-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.kb-header__actions {
  display: flex;
  gap: 8px;
}

.kb-filters {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.kb-search {
  width: 240px;
}

.kb-body {
  margin-top: 0;
}

.kb-panel {
  min-height: 720px;
  border-radius: 12px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header__meta {
  color: #909399;
  font-size: 12px;
}

.panel-header__actions {
  display: flex;
  gap: 8px;
}

.kb-item {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.kb-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.08);
}

.kb-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.kb-item__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.kb-item__name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.kb-item__desc {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  min-height: 40px;
}

.kb-item__meta,
.kb-item__bottom {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}

.kb-pagination,
.table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.kb-descriptions {
  margin-bottom: 16px;
}

.kb-tabs {
  margin-top: 8px;
}

.tab-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.drawer-body {
  padding: 0 12px 12px;
}

.chunk-card,
.task-card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fff;
}

.chunk-card__header,
.task-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.chunk-card__content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #606266;
  font-size: 13px;
}

.task-card__row {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  word-break: break-all;
}

.chat-config {
  margin-bottom: 12px;
}

.chat-box {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.chat-messages {
  min-height: 360px;
  max-height: 420px;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}

.chat-message {
  margin-bottom: 14px;
}

.chat-message__role {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.chat-message__content {
  display: inline-block;
  max-width: 90%;
  padding: 10px 12px;
  border-radius: 10px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
}

.chat-message.user .chat-message__content {
  background: #409eff;
  color: #fff;
}

.chat-message.assistant .chat-message__content {
  background: #fff;
  color: #303133;
  border: 1px solid #ebeef5;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.chat-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>