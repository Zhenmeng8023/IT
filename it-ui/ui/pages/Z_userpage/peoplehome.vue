<template>
  <UserProfilePage mode="self" />
</template>

<script>
import UserProfilePage from './UserProfilePage.vue'

export default {
  name: 'PeopleHomePage',
  layout: 'default',
  components: {
    UserProfilePage
  }
}
</script>
