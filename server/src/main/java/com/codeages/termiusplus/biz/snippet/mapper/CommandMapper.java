package com.codeages.termiusplus.biz.snippet.mapper;

import com.codeages.termiusplus.biz.snippet.entity.Command;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CommandMapper {
    CommandDto toDto(Command command);

    Command toCreateEntity(CommandCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Command toUpdateEntity(@MappingTarget Command command, CommandUpdateParams updateParams);
}
