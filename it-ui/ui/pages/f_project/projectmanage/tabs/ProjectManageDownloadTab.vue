<template>
  <ProjectDownloadRecordManage :key="panelKey" :project-id="projectId" />
</template>

<script>
import ProjectDownloadRecordManage from '../components/ProjectDownloadRecordManage.vue'

export default {
  name: 'ProjectManageDownloadTab',
  components: {
    ProjectDownloadRecordManage
  },
  props: {
    projectId: {
      type: [String, Number],
      default: null
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
