<template>
  <div>
    <el-upload
      :class="{hidden:triggerHidden}"
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
    </el-upload>
  </div>
</template>
<script setup name='FileUpload' lang='ts'>
import { BASE_RESOURCES_URL } from "@/config/constants";
import { reactive, watch, ref } from "vue"
import { ElMessage } from "element-plus";
import type { UploadUserFile, UploadRequestOptions, UploadRawFile } from "element-plus";
import { uploadFileApi} from '@/api/storageApi'
import type { AnyObjsDefine, ResultDefine } from "@/types/common";

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
</style>
