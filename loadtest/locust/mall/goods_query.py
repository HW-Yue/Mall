"""
Mall 商城商品分页查询压测
──────────────────────────────────────────────
接口：POST /gw/api/v1/mall/index/query_goods_page
请求体：{ categoryId?: int, pageNum: int, pageSize: int }

用途：
  1. 验证网关 /gw/api/v1/mall/** 路由 + mall 服务 /actuator/prometheus 可达
  2. 给 Prometheus 告警链路制造足够流量，触发 Sentinel 的
     MallServiceQueryGoodsPageBlockHigh 等规则（需先在 Nacos 中配置较低阈值）

直接启动：
  python locust/mall/goods_query.py
  访问 http://100.86.250.112:7322
"""
import os
import random
import subprocess
import sys

from locust import HttpUser, between, task

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common import GATEWAY_HOST, LOCUST_PROCESSES
from mall.config import (
    CATEGORY_ALL_RATIO,
    CATEGORY_ID_POOL,
    DEFAULT_PAGE_NUM,
    DEFAULT_PAGE_SIZE,
    LOCUST_WEB_PORT,
    PATH_MALL_GOODS_PAGE,
)


class MallGoodsQueryUser(HttpUser):
    host = GATEWAY_HOST
    wait_time = between(0.1, 0.5)

    @task
    def query_goods_page(self):
        if random.random() < CATEGORY_ALL_RATIO:
            category_id = None
        else:
            category_id = random.choice(CATEGORY_ID_POOL)

        payload = {
            "categoryId": category_id,
            "pageNum": DEFAULT_PAGE_NUM,
            "pageSize": DEFAULT_PAGE_SIZE,
        }
        if category_id is None:
            payload.pop("categoryId")

        with self.client.post(
            PATH_MALL_GOODS_PAGE,
            json=payload,
            name="mall | POST query_goods_page",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
                return
            try:
                body = resp.json()
            except ValueError:
                resp.failure(f"non-json response: {resp.text[:200]}")
                return
            if body.get("code") != "0000":
                # 业务错误（含 Sentinel 限流）在 Locust 里能直接看出来
                resp.failure(f"biz error [{body.get('code')}]: {body.get('info')}")
                return
            resp.success()


if __name__ == "__main__":
    cmd = [
        sys.executable, "-m", "locust",
        "-f", __file__, "MallGoodsQueryUser",
        "--host", GATEWAY_HOST,
        "--web-port", str(LOCUST_WEB_PORT),
        "--processes", str(LOCUST_PROCESSES),
    ]
    sys.exit(subprocess.run(cmd).returncode)
