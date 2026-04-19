<template>
    <div class="footer-player">
      <span>©timegone</span>
      <span class="now-playing">🎵 Close To You</span>
    </div>
  </template>
  
  <script>
  export default {
    name: 'FooterPlayer'
  }
  </script>
  
  <style scoped>
  .footer-player {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: var(--it-text-subtle);
    font-size: 0.9rem;
  }
  </style>