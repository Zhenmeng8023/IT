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
            :is-self="isSelfMode"
            @edit="openEditDialog"
            @avatar-error="handleAvatarError"
          />
        </aside>

        <main class="main-column">
          <section class="command-panel">
            <div class="command-copy">
              <div>
                <span class="command-kicker">{{ isSelfMode ? 'Workspace' : 'Profile' }}</span>
                <h2 class="command-title">{{ isSelfMode ? '个人中心' : `${currentUserDisplayName} 的主页` }}</h2>
                <p class="command-subtitle">{{ dashboardSubtitle }}</p>
              </div>
            </div>
            <div class="summary-chips">
              <span v-for="item in summaryChips" :key="item.label" class="summary-chip">
                {{ item.label }} {{ item.value }}
              </span>
            </div>
          </section>

          <section class="workspace-shell">
            <div class="panel-head">
              <div>
                <h3 class="panel-title">管理工作台</h3>
                <p class="panel-subtitle">{{ workspaceShellSubtitle }}</p>
              </div>
            </div>

            <el-tabs v-model="activeCenterTab" class="center-tabs">
              <el-tab-pane label="概览" name="overview">
                <div class="tab-scroller">
                  <div class="tab-section-head">
                    <div>
                      <h4 class="tab-section-title">工作概览</h4>
                      <p class="tab-section-subtitle">{{ dashboardSubtitle }}</p>
                    </div>
                  </div>

                  <div class="overview-metrics">
                    <article v-for="item in overviewMetrics" :key="item.label" class="overview-metric-card">
                      <span class="overview-metric-card__label">{{ item.label }}</span>
                      <strong class="overview-metric-card__value">{{ item.value }}</strong>
                      <span class="overview-metric-card__note">{{ item.note }}</span>
                    </article>
                  </div>

                  <div class="quick-entry-grid">
                    <button
                      v-for="item in quickEntryCards"
                      :key="item.key"
                      type="button"
                      class="quick-entry-card"
                      @click="handleQuickEntry(item.key)"
                    >
                      <span class="quick-entry-card__label">{{ item.label }}</span>
                      <strong class="quick-entry-card__value">{{ item.value }}</strong>
                      <span class="quick-entry-card__note">{{ item.note }}</span>
                    </button>
                  </div>

                  <div class="overview-dual-grid">
                    <section class="overview-panel">
                      <div class="overview-panel__head">
                        <h5>最近内容</h5>
                        <el-button type="text" @click="activeCenterTab = 'content'">查看全部</el-button>
                      </div>
                      <div v-if="assetItems.length" class="overview-list">
                        <button
                          v-for="item in assetItems.slice(0, 4)"
                          :key="'overview-asset-' + item.key"
                          type="button"
                          class="overview-list__item"
                          @click="openAssetDetail(item)"
                        >
                          <span>{{ item.typeLabel }}</span>
                          <strong>{{ item.title }}</strong>
                          <em>{{ item.dateText }}</em>
                        </button>
                      </div>
                      <div v-else class="panel-empty panel-empty--compact">
                        <p>{{ isSelfMode ? '还没有内容资产，先写一篇文章。' : '该用户暂时没有公开内容。' }}</p>
                      </div>
                    </section>

                    <section class="overview-panel">
                      <div class="overview-panel__head">
                        <h5>最近项目</h5>
                        <el-button type="text" @click="activeCenterTab = 'project'">查看全部</el-button>
                      </div>
                      <div v-if="projectItems.length" class="overview-list">
                        <button
                          v-for="item in projectItems.slice(0, 4)"
                          :key="'overview-project-' + item.id"
                          type="button"
                          class="overview-list__item"
                          @click="openProjectDetail(item.id)"
                        >
                          <span>{{ formatProjectRole(item) }}</span>
                          <strong>{{ item.name }}</strong>
                          <em>{{ formatDate(item.updatedAt || item.createdAt) }}</em>
                        </button>
                      </div>
                      <div v-else class="panel-empty panel-empty--compact">
                        <p>{{ projectEmptyText }}</p>
                      </div>
                    </section>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="内容" name="content">
                <div class="tab-scroller">
                  <div class="tab-section-head">
                    <div>
                      <h4 class="tab-section-title">内容资产</h4>
                      <p class="tab-section-subtitle">{{ contentTabSubtitle }}</p>
                    </div>
                    <div v-if="isSelfMode" class="tab-section-actions">
                      <el-button size="small" plain @click="handleCreateKnowledge">新建产品</el-button>
                      <el-button size="small" type="primary" @click="handleCreateContent">写文章</el-button>
                    </div>
                  </div>

                  <div class="asset-filters">
                    <button
                      v-for="item in assetFilterOptions"
                      :key="item.key"
                      type="button"
                      class="asset-filter"
                      :class="{ active: activeAssetFilter === item.key }"
                      @click="activeAssetFilter = item.key"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ item.count }}</strong>
                    </button>
                  </div>

                  <div v-loading="sectionLoading" class="asset-panel__body">
                    <article
                      v-for="item in filteredAssetItems"
                      :key="item.key"
                      class="asset-card"
                      @click="openAssetDetail(item)"
                    >
                      <div class="asset-card__head">
                        <div class="asset-badges">
                          <span class="asset-badge" :class="`asset-badge--${item.type}`">{{ item.typeLabel }}</span>
                          <span v-if="item.statusText" class="asset-badge asset-badge--status">{{ item.statusText }}</span>
                        </div>
                        <span class="asset-date">{{ item.dateText }}</span>
                      </div>

                      <div class="asset-card__body">
                        <h4>{{ item.title }}</h4>
                        <p>{{ item.summary }}</p>
                      </div>

                      <div class="asset-meta">
                        <span v-for="meta in item.meta" :key="meta" class="asset-meta__item">{{ meta }}</span>
                      </div>

                      <div v-if="isSelfMode" class="asset-card__actions">
                        <el-button
                          v-if="item.type === 'knowledge'"
                          size="mini"
                          plain
                          @click.stop="handleEditAsset(item)"
                        >
                          编辑
                        </el-button>
                        <el-button
                          size="mini"
                          type="danger"
                          plain
                          @click.stop="handleDeleteAsset(item)"
                        >
                          删除
                        </el-button>
                      </div>
                    </article>

                    <div v-if="!filteredAssetItems.length" class="panel-empty">
                      <i class="el-icon-files"></i>
                      <p>{{ assetEmptyText }}</p>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="项目" name="project">
                <div class="tab-scroller">
                  <div class="tab-section-head">
                    <div>
                      <h4 class="tab-section-title">项目资产</h4>
                      <p class="tab-section-subtitle">{{ projectTabSubtitle }}</p>
                    </div>
                    <div v-if="isSelfMode" class="tab-section-actions">
                      <el-button size="small" plain @click="handleProjectAction('mine')">我的项目</el-button>
                      <el-button size="small" plain @click="handleProjectAction('collection')">项目收藏</el-button>
                      <el-button size="small" type="primary" @click="handleProjectAction('create')">新建项目</el-button>
                    </div>
                  </div>

                  <div class="project-stat-grid">
                    <article v-for="item in projectMetricCards" :key="item.label" class="project-stat-card">
                      <span class="project-stat-card__label">{{ item.label }}</span>
                      <strong class="project-stat-card__value">{{ item.value }}</strong>
                    </article>
                  </div>

                  <div v-loading="projectLoading" class="asset-panel__body">
                    <article
                      v-for="item in projectItems"
                      :key="'project-' + item.id"
                      class="asset-card project-asset-card"
                      @click="openProjectDetail(item.id)"
                    >
                      <div class="asset-card__head">
                        <div class="asset-badges">
                          <span class="asset-badge asset-badge--project">{{ formatProjectRole(item) }}</span>
                          <span class="asset-badge asset-badge--status">{{ formatProjectVisibility(item.visibility) }}</span>
                        </div>
                        <span class="asset-date">{{ formatDate(item.updatedAt || item.createdAt) }}</span>
                      </div>

                      <div class="asset-card__body">
                        <h4>{{ item.name }}</h4>
                        <p>{{ item.description || '暂无项目描述' }}</p>
                      </div>

                      <div class="asset-meta">
                        <span class="asset-meta__item">收藏 {{ item.stars || 0 }}</span>
                        <span class="asset-meta__item">下载 {{ item.downloads || 0 }}</span>
                        <span class="asset-meta__item">浏览 {{ item.views || 0 }}</span>
                      </div>

                      <div v-if="isSelfMode" class="asset-card__actions">
                        <el-button size="mini" plain @click.stop="openProjectDetail(item.id)">详情</el-button>
                        <el-button
                          v-if="item.isOwner"
                          size="mini"
                          type="primary"
                          plain
                          @click.stop="openProjectWorkbench(item.id)"
                        >
                          工作台
                        </el-button>
                      </div>
                    </article>

                    <div v-if="!projectItems.length" class="panel-empty">
                      <i class="el-icon-s-management"></i>
                      <p>{{ projectEmptyText }}</p>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="互动" name="activity">
                <div class="tab-scroller">
                  <div class="tab-section-head">
                    <div>
                      <h4 class="tab-section-title">互动与活跃</h4>
                      <p class="tab-section-subtitle">{{ activityTabSubtitle }}</p>
                    </div>
                  </div>

                  <div v-if="isSelfMode" class="shortcut-grid">
                    <button
                      v-for="item in activityShortcutCards"
                      :key="item.key"
                      type="button"
                      class="shortcut-card"
                      @click="handleActivityShortcut(item.key)"
                    >
                      <span class="shortcut-card__label">{{ item.label }}</span>
                      <strong class="shortcut-card__value">{{ item.value }}</strong>
                      <span class="shortcut-card__note">{{ item.note }}</span>
                    </button>
                  </div>

                  <div class="activity-metrics">
                    <article v-for="item in activityCards" :key="item.label" class="activity-metric">
                      <span class="activity-metric__label">{{ item.label }}</span>
                      <strong class="activity-metric__value">{{ item.value }}</strong>
                    </article>
                  </div>

                  <div class="activity-layout">
                    <div class="heatmap-panel">
                      <HeatmapTracker :activity-data="activityData" :loading="sectionLoading" :days="30" />
                    </div>

                    <div class="activity-feed-panel">
                      <div class="activity-feed__head">
                        <h4>最近记录</h4>
                        <span>{{ activityPanelSubtitle }}</span>
                      </div>

                      <div class="activity-feed">
                        <div v-for="item in recentActivityRows" :key="item.date" class="activity-row">
                          <div>
                            <span class="activity-row__date">{{ item.date }}</span>
                            <p class="activity-row__detail">{{ item.detail }}</p>
                          </div>
                          <strong class="activity-row__count">{{ item.count }}</strong>
                        </div>

                        <div v-if="!recentActivityRows.length" class="activity-empty">
                          最近 30 天还没有明显活跃记录
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane v-if="isSelfMode" label="个人知识库" name="knowledge-base">
                <div class="tab-scroller">
                  <FrontPersonalKnowledgeBaseCenter class="profile-kb-embed" />
                </div>
              </el-tab-pane>

              <el-tab-pane v-if="isSelfMode" label="服务" name="service">
                <div class="tab-scroller">
                  <div class="tab-section-head">
                    <div>
                      <h4 class="tab-section-title">账户服务</h4>
                      <p class="tab-section-subtitle">{{ serviceTabSubtitle }}</p>
                    </div>
                  </div>

                  <div class="account-grid">
                    <button
                      v-for="item in accountCards"
                      :key="item.key"
                      type="button"
                      class="account-card"
                      @click="handleAccountAction(item.key)"
                    >
                      <span class="account-card__label">{{ item.label }}</span>
                      <strong class="account-card__value">{{ item.value }}</strong>
                      <span class="account-card__note">{{ item.note }}</span>
                    </button>
                  </div>

                  <div class="service-tip-grid">
                    <article v-for="item in serviceTips" :key="item.title" class="service-tip-card">
                      <h5>{{ item.title }}</h5>
                      <p>{{ item.desc }}</p>
                    </article>
                  </div>
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
      width="860px"
      top="6vh"
      class="edit-dialog"
      :close-on-click-modal="false"
      :append-to-body="true"
      @close="handleDialogClose"
    >
      <el-form ref="form" :model="formData" label-position="top" class="edit-form">
        <div class="edit-dialog-layout">
          <section class="edit-avatar-column">
            <div class="avatar-edit-panel">
              <div class="avatar-upload-card">
                <div class="avatar-live-preview">
                  <img :src="avatarPreviewSource" :style="avatarImageStyle" alt="头像预览">
                </div>
                <div class="avatar-upload-copy">
                  <strong>头像设置</strong>
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
                    选择图片
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
                  <el-avatar :size="48" :src="avatar.value"></el-avatar>
                  <span>{{ avatar.label }}</span>
                </div>
              </div>
            </div>
          </section>

          <section class="edit-fields-column">
            <div class="edit-form-grid">
              <el-form-item label="昵称" class="field field--span-2">
                <el-input v-model="formData.nickname" clearable placeholder="请输入昵称"></el-input>
              </el-form-item>

              <el-form-item label="电话" class="field">
                <el-input v-model="formData.userphone" clearable placeholder="请输入电话"></el-input>
              </el-form-item>

              <el-form-item label="性别" class="field">
                <el-radio-group v-model="formData.usersex" class="radio-row">
                  <el-radio label="male">男</el-radio>
                  <el-radio label="female">女</el-radio>
                  <el-radio label="other">其他</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="生日" class="field">
                <el-date-picker
                  v-model="formData.userbrithday"
                  type="date"
                  value-format="yyyy-MM-dd"
                  format="yyyy-MM-dd"
                  placeholder="请选择生日"
                  style="width: 100%;"
                />
              </el-form-item>

              <el-form-item label="标签" class="field">
                <el-cascader
                  v-model="formData.authorTagId"
                  :options="tagList"
                  :props="tagCascaderProps"
                  clearable
                  placeholder="请选择标签"
                  style="width: 100%;"
                />
              </el-form-item>

              <el-form-item label="地区" class="field field--span-2">
                <el-cascader
                  v-model="formData.usercity"
                  :options="cityList"
                  :props="regionCascaderProps"
                  clearable
                  placeholder="请选择地区"
                  style="width: 100%;"
                />
              </el-form-item>

              <el-form-item label="签名" class="field field--span-2">
                <el-input
                  v-model="formData.usersign"
                  type="textarea"
                  :rows="2"
                  placeholder="留下一句个人签名"
                  clearable
                />
              </el-form-item>
            </div>
          </section>
        </div>
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
import HeatmapTracker from './components/HeatmapTracker.vue'
import UserProfileHero from './profile/components/UserProfileHero.vue'
import FrontPersonalKnowledgeBaseCenter from '@/pages/front_ai/personal/KnowledgeBaseCenter.vue'
import { GetAllRegions, GetAllTags, UpdateCurrentUser, DeleteBlog, DeleteCircleComment, UploadUserAvatar } from '@/api/index.js'
import { getMyProfileOverview, getUserPublicProfileOverview } from '@/api/userProfileOverview'
import { getMyProjects, getMyStarredProjects, getParticipatedProjects, pageProjects } from '@/api/project'
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
    HeatmapTracker,
    UserProfileHero,
    FrontPersonalKnowledgeBaseCenter
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
      ownedProjects: [],
      starredProjects: [],
      participatedProjects: [],
      publicProjects: [],
      projectLoading: false,

      activeCenterTab: 'overview',
      activeAssetFilter: 'all',

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
    currentUserDisplayName() {
      return this.profile.nickname || this.profile.username || '用户'
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
    totalRevenueText() {
      if (this.stats.totalRevenue === null || this.stats.totalRevenue === undefined) return '--'
      return `¥${Number(this.stats.totalRevenue || 0).toFixed(2)}`
    },
    dashboardSubtitle() {
      if (this.isSelfMode) {
        return `围绕概览、内容、项目、互动和服务五类能力组织，最近 30 天活跃 ${this.activeDaysCount} 天。`
      }
      return '围绕公开资料、内容、项目和活跃状态组织这位用户的主页信息。'
    },
    summaryChips() {
      return [
        {
          label: '内容',
          value: this.assetItems.length
        },
        {
          label: '项目',
          value: this.projectItems.length
        },
        {
          label: '活跃',
          value: this.activeDaysCount
        },
        {
          label: this.isSelfMode ? '收益' : '获赞',
          value: this.isSelfMode ? this.totalRevenueText : `${this.stats.totalLikes || 0}`
        }
      ]
    },
    workspaceShellSubtitle() {
      if (this.isSelfMode) {
        return '按概览、内容、项目、互动和服务五个工作面统一管理。'
      }
      return '按概览、内容、项目、互动四类信息查看这位用户的公开状态。'
    },
    overviewMetrics() {
      return [
        {
          label: '内容资产',
          value: `${this.assetItems.length}`,
          note: '博客 + 帖子 + 知识产品'
        },
        {
          label: '项目资产',
          value: `${this.projectItems.length}`,
          note: this.isSelfMode ? '我的项目与协作项目' : '该用户公开项目'
        },
        {
          label: this.isSelfMode ? '累计收益' : '累计获赞',
          value: this.isSelfMode ? this.totalRevenueText : `${this.stats.totalLikes || 0}`,
          note: this.isSelfMode ? '来自内容变现' : '来自公开互动'
        },
        {
          label: '活跃天数',
          value: `${this.activeDaysCount}`,
          note: '最近 30 天'
        }
      ]
    },
    quickEntryCards() {
      if (this.isSelfMode) {
        return [
          { key: 'write', label: '写文章', value: '创作', note: '发布新内容' },
          { key: 'knowledge', label: '新建产品', value: '变现', note: '创建知识产品' },
          { key: 'project_mine', label: '我的项目', value: `${this.ownedProjects.length}`, note: '进入项目中心' },
          { key: 'wallet', label: '钱包', value: this.totalRevenueText === '--' ? '账户' : this.totalRevenueText, note: '资金与结算' },
          { key: 'orders', label: '订单', value: '记录', note: '查看购买与支付' },
          { key: 'history', label: '历史', value: `${this.stats.historyCount || 0}`, note: '回看浏览记录' }
        ]
      }

      return [
        { key: 'public_blog', label: '公开博客', value: `${this.blogList.length}`, note: '查看内容' },
        { key: 'public_post', label: '公开帖子', value: `${this.postList.length}`, note: '查看讨论' },
        { key: 'public_project', label: '公开项目', value: `${this.projectItems.length}`, note: '查看项目' },
        { key: 'public_activity', label: '活跃天数', value: `${this.activeDaysCount}`, note: '最近 30 天' }
      ]
    },
    projectItems() {
      const source = this.isSelfMode
        ? [...this.ownedProjects, ...this.participatedProjects, ...this.starredProjects]
        : [...this.publicProjects]
      const unique = new Map()
      source.forEach((item) => {
        if (!item || !item.id) return
        const key = Number(item.id)
        if (!unique.has(key)) {
          unique.set(key, item)
          return
        }
        const current = unique.get(key)
        const currentStamp = this.toTimestamp(current.updatedAt || current.createdAt)
        const nextStamp = this.toTimestamp(item.updatedAt || item.createdAt)
        if (nextStamp > currentStamp) {
          unique.set(key, item)
        }
      })
      return [...unique.values()].sort((a, b) => {
        return this.toTimestamp(b.updatedAt || b.createdAt) - this.toTimestamp(a.updatedAt || a.createdAt)
      })
    },
    projectMetricCards() {
      if (this.isSelfMode) {
        return [
          { label: '我创建的项目', value: `${this.ownedProjects.length}` },
          { label: '我参与的项目', value: `${this.participatedProjects.length}` },
          { label: '我收藏的项目', value: `${this.starredProjects.length}` }
        ]
      }
      return [
        { label: '公开项目', value: `${this.publicProjects.length}` },
        { label: '公开内容', value: `${this.assetItems.length}` },
        { label: '活跃天数', value: `${this.activeDaysCount}` }
      ]
    },
    projectTabSubtitle() {
      if (this.isSelfMode) {
        return '统一查看创建、参与和收藏的项目资产，并快速进入工作台。'
      }
      return '查看这位用户公开展示的项目资产。'
    },
    projectEmptyText() {
      if (this.isSelfMode) {
        return '暂时还没有项目，去项目中心创建第一个项目吧。'
      }
      return '该用户暂时没有公开项目。'
    },
    contentTabSubtitle() {
      if (this.isSelfMode) {
        return '博客、帖子和知识产品统一在这里创作、查看和维护。'
      }
      return '公开内容按统一时间线展示。'
    },
    activityTabSubtitle() {
      if (this.isSelfMode) {
        return '查看收藏、浏览与最近活跃状态。'
      }
      return '通过热力图和记录快速了解这位用户最近的活跃节奏。'
    },
    serviceTabSubtitle() {
      return '钱包、订单、优惠券、会员与项目入口集中在这里。'
    },
    assetItems() {
      const blogs = this.blogList.map(item => ({
        key: `blog-${item.id}`,
        id: item.id,
        type: 'blog',
        typeLabel: '博客',
        statusText: '公开内容',
        title: item.title || '未命名博客',
        summary: this.extractSummary(item),
        dateText: this.formatDate(item.createTime),
        timestamp: this.toTimestamp(item.createTime),
        meta: [`浏览 ${item.viewCount || 0}`, `点赞 ${item.likeCount || 0}`],
        raw: item
      }))

      const posts = this.postList.map(item => ({
        key: `post-${item.id}`,
        id: item.id,
        type: 'post',
        typeLabel: '帖子',
        statusText: '社区内容',
        title: item.title || '未命名帖子',
        summary: this.extractSummary(item),
        dateText: this.formatDate(item.createTime),
        timestamp: this.toTimestamp(item.createTime),
        meta: [`评论 ${item.commentCount || 0}`, `点赞 ${item.likeCount || 0}`],
        raw: item
      }))

      const knowledge = this.knowledgeList.map(item => ({
        key: `knowledge-${item.id}`,
        id: item.id,
        type: 'knowledge',
        typeLabel: '产品',
        statusText: item.status === 'published' ? '已发布' : '草稿',
        title: item.title || '未命名知识产品',
        summary: this.extractSummary(item),
        dateText: this.formatDate(item.createTime),
        timestamp: this.toTimestamp(item.createTime),
        meta: [`价格 ¥${Number(item.price || 0).toFixed(2)}`, `浏览 ${item.viewCount || 0}`],
        raw: item
      }))

      return [...blogs, ...posts, ...knowledge].sort((a, b) => b.timestamp - a.timestamp)
    },
    assetFilterOptions() {
      return [
        {
          key: 'all',
          label: '全部',
          count: this.assetItems.length
        },
        {
          key: 'blog',
          label: '博客',
          count: this.blogList.length
        },
        {
          key: 'post',
          label: '帖子',
          count: this.postList.length
        },
        {
          key: 'knowledge',
          label: '产品',
          count: this.knowledgeList.length
        }
      ]
    },
    filteredAssetItems() {
      if (this.activeAssetFilter === 'all') return this.assetItems
      return this.assetItems.filter(item => item.type === this.activeAssetFilter)
    },
    assetEmptyText() {
      if (this.activeAssetFilter === 'all') {
        return this.isSelfMode ? '还没有内容资产，先写一篇文章或创建一个产品吧。' : '这个用户暂时还没有公开内容。'
      }
      const current = this.assetFilterOptions.find(item => item.key === this.activeAssetFilter)
      return `${current ? current.label : '当前分类'}暂时为空`
    },
    activityShortcutCards() {
      if (!this.isSelfMode) return []
      return [
        {
          key: 'collection',
          label: '我的收藏',
          value: `${this.stats.totalCollects || 0}`,
          note: '回看已收藏内容'
        },
        {
          key: 'history',
          label: '浏览历史',
          value: `${this.stats.historyCount || 0}`,
          note: '继续上次阅读'
        }
      ]
    },
    accountCards() {
      return [
        {
          key: 'wallet',
          label: '钱包',
          value: this.totalRevenueText === '--' ? '账户' : this.totalRevenueText,
          note: '查看余额与收益'
        },
        {
          key: 'orders',
          label: '订单',
          value: '交易',
          note: '查看订单与购买记录'
        },
        {
          key: 'coupons',
          label: '优惠券',
          value: '权益',
          note: '管理可用权益'
        },
        {
          key: 'vip',
          label: '会员',
          value: '订阅',
          note: '查看会员服务'
        },
        {
          key: 'project',
          label: '项目中心',
          value: `${this.ownedProjects.length}`,
          note: '管理我的项目'
        }
      ]
    },
    serviceTips() {
      return [
        { title: '订单与购买', desc: '在订单页统一查看支付流水、购买记录和售后状态。' },
        { title: '优惠券与会员', desc: '在权益模块查看可用优惠券与会员有效期。' },
        { title: '项目资产', desc: '项目中心用于管理创建项目、协作项目和项目收藏。' }
      ]
    },
    activityPanelSubtitle() {
      return this.isSelfMode ? '查看最近 30 天的活跃趋势和节奏。' : '用最近的活跃记录快速判断状态。'
    },
    activityCards() {
      if (this.isSelfMode) {
        return [
          { label: '活跃天数', value: `${this.activeDaysCount}` },
          { label: '提交', value: `${this.activitySummary.commits || 0}` },
          { label: '点赞', value: `${this.activitySummary.likes || 0}` },
          { label: '日志', value: `${this.activitySummary.logs || 0}` }
        ]
      }

      return [
        { label: '活跃天数', value: `${this.activeDaysCount}` },
        { label: '博客', value: `${this.activitySummary.blogs || 0}` },
        { label: '帖子', value: `${this.activitySummary.posts || 0}` },
        { label: '点赞', value: `${this.activitySummary.likes || 0}` }
      ]
    },
    recentActivityRows() {
      return [...this.activityData]
        .filter(item => Number(item && item.count ? item.count : 0) > 0)
        .sort((a, b) => this.toTimestamp(b.date) - this.toTimestamp(a.date))
        .slice(0, 5)
        .map(item => ({
          date: item.date,
          count: item.count,
          detail: this.formatActivityBreakdown(item.breakdown)
        }))
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler() {
        this.initializePage()
      }
    },
    '$route.query.tab'(value) {
      if (!this.isSelfMode) return
      const nextTab = this.normalizeCenterTab(value)
      if (nextTab !== this.activeCenterTab) {
        this.activeCenterTab = nextTab
      }
    },
    activeCenterTab(value) {
      this.syncCenterTabToRoute(value)
    }
  },
  beforeDestroy() {
    this.revokeAvatarPreview()
  },
  methods: {
    normalizeCenterTab(tab) {
      const selfTabs = ['overview', 'content', 'project', 'activity', 'knowledge-base', 'service']
      const publicTabs = ['overview', 'content', 'project', 'activity']
      const allowed = this.isSelfMode ? selfTabs : publicTabs
      const normalized = String(tab || '').trim()
      return allowed.includes(normalized) ? normalized : 'overview'
    },

    syncCenterTabToRoute(tab) {
      if (!this.isSelfMode || !this.$route) return
      const normalized = this.normalizeCenterTab(tab)
      const query = { ...(this.$route.query || {}) }
      const current = String(query.tab || '')

      if (normalized === 'overview') {
        if (!current) return
        delete query.tab
        this.$router.replace({ path: this.$route.path, query }).catch(() => {})
        return
      }

      if (current === normalized) return
      this.$router.replace({ path: this.$route.path, query: { ...query, tab: normalized } }).catch(() => {})
    },

    async initializePage() {
      this.pageError = ''
      this.pageLoading = true
      this.sectionLoading = true
      this.activeAssetFilter = 'all'
      this.activeCenterTab = this.normalizeCenterTab(this.$route.query && this.$route.query.tab)

      try {
        await Promise.all([
          this.loadProfileOverview(),
          this.loadProjectOverview()
        ])
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

      this.syncFormDataFromProfile()
      this.selectedAvatarUrl = this.profile.avatarUrl || DEFAULT_AVATAR
      this.syncAvatarPositionFromUrl(this.selectedAvatarUrl)
      this.selectedAvatarFile = null
      this.revokeAvatarPreview()
    },

    async loadProjectOverview() {
      this.projectLoading = true
      try {
        if (this.isSelfMode) {
          const [mineRes, starredRes, participatedRes] = await Promise.allSettled([
            getMyProjects({ page: 1, size: 12 }),
            getMyStarredProjects({ page: 1, size: 12 }),
            getParticipatedProjects({ page: 1, size: 12 })
          ])
          this.ownedProjects = this.normalizeProjectList(this.unwrapProjectPageResult(this.safeSettled(mineRes)), { isOwner: true, sourceType: 'mine' })
          this.starredProjects = this.normalizeProjectList(this.unwrapProjectPageResult(this.safeSettled(starredRes)), { sourceType: 'starred' })
          this.participatedProjects = this.normalizeProjectList(this.unwrapProjectPageResult(this.safeSettled(participatedRes)), { sourceType: 'participated' })
          this.publicProjects = []
          return
        }

        const response = await pageProjects({
          authorId: Number(this.routeUserId),
          page: 1,
          size: 12,
          sortBy: 'latest'
        })
        this.publicProjects = this.normalizeProjectList(this.unwrapProjectPageResult(response), { isOwner: false, sourceType: 'public' })
        this.ownedProjects = []
        this.starredProjects = []
        this.participatedProjects = []
      } catch (error) {
        console.error('加载项目数据失败:', error)
        this.ownedProjects = []
        this.starredProjects = []
        this.participatedProjects = []
        this.publicProjects = []
      } finally {
        this.projectLoading = false
      }
    },

    safeSettled(result) {
      return result && result.status === 'fulfilled' ? result.value : null
    },

    unwrapProjectPageResult(response) {
      if (!response || typeof response !== 'object') return {}
      if (response.data && typeof response.data === 'object') return response.data
      return response
    },

    normalizeProjectList(pageResult, options = {}) {
      const isOwner = options.isOwner === true
      const sourceType = options.sourceType || ''
      const list = Array.isArray(pageResult && pageResult.list) ? pageResult.list : []
      return list
        .map((item) => {
          if (!item || !item.id) return null
          return {
            id: Number(item.id),
            name: item.name || item.title || `项目 ${item.id}`,
            description: item.description || '',
            visibility: item.visibility || '',
            status: item.status || '',
            stars: Number(item.stars || 0),
            downloads: Number(item.downloads || 0),
            views: Number(item.views || 0),
            createdAt: item.createdAt || item.createTime || '',
            updatedAt: item.updatedAt || item.updateTime || item.createdAt || item.createTime || '',
            isOwner: isOwner || Number(item.authorId) === Number(this.profile.id) || Number(item.creatorId) === Number(this.profile.id),
            role: item.role || '',
            sourceType,
            raw: item
          }
        })
        .filter(Boolean)
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

    handleQuickEntry(key) {
      if (!key) return
      if (key === 'write') return this.handleCreateContent()
      if (key === 'knowledge') return this.handleCreateKnowledge()
      if (key === 'project_mine') return this.handleProjectAction('mine')
      if (key === 'wallet') return this.handleWalletClick()
      if (key === 'orders') return this.handleOrdersClick()
      if (key === 'history') return this.handleHistoryClick()
      if (key === 'public_blog') return (this.activeCenterTab = 'content')
      if (key === 'public_post') return (this.activeCenterTab = 'content')
      if (key === 'public_project') return (this.activeCenterTab = 'project')
      if (key === 'public_activity') return (this.activeCenterTab = 'activity')
    },

    handleProjectAction(action) {
      if (!this.isSelfMode) return
      if (action === 'create') {
        this.$router.push('/myproject')
        return
      }
      if (action === 'mine') {
        this.$router.push('/myproject')
        return
      }
      if (action === 'collection') {
        this.$router.push('/projectcollection')
      }
    },

    formatProjectRole(project) {
      if (!project) return '项目'
      if (project.isOwner) return '我创建的项目'
      if (project.sourceType === 'starred') return '我收藏的项目'
      if (project.sourceType === 'participated') return '我参与的项目'
      if (project.role) return `协作角色：${project.role}`
      return this.isSelfMode ? '我参与的项目' : '公开项目'
    },

    formatProjectVisibility(value) {
      const map = {
        public: '公开',
        private: '私有',
        internal: '内部'
      }
      const key = String(value || '').toLowerCase()
      return map[key] || '可访问'
    },

    openProjectDetail(projectId) {
      if (!projectId) return
      this.$router.push(`/projectdetail?projectId=${projectId}`)
    },

    openProjectWorkbench(projectId) {
      if (!projectId) return
      this.$router.push(`/projectmanage?projectId=${projectId}`)
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

    handleOrdersClick() {
      if (!this.isSelfMode) return
      this.$router.push('/orders_purchases')
    },

    handleVipClick() {
      if (!this.isSelfMode) return
      this.$router.push('/vip')
    },

    handleWalletClick() {
      if (!this.isSelfMode) return
      this.$router.push('/wallet')
    },

    handleCreateContent() {
      if (!this.isSelfMode) return
      this.$router.push('/blogwrite')
    },

    handleActivityShortcut(key) {
      if (!this.isSelfMode) return
      if (key === 'collection') {
        this.handleCollectClick()
        return
      }
      if (key === 'history') {
        this.handleHistoryClick()
      }
    },

    handleAccountAction(key) {
      if (!this.isSelfMode) return
      if (key === 'wallet') {
        this.handleWalletClick()
        return
      }
      if (key === 'orders') {
        this.handleOrdersClick()
        return
      }
      if (key === 'coupons') {
        this.handleCouponsClick()
        return
      }
      if (key === 'vip') {
        this.handleVipClick()
        return
      }
      if (key === 'project') {
        this.handleProjectAction('mine')
      }
    },

    extractSummary(item) {
      const raw = item && (item.summary || item.content) ? (item.summary || item.content) : ''
      const plain = String(raw).replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
      if (!plain) return '暂无摘要'
      return plain.length > 80 ? `${plain.slice(0, 80)}...` : plain
    },

    formatDate(value) {
      if (!value) return '未知时间'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '未知时间'
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },

    toTimestamp(value) {
      if (!value) return 0
      const date = new Date(value)
      return Number.isNaN(date.getTime()) ? 0 : date.getTime()
    },

    formatActivityBreakdown(breakdown = {}) {
      const parts = [
        breakdown.commits ? `提交 ${breakdown.commits}` : '',
        breakdown.blogs ? `博客 ${breakdown.blogs}` : '',
        breakdown.posts ? `帖子 ${breakdown.posts}` : '',
        breakdown.likes ? `点赞 ${breakdown.likes}` : '',
        breakdown.collects ? `收藏 ${breakdown.collects}` : '',
        breakdown.logs ? `日志 ${breakdown.logs}` : ''
      ].filter(Boolean)

      return parts.length ? parts.slice(0, 3).join(' / ') : '当天有活跃记录'
    },

    openAssetDetail(item) {
      if (!item) return
      if (item.type === 'blog') {
        this.openBlogDetail(item.id)
        return
      }
      if (item.type === 'post') {
        this.openPostDetail(item.id)
        return
      }
      this.openKnowledgeDetail(item.id)
    },

    handleEditAsset(item) {
      if (!item || item.type !== 'knowledge') return
      this.handleEditKnowledge(item.raw)
    },

    handleDeleteAsset(item) {
      if (!item) return
      if (item.type === 'blog') {
        this.handleDeleteBlog(item.raw)
        return
      }
      if (item.type === 'post') {
        this.handleDeletePost(item.raw)
        return
      }
      this.handleDeleteKnowledge(item.raw)
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
      this.$confirm(`确认删除帖子《${post.title}》吗？此操作不可恢复。`, '提示', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await DeleteCircleComment(post.id)
          this.$message.success('帖子删除成功')
          await this.loadProfileOverview()
        } catch (error) {
          console.error('删除帖子失败:', error)
          this.$message.error('删除帖子失败，请稍后重试')
        }
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
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.profile-shell {
  max-width: 1380px;
  margin: 0 auto;
  padding: 20px 24px 16px;
  flex: 1;
  width: 100%;
  overflow: visible;
}

.page-error {
  margin-bottom: 16px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.left-column {
  min-height: 0;
}

.main-column {
  min-width: 0;
  min-height: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 16px;
}

.left-column :deep(.profile-hero-card) {
  height: 100%;
}

.command-panel,
.workspace-shell,
.service-panel {
  border-radius: 12px;
  border: 1px solid var(--it-border);
  background: linear-gradient(180deg, var(--it-surface-solid), var(--it-surface));
  box-shadow: var(--it-shadow-soft, var(--it-shadow));
}

.command-panel {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.command-copy {
  min-width: 0;
}

.command-kicker {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--it-accent);
}

.command-title,
.panel-title {
  margin: 0;
  color: var(--it-text);
}

.command-title {
  margin-top: 6px;
  font-size: 26px;
  line-height: 1.15;
}

.command-subtitle,
.panel-subtitle {
  margin: 8px 0 0;
  color: var(--it-text-subtle);
  font-size: 13px;
  line-height: 1.6;
}

.command-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.summary-chips {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-muted);
  color: var(--it-text-subtle);
  font-size: 12px;
  font-weight: 600;
}

.workspace-shell {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dashboard-grid {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 14px;
}

.side-panels,
.service-panel {
  min-height: 0;
}

.service-panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-head--compact {
  justify-content: flex-start;
}

.panel-title {
  font-size: 20px;
  line-height: 1.25;
}

.user-profile-page ::v-deep .el-button:not(.el-button--text) {
  height: 34px;
  padding: 0 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
}

.user-profile-page ::v-deep .el-button--small:not(.el-button--text) {
  height: 32px;
  padding: 0 12px;
}

.user-profile-page ::v-deep .el-button--mini:not(.el-button--text) {
  height: 28px;
  padding: 0 10px;
  border-radius: 8px;
  font-size: 12px;
}

.center-tabs ::v-deep .el-tabs__header {
  margin: 0 0 6px;
}

.center-tabs ::v-deep .el-tabs__nav-wrap::after {
  background: var(--it-border);
}

.center-tabs ::v-deep .el-tabs__item {
  height: 34px;
  line-height: 34px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--it-text-subtle);
}

.center-tabs ::v-deep .el-tabs__item.is-active {
  color: var(--it-accent);
  font-weight: 600;
}

.center-tabs ::v-deep .el-tabs__active-bar {
  background: var(--it-accent);
}

.center-tabs {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.center-tabs ::v-deep .el-tabs__content {
  min-height: 0;
  flex: 1;
}

.center-tabs ::v-deep .el-tab-pane {
  height: 100%;
  min-height: 0;
}

.tab-scroller {
  min-height: 320px;
  overflow: visible;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tab-scroller::-webkit-scrollbar,
.activity-feed::-webkit-scrollbar,
.asset-panel__body::-webkit-scrollbar {
  width: 8px;
}

.tab-scroller::-webkit-scrollbar-thumb,
.activity-feed::-webkit-scrollbar-thumb,
.asset-panel__body::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: color-mix(in srgb, var(--it-accent) 20%, transparent);
}

.tab-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.tab-section-title {
  margin: 0;
  color: var(--it-text);
  font-size: 17px;
}

.tab-section-subtitle {
  margin: 8px 0 0;
  color: var(--it-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.tab-section-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-metric-card {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.overview-metric-card__label {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.overview-metric-card__value {
  color: var(--it-text);
  font-size: 24px;
  line-height: 1;
}

.overview-metric-card__note {
  color: var(--it-text-muted);
  font-size: 12px;
}

.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-entry-card {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 15px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 7px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.quick-entry-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border));
  box-shadow: var(--it-shadow-soft, var(--it-shadow));
}

.quick-entry-card__label {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.quick-entry-card__value {
  color: var(--it-text);
  font-size: 18px;
}

.quick-entry-card__note {
  color: var(--it-text-muted);
  font-size: 12px;
}

.overview-dual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.overview-panel {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 14px;
}

.overview-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.overview-panel__head h5 {
  margin: 0;
  color: var(--it-text);
  font-size: 14px;
}

.overview-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-list__item {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface);
  padding: 11px 13px;
  text-align: left;
  display: grid;
  gap: 4px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.overview-list__item:hover {
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border));
  background: color-mix(in srgb, var(--it-accent-soft) 58%, var(--it-surface));
}

.overview-list__item span,
.overview-list__item em {
  font-size: 12px;
  color: var(--it-text-subtle);
  font-style: normal;
}

.overview-list__item strong {
  color: var(--it-text);
  font-size: 13px;
  line-height: 1.4;
}

.asset-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.asset-filter {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-muted);
  color: var(--it-text-subtle);
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.asset-filter.active {
  color: var(--it-accent);
  border-color: color-mix(in srgb, var(--it-accent) 20%, var(--it-border));
  background: var(--it-accent-soft);
}

.asset-panel__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.project-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.project-stat-card {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 13px 15px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.project-stat-card__label {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.project-stat-card__value {
  color: var(--it-text);
  font-size: 20px;
}

.shortcut-grid,
.account-grid,
.activity-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.shortcut-card,
.account-card,
.activity-metric {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.shortcut-card,
.account-card {
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
  text-align: left;
}

.shortcut-card:hover,
.account-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border));
}

.shortcut-card__label,
.account-card__label,
.activity-metric__label {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.shortcut-card__value,
.account-card__value,
.activity-metric__value {
  color: var(--it-text);
  font-size: 20px;
  line-height: 1;
}

.shortcut-card__note,
.account-card__note {
  color: var(--it-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.activity-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) 320px;
  gap: 12px;
}

.heatmap-panel {
  min-height: 0;
  padding: 14px;
  overflow: hidden;
}

.activity-feed-panel {
  padding: 14px;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.activity-feed__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.activity-feed__head h4 {
  margin: 0;
  font-size: 15px;
  color: var(--it-text);
}

.activity-feed__head span {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.activity-feed {
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.asset-card {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 11px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.asset-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--it-accent) 22%, var(--it-border));
  box-shadow: var(--it-shadow-soft, var(--it-shadow));
}

.asset-card__head,
.asset-meta,
.asset-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.asset-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.asset-badge,
.asset-meta__item {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--it-border);
  background: var(--it-surface);
  color: var(--it-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.asset-badge--blog {
  color: var(--it-accent);
  background: color-mix(in srgb, var(--it-accent-soft) 84%, transparent);
}

.asset-badge--post {
  color: var(--it-warning);
  background: color-mix(in srgb, var(--it-warning-soft) 82%, transparent);
}

.asset-badge--knowledge {
  color: var(--it-success);
  background: color-mix(in srgb, var(--it-success-soft) 82%, transparent);
}

.asset-badge--project {
  color: color-mix(in srgb, var(--it-accent) 70%, #88aaff);
  background: color-mix(in srgb, var(--it-accent-soft) 76%, transparent);
}

.asset-badge--status {
  color: var(--it-text-subtle);
}

.asset-date {
  color: var(--it-text-subtle);
  font-size: 12px;
}

.asset-card__body h4 {
  margin: 0;
  font-size: 16px;
  color: var(--it-text);
}

.asset-card__body p {
  margin: 8px 0 0;
  color: var(--it-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.panel-empty,
.activity-empty {
  min-height: 160px;
  border-radius: 10px;
  border: 1px dashed var(--it-border);
  background: var(--it-surface-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--it-text-subtle);
  text-align: center;
}

.panel-empty--compact {
  min-height: 96px;
}

.panel-empty i {
  font-size: 26px;
}

.activity-row {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.activity-row__date {
  display: inline-block;
  color: var(--it-text);
  font-size: 13px;
  font-weight: 600;
}

.activity-row__detail {
  margin: 6px 0 0;
  color: var(--it-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.activity-row__count {
  color: var(--it-text);
  font-size: 22px;
}

.service-tip-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.service-tip-card {
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 13px;
}

.service-tip-card h5 {
  margin: 0;
  color: var(--it-text);
  font-size: 14px;
}

.service-tip-card p {
  margin: 8px 0 0;
  color: var(--it-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.profile-footer {
  margin-top: 8px;
  padding-bottom: 10px;
  flex: 0 0 auto;
}

.edit-dialog ::v-deep .el-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.edit-dialog ::v-deep .el-dialog__header {
  padding: 18px 22px;
  border-bottom: 1px solid var(--it-border);
}

.edit-dialog ::v-deep .el-dialog__body {
  padding: 18px 22px 10px;
}

.edit-dialog ::v-deep .el-dialog__footer {
  padding: 12px 22px 18px;
}

.edit-dialog-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.edit-avatar-column,
.edit-fields-column {
  min-width: 0;
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-solid);
  padding: 15px;
}

.edit-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
}

.field {
  min-width: 0;
  margin-bottom: 0;
}

.field--span-2 {
  grid-column: 1 / -1;
}

.edit-form ::v-deep .el-form-item__label {
  padding-bottom: 6px;
  line-height: 1.2;
  color: var(--it-text);
}

.edit-form ::v-deep .el-form-item__content {
  line-height: 1.2;
}

.radio-row {
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.avatar-edit-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.avatar-upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 13px;
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-muted);
  text-align: center;
}

.avatar-live-preview {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  flex: 0 0 88px;
  border: 3px solid var(--it-surface-solid);
  box-shadow: var(--it-shadow);
}

.avatar-live-preview img {
  display: block;
  width: 100%;
  height: 100%;
}

.avatar-upload-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: center;
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
  padding: 13px 14px;
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface);
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
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.avatar-option {
  border: 1px solid var(--it-border);
  border-radius: 10px;
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

.profile-kb-embed {
  width: 100%;
}

.profile-kb-embed ::v-deep .kb-front-page {
  padding: 0;
}

@media (max-width: 1200px) {
  .user-profile-page {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .profile-shell {
    overflow: visible;
  }

  .profile-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .tab-scroller,
  .asset-panel__body,
  .activity-feed {
    overflow: visible;
    padding-right: 0;
    height: auto;
  }

  .activity-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .profile-shell {
    padding: 16px 12px;
  }

  .command-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-chips,
  .overview-metrics,
  .quick-entry-grid,
  .overview-dual-grid,
  .project-stat-grid,
  .shortcut-grid,
  .account-grid,
  .service-tip-grid,
  .activity-metrics,
  .edit-form-grid,
  .avatar-selector,
  .activity-layout {
    grid-template-columns: 1fr;
  }

  .edit-dialog-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .command-actions {
    width: 100%;
  }

  .command-actions .el-button {
    flex: 1 1 calc(50% - 6px);
  }

  .tab-section-actions {
    width: 100%;
  }

  .tab-section-actions .el-button {
    flex: 1 1 calc(50% - 4px);
  }
}
</style>
