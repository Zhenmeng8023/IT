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
import { pinia } from '@/plugins/pinia'
import { useUserStore } from './user'

function clone(value) {
  return JSON.parse(JSON.stringify(value || []))
}

function sortMenus(menus) {
  if (!menus || !Array.isArray(menus)) return []
  
  // 对当前层级的菜单按sortOrder排序
  const sortedMenus = menus.sort((a, b) => {
    const orderA = a.sortOrder || a.sort_order || 0
    const orderB = b.sortOrder || b.sort_order || 0
    return orderA - orderB
  })
  
  // 递归排序子菜单
  sortedMenus.forEach(menu => {
    if (menu.children && Array.isArray(menu.children)) {
      menu.children = sortMenus(menu.children)
    }
  })
  
  return sortedMenus
}

function buildTree(menuList) {
  const map = {}
  const roots = []

  menuList.forEach(item => {
    map[item.id] = {
      ...item,
      children: []
    }
  })

  menuList.forEach(item => {
    const current = map[item.id]
    if (!item.parentId || !map[item.parentId]) {
      roots.push(current)
      return
    }
    map[item.parentId].children.push(current)
  })

  // 对根菜单进行排序
  return sortMenus(roots)
}

function filterMenus(menus, userStore) {
  return menus
    .filter(menu => {
      if (!menu) {
        return false
      }
      if (menu.type && menu.type !== 'menu') {
        return false
      }
      if (menu.path === '/login' || menu.path === '/registe') {
        return false
      }
      if (menu.permission && menu.permission.permissionCode) {
        return userStore.hasPermission(menu.permission.permissionCode)
      }
      return true
    })
    .map(menu => {
      const current = { ...menu }
      current.children = Array.isArray(current.children)
        ? filterMenus(current.children, userStore)
        : []
      return current
    })
}

export const useMenuStore = defineStore('menu', {
  state: () => ({
    menus: [],
    currentMenu: null,
    userMenus: [],
    loading: false,
    error: null
  }),
  getters: {
    getMenus: state => state.menus,
    getCurrentMenu: state => state.currentMenu,
    getUserMenus: state => state.userMenus,
    getLoading: state => state.loading,
    getError: state => state.error,
    getFilteredMenus: state => {
      const userStore = useUserStore(pinia)
      const source = clone(state.menus)
      const hasNestedChildren = source.some(item => Array.isArray(item.children) && item.children.length > 0)
      const tree = hasNestedChildren ? source : buildTree(source)
      return filterMenus(tree, userStore)
    },
    getFilteredUserMenus: state => {
      const userStore = useUserStore(pinia)
      const source = clone(state.userMenus)
      const hasNestedChildren = source.some(item => Array.isArray(item.children) && item.children.length > 0)
      const tree = hasNestedChildren ? source : buildTree(source)
      return filterMenus(tree, userStore)
    }
  },
  actions: {
    async fetchMenus() {
      this.loading = true
      this.error = null
      try {
        const userStore = useUserStore(pinia)
        const user = userStore.getUserInfo
        const response = user?.id ? await GetUserMenus(user.id) : await GetAllMenus()
        this.menus = Array.isArray(response?.data) ? response.data : []
      } catch (error) {
        this.error = error.message
        console.error('获取菜单列表失败:', error)
        try {
          const response = await GetAllMenus()
          this.menus = Array.isArray(response?.data) ? response.data : []
        } catch (fallbackError) {
          console.error('fallback 获取菜单失败:', fallbackError)
          this.menus = []
        }
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
              if (!Array.isArray(menu.children)) {
                menu.children = []
              }
              menu.children.push(response.data)
              return true
            }
            if (Array.isArray(menu.children) && addChildMenu(menu.children)) {
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
            if (Array.isArray(menus[i].children) && updateMenuInTree(menus[i].children)) {
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
            if (Array.isArray(menus[i].children) && deleteMenuFromTree(menus[i].children)) {
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
        this.userMenus = Array.isArray(response?.data) ? response.data : []
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
    }
  }
})