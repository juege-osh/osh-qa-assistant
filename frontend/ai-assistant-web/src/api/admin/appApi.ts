import {doGet,appJsonPost} from '@/network/request'

/**
 * 分页查询
 */
export function pageAppApi(searchData:object) {
  return appJsonPost({
      url: "/manager/app/queryPage",
      data: searchData
  })
}
