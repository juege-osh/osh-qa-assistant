import { doGet } from '@/network/request'

export function getOverviewApi() {
  return doGet({ url: '/statistics/overview' })
}
