package com.yue.common.log.conditional;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * yue.log.trace.enabled（默认 true）且 provider 为 legacy（默认 / 未配置即为 legacy）
 */
public class OnLegacyTraceModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!context.getEnvironment().getProperty("yue.log.trace.enabled", Boolean.class, true)) {
            return false;
        }
        String provider = context.getEnvironment().getProperty("yue.log.trace.provider", "legacy");
        return "legacy".equalsIgnoreCase(provider);
    }
}
