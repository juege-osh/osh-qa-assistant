import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router/index'
import '@/permission/permission'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import pinia from '@/store'
import '@/assets/css/common.css'

const app = createApp(App)
// 使用路由
app.use(router)
// 使用ui库
app.use(ElementPlus,{size:"small",locale:zhCn})
// 将图标注册为vue的全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  // key:组件的name,如Plus,component:组件 
  app.component(key, component)
}
// 使用pinia
app.use(pinia)
app.mount('#app')
