package com.permissionsystem.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.permissionsystem.dto.LogDTO;
import com.permissionsystem.dto.LogQueryParam;
import com.permissionsystem.dto.PageResult;
import com.permissionsystem.mapper.LogMapper;
import com.permissionsystem.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {
    @Autowired
    private LogMapper logMapper;
    @Override
    public PageResult page(LogQueryParam param) {
        PageHelper.startPage(param.getPage(), param.getPageSize());
        List<LogDTO> logList = logMapper.list(param);
        Page<LogDTO> p=(Page<LogDTO>)logList;
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public void delete(String ids) {
        //ids转为list
        String[] idArray = ids.split(",");
        List<Integer> idsList = new ArrayList<>();
        for (String id : idArray) {
            idsList.add(Integer.parseInt(id));
        }
         logMapper.delete(idsList);
    }

    @Override
    public List<LogDTO> findLogs(LogQueryParam param) {
        List<LogDTO> logList = logMapper.list(param);
        return  logList;
    }
}
