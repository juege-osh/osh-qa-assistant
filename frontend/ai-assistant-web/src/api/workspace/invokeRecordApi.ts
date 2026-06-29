import {doGet,appJsonPost} from '@/network/request'

/**
 * 分页查询
 */
export function pageInvokeRecordApi(searchData:object) {
  return appJsonPost({
      url: "/consumer/invokeRecord/queryPage",
      data: searchData
  })
}

export function queryInvokeRecordOverviewApi() {
  return appJsonPost({
      url: "/consumer/invokeRecord/queryOverview"
  })
}

export function saveRagAcceptanceBatchApi(data: object) {
  return appJsonPost({
    url: "/consumer/ragAcceptance/saveBatch",
    data
  })
}

export function listRagAcceptanceBatchApi() {
  return doGet({
    url: "/consumer/ragAcceptance/listMine"
  })
}

export function queryRagAcceptanceBatchDetailApi(id: string | number) {
  return doGet({
    url: "/consumer/ragAcceptance/detail",
    params: { id }
  })
}
