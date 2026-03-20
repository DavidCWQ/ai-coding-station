<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'

import { userLogin } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

type LoginFormModel = {
  userAccount: string
  userPassword: string
}

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(false)

const form = reactive<LoginFormModel>({
  userAccount: '',
  userPassword: '',
})

const rules: Record<keyof LoginFormModel, Rule[]> = {
  userAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
}

const getErrorMessage = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const maybe = err as Record<string, unknown>
    const msg = maybe.message
    if (typeof msg === 'string') return msg
  }
  return '请求失败'
}

const onFinish = async () => {
  try {
    loading.value = true

    const res = await userLogin({
      userAccount: form.userAccount,
      userPassword: form.userPassword,
    })

    if (!res?.data) {
      message.error('登录失败')
      return
    }

    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    await router.push('/')
  } catch (err) {
    message.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <a-card class="login-card" :bordered="false">
      <div class="login-card__header">
        <div class="login-card__title">登录</div>
        <div class="login-card__desc">欢迎使用 AI Coding Station</div>
      </div>

      <a-form :model="form" :rules="rules" layout="vertical" @finish="onFinish">
        <a-form-item name="userAccount" label="账号">
          <a-input v-model:value="form.userAccount" autocomplete="username" />
        </a-form-item>

        <a-form-item name="userPassword" label="密码">
          <a-input-password
            v-model:value="form.userPassword"
            autocomplete="current-password"
          />
        </a-form-item>

        <a-form-item>
          <a-button block type="primary" html-type="submit" :loading="loading">
            登录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-card__footer">
        <span>没有账号？</span>
        <router-link class="login-card__link" to="/user/register">去注册</router-link>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.login-card__header {
  margin-bottom: 16px;
}

.login-card__title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
}

.login-card__desc {
  color: rgba(0, 0, 0, 0.65);
}

.login-card__footer {
  margin-top: 16px;
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
}

.login-card__link {
  margin-left: 6px;
}
</style>

