<template>
  <div class="role-management">
    <!-- 顶部操作区 -->
    <div class="operation-bar">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="角色名">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入角色名"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <div class="action-buttons">
        <el-button type="primary" @click="handleAddRole">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
        <el-button type="danger" :disabled="selectedRoles.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="warning" :disabled="selectedRoles.length === 0" @click="handlePinRoles">
          置顶
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="roleList"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="ID" width="100" align="center">
          <template #default="{ row }">
            <div class="id-cell">
              <span class="id-text" :title="getDisplayId(row)">{{ getDisplayId(row) }}</span>
              <el-icon class="copy-icon" @click.stop="copyId(getDisplayId(row))"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色名" min-width="220" align="center">
          <template #default="{ row }">
            <div class="cell-inline">
              <span class="text-ellipsis" :title="row.name">{{ row.name }}</span>
              <el-tag v-if="isPinnedRole(row.id) || String(row.name || '') === '超级管理员'" type="danger" size="small" class="top-flag">置顶</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEditRole(row)">
                修改
              </el-button>
              <el-button type="danger" size="small" @click="handleDeleteRole(row)">
                删除
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

    <!-- 新增/修改角色对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      :title="isEdit ? '修改角色' : '新增角色'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="roleFormRef"
        :model="roleForm"
        :rules="roleRules"
        label-width="80px"
      >
        <el-form-item label="角色名" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名" />
        </el-form-item>
        <el-form-item label="权限" prop="permissions">
          <div class="permission-groups">
            <div class="permission-group">
              <h4>用户管理权限</h4>
              <el-checkbox-group v-model="roleForm.permissions">
                <el-checkbox :value="1">添加用户</el-checkbox>
                <el-checkbox :value="2">修改用户</el-checkbox>
                <el-checkbox :value="3">删除用户</el-checkbox>
                <el-checkbox :value="8">查看用户</el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="permission-group">
              <h4>角色管理权限</h4>
              <el-checkbox-group v-model="roleForm.permissions">
                <el-checkbox :value="4">添加角色</el-checkbox>
                <el-checkbox :value="5">修改角色</el-checkbox>
                <el-checkbox :value="6">删除角色</el-checkbox>
                <el-checkbox :value="9">查看角色</el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="permission-group">
              <h4>系统管理权限</h4>
              <el-checkbox-group v-model="roleForm.permissions">
                <el-checkbox :value="7">查看日志</el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, DocumentCopy } from '@element-plus/icons-vue'
import { roleApi } from '../../api/role.js'

// 响应式数据
const loading = ref(false)
const roleList = ref([])
const selectedRoles = ref([])
const roleDialogVisible = ref(false)
const isEdit = ref(false)

// 搜索表单
const searchForm = reactive({
  name: ''
})

// 分页数据
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 角色表单
const roleFormRef = ref()
const roleForm = reactive({
  id: null,
  name: '',
  permissions: []
})

// 表单验证规则
const roleRules = {
  name: [
    { required: true, message: '请输入角色名', trigger: 'blur' }
  ],
  permissions: [
    { required: true, message: '请选择至少一个权限', trigger: 'change' }
  ]
}

// 权限数据（写死在前端）
const permissionData = [
  { id: 1, name: 'user:create', description: '添加用户' },
  { id: 2, name: 'user:update', description: '修改用户' },
  { id: 3, name: 'user:delete', description: '删除用户' },
  { id: 4, name: 'role:create', description: '添加角色' },
  { id: 5, name: 'role:update', description: '修改角色' },
  { id: 6, name: 'role:delete', description: '删除角色' },
  { id: 7, name: 'log:list', description: '查看日志' },
  { id: 8, name: 'user:list', description: '查看用户' },
  { id: 9, name: 'role:list', description: '查看角色' }
]

// 权限ID到名称的映射
const permissionNames = {
  1: '添加用户',
  2: '修改用户',
  3: '删除用户',
  4: '添加角色',
  5: '修改角色',
  6: '删除角色',
  7: '查看日志',
  8: '查看用户',
  9: '查看角色'
}

// 获取权限名称
const getPermissionName = (permissionId) => {
  return permissionNames[permissionId] || `权限${permissionId}`
}

// 获取角色列表
const getRoleList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const response = await roleApi.getRoleList(params)
    const rows = response.data.rows || []
    const pinnedSet = new Set(getPinnedRoleIds())
    roleList.value = rows.slice().sort((a, b) => {
      const aPinned = a?.id && pinnedSet.has(a.id)
      const bPinned = b?.id && pinnedSet.has(b.id)
      if (aPinned !== bPinned) return aPinned ? -1 : 1
      const aSuper = String(a?.name || '') === '超级管理员'
      const bSuper = String(b?.name || '') === '超级管理员'
      if (aSuper !== bSuper) return aSuper ? -1 : 1
      return 0
    })
    pagination.total = response.data.total || 0
  } catch (error) {
    console.error('获取角色列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getRoleList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    name: ''
  })
  pagination.page = 1
  getRoleList()
}

// 新增角色
const handleAddRole = () => {
  isEdit.value = false
  Object.assign(roleForm, {
    id: null,
    name: '',
    permissions: []
  })
  roleDialogVisible.value = true
}

// 修改角色
const handleEditRole = async (row) => {
  isEdit.value = true
  roleDialogVisible.value = true
  
  try {
    // 获取角色的完整信息（包括权限）
    const response = await roleApi.getRoleById(row.id)
    if (response.data) {
      Object.assign(roleForm, {
        id: response.data.id,
        name: response.data.name,
        permissions: response.data.permissions || []
      })
    } else {
      // 如果获取失败，使用列表中的基本信息
      Object.assign(roleForm, {
        id: row.id,
        name: row.name,
        permissions: []
      })
    }
  } catch (error) {
    console.error('获取角色详情失败:', error)
    // 如果获取失败，使用列表中的基本信息
    Object.assign(roleForm, {
      id: row.id,
      name: row.name,
      permissions: []
    })
  }
}

// 删除角色
const handleDeleteRole = (row) => {
  ElMessageBox.confirm(`确定要删除角色"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await roleApi.deleteRole([row.id])
      ElMessage.success('删除成功')
      getRoleList()
    } catch (error) {
      console.error('删除角色失败:', error)
    }
  })
}

// 批量删除角色
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的${selectedRoles.value.length}个角色吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedRoles.value.map(role => role.id)
      await roleApi.deleteRole(ids)
      ElMessage.success('删除成功')
      getRoleList()
    } catch (error) {
      console.error('批量删除失败:', error)
    }
  })
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedRoles.value = selection
}

// 提交角色表单
const handleSubmitRole = async () => {
  if (!roleFormRef.value) return
  
  await roleFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await roleApi.updateRole(roleForm.id, roleForm)
          ElMessage.success('修改成功')
        } else {
          await roleApi.addRole(roleForm)
          ElMessage.success('添加成功')
        }
        roleDialogVisible.value = false
        getRoleList()
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

// 分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.page = 1
  getRoleList()
}

// 当前页变化
const handleCurrentChange = (val) => {
  pagination.page = val
  getRoleList()
}

// 对话框关闭
const handleDialogClose = () => {
  roleFormRef.value?.resetFields()
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
const PINNED_ROLE_KEY = 'pinnedRoles'
const getPinnedRoleIds = () => {
  try {
    const raw = localStorage.getItem(PINNED_ROLE_KEY)
    const list = JSON.parse(raw || '[]')
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}
const setPinnedRoleIds = (ids) => {
  const unique = Array.from(new Set(ids.filter(Boolean)))
  localStorage.setItem(PINNED_ROLE_KEY, JSON.stringify(unique))
}
const isPinnedRole = (id) => getPinnedRoleIds().includes(id)

// 批量置顶选中的角色
const handlePinRoles = () => {
  const current = getPinnedRoleIds()
  const toAdd = selectedRoles.value.map(r => r.id).filter(Boolean)
  setPinnedRoleIds([...current, ...toAdd])
  ElMessage.success('已置顶所选角色')
  getRoleList()
}

// 初始化
onMounted(() => {
  getRoleList()
})
</script>

<style scoped>
.role-management {
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

.permission-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}

.permission-groups {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  padding: 15px;
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
  max-width: 76px; /* 列宽100 - 内边距/图标空间的保守值 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.copy-icon {
  cursor: pointer;
  color: #673AB7;
}

.permission-group {
  margin-bottom: 20px;
}

.permission-group:last-child {
  margin-bottom: 0;
}

.permission-group h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 14px;
  font-weight: 600;
}

.permission-group .el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
  gap: 12px;
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
  padding: 12px 16px;
}

/* 表格整体样式优化 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
}

:deep(.el-table tr:hover > td) {
  background-color: #f5f7fa;
}
</style>

