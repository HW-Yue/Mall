package com.yue.opsagent.springai.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 {@code ops-ai.sop.rules-dir} 下每个 YAML 文件解析为一条 {@link OpsAiProperties.Sop.Rule}，
 * 追加到已在 {@code application.yml} 中绑定的 {@link OpsAiProperties.Sop#getRules()} 之后。
 */
@Component
public class ExternalSopRulesLoader implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ExternalSopRulesLoader.class);

    private final OpsAiProperties opsAiProperties;

    public ExternalSopRulesLoader(OpsAiProperties opsAiProperties) {
        this.opsAiProperties = opsAiProperties;
    }

    @Override
    public void afterPropertiesSet() {
        String dir = opsAiProperties.getSop().getRulesDir();
        if (dir == null || dir.isBlank()) {
            return;
        }
        String base = dir.trim();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Set<Resource> unique = new LinkedHashSet<>();
        try {
            for (String suffix : List.of("*.yml", "*.yaml")) {
                Resource[] batch = resolver.getResources(base + suffix);
                unique.addAll(Arrays.asList(batch));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot scan SOP rules under " + base, e);
        }
        List<Resource> files = new ArrayList<>(unique);
        files.sort(Comparator.comparing(r -> r.getFilename() != null ? r.getFilename() : r.toString()));

        if (files.isEmpty()) {
            log.debug("No SOP rule files under {} (*.yml, *.yaml)", base);
            return;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<OpsAiProperties.Sop.Rule> loaded = new ArrayList<>();
        for (Resource resource : files) {
            if (!resource.exists() || !resource.isReadable()) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                OpsAiProperties.Sop.Rule rule = mapper.readValue(in, OpsAiProperties.Sop.Rule.class);
                loaded.add(rule);
                log.info("Loaded SOP rule from {} (match-alertname={})",
                        resource.getDescription(),
                        rule.getMatchAlertname());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse SOP rule file: " + resource.getDescription(), e);
            }
        }

        List<OpsAiProperties.Sop.Rule> merged = new ArrayList<>(opsAiProperties.getSop().getRules());
        merged.addAll(loaded);
        opsAiProperties.getSop().setRules(merged);
        log.info("SOP rules total: {} ({} from external files)", merged.size(), loaded.size());
    }
}
