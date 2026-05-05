package cn.bugstack.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest.BaseBuilder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigTest {

    @Test
    void routeConfigurationBuildsFallbackAndSampleRoutes() {
        RouteConfiguration configuration = new RouteConfiguration();
        RouteConfiguration.UriConfiguration uriConfiguration = new RouteConfiguration.UriConfiguration();
        assertThat(uriConfiguration.getHttp()).isEqualTo("http://gaga.plus");
        uriConfiguration.setHttp("http://example.com");

        assertThat(configuration.fallback().block()).isEqualTo("fallback");
        assertThat(uriConfiguration.getHttp()).isEqualTo("http://example.com");
    }

    @Test
    void requestRateLimiterResolvesIpApiAndParamKeys() {
        RequestRateLimiter limiter = new RequestRateLimiter();
        BaseBuilder<?> builder = MockServerHttpRequest.get("/gw/api/v1/mall/list?param-name=v1")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080));
        MockServerWebExchange exchange = MockServerWebExchange.from(builder);

        KeyResolver ip = limiter.ipKeyResolver();
        KeyResolver api = limiter.apiKeyResolver();
        KeyResolver param = limiter.paramKeyResolver();

        assertThat(ip.resolve(exchange).block()).isEqualTo("127.0.0.1");
        assertThat(api.resolve(exchange).block()).isEqualTo("/gw/api/v1/mall/list");
        assertThat(param.resolve(exchange).block()).isEqualTo("v1");
    }

    @Test
    void sentinelConverterAndYamlRoutesMatchGatewayPlan() {
        SentinelGatewayDataSourceConfig config = new SentinelGatewayDataSourceConfig();
        List<GatewayFlowRule> rules = config.sentinelJsonGwFlowConverter(new ObjectMapper())
                .convert("[{\"resource\":\"mall\",\"count\":1.0}]");
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getResource()).isEqualTo("mall");

        Properties props = loadYaml("application-dev.yml");
        var env = new org.springframework.mock.env.MockEnvironment();
        env.getPropertySources().addFirst(new PropertiesPropertySource("yaml", props));
        GatewayProperties gatewayProperties = Binder.get(env)
                .bind("spring.cloud.gateway", Bindable.of(GatewayProperties.class))
                .orElseThrow(IllegalStateException::new);
        Map<String, CorsConfiguration> cors = Binder.get(env)
                .bind("spring.cloud.gateway.globalcors.cors-configurations", Bindable.mapOf(String.class, CorsConfiguration.class))
                .orElseThrow(IllegalStateException::new);

        Map<String, org.springframework.cloud.gateway.route.RouteDefinition> routes = gatewayProperties.getRoutes()
                .stream().collect(Collectors.toMap(org.springframework.cloud.gateway.route.RouteDefinition::getId, r -> r));

        assertThat(routes).containsKeys("1", "2", "order-service", "group-buy-service", "seckill-service", "ops-agent-spring-ai");
        assertThat(routes.get("ops-agent-spring-ai").getMetadata()).containsEntry("response-timeout", -1);
        List<FilterDefinition> opsFilters = routes.get("ops-agent-spring-ai").getFilters();
        assertThat(opsFilters).anySatisfy(f -> assertThat(f.getName()).isEqualTo("RewritePath"));
        assertThat(cors).containsKey("/**");
        assertThat(cors.get("/**").getAllowedOriginPatterns()).contains("*");
        assertThat(props.getProperty("spring.cloud.sentinel.scg.fallback.response-body"))
                .contains("Blocked by Sentinel(gateway)");
    }

    private Properties loadYaml(String path) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(path));
        return factory.getObject();
    }
}
