<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'

import { adminUpdateApp } from '@/api/appAdminController'
import { getApp, updateApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import { apiLongId } from '@/utils/appId'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const appIdStr = computed(() => String(route.params.id ?? ''))

const pageLoading = ref(true)
const saving = ref(false)
const appVo = ref<API.AppVO | null>(null)

const form = reactive({
  appName: '',
  cover: '',
  priority: 0,
  initPrompt: '',
  deployKey: '',
  createTime: '',
  updateTime: '',
})

const rules: Record<'appName', Rule[]> = {
  appName: [{ required: true, message: '请输入应用名称', trigger: 'blur' }],
}

const isAdmin = computed(() => loginUserStore.loginUser?.userRole === 'admin')

const canEdit = computed(() => {
  const u = loginUserStore.loginUser
  const app = appVo.value
  if (!u?.id || !app?.userId) return false
  if (u.userRole === 'admin') return true
  return String(u.id) === String(app.userId)
})

const getErr = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const m = (err as Record<string, unknown>).message
    if (typeof m === 'string') return m
  }
  return '操作失败'
}

const load = async () => {
  pageLoading.value = true
  try {
    const res = await getApp({ id: apiLongId(appIdStr.value) })
    const vo = res.data?.data ?? null
    appVo.value = vo
    if (vo) {
      form.appName = vo.appName ?? ''
      form.cover = vo.cover ?? ''
      form.priority = vo.priority ?? 0
      form.initPrompt = vo.initPrompt ?? ''
      form.deployKey = vo.deployKey ?? ''
      form.createTime = vo.createTime ?? ''
      form.updateTime = vo.updateTime ?? ''
    }
  } catch (e) {
    message.error(getErr(e))
    appVo.value = null
  } finally {
    pageLoading.value = false
  }
}

const onSubmit = async () => {
  if (!canEdit.value) {
    message.error('无权编辑')
    return
  }
  saving.value = true
  try {
    if (isAdmin.value) {
      await adminUpdateApp({
        id: apiLongId(appIdStr.value),
        appName: form.appName,
        cover: form.cover || undefined,
        priority: form.priority,
      })
    } else {
      await updateApp({
        id: apiLongId(appIdStr.value),
        appName: form.appName,
      })
    }
    message.success('保存成功')
    await router.push(`/app/${appIdStr.value}`)
  } catch (e) {
    message.error(getErr(e))
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="app-edit">
    <a-spin :spinning="pageLoading">
      <a-result
        v-if="appVo && !canEdit"
        status="403"
        title="无权编辑"
        sub-title="仅本人或管理员可编辑该应用"
      >
        <template #extra>
          <a-button type="primary" @click="router.push('/app')">返回应用工坊</a-button>
        </template>
      </a-result>

      <a-card v-else-if="appVo" title="编辑应用" :bordered="false" class="app-edit__card">
        <a-form :model="form" :rules="rules" layout="vertical" @finish="onSubmit">
          <a-form-item label="应用名称" name="appName">
            <a-input v-model:value="form.appName" allow-clear />
          </a-form-item>

          <template v-if="isAdmin">
            <a-form-item label="封面图 URL">
              <a-input v-model:value="form.cover" allow-clear placeholder="https://..." />
            </a-form-item>
          </template>

          <a-form-item label="初始提示词">
            <a-textarea :value="form.initPrompt || '—'" :rows="4" disabled />
          </a-form-item>

          <a-form-item label="部署密钥">
            <a-input :value="form.deployKey || '—'" disabled />
          </a-form-item>

          <a-form-item label="创建时间">
            <a-input :value="form.createTime || '—'" disabled />
          </a-form-item>

          <a-form-item label="更新时间">
            <a-input :value="form.updateTime || '—'" disabled />
          </a-form-item>

          <a-form-item label="优先级（99=精选）">
            <a-input-number
              v-model:value="form.priority"
              :min="0"
              :max="999"
              :disabled="!isAdmin"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="saving">保存</a-button>
              <a-button @click="router.push(`/app/${appIdStr}`)">进入对话</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-empty v-else-if="!pageLoading" description="应用不存在" />
    </a-spin>
  </div>
</template>

<style scoped>
.app-edit {
  max-width: 560px;
  margin: 0 auto;
}

.app-edit__card {
  border-radius: 12px;
}
</style>
