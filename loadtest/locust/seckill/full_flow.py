"""
场景：秒杀全链路压测
─────────────────────────────────────────────────────────────────
流程：
  Step 1  POST /gw/api/v1/seckill/trade/create_pay_order  (isTest=false)
          → 返回 seckillToken，同时触发 Redis 扣库存 + RocketMQ 建单

  Step 2  轮询 POST /gw/api/v1/order/query_seckill_order
          → 等待 order-service 消费 MQ 完成建单，status==1 时取得 orderId
          （每隔 POLL_INTERVAL_SEC 重试，最多 POLL_MAX_RETRIES 次）

  Step 3  POST /gw/api/v1/order/get_pay_url
          → order-service 调 pay-service，pay-service 调 mock_alipay
          → 返回 mock 支付 URL

前置条件：
  1. 启动 mock_alipay：  make seckill-mock
  2. Docker test 环境下 pay-service 的 application-test.yml 已将支付宝网关指向：
       alipay.gatewayUrl: http://host.docker.internal:7000/gateway.do
  3. 预热 Redis 库存（数量 >= 压测总请求数）：
       redis-cli SET seckill:stock:1:sku_001 500000

直接启动：
  python locust/seckill/full_flow.py
  访问 http://100.86.250.112:7321
"""
import os
import subprocess
import sys
import time

from locust import HttpUser, between, events, task

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common import GATEWAY_HOST, LOCUST_PROCESSES, next_loadtest_user_id
from seckill.config import (
    ACTIVITY_ID,
    CHANNEL,
    LOCUST_WEB_PORT,
    PATH_ORDER_GET_PAY_URL,
    PATH_ORDER_QUERY_SECKILL,
    PATH_SECKILL_CREATE_ORDER,
    POLL_INTERVAL_SEC,
    POLL_MAX_RETRIES,
    PRODUCT_ID,
    SOURCE,
)


class SeckillFullFlowUser(HttpUser):
    host = GATEWAY_HOST
    wait_time = between(1, 3)

    @task
    def seckill_to_pay(self):
        user_id = next_loadtest_user_id()

        seckill_token = self._step_create_order(user_id)
        if not seckill_token:
            return

        order_id = self._step_poll_order_id(seckill_token)
        if not order_id:
            self._fire_failure(
                "order/query_seckill_order [poll]",
                "orderId not ready after max retries",
            )
            return

        self._step_get_pay_url(user_id, order_id)

    def _step_create_order(self, user_id: str) -> str | None:
        payload = {
            "userId": user_id,
            "productId": PRODUCT_ID,
            "activityId": ACTIVITY_ID,
            "source": SOURCE,
            "channel": CHANNEL,
            "goodsName": "压测商品",
            "goodsImageUrl": "",
            "isTest": False,
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
                # 库存耗尽不算压测失败
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
        for attempt in range(POLL_MAX_RETRIES):
            with self.client.post(
                PATH_ORDER_QUERY_SECKILL,
                json={"seckillToken": seckill_token},
                name="order | POST query_seckill_order [poll]",
                catch_response=True,
            ) as resp:
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
                resp.success()

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


if __name__ == "__main__":
    cmd = [
        sys.executable, "-m", "locust",
        "-f", __file__, "SeckillFullFlowUser",
        "--host", GATEWAY_HOST,
        "--web-port", str(LOCUST_WEB_PORT),
        "--processes", str(LOCUST_PROCESSES),
    ]
    sys.exit(subprocess.run(cmd).returncode)
