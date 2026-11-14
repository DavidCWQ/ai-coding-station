<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType, TableProps } from 'ant-design-vue'
import dayjs from 'dayjs'

import { adminDeleteApp, adminUpdateApp, listApp } from '@/api/appAdminController'
import { deleteApp, listMyApps } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'

const FEATURED_PRIORITY = 99
const PINNED_PRIORITY = 999

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any
const loginUserStore = useLoginUserStore()

const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

/** 与后端一致：传 priority 时表示 priority >= 该值（普通≥0、精选≥99、置顶≥999） */
const PRIORITY_FILTER_ALL = 'all' as const
const PRIORITY_FILTER_NORMAL = 0
const PRIORITY_FILTER_FEATURED = FEATURED_PRIORITY
const PRIORITY_FILTER_PINNED = PINNED_PRIORITY

type PriorityFilterValue = typeof PRIORITY_FILTER_ALL | number

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  userId: '' as string,
  /** 按优先级下限筛选；'all' 表示不限 */
  priorityFilter: PRIORITY_FILTER_ALL as PriorityFilterValue,
})

const priorityFilterOptions = [
  { label: '不限', value: PRIORITY_FILTER_ALL },
  { label: '普通', value: PRIORITY_FILTER_NORMAL },
  { label: '精选', value: PRIORITY_FILTER_FEATURED },
  { label: '置顶', value: PRIORITY_FILTER_PINNED },
] as const

const allColumns: TableColumnType<API.AppVO>[] = [
  { title: 'ID', key: 'id', width: 100, ellipsis: true },
  { title: '应用名', dataIndex: 'appName', key: 'appName', ellipsis: true },
  { title: '用户ID', key: 'userId', width: 120 },
  { title: '优先级', key: 'priority', width: 96 },
  { title: '修改时间', key: 'updateTime', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' },
]

const columns = computed((): TableColumnType<API.AppVO>[] =>
  isAdmin.value ? allColumns : allColumns.filter((c) => c.key !== 'userId'),
)

type PriorityBtn = { text: string; priority: number; danger: boolean }

const getAdminPriorityButtons = (priority: number): PriorityBtn[] => {
  if (priority >= PINNED_PRIORITY) {
    return [
      { text: '精选', priority: FEATURED_PRIORITY, danger: false },
      { text: '取消置顶', priority: FEATURED_PRIORITY, danger: false },
    ]
  }
  if (priority >= FEATURED_PRIORITY) {
    return [
      { text: '取消精选', priority: 0, danger: false },
      { text: '置顶', priority: PINNED_PRIORITY, danger: false },
    ]
  }
  if (priority >= 0) {
    return [
      { text: '精选', priority: FEATURED_PRIORITY, danger: false },
      { text: '置顶', priority: PINNED_PRIORITY, danger: false },
    ]
  }
  return [{ text: '取消锁定', priority: 0, danger: false }]
}

const getPriorityInfo = (priority: number) => {
  let tag: { color: string; text: string }
  if (priority >= PINNED_PRIORITY) {
    tag = { color: 'gold', text: '置顶' }
  } else if (priority >= FEATURED_PRIORITY) {
    tag = { color: 'green', text: '精选' }
  } else if (priority >= 0) {
    tag = { color: 'gray', text: '普通' }
  } else {
    tag = { color: 'red', text: '锁定' }
  }
  const buttons = isAdmin.value ? getAdminPriorityButtons(priority) : []
  return { tag, buttons }
}

const pagination = computed<TableProps['pagination']>(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const fmt = (v?: string) => {
  if (!v) return '—'
  const d = dayjs(v)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : v
}

const priorityQueryPart = (): Pick<API.AppQueryRequest, 'priority'> | Record<string, never> => {
  const pf = searchParams.priorityFilter
  return typeof pf === 'number' ? { priority: pf } : {}
}

const buildAdminQuery = (): API.AppQueryRequest => {
  const uid = searchParams.userId.trim()
  return {
    id: 0,
    pageNum: searchParams.pageNum,
    pageSize: Math.min(searchParams.pageSize, 20),
    appName: searchParams.appName.trim() || undefined,
    userId: uid ? apiLongId(uid) : undefined,
    sortField: 'update_time',
    sortOrder: 'descend',
    ...priorityQueryPart(),
  }
}

const buildMyQuery = (): API.AppQueryRequest => ({
  id: 0,
  pageNum: searchParams.pageNum,
  pageSize: Math.min(searchParams.pageSize, 20),
  appName: searchParams.appName.trim() || undefined,
  sortField: 'update_time',
  sortOrder: 'descend',
  ...priorityQueryPart(),
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = isAdmin.value ? await listApp(buildAdminQuery()) : await listMyApps(buildMyQuery())
    const page = res.data?.data
    data.value = page?.records ?? []
    total.value = page?.totalRow ?? 0
  } catch (e) {
    message.error(getErrorMessage(e, '请求失败'))
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  void fetchData()
}

const onTableChange: TableProps['onChange'] = (pag) => {
  searchParams.pageNum = pag.current ?? 1
  searchParams.pageSize = pag.pageSize ?? searchParams.pageSize
  void fetchData()
}

const goEdit = (id: string) => {
  void router.push(`/app/edit/${id}`)
}

const doDelete = async (id: string) => {
  try {
    if (isAdmin.value) {
      await adminDeleteApp({ id: apiLongId(id) })
    } else {
      await deleteApp({ id: apiLongId(id) })
    }
    message.success('已删除')
    await fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '请求失败'))
  }
}

const setPriority = async (id: string, priority: number) => {
  try {
    await adminUpdateApp({
      id: apiLongId(id),
      priority,
    })
    message.success('已更新')
    await fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '请求失败'))
  }
}

onMounted(() => {
  void fetchData()
})
</script>

<template>
  <div class="app-manage">
    <a-card title="应用管理" :bordered="false">
      <a-form :model="searchParams" layout="inline" class="app-manage__search" @finish="doSearch">
        <a-form-item label="应用名">
          <a-input v-model:value="searchParams.appName" allow-clear class="app-manage__control" />
        </a-form-item>
        <a-form-item v-if="isAdmin" label="用户ID">
          <a-input v-model:value="searchParams.userId" allow-clear class="app-manage__control app-manage__control--id" />
        </a-form-item>
        <a-form-item label="优先级">
          <a-select
            v-model:value="searchParams.priorityFilter"
            class="app-manage__control app-manage__control--priority"
            :options="priorityFilterOptions"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" class="app-manage__submit">搜索</a-button>
        </a-form-item>
      </a-form>

      <div class="app-manage__cards">
        <article v-for="record in data" :key="idFromData(record.id)" class="app-manage__card">
          <div class="app-manage__card-top">
            <h3 class="app-manage__card-name">{{ record.appName || '未命名应用' }}</h3>
            <a-tag :color="getPriorityInfo(record.priority).tag.color">
              {{ getPriorityInfo(record.priority).tag.text }}
            </a-tag>
          </div>
          <p class="app-manage__card-meta">{{ fmt(record.updateTime || record.createTime) }}</p>
          <p v-if="isAdmin" class="app-manage__card-meta">用户 {{ record.userId != null ? String(record.userId) : '—' }}</p>
          <div class="app-manage__card-actions">
            <a-button @click="goEdit(idFromData(record.id))">编辑</a-button>
            <a-button
              v-for="btn in getPriorityInfo(record.priority).buttons"
              :key="btn.text"
              :danger="btn.danger"
              @click="setPriority(idFromData(record.id), btn.priority)"
            >
              {{ btn.text }}
            </a-button>
            <a-popconfirm title="确定删除？" @confirm="doDelete(idFromData(record.id))">
              <a-button danger>删除</a-button>
            </a-popconfirm>
          </div>
        </article>
        <a-empty v-if="!loading && data.length === 0" description="没有应用" />
      </div>

      <a-table
        class="app-manage__table"
        row-key="id"
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 960 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            {{ idFromData(record.id) }}
          </template>
          <template v-else-if="column.key === 'userId'">
            {{ record.userId != null ? String(record.userId) : '—' }}
          </template>
          <template v-else-if="column.key === 'priority'">
            <a-tag :color="getPriorityInfo(record.priority).tag.color">
              {{ getPriorityInfo(record.priority).tag.text }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'updateTime'">
            {{ fmt(record.updateTime || record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap>
              <a-button type="link" size="small" @click="goEdit(idFromData(record.id))">编辑</a-button>
              <a-button
                v-for="btn in getPriorityInfo(record.priority).buttons"
                :key="btn.text"
                type="link"
                size="small"
                :danger="btn.danger"
                @click="setPriority(idFromData(record.id), btn.priority)"
              >
                {{ btn.text }}
              </a-button>
              <a-popconfirm title="确定删除？" @confirm="doDelete(idFromData(record.id))">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.app-manage {
  padding: 0;
}

.app-manage__search {
  margin-bottom: 16px;
}

.app-manage__control {
  width: 180px;
}

.app-manage__control--id {
  width: 140px;
}

.app-manage__control--priority {
  width: 120px;
}

.app-manage__cards {
  display: none;
}

@media (max-width: 768px) {
  .app-manage__search {
    display: flex;
    flex-direction: column;
  }

  .app-manage__search :deep(.ant-form-item) {
    margin-inline-end: 0;
    margin-bottom: 12px;
  }

  .app-manage__search :deep(.ant-form-item-row) {
    flex-direction: column;
    align-items: stretch;
  }

  .app-manage__control,
  .app-manage__control--id,
  .app-manage__control--priority,
  .app-manage__search :deep(.ant-select) {
    width: 100% !important;
  }

  .app-manage__submit {
    width: 100%;
  }

  .app-manage__table {
    display: none;
  }

  .app-manage__cards {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .app-manage__card {
    padding: 14px;
    border: 1px solid rgba(5, 5, 5, 0.06);
    border-radius: 12px;
    background: #fafafa;
  }

  .app-manage__card-top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  .app-manage__card-name {
    margin: 0;
    font-size: 16px;
    line-height: 1.4;
  }

  .app-manage__card-meta {
    margin: 6px 0 0;
    color: rgba(0, 0, 0, 0.45);
    font-size: 13px;
  }

  .app-manage__card-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
  }

  .app-manage__card-actions :deep(.ant-btn) {
    min-height: 36px;
  }
}
</style>
