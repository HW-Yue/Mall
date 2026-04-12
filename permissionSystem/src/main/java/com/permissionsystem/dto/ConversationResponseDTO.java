package com.permissionsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDTO {
    private String conversationId;
    private List<Reply> replies = new ArrayList<>(); // 初始化为空列表;
}