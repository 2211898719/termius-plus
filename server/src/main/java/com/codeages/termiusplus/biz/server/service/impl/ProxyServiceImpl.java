package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.*;
import com.codeages.termiusplus.biz.server.entity.QProxy;
import com.codeages.termiusplus.biz.server.mapper.ProxyMapper;
import com.codeages.termiusplus.biz.server.repository.ProxyRepository;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProxyServiceImpl implements ProxyService {

    private final ProxyRepository proxyRepository;

    private final ProxyMapper proxyMapper;

    private final Validator validator;

    private final HttpServletResponse response;

    @Value("${file.dir}")
    private String fileDir;

    public ProxyServiceImpl(ProxyRepository proxyRepository, ProxyMapper proxyMapper, Validator validator,
                            HttpServletResponse response) {
        this.proxyRepository = proxyRepository;
        this.proxyMapper = proxyMapper;
        this.validator = validator;
        this.response = response;
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
        return proxyRepository.findAll(builder, pageable).map(proxyMapper::toDto);
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
        proxyRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        proxyRepository.deleteById(id);
    }

    @Override
    public Optional<ProxyDto> findById(Long proxyId) {
        return proxyRepository.findById(proxyId).map(proxyMapper::toDto);
    }

    @Override
    public List<ProxyDto> findByIds(List<Long> proxyId) {
        return proxyRepository.findAllById(proxyId).stream().map(proxyMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public ClashProxyDTO getClashProxy() {
        File file = FileUtil.file(fileDir+"/clash.json");
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
        HttpResponse direct = HttpRequest.get("https://cdn.jsdelivr.net/gh/Loyalsoldier/clash-rules@release/direct.txt").execute();
        if (direct.getStatus() == 200) {
            clashProxyDTO.setDirect(parsePayload(direct.body()));
        }

        HttpResponse proxy = HttpRequest.get("https://cdn.jsdelivr.net/gh/Loyalsoldier/clash-rules@release/proxy.txt").execute();
        if (proxy.getStatus() == 200) {
            clashProxyDTO.setProxy(parsePayload(proxy.body()));
        }

        HttpResponse reject = HttpRequest.get("https://cdn.jsdelivr.net/gh/Loyalsoldier/clash-rules@release/reject.txt").execute();
        if (reject.getStatus() == 200) {
            clashProxyDTO.setReject(parsePayload(reject.body()));
        }

        File file = FileUtil.file(fileDir+"/clash.json");
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }

        FileUtil.writeUtf8String(JSONUtil.toJsonStr(clashProxyDTO), file);

        return clashProxyDTO;
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


