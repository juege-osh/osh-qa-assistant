import { createWebHashHistory, createRouter } from 'vue-router'
// 定义静态路由
const staticRoutes = [
  {
    path:'/login',
    name: "Login",
    component: () => import("@/views/Login.vue"),
    meta: {
      authorityName: "登录"
    }
  },
  {
    path:'/',
    name: "Layout",
    component: () => import("@/views/Layout.vue"),
    children: [
      {
        // vue-router中params传参的方式(/:xxx),xxx可以是任意的值,404配置的时候
        // 一般都叫做pathMatch或catchAll
        path:'/:catchAll(.*)',
        component: () => import("@/components/NotFound.vue"),
        meta: {
          authorityName: "找不到了"
        }
      }
    ],
    meta: {
      authorityName: "首页"
    }
  },
  {
    path:'/redirect/:originalPath(.*)',
    component: () => import("@/components/RedirectSelf.vue")
  }
]

const router = createRouter({
  history:createWebHashHistory(),
  routes:staticRoutes
})

export default router