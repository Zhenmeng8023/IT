<template>
  <div class="history-page">
    <nav class="navbar">
      <div class="navbar-content">
        <div class="logo" @click="goBack">
          <span class="logo-icon">●</span>
          <span class="logo-text">ITSpace</span>
        </div>
        <el-button type="text" class="back-btn" @click="goBack">
          <i class="el-icon-arrow-left"></i> 返回个人主页
        </el-button>
      </div>
    </nav>

    <div class="main-content">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-kicker">Browsing Chronicle</span>
          <h1 class="hero-title">浏览历史</h1>
          <p class="hero-subtitle">把最近浏览过的内容沉淀成一张清晰的足迹面板，方便你继续阅读与回看灵感。</p>
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
            <span class="panel-tag">Profile Snapshot</span>
            <h2>个人信息</h2>
          </div>
          <div class="profile-items">
            <div class="profile-item">
              <span class="item-label">昵称</span>
              <span class="item-value">{{ username || '未设置昵称' }}</span>
            </div>
            <div class="profile-item">
              <span class="item-label">生日</span>
              <span class="item-value">{{ formatDate(userbrithday) || '未设置生日' }}</span>
            </div>
            <div class="profile-item">
              <span class="item-label">邮箱</span>
              <span class="item-value">{{ useremail || '未设置邮箱' }}</span>
            </div>
            <div class="profile-item">
              <span class="item-label">联系地址</span>
              <span class="item-value">{{ useraddress || '未设置地址' }}</span>
            </div>
            <div class="profile-item">
              <span class="item-label">性别</span>
              <span class="item-value">{{ usersex || '未设置性别' }}</span>
            </div>
            <div class="profile-item">
              <span class="item-label">标签</span>
              <span class="item-value">{{ userbog.length ? userbog.join('、') : '未设置标签' }}</span>
            </div>
            <div class="profile-item">
              <span class="item-label">签名</span>
              <span class="item-value multiline">{{ usersign || '这个用户还没有留下签名。' }}</span>
            </div>
          </div>
        </aside>

        <section class="history-panel">
          <div class="panel-header">
            <span class="panel-tag">Recent Visits</span>
            <h2>最近阅读内容</h2>
          </div>

          <div v-if="historyList.length" class="history-list">
            <article v-for="(item, index) in historyList" :key="`${item.title}-${index}`" class="history-card">
              <div class="history-cover">
                <el-image :src="item.pic" fit="cover" :preview-src-list="[item.pic]" class="cover-image" />
                <span class="history-index">0{{ index + 1 }}</span>
              </div>
              <div class="history-body">
                <div class="history-meta">
                  <span class="history-author">
                    <i class="el-icon-user-solid"></i>
                    {{ item.zuozhe }}
                  </span>
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
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.18), transparent 28%),
    radial-gradient(circle at top right, rgba(110, 231, 255, 0.12), transparent 24%),
    linear-gradient(135deg, #060914 0%, #0d1321 45%, #151d2d 100%);
  color: #ffffff;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

.navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(6, 9, 20, 0.78);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.navbar-content {
  max-width: 1320px;
  margin: 0 auto;
  padding: 14px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.logo-icon {
  color: #6ee7ff;
  font-size: 28px;
  line-height: 1;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffffff, #7dd3fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.back-btn {
  color: #d6e7ff;
  font-size: 14px;
}

.back-btn:hover {
  color: #6ee7ff;
}

.main-content {
  max-width: 1320px;
  margin: 0 auto;
  padding: 36px 28px 56px;
}

.hero-panel,
.profile-panel,
.history-panel {
  background: rgba(12, 18, 32, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24px 50px rgba(2, 6, 23, 0.35);
  backdrop-filter: blur(20px);
}

.hero-panel {
  border-radius: 32px;
  padding: 32px;
  margin-bottom: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 24px;
  position: relative;
  overflow: hidden;
}

.hero-panel::after {
  content: '';
  position: absolute;
  inset: auto -60px -80px auto;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(110, 231, 255, 0.26), transparent 68%);
  pointer-events: none;
}

.hero-kicker,
.panel-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(110, 231, 255, 0.12);
  color: #9eeaf9;
  border: 1px solid rgba(110, 231, 255, 0.18);
}

.hero-title {
  margin: 14px 0 10px;
  font-size: clamp(32px, 5vw, 52px);
  line-height: 1.04;
}

.hero-subtitle {
  margin: 0;
  max-width: 640px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(214, 231, 255, 0.78);
}

.hero-stats {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.hero-stat {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
}

.hero-stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: rgba(214, 231, 255, 0.6);
}

.hero-stat-value {
  display: block;
  font-size: 20px;
  line-height: 1.4;
  color: #ffffff;
}

.content-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.profile-panel,
.history-panel {
  border-radius: 28px;
  padding: 28px;
}

.panel-header {
  margin-bottom: 22px;
}

.panel-header h2 {
  margin: 14px 0 0;
  font-size: 24px;
}

.profile-items {
  display: grid;
  gap: 14px;
}

.profile-item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.item-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: rgba(214, 231, 255, 0.56);
}

.item-value {
  display: block;
  font-size: 14px;
  line-height: 1.7;
  color: #ffffff;
}

.item-value.multiline {
  color: rgba(235, 244, 255, 0.78);
}

.history-list {
  display: grid;
  gap: 18px;
}

.history-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.02));
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: transform 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease;
}

.history-card:hover {
  transform: translateY(-4px);
  border-color: rgba(110, 231, 255, 0.28);
  box-shadow: 0 18px 30px rgba(2, 6, 23, 0.24);
}

.history-cover {
  position: relative;
  min-height: 132px;
}

.cover-image {
  width: 100%;
  height: 100%;
  border-radius: 20px;
  overflow: hidden;
}

.history-index {
  position: absolute;
  top: 12px;
  left: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(6, 9, 20, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #9eeaf9;
  font-size: 12px;
  font-weight: 700;
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
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.history-author,
.history-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
}

.history-author {
  color: rgba(235, 244, 255, 0.82);
  background: rgba(255, 255, 255, 0.05);
}

.history-chip {
  color: #a7f3d0;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.16);
}

.history-title {
  margin: 0 0 12px;
  font-size: 22px;
  line-height: 1.35;
}

.history-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(214, 231, 255, 0.68);
}

.empty-state {
  padding: 72px 24px;
  text-align: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px dashed rgba(255, 255, 255, 0.12);
}

.empty-state i {
  font-size: 54px;
  color: rgba(110, 231, 255, 0.6);
  margin-bottom: 18px;
}

.empty-state h3 {
  margin: 0 0 10px;
  font-size: 22px;
}

.empty-state p {
  margin: 0;
  color: rgba(214, 231, 255, 0.68);
}

@media screen and (max-width: 1024px) {
  .hero-panel,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 768px) {
  .navbar-content,
  .main-content {
    padding-left: 18px;
    padding-right: 18px;
  }

  .hero-panel,
  .profile-panel,
  .history-panel {
    padding: 22px;
  }

  .history-card {
    grid-template-columns: 1fr;
  }

  .history-cover {
    min-height: 200px;
  }
}
</style>
