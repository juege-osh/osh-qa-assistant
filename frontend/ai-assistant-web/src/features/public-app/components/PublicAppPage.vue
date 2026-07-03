<template>
  <div class="public-page">
    <div class="public-shell">
      <section class="public-body">
        <PublicAppSidebar />
        <PublicAppChatPanel />
      </section>
    </div>

    <PublicAppDialogs />
  </div>
</template>

<script setup lang="ts">
import PublicAppChatPanel from './PublicAppChatPanel.vue'
import PublicAppDialogs from './PublicAppDialogs.vue'
import PublicAppSidebar from './PublicAppSidebar.vue'
import { providePublicAppFeatureModel } from '../composables/usePublicAppFeature'

providePublicAppFeatureModel()
</script>

<style scoped>
.public-page {
  --public-accent: #635bff;
  --public-accent-strong: #4b44e8;
  --public-accent-soft: #eff1ff;
  --public-success: #12b76a;
  --public-bg: #f6f9fc;
  --public-bg-soft: #eef3f8;
  --public-panel: #ffffff;
  --public-panel-strong: #ffffff;
  --public-panel-muted: #f9fbff;
  --public-border: #e6ebf2;
  --public-border-strong: #d7dfeb;
  --public-text: #101828;
  --public-text-soft: #344054;
  --public-text-muted: #667085;
  --public-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
  --public-shadow-soft: 0 4px 12px rgba(15, 23, 42, 0.03);
  --public-radius-panel: 20px;
  --public-radius-control: 12px;
  height: 100dvh;
  padding: 20px 22px 24px;
  background:
    radial-gradient(circle at top left, rgba(210, 221, 244, 0.54), transparent 24%),
    radial-gradient(circle at top right, rgba(235, 240, 249, 0.72), transparent 18%),
    linear-gradient(180deg, #f9fbfe 0%, #f6f9fc 18%, #f4f8fc 100%);
  position: relative;
  overflow: hidden;
}

.public-shell {
  max-width: 1500px;
  margin: 0 auto;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.public-body {
  display: grid;
  grid-template-columns: 296px minmax(0, 1fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 1180px) {
  .public-body {
    grid-template-columns: 1fr;
  }

  .history-panel {
    order: 2;
  }

  .chat-panel {
    order: 1;
  }
}

@media (max-width: 900px) {
  .public-page {
    padding: 12px;
  }
  .public-body {
    gap: 14px;
  }

  .history-panel,
  .chat-panel {
    border-radius: 22px;
  }
}
</style>
