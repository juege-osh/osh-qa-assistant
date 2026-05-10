import type { RouteRecordRaw } from "vue-router";
import type { UserRoutesDefine } from "@/types/common.d.ts";
import router from "@/router";
/**
 * 引入nprogress的js和css后
 * 开始和结束分别调用Nprogress.start()和Nprogress.done()即可
 */
import Nprogress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStoreExternal } from '@/store/useUserStore'

// moduleObj结构:/src/views/dept/DeptManage.vue: () => import("/src/views/dept/DeptManage.vue")
const moduleObj = import.meta.glob('/src/views/**/*.vue')

// 获取用户相关的store
const userStore = useUserStoreExternal()
const WHITE_URL_LIST = ["/login"]
// 前置守卫进行权限控制
router.beforeEach((toRoute, fromRoute, next) => {
  Nprogress.start()
  
  // 动态路由加载,进入时机:登录之后第一次访问/刷新页面的时候
  if (userStore.userInfo.userRoutes.length > 0 && !userStore.userRoutesAttached) {
    // 执行加载动态路由
    let dynamicRoutes = buildDynamicRoutes(userStore.userInfo.userRoutes)
    // 指定父级为Layout
    dynamicRoutes.forEach(dynamicRoute => router.addRoute("Layout", dynamicRoute))
    // 修改状态为已加载
    userStore.updateUserRoutesAttached(true)
    // 重新触发导航(因为之前访问的时候还没有这个路径)
    return next({ ...toRoute, replace: true })
  }
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

function buildDynamicRoutes(originalRoutes: UserRoutesDefine) {
  let result: RouteRecordRaw[] = []
  originalRoutes.forEach(originalRoute => {
    // 是否是路由
    let isRoute = originalRoute.path
    if (isRoute) {
      // 把orginalRoute转为newRoute
      let newRoute: RouteRecordRaw = {
        path: originalRoute.path as string,
        // () => import("../views/Login.vue")
        component: null,
        children: [],
        meta: { ...originalRoute.meta }
      }
      // 设置component
      newRoute.component = moduleObj[`/src/views/${originalRoute.componentPath}` + '.vue']
      // 递归调用
      if (originalRoute.children) {
        let children = buildDynamicRoutes(originalRoute.children)
        newRoute.children = children
      }
      result.push(newRoute)
    }
  })
  return result
}