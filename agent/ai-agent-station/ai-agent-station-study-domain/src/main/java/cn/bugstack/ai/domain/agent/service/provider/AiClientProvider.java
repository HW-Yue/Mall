package cn.bugstack.ai.domain.agent.service.provider;

import cn.bugstack.ai.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.bugstack.ai.domain.agent.service.IArmoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Client 按需提供者：从 Spring 容器获取 ChatClient，若不存在则触发运行时局部装配后再次获取。
 * 用于延迟加载（Lazy Loading），避免启动时全量装配带来的启动慢与内存占用高。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 */
@Slf4j
@Component
public class AiClientProvider {

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private IArmoryService armoryService;

    /** 按 clientId 加锁，避免高并发下对同一 clientId 重复装配 */
    private final ConcurrentHashMap<String, Object> clientLocks = new ConcurrentHashMap<>();

    /**
     * 获取指定 clientId 对应的 ChatClient。若容器中尚无该 Bean，则先执行局部装配再返回。
     * 使用按 clientId 的双重检查锁，保证同一 clientId 只装配一次。
     *
     * @param clientId 客户端 ID
     * @return ChatClient 实例，不会为 null
     * @throws cn.bugstack.ai.types.exception.BizException 当运行时装配失败时（如配置错误、API 不可用等）
     */
    public ChatClient getChatClient(String clientId) {
        String beanName = AiAgentEnumVO.AI_CLIENT.getBeanName(clientId);

        if (applicationContext.containsBean(beanName)) {
            return applicationContext.getBean(beanName, ChatClient.class);
        }

        Object lock = clientLocks.computeIfAbsent(clientId, k -> new Object());
        synchronized (lock) {
            if (!applicationContext.containsBean(beanName)) {
                log.info("运行时动态装配 AI Client: {}", clientId);
                armoryService.armoryByClientId(clientId);
            }
        }

        return applicationContext.getBean(beanName, ChatClient.class);
    }
}
