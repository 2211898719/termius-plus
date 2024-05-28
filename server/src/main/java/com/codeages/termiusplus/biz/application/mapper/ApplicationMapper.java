package com.codeages.termiusplus.biz.application.mapper;

import com.codeages.termiusplus.biz.application.dto.ApplicationCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationUpdateParams;
import com.codeages.termiusplus.biz.application.entity.Application;
import com.codeages.termiusplus.biz.server.dto.TreeSortParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    ApplicationDto toDto(Application application);

    Application toCreateEntity(ApplicationCreateParams createParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Application toUpdateEntity(@MappingTarget Application application, ApplicationUpdateParams updateParams);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Application toUpdateAllEntity(@MappingTarget Application application, TreeSortParams serverUpdateParam);
}
