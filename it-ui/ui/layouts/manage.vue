<template>
  <el-container class="app">
    <el-header class="header">
      <div class="header-content">
        <span class="header-title">博客管理系统</span>
        <div class="header-right">
          <ThemeToggle />
          <el-dropdown @command="handleDropdownCommand">
            <span class="el-dropdown-link">
              管理员<i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边栏导航 start-->
      <el-aside width="220px">
        <el-menu
          :default-active="$route.path"
          :default-openeds="openedMenus"
          class="el-menu-vertical-demo"
          router
          @open="handleMenuOpen"
          @close="handleMenuClose"
          :unique-opened="false">
          
          <!-- 动态菜单 - 使用递归组件 -->
          <menu-item 
            v-for="menu in menus" 
            :key="menu.id" 
            :menu="menu"
            :menu-map="menuMap">
          </menu-item>
        </el-menu>
      </el-aside>
      <!-- 侧边栏导航 end -->
      
      <el-container>
        <!-- 标签页区域 -->
        <div class="tabs-container" v-if="tabs.length > 0">
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
        
        <el-main class="main-content">
          <!-- 路由视图 -->
          <nuxt/>
        </el-main>
        
        <el-footer class="footer">
          © 2026 博客管理系统 - 技术支持
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
      <el-submenu v-if="hasChildren" :key="menu.id" :index="menu.id">
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
    // 设置默认激活菜单
    this.activeIndex = this.$route.path || '/homepage'
    // 添加首页标签
    this.addTab('/homepage')
    // 获取菜单数据
    await this.fetchMenus()
    // 根据当前路由展开对应的菜单
    this.expandMenuByRoute(this.$route.path)
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

.header {
  background: var(--it-header-bg);
  color: var(--it-text);
  box-shadow: var(--it-shadow);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--it-border);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 64px;
  z-index: 1001;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--it-text);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-dropdown-link {
  color: var(--it-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: var(--it-radius-control);
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  transition: border-color .2s ease, color .2s ease, background-color .2s ease;
}

.el-dropdown-link:hover {
  color: var(--it-accent);
  border-color: var(--it-border-strong);
  background: var(--it-accent-soft);
}

.el-aside {
  background-color: var(--it-sidebar-bg);
  border-right: 1px solid var(--it-border);
  height: calc(100vh - 64px);
  position: fixed;
  left: 0;
  top: 64px;
  z-index: 1000;
  overflow-y: auto;
  backdrop-filter: blur(18px);
}

.menu-search-container {
  padding: 12px;
  background-color: transparent;
  border-bottom: 1px solid var(--it-border);
}

.menu-search .el-input__inner {
  background-color: var(--it-surface-muted);
  border: 1px solid var(--it-border);
  color: var(--it-text);
}

.menu-search .el-input__inner:focus {
  border-color: var(--it-accent);
}

.menu-search .el-input__prefix {
  color: var(--it-text-subtle);
}

.el-menu {
  border: none;
  background: transparent !important;
}

.el-menu-vertical-demo {
  height: calc(100% - 60px);
}

.tabs-container {
  background: var(--it-header-bg);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid var(--it-border);
  padding: 0 20px;
  margin-left: 220px;
  width: calc(100% - 220px);
  position: fixed;
  top: 64px;
  z-index: 999;
  height: 46px;
}

.custom-tabs {
  margin: 0;
}

.custom-tabs .el-tabs__header {
  margin: 0;
  border-bottom: none;
}

.custom-tabs .el-tabs__item {
  height: 38px;
  line-height: 38px;
  font-size: 14px;
  border: 1px solid var(--it-border);
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  margin-right: 6px;
  background: var(--it-surface-muted);
  color: var(--it-text-muted);
}

.custom-tabs .el-tabs__item.is-active {
  background: var(--it-surface-elevated, var(--it-surface-solid));
  border-bottom-color: var(--it-surface-elevated, var(--it-surface-solid));
  color: var(--it-accent);
}

.custom-tabs .el-tabs__item:hover {
  color: var(--it-accent);
}

.custom-tabs .el-tabs__nav-wrap::after {
  background-color: var(--it-border);
}

.main-content {
  background-color: transparent;
  padding: 20px;
  min-height: calc(100vh - 110px);
  margin-left: 220px;
  width: calc(100% - 220px);
  margin-top: 110px;
}

.footer {
  background-color: var(--it-header-bg);
  color: var(--it-text-subtle);
  text-align: center;
  line-height: 40px;
  border-top: 1px solid var(--it-border);
}

.el-menu-item:hover {
  background-color: var(--it-accent-soft) !important;
}

.el-menu-item.is-active {
  background: var(--it-primary-gradient) !important;
  color: #fff !important;
  box-shadow: var(--it-shadow) !important;
}

.el-submenu .el-menu-item {
  padding-left: 50px !important;
  background-color: transparent !important;
}

.el-submenu .el-submenu .el-menu-item {
  padding-left: 70px !important;
  background-color: transparent !important;
}

.el-submenu .el-menu-item:hover {
  background-color: var(--it-accent-soft) !important;
}
</style>