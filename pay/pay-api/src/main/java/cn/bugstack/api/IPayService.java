package cn.bugstack.api;

import cn.bugstack.api.dto.CreatePayRequestDTO;

import cn.bugstack.api.response.Response;
import com.alipay.api.AlipayApiException;

import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;

public interface IPayService {

    public Response<String> createPayOrder( CreatePayRequestDTO createPayRequestDTO);
    public String payNotify(HttpServletRequest request)throws AlipayApiException, ParseException;
    Response<String> activePayNotify(String outTradeNo);
}
