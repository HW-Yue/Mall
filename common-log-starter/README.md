# common-log-starter

全链路日志 TraceId 自动配置 Starter，支持 Servlet / WebFlux / Feign / 异步线程池。

## 功能特性

- **网关层**：自动生成 trace-id，放入请求头和 Reactor Context
- **服务层**：从 HTTP Header 接收 trace-id，放入 MDC
- **Feign 调用**：自动透传 trace-id 到下游服务
- **异步线程**：ThreadPoolTaskExecutor 自动装饰，保证子线程继承 trace-id
- **WebFlux 兼容**：使用 Reactor Context 处理线程切换问题
- **Logstash / ELK**：可选；通过 **`logstash-appender.xml`** 启用，JSON 中带 **`service`**（`spring.application.name`），便于按服务名拼 ES 索引

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.yue</groupId>
    <artifactId>common-log-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置日志（可选）

**文件名请使用 `logback-spring.xml`**（与 `src/main/resources` 同级），以便 `<springProfile>`、`<springProperty>` 与 Spring 环境联动。

#### 2.1 仅控制台 + 本地文件（不上报 ELK）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="com/yue/common/log/logback/base.xml"/>

    <logger name="com.yue" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE_INFO"/>
        <appender-ref ref="ASYNC_FILE_ERROR"/>
    </root>
</configuration>
```

#### 2.2 同时上报 Logstash（ELK）

**为什么网关/有的服务起不来？**  
`LogstashTcpSocketAppender` 来自 **`logstash-logback-encoder`**。`base.xml` **不再**定义 `LOGSTASH`，避免没加该依赖时一启动就解析失败。只有在你 **显式** `include logstash-appender.xml` 且 **classpath 上有 encoder** 时才会加载 Logstash。

1. **在业务模块 `pom.xml` 中增加依赖**（`optional` 不会传递，必须自己声明）：

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

（Spring Boot **2.7.x**（如 **2.7.5**）自带 Logback **1.2.x**，请使用 **`logstash-logback-encoder` 7.x**（文档示例为 7.4）。Spring Boot 3 可酌情升到 encoder **8.x**，需自行验证。）

2. **`application.yml`（或 Nacos）**：每个微服务不同应用名 + Logstash 地址（可选，有默认值）：

```yaml
spring:
  application:
    name: mall-order-service   # 写入 Logstash JSON 字段 service

logstash:
  host: 192.168.1.100         # 默认 127.0.0.1
  port: 4560                  # 默认 4560
```

3. **`logback-spring.xml`**：在 `base.xml` **之后**再 include 一份，并在 `root` 上挂 `LOGSTASH`。**不要**在业务里再手写同名 `LOGSTASH` appender。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="com/yue/common/log/logback/base.xml"/>
    <include resource="com/yue/common/log/logback/logstash-appender.xml"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE_INFO"/>
        <appender-ref ref="ASYNC_FILE_ERROR"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

**仅控制台 + 文件、不上报 ELK**（如部分网关）：只 `include base.xml`，**不要** include `logstash-appender.xml`，**不要** `appender-ref LOGSTASH`，也**不必**加 `logstash-logback-encoder` 依赖。

**Logstash 侧按服务名建索引示例**（字段名与 JSON 顶层 `service` 一致）：

```ruby
output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "%{[service]}-%{+YYYY.MM.dd}"
  }
}
```

若部分事件缺少 `service`，需在 Logstash `filter` 中设置默认值或使用条件 `output`，避免索引名异常。

`spring.application.name` 中请勿包含英文双引号 `"` 或反斜杠 `\`，以免 JSON `customFields` 被破坏；建议使用字母、数字、中划线（如 `order-service`）。

#### 2.3 自定义日志格式（可选）

```xml
<property name="LOG_PATTERN" value="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} -%X{trace-id} - %msg%n"/>
<include resource="com/yue/common/log/logback/base.xml"/>
```

### 3. 完成

TraceId 相关能力：添加依赖后自动生效。日志样式与是否上报 ELK：按上文在 `logback-spring.xml` / `application.yml` 中配置。

## 配置属性

```yaml
yue:
  log:
    trace:
      enabled: true              # 总开关（默认 true）
      header-name: trace-id      # HTTP Header 名（默认 trace-id）
      mdc-key: trace-id          # MDC Key（默认 trace-id）
      response-header: false     # 响应头返回 trace-id（默认 false）
      task-decorator: true       # 线程池装饰器（默认 true）

# 以下为 Spring / 独立属性，供 logback logstash-appender.xml 使用
spring:
  application:
    name: your-service-name      # 写入 Logstash JSON 字段 service；未配置时为 unknown

logstash:
  host: 127.0.0.1                # Logstash TCP 主机
  port: 4560                     # Logstash TCP 端口
```

## 使用场景

### 场景 1：网关服务（WebFlux）

```yaml
# application.yml
yue:
  log:
    trace:
      response-header: true   # 让前端能看到 trace-id，方便排查
```

### 场景 2：业务服务（Servlet）

```java
@Slf4j
@Service
public class OrderService {
    public void createOrder() {
        // 日志自动带 trace-id
        log.info("创建订单...");
        // 输出: 2024-01-15 10:23:45.123 [http-nio-8080-exec-1] INFO c.y.s.OrderService -a1b2c3d4... - 创建订单...
    }
}
```

### 场景 3：Feign 调用

```java
@FeignClient(name = "order-service")
public interface OrderServiceClient {
    @PostMapping("/api/v1/order/create")
    Response createOrder(@RequestBody OrderRequest request);
}

// 调用时会自动在 Header 中带上 trace-id
orderServiceClient.createOrder(request);
```

### 场景 4：异步线程池

```java
@Configuration
public class AsyncConfig {
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 无需手动设置 TaskDecorator，starter 会自动添加 MdcTaskDecorator
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.initialize();
        return executor;
    }
}

// 使用
@Async("taskExecutor")
public void asyncMethod() {
    // 子线程中日志自动继承父线程的 trace-id
    log.info("异步任务执行...");
}
```

## 实现原理

```
用户请求 → 网关(创建trace-id) → [HTTP Header] → 服务A(接收→MDC) → [Feign Header] → 服务B(接收→MDC)
                              ↓                                       ↓
                        Reactor Context                          异步线程池
                              ↓                                       ↓
                        日志输出: -trace-id                      日志输出: -trace-id
```

## 注意事项

1. **必须清理 MDC**：Filter 的 finally 块中会自动清理，无需关注
2. **WebFlux 线程切换**：通过 Reactor Context 和 Hook 实现，无需关注
3. **线程池装饰**：只有 Spring 的 ThreadPoolTaskExecutor 会自动装饰，原生线程池需要手动包装 Runnable

## 版本兼容性

| Spring Boot 版本 | 支持状态 |
|-----------------|---------|
| 2.6.x / 2.7.x（含 2.7.5） | ✅ **当前 JAR 为此设计**：`javax.servlet` / Java 8+；ELK 用 `logstash-logback-encoder` **7.x** |
| 3.0.x+          | ⚠️ **同一 artifact 未做 Jakarta 迁移**：Servlet 栈使用 `javax.servlet.*`，与 Boot 3 的 `jakarta.servlet.*` **不兼容**，Tomcat 系服务请勿混用本 JAR。纯 WebFlux/Gateway 也未在 Boot 3 上完整回归。**logback 的 `base.xml` / `logstash-appender.xml` 与 Spring 版本无关**，可复制到 Boot 3 工程单独使用。 |

若需 **Boot 2 与 Boot 3 共用同一套 Trace 能力**，需要单独发布 **Jakarta 版** starter（把 `javax.servlet` / `javax.annotation` 改为 `jakarta.*` 并用 Boot 3 + Java 17 编译），或维护两条依赖坐标/分支。

