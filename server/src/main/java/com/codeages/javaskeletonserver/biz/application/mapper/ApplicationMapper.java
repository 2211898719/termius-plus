package com.codeages.javaskeletonserver.biz.application.mapper;

import com.codeages.javaskeletonserver.biz.application.dto.ApplicationCreateParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationUpdateParams;
import com.codeages.javaskeletonserver.biz.application.entity.Application;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import com.codeages.javaskeletonserver.biz.server.entity.Server;
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
