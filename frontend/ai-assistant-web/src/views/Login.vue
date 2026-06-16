<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-showcase">
        <div class="showcase-header">
          <h1 class="showcase-main-title">构建企业级 AI 评测闭环</h1>
          <p class="showcase-subtitle">统一管理文档、配置与验证记录，全方位提升模型效果追踪效率。</p>
        </div>

        <div class="value-grid">
          <div class="value-card">
            <div class="value-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="value-content">
              <h3 class="value-title">对话测试</h3>
              <p class="value-text">验证多轮对话效果，追踪上下文理解</p>
            </div>
          </div>

          <div class="value-card">
            <div class="value-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="value-content">
              <h3 class="value-title">知识管理</h3>
              <p class="value-text">统一管理文档、配置和验证记录</p>
            </div>
          </div>

          <div class="value-card">
            <div class="value-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="value-content">
              <h3 class="value-title">结果分析</h3>
              <p class="value-text">完整的测试闭环和数据追踪</p>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="panel-head">
          <h2 class="panel-title">登录</h2>
        </div>

        <el-form
          ref="loginForm"
          :model="formData"
          :rules="rules"
          size="large"
          label-position="top"
          class="auth-form"
          hide-required-asterisk
        >
          <el-form-item label="身份" prop="role">
            <el-segmented v-model="formData.role" :options="roleOptions" class="role-switch" />
          </el-form-item>

          <el-form-item label="账号" prop="username">
            <el-input
              v-model="formData.username"
              placeholder="请输入用户名"
              clearable
            >
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="pwd">
            <el-input
              v-model="formData.pwd"
              type="password"
              placeholder="请输入密码"
              show-password
              clearable
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
            <div class="form-item-extra">
              <el-link type="primary" :underline="false" @click="toForgetPassword">
                忘记密码？
              </el-link>
            </div>
          </el-form-item>

          <el-form-item label="验证码" prop="code">
            <div class="captcha-row">
              <el-input
                v-model="formData.code"
                placeholder="请输入验证码"
                maxlength="6"
                class="captcha-input"
              />
              <el-tooltip content="点击刷新验证码" placement="top">
                <div class="captcha-box" @click="refresh">
                  <el-image
                    class="captcha-img"
                    :src="verifyData.imageSrc"
                    fit="contain"
                  />
                </div>
              </el-tooltip>
            </div>
          </el-form-item>

          <div class="action-row">
            <el-button type="primary" class="action-btn" @click="submitForm">
              进入平台
            </el-button>
          </div>

          <div class="helper-row">
            <div class="to-register" v-if="formData.role === 'USER'">
              还没有账号？
              <el-link @click="toRegister" type="primary" :underline="false">
                立即注册
              </el-link>
            </div>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup name='Login' lang='ts'>
import { onMounted, reactive, ref } from "vue";
import {
  ChatDotRound,
  Connection,
  DataAnalysis,
  Lock,
  UserFilled,
} from "@element-plus/icons-vue";
import { getCodeApi, loginApi } from '@/api/authApi';
import { getRoutesByRole } from '@/config/userRoutes';
import { useUserStore } from '@/store/useUserStore';
import { useRouter } from "vue-router";

const formData = reactive({
  username: '',
  pwd: '',
  captchaId: '',
  code: '',
  role: 'USER'
});

const roleOptions = [
  { label: '用户端', value: 'USER' },
  { label: '管理端', value: 'ADMIN' }
];

const verifyData = reactive({
  imageSrc: "",
});

const loginForm = ref();
const userStore = useUserStore();
const router = useRouter();

const rules = reactive({
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
  code: [
    {
      required: true,
      message: '请输入验证码',
      trigger: 'blur'
    }
  ],
  role: [
    {
      required: true,
      message: '请选择身份',
      trigger: 'change'
    }
  ],
});

function submitForm() {
  loginForm.value.validate((valid: boolean) => {
    if (!valid) return;
    loginApi(formData).then(result => {
      const userInfo = result.data;
      userInfo.userRoutes = getRoutesByRole(userInfo.role);
      userStore.storeUserInfo(userInfo);
      userStore.storeToken(userInfo.token);
      router.replace(userInfo.role === 'ADMIN' ? "/admin/manager/manage" : "/workspace/app/manage");
    }).catch(() => {
      refresh();
    });
  });
}

function refresh() {
  getCodeApi().then(result => {
    formData.captchaId = result.data.captchaId;
    verifyData.imageSrc = result.data.text;
  });
}

function resetForm() {
  loginForm.value.resetFields();
}

function toRegister() {
  router.replace("/register");
}

function toForgetPassword() {
  // TODO: 跳转到忘记密码页面
  console.log('跳转到忘记密码页面');
  // router.push("/forget-password");
}

onMounted(() => {
  if (userStore.token) {
    router.push(userStore.userInfo.role === 'ADMIN' ? "/admin/manager/manage" : "/workspace/app/manage");
  }
  refresh();
});
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: calc(100vh - 178px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #F7F8FA;
}

.auth-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 420px);
  gap: 40px;
  width: min(1140px, 100%);
  padding: 40px;
  border: none;
  border-radius: 24px;
  background: #FFFFFF;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

.auth-showcase {
  position: relative;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.auth-showcase::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.auth-showcase::after {
  content: '';
  position: absolute;
  bottom: -80px;
  left: -80px;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

/* 左侧价值主张 */
.showcase-header {
  position: relative;
  z-index: 1;
  margin-bottom: 40px;
}

.showcase-main-title {
  font-size: 28px;
  font-weight: 600;
  color: #1D2129;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.showcase-subtitle {
  font-size: 14px;
  color: #86909C;
  line-height: 1.7;
  margin: 0;
}

/* 价值卡片网格 */
.value-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  position: relative;
  z-index: 1;
}

.value-card {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #F2F6FF;
  border: none;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.value-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #2563EB 0%, #3B82F6 100%);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.value-card:hover::before {
  transform: scaleX(1);
}

.value-card:hover {
  background: #E8F0FE;
  box-shadow:
    0 4px 16px rgba(37, 99, 235, 0.1),
    0 2px 8px rgba(37, 99, 235, 0.06);
  transform: translateY(-4px);
}

.value-icon {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: #2563EB;
  color: #FFFFFF;
  font-size: 24px;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.value-card:hover .value-icon {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
}

.value-content {
  flex: 1;
  min-width: 0;
}

.value-title {
  font-size: 17px;
  font-weight: 700;
  color: #1D2129;
  margin: 0;
  transition: color 0.2s ease;
}

.value-card:hover .value-title {
  color: #2563EB;
}

.value-text {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
  color: #86909C;
}

.auth-panel {
  position: relative;
  padding: 48px 36px;
  border-radius: 20px;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  box-shadow: 0 4px 16px rgba(17, 24, 39, 0.04);
}

.panel-head {
  margin-bottom: 32px;
}

.panel-title {
  font-size: 32px;
  font-weight: 800;
  color: #111827;
  letter-spacing: -0.02em;
}

.panel-desc {
  margin-top: 8px;
  color: #6B7280;
  line-height: 1.5;
  font-size: 15px;
}

.auth-form {
  margin-top: 28px;
}

.role-switch {
  width: 100%;
}

.action-row {
  margin-top: 24px;
}

.action-btn {
  width: 100%;
  height: 44px;
  font-weight: 700;
  font-size: 16px;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.helper-row {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.to-register {
  font-size: 14px;
  color: #6B7280;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
  margin-bottom: 8px;
  line-height: 1.5;
}

:deep(.el-form-item__error) {
  font-size: 13px;
  color: #EF4444;
  padding-top: 6px;
}

.form-item-extra {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.form-item-extra .el-link {
  font-size: 13px;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #E5E7EB inset;
  padding: 0 14px;
  height: 40px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  background: #FAFBFC;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #D1D5DB inset;
  background: #FFFFFF;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #2563EB inset;
  background: #FFFFFF;
}

:deep(.el-input__prefix) {
  color: #9CA3AF;
  margin-right: 8px;
  display: flex;
  align-items: center;
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: #111827;
  line-height: 40px;
  height: 40px;
}

:deep(.el-input__inner::placeholder) {
  color: #D1D5DB;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  border-color: #2563EB;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #1D4ED8 0%, #1E40AF 100%);
  border-color: #1D4ED8;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
  transform: translateY(-1px);
}

:deep(.el-button--primary:active) {
  transform: translateY(0);
}

:deep(.el-segmented) {
  background: #F3F4F6;
  padding: 4px;
  border-radius: 10px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05);
  height: 48px;
}

:deep(.el-segmented__item) {
  border-radius: 8px;
  font-weight: 600;
  padding: 0 20px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

:deep(.el-segmented__item.is-selected) {
  background: #2563EB;
  color: #FFFFFF;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

:deep(.el-segmented__item:not(.is-selected):hover) {
  background: #E5E7EB;
}

/* 验证码布局 */
.captcha-row {
  display: flex;
  gap: 12px;
}

.captcha-input {
  flex: 1;
}

.captcha-box {
  width: 140px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #E5E7EB;
  background: #F9FAFB;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.captcha-box:hover {
  border-color: #2563EB;
  background: #EFF6FF;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.captcha-img {
  width: 100%;
  height: 100%;
  filter: grayscale(30%) contrast(85%) brightness(1.05);
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
    padding: 32px;
    gap: 32px;
  }

  .auth-showcase,
  .auth-panel {
    padding: 36px 28px;
  }

  .showcase-main-title {
    font-size: 24px;
  }

  .value-grid {
    gap: 20px;
  }
}

@media (max-width: 640px) {
  .auth-page {
    min-height: auto;
    padding: 8px;
  }

  .auth-shell {
    padding: 20px;
    border-radius: 20px;
    gap: 24px;
  }

  .auth-showcase {
    padding: 28px 20px;
  }

  .auth-panel {
    padding: 32px 20px;
  }

  .panel-title {
    font-size: 28px;
  }

  .showcase-main-title {
    font-size: 22px;
  }

  .showcase-subtitle {
    font-size: 13px;
  }

  .value-grid {
    gap: 16px;
  }

  .value-card {
    padding: 16px;
  }
}
</style>
