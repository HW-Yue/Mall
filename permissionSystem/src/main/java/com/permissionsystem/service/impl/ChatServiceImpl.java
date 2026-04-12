package com.permissionsystem.service.impl;


import com.permissionsystem.dto.ConversationRequestDTO;
import com.permissionsystem.dto.Result;
import com.permissionsystem.service.ChatService;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    @Override
    public Result getConversations(ConversationRequestDTO request) {
            return Result.success();
    }
}