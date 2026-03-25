import { defineStore } from 'pinia'
import {
  GetAllMenus,
  GetMenuById,
  CreateMenu,
  CreateRootMenu,
  GetSubMenus,
  UpdateMenu,
  DeleteMenu,
  GetMenusPage,
  GetUserMenus
} from '@/api'
import { useUserStore } from './user'

export const useMenuStore = defineStore('menu', {
  state: () => ({
    menus: [],
    currentMenu: null,
    userMenus: [],
    loading: false,
    error: null
  }),
  getters: {
    getMenus: (state) => state.menus,
    getCurrentMenu: (state) => state.currentMenu,
    getUserMenus: (state) => state.userMenus,
    getLoading: (state) => state.loading,
    getError: (state) => state.error,
    getFilteredMenus: (state) => {
      const userStore = useUserStore()
      
      // 递归过滤菜单
      const filterMenus = (menus) => {
        return menus.filter(menu => {
          // 只显示类型为 'menu' 的菜单项，不显示 'button' 类型
          if (menu.type && menu.type !== 'menu') {
            return false
          }
          
          // 过滤掉登录和注册页面，这些是静态路由，不应该出现在侧边栏菜单中
          if (menu.path === '/login' || menu.path === '/registe') {
            return false
          }
          
          // 检查菜单是否需要权限
          if (menu.permission && menu.permission.permissionCode) {
            // 检查用户是否拥有该权限
            const hasPerm = userStore.hasPermission(menu.permission.permissionCode)
            console.log(`检查菜单 ${menu.name} 的权限 ${menu.permission.permissionCode}: ${hasPerm}`)
            if (!hasPerm) {
              return false
            }
          }
          
          // 递归处理子菜单
          if (menu.children && menu.children.length > 0) {
            menu.children = filterMenus(menu.children)
            // 即使子菜单都被过滤掉，只要当前菜单本身有权限，就仍然显示
            // 这样可以确保父菜单不会因为子菜单没有权限而消失
          }
          
          return true
        })
      }
      
      const filtered = filterMenus([...state.menus])
      console.log('过滤后的菜单:', filtered)
      return filtered
    },
    getFilteredUserMenus: (state) => {
      const userStore = useUserStore()
      
      // 递归过滤用户菜单
      const filterMenus = (menus) => {
        return menus.filter(menu => {
          // 只显示类型为 'menu' 的菜单项，不显示 'button' 类型
          if (menu.type && menu.type !== 'menu') {
            return false
          }
          
          // 过滤掉登录和注册页面，这些是静态路由，不应该出现在侧边栏菜单中
          if (menu.path === '/login' || menu.path === '/registe') {
            return false
          }
          
          // 检查菜单是否需要权限
          if (menu.permission && menu.permission.permissionCode) {
            // 检查用户是否拥有该权限
            if (!userStore.hasPermission(menu.permission.permissionCode)) {
              return false
            }
          }
          
          // 递归处理子菜单
          if (menu.children && menu.children.length > 0) {
            menu.children = filterMenus(menu.children)
            // 即使子菜单都被过滤掉，只要当前菜单本身有权限，就仍然显示
            // 这样可以确保父菜单不会因为子菜单没有权限而消失
          }
          
          return true
        })
      }
      
      return filterMenus([...state.userMenus])
    }
  },
  actions: {
    // 获取用户菜单权限
    async fetchMenus() {
      this.loading = true
      this.error = null
      try {
        const userStore = useUserStore()
        const userId = userStore.getUser?.id
        
        if (userId) {
          // 优先获取用户的菜单权限
          const response = await GetUserMenus(userId)
          this.menus = response.data
          console.log('获取用户菜单权限成功:', response.data)
        } else {
          // 如果没有用户ID，获取所有菜单
          const response = await GetAllMenus()
          this.menus = response.data
          console.log('获取所有菜单成功:', response.data)
        }
      } catch (error) {
        this.error = error.message
        console.error('获取菜单列表失败:', error)
        
        // 出错时尝试获取所有菜单作为 fallback
        try {
          const response = await GetAllMenus()
          this.menus = response.data
          console.log(' fallback: 获取所有菜单成功:', response.data)
        } catch (fallbackError) {
          console.error('fallback 获取菜单失败:', fallbackError)
        }
      } finally {
        this.loading = false
      }
    },
    
    // 根据ID获取菜单
    async fetchMenuById(id) {
      this.loading = true
      this.error = null
      try {
        const response = await GetMenuById(id)
        this.currentMenu = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取菜单详情失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 创建根菜单
    async createRootMenu(menuData) {
      this.loading = true
      this.error = null
      try {
        const response = await CreateRootMenu(menuData)
        this.menus.push(response.data)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('创建根菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 创建子菜单
    async createMenu(menuData) {
      this.loading = true
      this.error = null
      try {
        const response = await CreateMenu(menuData)
        // 找到父菜单并添加子菜单
        const addChildMenu = (menus) => {
          for (const menu of menus) {
            if (menu.id === menuData.parentId) {
              if (!menu.children) {
                menu.children = []
              }
              menu.children.push(response.data)
              return true
            }
            if (menu.children && addChildMenu(menu.children)) {
              return true
            }
          }
          return false
        }
        if (!addChildMenu(this.menus)) {
          this.menus.push(response.data)
        }
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('创建菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取子菜单
    async fetchSubMenus(parentId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetSubMenus(parentId)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('获取子菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 更新菜单
    async updateMenu(id, menuData) {
      this.loading = true
      this.error = null
      try {
        const response = await UpdateMenu(id, menuData)
        // 更新菜单树中的对应菜单
        const updateMenuInTree = (menus) => {
          for (let i = 0; i < menus.length; i++) {
            if (menus[i].id === id) {
              menus[i] = response.data
              return true
            }
            if (menus[i].children && updateMenuInTree(menus[i].children)) {
              return true
            }
          }
          return false
        }
        updateMenuInTree(this.menus)
        if (this.currentMenu && this.currentMenu.id === id) {
          this.currentMenu = response.data
        }
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('更新菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 删除菜单
    async deleteMenu(id) {
      this.loading = true
      this.error = null
      try {
        await DeleteMenu(id)
        // 从菜单树中删除对应菜单
        const deleteMenuFromTree = (menus) => {
          for (let i = 0; i < menus.length; i++) {
            if (menus[i].id === id) {
              menus.splice(i, 1)
              return true
            }
            if (menus[i].children && deleteMenuFromTree(menus[i].children)) {
              return true
            }
          }
          return false
        }
        deleteMenuFromTree(this.menus)
        if (this.currentMenu && this.currentMenu.id === id) {
          this.currentMenu = null
        }
      } catch (error) {
        this.error = error.message
        console.error('删除菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 分页获取菜单
    async fetchMenusPage(params) {
      this.loading = true
      this.error = null
      try {
        const response = await GetMenusPage(params)
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('分页获取菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取用户菜单
    async fetchUserMenus(userId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetUserMenus(userId)
        this.userMenus = response.data
        return response.data
      } catch (error) {
        this.error = error.message
        console.error('获取用户菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 重置状态
    resetState() {
      this.menus = []
      this.currentMenu = null
      this.userMenus = []
      this.loading = false
      this.error = null
    }
  }
})