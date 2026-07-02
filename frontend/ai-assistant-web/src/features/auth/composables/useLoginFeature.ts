import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRoutesByRole } from '@/config/userRoutes'
import { useUserStore } from '@/store/useUserStore'
import { getLoginCodeApi, loginApi } from '../api/authApi'
import type { LoginFormState } from '../types'

export function useLoginFeature() {
  const formData = reactive<LoginFormState>({
    username: '',
    pwd: '',
    captchaId: '',
    code: '',
    role: 'USER'
  })

  const roleOptions = [
    { label: '用户端', value: 'USER' },
    { label: '管理端', value: 'ADMIN' }
  ]

  const verifyData = reactive({
    imageSrc: ''
  })

  const loginFormRef = ref()
  const userStore = useUserStore()
  const router = useRouter()

  const rules = reactive({
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    role: [{ required: true, message: '请选择身份', trigger: 'change' }]
  })

  function submitForm() {
    loginFormRef.value.validate((valid: boolean) => {
      if (!valid) {
        return
      }
      loginApi(formData).then((result) => {
        const userInfo = result.data
        userInfo.userRoutes = getRoutesByRole(userInfo.role)
        userStore.storeUserInfo(userInfo)
        userStore.storeToken(userInfo.token)
        router.replace(userInfo.role === 'ADMIN' ? '/admin/manager/manage' : '/workspace/app/manage')
      }).catch(() => {
        refresh()
      })
    })
  }

  function refresh() {
    getLoginCodeApi().then((result) => {
      formData.captchaId = result.data.captchaId
      verifyData.imageSrc = result.data.text
    })
  }

  function toRegister() {
    router.replace('/register')
  }

  function toForgetPassword() {
    console.log('跳转到忘记密码页面')
  }

  onMounted(() => {
    if (userStore.token) {
      router.push(userStore.userInfo.role === 'ADMIN' ? '/admin/manager/manage' : '/workspace/app/manage')
      return
    }
    refresh()
  })

  return {
    formData,
    roleOptions,
    verifyData,
    loginFormRef,
    rules,
    submitForm,
    refresh,
    toRegister,
    toForgetPassword
  }
}
