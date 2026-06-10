<template>
  <div class="auth-page">
    <div class="auth-shell">
      <section class="auth-showcase">
        <div class="showcase-badge">OSH Wisdom</div>
        <h1 class="showcase-title">让知识库验证、应用调试与会话测试回到同一条工作流。</h1>
        <p class="showcase-desc">
          面向知识平台使用者的统一入口。登录后即可进入应用、知识库与问答工作台，延续当前会话上下文并追踪验证结果。
        </p>

        <div class="showcase-grid">
          <article class="showcase-card showcase-card-wide">
            <div class="showcase-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="showcase-copy">
              <div class="showcase-card-title">连续对话测试</div>
              <div class="showcase-card-text">验证多轮问答、上下文衔接与回答重试效果。</div>
            </div>
          </article>
          <article class="showcase-card">
            <div class="showcase-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="showcase-copy">
              <div class="showcase-card-title">统一工作入口</div>
              <div class="showcase-card-text">一个账号即可访问应用、知识库、文档和记录。</div>
            </div>
          </article>
          <article class="showcase-card">
            <div class="showcase-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="showcase-copy">
              <div class="showcase-card-title">验证闭环更清晰</div>
              <div class="showcase-card-text">从提问、观察结果到记录复盘，链路更完整。</div>
            </div>
          </article>
        </div>
      </section>

      <section class="auth-panel">
        <div class="panel-head">
          <div class="panel-eyebrow">Unified Access</div>
          <h2 class="panel-title">统一登录</h2>
          <p class="panel-desc">选择身份后登录平台，继续你的当前工作流。</p>
        </div>

        <el-form
          ref="loginForm"
          :model="formData"
          :rules="rules"
          size="large"
          label-position="top"
          class="auth-form"
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
              <template #prepend>
                <el-button :icon="UserFilled" />
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
              <template #prepend>
                <el-button :icon="Lock" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="验证码" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="请输入验证码"
              maxlength="6"
            >
              <template #append>
                <el-image
                  class="captcha-img"
                  :src="verifyData.imageSrc"
                  @click="refresh"
                />
              </template>
            </el-input>
          </el-form-item>

          <div class="action-row">
            <el-button type="primary" class="action-btn" @click="submitForm">
              进入平台
            </el-button>
            <el-button class="action-btn action-btn-secondary" @click="resetForm">
              清空
            </el-button>
          </div>

          <div class="helper-row">
            <span class="helper-text">验证码点击即可刷新</span>
            <div class="to-register" v-if="formData.role === 'USER'">
              还没有账号？
              <el-link @click="toRegister" type="primary" underline="never">
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
      router.replace(userInfo.role === 'ADMIN' ? "/admin/manager/manage" : "/workspace/chat");
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

onMounted(() => {
  if (userStore.token) {
    router.push(userStore.userInfo.role === 'ADMIN' ? "/admin/manager/manage" : "/workspace/chat");
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
}

.auth-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 440px);
  gap: 24px;
  width: min(1100px, 100%);
  padding: 24px;
  border: 1px solid rgba(227, 232, 241, 0.8);
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(248, 251, 255, 0.98), rgba(255, 255, 255, 0.92));
  box-shadow: 0 28px 60px rgba(37, 48, 71, 0.1);
}

.auth-shell::before {
  content: "";
  position: absolute;
  inset: 0 auto auto 0;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.16), transparent 70%);
  pointer-events: none;
}

.auth-showcase {
  position: relative;
  padding: 28px;
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f4f8ff 100%);
  border: 1px solid rgba(227, 232, 241, 0.72);
  overflow: hidden;
}

.auth-showcase::after {
  content: "";
  position: absolute;
  right: -40px;
  bottom: -60px;
  width: 220px;
  height: 220px;
  border-radius: 32px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.16), rgba(103, 194, 58, 0.12));
  transform: rotate(18deg);
  pointer-events: none;
}

.showcase-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: var(--space-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.showcase-title {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  max-width: 11ch;
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1.08;
  letter-spacing: -0.04em;
  font-weight: 800;
  color: var(--space-text);
}

.showcase-desc {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  max-width: 52ch;
  color: var(--space-text-soft);
  line-height: 1.75;
  font-size: 14px;
}

.showcase-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.showcase-card {
  display: flex;
  gap: 12px;
  min-height: 116px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(227, 232, 241, 0.88);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 28px rgba(37, 48, 71, 0.05);
}

.showcase-card-wide {
  grid-column: span 2;
}

.showcase-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(102, 177, 255, 0.18), rgba(64, 158, 255, 0.08));
  color: var(--space-primary);
  font-size: 18px;
}

.showcase-card-title {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
}

.showcase-card-text {
  margin-top: 6px;
  color: var(--space-text-soft);
  line-height: 1.65;
  font-size: 13px;
}

.auth-panel {
  position: relative;
  z-index: 1;
  padding: 30px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(227, 232, 241, 0.9);
  box-shadow: 0 18px 40px rgba(37, 48, 71, 0.08);
}

.panel-head {
  margin-bottom: 18px;
}

.panel-eyebrow {
  color: var(--space-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.panel-title {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 800;
  color: var(--space-text);
  letter-spacing: -0.02em;
}

.panel-desc {
  margin-top: 10px;
  color: var(--space-text-soft);
  line-height: 1.7;
  font-size: 13px;
}

.auth-form {
  margin-top: 18px;
}

.role-switch {
  width: 100%;
}

.captcha-img {
  height: 40px;
  min-width: 112px;
  cursor: pointer;
  border-radius: 10px;
  overflow: hidden;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}

.action-btn {
  flex: 1;
  height: 44px;
  margin-left: 0 !important;
  font-weight: 700;
}

.action-btn-secondary {
  color: var(--space-text) !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

.helper-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.helper-text,
.to-register {
  font-size: 13px;
  color: var(--space-muted);
}

:deep(.el-input-group__append),
:deep(.el-input-group__prepend) {
  padding: 0 10px;
  background: #f8fbff;
}

:deep(.el-form-item__label) {
  font-weight: 600;
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .auth-showcase,
  .auth-panel {
    padding: 22px;
  }

  .showcase-title {
    max-width: none;
    font-size: 34px;
  }
}

@media (max-width: 640px) {
  .auth-page {
    min-height: auto;
    padding: 4px;
  }

  .auth-shell {
    gap: 16px;
    padding: 14px;
    border-radius: 22px;
  }

  .showcase-grid {
    grid-template-columns: 1fr;
  }

  .showcase-card-wide {
    grid-column: span 1;
  }

  .action-row {
    flex-direction: column;
  }

  .panel-title {
    font-size: 24px;
  }
}
</style>
