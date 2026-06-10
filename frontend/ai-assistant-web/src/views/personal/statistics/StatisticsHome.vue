<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div class="hero-title">使用统计</div>
      <div class="hero-subtitle">
        查看你的 API 调用情况、成功率和使用趋势，帮助你了解知识库的使用效果。
      </div>
    </section>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">总调用次数</div>
        <div class="stat-value">{{ stats.totalCalls || 0 }}</div>
        <div class="stat-help">累计触发 AI 调用的总次数，用于观察整体使用规模。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">成功次数</div>
        <div class="stat-value" style="color: var(--space-success)">{{ stats.successCalls || 0 }}</div>
        <div class="stat-help">模型成功返回结果的次数，越高说明链路越稳定。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">失败次数</div>
        <div class="stat-value" style="color: #f87171">{{ stats.failCalls || 0 }}</div>
        <div class="stat-help">检索、重排、模型或网络异常都会体现在这里。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">成功率</div>
        <div class="stat-value">{{ stats.successRate || 0 }}%</div>
        <div class="stat-help">成功次数 / 总调用次数，可作为体验健康度的直观指标。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日调用</div>
        <div class="stat-value" style="color: var(--space-secondary)">{{ stats.todayCalls || 0 }}</div>
        <div class="stat-help">观察当日活跃度，方便课程演示和运营复盘。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计 Token</div>
        <div class="stat-value">{{ stats.totalCostToken || 0 }}</div>
        <div class="stat-help">累计消耗的 token 总量，能帮助讲清模型成本和调用规模。</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ stats.avgCostTime || 0 }}ms</div>
        <div class="stat-help">调用链路平均响应时长，适合演示性能优化和排障思路。</div>
      </div>
    </div>

    <section class="glass-panel summary-panel">
      <div class="summary-title">解读建议</div>
      <div class="summary-list">
        <div class="summary-item">如果失败数增加，优先检查知识库文件状态、向量检索和模型密钥。</div>
        <div class="summary-item">如果总调用增长但成功率下降，说明访问量上来了，但链路稳定性需要跟进。</div>
        <div class="summary-item">如果今日调用高但总调用不高，多半是近期正在集中联调或演示。</div>
        <div class="summary-item">如果平均耗时偏高，优先检查向量检索、重排模型和大模型接口延迟。</div>
        <div class="summary-item">如果 token 增长过快，说明上下文过长或提问轮次过多，适合进一步优化提示词和召回策略。</div>
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
