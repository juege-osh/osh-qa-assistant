<template>
  <div>
    <el-dialog
      class="workspace-form-dialog publish-form-dialog"
      title="公开发布"
      :model-value="publishDialogVisible"
      @open="handleOpen"
      :before-close="handleCancel"
      width="720px"
    >
      <div class="dialog-intro">
        设置好公开链接后，别人就能直接访问这个应用。
      </div>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-info-grid">
          <div class="workspace-info-item">
            <div class="workspace-info-label">应用 ID</div>
            <div class="workspace-info-value workspace-info-value--mono">{{ formData.appId || props.idToPublish }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">当前状态</div>
            <div class="workspace-info-value">{{ Number(formData.enabled) === 1 ? '已启用公开访问' : '未启用公开访问' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">访问方式</div>
            <div class="workspace-info-value">{{ formData.accessType === 'PASSWORD' ? '密码访问' : '公开访问' }}</div>
          </div>
          <div class="workspace-info-item workspace-info-item--full">
            <div class="workspace-info-label">建议公开路径</div>
            <div class="publish-link-row">
              <span class="workspace-table-code publish-path-code">{{ publishPath }}</span>
              <el-button class="workspace-btn workspace-btn--ghost" :disabled="!formData.slug" @click="copyPublishPath">
                复制路径
              </el-button>
            </div>
          </div>
        </div>
      </section>
      <section class="workspace-dialog-tip-panel">
        公开访问适合低门槛演示和外部直达；如果需要控制访问范围，优先切到密码访问，并把密码通过业务侧渠道单独发给目标用户。
      </section>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-tip-grid">
          <article v-for="item in publishFocusCards" :key="item.title" class="workspace-tip-card">
            <div class="workspace-tip-card__title">{{ item.title }}</div>
            <div class="workspace-tip-card__desc">{{ item.desc }}</div>
          </article>
        </div>
      </section>
      <el-form
        ref="publishForm"
        :model="formData"
        :rules="rules"
        class="publish-form"
        label-position="top"
      >
        <div class="workspace-form-grid workspace-form-grid--single">
          <el-form-item label="是否启用公开访问" prop="enabled" class="workspace-form-span-2">
            <el-switch
              v-model="enabledSwitch"
              inline-prompt
              active-text="启用"
              inactive-text="关闭"
            />
            <div class="field-help">关闭后，公开链接会暂停使用。</div>
          </el-form-item>
          <el-form-item label="公开访问标识" prop="slug" class="workspace-form-span-2">
            <el-input v-model="formData.slug" maxlength="100" show-word-limit placeholder="例如：pipeline-helper"></el-input>
            <div class="field-help">只允许字母、数字、下划线和中划线，长度 3-100 位。</div>
          </el-form-item>
          <el-form-item label="访问方式" prop="accessType" class="workspace-form-span-2">
            <el-radio-group v-model="formData.accessType" class="workspace-radio-group">
              <el-radio label="PUBLIC" border>公开访问</el-radio>
              <el-radio label="PASSWORD" border>密码访问</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="formData.accessType === 'PASSWORD'"
            label="访问密码"
            prop="accessPassword"
            class="workspace-form-span-2"
          >
            <el-input
              v-model="formData.accessPassword"
              type="password"
              show-password
              clearable
              placeholder="首次启用密码访问时必填；后续留空则沿用旧密码"
            ></el-input>
            <div class="field-help">
              {{ formData.hasPassword ? '留空就继续使用原密码。' : '第一次启用密码访问时需要设置。' }}
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel">取消</el-button>
          <el-button
            class="workspace-btn workspace-btn--ghost"
            :disabled="!formData.appId || Number(formData.enabled) !== 1"
            @click="disablePublish"
          >
            一键关闭
          </el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit">
            保存配置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PublishApp" lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { disablePublishConfigApi, queryPublishConfigApi, savePublishConfigApi } from '@/api/workspace/appApi'
import { writeClipboardText } from '@/util/clipboard'

const props = defineProps<{
  publishDialogVisible: boolean,
  idToPublish: string
}>()

const emitter = defineEmits(["closeDialog", "publishSuccess"])

const publishForm = ref()
const formData = reactive({
  appId: '',
  enabled: 0,
  slug: '',
  accessType: 'PUBLIC',
  accessPassword: '',
  hasPassword: false
})

const enabledSwitch = computed({
  get: () => Number(formData.enabled) === 1,
  set: (value: boolean) => {
    formData.enabled = value ? 1 : 0
  }
})

const publishPath = computed(() => {
  const slug = String(formData.slug || '').trim()
  if (!slug) {
    return `${window.location.origin}/#/public/app/{slug}`
  }
  return `${window.location.origin}/#/public/app/${slug}`
})
const publishFocusCards = computed(() => [
  {
    title: Number(formData.enabled) === 1 ? '当前已经启用公开访问' : '当前公开访问仍然关闭',
    desc: Number(formData.enabled) === 1
      ? '保存后会继续沿用这个公开入口，适合在演示、分享和外部直达场景里使用。'
      : '关闭状态下外部无法直接访问，适合还在调整内容、提示词或访问策略的阶段。'
  },
  {
    title: formData.accessType === 'PASSWORD' ? '密码访问更适合小范围受控分享' : '公开访问更适合低门槛直达体验',
    desc: formData.accessType === 'PASSWORD'
      ? (formData.hasPassword ? '当前已存在访问密码，留空就继续沿用旧密码。' : '首次启用密码访问时，需要先设置一组外部访问密码。')
      : '适合无需单独发密码的演示、推广或临时协作场景，但公开范围会更广。'
  },
  {
    title: formData.slug ? '建议先确认公开标识是否易于识别' : '还需要补一个可读的公开标识',
    desc: formData.slug
      ? `当前公开路径会使用「${formData.slug}」，建议保持简短、清晰，方便后续分享和记忆。`
      : '公开标识会直接出现在访问路径里，建议使用简短且能体现应用用途的英文标识。'
  }
])

const rules = reactive({
  slug: [
    { required: true, message: '请输入公开访问标识', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9][a-zA-Z0-9_-]{2,99}$/,
      message: '公开链接标识只能包含字母、数字、下划线和中划线，长度为3-100位',
      trigger: 'blur'
    }
  ],
  accessType: [{ required: true, message: '请选择访问方式', trigger: 'change' }],
  accessPassword: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (formData.accessType !== 'PASSWORD') {
          callback()
          return
        }
        if (String(value || '').trim() || formData.hasPassword) {
          callback()
          return
        }
        callback(new Error('密码访问模式下请先设置访问密码'))
      },
      trigger: 'blur'
    }
  ]
})

function resetFormData() {
  formData.appId = props.idToPublish
  formData.enabled = 0
  formData.slug = ''
  formData.accessType = 'PUBLIC'
  formData.accessPassword = ''
  formData.hasPassword = false
}

function handleOpen() {
  resetFormData()
  queryPublishConfig()
}

function handleCancel() {
  publishForm.value?.resetFields()
  resetFormData()
  emitter('closeDialog')
}

function queryPublishConfig() {
  queryPublishConfigApi(props.idToPublish).then(result => {
    const data = result.data || {}
    formData.appId = resolveAppId(data.appId)
    formData.enabled = Number(data.enabled ?? 0)
    formData.slug = String(data.slug || '')
    formData.accessType = String(data.accessType || 'PUBLIC')
    formData.accessPassword = ''
    formData.hasPassword = Boolean(data.hasPassword)
  })
}

function resolveAppId(appId?: string | number | null) {
  const rawAppId = appId ?? props.idToPublish
  return String(rawAppId || '').trim()
}

function onSubmit() {
  publishForm.value.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    savePublishConfigApi({
      // Preserve Long ids as strings to avoid JS precision loss before they reach the backend.
      appId: resolveAppId(formData.appId),
      enabled: Number(formData.enabled),
      slug: String(formData.slug || '').trim(),
      accessType: formData.accessType,
      accessPassword: String(formData.accessPassword || '').trim()
    }).then(result => {
      ElMessage.success(result.msg || '保存成功')
      formData.accessPassword = ''
      formData.hasPassword = Boolean(result.data?.hasPassword || formData.accessType === 'PASSWORD')
      emitter('publishSuccess')
    })
  })
}

function disablePublish() {
  disablePublishConfigApi(props.idToPublish).then(result => {
    ElMessage.success(result.msg || '关闭成功')
    formData.enabled = 0
    emitter('publishSuccess')
  })
}

async function copyPublishPath() {
  try {
    await writeClipboardText(publishPath.value)
    ElMessage.success('公开路径已复制')
  } catch (_error) {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.publish-link-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.publish-path-code {
  flex: 1;
  min-width: min(100%, 320px);
  justify-content: flex-start;
}
</style>
