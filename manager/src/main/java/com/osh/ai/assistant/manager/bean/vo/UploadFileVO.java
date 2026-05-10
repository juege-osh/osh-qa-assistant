package com.osh.ai.assistant.manager.bean.vo;
import com.osh.ai.assistant.common.enums.UploadFileStatusEnum;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;
import java.util.Date;
/**
  * 文件信息视图对象
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@Data
public class UploadFileVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * knowledge_base表的主键
     */
    private Long libId;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件存放路径,格式如:resources/type/20230523/123.jpg
     */
    private String storePath;
    /**
     * 字符数
     */
    private Long charCount;
    /**
     * 召回次数
     */
    private Long recallCount;
    /**
     * 状态
     * @see UploadFileStatusEnum
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 用户名
     */
    private String username;
    /**
     * 应用名
     */
    private String appName;
    /**
     * 知识库名称
     */
    private String libName;
    /**
     * {@link #status}描述
     */
    private String statusDesc;

    public String getStatusDesc() {
        return EnumUtil.getDescByCode(status, UploadFileStatusEnum.class);
    }
}
