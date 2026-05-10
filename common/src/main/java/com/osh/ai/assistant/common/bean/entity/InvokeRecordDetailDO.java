package com.osh.ai.assistant.common.bean.entity;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.math.BigDecimal;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 调用记录明细

 */
@Data
@TableName("invoke_record_detail")
public class InvokeRecordDetailDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * invoke_record表的主键
     */
    private Long invokeRecordId;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 消耗token数
     */
    private Long costToken;
    /**
     * 状态,0:失败,1:成功
     */
    private Integer status;
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
}
