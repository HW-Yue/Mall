import request from './request.js'

// 聊天相关API
export const chatApi = {
  // 发送消息
  sendMessage(data) {
    return request({
      url: '/chat/conversations',
      method: 'POST',
      data
    })
  },

  // 获取聊天历史
  getChatHistory(conversationId) {
    return request({
      url: `/chat/history/${conversationId}`,
      method: 'GET'
    })
  },

  // 获取会话列表
  getConversations(params) {
    return request({
      url: '/chat/conversations',
      method: 'GET',
      params
    })
  },

  // 删除会话
  deleteConversation(conversationId) {
    return request({
      url: `/chat/conversations/${conversationId}`,
      method: 'DELETE'
    })
  },

  // 清空所有会话
  clearAllConversations() {
    return request({
      url: '/chat/conversations/clear',
      method: 'DELETE'
    })
  },

  // 获取会话详情
  getConversationDetail(conversationId) {
    return request({
      url: `/chat/conversations/${conversationId}`,
      method: 'GET'
    })
  }
}
