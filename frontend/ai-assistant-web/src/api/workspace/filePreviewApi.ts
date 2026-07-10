import { appJsonPost, doGet } from '@/network/request'

export function previewFileApi(id: string) {
  return doGet({ url: '/consumer/file/preview', params: { id } })
}

export function previewFileSplitApi(data: object) {
  return appJsonPost({ url: '/consumer/file/previewSplit', data })
}
