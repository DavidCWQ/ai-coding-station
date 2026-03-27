<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType, TableProps } from 'ant-design-vue'
import dayjs from 'dayjs'

import { deleteSession, listHistory, listSessions, updateSessionTitle } from '@/api/chatController'
import { apiLongId, idFromData } from '@/utils/id'
import { getErrorMessage } from '@/utils/error'

type SessionRow = API.ChatSessionVO & { _idText: string }

const data = ref<SessionRow[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
  appId: '',
})

const columns: TableColumnType<SessionRow>[] = [
  { title: '会话ID', key: 'id', width: 160, ellipsis: true },
  { title: '应用ID', key: 'appId', width: 120, ellipsis: true },
  { title: '标题', key: 'title', ellipsis: true },
  { title: '最后消息时间', key: 'lastMsgTime', width: 180 },
  { title: '创建时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' },
]

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

const getAppIdNumber = () => {
  const v = searchParams.appId.trim()
  if (!v) return null
  return apiLongId(v)
}

const fetchData = async () => {
  const appIdNum = getAppIdNumber()
  if (appIdNum == null) {
    data.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await listSessions({
      appId: appIdNum,
      pageNum: searchParams.pageNum,
      pageSize: Math.min(searchParams.pageSize, 20),
    })
    const page = res.data?.data
    const records = (page?.records ?? []) as API.ChatSessionVO[]
    data.value = records.map((r) => ({
      ...r,
      _idText: r.id != null ? String(r.id) : '—',
    }))
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

// 详情抽屉：按游标分页把历史逐页加载出来（用于查看，不改变主列表）
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailHasMore = ref(false)
const detailCursor = ref<{ beforeMessageId?: string; beforeCreateTime?: string }>({})
const detailMessages = ref<API.ChatHistoryVO[]>([])
const current = ref<{ appId: string; sessionId: string; title?: string } | null>(null)
const titleEditing = ref(false)
const titleValue = ref('')

const openDetail = async (row: SessionRow) => {
  if (row.id == null || row.appId == null) return
  current.value = {
    appId: String(row.appId),
    sessionId: String(row.id),
    title: row.title,
  }
  titleValue.value = row.title ?? ''
  detailOpen.value = true
  detailMessages.value = []
  detailCursor.value = {}
  detailHasMore.value = true
  await loadMoreDetail('initial')
}

const loadMoreDetail = async (mode: 'initial' | 'more') => {
  if (!current.value) return
  if (detailLoading.value) return
  if (mode === 'more' && !detailHasMore.value) return
  detailLoading.value = true
  try {
    const res = await listHistory({
      appId: apiLongId(current.value.appId),
      sessionId: apiLongId(current.value.sessionId),
      pageSize: 10,
      beforeMessageId: mode === 'more' && detailCursor.value.beforeMessageId
        ? apiLongId(detailCursor.value.beforeMessageId)
        : undefined,
      beforeCreateTime: mode === 'more' ? detailCursor.value.beforeCreateTime : undefined,
    })
    const list = res.data?.data ?? []
    if (mode === 'initial') {
      detailMessages.value = []
      detailCursor.value = {}
    }
    // 服务端时间正序；为“加载更多旧消息”，这里 prepend
    detailMessages.value = [...list, ...detailMessages.value]
    const first = detailMessages.value[0]
    if (first?.id != null) {
      detailCursor.value = { beforeMessageId: String(first.id), beforeCreateTime: first.createTime }
    }
    detailHasMore.value = Array.isArray(list) && list.length >= 10
  } catch (e) {
    message.error(getErrorMessage(e, '加载会话详情失败'))
  } finally {
    detailLoading.value = false
  }
}

const onDelete = async (row: SessionRow) => {
  if (row.id == null) return
  try {
    await deleteSession({ id: apiLongId(String(row.id)) })
    message.success('已删除')
    await fetchData()
    if (current.value?.sessionId === String(row.id)) {
      detailOpen.value = false
    }
  } catch (e) {
    message.error(getErrorMessage(e, '请求失败'))
  }
}

const saveTitle = async () => {
  if (!current.value) return
  const t = titleValue.value.trim()
  if (!t) {
    message.warning('标题不能为空')
    return
  }
  titleEditing.value = true
  try {
    await updateSessionTitle({ sessionId: apiLongId(current.value.sessionId), title: t })
    message.success('已更新')
    detailOpen.value = false
    await fetchData()
  } catch (e) {
    message.error(getErrorMessage(e, '更新失败'))
  } finally {
    titleEditing.value = false
  }
}

onMounted(() => {
  // 默认不拉数据，避免 appId 为空时反复报错；用户输入 appId 后再查询
})
</script>

<template>
  <div class="chat-manage">
    <a-card title="对话管理" :bordered="false">
      <a-form :model="searchParams" layout="inline" class="chat-manage__search" @finish="doSearch">
        <a-form-item label="应用ID" required>
          <a-input
            v-model:value="searchParams.appId"
            allow-clear
            style="width: 180px"
            placeholder="请输入应用ID"
          />
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
        :scroll="{ x: 980 }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            {{ record._idText }}
          </template>
          <template v-else-if="column.key === 'appId'">
            {{ record.appId != null ? idFromData(record.appId) : '—' }}
          </template>
          <template v-else-if="column.key === 'title'">
            {{ record.title || '—' }}
          </template>
          <template v-else-if="column.key === 'lastMsgTime'">
            {{ fmt(record.lastMsgTime) }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ fmt(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap>
              <a-button type="link" size="small" @click="openDetail(record)">查看详情</a-button>
              <a-popconfirm title="确定删除该会话？" @confirm="onDelete(record)">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="会话详情" width="720" :destroy-on-close="true">
      <template v-if="current">
        <div class="chat-manage__detail-meta">
          <div class="chat-manage__detail-line">
            <span class="chat-manage__label">应用ID</span>
            <span class="chat-manage__value">{{ idFromData(current.appId) }}</span>
          </div>
          <div class="chat-manage__detail-line">
            <span class="chat-manage__label">会话ID</span>
            <span class="chat-manage__value">{{ String(current.sessionId) }}</span>
          </div>
          <div class="chat-manage__detail-line">
            <span class="chat-manage__label">标题</span>
            <a-input v-model:value="titleValue" style="max-width: 360px" placeholder="请输入会话标题" />
            <a-button type="primary" size="small" :loading="titleEditing" @click="saveTitle">保存</a-button>
          </div>
        </div>

        <div class="chat-manage__detail-toolbar">
          <a-button v-if="detailHasMore" size="small" :loading="detailLoading" @click="loadMoreDetail('more')">
            加载更多
          </a-button>
        </div>

        <a-list :data-source="detailMessages" :loading="detailLoading" item-layout="vertical">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta
                :title="`${item.messageType || '—'} · ${fmt(item.createTime)}`"
                :description="`messageId: ${item.id ?? '—'}`"
              />
              <pre class="chat-manage__msg">{{ item.message || '' }}</pre>
            </a-list-item>
          </template>
        </a-list>

        <a-empty v-if="!detailLoading && detailMessages.length === 0" description="暂无会话消息" />
      </template>
      <a-empty v-else description="请选择一条会话查看" />
    </a-drawer>
  </div>
</template>

<style scoped>
.chat-manage {
  padding: 0;
}

.chat-manage__search {
  margin-bottom: 16px;
}

.chat-manage__detail-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.chat-manage__detail-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.chat-manage__label {
  width: 68px;
  color: rgba(0, 0, 0, 0.45);
}

.chat-manage__value {
  color: rgba(0, 0, 0, 0.88);
}

.chat-manage__detail-toolbar {
  display: flex;
  justify-content: flex-start;
  margin: 8px 0 12px;
}

.chat-manage__msg {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(5, 5, 5, 0.08);
  border-radius: 8px;
  padding: 10px 12px;
}
</style>

