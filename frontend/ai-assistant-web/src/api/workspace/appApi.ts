import {doGet,appJsonPost} from '@/network/request'

/**
 * 新增
 */
export function addAppApi(data:object) {
  return appJsonPost({
      url: "/consumer/app/add",
      data: data
  })
}

/**
 * 分页查询
 */
export function pageAppApi(searchData:object) {
  return appJsonPost({
      url: "/consumer/app/queryPage",
      data: searchData
  })
}

/**
 * 按id删除
 */
export function deleteAppByIdApi(id:string) {
  return doGet({
      url: "/consumer/app/deleteById",
      params: {id}
  })
}
/**
 * 检查聊天条件
 */
export function checkChatConditionApi(id:string) {
  return doGet({
      url: "/consumer/app/checkChatCondition",
      params: {id}
  })
}

/**
 * 通过id进行查询
 */
export function queryAppByIdApi(id:string) {
  return doGet({
      url: "/consumer/app/queryById",
      params: {id}
  })
}
/**
 * 解绑知识库
 */
export function unBindLibApi(id:string) {
  return doGet({
      url: "/consumer/app/unBindLib",
      params: {id}
  })
}
/**
 * 通过id进行修改
 */
export function modifyAppByIdApi(data:object) {
  return appJsonPost({
      url: "/consumer/app/modifyById",
      data: data
  })
}
/**
 * 绑定知识库
 */
export function bindLibApi(data:object) {
  return appJsonPost({
      url: "/consumer/app/bindLib",
      data: data
  })
}
