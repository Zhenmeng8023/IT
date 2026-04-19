<template>
  <el-container class="app admin-shell" :class="{ 'is-collapsed': sidebarCollapsed }">
    <el-header class="header admin-header">
      <div class="header-content admin-header-content">
        <div class="admin-brand-group">
          <button class="sidebar-toggle" type="button" @click="toggleSidebar">
            <i :class="sidebarCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>
          <div class="brand-mark">IT</div>
          <div class="brand-copy">
            <span class="header-title">管理后台</span>
            <span class="header-subtitle">内容 · 用户 · 项目 · 系统</span>
          </div>
        </div>

        <div class="admin-header-center">
          <div class="admin-context-chip">
            <span class="chip-label">当前页面</span>
            <strong>{{ currentPageTitle }}</strong>
          </div>
        </div>

        <div class="header-right admin-header-right">
          <ThemeToggle />
          <el-dropdown @command="handleDropdownCommand">
            <span class="el-dropdown-link admin-user-pill">
              <span class="user-avatar">管</span>
              <span class="user-copy">
                <strong>管理员</strong>
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
        <div class="admin-aside-head" :class="{ 'is-collapsed': sidebarCollapsed }">
          <div class="aside-title-group" v-if="!sidebarCollapsed">
            <span class="aside-title">导航目录</span>
            <span class="aside-subtitle">快速进入管理模块</span>
          </div>
          <div class="aside-actions" v-if="!sidebarCollapsed">
            <button class="aside-action-btn" type="button" @click="expandAllMenus" title="展开全部">
              <i class="el-icon-s-operation"></i>
            </button>
            <button class="aside-action-btn" type="button" @click="collapseAllMenus" title="收起全部">
              <i class="el-icon-minus"></i>
            </button>
          </div>
        </div>

        <el-menu
          :default-active="$route.path"
          :default-openeds="openedMenus"
          class="el-menu-vertical-demo admin-menu"
          router
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          @open="handleMenuOpen"
          @close="handleMenuClose"
          :unique-opened="false">
          <menu-item
            v-for="menu in menus"
            :key="menu.id"
            :menu="menu"
            :menu-map="menuMap">
          </menu-item>
        </el-menu>
      </el-aside>

      <el-container class="admin-content-shell">
        <div class="tabs-container admin-tabs" v-if="tabs.length > 0" :style="contentOffsetStyle">
          <el-tabs
            v-model="activeTab"
            type="card"
            closable
            @tab-click="handleTabClick"
            @tab-remove="handleTabRemove"
            class="custom-tabs">
            <el-tab-pane
              v-for="tab in tabs"
              :key="tab.name"
              :label="tab.title"
              :name="tab.name">
            </el-tab-pane>
          </el-tabs>
        </div>

        <el-main class="main-content admin-main" :style="mainOffsetStyle">
          <div class="admin-main-inner">
            <nuxt/>
          </div>
        </el-main>

        <el-footer class="footer admin-footer" :style="contentOffsetStyle">
          <span>© 2026 IT 管理后台</span>
          <span>统一主题 · 优雅布局 · 高效运维</span>
        </el-footer>
      </el-container>
    </el-container>
  </el-container>
</template>

<script>
import { useMenuStore } from '~/store/menu'
import { useUserStore } from '~/store/user'

// 递归菜单组件
const MenuItem = {
  name: 'MenuItem',
  props: {
    menu: {
      type: Object,
      required: true
    },
    menuMap: {
      type: Object,
      default: () => ({})
    }
  },
  template: `
    <div>
      <!-- 有子菜单的情况 -->
      <el-submenu v-if="hasChildren" :key="menu.id" :index="String(menu.id)">
        <template slot="title">
          <i :class="menu.icon || 'el-icon-menu'"></i>
          <span>{{ menu.name }}</span>
        </template>
        <menu-item 
          v-for="child in menu.children" 
          :key="child.id" 
          :menu="child"
          :menu-map="menuMap">
        </menu-item>
      </el-submenu>
      
      <!-- 没有子菜单的情况 -->
      <el-menu-item v-else :key="menu.id" :index="menu.path">
        <i :class="menu.icon || 'el-icon-menu'"></i>
        <span slot="title">{{ menu.name }}</span>
      </el-menu-item>
    </div>
  `,
  computed: {
    hasChildren() {
      return this.menu.children && this.menu.children.length > 0
    }
  }
}

import { getToken } from '@/utils/auth';

export default {
  components: {
    MenuItem
  },
  data() {
    return {
      activeIndex: '/manage',
      activeTab: '',
      tabs: [],
      menus: [],
      openedMenus: [], // 存储展开的菜单项
      menuSearchQuery: '', // 菜单搜索关键词
      filteredMenuList: [], // 过滤后的菜单列表
      sidebarCollapsed: false,
      // 菜单项映射关系
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
        '/notificationmanage': { title: '消息通知管理', name: 'notificationmanage' },
        '/blogmanage': { title: '博客管理', name: 'blogmanage' },
        '/audit': { title: '博客审核', name: 'audit' },
        '/label': { title: '标签管理', name: 'label' },
        '/circlemanage': { title: '圈子管理', name: 'circlemanage' },
        '/circleaudit': { title: '圈子审核', name: 'circleaudit' },
        '/projectaudit': { title: '项目审核中心', name: 'projectaudit' },
        '/projectmiss': { title: '项目下架管理', name: 'projectmiss' }
      }
    }
  },
  async mounted() {
    const cachedCollapsed = process.client ? window.localStorage.getItem('adminSidebarCollapsed') : null
    if (cachedCollapsed !== null) {
      this.sidebarCollapsed = cachedCollapsed === '1'
    }
    // 设置默认激活菜单
    this.activeIndex = this.$route.path || '/homepage'
    // 添加首页标签
    this.addTab('/homepage')
    // 获取菜单数据
    await this.fetchMenus()
    // 根据当前路由展开对应的菜单
    this.expandMenuByRoute(this.$route.path)
  },
  computed: {
    sidebarWidth() {
      return this.sidebarCollapsed ? '84px' : '248px'
    },
    currentPageTitle() {
      return (this.menuMap[this.$route.path] && this.menuMap[this.$route.path].title) || '系统概览'
    },
    contentOffsetStyle() {
      return {
        marginLeft: this.sidebarWidth,
        width: `calc(100% - ${this.sidebarWidth})`
      }
    },
    mainOffsetStyle() {
      return {
        marginLeft: this.sidebarWidth,
        width: `calc(100% - ${this.sidebarWidth})`,
        marginTop: this.tabs.length > 0 ? '112px' : '72px'
      }
    }
  },
  watch: {
    '$route.path': function(newPath) {
      this.activeIndex = newPath
      this.addTab(newPath)
      // 路由变化时自动展开对应菜单
      this.expandMenuByRoute(newPath)
    },
    // 监听用户权限变化，重新加载菜单
    '$store.state.user.permissions': {
      handler: async function() {
        console.log('权限变化，重新加载菜单')
        await this.fetchMenus()
        // 重新加载菜单后，根据当前路由展开对应菜单
        this.expandMenuByRoute(this.$route.path)
      },
      deep: true
    },
    // 监听菜单数据变化，重新展开对应菜单
    'menus': {
      handler: function() {
        this.expandMenuByRoute(this.$route.path)
      },
      deep: true
    }
  },
  methods: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      if (process.client) {
        window.localStorage.setItem('adminSidebarCollapsed', this.sidebarCollapsed ? '1' : '0')
      }
    },

    // 处理下拉菜单命令
    async handleDropdownCommand(command) {
      console.log('下拉菜单命令:', command)
      switch (command) {
        case 'profile':
          this.$router.push('/user')
          break
        case 'logout':
          console.log('开始退出登录')
          try {
            const userStore = useUserStore()
            console.log('获取userStore成功')
            await userStore.logout()
            console.log('执行logout成功')
            this.$router.replace('/login')
            console.log('跳转到登录页')
            this.$message({
              message: '退出登录成功',
              type: 'success'
            })
            console.log('退出登录完成')
          } catch (error) {
            console.error('退出登录失败:', error)
            this.$message.error('退出登录失败，请重试')
          }
          break
        default:
          break
      }
    },

    // 展开所有菜单
    expandAllMenus() {
      const getAllMenuPaths = (menus, path = []) => {
        const paths = []
        menus.forEach(menu => {
          if (menu.children && menu.children.length > 0) {
            paths.push(menu.path)
            paths.push(...getAllMenuPaths(menu.children))
          }
        })
        return paths
      }
      
      this.openedMenus = getAllMenuPaths(this.menus)
    },
    
    // 折叠所有菜单
    collapseAllMenus() {
      this.openedMenus = []
    },
    
    // 将扁平化菜单数据转换为树形结构
    buildMenuTree(menuList) {
      const menuMap = {}
      const rootMenus = []
      
      // 首先将所有菜单放入映射表
      menuList.forEach(menu => {
        menuMap[menu.id] = { ...menu, children: [] }
      })
      
      // 构建树形结构
      menuList.forEach(menu => {
        if (menu.parentId === null || menu.parentId === 0) {
          // 根菜单
          rootMenus.push(menuMap[menu.id])
        } else {
          // 子菜单
          if (menuMap[menu.parentId]) {
            menuMap[menu.parentId].children.push(menuMap[menu.id])
          }
        }
      })
      
      return rootMenus
    },
    
    async fetchMenus() {
      const menuStore = useMenuStore()
      const userStore = useUserStore()
      
      console.log('用户登录状态:', userStore.getIsLoggedIn)
      console.log('用户权限:', userStore.getPermissions)
      
      // 从本地存储恢复权限状态
      userStore.restorePermissions()
      console.log('恢复权限后登录状态:', userStore.getIsLoggedIn)
      console.log('恢复权限后权限列表:', userStore.getPermissions)
      
      // 尝试刷新权限，确保获取最新权限
      try {
        await userStore.refreshPermissions()
        console.log('刷新权限后:', userStore.getPermissions)
      } catch (error) {
        console.error('刷新权限失败:', error)
      }
      
      // 强制重新加载菜单数据，确保权限过滤生效
      await menuStore.fetchMenus()
      console.log('重新加载菜单数据:', menuStore.getMenus)
      
      // 定义完整的菜单树，包含所有可能的菜单和权限
      const completeMenuTree = [
        {
          id: 1,
          path: '/dashboard',
          name: '仪表盘',
          icon: 'el-icon-s-home',
          type: 'menu',
          permission: {
            permissionCode: 'view:admin:dashboard'
          },
          children: []
        },
        {
          id: 2,
          path: '/usermanage',
          name: '用户管理',
          icon: 'el-icon-user',
          type: 'menu',
          permission: {
            permissionCode: 'view:admin:user-info'
          },
          children: [
            {
              id: 3,
              path: '/info',
              name: '用户信息管理',
              icon: 'el-icon-user-solid',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:user-info'
              }
            },
            {
              id: 4,
              path: '/count',
              name: '账户管理',
              icon: 'el-icon-s-finance',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:user-count'
              }
            }
          ]
        },
        {
          id: 5,
          path: '/system',
          name: '系统管理',
          icon: 'el-icon-setting',
          type: 'menu',
          permission: {
            permissionCode: 'view:menu'
          },
          children: [
            {
              id: 6,
              path: '/role',
              name: '角色管理',
              icon: 'el-icon-rank',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:user-role'
              }
            },
            {
              id: 7,
              path: '/menu',
              name: '菜单管理',
              icon: 'el-icon-menu',
              type: 'menu',
              permission: {
                permissionCode: 'view:menu'
              }
            },
            {
              id: 8,
              path: '/permission',
              name: '权限管理',
              icon: 'el-icon-lock',
              type: 'menu',
              permission: {
                permissionCode: 'view:permission'
              }
            },
            {
              id: 9,
              path: '/log',
              name: '日志管理',
              icon: 'el-icon-document',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:system-log'
              }
            },
            {
              id: 99,
              path: '/notificationmanage',
              name: '消息通知管理',
              icon: 'el-icon-message-solid',
              type: 'menu',
              permission: {
                permissionCode: 'view:notification'
              }
            }
          ]
        },
        {
          id: 10,
          path: '/blogmanage',
          name: '博客管理',
          icon: 'el-icon-edit',
          type: 'menu',
          permission: {
            permissionCode: 'view:admin:blog-audit'
          },
          children: [
            {
              id: 11,
              path: '/audit',
              name: '博客审核',
              icon: 'el-icon-check',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:blog-audit'
              }
            },
            {
              id: 12,
              path: '/label',
              name: '标签管理',
              icon: 'el-icon-tag',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:label-manage'
              }
            }
          ]
        },
        {
          id: 13,
          path: '/circlemanage',
          name: '圈子管理',
          icon: 'el-icon-chat-dot-round',
          type: 'menu',
          permission: {
            permissionCode: 'view:admin:circle-manage'
          },
          children: [
            {
              id: 14,
              path: '/circleaudit',
              name: '圈子审核',
              icon: 'el-icon-check',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:circle-audit'
              }
            }
          ]
        }
      ]
      
      // 处理菜单数据
      try {
        // 如果API返回了菜单数据，使用API数据
        if (menuStore.getMenus.length > 0) {
          // 获取过滤后的菜单（已根据权限过滤）
          const filteredMenus = menuStore.getFilteredMenus
          console.log('过滤后的菜单:', filteredMenus)
          
          // 检查是否为扁平化数据，如果是则转换为树形结构
          if (filteredMenus.length > 0) {
            // 检查第一个菜单是否有children属性
            const hasChildrenProperty = filteredMenus[0].hasOwnProperty('children')
            const hasChildren = hasChildrenProperty && filteredMenus[0].children && filteredMenus[0].children.length > 0
            
            if (!hasChildrenProperty || !hasChildren) {
              console.log('检测到扁平化菜单数据，开始转换为树形结构')
              this.menus = this.buildMenuTree(filteredMenus)
              console.log('转换后的树形菜单:', this.menus)
            } else {
              this.menus = filteredMenus
            }
          } else {
            this.menus = []
          }
        } else {
          // API返回空数据，显示空菜单
          console.log('API返回空数据，显示空菜单')
          this.menus = []
        }
      } catch (error) {
        console.error('处理菜单失败:', error)
        // 加载失败时显示空菜单
        console.log('处理失败，显示空菜单')
        this.menus = []
      }
      
      console.log('最终显示的菜单:', this.menus)
    },
    
    // 添加标签页
    addTab(path) {
      if (!path || !this.menuMap[path]) return
      
      const tabInfo = this.menuMap[path]
      
      // 检查标签是否已存在
      const existingTab = this.tabs.find(tab => tab.name === tabInfo.name)
      
      if (!existingTab) {
        this.tabs.push({
          name: tabInfo.name,
          title: tabInfo.title,
          path: path
        })
      }
      
      // 激活当前标签
      this.activeTab = tabInfo.name
    },
    
    // 处理标签点击
    handleTabClick(tab) {
      const tabInfo = this.tabs.find(t => t.name === tab.name)
      if (tabInfo && tabInfo.path) {
        this.$router.push(tabInfo.path)
      }
    },
    
    // 处理标签关闭
    handleTabRemove(tabName) {
      // 不能关闭最后一个标签
      if (this.tabs.length <= 1) {
        this.$message.warning('至少保留一个标签页')
        return
      }
      
      const tabIndex = this.tabs.findIndex(tab => tab.name === tabName)
      if (tabIndex > -1) {
        this.tabs.splice(tabIndex, 1)
        
        // 如果关闭的是当前激活的标签，激活前一个标签
        if (this.activeTab === tabName) {
          const newActiveTab = this.tabs[Math.max(0, tabIndex - 1)]
          this.activeTab = newActiveTab.name
          this.$router.push(newActiveTab.path)
        }
      }
    },
    
    // 根据路由路径展开对应的菜单
    expandMenuByRoute(routePath) {
      if (!routePath || routePath === '/homepage') {
        return
      }
      
      const findMenuIds = (menus, targetPath, ids = []) => {
        for (const menu of menus) {
          const currentIds = [...ids, menu.id]
          
          if (menu.path === targetPath) {
            return currentIds.slice(0, -1) // 返回父级菜单ID
          }
          
          if (menu.children && menu.children.length > 0) {
            const result = findMenuIds(menu.children, targetPath, currentIds)
            if (result) return result
          }
        }
        return null
      }
      
      const parentIds = findMenuIds(this.menus, routePath)
      if (parentIds) {
        // 确保父菜单ID都在展开列表中，不影响其他菜单的展开状态
        parentIds.forEach(id => {
          if (!this.openedMenus.includes(id)) {
            this.openedMenus.push(id)
          }
        })
      }
    },
    
    // 菜单展开事件
    handleMenuOpen(index, indexPath) {
      // 检查当前菜单路径是否已在展开列表中
      if (!this.openedMenus.includes(index)) {
        // 添加到展开列表
        this.openedMenus.push(index)
      }
    },
    
    // 菜单折叠事件
    handleMenuClose(index, indexPath) {
      // 从展开列表中移除
      const idx = this.openedMenus.indexOf(index)
      if (idx > -1) {
        this.openedMenus.splice(idx, 1)
      }
    }
  }
}
</script>

<style scoped>
html, body, #__nuxt, #__layout, .app {
  height: 100%;
  margin: 0;
  padding: 0;
}

.admin-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 24%),
    radial-gradient(circle at bottom left, rgba(16, 185, 129, 0.10), transparent 24%),
    linear-gradient(180deg, color-mix(in srgb, var(--it-page-bg) 92%, #010611) 0%, var(--it-page-bg) 100%);
}

.admin-header {
  background: color-mix(in srgb, var(--it-header-bg) 90%, transparent);
  color: var(--it-text);
  box-shadow: var(--it-shadow-strong);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--it-border);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 64px;
  z-index: 1001;
}

.admin-header-content {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 20px;
  align-items: center;
  height: 100%;
  padding: 0 16px 0 14px;
}

.admin-brand-group {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.sidebar-toggle,
.aside-action-btn {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--it-border);
  border-radius: 10px;
  background: var(--it-surface-muted);
  color: var(--it-text-muted);
  cursor: pointer;
  transition: all .2s ease;
}

.sidebar-toggle:hover,
.aside-action-btn:hover {
  color: var(--it-accent);
  border-color: var(--it-border-strong);
  background: var(--it-accent-soft);
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .08em;
  color: #fff;
  background: var(--it-primary-gradient);
  box-shadow: var(--it-button-shadow);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--it-text);
}

.header-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: var(--it-text-subtle);
}

.admin-header-center {
  display: flex;
  justify-content: center;
}

.admin-context-chip {
  min-width: min(420px, 100%);
  max-width: 520px;
  padding: 10px 16px;
  border-radius: 14px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 82%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  text-align: center;
}

.chip-label {
  margin-right: 8px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.admin-context-chip strong {
  color: var(--it-text);
  font-size: 13px;
  font-weight: 600;
}

.admin-header-right {
  gap: 10px;
}

.admin-user-pill {
  min-width: 142px;
  color: var(--it-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 14px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 86%, transparent);
  transition: border-color .2s ease, color .2s ease, background-color .2s ease, transform .2s ease;
}

.admin-user-pill:hover {
  color: var(--it-accent);
  border-color: var(--it-border-strong);
  background: var(--it-accent-soft);
  transform: translateY(-1px);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--it-accent-soft);
  color: var(--it-accent);
  font-size: 13px;
  font-weight: 700;
}

.user-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.user-copy strong {
  font-size: 13px;
  color: var(--it-text);
}

.user-copy small {
  margin-top: 2px;
  font-size: 11px;
  color: var(--it-text-subtle);
}

.admin-aside {
  background: color-mix(in srgb, var(--it-sidebar-bg) 92%, transparent);
  border-right: 1px solid var(--it-border);
  height: calc(100vh - 64px);
  position: fixed;
  left: 0;
  top: 64px;
  z-index: 1000;
  overflow-y: auto;
  overflow-x: hidden;
  backdrop-filter: blur(18px);
  box-shadow: inset -1px 0 0 rgba(255,255,255,0.04);
  transition: width .24s ease;
}

.admin-aside-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 14px 12px;
  border-bottom: 1px solid var(--it-border);
}

.admin-aside-head.is-collapsed {
  justify-content: center;
}

.aside-title-group {
  display: flex;
  flex-direction: column;
}

.aside-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--it-text);
}

.aside-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: var(--it-text-subtle);
}

.aside-actions {
  display: inline-flex;
  gap: 6px;
}

.admin-menu {
  border: none;
  background: transparent !important;
  padding: 10px 10px 24px;
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
  border-radius: 12px;
  color: var(--it-text-muted);
}

.admin-menu /deep/ .el-menu-item i,
.admin-menu /deep/ .el-submenu__title i {
  color: inherit;
}

.admin-menu /deep/ .el-submenu .el-menu-item {
  min-width: 0;
  background: transparent !important;
}

.admin-menu /deep/ .el-menu-item:hover,
.admin-menu /deep/ .el-submenu__title:hover {
  background-color: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.admin-menu /deep/ .el-menu-item.is-active {
  background: var(--it-primary-gradient) !important;
  color: #fff !important;
  box-shadow: var(--it-button-shadow) !important;
}

.admin-menu /deep/ .el-menu-item.is-active i {
  color: #fff !important;
}

.admin-tabs {
  background: color-mix(in srgb, var(--it-header-bg) 88%, transparent);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--it-border);
  padding: 8px 18px 0;
  position: fixed;
  top: 64px;
  z-index: 999;
  height: 48px;
  transition: margin-left .24s ease, width .24s ease;
}

.custom-tabs .el-tabs__header {
  margin: 0;
  border-bottom: none;
}

.custom-tabs .el-tabs__nav-wrap::after {
  background-color: transparent;
}

.custom-tabs /deep/ .el-tabs__item {
  height: 34px;
  line-height: 34px;
  font-size: 13px;
  border: 1px solid var(--it-border);
  border-bottom: none;
  border-radius: 12px 12px 0 0;
  margin-right: 8px;
  background: color-mix(in srgb, var(--it-surface-muted) 86%, transparent);
  color: var(--it-text-muted);
  padding: 0 14px;
}

.custom-tabs /deep/ .el-tabs__item.is-active {
  background: color-mix(in srgb, var(--it-surface-solid) 92%, transparent);
  color: var(--it-accent);
  border-bottom-color: color-mix(in srgb, var(--it-surface-solid) 92%, transparent);
}

.admin-main {
  background-color: transparent;
  padding: 24px 22px 96px;
  min-height: calc(100vh - 112px);
  transition: margin-left .24s ease, width .24s ease, margin-top .24s ease;
}

.admin-main-inner {
  min-height: calc(100vh - 180px);
  width: 100% !important;
  max-width: none !important;
  margin: 0 !important;
  padding: 0;
}

.admin-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 0 18px;
  background: color-mix(in srgb, var(--it-header-bg) 90%, transparent);
  color: var(--it-text-subtle);
  border-top: 1px solid var(--it-border);
  position: fixed;
  bottom: 0;
  z-index: 998;
  transition: margin-left .24s ease, width .24s ease;
}

@media (max-width: 1100px) {
  .admin-header-content {
    grid-template-columns: auto auto;
  }

  .admin-header-center {
    display: none;
  }
}

@media (max-width: 720px) {
  .admin-header-content {
    padding: 0 10px;
  }

  .brand-copy,
  .admin-footer {
    display: none;
  }
}
</style>
