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

    static get STATUS_CONFIGS() {
        return {
            'CREATE': { text: '新创建', class: 'bg-blue-50 text-blue-600', icon: 'plus-circle' },
            'PAY_WAIT': { text: '等待支付', class: 'bg-orange-50 text-orange-600', icon: 'credit-card' },
            'PAY_SUCCESS': { text: '支付成功', class: 'bg-green-50 text-green-600', icon: 'check-circle' },
            'DEAL_DONE': { text: '交易完成', class: 'bg-indigo-50 text-indigo-600', icon: 'package' },
            'CLOSE': { text: '已关闭', class: 'bg-slate-100 text-slate-400', icon: 'x-circle' },
            'WAIT_REFUND': { text: '退款中', class: 'bg-red-50 text-red-600', icon: 'refresh-ccw' },
            'REFUNDED': { text: '已退款', class: 'bg-rose-50 text-rose-600', icon: 'rotate-ccw' },
            'WAIT_SHIP': { text: '待发货', class: 'bg-cyan-50 text-cyan-600', icon: 'package-check' },
            'SHIPPED': { text: '已发货', class: 'bg-violet-50 text-violet-600', icon: 'truck' },
            'DELIVERED': { text: '已签收', class: 'bg-emerald-50 text-emerald-600', icon: 'badge-check' }
        };
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
            userIdElement.textContent = AppUtils.obfuscateUserId(this.userId);
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
                this.updateLoadMoreButton();
                this.updateStatistics(result.data.orderList, this.lastId === null);
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

    updateStatistics(orders, isFirstLoad) {
        if (!isFirstLoad) return; // 仅在首次加载时估算统计（或根据全量数据统计）
        
        // 注意：由于是分页加载，这里的统计仅作为演示或针对当前页。
        // 在真实项目中，后端应返回各状态的总数。
        const counts = {
            total: orders.length,
            completed: orders.filter(o => o.status === 'PAY_SUCCESS' || o.status === 'DEAL_DONE').length,
            pending: orders.filter(o => o.status === 'PAY_WAIT' || o.status === 'CREATE').length
        };

        const totalEl = document.getElementById('totalOrderCount');
        const completedEl = document.getElementById('completedOrderCount');
        const pendingEl = document.getElementById('pendingOrderCount');

        if (totalEl) totalEl.textContent = counts.total || '0';
        if (completedEl) completedEl.textContent = counts.completed || '0';
        if (pendingEl) pendingEl.textContent = counts.pending || '0';
    }
    
    renderOrderList(orders, isFirstLoad = false) {
        const orderListElement = document.getElementById('orderList');
        const emptyStateElement = document.getElementById('emptyState');
        
        if (isFirstLoad) {
            orderListElement.innerHTML = '';
        }
        
        if (orders && orders.length > 0) {
            emptyStateElement.classList.add('hidden');
            
            orders.forEach(order => {
                const orderElement = this.createOrderElement(order);
                orderListElement.appendChild(orderElement);
            });
            if (window.lucide) window.lucide.createIcons();
        } else if (isFirstLoad) {
            emptyStateElement.classList.remove('hidden');
        }
    }

    getStatusConfig(status) {
        return OrderListManager.STATUS_CONFIGS[status] || {
            text: status,
            class: 'bg-slate-100 text-slate-500',
            icon: 'help-circle'
        };
    }

    getRefundAction(status) {
        switch (status) {
            case 'CLOSE':
                return { disabled: true, text: '已关闭' };
            case 'WAIT_REFUND':
                return { disabled: true, text: '退款处理中' };
            case 'REFUNDED':
                return { disabled: true, text: '已退款' };
            case 'WAIT_SHIP':
                return { disabled: true, text: '待发货' };
            case 'SHIPPED':
                return { disabled: true, text: '已发货' };
            case 'DELIVERED':
                return { disabled: true, text: '已签收' };
            default:
                return { disabled: false, text: '退款/取消' };
        }
    }
    
    createOrderElement(order) {
        const config = this.getStatusConfig(order.status);
        const refundAction = this.getRefundAction(order.status);
        const orderDiv = document.createElement('div');
        orderDiv.className = 'order-card bg-white rounded-[32px] border border-slate-100 p-6 shadow-sm';
        
        orderDiv.innerHTML = `
            <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
                <div class="flex items-center gap-3">
                    <div class="p-3 rounded-2xl ${config.class.split(' ')[0]}">
                        <i data-lucide="${config.icon}" class="w-6 h-6"></i>
                    </div>
                    <div>
                        <div class="flex items-center gap-2 mb-0.5">
                            <span class="text-xs font-bold text-slate-400 uppercase tracking-widest">Order ID</span>
                            <button onclick="orderManager.copyOrderId('${order.orderId}')" class="p-1 hover:bg-slate-100 rounded-md transition-colors">
                                <i data-lucide="copy" class="w-3 h-3 text-slate-400"></i>
                            </button>
                        </div>
                        <p class="text-sm font-bold text-slate-900 font-mono">${order.orderId}</p>
                    </div>
                </div>
                <div class="flex items-center gap-3">
                    <span class="px-4 py-1.5 rounded-full text-xs font-bold ${config.class}">${config.text}</span>
                </div>
            </div>

            <div class="flex items-start gap-4 mb-6 pb-6 border-b border-slate-50">
                <div class="w-16 h-16 rounded-2xl bg-slate-50 flex-shrink-0 flex items-center justify-center">
                    <i data-lucide="shopping-bag" class="w-8 h-8 text-slate-200"></i>
                </div>
                <div class="flex-1">
                    <h4 class="text-base font-extrabold text-slate-900 mb-1">${order.productName || 'AI 数字资源商品'}</h4>
                    <div class="flex items-center gap-3 text-xs font-medium text-slate-400">
                        <span class="flex items-center gap-1"><i data-lucide="calendar" class="w-3 h-3"></i> ${this.formatTime(order.orderTime)}</span>
                        <span class="flex items-center gap-1"><i data-lucide="tag" class="w-3 h-3"></i> ${order.marketType === 'group_buy' ? '拼团' : (order.marketType === 'seckill' ? '秒杀' : '普通')}</span>
                    </div>
                </div>
                <div class="text-right">
                    <p class="text-xs font-bold text-slate-400 uppercase mb-1 leading-none">Amount</p>
                    <p class="text-xl font-black text-slate-900 tracking-tight">￥${(order.payAmount || order.totalAmount || 0).toFixed(2)}</p>
                </div>
            </div>

            <div class="flex items-center justify-end gap-3">
                ${order.status === 'PAY_WAIT' ? `
                <button class="px-6 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-bold hover:bg-blue-700 transition-all shadow-lg shadow-blue-100 active:scale-95" onclick="orderManager.goPay('${order.orderId}')">
                    立即支付
                </button>
                ` : ''}
                <button class="px-6 py-2.5 rounded-xl bg-slate-50 text-slate-600 text-sm font-bold hover:bg-slate-100 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
                        onclick="orderManager.showRefundModal('${order.orderId}', '${order.marketType || 'normal'}')"
                        ${refundAction.disabled ? 'disabled' : ''}>
                    ${refundAction.text}
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
        const targetUrl = `payment.html?orderId=${encodeURIComponent(orderId)}&userId=${encodeURIComponent(this.userId)}&autoPay=1`;
        window.location.href = targetUrl;
    }
    
    getStatusText(status) {
        const config = this.getStatusConfig(status);
        return config.text || status;
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
