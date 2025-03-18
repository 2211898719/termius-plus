package com.codeages.termiusplus.biz.ai;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ai.dto.Ctool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIService {

    @Value("${deepseek.api-key}")
    private String httpKey;

    private final WebClient webClient;


    public AIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://spark-api-open.xf-yun.com").build();
    }


    public Flux<String> chat(String content, String message) {
        WebClient.RequestHeadersSpec<?> req = webClient.post().uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JSONUtil.toJsonStr(Map.of(
                        "model", "generalv3",
                        "messages", List.of(
                                Map.of("role", "system", "content", content),
                                Map.of(
                                        "role", "user",
                                        "content", message
                                )
                        ),
                        "stream", true
                )))  // 发送 JSON 请求体
                .header("Authorization", "Bearer " + httpKey);


        return req
                .retrieve()
                .bodyToFlux(String.class).map(s -> {
                    if ("[DONE]".equals(s)) {
                        return "DONE";
                    }

                    return JSONUtil.toBean(s, Ctool.class).getChoices().get(0).getDelta().getContent();
                });
    }

}
