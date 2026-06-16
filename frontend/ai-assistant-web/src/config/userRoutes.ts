import type { UserRoutesDefine } from '@/types/common.d.ts'

export const adminRoutes: UserRoutesDefine = [
  {
    path: 'admin',
    componentPath: 'admin/manager/ManagerHome',
    children: [
      {
        path: 'manager/manage',
        componentPath: 'admin/manager/ManagerManage',
        meta: { authorityName: '管理员管理', showInMenu: true, icon: 'User' }
      },
      {
        path: 'user/manage',
        componentPath: 'admin/user/UserManage',
        meta: { authorityName: '用户管理', showInMenu: true, icon: 'Avatar' }
      },
      {
        path: 'invokeRecord/manage',
        componentPath: 'admin/invokerecord/InvokeRecordManage',
        meta: { authorityName: '调用记录管理', showInMenu: true, icon: 'Link' }
      },
      {
        path: 'app/manage',
        componentPath: 'admin/app/AppManage',
        meta: { authorityName: '应用管理', showInMenu: true, icon: 'Operation' }
      },
      {
        path: 'knowledgeLib/manage',
        componentPath: 'admin/knowledgelib/KnowledgeLibManage',
        meta: { authorityName: '知识库管理', showInMenu: true, icon: 'Notebook' }
      },
      {
        path: 'uploadFile/manage',
        componentPath: 'admin/uploadfile/UploadFileManage',
        meta: { authorityName: '文件管理', showInMenu: true, icon: 'Document' }
      }
    ]
  }
]

export const workspaceRoutes: UserRoutesDefine = [
  {
    path: 'workspace',
    componentPath: 'personal/PersonalHome',
    children: [
      {
        path: 'chat',
        componentPath: 'personal/chat/ChatList',
        meta: { authorityName: '智能聊天助手', activeMenuPath: '/workspace/app/manage' }
      },
      {
        path: 'app/manage',
        componentPath: 'personal/app/AppManage',
        meta: { authorityName: '应用', showInMenu: true, icon: 'Operation' }
      },
      {
        path: 'knowledgeLib/manage',
        componentPath: 'personal/knowledgelib/KnowledgeLibManage',
        meta: { authorityName: '知识库', showInMenu: true, icon: 'Notebook' }
      },
      {
        path: 'uploadFile/manage',
        componentPath: 'personal/uploadfile/UploadFileManage',
        meta: { authorityName: '文档管理', activeMenuPath: '/workspace/knowledgeLib/manage' }
      },
      {
        path: 'invokeRecord/manage',
        componentPath: 'personal/invokerecord/InvokeRecordManage',
        meta: { authorityName: '调用记录管理', showInMenu: true, icon: 'Link' }
      },
      {
        path: 'statistics',
        componentPath: 'personal/statistics/StatisticsHome',
        meta: { authorityName: '使用统计', showInMenu: true, icon: 'DataAnalysis' }
      },
      {
        path: 'userCenter',
        componentPath: 'personal/user/UserCenter',
        meta: { authorityName: '用户中心', showInMenu: true, icon: 'User' }
      }
    ]
  }
]

export function getRoutesByRole(role?: string): UserRoutesDefine {
  return role === 'ADMIN' ? adminRoutes : workspaceRoutes
}
