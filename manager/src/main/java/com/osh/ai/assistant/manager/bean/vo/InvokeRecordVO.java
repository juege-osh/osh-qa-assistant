package com.osh.ai.assistant.manager.bean.vo;
import com.osh.ai.assistant.common.enums.InvokeStatusEnum;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
  * 调用记录视图对象
  */
@Data
public class InvokeRecordVO {
    /**
     * 主键
     */
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
     * 状态
     * @see InvokeStatusEnum
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
     * 知识库名称
     */
    private String libName;
    /**
     * 应用名称
     */
    private String appName;

    private List<InvokeRecordDetailVO> detailList;

    public String getStatusDesc() {
        return EnumUtil.getDescByCode(status, InvokeStatusEnum.class);
    }

}
