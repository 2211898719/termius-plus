package com.codeages.termiusplus.biz.server.mapper;

import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorDto;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorUpdateParams;
import com.codeages.termiusplus.biz.server.entity.ServerServiceMonitor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ServerServiceMonitorMapper {
    ServerServiceMonitorDto toDto(ServerServiceMonitor serverServiceMonitor);

    ServerServiceMonitor toCreateEntity(ServerServiceMonitorCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ServerServiceMonitor toUpdateEntity(@MappingTarget ServerServiceMonitor serverServiceMonitor,
                                        ServerServiceMonitorUpdateParams updateParams);
}
