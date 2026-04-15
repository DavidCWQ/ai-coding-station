<script setup lang="ts">
import SessionActionMenu from '@/components/agent/SessionActionMenu.vue'
import type { AgentSessionRow } from '@/types/agent'

defineProps<{
  sessions: AgentSessionRow[]
  sessionsLoading: boolean
  activeSessionId: string | null
  isLoggedIn: boolean
}>()

const emit = defineEmits<{
  select: [sessionId: string]
  action: [key: 'rename' | 'delete', item: AgentSessionRow]
}>()

const formatTime = (t?: string) => {
  if (!t) return '刚刚'
  const d = new Date(t)
  if (Number.isNaN(d.valueOf())) return '刚刚'
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

const sessionLabel = (item: AgentSessionRow) => item.title?.trim() || '新对话'

const emptyHint = (loggedIn: boolean) =>
  loggedIn ? '暂无历史对话' : '登录后可查看历史对话'
</script>

<template>
  <div class="agent-sidebar__sessions">
    <div class="agent-sidebar__label">历史对话</div>
    <a-spin :spinning="sessionsLoading">
      <div v-if="sessions.length === 0" class="agent-sidebar__empty">
        {{ emptyHint(isLoggedIn) }}
      </div>
      <div
        v-for="item in sessions"
        :key="item.id"
        class="agent-session-item"
        :class="{ 'agent-session-item--active': activeSessionId === item.id }"
        @click="emit('select', item.id)"
      >
        <span class="agent-session-item__title">{{ sessionLabel(item) }}</span>
        <div class="agent-session-item__right">
          <span class="agent-session-item__time">{{ formatTime(item.lastMsgTime) }}</span>
          <SessionActionMenu @action="(key) => emit('action', key, item)" />
        </div>
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.agent-sidebar__sessions {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.agent-sidebar__label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}

.agent-sidebar__empty {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  padding: 8px 4px;
}

.agent-session-item {
  width: 100%;
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 10px;
  background: #fff;
  text-align: left;
  padding: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  transition:
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.agent-session-item:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.agent-session-item--active {
  border-color: #91caff;
  background: #e6f4ff;
}

.agent-session-item__title {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-session-item__time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  transition: transform 0.2s ease;
}

.agent-session-item__right {
  position: relative;
  min-width: 54px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

:deep(.agent-session-item__more) {
  position: absolute;
  right: 0;
  top: 50%;
  color: rgba(0, 0, 0, 0.45);
  opacity: 0;
  pointer-events: none;
  transform: translate(4px, -50%);
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.agent-session-item:hover .agent-session-item__time,
.agent-session-item:focus-within .agent-session-item__time {
  transform: translateX(-28px);
}

.agent-session-item:hover :deep(.agent-session-item__more),
.agent-session-item:focus-within :deep(.agent-session-item__more) {
  opacity: 1;
  pointer-events: auto;
  transform: translate(0, -50%);
}
</style>
