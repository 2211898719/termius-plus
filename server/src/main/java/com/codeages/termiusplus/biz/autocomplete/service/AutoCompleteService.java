package com.codeages.termiusplus.biz.autocomplete.service;

import com.codeages.termiusplus.biz.patrol.service.AgentLlmClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCompleteService {

    private static final String SYSTEM_PROMPT = """
            实现一个 linux 命令居然要求如下：
            1. 只返回命令本身，不要任何解释
            2. 如果不确定，返回最有可能的命令
            3. 如果没有合适的命令，返回 '无法提供建议'
            4. 不要返回多于一个命令
            5. 不要返回任何多余的信息
            6. 只返回命令，不要任何解释
            7. 如果不确定，返回最有可能的命令
            8. 如果没有合适的命令，返回 '无法提供建议'
            9. 不要返回多于一个命令
            10. 不要返回任何多余的信息
            """;

    private final AgentLlmClientFactory factory;

    public String chat(String userMessage) {
        ChatClient chatClient = factory.createChatClient();

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();
    }
}
