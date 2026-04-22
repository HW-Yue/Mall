"""
商城（Mall）压测配置
──────────────────────────────
对应 mall-service 的前台查询接口。
"""

# ── 接口路径 ────────────────────────────────────────
PATH_MALL_CATEGORY_LIST = "/gw/api/v1/mall/index/query_category_type_list"
PATH_MALL_GOODS_PAGE = "/gw/api/v1/mall/index/query_goods_page"
PATH_MALL_SKU_DETAIL = "/gw/api/v1/mall/index/query_sku_detail"

# ── 分页参数 ────────────────────────────────────────
DEFAULT_PAGE_NUM = 1
DEFAULT_PAGE_SIZE = 10

# ── 类目 ID 候选池 ──────────────────────────────────
# 每个 task 随机选一个，让流量分散到多个类目。
# 即使后端没有对应类目、返回空列表，也算成功（我们关注的是网关+mall 的 RT/吞吐）。
CATEGORY_ID_POOL = [1, 2, 3, 4, 5]

# 不传 categoryId（查全部）的概率
CATEGORY_ALL_RATIO = 0.3

# ── Locust Web UI（与 seckill=7321 错开）─────────────
LOCUST_WEB_PORT = 7322
