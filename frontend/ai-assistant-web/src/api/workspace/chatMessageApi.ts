import {doGet,appJsonPost} from '@/network/request'
/**
 * 查询指定聊天的聊天记录
 */
export function listHistoryMessageApi(chatId:string) {
  return doGet({
      url: "/consumer/chatMessage/listHistory",
      params: {"chatId":chatId}
  })
}
