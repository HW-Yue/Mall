package com.permissionsystem.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reply {
    private String type;
    private Object content; // 根据您的接口定义，这里可能是字符串或其他对象
}