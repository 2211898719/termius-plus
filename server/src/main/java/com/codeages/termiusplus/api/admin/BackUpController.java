package com.codeages.termiusplus.api.admin;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ZipUtil;
import com.codeages.termiusplus.biz.storage.enums.FileTargetTypeEnum;
import com.codeages.termiusplus.biz.storage.service.StorageService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api-admin/back")
public class BackUpController {

    private final DataSource dataSource;

    private final StorageService storageService;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${DB_HOST}")
    private String host;

    @Value("${DB_PORT}")
    private String port;

    private static final String DUMP_SQL_TEMPLATE = "mysqldump -h ${host} -u${username} -P${port} -p${password} --databases ${databaseName} > ${uri}";

    private String DUMP_SQL;

    private String DATABASE_NAME;

    @SneakyThrows
    @PostConstruct
    private void init() {
        this.DATABASE_NAME = dataSource.getConnection().getCatalog();

        this.DUMP_SQL = new StringSubstitutor(
                Map.of(
                        "username", username,
                        "password", password,
                        "host", host,
                        "port", port,
                        "databaseName", this.DATABASE_NAME
                )
        ).replace(DUMP_SQL_TEMPLATE);
    }

    public BackUpController(DataSource dataSource, StorageService storageService) {
        this.dataSource = dataSource;
        this.storageService = storageService;
    }

    @GetMapping("/back")
    public void back(HttpServletResponse response) {
        String backUri = FileUtil.getTmpDirPath() + FileUtil.FILE_SEPARATOR + this.DATABASE_NAME + "-" + DateUtil.format(
                DateUtil.date(),
                DatePattern.CHINESE_DATE_TIME_PATTERN
        ) + FileUtil.FILE_SEPARATOR;
        FileUtil.mkdir(backUri);

        String sqlDir = backUri + "sql.sql";

        backSql(sqlDir);


        com.codeages.termiusplus.biz.storage.utils.FileUtil.writeFileToResponse(
                storageService.store(
                        FileUtil.file(ZipUtil.zip(backUri)),
                        FileTargetTypeEnum.BASE
                ),
                response
        );

        FileUtil.del(backUri);
    }

    @SneakyThrows
    private void backSql(String sqlUri) {
        Map<String, String> param = Map.of( "uri", sqlUri);

        String backCommand = new StringSubstitutor(param).replace(DUMP_SQL);
        log.info("执行命令：{}", backCommand);

        exec(backCommand);
    }

    @SneakyThrows
    private void exec(String cmd) {
        List<String> cmds = new ArrayList<>();
        cmds.add("sh");
        cmds.add("-c");
        cmds.add(cmd);
        ProcessBuilder pb = new ProcessBuilder(cmds);
        Process p = pb.start();
        p.waitFor();
        //获取命令执行的结果
        String result = IoUtil.readUtf8(p.getInputStream());
        log.info("执行结果：{}", result);
    }



}
