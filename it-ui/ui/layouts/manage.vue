<template>
  <el-container class="admin-shell" :class="{ 'is-collapsed': sidebarCollapsed }">
    <el-header class="admin-header">
      <div class="admin-header__inner">
        <div class="admin-brand">
          <button class="icon-button" type="button" @click="toggleSidebar">
            <i :class="sidebarCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>
          <div class="brand-mark">IT</div>
          <div class="brand-copy">
            <strong>管理后台</strong>
            <span>内容、用户、项目、系统</span>
          </div>
        </div>

        <div class="admin-page-chip">
          <span>当前页面</span>
          <strong>{{ currentPageTitle }}</strong>
        </div>

        <div class="admin-actions">
          <ThemeToggle />
          <el-dropdown @command="handleDropdownCommand">
            <span class="admin-user">
              <span class="admin-user__avatar">{{ displayName.slice(0, 1) }}</span>
              <span class="admin-user__text">
                <strong>{{ displayName }}</strong>
                <small>后台操作台</small>
              </span>
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-container class="admin-body">
      <el-aside :width="sidebarWidth" class="admin-aside">
        <div class="admin-aside__head" v-if="!sidebarCollapsed">
          <div>
            <strong>导航目录</strong>
            <span>后台菜单</span>
          </div>
          <div class="aside-actions">
            <button class="small-button" type="button" title="展开全部" @click="expandAllMenus">
              <i class="el-icon-s-operation"></i>
            </button>
            <button class="small-button" type="button" title="收起全部" @click="collapseAllMenus">
              <i class="el-icon-minus"></i>
            </button>
          </div>
        </div>

        <el-menu
          :default-active="$route.path"
          :default-openeds="openedMenus"
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          :unique-opened="false"
          class="admin-menu"
          router
          @open="handleMenuOpen"
          @close="handleMenuClose"
        >
          <menu-item
            v-for="menu in menus"
            :key="menu.id || menu.path || menu.name"
            :menu="menu"
          />
        </el-menu>
      </el-aside>

      <el-container class="admin-content" :style="contentOffsetStyle">
        <div class="admin-tabs" v-if="tabs.length > 0">
          <el-tabs
            v-model="activeTab"
            type="card"
            closable
            @tab-click="handleTabClick"
            @tab-remove="handleTabRemove"
          >
            <el-tab-pane
              v-for="tab in tabs"
              :key="tab.name"
              :label="tab.title"
              :name="tab.name"
            />
          </el-tabs>
        </div>

        <el-main class="admin-main" :class="{ 'has-tabs': tabs.length > 0 }">
          <nuxt />
        </el-main>

        <el-footer class="admin-footer">
          <span>© 2026 IT 管理后台</span>
          <span>统一权限、菜单与路由</span>
        </el-footer>
      </el-container>
    </el-container>
  </el-container>
</template>

<script>
import { useMenuStore } from '~/store/menu'
import { useUserStore } from '~/store/user'
import { adminMenuPathMap } from '@/utils/permissionConfig'

const MenuItem = {
  name: 'MenuItem',
  props: {
    menu: {
      type: Object,
      required: true
    }
  },
  template: `
    <el-submenu v-if="hasChildren" :index="submenuIndex">
      <template slot="title">
        <i :class="menu.icon || 'el-icon-menu'"></i>
        <span>{{ menu.name }}</span>
      </template>
      <menu-item
        v-for="child in menu.children"
        :key="child.id || child.path || child.name"
        :menu="child"
      />
    </el-submenu>
    <el-menu-item v-else :index="menu.path || ''" :disabled="!menu.path">
      <i :class="menu.icon || 'el-icon-menu'"></i>
      <span slot="title">{{ menu.name }}</span>
    </el-menu-item>
  `,
  computed: {
    hasChildren() {
      return Array.isArray(this.menu.children) && this.menu.children.length > 0
    },
    submenuIndex() {
      return String(this.menu.id || this.menu.path || this.menu.name)
    }
  }
}

export default {
  name: 'ManageLayout',
  components: {
    MenuItem
  },
  data() {
    return {
      activeTab: '',
      tabs: [],
      menus: [],
      openedMenus: [],
      sidebarCollapsed: false,
      menuMap: adminMenuPathMap,
      menuLoading: false
    }
  },
  computed: {
    sidebarWidth() {
      return this.sidebarCollapsed ? '84px' : '248px'
    },
    contentOffsetStyle() {
      return {
        marginLeft: this.sidebarWidth,
        width: `calc(100% - ${this.sidebarWidth})`
      }
    },
    currentPageTitle() {
      return this.resolveTitle(this.$route.path) || '后台首页'
    },
    displayName() {
      const userStore = useUserStore()
      const user = userStore.getUserInfo || {}
      return user.nickname || user.username || '管理员'
    }
  },
  watch: {
    '$route.path': {
      immediate: true,
      handler(path) {
        this.addTab(path)
        this.expandMenuByRoute(path)
      }
    },
    menus: {
      deep: true,
      handler() {
        this.expandMenuByRoute(this.$route.path)
      }
    }
  },
  async mounted() {
    const cachedCollapsed = process.client ? window.localStorage.getItem('adminSidebarCollapsed') : null
    if (cachedCollapsed !== null) {
      this.sidebarCollapsed = cachedCollapsed === '1'
    }

    await this.fetchMenus()

    if (this.$route.path === '/admin') {
      const firstPath = this.findFirstMenuPath(this.menus)
      this.$router.replace(firstPath || '/noPermission')
    }
  },
  methods: {
    async fetchMenus() {
      if (this.menuLoading) {
        return
      }

      this.menuLoading = true
      try {
        const userStore = useUserStore()
        userStore.restorePermissions()

        try {
          await userStore.refreshPermissions()
        } catch (error) {
          console.warn('刷新权限失败，继续使用当前权限缓存:', error)
        }

        const menuStore = useMenuStore()
        await menuStore.fetchMenus()
        this.menus = menuStore.getFilteredMenus
      } catch (error) {
        console.error('加载后台菜单失败:', error)
        this.menus = []
      } finally {
        this.menuLoading = false
      }
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      if (process.client) {
        window.localStorage.setItem('adminSidebarCollapsed', this.sidebarCollapsed ? '1' : '0')
      }
    },
    async handleDropdownCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user')
        return
      }

      if (command === 'logout') {
        try {
          const userStore = useUserStore()
          await userStore.logout()
          this.$message.success('退出登录成功')
          this.$router.replace('/login')
        } catch (error) {
          console.error('退出登录失败:', error)
          this.$message.error('退出登录失败，请重试')
        }
      }
    },
    resolveTitle(path) {
      return this.menuMap[path] && this.menuMap[path].title
    },
    resolveTabName(path) {
      const matched = this.menuMap[path]
      return matched ? matched.name : String(path || '').replace(/[/:-]+/g, '_').replace(/^_+|_+$/g, '')
    },
    addTab(path) {
      if (!path || !this.resolveTitle(path)) {
        return
      }

      const name = this.resolveTabName(path)
      if (!this.tabs.some(tab => tab.name === name)) {
        this.tabs.push({
          name,
          title: this.resolveTitle(path),
          path
        })
      }

      this.activeTab = name
    },
    handleTabClick(tab) {
      const tabInfo = this.tabs.find(item => item.name === tab.name)
      if (tabInfo && tabInfo.path && tabInfo.path !== this.$route.path) {
        this.$router.push(tabInfo.path)
      }
    },
    handleTabRemove(tabName) {
      if (this.tabs.length <= 1) {
        this.$message.warning('至少保留一个页签')
        return
      }

      const tabIndex = this.tabs.findIndex(tab => tab.name === tabName)
      if (tabIndex === -1) {
        return
      }

      const removed = this.tabs.splice(tabIndex, 1)[0]
      if (removed.name === this.activeTab) {
        const nextTab = this.tabs[Math.max(0, tabIndex - 1)]
        this.activeTab = nextTab.name
        this.$router.push(nextTab.path)
      }
    },
    expandAllMenus() {
      const indexes = []
      const walk = (menus) => {
        menus.forEach((menu) => {
          if (Array.isArray(menu.children) && menu.children.length > 0) {
            indexes.push(String(menu.id || menu.path || menu.name))
            walk(menu.children)
          }
        })
      }
      walk(this.menus)
      this.openedMenus = indexes
    },
    collapseAllMenus() {
      this.openedMenus = []
    },
    expandMenuByRoute(routePath) {
      if (!routePath) {
        return
      }

      const findParents = (menus, targetPath, parents = []) => {
        for (const menu of menus) {
          const currentIndex = String(menu.id || menu.path || menu.name)
          if (menu.path === targetPath) {
            return parents
          }
          if (Array.isArray(menu.children) && menu.children.length > 0) {
            const found = findParents(menu.children, targetPath, [...parents, currentIndex])
            if (found) {
              return found
            }
          }
        }
        return null
      }

      const parentIndexes = findParents(this.menus, routePath)
      if (!parentIndexes) {
        return
      }

      parentIndexes.forEach((index) => {
        if (!this.openedMenus.includes(index)) {
          this.openedMenus.push(index)
        }
      })
    },
    handleMenuOpen(index) {
      if (!this.openedMenus.includes(index)) {
        this.openedMenus.push(index)
      }
    },
    handleMenuClose(index) {
      const position = this.openedMenus.indexOf(index)
      if (position > -1) {
        this.openedMenus.splice(position, 1)
      }
    },
    findFirstMenuPath(menus = []) {
      for (const menu of menus) {
        if (menu.path && this.resolveTitle(menu.path)) {
          return menu.path
        }
        const childPath = this.findFirstMenuPath(menu.children || [])
        if (childPath) {
          return childPath
        }
      }
      return ''
    }
  }
}
</script>

<style scoped>
html,
body,
#__nuxt,
#__layout,
.admin-shell {
  min-height: 100%;
  margin: 0;
}

.admin-shell {
  min-height: 100vh;
  background: var(--it-page-bg);
  color: var(--it-text);
}

.admin-header {
  height: 64px;
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1001;
  background: color-mix(in srgb, var(--it-header-bg) 92%, transparent);
  border-bottom: 1px solid var(--it-border);
  box-shadow: var(--it-shadow-strong);
  backdrop-filter: blur(18px);
}

.admin-header__inner {
  height: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 20px;
  padding: 0 16px;
}

.admin-brand,
.admin-actions,
.admin-user,
.aside-actions {
  display: flex;
  align-items: center;
}

.admin-brand {
  gap: 12px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  background: var(--it-primary-gradient);
  font-weight: 700;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-copy strong {
  font-size: 16px;
}

.brand-copy span,
.admin-page-chip span,
.admin-user__text small,
.admin-aside__head span,
.admin-footer {
  color: var(--it-text-subtle);
}

.admin-page-chip {
  justify-self: center;
  min-width: min(420px, 100%);
  padding: 9px 14px;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  text-align: center;
  background: color-mix(in srgb, var(--it-surface-solid) 86%, transparent);
}

.admin-page-chip span {
  margin-right: 8px;
  font-size: 12px;
}

.admin-page-chip strong {
  font-size: 13px;
}

.admin-actions {
  gap: 10px;
}

.admin-user {
  gap: 10px;
  padding: 7px 10px;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  color: var(--it-text);
  background: color-mix(in srgb, var(--it-surface-solid) 86%, transparent);
  cursor: pointer;
}

.admin-user__avatar {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--it-accent);
  background: var(--it-accent-soft);
  font-weight: 700;
}

.admin-user__text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.icon-button,
.small-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--it-border);
  border-radius: 8px;
  color: var(--it-text-muted);
  background: var(--it-surface-muted);
  cursor: pointer;
}

.icon-button {
  width: 34px;
  height: 34px;
}

.small-button {
  width: 30px;
  height: 30px;
}

.icon-button:hover,
.small-button:hover,
.admin-user:hover {
  color: var(--it-accent);
  border-color: var(--it-border-strong);
  background: var(--it-accent-soft);
}

.admin-body {
  min-height: 100vh;
}

.admin-aside {
  position: fixed;
  top: 64px;
  left: 0;
  z-index: 1000;
  height: calc(100vh - 64px);
  overflow-y: auto;
  overflow-x: hidden;
  border-right: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-sidebar-bg) 92%, transparent);
  backdrop-filter: blur(18px);
  transition: width .24s ease;
}

.admin-aside__head {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--it-border);
}

.admin-aside__head div:first-child {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.aside-actions {
  gap: 6px;
}

.admin-menu {
  border: none;
  background: transparent !important;
  padding: 10px;
}

.admin-menu.el-menu--collapse {
  padding-left: 8px;
  padding-right: 8px;
}

.admin-menu /deep/ .el-menu-item,
.admin-menu /deep/ .el-submenu__title {
  height: 42px;
  line-height: 42px;
  margin-bottom: 6px;
  border-radius: 8px;
  color: var(--it-text-muted);
}

.admin-menu /deep/ .el-menu-item:hover,
.admin-menu /deep/ .el-submenu__title:hover {
  color: var(--it-accent) !important;
  background: var(--it-accent-soft) !important;
}

.admin-menu /deep/ .el-menu-item.is-active {
  color: #fff !important;
  background: var(--it-primary-gradient) !important;
  box-shadow: var(--it-button-shadow);
}

.admin-menu /deep/ .el-menu-item.is-active i {
  color: #fff !important;
}

.admin-content {
  min-height: 100vh;
  transition: margin-left .24s ease, width .24s ease;
}

.admin-tabs {
  position: fixed;
  top: 64px;
  left: inherit;
  right: 0;
  z-index: 999;
  height: 48px;
  width: inherit;
  padding: 8px 18px 0;
  border-bottom: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-header-bg) 90%, transparent);
  backdrop-filter: blur(16px);
}

.admin-tabs /deep/ .el-tabs__header {
  margin: 0;
}

.admin-tabs /deep/ .el-tabs__item {
  height: 34px;
  line-height: 34px;
  border-radius: 8px 8px 0 0;
}

.admin-main {
  width: 100%;
  min-height: calc(100vh - 108px);
  margin-top: 64px;
  padding: 24px 22px 76px;
  background: transparent;
}

.admin-main.has-tabs {
  margin-top: 112px;
}

.admin-footer {
  position: fixed;
  left: inherit;
  right: 0;
  bottom: 0;
  width: inherit;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-top: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-header-bg) 90%, transparent);
}

@media (max-width: 1100px) {
  .admin-header__inner {
    grid-template-columns: auto auto;
  }

  .admin-page-chip {
    display: none;
  }
}

@media (max-width: 720px) {
  .brand-copy,
  .admin-user__text,
  .admin-footer {
    display: none;
  }
}
</style>
