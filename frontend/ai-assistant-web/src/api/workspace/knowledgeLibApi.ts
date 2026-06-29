import {doGet,appJsonPost} from '@/network/request'

/**
 * 新增
 */
export function addKnowledgeLibApi(data:object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/add",
      data: data
  })
}

/**
 * 分页查询
 */
export function pageKnowledgeLibApi(searchData:object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/queryPage",
      data: searchData
  })
}

/**
 * 按id删除
 */
export function deleteKnowledgeLibByIdApi(id:string) {
  return doGet({
      url: "/consumer/knowledgeLib/deleteById",
      params: {id}
  })
}

/**
 * 通过id进行查询
 */
export function queryKnowledgeLibByIdApi(id:string) {
  return doGet({
      url: "/consumer/knowledgeLib/queryById",
      params: {id}
  })
}
/**
 * 通过id进行修改
 */
export function modifyKnowledgeLibByIdApi(data:object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/modifyById",
      data: data
  })
}
/**
 * 列出当前用户可用的知识库
 */
export function listAvailableLibApi(appId?:string) {
  return doGet({
      url: "/consumer/knowledgeLib/listAvailableLib",
      params:{appId}
  })
}

/**
 * 调试知识库召回结果
 */
export function debugKnowledgeLibRecallApi(data:object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/debugRecall",
      data
  })
}

/**
 * 保存知识库实验版本
 */
export function saveKnowledgeLibExperimentApi(data: object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/experiment/save",
      data
  })
}

/**
 * 列出知识库实验版本
 */
export function listKnowledgeLibExperimentApi(libId: string) {
  return doGet({
      url: "/consumer/knowledgeLib/experiment/list",
      params: { libId }
  })
}

/**
 * 重命名知识库实验版本
 */
export function renameKnowledgeLibExperimentApi(data: object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/experiment/rename",
      data
  })
}

/**
 * 标记推荐实验版本
 */
export function recommendKnowledgeLibExperimentApi(data: object) {
  return appJsonPost({
      url: "/consumer/knowledgeLib/experiment/recommend",
      data
  })
}

/**
 * 删除知识库实验版本
 */
export function deleteKnowledgeLibExperimentApi(id: string) {
  return doGet({
      url: "/consumer/knowledgeLib/experiment/deleteById",
      params: { id }
  })
}
