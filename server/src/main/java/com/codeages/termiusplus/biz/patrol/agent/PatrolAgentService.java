package com.codeages.termiusplus.biz.patrol.agent;

import com.codeages.termiusplus.biz.patrol.agent.tool.*;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(tools = {ExecuteCommandTool.class, NginxTool.class, DiskTool.class, ServiceTool.class, CleanupTool.class})
public interface PatrolAgentService {

    @SystemMessage("你是一个 Linux 服务器运维 AI 助手。你可以帮助用户检查和解决服务器问题。" +
            "你可以使用工具来执行命令、检查磁盘、分析 nginx 配置等。" +
            "\n重要规则：" +
            "\n1. 只能使用提供的工具，不要自行构造任意命令" +
            "\n2. 对于危险操作（删除文件、重启服务等），必须先告知用户并获得确认" +
            "\n3. 先收集信息再分析，给出清晰的问题诊断和解决方案" +
            "\n4. 如果用户要求执行危险操作，先用 cleanupFiles 等工具列出将要影响的文件，等用户确认后再调用 confirmCleanup 等工具" +
            "\n5. 输出使用 Markdown 格式，便于前端展示")
    String chat(@UserMessage String userMessage);
}
