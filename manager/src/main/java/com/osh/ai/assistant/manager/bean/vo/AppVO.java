package com.osh.ai.assistant.manager.bean.vo;
import com.osh.ai.assistant.common.enums.YesNoEnum;
import com.osh.ai.assistant.common.jackson.AddressableUrl;
import com.osh.ai.assistant.common.util.EnumUtil;
import lombok.Data;
import java.util.Date;
/**
  * 应用信息视图对象
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@Data
public class AppVO {
    /**
     * 主键
     */
    private Long id;
    /**
     * user表的主键
     */
    private Long userId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 应用描述
     */
    private String appDesc;
    /**
     * 图标存放路径,格式如:resources/type/20230523/123.jpg
     */
    @AddressableUrl
    private String iconPath;
    /**
     * 超出知识库的问题是否回答
     * @see YesNoEnum
     */
    private Integer outLibEnable;
    /**
     * {@link #outLibEnable}描述
     */
    private String outLibEnableDesc;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 修改时间
     */
    private Date modifiedTime;
    /**
     * 关联的知识库id
     */
    private Long libId;
    /**
     * 关联的知识库名称
     */
    private String libName;

    public String getOutLibEnableDesc() {
        return EnumUtil.getDescByCode(outLibEnable, YesNoEnum.class);
    }
}
