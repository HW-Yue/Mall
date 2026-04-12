import request from './request.js'

// 认证相关API
export const authApi = {
  // 用户登录
  login(data) {
    return request({
      url: '/login',
      method: 'POST',
      data
    })
  },

  // 用户登出
  logout() {
    return request({
      url: '/auth/logout',
      method: 'POST'
    })
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request({
      url: '/auth/me',
      method: 'GET'
    })
  },

  // 刷新token
  refreshToken() {
    return request({
      url: '/auth/refresh',
      method: 'POST'
    })
  }
}
