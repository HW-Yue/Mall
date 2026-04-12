package cn.bugstack.api;

import cn.bugstack.api.dto.RegisterRequestDTO;
import cn.bugstack.api.response.Response;

public interface IAuthService {

    Response<String> weixinQrCodeTicket();

    Response<String> weixinQrCodeTicket(String sceneStr);

    Response<String> checkLogin(String ticket);

    Response<String> checkLogin(String ticket, String sceneStr);

    /** 微信扫码后未绑定用户时，注册并绑定账号，返回 username 作为前端 loginToken */
    Response<String> register(RegisterRequestDTO request);
}
