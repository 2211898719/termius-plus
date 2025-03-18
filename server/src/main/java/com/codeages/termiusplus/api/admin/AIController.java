package com.codeages.termiusplus.api.admin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ai.AIService;
import com.codeages.termiusplus.biz.server.context.ServerContext;
import com.codeages.termiusplus.biz.server.dto.AIChatParams;
import com.codeages.termiusplus.biz.server.dto.AiCompletionMetadata;
import com.codeages.termiusplus.ws.ssh.SshHandler;
import com.cxytiandi.encrypt.springboot.annotation.DecryptIgnore;
import com.cxytiandi.encrypt.springboot.annotation.EncryptIgnore;
import io.github.pigmesh.ai.deepseek.core.DeepSeekClient;
import io.github.pigmesh.ai.deepseek.core.OpenAiClient;
import io.github.pigmesh.ai.deepseek.core.SyncOrAsyncOrStreaming;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionModel;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionRequest;
import io.github.pigmesh.ai.deepseek.core.chat.ChatCompletionResponse;
import io.github.pigmesh.ai.deepseek.core.chat.ResponseFormatType;
import io.github.pigmesh.ai.deepseek.core.search.FreshnessEnums;
import io.github.pigmesh.ai.deepseek.core.search.SearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api-admin/ai")
public class AIController {

    @Value("${fitten.url}")
    private String fittenUrl;

    @Autowired
    private AIService aiService;

    @PostMapping("/complete")
    @EncryptIgnore
    @DecryptIgnore
    public Map<String, Object> complete(@RequestBody AiCompletionMetadata data) {
        HttpRequest request = HttpRequest.post(fittenUrl)
                                         .body(JSONUtil.toJsonStr(Map.of(
                                                 "inputs",
                                                 "!FCPREFIX!" + data.getCompletionMetadata()
                                                                    .getTextBeforeCursor() + "!FCSUFFIX!" + data.getCompletionMetadata()
                                                                                                                .getTextAfterCursor() + "!FCMIDDLE!",
                                                 "meta_datas",
                                                 Map.of(
                                                         "filename",
                                                         data.getCompletionMetadata()
                                                             .getFilename()
                                                       )
                                                                        )));

        HttpResponse response = request.execute();
        log.info("response: {}", response.body());
        Map<String, String> map = JSONUtil.toBean(response.body(), Map.class);

        return Map.of(
                "completion",
                map.get("generated_text")
                   .replace("<|endoftext|>", "")
                     );
    }


    @Autowired
    private DeepSeekClient deepSeekClient;

    // sse 流式返回
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @EncryptIgnore
    @DecryptIgnore
    public Flux chat(AIChatParams params) {
        String content = params.getPrompt();
        String message = params.getMessage();
        SshHandler.HandlerItem handlerItem = ServerContext.SSH_POOL.get(params.getSessionId());
        String lastCommandLog = handlerItem.getLastCommand();
        message = message.replace("{{commandLog}}", Objects.requireNonNullElse(lastCommandLog, ""));
        content = content.replace("{{commandLog}}", Objects.requireNonNullElse(lastCommandLog, ""));

        ChatCompletionRequest request = ChatCompletionRequest.builder()

                                                             // 模型选择，支持 DEEPSEEK_CHAT、DEEPSEEK_REASONER 等
                                                             .model(ChatCompletionModel.DEEPSEEK_REASONER)

                .addSystemMessage(content)
                                                             // 添加用户消息
                                                             .addUserMessage(message)

                                                             .build();

        return deepSeekClient.chatFluxCompletion(request).map(response -> response);
    }


}


