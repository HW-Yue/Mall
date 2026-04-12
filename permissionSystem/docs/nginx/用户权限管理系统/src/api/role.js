import request from './request.js'

// 角色管理API
export const roleApi = {
  // 获取角色列表
  getRoleList(params) {
    return request({
      url: '/roles',
      method: 'GET',
      params
    })
  },

  // 添加角色
  addRole(data) {
    return request({
      url: '/roles',
      method: 'POST',
      data
    })
  },

  // 修改角色
  updateRole(id, data) {
    return request({
      url: `/roles/${id}`,
      method: 'PUT',
      data
    })
  },

  // 删除角色（支持单个和批量删除）
  deleteRole(ids) {
    return request({
      url: '/roles',
      method: 'DELETE',
      params: { ids: Array.isArray(ids) ? ids.join(',') : ids }
    })
  },

  // 根据ID查询角色信息
  getRoleById(id) {
    return request({
      url: `/roles/${id}`,
      method: 'GET'
    })
  }
}

