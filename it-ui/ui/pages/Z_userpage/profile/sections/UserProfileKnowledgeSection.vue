<template>
  <section class="knowledge-section">
    <div class="section-header">
      <h3>{{ isSelf ? '我的知识产品' : 'TA 的知识产品' }}</h3>
      <el-button
        v-if="isSelf"
        size="small"
        type="primary"
        icon="el-icon-plus"
        @click="$emit('create')"
      >
        新建知识产品
      </el-button>
    </div>

    <div v-loading="loading" class="section-body">
      <el-card
        v-for="item in knowledgeList"
        :key="item.id"
        class="knowledge-card"
        shadow="hover"
      >
        <div class="card-main" @click="$emit('open-detail', item.id)">
          <h4>{{ item.title }}</h4>
          <p>{{ summaryText(item) }}</p>
          <div class="meta-row">
            <span><i class="el-icon-view"></i>{{ item.viewCount || 0 }}</span>
            <span><i class="el-icon-star-off"></i>{{ item.likeCount || 0 }}</span>
            <span><i class="el-icon-money"></i>¥{{ Number(item.price || 0).toFixed(2) }}</span>
            <span><i class="el-icon-time"></i>{{ formatDate(item.createTime) }}</span>
            <el-tag :type="item.status === 'published' ? 'success' : 'info'" size="mini">
              {{ item.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </div>
        </div>

        <div v-if="isSelf" class="card-actions">
          <el-button
            type="primary"
            icon="el-icon-edit"
            size="mini"
            circle
            @click.stop="$emit('edit', item)"
          />
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="mini"
            circle
            @click.stop="$emit('delete', item)"
          />
        </div>
      </el-card>

      <div v-if="!knowledgeList.length" class="empty-state">
        <i class="el-icon-reading"></i>
        <p>{{ isSelf ? '暂无知识产品，开始创建第一篇付费内容吧。' : '暂无公开知识产品' }}</p>
      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'UserProfileKnowledgeSection',
  props: {
    isSelf: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    knowledgeList: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    summaryText(item) {
      const raw = item.summary || item.content || ''
      const plain = String(raw).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
      if (!plain) return '暂无摘要'
      return plain.length > 110 ? `${plain.slice(0, 110)}...` : plain
    },
    formatDate(value) {
      if (!value) return '未知时间'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '未知时间'
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
}
</script>

<style scoped>
.knowledge-section {
  margin-top: 18px;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.section-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--it-text);
}

.section-body {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.knowledge-card {
  border-radius: 8px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.knowledge-card ::v-deep .el-card__body {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.card-main {
  flex: 1;
  cursor: pointer;
}

.card-main h4 {
  margin: 0;
  font-size: 17px;
  color: var(--it-text);
}

.card-main p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: var(--it-text-muted);
}

.meta-row {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--it-text-subtle);
  font-size: 13px;
}

.meta-row span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  padding: 26px 14px;
  border-radius: 8px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-muted);
  text-align: center;
  color: var(--it-text-subtle);
}

.empty-state i {
  font-size: 28px;
}

.empty-state p {
  margin: 8px 0 0;
}
</style>
