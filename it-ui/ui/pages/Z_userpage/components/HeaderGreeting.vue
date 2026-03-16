<!-- 动态问候 -->
<template>
    <div class="greeting">
      <h1>
        Hello <br />
        I'm <span class="name">{{ userName }}</span>, Nice to meet you!
      </h1>
      <p class="current-date">{{ currentDate }}</p>
      <NotificationBell />
    </div>
  </template>
  
  <script>
  import NotificationBell from '@/components/NotificationBell.vue'
  export default {
    components: {
      NotificationBell
    },
    name: 'HeaderGreeting',
    data() {
      return {
        currentDate: '',
        timer: null
      }
    },
    props: {
      userName: {
        type: String,
        default: ''
      }
    },
    mounted() {
      this.updateDate()
      this.timer = setInterval(this.updateDate, 1000 * 60) // 每分钟更新一次
    },
    beforeDestroy() {
      clearInterval(this.timer)
    },
    methods: {
      updateDate() {
        const now = new Date()
        const year = now.getFullYear()
        const month = now.getMonth() + 1
        const day = now.getDate()
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        const weekday = weekdays[now.getDay()]
        this.currentDate = `${year}/${month}/${day} ${weekday}`
      }
    }
  }
  </script>
  
  <style scoped>
  .greeting h1 {
    font-size: 2.5rem;
    font-weight: 400;
    line-height: 1.3;
  }
  .name {
    font-weight: 600;
    color: #ffffff;
  }
  .current-date {
    color: #7f8c8d;
    font-size: 1.1rem;
  }
  </style>