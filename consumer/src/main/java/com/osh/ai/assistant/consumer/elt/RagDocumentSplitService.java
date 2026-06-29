package com.osh.ai.assistant.consumer.elt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.consumer.bean.vo.RagSplitChunkVO;
import com.osh.ai.assistant.consumer.bean.vo.RagSplitConfigVO;
import com.osh.ai.assistant.consumer.config.properties.RagSplitProperties;
import com.osh.ai.assistant.consumer.elt.reader.DocReader;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class RagDocumentSplitService {
    private static final Pattern HEADING_PATTERN = Pattern.compile(
        "^(#{1,6}\\s+.+|第[一二三四五六七八九十百零\\d]+[章节部分篇条].*|[一二三四五六七八九十]+[、.．].*|\\d{1,3}[、.．)]\\s*.+|[(（]?[一二三四五六七八九十\\d]+[)）]\\s*.+|Q[:：].+|A[:：].+)$"
    );

    private final DocReader docReader;
    private final RagSplitProperties ragSplitProperties;

    public List<Document> read(String storePath) {
        return docReader.read(storePath);
    }

    public List<Document> split(String storePath) {
        return splitDocuments(read(storePath));
    }

    public List<Document> splitDocuments(List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return Collections.emptyList();
        }
        if (!useSemanticSplit()) {
            return buildTokenTextSplitter().split(documents);
        }
        List<Document> splitDocuments = new ArrayList<>();
        for (Document document : documents) {
            splitDocuments.addAll(splitDocumentBySemanticSections(document));
            if (splitDocuments.size() >= ragSplitProperties.getMaxNumChunks()) {
                break;
            }
        }
        if (splitDocuments.size() > ragSplitProperties.getMaxNumChunks()) {
            return new ArrayList<>(splitDocuments.subList(0, ragSplitProperties.getMaxNumChunks()));
        }
        return splitDocuments;
    }

    private List<Document> splitDocumentBySemanticSections(Document document) {
        if (document == null || StrUtil.isBlank(document.getText())) {
            return Collections.emptyList();
        }
        String normalizedText = normalizeText(document.getText());
        List<String> sections = buildSemanticSections(normalizedText);
        if (CollUtil.isEmpty(sections)) {
            return splitWithTokenSplitter(normalizedText, document.getMetadata());
        }
        List<Document> ret = new ArrayList<>();
        for (String section : sections) {
            if (StrUtil.isBlank(section)) {
                continue;
            }
            if (StrUtil.length(section) > ragSplitProperties.getSemanticSectionMaxChars()) {
                ret.addAll(splitWithTokenSplitter(section, document.getMetadata()));
            } else {
                ret.add(new Document(section, copyMetadata(document.getMetadata())));
            }
            if (ret.size() >= ragSplitProperties.getMaxNumChunks()) {
                break;
            }
        }
        return ret;
    }

    private List<String> buildSemanticSections(String text) {
        List<String> blocks = extractBlocks(text);
        if (CollUtil.isEmpty(blocks)) {
            return Collections.emptyList();
        }
        List<String> sections = new ArrayList<>();
        String currentHeading = null;
        StringBuilder currentBody = new StringBuilder();
        for (String block : blocks) {
            if (isHeadingBlock(block)) {
                flushSection(sections, currentHeading, currentBody);
                currentHeading = block.trim();
                currentBody.setLength(0);
                continue;
            }
            appendParagraph(currentBody, block);
            if (currentBody.length() >= ragSplitProperties.getSemanticSectionMaxChars()) {
                flushSection(sections, currentHeading, currentBody);
                currentBody.setLength(0);
            }
        }
        flushSection(sections, currentHeading, currentBody);
        return sections;
    }

    private List<String> extractBlocks(String text) {
        String[] lines = text.split("\n");
        List<String> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String line : lines) {
            String trimmed = StrUtil.trim(line);
            if (StrUtil.isBlank(trimmed)) {
                flushParagraph(blocks, paragraph);
                continue;
            }
            if (isHeadingLine(trimmed)) {
                flushParagraph(blocks, paragraph);
                blocks.add(trimmed);
                continue;
            }
            if (!paragraph.isEmpty()) {
                paragraph.append('\n');
            }
            paragraph.append(trimmed);
        }
        flushParagraph(blocks, paragraph);
        return blocks;
    }

    private void flushParagraph(List<String> blocks, StringBuilder paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        String content = paragraph.toString().trim();
        if (StrUtil.isNotBlank(content)) {
            blocks.add(content);
        }
        paragraph.setLength(0);
    }

    private void appendParagraph(StringBuilder currentBody, String block) {
        if (currentBody.isEmpty()) {
            currentBody.append(block.trim());
            return;
        }
        currentBody.append("\n\n").append(block.trim());
    }

    private void flushSection(List<String> sections, String heading, StringBuilder body) {
        String bodyText = body.toString().trim();
        if (StrUtil.isBlank(bodyText)) {
            return;
        }
        if (StrUtil.isBlank(heading)) {
            sections.add(bodyText);
            return;
        }
        sections.add(heading.trim() + "\n\n" + bodyText);
    }

    private boolean isHeadingBlock(String block) {
        return isHeadingLine(StrUtil.trim(block));
    }

    private boolean isHeadingLine(String line) {
        if (StrUtil.isBlank(line)) {
            return false;
        }
        if (HEADING_PATTERN.matcher(line).matches()) {
            return true;
        }
        if (line.length() > 40) {
            return false;
        }
        if (line.endsWith("：") || line.endsWith(":")) {
            return true;
        }
        if (containsSentenceEnding(line)) {
            return false;
        }
        return line.length() <= 24;
    }

    private boolean containsSentenceEnding(String line) {
        return StrUtil.containsAny(line, "。", "！", "？", "；", ".", "!", "?", ";");
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized.trim();
    }

    private List<Document> splitWithTokenSplitter(String text, Map<String, Object> metadata) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        return buildTokenTextSplitter().split(List.of(new Document(text, copyMetadata(metadata))));
    }

    private Map<String, Object> copyMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }

    private boolean useSemanticSplit() {
        return "semantic".equalsIgnoreCase(ragSplitProperties.getStrategy());
    }

    private TokenTextSplitter buildTokenTextSplitter() {
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
            ragSplitProperties.getChunkSize(),
            ragSplitProperties.getMinChunkSizeChars(),
            ragSplitProperties.getMinChunkLengthToEmbed(),
            ragSplitProperties.getMaxNumChunks(),
            ragSplitProperties.isKeepSeparator()
        );
        return tokenTextSplitter;
    }

    public RagSplitConfigVO getSplitConfig() {
        RagSplitConfigVO configVO = new RagSplitConfigVO();
        configVO.setStrategy(ragSplitProperties.getStrategy());
        configVO.setChunkSize(ragSplitProperties.getChunkSize());
        configVO.setMinChunkSizeChars(ragSplitProperties.getMinChunkSizeChars());
        configVO.setMinChunkLengthToEmbed(ragSplitProperties.getMinChunkLengthToEmbed());
        configVO.setMaxNumChunks(ragSplitProperties.getMaxNumChunks());
        configVO.setKeepSeparator(ragSplitProperties.isKeepSeparator());
        configVO.setSemanticSectionMaxChars(ragSplitProperties.getSemanticSectionMaxChars());
        configVO.setPreviewChunkLimit(ragSplitProperties.getPreviewChunkLimit());
        return configVO;
    }

    public List<RagSplitChunkVO> buildChunkPreview(List<Document> splitDocuments) {
        if (CollUtil.isEmpty(splitDocuments)) {
            return Collections.emptyList();
        }
        int limit = Math.max(ragSplitProperties.getPreviewChunkLimit(), 0);
        return IntStream.range(0, Math.min(splitDocuments.size(), limit))
            .mapToObj(index -> toChunkVO(index, splitDocuments.get(index)))
            .toList();
    }

    private RagSplitChunkVO toChunkVO(int index, Document document) {
        RagSplitChunkVO chunkVO = new RagSplitChunkVO();
        String text = document == null ? null : document.getText();
        chunkVO.setIndex(index + 1);
        chunkVO.setLength(StrUtil.length(text));
        chunkVO.setContent(StrUtil.blankToDefault(text, ""));
        return chunkVO;
    }
}
