package com.osh.ai.assistant.common.bean.entity;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * 调用记录
 */
@Data
@TableName("invoke_record")
public class InvokeRecordDO {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 请求id,可用于追踪定位请求
     */
    private String requestId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户名
     */
    private String username;
    /**
     * appKey,接口调用时需要传入
     */
    private String appKey;
    /**
     * 应用id
     */
    private Long appId;
    /**
     * 知识库id
     */
    private Long libId;
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
}
