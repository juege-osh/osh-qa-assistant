import { doGet } from '@/network/request'

export function getOverviewApi() {
  return doGet({ url: '/consumer/statistics/overview' })
}
