package com.codeages.termiusplus.biz.server.mapper;

import com.codeages.termiusplus.biz.server.dto.ServerCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.dto.ServerUpdateParams;
import com.codeages.termiusplus.biz.server.dto.TreeSortParams;
import com.codeages.termiusplus.biz.server.entity.Server;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServerMapper {
    ServerDto toDto(Server server);

    Server toCreateEntity(ServerCreateParams createParams);

    @Mapping(target = "sort", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Server toUpdateEntity(@MappingTarget Server server, ServerUpdateParams params);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Server toUpdateAllEntity(@MappingTarget Server server, TreeSortParams serverUpdateParam);
}
