import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { BASE_URL } from '@/config/constants'
import { ElMessage } from 'element-plus'
import { clearAll } from '@/util/storageUtil'
import Nprogress from 'nprogress'
import type { ResultDefine } from '@/types/common.d.ts'
import { useUserStoreExternal } from "@/store/useUserStore";
import { useTabStoreExternal } from '@/store/useTabStore'
import qs from 'qs'

// 对axios的全局设置
axios.defaults.baseURL = BASE_URL
axios.defaults.timeout = 1000 * 60 * 10 // 单位ms
axios.defaults.headers["Content-Type"] = "application/json"

let userStore = useUserStoreExternal()
const tabStore = useTabStoreExternal()
/**
 * 创建axios实例
 */
const instance: AxiosInstance = axios.create()
/**
 * 给Axios实例添加请求、响应拦截器
 */
// 请求拦截器
instance.interceptors.request.use(config => {
  Nprogress.start()
  // 获取token并且加入到请求头中
  const userToken = userStore.token
  // null时后端接收到的是"null"
  if (userToken) {
    config.headers['Authorization'] = userToken
  }
  return config;
}, error => {
  console.log("请求发送失败", error)
  ElMessage({ message: "请求发送失败", type: "error" })
  return Promise.reject("请求发送失败")
});
// 响应拦截器
instance.interceptors.response.use((resultWrapper: AxiosResponse<any, any>) => {
  Nprogress.done()
  // result为服务端返回的结果:通用结果或Blob({size: 449525,type: "application/octet-stream"})
  const result = resultWrapper.data;
  if (!result) {
    // 后端返回的是void的情况result是空的
    return resultWrapper;
  }
  // 非流式请求响应结果
  if (result.success) {
    return result;
  } else {
    if (result.code && result.code === 30001) {
      ElMessage({ message: "权限不足", type: "error" })
      // 没权限或token过期
      clearAll()
      // 重置所有pinia的状态,避免刷新前在app中又存到storage中了
      userStore.resetAll()
      tabStore.resetAll()
      // 刷新当前页面相当于清空了pinia中的状态数据
      window.location.reload()
    } else {
      // 对业务异常进行统一的提示
      ElMessage({ message: result.msg, type: "error" })
      return Promise.reject(result)
    }
  }
},
  // 响应码不是200时进入到promise的onRejected状态回调,AxiosError
  error => {
    Nprogress.done()
    // 如连接超时Network Error等
    console.log("服务器响应失败", error)
    // 对错误进行统一的提示
    ElMessage({ message: "服务器响应失败", type: "error" })
    return Promise.reject("服务器响应失败")
  });

/**
 * 用来发送get请求
 * @param config
 *        {"url":'/abc',params:{id:1}} 或 {url:'/abc?id=1'}
 * @returns {AxiosPromise}
 */
export function doGet(config: AxiosRequestConfig): Promise<ResultDefine> {
  return instance(config)
}

/**
 * 用来发送Content-Type:application/json的post请求
 * @param config
 *        {"url":'/abc',data:{id:1}}
 * @returns {AxiosPromise}
 */
export function appJsonPost(config: AxiosRequestConfig): Promise<ResultDefine> {
  return instance({
    ...config,
    method: "post"
  })
}

/**
 * 用来发送Content-Type:application/x-www-form-urlencoded的post请求
 * @param config
 *        {"url":'/abc',data:{id:1}}
 * @returns {AxiosPromise}
 */
export function formPost(config: AxiosRequestConfig): Promise<ResultDefine> {
  return instance({
    ...config,
    transformRequest(data) {
      // id=1&name=abc&age=34
      return qs.stringify(data)
    },
    method: "post",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    }
  })
}

/**
 * 用来发送Content-Type:multipart/form-data的post请求
 * @param url:'/abc'
 * @param formData:FormData
 * @returns {AxiosPromise}
 */
export function filePost(url: string, formData: FormData): Promise<ResultDefine> {
  return instance.post(url, formData, {
    headers: {
      "Content-Type": "multipart/form-data"
    }
  })
}
/**
 * 用来发送get请求返回流,用于下载
 * @param config
 *        {"url":'/abc',params:{id:1}} 或 {url:'/abc?id=1'}
 * @returns {AxiosPromise}
 */
export function doGetStream(config: AxiosRequestConfig): Promise<AxiosResponse<any, any>> {
  return instance({
    ...config,
    // 指定返回值类型
    responseType:'blob'
  })
}
