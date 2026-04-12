import request from './request.js'

// 系统日志API
export const logApi = {
  // 获取日志列表
  getLogList(params) {
    return request({
      url: '/logs',
      method: 'GET',
      params
    })
  },

  // 删除日志
  deleteLog(ids) {
    return request({
      url: '/logs',
      method: 'DELETE',
      params: { ids: ids.join(',') }
    })
  },

  // 导出日志
  exportLogs(params) {
    return request({
      url: '/logs/export',
      method: 'GET',
      params,
      responseType: 'blob'
    })
  },

  // 清空日志
  clearLogs() {
    return request({
      url: '/logs/clear',
      method: 'DELETE'
    })
  }
}

