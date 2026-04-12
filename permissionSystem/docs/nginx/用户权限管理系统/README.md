# 用户权限管理系统

基于Vue 3 + Element Plus的用户权限管理系统，包含用户管理、角色管理、权限控制和智能客服功能。

## 功能特性

- 🔐 **用户认证** - 登录/登出功能
- 👥 **用户管理** - 用户的增删改查、角色分配
- 🛡️ **角色管理** - 角色的增删改查、权限配置
- 🔑 **权限控制** - 基于角色的权限管理
- 🤖 **智能客服** - 集成AI聊天功能
- 📱 **响应式设计** - 适配各种屏幕尺寸

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **UI组件库**: Element Plus
- **路由管理**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite
- **图标库**: Element Plus Icons

## 项目结构

```
src/
├── api/                 # API接口
│   ├── request.js      # Axios配置
│   ├── auth.js         # 认证相关API
│   ├── user.js         # 用户管理API
│   ├── role.js         # 角色管理API
│   └── chat.js         # 聊天API
├── components/         # 公共组件
├── router/            # 路由配置
│   └── index.js
├── views/             # 页面组件
│   ├── Login.vue      # 登录页
│   ├── layout/        # 布局组件
│   │   └── Layout.vue
│   ├── user/          # 用户管理
│   │   └── UserManagement.vue
│   ├── role/          # 角色管理
│   │   └── RoleManagement.vue
│   └── chat/          # 智能客服
│       └── Chatbot.vue
├── App.vue            # 根组件
└── main.js            # 入口文件
```

## 快速开始

### 环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

### 安装依赖

```bash
npm install
```

### 开发环境

```bash
npm run dev
```

### 构建生产版本

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 权限说明

系统包含以下权限类型：

### 用户管理权限
- `user:create` - 添加用户
- `user:update` - 修改用户
- `user:delete` - 删除用户
- `user:view` - 查看用户

### 角色管理权限
- `role:create` - 添加角色
- `role:update` - 修改角色
- `role:delete` - 删除角色
- `role:view` - 查看角色

### 系统管理权限
- `system:config` - 系统配置
- `system:log` - 系统日志
- `system:backup` - 数据备份

## 开发说明

### API接口

所有API接口都通过 `src/api/` 目录下的文件进行管理，使用统一的请求拦截器处理认证和错误。

### 路由守卫

系统使用Vue Router的导航守卫进行权限控制，未登录用户会被重定向到登录页面。

### 状态管理

用户信息和认证状态通过localStorage进行持久化存储。

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 许可证

MIT License