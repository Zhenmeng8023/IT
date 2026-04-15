import request from '@/utils/request'

export function getCircleCategories(params = {}) {
  return request({
    url: '/circle/category/list',
    method: 'get',
    params
  })
}

export function createCircleCategory(data) {
  return request({
    url: '/circle/category/add',
    method: 'post',
    data
  })
}

export function updateCircleCategory(categoryId, data) {
  return request({
    url: `/circle/category/${categoryId}`,
    method: 'put',
    data
  })
}

export function deleteCircleCategory(categoryId) {
  return request({
    url: `/circle/category/delete/${categoryId}`,
    method: 'delete'
  })
}

export function updateCircleCategorySort(categoryId, data) {
  return request({
    url: `/circle/category/sort/${categoryId}`,
    method: 'post',
    data
  })
}

export function toggleCircleCategoryHot(categoryId, data = {}) {
  return request({
    url: `/circle/category/hot/${categoryId}`,
    method: 'post',
    data
  })
}

export function toggleCircleCategoryEnabled(categoryId, data = {}) {
  return request({
    url: `/circle/category/active/${categoryId}`,
    method: 'post',
    data
  })
}

export function getCirclesByCategory(categoryId, params = {}) {
  return request({
    url: `/circle/category/circles/${categoryId}`,
    method: 'get',
    params
  })
}

export function removeCircleFromCategory(categoryId, circleId) {
  return request({
    url: `/circle/category/remove-circle/${categoryId}`,
    method: 'post',
    data: { circleId }
  })
}
