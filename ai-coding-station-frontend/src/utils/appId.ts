/**
 * 应用 ID 为雪花 Long，超出 JS Number 安全整数。
 * 路由与请求须全程使用字符串；OpenAPI 生成类型多为 number，用断言绕过类型，运行时不要 Number(id)。
 */
export function assertValidAppId(appId: string): asserts appId is string {
  if (!appId || !/^\d+$/.test(appId)) {
    throw new Error('非法应用 ID')
  }
}

/**
 * 传给「签名为 number」的 OpenAPI 参数：URL 模板或 JSON 里实际写入字符串，避免精度丢失。
 */
export function apiLongId(appId: string): number {
  assertValidAppId(appId)
  return appId as unknown as number
}

export function appIdFromData(id: number | string | undefined | null): string {
  if (id === undefined || id === null) return ''
  return String(id)
}
