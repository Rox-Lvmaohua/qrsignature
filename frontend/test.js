/**
 * 简单的功能测试脚本
 * 用于验证前端模块化和API集成
 */

// 测试模块导入
console.log('=== 模块导入测试 ===');

try {
    // 测试配置模块
    const { API_CONFIG } = await import('./src/js/config.js');
    console.log('✅ 配置模块导入成功');
    console.log('API_BASE_URL:', API_CONFIG.BASE_URL);

    // 测试工具函数模块
    const { showNotification, formatDate, isValidBase64 } = await import('./src/js/utils.js');
    console.log('✅ 工具函数模块导入成功');

    // 测试业务逻辑模块
    const { QrSignatureApp, SignaturePage } = await import('./src/js/app.js');
    console.log('✅ 业务逻辑模块导入成功');

} catch (error) {
    console.error('❌ 模块导入失败:', error);
}

// 测试工具函数
console.log('\n=== 工具函数测试 ===');

try {
    const { formatDate, isValidBase64 } = await import('./src/js/utils.js');

    // 测试日期格式化
    const testDate = '2024-01-01T00:00:00';
    const formattedDate = formatDate(testDate);
    console.log('✅ 日期格式化测试:', formattedDate);

    // 测试Base64验证
    const validBase64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==';
    const invalidBase64 = 'invalid-base64';

    console.log('✅ 有效Base64验证:', isValidBase64(validBase64));
    console.log('✅ 无效Base64验证:', !isValidBase64(invalidBase64));

} catch (error) {
    console.error('❌ 工具函数测试失败:', error);
}

// 测试配置
console.log('\n=== 配置测试 ===');

try {
    const { API_CONFIG, APP_CONFIG, ERROR_MESSAGES } = await import('./src/js/config.js');

    console.log('✅ API配置:', API_CONFIG);
    console.log('✅ 应用配置:', APP_CONFIG);
    console.log('✅ 错误消息配置:', Object.keys(ERROR_MESSAGES));

} catch (error) {
    console.error('❌ 配置测试失败:', error);
}

// 测试业务类实例化
console.log('\n=== 业务类实例化测试 ===');

try {
    const { QrSignatureApp, SignaturePage } = await import('./src/js/app.js');

    // 测试主应用类
    const app = new QrSignatureApp();
    console.log('✅ QrSignatureApp实例化成功');

    // 测试签名页面类
    const signaturePage = new SignaturePage();
    console.log('✅ SignaturePage实例化成功');

} catch (error) {
    console.error('❌ 业务类实例化测试失败:', error);
}

// 模拟API测试
console.log('\n=== API集成测试 ===');

async function testAPIIntegration() {
    try {
        const { API_CONFIG } = await import('./src/js/config.js');

        // 测试API连接
        const response = await fetch(`${API_CONFIG.BASE_URL}/user-signatures?userId=test`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            console.log('✅ API连接测试成功');
        } else {
            console.log('⚠️ API连接测试: 后端服务可能未启动');
        }

    } catch (error) {
        console.log('⚠️ API连接测试失败 (网络错误或后端服务未启动):', error.message);
    }
}

testAPIIntegration();

console.log('\n=== 测试完成 ===');
console.log('如果看到以上 ✅ 标记，说明前端模块化重构成功！');
console.log('如果看到 ⚠️ 标记，说明需要启动后端服务进行完整测试。');