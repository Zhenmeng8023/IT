<template>
  <el-container class="layout-wrap">
    <el-header class="layout-header">
      <div class="layout-header-inner">
        <div class="layout-logo">博客管理系统</div>

        <div class="layout-user">
          <el-dropdown trigger="click">
            <span class="layout-user-link">
              {{ displayName }}
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="handleProfile">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click.native="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-aside class="layout-aside" width="220px">
      <el-menu
        :default-active="activeIndex"
        :default-openeds="openedMenus"
        class="layout-menu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        :unique-opened="false"
        @open="handleMenuOpen"
        @close="handleMenuClose"
      >
        <template v-for="m in menus">
          <el-submenu
            v-if="hasChildren(m)"
            :key="'sub-' + (m.id || m.path || m.name)"
            :index="String(m.id || m.path || m.name)"
          >
            <template slot="title">
              <i :class="m.icon || 'el-icon-menu'"></i>
              <span>{{ m.name }}</span>
            </template>

            <template v-for="c in m.children">
              <el-submenu
                v-if="hasChildren(c)"
                :key="'sub2-' + (c.id || c.path || c.name)"
                :index="String(c.id || c.path || c.name)"
              >
                <template slot="title">
                  <i :class="c.icon || 'el-icon-menu'"></i>
                  <span>{{ c.name }}</span>
                </template>

                <el-menu-item
                  v-for="g in c.children"
                  :key="'item3-' + (g.id || g.path || g.name)"
                  :index="g.path || ''"
                  :disabled="!g.path"
                >
                  <i :class="g.icon || 'el-icon-menu'"></i>
                  <span slot="title">{{ g.name }}</span>
                </el-menu-item>
              </el-submenu>

              <el-menu-item
                v-else
                :key="'item2-' + (c.id || c.path || c.name)"
                :index="c.path || ''"
                :disabled="!c.path"
              >
                <i :class="c.icon || 'el-icon-menu'"></i>
                <span slot="title">{{ c.name }}</span>
              </el-menu-item>
            </template>
          </el-submenu>

          <el-menu-item
            v-else
            :key="'item1-' + (m.id || m.path || m.name)"
            :index="m.path || ''"
            :disabled="!m.path"
          >
            <i :class="m.icon || 'el-icon-menu'"></i>
            <span slot="title">{{ m.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <div class="layout-main">
      <div class="layout-tabs" v-if="tabs.length > 0">
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

      <el-main class="layout-content">
        <nuxt />
      </el-main>

      <el-footer class="layout-footer">
        © 2026 博客管理系统 - 技术支持
      </el-footer>
    </div>
  </el-container>
</template>

<script>
import { pinia } from '~/plugins/pinia'
import { useMenuStore } from '~/store/menu'
import { useUserStore } from '~/store/user'

export default {
  name: 'ManageLayout',
  data() {
    return {
      activeIndex: '/homepage',
      activeTab: '',
      tabs: [],
      menus: [],
      openedMenus: [],
      menuLoading: false,
      menuMap: {
        '/homepage': { title: '首页', name: 'homepage' },
        '/dashboard': { title: '仪表盘', name: 'dashboard' },
        '/usermanage': { title: '用户管理', name: 'usermanage' },
        '/info': { title: '用户信息管理', name: 'info' },
        '/count': { title: '账户管理', name: 'count' },
        '/system': { title: '系统管理', name: 'system' },
        '/role': { title: '角色管理', name: 'role' },
        '/menu': { title: '菜单管理', name: 'menu' },
        '/permission': { title: '权限管理', name: 'permission' },
        '/log': { title: '日志管理', name: 'log' },
        '/blogmanage': { title: '博客管理', name: 'blogmanage' },
        '/audit': { title: '博客审核', name: 'audit' },
        '/label': { title: '标签管理', name: 'label' },
        '/circlemanage': { title: '圈子管理', name: 'circlemanage' },
        // '/circleaudit': { title: '圈子审核', name: 'circleaudit' }
      }
    }
  },
  computed: {
    menuStore() {
      return useMenuStore(pinia)
    },
    userStore() {
      return useUserStore(pinia)
    },
    displayName() {
      const u = this.userStore.getUserInfo
      return u?.username || u?.nickname || '管理员'
    }
  },
  async mounted() {
    this.activeIndex = this.$route.path || '/homepage'
    this.addTab('/homepage')
    await this.fetchMenus()
    this.$nextTick(() => {
      this.expandMenuByRoute(this.$route.path)
    })
  },
  watch: {
    '$route.path'(n) {
      this.activeIndex = n
      this.addTab(n)
      this.expandMenuByRoute(n)
    },
    menus: {
      handler() {
        this.$nextTick(() => {
          this.expandMenuByRoute(this.$route.path)
        })
      },
      deep: true
    }
  },
  methods: {
    handleProfile() {
      this.$router.push('/user');
    },
    hasChildren(m) {
      return Array.isArray(m?.children) && m.children.length > 0
    },
    async fetchMenus() {
      if (this.menuLoading) return
      this.menuLoading = true
      try {
        if (process.client) {
          this.userStore.restorePermissions()
        }
        await this.menuStore.fetchMenus()
        const list = this.menuStore.getFilteredMenus || []
        this.menus = this.normalizeMenus(list)
      } catch (e) {
        console.error('加载菜单失败:', e)
        this.menus = []
      } finally {
        this.menuLoading = false
      }
    },
    // 对菜单进行排序（稳定排序）
    sortMenus(menus) {
      if (!menus || !Array.isArray(menus)) return []
      
      // 使用稳定排序算法，确保相同sortOrder的菜单项顺序一致
      const sortedMenus = menus.sort((a, b) => {
        const orderA = a.sortOrder || a.sort_order || 0
        const orderB = b.sortOrder || b.sort_order || 0
        
        // 如果排序序号相同，按id大小排序确保稳定性
        if (orderA === orderB) {
          return (a.id || 0) - (b.id || 0)
        }
        return orderA - orderB
      })
      
      // 递归排序子菜单
      sortedMenus.forEach(menu => {
        if (menu.children && Array.isArray(menu.children)) {
          menu.children = this.sortMenus(menu.children)
        }
      })
      
      return sortedMenus
    },

    normalizeMenus(a) {
      const b = Array.isArray(a) ? JSON.parse(JSON.stringify(a)) : []
      if (b.length === 0) return []

      const c = b.some(d => Array.isArray(d.children) && d.children.length > 0)
      const s = c ? b : this.buildMenuTree(b)

      const f = g => {
        return g
          .filter(h => h && (!h.type || h.type === 'menu') && h.path !== '/circleaudit')
          .map(h => {
            const i = { ...h }
            i.children = Array.isArray(i.children) ? f(i.children) : []
            return i
          })
      }

      // 对菜单进行排序
      const processedMenus = f(s)
      return this.sortMenus(processedMenus)
    },
    buildMenuTree(a) {
      const b = {}
      const c = []

      a.forEach(d => {
        b[d.id] = { ...d, children: [] }
      })

      a.forEach(d => {
        const e = b[d.id]
        if (!d.parentId || !b[d.parentId]) {
          c.push(e)
          return
        }
        b[d.parentId].children.push(e)
      })

      // 对顶级菜单进行排序
      c.sort((x, y) => {
        const orderX = x.sortOrder || x.sort_order || 0
        const orderY = y.sortOrder || y.sort_order || 0
        return orderX - orderY
      })

      // 将首页菜单项移到最前面
      const homepageIndex = c.findIndex(item => item.path === '/homepage')
      if (homepageIndex > 0) {
        const homepageItem = c.splice(homepageIndex, 1)[0]
        c.unshift(homepageItem)
      }

      return c
    },
    addTab(a) {
      if (!a || !this.menuMap[a]) return
      const b = this.menuMap[a]
      const c = this.tabs.find(d => d.name === b.name)
      if (!c) {
        this.tabs.push({ name: b.name, title: b.title, path: a })
      }
      this.activeTab = b.name
    },
    handleTabClick(a) {
      const b = this.tabs.find(c => c.name === a.name)
      if (b?.path) this.$router.push(b.path)
    },
    handleTabRemove(a) {
      if (this.tabs.length <= 1) {
        this.$message.warning('至少保留一个标签页')
        return
      }
      const b = this.tabs.findIndex(c => c.name === a)
      if (b === -1) return

      this.tabs.splice(b, 1)

      if (this.activeTab === a) {
        const c = this.tabs[b] || this.tabs[b - 1]
        if (c) {
          this.activeTab = c.name
          this.$router.push(c.path)
        }
      }
    },
    expandMenuByRoute(a) {
      const f = (b, c, d = []) => {
        for (const e of b) {
          const g = [...d, String(e.id || e.path || e.name)]
          if (e.path === c) return g.slice(0, -1)
          if (Array.isArray(e.children) && e.children.length > 0) {
            const h = f(e.children, c, g)
            if (h) return h
          }
        }
        return null
      }
      const b = f(this.menus, a)
      this.openedMenus = b ? Array.from(new Set(b)) : []
    },
    handleMenuOpen(a) {
      const b = String(a)
      if (!this.openedMenus.includes(b)) this.openedMenus.push(b)
    },
    handleMenuClose(a) {
      const b = String(a)
      this.openedMenus = this.openedMenus.filter(c => c !== b)
    },
    async handleLogout() {
      this.userStore.logout()
      await this.$router.push('/login')
    }
  }
}
</script>

<style>
html, body, #__nuxt, #__layout {
  height: 100%;
  margin: 0;
  padding: 0;
  background: #f5f7fa;
}
</style>

<style scoped>
.layout-wrap {
  min-height: 100vh;
  background: #f5f7fa;
}

.layout-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1002;
  height: 60px !important;
  padding: 0;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.layout-header-inner {
  height: 60px;
  padding: 0 28px 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.layout-logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 60px;
}

.layout-user {
  display: flex;
  align-items: center;
  height: 60px;
}

.layout-user-link {
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

.layout-aside {
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
  z-index: 1001;
  background: #304156;
  overflow-y: auto;
  overflow-x: hidden;
}

.layout-menu {
  border-right: none;
  min-height: calc(100vh - 60px);
}

.layout-main {
  margin-left: 220px;
  padding-top: 60px;
  min-height: 100vh;
}

.layout-tabs {
  position: fixed;
  top: 60px;
  left: 220px;
  right: 0;
  z-index: 1000;
  background: #fff;
  padding: 0 16px;
  border-bottom: 1px solid #ebeef5;
}

.layout-content {
  margin-top: 41px;
  min-height: calc(100vh - 60px - 41px - 40px);
  padding: 20px;
  background: #f5f7fa;
  box-sizing: border-box;
}

.layout-footer {
  margin-left: 0;
  height: 40px !important;
  line-height: 40px;
  text-align: center;
  color: #909399;
  background: #fff;
  border-top: 1px solid #ebeef5;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-submenu__title) {
  height: 50px;
  line-height: 50px;
}

:deep(.el-tabs__header) {
  margin: 0;
}

:deep(.el-tabs__nav-wrap) {
  padding-top: 4px;
}

:deep(.el-tabs__item) {
  height: 36px;
  line-height: 36px;
}

:deep(.el-main) {
  overflow: visible;
}
</style>