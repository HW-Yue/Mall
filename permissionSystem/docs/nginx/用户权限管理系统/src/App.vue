<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// 这里可以添加全局逻辑
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

#app {
  height: 100%;
}

/* 全局样式重置 */
.el-button--primary {
  background-color: #673AB7;
  border-color: #673AB7;
}

.el-button--primary:hover {
  background-color: #5E35B1;
  border-color: #5E35B1;
}

.el-button--primary:focus {
  background-color: #5E35B1;
  border-color: #5E35B1;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>






