import router from '@/router'
import Nprogress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStoreExternal } from '@/store/useUserStore'

const userStore = useUserStoreExternal()
const WHITE_URL_LIST = ['/', '/login', '/register', '/doc']

router.beforeEach((toRoute, fromRoute, next) => {
  Nprogress.start()
  if (WHITE_URL_LIST.includes(toRoute.path) || toRoute.path.startsWith('/public/app/')) {
    return next()
  }

  if (!userStore.token) {
    return next({ path: '/login' })
  }

  const requiredRole = toRoute.meta.role as string | undefined
  const currentRole = userStore.userInfo.role
  if (requiredRole && currentRole && requiredRole !== currentRole) {
    return next({ path: currentRole === 'ADMIN' ? '/admin' : '/workspace/chat' })
  }
  return next()
})

router.afterEach((toRoute) => {
  Nprogress.done()
  document.title = (toRoute.meta.authorityName as string) || 'OSH Wisdom'
})
