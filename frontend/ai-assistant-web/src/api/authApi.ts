import { doGet, appJsonPost } from '@/network/request'

export function getCodeApi() {
  return doGet({
    url: '/auth/getCode'
  })
}

export function loginApi(data: object) {
  return appJsonPost({
    url: '/auth/login',
    data
  })
}
