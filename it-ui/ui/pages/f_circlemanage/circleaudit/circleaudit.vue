<template>
  <CircleManageConsole
    page-title="圈子审核"
    page-subtitle="默认聚焦待审核圈子，但成员、帖子和详情管理能力与圈子管理页完全一致。"
    default-lifecycle="pending"
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
  name: 'CircleAuditPage',
  layout: 'manage',
  components: {
    CircleManageConsole
  }
}
</script>
