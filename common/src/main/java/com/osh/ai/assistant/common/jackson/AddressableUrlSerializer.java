package com.osh.ai.assistant.common.jackson;

import cn.hutool.core.util.StrUtil;
import com.osh.ai.assistant.common.constants.CommonConstants;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * 自定义序列化逻辑
 *

 */
public class AddressableUrlSerializer extends JsonSerializer<String> implements ContextualSerializer {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (StrUtil.isBlank(value)){
            gen.writeString(value);
            return;
        }
        if (!value.startsWith(CommonConstants.RESOURCES_PREFIX)) {
            gen.writeString(value);
            return;
        }
        String baseResourceUrl = resolveBaseResourceUrl();
        if (StrUtil.isBlank(baseResourceUrl)) {
            gen.writeString(value);
            return;
        }
        value = baseResourceUrl + value;
        gen.writeString(value);
    }

    private String resolveBaseResourceUrl() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return null;
        }
        String requestUri = request.getRequestURI();
        String scopePrefix = "";
        if (requestUri.startsWith("/manager/")) {
            scopePrefix = "/manager/";
        } else if (requestUri.startsWith("/consumer/")) {
            scopePrefix = "/consumer/";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme())
            .append("://")
            .append(request.getServerName());
        int serverPort = request.getServerPort();
        if (!isDefaultPort(request.getScheme(), serverPort)) {
            builder.append(":").append(serverPort);
        }
        builder.append(scopePrefix);
        return builder.toString();
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
            || ("https".equalsIgnoreCase(scheme) && port == 443);
    }


    /**
     *
     * @param property 要序列化的字段
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        AddressableUrl annotation = property.getAnnotation(AddressableUrl.class);
        if (annotation != null && String.class.isAssignableFrom(property.getType().getRawClass())) {
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
