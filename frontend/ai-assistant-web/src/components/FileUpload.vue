<template>
  <div :class="rootClass">
    <div v-if="showUploadMeta" class="workspace-file-upload__meta">
      <div class="workspace-file-upload__meta-copy">
        <div class="workspace-file-upload__meta-title">{{ uploadMetaTitle }}</div>
        <div class="workspace-file-upload__meta-desc">{{ uploadMetaDesc }}</div>
      </div>
      <div class="workspace-file-upload__meta-tags">
        <span class="workspace-file-upload__meta-tag">{{ uploadModeLabel }}</span>
        <span class="workspace-file-upload__meta-tag workspace-file-upload__meta-tag--soft">已选 {{ uploadData.files.length }}/{{ props.limit }}</span>
      </div>
    </div>
    <el-upload
      :class="['workspace-file-upload__upload', { hidden: triggerHidden }]"
      :auto-upload="true" :show-file-list="true" :list-type="listType"
      :multiple="multiple"
      :drag="drag"
      :limit="limit" @exceed="handleExceed" v-model:file-list="uploadData.files"
      :http-request="handleHttpRequest" ref="elUploadRef">
      <!-- 插槽的透传实现 -->
      <template v-slot:trigger>
        <!-- 外部若传此插槽则用外部的,默认为Plus-->
        <slot name="trigger">
          <el-icon>
            <Plus />
          </el-icon>
        </slot>
      </template>
      <template v-slot:default>
        <slot name="default">
          <div v-if="drag" class="workspace-file-upload__drop-copy">
            <el-icon class="workspace-file-upload__drop-icon"><UploadFilled /></el-icon>
            <div class="workspace-file-upload__drop-title">拖拽文件到这里，或点击上传</div>
            <div class="workspace-file-upload__drop-desc">支持常用文档格式，上传后会自动进入当前流程。</div>
          </div>
        </slot>
      </template>
    </el-upload>
  </div>
</template>
<script setup name='FileUpload' lang='ts'>
import { BASE_RESOURCES_URL } from "@/config/constants";
import { reactive, watch, ref, computed } from "vue"
import { ElMessage } from "element-plus";
import type { UploadUserFile, UploadRequestOptions, UploadRawFile } from "element-plus";
import { uploadFileApi} from '@/api/workspace/storageApi'
import type { AnyObjsDefine, ResultDefine } from "@/types/common";
import { Plus, UploadFilled } from "@element-plus/icons-vue";

let elUploadRef = ref()
// 是否显示trigger区域
let triggerHidden = ref(false)
let uploadData = reactive<{
  files: UploadUserFile[]
}>({ files: [] })

const props = withDefaults(defineProps<{
  limit?: number,
  // 用于更新时回显
  fileList?: UploadUserFile[],
  // picture-card,picture,text
  listType?: string,
  multiple?: boolean,
  drag?: boolean,
  // 上传到服务器后的文件所在目录,如:avatar
  module?: string,
}>(), {
  // 指定limit的默认值
  limit: () => 1,
  fileList: () => [],
  listType: () => "picture-card",
  multiple: () => false,
  drag: () => false,
  module: () => "",
})


let emitter = defineEmits<{
  // 指定事件的参数类型
  fileListChange: [uploadResultList: AnyObjsDefine]
}>()

const rootClass = computed(() => [
  'workspace-file-upload',
  props.drag ? 'workspace-file-upload--drag' : '',
  props.listType === 'picture-card' ? 'workspace-file-upload--picture-card' : '',
  props.listType === 'picture' ? 'workspace-file-upload--picture' : '',
  props.listType === 'text' ? 'workspace-file-upload--text' : '',
  props.multiple ? 'workspace-file-upload--multiple' : '',
])

const showUploadMeta = computed(() => props.drag || props.listType === 'text' || props.multiple)

const uploadModeLabel = computed(() => {
  if (props.drag) {
    return '拖拽上传'
  }
  if (props.listType === 'text') {
    return '文档列表'
  }
  if (props.listType === 'picture') {
    return '图片列表'
  }
  return '文件上传'
})

const uploadMetaTitle = computed(() => {
  if (props.drag) {
    return '资料上传区'
  }
  if (props.listType === 'text') {
    return '文件清单'
  }
  return '上传文件'
})

const uploadMetaDesc = computed(() => {
  if (props.drag) {
    return '保持资料命名清晰，上传后会直接进入当前处理流程。'
  }
  if (props.listType === 'text') {
    return '适合集中补充文档、清单或其他资料文件。'
  }
  return '上传后会自动同步到当前表单。'
})

// 监听props.fileList的变化
watch(() => props.fileList, newValue => {
  uploadData.files = newValue
}, {
  immediate: true,
  deep: true
})

// 监听uploadData.files的变化,变化是异步的
watch(() => uploadData.files, newValue => {
  emitter("fileListChange", extractUploadResult())
  if(uploadData.files.length >= props.limit) {
    // 隐藏trigger区域
    triggerHidden.value = true
  }else {
    triggerHidden.value = false
  }
}, {
  deep: true
})

/**
 * 清空显示的文件列表
 */
function doClearFileList() {
  uploadData.files = []
}

/**
 * @param files 超出的文件列表,每个file元素都是js原生File对象,举例如:
 * {"uid": 1719572224307,lastModified:1719458418674,name: "2.png"
 * ,size:257269,type: "image/png"}
 * @param fileList 已上传的文件列表
 */
function handleExceed(files: File[], fileList: UploadUserFile[]) {
  ElMessage({ message: `数量超出限制,只能上传${props.limit}个`, type: "error" });
}

/**
 * 最新的已上传文件的服务端响应结果
 * [{relativePath:"resources/xxx",originalFilename:"xxx",size:123}]
 */
function extractUploadResult() {
  let uploadResults:object[] = []
  for (let index = 0; index < uploadData.files.length; index++) {
    const obj = uploadData.files[index];
    if (obj.response) {// 新增或修改新上传的文件时
        // 上传成功后后端返回的结果data
        uploadResults.push((obj.response as ResultDefine).data)
    }else { // multiple=true时其他尚未上传成功的文件也会走到这里
      if(props.fileList && props.fileList.length > 0) { // 只有更新时再处理
        // 回显时自己设置的只有{url:"http://localhost:9000/zzz",name:"xxx",size:123},这里返回zzz
        let newObj = { ...obj } as UploadUserFile & { relativePath?: string }
        newObj.url = (newObj.url || "").replace(BASE_RESOURCES_URL, "")
        newObj.relativePath = newObj.url
        uploadResults.push(newObj)
      }
    }
  }
  return uploadResults
}
/**
 * 如果有多个文件需要上传,则调用多次这个方法
 * @param options 举例如:
 * {
 *  "headers": {
 *    "Authorization": "xxx"
 *   },
 *  "withCredentials": false,
 *  "file": js原生File对象,
 *  "data": {},
 *  "method": "post",
 *  "filename": "file",
 *  "action": "http://localhost:9000/storage/uploadFile"
 * }
 */
function handleHttpRequest(options: UploadRequestOptions) {
  let file = options.file as UploadRawFile
  const formData = new FormData();
  formData.append("file", file);
  formData.append("module", props.module);
  uploadFileApi(formData).then((res) => {
    processResultDefine(res,file)
  })
}
// 处理服务端返回的上传结果
function processResultDefine(resultDefine: ResultDefine,file:UploadRawFile) {
  for (let index = 0; index < uploadData.files.length; index++) {
    const element = uploadData.files[index];
    // 回显的没有raw
    if (element.raw && element.raw.uid === file.uid) {
      element.response = resultDefine
    }
  }
}
// 暴露给父组件使用
defineExpose({ doClearFileList })
</script>
<style scoped>
.workspace-file-upload {
  width: 100%;
}

.workspace-file-upload__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(223, 230, 241, 0.94);
  border-radius: 18px;
  background:
    radial-gradient(circle at top left, rgba(99, 91, 255, 0.05), transparent 42%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(246, 249, 255, 0.94));
}

.workspace-file-upload__meta-copy {
  min-width: 0;
}

.workspace-file-upload__meta-title {
  color: var(--space-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.workspace-file-upload__meta-desc {
  margin-top: 4px;
  color: var(--space-text-soft);
  font-size: 12px;
  line-height: 1.65;
}

.workspace-file-upload__meta-tags {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.workspace-file-upload__meta-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(99, 91, 255, 0.14);
  border-radius: 999px;
  background: rgba(239, 241, 255, 0.92);
  color: var(--space-primary);
  font-size: 12px;
  font-weight: 600;
}

.workspace-file-upload__meta-tag--soft {
  border-color: rgba(223, 230, 241, 0.96);
  background: rgba(255, 255, 255, 0.92);
  color: var(--space-text-soft);
}

.workspace-file-upload__upload {
  width: 100%;
}

.workspace-file-upload__drop-copy {
  display: grid;
  justify-items: center;
  gap: 10px;
  text-align: center;
}

.workspace-file-upload__drop-icon {
  width: 26px;
  height: 26px;
  font-size: 26px;
  color: var(--space-primary);
}

.workspace-file-upload__drop-title {
  color: var(--space-text);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
}

.workspace-file-upload__drop-desc {
  color: var(--space-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.workspace-file-upload--drag :deep(.el-upload) {
  display: block;
  width: 100%;
}

.workspace-file-upload--drag :deep(.el-upload-dragger) {
  width: 100%;
  padding: 42px 28px 34px;
  border: 1px dashed rgba(206, 216, 231, 0.96) !important;
  border-radius: 22px !important;
  background:
    radial-gradient(circle at top, rgba(99, 91, 255, 0.06), transparent 44%),
    linear-gradient(180deg, rgba(251, 252, 255, 0.98), rgba(246, 249, 255, 0.94)) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 10px 22px rgba(37, 48, 71, 0.04);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.workspace-file-upload--drag :deep(.el-upload-dragger:hover) {
  border-color: rgba(99, 91, 255, 0.28) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 14px 28px rgba(99, 91, 255, 0.08);
  transform: translateY(-1px);
}

.workspace-file-upload--drag :deep(.el-upload-list) {
  margin-top: 16px;
}

.workspace-file-upload--drag :deep(.el-upload-list__item) {
  margin-top: 0;
  border: 1px solid rgba(216, 225, 238, 0.92);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
}

.workspace-file-upload--picture-card :deep(.el-upload--picture-card) {
  width: 132px;
  height: 132px;
  border: 1px dashed rgba(205, 216, 232, 0.96) !important;
  border-radius: 20px !important;
  background:
    radial-gradient(circle at top, rgba(99, 91, 255, 0.08), transparent 48%),
    linear-gradient(180deg, rgba(250, 252, 255, 0.98), rgba(245, 249, 255, 0.94)) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.88),
    0 10px 22px rgba(37, 48, 71, 0.04);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.workspace-file-upload--picture-card :deep(.el-upload--picture-card:hover) {
  border-color: rgba(99, 91, 255, 0.28) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 14px 28px rgba(99, 91, 255, 0.08);
  transform: translateY(-1px);
}

.workspace-file-upload--picture-card :deep(.el-upload--picture-card .el-icon) {
  font-size: 28px;
  color: var(--space-primary);
}

.workspace-file-upload--picture-card :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 0;
}

.workspace-file-upload--picture-card :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 132px;
  height: 132px;
  margin: 0;
  border: 1px solid rgba(215, 226, 240, 0.96) !important;
  border-radius: 20px !important;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.95)) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 10px 22px rgba(37, 48, 71, 0.05);
  overflow: hidden;
}

.workspace-file-upload--text :deep(.el-upload-list),
.workspace-file-upload--picture :deep(.el-upload-list) {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid rgba(223, 230, 241, 0.94);
  border-radius: 18px;
  background: rgba(248, 251, 255, 0.92);
}

.workspace-file-upload--text :deep(.el-upload-list__item),
.workspace-file-upload--picture :deep(.el-upload-list__item) {
  margin-top: 0;
  padding: 10px 12px;
  border: 1px solid rgba(216, 225, 238, 0.9);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
}

.workspace-file-upload--text :deep(.el-upload-list__item:hover),
.workspace-file-upload--picture :deep(.el-upload-list__item:hover),
.workspace-file-upload--drag :deep(.el-upload-list__item:hover) {
  border-color: rgba(99, 91, 255, 0.22);
  background: rgba(251, 252, 255, 0.98);
}

.workspace-file-upload--text :deep(.el-upload-list__item-name),
.workspace-file-upload--picture :deep(.el-upload-list__item-name),
.workspace-file-upload--drag :deep(.el-upload-list__item-name) {
  color: var(--space-text-soft);
  font-weight: 600;
}

.workspace-file-upload--text :deep(.el-upload-list__item .el-icon),
.workspace-file-upload--picture :deep(.el-upload-list__item .el-icon),
.workspace-file-upload--drag :deep(.el-upload-list__item .el-icon) {
  color: var(--space-primary);
}

/* 超出limit后因此trigger区域 */
.hidden :deep(.el-upload--picture-card) {
  display: none !important;
}
.hidden :deep(.el-upload--picture) {
  display: none !important;
}
.hidden :deep(.el-upload--text) {
  display: none !important;
}
.hidden :deep(.el-upload-dragger) {
  display: none !important;
}

@media (max-width: 720px) {
  .workspace-file-upload__meta {
    flex-direction: column;
  }

  .workspace-file-upload__meta-tags {
    justify-content: flex-start;
  }
}
</style>
