import { ElMessage } from 'element-plus'
import { pageAppApi } from '@/api/workspace/appApi'
import { STORAGE_LAST_APP_ID_KEY } from '@/config/constants'
import { getItem, saveItem } from '@/util/storageUtil'

export function formatWorkspaceChatTime(timestamp: string | number | Date) {
  if (!timestamp) {
    return ''
  }
  const date = new Date(timestamp)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

export function normalizeAppIdFromQuery(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]).trim() : ''
  }
  return value ? String(value).trim() : ''
}

export function normalizeRouteChatId(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : ''
  }
  return value ? String(value) : ''
}

export function restoreLastWorkspaceAppId() {
  return getItem(STORAGE_LAST_APP_ID_KEY) || ''
}

export function persistLastWorkspaceAppId(appId: string) {
  saveItem(STORAGE_LAST_APP_ID_KEY, appId)
}

export async function autoSelectWorkspaceAppId(options?: {
  onSelected?: (appId: string) => void
  emptyMessage?: string
  errorMessage?: string
}) {
  try {
    const result = await pageAppApi({ pageNow: 1, pageSize: 1 })
    if (result.data && result.data.length > 0) {
      const appId = String(result.data[0].id)
      persistLastWorkspaceAppId(appId)
      options?.onSelected?.(appId)
      return appId
    }
    ElMessage.info(options?.emptyMessage || '您还没有创建应用，请先前往应用管理创建应用后再开始聊天')
    return ''
  } catch {
    ElMessage.warning(options?.errorMessage || '自动加载应用列表失败，请手动从应用列表进入聊天')
    return ''
  }
}
