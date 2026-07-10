import { appJsonPost, doGet } from '@/network/request'

export function addChatApi(data: object) {
  return appJsonPost({
    url: '/consumer/chat/add',
    data
  })
}

export function renameChatApi(data: object) {
  return appJsonPost({
    url: '/consumer/chat/rename',
    data
  })
}

export function listRecentChatApi(appId: string) {
  return doGet({
    url: '/consumer/chat/listRecent',
    params: { appId }
  })
}

export function deleteChatByIdApi(id: string) {
  return doGet({
    url: '/consumer/chat/deleteById',
    params: { id }
  })
}

export function chatApi(data: object) {
  return appJsonPost({
    url: '/consumer/chat/chat',
    data
  })
}

export function listHistoryMessageApi(chatId: string) {
  return doGet({
    url: '/consumer/chatMessage/listHistory',
    params: { chatId }
  })
}
