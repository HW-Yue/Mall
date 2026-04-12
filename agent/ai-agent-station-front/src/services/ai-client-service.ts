/**
 * AI客户端API服务
 */

import { API_ENDPOINTS, DEFAULT_HEADERS } from '../config';

// 定义响应数据类型
export interface AiClientResponseDTO {
  id: number;
  clientId: string;
  clientName: string;
  description?: string;
  status: number;
  createTime: string;
  updateTime: string;
  /** 客户端类型槽位，用于按策略二级联动筛选 */
  clientType?: string;
}

// 定义API响应格式
export interface ApiResponse<T> {
  code: string;
  info: string;
  data: T;
}

/**
 * AI客户端API服务类
 */
export class AiClientService {
  private static readonly BASE_URL = API_ENDPOINTS.AI_CLIENT.BASE;

  /**
   * 查询所有AI客户端配置
   */
  static async queryAllAiClients(): Promise<AiClientResponseDTO[]> {
    try {
      const response = await fetch(`${this.BASE_URL}${API_ENDPOINTS.AI_CLIENT.QUERY_ALL}`, {
        method: 'GET',
        headers: DEFAULT_HEADERS,
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result: ApiResponse<AiClientResponseDTO[]> = await response.json();
      
      if (result.code === '0000') {
        return result.data || [];
      } else {
        throw new Error(result.info || '查询失败');
      }
    } catch (error) {
      console.error('查询AI客户端配置失败:', error);
      // 返回空数组作为降级处理
      return [];
    }
  }

  /**
   * 查询所有启用的AI客户端配置
   */
  static async queryEnabledAiClients(): Promise<AiClientResponseDTO[]> {
    try {
      const response = await fetch(`${this.BASE_URL}${API_ENDPOINTS.AI_CLIENT.QUERY_ENABLED}`, {
        method: 'GET',
        headers: DEFAULT_HEADERS,
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result: ApiResponse<AiClientResponseDTO[]> = await response.json();
      
      if (result.code === '0000') {
        return result.data || [];
      } else {
        throw new Error(result.info || '查询失败');
      }
    } catch (error) {
      console.error('查询启用的AI客户端配置失败:', error);
      // 返回空数组作为降级处理
      return [];
    }
  }

  /**
   * 根据 client_type 查询客户端实例列表（二级联动步骤 2）
   * 若后端未实现该接口，则降级为 queryEnabledAiClients 并按 clientType 过滤（需后端在列表中返回 clientType）
   */
  static async queryClientsByClientType(clientType: string): Promise<AiClientResponseDTO[]> {
    try {
      const url = `${this.BASE_URL}${API_ENDPOINTS.AI_CLIENT.QUERY_BY_CLIENT_TYPE}?clientType=${encodeURIComponent(clientType)}`;
      const response = await fetch(url, { method: 'GET', headers: DEFAULT_HEADERS });
      if (!response.ok) {
        return this.queryEnabledAiClients().then((list) =>
          list.filter((c) => (c.clientType ?? '') === clientType)
        );
      }
      const result: ApiResponse<AiClientResponseDTO[]> = await response.json();
      if (result.code === '0000') return result.data || [];
      return this.queryEnabledAiClients().then((list) =>
        list.filter((c) => (c.clientType ?? '') === clientType)
      );
    } catch {
      return this.queryEnabledAiClients().then((list) =>
        list.filter((c) => (c.clientType ?? '') === clientType)
      );
    }
  }
}