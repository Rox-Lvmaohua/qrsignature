# QR签名系统前端本地开发指南

## 🚀 快速启动

### 前置条件
- Node.js 16+
- npm 或 yarn
- Java 17+ (用于后端)

### 启动步骤

#### 1. 安装依赖
```bash
cd frontend
npm install
```

#### 2. 启动开发服务器
```bash
npm run dev
```
如果3000端口被占用，可以指定其他端口：
```bash
npx webpack serve --mode development --port 3001
```

#### 3. 访问应用
打开浏览器访问: http://localhost:3000

#### 4. 启动后端服务 (另开终端)
```bash
# 在项目根目录
mvn spring-boot:run
```

现在你可以：
- 访问 http://localhost:3000 使用前端页面
- 前端会自动代理 API 请求到 http://localhost:8080
- 支持热重载，修改代码后会自动刷新

## ✨ 新特性

### Bootstrap 5 集成
- 现代化的UI设计
- 响应式布局，支持移动端
- 美观的卡片式布局
- 渐变背景和毛玻璃效果

### 增强的功能
- **二维码优化**: 更清晰的显示效果，支持直接跳转
- **通知系统**: 使用Bootstrap样式的通知，支持手动关闭
- **API代理**: 自动代理后端请求，支持开发调试
- **状态监控**: 实时状态显示，使用Bootstrap组件

### 界面优化
- 主页面：卡片式布局，美观的表单设计
- 签名页面：专业的签名界面，支持历史签名选择
- 响应式设计：完美适配手机和平板

## 📋 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 构建开发版本
npm run build:dev

# 代码检查
npm run lint

# 自动修复代码
npm run lint -- --fix

# 清理构建文件
npm run clean

# 直接启动 (自动安装依赖)
npm start
```

## 🔧 开发环境特性

### 热重载
- 修改 JavaScript 文件：自动刷新
- 修改 CSS 文件：样式热更新
- 修改 HTML 文件：自动刷新

### API 代理
开发服务器会自动代理以下请求：
- `/api/*` → `http://localhost:8080/api/*`

### 源码映射
- 支持断点调试
- 可以在浏览器中查看原始源码

## 🐛 常见问题

### 1. 端口冲突
如果 3000 端口被占用：
```bash
# 修改端口
npx webpack serve --port 3001
```

### 2. 依赖安装失败
```bash
# 清理缓存重新安装
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 3. 后端连接失败
确保后端服务在 8080 端口运行：
```bash
# 在项目根目录
mvn spring-boot:run
```

### 4. 模块导入错误
```bash
# 检查浏览器控制台
# 确保 import 路径正确
```

## 📝 开发工作流

### 1. 修改代码
```bash
# 编辑 src/js/ 下的文件
# 编辑 src/styles/ 下的文件
# 编辑 public/ 下的 HTML 文件
```

### 2. 实时预览
- 保存文件后浏览器会自动刷新
- 查看控制台输出和错误信息

### 3. 调试
```javascript
// 在代码中添加调试信息
console.log('Debug info:', data);

// 使用断点
debugger;
```

## 🔍 调试技巧

### 1. 浏览器开发者工具
- Network 标签：查看 API 请求
- Console 标签：查看错误信息
- Sources 标签：调试 JavaScript 代码
- Application 标签：查看本地存储

### 2. API 请求调试
```javascript
// 在 utils.js 中添加请求拦截器
console.log('API Request:', url, options);
```

### 3. 状态调试
```javascript
// 在 app.js 中添加状态日志
console.log('App State:', this.currentToken, this.signRecordId);
```

## 🎯 测试功能

### 1. 测试签名生成
1. 访问 http://localhost:3000
2. 填写表单信息
3. 点击"生成签署页面"
4. 查看生成的二维码

### 2. 测试签名功能
1. 扫描二维码或点击链接
2. 在签名页面进行签名
3. 测试保存签名功能
4. 验证历史签名功能

### 3. 测试API集成
- 查看浏览器 Network 面板
- 验证 API 请求和响应
- 检查错误处理

## 🎨 界面特色

### 设计理念
- **现代化**: 使用 Bootstrap 5 和渐变设计
- **专业性**: 卡片式布局，清晰的视觉层次
- **易用性**: 直观的操作流程
- **响应式**: 完美适配各种设备

### 颜色方案
- **主色调**: 蓝紫色渐变 (#667eea → #764ba2)
- **状态色**: Bootstrap 标准色系
- **背景色**: 半透明白色卡片
- **文字色**: 深色文字，保证可读性

## 💡 提示

- 前端和后端需要同时运行
- 修改代码后无需手动刷新浏览器
- 遇到问题时先查看浏览器控制台
- 使用 npm run lint 检查代码质量
- 二维码支持直接扫描跳转

## 📋 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 构建开发版本
npm run build:dev

# 代码检查
npm run lint

# 自动修复代码
npm run lint -- --fix

# 清理构建文件
npm run clean

# 直接启动 (自动安装依赖)
npm start
```

## 🔧 开发环境特性

### 热重载
- 修改 JavaScript 文件：自动刷新
- 修改 CSS 文件：样式热更新
- 修改 HTML 文件：自动刷新

### API 代理
开发服务器会自动代理以下请求：
- `/api/*` → `http://localhost:8080/api/*`

### 源码映射
- 支持断点调试
- 可以在浏览器中查看原始源码

## 🐛 常见问题

### 1. 端口冲突
如果 3000 端口被占用：
```bash
# 修改端口
npx webpack serve --port 3001
```

### 2. 依赖安装失败
```bash
# 清理缓存重新安装
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 3. 后端连接失败
确保后端服务在 8080 端口运行：
```bash
# 在项目根目录
mvn spring-boot:run
```

### 4. 模块导入错误
```bash
# 检查浏览器控制台
# 确保 import 路径正确
```

## 📝 开发工作流

### 1. 修改代码
```bash
# 编辑 src/js/ 下的文件
# 编辑 src/styles/ 下的文件
# 编辑 public/ 下的 HTML 文件
```

### 2. 实时预览
- 保存文件后浏览器会自动刷新
- 查看控制台输出和错误信息

### 3. 调试
```javascript
// 在代码中添加调试信息
console.log('Debug info:', data);

// 使用断点
debugger;
```

## 🔍 调试技巧

### 1. 浏览器开发者工具
- Network 标签：查看 API 请求
- Console 标签：查看错误信息
- Sources 标签：调试 JavaScript 代码
- Application 标签：查看本地存储

### 2. API 请求调试
```javascript
// 在 utils.js 中添加请求拦截器
console.log('API Request:', url, options);
```

### 3. 状态调试
```javascript
// 在 app.js 中添加状态日志
console.log('App State:', this.currentToken, this.signRecordId);
```

## 🎯 开发环境配置

### 自定义配置
```javascript
// src/js/config.js
export const API_CONFIG = {
    BASE_URL: 'http://localhost:8080/api/sign', // 开发环境
    TIMEOUT: 30000,
    RETRY_COUNT: 3
};
```

### 环境变量
创建 `.env.local` 文件：
```bash
API_BASE_URL=http://localhost:8080/api
NODE_ENV=development
```

## 🚨 测试功能

### 1. 测试签名生成
1. 访问 http://localhost:3000
2. 填写表单信息
3. 点击"生成签署页面"
4. 查看生成的二维码

### 2. 测试签名功能
1. 扫描二维码或点击链接
2. 在签名页面进行签名
3. 测试保存签名功能
4. 验证历史签名功能

### 3. 测试API集成
- 查看浏览器 Network 面板
- 验证 API 请求和响应
- 检查错误处理

## 💡 提示

- 前端和后端需要同时运行
- 修改代码后无需手动刷新浏览器
- 遇到问题时先查看浏览器控制台
- 使用 npm run lint 检查代码质量