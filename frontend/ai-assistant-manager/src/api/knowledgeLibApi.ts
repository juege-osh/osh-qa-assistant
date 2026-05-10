import {doGet,appJsonPost} from '@/network/request'

/**
 * 分页查询
 */
export function pageKnowledgeLibApi(searchData:object) {
  return appJsonPost({
      url: "/knowledgeLib/queryPage",
      data: searchData
  })
}
