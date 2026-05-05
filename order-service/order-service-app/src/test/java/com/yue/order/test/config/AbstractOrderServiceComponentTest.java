package com.yue.order.test.config;

import com.yue.order.OrderServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * order-service 组件测试基类。
 * <p>
 * 只激活 test-mock profile；Feign 依赖在具体测试类里用 @MockBean 或 MockitoBean 替换。
 * </p>
 */
@SpringBootTest(classes = OrderServiceApplication.class)
@ActiveProfiles("test-mock")
@Import(RocketMqMockTestConfig.class)
public abstract class AbstractOrderServiceComponentTest {
}
