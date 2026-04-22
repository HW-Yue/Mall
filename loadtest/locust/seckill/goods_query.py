"""
场景：秒杀商品列表查询压测
────────────────────────────────
接口：GET /gw/api/v1/seckill/market/query_goods_list
无参数。

用途：在开始秒杀锁单压测前，先用此脚本确认：
  1. 网关路由正常
  2. seckill-service 可达
  3. 数据库 / 缓存中有商品数据

直接启动：
  python locust/seckill/goods_query.py
  访问 http://localhost:7321
"""
import os
import subprocess
import sys

from locust import HttpUser, between, task

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common import GATEWAY_HOST, LOCUST_PROCESSES
from seckill.config import LOCUST_WEB_PORT, PATH_SECKILL_GOODS_LIST


class SeckillGoodsQueryUser(HttpUser):
    host = GATEWAY_HOST
    wait_time = between(0.1, 0.5)

    @task
    def query_goods_list(self):
        with self.client.get(
            PATH_SECKILL_GOODS_LIST,
            name="seckill | GET query_goods_list",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
                return
            body = resp.json()
            if body.get("code") != "0000":
                resp.failure(f"biz error: {body.get('info')}")
                return
            goods = (body.get("data") or {}).get("seckillGoodsList") or []
            if not goods:
                resp.failure("seckillGoodsList is empty — check DB/activity config")
                return
            resp.success()


if __name__ == "__main__":
    cmd = [
        sys.executable, "-m", "locust",
        "-f", __file__, "SeckillGoodsQueryUser",
        "--host", GATEWAY_HOST,
        "--web-port", str(LOCUST_WEB_PORT),
        "--processes", str(LOCUST_PROCESSES),
    ]
    sys.exit(subprocess.run(cmd).returncode)
