package com.codeages.termiusplus.biz.application.mapper;

import com.codeages.termiusplus.biz.application.entity.ApplicationMonitorLog;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorLogDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorLogCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorLogUpdateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorLogSearchParams;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ApplicationMonitorLogMapper {
    ApplicationMonitorLogDto toDto(ApplicationMonitorLog applicationMonitorLog);

    ApplicationMonitorLog toCreateEntity(ApplicationMonitorLogCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ApplicationMonitorLog toUpdateEntity(@MappingTarget ApplicationMonitorLog applicationMonitorLog, ApplicationMonitorLogUpdateParams updateParams);
}
