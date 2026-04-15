<template>
  <section v-if="visible" class="project-section">
    <div class="section-header">
      <h3>{{ isSelf ? '我的发布' : 'TA 的发布' }}</h3>
      <el-radio-group :value="activeTab" size="small" @input="$emit('change-tab', $event)">
        <el-radio-button label="blogs">博客</el-radio-button>
        <el-radio-button label="posts">帖子</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="section-body">
      <template v-if="activeTab === 'blogs'">
        <el-card
          v-for="blog in blogList"
          :key="blog.id"
          class="content-card"
          shadow="hover"
        >
          <div class="card-main" @click="$emit('open-blog', blog.id)">
            <h4>{{ blog.title }}</h4>
            <p>{{ summaryText(blog) }}</p>
            <div class="meta-row">
              <span><i class="el-icon-view"></i>{{ blog.viewCount || 0 }}</span>
              <span><i class="el-icon-star-off"></i>{{ blog.likeCount || 0 }}</span>
              <span><i class="el-icon-time"></i>{{ formatDate(blog.createTime) }}</span>
            </div>
          </div>

          <div v-if="isSelf" class="card-actions">
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="mini"
              circle
              @click.stop="$emit('delete-blog', blog)"
            />
          </div>
        </el-card>

        <div v-if="!blogList.length" class="empty-state">
          <i class="el-icon-document"></i>
          <p>暂无博客</p>
        </div>
      </template>

      <template v-else>
        <el-card
          v-for="post in postList"
          :key="post.id"
          class="content-card"
          shadow="hover"
        >
          <div class="card-main" @click="$emit('open-post', post.id)">
            <h4>{{ post.title }}</h4>
            <p>{{ summaryText(post) }}</p>
            <div class="meta-row">
              <span><i class="el-icon-chat-dot-round"></i>{{ post.commentCount || 0 }}</span>
              <span><i class="el-icon-star-off"></i>{{ post.likeCount || 0 }}</span>
              <span><i class="el-icon-time"></i>{{ formatDate(post.createTime) }}</span>
            </div>
          </div>

          <div v-if="isSelf" class="card-actions">
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="mini"
              circle
              @click.stop="$emit('delete-post', post)"
            />
          </div>
        </el-card>

        <div v-if="!postList.length" class="empty-state">
          <i class="el-icon-chat-dot-round"></i>
          <p>暂无帖子</p>
        </div>
      </template>
    </div>
  </section>
</template>

<script>
export default {
  name: 'UserProfileProjectSection',
  props: {
    visible: {
      type: Boolean,
      default: true
    },
    isSelf: {
      type: Boolean,
      default: false
    },
    activeTab: {
      type: String,
      default: 'blogs'
    },
    loading: {
      type: Boolean,
      default: false
    },
    blogList: {
      type: Array,
      default: () => []
    },
    postList: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    summaryText(item) {
      const raw = item.summary || item.content || ''
      const plain = String(raw).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
      if (!plain) return '暂无摘要'
      return plain.length > 110 ? `${plain.slice(0, 110)}...` : plain
    },
    formatDate(value) {
      if (!value) return '未知时间'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '未知时间'
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
  }
}
</script>

<style scoped>
.project-section {
  margin-top: 18px;
  padding: 20px;
  border-radius: 20px;
  border: 1px solid #e6edf7;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.section-header h3 {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.section-body {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.content-card {
  border-radius: 14px;
  border: 1px solid #e5edf8;
}

.content-card ::v-deep .el-card__body {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.card-main {
  flex: 1;
  cursor: pointer;
}

.card-main h4 {
  margin: 0;
  font-size: 17px;
  color: #111827;
}

.card-main p {
  margin: 10px 0 0;
  line-height: 1.7;
  color: #4b5563;
}

.meta-row {
  margin-top: 12px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: #6b7280;
  font-size: 13px;
}

.meta-row span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.card-actions {
  margin-top: 2px;
}

.empty-state {
  padding: 26px 14px;
  border-radius: 14px;
  background: #f8fbff;
  text-align: center;
  color: #6b7280;
}

.empty-state i {
  font-size: 28px;
}

.empty-state p {
  margin: 8px 0 0;
}
</style>
