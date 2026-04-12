import request from './request.js'

// 用户管理API
export const userApi = {
  // 获取用户列表
  getUserList(params) {
    return request({
      url: '/users',
      method: 'GET',
      params
    })
  },

  // 添加用户
  addUser(data) {
    return request({
      url: '/users',
      method: 'POST',
      data
    })
  },

  // 修改用户
  updateUser(id, data) {
    return request({
      url: `/users/${id}`,
      method: 'PUT',
      data
    })
  },

  // 删除用户
  deleteUser(ids) {
    return request({
      url: '/users',
      method: 'DELETE',
      params: { ids: ids.join(',') }
    })
  },

  // 根据ID查询用户信息
  getUserById(id) {
    return request({
      url: `/users/${id}`,
      method: 'GET'
    })
  },

  // 分配用户角色
  assignUserRoles(id, roleIds) {
    return request({
      url: `/users/${id}/roles`,
      method: 'PUT',
      data: { roleIds }
    })
  },

  // 获取用户角色
  getUserRoles(id) {
    return request({
      url: `/users/${id}/roles`,
      method: 'GET'
    })
  }
}

