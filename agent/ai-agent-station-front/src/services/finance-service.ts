/**
 * 计费/资产 API 服务
 */

import { API_ENDPOINTS, DEFAULT_HEADERS } from '../config';

export interface ApiResponse<T> {
  code?: string;
  info?: string;
  data?: T;
}

export interface FinanceAccountItemDTO {
  goodsType: string;
  resKey: string;
  totalBalance: number;
  frozenAmount: number;
  unit: string;
  displayName: string;
}

export interface FinanceDeliveryLogItemDTO {
  orderId: string;
  bizType: string;
  bizTypeName?: string;
  goodsType: string;
  resKey: string;
  resValue: number;
  unit: string;
  status: number; // 1-处理中, 2-成功, 3-失败
  createTime: string;
}

export interface FinanceDeliveryLogsPageDTO {
  list: FinanceDeliveryLogItemDTO[];
  total: number;
}

export interface FinanceDeliveryLogsQuery {
  pageNum: number;
  pageSize: number;
  bizType?: string;
  status?: number;
}

function getUserIdFromCookie(): string | undefined {
  try {
    const cookie = document.cookie || '';
    const segments = cookie.split(';');
    for (const seg of segments) {
      const [rawKey, ...rest] = seg.split('=');
      if (!rawKey) continue;
      const key = rawKey.trim();
      if (key === 'loginToken') {
        const value = rest.join('=');
        if (!value) return undefined;
        return decodeURIComponent(value);
      }
    }
    return undefined;
  } catch {
    return undefined;
  }
}

function getCurrentUserId(): string | undefined {
  // 当前约定：userId 只存放在 cookie.loginToken 中，且值就是 userId 本身
  return getUserIdFromCookie();
}

function unwrapData<T>(result: any): T {
  // 兼容后端直接返回 data（无 code 包裹）或 {code, info, data} 两种形式
  if (result && typeof result === 'object' && 'code' in result) {
    if (result.code === '0000') return result.data as T;
    throw new Error(result.info || '请求失败');
  }
  return result as T;
}

export class FinanceService {
  private static readonly BASE_URL = API_ENDPOINTS.FINANCE.BASE;

  static async getAccountList(): Promise<FinanceAccountItemDTO[]> {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error('未获取到用户ID，请重新登录后重试');
    }

    const response = await fetch(`${this.BASE_URL}${API_ENDPOINTS.FINANCE.ACCOUNT_LIST}`, {
      method: 'POST',
      headers: DEFAULT_HEADERS,
      body: JSON.stringify({ userId }),
    });
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const result = await response.json();
    return unwrapData<FinanceAccountItemDTO[]>(result);
  }

  static async getDeliveryLogs(query: FinanceDeliveryLogsQuery): Promise<FinanceDeliveryLogsPageDTO> {
    const userId = getCurrentUserId();
    if (!userId) {
      throw new Error('未获取到用户ID，请重新登录后重试');
    }

    const payload: any = {
      userId,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    };
    if (query.bizType) payload.bizType = query.bizType;
    if (typeof query.status === 'number') payload.status = query.status;

    const response = await fetch(`${this.BASE_URL}${API_ENDPOINTS.FINANCE.DELIVERY_LOGS}`, {
      method: 'POST',
      headers: DEFAULT_HEADERS,
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const result = await response.json();
    return unwrapData<FinanceDeliveryLogsPageDTO>(result);
  }
}

