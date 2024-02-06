package com.codeages.javaskeletonserver.biz.log.mapper;

import com.codeages.javaskeletonserver.biz.log.dto.*;
import com.codeages.javaskeletonserver.biz.log.entity.CommandLog;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CommandLogMapper {
    CommandLogDto toDto(CommandLog commandLog);

    CommandLogSimpleDto toSimpleDto(CommandLog commandLog);

    CommandLog toCreateEntity(CommandLogCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    CommandLog toUpdateEntity(@MappingTarget CommandLog commandLog, CommandLogUpdateParams updateParams);
}
