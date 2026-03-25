<template>
  <el-submenu v-if="hasChildren" :index="String(menu.id)">
    <template slot="title">
      <i :class="menu.icon || 'el-icon-menu'"></i>
      <span>{{ menu.name }}</span>
    </template>

    <MenuItem
      v-for="child in menu.children"
      :key="child.id"
      :menu="child"
    />
  </el-submenu>

  <el-menu-item v-else :index="menu.path">
    <i :class="menu.icon || 'el-icon-menu'"></i>
    <span slot="title">{{ menu.name }}</span>
  </el-menu-item>
</template>

<script>
export default {
  name: 'MenuItem',
  props: {
    menu: {
      type: Object,
      required: true
    }
  },
  computed: {
    hasChildren() {
      return Array.isArray(this.menu.children) && this.menu.children.length > 0
    }
  }
}
</script>