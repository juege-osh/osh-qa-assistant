<template>
  <div class="workspace-auth-page">
    <div
      :class="[
        'workspace-auth-shell',
        reverse ? 'workspace-auth-shell--reverse' : '',
        panelRight ? 'workspace-auth-shell--panel-right' : ''
      ]"
    >
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
  panelRight?: boolean
  reverse?: boolean
}>(), {
  panelRight: false,
  reverse: false
})
</script>
