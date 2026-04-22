"""
Mall 商城类目列表查询压测
──────────────────────────────────────────────
接口：GET /gw/api/v1/mall/index/query_category_type_list
无参数。

用途：比 goods_page 更轻量的接口，快速产生 sentinel_pass_qps / RT 指标。

直接启动：
  python locust/mall/category_query.py
  访问 http://localhost:7322
"""
import os
import subprocess
import sys

from locust import HttpUser, between, task

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from common import GATEWAY_HOST, LOCUST_PROCESSES
from mall.config import LOCUST_WEB_PORT, PATH_MALL_CATEGORY_LIST


class MallCategoryQueryUser(HttpUser):
    host = GATEWAY_HOST
    wait_time = between(0.05, 0.3)

    @task
    def query_category_type_list(self):
        with self.client.get(
            PATH_MALL_CATEGORY_LIST,
            name="mall | GET query_category_type_list",
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
                resp.failure(f"biz error [{body.get('code')}]: {body.get('info')}")
                return
            resp.success()


if __name__ == "__main__":
    cmd = [
        sys.executable, "-m", "locust",
        "-f", __file__, "MallCategoryQueryUser",
        "--host", GATEWAY_HOST,
        "--web-port", str(LOCUST_WEB_PORT),
        "--processes", str(LOCUST_PROCESSES),
    ]
    sys.exit(subprocess.run(cmd).returncode)
