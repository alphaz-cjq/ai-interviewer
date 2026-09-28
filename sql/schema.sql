-- ============================================
-- AI 智能面试官系统 — 数据库建表脚本
-- 数据库名: ai_interviewer
-- 使用方法: 复制到 Navicat/DataGrip 中执行
-- ============================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS ai_interviewer
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_interviewer;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密后）',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`        VARCHAR(20)  DEFAULT 'CANDIDATE' COMMENT '角色: CANDIDATE/INTERVIEWER/ADMIN',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 简历表
-- ============================================
CREATE TABLE IF NOT EXISTS `resume` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '简历ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `name`        VARCHAR(200) NOT NULL COMMENT '简历名称',
    `file_path`   VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_type`   VARCHAR(20)  NOT NULL COMMENT '文件类型: pdf/docx/txt',
    `parsed_text` LONGTEXT     DEFAULT NULL COMMENT 'Tika解析后的纯文本',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';

-- ============================================
-- 岗位表（JD）
-- ============================================
CREATE TABLE IF NOT EXISTS `job_position` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
    `title`        VARCHAR(100) NOT NULL COMMENT '岗位名称',
    `description`  TEXT         DEFAULT NULL COMMENT '岗位描述',
    `requirements` TEXT         DEFAULT NULL COMMENT '任职要求',
    `skills`       VARCHAR(500) DEFAULT NULL COMMENT '技能要求，逗号分隔',
    `status`       TINYINT      DEFAULT 1 COMMENT '状态: 1=开放 0=关闭',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- ============================================
-- 面试记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `interview` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '面试ID',
    `user_id`     BIGINT       NOT NULL COMMENT '候选人用户ID',
    `job_id`      BIGINT       NOT NULL COMMENT '面试岗位ID',
    `resume_id`   BIGINT       NOT NULL COMMENT '使用的简历ID',
    `status`      VARCHAR(20)  DEFAULT 'IN_PROGRESS' COMMENT '面试状态',
    `total_score` INT          DEFAULT NULL COMMENT '总分',
    `summary`     TEXT         DEFAULT NULL COMMENT 'AI生成的面评总结',
    `start_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `end_time`    DATETIME     DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试记录表';

-- ============================================
-- 面试题目记录表（一次面试中的每一道题）
-- ============================================
CREATE TABLE IF NOT EXISTS `interview_question` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '题目记录ID',
    `interview_id`  BIGINT      NOT NULL COMMENT '面试ID',
    `question_text` TEXT        NOT NULL COMMENT '题目内容',
    `answer_text`   TEXT        DEFAULT NULL COMMENT '候选人的回答',
    `question_type` VARCHAR(30) DEFAULT 'TECHNICAL' COMMENT '题型',
    `question_num`  INT         DEFAULT 1 COMMENT '第几题',
    `score`         INT         DEFAULT NULL COMMENT '该题得分(0-10)',
    `comment`       TEXT        DEFAULT NULL COMMENT 'AI对该题的评价',
    PRIMARY KEY (`id`),
    INDEX `idx_interview_id` (`interview_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目记录表';

-- ============================================
-- 题库表
-- ============================================
CREATE TABLE IF NOT EXISTS `question_bank` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `job_id`        BIGINT       DEFAULT NULL COMMENT '关联岗位（NULL=通用题）',
    `question_text` TEXT         NOT NULL COMMENT '题目内容',
    `question_type` VARCHAR(30)  DEFAULT 'TECHNICAL' COMMENT '题型',
    `difficulty`    VARCHAR(10)  DEFAULT 'MEDIUM' COMMENT '难度: EASY/MEDIUM/HARD',
    `skill_tag`     VARCHAR(100) DEFAULT NULL COMMENT '技能标签',
    `ref_answer`    TEXT         DEFAULT NULL COMMENT '参考答案',
    PRIMARY KEY (`id`),
    INDEX `idx_skill_tag` (`skill_tag`),
    INDEX `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- ============================================
-- 技能标签表
-- ============================================
CREATE TABLE IF NOT EXISTS `skill_tag` (
    `id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `tag_name`  VARCHAR(100) NOT NULL COMMENT '标签名',
    `category`  VARCHAR(50)  DEFAULT NULL COMMENT '分类: BACKEND/FRONTEND/DATABASE/DEVOPS/SOFT_SKILL',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能标签表';

-- ============================================
-- 面试报告表
-- ============================================
CREATE TABLE IF NOT EXISTS `interview_report` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    `interview_id`  BIGINT   NOT NULL COMMENT '面试ID',
    `total_score`   INT      DEFAULT NULL COMMENT '总分',
    `dimensions`    JSON     DEFAULT NULL COMMENT '各维度得分（JSON）',
    `strengths`     TEXT     DEFAULT NULL COMMENT '优势分析',
    `weaknesses`    TEXT     DEFAULT NULL COMMENT '不足之处',
    `suggestion`    TEXT     DEFAULT NULL COMMENT '改进建议',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_id` (`interview_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试报告表';

-- ============================================
-- 插入示例数据（可选）
-- ============================================

-- 插入一个测试岗位
INSERT INTO `job_position` (`title`, `description`, `requirements`, `skills`)
VALUES (
    'Java后端开发工程师（校招）',
    '负责公司核心业务系统的后端开发和维护，参与系统架构设计',
    '1. 计算机相关专业本科及以上学历\n2. 熟悉Java编程语言，了解JVM原理\n3. 掌握Spring Boot、MyBatis等主流框架\n4. 熟悉MySQL、Redis等数据库\n5. 有项目经验者优先',
    'Java,Spring Boot,MySQL,Redis,数据结构,JVM'
);

-- 插入几个技能标签
INSERT INTO `skill_tag` (`tag_name`, `category`) VALUES
('Java基础', 'BACKEND'),
('Spring Boot', 'BACKEND'),
('MySQL', 'DATABASE'),
('Redis', 'DATABASE'),
('JVM', 'BACKEND'),
('数据结构', 'BACKEND'),
('计算机网络', 'BACKEND'),
('操作系统', 'BACKEND'),
('系统设计', 'BACKEND'),
('沟通表达', 'SOFT_SKILL');

-- 插入几道示例题目
INSERT INTO `question_bank` (`question_text`, `question_type`, `difficulty`, `skill_tag`, `ref_answer`)
VALUES
('请解释Java中HashMap的底层实现原理', 'TECHNICAL', 'MEDIUM', 'Java基础',
 'HashMap基于数组+链表+红黑树实现。通过key的hashCode计算数组下标，...'),
('Spring Boot的自动配置原理是什么？', 'TECHNICAL', 'MEDIUM', 'Spring Boot',
 '@SpringBootApplication包含@EnableAutoConfiguration，它会...'),
('请设计一个秒杀系统的架构', 'TECHNICAL', 'HARD', '系统设计',
 '需要考虑：前端限流、CDN、Nginx负载均衡、Redis预减库存、消息队列异步下单、...'),
('请介绍一下你做过的最有挑战性的项目', 'BEHAVIORAL', 'EASY', '沟通表达',
 'STAR法则回答：情境(Situation)、任务(Task)、行动(Action)、结果(Result)');
