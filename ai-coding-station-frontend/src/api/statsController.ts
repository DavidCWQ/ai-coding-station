import request, { type ApiResponse } from '@/request'

export type StatsTrendPoint = {
  date: string
  pv: number
  uv: number
}

export type StatsAppRank = {
  appId: string
  appName: string
  pv: number
  uv: number
}

export type StatsOverview = {
  todayPv: number
  todayUv: number
  online: number
  days: number
  trend: StatsTrendPoint[]
  ranking: StatsAppRank[]
}

export function getStatsOverview(days: 7 | 30) {
  return request.get<ApiResponse<StatsOverview>>('/stats/admin/overview', {
    params: { days },
  })
}
