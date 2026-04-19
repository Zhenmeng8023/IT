<template>
  <article
    class="thread-node"
    :class="{
      'is-child': depth > 0,
      'is-top-level': depth === 0
    }"
    :style="threadIndentStyle"
    :data-depth="depth"
    data-testid="circle-comment-thread"
  >
    <div class="thread-card" :class="{ 'has-children': hasChildren }">
      <el-avatar
        :size="avatarSize"
        :src="comment.avatar"
        class="thread-avatar"
      ></el-avatar>

      <div class="thread-main">
        <div class="thread-meta">
          <div class="thread-heading">
            <span class="thread-author">{{ comment.nickname || '匿名用户' }}</span>
            <span v-if="comment.isPostAuthor" class="thread-badge">楼主</span>
            <span v-if="comment.replyToName" class="thread-reply-tag">
              回复 {{ comment.replyToName }}
            </span>
          </div>
          <span class="thread-time">{{ formatTime(comment.createTime) }}</span>
        </div>

        <div class="thread-text">{{ comment.content || '暂无内容' }}</div>

        <div class="thread-actions">
          <el-button
            type="text"
            size="small"
            class="thread-action-btn"
            data-testid="circle-reply-button"
            @click="$emit('show-reply', comment)"
          >
            <i class="el-icon-chat-line-round"></i>
            <span>回复</span>
          </el-button>
        </div>
      </div>
    </div>

    <div
      v-if="isReplying"
      class="reply-editor"
      :class="{ 'is-nested': depth > 0 }"
    >
      <el-input
        data-testid="circle-reply-input"
        type="textarea"
        :rows="2"
        resize="none"
        :placeholder="replyPlaceholder"
        :value="replyContent"
        @input="$emit('update-reply-content', $event)"
      ></el-input>
      <div class="reply-editor-actions">
        <el-button size="small" @click="$emit('cancel-reply')">取消</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="replySubmitting"
          :disabled="replySubmitDisabled"
          @click="$emit('submit-reply', { topComment: resolvedTopComment, replyTo: comment })"
        >
          提交回复
        </el-button>
      </div>
    </div>

    <div v-if="hasChildren" class="thread-children">
      <CircleCommentThread
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :top-comment="resolvedTopComment"
        :active-reply-target-id="activeReplyTargetId"
        :reply-content="replyContent"
        :reply-submitting="replySubmitting"
        :reply-submit-disabled="replySubmitDisabled"
        :can-submit-comment="canSubmitComment"
        :user-id="userId"
        :format-time="formatTime"
        :depth="depth + 1"
        @show-reply="$emit('show-reply', $event)"
        @cancel-reply="$emit('cancel-reply')"
        @update-reply-content="$emit('update-reply-content', $event)"
        @submit-reply="$emit('submit-reply', $event)"
      />
    </div>
  </article>
</template>

<script>
export default {
  name: 'CircleCommentThread',
  props: {
    comment: {
      type: Object,
      required: true
    },
    topComment: {
      type: Object,
      default: null
    },
    activeReplyTargetId: {
      type: [String, Number],
      default: null
    },
    replyContent: {
      type: String,
      default: ''
    },
    replySubmitting: {
      type: Boolean,
      default: false
    },
    replySubmitDisabled: {
      type: Boolean,
      default: false
    },
    canSubmitComment: {
      type: Boolean,
      default: false
    },
    userId: {
      type: [String, Number],
      default: null
    },
    formatTime: {
      type: Function,
      required: true
    },
    depth: {
      type: Number,
      default: 0
    }
  },
  computed: {
    resolvedTopComment() {
      return this.topComment || this.comment
    },
    hasChildren() {
      return Array.isArray(this.comment.children) && this.comment.children.length > 0
    },
    isReplying() {
      return String(this.activeReplyTargetId || '') === String(this.comment.id || '')
    },
    replyPlaceholder() {
      return `回复 @${this.comment.nickname || '用户'}`
    },
    avatarSize() {
      if (this.depth === 0) {
        return 42
      }

      return this.depth > 2 ? 28 : 34
    },
    threadIndentStyle() {
      const indent = Math.min(this.depth * 20, 40)
      return {
        '--thread-indent': `${indent}px`
      }
    }
  }
}
</script>

<style scoped>
.thread-node {
  position: relative;
}

.thread-node.is-child {
  margin-top: 14px;
  margin-left: var(--thread-indent);
}

.thread-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-solid) 90%, transparent);
  box-shadow: var(--it-shadow-soft);
}

.thread-avatar {
  flex-shrink: 0;
  border: 2px solid color-mix(in srgb, var(--it-accent-soft) 70%, transparent);
}

.thread-main {
  min-width: 0;
}

.thread-meta,
.thread-heading,
.thread-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.thread-meta {
  justify-content: space-between;
  margin-bottom: 10px;
}

.thread-author {
  font-size: 14px;
  font-weight: 700;
  color: var(--it-text);
}

.thread-badge,
.thread-reply-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.thread-badge {
  background: var(--it-accent-soft);
  color: var(--it-accent);
}

.thread-reply-tag {
  background: color-mix(in srgb, var(--it-surface-muted) 80%, transparent);
  color: var(--it-text-muted);
}

.thread-time {
  font-size: 12px;
  color: var(--it-text-subtle);
}

.thread-text {
  color: var(--it-text-muted);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.thread-actions {
  margin-top: 12px;
}

.thread-action-btn {
  padding: 0;
  color: var(--it-text-muted);
  font-weight: 600;
}

.thread-action-btn:hover,
.thread-action-btn:focus {
  color: var(--it-accent);
}

.thread-action-btn i {
  margin-right: 4px;
}

.reply-editor {
  margin-top: 12px;
  margin-left: calc(var(--thread-indent) + 18px);
  padding: 16px;
  border-radius: 16px;
  border: 1px solid var(--it-border);
  background: color-mix(in srgb, var(--it-surface-muted) 82%, transparent);
}

.reply-editor.is-nested {
  margin-left: calc(var(--thread-indent) + 8px);
}

.reply-editor :deep(.el-textarea__inner) {
  min-height: 84px;
  border-radius: 14px !important;
  border-color: var(--it-border);
  background: var(--it-surface-solid);
  color: var(--it-text);
}

.reply-editor :deep(.el-textarea__inner:focus) {
  border-color: var(--it-accent);
  box-shadow: 0 0 0 3px var(--it-accent-soft);
}

.reply-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

.reply-editor-actions .el-button {
  min-width: 88px;
}

.reply-editor-actions .el-button--primary {
  border-color: transparent;
  background: var(--it-primary-gradient);
}

.thread-children {
  position: relative;
  margin-top: 14px;
}

.thread-children::before {
  content: '';
  position: absolute;
  left: 20px;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, var(--it-border), transparent);
}

@media screen and (max-width: 768px) {
  .thread-card {
    padding: 16px;
  }

  .thread-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .thread-node.is-child,
  .reply-editor,
  .reply-editor.is-nested {
    margin-left: 0;
  }

  .thread-children::before {
    display: none;
  }
}
</style>
