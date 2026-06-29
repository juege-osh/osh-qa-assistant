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

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class RagDocumentSplitService {

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
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(
            ragSplitProperties.getChunkSize(),
            ragSplitProperties.getMinChunkSizeChars(),
            ragSplitProperties.getMinChunkLengthToEmbed(),
            ragSplitProperties.getMaxNumChunks(),
            ragSplitProperties.isKeepSeparator()
        );
        return tokenTextSplitter.split(documents);
    }

    public RagSplitConfigVO getSplitConfig() {
        RagSplitConfigVO configVO = new RagSplitConfigVO();
        configVO.setChunkSize(ragSplitProperties.getChunkSize());
        configVO.setMinChunkSizeChars(ragSplitProperties.getMinChunkSizeChars());
        configVO.setMinChunkLengthToEmbed(ragSplitProperties.getMinChunkLengthToEmbed());
        configVO.setMaxNumChunks(ragSplitProperties.getMaxNumChunks());
        configVO.setKeepSeparator(ragSplitProperties.isKeepSeparator());
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
