/**
 * 微信登录 & 绑定 注册 API 服务
 */

import { DEFAULT_HEADERS } from '../config';
import type { ApiResponse } from './admin-user-service';

// 复用老项目的登录网关地址
const WECHAT_LOGIN_BASE_URL = 'http://127.0.0.1:8080/api/v1/login';

export interface WechatRegisterRequestDTO {
  ticket: string;
  username: string;
  password: string;
}

export class WechatLoginService {
  /**
   * 获取微信扫码登录 ticket
   */
  static async getWeixinQrTicket(): Promise<string | null> {
    try {
      const resp = await fetch(`${WECHAT_LOGIN_BASE_URL}/weixin_qrcode_ticket`, {
        method: 'GET',
        headers: DEFAULT_HEADERS,
      });

      if (!resp.ok) {
        console.error('获取微信登录 ticket 失败，HTTP 状态码:', resp.status);
        return null;
      }

      const result: ApiResponse<string> = await resp.json();
      if (result.code === '0000' && result.data) {
        return result.data;
      }

      console.error('获取微信登录 ticket 失败:', result.info);
      return null;
    } catch (e) {
      console.error('请求微信登录 ticket 异常:', e);
      return null;
    }
  }

  /**
   * 轮询检查扫码登录状态
   */
  static async checkLogin(ticket: string): Promise<ApiResponse<string> | null> {
    try {
      const resp = await fetch(
        `${WECHAT_LOGIN_BASE_URL}/check_login?ticket=${encodeURIComponent(ticket)}`,
        {
          method: 'GET',
          headers: DEFAULT_HEADERS,
        }
      );

      if (!resp.ok) {
        console.error('检查微信登录状态失败，HTTP 状态码:', resp.status);
        return null;
      }

      const result: ApiResponse<string> = await resp.json();
      return result;
    } catch (e) {
      console.error('检查微信登录状态异常:', e);
      return null;
    }
  }

  /**
   * 微信扫码后，注册并绑定账号
   */
  static async registerAndBind(data: WechatRegisterRequestDTO): Promise<ApiResponse<string> | null> {
    try {
      const resp = await fetch(`${WECHAT_LOGIN_BASE_URL}/register`, {
        method: 'POST',
        headers: DEFAULT_HEADERS,
        body: JSON.stringify(data),
      });

      if (!resp.ok) {
        console.error('微信注册绑定失败，HTTP 状态码:', resp.status);
        return null;
      }

      const result: ApiResponse<string> = await resp.json();
      return result;
    } catch (e) {
      console.error('微信注册绑定异常:', e);
      return null;
    }
  }
}

