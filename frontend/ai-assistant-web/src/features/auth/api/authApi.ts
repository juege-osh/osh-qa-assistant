import { appJsonPost, doGet } from '@/network/request'

export function getLoginCodeApi() {
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

export function getRegisterCodeApi() {
  return doGet({
    url: '/consumer/user/getCode'
  })
}

export function registerApi(data: object) {
  return appJsonPost({
    url: '/consumer/user/register',
    data
  })
}
