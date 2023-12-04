package com.codeages.javaskeletonserver.biz.snippet.mapper;

import com.codeages.javaskeletonserver.biz.snippet.entity.Command;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandDto;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandCreateParams;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandUpdateParams;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandSearchParams;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CommandMapper {
    CommandDto toDto(Command command);

    Command toCreateEntity(CommandCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Command toUpdateEntity(@MappingTarget Command command, CommandUpdateParams updateParams);
}
