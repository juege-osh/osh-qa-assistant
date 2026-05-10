import { createWebHashHistory, createRouter } from 'vue-router'
// 定义静态路由
const staticRoutes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/',
    name: "Layout",
    component: () => import("@/views/Layout.vue"),
    children: [
      {
        path: 'login',
        name: "Login",
        component: () => import("@/views/Login.vue"),
        meta: {
          authorityName: "登录"
        }
      },
      {
        path: 'register',
        name: "Register",
        component: () => import("@/views/Register.vue"),
        meta: {
          authorityName: "注册"
        }
      },
      {
        path: 'doc',
        name: 'Doc',
        component: () => import("@/views/doc/Doc.vue"),
        meta: {
          authorityName: '文档'
        }
      },
      {
        path: "personal",
        component: () => import("@/views/personal/PersonalHome.vue"),
        children: [
          {
            path: "userCenter",
            component: () => import("@/views/personal/user/UserCenter.vue"),
            meta: {
              authorityName: "用户中心",
            }
          },
          {
            path: "invokeRecord",
            component: () => import("@/views/personal/invokerecord/InvokeRecordHome.vue"),
            children: [
              {
                path: "manage",
                component: () => import("@/views/personal/invokerecord/InvokeRecordManage.vue"),
                meta: {
                  authorityName: "调用记录管理",
                }
              }
            ]
          },
          {
            path: "app",
            component: () => import("@/views/personal/app/AppHome.vue"),
            children: [
              {
                path: "manage",
                component: () => import("@/views/personal/app/AppManage.vue"),
                meta: {
                  authorityName: "应用",
                }
              }
            ]
          },
          {
            path: "knowledgeLib",
            component: () => import("@/views/personal/knowledgelib/KnowledgeLibHome.vue"),
            children: [
              {
                path: "manage",
                component: () => import("@/views/personal/knowledgelib/KnowledgeLibManage.vue"),
                meta: {
                  authorityName: "知识库",
                }
              }
            ]
          },
          {
            path: "uploadFile",
            component: () => import("@/views/personal/uploadfile/UploadFileHome.vue"),
            children: [
              {
                path: "manage",
                component: () => import("@/views/personal/uploadfile/UploadFileManage.vue"),
                meta: {
                  authorityName: "文档管理",
                }
              },
              {
                path: "toAdd",
                component: () => import("@/views/personal/uploadfile/AddFile.vue"),
                meta: {
                  authorityName: "添加文档",
                }
              },
            ]
          },
          {
            path: "chat",
            component: () => import("@/views/personal/chat/Chat.vue"),
            meta: {
                  authorityName: "智能聊天助手",
            }
          },
        ]
      },
      {
        // vue-router中params传参的方式(/:xxx),xxx可以是任意的值,404配置的时候
        // 一般都叫做pathMatch或catchAll
        path: '/:catchAll(.*)',
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
    path: '/redirect/:originalPath(.*)',
    component: () => import("@/components/RedirectSelf.vue")
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: staticRoutes
})

export default router
