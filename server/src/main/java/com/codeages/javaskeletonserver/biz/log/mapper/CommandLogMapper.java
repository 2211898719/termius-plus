package com.codeages.javaskeletonserver.biz.log.mapper;

import com.codeages.javaskeletonserver.biz.log.entity.CommandLog;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogDto;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogUpdateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogSearchParams;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CommandLogMapper {
    CommandLogDto toDto(CommandLog commandLog);

    CommandLog toCreateEntity(CommandLogCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    CommandLog toUpdateEntity(@MappingTarget CommandLog commandLog, CommandLogUpdateParams updateParams);
}
