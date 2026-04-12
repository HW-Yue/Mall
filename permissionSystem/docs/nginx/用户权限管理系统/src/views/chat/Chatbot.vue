<template>
  <div class="gemini-chat-container">
    <!-- 主聊天区域 -->
    <div class="main-chat">
      
      <!-- 顶部欢迎区域 -->
      <div v-if="messages.length === 0" class="welcome-section">
        <div class="welcome-content">
          <div class="welcome-icon">
            <el-icon size="48"><ChatDotRound /></el-icon>
          </div>
          <h1 class="welcome-title">Hello, {{ userInfo.name || '用户' }}</h1>
          <p class="welcome-subtitle">我是您的智能助手，有什么可以帮助您的吗？</p>
        </div>
      </div>

    <!-- 消息区域 -->
    <div v-if="messages.length > 0" ref="messageContainer" class="messages-container">
      <div
        v-for="(message, index) in messages"
        :key="index"
        :class="['message-wrapper', message.type]"
      >
        <div class="message-content">
          <div v-if="message.type === 'user'" class="user-message">
            {{ message.content }}
          </div>
          <div v-else class="bot-message">
            <div v-if="message.replyType === 'text'" class="text-content">
              {{ message.content }}
            </div>
            <div v-else-if="message.replyType === 'options'" class="options-content">
              <div class="options-title">{{ message.content.title }}</div>
              <div class="options-grid">
                <button
                  v-for="(item, itemIndex) in message.content.items"
                  :key="itemIndex"
                  class="option-chip"
                  @click="handleOptionClick(item.text)"
                >
                  {{ item.text }}
                </button>
              </div>
            </div>
            <div v-else-if="message.replyType === 'card'" class="card-content">
              <div class="card-title">{{ message.content.title }}</div>
              <div class="card-description">{{ message.content.description }}</div>
              <div v-if="message.content.buttons" class="card-actions">
                <button
                  v-for="(button, buttonIndex) in message.content.buttons"
                  :key="buttonIndex"
                  class="card-button"
                  @click="handleCardButtonClick(button)"
                >
                  {{ button.text }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-section">
      <div class="input-container">
        <div class="input-wrapper">
          <button class="attach-button" @click="handleAttach">
            <el-icon><Plus /></el-icon>
          </button>
          <button class="rag-button" @click="showRagDialog = true" title="RAG知识库管理">
            <el-icon><Collection /></el-icon>
          </button>
          <div class="input-field">
            <textarea
              v-model="inputMessage"
              ref="inputRef"
              placeholder="Ask AI"
              :disabled="sending"
              @keydown="handleKeydown"
              @input="handleInput"
              rows="1"
            ></textarea>
            <button class="voice-button" @click="handleVoice">
              <el-icon><Microphone /></el-icon>
            </button>
          </div>
          <button 
            class="send-button"
            :disabled="!inputMessage.trim() || sending"
            @click="handleSendMessage"
          >
            <el-icon v-if="!sending"><ArrowRight /></el-icon>
            <el-icon v-else class="loading"><Loading /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 建议按钮区域 -->
    <div v-if="messages.length === 0" class="suggestions-section">
      <div class="suggestions-grid">
        <button class="suggestion-chip" @click="handleSuggestion('这个系统的作者是谁？')">
          <el-icon><User /></el-icon>
          系统信息
        </button>
        <button class="suggestion-chip" @click="handleSuggestion('这个系统可以做什么？')">
          <el-icon><Setting /></el-icon>
          功能介绍
        </button>
      </div>
    </div>
    </div>

    <!-- RAG知识库管理弹窗 -->
    <el-dialog
      v-model="showRagDialog"
      title="RAG知识库管理"
      width="80%"
      :before-close="handleRagDialogClose"
    >
      <div class="rag-management">
        <!-- 操作按钮区域 -->
        <div class="rag-actions">
          <el-button type="primary" @click="showAddForm = true">
            <el-icon><Plus /></el-icon>
            新增知识
          </el-button>
        </div>

        <!-- 新增知识表单 -->
        <el-form v-if="showAddForm" :model="ragForm" :rules="ragRules" ref="ragFormRef" class="rag-form">
          <el-form-item label="知识内容" prop="content">
            <el-input
              v-model="ragForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入要存储的知识内容..."
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleAddRag" :loading="ragLoading">保存</el-button>
            <el-button @click="showAddForm = false">取消</el-button>
          </el-form-item>
        </el-form>

        <!-- 仅保留新增知识功能 -->
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ChatDotRound, 
  User, 
  Plus, 
  Microphone, 
  ArrowRight, 
  Loading,
  Setting,
  Key,
  DataAnalysis,
  Star,
  Delete,
  ArrowLeft,
  Close,
  Collection,
  Search
} from '@element-plus/icons-vue'
import { chatApi } from '../../api/chat.js'
import { API_ENDPOINTS } from '../../config/api.js'

// 响应式数据
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const conversationId = ref('')
const messageContainer = ref()
const inputRef = ref()
const conversations = ref([])
const sidebarCollapsed = ref(false)

// RAG相关数据
const showRagDialog = ref(false)
const showAddForm = ref(false)
const ragForm = ref({
  content: ''
})
const ragFormRef = ref()
const ragLoading = ref(false)
// 仅保留新增知识功能

// 计算属性
const userInfo = computed(() => {
  const stored = localStorage.getItem('userInfo')
  return stored ? JSON.parse(stored) : { name: '用户' }
})

// RAG表单验证规则
const ragRules = {
  content: [
    { required: true, message: '请输入知识内容', trigger: 'blur' },
    { min: 10, message: '知识内容至少10个字符', trigger: 'blur' }
  ]
}

// 发送消息（流式处理）
const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value) return
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  adjustTextareaHeight()
  
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: userMessage
  })
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  sending.value = true
  
  // 添加机器人消息占位符（用于流式更新）
  const botMessageIndex = messages.value.length
  messages.value.push({
    type: 'bot',
    content: '',
    replyType: 'text',
    isStreaming: true
  })
  
  try {
    const requestData = {
      message: {
        type: 'text',
        content: userMessage
      }
    }
    
    // 添加会话ID（如果有的话）
    if (conversationId.value) {
      requestData.conversationId = conversationId.value
      console.log('添加会话ID到请求:', conversationId.value)
    } else {
      console.log('当前没有会话ID，将创建新会话')
    }
    
    // 添加用户ID
    const userId = getCurrentUserId()
    if (userId) {
      requestData.userId = userId
      console.log('添加用户ID到请求:', userId)
    } else {
      console.log('警告：无法获取用户ID')
    }
    
    console.log('完整请求数据:', JSON.stringify(requestData, null, 2))
    
    // 使用流式处理
    await handleStreamingResponse(requestData, botMessageIndex)
    
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请重试')
    // 移除失败的流式消息
    messages.value.splice(botMessageIndex, 1)
  } finally {
    sending.value = false
  }
}

// 处理流式响应
const handleStreamingResponse = async (requestData, messageIndex) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(API_ENDPOINTS.CHAT_CONVERSATIONS, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(requestData)
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    // 尝试从响应头获取会话ID（可能因为CORS限制无法访问）
    const conversationIdFromHeader = response.headers.get('X-Conversation-Id')
    console.log('响应头中的会话ID:', conversationIdFromHeader)
    console.log('当前会话ID:', conversationId.value)
    
    // 检查所有可用的响应头
    console.log('所有响应头:', Array.from(response.headers.entries()))
    
    if (conversationIdFromHeader) {
      if (!conversationId.value) {
        // 第一次获取会话ID
        conversationId.value = conversationIdFromHeader
        localStorage.setItem('currentConversationId', conversationId.value)
        console.log('首次设置会话ID:', conversationIdFromHeader)
      } else if (conversationId.value !== conversationIdFromHeader) {
        // 会话ID发生变化，更新它
        conversationId.value = conversationIdFromHeader
        localStorage.setItem('currentConversationId', conversationId.value)
        console.log('更新会话ID:', conversationIdFromHeader)
      } else {
        console.log('会话ID未变化:', conversationIdFromHeader)
      }
    } else {
      console.log('响应头中没有会话ID（可能是CORS限制）')
      console.log('建议后端在CORS配置中添加: exposedHeaders = "X-Conversation-Id"')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()
      
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')
      
      for (const line of lines) {
        if (line.trim() === '') continue
        
        try {
          // 尝试解析JSON数据
          const data = JSON.parse(line)
          
          // 处理流式内容
          if (data.content) {
            fullContent += data.content
            // 更新消息内容
            messages.value[messageIndex].content = fullContent
            // 滚动到底部
            await nextTick()
            scrollToBottom()
          }
          
        } catch (parseError) {
          // 如果不是JSON，可能是纯文本流
          if (line.trim()) {
            fullContent += line
            messages.value[messageIndex].content = fullContent
            await nextTick()
            scrollToBottom()
          }
        }
      }
    }
    
    // 流式结束，标记为非流式状态
    messages.value[messageIndex].isStreaming = false
    
  } catch (error) {
    console.error('流式响应处理失败:', error)
    throw error
  }
}

// 处理建议点击
const handleSuggestion = (text) => {
  inputMessage.value = text
  handleSendMessage()
}

// 处理选项点击
const handleOptionClick = (optionText) => {
  inputMessage.value = optionText
  handleSendMessage()
}

// 处理卡片按钮点击
const handleCardButtonClick = (button) => {
  if (button.action) {
    ElMessage.info(`点击了: ${button.text}`)
  } else {
    inputMessage.value = button.text
    handleSendMessage()
  }
}

// 处理附件
const handleAttach = () => {
  ElMessage.info('附件功能开发中...')
}

// 处理语音
const handleVoice = () => {
  ElMessage.info('语音功能开发中...')
}

// 处理键盘事件
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSendMessage()
  }
}

// 处理输入
const handleInput = () => {
  adjustTextareaHeight()
}

// 调整文本框高度
const adjustTextareaHeight = () => {
  nextTick(() => {
    if (inputRef.value) {
      inputRef.value.style.height = 'auto'
      inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
    }
  })
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

// 获取当前用户ID
const getCurrentUserId = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      // 直接返回 username，因为后端期望的是 username
      return user.username
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
  return undefined
}

// 会话管理功能
// 获取会话列表请求已删除

const loadConversation = async (id) => {
  if (id === conversationId.value) return
  
  conversationId.value = id
  messages.value = []
  
  // 暂时屏蔽获取聊天历史功能
  // try {
  //   const response = await chatApi.getChatHistory(id)
  //   messages.value = response.data || []
  //   await nextTick()
  //   scrollToBottom()
  // } catch (error) {
  //   console.error('加载聊天历史失败:', error)
  //   ElMessage.error('加载聊天历史失败')
  // }
  
  ElMessage.info('聊天历史功能暂未实现')
}

const deleteConversation = async (id) => {
  try {
    await chatApi.deleteConversation(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    
    if (id === conversationId.value) {
      conversationId.value = ''
      messages.value = []
    }
    
    ElMessage.success('会话已删除')
  } catch (error) {
    console.error('删除会话失败:', error)
    ElMessage.error('删除会话失败')
  }
}

const handleNewChat = () => {
  conversationId.value = ''
  messages.value = []
  inputMessage.value = ''
  // 清除保存的会话ID
  localStorage.removeItem('currentConversationId')
}

const handleClearAll = async () => {
  try {
    await chatApi.clearAllConversations()
    conversations.value = []
    conversationId.value = ''
    messages.value = []
    // 清除保存的会话ID
    localStorage.removeItem('currentConversationId')
    ElMessage.success('所有会话已清空')
  } catch (error) {
    console.error('清空会话失败:', error)
    ElMessage.error('清空会话失败')
  }
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  return date.toLocaleDateString()
}

// RAG相关方法
const handleRagDialogClose = () => {
  showRagDialog.value = false
  showAddForm.value = false
  ragForm.value.content = ''
}

const handleAddRag = async () => {
  if (!ragFormRef.value) return
  
  await ragFormRef.value.validate(async (valid) => {
    if (valid) {
      ragLoading.value = true
      try {
        // 这里调用后端API添加知识
        const response = await fetch(API_ENDPOINTS.RAG_KNOWLEDGE, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          body: JSON.stringify({
            content: ragForm.value.content
          })
        })
        
        if (response.ok) {
          ElMessage.success('知识添加成功')
          ragForm.value.content = ''
          showAddForm.value = false
          loadRagKnowledgeList()
        } else {
          ElMessage.error('知识添加失败')
        }
      } catch (error) {
        console.error('添加知识失败:', error)
        ElMessage.error('添加知识失败')
      } finally {
        ragLoading.value = false
      }
    }
  })
}

// 删除知识功能移除

// 加载知识库列表功能移除

// RAG查询入口移除

// RAG查询功能移除

// 初始化
onMounted(() => {
  // 恢复之前的会话ID
  const savedConversationId = localStorage.getItem('currentConversationId')
  if (savedConversationId) {
    conversationId.value = savedConversationId
    // 暂时屏蔽加载历史消息功能
    // loadConversation(savedConversationId)
  }
  
  // 会话列表请求已删除
})
</script>

<style scoped>
.gemini-chat-container {
  height: calc(100vh - 120px);
  display: flex;
  background: #ffffff;
  position: relative;
}

/* 侧边栏样式 */
.sidebar {
  width: 300px;
  background: #f8f9fa;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.sidebar-collapsed {
  width: 0;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e8eaed;
  background: white;
}

.sidebar-header h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.sidebar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  padding: 12px;
  margin-bottom: 4px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.conversation-item:hover {
  background: #f1f3f4;
  border-color: #dadce0;
}

.conversation-item.active {
  background: #e8f0fe;
  border-color: #1a73e8;
}

.conversation-title {
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-preview {
  font-size: 12px;
  color: #5f6368;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conversation-time {
  font-size: 11px;
  color: #5f6368;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

/* 主聊天区域 */
.main-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  position: relative;
}

.main-chat.sidebar-expanded {
  margin-left: 0;
}

/* 侧边栏展开按钮 */
.sidebar-toggle-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 1000;
  background: #4285f4;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.sidebar-toggle-btn:hover {
  background: #3367d6;
  transform: scale(1.05);
}

/* 欢迎区域 */
.welcome-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: white;
}

.welcome-content {
  text-align: center;
  max-width: 600px;
}

.welcome-icon {
  margin-bottom: 24px;
  color: #4285f4;
}

.welcome-title {
  font-size: 32px;
  font-weight: 400;
  color: #1a1a1a;
  margin: 0 0 16px 0;
  line-height: 1.2;
}

.welcome-subtitle {
  font-size: 16px;
  color: #5f6368;
  margin: 0;
  line-height: 1.5;
}

/* 消息区域 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  max-height: calc(100vh - 200px);
  background: white;
}

.message-wrapper {
  margin-bottom: 24px;
  padding: 0 20px;
}

.message-wrapper.user {
  display: flex;
  justify-content: flex-end;
}

.message-wrapper.bot {
  display: flex;
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  word-wrap: break-word;
}

.user-message {
  background: #4285f4;
  color: white;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.4;
  font-size: 14px;
}

.bot-message {
  background: #f1f3f4;
  color: #1a1a1a;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.4;
  font-size: 14px;
}

.text-content {
  white-space: pre-wrap;
}

/* 选项内容 */
.options-content {
  background: transparent;
  padding: 0;
}

.options-title {
  font-weight: 500;
  margin-bottom: 12px;
  color: #1a1a1a;
}

.options-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-chip {
  background: #e8f0fe;
  color: #1a73e8;
  border: 1px solid #dadce0;
  border-radius: 16px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.option-chip:hover {
  background: #1a73e8;
  color: white;
  border-color: #1a73e8;
}

/* 卡片内容 */
.card-content {
  background: transparent;
  padding: 0;
}

.card-title {
  font-weight: 500;
  margin-bottom: 8px;
  color: #1a1a1a;
}

.card-description {
  color: #5f6368;
  margin-bottom: 12px;
  line-height: 1.4;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-button {
  background: #e8f0fe;
  color: #1a73e8;
  border: 1px solid #dadce0;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.card-button:hover {
  background: #1a73e8;
  color: white;
  border-color: #1a73e8;
}

/* 输入区域 */
.input-section {
  padding: 20px;
  background: white;
  border-top: 1px solid #e8eaed;
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: white;
  border: 1px solid #dadce0;
  border-radius: 24px;
  padding: 8px 16px;
  transition: all 0.2s;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 1px 3px 1px rgba(60,64,67,.15);
}

.input-wrapper:focus-within {
  border-color: #4285f4;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 1px 3px 1px rgba(60,64,67,.15), 0 0 0 1px #4285f4;
}

.input-field {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-field textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 16px;
  line-height: 1.5;
  min-height: 24px;
  max-height: 120px;
  background: transparent;
  color: #1a1a1a;
}

.input-field textarea::placeholder {
  color: #5f6368;
}

.attach-button,
.voice-button,
.rag-button {
  background: none;
  border: none;
  color: #5f6368;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attach-button:hover,
.voice-button:hover,
.rag-button:hover {
  background: #f1f3f4;
  color: #1a1a1a;
}

.rag-button {
  color: #4285f4;
}

.rag-button:hover {
  background: #e8f0fe;
  color: #1a73e8;
}

.send-button {
  background: #4285f4;
  color: white;
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.send-button:hover:not(:disabled) {
  background: #3367d6;
}

.send-button:disabled {
  background: #dadce0;
  color: #5f6368;
  cursor: not-allowed;
}

.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 建议区域 */
.suggestions-section {
  padding: 20px;
  background: white;
  border-top: 1px solid #e8eaed;
}

.suggestions-grid {
  max-width: 800px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.suggestion-chip {
  background: #f8f9fa;
  color: #1a1a1a;
  border: 1px solid #dadce0;
  border-radius: 16px;
  padding: 12px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.suggestion-chip:hover {
  background: #e8f0fe;
  color: #1a73e8;
  border-color: #1a73e8;
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 1px 3px 1px rgba(60,64,67,.15);
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #dadce0;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #5f6368;
}

/* RAG管理样式 */
.rag-management {
  padding: 20px 0;
}

.rag-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
}

.rag-form {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.rag-query {
  margin-bottom: 20px;
}

.rag-result {
  margin-top: 16px;
  padding: 16px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
}

.rag-result h4 {
  margin: 0 0 12px 0;
  color: #0369a1;
  font-size: 14px;
}

.result-content {
  color: #1e40af;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-title {
    font-size: 24px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .suggestions-grid {
    grid-template-columns: 1fr;
  }
  
  .input-wrapper {
    padding: 6px 12px;
  }
  
  .input-field textarea {
    font-size: 16px;
  }
  
  .rag-actions {
    flex-direction: column;
  }
}
</style>