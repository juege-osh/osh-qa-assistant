<template>
  <AuthSplitLayout>
    <template #showcase>
      <div class="workspace-auth-copy">
        <div class="workspace-auth-kicker">AI QA Workspace</div>
        <h1 class="workspace-auth-title">构建企业级 AI 评测闭环</h1>
        <p class="workspace-auth-desc">统一管理文档、配置与验证记录，全方位提升模型效果追踪效率。</p>
      </div>

      <div class="workspace-auth-grid login-value-grid">
        <article class="workspace-auth-card" v-for="item in valueCards" :key="item.title">
          <div class="workspace-auth-icon">
            <component :is="item.icon" />
          </div>
          <div>
            <h3 class="workspace-auth-card-title">{{ item.title }}</h3>
            <p class="workspace-auth-card-text">{{ item.text }}</p>
          </div>
        </article>
      </div>
    </template>

    <template #panel>
      <div class="workspace-auth-panel-head">
        <div class="workspace-auth-panel-kicker">Welcome Back</div>
        <h2 class="workspace-auth-panel-title">登录</h2>
        <p class="workspace-auth-panel-desc">选择身份并进入你的工作空间。</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="formData"
        :rules="rules"
        size="large"
        label-position="top"
        class="workspace-auth-form"
        hide-required-asterisk
      >
        <el-form-item label="身份" prop="role">
          <el-segmented v-model="formData.role" :options="roleOptions" class="role-switch" />
        </el-form-item>

        <el-form-item label="账号" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" clearable>
            <template #prefix>
              <el-icon><UserFilled /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="pwd">
          <el-input v-model="formData.pwd" type="password" placeholder="请输入密码" show-password clearable>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
          <div class="workspace-auth-inline-action">
            <el-link type="primary" :underline="false" @click="toForgetPassword">忘记密码？</el-link>
          </div>
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="workspace-auth-captcha-row">
            <el-input v-model="formData.code" placeholder="请输入验证码" maxlength="6" class="workspace-auth-captcha-input" />
            <el-tooltip content="点击刷新验证码" placement="top">
              <div class="workspace-auth-captcha-box" @click="refresh">
                <el-image class="workspace-auth-captcha-img" :src="verifyData.imageSrc" fit="contain" />
              </div>
            </el-tooltip>
          </div>
        </el-form-item>

        <div class="workspace-auth-action-row">
          <el-button type="primary" class="workspace-auth-action-btn" @click="submitForm">进入平台</el-button>
        </div>

        <div class="workspace-auth-helper-row">
          <div v-if="formData.role === 'USER'" class="workspace-auth-helper-text">
            还没有账号？
            <el-link @click="toRegister" type="primary" :underline="false">立即注册</el-link>
          </div>
        </div>
      </el-form>
    </template>
  </AuthSplitLayout>
</template>

<script setup lang="ts">
import { ChatDotRound, Connection, DataAnalysis, Lock, UserFilled } from '@element-plus/icons-vue'
import AuthSplitLayout from './AuthSplitLayout.vue'
import { useLoginFeature } from '../composables/useLoginFeature'

const {
  formData,
  roleOptions,
  verifyData,
  loginFormRef,
  rules,
  submitForm,
  refresh,
  toRegister,
  toForgetPassword
} = useLoginFeature()

const valueCards = [
  { icon: ChatDotRound, title: '对话测试', text: '验证多轮对话效果，追踪上下文理解。' },
  { icon: Connection, title: '知识管理', text: '统一管理文档、配置和验证记录。' },
  { icon: DataAnalysis, title: '结果分析', text: '形成完整的测试闭环和数据追踪。' }
]
</script>

<style scoped>
.login-value-grid {
  grid-template-columns: 1fr;
}

.role-switch {
  width: 100%;
}
</style>
