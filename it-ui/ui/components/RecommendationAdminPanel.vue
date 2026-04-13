<template>
  <div class="recommendation-admin-page">
    <div class="page-header">
      <div>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
      <div class="header-actions">
        <el-button icon="el-icon-refresh" :loading="loading" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <el-alert
      :title="activeModelText"
      type="success"
      :closable="false"
      show-icon
      class="active-model-alert"
    />

    <el-row :gutter="16" class="summary-grid">
      <el-col v-for="card in summaryCards" :key="card.key" :span="8">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-grid">
      <el-col :span="9">
        <el-card shadow="never" class="strategy-card">
          <template slot="header">
            <div class="card-header">
              <span>推荐策略概览</span>
              <el-tag size="mini" type="info">{{ modeLabel }}</el-tag>
            </div>
          </template>

          <div class="strategy-block">
            <h3>{{ strategyTitle }}</h3>
            <p>{{ strategyDescription }}</p>
            <div class="strategy-tags">
              <el-tag v-for="item in strategyTags" :key="item" size="mini" effect="dark">
                {{ item }}
              </el-tag>
            </div>
          </div>

          <el-divider />

          <div class="strategy-block">
            <h3>管理建议</h3>
            <ul class="strategy-list">
              <li v-for="item in strategySuggestions" :key="item">{{ item }}</li>
            </ul>
          </div>
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card shadow="never" class="data-card">
          <template slot="header">
            <div class="card-header">
              <span>模型配置</span>
              <el-input
                v-model.trim="modelKeyword"
                clearable
                size="small"
                placeholder="搜索模型名称 / Provider"
                prefix-icon="el-icon-search"
                class="header-search"
              />
            </div>
          </template>

          <el-table
            v-loading="loading"
            :data="filteredModels"
            border
            stripe
            class="admin-table"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="modelName" label="模型名称" min-width="150" />
            <el-table-column prop="providerCode" label="Provider" width="120" />
            <el-table-column prop="modelType" label="类型" width="100" />
            <el-table-column prop="deploymentMode" label="部署方式" width="110" />
            <el-table-column label="启用" width="90">
              <template slot-scope="{ row }">
                <el-tag :type="row.isEnabled ? 'success' : 'info'" size="mini">
                  {{ row.isEnabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前默认" width="100">
              <template slot-scope="{ row }">
                <el-tag v-if="row.isActive" type="success" size="mini">默认</el-tag>
                <span v-else class="muted-text">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" size="mini" :loading="row._testing" @click="handleTestModel(row)">测试</el-button>
                <el-button type="text" size="mini" :disabled="row.isActive" @click="handleActivateModel(row)">设为默认</el-button>
                <el-button v-if="row.isEnabled" type="text" size="mini" class="danger-text" @click="handleToggleModel(row, false)">停用</el-button>
                <el-button v-else type="text" size="mini" @click="handleToggleModel(row, true)">启用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="data-card template-card">
          <template slot="header">
            <div class="card-header">
              <span>提示词模板</span>
              <el-input
                v-model.trim="templateKeyword"
                clearable
                size="small"
                placeholder="搜索模板名称 / 场景码"
                prefix-icon="el-icon-search"
                class="header-search"
              />
            </div>
          </template>

          <el-table
            v-loading="loading"
            :data="filteredTemplates"
            border
            stripe
            class="admin-table"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="templateName" label="模板名称" min-width="160" />
            <el-table-column prop="sceneCode" label="场景码" min-width="150" />
            <el-table-column prop="templateType" label="类型" width="120" />
            <el-table-column label="状态" width="110">
              <template slot-scope="{ row }">
                <el-tag :type="row.isEnabled ? 'success' : 'info'" size="mini">
                  {{ row.isEnabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" size="mini" class="primary-text" @click="handlePublishTemplate(row)">发布</el-button>
                <el-button v-if="row.isEnabled" type="text" size="mini" class="danger-text" @click="handleToggleTemplate(row, false)">停用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  activateAiModel,
  enableAiModel,
  disableAiModel,
  disablePromptTemplate,
  extractApiData,
  extractPageContent,
  getActiveAiModel,
  pageAiModels,
  pagePromptTemplates,
  publishPromptTemplate,
  testAiModelConnection
} from '~/api/aiAdmin'

const MODE_META = {
  blog: {
    title: '博客算法推荐管理',
    subtitle: '围绕内容热度、作者权重、标签匹配和发布时间维护推荐配置。',
    modeLabel: '博客推荐',
    strategyTitle: '博客推荐策略',
    strategyDescription: '关注内容质量、标签一致性、作者活跃度和时效热度，优先保证高相关内容进入推荐链路。',
    strategyTags: ['内容热度', '标签命中', '作者活跃', '发布时间'],
    strategySuggestions: [
      '优先使用支持流式输出的模型，便于后续生成摘要和推荐理由。',
      '博客模板建议围绕标题、正文摘要、标签和作者画像配置变量。',
      '热门博客的推荐权重应当保留时间衰减，避免旧内容长期霸榜。'
    ]
  },
  project: {
    title: '项目算法推荐管理',
    subtitle: '围绕需求匹配、预算区间、协作能力和交付效率维护推荐配置。',
    modeLabel: '项目推荐',
    strategyTitle: '项目推荐策略',
    strategyDescription: '重点关注技术栈契合、预算范围、团队协作和项目阶段，让推荐结果更贴近实际协作场景。',
    strategyTags: ['需求匹配', '预算约束', '协作历史', '项目阶段'],
    strategySuggestions: [
      '项目推荐模板应保留需求描述、预算区间、技术栈和交付周期字段。',
      '默认模型建议选择支持结构化输出和工具调用的模型。',
      '对已启用模板定期做连通性测试，避免推荐链路依赖失效。'
    ]
  }
}

function normalizeListPayload(response) {
  return extractPageContent(response).content || []
}

function normalizeItem(item = {}) {
  return {
    ...item,
    isEnabled: item.isEnabled !== undefined ? item.isEnabled : item.enabled,
    isActive: !!item.isActive
  }
}

export default {
  name: 'RecommendationAdminPanel',
  props: {
    mode: {
      type: String,
      default: 'blog',
      validator: value => ['blog', 'project'].includes(value)
    }
  },
  data() {
    return {
      loading: false,
      modelKeyword: '',
      templateKeyword: '',
      activeModel: null,
      models: [],
      templates: []
    }
  },
  computed: {
    meta() {
      return MODE_META[this.mode] || MODE_META.blog
    },
    pageTitle() {
      return this.meta.title
    },
    pageSubtitle() {
      return this.meta.subtitle
    },
    modeLabel() {
      return this.meta.modeLabel
    },
    strategyTitle() {
      return this.meta.strategyTitle
    },
    strategyDescription() {
      return this.meta.strategyDescription
    },
    strategyTags() {
      return this.meta.strategyTags
    },
    strategySuggestions() {
      return this.meta.strategySuggestions
    },
    activeModelText() {
      if (!this.activeModel || !this.activeModel.id) {
        return '当前还没有设置默认模型，请从模型列表中指定一个默认推荐模型。'
      }
      return `当前默认模型：${this.activeModel.modelName || '-'} · ${this.activeModel.providerCode || '-'}`
    },
    summaryCards() {
      const enabledModels = this.models.filter(item => item.isEnabled).length
      const activeModel = this.models.find(item => item.isActive) || this.activeModel || {}
      const enabledTemplates = this.templates.filter(item => item.isEnabled).length
      const publishedTemplates = this.templates.filter(item => item.publishStatus === 'PUBLISHED' || item.isPublished).length
      return [
        {
          key: 'models',
          label: '模型总数',
          value: this.models.length,
          desc: `${enabledModels} 个可用`
        },
        {
          key: 'active',
          label: '当前默认',
          value: activeModel.modelName || '未设置',
          desc: activeModel.providerCode || '请先指定模型'
        },
        {
          key: 'templates',
          label: '模板总数',
          value: this.templates.length,
          desc: `${enabledTemplates} 个启用，${publishedTemplates} 个已发布`
        }
      ]
    },
    filteredModels() {
      const keyword = this.modelKeyword.toLowerCase()
      if (!keyword) return this.models
      return this.models.filter(item => {
        const source = `${item.modelName || ''} ${item.providerCode || ''} ${item.modelType || ''} ${item.deploymentMode || ''}`.toLowerCase()
        return source.includes(keyword)
      })
    },
    filteredTemplates() {
      const keyword = this.templateKeyword.toLowerCase()
      if (!keyword) return this.templates
      return this.templates.filter(item => {
        const source = `${item.templateName || ''} ${item.sceneCode || ''} ${item.templateType || ''}`.toLowerCase()
        return source.includes(keyword)
      })
    }
  },
  mounted() {
    this.refreshAll()
  },
  methods: {
    async refreshAll() {
      this.loading = true
      try {
        const [modelRes, templateRes, activeRes] = await Promise.all([
          pageAiModels({ page: 0, size: 20 }),
          pagePromptTemplates({ page: 0, size: 20 }),
          getActiveAiModel().catch(() => null)
        ])

        this.models = normalizeListPayload(modelRes).map(normalizeItem)
        this.templates = normalizeListPayload(templateRes).map(normalizeItem)
        const activePayload = extractApiData(activeRes)
        this.activeModel = activePayload ? normalizeItem(activePayload) : this.models.find(item => item.isActive) || null
      } catch (error) {
        this.$message.error('加载算法推荐管理页失败')
        console.error('加载算法推荐管理页失败:', error)
        this.models = []
        this.templates = []
        this.activeModel = null
      } finally {
        this.loading = false
      }
    },
    async handleTestModel(row) {
      row._testing = true
      try {
        await testAiModelConnection(row.id)
        this.$message.success('连通性测试已提交')
      } catch (error) {
        this.$message.error(error?.response?.data?.message || '连通性测试失败')
      } finally {
        row._testing = false
      }
    },
    async handleActivateModel(row) {
      try {
        await activateAiModel(row.id)
        this.$message.success('已设为默认模型')
        await this.refreshAll()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || '设置默认模型失败')
      }
    },
    async handleToggleModel(row, enable) {
      try {
        if (enable) {
          await enableAiModel(row.id)
        } else {
          await disableAiModel(row.id)
        }
        this.$message.success(enable ? '模型已启用' : '模型已停用')
        await this.refreshAll()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || '模型状态更新失败')
      }
    },
    async handlePublishTemplate(row) {
      try {
        await publishPromptTemplate(row.id)
        this.$message.success('模板已发布')
        await this.refreshAll()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || '发布模板失败')
      }
    },
    async handleToggleTemplate(row, enable) {
      try {
        if (!enable) {
          await disablePromptTemplate(row.id)
        }
        this.$message.success('模板已停用')
        await this.refreshAll()
      } catch (error) {
        this.$message.error(error?.response?.data?.message || '模板状态更新失败')
      }
    }
  }
}
</script>

<style scoped>
.recommendation-admin-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #6b7280;
}

.active-model-alert {
  margin-bottom: 16px;
}

.summary-grid {
  margin-bottom: 16px;
}

.summary-card {
  min-height: 110px;
  border-radius: 16px;
  border: 1px solid #e8eef7;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.summary-label {
  color: #6b7280;
  font-size: 13px;
}

.summary-value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  word-break: break-word;
}

.summary-desc {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.content-grid {
  align-items: stretch;
}

.strategy-card,
.data-card {
  border-radius: 18px;
  border: 1px solid #e8eef7;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.data-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-search {
  width: 240px;
}

.strategy-block h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #111827;
}

.strategy-block p {
  margin: 0;
  color: #4b5563;
  line-height: 1.8;
}

.strategy-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.strategy-list {
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.9;
}

.admin-table /deep/ .el-table__header-wrapper th {
  background: #f8fbff;
}

.muted-text {
  color: #94a3b8;
}

.primary-text {
  color: #409eff;
}

.danger-text {
  color: #f56c6c;
}

@media (max-width: 1200px) {
  .page-header {
    flex-direction: column;
  }

  .header-search {
    width: 180px;
  }
}

@media (max-width: 900px) {
  .recommendation-admin-page {
    padding: 14px;
  }

  .summary-grid .el-col,
  .content-grid .el-col {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
    margin-bottom: 12px;
  }
}
</style>
