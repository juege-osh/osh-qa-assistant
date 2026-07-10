<template>
  <div class="page-shell doc-page">
    <section class="workspace-context-strip">
      <div class="workspace-context-copy">
        <span class="workspace-status-pill workspace-status-pill--active">接口文档</span>
        <span class="workspace-context-note">用于接入公开问答与应用调用能力，建议先确认凭证和流式消费方式，再进入具体接口联调。</span>
      </div>
      <div class="workspace-context-actions">
        <span class="workspace-inline-tag workspace-inline-tag--soft">接口 {{ apiList.length }}</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">JSON 请求</span>
        <span class="workspace-inline-tag workspace-inline-tag--soft">流式响应</span>
      </div>
    </section>

    <section class="doc-layout">
      <aside class="workspace-section-card doc-sidebar">
        <div class="workspace-section-copy">
          <div class="panel-title">目录导航</div>
          <div class="doc-sidebar-desc workspace-section-desc">点击左侧目录可快速跳转到对应接口说明。</div>
        </div>
        <div class="workspace-inline-tags doc-sidebar-tags">
          <span class="workspace-inline-tag workspace-inline-tag--soft">接口 {{ apiList.length }}</span>
          <span class="workspace-inline-tag workspace-inline-tag--soft">JSON</span>
          <span class="workspace-inline-tag workspace-inline-tag--active">流式响应</span>
        </div>
        <section class="workspace-auth-note-card doc-note-card">
          <div class="workspace-auth-note-card__title">联调顺序</div>
          <div class="workspace-auth-note-card__desc">按这个顺序准备，通常能更快排除环境、凭证和流式消费上的基础问题。</div>
          <div class="workspace-auth-list">
            <div class="workspace-auth-list__item">
              <span class="workspace-auth-list__badge">1</span>
              <span>先从账户或应用侧确认 `appKey`、`appId` 和目标环境。</span>
            </div>
            <div class="workspace-auth-list__item">
              <span class="workspace-auth-list__badge">2</span>
              <span>再按文档准备 JSON 请求体和会话标识，避免字段缺失或类型不匹配。</span>
            </div>
            <div class="workspace-auth-list__item">
              <span class="workspace-auth-list__badge">3</span>
              <span>最后确认调用方支持持续读取流式数据块，并在结束标志后主动关闭读取。</span>
            </div>
          </div>
        </section>
        <el-scrollbar class="doc-menu-scroll">
          <el-menu :default-active="activeIndex" class="doc-menu" @select="handleSelect">
            <el-menu-item v-for="(api,index) in apiList" :key="api.id" :index="api.id">
              <span>{{ (index + 1) + '.' }} {{ api.name }}</span>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </aside>

      <div class="doc-content space-scrollbar">
        <section class="workspace-section-card doc-panel doc-panel--overview">
          <div class="workspace-overview-head">
            <div>
              <div class="panel-title">通用请求信息</div>
              <div class="workspace-panel-desc">先确认请求方式、内容类型和基础地址，再按接口参数完成联调。</div>
            </div>
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--active">POST</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">Content-Type JSON</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">SSE</span>
            </div>
          </div>
          <div class="workspace-info-grid doc-info-grid">
            <article class="workspace-info-item">
              <span class="workspace-info-label">请求方式</span>
              <strong class="workspace-info-value">POST</strong>
            </article>
            <article class="workspace-info-item">
              <span class="workspace-info-label">Content-Type</span>
              <strong class="workspace-info-value workspace-info-value--mono">application/json</strong>
            </article>
            <article class="workspace-info-item workspace-info-item--full">
              <span class="workspace-info-label">baseUrl</span>
              <strong class="workspace-info-value workspace-info-value--mono">http://localhost:9000</strong>
            </article>
          </div>
          <div class="summary-list doc-summary-list">
            <div class="summary-item">调用前先从账户或应用侧拿到必要凭证，避免联调时把问题误判成接口异常。</div>
            <div class="summary-item">如果需要接流式返回，调用方要能持续接收数据块，并在收到 `[DONE]` 后主动结束读取。</div>
          </div>
        </section>

        <section class="workspace-section-card doc-focus-panel">
          <div class="workspace-overview-head">
            <div>
              <div class="panel-title panel-title--md">联调前先确认</div>
              <div class="workspace-panel-desc">把最容易漏掉的几件事提前确认掉，能明显减少把接入问题误判成接口异常的情况。</div>
            </div>
          </div>
          <div class="workspace-tip-grid doc-tip-grid">
            <article
              v-for="item in accessChecklist"
              :key="item.title"
              :class="['workspace-tip-card', 'doc-tip-card', item.tone ? `doc-tip-card--${item.tone}` : '']"
            >
              <div class="doc-tip-card__head">
                <span :class="['doc-tip-card__dot', item.tone ? `doc-tip-card__dot--${item.tone}` : '']"></span>
                <div class="workspace-tip-card__title">{{ item.title }}</div>
              </div>
              <div class="workspace-tip-card__desc">{{ item.desc }}</div>
            </article>
          </div>
        </section>

        <section
          v-for="(api,index) in apiList"
          :key="api.id"
          :id="api.id"
          class="workspace-section-card doc-panel doc-api-panel"
        >
          <div class="doc-api-head workspace-overview-head">
            <div class="doc-api-title-wrap">
              <div class="doc-api-index">{{ (index + 1) + '.' }}</div>
              <div>
                <div class="panel-title">{{ api.name }}</div>
                <div class="doc-api-desc workspace-panel-desc">{{ api.desc || '用于当前业务场景下的统一问答请求。' }}</div>
              </div>
            </div>
            <div class="workspace-inline-tags">
              <span class="workspace-inline-tag workspace-inline-tag--active">POST</span>
              <span class="workspace-inline-tag workspace-inline-tag--soft">流式输出</span>
            </div>
          </div>

          <div class="workspace-info-grid doc-info-grid">
            <article class="workspace-info-item">
              <span class="workspace-info-label">使用场景</span>
              <strong class="workspace-info-value">{{ api.name }}</strong>
            </article>
            <article class="workspace-info-item workspace-info-item--full">
              <span class="workspace-info-label">接口地址</span>
              <strong class="workspace-info-value workspace-info-value--mono">{{ api.url }}</strong>
            </article>
          </div>
          <div class="workspace-chip-row workspace-chip-row--wide doc-api-summary">
            <span class="workspace-chip workspace-chip--info">入参 {{ api.params.length }}</span>
            <span class="workspace-chip workspace-chip--warning">必填 {{ requiredCountMap[api.id] || 0 }}</span>
            <span class="workspace-chip workspace-chip--info">流式返回</span>
          </div>

          <div class="workspace-section-copy doc-section-copy">
            <div class="doc-section-title workspace-section-title">请求入参</div>
            <div class="workspace-section-desc">按表格要求准备请求体字段，必填项缺失时通常会直接导致调用失败。</div>
          </div>
          <section class="table-panel doc-table-panel">
            <el-table :data="api.params">
              <el-table-column prop="name" label="参数名" width="180" />
              <el-table-column prop="desc" label="说明" />
              <el-table-column prop="type" label="类型" width="120" />
              <el-table-column prop="required" label="是否必填" width="100" />
            </el-table>
          </section>

          <div class="workspace-section-copy doc-section-copy">
            <div class="doc-section-title workspace-section-title">响应结果</div>
            <div class="workspace-section-desc">当前接口采用流式输出，前端或调用方需要按增量片段持续消费结果。</div>
          </div>
          <section class="workspace-auth-note-card doc-response-note">
            <div class="workspace-auth-note-card__title">流式返回说明</div>
            <div class="workspace-auth-note-card__desc">调用成功后会持续返回数据块，结束标志为 `[DONE]`。如果调用方只等待一次完整响应，通常会把正常的流式输出误判成超时或卡住。</div>
          </section>
        </section>
      </div>
    </section>
  </div>
</template>
<script setup name='Doc' lang='ts'>
import type { AnyObjsDefine } from '@/types/common'
import { computed, ref } from 'vue'

// 接口列表
const apiList = [
  {
    id: 'apiChat',
    name: '智能问答',
    url: '/consumer/api/chat',
    params: [
      { name: 'appKey', desc: '系统颁发的appKey,在用户中心查看', type: 'String', required: '是' },
      { name: 'userInput', desc: '用户输入', type: 'String', required: '是' },
      { name: 'appId', desc: '应用标识,在应用列表中查看', type: 'String', required: '是' },
      { name: 'chatId', desc: '唯一会话标识,需由调用方自行生成,用于区分不同会话', type: 'String', required: '是' },
    ]
  },
] as AnyObjsDefine

const activeIndex = ref('apiChat')

const accessChecklist = [
  {
    title: '先确认 appKey 与 appId',
    desc: '调用前先从账户或应用侧拿到正确凭证，避免把身份错误误判成接口调用失败。',
    tone: 'warning'
  },
  {
    title: '统一使用 JSON 请求',
    desc: '确保请求头和请求体格式一致，字段缺失或类型错误通常会直接导致服务端校验失败。',
    tone: 'success'
  },
  {
    title: '调用方需要支持流式消费',
    desc: '如果前端或脚本没有持续读取数据块，就容易把正常流式输出误以为接口卡住。',
    tone: 'warning'
  }
] as const

const requiredCountMap = computed(() => {
  return apiList.reduce<Record<string, number>>((acc, api) => {
    acc[api.id] = (api.params || []).filter((param: { required?: string }) => param.required === '是').length
    return acc
  }, {})
})

function handleSelect(index: string) {
  activeIndex.value = index
  document.getElementById(index)?.scrollIntoView({ behavior: 'smooth' })
}
</script>
<style scoped>
.doc-page {
  overflow: hidden;
}

.doc-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
  flex: 1;
}

.doc-sidebar,
.doc-panel {
  padding: 18px;
}

.doc-sidebar {
  height: 100%;
  align-self: start;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
}

.doc-sidebar-tags {
  margin-top: 14px;
}

.doc-note-card {
  margin-top: 16px;
}

.doc-menu {
  margin-top: 14px;
}

.doc-menu-scroll {
  min-height: 0;
  max-height: min(56vh, 560px);
  margin-top: 14px;
  padding-right: 4px;
}

.doc-menu-scroll :deep(.el-scrollbar__view) {
  min-height: 100%;
}

.doc-menu :deep(.el-menu) {
  border-right: 0;
  background: transparent;
}

.doc-menu :deep(.el-menu-item) {
  min-height: 40px;
  margin-bottom: 6px;
  padding: 0 12px !important;
  border-radius: 12px;
  color: var(--space-text-soft);
  font-weight: 600;
  line-height: 1.5;
  white-space: normal;
}

.doc-menu :deep(.el-menu-item:hover) {
  background: rgba(239, 241, 255, 0.96) !important;
  color: var(--space-primary) !important;
}

.doc-menu :deep(.el-menu-item.is-active) {
  background: rgba(239, 241, 255, 0.96) !important;
  color: var(--space-primary) !important;
  box-shadow: inset 2px 0 0 var(--space-primary);
}

.doc-info-grid {
  margin-top: 16px;
}

.doc-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.doc-panel--overview {
  background:
    radial-gradient(circle at top right, rgba(99, 91, 255, 0.05), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.95));
}

.doc-focus-panel {
  padding: 18px 20px;
}

.doc-tip-grid {
  margin-top: 14px;
}

.doc-tip-card {
  position: relative;
  overflow: hidden;
}

.doc-tip-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 4px 0 0 4px;
  background: rgba(148, 163, 184, 0.44);
}

.doc-tip-card--success::before {
  background: linear-gradient(180deg, #12b76a 0%, #0f9f5f 100%);
}

.doc-tip-card--warning::before {
  background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%);
}

.doc-tip-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-tip-card__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.78);
  flex-shrink: 0;
}

.doc-tip-card__dot--success {
  background: #12b76a;
}

.doc-tip-card__dot--warning {
  background: #f59e0b;
}

.doc-api-panel {
  scroll-margin-top: 16px;
}

.doc-api-head {
  margin-bottom: 16px;
}

.doc-api-title-wrap {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.doc-api-index {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(99, 91, 255, 0.12);
  background: rgba(239, 241, 255, 0.82);
  color: var(--space-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
}

.doc-section-title {
  line-height: 1.4;
}

.doc-section-copy {
  margin: 18px 0 10px;
}

.doc-table-panel {
  padding: 0;
}

.doc-summary-list {
  margin-top: 16px;
}

.doc-api-summary {
  margin-top: 16px;
  gap: 10px;
}

.doc-response-note {
  margin-top: 10px;
}

@media (max-width: 980px) {
  .doc-layout {
    grid-template-columns: 1fr;
  }

  .doc-info-grid {
    grid-template-columns: 1fr;
  }

  .doc-sidebar {
    height: auto;
    position: static;
  }

  .doc-menu-scroll {
    max-height: none;
  }

  .doc-api-title-wrap {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .doc-tip-grid {
    grid-template-columns: 1fr;
  }
}
</style>
