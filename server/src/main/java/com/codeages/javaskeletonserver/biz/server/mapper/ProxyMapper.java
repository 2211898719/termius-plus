package com.codeages.javaskeletonserver.biz.server.mapper;

import com.codeages.javaskeletonserver.biz.server.dto.ProxyCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyDto;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyUpdateParams;
import com.codeages.javaskeletonserver.biz.server.entity.Proxy;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProxyMapper {
    ProxyDto toDto(Proxy proxy);

    Proxy toCreateEntity(ProxyCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Proxy toUpdateEntity(@MappingTarget Proxy proxy, ProxyUpdateParams updateParams);
}
