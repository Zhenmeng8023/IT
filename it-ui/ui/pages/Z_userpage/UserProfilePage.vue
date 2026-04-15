<template>
  <div class="user-profile-page">
    <div class="profile-shell" v-loading="pageLoading">
      <el-alert
        v-if="pageError"
        :title="pageError"
        type="error"
        show-icon
        :closable="false"
        class="page-error"
      />

      <div v-else class="profile-layout">
        <aside class="left-column">
          <UserProfileHero
            :profile="profile"
            :stats="stats"
            :is-self="isSelfMode"
            @edit="openEditDialog"
            @collect="handleCollectClick"
            @knowledge="scrollToKnowledgeSection"
            @avatar-error="handleAvatarError"
          />
        </aside>

        <main class="main-column">
          <UserProfileStats
            :stats="stats"
            :is-self="isSelfMode"
            @collect="handleCollectClick"
            @toggle-project="handleToggleProjectSection"
            @knowledge="scrollToKnowledgeSection"
            @coupons="handleCouponsClick"
            @wallet="handleWalletClick"
          />

          <div ref="projectSectionRef">
            <UserProfileProjectSection
              :visible="showProjectSection"
              :is-self="isSelfMode"
              :loading="sectionLoading"
              :active-tab="activePostTab"
              :blog-list="blogList"
              :post-list="postList"
              @change-tab="handlePostTabChange"
              @open-blog="openBlogDetail"
              @open-post="openPostDetail"
              @delete-blog="handleDeleteBlog"
              @delete-post="handleDeletePost"
            />
          </div>

          <div ref="knowledgeSectionRef">
            <UserProfileKnowledgeSection
              :is-self="isSelfMode"
              :loading="sectionLoading"
              :knowledge-list="knowledgeList"
              @open-detail="openKnowledgeDetail"
              @create="handleCreateKnowledge"
              @edit="handleEditKnowledge"
              @delete="handleDeleteKnowledge"
            />
          </div>

          <UserProfileActivitySection
            :loading="sectionLoading"
            :is-self="isSelfMode"
            :display-name="profile.nickname || profile.username"
            :activity-data="activityData"
            :activity-summary="activitySummary"
          />
        </main>
      </div>
    </div>

    <el-dialog
      title="编辑个人资料"
      :visible.sync="dialogFormVisible"
      width="520px"
      class="edit-dialog"
      :close-on-click-modal="false"
      :append-to-body="true"
      @close="handleDialogClose"
    >
      <el-form ref="form" :model="formData" label-width="86px" class="edit-form">
        <el-form-item label="头像">
          <div class="avatar-selector">
            <div
              v-for="avatar in avatarOptions"
              :key="avatar.value"
              class="avatar-option"
              :class="{ active: selectedAvatarUrl === avatar.value }"
              @click="selectAvatar(avatar.value)"
            >
              <el-avatar :size="56" :src="avatar.value"></el-avatar>
              <span>{{ avatar.label }}</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="昵称">
          <el-input v-model="formData.nickname" clearable placeholder="请输入昵称"></el-input>
        </el-form-item>

        <el-form-item label="电话">
          <el-input v-model="formData.userphone" clearable placeholder="请输入电话"></el-input>
        </el-form-item>

        <el-form-item label="生日">
          <el-date-picker
            v-model="formData.userbrithday"
            type="date"
            value-format="yyyy-MM-dd"
            format="yyyy-MM-dd"
            placeholder="请选择生日"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="formData.usersex">
            <el-radio label="male">男</el-radio>
            <el-radio label="female">女</el-radio>
            <el-radio label="other">其他</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="签名">
          <el-input
            v-model="formData.usersign"
            type="textarea"
            :rows="3"
            placeholder="留下一句个人签名"
            clearable
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-cascader
            v-model="formData.authorTagId"
            :options="tagList"
            :props="tagCascaderProps"
            clearable
            placeholder="请选择标签"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="地区">
          <el-cascader
            v-model="formData.usercity"
            :options="cityList"
            :props="regionCascaderProps"
            clearable
            placeholder="请选择地区"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>

      <span slot="footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="onSubmit">保存修改</el-button>
      </span>
    </el-dialog>

    <footer class="profile-footer">
      <FooterPlayer />
    </footer>
  </div>
</template>

<script>
import FooterPlayer from './components/FooterPlayer.vue'
import UserProfileHero from './profile/components/UserProfileHero.vue'
import UserProfileStats from './profile/components/UserProfileStats.vue'
import UserProfileProjectSection from './profile/sections/UserProfileProjectSection.vue'
import UserProfileKnowledgeSection from './profile/sections/UserProfileKnowledgeSection.vue'
import UserProfileActivitySection from './profile/sections/UserProfileActivitySection.vue'
import { GetAllRegions, GetAllTags, UpdateCurrentUser, DeleteBlog } from '@/api/index.js'
import { getMyProfileOverview, getUserPublicProfileOverview } from '@/api/userProfileOverview'

const DEFAULT_AVATAR = '/pic/choubi.jpg'
const DEFAULT_PROFILE = () => ({
  id: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  bio: '',
  gender: '',
  birthday: '',
  regionId: null,
  regionName: '',
  authorTagId: null,
  authorTagName: '',
  avatarUrl: DEFAULT_AVATAR
})
const DEFAULT_STATS = () => ({
  totalLikes: 0,
  totalCollects: 0,
  historyCount: 0,
  totalKnowledge: 0,
  totalRevenue: 0
})
const DEFAULT_ACTIVITY_SUMMARY = () => ({
  commits: 0,
  blogs: 0,
  posts: 0,
  likes: 0,
  collects: 0,
  logs: 0
})

export default {
  name: 'UserProfilePage',
  components: {
    FooterPlayer,
    UserProfileHero,
    UserProfileStats,
    UserProfileProjectSection,
    UserProfileKnowledgeSection,
    UserProfileActivitySection
  },
  props: {
    mode: {
      type: String,
      default: 'auto'
    }
  },
  data() {
    return {
      pageLoading: false,
      sectionLoading: false,
      submitLoading: false,
      pageError: '',

      profile: DEFAULT_PROFILE(),
      stats: DEFAULT_STATS(),
      activityData: [],
      activitySummary: DEFAULT_ACTIVITY_SUMMARY(),

      blogList: [],
      postList: [],
      knowledgeList: [],

      showProjectSection: true,
      activePostTab: 'blogs',

      dialogFormVisible: false,
      cityList: [],
      tagList: [],
      selectedAvatarUrl: '',

      formData: {
        nickname: '',
        userbrithday: '',
        userphone: '',
        usersex: '',
        usersign: '',
        authorTagId: [],
        usercity: []
      },

      avatarOptions: [
        { label: '默认', value: '/pic/choubi.jpg' },
        { label: '小熊', value: '/pic/bear.jpg' },
        { label: '鸭子', value: '/pic/duck.jpg' },
        { label: '兔子', value: '/pic/rabbit.jpg' },
        { label: '乌龟', value: '/pic/tortoise.jpg' }
      ],

      regionCascaderProps: {
        expandTrigger: 'hover',
        value: 'value',
        label: 'label',
        children: 'children',
        checkStrictly: true
      },
      tagCascaderProps: {
        expandTrigger: 'hover',
        value: 'value',
        label: 'label',
        children: 'children',
        checkStrictly: true
      }
    }
  },
  computed: {
    routeUserId() {
      const id = this.$route && this.$route.params ? this.$route.params.id : ''
      if (!id || id === ':id' || id === 'null' || id === 'undefined') return ''
      return String(id)
    },
    isSelfMode() {
      if (this.mode === 'self') return true
      if (this.mode === 'other') return false
      return !this.routeUserId
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler() {
        this.initializePage()
      }
    }
  },
  methods: {
    async initializePage() {
      this.pageError = ''
      this.pageLoading = true
      this.sectionLoading = true

      try {
        await this.loadProfileOverview()
        if (this.isSelfMode) {
          await Promise.all([this.loadCityList(), this.loadTagList()])
        }
      } catch (error) {
        console.error('加载用户主页失败:', error)
        this.pageError = '用户主页加载失败，请稍后重试'
      } finally {
        this.pageLoading = false
        this.sectionLoading = false
      }
    },

    async loadProfileOverview() {
      const overview = this.isSelfMode
        ? await getMyProfileOverview()
        : await getUserPublicProfileOverview(this.routeUserId)

      this.profile = {
        ...DEFAULT_PROFILE(),
        ...(overview.profile || {}),
        avatarUrl: overview.profile && overview.profile.avatarUrl ? overview.profile.avatarUrl : DEFAULT_AVATAR
      }
      this.stats = { ...DEFAULT_STATS(), ...(overview.stats || {}) }
      this.activityData = Array.isArray(overview.activity && overview.activity.activities)
        ? overview.activity.activities
        : []
      this.activitySummary = {
        ...DEFAULT_ACTIVITY_SUMMARY(),
        ...(overview.activity && overview.activity.summary ? overview.activity.summary : {})
      }

      this.blogList = Array.isArray(overview.blogList) ? overview.blogList : []
      this.postList = Array.isArray(overview.postList) ? overview.postList : []
      this.knowledgeList = Array.isArray(overview.knowledgeList) ? overview.knowledgeList : []

      this.showProjectSection = this.isSelfMode ? true : true
      this.syncFormDataFromProfile()
      this.selectedAvatarUrl = this.profile.avatarUrl || DEFAULT_AVATAR
    },

    syncFormDataFromProfile() {
      this.formData = {
        nickname: this.profile.nickname || '',
        userbrithday: this.toDateInput(this.profile.birthday),
        userphone: this.profile.phone || '',
        usersex: this.profile.gender || '',
        usersign: this.profile.bio || '',
        authorTagId: this.normalizeCascaderValue(this.profile.authorTagId),
        usercity: this.normalizeCascaderValue(this.profile.regionId)
      }
    },

    toDateInput(value) {
      if (!value) return ''
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return ''
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    normalizeCascaderValue(value) {
      if (Array.isArray(value)) return value
      return value ? [value] : []
    },

    async loadCityList() {
      try {
        const response = await GetAllRegions()
        const regions = Array.isArray(response) ? response : (response && response.data ? response.data : [])
        this.cityList = this.formatRegionData(regions)
        this.syncRegionName()
      } catch (error) {
        console.error('加载地区列表失败:', error)
      }
    },

    async loadTagList() {
      try {
        const response = await GetAllTags()
        const tags = Array.isArray(response) ? response : (response && response.data ? response.data : [])
        this.tagList = this.formatTagData(tags)
      } catch (error) {
        console.error('加载标签列表失败:', error)
      }
    },

    syncRegionName() {
      if (!this.profile.regionId) return
      const regionName = this.getRegionNameByCode(this.profile.regionId)
      if (regionName) {
        this.profile = { ...this.profile, regionName }
      }
    },

    formatRegionData(regions) {
      const regionMap = {}
      regions.forEach((region) => {
        regionMap[region.id] = {
          value: region.id,
          label: region.name,
          children: []
        }
      })

      const result = []
      regions.forEach((region) => {
        if (!region.parentId) {
          result.push(regionMap[region.id])
          return
        }
        if (regionMap[region.parentId]) {
          regionMap[region.parentId].children.push(regionMap[region.id])
        }
      })

      return this.filterEmptyChildren(result)
    },

    formatTagData(tags) {
      const tagMap = {}
      tags.forEach((tag) => {
        tagMap[tag.id] = {
          value: tag.id,
          label: tag.name,
          children: []
        }
      })

      const result = []
      tags.forEach((tag) => {
        const parentId = tag.parentId || tag.parent_id
        if (!parentId) {
          result.push(tagMap[tag.id])
          return
        }
        if (tagMap[parentId]) {
          tagMap[parentId].children.push(tagMap[tag.id])
        }
      })

      return this.filterEmptyChildren(result)
    },

    filterEmptyChildren(nodes) {
      return nodes.map((node) => {
        const copy = { ...node }
        if (copy.children && copy.children.length) {
          copy.children = this.filterEmptyChildren(copy.children)
        } else {
          delete copy.children
        }
        return copy
      })
    },

    getRegionNameByCode(code) {
      const stack = [...this.cityList]
      while (stack.length) {
        const node = stack.shift()
        if (String(node.value) === String(code)) return node.label
        if (node.children) stack.push(...node.children)
      }
      return ''
    },

    handleAvatarError() {
      this.profile = { ...this.profile, avatarUrl: DEFAULT_AVATAR }
    },

    openEditDialog() {
      if (!this.isSelfMode) return
      this.selectedAvatarUrl = this.profile.avatarUrl || DEFAULT_AVATAR
      this.syncFormDataFromProfile()
      this.dialogFormVisible = true
    },

    selectAvatar(url) {
      this.selectedAvatarUrl = url
    },

    handleDialogClose() {
      this.submitLoading = false
    },

    onSubmit() {
      if (!this.isSelfMode) return
      this.submitLoading = true

      if (!this.$refs.form) {
        this.submitLoading = false
        return
      }

      this.$refs.form.validate(async (valid) => {
        if (!valid) {
          this.submitLoading = false
          return
        }

        try {
          const payload = this.buildProfileUpdatePayload()
          await UpdateCurrentUser(payload)
          this.$message.success('资料更新成功')
          this.dialogFormVisible = false
          await this.loadProfileOverview()
        } catch (error) {
          console.error('更新用户资料失败:', error)
          this.$message.error('资料更新失败，请稍后重试')
        } finally {
          this.submitLoading = false
        }
      })
    },

    buildProfileUpdatePayload() {
      const regionId = this.pickLastValue(this.formData.usercity) || this.profile.regionId
      const authorTagId = this.pickLastValue(this.formData.authorTagId) || this.profile.authorTagId

      return {
        nickname: this.formData.nickname || '',
        phone: this.formData.userphone || '',
        gender: this.formData.usersex || 'other',
        bio: this.formData.usersign || '',
        regionId: regionId ? String(regionId) : null,
        avatarUrl: this.selectedAvatarUrl || this.profile.avatarUrl || DEFAULT_AVATAR,
        birthday: this.formData.userbrithday || null,
        authorTagId: authorTagId ? String(authorTagId) : null
      }
    },

    pickLastValue(value) {
      if (Array.isArray(value) && value.length) {
        return value[value.length - 1]
      }
      return value || null
    },

    handleCollectClick() {
      this.$router.push('/collection')
    },

    handleCouponsClick() {
      if (!this.isSelfMode) return
      this.$router.push('/coupons')
    },

    handleWalletClick() {
      if (!this.isSelfMode) return
      this.$router.push('/wallet')
    },

    handleToggleProjectSection() {
      if (!this.isSelfMode) return
      this.showProjectSection = !this.showProjectSection
      if (this.showProjectSection) {
        this.scrollToSection('projectSectionRef')
      }
    },

    handlePostTabChange(tab) {
      this.activePostTab = tab
    },

    scrollToKnowledgeSection() {
      this.scrollToSection('knowledgeSectionRef')
    },

    scrollToSection(refName) {
      this.$nextTick(() => {
        const target = this.$refs[refName]
        if (!target || !target.$el && !target.scrollIntoView) return
        const element = target.$el || target
        if (element && element.scrollIntoView) {
          element.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      })
    },

    openBlogDetail(id) {
      this.$router.push(`/blog/${id}`)
    },

    openPostDetail(id) {
      this.$router.push(`/circle/${id}`)
    },

    openKnowledgeDetail(id) {
      this.$router.push(`/blog/${id}`)
    },

    handleCreateKnowledge() {
      if (!this.isSelfMode) return
      this.$router.push('/blogwrite')
    },

    handleEditKnowledge(item) {
      if (!this.isSelfMode || !item || !item.id) return
      this.$router.push(`/blog/${item.id}`)
    },

    handleDeleteBlog(blog) {
      this.$confirm(`确认删除博客《${blog.title}》吗？此操作不可恢复。`, '提示', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await DeleteBlog(blog.id)
          this.$message.success('博客删除成功')
          await this.loadProfileOverview()
        } catch (error) {
          console.error('删除博客失败:', error)
          this.$message.error('删除博客失败，请稍后重试')
        }
      }).catch(() => {})
    },

    handleDeletePost(post) {
      this.$confirm(`确认移除帖子《${post.title}》吗？`, '提示', {
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.postList = this.postList.filter(item => item.id !== post.id)
        this.$message.success('帖子已从当前列表移除')
      }).catch(() => {})
    },

    handleDeleteKnowledge(item) {
      this.$confirm(`确认删除知识产品《${item.title}》吗？此操作不可恢复。`, '提示', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await DeleteBlog(item.id)
          this.$message.success('知识产品删除成功')
          await this.loadProfileOverview()
        } catch (error) {
          console.error('删除知识产品失败:', error)
          this.$message.error('删除知识产品失败，请稍后重试')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 12%, rgba(14, 165, 233, 0.15), transparent 36%),
    radial-gradient(circle at 88% 6%, rgba(59, 130, 246, 0.11), transparent 28%),
    #f3f7fc;
}

.profile-shell {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 20px 12px;
}

.page-error {
  margin-bottom: 16px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.left-column {
  position: sticky;
  top: 16px;
}

.main-column {
  min-width: 0;
}

.profile-footer {
  margin-top: 20px;
  padding-bottom: 10px;
}

.avatar-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.avatar-option {
  width: 84px;
  border: 1px solid #dde8f8;
  border-radius: 10px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #4b5563;
}

.avatar-option.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #111827;
}

@media (max-width: 1200px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .left-column {
    position: static;
  }
}
</style>
