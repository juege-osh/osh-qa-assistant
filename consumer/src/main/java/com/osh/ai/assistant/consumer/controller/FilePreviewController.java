package com.osh.ai.assistant.consumer.controller;

import com.osh.ai.assistant.common.bean.entity.UploadFileDO;
import com.osh.ai.assistant.common.bean.res.Result;
import com.osh.ai.assistant.common.enums.UploadFileStatusEnum;
import com.osh.ai.assistant.common.ex.BizEx;
import com.osh.ai.assistant.common.util.EnumUtil;
import com.osh.ai.assistant.consumer.elt.reader.TikaDocReader;
import com.osh.ai.assistant.consumer.service.UploadFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Slf4j
public class FilePreviewController {

    private final UploadFileService uploadFileService;
    private final TikaDocReader tikaDocReader;

    /**
     * 预览文件内容（返回前1000行或前50KB）
     */
    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam("id") Long id) {
        UploadFileDO file = uploadFileService.requireOwnedEntity(id);

        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.getFileName());
        result.put("charCount", file.getCharCount());
        result.put("status", file.getStatus());
        result.put("statusDesc", EnumUtil.getDescByCode(file.getStatus(), UploadFileStatusEnum.class));

        try {
            String content = tikaDocReader.read(file.getStorePath())
                .stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
            result.put("content", truncate(content));
        } catch (BizEx e) {
            log.warn("预览文件失败,id={}, msg={}", id, e.getMessage());
            result.put("content", e.getMessage());
        } catch (Exception e) {
            log.error("读取文件失败", e);
            result.put("content", "读取文件失败: " + e.getMessage());
        }

        return Result.buildSuccess(result);
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
