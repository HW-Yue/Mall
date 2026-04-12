<template>
  <div class="user-management">
    <!-- 顶部操作区 -->
    <div class="operation-bar">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="昵称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入昵称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="searchForm.email"
            placeholder="请输入邮箱"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="searchForm.createdAtRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <div class="action-buttons">
        <el-button type="primary" @click="handleAddUser">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
        <el-button type="danger" :disabled="selectedUsers.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="warning" :disabled="selectedUsers.length === 0" @click="handlePinUsers">
          置顶
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="userList"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="ID" width="80" align="center">
          <template #default="{ row }">
            <div class="id-cell">
              <span class="id-text" :title="getDisplayId(row)">{{ getDisplayId(row) }}</span>
              <el-icon class="copy-icon" @click.stop="copyId(getDisplayId(row))"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="账号" min-width="160" align="center">
          <template #default="{ row }">
            <div class="cell-inline">
              <span class="text-ellipsis" :title="row.username">{{ row.username }}</span>
              <el-tag v-if="isPinnedUser(row.id) || String(row.username || '').toLowerCase() === 'admin'" type="danger" size="small" class="top-flag">置顶</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="昵称" min-width="120" align="center" />
        <el-table-column prop="email" label="邮箱" min-width="180" align="center" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" align="center" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" align="center" />
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEditUser(row)">
                修改
              </el-button>
              <el-button type="danger" size="small" @click="handleDeleteUser(row)">
                删除
              </el-button>
              <el-button type="warning" size="small" @click="handleAssignRoles(row)">
                赋予角色
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/修改用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEdit ? '修改用户' : '新增用户'"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="80px"
      >
        <el-form-item label="昵称" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="分配角色"
      width="600px"
    >
      <el-form label-width="100px">
        <el-form-item label="用户信息">
          <div class="user-info">
            <el-tag type="info" size="large">{{ currentUser.name }}</el-tag>
            <span class="user-detail">({{ currentUser.username }})</span>
          </div>
        </el-form-item>
        <el-form-item label="当前角色">
          <div v-if="selectedRoleIds.length === 0" class="no-roles">
            <el-text type="info">该用户暂无角色</el-text>
          </div>
          <div v-else class="current-roles">
            <el-tag
              v-for="roleId in selectedRoleIds"
              :key="roleId"
              type="success"
              closable
              @close="removeRole(roleId)"
              class="role-tag"
            >
              {{ getRoleName(roleId) }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="所有角色">
          <div class="all-roles">
            <el-checkbox-group v-model="selectedRoleIds">
              <el-checkbox
                v-for="role in roleList"
                :key="role.id"
                :value="role.id"
                class="role-checkbox"
              >
                {{ role.name }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitRoles">保存修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, DocumentCopy } from '@element-plus/icons-vue'
import { userApi } from '../../api/user.js'
import { roleApi } from '../../api/role.js'

// 响应式数据
const loading = ref(false)
const userList = ref([])
const roleList = ref([])
const selectedUsers = ref([])
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const isEdit = ref(false)
const currentUser = ref({})
const selectedRoleIds = ref([])

// 搜索表单
const searchForm = reactive({
  name: '',
  email: '',
  createdAtRange: [],
  
})

// 分页数据
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 用户表单
const userFormRef = ref()
const userForm = reactive({
  id: null,
  name: '',
  username: '',
  password: '',
  email: ''
})

// 表单验证规则
const userRules = {
  name: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 获取用户列表
const getUserList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name,
      email: searchForm.email,
      createdStart: searchForm.createdAtRange?.[0] || '',
      createdEnd: searchForm.createdAtRange?.[1] || ''
    }
    const response = await userApi.getUserList(params)
    const rows = response.data.rows || []
    // 置顶优先，其次 admin，最后保持原相对顺序
    const pinnedSet = new Set(getPinnedUserIds())
    userList.value = rows.slice().sort((a, b) => {
      const aPinned = a?.id && pinnedSet.has(a.id)
      const bPinned = b?.id && pinnedSet.has(b.id)
      if (aPinned !== bPinned) return aPinned ? -1 : 1
      const aAdmin = String(a?.username || '').toLowerCase() === 'admin'
      const bAdmin = String(b?.username || '').toLowerCase() === 'admin'
      if (aAdmin !== bAdmin) return aAdmin ? -1 : 1
      return 0
    })
    pagination.total = response.data.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取角色列表
const getRoleList = async () => {
  try {
    const response = await roleApi.getRoleList({ page: 1, pageSize: 1000 })
    roleList.value = response.data.rows || []
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 获取用户已有角色
const getUserRoles = async (userId) => {
  try {
    const response = await userApi.getUserRoles(userId)
    selectedRoleIds.value = response.data || []
  } catch (error) {
    console.error('获取用户角色失败:', error)
    selectedRoleIds.value = []
  }
}


// 根据角色ID获取角色名称
const getRoleName = (roleId) => {
  const role = roleList.value.find(r => r.id === roleId)
  return role ? role.name : `角色${roleId}`
}


// 移除角色
const removeRole = (roleId) => {
  const index = selectedRoleIds.value.indexOf(roleId)
  if (index > -1) {
    selectedRoleIds.value.splice(index, 1)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getUserList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    email: '',
    createdAtRange: [],
    
  })
  pagination.page = 1
  getUserList()
}

// 新增用户
const handleAddUser = () => {
  isEdit.value = false
  Object.assign(userForm, {
    id: null,
    name: '',
    username: '',
    password: '',
    email: ''
  })
  userDialogVisible.value = true
}

// 修改用户
const handleEditUser = (row) => {
  isEdit.value = true
  Object.assign(userForm, { ...row })
  userDialogVisible.value = true
}

// 删除用户
const handleDeleteUser = (row) => {
  ElMessageBox.confirm(`确定要删除用户"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await userApi.deleteUser([row.id])
      ElMessage.success('删除成功')
      getUserList()
    } catch (error) {
      console.error('删除用户失败:', error)
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的${selectedUsers.value.length}个用户吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedUsers.value.map(user => user.id)
      await userApi.deleteUser(ids)
      ElMessage.success('删除成功')
      getUserList()
    } catch (error) {
      console.error('批量删除失败:', error)
    }
  })
}

// 分配角色
const handleAssignRoles = async (row) => {
  currentUser.value = { ...row }
  selectedRoleIds.value = []
  roleDialogVisible.value = true
  await getRoleList()
  await getUserRoles(row.id)
}

// 提交用户表单
const handleSubmitUser = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await userApi.updateUser(userForm.id, userForm)
          ElMessage.success('修改成功')
        } else {
          await userApi.addUser(userForm)
          ElMessage.success('添加成功')
        }
        userDialogVisible.value = false
        getUserList()
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

// 提交角色分配
const handleSubmitRoles = async () => {
  try {
    await userApi.assignUserRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } catch (error) {
    console.error('角色分配失败:', error)
  }
}

// 复制ID
const copyId = async (id) => {
  try {
    await navigator.clipboard.writeText(id)
    ElMessage.success('ID 已复制到剪贴板')
  } catch (e) {
    console.error('复制失败:', e)
    ElMessage.error('复制失败')
  }
}

// 统一获取显示用ID（优先 uuid，其次 id）
const getDisplayId = (row) => {
  return row.uuid || row.id
}

// 置顶：本地缓存
const PINNED_USER_KEY = 'pinnedUsers'
const getPinnedUserIds = () => {
  try {
    const raw = localStorage.getItem(PINNED_USER_KEY)
    const list = JSON.parse(raw || '[]')
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}
const setPinnedUserIds = (ids) => {
  const unique = Array.from(new Set(ids.filter(Boolean)))
  localStorage.setItem(PINNED_USER_KEY, JSON.stringify(unique))
}
const isPinnedUser = (id) => getPinnedUserIds().includes(id)

// 批量置顶选中的用户
const handlePinUsers = () => {
  const current = getPinnedUserIds()
  const toAdd = selectedUsers.value.map(u => u.id).filter(Boolean)
  setPinnedUserIds([...current, ...toAdd])
  ElMessage.success('已置顶所选用户')
  getUserList()
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

// 分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.page = 1
  getUserList()
}

// 当前页变化
const handleCurrentChange = (val) => {
  pagination.page = val
  getUserList()
}

// 对话框关闭
const handleDialogClose = () => {
  userFormRef.value?.resetFields()
}

// 初始化
onMounted(() => {
  getUserList()
})
</script>

<style scoped>
.user-management {
  background: white;
  border-radius: 8px;
  padding: 20px;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e6e6e6;
}

.search-form {
  flex: 1;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

:deep(.el-button--primary) {
  background-color: #673AB7;
  border-color: #673AB7;
}

:deep(.el-button--primary:hover) {
  background-color: #5E35B1;
  border-color: #5E35B1;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
  flex-shrink: 0;
}

/* 表格列对齐优化 */
:deep(.el-table th) {
  text-align: center;
}

:deep(.el-table td) {
  text-align: center;
}

/* 表格内容居中对齐 */
:deep(.el-table .cell) {
  text-align: center;
  padding: 8px 12px;
}

/* 角色分配对话框样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-detail {
  color: #666;
  font-size: 14px;
}

.no-roles {
  color: #999;
  font-style: italic;
}

.current-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tag {
  margin: 2px;
}

.all-roles {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  padding: 12px;
  background-color: #fafafa;
}

.role-checkbox {
  display: block;
  margin: 8px 0;
  width: 100%;
}

/* 通用内联布局 */
.cell-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
}

.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-flag {
  vertical-align: middle;
}

/* ID 单元格样式：固定宽度内省略 */
.id-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
}

.id-text {
  display: inline-block;
  max-width: 56px; /* 宽度=列宽(80) - 左右内边距和图标空间的保守值 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.copy-icon {
  cursor: pointer;
  color: #673AB7;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

