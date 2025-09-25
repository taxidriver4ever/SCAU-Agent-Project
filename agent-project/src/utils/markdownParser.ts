// src/utils/markdownParser.ts
import { marked } from 'marked';

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
});

export function parseMarkdown(markdown: string): string {
  if (!markdown) return '';
  return marked.parse(markdown) as string;
}