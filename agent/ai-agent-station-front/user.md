# AI Agent Station 前端 - 用户说明

## 项目简介

AI Agent Station 前端是基于 React + Rsbuild 的 Web 应用，使用 Semi Design 组件库，用于 AI 智能体工作台相关功能。

## 环境要求

- **Node.js**：建议 18.x 或以上
- **包管理器**：npm / yarn / pnpm 任选其一

## 安装与运行

### 1. 安装依赖

在项目根目录执行：

```bash
npm install
```

或使用 yarn / pnpm：

```bash
yarn
# 或
pnpm install
```

### 2. 启动开发环境

安装完成后，执行以下任一命令启动开发服务器（会默认打开浏览器）：

```bash
npm run dev
```

或：

```bash
npm start
```

- `npm run dev`：以 `MODE=app` 启动，适合应用模式开发
- `npm start`：常规开发模式启动

开发服务器启动后，在浏览器中访问控制台提示的地址（通常为 `http://localhost:3000`）即可。

## 常用脚本

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发模式启动（MODE=app，并自动打开浏览器） |
| `npm start` | 开发模式启动（自动打开浏览器） |
| `npm run lint` | 执行 ESLint 检查 |
| `npm run lint:fix` | 执行 ESLint 并自动修复 |
| `npm run clean` | 清理构建产物（dist） |

## 技术栈

- **框架**：React 18
- **构建**：Rsbuild
- **UI**：Semi Design（@douyinfe/semi-ui）
- **路由**：React Router v6
- **样式**：Less、styled-components

## 入口与配置

- 应用入口：`src/app.tsx`
- 构建配置：`rsbuild.config.ts`
- HTML 模板：`index.html`

如有问题可先查看控制台报错或联系项目维护者。
