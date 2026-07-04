import type { UserRoutesDefine } from "@/types/common.d.ts"
/**
 * 用户拥有的路由信息格式
 * 获取时机:登录成功后加载,前端存入到pinia中
 * 用于:
 * 1.菜单的计算
 */
const userRoutes: UserRoutesDefine = [
  {
    path: "personal",
    componentPath: "personal/PersonalHome",
    children: [
      {
        path: "userCenter",
        componentPath: "personal/user/UserCenter",
        meta: {
          authorityName: "用户中心",
          showInMenu: true,
          icon: ""
        }
      },
      {
        path: "invokeRecord",
        componentPath: "personal/invokerecord/InvokeRecordHome",
        children: [
          {
            path: "manage",
            componentPath: "personal/invokerecord/InvokeRecordManage",
            meta: {
              authorityName: "调用记录",
              showInMenu: true,
              icon: ""
            }
          }
        ]
      },
      {
        path: "app",
        componentPath: "personal/app/AppHome",
        children: [
          {
            path: "manage",
            componentPath: "personal/app/AppManage",
            meta: {
              authorityName: "应用",
              showInMenu: true,
              icon: ""
            }
          }
        ]
      },
      {
        path: "knowledgeLib",
        componentPath: "personal/knowledgelib/KnowledgeLibHome",
        children: [
          {
            path: "manage",
            componentPath: "personal/knowledgelib/KnowledgeLibManage",
            meta: {
              authorityName: "知识库",
              showInMenu: true,
              icon: ""
            }
          }
        ]
      },
      {
        path: "statistics",
        componentPath: "personal/statistics/StatisticsHome",
        meta: {
          authorityName: "使用统计",
          showInMenu: true,
          icon: ""
        }
      },
      {
        path: "uploadFile",
        componentPath: "personal/uploadfile/UploadFileHome",
        children: [
          {
            path: "manage",
            componentPath: "personal/uploadfile/UploadFileManage",
            meta: {
              authorityName: "文档管理",
              activeMenuPath: "/personal/knowledgeLib/manage"
            }
          },
          {
            path: "toAdd",
            componentPath: "personal/uploadfile/AddFile",
            meta: {
              authorityName: "添加文档",
              activeMenuPath: "/personal/knowledgeLib/manage"
            }
          }
        ]
      },
      {
        path: "chat",
        componentPath: "personal/chat/Chat",
        meta: {
          authorityName: "智能聊天助手",
          activeMenuPath: "/personal/app/manage"
        }
      }
    ]
  }
]

export default userRoutes
