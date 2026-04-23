<template>
  <div class="kb-sidebar">
    <div class="page-toolbar">
      <div class="page-toolbar__left">
        <el-radio-group
          v-if="showListModeSwitch"
          :value="listMode"
          size="small"
          @input="$emit('update:listMode', $event)"
          @change="$emit('mode-change')"
        >
          <el-radio-button label="owner">我的知识库</el-radio-button>
          <el-radio-button label="project">项目知识库</el-radio-button>
        </el-radio-group>

        <el-input-number
          v-if="showOwnerIdInput && listMode === 'owner'"
          :value="ownerId"
          :min="1"
          controls-position="right"
          size="small"
          @input="$emit('update:ownerId', $event)"
        />

        <el-input-number
          v-else-if="showProjectIdInput"
          :value="projectId"
          :min="1"
          controls-position="right"
          size="small"
          @input="$emit('update:projectId', $event)"
        />

        <el-input
          :value="keyword"
          size="small"
          clearable
          placeholder="搜索知识库名称或描述"
          style="width: 240px"
          @input="$emit('update:keyword', $event)"
        />
      </div>

      <div class="page-toolbar__right">
        <el-button size="small" @click="$emit('refresh')">刷新</el-button>
        <el-button v-if="canCreateKnowledgeBase" type="primary" size="small" @click="$emit('create')">新建知识库</el-button>
      </div>
    </div>

    <div class="kb-sidebar__title">知识库列表</div>

    <div v-loading="loading" class="kb-list">
      <el-empty
        v-if="!knowledgeBases.length"
        description="暂无知识库"
        :image-size="72"
      />

      <div
        v-for="item in knowledgeBases"
        :key="item.id"
        class="kb-list-item"
        :class="{ active: currentKnowledgeBase && currentKnowledgeBase.id === item.id }"
        @click="$emit('select', item)"
      >
        <div class="kb-list-item__top">
          <div class="kb-list-item__name">{{ item.name || `知识库 #${item.id}` }}</div>
          <el-tag size="mini" :type="kbStatusTagType(item.status)">{{ item.status || 'UNKNOWN' }}</el-tag>
        </div>

        <div class="kb-list-item__desc">{{ item.description || '暂无描述' }}</div>

        <div class="kb-list-item__meta">
          <span>ID {{ item.id }}</span>
          <span>{{ item.scopeType || '-' }}</span>
          <span>{{ item.visibility || '-' }}</span>
        </div>

        <div class="kb-list-item__actions">
          <el-button v-if="canEditKnowledgeBaseItem(item)" type="text" size="mini" @click.stop="$emit('edit', item)">编辑</el-button>
          <el-button v-if="canEditKnowledgeBaseItem(item)" type="text" size="mini" @click.stop="$emit('reindex', item)">重建索引</el-button>
        </div>
      </div>
    </div>

    <div class="table-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pagination.page + 1"
        :page-size="pagination.size"
        :total="pagination.total"
        @current-change="$emit('page-change', $event)"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'KnowledgeBaseListPanel',
  props: {
    listMode: {
      type: String,
      default: 'owner'
    },
    ownerId: {
      type: Number,
      default: null
    },
    projectId: {
      type: Number,
      default: null
    },
    keyword: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    knowledgeBases: {
      type: Array,
      default: () => []
    },
    currentKnowledgeBase: {
      type: Object,
      default: null
    },
    pagination: {
      type: Object,
      default: () => ({ page: 0, size: 10, total: 0 })
    },
    canCreateKnowledgeBase: {
      type: Boolean,
      default: false
    },
    canEditKnowledgeBaseItem: {
      type: Function,
      default: () => false
    },
    kbStatusTagType: {
      type: Function,
      default: () => 'info'
    },
    showListModeSwitch: {
      type: Boolean,
      default: true
    },
    showOwnerIdInput: {
      type: Boolean,
      default: true
    },
    showProjectIdInput: {
      type: Boolean,
      default: true
    }
  }
}
</script>

<style scoped>
.page-toolbar,
.page-toolbar__left,
.page-toolbar__right,
.kb-list-item__meta,
.kb-list-item__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}

.kb-sidebar {
  width: 320px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.kb-sidebar__title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.kb-list {
  flex: 1;
  overflow: auto;
  min-height: 0;
}

.kb-list-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.kb-list-item:hover,
.kb-list-item.active {
  border-color: #409eff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.12);
}

.kb-list-item__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kb-list-item__name {
  font-weight: 600;
}

.kb-list-item__desc,
.kb-list-item__meta {
  color: #909399;
  font-size: 12px;
}

.kb-list-item__desc {
  margin: 8px 0;
  line-height: 1.6;
  min-height: 40px;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
