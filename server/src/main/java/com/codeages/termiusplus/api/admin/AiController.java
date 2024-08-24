package com.codeages.termiusplus.api.admin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.server.dto.AiCompletionMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api-admin/ai")
public class AiController {

    @Value("${fitten.url}")
    private String fittenUrl;

    @PostMapping("/complete")
    public Map<String, Object> complete(@RequestBody AiCompletionMetadata data) {
        HttpRequest request = HttpRequest.post(fittenUrl)
                                         .body(JSONUtil.toJsonStr(Map.of(
                                                 "inputs", "!FCPREFIX!"+data.getCompletionMetadata().getTextBeforeCursor()+"!FCSUFFIX!"+data.getCompletionMetadata().getTextAfterCursor()+"!FCMIDDLE!"
                                         )));

        HttpResponse response = request.execute();
        log.info("response: {}", response.body());
        Map<String,String> map = JSONUtil.toBean(response.body(), Map.class);

        return Map.of("completion", map.get("generated_text").replace("<|endoftext|>", ""));
    }
}
