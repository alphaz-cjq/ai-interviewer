
/// <reference types="vite/client" />

// typeScript 不知道如何识别 .vue 文件（它默认只认识 .ts 和 .js 文件）。
//添加一个 类型声明文件（env.d.ts），告诉 TypeScript：“遇到 .vue 文件时，当成 Vue 组件来处理。”
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}