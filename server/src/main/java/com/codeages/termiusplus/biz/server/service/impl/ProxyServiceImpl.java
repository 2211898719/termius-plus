package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.*;
import com.codeages.termiusplus.biz.server.entity.Proxy;
import com.codeages.termiusplus.biz.server.entity.QProxy;
import com.codeages.termiusplus.biz.server.event.DeleteProxyEvent;
import com.codeages.termiusplus.biz.server.mapper.ProxyMapper;
import com.codeages.termiusplus.biz.server.repository.ProxyRepository;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private final ProxyRepository proxyRepository;

    private final ProxyMapper proxyMapper;

    private final Validator validator;

    private final ApplicationContext applicationContext;

    @Value("${file.dir}")
    private String fileDir;

    public ProxyServiceImpl(ProxyRepository proxyRepository, ProxyMapper proxyMapper, Validator validator,
                            ApplicationContext applicationContext) {
        this.proxyRepository = proxyRepository;
        this.proxyMapper = proxyMapper;
        this.validator = validator;
        this.applicationContext = applicationContext;
    }

    @Override
    public Page<ProxyDto> search(ProxySearchParams searchParams, Pageable pageable) {
        QProxy q = QProxy.proxy;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getIp())) {
            builder.and(q.ip.eq(searchParams.getIp()));
        }
        if (searchParams.getIp() != null) {
            builder.and(q.ip.eq(searchParams.getIp()));
        }

        if (StrUtil.isNotEmpty(searchParams.getPassword())) {
            builder.and(q.password.eq(searchParams.getPassword()));
        }
        if (searchParams.getPassword() != null) {
            builder.and(q.password.eq(searchParams.getPassword()));
        }

        return proxyRepository.findAll(builder, pageable)
                              .map(proxyMapper::toDto);
    }

    @Override
    public ProxyDto create(ProxyCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        return proxyMapper.toDto(proxyRepository.save(proxyMapper.toCreateEntity(createParams)));
    }

    @Override
    public void update(ProxyUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var proxy = proxyRepository.findById(updateParams.getId())
                                   .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        proxyMapper.toUpdateEntity(proxy, updateParams);
        proxyRepository.save(proxy);
    }

    @Override
    public void delete(Long id) {
        Proxy proxy = proxyRepository.findById(id)
                                     .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        applicationContext.publishEvent(new DeleteProxyEvent(this, proxyMapper.toDto(proxy)));

        proxyRepository.deleteById(id);
    }

    @Override
    public Optional<ProxyDto> findById(Long proxyId) {
        return proxyRepository.findById(proxyId)
                              .map(proxyMapper::toDto);
    }

    @Override
    public List<ProxyDto> findByIds(List<Long> proxyId) {
        return proxyRepository.findAllById(proxyId)
                              .stream()
                              .map(proxyMapper::toDto)
                              .collect(Collectors.toList());
    }

    @Override
    public ClashProxyDTO getClashProxy() {
        File file = FileUtil.file(fileDir + "/clash.json");
        if (file.exists()) {
            String content = FileUtil.readUtf8String(file);
            return JSONUtil.toBean(content, ClashProxyDTO.class);
        }

        return syncClashProxy();
    }

    @Override
    @SneakyThrows
    public ClashProxyDTO syncClashProxy() {
        ClashProxyDTO clashProxyDTO = new ClashProxyDTO();
        HttpResponse direct = HttpRequest.get("https://cdn.jsdelivr.net/gh/Loyalsoldier/clash-rules@release/direct.txt")
                                         .execute();
        if (direct.getStatus() == 200) {
            clashProxyDTO.setDirect(parsePayload(direct.body()));
        }

        HttpResponse proxy = HttpRequest.get("https://cdn.jsdelivr.net/gh/Loyalsoldier/clash-rules@release/proxy.txt")
                                        .execute();
        if (proxy.getStatus() == 200) {
            clashProxyDTO.setProxy(parsePayload(proxy.body()));
        }

        HttpResponse reject = HttpRequest.get("https://cdn.jsdelivr.net/gh/Loyalsoldier/clash-rules@release/reject.txt")
                                         .execute();
        if (reject.getStatus() == 200) {
            clashProxyDTO.setReject(parsePayload(reject.body()));
        }

        File file = FileUtil.file(fileDir + "/clash.json");
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }

        FileUtil.writeUtf8String(JSONUtil.toJsonStr(clashProxyDTO), file);

        return clashProxyDTO;
    }

    @Override
    public void syncProxyOpen() {
        List<Proxy> content = proxyRepository.findAll();
        CompletableFuture<?>[] futures = content.stream()
                                                .map(p -> CompletableFuture.runAsync(() -> {
                                                    boolean test = testProxy(p);
                                                    p.setOpen(test);
                                                }))
                                                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                         .join();

        proxyRepository.saveAll(content);

        log.info(
                "获取代理状态结束：{}/{}",
                content.stream()
                       .map(Proxy::getOpen)
                       .filter(Boolean.TRUE::equals)
                       .count(),
                content.size()
                );
    }

    private boolean testProxy(Proxy proxy) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(proxy.getIp(), Math.toIntExact(proxy.getPort())), 5000); // 2秒超时
            return true; // 连接成功，端口开放
        } catch (IOException e) {
            return false; // 连接失败，端口关闭
        }
    }

    public static List<String> parsePayload(String data) {
        List<String> payload = new ArrayList<>();
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.startsWith("  - '")) {
                String value = line.substring(5, line.length() - 2);
                value = value.replace(".", "\\.");
                if (value.startsWith("+")) {
                    value = value.substring(1);
                    value = ".*" + value;
                }
                value = "/" + value + "/";
                payload.add(value);
            }
        }
        return payload;
    }


}


