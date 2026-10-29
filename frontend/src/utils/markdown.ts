import { marked } from 'marked'

/**
 * 把 Markdown 文本渲染成 HTML，用于展示 AI 生成的结构化内容（如评估报告的优势/不足/建议）。
 *
 * 关键配置：
 *  - breaks: true  单个换行也转成 <br>，否则 AI 返回的单换行文本会挤在一起
 *  - gfm: true     支持 GitHub 风格 Markdown（列表、加粗等）
 */
export function renderMarkdown(text: unknown): string {
  if (typeof text !== 'string' || !text) return ''
  return marked.parse(text, { breaks: true, gfm: true }) as string
}
