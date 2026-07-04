<template>
  <div class="workspace-auth-page">
    <div :class="['workspace-auth-shell', reverse ? 'workspace-auth-shell--reverse' : '']">
      <section class="workspace-auth-panel workspace-auth-panel--form">
        <slot name="panel"></slot>
      </section>
      <section class="workspace-auth-showcase">
        <slot name="showcase"></slot>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  reverse?: boolean
}>(), {
  reverse: false
})
</script>
