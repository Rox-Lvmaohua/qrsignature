// 签名页面逻辑
import { SignaturePage } from './app.js';
import { showNotification } from './utils.js';

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;

    if (path === '/signing.html' || path.includes('signing.html') || path === '/signature.html' || path.includes('signature.html')) {
        // 签名页面
        window.signaturePage = new SignaturePage();
    }
});

// 导出全局函数供HTML使用
window.showNotification = showNotification;