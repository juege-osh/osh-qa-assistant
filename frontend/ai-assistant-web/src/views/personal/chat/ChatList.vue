<template>
  <div class="page-shell chat-list-page">
    <section class="context-strip">
      <el-button text class="context-back workspace-btn workspace-btn--text" @click="router.push('/workspace/app/manage')">返回应用</el-button>
      <span class="context-note">聊天会话属于应用调试入口，可从应用列表继续切换场景。</span>
    </section>
    <section class="session-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">会话管理</div>
          <div class="panel-desc">共 {{ pageData.chats.length }} 个会话，最近更新：{{ latestUpdateTime }}</div>
        </div>
        <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">
          <el-icon><Plus /></el-icon>
          新开聊天
        </el-button>
      </div>

      <div class="session-search">
        <el-input
          v-model="sessionKeyword"
          placeholder="搜索会话名称"
          size="large"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div class="session-grid">
        <div
          v-for="s in filteredChats"
          :key="s.id"
          class="session-item"
          @click="openChat(s.id)"
        >
          <div class="session-body">
            <div class="session-name">{{ s.chatName }}</div>
            <div class="session-meta">
              <span>会话 ID：{{ s.id }}</span>
              <span v-if="formatTime(s.createTime)">创建：{{ formatTime(s.createTime) }}</span>
            </div>
          </div>
          <div class="session-actions">
            <el-button text class="workspace-icon-btn" @click.stop="openRenameDialog(s)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button text type="danger" class="workspace-icon-btn workspace-icon-btn--danger" @click.stop="delChat(s.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        
        <div v-if="!pageData.chats.length" class="empty-state">
          <el-empty description="还没有聊天会话">
            <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="addChat">创建第一个会话</el-button>
          </el-empty>
        </div>
      </div>
    </section>

    <!-- 重命名对话框 -->
    <el-dialog title="重命名会话" v-model="renameDialogVisible" width="500px">
      <el-form>
        <el-form-item label="会话名称">
          <el-input v-model="renameChatName" placeholder="请输入会话名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ChatList" lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue';
import { addChatApi, deleteChatByIdApi, listRecentChatApi, renameChatApi } from '@/api/workspace/chatApi';
import { pageAppApi } from '@/api/workspace/appApi';
import { getItem, saveItem } from '@/util/storageUtil';
import { STORAGE_LAST_APP_ID_KEY } from '@/config/constants';
import type { AnyObjDefine, AnyObjsDefine } from '@/types/common';

const route = useRoute();
const router = useRouter();
const sessionKeyword = ref('');
const renameDialogVisible = ref(false);
const renameChatName = ref('');
const renameChatId = ref('');

const pageData = reactive({
  appId: '',
  chats: [] as AnyObjsDefine
});

const filteredChats = computed(() => {
  if (!sessionKeyword.value) return pageData.chats;
  return pageData.chats.filter((chat: AnyObjDefine) =>
    String(chat.chatName || '').toLowerCase().includes(sessionKeyword.value.toLowerCase())
  );
});

const latestUpdateTime = computed(() => {
  if (!pageData.chats.length) return '暂无';
  const latest = pageData.chats[0];
  return formatTime(latest.createTime);
});

function formatTime(timestamp: string | number | Date) {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function ensureAppSelected(showMessage = true) {
  if (String(pageData.appId || '').trim()) {
    return true;
  }
  if (showMessage) {
    ElMessage.warning('正在自动加载应用，请稍候...');
    autoSelectApp();
  }
  return false;
}

function loadChats() {
  if (!ensureAppSelected()) {
    return;
  }
  listRecentChatApi(pageData.appId).then(result => {
    pageData.chats = result.data || [];
  });
}

function addChat() {
  if (!ensureAppSelected()) {
    return;
  }
  const newName = `新会话${Date.now()}`;
  addChatApi({
    appId: pageData.appId,
    chatName: newName
  }).then(result => {
    ElMessage.success('会话创建成功');
    loadChats();
    // 直接跳转到新创建的会话
    router.push(`/workspace/chat/${result.data.id}`);
  });
}

function openChat(chatId: string) {
  router.push(`/workspace/chat/${chatId}`);
}

function openRenameDialog(chat: AnyObjDefine) {
  renameChatId.value = chat.id;
  renameChatName.value = chat.chatName;
  renameDialogVisible.value = true;
}

function confirmRename() {
  if (!renameChatName.value.trim()) {
    ElMessage.warning('会话名称不能为空');
    return;
  }
  renameChatApi({
    id: renameChatId.value,
    chatName: renameChatName.value
  }).then(() => {
    ElMessage.success('重命名成功');
    renameDialogVisible.value = false;
    loadChats();
  });
}

function delChat(chatId: string) {
  ElMessageBox.confirm('确定删除此会话吗？删除后无法恢复。', '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    deleteChatByIdApi(chatId).then(() => {
      ElMessage.success('删除成功');
      loadChats();
    });
  }).catch(() => {});
}

function handleQueryStr() {
  const appIdFromQs = route.query.appId;
  const normalizedAppId = Array.isArray(appIdFromQs) ? appIdFromQs[0] : appIdFromQs;
  if (normalizedAppId) {
    pageData.appId = String(normalizedAppId).trim();
    saveItem(STORAGE_LAST_APP_ID_KEY, pageData.appId);
    return;
  }
  const lastAppId = getItem(STORAGE_LAST_APP_ID_KEY);
  if (lastAppId) {
    pageData.appId = lastAppId;
  }
}

async function autoSelectApp() {
  try {
    const result = await pageAppApi({ pageNow: 1, pageSize: 1 });
    if (result.data && result.data.length > 0) {
      pageData.appId = String(result.data[0].id);
      saveItem(STORAGE_LAST_APP_ID_KEY, pageData.appId);
      loadChats();
    } else {
      ElMessage.info('您还没有创建应用，请先前往应用管理创建应用后再开始聊天');
    }
  } catch {
    ElMessage.warning('自动加载应用列表失败，请手动从应用列表进入聊天');
  }
}

onMounted(() => {
  handleQueryStr();
  if (pageData.appId) {
    loadChats();
  } else {
    autoSelectApp();
  }
});
</script>

<style scoped>
.chat-list-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 14px;
}

.context-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 2px;
}

.context-back {
  margin-left: -10px;
}

.context-note {
  color: var(--space-text-soft);
  font-size: 13px;
}

.session-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px 22px;
  min-height: 0;
  border: 1px solid rgba(227, 232, 241, 0.72);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 36px rgba(37, 48, 71, 0.06);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--space-text);
}

.panel-desc {
  margin-top: 3px;
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.4;
}

.panel-header :deep(.workspace-btn--primary) {
  min-width: 116px;
}

.session-search {
  margin-bottom: 12px;
}

.session-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  align-content: start;
  gap: 10px;
  overflow-y: auto;
  padding: 2px 2px 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 78px;
  padding: 12px 14px;
  border: 1px solid var(--space-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  cursor: pointer;
  transition: all 0.2s ease;
}

.session-item:hover {
  border-color: var(--space-primary);
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.1);
}

.session-body {
  flex: 1;
  min-width: 0;
}

.session-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--space-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--space-muted);
}

.session-actions {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}
</style>
