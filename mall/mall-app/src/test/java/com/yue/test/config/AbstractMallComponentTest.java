package com.yue.test.config;

import com.yue.MallApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * mall 组件测试基类。
 */
@SpringBootTest(classes = MallApplication.class)
@ActiveProfiles("test-mock")
@Import(RocketMqMockTestConfig.class)
public abstract class AbstractMallComponentTest {
}
