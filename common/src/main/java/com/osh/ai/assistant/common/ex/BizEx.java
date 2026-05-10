package com.osh.ai.assistant.common.ex;


import com.osh.ai.assistant.common.enums.CodeEnum;
import lombok.Getter;


@Getter
public class BizEx extends RuntimeException {
    private Integer code;
    public BizEx(String msg) {
        super(msg);
        this.code = CodeEnum.BIZ_ERR.getCode();
    }
    public BizEx(CodeEnum codeEnum) {
        super(codeEnum.getDesc());
        this.code = codeEnum.getCode();
    }
    public BizEx(Integer code,String msg) {
        super(msg);
        this.code = code;
    }
}
