import { doGet } from '@/network/request'

export function previewFileApi(id: string) {
  return doGet({ url: '/consumer/file/preview', params: { id } })
}
