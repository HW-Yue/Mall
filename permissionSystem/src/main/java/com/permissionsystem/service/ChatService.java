package com.permissionsystem.service;

import com.permissionsystem.dto.ConversationRequestDTO;
import com.permissionsystem.dto.Result;

public interface ChatService {
    Result getConversations(ConversationRequestDTO request);
}
