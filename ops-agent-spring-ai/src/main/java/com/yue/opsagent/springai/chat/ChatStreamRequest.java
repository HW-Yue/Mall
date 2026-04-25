package com.yue.opsagent.springai.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatStreamRequest(
        @NotBlank String message
) {}
