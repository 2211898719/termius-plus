package com.codeages.termiusplus.biz.log.mapper;

import com.codeages.termiusplus.biz.log.dto.CommandLogCreateParams;
import com.codeages.termiusplus.biz.log.dto.CommandLogDto;
import com.codeages.termiusplus.biz.log.dto.CommandLogSimpleDto;
import com.codeages.termiusplus.biz.log.dto.CommandLogUpdateParams;
import com.codeages.termiusplus.biz.log.entity.CommandLog;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CommandLogMapper {
    CommandLogDto toDto(CommandLog commandLog);

    CommandLogSimpleDto toSimpleDto(CommandLog commandLog);

    CommandLog toCreateEntity(CommandLogCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    CommandLog toUpdateEntity(@MappingTarget CommandLog commandLog, CommandLogUpdateParams updateParams);
}
