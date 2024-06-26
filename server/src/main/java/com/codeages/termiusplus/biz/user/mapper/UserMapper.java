package com.codeages.termiusplus.biz.user.mapper;

import com.codeages.termiusplus.biz.user.dto.UserCreateParams;
import com.codeages.termiusplus.biz.user.dto.UserDto;
import com.codeages.termiusplus.biz.user.dto.UserWithRolesDto;
import com.codeages.termiusplus.biz.user.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Named(value = "toDto")
    UserDto toDto(User user);

    @IterableMapping(qualifiedByName = "toDto")
    List<UserDto> toDto(List<User> user);

    UserWithRolesDto toWithRolesDto(User user);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    User toEntity(UserCreateParams params);
}
