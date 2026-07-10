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

    public List<Document> split(String storePath, RagSplitRuntimeConfig config) {
        return splitDocuments(read(storePath), config);
    }

    public List<Document> splitDocuments(List<Document> documents) {
        return splitDocuments(documents, buildCurrentConfig());
    }

    public List<Document> splitDocuments(List<Document> documents, RagSplitRuntimeConfig config) {
        if (CollUtil.isEmpty(documents)) {
            return Collections.emptyList();
        }
        if (!useSemanticSplit(config)) {
            return buildTokenTextSplitter(config).split(documents);
        }
        List<Document> splitDocuments = new ArrayList<>();
        for (Document document : documents) {
            splitDocuments.addAll(splitDocumentBySemanticSections(document, config));
            if (splitDocuments.size() >= config.getMaxNumChunks()) {
                break;
            }
        }
        if (splitDocuments.size() > config.getMaxNumChunks()) {
            return new ArrayList<>(splitDocuments.subList(0, config.getMaxNumChunks()));
        }
        return splitDocuments;
    }

    private List<Document> splitDocumentBySemanticSections(Document document, RagSplitRuntimeConfig config) {
        if (document == null || StrUtil.isBlank(document.getText())) {
            return Collections.emptyList();
        }
        String normalizedText = normalizeText(document.getText());
        List<String> sections = buildSemanticSections(normalizedText, config);
        if (CollUtil.isEmpty(sections)) {
            return splitWithTokenSplitter(normalizedText, document.getMetadata(), config);
        }
        List<Document> ret = new ArrayList<>();
        for (String section : sections) {
            if (StrUtil.isBlank(section)) {
                continue;
            }
            if (StrUtil.length(section) > config.getSemanticSectionMaxChars()) {
                ret.addAll(splitWithTokenSplitter(section, document.getMetadata(), config));
            } else {
                ret.add(new Document(section, copyMetadata(document.getMetadata())));
            }
            if (ret.size() >= config.getMaxNumChunks()) {
                break;
            }
        }
        return ret;
    }

    private List<String> buildSemanticSections(String text, RagSplitRuntimeConfig config) {
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
            if (currentBody.length() >= config.getSemanticSectionMaxChars()) {
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

    private List<Document> splitWithTokenSplitter(String text, Map<String, Object> metadata, RagSplitRuntimeConfig config) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        return buildTokenTextSplitter(config).split(List.of(new Document(text, copyMetadata(metadata))));
    }

    private Map<String, Object> copyMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }

    private boolean useSemanticSplit(RagSplitRuntimeConfig config) {
        return "semantic".equalsIgnoreCase(config.getStrategy());
    }

    private TokenTextSplitter buildTokenTextSplitter(RagSplitRuntimeConfig config) {
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
            config.getChunkSize(),
            config.getMinChunkSizeChars(),
            config.getMinChunkLengthToEmbed(),
            config.getMaxNumChunks(),
            config.isKeepSeparator()
        );
        return tokenTextSplitter;
    }

    public RagSplitConfigVO getSplitConfig() {
        return toConfigVO(buildCurrentConfig());
    }

    public RagSplitRuntimeConfig buildCurrentConfig() {
        RagSplitRuntimeConfig config = new RagSplitRuntimeConfig();
        config.setStrategy(ragSplitProperties.getStrategy());
        config.setChunkSize(ragSplitProperties.getChunkSize());
        config.setMinChunkSizeChars(ragSplitProperties.getMinChunkSizeChars());
        config.setMinChunkLengthToEmbed(ragSplitProperties.getMinChunkLengthToEmbed());
        config.setMaxNumChunks(ragSplitProperties.getMaxNumChunks());
        config.setKeepSeparator(ragSplitProperties.isKeepSeparator());
        config.setSemanticSectionMaxChars(ragSplitProperties.getSemanticSectionMaxChars());
        config.setPreviewChunkLimit(ragSplitProperties.getPreviewChunkLimit());
        return config;
    }

    public RagSplitRuntimeConfig mergePreviewConfig(
        String strategy,
        Integer chunkSize,
        Integer minChunkSizeChars,
        Integer minChunkLengthToEmbed,
        Integer maxNumChunks,
        Boolean keepSeparator,
        Integer semanticSectionMaxChars,
        Integer previewChunkLimit
    ) {
        RagSplitRuntimeConfig config = buildCurrentConfig();
        if (StrUtil.isNotBlank(strategy)) {
            config.setStrategy(strategy.trim());
        }
        if (chunkSize != null) {
            config.setChunkSize(chunkSize);
        }
        if (minChunkSizeChars != null) {
            config.setMinChunkSizeChars(minChunkSizeChars);
        }
        if (minChunkLengthToEmbed != null) {
            config.setMinChunkLengthToEmbed(minChunkLengthToEmbed);
        }
        if (maxNumChunks != null) {
            config.setMaxNumChunks(maxNumChunks);
        }
        if (keepSeparator != null) {
            config.setKeepSeparator(keepSeparator);
        }
        if (semanticSectionMaxChars != null) {
            config.setSemanticSectionMaxChars(semanticSectionMaxChars);
        }
        if (previewChunkLimit != null) {
            config.setPreviewChunkLimit(previewChunkLimit);
        }
        return config;
    }

    public RagSplitRuntimeConfig buildConfigFromSnapshot(String strategy, String splitConfigJson) {
        RagSplitRuntimeConfig config = buildCurrentConfig();
        if (StrUtil.isNotBlank(splitConfigJson)) {
            RagSplitConfigVO configVO = cn.hutool.json.JSONUtil.toBean(splitConfigJson, RagSplitConfigVO.class);
            if (configVO != null) {
                if (StrUtil.isNotBlank(configVO.getStrategy())) {
                    config.setStrategy(configVO.getStrategy());
                }
                if (configVO.getChunkSize() != null) {
                    config.setChunkSize(configVO.getChunkSize());
                }
                if (configVO.getMinChunkSizeChars() != null) {
                    config.setMinChunkSizeChars(configVO.getMinChunkSizeChars());
                }
                if (configVO.getMinChunkLengthToEmbed() != null) {
                    config.setMinChunkLengthToEmbed(configVO.getMinChunkLengthToEmbed());
                }
                if (configVO.getMaxNumChunks() != null) {
                    config.setMaxNumChunks(configVO.getMaxNumChunks());
                }
                if (configVO.getKeepSeparator() != null) {
                    config.setKeepSeparator(configVO.getKeepSeparator());
                }
                if (configVO.getSemanticSectionMaxChars() != null) {
                    config.setSemanticSectionMaxChars(configVO.getSemanticSectionMaxChars());
                }
                if (configVO.getPreviewChunkLimit() != null) {
                    config.setPreviewChunkLimit(configVO.getPreviewChunkLimit());
                }
            }
        }
        if (StrUtil.isNotBlank(strategy)) {
            config.setStrategy(strategy.trim());
        }
        return config;
    }

    public RagSplitConfigVO toConfigVO(RagSplitRuntimeConfig runtimeConfig) {
        RagSplitConfigVO configVO = new RagSplitConfigVO();
        configVO.setStrategy(runtimeConfig.getStrategy());
        configVO.setChunkSize(runtimeConfig.getChunkSize());
        configVO.setMinChunkSizeChars(runtimeConfig.getMinChunkSizeChars());
        configVO.setMinChunkLengthToEmbed(runtimeConfig.getMinChunkLengthToEmbed());
        configVO.setMaxNumChunks(runtimeConfig.getMaxNumChunks());
        configVO.setKeepSeparator(runtimeConfig.isKeepSeparator());
        configVO.setSemanticSectionMaxChars(runtimeConfig.getSemanticSectionMaxChars());
        configVO.setPreviewChunkLimit(runtimeConfig.getPreviewChunkLimit());
        return configVO;
    }

    public List<RagSplitChunkVO> buildChunkPreview(List<Document> splitDocuments) {
        return buildChunkPreview(splitDocuments, buildCurrentConfig());
    }

    public List<RagSplitChunkVO> buildChunkPreview(List<Document> splitDocuments, RagSplitRuntimeConfig config) {
        if (CollUtil.isEmpty(splitDocuments)) {
            return Collections.emptyList();
        }
        int limit = Math.max(config.getPreviewChunkLimit(), 0);
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
