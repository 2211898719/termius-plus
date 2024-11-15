package com.codeages.termiusplus.biz.snippet.mapper;

import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import com.codeages.termiusplus.biz.snippet.entity.Command;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CommandMapper {
    CommandDto toDto(Command command);

    Command toCreateEntity(CommandCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Command toUpdateEntity(@MappingTarget Command command, CommandUpdateParams updateParams);
}
