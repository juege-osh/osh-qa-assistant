import { createRouter, createWebHashHistory } from 'vue-router'

const staticRoutes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/features/auth').then((module) => module.LoginPage),
        meta: { authorityName: '登录' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/features/auth').then((module) => module.RegisterPage),
        meta: { authorityName: '注册', role: 'GUEST' }
      },
      {
        path: 'doc',
        name: 'Doc',
        component: () => import('@/views/doc/Doc.vue'),
        meta: { authorityName: '接口文档' }
      },
      {
        path: 'public/app/:slug',
        name: 'PublicAppChat',
        component: () => import('@/features/public-app').then((module) => module.PublicAppPage),
        meta: { authorityName: '公开应用' }
      },
      {
        path: 'admin',
        component: () => import('@/views/admin/manager/ManagerHome.vue'),
        meta: { authorityName: '管理控制台', role: 'ADMIN' },
        children: [
          {
            path: '',
            redirect: '/admin/manager/manage'
          },
          {
            path: 'manager/manage',
            component: () => import('@/views/admin/manager/ManagerManage.vue'),
            meta: { authorityName: '管理员管理', role: 'ADMIN' }
          },
          {
            path: 'user/manage',
            component: () => import('@/views/admin/user/UserManage.vue'),
            meta: { authorityName: '用户管理', role: 'ADMIN' }
          },
          {
            path: 'invokeRecord/manage',
            component: () => import('@/views/admin/invokerecord/InvokeRecordManage.vue'),
            meta: { authorityName: '调用记录管理', role: 'ADMIN' }
          },
          {
            path: 'app/manage',
            component: () => import('@/views/admin/app/AppManage.vue'),
            meta: { authorityName: '应用管理', role: 'ADMIN' }
          },
          {
            path: 'knowledgeLib/manage',
            component: () => import('@/views/admin/knowledgelib/KnowledgeLibManage.vue'),
            meta: { authorityName: '知识库管理', role: 'ADMIN' }
          },
          {
            path: 'uploadFile/manage',
            component: () => import('@/views/admin/uploadfile/UploadFileManage.vue'),
            meta: { authorityName: '文件管理', role: 'ADMIN' }
          }
        ]
      },
      {
        path: 'workspace',
        component: () => import('@/views/personal/PersonalHome.vue'),
        meta: { authorityName: '助手工作台', role: 'USER' },
        children: [
          {
            path: '',
            redirect: '/workspace/app/manage'
          },
          {
            path: 'userCenter',
            component: () => import('@/views/personal/user/UserCenter.vue'),
            meta: { authorityName: '用户中心', role: 'USER' }
          },
          {
            path: 'invokeRecord/manage',
            component: () => import('@/views/personal/invokerecord/InvokeRecordManage.vue'),
            meta: { authorityName: '调用记录管理', role: 'USER' }
          },
          {
            path: 'app/manage',
            component: () => import('@/views/personal/app/AppManage.vue'),
            meta: { authorityName: '应用', role: 'USER' }
          },
          {
            path: 'knowledgeLib/manage',
            component: () => import('@/views/personal/knowledgelib/KnowledgeLibManage.vue'),
            meta: { authorityName: '知识库', role: 'USER' }
          },
          {
            path: 'statistics',
            component: () => import('@/views/personal/statistics/StatisticsHome.vue'),
            meta: { authorityName: '使用统计', role: 'USER' }
          },
          {
            path: 'uploadFile/manage',
            component: () => import('@/views/personal/uploadfile/UploadFileManage.vue'),
            meta: { authorityName: '文档管理', role: 'USER', activeMenuPath: '/workspace/knowledgeLib/manage' }
          },
          {
            path: 'uploadFile/toAdd',
            component: () => import('@/views/personal/uploadfile/AddFile.vue'),
            meta: { authorityName: '添加文档', role: 'USER', activeMenuPath: '/workspace/knowledgeLib/manage' }
          },
          {
            path: 'chat',
            component: () => import('@/features/workspace-chat').then((module) => module.WorkspaceChatListPage),
            meta: { authorityName: '智能聊天助手', role: 'USER', activeMenuPath: '/workspace/app/manage' }
          },
          {
            path: 'chat/:chatId',
            component: () => import('@/features/workspace-chat').then((module) => module.WorkspaceChatConversationPage),
            meta: { authorityName: '对话详情', role: 'USER', activeMenuPath: '/workspace/app/manage' }
          }
        ]
      },
      {
        path: '/:catchAll(.*)',
        component: () => import('@/components/NotFound.vue'),
        meta: { authorityName: '找不到了' }
      }
    ],
    meta: { authorityName: '首页' }
  },
  {
    path: '/redirect/:originalPath(.*)',
    component: () => import('@/components/RedirectSelf.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: staticRoutes
})

export default router
