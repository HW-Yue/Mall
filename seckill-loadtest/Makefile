PYTHON := .venv/bin/python
PIP := .venv/bin/pip

.PHONY: init mock goods lock full k6-goods k6-lock k6-full k6-100k k6-rps-10k-50k

init:
	python3 -m venv .venv
	$(PIP) install -r requirements.txt

mock:
	$(PYTHON) locust/mock/alipay.py

goods:
	$(PYTHON) locust/seckill/goods_query.py

lock:
	$(PYTHON) locust/seckill/lock_only.py

full:
	$(PYTHON) locust/full_flow/seckill_to_pay.py

k6-goods:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=goods k6 run k6/seckill-full-flow.js

k6-lock:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=lock LOCK_ONLY_IS_TEST=true k6 run k6/seckill-full-flow.js

k6-full:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=full FULL_FLOW_IS_TEST=false k6 run k6/seckill-full-flow.js

k6-100k:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=lock PRESET=100k EXECUTOR_MODE=ramping-vus LOCK_ONLY_IS_TEST=true k6 run k6/seckill-full-flow.js

k6-rps-10k-50k:
	K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PORT=5665 MODE=goods PRESET=rps-10k-50k EXECUTOR_MODE=ramping-arrival-rate TIME_UNIT=1s PRE_ALLOCATED_VUS=10000 MAX_VUS=60000 k6 run k6/seckill-full-flow.js
