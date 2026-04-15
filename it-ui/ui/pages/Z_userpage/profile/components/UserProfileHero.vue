<template>
  <section class="profile-hero-card">
    <div class="profile-header">
      <div class="avatar-wrapper" :style="avatarPositionStyle">
        <div class="profile-avatar">
          <img
            :src="avatarSrc"
            :style="avatarImageStyle"
            alt="用户头像"
            @error="handleAvatarError"
          >
        </div>
      </div>

      <h2 class="profile-name">{{ displayName }}</h2>
      <p class="profile-bio">{{ profile.bio || '这个用户很懒，什么都没留下~' }}</p>
    </div>

    <div class="profile-stats">
      <button type="button" class="stat-item" @click="$emit('collect')">
        <span class="stat-value">{{ stats.totalCollects || 0 }}</span>
        <span class="stat-label">收藏</span>
      </button>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.totalLikes || 0 }}</span>
        <span class="stat-label">获赞</span>
      </div>
      <div class="stat-divider"></div>
      <button type="button" class="stat-item" @click="$emit('knowledge')">
        <span class="stat-value">{{ stats.totalKnowledge || 0 }}</span>
        <span class="stat-label">知识产品</span>
      </button>
    </div>

    <div class="profile-info">
      <div class="info-item">
        <i class="el-icon-location-outline"></i>
        <span>{{ profile.regionName || '未设置居住地' }}</span>
      </div>
      <div class="info-item">
        <i class="el-icon-message"></i>
        <span>{{ profile.email || '未设置邮箱' }}</span>
      </div>
      <div class="info-item">
        <i class="el-icon-phone"></i>
        <span>{{ profile.phone || '未设置电话' }}</span>
      </div>
      <div class="info-item">
        <i class="el-icon-date"></i>
        <span>{{ birthdayText }}</span>
      </div>
      <div class="info-item">
        <i class="el-icon-present"></i>
        <span>性别：{{ genderText }}</span>
      </div>
      <div class="info-item tags">
        <i class="el-icon-collection-tag"></i>
        <el-tag v-if="profile.authorTagName" size="small">{{ profile.authorTagName }}</el-tag>
        <span v-else>未设置标签</span>
      </div>
    </div>

    <el-button v-if="isSelf" type="primary" class="edit-profile-btn" @click="$emit('edit')">
      <i class="el-icon-edit"></i>
      编辑资料
    </el-button>
  </section>
</template>

<script>
export default {
  name: 'UserProfileHero',
  props: {
    profile: {
      type: Object,
      default: () => ({})
    },
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
    displayName() {
      return this.profile.nickname || this.profile.username || '未命名用户'
    },
    avatarSrc() {
      return this.profile.avatarUrl || '/pic/choubi.jpg'
    },
    birthdayText() {
      if (!this.profile.birthday) return '未设置生日'
      const date = new Date(this.profile.birthday)
      if (Number.isNaN(date.getTime())) return '未设置生日'
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    genderText() {
      if (this.profile.gender === 'male') return '男'
      if (this.profile.gender === 'female') return '女'
      return '其他'
    },
    avatarPositionStyle() {
      const position = this.parseAvatarPosition(this.profile.avatarUrl)
      return {
        '--avatar-position': `${position.x}% ${position.y}%`
      }
    },
    avatarImageStyle() {
      const position = this.parseAvatarPosition(this.profile.avatarUrl)
      return {
        objectFit: 'cover',
        objectPosition: `${position.x}% ${position.y}%`
      }
    }
  },
  methods: {
    handleAvatarError() {
      this.$emit('avatar-error')
      return true
    },
    parseAvatarPosition(url) {
      const hash = String(url || '').split('#')[1] || ''
      const match = hash.match(/avatar-position=(\d+(?:\.\d+)?),(\d+(?:\.\d+)?)/)
      if (!match) {
        return { x: 50, y: 50 }
      }
      return {
        x: this.clampPosition(match[1]),
        y: this.clampPosition(match[2])
      }
    },
    clampPosition(value) {
      const numberValue = Number(value)
      if (!Number.isFinite(numberValue)) return 50
      return Math.max(0, Math.min(100, Math.round(numberValue)))
    }
  }
}
</script>

<style scoped>
.profile-hero-card {
  background: linear-gradient(180deg, var(--it-surface-elevated), var(--it-surface));
  border: 1px solid var(--it-border);
  border-radius: 8px;
  box-shadow: var(--it-shadow);
  padding: 24px;
}

.profile-header {
  text-align: center;
}

.avatar-wrapper {
  position: relative;
  width: fit-content;
  margin: 0 auto 16px;
}

.avatar-wrapper .avatar-edit-overlay,
.avatar-wrapper > button {
  display: none !important;
}

.profile-avatar {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  overflow: hidden;
  border: 5px solid var(--it-surface-solid);
  box-shadow: 0 12px 28px color-mix(in srgb, var(--it-accent) 22%, transparent);
}

.profile-avatar img {
  display: block;
  width: 100%;
  height: 100%;
}

.profile-name {
  margin: 0;
  font-size: 24px;
  color: var(--it-text);
}

.profile-bio {
  margin: 12px 0 0;
  color: var(--it-text-muted);
  line-height: 1.7;
}

.profile-stats {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
  border-radius: 8px;
  padding: 8px;
}

.stat-item {
  flex: 1;
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
  color: var(--it-text);
}

button.stat-item {
  cursor: pointer;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: var(--it-border);
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.profile-info {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--it-text-muted);
  font-size: 14px;
}

.info-item i {
  color: var(--it-accent);
  font-size: 16px;
}

.edit-profile-btn {
  width: 100%;
  margin-top: 20px;
  border-radius: 8px;
}
</style>
