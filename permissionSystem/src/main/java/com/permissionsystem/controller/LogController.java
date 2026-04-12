package com.permissionsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.permissionsystem.dto.LogDTO;
import com.permissionsystem.dto.LogQueryParam;
import com.permissionsystem.dto.PageResult;
import com.permissionsystem.dto.Result;
import com.permissionsystem.service.LogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/logs")
public class LogController {

    @Autowired
    LogService logService;
    // todo 查询日志列表
    @GetMapping
     public Result page(LogQueryParam param){
        log.info("查询日志列表");
        System.out.println("------------------------"+param.getOperationType());
        PageResult pageResult = logService.page(param);
        return Result.success(pageResult);
    }
    // todo 删除日志
    @DeleteMapping
    public Result deleteLog(@RequestParam String ids){
        log.info("删除日志");
        logService.delete(ids);
        return Result.success();
    }
    //todo 导出日志
    @GetMapping("/export")
    public void exportLogs(LogQueryParam param, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("操作日志", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 1. 查询数据
        List<LogDTO> logs = logService.findLogs(param);

        // 2. 导出到 Excel
        EasyExcel.write(response.getOutputStream(), LogDTO.class)
                .sheet("日志明细")
                .doWrite(logs);
    }

}
