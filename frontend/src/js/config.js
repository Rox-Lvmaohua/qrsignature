// 配置文件
export const API_CONFIG = {
    // 开发环境使用代理，生产环境使用完整URL
    BASE_URL: process.env.NODE_ENV === 'production'
        ? 'http://localhost:29308/api/sign'
        : '/api/sign',
    TIMEOUT: 30000,
    RETRY_COUNT: 3
};

// 环境配置
export const ENV_CONFIG = {
    DEVELOPMENT: 'development',
    PRODUCTION: 'production',
    CURRENT: process.env.NODE_ENV || 'development'
};

export const APP_CONFIG = {
    POLLING_INTERVAL: 2000,
    SIGNATURE_HISTORY_COUNT: 10,
    NOTIFICATION_DURATION: 3000
};

export const ERROR_MESSAGES = {
    NETWORK_ERROR: '网络连接失败，请检查网络设置',
    INVALID_TOKEN: '无效的签名链接',
    SIGNATURE_EXISTS: '该用户已存在历史签名，不可重复保存',
    SAVE_SIGNATURE_FAILED: '保存签名失败',
    LOAD_SIGNATURE_FAILED: '加载签名失败'
};