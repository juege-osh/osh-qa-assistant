import {doGet,appJsonPost} from '@/network/request'

/**
 * 分页查询
 */
export function pageUploadFileApi(searchData:object) {
  return appJsonPost({
      url: "/uploadFile/queryPage",
      data: searchData
  })
}
