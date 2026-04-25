// 支付确认页脚本（接口路径见 js/api-config.js）

(function () {
    var params = new URLSearchParams(window.location.search);
    var orderId = params.get("orderId") || "";
    var userId = params.get("userId") || (window.AppUtils && window.AppUtils.getCurrentUserId()) || "";
    var payAmountParam = params.get("payAmount") || "";

    document.addEventListener("DOMContentLoaded", function () {
        if (!orderId) {
            showError("缺少订单号，请重新下单");
            document.getElementById("payBtn").disabled = true;
            return;
        }
        document.getElementById("orderIdDisplay").textContent = orderId;
        var amtEl = document.getElementById("amountDisplay");
        if (payAmountParam && !isNaN(Number(payAmountParam))) {
            amtEl.textContent = "￥" + Number(payAmountParam).toFixed(2);
        } else {
            amtEl.textContent = "待确认";
        }
    });

    window.doPayment = function () {
        if (!orderId || !userId) {
            showError("订单信息不完整，请重新下单");
            return;
        }

        var btn = document.getElementById("payBtn");
        btn.disabled = true;
        showLoading(true);
        hideError();

        var url = window.AppApi.order(window.AppApiPaths.order.getPayUrl);
        fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: userId, orderId: orderId }),
        })
            .then(function (res) { return res.json(); })
            .then(function (result) {
                if (result.code === "0000" && result.data) {
                    // data 是支付宝自动提交的 HTML form，用 document.write 写入后脚本会执行并跳转
                    document.open();
                    document.write(result.data);
                    document.close();
                } else {
                    showError("获取支付链接失败：" + (result.info || "请稍后重试"));
                    btn.disabled = false;
                }
            })
            .catch(function (err) {
                console.error("获取支付链接异常：", err);
                showError("网络异常，请稍后重试");
                btn.disabled = false;
            })
            .finally(function () {
                showLoading(false);
            });
    };

    function showLoading(show) {
        var mask = document.getElementById("loadingMask");
        if (mask) mask.classList.toggle("show", show);
    }

    function showError(msg) {
        var el = document.getElementById("errorMsg");
        if (el) { el.textContent = msg; el.style.display = "block"; }
    }

    function hideError() {
        var el = document.getElementById("errorMsg");
        if (el) el.style.display = "none";
    }
})();
