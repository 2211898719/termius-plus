package com.codeages.termiusplus.api.admin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ai.AIService;
import com.codeages.termiusplus.biz.server.context.ServerContext;
import com.codeages.termiusplus.biz.server.dto.AIChatParams;
import com.codeages.termiusplus.biz.server.dto.AiCompletionMetadata;
import com.codeages.termiusplus.ws.ssh.SshHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
//import reactor.core.publisher.Flux;

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
    public Map<String, Object> complete(@RequestBody AiCompletionMetadata data) {
        HttpRequest request = HttpRequest.post(fittenUrl)
                .body(JSONUtil.toJsonStr(Map.of(
                        "inputs", "!FCPREFIX!" + data.getCompletionMetadata().getTextBeforeCursor() + "!FCSUFFIX!" + data.getCompletionMetadata().getTextAfterCursor() + "!FCMIDDLE!",
                        "meta_datas", Map.of(
                                "filename", data.getCompletionMetadata().getFilename()
                        )
                )));

        HttpResponse response = request.execute();
        log.info("response: {}", response.body());
        Map<String, String> map = JSONUtil.toBean(response.body(), Map.class);

        return Map.of("completion", map.get("generated_text").replace("<|endoftext|>", ""));
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(AIChatParams params) {
        SseEmitter sseEmitter = new SseEmitter();

        String content = params.getPrompt();
        SshHandler.HandlerItem handlerItem = ServerContext.SSH_POOL.get(params.getSessionId());
        String message = params.getMessage();
        String lastCommandLog = "";
        message = message.replace("{{commandLog}}", Objects.requireNonNullElse(lastCommandLog, ""));
        content = content.replace("{{commandLog}}", Objects.requireNonNullElse(lastCommandLog, ""));

        Flux<String> chat = aiService.chat(content, message);
        chat.map(s->Map.of("message",s)).subscribe(data -> {
            try {
                sseEmitter.send(data);
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        }, error -> {
            try {
                sseEmitter.completeWithError(error);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, sseEmitter::complete);

        return sseEmitter;
    }


}


