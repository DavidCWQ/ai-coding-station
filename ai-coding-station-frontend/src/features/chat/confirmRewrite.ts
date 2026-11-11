import { Modal } from 'ant-design-vue'

export function confirmRewrite(): Promise<boolean> {
  return new Promise((resolve) => {
    let settled = false
    const finish = (value: boolean) => {
      if (settled) return
      settled = true
      resolve(value)
    }
    Modal.confirm({
      title: '从这里重新开始',
      content: '将从这里重新生成，之后的对话会被去掉。',
      okText: '继续',
      cancelText: '取消',
      centered: true,
      onOk: () => finish(true),
      onCancel: () => finish(false),
    })
  })
}
