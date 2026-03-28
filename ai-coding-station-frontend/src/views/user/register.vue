<script setup lang="ts">
import { getCurrentInstance, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule, RuleObject } from 'ant-design-vue/es/form/interface'

import { userRegister } from '@/api/userController'

type RegisterFormModel = {
  userAccount: string
  userPassword: string
  checkPassword: string
}

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any
const loading = ref(false)

const form = reactive<RegisterFormModel>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (
  _rule: RuleObject,
  value: unknown,
  callback: (error?: string) => void,
) => {
  if (typeof value !== 'string' || value.length === 0) {
    callback('请确认密码')
    return
  }

  if (value !== form.userPassword) {
    callback('两次密码不一致')
    return
  }

  callback()
}

const rules: Record<keyof RegisterFormModel, Rule[]> = {
  userAccount: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' },
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

    await userRegister({
      userAccount: form.userAccount,
      userPassword: form.userPassword,
      checkPassword: form.checkPassword,
    })

    message.success('注册成功')
    await router.push('/user/login')
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
        <div class="login-card__title">注册</div>
        <div class="login-card__desc">创建你的账号以开始使用</div>
      </div>

      <a-form :model="form" :rules="rules" layout="vertical" @finish="onFinish">
        <a-form-item name="userAccount" label="账号">
          <a-input v-model:value="form.userAccount" autocomplete="username" />
        </a-form-item>

        <a-form-item name="userPassword" label="密码">
          <a-input-password
            v-model:value="form.userPassword"
            autocomplete="new-password"
          />
        </a-form-item>

        <a-form-item name="checkPassword" label="确认密码">
          <a-input-password
            v-model:value="form.checkPassword"
            autocomplete="new-password"
          />
        </a-form-item>

        <a-form-item>
          <a-button block type="primary" html-type="submit" :loading="loading">
            注册
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-card__footer">
        <span>已有账号？</span>
        <router-link class="login-card__link" to="/user/login">去登录</router-link>
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

