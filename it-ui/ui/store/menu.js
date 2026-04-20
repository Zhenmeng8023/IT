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
import { getAdminFallbackMenus, isKnownRoutePath } from '@/utils/permissionConfig'

function cloneMenuTree(menus = []) {
  return menus.map((menu) => ({
    ...menu,
    permission: menu.permission ? { ...menu.permission } : undefined,
    children: Array.isArray(menu.children) ? cloneMenuTree(menu.children) : []
  }))
}

function buildMenuTree(menuList = []) {
  const menuMap = {}
  const rootMenus = []

  menuList.forEach((menu) => {
    menuMap[menu.id] = {
      ...menu,
      children: Array.isArray(menu.children) ? cloneMenuTree(menu.children) : []
    }
  })

  menuList.forEach((menu) => {
    const parentId = menu.parentId
    const isRootMenu = parentId === null || parentId === 0 || parentId === undefined

    if (isRootMenu) {
      rootMenus.push(menuMap[menu.id])
      return
    }

    if (menuMap[parentId]) {
      menuMap[parentId].children.push(menuMap[menu.id])
    }
  })

  return rootMenus
}

function normalizeMenuTree(menus = []) {
  const hasNestedChildren = menus.some(menu => Array.isArray(menu.children) && menu.children.length > 0)
  const hasParentRelation = menus.some(menu => menu.parentId !== null && menu.parentId !== 0 && menu.parentId !== undefined)

  if (!hasNestedChildren && hasParentRelation) {
    return buildMenuTree(menus)
  }

  return menus
}

function filterMenuTree(menus, userStore) {
  return menus.reduce((result, menu) => {
    if (!menu || (menu.type && menu.type !== 'menu')) {
      return result
    }

    if (menu.path === '/login' || menu.path === '/registe') {
      return result
    }

    const children = Array.isArray(menu.children) ? filterMenuTree(menu.children, userStore) : []
    const permissionCode = menu.permission && menu.permission.permissionCode
    const hasSelfPermission = permissionCode ? userStore.hasPermission(permissionCode) : true
    const canOpenRoute = !!menu.path && isKnownRoutePath(menu.path) && hasSelfPermission
    const shouldKeep = children.length > 0 || canOpenRoute

    if (!shouldKeep) {
      return result
    }

    result.push({
      ...menu,
      permission: menu.permission ? { ...menu.permission } : undefined,
      children
    })
    return result
  }, [])
}

export const useMenuStore = defineStore('menu', {
  state: () => ({
    menus: [],
    currentMenu: null,
    userMenus: [],
    loading: false,
    error: null,
    usingFallback: false
  }),
  getters: {
    getMenus: (state) => state.menus,
    getCurrentMenu: (state) => state.currentMenu,
    getUserMenus: (state) => state.userMenus,
    getLoading: (state) => state.loading,
    getError: (state) => state.error,
    getUsingFallback: (state) => state.usingFallback,
    getFilteredMenus: (state) => {
      const userStore = useUserStore()

      const filtered = filterMenuTree(normalizeMenuTree(cloneMenuTree(state.menus)), userStore)
      console.log('过滤后的菜单:', filtered)
      return filtered
    },
    getFilteredUserMenus: (state) => {
      const userStore = useUserStore()

      return filterMenuTree(normalizeMenuTree(cloneMenuTree(state.userMenus)), userStore)
    }
  },
  actions: {
    // 获取所有菜单
    async fetchMenus() {
      this.loading = true
      this.error = null
      try {
        const response = await GetAllMenus()
        const menus = Array.isArray(response && response.data) ? response.data : []

        if (menus.length > 0) {
          this.menus = menus
          this.usingFallback = false
        } else {
          this.menus = getAdminFallbackMenus()
          this.usingFallback = true
          console.warn('菜单接口返回空数据，已切换到本地兜底菜单')
        }
      } catch (error) {
        this.error = error.message
        console.error('获取菜单列表失败:', error)
        this.menus = getAdminFallbackMenus()
        this.usingFallback = true
        console.warn('菜单接口获取失败，已切换到本地兜底菜单')
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
      this.usingFallback = false
    }
  }
})
