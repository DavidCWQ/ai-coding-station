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
  return {
    width,
    height,
    padLeft,
    padTop,
    plotBottom,
    max: countText(max),
    pvLine: items.map((item) => `${item.x},${item.yPv}`).join(' '),
    uvLine: items.map((item) => `${item.x},${item.yUv}`).join(' '),
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
        </a-row>
        <p class="visit-stats__note">
          全站数字只含首页、关于和应用工坊。应用详情单独计入下方排行。在线人数是最近 5 分钟内有心跳的会话，概览最多缓存 1 分钟。
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
              <text :x="chart.padLeft - 8" :y="chart.padTop + 4" text-anchor="end" class="visit-stats__label">
                {{ chart.max }}
              </text>
              <text :x="chart.padLeft - 8" :y="chart.plotBottom" text-anchor="end" class="visit-stats__label">0</text>
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
              <polyline :points="chart.pvLine" class="visit-stats__line visit-stats__line--pv" />
              <polyline :points="chart.uvLine" class="visit-stats__line visit-stats__line--uv" />
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
  stroke-width: 2;
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
