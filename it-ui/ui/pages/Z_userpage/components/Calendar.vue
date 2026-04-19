<!-- 日历组件 -->
<template>
    <div class="calendar">
      <div class="calendar-header">
        <button @click="prevMonth" >&lt;</button>
        <span>{{ year }}年{{ month }}月</span>
        <button @click="nextMonth" >&gt;</button>
      </div>
      <div class="weekdays">
        <span v-for="(day, index) in weekdays" :key="index">{{ day }}</span>
      </div>
      <div class="days-grid">
        <!-- 填充上个月的空缺 -->
        <span v-for="n in firstDayOfMonth" :key="'empty-' + n" class="day empty"></span>
        <!-- 当前月的日期 -->
        <span 
          v-for="date in daysInMonth" 
          :key="date" 
          class="day"
          :class="{ today: isToday(date) }"
        >
          {{ date }}
        </span>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'Calendar',
    data() {
      const currentDate = new Date()
      return {
        year: currentDate.getFullYear(),
        month: currentDate.getMonth() + 1,
        weekdays: ['一', '二', '三', '四', '五', '六', '日']
      }
    },
    computed: {
      // 计算当前月份的第一天是星期几（周一为0，周日为6）
      firstDayOfMonth() {
        const firstDay = new Date(this.year, this.month - 1, 1).getDay()
        // 将周日（0）转换为6，周一（1）转换为0，以此类推
        return firstDay === 0 ? 6 : firstDay - 1
      },
      daysInMonth() {
        return new Date(this.year, this.month, 0).getDate()
      }
    },
    methods: {
      isToday(date) {
        const today = new Date()
        return date === today.getDate() && 
               this.month === today.getMonth() + 1 && 
               this.year === today.getFullYear()
      },
      prevMonth() {
        if (this.month === 1) {
          this.month = 12
          this.year -= 1
        } else {
          this.month -= 1
        }
      },
      nextMonth() {
        if (this.month === 12) {
          this.month = 1
          this.year += 1
        } else {
          this.month += 1
        }
      }
    }
  }
  </script>
  
  <style scoped>
  .calendar {
    background: var(--it-panel-bg-strong);
    border: 1px solid var(--it-border);
    box-shadow: var(--it-shadow);
    border-radius: 12px;
    padding: 20px;
    color: var(--it-text);
  }
  .calendar-header {
    display: flex;
    justify-content: space-between;
    text-align: center;
    align-items: center;
    color: var(--it-text);
    margin-bottom: 30px;
  }
  .calendar-header button {
    background: var(--it-surface-muted);
    color: var(--it-text);
    border: 1px solid var(--it-border);
    border-radius: 4px;
    padding: 5px 10px;
    cursor: pointer;
    transition: all 0.2s;
  }
  .calendar-header button:hover {
    background: var(--it-accent-soft);
    border-color: var(--it-border-strong);
  }
  .weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    text-align: center;
    font-weight: 600;
    color: var(--it-text-subtle);
    margin-bottom: 10px;
  }
  .days-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 5px;
  }
  .day {
    text-align: center;
    padding: 8px 0;
    border-radius: 50%;
  }
  .day.today {
    background: var(--it-primary-gradient);
    color: #ffffff;
    font-weight: 600;
  }
  .day.empty {
    visibility: hidden;
  }
  .style1 {
    color: var(--it-text);
  }
  </style>