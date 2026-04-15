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
        {{ isSelf ? '我的收藏' : 'TA 的收藏' }}
      </el-button>
      <el-button v-if="isSelf" type="success" plain class="action-btn" @click="$emit('toggle-project')">
        <i class="el-icon-s-promotion"></i>
        我的发布
      </el-button>
      <el-button type="primary" plain class="action-btn" @click="$emit('knowledge')">
        <i class="el-icon-reading"></i>
        {{ isSelf ? '我的知识产品' : 'TA 的知识产品' }}
      </el-button>
      <el-button v-if="isSelf" type="info" plain class="action-btn" @click="$emit('coupons')">
        <i class="el-icon-ticket"></i>
        我的优惠券
      </el-button>
      <el-button v-if="isSelf" type="info" plain class="action-btn" @click="$emit('wallet')">
        <i class="el-icon-wallet"></i>
        账户充值
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
  gap: 18px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
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
  background: linear-gradient(135deg, #fb7185, #f43f5e);
}

.collect-icon {
  background: linear-gradient(135deg, #f59e0b, #f97316);
}

.knowledge-icon {
  background: linear-gradient(135deg, #06b6d4, #0ea5e9);
}

.revenue-icon {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  color: var(--it-text);
  font-weight: 700;
  font-size: 20px;
}

.stat-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-btn {
  border-radius: 8px;
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
