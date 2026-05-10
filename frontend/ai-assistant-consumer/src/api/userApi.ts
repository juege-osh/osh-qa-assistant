import {doGet,appJsonPost} from '@/network/request'

/**
 * 登录
 * @param data
 */
export function loginApi(data:object) {
  return appJsonPost({
    url:'/user/login',
    data
  })
}
/**
 * 修改个人信息
 */
export function modifyUserByIdApi(data:object) {
  return appJsonPost({
      url: "/user/modifyById",
      data: data
  })
}
/**
 * 修改密码
 */
export function updatePwdApi(data:object) {
  return appJsonPost({
      url: "/user/updatePwd",
      data: data
  })
}

/**
 * 注册
 */
export function registerApi(data:object) {
  return appJsonPost({
      url: "/user/register",
      data: data
  })
}

/**
 * 通过id进行查询
 */
export function queryUserByIdApi(id:string) {
  return doGet({
      url: "/user/queryById",
      params: {id}
  })
}

/**
 * 获取验证码
 */
export function getCodeApi() {
  return doGet({
      url: "/user/getCode"
  })
}

