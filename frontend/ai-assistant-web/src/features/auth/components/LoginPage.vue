<template>
  <AuthSplitLayout :panel-right="true">
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
      </el-form>
    </template>

    <template #showcase>
      <div class="workspace-auth-copy workspace-auth-copy--rag">
        <div class="workspace-auth-kicker">RAG 应用问答系统</div>
        <h1 class="workspace-auth-title">让资料接入、检索增强与回答生成，在一个工作台里连成闭环。</h1>
        <p class="workspace-auth-desc">
          登录后继续整理知识、配置应用并执行多轮问答验证，快速观察答案是否真正建立在可检索、可追溯的上下文之上。
        </p>
      </div>

      <section class="workspace-auth-rag-board">
        <div class="workspace-auth-rag-glow workspace-auth-rag-glow--primary"></div>
        <div class="workspace-auth-rag-glow workspace-auth-rag-glow--secondary"></div>

        <div class="workspace-auth-rag-topbar">
          <span class="workspace-auth-rag-chip">知识库在线</span>
          <span class="workspace-auth-rag-chip">支持连续验证</span>
        </div>

        <div class="workspace-auth-rag-flow">
          <article v-for="item in ragPipeline" :key="item.step" class="workspace-auth-rag-step">
            <div class="workspace-auth-rag-step__head">
              <span class="workspace-auth-rag-step__index">{{ item.step }}</span>
              <div class="workspace-auth-icon workspace-auth-icon--rag">
                <component :is="item.icon" />
              </div>
            </div>
            <div class="workspace-auth-rag-step__tag">{{ item.tag }}</div>
            <h3 class="workspace-auth-card-title">{{ item.title }}</h3>
            <p class="workspace-auth-card-text">{{ item.text }}</p>
          </article>
        </div>

      </section>

      <div class="workspace-auth-grid workspace-auth-grid--rag">
        <article class="workspace-auth-card workspace-auth-card--compact workspace-auth-card--rag" v-for="item in ragHighlights" :key="item.title">
          <div class="workspace-auth-icon workspace-auth-icon--rag">
            <component :is="item.icon" />
          </div>
          <div>
            <h3 class="workspace-auth-card-title">{{ item.title }}</h3>
            <p class="workspace-auth-card-text">{{ item.text }}</p>
          </div>
        </article>
      </div>
    </template>
  </AuthSplitLayout>
</template>

<script setup lang="ts">
import { ChatDotRound, Collection, Connection, Lock, Promotion, UserFilled } from '@element-plus/icons-vue'
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

const ragPipeline = [
  { step: '01', icon: Collection, tag: '多源接入', title: '资料入库', text: '将制度文档、FAQ 与业务说明统一沉淀为可持续更新的知识底座。' },
  { step: '02', icon: Connection, tag: '上下文组装', title: '语义检索', text: '围绕当前问题召回相关片段，帮助回答更聚焦、更贴近真实资料。' },
  { step: '03', icon: Promotion, tag: '连续追问', title: '生成作答', text: '在检索结果基础上组织回答，并继续多轮验证上下文与输出质量。' }
]

const ragHighlights = [
  { icon: Collection, title: '统一知识入口', text: '文档、知识库与应用资料在同一工作台收拢，减少分散维护。' },
  { icon: Connection, title: '可检索上下文', text: '回答建立在命中资料之上，便于持续核对来源与命中效果。' },
  { icon: ChatDotRound, title: '多轮问答验证', text: '继续追问、回看上下文与输出表现，让验证过程更连贯。' }
]
</script>
