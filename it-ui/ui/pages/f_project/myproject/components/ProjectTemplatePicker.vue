<template>
  <div class="template-picker">
    <div class="picker-toolbar">
      <el-input
        v-model="keyword"
        size="small"
        clearable
        placeholder="搜索模板名称/描述"
        class="picker-keyword"
        @keyup.enter.native="loadTemplates"
        @clear="loadTemplates"
      />
      <el-checkbox v-model="mineOnly" @change="loadTemplates">仅看我的</el-checkbox>
      <el-button size="small" icon="el-icon-refresh" @click="loadTemplates">刷新</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-table
          v-loading="loading"
          :data="templateList"
          border
          height="420"
          highlight-current-row
          row-key="id"
          @current-change="handleCurrentChange"
        >
          <el-table-column prop="name" label="模板名称" min-width="160" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column label="内容" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.docCount || 0 }}/{{ scope.row.taskCount || 0 }}/{{ scope.row.fileCount || 0 }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never" class="picker-detail-card" v-loading="detailLoading">
          <template v-if="detail && detail.id">
            <div class="picker-detail-head">
              <div>
                <div class="picker-detail-title">{{ detail.name }}</div>
                <div class="picker-detail-desc">{{ detail.description || '暂无模板描述' }}</div>
              </div>
              <div class="picker-detail-tags">
                <el-tag size="mini" type="success">文档 {{ detail.docCount || 0 }}</el-tag>
                <el-tag size="mini" type="warning">任务 {{ detail.taskCount || 0 }}</el-tag>
                <el-tag size="mini">文件 {{ detail.fileCount || 0 }}</el-tag>
                <el-tag size="mini" type="info">目录 {{ detail.folderCount || 0 }}</el-tag>
                <el-tag size="mini" type="danger">活动流 {{ detail.activityCount || 0 }}</el-tag>
              </div>
            </div>

            <div v-if="detail.readmeContent" class="picker-readme">
              <div class="picker-readme-title">{{ detail.readmeTitle || 'README' }}</div>
              <pre class="picker-readme-content">{{ detail.readmeContent }}</pre>
            </div>

            <el-tabs>
              <el-tab-pane :label="`文档 (${(detail.docItems || []).length})`"></el-tab-pane>
              <el-tab-pane :label="`任务 (${(detail.taskItems || []).length})`"></el-tab-pane>
              <el-tab-pane :label="`文件 (${(detail.fileItems || []).length + (detail.folderItems || []).length})`"></el-tab-pane>
            </el-tabs>
          </template>
          <el-empty v-else description="请选择一个模板"></el-empty>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listProjectTemplates, getProjectTemplateDetail } from '@/api/projectTemplate'

export default {
  name: 'ProjectTemplatePicker',
  props: {
    currentUserId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      keyword: '',
      mineOnly: false,
      loading: false,
      detailLoading: false,
      templateList: [],
      detail: null
    }
  },
  mounted() {
    this.loadTemplates()
  },
  methods: {
    extractPayload(res) {
      if (res && typeof res === 'object') {
        if (res.data && typeof res.data === 'object' && res.data.data !== undefined) return res.data.data
        if (res.data !== undefined) return res.data
      }
      return res
    },
    normalizeTemplateList(payload) {
      const list = Array.isArray(payload?.list) ? payload.list : Array.isArray(payload) ? payload : []
      return list.map(item => ({
        ...item,
        id: item.id,
        name: item.name || '',
        category: item.category || '',
        docCount: Number(item.docCount || 0),
        taskCount: Number(item.taskCount || 0),
        fileCount: Number(item.fileCount || 0),
        folderCount: Number(item.folderCount || 0),
        activityCount: Number(item.activityCount || 0)
      }))
    },
    normalizeTemplateDetail(detail) {
      return detail || null
    },
    async loadTemplates() {
      this.loading = true
      try {
        const res = await listProjectTemplates({
          keyword: this.keyword,
          mineOnly: this.mineOnly
        })
        this.templateList = this.normalizeTemplateList(this.extractPayload(res))
        if (this.templateList.length > 0) {
          this.handleCurrentChange(this.templateList[0])
        } else {
          this.detail = null
          this.$emit('select', null)
        }
      } catch (e) {
        this.templateList = []
        this.detail = null
        this.$emit('select', null)
        this.$message.error('加载模板列表失败')
      } finally {
        this.loading = false
      }
    },
    async handleCurrentChange(row) {
      if (!row || !row.id) {
        this.detail = null
        this.$emit('select', null)
        return
      }
      this.detailLoading = true
      try {
        const res = await getProjectTemplateDetail(row.id)
        this.detail = this.normalizeTemplateDetail(this.extractPayload(res))
        this.$emit('select', this.detail)
      } catch (e) {
        this.detail = null
        this.$emit('select', null)
        this.$message.error('加载模板详情失败')
      } finally {
        this.detailLoading = false
      }
    }
  }
}
</script>

<style scoped>
.picker-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.picker-keyword {
  width: 320px;
}
.picker-detail-card {
  min-height: 420px;
}
.picker-detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.picker-detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.picker-detail-desc {
  color: #606266;
  line-height: 1.7;
}
.picker-detail-tags {
  white-space: nowrap;
}
.picker-readme {
  margin-top: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}
.picker-readme-title {
  padding: 10px 12px;
  font-weight: 600;
  border-bottom: 1px solid #ebeef5;
}
.picker-readme-content {
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 220px;
  overflow: auto;
}
</style>
