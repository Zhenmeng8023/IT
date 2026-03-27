<template>
  <div v-if="visible" class="scene-ai-dock">
    <el-card shadow="never" class="scene-ai-card">
      <div class="scene-ai-card__header">
        <div>
          <div class="scene-ai-card__title">{{ title }}</div>
          <div class="scene-ai-card__desc">{{ desc }}</div>
        </div>
        <el-button type="text" icon="el-icon-close" @click="closed = true" />
      </div>

      <el-input
        v-model.trim="contextText"
        type="textarea"
        :rows="5"
        resize="none"
        :placeholder="placeholder"
      />

      <div class="scene-ai-card__actions">
        <el-button
          v-for="item in actions"
          :key="item.key"
          size="mini"
          type="primary"
          plain
          :loading="loadingKey === item.key"
          @click="handleAction(item.key)"
        >
          {{ item.label }}
        </el-button>
      </div>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="760px">
      <el-input v-model="resultText" type="textarea" :rows="18" resize="none" />
      <div slot="footer">
        <el-button @click="copyResult">复制结果</el-button>
        <el-button
          v-if="scene === 'blog'"
          type="warning"
          @click="applyToPage"
        >
          尝试写回页面
        </el-button>
        <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  aiSummarizeProject,
  aiSplitProjectTasks,
  aiPolishBlog,
  aiGenerateBlogSummary
} from '@/api/aiAssistant'

export default {
  name: 'SceneAiDock',
  data() {
    return {
      closed: false,
      contextText: '',
      loadingKey: '',
      dialogVisible: false,
      dialogTitle: 'AI 结果',
      resultText: ''
    }
  },
  computed: {
    userId() {
      try {
        const raw = localStorage.getItem('userInfo')
        if (!raw) return 1
        const user = JSON.parse(raw)
        return user?.id || user?.userId || 1
      } catch (e) {
        return 1
      }
    },
    scene() {
      const path = this.$route.path || ''
      if (path.includes('/projectdetail')) return 'project'
      if (path.includes('/blogwrite')) return 'blog'
      return ''
    },
    visible() {
      return !!this.scene && !this.closed
    },
    title() {
      return this.scene === 'blog' ? 'AI 写作助手' : 'AI 项目助手'
    },
    desc() {
      return this.scene === 'blog'
        ? '直接在写作场景里调用 AI 润色和摘要'
        : '直接在项目场景里调用 AI 总结和任务拆解'
    },
    placeholder() {
      return this.scene === 'blog'
        ? '可补充博客上下文，或直接粘贴需要润色的内容'
        : '可补充项目上下文，或直接粘贴需要总结的项目内容'
    },
    actions() {
      if (this.scene === 'blog') {
        return [
          { key: 'polish', label: 'AI 润色' },
          { key: 'summary', label: 'AI 生成摘要' }
        ]
      }
      return [
        { key: 'project-summary', label: 'AI 总结项目' },
        { key: 'project-tasks', label: 'AI 拆任务' }
      ]
    }
  },
  watch: {
    '$route.path'() {
      this.closed = false
      this.contextText = ''
    }
  },
  methods: {
    async handleAction(key) {
      if (!this.contextText) {
        this.$message.warning('请先输入一些上下文内容')
        return
      }

      this.loadingKey = key
      try {
        let result = ''

        if (key === 'project-summary') {
          result = await aiSummarizeProject({
            userId: this.userId,
            modelId: 1,
            title: document.title || '项目详情',
            content: this.contextText
          })
          this.dialogTitle = 'AI 总结项目'
        }

        if (key === 'project-tasks') {
          result = await aiSplitProjectTasks({
            userId: this.userId,
            modelId: 1,
            title: document.title || '项目详情',
            content: this.contextText
          })
          this.dialogTitle = 'AI 拆任务'
        }

        if (key === 'polish') {
          result = await aiPolishBlog({
            userId: this.userId,
            modelId: 1,
            title: document.title || '博客编辑',
            content: this.contextText
          })
          this.dialogTitle = 'AI 润色结果'
        }

        if (key === 'summary') {
          result = await aiGenerateBlogSummary({
            userId: this.userId,
            modelId: 1,
            title: document.title || '博客编辑',
            content: this.contextText
          })
          this.dialogTitle = 'AI 摘要与标签'
        }

        this.resultText = result || '未返回内容'
        this.dialogVisible = true
      } catch (e) {
        this.$message.error('AI 请求失败')
      } finally {
        this.loadingKey = ''
      }
    },
    async copyResult() {
      try {
        await navigator.clipboard.writeText(this.resultText || '')
        this.$message.success('已复制')
      } catch (e) {
        this.$message.warning('复制失败，请手动复制')
      }
    },
    applyToPage() {
      const text = this.resultText || ''
      if (!text) {
        this.$message.warning('没有可写回的内容')
        return
      }

      const textarea = document.querySelector('textarea')
      if (textarea) {
        textarea.value = text
        textarea.dispatchEvent(new Event('input', { bubbles: true }))
        this.$message.success('已尝试写回 textarea')
        return
      }

      const quillEditor = document.querySelector('.ql-editor')
      if (quillEditor) {
        quillEditor.innerHTML = text.replace(/\n/g, '<br>')
        quillEditor.dispatchEvent(new Event('input', { bubbles: true }))
        this.$message.success('已尝试写回编辑器')
        return
      }

      this.$message.warning('未找到可写回的编辑区域，请手动复制')
    }
  }
}
</script>

<style scoped>
.scene-ai-dock {
  position: fixed;
  right: 24px;
  bottom: 104px;
  z-index: 9998;
  width: 340px;
}
.scene-ai-card {
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}
.scene-ai-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}
.scene-ai-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}
.scene-ai-card__desc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.scene-ai-card__actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>