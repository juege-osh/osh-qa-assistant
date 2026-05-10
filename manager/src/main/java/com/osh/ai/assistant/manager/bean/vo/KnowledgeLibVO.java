package com.osh.ai.assistant.manager.bean.vo;
import com.osh.ai.assistant.common.jackson.AddressableUrl;
import lombok.Data;
import java.util.Date;
/**
  * 知识库视图对象
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@Data
public class KnowledgeLibVO {
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
     * 知识库名称
     */
    private String libName;
    /**
     * 知识库描述
     */
    private String libDesc;
    /**
     * 图标存放路径,格式如:resources/type/20230523/123.jpg
     */
    @AddressableUrl
    private String iconPath;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 修改时间
     */
    private Date modifiedTime;
}
