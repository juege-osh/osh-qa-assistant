<template>
  <div class="auth-page">
    <div class="auth-shell register-shell">
      <section class="auth-panel">
        <div class="panel-head">
          <div class="panel-eyebrow">Create Workspace Access</div>
          <h2 class="panel-title">用户注册</h2>
          <p class="panel-desc">创建平台账号后，即可开始构建知识库与验证问答效果。</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="formData"
          size="large"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
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

          <div class="action-row">
            <el-button type="primary" class="action-btn" @click="register">
              创建账号
            </el-button>
            <el-button class="action-btn action-btn-secondary" @click="resetForm">
              清空
            </el-button>
          </div>

          <div class="helper-row">
            <span class="helper-text">注册后可直接返回登录页继续使用</span>
            <div class="to-login">
              已有账号？
              <el-link @click="toLogin" type="primary" underline="never">
                去登录
              </el-link>
            </div>
          </div>
        </el-form>
      </section>

      <section class="auth-showcase">
        <div class="showcase-badge">Workspace Setup</div>
        <h1 class="showcase-title">从账号、知识库到应用验证，快速建立自己的问答实验空间。</h1>
        <p class="showcase-desc">
          适合从零开始整理资料、创建应用并进入真实会话测试。注册完成后，你可以持续沉淀知识内容并追踪调用记录。
        </p>

        <div class="showcase-grid">
          <article class="showcase-card showcase-card-wide">
            <div class="showcase-icon">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="showcase-copy">
              <div class="showcase-card-title">沉淀知识资产</div>
              <div class="showcase-card-text">上传文档、创建知识库，快速搭起可用的内容基础。</div>
            </div>
          </article>
          <article class="showcase-card">
            <div class="showcase-icon">
              <el-icon><Files /></el-icon>
            </div>
            <div class="showcase-copy">
              <div class="showcase-card-title">统一管理资料</div>
              <div class="showcase-card-text">文档、应用、调用记录都在同一个工作台里收拢。</div>
            </div>
          </article>
          <article class="showcase-card">
            <div class="showcase-icon">
              <el-icon><Promotion /></el-icon>
            </div>
            <div class="showcase-copy">
              <div class="showcase-card-title">快速进入验证</div>
              <div class="showcase-card-text">注册完成即可开始问答测试，缩短从准备到验证的路径。</div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup name='Register' lang='ts'>
import { onMounted, reactive, ref } from "vue";
import { Collection, Files, Promotion } from "@element-plus/icons-vue";
import { ElMessage } from 'element-plus';
import { getCodeApi, registerApi } from '@/api/workspace/userApi';
import { useRouter } from "vue-router";

const formData = reactive({
  pwd: '',
  username: '',
  captchaId: '',
  code: '',
});

const verifyData = reactive({
  imageSrc: "",
});

const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  pwd: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [
    {
      required: true,
      message: '请输入验证码',
      trigger: 'blur'
    }
  ],
});

const router = useRouter();
const registerFormRef = ref();

function register() {
  registerFormRef.value.validate((valid: boolean) => {
    if (!valid) return;
    registerApi(formData).then(result => {
      ElMessage({ message: result.msg, type: "success" });
      router.push('/login');
      registerFormRef.value.resetFields();
    }).catch(() => {
      refresh();
    });
  });
}

function toLogin() {
  router.push("/login");
}

function resetForm() {
  registerFormRef.value.resetFields();
}

function refresh() {
  getCodeApi().then(result => {
    formData.captchaId = result.data.captchaId;
    verifyData.imageSrc = result.data.text;
  });
}

onMounted(() => {
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
  grid-template-columns: minmax(360px, 440px) minmax(0, 1.15fr);
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
  inset: auto 0 0 auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.12), transparent 70%);
  pointer-events: none;
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
  left: -50px;
  top: -40px;
  width: 220px;
  height: 220px;
  border-radius: 36px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.14), rgba(103, 194, 58, 0.1));
  transform: rotate(-18deg);
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
  max-width: 12ch;
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
.to-login {
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

  .auth-panel,
  .auth-showcase {
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
