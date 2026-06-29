package com.osh.ai.assistant.consumer.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.osh.ai.assistant.common.bean.entity.AppDO;
import com.osh.ai.assistant.common.bean.entity.InvokeRecordDO;
import com.osh.ai.assistant.common.bean.entity.InvokeRecordDetailDO;
import com.osh.ai.assistant.common.bean.entity.KnowledgeLibDO;
import com.osh.ai.assistant.common.bean.entity.RagAcceptanceBatchDO;
import com.osh.ai.assistant.common.bean.entity.RagAcceptanceItemDO;
import com.osh.ai.assistant.common.bean.entity.UserDO;
import com.osh.ai.assistant.common.context.UserContext;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.ConvertUtil;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceBatchSaveReq;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceItemSaveReq;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceRunDefaultBatchReq;
import com.osh.ai.assistant.consumer.bean.dto.ChatDTO;
import com.osh.ai.assistant.consumer.bean.vo.RagAcceptanceBatchVO;
import com.osh.ai.assistant.consumer.bean.vo.RagAcceptanceItemVO;
import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;
import com.osh.ai.assistant.consumer.manager.InvokeManager;
import com.osh.ai.assistant.consumer.mapper.RagAcceptanceBatchMapper;
import com.osh.ai.assistant.consumer.mapper.RagAcceptanceItemMapper;
import com.osh.ai.assistant.consumer.service.AiChatService;
import com.osh.ai.assistant.consumer.service.AppService;
import com.osh.ai.assistant.consumer.service.InvokeRecordDetailService;
import com.osh.ai.assistant.consumer.service.InvokeRecordService;
import com.osh.ai.assistant.consumer.service.KnowledgeLibService;
import com.osh.ai.assistant.consumer.service.RagAcceptanceService;
import com.osh.ai.assistant.consumer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RagAcceptanceServiceImpl implements RagAcceptanceService {
    private final RagAcceptanceBatchMapper batchMapper;
    private final RagAcceptanceItemMapper itemMapper;
    private final AppService appService;
    private final KnowledgeLibService knowledgeLibService;
    private final UserService userService;
    private final InvokeManager invokeManager;
    private final AiChatService aiChatService;
    private final InvokeRecordService invokeRecordService;
    private final InvokeRecordDetailService invokeRecordDetailService;

    private static final List<DefaultQuestionTemplate> DEFAULT_QUESTION_TEMPLATES = List.of(
        new DefaultQuestionTemplate("TC-01", "标准知识问答", "这个知识库当前主要覆盖哪些主题？", "能说明当前知识库覆盖范围、主题边界或主要文档类型。"),
        new DefaultQuestionTemplate("TC-02", "标准知识问答", "某个制度或流程的核心结论是什么？", "能直接给结论，并指出依据或关键规则。"),
        new DefaultQuestionTemplate("TC-03", "任务指导", "我应该从哪里开始完成这个任务？", "给出开始入口、前置准备和建议顺序。"),
        new DefaultQuestionTemplate("TC-04", "任务指导", "如果我要把这个流程走通，先后步骤应该是什么？", "按顺序说明步骤，并提示关键注意事项。"),
        new DefaultQuestionTemplate("TC-05", "模糊提问", "这个事情我该怎么弄？", "先识别问题模糊，再引导补充关键信息或给出安全的初始路径。"),
        new DefaultQuestionTemplate("TC-06", "模糊提问", "我现在不知道从哪开始，你建议我先看什么？", "给出最小起步建议和优先阅读内容。"),
        new DefaultQuestionTemplate("TC-07", "未命中知识", "请回答一个当前知识库里没有覆盖的外部问题。", "明确说明当前缺少依据，不要强答，并给出下一步建议。"),
        new DefaultQuestionTemplate("TC-08", "未命中知识", "我只说一个很模糊的事情，你直接告诉我答案。", "提醒问题信息不足，建议补充关键词或制度名称。"),
        new DefaultQuestionTemplate("TC-09", "失败场景", "如果当前没找到相关知识，你会怎么告诉我？", "反馈应说明未找到足够依据，并给出可理解的下一步。"),
        new DefaultQuestionTemplate("TC-10", "失败场景", "如果知识库检索暂时不可用，你会怎么提示我？", "反馈应避免报错堆栈，提示稍后重试或换更明确问法。")
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveBatch(RagAcceptanceBatchSaveReq req) {
        RagAcceptanceBatchDO entity;
        if (req.getId() == null) {
            entity = new RagAcceptanceBatchDO();
            entity.setUserId(UserContext.getUserId());
            fillBatchFields(entity, req);
            batchMapper.insert(entity);
        } else {
            entity = requireOwnedBatch(req.getId());
            fillBatchFields(entity, req);
            batchMapper.updateById(entity);
            LambdaUpdateWrapper<RagAcceptanceItemDO> deleteWrapper = new LambdaUpdateWrapper<>();
            deleteWrapper.eq(RagAcceptanceItemDO::getBatchId, entity.getId());
            itemMapper.delete(deleteWrapper);
        }

        for (RagAcceptanceItemSaveReq itemReq : req.getItems()) {
            RagAcceptanceItemDO item = ConvertUtil.convert(itemReq, RagAcceptanceItemDO.class);
            item.setId(null);
            item.setBatchId(entity.getId());
            itemMapper.insert(item);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long runDefaultBatch(RagAcceptanceRunDefaultBatchReq req) {
        AppDO app = appService.requireOwnedEntity(req.getAppId());
        appService.checkChatCondition(app.getId());
        KnowledgeLibDO knowledgeLib = app.getLibId() == null ? null : knowledgeLibService.requireOwnedEntity(app.getLibId());
        UserDO currentUser = userService.getById(UserContext.getUserId());
        if (currentUser == null) {
            throw new BizEx("当前用户不存在");
        }

        RagAcceptanceBatchSaveReq saveReq = new RagAcceptanceBatchSaveReq();
        saveReq.setAppId(app.getId());
        saveReq.setLibId(app.getLibId());
        saveReq.setBatchName(StrUtil.trim(req.getBatchName()));
        saveReq.setAppName(app.getAppName());
        saveReq.setSceneType(StrUtil.blankToDefault(StrUtil.trim(req.getSceneType()), "内部知识问答"));
        saveReq.setKnowledgeScope(knowledgeLib == null ? null : knowledgeLib.getLibName());
        saveReq.setReleaseVersion("默认问题集自动跑测");
        saveReq.setExperimentVersion(knowledgeLib == null ? null : knowledgeLib.getActiveExperimentName());
        saveReq.setActiveExperimentId(knowledgeLib == null ? null : knowledgeLib.getActiveExperimentId());
        saveReq.setActiveExperimentName(knowledgeLib == null ? null : knowledgeLib.getActiveExperimentName());
        saveReq.setActiveSplitStrategy(knowledgeLib == null ? null : knowledgeLib.getActiveSplitStrategy());
        saveReq.setVersionRemark(buildVersionRemark(app, knowledgeLib));
        saveReq.setQuickView("defaultQuestionPool");
        saveReq.setQuickViewDesc("系统直接批量执行默认问题集，生成真实回答、真实调用记录和正式验收批次。");
        saveReq.setTesterName(StrUtil.emptyToNull(StrUtil.trim(req.getTesterName())));
        saveReq.setTestDate(DateUtil.now());
        saveReq.setSummaryConclusion(StrUtil.emptyToNull(StrUtil.trim(req.getSummaryConclusion())));
        saveReq.setNextAction(StrUtil.emptyToNull(StrUtil.trim(req.getNextAction())));

        List<RagAcceptanceItemSaveReq> items = new ArrayList<>();
        for (DefaultQuestionTemplate template : DEFAULT_QUESTION_TEMPLATES) {
            ExecutionSnapshot snapshot = executeQuestion(app, currentUser, template);
            AcceptanceEvaluation evaluation = evaluateAcceptance(template, snapshot);
            RagAcceptanceItemSaveReq item = new RagAcceptanceItemSaveReq();
            item.setInvokeRecordId(snapshot.invokeRecordId());
            item.setInvokeRecordDetailId(snapshot.invokeRecordDetailId());
            item.setTestCaseNo(template.testCaseNo());
            item.setQuestionType(template.questionType());
            item.setUserQuestion(template.userQuestion());
            item.setExpectedKnowledge(template.expectedKnowledge());
            item.setActualAnswerSummary(summarizeAnswer(snapshot.answer()));
            item.setActualAnswer(snapshot.answer());
            item.setFailReason(snapshot.failReason());
            item.setGracefulFailureConclusion(snapshot.failReason() == null ? "" : "待确认");
            item.setInvokeStatus(snapshot.invokeStatus());
            item.setModelName(snapshot.modelName());
            item.setAppName(app.getAppName());
            item.setLibName(knowledgeLib == null ? "" : knowledgeLib.getLibName());
            item.setCostTime(snapshot.costTime());
            item.setCostToken(snapshot.costToken());
            item.setHitConclusion(evaluation.hitConclusion());
            item.setGroundedConclusion(evaluation.groundedConclusion());
            item.setReadableConclusion(evaluation.readableConclusion());
            item.setGracefulFailureConclusion(evaluation.gracefulFailureConclusion());
            item.setHitRateConclusion(evaluation.hitRateConclusion());
            item.setCompletenessConclusion(evaluation.completenessConclusion());
            item.setFollowUpCategory(evaluation.followUpCategory());
            item.setFollowUpAction(evaluation.followUpAction());
            item.setRemark(evaluation.remark());
            items.add(item);
        }
        saveReq.setItems(items);
        if (StrUtil.isBlank(saveReq.getSummaryConclusion())) {
            saveReq.setSummaryConclusion(buildSummaryConclusion(items));
        }
        if (StrUtil.isBlank(saveReq.getNextAction())) {
            saveReq.setNextAction(buildNextAction(items));
        }
        return saveBatch(saveReq);
    }

    @Override
    public List<RagAcceptanceBatchVO> listMine() {
        LambdaQueryWrapper<RagAcceptanceBatchDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(RagAcceptanceBatchDO::getUserId, UserContext.getUserId())
            .orderByDesc(RagAcceptanceBatchDO::getModifiedTime)
            .orderByDesc(RagAcceptanceBatchDO::getId);
        List<RagAcceptanceBatchDO> batches = batchMapper.selectList(lqw);
        List<RagAcceptanceBatchVO> result = new ArrayList<>();
        for (RagAcceptanceBatchDO batch : batches) {
            RagAcceptanceBatchVO vo = ConvertUtil.convert(batch, RagAcceptanceBatchVO.class);
            List<RagAcceptanceItemDO> items = listItems(batch.getId());
            vo.setItems(ConvertUtil.convert(items, RagAcceptanceItemVO.class));
            vo.setItemCount(items.size());
            vo.setPassCount((int) items.stream().filter(this::isPassItem).count());
            vo.setFollowUpCount((int) items.stream().filter(this::isFollowUpItem).count());
            result.add(vo);
        }
        return result;
    }

    @Override
    public RagAcceptanceBatchVO detail(Long id) {
        RagAcceptanceBatchDO batch = requireOwnedBatch(id);
        RagAcceptanceBatchVO vo = ConvertUtil.convert(batch, RagAcceptanceBatchVO.class);
        List<RagAcceptanceItemDO> items = listItems(batch.getId());
        vo.setItems(ConvertUtil.convert(items, RagAcceptanceItemVO.class));
        vo.setItemCount(items.size());
        vo.setPassCount((int) items.stream().filter(this::isPassItem).count());
        vo.setFollowUpCount((int) items.stream().filter(this::isFollowUpItem).count());
        return vo;
    }

    private void fillBatchFields(RagAcceptanceBatchDO entity, RagAcceptanceBatchSaveReq req) {
        entity.setAppId(req.getAppId());
        entity.setLibId(req.getLibId());
        entity.setBatchName(StrUtil.trim(req.getBatchName()));
        entity.setAppName(StrUtil.emptyToNull(StrUtil.trim(req.getAppName())));
        entity.setSceneType(StrUtil.emptyToNull(StrUtil.trim(req.getSceneType())));
        entity.setKnowledgeScope(StrUtil.emptyToNull(StrUtil.trim(req.getKnowledgeScope())));
        entity.setReleaseVersion(StrUtil.emptyToNull(StrUtil.trim(req.getReleaseVersion())));
        entity.setExperimentVersion(StrUtil.emptyToNull(StrUtil.trim(req.getExperimentVersion())));
        entity.setActiveExperimentId(req.getActiveExperimentId());
        entity.setActiveExperimentName(StrUtil.emptyToNull(StrUtil.trim(req.getActiveExperimentName())));
        entity.setActiveSplitStrategy(StrUtil.emptyToNull(StrUtil.trim(req.getActiveSplitStrategy())));
        entity.setVersionRemark(StrUtil.emptyToNull(StrUtil.trim(req.getVersionRemark())));
        entity.setQuickView(StrUtil.emptyToNull(StrUtil.trim(req.getQuickView())));
        entity.setQuickViewDesc(StrUtil.emptyToNull(StrUtil.trim(req.getQuickViewDesc())));
        entity.setTesterName(StrUtil.emptyToNull(StrUtil.trim(req.getTesterName())));
        entity.setSummaryConclusion(StrUtil.emptyToNull(StrUtil.trim(req.getSummaryConclusion())));
        entity.setNextAction(StrUtil.emptyToNull(StrUtil.trim(req.getNextAction())));
        entity.setTestDate(parseDate(req.getTestDate()));
    }

    private Date parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return DateUtil.parse(value).toJdkDate();
    }

    private RagAcceptanceBatchDO requireOwnedBatch(Long id) {
        RagAcceptanceBatchDO batch = batchMapper.selectById(id);
        if (batch == null || !Objects.equals(batch.getUserId(), UserContext.getUserId())) {
            throw new BizEx("验收批次不存在或无权限访问");
        }
        return batch;
    }

    private List<RagAcceptanceItemDO> listItems(Long batchId) {
        LambdaQueryWrapper<RagAcceptanceItemDO> lqw = new LambdaQueryWrapper<>();
        lqw.eq(RagAcceptanceItemDO::getBatchId, batchId)
            .orderByAsc(RagAcceptanceItemDO::getId);
        return itemMapper.selectList(lqw);
    }

    private boolean isPassItem(RagAcceptanceItemDO item) {
        return isPass(item.getHitConclusion())
            && isPass(item.getGroundedConclusion())
            && isPass(item.getReadableConclusion())
            && isPass(item.getGracefulFailureConclusion());
    }

    private boolean isFollowUpItem(RagAcceptanceItemDO item) {
        return StrUtil.isNotBlank(item.getFollowUpCategory()) || StrUtil.isNotBlank(item.getFollowUpAction());
    }

    private boolean isPass(String value) {
        return "通过".equals(StrUtil.trim(value));
    }

    private ExecutionSnapshot executeQuestion(AppDO app, UserDO currentUser, DefaultQuestionTemplate template) {
        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setUserInput(template.userQuestion());
        chatDTO.setConversationId("rag-acceptance-batch-" + IdWorker.getId());
        chatDTO.setAppKey(currentUser.getAppKey());
        InvokeRecordBuilder builder = invokeManager.initInvokeRecordBuild(chatDTO, currentUser, app);
        aiChatService.doChatSilently(app, builder);
        return loadExecutionSnapshot(builder.getId());
    }

    private ExecutionSnapshot loadExecutionSnapshot(Long invokeRecordId) {
        InvokeRecordDO record = invokeRecordService.getById(invokeRecordId);
        if (record == null) {
            throw new BizEx("默认问题集跑测失败，未生成调用记录");
        }
        List<InvokeRecordDetailDO> details = invokeRecordDetailService.selectByInvokeRecordId(invokeRecordId);
        if (details.isEmpty()) {
            throw new BizEx("默认问题集跑测失败，未生成调用明细");
        }
        InvokeRecordDetailDO detail = details.get(0);
        return new ExecutionSnapshot(
            invokeRecordId,
            detail.getId(),
            StrUtil.blankToDefault(detail.getAssistantMessage(), ""),
            StrUtil.emptyToNull(detail.getFailReason()),
            record.getStatus() == null ? "" : String.valueOf(record.getStatus()),
            StrUtil.blankToDefault(detail.getModelName(), ""),
            detail.getCostTime(),
            detail.getCostToken()
        );
    }

    private String summarizeAnswer(String answer) {
        String normalized = StrUtil.trim(StrUtil.blankToDefault(answer, ""));
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        normalized = normalized.replace("\r\n", " ").replace('\n', ' ');
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120);
    }

    private String buildVersionRemark(AppDO app, KnowledgeLibDO knowledgeLib) {
        if (knowledgeLib == null) {
            return "当前应用未绑定知识库，默认问题集自动跑测仅用于校验失败反馈与应用级提示词行为。";
        }
        return "自动跑测时使用应用【" + app.getAppName() + "】绑定知识库【" + knowledgeLib.getLibName()
            + "】的当前生效切分版本，实验版本=" + StrUtil.blankToDefault(knowledgeLib.getActiveExperimentName(), "未命名")
            + "，切分策略=" + StrUtil.blankToDefault(knowledgeLib.getActiveSplitStrategy(), "未设置");
    }

    private AcceptanceEvaluation evaluateAcceptance(DefaultQuestionTemplate template, ExecutionSnapshot snapshot) {
        String answer = normalize(snapshot.answer());
        boolean invokeSuccess = InvokeStatusEnum.SUCCESS.getCode().toString().equals(snapshot.invokeStatus());
        boolean saysNoEnoughEvidence = containsAny(answer, "没有足够依据", "暂时无法可靠回答", "资料不足以支撑", "暂无法提供可靠答案");
        boolean asksMoreDetail = containsAny(answer, "请补充", "具体", "制度名称", "流程名称", "任务名称");
        boolean saysRetryLater = containsAny(answer, "稍后重试", "更明确的问题", "换一个更具体", "联系知识库运营", "联系维护人");
        boolean containsStackWords = containsAny(answer, "exception", "sqlsyntaxerror", "stack", "trace", "nullpointer");

        String hitConclusion = evaluateHit(template, invokeSuccess, answer, asksMoreDetail, saysNoEnoughEvidence);
        String groundedConclusion = evaluateGrounded(invokeSuccess, answer, hitConclusion, saysNoEnoughEvidence);
        String readableConclusion = evaluateReadable(answer);
        String gracefulFailureConclusion = evaluateGraceful(template, invokeSuccess, answer, saysNoEnoughEvidence, saysRetryLater, containsStackWords);
        String hitRateConclusion = "通过".equals(hitConclusion) ? "通过" : "待确认";
        String completenessConclusion = evaluateCompleteness(template, answer, hitConclusion);
        String followUpCategory = resolveFollowUpCategory(template, invokeSuccess, hitConclusion, groundedConclusion, readableConclusion, gracefulFailureConclusion);
        String followUpAction = buildFollowUpAction(template, followUpCategory, invokeSuccess, answer);
        String remark = buildAcceptanceRemark(template, hitConclusion, groundedConclusion, gracefulFailureConclusion);
        return new AcceptanceEvaluation(
            hitConclusion,
            groundedConclusion,
            readableConclusion,
            gracefulFailureConclusion,
            hitRateConclusion,
            completenessConclusion,
            followUpCategory,
            followUpAction,
            remark
        );
    }

    private String evaluateHit(DefaultQuestionTemplate template, boolean invokeSuccess, String answer,
                               boolean asksMoreDetail, boolean saysNoEnoughEvidence) {
        if (!invokeSuccess || StrUtil.isBlank(answer)) {
            return "不通过";
        }
        return switch (template.testCaseNo()) {
            case "TC-01" -> hasCoverageTopics(answer) ? "通过" : "不通过";
            case "TC-02" -> asksMoreDetail || containsAny(answer, "核心结论", "关键规则", "直接结论") ? "通过" : "待确认";
            case "TC-03" -> containsCountAtLeast(answer, 2, "确认目标任务名称", "所属知识库", "生效切分版本", "发布策略", "第一步") ? "通过" : "不通过";
            case "TC-04" -> containsCountAtLeast(answer, 3, "准备文档", "上传到知识库", "发布为当前生效版本", "重建索引", "运行默认问题集") ? "通过" : "不通过";
            case "TC-05" -> asksMoreDetail && containsAny(answer, "模糊", "任务名称", "流程名称", "制度名称") ? "通过" : "不通过";
            case "TC-06" -> containsAny(answer, "先看", "优先阅读", "确认目标任务名称", "所属知识库", "生效版本", "最小起步") ? "通过" : "不通过";
            case "TC-07" -> saysNoEnoughEvidence && saysRetryOrEscalate(answer) ? "通过" : "不通过";
            case "TC-08" -> containsAny(answer, "信息不足", "补充") && containsAny(answer, "具体", "任务名称", "流程名称", "制度名称") ? "通过" : "不通过";
            case "TC-09" -> saysNoEnoughEvidence && saysRetryOrEscalate(answer) ? "通过" : "不通过";
            case "TC-10" -> containsAny(answer, "检索暂时不可用") && containsAny(answer, "稍后重试", "更明确的问题") ? "通过" : "不通过";
            default -> "待确认";
        };
    }

    private String evaluateGrounded(boolean invokeSuccess, String answer, String hitConclusion, boolean saysNoEnoughEvidence) {
        if (!invokeSuccess || StrUtil.isBlank(answer)) {
            return "不通过";
        }
        if (saysNoEnoughEvidence || containsAny(answer, "参考来源", "依据", "根据知识库", "上下文")) {
            return "通过";
        }
        if ("通过".equals(hitConclusion)) {
            return "通过";
        }
        return "待确认";
    }

    private String evaluateReadable(String answer) {
        if (StrUtil.isBlank(answer)) {
            return "不通过";
        }
        if (answer.length() > 1600) {
            return "待确认";
        }
        if (containsAny(answer, "\n", "1.", "2.", "建议", "请", "如下", "步骤")) {
            return "通过";
        }
        return answer.length() >= 20 ? "通过" : "待确认";
    }

    private String evaluateGraceful(DefaultQuestionTemplate template, boolean invokeSuccess, String answer,
                                    boolean saysNoEnoughEvidence, boolean saysRetryLater, boolean containsStackWords) {
        if (containsStackWords) {
            return "不通过";
        }
        if (!invokeSuccess) {
            return containsAny(answer, "稍后重试", "更明确的问题", "暂时不可用") ? "通过" : "待确认";
        }
        if ("失败场景".equals(template.questionType()) || "未命中知识".equals(template.questionType())) {
            return (saysNoEnoughEvidence || containsAny(answer, "检索暂时不可用")) && saysRetryLater ? "通过" : "不通过";
        }
        if (saysNoEnoughEvidence) {
            return saysRetryLater || containsAny(answer, "请补充", "具体") ? "通过" : "待确认";
        }
        return "通过";
    }

    private String evaluateCompleteness(DefaultQuestionTemplate template, String answer, String hitConclusion) {
        if (!"通过".equals(hitConclusion)) {
            return "待确认";
        }
        return switch (template.testCaseNo()) {
            case "TC-03", "TC-04" -> containsCountAtLeast(answer, 2, "1.", "2.", "步骤", "建议", "先", "然后", "最后") ? "通过" : "待确认";
            case "TC-07", "TC-09", "TC-10" -> containsAny(answer, "建议", "稍后重试", "联系") ? "通过" : "待确认";
            default -> answer.length() >= 30 ? "通过" : "待确认";
        };
    }

    private String resolveFollowUpCategory(DefaultQuestionTemplate template, boolean invokeSuccess, String hitConclusion,
                                           String groundedConclusion, String readableConclusion, String gracefulFailureConclusion) {
        if (!invokeSuccess) {
            return "observe";
        }
        if ("不通过".equals(groundedConclusion)) {
            return "knowledge";
        }
        if ("不通过".equals(gracefulFailureConclusion) || "不通过".equals(readableConclusion)) {
            return "prompt";
        }
        if ("不通过".equals(hitConclusion) && ("标准知识问答".equals(template.questionType()) || "任务指导".equals(template.questionType()))) {
            return "chunking";
        }
        return "";
    }

    private String buildFollowUpAction(DefaultQuestionTemplate template, String followUpCategory,
                                       boolean invokeSuccess, String answer) {
        if (StrUtil.isBlank(followUpCategory)) {
            return "";
        }
        if (!invokeSuccess) {
            return "检查调用记录、数据库/向量库链路和异常告警，先恢复稳定可用性。";
        }
        return switch (followUpCategory) {
            case "knowledge" -> "补充该问题对应的制度、流程或任务说明，确保知识库里有可直接支撑回答的原文依据。";
            case "chunking" -> "对照当前生效切分版本检查召回片段是否切散、断裂或遗漏，优先比较不同切分版本的命中差异。";
            case "prompt" -> "继续收紧提示词，让失败反馈更体面，或让任务指导类回答更直接、更像可执行步骤。";
            case "observe" -> "补齐运行链路观测，记录失败原因和恢复手段，避免再次只能看到数据库异常。";
            default -> "结合该条回答继续人工复盘。";
        };
    }

    private String buildAcceptanceRemark(DefaultQuestionTemplate template, String hitConclusion,
                                         String groundedConclusion, String gracefulFailureConclusion) {
        if ("不通过".equals(hitConclusion) && "标准知识问答".equals(template.questionType())) {
            return "当前回答没有直接命中问题本身，需要继续比较知识切分和召回效果。";
        }
        if ("不通过".equals(gracefulFailureConclusion)) {
            return "失败反馈还不够体面，需要继续优化提示词和用户可见文案。";
        }
        if ("待确认".equals(groundedConclusion)) {
            return "建议人工复核引用依据与回答结论是否完全对齐。";
        }
        return "";
    }

    private String buildSummaryConclusion(List<RagAcceptanceItemSaveReq> items) {
        long passCount = items.stream().filter(this::isPassSaveItem).count();
        long chunkingCount = items.stream().filter(item -> "chunking".equals(item.getFollowUpCategory())).count();
        long promptCount = items.stream().filter(item -> "prompt".equals(item.getFollowUpCategory())).count();
        long observeCount = items.stream().filter(item -> "observe".equals(item.getFollowUpCategory())).count();
        return "默认问题集自动跑测共 " + items.size() + " 条，当前全部通过 " + passCount + " 条；"
            + "主要待修复方向为切分=" + chunkingCount + "，提示词=" + promptCount + "，观测=" + observeCount + "。";
    }

    private String buildNextAction(List<RagAcceptanceItemSaveReq> items) {
        if (items.stream().anyMatch(item -> "chunking".equals(item.getFollowUpCategory()))) {
            return "优先比较不同切分版本在标准知识问答和任务指导场景下的命中差异，再决定当前生效版本。";
        }
        if (items.stream().anyMatch(item -> "prompt".equals(item.getFollowUpCategory()))) {
            return "继续优化提示词和失败反馈文案，再重跑默认问题集确认体验改善。";
        }
        if (items.stream().anyMatch(item -> "observe".equals(item.getFollowUpCategory()))) {
            return "补齐运行链路观测与故障恢复信息，再重新执行默认问题集跑测。";
        }
        return "继续扩充真实问题集，并基于正式批次结果持续复盘。";
    }

    private boolean hasCoverageTopics(String answer) {
        int topicHit = 0;
        if (containsAny(answer, "内部文档知识问答")) {
            topicHit++;
        }
        if (containsAny(answer, "训练测试任务")) {
            topicHit++;
        }
        if (containsAny(answer, "体面反馈", "失败时的体面反馈")) {
            topicHit++;
        }
        return containsAny(answer, "三个主题", "3个主题", "主要覆盖") || topicHit >= 2;
    }

    private boolean isPassSaveItem(RagAcceptanceItemSaveReq item) {
        return isPass(item.getHitConclusion())
            && isPass(item.getGroundedConclusion())
            && isPass(item.getReadableConclusion())
            && isPass(item.getGracefulFailureConclusion());
    }

    private boolean saysRetryOrEscalate(String answer) {
        return containsAny(answer, "更具体", "联系知识库运营", "联系维护人", "补充相关内容", "稍后重试");
    }

    private boolean containsCountAtLeast(String answer, int threshold, String... tokens) {
        int count = 0;
        for (String token : tokens) {
            if (StrUtil.contains(answer, token)) {
                count++;
            }
            if (count >= threshold) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String answer, String... tokens) {
        if (StrUtil.isBlank(answer)) {
            return false;
        }
        String normalized = answer.toLowerCase();
        for (String token : tokens) {
            if (normalized.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return StrUtil.blankToDefault(text, "").replace("\r\n", "\n").trim();
    }

    private record DefaultQuestionTemplate(String testCaseNo, String questionType, String userQuestion, String expectedKnowledge) {
    }

    private record ExecutionSnapshot(Long invokeRecordId, Long invokeRecordDetailId, String answer, String failReason,
                                     String invokeStatus, String modelName, Integer costTime, Long costToken) {
    }

    private record AcceptanceEvaluation(String hitConclusion, String groundedConclusion, String readableConclusion,
                                        String gracefulFailureConclusion, String hitRateConclusion,
                                        String completenessConclusion, String followUpCategory,
                                        String followUpAction, String remark) {
    }
}
