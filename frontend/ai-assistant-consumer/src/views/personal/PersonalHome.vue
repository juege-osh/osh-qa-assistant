<template>
  <div class="personal-home">
    <el-row :gutter="20" class="personal-row">
      <el-col :xl="5" :lg="6" :md="7" :sm="24" :xs="24">
        <side-bar></side-bar>
      </el-col>
      <el-col :xl="19" :lg="18" :md="17" :sm="24" :xs="24">
        <router-view></router-view>
      </el-col>
    </el-row>
  </div>
</template>
<script setup name='PersonalHome' lang='ts'>
import SideBar from '@/components/SideBar.vue';
</script>
<style scoped>
.personal-home,
.personal-row {
  height: 100%;
}

.personal-home {
  min-height: calc(100vh - 190px);
}

@media (max-width: 992px) {
  .personal-row {
    gap: 16px 0;
  }
}
</style>
