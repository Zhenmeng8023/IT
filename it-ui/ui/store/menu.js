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
    getError: (state) => state.error
  },
  actions: {
    // 获取所有菜单
    async fetchMenus() {
      this.loading = true
      this.error = null
      try {
        const response = await GetAllMenus()
        this.menus = response.data
      } catch (error) {
        this.error = error.message
        console.error('获取菜单列表失败:', error)
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