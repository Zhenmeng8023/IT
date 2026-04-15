<template>
  <section class="profile-hero-card">
    <div class="profile-header">
      <div class="avatar-wrapper">
        <el-avatar
          :size="132"
          :src="profile.avatarUrl"
          class="profile-avatar"
          @error="handleAvatarError"
        />
        <button
          v-if="isSelf"
          type="button"
          class="avatar-edit-overlay"
          @click="$emit('edit')"
        >
          <i class="el-icon-edit"></i>
          <span>编辑资料</span>
        </button>
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
    }
  },
  methods: {
    handleAvatarError() {
      this.$emit('avatar-error')
      return true
    }
  }
}
</script>

<style scoped>
.profile-hero-card {
  background: linear-gradient(168deg, #ffffff, #f8fbff);
  border: 1px solid #e6edf7;
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
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

.profile-avatar {
  border: 5px solid #ffffff;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.18);
}

.avatar-edit-overlay {
  position: absolute;
  inset: 0;
  border: none;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.54);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  font-size: 12px;
}

.profile-name {
  margin: 0;
  font-size: 24px;
  color: #111827;
}

.profile-bio {
  margin: 12px 0 0;
  color: #4b5563;
  line-height: 1.7;
}

.profile-stats {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f9fbff;
  border: 1px solid #e5eefb;
  border-radius: 14px;
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
  color: #1f2937;
}

button.stat-item {
  cursor: pointer;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: #d7e2f5;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
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
  color: #374151;
  font-size: 14px;
}

.info-item i {
  color: #409eff;
  font-size: 16px;
}

.edit-profile-btn {
  width: 100%;
  margin-top: 20px;
  border-radius: 12px;
}
</style>
