package com.osh.ai.assistant.consumer.constants;


public class ConsumerConstants {

    // 与 OpenAI 流式响应风格保持一致，前端收到这个标记后结束当前一轮展示。
    public static final String STREAM_END = "[DONE]";
    public static final Double MIN_SCORE = 0.3D;
}
