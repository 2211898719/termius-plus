package com.codeages.termiusplus.biz.application.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.hutool.script.ScriptUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.application.config.ApplicationMonitorRequestConfig;
import com.codeages.termiusplus.biz.application.dto.*;
import com.codeages.termiusplus.biz.application.entity.ApplicationMonitorLog;
import com.codeages.termiusplus.biz.application.entity.QApplicationMonitor;
import com.codeages.termiusplus.biz.application.enums.ApplicationMonitorCheckTypeEnum;
import com.codeages.termiusplus.biz.application.enums.ApplicationMonitorTypeEnum;
import com.codeages.termiusplus.biz.application.mapper.ApplicationMonitorMapper;
import com.codeages.termiusplus.biz.application.repository.ApplicationMonitorLogRepository;
import com.codeages.termiusplus.biz.application.repository.ApplicationMonitorRepository;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApplicationMonitorServiceImpl implements ApplicationMonitorService {

    private final ApplicationMonitorRepository applicationMonitorRepository;

    private final ApplicationMonitorMapper applicationMonitorMapper;

    private final Validator validator;

    private final ApplicationMonitorLogRepository applicationMonitorLogRepository;

    @Value("${monitor.timeout:60s}")
    private Duration timeout;



    public ApplicationMonitorServiceImpl(ApplicationMonitorRepository applicationMonitorRepository,
                                         ApplicationMonitorMapper applicationMonitorMapper, Validator validator,
                                         ApplicationMonitorLogRepository applicationMonitorLogRepository) {
        this.applicationMonitorRepository = applicationMonitorRepository;
        this.applicationMonitorMapper = applicationMonitorMapper;
        this.validator = validator;
        this.applicationMonitorLogRepository = applicationMonitorLogRepository;
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
        return applicationMonitorRepository.findAll(builder, pageable)
                                           .map(applicationMonitorMapper::toDto);
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
        applicationMonitorRepository.findById(id)
                                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

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
            //[00:00, 23:06]
            List<String> timeRange = config.getTimeRange();
            //处理时间范围
            if (CollectionUtil.isNotEmpty(timeRange)) {
                String monitorStartTime = timeRange.get(0);
                String monitorEndTime = timeRange.get(1);
                String nowTime = DateUtil.format(DateUtil.date(), "HH:mm");
                if (nowTime.compareTo(monitorStartTime) < 0 || nowTime.compareTo(monitorEndTime) > 0) {
                    return new ApplicationMonitorExecDto(false, null, null, true, "不在监控时间范围内", "failure", 0L);
                }
            }

            if (StrUtil.isBlank(config.getUrl())) {
                return new ApplicationMonitorExecDto(true, null, null, true, "", "failure", 0L);
            }

            HttpRequest request = HttpRequest.of(config.getUrl())
                                             .method(config.getMethod())
                                             .header(config.getHeaders())
                                             .body(config.getBody());
            request.setMaxRedirectCount(5);
            request.timeout((int) timeout.toMillis());

            if (monitorDto.getProxy() != null) {
                request.setProxy(new Proxy(
                        monitorDto.getProxy()
                                  .getType()
                                  .getType(),
                        new InetSocketAddress(
                                monitorDto.getProxy()
                                          .getIp(),
                                monitorDto.getProxy()
                                          .getPort()
                                          .intValue()
                        )
                ));
            }

            HttpResponse response = request.execute();
            String body = response.body();
            long endTime = System.currentTimeMillis();

            String responseRegex = config.getResponseRegex();

            String checkResult;
            if (ApplicationMonitorCheckTypeEnum.JAVASCRIPT.equals(config.getCheckType())) {
                checkResult = (String) ScriptUtil.invoke(config.getResponseRegex(), "check", body);
            } else {
                checkResult = Pattern.matches(responseRegex, body) ? "success" : "返回值不匹配";
            }

            return new ApplicationMonitorExecDto(
                    true,
                    request.toString(),
                    response.toString(),
                    checkResult.equals("success"),
                    body,
                    checkResult,
                    endTime - startTime
            );
        } catch (Exception e) {
            log.error("请求监控失败", e);
            return new ApplicationMonitorExecDto(false, null, null, false, e.getMessage(), "请求无响应", 0L);
        }
    }

    @Override
    public void updateStatusAndSendMessage(ApplicationMonitorDto applicationMonitorDto,
                                           ApplicationMonitorExecDto testDto) {
        ApplicationMonitorUpdateParams applicationMonitorUpdateParams = new ApplicationMonitorUpdateParams();
        applicationMonitorUpdateParams.setId(applicationMonitorDto.getId());
        if (Boolean.FALSE.equals(testDto.isSuccess())) {
            log.error(
                    "应用出现异常，应用名称：" + applicationMonitorDto.getApplicationName() + "，应用内容：" + applicationMonitorDto.getApplicationContent() + "，异常原因：" + testDto.getRemark(),
                    testDto
                     );

            ApplicationMonitorLog applicationMonitorLog = new ApplicationMonitorLog();
            applicationMonitorLog.setApplicationId(applicationMonitorDto.getApplicationId());
            applicationMonitorLog.setDate(DateUtil.date());
            applicationMonitorLogRepository.save(applicationMonitorLog);

            applicationMonitorUpdateParams.setFailureCount(applicationMonitorDto.getFailureCount() == null ? 1L : applicationMonitorDto.getFailureCount() + 1);
            applicationMonitorDto.setFailureCount(applicationMonitorUpdateParams.getFailureCount());
            applicationMonitorUpdateParams.setFailureTime(DateUtil.date());
            applicationMonitorUpdateParams.setResponseResult(testDto.getResponse());

            update(applicationMonitorUpdateParams);
        } else {
            applicationMonitorUpdateParams.setFailureCount(0L);
            applicationMonitorUpdateParams.setFailureTime(null);
            applicationMonitorUpdateParams.setResponseResult(null);
        }

        applicationMonitorUpdateParams.setResponseTime(testDto.getResponseTime() == null ? 0L : testDto.getResponseTime());
        update(applicationMonitorUpdateParams);
    }

    @Override
    public List<ApplicationMonitorLogCountDto> getApplicationErrorRank() {
        Date startDate = DateUtil.beginOfYear(DateUtil.date());
        Date endDate = DateUtil.endOfYear(DateUtil.date());
        return applicationMonitorLogRepository.rankApplicationMonitorLogs(startDate, endDate, Pageable.ofSize(10))
                                              .getContent()
                                              .stream()
                                              .map(e -> {
                                                  ApplicationMonitorLogCountDto dto = new ApplicationMonitorLogCountDto();
                                                  dto.setApplicationId(Long.valueOf(String.valueOf(e[0])));
                                                  dto.setErrorSeconds(Long.valueOf(String.valueOf(e[1])));
                                                  return dto;
                                              })
                                              .collect(Collectors.toList());
    }
}


