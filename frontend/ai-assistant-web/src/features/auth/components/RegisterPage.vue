<template>
  <AuthSplitLayout :reverse="true">
    <template #panel>
      <div class="workspace-auth-panel-head">
        <div class="workspace-auth-panel-kicker">创建平台账号</div>
        <h2 class="workspace-auth-panel-title">用户注册</h2>
        <p class="workspace-auth-panel-desc">创建账号后，就可以开始整理资料、创建应用并进入真实问答验证。</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="formData"
        :rules="rules"
        size="large"
        label-position="top"
        class="workspace-auth-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" clearable>
            <template #prefix>
              <el-icon><UserFilled /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="pwd">
          <el-input v-model="formData.pwd" type="password" placeholder="请输入密码" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
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

        <div class="workspace-auth-action-row workspace-auth-action-row--split">
          <el-button type="primary" class="workspace-auth-action-btn" @click="register">创建账号</el-button>
          <el-button class="workspace-auth-action-btn workspace-auth-action-btn--secondary" @click="resetForm">清空</el-button>
        </div>

        <div class="workspace-auth-helper-row workspace-auth-helper-row--space">
          <span class="workspace-auth-helper-text">注册成功后可直接返回登录页继续使用</span>
          <div class="workspace-auth-helper-text">
            已有账号？
            <el-link @click="toLogin" type="primary" underline="never">去登录</el-link>
          </div>
        </div>

        <section class="workspace-auth-note-card">
          <div class="workspace-auth-note-card__title">开始前建议</div>
          <div class="workspace-auth-note-card__desc">先创建账号并确认验证码可用，再按统一流程整理资料、配置应用并进入验证，会更顺畅。</div>
          <div class="workspace-auth-list">
            <div v-for="item in onboardSteps" :key="item.step" class="workspace-auth-list__item">
              <span class="workspace-auth-list__badge">{{ item.step }}</span>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </section>
      </el-form>
    </template>

    <template #showcase>
      <div class="workspace-auth-copy">
        <div class="workspace-auth-kicker">开始搭建自己的工作空间</div>
        <h1 class="workspace-auth-title">从注册开始，逐步建立自己的资料整理、应用配置与问答验证流程。</h1>
        <p class="workspace-auth-desc">
          完成注册后，就可以持续沉淀文档、创建知识库与应用，并在同一工作台里继续真实会话测试。
        </p>
      </div>

      <div class="workspace-auth-metric-strip">
        <article v-for="item in quickFacts" :key="item.label" class="workspace-auth-metric">
          <div class="workspace-auth-metric__value">{{ item.value }}</div>
          <div class="workspace-auth-metric__label">{{ item.label }}</div>
        </article>
      </div>

      <div class="workspace-auth-grid">
        <article class="workspace-auth-card workspace-auth-card--wide workspace-auth-card--compact">
          <div class="workspace-auth-icon"><el-icon><Collection /></el-icon></div>
          <div>
            <div class="workspace-auth-card-title">先搭好资料基础</div>
            <div class="workspace-auth-card-text">上传文档、创建知识库，再进入应用配置和真实验证会更稳。</div>
          </div>
        </article>
        <article class="workspace-auth-card workspace-auth-card--compact">
          <div class="workspace-auth-icon"><el-icon><Files /></el-icon></div>
          <div>
            <div class="workspace-auth-card-title">统一管理资源</div>
            <div class="workspace-auth-card-text">文档、应用与调用记录都在同一个工作台里收拢。</div>
          </div>
        </article>
        <article class="workspace-auth-card workspace-auth-card--compact">
          <div class="workspace-auth-icon"><el-icon><Promotion /></el-icon></div>
          <div>
            <div class="workspace-auth-card-title">尽快进入验证</div>
            <div class="workspace-auth-card-text">准备完成后即可开始问答测试，缩短从准备到验证的路径。</div>
          </div>
        </article>
      </div>
    </template>
  </AuthSplitLayout>
</template>

<script setup lang="ts">
import { Collection, Files, Lock, Promotion, UserFilled } from '@element-plus/icons-vue'
import AuthSplitLayout from './AuthSplitLayout.vue'
import { useRegisterFeature } from '../composables/useRegisterFeature'

const {
  formData,
  verifyData,
  rules,
  registerFormRef,
  register,
  toLogin,
  resetForm,
  refresh
} = useRegisterFeature()

const quickFacts = [
  { value: '快速开始', label: '完成注册后即可进入统一工作台' },
  { value: '持续沉淀', label: '文档、知识库、应用与调用记录可以连续积累' }
]

const onboardSteps = [
  { step: '1', text: '完成账号创建后，先确认验证码和基础登录流程可正常使用。' },
  { step: '2', text: '进入工作台后优先整理文档和知识内容，再继续配置应用与验证。' }
]
</script>
