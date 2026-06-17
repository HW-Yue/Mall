# AI Agent 工程版讲稿（AI 应用 / Agent 工程方向）

**主线**：ops-agent-spring-ai 的完整工程化设计
**目标**：每个亮点都要能讲到 LLM / Tool Calling / ReAct / 上下文管理 / 安全控制的具体取舍

> 数据口径：项目无真实生产流量。"诊断时长 15min → 3min"等数字均为演示环境实测 + 对照人工基线的设计目标，不能讲成生产降本。

---

## 一、1 分钟项目总览（自我介绍用）

> 我做了一个叫 Nexus 的电商交易系统，整体是 6 个业务微服务 + 1 个 AI 运维 Agent。**业务侧是 Agent 的"操作面"**——提供完整的电商交易链路、Prometheus 指标、Nacos 配置、MQ Topic，让 Agent 有真东西可以观测和操作。
>
> 重点投入的是 `ops-agent-spring-ai`：基于 **Spring AI Alibaba + DashScope（通义千问）**，把 Alertmanager 告警接进来，按 SOP 规则匹配后用 **ReAct 编排 8 个领域 Skill**（Docker / MySQL / RocketMQ / Prometheus / ES / Redis / Nacos / Catalog）自动诊断。
>
> 整套设计的关键不是"调一次大模型"，而是：① SOP 硬匹配 + AI 兜底匹配的二段路由 ② Parent ReAct + 子 Agent 委派的两层 ReAct ③ Wrench 规则链统一工具执行 ④ 内存审批队列控制写操作 ⑤ Web 控制台可视化运行时间线和审批。

---

## 二、核心亮点（STAR 深挖）

### 亮点 1：SOP + AI 二段路由（不是纯 AI，也不是纯规则）

**S（背景）**
纯规则路由（关键字匹配）覆盖率低，告警格式稍变就漏；纯 AI 路由不可控，相同告警每次走的路径可能都不一样，故障复盘困难。

**T（任务）**
设计一套"**可解释 + 有兜底**"的路由方案。

**A（行动）**
1. **硬匹配优先**：YAML SOP 规则（`src/main/resources/sop/rules/`）按 `labels.alertname` + 文本特征精确匹配。命中即走该规则定义的 Skill 集合。
2. **AI 兜底匹配**：硬匹配失败时，把告警内容 + 所有 SOP 的 description 给到大模型，让它选最相关的 SOP（用 JSON 结构化输出强约束）。
3. **未命中策略**：结构化告警未命中 → 自动生成排查草案（基于 alertname 推荐相关 Skill）；纯文本预警未命中 → AI 直接做开放式诊断。
4. **路由可观察**：每次路由结果都打到 Prometheus（hit_hard / hit_ai / no_match 三个 counter），方便迭代 SOP 规则。

**R（结果 / 设计指标）**
- 演示场景下硬匹配 + AI 兜底总覆盖率 100%；
- 硬匹配占比 70%+（说明 SOP 规则收益高，AI 只在长尾兜底）；
- 单条告警路由耗时 < 1s（硬匹配毫秒级，AI 兜底 ~500ms）。

---

### 亮点 2：两层 ReAct + 子 Agent 委派（不是简单的 Function Calling）

**S（背景）**
如果只有一个 ReAct Agent 拥有全部工具，提示词会爆炸（8 个 Skill × 多个工具）、上下文窗口压力大、模型选错工具概率高。

**T（任务）**
让模型每次只"看到"它当前需要的工具集，分而治之。

**A（行动）**
1. **Parent ReAct Agent** 只看到 8 个 Skill 的高层 description（不含工具细节），只决定"委派给哪个子 Agent"。
2. **每个 Skill 对应一个 Sub Agent**（`AbstractISubReactAgent`），子 Agent 自己也是一个 ReAct 循环，但只看自己域内的工具列表和 prompt fragment。
3. **子 Agent 强 JSON 输出**：每轮必须返回单一合法 JSON action：
   ```
   {"action":"CALL_TOOL","tool":"<toolName>","args":{...}}
   {"action":"FINAL","answer":"<中文摘要>"}
   ```
   非法格式直接抛出，不容忍模型漂移。
4. **子 Agent 返回 Parent 时压缩成中文摘要**，原始工具结果只在子 Agent 上下文里存在，不污染 Parent 上下文。
5. **空结果即证据**：工具返回空、未找到、404、连接失败时，子 Agent 必须把这当作"已确认没有问题"，**禁止发散到无关工具**——这是 prompt 里硬约束。

**权衡**：
- **方案 A（已否决）**：单 Agent + 全部工具——上下文压力大，多工具调用准确率明显下降。
- **方案 B（已否决）**：Plan-and-Execute——运维场景必须看到上一步结果才知道下一步问什么，没法提前规划。
- **方案 C（采用）**：两层 ReAct + 子 Agent 委派——分而治之，每层上下文受控。

**R（结果 / 设计指标）**
- Parent 上下文稳定在 < 4k token；
- 子 Agent 上下文按域控制，平均 < 8k token；
- 工具选择准确率（演示场景）显著高于单 Agent 方案。

---

### 亮点 3：Wrench 规则链统一工具执行（横切关注点抽出来）

**S（背景）**
工具执行涉及很多横切关注点：白名单检查、审批检查、运行取消检查、链路追踪、结果归档。如果在每个 Skill 里重复写，会成为腐烂温床。

**T（任务）**
用规则链把这些横切关注点抽到中央，Skill 只负责领域逻辑。

**A（行动）**
工具调用统一路径：
```
HTTP /api/v1/tools/execute
  → ToolsController
  → MasterRegistry.execute(skill, tool, args)
  → Wrench Rule Chain（6 个 filter 依次执行）
      ├── SkillResolveRuleFilter（按 skill name 找到对应 registry）
      ├── ToolWhitelistRuleFilter（不在白名单的工具直接拒绝）
      ├── ToolApprovalRuleFilter（risky 写操作转审批队列，返回 PENDING_APPROVAL）
      ├── RunCancelRuleFilter（运行被取消则中止）
      ├── ToolTraceStartRuleFilter（埋 SkyWalking span）
      ├── ToolExecuteRuleFilter（真正调 Skill 的 toolkit）
      └── ToolResultRecordRuleFilter（结果归档到运行事件流）
  → 具体 OpsSkillRegistry.execute(tool, args)
  → Toolkit
  → ToolResult
```

**关键设计原则**：
- **只有横切关注点放 filter**，领域逻辑必须留在 Skill 里
- **Skill 不能绕过 MasterRegistry**——这是硬规则，保证审批和审计零绕过
- **新增一个 Skill 只需实现 `OpsSkillRegistry` 接口 + `@Component`**，自动被 MasterRegistry 注入并参与规则链

**R（结果）**
- 7 个 filter 集中管理，新增横切关注点（比如限流）只改一个地方；
- 8 个 Skill 域代码高度同构，新增一个域 ~200 行代码。

---

### 亮点 4：内存审批队列控制写操作（不是 demo，是认真的安全设计）

**S（背景）**
Agent 自动执行写操作（比如改 Nacos 配置）是绝对的高风险——模型幻觉一次就可能改坏生产。但完全禁止写操作，Agent 就退化成只能看不能做。

**T（任务）**
让所有写操作走"暂存 + 人工审批"的流程，保留 Agent 的执行力但杜绝失控。

**A（行动）**
1. **Skill 在接口层声明写操作**：
   ```java
   public boolean requiresApproval(String toolName) {
       return "nacos_publish_config".equals(toolName) || ...;
   }
   ```
2. **`ToolApprovalRuleFilter`** 在规则链里拦截，把整个调用上下文（skill、tool、args、调用者、时间）塞到内存审批队列，立即返回 `PENDING_APPROVAL`，**Agent 这一步动作直接结束**。
3. **运维同学在 Web 控制台**看到审批队列，逐条批准或拒绝。
4. **批准后由审批服务回放**这次调用——重新走规则链，但跳过 approval filter，落到真正的 toolkit 执行。
5. **审批结果回写到 run state**，Agent 后续可以查到结果继续推理。

**为什么不持久化审批队列？**
- 演示项目场景下持久化收益小，重启场景丢弃即可
- 持久化会让"审批通过后回放"的语义变复杂（要不要担心审批人离职、审批积压几天后环境变了等问题）
- 真上生产，要么改用 Outbox + 数据库表 + TTL，要么直接走工单系统（Jira / ServiceNow）

**R（结果 / 设计指标）**
- 写操作 100% 走审批，零绕过路径（绕过路径在代码里都不存在）；
- 演示中 Agent 可以"建议改 Nacos 配置"但永远不会直接改，体现"建议 → 审批 → 执行"的安全闭环。

---

### 亮点 5：业务系统作为 Agent 的"操作面"（差异化解释）

**S（背景）**
很多 AI Agent 项目演示时用的是"假数据 + 静态工具"，调用一次就结束。我想让 Agent 跑在真东西上。

**T（任务）**
让业务系统真的能产生告警、暴露指标、被 Agent 操作。

**A（行动）**
- 6 个业务微服务全部接入 Prometheus（订单成功率、支付延迟、MQ 积压）、ELK（日志检索）、SkyWalking（链路）。
- 业务跑压测时如果 RT 飙高/MQ 积压，Prometheus 计算告警 → Alertmanager → ops-agent。
- ops-agent 用 `metrics_ops` 查 Prometheus、`mysql_inspect` 查慢查询、`rocketmq_inspect` 查 Topic 积压、`docker_ops` 看容器资源，**真正闭环**。
- 写操作侧：`nacos_config` 可以提议调整 Sentinel 限流 / DynamicTp 线程池参数，审批通过后生效。

**R（结果）**
- "AI 帮你定位真实的微服务故障"在演示中可以完整跑通，不是 mock；
- 项目同时验证了"业务系统怎么对 Agent 友好"——可观测埋点完整 + 配置中心受控写 + 全链路追踪。

---

## 三、可能被追问的题（30 秒回答骨架）

### Q1: ReAct 和 Function Calling 有什么区别？你为什么用 ReAct？
A: Function Calling 是模型一次性决定调哪些函数（OpenAI/Anthropic API 原生支持）；ReAct 是"思考-行动-观察"循环，每次只调一个工具，看到结果再决定下一步。运维场景必须看上一步结果才知道下一步（先看告警再决定查哪个组件），ReAct 是天然契合的范式。Function Calling 也能配合 ReAct——每轮 ReAct 内部用 Function Calling 选工具，我这个项目就是这么做的。

### Q2: 为什么不用 LangChain / LangGraph？
A: 整个项目是 Java，引 Python 跨语言成本极高。Spring AI Alibaba 的 `ChatClient` + `ToolCallback` 已经能覆盖我需要的能力，Wrench 规则链满足了我对工具执行的控制需求。LangGraph 的图编排很优雅但运维 ReAct 场景图比较简单，收益不抵成本。

### Q3: 怎么防止 LLM 幻觉调用不存在的工具？
A: 三层防御：① Prompt 里硬约束"只能从给定列表选"；② `ToolWhitelistRuleFilter` 在执行前对照 `toolNames()` 白名单，不在的直接 reject；③ JSON 输出格式约束，非法 JSON 直接抛出让 Agent 重试。模型偶尔会编造工具名，被白名单挡掉后 ReAct 下一轮会自动纠正。

### Q4: 多轮对话上下文怎么管理？
A: 我用的不是无脑滚动窗口。Parent 上下文只保留每次子 Agent 的中文摘要（压缩到几百 token）；子 Agent 上下文保留自己域内的完整 ReAct 历史，超过阈值时按 thought + observation 对截断最早的几轮。`AbstractISubReactAgent` 里有显式的轮数上限（防止死循环）和 token 软上限。

### Q5: Token 成本怎么算？怎么控制？
A: 主要成本在 Parent 决策 + 子 Agent 多轮。控制手段：① Parent 只看 Skill description（短），不看具体工具列表；② 子 Agent 结果摘要化；③ 工具结果在规则链里归档但不回灌到 prompt；④ 一轮 ReAct 上限轮数硬截断；⑤ DashScope 选 turbo 系列做规划，qwen-max 只在 AI 兜底匹配这种关键决策点用。

### Q6: AI 兜底匹配怎么保证不乱选 SOP？
A: 用结构化输出强约束——给模型的 prompt 要求只返回 `{"sop_id": "...", "confidence": 0.0-1.0, "reason": "..."}` 的 JSON。confidence 低于阈值（比如 0.6）直接走未命中策略，不强行匹配。

### Q7: 工具执行失败怎么处理？
A: 子 Agent prompt 里明确写："工具返回空、404、连接失败时，把它当作已确认没有问题的证据，不要发散到无关工具"。这避免了模型在"我没查到东西"和"还有更多工具可以试"之间无限循环。失败本身也是 observation，Agent 会把它纳入下一轮思考。

### Q8: 这套 Agent 怎么评估效果？
A: 三个维度：① **正确率**——人工构造典型告警，看 Agent 是否给出和人工排查一致的结论；② **效率**——诊断耗时 vs 人工对照基线（演示中 ~15min → ~3min）；③ **安全性**——写操作必须 100% 走审批，零绕过路径（这个是代码级保证，不靠测试）。线上的 A/B 没有，因为是个人项目。

### Q9: 如果换一个 LLM 提供商（比如换成 GPT-4）容易吗？
A: Spring AI Alibaba 的 `ChatClient` 已经做了抽象，换模型主要改配置和 prompt 调优（不同模型对 JSON 输出严格度不一样）。我的所有 prompt 都集中在子 Agent 基类和 Parent Agent 里，迁移面有限。但生产级迁移要重新跑回归用例，因为模型行为细节差异不小。

### Q10: Agent 会不会被 Prompt Injection 攻击？
A: 当前演示项目没做完整防护，但设计上已经有几层缓冲：① 告警内容来自 Alertmanager（可信内部源），不是开放用户输入；② 工具白名单 + 审批队列让"被注入也跑不出危险动作"；③ 工具参数 schema 校验。生产场景下还需要加：输入清洗、prompt 与数据分离、敏感工具二次确认。

---

## 四、自评 / 复盘金句

- "做 Agent 工程最大的体会是：**Prompt 的对就是工程的对**。任何'让 AI 别做傻事'的指令，最好让代码层面也做不到——比如工具白名单，靠 prompt 说不要乱调，不如规则链直接拒绝。"
- "我刻意没用任何 Python 库，全部用 Spring 生态实现——目的是验证 'Java 做 Agent 是否可行'。结论是**可行但有代价**：prompt 调试、上下文管理工具链确实比 Python 弱，需要自己造轮子。"
- "如果重做一次，我会更早把**评估集**搭起来——用 20 条典型告警 + 期望结论组成回归用例，每次改 prompt 都跑一遍。现在迭代主要靠人肉看 case，效率不高。"
- "Agent 不是 chatbot 套壳。chatbot 调一次模型完事，Agent 必须把'多轮决策 + 工具执行 + 状态管理 + 安全控制'当成一个完整工程问题来设计——这是我这个项目最想表达的。"
