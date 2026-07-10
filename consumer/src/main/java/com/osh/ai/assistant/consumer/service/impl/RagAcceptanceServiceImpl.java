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
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceRunBatchReq;
import com.osh.ai.assistant.consumer.bean.req.invokerecord.RagAcceptanceRunQuestionReq;
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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    private static final List<AcceptanceQuestionTemplate> DEFAULT_QUESTION_TEMPLATES = List.of(
        new AcceptanceQuestionTemplate("TC-01", "标准知识问答", "这个知识库当前主要覆盖哪些主题？", "能说明当前知识库覆盖范围、主题边界或主要文档类型。"),
        new AcceptanceQuestionTemplate("TC-02", "标准知识问答", "某个制度或流程的核心结论是什么？", "能直接给结论，并指出依据或关键规则。"),
        new AcceptanceQuestionTemplate("TC-03", "任务指导", "我应该从哪里开始完成这个任务？", "给出开始入口、前置准备和建议顺序。"),
        new AcceptanceQuestionTemplate("TC-04", "任务指导", "如果我要把这个流程走通，先后步骤应该是什么？", "按顺序说明步骤，并提示关键注意事项。"),
        new AcceptanceQuestionTemplate("TC-05", "模糊提问", "这个事情我该怎么弄？", "先识别问题模糊，再引导补充关键信息或给出安全的初始路径。"),
        new AcceptanceQuestionTemplate("TC-06", "模糊提问", "我现在不知道从哪开始，你建议我先看什么？", "给出最小起步建议和优先阅读内容。"),
        new AcceptanceQuestionTemplate("TC-07", "未命中知识", "请回答一个当前知识库里没有覆盖的外部问题。", "明确说明当前缺少依据，不要强答，并给出下一步建议。"),
        new AcceptanceQuestionTemplate("TC-08", "未命中知识", "我只说一个很模糊的事情，你直接告诉我答案。", "提醒问题信息不足，建议补充关键词或制度名称。"),
        new AcceptanceQuestionTemplate("TC-09", "失败场景", "如果当前没找到相关知识，你会怎么告诉我？", "反馈应说明未找到足够依据，并给出可理解的下一步。"),
        new AcceptanceQuestionTemplate("TC-10", "失败场景", "如果知识库检索暂时不可用，你会怎么提示我？", "反馈应避免报错堆栈，提示稍后重试或换更明确问法。")
    );

    @Override
    public Long saveBatch(RagAcceptanceBatchSaveReq req) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> saveBatchInternal(req)));
    }

    private Long saveBatchInternal(RagAcceptanceBatchSaveReq req) {
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
    public Long runDefaultBatch(RagAcceptanceRunDefaultBatchReq req) {
        RunBatchContext context = prepareRunBatchContext(req.getAppId());
        RagAcceptanceBatchSaveReq saveReq = initBatchSaveReq(req.getBatchName(), req.getSceneType(), req.getTesterName(),
            req.getSummaryConclusion(), req.getNextAction(), context.app(), context.knowledgeLib());
        saveReq.setReleaseVersion("默认问题集自动跑测");
        saveReq.setQuickView("defaultQuestionPool");
        saveReq.setQuickViewDesc("系统直接批量执行默认问题集，生成真实回答、真实调用记录和正式验收批次。");
        return runBatchAndSave(saveReq, context, DEFAULT_QUESTION_TEMPLATES, "默认问题集自动跑测");
    }

    @Override
    public Long runBatch(RagAcceptanceRunBatchReq req) {
        RunBatchContext context = prepareRunBatchContext(req.getAppId());
        RagAcceptanceBatchSaveReq saveReq = initBatchSaveReq(req.getBatchName(), req.getSceneType(), req.getTesterName(),
            req.getSummaryConclusion(), req.getNextAction(), context.app(), context.knowledgeLib());
        saveReq.setReleaseVersion("真实问题集自动跑测");
        saveReq.setQuickView("realQuestionPool");
        saveReq.setQuickViewDesc("系统直接批量执行运营人提供的真实问题集，生成真实回答、真实调用记录和正式验收批次。");
        List<AcceptanceQuestionTemplate> templates = req.getQuestions().stream()
            .map(this::toAcceptanceQuestionTemplate)
            .toList();
        return runBatchAndSave(saveReq, context, templates, "真实问题集自动跑测");
    }

    private RunBatchContext prepareRunBatchContext(Long appId) {
        AppDO app = appService.requireOwnedEntity(appId);
        appService.checkChatCondition(app.getId());
        KnowledgeLibDO knowledgeLib = app.getLibId() == null ? null : knowledgeLibService.requireOwnedEntity(app.getLibId());
        UserDO currentUser = userService.getById(UserContext.getUserId());
        if (currentUser == null) {
            throw new BizEx("当前用户不存在");
        }
        return new RunBatchContext(app, knowledgeLib, currentUser);
    }

    private RagAcceptanceBatchSaveReq initBatchSaveReq(String batchName, String sceneType, String testerName,
                                                       String summaryConclusion, String nextAction,
                                                       AppDO app, KnowledgeLibDO knowledgeLib) {
        RagAcceptanceBatchSaveReq saveReq = new RagAcceptanceBatchSaveReq();
        saveReq.setAppId(app.getId());
        saveReq.setLibId(app.getLibId());
        saveReq.setBatchName(StrUtil.trim(batchName));
        saveReq.setAppName(app.getAppName());
        saveReq.setSceneType(StrUtil.blankToDefault(StrUtil.trim(sceneType), "内部知识问答"));
        saveReq.setKnowledgeScope(knowledgeLib == null ? null : knowledgeLib.getLibName());
        saveReq.setExperimentVersion(knowledgeLib == null ? null : knowledgeLib.getActiveExperimentName());
        saveReq.setActiveExperimentId(knowledgeLib == null ? null : knowledgeLib.getActiveExperimentId());
        saveReq.setActiveExperimentName(knowledgeLib == null ? null : knowledgeLib.getActiveExperimentName());
        saveReq.setActiveSplitStrategy(knowledgeLib == null ? null : knowledgeLib.getActiveSplitStrategy());
        saveReq.setVersionRemark(buildVersionRemark(app, knowledgeLib));
        saveReq.setTesterName(StrUtil.emptyToNull(StrUtil.trim(testerName)));
        saveReq.setTestDate(DateUtil.now());
        saveReq.setSummaryConclusion(StrUtil.emptyToNull(StrUtil.trim(summaryConclusion)));
        saveReq.setNextAction(StrUtil.emptyToNull(StrUtil.trim(nextAction)));
        return saveReq;
    }

    private Long runBatchAndSave(RagAcceptanceBatchSaveReq saveReq, RunBatchContext context,
                                 List<AcceptanceQuestionTemplate> templates, String batchLabel) {
        Long batchId = createRunningBatch(saveReq, batchLabel, templates.size());
        List<RagAcceptanceItemSaveReq> items = new ArrayList<>();
        int completedCount = 0;
        try {
            for (AcceptanceQuestionTemplate template : templates) {
                ExecutionSnapshot snapshot = executeQuestion(context.app(), context.currentUser(), template);
                AcceptanceEvaluation evaluation = evaluateAcceptance(template, snapshot);
                RagAcceptanceItemSaveReq item = buildAcceptanceItem(context, template, snapshot, evaluation);
                items.add(item);
                completedCount++;
                appendBatchItem(batchId, item);
                refreshRunningBatch(batchId, batchLabel, completedCount, templates.size());
            }
            if (StrUtil.isBlank(saveReq.getSummaryConclusion())) {
                saveReq.setSummaryConclusion(buildSummaryConclusion(items, batchLabel));
            }
            if (StrUtil.isBlank(saveReq.getNextAction())) {
                saveReq.setNextAction(buildNextAction(items));
            }
            finalizeBatch(batchId, saveReq);
            return batchId;
        } catch (Exception ex) {
            markBatchInterrupted(batchId, batchLabel, completedCount, templates.size(), ex);
            throw ex;
        }
    }

    private Long createRunningBatch(RagAcceptanceBatchSaveReq saveReq, String batchLabel, int totalCount) {
        RagAcceptanceBatchDO entity = new RagAcceptanceBatchDO();
        entity.setUserId(UserContext.getUserId());
        fillBatchFields(entity, saveReq);
        entity.setSummaryConclusion(buildRunningSummary(batchLabel, 0, totalCount));
        entity.setNextAction(buildRunningNextAction(0, totalCount));
        Date now = new Date();
        entity.setCreatedTime(now);
        entity.setModifiedTime(now);
        batchMapper.insert(entity);
        return entity.getId();
    }

    private RagAcceptanceItemSaveReq buildAcceptanceItem(RunBatchContext context, AcceptanceQuestionTemplate template,
                                                         ExecutionSnapshot snapshot, AcceptanceEvaluation evaluation) {
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
        item.setAppName(context.app().getAppName());
        item.setLibName(context.knowledgeLib() == null ? "" : context.knowledgeLib().getLibName());
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
        return item;
    }

    private void appendBatchItem(Long batchId, RagAcceptanceItemSaveReq itemReq) {
        transactionTemplate.executeWithoutResult(status -> {
            RagAcceptanceItemDO item = ConvertUtil.convert(itemReq, RagAcceptanceItemDO.class);
            item.setId(null);
            item.setBatchId(batchId);
            itemMapper.insert(item);
        });
    }

    private void refreshRunningBatch(Long batchId, String batchLabel, int completedCount, int totalCount) {
        transactionTemplate.executeWithoutResult(status -> {
            RagAcceptanceBatchDO batch = new RagAcceptanceBatchDO();
            batch.setId(batchId);
            batch.setSummaryConclusion(buildRunningSummary(batchLabel, completedCount, totalCount));
            batch.setNextAction(buildRunningNextAction(completedCount, totalCount));
            batch.setModifiedTime(new Date());
            batchMapper.updateById(batch);
        });
    }

    private void finalizeBatch(Long batchId, RagAcceptanceBatchSaveReq saveReq) {
        transactionTemplate.executeWithoutResult(status -> {
            RagAcceptanceBatchDO batch = requireOwnedBatch(batchId);
            fillBatchFields(batch, saveReq);
            batch.setModifiedTime(new Date());
            batchMapper.updateById(batch);
        });
    }

    private void markBatchInterrupted(Long batchId, String batchLabel, int completedCount, int totalCount, Exception ex) {
        transactionTemplate.executeWithoutResult(status -> {
            RagAcceptanceBatchDO batch = new RagAcceptanceBatchDO();
            batch.setId(batchId);
            batch.setSummaryConclusion(buildInterruptedSummary(batchLabel, completedCount, totalCount, ex));
            batch.setNextAction("请先查看当前批次已落库条目与调用日志，再从失败题继续补跑，不要直接把整批结果当成未执行。");
            batch.setModifiedTime(new Date());
            batchMapper.updateById(batch);
        });
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

    private AcceptanceQuestionTemplate toAcceptanceQuestionTemplate(RagAcceptanceRunQuestionReq req) {
        return new AcceptanceQuestionTemplate(
            StrUtil.trim(req.getTestCaseNo()),
            StrUtil.blankToDefault(StrUtil.trim(req.getQuestionType()), "真实业务问题"),
            StrUtil.trim(req.getUserQuestion()),
            StrUtil.emptyToNull(StrUtil.trim(req.getExpectedKnowledge()))
        );
    }

    private ExecutionSnapshot executeQuestion(AppDO app, UserDO currentUser, AcceptanceQuestionTemplate template) {
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
            throw new BizEx("问题集跑测失败，未生成调用记录");
        }
        List<InvokeRecordDetailDO> details = invokeRecordDetailService.selectByInvokeRecordId(invokeRecordId);
        if (details.isEmpty()) {
            throw new BizEx("问题集跑测失败，未生成调用明细");
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

    private AcceptanceEvaluation evaluateAcceptance(AcceptanceQuestionTemplate template, ExecutionSnapshot snapshot) {
        String answer = primaryAnswer(snapshot.answer());
        boolean invokeSuccess = InvokeStatusEnum.SUCCESS.getCode().toString().equals(snapshot.invokeStatus());
        boolean saysNoEnoughEvidence = containsAny(answer,
            "没有足够依据", "没有直接依据", "缺少知识依据", "当前知识库里没有直接依据",
            "暂时无法可靠回答", "资料不足以支撑", "暂无法提供可靠答案");
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

    private String evaluateHit(AcceptanceQuestionTemplate template, boolean invokeSuccess, String answer,
                               boolean asksMoreDetail, boolean saysNoEnoughEvidence) {
        if (StrUtil.isBlank(answer)) {
            return "不通过";
        }
        if (!invokeSuccess) {
            return isControlledRefusalPass(template, answer, saysNoEnoughEvidence) ? "通过" : "不通过";
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
            default -> evaluateCustomHit(template, answer, asksMoreDetail, saysNoEnoughEvidence);
        };
    }

    private String evaluateCustomHit(AcceptanceQuestionTemplate template, String answer,
                                     boolean asksMoreDetail, boolean saysNoEnoughEvidence) {
        if (StrUtil.isBlank(answer)) {
            return "不通过";
        }
        if (looksLikeCoverageQuestion(template)) {
            return evaluateCoverageQuestion(answer);
        }
        if (looksLikeRuntimeVsKnowledgeQuestion(template)) {
            return evaluateRuntimeVsKnowledgeQuestion(answer);
        }
        if (looksLikePreflightChecklistQuestion(template)) {
            return evaluatePreflightChecklistQuestion(answer);
        }
        if (looksLikeStaleIndexQuestion(template)) {
            return evaluateStaleIndexQuestion(answer);
        }
        if (looksLikeClarifiedFollowUpQuestion(template)) {
            return evaluateClarifiedFollowUpQuestion(answer);
        }
        if (looksLikeFollowUpDecisionQuestion(template)) {
            return evaluateFollowUpDecisionQuestion(answer);
        }
        if (looksLikeChunkingDiagnosisQuestion(template)) {
            return evaluateChunkingDiagnosisQuestion(answer);
        }
        if (looksLikePromptFixQuestion(template)) {
            return evaluatePromptFixQuestion(answer);
        }
        if (looksLikeKnowledgeDiagnosisQuestion(template)) {
            return evaluateKnowledgeDiagnosisQuestion(answer);
        }
        if (looksLikeForcedRiskRefusalQuestion(template)) {
            return evaluateForcedRiskRefusalQuestion(answer, saysNoEnoughEvidence);
        }
        if (looksLikeActiveVersionVerificationQuestion(template)) {
            return evaluateActiveVersionVerificationQuestion(answer);
        }
        if (looksLikeMinimalLoopQuestion(template)) {
            return evaluateMinimalLoopQuestion(answer);
        }
        if (looksLikeOrderedStepsQuestion(template)) {
            return evaluateOrderedStepsQuestion(answer);
        }
        if (looksLikeFirstStepQuestion(template)) {
            return containsAny(answer, "第一步", "先确认")
                && containsCountAtLeast(answer, 2, "目标任务名称", "所属知识库", "目标任务", "知识库")
                ? "通过" : "不通过";
        }
        if (looksLikeAmbiguousQuestion(template)) {
            return asksMoreDetail && containsAny(answer, "最小起步", "先查看", "生效版本", "覆盖范围") ? "通过" : "不通过";
        }
        if (looksLikeOutOfScopeQuestion(template)) {
            return saysNoEnoughEvidence && saysRetryOrEscalate(answer) ? "通过" : "不通过";
        }
        if ("未命中知识".equals(template.questionType()) || "失败场景".equals(template.questionType())) {
            return saysNoEnoughEvidence || containsAny(answer, "检索暂时不可用", "稍后重试") ? "通过" : "待确认";
        }
        if ("模糊提问".equals(template.questionType())) {
            return asksMoreDetail || containsAny(answer, "建议", "先看", "最小起步") ? "通过" : "待确认";
        }
        int expectedKeywordHits = countExpectedKeywordHits(template.expectedKnowledge(), answer);
        int requiredKeywordHits = requiredExpectedKeywordHits(template.expectedKnowledge());
        if (requiredKeywordHits > 0) {
            if (expectedKeywordHits >= requiredKeywordHits) {
                return "通过";
            }
            return expectedKeywordHits > 0 ? "待确认" : "不通过";
        }
        return answer.length() >= 20 ? "通过" : "待确认";
    }

    private boolean isControlledRefusalPass(AcceptanceQuestionTemplate template, String answer, boolean saysNoEnoughEvidence) {
        if (!saysNoEnoughEvidence || !saysRetryOrEscalate(answer)) {
            return false;
        }
        return looksLikeOutOfScopeQuestion(template)
            || "未命中知识".equals(template.questionType())
            || "失败场景".equals(template.questionType());
    }

    private String evaluateGrounded(boolean invokeSuccess, String answer, String hitConclusion, boolean saysNoEnoughEvidence) {
        if (StrUtil.isBlank(answer)) {
            return "不通过";
        }
        if (!invokeSuccess && saysNoEnoughEvidence) {
            return "通过";
        }
        if (!invokeSuccess) {
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

    private String evaluateGraceful(AcceptanceQuestionTemplate template, boolean invokeSuccess, String answer,
                                    boolean saysNoEnoughEvidence, boolean saysRetryLater, boolean containsStackWords) {
        if (containsStackWords) {
            return "不通过";
        }
        if (!invokeSuccess && saysNoEnoughEvidence && saysRetryOrEscalate(answer)) {
            return "通过";
        }
        if (!invokeSuccess) {
            return containsAny(answer, "稍后重试", "更明确的问题", "暂时不可用") ? "通过" : "待确认";
        }
        if ("失败场景".equals(template.questionType()) || "未命中知识".equals(template.questionType())) {
            return (saysNoEnoughEvidence || containsAny(answer, "检索暂时不可用")) && saysRetryLater ? "通过" : "不通过";
        }
        if (isOperationalDecisionQuestion(template)) {
            return "通过";
        }
        if (saysNoEnoughEvidence) {
            return saysRetryLater || containsAny(answer, "请补充", "具体") ? "通过" : "待确认";
        }
        return "通过";
    }

    private String evaluateCompleteness(AcceptanceQuestionTemplate template, String answer, String hitConclusion) {
        if (!"通过".equals(hitConclusion)) {
            return "待确认";
        }
        return switch (template.testCaseNo()) {
            case "TC-03", "TC-04" -> containsCountAtLeast(answer, 2, "1.", "2.", "步骤", "建议", "先", "然后", "最后") ? "通过" : "待确认";
            case "TC-07", "TC-09", "TC-10" -> containsAny(answer, "建议", "稍后重试", "联系") ? "通过" : "待确认";
            default -> answer.length() >= 30 ? "通过" : "待确认";
        };
    }

    private String resolveFollowUpCategory(AcceptanceQuestionTemplate template, boolean invokeSuccess, String hitConclusion,
                                           String groundedConclusion, String readableConclusion, String gracefulFailureConclusion) {
        if (!invokeSuccess && "通过".equals(gracefulFailureConclusion) && "通过".equals(groundedConclusion)) {
            return "";
        }
        if (!invokeSuccess) {
            return "observe";
        }
        if ("不通过".equals(groundedConclusion)) {
            return "knowledge";
        }
        if ("不通过".equals(gracefulFailureConclusion) || "不通过".equals(readableConclusion)) {
            return "prompt";
        }
        if ("待确认".equals(hitConclusion) && isChunkingSensitiveQuestion(template)) {
            return "chunking";
        }
        if ("不通过".equals(hitConclusion) && ("标准知识问答".equals(template.questionType()) || "任务指导".equals(template.questionType()))) {
            return "chunking";
        }
        return "";
    }

    private String buildFollowUpAction(AcceptanceQuestionTemplate template, String followUpCategory,
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

    private String buildAcceptanceRemark(AcceptanceQuestionTemplate template, String hitConclusion,
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

    private String buildSummaryConclusion(List<RagAcceptanceItemSaveReq> items, String batchLabel) {
        long passCount = items.stream().filter(this::isPassSaveItem).count();
        long chunkingCount = items.stream().filter(item -> "chunking".equals(item.getFollowUpCategory())).count();
        long promptCount = items.stream().filter(item -> "prompt".equals(item.getFollowUpCategory())).count();
        long observeCount = items.stream().filter(item -> "observe".equals(item.getFollowUpCategory())).count();
        return batchLabel + "共 " + items.size() + " 条，当前全部通过 " + passCount + " 条；"
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

    private String buildRunningSummary(String batchLabel, int completedCount, int totalCount) {
        return batchLabel + "运行中，已完成 " + completedCount + " / " + totalCount
            + " 条；批次已提前创建，可继续通过列表页观察逐题落库进度。";
    }

    private String buildRunningNextAction(int completedCount, int totalCount) {
        return "当前批次仍在执行中，已完成 " + completedCount + " / " + totalCount
            + " 条；如长时间停留，请先查看已落库条目，再判断是否卡在单题调用。";
    }

    private String buildInterruptedSummary(String batchLabel, int completedCount, int totalCount, Exception ex) {
        String message = StrUtil.blankToDefault(StrUtil.trim(ex.getMessage()), "请查看调用记录与后端日志");
        if (message.length() > 60) {
            message = message.substring(0, 60);
        }
        return batchLabel + "中断，已完成 " + completedCount + " / " + totalCount + " 条；最近异常：" + message;
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

    private String evaluateCoverageQuestion(String answer) {
        boolean hasQuestionTypeHint = containsAny(answer, "问题", "类型", "主题", "覆盖");
        return hasCoverageTopics(answer) && hasQuestionTypeHint ? "通过" : "待确认";
    }

    private String evaluateRuntimeVsKnowledgeQuestion(String answer) {
        boolean hasBatchSignals = containsCountAtLeast(answer, 2,
            "正式验收批次", "待跟进分类", "同一组真实问题", "同样一组问题", "对比不同切分版本", "不同版本之间",
            "同一组问题", "失败模式", "得分差异明显", "排除法", "先排除运行态问题",
            "字段显示是新版本", "生效版本字段", "activeExperimentName", "activeSplitStrategy", "答案质量还像旧版本");
        boolean hasKnowledgeSide = containsCountAtLeast(answer, 2,
            "知识问题", "知识没写清楚", "没有足够依据", "缺规则边界", "分类口径", "知识库里应该有",
            "补知识", "知识库内容", "知识缺口", "知识内容本身没写清楚", "知识里缺具体边界");
        boolean hasRuntimeSide = containsCountAtLeast(answer, 2,
            "运行态问题", "切分问题", "切分版本", "模型配置", "索引", "检索", "上游", "暂时不可用", "超时",
            "observe", "流处理失败", "stream processing failed", "索引是否一致", "切换没真正生效", "回答一致性");
        return hasBatchSignals && hasKnowledgeSide && hasRuntimeSide ? "通过" : "待确认";
    }

    private String evaluatePreflightChecklistQuestion(String answer) {
        boolean hasTaskAndLib = containsCountAtLeast(answer, 2, "任务名称", "任务目标", "所属知识库", "知识库");
        boolean hasVersion = containsAny(answer, "生效切分版本", "当前生效切分版本", "切分版本");
        boolean hasPromptAndModel = containsCountAtLeast(answer, 2, "Prompt", "提示词", "模型", "模型开关");
        return hasTaskAndLib && hasVersion && hasPromptAndModel ? "通过" : "待确认";
    }

    private String evaluateStaleIndexQuestion(String answer) {
        boolean hasIllusion = containsCountAtLeast(answer, 2,
            "假象", "页面状态", "字段", "旧版本", "activeExperimentName", "activeSplitStrategy", "回答质量仍然是旧版本", "回答效果仍然像旧版本");
        boolean hasRepairAction = containsCountAtLeast(answer, 2,
            "重建索引", "复跑", "正式验收批次", "验收批次", "当前生效版本", "同一组真实问题", "对比");
        boolean hasRootCause = containsCountAtLeast(answer, 2,
            "实际被检索使用的内容", "旧索引", "索引还没按新版本重建完成", "只是先切标识", "标识", "误判成模型 / 提示词问题", "误判成模型/提示词问题");
        return hasIllusion && (hasRepairAction || hasRootCause) ? "通过" : "待确认";
    }

    private String evaluateClarifiedFollowUpQuestion(String answer) {
        boolean hasFirstTurn = containsCountAtLeast(answer, 2,
            "第一次", "模糊", "缺少什么信息", "最小起步建议", "补充关键词");
        boolean hasSecondTurn = containsCountAtLeast(answer, 2,
            "第二次", "补了关键词", "补充了关键词", "顺着关键词", "继续回答", "直接给答案", "不要再重复", "自然承接");
        return hasFirstTurn && hasSecondTurn ? "通过" : "待确认";
    }

    private String evaluateOrderedStepsQuestion(String answer) {
        boolean hasOrderWords = containsCountAtLeast(answer, 3,
            "1.", "2.", "3.", "第一步", "第二步", "第三步", "第四步", "第五步", "第六步", "先", "然后", "接着", "最后", "顺序");
        boolean hasKeySteps = containsCountAtLeast(answer, 4,
            "准备文档", "上传到知识库", "保存切分实验版本", "发布为当前生效版本", "重建索引", "绑定应用", "运行默认问题集", "正式验收");
        return hasOrderWords && hasKeySteps ? "通过" : "待确认";
    }

    private String evaluateFollowUpDecisionQuestion(String answer) {
        boolean hasThreeActions = containsCountAtLeast(answer, 3, "补知识", "补切分", "补提示词");
        boolean hasDecisionHints = containsCountAtLeast(answer, 2,
            "待跟进分类", "正式验收批次", "切分版本", "同一组问题", "长步骤题", "没有足够依据", "答案组织");
        return hasThreeActions && hasDecisionHints ? "通过" : "待确认";
    }

    private String evaluateChunkingDiagnosisQuestion(String answer) {
        boolean hasChunking = containsAny(answer, "切分", "chunking", "切分策略");
        boolean hasSymptoms = containsCountAtLeast(answer, 2, "长步骤题", "覆盖范围题", "总结型题", "断裂", "遗漏", "顺序不稳", "掉分");
        return hasChunking && hasSymptoms ? "通过" : "待确认";
    }

    private String evaluatePromptFixQuestion(String answer) {
        boolean hasPrompt = containsAny(answer, "提示词", "prompt");
        boolean hasSymptoms = containsCountAtLeast(answer, 2,
            "结论不够直接", "结论不直接", "失败反馈不够体面", "先结论", "再依据", "再下一步", "组织顺序", "结构乱", "结构混乱");
        return hasPrompt && hasSymptoms ? "通过" : "待确认";
    }

    private String evaluateKnowledgeDiagnosisQuestion(String answer) {
        boolean hasKnowledge = containsAny(answer, "补知识", "知识内容", "知识本身", "知识库");
        boolean hasSignals = containsCountAtLeast(answer, 2, "没有足够依据", "两个切分版本", "不同切分版本", "都答不稳", "规则", "口径", "操作边界");
        return hasKnowledge && hasSignals ? "通过" : "待确认";
    }

    private String evaluateForcedRiskRefusalQuestion(String answer, boolean saysNoEnoughEvidence) {
        boolean hasFirmRefusal = saysNoEnoughEvidence || containsCountAtLeast(answer, 2,
            "不会猜", "不能猜", "不能直接给出结论", "不能编造答案", "不会直接猜测", "明确拒绝猜测",
            "高风险", "高风险领域", "猜测性结论");
        boolean hasReason = containsCountAtLeast(answer, 2,
            "没有足够依据", "没有依据", "误判风险", "合规风险", "不是负责任的做法", "不能直接给出结论",
            "误导你的实际判断", "缺少具体来源依据", "没有覆盖这个方向的具体依据", "风险较高",
            "没有依据的情况下", "说明原因");
        boolean hasNextStep = containsCountAtLeast(answer, 2,
            "补充信息", "联系运营人", "补知识", "下一步", "制度名称", "制度文件", "文件出处", "流程节点", "流程来源",
            "业务背景", "替代方案", "合理的替代方案", "给动作", "给台阶", "换成知识库范围内的问题");
        boolean hasStructuredRefusal = containsCountAtLeast(answer, 2,
            "先明确边界，再说明原因，最后给动作", "明确边界", "说明原因", "最后给动作", "拒答的核心原则");
        return hasFirmRefusal && ((hasReason && hasNextStep) || (hasReason && hasStructuredRefusal)) ? "通过" : "待确认";
    }

    private String evaluateActiveVersionVerificationQuestion(String answer) {
        boolean hasRerunAction = containsAny(answer, "按当前生效版本复跑", "复跑同一组真实问题", "运行默认问题集跑测");
        boolean hasSnapshotEvidence = containsCountAtLeast(answer, 2,
            "activeExperimentName", "activeSplitStrategy", "正式验收批次", "对比结果", "重建索引", "当前生效版本");
        return hasRerunAction && hasSnapshotEvidence ? "通过" : "待确认";
    }

    private String evaluateMinimalLoopQuestion(String answer) {
        boolean hasOrderWords = containsCountAtLeast(answer, 4,
            "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.",
            "第一步", "第二步", "第三步", "第四步", "第五步", "先", "再", "然后", "最后");
        boolean hasSetup = containsCountAtLeast(answer, 2, "任务目标", "任务名称", "知识库", "生效切分版本", "Prompt", "提示词", "模型");
        boolean hasRunAndRepair = containsCountAtLeast(answer, 3, "运行默认问题集", "真实问题集", "正式验收批次", "补知识", "补切分", "补提示词", "复跑");
        return hasOrderWords && hasSetup && hasRunAndRepair ? "通过" : "待确认";
    }

    private boolean looksLikeCoverageQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "主要能回答哪些", "覆盖哪些", "哪些类型的问题", "覆盖范围");
    }

    private boolean looksLikeRuntimeVsKnowledgeQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "更像知识问题还是运行态问题", "知识问题还是运行态问题", "答案不稳");
    }

    private boolean looksLikePreflightChecklistQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "开始跑测前", "最少要先确认哪几件事", "接手一个新的训练测试任务");
    }

    private boolean looksLikeStaleIndexQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "忘了重建索引", "最容易出现什么假象", "伪生效");
    }

    private boolean looksLikeClarifiedFollowUpQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "第一次问题问得很模糊", "第二次才补了关键词", "最合适的承接方式");
    }

    private boolean looksLikeOrderedStepsQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "按什么顺序", "建议我按什么顺序", "真正走通", "先后步骤", "流程顺序");
    }

    private boolean looksLikeFollowUpDecisionQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "优先补知识", "补知识、补切分，还是补提示词", "补知识、补切分还是补提示词");
    }

    private boolean looksLikeChunkingDiagnosisQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "长步骤题", "总结型题", "反复掉分", "优先怀疑什么");
    }

    private boolean looksLikePromptFixQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "结论不够直接", "结论也不够直接", "收尾也不够体面", "更适合先修哪一层", "先修提示词", "结构总是乱");
    }

    private boolean looksLikeKnowledgeDiagnosisQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "两个切分版本都答不稳", "都老提示没有足够依据", "更像哪类问题");
    }

    private boolean looksLikeForcedRiskRefusalQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "还一直追问让我猜一个结论", "怎样拒答才算体面", "高风险问题");
    }

    private boolean looksLikeActiveVersionVerificationQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "切换了知识库的生效切分版本之后", "验证切换到底有没有真的生效", "生效切分版本之后", "忘了重建索引");
    }

    private boolean looksLikeMinimalLoopQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "最小可重复验收", "开始前到复跑后的动作顺序", "动作顺序应该怎么排");
    }

    private boolean looksLikeFirstStepQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "第一步应该先做什么", "第一步先做什么", "从哪里开始");
    }

    private boolean looksLikeAmbiguousQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "这个事情我该怎么弄", "我该怎么弄", "怎么办");
    }

    private boolean isOperationalDecisionQuestion(AcceptanceQuestionTemplate template) {
        return looksLikeCoverageQuestion(template)
            || looksLikeRuntimeVsKnowledgeQuestion(template)
            || looksLikePreflightChecklistQuestion(template)
            || looksLikeOrderedStepsQuestion(template)
            || looksLikeFollowUpDecisionQuestion(template)
            || looksLikeChunkingDiagnosisQuestion(template)
            || looksLikePromptFixQuestion(template)
            || looksLikeKnowledgeDiagnosisQuestion(template)
            || looksLikeActiveVersionVerificationQuestion(template)
            || looksLikeMinimalLoopQuestion(template);
    }

    private boolean looksLikeOutOfScopeQuestion(AcceptanceQuestionTemplate template) {
        return containsAny(template.userQuestion(), "美国联邦所得税", "2026 年怎么报", "外部问题", "超出知识库范围", "超出知识库覆盖范围");
    }

    private boolean isChunkingSensitiveQuestion(AcceptanceQuestionTemplate template) {
        return looksLikeCoverageQuestion(template)
            || looksLikeOrderedStepsQuestion(template)
            || "标准知识问答".equals(template.questionType())
            || "任务指导".equals(template.questionType());
    }

    private int countExpectedKeywordHits(String expectedKnowledge, String answer) {
        if (StrUtil.isBlank(expectedKnowledge) || StrUtil.isBlank(answer)) {
            return 0;
        }
        int hitCount = 0;
        for (String keyword : extractExpectedKeywords(expectedKnowledge)) {
            if (containsAny(answer, keyword)) {
                hitCount++;
            }
        }
        return hitCount;
    }

    private int requiredExpectedKeywordHits(String expectedKnowledge) {
        List<String> keywords = extractExpectedKeywords(expectedKnowledge);
        if (keywords.isEmpty()) {
            return 0;
        }
        return Math.min(2, keywords.size());
    }

    private List<String> extractExpectedKeywords(String expectedKnowledge) {
        if (StrUtil.isBlank(expectedKnowledge)) {
            return List.of();
        }
        return StrUtil.split(expectedKnowledge, '、')
            .stream()
            .flatMap(part -> StrUtil.split(part, '，').stream())
            .flatMap(part -> StrUtil.split(part, '。').stream())
            .map(String::trim)
            .map(this::normalizeExpectedKeyword)
            .filter(StrUtil::isNotBlank)
            .filter(keyword -> keyword.length() >= 2)
            .distinct()
            .toList();
    }

    private String normalizeExpectedKeyword(String keyword) {
        String normalized = StrUtil.blankToDefault(keyword, "");
        for (String prefix : List.of("概括", "给出", "说明", "明确说明", "明确", "提示", "识别", "建议", "安全的", "最小", "并且", "同时")) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
    }

    private boolean isPassSaveItem(RagAcceptanceItemSaveReq item) {
        return isPass(item.getHitConclusion())
            && isPass(item.getGroundedConclusion())
            && isPass(item.getReadableConclusion())
            && isPass(item.getGracefulFailureConclusion());
    }

    private boolean saysRetryOrEscalate(String answer) {
        return containsAny(answer,
            "更具体", "联系知识库运营", "联系维护人", "联系专业负责人", "联系对应专业负责人",
            "补充相关内容", "补充明确的制度名称", "流程来源", "稍后重试");
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

    private String primaryAnswer(String text) {
        String normalized = normalize(text);
        if (StrUtil.isBlank(normalized)) {
            return normalized;
        }
        int referenceIndex = normalized.indexOf("参考来源");
        if (referenceIndex < 0) {
            return normalized;
        }
        return StrUtil.trim(normalized.substring(0, referenceIndex));
    }

    private record AcceptanceQuestionTemplate(String testCaseNo, String questionType, String userQuestion, String expectedKnowledge) {
    }

    private record ExecutionSnapshot(Long invokeRecordId, Long invokeRecordDetailId, String answer, String failReason,
                                     String invokeStatus, String modelName, Integer costTime, Long costToken) {
    }

    private record RunBatchContext(AppDO app, KnowledgeLibDO knowledgeLib, UserDO currentUser) {
    }

    private record AcceptanceEvaluation(String hitConclusion, String groundedConclusion, String readableConclusion,
                                        String gracefulFailureConclusion, String hitRateConclusion,
                                        String completenessConclusion, String followUpCategory,
                                        String followUpAction, String remark) {
    }
}
