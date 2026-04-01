import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import { listFeaturedApps, listMyApps } from '@/api/appController'
import { getErrorMessage } from '@/utils/error'

export type AppListKind = 'my' | 'featured'

function emptyQueryBody(extra: Partial<API.AppQueryRequest> = {}): API.AppQueryRequest {
  return {
    id: 0,
    pageNum: 1,
    pageSize: 12,
    ...extra,
  }
}

export function useAppList(kind: AppListKind) {
  const loading = ref(false)
  const records = ref<API.AppVO[]>([])
  const total = ref(0)
  const query = reactive({
    pageNum: 1,
    pageSize: 12,
    appName: '',
  })

  const fetchList = async () => {
    loading.value = true
    try {
      const name = query.appName.trim()
      const body = emptyQueryBody({
        pageNum: query.pageNum,
        pageSize: Math.min(query.pageSize, 15),
        appName: name || undefined,
        sortField: kind === 'my' ? 'update_time' : 'priority',
        sortOrder: 'desc',
      })
      const res =
        kind === 'my' ? await listMyApps(body) : await listFeaturedApps(body)
      const page = res.data?.data
      records.value = page?.records ?? []
      total.value = page?.totalRow ?? 0
    } catch (e) {
      message.error(getErrorMessage(e, '加载失败'))
      records.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const doSearch = () => {
    query.pageNum = 1
    void fetchList()
  }

  const onPageChange = (page: number, pageSize: number) => {
    query.pageNum = page
    query.pageSize = pageSize
    void fetchList()
  }

  return reactive({
    loading,
    records,
    total,
    query,
    fetchList,
    doSearch,
    onPageChange,
  })
}
