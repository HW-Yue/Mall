// 订单明细页面JavaScript
class OrderListManager {
    constructor() {
        this.userId = AppUtils.getUserIdFromUrl(); // 从公共工具获取用户ID
        this.lastId = null;
        this.pageSize = 10;
        this.hasMore = true;
        this.loading = false;
        this.currentRefundOrderId = null;
        this.currentRefundMarketType = null;

        this.init();
    }
    
    init() {
        this.bindEvents();
        this.displayUserId();
        this.loadOrderList();
    }
    
    bindEvents() {
        // 加载更多按钮事件
        document.getElementById('loadMoreBtn').addEventListener('click', () => {
            this.loadOrderList();
        });
        
        // 退单弹窗事件
        document.getElementById('cancelRefund').addEventListener('click', () => {
            this.hideRefundModal();
        });
        
        document.getElementById('confirmRefund').addEventListener('click', () => {
            this.processRefund();
        });
        
        // 点击弹窗外部关闭
        document.getElementById('refundModal').addEventListener('click', (e) => {
            if (e.target.id === 'refundModal') {
                this.hideRefundModal();
            }
        });
    }
    
    displayUserId() {
        const userIdElement = document.getElementById('userIdDisplay');
        if (userIdElement && this.userId) {
            userIdElement.textContent = `用户ID: ${AppUtils.obfuscateUserId(this.userId)}`;
        }
    }
    
    async loadOrderList() {
        if (this.loading || !this.hasMore) return;
        
        this.loading = true;
        this.showLoading();
        
        try {
            const requestData = {
                userId: this.userId,
                lastId: this.lastId,
                pageSize: this.pageSize
            };
            
            const response = await fetch(AppApi.order(AppApiPaths.order.queryUserOrderList), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });
            
            const result = await response.json();
            
            if (result.code === '0000' && result.data) {
                this.renderOrderList(result.data.orderList, this.lastId === null);
                this.hasMore = result.data.hasMore;
                this.lastId = result.data.lastId;
                
                // 更新加载更多按钮状态
                this.updateLoadMoreButton();
            } else {
                this.showError('加载订单列表失败: ' + (result.info || '未知错误'));
            }
        } catch (error) {
            console.error('加载订单列表出错:', error);
            this.showError('网络错误，请稍后重试');
        } finally {
            this.loading = false;
            this.hideLoading();
        }
    }
    
    renderOrderList(orders, isFirstLoad = false) {
        const orderListElement = document.getElementById('orderList');
        const emptyStateElement = document.getElementById('emptyState');
        
        if (isFirstLoad) {
            orderListElement.innerHTML = '';
        }
        
        if (orders && orders.length > 0) {
            emptyStateElement.style.display = 'none';
            
            orders.forEach(order => {
                const orderElement = this.createOrderElement(order);
                orderListElement.appendChild(orderElement);
            });
        } else if (isFirstLoad) {
            emptyStateElement.style.display = 'block';
        }
    }
    
    createOrderElement(order) {
        const orderDiv = document.createElement('div');
        orderDiv.className = 'order-item';
        orderDiv.innerHTML = `
            <div class="order-header">
                <div class="order-id" onclick="orderManager.copyOrderId('${order.orderId}')" title="点击复制订单号">
                    订单号: <span class="order-id-text">${order.orderId}</span>
                    <span class="copy-icon">📋</span>
                </div>
                <div class="order-status status-${order.status}">${this.getStatusText(order.status)}</div>
            </div>
            <div class="order-content">
                <div class="product-name">${order.productName || '商品名称'}</div>
                <div class="order-details">
                    <div class="order-time">${this.formatTime(order.orderTime)}</div>
                    <div class="pay-amount">¥${order.payAmount || order.totalAmount}</div>
                </div>
            </div>
            <div class="order-actions">
                ${order.status === 'PAY_WAIT' ? `
                <button class="pay-btn" onclick="orderManager.goPay('${order.orderId}')">
                    立即支付
                </button>
                ` : ''}
                <button class="refund-btn"
                        onclick="orderManager.showRefundModal('${order.orderId}', '${order.marketType || 'normal'}')"
                        ${order.status === 'CLOSE' ? 'disabled' : ''}>
                    ${order.status === 'CLOSE' ? '已关闭' : '申请退单'}
                </button>
            </div>
        `;
        
        return orderDiv;
    }
    
    /**
     * 立即支付：请求后端获取支付链接，新窗口打开支付宝收银台
     * @param {string} orderId 订单号
     */
    async goPay(orderId) {
        this.showLoading();
        try {
            const response = await fetch(AppApi.order(AppApiPaths.order.getPayUrl), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: this.userId, orderId: orderId })
            });
            const result = await response.json();
            if (result.code !== '0000' || !result.data) {
                this.showError('获取支付链接失败: ' + (result.info || '未知错误'));
                return;
            }
            window.open(result.data, '_blank');
        } catch (error) {
            console.error('立即支付出错:', error);
            this.showError('网络错误，请稍后重试');
        } finally {
            this.hideLoading();
        }
    }
    
    getStatusText(status) {
        const statusMap = {
            'CREATE': '新创建',
            'PAY_WAIT': '等待支付',
            'PAY_SUCCESS': '支付成功',
            'DEAL_DONE': '交易完成',
            'CLOSE': '已关闭',
            'WAIT_REFUND': '退款中',
        };
        return statusMap[status] || status;
    }
    
    formatTime(timeStr) {
        if (!timeStr) return '';
        const date = new Date(timeStr);
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    }
    
    updateLoadMoreButton() {
        const loadMoreBtn = document.getElementById('loadMoreBtn');
        if (this.hasMore) {
            loadMoreBtn.style.display = 'block';
            loadMoreBtn.disabled = false;
            loadMoreBtn.textContent = '加载更多';
        } else {
            loadMoreBtn.style.display = 'none';
        }
    }
    
    showRefundModal(orderId, marketType) {
        this.currentRefundOrderId = orderId;
        this.currentRefundMarketType = marketType || 'normal';
        document.getElementById('refundModal').style.display = 'flex';
    }

    hideRefundModal() {
        document.getElementById('refundModal').style.display = 'none';
        this.currentRefundOrderId = null;
        this.currentRefundMarketType = null;
    }

    async processRefund() {
        if (!this.currentRefundOrderId) return;

        this.showLoading();

        try {
            const requestData = {
                userId: this.userId,
                orderId: this.currentRefundOrderId
            };

            const marketType = this.currentRefundMarketType || 'normal';
            let refundUrl;
            if (marketType === 'group_buy') {
                refundUrl = AppApi.groupBuy(AppApiPaths.gbm.refund);
            } else if (marketType === 'seckill') {
                refundUrl = AppApi.seckill(AppApiPaths.seckill.refund);
            } else {
                refundUrl = AppApi.order(AppApiPaths.order.refund);
            }

            const response = await fetch(refundUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            });

            const result = await response.json();

            if (result.code === '0000') {
                this.showSuccess('退单申请成功');
                this.hideRefundModal();
                this.refreshOrderList();
            } else {
                this.showError('退单失败: ' + (result.info || '未知错误'));
            }
        } catch (error) {
            console.error('退单操作出错:', error);
            this.showError('网络错误，请稍后重试');
        } finally {
            this.hideLoading();
        }
    }
    
    refreshOrderList() {
        this.lastId = null;
        this.hasMore = true;
        document.getElementById('orderList').innerHTML = '';
        this.loadOrderList();
    }
    
    showLoading() {
        document.getElementById('loadingTip').style.display = 'block';
    }
    
    hideLoading() {
        document.getElementById('loadingTip').style.display = 'none';
    }
    
    showError(message) {
        alert('错误: ' + message);
    }
    
    showSuccess(message) {
        alert('成功: ' + message);
    }
    
    // 复制订单号功能
    copyOrderId(orderId) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(orderId).then(() => {
                this.showToast('订单号已复制到剪贴板');
            }).catch(err => {
                console.error('复制失败:', err);
                this.fallbackCopyTextToClipboard(orderId);
            });
        } else {
            this.fallbackCopyTextToClipboard(orderId);
        }
    }
    
    // 兼容旧浏览器的复制方法
    fallbackCopyTextToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        
        try {
            const successful = document.execCommand('copy');
            if (successful) {
                this.showToast('订单号已复制到剪贴板');
            } else {
                this.showToast('复制失败，请手动复制');
            }
        } catch (err) {
            console.error('复制失败:', err);
            this.showToast('复制失败，请手动复制');
        }
        
        document.body.removeChild(textArea);
    }
    
    // 显示提示消息
    showToast(message) {
        // 移除已存在的提示
        const existingToast = document.querySelector('.copy-toast');
        if (existingToast) {
            existingToast.remove();
        }
        
        // 创建新的提示元素
        const toast = document.createElement('div');
        toast.className = 'copy-toast';
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%);
            background: #333;
            color: white;
            padding: 12px 20px;
            border-radius: 6px;
            z-index: 1000;
            font-size: 14px;
            opacity: 0;
            transition: opacity 0.3s ease;
        `;
        
        document.body.appendChild(toast);
        
        // 显示动画
        setTimeout(() => {
            toast.style.opacity = '1';
        }, 100);
        
        // 3秒后移除
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }, 3000);
    }
}

// 页面加载完成后初始化
let orderManager;
document.addEventListener('DOMContentLoaded', function() {
    orderManager = new OrderListManager();
});

// 从 bfcache 恢复时（浏览器返回键）重新拉取订单列表
window.addEventListener('pageshow', function(event) {
    if (event.persisted && orderManager) {
        orderManager.refreshOrderList();
    }
});

// 用户切换回此标签页时刷新
document.addEventListener('visibilitychange', function() {
    if (document.visibilityState === 'visible' && orderManager) {
        orderManager.refreshOrderList();
    }
});