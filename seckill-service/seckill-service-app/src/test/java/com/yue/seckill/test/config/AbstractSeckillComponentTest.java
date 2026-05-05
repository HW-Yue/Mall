package com.yue.seckill.test.config;

import com.yue.seckill.SeckillServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * seckill-service 组件测试基类。
 */
@SpringBootTest(classes = SeckillServiceApplication.class)
@ActiveProfiles("test-mock")
@Import(RocketMqMockTestConfig.class)
public abstract class AbstractSeckillComponentTest {
}
