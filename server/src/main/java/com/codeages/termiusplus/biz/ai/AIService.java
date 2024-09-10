package com.codeages.termiusplus.biz.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIService {

    @Value("${spark.httpKey}")
    private String httpKey;

    public String chat(String content, String message) {
        HttpRequest request = HttpRequest.post("https://spark-api-open.xf-yun.com/v1/chat/completions");
        request.header("Authorization", "Bearer "+httpKey );
        request.body(JSONUtil.toJsonStr(Map.of(
                "model", "generalv3",
                "messages", List.of(
                        Map.of("role", "system", "content", content),
                        Map.of(
                                "role", "user",
                                "content", message
                        )
                )
        )));
        log.info("chat request: {}", request);

        return String.valueOf(JSONUtil.parse(request.execute().body()).getByPath("choices[0].message.content"));
    }

}
