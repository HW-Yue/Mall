package com.permissionsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserQueryParam {
    private Integer page=1;
    private Integer pageSize=10;
    private String name;
    private String username;
    private String email;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdEnd;


}
