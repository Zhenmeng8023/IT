<template>
  <el-card class="admin-table-card" shadow="never">
    <slot />
    <div v-if="$slots.pagination" class="admin-table-card__pagination">
      <slot name="pagination" />
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'AdminTableCard'
}
</script>

<style scoped>
.admin-table-card {
  margin-bottom: 18px;
}

.admin-table-card__pagination {
  margin-top: 18px;
  text-align: right;
}
</style>
