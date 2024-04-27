package com.codeages.javaskeletonserver.biz.application.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.application.config.ApplicationMonitorRequestConfig;
import com.codeages.javaskeletonserver.biz.application.dto.*;
import com.codeages.javaskeletonserver.biz.application.entity.QApplicationMonitor;
import com.codeages.javaskeletonserver.biz.application.enums.ApplicationMonitorTypeEnum;
import com.codeages.javaskeletonserver.biz.application.mapper.ApplicationMonitorMapper;
import com.codeages.javaskeletonserver.biz.application.repository.ApplicationMonitorRepository;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationMonitorService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.github.jaemon.dinger.DingerSender;
import com.github.jaemon.dinger.core.entity.DingerRequest;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import com.querydsl.core.BooleanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ApplicationMonitorServiceImpl implements ApplicationMonitorService {

    private final ApplicationMonitorRepository applicationMonitorRepository;

    private final ApplicationMonitorMapper applicationMonitorMapper;

    private final Validator validator;

    private final DingerSender dingerSender;

    @Value("${monitor.count:3}")
    private int monitorCount;

    public ApplicationMonitorServiceImpl(ApplicationMonitorRepository applicationMonitorRepository,
                                         ApplicationMonitorMapper applicationMonitorMapper,
                                         Validator validator, DingerSender dingerSender) {
        this.applicationMonitorRepository = applicationMonitorRepository;
        this.applicationMonitorMapper = applicationMonitorMapper;
        this.validator = validator;
        this.dingerSender = dingerSender;
    }

    @Override
    public Page<ApplicationMonitorDto> search(ApplicationMonitorSearchParams searchParams, Pageable pageable) {
        QApplicationMonitor q = QApplicationMonitor.applicationMonitor;
        BooleanBuilder builder = new BooleanBuilder();
        if (searchParams.getApplicationId() != null) {
            builder.and(q.applicationId.eq(searchParams.getApplicationId()));
        }
        if (StrUtil.isNotEmpty(searchParams.getConfig())) {
            builder.and(q.config.eq(searchParams.getConfig()));
        }
        if (StrUtil.isNotEmpty(searchParams.getRemark())) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        return applicationMonitorRepository.findAll(builder, pageable).map(applicationMonitorMapper::toDto);
    }

    @Override
    public void create(ApplicationMonitorCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        applicationMonitorRepository.save(applicationMonitorMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(ApplicationMonitorUpdateParams updateParams) {
        var applicationMonitor = applicationMonitorRepository.findById(updateParams.getId())
                                                             .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        applicationMonitorMapper.toUpdateEntity(applicationMonitor, updateParams);
        applicationMonitorRepository.save(applicationMonitor);
    }

    @Override
    public void delete(Long id) {
        applicationMonitorRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        applicationMonitorRepository.deleteById(id);
    }

    @Override
    public void deleteByApplicationId(Long applicationId) {
        applicationMonitorRepository.deleteByApplicationId(applicationId);
    }

    @Override
    public Optional<ApplicationMonitorDto> getByApplicationId(Long applicationId) {
        return applicationMonitorRepository.findByApplicationId(applicationId)
                                           .map(applicationMonitorMapper::toDto);
    }

    @Override
    public ApplicationMonitorExecDto exec(ApplicationMonitorDto monitorDto) {
        if (monitorDto.getType() == null) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "类型不能为空");
        }

        if (!ApplicationMonitorTypeEnum.REQUEST.equals(monitorDto.getType())) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "暂不支持其他类型");
        }


        try {
            long startTime = System.currentTimeMillis();
            ApplicationMonitorRequestConfig config = JSONUtil.toBean(
                    monitorDto.getConfig(),
                    ApplicationMonitorRequestConfig.class
            );

            if (StrUtil.isBlank(config.getUrl())) {
                return new ApplicationMonitorExecDto(null, null, true, "", 0L);
            }

            HttpRequest request = HttpRequest.of(config.getUrl())
                                             .method(config.getMethod())
                                             .header(config.getHeaders())
                                             .body(config.getBody());

            HttpResponse response = request.execute();
            String body = response.body();
            long endTime = System.currentTimeMillis();

            String responseRegex = config.getResponseRegex();

            return new ApplicationMonitorExecDto(
                    request.toString(),
                    response.toString(),
                    Pattern.matches(responseRegex, body),
                    body,
                    endTime - startTime
            );
        } catch (Exception e) {
            return new ApplicationMonitorExecDto(null, null, false, e.getMessage(), -1L);
        }
    }

    @Override
    public void updateStatus(ApplicationMonitorDto applicationMonitorDto, ApplicationMonitorExecDto testDto) {
        ApplicationMonitorUpdateParams applicationMonitorUpdateParams = new ApplicationMonitorUpdateParams();
        applicationMonitorUpdateParams.setId(applicationMonitorDto.getId());
        if (Boolean.FALSE.equals(testDto.isSuccess())) {
            log.error(
                    "应用出现异常，应用名称：" + applicationMonitorDto.getApplicationName() + "，应用内容：" + applicationMonitorDto.getApplicationContent(),
                    testDto
            );
            applicationMonitorUpdateParams.setFailureCount(applicationMonitorDto.getFailureCount() == null ? 1L : applicationMonitorDto.getFailureCount() + 1);
            applicationMonitorUpdateParams.setFailureTime(DateUtil.date());
            applicationMonitorUpdateParams.setResponseResult(testDto.getResponse());

            update(applicationMonitorUpdateParams);

            if ((applicationMonitorUpdateParams.getFailureCount() <= monitorCount)) {
                dingerSender.send(
                        MessageSubType.TEXT,
                        DingerRequest.request(
                                "第" + applicationMonitorUpdateParams.getFailureCount() + "次提醒，连续提醒" + monitorCount + "次，应用出现异常，请尽快处理，应用名称：" + applicationMonitorDto.getApplicationName() + "，应用内容：" + applicationMonitorDto.getApplicationContent(),
                                StrUtil.isEmpty(applicationMonitorDto.getMasterMobile())? null : List.of(applicationMonitorDto.getMasterMobile())
                        )
                );
            }
        } else {
            applicationMonitorUpdateParams.setFailureCount(0L);
            applicationMonitorUpdateParams.setFailureTime(null);
            applicationMonitorUpdateParams.setResponseResult(null);
        }

        applicationMonitorUpdateParams.setResponseTime(testDto.getResponseTime());
        update(applicationMonitorUpdateParams);
    }
}


