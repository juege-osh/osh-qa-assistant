package com.osh.ai.assistant.manager.controller;
import com.osh.ai.assistant.common.bean.res.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.osh.ai.assistant.manager.bean.req.knowledgelib.KnowledgeLibPageReq;
import com.osh.ai.assistant.manager.bean.vo.KnowledgeLibVO;
import com.osh.ai.assistant.manager.service.KnowledgeLibService;
/**
  * 知识库Controller层
  * @author 项目维护者
  * @see <a href="https://example.invalid">项目维护者</a>
  */
@RestController
@RequestMapping("/knowledgeLib")
@RequiredArgsConstructor
@Slf4j
@Validated
public class KnowledgeLibController {

    private final KnowledgeLibService knowledgeLibService;

    /**
     * 知识库分页查询
     */
    @PostMapping("/queryPage")
    public Result<List<KnowledgeLibVO>> queryPage(@RequestBody KnowledgeLibPageReq pageReq) {
        return  knowledgeLibService.queryPage(pageReq);
    }
}
