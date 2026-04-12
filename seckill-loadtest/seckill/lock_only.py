"""
场景 2 / 2：秒杀锁单压测（isTest=true，只锁单不发 MQ）
─────────────────────────────────────────────────────────
接口：POST /gw/api/v1/seckill/trade/create_pay_order
isTest=true 时，seckill-service 跳过 Redis 库存扣减和 RocketMQ，
直接返回一个 mock seckillToken。

用途：
  - 测试 seckill-service 自身的 HTTP 接口吞吐量（排除 Redis/MQ 瓶颈）
  - 压测接口参数校验、JSON 序列化、Spring 路由层的极限 QPS
  - 不污染 Redis 库存，不产生真实订单，可反复运行

直接启动：
  python seckill/lock_only.py
  然后访问 http://localhost:8089
"""
import sys
import os
import subprocess

from locust import HttpUser, task, between

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from config import (
    GATEWAY_HOST,
    PATH_SECKILL_CREATE_ORDER,
    ACTIVITY_ID, PRODUCT_ID, SOURCE, CHANNEL,
    next_loadtest_user_id,
)


class SeckillLockOnlyUser(HttpUser):
    """
    秒杀锁单压测（isTest=true）。
    服务端不操作 Redis/MQ，纯测接口层 QPS。
    """
    host = GATEWAY_HOST
    wait_time = between(0.05, 0.2)

    @task
    def create_seckill_order(self):
        payload = {
            "userId":        next_loadtest_user_id(),
            "productId":     PRODUCT_ID,
            "activityId":    ACTIVITY_ID,
            "source":        SOURCE,
            "channel":       CHANNEL,
            "goodsName":     "压测商品",
            "goodsImageUrl": "",
            "isTest":        False,
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


# ── 入口 ─────────────────────────────────────────────────────
if __name__ == "__main__":
    # Windows 下避免 subprocess 导致的 gevent 信号问题
    from locust.main import main
    sys.argv = [
        "locust",
        "-f", __file__,
        "--class-picker",
        "--host", GATEWAY_HOST,
        "--web-port", "7321",
    ]
    main()
