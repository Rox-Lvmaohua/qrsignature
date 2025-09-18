# QR签名系统 - API接口文档

## 📋 接口总览

本项目提供完整的QR码电子签名API接口，支持二维码生成、签署状态轮询、签名确认等功能。

## 🔗 基础信息

- **基础URL**: `http://localhost:29308`
- **Content-Type**: `application/json`
- **响应格式**: JSON

## 📚 接口列表

### 1. 生成签署URL

**接口地址**: `POST /api/sign/url`

**功能描述**: 根据项目信息生成包含JWT token的签署URL

**请求参数**:
```json
{
  "projectId": "project-001",
  "userId": "user-001",
  "fileId": "file-001",
  "metaCode": "META-CODE-001"
}
```

**响应示例**:
```json
{
  "signUrl": "http://localhost:29308/api/sign/eyJhbGciOiJIUzI1NiJ9...",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "status": "未扫描"
}
```

**错误响应**:
```json
{
  "error": "生成签署URL失败",
  "message": "错误详情信息"
}
```

---

### 2. 检查签署状态

**接口地址**: `GET /api/sign/status`

**功能描述**: 根据项目信息查询签署状态，支持轮询

**请求参数**:
- `projectId`: 项目ID (必需)
- `userId`: 用户ID (必需)
- `fileId`: 文件ID (必需)

**响应示例** (未签署):
```json
{
  "projectId": "project-001",
  "userId": "user-001",
  "fileId": "file-001",
  "metaCode": "META-CODE-001",
  "status": "未扫描",
  "signatureBase64": null
}
```

**响应示例** (已签署):
```json
{
  "projectId": "project-001",
  "userId": "user-001",
  "fileId": "file-001",
  "metaCode": "META-CODE-001",
  "status": "已签署",
  "signatureBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**错误响应**:
```json
{
  "error": "检查签署状态失败",
  "message": "签署记录不存在"
}
```

---

### 3. 确认签名

**接口地址**: `POST /api/sign/confirm`

**功能描述**: 提交签名数据，完成签名确认

**请求头**:
- `Authorization`: `Bearer <JWT_TOKEN>`

**请求参数**:
```json
{
  "signatureBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**响应示例**:
```json
{
  "message": "签署成功",
  "status": "已签署",
  "signatureBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "signRecordId": "uuid-string-here"
}
```

**错误响应**:
```json
{
  "error": "认证失败",
  "message": "缺少Bearer Token"
}
```

---

## 🔄 完整流程示例

### 步骤1: 生成签署URL
```bash
curl -X POST http://localhost:29308/api/sign/url \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "project-001",
    "userId": "user-001",
    "fileId": "file-001",
    "metaCode": "META-CODE-001"
  }'
```

### 步骤2: 轮询检查签署状态
```bash
curl -X GET "http://localhost:29308/api/sign/status?projectId=project-001&userId=user-001&fileId=file-001"
```

### 步骤3: 确认签名
```bash
curl -X POST http://localhost:29308/api/sign/confirm \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "signatureBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
  }'
```

## 📊 状态枚举

### 签署状态
- `UNSCANNED` - 未扫描
- `SCANNED_UNCONFIRMED` - 已扫描未签署
- `SIGNED` - 已签署

## 🔐 安全机制

### JWT Token
- 使用HS256算法签名
- 15分钟过期时间
- 包含项目信息（projectId, userId, fileId, metaCode）

### Redis缓存
- 缓存key: JWT token
- 缓存value: 项目信息映射
- TTL: 15分钟
- 签名完成后自动清除

## 🛠️ 技术细节

### 数据库
- **数据库**: SQLite
- **表名**: sign_record
- **主键**: UUID字符串
- **审计字段**: createTime, updateTime

### 缓存配置
- **Redis**: 10.219.24.10:6379
- **数据库**: 8
- **连接池**: 最大8个连接

### 应用配置
- **端口**: 29308
- **主机**: localhost
- **JWT密钥**: 配置在application.yml中

## 📝 错误码说明

### 常见错误
1. **生成URL失败**
   - 原因: 参数缺失或数据库错误
   - 解决: 检查请求参数和数据库连接

2. **Token验证失败**
   - 原因: Token无效或已过期
   - 解决: 重新生成Token

3. **签名确认失败**
   - 原因: Token过期或重复签名
   - 解决: 重新获取有效Token

## 🚀 部署说明

### 环境要求
- Java 17+
- Redis服务器
- SQLite数据库（自动创建）

### 启动命令
```bash
mvn spring-boot:run
```

### 访问地址
- 主页: http://localhost:29308
- API文档: 本文件

## 🔄 版本信息

- **版本**: 1.0.0
- **最后更新**: 2024年
- **Spring Boot**: 3.2.0
- **Java**: 17+

---

## 📞 技术支持

如有问题请查看：
- 项目源码中的CLAUDE.md文件
- 开发计划文档plan.md
- 应用日志输出