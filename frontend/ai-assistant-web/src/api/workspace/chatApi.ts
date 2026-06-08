import {doGet,appJsonPost} from '@/network/request'
/**
 * 添加聊天会话
 */
export function addChatApi(data:object) {
  return appJsonPost({
      url: "/consumer/chat/add",
      data: data
  })
}

export function renameChatApi(data:object) {
  return appJsonPost({
      url: "/consumer/chat/rename",
      data: data
  })
}
/**
 * 列出当前用户最近的聊天会话
 */
export function listRecentChatApi(appId:string) {
  return doGet({
      url: "/consumer/chat/listRecent",
      params:{"appId":appId}
  })
}
/**
 * 按id删除
 */
export function deleteChatByIdApi(id:string) {
  return doGet({
      url: "/consumer/chat/deleteById",
      params:{id}
  })
}
/**
 * 聊天
 */
export function chatApi(data:object) {
  return appJsonPost({
      url: "/consumer/chat/chat",
      data: data
  })
}
