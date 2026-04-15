<template>
  <div class="content-section">
    <section class="hero-card">
      <div class="hero-badge">{{ isSelf ? '创作工作台' : '用户名片' }}</div>
      <h3 class="hero-title">
        {{ isSelf ? '让今天的内容继续累积你的影响力' : `${displayName || '这位用户'} 的动态概览` }}
      </h3>
      <p class="hero-desc">
        {{ isSelf ? '把博客、圈子内容和知识产品串起来，用户中心会更像一个正在生长的个人空间。' : '这里展示了最近的活跃状态和一些常用入口。' }}
      </p>

      <div class="hero-stats">
        <div class="hero-stat">
          <span class="hero-stat-value">{{ totalPosts }}</span>
          <span class="hero-stat-label">近30天发布</span>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-value">{{ activeDays }}</span>
          <span class="hero-stat-label">活跃天数</span>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-value">{{ peakCount }}</span>
          <span class="hero-stat-label">单日峰值</span>
        </div>
      </div>

      <div class="hero-actions">
        <button class="primary-action" @click="navigateTo('/blogwrite')">写文章</button>
        <button class="ghost-action" @click="navigateTo('/circle')">去圈子</button>
      </div>
    </section>

    <section class="quick-grid">
      <article
        v-for="item in quickLinks"
        :key="item.title"
        class="quick-card"
        @click="navigateTo(item.path)"
      >
        <div class="quick-icon">
          <i :class="item.icon"></i>
        </div>
        <div class="quick-copy">
          <h4>{{ item.title }}</h4>
          <p>{{ item.desc }}</p>
        </div>
      </article>
    </section>

    <section class="tips-card">
      <div class="tips-header">
        <h4>成长清单</h4>
        <span>近期可做</span>
      </div>
      <ul class="tips-list">
        <li>补齐头像、签名、标签后，个人页会更像完整的作者主页。</li>
        <li>连续发布几天后，热力图区块会更有层次，也更能体现活跃度。</li>
        <li>知识产品和博客数据打通后，右侧模块还可以继续扩展成作品橱窗。</li>
      </ul>
    </section>
  </div>
</template>

<script>
export default {
  name: 'ContentSection',
  props: {
    displayName: {
      type: String,
      default: ''
    },
    isSelf: {
      type: Boolean,
      default: false
    },
    activityData: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    totalPosts() {
      return this.activityData.reduce((sum, item) => sum + Number(item.count || 0), 0)
    },
    activeDays() {
      return this.activityData.filter(item => Number(item.count || 0) > 0).length
    },
    peakCount() {
      return this.activityData.reduce((max, item) => Math.max(max, Number(item.count || 0)), 0)
    },
    quickLinks() {
      return [
        {
          title: '我的博客',
          desc: '继续沉淀系统化内容',
          icon: 'el-icon-document',
          path: '/blog'
        },
        {
          title: '我的收藏',
          desc: '回看灵感和参考资料',
          icon: 'el-icon-star-off',
          path: '/collection'
        },
        {
          title: '账户中心',
          desc: '充值、优惠券与钱包',
          icon: 'el-icon-wallet',
          path: '/wallet'
        },
        {
          title: '知识产品',
          desc: '把内容转成可复用资产',
          icon: 'el-icon-reading',
          path: '/vip'
        }
      ]
    }
  },
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.content-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
  color: var(--it-text);
}

.hero-card,
.quick-card,
.tips-card {
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  border-radius: 8px;
  box-shadow: var(--it-shadow);
}

.hero-card {
  padding: 24px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--it-accent-soft) 72%, transparent), transparent 58%),
    var(--it-surface-solid);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  background: var(--it-accent-soft);
  border: 1px solid var(--it-border);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 600;
}

.hero-title {
  margin: 16px 0 10px;
  font-size: 26px;
  line-height: 1.35;
  color: var(--it-text);
}

.hero-desc {
  margin: 0;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--it-text-muted);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 8px;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}

.hero-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--it-text);
}

.hero-stat-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.primary-action,
.ghost-action {
  height: 42px;
  padding: 0 18px;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.primary-action {
  background: var(--it-primary-gradient);
  color: #ffffff;
  box-shadow: var(--it-shadow);
}

.ghost-action {
  background: var(--it-surface-solid);
  border-color: var(--it-border);
  color: var(--it-text-muted);
}

.primary-action:hover,
.ghost-action:hover,
.quick-card:hover {
  transform: translateY(-2px);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.quick-card:hover {
  border-color: var(--it-border-strong);
  background: var(--it-surface-hover);
}

.quick-icon {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 20px;
}

.quick-copy h4 {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--it-text);
}

.quick-copy p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--it-text-muted);
}

.tips-card {
  padding: 20px 22px;
}

.tips-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.tips-header h4 {
  margin: 0;
  font-size: 16px;
  color: var(--it-text);
}

.tips-header span {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: var(--it-text-muted);
}

.tips-list li {
  line-height: 1.8;
  font-size: 13px;
}

@media screen and (max-width: 768px) {
  .hero-title {
    font-size: 22px;
  }

  .hero-stats,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
