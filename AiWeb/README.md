# AiWeb 后端服务

## 项目简介

AiWeb 是一个基于 Spring Boot 的后端服务，集成了 FastGPT 知识库功能，支持用户文件上传、自动创建知识库、文件管理等功能。

## 核心功能

### 1. 用户认证系统
- 用户注册、登录、密码重置
- JWT Token 认证
- 用户注销时自动清理 FastGPT 数据集

### 2. FastGPT 集成
- 自动为用户创建专属 FastGPT 数据集
- 文件上传到 FastGPT 知识库
- 支持中英文文件名（解决编码问题）
- 文件与知识库深度绑定

### 3. 文件管理
- 查看用户上传的文件列表
- 删除文件时同步删除 FastGPT 中的对应文档
- 文件状态跟踪（上传中、处理中、已完成、失败）

## API 接口文档

### 认证相关接口

#### 1. 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "用户名",
  "password": "密码",
  "email": "邮箱地址"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

#### 2. 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "用户名",
  "password": "密码"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "用户名",
      "email": "邮箱地址"
    }
  }
}
```

#### 3. 用户注销
```http
POST /api/auth/delete_me
Authorization: Bearer <token>
```

**功能说明：** 删除用户账户及其关联的 FastGPT 数据集

### FastGPT 文件管理接口

#### 1. 自动上传文件
```http
POST /api/v1/collections/auto-upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

参数：
- file: 文件（必需）
- parentId: 父级ID（可选）
- chunkSize: 分块大小（可选）
```

**功能说明：**
- 自动为用户创建 FastGPT 数据集（如果不存在）
- 解决中英文文件名编码问题
- 记录文件信息到数据库
- 返回 FastGPT 处理结果

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "id": "document_id",
    "name": "文件名.pdf",
    "status": "completed"
  }
}
```

#### 2. 获取用户文件列表
```http
GET /api/v1/collections/documents
Authorization: Bearer <token>
```

**响应示例：**
```json
[
  {
    "id": 1,
    "username": "用户名",
    "datasetId": "dataset_123",
    "fastgptDocumentId": "doc_456",
    "originalFilename": "测试文档.pdf",
    "fileSize": 1024000,
    "fileType": "pdf",
    "uploadTime": "2025-01-14T10:30:00",
    "status": "completed",
    "errorMessage": null
  }
]
```

#### 3. 删除文件
```http
DELETE /api/v1/collections/documents/{documentId}
Authorization: Bearer <token>
```

**功能说明：**
- 删除数据库中的文件记录
- 同步删除 FastGPT 中的对应文档
- 验证用户权限

**响应示例：**
```json
{
  "code": 200,
  "message": "文档删除成功"
}
```

## 数据库表结构

### users 表
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键ID |
| username | varchar(50) | 用户名 |
| password | varchar(255) | 密码（加密） |
| email | varchar(100) | 邮箱地址 |
| fastgpt_dataset_id | varchar(100) | FastGPT数据集ID |

### user_documents 表
| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键ID |
| user_id | bigint | 用户ID |
| username | varchar(50) | 用户名 |
| dataset_id | varchar(100) | FastGPT数据集ID |
| fastgpt_document_id | varchar(100) | FastGPT文档ID |
| original_filename | varchar(255) | 原始文件名 |
| file_size | bigint | 文件大小（字节） |
| file_type | varchar(20) | 文件类型 |
| upload_time | datetime | 上传时间 |
| status | varchar(20) | 文档状态 |
| error_message | text | 错误信息 |

## 部署说明

### 1. 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### 2. 数据库初始化
执行 SQL 脚本创建必要的表：
```sql
-- 执行 src/main/resources/sql/create_user_documents_table.sql
```

### 3. 配置文件
修改 `application.yml` 中的配置：
```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://your-db-host:3306/aiweb
    username: your-username
    password: your-password

# FastGPT 配置
fastgpt:
  api:
    baseUrl: http://your-fastgpt-host:3001
    key: your-fastgpt-api-key
```

### 4. 启动服务
```bash
mvn clean compile
mvn spring-boot:run
```

## 前端集成指南

### 1. 认证流程
```javascript
// 登录后保存 token
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});
const { data } = await loginResponse.json();
localStorage.setItem('token', data.token);
```

### 2. 文件上传
```javascript
// 自动获取 token 并上传文件
const token = localStorage.getItem('token');
const formData = new FormData();
formData.append('file', file);

const response = await fetch('/api/v1/collections/auto-upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});
```

### 3. 文件列表
```javascript
// 获取用户文件列表
const token = localStorage.getItem('token');
const response = await fetch('/api/v1/collections/documents', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const documents = await response.json();
```

### 4. 删除文件
```javascript
// 删除文件
const token = localStorage.getItem('token');
const response = await fetch(`/api/v1/collections/documents/${documentId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
```

## 常见问题

### 1. 文件名乱码问题
**问题：** 上传的中英文文件名显示为乱码
**解决方案：** 已通过 UTF-8 编码处理解决，确保文件名正确显示

### 2. JWT Token 认证
**问题：** 请求时提示未认证
**解决方案：** 确保请求头格式正确：`Authorization: Bearer <token>`

### 3. FastGPT 数据集创建失败
**问题：** 创建数据集时返回错误
**解决方案：** 检查 FastGPT 服务是否正常运行，API Key 是否正确

## 技术栈

- **后端框架：** Spring Boot 3.x
- **数据库：** MySQL 8.0
- **ORM：** MyBatis Plus
- **认证：** JWT
- **文件处理：** Spring MultipartFile
- **HTTP客户端：** RestTemplate

## 更新日志

### v1.1.0 (2025-01-14)
- ✅ 修复文件名编码问题
- ✅ 新增用户文档管理功能
- ✅ 实现文件与知识库深度绑定
- ✅ 新增文件列表和删除接口
- ✅ 完善错误处理和日志记录

### v1.0.0 (2025-01-13)
- ✅ 基础用户认证系统
- ✅ FastGPT 集成
- ✅ 自动数据集创建
- ✅ 文件上传功能
