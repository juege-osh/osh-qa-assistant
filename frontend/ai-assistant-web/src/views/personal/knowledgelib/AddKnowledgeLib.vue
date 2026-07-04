<template>
  <div>
    <el-dialog
      class="workspace-form-dialog"
      title="新增知识库"
      :model-value="addDialogVisible"
      :before-close="handleCancel"
      width="640px"
    >
      <div class="dialog-intro">
        先建立知识库，再逐步上传资料、调试召回并绑定到应用。
      </div>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-info-grid workspace-info-grid--compact">
          <div class="workspace-info-item">
            <div class="workspace-info-label">创建后可以做什么</div>
            <div class="workspace-info-value">上传文档、调试召回、绑定应用</div>
          </div>
          <div class="workspace-info-item">
            <div class="workspace-info-label">建议先准备</div>
            <div class="workspace-info-value">清晰的名称、范围描述和识别图标</div>
          </div>
        </div>
      </section>
      <section class="workspace-dialog-tip-panel">
        新建知识库时，建议先把范围定义清楚，再开始上传资料。资料越集中、边界越清楚，后续绑定应用和检索调试时越容易定位问题。
      </section>
      <section class="workspace-info-card workspace-dialog-summary-card">
        <div class="workspace-tip-grid">
          <article v-for="item in createKnowledgeFocusCards" :key="item.title" class="workspace-tip-card">
            <div class="workspace-tip-card__title">{{ item.title }}</div>
            <div class="workspace-tip-card__desc">{{ item.desc }}</div>
          </article>
        </div>
      </section>
      <el-form ref="addForm" :model="formData" :rules="rules" label-position="top">
        <div class="workspace-form-grid workspace-form-grid--single">
          <el-form-item label="知识库名称" prop="libName">
            <el-input v-model="formData.libName" maxlength="40" show-word-limit placeholder="例如：产品知识库"></el-input>
            <div class="field-help">建议直接体现资料范围或使用场景，后续绑定应用时更容易识别。</div>
          </el-form-item>
          <el-form-item label="知识库描述" prop="libDesc">
            <el-input :rows="5" type="textarea" resize="none" v-model="formData.libDesc" placeholder="说明这个知识库覆盖哪些资料、给谁使用。"></el-input>
            <div class="field-help">建议说明内容边界、典型问题和适用对象，方便后续调试和协作。</div>
          </el-form-item>
          <el-form-item label="知识库图标" prop="iconPath" class="upload-card-field">
            <div class="workspace-upload-panel">
              <FileUpload @file-list-change="handleFileListChange" ref="fileUploadRef"></FileUpload>
              <div class="field-help">建议使用清晰的方形图标，方便在应用绑定和列表页识别。</div>
            </div>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="workspace-dialog-footer">
          <el-button class="workspace-btn workspace-btn--ghost" @click="handleCancel()">取消</el-button>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="onSubmit()">创建知识库</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name='AddKnowledgeLib' lang='ts'>
import { ref, reactive } from 'vue'
import { addKnowledgeLibApi } from '@/api/workspace/knowledgeLibApi';
import { ElMessage } from 'element-plus';
import FileUpload from '@/components/FileUpload.vue';
import type { AnyObjsDefine } from '@/types/common';
let formData = reactive({
  libName: '',
  libDesc: '',
  iconPath: '',
})
const createKnowledgeFocusCards = [
  {
    title: '优先定义知识范围',
    desc: '先明确这套知识库服务谁、覆盖什么资料，再决定上传哪些内容，后续更容易做边界控制。'
  },
  {
    title: '名称建议直接体现资料主题',
    desc: '比起抽象命名，能直接说明产品、流程或制度范围的名称，更方便后续绑定应用时快速识别。'
  },
  {
    title: '创建后先上传核心资料',
    desc: '不需要一开始就追求资料特别全，先让最关键的问题有依据可查，后续再逐步补充更稳妥。'
  }
] as const
let rules = reactive({
  libName: [{ required: true, message: "请输入知识库名称", trigger: "blur" }],
  libDesc: [{ required: true, message: "请输入知识库描述", trigger: "blur" }],
})
defineProps<{ addDialogVisible: boolean }>()

let emitter = defineEmits(["closeDialog", "addSuccess"])

let addForm = ref()
let fileUploadRef = ref()
function onSubmit() {
  addForm.value.validate((valid: boolean) => {
    if (!valid) return
    addKnowledgeLibApi(formData).then(result => {
      ElMessage({ "message": result.msg, "type": "success" })
      fileUploadRef.value.doClearFileList()
      addForm.value.resetFields()
      emitter("addSuccess")
    })
  })
}
// 取消对话框
function handleCancel() {
  fileUploadRef.value.doClearFileList()
  addForm.value.resetFields()
  emitter("closeDialog")
}
// 处理文件列表变更
function handleFileListChange(uploadResultList: AnyObjsDefine) {
  if (uploadResultList && uploadResultList.length > 0) {
    formData.iconPath = uploadResultList[0].relativePath
  } else {
    formData.iconPath = ''
  }
  addForm.value.validateField('iconPath');
}
</script>
<style scoped></style>
