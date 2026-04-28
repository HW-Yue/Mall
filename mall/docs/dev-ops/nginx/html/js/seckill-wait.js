(function () {
    var params = new URLSearchParams(window.location.search);
    var seckillToken = params.get("seckillToken") || "";
    var userId = params.get("userId") || (window.AppUtils && window.AppUtils.getCurrentUserId()) || "";
    var payAmountParam = params.get("payAmount") || "";
    var statusTextEl = null;
    var errorMsgEl = null;
    var pollingTimer = null;
    var retryCount = 0;
    var maxRetry = 60; // 约 5 分钟

    function normalizePayAmount(value) {
        var num = Number(value);
        if (isNaN(num)) return null;
        return Math.max(0, num);
    }

    function setStatus(text) {
        if (statusTextEl) statusTextEl.textContent = text;
    }

    function showError(text) {
        if (!errorMsgEl) return;
        errorMsgEl.classList.remove("hidden");
        errorMsgEl.textContent = text;
    }

    function hideError() {
        if (!errorMsgEl) return;
        errorMsgEl.classList.add("hidden");
    }

    function clearPolling() {
        if (pollingTimer) {
            clearInterval(pollingTimer);
            pollingTimer = null;
        }
    }

    function querySeckillOrder() {
        if (!seckillToken) {
            showError("缺少秒杀 token");
            return Promise.resolve(null);
        }
        var url = AppApi.order(AppApiPaths.order.querySeckillOrder);
        return fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ seckillToken: seckillToken })
        })
            .then(function (res) { return res.json(); })
            .then(function (json) {
                if (json.code !== "0000" || !json.data) return null;
                // status 为 1 表示已处理成功
                if (json.data.status === 1 && json.data.orderId) {
                    return json.data.orderId;
                }
                return null;
            })
            .catch(function () {
                return null;
            });
    }

    function pollOnce() {
        retryCount++;
        querySeckillOrder().then(function (orderId) {
            if (orderId) {
                clearPolling();
                setStatus("订单已生成，即将跳转...");
                var payQ =
                    "payment.html?orderId=" + encodeURIComponent(orderId) +
                    "&userId=" + encodeURIComponent(userId);
                var amount = normalizePayAmount(payAmountParam);
                if (amount != null) {
                    payQ += "&payAmount=" + encodeURIComponent(amount.toFixed(2));
                }
                // 延迟 1s 跳转，让用户看清状态
                setTimeout(function() {
                    window.location.href = payQ;
                }, 1000);
                return;
            }

            if (retryCount >= maxRetry) {
                clearPolling();
                setStatus("处理超时");
                showError("订单生成较慢，请稍后在订单列表查看。");
                var area = document.getElementById('actionArea');
                if (area) {
                    area.classList.remove('opacity-0', 'pointer-events-none');
                }
            }
        });
    }

    function startPolling() {
        hideError();
        clearPolling();
        retryCount = 0;
        pollOnce();
        pollingTimer = setInterval(pollOnce, 5000);
    }

    document.addEventListener("DOMContentLoaded", function () {
        statusTextEl = document.getElementById("statusText");
        errorMsgEl = document.getElementById("errorMsg");
        var tokenTextEl = document.getElementById("tokenText");
        var retryBtn = document.getElementById("retryBtn");

        if (tokenTextEl) {
            // 截取前 12 位，避免太长
            tokenTextEl.textContent = seckillToken ? seckillToken.substring(0, 12) + "..." : "---";
        }
        
        if (retryBtn) {
            retryBtn.addEventListener("click", function() {
                var area = document.getElementById('actionArea');
                if (area) area.classList.add('opacity-0', 'pointer-events-none');
                startPolling();
            });
        }

        if (!seckillToken || !userId) {
            setStatus("参数缺失");
            showError("页面缺少必要参数，请返回商城重试。");
            return;
        }

        startPolling();
    });
})();
