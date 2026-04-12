# MarkdownRenderer

Vue 3 的 Markdown 流式渲染组件，支持代码高亮与一键复制（类似 Gemini/ChatGPT 的聊天展示）。

## 安装依赖

在**使用 Vue 3** 的项目中安装：

```bash
npm i markdown-it highlight.js github-markdown-css
# 若使用 Tailwind 并希望用 prose 排版：
npm i -D @tailwindcss/typography
```

可选（公式支持，按需其一）：

```bash
npm i markdown-it-mathjax3
# 或
npm i markdown-it-katex
```

## 使用

1. 在入口或布局中引入 GitHub 样式（若不用 Tailwind prose，建议引入）：

```ts
import 'github-markdown-css/github-markdown.css';
```

2. 在页面中引用组件：

```vue
<template>
  <MarkdownRenderer :content="streamingContent" />
</template>

<script setup lang="ts">
import { MarkdownRenderer } from '@/components/MarkdownRenderer';

const streamingContent = ref('');
// 流式更新 streamingContent 即可
</script>
```

## Props

| 属性       | 类型    | 默认值  | 说明               |
| ---------- | ------- | ------- | ------------------ |
| `content`  | `string`| `''`    | 流式/静态 Markdown |
| `highlight`| `boolean`| `true` | 是否启用代码高亮   |

## 说明

- 当前仓库主技术栈为 **React**，若在本仓库内使用本组件，需要先接入 Vue 与上述依赖；或可将本组件复制到已有的 Vue 3 项目中使用。
