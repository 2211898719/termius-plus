package com.codeages.termiusplus.biz.patrol.service.impl;

import com.codeages.termiusplus.biz.patrol.service.AgentLlmClientFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

//@Service - switched to MiniMax
public class OpenAILlmClientFactory implements AgentLlmClientFactory {

    @Value("${spring.ai.deepseek.api-key:${deepseek.api-key:test_deepseek_api_key}}")
    private String apiKey;

    @Override
    public ChatClient createChatClient() {
        var apiBuilder = OpenAiApi.builder()
                                  .apiKey(apiKey)
                                  .baseUrl("https://api.deepseek.com");
        OpenAiApi openAiApi = apiBuilder.build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                                                     .model("deepseek-v4-flash")
                                                     .extraBody(Map.of("thinking", Map.of("type", "disabled")))
                                                     .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                                                   .openAiApi(openAiApi)
                                                   .defaultOptions(options)
                                                   .build();

        return ChatClient.builder(chatModel).build();
    }
}
