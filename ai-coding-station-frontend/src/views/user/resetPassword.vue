<script setup lang="ts">
import { getCurrentInstance, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule, RuleObject } from 'ant-design-vue/es/form/interface'
import { useRoute } from 'vue-router'

import { resetPassword } from '@/api/userController'

const route = useRoute()
const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any
const loading = ref(false)
const token = String(route.query.token || '')

const form = reactive({
  newPassword: '',
  checkNewPassword: '',
})

const validateCheckPassword = (
  _rule: RuleObject,
  value: unknown,
  callback: (error?: string) => void,
) => {
  if (typeof value !== 'string' || value.length === 0) {
    callback('请再次输入新密码')
    return
  }
  if (value !== form.newPassword) {
    callback('两次新密码不一致')
    return
  }
  callback()
}

const rules: Record<'newPassword' | 'checkNewPassword', Rule[]> = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  checkNewPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' },
  ],
}

const onFinish = async () => {
  if (!token) {
    message.error('重置链接无效或已过期')
    return
  }
  try {
    loading.value = true
    await resetPassword({
      token,
      newPassword: form.newPassword,
      checkNewPassword: form.checkNewPassword,
    })
    message.success('密码已重置，请重新登录')
    await router.push('/user/login')
  } catch {
    // 拦截器已提示接口返回的错误
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <a-card class="login-card" :bordered="false">
      <div class="login-card__header">
        <div class="login-card__title">重置密码</div>
        <div class="login-card__desc">设置一个新的登录密码</div>
      </div>

      <a-alert
        v-if="!token"
        type="error"
        message="重置链接无效或已过期"
        show-icon
      />

      <a-form v-else :model="form" :rules="rules" layout="vertical" @finish="onFinish">
        <a-form-item name="newPassword" label="新密码">
          <a-input-password v-model:value="form.newPassword" autocomplete="new-password" />
        </a-form-item>

        <a-form-item name="checkNewPassword" label="确认新密码">
          <a-input-password v-model:value="form.checkNewPassword" autocomplete="new-password" />
        </a-form-item>

        <a-form-item>
          <a-button block type="primary" html-type="submit" :loading="loading">确认重置</a-button>
        </a-form-item>
      </a-form>

      <div class="login-card__footer">
        <router-link class="login-card__link" to="/user/login">返回登录</router-link>
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
  max-width: 380px;
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
}
</style>
