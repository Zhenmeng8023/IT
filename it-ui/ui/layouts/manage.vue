<template>
  <el-container class="app">
    <el-header class="header">
      <div class="header-content">
        <span class="header-title">博客管理系统</span>
        <div class="header-right">
          <el-dropdown>
            <span class="el-dropdown-link">
              管理员<i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item>个人中心</el-dropdown-item>
              <el-dropdown-item>修改密码</el-dropdown-item>
              <el-dropdown-item divided>退出登录</el-dropdown-item>
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
          class="el-menu-vertical-demo"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF">
          
          <!-- 首页 -->
          <el-menu-item index="/homepage">
            <i class="el-icon-s-home"></i>
            <span slot="title">首页</span>
          </el-menu-item>
          
          <!-- 动态菜单 -->
          <template v-for="menu in menus">
            <el-submenu v-if="menu.children && menu.children.length > 0" :key="menu.id" :index="menu.path">
              <template slot="title">
                <i :class="menu.icon"></i>
                <span>{{ menu.name }}</span>
              </template>
              <template v-for="childMenu in menu.children">
                <el-menu-item v-if="!childMenu.children || childMenu.children.length === 0" :key="childMenu.id" :index="childMenu.path">
                  <i :class="childMenu.icon"></i>
                  <span>{{ childMenu.name }}</span>
                </el-menu-item>
                <el-submenu v-else :key="childMenu.id" :index="childMenu.path">
                  <template slot="title">
                    <i :class="childMenu.icon"></i>
                    <span>{{ childMenu.name }}</span>
                  </template>
                  <el-menu-item v-for="grandChildMenu in childMenu.children" :key="grandChildMenu.id" :index="grandChildMenu.path">
                    <i :class="grandChildMenu.icon"></i>
                    <span>{{ grandChildMenu.name }}</span>
                  </el-menu-item>
                </el-submenu>
              </template>
            </el-submenu>
            <el-menu-item v-else :key="menu.id" :index="menu.path">
              <i :class="menu.icon"></i>
              <span slot="title">{{ menu.name }}</span>
            </el-menu-item>
          </template>
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

export default {
  data() {
    return {
      activeIndex: '/manage',
      activeTab: '',
      tabs: [],
      menus: [],
      // 菜单项映射关系
      menuMap: {
        '/homepage': { title: '首页', name: 'homepage' },
        '/usermanage': { title: '用户管理', name: 'usermanage' },
        '/count': { title: '账户管理', name: 'count' },
        '/info': { title: '用户信息管理', name: 'info' },
        '/role': { title: '角色管理', name: 'role' },
        '/menu': { title: '菜单管理', name: 'menu' },
        '/permission': { title: '权限管理', name: 'permission' },
        '/log': { title: '日志管理', name: 'log' },
        '/label': { title: '标签管理', name: 'label' },
        '/audit': { title: '博客审核', name: 'audit' },
        '/dashboard': { title: '仪表盘', name: 'dashboard' },
        // '/algoreco': { title: '推荐算法', name: 'algoreco' },
        // '/projectaudit': { title: '项目审核', name: 'projectaudit' },
        // '/projectmiss': { title: '项目下架', name: 'projectmiss' },
        // '/projectalgoreco': { title: '项目推荐算法', name: 'projectalgoreco' },
        // '/circlefriend': { title: '好友管理', name: 'circlefriend' },
        '/circleaudit': { title: '圈子审核', name: 'circleaudit' },
        // '/circlesort': { title: '圈子分类', name: 'circlesort' },
        '/circlemanage': { title: '圈子管理', name: 'circlemanage' },
        // '/circleofficial': { title: '官方圈子详细管理', name: 'circleofficial' }
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
  },
  watch: {
    '$route.path': function(newPath) {
      this.activeIndex = newPath
      this.addTab(newPath)
    },
    // 监听用户权限变化，重新加载菜单
    '$store.state.user.permissions': {
      handler: async function() {
        console.log('权限变化，重新加载菜单')
        await this.fetchMenus()
      },
      deep: true
    }
  },
  methods: {
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
            permissionCode: 'view:admin:user-manage'
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
                permissionCode: 'view:admin:stat-count'
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
            permissionCode: 'view:admin:system-manage'
          },
          children: [
            {
              id: 6,
              path: '/role',
              name: '角色管理',
              icon: 'el-icon-rank',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:role-manage'
              }
            },
            {
              id: 7,
              path: '/menu',
              name: '菜单管理',
              icon: 'el-icon-menu',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:menu-manage'
              }
            },
            {
              id: 8,
              path: '/permission',
              name: '权限管理',
              icon: 'el-icon-lock',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:permission-manage'
              }
            },
            {
              id: 9,
              path: '/log',
              name: '日志管理',
              icon: 'el-icon-document',
              type: 'menu',
              permission: {
                permissionCode: 'view:admin:log-view'
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
            permissionCode: 'view:admin:blog-manage'
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
          this.menus = filteredMenus
        } else {
          // API返回空数据，使用本地完整菜单树
          console.log('API返回空数据，使用本地完整菜单树')
          menuStore.menus = completeMenuTree
          const filteredMenus = menuStore.getFilteredMenus
          console.log('过滤后的菜单:', filteredMenus)
          this.menus = filteredMenus
        }
      } catch (error) {
        console.error('处理菜单失败:', error)
        // 加载失败时使用本地完整菜单树
        console.log('处理失败，使用本地完整菜单树')
        menuStore.menus = completeMenuTree
        const filteredMenus = menuStore.getFilteredMenus
        console.log('过滤后的菜单:', filteredMenus)
        this.menus = filteredMenus
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

/* 头部样式 */
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
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
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
}

/* 侧边栏样式 */
.el-aside {
  background-color: #304156;
  height: calc(100vh - 60px);
  position: fixed;
  left: 0;
  top: 60px;
  z-index: 1000;
  overflow-y: auto;
}

.el-menu {
  border: none;
}

.el-menu-vertical-demo {
  height: 100%;
}

/* 标签页样式 */
.tabs-container {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  margin-left: 220px;
  width: calc(100% - 220px);
  position: fixed;
  top: 60px;
  z-index: 999;
  height: 42px; /* 固定标签页高度 */
}

.custom-tabs {
  margin: 0;
}

.custom-tabs .el-tabs__header {
  margin: 0;
  border-bottom: none;
}

.custom-tabs .el-tabs__item {
  height: 36px;
  line-height: 36px;
  font-size: 14px;
  border: 1px solid #e4e7ed;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  margin-right: 4px;
  background: #f5f7fa;
}

.custom-tabs .el-tabs__item.is-active {
  background: #fff;
  border-bottom-color: #fff;
  color: #409EFF;
}

.custom-tabs .el-tabs__item:hover {
  color: #409EFF;
}

.custom-tabs .el-tabs__nav-wrap::after {
  background-color: #e4e7ed;
}

/* 主内容区域样式 */
.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  min-height: calc(100vh - 102px); /* 减去头部和标签页高度 */
  margin-left: 220px;
  width: calc(100% - 220px);
  margin-top: 102px; /* 头部60px + 标签页42px */
}

/* 底部样式 */
.footer {
  background-color: #f5f7fa;
  color: #909399;
  text-align: center;
  line-height: 40px;
  border-top: 1px solid #e4e7ed;
}

/* 菜单项悬停效果 */
.el-menu-item:hover {
  background-color: #263445 !important;
}

.el-menu-item.is-active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: #fff !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    height: 60px;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    z-index: 1001;
  }
  
  .el-aside {
    width: 180px !important;
    position: fixed;
    left: 0;
    top: 60px;
    z-index: 1000;
  }
  
  .tabs-container {
    margin-left: 180px;
    width: calc(100% - 180px);
    position: fixed;
    top: 60px;
    z-index: 999;
    height: 42px;
  }
  
  .main-content {
    margin-left: 180px;
    width: calc(100% - 180px);
    margin-top: 102px; /* 头部60px + 标签页42px */
    min-height: calc(100vh - 102px);
  }
  
  .header-title {
    font-size: 16px;
  }
}
</style>