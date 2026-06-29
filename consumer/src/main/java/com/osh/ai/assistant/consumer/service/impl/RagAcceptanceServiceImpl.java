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
            items.add(item);
        }
        saveReq.setItems(items);
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

    private record DefaultQuestionTemplate(String testCaseNo, String questionType, String userQuestion, String expectedKnowledge) {
    }

    private record ExecutionSnapshot(Long invokeRecordId, Long invokeRecordDetailId, String answer, String failReason,
                                     String invokeStatus, String modelName, Integer costTime, Long costToken) {
    }
}
