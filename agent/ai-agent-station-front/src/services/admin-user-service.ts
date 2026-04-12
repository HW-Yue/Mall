/**
 * 管理员用户API服务
 */

import { API_ENDPOINTS, DEFAULT_HEADERS } from '../config';

// 定义API响应格式
export interface ApiResponse<T> {
  code: string;
  info: string;
  data: T;
}

/**
 * 管理员用户API服务类
 */
export class AdminUserService {
  private static readonly BASE_URL = API_ENDPOINTS.ADMIN_USER.BASE;

  /**
   * 验证管理员用户登录
   * @param loginData 登录数据
   * @returns Promise<string | null> 登录成功返回 userId（或用户名），否则返回 null
   */
  static async validateAdminUserLogin(loginData: { username: string; password: string }): Promise<string | null> {
    try {
      const response = await fetch(`${this.BASE_URL}${API_ENDPOINTS.ADMIN_USER.VALIDATE_LOGIN}`, {
        method: 'POST',
        headers: DEFAULT_HEADERS,
        body: JSON.stringify(loginData),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result: ApiResponse<string | boolean> = await response.json();
      
      if (result.code === '0000') {
        // 兼容后端返回 true/false 或 userId 两种情况：
        if (typeof result.data === 'string') {
          return result.data;
        }
        return loginData.username;
      } else {
        console.error('登录验证失败:', result.info);
        return null;
      }
    } catch (error) {
      console.error('登录验证请求失败:', error);
      return null;
    }
  }

  /**
   * 退出登录
   * 前端上只要清理 loginToken 即可，调用后端接口留作扩展
   */
  static async logout(): Promise<void> {
    try {
      // 若后端实现了退出接口，可以在 API_ENDPOINTS 中补充并在这里调用
      if ((API_ENDPOINTS as any).ADMIN_USER.LOGOUT) {
        await fetch(`${this.BASE_URL}${(API_ENDPOINTS as any).ADMIN_USER.LOGOUT}`, {
          method: 'POST',
          headers: DEFAULT_HEADERS,
        });
      }
    } catch (error) {
      console.error('调用退出登录接口失败:', error);
    }
  }
}