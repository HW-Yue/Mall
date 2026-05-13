package com.yue.opsagent.springai.service;

import com.yue.opsagent.springai.domain.opsroute.OpsRunSession;
import com.yue.opsagent.springai.domain.opsroute.OpsRunSummary;

import java.util.List;

public interface OpsRunSummaryRepository {

    void upsert(OpsRunSession session);

    List<OpsRunSummary> findRecentRuns(int size);

    static OpsRunSummaryRepository noop() {
        return new OpsRunSummaryRepository() {
            @Override
            public void upsert(OpsRunSession session) {
            }

            @Override
            public List<OpsRunSummary> findRecentRuns(int size) {
                return List.of();
            }
        };
    }
}
