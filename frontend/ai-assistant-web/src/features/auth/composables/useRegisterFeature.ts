import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRegisterCodeApi, registerApi } from '../api/authApi'
import type { RegisterFormState } from '../types'

export function useRegisterFeature() {
  const formData = reactive<RegisterFormState>({
    pwd: '',
    username: '',
    captchaId: '',
    code: ''
  })

  const verifyData = reactive({
    imageSrc: ''
  })

  const rules = reactive({
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
  })

  const router = useRouter()
  const registerFormRef = ref()

  function register() {
    registerFormRef.value.validate((valid: boolean) => {
      if (!valid) {
        return
      }
      registerApi(formData).then((result) => {
        ElMessage({ message: result.msg, type: 'success' })
        router.push('/login')
        registerFormRef.value.resetFields()
      }).catch(() => {
        refresh()
      })
    })
  }

  function toLogin() {
    router.push('/login')
  }

  function resetForm() {
    registerFormRef.value.resetFields()
  }

  function refresh() {
    getRegisterCodeApi().then((result) => {
      formData.captchaId = result.data.captchaId
      verifyData.imageSrc = result.data.text
    })
  }

  onMounted(() => {
    refresh()
  })

  return {
    formData,
    verifyData,
    rules,
    registerFormRef,
    register,
    toLogin,
    resetForm,
    refresh
  }
}
