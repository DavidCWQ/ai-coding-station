<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType, TableProps } from 'ant-design-vue'
import dayjs from 'dayjs'

import { deleteUser, listUserVoByPage } from '@/api/adminUserController'

const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
  userAccount: '',
  userName: '',
})

const columns: TableColumnType<API.UserVO>[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 88 },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount', ellipsis: true },
  { title: '用户名', dataIndex: 'userName', key: 'userName', ellipsis: true },
  { title: '头像', key: 'userAvatar', width: 88 },
  { title: '简介', dataIndex: 'userProfile', key: 'userProfile', ellipsis: true },
  { title: '角色', key: 'userRole', width: 100 },
  { title: '注册时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' },
]

const pagination = computed<TableProps['pagination']>(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const formatTime = (v?: string) => {
  if (!v) return '—'
  const d = dayjs(v)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : v
}

const getErrorMessage = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const maybe = err as Record<string, unknown>
    const msg = maybe.message
    if (typeof msg === 'string') return msg
  }
  return '请求失败'
}

const fetchData = async () => {
  try {
    loading.value = true
    const account = searchParams.userAccount.trim()
    const name = searchParams.userName.trim()
    const res = await listUserVoByPage({
      pageNum: searchParams.pageNum,
      pageSize: searchParams.pageSize,
      userAccount: account || undefined,
      userName: name || undefined,
    })
    const page = res.data?.data
    data.value = page?.records ?? []
    total.value = page?.totalRow ?? 0
  } catch (err) {
    message.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  void fetchData()
}

const doTableChange: TableProps['onChange'] = (pag) => {
  searchParams.pageNum = pag.current ?? 1
  searchParams.pageSize = pag.pageSize ?? searchParams.pageSize
  void fetchData()
}

const doDelete = async (id: number | undefined) => {
  if (id === undefined) return
  try {
    await deleteUser({ id })
    message.success('删除成功')
    await fetchData()
  } catch (err) {
    message.error(getErrorMessage(err))
  }
}

onMounted(() => {
  void fetchData()
})
</script>

<template>
  <div class="user-manage">
    <a-card title="用户管理" :bordered="false">
      <a-form :model="searchParams" layout="inline" class="user-manage__search" @finish="doSearch">
        <a-form-item label="账号" name="userAccount">
          <a-input
            v-model:value="searchParams.userAccount"
            allow-clear
            placeholder="账号"
            style="width: 180px"
            @press-enter="doSearch"
          />
        </a-form-item>
        <a-form-item label="用户名" name="userName">
          <a-input
            v-model:value="searchParams.userName"
            allow-clear
            placeholder="用户名"
            style="width: 180px"
            @press-enter="doSearch"
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
        :scroll="{ x: 1100 }"
        :locale="{ emptyText: '暂无数据' }"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userAvatar'">
            <a-image
              v-if="record.userAvatar"
              :src="record.userAvatar"
              :width="40"
              :height="40"
              style="border-radius: 4px; object-fit: cover"
            />
            <span v-else>—</span>
          </template>
          <template v-else-if="column.key === 'userRole'">
            <a-tag v-if="record.userRole === 'admin'" color="green">admin</a-tag>
            <a-tag v-else-if="record.userRole === 'user'" color="blue">user</a-tag>
            <a-tag v-else>{{ record.userRole || '—' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="确定删除该用户？" ok-text="确定" cancel-text="取消" @confirm="doDelete(record.id)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.user-manage {
  padding: 16px;
  max-width: 1400px;
  margin: 0 auto;
}

.user-manage__search {
  margin-bottom: 16px;
}
</style>
