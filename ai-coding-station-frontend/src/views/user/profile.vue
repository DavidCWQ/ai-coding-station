<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import { storeToRefs } from 'pinia'

import { updateUser } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

type ProfileFormModel = {
  userName: string
  userAvatar: string
  userProfile: string
  userAccount: string
  userRole: string
  createTime: string
}

const loginUserStore = useLoginUserStore()
const { loginUser } = storeToRefs(loginUserStore)
const router = useRouter()

const loading = ref(false)

const form = reactive<ProfileFormModel>({
  userName: '',
  userAvatar: '',
  userProfile: '',
  userAccount: '',
  userRole: '',
  createTime: '',
})

const rules: Record<'userName', Rule[]> = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

const syncFormFromStore = () => {
  const u = loginUser.value
  if (!u) return
  form.userName = u.userName ?? ''
  form.userAvatar = u.userAvatar ?? ''
  form.userProfile = u.userProfile ?? ''
  form.userAccount = u.userAccount ?? ''
  form.userRole = u.userRole ?? ''
  form.createTime = u.createTime ?? ''
}

onMounted(() => {
  if (!loginUser.value?.id) return
  syncFormFromStore()
})

watch(
  loginUser,
  () => {
    if (loginUser.value?.id) syncFormFromStore()
  },
  { deep: true },
)

const getErrorMessage = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const maybe = err as Record<string, unknown>
    const msg = maybe.message
    if (typeof msg === 'string') return msg
  }
  return '保存失败'
}

const onSave = async () => {
  const id = loginUser.value?.id
  if (!id) {
    message.error('未登录')
    await router.push('/user/login')
    return
  }

  try {
    loading.value = true
    await updateUser({
      id,
      userName: form.userName,
      userAvatar: form.userAvatar,
      userProfile: form.userProfile,
    })
    await loginUserStore.fetchLoginUser({ force: true })
    message.success('保存成功')
  } catch (err) {
    message.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <a-card class="profile-card" :bordered="false">
      <div class="profile-card__header">
        <div class="profile-card__title">个人主页</div>
        <div class="profile-card__desc">编辑你的公开资料</div>
      </div>

      <a-form :model="form" :rules="rules" layout="vertical" @finish="onSave">
        <a-form-item name="userName" label="用户名">
          <a-input v-model:value="form.userName" allow-clear />
        </a-form-item>

        <a-form-item name="userAvatar" label="头像 URL">
          <a-input v-model:value="form.userAvatar" allow-clear placeholder="https://..." />
        </a-form-item>

        <a-form-item name="userProfile" label="个人简介">
          <a-textarea v-model:value="form.userProfile" :rows="4" allow-clear />
        </a-form-item>

        <a-form-item label="账号">
          <a-input :value="form.userAccount" disabled />
        </a-form-item>

        <a-form-item label="角色">
          <a-input :value="form.userRole" disabled />
        </a-form-item>

        <a-form-item label="注册时间">
          <a-input :value="form.createTime" disabled />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading">保存</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - 64px - 48px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px 16px;
}

.profile-card {
  width: 100%;
  max-width: 600px;
}

.profile-card__header {
  margin-bottom: 16px;
}

.profile-card__title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
}

.profile-card__desc {
  color: rgba(0, 0, 0, 0.65);
}
</style>
