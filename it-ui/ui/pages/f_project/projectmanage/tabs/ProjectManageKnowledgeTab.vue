<template>
  <FrontProjectKnowledgeBaseCenter :fixed-project-id="projectId" :embedded="true" />
</template>

<script>
import FrontProjectKnowledgeBaseCenter from '@/pages/front_ai/project/KnowledgeBaseCenter.vue'

export default {
  name: 'ProjectManageKnowledgeTab',
  components: {
    FrontProjectKnowledgeBaseCenter
  },
  props: {
    projectId: {
      type: [String, Number],
      default: null
    }
  }
}
</script>
