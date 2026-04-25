package com.yue.opsagent.springai.domain.opsroute.node;

public abstract class AbstractOpsRouteNode {

    public String name() {
        return getClass().getSimpleName().replace("Node", "");
    }
}
