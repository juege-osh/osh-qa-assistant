<template>
  <div>
    <el-dialog
      class="workspace-form-dialog"
      title="绑定知识库"
      :model-value="bindLibDialogVisible"
      @open="handleOpen"
      :before-close="handleCancel"
      width="640px"
    >
      <div class="dialog-intro">
        绑定后，这个应用就会优先使用所选知识库参与回答。
      </div>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">应用名称</div>
            <div class="workspace-info-value">{{ formData.appName || '--' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">应用 ID</div>
            <div class="workspace-info-value workspace-info-value--mono">{{ formData.id || '--' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">当前绑定</div>
            <div class="workspace-info-value">{{ currentLibName || '暂未绑定知识库' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">本次选择</div>
            <div class="workspace-info-value">{{ selectedLibName }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">可选知识库</div>
            <div class="workspace-info-value">{{ availableLibCountDisplay }}</div>
          </div>
        </div>
      </section>
      <el-form ref="bindForm" :model="formData" :rules="rules" label-position="top">
        <div class="workspace-form-grid workspace-form-grid--single">
          <el-form-item label="应用名称" prop="appName">
            <el-input v-model="formData.appName" disabled></el-input>
          </el-form-item>
          <el-form-item label="选择知识库" prop="libId">
            <el-select v-model="formData.libId" placeholder="请选择知识库" filterable :disabled="!pageData.knowledgeLibs.length">
              <el-option v-for="one in pageData.knowledgeLibs" :key="one.id" :label="one.libName"
                :value="one.id"></el-option>
            </el-select>
            <div class="field-help">
              {{ pageData.knowledgeLibs.length ? '后续还可以在应用管理页重新绑定或解绑。' : '当前还没有可用知识库，建议先去知识库管理创建后再回来绑定。' }}
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" :disabled="!pageData.knowledgeLibs.length" @click="onSubmit()">确认绑定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='AddApp' lang='ts'>
import { computed, reactive, ref } from 'vue'
import { bindLibApi, queryAppByIdApi } from '@/api/workspace/appApi';
import { ElMessage } from 'element-plus';
import type { AnyObjDefine, AnyObjsDefine } from '@/types/common';
import { listAvailableLibApi } from '@/api/workspace/knowledgeLibApi';

let formData = reactive({
  id: '',
  appName: '',
  libId: '',
})
let rules = reactive({
  libId: [{ required: true, message: "请选择知识库", trigger: "blur" }],
})
let pageData = reactive({
  knowledgeLibs: [] as AnyObjsDefine
})
const currentLibName = ref('')
const availableLibCountDisplay = computed(() => pageData.knowledgeLibs.length.toLocaleString('zh-CN'))
const selectedLibName = computed(() => {
  const matched = pageData.knowledgeLibs.find((item: AnyObjDefine) => String(item.id) === String(formData.libId))
  return matched?.libName || '请选择知识库'
})
// props
const props = defineProps<{
  bindLibDialogVisible: boolean,
  idToBindLib: string
}>()

let emitter = defineEmits(["closeDialog", "bindSuccess"])

let bindForm = ref()
function onSubmit() {
  bindForm.value.validate((valid: boolean) => {
    if (!valid) return
    bindLibApi(formData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      bindForm.value.resetFields()
      emitter("bindSuccess")
    })
  })
}
// 取消对话框
function handleCancel() {
  bindForm.value?.resetFields()
  currentLibName.value = ''
  pageData.knowledgeLibs = []
  emitter("closeDialog")
}
function queryAppById() {
  queryAppByIdApi(props.idToBindLib)
    .then(result => {
      Object.assign(formData, result.data)
      currentLibName.value = result.data?.libName || ''
      listAvailable()
    })
}
// 列出知识库
function listAvailable() {
  listAvailableLibApi().then(result => {
    if (result.data) {
      pageData.knowledgeLibs = result.data
      if (!formData.libId && result.data.length) {
        formData.libId = result.data[0].id
      }
    }
  })
}
// 打开对话框回调
function handleOpen() {
  queryAppById()
}
</script>
<style scoped></style>
