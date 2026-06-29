SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app'
      AND COLUMN_NAME = 'custom_prompt'
  ),
  'SELECT 1',
  'ALTER TABLE `app` ADD COLUMN `custom_prompt` text DEFAULT NULL COMMENT ''应用级自定义系统提示词'' AFTER `out_lib_enable`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'app'
      AND COLUMN_NAME = 'chat_model'
  ),
  'SELECT 1',
  'ALTER TABLE `app` ADD COLUMN `chat_model` varchar(150) DEFAULT NULL COMMENT ''应用级聊天模型名称'' AFTER `custom_prompt`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_lib'
      AND COLUMN_NAME = 'active_split_strategy'
  ),
  'SELECT 1',
  'ALTER TABLE `knowledge_lib` ADD COLUMN `active_split_strategy` varchar(50) DEFAULT NULL COMMENT ''当前生效切分策略'' AFTER `icon_path`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_lib'
      AND COLUMN_NAME = 'active_split_config_json'
  ),
  'SELECT 1',
  'ALTER TABLE `knowledge_lib` ADD COLUMN `active_split_config_json` text DEFAULT NULL COMMENT ''当前生效切分参数快照(json)'' AFTER `active_split_strategy`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_lib'
      AND COLUMN_NAME = 'active_experiment_id'
  ),
  'SELECT 1',
  'ALTER TABLE `knowledge_lib` ADD COLUMN `active_experiment_id` bigint DEFAULT NULL COMMENT ''当前生效实验版本id'' AFTER `active_split_config_json`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'knowledge_lib'
      AND COLUMN_NAME = 'active_experiment_name'
  ),
  'SELECT 1',
  'ALTER TABLE `knowledge_lib` ADD COLUMN `active_experiment_name` varchar(150) DEFAULT NULL COMMENT ''当前生效实验版本名称'' AFTER `active_experiment_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `knowledge_lib_experiment`
(
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lib_id`             bigint       NOT NULL COMMENT 'knowledge_lib表的主键',
  `version_name`       varchar(150) DEFAULT NULL COMMENT '实验版本名称',
  `query_text`         varchar(500) NOT NULL COMMENT '调试问题',
  `top_k`              int          NOT NULL DEFAULT 5 COMMENT '调试使用的topK',
  `split_strategy`     varchar(50)  NOT NULL COMMENT '切分策略',
  `split_config_json`  text         DEFAULT NULL COMMENT '切分参数快照(json)',
  `raw_hit_count`      int          NOT NULL DEFAULT 0 COMMENT '原始召回数',
  `rerank_hit_count`   int          NOT NULL DEFAULT 0 COMMENT '重排后召回数',
  `diagnosis_title`    varchar(150) NOT NULL COMMENT '诊断结论',
  `category_code`      varchar(50)  NOT NULL COMMENT '建议归类编码',
  `category_label`     varchar(50)  NOT NULL COMMENT '建议归类名称',
  `raw_top_summary`    varchar(500) DEFAULT NULL COMMENT '原始Top1摘要',
  `rerank_top_summary` varchar(500) DEFAULT NULL COMMENT '重排Top1摘要',
  `note_text`          varchar(500) DEFAULT NULL COMMENT '实验备注',
  `recommend_reason`   varchar(500) DEFAULT NULL COMMENT '推荐理由',
  `recommended`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否推荐版本,1:是,0:否',
  `created_time`       datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time`      datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_lib_id` (`lib_id`),
  KEY `idx_recommended` (`recommended`)
) ENGINE=InnoDB COMMENT='知识库实验版本';

CREATE TABLE IF NOT EXISTS `rag_acceptance_batch`
(
  `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`                bigint       NOT NULL COMMENT 'user表的主键',
  `app_id`                 bigint       DEFAULT NULL COMMENT 'app表的主键',
  `lib_id`                 bigint       DEFAULT NULL COMMENT 'knowledge_lib表的主键',
  `batch_name`             varchar(150) NOT NULL COMMENT '验收批次名称',
  `app_name`               varchar(150) DEFAULT NULL COMMENT '应用名称',
  `scene_type`             varchar(100) DEFAULT NULL COMMENT '场景类型',
  `knowledge_scope`        varchar(300) DEFAULT NULL COMMENT '知识库范围',
  `release_version`        varchar(100) DEFAULT NULL COMMENT '发布版本',
  `experiment_version`     varchar(100) DEFAULT NULL COMMENT '实验版本',
  `active_experiment_id`   bigint       DEFAULT NULL COMMENT '生效实验版本id',
  `active_experiment_name` varchar(150) DEFAULT NULL COMMENT '生效实验版本名称',
  `active_split_strategy`  varchar(50)  DEFAULT NULL COMMENT '生效切分策略',
  `version_remark`         varchar(500) DEFAULT NULL COMMENT '版本说明',
  `quick_view`             varchar(50)  DEFAULT NULL COMMENT '聚焦视图',
  `quick_view_desc`        varchar(500) DEFAULT NULL COMMENT '聚焦说明',
  `tester_name`            varchar(100) DEFAULT NULL COMMENT '验收人',
  `test_date`              datetime     DEFAULT NULL COMMENT '验收日期',
  `summary_conclusion`     text         DEFAULT NULL COMMENT '汇总结论',
  `next_action`            text         DEFAULT NULL COMMENT '后续动作',
  `created_time`           datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time`          datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='RAG验收批次';

CREATE TABLE IF NOT EXISTS `rag_acceptance_item`
(
  `id`                          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id`                    bigint       NOT NULL COMMENT 'rag_acceptance_batch表的主键',
  `invoke_record_id`            bigint       NOT NULL COMMENT 'invoke_record表的主键',
  `invoke_record_detail_id`     bigint       DEFAULT NULL COMMENT 'invoke_record_detail表的主键',
  `test_case_no`                varchar(100) NOT NULL COMMENT '测试编号',
  `question_type`               varchar(100) DEFAULT NULL COMMENT '问题类型',
  `user_question`               text         DEFAULT NULL COMMENT '用户问题',
  `expected_knowledge`          text         DEFAULT NULL COMMENT '期望知识点或期望行为',
  `actual_answer_summary`       text         DEFAULT NULL COMMENT '实际回答摘要',
  `actual_answer`               text         DEFAULT NULL COMMENT '实际回答全文',
  `fail_reason`                 varchar(500) DEFAULT NULL COMMENT '失败原因',
  `hit_conclusion`              varchar(50)  DEFAULT NULL COMMENT '命中问题结论',
  `grounded_conclusion`         varchar(50)  DEFAULT NULL COMMENT '可信结论',
  `readable_conclusion`         varchar(50)  DEFAULT NULL COMMENT '易懂结论',
  `graceful_failure_conclusion` varchar(50)  DEFAULT NULL COMMENT '失败体面结论',
  `hit_rate_conclusion`         varchar(50)  DEFAULT NULL COMMENT 'HitRate结论',
  `completeness_conclusion`     varchar(50)  DEFAULT NULL COMMENT '完整性结论',
  `follow_up_category`          varchar(50)  DEFAULT NULL COMMENT '跟进分类',
  `follow_up_action`            varchar(500) DEFAULT NULL COMMENT '跟进动作',
  `remark`                      varchar(500) DEFAULT NULL COMMENT '备注',
  `invoke_status`               varchar(50)  DEFAULT NULL COMMENT '调用状态',
  `model_name`                  varchar(150) DEFAULT NULL COMMENT '模型名称',
  `app_name`                    varchar(150) DEFAULT NULL COMMENT '应用名称',
  `lib_name`                    varchar(150) DEFAULT NULL COMMENT '知识库名称',
  `cost_time`                   int          DEFAULT NULL COMMENT '耗时毫秒',
  `cost_token`                  bigint       DEFAULT NULL COMMENT 'token数',
  `created_time`                datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time`               datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_invoke_record_id` (`invoke_record_id`)
) ENGINE=InnoDB COMMENT='RAG验收条目';

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invoke_record'
      AND COLUMN_NAME = 'fail_reason'
  ),
  'SELECT 1',
  'ALTER TABLE `invoke_record` ADD COLUMN `fail_reason` varchar(500) DEFAULT NULL COMMENT ''失败原因'' AFTER `cost_time`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
