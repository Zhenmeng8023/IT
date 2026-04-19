<template>
  <div class="heatmap-tracker">
    <div v-if="loading" class="heatmap-loading">
      <div class="loading-grid">
        <span v-for="item in 35" :key="item" class="loading-cell"></span>
      </div>
      <p>正在生成活跃热力图...</p>
    </div>

    <div v-else class="heatmap-content">
      <div class="heatmap-summary">
        <div class="summary-card">
          <span class="summary-value">{{ totalCount }}</span>
          <span class="summary-label">综合活跃分</span>
        </div>
        <div class="summary-card">
          <span class="summary-value">{{ activeDays }}</span>
          <span class="summary-label">活跃天数</span>
        </div>
        <div class="summary-card">
          <span class="summary-value">{{ maxCount }}</span>
          <span class="summary-label">单日最高分</span>
        </div>
      </div>

      <div class="heatmap-visual">
        <div class="visual-copy">
          <span class="visual-kicker">Activity Map</span>
          <h4>最近的综合活跃会在这里逐渐积累</h4>
          <p>项目提交是主信号，博客只在有效发布当天记分，点赞、收藏和日志活跃只做轻量补充。颜色越亮，代表当天整体参与度越高。</p>
        </div>

        <div class="board-shell">
          <div class="weekday-row">
            <span v-for="(label, index) in fullWeekdayLabels" :key="index" class="weekday-chip">
              {{ label }}
            </span>
          </div>

          <div class="heatmap-grid">
            <el-tooltip
              v-for="cell in cells"
              :key="cell.key"
              effect="dark"
              placement="top"
              :content="formatTooltip(cell)"
              :disabled="cell.isFuture"
            >
              <div
                class="heatmap-cell"
                :class="[
                  `level-${getLevel(cell.count)}`,
                  { 'is-today': cell.isToday, 'is-future': cell.isFuture }
                ]"
              >
                <span class="cell-day">{{ cell.day }}</span>
                <span class="cell-count">{{ cell.count || '' }}</span>
              </div>
            </el-tooltip>
          </div>

          <div class="heatmap-footer">
            <span class="range-label">{{ rangeLabel }}</span>
            <div class="legend">
              <span>少</span>
              <i v-for="level in 5" :key="level" class="legend-cell" :class="`level-${level - 1}`"></i>
              <span>多</span>
            </div>
          </div>
        </div>
      </div>

      <p v-if="!totalCount" class="empty-tip">最近 30 天还没有发布记录，继续加油。</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HeatmapTracker',
  props: {
    activityData: {
      type: Array,
      default: () => []
    },
    days: {
      type: Number,
      default: 30
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      fullWeekdayLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    }
  },
  computed: {
    countMap() {
      return this.activityData.reduce((map, item) => {
        const dateKey = item && item.date ? item.date : ''
        const count = Number(item && item.count ? item.count : 0)
        if (dateKey) {
          map[dateKey] = count
        }
        return map
      }, {})
    },
    cells() {
      const today = this.startOfDay(new Date())
      const startDate = this.addDays(today, -(this.days - 1))
      const alignedStartDate = this.addDays(startDate, -this.getWeekdayIndex(startDate))
      const alignedEndDate = this.addDays(today, 6 - this.getWeekdayIndex(today))
      const result = []
      let current = new Date(alignedStartDate)

      while (current.getTime() <= alignedEndDate.getTime()) {
        const key = this.toDateKey(current)
        const isFuture = current.getTime() > today.getTime()
        result.push({
          key,
          date: key,
          day: current.getDate(),
          count: isFuture ? 0 : (this.countMap[key] || 0),
          isToday: key === this.toDateKey(today),
          isFuture
        })
        current = this.addDays(current, 1)
      }

      return result
    },
    totalCount() {
      return this.activityData.reduce((sum, item) => sum + Number(item.count || 0), 0)
    },
    activeDays() {
      return this.activityData.filter(item => Number(item.count || 0) > 0).length
    },
    maxCount() {
      return this.activityData.reduce((max, item) => Math.max(max, Number(item.count || 0)), 0)
    },
    rangeLabel() {
      const endDate = this.startOfDay(new Date())
      const startDate = this.addDays(endDate, -(this.days - 1))
      return `${this.toDateKey(startDate)} 至 ${this.toDateKey(endDate)}`
    }
  },
  methods: {
    startOfDay(date) {
      const target = new Date(date)
      target.setHours(0, 0, 0, 0)
      return target
    },
    addDays(date, amount) {
      const target = new Date(date)
      target.setDate(target.getDate() + amount)
      return this.startOfDay(target)
    },
    getWeekdayIndex(date) {
      const day = date.getDay()
      return day === 0 ? 6 : day - 1
    },
    toDateKey(date) {
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    getLevel(count) {
      if (!count) return 0
      if (this.maxCount <= 1) return 4
      const ratio = count / this.maxCount
      if (ratio >= 0.75) return 4
      if (ratio >= 0.5) return 3
      if (ratio >= 0.25) return 2
      return 1
    },
    formatTooltip(cell) {
      if (cell.isFuture) {
        return `${cell.date} 暂无数据`
      }
      const source = this.activityData.find((item) => item.date === cell.date) || {}
      const breakdown = source.breakdown || {}
      const parts = [
        breakdown.commits ? `提交 ${breakdown.commits}` : '',
        breakdown.blogs ? `有效博客 ${breakdown.blogs}` : '',
        breakdown.posts ? `帖子 ${breakdown.posts}` : '',
        breakdown.likes ? `点赞 ${breakdown.likes}` : '',
        breakdown.collects ? `收藏 ${breakdown.collects}` : '',
        breakdown.logs ? `日志 ${breakdown.logs}` : ''
      ].filter(Boolean)

      const detailText = parts.length ? `，${parts.join(' / ')}` : ''
      return `${cell.date} 综合得分 ${cell.count}${detailText}`
    }
  }
}
</script>

<style scoped>
.heatmap-tracker {
  width: 100%;
}

.heatmap-loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: var(--it-text-subtle);
}

.loading-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.loading-cell {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  background: var(--it-accent-soft);
  animation: pulse 1.4s ease-in-out infinite;
}

.heatmap-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.heatmap-visual {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.visual-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.visual-kicker {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--it-accent);
}

.visual-copy h4 {
  margin: 0;
  font-size: 22px;
  line-height: 1.45;
  color: var(--it-text);
}

.visual-copy p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--it-text-subtle);
}

.board-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border-radius: 8px;
  background: var(--it-surface-muted);
  border: 1px solid var(--it-border);
}

.heatmap-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--it-surface-solid) 82%, var(--it-accent-soft));
  border: 1px solid var(--it-border);
}

.summary-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--it-text);
}

.summary-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.weekday-chip {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  border-radius: 8px;
  background: var(--it-surface-solid);
  border: 1px solid var(--it-border);
  font-size: 12px;
  color: var(--it-text-subtle);
}

.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.heatmap-cell {
  position: relative;
  min-height: 72px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--it-border);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.heatmap-cell:hover {
  transform: translateY(-2px);
  border-color: var(--it-border-strong);
  box-shadow: var(--it-shadow);
}

.heatmap-cell.level-0 {
  background: var(--it-surface-solid);
}

.heatmap-cell.level-1 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 10%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 18%, var(--it-surface-solid)));
}

.heatmap-cell.level-2 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 18%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 30%, var(--it-surface-solid)));
}

.heatmap-cell.level-3 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 30%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 48%, var(--it-surface-solid)));
}

.heatmap-cell.level-4 {
  background: var(--it-primary-gradient);
}
.heatmap-cell.level-4 .cell-day,
.heatmap-cell.level-4 .cell-count {
  color: #ffffff;
}

.heatmap-cell.is-today {
  box-shadow: 0 0 0 2px var(--it-accent-soft);
}

.heatmap-cell.is-future {
  opacity: 0.38;
}

.cell-day {
  font-size: 12px;
  font-weight: 600;
  color: var(--it-text);
}

.cell-count {
  align-self: flex-end;
  font-size: 18px;
  font-weight: 700;
  color: var(--it-text);
}

.heatmap-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  max-width: 100%;
}

.range-label {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.legend {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--it-text-subtle);
}

.legend-cell {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.empty-tip {
  margin: 0;
  font-size: 13px;
  color: var(--it-text-subtle);
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 1;
  }
}

@media screen and (max-width: 768px) {
  .heatmap-summary {
    grid-template-columns: 1fr;
  }

  .weekday-row,
  .heatmap-grid {
    gap: 8px;
  }

  .weekday-chip {
    min-height: 28px;
    font-size: 11px;
  }

  .heatmap-cell {
    min-height: 56px;
    padding: 8px;
    border-radius: 8px;
  }

  .cell-count {
    font-size: 15px;
  }
}

.heatmap-tracker .summary-card {
  min-height: 92px;
}

.heatmap-tracker .heatmap-cell.level-1 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 8%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 16%, var(--it-surface-solid)));
}

.heatmap-tracker .heatmap-cell.level-2 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 14%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 24%, var(--it-surface-solid)));
}

.heatmap-tracker .heatmap-cell.level-3 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 22%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 38%, var(--it-surface-solid)));
}

.heatmap-tracker .heatmap-cell.level-4 {
  background: linear-gradient(180deg, color-mix(in srgb, var(--it-accent) 52%, var(--it-surface-solid)), color-mix(in srgb, var(--it-accent) 78%, black 8%));
}

</style>
