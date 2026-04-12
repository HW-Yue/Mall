package com.permissionsystem.dto;

public class JwtResponse {
    private final String jwt;
    private final String username;
    // 您可以添加更多想返回给前端的用户信息

    public JwtResponse(String jwt, String username) {
        this.jwt = jwt;
        this.username = username;
    }

    // Getters
    public String getJwt() {
        return jwt;
    }

    public String getUsername() {
        return username;
    }
}