package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.res.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibAddReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRecommendReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentRenameReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibExperimentSaveReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibRecallDebugReq;
import com.osh.ai.assistant.consumer.bean.req.knowledgelib.KnowledgeLibUpdateReq;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibExperimentVO;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibRecallDebugVO;
import com.osh.ai.assistant.consumer.bean.vo.KnowledgeLibVO;
import com.osh.ai.assistant.consumer.service.KnowledgeLibDebugService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibExperimentService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;

/**
 * 知识库Controller层
 *

 */
@RestController
@RequestMapping("/knowledgeLib")
@RequiredArgsConstructor
@Slf4j
@Validated
public class KnowledgeLibController {

    private final KnowledgeLibService knowledgeLibService;
    private final KnowledgeLibDebugService knowledgeLibDebugService;
    private final KnowledgeLibExperimentService knowledgeLibExperimentService;

    /**
     * 知识库新增
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody @Validated KnowledgeLibAddReq addReq) {
        knowledgeLibService.add(addReq);
        return Result.buildSuccessMsg("添加成功");
    }

    /**
     * 知识库分页查询
     */
    @PostMapping("/queryPage")
    public Result<List<KnowledgeLibVO>> queryPage(@RequestBody KnowledgeLibPageReq pageReq) {
        return knowledgeLibService.queryPage(pageReq);
    }

    /**
     * 按id删除知识库
     */
    @GetMapping("/deleteById")
    public Result<Void> deleteById(@RequestParam("id") Long id) {
        knowledgeLibService.deleteById(id);
        return Result.buildSuccessMsg("删除成功");
    }

    /**
     * 按主键id查询知识库
     */
    @GetMapping("/queryById")
    public Result<KnowledgeLibVO> queryById(@RequestParam("id") Long id) {
        return Result.buildSuccess(knowledgeLibService.queryById(id));
    }

    /**
     * 按主键id修改知识库
     */
    @PostMapping("/modifyById")
    public Result<Void> modifyById(@RequestBody @Validated KnowledgeLibUpdateReq updateReq) {
        knowledgeLibService.modifyById(updateReq);
        return Result.buildSuccessMsg("更新成功");
    }

    /**
     * 列出当前用户可用的知识库
     */
    @GetMapping("/listAvailableLib")
    public Result<List<KnowledgeLibVO>> listAvailableLib(@RequestParam(value = "appId", required = false) Long appId) {
        return Result.buildSuccess(knowledgeLibService.listAvailableLib(appId));
    }

    /**
     * 调试知识库召回结果
     */
    @PostMapping("/debugRecall")
    public Result<KnowledgeLibRecallDebugVO> debugRecall(@RequestBody @Validated KnowledgeLibRecallDebugReq req) {
        return Result.buildSuccess(knowledgeLibDebugService.debugRecall(req));
    }

    /**
     * 保存实验版本
     */
    @PostMapping("/experiment/save")
    public Result<Void> saveExperiment(@RequestBody @Validated KnowledgeLibExperimentSaveReq req) {
        knowledgeLibExperimentService.save(req);
        return Result.buildSuccessMsg("实验版本保存成功");
    }

    /**
     * 列出知识库实验版本
     */
    @GetMapping("/experiment/list")
    public Result<List<KnowledgeLibExperimentVO>> listExperiment(@RequestParam("libId") Long libId) {
        return Result.buildSuccess(knowledgeLibExperimentService.listByLibId(libId));
    }

    /**
     * 重命名实验版本
     */
    @PostMapping("/experiment/rename")
    public Result<Void> renameExperiment(@RequestBody @Validated KnowledgeLibExperimentRenameReq req) {
        knowledgeLibExperimentService.rename(req);
        return Result.buildSuccessMsg("实验版本名称更新成功");
    }

    /**
     * 标记推荐版本
     */
    @PostMapping("/experiment/recommend")
    public Result<Void> recommendExperiment(@RequestBody @Validated KnowledgeLibExperimentRecommendReq req) {
        knowledgeLibExperimentService.markRecommended(req);
        return Result.buildSuccessMsg("已标记推荐版本");
    }

    /**
     * 删除实验版本
     */
    @GetMapping("/experiment/deleteById")
    public Result<Void> deleteExperiment(@RequestParam("id") Long id) {
        knowledgeLibExperimentService.deleteById(id);
        return Result.buildSuccessMsg("实验版本删除成功");
    }
}
