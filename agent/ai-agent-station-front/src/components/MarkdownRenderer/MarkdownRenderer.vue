<template>
  <div
    class="markdown-body prose prose-slate dark:prose-invert max-w-none"
    v-html="renderedHtml"
    @click="handleCopyClick"
  />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
// 基础 markdown 样式（需在入口或布局中引入：import 'github-markdown-css/github-markdown.css'）
// 若使用 Tailwind，可安装 @tailwindcss/typography 并在 tailwind.config 中启用，此处已使用 prose 类做二次微调

// 扩展：公式支持（按需取消注释并安装对应包）
// import markdownItMathjax3 from 'markdown-it-mathjax3';
// 或
// import markdownItKatex from 'markdown-it-katex';

function escapeHtml(html: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return html.replace(/[&<>"']/g, (ch) => map[ch] ?? ch);
}

const props = withDefaults(
  defineProps<{
    /** 流式/静态的 markdown 文本 */
    content?: string;
    /** 是否高亮代码，默认 true */
    highlight?: boolean;
  }>(),
  { content: '', highlight: true }
);

const highlightEnabled = ref(props.highlight);
watch(
  () => props.highlight,
  (v) => {
    highlightEnabled.value = v ?? true;
  },
  { immediate: true }
);

// markdown-it 实例：只创建一次，流式时仅重新 render，避免重复创建
const md = (() => {
  const m = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
    highlight(str: string, lang: string) {
      if (!highlightEnabled.value || !lang) {
        return escapeHtml(str);
      }
      try {
        const { value } = hljs.highlight(str, {
          language: hljs.getLanguage(lang) ? lang : 'plaintext',
          ignoreIllegals: true,
        });
        return value;
      } catch {
        return escapeHtml(str);
      }
    },
  });

  const defaultFence = m.renderer.rules.fence!;
  m.renderer.rules.fence = (tokens, idx, options, env, self) => {
    const token = tokens[idx];
    const lang = (token.info?.trim() || '').split(/\s+/)[0] || 'text';
    const content = defaultFence(tokens, idx, options, env, self);
    return `<div class="code-block-wrap" data-lang="${escapeHtml(lang)}">
  <div class="code-block-header">
    <span class="code-block-lang">${escapeHtml(lang)}</span>
    <button type="button" class="code-block-copy" data-copy-code title="复制代码">复制</button>
  </div>
  ${content}
</div>`;
  };

  // 扩展：公式插件（按需取消注释）
  // m.use(markdownItMathjax3);
  // 或
  // m.use(markdownItKatex);

  return m;
})();

// 使用 computed 处理渲染，流式场景下 content 频繁变动时只重算解析结果，避免多余副作用
const renderedHtml = computed(() => {
  if (!props.content?.trim()) return '';
  return md.render(props.content);
});

// 复制按钮点击委托（事件委托到容器，避免为每个代码块绑定）
function handleCopyClick(e: MouseEvent) {
  const target = e.target as HTMLElement;
  const btn = target.closest('[data-copy-code]');
  if (!btn) return;
  e.preventDefault();
  const pre = btn.closest('.code-block-wrap')?.querySelector('pre code');
  if (!pre) return;
  const text = pre.textContent ?? '';
  copyToClipboard(text).then(() => {
    btn.setAttribute('data-copied', 'true');
    btn.textContent = '已复制';
    setTimeout(() => {
      btn.removeAttribute('data-copied');
      btn.textContent = '复制';
    }, 2000);
  });
}

function copyToClipboard(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    return navigator.clipboard.writeText(text);
  }
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  try {
    document.execCommand('copy');
    return Promise.resolve();
  } finally {
    document.body.removeChild(ta);
  }
}

</script>

<style scoped>
/* 若未使用 Tailwind，可去掉 prose 相关类，仅保留 markdown-body */
.markdown-body {
  box-sizing: border-box;
  min-width: 200px;
  max-width: 980px;
  margin: 0 auto;
  padding: 16px;
}

/* 代码块容器：复制按钮 + 语言标签 */
.code-block-wrap {
  position: relative;
  margin: 1em 0;
  border-radius: 8px;
  overflow: hidden;
  background: var(--code-bg, #0d1117);
}

.code-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 12px;
  color: var(--code-header-text, #8b949e);
  background: var(--code-header-bg, #161b22);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.code-block-lang {
  text-transform: capitalize;
}

.code-block-copy {
  padding: 4px 10px;
  font-size: 12px;
  color: var(--code-copy-text, #8b949e);
  cursor: pointer;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.code-block-copy:hover {
  color: #58a6ff;
  background: rgba(255, 255, 255, 0.05);
}

.code-block-copy[data-copied='true'] {
  color: #3fb950;
}

.code-block-wrap :deep(pre) {
  margin: 0;
  padding: 1em;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
}

.code-block-wrap :deep(pre code) {
  padding: 0;
  background: transparent;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
}

/* 与 github-markdown-css 协同；若用了 Tailwind prose，会进一步覆盖部分样式 */
.markdown-body :deep(pre) {
  background: var(--code-bg, #0d1117);
}
</style>
