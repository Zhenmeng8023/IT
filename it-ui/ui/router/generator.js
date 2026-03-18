import { useUserStore } from '@/store/user'
import { useMenuStore } from '@/store/menu'

// 构建菜单树结构
function buildMenuTree(menus) {
  const menuMap = new Map()
  const rootMenus = []
  
  // 首先创建所有菜单的映射
  menus.forEach(menu => {
    menuMap.set(menu.id, { ...menu, children: [] })
  })
  
  // 构建父子关系
  menus.forEach(menu => {
    if (menu.parentId === 0 || !menu.parentId) {
      // 根菜单
      rootMenus.push(menuMap.get(menu.id))
    } else {
      // 子菜单
      const parent = menuMap.get(menu.parentId)
      if (parent) {
        parent.children.push(menuMap.get(menu.id))
      }
    }
  })
  
  // 对菜单进行排序
  const sortMenus = (menuList) => {
    menuList.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    menuList.forEach(menu => {
      if (menu.children && menu.children.length > 0) {
        sortMenus(menu.children)
      }
    })
  }
  
  sortMenus(rootMenus)
  return rootMenus
}

// 生成路由配置
export function generateRoutes(menus) {
  const userStore = useUserStore()
  
  // 构建菜单树
  const menuTree = buildMenuTree(menus)
  
  // 递归生成路由
  const generateRoute = (menu) => {
    // 跳过隐藏的菜单
    if (menu.isHidden) {
      return null
    }
    
    // 跳过非菜单类型
    if (menu.type !== 'menu') {
      return null
    }
    
    // 检查权限
    if (menu.permission?.permissionCode && !userStore.hasPermission(menu.permission.permissionCode)) {
      return null
    }
    
    // 处理组件路径
    let componentPath = menu.component
    if (componentPath === '/') {
      // 默认组件路径
      componentPath = '/Layout.vue'
    } else if (!componentPath) {
      // 没有组件路径时使用默认组件
      componentPath = '/NotFound.vue'
    }
    
    const route = {
      path: menu.path,
      name: menu.name,
      component: () => import(`@/views${componentPath}`),
      meta: {
        title: menu.name,
        icon: menu.icon,
        permission: menu.permission?.permissionCode, // 关联权限代码
        hidden: menu.isHidden
      },
      children: []
    }
    
    // 处理子菜单
    if (menu.children && menu.children.length > 0) {
      route.children = menu.children
        .map(childMenu => generateRoute(childMenu))
        .filter(Boolean) // 过滤掉null值
    }
    
    return route
  }
  
  // 生成路由并过滤掉null值
  const routes = menuTree
    .map(menu => generateRoute(menu))
    .filter(Boolean)
  
  return routes
}

// 更新路由配置
export function updateRoutes(permissions) {
  const menuStore = useMenuStore()
  const menus = menuStore.getMenus // 或使用 menuStore.getUserMenus
  
  // 生成路由
  const routes = generateRoutes(menus)
  
  // 动态添加路由到路由实例
  // 具体实现根据项目的路由库而定（如 Vue Router）
  // 示例：router.addRoutes(routes)
  return routes
}