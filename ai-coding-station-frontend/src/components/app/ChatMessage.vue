<script setup lang="ts">
defineProps<{
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}>()
</script>

<template>
  <div class="msg" :class="role === 'user' ? 'msg--user' : 'msg--ai'">
    <div class="msg__bubble">
      <pre class="msg__text">{{ content }}</pre>
      <span v-if="streaming" class="msg__cursor" aria-hidden="true">▍</span>
    </div>
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  margin-bottom: 12px;
}

.msg--user {
  justify-content: flex-end;
}

.msg--ai {
  justify-content: flex-start;
}

.msg__bubble {
  max-width: min(92%, 640px);
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.55;
  font-size: 14px;
  position: relative;
}

.msg--user .msg__bubble {
  background: #1677ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg--ai .msg__bubble {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.88);
  border-bottom-left-radius: 4px;
}

.msg__text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.msg__cursor {
  display: inline-block;
  animation: blink 1s step-end infinite;
  margin-left: 2px;
  color: rgba(0, 0, 0, 0.45);
}

.msg--user .msg__cursor {
  color: rgba(255, 255, 255, 0.8);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>
