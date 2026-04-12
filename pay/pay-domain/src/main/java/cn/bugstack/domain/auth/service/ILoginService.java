package cn.bugstack.domain.auth.service;

import java.io.IOException;

public interface ILoginService {

    String createQrCodeTicket() throws Exception;

    String createQrCodeTicket(String sceneStr) throws Exception;

    String checkLogin(String ticket);

    String checkLogin(String ticket, String sceneStr);

    void saveLoginState(String ticket, String openid) throws IOException;

    /**
     * 微信扫码后未绑定用户时，用 ticket 注册并绑定账号。
     * @return 注册成功返回 username（作为前端 loginToken），失败返回 null
     */
    String register(String ticket, String username, String password);
}
