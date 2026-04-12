package com.yue.api;

import com.yue.api.response.Response;

/**
 * DCC 动态配置 HTTP 契约
 */
public interface IDCCController {

    Response<Boolean> updateConfig(String key, String value);
}
