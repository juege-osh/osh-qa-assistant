import { doGet } from '@/network/request'

export function previewFileApi(id: string) {
  return doGet({ url: '/file/preview', params: { id } })
}
