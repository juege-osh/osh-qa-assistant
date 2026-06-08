import {reactive} from "vue"
import {ElMessage, ElMessageBox} from 'element-plus'
import type { AnyObjDefine, AnyObjsDefine, UseTableOptDefine} from "@/types/common.d.ts"

/**
 *
 * @param opts
 */
export function useTable(opts:UseTableOptDefine) {
    const {searchFormData = {}, loadTableApi, deleteByIdApi} = opts
    let searchData = reactive<AnyObjDefine>(Object.assign({
      pageNow: 1,
      pageSize: 50
    }, searchFormData))
    let tableData = reactive({
        rows: [] as AnyObjsDefine,
        total: 0
    })

    // 加载表格
    function loadTable() {
        loadTableApi && loadTableApi(searchData)
            .then(result => {
                tableData.rows = result.data
                // 后端做了long类型序列化为字符串,此处需要转为数字
                tableData.total = Number.parseInt(result.total)
            })
    }

    // 当前要变成x条/页,则pageSize值就是x
    function handlePageSizeChange(pageSize:number) {
        searchData.pageSize = pageSize
        loadTable()
    }

    // 当前要前往第x页,则此处的pageNow值就是x
    function handlePageNowChange(pageNow:number) {
        searchData.pageNow = pageNow
        loadTable()
    }

    // 按id删除
    function deleteById(id:string) {
        ElMessageBox.confirm('确认要删除吗?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => {
            deleteByIdApi && deleteByIdApi(id)
                .then(result => {
                    ElMessage({
                        type: "success",
                        message: result.msg
                    })
                    loadTable()
                })
        })
    }

    return {
        searchData,
        tableData,
        loadTable,
        handlePageSizeChange,
        handlePageNowChange,
        deleteById
    }
}
