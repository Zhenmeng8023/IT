<template>
  <ProjectActivityManagePanel
    :key="panelKey"
    :project-id="projectId"
    :initial-activity-id="initialActivityId"
    :member-options="memberOptions"
    @total-change="$emit('activity-total-change', $event)"
  />
</template>

<script>
import ProjectActivityManagePanel from '../components/ProjectActivityManagePanel.vue'

export default {
  name: 'ProjectManageActivityTab',
  components: {
    ProjectActivityManagePanel
  },
  props: {
    projectId: {
      type: [String, Number],
      default: null
    },
    initialActivityId: {
      type: Number,
      default: null
    },
    memberOptions: {
      type: Array,
      default: () => []
    },
    refreshSeed: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      panelKey: 0
    }
  },
  watch: {
    refreshSeed() {
      this.panelKey += 1
    }
  }
}
</script>
