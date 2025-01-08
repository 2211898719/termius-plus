package com.codeages.termiusplus.biz.snippet.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandSearchParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import com.codeages.termiusplus.biz.snippet.entity.Command;
import com.codeages.termiusplus.biz.snippet.entity.QCommand;
import com.codeages.termiusplus.biz.snippet.mapper.CommandMapper;
import com.codeages.termiusplus.biz.snippet.repository.CommandRepository;
import com.codeages.termiusplus.biz.snippet.service.CommandService;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.ws.ssh.EventType;
import com.codeages.termiusplus.ws.ssh.MessageDto;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class CommandServiceImpl implements CommandService {

    private final CommandRepository commandRepository;

    private final CommandMapper commandMapper;

    private final Validator validator;
    private final ServerService serverService;

    public CommandServiceImpl(CommandRepository commandRepository, CommandMapper commandMapper, Validator validator,
                              ServerService serverService) {
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
        this.validator = validator;
        this.serverService = serverService;
    }

    @Override
    public Page<CommandDto> search(CommandSearchParams searchParams, Pageable pageable) {
        QCommand q = QCommand.command1;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getName())) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        if (searchParams.getName() != null) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        if (StrUtil.isNotEmpty(searchParams.getCommand())) {
            builder.and(q.command.eq(searchParams.getCommand()));
        }
        if (searchParams.getCommand() != null) {
            builder.and(q.command.eq(searchParams.getCommand()));
        }
        if (StrUtil.isNotEmpty(searchParams.getRemark())) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        if (searchParams.getRemark() != null) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        return commandRepository.findAll(builder, pageable).map(commandMapper::toDto);
    }

    @Override
    public void create(CommandCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        commandRepository.save(commandMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(CommandUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var command = commandRepository.findById(updateParams.getId())
                                       .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        commandMapper.toUpdateEntity(command, updateParams);
        commandRepository.save(command);
    }

    @Override
    public void delete(Long id) {
        commandRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        commandRepository.deleteById(id);
    }

    @Override
    @SneakyThrows
    public void execute(Long id) {
        Command command = commandRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (StrUtil.isEmpty(command.getServerIds())) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "not run command with serverIds is empty");
        }

        List<Long> serverIds = Arrays.stream(command.getServerIds().split(",")).map(Long::parseLong).toList();
        for (Long serverId : serverIds) {
            SSHClient sshClient = serverService.createSSHClient(serverId);
            Session session = sshClient.startSession();
            Session.Shell shell = session.startShell();

            InputStream inputStream = shell.getInputStream();
            CompletableFuture.runAsync(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int i;
                    //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                    while ((i = inputStream.read(buffer)) != -1) {
                        String originData = new String(Arrays.copyOfRange(buffer, 0, i), session.getRemoteCharset());
                        log.info("read command output: {}", originData);
                    }
                } catch (Exception e) {
                    log.error("read command output error", e);
                }
            });

            shell.getOutputStream().write(command.getCommand().replaceAll("\\{\\{版本}}","v1.0.7").getBytes());
            shell.getOutputStream().flush();
            shell.close();
            session.close();
            sshClient.close();
        }

        log.info("execute command success, commandId:{}, serverIds:{}", id, serverIds);
    }

//    @PostConstruct
//    public void execute() {
//        CompletableFuture.runAsync(()->{
//            execute(14L);
//        });
//    }
}


