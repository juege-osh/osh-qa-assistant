import { appJsonPost, doGet } from '@/network/request'

export function queryPublicAppDetailApi(slug: string) {
  return doGet({
    url: '/consumer/public/app/detail',
    params: { slug }
  })
}

export function verifyPublicAppPasswordApi(data: object) {
  return appJsonPost({
    url: '/consumer/public/app/verifyPassword',
    data
  })
}
