<template>
  <section class="public-header">
    <div class="public-header-main">
      <el-avatar :src="iconUrl" :size="54"></el-avatar>
      <div class="public-header-copy">
        <div class="public-header-title">{{ detail.appName || '公开问答入口' }}</div>
        <div class="public-header-meta">
          <span class="header-meta-item">{{ detail.slug || routeSlug || '未发布' }}</span>
          <span class="header-meta-item">{{ accessModeLabel }}</span>
          <span class="header-meta-item">{{ identityLabel }}</span>
        </div>
      </div>
    </div>

    <div class="public-header-actions">
      <el-button class="header-btn" @click="showAppInfoDialog = true">应用信息</el-button>
      <template v-if="isLoggedIn">
        <el-button class="header-btn" @click="jumpToWorkspace">进入工作台</el-button>
      </template>
      <template v-else>
        <el-button class="header-btn" @click="goLogin">登录</el-button>
        <el-button type="primary" class="header-btn header-btn--primary" @click="goRegister">注册</el-button>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { usePublicAppFeatureModel } from '../composables/usePublicAppFeature'

const model = usePublicAppFeatureModel()
const {
  showAppInfoDialog,
  isLoggedIn,
  detail,
  iconUrl,
  routeSlug,
  accessModeLabel,
  identityLabel,
  goLogin,
  goRegister,
  jumpToWorkspace
} = model
</script>

<style scoped>
.public-header {
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid var(--public-border);
  border-radius: var(--public-radius-panel);
  background: var(--public-panel);
  box-shadow: var(--public-shadow-soft);
}

.public-header-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.public-header-copy {
  min-width: 0;
}

.public-header-title {
  color: var(--public-text);
  font-size: 28px;
  line-height: 1.2;
  font-weight: 750;
  letter-spacing: -0.02em;
}

.public-header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.header-meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--public-border);
  border-radius: 999px;
  background: var(--public-panel-muted);
  color: var(--public-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.public-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.header-btn {
  min-width: 112px;
  height: 40px;
  border-radius: var(--public-radius-control);
  border-color: var(--public-border) !important;
  color: var(--public-text-soft) !important;
  background: var(--public-panel-strong) !important;
  box-shadow: none !important;
}

.header-btn--primary {
  border-color: var(--public-accent) !important;
  color: #ffffff !important;
  background: linear-gradient(180deg, var(--public-accent) 0%, var(--public-accent-strong) 100%) !important;
  box-shadow: 0 10px 18px rgba(99, 91, 255, 0.22) !important;
}

@media (max-width: 900px) {
  .public-header {
    padding: 16px;
    flex-direction: column;
    align-items: stretch;
  }

  .public-header-main {
    align-items: flex-start;
  }

  .public-header-title {
    font-size: 22px;
    line-height: 1.35;
  }

  .public-header-meta {
    gap: 8px;
    margin-top: 8px;
  }

  .public-header :deep(.el-avatar) {
    width: 42px !important;
    height: 42px !important;
  }

  .public-header-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .public-header-title {
    font-size: 18px;
  }
}
</style>
