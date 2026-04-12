# ──────────────────────────────────────────────────────────────
#  压测全局配置
# ──────────────────────────────────────────────────────────────
import itertools
import os
import threading

# 所有请求走网关（端口 8090），前缀 /gw
GATEWAY_HOST = "http://localhost:8090"

# ── 接口路径 ─────────────────────────────────────────────────
# 秒杀服务
PATH_SECKILL_GOODS_LIST   = "/gw/api/v1/seckill/market/query_goods_list"
PATH_SECKILL_CREATE_ORDER = "/gw/api/v1/seckill/trade/create_pay_order"

# 订单服务
PATH_ORDER_QUERY_SECKILL  = "/gw/api/v1/order/query_seckill_order"
PATH_ORDER_GET_PAY_URL    = "/gw/api/v1/order/get_pay_url"

# ── 测试数据（与 DB / Redis 中真实活动对齐）─────────────────
# 从 seckill_service.sql 读取的实际数据：
#   活动 200001: AI模型整点秒杀 (库存 10000，供压测)
#   活动 200002: 存储资源限时秒杀 (库存 10000，供压测)
#   商品 1001/1003/1005 属于活动 200001
#   商品 2002/2004/2005 属于活动 200002
ACTIVITY_ID   = 200001     # 秒杀活动 ID
PRODUCT_ID    = "1001"     # 秒杀商品 ID（sms_seckill_sku.sku_id）
SOURCE        = "s01"
CHANNEL       = "c01"

_loadtest_user_lock = threading.Lock()
_loadtest_user_seq = itertools.count(1)


def next_loadtest_user_id() -> str:
    """
    每次调用生成新的 userId，进程内严格递增、不重复。
    带 os.getpid()，Locust 多 worker（多进程）时不同进程之间也不会撞号。
    """
    with _loadtest_user_lock:
        n = next(_loadtest_user_seq)
    return f"user_{os.getpid()}_{n:012d}"

# ── Mock 支付宝配置 ──────────────────────────────────────────
MOCK_ALIPAY_PORT = 7000
# pay 服务在 application-dev.yml 中将 alipay.gateway-url 改成下面这个地址，
# mock_alipay 就能接管所有支付宝请求
MOCK_ALIPAY_HOST = f"http://localhost:{MOCK_ALIPAY_PORT}"

# mock_alipay 收到支付后，向 pay 服务发送的回调地址
NOTIFY_URL = "http://localhost:9091/api/v1/alipay/alipay_notify_url"

# ── 轮询参数 ─────────────────────────────────────────────────
# full_flow 场景等待 order-service 异步建单的最大次数 × 间隔
POLL_MAX_RETRIES  = 10
POLL_INTERVAL_SEC = 0.5   # 每次轮询间隔（秒），总计最多等 5 秒
