package cn.bugstack.mcp.server.weixin;

import cn.bugstack.mcp.server.weixin.domain.service.WeiXinNoticeService;
import cn.bugstack.mcp.server.weixin.infrastructure.gateway.IWeixinApiService;
import cn.bugstack.mcp.server.weixin.types.properties.WeiXinApiProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class McpServerApplication implements CommandLineRunner {

    @Resource
    private WeiXinApiProperties properties;

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public IWeixinApiService weixinApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weixin.qq.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        return retrofit.create(IWeixinApiService.class);
    }

    @Bean(name = "weixinAccessToken")
    public Cache<String, String> weixinAccessToken() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }

    @Bean
    public ToolCallbackProvider weixinTools(WeiXinNoticeService weiXinNoticeService) {
        return MethodToolCallbackProvider.builder().toolObjects(weiXinNoticeService).build();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("check properties ...");
        if (properties.getAppid() == null || properties.getAppsecret() == null || properties.getTouser() == null || properties.getTemplate_id() == null || properties.getOriginal_id() == null) {
            log.warn("weixin properties key is null, please set it in application.yml");
        } else {
            log.info("weixin properties key {}", properties.getAppid());
        }
    }

}
