<template>
  <div>
    <!-- 简单的原生控件栏 -->
    <div class="toolbar">
      <label>
        <input type="checkbox" v-model="isTracking" />
        {{ isTracking ? '🟢 采集中' : '⚪️ 停止采集' }}
      </label>

      <button @click="toggleOverlay">
        {{ showOverlay ? '🔴 隐藏热力图' : '🟡 显示热力图' }}
      </button>

      <label>
        半径:
        <input type="range" v-model.number="heatmapRadius" min="5" max="50" step="1" />
        {{ heatmapRadius }}px
      </label>

      <button @click="clearData">🗑️ 清除数据</button>
    </div>

    <!-- 被跟踪的内容区域 (相对定位，用于放置热力图层) -->
    <div
      ref="trackArea"
      v-heatmap="isTracking"
      class="track-area"
    >
      <!-- 模拟一些可点击的内容，比如卡片 -->
      <div class="card-grid">
        <div class="card" v-for="n in 8" :key="n">
          <h3>卡片 {{ n }}</h3>
          <p>点我试试</p>
        </div>
      </div>

      <!-- 热力图叠加层容器，由 heatmap.js 管理 -->
      <div ref="heatmapContainer" class="heatmap-overlay"></div>
    </div>
  </div>
</template>

<script>
import heatmap from 'heatmap.js'
// import heatmap from 'heatmap.js'

export default {
  name: 'HeatmapTracker',
  data() {
    return {
      isTracking: false,         // 是否采集点击
      showOverlay: false,        // 是否显示热力图
      heatmapRadius: 30,         // 热力点半径
      heatmapInstance: null,     // heatmap.js 实例
      clickData: [],             // 存储采集到的点击坐标
    }
  },
  mounted() {
    // 初始化 heatmap.js 实例
    this.initHeatmap()

    // 监听 vue-heatmapjs 发出的点击事件 (需查阅 vue-heatmapjs 文档，这里假设它会通过 $emit 发送)
    // 如果 vue-heatmapjs 不支持事件，则需要改用其他方式采集（例如手动监听 click），但这里先按常见情况处理
    // 假设 vue-heatmapjs 指令内部会通过事件总线或直接调用回调，这里我们使用 $on 监听自定义事件 'heatmap-point'
    this.$on('heatmap-point', (point) => {
      this.clickData.push({
        x: point.x,
        y: point.y,
        value: 1   // 每次点击强度为1
      })
      // 如果当前显示热力图，则实时更新
      if (this.showOverlay) {
        this.updateHeatmap()
      }
    })
  },
  methods: {
    // 初始化 heatmap.js
    initHeatmap() {
      this.heatmapInstance = heatmap.create({
        container: this.$refs.heatmapContainer,
        radius: this.heatmapRadius,
        maxOpacity: 0.8,
        minOpacity: 0.1,
        blur: 0.85
      })
    },

    // 切换热力图显示/隐藏
    toggleOverlay() {
      this.showOverlay = !this.showOverlay
      if (this.showOverlay) {
        this.updateHeatmap()
      } else {
        // 清空画布（设置空数据）
        this.heatmapInstance.setData({ max: 0, data: [] })
      }
    },

    // 更新热力图数据
    updateHeatmap() {
      if (!this.heatmapInstance) return
      // 计算最大强度（这里简单取点击次数，实际可考虑加权）
      const max = this.clickData.length || 1
      this.heatmapInstance.setData({
        max: max,
        data: this.clickData.map(p => ({
          x: p.x,
          y: p.y,
          value: p.value
        }))
      })
      // 应用当前半径
      this.heatmapInstance.configure({ radius: this.heatmapRadius })
    },

    // 清除所有数据
    clearData() {
      this.clickData = []
      if (this.showOverlay) {
        this.heatmapInstance.setData({ max: 0, data: [] })
      }
    }
  },
  watch: {
    // 半径变化时实时调整热力图显示（如果当前显示的话）
    heatmapRadius(newVal) {
      if (this.showOverlay && this.heatmapInstance) {
        this.heatmapInstance.configure({ radius: newVal })
      }
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 20px;
  align-items: center;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.track-area {
  position: relative;
  min-height: 500px;
  border: 1px dashed #ccc;
  padding: 10px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.heatmap-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none; /* 不干扰点击事件 */
}
</style>