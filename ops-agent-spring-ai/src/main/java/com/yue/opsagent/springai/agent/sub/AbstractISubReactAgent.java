package com.yue.opsagent.springai.agent.sub;

import com.yue.opsagent.springai.agent.react.ReactAgentSpec;
import com.yue.opsagent.springai.agent.react.ReactRunner;
import com.yue.opsagent.springai.agent.react.ReactTool;
import com.yue.opsagent.springai.infrastructure.config.OpsAiProperties;
import com.yue.opsagent.springai.infrastructure.observability.LlmCallTracer;
import com.yue.opsagent.springai.infrastructure.observability.OpsAiMetrics;
import com.yue.opsagent.springai.skill.api.OpsSkillRegistry;
import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;

public abstract class AbstractISubReactAgent implements ISubAgent {

    private static final Logger log = LoggerFactory.getLogger(AbstractISubReactAgent.class);

    private final MasterRegistry masterRegistry;
    private final OpsSkillRegistry domainRegistry;
    private final int maxSubIters;
    private final ReactRunner reactRunner;

    protected AbstractISubReactAgent(
            ChatModel chatModel,
            MasterRegistry masterRegistry,
            OpsSkillRegistry domainRegistry,
            OpsAiProperties props,
            LlmCallTracer llmCallTracer,
            OpsAiMetrics metrics) {
        this.masterRegistry = masterRegistry;
        this.domainRegistry = domainRegistry;
        this.maxSubIters = props.getReact().getMaxSubIters();
        this.reactRunner = new ReactRunner(chatModel, llmCallTracer, metrics);
    }

    @Override
    public String runReact(String task, Map<String, Object> context) {
        String taskPreview = task == null ? "" : task;
        if (taskPreview.length() > 160) {
            taskPreview = taskPreview.substring(0, 160) + "...";
        }
        log.info("[SubAgent] 进入 domain={} taskPreview={}", domainId(), taskPreview);
        return reactRunner.run(new ReactAgentSpec(
                "SubAgent:" + domainId(),
                "subagent:" + domainId(),
                buildSystemPrompt(),
                buildFirstUserMessage(task, context),
                context == null ? Map.of() : context,
                reactTools(),
                maxSubIters));
    }

    private String buildSystemPrompt() {
        String ht = domainRegistry.helpToolName();
        String tk = OpsSkillRegistry.HELP_ARG_TOOL;
        return """
                你是运维子Agent，仅处理一个技术域。你的目标是用最少工具调用回答调度者的明确问题。
                只允许输出**一段合法 JSON**（不要 markdown），格式二选一：
                1) {"action":"CALL_TOOL","tool":"<工具名>","args":{...键值...}}
                2) {"action":"FINAL","answer":"<给调度者的中文摘要>"}
                CALL_TOOL 时 args 必须与工具要求一致。查询某业务工具的完整参数、约束与注意事项时，先调用本 skill 的辅助工具 \
                """
                + ht
                + """
                ，args 必须包含非空字段 \
                """
                + tk
                + """
                （值为下方「业务工具」名称之一，勿填 \
                """
                + ht
                + """
                自身）。无参业务工具允许 args 为 {}。
                工具返回已经能回答任务时必须 FINAL，不要继续扩展排查范围。
                如果工具返回空列表、空 result、not found、unknown、404、连接失败或指标为空，要把“查不到”作为事实返回，不要换一堆无关工具试探。
                如果任务是确认某服务是否存在，只需要给出：存在/不存在/无法确认 + 证据。

                当前 skill：\
                """
                + domainRegistry.name()
                + " — "
                + domainRegistry.description()
                + """

                业务工具与概要（详细用法用 \
                """
                + ht
                + """
                 查询）：
                """
                + domainRegistry.helpToolMenuLine()
                + "\n"
                + domainRegistry.toolMenuBrief();
    }

    private String buildFirstUserMessage(String task, Map<String, Object> context) {
        return "任务：\n" + task + "\n\n上下文 JSON：\n" + context;
    }

    private List<ReactTool> reactTools() {
        return domainRegistry.toolNames().stream()
                .<ReactTool>map(toolName -> new ReactTool() {
                    @Override
                    public String name() {
                        return toolName;
                    }

                    @Override
                    public String description() {
                        return domainRegistry.toolSpecification(toolName);
                    }

                    @Override
                    public String execute(Map<String, Object> args, Map<String, Object> context) {
                        ToolResult tr = masterRegistry.execute(domainRegistry.name(), toolName, args == null ? Map.of() : args);
                        if (tr instanceof ToolResult.Pending) {
                            log.warn("[SubAgent] domain={} 审批挂起 tool={}", domainId(), toolName);
                            return "子Agent结束（审批挂起）: " + tr.toJson();
                        }
                        return tr.toJson();
                    }
                })
                .toList();
    }
}
