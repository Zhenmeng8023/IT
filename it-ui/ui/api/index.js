// 导入axios库
// 注意：在nuxt.js中，我们应该使用$axios实例，而不是直接导入axios
// 但为了在非组件环境中使用，我们仍然导入axios并配置baseURL
import axios from 'axios'
import { getToken } from '@/utils/auth'

// 配置axios的baseURL
axios.defaults.baseURL = 'http://localhost:18080/'

// 添加请求拦截器，确保携带token
axios.interceptors.request.use(
  config => {
    if (getToken()) {
      // let each request carry token
      // ['X-Token'] is a custom headers key
      // please modify it according to the actual situation
      config.headers['X-Token'] = getToken()
    }
    return config
  },
  error => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

/**
 * 用户认证模块
 */

/**
 * 发送验证码
 * @param {Object} data - 包含邮箱或手机号的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SendVerifyCode = (data) => axios.post('/register/send-verify-code', data)

/**
 * 用户注册
 * @param {Object} data - 包含用户名、密码、验证码等注册信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const Register = (data) => axios.post('/register', data)

/**
 * 用户登录
 * @param {Object} data - 包含用户名和密码的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const Login = (data) => axios.post('/login', data)

/**
 * 用户登出
 * @returns {Promise} - 返回axios请求的Promise
 */
export const Logout = () => axios.post('/logout')

/**
 * 刷新令牌
 * @param {Object} data - 包含刷新令牌的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const RefreshToken = (data) => axios.post('/api/token/refresh', data)

/**
 * 发送密码重置验证码
 * @param {Object} data - 包含邮箱或手机号的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SendPasswordResetVerifyCode = (data) => axios.post('/password-reset/send-verify-code', data)

/**
 * 验证密码重置令牌
 * @param {Object} params - 包含令牌的参数对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const VerifyPasswordResetToken = (params) => axios.get('/password-reset/verify-token', { params })

/**
 * 重置密码
 * @param {Object} data - 包含新密码和令牌的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ResetPassword = (data) => axios.post('/password-reset/reset', data)

/**
 * 用户管理模块
 */

/**
 * 创建用户
 * @param {Object} data - 包含用户信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateUser = (data) => axios.post('/api/users', data)

/**
 * 更新用户信息
 * @param {string} id - 用户ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateUser = (id, data) => axios.put(`/api/users/${id}`, data)

/**
 * 删除用户
 * @param {string} id - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteUser = (id) => axios.delete(`/api/users/${id}`)

/**
 * 根据ID获取用户信息
 * @param {string} id - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserById = (id) => axios.get(`/api/users/${id}`)

/**
 * 获取当前登录用户信息
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCurrentUser = () => axios.get('/api/users/current')

/**
 * 更新当前用户信息
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateCurrentUser = (data) => axios.put('/api/users/updatemine', data)

/**
 * 修改密码
 * @param {Object} data - 包含旧密码和新密码的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ChangePassword = (data) => axios.put('/api/users/changepwd', data)

/**
 * 修改邮箱
 * @param {Object} data - 包含新邮箱和验证码的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ChangeEmail = (data) => axios.put('/api/users/changeemail', data)

/**
 * 修改用户名
 * @param {Object} data - 包含新用户名的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ChangeUsername = (data) => axios.put('/api/users/changeusername', data)

/**
 * 验证当前密码
 * @param {Object} data - 包含当前密码的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const VerifyCurrentPassword = (data) => axios.post('/api/users/verify-pwd', data)

/**
 * 绑定第三方账号
 * @param {string} id - 用户ID
 * @param {Object} data - 包含第三方账号信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const BindThirdPartyAccount = (id, data) => axios.post(`/api/users/${id}/bind-third`, data)

/**
 * 获取用户公开信息
 * @param {string} id - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserPublicInfo = (id) => axios.get(`/api/users/${id}/public`)

/**
 * 角色权限模块
 */

/**
 * 获取所有角色
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllRoles = () => axios.get('/api/roles')

/**
 * 根据ID获取角色
 * @param {string} id - 角色ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetRoleById = (id) => axios.get(`/api/roles/${id}`)

/**
 * 创建角色
 * @param {Object} data - 包含角色信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateRole = (data) => axios.post('/api/roles', data)

/**
 * 更新角色
 * @param {string} id - 角色ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateRole = (id, data) => axios.put(`/api/roles/${id}`, data)

/**
 * 删除角色
 * @param {string} id - 角色ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteRole = (id) => axios.delete(`/api/roles/${id}`)

/**
 * 获取所有权限
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllPermissions = () => axios.get('/api/permissions')

/**
 * 根据ID获取权限
 * @param {string} id - 权限ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetPermissionById = (id) => axios.get(`/api/permissions/${id}`)

/**
 * 创建权限
 * @param {Object} data - 包含权限信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreatePermission = (data) => axios.post('/api/permissions', data)

/**
 * 更新权限
 * @param {string} id - 权限ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdatePermission = (id, data) => axios.put(`/api/permissions/${id}`, data)

/**
 * 删除权限
 * @param {string} id - 权限ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeletePermission = (id) => axios.delete(`/api/permissions/${id}`)

/**
 * 为角色分配权限
 * @param {string} roleId - 角色ID
 * @param {Object} data - 包含权限ID列表的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const AssignPermissionsToRole = (roleId, data) => axios.put(`/api/roles/${roleId}/permissions`, data)

/**
 * 获取角色的权限
 * @param {string} roleId - 角色ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetRolePermissions = (roleId) => axios.get(`/api/roles/${roleId}/permissions`)

/**
 * 为用户分配角色
 * @param {string} userId - 用户ID
 * @param {Object} data - 包含角色ID列表的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const AssignRolesToUser = (userId, data) => axios.put(`/api/users/${userId}/assign-roles`, data)

/**
 * 获取用户的角色
 * @param {string} userId - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserRoles = (userId) => axios.get(`/api/users/${userId}/roles`)

/**
 * 获取用户的权限
 * @param {string} userId - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserPermissions = (userId) => axios.get(`/api/users/${userId}/permissions`)

/**
 * 菜单管理模块
 */

/**
 * 获取所有菜单
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllMenus = () => axios.get('/api/menus')

/**
 * 根据ID获取菜单
 * @param {string} id - 菜单ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetMenuById = (id) => axios.get(`/api/menus/${id}`)

/**
 * 创建菜单
 * @param {Object} data - 包含菜单信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateMenu = (data) => axios.post('/api/menus', data)

/**
 * 创建根菜单
 * @param {Object} data - 包含根菜单信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateRootMenu = (data) => axios.post('/api/menus/root', data)

/**
 * 获取子菜单
 * @param {string} parentId - 父菜单ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetSubMenus = (parentId) => axios.get(`/api/menus/${parentId}/children`)

/**
 * 更新菜单
 * @param {string} id - 菜单ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateMenu = (id, data) => axios.put(`/api/menus/${id}`, data)

/**
 * 删除菜单
 * @param {string} id - 菜单ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteMenu = (id) => axios.delete(`/api/menus/${id}`)

/**
 * 分页获取菜单
 * @param {Object} params - 包含分页参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetMenusPage = (params) => axios.get('/api/admin/menus/page', { params })

/**
 * 为角色分配菜单
 * @param {string} roleId - 角色ID
 * @param {Object} data - 包含菜单ID列表的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const AssignMenusToRole = (roleId, data) => axios.put(`/api/roles/${roleId}/assign-menus`, data)

/**
 * 为角色分配按钮权限
 * @param {string} roleId - 角色ID
 * @param {Object} data - 包含按钮权限ID列表的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const AssignButtonsToRole = (roleId, data) => axios.put(`/api/roles/${roleId}/assign-buttons`, data)

/**
 * 博客管理模块
 */

/**
 * 创建博客
 * @param {Object} data - 包含博客信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateBlog = (data) => axios.post('/api/blogs', data)

/**
 * 根据ID获取博客
 * @param {string} id - 博客ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetBlogById = (id) => axios.get(`/api/blogs/${id}`)

/**
 * 获取所有博客（分页）
 * @param {Object} params - 包含分页和筛选参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllBlogs = (params) => axios.get('/api/blogs', { params })

/**
 * 更新博客
 * @param {string} id - 博客ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateBlog = (id, data) => axios.put(`/api/blogs/${id}`, data)

/**
 * 删除博客
 * @param {string} id - 博客ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteBlog = (id) => axios.delete(`/api/blogs/${id}`)

/**
 * 搜索博客
 * @param {Object} params - 包含搜索参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SearchBlogs = (params) => axios.get('/api/blogs/search', { params })

/**
 * 根据作者ID获取博客
 * @param {string} authorId - 作者ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetBlogsByAuthorId = (authorId) => axios.get(`/api/blogs/author/${authorId}`)

/**
 * 根据标签搜索博客
 * @param {Object} params - 包含标签ID的参数对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SearchBlogsByTag = (params) => axios.get('/api/blogs/search/tag', { params })

/**
 * 根据作者搜索博客
 * @param {Object} params - 包含作者信息的参数对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SearchBlogsByAuthor = (params) => axios.get('/api/blogs/search/author', { params })

/**
 * 取消发布博客
 * @param {string} id - 博客ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UnpublishBlog = (id) => axios.put(`/api/blogs/${id}/unpublish`)

/**
 * 重新发布博客
 * @param {string} id - 博客ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const RepublishBlog = (id) => axios.put(`/api/blogs/${id}/republish`)

/**
 * 获取未发布的博客
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUnpublishedBlogs = () => axios.get('/api/blogs/unpublished')

/**
 * 保存博客草稿
 * @param {Object} data - 包含博客草稿信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SaveBlogDraft = (data) => axios.post('/api/blogs/draft', data)

/**
 * 获取博客草稿
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetBlogDrafts = () => axios.get('/api/blogs/draft')

/**
 * 排序博客
 * @param {string} type - 排序类型
 * @param {Object} params - 包含排序参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SortBlogs = (type, params) => axios.get(`/api/blogs/${type}`, { params })

/**
 * 下载博客
 * @param {string} id - 博客ID
 * @param {Object} data - 包含下载参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DownloadBlog = (id, data) => axios.post(`/api/blogs/${id}/download`, data)

/**
 * 博客互动模块
 */

// /**
//  * 点赞博客
//  * @param {string} id - 博客ID
//  * @returns {Promise} - 返回axios请求的Promise
//  */
// export const LikeBlog = (id) => axios.post(`/api/blogs/${id}/like`)

/**
 * 添加点赞
 * @param {Object} data - 包含点赞信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const AddLike = (data) => axios.post('/api/blogs/likes', data)

/**
 * 删除点赞
 * @param {string} id - 点赞ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteLike = (id) => axios.delete(`/api/blogs/likes/${id}`)

/**
 * 根据ID获取点赞
 * @param {string} id - 点赞ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetLikeById = (id) => axios.get(`/api/blogs/likes/${id}`)

/**
 * 检查用户是否点赞
 * @param {string} userId - 用户ID
 * @param {string} targetType - 目标类型
 * @param {string} targetId - 目标ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CheckUserLiked = (userId, targetType, targetId) => axios.get(`/api/blogs/likes/user/${userId}/target/${targetType}/${targetId}`)

/**
 * 获取目标的点赞
 * @param {string} targetType - 目标类型
 * @param {string} targetId - 目标ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetLikesByTarget = (targetType, targetId) => axios.get(`/api/blogs/likes/target/${targetType}/${targetId}`)

/**
 * 获取用户的点赞
 * @param {string} userId - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetLikesByUser = (userId) => axios.get(`/api/blogs/likes/user/${userId}`)

/**
 * 获取所有点赞
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllLikes = () => axios.get('/api/blogs/likes')

/**
 * 收藏博客
 * @param {Object} data - 包含收藏信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CollectBlog = (data) => axios.post('/api/blogs/collects', data)

/**
 * 取消收藏博客
 * @param {string} id - 收藏记录ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CancelCollectBlog = (id) => axios.delete(`/api/blogs/collects/${id}`)

/**
 * 获取所有收藏记录
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllCollects = () => axios.get('/api/blogs/collects')

/**
 * 根据ID获取收藏记录
 * @param {string} id - 收藏记录ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCollectById = (id) => axios.get(`/api/blogs/collects/${id}`)

/**
 * 检查是否已收藏
 * @param {string} userId - 用户ID
 * @param {string} targetType - 目标类型
 * @param {string} targetId - 目标ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const IsCollected = (userId, targetType, targetId) => axios.get(`/api/blogs/collects/user/${userId}/target/${targetType}/${targetId}`)

/**
 * 获取目标的所有收藏记录
 * @param {string} targetType - 目标类型
 * @param {string} targetId - 目标ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCollectsByTarget = (targetType, targetId) => axios.get(`/api/blogs/collects/target/${targetType}/${targetId}`)

/**
 * 获取用户的所有收藏记录
 * @param {string} userId - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCollectsByUser = (userId) => axios.get(`/api/blogs/collects/user/${userId}`)

/**
 * 添加评论
 * @param {Object} data - 包含评论信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const AddComment = (data) => axios.post('/api/blogs/comments', data)

/**
 * 根据ID获取评论
 * @param {string} id - 评论ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCommentById = (id) => axios.get(`/api/blogs/comments/${id}`)

/**
 * 获取帖子的评论
 * @param {string} postId - 帖子ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCommentsByPost = (postId) => axios.get(`/api/blogs/comments/post/${postId}`)

/**
 * 回复评论
 * @param {Object} data - 包含回复信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ReplyComment = (data) => axios.post('/api/blogs/comments/reply', data)

/**
 * 删除评论
 * @param {string} id - 评论ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteComment = (id) => axios.delete(`/api/blogs/comments/${id}`)

/**
 * 获取评论的回复
 * @param {string} commentId - 评论ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCommentReplies = (commentId) => axios.get(`/api/blogs/comments/${commentId}/replies`)

/**
 * 获取所有评论
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllComments = () => axios.get('/api/blogs/comments')

/**
 * 圈子模块
 */

/**
 * 创建圈子
 * @param {Object} data - 包含圈子信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateCircle = (data) => axios.post('/api/circle', data)

/**
 * 更新圈子
 * @param {string} id - 圈子ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateCircle = (id, data) => axios.put(`/api/circle/${id}`, data)

/**
 * 删除圈子
 * @param {string} id - 圈子ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteCircle = (id) => axios.delete(`/api/circle/${id}`)

/**
 * 根据ID获取圈子信息
 * @param {string} id - 圈子ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCircleById = (id) => axios.get(`/api/circle/${id}`)

/**
 * 获取所有圈子
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllCircles = () => axios.get('/api/circle')

/**
 * 根据创建者ID获取圈子
 * @param {string} creatorId - 创建者ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCirclesByCreator = (creatorId) => axios.get(`/api/circle/creator/${creatorId}`)

/**
 * 获取公开圈子
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetPublicCircles = () => axios.get('/api/circle/public')

/**
 * 根据类型获取圈子
 * @param {string} type - 圈子类型
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCirclesByType = (type) => axios.get(`/api/circle/type/${type}`)

/**
 * 根据名称获取圈子
 * @param {string} name - 圈子名称
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCircleByName = (name) => axios.get(`/api/circle/name/${name}`)

/**
 * 检查圈子名称是否存在
 * @param {string} name - 圈子名称
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CheckCircleNameExists = (name) => axios.get(`/api/circle/exists/${name}`)

/**
 * 获取圈子成员
 * @param {string} circleId - 圈子ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetCircleMembers = (circleId) => axios.get(`/api/circle/${circleId}/members`)

/**
 * 加入圈子
 * @param {string} circleId - 圈子ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const JoinCircle = (circleId) => axios.post(`/api/circle/${circleId}/members`)

/**
 * 离开圈子
 * @param {string} circleId - 圈子ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const LeaveCircle = (circleId) => axios.delete(`/api/circle/${circleId}/members`)

/**
 * 设置圈子管理员
 * @param {string} circleId - 圈子ID
 * @param {Object} data - 包含管理员信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const SetCircleAdmins = (circleId, data) => axios.put(`/api/circle/${circleId}/admins`, data)



/**
 * 创建通知
 * @param {Object} data - 包含通知信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateNotification = (data) => axios.post('/api/notifications', data)

/**
 * 获取我的通知
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetMyNotifications = () => axios.get('/api/notifications/my')

/**
 * 获取我的未读通知数量
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetMyUnreadNotificationCount = () => axios.get('/api/notifications/unread-count')

/**
 * 标记通知为已读
 * @param {string} id - 通知ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const MarkNotificationAsRead = (id) => axios.put(`/api/notifications/${id}/read`)

/**
 * 标记所有通知为已读
 * @returns {Promise} - 返回axios请求的Promise
 */
export const MarkAllNotificationsAsRead = () => axios.put('/api/notifications/read-all')

/**
 * 删除通知
 * @param {string} id - 通知ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteNotification = (id) => axios.delete(`/api/notifications/${id}`)

/**
 * 获取用户的通知
 * @param {string} userId - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserNotifications = (userId) => axios.get(`/api/notifications/user/${userId}`)

/**
 * 标签与公共模块
 */

/**
 * 获取所有地区
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllRegions = () => axios.get('/api/common/regions')

/**
 * 根据ID获取地区
 * @param {string} id - 地区ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetRegionById = (id) => axios.get(`/api/common/regions/${id}`)

/**
 * 获取所有标签
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetAllTags = () => axios.get('/api/common/tags')

/**
 * 根据ID获取标签
 * @param {string} id - 标签ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetTagById = (id) => axios.get(`/api/common/tags/${id}`)

/**
 * 获取热门标签
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetHotTags = () => axios.get('/api/common/tags/hot')

/**
 * 分页获取标签
 * @param {Object} params - 包含分页参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetTagsPage = (params) => axios.get('/api/admin/tags/page', { params })

/**
 * 创建标签
 * @param {Object} data - 包含标签信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateTag = (data) => axios.post('/api/admin/tags', data)

/**
 * 更新标签
 * @param {string} id - 标签ID
 * @param {Object} data - 包含更新信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateTag = (id, data) => axios.put(`/api/admin/tags/${id}`, data)

/**
 * 删除标签
 * @param {string} id - 标签ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteTag = (id) => axios.delete(`/api/admin/tags/${id}`)

/**
 * 文件与系统配置模块
 */

/**
 * 上传文件
 * @param {Object} data - 包含文件的FormData对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UploadFile = (data) => axios.post('/api/upload', data)

/**
 * 删除文件
 * @param {string} fileId - 文件ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const DeleteFile = (fileId) => axios.delete(`/api/upload/${fileId}`)

/**
 * 获取系统配置
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetSystemConfig = () => axios.get('/api/config')

/**
 * 更新系统配置
 * @param {Object} data - 包含配置信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const UpdateSystemConfig = (data) => axios.put('/api/config', data)

/**
 * 健康检查
 * @returns {Promise} - 返回axios请求的Promise
 */
export const HealthCheck = () => axios.get('/api/health')

/**
 * 后台管理模块
 */

/**
 * 分页获取用户
 * @param {Object} params - 包含分页和筛选参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUsersPage = (params) => axios.get('/api/admin/users/page', { params })

/**
 * 批量删除用户
 * @param {Object} data - 包含用户ID列表的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const BatchDeleteUsers = (data) => axios.delete('/api/admin/users/batch', { data })

/**
 * 批量更新用户状态
 * @param {Object} data - 包含用户ID列表和状态的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const BatchUpdateUserStatus = (data) => axios.put('/api/admin/users/batch-status', data)

/**
 * 禁言用户
 * @param {string} id - 用户ID
 * @param {Object} data - 包含禁言信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const MuteUser = (id, data) => axios.put(`/api/admin/users/${id}/mute`, data)

/**
 * 获取待审核博客
 * @param {Object} params - 包含分页参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetPendingReviewBlogs = (params) => axios.get('/api/admin/review/blogs/pending', { params })

/**
 * 批准博客
 * @param {string} id - 博客ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ApproveBlog = (id) => axios.put(`/api/admin/review/blogs/${id}/approve`)

/**
 * 拒绝博客
 * @param {string} id - 博客ID
 * @param {Object} data - 包含拒绝原因的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const RejectBlog = (id, data) => axios.put(`/api/admin/review/blogs/${id}/reject`, data)

/**
 * 批量审核博客
 * @param {Object} data - 包含博客ID列表和审核结果的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const BatchReviewBlogs = (data) => axios.put('/api/admin/review/blogs/batch', data)

/**
 * 创建举报
 * @param {Object} data - 包含举报信息的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CreateReport = (data) => axios.post('/api/reports', data)

/**
 * 分页获取举报
 * @param {Object} params - 包含分页参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetReportsPage = (params) => axios.get('/api/admin/reports/page', { params })

/**
 * 处理举报
 * @param {string} id - 举报ID
 * @param {Object} data - 包含处理结果的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const HandleReport = (id, data) => axios.put(`/api/reports/${id}/handle`, data)

/**
 * 分页获取操作日志
 * @param {Object} params - 包含分页和筛选参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetOperationLogsPage = (params) => axios.get('/api/admin/logs/operations/page', { params })

/**
 * 分页获取登录日志
 * @param {Object} params - 包含分页和筛选参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetLoginLogsPage = (params) => axios.get('/api/admin/logs/logins/page', { params })

/**
 * 导出操作日志
 * @param {Object} params - 包含导出参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ExportOperationLogs = (params) => axios.get('/api/admin/logs/operations/export', { params, responseType: 'blob' })

/**
 * 导出登录日志
 * @param {Object} params - 包含导出参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const ExportLoginLogs = (params) => axios.get('/api/admin/logs/logins/export', { params, responseType: 'blob' })

/**
 * 获取系统日志
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetSystemLogs = () => axios.get('/api/logs')

/**
 * 获取用户日志
 * @param {string} userId - 用户ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserLogs = (userId) => axios.get(`/api/logs/user/${userId}`)

/**
 * 获取操作日志详情
 * @param {string} id - 日志ID
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetOperationLogDetail = (id) => axios.get(`/api/admin/logs/operations/${id}`)

/**
 * 清理过期日志
 * @param {Object} data - 包含清理参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const CleanExpiredLogs = (data) => axios.delete('/api/admin/logs/clean', { data })

/**
 * 获取用户增长统计
 * @param {Object} params - 包含统计参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserGrowthStats = (params) => axios.get('/api/admin/stats/users/growth', { params })

/**
 * 获取博客发布统计
 * @param {Object} params - 包含统计参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetBlogPublishStats = (params) => axios.get('/api/admin/stats/blogs/published', { params })

/**
 * 获取系统活动统计
 * @param {Object} params - 包含统计参数的对象
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetSystemActivityStats = (params) => axios.get('/api/admin/stats/system/activity', { params })

/**
 * 获取统计数据
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetStats = () => axios.get('/api/stats')

/**
 * 获取用户注册统计
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetUserRegisterStats = () => axios.get('/api/stats/users')

/**
 * 获取博客统计
 * @returns {Promise} - 返回axios请求的Promise
 */
export const GetBlogStats = () => axios.get('/api/stats/blogs')