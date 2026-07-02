<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">使用统计</div>
      <div class="hero-subtitle">
        看看最近用了多少、成功率怎么样。
      </div>
    </section>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ stats.totalCalls || 0 }}</div>
        <div class="stat-help">一共调用了多少次。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">成功次数</div>
        <div class="stat-value" style="color: var(--space-success)">{{ stats.successCalls || 0 }}</div>
        <div class="stat-help">成功返回结果的次数。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">失败次数</div>
        <div class="stat-value" style="color: #f87171">{{ stats.failCalls || 0 }}</div>
        <div class="stat-help">失败次数高时先排查链路异常。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">成功率</div>
        <div class="stat-value">{{ stats.successRate || 0 }}%</div>
        <div class="stat-help">越高越稳定。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日调用</div>
        <div class="stat-value" style="color: var(--space-secondary)">{{ stats.todayCalls || 0 }}</div>
        <div class="stat-help">今天一共调用了多少次。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计 Token</div>
        <div class="stat-value">{{ stats.totalCostToken || 0 }}</div>
        <div class="stat-help">大致反映整体使用量。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ stats.avgCostTime || 0 }}ms</div>
        <div class="stat-help">响应越短，体验通常越顺。</div>
      </div>
    </div>

    <section class="glass-panel summary-panel">
      <div class="summary-title">怎么看这些数据</div>
      <div class="summary-list">
        <div class="summary-item">失败数突然变高时，先检查知识库状态和模型配置。</div>
        <div class="summary-item">调用量上升但成功率下降时，优先看异常链路。</div>
        <div class="summary-item">平均耗时偏高时，先排查检索、重排和模型响应。</div>
        <div class="summary-item">Token 增长过快时，可以回头优化问题长度、提示词和召回策略。</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOverviewApi } from '@/api/workspace/statisticsApi'

const stats = ref<any>({})

onMounted(() => {
  getOverviewApi().then((res: any) => {
    stats.value = res.data || {}
  })
})
</script>

<style scoped>
.summary-panel {
  padding: 18px 20px;
}

.summary-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--space-text);
}

.summary-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.summary-item {
  padding: 12px 16px;
  border: 1px solid var(--space-border);
  border-radius: 8px;
  background: #fafafa;
  color: var(--space-muted);
  line-height: 1.7;
  font-size: 13px;
}
</style>
