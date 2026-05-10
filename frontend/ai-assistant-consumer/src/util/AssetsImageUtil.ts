export const getImage = (imageName:string)=> {
  // import.meta.url: http://localhost:8001/src/util/AssetsImageUtil.ts?t=1715706102599
  return new URL(`../assets/images/${imageName}`,import.meta.url).href
}