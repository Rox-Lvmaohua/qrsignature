# QR签名系统前端

## 项目介绍

这是QR签名系统的前端部分，使用现代前端技术栈构建，支持模块化开发和独立部署。

## 技术栈

- **构建工具**: Webpack 5
- **模块系统**: ES6 Modules
- **代码转换**: Babel
- **样式处理**: CSS + MiniCssExtractPlugin
- **开发服务器**: Webpack Dev Server
- **代码规范**: ESLint + Standard

## 功能特性

- 📱 响应式设计，支持移动端
- 🔐 QR码生成和扫描
- ✋ 手写签名功能
- 💾 签名保存和历史管理
- 🔄 实时状态监控
- 🎨 现代化UI设计

## 快速开始

### 安装依赖

```bash
cd frontend
npm install
```

### 开发环境

```bash
npm run dev
```

启动开发服务器，访问 http://localhost:3000

### 生产构建

```bash
npm run build
```

构建完成后，文件会输出到 `dist/` 目录。

### 代码检查

```bash
npm run lint
```

## 项目结构

```
frontend/
├── public/                 # 静态文件
│   ├── index.html         # 主页面
│   └── sign.html          # 签名页面
├── src/
│   ├── js/               # JavaScript源码
│   │   ├── app.js        # 核心业务逻辑
│   │   ├── main.js       # 主页面入口
│   │   ├── signature.js  # 签名页面入口
│   │   ├── config.js     # 配置文件
│   │   └── utils.js      # 工具函数
│   ├── styles/           # CSS样式
│   │   ├── common.css    # 通用样式
│   │   ├── main.css      # 主页面样式
│   │   └── signature.css # 签名页面样式
│   └── assets/           # 静态资源
├── dist/                  # 构建输出
└── package.json          # 项目配置
```

## 环境配置

项目支持多环境配置：

- **开发环境**: `npm run dev`
- **生产环境**: `npm run build`

## 部署

### 静态文件部署

构建后的文件可以直接部署到任何静态文件服务器：

```bash
npm run build
# 将 dist/ 目录下的文件部署到服务器
```

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /path/to/dist;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理到后端
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

## 开发说明

### 模块化架构

- **app.js**: 包含核心业务逻辑类（QrSignatureApp, SignaturePage）
- **main.js**: 主页面入口文件
- **signature.js**: 签名页面入口文件
- **config.js**: 应用配置
- **utils.js**: 通用工具函数

### 样式系统

- **common.css**: 通用样式和组件样式
- **main.css**: 主页面专用样式
- **signature.css**: 签名页面专用样式

### API集成

前端通过以下API与后端交互：

- `POST /api/sign/url` - 生成签署URL
- `GET /api/sign/status` - 查询签署状态
- `POST /api/sign/confirm` - 确认签名
- `GET /api/sign/user-signatures` - 获取用户历史签名
- `GET /api/sign/check-signature-exists` - 检查签名保存权限

## 故障排除

### 常见问题

1. **模块导入错误**
   ```bash
   npm install
   # 确保所有依赖都正确安装
   ```

2. **构建失败**
   ```bash
   npm run clean
   npm run build
   # 清理后重新构建
   ```

3. **开发服务器无法启动**
   ```bash
   # 检查端口是否被占用
   netstat -an | grep 3000
   ```

4. **API请求失败**
   - 确保后端服务运行在8080端口
   - 检查CORS配置
   - 查看浏览器开发者工具的网络面板

### 调试技巧

1. 开启源码映射：
   ```javascript
   // webpack.config.js中设置
   devtool: 'source-map'
   ```

2. 使用浏览器开发者工具调试：
   - Network面板：查看API请求
   - Console面板：查看错误信息
   - Sources面板：调试JavaScript代码

## 贡献指南

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 许可证

MIT License