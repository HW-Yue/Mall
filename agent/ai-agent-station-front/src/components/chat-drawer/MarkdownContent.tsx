import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export interface MarkdownContentProps {
  content: string;
  className?: string;
}

/**
 * Markdown 渲染：使用 react-markdown + remark-gfm，支持 GFM（列表、表格、任务列表等），
 * 嵌套列表兼容 2/4 空格缩进。样式由 Tailwind Typography (prose) 提供。
 */
export const MarkdownContent: React.FC<MarkdownContentProps> = ({ content, className }) => {
  const wrapClassName = [
    'prose',
    'prose-slate',
    'dark:prose-invert',
    'max-w-none',
    'break-words',
    'markdown-content',
    // 正文颜色：提升对比度
    'prose-p:text-slate-800 dark:prose-p:text-slate-200',
    'prose-li:text-slate-800 dark:prose-li:text-slate-200',
    'prose-blockquote:text-slate-800 dark:prose-blockquote:text-slate-200',
    // 标题与加粗：纯黑/纯白，阅读更清晰
    'prose-headings:text-black dark:prose-headings:text-white',
    'prose-strong:text-black dark:prose-strong:text-white',
    // 行间距微调，避免拥挤
    'prose-p:leading-relaxed prose-li:leading-relaxed prose-headings:leading-snug',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={wrapClassName}>
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content ?? ''}</ReactMarkdown>
    </div>
  );
};
