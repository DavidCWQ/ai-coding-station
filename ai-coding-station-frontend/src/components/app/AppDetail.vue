<script setup lang="ts">
import { computed } from 'vue'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons-vue'

import UserAvatar from '@/components/UserAvatar.vue'

const props = defineProps<{
  open: boolean
  app: API.AppVO | null
  canOperate: boolean
  createTimeText: string
}>()

const emit = defineEmits<{
  'update:open': [v: boolean]
  edit: []
  delete: []
}>()

const modalOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const ownerName = computed(() => props.app?.user?.userName || props.app?.user?.userAccount || '用户')
</script>

<template>
  <a-modal
    v-model:open="modalOpen"
    title="应用详情"
    width="520px"
    :footer="null"
  >
    <div v-if="app" class="app-detail-modal">
      <div class="app-detail-modal__row app-detail-modal__owner-row">
        <span class="app-detail-modal__label">创建者</span>
        <div class="app-detail-modal__owner">
          <UserAvatar
            letter-class="app-detail-modal__owner-avatar-letter"
            :src="app.user?.userAvatar"
            :name="app.user?.userName"
            :account="app.user?.userAccount"
          />
          <span class="app-detail-modal__owner-name">{{ ownerName }}</span>
        </div>
      </div>
      <div class="app-detail-modal__row">
        <span class="app-detail-modal__label">创建时间</span>
        <span>{{ createTimeText }}</span>
      </div>
      <div v-if="canOperate" class="app-detail-modal__actions">
        <a-space>
          <a-button @click="emit('edit')">
            <template #icon><EditOutlined /></template>
            修改
          </a-button>
          <a-button danger @click="emit('delete')">
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.app-detail-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.app-detail-modal__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.app-detail-modal__label {
  color: rgba(0, 0, 0, 0.55);
}

.app-detail-modal__owner {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.app-detail-modal__owner-name {
  text-transform: capitalize;
}

.app-detail-modal__owner-avatar-letter :deep(.ant-avatar-string) {
  text-transform: uppercase;
}

.app-detail-modal__owner-row {
  margin-top: 6px;
}

.app-detail-modal__actions {
  padding-top: 8px;
  border-top: 1px solid rgba(5, 5, 5, 0.06);
}
</style>
