package com.yue.common.log.conditional;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * yue.log.trace.enabled 为 true 且 provider 为 skywalking
 */
public class OnSkyWalkingTraceModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!context.getEnvironment().getProperty("yue.log.trace.enabled", Boolean.class, true)) {
            return false;
        }
        return "skywalking".equalsIgnoreCase(
                context.getEnvironment().getProperty("yue.log.trace.provider", "legacy"));
    }
}
