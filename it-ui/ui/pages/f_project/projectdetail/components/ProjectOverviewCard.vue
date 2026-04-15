<template>
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

        <div v-if="tagList.length" class="tag-list">
          <el-tag v-for="tag in tagList" :key="tag" size="mini" effect="plain" class="tag-item">{{ tag }}</el-tag>
        </div>

        <div class="meta-row">
          <div class="author-box">
            <el-avatar :size="40" :src="project.authorAvatar || ''">{{ (project.authorName || '未知作者').slice(0, 1) }}</el-avatar>
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
</template>

<script>
export default {
  name: 'ProjectOverviewCard',
  props: {
    project: {
      type: Object,
      default: () => ({})
    },
    categoryLabel: {
      type: String,
      default: ''
    },
    statusTagType: {
      type: String,
      default: 'info'
    },
    statusLabel: {
      type: String,
      default: ''
    },
    visibilityLabel: {
      type: String,
      default: ''
    },
    tagList: {
      type: Array,
      default: () => []
    },
    formatTime: {
      type: Function,
      default: () => '-'
    }
  }
}
</script>
