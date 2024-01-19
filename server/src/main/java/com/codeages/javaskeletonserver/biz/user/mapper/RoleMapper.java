package com.codeages.javaskeletonserver.biz.user.mapper;

import com.codeages.javaskeletonserver.biz.user.entity.Role;
import com.codeages.javaskeletonserver.biz.user.dto.RoleDto;
import com.codeages.javaskeletonserver.biz.user.dto.RoleCreateParams;
import com.codeages.javaskeletonserver.biz.user.dto.RoleUpdateParams;
import com.codeages.javaskeletonserver.biz.user.dto.RoleSearchParams;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role role);

    List<RoleDto> toDto(List<Role> role);

    Role toCreateEntity(RoleCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Role toUpdateEntity(@MappingTarget Role role, RoleUpdateParams updateParams);
}
