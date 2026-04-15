<script setup lang="ts">
import { DeleteOutlined, EditOutlined, EllipsisOutlined } from '@ant-design/icons-vue'

const emit = defineEmits<{
  action: [key: 'rename' | 'delete']
}>()

const onMenuClick = (info: { key: string | number }) => {
  const key = String(info.key)
  if (key === 'rename' || key === 'delete') {
    emit('action', key)
  }
}
</script>

<template>
  <a-dropdown :trigger="['click']">
    <a-button type="text" size="small" class="agent-session-item__more" @click.stop>
      <template #icon><EllipsisOutlined /></template>
    </a-button>
    <template #overlay>
      <a-menu @click="onMenuClick">
        <a-menu-item key="rename">
          <template #icon><EditOutlined /></template>
          重命名
        </a-menu-item>
        <a-menu-item key="delete" danger>
          <template #icon><DeleteOutlined /></template>
          删除
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

