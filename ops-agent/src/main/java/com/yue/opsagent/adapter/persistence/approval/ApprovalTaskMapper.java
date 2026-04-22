package com.yue.opsagent.adapter.persistence.approval;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 审批任务 MyBatis Mapper。SQL 定义在
 * {@code classpath:mybatis/mapper/approval_task_mapper.xml}。
 *
 * <p>风格仿 mall 的 {@code ISkuDao}：{@code @Mapper} 注解 + XML 映射，
 * {@code resultMap id=dataMap}。</p>
 */
@Mapper
public interface ApprovalTaskMapper {

    int insert(ApprovalTaskPO po);

    /**
     * 更新状态并写回 decidedAt。返回受影响行数（用于幂等判断）。
     */
    int updateStatus(@Param("id") String id,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage,
                     @Param("decidedAt") OffsetDateTime decidedAt);

    /**
     * 仅更新 {@code reasoning} 字段（不触碰 status / decidedAt），供诊断子 Agent 异步回填用。
     * 返回受影响行数（0 表示任务不存在）。
     */
    int updateReasoning(@Param("id") String id,
                        @Param("reasoning") String reasoning);

    ApprovalTaskPO findById(@Param("id") String id);

    /**
     * @param status 为 null 时返回全部
     */
    List<ApprovalTaskPO> list(@Param("status") String status);

    /**
     * 把 PENDING 且 createdAt 早于 threshold 的任务批量置 EXPIRED，返回被改的 id 列表。
     * 调用端拿到 id 列表后再 {@link #findByIds(List)} 回查最新值以广播 SSE。
     */
    List<String> selectExpiredPendingIds(@Param("threshold") OffsetDateTime threshold);

    int expirePendingBefore(@Param("threshold") OffsetDateTime threshold,
                            @Param("errorMessage") String errorMessage,
                            @Param("decidedAt") OffsetDateTime decidedAt);

    List<ApprovalTaskPO> findByIds(@Param("ids") List<String> ids);
}
