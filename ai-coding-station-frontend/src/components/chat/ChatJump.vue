<script setup lang="ts">
defineProps<{
  show: boolean
}>()

defineEmits<{
  jump: []
}>()
</script>

<template>
  <button v-if="show" type="button" class="chat-jump" aria-label="回到底部" @click="$emit('jump')">
    <svg viewBox="0 0 16 16" aria-hidden="true">
      <path
        d="M3.2 6.2 8 11l4.8-4.8"
        fill="none"
        stroke="currentColor"
        stroke-width="1.6"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
  </button>
</template>

<style scoped>
.chat-jump {
  position: absolute;
  left: 50%;
  bottom: 14px;
  z-index: 2;
  transform: translateX(-50%);
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50%;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  cursor: pointer;
}

.chat-jump:hover {
  color: rgba(0, 0, 0, 0.88);
}

.chat-jump svg {
  width: 16px;
  height: 16px;
}
</style>
