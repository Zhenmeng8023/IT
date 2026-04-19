<template>
  <div class="task-checklist-panel">
    <div class="panel-card">
      <div class="top-row">
        <div class="progress-info">
          <div class="title">检查项</div>
          <div class="desc">已完成 {{ checkedCount }} / {{ items.length }}</div>
        </div>
        <el-progress :percentage="progressPercent" :stroke-width="10" />
      </div>

      <div class="create-row">
        <el-input v-model="draft" size="small" placeholder="新增检查项内容" @keyup.enter.native="addItem" />
        <el-button type="primary" size="small" :loading="submitting" @click="addItem">新增</el-button>
      </div>
    </div>

    <div v-if="!items.length" class="panel-card empty-wrap">
      <el-empty description="暂无检查项" />
    </div>

    <div v-else class="item-list">
      <div
        v-for="(item, index) in items"
        :key="item.id"
        class="item-card"
        :class="{
          'is-dragging': dragIndex === index,
          'is-drag-over': dragOverIndex === index && dragIndex !== index
        }"
        @dragover.prevent="handleDragOver(index)"
        @drop.prevent="handleDrop(index)"
      >
        <div class="item-main">
          <div
            class="drag-handle"
            draggable="true"
            title="拖动排序"
            @dragstart="handleDragStart(index, $event)"
            @dragend="handleDragEnd"
          >
            ⋮⋮
          </div>

          <el-checkbox :value="!!item.checked" @change="toggleItem(item)"></el-checkbox>
          <el-input v-model="item.content" size="small" class="content-input" />
        </div>

        <div class="item-actions">
          <el-tag v-if="item.checked" size="mini" type="success">已完成</el-tag>
          <span class="sort-text">拖动左侧把手可排序</span>
          <el-button size="mini" type="primary" plain :loading="savingId === item.id" @click="saveItem(item)">保存</el-button>
          <el-button size="mini" type="danger" plain :loading="deletingId === item.id" @click="removeItem(item)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  createTaskChecklistItem,
  deleteTaskChecklistItem,
  getTaskChecklist,
  sortTaskChecklistItems,
  toggleTaskChecklistItem,
  updateTaskChecklistItem
} from '@/api/project'

export default {
  name: 'TaskChecklist',
  props: {
    taskId: {
      type: Number,
      default: null
    },
    task: {
      type: Object,
      default: function() {
        return null
      }
    }
  },
  data() {
    return {
      items: [],
      draft: '',
      submitting: false,
      sortSaving: false,
      savingId: null,
      deletingId: null,
      dragIndex: -1,
      dragOverIndex: -1
    }
  },
  computed: {
    checkedCount() {
      return this.items.filter(item => !!item.checked).length
    },
    progressPercent() {
      if (!this.items.length) return 0
      return Math.round((this.checkedCount / this.items.length) * 100)
    }
  },
  watch: {
    taskId: {
      immediate: true,
      handler(val) {
        if (val) this.loadChecklist()
        else this.items = []
      }
    }
  },
  methods: {
    normalizeItems(list) {
      return (Array.isArray(list) ? list : []).map((item, index) => ({
        ...item,
        checked: !!(item.checked || item.isChecked),
        content: item.content || '',
        sortOrder: typeof item.sortOrder === 'number' ? item.sortOrder : index + 1
      }))
    },
    async loadChecklist() {
      if (!this.taskId) return
      const res = await getTaskChecklist(this.taskId)
      this.items = this.normalizeItems(res && res.data)
    },
    async addItem() {
      const content = (this.draft || '').trim()
      if (!content) {
        this.$message.warning('请输入检查项内容')
        return
      }
      this.submitting = true
      try {
        await createTaskChecklistItem(this.taskId, { content, sortOrder: this.items.length + 1 })
        this.draft = ''
        await this.loadChecklist()
        this.$emit('changed')
        this.$message.success('新增成功')
      } finally {
        this.submitting = false
      }
    },
    async saveItem(item) {
      const content = (item.content || '').trim()
      if (!content) {
        this.$message.warning('检查项内容不能为空')
        return
      }
      this.savingId = item.id
      try {
        await updateTaskChecklistItem(item.id, { content, sortOrder: item.sortOrder })
        this.$emit('changed')
        this.$message.success('保存成功')
      } finally {
        this.savingId = null
      }
    },
    async toggleItem(item) {
      await toggleTaskChecklistItem(item.id)
      await this.loadChecklist()
      this.$emit('changed')
      this.$message.success('状态已更新')
    },
    async removeItem(item) {
      try {
        await this.$confirm('确认删除该检查项吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      this.deletingId = item.id
      try {
        await deleteTaskChecklistItem(item.id)
        await this.loadChecklist()
        this.$emit('changed')
        this.$message.success('删除成功')
      } finally {
        this.deletingId = null
      }
    },
    handleDragStart(index, event) {
      this.dragIndex = index
      this.dragOverIndex = index
      if (event && event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move'
        event.dataTransfer.setData('text/plain', String(index))
      }
    },
    handleDragOver(index) {
      this.dragOverIndex = index
    },
    async handleDrop(index) {
      if (this.dragIndex === -1 || index === -1 || this.dragIndex === index) {
        this.resetDragState()
        return
      }
      const list = this.items.slice()
      const current = list[this.dragIndex]
      list.splice(this.dragIndex, 1)
      list.splice(index, 0, current)
      this.items = this.resetSortOrder(list)
      this.resetDragState()
      await this.persistSort()
    },
    handleDragEnd() {
      this.resetDragState()
    },
    resetDragState() {
      this.dragIndex = -1
      this.dragOverIndex = -1
    },
    resetSortOrder(list) {
      return list.map((item, index) => ({ ...item, sortOrder: index + 1 }))
    },
    async persistSort() {
      if (this.sortSaving || !this.taskId) return
      this.sortSaving = true
      try {
        await sortTaskChecklistItems(this.taskId, {
          taskId: this.taskId,
          items: this.items.map(item => ({ id: item.id, sortOrder: item.sortOrder }))
        })
        this.$emit('changed')
        this.$message.success('排序已保存')
      } catch (e) {
        await this.loadChecklist()
        throw e
      } finally {
        this.sortSaving = false
      }
    }
  }
}
</script>

<style scoped>
.task-checklist-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-card,
.item-card {
  border: 1px solid var(--it-border);
  border-radius: 14px;
  background: var(--it-panel-bg-strong);
  box-shadow: var(--it-shadow-soft);
  padding: 14px;
}

.top-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 700;
  color: var(--it-text);
}

.desc {
  font-size: 13px;
  color: var(--it-text-subtle);
  margin-top: 2px;
}

.create-row {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-card {
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-surface-elevated) 92%, transparent), var(--it-surface-solid));
  transition: border-color .2s ease, background-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.item-card:hover {
  transform: translateY(-1px);
  border-color: var(--it-border-strong);
}

.item-card.is-dragging {
  opacity: .82;
  border-color: var(--it-accent);
  box-shadow: 0 12px 26px var(--it-accent-soft);
}

.item-card.is-drag-over {
  border-color: color-mix(in srgb, var(--it-success) 24%, var(--it-border));
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-success) 10%, var(--it-surface-elevated)), var(--it-surface-elevated));
}

.item-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drag-handle {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px dashed var(--it-border-strong);
  background: var(--it-surface-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--it-text-subtle);
  font-size: 14px;
  cursor: move;
  user-select: none;
  flex-shrink: 0;
}

.drag-handle:hover {
  color: var(--it-accent);
  border-color: var(--it-accent);
  background: var(--it-accent-soft);
}

.content-input {
  flex: 1;
}

.item-actions {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  align-items: center;
}

.sort-text {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.empty-wrap {
  padding: 24px 14px;
  border-style: dashed;
  background: linear-gradient(135deg, color-mix(in srgb, var(--it-accent) 4%, var(--it-surface-elevated)), var(--it-surface-solid));
}
</style>

