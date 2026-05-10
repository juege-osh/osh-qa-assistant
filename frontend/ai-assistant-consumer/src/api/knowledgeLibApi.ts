import {doGet,appJsonPost} from '@/network/request'

/**
 * 新增
 */
export function addKnowledgeLibApi(data:object) {
  return appJsonPost({
      url: "/knowledgeLib/add",
      data: data
  })
}

/**
 * 分页查询
 */
export function pageKnowledgeLibApi(searchData:object) {
  return appJsonPost({
      url: "/knowledgeLib/queryPage",
      data: searchData
  })
}

/**
 * 按id删除
 */
export function deleteKnowledgeLibByIdApi(id:string) {
  return doGet({
      url: "/knowledgeLib/deleteById",
      params: {id}
  })
}

/**
 * 通过id进行查询
 */
export function queryKnowledgeLibByIdApi(id:string) {
  return doGet({
      url: "/knowledgeLib/queryById",
      params: {id}
  })
}
/**
 * 通过id进行修改
 */
export function modifyKnowledgeLibByIdApi(data:object) {
  return appJsonPost({
      url: "/knowledgeLib/modifyById",
      data: data
  })
}
/**
 * 列出当前用户可用的知识库
 */
export function listAvailableLibApi(appId?:string) {
  return doGet({
      url: "/knowledgeLib/listAvailableLib",
      params:{appId}
  })
}
