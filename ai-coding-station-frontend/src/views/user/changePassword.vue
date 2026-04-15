<script setup lang="ts">
import { getCurrentInstance, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule, RuleObject } from 'ant-design-vue/es/form/interface'

import { changePassword } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

type ChangePasswordFormModel = {
  oldPassword: string
  newPassword: string
  checkNewPassword: string
}

const vm = getCurrentInstance()
const router = vm?.proxy?.$router as any
const loginUserStore = useLoginUserStore()
const loading = ref(false)

const form = reactive<ChangePasswordFormModel>({
  oldPassword: '',
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

const rules: Record<keyof ChangePasswordFormModel, Rule[]> = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  checkNewPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' },
  ],
}

const getErrorMessage = (err: unknown): string => {
  if (typeof err === 'object' && err !== null) {
    const maybe = err as Record<string, unknown>
    const msg = maybe.message
    if (typeof msg === 'string') return msg
  }
  return '修改失败'
}

const onFinish = async () => {
  try {
    loading.value = true
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      checkNewPassword: form.checkNewPassword,
    })
    message.success('密码修改成功，请重新登录')
    await loginUserStore.logout()
    await router.push('/user/login')
  } catch (err) {
    message.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="change-password-page">
    <a-card class="change-password-card" :bordered="false">
      <div class="change-password-card__header">
        <div class="change-password-card__title">修改密码</div>
        <div class="change-password-card__desc">请先验证原密码，再设置新密码</div>
      </div>

      <a-form :model="form" :rules="rules" layout="vertical" @finish="onFinish">
        <a-form-item name="oldPassword" label="原密码">
          <a-input-password v-model:value="form.oldPassword" autocomplete="current-password" />
        </a-form-item>

        <a-form-item name="newPassword" label="新密码">
          <a-input-password v-model:value="form.newPassword" autocomplete="new-password" />
        </a-form-item>

        <a-form-item name="checkNewPassword" label="确认新密码">
          <a-input-password v-model:value="form.checkNewPassword" autocomplete="new-password" />
        </a-form-item>

        <a-form-item>
          <a-button block type="primary" html-type="submit" :loading="loading">确认修改</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.change-password-page {
  min-height: calc(100vh - 64px - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}

.change-password-card {
  width: 100%;
  max-width: 380px;
}

.change-password-card__header {
  margin-bottom: 16px;
}

.change-password-card__title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 4px;
}

.change-password-card__desc {
  color: rgba(0, 0, 0, 0.65);
}
</style>
