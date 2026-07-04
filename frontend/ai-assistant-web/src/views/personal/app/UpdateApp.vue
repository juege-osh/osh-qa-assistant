<template>
  <div>
    <el-dialog
      class="workspace-form-dialog app-form-dialog"
      title="更新应用"
      :model-value="updateDialogVisible"
      @open="handleOpen"
      width="680px"
      :before-close="handleCancel"
    >
      <div class="dialog-intro">
        修改后会直接影响这个应用的回答表现和公开展示。
      </div>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">应用 ID</div>
            <div class="workspace-info-value workspace-info-value--mono">{{ formData.id || props.idToUpdate || '--' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">当前模型策略</div>
            <div class="workspace-info-value">{{ currentModelStrategy }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">知识库外回答</div>
            <div class="workspace-info-value">{{ Number(formData.outLibEnable) === 1 ? '允许扩展回答' : '优先依据知识库' }}</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">图标状态</div>
            <div class="workspace-info-value">{{ formData.iconPath ? '已配置图标' : '暂未配置图标' }}</div>
          </div>
        </div>
      </section>
      <section class="workspace-dialog-tip-panel">
        更新应用时，建议一次只聚焦一类变化。比如先改描述和提示词，再去聊天页验证；如果同时改模型、提示词和知识库，更难判断效果变化来自哪里。
      </section>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-tip-grid">
          <article v-for="item in updateAppFocusCards" :key="item.title" class="workspace-tip-card">
            <div class="workspace-tip-card__title">{{ item.title }}</div>
            <div class="workspace-tip-card__desc">{{ item.desc }}</div>
          </article>
        </div>
      </section>
      <el-form
        ref="updateForm"
        :model="formData"
        :rules="rules"
        class="app-form"
        label-position="top"
      >
        <div class="workspace-form-grid workspace-form-grid--aside">
          <el-form-item label="应用名称" prop="appName" class="workspace-form-span-2">
            <el-input v-model="formData.appName" placeholder="例如：管道助手"></el-input>
          </el-form-item>
          <el-form-item label="应用图标" prop="iconPath" class="label-align-item app-upload-item">
            <div class="workspace-upload-panel">
              <FileUpload :file-list="uploadInfo.fileList" @file-list-change="handleFileListChange" ref="fileUploadRef">
              </FileUpload>
              <div class="field-help">建议使用清晰的方形图标。</div>
            </div>
          </el-form-item>
          <el-form-item label="应用描述" prop="appDesc">
            <el-input
              v-model="formData.appDesc"
              :rows="4"
              type="textarea"
              resize="none"
              placeholder="一句话说明这个应用帮谁解决什么问题。"
            ></el-input>
          </el-form-item>
          <el-form-item label="超出知识库范围是否回答" prop="outLibEnable" class="workspace-form-span-2">
            <el-radio-group v-model="formData.outLibEnable" class="workspace-radio-group">
              <el-radio :label="0" border>否，优先保证依据可靠</el-radio>
              <el-radio :label="1" border>是，可允许扩展回答</el-radio>
            </el-radio-group>
            <div class="field-help">知识问答场景通常选“否”。</div>
          </el-form-item>
          <el-form-item label="应用补充提示词" prop="customPrompt" class="workspace-form-span-2">
            <el-input
              v-model="formData.customPrompt"
              :rows="4"
              type="textarea"
              resize="none"
              placeholder="想固定回答语气或结构时再填，例如：先给结论，再列步骤。"
            ></el-input>
            <div class="field-help">留空就使用默认规则。</div>
          </el-form-item>
          <el-form-item label="聊天模型名称" prop="chatModel" class="workspace-form-span-2">
            <el-input
              v-model="formData.chatModel"
              placeholder="可选，例如：MiniMax-M2.7-highspeed。留空则走系统默认模型。"
            ></el-input>
            <div class="field-help">留空就使用默认模型。</div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit">保存更新</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='UpdateCategory' lang='ts'>
import { computed, reactive, ref } from "vue"
import { queryAppByIdApi, modifyAppByIdApi } from "@/api/workspace/appApi"
import { ElMessage } from "element-plus";
import type { UploadUserFile } from 'element-plus';
import type { AnyObjsDefine } from "@/types/common";
import FileUpload from '@/components/FileUpload.vue';
import { listAvailableLibApi } from "@/api/workspace/knowledgeLibApi";
import { useResource } from "@/hooks/useResource";
let formData = reactive({
  id: '',
  appName: '',
  iconPath: '',
  appDesc: '',
  outLibEnable: 0,
  customPrompt: '',
  chatModel: ''
})
let rules = reactive({
  appName: [{ required: true, message: "请输入应用名称", trigger: "blur" }],
  appDesc: [{ required: true, message: "请输入应用描述", trigger: "blur" }],
  libId: [{ required: true, message: "请选择知识库", trigger: "blur" }],
  outLibEnable: [{ required: true, message: "请选择是否回答", trigger: "blur" }],
})
let pageData = reactive({
  knowledgeLibs: [] as AnyObjsDefine
})
const currentModelStrategy = computed(() => String(formData.chatModel || '').trim() ? '已指定专用模型' : '使用系统默认模型')
const updateAppFocusCards = computed(() => [
  {
    title: '建议先改最核心的一项配置',
    desc: '如果同时调整模型、提示词和对外回答策略，后续很难快速判断效果变化到底来自哪一部分。'
  },
  {
    title: Number(formData.outLibEnable) === 1 ? '当前允许知识库外扩展回答' : '当前仍然优先依据知识库回答',
    desc: Number(formData.outLibEnable) === 1
      ? '更适合对开放问题容忍度更高的场景，但也要继续观察回答边界是否会变松。'
      : '更适合强调依据可靠性的知识问答场景，后续可以继续关注命中和检索效果。'
  },
  {
    title: String(formData.chatModel || '').trim() ? '专用模型值得继续做效果对比' : '当前仍可先用默认模型验证链路',
    desc: String(formData.chatModel || '').trim()
      ? '更新后建议继续和默认模型对比回答速度、风格和稳定性，避免只凭主观感觉判断。'
      : '如果默认模型已经能满足当前场景，优先把更多精力放在知识组织和提示词结构上。'
  }
])
// 上传文件fileList
let uploadInfo = reactive<{
  fileList: UploadUserFile[]
}>({ fileList: [] })

let {toAddressable} = useResource()
let updateForm = ref()
// props
const props = defineProps<{
  updateDialogVisible: boolean,
  idToUpdate: string
}>()
let emitter = defineEmits(["updateSuccess", "closeDialog"])

// 方法
function onSubmit() {
  updateForm.value.validate((valid: boolean) => {
    if (!valid) return
    modifyAppByIdApi(formData).then(result => {
      ElMessage({ message: result.msg, type: "success" })
      uploadInfo.fileList = []
      updateForm.value.resetFields()
      emitter("updateSuccess")
    })
  })
}
//取消
function handleCancel() {
  uploadInfo.fileList = []
  updateForm.value.resetFields()
  emitter("closeDialog")
}
function queryAppById() {
  // 不然就累加了
  uploadInfo.fileList = []
  queryAppByIdApi(props.idToUpdate)
    .then(result => {
      Object.assign(formData, result.data)
      // 设置图片
      let addressablePath = toAddressable(result.data.iconPath)
      if (addressablePath) {
        uploadInfo.fileList.push({ url: addressablePath, name: '' })
      }
    })
}
// 处理文件列表变更
function handleFileListChange(uploadResultList:AnyObjsDefine) {
  if(uploadResultList && uploadResultList.length > 0) {
    formData.iconPath = uploadResultList[0].relativePath
  }else{
    formData.iconPath = ''
  }
  updateForm.value.validateField('iconPath');
}
// 列出知识库
function listAvailable() {
 listAvailableLibApi(props.idToUpdate).then(result => {
    if(result.data) {
      pageData.knowledgeLibs = result.data
    }
  })
}
// 打开对话框回调
function handleOpen() {
  queryAppById()
  listAvailable()
}
</script>
