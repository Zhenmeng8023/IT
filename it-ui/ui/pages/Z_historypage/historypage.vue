<template>
  <div class="history-page">
    <section class="page-hero">
      <div>
        <span class="hero-badge">Browsing Chronicle</span>
        <h1 class="hero-title">浏览历史</h1>
        <p class="hero-subtitle">把最近浏览过的内容沉淀成清晰的足迹面板，方便继续阅读与回看灵感。</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <span class="hero-stat-label">浏览条目</span>
          <strong class="hero-stat-value">{{ historyList.length }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-label">账号昵称</span>
          <strong class="hero-stat-value">{{ username || '未设置' }}</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat-label">兴趣标签</span>
          <strong class="hero-stat-value">{{ userbog.length ? userbog.join(' / ') : '待完善' }}</strong>
        </div>
      </div>
    </section>

    <div class="content-grid">
      <aside class="profile-panel">
        <div class="panel-header">
          <div>
            <span class="panel-tag">Profile Snapshot</span>
            <h2>个人信息</h2>
          </div>
          <el-button plain size="small" @click="goBack">返回个人主页</el-button>
        </div>
        <div class="profile-items">
          <div class="profile-item"><span class="item-label">昵称</span><span class="item-value">{{ username || '未设置昵称' }}</span></div>
          <div class="profile-item"><span class="item-label">生日</span><span class="item-value">{{ formatDate(userbrithday) || '未设置生日' }}</span></div>
          <div class="profile-item"><span class="item-label">邮箱</span><span class="item-value">{{ useremail || '未设置邮箱' }}</span></div>
          <div class="profile-item"><span class="item-label">联系地址</span><span class="item-value">{{ useraddress || '未设置地址' }}</span></div>
          <div class="profile-item"><span class="item-label">性别</span><span class="item-value">{{ usersex || '未设置性别' }}</span></div>
          <div class="profile-item"><span class="item-label">标签</span><span class="item-value">{{ userbog.length ? userbog.join('、') : '未设置标签' }}</span></div>
          <div class="profile-item"><span class="item-label">签名</span><span class="item-value multiline">{{ usersign || '这个用户还没有留下签名。' }}</span></div>
        </div>
      </aside>

      <section class="history-panel">
        <div class="panel-header">
          <div>
            <span class="panel-tag">Recent Visits</span>
            <h2>最近阅读内容</h2>
          </div>
        </div>

        <div v-if="historyList.length" class="history-list">
          <article v-for="(item, index) in historyList" :key="`${item.title}-${index}`" class="history-card">
            <div class="history-cover">
              <el-image :src="item.pic" fit="cover" :preview-src-list="[item.pic]" class="cover-image" />
              <span class="history-index">{{ String(index + 1).padStart(2, '0') }}</span>
            </div>
            <div class="history-body">
              <div class="history-meta">
                <span class="history-author"><i class="el-icon-user-solid"></i>{{ item.zuozhe }}</span>
                <span class="history-chip">已浏览</span>
              </div>
              <h3 class="history-title">{{ item.title }}</h3>
              <p class="history-desc">保留最近一次阅读入口，方便继续回看内容脉络与作者观点。</p>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <i class="el-icon-time"></i>
          <h3>暂无历史记录</h3>
          <p>先去看看博客或圈子内容，这里会自动为你沉淀浏览足迹。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
export default {
    layout: 'default',
    data() {
        return {
            historyList: [
                {
                    zuozhe: '历史记录1的博客作者',
                    title: '这是历史记录1博客的页面跳转',
                    pic:'/pic/choubi.jpg'
                },
                {
                    zuozhe: '历史记录2的博客作者',
                    title: '这是历史记录2博客的页面跳转',
                    pic:'/pic/choubi.jpg'
                }
            ],
            username:'',          //昵称
            userbrithday: '',     //生日
            useremail: '',        //邮箱
            useraddress: '',      //联系地址
            usersex: '',          //性别
            userbog:[],           //标签
            usersign: '',         //签名
        }
    },
    methods: {
        formatDate(date) {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}年${month}月${day}日`;
        },
        goBack() {
            this.$router.push('/user');
        }
    },
    beforeRouteLeave(to, from, next) {
        // 在离开页面前恢复body默认样式
        document.body.style.overflow = '';
        next();
    },
    destroyed() {
        // 组件销毁时确保恢复body样式
        document.body.style.overflow = '';
    }
}
</script>

<style scoped>
.history-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 0 48px;
  color: var(--it-text);
}

.page-hero,
.profile-panel,
.history-panel {
  border-radius: var(--it-radius-card-lg);
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.page-hero {
  padding: 30px 32px;
  margin-bottom: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(240px, 1fr);
  gap: 20px;
}

.hero-badge,
.panel-tag,
.history-chip,
.history-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 12px;
  font-weight: 700;
}

.hero-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 44px);
  line-height: 1.08;
}

.hero-subtitle,
.history-desc,
.item-label,
.hero-stat-label {
  color: var(--it-text-muted);
  line-height: 1.75;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-stat {
  padding: 18px 20px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.hero-stat-value {
  display: block;
  margin-top: 10px;
  font-size: 22px;
  color: var(--it-text);
}

.content-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.profile-panel,
.history-panel {
  padding: 22px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.panel-header h2 {
  margin: 12px 0 0;
  font-size: 24px;
}

.profile-items {
  display: grid;
  gap: 12px;
}

.profile-item {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.item-value {
  color: var(--it-text);
  font-weight: 600;
}

.multiline {
  white-space: pre-wrap;
  line-height: 1.7;
}

.history-list {
  display: grid;
  gap: 16px;
}

.history-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  padding: 16px;
  border-radius: var(--it-radius-card);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.history-cover {
  position: relative;
}

.cover-image,
.history-cover :deep(.el-image) {
  width: 100%;
  height: 160px;
  border-radius: 18px;
  overflow: hidden;
}

.history-index {
  position: absolute;
  top: 12px;
  left: 12px;
}

.history-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.history-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--it-text-light);
  font-size: 13px;
}

.history-title {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.35;
}

.empty-state {
  min-height: 260px;
  border-radius: var(--it-radius-card);
  border: 1px dashed var(--it-border-strong);
  background: color-mix(in srgb, var(--it-surface-solid) 88%, transparent);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.empty-state i {
  font-size: 30px;
  color: var(--it-accent);
}

@media (max-width: 1100px) {
  .content-grid,
  .page-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-stats,
  .history-card {
    grid-template-columns: 1fr;
  }
}
</style>
