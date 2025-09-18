class QrSignatureApp {
    constructor() {
        this.apiUrl = 'http://localhost:29308/api/sign';
        this.currentToken = null;
        this.pollingInterval = null;
        this.currentProjectId = null;
        this.currentUserId = null;
        this.currentFileId = null;
        this.init();
    }

    init() {
        this.bindEvents();
    }

    bindEvents() {
        // 绑定表单提交事件
        document.getElementById('generateBtn').addEventListener('click', () => this.generateSignPage());
        document.getElementById('resetBtn').addEventListener('click', () => this.resetForm());
    }

    async generateSignPage() {
        const projectId = document.getElementById('projectId').value.trim();
        const userId = document.getElementById('userId').value.trim();
        const fileId = document.getElementById('fileId').value.trim();
        const metaCode = document.getElementById('metaCode').value.trim();

        if (!projectId || !userId || !fileId || !metaCode) {
            this.showStatus('请填写所有必需字段', 'error');
            return;
        }

        this.currentProjectId = projectId;
        this.currentUserId = userId;
        this.currentFileId = fileId;
        this.signatureId = null;

        try {
            this.showLoading(true);
            const response = await fetch(`${this.apiUrl}/url`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    projectId,
                    userId,
                    fileId,
                    metaCode
                })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || '生成签署页面失败');
            }

            this.currentToken = data.token;
            this.signatureId = data.signatureId;
            this.showSignPage(data.signUrl, data.signatureSequence);
            this.startPolling();

        } catch (error) {
            this.showStatus(error.message, 'error');
            this.showLoading(false);
        }
    }

    showSignPage(url, signatureSequence) {
        const signContainer = document.getElementById('signContainer');
        const signInfo = document.getElementById('signInfo');

        // 清除之前的内容
        signInfo.innerHTML = '';

        // 生成签署页面链接
        signInfo.innerHTML = `
            <div class="sign-link">
                <p><strong>签署页面链接：</strong></p>
                <a href="${url}" target="_blank">${url}</a>
                <p style="margin-top: 10px; font-size: 14px; color: #666;">
                    签署序号: ${signatureSequence} | 点击链接或在新标签页中打开进行签名
                </p>
            </div>
        `;

        signContainer.classList.add('show');
        document.getElementById('requestForm').style.display = 'none';
        this.showStatus(`签署页面已生成（第${signatureSequence}次签署），请点击链接进行签名`, 'info');
    }

    startPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
        }

        this.pollingInterval = setInterval(async () => {
            if (!this.currentProjectId || !this.currentUserId || !this.currentFileId || !this.currentToken) return;

            try {
                const response = await fetch(`${this.apiUrl}/status?id=${this.signatureId}`, {
                    headers: {
                        'Authorization': this.currentToken
                    }
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || '查询状态失败');
                }

                // 检查状态是否变化
                if (data.status !== '未扫描') {
                    this.showStatus(`当前状态: ${data.status} (第${data.signatureSequence || 1}次签署)`, 'info');

                    // 如果已签署，停止轮询并显示结果
                    if (data.status === '已签署') {
                        this.stopPolling();
                        this.showSignatureComplete(data.signatureBase64, data.signatureSequence);
                    }
                }

            } catch (error) {
                console.error('轮询失败:', error);
                // 如果token无效，停止轮询
                if (error.message.includes('无效的Token') || error.message.includes('未传入token')) {
                    this.stopPolling();
                    this.showStatus('token已过期，请重新生成', 'error');
                }
            }
        }, 2000); // 每2秒轮询一次
    }

    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }

    showSignatureComplete(signatureBase64, signatureSequence) {
        const signContainer = document.getElementById('signContainer');
        const signatureResult = document.getElementById('signatureResult');

        signContainer.classList.remove('show');
        signatureResult.classList.remove('hidden');

        // 显示签名图片
        const signatureImage = document.getElementById('signatureImage');
        if (signatureImage && signatureBase64) {
            signatureImage.src = signatureBase64;
        }

        // 显示签署序号
        const sequenceInfo = document.getElementById('sequenceInfo');
        if (sequenceInfo) {
            sequenceInfo.textContent = `第${signatureSequence || 1}次签署`;
        }

        this.showStatus(`签名已完成！（第${signatureSequence || 1}次签署）`, 'success');
    }

    resetForm() {
        this.stopPolling();
        this.currentToken = null;
        this.currentProjectId = null;
        this.currentUserId = null;
        this.currentFileId = null;
        this.signatureId = null;

        // 重置表单
        document.getElementById('requestForm').style.display = 'block';
        document.getElementById('signContainer').classList.remove('show');
        document.getElementById('signatureResult').classList.add('hidden');

        // 清空状态
        this.showStatus('', '');
        this.showLoading(false);
    }

    showStatus(message, type) {
        const statusDiv = document.getElementById('status');
        if (!statusDiv) return;

        statusDiv.textContent = message;
        statusDiv.className = 'status';

        if (type) {
            statusDiv.classList.add(type);
        }

        if (!message) {
            statusDiv.style.display = 'none';
        } else {
            statusDiv.style.display = 'block';
        }
    }

    showLoading(show) {
        const generateBtn = document.getElementById('generateBtn');
        if (!generateBtn) return;

        if (show) {
            generateBtn.innerHTML = '<span class="loading"></span> 生成中...';
            generateBtn.disabled = true;
        } else {
            generateBtn.innerHTML = '生成签署页面';
            generateBtn.disabled = false;
        }
    }
}

// 签名页面类
class SignaturePage {
    constructor() {
        this.apiUrl = 'http://localhost:29308/api/sign';
        this.token = this.getTokenFromURL();
        this.currentUserId = null;
        this.selectedUserSignatureId = null;
        this.signatureMode = 'draw'; // 'draw' or 'historical'
        this.init();
    }

    init() {
        if (!this.token) {
            this.showError('无效的签名链接');
            return;
        }

        this.loadSignPage();
        this.bindEvents();
    }

    getTokenFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('token');
    }

    async loadSignPage() {
        try {
            // 尝试从token中获取用户信息
            const response = await fetch(`${this.apiUrl}/user-info`, {
                headers: {
                    'Authorization': this.token
                }
            });

            if (response.ok) {
                const userData = await response.json();
                this.currentUserId = userData.userId;
                await this.loadUserSignatures();
            } else {
                console.warn('无法获取用户信息，继续加载页面');
                this.userSignatures = [];
            }
        } catch (error) {
            console.warn('加载用户信息失败，继续加载页面:', error);
            this.userSignatures = [];
        }

        // 无论是否获取到用户信息，都渲染页面
        this.renderSignaturePage();
    }

    async loadUserSignatures() {
        if (!this.currentUserId) {
            this.userSignatures = [];
            return;
        }

        try {
            const response = await fetch(`${this.apiUrl}/user-signatures?userId=${this.currentUserId}`);
            if (response.ok) {
                const signatures = await response.json();
                this.userSignatures = signatures || [];
            } else {
                this.userSignatures = [];
            }
        } catch (error) {
            console.warn('加载用户签名失败:', error);
            this.userSignatures = [];
        }
    }

    renderSignaturePage() {
        const signForm = document.getElementById('signForm');

        let historicalSignaturesHtml = '';
        const signatures = this.userSignatures || [];
        if (signatures.length > 0) {
            historicalSignaturesHtml = `
                <div class="historical-signatures">
                    <h4>历史签名：</h4>
                    <div class="signature-grid">
                        ${signatures.map(sig => `
                            <div class="signature-item" data-signature-id="${sig.id}">
                                <img src="${sig.signatureBase64}" alt="签名">
                                <p>${sig.signatureName}</p>
                                ${sig.isDefault ? '<small style="color: #28a745;">默认</small>' : ''}
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        }

        signForm.innerHTML = `
            <div class="form-info">
                <h3>签名确认</h3>
                <p><strong>签名链接:</strong> ${window.location.href}</p>
                <p>请在下方选择签名方式：</p>
            </div>

            <div class="signature-mode">
                <h4>签名方式：</h4>
                <div class="mode-selector">
                    <div class="mode-btn active" data-mode="draw">手写签名</div>
                    <div class="mode-btn" data-mode="historical">历史签名</div>
                </div>
            </div>

            <div id="drawMode" class="signature-canvas">
                <h4>请在下方区域签名：</h4>
                <canvas id="signatureCanvas" width="400" height="200" style="border: 2px solid #ddd; border-radius: 8px; cursor: crosshair;"></canvas>
                <div class="canvas-controls">
                    <button onclick="signaturePage.clearCanvas()">清除</button>
                    <button onclick="signaturePage.confirmSignature()" class="confirm-btn">确认签名</button>
                </div>
            </div>

            <div id="historicalMode" class="historical-signatures hidden">
                ${historicalSignaturesHtml}
                <div class="canvas-controls">
                    <button onclick="signaturePage.confirmSignature()" class="confirm-btn">确认签名</button>
                </div>
            </div>

            <div class="signature-options">
                <h4>签名选项：</h4>
                <div class="checkbox-group">
                    <div class="checkbox-item">
                        <input type="checkbox" id="saveForReuse">
                        <label for="saveForReuse">保存此签名以便下次使用</label>
                    </div>
                    <div class="checkbox-item" id="signatureNameGroup" style="display: none;">
                        <input type="text" id="signatureName" class="text-input" placeholder="签名名称（可选）">
                    </div>
                    <div class="checkbox-item">
                        <input type="checkbox" id="setAsDefault">
                        <label for="setAsDefault">设为默认签名</label>
                    </div>
                </div>
            </div>
        `;

        this.bindSignatureEvents();
        this.initSignatureCanvas();
    }

    bindSignatureEvents() {
        // 签名方式切换
        document.querySelectorAll('.mode-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');

                const mode = e.target.dataset.mode;
                this.signatureMode = mode;

                document.getElementById('drawMode').classList.toggle('hidden', mode !== 'draw');
                document.getElementById('historicalMode').classList.toggle('hidden', mode !== 'historical');
            });
        });

        // 保存签名选项
        document.getElementById('saveForReuse').addEventListener('change', (e) => {
            document.getElementById('signatureNameGroup').style.display = e.target.checked ? 'flex' : 'none';
        });

        // 历史签名选择
        document.querySelectorAll('.signature-item').forEach(item => {
            item.addEventListener('click', (e) => {
                document.querySelectorAll('.signature-item').forEach(i => i.classList.remove('selected'));
                e.currentTarget.classList.add('selected');
                this.selectedUserSignatureId = e.currentTarget.dataset.signatureId;
            });
        });
    }

    initSignatureCanvas() {
        const canvas = document.getElementById('signatureCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        let isDrawing = false;
        let lastX = 0;
        let lastY = 0;

        ctx.strokeStyle = '#000';
        ctx.lineWidth = 2;
        ctx.lineCap = 'round';

        // 鼠标事件
        canvas.addEventListener('mousedown', (e) => {
            isDrawing = true;
            const rect = canvas.getBoundingClientRect();
            lastX = e.clientX - rect.left;
            lastY = e.clientY - rect.top;
        });

        canvas.addEventListener('mousemove', (e) => {
            if (!isDrawing) return;

            const rect = canvas.getBoundingClientRect();
            const currentX = e.clientX - rect.left;
            const currentY = e.clientY - rect.top;

            ctx.beginPath();
            ctx.moveTo(lastX, lastY);
            ctx.lineTo(currentX, currentY);
            ctx.stroke();

            lastX = currentX;
            lastY = currentY;
        });

        canvas.addEventListener('mouseup', () => {
            isDrawing = false;
        });

        canvas.addEventListener('mouseout', () => {
            isDrawing = false;
        });

        // 触摸事件（移动设备支持）
        canvas.addEventListener('touchstart', (e) => {
            e.preventDefault();
            isDrawing = true;
            const rect = canvas.getBoundingClientRect();
            const touch = e.touches[0];
            lastX = touch.clientX - rect.left;
            lastY = touch.clientY - rect.top;
        });

        canvas.addEventListener('touchmove', (e) => {
            e.preventDefault();
            if (!isDrawing) return;

            const rect = canvas.getBoundingClientRect();
            const touch = e.touches[0];
            const currentX = touch.clientX - rect.left;
            const currentY = touch.clientY - rect.top;

            ctx.beginPath();
            ctx.moveTo(lastX, lastY);
            ctx.lineTo(currentX, currentY);
            ctx.stroke();

            lastX = currentX;
            lastY = currentY;
        });

        canvas.addEventListener('touchend', (e) => {
            e.preventDefault();
            isDrawing = false;
        });
    }

    clearCanvas() {
        const canvas = document.getElementById('signatureCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    async confirmSignature() {
        let signatureBase64 = null;
        let userSignatureId = null;

        if (this.signatureMode === 'draw') {
            const canvas = document.getElementById('signatureCanvas');
            if (!canvas) return;

            // 检查画布是否为空
            const ctx = canvas.getContext('2d');
            const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const isEmpty = imageData.data.every(pixel => pixel === 0);

            if (isEmpty) {
                this.showError('请先进行签名');
                return;
            }

            signatureBase64 = canvas.toDataURL('image/png');
        } else if (this.signatureMode === 'historical') {
            if (!this.selectedUserSignatureId) {
                this.showError('请选择一个历史签名');
                return;
            }
            userSignatureId = this.selectedUserSignatureId;
        }

        const saveForReuse = document.getElementById('saveForReuse').checked;
        const signatureName = document.getElementById('signatureName').value.trim();
        const setAsDefault = document.getElementById('setAsDefault').checked;

        try {
            const response = await fetch(`${this.apiUrl}/confirm`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': this.token
                },
                body: JSON.stringify({
                    signatureBase64: signatureBase64,
                    userSignatureId: userSignatureId,
                    saveForReuse: saveForReuse,
                    signatureName: signatureName,
                    setAsDefault: setAsDefault
                })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || '签名确认失败');
            }

            this.showSuccess(data);
        } catch (error) {
            this.showError(error.message);
        }
    }

    showSuccess(data) {
        document.getElementById('signForm').innerHTML = `
            <div class="success-message">
                <h3>✅ 签名成功！</h3>
                <p>您的签名已成功保存。</p>
                <div class="signature-preview">
                    <h4>签名预览：</h4>
                    <img src="${data.signatureBase64}" alt="签名图片" style="max-width: 100%; max-height: 200px; border-radius: 8px;">
                </div>
                <p><small>签名记录ID: ${data.signRecordId}</small></p>
                <p><small>第${data.signatureSequence || 1}次签署</small></p>
                <button onclick="window.close()">关闭页面</button>
                <button onclick="window.location.href='/'">返回首页</button>
            </div>
        `;
    }

    showError(message) {
        document.getElementById('signForm').innerHTML = `
            <div class="error-message">
                <h3>❌ 错误</h3>
                <p>${message}</p>
                <button onclick="window.location.href='/'">返回首页</button>
            </div>
        `;
    }

    bindEvents() {
        // 事件绑定已在相应方法中处理
    }
}

// 根据页面路径决定初始化哪个类
document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;

    if (path === '/signing.html' || path.includes('signing.html')) {
        // 签名页面
        window.signaturePage = new SignaturePage();
    } else {
        // 主页面
        window.qrSignatureApp = new QrSignatureApp();

        // 暴露全局函数供HTML调用
        window.generateSignPage = () => window.qrSignatureApp.generateSignPage();
        window.resetForm = () => window.qrSignatureApp.resetForm();
    }
});

// 工具函数
window.showNotification = function(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        color: white;
        font-weight: 600;
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;

    switch(type) {
        case 'success':
            notification.style.backgroundColor = '#28a745';
            break;
        case 'error':
            notification.style.backgroundColor = '#dc3545';
            break;
        case 'warning':
            notification.style.backgroundColor = '#ffc107';
            notification.style.color = '#212529';
            break;
        default:
            notification.style.backgroundColor = '#17a2b8';
    }

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            if (document.body.contains(notification)) {
                document.body.removeChild(notification);
            }
        }, 300);
    }, 3000);
};

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
`;
document.head.appendChild(style);