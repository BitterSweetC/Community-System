<template>
  <div class="chat-widget-container">
    <!-- Chat Toggle Button (Always Visible) -->
      <button 
        class="chat-toggle-btn" 
        @click="toggleChat"
        title="打开AI助手"
        :class="{ 'is-active': isOpen }"
      >
        <transition name="icon-fade" mode="out-in">
           <el-icon v-if="!isOpen" class="icon"><ChatDotRound /></el-icon>
           <el-icon v-else class="icon"><ArrowDown /></el-icon>
        </transition>
      </button>

      <!-- Chat Window -->
      <transition name="slide-up">
        <div v-if="isOpen" class="chat-window">
          <!-- Header -->
          <div class="chat-header">
            <div class="header-left">
              <el-icon><Service /></el-icon>
              <span>社团智能助手</span>
            </div>
            <!-- Header Close Button Removed (Use Toggle Button) -->
          </div>

          <!-- Messages Area -->
          <div class="chat-messages" ref="messagesRef">
            <div v-if="messages.length === 0" class="welcome-box">
              <p>👋 你好！我是社团AI助手。</p>
              <p>你可以问我：</p>
              <div class="tags">
                <span @click="setInput('最近有什么热门活动？')">热门活动</span>
                <span @click="setInput('如何创建新社团？')">创建社团</span>
                <span @click="setInput('社团招新什么时候开始？')">招新时间</span>
              </div>
            </div>
            
            <div 
              v-for="(msg, idx) in messages" 
              :key="idx" 
              class="message-row"
              :class="msg.role"
            >
              <div class="avatar" v-if="msg.role === 'ai'">
                <el-icon><Service /></el-icon>
              </div>
              <div class="bubble">
                <div v-if="msg.loading" class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
                <div v-else>{{ msg.content }}</div>
              </div>
            </div>
          </div>

          <!-- Input Area -->
          <div class="chat-input-area">
            <textarea 
              v-model="inputMessage" 
              placeholder="请输入您的问题..." 
              @keydown.enter.prevent="sendMessage"
              :disabled="isLoading"
            ></textarea>
            <button 
              class="send-btn" 
              @click="sendMessage" 
              :disabled="!inputMessage.trim() || isLoading"
            >
              <el-icon><Position /></el-icon>
            </button>
          </div>
        </div>
      </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ArrowDown, ChatDotRound, Position, Service } from '@element-plus/icons-vue'
import api from '@/api/axios'

// --- State ---
const isOpen = ref(false)
const inputMessage = ref('')
const isLoading = ref(false)
const messages = ref([])
const messagesRef = ref(null)

// --- Actions ---
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
  }
}

const setInput = (text) => {
  inputMessage.value = text
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isLoading.value) return

  messages.value.push({ role: 'user', content })
  inputMessage.value = ''
  isLoading.value = true
  scrollToBottom()

  const aiMsgIndex = messages.value.push({ role: 'ai', content: '', loading: true }) - 1
  scrollToBottom()

  try {
    const reply = await api.post('/club/chat', { message: content }, { timeout: 30000 })
    messages.value[aiMsgIndex].loading = false
    messages.value[aiMsgIndex].content = reply
  } catch (error) {
    console.error('Chat Error:', error)
    messages.value[aiMsgIndex].loading = false
    messages.value[aiMsgIndex].content = '😔 抱歉，我现在有点累，请稍后再试。'
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
/* Container positioned absolutely relative to parent (Layout) */
.chat-widget-container {
  position: absolute;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  display: flex;
  flex-direction: column;
  align-items: flex-end; /* Align items to the right */
  pointer-events: none; /* Allow clicks to pass through empty space */
}

/* Toggle Button */
.chat-toggle-btn {
  /* Restore relative positioning */
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #3b82f6);
  border: none;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s, background 0.2s;
  color: white;
  z-index: 100001;
  pointer-events: auto; /* Enable clicks */
  margin-top: 20px; /* Space between window and button */
}

.chat-toggle-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
}

.chat-toggle-btn.is-active {
  background: #3b82f6; 
}

.chat-toggle-btn .icon {
  font-size: 28px;
}

/* Chat Window */
.chat-window {
  /* Restore relative positioning within flex container */
  position: relative;
  bottom: auto;
  right: auto;
  width: 380px;
  height: 500px;
  max-height: 70vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(0,0,0,0.05);
  transform-origin: bottom right;
  z-index: 100000;
  pointer-events: auto; /* Enable clicks */
}

/* Header */
.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #4f46e5, #3b82f6);
  color: white;
  display: flex;
  justify-content: center; /* Center title */
  align-items: center;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

/* Messages Area */
.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-box {
  text-align: center;
  color: #64748b;
  margin-top: 40px;
}

.welcome-box p {
  margin-bottom: 8px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.tags span {
  background: white;
  border: 1px solid #e2e8f0;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: #4f46e5;
}

.tags span:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

/* Message Bubbles */
.message-row {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-row.ai {
  align-self: flex-start;
}

.avatar {
  width: 32px;
  height: 32px;
  background: #eff6ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
  word-wrap: break-word;
}

.message-row.user .bubble {
  background: #3b82f6;
  color: white;
  border-bottom-right-radius: 2px;
}

.message-row.ai .bubble {
  background: white;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 2px;
}

/* Typing Indicator */
.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  margin: 0 2px;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Input Area */
.chat-input-area {
  padding: 16px;
  background: white;
  border-top: 1px solid #e2e8f0;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

textarea {
  flex: 1;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
  resize: none;
  height: 44px;
  max-height: 120px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}

textarea:focus {
  border-color: #3b82f6;
}

.send-btn {
  width: 40px;
  height: 40px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.send-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  background: #2563eb;
}

/* Transitions */
.scale-enter-active, .scale-leave-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.scale-enter-from, .scale-leave-to { opacity: 0; transform: scale(0.5); }

.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }
</style>