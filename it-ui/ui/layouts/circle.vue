<template>
  <div data-testid="circle-layout" class="layout-container circle-layout">
    <el-header class="header circle-header">
      <div class="header-content circle-header-inner">
        <div class="header-left">
          <button class="collapse-btn" type="button" @click="toggleSidebar">
            <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>

          <div class="brand-block" @click="$router.push('/')">
            <span class="brand-mark">IT</span>
            <div class="brand-copy">
              <strong>IT Forum</strong>
              <span>圈子空间</span>
            </div>
          </div>
        </div>

        <div v-if="!isSpecialPage" class="search-area">
          <el-input
            v-model="searchKeyword"
            class="search-input"
            clearable
            prefix-icon="el-icon-search"
            placeholder="输入圈子名或帖子关键词"
            @keyup.enter.native="handleSearch"
          ></el-input>
          <el-button type="primary" class="search-btn" @click="handleSearch">搜索</el-button>
        </div>

        <div class="right-actions header-actions">
          <ThemeToggle />
          <el-button data-testid="circle-post-open" type="primary" class="header-btn header-btn--primary" @click="openPostDialog">写帖子</el-button>
          <el-button data-testid="circle-create-open" class="header-btn" @click="openCreateDialog">创建圈子</el-button>
          <el-button class="header-btn" @click="openJoinDialog">加入圈子</el-button>
          <AppUserMenu :size="36" />
        </div>
      </div>
    </el-header>

    <el-dialog
      data-testid="circle-post-dialog"
      title="发布新帖子"
      :visible.sync="dialogVisible"
      width="600px"
      custom-class="circle-dialog"
      :before-close="handleDialogClose"
      :destroy-on-close="true"
      @closed="resetPostDialogState"
    >
      <el-form
        :key="postDialogKey"
        ref="postForm"
        data-testid="circle-post-form"
        :model="postForm"
        :rules="postRules"
        label-width="80px"
      >
        <el-form-item label="内容" prop="content">
          <el-input
            data-testid="circle-post-content-input"
            v-model="postForm.content"
            type="textarea"
            :rows="8"
            maxlength="5000"
            resize="none"
            show-word-limit
            placeholder="请输入帖子内容..."
          ></el-input>
        </el-form-item>

        <el-form-item label="发布到圈子" prop="circleIds">
          <el-select
            ref="postCircleSelect"
            data-testid="circle-post-circle-select"
            v-model="postForm.circleIds"
            class="circle-select"
            multiple
            clearable
            filterable
            popper-class="circle-post-circle-select-popper"
            placeholder="请选择要发布的圈子（可多选）"
            :loading="circleLoading"
          >
            <el-option
              v-for="circle in circleOptions"
              :key="circle.id"
              :label="circle.name"
              :value="circle.id"
            ></el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button data-testid="circle-post-cancel" @click="requestClosePostDialog">取消</el-button>
        <el-button data-testid="circle-post-submit" type="primary" :loading="submitting" @click="submitPost">发布</el-button>
      </span>
    </el-dialog>

    <el-dialog
      data-testid="circle-create-dialog"
      title="创建新圈子"
      :visible.sync="createDialogVisible"
      width="520px"
      custom-class="circle-dialog"
      @close="resetCreateForm"
    >
      <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="圈子名称" prop="name">
          <el-input data-testid="circle-create-name-input" v-model="createForm.name" placeholder="请输入圈子名称"></el-input>
        </el-form-item>
        <el-form-item label="圈子描述" prop="description">
          <el-input
            data-testid="circle-create-description-input"
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="请输入圈子描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="可见性" prop="visibility">
          <el-radio-group v-model="createForm.visibility">
            <el-radio label="public">公开（任何人可加入）</el-radio>
            <el-radio label="private">私密（仅邀请）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="最大成员数" prop="maxMembers">
          <el-input-number v-model="createForm.maxMembers" :min="1" :max="10000"></el-input-number>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button data-testid="circle-create-submit" type="primary" :loading="submittingCreate" @click="submitCreate">创建</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="加入圈子"
      :visible.sync="joinDialogVisible"
      width="520px"
      custom-class="circle-dialog"
      @close="resetJoinForm"
    >
      <el-form ref="joinForm" :model="joinForm" label-width="80px">
        <el-form-item
          label="选择圈子"
          prop="circleId"
          :rules="[{ required: true, message: '请选择圈子', trigger: 'change' }]"
        >
          <el-select
            v-model="joinForm.circleId"
            style="width: 100%"
            filterable
            placeholder="请选择要加入的圈子"
            :loading="circleLoading"
          >
            <el-option
              v-for="circle in circleOptions"
              :key="circle.id"
              :label="circle.name"
              :value="circle.id"
            >
              <span class="join-option-name">{{ circle.name }}</span>
              <span class="join-option-meta">{{ circle.memberCount || 0 }} 成员</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="joinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingJoin" @click="submitJoin">加入</el-button>
      </span>
    </el-dialog>

    <el-container class="main-shell" :class="{ 'is-compact': isCompact }">
      <el-aside :width="asideWidth" class="asid-content circle-sidebar" :class="{ 'is-collapsed': menuCollapsed }">
        <el-menu
          :default-active="activeMenu"
          class="module-menu circle-menu"
          :collapse="menuCollapsed"
          :collapse-transition="true"
          @select="handleMenuSelect"
        >
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-content layout-main">
        <nuxt />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { GetAllCircles, CreateCircle, CircleJoin, CreateCircleComment } from '@/api/circle'
import { useUserStore } from '@/store/user'

export default {
  data() {
    return {
      isCollapse: false,
      isCompact: false,
      searchKeyword: '',
      activeTag: 'all',
      dialogVisible: false,
      postDialogKey: 0,
      submitting: false,
      postForm: {
        content: '',
        circleIds: []
      },
      postRules: {
        content: [
          { required: true, message: '请输入帖子内容', trigger: 'blur' },
          { min: 5, max: 5000, message: '长度在 5 到 5000 个字符', trigger: 'blur' }
        ],
        circleIds: [
          { type: 'array', required: true, message: '请至少选择一个圈子', trigger: 'change' }
        ]
      },
      createDialogVisible: false,
      createForm: {
        name: '',
        description: '',
        visibility: 'public',
        maxMembers: null
      },
      createRules: {
        name: [
          { required: true, message: '请输入圈子名称', trigger: 'blur' },
          { max: 100, message: '圈子名称长度不能超过 100 个字符', trigger: 'blur' }
        ],
        description: [
          { max: 1000, message: '圈子描述长度不能超过 1000 个字符', trigger: 'blur' }
        ],
        visibility: [
          { required: true, message: '请选择可见性', trigger: 'change' }
        ],
        maxMembers: [
          { type: 'number', min: 1, max: 10000, message: '最大成员数应在 1-10000 之间', trigger: 'blur' }
        ]
      },
      submittingCreate: false,
      joinDialogVisible: false,
      joinForm: {
        circleId: null
      },
      submittingJoin: false,
      circleOptions: [],
      circleLoading: false,
      menuItems: [
        { index: '/', icon: 'el-icon-s-home', title: '首页' },
        { index: '/blog', icon: 'el-icon-document', title: '博客' },
        { index: '/circle', icon: 'el-icon-s-comment', title: '圈子' },
        { index: '/projectlist', icon: 'el-icon-document', title: '项目列表' },
        { index: '/myproject', icon: 'el-icon-folder-opened', title: '我的项目' },
        { index: '/projectcollection', icon: 'el-icon-star-on', title: '收藏项目' },
        { index: '/wallet', icon: 'el-icon-wallet', title: '我的钱包' },
        { index: '/vip', icon: 'el-icon-crown', title: 'VIP服务' },
        { index: '/user', icon: 'el-icon-user', title: '个人中心' }
      ],
      userId: null,
      username: ''
    }
  },
  computed: {
    asideWidth() {
      if (this.isCompact) {
        return '100%'
      }
      return this.isCollapse ? '84px' : '228px'
    },
    menuCollapsed() {
      return this.isCompact ? false : this.isCollapse
    },
    isSpecialPage() {
      const path = this.$route.path
      return path.startsWith('/circle/') || path.startsWith('/write')
    },
    activeMenu() {
      const path = this.$route.path
      if (path.startsWith('/blog')) return '/blog'
      if (path.startsWith('/circle')) return '/circle'
      if (path.startsWith('/user')) return '/user'
      if (path.startsWith('/projectlist')) return '/projectlist'
      if (path.startsWith('/myproject')) return '/myproject'
      if (path.startsWith('/projectcollection')) return '/projectcollection'
      if (path.startsWith('/wallet')) return '/wallet'
      if (path.startsWith('/vip')) return '/vip'
      return '/'
    }
  },
  watch: {
    '$route.query': {
      handler(query) {
        this.searchKeyword = query.keyword || ''
        this.activeTag = query.tag || 'all'
      },
      immediate: true
    }
  },
  created() {
    this.fetchCircles()
    this.restoreSession()
  },
  mounted() {
    this.syncViewport()
    window.addEventListener('resize', this.syncViewport, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewport)
  },
  methods: {
    getUserStore() {
      return useUserStore()
    },
    syncViewport() {
      if (!process.client) {
        return
      }
      this.isCompact = window.innerWidth <= 960
    },
    async restoreSession() {
      if (!process.client) {
        return
      }

      const userStore = this.getUserStore()
      userStore.restorePermissions()

      if (!userStore.userInfo && !userStore.token) {
        this.userId = null
        this.username = ''
        return
      }

      try {
        await userStore.syncSessionFromServer({
          forceReloadPermissions: !userStore.permissions?.length
        })
        const currentUser = userStore.userInfo || userStore.user || {}
        this.userId = currentUser.id || currentUser.userId || null
        this.username = currentUser.nickname || currentUser.username || currentUser.name || ''
      } catch (error) {
        userStore.clearLocalState()
        this.userId = null
        this.username = ''
      }
    },
    ensureAuthenticated(actionName = '继续操作') {
      const userStore = this.getUserStore()
      const currentUser = userStore.userInfo || userStore.user
      if (userStore.isLoggedIn && currentUser && this.userId) {
        return true
      }

      this.$confirm(`${actionName}前需要先登录，是否前往登录页？`, '未登录', {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.$router.push({
          path: '/login',
          query: { redirect: this.$route.fullPath || '/circle' }
        })
      }).catch(() => {})

      return false
    },
    async fetchCircles() {
      this.circleLoading = true
      try {
        const res = await GetAllCircles()
        let circles = []
        if (Array.isArray(res)) {
          circles = res
        } else if (res.data && Array.isArray(res.data)) {
          circles = res.data
        } else if (res.data && res.data.list) {
          circles = res.data.list
        }
        this.circleOptions = circles.filter(circle => circle.type !== 'pending')
      } catch (error) {
        this.$message.error('获取圈子列表失败')
      } finally {
        this.circleLoading = false
      }
    },
    toggleSidebar() {
      if (this.isCompact) {
        return
      }
      this.isCollapse = !this.isCollapse
    },
    handleSearch() {
      this.$router.push({
        query: {
          ...this.$route.query,
          keyword: this.searchKeyword || undefined,
          page: 1
        }
      })
    },
    handleMenuSelect(index) {
      const protectedRoutes = new Set(['/myproject', '/projectcollection', '/wallet', '/vip', '/user'])
      if (index === '/') {
        if (process.client && window.location.pathname !== '/') {
          window.location.assign('/')
        }
        return
      }
      if (protectedRoutes.has(index) && !this.ensureAuthenticated('访问该页面')) {
        return
      }
      if (this.$route.path !== index) {
        this.$router.push(index).catch(() => {})
      }
    },
    handleRequestError(error, action) {
      if (error.response) {
        this.$message.error(`${action}失败：${error.response.data?.message || `服务器错误 ${error.response.status}`}`)
      } else if (error.request) {
        this.$message.error('网络错误，请检查连接')
      } else {
        this.$message.error(`${action}请求错误：${error.message}`)
      }
    },
    openPostDialog() {
      if (!this.ensureAuthenticated('发帖')) {
        return
      }
      this.resetPostDialogState()
      this.dialogVisible = true
    },
    hasPostDraft() {
      return Boolean((this.postForm.content || '').trim()) || this.postForm.circleIds.length > 0
    },
    confirmPostDialogClose(onConfirm) {
      if (!this.hasPostDraft()) {
        onConfirm()
        return
      }

      this.$confirm('确定关闭？未保存的内容将会丢失', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => onConfirm()).catch(() => {})
    },
    requestClosePostDialog() {
      this.confirmPostDialogClose(() => {
        this.dialogVisible = false
      })
    },
    resetPostDialogState() {
      this.postDialogKey += 1
      this.postForm = {
        content: '',
        circleIds: []
      }
      this.$nextTick(() => {
        this.$refs.postCircleSelect?.blur?.()
        this.$refs.postForm?.clearValidate?.()
      })
    },
    handleDialogClose(done) {
      this.confirmPostDialogClose(done)
    },
    async submitPost() {
      if (!this.ensureAuthenticated('发帖')) {
        return
      }
      this.$refs.postForm.validate(async valid => {
        if (!valid) {
          this.$message.error('请完善帖子信息')
          return
        }

        this.submitting = true
        try {
          for (const circleId of this.postForm.circleIds) {
            await CreateCircleComment({
              content: this.postForm.content,
              circleId,
              authorId: this.userId,
              parentCommentId: null
            })
          }
          this.$message.success('帖子发布成功！')
          this.dialogVisible = false
          if (this.$route.path === '/circle') {
            this.$router.push({
              path: '/circle',
              query: { ...this.$route.query, _t: Date.now() }
            })
          }
        } catch (error) {
          this.handleRequestError(error, '发布')
        } finally {
          this.submitting = false
        }
      })
    },
    openCreateDialog() {
      if (!this.ensureAuthenticated('创建圈子')) {
        return
      }
      this.createDialogVisible = true
      this.$nextTick(() => {
        this.$refs.createForm?.clearValidate()
      })
    },
    resetCreateForm() {
      this.createForm.name = ''
      this.createForm.description = ''
      this.createForm.visibility = 'public'
      this.createForm.maxMembers = null
      this.$refs.createForm?.clearValidate()
    },
    async submitCreate() {
      if (!this.ensureAuthenticated('创建圈子')) {
        return
      }
      this.$refs.createForm.validate(async valid => {
        if (!valid) return
        this.submittingCreate = true
        try {
          const createData = Object.entries(this.createForm).reduce((acc, [key, value]) => {
            if (value !== null && value !== undefined && value !== '') {
              acc[key] = value
            }
            return acc
          }, {})
          const response = await CreateCircle(createData)

          let isSuccess = false
          let errorMessage = '未知错误'

          if (response.status === 200) {
            isSuccess = true
          } else if (Array.isArray(response)) {
            isSuccess = true
          } else if (response.data) {
            isSuccess = response.data.code === 0 || response.data.success
            errorMessage = response.data.message || errorMessage
          } else if (typeof response === 'object') {
            isSuccess = response.code === 0 || response.success
            errorMessage = response.message || errorMessage
          }

          if (isSuccess) {
            this.$message.success('圈子创建成功')
            this.createDialogVisible = false
            await this.fetchCircles()
          } else {
            this.$message.error(`创建失败：${errorMessage}`)
          }
        } catch (error) {
          this.handleRequestError(error, '创建')
        } finally {
          this.submittingCreate = false
        }
      })
    },
    async openJoinDialog() {
      if (!this.ensureAuthenticated('加入圈子')) {
        return
      }
      if (!this.circleOptions.length) {
        await this.fetchCircles()
      }
      this.joinDialogVisible = true
      this.$nextTick(() => {
        this.$refs.joinForm?.clearValidate()
      })
    },
    resetJoinForm() {
      this.joinForm.circleId = null
      this.$refs.joinForm?.clearValidate()
    },
    async submitJoin() {
      if (!this.ensureAuthenticated('加入圈子')) {
        return
      }
      this.$refs.joinForm.validate(async valid => {
        if (!valid) return
        this.submittingJoin = true
        try {
          await CircleJoin(this.joinForm.circleId)
          this.$message.success('成功加入圈子')
          this.joinDialogVisible = false
          await this.fetchCircles()
          if (this.$route.path === '/circle') {
            this.$router.push({
              path: '/circle',
              query: { ...this.$route.query, _t: Date.now() }
            })
          }
        } catch (error) {
          this.handleRequestError(error, '加入')
        } finally {
          this.submittingJoin = false
        }
      })
    }
  }
}
</script>

<style scoped>
.circle-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--it-page-bg);
  color: var(--it-text);
}

.circle-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: var(--it-header-height);
  padding: 0;
  background: var(--it-header-bg);
  border-bottom: 1px solid var(--it-border);
  backdrop-filter: blur(18px);
  box-shadow: var(--it-shadow);
}

.circle-header-inner {
  width: 100%;
  max-width: var(--it-shell-max);
  margin: 0 auto;
  padding: 0 var(--it-shell-padding-x);
  display: grid;
  grid-template-columns: auto minmax(0, 520px) auto;
  gap: 16px;
  align-items: center;
  min-height: var(--it-header-height);
}

.header-left,
.brand-block,
.header-actions,
.search-area {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 14px;
  min-width: 0;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text-muted);
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.collapse-btn:hover {
  color: var(--it-accent);
  border-color: var(--it-border-strong);
  background: var(--it-accent-soft);
}

.brand-block {
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: var(--it-primary-gradient);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  box-shadow: var(--it-shadow);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 15px;
  color: var(--it-text);
}

.brand-copy span {
  font-size: 12px;
  color: var(--it-text-muted);
}

.search-area {
  gap: 10px;
  min-width: 0;
  width: 100%;
}

.search-input {
  flex: 1 1 auto;
  min-width: 0;
}

.right-actions.header-actions {
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-left: auto;
  flex-shrink: 0;
  width: auto;
  min-width: fit-content;
}

.header-btn,
.search-btn {
  border-radius: 12px;
}

.header-btn {
  border-color: var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 92%, transparent);
  color: var(--it-text);
}

.header-btn:hover,
.header-btn:focus {
  color: var(--it-accent);
  border-color: color-mix(in srgb, var(--it-accent) 30%, var(--it-border));
  background: var(--it-accent-soft);
}

.header-btn--primary,
.search-btn {
  border-color: transparent;
  background: var(--it-primary-gradient);
  color: #fff;
}

.header-btn--primary:hover,
.search-btn:hover {
  color: #fff;
}

.main-shell {
  flex: 1;
  overflow: hidden;
}

.asid-content.circle-sidebar {
  width: auto !important;
  background: var(--it-sidebar-bg);
  border-right: 1px solid var(--it-border);
  overflow: hidden;
}

.module-menu.circle-menu {
  border-right: none;
  height: 100%;
  background: transparent !important;
}

.circle-menu:not(.el-menu--collapse) {
  width: 200px;
}

.module-menu.circle-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--it-radius-control);
  color: var(--it-text-muted) !important;
  background: transparent !important;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.module-menu.circle-menu :deep(.el-menu-item:hover) {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.module-menu.circle-menu :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: var(--it-primary-gradient) !important;
  box-shadow: var(--it-shadow);
}

.layout-main {
  padding: 24px;
  overflow-y: auto;
  background: transparent;
}

.join-option-name {
  float: left;
}

.join-option-meta {
  float: right;
  color: var(--it-text-subtle);
  font-size: 12px;
}

:deep(.circle-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

:deep(.circle-dialog .el-dialog) {
  background: var(--it-surface-solid);
  border: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-strong);
}

:deep(.circle-dialog .el-dialog__header) {
  padding: 22px 24px 12px;
}

:deep(.circle-dialog .el-dialog__title) {
  color: var(--it-text);
  font-weight: 700;
}

:deep(.circle-dialog .el-dialog__body) {
  padding: 12px 24px 20px;
}

:deep(.circle-dialog .el-form-item__label) {
  color: var(--it-text-muted);
}

:deep(.circle-dialog .el-textarea__inner),
:deep(.circle-dialog .el-input__inner) {
  background: var(--it-surface-muted);
  border-color: var(--it-border);
  color: var(--it-text);
}

:deep(.circle-dialog .el-textarea__inner:focus),
:deep(.circle-dialog .el-input__inner:focus) {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
}

:deep(.circle-dialog .el-dialog__footer) {
  padding: 0 24px 24px;
}

.circle-select {
  width: 100%;
}

@media screen and (max-width: 960px) {
  .circle-header-inner {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'left actions'
      'search search';
  }

  .header-left {
    grid-area: left;
  }

  .search-area {
    grid-area: search;
  }

  .header-actions {
    grid-area: actions;
  }

  .main-shell.is-compact {
    display: block;
  }

  .asid-content.circle-sidebar {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid var(--it-border);
  }

  .circle-menu:not(.el-menu--collapse) {
    width: 100%;
  }
}

@media screen and (max-width: 768px) {
  .circle-header-inner {
    grid-template-columns: 1fr;
    grid-template-areas:
      'left'
      'actions'
      'search';
  }

  .brand-copy span {
    display: none;
  }

  .search-area {
    flex-direction: column;
    align-items: stretch;
  }

  .right-actions.header-actions {
    width: 100%;
    justify-content: flex-end;
    margin-left: 0;
  }

  .header-btn,
  .search-btn {
    width: 100%;
  }

  .layout-main {
    padding: 16px 12px 24px;
  }
}
</style>
