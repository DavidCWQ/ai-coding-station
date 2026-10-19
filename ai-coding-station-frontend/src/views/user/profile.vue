<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import { storeToRefs } from 'pinia'

import { bindEmail, updateMyProfile } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

type ProfileFormModel = {
  userName: string
  userAvatar: string
  userProfile: string
  userAccount: string
  userRole: string
  createTime: string
}

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any
const loginUserStore = useLoginUserStore()
const { loginUser } = storeToRefs(loginUserStore)

const loading = ref(false)
const emailLoading = ref(false)
const emailForm = reactive({
  userEmail: '',
  password: '',
})

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
  emailForm.userEmail = u.userEmail ?? ''
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

const onBindEmail = async () => {
  if (!emailForm.password) {
    message.error('请输入当前密码')
    return
  }
  try {
    emailLoading.value = true
    await bindEmail({
      userEmail: emailForm.userEmail.trim(),
      password: emailForm.password,
    })
    const bound = emailForm.userEmail.trim().length > 0
    emailForm.password = ''
    await loginUserStore.fetchLoginUser({ force: true })
    message.success(bound ? '邮箱已保存' : '已解除邮箱绑定')
  } catch {
    // 拦截器已提示接口返回的错误
  } finally {
    emailLoading.value = false
  }
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
    await updateMyProfile({
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

      <a-divider />

      <div class="profile-card__header">
        <div class="profile-card__title">找回密码邮箱</div>
        <div class="profile-card__desc">绑定后才能通过邮件重置密码。留空并确认可解除绑定。</div>
      </div>

      <a-form :model="emailForm" layout="vertical" @finish="onBindEmail">
        <a-form-item name="userEmail" label="邮箱">
          <a-input
            v-model:value="emailForm.userEmail"
            allow-clear
            placeholder="name@example.com"
            autocomplete="email"
          />
        </a-form-item>
        <a-form-item
          name="password"
          label="当前密码"
          :rules="[{ required: true, message: '请输入当前密码' }]"
        >
          <a-input-password v-model:value="emailForm.password" autocomplete="current-password" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="button" :loading="emailLoading" @click="onBindEmail">
            保存邮箱
          </a-button>
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
