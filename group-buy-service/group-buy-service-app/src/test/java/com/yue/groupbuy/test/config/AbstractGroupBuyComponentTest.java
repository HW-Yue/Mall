package com.yue.groupbuy.test.config;

import com.yue.groupbuy.GroupBuyServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * group-buy-service 组件测试基类。
 */
@SpringBootTest(classes = GroupBuyServiceApplication.class)
@ActiveProfiles("test-mock")
@Import(RocketMqMockTestConfig.class)
public abstract class AbstractGroupBuyComponentTest {
}
