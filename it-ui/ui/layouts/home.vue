<template>
  <div class="home-layout">
    <main class="home-layout__main">
      <nuxt />
    </main>

    <el-backtop :visibility-height="260" />

    <client-only>
      <AIAssistant />
      <SceneAiDock />
    </client-only>
  </div>
</template>

<script>
export default {
  name: 'HomeLayout'
}
</script>

<style scoped>
.home-layout {
  min-height: 100vh;
  background: var(--it-page-bg);
}

.home-layout__main {
  min-height: 100vh;
}

.el-backtop {
  background: var(--it-accent) !important;
  color: #ffffff !important;
  box-shadow: var(--it-shadow) !important;
}
</style>
