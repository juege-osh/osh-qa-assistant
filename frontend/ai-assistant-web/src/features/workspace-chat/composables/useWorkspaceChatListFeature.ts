import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addChatApi, deleteChatByIdApi, listRecentChatApi, renameChatApi } from '../api/chatApi'
import {
  autoSelectWorkspaceAppId,
  formatWorkspaceChatTime,
  normalizeAppIdFromQuery,
  persistLastWorkspaceAppId,
  restoreLastWorkspaceAppId
} from './useWorkspaceChatShared'
import type { WorkspaceChatSession } from '../types'

export function useWorkspaceChatListFeature() {
  const route = useRoute()
  const router = useRouter()
  const sessionKeyword = ref('')
  const renameDialogVisible = ref(false)
  const renameChatName = ref('')
  const renameChatId = ref('')

  const pageData = reactive({
    appId: '',
    chats: [] as WorkspaceChatSession[]
  })

  const filteredChats = computed(() => {
    if (!sessionKeyword.value) {
      return pageData.chats
    }
    const keyword = sessionKeyword.value.toLowerCase()
    return pageData.chats.filter((chat) => String(chat.chatName || '').toLowerCase().includes(keyword))
  })

  const latestUpdateTime = computed(() => {
    if (!pageData.chats.length) {
      return '暂无'
    }
    return formatWorkspaceChatTime(pageData.chats[0].createTime || '')
  })

  function applyRouteAppId() {
    const appIdFromQuery = normalizeAppIdFromQuery(route.query.appId)
    if (appIdFromQuery) {
      pageData.appId = appIdFromQuery
      persistLastWorkspaceAppId(appIdFromQuery)
      return
    }
    const lastAppId = restoreLastWorkspaceAppId()
    if (lastAppId) {
      pageData.appId = lastAppId
    }
  }

  function ensureAppSelected(showMessage = true) {
    if (String(pageData.appId || '').trim()) {
      return true
    }
    if (showMessage) {
      ElMessage.warning('正在自动加载应用，请稍候...')
      autoSelectApp()
    }
    return false
  }

  function loadChats() {
    if (!ensureAppSelected()) {
      return
    }
    listRecentChatApi(pageData.appId).then((result) => {
      pageData.chats = (result.data || []) as WorkspaceChatSession[]
    })
  }

  function openChat(chatId: string) {
    router.push(`/workspace/chat/${chatId}`)
  }

  function addChat() {
    if (!ensureAppSelected()) {
      return
    }
    addChatApi({
      appId: pageData.appId,
      chatName: `新会话${Date.now()}`
    }).then((result) => {
      ElMessage.success('会话创建成功')
      loadChats()
      router.push(`/workspace/chat/${result.data.id}`)
    })
  }

  function openRenameDialog(chat: WorkspaceChatSession) {
    renameChatId.value = chat.id
    renameChatName.value = chat.chatName
    renameDialogVisible.value = true
  }

  function confirmRename() {
    if (!renameChatName.value.trim()) {
      ElMessage.warning('会话名称不能为空')
      return
    }
    renameChatApi({
      id: renameChatId.value,
      chatName: renameChatName.value
    }).then(() => {
      ElMessage.success('重命名成功')
      renameDialogVisible.value = false
      loadChats()
    })
  }

  function deleteChat(chatId: string) {
    ElMessageBox.confirm('确定删除此会话吗？删除后无法恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    }).then(() => {
      deleteChatByIdApi(chatId).then(() => {
        ElMessage.success('删除成功')
        loadChats()
      })
    }).catch(() => {})
  }

  async function autoSelectApp() {
    const appId = await autoSelectWorkspaceAppId({
      onSelected: (nextAppId) => {
        pageData.appId = nextAppId
      }
    })
    if (appId) {
      pageData.appId = appId
      loadChats()
    }
  }

  watch(() => route.query.appId, () => {
    applyRouteAppId()
    if (pageData.appId) {
      loadChats()
    }
  })

  onMounted(() => {
    applyRouteAppId()
    if (pageData.appId) {
      loadChats()
      return
    }
    autoSelectApp()
  })

  return {
    router,
    sessionKeyword,
    renameDialogVisible,
    renameChatName,
    pageData,
    filteredChats,
    latestUpdateTime,
    formatTime: formatWorkspaceChatTime,
    addChat,
    openChat,
    openRenameDialog,
    confirmRename,
    deleteChat
  }
}
