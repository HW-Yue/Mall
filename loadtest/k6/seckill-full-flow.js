import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://100.86.250.112:8090";
const mode = (__ENV.MODE || "full").toLowerCase(); // goods | lock | full

const activityId = Number(__ENV.ACTIVITY_ID || 200001);
const productId = __ENV.PRODUCT_ID || "1001";
const source = __ENV.SOURCE || "s01";
const channel = __ENV.CHANNEL || "c01";

const pollRetries = Number(__ENV.POLL_RETRIES || 10);
const pollIntervalSec = Number(__ENV.POLL_INTERVAL_SEC || 0.5);
const thinkTimeSec = Number(__ENV.THINK_TIME_SEC || 0.1);
const executorMode = (__ENV.EXECUTOR_MODE || "ramping-vus").toLowerCase(); // ramping-vus | constant-arrival-rate | ramping-arrival-rate
const preset = (__ENV.PRESET || "default").toLowerCase(); // default | 100k | rps-10k-50k

const testTag = __ENV.TEST_TAG || "k6";
const lockOnlyIsTest = (__ENV.LOCK_ONLY_IS_TEST || "true").toLowerCase() === "true";
const fullFlowIsTest = (__ENV.FULL_FLOW_IS_TEST || "false").toLowerCase() === "true";

const PATH_SECKILL_GOODS_LIST = "/gw/api/v1/seckill/market/query_goods_list";
const PATH_SECKILL_CREATE_ORDER = "/gw/api/v1/seckill/trade/create_pay_order";
const PATH_ORDER_QUERY_SECKILL = "/gw/api/v1/order/query_seckill_order";
const PATH_ORDER_GET_PAY_URL = "/gw/api/v1/order/get_pay_url";

const flowSuccessRate = new Rate("full_flow_success_rate");
const bizErrorCount = new Counter("biz_error_count");

function buildScenario() {
  if (executorMode === "ramping-arrival-rate") {
    if (preset === "rps-10k-50k") {
      return {
        executor: "ramping-arrival-rate",
        startRate: Number(__ENV.START_RATE || 10000),
        timeUnit: __ENV.TIME_UNIT || "1s",
        preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 10000),
        maxVUs: Number(__ENV.MAX_VUS || 60000),
        stages: [
          { duration: __ENV.STAGE_1_DURATION || "2m", target: Number(__ENV.STAGE_1_TARGET || 10000) },
          { duration: __ENV.STAGE_2_DURATION || "2m", target: Number(__ENV.STAGE_2_TARGET || 20000) },
          { duration: __ENV.STAGE_3_DURATION || "2m", target: Number(__ENV.STAGE_3_TARGET || 30000) },
          { duration: __ENV.STAGE_4_DURATION || "2m", target: Number(__ENV.STAGE_4_TARGET || 40000) },
          { duration: __ENV.STAGE_5_DURATION || "2m", target: Number(__ENV.STAGE_5_TARGET || 50000) },
          { duration: __ENV.STAGE_6_DURATION || "1m", target: Number(__ENV.STAGE_6_TARGET || 0) },
        ],
      };
    }
    return {
      executor: "ramping-arrival-rate",
      startRate: Number(__ENV.START_RATE || 1000),
      timeUnit: __ENV.TIME_UNIT || "1s",
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 2000),
      maxVUs: Number(__ENV.MAX_VUS || 20000),
      stages: [
        { duration: __ENV.STAGE_1_DURATION || "1m", target: Number(__ENV.STAGE_1_TARGET || 2000) },
        { duration: __ENV.STAGE_2_DURATION || "1m", target: Number(__ENV.STAGE_2_TARGET || 5000) },
        { duration: __ENV.STAGE_3_DURATION || "30s", target: Number(__ENV.STAGE_3_TARGET || 0) },
      ],
    };
  }

  if (executorMode === "constant-arrival-rate") {
    if (preset === "100k") {
      return {
        executor: "constant-arrival-rate",
        rate: Number(__ENV.RATE || 100000),
        timeUnit: __ENV.TIME_UNIT || "1s",
        duration: __ENV.DURATION || "2m",
        preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 20000),
        maxVUs: Number(__ENV.MAX_VUS || 120000),
      };
    }
    return {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.RATE || 5000),
      timeUnit: __ENV.TIME_UNIT || "1s",
      duration: __ENV.DURATION || "1m",
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 2000),
      maxVUs: Number(__ENV.MAX_VUS || 20000),
    };
  }

  if (preset === "100k") {
    return {
      executor: "ramping-vus",
      startVUs: Number(__ENV.START_VUS || 1000),
      stages: [
        { duration: __ENV.STAGE_1_DURATION || "2m", target: Number(__ENV.STAGE_1_TARGET || 20000) },
        { duration: __ENV.STAGE_2_DURATION || "3m", target: Number(__ENV.STAGE_2_TARGET || 50000) },
        { duration: __ENV.STAGE_3_DURATION || "3m", target: Number(__ENV.STAGE_3_TARGET || 100000) },
        { duration: __ENV.STAGE_4_DURATION || "2m", target: Number(__ENV.STAGE_4_TARGET || 0) },
      ],
      gracefulRampDown: __ENV.GRACEFUL_RAMP_DOWN || "30s",
    };
  }

  return {
    executor: "ramping-vus",
    startVUs: Number(__ENV.START_VUS || 10),
    stages: [
      { duration: __ENV.STAGE_1_DURATION || "30s", target: Number(__ENV.STAGE_1_TARGET || 50) },
      { duration: __ENV.STAGE_2_DURATION || "1m", target: Number(__ENV.STAGE_2_TARGET || 200) },
      { duration: __ENV.STAGE_3_DURATION || "30s", target: Number(__ENV.STAGE_3_TARGET || 0) },
    ],
    gracefulRampDown: __ENV.GRACEFUL_RAMP_DOWN || "10s",
  };
}

export const options = {
  scenarios: {
    seckill_flow: {
      ...buildScenario(),
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<800"],
    full_flow_success_rate: ["rate>0.90"],
  },
};

function headers() {
  return {
    headers: {
      "Content-Type": "application/json",
      "X-Load-Test": "k6",
      "X-Test-Tag": testTag,
    },
  };
}

function buildUserId() {
  return `k6_${__VU}_${__ITER}_${Date.now()}`;
}

function isBizOk(body) {
  return body && body.code === "0000";
}

function parseJsonSafe(resp) {
  try {
    return resp.json();
  } catch (e) {
    return null;
  }
}

function queryGoodsList() {
  const resp = http.get(`${baseUrl}${PATH_SECKILL_GOODS_LIST}`, headers());
  const body = parseJsonSafe(resp);
  const goods = body?.data?.seckillGoodsList || [];

  const ok = check(resp, {
    "goods:list status=200": (r) => r.status === 200,
    "goods:list biz=0000": () => isBizOk(body),
    "goods:list non-empty": () => goods.length > 0,
  });

  if (!ok && body?.code && body.code !== "0000") {
    bizErrorCount.add(1);
  }

  return ok;
}

function createSeckillOrder(isTestMode) {
  const userId = buildUserId();
  const payload = JSON.stringify({
    userId,
    productId,
    activityId,
    source,
    channel,
    goodsName: "k6压测商品",
    goodsImageUrl: "",
    isTest: isTestMode,
  });

  const resp = http.post(`${baseUrl}${PATH_SECKILL_CREATE_ORDER}`, payload, headers());
  const body = parseJsonSafe(resp);
  const token = body?.data?.seckillToken || "";

  const ok = check(resp, {
    "lock:create status=200": (r) => r.status === 200,
    "lock:create biz=0000": () => isBizOk(body),
    "lock:create token exists": () => token.length > 0,
  });

  if (!ok && body?.code && body.code !== "0000") {
    bizErrorCount.add(1);
  }

  return {
    ok,
    userId,
    token,
    code: body?.code || "",
  };
}

function pollOrderId(seckillToken) {
  for (let i = 0; i < pollRetries; i += 1) {
    const resp = http.post(
      `${baseUrl}${PATH_ORDER_QUERY_SECKILL}`,
      JSON.stringify({ seckillToken }),
      headers()
    );
    const body = parseJsonSafe(resp);
    const status = body?.data?.status;
    const orderId = body?.data?.orderId || "";

    const ok = check(resp, {
      "order:poll status=200": (r) => r.status === 200,
      "order:poll biz=0000": () => isBizOk(body),
    });

    if (!ok) {
      if (body?.code && body.code !== "0000") {
        bizErrorCount.add(1);
      }
      return "";
    }

    if (status === 1 && orderId) {
      return orderId;
    }

    sleep(pollIntervalSec);
  }

  return "";
}

function getPayUrl(userId, orderId) {
  const resp = http.post(
    `${baseUrl}${PATH_ORDER_GET_PAY_URL}`,
    JSON.stringify({ userId, orderId }),
    headers()
  );
  const body = parseJsonSafe(resp);
  const payUrl = body?.data || "";

  const ok = check(resp, {
    "pay:url status=200": (r) => r.status === 200,
    "pay:url biz=0000": () => isBizOk(body),
    "pay:url exists": () => typeof payUrl === "string" && payUrl.length > 0,
  });

  if (!ok && body?.code && body.code !== "0000") {
    bizErrorCount.add(1);
  }

  return ok;
}

export default function () {
  if (mode === "goods") {
    queryGoodsList();
    sleep(thinkTimeSec);
    return;
  }

  if (mode === "lock") {
    createSeckillOrder(lockOnlyIsTest);
    sleep(thinkTimeSec);
    return;
  }

  const lockResult = createSeckillOrder(fullFlowIsTest);
  if (!lockResult.ok || !lockResult.token) {
    flowSuccessRate.add(0);
    sleep(thinkTimeSec);
    return;
  }

  const orderId = pollOrderId(lockResult.token);
  if (!orderId) {
    flowSuccessRate.add(0);
    sleep(thinkTimeSec);
    return;
  }

  const payOk = getPayUrl(lockResult.userId, orderId);
  flowSuccessRate.add(payOk ? 1 : 0);
  sleep(thinkTimeSec);
}
