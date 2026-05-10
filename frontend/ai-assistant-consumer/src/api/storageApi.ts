import {filePost} from '@/network/request'

/**
 * 文件上传
 */
export function uploadFileApi(formData:FormData) {
  return filePost("/storage/uploadFile",formData)
}
