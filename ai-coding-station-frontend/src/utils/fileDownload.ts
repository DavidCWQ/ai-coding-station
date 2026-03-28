/**
 * 从 Content-Disposition 解析文件名（支持 filename="..." 与 RFC 5987 filename*=UTF-8''...）
 */
export function parseFilenameFromContentDisposition(header: string | undefined): string | null {
  if (!header || typeof header !== 'string') return null
  const utf8 = /filename\*=UTF-8''([^;\s]+)/i.exec(header)
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1].replace(/['"]/g, ''))
    } catch {
      return utf8[1]
    }
  }
  const quoted = /filename\s*=\s*"((?:\\.|[^"\\])*)"/i.exec(header)
  if (quoted?.[1]) {
    return quoted[1].replace(/\\(.)/g, '$1')
  }
  const simple = /filename\s*=\s*([^;\s]+)/i.exec(header)
  if (simple?.[1]) {
    return simple[1].replace(/^["']|["']$/g, '')
  }
  return null
}

/** 去掉常见非法文件名字符，避免触发下载失败 */
export function sanitizeDownloadFilename(name: string): string {
  return name
    .replace(/[/\\?%*:|"<>]/g, '_')
    .trim()
    .slice(0, 200)
}

export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
