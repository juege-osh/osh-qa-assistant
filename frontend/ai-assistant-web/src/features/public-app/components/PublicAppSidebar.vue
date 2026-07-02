<template>
  <aside class="history-panel">
    <button type="button" class="new-session-btn" :disabled="interactionLocked" @click="startNewConversation">
      <el-icon><Plus /></el-icon>
      开启新对话
    </button>

    <div class="history-section">
      <div class="history-title">
        <el-icon><Clock /></el-icon>
        最近对话
      </div>

      <div v-if="sessions.length" class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          role="button"
          tabindex="0"
          :aria-disabled="interactionLocked ? 'true' : 'false'"
          :class="[
            'session-item',
            session.id === activeSessionId ? 'session-item--active' : '',
            interactionLocked ? 'session-item--locked' : ''
          ]"
          @click="selectSession(session.id)"
          @keydown.enter.prevent="selectSession(session.id)"
          @keydown.space.prevent="selectSession(session.id)"
        >
          <div class="session-main">
            <div class="session-name">{{ session.title }}</div>
            <div class="session-actions">
              <button
                type="button"
                class="session-action-btn"
                title="重命名"
                :disabled="interactionLocked"
                aria-label="重命名会话"
                @click.stop="openRenameDialog(session)"
              >
                <el-icon><Edit /></el-icon>
              </button>
              <button
                type="button"
                class="session-action-btn session-action-btn--danger"
                title="删除"
                :disabled="interactionLocked"
                aria-label="删除会话"
                @click.stop="deleteSession(session.id)"
              >
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
          <div class="session-meta">
            <span>{{ formatSessionTime(session.updatedAt) }}</span>
            <span>{{ session.messages.length }} 条消息</span>
          </div>
        </div>
      </div>

      <div v-else class="history-empty">
        还没有历史对话，先开启一轮新的公开问答。
      </div>
    </div>

    <div class="history-note">
      <div class="history-note-title">历史记录</div>
      <div class="history-note-text">
        当前公开页历史保存在本地浏览器，仅当前设备和当前身份可见。
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { Clock, Delete, Edit, Plus } from '@element-plus/icons-vue'
import { usePublicAppFeatureModel } from '../composables/usePublicAppFeature'

const model = usePublicAppFeatureModel()
const {
  sessions,
  activeSessionId,
  interactionLocked,
  startNewConversation,
  selectSession,
  openRenameDialog,
  deleteSession,
  formatSessionTime
} = model
</script>

<style scoped>
.history-panel {
  padding: 18px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  gap: 22px;
  overflow: hidden;
  border: 1px solid #e7edf4;
  border-radius: 30px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.04);
}

.new-session-btn,
.session-item {
  border: 1px solid #e4e9f0;
  background: #ffffff;
  color: #111827;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.new-session-btn {
  width: 100%;
  min-height: 64px;
  padding: 16px 18px;
  border-radius: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-size: 17px;
  font-weight: 600;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.04);
}

.new-session-btn:hover,
.session-item:hover {
  transform: translateY(-1px);
  border-color: #d7dfea;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
}

.new-session-btn:disabled,
.session-action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

.history-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  flex: 1;
}

.history-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #344054;
  font-size: 16px;
  font-weight: 600;
}

.session-list {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  gap: 10px;
  overflow-y: auto;
  padding-right: 4px;
}

.session-item {
  width: 100%;
  text-align: left;
  padding: 16px 16px 14px;
  border-radius: 20px;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff 0%, #fcfdff 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.02);
}

.session-item:focus-visible {
  outline: 2px solid #2c8cff;
  outline-offset: 2px;
}

.session-item--locked {
  cursor: not-allowed;
}

.session-item--locked:hover {
  transform: none;
  border-color: #e5e7eb;
  box-shadow: none;
}

.session-item--active {
  border-color: #d5dde9;
  background: linear-gradient(180deg, #ffffff 0%, #f4f7fb 100%);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.session-item--active.session-item--locked:hover {
  border-color: #d9e1ee;
}

.session-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.session-name {
  min-width: 0;
  flex: 1;
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  word-break: break-word;
}

.session-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.session-action-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #e5e9f0;
  border-radius: 10px;
  background: transparent;
  color: #6b7280;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.session-action-btn:hover {
  border-color: #d8e0eb;
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
}

.session-action-btn:disabled:hover {
  border-color: transparent;
  background: transparent;
  color: #6b7280;
}

.session-action-btn--danger:hover {
  border-color: #fecaca;
  background: #fff1f2;
  color: #b91c1c;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: #6b7280;
  font-size: 12px;
}

.history-empty,
.history-note-text {
  color: #6b7280;
  line-height: 1.65;
}

.history-empty {
  padding: 16px;
  border-radius: 18px;
  border: 1px dashed #dde5f0;
  background: #fafbfd;
  font-size: 13px;
}

.history-note {
  margin-top: auto;
  padding: 18px;
  border: 1px solid #e7edf4;
  border-radius: 22px;
  background: linear-gradient(180deg, #fbfcff 0%, #f6f8fc 100%);
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.history-note-title {
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
}

.history-note-text {
  margin-top: 8px;
  font-size: 13px;
}

@media (max-width: 900px) {
  .history-panel {
    gap: 18px;
  }
}

@media (max-width: 640px) {
  .session-item {
    padding: 14px;
  }

  .session-main {
    align-items: center;
  }
}
</style>
