-- 创建用户角色知识库绑定关系表
CREATE TABLE IF NOT EXISTS user_role_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    knowledge_base_id VARCHAR(100) NOT NULL COMMENT '知识库ID',
    knowledge_base_name VARCHAR(200) NOT NULL COMMENT '知识库名称',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    INDEX idx_user_id (user_id),
    INDEX idx_knowledge_base_id (knowledge_base_id),
    UNIQUE KEY uk_user_role (user_id, role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色知识库绑定关系表';

-- 创建知识库文件表
CREATE TABLE IF NOT EXISTS knowledge_base_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    knowledge_base_id VARCHAR(100) NOT NULL COMMENT '知识库ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    INDEX idx_knowledge_base_id (knowledge_base_id),
    INDEX idx_file_name (file_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文件表';
