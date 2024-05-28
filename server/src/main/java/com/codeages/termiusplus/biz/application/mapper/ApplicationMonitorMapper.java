package com.codeages.termiusplus.biz.application.mapper;

import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorUpdateParams;
import com.codeages.termiusplus.biz.application.entity.ApplicationMonitor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ApplicationMonitorMapper {
    ApplicationMonitorDto toDto(ApplicationMonitor applicationMonitor);

    ApplicationMonitor toCreateEntity(ApplicationMonitorCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ApplicationMonitor toUpdateEntity(@MappingTarget ApplicationMonitor applicationMonitor,
                                      ApplicationMonitorUpdateParams updateParams);
}
