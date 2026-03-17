<template>
  <div class="permission-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>高级权限管理</h1>
      <p>管理系统角色、菜单、权限的完整权限体系</p>
    </div>

    <!-- 标签页导航 -->
    <el-tabs v-model="activeTab" type="border-card" @tab-click="handleTabClick">
      <!-- 角色管理标签页 -->
      <el-tab-pane label="角色管理" name="role">
        <RoleManagement ref="roleManagement" />
      </el-tab-pane>

      <!-- 菜单管理标签页 -->
      <el-tab-pane label="菜单管理" name="menu">
        <MenuManagement ref="menuManagement" />
      </el-tab-pane>

      <!-- 权限管理标签页 -->
      <el-tab-pane label="权限管理" name="permission">
        <PermissionManagement ref="permissionManagement" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import RoleManagement from './components/RoleManagement.vue'
import MenuManagement from './components/MenuManagement.vue'
import PermissionManagement from './components/PermissionManagement.vue'
export default {
  name: 'PermissionManagement',
  layout:"manage",
  components: {
    RoleManagement,
    MenuManagement,
    PermissionManagement,
  },
  data() {
    return {
      activeTab: 'role'
    }
  },
  methods: {
    handleTabClick(tab) {
      // 切换标签页时刷新对应组件的数据
      if (tab.name === 'role' && this.$refs.roleManagement) {
        this.$refs.roleManagement.refreshData()
      } else if (tab.name === 'menu' && this.$refs.menuManagement) {
        this.$refs.menuManagement.refreshData()
      } else if (tab.name === 'permission' && this.$refs.permissionManagement) {
        this.$refs.permissionManagement.refreshData()
      }
    }
  }
}
</script>

<style scoped>
.permission-management {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 14px;
  color: #606266;
}

::v-deep .el-tabs__content {
  padding: 0;
}
</style>