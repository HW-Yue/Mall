package cn.bugstack.trigger.http;

import cn.bugstack.api.IAuthService;
import cn.bugstack.api.dto.RegisterRequestDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.auth.service.ILoginService;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController()
@RequestMapping("/api/v1/login-pay/login/")
public class LoginController implements IAuthService {

    @Resource
    private ILoginService loginService;

    /**
     * http://xfg-studio.natapp1.cc/api/v1/login/weixin_qrcode_ticket
     * @return
     */
    @RequestMapping(value = "weixin_qrcode_ticket", method = RequestMethod.GET)
    @Override
    public Response<String> weixinQrCodeTicket() {
        try {
            String qrCodeTicket = loginService.createQrCodeTicket();
            log.info("生成微信扫码登录 ticket:{}", qrCodeTicket);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(qrCodeTicket)
                    .build();
        } catch (Exception e) {
            log.error("生成微信扫码登录 ticket 失败", e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * http://xfg-studio.natapp1.cc/api/v1/login/weixin_qrcode_ticket_scene?sceneStr=
     * @return
     */
    @RequestMapping(value = "weixin_qrcode_ticket_scene", method = RequestMethod.GET)
    @Override
    public Response<String> weixinQrCodeTicket(@RequestParam String sceneStr) {
        try {
            String qrCodeTicket = loginService.createQrCodeTicket(sceneStr);
            log.info("生成微信扫码登录 ticket:{}", qrCodeTicket);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(qrCodeTicket)
                    .build();
        } catch (Exception e) {
            log.error("生成微信扫码登录 ticket 失败", e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * http://xfg-studio.natapp1.cc/api/v1/login/check_login
     */
    @RequestMapping(value = "check_login", method = RequestMethod.GET)
    @Override
    public Response<String> checkLogin(@RequestParam String ticket) {
        try {
            String openidToken = loginService.checkLogin(ticket);
            log.info("扫码检测登录结果 ticket:{} openidToken:{}", ticket, openidToken);
            if (StringUtils.isNotBlank(openidToken)) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.SUCCESS.getCode())
                        .info(Constants.ResponseCode.SUCCESS.getInfo())
                        .data(openidToken)
                        .build();
            } else {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.NO_LOGIN.getCode())
                        .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                        .build();
            }
        } catch (Exception e) {
            log.error("扫码检测登录结果失败 ticket:{}", ticket, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "check_login_scene", method = RequestMethod.GET)
    @Override
    public Response<String> checkLogin(@RequestParam String ticket, @RequestParam String sceneStr) {
        try {
            String openidToken = loginService.checkLogin(ticket, sceneStr);
            log.info("扫码检测登录结果 ticket:{} openidToken:{} sceneStr:{}", ticket, openidToken, sceneStr);
            if (StringUtils.isNotBlank(openidToken)) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.SUCCESS.getCode())
                        .info(Constants.ResponseCode.SUCCESS.getInfo())
                        .data(openidToken)
                        .build();
            } else {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.NO_LOGIN.getCode())
                        .info(Constants.ResponseCode.NO_LOGIN.getInfo())
                        .build();
            }
        } catch (Exception e) {
            log.error("扫码检测登录结果失败 ticket:{}", ticket, e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 微信扫码后未绑定用户时，前端提交绑定：ticket + 用户名 + 密码，注册并绑定。
     * 返回 username，前端存为 loginToken（与 check_login 一致，前端显示为用户名）。
     */
    @RequestMapping(value = "register", method = RequestMethod.POST)
    @Override
    public Response<String> register(@RequestBody RegisterRequestDTO request) {
        try {
            if (request == null || StringUtils.isAnyBlank(request.getTicket(), request.getUsername(), request.getPassword())) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("请填写用户名和密码")
                        .build();
            }
            String username = loginService.register(request.getTicket(), request.getUsername(), request.getPassword());
            if (StringUtils.isBlank(username)) {
                return Response.<String>builder()
                        .code(Constants.ResponseCode.UN_ERROR.getCode())
                        .info("绑定失败，ticket 无效或用户名已存在")
                        .build();
            }
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(username)
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("注册绑定失败", e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
