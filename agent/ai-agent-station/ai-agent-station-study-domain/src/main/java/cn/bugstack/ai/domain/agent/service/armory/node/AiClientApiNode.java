package cn.bugstack.ai.domain.agent.service.armory.node;

import cn.bugstack.ai.domain.agent.model.entity.ArmoryCommandEntity;
import cn.bugstack.ai.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.bugstack.ai.domain.agent.model.valobj.AiClientApiVO;
import cn.bugstack.ai.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * OpenAI API配置节点
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/1 07:09
 */
@Slf4j
@Service
public class AiClientApiNode extends AbstractArmorySupport {

    @Resource
    private AiClientToolMcpNode aiClientToolMcpNode;

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，API 接口请求{}", JSON.toJSONString(requestParameter));

        List<AiClientApiVO> aiClientApiList = dynamicContext.getValue(dataName());

        if (aiClientApiList == null || aiClientApiList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client api");
            return router(requestParameter, dynamicContext);
        }

        RestClient.Builder loggingRestClientBuilder = resolveLoggingRestClientBuilder();
        WebClient.Builder loggingWebClientBuilder = resolveLoggingWebClientBuilder();

        for (AiClientApiVO aiClientApiVO : aiClientApiList) {
            OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                    .baseUrl(aiClientApiVO.getBaseUrl())
                    .apiKey(aiClientApiVO.getApiKey())
                    .completionsPath(aiClientApiVO.getCompletionsPath())
                    .embeddingsPath(aiClientApiVO.getEmbeddingsPath());
            if (loggingRestClientBuilder != null) {
                apiBuilder.restClientBuilder(loggingRestClientBuilder);
            }
            if (loggingWebClientBuilder != null) {
                apiBuilder.webClientBuilder(loggingWebClientBuilder);
            }
            OpenAiApi openAiApi = apiBuilder.build();

            // 注册 OpenAiApi Bean 对象
            registerBean(beanName(aiClientApiVO.getApiId()), OpenAiApi.class, openAiApi);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientToolMcpNode;
    }

    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_API.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_API.getDataName();
    }

    /**
     * 解析 dev/test 下可选的「AI 请求日志」RestClient.Builder（若存在则拦截并打印完整请求体）。
     * Bean 名与 app 模块 AiRequestLoggingConfiguration 中定义一致，生产环境不注册该 Bean。
     */
    private RestClient.Builder resolveLoggingRestClientBuilder() {
        try {
            return applicationContext.getBean("aiRequestLoggingRestClientBuilder", RestClient.Builder.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    /**
     * 解析 dev/test 下可选的「AI 请求日志」WebClient.Builder（若存在则拦截流式请求并打印完整请求体）。
     */
    private WebClient.Builder resolveLoggingWebClientBuilder() {
        try {
            return applicationContext.getBean("aiRequestLoggingWebClientBuilder", WebClient.Builder.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

}
