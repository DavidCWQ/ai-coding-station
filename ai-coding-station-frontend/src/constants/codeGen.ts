/** 与后端 CodeGenTypeEnum.value 一致；创建应用时含 "MULTI" 会识别为多文件模式 */
export const CODE_GEN_HTML = 'html' as const
export const CODE_GEN_MULTI_FILE = 'multi_file' as const

export type CodeGenTypeValue = typeof CODE_GEN_HTML | typeof CODE_GEN_MULTI_FILE
