package com.yue.trigger.service.admin;

import cn.bugstack.wrench.dynamic.config.center.domain.model.valobj.AttributeVO;
import com.yue.api.response.Response;
import com.yue.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class DCCAppServiceImpl {

    @Resource(name = "dynamicConfigCenterRedisTopic")
    private RTopic dccTopic;

    public Response<Boolean> updateConfig(String key, String value) {
        try {
            dccTopic.publish(new AttributeVO(key, value));
            log.info("DCC 动态配置已发布 key:{} value:{}", key, value);
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("DCC 动态配置值变更失败 key:{} value:{}", key, value, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
