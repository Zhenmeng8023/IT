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
  background: var(--it-surface);
  border: 1px solid var(--it-border);
  border-radius: 8px;
  box-shadow: var(--it-shadow);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-header {
  text-align: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--it-border);
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
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--it-surface-solid);
}

.profile-avatar img {
  display: block;
  width: 100%;
  height: 100%;
}

.profile-name {
  margin: 0;
  font-size: 22px;
  color: var(--it-text);
}

.profile-bio {
  margin: 8px 0 0;
  color: var(--it-text-muted);
  line-height: 1.6;
  font-size: 13px;
}

.profile-info {
  display: grid;
  gap: 8px;
  flex: 1;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--it-text-muted);
  font-size: 13px;
}

.info-item i {
  color: var(--it-accent);
  font-size: 14px;
}

.edit-profile-btn {
  width: 100%;
  border-radius: 8px;
  margin-top: auto;
}
</style>
