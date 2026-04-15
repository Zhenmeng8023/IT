<template>
  <ProjectDocList
    :key="docListKey"
    ref="projectDocListRef"
    :project-id="projectId"
    :initial-doc-id="initialDocId"
    :initial-mode="initialMode"
    @count-change="$emit('doc-count-change', $event)"
    @changed="$emit('request-refresh')"
    @primary-changed="$emit('request-refresh')"
  />
</template>

<script>
import ProjectDocList from '../components/ProjectDocList.vue'

export default {
  name: 'ProjectManageDocTab',
  components: {
    ProjectDocList
  },
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    initialDocId: {
      type: [String, Number],
      default: null
    },
    initialMode: {
      type: String,
      default: 'view'
    },
    refreshSeed: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      docListKey: 0
    }
  },
  watch: {
    refreshSeed() {
      this.docListKey += 1
    }
  }
}
</script>
