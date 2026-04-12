"""
Mock 支付宝服务
─────────────────────────────────────────────────────────────────
替代真实支付宝 SDK，用于本地 / 压测环境。
pay-service 的 alipay.gateway-url 改为本服务地址即可接管所有支付请求。

接口：
  POST /alipay.trade.page.pay           →  返回 mock 收银台 HTML（pay-service 转换为链接）
  GET  /mock/pay_page                   →  模拟收银台页面，点击按钮触发支付成功
  POST /mock/confirm_pay                →  用户确认，向 pay-service 发送回调
  POST /mock/trigger_callback           →  压测自动触发回调（不需要人点）
  GET  /mock/orders                     →  查看所有 mock 订单

启动：
  python mock/alipay.py
  PORT=7000 NOTIFY_URL=http://localhost:9091/api/v1/alipay/alipay_notify_url python mock/alipay.py
"""
import os
import sys
import time
import uuid
import threading
import subprocess

import requests
from flask import Flask, request, jsonify, redirect

app = Flask(__name__)

# ── 配置 ─────────────────────────────────────────────────────
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config import MOCK_ALIPAY_PORT, NOTIFY_URL

PORT = int(os.getenv("PORT", MOCK_ALIPAY_PORT))
_NOTIFY_URL = os.getenv("NOTIFY_URL", NOTIFY_URL)

# 内存订单表 {out_trade_no: order_dict}
_orders: dict = {}
_lock = threading.Lock()


# ─────────────────────────────────────────────────────────────
#  pay-service 调用的支付宝 SDK 接口
#  （alipay.trade.page.pay — 支付宝网页支付下单）
# ─────────────────────────────────────────────────────────────

@app.post("/gateway.do")
@app.post("/alipay.trade.page.pay")
def alipay_trade_page_pay():
    """
    pay-service 通过 alipay SDK 调此接口创建支付。
    支付宝真实场景返回 HTML 表单，这里返回一个跳转到 mock 收银台的 URL。
    """
    # alipay SDK 以 form 表单或 JSON 提交
    params = request.form.to_dict() or request.get_json(force=True, silent=True) or {}
    biz_str = params.get("biz_content", "{}")
    import json
    try:
        biz = json.loads(biz_str)
    except Exception:
        biz = {}

    out_trade_no = biz.get("out_trade_no") or params.get("out_trade_no") or str(uuid.uuid4())
    total_amount = biz.get("total_amount") or params.get("total_amount", "0.00")
    subject      = biz.get("subject") or params.get("subject", "Mock商品")
    trade_no     = f"MOCK_{int(time.time() * 1000)}_{out_trade_no[-8:]}"
    pay_url      = f"http://localhost:{PORT}/mock/pay_page?outTradeNo={out_trade_no}&tradeNo={trade_no}&amount={total_amount}"

    with _lock:
        _orders[out_trade_no] = {
            "outTradeNo":  out_trade_no,
            "tradeNo":     trade_no,
            "totalAmount": total_amount,
            "subject":     subject,
            "status":      "WAIT_BUYER_PAY",
            "createTime":  time.time(),
            "payUrl":      pay_url,
        }

    app.logger.info("[Mock] 创建支付 outTradeNo=%s", out_trade_no)
    # pay-service 期望从响应里拿到跳转 URL，直接返回 JSON 方便解析
    return jsonify({
        "alipay_trade_page_pay_response": {
            "code":         "10000",
            "msg":          "Success",
            "out_trade_no": out_trade_no,
            "trade_no":     trade_no,
        },
        "payUrl": pay_url,
        "sign":   "mock_sign",
    })


# ─────────────────────────────────────────────────────────────
#  Mock 收银台
# ─────────────────────────────────────────────────────────────

@app.get("/mock/pay_page")
def pay_page():
    out_trade_no = request.args.get("outTradeNo", "")
    trade_no     = request.args.get("tradeNo", "")
    amount       = request.args.get("amount", "0.00")
    return f"""<!DOCTYPE html>
<html>
<body style="font-family:sans-serif;text-align:center;padding:60px">
  <h2>Mock 支付宝收银台</h2>
  <p>订单号：<code>{out_trade_no}</code></p>
  <p>金额：<strong>¥{amount}</strong></p>
  <form method="post" action="/mock/confirm_pay">
    <input type="hidden" name="outTradeNo" value="{out_trade_no}">
    <input type="hidden" name="tradeNo"    value="{trade_no}">
    <button type="submit"
      style="padding:12px 36px;font-size:16px;background:#1677ff;
             color:#fff;border:none;border-radius:8px;cursor:pointer">
      确认支付
    </button>
  </form>
</body>
</html>"""


@app.post("/mock/confirm_pay")
def confirm_pay():
    '''用户在收银台点击"确认支付"'''
    out_trade_no = request.form.get("outTradeNo", "")
    trade_no     = request.form.get("tradeNo", "")
    _async_notify(out_trade_no, trade_no)
    return """<html><body style="text-align:center;padding:60px;font-family:sans-serif">
      <h2 style="color:green">✓ 支付成功（Mock）</h2>
      <p>已向 pay-service 发送回调</p>
    </body></html>"""


# ─────────────────────────────────────────────────────────────
#  压测工具接口
# ─────────────────────────────────────────────────────────────

@app.post("/mock/trigger_callback")
def trigger_callback():
    """
    压测 / 自动化用：直接触发某笔订单的支付成功回调，无需打开收银台。

    请求体：
      { "outTradeNo": "ORDER_xxx" }
    """
    body = request.get_json(force=True, silent=True) or {}
    out_trade_no = body.get("outTradeNo", "")
    if not out_trade_no:
        return jsonify({"code": "400", "msg": "outTradeNo required"}), 400

    with _lock:
        order = _orders.get(out_trade_no)
    if not order:
        return jsonify({"code": "404", "msg": f"order not found: {out_trade_no}"}), 404

    _async_notify(out_trade_no, order["tradeNo"])
    return jsonify({"code": "0000", "msg": "callback triggered async"})


@app.get("/mock/orders")
def list_orders():
    """查看当前内存中的所有 mock 订单"""
    with _lock:
        snapshot = list(_orders.values())
    return jsonify({"total": len(snapshot), "orders": snapshot})


# ─────────────────────────────────────────────────────────────
#  内部：异步发送支付宝回调
# ─────────────────────────────────────────────────────────────

def _async_notify(out_trade_no: str, trade_no: str) -> None:
    """在后台线程向 pay-service 发送 form 表单格式的支付回调"""
    with _lock:
        order = _orders.get(out_trade_no, {})
        if order:
            order["status"] = "TRADE_SUCCESS"

    def _send():
        payload = {
            "notify_type":  "trade_status_sync",
            "notify_id":    str(uuid.uuid4()),
            "notify_time":  time.strftime("%Y-%m-%d %H:%M:%S"),
            "trade_no":     trade_no,
            "out_trade_no": out_trade_no,
            "trade_status": "TRADE_SUCCESS",
            "total_amount": order.get("totalAmount", "0.00"),
            "seller_id":    "mock_seller_2088",
            "buyer_id":     "mock_buyer_001",
            "sign_type":    "RSA2",
            "sign":         "mock_sign_bypass",   # mock 不验签
        }
        try:
            r = requests.post(_NOTIFY_URL, data=payload, timeout=5)
            app.logger.info("[Mock] 回调 → %s %s", r.status_code, r.text[:120])
        except Exception as e:
            app.logger.warning("[Mock] 回调失败: %s", e)

    threading.Thread(target=_send, daemon=True).start()


# ─────────────────────────────────────────────────────────────
#  入口
# ─────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"[Mock Alipay] 监听 http://0.0.0.0:{PORT}")
    print(f"[Mock Alipay] 支付成功回调目标: {_NOTIFY_URL}")
    print(f"[Mock Alipay] 收银台页面: http://localhost:{PORT}/mock/pay_page?outTradeNo=xxx&tradeNo=xxx&amount=1.00")
    print(f"[Mock Alipay] 查看订单:   GET  http://localhost:{PORT}/mock/orders")
    print(f"[Mock Alipay] 手动触发:   POST http://localhost:{PORT}/mock/trigger_callback")
    app.run(host="0.0.0.0", port=PORT, debug=False)
