import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { message } from 'ant-design-vue'

export interface ApiResponse<T = any> {
  code: number
  data: T
  message?: string
  [key: string]: unknown
}

const BASE_URL = import.meta.env.VITE_APP_API_BASE_URL ?? '/api'

const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 60_000,
  withCredentials: true,
})

// 请求拦截器，发请求前统一拦截
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 可在此统一附加 token、traceId 等
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// 响应拦截器，所有接口返回都会先经过这里
request.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const res = response.data

    // 未登录（统一处理：除了获取用户信息外，跳转到登录页）
    if (res?.code === 40100) {
      const isGetLogin = response.request?.responseURL?.includes?.('user/get/login')
      const inLoginPage = window.location.pathname.includes('/user/login')

      if (!isGetLogin && !inLoginPage) {
        message.warning('请先登录')
        const redirect = encodeURIComponent(window.location.href)
        window.location.href = `/user/login?redirect=${redirect}`
      }

      return Promise.reject(res)
    }

    // code 不是成功态：统一提示并让调用方进入 catch
    const code = res?.code
    const isSuccess = code === undefined || code === 0 || code === 200

    if (!isSuccess) {
      if (res?.message) message.error(res.message)
      return Promise.reject(res)
    }

    // 成功：返回完整结构
    return response
  },
  // HTTP 层错误 (500/Internet)
  (error: AxiosError<ApiResponse>) => {
    const resp = error.response
    if (resp?.data?.message) {
      message.error(resp.data.message)
    } else if (error.message) {
      message.error(error.message)
    }
    return Promise.reject(error)
  },
)

export default request

