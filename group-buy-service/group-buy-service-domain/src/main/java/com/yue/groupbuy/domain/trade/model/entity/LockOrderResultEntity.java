package com.yue.groupbuy.domain.trade.model.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LockOrderResultEntity {

    private String teamId;
    private boolean newTeam;
}
