import { describe, expect, it } from 'vitest'
import { parseFilenameFromContentDisposition, sanitizeDownloadFilename } from './fileDownload'

describe('parseFilenameFromContentDisposition', () => {
  it('parses RFC5987 utf8 filename', () => {
    const header = "attachment; filename*=UTF-8''%E6%B5%8B%E8%AF%95.zip"
    expect(parseFilenameFromContentDisposition(header)).toBe('测试.zip')
  })

  it('parses quoted filename', () => {
    const header = 'attachment; filename="report 2026.pdf"'
    expect(parseFilenameFromContentDisposition(header)).toBe('report 2026.pdf')
  })

  it('returns null for invalid header', () => {
    expect(parseFilenameFromContentDisposition(undefined)).toBeNull()
    expect(parseFilenameFromContentDisposition('inline')).toBeNull()
  })
})

describe('sanitizeDownloadFilename', () => {
  it('replaces unsupported symbols and trims', () => {
    expect(sanitizeDownloadFilename('  report:/\\*?.zip  ')).toBe('report_____.zip')
  })
})
