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
import {
  getAdminFallbackMenus,
  getAdminMenuRoutePermission,
  getAdminMenuRouteTitle,
  isKnownAdminMenuGroupPath,
  isKnownAdminRoutePath,
  normalizeAdminMenuPath
} from '@/utils/permissionConfig'

function cloneMenuTree(menus = []) {
  return menus.map((menu) => ({
    ...menu,
    permission: menu.permission ? { ...menu.permission } : undefined,
    children: Array.isArray(menu.children) ? cloneMenuTree(menu.children) : []
  }))
}

function getSortValue(menu) {
  const value = menu?.sortOrder ?? menu?.sort_order ?? menu?.sort ?? 0
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : 0
}

function sortMenuTree(menus = []) {
  return [...menus]
    .sort((left, right) => {
      const sortDiff = getSortValue(left) - getSortValue(right)
      if (sortDiff !== 0) {
        return sortDiff
      }
      return Number(left?.id || 0) - Number(right?.id || 0)
    })
    .map(menu => ({
      ...menu,
      children: Array.isArray(menu.children) ? sortMenuTree(menu.children) : []
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
    const parentId = menu.parentId ?? menu.parent_id
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
  const hasParentRelation = menus.some((menu) => {
    const parentId = menu.parentId ?? menu.parent_id
    return parentId !== null && parentId !== 0 && parentId !== undefined
  })

  const tree = !hasNestedChildren && hasParentRelation ? buildMenuTree(menus) : menus
  return sortMenuTree(tree)
}

function getPermissionCode(menu) {
  return (
    menu?.permission?.permissionCode ||
    menu?.permission?.permission_code ||
    menu?.permissionCode ||
    menu?.permission_code ||
    ''
  )
}

function isHiddenMenu(menu) {
  const value = menu?.isHidden ?? menu?.is_hidden ?? menu?.hidden ?? false
  return value === true || value === 1 || value === '1' || value === 'true'
}

function isButtonLikeMenu(menu) {
  const type = String(menu?.type || menu?.menuType || menu?.menu_type || '').toLowerCase()
  return ['button', 'btn', 'permission'].includes(type)
}

function decorateAdminMenu(menu, normalizedPath) {
  const catalogTitle = getAdminMenuRouteTitle(normalizedPath)

  return {
    ...menu,
    path: normalizedPath || menu.path,
    name: catalogTitle || menu.name,
    permission: menu.permission ? { ...menu.permission } : undefined
  }
}

function filterMenuTree(menus, userStore) {
  return menus.reduce((result, menu) => {
    if (!menu || isHiddenMenu(menu) || isButtonLikeMenu(menu)) {
      return result
    }

    const normalizedPath = menu.path ? normalizeAdminMenuPath(menu.path) : ''
    const children = Array.isArray(menu.children) ? filterMenuTree(menu.children, userStore) : []
    const isRoute = normalizedPath ? isKnownAdminRoutePath(normalizedPath) : false
    const isGroup = normalizedPath ? isKnownAdminMenuGroupPath(normalizedPath) : false
    const catalogPermission = isRoute ? getAdminMenuRoutePermission(normalizedPath) : ''
    const permissionCode = catalogPermission || getPermissionCode(menu)
    const hasSelfPermission = permissionCode ? userStore.hasPermission(permissionCode) : true
    const canOpenRoute = isRoute && hasSelfPermission
    const shouldKeep = children.length > 0 || canOpenRoute || (isGroup && children.length > 0)

    if (!shouldKeep) {
      return result
    }

    result.push({
      ...decorateAdminMenu(menu, normalizedPath),
      children
    })
    return result
  }, [])
}

function findFirstMenuPath(menus = []) {
  for (const menu of menus) {
    if (menu.path && isKnownAdminRoutePath(menu.path)) {
      return menu.path
    }

    const childPath = findFirstMenuPath(menu.children || [])
    if (childPath) {
      return childPath
    }
  }

  return ''
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
      return filterMenuTree(normalizeMenuTree(cloneMenuTree(state.menus)), userStore)
    },
    getFilteredUserMenus: (state) => {
      const userStore = useUserStore()
      return filterMenuTree(normalizeMenuTree(cloneMenuTree(state.userMenus)), userStore)
    },
    getFirstAvailableAdminPath() {
      return findFirstMenuPath(this.getFilteredMenus)
    }
  },
  actions: {
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
          console.warn('菜单接口返回空数据，已切换到本地后台兜底菜单')
        }
      } catch (error) {
        this.error = error.message
        console.error('获取菜单列表失败:', error)
        this.menus = getAdminFallbackMenus()
        this.usingFallback = true
        console.warn('菜单接口获取失败，已切换到本地后台兜底菜单')
      } finally {
        this.loading = false
      }
    },

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

    async createMenu(menuData) {
      this.loading = true
      this.error = null
      try {
        const response = await CreateMenu(menuData)
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

    async updateMenu(id, menuData) {
      this.loading = true
      this.error = null
      try {
        const response = await UpdateMenu(id, menuData)
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

    async deleteMenu(id) {
      this.loading = true
      this.error = null
      try {
        await DeleteMenu(id)
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

    async fetchUserMenus(userId) {
      this.loading = true
      this.error = null
      try {
        const response = await GetUserMenus(userId)
        this.userMenus = Array.isArray(response && response.data) ? response.data : []
        return this.userMenus
      } catch (error) {
        this.error = error.message
        console.error('获取用户菜单失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

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
