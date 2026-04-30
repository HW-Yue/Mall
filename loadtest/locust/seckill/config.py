"""
秒杀压测配置
──────────────────────────────
与 DB / Redis 中真实活动对齐：
  活动 200001: AI模型整点秒杀      (库存 10000)
  活动 200002: 存储资源限时秒杀    (库存 10000)
  商品 1001/1003/1005 属于 200001
  商品 2002/2004/2005 属于 200002
"""

# ── 接口路径 ────────────────────────────────────────
PATH_SECKILL_GOODS_LIST = "/gw/api/v1/seckill/market/query_goods_list"
PATH_SECKILL_CREATE_ORDER = "/gw/api/v1/seckill/trade/create_pay_order"
PATH_ORDER_QUERY_SECKILL = "/gw/api/v1/order/query_seckill_order"
PATH_ORDER_GET_PAY_URL = "/gw/api/v1/order/get_pay_url"

# ── 业务参数 ────────────────────────────────────────
ACTIVITY_ID = 200001
PRODUCT_ID = "1001"
SOURCE = "s01"
CHANNEL = "c01"

# ── Mock 支付宝 ─────────────────────────────────────
# pay-service 的 alipay.gateway-url 改为 MOCK_ALIPAY_HOST 即可接管所有支付请求
MOCK_ALIPAY_PORT = 7000
MOCK_ALIPAY_HOST = f"http://100.86.250.112:{MOCK_ALIPAY_PORT}"
# mock_alipay 运行在宿主机，回调 pay 容器映射到宿主机的 test 端口
NOTIFY_URL = "http://127.0.0.1:18080/api/v1/alipay/alipay_notify_url"

# ── 轮询参数（full_flow 等待 MQ 建单）────────────────
POLL_MAX_RETRIES = 10
POLL_INTERVAL_SEC = 0.5

# ── Locust Web UI ───────────────────────────────────
LOCUST_WEB_PORT = 7321
