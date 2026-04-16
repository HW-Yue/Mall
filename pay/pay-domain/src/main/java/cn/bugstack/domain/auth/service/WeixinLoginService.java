package cn.bugstack.domain.auth.service;

import cn.bugstack.domain.auth.adapter.port.ILoginPort;
import com.google.common.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;

@Slf4j
@Service
public class WeixinLoginService implements ILoginService {

    @Resource
    private ILoginPort loginPort;
    @Resource
    private Cache<String, String> openidToken;

    @Override
    public String createQrCodeTicket() throws Exception {
        return loginPort.createQrCodeTicket();
    }

    @Override
    public String createQrCodeTicket(String sceneStr) throws Exception {
        String ticket = loginPort.createQrCodeTicket(sceneStr);
        // 保存浏览器指纹信息和ticket映射关系
        openidToken.put(sceneStr, ticket);
        return ticket;
    }

    /** 前端约定：未绑定用户时返回该常量，前端弹出绑定/注册框 */
    public static final String NEED_REGISTER = "__need_register__";

    @Override
    public String checkLogin(String ticket) {
        String openid = openidToken.getIfPresent(ticket);
        if (StringUtils.isBlank(openid)) return null;
        String username = loginPort.queryUsernameByOpenid(openid);
        return username != null ? username : NEED_REGISTER;
    }

    @Override
    public String checkLogin(String ticket, String sceneStr) {
        String cacheTicket = openidToken.getIfPresent(sceneStr);
        if (StringUtils.isBlank(cacheTicket) || !cacheTicket.equals(ticket)) return null;
        return checkLogin(ticket);
    }

    @Override
    public void saveLoginState(String ticket, String openid) throws IOException {
        openidToken.put(ticket, openid);
        loginPort.sendLoginTemplate(openid);
    }

    @Override
    public String register(String ticket, String username, String password) {
        if (StringUtils.isAnyBlank(ticket, username, password)) return null;
        String openid = openidToken.getIfPresent(ticket);
        if (StringUtils.isBlank(openid)) return null;
        loginPort.registerUser(openid, username, password);
        return username;
    }

}
