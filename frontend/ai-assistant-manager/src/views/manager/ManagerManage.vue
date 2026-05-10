<template>
    <div class="page-shell">
        <section class="hero-panel">
            <div class="hero-title">管理员席位管理</div>
            <div class="hero-subtitle">
                管理员列表决定了谁可以进入控制台进行平台运维。这里可以集中查看账号、性别、头像、最近登录时间，并新增或移除管理员。
            </div>
            <div class="hero-meta">
                <span class="hero-badge">管理员总数：{{ tableData.total || 0 }}</span>
                <span class="hero-badge">支持多条件筛选</span>
                <span class="hero-badge">可直接新增管理员</span>
            </div>
        </section>

        <section class="toolbar-panel glass-panel">
            <div class="toolbar-copy">
                <div class="toolbar-title">管理员列表</div>
                <div class="toolbar-desc">
                    建议定期审查最近登录时间和管理员数量，避免遗留不用的后台账号继续保留访问权限。
                </div>
            </div>
            <div class="toolbar-actions">
                <el-form :model="searchData" :inline="true">
                    <el-form-item>
                        <el-input type="text" placeholder="用户名" v-model="searchData.username" clearable
                            style="width: 150px"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-input type="text" placeholder="姓名" v-model="searchData.realName" clearable
                            style="width: 150px"></el-input>
                    </el-form-item>
                    <el-form-item label="性别:">
                        <el-select v-model="searchData.sex" style="width: 150px">
                            <el-option v-for="one in formCfg.sexList" :key="one.sex" :label="one.sexName"
                                :value="one.sex"></el-option>
                        </el-select>
                    </el-form-item>
                    <el-form-item>
                        <el-button @click="loadTable" type="primary">查询</el-button>
                    </el-form-item>
                    <el-form-item>
                        <el-button @click="addDialogVisible = true" type="success" :icon="Plus">
                            新增管理员
                        </el-button>
                    </el-form-item>
                </el-form>
            </div>
        </section>

        <!-- 表格   -->
        <section class="glass-panel table-panel">
            <el-table :data="tableData.rows" stripe :border="true" style="width: 100%">
                <el-table-column prop="id" label="系统编号">
                </el-table-column>
                <el-table-column prop="username" label="用户名">
                </el-table-column>
                <el-table-column prop="realName" label="姓名">
                </el-table-column>
                <el-table-column prop="sexDesc" label="性别">
                </el-table-column>
                <el-table-column prop="lastLoginTime" label="上次登录时间" width="150">
                </el-table-column>
                <el-table-column prop="avatarPath" label="头像">
                    <!-- preview-teleported避免预览的图片被表格遮挡 -->
                    <template v-slot:default="scope">
                        <el-image v-if="scope.row.avatarPath" style="width: 50px;height: 50px;"
                            :src="scope.row.avatarPath"
                            :preview-src-list="[scope.row.avatarPath]" :preview-teleported="true"
                            fit="fill"></el-image>
                    </template>
                </el-table-column>
                <el-table-column label="操作">
                    <template v-slot:default="scope">
                        <el-button type="danger" :icon="Delete" @click="deleteById(scope.row.id)">删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </section>
        <!-- 分页   -->
        <section class="mt-dot5 glass-panel pagination-panel">
            <el-pagination @size-change="handlePageSizeChange" @current-change="handlePageNowChange"
                :current-page="searchData.pageNow" :page-sizes="[10, 30, 50]" :page-size="searchData.pageSize"
                layout="total, sizes, prev, pager, next, jumper" :total="tableData.total">
            </el-pagination>
        </section>
        <!-- 新增对话框 -->
        <AddManager :addDialogVisible="addDialogVisible" @closeDialog="addDialogVisible = false"
            @addSuccess="handleAddSuccess">
        </AddManager>
    </div>
</template>
<script setup name='ManagerManage' lang='ts'>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue';
import AddManager from '@/views/manager/AddManager.vue';
import { useTable } from '@/hooks/useTable';
import { pageManagerApi, deleteManagerByIdApi } from '@/api/managerApi';

// 添加对话框是否显示
let addDialogVisible = ref(false)
let searchFormData = reactive({
    username: '',
    realName: '',
    sex: '',
})

let formCfg = reactive({
    sexList: [
        { sex: '', sexName: "全部" },
        { sex: '1', sexName: "男" },
        { sex: '0', sexName: "女" },
    ]
})

let {
    searchData,
    tableData,
    loadTable,
    deleteById,
    handlePageSizeChange,
    handlePageNowChange,
} = useTable({ searchFormData, loadTableApi: pageManagerApi, deleteByIdApi: deleteManagerByIdApi })

function handleAddSuccess() {
    addDialogVisible.value = false
    loadTable()
}

onMounted(() => { loadTable() })
</script>
<style scoped>
.table-panel,
.pagination-panel {
    padding: 18px;
}
</style>
