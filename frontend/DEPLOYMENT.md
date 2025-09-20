# QR签名系统前端部署指南

## 部署方式

### 选项1: 独立静态文件服务器 (推荐)

这种方式将前端和后端完全分离，独立部署和扩展。

#### 1. 构建前端项目
```bash
cd frontend
npm install
npm run build
```

#### 2. 部署到Nginx
```nginx
# /etc/nginx/sites-available/qrsignature
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /var/www/qrsignature/frontend/dist;
    index index.html;

    # 前端路由支持 (SPA)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理到后端Spring Boot服务
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # 静态资源缓存优化
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header Vary Accept-Encoding;
    }

    # HTML文件不缓存
    location ~* \.html$ {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        add_header Pragma "no-cache";
        add_header Expires "0";
    }

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;
}
```

#### 3. 部署后端Spring Boot服务
```bash
# 构建Spring Boot项目
cd backend
mvn clean package -DskipTests

# 运行服务
java -jar target/qrsignature-1.0.0.jar --spring.profiles.active=prod
```

#### 4. 系统服务配置 (可选)
```bash
# 创建systemd服务
sudo tee /etc/systemd/system/qrsignature-backend.service > /dev/null <<EOF
[Unit]
Description=QR Signature Backend Service
After=network.target

[Service]
Type=simple
User=qrsignature
WorkingDirectory=/opt/qrsignature/backend
ExecStart=/usr/bin/java -jar target/qrsignature-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable qrsignature-backend
sudo systemctl start qrsignature-backend
```

### 选项2: 集成到Spring Boot项目

这种方式将前端构建文件集成到Spring Boot项目中，统一部署。

#### 1. 构建前端项目
```bash
cd frontend
npm install
npm run build
```

#### 2. 复制构建文件到Spring Boot项目
```bash
# 创建静态资源目录
mkdir -p ../src/main/resources/static

# 复制构建文件
cp -r dist/* ../src/main/resources/static/
```

#### 3. 修改Spring Boot配置
```yaml
# src/main/resources/application.yml
spring:
  web:
    resources:
      static-locations:
        - classpath:/static/
        - classpath:/public/
      add-mappings: true

  # 生产环境配置
  mvc:
    throw-exception-if-no-handler-found: true
  web:
    resources:
      add-mappings: false

server:
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: 1024
```

#### 4. 添加CORS配置
```java
// src/main/java/com/qrsignature/config/WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
```

#### 5. 构建和部署
```bash
# 构建整个项目
mvn clean package -DskipTests

# 运行
java -jar target/qrsignature-1.0.0.jar
```

### 选项3: Docker容器化部署

使用Docker进行容器化部署，便于环境一致性和扩展。

#### 1. 前端Dockerfile
```dockerfile
# frontend/Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### 2. 后端Dockerfile
```dockerfile
# backend/Dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app
COPY target/qrsignature-1.0.0.jar app.jar

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]
```

#### 3. Docker Compose
```yaml
# docker-compose.yml
version: '3.8'

services:
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - qrsignature-network

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/data/qrsignature.db
    volumes:
      - ./data:/data
    networks:
      - qrsignature-network

networks:
  qrsignature-network:
    driver: bridge

volumes:
  qrsignature-data:
```

#### 4. 部署命令
```bash
# 构建和启动
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 选项4: 云平台部署

#### 1. AWS部署
```bash
# 使用S3托管静态文件
aws s3 sync ./dist s3://your-bucket-name --delete

# 使用CloudFront CDN
aws cloudfront create-distribution --origin-domain-name your-bucket-name.s3.amazonaws.com

# 使用ECS部署后端
# 创建任务定义和服务
```

#### 2. 部署脚本示例
```bash
#!/bin/bash
# deploy.sh

set -e

echo "开始部署 QR签名系统..."

# 1. 构建前端
echo "构建前端..."
cd frontend
npm install
npm run build
cd ..

# 2. 构建后端
echo "构建后端..."
mvn clean package -DskipTests

# 3. 备份当前版本
echo "备份当前版本..."
if [ -d "/var/www/qrsignature/backup" ]; then
    rm -rf /var/www/qrsignature/backup/*
fi
mkdir -p /var/www/qrsignature/backup
cp -r /var/www/qrsignature/frontend/dist /var/www/qrsignature/backup/
cp /var/www/qrsignature/backend/target/qrsignature-*.jar /var/www/qrsignature/backup/

# 4. 部署新版本
echo "部署新版本..."
# 停止服务
systemctl stop qrsignature-backend

# 部署前端
cp -r frontend/dist/* /var/www/qrsignature/frontend/

# 部署后端
cp backend/target/qrsignature-*.jar /var/www/qrsignature/backend/

# 启动服务
systemctl start qrsignature-backend

# 5. 验证部署
echo "验证部署..."
sleep 10
if curl -f http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ 部署成功！"
else
    echo "❌ 部署失败，正在回滚..."
    # 回滚逻辑
    systemctl stop qrsignature-backend
    cp /var/www/qrsignature/backup/qrsignature-*.jar /var/www/qrsignature/backend/
    systemctl start qrsignature-backend
    echo "已回滚到上一个版本"
fi

echo "部署完成！"
```

## 开发环境配置

### 启动开发服务器

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 后端API代理

开发服务器会自动代理API请求到 `http://localhost:8080`

## 环境配置

### 开发环境 (.env.development)
```
API_BASE_URL=http://localhost:8080/api
NODE_ENV=development
```

### 生产环境 (.env.production)
```
API_BASE_URL=https://your-api-domain.com/api
NODE_ENV=production
```

## 构建优化

### 生产构建
```bash
npm run build
```

### 开发构建
```bash
npm run build:dev
```

### 清理构建文件
```bash
npm run clean
```

## 性能优化

### 代码分割
Webpack会自动进行代码分割，生成以下文件：
- `main.[hash].js` - 主页面代码
- `signature.[hash].js` - 签名页面代码
- `vendors.[hash].js` - 第三方库
- `common.[hash].js` - 公共代码

### 缓存策略
- JavaScript文件: 1年缓存
- CSS文件: 1年缓存
- 图片资源: 1年缓存
- HTML文件: 无缓存

### 压缩优化
- JavaScript代码压缩
- CSS代码压缩
- HTML代码压缩
- 图片资源优化

## 监控和日志

### 错误监控
```javascript
// 在utils.js中添加错误监控
window.addEventListener('error', (event) => {
    console.error('Frontend Error:', event.error);
    // 可以发送到错误监控服务
});

window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled Promise Rejection:', event.reason);
    // 可以发送到错误监控服务
});
```

### 性能监控
```javascript
// 页面加载性能监控
window.addEventListener('load', () => {
    const navigation = performance.getEntriesByType('navigation')[0];
    console.log('Page Load Time:', navigation.loadEventEnd - navigation.startTime);
});
```

## 故障排除

### 常见问题

1. **构建失败**
   ```bash
   npm run clean
   npm install
   npm run build
   ```

2. **静态资源404**
   - 检查Nginx配置中的root路径
   - 确保文件权限正确

3. **API请求失败**
   - 检查后端服务是否运行
   - 确认CORS配置正确
   - 验证API URL配置

4. **页面白屏**
   - 检查JavaScript文件是否正确加载
   - 查看浏览器控制台错误信息

### 调试技巧

1. **启用源码映射**
   ```javascript
   // 在webpack.config.js中
   devtool: 'source-map'
   ```

2. **网络请求调试**
   - 使用浏览器开发者工具的Network面板
   - 检查API请求和响应

3. **控制台调试**
   - 添加console.log调试信息
   - 使用断点调试

## 安全考虑

1. **XSS防护**
   - 使用textContent而非innerHTML
   - 对用户输入进行验证和清理

2. **CSRF防护**
   - 后端API需要CSRF token
   - 使用SameSite cookie属性

3. **HTTPS**
   - 生产环境必须使用HTTPS
   - 配置SSL证书

## 备份和恢复

### 备份策略
- 定期备份构建文件
- 保留多个版本的构建文件
- 备份配置文件

### 恢复流程
1. 从备份恢复文件
2. 重启Web服务器
3. 验证功能正常

## 版本管理

### 版本号格式
使用语义化版本号：`主版本号.次版本号.修订号`

### 发布流程
1. 更新版本号
2. 创建新的构建
3. 备份当前版本
4. 部署新版本
5. 验证功能正常