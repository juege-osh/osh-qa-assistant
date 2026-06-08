import {appJsonPost,doGet} from '@/network/request'

/**
 * 分页查询
 */
export function pageUserApi(searchData:object) {
  return appJsonPost({
      url: "/manager/user/queryPage",
      data: searchData
  })
}
/**
 * 通过id进行查询
 */
export function queryUserByIdApi(id:string) {
  return doGet({
      url: "/manager/user/queryById",
      params: {id}
  })
}
