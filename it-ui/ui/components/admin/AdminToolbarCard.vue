<template>
  <el-card class="admin-toolbar-card" shadow="never">
    <div class="admin-toolbar-card__inner">
      <slot />
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'AdminToolbarCard'
}
</script>

<style scoped>
.admin-toolbar-card {
  margin-bottom: 18px;
}

.admin-toolbar-card__inner {
  width: 100%;
}
</style>
