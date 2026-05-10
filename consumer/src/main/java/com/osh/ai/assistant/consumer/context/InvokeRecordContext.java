package com.osh.ai.assistant.consumer.context;

import com.osh.ai.assistant.consumer.builder.InvokeRecordBuilder;

/**
 * 存放调用记录id上下文
 */
public class InvokeRecordContext {
    private static final ThreadLocal<InvokeRecordBuilder> THREAD_LOCAL = new ThreadLocal<>();
    public static void set(InvokeRecordBuilder invokeRecordBuilder) {
        THREAD_LOCAL.set(invokeRecordBuilder);
    }
    public static InvokeRecordBuilder get() {
        return THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
