<template>
  <div class="login-layout">
    <nuxt />

    <client-only>
      <AIAssistant v-if="showAiLayer" />
      <SceneAiDock v-if="showAiLayer" />
    </client-only>
  </div>
</template>

<script>
export default {
  name: 'LoginLayout',
  computed: {
    showAiLayer() {
      const hiddenRoutes = ['/login', '/registe', '/noPermission']
      return !hiddenRoutes.includes(this.$route.path)
    }
  }
}
</script>

<style scoped>
.login-layout {
  min-height: 100vh;
}
</style>
