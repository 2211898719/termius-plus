package com.codeages.termiusplus.biz.user.mapper;

import com.codeages.termiusplus.biz.user.entity.Role;
import com.codeages.termiusplus.biz.user.dto.RoleDto;
import com.codeages.termiusplus.biz.user.dto.RoleCreateParams;
import com.codeages.termiusplus.biz.user.dto.RoleUpdateParams;
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
