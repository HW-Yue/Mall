package com.yue.seckill.types.exception;

import com.yue.seckill.types.enums.ResponseCode;

public class AppException extends RuntimeException {

    private final String code;
    private final String info;

    public AppException(ResponseCode responseCode) {
        super(responseCode.getInfo());
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    public AppException(String code, String info) {
        super(info);
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }
}
