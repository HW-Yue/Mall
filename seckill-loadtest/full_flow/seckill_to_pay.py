"""
场景：秒杀全链路压测
─────────────────────────────────────────────────────────────────
流程：
  Step 1  POST /gw/api/v1/seckill/trade/create_pay_order  (isTest=false)
          → 返回 seckillToken，同时触发真实 Redis 扣库存 + RocketMQ 建单

  Step 2  轮询 POST /gw/api/v1/order/query_seckill_order
          → 等待 order-service 消费 MQ 完成建单，status==1 时取得 orderId
          （每隔 0.5s 重试，最多 10 次，共等 ~5s）

  Step 3  POST /gw/api/v1/order/get_pay_url
          → order-service 调 pay-service，pay-service 调 mock_alipay
          → 返回 mock 支付 URL

前置条件：
  1. 启动 mock_alipay：  python mock/alipay.py
  2. pay-service 的 application-dev.yml 中将支付宝网关改为：
       alipay.gateway-url: http://localhost:7000
  3. 提前预热 Redis 库存（数量 >= 压测总请求数）：
       redis-cli SET seckill:stock:1:sku_001 500000

直接启动：
  python full_flow/seckill_to_pay.py
  然后访问 http://localhost:8089
"""
import sys
import os
import time
import subprocess

from locust import HttpUser, task, between, events

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config import (
    GATEWAY_HOST,
    PATH_SECKILL_CREATE_ORDER,
    PATH_ORDER_QUERY_SECKILL,
    PATH_ORDER_GET_PAY_URL,
    ACTIVITY_ID, PRODUCT_ID, SOURCE, CHANNEL,
    POLL_MAX_RETRIES, POLL_INTERVAL_SEC,
    next_loadtest_user_id,
)


class SeckillFullFlowUser(HttpUser):
    """
    秒杀全链路：锁单 → 轮询建单结果 → 请求支付 URL。
    wait_time 设长一些，给 MQ 异步建单留出时间。
    """
    host = GATEWAY_HOST
    wait_time = between(1, 3)

    @task
    def seckill_to_pay(self):
        user_id = next_loadtest_user_id()

        # ── Step 1：秒杀锁单 ──────────────────────────────────
        seckill_token = self._step_create_order(user_id)
        if not seckill_token:
            return

        # ── Step 2：轮询等待 order-service 建单完成 ───────────
        order_id = self._step_poll_order_id(seckill_token)
        if not order_id:
            self._fire_failure("order/query_seckill_order [poll]", "orderId not ready after max retries")
            return

        # ── Step 3：获取支付 URL（对接 mock_alipay）──────────
        self._step_get_pay_url(user_id, order_id)

    # ── 内部步骤 ─────────────────────────────────────────────

    def _step_create_order(self, user_id: str) -> str | None:
        payload = {
            "userId":        user_id,
            "productId":     PRODUCT_ID,
            "activityId":    ACTIVITY_ID,
            "source":        SOURCE,
            "channel":       CHANNEL,
            "goodsName":     "压测商品",
            "goodsImageUrl": "",
            "isTest":        False,      # 真实链路
        }
        with self.client.post(
            PATH_SECKILL_CREATE_ORDER,
            json=payload,
            name="seckill | POST create_pay_order [full-flow]",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
                return None
            body = resp.json()
            code = body.get("code")
            if code != "0000":
                # 库存耗尽不算压测失败，记录为 success 并退出当前 task
                if code in ("STOCK_NOT_ENOUGH",):
                    resp.success()
                else:
                    resp.failure(f"biz error [{code}]: {body.get('info')}")
                return None
            token = (body.get("data") or {}).get("seckillToken", "")
            if not token:
                resp.failure("seckillToken missing")
                return None
            resp.success()
            return token

    def _step_poll_order_id(self, seckill_token: str) -> str | None:
        """轮询 order-service，直到 MQ 消费完成建单（status==1）"""
        for attempt in range(POLL_MAX_RETRIES):
            t0 = time.monotonic()
            with self.client.post(
                PATH_ORDER_QUERY_SECKILL,
                json={"seckillToken": seckill_token},
                name="order | POST query_seckill_order [poll]",
                catch_response=True,
            ) as resp:
                elapsed_ms = int((time.monotonic() - t0) * 1000)
                if resp.status_code != 200:
                    resp.failure(f"HTTP {resp.status_code}")
                    return None
                body = resp.json()
                if body.get("code") != "0000":
                    resp.failure(f"biz error: {body.get('info')}")
                    return None
                data = body.get("data") or {}
                if data.get("status") == 1:
                    resp.success()
                    return data.get("orderId")
                resp.success()  # status==0 本身是正常响应

            # 还没好，等一下再试
            if attempt < POLL_MAX_RETRIES - 1:
                time.sleep(POLL_INTERVAL_SEC)

        return None

    def _step_get_pay_url(self, user_id: str, order_id: str) -> None:
        with self.client.post(
            PATH_ORDER_GET_PAY_URL,
            json={"userId": user_id, "orderId": order_id},
            name="order | POST get_pay_url [full-flow]",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
                return
            body = resp.json()
            if body.get("code") != "0000":
                resp.failure(f"biz error: {body.get('info')}")
                return
            pay_url = body.get("data", "")
            if not pay_url:
                resp.failure("payUrl missing in response")
                return
            resp.success()

    @staticmethod
    def _fire_failure(name: str, msg: str) -> None:
        events.request.fire(
            request_type="INTERNAL",
            name=name,
            response_time=0,
            response_length=0,
            exception=Exception(msg),
            context={},
        )


# ── 入口 ─────────────────────────────────────────────────────
if __name__ == "__main__":
    cmd = [
        sys.executable, "-m", "locust",
        "-f", __file__,
        "SeckillFullFlowUser",
        "--host", GATEWAY_HOST,
        "--web-port", "7321",
    ]
    sys.exit(subprocess.run(cmd).returncode)
