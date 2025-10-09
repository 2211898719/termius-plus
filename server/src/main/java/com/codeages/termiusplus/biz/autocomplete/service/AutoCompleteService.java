package com.codeages.termiusplus.biz.autocomplete.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface AutoCompleteService {

    @SystemMessage("实现一个 linux 命令居然要求如下：\n" +
            "1. 只返回命令本身，不要任何解释\n" +
            "2. 如果不确定，返回最有可能的命令\n" +
            "3. 如果没有合适的命令，返回 '无法提供建议'\n" +
            "4. 不要返回多于一个命令\n" +
            "5. 不要返回任何多余的信息\n" +
            "6. 只返回命令，不要任何解释\n" +
            "7. 如果不确定，返回最有可能的命令\n" +
            "8. 如果没有合适的命令，返回 '无法提供建议'\n" +
            "9. 不要返回多于一个命令\n" +
            "10. 不要返回任何多余的信息")
    String chat(String userMessage);
}
