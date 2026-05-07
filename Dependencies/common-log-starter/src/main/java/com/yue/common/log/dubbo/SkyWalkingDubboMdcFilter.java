package com.yue.common.log.dubbo;

import com.yue.common.log.CommonLogConstants;
import com.yue.common.log.skywalking.SkyWalkingTraceIds;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.MDC;

/**
 * Aligns Dubbo RPC business logs with the current SkyWalking trace id.
 */
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class SkyWalkingDubboMdcFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String mdcKey = CommonLogConstants.MDC_KEY;
        String traceId = resolveTraceId(invocation, mdcKey);
        String originalTraceId = MDC.get(mdcKey);

        try {
            if (traceId != null) {
                MDC.put(mdcKey, traceId);
                invocation.setAttachmentIfAbsent(mdcKey, traceId);
            }
            return invoker.invoke(invocation);
        } finally {
            restoreTraceId(mdcKey, originalTraceId);
        }
    }

    private String resolveTraceId(Invocation invocation, String mdcKey) {
        String traceId = SkyWalkingTraceIds.currentOrNull();
        if (isNotBlank(traceId)) {
            return traceId;
        }

        traceId = MDC.get(mdcKey);
        if (isNotBlank(traceId)) {
            return traceId;
        }

        traceId = invocation.getAttachment(mdcKey);
        if (isNotBlank(traceId)) {
            return traceId;
        }

        String defaultTraceId = invocation.getAttachment(CommonLogConstants.DEFAULT_MDC_KEY);
        return isNotBlank(defaultTraceId) ? defaultTraceId : null;
    }

    private void restoreTraceId(String mdcKey, String originalTraceId) {
        if (originalTraceId == null) {
            MDC.remove(mdcKey);
        } else {
            MDC.put(mdcKey, originalTraceId);
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
