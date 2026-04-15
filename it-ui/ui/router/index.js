import { getPublicRouteSource, getRouteSource } from './route-source'

export const constantRoutes = getPublicRouteSource()
export const routeCatalog = getRouteSource()

export default {
  constantRoutes,
  routeCatalog
}
