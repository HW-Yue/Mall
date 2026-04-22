# Issue #001: agentscope-1.0.11 Jackson 2.x / 3.x 依赖冲突

## 状态
**open** — 待上游框架修复后移除 workaround

## 问题描述

运行 `ops-agent` 时，AgentScope 执行 Tool 调用（`ToolValidator.validateInput`）会抛出 `NoClassDefFoundError`：

```
java.lang.NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs
    at tools.jackson.databind.introspect.JacksonAnnotationIntrospector.<clinit>
    at tools.jackson.databind.cfg.MapperBuilder.<clinit>
    ...
    at com.networknt.schema.SchemaRegistry.getSchema
    at io.agentscope.core.tool.ToolValidator.validateInput
```

## 根本原因

`agentscope-1.0.11` 的 BOM 自身存在依赖冲突：

| 依赖 | agentscope 声明的版本 | 内部使用的 Jackson 版本 |
|------|----------------------|-----------------------|
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.1 | **Jackson 2.x** |
| `com.networknt:json-schema-validator` | 3.0.1 | **Jackson 3.x** (`tools.jackson`) |

`json-schema-validator-3.0.1` 依赖 `tools.jackson.databind:3.1.0`（Jackson 3.x），而 Jackson 3.x 的 `JacksonAnnotationIntrospector` 试图引用注解 `@JsonSerializeAs`，这个注解在 Jackson 2.x 中**不存在**。因此只要 classpath 上同时出现 Jackson 2.x 和 Jackson 3.x，就会触发 `ClassNotFoundException`。

**这不是 Spring Boot 版本问题** — 升级到任何版本的 Spring Boot（包括当前未发布的 3.5+/4.x）都无法解决，因为 `tools.jackson` 是独立于 Spring Boot BOM 的第三方库，Spring Boot 管不到它。

## 上游状态

- `agentscope-1.0.11`（已发布）：有这个问题
- `agentscope-1.0.12-SNAPSHOT`（源码仓库）：仍然锁 `json-schema-validator:3.0.1`，**未修复**

## 当前 Workaround

在 `ops-agent/pom.xml` 中：

1. **全局覆盖** `json-schema-validator` 版本为 `2.0.0`（使用 Jackson 2.x）
2. **排除**所有 agentscope 依赖传递进来的 `json-schema-validator 3.0.1` 和 `tools.jackson.*`

```xml
<properties>
    <!-- json-schema-validator 3.x 使用 Jackson 3.x（tools.jackson），
         与 Spring Boot 3.2 的 Jackson 2.x 冲突；锁定 2.x 版本 -->
    <json-schema-validator.version>2.0.0</json-schema-validator.version>
</properties>

<!-- 对 agentscope 三个依赖均做排除 -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope</artifactId>
    <exclusions>
        <exclusion><groupId>com.networknt</groupId><artifactId>json-schema-validator</artifactId></exclusion>
        <exclusion><groupId>tools.jackson</groupId><artifactId>*</artifactId></exclusion>
    </exclusions>
</dependency>
<!-- agentscope-extensions-agui、agentscope-agui-spring-boot-starter 同样排除 -->

<!-- 显式引入兼容版本 -->
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>${json-schema-validator.version}</version>
</dependency>
```

## 潜在风险

`json-schema-validator` 2.0.0 相比 3.0.1 可能缺少某些 JSON Schema draft 特性。如果 agentscope 的 `@Tool` 参数校验用到了这些特性，可能导致校验行为差异。目前本地测试 Agent 能正常执行 tool 调用，未发现异常。

## 何时可以移除 Workaround

当 `agentscope` 官方发布的新版本满足以下任一条件时：

- 将 `json-schema-validator` 降级到 Jackson 2.x 兼容版本（如 2.0.x）
- 或者将 Jackson 2.x 依赖全部迁移到 Jackson 3.x 并保持内部一致

## 相关文件

- `ops-agent/pom.xml` — workaround 实现位置
- `ops-agent/src/main/java/com/yue/opsagent/adapter/tool/` — Tool 定义，运行时触发此问题
