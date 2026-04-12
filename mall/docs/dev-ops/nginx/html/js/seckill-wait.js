(function () {
    var params = new URLSearchParams(window.location.search);
    var seckillToken = params.get("seckillToken") || "";
    var userId = params.get("userId") || (window.AppUtils && window.AppUtils.getCurrentUserId()) || "";
    var statusTextEl = null;
    var errorMsgEl = null;
    var pollingTimer = null;
    var retryCount = 0;
    var maxRetry = 60; // 约 5 分钟

    function setStatus(text) {
        if (statusTextEl) statusTextEl.textContent = text;
    }

    function showError(text) {
        if (!errorMsgEl) return;
        errorMsgEl.style.display = "block";
        errorMsgEl.textContent = text;
    }

    function hideError() {
        if (!errorMsgEl) return;
        errorMsgEl.style.display = "none";
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
        setStatus("正在生成订单，第 " + retryCount + " 次检查...");
        querySeckillOrder().then(function (orderId) {
            if (orderId) {
                clearPolling();
                setStatus("订单已生成，正在跳转支付页...");
                window.location.href =
                    "payment.html?orderId=" + encodeURIComponent(orderId) +
                    "&userId=" + encodeURIComponent(userId);
                return;
            }

            if (retryCount >= maxRetry) {
                clearPolling();
                setStatus("订单生成较慢，请稍后在订单列表查看");
                showError("轮询超时，你可以去订单列表页继续查看订单状态。");
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

        if (tokenTextEl) tokenTextEl.textContent = seckillToken || "-";
        if (retryBtn) retryBtn.addEventListener("click", startPolling);

        if (!seckillToken || !userId) {
            setStatus("参数缺失");
            showError("页面缺少 seckillToken 或 userId，请返回重试。");
            return;
        }

        startPolling();
    });
})();
