<template>
  <section class="profile-stats-panel">
    <div class="stats-cards">
      <article class="stat-card">
        <div class="stat-icon like-icon">
          <i class="el-icon-star-off"></i>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ stats.totalLikes || 0 }}</div>
          <div class="stat-label">总获赞</div>
        </div>
      </article>

      <article class="stat-card">
        <div class="stat-icon collect-icon">
          <i class="el-icon-collection"></i>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ stats.totalCollects || 0 }}</div>
          <div class="stat-label">收藏数</div>
        </div>
      </article>

      <article
        v-if="isSelf"
        class="stat-card stat-card--action"
        role="button"
        tabindex="0"
        @click="$emit('history')"
        @keyup.enter="$emit('history')"
      >
        <div class="stat-icon history-icon">
          <i class="el-icon-time"></i>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ stats.historyCount || 0 }}</div>
          <div class="stat-label">浏览历史</div>
        </div>
      </article>

      <article class="stat-card">
        <div class="stat-icon knowledge-icon">
          <i class="el-icon-reading"></i>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ stats.totalKnowledge || 0 }}</div>
          <div class="stat-label">知识产品</div>
        </div>
      </article>

      <article class="stat-card">
        <div class="stat-icon revenue-icon">
          <i class="el-icon-money"></i>
        </div>
        <div class="stat-info">
          <div class="stat-number">{{ totalRevenueText }}</div>
          <div class="stat-label">总收益</div>
        </div>
      </article>
    </div>

    <div class="action-buttons">
      <el-button type="warning" plain class="action-btn" @click="$emit('collect')">
        <i class="el-icon-star-off"></i>
        收藏
      </el-button>
      <el-button v-if="isSelf" type="success" plain class="action-btn" @click="$emit('toggle-project')">
        <i class="el-icon-s-promotion"></i>
        发布
      </el-button>
      <el-button type="primary" plain class="action-btn" @click="$emit('knowledge')">
        <i class="el-icon-reading"></i>
        知识产品
      </el-button>
      <el-button v-if="isSelf" type="info" plain class="action-btn" @click="$emit('history')">
        <i class="el-icon-time"></i>
        历史
      </el-button>
      <el-button v-if="isSelf" type="info" plain class="action-btn" @click="$emit('coupons')">
        <i class="el-icon-ticket"></i>
        优惠券
      </el-button>
      <el-button v-if="isSelf" type="info" plain class="action-btn" @click="$emit('wallet')">
        <i class="el-icon-wallet"></i>
        钱包
      </el-button>
    </div>
  </section>
</template>

<script>
export default {
  name: 'UserProfileStats',
  props: {
    stats: {
      type: Object,
      default: () => ({})
    },
    isSelf: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    totalRevenueText() {
      if (this.stats.totalRevenue === null || this.stats.totalRevenue === undefined) return '--'
      return `¥${Number(this.stats.totalRevenue || 0).toFixed(2)}`
    }
  }
}
</script>

<style scoped>
.profile-stats-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--it-border);
  background: var(--it-surface);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.like-icon {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-danger) 78%, white), var(--it-danger));
}

.collect-icon {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-warning) 78%, white), var(--it-warning));
}

.knowledge-icon {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 72%, white), var(--it-accent));
}

.history-icon {
  background: linear-gradient(135deg, color-mix(in srgb, #6d7c90 78%, white), #4d5c6d);
}

.revenue-icon {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 76%, white), var(--it-success));
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  color: var(--it-text);
  font-weight: 700;
  font-size: 18px;
}

.stat-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.stat-card--action {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.stat-card--action:hover,
.stat-card--action:focus {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(31, 51, 73, 0.08);
  border-color: color-mix(in srgb, var(--it-accent) 30%, var(--it-border));
  outline: none;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-btn {
  border-radius: 8px;
  min-width: 0;
}

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
