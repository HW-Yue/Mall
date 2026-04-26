package com.yue.opsagent.springai.agent.react;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReactActionParserTest {

    @Test
    void parsesCallToolAction() {
        var parsed = ReactActionParser.parse("""
                {"action":"CALL_TOOL","tool":"docker_skill","args":{"task":"查容器","limit":3,"safe":true}}
                """);

        assertThat(parsed).isPresent();
        assertThat(parsed.get()).isInstanceOf(ReactActionParser.ParsedAction.CallTool.class);
        var call = (ReactActionParser.ParsedAction.CallTool) parsed.get();
        assertThat(call.tool()).isEqualTo("docker_skill");
        assertThat(call.args())
                .containsEntry("task", "查容器")
                .containsEntry("limit", 3)
                .containsEntry("safe", true);
    }

    @Test
    void parsesFinalActionInsideMarkdownFence() {
        var parsed = ReactActionParser.parse("""
                ```json
                {"action":"FINAL","answer":"完成"}
                ```
                """);

        assertThat(parsed).isPresent();
        assertThat(parsed.get()).isEqualTo(new ReactActionParser.ParsedAction.FinalAction("完成"));
    }

    @Test
    void rejectsInvalidJsonOrMissingAction() {
        assertThat(ReactActionParser.parse("not-json")).isEmpty();
        assertThat(ReactActionParser.parse("{\"answer\":\"done\"}")).isEmpty();
    }
}
