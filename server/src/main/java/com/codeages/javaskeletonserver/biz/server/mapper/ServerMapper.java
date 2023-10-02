package com.codeages.javaskeletonserver.biz.server.mapper;

import com.codeages.javaskeletonserver.biz.server.dto.ServerCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.dto.ServerUpdateParams;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import com.codeages.javaskeletonserver.biz.server.entity.Server;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ServerMapper {
    ServerDto toDto(Server server);

    Server toCreateEntity(ServerCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Server toUpdateEntity(@MappingTarget Server server, ServerUpdateParams updateParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Server toUpdateAllEntity(@MappingTarget Server server, TreeSortParams serverUpdateParam);
}
