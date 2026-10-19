<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'

import { forgotPassword } from '@/api/userController'

const loading = ref(false)
const hint = ref('')

const form = reactive({
  userAccount: '',
})

const rules: Record<'userAccount', Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号至少4位', trigger: 'blur' },
  ],
}

const onFinish = async () => {
  hint.value = ''
  try {
    loading.value = true
    const res = await forgotPassword({
      userAccount: form.userAccount.trim(),
    })
    hint.value = res.data?.data || '如果该账号已绑定邮箱，重置邮件已发送，请查收'
  } catch (err) {
    const msg =
      typeof err === 'object' && err !== null && 'message' in err
        ? String((err as { message?: string }).message || '')
        : ''
    if (!msg) {
      message.error('请求失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <a-card class="login-card" :bordered="false">
      <div class="login-card__header">
        <div class="login-card__title">忘记密码</div>
        <div class="login-card__desc">输入账号。如果已绑定邮箱，我们会发送重置链接。</div>
      </div>

      <a-form :model="form" :rules="rules" layout="vertical" @finish="onFinish">
        <a-form-item name="userAccount" label="账号">
          <a-input v-model:value="form.userAccount" autocomplete="username" />
        </a-form-item>

        <a-alert v-if="hint" class="login-card__hint" type="success" :message="hint" show-icon />

        <a-form-item>
          <a-button block type="primary" html-type="submit" :loading="loading">发送重置邮件</a-button>
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

.login-card__hint {
  margin-bottom: 16px;
}

.login-card__footer {
  margin-top: 16px;
  text-align: center;
}

.login-card__link {
  margin-left: 6px;
}
</style>
