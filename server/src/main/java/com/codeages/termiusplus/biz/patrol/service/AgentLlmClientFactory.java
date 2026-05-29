package com.codeages.termiusplus.biz.patrol.service;

import org.springframework.ai.chat.client.ChatClient;

/**
 * 按配置创建 {@link ChatClient}，便于在 DeepSeek、MiniMax 等提供方之间切换。
 */
@FunctionalInterface
public interface AgentLlmClientFactory {

    ChatClient createChatClient();
}
