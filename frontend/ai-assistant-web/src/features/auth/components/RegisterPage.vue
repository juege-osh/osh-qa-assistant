<template>
  <AuthSplitLayout :reverse="true">
    <template #panel>
      <div class="workspace-auth-panel-head">
        <div class="workspace-auth-panel-kicker">Create Workspace Access</div>
        <h2 class="workspace-auth-panel-title">用户注册</h2>
        <p class="workspace-auth-panel-desc">创建平台账号后，即可开始构建知识库与验证问答效果。</p>
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
          <el-input v-model="formData.username" placeholder="请输入用户名" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="pwd">
          <el-input v-model="formData.pwd" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入验证码" maxlength="6">
            <template #append>
              <el-image class="workspace-auth-captcha-img workspace-auth-captcha-inline" :src="verifyData.imageSrc" @click="refresh" />
            </template>
          </el-input>
        </el-form-item>

        <div class="workspace-auth-action-row workspace-auth-action-row--split">
          <el-button type="primary" class="workspace-auth-action-btn" @click="register">创建账号</el-button>
          <el-button class="workspace-auth-action-btn workspace-auth-action-btn--secondary" @click="resetForm">清空</el-button>
        </div>

        <div class="workspace-auth-helper-row workspace-auth-helper-row--space">
          <span class="workspace-auth-helper-text">注册后可直接返回登录页继续使用</span>
          <div class="workspace-auth-helper-text">
            已有账号？
            <el-link @click="toLogin" type="primary" underline="never">去登录</el-link>
          </div>
        </div>
      </el-form>
    </template>

    <template #showcase>
      <div class="workspace-auth-copy">
        <div class="workspace-auth-kicker">Workspace Setup</div>
        <h1 class="workspace-auth-title">从账号、知识库到应用验证，快速建立自己的问答实验空间。</h1>
        <p class="workspace-auth-desc">
          适合从零开始整理资料、创建应用并进入真实会话测试。注册完成后，你可以持续沉淀知识内容并追踪调用记录。
        </p>
      </div>

      <div class="workspace-auth-grid">
        <article class="workspace-auth-card workspace-auth-card--wide">
          <div class="workspace-auth-icon"><el-icon><Collection /></el-icon></div>
          <div>
            <div class="workspace-auth-card-title">沉淀知识资产</div>
            <div class="workspace-auth-card-text">上传文档、创建知识库，快速搭起可用的内容基础。</div>
          </div>
        </article>
        <article class="workspace-auth-card">
          <div class="workspace-auth-icon"><el-icon><Files /></el-icon></div>
          <div>
            <div class="workspace-auth-card-title">统一管理资料</div>
            <div class="workspace-auth-card-text">文档、应用、调用记录都在同一个工作台里收拢。</div>
          </div>
        </article>
        <article class="workspace-auth-card">
          <div class="workspace-auth-icon"><el-icon><Promotion /></el-icon></div>
          <div>
            <div class="workspace-auth-card-title">快速进入验证</div>
            <div class="workspace-auth-card-text">注册完成即可开始问答测试，缩短从准备到验证的路径。</div>
          </div>
        </article>
      </div>
    </template>
  </AuthSplitLayout>
</template>

<script setup lang="ts">
import { Collection, Files, Promotion } from '@element-plus/icons-vue'
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
</script>
