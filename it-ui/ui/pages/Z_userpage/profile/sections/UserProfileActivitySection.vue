<template>
  <section class="activity-section">
    <div class="activity-header">
      <h3>近期活跃热力图</h3>
      <span>最近 30 天综合活跃度</span>
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <span class="summary-label">提交</span>
        <span class="summary-value">{{ activitySummary.commits || 0 }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">博客</span>
        <span class="summary-value">{{ activitySummary.blogs || 0 }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">帖子</span>
        <span class="summary-value">{{ activitySummary.posts || 0 }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">点赞</span>
        <span class="summary-value">{{ activitySummary.likes || 0 }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">收藏</span>
        <span class="summary-value">{{ activitySummary.collects || 0 }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">日志</span>
        <span class="summary-value">{{ activitySummary.logs || 0 }}</span>
      </div>
    </div>

    <div class="activity-layout">
      <div class="heatmap-shell">
        <HeatmapTracker :activity-data="activityData" :loading="loading" :days="30" />
      </div>

      <div class="content-shell">
        <ContentSection
          :display-name="displayName"
          :is-self="isSelf"
          :activity-data="activityData"
        />
      </div>
    </div>
  </section>
</template>

<script>
import HeatmapTracker from '../../components/HeatmapTracker.vue'
import ContentSection from '../../components/ContentSection.vue'

export default {
  name: 'UserProfileActivitySection',
  components: {
    HeatmapTracker,
    ContentSection
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    isSelf: {
      type: Boolean,
      default: false
    },
    displayName: {
      type: String,
      default: ''
    },
    activityData: {
      type: Array,
      default: () => []
    },
    activitySummary: {
      type: Object,
      default: () => ({})
    }
  }
}
</script>

<style scoped>
.activity-section {
  margin-top: 18px;
  padding: 20px;
  border-radius: 20px;
  border: 1px solid #e6edf7;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.activity-header h3 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.activity-header span {
  color: #6b7280;
  font-size: 13px;
}

.summary-row {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.summary-item {
  padding: 10px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #e5edf8;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  color: #6b7280;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.activity-layout {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.heatmap-shell,
.content-shell {
  border-radius: 16px;
  overflow: hidden;
}

.heatmap-shell {
  border: 1px solid #e5edf8;
  padding: 14px;
  background: #fdfefe;
}

@media (max-width: 1300px) {
  .summary-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .activity-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
