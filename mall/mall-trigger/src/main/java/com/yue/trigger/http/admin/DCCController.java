package com.yue.trigger.http.admin;

import com.yue.api.IDCCController;
import com.yue.api.response.Response;
import com.yue.trigger.service.admin.DCCAppServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 动态配置管理
 * @create 2025-01-03 19:16
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/dcc/")
public class DCCController implements IDCCController {

    @Resource
    private DCCAppServiceImpl dccAppService;

    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value) {
        return dccAppService.updateConfig(key, value);
    }
}
