package com.yue.opsagent.springai.trigger.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatStreamRequest(
        @NotBlank String message
) {}
