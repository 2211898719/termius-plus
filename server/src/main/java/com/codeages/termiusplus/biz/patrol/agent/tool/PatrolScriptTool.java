package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.patrol.agent.ToolCallHelper;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptCreateParams;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptDto;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatrolScriptTool {

    private final PatrolScriptService scriptService;
    private Sinks.Many<String> sink;

    public void setSink(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    @Tool(description = "创建一个可复用的巡检脚本并保存到巡检脚本库。"
            + "脚本内容必须是可独立执行的 bash 脚本，并输出 JSON 格式结果到 stdout，"
            + "JSON 至少包含 status(ok/warning/error)、message(简要描述)、details(详细数据) 三个字段。"
            + "当用户希望把一个检查逻辑沉淀为可重复执行的巡检任务时调用此工具。"
            + "如果脚本只适用于部分服务器或某些分组，传入 serverIds / groupIds；都不传则适用于所有服务器。")
    public String createPatrolScript(
            @ToolParam(description = "脚本名称，简短概括用途") String name,
            @ToolParam(description = "脚本详细描述，说明它检查什么、何时报警") String description,
            @ToolParam(description = "完整的 bash 脚本内容（包含 shebang），要求输出 JSON 格式结果到 stdout") String scriptContent,
            @ToolParam(description = "输出 JSON 的 schema 描述，可选") String outputSchema,
            @ToolParam(description = "分类：disk / nginx / security / service / custom") String category,
            @ToolParam(description = "限定执行的服务器ID列表，JSON 数组字符串如 [1,2,3]，可空表示不限") String serverIds,
            @ToolParam(description = "限定执行的服务器分组ID列表，JSON 数组字符串如 [4,5]，包含子分组下所有服务器，可空表示不限") String groupIds) {
        return ToolCallHelper.execute(sink, "createPatrolScript",
                "name=" + name + ", category=" + category,
                () -> {
                    try {
                        PatrolScriptCreateParams params = new PatrolScriptCreateParams();
                        params.setName(name);
                        params.setDescription(description);
                        params.setScriptContent(scriptContent);
                        params.setOutputSchema(outputSchema);
                        if (category != null && !category.isBlank()) {
                            params.setCategory(category);
                        }
                        if (serverIds != null && !serverIds.isBlank()) params.setServerIds(serverIds);
                        if (groupIds != null && !groupIds.isBlank()) params.setGroupIds(groupIds);
                        PatrolScriptDto created = scriptService.create(params);
                        StringBuilder sb = new StringBuilder();
                        sb.append("已创建巡检脚本，ID=").append(created.getId())
                          .append("，名称=").append(created.getName()).append("\n\n");
                        sb.append("请把下面的脚本内容**完整地告诉用户**(用 Markdown ```bash 代码块包裹),不要只说「已创建」:\n\n");
                        sb.append("**脚本名称**: ").append(created.getName()).append("\n");
                        sb.append("**分类**: ").append(created.getCategory() == null ? "custom" : created.getCategory()).append("\n");
                        if (created.getDescription() != null && !created.getDescription().isBlank()) {
                            sb.append("**说明**: ").append(created.getDescription()).append("\n");
                        }
                        sb.append("**脚本内容**:\n```bash\n").append(created.getScriptContent()).append("\n```\n");
                        if (created.getOutputSchema() != null && !created.getOutputSchema().isBlank()) {
                            sb.append("**输出 schema**: ").append(created.getOutputSchema()).append("\n");
                        }
                        return sb.toString();
                    } catch (Exception e) {
                        log.error("创建巡检脚本失败", e);
                        return "创建巡检脚本失败: " + e.getMessage();
                    }
                });
    }
}
