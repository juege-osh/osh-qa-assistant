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
      <div class="publish-summary">
        <div class="summary-item">
          <span class="summary-label">应用 ID</span>
          <span class="summary-value">{{ formData.appId || props.idToPublish }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">当前状态</span>
          <el-tag :type="Number(formData.enabled) === 1 ? 'success' : 'info'">
            {{ Number(formData.enabled) === 1 ? '已启用公开访问' : '未启用公开访问' }}
          </el-tag>
        </div>
        <div class="summary-item summary-item--full">
          <span class="summary-label">建议公开路径</span>
          <div class="link-row">
            <el-input :model-value="publishPath" readonly></el-input>
            <el-button class="workspace-btn workspace-btn--ghost" :disabled="!formData.slug" @click="copyPublishPath">
              复制路径
            </el-button>
          </div>
        </div>
      </div>
      <el-form
        ref="publishForm"
        :model="formData"
        :rules="rules"
        class="publish-form"
        label-position="top"
      >
        <div class="form-grid">
          <el-form-item label="是否启用公开访问" prop="enabled" class="field-span-2">
            <el-switch
              v-model="enabledSwitch"
              inline-prompt
              active-text="启用"
              inactive-text="关闭"
            />
            <div class="field-help">关闭后，公开链接会暂停使用。</div>
          </el-form-item>
          <el-form-item label="公开访问标识" prop="slug" class="field-span-2">
            <el-input v-model="formData.slug" maxlength="100" show-word-limit placeholder="例如：pipeline-helper"></el-input>
            <div class="field-help">只允许字母、数字、下划线和中划线，长度 3-100 位。</div>
          </el-form-item>
          <el-form-item label="访问方式" prop="accessType" class="field-span-2">
            <el-radio-group v-model="formData.accessType" class="radio-group">
              <el-radio label="PUBLIC" border>公开访问</el-radio>
              <el-radio label="PASSWORD" border>密码访问</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="formData.accessType === 'PASSWORD'"
            label="访问密码"
            prop="accessPassword"
            class="field-span-2"
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
        <div class="dialog-footer publish-dialog-footer">
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
:deep(.publish-form-dialog) {
  border-radius: 24px !important;
  overflow: hidden;
}

:deep(.publish-form-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 24px 26px 10px;
}

:deep(.publish-form-dialog .el-dialog__body) {
  padding: 0 26px 4px;
}

:deep(.publish-form-dialog .el-dialog__footer) {
  padding: 10px 26px 22px;
}

:deep(.publish-form .el-form-item) {
  margin-bottom: 18px;
}

:deep(.publish-form .el-form-item__label) {
  padding-bottom: 8px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.25;
}

:deep(.publish-form .el-input__wrapper) {
  min-height: 44px;
}

:deep(.publish-form .el-input__inner) {
  font-size: 15px;
}

:deep(.publish-form .el-radio.is-bordered) {
  min-height: 44px;
  margin: 0;
  padding: 0 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
}

.dialog-intro {
  margin-bottom: 16px;
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.6;
}

.publish-summary {
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid var(--space-border);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(249, 250, 251, 0.96), rgba(244, 247, 250, 0.92));
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-item--full {
  grid-column: 1 / -1;
}

.summary-label {
  color: var(--space-text-soft);
  font-size: 12px;
}

.summary-value {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 600;
}

.link-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
}

.field-span-2 {
  grid-column: 1 / -1;
}

.field-help {
  margin-top: 6px;
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.55;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.publish-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 900px) {
  .publish-summary {
    grid-template-columns: 1fr;
  }

  .link-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
