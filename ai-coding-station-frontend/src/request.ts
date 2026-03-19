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

const myAxios: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 60_000,
  withCredentials: true,
})

myAxios.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 可在此统一附加 token、traceId 等
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

myAxios.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response

    if (!data) {
      return response
    }

    // 未登录
    if (data.code === 40100) {
      const isGetLogin = response.request?.responseURL?.includes?.('user/get/login')
      const inLoginPage = window.location.pathname.includes('/user/login')

      if (!isGetLogin && !inLoginPage) {
        message.warning('请先登录')
        const redirect = encodeURIComponent(window.location.href)
        window.location.href = `/user/login?redirect=${redirect}`
      }

      return response
    }

    // 统一错误提示（根据实际 code 定义）
    if (data.code && data.code !== 0 && data.code !== 200) {
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

export default myAxios
