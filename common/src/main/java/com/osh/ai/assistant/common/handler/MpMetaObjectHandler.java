package com.osh.ai.assistant.common.handler;

import com.osh.ai.assistant.common.bean.dto.TokenDTO;
import com.osh.ai.assistant.common.context.UserContext;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author zhaodaowen
 */
public class MpMetaObjectHandler implements MetaObjectHandler {
    /**
     * 使用mp的插入类api时会回调
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        // 通用填充
        this.setFieldValByName("createdTime", now,metaObject);
        this.setFieldValByName("creator", getCurrentUserName(),metaObject);
        // 根据更新策略填充
        this.strictInsertFill(metaObject, "modifiedTime", () -> now, Date.class);
        this.setFieldValByName("modifier",getCurrentUserName(),metaObject);
    }

    private Object getCurrentUserName() {
        TokenDTO dto = UserContext.get();
        if (dto == null) {
            // api调用的时候并没有context
            return null;
        }
        return dto.getUsername();
    }

    /**
     * 使用mp的更新类api时会回调增加modifiedTime字段的更新
     * UPDATE user SET user_name=?, modified_time=? WHERE deleted='N' AND (user_id = ? AND age <= ?)
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject,"modifiedTime",Date::new,Date.class);
        this.setFieldValByName("modifier",getCurrentUserName(),metaObject);
    }

    /**
     * 严格模式默认填充策略(通用填充不会回调此方法):
     * 如果待更新实体需要填充的字段(如modifiedTime、modifer等)值为null&&supplier提供的值不为null时才真正用
     * insertFill或updateFill中提供的值
     * @param fieldVal 开发者在insertFill或updateFill中自己写的supplier
     */
    @Override
    public MetaObjectHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        // 不管待填充字段有没有值,都直接使用insertFill或updateFill中自己写的supplier
        Object obj = fieldVal.get();
        if (Objects.nonNull(obj)) {
            metaObject.setValue(fieldName, obj);
        }
        return this;
    }
}
