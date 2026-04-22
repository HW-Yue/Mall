"""
跨服务共享的压测基础设施
────────────────────────────────
- GATEWAY_HOST         所有服务统一走 springcloud-gateway
- LOCUST_PROCESSES     Locust 默认 worker 进程数
- next_loadtest_user_id()  进程内严格递增、多进程间不冲突的 userId 生成器
"""
import itertools
import os
import threading

GATEWAY_HOST = os.getenv("GATEWAY_HOST", "http://100.86.250.112:8090")

LOCUST_PROCESSES = int(os.getenv("LOCUST_PROCESSES", "10"))

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
