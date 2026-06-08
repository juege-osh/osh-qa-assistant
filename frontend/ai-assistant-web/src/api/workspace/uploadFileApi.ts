import {doGet,appJsonPost} from '@/network/request'
/**
 * 新增
 */
export function addUploadFileApi(data:object) {
  return appJsonPost({
      url: "/consumer/uploadFile/add",
      data: data
  })
}
/**
 * 分页查询
 */
export function pageUploadFileApi(searchData:object) {
  return appJsonPost({
      url: "/consumer/uploadFile/queryPage",
      data: searchData
  })
}
/**
 * 更新文件状态
 */
export function updateUploadFileStatusApi(data:object) {
  return appJsonPost({
      url: "/consumer/uploadFile/updateStatus",
      data: data
  })
}
/**
 * 按id删除
 */
export function deleteUploadFileByIdApi(id:string) {
  return doGet({
      url: "/consumer/uploadFile/deleteById",
      params: {id}
  })
}
