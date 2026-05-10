import {doGet,appJsonPost} from '@/network/request'

/**
 * 分页查询
 */
export function pageInvokeRecordApi(searchData:object) {
  return appJsonPost({
      url: "/invokeRecord/queryPage",
      data: searchData
  })
}

export function queryInvokeRecordOverviewApi() {
  return appJsonPost({
      url: "/invokeRecord/queryOverview"
  })
}
