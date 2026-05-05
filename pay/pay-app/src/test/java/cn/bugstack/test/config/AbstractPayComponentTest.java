package cn.bugstack.test.config;

import cn.bugstack.PayApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * pay 组件测试基类。
 */
@SpringBootTest(classes = PayApplication.class)
@ActiveProfiles("test-mock")
@Import(RocketMqMockTestConfig.class)
public abstract class AbstractPayComponentTest {
}
