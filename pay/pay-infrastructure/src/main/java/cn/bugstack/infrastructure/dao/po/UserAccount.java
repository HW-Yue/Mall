package cn.bugstack.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAccount {

    private Long id;
    private String username;
    private String password;
    private String openid;
    private Date createTime;
    private Date updateTime;
}
