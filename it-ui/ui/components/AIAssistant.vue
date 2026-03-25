<template>
  <div class="ai-assistant">
    <!-- 悬浮按钮 -->
    <div class="ai-float-btn" @click="toggleChat" :class="{ active: chatVisible }">
      <i class="el-icon-chat-dot-round"></i>
    </div>

    <!-- 聊天窗口 -->
    <div v-show="chatVisible" class="ai-chat-window">
      <div class="chat-header">
        <span>AI 助手</span>
        <el-button type="text" icon="el-icon-close" @click="closeChat"></el-button>
      </div>
      <div class="chat-messages" ref="messagesContainer">
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="message-avatar">
            <i v-if="msg.role === 'user'" class="el-icon-user"></i>
            <i v-else class="el-icon-service"></i>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="msg.content"></div>
            <div v-if="msg.code" class="code-block">
              <pre><code>{{ msg.code }}</code></pre>
            </div>
          </div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="message-avatar"><i class="el-icon-service"></i></div>
          <div class="message-content typing">
            <span>.</span><span>.</span><span>.</span>
          </div>
        </div>
      </div>
      <div class="chat-input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入你的问题，或选中文本后点击快捷按钮..."
          @keyup.ctrl.enter="sendMessage"
        />
        <div class="chat-actions">
          <el-button size="small" @click="quickAction('explain')" :disabled="!selectedText">解释/翻译</el-button>
          <el-button size="small" @click="quickAction('code')" :disabled="!selectedText">代码解读</el-button>
          <el-button type="primary" size="small" @click="sendMessage" :loading="loading">发送</el-button>
        </div>
      </div>
    </div>

    <!-- 选中文本快捷栏 -->
    <div v-if="selectionVisible" class="selection-toolbar" :style="{ top: selectionTop, left: selectionLeft }">
      <el-button size="mini" @click="handleSelectionExplain">解释/翻译</el-button>
      <el-button size="mini" @click="handleSelectionCode">代码解读</el-button>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AIAssistant',
  data() {
    return {
      chatVisible: false,
      messages: [
        { role: 'assistant', content: '你好！我是AI助手，可以帮你解释文本、翻译、解读代码。试试选中任意文字或代码块。' }
      ],
      inputText: '',
      loading: false,
      selectedText: '',
      selectionVisible: false,
      selectionTop: 0,
      selectionLeft: 0,
    };
  },
  mounted() {
    // 监听鼠标松开事件，获取选中文本
    document.addEventListener('mouseup', this.handleTextSelection);
    // 点击其他地方关闭快捷栏
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.selection-toolbar')) {
        this.selectionVisible = false;
      }
    });
  },
  beforeDestroy() {
    document.removeEventListener('mouseup', this.handleTextSelection);
  },
  methods: {
    toggleChat() {
      this.chatVisible = !this.chatVisible;
      if (this.chatVisible) {
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },
    closeChat() {
      this.chatVisible = false;
    },
    handleTextSelection(e) {
      const selection = window.getSelection();
      const text = selection.toString().trim();
      if (text) {
        // 获取选区位置，用于定位快捷栏
        const range = selection.getRangeAt(0);
        const rect = range.getBoundingClientRect();
        this.selectionTop = rect.bottom + window.scrollY + 5;
        this.selectionLeft = rect.left + window.scrollX;
        this.selectedText = text;
        this.selectionVisible = true;
      } else {
        this.selectionVisible = false;
      }
    },
    async handleSelectionExplain() {
      this.selectionVisible = false;
      await this.quickAction('explain');
    },
    async handleSelectionCode() {
      this.selectionVisible = false;
      await this.quickAction('code');
    },
    async quickAction(type) {
      if (!this.selectedText) return;
      let question = '';
      if (type === 'explain') {
        // 检测是否英文（简单正则）
        const isEnglish = /^[a-zA-Z\s.,!?]+$/.test(this.selectedText);
        if (isEnglish) {
          question = `请将以下英文翻译成中文并解释：\n${this.selectedText}`;
        } else {
          question = `请解释以下内容：\n${this.selectedText}`;
        }
      } else if (type === 'code') {
        question = `请分析以下代码的作用和逻辑：\n${this.selectedText}`;
      }
      // 将问题添加到聊天记录并发送
      this.messages.push({ role: 'user', content: question });
      this.inputText = '';
      this.selectedText = '';
      await this.sendToAI(question);
    },
    async sendMessage() {
      if (!this.inputText.trim()) return;
      const userMsg = this.inputText;
      this.messages.push({ role: 'user', content: userMsg });
      this.inputText = '';
      await this.sendToAI(userMsg);
    },
    async sendToAI(question) {
      this.loading = true;
      try {
        // 调用AI聊天接口（根据实际后端调整）
        // 这里使用通用聊天接口，支持多轮对话（历史记录）
        const res = await axios.post('/api/ai/chat', { message: question, history: this.getHistory() });
        const answer = res.data.answer;
        this.messages.push({ role: 'assistant', content: answer });
        this.$nextTick(() => this.scrollToBottom());
      } catch (error) {
        console.error('AI请求失败', error);
        this.$message.error('AI服务暂时不可用，请稍后重试');
      } finally {
        this.loading = false;
      }
    },
    getHistory() {
      // 返回最近10条对话历史（不包括当前消息）
      return this.messages.slice(-10).map(m => ({
        role: m.role,
        content: m.content
      }));
    },
    scrollToBottom() {
      const container = this.$refs.messagesContainer;
      if (container) container.scrollTop = container.scrollHeight;
    }
  }
};
</script>

<style scoped>
.ai-assistant {
  position: relative;
  z-index: 9999;
}
/* 悬浮按钮 */
.ai-float-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 4px 12px rgba(59,130,246,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  color: white;
  font-size: 28px;
}
.ai-float-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(59,130,246,0.5);
}
.ai-float-btn.active {
  transform: rotate(90deg);
}
/* 聊天窗口 */
.ai-chat-window {
  position: fixed;
  bottom: 100px;
  right: 30px;
  width: 400px;
  height: 500px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1000;
}
.chat-header {
  padding: 12px 16px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-header .el-button {
  color: white;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f9fafb;
}
.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.message.user {
  flex-direction: row-reverse;
}
.message.user .message-content {
  background: #3b82f6;
  color: white;
  border-radius: 18px 18px 4px 18px;
}
.message.assistant .message-content {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 18px 18px 18px 4px;
}
.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #6b7280;
}
.message.user .message-avatar {
  background: #3b82f6;
  color: white;
}
.message-content {
  max-width: 70%;
  padding: 8px 12px;
  word-break: break-word;
}
.message-text {
  font-size: 14px;
  line-height: 1.5;
}
.message-text code {
  background: #f1f5f9;
  padding: 2px 4px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.9em;
}
.typing span {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  margin: 0 2px;
  animation: blink 1.4s infinite both;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0% { opacity: 0.2; }
  20% { opacity: 1; }
  100% { opacity: 0.2; }
}
.chat-input-area {
  padding: 12px;
  border-top: 1px solid #e5e7eb;
  background: white;
}
.chat-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  justify-content: flex-end;
}
/* 选中文本快捷栏 */
.selection-toolbar {
  position: absolute;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  padding: 4px;
  display: flex;
  gap: 4px;
  z-index: 10000;
}
</style>