<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType, TableProps } from 'ant-design-vue'
import dayjs from 'dayjs'

import { adminDeleteApp, adminUpdateApp, listApp } from '@/api/appAdminController'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'

const FEATURED_PRIORITY = 99
const PINNED_PRIORITY = 999

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any

const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  userId: '' as string,
})

const columns: TableColumnType<API.AppVO>[] = [
  { title: 'ID', key: 'id', width: 100, ellipsis: true },
  { title: '应用名', dataIndex: 'appName', key: 'appName', ellipsis: true },
  { title: '用户ID', key: 'userId', width: 120 },
  { title: '优先级', key: 'priority', width: 96 },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' },
]

const getPriorityInfo = (priority: number) => {
  if (priority >= PINNED_PRIORITY) {
    return {
      tag: { color: 'gold', text: '置顶' },
      buttons: [
        { text: '精选', priority: FEATURED_PRIORITY, danger: false },
        { text: '取消置顶', priority: FEATURED_PRIORITY, danger: false }
      ]
    }
  }
  if (priority >= FEATURED_PRIORITY) {
    return {
      tag: { color: 'green', text: '精选' },
      buttons: [
        { text: '取消精选', priority: 0, danger: false },
        { text: '置顶', priority: PINNED_PRIORITY, danger: false }
      ]
    }
  }
  if (priority >= 0) {
    return {
      tag: { color: 'gray', text: '普通' },
      buttons: [
        { text: '精选', priority: FEATURED_PRIORITY, danger: false },
        { text: '置顶', priority: PINNED_PRIORITY, danger: false }
      ]
    }
  }
  return {
    tag: { color: 'red', text: '锁定' },
    buttons: [{ text: '取消锁定', priority: 0, danger: false }]
  }
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

const buildQuery = (): API.AppQueryRequest => {
  const uid = searchParams.userId.trim()
  return {
    id: 0,
    pageNum: searchParams.pageNum,
    pageSize: Math.min(searchParams.pageSize, 20),
    appName: searchParams.appName.trim() || undefined,
    userId: uid ? apiLongId(uid) : undefined,
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listApp(buildQuery())
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
    await adminDeleteApp({ id: apiLongId(id) })
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
          <a-input v-model:value="searchParams.appName" allow-clear style="width: 180px" />
        </a-form-item>
        <a-form-item label="用户ID">
          <a-input v-model:value="searchParams.userId" allow-clear style="width: 140px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>

      <a-table
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
          <template v-else-if="column.key === 'createTime'">
            {{ fmt(record.createTime) }}
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
</style>
