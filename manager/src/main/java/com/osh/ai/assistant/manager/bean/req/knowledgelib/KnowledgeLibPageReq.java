package com.osh.ai.assistant.manager.bean.req.knowledgelib;
import com.osh.ai.assistant.common.bean.req.BasePageReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询入参

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeLibPageReq extends BasePageReq {
      /**
      * 用户名
      */
     private String username;
      /**
      * 知识库名称
      */
     private String libName;
}
