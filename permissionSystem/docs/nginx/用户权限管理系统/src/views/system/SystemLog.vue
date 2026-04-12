<template>
  <div class="system-log">
    <!-- 顶部操作区 -->
    <div class="operation-bar">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="searchForm.operationType"
            placeholder="请选择操作类型"
            clearable
            style="width: 200px"
          >
            <el-option label="用户管理" value="user" />
            <el-option label="角色管理" value="role" />
            <el-option label="系统管理" value="system" />
            <el-option label="登录登出" value="auth" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <div class="action-buttons">
        <el-button type="danger" :disabled="selectedLogs.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="warning" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出日志
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="logList"
        @selection-change="handleSelectionChange"
        stripe
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="ID" width="80" align="center">
          <template #default="{ row }">
            <span class="id-text" :title="getDisplayId(row)">{{ getDisplayId(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" align="center">
          <template #default="{ row }">
            <div class="cell-inline">
              <span class="text-ellipsis" :title="row.username">{{ row.username }}</span>
              <el-icon class="copy-icon" @click.stop="copyText(row.username)"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operationType" label="操作类型" min-width="140" align="center">
          <template #default="{ row }">
            <div class="cell-inline">
              <el-tag :type="getActionTagType(row.operationType)" :title="row.operationType">
                <span class="text-ellipsis">{{ getActionName(row.operationType) }}</span>
              </el-tag>
              <el-icon class="copy-icon" @click.stop="copyText(row.operationType)"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="具体操作" min-width="300" align="left">
          <template #default="{ row }">
            <div class="cell-inline">
              <span class="text-ellipsis" :title="row.method">{{ row.method }}</span>
              <el-icon class="copy-icon" @click.stop="copyText(row.method)"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP地址" min-width="120" align="center">
          <template #default="{ row }">
            <div class="cell-inline">
              <span class="text-ellipsis" :title="row.ip">{{ row.ip }}</span>
              <el-icon class="copy-icon" @click.stop="copyText(row.ip)"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="userAgent" label="用户代理" min-width="200" align="left">
          <template #default="{ row }">
            <div class="cell-inline">
              <span class="text-ellipsis" :title="row.userAgent">{{ row.userAgent }}</span>
              <el-icon class="copy-icon" @click.stop="copyText(row.userAgent)"><DocumentCopy /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createDate" label="操作时间" min-width="160" align="center" />
        <el-table-column label="操作耗时" width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.costTime }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="danger" size="small" @click="handleDeleteLog(row)">
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, DocumentCopy } from '@element-plus/icons-vue'
import { logApi } from '../../api/log.js'

// 响应式数据
const loading = ref(false)
const logList = ref([])
const selectedLogs = ref([])

// 搜索表单
const searchForm = reactive({
  username: '',
  operationType: '',
  dateRange: []
})

// 分页数据
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 操作类型映射
const actionNames = {
  'user': '用户管理',
  'role': '角色管理',
  'system': '系统管理',
  'auth': '登录登出'
}

// 后端可能返回控制器类名，做兼容映射
const controllerTypeMap = {
  'com.permissionsystem.controller.UserController': 'user',
  'com.permissionsystem.controller.RoleController': 'role',
  'com.permissionsystem.controller.LogController': 'system',
  'com.permissionsystem.controller.AuthController': 'auth'
}

// 获取操作类型名称（兼容类名与简写枚举）
const getActionName = (operationType) => {
  if (!operationType) return ''
  // 直接命中简写
  if (actionNames[operationType]) return actionNames[operationType]
  // 精确命中类名
  if (controllerTypeMap[operationType]) return actionNames[controllerTypeMap[operationType]]
  // 包含类名子串时的模糊匹配
  const hit = Object.keys(controllerTypeMap).find(key => operationType.includes(key))
  if (hit) return actionNames[controllerTypeMap[hit]]
  return operationType
}

// 获取操作类型标签类型
const getActionTagType = (operationType) => {
  const typeMap = {
    'user': 'primary',
    'role': 'success',
    'system': 'warning',
    'auth': 'info'
  }
  return typeMap[operationType] || 'default'
}

// 获取日志列表
const getLogList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      username: searchForm.username || undefined,
      operationType: searchForm.operationType || undefined,
      startTime: searchForm.dateRange?.[0] || '',
      endTime: searchForm.dateRange?.[1] || ''
    }
    // 过滤掉 undefined 字段，避免传 action 等历史残留
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === '') {
        delete params[key]
      }
    })

    const response = await logApi.getLogList(params)
    logList.value = response.data.rows || []
    pagination.total = response.data.total || 0
  } catch (error) {
    console.error('获取日志列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getLogList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    username: '',
    operationType: '',
    dateRange: []
  })
  pagination.page = 1
  getLogList()
}

// 删除日志
const handleDeleteLog = (row) => {
  ElMessageBox.confirm(`确定要删除这条日志吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await logApi.deleteLog([row.id])
      ElMessage.success('删除成功')
      getLogList()
    } catch (error) {
      console.error('删除日志失败:', error)
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除选中的${selectedLogs.value.length}条日志吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedLogs.value.map(log => log.id)
      await logApi.deleteLog(ids)
      ElMessage.success('删除成功')
      getLogList()
    } catch (error) {
      console.error('批量删除失败:', error)
    }
  })
}

// 下载工具
const downloadBlob = (blob, suggestedName = 'logs.xlsx') => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = suggestedName
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

// 导出日志
const handleExport = async () => {
  try {
    // 优先导出选中项
    const selectedIds = selectedLogs.value.map(item => item.id).filter(Boolean)

    // 构造查询参数
    const params = selectedIds.length > 0
      ? { id: selectedIds.join(',') }
      : {
          username: searchForm.username || undefined,
          operationType: searchForm.operationType || undefined,
          startTime: searchForm.dateRange?.[0] || undefined,
          endTime: searchForm.dateRange?.[1] || undefined
        }

    // 清理 undefined
    Object.keys(params).forEach((k) => {
      if (params[k] === undefined || params[k] === '') delete params[k]
    })

    const res = await logApi.exportLogs(params)

    // 兼容 axios 封装：可能返回 { code, data(blob) } 或直接 blob
    const blob = res instanceof Blob ? res : (res?.data instanceof Blob ? res.data : null)
    if (!blob) throw new Error('导出数据为空')

    // 从响应头提取文件名（若封装层可取到 headers）
    let filename = 'logs.xlsx'
    try {
      const headers = res?.headers || res?.config?.headers || {}
      const contentDisposition = headers['content-disposition'] || headers['Content-Disposition']
      const match = contentDisposition && contentDisposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/)
      const name = decodeURIComponent(match?.[1] || match?.[2] || '')
      if (name) filename = name
    } catch {}

    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败:', e)
    ElMessage.error(e?.message || '导出失败')
  }
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedLogs.value = selection
}

// 分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.page = 1
  getLogList()
}

// 当前页变化
const handleCurrentChange = (val) => {
  pagination.page = val
  getLogList()
}

// 初始化
onMounted(() => {
  getLogList()
})

// 统一获取显示用ID（优先 uuid，其次 id）
const getDisplayId = (row) => {
  return row.uuid || row.id
}

// 复制文本
const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text ?? '')
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    console.error('复制失败:', e)
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.system-log {
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

/* ID 文本省略显示 */
.id-text {
  display: inline-block;
  max-width: 56px; /* 对应列宽80的保守可视宽度 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

/* 通用单元格内联布局与省略 */
.cell-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
}

.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-icon {
  cursor: pointer;
  color: #673AB7;
}

/* 表格列对齐优化 */
:deep(.el-table th) {
  text-align: center;
}

:deep(.el-table td) {
  text-align: center;
}

:deep(.el-table .cell) {
  text-align: center;
  padding: 8px 12px;
}

/* 具体操作列左对齐 */
:deep(.el-table td:nth-child(4) .cell) {
  text-align: left;
}

:deep(.el-table td:nth-child(7) .cell) {
  text-align: left;
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

:deep(.el-button--primary) {
  background-color: #673AB7;
  border-color: #673AB7;
}

:deep(.el-button--primary:hover) {
  background-color: #5E35B1;
  border-color: #5E35B1;
}
</style>

