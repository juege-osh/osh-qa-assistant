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
  padding: 22px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid #e6ecf3;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(18px);
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
  color: #111827;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.public-header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}

.header-meta-item {
  color: #667085;
  font-size: 14px;
}

.public-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.header-btn {
  min-width: 112px;
  height: 44px;
  border-radius: 16px;
  border-color: #dfe6ef;
  color: #344054;
  background: #ffffff !important;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04) !important;
}

.header-btn--primary {
  border: 0 !important;
  color: #ffffff !important;
  background: linear-gradient(180deg, #4da3ff 0%, #2c8cff 100%) !important;
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
    font-size: 20px;
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
