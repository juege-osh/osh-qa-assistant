package com.osh.ai.assistant.consumer.bean.req.knowledgelib;
import com.osh.ai.assistant.common.bean.req.BasePageReq;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询入参

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeLibPageReq extends BasePageReq {
      /**
      * user表的主键
      */
      @JsonIgnore
     private Long userId;
      /**
      * 知识库名称
      */
     private String libName;
}
