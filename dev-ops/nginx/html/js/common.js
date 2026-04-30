// 公共配置和工具函数。接口路径见 js/api-config.js；引入顺序：common.js → api-config.js → 业务脚本。
// 与 springcloud-gateway 同端口时，各路径见 api-config.js。普通品走 mall create_normal_order 需本机 RocketMQ（order-service 消费 normal-order-create）；秒杀需 seckill-order-create 消费。
const AppConfig = {
    groupBuyMarketUrl: "http://100.86.250.112:8090",
    payApiBase: "http://100.86.250.112:8090",
    loginApiUrl: "http://100.86.250.112:8090",
    orderServiceUrl: "http://100.86.250.112:8090",
    groupBuyServiceUrl: "http://100.86.250.112:8090",
    seckillServiceUrl: "http://100.86.250.112:8090",
};

// 工具函数
const AppUtils = {
    // 获取Cookie值
    getCookie: function(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    },
    
    // 获取当前登录用户ID
    getCurrentUserId: function() {
        const urlParams = new URLSearchParams(window.location.search);
        const urlUserId = urlParams.get('userId');
        if (urlUserId) return urlUserId;
        const userId = this.getCookie("loginToken");
        if (!userId) {
            window.location.href = "login.html"; // 跳转到登录页
            return null;
        }
        return userId;
    },
    
    // 混淆用户ID显示
    obfuscateUserId: function(userId) {
        if (userId.length <= 4) {
            // 如果 userId 的长度小于或等于 4，则无需替换任何字符
            return userId;
        } else {
            // 获取前两位和后两位
            const start = userId.slice(0, 2);
            const end = userId.slice(-2);
            // 计算中间部分应该被替换成多少个 *
            const middle = '*'.repeat(userId.length - 4);
            // 返回成功替换后的字符串
            return `${start}${middle}${end}`;
        }
    },
    
    // 从URL参数获取用户ID（可选功能）
    getUserIdFromUrl: function() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('userId') || this.getCurrentUserId();
    }
};

// 导出配置和工具函数到全局
window.AppConfig = AppConfig;
window.AppUtils = AppUtils;
window.getCookie = AppUtils.getCookie;
window.obfuscateUserId = AppUtils.obfuscateUserId;