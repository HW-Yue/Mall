package cn.bugstack.api.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {

    /** 微信扫码后的 ticket，用于关联 openid */
    private String ticket;
    private String username;
    private String password;
}
