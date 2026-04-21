<template>
  <div class="front-nav-shell">
    <el-header class="header front-header">
      <div class="header-content">
        <div class="header-left">
          <button class="collapse-btn" type="button" @click="$emit('toggle-sidebar')">
            <i :class="menuCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>

          <div class="brand-block" @click="$router.push('/')">
            <span class="brand-mark">IT</span>
            <div class="brand-copy">
              <strong>IT Forum</strong>
              <span>{{ brandSubtitle }}</span>
            </div>
          </div>
        </div>

        <div v-if="showSearch" class="search-area">
          <slot name="search"></slot>
        </div>

        <div class="right-actions">
          <slot name="actions"></slot>
        </div>
      </div>
    </el-header>

    <el-container class="main-shell" :class="{ 'is-compact': isCompact }">
      <el-aside
        :width="asideWidth"
        class="asid-content front-sidebar"
        :class="{ 'is-collapsed': menuCollapsed }"
      >
        <el-menu
          :default-active="activeMenu"
          class="module-menu front-menu"
          :collapse="menuCollapsed"
          :collapse-transition="true"
          @select="$emit('menu-select', $event)"
        >
          <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-content" :class="mainClass">
        <slot name="main-prefix"></slot>
        <slot></slot>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'FrontNavShell',
  props: {
    brandSubtitle: {
      type: String,
      default: ''
    },
    showSearch: {
      type: Boolean,
      default: true
    },
    menuItems: {
      type: Array,
      default: () => []
    },
    activeMenu: {
      type: String,
      default: '/'
    },
    asideWidth: {
      type: String,
      default: '228px'
    },
    menuCollapsed: {
      type: Boolean,
      default: false
    },
    isCompact: {
      type: Boolean,
      default: false
    },
    mainClass: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.front-nav-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--it-page-bg);
  color: var(--it-text);
}

.front-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  min-height: var(--it-header-height);
  height: auto;
  padding: 0;
  background: var(--it-header-bg);
  border-bottom: 1px solid var(--it-border);
  box-shadow: var(--it-shadow);
  backdrop-filter: blur(18px);
}

.header-content {
  width: 100%;
  max-width: var(--it-shell-max) !important;
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 520px) auto;
  align-items: center;
  gap: 16px;
  min-height: var(--it-header-height);
  padding: 0 var(--it-shell-padding-x) !important;
  box-sizing: border-box;
}

.header-left {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  border: 1px solid var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  color: var(--it-accent);
  background: var(--it-accent-soft);
}

.brand-block {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--it-primary-gradient);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 15px;
  color: var(--it-text);
}

.brand-copy span {
  font-size: 12px;
  color: var(--it-text-muted);
}

.search-area {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.right-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-left: auto;
  flex-shrink: 0;
  width: auto;
  min-width: fit-content;
}

.right-actions > * {
  flex-shrink: 0;
}

.main-shell {
  flex: 1;
  overflow: hidden;
}

.asid-content.front-sidebar {
  width: auto !important;
  background: var(--it-sidebar-bg);
  border-right: 1px solid var(--it-border);
  overflow: hidden;
}

.module-menu.front-menu {
  border-right: none;
  height: 100%;
  background: transparent !important;
}

.front-menu:not(.el-menu--collapse) {
  width: 200px;
}

.module-menu.front-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  border-radius: var(--it-radius-control);
  color: var(--it-text-muted) !important;
  background: transparent !important;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.module-menu.front-menu :deep(.el-menu-item:hover) {
  background: var(--it-accent-soft) !important;
  color: var(--it-accent) !important;
}

.module-menu.front-menu :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: var(--it-primary-gradient) !important;
  box-shadow: var(--it-shadow);
}

.main-content {
  padding: 24px;
  overflow-y: auto;
  background: transparent;
}

@media screen and (max-width: 1220px) {
  .header-content {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'left actions'
      'search search';
  }

  .header-left {
    grid-area: left;
  }

  .search-area {
    grid-area: search;
    width: 100%;
  }

  .right-actions {
    grid-area: actions;
  }
}

@media screen and (max-width: 960px) {
  .main-shell.is-compact {
    display: block;
  }

  .asid-content.front-sidebar {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid var(--it-border);
  }

  .front-menu:not(.el-menu--collapse) {
    width: 100%;
  }
}

@media screen and (max-width: 768px) {
  .header-content {
    grid-template-columns: 1fr;
    grid-template-areas:
      'left'
      'actions'
      'search';
  }

  .brand-copy span {
    display: none;
  }

  .right-actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .search-area {
    width: 100%;
  }
}
</style>
