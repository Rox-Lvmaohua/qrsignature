// 核心业务逻辑
import { API_CONFIG, APP_CONFIG, ERROR_MESSAGES } from './config.js';
import { showNotification, isCanvasEmpty, formatDate, copyToClipboard, isValidBase64, getUrlParameter, showLoading } from './utils.js';

/**
 * QR签名应用主类
 */
export class QrSignatureApp {
    constructor() {
        this.apiUrl = API_CONFIG.BASE_URL;
        this.currentToken = null;
        this.pollingInterval = null;
        this.currentProjectId = null;
        this.currentUserId = null;
        this.currentFileId = null;
        this.signRecordId = null;
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
            console.log(data)

            if (!response.ok) {
                throw new Error(data.message || '生成签署页面失败');
            }

            this.currentToken = data.token;
            this.signRecordId = data.signRecordId;
            this.showSignPage(data.signUrl);
            this.startPolling();

        } catch (error) {
            console.error('生成签署页面失败:', error);
            let errorMessage = error.message || '生成签署页面失败';

            // 网络错误处理
            if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
                errorMessage = ERROR_MESSAGES.NETWORK_ERROR;
            }

            this.showStatus(errorMessage, 'error');
            this.showLoading(false);

            // 显示重试按钮
            setTimeout(() => {
                const statusDiv = document.getElementById('status');
                if (statusDiv && statusDiv.textContent.includes(ERROR_MESSAGES.NETWORK_ERROR)) {
                    statusDiv.innerHTML += '<br><button onclick="qrSignatureApp.generateSignPage()" style="margin-top: 10px; padding: 8px 16px; background: #dc3545; color: white; border: none; border-radius: 4px; cursor: pointer;">重试</button>';
                }
            }, 1000);
        }
    }

    showSignPage(url) {
        const signContainer = document.getElementById('signContainer');
        const signInfo = document.getElementById('signInfo');

        // 清除之前的内容
        signInfo.innerHTML = '';

        // 生成签署页面二维码（直接跳转）
        signInfo.innerHTML = `
            <div class="qr-code">
                <h5 class="mb-3"><i class="bi bi-qr-code-scan me-2"></i>扫码直接签名</h5>
                <p class="text-muted mb-4">扫描下方二维码，直接跳转到签名页面</p>
                <div id="qrcode" class="d-flex justify-content-center align-items-center"></div>
                <div class="mt-3">
                    <small class="text-muted">
                        <i class="bi bi-info-circle me-1"></i>
                        二维码包含完整的签名信息，扫码即可直接签名
                    </small>
                </div>
            </div>
            <div class="mt-3 text-center">
                <button class="btn btn-outline-secondary btn-sm" onclick="copyToClipboard('${url}')">
                    <i class="bi bi-clipboard me-1"></i>复制签名链接
                </button>
                <a href="${url}" target="_blank" class="btn btn-outline-primary btn-sm ms-2">
                    <i class="bi bi-box-arrow-up-right me-1"></i>直接打开
                </a>
            </div>
        `;

        // 使用Bootstrap的d-none类控制显示/隐藏
        signContainer.classList.remove('d-none');
        document.getElementById('requestForm').classList.add('d-none');
        this.showStatus('签署页面已生成，请扫码或点击链接进行签名', 'info');

        // 生成二维码
        this.generateQRCode(url);
    }

    async generateQRCode(url) {
        const qrContainer = document.getElementById('qrcode');
        if (!qrContainer) {
            console.error('无法找到二维码容器元素')
            return;
        }

        // 清除之前的二维码
        qrContainer.innerHTML = '';

        // 显示加载状态
        qrContainer.innerHTML = `
            <div class="text-center">
                <div class="spinner-border text-primary mb-3" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <p class="text-muted">正在生成二维码...</p>
            </div>
        `;

        try {
            // 等待QRCode库加载完成
            console.log('等待QRCode库加载...');
            const isQRCodeAvailable = await this.waitForQRCode();
            console.log('QRCode库可用状态:', isQRCodeAvailable);

            if (!isQRCodeAvailable) {
                // 提供手动重试选项
                qrContainer.innerHTML = `
                    <div class="alert alert-warning text-center">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        <strong>二维码库加载失败</strong>
                        <div class="mt-3">
                            <button class="btn btn-warning btn-sm" onclick="window.qrSignatureApp.generateQRCode('${url.replace(/'/g, "\\'")}')">
                                <i class="bi bi-arrow-clockwise me-1"></i>重试
                            </button>
                        </div>
                        <div class="mt-3 small">
                            <p class="mb-2">签署页面链接：</p>
                            <a href="${url}" target="_blank" class="text-break">${url}</a>
                        </div>
                    </div>
                `;
                return;
            }

            // 清除加载状态
            qrContainer.innerHTML = '';
            // 创建二维码容器div
            const qrDiv = document.createElement('div');
            qrDiv.id = 'qrcode-display';
            qrContainer.appendChild(qrDiv);

            // 使用 qrcodejs 库的正确 API
            new QRCode(qrDiv, {
                text: url,
                width: 200,
                height: 200,
                colorDark: '#000000',
                colorLight: '#ffffff',
                correctLevel: QRCode.CorrectLevel.H
            });

            console.log('二维码生成成功');
        } catch (error) {
            console.error('QRCode库加载异常:', error);
            qrContainer.innerHTML = `
                <div class="qrcode-error">
                    <p style="color: #dc3545;">二维码库加载异常</p>
                    <button onclick="window.qrSignatureApp.generateQRCode('${url.replace(/'/g, "\\'")}')"
                            style="background: #dc3545; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin-top: 10px;">
                        重试
                    </button>
                    <div style="margin-top: 10px; font-size: 12px; color: #6c757d;">
                        <p>签署页面链接：</p>
                        <a href="${url}" target="_blank" style="word-break: break-all; color: #007bff;">${url}</a>
                    </div>
                </div>
            `;
        }
    }

    async waitForQRCode() {
        // 如果QRCode已经可用，直接返回
        if (typeof QRCode !== 'undefined') {
            return true;
        }

        // 如果全局检测函数可用，使用它
        if (typeof checkQRCodeAvailability === 'function') {
            return await checkQRCodeAvailability();
        }

        // 否则使用简单的轮询检测
        return new Promise((resolve) => {
            let attempts = 0;
            const maxAttempts = 50; // 5秒超时

            const checkInterval = setInterval(() => {
                attempts++;

                if (typeof QRCode !== 'undefined') {
                    clearInterval(checkInterval);
                    resolve(true);
                } else if (attempts >= maxAttempts) {
                    clearInterval(checkInterval);
                    resolve(false);
                }
            }, 100);
        });
    }

    startPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
        }

        this.pollingInterval = setInterval(async () => {
            if (!this.signRecordId || !this.currentProjectId || !this.currentUserId || !this.currentFileId || !this.currentToken) return;

            try {
                const response = await fetch(`${this.apiUrl}/status?signRecordId=${this.signRecordId}`, {
                    headers: {
                        'Authorization': this.currentToken
                    }
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || '查询状态失败');
                }

                // 检查状态是否变化
                if (data.status) {
                    let statusClass = 'info';
                    let statusIndicator = 'unscanned';

                    if (data.status === '已扫描未签署') {
                        statusClass = 'warning';
                        statusIndicator = 'scanned';
                    } else if (data.status === '已签署') {
                        statusClass = 'success';
                        statusIndicator = 'signed';
                        this.stopPolling();
                        this.showSignatureComplete(data.signatureBase64);
                    }

                    this.showStatus(`<span class="status-indicator ${statusIndicator}"></span>当前状态: ${data.status}`, statusClass);
                }

            } catch (error) {
                console.error('轮询失败:', error);
                // 如果token无效，停止轮询
                if (error.message.includes('无效的Token') || error.message.includes('未传入token')) {
                    this.stopPolling();
                    this.showStatus('token已过期，请重新生成', 'error');
                }
            }
        }, APP_CONFIG.POLLING_INTERVAL); // 每2秒轮询一次
    }

    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }

    showSignatureComplete(signatureBase64) {
        const signContainer = document.getElementById('signContainer');
        const signatureResult = document.getElementById('signatureResult');

        signContainer.classList.add('d-none');
        signatureResult.classList.remove('d-none');

        // 显示签名图片
        const signatureImage = document.getElementById('signatureImage');
        if (signatureImage && signatureBase64) {
            signatureImage.src = signatureBase64;
        }

        this.showStatus('签名已完成！', 'success');
    }

    resetForm() {
        this.stopPolling();
        this.currentToken = null;
        this.currentProjectId = null;
        this.currentUserId = null;
        this.currentFileId = null;

        // 重置表单，使用Bootstrap的d-none类控制显示/隐藏
        document.getElementById('requestForm').classList.remove('d-none');
        document.getElementById('signContainer').classList.add('d-none');
        document.getElementById('signatureResult').classList.add('d-none');

        // 清空状态
        this.showStatus('', '');
        this.showLoading(false);
    }

    showStatus(message, type) {
        const statusDiv = document.getElementById('status');
        if (!statusDiv) return;

        statusDiv.innerHTML = message;

        // 清除所有状态类
        statusDiv.className = '';

        if (!message) {
            statusDiv.style.display = 'none';
        } else {
            statusDiv.style.display = 'block';

            // 根据类型添加Bootstrap样式
            switch(type) {
                case 'error':
                    statusDiv.className = 'alert alert-danger';
                    break;
                case 'success':
                    statusDiv.className = 'alert alert-success';
                    break;
                case 'warning':
                    statusDiv.className = 'alert alert-warning';
                    break;
                default:
                    statusDiv.className = 'alert alert-info';
            }
        }
    }

    showLoading(show) {
        const generateBtn = document.getElementById('generateBtn');
        if (!generateBtn) return;

        if (show) {
            // 保存原始文本
            if (!generateBtn.dataset.originalText) {
                generateBtn.dataset.originalText = generateBtn.textContent;
            }
            generateBtn.innerHTML = '<span class="loading"></span> 生成中...';
            generateBtn.disabled = true;
        } else {
            generateBtn.innerHTML = generateBtn.dataset.originalText || '生成签署页面';
            generateBtn.disabled = false;
        }
    }
}

/**
 * 签名页面类
 */
export class SignaturePage {
    constructor() {
        this.apiUrl = API_CONFIG.BASE_URL;
        this.token = this.getTokenFromURL();
        this.selectedSignatureId = null;
        this.init();
    }

    init() {
        if (!this.token) {
            this.showError(ERROR_MESSAGES.INVALID_TOKEN);
            return;
        }

        this.loadSignPage();
        this.bindEvents();
    }

    getTokenFromURL() {
        return getUrlParameter('token');
    }

    async loadSignPage() {
        document.getElementById('signForm').innerHTML = `
            <div class="form-info">
                <h3>签名确认</h3>
                <p><strong>签名链接:</strong> ${window.location.href}</p>
                <p>请在下方区域签名：</p>
            </div>

            <div class="signature-history">
                <h4>历史签名</h4>
                <div id="signatureGrid" class="signature-grid">
                    <div class="loading"></div>
                    <p>正在加载历史签名...</p>
                </div>
            </div>

            <div class="signature-canvas">
                <h4>请在下方区域签名：</h4>
                <canvas id="signatureCanvas" width="400" height="200" style="border: 2px solid #ddd; border-radius: 8px; cursor: crosshair;"></canvas>
                <div class="save-option">
                    <label>
                        <input type="checkbox" id="saveSignature" checked onchange="signaturePage.onSaveSignatureChange(this)">
                        保存签名供下次使用
                    </label>
                    <p id="saveSignatureHint" style="margin: 8px 0 0 0; color: #6c757d; font-size: 14px;">勾选后，您的签名将被保存，下次可直接使用</p>
                </div>
                <div class="canvas-controls">
                    <button onclick="signaturePage.clearCanvas()">清除</button>
                    <button onclick="signaturePage.useHistorySignature()" class="use-history-btn">使用历史签名</button>
                    <button onclick="signaturePage.confirmSignature()" class="confirm-btn">确认签名</button>
                </div>
            </div>
        `;

        this.initSignatureCanvas();
        await this.loadSignatureHistory();
    }

    async loadSignatureHistory() {
        const signatureGrid = document.getElementById('signatureGrid');
        if (!signatureGrid) return;

        try {
            // 从token中获取用户ID
            const userId = this.getUserIdFromToken();
            if (!userId) {
                signatureGrid.innerHTML = '<div class="no-signatures">无法获取用户信息</div>';
                return;
            }

            // 调用后端API获取用户历史签名
            const response = await fetch(`${this.apiUrl}/user-signatures?userId=${encodeURIComponent(userId)}`, {
                method: 'GET'
            });

            if (!response.ok) {
                if (response.status === 404) {
                    signatureGrid.innerHTML = '<div class="no-signatures">暂无历史签名<br><small>勾选保存签名后可在此查看</small></div>';
                    return;
                }
                throw new Error('获取历史签名失败');
            }

            const data = await response.json();
            const signatures = data.signatures || [];

            if (signatures.length === 0) {
                signatureGrid.innerHTML = '<div class="no-signatures">暂无历史签名<br><small>勾选保存签名后可在此查看</small></div>';
                return;
            }

            signatureGrid.innerHTML = signatures.map(sig => `
                <div class="signature-item" data-id="${sig.id}" onclick="signaturePage.selectSignature('${sig.id}')">
                    <img src="${sig.signatureBase64}" alt="历史签名">
                    <div class="signature-info">
                        <div class="signature-name">历史签名</div>
                        <div class="signature-date">${formatDate(sig.createdAt)}</div>
                    </div>
                </div>
            `).join('');

        } catch (error) {
            console.error('加载历史签名失败:', error);
            signatureGrid.innerHTML = '<div class="no-signatures">加载历史签名失败</div>';
        }
    }

    getUserIdFromToken() {
        try {
            // 简单解析JWT token获取userId（实际应用中应该在后端解析）
            const payload = JSON.parse(atob(this.token.split('.')[1]));
            return payload.userId;
        } catch (error) {
            console.error('解析token失败:', error);
            return null;
        }
    }

    selectSignature(signatureId) {
        // 移除之前的选中状态
        document.querySelectorAll('.signature-item').forEach(item => {
            item.classList.remove('selected');
        });

        // 添加新的选中状态
        const selectedItem = document.querySelector(`[data-id="${signatureId}"]`);
        if (selectedItem) {
            selectedItem.classList.add('selected');
            this.selectedSignatureId = signatureId;
        }
    }

    useHistorySignature() {
        if (!this.selectedSignatureId) {
            showNotification('请先选择一个历史签名', 'warning');
            return;
        }

        // 直接使用选中的历史签名ID，在确认签名时会发送到后端
        showNotification('已选择历史签名，请点击确认签名', 'success');

        // 可选：在画布上显示"使用历史签名"的提示
        const canvas = document.getElementById('signatureCanvas');
        if (canvas) {
            const ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            ctx.fillStyle = '#6c757d';
            ctx.font = '16px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('已选择历史签名', canvas.width / 2, canvas.height / 2);
            ctx.font = '12px Arial';
            ctx.fillText('点击"确认签名"完成签署', canvas.width / 2, canvas.height / 2 + 20);
        }
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

    async onSaveSignatureChange(checkbox) {
        const userId = this.getUserIdFromToken();
        if (!userId) return;

        if (checkbox.checked) {
            try {
                const canSave = await this.checkCanSaveSignature(userId);
                if (!canSave) {
                    // 取消勾选
                    checkbox.checked = false;

                    // 更新提示信息
                    const hint = document.getElementById('saveSignatureHint');
                    if (hint) {
                        hint.textContent = '该用户已存在历史签名，不可重复保存';
                        hint.style.color = '#dc3545';
                    }

                    showNotification('该用户已存在历史签名，不可重复保存', 'warning');
                } else {
                    // 更新提示信息
                    const hint = document.getElementById('saveSignatureHint');
                    if (hint) {
                        hint.textContent = '勾选后，您的签名将被保存，下次可直接使用';
                        hint.style.color = '#6c757d';
                    }
                }
            } catch (error) {
                console.error('检查签名保存权限失败:', error);
                showNotification('检查签名保存权限失败', 'error');
                checkbox.checked = false;
            }
        } else {
            // 取消勾选时恢复默认提示
            const hint = document.getElementById('saveSignatureHint');
            if (hint) {
                hint.textContent = '勾选后，您的签名将被保存，下次可直接使用';
                hint.style.color = '#6c757d';
            }
        }
    }

    clearCanvas() {
        const canvas = document.getElementById('signatureCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    async confirmSignature() {
        const canvas = document.getElementById('signatureCanvas');
        if (!canvas) return;

        // 检查画布是否为空
        if (isCanvasEmpty(canvas) && !this.selectedSignatureId) {
            this.showError('请先进行签名或选择历史签名');
            return;
        }

        let signatureBase64 = '';
        if (this.selectedSignatureId) {
            // 使用历史签名，从DOM中获取
            const selectedElement = document.querySelector(`[data-id="${this.selectedSignatureId}"] img`);
            if (selectedElement) {
                signatureBase64 = selectedElement.src;
            }
        } else {
            // 使用新签名
            signatureBase64 = canvas.toDataURL('image/png');
        }

        const saveForReuse = document.getElementById('saveSignature').checked;

        // 如果勾选了保存签名，先检查是否可以保存
        if (saveForReuse && !this.selectedSignatureId) {
            const userId = this.getUserIdFromToken();
            if (userId) {
                try {
                    const canSave = await this.checkCanSaveSignature(userId);
                    if (!canSave) {
                        const shouldOverride = await this.showOverrideDialog();
                        if (!shouldOverride) {
                            return; // 用户取消
                        }
                    }
                } catch (error) {
                    console.error('检查签名保存权限失败:', error);
                    showNotification('检查签名保存权限失败', 'error');
                    return;
                }
            }
        }

        try {
            const requestBody = {
                signatureBase64: signatureBase64,
                saveForReuse: saveForReuse
            };

            // 如果使用了历史签名，添加userSignatureId
            if (this.selectedSignatureId) {
                requestBody.userSignatureId = this.selectedSignatureId;
            }

            const response = await fetch(`${this.apiUrl}/confirm`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': this.token
                },
                body: JSON.stringify(requestBody)
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || '签名确认失败');
            }

            this.showSuccess(data);
        } catch (error) {
            console.error('签名确认失败:', error);
            let errorMessage = error.message || '签名确认失败';

            // 网络错误处理
            if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
                errorMessage = ERROR_MESSAGES.NETWORK_ERROR;
            }

            this.showError(errorMessage);
        }
    }

    async checkCanSaveSignature(userId) {
        try {
            const response = await fetch(`${this.apiUrl}/check-signature-exists?userId=${encodeURIComponent(userId)}`, {
                method: 'GET'
            });
            const data = await response.json();
            return data.canSave;
        } catch (error) {
            console.error('检查签名保存权限失败:', error);
            return false;
        }
    }

    async showOverrideDialog() {
        return new Promise((resolve) => {
            const dialog = document.createElement('div');
            dialog.className = 'signature-confirmation-dialog';
            dialog.innerHTML = `
                <div class="dialog-content">
                    <h3>签名保存确认</h3>
                    <p>该用户已存在历史签名，是否要覆盖？</p>
                    <div class="dialog-buttons">
                        <button class="btn-cancel" onclick="this.closest('.signature-confirmation-dialog').remove(); resolve(false);">取消</button>
                        <button class="btn-confirm" onclick="this.closest('.signature-confirmation-dialog').remove(); resolve(true);">覆盖</button>
                    </div>
                </div>
            `;

            // 添加样式
            const style = document.createElement('style');
            style.textContent = `
                .signature-confirmation-dialog {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(0, 0, 0, 0.5);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 10000;
                }

                .dialog-content {
                    background: white;
                    padding: 30px;
                    border-radius: 12px;
                    text-align: center;
                    max-width: 400px;
                    width: 90%;
                }

                .dialog-content h3 {
                    margin: 0 0 20px 0;
                    color: #333;
                }

                .dialog-content p {
                    margin: 0 0 30px 0;
                    color: #666;
                }

                .dialog-buttons {
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                }

                .dialog-buttons button {
                    padding: 10px 20px;
                    border: none;
                    border-radius: 6px;
                    cursor: pointer;
                    font-size: 14px;
                }

                .btn-cancel {
                    background: #6c757d;
                    color: white;
                }

                .btn-confirm {
                    background: #28a745;
                    color: white;
                }
            `;
            document.head.appendChild(style);

            document.body.appendChild(dialog);
        });
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
