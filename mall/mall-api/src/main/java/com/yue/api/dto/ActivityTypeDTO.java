package com.yue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String typeName;
    private String typeCode;
    private Integer status;
}
