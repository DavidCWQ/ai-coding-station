<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { TableColumnType, TableProps } from 'ant-design-vue'

import { getStatsOverview, type StatsOverview } from '@/api/statsController'

const RANK_PAGE_SIZE = 10

const days = ref<7 | 30>(7)
const loading = ref(false)
const overview = ref<StatsOverview | null>(null)
const rankPage = ref(1)

const columns: TableColumnType[] = [
  { title: '排名', key: 'rank', width: 72 },
  { title: '应用', dataIndex: 'appName', key: 'appName', ellipsis: true },
  { title: '浏览量', dataIndex: 'pv', key: 'pv', width: 120 },
  { title: '独立访客', dataIndex: 'uv', key: 'uv', width: 120 },
]

const trend = computed(() => overview.value?.trend ?? [])
const ranking = computed(() => overview.value?.ranking ?? [])
const activeIndex = ref<number | null>(null)

const countText = (value: number) => Number(value).toLocaleString('zh-CN')

type PlotPoint = { x: number; y: number }

/** 过每个点的单调三次曲线，拐弯不冲出相邻点的高低。 */
const smoothPath = (points: PlotPoint[]) => {
  if (points.length === 0) return ''
  const first = points[0]
  if (!first) return ''
  if (points.length === 1) return `M ${first.x} ${first.y}`
  const slopes: number[] = []
  for (let i = 0; i < points.length - 1; i += 1) {
    const current = points[i]
    const next = points[i + 1]
    if (!current || !next) continue
    const dx = next.x - current.x
    slopes.push(dx === 0 ? 0 : (next.y - current.y) / dx)
  }
  const tangents = points.map((_, index) => {
    if (index === 0) return slopes[0] ?? 0
    if (index === points.length - 1) return slopes[slopes.length - 1] ?? 0
    const left = slopes[index - 1] ?? 0
    const right = slopes[index] ?? 0
    if (left * right <= 0) return 0
    return (left + right) / 2
  })
  for (let i = 0; i < slopes.length; i += 1) {
    const slope = slopes[i] ?? 0
    if (slope === 0) {
      tangents[i] = 0
      tangents[i + 1] = 0
      continue
    }
    const a = (tangents[i] ?? 0) / slope
    const b = (tangents[i + 1] ?? 0) / slope
    const size = a * a + b * b
    if (size > 9) {
      const scale = 3 / Math.sqrt(size)
      tangents[i] = scale * a * slope
      tangents[i + 1] = scale * b * slope
    }
  }
  const round = (value: number) => Math.round(value * 10) / 10
  let path = `M ${round(first.x)} ${round(first.y)}`
  for (let i = 0; i < points.length - 1; i += 1) {
    const current = points[i]
    const next = points[i + 1]
    if (!current || !next) continue
    const dx = next.x - current.x
    const c1x = current.x + dx / 3
    const c1y = current.y + (tangents[i] ?? 0) * dx / 3
    const c2x = next.x - dx / 3
    const c2y = next.y - (tangents[i + 1] ?? 0) * dx / 3
    path += ` C ${round(c1x)} ${round(c1y)}, ${round(c2x)} ${round(c2y)}, ${round(next.x)} ${round(next.y)}`
  }
  return path
}

const areaPath = (line: string, points: PlotPoint[], bottom: number) => {
  const first = points[0]
  const last = points[points.length - 1]
  if (!line || !first || !last) return ''
  return `${line} L ${last.x} ${bottom} L ${first.x} ${bottom} Z`
}

const formatTrendDate = (iso: string, withYear: boolean) => {
  const [year, month, day] = iso.split('-')
  if (!year || !month || !day) return iso
  const text = `${Number(month)}月${Number(day)}日`
  return withYear ? `${year}年${text}` : text
}

const rankPagination = computed(() => ({
  current: rankPage.value,
  pageSize: RANK_PAGE_SIZE,
  total: ranking.value.length,
  hideOnSinglePage: true,
  showSizeChanger: false,
}))

const onRankChange: TableProps['onChange'] = (pag) => {
  rankPage.value = pag.current ?? 1
}

const chart = computed(() => {
  const points = trend.value
  const width = 640
  const height = 220
  const padLeft = 44
  const padRight = 16
  const padTop = 16
  const plotBottom = height - 8
  const plotRight = width - padRight
  const max = Math.max(1, ...points.map((point) => Math.max(point.pv, point.uv)))
  const step = points.length <= 1 ? 0 : (plotRight - padLeft) / (points.length - 1)
  const withYear = new Set(points.map((point) => point.date.slice(0, 4))).size > 1
  const yOf = (value: number) => padTop + (1 - value / max) * (plotBottom - padTop)
  const xs = points.map((_, index) => padLeft + index * step)
  const items = points.map((point, index) => {
    const x = xs[index] ?? padLeft
    const prev = index === 0 ? padLeft : ((xs[index - 1] ?? x) + x) / 2
    const next = index === points.length - 1 ? plotRight : (x + (xs[index + 1] ?? x)) / 2
    return {
      x,
      yPv: yOf(point.pv),
      yUv: yOf(point.uv),
      pvText: countText(point.pv),
      uvText: countText(point.uv),
      dateText: formatTrendDate(point.date, withYear),
      hitX: prev,
      hitWidth: Math.max(next - prev, 1),
    }
  })
  const pvPoints = items.map((item) => ({ x: item.x, y: item.yPv }))
  const uvPoints = items.map((item) => ({ x: item.x, y: item.yUv }))
  const pvLine = smoothPath(pvPoints)
  const uvLine = smoothPath(uvPoints)
  const mid = padTop + (plotBottom - padTop) / 2
  return {
    width,
    height,
    padLeft,
    padTop,
    plotBottom,
    plotRight,
    max: countText(max),
    gridYs: [padTop, mid, plotBottom],
    pvLine,
    uvLine,
    pvArea: areaPath(pvLine, pvPoints, plotBottom),
    uvArea: areaPath(uvLine, uvPoints, plotBottom),
    items,
  }
})

const active = computed(() => {
  const index = activeIndex.value
  if (index == null) return null
  return chart.value.items[index] ?? null
})

const onPlotLeave = (event: PointerEvent) => {
  if (event.pointerType === 'touch') return
  activeIndex.value = null
}

const chartLabel = computed(() => {
  if (!trend.value.length) return '暂无趋势'
  const first = trend.value[0]?.date ?? ''
  const last = trend.value[trend.value.length - 1]?.date ?? ''
  return `${first} 至 ${last} 的全站浏览量和独立访客`
})

const load = async () => {
  loading.value = true
  try {
    const res = await getStatsOverview(days.value)
    overview.value = res.data?.data ?? null
  } catch {
    overview.value = null
  } finally {
    loading.value = false
  }
}

watch(days, () => {
  rankPage.value = 1
  activeIndex.value = null
  void load()
})

watch(trend, () => {
  activeIndex.value = null
})

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="visit-stats">
    <a-card title="访问统计" :bordered="false">
      <template #extra>
        <a-radio-group v-model:value="days" button-style="solid">
          <a-radio-button :value="7">近 7 日</a-radio-button>
          <a-radio-button :value="30">近 30 日</a-radio-button>
        </a-radio-group>
      </template>

      <a-spin :spinning="loading">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="8">
            <a-statistic title="今日浏览量" :value="overview?.todayPv ?? 0" />
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-statistic title="今日独立访客" :value="overview?.todayUv ?? 0" />
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-statistic title="当前在线" :value="overview?.online ?? 0" />
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-statistic :title="`近 ${days} 日独立访客`" :value="overview?.periodUv ?? 0" />
          </a-col>
          <a-col :xs="24" :sm="8">
            <a-statistic title="其中回访" :value="overview?.returningUv ?? 0" />
          </a-col>
        </a-row>
        <p class="visit-stats__note">
          全站数字只含首页、关于和应用工坊。应用详情单独计入下方排行。在线人数是最近 5 分钟内有心跳的会话，概览最多缓存 1 分钟。近 {{ days }} 日的独立访客按人合并，同一个人来多天只计一次；回访是至少来过两天的人。明细只保存 IP 与浏览器的摘要，不留原始地址，所以不按地区拆分。
        </p>

        <h3 class="visit-stats__heading">全站趋势</h3>
        <div class="visit-stats__chart">
          <div class="visit-stats__plot">
            <svg
              :viewBox="`0 0 ${chart.width} ${chart.height}`"
              role="img"
              :aria-label="chartLabel"
              @pointerleave="onPlotLeave"
            >
              <defs>
                <linearGradient id="visit-stats-pv-fill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#1677ff" stop-opacity="0.28" />
                  <stop offset="100%" stop-color="#1677ff" stop-opacity="0.02" />
                </linearGradient>
                <linearGradient id="visit-stats-uv-fill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#52c41a" stop-opacity="0.22" />
                  <stop offset="100%" stop-color="#52c41a" stop-opacity="0.02" />
                </linearGradient>
              </defs>
              <text :x="chart.padLeft - 8" :y="chart.padTop + 4" text-anchor="end" class="visit-stats__label">
                {{ chart.max }}
              </text>
              <text :x="chart.padLeft - 8" :y="chart.plotBottom" text-anchor="end" class="visit-stats__label">0</text>
              <line
                v-for="(y, index) in chart.gridYs"
                :key="`grid-${index}`"
                :x1="chart.padLeft"
                :x2="chart.plotRight"
                :y1="y"
                :y2="y"
                class="visit-stats__grid"
              />
              <rect
                v-if="active"
                :x="active.hitX"
                :y="chart.padTop"
                :width="active.hitWidth"
                :height="chart.plotBottom - chart.padTop"
                class="visit-stats__band"
              />
              <line
                v-if="active"
                :x1="active.x"
                :x2="active.x"
                :y1="chart.padTop"
                :y2="chart.plotBottom"
                class="visit-stats__guide"
              />
              <path v-if="chart.pvArea" :d="chart.pvArea" fill="url(#visit-stats-pv-fill)" class="visit-stats__area" />
              <path v-if="chart.uvArea" :d="chart.uvArea" fill="url(#visit-stats-uv-fill)" class="visit-stats__area" />
              <path :d="chart.pvLine" class="visit-stats__line visit-stats__line--pv" />
              <path :d="chart.uvLine" class="visit-stats__line visit-stats__line--uv" />
              <template v-for="(item, index) in chart.items" :key="`dot-${index}`">
                <circle
                  :cx="item.x"
                  :cy="item.yPv"
                  :r="activeIndex === index ? 5 : 3"
                  class="visit-stats__dot visit-stats__dot--pv"
                  :class="{ 'visit-stats__dot--active': activeIndex === index }"
                />
                <circle
                  :cx="item.x"
                  :cy="item.yUv"
                  :r="activeIndex === index ? 5 : 3"
                  class="visit-stats__dot visit-stats__dot--uv"
                  :class="{ 'visit-stats__dot--active': activeIndex === index }"
                />
              </template>
              <rect
                v-for="(item, index) in chart.items"
                :key="`hit-${index}`"
                :x="item.hitX"
                :y="chart.padTop"
                :width="item.hitWidth"
                :height="chart.plotBottom - chart.padTop"
                class="visit-stats__hit"
                @pointerenter="activeIndex = index"
                @pointerdown="activeIndex = index"
              />
            </svg>
            <div
              v-if="active"
              class="visit-stats__tip"
              :class="{ 'visit-stats__tip--flip': active.x > chart.width * 0.68 }"
              :style="{ left: `${(active.x / chart.width) * 100}%` }"
              role="tooltip"
            >
              <div class="visit-stats__tip-date">{{ active.dateText }}</div>
              <div class="visit-stats__tip-row">
                <i class="visit-stats__swatch visit-stats__swatch--pv" />
                <span>浏览量</span>
                <strong>{{ active.pvText }}</strong>
              </div>
              <div class="visit-stats__tip-row">
                <i class="visit-stats__swatch visit-stats__swatch--uv" />
                <span>独立访客</span>
                <strong>{{ active.uvText }}</strong>
              </div>
            </div>
          </div>
          <div class="visit-stats__legend">
            <span><i class="visit-stats__swatch visit-stats__swatch--pv" />浏览量</span>
            <span><i class="visit-stats__swatch visit-stats__swatch--uv" />独立访客</span>
          </div>
        </div>

        <h3 class="visit-stats__heading">应用访问排行</h3>
        <a-table
          row-key="appId"
          :columns="columns"
          :data-source="ranking"
          :pagination="rankPagination"
          @change="onRankChange"
          :locale="{ emptyText: '这段时间还没有应用访问' }"
        >
          <template #bodyCell="{ column, index, text }">
            <template v-if="column.key === 'rank'">{{ (rankPage - 1) * RANK_PAGE_SIZE + index + 1 }}</template>
            <template v-else-if="column.key === 'pv' || column.key === 'uv'">
              {{ Number(text).toLocaleString('zh-CN') }}
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.visit-stats__note {
  margin: 16px 0 0;
  color: rgba(0, 0, 0, 0.45);
}

.visit-stats__heading {
  margin: 24px 0 12px;
  font-size: 16px;
}

.visit-stats__chart {
  overflow-x: auto;
}

.visit-stats__plot {
  position: relative;
  min-width: 520px;
}

.visit-stats__chart svg {
  display: block;
  width: 100%;
  height: auto;
}

.visit-stats__grid {
  stroke: rgba(0, 0, 0, 0.08);
}

.visit-stats__area {
  pointer-events: none;
}

.visit-stats__band {
  fill: rgba(22, 119, 255, 0.06);
}

.visit-stats__guide {
  stroke: rgba(0, 0, 0, 0.35);
  stroke-dasharray: 3 3;
  pointer-events: none;
}

.visit-stats__hit {
  fill: transparent;
  cursor: crosshair;
}

.visit-stats__line {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.visit-stats__line--pv,
.visit-stats__dot--pv {
  stroke: #1677ff;
}

.visit-stats__line--uv,
.visit-stats__dot--uv {
  stroke: #52c41a;
}

.visit-stats__dot--pv {
  fill: #1677ff;
}

.visit-stats__dot--uv {
  fill: #52c41a;
}

.visit-stats__dot--active {
  stroke: #fff;
  stroke-width: 2;
}

.visit-stats__tip-row .visit-stats__swatch {
  margin-right: 0;
}

.visit-stats__tip {
  position: absolute;
  top: 0;
  z-index: 2;
  min-width: 148px;
  padding: 8px 10px;
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
  line-height: 1.7;
  pointer-events: none;
  transform: translateX(12px);
}

.visit-stats__tip--flip {
  transform: translateX(calc(-100% - 12px));
}

.visit-stats__tip-date {
  margin-bottom: 2px;
  color: rgba(0, 0, 0, 0.88);
  font-weight: 600;
}

.visit-stats__tip-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.visit-stats__tip-row strong {
  margin-left: auto;
  color: rgba(0, 0, 0, 0.88);
  font-variant-numeric: tabular-nums;
}

.visit-stats__label {
  fill: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

.visit-stats__legend {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.65);
}

.visit-stats__swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border-radius: 2px;
  vertical-align: -1px;
}

.visit-stats__swatch--pv {
  background: #1677ff;
}

.visit-stats__swatch--uv {
  background: #52c41a;
}
</style>
