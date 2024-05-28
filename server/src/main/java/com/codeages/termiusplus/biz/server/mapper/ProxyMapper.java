package com.codeages.termiusplus.biz.server.mapper;

import com.codeages.termiusplus.biz.server.dto.ProxyCreateParams;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import com.codeages.termiusplus.biz.server.dto.ProxyUpdateParams;
import com.codeages.termiusplus.biz.server.entity.Proxy;
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
