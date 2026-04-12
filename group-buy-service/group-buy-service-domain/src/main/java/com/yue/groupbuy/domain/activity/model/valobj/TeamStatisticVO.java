package com.yue.groupbuy.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamStatisticVO {

    /** 开团队伍数量 */
    private Integer allTeamCount;
    /** 成团队伍数量 */
    private Integer allTeamCompleteCount;
    /** 参团人数总量 */
    private Integer allTeamUserCount;

}
