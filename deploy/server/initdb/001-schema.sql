CREATE DATABASE IF NOT EXISTS ai_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_assistant;

CREATE TABLE IF NOT EXISTS `manager`
(
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        varchar(50)  NOT NULL COMMENT '用户名',
  `real_name`       varchar(50)  NOT NULL COMMENT '姓名',
  `pwd`             varchar(200) NOT NULL COMMENT '密码',
  `sex`             tinyint      NOT NULL COMMENT '1:男,0:女',
  `avatar_path`     varchar(300) DEFAULT NULL COMMENT '头像存放路径,格式如:resources/type/20230523/123.jpg',
  `last_login_time` datetime     DEFAULT NULL COMMENT '上次登录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE=InnoDB COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS `user`
(
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      varchar(50)  NOT NULL COMMENT '用户名',
  `pwd`           varchar(200) NOT NULL COMMENT '密码',
  `app_key`       varchar(100) NOT NULL COMMENT 'appKey,接口调用时需要传入',
  `register_time` datetime     NOT NULL DEFAULT current_timestamp() COMMENT '注册时间',
  `avatar_path`   varchar(300) DEFAULT NULL COMMENT '头像存放路径,格式如:resources/type/20230523/123.jpg',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `invoke_record`
(
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `request_id`  varchar(100) NOT NULL COMMENT '请求id,可用于追踪定位请求',
  `user_id`     bigint       NOT NULL COMMENT '用户id',
  `username`    varchar(50)  NOT NULL COMMENT '用户名',
  `app_key`     varchar(100) NOT NULL COMMENT 'appKey,接口调用时需要传入',
  `app_id`      bigint       NOT NULL COMMENT 'app表的主键',
  `lib_id`      bigint       NOT NULL COMMENT '知识库表的主键',
  `status`      tinyint      NOT NULL COMMENT '状态,0:失败,1:成功',
  `cost_time`   int          NOT NULL COMMENT '耗时,单位:毫秒',
  `start_time`  datetime     NOT NULL DEFAULT current_timestamp() COMMENT '开始时间',
  `end_time`    datetime     NOT NULL DEFAULT current_timestamp() COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB COMMENT='调用记录';

CREATE TABLE IF NOT EXISTS `invoke_record_detail`
(
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invoke_record_id`  bigint       NOT NULL COMMENT 'invoke_record表的主键',
  `model_name`        varchar(200) NOT NULL COMMENT '模型名称',
  `cost_token`        bigint       NOT NULL DEFAULT 0 COMMENT '消耗token数',
  `status`            tinyint      NOT NULL COMMENT '状态,0:失败,1:成功',
  `cost_time`         int          NOT NULL COMMENT '耗时,单位:毫秒',
  `fail_reason`       varchar(500) DEFAULT NULL COMMENT '失败原因',
  `start_time`        datetime     NOT NULL DEFAULT current_timestamp() COMMENT '开始时间',
  `end_time`          datetime     NOT NULL DEFAULT current_timestamp() COMMENT '结束时间',
  `user_input`        text         DEFAULT NULL COMMENT '用户输入',
  `assistant_message` text         DEFAULT NULL COMMENT '响应结果',
  PRIMARY KEY (`id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB COMMENT='调用记录明细';

CREATE TABLE IF NOT EXISTS `app`
(
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         bigint       NOT NULL COMMENT 'user表的主键',
  `app_name`        varchar(150) NOT NULL COMMENT '应用名称',
  `app_desc`        varchar(300) NOT NULL COMMENT '应用描述',
  `icon_path`       varchar(300) DEFAULT NULL COMMENT '图标存放路径,格式如:resources/type/20230523/123.jpg',
  `out_lib_enable`  tinyint      NOT NULL DEFAULT 0 COMMENT '超出知识库的问题是否回答,1:是,0:否',
  `lib_id`          bigint       DEFAULT NULL COMMENT 'knowledge_lib表的主键',
  `created_time`    datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time`   datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='应用信息';

CREATE TABLE IF NOT EXISTS `knowledge_lib`
(
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         bigint       NOT NULL COMMENT 'user表的主键',
  `lib_name`        varchar(150) NOT NULL COMMENT '知识库名称',
  `lib_desc`        varchar(300) NOT NULL COMMENT '知识库描述',
  `icon_path`       varchar(300) DEFAULT NULL COMMENT '图标存放路径,格式如:resources/type/20230523/123.jpg',
  `created_time`    datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time`   datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='知识库';

CREATE TABLE IF NOT EXISTS `upload_file`
(
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lib_id`        bigint       NOT NULL COMMENT 'knowledge_lib表的主键',
  `file_name`     varchar(150) NOT NULL COMMENT '文件名称',
  `store_path`    varchar(300) NOT NULL COMMENT '文件存放路径,格式如:resources/type/20230523/123.jpg',
  `char_count`    bigint       NOT NULL DEFAULT 0 COMMENT '字符数',
  `recall_count`  bigint       NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status`        tinyint      NOT NULL COMMENT '状态,0:禁用,1:启用',
  `doc_ids`       text         DEFAULT NULL COMMENT '关联的文档id列表,json数组',
  `created_time`  datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lib_id` (`lib_id`)
) ENGINE=InnoDB COMMENT='文件信息';

CREATE TABLE IF NOT EXISTS `knowledge_lib_experiment`
(
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lib_id`             bigint       NOT NULL COMMENT 'knowledge_lib表的主键',
  `version_name`       varchar(150) DEFAULT NULL COMMENT '实验版本名称',
  `query_text`         varchar(500) NOT NULL COMMENT '调试问题',
  `split_strategy`     varchar(50)  NOT NULL COMMENT '切分策略',
  `raw_hit_count`      int          NOT NULL DEFAULT 0 COMMENT '原始召回数',
  `rerank_hit_count`   int          NOT NULL DEFAULT 0 COMMENT '重排后召回数',
  `diagnosis_title`    varchar(150) NOT NULL COMMENT '诊断结论',
  `category_code`      varchar(50)  NOT NULL COMMENT '建议归类编码',
  `category_label`     varchar(50)  NOT NULL COMMENT '建议归类名称',
  `raw_top_summary`    varchar(500) DEFAULT NULL COMMENT '原始Top1摘要',
  `rerank_top_summary` varchar(500) DEFAULT NULL COMMENT '重排Top1摘要',
  `recommended`        tinyint      NOT NULL DEFAULT 0 COMMENT '是否推荐版本,1:是,0:否',
  `created_time`       datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time`      datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_lib_id` (`lib_id`),
  KEY `idx_recommended` (`recommended`)
) ENGINE=InnoDB COMMENT='知识库实验版本';

CREATE TABLE IF NOT EXISTS `chat`
(
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id`        bigint       NOT NULL COMMENT 'app表的主键',
  `chat_name`     varchar(150) NOT NULL COMMENT '聊天名称',
  `created_time`  datetime     NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `modified_time` datetime     NOT NULL DEFAULT current_timestamp() COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB COMMENT='聊天会话';

CREATE TABLE IF NOT EXISTS `chat_message`
(
  `id`           bigint   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chat_id`      bigint   NOT NULL COMMENT 'chat表的主键',
  `type`         tinyint  NOT NULL COMMENT '消息类型,见ChatMessageTypeEnum',
  `message`      text     NOT NULL COMMENT '消息内容',
  `created_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat_id` (`chat_id`)
) ENGINE=InnoDB COMMENT='聊天消息';
