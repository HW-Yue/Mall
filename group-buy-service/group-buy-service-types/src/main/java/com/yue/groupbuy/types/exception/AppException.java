package com.yue.groupbuy.types.exception;

import com.yue.groupbuy.types.enums.ResponseCode;

public class AppException extends RuntimeException {

    private final String code;
    private final String info;

    public AppException(String code, String info) {
        super(info);
        this.code = code;
        this.info = info;
    }

    public AppException(ResponseCode responseCode) {
        super(responseCode.getInfo());
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    public AppException(String message) {
        super(message);
        this.code = ResponseCode.UN_ERROR.getCode();
        this.info = message;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }
}
