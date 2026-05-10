import type { UserRoutesDefine } from "@/types/common.d.ts"
/**
 * 用户拥有的路由信息格式
 * 获取时机:登录成功后加载,前端存入到pinia中
 * 用于:
 * 1.动态路由--tabs
 * 2.菜单的计算
 */
const userRoutes: UserRoutesDefine = [
  {
    path: "manager",
    componentPath: "manager/ManagerHome",
    children: [
      {
        path: "manage",
        componentPath: "manager/ManagerManage",
        meta: {
          authorityName: "管理员管理",
          showInMenu: true,
          icon: "User"
        }
      }
    ]
  },
  {
    path: "user",
    componentPath: "user/UserHome",
    children: [
      {
        path: "manage",
        componentPath: "user/UserManage",
        meta: {
          authorityName: "用户管理",
          showInMenu: true,
          icon: "Avatar"
        }
      }
    ]
  },
  {
    path: "invokeRecord",
    componentPath: "invokerecord/InvokeRecordHome",
    children: [
      {
        path: "manage",
        componentPath: "invokerecord/InvokeRecordManage",
        meta: {
          authorityName: "调用记录管理",
          showInMenu: true,
          icon: "Link"
        }
      }
    ]
  },
  {
    path: "app",
    componentPath: "app/AppHome",
    children: [
      {
        path: "manage",
        componentPath: "app/AppManage",
        meta:{
          authorityName:"应用管理",
          showInMenu:true,
          icon:"Operation"
        }
      }
    ]
  },
  {
    path: "knowledgeLib",
    componentPath: "knowledgelib/KnowledgeLibHome",
    children: [
      {
        path: "manage",
        componentPath: "knowledgelib/KnowledgeLibManage",
        meta:{
          authorityName:"知识库管理",
          showInMenu:true,
          icon:"Notebook"
        }
      }
    ]
  },
  {
    path: "uploadFile",
    componentPath: "uploadfile/UploadFileHome",
    children: [
      {
        path: "manage",
        componentPath: "uploadfile/UploadFileManage",
        meta:{
          authorityName:"文件管理",
          showInMenu:true,
          icon:"Document"
        }
      }
    ]
  },
]

export default userRoutes
