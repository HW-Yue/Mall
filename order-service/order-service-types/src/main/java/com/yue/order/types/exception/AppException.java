package com.yue.order.types.exception;

import com.yue.order.types.enums.ResponseCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 5317680961212299217L;

    private String code;
    private String info;

    public AppException(String code, String message) {
        this.code = code;
        this.info = message;
    }

    public AppException(ResponseCode responseCode) {
        this.code = responseCode.getCode();
        this.info = responseCode.getInfo();
    }

    @Override
    public String getMessage() {
        return info != null ? info : super.getMessage();
    }
}
