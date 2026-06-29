package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.enums.UploadFileStatusEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.EnumUtil;
import com.osh.ai.assistant.consumer.bean.req.uploadfile.FileSplitPreviewReq;
import com.osh.ai.assistant.consumer.bean.vo.FilePreviewVO;
import com.osh.ai.assistant.consumer.elt.RagSplitRuntimeConfig;
import com.osh.ai.assistant.consumer.elt.RagDocumentSplitService;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Slf4j
public class FilePreviewController {

    private final UploadFileService uploadFileService;
    private final RagDocumentSplitService ragDocumentSplitService;

    /**
     * 预览文件内容（返回前1000行或前50KB）
     */
    @GetMapping("/preview")
    public Result<FilePreviewVO> preview(@RequestParam("id") Long id) {
        UploadFileDO file = uploadFileService.requireOwnedEntity(id);
        return Result.buildSuccess(buildPreview(file, ragDocumentSplitService.buildCurrentConfig()));
    }

    /**
     * 按试算规则预览切分结果
     */
    @PostMapping("/previewSplit")
    public Result<FilePreviewVO> previewSplit(@RequestBody @Validated FileSplitPreviewReq req) {
        UploadFileDO file = uploadFileService.requireOwnedEntity(req.getId());
        RagSplitRuntimeConfig runtimeConfig = ragDocumentSplitService.mergePreviewConfig(
            req.getStrategy(),
            req.getChunkSize(),
            req.getMinChunkSizeChars(),
            req.getMinChunkLengthToEmbed(),
            req.getMaxNumChunks(),
            req.getKeepSeparator(),
            req.getSemanticSectionMaxChars(),
            req.getPreviewChunkLimit()
        );
        return Result.buildSuccess(buildPreview(file, runtimeConfig));
    }

    private FilePreviewVO buildPreview(UploadFileDO file, RagSplitRuntimeConfig runtimeConfig) {
        FilePreviewVO result = new FilePreviewVO();
        result.setFileName(file.getFileName());
        result.setCharCount(file.getCharCount());
        result.setStatus(file.getStatus());
        result.setStatusDesc(EnumUtil.getDescByCode(file.getStatus(), UploadFileStatusEnum.class));
        result.setSplitConfig(ragDocumentSplitService.toConfigVO(runtimeConfig));

        try {
            List<Document> rawDocuments = ragDocumentSplitService.read(file.getStorePath());
            String content = rawDocuments
                .stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
            List<Document> splitDocuments = ragDocumentSplitService.splitDocuments(rawDocuments, runtimeConfig);
            result.setContent(truncate(content));
            result.setChunkCount(splitDocuments.size());
            result.setChunks(ragDocumentSplitService.buildChunkPreview(splitDocuments, runtimeConfig));
        } catch (BizEx e) {
            log.warn("预览文件失败,id={}, msg={}", file.getId(), e.getMessage());
            result.setContent(e.getMessage());
            result.setChunkCount(0);
        } catch (Exception e) {
            log.error("读取文件失败", e);
            result.setContent("读取文件失败: " + e.getMessage());
            result.setChunkCount(0);
        }
        return result;
    }

    private String truncate(String content) {
        if (content == null || content.isBlank()) {
            return "暂无可预览内容";
        }
        if (content.length() <= 50000) {
            return content;
        }
        return content.substring(0, 50000) + "\n\n... (内容过长，已截断)";
    }
}
