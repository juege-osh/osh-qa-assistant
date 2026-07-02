<template>
  <div class="public-page">
    <div class="public-shell">
      <PublicAppHeader />

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
import PublicAppHeader from './PublicAppHeader.vue'
import PublicAppSidebar from './PublicAppSidebar.vue'
import { providePublicAppFeatureModel } from '../composables/usePublicAppFeature'

providePublicAppFeatureModel()
</script>

<style scoped>
.public-page {
  min-height: 100dvh;
  padding: 14px;
  background:
    radial-gradient(circle at top left, rgba(225, 238, 252, 0.82), transparent 24%),
    linear-gradient(180deg, #f0f5fb 0%, #f7f8fb 18%, #f4f7fb 100%);
  position: relative;
}

.public-page::before {
  content: '';
  position: fixed;
  inset: 14px;
  border-radius: 28px;
  border: 1px solid rgba(214, 224, 236, 0.92);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.85);
  pointer-events: none;
}

.public-shell {
  min-height: calc(100dvh - 28px);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.public-body {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 18px;
  flex: 1;
  min-height: 0;
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
    padding: 10px;
  }

  .public-page::before {
    inset: 10px;
    border-radius: 22px;
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
