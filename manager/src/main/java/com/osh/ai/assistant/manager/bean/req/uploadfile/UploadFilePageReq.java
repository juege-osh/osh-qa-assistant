package com.osh.ai.assistant.manager.bean.req.uploadfile;
import com.osh.ai.assistant.common.bean.req.BasePageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件信息分页查询入参

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UploadFilePageReq extends BasePageReq {
    /**
     * 用户名
     */
    private String username;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 知识库名称
     */
    private String libName;
}
