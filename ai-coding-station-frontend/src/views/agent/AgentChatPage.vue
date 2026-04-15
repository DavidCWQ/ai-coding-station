<script setup lang="ts">
import {
  PaperClipOutlined,
  PlusOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'

import UserAvatar from '@/components/UserAvatar.vue'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import AgentSessionList from '@/components/agent/AgentSessionList.vue'
import DeleteSessionModal from '@/components/agent/DeleteSessionModal.vue'
import RenameSessionModal from '@/components/agent/RenameSessionModal.vue'
import { useAgentChatPageState } from '@/composables/agent/useAgentChatPageState'

const {
  agentMeta,
  loginUser,
  isLoggedIn,
  displayUserName,
  pageLoading,
  inputText,
  messages,
  listEl,
  jumpToLogin,
  sessions,
  sessionsLoading,
  activeSessionId,
  renameModalOpen,
  renameTitle,
  deleteModalOpen,
  runDeleteSession,
  submitRename,
  onSessionAction,
  hasMoreHistory,
  historyLoading,
  loadHistory,
  chatLoading,
  sendUser,
  onAttachmentClick,
  createNewConversation,
  selectSession,
} = useAgentChatPageState()
</script>

<template>
  <div class="agent-chat">
    <a-spin :spinning="pageLoading">
      <div class="agent-layout">
        <aside class="agent-sidebar">
          <div class="agent-sidebar__header">
            <h2 class="agent-sidebar__title">{{ agentMeta?.title ?? '智能体' }}</h2>
            <p v-if="agentMeta" class="agent-sidebar__desc">{{ agentMeta.description }}</p>
            <a-button type="primary" block @click="createNewConversation">
              <template #icon><PlusOutlined /></template>
              新对话
            </a-button>
          </div>

          <AgentSessionList
            :sessions="sessions"
            :sessions-loading="sessionsLoading"
            :active-session-id="activeSessionId"
            :is-logged-in="isLoggedIn"
            @select="selectSession"
            @action="onSessionAction"
          />

          <div class="agent-sidebar__user">
            <template v-if="isLoggedIn">
              <UserAvatar :src="loginUser?.userAvatar" :name="loginUser?.userName" :account="loginUser?.userAccount" />
              <div class="agent-sidebar__user-text">
                <div class="agent-sidebar__user-name">{{ displayUserName }}</div>
                <div class="agent-sidebar__user-sub">已登录</div>
              </div>
            </template>
            <template v-else>
              <a-button type="default" block @click="jumpToLogin">登录后继续</a-button>
            </template>
          </div>
        </aside>

        <section class="agent-main">
          <div ref="listEl" class="agent-main__messages">
            <div class="agent-main__messages-inner">
              <div class="agent-main__more">
                <a-button
                  v-if="hasMoreHistory && isLoggedIn"
                  size="small"
                  :loading="historyLoading"
                  @click="loadHistory('more')"
                >
                  加载更多
                </a-button>
              </div>
              <ChatMessage
                v-for="m in messages"
                :key="m.key"
                :role="m.role"
                :content="m.content"
                :streaming="m.streaming"
              />
              <a-empty
                v-if="messages.length === 0"
                class="agent-main__empty"
                :description="isLoggedIn ? '暂无消息，开始提问吧' : '登录后可开启智能体对话'"
              />
            </div>
          </div>

          <div class="agent-main__composer">
            <div class="agent-main__composer-inner">
              <div
                class="agent-main__input-box"
                :class="{ 'agent-main__input-box--disabled': chatLoading }"
              >
                <a-textarea
                  v-model:value="inputText"
                  :rows="3"
                  :disabled="chatLoading"
                  class="agent-main__textarea"
                  placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                  @press-enter="
                    (e: KeyboardEvent) => {
                      if (!e.shiftKey) {
                        e.preventDefault()
                        sendUser()
                      }
                    }
                  "
                />
                <div class="agent-main__actions">
                  <a-tooltip title="附件">
                    <button
                      class="agent-icon-btn"
                      type="button"
                      :disabled="chatLoading"
                      @click="onAttachmentClick"
                    >
                      <PaperClipOutlined />
                    </button>
                  </a-tooltip>
                  <a-tooltip title="发送">
                    <button
                      class="agent-icon-btn agent-icon-btn--send"
                      type="button"
                      :disabled="chatLoading"
                      @click="sendUser"
                    >
                      <SendOutlined />
                    </button>
                  </a-tooltip>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </a-spin>

    <RenameSessionModal
      v-model:open="renameModalOpen"
      :initial-title="renameTitle"
      @submit="submitRename"
    />
    <DeleteSessionModal v-model:open="deleteModalOpen" :on-confirm="runDeleteSession" />
  </div>
</template>

<style scoped>
.agent-chat {
  width: 100%;
  padding: 0;
}

.agent-layout {
  min-height: calc(100vh - 112px);
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  border: 1px solid rgba(5, 5, 5, 0.06);
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
}

.agent-sidebar {
  border-right: 1px solid rgba(5, 5, 5, 0.06);
  background: #fafafa;
  display: flex;
  flex-direction: column;
}

.agent-sidebar__header {
  padding: 16px;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
}

.agent-sidebar__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.agent-sidebar__desc {
  margin: 8px 0 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.56);
}

.agent-sidebar__user {
  border-top: 1px solid rgba(5, 5, 5, 0.06);
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-sidebar__user-text {
  min-width: 0;
}

.agent-sidebar__user-name {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.agent-sidebar__user-sub {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.agent-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
}

.agent-main__messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 8px;
  background: #fcfcfc;
}

.agent-main__messages-inner {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-main__more {
  min-height: 24px;
  display: flex;
  justify-content: center;
}

.agent-main__empty {
  margin: 56px 0;
}

.agent-main__composer {
  border-top: 1px solid rgba(5, 5, 5, 0.06);
  padding: 12px 20px 16px;
  background: #fff;
}

.agent-main__composer-inner {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

.agent-main__input-box {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(5, 5, 5, 0.12);
  background: #fff;
}

/* 与 Ant Input disabled 底色一致，避免右下角工具条仍为白底 */
.agent-main__input-box--disabled {
  background: #f5f5f5;
  border-color: #d9d9d9;
}

.agent-main__input-box--disabled .agent-main__actions {
  background: #EBEBEB;
}

.agent-main__textarea {
  padding-bottom: 46px;
}

.agent-main__input-box :deep(.ant-input) {
  border: none !important;
  box-shadow: none !important;
  border-radius: 16px !important;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
  resize: none !important;
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar) {
  width: 8px;
  height: 8px;
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-track) {
  background: transparent;
  margin: 0 0 10px;
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.2);
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-thumb:hover) {
  background: rgba(0, 0, 0, 0.3);
}

.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-button),
.agent-main__input-box :deep(.ant-input::-webkit-scrollbar-corner),
.agent-main__input-box :deep(.ant-input::-webkit-resizer) {
  display: none;
  width: 0;
  height: 0;
}

.agent-main__actions {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  background: #fff;
  padding-left: 8px;
}

.agent-icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: rgba(0, 0, 0, 0.45);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.agent-icon-btn:hover {
  background: rgba(0, 0, 0, 0.08);
  color: rgba(0, 0, 0, 0.85);
}

.agent-icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.agent-icon-btn--send {
  color: #1677ff;
}

.agent-icon-btn--send:hover {
  background: rgba(22, 119, 255, 0.14);
  color: #0958d9;
}

@media (max-width: 960px) {
  .agent-layout {
    grid-template-columns: 1fr;
  }

  .agent-sidebar {
    border-right: none;
    border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  }
}
</style>
