package com.yue.opsagent.springai.agent.sub;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SubAgentRegistry {

    private final Map<String, ISubAgent> byDomainId;

    public SubAgentRegistry(Collection<ISubAgent> agents) {
        this.byDomainId = agents.stream().collect(Collectors.toMap(ISubAgent::domainId, Function.identity(), (a, b) -> a));
    }

    public ISubAgent require(String domainId) {
        ISubAgent s = byDomainId.get(domainId);
        if (s == null) {
            throw new IllegalArgumentException("Unknown subAgentId: " + domainId + ", known: " + byDomainId.keySet());
        }
        return s;
    }
}
