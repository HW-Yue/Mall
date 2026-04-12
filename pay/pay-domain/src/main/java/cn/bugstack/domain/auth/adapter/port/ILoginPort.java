package cn.bugstack.domain.auth.adapter.port;

import java.io.IOException;

public interface ILoginPort {

    String createQrCodeTicket() throws IOException;

    String createQrCodeTicket(String sceneStr) throws IOException;

    void sendLoginTemplate(String openid) throws IOException;

    /** 根据 openid 查询已绑定用户的 username，未绑定返回 null */
    String queryUsernameByOpenid(String openid);

    /** 绑定微信并注册用户，密码由 port 层做摘要后入库 */
    void registerUser(String openid, String username, String password);
}
