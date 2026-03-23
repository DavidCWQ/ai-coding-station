<script setup lang="ts">
const props = withDefaults(defineProps<{
  text?: string
}>(), {
  text: '返回',
})

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <a-button type="text" class="back-button" @click="emit('click')">
    <span class="back-button__inner">
      <svg
        class="back-button__icon"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M10 19L3 12l7-7M3 12h16"
          stroke="currentColor"
          stroke-width="2.25"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <span>{{ text }}</span>
    </span>
  </a-button>
</template>

<style scoped>
.back-button__inner {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.back-button__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}
</style>
