package cn.bugstack.mcp.server.weixin.domain.adapter;

import cn.bugstack.mcp.server.weixin.domain.model.WeiXinNoticeFunctionRequest;
import cn.bugstack.mcp.server.weixin.domain.model.WeiXinNoticeFunctionResponse;

import java.io.IOException;

public interface IWeiXiPort {
    WeiXinNoticeFunctionResponse weixinNotice(WeiXinNoticeFunctionRequest request) throws IOException;

}
