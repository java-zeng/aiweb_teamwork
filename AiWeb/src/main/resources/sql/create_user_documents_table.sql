-- 创建用户文档表
-- 用于记录用户上传到FastGPT的文档信息

CREATE TABLE IF NOT EXISTS `user_documents` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `dataset_id` varchar(100) NOT NULL COMMENT 'FastGPT数据集ID',
  `fastgpt_document_id` varchar(100) DEFAULT NULL COMMENT 'FastGPT文档ID',
  `original_filename` varchar(255) NOT NULL COMMENT '原始文件名（正确编码的中英文名称）',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(20) DEFAULT NULL COMMENT '文件类型（扩展名）',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` varchar(20) NOT NULL DEFAULT 'uploading' COMMENT '文档状态：uploading-上传中, processing-处理中, completed-已完成, failed-失败',
  `error_message` text DEFAULT NULL COMMENT '错误信息（如果状态为failed）',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_dataset_id` (`dataset_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户上传文档记录表';

-- 为现有用户表添加fastgpt_dataset_id字段（如果还没有的话）
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `fastgpt_dataset_id` varchar(100) DEFAULT NULL COMMENT 'FastGPT数据集ID';
