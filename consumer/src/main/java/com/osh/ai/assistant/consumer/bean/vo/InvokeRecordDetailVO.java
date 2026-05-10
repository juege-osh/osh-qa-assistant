package com.osh.ai.assistant.consumer.bean.vo;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;

import java.util.Date;
/**
  * 调用记录明细视图对象
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@Data
public class InvokeRecordDetailVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * invoke_record表的主键
     */
    private Long invokeRecordId;
    /**
     * 模型类型
     */
    private String modelName;
    /**
     * 消耗token数
     */
    private Integer costToken;
    /**
     * 状态,0:失败,1:成功
     */
    private Integer status;
    /**
     * {@link #status}描述
     */
    private String statusDesc;
    /**
     * 耗时,单位:毫秒
     */
    private Integer costTime;
    /**
     * 失败原因
     */
    private String failReason;
    /**
     * 开始时间
     */
    private Date startTime;
    /**
     * 结束时间
     */
    private Date endTime;
    /**
     * 用户输入
     */
    private String userInput;
    /**
     * 相应结果
     */
    private String assistantMessage;

    public String getStatusDesc() {
        return EnumUtil.getDescByCode(status, InvokeStatusEnum.class);
    }
}
