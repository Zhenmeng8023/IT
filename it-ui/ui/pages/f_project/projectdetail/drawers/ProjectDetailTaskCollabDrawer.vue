<template>
  <ProjectTaskCollabDrawerBase
    :visible.sync="taskCollabDrawerVisibleProxy"
    :task="selectedTaskForCollab"
    :project-id="projectId"
    :current-user-id="currentUserId"
    :current-member-joined-at="currentMemberRecord && currentMemberRecord.joinedAt ? currentMemberRecord.joinedAt : ''"
    :can-manage-project="canManageProject"
    :active-tab.sync="taskCollabActiveTabProxy"
    :refresh-seed="taskCollabRefreshSeed"
    @changed="handleTaskCollabChanged"
    @status-updated="handleTaskCollabStatusUpdated"
    @tab-change="taskCollabActiveTabProxy = $event || 'overview'"
    @close="handleTaskCollabDrawerClosed"
  />
</template>

<script>
import ProjectTaskCollabDrawerBase from '../../components/ProjectTaskCollabDrawer.vue'

export default {
  name: 'ProjectDetailTaskCollabDrawer',
  components: {
    ProjectTaskCollabDrawerBase
  },
  props: {
    taskCollabDrawerVisible: {
      type: Boolean,
      default: false
    },
    selectedTaskForCollab: {
      type: Object,
      default: null
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    currentUserId: {
      type: [Number, String],
      default: null
    },
    currentMemberRecord: {
      type: Object,
      default: null
    },
    canManageProject: {
      type: Boolean,
      default: false
    },
    taskCollabActiveTab: {
      type: String,
      default: 'overview'
    },
    taskCollabRefreshSeed: {
      type: Number,
      default: 0
    },
    handleTaskCollabChanged: {
      type: Function,
      default: () => {}
    },
    handleTaskCollabStatusUpdated: {
      type: Function,
      default: () => {}
    },
    handleTaskCollabDrawerClosed: {
      type: Function,
      default: () => {}
    }
  },
  computed: {
    taskCollabDrawerVisibleProxy: {
      get() {
        return this.taskCollabDrawerVisible
      },
      set(value) {
        this.$emit('update:taskCollabDrawerVisible', value)
      }
    },
    taskCollabActiveTabProxy: {
      get() {
        return this.taskCollabActiveTab
      },
      set(value) {
        this.$emit('update:taskCollabActiveTab', value)
      }
    }
  }
}
</script>
