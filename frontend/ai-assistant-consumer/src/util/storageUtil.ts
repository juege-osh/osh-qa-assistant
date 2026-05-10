/**
 * 存储key和value到localStorage
 */
export function saveItem (key:string,value:any) {
  if (typeof value === 'object') {
    window.localStorage.setItem(key,JSON.stringify(value))
  }else {
    window.localStorage.setItem(key,value)
  }
}

export function getItem (key:string):string|null {
  // 如果key对应的值不存在返回null
  return window.localStorage.getItem(key)
}

export function clearAll () {
  window.localStorage.clear()
}
