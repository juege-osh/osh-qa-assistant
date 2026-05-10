import router from "@/router";
/**
 * 引入nprogress的js和css后
 * 开始和结束分别调用Nprogress.start()和Nprogress.done()即可
 */
import Nprogress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStoreExternal } from '@/store/useUserStore'

// 获取用户相关的store
const userStore = useUserStoreExternal()
const WHITE_URL_LIST = ["/","/login","/register","/doc"]
// 前置守卫进行权限控制
router.beforeEach((toRoute, fromRoute, next) => {
  Nprogress.start()
  // 获取token
  let token = userStore.token
  if (WHITE_URL_LIST.includes(toRoute.path)) {
    // 访问的路径在白名单中
    return next()
  } else {
    if (token) {
      return next()
    }
    // 没token则跳转到登录页面
    return next({ path: WHITE_URL_LIST[0] })
  }
})

// 后置守卫
router.afterEach((toRoute, fromRoute) => {
  Nprogress.done()
  document.title = toRoute.meta.authorityName as string
})
