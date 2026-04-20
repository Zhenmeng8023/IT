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
          <section class="overview-panel">
            <div class="overview-panel__head">
              <div>
                <h2 class="overview-title">{{ isSelfMode ? '个人中心' : `${profile.nickname || profile.username || '用户'} 的主页` }}</h2>
              </div>
              <div class="overview-head__meta">
                <span class="overview-meta-chip">{{ workspaceMetaText }}</span>
              </div>
            </div>

            <UserProfileStats
              :stats="stats"
              :is-self="isSelfMode"
              @collect="handleCollectClick"
              @toggle-project="handleToggleProjectSection"
              @knowledge="scrollToKnowledgeSection"
              @history="handleHistoryClick"
              @coupons="handleCouponsClick"
              @wallet="handleWalletClick"
            />

            <div class="highlight-grid">
              <article
                v-for="item in overviewHighlights"
                :key="item.label"
                class="highlight-card"
              >
                <span class="highlight-label">{{ item.label }}</span>
                <strong class="highlight-value">{{ item.value }}</strong>
              </article>
            </div>
          </section>

          <section ref="workspacePanelRef" class="workspace-panel">
            <div class="workspace-panel__head">
              <div>
                <h3 class="workspace-title">内容面板</h3>
              </div>
              <div class="workspace-summary">
                <span class="workspace-summary__item">博客 {{ blogList.length }}</span>
                <span class="workspace-summary__item">帖子 {{ postList.length }}</span>
                <span class="workspace-summary__item">知识产品 {{ knowledgeList.length }}</span>
              </div>
            </div>

            <el-tabs v-model="activeWorkspaceTab" class="workspace-tabs" stretch>
              <el-tab-pane label="发布内容" name="publish">
                <div class="workspace-scroll">
                  <UserProfileProjectSection
                    :visible="activeWorkspaceTab === 'publish'"
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
              </el-tab-pane>

              <el-tab-pane label="知识产品" name="knowledge">
                <div class="workspace-scroll">
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
              </el-tab-pane>

              <el-tab-pane label="活跃概览" name="activity">
                <div class="workspace-scroll">
                  <UserProfileActivitySection
                    :loading="sectionLoading"
                    :is-self="isSelfMode"
                    :display-name="profile.nickname || profile.username"
                    :activity-data="activityData"
                    :activity-summary="activitySummary"
                  />
                </div>
              </el-tab-pane>
            </el-tabs>
          </section>
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
          <div class="avatar-edit-panel">
            <div class="avatar-upload-card">
              <div class="avatar-live-preview">
                <img :src="avatarPreviewSource" :style="avatarImageStyle" alt="头像预览">
              </div>
              <div class="avatar-upload-copy">
                <strong>当前头像</strong>
                <span>支持 JPG、PNG、WEBP，最大 5MB</span>
              </div>
              <el-upload
                ref="avatarUpload"
                action="#"
                accept="image/jpeg,image/png,image/webp"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleAvatarFileChange"
              >
                <el-button size="small" type="primary" plain>
                  <i class="el-icon-upload2"></i>
                  选择本地图片
                </el-button>
              </el-upload>
            </div>

            <div class="avatar-position-panel">
              <div class="avatar-position-head">
                <span>头像位置</span>
                <em>拖动后会影响圆形裁切显示</em>
              </div>
              <div class="avatar-position-row">
                <span>左右</span>
                <el-slider v-model="avatarPositionX" :min="0" :max="100" :show-tooltip="false" />
              </div>
              <div class="avatar-position-row">
                <span>上下</span>
                <el-slider v-model="avatarPositionY" :min="0" :max="100" :show-tooltip="false" />
              </div>
            </div>

            <div class="avatar-selector-title">系统头像</div>
            <div class="avatar-selector">
              <div
                v-for="avatar in avatarOptions"
                :key="avatar.value"
                class="avatar-option"
                :class="{ active: !selectedAvatarFile && stripAvatarPosition(selectedAvatarUrl) === avatar.value }"
                @click="selectAvatar(avatar.value)"
              >
                <el-avatar :size="52" :src="avatar.value"></el-avatar>
                <span>{{ avatar.label }}</span>
              </div>
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
import { GetAllRegions, GetAllTags, UpdateCurrentUser, DeleteBlog, UploadUserAvatar } from '@/api/index.js'
import { getMyProfileOverview, getUserPublicProfileOverview } from '@/api/userProfileOverview'
import { useUserStore } from '@/store/user'

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
      activeWorkspaceTab: 'publish',

      dialogFormVisible: false,
      cityList: [],
      tagList: [],
      selectedAvatarUrl: '',
      selectedAvatarFile: null,
      avatarPreviewUrl: '',
      avatarUploading: false,
      avatarPositionX: 50,
      avatarPositionY: 50,

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
    },
    avatarPreviewSource() {
      return this.avatarPreviewUrl || this.selectedAvatarUrl || this.profile.avatarUrl || DEFAULT_AVATAR
    },
    avatarPositionStyle() {
      return {
        '--avatar-position': `${this.avatarPositionX}% ${this.avatarPositionY}%`
      }
    },
    avatarImageStyle() {
      return {
        objectFit: 'cover',
        objectPosition: `${this.avatarPositionX}% ${this.avatarPositionY}%`
      }
    },
    activeDaysCount() {
      return Array.isArray(this.activityData)
        ? this.activityData.filter(item => Number(item && item.count ? item.count : 0) > 0).length
        : 0
    },
    workspaceMetaText() {
      if (this.isSelfMode) {
        return `最近 30 天活跃 ${this.activeDaysCount} 天`
      }
      return `累计浏览 ${this.stats.historyCount || 0} 次`
    },
    overviewHighlights() {
      return [
        {
          label: '内容',
          value: `${(this.blogList.length || 0) + (this.postList.length || 0)}`
        },
        {
          label: '历史',
          value: `${this.stats.historyCount || 0}`
        },
        {
          label: '活跃',
          value: `${this.activeDaysCount}`
        }
      ]
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
  beforeDestroy() {
    this.revokeAvatarPreview()
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
      this.syncAvatarPositionFromUrl(this.selectedAvatarUrl)
      this.selectedAvatarFile = null
      this.revokeAvatarPreview()
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
      this.selectedAvatarFile = null
      this.revokeAvatarPreview()
      this.selectedAvatarUrl = this.profile.avatarUrl || DEFAULT_AVATAR
      this.syncAvatarPositionFromUrl(this.selectedAvatarUrl)
      this.syncFormDataFromProfile()
      this.dialogFormVisible = true
    },

    selectAvatar(url) {
      this.selectedAvatarFile = null
      this.revokeAvatarPreview()
      this.selectedAvatarUrl = url
      this.avatarPositionX = 50
      this.avatarPositionY = 50
    },

    handleAvatarFileChange(file) {
      const rawFile = file && file.raw ? file.raw : null
      if (!this.validateAvatarFile(rawFile)) {
        if (this.$refs.avatarUpload) {
          this.$refs.avatarUpload.clearFiles()
        }
        return false
      }

      this.revokeAvatarPreview()
      this.selectedAvatarFile = rawFile
      this.selectedAvatarUrl = ''
      this.avatarPositionX = 50
      this.avatarPositionY = 50
      this.avatarPreviewUrl = URL.createObjectURL(rawFile)
      return false
    },

    validateAvatarFile(file) {
      if (!file) {
        this.$message.error('请选择头像图片')
        return false
      }

      const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
      if (!allowedTypes.includes(file.type)) {
        this.$message.error('头像仅支持 JPG、PNG、WEBP 格式')
        return false
      }

      const maxSize = 5 * 1024 * 1024
      if (file.size > maxSize) {
        this.$message.error('头像大小不能超过 5MB')
        return false
      }

      return true
    },

    revokeAvatarPreview() {
      if (
        this.avatarPreviewUrl &&
        this.avatarPreviewUrl.startsWith('blob:') &&
        typeof URL !== 'undefined' &&
        URL.revokeObjectURL
      ) {
        URL.revokeObjectURL(this.avatarPreviewUrl)
      }
      this.avatarPreviewUrl = ''
    },

    async resolveAvatarUrlForSubmit() {
      if (!this.selectedAvatarFile) {
        return this.attachAvatarPosition(this.selectedAvatarUrl || this.profile.avatarUrl || DEFAULT_AVATAR)
      }

      this.avatarUploading = true
      try {
        const formData = new FormData()
        formData.append('file', this.selectedAvatarFile)
        const response = await UploadUserAvatar(formData)
        const payload = response && response.data ? response.data : response
        const avatarUrl = payload && (payload.avatarUrl || payload.url || (payload.data && payload.data.avatarUrl))
        if (!avatarUrl) {
          throw new Error('头像上传结果缺少地址')
        }
        return this.attachAvatarPosition(avatarUrl)
      } finally {
        this.avatarUploading = false
      }
    },

    stripAvatarPosition(url) {
      if (!url) return ''
      return String(url).split('#')[0]
    },

    attachAvatarPosition(url) {
      const baseUrl = this.stripAvatarPosition(url)
      if (!baseUrl) return ''
      const x = this.clampAvatarPosition(this.avatarPositionX)
      const y = this.clampAvatarPosition(this.avatarPositionY)
      if (x === 50 && y === 50) {
        return baseUrl
      }
      return `${baseUrl}#avatar-position=${x},${y}`
    },

    parseAvatarPosition(url) {
      const hash = String(url || '').split('#')[1] || ''
      const match = hash.match(/avatar-position=(\d+(?:\.\d+)?),(\d+(?:\.\d+)?)/)
      if (!match) {
        return { x: 50, y: 50 }
      }
      return {
        x: this.clampAvatarPosition(match[1]),
        y: this.clampAvatarPosition(match[2])
      }
    },

    clampAvatarPosition(value) {
      const numberValue = Number(value)
      if (!Number.isFinite(numberValue)) return 50
      return Math.max(0, Math.min(100, Math.round(numberValue)))
    },

    syncAvatarPositionFromUrl(url) {
      const position = this.parseAvatarPosition(url)
      this.avatarPositionX = position.x
      this.avatarPositionY = position.y
    },

    handleDialogClose() {
      this.submitLoading = false
      this.avatarUploading = false
      this.selectedAvatarFile = null
      this.revokeAvatarPreview()
      if (this.$refs.avatarUpload) {
        this.$refs.avatarUpload.clearFiles()
      }
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
          const avatarUrl = await this.resolveAvatarUrlForSubmit()
          const payload = this.buildProfileUpdatePayload(avatarUrl)
          const updateResponse = await UpdateCurrentUser(payload)
          this.syncUserStore(updateResponse)
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

    buildProfileUpdatePayload(avatarUrl) {
      const regionId = this.pickLastValue(this.formData.usercity) || this.profile.regionId
      const authorTagId = this.pickLastValue(this.formData.authorTagId) || this.profile.authorTagId

      return {
        nickname: this.formData.nickname || '',
        phone: this.formData.userphone || '',
        gender: this.formData.usersex || 'other',
        bio: this.formData.usersign || '',
        regionId: regionId ? String(regionId) : null,
        avatarUrl: avatarUrl || this.profile.avatarUrl || DEFAULT_AVATAR,
        birthday: this.formData.userbrithday || null,
        authorTagId: authorTagId ? String(authorTagId) : null
      }
    },

    syncUserStore(response) {
      const payload = response && response.data ? response.data : response
      if (payload && typeof payload === 'object') {
        const userStore = useUserStore()
        userStore.setUserInfo(payload)
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

    handleHistoryClick() {
      if (!this.isSelfMode) return
      this.$router.push('/history')
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
      this.activeWorkspaceTab = 'publish'
      this.scrollToSection('workspacePanelRef')
    },

    handlePostTabChange(tab) {
      this.activePostTab = tab
    },

    scrollToKnowledgeSection() {
      this.activeWorkspaceTab = 'knowledge'
      this.scrollToSection('workspacePanelRef')
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
  background: var(--it-page-bg);
  color: var(--it-text);
}

.profile-shell {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 20px 18px;
}

.page-error {
  margin-bottom: 16px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.left-column {
  position: sticky;
  top: 16px;
}

.main-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-panel,
.workspace-panel {
  border-radius: 10px;
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  box-shadow: var(--it-shadow);
}

.overview-panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-panel__head,
.workspace-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.overview-title,
.workspace-title {
  margin: 0;
  color: var(--it-text);
}

.overview-title {
  font-size: 22px;
  line-height: 1.2;
}

.workspace-title {
  font-size: 18px;
}

.overview-head__meta {
  display: flex;
  align-items: center;
}

.overview-meta-chip,
.workspace-summary__item {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.highlight-card {
  min-width: 0;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
}

.highlight-label {
  display: block;
  color: var(--it-text-subtle);
  font-size: 12px;
}

.highlight-value {
  display: block;
  margin-top: 6px;
  color: var(--it-text);
  font-size: 20px;
  line-height: 1;
}

.workspace-panel {
  padding: 14px;
}

.workspace-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.workspace-tabs {
  margin-top: 10px;
}

.workspace-scroll {
  max-height: calc(100vh - 300px);
  overflow: auto;
  padding-right: 6px;
}

.workspace-scroll::-webkit-scrollbar {
  width: 8px;
}

.workspace-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: color-mix(in srgb, var(--it-accent) 24%, transparent);
}

.workspace-scroll :deep(.project-section),
.workspace-scroll :deep(.knowledge-section),
.workspace-scroll :deep(.activity-section) {
  margin-top: 0;
}

.profile-footer {
  margin-top: 20px;
  padding-bottom: 10px;
}

.edit-dialog ::v-deep .el-dialog {
  border-radius: 8px;
  overflow: hidden;
}

.edit-dialog ::v-deep .el-dialog__header {
  padding: 18px 22px;
  border-bottom: 1px solid var(--it-border);
}

.edit-dialog ::v-deep .el-dialog__body {
  padding: 20px 24px 12px;
}

.edit-dialog ::v-deep .el-dialog__footer {
  padding: 14px 24px 20px;
}

.avatar-edit-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.avatar-upload-card {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  background: var(--it-surface-muted);
}

.avatar-live-preview {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  overflow: hidden;
  flex: 0 0 76px;
  border: 3px solid var(--it-surface-solid);
  box-shadow: var(--it-shadow);
}

.avatar-live-preview img {
  display: block;
  width: 100%;
  height: 100%;
}

.avatar-upload-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.avatar-upload-copy strong {
  color: var(--it-text);
  font-size: 14px;
}

.avatar-upload-copy span,
.avatar-selector-title {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.avatar-selector-title {
  font-weight: 600;
}

.avatar-position-panel {
  padding: 12px 14px;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  background: var(--it-surface-solid);
}

.avatar-position-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  color: var(--it-text);
  font-size: 13px;
  font-weight: 600;
}

.avatar-position-head em {
  color: var(--it-text-subtle);
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.avatar-position-row {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: var(--it-text-muted);
  font-size: 12px;
}

.avatar-position-row ::v-deep .el-slider__runway {
  margin: 11px 0;
}

.avatar-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.avatar-option {
  width: 78px;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--it-text-muted);
  background: var(--it-surface-solid);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.avatar-option:hover {
  transform: translateY(-1px);
  border-color: var(--it-border-strong);
  box-shadow: var(--it-shadow);
}

.avatar-option.active {
  border-color: var(--it-accent);
  background: var(--it-accent-soft);
  color: var(--it-text);
}

.avatar-option span {
  font-size: 12px;
}

@media (max-width: 1200px) {
  .highlight-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workspace-scroll {
    max-height: calc(100vh - 280px);
  }

  .profile-layout {
    grid-template-columns: 1fr;
  }

  .left-column {
    position: static;
  }
}

@media (max-width: 640px) {
  .profile-shell {
    padding: 16px 12px;
  }

  .overview-panel,
  .workspace-panel {
    padding: 16px;
  }

  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .workspace-scroll {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }

  .avatar-upload-card {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .avatar-upload-card .el-upload {
    grid-column: 1 / -1;
  }
}
</style>
