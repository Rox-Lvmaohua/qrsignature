// 主页面逻辑
import { QrSignatureApp } from './app.js';
import { showNotification } from './utils.js';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;

    if (path === '/' || path === '/index.html') {
        // 主页面
        window.qrSignatureApp = new QrSignatureApp();
    }
});

// 导出全局函数供HTML使用
window.showNotification = showNotification;