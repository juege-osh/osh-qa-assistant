<template>
  <section class="toolbar-panel workspace-section-card workspace-toolbar-panel invoke-filter-toolbar">
    <div class="toolbar-copy workspace-toolbar-copy">
      <div class="workspace-toolbar-kicker">调用筛选</div>
      <div class="toolbar-title">筛选记录</div>
      <div class="toolbar-desc">
        先把应用、问题和时间范围缩小到你要看的样本，再切到对应工作区继续查看记录、做复盘或整理验收批次。
      </div>
    </div>
    <div class="toolbar-actions workspace-toolbar-actions">
      <el-form :model="model.searchData" :inline="true" class="workspace-toolbar-form">
        <el-form-item class="workspace-toolbar-field workspace-toolbar-field--md">
          <el-input v-model="model.searchData.appName" placeholder="按应用名称筛选" clearable />
        </el-form-item>
        <el-form-item class="workspace-toolbar-field workspace-toolbar-field--lg">
          <el-input v-model="model.searchData.userInputKeyword" placeholder="按问题关键词筛选" clearable />
        </el-form-item>
        <el-form-item label="状态" class="workspace-toolbar-field workspace-toolbar-field--sm">
          <el-select v-model="model.searchData.status">
            <el-option label="全部" value="" />
            <el-option label="失败" value="0" />
            <el-option label="成功" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item class="workspace-toolbar-field workspace-toolbar-field--date">
          <el-date-picker
            v-model="model.searchData.startTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始时间"
          />
        </el-form-item>
        <el-form-item class="workspace-toolbar-field workspace-toolbar-field--date">
          <el-date-picker
            v-model="model.searchData.endTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="workspace-btn workspace-btn--primary" @click="model.loadTable">查询</el-button>
        </el-form-item>
        <el-form-item>
          <el-button class="workspace-btn workspace-btn--ghost" @click="model.resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useInvokeRecordFeatureModel } from '../composables/useInvokeRecordFeature'

const model = useInvokeRecordFeatureModel()
</script>

<style scoped>
.invoke-filter-toolbar {
  border-radius: 20px;
}
</style>
