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

const BASE_URL = import.meta.env.VITE_APP_API_BASE_URL ?? 'http://localhost:8142/api'

const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 60_000,
  withCredentials: true,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 可在此统一附加 token、traceId 等
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response

    // 未登录
    if (data?.code === 40100) {
      const isGetLogin = response.request?.responseURL?.includes?.('user/get/login')
      const inLoginPage = window.location.pathname.includes('/user/login')

      if (!isGetLogin && !inLoginPage) {
        message.warning('请先登录')
        const redirect = encodeURIComponent(window.location.href)
        window.location.href = `/user/login?redirect=${redirect}`
      }
    } else if (data?.code && data.code !== 0 && data.code !== 200) {
      // 通用错误提示（根据项目约定调整成功 code）
      if (data.message) {
        message.error(data.message)
      }
    }

    return response
  },
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

