package com.codeages.termiusplus.biz.message;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendHeaders;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendRequest;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.codeages.termiusplus.biz.server.job.ServerRunInfoJob;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import com.taobao.api.ApiException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@ConditionalOnExpression("${spring.dingtalk.enabled:false}")
public class MessageServiceDingtalkCorpRobotImpl implements MessageService {

    public static final ThreadLocal<String> DINGTALK_TOKEN_THREAD_LOCAL = new ThreadLocal<>();

    @Autowired
    private RedisTemplate<String, String> redis;


    @Value("${spring.dingtalk.enabled}")
    private Boolean dingTalkEnabled;
    @Value("${spring.dingtalk.agent-id}")
    private String dingTalkAgentId;
    @Value("${spring.dingtalk.client-id}")
    private String dinTalkClientId;
    @Value("${spring.dingtalk.client-secret}")
    private String dinTalkClientSecret;
    @Value("${spring.dingtalk.robot-code}")
    private String dinTalkRobotCode;
    @Value("${spring.dingtalk.cool-app-code}")
    private String dinTalkCoolAppCode;

    @SneakyThrows
    @Override
    public void send(MessageSubType messageType, String title, String message) {
        String sessionWebhook = DINGTALK_TOKEN_THREAD_LOCAL.get();
        String conversationId = redis.opsForValue()
                                     .get("dingtalk-robot-message");
        if (CharSequenceUtil.isNotBlank(sessionWebhook)) {
            sendMessageWebhook(sessionWebhook, List.of(), title, message);
        } else if (CharSequenceUtil.isNotBlank(conversationId)) {
            sendMessageByConversationId(conversationId, List.of(), title, message);
        } else {
            log.error("sessionWebhook and conversationId is null");
        }
    }


    @PostConstruct
    public void main() throws Exception {
        OpenDingTalkStreamClientBuilder
                .custom()
                .credential(new AuthClientCredential(
                        dinTalkClientId,
                        dinTalkClientSecret
                ))
                //注册机器人监听器
                .registerCallbackListener("/v1.0/im/bot/messages/get", robotMessage -> {
                    log.info("receive robotMessage, {}", robotMessage);
                    JSONObject json = new JSONObject(robotMessage);
                    //开发者根据自身业务需求，处理机器人回调
                    String content = json.getByPath("text.content", String.class)
                                         .trim();

                    DINGTALK_TOKEN_THREAD_LOCAL.set(json.getStr("sessionWebhook"));
                    if (content.equals("订阅监控消息")) {
                        redis.opsForValue()
                             .set("dingtalk-robot-message", json.getStr("conversationId"));
                        sendMessageByConversationId(
                                json.getStr("conversationId"),
                                List.of(),
                                "回复",
                                "订阅成功"
                                                   );
                    } else if (content.equals("取消订阅监控消息")) {
                        redis.delete("dingtalk-robot-message");
                        send(MessageSubType.MARKDOWN, "取消订阅监控消息", "已取消订阅监控消息");
                    } else if (content.equals("获取服务器硬盘情况")) {
                        send(MessageSubType.MARKDOWN, "服务器硬盘情况", "正在获取服务器硬盘情况，请稍后...");

                        SpringUtil.getBean(ServerRunInfoJob.class)
                                  .estimateDiskUsage();
                    }

                    DINGTALK_TOKEN_THREAD_LOCAL.remove();

                    return new JSONObject();
                })
                .build()
                .start();
    }

    public void sendMessageByConversationId(String conversationId, List<String> userIds,
                                            String title, String content) {
        OrgGroupSendHeaders orgGroupSendHeaders = new OrgGroupSendHeaders();
        orgGroupSendHeaders.setXAcsDingtalkAccessToken(getToken());

        OrgGroupSendRequest orgGroupSendRequest = new OrgGroupSendRequest();
        orgGroupSendRequest.setMsgKey("sampleMarkdown");
        orgGroupSendRequest.setRobotCode(dinTalkRobotCode);

        orgGroupSendRequest.setOpenConversationId(conversationId);

        JSONObject msgParam = new JSONObject();
        msgParam.put("text", content);
        msgParam.put("title", title);
        orgGroupSendRequest.setMsgParam(msgParam.toJSONString(0));
        try {
            Config config = new Config();
            config.protocol = "https";
            config.regionId = "central";
            com.aliyun.dingtalkrobot_1_0.Client client = new com.aliyun.dingtalkrobot_1_0.Client(config);
            OrgGroupSendResponse orgGroupSendResponse = client.orgGroupSendWithOptions(
                    orgGroupSendRequest,
                    orgGroupSendHeaders,
                    new com.aliyun.teautil.models.RuntimeOptions()
                                                                                      );
            if (Objects.isNull(orgGroupSendResponse) || Objects.isNull(orgGroupSendResponse.getBody())) {
                log.error(
                        "RobotGroupMessagesService_send orgGroupSendWithOptions return error, response={}",
                        orgGroupSendResponse
                         );
            }
        } catch (TeaException e) {
            log.error("RobotGroupMessagesService_send orgGroupSendWithOptions throw TeaException, errCode={}, " +
                              "errorMessage={}", e.getCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("RobotGroupMessagesService_send orgGroupSendWithOptions throw Exception", e);
            try {
                throw e;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public String getToken() {
        GetAccessTokenRequest getAccessTokenRequest = new GetAccessTokenRequest();
        getAccessTokenRequest.setAppKey(dinTalkClientId);
        getAccessTokenRequest.setAppSecret(dinTalkClientSecret);
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        try {
            com.aliyun.dingtalkoauth2_1_0.Client client = new com.aliyun.dingtalkoauth2_1_0.Client(config);
            GetAccessTokenResponse accessToken = client.getAccessToken(getAccessTokenRequest);
            return accessToken.getBody()
                              .getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessageWebhook(String webhook, List<String> userIds,
                                          String title, String content) throws ApiException {
        DingTalkClient client = new DefaultDingTalkClient(webhook);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("markdown");
        OapiRobotSendRequest.Markdown text = new OapiRobotSendRequest.Markdown();
        text.setText(content);
        text.setTitle(title);
        request.setMarkdown(text);
        OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
        at.setAtUserIds(userIds);
        at.setIsAtAll(false);
        request.setAt(at);
        OapiRobotSendResponse response = client.execute(request);
        System.out.println(response.getBody());
    }
}
