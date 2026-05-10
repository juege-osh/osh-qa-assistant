<template>
  <div class="login">
    <!-- 居中卡片 -->
    <div class="card">
      <h2 class="title">用户登录</h2>
      <el-form :model="formData" :rules="rules" size="large" label-position="top" ref="loginForm">
      <el-form-item label="用户名:" label-width="80px" prop="username">
        <el-input v-model="formData.username" type="text" placeholder="请输入用户名" clearable>
          <template v-slot:prepend><el-button :icon="UserFilled"></el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码:" label-width="80px" prop="pwd">
        <el-input v-model="formData.pwd" type="password" placeholder="请输入密码" show-password clearable>
          <template v-slot:prepend><el-button :icon="Lock"></el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code">
        <el-input v-model="formData.code" type="text" placeholder="请输入验证码">
          <template v-slot:append><el-image class="captcha-img" :src="verifyData.imageSrc"
              @click="refresh" /></template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <div class="btn-group">
          <el-button class="btn-item" type="success" @click="submitForm">登录</el-button>
          <el-button class="btn-item" type="primary" @click="resetForm">重置</el-button>
        </div>
      </el-form-item>
    </el-form>
    </div>
  </div>
</template>
<script setup name='Login' lang='ts'>
import { ref, reactive, onMounted } from "vue";
import { UserFilled, Lock } from "@element-plus/icons-vue"
import { getCodeApi, loginApi } from '@/api/managerApi'
import { useUserStore } from '@/store/useUserStore'
import { useRouter } from "vue-router"
import userRoutes from '@/config/userRoutes'
let formData = reactive({
  username: '',
  pwd: '',
  captchaId: '',
  code: ''
})
let verifyData = reactive({
  imageSrc: "",
})
let loginForm = ref()
let userStore = useUserStore()
let router = useRouter()
// 校验规则
let rules = reactive({
  username: [
    {
      required: true,
      message: '请输入用户名',
      trigger: 'blur'
    }
  ],
  pwd: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur'
    }
  ],
})
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
// 提交表单
function submitForm() {
  loginForm.value.validate((valid: boolean) => {
    if (!valid) return
    loginApi(formData).then(result => {
      // 获取后端返回值
      let userInfo = result.data
      // 设置用户的动态路由信息
      userInfo.userRoutes = userRoutes
      // 调用store进行存储
      userStore.storeUserInfo(userInfo)
      userStore.storeToken(userInfo.token)
      // 跳转到首页
      router.replace("/")
    }).catch(ex => {
      refresh()
    })
  })
}
//重置表单
function resetForm() {
  loginForm.value.resetFields()
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

.login::before,
.register::before {
  content: "";
  position: absolute;
  width: 520px;
  height: 520px;
  left: 8%;
  top: 10%;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #fff, #9ff3ff 10%, rgba(100, 232, 255, 0.24) 28%, rgba(139, 92, 246, 0.16) 54%, transparent 70%);
  filter: blur(.4px);
  opacity: .42;
}

.login::after,
.register::after {
  content: "";
  position: absolute;
  width: 720px;
  height: 180px;
  right: -120px;
  bottom: 12%;
  border-radius: 50%;
  border: 1px solid rgba(100, 232, 255, 0.24);
  transform: rotate(-18deg);
  box-shadow: 0 0 42px rgba(100, 232, 255, 0.16);
}

.card {
  position: relative;
  z-index: 1;
  width: 450px;
  padding: 38px 42px;
  border: 1px solid var(--space-border);
  border-radius: 28px;
  background: linear-gradient(145deg, rgba(12, 20, 48, 0.88), rgba(19, 14, 52, 0.76));
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.5), 0 0 60px rgba(100, 232, 255, 0.12);
  backdrop-filter: blur(22px);
}

.card::before {
  content: "AI ASSISTANT · SPACE CONSOLE";
  display: block;
  margin-bottom: 10px;
  color: var(--space-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .18em;
  text-align: center;
}

.title {
  position: relative;
  text-align: center;
  margin-bottom: 30px;
  font-size: 28px;
  font-weight: 900;
  color: #fff;
  letter-spacing: .08em;
  text-shadow: 0 0 24px rgba(100, 232, 255, 0.45);
}

.title::after {
  content: "";
  display: block;
  width: 88px;
  height: 3px;
  margin: 14px auto 0;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, var(--space-primary), var(--space-pink), transparent);
}

.captcha-img {
  height: 40px;
  min-width: 112px;
  cursor: pointer;
  border-radius: 10px;
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
  font-size: 14px;
  color: var(--space-muted);
}

:deep(.el-input-group__append),
:deep(.el-input-group__prepend) {
  padding: 0 10px;
}

:deep(.el-form-item__label) {
  font-weight: 700;
}
</style>
