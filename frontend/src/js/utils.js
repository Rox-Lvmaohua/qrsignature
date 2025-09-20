// 工具函数
import { APP_CONFIG } from './config.js';

/**
 * 显示通知消息
 * @param {string} message 消息内容
 * @param {string} type 消息类型 (success, error, warning, info)
 */
export function showNotification(message, type = 'info') {
    // 移除已存在的通知
    const existingNotification = document.getElementById('notification');
    if (existingNotification) {
        existingNotification.remove();
    }

    const notification = document.createElement('div');
    notification.id = 'notification';

    let alertClass = 'alert-info';
    let icon = 'bi-info-circle';

    switch(type) {
        case 'success':
            alertClass = 'alert-success';
            icon = 'bi-check-circle';
            break;
        case 'error':
            alertClass = 'alert-danger';
            icon = 'bi-x-circle';
            break;
        case 'warning':
            alertClass = 'alert-warning';
            icon = 'bi-exclamation-triangle';
            break;
        default:
            alertClass = 'alert-info';
            icon = 'bi-info-circle';
    }

    notification.className = `alert ${alertClass} position-fixed top-0 end-0 m-3`;
    notification.style.cssText = `
        z-index: 9999;
        min-width: 300px;
        animation: slideInRight 0.3s ease;
    `;

    notification.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="bi ${icon} me-2"></i>
            <div>${message}</div>
            <button type="button" class="btn-close ms-auto" onclick="this.parentElement.parentElement.remove()"></button>
        </div>
    `;

    document.body.appendChild(notification);

    // 自动消失
    setTimeout(() => {
        if (document.body.contains(notification)) {
            notification.style.animation = 'slideOutRight 0.3s ease';
            setTimeout(() => {
                if (document.body.contains(notification)) {
                    notification.remove();
                }
            }, 300);
        }
    }, APP_CONFIG.NOTIFICATION_DURATION);
}

/**
 * 检查Canvas是否为空
 * @param {HTMLCanvasElement} canvas
 * @returns {boolean}
 */
export function isCanvasEmpty(canvas) {
    if (!canvas) return true;

    const ctx = canvas.getContext('2d');
    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    return imageData.data.every(pixel => pixel === 0);
}

/**
 * 格式化日期
 * @param {string} dateString
 * @returns {string}
 */
export function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN');
}

/**
 * 复制文本到剪贴板
 * @param {string} text
 * @returns {Promise<void>}
 */
export async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        showNotification('已复制到剪贴板', 'success');
    } catch (error) {
        // 降级处理
        const textarea = document.createElement('textarea');
        textarea.value = text;
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
        showNotification('已复制到剪贴板', 'success');
    }
}

/**
 * 防抖函数
 * @param {Function} func
 * @param {number} wait
 * @returns {Function}
 */
export function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * 节流函数
 * @param {Function} func
 * @param {number} wait
 * @returns {Function}
 */
export function throttle(func, wait) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, wait);
        }
    };
}

/**
 * 验证Base64字符串
 * @param {string} base64String
 * @returns {boolean}
 */
export function isValidBase64(base64String) {
    if (typeof base64String !== 'string') return false;

    try {
        const base64Regex = /^data:image\/(png|jpeg|jpg|gif);base64,([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$/;
        return base64Regex.test(base64String);
    } catch (error) {
        return false;
    }
}

/**
 * 获取URL参数
 * @param {string} name
 * @returns {string|null}
 */
export function getUrlParameter(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

/**
 * 显示加载状态
 * @param {HTMLElement} element
 * @param {boolean} show
 * @param {string} text
 */
export function showLoading(element, show = true, text = '加载中...') {
    if (!element) return;

    if (show) {
        element.innerHTML = `
            <div class="loading-spinner">
                <div class="spinner"></div>
                <span>${text}</span>
            </div>
        `;
        element.disabled = true;
    } else {
        element.innerHTML = element.dataset.originalText || element.textContent;
        element.disabled = false;
    }
}

// 添加CSS动画
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }

    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }

    .notification {
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    }

    .loading-spinner {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
    }

    .spinner {
        width: 16px;
        height: 16px;
        border: 2px solid #f3f3f3;
        border-top: 2px solid currentColor;
        border-radius: 50%;
        animation: spin 1s linear infinite;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;
document.head.appendChild(style);