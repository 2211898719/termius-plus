package com.codeages.termiusplus.biz.patrol.service.impl;

import com.codeages.termiusplus.biz.patrol.service.AgentLlmClientFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MiniMaxLlmClientFactory implements AgentLlmClientFactory {

    @Value("${minimax.api-key}")
    private String apiKey;

    @Value("${minimax.base-url:https://api.minimax.chat/v1}")
    private String baseUrl;

    @Value("${minimax.model:MiniMax-Text-01}")
    private String model;

    @Override
    public ChatClient createChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                                       .apiKey(apiKey)
                                       .baseUrl(baseUrl)
                                       .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                     .model(model)
                                                     .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                                                   .openAiApi(openAiApi)
                                                   .defaultOptions(options)
                                                   .build();

        return ChatClient.builder(chatModel).build();
    }
}