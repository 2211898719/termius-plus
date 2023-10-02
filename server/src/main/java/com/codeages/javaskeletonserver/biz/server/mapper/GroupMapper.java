package com.codeages.javaskeletonserver.biz.server.mapper;

import com.codeages.javaskeletonserver.biz.server.dto.GroupCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupDto;
import com.codeages.javaskeletonserver.biz.server.dto.GroupUpdateParams;
import com.codeages.javaskeletonserver.biz.server.entity.Group;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface GroupMapper {
    GroupDto toDto(Group group);

    Group toCreateEntity(GroupCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Group toUpdateEntity(@MappingTarget Group group, GroupUpdateParams updateParams);
}
