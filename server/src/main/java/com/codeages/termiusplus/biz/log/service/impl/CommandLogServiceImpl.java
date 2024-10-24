package com.codeages.termiusplus.biz.log.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.log.dto.*;
import com.codeages.termiusplus.biz.log.entity.CommandLog;
import com.codeages.termiusplus.biz.log.entity.QCommandLog;
import com.codeages.termiusplus.biz.log.mapper.CommandLogMapper;
import com.codeages.termiusplus.biz.log.repository.CommandLogRepository;
import com.codeages.termiusplus.biz.log.service.CommandLogService;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.validation.Validator;
import java.util.Date;
import java.util.Map;

@Service
public class CommandLogServiceImpl implements CommandLogService {

    private final CommandLogRepository commandLogRepository;

    private final CommandLogMapper commandLogMapper;

    private final ServerService serverService;

    private final Validator validator;

    @Value("${file.dir}")
    private String fileDir;

    private static final String LOG_FILE_PARENT = "CommandLog";

    private static final String LOG_FILE_NAME = "${sessionId}-${serverIp}-${serverPort}-${userId}-${createdAt}.log";

    public CommandLogServiceImpl(CommandLogRepository commandLogRepository,
                                 CommandLogMapper commandLogMapper, ServerService serverService,
                                 Validator validator) {
        this.commandLogRepository = commandLogRepository;
        this.commandLogMapper = commandLogMapper;
        this.serverService = serverService;
        this.validator = validator;
    }

    @Override
    public Page<CommandLogSimpleDto> search(CommandLogSearchParams searchParams, Pageable pageable) {
        QCommandLog q = QCommandLog.commandLog;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getSessionId())) {
            builder.and(q.sessionId.eq(searchParams.getSessionId()));
        }
        if (searchParams.getServerId() != null) {
            builder.and(q.serverId.eq(searchParams.getServerId()));
        }
        if (searchParams.getUserId() != null) {
            builder.and(q.userId.eq(searchParams.getUserId()));
        }
        return commandLogRepository.findAll(builder, pageable).map(commandLogMapper::toSimpleDto);
    }

    @Override
    public CommandLogDto create(CommandLogCreateParams createParams) {
        String date = DateUtil.format(new Date(), "yyyyMMdd");
        //CommandData存储日志文件地址
        String logFileName = fileDir + FileUtil.FILE_SEPARATOR + LOG_FILE_PARENT + FileUtil.FILE_SEPARATOR + date + FileUtil.FILE_SEPARATOR + LOG_FILE_NAME;
        ServerDto serverDto = serverService.findById(createParams.getServerId());
        String filePath = StringSubstitutor.replace(
                logFileName,
                Map.of(
                        "sessionId", createParams.getSessionId(),
                        "serverIp", serverDto.getIp(),
                        "serverPort", serverDto.getPort().toString(),
                        "userId", createParams.getUserId().toString(),
                        "createdAt", String.valueOf(System.currentTimeMillis())
                )
        );
        CommandLog createEntity = commandLogMapper.toCreateEntity(createParams);
        createEntity.setCommandData(filePath);

        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        return commandLogMapper.toDto(commandLogRepository.save(createEntity));
    }

    @Override
    public void update(CommandLogUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var commandLog = commandLogRepository.findById(updateParams.getId())
                                             .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        commandLogMapper.toUpdateEntity(commandLog, updateParams);
        commandLogRepository.save(commandLog);
    }

    @Override
    public void delete(Long id) {
        commandLogRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        commandLogRepository.deleteById(id);
    }

    @Override
    public CommandLogDto get(Long id) {
        CommandLogDto commandLogDto = commandLogRepository.findById(id)
                                                          .map(commandLogMapper::toDto)
                                                          .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (FileUtil.isFile(commandLogDto.getCommandData())) {
            commandLogDto.setCommandData(FileUtil.readUtf8String(commandLogDto.getCommandData()));
        }
        return commandLogDto;
    }
}


