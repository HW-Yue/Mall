package com.yue.opsagent.springai.trigger.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ToolExecuteRequest(
        @NotBlank String skill,
        @NotBlank String tool,
        Map<String, Object> args
) {}
