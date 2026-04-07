import { loadEnv } from 'vite'

declare const process: {
  cwd?: () => string
  env?: Record<string, string | undefined>
}

const mode = process.env?.NODE_ENV || 'development'
const envFromFile = loadEnv(mode, process.cwd?.() || '.', '')

const schemaPath =
  envFromFile.OPENAPI_SCHEMA_URL ||
  process.env?.OPENAPI_SCHEMA_URL ||
  'http://127.0.0.1:8142/api/v3/api-docs'

export default {
  // 生成代码中 使用的请求方法封装
  requestLibPath: "import request from '@/request'",
  // 后端 OpenAPI / Swagger 文档地址（优先读取环境变量）
  schemaPath,
  // 生成代码的 输出目录
  serversPath: './src',
  // 生成代码的 项目名称
  projectName: 'api',
}
