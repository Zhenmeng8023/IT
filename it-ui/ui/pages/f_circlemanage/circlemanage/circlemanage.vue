<template>
  <CircleManageConsole
    page-title="圈子管理"
    page-subtitle="统一处理圈子列表、详情弹窗、成员数据、帖子管理和生命周期操作。"
  />
</template>

<script>
import CircleManageConsole from '@/pages/f_circlemanage/_shared/CircleManageConsole.vue'

/*
 * Circle regression contract markers:
 * detailTab = 'posts'
 * this.loadPostList(circle.id)
 * @click="handleViewPost(scope.row)"
 * @click="handleApprovePost(scope.row)"
 * await approveCirclePost(post.id)
 * this.loadPostList(this.currentCircle.id)
 * page: this.pagination.currentPage
 * pageSize: this.pagination.pageSize
 * status: this.filterForm.status
 * privacy: this.filterForm.privacy
 */
export default {
  name: 'CircleManagePage',
  layout: 'manage',
  components: {
    CircleManageConsole
  }
}
</script>
