import type { LocationQuery } from "vue-router"

/**
 * 用户路由meta信息
 */
export interface UserRouteMetaDefine {
  // 权限名称
  authorityName:string,
  // 是否需要在菜单中显示
  showInMenu?:boolean,
  // element plus图标的名字,如  Plus
  icon?:string,
}
/**
 * 用户路由信息
 */
export interface UserRouteDefine {
  // 相对路径,如/clazz/manage,此处就写 manage
  path?:string,
  // 组件相对src/views/的相对路径
  componentPath?:string,
  meta?:UserRouteMetaDefine,
  children?:UserRouteDefine[]
}

export type UserRoutesDefine = Array<UserRouteDefine>

/**
 * 菜单定义
 */
export interface MenuItemDefine {
  // 绝对访问路径,如 /user/manage
  path:string,
  // 菜单名称,由authorityName而来
  menuName:string,
  // element plus图标的名字,如  Plus
  icon:string,
  // 子级菜单
  children?:MenuItemDefine[]
}
export type MenuItemsDefine = Array<MenuItemDefine>

/**
 * 选项卡
 */
export interface TabDefine {
  // 当前tab对应的route.path,如/index
  path:string,
  // 页面上显示的tab的名字,从路由的authorityName获取
  text:string,
  // 当前tab对应的route.fullPath,如/index?a=33
  fullPath:string,
  // 查询字符串
  query?:LocationQuery
}

// Tab的数组类型
export type TabArrayDefine = Array<TabDefine>

/**
 * 用户信息
 */
export interface ManagerDefine {
  // id
  id: string,
  // 用户名
  username: string,
  // 图片地址
  avatarPath:string,
  // 用户的动态路由
  userRoutes:UserRoutesDefine
}
/**
 * 后端统一返回结构
 */
export interface ResultDefine {
  data:any,
  code:number,
  msg:string,
  success:boolean,
  total:string,
  extra: AnyObjDefine
}

/**
 * 一个对象,这个对象的key可以是任意string类型的字符串,value可以是任意类型
 */
export interface AnyObjDefine {
  [key:string]: any
}

export type AnyObjsDefine = Array<AnyObjDefine>

export interface UseTableOptDefine {
  // 表格查询业务参数
  searchFormData?:AnyObjDefine,
  // 表格查询api
  loadTableApi?:(obj:object) => Promise<ResultDefine>,
  // 按id删除api
  deleteByIdApi?:(id:string) => Promise<ResultDefine>
}
