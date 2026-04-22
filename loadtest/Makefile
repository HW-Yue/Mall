PYTHON := .venv/bin/python
PIP := .venv/bin/pip

.PHONY: init \
        seckill-mock seckill-goods seckill-lock seckill-full \
        seckill-k6-goods seckill-k6-lock seckill-k6-full seckill-k6-100k seckill-k6-rps-10k-50k \
        mall-category mall-goods

# ── 初始化 ───────────────────────────────────────────
init:
	python3 -m venv .venv
	$(PIP) install -r requirements.txt

# ── Seckill ──────────────────────────────────────────
seckill-mock:
	$(PYTHON) locust/seckill/mock_alipay.py

seckill-goods:
	$(PYTHON) locust/seckill/goods_query.py

seckill-lock:
	$(PYTHON) locust/seckill/lock_only.py

seckill-full:
	$(PYTHON) locust/seckill/full_flow.py

seckill-k6-goods:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=goods \
		k6 run k6/seckill-full-flow.js

seckill-k6-lock:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=lock LOCK_ONLY_IS_TEST=true \
		k6 run k6/seckill-full-flow.js

seckill-k6-full:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=full FULL_FLOW_IS_TEST=false \
		k6 run k6/seckill-full-flow.js

seckill-k6-100k:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=lock PRESET=100k \
		EXECUTOR_MODE=ramping-vus LOCK_ONLY_IS_TEST=true \
		k6 run k6/seckill-full-flow.js

seckill-k6-rps-10k-50k:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=goods PRESET=rps-10k-50k \
		EXECUTOR_MODE=ramping-arrival-rate TIME_UNIT=1s \
		PRE_ALLOCATED_VUS=10000 MAX_VUS=60000 \
		k6 run k6/seckill-full-flow.js

# ── Mall ─────────────────────────────────────────────
mall-category:
	$(PYTHON) locust/mall/category_query.py

mall-goods:
	$(PYTHON) locust/mall/goods_query.py
