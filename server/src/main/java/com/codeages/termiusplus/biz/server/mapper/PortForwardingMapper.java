package com.codeages.termiusplus.biz.server.mapper;

import com.codeages.termiusplus.biz.server.dto.*;
import com.codeages.termiusplus.biz.server.entity.PortForwarding;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PortForwardingMapper {
    PortForwarderDto toDto(PortForwarding portForwarder);

    @Mapping(target = "id", ignore = true)
    PortForwarding toEntity(PortForwarderDto portForwarderDto);

    List<PortForwarderDto> toDto(List<PortForwarding> portForwarderList);
}
