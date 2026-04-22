"""
场景：秒杀锁单压测（isTest=true，只锁单不发 MQ）
─────────────────────────────────────────────────────────
接口：POST /gw/api/v1/seckill/trade/create_pay_order
isTest=true 时，seckill-service 跳过 Redis 库存扣减和 RocketMQ，
直接返回一个 mock seckillToken。

用途：
  - 测试 seckill-service 自身 HTTP 接口吞吐量（排除 Redis/MQ 瓶颈）
  - 压测接口参数校验、JSON 序列化、Spring 路由层的极限 QPS
  - 不污染 Redis 库存，不产生真实订单，可反复运行

直接启动：
  python locust/seckill/lock_only.py
  访问 http://localhost:7321
"""
import os
import subprocess
import sys

from locust import HttpUser, between, task

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common import GATEWAY_HOST, LOCUST_PROCESSES, next_loadtest_user_id
from seckill.config import (
    ACTIVITY_ID,
    CHANNEL,
    LOCUST_WEB_PORT,
    PATH_SECKILL_CREATE_ORDER,
    PRODUCT_ID,
    SOURCE,
)


class SeckillLockOnlyUser(HttpUser):
    host = GATEWAY_HOST
    wait_time = between(0.05, 0.2)

    @task
    def create_seckill_order(self):
        payload = {
            "userId": next_loadtest_user_id(),
            "productId": PRODUCT_ID,
            "activityId": ACTIVITY_ID,
            "source": SOURCE,
            "channel": CHANNEL,
            "goodsName": "压测商品",
            "goodsImageUrl": "",
            "isTest": True,
        }
        with self.client.post(
            PATH_SECKILL_CREATE_ORDER,
            json=payload,
            name="seckill | POST create_pay_order [lock-only]",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
                return
            body = resp.json()
            if body.get("code") != "0000":
                resp.failure(f"biz error: {body.get('info')}")
                return
            token = (body.get("data") or {}).get("seckillToken", "")
            if not token:
                resp.failure("seckillToken missing in response")
                return
            resp.success()


if __name__ == "__main__":
    cmd = [
        sys.executable, "-m", "locust",
        "-f", __file__, "SeckillLockOnlyUser",
        "--host", GATEWAY_HOST,
        "--web-port", str(LOCUST_WEB_PORT),
        "--processes", str(LOCUST_PROCESSES),
    ]
    sys.exit(subprocess.run(cmd).returncode)
