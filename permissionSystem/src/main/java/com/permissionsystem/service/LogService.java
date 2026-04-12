package com.permissionsystem.service;

import com.permissionsystem.dto.LogDTO;
import com.permissionsystem.dto.LogQueryParam;
import com.permissionsystem.dto.PageResult;

import java.util.List;

public interface LogService {


    PageResult page(LogQueryParam param);

    void delete(String ids);

    List<LogDTO> findLogs(LogQueryParam param);
}
