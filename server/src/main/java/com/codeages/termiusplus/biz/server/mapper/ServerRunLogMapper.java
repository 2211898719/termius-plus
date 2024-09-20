package com.codeages.termiusplus.biz.server.mapper;

import com.codeages.termiusplus.biz.server.dto.ServerRunLogDTO;
import com.codeages.termiusplus.biz.server.entity.ServerRunLog;
import com.codeages.termiusplus.biz.server.dto.ServerRunLogCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerRunLogUpdateParams;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServerRunLogMapper {
    ServerRunLogDTO toDto(ServerRunLog serverRunLog);

    List<ServerRunLogDTO> toDtoList(List<ServerRunLog> serverRunLog);

    ServerRunLog toCreateEntity(ServerRunLogCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ServerRunLog toUpdateEntity(@MappingTarget ServerRunLog serverRunLog, ServerRunLogUpdateParams updateParams);
}
