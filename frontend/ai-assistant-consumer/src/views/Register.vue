<template>
  <div class="register">
    <!-- 居中卡片 -->
    <div class="card">
      <h2 class="title">用户注册</h2>
      <el-form ref="registerFormRef" :model="formData" size="large" :rules="rules" label-width="120px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="pwd">
          <el-input v-model="formData.pwd" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入验证码" maxlength="6">
            <template #append>
              <el-image class="captcha-img" :src="verifyData.imageSrc" @click="refresh" />
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" round style="width: 40%" @click="register">
            注册
          </el-button>
          <el-button
            type="primary"
            round
            style="width: 45%"
            @click="resetForm"
          >
            重置
          </el-button>
        </el-form-item>

        <div class="to-login">
          已有账号？
          <el-link @click="toLogin" type="primary" underline="never">
            去登录
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup name='Register' lang='ts'>
import { ref, reactive, onMounted } from "vue";
import { getCodeApi, registerApi } from '@/api/userApi'
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from 'element-plus'
let formData = reactive({
  pwd: '',
  username: '',
  captchaId: '',
  code: '',
})
let verifyData = reactive({
  imageSrc: "",
})
// 校验规则
let rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [
    {
      required: true,
      message: '请输入验证码',
      trigger: 'blur'
    }
  ],
})
let router = useRouter()

let registerFormRef = ref()
function register() {
  registerFormRef.value.validate((valid: boolean) => {
    if (!valid) return
    // 校验通过
    registerApi(formData).then(result => {
      ElMessage({ message: result.msg, type: "success" })
      router.push('/login')
      registerFormRef.value.resetFields()
    }).catch(ex => {
      refresh()
    })
  })
}
function toLogin() {
  router.push("/login")
}
//重置表单
function resetForm() {
  registerFormRef.value.resetFields()
}
/**
 * 刷新验证码
 */
function refresh() {
  getCodeApi().then(result => {
    // 每次请求验证码对应的唯一标识
    formData.captchaId = result.data.captchaId
    verifyData.imageSrc = result.data.text
  })
}
onMounted(() => {
  refresh()
})
</script>

<style scoped>
.login,
.register {
  position: relative;
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 28px;
}

.card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 32px 36px;
  border: 1px solid var(--space-border);
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.card::before {
  content: "OSH WISDOM · 智能知识平台";
  display: block;
  margin-bottom: 10px;
  color: var(--space-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: .14em;
  text-align: center;
}

.title {
  position: relative;
  text-align: center;
  margin-bottom: 24px;
  font-size: 22px;
  font-weight: 700;
  color: var(--space-text);
  letter-spacing: .04em;
}

.title::after {
  content: "";
  display: block;
  width: 60px;
  height: 2px;
  margin: 10px auto 0;
  border-radius: 999px;
  background: var(--space-primary);
}

.captcha-img {
  height: 40px;
  min-width: 112px;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;
}

.btn-group {
  flex: 1;
  display: flex;
  justify-content: space-around;
  gap: 14px;
}

.btn-item {
  flex: 1;
  padding: 0 40px;
}

.to-register,
.to-login {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: var(--space-muted);
}

:deep(.el-input-group__append),
:deep(.el-input-group__prepend) {
  padding: 0 10px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
}
</style>
