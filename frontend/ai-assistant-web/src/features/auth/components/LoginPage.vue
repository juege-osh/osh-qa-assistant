<template>
  <AuthSplitLayout>
    <template #showcase>
      <div class="workspace-auth-copy">
        <div class="workspace-auth-kicker">统一工作入口</div>
        <h1 class="workspace-auth-title">进入工作台，继续当前的问答、资料整理与验证进度。</h1>
        <p class="workspace-auth-desc">登录后即可回到统一工作空间，在同一套页面里继续应用配置、知识整理和会话验证。</p>
      </div>

      <div class="workspace-auth-metric-strip">
        <article v-for="item in valueMetrics" :key="item.label" class="workspace-auth-metric">
          <div class="workspace-auth-metric__value">{{ item.value }}</div>
          <div class="workspace-auth-metric__label">{{ item.label }}</div>
        </article>
      </div>

      <div class="workspace-auth-grid">
        <article class="workspace-auth-card workspace-auth-card--compact" v-for="item in valueCards" :key="item.title">
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
        <div class="workspace-auth-panel-kicker">欢迎回来</div>
        <h2 class="workspace-auth-panel-title">登录</h2>
        <p class="workspace-auth-panel-desc">选择身份并进入当前工作空间。</p>
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
          <div class="workspace-auth-segmented">
            <el-segmented v-model="formData.role" :options="roleOptions" />
          </div>
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
          <div class="workspace-auth-helper-text">验证码可点击右侧图片刷新</div>
          <div v-if="formData.role === 'USER'" class="workspace-auth-helper-text">
            还没有账号？
            <el-link @click="toRegister" type="primary" :underline="false">立即注册</el-link>
          </div>
        </div>

        <section class="workspace-auth-note-card">
          <div class="workspace-auth-note-card__title">登录提醒</div>
          <div class="workspace-auth-note-card__desc">先确认身份和当前环境，再输入账号、密码与验证码，能更快进入正确工作空间。</div>
          <div class="workspace-auth-list">
            <div v-for="item in loginTips" :key="item.step" class="workspace-auth-list__item">
              <span class="workspace-auth-list__badge">{{ item.step }}</span>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </section>
      </el-form>
    </template>
  </AuthSplitLayout>
</template>

<script setup lang="ts">
import { ChatDotRound, Connection, Lock, UserFilled } from '@element-plus/icons-vue'
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
  { icon: ChatDotRound, title: '对话验证', text: '继续多轮问答测试，快速查看上下文与输出表现。' },
  { icon: Connection, title: '资料整理', text: '统一收拢文档、知识库与应用配置，减少来回切换。' }
]

const valueMetrics = [
  { value: '统一入口', label: '登录后直接回到对应身份的工作空间' },
  { value: '连续推进', label: '资料整理、应用配置与问答验证在同一路径里完成' }
]

const loginTips = [
  { step: '1', text: '先确认当前身份，系统会按用户端或管理端进入对应页面。' },
  { step: '2', text: '账号、密码和验证码建议都按当前环境重新确认，避免基础输入错误影响进入。' }
]
</script>
